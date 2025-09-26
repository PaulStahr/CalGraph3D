import numpy as np
from enum import Enum
import logging

from calgraph3d.data.raytrace.RaytraceScene import RaytraceScene
from calgraph3d.data.raytrace.Intersection import Intersection
from calgraph3d.data.raytrace.OpticalSurfaceObject import OpticalSurfaceObject
from calgraph3d.data.raytrace.OpticalVolumeObject import OpticalVolumeObject
from calgraph3d.data.raytrace.MeshObject import MeshObject
from jsymmath.geometry.Geometry import NearestPointCalculator
from jsymmath.util import ArrayUtil


logger = logging.getLogger(__name__)


class VisualizationMode(Enum):
    REFRACTIVE_INDEX = 1
    FOCAL_DISTANCE = 2
    DIVERGENCE = 3

class PropertyOnLineCalculator:
    def __init__(self, scene: RaytraceScene):
        self.scene = scene
        self.lineEvaluationDistances = None
        self.lineRefractiveIndices = None
        self.lineObjects = None
        self.bundleIntersectionDistances = None
        self.bundleRearPrincipalPoints = None
        self.bundleEvaluationDistances = None
        self.xp = None

    def switchToLibrary(self, xp):
        self.xp = xp
        self.lineEvaluationDistances = ArrayUtil.convert(self.lineEvaluationDistances, xp)
        self.lineRefractiveIndices = ArrayUtil.convert(self.lineRefractiveIndices, xp)
        self.bundleIntersectionDistances = ArrayUtil.convert(self.bundleIntersectionDistances, xp)
        self.bundleRearPrincipalPoints = ArrayUtil.convert(self.bundleRearPrincipalPoints, xp)
        self.bundleEvaluationDistances = ArrayUtil.convert(self.bundleEvaluationDistances, xp)
        self.lineObjects = ArrayUtil.convert(self.lineObjects, xp)

    def interpolate_rear_principal_point(self, x):
        return self.xp.interp(x, self.bundleEvaluationDistances, self.bundleRearPrincipalPoints)

    def interpolate_bundle_intersection_point(self, x):
        return self.xp.interp(x, self.bundleEvaluationDistances, self.bundleIntersectionDistances)

    def interpolate_ior(self, x):
        return self.xp.interp(x, self.lineEvaluationDistances, self.lineRefractiveIndices)

    def get_dioptres(self, measurement_point):
        dioptre = 1000 / (self.bundleIntersectionDistances - measurement_point)
        self.xp.nan_to_num(dioptre, nan=0, copy=False)
        return dioptre

    def getLineInformation(
            self,
            start_position: np.ndarray,
            start_direction: np.ndarray,
            rangeBegin: float,
            rangeEnd: float,
            mode: VisualizationMode,
            num_eval: int=1000,
            spread: float=0.1,
            sphereArc: bool=True):
        xp = ArrayUtil.getArrayModule(start_position)
        self.xp = xp
        start_direction = start_direction / xp.linalg.norm(start_direction)
        nextSurfaces = self.scene.optical_surface_objects
        nextVolumes = self.scene.optical_volume_objects
        meshSurfaces = self.scene.optical_mesh_objects



        # ---- REFRACTIVE_INDEX mode ----
        if mode == VisualizationMode.REFRACTIVE_INDEX:
            start_position = start_position.reshape(1, 3)
            start_direction = start_direction.reshape(1, 3)
            object_ids = []
            rayDistances = []
            refractiveIndices = []
            locations = []
            resultObjects = []
            lowerBound = xp.full(shape=(1,), fill_value=rangeBegin, dtype=start_position.dtype)

            while True:
                intersection = Intersection(shape=1, xp=xp)
                intersection.distance[:] = rangeEnd
                RaytraceScene.getNextIntersection(
                    start_position,
                    start_direction,
                    intersection,
                    lowerBound,
                    nextSurfaces,
                    nextVolumes,
                    meshSurfaces
                )
                assert (intersection.distance + 0.01 >= lowerBound).all(), f"Intersection distance {intersection.distance} is less than lower bound {lowerBound}"
                lowerBound[:] = intersection.distance + 0.01
                obj = intersection.object
                if obj[0] == -1 or len(object_ids) > 100:
                    break
                rayDistances.append(intersection.distance.copy())
                object_ids.append(obj)
                opt_object = self.scene.getObjectById(int(obj[0]))
                if isinstance(opt_object, OpticalVolumeObject):
                    opt_object.getIntersection(
                        start_position,
                        start_direction,
                        intersection,
                        lowerBound,
                        xp.full(shape=(1,), fill_value=rangeEnd, dtype=start_position.dtype),
                        xp=xp, enterVolume=False)
                    lowerBound[:] = intersection.distance
                    rayDistances.append(intersection.distance)
                    object_ids.append(obj)


                successors = self.scene.get_successors(self.scene.getObjectById(int(obj[0])))
                nextSurfaces = [s for s in successors if isinstance(s, OpticalSurfaceObject)]
                nextVolumes = [s for s in successors if isinstance(s, OpticalVolumeObject)]
                meshSurfaces = [s for s in successors if isinstance(s, MeshObject)]
            rayDistances = xp.concatenate(rayDistances)
            object_ids = xp.concatenate(object_ids)
            print("rayDistances", " ".join([f"{d:0.3f}" for d in rayDistances[0:10]]))
            print("objectIds", object_ids[0:10])
            for i, object_id in enumerate(object_ids):
                optobj = self.scene.getObjectById(int(object_id))
                if isinstance(optobj, OpticalSurfaceObject):
                    if xp.inner(ArrayUtil.convert(optobj.direction, xp), start_direction) < 0:
                        ior0, ior1 = optobj.ior0, optobj.ior1
                    else:
                        ior0, ior1 = optobj.ior1, optobj.ior0
                    locations.extend([rayDistances[i]] * 2)
                    refractiveIndices.extend([xp.full(shape=(1,), fill_value=ior0), xp.full(shape=(1,), fill_value=ior1)])
                    resultObjects.extend([object_id, object_id])
                elif isinstance(optobj, OpticalVolumeObject):
                    start = rayDistances[i]
                    stop = rayDistances[i + 1] if i + 1 < len(rayDistances) else rangeEnd
                    logger.log(logging.INFO, f"Sampling volume {optobj} from {start} to {stop}")
                    mults = np.linspace(start, stop, 10)
                    volume: OpticalVolumeObject = optobj
                    locations.extend(mults)
                    resultObjects.extend([object_id] * len(mults))
                    refractiveIndices.extend(volume.getRefractiveIndex((start_position[np.newaxis, :] + start_direction[np.newaxis, :] * mults[:,np.newaxis])))
                elif isinstance(optobj, MeshObject):
                    raise NotImplementedError("MeshObject refractive index sampling not implemented yet")
                else:
                    raise NotImplementedError(f"Unknown object type {type(optobj)}")
            self.lineEvaluationDistances = xp.asarray(locations)
            self.lineRefractiveIndices = xp.concatenate(refractiveIndices)
            self.lineObjects = xp.asarray(resultObjects)
        # ---- FOCAL_DISTANCE mode ----
        elif mode == VisualizationMode.FOCAL_DISTANCE:
            num_rays = 3000
            trajectory = []
            lower_bound = xp.full(shape=(num_rays,), fill_value=rangeBegin, dtype=start_position.dtype)
            upper_bound = xp.full(shape=(num_rays,), fill_value=rangeEnd, dtype=start_position.dtype)
            start_objects = self.scene.getActiveSurfaces() + self.scene.getActiveVolumes() + self.scene.getActiveMeshes()
            sp = xp.random.randn(num_rays, 3) * spread
            sp -= (sp @ start_direction.T)[:,np.newaxis] * start_direction[np.newaxis, :]
            all_start_positions = xp.repeat(start_position[xp.newaxis], num_rays, axis=0) + sp
            all_start_directions = xp.repeat(start_direction[xp.newaxis], num_rays, axis=0)
            self.scene.calculateRays(
                all_start_positions,
                all_start_directions,
                start_objects,
                None,
                trajectory,
                lower_bound,
                upper_bound,
                maxVolumeIterations = 3,
                maxBounces = 100,
                xp=xp)

            trajectory = xp.stack(trajectory, axis=1) # shape (num_rays, num_points, 3)

            pyvistadebug = False
            if pyvistadebug:
                #open pyvista-scene that shows all rays in 3d. So for each i (:,i,:) should be a line with multiple points. And for each i, j (i,j,:) should be a point.
                import pyvista as pv
                pl = pv.Plotter()
                trajectory_np = ArrayUtil.convert(trajectory, np)
                for i in range(min(trajectory_np.shape[0],100)):
                    pl.add_lines(np.concatenate((trajectory_np[i,:-1,:], trajectory_np[i, 1:,:]), axis=0), color = np.random.uniform(0,1,3) , width=1)
                #pl.add_mesh(pv.Sphere(radius=0.01, center=ArrayUtil.convert(trajectory_np, np).flatten()), color="red")
                pl.show()


            directions = xp.diff(trajectory, axis=1)
            trajectory_distances = (trajectory - start_position[xp.newaxis, xp.newaxis, :]) @ start_direction # shape (num_rays, num_points)
            indices = xp.ones(num_rays, dtype=xp.uint32)
            all_ray_indices = xp.arange(num_rays)
            intersection_distances = []
            intersection_rear_principal_points = []
            bundleEvaluationDistances = np.linspace(rangeBegin, rangeEnd, num_eval)
            for iDist, d in enumerate(bundleEvaluationDistances):
                to_increase = (trajectory_distances[all_ray_indices, indices] < d) & (indices < trajectory_distances.shape[1] - 1)
                while xp.any(to_increase):
                    indices[to_increase] += 1
                    to_increase = (trajectory_distances[all_ray_indices, indices] < d) & (indices < trajectory_distances.shape[1] - 1)

                current_positions = trajectory[all_ray_indices, indices - 1]
                current_directions = directions[all_ray_indices, indices - 1]
                npc = NearestPointCalculator(position=current_positions,
                                             direction=current_directions,
                                             tolerance=1e-4)

                intersection_distances.append((npc.intersection - start_position) @ start_direction[:, np.newaxis])
                #Calculate back principal points. Use all_start_positions, current_positions and current_directions for this
                npc = NearestPointCalculator(position=current_positions - all_start_positions,
                                       direction=current_directions,)
                intersection_rear_principal_points.append(npc.intersection @ start_direction[:, np.newaxis])


            self.bundleEvaluationDistances = xp.asarray(bundleEvaluationDistances)
            self.bundleIntersectionDistances = xp.concatenate(xp.asarray(intersection_distances))
            self.bundleRearPrincipalPoints = xp.concatenate(xp.asarray(intersection_rear_principal_points))

        # ---- DIVERGENCE mode ----
        elif mode == VisualizationMode.DIVERGENCE:
            num_rays = 1000
            num_eval = 200
            lights = []
            self.scene.getActiveLights(lights)

            bundleEvaluationDistances = np.linspace(rangeBegin, rangeEnd, num_eval)
            for light in lights:
                # Fake ray endpoints and directions
                endpoints = np.random.randn(num_rays, 3)
                enddirs = np.random.randn(num_rays, 3)
                enddirs /= np.linalg.norm(enddirs, axis=1, keepdims=True)

                # variance along direction (simplified stub)
                proj = endpoints @ start_direction
                # group by nearest distance sample
                bins = np.searchsorted(bundleEvaluationDistances, proj)
                variance = np.zeros(num_eval)
                counts = np.zeros(num_eval)
                for i, b in enumerate(bins):
                    if 0 <= b < num_eval:
                        variance[b] += np.linalg.norm(enddirs[i])**2
                        counts[b] += 1
                counts[counts == 0] = 1
                variance = np.sqrt(variance / counts)

                maxv = np.nanmax(variance)