import inspect

import numpy as np

from calgraph3d.data.raytrace.Intersection import Intersection
from calgraph3d.data.raytrace.MaterialType import MaterialType
from calgraph3d.data.raytrace.MeshObject import MeshObject
from calgraph3d.data.raytrace.TextureMapping import TextureMapping
from calgraph3d.data.raytrace.TextureObject import TextureObject
from calgraph3d.data.raytrace.OpticalSurfaceObject import OpticalSurfaceObject
from calgraph3d.data.raytrace.OpticalVolumeObject import OpticalVolumeObject
from calgraph3d.data.raytrace.OpticalObject import OpticalObject
from jsymmath.util import ArrayUtil
from typing import Callable
import enum
import logging

logger = logging.getLogger(__name__)

class BlendFactor(enum.Enum):
    ZERO = enum.auto()
    ONE = enum.auto()
    SRC_ALPHA = enum.auto()
    ONE_MINUS_SRC_ALPHA = enum.auto()
    DST_ALPHA = enum.auto()
    ONE_MINUS_DST_ALPHA = enum.auto()
    SRC_ALPHA_REMAINING = enum.auto()
    ONE_MINUS_SRC_ALPHA_REMAINING = enum.auto()

def resolve_factor(factor: BlendFactor, Cs, As, Cd, Ad):
    match factor:
        case BlendFactor.ZERO:
            return 0.0
        case BlendFactor.ONE:
            return 1.0
        case BlendFactor.SRC_ALPHA:
            return As
        case BlendFactor.ONE_MINUS_SRC_ALPHA:
            return 1.0 - As
        case BlendFactor.DST_ALPHA:
            return Ad
        case BlendFactor.ONE_MINUS_DST_ALPHA:
            return 1.0 - Ad
        case BlendFactor.SRC_ALPHA_REMAINING:
            return (1.0 - Ad) * As
        case BlendFactor.ONE_MINUS_SRC_ALPHA_REMAINING:
            return Ad * (1.0 - As)
        case _:
            raise ValueError(f"Unsupported blend factor: {factor}")

def blendFunc(srcFactor: BlendFactor,
              dstFactor: BlendFactor) -> Callable:

    def blend(destination_color: np.ndarray,
              source_color: np.ndarray) -> np.ndarray:

        Cd = destination_color[..., :3]
        Ad = destination_color[..., 3:4]

        Cs = source_color[..., :3]
        As = source_color[..., 3:4]

        Fs = resolve_factor(srcFactor, Cs, As, Cd, Ad)
        Fd = resolve_factor(dstFactor, Cs, As, Cd, Ad)

        Cout = Cs * Fs + Cd * Fd
        Aout = As + Ad * (1.0 - As) # As + Ad - Ad * As

        return np.concatenate([Cout, Aout], axis=-1)

    return blend

class RaytraceScene:
    def __init__(self, maxBounces:int=10):
        self.optical_surface_objects = []
        self.optical_volume_objects = []
        self.optical_mesh_objects = []
        self.successor_set = {}
        self.texture:TextureObject|None = None
        self.textureMapping:TextureMapping = TextureMapping.SPHERICAL
        self.maxBounces:int = maxBounces

    @staticmethod
    def getNextIntersection(
            position:np.ndarray,
            direction:np.ndarray,
            nearest:Intersection,
            lowerBound:np.ndarray,
            surfaceSuccessor:list,
            volumeSuccessor:list,
            meshSuccessor:list,
            xp=None):
        if xp is None:
            xp = inspect.getmodule(type(position))
        for objectClass in [surfaceSuccessor, volumeSuccessor, meshSuccessor]:
            for opticalObject in objectClass:
                opticalObject.getIntersection(position, direction, nearest, lowerBound, nearest.distance, xp=xp)

    def getObjectById(self, object_id:int):
        assert isinstance(object_id, int), f"id must be an integer, got {type(object_id)}"
        for object_arrays in [self.optical_surface_objects, self.optical_volume_objects, self.optical_mesh_objects]:
            for obj in object_arrays:
                if obj.id == object_id:
                    return obj
        return None

    def getObjectByLabel(self, label:str):
        assert isinstance(label, str), f"label must be a string, got {type(label)}"
        for object_arrays in [self.optical_surface_objects, self.optical_volume_objects, self.optical_mesh_objects]:
            for obj in object_arrays:
                if obj.label == label:
                    return obj
        return None

    def getActiveSurfaces(self):
        return [obj for obj in self.optical_surface_objects if obj.active]

    def getActiveVolumes(self):
        return [obj for obj in self.optical_volume_objects if obj.active]

    def getActiveMeshes(self):
        return [obj for obj in self.optical_mesh_objects if obj.active]

    def getActiveEmissions(self):
        return [obj for obj in self.optical_surface_objects + self.optical_volume_objects + self.optical_mesh_objects if obj.active and obj.materialType == MaterialType.EMISSION]

    def get_predecessors(self, obj:OpticalSurfaceObject|OpticalVolumeObject|MeshObject):
        return {source for source, dest in self.successor_set if dest == obj}

    def get_successors(self, obj:OpticalSurfaceObject|OpticalVolumeObject|MeshObject):
        return {dest for source, dest in self.successor_set if source == obj}

    def reset_ids(self):
        for object_arrays in [self.optical_surface_objects, self.optical_volume_objects, self.optical_mesh_objects]:
            for obj in object_arrays:
                obj.reset_id()

    def check_scene(self):
        all_objects = self.optical_surface_objects + self.optical_volume_objects + self.optical_mesh_objects
        ids = [obj.id for obj in all_objects]
        ids = np.sort(ids)
        duplicated_ids = np.unique(ids[1:][ids[1:] == ids[:-1]])
        if len(duplicated_ids) > 0:
            raise ValueError(f"Duplicated object ids found: {duplicated_ids}")

    def calculateRays(
            self,
            position:np.ndarray,
            direction:np.ndarray,
            start_objects:list,
            color:np.ndarray = None,
            trajectory:list=None,
            lower_bound:np.ndarray=None,
            upper_bound:np.ndarray=None,
            maxVolumeIterations:int=1000,
            maxBounces:int|None=None,
            blend_function = None,
            xp=None):
        self.check_scene()
        if blend_function is None:
            blend_function = blendFunc(BlendFactor.SRC_ALPHA, BlendFactor.ONE_MINUS_SRC_ALPHA)
        if maxBounces is None:
            maxBounces = self.maxBounces
        if xp is None:
            xp = inspect.getmodule(type(position))
        position = xp.copy(position)
        direction = xp.copy(direction)
        optical_objects = self.optical_surface_objects + self.optical_volume_objects + self.optical_mesh_objects
        optical_objects = [obj for obj in optical_objects if obj.active]
        successor_list = [list() for _ in range(len(optical_objects))]
        for source_object, destination_object  in self.successor_set:
            if source_object not in optical_objects or destination_object not in optical_objects:
                continue
            source_index = optical_objects.index(source_object)
            destination_index = optical_objects.index(destination_object)
            successor_list[source_index].append(destination_index)

        rg = xp.arange(len(position), dtype=xp.int32)
        allowed_successors = [list() for _ in range(len(optical_objects)+1)] #stores for every object, which rays are allowed to hit it
        for start_object in start_objects:
            if start_object not in optical_objects:
                logger.log(logging.WARNING, f"Start object {start_object} not in optical objects")
                continue
            allowed_successors[optical_objects.index(start_object)].append(rg)
        allowed_successors[-1].append(rg)
        last_intersector = xp.zeros(len(position), dtype=xp.int32)

        for ibounce in range(maxBounces):
            intersection = Intersection(position.shape[:-1], xp=xp)
            if trajectory is not None:
                trajectory.append(position.copy())
            direction /= xp.linalg.norm(direction, axis=-1, keepdims=True)
            lower_bound[:] = 1e-6
            intersection.distance[:] = upper_bound[:]
            # get the closest intersection
            if np.all(np.asarray([len(suc) for suc in allowed_successors]) == 0):
                break
            possible_intersectors = [list() for _ in range(len(optical_objects))]
            for i_optical_object, optical_object in enumerate(optical_objects):
                if len(allowed_successors[i_optical_object]) == 0:
                    continue
                mask = xp.concatenate(allowed_successors[i_optical_object])
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
                assert update_mask is not None, f"Optical object {optical_object} returned None as update mask"
                if len(update_mask) == 0:
                    continue
                mask = mask[update_mask]
                intersection[mask] = current_intersection[update_mask]
                last_intersector[mask] = optical_object.id
                possible_intersectors[i_optical_object].append(mask)
            if len(allowed_successors[-1]) != 0:
                mask = xp.concatenate(allowed_successors[-1])
                current_intersection = intersection[mask]
                update_mask = np.nonzero(current_intersection.object == -1)[0]
                if len(update_mask) != 0:
                    if self.texture is not None and color is not None:
                        update2global = mask[update_mask]
                        texture_coordinates = self.textureMapping.mapCartToTex(direction[update2global], xp=xp)
                        object_color = self.texture.getColor(texture_coordinates, xp=xp)
                        if object_color.shape[-1] != 4:
                            object_color = xp.concatenate([object_color, xp.ones(shape=(object_color.shape[0], 1), dtype=object_color.dtype)], axis=-1)
                        #color[update2global] *= object_color
                        color[update2global] = blend_function(color[update2global], object_color)

            allowed_successors = [list() for _ in range(len(optical_objects) + 1)]
            for i_optical_object, optical_object in enumerate(optical_objects):
                cur_possible_intersectors = possible_intersectors[i_optical_object]
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
                            if color is not None:
                                if isinstance(optical_object, OpticalSurfaceObject):
                                    object_color = optical_object.getColor(active_intersection.position, xp=xp)
                                    color[mask] = blend_function(color[mask], object_color)
                                elif isinstance(optical_object, MeshObject):
                                    faceIndex = intersection.faceIndex[mask]
                                    color[mask,0:3] = xp.stack((faceIndex % 7, (faceIndex / 7) % 7, ((faceIndex / (7* 7)) % 7)), axis=-1) / 8
                                    color[mask,3] = 1.0
                        case MaterialType.DELETION:
                            position[mask] = xp.nan
                            direction[mask] = xp.nan
                        case MaterialType.REFRACTION:
                            c = xp.sum(direction[mask] * active_intersection.normal, axis=-1)
                            normaldot = xp.sum(xp.square(active_intersection.normal), axis=-1)
                            iorq = optical_object.get_ior(
                                position=active_intersection.position,
                                non_inverted=c>0,
                                xp=xp)
                            tmp = iorq * normaldot / xp.square(c) + 1
                            position[mask] = active_intersection.position
                            direction[mask] += active_intersection.normal * (xp.where(tmp > 0, xp.sqrt(tmp) - 1, -2) * c / normaldot)[..., xp.newaxis]

                            if color is not None:
                                if isinstance(optical_object, OpticalSurfaceObject):
                                    object_color = optical_object.getColor(active_intersection.position, xp=xp)
                                    if object_color is not None:
                                        color[mask] = blend_function(color[mask], object_color)
                                elif isinstance(optical_object, MeshObject):
                                    faceIndex = intersection.faceIndex[mask]
                                    object_color = xp.stack((faceIndex % 7, (faceIndex / 7) % 7, ((faceIndex / (7* 7)) % 7),xp.full(len(faceIndex), fill_value=8)), axis=-1) / 8
                                    color[mask] = blend_function(color[mask], object_color)

                        case MaterialType.REFLECTION:
                            position[mask] = active_intersection.position
                            direction[mask] -= 2 * active_intersection.normal * xp.sum(direction[mask] * active_intersection.normal, axis=-1, keepdims=True)
                        case _:
                            raise NotImplementedError(f"Material type {optical_object.materialType} not implemented")
                if isinstance(optical_object, OpticalVolumeObject):
                    active_intersection = intersection[mask]
                    next_position, next_direction, iterations = optical_object.calculateRays(
                        position=active_intersection.position,
                        direction=direction[mask],
                        maxIterations=maxVolumeIterations,
                        xp=xp)
                    logger.log(logging.DEBUG, 1000-xp.average(iterations))
                    position[mask] = next_position
                    direction[mask] = next_direction
                for j in successor_list[i_optical_object]:
                    allowed_successors[j].append(mask)
                if optical_object.materialType != MaterialType.ABSORPTION:
                    allowed_successors[-1].append(mask)

        if trajectory is not None:
            trajectory.append(position.copy())
        return position, direction, last_intersector