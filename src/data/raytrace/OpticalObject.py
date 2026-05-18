import numpy as np
from enum import Enum
from typing import Any, Callable, List, Optional

from calgraph3d.data.raytrace.Intersection import Intersection
from calgraph3d.data.raytrace.MaterialType import MaterialType
from calgraph3d.data.raytrace.ParseUtil import ParseUtil


class SceneObjectColumnType(Enum):

    ID = ("Id", "Unnamed")
    ACTIVE = ("Visible", True)

    # Geometry
    POSITION = ("Position", np.zeros(3))
    DIRECTION = ("Direction", np.array([1.0, 0.0, 0.0]))
    TRANSFORMATION = ("Transformation", np.eye(4))

    # Optical
    MATERIAL = ("Material", None)
    SURFACE = ("Surface", None)
    ANCHOR_POINT = ("AnchorPoint", "NORMAL_INTERSECTION")

    # Radii / shape
    MAXRADIUS = ("Radius", 10.0)
    MINRADIUS = ("MinimumRadius", 0.0)
    CONIC_CONSTANT = ("ConicConstant", 1.0)

    # Refractive indices
    IOR0 = ("Ior0", 1.0)
    IOR1 = ("Ior1", 1.0)
    DIFFUSE = ("Diffuse", 0.0)

    # Ray tracing controls
    TRACED_RAYS = ("TracedRays", 100)
    UNTRACED_RAYS = ("UntracedRays", 10000)

    # Flags
    BIDIRECTIONAL = ("Bidirectional", False)
    SMOOTH = ("Smooth", False)
    INVERT_INOUT = ("InvertInsideOutside", False)
    INVERT_NORMAL = ("InvertNormal", False)
    ALPHA_TO_RADIUS = ("AlphaToRadius", False)

    # UI / actions (missing Java columns added)
    PATH = ("Path", "")
    FRAME = ("Frame", "undef")

    LOAD = ("Load", None)
    OPEN = ("Open", None)
    SAVE = ("Save", None)
    SAVE_TO = ("SaveTo", None)
    VIEW = ("View", None)
    DELETE = ("Delete", None)

    # Volume-specific
    VOLUME_SCALING = ("VolumeScaling", 1000.0)
    MAX_STEPS = ("MaxSteps", 8000)
    INNER_POINT_TRAJECTORY_COUNT = ("InnerPoints", 10)

    # Object relations
    PREVIOUS_OBJECTS = ("PreviousObjects", "")
    FOLLOWING_OBJECTS = ("FollowingObjects", "")
    END_OBJECTS = ("EndObjects", "")

    # Visuals
    COLOR = ("Color", np.array([255, 255, 255, 255]))
    TEXTURE_OBJECT = ("TextureObject", None)
    TEXTURE_MAPPING = ("TextureMapping", "SPHERICAL")

    def __init__(self, display_name: str, default_value: Any):
        self.display_name = display_name
        self.default_value = default_value

    @classmethod
    def getByName(cls, name: str) -> Optional["SceneObjectColumnType"]:
        for item in cls:
            if item.display_name == name:
                return item
        return None


class ColumnTypes:
    """
    Lightweight replacement for Java COLUMN_TYPES.
    """

    def __init__(
        self,
        cols: List[SceneObjectColumnType],
        visible_cols: Optional[List[SceneObjectColumnType]] = None,
    ):
        self.cols = cols
        self.visible_cols = visible_cols if visible_cols is not None else cols

    def getColumnNumber(self, col: SceneObjectColumnType) -> int:
        return self.cols.index(col)

    def getVisibleColumnNumber(self, col: SceneObjectColumnType) -> int:
        return self.visible_cols.index(col)

    def colSize(self) -> int:
        return len(self.cols)

    def visibleColsSize(self) -> int:
        return len(self.visible_cols)

    def getCol(self, index: int) -> SceneObjectColumnType:
        return self.cols[index]

    def getVisibleCol(self, index: int) -> SceneObjectColumnType:
        return self.visible_cols[index]

    def getVisibleColumnNames(self) -> List[str]:
        return [c.display_name for c in self.visible_cols]


class OpticalObject:
    _counter = 0
    EMPTY_ARRAY = []

    def __init__(self, label: str | None = None):
        # Core object state
        self.modCount = 0
        self.midpoint: np.ndarray = np.zeros(shape=3)

        self.ior0 = np.nan
        self.ior1 = np.nan
        self.ior = np.nan
        self.iorq = np.nan
        self.invior = np.nan
        self.inviorq = np.nan

        self.invertNormal: bool = False
        self.materialType: MaterialType | None = None

        self.active: bool = True
        self.isUpdating: bool = False

        # Modification tracking
        self.modCount: int = 0
        self._oldModCount: int = 0

        # Data listeners
        self._dataChangeListeners: List[Callable[[Any], None]] = []

        # Object identification
        OpticalObject._counter += 1
        self.id = OpticalObject._counter

        self.label = label
        self.color = None

        # Graph relationships
        self.successor = []
        self.predecessor = []

        # Variable dependency tracking
        self.includedVariableIds: List[int] = []
        self.includedVariableTypes: List[int] = []

    def __repr__(self):
        if self.label is not None:
            return f"{self.id} {self.label}"
        return f"OpticalObject {self.id} ({self.__class__.__name__})"

    # -------------------------------------------------------------------------
    # ID / modification handling
    # -------------------------------------------------------------------------

    def reset_id(self):
        self.id = OpticalObject._counter
        OpticalObject._counter += 1

    def modified(self):
        self.modCount += 1

    def triggerModificationEvents(self):
        if self._oldModCount != self.modCount:
            self._oldModCount = self.modCount

            for listener in self._dataChangeListeners:
                listener(self)

    # -------------------------------------------------------------------------
    # Listener management
    # -------------------------------------------------------------------------

    def addDataChangeListener(self, listener: Callable[[Any], None]):
        self._dataChangeListeners.append(listener)

    def removeDataChangeListener(self, listener: Callable[[Any], None]):
        if listener in self._dataChangeListeners:
            self._dataChangeListeners.remove(listener)

    # -------------------------------------------------------------------------
    # IOR handling
    # -------------------------------------------------------------------------

    def updateIOR(self):
        ior0 = float(self.ior0)
        ior1 = float(self.ior1)

        if self.invertNormal:
            self.ior = ior1 / ior0
            self.invior = ior0 / ior1
        else:
            self.ior = ior0 / ior1
            self.invior = ior1 / ior0

        self.iorq = self.ior * self.ior - 1
        self.inviorq = self.invior * self.invior - 1

    # -------------------------------------------------------------------------
    # Dependency tracking
    # -------------------------------------------------------------------------

    def addId(self, variable_id: int, variable_type: int):
        self.includedVariableIds.append(variable_id)
        self.includedVariableTypes.append(variable_type)

    def removeType(self, variable_type: int):
        filtered = [
            (vid, vtype)
            for vid, vtype in zip(
                self.includedVariableIds,
                self.includedVariableTypes,
            )
            if vtype != variable_type
        ]

        self.includedVariableIds = [x[0] for x in filtered]
        self.includedVariableTypes = [x[1] for x in filtered]

    # -------------------------------------------------------------------------
    # Column/value system
    # -------------------------------------------------------------------------

    def getTypes(self) -> ColumnTypes | None:
        return None

    def getValue(self, ct: SceneObjectColumnType):
        mapping = {
            SceneObjectColumnType.ID: self.label,
            SceneObjectColumnType.ACTIVE: self.active,
            SceneObjectColumnType.POSITION: self.midpoint,
            SceneObjectColumnType.IOR0: self.ior0,
            SceneObjectColumnType.IOR1: self.ior1,
            SceneObjectColumnType.INVERT_NORMAL: self.invertNormal,
            SceneObjectColumnType.COLOR: self.color,
            SceneObjectColumnType.MATERIAL: self.materialType,
        }

        return mapping.get(ct, None)

    def setValue(
            self,
            ct: SceneObjectColumnType,
            value,
            variables:dict=None,
            parser:ParseUtil=None):
        raise NotImplementedError("setValue must be implemented in subclasses")

    def setValues(self, columns, values, variables:dict=None, parser:ParseUtil=None):
        if len(columns) != len(values):
            raise ValueError("columns and values must have same length")

        self.isUpdating = True

        for c, v in zip(columns, values):
            self.setValue(c, v, variables=variables, parser=parser)

        self.isUpdating = False
        self.valueChanged(None)

    def valueChanged(self, ct):
        """
        Hook for subclasses.
        """
        pass

    def updateValue(self, sct, variables=None, parser=None):
        """
        Hook for subclasses.
        """
        raise NotImplementedError(
            "updateValue must be implemented in subclasses"
        )

    # -------------------------------------------------------------------------
    # Copy/read support
    # -------------------------------------------------------------------------

    def copy(self):
        raise NotImplementedError("copy must be implemented in subclasses")

    def read(self, other: "OpticalObject"):
        types = other.getTypes()

        if types is None:
            return

        for i in range(types.colSize()):
            col = types.getCol(i)
            self.setValue(col, other.getValue(col))

    # -------------------------------------------------------------------------
    # Raytracing interface
    # -------------------------------------------------------------------------

    def calculateRays(self, position, direction):
        raise NotImplementedError(
            "calculateRays must be implemented in subclasses"
        )

    def getIntersection(
        self,
        position: np.ndarray,
        direction: np.ndarray,
        intersection: Intersection,
        lowerBound,
        upperBound,
    ):
        raise NotImplementedError(
            "getIntersection must be implemented in subclasses"
        )
