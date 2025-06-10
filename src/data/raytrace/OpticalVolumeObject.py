import numpy as np
from calgraph3d.data.raytrace.OpticalObject import OpticalObject
from calgraph3d.data.raytrace.SurfaceType import SurfaceType
from volumeraytracer.volume_raytracer import OpticalVolume
from calgraph3d.data.raytrace.Intersection import Intersection
from typing import override

class OpticalVolumeObject(OpticalObject):
    def __init__(self):
        super().__init__()
        self.ior = None
        self.transculency = None
        self.scale = None
        self.volume = None
        self.globalToCudaCubes = np.array(
            [[1, 0, 0, 0],
             [0, 1, 0, 0],
             [0, 0, 1, 0],
             [0, 0, 0, 1]], dtype=np.float32
        )
        self.cudaCubesToGlobal = np.linalg.inv(self.globalToCudaCubes)
        self.update()

    def update(self):
        self.cudaCubesToGlobal = np.linalg.inv(self.globalToCudaCubes)
        self.volume = OpticalVolume(self.ior, self.transculency, self.scale)

    def getRefractiveIndex(self, positions):
        positions = positions @ self.cudaCubesToGlobal[:3, :3].T + self.cudaCubesToGlobal[:3, 3]
        return self.volume.get_ior(positions)

    def setSize(self, size):
        self.size = size
        self.ior = np.ones(size, dtype=np.float32)
        self.transculency = np.ones(size, dtype=np.float32)

    @override
    def calculateRays(self, position, direction):
        position = position @ self.globalToCudaCubes[:3, :3].T + self.globalToCudaCubes[:3, 3]
        direction = direction @ self.globalToCudaCubes[:3, :3].T
        rays = self.volume.trace_rays(position, direction, iterations=1000, bounds=self.size)
        rays = rays @ self.cudaCubesToGlobal[:3, :3].T + self.cudaCubesToGlobal[:3, 3]
        return rays

    def getVertexPositions(self):
        grid = np.mgrid[tuple([slice(0,size) for size in self.size])]
        grid = grid @ self.globalToCudaCubes[:3, :3].T + self.globalToCudaCubes[:3, 3]
        return grid

    java_code = """public Intersection getIntersection(Vector3d position, Vector3d direction, Intersection intersection, double lowerBound, double upperBound)
	{
		double x = this.midpoint.x - position.x, y = this.midpoint.y - position.y, z = this.midpoint.z - position.z;
		int mindir = -1;
		for (int i = 0; i < 3; ++i)
		{
			double dotprod=unitVolumeToGlobalRows[i].dot(direction);
			double offsetdot=unitVolumeToGlobalRows[i].dot(x,y,z);
			if (dotprod == 0)
			{
				if (Math.abs(offsetdot) < 1)
				{
					continue;
				}
				return null;
			}
			dotprod = 1 / dotprod;
			offsetdot *= dotprod;
			dotprod = Math.abs(dotprod);
			double alpha0 = offsetdot - dotprod;
			double alpha1 = offsetdot + dotprod;

			if (alpha0 > lowerBound)
			{
				lowerBound = alpha0;
				mindir = i;
			}
			upperBound = Math.min(upperBound, alpha1);
			if (lowerBound > upperBound)
			{
				return null;
			}
		}
		intersection.position.set(position, direction, lowerBound);
		if (mindir != -1)
		{
			intersection.normal.set(unitVolumeToGlobalRows[mindir]);
		}
		intersection.object = this;
		intersection.distance = lowerBound;
		return intersection;
	}"""

    @override
    def getIntersection(self, position:np.ndarray, direction:np.ndarray, intersection:Intersection, lowerBound:np.ndarray, upperBound:np.ndarray):
        intersection = Intersection(position.shape[:-1])
        position_local = position @ self.globalToCudaCubes[:3, :3].T + self.globalToCudaCubes[:3, 3]
        direction = direction @ self.globalToCudaCubes[:3, :3].T
        intersection.distance[:] = np.inf
        lowerBound = np.copy(lowerBound)
        upperBound = np.copy(upperBound)
        firstcontact = np.zeros(shape=position.shape[:-1], dtype=int)
        for i in range(3):
            dirdot0 = -position_local[i] / direction[i]
            dirdot1 = (self.size[i] - position_local[i]) / direction[i]
            mindot = np.minimum(dirdot0, dirdot1)
            firstcontact[mindot < lowerBound] = i
            lowerBound = np.maximum(lowerBound, mindot)
            upperBound = np.minimum(upperBound, np.maximum(dirdot0, dirdot1))
        mask = lowerBound < upperBound
        intersection.object[mask] = self
        intersection.position[mask] = position[mask] + direction[mask] * lowerBound[mask]
        intersection.normal[mask] = self.globalToCudaCubes[firstcontact[mask], :3]
        intersection.distance[mask] = lowerBound[mask]
        return mask



