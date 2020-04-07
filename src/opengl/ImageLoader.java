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
import java.awt.image.Raster;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import javax.imageio.ImageIO;
/** 
* @author  Paul Stahr
* @version 04.02.2012
*/

public class ImageLoader {
	public static enum TargetFormat
	{
		R8G8B8A8 {
			@Override
			public void putPixel(Object o, ColorModel cModel, ByteBuffer bb) {
				bb.put((byte)cModel.getRed(o))
				  .put((byte)cModel.getGreen(o))
				  .put((byte)cModel.getBlue(o))
				  .put((byte)cModel.getAlpha(o));
			}
		}, B8G8R8 {
			@Override
			public void putPixel(Object o, ColorModel cModel, ByteBuffer bb) {
				bb.put((byte)cModel.getBlue(o))
				  .put((byte)cModel.getGreen(o))
				  .put((byte)cModel.getRed(o));
			}
		}, R8G8B8 {
			@Override
			public void putPixel(Object o, ColorModel cModel, ByteBuffer bb) {
				bb.put((byte)cModel.getRed(o))
				  .put((byte)cModel.getGreen(o))
				  .put((byte)cModel.getBlue(o));
			}
		}, A8 {
			@Override
			public void putPixel(Object o, ColorModel cModel, ByteBuffer bb) {
				bb.put((byte)cModel.getAlpha(o));
			}
		}, S8 {
			@Override
			public void putPixel(Object o, ColorModel cModel, ByteBuffer bb) {
				bb.put((byte)((cModel.getRed(o) + cModel.getGreen(o) + cModel.getBlue(o)) / 3));
			}
		};
		
		public abstract void putPixel(Object o, ColorModel cModel, ByteBuffer bb);
	}
	private int width, height;
	private ByteBuffer data;
	private TargetFormat format;
	
	public ImageLoader(){}
	
	private void setSize(int size){
		if (data == null || data.capacity() < size)
			data = ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder());
		else
			data.limit(size);
	}
	
	public final void loadImage(InputStream inStream) throws IOException{
		loadImage(ImageIO.read(inStream));		
	}
	
	public final void loadImage(String path) throws IOException{
		if (path == null)
			throw new NullPointerException();
		final File file = new File(path);
		if (!file.exists())
			throw new FileNotFoundException();
		loadImage(ImageIO.read(file));
	}
	
	private static final TargetFormat getBestFittingFormat(BufferedImage image){
		switch (image.getType())
		{
			case BufferedImage.TYPE_3BYTE_BGR:		return TargetFormat.R8G8B8;
			case BufferedImage.TYPE_4BYTE_ABGR:		return TargetFormat.R8G8B8A8;
			case BufferedImage.TYPE_4BYTE_ABGR_PRE:	return TargetFormat.R8G8B8A8;
			case BufferedImage.TYPE_BYTE_BINARY:	return TargetFormat.A8;
			case BufferedImage.TYPE_BYTE_GRAY:		return TargetFormat.A8;
			case BufferedImage.TYPE_BYTE_INDEXED:	return TargetFormat.R8G8B8A8;
			case BufferedImage.TYPE_CUSTOM:			return TargetFormat.R8G8B8A8;
			case BufferedImage.TYPE_INT_ARGB:		return TargetFormat.R8G8B8A8;
			case BufferedImage.TYPE_INT_ARGB_PRE:	return TargetFormat.R8G8B8A8;
			case BufferedImage.TYPE_INT_BGR:		return TargetFormat.R8G8B8;
			case BufferedImage.TYPE_INT_RGB:		return TargetFormat.R8G8B8;
			case BufferedImage.TYPE_USHORT_555_RGB:	return TargetFormat.R8G8B8;
			case BufferedImage.TYPE_USHORT_565_RGB:	return TargetFormat.R8G8B8;
			case BufferedImage.TYPE_USHORT_GRAY:	return TargetFormat.A8;
			default: return TargetFormat.R8G8B8A8;
		}
	}
	
	public final void loadImage(BufferedImage bufImage)
	{
		loadImage(bufImage, null);
	}
	
	public final void loadImage(BufferedImage bufImage, TargetFormat targetFormat){
		if (targetFormat == null)
		{
			targetFormat = getBestFittingFormat(bufImage);
		}
		width = bufImage.getWidth();
		height = bufImage.getHeight();
		format = targetFormat;
		ColorModel cModel = bufImage.getColorModel();
		final Raster raster = bufImage.getRaster();
		Object o = null;
		setSize(width * height * 16);
		for (int i=height-1;i>=0;i--){
			for (int j=0;j<width;j++){
				o = raster.getDataElements(j, i, o);
				targetFormat.putPixel(o, cModel, data);
			}
		}
		data.limit(data.position());
		data.position(0);
	}
	
	public final ByteBuffer getData(){
		return data.asReadOnlyBuffer();
	}
	
	public final TargetFormat getFormat(){
		return format;
	}
	
	public final int getWidth(){
		return width;
	}
	
	public final int getHeight(){
		return height;
	}

	public float getAspect() {
		return (float)width / height;
	}
}
