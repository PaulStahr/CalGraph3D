import numpy as np
from calgraph3d.data.raytrace.Intersection import Intersection
from calgraph3d.data.raytrace.MaterialType import MaterialType

class OpticalObject():
    _counter = 0

    def __init__(self, label=None):
        self.midpoint:np.ndarray = np.zeros(shape=3)
        self.ior0 = np.nan
        self.ior1 = np.nan
        self.ior = np.nan
        self.iorq = np.nan
        self.invior = np.nan
        self.inviorq = np.nan
        self.invertNormal:bool = False
        self.materialType:MaterialType|None = None
        self.active:bool = True
        self.modCount:int = 0
        OpticalObject._counter += 1
        self.id = OpticalObject._counter
        self.label = label

    def __repr__(self):
        if self.label is not None:
            return f"{self.id} {self.label}"
        return f"OpticalObject {self.id} ({self.__class__.__name__})"

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