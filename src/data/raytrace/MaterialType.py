from enum import Enum


class MaterialType(Enum):
    ABSORPTION = "Absorbation"
    DELETION = "Deletion"
    REFRACTION = "Refraction"
    REFLECTION = "Reflection"
    EMISSION = "Emission"
    RANDOM = "Random"

    def __str__(self):
        return self.value

    # ------------------------------------------------------------
    # Java-style: getByName("REFRACTION")
    # ------------------------------------------------------------
    @classmethod
    def get_by_name(cls, name: str):
        if name is None:
            return None
        return cls.__members__.get(name)

    # ------------------------------------------------------------
    # XML-safe: getByValue("Refraction")
    # ------------------------------------------------------------
    @classmethod
    def get_by_value(cls, value: str):
        if value is None:
            return None
        for item in cls:
            if item.value == value:
                return item
        raise ValueError(f"Unknown material type: {value}")