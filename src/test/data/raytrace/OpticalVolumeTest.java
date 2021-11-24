package test.data.raytrace;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import data.raytrace.GuiOpticalVolumeObject;
import data.raytrace.OpticalObject;
import data.raytrace.OpticalObject.SCENE_OBJECT_COLUMN_TYPE;
import data.raytrace.OpticalSurfaceObject;
import data.raytrace.OpticalVolumeObject;
import data.raytrace.ParseUtil;
import maths.OperationCompiler;
import maths.exception.OperationParseException;
import maths.variable.VariableStack;
import util.ArrayTools;
import util.ArrayUtil;
@RunWith(Parameterized.class)
public class OpticalVolumeTest {

    private static interface CalculationMethod{
        public void calculateRays(OpticalVolumeObject ovo, float position[], float direction[], int iteration[], int fromIndex, int toIndex);
    }
    private static class FloatMethod implements CalculationMethod{
        @Override
        public void calculateRays(OpticalVolumeObject ovo, float position[], float direction[], int iteration[], int fromIndex, int toIndex)
        {
            ovo.calculateRays(position, direction, iteration, fromIndex, toIndex);
        }
    }

    private static class ObjectMethod implements CalculationMethod{
        @Override
        public void calculateRays(OpticalVolumeObject ovo, float position[], float direction[], int iteration[], int fromIndex, int toIndex)
        {
            OpticalObject obj[] = new OpticalObject[(toIndex - fromIndex) / 3];
            Arrays.fill(obj, ovo);
            ovo.calculateRays(position, direction, fromIndex, toIndex, obj, 0, null, iteration);
        }
    }

    @Parameters
    public static List<CalculationMethod> params() {
        return ArrayTools.unmodifiableList(new CalculationMethod[] {new FloatMethod(), new ObjectMethod()});
    }

    private final CalculationMethod cm;

    public OpticalVolumeTest(CalculationMethod cm)
    {
        this.cm = cm;
    }

    @Test
    private void testCubeMirror() throws OperationParseException {
        VariableStack vs = new VariableStack();
        ParseUtil parser = new ParseUtil();
        GuiOpticalVolumeObject ovo = new GuiOpticalVolumeObject(vs, parser);
        ovo.setValue(SCENE_OBJECT_COLUMN_TYPE.POSITION, "{0,0,0}", vs, parser);
        ovo.setValue(SCENE_OBJECT_COLUMN_TYPE.TRANSFORMATION, "{{1,0,0,0},{0,1,0,0},{0,0,1,0},{0,0,0,1}}", vs, parser);
        ovo.setSize(10, 10, 10);
        ovo.volumeScaling = 100;
        ovo.editValues(OpticalSurfaceObject.EMPTY_SURFACE_ARRAY,
                OperationCompiler.compile("if(x > -0.5,0x6000,0x10000)"),
                OperationCompiler.compile("0x7FFFFFFF"),
                null, null, vs, ovo.getVolume());
        float position[] = {-0.99f,-0.99f,0};
        float direction[] = {200,100,0};
        int iteration[] = new int[1];
        ovo.setValue(SCENE_OBJECT_COLUMN_TYPE.MAX_STEPS, "10000", vs, parser);
        ovo.setValue(SCENE_OBJECT_COLUMN_TYPE.VOLUME_SCALING, "0.1", vs, parser);
        cm.calculateRays(ovo, position, direction, iteration, 0, 3);
        float positionExpected[] = {-1,0.491f,0};
        float directionExpected[] = {-2,1,0};
        ArrayUtil.mult(direction, 0, direction.length, (float)Math.sqrt(1f/ArrayUtil.normQ(direction, 0, direction.length, 0)));
        ArrayUtil.mult(directionExpected, 0, directionExpected.length, (float)Math.sqrt(1f/ArrayUtil.normQ(directionExpected, 0, directionExpected.length, 0)));
        assertEquals("position |" + Arrays.toString(position) + '-' + Arrays.toString(positionExpected) + "|=" + ArrayUtil.distanceQ(position, 0, positionExpected, 0,3), 0, ArrayUtil.distanceQ(position, 0, positionExpected, 0,3), 0.01);
        assertEquals("direction |" + Arrays.toString(direction) + '-' + Arrays.toString(directionExpected) + "|=" + ArrayUtil.distanceQ(direction, 0, directionExpected, 0,3), 0, ArrayUtil.distanceQ(direction, 0, directionExpected, 0,3), 0.01);
    }

    @Test
    private void testCuboidMirror() throws OperationParseException {
        VariableStack vs = new VariableStack();
        ParseUtil parser = new ParseUtil();
        GuiOpticalVolumeObject ovo = new GuiOpticalVolumeObject(vs, parser);
        ovo.setValue(SCENE_OBJECT_COLUMN_TYPE.POSITION, "{0,0,0}", vs, parser);
        ovo.setValue(SCENE_OBJECT_COLUMN_TYPE.TRANSFORMATION, "{{1,0,0,0},{0,2,0,0},{0,0,1,0},{0,0,0,1}}", vs, parser);
        ovo.setSize(10, 20, 10);
        ovo.volumeScaling = 100;
        ovo.editValues(OpticalSurfaceObject.EMPTY_SURFACE_ARRAY,
                OperationCompiler.compile("if(x > -0.5,0x6000,0x10000)"),
                OperationCompiler.compile("0x7FFFFFFF"),
                null, null, vs, ovo.getVolume());
        float position[] = {-0.99f,-0.99f,0};
        float direction[] = {200,100,0};
        int iteration[] = new int[1];
        ovo.setValue(SCENE_OBJECT_COLUMN_TYPE.MAX_STEPS, "10000", vs, parser);
        ovo.setValue(SCENE_OBJECT_COLUMN_TYPE.VOLUME_SCALING, "0.1", vs, parser);
        cm.calculateRays(ovo, position, direction, iteration, 0, 3);
        System.out.println("iterations " + Arrays.toString(iteration));
        float positionExpected[] = {-1,0.491f,0};
        float directionExpected[] = {-2,1,0};
        ArrayUtil.mult(direction, 0, direction.length, (float)Math.sqrt(1f/ArrayUtil.normQ(direction, 0, direction.length, 0)));
        ArrayUtil.mult(directionExpected, 0, directionExpected.length, (float)Math.sqrt(1f/ArrayUtil.normQ(directionExpected, 0, directionExpected.length, 0)));
        assertEquals("position |" + Arrays.toString(position) + '-' + Arrays.toString(positionExpected) + "|=" + ArrayUtil.distanceQ(position, 0, positionExpected, 0,3), 0, ArrayUtil.distanceQ(position, 0, positionExpected, 0,3),0.01);
        assertEquals("direction |" + Arrays.toString(direction) +'-' + Arrays.toString(directionExpected) + "|=" + ArrayUtil.distanceQ(direction, 0, directionExpected, 0,3), 0, ArrayUtil.distanceQ(direction, 0, directionExpected, 0,3),0.01);
    }

    @Test
    private void testCuboidRotatedMirror() throws OperationParseException {
        VariableStack vs = new VariableStack();
        ParseUtil parser = new ParseUtil();
        GuiOpticalVolumeObject ovo = new GuiOpticalVolumeObject(vs, parser);
        ovo.setValue(SCENE_OBJECT_COLUMN_TYPE.POSITION, "{0,0,0}", vs, parser);
        ovo.setValue(SCENE_OBJECT_COLUMN_TYPE.TRANSFORMATION, "{{0,1,0,0},{-2,0,0,0},{0,0,1,0},{0,0,0,1}}", vs, parser);
        ovo.setSize(10, 20, 10);
        ovo.volumeScaling = 100;
        ovo.editValues(OpticalSurfaceObject.EMPTY_SURFACE_ARRAY,
                OperationCompiler.compile("if(x > -0.5,0x6000,0x10000)"),
                OperationCompiler.compile("0x7FFFFFFF"),
                null, null, vs, ovo.getVolume());
        float position[] = {-0.99f,-0.99f,0};
        float direction[] = {200,100,0};
        int iteration[] = new int[1];
        ovo.setValue(SCENE_OBJECT_COLUMN_TYPE.MAX_STEPS, "10000", vs, parser);
        ovo.setValue(SCENE_OBJECT_COLUMN_TYPE.VOLUME_SCALING, "0.1", vs, parser);
        cm.calculateRays(ovo, position, direction, iteration, 0, 3);
        System.out.println("iterations " + Arrays.toString(iteration));
        float positionExpected[] = {-1,0.491f,0};
        float directionExpected[] = {-2,1,0};
        ArrayUtil.mult(direction, 0, direction.length, (float)Math.sqrt(1f/ArrayUtil.normQ(direction, 0, direction.length, 0)));
        ArrayUtil.mult(directionExpected, 0, directionExpected.length, (float)Math.sqrt(1f/ArrayUtil.normQ(directionExpected, 0, directionExpected.length, 0)));
        assertEquals("position |" + Arrays.toString(position) + '-' + Arrays.toString(positionExpected) + "|=" + ArrayUtil.distanceQ(position, 0, positionExpected, 0,3), 0, ArrayUtil.distanceQ(position, 0, positionExpected, 0,3), 0.01);
        assertEquals("direction |" + Arrays.toString(direction) + '-' + Arrays.toString(directionExpected) + "|=" + ArrayUtil.distanceQ(direction, 0, directionExpected, 0,3), 0, ArrayUtil.distanceQ(direction, 0, directionExpected, 0,3), 0.01);
    }
}
