import math
import random
from typing import Optional, List
from calgraph3d.data.raytrace.OpticalSurfaceObject import OpticalSurfaceObject
from calgraph3d.data.raytrace.MeshObject import MeshObject
from calgraph3d.data.raytrace.SurfaceType import SurfaceType
from jsymmath.geometry.Geometry import Geometry
from jsymmath.util import ArrayUtil

import numpy as np
import inspect


class RayGenerator:
    def __init__(
            self,
            source = None,
            threeDimensional = True,
            rng = None,
            gen = None):
        self.source = source
        self.modCount = -1
        self.threeDimensional = threeDimensional
        self.projection_vector = None
        self.v0:np.ndarray = np.zeros(3)
        self.v1:np.ndarray = np.zeros(3)
        self.arc_open = 0.0
        self.cos_arc_open = 0.0
        self.radius_radius_geom_ratio = 0.0
        self.elevation = math.nan
        self.azimuth = math.nan
        self.rng = np.random.default_rng() if rng is None else rng
        self.ignore_random = False

        if gen:
            self.source = gen.source
            self.threeDimensional = gen.threeDimensional
            self.v0 = gen.v0.copy()
            self.v1 = gen.v1.copy()
            self.arc_open = gen.arc_open
            self.elevation = gen.elevation
            self.azimuth = gen.azimuth


    def get_source(self):
        return self.source

    def init(self):
        if isinstance(self.source, OpticalSurfaceObject):
            surf:OpticalSurfaceObject = self.source
            direction = surf.direction
            radius = surf.directionLength
            self.radius_radius_geom_ratio = radius / surf.maxRadiusGeometric

            match surf.surf:
                case SurfaceType.FLAT:
                    if self.threeDimensional:
                        self.v0 = Geometry.getOrthogonalVector(direction)
                        self.v1 = np.cross(direction, self.v0)
                        self.v1 *= surf.maxRadiusGeometric / np.linalg.norm(self.v1)
                    else:
                        self.v0 = direction
                        self.v0 = np.array([self.v0[1], -self.v0[0], 0])  # Rotate 90 degrees in 2D
                    self.v0 *= surf.maxRadiusGeometric / np.linalg.norm(self.v0)

                case SurfaceType.SPHERICAL:
                    if surf.maxRadiusGeometric / radius < 1:
                        self.arc_open = math.asin(surf.maxRadiusGeometric / radius)
                    else:
                        self.arc_open = math.pi - math.asin(self.radius_radius_geom_ratio)

                    if self.threeDimensional:
                        self.v0 = Geometry.getOrthogonalVector(direction)
                        self.v1 = np.cross(direction, self.v0)
                        self.v1 *= radius / np.linalg.norm(self.v1)
                    else:
                        self.v0 = np.cross(direction, self.projection_vector)
                    self.v0 *= radius / np.linalg.norm(self.v0)
                case _:
                    raise ValueError(f"Unsupported surface type: {surf.surf}")
            self.cos_arc_open = 1 - math.cos(self.arc_open)

    def generate(self, num_rays:int, xp=np):
        if self.source.modCount != self.modCount:
            self.init()
            self.modCount = self.source.modCount

        diffuse = 0
        if isinstance(self.source, OpticalSurfaceObject):
            surf = self.source
            diffuse = surf.diffuse

            match surf.surf:
                case SurfaceType.FLAT:
                    direction = ArrayUtil.convert(surf.direction,xp)[xp.newaxis,...]
                    direction = xp.repeat(direction, num_rays, axis=0)
                    if self.threeDimensional:
                        elevation = xp.sqrt(ArrayUtil.convert(self.rng.random(num_rays), xp))
                        azimuth = (xp.pi * 2) * ArrayUtil.convert(self.rng.random(num_rays), xp)
                        position = (ArrayUtil.convert(surf.midpoint[xp.newaxis,...],xp)
                                    + ArrayUtil.convert(self.v0[xp.newaxis,...], xp) * (elevation * xp.sin(azimuth))[...,np.newaxis]
                                    + ArrayUtil.convert(self.v1[xp.newaxis,...], xp) * (elevation * xp.cos(azimuth))[...,np.newaxis])
                    else:
                        position = surf.midpoint[xp.newaxis,...] + self.v0[xp.newaxis,...] * xp.linspace(-1,1,num_rays)

                case SurfaceType.SPHERICAL:
                    direction = ArrayUtil.convert(surf.direction,xp)[xp.newaxis,...]
                    if self.threeDimensional:
                        if math.isnan(self.elevation):
                            elevation = xp.arccos(1 - xp.asarray(self.rng.random(num_rays)) * self.cos_arc_open)[..., xp.newaxis]
                        else:
                            elevation = self.elevation
                        if math.isnan(self.azimuth):
                            azimuth = (xp.asarray(self.rng.random(num_rays)) * (2 * math.pi))[..., xp.newaxis]
                        else:
                            azimuth = self.azimuth

                        sin = xp.sin(elevation)
                        v0 = ArrayUtil.convert(self.v0, xp)
                        v1 = ArrayUtil.convert(self.v1, xp)
                        direction = (direction * xp.cos(elevation)
                                     + v0[xp.newaxis, ...] * (sin * xp.sin(azimuth))
                                     + v1[xp.newaxis, ...] * (sin * xp.cos(azimuth)))
                    else:
                        alpha = 0 if num_rays <= 1 else (xp.linspace(-1,1,num_rays) * self.arc_open)[..., xp.newaxis]
                        direction = surf.direction[xp.newaxis, ...] * xp.cos(alpha) + self.v0[xp.newaxis, ...] * xp.sin(alpha)

                    position = xp.asarray(surf.midpoint[xp.newaxis, ...]) + direction

                    if False:# or surf.alpha_as_radius:
                        color = surf.read_color(position, direction)
                        dist = color[3] * 10
                        diffuse *= math.sqrt(1 / dist)
                        position = surf.midpoint + direction * dist
                case _:
                    raise ValueError(f"Unsupported surface type: {surf.surf}")

            if surf.invertNormal:
                direction = -direction

        elif isinstance(self.source, MeshObject):
            mesh = self.source
            face = mesh.get_face(self.rand_val())
            diffuse = mesh.diffuse
            alpha, beta = self.rand_val(), self.rand_val()
            if alpha + beta > 1:
                alpha = 1 - alpha
                beta = 1 - beta

            mesh.get_point(face, alpha, beta, position)
            direction_func = mesh.get_normal if mesh.smooth else lambda f, a, b, d: mesh.get_normal(f, d)
            direction_func(face, alpha, beta, direction)
            mesh.get_texture_coord(face, alpha, beta, texture_coordinate)
            direction = -direction

        if diffuse != 0:
            direction = RayGenerator.apply_diffuse_to_directions(
                direction=direction,
                diffuse=diffuse,
                projection_vector = self.projection_vector,
                rng = self.rng,
                three_dimensional=self.threeDimensional or isinstance(self.source, MeshObject),
                xp=xp
            )
        return position, direction

    @staticmethod
    def apply_diffuse_to_directions(
            direction:np.ndarray,
            diffuse:float,
            projection_vector:np.ndarray,
            rng:Optional[np.random.Generator],
            three_dimensional:bool,
            xp):
        """

        :param direction:
        :param diffuse: Controls maximum elevation |sin(θ)| ≤ diffuse.
        :param rng:
        :param three_dimensional:
        :param xp:
        :return:
        """
        direction /= xp.linalg.norm(direction, axis=-1, keepdims=True)
        num_rays = direction.shape[0]
        if three_dimensional:
            w = xp.asarray(rng.random(num_rays)) * xp.square(diffuse)
            rho = xp.asarray(rng.random(num_rays)) * 2 * xp.pi
            r = xp.sqrt(w)
            z = xp.sqrt(1 - w)
            f = xp.stack([
                r * xp.cos(rho),
                r * xp.sin(rho),
                z], axis=-1)

            direction[..., 2] -= 1
            dot = xp.sum(xp.square(direction), axis=-1)
            dot = xp.where(dot < 1e-6, 1, 2 / dot * xp.sum(direction * f, axis=-1))
            direction = f - dot[..., xp.newaxis] * direction
        else:
            w = (2 * rng.random(num_rays) - 1) * diffuse
            h = xp.sqrt(1 - xp.square(w))
            direction_orthogonal = xp.cross(direction, projection_vector)

            direction = direction * h[..., xp.newaxis] + direction_orthogonal * w[..., xp.newaxis]

            #x = direction[..., 0] * h + direction[..., 1] * w
            #y = direction[..., 1] * h - direction[..., 0] * w
            #direction[..., 0] = x
            #direction[..., 1] = y
            #direction[..., 2] *= h
        return direction


    def generate_batch(self, begin_index, end_index, num_rays, startpoints, startdirs, texture_coords, position, direction, texture_coordinate, color):
        for j in range(begin_index, end_index):
            self.generate(j, num_rays, position, direction, texture_coordinate, color)
            idx = j * 3
            direction.write(startdirs, idx)
            position.write(startpoints, idx)
            texture_coordinate.write(texture_coords, j * 2)

    def set_arcs(self, elevation, azimuth):
        self.elevation = elevation
        self.azimuth = azimuth

    def get_successor_volumes(self):
        return self.source.volume_successor

    def get_successor_surfaces(self):
        return self.source.surface_successor

    def set_source(self, optical_object):
        self.source = optical_object
        self.modCount = -1
