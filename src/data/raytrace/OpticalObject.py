import numpy as np
from calgraph3d.data.raytrace.Intersection import Intersection


class OpticalObject():
    def __init__(self):
        self.midpoint = np.zeros(shape=3)
        self.ior0 = 1.3
        self.ior1 = 1
        self.invertNormal = False
        self.materialType = None

    def updateIOR(self):
        ior0 = float(self.ior0)
        ior1 = float(self.ior1)

        if self.invertNormal:
            self.ior = ior1 / ior0
            self.invior = ior0 / ior1
        else:
            self.ior = ior0 / ior1
            self.invior = ior1 / ior0

        self.iorq = self.ior * self.ior - 1
        self.inviorq = self.invior * self.invior - 1

    def calculateRays(self, position, direction):
        raise NotImplementedError("calculateRays must be implemented in subclasses")

    def getIntersection(self, position:np.ndarray, direction:np.ndarray, intersection:Intersection, lowerBound:np.ndarray, upperBound:np.ndarray):
        raise NotImplementedError("getIntersection must be implemented in subclasses")