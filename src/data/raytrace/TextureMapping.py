import numpy as np
from abc import ABC, abstractmethod

class TextureMapping(ABC):
    name = ""

    @abstractmethod
    def mapCartToTex(self, coords, xp=np):
        pass

    @abstractmethod
    def mapTexToCart(self, tex_coords, xp=np):
        pass

    @abstractmethod
    def mapTexToSpherical(self, tex_coords, xp=np):
        pass

    @abstractmethod
    def mapSphericalToTex(self, tex_coords, xp=np):
        pass

    @abstractmethod
    def density_compensation(self, width, height):
        pass

    def default_aspect(self):
        return 1


class PerspectiveMapping(TextureMapping):
    name = "Perspective"

    def mapCartToTex(self, coords):
        return coords[..., 0:2] / coords[..., 2:3]

    def mapTexToCart(self, tex_coords):
        return np.stack((tex_coords, np.ones(shape=(*tex_coords.shape[:-1], 1))), axis=-1)

    def mapTexToSpherical(self, tex_coords):
        azimuth = np.arctan2(tex_coords[:, 1], tex_coords[:, 0]) + np.pi
        elevation = np.arctan(np.linalg.norm(tex_coords, axis=-1))
        return np.stack((azimuth, elevation), axis=-1)

    def mapSphericalToTex(self, sphere_coords):
        s = np.sin(sphere_coords)
        c = np.cos(sphere_coords)
        return s[...,1] * np.stack((c[...,0], s[...,0]),axis=-1) / c[..., 1]

    def density_compensation(self, width, height):
        raise NotImplementedError("Density compensation not implemented for PerspectiveMapping.")

TextureMapping.PERSPECTIVE = PerspectiveMapping()

TWO_PI = 2 * np.pi
INV_PI = 1.0 / np.pi
INV_TWO_PI = 1.0 / (2 * np.pi)

class SphericalMapping(TextureMapping):
    name = "Spherical"

    def mapCartToTex(self, coords, xp=np):
        azimuth = xp.arctan2(coords[...,1], coords[...,0]) * INV_TWO_PI + 0.5
        elevation = xp.arctan2(np.linalg.norm(coords[...,0:2], axis=-1), coords[...,2]) * INV_PI
        return xp.stack((azimuth, elevation),axis=-1)

    def mapTexToCart(self, tex_coords, xp=np):
        tex_coords = xp.asarray((tex_coords[..., 0] * TWO_PI, tex_coords[..., 1] * np.pi))
        s = xp.sin(tex_coords)
        c = xp.cos(tex_coords)
        return xp.stack((
            s[1] * c[0],
            s[1] * s[0],
            c[1])
        , axis=-1)

    def mapTexToSpherical(self, tex_coords):
        return tex_coords

    def mapSphericalToTex(self, tex_coords):
        return tex_coords

    def density_compensation(self, width, height):
        return np.cos(np.linspace(0, np.pi, height)).reshape(-1, 1)

TextureMapping.SPHERICAL = SphericalMapping()

class FisheyeEquidistantMapping(TextureMapping):
    name = "FisheyeEquidistant"


    """		@Override
		public double mapCartToTex(double x, double y, double z, Vector2d out) {
			double len = Math.sqrt(x * x + y * y);
			if (len == 0 && z != 0) {out.set(0.5,0.5); return 1;}
			len = Math.atan2(len, z)/ (len * TWO_PI);
			out.x = x * len + 0.5;
			out.y = y * len + 0.5;
			return Math.sin(len) / len;
		}"""
    def mapCartToTex(self, coords, xp=np):
        len = xp.linalg.norm(coords[...,0:2], axis=-1)
        len = xp.arctan2(len, coords[...,2]) / (len * TWO_PI)
        return len[...,xp.newaxis] * coords[...,0:2] + 0.5

    def mapTexToCart(self, tex_coords):
        r = np.sqrt(np.linalg.norm(tex_coords, axis=1))
        theta = np.arctan2(tex_coords[...,1], tex_coords[...,0])
        sin_r = np.sin(r)
        return np.stack(
            sin_r * np.cos(theta),
            sin_r * np.sin(theta),
            np.cos(r), axis=-1
        )

    def mapTexToSpherical(self, tex_coords):
        r = np.linalg.norm(tex_coords, axis=-1)
        theta = np.arctan2(tex_coords[...,1], tex_coords[...,0])
        return theta + np.pi, r

    def mapSphericalToTex(self, azimuth, elevation):
        return elevation * np.cos(azimuth), elevation * np.sin(azimuth)

    def density_compensation(self, width, height):
        return np.linspace(0, np.pi, height).reshape(-1, 1)

TextureMapping.FISHEYE_EQUIDISTANT = FisheyeEquidistantMapping()

class FisheyeEquidistantHalfMapping(FisheyeEquidistantMapping):
    name = "FisheyeEquidistantHalf"

    def mapCartToTex(self, coords):
        return super().mapCartToTex(coords) / 2

    def mapTexToCart(self, tex_coords):
        return super().mapTexToCart(tex_coords * 2)

    def mapTexToSpherical(self, tex_coords):
        return super().mapTexToSpherical(tex_coords * 2)

    def mapSphericalToTex(self, azimuth, elevation):
        return super().mapSphericalToTex(azimuth, elevation) / 2


class FlatMapping(TextureMapping):
    name = "Flat"

    def mapCartToTex(self, coords):
        return coords[..., 0:2]

    def mapTexToCart(self, tex_coords):
        return np.stack((tex_coords, np.zeros(shape=(*tex_coords.shape[:-1], 1))), axis=-1)

    def mapTexToSpherical(self, tex_coords):
        return tex_coords

    def mapSphericalToTex(self, spherical_coords):
        return spherical_coords

    def density_compensation(self, width, height):
        return np.ones((height, width))

    def default_aspect(self):
        return 1


texture_mappings = {
    "Perspective": PerspectiveMapping(),
    "Spherical": SphericalMapping(),
    "FisheyeEquidistant": FisheyeEquidistantMapping(),
    "FisheyeEquidistantHalf": FisheyeEquidistantHalfMapping(),
    "Flat": FlatMapping()
}

