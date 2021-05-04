package test.geometry;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Test;

import geometry.Geometry;
import geometry.Vector3d;
import jcomponents.util.ImageUtil;
import util.data.DoubleArrayList;
import util.data.IntegerArrayList;

public class GeometryTest {
    public static interface InitFunctional2d{
        public float eval(float x, float y);
    }

    public static interface InitFunctional3d{
        public float eval(float x, float y, float z);
    }

    public static final InitFunctional3d sphereGenerator = new InitFunctional3d() {
        @Override
        public float eval(float x, float y, float z)
        {
            return (float)(Math.sqrt(x * x + y * y + z * z) - 0.5);
        }
    };

    public static final InitFunctional3d identityXGenerator = new InitFunctional3d() {
        @Override
        public float eval(float x, float y, float z)
        {
            return x;
        }
    };

    public static final InitFunctional3d identityYGenerator = new InitFunctional3d() {
        @Override
        public float eval(float x, float y, float z)
        {
            return x;
        }
    };

    public static final InitFunctional3d identityZGenerator = new InitFunctional3d() {
        @Override
        public float eval(float x, float y, float z)
        {
            return x;
        }
    };

    public static final InitFunctional2d circleGenerator = new InitFunctional2d() {
        @Override
        public float eval(float x, float y)
        {
            return (float)(Math.sqrt(x * x + y * y) - 0.5);
        }
    };

    public static final InitFunctional2d identityXGenerator2d = new InitFunctional2d() {
        @Override
        public float eval(float x, float y)
        {
            return x;
        }
    };

    public static final InitFunctional2d identityYGenerator2d = new InitFunctional2d() {
        @Override
        public float eval(float x, float y)
        {
            return x;
        }
    };



    public static float eval(InitFunctional3d initF, float x, float y, float z, int width, int height, int depth)
    {
        float invWidth = 1f/width, invHeight = 1f/height, invDepth = 1f / depth;
        return initF.eval((x * 2 + 1 - width) * invWidth, (y * 2 + 1 - height) * invHeight, (z * 2 + 1 - depth) * invDepth);
    }

    public static float eval(InitFunctional2d initF, float x, float y, int width, int height)
    {
        float invWidth = 1f/width, invHeight = 1f/height;
        return initF.eval((x * 2 + 1 - width) * invWidth, (y * 2 + 1 - height) * invHeight);
    }

    public static float[] generateArray(InitFunctional3d f, int width, int height, int depth) {
        float data[] = new float[width * height * depth];
        for (int z = 0, index = 0; z < depth; ++z)
        {
            for (int y = 0; y < height; ++y)
            {
                for (int x = 0; x < width; ++x)
                {
                    data[index ++] = eval(f, x, y, z, width, height, depth);
                }
            }
        }
        return data;
    }

    public static float[] generateArray(InitFunctional2d f, int width, int height) {
        float data[] = new float[width * height];
        for (int y = 0, index = 0; y < height; ++y)
        {
            for (int x = 0; x < width; ++x)
            {
                data[index ++] = eval(f, x, y, width, height);
            }
        }
        return data;
    }

    @Test
    public void testVolumeToMesh(){
        int width = 10, height = 10, depth = 10;
        float data[] = generateArray(sphereGenerator, width, height, depth);
        IntegerArrayList faceIndices = new IntegerArrayList();
        DoubleArrayList vertexPositions = new DoubleArrayList();
        Geometry.volumeToMesh(data, width, height, depth, 0, faceIndices, vertexPositions);
        Vector3d v = new Vector3d();
        float expectedDist = 10f/4;
        for (int i = 0; i < vertexPositions.size(); i += 3)
        {
            v.set(vertexPositions, i);
            float smoothed = ImageUtil.getSmoothedPixel(v.x, v.y, v.z, data, width, height, depth);
            try {
                assert(Math.abs(smoothed) < 0.05);
            }catch (AssertionError e){
                throw new AssertionError("Assert failed, smoothed " + smoothed + " should be 0", e);
            }
            double dist = Math.sqrt(v.distanceQ(4.5, 4.5, 4.5));
            try {
                assert(Math.abs(dist - expectedDist) < 0.05);
            }catch (AssertionError e){
                throw new AssertionError("Assert failed, distance of " + v + " is " + dist + " should be " + expectedDist, e);
            }
        }
    }

    @Test
    public void clipCornerTest() {
        float data[] = new float[8];
        IntegerArrayList faceIndices = new IntegerArrayList();
        DoubleArrayList vertexPositions = new DoubleArrayList();
        for (int i = 0; i < 8; ++i)
        {
            Arrays.fill(data, -1);
            data[i] = 0.000001f;
            faceIndices.clear();
            vertexPositions.clear();
            Geometry.volumeToMesh(data, 2, 2, 2, 0, faceIndices, vertexPositions);
            assertEquals("Corner " + i, 3, faceIndices.size());
            assertEquals(9, vertexPositions.size());
        }
    }

    @Test
    public void testVolumeToMeshSmall() {
        float data[] = new float[8];
        IntegerArrayList faceIndices = new IntegerArrayList();
        DoubleArrayList vertexPositions = new DoubleArrayList();
        for (int i = 0; i < 256; ++i)
        {
            for (int j = 0; j < 8; ++j)
            {
                data[j] = ((i >> j) % 2) * 2 - 1;
            }
            faceIndices.clear();
            vertexPositions.clear();
            Geometry.volumeToMesh(data, 2, 2, 2, 0, faceIndices, vertexPositions);
        }
    }
}
