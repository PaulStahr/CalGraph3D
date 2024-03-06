package test.data.raytrace;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import data.raytrace.GuiOpticalSurfaceObject;
import data.raytrace.OpticalObject.SCENE_OBJECT_COLUMN_TYPE;
import data.raytrace.ParseUtil;
import data.raytrace.RaySimulation.MaterialType;
import data.raytrace.RaySimulation.SurfaceType;
import data.raytrace.RaytraceScene;
import data.raytrace.RaytraceScene.RaySimulationObject;
import geometry.Vector3d;
import maths.exception.OperationParseException;
import maths.variable.VariableStack;

public class OpticalSurfaceTest {
    @Test
    public void testHperbolic() throws OperationParseException {
        RaytraceScene rs = new RaytraceScene("Hyperbolic");
        ParseUtil parser = new ParseUtil();
        VariableStack va = new VariableStack();
        GuiOpticalSurfaceObject goso = new GuiOpticalSurfaceObject(va, parser);
        goso.setValue(SCENE_OBJECT_COLUMN_TYPE.SURFACE, SurfaceType.HYPERBOLIC, va, parser);
        goso.setValue(SCENE_OBJECT_COLUMN_TYPE.IOR0, 1, va,parser);
        goso.setValue(SCENE_OBJECT_COLUMN_TYPE.IOR1, Math.sqrt(2), va,parser);
        goso.setValue(SCENE_OBJECT_COLUMN_TYPE.DIRECTION, new Vector3d(1,0,0), va, parser);
        goso.setValue(SCENE_OBJECT_COLUMN_TYPE.MINRADIUS, 0, va, parser);
        goso.setValue(SCENE_OBJECT_COLUMN_TYPE.MAXRADIUS, 100, va, parser);
        goso.setValue(SCENE_OBJECT_COLUMN_TYPE.MATERIAL, MaterialType.REFRACTION, va, parser);
        goso.setValue(SCENE_OBJECT_COLUMN_TYPE.ACTIVE, true, va, parser);
        rs.add(goso);
        RaySimulationObject rso = new RaySimulationObject();
        Vector3d expectedPoint = new Vector3d(2+Math.sqrt(2),0,0);
        rs.updateScene();
        for (int i = 0; i< 10; ++i)
        {
            rso.position.set(-10,i/100.,0);
            rso.direction.set(1, 0,0);
            rso.numBounces = 0;
            rs.calculateRay(rso, 10, null, 0, rs.copyActiveSurfaces(), null, 0);
            rso.position.sub(expectedPoint);
            rso.position.add(rso.direction, -rso.direction.dot(rso.position)/rso.direction.dot());
            assertEquals(0,rso.position.norm(), 1E-5);
        }
    }

    @Test
    public void testSpherical() throws OperationParseException {
        RaytraceScene rs = new RaytraceScene("Spherical");
        ParseUtil parser = new ParseUtil();
        VariableStack va = new VariableStack();
        GuiOpticalSurfaceObject goso = new GuiOpticalSurfaceObject(va, parser);
        goso.setValue(SCENE_OBJECT_COLUMN_TYPE.SURFACE, SurfaceType.SPHERICAL, va, parser);
        goso.setValue(SCENE_OBJECT_COLUMN_TYPE.DIRECTION, new Vector3d(1,0,0), va, parser);
        goso.setValue(SCENE_OBJECT_COLUMN_TYPE.MINRADIUS, 0, va, parser);
        goso.setValue(SCENE_OBJECT_COLUMN_TYPE.MAXRADIUS, 100, va, parser);
        goso.setValue(SCENE_OBJECT_COLUMN_TYPE.MATERIAL, MaterialType.REFLECTION, va, parser);
        goso.setValue(SCENE_OBJECT_COLUMN_TYPE.ACTIVE, true, va, parser);
        rs.add(goso);
        RaySimulationObject rso = new RaySimulationObject();
        rs.updateScene();
        for (int i = 0; i< 10; ++i)
        {
            rso.position.set(-10,i/100.,0);
            Vector3d expectedPoint = new Vector3d(-Math.sqrt(1 - rso.position.y * rso.position.y),rso.position.y,0);
            rso.direction.set(1, 0,0);
            rso.numBounces = 0;
            rs.calculateRay(rso, 10, null, 0, rs.copyActiveSurfaces(), null, 0);
            rso.position.sub(expectedPoint);
            rso.position.add(rso.direction, -rso.direction.dot(rso.position)/rso.direction.dot());
            assertEquals(0,rso.position.norm(), 1E-5);
        }
    }

    @Test
    public void testParabolic() throws OperationParseException {
        RaytraceScene rs = new RaytraceScene("Parabolic");
        ParseUtil parser = new ParseUtil();
        VariableStack va = new VariableStack();
        GuiOpticalSurfaceObject goso = new GuiOpticalSurfaceObject(va, parser);
        goso.setValue(SCENE_OBJECT_COLUMN_TYPE.SURFACE, SurfaceType.HYPERBOLIC, va, parser);
        goso.setValue(SCENE_OBJECT_COLUMN_TYPE.DIRECTION, new Vector3d(1,0,0), va, parser);
        goso.setValue(SCENE_OBJECT_COLUMN_TYPE.MINRADIUS, 0, va, parser);
        goso.setValue(SCENE_OBJECT_COLUMN_TYPE.MAXRADIUS, 100, va, parser);
        goso.setValue(SCENE_OBJECT_COLUMN_TYPE.MATERIAL, MaterialType.REFLECTION, va, parser);
        goso.setValue(SCENE_OBJECT_COLUMN_TYPE.ACTIVE, true, va, parser);
        rs.add(goso);
        RaySimulationObject rso = new RaySimulationObject();
        Vector3d expectedPoint = new Vector3d(0.5,0,0);
        rs.updateScene();
        for (int i = 0; i< 10; ++i)
        {
            rso.position.set(-1,i/100.,0);
            rso.direction.set(1, -1e-4,0);
            rso.numBounces = 0;
            rs.calculateRay(rso, 1, null, 0, rs.copyActiveSurfaces(), null, 0);
            rso.position.sub(expectedPoint);
            rso.position.add(rso.direction, -rso.direction.dot(rso.position)/rso.direction.dot());
            assertEquals(0,rso.position.norm(), 4E-4);
        }
    }
}
