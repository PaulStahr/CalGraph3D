package test.geometry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import geometry.Geometry.NearestPointCalculator;
import geometry.Vector3d;

@RunWith(Parameterized.class)
public class NearestPointCalculatorTest {
    @Parameters
    public static List<Vector3d> params() {return Arrays.asList(new Vector3d(0,0,0),new Vector3d(1,0,0),new Vector3d(0,1,0),new Vector3d(0,0,100));}

    private final Vector3d translate;

    public NearestPointCalculatorTest(Vector3d translate) {
        this.translate = translate;
    }

    @Test
    public void testNearestPointCalculatorArray() {
        NearestPointCalculator npc = new NearestPointCalculator(3);
        npc.addRay(new Vector3d(0,1,-2).add(translate).toArrayF(), new float[] {0,1,2},0);
        npc.addRay(new Vector3d(2,1,-1).add(translate).toArrayF(), new float[] {0,1,1},0);
        assertEquals(3,npc.calculate());
        Vector3d result = new Vector3d();
        Vector3d expected = new Vector3d(1,2,0).add(translate);
        npc.get(result);
        assertTrue("expected: " + expected + " result " + result, expected.distance(result)<0.01);
    }


    @Test
    public void testNearestPointCalculatorVector() {
        NearestPointCalculator npc = new NearestPointCalculator(3);
        npc.addRay(new Vector3d(0,1,-2).add(translate), new Vector3d(0,1,2),0);
        npc.addRay(new Vector3d(2,1,-1).add(translate), new Vector3d(0,1,1),0);
        assertEquals(3,npc.calculate());
        Vector3d result = new Vector3d();
        Vector3d expected = new Vector3d(1,2,0).add(translate);
        npc.get(result);
        assertTrue("expected: " + expected + " result " + result, expected.distance(result)<0.01);
    }

    @Test
    public void testNearestPointCalculatorCross() {
        NearestPointCalculator npc = new NearestPointCalculator(3);
        npc.addRay(translate, new Vector3d(1,0,0),0);
        npc.addRay(translate, new Vector3d(0,1,0),0);
        npc.addRay(translate, new Vector3d(0,0,1),0);
        assertEquals(3,npc.calculate());
        Vector3d result = new Vector3d();
        Vector3d expected = translate;
        npc.get(result);
        assertTrue("expected: " + expected + " result " + result, expected.distance(result)<0.01);
    }
}
