import numpy as np

from calgraph3d.data.raytrace.OpticalObject import OpticalObject
from calgraph3d.data.raytrace.OpticalVolumeObject import OpticalVolumeObject
from calgraph3d.data.raytrace.SurfaceType import SurfaceType
from calgraph3d.data.raytrace.TextureMapping import TextureMapping
from calgraph3d.data.raytrace.TextureObject import TextureObject
from calgraph3d.opengl import BufferUtils
from jsymmath.geometry.Geometry import Geometry
from jsymmath.util import ArrayUtil
import numbers
from calgraph3d.data.raytrace.FaceDirection import FaceDirection
from calgraph3d.data.raytrace.Intersection import Intersection
import math
from enum import Enum

class BooleanModifier(Enum):
    INTERSECT = 1
    DIFFERENCE = 2


class OpticalModifier:
    def __init__(self, boolean_type: BooleanModifier, object: OpticalObject):
        self.boolean_type = boolean_type
        self.object = object

    def get_boolean_mask(self, positions:np.ndarray, xp=np):
        inner_outer = self.object.evaluate_inner_outer(positions, xp=xp)
        match self.boolean_type:
            case BooleanModifier.INTERSECT:
                return inner_outer <= 0
            case BooleanModifier.DIFFERENCE:
                return inner_outer > 0
            case _:
                raise Exception(f'Unknown boolean type: {self.boolean_type}')

    @staticmethod
    def get_stack_mask(stack, positions:np.ndarray, xp=np):
        bmask = xp.ones(shape=positions.shape[:-1], dtype=bool)
        for m in stack:
            bmask &= m.get_boolean_mask(positions, xp)
        return bmask

class OpticalSurfaceObject(OpticalObject):
    EMPTY_SURFACE_ARRAY = []

    def __init__(self, orig=None, label=None):
        super().__init__(label=label)
        self.abbeNumber = 1
        self.conicConstant = 1
        self.direction = np.zeros(shape=3)
        self.directionNormalized = np.zeros(shape=3)
        self.surf:SurfaceType = SurfaceType.FLAT
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
        self.faceDirection:FaceDirection = FaceDirection.BOTH
        self.maxArcOpen = 0
        self.minArcOpen = 0
        self.textureMapping:TextureMapping = TextureMapping.SPHERICAL
        self.testAlpha = False
        self.texture:TextureObject|None = None
        self.alphaTexture:TextureObject|None = None
        self.alphaAsRadius:bool = False
        self.mapLocal = True
        self.diffuse:float = 0
        self.alphaAsMask = False
        self.boolean_modifiers = []
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

    def getTextureCoordinates(self, positions:np.ndarray, xp=np):
        if self.mapLocal:
            matGlobalToSurface = ArrayUtil.convert(self.matGlobalToSurface, xp)
            positions = (positions @ matGlobalToSurface.T[0:3,0:3]) + matGlobalToSurface[3,0:3]
            return self.textureMapping.mapCartToTex(positions)
        else:
            direction = positions - ArrayUtil.convert(self.midpoint, xp)
            return self.textureMapping.mapCartToTex(direction)

    def setRadius(self, minRadiusGeometric=None, maxRadiusGeometric=None):
        if minRadiusGeometric is not None:
            self.minRadiusGeometric = minRadiusGeometric
        if maxRadiusGeometric is not None:
            self.maxRadiusGeometric = maxRadiusGeometric
        self.update()

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

    def getZFromR(self, r):
        r /= self.directionLength
        if self.surf == SurfaceType.FLAT:
            return 0
        elif self.surf == SurfaceType.HYPERBOLIC:
            z = 2 - np.sqrt(r ** 2 + 1)
        elif self.surf == SurfaceType.PARABOLIC:
            z = 1 - r ** 2 * 0.5
        elif self.surf == SurfaceType.SPHERICAL:
            z = np.sqrt(1 - r ** 2)
        elif self.surf == SurfaceType.CUSTOM:
            z = (self.conicConstant - np.sqrt(1 - (1 + self.conicConstant) * r**2)) / (1 + self.conicConstant)
        elif self.surf == SurfaceType.CYLINDER:
            z = 1
        else:
            raise Exception('Type unknown')
        z *= self.directionLength
        return z

    def getMeshVertices(self, latitudes, longitudes):
        if self.surf == SurfaceType.FLAT:
            low = self.minRadiusGeometric * self.invDirectionLength
            high = self.maxRadiusGeometric * self.invDirectionLength
        elif self.surf == SurfaceType.SPHERICAL:
            low = self.minArcOpen
            high = self.maxArcOpen
        elif self.surf == SurfaceType.HYPERBOLIC or self.surf == SurfaceType.PARABOLIC:
            low = self.minRadiusGeometric
            high = self.maxRadiusGeometric
        elif self.surf == SurfaceType.CUSTOM:
            low = self.dotProdLowerBound + 1
            high = self.dotProdUpperBound + 1
        elif self.surf == SurfaceType.CYLINDER:
            low = self.minRadiusGeometric * self.invDirectionLength
            high = self.maxRadiusGeometric * self.invDirectionLength
        else:
            raise Exception
        rho = np.linspace(0, 2 * math.pi, latitudes, endpoint=False)
        t = np.linspace(low, high, longitudes, endpoint=True)
        if self.surf == SurfaceType.FLAT:
            z = np.zeros(shape=longitudes)
            r = t
        elif self.surf == SurfaceType.HYPERBOLIC:
            z = 2 - np.sqrt(t ** 2 + 1)
            r = t
        elif self.surf == SurfaceType.PARABOLIC:
            z = 1 - t ** 2 * 0.5
            r = t
        elif self.surf == SurfaceType.SPHERICAL:
            z = np.cos(t)
            r = np.sin(t)
        elif self.surf == SurfaceType.CUSTOM:
            z = 1 - t
            r = np.sqrt(t * (2 - t * (1 + self.conicConstant)))
        elif self.surf == SurfaceType.CYLINDER:
            z = t
            r = np.ones_like(t)
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

    def evaluate_inner_outer(self, position, normalize:bool|str=False, xp=np):
        pos = position - xp.asarray(self.midpoint, dtype=position.dtype)

        match self.surf:
            case SurfaceType.FLAT:
                result = -xp.inner(pos, self.directionNormalized)
                if normalize == 'seperate':
                    derivative = xp.repeat(-self.directionNormalized[np.newaxis, :], repeats=np.prod(pos.shape[:-1]), axis=0)
                    return result, derivative.reshape(pos.shape[:-1] + (3,))
                return result
            case SurfaceType.SPHERICAL:
                res = xp.sum(np.square(pos), axis=-1)
                if normalize:
                    if normalize == 'seperate':
                        return res - self.directionLengthQ, pos * 2
                    xp.sqrt(res, out=res)
                    return res - self.directionLength
                return res - self.directionLengthQ
            case SurfaceType.CUSTOM:
                posdot = xp.sum(xp.square(pos), axis=-1)
                directionNormalized = ArrayUtil.convert(self.directionNormalized, xp)
                mdir = xp.inner(directionNormalized, pos)
                dirdot = self.directionLength - mdir

                res = posdot + self.conicConstant * xp.square(dirdot) - self.directionLengthQ
                if normalize != False:
                    div = 2 * (pos - (self.conicConstant * dirdot)[..., None] * directionNormalized[*([None]*len(dirdot.shape)), :])
                    if normalize == 'seperate':
                        return res, div
                    res /= np.linalg.norm(div, axis=-1)
                return res
            case SurfaceType.CYLINDER:
                dotprod = self.directionNormalized.dot(pos)
                dist = self.directionNormalized.distanceQ(dotprod, pos)
                return xp.maximum(dotprod + self.dotProdUpperBound2,
                           xp.maximum(-(self.dotProdLowerBound2 + dotprod), dist - self.directionLengthQ))
            case SurfaceType.HYPERBOLIC:
                dirdot = xp.inner(self.directionNormalized, pos)
                if normalize != False:
                    raise NotImplementedError("Normalization for hyperbolic surfaces is not implemented.")
                return 2 * self.directionLength - dirdot - xp.sqrt(
                    xp.sum(xp.square(pos), axis=-1) - xp.square(dirdot) + self.directionLengthQ)
            case SurfaceType.PARABOLIC:
                dirdot = xp.inner(self.directionNormalized, pos) - self.directionLength
                return xp.sum(xp.square(pos), axis=-1) - xp.square(dirdot) - self.directionLengthQ
            case _:
                raise Exception(f'Unknown surface type: {self.surf}')

    def getPlaneIntersection(self,
                             position:np.ndarray,
                             normal:np.ndarray,
                             num_points:int) -> list[np.ndarray]:
        #return points (num_connected_lines x num_points x 3) that are defined by the intersection of the plane and the optical object
        if self.surf == SurfaceType.FLAT:
            result = []
            # intersect both planes, pay attention to minRadiusGeometric and maxRadiusGeometric. We ignore num_points in this case and use two points per line
            dirNormalized = self.directionNormalized
            planeNormal = normal / np.linalg.norm(normal)
            if np.abs(np.dot(dirNormalized, planeNormal)) < 1e-6:
                #if we hit the plane then we have to return a full circle with maxRadiusGeometric
                if np.abs(np.dot(planeNormal, position - self.midpoint)) < 1e-6:
                    angles = np.linspace(0, 2 * np.pi, num_points, endpoint=True)
                    result.append(self.midpoint + self.maxRadiusGeometric * (np.cos(angles)[:, None] * Geometry.getOrthogonalVector(dirNormalized) + np.sin(angles)[:, None] * np.cross(dirNormalized, Geometry.getOrthogonalVector(dirNormalized))))
                    if self.minRadiusGeometric > 0:
                        result.append(self.midpoint + self.minRadiusGeometric * (np.cos(angles)[:, None] * Geometry.getOrthogonalVector(dirNormalized) + np.sin(angles)[:, None] * np.cross(dirNormalized, Geometry.getOrthogonalVector(dirNormalized))))
            else:
                # Compute intersection point of infinite planes
                d = np.dot(dirNormalized, self.midpoint)
                t = (d - np.dot(dirNormalized, position)) / np.dot(dirNormalized, planeNormal)
                intersection_point = position + t * planeNormal

                # Build orthonormal basis in intersection plane
                v1 = np.cross(dirNormalized, planeNormal)
                v1_norm = np.linalg.norm(v1)
                if v1_norm < 1e-12:
                    v1 = Geometry.getOrthogonalVector(planeNormal)
                    v1 = v1 / np.linalg.norm(v1)
                else:
                    v1 = v1 / v1_norm

                v2 = np.cross(planeNormal, v1)
                v2 = v2 / np.linalg.norm(v2)

                # Vectorized angles
                angles = np.linspace(0, 2 * np.pi, num_points, endpoint=False)
                cos_a = np.cos(angles)[:, None]  # shape (num_points, 1)
                sin_a = np.sin(angles)[:, None]

                # Vectorized circles (max radius)
                circle_max = (intersection_point
                              + self.maxRadiusGeometric * (cos_a * v1 + sin_a * v2))
                result.append(circle_max)

                # Inner circle (min radius)
                if self.minRadiusGeometric > 0:
                    circle_min = (intersection_point
                                  + self.minRadiusGeometric * (cos_a * v1 + sin_a * v2))
                    result.append(circle_min)
            return result
        if self.surf == SurfaceType.SPHERICAL:
            pass


    def getColor(self, positions:np.ndarray, xp=np):
        result = ArrayUtil.convert(self.color, xp)
        if self.texture is not None:
            texcoords = self.getTextureCoordinates(positions, xp)
            tCol = self.texture.getColor(texcoords, xp)
            if result is not None:
                result = result * tCol
            else:
                result = tCol
        return ArrayUtil.convert(result, xp)


    def getIntersection(self,
                        ray_pos:np.ndarray,
                        ray_dir:np.ndarray,
                        intersection:Intersection,
                        ray_tmin:np.ndarray,
                        ray_tmax:np.ndarray,
                        xp=np):
        shape = ray_pos.shape[:-1]
        update_mask = xp.zeros(shape=shape, dtype=bool)
        xyz = ray_pos - ArrayUtil.convert(self.midpoint, xp)
        directionNormalized = ArrayUtil.convert(self.directionNormalized, xp)
        midpoint = ArrayUtil.convert(self.midpoint, xp)

        if self.surf == SurfaceType.FLAT:
            alpha = -xp.inner(directionNormalized, xyz) / xp.inner(directionNormalized, ray_dir)
            mask_b2a = xp.nonzero((ray_tmin < alpha) & (alpha < ray_tmax))[0]
            if len(mask_b2a) > 0:
                ray_dir_b = ray_dir[mask_b2a]
                alpha_b = alpha[mask_b2a]
                xyz_b = xyz[mask_b2a]
                ray_dir_scaled_b = ray_dir_b * alpha_b[...,xp.newaxis]
                distanceQ = xp.sum(xp.square(ray_dir_scaled_b + xyz_b), axis=-1)
                mask_c2b = xp.nonzero((self.minRadiusGeometricQ < distanceQ) & (distanceQ < self.radiusGeometricQ))[0]
                if len(mask_c2b) > 0:
                    mask_c2a = mask_b2a[mask_c2b]
                    intersection_pos_c = ray_pos[mask_c2a] + ray_dir_scaled_b[mask_c2b]
                    if len(self.boolean_modifiers) > 0:
                        mask_d2c = OpticalModifier.get_stack_mask(self.boolean_modifiers, intersection_pos_c, xp)
                        mask_d2a = mask_c2a[mask_d2c]
                        intersection_pos_d = intersection_pos_c[mask_d2c]
                    else:
                        mask_d2a = mask_c2a
                        intersection_pos_d = intersection_pos_c
                    intersection.position[mask_d2a] = intersection_pos_d
                    intersection.normal[mask_d2a] = self.direction[np.newaxis,...]
                    update_mask[mask_d2a] = True
                    intersection.distance[mask_d2a] = alpha[mask_d2a]
                    intersection.object[mask_d2a] = self.id
            return update_mask
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
            for i in (FaceDirection.FRONT, FaceDirection.BACK):
                if len(mask_b2a) == 0:
                    break
                alpha_b = -b_a[mask_b2a] - sqrt_a[mask_b2a]
                mask_c2b = xp.nonzero((ray_tmin[mask_b2a] < alpha_b) & (alpha_b < ray_tmax[mask_b2a]))[0]
                if len(mask_c2b) > 0:
                    mask_c2a = mask_b2a[mask_c2b]
                    alpha_c = alpha_b[mask_c2b]
                    dotProd_c = dirproj[mask_c2a] - scal_a[mask_c2a] * alpha_c
                    mask_d2c = xp.nonzero((self.dotProdLowerBound2 <= dotProd_c) & (dotProd_c <= self.dotProdUpperBound2))[0]
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
            mask_b2a = xp.nonzero(xp.isfinite(sqrt_a))[0]
            for i in (FaceDirection.FRONT, FaceDirection.BACK):
                if len(mask_b2a) == 0:
                    break
                if self.faceDirection == i or self.faceDirection == FaceDirection.BOTH:
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
                            ray_pos_d = ray_pos[mask_d2a]
                            ray_dir_d = ray_dir[mask_d2a]
                            intersection_pos_d = ray_pos_d + ray_dir_d * alpha_d[:,np.newaxis]
                            dotProd_d = dotProd_c[mask_d2c]

                            if len(self.boolean_modifiers) > 0:
                                mask_e2d = OpticalModifier.get_stack_mask(self.boolean_modifiers, intersection_pos_d, xp)
                                mask_e2a = mask_d2a[mask_e2d]
                                intersection_pos_e = intersection_pos_d[mask_e2d]
                                dotProd_e = dotProd_d[mask_e2d]
                                alpha_d = alpha_d[mask_e2d]
                            else:
                                mask_e2a, intersection_pos_e, dotProd_e = mask_d2a, intersection_pos_d, dotProd_d

                            intersection.position[mask_e2a] = intersection_pos_e
                            intersection.normal[mask_e2a] = intersection_pos_e - midpoint[np.newaxis,:] - (directionNormalized[np.newaxis,:] * self.conicConstant) * dotProd_e[:,np.newaxis]
                            intersection.object[mask_e2a] = self.id
                            intersection.distance[mask_e2a] = alpha_d
                            update_mask[mask_e2a] = True
                            mask_b2a = xp.nonzero(xp.isfinite(sqrt_a) & ~update_mask)[0]
                sqrt_a = -sqrt_a
            return update_mask
        elif self.surf == SurfaceType.SPHERICAL:
            c_a = xp.sum(xp.square(xyz), axis=-1) - self.directionLengthQ
            b_a = xp.sum(ray_dir * xyz, axis=-1)
            sqrt_a = xp.square(b_a) - c_a
            mask_b2a = xp.nonzero(sqrt_a >= 0)[0]
            if len(mask_b2a) > 0:
                sqrt_b = xp.sqrt(sqrt_a[mask_b2a])
                b_b = b_a[mask_b2a]
                dirproj_b = xp.inner(directionNormalized, xyz[mask_b2a])
                scal_b = xp.inner(directionNormalized, ray_dir[mask_b2a])
                mask_c2b = xp.arange(len(mask_b2a),dtype=int)
                for i in (FaceDirection.FRONT, FaceDirection.BACK):
                    if len(mask_c2b) == 0:
                        break
                    if self.faceDirection == i or self.faceDirection == FaceDirection.BOTH:
                        alpha_c = -b_b[mask_c2b] - sqrt_b[mask_c2b]
                        mask_c2a = mask_b2a[mask_c2b]
                        mask_d2c = xp.nonzero((ray_tmin[mask_c2a] < alpha_c) & (alpha_c < ray_tmax[mask_c2a]))[0]
                        if len(mask_d2c) > 0:
                            mask_d2b = mask_c2b[mask_d2c]
                            alpha_d = alpha_c[mask_d2c]
                            dotProd_d = dirproj_b[mask_d2b] + scal_b[mask_d2b] * alpha_d
                            mask_e2d = xp.nonzero((self.dotProdUpperBound2 <= dotProd_d) & (dotProd_d <= self.dotProdLowerBound2))[0]
                            if len(mask_e2d) > 0:
                                alpha_e = alpha_d[mask_e2d]
                                mask_e2a = mask_b2a[mask_d2b[mask_e2d]]
                                da_e = ray_dir[mask_e2a] * alpha_e[:,np.newaxis]
                                intersection_position = ray_pos[mask_e2a] + da_e
                                if self.alphaTexture is not None:
                                    texture_coordinates = self.getTextureCoordinates(intersection_position, xp=xp)
                                    object_color = self.alphaTexture.getColor(texture_coordinates, xp=xp)
                                    mask_f2e = xp.nonzero(object_color > 0)[0]
                                    mask_e2a = mask_e2a[mask_f2e]
                                    alpha_e = alpha_e[mask_f2e]
                                    da_e = da_e[mask_f2e]
                                    intersection_position = intersection_position[mask_f2e]
                                intersection.position[mask_e2a] = intersection_position
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

    def get_ior(
        self,
        position=None,
        non_inverted=None,
        xp=np):
        if self.iorq is not None: #Most common fastpath
            if non_inverted is None:
                return self.iorq
            return xp.where(non_inverted, self.iorq, self.inviorq)
        if isinstance(self.ior0, numbers.Number):
            ior0 = self.ior0
        elif isinstance(self.ior0, OpticalVolumeObject):
            ior0 = self.ior0.getRefractiveIndex(position)
        else:
            raise Exception(f"Unknown ior type {self.ior0}")
        if isinstance(self.ior1, numbers.Number):
            ior1 = self.ior1
        elif isinstance(self.ior1, OpticalVolumeObject):
            ior1 = self.ior1.getRefractiveIndex(position)
        else:
            raise Exception(f"Unknown ior type {self.ior1}")
        iorq = ior0 / ior1
        iorq = xp.square(iorq) - 1
        if non_inverted is not None:
            iorq = xp.where(non_inverted, iorq, 1 / iorq)
        return iorq



    def densityCompensation(self, width, height, imageColorArray, channels, stride):
        self.textureMapping.densityCompensation(width, height, imageColorArray, channels, stride)

    def inverseDensityCompensation(self, width, height, imageColorArray, channels, stride):
        self.textureMapping.inverseDensityCompensation(width, height, imageColorArray, channels, stride)
