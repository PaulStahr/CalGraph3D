import numpy as np
from calgraph3d.data.raytrace.OpticalObject import OpticalObject
from calgraph3d.data.raytrace.SurfaceType import SurfaceType
from calgraph3d.data.raytrace.TextureMapping import TextureMapping
from calgraph3d.data.raytrace.Intersection import Intersection
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
            self.dotProdUpperBound = maxRatio / (1 + (np.sqrt(tmp) if tmp > 0 else np.sqrt(min(-tmp, 1)))) - 1
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
                self.maxArcOpen = np.arcsin(self.maxRadiusGeometric * self.invDirectionLength)
            else:
                self.maxArcOpen = math.pi - np.arcsin(self.directionLength / self.maxRadiusGeometric)
                self.dotProdUpperBound = -np.sqrt(1 - 1 / maxRatio)

            if self.minRadiusGeometric < self.directionLength:
                self.minArcOpen = np.arcsin(self.minRadiusGeometric * self.invDirectionLength)
            else:
                self.minArcOpen = np.pi - np.arcsin(self.directionLength / self.minRadiusGeometric)

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
            res = np.sum(np.square(pos), axis=-1)
            if normalize:
                np.sqrt(res, out=res)
                return res - self.directionLength
            return res - self.directionLengthQ
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

    def getIntersection(self, ray_pos, ray_dir, intersection, ray_tmin, ray_tmax, xp=np):
        xyz = ray_pos - self.midpoint

        if self.surf == SurfaceType.FLAT:
            alpha = -self.direction.dot(x, y, z) / self.direction.dot(ray_dir)
            if ray_tmin < alpha < ray_tmax:
                distanceQ = ray_dir.distanceQ(-alpha, x, y, z)
                if (self.minRadiusGeometricQ < distanceQ < self.radiusGeometricQ) != self.invertInsideOutside:
                    intersection.position.set(ray_pos, ray_dir, alpha)
                    intersection.normal.set(self.direction)
                    intersection.distance = alpha
                    intersection.object = self
                    return intersection

        elif self.surf == SurfaceType.HYPERBOLIC:
            dirproj = self.directionLength - self.directionNormalized.dot(x, y, z)
            c_a = x * x + y * y + z * z - 2 * dirproj * dirproj - self.directionLengthQ
            scal = self.directionNormalized.dot(ray_dir)
            b = ray_dir.dot(x, y, z) + 2 * scal * dirproj
            a = 1 / (1 - 2 * scal * scal)
            c_a *= a
            b *= a
            sqrt = math.sqrt(b * b - c_a)
            while sqrt >= 0:
                alpha = -b - sqrt
                if ray_tmin < alpha < ray_tmax:
                    dotProd = dirproj - scal * alpha
                    if self.dotProdLowerBound2 <= dotProd <= self.dotProdUpperBound2:
                        dax = ray_dir.x * alpha
                        day = ray_dir.y * alpha
                        daz = ray_dir.z * alpha
                        intersection.position.setAdd(ray_pos, dax, day, daz)
                        intersection.normal.set(x + dax, y + day, z + daz, self.directionNormalized, 2 * dotProd)
                        intersection.object = self
                        intersection.distance = alpha
                        return intersection
                sqrt = -sqrt

        elif self.surf == SurfaceType.PARABOLIC:
            dirproj = self.directionLength - self.directionNormalized.dot(x, y, z)
            c_a = x * x + y * y + z * z - self.directionLengthQ - dirproj * dirproj
            scal = self.directionNormalized.dot(ray_dir)
            b = ray_dir.dot(x, y, z) + scal * dirproj
            a = 1 / (1 - scal * scal)
            c_a *= a
            b *= a
            sqrt = math.sqrt(b * b - c_a)
            while sqrt >= 0:
                alpha = -b - sqrt
                if ray_tmin < alpha < ray_tmax:
                    dotProd = dirproj - scal * alpha
                    if self.dotProdLowerBound2 <= dotProd <= self.dotProdUpperBound2:
                        dax = ray_dir.x * alpha
                        day = ray_dir.y * alpha
                        daz = ray_dir.z * alpha
                        intersection.position.setAdd(ray_pos, dax, day, daz)
                        intersection.normal.set(x + dax, y + day, z + daz, self.directionNormalized, dotProd)
                        intersection.object = self
                        intersection.distance = alpha
                        return intersection
                sqrt = -sqrt
        elif self.surf == SurfaceType.CUSTOM:
            dirproj_a = self.directionLength - xp.inner(self.directionNormalized, xyz)
            c_a = xp.sum(xp.square(xyz), axis=-1) + self.conicConstant * dirproj_a * dirproj_a - self.directionLengthQ
            scal_a = xp.inner(self.directionNormalized, ray_dir)
            b_a = xp.sum(ray_dir * xyz, axis=-1) - self.conicConstant * scal_a * dirproj_a
            a_a = 1 / (1 + self.conicConstant * scal_a * scal_a)
            c_a *= a_a
            b_a *= a_a
            sqrt_a = xp.sqrt(np.square(b_a) - c_a)
            mask_b2a = np.nonzero(sqrt_a >= 0)[0]
            while True:
                alpha_b = -b_a[mask_b2a] - sqrt_a[mask_b2a]
                mask_c2b = xp.nonzero(np.logical_and(ray_tmin < alpha_b, alpha_b < ray_tmax))[0]
                if len(mask_c2b) > 0:
                    mask_c2a = mask_b2a[mask_c2b]
                    alpha_c = alpha_b[mask_c2b]
                    dotProd_c = dirproj_a[mask_c2a] - scal_a[mask_c2a] * alpha_c
                    mask_d2c = xp.nonzero(xp.logical_and(self.dotProdLowerBound2 <= dotProd_c, dotProd_c <= self.dotProdUpperBound2))[0]
                    if len(mask_d2c) > 0:
                        mask_d2a = mask_c2a[mask_d2c]
                        alpha_d = alpha_c[mask_d2c]
                        intersection.position[mask_d2a] = ray_pos[mask_d2a] + ray_dir[mask_d2a] * alpha_d[:,np.newaxis]
                        intersection.normal[mask_d2a] = intersection.position[mask_d2a] - self.midpoint[np.newaxis,:] - self.directionNormalized[np.newaxis,:] * self.conicConstant * dotProd_c[mask_d2c,np.newaxis]
                        intersection.object[mask_d2a] = self
                        intersection.distance[mask_d2a] = alpha_d
                        mask_b2a = xp.nonzero(intersection.object != self)[0]
                sqrt_a = -sqrt_a
                if not xp.any(sqrt_a[mask_b2a] < 0):
                    break
        elif self.surf == SurfaceType.SPHERICAL:
            c_a = xp.sum(xp.square(xyz), axis=-1) - self.directionLengthQ
            b_a = xp.sum(ray_dir * xyz, axis=-1)
            sqrt_a = xp.square(b_a) - c_a
            mask_b2a = np.nonzero(sqrt_a >= 0)[0]
            if len(mask_b2a) > 0:
                sqrt_b = xp.sqrt(sqrt_a[mask_b2a])
                b_b = b_a[mask_b2a]
                dirproj_b = xp.inner(self.directionNormalized, xyz[mask_b2a])
                scal_b = xp.inner(self.directionNormalized, ray_dir[mask_b2a])
                mask_c2b = np.arange(len(mask_b2a),dtype=int)
                while True:
                    alpha_c = -b_b[mask_c2b] - sqrt_b[mask_c2b]
                    mask_d2c = xp.nonzero(np.logical_and(ray_tmin < alpha_c, alpha_c < ray_tmax))[0]
                    if len(mask_d2c) > 0:
                        mask_d2b = mask_c2b[mask_d2c]
                        alpha_d = alpha_c[mask_d2c]
                        dotProd_d = dirproj_b[mask_d2b] + scal_b[mask_d2b] * alpha_d
                        mask_e2d = xp.nonzero(xp.logical_and(self.dotProdUpperBound2 <= dotProd_d, dotProd_d <= self.dotProdLowerBound2))[0]
                        if len(mask_e2d) > 0:
                            alpha_e = alpha_d[mask_e2d]
                            mask_e2a = mask_b2a[mask_d2b[mask_e2d]]
                            da_e = ray_dir[mask_e2a] * alpha_e[:,np.newaxis]
                            intersection.position[mask_e2a] = ray_pos[mask_e2a] + da_e
                            intersection.normal[mask_e2a] = xyz[mask_e2a] + da_e
                            intersection.object[mask_e2a] = self
                            intersection.distance[mask_e2a] = alpha_e
                            mask_c2b = xp.nonzero(intersection.object[mask_b2a] != self)[0]
                    sqrt_b = -sqrt_b
                    if not xp.any(sqrt_b[mask_c2b] < 0):
                        break
        elif self.surf == SurfaceType.CYLINDER:
            dirproj = -self.directionNormalized.dot(x, y, z)
            q = x * x + y * y + z * z - dirproj * dirproj - self.directionLengthQ
            scal = self.directionNormalized.dot(ray_dir)
            p = ray_dir.dot(x, y, z) + dirproj * scal
            a = 1 / (1 - scal * scal)
            q *= a
            p *= a
            sqrt = math.sqrt(p * p - q)
            while sqrt >= 0:
                ray_t = -p - sqrt
                if ray_tmin < ray_t < ray_tmax:
                    dotProd = dirproj - scal * ray_t
                    if dotProd >= self.dotProdUpperBound2 and dotProd <= self.dotProdLowerBound2:
                        intersection.position.set(ray_pos, ray_dir, ray_t)
                        intersection.normal.set(intersection.position, self.midpoint, self.directionNormalized,
                                                dotProd)
                        intersection.object = self
                        intersection.distance = ray_t
                        return intersection
                sqrt = -sqrt
        return None

    def densityCompensation(self, width, height, imageColorArray, channels, stride):
        self.textureMapping.densityCompensation(width, height, imageColorArray, channels, stride)

    def inverseDensityCompensation(self, width, height, imageColorArray, channels, stride):
        self.textureMapping.inverseDensityCompensation(width, height, imageColorArray, channels, stride)
