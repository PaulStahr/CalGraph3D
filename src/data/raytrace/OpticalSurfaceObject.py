import numpy as np
from calgraph3d.data.raytrace.OpticalObject import OpticalObject
from calgraph3d.data.raytrace.SurfaceType import SurfaceType
from calgraph3d.data.raytrace.TextureMapping import TextureMapping
from jsymmath.geometry.Geometry import Geometry
import math


class OpticalSurfaceObject(OpticalObject):
    EMPTY_SURFACE_ARRAY = []

    def __init__(self, orig=None):
        super().__init__()
        self.abbeNumber = 1
        self.conicConstant = 1
        self.direction = np.zeros(shape=3)
        self.directionNormalized = np.zeros(shape=3)
        self.surf = SurfaceType.FLAT
        self.maxRadiusGeometric = 1
        self.radiusGeometricQ = 1
        self.minRadiusGeometric = 0
        self.minRadiusGeometricQ = 0
        self.directionLength = 0
        self.directionLengthQ = 0
        self.invDirectionLengthQ = 0
        self.matGlobalToSurface = np.eye(4)
        self.matSurfaceToGlobal = np.eye(4)
        self.invDirectionLength = 0
        self.dotProdUpperBound = 0
        self.dotProdLowerBound = 0
        self.dotProdUpperBound2 = 0
        self.dotProdLowerBound2 = 0
        self.maxArcOpen = 0
        self.minArcOpen = 0
        self.textureMapping = TextureMapping.SPHERICAL
        self.alphaAsRadius = False
        self.mapLocal = True
        self.alphaAsMask = False
        if orig is not None:
            self.__dict__.update(vars(orig))

    def getMaxArcOpen(self):
        return self.maxArcOpen

    def getMinArcOpen(self):
        return self.minArcOpen

    def getDotProdUpperBound(self):
        return self.dotProdUpperBound

    def getDotProdLowerBound(self):
        return self.dotProdLowerBound

    def getTextureCoordinates(self, position, dir, out):
        if self.mapLocal:
            x, y, z = position.x, position.y, position.z
            tmp0 = self.matGlobalToSurface.rdotAffineX(x, y, z)
            tmp1 = self.matGlobalToSurface.rdotAffineY(x, y, z)
            tmp2 = self.matGlobalToSurface.rdotAffineZ(x, y, z)
            self.textureMapping.mapCartToTex(tmp0, tmp1, tmp2, out)
        else:
            direction = position - self.midpoint
            self.textureMapping.mapCartToTex(direction, out)

    def update(self):
        self.directionLengthQ = np.sum(np.square(self.direction))
        self.directionLength = np.sqrt(self.directionLengthQ)
        self.invDirectionLengthQ = 1 / self.directionLengthQ
        self.invDirectionLength = 1 / self.directionLength
        self.directionNormalized = self.direction * self.invDirectionLength
        self.updateIOR()
        self.radiusGeometricQ = self.maxRadiusGeometric * self.maxRadiusGeometric
        self.minRadiusGeometricQ = self.minRadiusGeometric * self.minRadiusGeometric
        minRatio = self.minRadiusGeometricQ * self.invDirectionLengthQ
        maxRatio = self.radiusGeometricQ * self.invDirectionLengthQ

        if self.surf == SurfaceType.CUSTOM:
            tmp = 1 - (1 + self.conicConstant) * minRatio
            self.dotProdLowerBound = minRatio / (1 + (np.sqrt(tmp) if tmp > 0 else -np.sqrt(-tmp))) - 1
            tmp = 1 - (1 + self.conicConstant) * maxRatio
            self.dotProdUpperBound = maxRatio / (1 + (np.sqrt(tmp) if tmp > 0 else -np.sqrt(max(tmp, -1)))) - 1
            if (self.dotProdUpperBound + 1) * (1 + self.conicConstant) > 2:
                self.dotProdUpperBound = 2 / (1 + self.conicConstant) - 1
        elif self.surf == SurfaceType.CYLINDER:
            self.dotProdLowerBound = -minRatio
            self.dotProdUpperBound = -maxRatio
            self.dotProdUpperBound2 = -self.maxRadiusGeometric
            self.dotProdLowerBound2 = -self.minRadiusGeometric
        elif self.surf == SurfaceType.HYPERBOLIC:
            self.dotProdLowerBound = np.sqrt(1 + minRatio) - 2
            self.dotProdUpperBound = np.sqrt(1 + maxRatio) - 2
        elif self.surf == SurfaceType.PARABOLIC:
            self.dotProdLowerBound = 0.5 * minRatio - 1
            self.dotProdUpperBound = 0.5 * maxRatio - 1
        elif self.surf == SurfaceType.SPHERICAL:
            if self.maxRadiusGeometric < self.directionLength:
                self.dotProdUpperBound = np.sqrt(1 - maxRatio)
                self.maxArcOpen = np.asin(self.maxRadiusGeometric * self.invDirectionLength)
            else:
                self.maxArcOpen = math.pi - np.asin(self.directionLength / self.maxRadiusGeometric)
                self.dotProdUpperBound = -np.sqrt(1 - 1 / maxRatio)

            if self.minRadiusGeometric < self.directionLength:
                self.minArcOpen = np.asin(self.minRadiusGeometric * self.invDirectionLength)
            else:
                self.minArcOpen = np.pi - np.asin(self.directionLength / self.minRadiusGeometric)

            self.dotProdLowerBound = np.cos(self.minArcOpen)

        # Update matrices
        self.matSurfaceToGlobal[0:3, 0:3] = Geometry.getOrthorgonalZMatrix(self.direction)
        self.matSurfaceToGlobal[0:3, 3] = self.midpoint
        self.matSurfaceToGlobal[3, 3] = 1
        self.matSurfaceToGlobal[3, 0:3] = 0
        self.matGlobalToSurface = np.linalg.inv(self.matSurfaceToGlobal)

        if self.surf != SurfaceType.CYLINDER:
            self.dotProdLowerBound2 = self.dotProdLowerBound * self.directionLength
            self.dotProdUpperBound2 = self.dotProdUpperBound * self.directionLength
            if self.surf != SurfaceType.SPHERICAL:
                self.dotProdLowerBound2 += self.directionLength
                self.dotProdUpperBound2 += self.directionLength

    def getMeshVertexCount(self, latitudes, longitudes):
        return longitudes * latitudes + (
            0 if self.surf == SurfaceType.CYLINDER or self.minRadiusGeometric > 0 else 1 - latitudes)

    def getMeshVertices(self, latitudes, longitudes):
        res = np.zeros(shape=(self.getMeshVertexCount(latitudes, longitudes), 3))
        index = 0
        z = 0
        multiply = 1. / (longitudes - 1)
        add = 0

        if self.surf == SurfaceType.FLAT:
            multiply *= (self.maxRadiusGeometric - self.minRadiusGeometric) * self.invDirectionLength
            add = self.minRadiusGeometric * self.invDirectionLength
        elif self.surf == SurfaceType.SPHERICAL:
            add = self.minArcOpen
            multiply *= self.maxArcOpen - self.minArcOpen
        elif self.surf == SurfaceType.HYPERBOLIC or self.surf == SurfaceType.PARABOLIC:
            add = self.minRadiusGeometric
            multiply *= (self.maxRadiusGeometric - self.minRadiusGeometric) * self.invDirectionLength
        elif self.surf == SurfaceType.CUSTOM:
            multiply *= self.dotProdUpperBound - self.dotProdLowerBound
            add = self.dotProdLowerBound + 1
        elif self.surf == SurfaceType.CYLINDER:
            add = self.minRadiusGeometric * self.invDirectionLength
            multiply *= (self.maxRadiusGeometric - self.minRadiusGeometric) * self.invDirectionLength
        s = math.pi * 2 / latitudes
        for ri in range(longitudes):
            r = multiply * ri + add
            if self.surf == SurfaceType.FLAT:
                pass
            elif self.surf == SurfaceType.HYPERBOLIC:
                z = 2 - math.sqrt(r * r + 1)
            elif self.surf == SurfaceType.PARABOLIC:
                z = 1 - r * r * 0.5
            elif self.surf == SurfaceType.SPHERICAL:
                z = math.cos(r)
                r = math.sin(r)
            elif self.surf == SurfaceType.CUSTOM:
                z = 1 - r
                r = math.sqrt(r * (2 - r * (1 + self.conicConstant)))
            elif self.surf == SurfaceType.CYLINDER:
                z = r
                r = 1
            else:
                raise Exception('Type unknown')

            if ri != 0 or self.surf == SurfaceType.CYLINDER or self.minRadiusGeometric > 0:
                for rhoi in range(latitudes):
                    rho = rhoi * s
                    res[index] = (self.matSurfaceToGlobal @ np.asarray((r * math.sin(rho), r * math.cos(rho), z, 1)))[
                                 0:3]
                    index += 1
            else:
                res[index] = (self.matSurfaceToGlobal @ np.asarray((0, 0, z, 1)))[0:3]
                index += 1
        return res

    def evaluate_inner_outer(self, position, normalize=True, xp=np):
        pos = position - xp.asarray(self.midpoint, dtype=position.dtype)
        surf = self.surf

        if surf == SurfaceType.FLAT:
            return -self.directionNormalized.dot(pos)
        elif surf == SurfaceType.SPHERICAL:
            return np.sum(np.square(pos), axis=-1) / self.directionLengthQ - 1
        elif surf == SurfaceType.CUSTOM:
            posdot = xp.sum(xp.square(pos), axis=-1)
            mdir = xp.inner(xp.asarray(self.directionNormalized, dtype=pos.dtype), pos)
            dirdot = self.directionLength - mdir

            res = posdot + self.conicConstant * np.square(dirdot) - self.directionLengthQ
            if normalize != False:
                div = 2 * xp.sqrt(posdot - dirdot * (
                        self.conicConstant * 2 * mdir - self.conicConstant ** 2 * dirdot))
                if normalize == 'seperate':
                    return res, div
                res /= div
            return res

        elif surf == SurfaceType.CYLINDER:
            dotprod = self.directionNormalized.dot(pos)
            dist = self.directionNormalized.distanceQ(dotprod, pos)
            return max(dotprod + self.dotProdUpperBound2,
                       max(-(self.dotProdLowerBound2 + dotprod), dist - self.directionLengthQ))
        elif surf == SurfaceType.HYPERBOLIC:
            dirdot = self.directionNormalized.dot(pos)
            return 2 * self.directionLength - dirdot - math.sqrt(
                np.square(pos) - np.square(dirdot) + self.directionLengthQ)
        elif surf == SurfaceType.PARABOLIC:
            dirdot = self.directionNormalized.dot(pos) - self.directionLength
            return np.square(pos) - np.square(dirdot) - self.directionLengthQ
        else:
            return float('nan')

    def getIntersection(self, ray_pos, ray_dir, intersection, ray_tmin, ray_tmax):
        x = ray_pos.x - self.midpoint.x
        y = ray_pos.y - self.midpoint.y
        z = ray_pos.z - self.midpoint.z

        surf = self.surf

        # Remaining implementation depends on the method implementations of this class.

    def densityCompensation(self, width, height, imageColorArray, channels, stride):
        self.textureMapping.densityCompensation(width, height, imageColorArray, channels, stride)

    def inverseDensityCompensation(self, width, height, imageColorArray, channels, stride):
        self.textureMapping.inverseDensityCompensation(width, height, imageColorArray, channels, stride)
