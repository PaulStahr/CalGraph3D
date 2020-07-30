package data.raytrace;

import java.awt.image.WritableRaster;

import geometry.Vector2d;
import geometry.Vector3d;
import maths.algorithm.Calculate;

public enum TextureMapping
{
	PERSPECTIVE("Perspective") {

		@Override
		public double mapCartToTex(double x, double y, double z, Vector2d out) {
			out.x = x / z;
			out.y = y / z;
			return 1;
		}

		@Override
		public double mapTexToCart(double x, double y, Vector3d out) {
			out.z = 1;
			out.x = x;
			out.y = y;
			return 1;
		}

		@Override
		public double mapTexToCart(double x, double y) {
			return 1;
		}

		@Override
		public double mapTexToSpherical(double x, double y, Vector2d out) {
			out.x = (Math.atan2(y, x) + Math.PI) * INV_TWO_PI;
		    out.y = Math.atan(Math.sqrt(x * x + y * y)) * INV_PI;
		    return Double.NaN;
		}

		@Override
		public double mapSphericalToTex(double azimuth, double elevation, Vector2d out) {
			double sin = Math.sin(elevation);
			double cos = Math.cos(elevation);
			out.x = sin * Math.cos(azimuth) / cos;
			out.y = sin * Math.sin(azimuth) / cos;
			return Double.NaN;
		}

		@Override
		public void densityCompensation(WritableRaster r) {
			throw new RuntimeException("Method not implemented");
		}
	},	SPHERICAL("Spherical") {
		@Override
		public double mapCartToTex(double x, double y, double z, Vector2d out) {
			out.x = (Math.atan2(y, x) + Math.PI) * INV_TWO_PI;
		    out.y = Math.atan2(Math.sqrt(x * x + y * y), z) * INV_PI;
		    return Double.NaN;
		}
		
		@Override
		public void densityCompensation(WritableRaster r) {
			int pixelValue[] = new int[4];
			double mult = Math.PI / r.getHeight();
			for (int x = 0; x < r.getWidth(); ++x)
			{
				for (int y = 0; y < r.getHeight(); ++y)
				{
					r.getPixel(x, y, pixelValue);
					pixelValue[3] = (int)(pixelValue[3] / Math.sin(y * mult));
					pixelValue[3] = Math.min(255, pixelValue[3]);
					r.setPixel(x,y,pixelValue);
				}
			}
		}
		
		@Override
		public double mapTexToCart(double x, double y, Vector3d out)
		{
			x *= Calculate.TWO_PI;
			y *= Math.PI;
			out.x = Math.sin(y) * Math.cos(x);
			out.y = Math.sin(y) * Math.sin(x);
			out.z = Math.cos(y);
			return 1/out.z;
		}

		@Override
		public double mapTexToCart(double x, double y)
		{
			y *= Math.PI;
			double z = Math.cos(y);
			return 1/z;
		}

		@Override
		public void mapSphericalToCart(double azimuth, double elevation, Vector3d out)
		{
			double sin = Math.sin(elevation);
			out.x = sin * Math.cos(azimuth);
			out.y = sin * Math.sin(azimuth);
			out.z = Math.cos(elevation);
			return;
		}

		@Override
		public double mapTexToSpherical(double x, double y, Vector2d out) {
			out.set(Calculate.modToZeroOne(x * (2 * Math.PI)), y * Math.PI);
			return Math.sin(out.x);
		}

		@Override
		public double mapSphericalToTex(double azimuth, double elevation, Vector2d out) {
			out.set(Calculate.modToZeroOne(azimuth / (2 * Math.PI)), elevation / Math.PI);
			return 1 / Math.sin(elevation);
		}
		
		@Override
		public int defaultAspect()
		{
			return 2;
		}
	}, FISHEYE_EQUIDISTANT("Equidistant") {
		@Override
		public double mapCartToTex(double x, double y, double z, Vector2d out) {
			double len = Math.sqrt(x * x + y * y);
			len = Math.atan2(len, z)/ (len * TWO_PI);
			out.x = x * len + 0.5;
			out.y = y * len + 0.5;
			return Math.sin(len) / len;
		}
		
		@Override
		public void densityCompensation(WritableRaster r) {
			int pixelValue[] = new int[4];
			int midX = r.getWidth() / 2;
			int midY = r.getHeight() / 2;
			for (int x = 0; x < r.getWidth(); ++x)
			{
				double diffX = (double)(x - midX) / midX;
				diffX *= diffX;
				for (int y = 0; y < r.getHeight(); ++y)
				{
					double diffY = (double)(y - midY) / midY;
					diffY *= diffY;
					double dist = Math.PI * Math.sqrt(diffX + diffY);
					r.getPixel(x, y, pixelValue);
					if (dist != 0)
					{
						pixelValue[3] *= dist / Math.sin(dist);
						pixelValue[3] = Math.min(255, pixelValue[3]);
					}
					r.setPixel(x,y,pixelValue);
				}
			}
		}

		@Override
		public double mapTexToCart(double x, double y, Vector3d out) {
			x = x - 0.5;
			y = y - 0.5;
			double rad = Math.sqrt(x * x + y * y);
			out.z = Math.cos(rad * Calculate.TWO_PI);
			double sin = Math.sin(rad * Calculate.TWO_PI);
			double det = rad == 0 ? Math.PI : sin / rad;
			out.x = x * det;
			out.y = y * det;
			return 1 / sin; //TODO det or 1 / sin
		}

		@Override
		public double mapTexToCart(double x, double y) {
			x = x - 0.5;
			y = y - 0.5;
			double rad = Math.sqrt(x * x + y * y);
			double sin = Math.sin(rad * Calculate.TWO_PI);
			return 1 / sin; //TODO det or 1 / sin
		}
		
		@Override
		public double mapTexToSpherical(double x, double y, Vector2d out)
		{
			x -= 0.5;
			y -= 0.5;
			out.x = Math.atan2(y, x);
			out.y = Math.sqrt(x * x + y * y) * Math.PI;
			return Math.sin(out.y) / out.y;
		}

		@Override
		public double mapSphericalToTex(double azimuth, double elevation, Vector2d out) {
			out.set(Math.sin(azimuth), Math.cos(azimuth));
			out.multiply(elevation / (Calculate.TWO_PI) );
			out.add(0.5,0.5);
			return elevation / Math.sin(elevation);
		}
		
	}, FISHEYE_EQUIDISTANT_HALF("Equidistant half") {
		@Override
		public double mapCartToTex(double x, double y, double z, Vector2d out) {
			double len = Math.sqrt(x * x + y * y);
			if (len == 0)
			{
				out.x = 0.5;
				out.y = 0.5;
				return 1;
			}
			len = 2 * INV_TWO_PI * Math.atan2(len, z)/ len;
			out.x = x * len + 0.5;
			out.y = y * len + 0.5;
			return Math.sin(len) / len;
			//System.out.println(x + " " + y + " " + z + "->" + out + " " + len);

			//System.out.println(out);
		}
		
		@Override
		public void densityCompensation(WritableRaster r) {
			int pixelValue[] = new int[4];
			int midX = r.getWidth() / 2;
			int midY = r.getHeight() / 2;
			double multx = 1./midX;
			double multy = 1./midY;
			for (int x = 0; x < r.getWidth(); ++x)
			{
				double diffX = (x - midX) * multx;
				diffX *= diffX;
				for (int y = 0; y < r.getHeight(); ++y)
				{
					double diffY = (y - midY) * multy;
					diffY *= diffY;
					double dist = Calculate.TWO_PI * Math.sqrt(diffX + diffY);
					r.getPixel(x, y, pixelValue);
					if (dist != 0)
					{
						pixelValue[3] *= dist / Math.sin(dist);
						pixelValue[3] = Math.min(255, pixelValue[3]);
					}
					r.setPixel(x,y,pixelValue);
				}
			}
		}

		@Override
		public double mapTexToCart(double x, double y, Vector3d out) {
			x = x - 0.5;
			y = y - 0.5;
			double rad = Math.sqrt(x * x + y * y);
			out.z = Math.cos(rad * Math.PI);
			double sin = Math.sin(rad * Math.PI);
			double det = rad == 0 ? Math.PI : sin / rad;
			out.x = x * det;
			out.y = y * det;
			return det;
		}
		
		@Override
		public double mapTexToCart(double x, double y) {
			x = x - 0.5;
			y = y - 0.5;
			double rad = Math.sqrt(x * x + y * y);
			double sin = Math.sin(rad * Math.PI);
			double det = rad == 0 ? Math.PI : sin / rad;
			return det;
		}
		
		@Override
		public double mapTexToSpherical(double x, double y, Vector2d out)
		{
			x -= 0.5;
			y -= 0.5;
			out.x = Math.atan2(y, x);
			out.y = Calculate.TWO_PI * Math.sqrt(x * x + y * y);
			return Math.sin(out.y) / out.y;
		}

		@Override
		public double mapSphericalToTex(double azimuth, double elevation, Vector2d out) {
			out.set(Math.cos(azimuth), Math.sin(azimuth));
			out.multiply(elevation / Math.PI );
			out.add(0.5,0.5);
			return elevation / Math.sin(elevation);
		}
		
	}, FLAT("Flat") {
		@Override
		public double mapCartToTex(double x, double y, double z, Vector2d out) {
			double len = Math.sqrt(x * x + y * y);
			len = len == 0 ? 0 : (INV_TWO_PI * Math.atan2(len, z)/ len);
			out.x = x * len + 0.5;
			out.y = y * len + 0.5;
			return Double.NaN;
		}

		@Override
		public double mapTexToCart(double x, double y, Vector3d out) {
			out.x = x = (x - 0.5) * 2;
			out.y = y = (y - 0.5) * 2;
			return 2 * (out.z = Math.sqrt(1 - x * x - y * y));
		}
		
		@Override
		public double mapTexToCart(double x, double y) {
			x = (x - 0.5) * 2;
			y = (y - 0.5) * 2;
			return 2 * Math.sqrt(1 - x * x - y * y);
		}
		
		@Override
		public double mapTexToSpherical(double x, double y, Vector2d out)
		{
			x -= 0.5;
			y -= 0.5;
			out.x = Math.atan2(y, x);
			double sin = Math.sqrt(x * x + y * y);
			out.y = Math.asin(sin * Math.PI);
			return 1 / (1 - sin);
		}

		@Override
		public double mapSphericalToTex(double azimuth, double elevation, Vector2d out) {
			out.set(Math.sin(azimuth), Math.cos(azimuth));
			double sin = Math.sin(elevation);
			out.multiply(sin / 2);
			out.add(0.5,0.5);
			return 1 - sin;
		}

		@Override
		public void densityCompensation(WritableRaster r) {
			int pixelValue[] = new int[4];
			double invW = 1. / r.getWidth(), invH = 1. / r.getHeight();
			for (int y = 0; y < r.getHeight(); ++y)
			{
				double yp = (y * 2 - r.getHeight()) * invH;
				for (int x = 0; x < r.getWidth(); ++x)
				{
					r.getPixel(x, y, pixelValue);
					double xp = (x * 2 - r.getWidth()) * invW;
					
					pixelValue[3] = (int)(pixelValue[3] / Math.asin(Math.sqrt(xp * xp + yp * yp)));
					pixelValue[3] = Math.min(255, pixelValue[3]);
					r.setPixel(x,y,pixelValue);
				}
			}
		}
	};
	
	public static final int size() {
		return tm.length;
	}
	
	public void mapSphericalToCart(double x, double y, Vector3d out) {
		throw new RuntimeException();
	}

	private static final double TWO_PI = 2 * Math.PI;
	private static final double INV_TWO_PI = Math.nextDown(1.0) / (2 * Math.PI);
	private static final double INV_PI = Math.nextDown(1.0) / Math.PI;
	public final String name;
	private TextureMapping(String name)
	{
		this.name = name;
	}
	
	public abstract double mapCartToTex(double x, double y, double z, Vector2d out);
	public abstract double mapTexToCart(double x, double y, Vector3d out);
	public abstract double mapTexToCart(double x, double y);
	public abstract double mapTexToSpherical(double x, double y, Vector2d out);
	public abstract double mapSphericalToTex(double azimuth, double elevation, Vector2d out);
	public abstract void densityCompensation(WritableRaster r);

	
	private static final TextureMapping tm[] = TextureMapping.values();
	public static String[] names (){
		 String res[] = new String[tm.length];
		 for (int i = 0; i < res.length; ++i)
		 {
			 res[i] = tm[i].name;
		 }
		 return res;
	 }
	
	public int defaultAspect()
	{
		return 1;
	}
	
	public static final TextureMapping getByName(String name)
	{
		for (TextureMapping current : tm)
		{
			if (name.equals(current.name))
			{
				return current;
			}
		}
		return valueOf(name);
	}

	@Override
	public String toString()
	{
		return name;
	}

	public static TextureMapping get(int i) {
		return tm[i];
	}

	public void densityCompensation(int width, int height, int[] imageColorArray, int channels, int stride)
	{
		for (int y = 0, index = 0; y < height; ++y)
		{
			for (int x = 0; x < width; ++x)
			{
				for (int i = 0; i < channels; ++i)
				{
					imageColorArray[index + i] *= mapTexToCart(x, y);
				}
				index += stride;
			}
		}
	}
	
	public void inverseDensityCompensation(int width, int height, int[] imageColorArray, int channels, int stride)
	{
		for (int y = 0, index = 0; y < height; ++y)
		{
			for (int x = 0; x < width; ++x)
			{
				for (int i = 0; i < channels; ++i)
				{
					imageColorArray[index + i] /= mapTexToCart(x, y);
				}
				index += stride;
			}
		}
	}
}