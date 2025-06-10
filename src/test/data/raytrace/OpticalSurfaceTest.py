from cupy_backends.cuda.libs.cublas import dtpttr
from triton.language import dtype

from calgraph3d.data.raytrace.MaterialType import MaterialType
from calgraph3d.data.raytrace.OpticalSurfaceObject import OpticalSurfaceObject
from calgraph3d.data.raytrace.SurfaceType import SurfaceType
from calgraph3d.data.raytrace.Intersection import Intersection
import numpy as np
import unittest
from functools import partial

from jsymmath.geometry.Geometry import Geometry
import jpype
from jpype import JPackage, JArray, JDouble, JException
import jpype.types as jtypes
import numpy as np
import os

from src.data.raytrace.RaytraceScene import RaytraceScene

# collect all .jar-filces in bin/ to a list
jar_files = os.listdir("bin")
jar_files = [f"bin/{f}" for f in jar_files if f.endswith('.jar')]


if not jpype.isJVMStarted():
    jpype.startJVM(classpath=[*jar_files, "./CalGraph3D.jar"])
data_package = JPackage("data")
geometry_package = JPackage("geometry")


class TestOpticalSurface(unittest.TestCase):

    @staticmethod
    def plot_surface(surf, plotter, color):
        import pyvista as pv
        mesh_size = (16, 16)
        mesh_vertices = surf.getMeshVertices(*mesh_size)
        mesh_faces = surf.getMeshFaces(*mesh_size).reshape((-1, 4))
        mesh_faces = np.hstack((np.full(shape=(mesh_faces.shape[0], 1), fill_value=4), mesh_faces)).astype(np.int32)
        polydata = pv.PolyData(mesh_vertices, mesh_faces)
        actor = plotter.add_mesh(polydata, color=color, show_edges=True, opacity=0.5)
        return actor

    @staticmethod
    def interleave(arrays, axis):
        shape = np.asarray(arrays[0].shape)
        shape[axis] += arrays[1].shape[axis]
        result = np.empty(shape, dtype=arrays[0].dtype)
        slices = [slice(None)] * arrays[0].ndim
        for i, arr in enumerate(arrays):
            slices[axis] = slice(i, None, len(arrays))
            result[tuple(slices)] = arr
        return result

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

        for oso.surf in (SurfaceType.SPHERICAL, SurfaceType.FLAT):
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
        oso.getIntersection(position, direction, intersection, np.full(shape=10, fill_value=0), np.full(shape=10, fill_value=100))
        expectedPoint = np.asarray((-np.sqrt(4 - np.square(position[:, 1])), position[:, 1], np.zeros(10))).T
        np.testing.assert_allclose(intersection.position, expectedPoint, atol=1e-5)
        intersection = Intersection(10)
        position[:, 0] = 0
        position[:, 1] = 0.1 * np.arange(1, 11)
        oso.getIntersection(position, direction, intersection, np.full(shape=10, fill_value=0), np.full(shape=10, fill_value=100))
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


    def testTextureCoordinates(self):
        oso = OpticalSurfaceObject()
        oso_java = data_package.raytrace.OpticalSurfaceObject()
        oso.surf = SurfaceType.SPHERICAL
        oso_java.surf = data_package.raytrace.RaySimulation.SurfaceType.SPHERICAL
        rnd = np.random.default_rng(seed=42)
        for i in range(20):
            if i < 3:
                direction = np.zeros(shape=3)
                direction[i] = 1
            else:
                direction = rnd.normal(size=3)
            if i < 10:
                midpoint = np.zeros(shape=3)
            else:
                midpoint = rnd.normal(size=3)
            oso.direction[:] = direction
            oso_java.direction.set(*direction)
            oso.midpoint[:] = midpoint
            oso_java.midpoint.set(*midpoint)
            oso.minRadiusGeometric = 0
            oso_java.minRadiusGeometric = 0
            oso.update()
            oso_java.update()

            matrix_java = oso_java.getSurfaceToGlobalTransformation().toArrayD()

            matrix_java = np.array(matrix_java).reshape((4,4))
            np.testing.assert_allclose(oso.matSurfaceToGlobal, matrix_java, rtol=1e-8)

            dims = (8,16)
            vertex_count = oso.getMeshVertexCount(*dims)
            vertex_count_java = oso_java.getMeshVertexCount(*dims)
            assert vertex_count == vertex_count_java, f"Vertex count mismatch: {vertex_count} != {vertex_count_java}"
            vertices = oso.getMeshVertices(*dims)
            # create float array of size vertices * 3
            vertices_java = jpype.JArray(jtypes.JFloat)(vertex_count * 3)
            oso_java.getMeshVertices(*dims, vertices_java)
            vertices_java = np.array(vertices_java).reshape((vertex_count, 3))
            np.testing.assert_allclose(vertices, vertices_java, atol=1e-8)


    def testHyperbolic(self):
        rs = RaytraceScene()
        oso = OpticalSurfaceObject()
        oso.surf = SurfaceType.HYPERBOLIC
        oso.direction[:] = (1, 0, 0)
        oso.midpoint[:] = (0, 0, 0)
        oso.minRadiusGeometric = 0
        oso.maxRadiusGeometric = 10
        oso.ior0 = 1
        oso.ior1 = np.sqrt(2)
        oso.materialType = MaterialType.REFRACTION
        oso.active = True
        oso.update()
        rs.optical_surface_objects.append(oso)
        position = np.empty(shape=(10, 3), dtype=float)
        position[:, 0] = -10
        position[:, 1] = np.linspace(0, 4, 10)
        position[:, 2] = 0
        direction = np.repeat(np.asarray([[1, 0, 0]], dtype=float), 10, axis=0)
        trajectory = []
        rs.calculateRays(
            position,
            direction,
            [oso],
            trajectory=trajectory,
            lower_bound=np.zeros(10, dtype=float),
            upper_bound=np.full(10, 100, dtype=float))
        trajectory.append(position + direction * 5)
        trajectory = np.asarray(trajectory)  # Lines, point on line, dimension
        show_plot = False
        if show_plot:
            import pyvista as pv
            plotter = pv.Plotter()
            plotter.enable_depth_peeling(number_of_peels=100, occlusion_ratio=0.1)
            plotter.enable_anti_aliasing()
            TestOpticalSurface.plot_surface(oso, plotter, 'blue')
            plotter.add_points(trajectory[0, :, :], color='red', point_size=20, render_points_as_spheres=True)
            plotter.add_lines(np.swapaxes(TestOpticalSurface.interleave((trajectory[:-1, ...],trajectory[1:, ...]),axis=0), 0, 1).reshape(-1,3), color='green', width=5)
            plotter.show()
        np.testing.assert_allclose(oso.evaluate_inner_outer(position), 0, atol=1e-8)
        expectedPoint = np.asarray((2 + np.sqrt(2), 0, 0)).reshape((1, 3))
        position -= expectedPoint
        direction /= np.linalg.norm(direction, axis=-1, keepdims=True)
        position += direction * (-np.sum(position * direction, axis=-1, keepdims=True))
        np.testing.assert_allclose(position, 0, atol=1e-5)


    def testSpherical(self):
        rs = RaytraceScene()
        oso = OpticalSurfaceObject()
        oso.surf = SurfaceType.SPHERICAL
        oso.direction[:] = (1, 0, 0)
        oso.midpoint[:] = (0, 0, 0)
        oso.minRadiusGeometric = 0
        oso.maxRadiusGeometric = 1
        oso.materialType = MaterialType.REFLECTION
        oso.active = True
        oso.update()
        rs.optical_surface_objects.append(oso)
        position = np.empty(shape=(10, 3), dtype=float)
        position[:, 0] = -2
        position[:, 1] = np.linspace(0, 0.8, 10)
        position[:, 2] = 0
        direction = np.repeat(np.asarray([[1,0,0]], dtype=float), 10, axis=0)
        trajectory = []
        rs.calculateRays(
            position,
            direction,
            [oso],
            trajectory=trajectory,
            lower_bound=np.zeros(10, dtype=float),
            upper_bound=np.full(10, 100, dtype=float))
        trajectory.append(position + direction * 2)
        trajectory = np.asarray(trajectory)  # Lines, point on line, dimension
        show_plot = False
        if show_plot:
            import pyvista as pv
            plotter = pv.Plotter()
            TestOpticalSurface.plot_surface(oso, plotter, 'blue')
            plotter.add_points(trajectory[0, :, :], color='red', point_size=20, render_points_as_spheres=True)
            plotter.add_lines(np.swapaxes(TestOpticalSurface.interleave((trajectory[:-1, ...],trajectory[1:, ...]),axis=0), 0, 1).reshape(-1,3), color='green', width=5)
            plotter.show()
        np.testing.assert_allclose(oso.evaluate_inner_outer(position), 0, atol=1e-8)
        expectedPoint = np.stack((np.sqrt(1 - np.square(position[:, 1])), position[:, 1], np.zeros(10)), axis=-1)
        np.testing.assert_allclose(position, expectedPoint, atol=1e-5)



    def testParabolic(self):
        rs = RaytraceScene()
        oso = OpticalSurfaceObject()
        oso.surf = SurfaceType.PARABOLIC
        oso.direction[:] = (1, 0, 0)
        oso.midpoint[:] = (0, 0, 0)
        oso.minRadiusGeometric = 0
        oso.maxRadiusGeometric = 1
        oso.materialType = MaterialType.REFLECTION
        oso.active = True
        oso.update()
        rs.optical_surface_objects.append(oso)
        position = np.empty(shape=(10, 3), dtype=float)
        position[:, 0] = -1
        position[:, 1] = np.linspace(0, 0.09, 10)
        position[:, 2] = 0
        direction = np.repeat(np.asarray([[1, -1e-4, 0]], dtype=float), 10, axis=0)
        trajectory = []
        rs.calculateRays(
            position,
            direction,
            [oso],
            trajectory=trajectory,
            lower_bound=np.zeros(10, dtype=float),
            upper_bound=np.full(10, 100, dtype=float))
        show_plot = False
        trajectory.append(position + direction * 2)
        trajectory = np.asarray(trajectory)  # Lines, point on line, dimension
        if show_plot:
            import pyvista as pv
            plotter = pv.Plotter()
            TestOpticalSurface.plot_surface(oso, plotter, 'blue')
            plotter.add_points(trajectory[0, :, :], color='red', point_size=20, render_points_as_spheres=True)
            plotter.add_lines(np.swapaxes(TestOpticalSurface.interleave((trajectory[:-1, ...],trajectory[1:, ...]),axis=0), 0, 1).reshape(-1,3), color='green', width=5)
            plotter.show()
        np.testing.assert_allclose(oso.evaluate_inner_outer(position), 0, atol=1e-8)
        expectedPoint = np.asarray((0.5, 0, 0)).reshape((1, 3))
        position -= expectedPoint
        direction /= np.linalg.norm(direction, axis=-1, keepdims=True)
        position -= direction * np.sum(position * direction, axis=-1, keepdims=True)
        np.testing.assert_allclose(position, 0, atol=1e-3)


if __name__ == '__main__':
    unittest.main()