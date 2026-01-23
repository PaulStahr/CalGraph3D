import numpy as np

class TextureObject:
    def __init__(self, data:np.ndarray):
        self.data = data
        self.extendX = "clip"
        self.extendY = "repeat"


    @staticmethod
    def getDoubleCoord(
            c:np.ndarray,
            axis_shape:int,
            extend_mode:str,
            ndim:int,
            round=False,
            doubleCoord=True,
            xp=np):
        cf = c * axis_shape
        if round:
            cf = xp.round(cf)
        ci = cf.astype(int)
        if not doubleCoord:
            match extend_mode:
                case "clip":
                    ci = xp.clip(ci, 0, axis_shape - 1)
                case "repeat":
                    ci = ci % axis_shape
                case _:
                    raise ValueError(f"Invalid extend mode: {extend_mode}")
            return ci
        match extend_mode:
            case "clip":
                c1 = xp.clip(ci + 1, 0, axis_shape - 1)
                c0 = xp.clip(ci, 0, axis_shape - 1)
            case "repeat":
                c1 = (ci + 1) % axis_shape
                c0 = ci % axis_shape
            case _:
                raise ValueError(f"Invalid extend mode: {extend_mode}")
        cr = (cf - c0)[..., *[np.newaxis] * (ndim - 2)]
        cl = 1.0 - cr
        return c0, c1, cl, cr


    def setColor(self, coords:np.ndarray, color:np.ndarray, round=False, xp=np):
        x = TextureObject.getDoubleCoord(
            coords[..., 1],
            self.data.shape[0],
            self.extendX,
            self.data.ndim,
            round,
            doubleCoord=False,
            xp=xp)
        y = self.getDoubleCoord(
            coords[..., 0],
            self.data.shape[1],
            self.extendY,
            self.data.ndim,
            round,
            doubleCoord=False,
            xp=xp)
        self.data[x, y] = color.astype(self.data.dtype)

    def addColor(self, coords:np.ndarray, color:np.ndarray, xp=np):
        x0, x1, xl, xr = TextureObject.getDoubleCoord(
            coords[..., 1],
            self.data.shape[0],
            self.extendX,
            self.data.ndim,
            xp=xp)
        y0, y1, yl, yr = self.getDoubleCoord(
            coords[..., 0],
            self.data.shape[1],
            self.extendY,
            self.data.ndim,
            xp=xp)

        if self.data.dtype == np.uint8:
            self.data[x0, y0] += (color * xl * yl).astype(self.data.dtype) * 255
            self.data[x0, y1] += (color * xl * yr).astype(self.data.dtype) * 255
            self.data[x1, y0] += (color * xr * yl).astype(self.data.dtype) * 255
            self.data[x1, y1] += (color * xr * yr).astype(self.data.dtype) * 255
        else:
            self.data[x0, y0] += (color * xl * yl).astype(self.data.dtype)
            self.data[x0, y1] += (color * xl * yr).astype(self.data.dtype)
            self.data[x1, y0] += (color * xr * yl).astype(self.data.dtype)
            self.data[x1, y1] += (color * xr * yr).astype(self.data.dtype)


    def getColor(self, coords:np.ndarray, xp=np):
        x0, x1, xl, xr = TextureObject.getDoubleCoord(
            coords[..., 1],
            self.data.shape[0],
            self.extendX,
            self.data.ndim,
            xp=xp)
        y0, y1, yl, yr = self.getDoubleCoord(
            coords[..., 0],
            self.data.shape[1],
            self.extendY,
            self.data.ndim,
            xp=xp)

        c00 = self.data[x0, y0]
        c01 = self.data[x0, y1]
        c10 = self.data[x1, y0]
        c11 = self.data[x1, y1]
        c0 = c00 * xl + c10 * xr
        c1 = c01 * xl + c11 * xr
        color = c0 * yl + c1 * yr
        return color.astype(np.float32) / 255.0

