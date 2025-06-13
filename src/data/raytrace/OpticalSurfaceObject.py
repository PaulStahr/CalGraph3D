import numpy as np
from networkx.algorithms.operators.binary import intersection

from calgraph3d.data.raytrace.OpticalObject import OpticalObject
from calgraph3d.data.raytrace.SurfaceType import SurfaceType
from calgraph3d.data.raytrace.TextureMapping import TextureMapping
from calgraph3d.opengl import BufferUtils
from jsymmath.geometry.Geometry import Geometry
from jsymmath.util import ArrayUtil
from calgraph3d.data.raytrace.Intersection import Intersection
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

    def getTextureCoordinates(self, positions:np.ndarray):
        if self.mapLocal:
            ones = np.ones(shape=(*positions.shape[0:-1],1))
            hom_positions = np.concatenate((positions, ones), axis=-1)
            positions = (hom_positions @  self.matGlobalToSurface.T)[...,:3]
            return self.textureMapping.mapCartToTex(positions)
        else:
            direction = position - self.midpoint
            return self.textureMapping.mapCartToTex(direction)

    def update(self):
        self.directionLengthQ = np.sum(np.square(self.direction))
        self.directionLength = np.sqrt(self.directionLengthQ)
        self.invDirectionLengthQ = 1 / self.directionLengthQ
        self.invDirectionLength = 1 / self.directionLength
        self.directionNormalized = self.direction * self.invDirectionLength
        self.updateIOR()
        self.radiusGeometricQ = self.maxRadiusGeometric ** 2
        self.minRadiusGeometricQ = self.minRadiusGeometric ** 2
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

    def getMesh(self, latitudes=32, longitudes=16):
        return self.getMeshVertices(latitudes, longitudes), self.getMeshFaces(latitudes, longitudes)

    def getMeshVertices(self, latitudes, longitudes):
        multiply = 1. / (longitudes - 1)

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
        else:
            raise Exception
        rho = np.arange(latitudes) * math.pi * 2 / latitudes
        r = multiply * np.arange(longitudes) + add
        if self.surf == SurfaceType.FLAT:
            z = 0
        elif self.surf == SurfaceType.HYPERBOLIC:
            z = 2 - np.sqrt(r ** 2 + 1)
        elif self.surf == SurfaceType.PARABOLIC:
            z = 1 - r ** 2 * 0.5
        elif self.surf == SurfaceType.SPHERICAL:
            z = np.cos(r)
            r = np.sin(r)
        elif self.surf == SurfaceType.CUSTOM:
            z = 1 - r
            r = np.sqrt(r * (2 - r * (1 + self.conicConstant)))
        elif self.surf == SurfaceType.CYLINDER:
            z = r
            r = np.ones_like(r)
        else:
            raise Exception('Type unknown')
        index = 0
        res = np.empty(shape=(self.getMeshVertexCount(latitudes, longitudes), 4))
        for ri in range(longitudes):
            if ri != 0 or self.surf == SurfaceType.CYLINDER or self.minRadiusGeometric > 0:
                res[index: index + latitudes] = np.stack((
                    r[ri] * np.sin(rho),
                    r[ri] * np.cos(rho),
                    np.full(shape=latitudes, fill_value=z[ri]),
                    np.full(shape=latitudes, fill_value=1)), axis=-1)
                index += latitudes
            else:
                res[index] = np.asarray((0, 0, z[ri], 1))
                index += 1
        res = (res @ self.matSurfaceToGlobal.T)[:,0:3]
        return res

    def getMeshFaces(self, latitudes, longitudes):
        if self.surf == SurfaceType.CYLINDER or self.minRadiusGeometric > 0:
            return BufferUtils.fillWithCylinderIndexData(latitudes, longitudes)
        return BufferUtils.fillWithRadialIndexData(latitudes, longitudes)

    @staticmethod
    def numerical_derivative(function, delta):
        def derivative(x, *args, **vargs):
            num_points, dim = x.shape
            testpoints = np.repeat(x[np.newaxis], dim * 2 + 1, axis=0)
            dim = x.shape[1]
            for i in range(dim):
                testpoints[i * 2 + 1, :, i] -= delta
                testpoints[i * 2 + 2, :, i] += delta
            testpoints = testpoints.reshape(((dim * 2 + 1) * num_points, dim))
            evaluated = function(testpoints, *args, **vargs)
            evaluated = evaluated.reshape((dim * 2 + 1, num_points))
            derivative = (evaluated[2::2] - evaluated[1::2]) / (2 * delta)
            return evaluated[0], derivative.T
        return derivative

    def evaluate_inner_outer(self, position, normalize=False, xp=np):
        pos = position - xp.asarray(self.midpoint, dtype=position.dtype)

        match self.surf:
            case SurfaceType.FLAT:
                result = -xp.inner(pos, self.directionNormalized)
                if normalize == 'seperate':
                    derivative = np.repeat(-self.directionNormalized[np.newaxis, :], repeats=np.prod(pos.shape[:-1]), axis=0)
                    return result, derivative.reshape(pos.shape[:-1] + (3,))
                return result
            case SurfaceType.SPHERICAL:
                res = np.sum(np.square(pos), axis=-1)
                if normalize:
                    if normalize == 'seperate':
                        return res - self.directionLengthQ, pos * 2
                    np.sqrt(res, out=res)
                    return res - self.directionLength
                return res - self.directionLengthQ
            case SurfaceType.CUSTOM:
                posdot = xp.sum(xp.square(pos), axis=-1)
                mdir = xp.inner(xp.asarray(self.directionNormalized, dtype=pos.dtype), pos)
                dirdot = self.directionLength - mdir

                res = posdot + self.conicConstant * np.square(dirdot) - self.directionLengthQ
                if normalize != False:
                    div = 2 * (pos - self.conicConstant * dirdot[:, None] * self.directionNormalized[None, :])
                    if normalize == 'seperate':
                        return res, div
                    res /= np.linalg.norm(div, axis=-1)
                return res
            case SurfaceType.CYLINDER:
                dotprod = self.directionNormalized.dot(pos)
                dist = self.directionNormalized.distanceQ(dotprod, pos)
                return max(dotprod + self.dotProdUpperBound2,
                           max(-(self.dotProdLowerBound2 + dotprod), dist - self.directionLengthQ))
            case SurfaceType.HYPERBOLIC:
                dirdot = xp.inner(self.directionNormalized, pos)
                return 2 * self.directionLength - dirdot - xp.sqrt(
                    xp.sum(xp.square(pos), axis=-1) - xp.square(dirdot) + self.directionLengthQ)
            case SurfaceType.PARABOLIC:
                dirdot = xp.inner(self.directionNormalized, pos) - self.directionLength
                return xp.sum(xp.square(pos), axis=-1) - xp.square(dirdot) - self.directionLengthQ
            case _:
                raise Exception(f'Unknown surface type: {self.surf}')

    def getIntersection(self, ray_pos:np.ndarray, ray_dir:np.ndarray, intersection:Intersection, ray_tmin:np.ndarray, ray_tmax:np.ndarray, xp=np):
        shape = ray_pos.shape[:-1]
        update_mask = xp.zeros(shape=shape, dtype=bool)
        xyz = ray_pos - ArrayUtil.convert(self.midpoint, xp)
        directionNormalized = ArrayUtil.convert(self.directionNormalized, xp)
        midpoint = ArrayUtil.convert(self.midpoint, xp)

        if self.surf == SurfaceType.FLAT:
            alpha = -xp.inner(self.direction, xyz) / xp.inner(self.direction, ray_dir)
            if ray_tmin < alpha < ray_tmax:
                distanceQ = ray_dir.distanceQ(-alpha, xyz)
                if (self.minRadiusGeometricQ < distanceQ < self.radiusGeometricQ) != self.invertInsideOutside:
                    intersection.position.set(ray_pos, ray_dir, alpha)
                    intersection.normal.set(self.direction)
                    intersection.distance = alpha
                    intersection.object = self.id
                    return intersection

        elif self.surf == SurfaceType.HYPERBOLIC:
            dirproj = self.directionLength - xp.inner(directionNormalized, xyz)
            c_a = xp.sum(xp.square(xyz), axis=-1) - 2 * xp.square(dirproj) - self.directionLengthQ
            scal_a = xp.inner(directionNormalized, ray_dir)
            b_a = xp.sum(ray_dir * xyz, axis=-1) + 2 * scal_a * dirproj
            a_a = 1 / (1 - 2 * xp.square(scal_a))
            c_a *= a_a
            b_a *= a_a
            sqrt_a = xp.sqrt(xp.square(b_a) - c_a)
            mask_b2a = xp.nonzero(sqrt_a >= 0)[0]
            for i in range(2):
                if len(mask_b2a) == 0:
                    break
                alpha_b = -b_a[mask_b2a] - sqrt_a[mask_b2a]
                mask_c2b = xp.nonzero(np.logical_and(ray_tmin[mask_b2a] < alpha_b, alpha_b < ray_tmax[mask_b2a]))[0]
                if len(mask_c2b) > 0:
                    mask_c2a = mask_b2a[mask_c2b]
                    alpha_c = alpha_b[mask_c2b]
                    dotProd_c = dirproj[mask_c2a] - scal_a[mask_c2a] * alpha_c
                    mask_d2c = xp.nonzero(xp.logical_and(self.dotProdLowerBound2 <= dotProd_c, dotProd_c <= self.dotProdUpperBound2))[0]
                    if len(mask_d2c) > 0:
                        mask_d2a = mask_c2a[mask_d2c]
                        alpha_d = alpha_c[mask_d2c]
                        da_d = ray_dir[mask_d2a] * alpha_d[:, np.newaxis]
                        intersection.position[mask_d2a] = ray_pos[mask_d2a] + da_d
                        intersection.normal[mask_d2a] = xyz[mask_d2a] + da_d + directionNormalized[np.newaxis,...] * (2 * dotProd_c[mask_d2c][...,np.newaxis])
                        intersection.object[mask_d2a] = self
                        intersection.distance[mask_d2a] = alpha_d
                        update_mask[mask_d2a] = True
                        mask_b2a = xp.nonzero(xp.isfinite(sqrt_a) & ~update_mask)[0]
                sqrt_a = -sqrt_a
                if not xp.any(sqrt_a[mask_b2a] < 0):
                    break
            return update_mask
        elif self.surf == SurfaceType.PARABOLIC:
            dirproj_a = self.directionLength - xp.inner(directionNormalized, xyz)
            c_a = xp.sum(xp.square(xyz), axis=-1) - np.square(dirproj_a) - self.directionLengthQ
            scal_a = xp.inner(directionNormalized, ray_dir)
            b_a = xp.sum(ray_dir * xyz, axis=-1) + scal_a * dirproj_a
            a_a = 1 / (1 - xp.square(scal_a))
            c_a *= a_a
            b_a *= a_a
            sqrt_a = xp.sqrt(xp.square(b_a) - c_a)
            mask_b2a = xp.nonzero(sqrt_a >= 0)[0]
            for i in range(2):
                if len(mask_b2a) == 0:
                    break
                alpha_b = -b_a[mask_b2a] - sqrt_a[mask_b2a]
                mask_c2b = xp.nonzero((ray_tmin[mask_b2a] < alpha_b) & (alpha_b < ray_tmax[mask_b2a]))[0]
                if len(mask_c2b) > 0:
                    mask_c2a = mask_b2a[mask_c2b]
                    alpha_c = alpha_b[mask_c2b]
                    dotProd_c = dirproj_a[mask_c2a] - scal_a[mask_c2a] * alpha_c
                    mask_d2c = xp.nonzero((self.dotProdLowerBound2 <= dotProd_c) & (dotProd_c <= self.dotProdUpperBound2))[0]
                    if len(mask_d2c) > 0:
                        mask_d2a = mask_c2a[mask_d2c]
                        alpha_d = alpha_c[mask_d2c]
                        intersection.position[mask_d2a] = ray_pos[mask_d2a] + ray_dir[mask_d2a] * alpha_d[:,np.newaxis]
                        intersection.normal[mask_d2a] = xyz[mask_d2a] + ray_dir[mask_d2a] * alpha_d[:,np.newaxis]
                        intersection.object[mask_d2a] = self.id
                        intersection.distance[mask_d2a] = alpha_d
                        update_mask[mask_d2a] = True
                        mask_b2a = xp.nonzero(xp.isfinite(sqrt_a) & ~update_mask)[0]
                sqrt_a = -sqrt_a
            return update_mask
        elif self.surf == SurfaceType.CUSTOM:
            dirproj_a = self.directionLength - xp.inner(directionNormalized, xyz)
            c_a = xp.sum(xp.square(xyz), axis=-1) + self.conicConstant * xp.square(dirproj_a) - self.directionLengthQ
            scal_a = xp.inner(directionNormalized, ray_dir)
            b_a = xp.sum(ray_dir * xyz, axis=-1) - self.conicConstant * scal_a * dirproj_a
            a_a = 1 / (1 + self.conicConstant * xp.square(scal_a))
            c_a *= a_a
            b_a *= a_a
            sqrt_a = xp.sqrt(np.square(b_a) - c_a)
            mask_b2a = np.nonzero(xp.isfinite(sqrt_a))[0]
            for i in range(2):
                if len(mask_b2a) == 0:
                    break
                alpha_b = -b_a[mask_b2a] - sqrt_a[mask_b2a]
                mask_c2b = xp.nonzero((ray_tmin[mask_b2a] < alpha_b) & (alpha_b < ray_tmax[mask_b2a]))[0]
                if len(mask_c2b) > 0:
                    mask_c2a = mask_b2a[mask_c2b]
                    alpha_c = alpha_b[mask_c2b]
                    dotProd_c = dirproj_a[mask_c2a] - scal_a[mask_c2a] * alpha_c
                    mask_d2c = xp.nonzero((self.dotProdLowerBound2 <= dotProd_c) & (dotProd_c <= self.dotProdUpperBound2))[0]
                    if len(mask_d2c) > 0:
                        mask_d2a = mask_c2a[mask_d2c]
                        alpha_d = alpha_c[mask_d2c]
                        intersection.position[mask_d2a] = ray_pos[mask_d2a] + ray_dir[mask_d2a] * alpha_d[:,np.newaxis]
                        intersection.normal[mask_d2a] = intersection.position[mask_d2a] - midpoint[np.newaxis,:] - directionNormalized[np.newaxis,:] * self.conicConstant * dotProd_c[mask_d2c,np.newaxis]
                        intersection.object[mask_d2a] = self.id
                        intersection.distance[mask_d2a] = alpha_d
                        update_mask[mask_d2a] = True
                        mask_b2a = xp.nonzero(xp.isfinite(sqrt_a) & ~update_mask)[0]
                sqrt_a = -sqrt_a
            return update_mask
        elif self.surf == SurfaceType.SPHERICAL:
            c_a = xp.sum(xp.square(xyz), axis=-1) - self.directionLengthQ
            b_a = xp.sum(ray_dir * xyz, axis=-1)
            sqrt_a = xp.square(b_a) - c_a
            mask_b2a = np.nonzero(sqrt_a >= 0)[0]
            if len(mask_b2a) > 0:
                sqrt_b = xp.sqrt(sqrt_a[mask_b2a])
                b_b = b_a[mask_b2a]
                dirproj_b = xp.inner(directionNormalized, xyz[mask_b2a])
                scal_b = xp.inner(directionNormalized, ray_dir[mask_b2a])
                mask_c2b = xp.arange(len(mask_b2a),dtype=int)
                for i in range(2):
                    if len(mask_c2b) == 0:
                        break
                    alpha_c = -b_b[mask_c2b] - sqrt_b[mask_c2b]
                    mask_c2a = mask_b2a[mask_c2b]
                    mask_d2c = xp.nonzero(np.logical_and(ray_tmin[mask_c2a] < alpha_c, alpha_c < ray_tmax[mask_c2a]))[0]
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
                            intersection.object[mask_e2a] = self.id
                            intersection.distance[mask_e2a] = alpha_e
                            update_mask[mask_e2a] = True
                            mask_c2b = xp.nonzero(xp.isfinite(sqrt_b) & ~update_mask[mask_b2a])[0]
                    sqrt_b = -sqrt_b
            return update_mask
        elif self.surf == SurfaceType.CYLINDER:
            dirproj = -xp.inner(directionNormalized, xyz)
            q = xp.sum(xp.square(xyz), axis=-1) - xp.square(dirproj) - self.directionLengthQ
            scal = directionNormalized.dot(ray_dir)
            p = xp.sum(ray_dir * xyz, axis=-1) + dirproj * scal
            a_a = 1 / (1 - scal * scal)
            q *= a_a
            p *= a_a
            sqrt = xp.sqrt(p * p - q)
            while sqrt >= 0:
                ray_t = -p - sqrt
                if ray_tmin < ray_t < ray_tmax:
                    dotProd = dirproj - scal * ray_t
                    if dotProd >= self.dotProdUpperBound2 and dotProd <= self.dotProdLowerBound2:
                        intersection.position.set(ray_pos, ray_dir, ray_t)
                        intersection.normal.set(intersection.position, midpoint, directionNormalized,
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
