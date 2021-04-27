package jcomponents.util;

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.util.Arrays;

import util.ArrayUtil;
import util.data.IntegerArrayList;

public class ImageUtil {
	public static void addToPixel(double xf, double yf, int width, int height, float add[], int addBegin, int addEnd, float multiplier, int image[])
	{
		int x = (int)(xf * 0x100);
		int y = (int)(yf * 0x100);
		int xMod = x % 0x100;
		int yMod = y % 0x100;
		x /= 0x100;
		y /= 0x100;
		if (x + 1 == width)
		{
			--x;
			xMod = 255;
		}
		if (y + 1 == height)
		{
			--y;
			yMod = 255;
		}
		
		int ch = addEnd - addBegin;
		int pIndex = (y * width + x) * ch;
		for (int i = 0; i < ch; ++i)
		{
			int addIndex = i + addBegin;
			float toAdd =  add[addIndex] * multiplier;
			image[pIndex + i]          		    += (int)(((0x100 - xMod) * (0x100 - yMod)) * toAdd);
			image[pIndex + ch + i]      	    += (int)((xMod           * (0x100 - yMod)) * toAdd);
			image[pIndex + width * ch + i]      += (int)(((0x100 - xMod) * yMod)           * toAdd);
			image[pIndex + width * ch + ch + i] += (int)((xMod           * yMod)           * toAdd);
		}
	}
	
	public static final void addToPixel(double xf, double yf, int width, int height, float add[], int addBegin, int addEnd, float multiplier, float image[])
	{
		int x = (int)(xf * 0x100);
		int y = (int)(yf * 0x100);
		int xMod = x % 0x100;
		int yMod = y % 0x100;
		x /= 0x100;
		y /= 0x100;
		if (x + 1 == width)
		{
			--x;
			xMod = 255;
		}
		if (y + 1 == height)
		{
			--y;
			yMod = 255;
		}
		
		int ch = addEnd - addBegin;
		int pIndex = (y * width + x) * ch;
		if (addEnd > add.length)
		{
			throw new ArrayIndexOutOfBoundsException("Illegal Input range: " + addBegin + '-' + addEnd + " for size " + add.length);
		}
		for (int i = 0; i < ch; ++i)
		{
			int addIndex = i + addBegin;
			float toAdd =  add[addIndex] * multiplier;
			image[pIndex + i]          		    += ((0x100 - xMod) * (0x100 - yMod)) * toAdd;
			image[pIndex + ch + i]      	    += (xMod           * (0x100 - yMod)) * toAdd;
			image[pIndex + width * ch + i]      += ((0x100 - xMod) * yMod)           * toAdd;
			image[pIndex + width * ch + ch + i] += (xMod           * yMod)           * toAdd;
		}
	}
	
	public static void addToPixel(double xf, double yf, int width, int height, float multiplier, int image[])
	{
		int x = (int)(xf * 0x100);
		int y = (int)(yf * 0x100);
		int xMod = x % 0x100;
		int yMod = y % 0x100;
		x /= 0x100;
		y /= 0x100;
		if (x + 1 == width)
		{
			--x;
			xMod = 255;
		}
		if (y + 1 == height)
		{
			--y;
			yMod = 255;
		}
		
		int pIndex = y * width + x;
		image[pIndex]             += (int)(((0x100 - xMod) * (0x100 - yMod)) * multiplier);
		image[pIndex + 1]      	  += (int)((xMod           * (0x100 - yMod)) * multiplier);
		image[pIndex + width]     += (int)(((0x100 - xMod) * yMod)           * multiplier);
		image[pIndex + width + 1] += (int)((xMod           * yMod)           * multiplier);
	}
	
	
	public static final BufferedImage deepCopy(BufferedImage bi) {
		if (bi == null)
		{
			return null;
		}
		ColorModel cm = bi.getColorModel();
		return new BufferedImage(cm, bi.copyData(null), cm.isAlphaPremultiplied(), null);
	}
	
	public static void convertSpericalToFlat(Raster source, WritableRaster destination)
	{
		/*int pixel[] = new int[4];
		for (int x = 0; x < source.getWidth(); ++x)
		{
			for (int y = 0; y < source.getHeight(); ++y)
			{
				source.getPixel(x, y, pixel);
				double arc = Math.atan2(x * 2 - source.getWidth(),y - source.getHeight() / 2);
				//destination.setPixel(destination.getWidth() / 2 + , destination.getHeight() / 2, pixel);
			}
		}*/
	}
	
	/*public static void interpolateInvisiblePixels2(Raster source)
	{
		Armadillo.
	}*/
	
	public static void interpolateInvisiblePixels(Raster source)
	{
		IntegerArrayList ial = new IntegerArrayList();
		
		int width = source.getWidth(), height = source.getHeight();
		int numPixels = width * height;
		int pixel[] = new int[4];
		for (int i = 0; i < numPixels; ++i)
		{
			int x = i % width;
			int y = i / width;
			source.getPixel(x, y, pixel);
			if (pixel[3] > 0)
			{
				if (x != 0)
				{
					source.getPixel(x - 1, y, pixel);
					if (pixel[3] == 0)
					{
						ial.add(i);
						continue;
					}
				}
				if (x != width - 1)
				{
					source.getPixel(x + 1, y, pixel);
					if (pixel[3] == 0)
					{
						ial.add(i);
						continue;
					}
				}
				if (y != 0)
				{
					source.getPixel(x, y - 1, pixel);
					if (pixel[3] == 0)
					{
						ial.add(i);
						continue;
					}
				}
				if (y != height - 1)
				{
					source.getPixel(x, y + 1, pixel);
					if (pixel[3] == 0)
					{
						ial.add(i);
						continue;
					}
				}
			}
		}
		while (ial.size() != 0)
		{
			for (int i = 0; i < ial.size(); ++i)
			{
				
			}
		}
	}

	public static void fill(WritableRaster outRaster, int[] color) {
		for (int y = 0; y < outRaster.getHeight(); ++y)
		{
			for (int x = 0; x < outRaster.getWidth(); ++x)
			{
				outRaster.setPixel(x, y, color);
			}
		}
	}
	
	
	public static int getSmoothedPixel(double xf, double yf, double zf, int data[], int width, int height, int depth)
	{
		int x = (int)(xf * 0x100);
		int y = (int)(yf * 0x100);
		int z = (int)(zf * 0x100);
		int xMod = x % 0x100;
		int yMod = y % 0x100;
		int zMod = z % 0x100;
		x /= 0x100;
		y /= 0x100;
		z /= 0x100;
		if (x + 1 == width)   {--x;xMod = 255;}
		if (y + 1 == height)  {--y;yMod = 255;}
		if (z + 1 == depth)   {--z;zMod = 255;}
		int index = ((z * height) + y) * width + x;
		long result = 0;
		for (int i = 0; i < 8; ++i)
		{
			int m0 =  i % 2;
			int m1 = (i / 2) % 2;
			int m2 = ((i / 4) % 2);
			long dat = data[index + ((m2 * height) + m1) * width + m0];
			dat *= (0x100 - xMod + m0 * (2 * xMod - 0x100));
			dat *= (0x100 - yMod + m1 * (2 * yMod - 0x100));
			dat *= (0x100 - zMod + m2 * (2 * zMod - 0x100));
			result += dat;
		}
		result /= 0x1000000;
		return (int)result;
	}
	
	public static float getSmoothedPixel(double xf, double yf, double zf, float data[], int width, int height, int depth)
	{
		int x = (int)(xf * 0x100);
		int y = (int)(yf * 0x100);
		int z = (int)(zf * 0x100);
		int xMod = x % 0x100;
		int yMod = y % 0x100;
		int zMod = z % 0x100;
		x /= 0x100;
		y /= 0x100;
		z /= 0x100;
		if (x + 1 == width)   {--x;xMod = 255;}
		if (y + 1 == height)  {--y;yMod = 255;}
		if (z + 1 == depth)   {--z;zMod = 255;}
		int index = ((z * height) + y) * width + x;
		float result = 0;
		for (int i = 0; i < 8; ++i)
		{
			int m0 =  i % 2;
			int m1 = (i / 2) % 2;
			int m2 = ((i / 4) % 2);
			float dat = data[index + ((m2 * height) + m1) * width + m0];
			long multiply  = (0x100 - xMod + m0 * (2 * xMod - 0x100));
			     multiply *= (0x100 - yMod + m1 * (2 * yMod - 0x100));
			     multiply *= (0x100 - zMod + m2 * (2 * zMod - 0x100));
			result += dat * multiply;
		}
		result /= 0x1000000;
		return result;
	}
	
	public static void getSmoothedPixel(double xf, double yf, int output[], int tmp[], Raster raster)
	{
		int x = (int)(xf * 0x100);
		int y = (int)(yf * 0x100);
		int xMod = x % 0x100;
		int yMod = y % 0x100;
		x /= 0x100;
		y /= 0x100;
		final int width = raster.getWidth(), height = raster.getHeight();
		if (x + 1 == width)
		{
			--x;
			xMod = 255;
		}
		if (y + 1 == height)
		{
			--y;
			yMod = 255;
		}
		
		raster.getPixel(x, y, tmp);
		ArrayUtil.addTo(tmp, 0, tmp.length, output, 0, (0x100 - xMod) * (0x100 - yMod));
		raster.getPixel(x + 1, y, tmp);
		ArrayUtil.addTo(tmp, 0, tmp.length, output, 0, xMod           * (0x100 - yMod));
		raster.getPixel(x, y + 1, tmp);
		ArrayUtil.addTo(tmp, 0, tmp.length, output, 0, (0x100 - xMod) * yMod);
		raster.getPixel(x + 1, y + 1, tmp);
		ArrayUtil.addTo(tmp, 0, tmp.length, output, 0, xMod           * yMod);
		ArrayUtil.divide(output, 0, output.length, output, 0, 0x10000);
	}

	public static void addToPixel(double xf, double yf, int width, int height, int add, int count[]) {
		int x = (int)(xf * 0x100);
		int y = (int)(yf * 0x100);
		int xMod = x % 0x100;
		int yMod = y % 0x100;
		x /= 0x100;
		y /= 0x100;
		if (x + 1 == width)
		{
			--x;
			xMod = 255;
		}
		if (y + 1 == height)
		{
			--y;
			yMod = 255;
		}
		count[y * width + x]           += (0x100 - xMod) * (0x100 - yMod) * add;
		count[y * width + x + 1]       += xMod           * (0x100 - yMod) * add;
		count[(y + 1) * width + x]     += (0x100 - xMod) * yMod           * add;
		count[(y + 1) * width + x + 1] += xMod           * yMod           * add;
	}

	public static void getChannel(Raster raster, byte channel, int[] data, int[] pixel) {
		for (int y = 0; y < raster.getHeight(); ++y)
		{
			for (int x = 0; x < raster.getWidth(); ++x)
			{
				raster.getPixel(x, y, pixel);
				data[y * raster.getWidth() + x] = pixel[channel];
			}
		}
	}

	public static void setChannel(WritableRaster raster, byte channel, int[] count, int[] pixel) {
		final int width = raster.getWidth(), height = raster.getHeight();
		for (int y = 0; y < height; ++y)
		{
			for (int x = 0; x < width; ++x)
			{
				raster.getPixel(x, y, pixel);
				pixel[channel] = Math.min(count[y * width + x], 255); 
				raster.setPixel(x,y,pixel);
			}
		}
	}

	public static void setRGBAChannels(WritableRaster raster, int[] count, int[] pixel) {
		final int width = raster.getWidth(), height = raster.getHeight();
		for (int y = 0; y < height; ++y)
		{
			for (int x = 0; x < width; ++x)
			{
				raster.getPixel(x, y, pixel);
				Arrays.fill(pixel, 0, 3, Math.min(count[y * width + x], 255)); 
				raster.setPixel(x,y,pixel);
			}
		}
	}

	public static void setRGB(WritableRaster raster, int[] imageColorArray, int[] pixel, int channels, int stride) {
		final int width = raster.getWidth(), height = raster.getHeight();
		for (int y = 0; y < height; ++y)
		{
			for (int x = 0; x < width; ++x)
			{
				System.arraycopy(imageColorArray, (y * width + x) * stride, pixel, 0, channels);
				raster.setPixel(x, y, pixel);
			}
		}
	}

	public static void setRGB(WritableRaster raster, float[] imageColorArray, int[] pixel, int channels, int stride) {
		final int width = raster.getWidth(), height = raster.getHeight();
		for (int y = 0; y < height; ++y)
		{
			for (int x = 0; x < width; ++x)
			{
				ArrayUtil.arraycopy(imageColorArray, (y * width + x) * stride, pixel, 0, channels);
				raster.setPixel(x, y, pixel);
			}
		}
	}
	
}
