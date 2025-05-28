from calgraph3d.data.raytrace.OpticalSurfaceObject import OpticalSurfaceObject
from calgraph3d.data.raytrace.SurfaceType import SurfaceType
from calgraph3d.data.raytrace.Intersection import Intersection
import numpy as np
import unittest
from functools import partial


class TestOpticalSurface(unittest.TestCase):
    def testInnerOuterCustom(self):
        rng = np.random.default_rng(seed=1)
        oso = OpticalSurfaceObject()
        oso.conicConstant = 2
        oso.surf = SurfaceType.CUSTOM
        oso.direction[:] = (10,0,10)
        oso.midpoint[:] = (10,30,40)
        dx = 0.001
        num_testpoints = 100
        rtol = 1e-8
        location = rng.normal(0, 100, (num_testpoints, 3))

        for oso.conicConstant in (-1,0,2,3,4):
            oso.update()
            derivative_numerical = OpticalSurfaceObject.numerical_derivative(oso.evaluate_inner_outer, dx)(location, normalize=False)[1]
            derivative_analytical = oso.evaluate_inner_outer(location, normalize='seperate')[1]
            np.testing.assert_allclose(derivative_numerical, derivative_analytical, rtol=rtol)


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