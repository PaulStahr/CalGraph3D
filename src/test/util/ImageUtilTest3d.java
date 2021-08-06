package test.util;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import test.geometry.GeometryTest;
import util.Interpolator;

@RunWith(Parameterized.class)
public class ImageUtilTest3d {
    @Parameters
    public static List<GeometryTest.InitFunctional3d> params() {
        return Arrays.asList(GeometryTest.identityXGenerator, GeometryTest.identityYGenerator, GeometryTest.identityZGenerator,
                GeometryTest.sphereGenerator);
    }
    public ImageUtilTest3d(GeometryTest.InitFunctional3d initF) {this.initF = initF;}
    private final GeometryTest.InitFunctional3d initF;

    @Test
    public void testInterpolate3d(){
        int width = 10, height = 10, depth = 10;
        float data[] = GeometryTest.generateArray(initF, width, height, depth);
        float testpoints[] = {0,0,0,1,1,1,0,1,2,4.5f,3.5f,2.5f,8,8.5f,8.8f};
        for (int i = 0; i < testpoints.length; i += 3)
        {
            float interpolated = Interpolator.interpolatePoint(testpoints[i], testpoints[i + 1], testpoints[i + 2], data, width, height, depth);
            float expected = GeometryTest.eval(initF, testpoints[i], testpoints[i + 1], testpoints[i + 2], width, height, depth);
            try {
                assert(Math.abs(interpolated - expected) < 0.05);
            }catch(AssertionError e){
                throw new AssertionError("interpolated was " + interpolated + " expected " + expected, e);
            }
        }
    }
}
