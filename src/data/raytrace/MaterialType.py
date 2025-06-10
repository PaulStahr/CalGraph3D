from enum import Enum
class MaterialType(Enum):
    ABSORBATION = "Absorbation"
    DELETION = "Deletion"
    REFRACTION = "Refraction"
    REFLECTION = "Reflection"
    EMISSION = "Emission"
    RANDOM = "Random"

    def __str__(self):
        return self.value