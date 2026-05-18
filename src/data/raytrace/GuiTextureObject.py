from __future__ import annotations

import logging
from pathlib import Path
from typing import Any

import numpy as np
from PIL import Image

from calgraph3d.data.raytrace.SurfaceType import SurfaceType
from calgraph3d.data.raytrace.TextureObject import TextureObject
from calgraph3d.data.raytrace.OpticalObject import (
    SceneObjectColumnType,
    ColumnTypes,
)

logger = logging.getLogger(__name__)


class GuiTextureObject(TextureObject):

    TYPES = ColumnTypes(
        cols=[
            SceneObjectColumnType.ID,
            SceneObjectColumnType.ACTIVE,
            SceneObjectColumnType.PATH,
            SceneObjectColumnType.POSITION,
            SceneObjectColumnType.DIRECTION,
            SceneObjectColumnType.FRAME,
            SceneObjectColumnType.OPEN,
            SceneObjectColumnType.LOAD,
            SceneObjectColumnType.SAVE,
            SceneObjectColumnType.SAVE_TO,
            SceneObjectColumnType.VIEW,
            SceneObjectColumnType.DELETE,
        ],
        visible_cols=[
            SceneObjectColumnType.ID,
            SceneObjectColumnType.ACTIVE,
            SceneObjectColumnType.POSITION,
            SceneObjectColumnType.DIRECTION,
            SceneObjectColumnType.TRANSFORMATION,
            SceneObjectColumnType.FRAME,
            SceneObjectColumnType.PATH,
            SceneObjectColumnType.OPEN,
            SceneObjectColumnType.LOAD,
            SceneObjectColumnType.SAVE,
            SceneObjectColumnType.SAVE_TO,
            SceneObjectColumnType.VIEW,
            SceneObjectColumnType.DELETE,
        ],
    )

    DEFAULT_VALUES = {
        col: col.default_value for col in TYPES.cols
    }

    # ---------------------------------------------------------------------
    # Init
    # ---------------------------------------------------------------------

    def __init__(self, content=None, variables=None, parser=None):
        super().__init__(data=None)

        self.filepath: Path | None = None
        self.filepathString = ""

        self.image: Image.Image | None = None
        self.raster = None

        self.imageObject = None  # Video/sequence abstraction placeholder
        self.frameNumber = -1
        self.frameString = "undef"

        self.positionStr = ""
        self.directionStr = ""

        self.transformationStr = None

        self.direction = np.zeros(3)
        self.midpoint = np.zeros(3)

        self.matCoordToTexture = np.eye(3)
        self.matCoordToRaster = np.eye(3)

        self._change_listeners = []

        # apply defaults
        for k, v in self.DEFAULT_VALUES.items():
            try:
                self.setValue(k, v, variables, parser)
            except Exception:
                pass

        # apply external content
        if content:
            for k, v in content.items():
                self.setValue(k, v, variables, parser)

    # ---------------------------------------------------------------------
    # Types
    # ---------------------------------------------------------------------

    def getTypes(self):
        return self.TYPES

    # ---------------------------------------------------------------------
    # Color sampling
    # ---------------------------------------------------------------------

    def getColor(self, x: float, y: float, result: list[int] | None = None):
        if self.raster is None:
            if result is not None:
                result[:] = [0, 0, 0, 0]
            return

        xi = int(x)
        yi = int(y)

        h, w = self.raster.shape[:2]

        if xi < 0 or xi >= w or yi < 0 or yi >= h:
            if result is not None:
                result[:] = [0, 0, 0, 0]
            return

        pixel = self.raster[yi, xi]

        if result is not None:
            result[:] = pixel.tolist()

    # ---------------------------------------------------------------------
    # Listener handling
    # ---------------------------------------------------------------------

    def addChangeListener(self, listener):
        self._change_listeners.append(listener)

    def removeChangeListener(self, listener):
        if listener in self._change_listeners:
            self._change_listeners.remove(listener)

    def valueChanged(self, ct=None, parser=None):
        if not self.isUpdating:
            self.isUpdating = True
            try:
                for l in self._change_listeners:
                    try:
                        l(self, ct)
                    except Exception:
                        logger.exception("Listener error")
            finally:
                self.isUpdating = False

    # ---------------------------------------------------------------------
    # Update logic (parser-driven)
    # ---------------------------------------------------------------------

    def updateValue(self, ct, variables=None, parser=None):

        if ct == SceneObjectColumnType.DIRECTION:
            self.direction = np.asarray(self.directionStr, dtype=float)

        elif ct == SceneObjectColumnType.POSITION:
            self.midpoint = np.asarray(self.positionStr, dtype=float)

        elif ct == SceneObjectColumnType.FRAME:

            if self.imageObject is None:
                self.image = None
                self.raster = None
            else:
                try:
                    frame = int(self.frameString)

                    if frame != self.frameNumber:
                        self.frameNumber = frame
                        self.image = self.imageObject.get_frame(frame)
                        self.raster = np.array(self.image)

                        self.modified()
                        self.triggerModificationEvents()

                except Exception:
                    logger.exception("Frame load failed")

        elif ct == SceneObjectColumnType.PATH:
            self.filepath = Path(self.filepathString)
            self.load()

        elif ct == SceneObjectColumnType.TRANSFORMATION:
            # placeholder: no real matrix parser implemented
            pass

        self.valueChanged(ct, parser)

    # ---------------------------------------------------------------------
    # Set values
    # ---------------------------------------------------------------------

    def setValue(self, ct, value, variables=None, parser=None):

        if ct == SceneObjectColumnType.ID:
            self.label = str(value)

        elif ct == SceneObjectColumnType.ACTIVE:
            self.active = bool(value)

        elif ct == SceneObjectColumnType.POSITION:
            self.midpoint = parser.parse_vector3(value, variables)
            self.positionStr = str(value)

        elif ct == SceneObjectColumnType.DIRECTION:
            self.direction = parser.parse_vector3(value, variables)
            self.directionStr = str(value)

        elif ct == SceneObjectColumnType.FRAME:
            self.frameNumber = parser.parse_integer(value, variables, default=np.nan)
            self.frameString = str(value)

        elif ct == SceneObjectColumnType.PATH:
            self.filepath = Path(value)
            self.filepathString = str(value)

        elif ct == SceneObjectColumnType.ACTIVE:
            self.active = bool(value)

        self.modified()
        self.valueChanged(ct)

    # ---------------------------------------------------------------------
    # Get values
    # ---------------------------------------------------------------------

    def getValue(self, ct):

        if ct == SceneObjectColumnType.ID:
            return self.label

        if ct == SceneObjectColumnType.POSITION:
            return self.positionStr

        if ct == SceneObjectColumnType.DIRECTION:
            return self.directionStr

        if ct == SceneObjectColumnType.FRAME:
            return self.frameString

        if ct == SceneObjectColumnType.PATH:
            return self.filepathString

        if ct == SceneObjectColumnType.ACTIVE:
            return self.active

        return ct.default_value

    # ---------------------------------------------------------------------
    # Loading
    # ---------------------------------------------------------------------

    def load(self):
        if self.filepath is None:
            self.image = None
            self.raster = None
            return

        path = self.filepath

        if path.is_dir():
            files = sorted(path.iterdir())
            images = [Image.open(f) for f in files if f.suffix.lower() in {".png", ".jpg", ".jpeg"}]
            self.imageObject = images

        else:
            self.image = Image.open(path)
            self.raster = np.array(self.image)

        self.frameNumber = -1
        self.modified()
        self.triggerModificationEvents()

    # ---------------------------------------------------------------------
    # Save
    # ---------------------------------------------------------------------

    def save(self):
        if self.image is None or self.filepath is None:
            return
        self.image.save(self.filepath)

    def saveTo(self, file: Path):
        if self.image is None:
            return
        self.image.save(file)

    # ---------------------------------------------------------------------
    # Intersection (unused here)
    # ---------------------------------------------------------------------

    def getIntersection(self, position, direction, intersection, lowerBound, upperBound):
        return None

    # ---------------------------------------------------------------------
    # Copy
    # ---------------------------------------------------------------------

    def copy(self, variables=None, parser=None):
        res = GuiTextureObject(variables=variables, parser=parser)
        res.read(self, variables, parser)
        return res