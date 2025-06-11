import enum
class SpatialUnit(enum.Enum):
    UM = -6
    MM = -3
    CM = -2
    DM = -1
    M = 0
    KM = 3

    @property
    def magnitude(self):
        return self.value