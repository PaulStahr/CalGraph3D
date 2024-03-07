import numpy as np


class Intersection:
    def __init__(self, count):
        self.position = np.zeros(shape=(count, 3))
        self.normal = np.zeros(shape=(count, 3))
        self.object = np.full(shape=count, fill_value=None)
        self.distance = np.zeros(shape=count)
