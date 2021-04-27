package test.util;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import jcomponents.util.ImageUtil;
import test.geometry.GeometryTest;

@RunWith(Parameterized.class)
public class ImageUtilTest {
    @Parameters
    public static List<GeometryTest.InitFunctional> params() {
        return Arrays.asList(new GeometryTest.InitFunctional[] {GeometryTest.identityXGenerator, GeometryTest.identityYGenerator, GeometryTest.identityZGenerator, GeometryTest.sphereGenerator});
    }
    public ImageUtilTest(GeometryTest.InitFunctional initF) {this.initF = initF;}
    private final GeometryTest.InitFunctional initF;

    @Test 
    public void testInterpolate(){
        int width = 10, height = 10, depth = 10;
        float data[] = GeometryTest.generateArray(initF, width, height, depth);
        float testpoints[] = {0,0,0,1,1,1,0,1,2,4.5f,3.5f,2.5f,8,8.5f,8.8f};
        for (int i = 0; i < testpoints.length; i += 3)
        {
            float interpolated = ImageUtil.getSmoothedPixel(testpoints[i], testpoints[i + 1], testpoints[i + 2], data, width, height, depth);
            float expected = GeometryTest.eval(initF, testpoints[i], testpoints[i + 1], testpoints[i + 2], width, height, depth);
            try {
                assert(Math.abs(interpolated - expected) < 0.05);
            }catch(AssertionError e){
                throw new AssertionError("interpolated was " + interpolated + " expected " + expected, e);
            }
        }
    }

}
