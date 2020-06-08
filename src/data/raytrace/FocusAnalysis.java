package data.raytrace;

import java.util.Arrays;

import data.DataHandler;
import data.raytrace.RaytraceScene.RaySimulationObject;
import data.raytrace.raygen.RayGenerator;
import geometry.Geometry.NearestPointCalculator;
import geometry.Vector2d;
import geometry.Vector3d;
import jcomponents.raytrace.RaySimulationData;
import util.RunnableRunner;
import util.RunnableRunner.ParallelRangeRunnable;
import util.data.DoubleArrayList;

public class FocusAnalysis {
	
	private Runnable finishRunnable;
	int maxBounces = 10;
	public GuiOpticalSurfaceObject lightSource;
	public int raycount;
	public double elevations[];
	public double avaragedByIncomingArc[];
	public double avarageCountPerArc[];
	public double focalDistance[];
	public double focalHitpointDistance[];
	public double surfaceElevation[];
	int acceptedRayCount[];
	public double surfaceElevationVariance[];
	double azimuths[][];
	int startIndex[] = new int[101];
	public double vertices[] = new double[startIndex[startIndex.length - 1] * 3];
	int numElevations = 100;
	public boolean threeDim;
	public int[] faces;
	public RaytraceScene scene;
	public OpticalSurfaceObject endpoint;
	public int width;
	public int height;
	public float[] values;
	public int[] pixelCount;
	public boolean wait = false;

	public void setFinishRunnable(Runnable runnable)
	{
		this.finishRunnable = runnable;
	}
	
	public void run()
	{
		elevations = new double[numElevations];
		avaragedByIncomingArc = new double[numElevations];
		avarageCountPerArc = new double[numElevations];
		avarageCountPerArc = new double[numElevations];
		focalDistance = new double[numElevations];
		focalHitpointDistance = new double[numElevations];
		surfaceElevation = new double[numElevations];
		acceptedRayCount = new int[numElevations];
		surfaceElevationVariance = new double[numElevations];
		azimuths = new double[numElevations][];
		startIndex = new int[numElevations + 1];
		if (lightSource == null)
		{
			throw new NullPointerException("No light Source");
		}
		final double multElevation = lightSource.getMaxArcOpen() / numElevations;
		for (int i = 0; i < numElevations; ++i)
		{
			double elevation = i * multElevation;
			elevations[i] = elevation;
			int jsteps = threeDim ? 1 + (int)(Math.sin(elevation) * numElevations) : 2;
			azimuths[i] = new double[jsteps];
			double jstep = Math.PI * 2 / jsteps;
			
			for (int j = 0; j < jsteps; ++j)
			{
				azimuths[i][j] = j * jstep;
			}
			startIndex[i + 1] = startIndex[i] + jsteps;
		}
		values = new float[width * height];
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
				Vector3d weightPoint = new Vector3d();
				RayGenerator gen = new RayGenerator();
				gen.threeDimensional = true;
				gen.setSource(lightSource);
				DoubleArrayList dal = new DoubleArrayList();
				
				NearestPointCalculator npc = new NearestPointCalculator(3);
				Vector3d focalPoint = new Vector3d();
				for (int i = from; i < to; ++i)
				{
					int countPerArc = 0;
					dal.clear();
					for (int j = 0; j < azimuths[i].length; ++j)
					{
						Arrays.fill(rsd.lastObject, null);
						weightPoint.set(0,0,0);
						gen.setArcs(elevations[i], azimuths[i][j]);
						scene.calculateRays(0, raycount, raycount, gen, 0, 0, null, null, rsd.endpoints, rsd.enddirs, null, null, rsd.accepted, rsd.bounces, rsd.lastObject, maxBounces, false, currentRay, RaytraceScene.UNACCEPTED_DELETE);
						
						int count = 0;
						npc.reset();
						for (int k = 0; k < raycount; ++k)
						{
							if (rsd.lastObject[k] == endpoint && rsd.accepted[k] == RaytraceScene.STATUS_ACCEPTED)
							{
								weightPoint.add(rsd.endpoints, k * 3);
								npc.addPoint(rsd.endpoints, rsd.enddirs, k * 3);
								++count;
							}
						}
						npc.calculate();
						npc.get(focalPoint);
						focalPoint.write(vertices, (startIndex[i] + j) * 3);
						focalDistance[i] += Math.sqrt(endpoint.midpoint.distanceQ(focalPoint));
						countPerArc += count;
						weightPoint.multiply(1/(double)count);
						double variance = 0;
						for (int k = 0; k < raycount; ++k)
						{
							if (rsd.lastObject[k] == endpoint && rsd.accepted[k] == RaytraceScene.STATUS_ACCEPTED)
							{
								focalHitpointDistance[i] += Math.sqrt(focalPoint.distanceQ(rsd.endpoints, k * 3));
								double dist = weightPoint.distanceQ(rsd.endpoints, k * 3);
								variance += dist;
								avaragedByIncomingArc[i] += dist;
							}
						}
						if (count == 0)
						{
							continue;
						}
						variance = Math.sqrt(variance / count);
						System.out.print(new StringBuilder().append('(').append(variance).append(' ').append(count).append(')'));

						for (int k = 0; k < raycount; ++k)
						{
							if (rsd.lastObject[k] == endpoint && rsd.accepted[k] == RaytraceScene.STATUS_ACCEPTED)
							{							
								position.set(rsd.endpoints, k * 3);
								direction.set(rsd.enddirs,  k * 3);
								endpoint.getTextureCoordinates(position, direction, tc);
								double diffx = tc.x - 0.5, diffy = tc.y - 0.5;
								dal.add(Math.sqrt(diffx * diffx + diffy * diffy) * (2 * Math.PI));
								//System.out.println(position + ' ' + tc);
								int x = (int)(tc.x * width);
								int y = (int)(tc.y * height);
								int pixelIndex = y * width + x;
								++pixelCount[pixelIndex];
								values[pixelIndex] += variance;
								++acceptedRayCount[i];
							}
						}
						avaragedByIncomingArc[i] += variance;
					}
					focalHitpointDistance[i] /= acceptedRayCount[i];
					avaragedByIncomingArc[i] = Math.sqrt(avaragedByIncomingArc[i] / countPerArc);
					avarageCountPerArc[i] = (double)countPerArc / azimuths[i].length;
					focalDistance[i] /= azimuths[i].length;
					surfaceElevation[i] = dal.sum() / acceptedRayCount[i];
					if (acceptedRayCount[i] != dal.size())
					{
						throw new RuntimeException();
					}
					surfaceElevationVariance[i] = Math.sqrt(dal.diffSumQ(surfaceElevation[i]) / dal.size());
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
			DataHandler.runnableRunner.runParallelAndWait(prr, "Focus Heatmap", null, 0, 100, 10);
		}
		else
		{
			DataHandler.runnableRunner.runParallel(prr, "Focus Heatmap", null, 0, 100, 10);
		}
	}
}
