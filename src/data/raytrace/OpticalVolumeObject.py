import numpy as np
from calgraph3d.data.raytrace.OpticalObject import OpticalObject
from volumeraytracer.volume_raytracer import OpticalVolume
from calgraph3d.data.raytrace.Intersection import Intersection
from jsymmath.util import ArrayUtil
import inspect
from typing import override

class OpticalVolumeObject(OpticalObject):
    def __init__(self, label=None):
        super().__init__(label=label)
        self.ior = None
        self.translucency = None
        self.checkInnerIntersection = False
        self.volume:OpticalVolume|None = None
        self.shape = (1, 1, 1)  # Default shape
        self.globalToCudaCubes = None
        self.cudaCubesToGlobal = None
        self.unitVolumeToGlobal = np.asarray(np.eye(4, dtype=np.float32))
        self.globalToUnitVolume = np.asarray(np.eye(4, dtype=np.float32))
        self.update()

    def update(self):
        self.unitVolumeToGlobal = np.linalg.inv(self.globalToUnitVolume)
        globalToCudaCubes = np.copy(self.globalToUnitVolume)
        globalToCudaCubes[0:3, 3] += 1
        globalToCudaCubes[0:3,:] *= np.asarray(self.shape)[:, np.newaxis] * 0.5
        self.globalToCudaCubes = globalToCudaCubes
        self.cudaCubesToGlobal = np.linalg.inv(self.globalToCudaCubes)

        if self.ior is not None and self.translucency is not None:
            self.volume = OpticalVolume(self.ior, self.translucency, np.ones(3, dtype=np.float32))

    def setTransformation(self, transformation, kind='globalToUnit'):
        if kind == 'globalToUnit':
            self.globalToUnitVolume = transformation
        elif kind == 'unitToGlobal':
            self.globalToUnitVolume = np.linalg.inv(transformation)
        else:
            raise ValueError("Invalid transformation kind. Use 'globalToUnit' or 'unitToGlobal'.")
        self.update()

    def getRefractiveIndex(self, positions):
        globalToCudaCubes = ArrayUtil.convert(self.globalToCudaCubes, ArrayUtil.getArrayModule(positions))
        positions = positions @ globalToCudaCubes[:3, :3].T + globalToCudaCubes[:3, 3]
        return self.volume.evaluate_ior(positions)

    def setSize(self, shape):
        self.shape = shape
        self.ior = np.ones(shape, dtype=np.float32)
        self.translucency = np.ones(shape, dtype=np.float32)
        self.volume = None
        self.update()

    def getMesh(self):
        #use marching cubes to get mesh from ior
        from skimage import measure
        if self.ior is None:
            return None, None
        iormin = np.min(self.ior)
        iormax = np.max(self.ior)
        if iormin < iormax:
            verts, faces, normals, values = measure.marching_cubes(self.ior, level=(iormin + iormax) * 0.5, spacing=(1, 1, 1), gradient_direction='ascent')
            verts = verts @ self.cudaCubesToGlobal[:3, :3].T + self.cudaCubesToGlobal[:3, 3]
            return verts, faces
        else:
            return None, None

    @override
    def calculateRays(self,
                      position:np.ndarray,
                      direction:np.ndarray,
                      maxIterations:int=1000,
                      xp=np):
        globalToCudaCubes = ArrayUtil.convert(self.globalToCudaCubes, xp)
        cudaCubesToGlobal = ArrayUtil.convert(self.cudaCubesToGlobal, xp)
        position = position @ globalToCudaCubes[:3, :3].T + globalToCudaCubes[:3, 3]
        direction = direction @ globalToCudaCubes[:3, :3].T
        position, direction, iterations = self.volume.trace_rays(
            positions=position.astype(xp.float32),
            directions=direction.astype(xp.float32),
            iterations=xp.full(shape=position.shape[:-1], fill_value=maxIterations, dtype=xp.uint32),
            bounds=self.shape)
        position = OpticalVolume.convert(position, xp)
        direction = OpticalVolume.convert(direction, xp)
        position = position @ cudaCubesToGlobal[:3, :3].T + cudaCubesToGlobal[:3, 3]
        direction = direction @ cudaCubesToGlobal[:3, :3].T
        return position, direction, iterations

    def getVertexPositions(self,xp=np):
        grid = xp.mgrid[tuple([slice(0,size) for size in self.shape])]
        cudaCubesToGlobal = ArrayUtil.convert(self.cudaCubesToGlobal, xp)
        grid = xp.moveaxis(grid, 0, -1) @ cudaCubesToGlobal[:3, :3].T + cudaCubesToGlobal[:3, 3]
        return grid

    @override
    def getIntersection(
            self,
            position:np.ndarray,
            direction:np.ndarray,
            intersection:Intersection,
            lowerBound:np.ndarray,
            upperBound:np.ndarray,
            xp=None,
            enterVolume:bool=True):
        assert lowerBound.shape == upperBound.shape, f"lowerBound and upperBound must have the same shape, got {lowerBound.shape} and {upperBound.shape}"
        xp = inspect.getmodule(type(position)) if xp is None else xp
        shape = xp.asarray(self.shape)
        globalToCudaCubes = ArrayUtil.convert(self.globalToCudaCubes, xp)
        position_local = position @ globalToCudaCubes[:3, :3].T + globalToCudaCubes[:3, 3]
        direction_local = direction @ globalToCudaCubes[:3, :3].T
        dirdot0 = -position_local / direction_local
        dirdot1 = (shape[xp.newaxis, :] - position_local) / direction_local
        mindot = xp.minimum(dirdot0, dirdot1)
        maxdot = xp.maximum(dirdot0, dirdot1)
        firstcontact = xp.argmin(mindot, axis=-1)
        lowerBound = xp.maximum(lowerBound, xp.max(mindot, axis=-1)) #TODO this doesn't work if ray is parallel
        upperBound = xp.minimum(upperBound, xp.min(maxdot, axis=-1))
        mask = xp.nonzero(lowerBound < upperBound)
        if self.checkInnerIntersection:
            num_steps = 1000
            position_masked = position_local[mask].astype(xp.float32)
            direction_masked = direction_local[mask].astype(xp.float32)
            position_masked += direction_masked * lowerBound[*mask, xp.newaxis]
            direction_masked *= ((upperBound[*mask, xp.newaxis] - lowerBound[*mask, xp.newaxis]) / num_steps)
            position_local, distances, iteration = self.volume.get_intersection(
                positions=position_masked,
                directions=direction_masked,
                iterations=xp.full(shape=len(mask[0]), fill_value=num_steps,dtype=xp.uint32),
                enterVolume=enterVolume,
                bounds=self.shape)
            inner_mask = xp.nonzero(iteration != 0)[0]
            position_local = ArrayUtil.convert(position_local[inner_mask], xp)
            cudaCubesToGlobal = ArrayUtil.convert(self.cudaCubesToGlobal, xp)
            intersection_position = position_local @ cudaCubesToGlobal[:3, :3].T + cudaCubesToGlobal[:3, 3]
            inner_mask = ArrayUtil.convert(inner_mask, xp)
            mask = (mask[0][inner_mask],)
            distance = xp.linalg.norm(intersection_position - position[mask], axis=-1) / xp.linalg.norm(direction[mask], axis=-1)
            #distance = distances * (upperBound[mask] - lowerBound[mask]) + lowerBound[mask]
        else:
            intersection_position = position[mask] + direction[mask] * lowerBound[mask, xp.newaxis]
            distance = lowerBound[mask]

        intersection.object[mask] = self.id
        intersection.position[mask] = intersection_position
        intersection.normal[mask] = globalToCudaCubes[firstcontact[mask], :3]
        intersection.distance[mask] = distance
        return mask
