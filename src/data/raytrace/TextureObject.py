import numpy as np

class TextureObject:
    def __init__(self, data:np.ndarray):
        self.data = data
        self.extendX = "clip"
        self.extendY = "repeat"


    def setColor(self, coords:np.ndarray, color:np.ndarray, xp=np):
        yf = coords[..., 0] * self.data.shape[1]
        xf = coords[..., 1] * self.data.shape[0]
        x = xf.astype(int)
        y = yf.astype(int)
        match self.extendX:
            case "clip":
                x0 = xp.clip(x, 0, self.data.shape[0] - 1)
            case "repeat":
                x0 = x % self.data.shape[0]
            case _:
                raise ValueError(f"Invalid extendX value: {self.extendX}")
        match self.extendY:
            case "clip":
                y0 = xp.clip(y, 0, self.data.shape[1] - 1)
            case "repeat":
                y0 = y % self.data.shape[1]
            case _:
                raise ValueError(f"Invalid extendY value: {self.extendY}")
        self.data[x0, y0] = color.astype(self.data.dtype)


    def addColor(self, coords:np.ndarray, color:np.ndarray, xp=np):
        yf = coords[..., 0] * self.data.shape[1]
        xf = coords[..., 1] * self.data.shape[0]
        x = xf.astype(int)
        y = yf.astype(int)
        match self.extendX:
            case "clip":
                x1 = xp.clip(x + 1, 0, self.data.shape[0] - 1)
                x0 = xp.clip(x, 0, self.data.shape[0] - 1)
            case "repeat":
                x1 = (x + 1) % self.data.shape[0]
                x0 = x % self.data.shape[0]
            case _:
                raise ValueError(f"Invalid extendX value: {self.extendX}")
        match self.extendY:
            case "clip":
                y1 = xp.clip(y + 1, 0, self.data.shape[1] - 1)
                y0 = xp.clip(y, 0, self.data.shape[1] - 1)
            case "repeat":
                y1 = (y + 1) % self.data.shape[1]
                y0 = y % self.data.shape[1]
            case _:
                raise ValueError(f"Invalid extendY value: {self.extendY}")

        xr = (xf - x0)[..., *[np.newaxis] * (self.data.ndim - 2)]
        xl = 1.0 - xr
        yr = (yf - y0)[..., *[np.newaxis] * (self.data.ndim - 2)]
        yl = 1.0 - yr

        self.data[x0, y0] += (color * xl * yl).astype(self.data.dtype)
        self.data[x0, y1] += (color * xl * yr).astype(self.data.dtype)
        self.data[x1, y0] += (color * xr * yl).astype(self.data.dtype)
        self.data[x1, y1] += (color * xr * yr).astype(self.data.dtype)


    def getColor(self, coords:np.ndarray, xp=np):
        yf = coords[..., 0] * self.data.shape[1]
        xf = coords[..., 1] * self.data.shape[0]
        x = xf.astype(int)
        y = yf.astype(int)
        match self.extendX:
            case "clip":
                x1 = xp.clip(x + 1, 0, self.data.shape[0] - 1)
                x0 = xp.clip(x, 0, self.data.shape[0] - 1)
            case "repeat":
                x1 = (x + 1) % self.data.shape[0]
                x0 = x % self.data.shape[0]
            case _:
                raise ValueError(f"Invalid extendX value: {self.extendX}")
        match self.extendY:
            case "clip":
                y1 = xp.clip(y + 1, 0, self.data.shape[1] - 1)
                y0 = xp.clip(y, 0, self.data.shape[1] - 1)
            case "repeat":
                y1 = (y + 1) % self.data.shape[1]
                y0 = y % self.data.shape[1]
            case _:
                raise ValueError(f"Invalid extendY value: {self.extendY}")

        c00 = self.data[x0, y0]
        c01 = self.data[x0, y1]
        c10 = self.data[x1, y0]
        c11 = self.data[x1, y1]
        xr = (xf - x0)[..., *[np.newaxis] * (self.data.ndim - 2)]
        xl = 1.0 - xr
        c0 = c00 * xl + c10 * xr
        c1 = c01 * xl + c11 * xr
        yr = (yf - y0)[..., *[np.newaxis] * (self.data.ndim - 2)]
        yl = 1.0 - yr
        color = c0 * yl + c1 * yr
        return color.astype(np.float32) / 255.0

