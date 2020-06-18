package data.raytrace;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import data.DataHandler;
import data.raytrace.OpticalObject.SCENE_OBJECT_COLUMN_TYPE;
import data.raytrace.RaytraceScene.RaySimulationObject;
import data.raytrace.raygen.RayGenerator;
import geometry.Geometry.NearestPointCalculator;
import geometry.Matrix4d;
import geometry.Vector2d;
import geometry.Vector3d;
import jcomponents.raytrace.RaySimulationData;
import maths.exception.OperationParseException;
import util.RunnableRunner;
import util.RunnableRunner.ParallelRangeRunnable;
import util.data.DoubleArrayList;

public class FocusAnalysis {
	private static final Logger logger = LoggerFactory.getLogger(FocusAnalysis.class);
	private Runnable finishRunnable;
	int maxBounces = 10;
	public GuiOpticalSurfaceObject lightSource;
	public int raycount;
	public double sourceElevations[];
	public double destinationEucledeanVariance[];
	public double acceptedRatio[];
	public double focalDistances[];
	public double focalHitpointDistances[];
	public double destinationElevationAveraged[];
	int acceptedRayCounts[];
	public double destinationElevationVariance[];
	double azimuths[][];
	int startIndex[];
	public double vertices[];
	int numElevations = 100;
	public boolean threeDim;
	public int[] faces;
	public RaytraceScene scene;
	public OpticalSurfaceObject destination;
	public int width;
	public int height;
	public float[] pixelVariance;
	public int[] pixelCount;
	public boolean wait = false;

	public void setFinishRunnable(Runnable runnable)
	{
		this.finishRunnable = runnable;
	}
	
	public void run()
	{
		sourceElevations = new double[numElevations];
		destinationEucledeanVariance = new double[numElevations];
		acceptedRatio = new double[numElevations];
		acceptedRatio = new double[numElevations];
		focalDistances = new double[numElevations];
		focalHitpointDistances = new double[numElevations];
		destinationElevationAveraged = new double[numElevations];
		acceptedRayCounts = new int[numElevations];
		destinationElevationVariance = new double[numElevations];
		azimuths = new double[numElevations][];
		startIndex = new int[numElevations + 1];
		if (lightSource == null)
		{
			throw new NullPointerException("No light Source");
		}
		if (destination == null)
		{
			throw new NullPointerException("No Destination");
		}
		final double multElevation = lightSource.getMaxArcOpen() / numElevations;
		for (int i = 0; i < numElevations; ++i)
		{
			double elevation = i * multElevation;
			sourceElevations[i] = elevation;
			int jsteps = threeDim ? 1 + (int)(Math.sin(elevation) * numElevations) : 2;
			azimuths[i] = new double[jsteps];
			double jstep = Math.PI * 2 / jsteps;
			
			for (int j = 0; j < jsteps; ++j)
			{
				azimuths[i][j] = j * jstep;
			}
			startIndex[i + 1] = startIndex[i] + jsteps;
		}
		pixelVariance = new float[width * height];
		pixelCount = new int[width * height];
		vertices = new double[startIndex[numElevations] * 3];
		ParallelRangeRunnable prr = new RunnableRunner.ParallelRangeRunnable() {
			

			@Override
			public void run(int from, int to) {
				Vector3d position = new Vector3d();
				Vector3d direction = new Vector3d();
				Vector2d tc = new Vector2d();
				RaySimulationData rsd = new RaySimulationData(raycount, false);
				RaySimulationObject currentRay = new RaySimulationObject();
				Vector3d bundleWeightPoint = new Vector3d();
				RayGenerator gen = new RayGenerator();
				gen.threeDimensional = true;
				gen.setSource(lightSource);
				DoubleArrayList destinationElevations = new DoubleArrayList();
				
				NearestPointCalculator npc = new NearestPointCalculator(3);
				Vector3d focalPoint = new Vector3d();
				for (int i = from; i < to; ++i)
				{
					double lineVariance = 0;
					double rayLineFocalHitpointDistance = 0;
					double lineFocalDistance = 0;
					int lineAcceptedCount = 0;
					destinationElevations.clear();
					for (int j = 0; j < azimuths[i].length; ++j)
					{
						Arrays.fill(rsd.lastObject, null);//TODO popably much unecessary memory
						bundleWeightPoint.set(0,0,0);
						gen.setArcs(sourceElevations[i], azimuths[i][j]);
						scene.calculateRays(0, raycount, raycount, gen, 0, 0, null, null, rsd.endpoints, rsd.enddirs, rsd.endcolor, null, rsd.accepted, rsd.bounces, rsd.lastObject, maxBounces, false, currentRay, RaytraceScene.UNACCEPTED_DELETE);
						
						int bundleAcceptedCount = 0;
						npc.reset();
						for (int k = 0; k < raycount; ++k)
						{
							if (rsd.lastObject[k] == destination && rsd.accepted[k] == RaytraceScene.STATUS_ACCEPTED)
							{
								bundleWeightPoint.add(rsd.endpoints, k * 3);
								npc.addPoint(rsd.endpoints, rsd.enddirs, k * 3);
								++bundleAcceptedCount;
							}
						}
						npc.calculate();
						npc.get(focalPoint);
						if (bundleAcceptedCount == 0){focalPoint.set(Double.NaN, Double.NaN, Double.NaN);}
						focalPoint.write(vertices, (startIndex[i] + j) * 3);
						lineFocalDistance += destination.midpoint.distance(focalPoint);
						lineAcceptedCount += bundleAcceptedCount;
						bundleWeightPoint.multiply(1/(double)bundleAcceptedCount);
						double bundleVariance = 0;
						if (bundleAcceptedCount == 0){continue;}
						for (int k = 0; k < raycount; ++k)
						{
							if (rsd.lastObject[k] == destination && rsd.accepted[k] == RaytraceScene.STATUS_ACCEPTED)
							{
								rayLineFocalHitpointDistance += focalPoint.distance(rsd.endpoints, k * 3);
								bundleVariance += bundleWeightPoint.distanceQ(rsd.endpoints, k * 3);
							}
						}
						bundleVariance /= bundleAcceptedCount;
						System.out.print(new StringBuilder().append('(').append(bundleVariance).append(' ').append(bundleAcceptedCount).append(')'));

						for (int k = 0; k < raycount; ++k)
						{
							if (rsd.lastObject[k] == destination && rsd.accepted[k] == RaytraceScene.STATUS_ACCEPTED)
							{							
								position.set(rsd.endpoints, k * 3);
								direction.set(rsd.enddirs,  k * 3);
								destination.getTextureCoordinates(position, direction, tc);
								destinationElevations.add(destination.directionNormalized.acosDistance(position, destination.midpoint));
								/*{
									double dx = tc.x - 0.5, dy = tc.y - 0.5;
									double elev = Math.sqrt(dx * dx + dy * dy) * (2 * Math.PI);
									if (Math.abs(dal.getD(dal.size() - 1) - elev) > 0.01)
									{
										System.out.println("Warning :" + dal.getD(dal.size() - 1) + "!=" + elev);
									}
								}*/
								int x = (int)(tc.x * width);
								int y = (int)(tc.y * height);
								int pixelIndex = y * width + x;
								++pixelCount[pixelIndex];
								pixelVariance[pixelIndex] += bundleVariance;
							}
						}
						lineVariance += bundleVariance;
					}
					acceptedRayCounts[i] = lineAcceptedCount;
					focalHitpointDistances[i] = rayLineFocalHitpointDistance / lineAcceptedCount;
					destinationEucledeanVariance[i] = lineVariance / azimuths[i].length;
					acceptedRatio[i] = (double)lineAcceptedCount / azimuths[i].length;
					focalDistances[i] = lineFocalDistance / azimuths[i].length;
					destinationElevationAveraged[i] = destinationElevations.average();
					destinationElevationVariance[i] = destinationElevations.diffSumQ(destinationElevationAveraged[i]) / destinationElevations.size();
					System.out.println();
				}
			}
			
			@Override
			public void finished()
			{
				faces = new int[startIndex[startIndex.length - 1] * 6 - (azimuths[0].length + azimuths[azimuths.length - 1].length) * 3] ;
				for (int i = 0, f = 0; i < azimuths.length - 1; ++i)
				{
					double azimuthsl[] = azimuths[i];
					double azimuthsu[] = azimuths[i + 1];
					int sil = startIndex[i];
					int siu = startIndex[i + 1];
					int j = 0,k = 0;
					while (j < azimuthsl.length && k < azimuthsu.length)
					{
						if (azimuthsl[j] < azimuthsu[k])
						{
							faces[f++] = sil + j;
							faces[f++] = sil + (++j) % azimuthsl.length;
							faces[f++] = siu + k;
						}
						else
						{
							faces[f++] = sil + j;
							faces[f++] = siu + k;
							faces[f++] = siu + (++k) % azimuthsu.length;
						}
					}
					if (j == azimuthsl.length)
					{
						while (k < azimuthsu.length)
						{
							faces[f++] = sil;
							faces[f++] = siu + k;
							faces[f++] = siu + (++k) % azimuthsu.length;
						}
					}
					if (k == azimuthsu.length)
					{
						while (j < azimuthsl.length)
						{
							faces[f++] = sil + j;
							faces[f++] = sil + (++j) % azimuthsl.length;
							faces[f++] = siu;
						}
					}
				}
				if (finishRunnable != null)
				{
					finishRunnable.run();
				}
			}
		};
		if (wait)
		{
			DataHandler.runnableRunner.runParallelAndWait(prr, "Focus Heatmap", null, 0, numElevations, 10);
		}
		else
		{
			DataHandler.runnableRunner.runParallel(prr, "Focus Heatmap", null, 0, numElevations, 10);
		}
	}

	public MeshObject createMeshObject() {
		ParseUtil parser = new ParseUtil();
		MeshObject mo = new MeshObject(scene.vs, parser);
		mo.setData(vertices, faces, null);
		if (!threeDim)
		{
			int lines[] = new int[2 * vertices.length / 3 - 4];
			for (int i = 0; i < lines.length / 2; ++i)
			{
				lines[i * 2] = i;
				lines[i * 2 + 1] = i + 2;
			}
			mo.setLines(lines);
		}
		try {
			mo.setValue(SCENE_OBJECT_COLUMN_TYPE.TRANSFORMATION, new Matrix4d(1), scene.vs, parser);
		} catch (OperationParseException e) {
			logger.error("Can't set Value", e);
		}
		return mo;
	}
}
