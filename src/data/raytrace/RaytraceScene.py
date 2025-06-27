import inspect

import numpy as np

from calgraph3d.data.raytrace.Intersection import Intersection
from calgraph3d.data.raytrace.MaterialType import MaterialType
from calgraph3d.data.raytrace.MeshObject import MeshObject
from calgraph3d.data.raytrace.TextureMapping import TextureMapping
from calgraph3d.data.raytrace.TextureObject import TextureObject
from calgraph3d.data.raytrace.OpticalSurfaceObject import OpticalSurfaceObject
from calgraph3d.data.raytrace.OpticalVolumeObject import OpticalVolumeObject
from jsymmath.util import ArrayUtil
import logging

logger = logging.getLogger(__name__)


class RaytraceScene:
    def __init__(self):
        self.optical_surface_objects = []
        self.optical_volume_objects = []
        self.successor_set = {}
        self.texture:TextureObject|None = None
        self.textureMapping:TextureMapping = TextureMapping.SPHERICAL


    def calculateRays(
            self,
            position:np.ndarray,
            direction:np.ndarray,
            start_objects:list,
            color:np.ndarray = None,
            trajectory:list=None,
            lower_bound:np.ndarray=None,
            upper_bound:np.ndarray=None,
            xp=None):
        if xp is None:
            xp = inspect.getmodule(type(position))
        optical_objects = self.optical_surface_objects + self.optical_volume_objects
        optical_objects = [obj for obj in optical_objects if obj.active]
        successor_list = [list() for _ in range(len(optical_objects))]
        for source_object, destination_object  in self.successor_set:
            if source_object not in optical_objects or destination_object not in optical_objects:
                continue
            source_index = optical_objects.index(source_object)
            destination_index = optical_objects.index(destination_object)
            successor_list[source_index].append(destination_index)

        rg = xp.arange(len(position), dtype=xp.int32)
        allowed_successors = [list() for _ in range(len(optical_objects)+1)]
        for start_object in start_objects:
            if start_object not in optical_objects:
                logger.log(logging.WARNING, f"Start object {start_object} not in optical objects")
                continue
            allowed_successors[optical_objects.index(start_object)].append(rg)
        allowed_successors[-1].append(rg)
        last_intersector = xp.zeros(len(position), dtype=xp.int32)

        for ibounce in range(10):
            intersection = Intersection(position.shape[:-1], xp=xp)
            if trajectory is not None:
                trajectory.append(position.copy())
            direction /= xp.linalg.norm(direction, axis=-1, keepdims=True)
            lower_bound[:] = 1e-6
            intersection.distance[:] = upper_bound[:]
            # get the closest intersection
            possible_intersectors = [list() for _ in range(len(optical_objects))]
            if np.all(np.asarray([len(suc) for suc in allowed_successors]) == 0):
                break
            for i, optical_object in enumerate(optical_objects):
                if len(allowed_successors[i]) == 0:
                    continue
                mask = xp.concatenate(allowed_successors[i])
                assert len(mask) != 0
                current_intersection = intersection[mask]
                update_mask = optical_object.getIntersection(
                    position[mask],
                    direction[mask],
                    current_intersection,
                    lower_bound[mask],
                    current_intersection.distance,
                    xp=xp)
                if isinstance(update_mask, xp.ndarray) and update_mask.dtype == bool:
                    update_mask = xp.nonzero(update_mask)[0]
                if len(update_mask) == 0:
                    continue
                mask = mask[update_mask]
                intersection[mask] = current_intersection[update_mask]
                last_intersector[mask] = optical_object.id
                possible_intersectors[i].append(mask)
            if len(allowed_successors[-1]) != 0:
                mask = xp.concatenate(allowed_successors[-1])
                current_intersection = intersection[mask]
                update_mask = np.nonzero(current_intersection.object == -1)[0]
                if len(update_mask) != 0:
                    if self.texture is not None and color is not None:
                        update2global = mask[update_mask]
                        texture_coordinates = self.textureMapping.mapCartToTex(direction[update2global], xp=xp)
                        object_color = self.texture.getColor(texture_coordinates, xp=xp)
                        if object_color.shape[1] != 4:
                            object_color = xp.concatenate([object_color, xp.ones(shape=(object_color.shape[0], 1), dtype=object_color.dtype)], axis=-1)
                        color[update2global] *= object_color

            allowed_successors = [list() for _ in range(len(optical_objects) + 1)]
            for i, optical_object in enumerate(optical_objects):
                cur_possible_intersectors = possible_intersectors[i]
                if len(cur_possible_intersectors) == 0:
                    continue
                cur_possible_intersectors = xp.concatenate(cur_possible_intersectors)
                mask = cur_possible_intersectors[intersection.object[cur_possible_intersectors] == optical_object.id]
                if len(mask) == 0:
                    continue
                if isinstance(optical_object, OpticalSurfaceObject|MeshObject):
                    active_intersection = intersection[mask]
                    match optical_object.materialType:
                        case MaterialType.ABSORPTION:
                            position[mask] = active_intersection.position
                            if color is not None and optical_object.texture is not None:
                                texture_coordinates = optical_object.getTextureCoordinates(active_intersection.position, xp=xp)
                                object_color = optical_object.texture.getColor(texture_coordinates)
                                color[mask] *= object_color
                            if isinstance(optical_object, MeshObject):
                                faceIndex = intersection.faceIndex[mask]
                                color[mask,0:3] = xp.stack((faceIndex % 7, (faceIndex / 7) % 7, ((faceIndex / (7* 7)) % 7)), axis=-1) / 8
                                color[mask,3] = 1.0
                        case MaterialType.DELETION:
                            position[mask] = xp.nan
                            direction[mask] = xp.nan
                        case MaterialType.REFRACTION:
                            c = xp.sum(direction[mask] * active_intersection.normal, axis=-1)
                            normaldot = xp.sum(xp.square(active_intersection.normal), axis=-1)
                            tmp = xp.where(c > 0, optical_object.iorq, optical_object.inviorq) * normaldot / xp.square(c) + 1
                            position[mask] = active_intersection.position
                            direction[mask] += active_intersection.normal * (xp.where(tmp > 0, xp.sqrt(tmp) - 1, -2) * c / normaldot)[..., xp.newaxis]
                        case MaterialType.REFLECTION:
                            position[mask] = active_intersection.position
                            direction[mask] -= 2 * active_intersection.normal * xp.sum(direction[mask] * active_intersection.normal, axis=-1, keepdims=True)
                        case _:
                            raise NotImplementedError(f"Material type {optical_object.materialType} not implemented")
                if isinstance(optical_object, OpticalVolumeObject):
                    active_intersection = intersection[mask]
                    next_position, next_direction, iterations = optical_object.calculateRays(
                        position=active_intersection.position,
                        direction=direction[mask], xp=xp)
                    logger.log(logging.DEBUG, 1000-xp.average(iterations))
                    position[mask] = next_position
                    direction[mask] = next_direction
                for j in successor_list[i]:
                    allowed_successors[j].append(mask)
                if optical_object.materialType != MaterialType.ABSORPTION:
                    allowed_successors[-1].append(mask)

        if trajectory is not None:
            trajectory.append(position.copy())
        return position, direction, last_intersector