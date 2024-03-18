from calgraph3d.data.raytrace.OpticalSurfaceObject import OpticalSurfaceObject
from calgraph3d.data.raytrace.SurfaceType import SurfaceType
from calgraph3d.data.raytrace.Intersection import Intersection
from scipy.misc import derivative
import numpy as np
import unittest
from functools import partial


class TestOpticalSurface(unittest.TestCase):
    @staticmethod
    def multiderivative(f, x, dx):
        ndirs = len(x)
        x = np.array(x, copy=True)
        res = np.zeros(ndirs)
        dxh = dx / 2
        for i in range(ndirs):
            orig = x[i]
            x[i] = orig - dxh
            a = f(x)
            x[i] = orig + dxh
            b = f(x)
            x[i] = orig
            res[i] = ((b - a) / dx)
        return res


    def testInnerOuterCustom(self):
        rng = np.random.default_rng(seed=1)
        oso = OpticalSurfaceObject()
        oso.conicConstant = 2
        oso.surf = SurfaceType.CUSTOM
        oso.direction[:] = (10,0,10)
        oso.midpoint[:] = (10,30,40)
        dx = 0.001
        func = partial(oso.evaluate_inner_outer, normalize=False)
        func_normed = partial(oso.evaluate_inner_outer, normalize=True)

        for oso.conicConstant in (-1,0,2,3,4):
            oso.update()
            for i in range(10):
                location = np.round(rng.normal(0,100,3))
                derivative = TestOpticalSurface.multiderivative(func, location, dx)
                derivative /= oso.evaluate_inner_outer(location, normalize='seperate')[1]
                np.testing.assert_allclose(np.linalg.norm(derivative), 1)
                np.testing.assert_allclose(np.linalg.norm(TestOpticalSurface.multiderivative(func_normed, location, dx)), 0.5, atol=1.1)


    def sphere_tests(self, oso):
        intersection = Intersection(10)
        position = np.zeros(shape=(10, 3))
        position[:, 0] = -2
        position[:, 1] = 0.1 * np.arange(1, 11)
        direction = np.zeros(shape=(10, 3))
        direction[:, 0] = 1
        oso.getIntersection(position, direction, intersection, 0, 100)
        expectedPoint = np.asarray((-np.sqrt(4 - np.square(position[:, 1])), position[:, 1], np.zeros(10))).T
        np.testing.assert_allclose(intersection.position, expectedPoint, atol=1e-5)
        intersection = Intersection(10)
        position[:, 0] = 0
        position[:, 1] = 0.1 * np.arange(1, 11)
        oso.getIntersection(position, direction, intersection, 0, 100)
        expectedPoint = np.asarray((np.sqrt(4 - np.square(position[:, 1])), position[:, 1], np.zeros(10))).T
        np.testing.assert_allclose(intersection.position, expectedPoint, atol=1e-5)

    def test_conical(self):
        oso = OpticalSurfaceObject()
        oso.surf = SurfaceType.CUSTOM
        oso.direction[:] = [2, 0, 0]
        oso.midpoint[:] = [0, 0, 0]
        oso.minRadiusGeometric = 0
        oso.maxRadiusGeometric = 100
        oso.conicConstant = 0
        oso.update()
        self.sphere_tests(oso)


    def test_spherical(self):
        oso = OpticalSurfaceObject()
        oso.surf = SurfaceType.SPHERICAL
        oso.direction[:] = [2,0,0]
        oso.midpoint[:] = [0,0,0]
        oso.minRadiusGeometric = 0
        oso.maxRadiusGeometric = 100
        oso.update()
        self.sphere_tests(oso)

if __name__ == '__main__':
    unittest.main()