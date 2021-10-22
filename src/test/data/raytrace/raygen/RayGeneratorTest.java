package test.data.raytrace.raygen;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import data.raytrace.GuiOpticalSurfaceObject;
import data.raytrace.GuiOpticalSurfaceObject.ANCHOR_POINT_ENUM;
import data.raytrace.OpticalObject.SCENE_OBJECT_COLUMN_TYPE;
import data.raytrace.ParseUtil;
import data.raytrace.RaySimulation.SurfaceType;
import data.raytrace.raygen.RayGenerator;
import geometry.Vector2d;
import geometry.Vector3d;
import maths.exception.OperationParseException;
import maths.variable.VariableStack;
import util.ArrayUtil;

public class RayGeneratorTest {


    public static float sum(float[] data, int begin, int end) {
        float result = 0;
        for (;begin < end; ++begin){result += data[begin];}
        return result;
    }


    public static float qdist(float[] l0, int begin0, float[] l1, int begin1, int size) {
        float result = 0;
        while (size != 0)
        {
            float diff = l0[begin0++] - l1[begin1++];
            result += diff * diff;
            --size;
        }
        return result;
    }

    @Test
    public void testCircle() throws OperationParseException
    {
        RayGenerator gen = new RayGenerator();
        ParseUtil parser = new ParseUtil();
        VariableStack vs = new VariableStack();
        GuiOpticalSurfaceObject oso = new GuiOpticalSurfaceObject(vs, parser);
        oso.setValue(SCENE_OBJECT_COLUMN_TYPE.DIRECTION, new Vector3d(1,0,0), vs, parser);
        oso.setValue(SCENE_OBJECT_COLUMN_TYPE.SURFACE, SurfaceType.SPHERICAL, vs, parser);
        oso.setValue(SCENE_OBJECT_COLUMN_TYPE.MAXRADIUS, 0, vs, parser);
        oso.setValue(SCENE_OBJECT_COLUMN_TYPE.DIFFUSE, 1, vs, parser);
        gen.setSource(oso);
        gen.threeDimensional = false;
        int numrays = 100000;
        Vector3d position = new Vector3d();
        Vector3d direction = new Vector3d();
        Vector2d textureCoordinate = new Vector2d();
        float color[] = new float[4];
        int numBins = 16;
        float distribution[] = new float[numBins];
        float expectedDistribution[] = new float[numBins];
        for (int i = 0; i < numBins; ++i)
        {
            double arc = (i -numBins / 2 + 0.5) * (Math.PI /numBins);
            expectedDistribution[i] = (float)(Math.cos(arc)) * ((float)Math.PI / (numBins * 2));
        }
        assertEquals(1, sum(expectedDistribution, 0, expectedDistribution.length), 0.01f);
        for (int i = 0; i < numrays; ++i)
        {
            gen.generate(i, numrays, position, direction, textureCoordinate, color);
            ++distribution[(int)(Math.asin(direction.y / direction.norm()) * numBins/ Math.PI - 1E-6 + numBins/2)];
        }
        ArrayUtil.mult(distribution, 0, distribution.length, 1f/numrays);
        assertEquals(0, qdist(distribution, 0, expectedDistribution, 0, distribution.length), 0.001f);
    }

    private void testSphereDiffuseDistribution(Vector3d dir) throws OperationParseException
    {
        RayGenerator gen = new RayGenerator();
        ParseUtil parser = new ParseUtil();
        VariableStack vs = new VariableStack();
        GuiOpticalSurfaceObject oso = new GuiOpticalSurfaceObject(vs, parser);
        oso.setValue(SCENE_OBJECT_COLUMN_TYPE.DIRECTION, dir, vs, parser);
        oso.setValue(SCENE_OBJECT_COLUMN_TYPE.SURFACE, SurfaceType.SPHERICAL, vs, parser);
        oso.setValue(SCENE_OBJECT_COLUMN_TYPE.MAXRADIUS, 0, vs, parser);
        oso.setValue(SCENE_OBJECT_COLUMN_TYPE.DIFFUSE, 1, vs, parser);
        gen.setSource(oso);
        gen.threeDimensional = true;
        int numrays = 100000;
        Vector3d position = new Vector3d();
        Vector3d direction = new Vector3d();
        Vector2d textureCoordinate = new Vector2d();
        float color[] = new float[4];
        int numBins = 16;
        float distribution[] = new float[numBins];
        float expectedDistribution[] = new float[numBins];
        for (int i = 0; i < expectedDistribution.length; ++i)
        {
            double arc = (i + 0.5) * (Math.PI /(numBins * 2));
            expectedDistribution[i] = (float)(Math.sin(arc) * Math.cos(arc) * (Math.PI / numBins));
        }
        assertEquals(1, sum(expectedDistribution, 0, expectedDistribution.length), 0.01f);
        for (int i = 0; i < numrays; ++i)
        {
            gen.generate(i, numrays, position, direction, textureCoordinate, color);
            ++distribution[(int)(Math.asin(direction.dot(dir) / direction.norm()) * 2 * numBins / Math.PI - 1E-6)];
        }
        ArrayUtil.mult(distribution, 0, distribution.length, 1f/numrays);
        assertEquals(0, qdist(distribution, 0, expectedDistribution, 0, distribution.length), 0.001f);
    }

    @Test
    public void testSphereDiffuseDistribution() throws OperationParseException
    {
        testSphereDiffuseDistribution(new Vector3d(1,0,0));
        testSphereDiffuseDistribution(new Vector3d(0,1,0));
        testSphereDiffuseDistribution(new Vector3d(0,0,1));
    }

    @Test
    public void testSphereSurfaceDistribution() throws OperationParseException
    {
        RayGenerator gen = new RayGenerator();
        ParseUtil parser = new ParseUtil();
        VariableStack vs = new VariableStack();
        GuiOpticalSurfaceObject oso = new GuiOpticalSurfaceObject(vs, parser);
        oso.setValue(SCENE_OBJECT_COLUMN_TYPE.ANCHOR_POINT, ANCHOR_POINT_ENUM.NORMAL_INTERSECTION, vs, parser);
        oso.setValue(SCENE_OBJECT_COLUMN_TYPE.POSITION, new Vector3d(0,0,0), vs, parser);
        oso.setValue(SCENE_OBJECT_COLUMN_TYPE.DIRECTION, new Vector3d(0,0,1), vs, parser);
        oso.setValue(SCENE_OBJECT_COLUMN_TYPE.SURFACE, SurfaceType.SPHERICAL, vs, parser);
        oso.setValue(SCENE_OBJECT_COLUMN_TYPE.MAXRADIUS, 1, vs, parser);
        oso.setValue(SCENE_OBJECT_COLUMN_TYPE.DIFFUSE, 1, vs, parser);
        gen.setSource(oso);
        gen.threeDimensional = true;
        int numrays = 100000;
        Vector3d position = new Vector3d();
        Vector3d direction = new Vector3d();
        Vector2d textureCoordinate = new Vector2d();
        float color[] = new float[4];
        int numBins = 16;
        float distribution[] = new float[numBins];
        float expectedDistribution[] = new float[numBins];
        for (int i = 0; i < expectedDistribution.length; ++i)
        {
            double arc = (i + 0.5) * (Math.PI /(numBins * 2));
            expectedDistribution[i] = (float)(Math.cos(arc) * Math.PI / (2 * numBins));
        }
        assertEquals(1, sum(expectedDistribution, 0, expectedDistribution.length), 0.01f);
        for (int i = 0; i < numrays; ++i)
        {
            gen.generate(i, numrays, position, direction, textureCoordinate, color);
            if ((int)(Math.asin(position.dot(0,0,1) / direction.norm()) * 2 * numBins/ Math.PI) >= distribution.length)
            {
                System.out.println(position);
            }
            ++distribution[(int)(Math.asin(position.dot(0,0,1) / direction.norm()) * 2 / Math.PI * numBins - 1E-6)];
        }
        ArrayUtil.mult(distribution, 0, distribution.length, 1f/numrays);
        assertEquals(0, qdist(distribution, 0, expectedDistribution, 0, distribution.length), 0.001f);
    }
}
