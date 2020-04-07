/*******************************************************************************
 * Copyright (c) 2019 Paul Stahr
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 ******************************************************************************/
package opengl;

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public class ImageWriter {
	public static final BufferedImage getImage(
			ByteBuffer bb,
			int width,
			int height)
	{
		final IntBuffer ib = bb.asIntBuffer();
	    BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		final ColorModel cm = image.getColorModel();
		final WritableRaster raster = image.getRaster();
		Object pixel = null;
		int offset = (height-1) * width;
	    for (int i=0;i<height;i++){
	    	for (int j=0;j<width;j++)
	    		raster.setDataElements(j, i, pixel = cm.getDataElements(ib.get(offset + j)>>8, pixel));
	    	offset -= width;
	    }
	    return image;
	}
	
	public static final BufferedImage getImageRGB(
			ByteBuffer bb,
			int width,
			int height)
	{
	    BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		final WritableRaster raster = image.getRaster();
		raster.setDataElements(0, 0, raster.getWidth(), raster.getHeight(), bb);
	    return image;
	}
}
