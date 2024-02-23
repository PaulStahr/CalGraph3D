import numpy as np


class OpticalObject():
    def __init__(self):
        self.midpoint = np.zeros(shape=3)
        self.ior0 = 1.3
        self.ior1 = 1
        self.invertNormal = False

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
