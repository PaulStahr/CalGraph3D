import numpy as np


class Intersection:
    def __init__(self, count):
        self.position = np.full(shape=(count, 3), fill_value=np.nan)
        self.normal = np.full(shape=(count, 3), fill_value=np.nan)
        self.object = np.full(shape=count, fill_value=None)
        self.distance = np.zeros(shape=count)
