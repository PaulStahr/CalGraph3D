from enum import Enum
class MaterialType(Enum):
    ABSORPTION = "Absorption"
    DELETION = "Deletion"
    REFRACTION = "Refraction"
    REFLECTION = "Reflection"
    EMISSION = "Emission"
    RANDOM = "Random"

    def __str__(self):
        return self.value