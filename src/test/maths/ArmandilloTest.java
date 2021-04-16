package test.maths;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import maths.Armadillo;

@RunWith(Parameterized.class)
public class ArmandilloTest {
    @Parameters
    public static List<Integer> params() {return Arrays.asList(new Integer[]{0,1,2});}
    private final int dir;
    
    public ArmandilloTest(int dir) {
        this.dir = dir;
    }
    
    @Test
    public void testDiffusionEquation(){
        int size = 10;
        int width  = dir == 0 ? size : 1;
        int height = dir == 1 ? size : 1;
        int depth  = dir == 2 ? size : 1;
        double data[] = new double[width * height * depth];
        boolean isGiven[] = new boolean[width * height * depth];
        isGiven[0] = isGiven[isGiven.length - 1] = true;
        Arrays.fill(data, 1, data.length - 1, 1);
        Armadillo.solveDiffusionEquation(width, height, depth, data, isGiven);
        for (int i = 0; i < data.length; ++i)
        {
            double expected = 4.5 * 4.5 * 0.5 - 0.5 * (i - 4.5) * (i - 4.5);
            assertTrue("expected " + expected + "got data[" + i + "]=" + data[i], Math.abs(expected - data[i]) < 0.01);
        }
    }
    
}
