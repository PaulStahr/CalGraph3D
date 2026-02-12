import numpy as np
from wx.py.editor import directory

from calgraph3d.data.raytrace.OpticalObject import OpticalObject
from volumeraytracer.volume_raytracer import OpticalVolume
from calgraph3d.data.raytrace.Intersection import Intersection
from jsymmath.geometry.AffineMatrix import AffineMatrix
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
        self.shape = (2, 2, 2)  # Default shape
        self.globalToCudaCubes = AffineMatrix(np.eye(4, dtype=np.float32))
        self.cudaCubesToGlobal = AffineMatrix(np.eye(4, dtype=np.float32))
        self.translucency_offset = 0
        self.unitVolumeToGlobal = AffineMatrix(np.eye(4, dtype=np.float32))
        self.globalToUnitVolume = AffineMatrix(np.eye(4, dtype=np.float32))
        self.update()

    def update(self):
        self.unitVolumeToGlobal = self.globalToUnitVolume.inv()
        globalToCudaCubes = AffineMatrix(self.globalToUnitVolume)
        globalToCudaCubes.translate(np.array([1, 1, 1]), inplace=True, post=True)
        globalToCudaCubes.mat[0:3,:] *= (np.asarray(self.shape)[:, np.newaxis]) * 0.5
        self.globalToCudaCubes = globalToCudaCubes
        self.cudaCubesToGlobal = self.globalToCudaCubes.inv()

        if self.ior is not None and self.translucency is not None:
            self.volume = OpticalVolume(self.ior, self.translucency + self.translucency_offset, np.ones(3, dtype=np.float32))
            self.volume.translucency_offset = self.translucency_offset

    def setTransformation(self, transformation, kind='globalToUnit'):
        if kind == 'globalToUnit':
            self.globalToUnitVolume.mat = transformation
        elif kind == 'unitToGlobal':
            self.globalToUnitVolume.mat = np.linalg.inv(transformation)
        else:
            raise ValueError("Invalid transformation kind. Use 'globalToUnit' or 'unitToGlobal'.")
        self.update()

    def getRefractiveIndex(self, positions):
        globalToCudaCubes = self.globalToCudaCubes.convert2lib(ArrayUtil.getArrayModule(positions))
        positions = globalToCudaCubes.apply(positions)
        return self.volume.evaluate_ior(positions)

    def evaluate_inner_outer(self, positions):
        globalToCudaCubes = self.globalToCudaCubes.convert2lib(ArrayUtil.getArrayModule(positions))
        positions = globalToCudaCubes.apply(positions)
        return self.volume.evaluate_translucency(positions)

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
            verts = self.cudaCubesToGlobal.apply(verts)
            return verts, faces
        else:
            return None, None

    def getIorRange(self, domain="valid"):
        return self.volume.get_ior_range(domain=domain)

    @override
    def calculateRays(self,
                      position:np.ndarray,
                      direction:np.ndarray,
                      maxIterations:int=1000,
                      xp=np):
        globalToCudaCubes = self.globalToCudaCubes.convert2lib(xp)
        cudaCubesToGlobal = self.cudaCubesToGlobal.convert2lib(xp)
        position = globalToCudaCubes.apply(position)
        direction = globalToCudaCubes.apply(direction, only_linear=True)
        position, direction, iterations = self.volume.trace_rays(
            positions=position.astype(xp.float32),
            directions=direction.astype(xp.float32),
            iterations=xp.full(shape=position.shape[:-1], fill_value=maxIterations, dtype=xp.uint32),
            bounds=self.shape)
        position = OpticalVolume.convert(position, xp)
        direction = OpticalVolume.convert(direction, xp)
        position = cudaCubesToGlobal.apply(position)
        direction = self.cudaCubesToGlobal.apply(direction, only_linear=True)
        return position, direction, iterations

    def getVertexPositions(self,xp=np):
        grid = xp.mgrid[tuple([slice(0,size) for size in self.shape])] + 0.5
        grid = self.cudaCubesToGlobal.apply(xp.moveaxis(grid, 0, -1))
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
        globalToCudaCubes = self.globalToCudaCubes.convert2lib(xp)
        position_local = globalToCudaCubes.apply(position)
        direction_local = globalToCudaCubes.apply(direction, only_linear=True)
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
            inner_mask = ArrayUtil.convert(inner_mask, xp)
            position_local += 0.1 * direction_masked[inner_mask]
            intersection_position = self.cudaCubesToGlobal.apply(position_local)

            mask = (mask[0][inner_mask],)
            distance = xp.linalg.norm(intersection_position - position[mask], axis=-1) / xp.linalg.norm(direction[mask], axis=-1)
            #distance = distances * (upperBound[mask] - lowerBound[mask]) + lowerBound[mask]
        else:
            intersection_position = position[mask] + direction[mask] * lowerBound[mask, xp.newaxis]
            distance = lowerBound[mask]

        intersection.object[mask] = self.id
        intersection.position[mask] = intersection_position
        intersection.normal[mask] = globalToCudaCubes.mat[firstcontact[mask], :3]
        intersection.distance[mask] = distance
        return mask
