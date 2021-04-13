package test.data.raytrace;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

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

    public void testInversity(TextureMapping tm){
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
    public void testSphericalInverse() {
        testInversity(TextureMapping.SPHERICAL);
    }
    
    @Test
    public void testFisheyeHalfInverse() {
        testInversity(TextureMapping.FISHEYE_EQUIDISTANT_HALF);
    }
    
    @Test
    public void testFisheyeInverse() {
        testInversity(TextureMapping.FISHEYE_EQUIDISTANT);
    }

    public void testSurfaceElement(TextureMapping tm) {
        
    }
}
