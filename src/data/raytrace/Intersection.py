import numpy as np


class Intersection:
    def __init__(self, shape, position=None, normal=None, object:int=None, distance=None, textureCoords=None, faceIndex = None, xp=np):
        if isinstance(shape, int):
            shape = (shape,)
        self.position = xp.full(shape=(*shape, 3), fill_value=xp.nan) if position is None else position
        self.normal = xp.full(shape=(*shape, 3), fill_value=xp.nan) if normal is None else normal
        self.object = xp.full(shape=shape, fill_value=-1, dtype=xp.int8) if object is None else object
        self.distance = xp.zeros(shape=shape) if distance is None else distance
        self.faceIndex = xp.full(shape=shape, fill_value=-1, dtype=xp.int32) if faceIndex is None else faceIndex
        self.textureCoords = xp.full(shape=(*shape, 2), fill_value=xp.nan) if textureCoords is None else textureCoords

    def __getitem__(self, item):
        return Intersection(self.position[item].shape[:-1],
                            position=self.position[item],
                            normal=self.normal[item],
                            object=self.object[item],
                            distance=self.distance[item],
                            faceIndex=self.faceIndex[item],
                            textureCoords=self.textureCoords[item])

    def __setitem__(self, key, value):
        if isinstance(value, Intersection):
            self.position[key] = value.position
            self.normal[key] = value.normal
            self.object[key] = value.object
            self.distance[key] = value.distance
            self.faceIndex[key] = value.faceIndex
            self.textureCoords[key] = value.textureCoords
        else:
            raise ValueError("Value must be an instance of Intersection")