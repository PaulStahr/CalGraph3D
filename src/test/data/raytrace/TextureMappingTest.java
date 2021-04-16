package test.data.raytrace;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import data.raytrace.TextureMapping;
import geometry.Vector2d;
import geometry.Vector3d;

public class TextureMappingTest {
    @Test
    public void testFisheyeEquidistant() {
        TextureMapping tm = TextureMapping.FISHEYE_EQUIDISTANT;
        Vector3d out = new Vector3d();
        tm.mapTexToCart(0.5, 0.5, out);
        Vector3d expected = new Vector3d(0,0,1);
        assertTrue("expected " + expected + " got " + out, out.distanceQ(expected) < 0.01);
        tm.mapTexToCart(0, 0.5, out);
        expected.set(0,0,-1);
        assertTrue("expected " + expected + " got " + out, out.distanceQ(expected) < 0.01);
    }

    @Test
    public void testFisheyeEquidistantHalf() {
        TextureMapping tm = TextureMapping.FISHEYE_EQUIDISTANT_HALF;
        Vector3d out = new Vector3d();
        tm.mapTexToCart(0.5, 0.5, out);
        Vector3d expected = new Vector3d(0,0,1);
        assertTrue("expected " + expected + " got " + out, out.distanceQ(expected) < 0.01);
        tm.mapTexToCart(0, 0.5, out);
        expected.set(-1,0,0);
        assertTrue("expected " + expected + " got " + out, out.distanceQ(expected) < 0.01);
    }
    
    @RunWith(Parameterized.class)
    public static class SpecificTests{
        @Parameters
        public static List<TextureMapping> params() {return Arrays.asList(TextureMapping.values());}
        
        private final TextureMapping tm;
        
        public SpecificTests(TextureMapping tm) {this.tm = tm;}

        @Override
        public String toString() {return super.toString() + tm.name;}

        @Test
        public void testInversity(){
            Vector2d textureIn = new Vector2d();
            Vector2d textureOut = new Vector2d();
            Vector3d cartesian = new Vector3d();
            for (int i = -1; i < 2; ++i)
            {
                for (int j = -1; j < 2; ++j)
                {
                    if (Math.abs(j) + Math.abs(i) < 2)
                    {
                        textureIn.set(i * 0.45 + 0.5, j * 0.45 + 0.5);
                        tm.mapTexToCart(textureIn.x, textureIn.y, cartesian);
                        tm.mapCartToTex(cartesian.x, cartesian.y, cartesian.z, textureOut);
                        assertTrue("expected " + textureIn + " got " + cartesian + " -> " + textureOut, textureOut.distanceQ(textureIn) < 0.01);
                    }
                }
            }
        }

        @Test
        public void testSurfaceElementTransform() {
            double eps = 0.00001;
            Vector2d textureIn = new Vector2d();
            Vector3d cartesian = new Vector3d();
            Vector3d cartesianDelta0 = new Vector3d();
            Vector3d cartesianDelta1 = new Vector3d();
            for (int i = -1; i < 2; ++i)
            {
                for (int j = -1; j < 2; ++j)
                {
                    if (Math.abs(j) + Math.abs(i) < 2)
                    {
                        textureIn.set(i * 0.45 + 0.5, j * 0.45 + 0.5);
                        double surfaceElement = tm.mapTexToCart(textureIn.x, textureIn.y, cartesian);
                        tm.mapTexToCart(textureIn.x + eps, textureIn.y, cartesianDelta0);
                        tm.mapTexToCart(textureIn.x, textureIn.y + eps, cartesianDelta1);
                        cartesianDelta0.sub(cartesian);
                        cartesianDelta1.sub(cartesian);
                        cartesian.cross(cartesianDelta0, cartesianDelta1);
                        double approximatedSurfaceElement = cartesian.norm() / (eps * eps);
                        assertTrue(tm.name + " texture position " + textureIn + " expected " + approximatedSurfaceElement + " got " + surfaceElement, Math.abs(approximatedSurfaceElement - surfaceElement) < 0.001);
                    }
                }
            }
        }
        
        @Test
        public void testSurfaceElementSingle() {
            double eps = 0.00001;
            Vector2d textureIn = new Vector2d();
            Vector3d cartesian = new Vector3d();
            Vector3d cartesianDelta0 = new Vector3d();
            Vector3d cartesianDelta1 = new Vector3d();
            for (int i = -1; i < 2; ++i)
            {
                for (int j = -1; j < 2; ++j)
                {
                    if (Math.abs(j) + Math.abs(i) < 2)
                    {
                        textureIn.set(i * 0.45 + 0.5, j * 0.45 + 0.5);
                        double surfaceElement = tm.mapTexToCart(textureIn.x, textureIn.y);
                        tm.mapTexToCart(textureIn.x, textureIn.y, cartesian);
                        tm.mapTexToCart(textureIn.x + eps, textureIn.y, cartesianDelta0);
                        tm.mapTexToCart(textureIn.x, textureIn.y + eps, cartesianDelta1);
                        cartesianDelta0.sub(cartesian);
                        cartesianDelta1.sub(cartesian);
                        cartesian.cross(cartesianDelta0, cartesianDelta1);
                        double approximatedSurfaceElement = cartesian.norm() / (eps * eps);
                        assertTrue(tm.name + " texture position " + textureIn + " expected " + approximatedSurfaceElement + " got " + surfaceElement, Math.abs(approximatedSurfaceElement - surfaceElement) < 0.001);
                    }
                }
            }
        }
    }
}
