import numpy as np


class Intersection:
    def __init__(self, shape, position=None, normal=None, object=None, distance=None):
        if isinstance(shape, int):
            shape = (shape,)
        self.position = np.full(shape=(*shape, 3), fill_value=np.nan) if position is None else position
        self.normal = np.full(shape=(*shape, 3), fill_value=np.nan) if normal is None else normal
        self.object = np.full(shape=shape, fill_value=None) if object is None else object
        self.distance = np.zeros(shape=shape) if distance is None else distance

    def __getitem__(self, item):
        return Intersection(self.position[item].shape[:-1],
                             position=self.position[item],
                             normal=self.normal[item],
                             object=self.object[item],
                             distance=self.distance[item])

    def __setitem__(self, key, value):
        if isinstance(value, Intersection):
            self.position[key] = value.position
            self.normal[key] = value.normal
            self.object[key] = value.object
            self.distance[key] = value.distance
        else:
            raise ValueError("Value must be an instance of Intersection")