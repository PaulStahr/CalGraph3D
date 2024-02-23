import numpy as np


class Intersection:
    def __init__(self):
        self.position = np.zeros(shape=3)
        self.normal = np.zeros(shape=3)
        self.object = None
        self.distance = 0
