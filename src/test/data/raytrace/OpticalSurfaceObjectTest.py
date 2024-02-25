from calgraph3d.data.raytrace.OpticalSurfaceObject import OpticalSurfaceObject
from calgraph3d.data.raytrace.SurfaceType import SurfaceType
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


if __name__ == '__main__':
    unittest.main()
