from __future__ import annotations

import logging
from typing import Any, Callable

from jsymmath.geometry.AffineMatrix import AffineMatrix
import numpy as np

from calgraph3d.data.raytrace.OpticalObject import (
    ColumnTypes,
    SceneObjectColumnType,
)
from calgraph3d.data.raytrace.OpticalVolumeObject import OpticalVolumeObject
from calgraph3d.data.raytrace.ParseUtil import ParseUtil

logger = logging.getLogger(__name__)


class GuiOpticalVolumeObject(OpticalVolumeObject):

    EMPTY_VOLUME_ARRAY = []

    TYPES = ColumnTypes(
        cols=[
            SceneObjectColumnType.ID,
            SceneObjectColumnType.ACTIVE,
            SceneObjectColumnType.LOAD,
            SceneObjectColumnType.VIEW,
            SceneObjectColumnType.POSITION,
            SceneObjectColumnType.TRANSFORMATION,
            SceneObjectColumnType.COLOR,
            SceneObjectColumnType.PREVIOUS_OBJECTS,
            SceneObjectColumnType.FOLLOWING_OBJECTS,
            SceneObjectColumnType.VOLUME_SCALING,
            SceneObjectColumnType.MAX_STEPS,
            SceneObjectColumnType.INNER_POINT_TRAJECTORY_COUNT,
        ],
        visible_cols=[
            SceneObjectColumnType.ID,
            SceneObjectColumnType.ACTIVE,
            SceneObjectColumnType.LOAD,
            SceneObjectColumnType.VIEW,
            SceneObjectColumnType.POSITION,
            SceneObjectColumnType.TRANSFORMATION,
            SceneObjectColumnType.COLOR,
        ],
    )

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
        self.transformationStr = ""
        self.colorStr = ""
        self.successorStr = ""
        self.predecessorStr = ""

        # Geometry / transform
        self.unitVolumeToGlobal = AffineMatrix(np.eye(4))
        self.midpoint = np.zeros(3)

        # Volume settings
        self.volumeScaling = 1000.0
        self.maxSteps = 8000
        self.numInnerTrajectoryPoints = 10

        # Data references
        self.vol = None
        self.dcm = None
        self.vs = None
        self.ip = None

        # Relations
        self.successorArray = []
        self.predecessorArray = []

        # Change listeners
        self.changeListeners: list[Callable] = []

        # Internal helper
        self._str_builder = []

        # Apply defaults
        for k, v in self.DEFAULT_VALUES.items():
            try:
                self.setValue(k, v, variables, parser)
            except Exception:
                pass

        # Apply provided content
        if content:
            for k, v in content.items():
                self.setValue(k, v, variables, parser)

    # -------------------------------------------------------------------------
    # Types
    # -------------------------------------------------------------------------

    def getTypes(self):
        return self.TYPES

    # -------------------------------------------------------------------------
    # Listener handling
    # -------------------------------------------------------------------------

    def addChangeListener(self, listener: Callable):
        self.changeListeners.append(listener)

    def removeChangeListener(self, listener: Callable):
        if listener in self.changeListeners:
            self.changeListeners.remove(listener)

    # -------------------------------------------------------------------------
    # Value changed
    # -------------------------------------------------------------------------

    def valueChanged(self, ct=None, parser=None):

        if not self.isUpdating:

            self.isUpdating = True

            try:
                for listener in self.changeListeners:
                    try:
                        listener(self, ct)
                    except Exception:
                        logger.exception(
                            "Exception while invoking change listener"
                        )

            finally:
                self.isUpdating = False

    # -------------------------------------------------------------------------
    # File loading
    # -------------------------------------------------------------------------

    def readBinaryFile(self, file: str):

        super().readBinaryFile(file)

        self.transformationStr = str(self.unitVolumeToGlobal.tolist())
        self.positionStr = str(self.midpoint.tolist())

        self.valueChanged(
            SceneObjectColumnType.TRANSFORMATION
        )

        self.valueChanged(
            SceneObjectColumnType.POSITION
        )

        self.modified()

    def readDycom(self, file: str):

        super().readDycom(file)

        self.transformationStr = str(self.unitVolumeToGlobal.tolist())
        self.positionStr = str(self.midpoint.tolist())

        self.valueChanged(
            SceneObjectColumnType.TRANSFORMATION
        )

        self.valueChanged(
            SceneObjectColumnType.POSITION
        )

        self.modified()

    # -------------------------------------------------------------------------
    # Update values
    # -------------------------------------------------------------------------

    def updateValue(
        self,
        ct: SceneObjectColumnType,
        variables=None,
        parser=None,
    ):

        try:
            if ct == SceneObjectColumnType.POSITION:
                self.midpoint = parser.parse_vector3(self.positionStr, variables, default=np.full(3, np.nan))
                self.unitVolumeToGlobal.mat[:3, 3] = self.midpoint
                self.applyMatrix()
            elif ct == SceneObjectColumnType.COLOR:
                self.color = self.colorStr
            elif ct == SceneObjectColumnType.TRANSFORMATION:
                self.unitVolumeToGlobal = AffineMatrix(parser.parse_matrix(self.transformationStr, variables=variables))
                print(f"set transformation for {self.label} {self.transformationStr}", self.unitVolumeToGlobal.mat)
                self.applyMatrix()

            self.valueChanged(ct)

        except Exception:
            logger.exception(
                "Failed updating value"
            )

    # -------------------------------------------------------------------------
    # Set values
    # -------------------------------------------------------------------------

    def setValue(
        self,
        ct: SceneObjectColumnType,
        value,
        variables=None,
        parser:ParseUtil=None,
    ):

        try:
            if ct == SceneObjectColumnType.ACTIVE:
                self.active = bool(value)

            elif ct == SceneObjectColumnType.ID:
                self.label = str(value)

            elif ct == SceneObjectColumnType.POSITION:
                self.midpoint = parser.parse_vector3(value, variables, default=np.full(3, np.nan))
                self.positionStr = str(value)
                self.unitVolumeToGlobal.mat[:3, 3] = self.midpoint
                self.applyMatrix()

            elif ct == SceneObjectColumnType.COLOR:
                self.color = value
                self.colorStr = str(value)

            elif ct == SceneObjectColumnType.TRANSFORMATION:
                self.setTransformation(parser.parse_matrix(value, variables=variables), kind="unitToGlobal")
                print(f"set transformation for {self.label} {value}", self.unitVolumeToGlobal.mat)
                self.transformationStr = str(value)

            elif ct == SceneObjectColumnType.FOLLOWING_OBJECTS:
                self.successorArray = list(value)
                self.successorStr = str(value)

            elif ct == SceneObjectColumnType.PREVIOUS_OBJECTS:
                self.predecessorArray = list(value)
                self.predecessorStr = str(value)

            elif ct == SceneObjectColumnType.MAX_STEPS:
                self.maxSteps = int(value)

            elif ct == SceneObjectColumnType.INNER_POINT_TRAJECTORY_COUNT:
                self.numInnerTrajectoryPoints = int(value)

            elif ct == SceneObjectColumnType.VOLUME_SCALING:
                self.volumeScaling = float(value)
                self.applyMatrix()

            self.modified()
            self.valueChanged(ct)

        except Exception:
            logger.exception(
                f"Failed setting value for {ct}"
            )

    # -------------------------------------------------------------------------
    # Get values
    # -------------------------------------------------------------------------

    def getValue(self, ct: SceneObjectColumnType):

        if ct == SceneObjectColumnType.ACTIVE:
            return self.active

        elif ct == SceneObjectColumnType.ID:
            return self.label

        elif ct == SceneObjectColumnType.POSITION:
            return self.positionStr

        elif ct == SceneObjectColumnType.TRANSFORMATION:
            return self.transformationStr

        elif ct == SceneObjectColumnType.COLOR:
            return self.colorStr

        elif ct == SceneObjectColumnType.FOLLOWING_OBJECTS:
            return self.successorStr

        elif ct == SceneObjectColumnType.PREVIOUS_OBJECTS:
            return self.predecessorStr

        elif ct == SceneObjectColumnType.MAX_STEPS:
            return self.maxSteps

        elif ct == SceneObjectColumnType.INNER_POINT_TRAJECTORY_COUNT:
            return self.numInnerTrajectoryPoints

        elif ct == SceneObjectColumnType.VOLUME_SCALING:
            return self.volumeScaling

        raise ValueError(f"Unsupported column type: {ct}")

    # -------------------------------------------------------------------------
    # Volume visualization
    # -------------------------------------------------------------------------

    def view(self):
        """
        Placeholder visualization hook.

        Replace with:
        - matplotlib viewer
        - pyqtgraph volume renderer
        - napari
        - vispy
        depending on your stack.
        """
        logger.info("Viewing volume object")

    # -------------------------------------------------------------------------
    # Read / copy
    # -------------------------------------------------------------------------

    def read(
        self,
        obj,
        variables=None,
        parser=None,
    ):

        super().read(obj)

        if isinstance(obj, GuiOpticalVolumeObject):

            if obj.vol is not None:
                self.vol = obj.vol.copy()

            self.dcm = obj.dcm
            self.vs = obj.vs
            self.ip = obj.ip

    def copy(
        self,
        variables=None,
        parser=None,
    ):

        result = GuiOpticalVolumeObject(
            variables=variables,
            parser=parser,
        )

        result.read(
            self,
            variables=variables,
            parser=parser,
        )

        return result