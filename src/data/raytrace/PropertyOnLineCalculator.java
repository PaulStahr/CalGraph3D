package data.raytrace;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import data.DataHandler;
import data.raytrace.RaytraceScene.RaySimulationObject;
import data.raytrace.raygen.RayGenerator;
import geometry.Geometry;
import geometry.Vector3d;
import io.Drawer;
import jcomponents.raytrace.RaySimulationData;
import maths.Controller;
import maths.Operation.CalculationController;
import maths.data.ArrayOperation;
import util.ArrayUtil;
import util.JFrameUtils;
import util.data.DoubleArrayList;

public class PropertyOnLineCalculator {
	private static final Logger logger = LoggerFactory.getLogger(PropertyOnLineCalculator.class);
	private final Intersection intersection = new Intersection();
	private RaytraceScene scene;
	public final DoubleArrayList dataPoints = new DoubleArrayList();

	public PropertyOnLineCalculator(RaytraceScene scene)
	{
		this.scene = scene;
	}

	public void paint(Vector3d position, Vector3d direction, double rangeBegin, double rangeEnd, int mode, boolean sphereArc, Drawer drawer)
	{
		dataPoints.clear();
		drawer.setColor(Color.BLACK);
		//ArrayList<IntersectionPoint> list = new ArrayList<>();
		ArrayList<OpticalObject> objects = new ArrayList<>();
		DoubleArrayList dal = new DoubleArrayList();
		OpticalObject nextSurfaces[] = scene.copyActiveSurfaces();
		OpticalObject nextVolumes[] = scene.getActiveVolumes();
		OpticalObject nextMeshes[] = scene.getActiveMeshes();
		intersection.distance = 1e-10;
		double width = drawer.getWidth(), height = drawer.getHeight();
		direction.normalize();
		switch (mode)
		{
			case 0:
			{
				while(true)
				{
					double lastDist = intersection.distance;
					intersection.distance = Double.POSITIVE_INFINITY;
					intersection.object = null;
					RaytraceScene.getNextIntersection(
								position,
								direction,
								intersection,
								lastDist + 1e-10,
								nextSurfaces,
								nextVolumes,
								nextMeshes);
					 dal.add(intersection.distance);
					 OpticalObject object = intersection.object;
					 if (object == null || objects.size() > 100)
					 {
						 break;
					 }
					 objects.add(intersection.object);
					 nextSurfaces = object.surfaceSuccessor;
					 nextVolumes = object.volumeSuccessor;
					 nextMeshes = object.meshSuccessor;
				}
				CalculationController control = new Controller();
				for (int i = 0; i < objects.size(); ++i)
				{
					OpticalObject obj = objects.get(i);
					if (obj instanceof OpticalSurfaceObject)
					{
						OpticalSurfaceObject oso = (OpticalSurfaceObject)obj;
						double ior0, ior1;
						if (oso.direction.dot(direction) < 0)
						{
							ior0 = oso.ior0.calculate(scene.vs, control).doubleValue();
							ior1 = oso.ior1.calculate(scene.vs, control).doubleValue();
						}
						else
						{
							ior0 = oso.ior1.calculate(scene.vs, control).doubleValue();
							ior1 = oso.ior0.calculate(scene.vs, control).doubleValue();
						}
						dataPoints.add(dal.getD(i));
						dataPoints.add(ior0);
						dataPoints.add(dal.getD(i));
						dataPoints.add(ior1);
						ior0 = (2 - ior0) * drawer.getHeight() / 2;
						ior1 = (2 - ior1) * drawer.getHeight() / 2;
						drawer.pushPoint(dal.getD(i)/4 + 500, ior0);
						drawer.pushPoint(dal.getD(i)/4 + 500, ior1);
					}
					else if (obj instanceof OpticalVolumeObject)
					{
						OpticalVolumeObject ovo = (OpticalVolumeObject)obj;
						double start = dal.getD(i);
						double steplength = (dal.getD(i + 1) - dal.getD(i))/99.;
						for (int j = 0; j < 100; ++j)
						{
							double mult = start + j * steplength;
							double ior = ovo.getRefractiveIndex(position.x + direction.x * mult, position.y + direction.y * mult, position.z + direction.z * mult);
							dataPoints.add(mult);
							dataPoints.add(ior);
							drawer.pushPoint(mult / 4 + 500, (2 - ior) * drawer.getHeight() / 2);
						}
					}
				}
				try {
					drawer.drawPolyLine();
				} catch (IOException ex) {
					JFrameUtils.logErrorAndShow("Can't update Visualisation", ex, logger);
				}
			break;
			}
			case 1:
			{
				int num_rays = 1000;
				int num_evaluations = 200;
				ArrayList<OpticalObject> oso = new ArrayList<>();
				scene.getActiveLights(oso);
				RayGenerator gen = new RayGenerator();
				RaySimulationObject rso = new RaySimulationObject();
				RaySimulationData rsd = new RaySimulationData(num_rays, false);
				for (int i = 0; i < oso.size(); ++i)
				{
					gen.threeDimensional = true;
					gen.setSource(oso.get(i));
					scene.calculateRays(0, num_rays, num_rays, gen, 0, 0, null, null, rsd.endpoints, rsd.enddirs, rsd.endcolor, null, rsd.accepted, rsd.bounces, rsd.lastObject, 10, false, rso, RaytraceScene.UNACCEPTED_MARK);

					double distances[] = ArrayUtil.fillEquidistant(rangeBegin, rangeEnd, new double[num_evaluations]);
					double result[] = sphereArc ? Geometry.getVarianceOnSphere(rsd.endpoints, rsd.enddirs, rsd.accepted, position, direction, distances, new double[num_evaluations])
							 : Geometry.getVariance(rsd.endpoints, rsd.enddirs, rsd.accepted, position, direction, distances, new double[num_evaluations]);
					ArrayUtil.sqrt(result, 0, result.length);
					double max = ArrayUtil.max(result);
					double multY = height / max;
					double multX = width / result.length;
					dataPoints.clear();
					for (int j = 0; j < result.length; ++j)
					{
						dataPoints.add(distances[j]);
						dataPoints.add(result[j]);
						drawer.pushPoint(j * multX, height - multY * result[j]);
					}
					try {
						drawer.drawPolyLine();
					} catch (IOException ex) {
						JFrameUtils.logErrorAndShow("Can't update Visualisation", ex, logger);
					}
					DataHandler.globalVariables.setGlobal("divergence", new ArrayOperation(result));
					System.out.println("result" + Arrays.toString(result));
				}
				break;
			}
		}
	}
}
