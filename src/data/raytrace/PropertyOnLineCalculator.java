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
import geometry.Geometry.NearestPointCalculator;
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
	private RaytraceScene scene;
	public final DoubleArrayList dataPoints = new DoubleArrayList();

	public static enum VisualizationMode
	{
	    REFRACTIVE_INDEX, FOCAL_DISTANCE, DIVERGENCE;
	}


	public PropertyOnLineCalculator(RaytraceScene scene)
	{
		this.scene = scene;
	}

	public void paint(Vector3d position, Vector3d direction, double rangeBegin, double rangeEnd, VisualizationMode mode, boolean sphereArc, Drawer drawer)
	{
		dataPoints.clear();
		drawer.setColor(Color.BLACK);
		//ArrayList<IntersectionPoint> list = new ArrayList<>();
		ArrayList<OpticalObject> objects = new ArrayList<>();
		DoubleArrayList rayObjectIntersectionDistances = new DoubleArrayList();
		DoubleArrayList focusIntersectionDistances = new DoubleArrayList();
		OpticalObject nextSurfaces[] = scene.copyActiveSurfaces();
		OpticalObject nextVolumes[] = scene.getActiveVolumes();
		OpticalObject nextMeshes[] = scene.getActiveMeshes();

        RaytraceScene.RaySimulationObject ray = new RaytraceScene.RaySimulationObject();
        Intersection intersection = ray.nearest;
		intersection.distance = 1e-10;
		double width = drawer.getWidth(), height = drawer.getHeight();
		direction.normalize();
		//Get 16 additional positions in a circle around position on the normal plane of direction
		Vector3d[] positions = new Vector3d[16];
		Vector3d[] orthonormals = new Vector3d[2];
        for (int i = 0; i < 2; ++i)
        {
            orthonormals[i] = new Vector3d();
        }

		Geometry.getOrthorgonalVectors(direction, orthonormals[0], orthonormals[1]);
		for (int i = 0; i < 16; ++i)
        {
            double angle = i * Math.PI / 8;
            positions[i] = new Vector3d(position);
            positions[i].add(orthonormals[0], Math.cos(angle));
            positions[i].add(orthonormals[1], Math.sin(angle));
        }
		switch (mode)
		{
		    case REFRACTIVE_INDEX:
            {
			    NearestPointCalculator npc = new NearestPointCalculator(3);
                Vector3d refracted_direction = new Vector3d();
                Vector3d focalPoint = new Vector3d();
				while(true)
				{
					double lastDist = intersection.distance;
					for (int i = 0; i < 16; ++i)
                    {
                        intersection.distance = Double.POSITIVE_INFINITY;
                        intersection.object = null;
                        RaytraceScene.getNextIntersection(
                                positions[i],
                                direction,
                                intersection,
                                lastDist + 1e-10,
                                nextSurfaces,
                                nextVolumes,
                                nextMeshes);
                        if (intersection.object instanceof SurfaceObject)
                        {
                            refracted_direction.set(direction);
                            scene.apply_surface_to_ray(ray, null, -1, null, -1, intersection, refracted_direction);
                            npc.addRay(intersection.position, refracted_direction, 0);
                        }
                    }
                    npc.calculate();
                    npc.get(focalPoint);
                    focusIntersectionDistances.add(direction.dot(focalPoint));

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
					rayObjectIntersectionDistances.add(intersection.distance);
					OpticalObject object = intersection.object;
					if (object == null || objects.size() > 100)
					{
					     break;
					}
					objects.add(object);

					nextSurfaces = object.surfaceSuccessor;
					nextVolumes = object.volumeSuccessor;
					nextMeshes = object.meshSuccessor;
					npc.reset();
				}
				CalculationController control = new Controller();
				System.out.println(Arrays.toString(objects.toArray(new OpticalObject[objects.size()])));
				System.out.println(Arrays.toString(focusIntersectionDistances.toArrayD()));
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
						double xLoc = rayObjectIntersectionDistances.getD(i);
						dataPoints.add(xLoc);
						dataPoints.add(ior0);
						dataPoints.add(xLoc);
						dataPoints.add(ior1);
						ior0 = (2 - ior0) * drawer.getHeight() / 2;
						ior1 = (2 - ior1) * drawer.getHeight() / 2;
						xLoc = (xLoc - rangeBegin) / (rangeEnd - rangeBegin) * width;
						drawer.pushPoint(xLoc, ior0);
						drawer.pushPoint(xLoc, ior1);
					}
					else if (obj instanceof OpticalVolumeObject)
					{
						OpticalVolumeObject ovo = (OpticalVolumeObject)obj;
						double start = rayObjectIntersectionDistances.getD(i);
						double steplength = (rayObjectIntersectionDistances.getD(i + 1) - start)/99.;
						for (int j = 0; j < 100; ++j)
						{
							double mult = start + j * steplength;
							double ior = ovo.getRefractiveIndex(position.x + direction.x * mult, position.y + direction.y * mult, position.z + direction.z * mult);
							dataPoints.add(mult);
							dataPoints.add(ior);
							double xLoc = mult;
							xLoc = (xLoc - rangeBegin) / (rangeEnd - rangeBegin) * width;
							drawer.pushPoint(xLoc, (2 - ior) * drawer.getHeight() / 2);
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
            case FOCAL_DISTANCE:
            {
                int num_rays = 3000;
                int num_evaluations = 200;
                ArrayList<OpticalObject> oso = new ArrayList<>();
                scene.getActiveLights(oso);
                RayGenerator gen = new RayGenerator();
                RaySimulationObject rso = new RaySimulationObject();
                RaySimulationData rsd = new RaySimulationData(num_rays, false);
                Vector3d npcDir = new Vector3d();
                Vector3d npcPos = new Vector3d();
                Vector3d npcCenter = new Vector3d();
                int maxBounces = 30;
                for (int i = 0; i < oso.size(); ++i)
                {
                    gen.threeDimensional = true;
                    gen.setSource(oso.get(i));
                    double[] trajectory = new double[num_rays * (maxBounces + 2) * 3];
                    Arrays.fill(trajectory, Double.NaN);
                    scene.calculateRays(0, num_rays, num_rays, gen, 0, 0, null, null, rsd.endpoints, rsd.enddirs, rsd.endcolor, trajectory , rsd.accepted, rsd.bounces, rsd.lastObject, maxBounces, false, rso, RaytraceScene.UNACCEPTED_MARK);
                    double distances[] = ArrayUtil.fillEquidistant(rangeBegin, rangeEnd, new double[num_evaluations]);
                    final NearestPointCalculator npc = new NearestPointCalculator(3);
                    double result[] = new double[distances.length];
                    double trajectoryDistances[][] = new double[num_rays][];

                    for (int iRay = 0; iRay < num_rays; ++iRay)
                    {
                        double tmp[] = new double[maxBounces * 3 + 6];
                        for (int iBounce = 0; iBounce < maxBounces; ++iBounce)
                        {
                            tmp[iBounce] = direction.dot(trajectory, (iRay * (maxBounces + 2) + iBounce) * 3);
                            if (Double.isNaN(tmp[iBounce]) || iBounce + 1== maxBounces)
                            {
                                trajectoryDistances[iRay] = Arrays.copyOf(tmp, iBounce);
                                break;
                            }
                        }
                    }
                    dataPoints.clear();

                    for (int iDist = 0; iDist < distances.length; ++iDist)
                    {
                        for (int iRay = 0; iRay < num_rays; ++iRay)
                        {
                            int idx = Arrays.binarySearch(trajectoryDistances[iRay], distances[iDist]);
                            if (idx < 0){
                                idx = -idx - 1;
                            }
                            if (idx >= trajectoryDistances[iRay].length || idx < 1)
                            {
                                continue;
                            }
                            if (distances[iDist] < trajectoryDistances[iRay][idx - 1] || distances[iDist] > trajectoryDistances[iRay][idx])
                            {
                                throw new RuntimeException();
                            }
                            npcDir.set(trajectory, (iRay * (maxBounces + 2) + idx) * 3);
                            npcPos.set(trajectory, (iRay * (maxBounces + 2) + idx - 1) * 3);
                            npcDir.sub(npcPos);
                            npcDir.normalize();
                            //System.out.println(npcDir + " " + npcPos);
                            npc.addRay(npcPos, npcDir, 0);
                        }

                        dataPoints.add(distances[iDist]);
                        if (npc.calculate() == 3 && npc.getUniformness() < 0.999)
                        {
                            npc.get(npcCenter);
                            //System.out.println(npcCenter);
                            result[iDist] = direction.dot(npcCenter);
                        }
                        else
                        {
                            result[iDist] = Double.NaN;
                        }

                        dataPoints.add(result[iDist]);
                        npc.reset();
                    }
                    double max = ArrayUtil.max(result);
                    double multY = height / max;
                    double multX = width / result.length;
                    for (int iDist = 0; iDist < distances.length; ++iDist)
                    {
                        drawer.pushPoint(iDist * multX, height - multY * result[iDist]);
                    }
                    try {
                        drawer.drawPolyLine();
                    } catch (IOException ex) {
                        JFrameUtils.logErrorAndShow("Can't update Visualisation", ex, logger);
                    }
                    DataHandler.globalVariables.setGlobal("focalpoints", new ArrayOperation(result));
                    System.out.println("result" + Arrays.toString(result));
                }
                break;
            }
			case DIVERGENCE:
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
