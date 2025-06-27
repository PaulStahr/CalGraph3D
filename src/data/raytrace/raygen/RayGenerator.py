import math
import random
from typing import Optional, List
from calgraph3d.data.raytrace.OpticalSurfaceObject import OpticalSurfaceObject
from calgraph3d.data.raytrace.MeshObject import MeshObject
from calgraph3d.data.raytrace.SurfaceType import SurfaceType
from jsymmath.geometry.Geometry import Geometry
from jsymmath.util import ArrayUtil

import numpy as np


class RayGenerator:
    def __init__(self, gen = None):
        self.source = None
        self.modCount = -1
        self.threeDimensional = False
        self.v0:np.ndarray = np.zeros(3)
        self.v1:np.ndarray = np.zeros(3)
        self.arc_open = 0.0
        self.cos_arc_open = 0.0
        self.radius_radius_geom_ratio = 0.0
        self.elevation = math.nan
        self.azimuth = math.nan
        self.rng = np.random.default_rng()
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

            if surf.surf == SurfaceType.FLAT:
                if self.threeDimensional:
                    self.v0 = Geometry.getOrthogonalVector(direction)
                    self.v1 = np.cross(direction, self.v0)
                    self.v1 *= surf.maxRadiusGeometric / np.linalg.norm(self.v1)
                else:
                    self.v0 = direction
                    self.v0 = np.array([self.v0[1], -self.v0[0], 0])  # Rotate 90 degrees in 2D
                self.v0 *= surf.maxRadiusGeometric / np.linalg.norm(self.v0)

            elif surf.surf == SurfaceType.SPHERICAL:
                if surf.maxRadiusGeometric / radius < 1:
                    self.arc_open = math.asin(surf.maxRadiusGeometric / radius)
                else:
                    self.arc_open = math.pi - math.asin(self.radius_radius_geom_ratio)

                if self.threeDimensional:
                    self.v0 = Geometry.getOrthogonalVector(direction)
                    self.v1 = np.cross(direction, self.v0)
                    self.v1 *= radius / np.linalg.norm(self.v1)
                else:
                    self.v0 = direction
                    self.v0 = np.array([self.v0[1], -self.v0[0], 0])
                self.v0 *= radius / np.linalg.norm(self.v0)

            self.cos_arc_open = 1 - math.cos(self.arc_open)

    def generate(self, num_rays:int, xp=np):
        if  self.source.modCount != self.modCount:
            self.init()
            self.modCount = self.source.modCount

        diffuse = 0
        if isinstance(self.source, OpticalSurfaceObject):
            surf = self.source
            diffuse = surf.diffuse

            if surf.surf == SurfaceType.FLAT:
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

            elif surf.surf == SurfaceType.SPHERICAL:
                direction = ArrayUtil.convert(surf.direction,xp)[xp.newaxis,...]
                direction = xp.repeat(direction, num_rays, axis=0)
                while True:
                    if self.threeDimensional:
                        elevation = self.elevation if not math.isnan(self.elevation) else (xp.arccos(1 - xp.asarray(self.rng.random(num_rays)) * self.cos_arc_open))[..., xp.newaxis]
                        azimuth = self.azimuth if not math.isnan(self.azimuth) else (xp.asarray(self.rng.random(num_rays)) * (2 * math.pi))[..., xp.newaxis]
                        sin = xp.sin(elevation)
                        direction = (direction * xp.cos(elevation)
                                     + ArrayUtil.convert(self.v0[xp.newaxis, ...], xp) * sin * xp.sin(azimuth)
                                     + ArrayUtil.convert(self.v1[xp.newaxis, ...], xp) * sin * xp.cos(azimuth))
                    else:
                        alpha = 0 if num_rays <= 1 else xp.linspace(-1,1,num_rays) * self.arc_open
                        direction = surf.direction * xp.cos(alpha) + self.v0 * xp.sin(alpha)

                    position = xp.asarray(surf.midpoint[xp.newaxis, ...]) + direction

                    if False:# or surf.alpha_as_radius:
                        color = surf.read_color(position, direction)
                        dist = color[3] * 10
                        diffuse *= math.sqrt(1 / dist)
                        position = surf.midpoint + direction * dist
                    break
            else:
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
            direction /= xp.linalg.norm(direction, axis=-1, keepdims=True)
            if self.threeDimensional or isinstance(self.source, MeshObject):
                w = xp.asarray(self.rng.random(num_rays)) * xp.square(diffuse)
                rho = xp.asarray(self.rng.random(num_rays)) * 2 * xp.pi
                f = xp.stack([xp.sqrt(w) * xp.cos(rho), xp.sqrt(w) * xp.sin(rho), xp.sqrt(1 - w)], axis=-1)
                direction[...,2] -= 1
                dot = xp.sum(xp.square(direction), axis=-1)
                dot = xp.where(dot < 1e-6, 1, 2 / dot * xp.sum(direction * f, axis=-1))
                direction = f - dot[..., xp.newaxis] * direction
            else:
                w = (2 * self.rng.random(num_rays) - 1) * diffuse
                h = math.sqrt(1 - xp.square(w))
                x = direction[...,0] * h + direction[...,1] * w
                y = direction[...,1] * h - direction[...,0] * w
                direction[..., 0] = x
                direction[..., 1] = y
                direction[..., 2] *= h
        return position, direction

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
