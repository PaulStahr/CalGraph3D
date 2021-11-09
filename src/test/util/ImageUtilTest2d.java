package test.util;

import static org.junit.Assert.assertEquals;

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import jcomponents.util.ImageUtil;
import test.geometry.GeometryTest;
import util.ArrayUtil;

@RunWith(Parameterized.class)
public class ImageUtilTest2d {
    @Parameters
    public static List<GeometryTest.InitFunctional2d> params() {
        return Arrays.asList(GeometryTest.identityXGenerator2d, GeometryTest.identityYGenerator2d, GeometryTest.circleGenerator);
    }
    public ImageUtilTest2d(GeometryTest.InitFunctional2d initF) {this.initF = initF;}
    private final GeometryTest.InitFunctional2d initF;

    @Test
    public void testInterpolate2d(){
        int width = 10, height = 10;
        float data[] = GeometryTest.generateArray(initF, width, height);
        float fusedMul[] = ArrayUtil.normalizeTo(data, 0, data.length, 0, 255);
        ArrayUtil.add(data, 0, data.length, 0.5f);
        float testpoints[] = {0,0,1,1,1,7,2.5f,4.5f,2,4.5f,3.5f,2.5f,8,8.5f,8.8f,3.4f};
        BufferedImage finalImg = new BufferedImage(width,height, BufferedImage.TYPE_INT_RGB);
        WritableRaster newRaster= (WritableRaster)finalImg.getData();
        newRaster.setPixels(0, 0, width, height, ArrayUtil.interleave(data,data,data));
        int interpolated[] = new int[4];
        int tmp[] = new int[4];
        for (int i = 0; i < testpoints.length; i += 2)
        {
            ImageUtil.getSmoothedPixel(testpoints[i], testpoints[i + 1], interpolated, tmp, newRaster);
            float expected = GeometryTest.eval(initF, testpoints[i], testpoints[i + 1], width, height);
            expected = expected * fusedMul[0] + fusedMul[1];
            assertEquals("interpolated at " + testpoints[i] + ' ' + testpoints[i + 1] + " was " + interpolated[0] + " expected " + expected, interpolated[0], expected, initF == GeometryTest.circleGenerator ? 3f : 1f);
        }
    }
}
