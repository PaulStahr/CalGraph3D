package data.raytrace;

import java.util.Arrays;

import geometry.Vector3d;
import maths.algorithm.Calculate;
import util.ArrayUtil;
import util.data.DoubleArrayList;

public class FitCircle extends Calculate.Optimizer
{
	public static final byte LINEAR = 0, QUADRATIC = 1, GAUSS = 2;
	public boolean surfaceDist = false;
	public byte method = 0;
	
	
	public double sigma = 0.1;
	private double gaussMult = Double.NaN;
	double spherePos[]; /*x, y, z, wight*/
	int pointCount;
	

	
	public FitCircle(int width, int height, int data[], TextureMapping tm)
	{
		super(2);
		setBound(0,0,2*Math.PI);
		setBound(1, 0, Math.PI);
		pointCount = ArrayUtil.countNonzero(data);
		spherePos = new double[pointCount * 4];
		pointCount = 0;
		Vector3d vec = new Vector3d();
		for (int y = 0; y < height; ++y)
		{
			for (int x = 0; x < width; ++x)
			{
				if (data[y * width + x] != 0)
				{
					double density = tm.mapTexToCart((double)x / width, (double)y / height, vec);
					vec.write(spherePos, pointCount);
					spherePos[pointCount + 3] = data[y * width + x] / density;
					pointCount += 4;
				}
			}
		}
	}
	
	public FitCircle(DoubleArrayList points) {
		super(2);
		setBound(0, 0, 2 * Math.PI);
		setBound(1, 0, Math.PI);
		pointCount = 0;
		for (int i = 0; i < points.size(); i += 2)
		{
			if (!(Double.isNaN(points.getD(i)) || Double.isNaN(points.getD(i + 1))))
			{
				++pointCount;
			}
		}
		spherePos = new double[pointCount * 4];
		Vector3d vec = new Vector3d();
		for (int i = 0, writeIndex = 0; i < points.size(); i += 2)
		{
			double azimuth = points.getD(i);
			double elevation = points.getD(i + 1);
			
			if (!(Double.isNaN(azimuth) || Double.isNaN(elevation)))
			{
				TextureMapping.SPHERICAL.mapSphericalToCart(azimuth, elevation, vec);
				vec.write(spherePos, writeIndex);
				spherePos[writeIndex + 3] = 1;
				writeIndex += 4;
			}
		}
		
	}
	
	@Override
	public void run()
	{
		gaussMult = 1 / (2 * sigma * sigma);
		super.run();
	}

	/*private double sphereDist()
	{
		
	}*/
	
	@Override
	public double func(double[] data) {
		double azimuth = data[0];
		double elevation = data[1];
		Vector3d vec = new Vector3d();
		TextureMapping.SPHERICAL.mapSphericalToCart(azimuth, elevation, vec);
		double x = vec.x, y = vec.y, z = vec.z;
		double score = 0;
		if (surfaceDist)
		{
			switch (method)
			{
				case LINEAR:
					for (int i = 0; i < spherePos.length; i += 4)
					{
						double dot = x * spherePos[i] + y * spherePos[i + 1] + z * spherePos[i + 2];
						score += Math.acos(dot) * spherePos[i + 3];
					}
					break;
				case QUADRATIC:
					for (int i = 0; i < spherePos.length; i += 4)
					{
						double dot = x * spherePos[i] + y * spherePos[i + 1] + z * spherePos[i + 2];
						double dist = Math.acos(dot);
						score += dist * dist * spherePos[i + 3];
					}
					break;
				case GAUSS:
					for (int i = 0; i < spherePos.length; i += 4)
					{
						double dot = x * spherePos[i] + y * spherePos[i + 1] + z * spherePos[i + 2];
						double dist = Math.acos(dot);
						score -= Math.exp(-dist * dist * gaussMult) * spherePos[i + 3];
					}
					break;
				
			}
		}
		else
		{
			switch (method)
			{
				case LINEAR:
					for (int i = 0; i < spherePos.length; i += 4)
					{
						double xDiff = x - spherePos[i], yDiff = y - spherePos[i + 1], zDiff = z - spherePos[i + 2];
						score += Math.sqrt(xDiff * xDiff + yDiff * yDiff + zDiff * zDiff) * spherePos[i + 3];
					}
					break;
				case QUADRATIC:
					for (int i = 0; i < spherePos.length; i += 4)
					{
						double xDiff = x - spherePos[i], yDiff = y - spherePos[i + 1], zDiff = z - spherePos[i + 2];
						score += (xDiff * xDiff + yDiff * yDiff + zDiff * zDiff) * spherePos[i + 3];
					}
					break;
				case GAUSS:
					for (int i = 0; i < spherePos.length; i += 4)
					{
						double xDiff = x - spherePos[i], yDiff = y - spherePos[i + 1], zDiff = z - spherePos[i + 2];
						double dist = xDiff * xDiff + yDiff * yDiff + zDiff * zDiff;
						score -= Math.exp(-dist * gaussMult) * spherePos[i + 3];
					}
					break;
			}
		}
		return score;
	}
	
	public double getIncludingCircleSize(double fraction)
	{
		double azimuth = getD(0);
		double elevation = getD(1);
		Vector3d vec = new Vector3d();
		TextureMapping.SPHERICAL.mapSphericalToCart(azimuth, elevation, vec);
		double x = vec.x, y = vec.y, z = vec.z;
		
		double distances[] = new double[spherePos.length / 4];
		for (int i = 0; i < distances.length; ++i)
		{
			int index = i * 4;
			double dot = x * spherePos[index] + y * spherePos[index + 1] + z * spherePos[index + 2];
			distances[i] = Math.acos(dot);
		}
		Arrays.sort(distances);
		return distances[(int)(distances.length * fraction)];
	}
}


