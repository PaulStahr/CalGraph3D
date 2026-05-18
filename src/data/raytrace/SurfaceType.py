from enum import Enum


class SurfaceType(Enum):
    FLAT = 1
    SPHERICAL = 2
    CUSTOM = 3
    CYLINDER = 4
    HYPERBOLIC = 5
    PARABOLIC = 6

    @staticmethod
    def get_by_name(name:str):
        name = name.upper()
        if name in SurfaceType.__members__:
            return SurfaceType[name]
        else:
            raise ValueError(f"Unknown surface type: {name}")
