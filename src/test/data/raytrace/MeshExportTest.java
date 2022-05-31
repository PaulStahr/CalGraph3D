package test.data.raytrace;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import data.raytrace.GuiOpticalSurfaceObject;
import data.raytrace.OpticalObject.SCENE_OBJECT_COLUMN_TYPE;
import data.raytrace.OpticalSurfaceObject;
import data.raytrace.ParseUtil;
import data.raytrace.RaySimulation.SurfaceType;
import geometry.Vector3d;
import maths.exception.OperationParseException;
import maths.variable.VariableStack;

@RunWith(Parameterized.class)
public class MeshExportTest{
    static final Logger logger = LoggerFactory.getLogger(MeshExportTest.class);
    @Parameters
    public static List<OpticalSurfaceObject> params() {
        ArrayList<OpticalSurfaceObject> surfaces = new ArrayList<>();
        ParseUtil p = new ParseUtil();
        VariableStack vs = new VariableStack();
        for (SurfaceType st : SurfaceType.values())
        {
            for (int i = 1; i <= 3; ++i)
            {
                for (int j = 0; j == 0 || (j < 3 && st == SurfaceType.CUSTOM); ++j)
                {
                    OpticalSurfaceObject oso = new GuiOpticalSurfaceObject(vs, p);
                    try {
                        oso.setValue(SCENE_OBJECT_COLUMN_TYPE.SURFACE, st, vs, p);
                        oso.setValue(SCENE_OBJECT_COLUMN_TYPE.MINRADIUS, 5, vs, p);
                        oso.setValue(SCENE_OBJECT_COLUMN_TYPE.MAXRADIUS, 10, vs, p);
                        oso.setValue(SCENE_OBJECT_COLUMN_TYPE.DIRECTION, new Vector3d(i,0,0), vs, p);
                        oso.setValue(SCENE_OBJECT_COLUMN_TYPE.CONIC_CONSTANT, -j - 1, vs, p);
                    } catch (OperationParseException | NumberFormatException e) {
                        logger.error("Exception while setting value", e);
                    }
                    surfaces.add(oso);
                }
            }
        }
        return surfaces;
    }
    final OpticalSurfaceObject oso;
    public MeshExportTest(OpticalSurfaceObject oso)
    {
        this.oso = oso;
    }


    @Test
    public void testMeshExport()
    {
        float vertices[] = new float[oso.getMeshVertexCount(11, 11) * 3];
        oso.getMeshVertices(11, 11, vertices);
        Vector3d v = new Vector3d();
        for (int i = 0; i < vertices.length; i += 3)
        {
            v.set(vertices, i);
            double inOut = oso.evaluate_inner_outer(v);
            assertEquals("Evaluation at " + v + " for " + oso.surf + " doesn't match", 0, inOut, 0.001);
        }
    }
}
