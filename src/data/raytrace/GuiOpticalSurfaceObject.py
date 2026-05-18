from __future__ import annotations

import logging
from enum import Enum
from typing import Any, Callable

import numpy as np

from calgraph3d.data.raytrace.MaterialType import MaterialType
from calgraph3d.data.raytrace.OpticalObject import (
    ColumnTypes,
    OpticalObject,
    SceneObjectColumnType,
)
from calgraph3d.data.raytrace.SurfaceType import SurfaceType
from calgraph3d.data.raytrace.TextureMapping import TextureMapping
from calgraph3d.data.raytrace.OpticalSurfaceObject import OpticalSurfaceObject

logger = logging.getLogger(__name__)


class AnchorPointEnum(Enum):
    NORMAL_INTERSECTION = "Normal Intersection"
    LENSE_CURVE = "Lense Curve"
    MIRROR_FOCAL = "Mirror Focal"

    @classmethod
    def names(cls):
        return [e.value for e in cls]

    @classmethod
    def getByName(cls, name: str):
        for current in cls:
            if current.value == name or current.name == name:
                return current
        return None

    @classmethod
    def get(cls, obj):
        if isinstance(obj, str):
            tmp = cls.getByName(obj)
            if tmp is not None:
                return tmp

        if isinstance(obj, cls):
            return obj

        raise ValueError(f"Unsupported type: {type(obj)}")

    def __str__(self):
        return self.value


class GuiOpticalSurfaceObject(OpticalSurfaceObject):

    TYPES = ColumnTypes(
        cols=[
            SceneObjectColumnType.ID,
            SceneObjectColumnType.ACTIVE,
            SceneObjectColumnType.POSITION,
            SceneObjectColumnType.DIRECTION,
            SceneObjectColumnType.MATERIAL,
            SceneObjectColumnType.MAXRADIUS,
            SceneObjectColumnType.MINRADIUS,
            SceneObjectColumnType.IOR0,
            SceneObjectColumnType.IOR1,
            SceneObjectColumnType.DIFFUSE,
            SceneObjectColumnType.COLOR,
            SceneObjectColumnType.INVERT_NORMAL,
        ],
        visible_cols=[
            SceneObjectColumnType.ID,
            SceneObjectColumnType.ACTIVE,
            SceneObjectColumnType.POSITION,
            SceneObjectColumnType.MATERIAL,
            SceneObjectColumnType.MAXRADIUS,
            SceneObjectColumnType.IOR0,
            SceneObjectColumnType.IOR1,
        ],
    )

    EMPTY_SURFACE_ARRAY = []

    DEFAULT_VALUES = {
        col: col.default_value
        for col in TYPES.cols
    }

    # -------------------------------------------------------------------------
    # Constructor
    # -------------------------------------------------------------------------

    def __init__(
        self,
        content: dict | None = None,
        variables=None,
        parser=None,
    ):
        super().__init__()

        # String representations
        self.positionStr = ""
        self.directionStr = ""
        self.ior0Str = ""
        self.ior1Str = ""
        self.maxRadiusStr = ""
        self.minRadiusStr = ""
        self.conicConstantStr = ""
        self.diffuseStr = ""
        self.successorStr = ""
        self.predecessorStr = ""
        self.endObjectStr = ""
        self.colorStr = ""

        # Geometry
        self.position = np.zeros(3)
        self.direction = np.array([1.0, 0.0, 0.0])

        # Optical parameters
        self.maxRadiusGeometric = 10.0
        self.minRadiusGeometric = 0.0
        self.diffuse = 0.0
        self.conicConstant = 1.0

        # Raytracing
        self.numTracedRays = 100
        self.numUntracedRays = 10000

        # Flags
        self.bidirectional = False
        self.invertInsideOutside = False
        self.alphaAsRadius = False

        # Surface / texture
        self.surf = None
        self.textureObjectStr = ""
        self.textureMapping = None

        # Relations
        self.successorArray = []
        self.predecessorArray = []
        self.endObjectArray = []

        # Anchor point
        self.anchorPoint = AnchorPointEnum.NORMAL_INTERSECTION

        # Change listeners
        self.changeListeners: list[Callable] = []

        # Load defaults
        for k, v in self.DEFAULT_VALUES.items():
            try:
                self.setValue(k, v, variables, parser)
            except Exception:
                pass

        # Apply content
        if content:
            for k, v in content.items():
                self.setValue(k, v, variables, parser)

    # -------------------------------------------------------------------------
    # Types
    # -------------------------------------------------------------------------

    def getTypes(self):
        return self.TYPES

    # -------------------------------------------------------------------------
    # Change listeners
    # -------------------------------------------------------------------------

    def addChangeListener(self, listener: Callable):
        self.changeListeners.append(listener)

    def removeChangeListener(self, listener: Callable):
        if listener in self.changeListeners:
            self.changeListeners.remove(listener)

    # -------------------------------------------------------------------------
    # Midpoint update
    # -------------------------------------------------------------------------

    def updateMidpoint(self):

        if self.anchorPoint == AnchorPointEnum.LENSE_CURVE:
            self.midpoint = self.position - self.direction

        elif self.anchorPoint == AnchorPointEnum.MIRROR_FOCAL:
            self.midpoint = (self.position + self.direction) * 0.5

        elif self.anchorPoint == AnchorPointEnum.NORMAL_INTERSECTION:
            self.midpoint = self.position.copy()

    # -------------------------------------------------------------------------
    # Value changed
    # -------------------------------------------------------------------------

    def valueChanged(self, ct=None, parser=None):

        self.modCount += 1

        if not self.isUpdating:

            self.isUpdating = True

            try:
                self.updateIOR()

                for listener in self.changeListeners:
                    try:
                        listener(self, ct)
                    except Exception:
                        logger.exception(
                            "Exception while invoking listener"
                        )

            finally:
                self.isUpdating = False

    # -------------------------------------------------------------------------
    # Set value
    # -------------------------------------------------------------------------

    def setValue(
            self,
            ct: SceneObjectColumnType,
            value,
            variables:dict,
            parser=None,
    ):

        # ------------------------------------------------------------
        # ACTIVE
        # ------------------------------------------------------------
        if ct == SceneObjectColumnType.ACTIVE:
            self.active = parser.parse_boolean(value) if parser else bool(value)

        # ------------------------------------------------------------
        # FLOATS
        # ------------------------------------------------------------
        elif ct == SceneObjectColumnType.DIFFUSE:
            self.diffuse = parser.parse_double(value, variables)
            self.diffuseStr = str(value)

        elif ct == SceneObjectColumnType.IOR0:
            self.ior0 = parser.parse_double(value, variables)
            self.ior0Str = str(value)

        elif ct == SceneObjectColumnType.IOR1:
            self.ior1 = parser.parse_double(value, variables, default=np.nan)
            self.ior1Str = str(value)

        elif ct == SceneObjectColumnType.SURFACE:
            self.surf = SurfaceType.get_by_name(str(value))  # validate surface type
            self.surfStr = str(value)

        elif ct == SceneObjectColumnType.MAXRADIUS:
            self.maxRadiusGeometric = parser.parse_double(value, variables, default=np.nan)
            self.maxRadiusStr = str(value)

        elif ct == SceneObjectColumnType.MINRADIUS:
            self.minRadiusGeometric = parser.parse_double(value, variables, default=np.nan)
            self.minRadiusStr = str(value)

        # ------------------------------------------------------------
        # VECTOR TYPES (THIS FIXES YOUR "{0,0,0}" BUG)
        # ------------------------------------------------------------
        elif ct == SceneObjectColumnType.DIRECTION:
            self.direction = parser.parse_vector3(value, variables, default=np.full(3, np.nan))
            self.directionStr = str(value)
            self.updateMidpoint()

        elif ct == SceneObjectColumnType.ANCHOR_POINT:
            self.anchorPoint = AnchorPointEnum.getByName(value)
            self.updateMidpoint()

        elif ct == SceneObjectColumnType.POSITION:
            self.position = parser.parse_vector3(value, variables, default=np.full(3, np.nan))
            self.positionStr = str(value)
            self.updateMidpoint()

        # ------------------------------------------------------------
        # STRING / ID
        # ------------------------------------------------------------
        elif ct == SceneObjectColumnType.ID:
            self.label = parser.parse_string(value) if parser else str(value)

        # ------------------------------------------------------------
        # MATERIAL ENUM
        # ------------------------------------------------------------
        elif ct == SceneObjectColumnType.MATERIAL:
            if isinstance(value, MaterialType):
                self.materialType = value
            elif isinstance(value, str):
                self.materialType = MaterialType.get_by_value(value)
            else:
                raise Exception(f"Material type not understood {value}")

        # ------------------------------------------------------------
        # COLOR
        # ------------------------------------------------------------
        elif ct == SceneObjectColumnType.COLOR:
            self.color = parser.parse_color(value) if parser else value
            self.colorStr = str(value)

        # ------------------------------------------------------------
        # BOOLEAN FLAG
        # ------------------------------------------------------------
        elif ct == SceneObjectColumnType.INVERT_NORMAL:
            self.invertNormal = parser.parse_boolean(value) if parser else bool(value)

        # ------------------------------------------------------------
        # END
        # ------------------------------------------------------------
        self.modified()
        self.valueChanged(ct, parser)

    # -------------------------------------------------------------------------
    # Update value
    # -------------------------------------------------------------------------

    def updateValue(
        self,
        ct: SceneObjectColumnType,
        variables=None,
        parser=None,
    ):

        if ct == SceneObjectColumnType.DIFFUSE:
            self.diffuse = float(self.diffuseStr)

        elif ct == SceneObjectColumnType.DIRECTION:
            self.direction = np.asarray(eval(self.directionStr))
            self.updateMidpoint()

        elif ct == SceneObjectColumnType.IOR0:
            self.ior0 = float(self.ior0Str)

        elif ct == SceneObjectColumnType.IOR1:
            self.ior1 = float(self.ior1Str)

        elif ct == SceneObjectColumnType.ANCHOR_POINT:
            self.anchorPoint = AnchorPointEnum.get(self.getValue(ct))
            self.updateMidpoint()

        elif ct == SceneObjectColumnType.POSITION:
            self.position = np.asarray(eval(self.positionStr))
            self.updateMidpoint()

        elif ct == SceneObjectColumnType.MAXRADIUS:
            self.maxRadiusGeometric = parser.parse_double(self.maxRadiusStr, variables, default=np.nan)

        elif ct == SceneObjectColumnType.MINRADIUS:
            self.minRadiusGeometric = parser.parse_double(self.minRadiusStr, variables, default=np.nan)

        elif ct == SceneObjectColumnType.COLOR:
            self.color = self.colorStr

        self.valueChanged(ct, parser)

    # -------------------------------------------------------------------------
    # Get value
    # -------------------------------------------------------------------------

    def getValue(self, ct: SceneObjectColumnType):

        if ct == SceneObjectColumnType.ACTIVE:
            return self.active

        elif ct == SceneObjectColumnType.DIFFUSE:
            return self.diffuseStr

        elif ct == SceneObjectColumnType.DIRECTION:
            return self.directionStr

        elif ct == SceneObjectColumnType.ID:
            return self.label

        elif ct == SceneObjectColumnType.IOR0:
            return self.ior0Str

        elif ct == SceneObjectColumnType.IOR1:
            return self.ior1Str

        elif ct == SceneObjectColumnType.MATERIAL:
            return self.materialType
        elif ct == SceneObjectColumnType.ANCHOR_POINT:
            return self.anchorPoint
        elif ct == SceneObjectColumnType.POSITION:
            return self.positionStr

        elif ct == SceneObjectColumnType.MAXRADIUS:
            return self.maxRadiusStr

        elif ct == SceneObjectColumnType.MINRADIUS:
            return self.minRadiusStr

        elif ct == SceneObjectColumnType.COLOR:
            return self.colorStr

        elif ct == SceneObjectColumnType.INVERT_NORMAL:
            return self.invertNormal

        raise ValueError(f"Unsupported column type: {ct}")

    # -------------------------------------------------------------------------
    # Copy
    # -------------------------------------------------------------------------

    def copy(self, variables=None, parser=None):

        result = GuiOpticalSurfaceObject(
            variables=variables,
            parser=parser,
        )

        for col in self.TYPES.cols:
            try:
                result.setValue(
                    col,
                    self.getValue(col),
                    variables,
                    parser,
                )
            except Exception:
                logger.exception(
                    f"Failed to copy attribute {col}"
                )

        return result