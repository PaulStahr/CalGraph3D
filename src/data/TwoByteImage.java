package data;

/*import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferUShort;
import java.awt.image.Raster;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.zip.InflaterInputStream;
*/

public class TwoByteImage {
	
	/*private void test(int width, int height, int bitsPerPixel, int alpha[], java.awt.color.ColorSpace colorSpace)
	{
		if(bitsPerPixel == 16) {
			short[] spixels;
		    if(interlace_method==NON_INTERLACED)
		       spixels = generate16BitRGBPixels(compr_data, false);
		    else {
		       spixels = generate16BitRGBInterlacedPixels(compr_data, false);
		               }
		    int[] off = {0, 1, 2}; //band offset, we have 3 bands
		    int numOfBands = 3;
		    boolean hasAlpha = false;
		    int trans = Transparency.OPAQUE;
		    int[] nBits = {16, 16, 16}; 
		    if(alpha != null) { // Deal with single color transparency
		       off = new int[] {0, 1, 2, 3}; //band offset, we have 4 bands
		       numOfBands = 4;
		       hasAlpha = true;
		       trans = Transparency.TRANSLUCENT;
		       nBits = new int[] {16, 16, 16, 16};                      
		    }
		    DataBufferUShort db = new DataBufferUShort(spixels, spixels.length);
		    Raster raster = Raster.createInterleavedRaster(db, width, height, width*numOfBands, numOfBands, off, null);
		    ComponentColorModel cm = new ComponentColorModel(colorSpace, nBits, hasAlpha, false, trans, DataBuffer.TYPE_USHORT);
		}
		return new BufferedImage(cm, raster, false, null);
	}
	
	private short[] generate16BitRGBPixels(byte[] compr_data, boolean fullAlpha) throws Exception {
	     //
	     int bytesPerPixel = 0;
	     int width;
	     int height;
	     byte[] pixBytes;

	     if (fullAlpha)
	         bytesPerPixel = 8;
	     else 
	         bytesPerPixel = 6;

	     int bytesPerScanLine = width*bytesPerPixel;         

	     // Now inflate the data.
	     pixBytes = new byte[height * bytesPerScanLine];

	     // Wrap an InflaterInputStream with a bufferedInputStream to speed up reading
	     BufferedInputStream bis = new BufferedInputStream(new InflaterInputStream(new ByteArrayInputStream(compr_data)));

	     apply_defilter(bis, pixBytes, height, bytesPerPixel, bytesPerScanLine);

	     short[] spixels = null;

	     if(alpha != null) { // Deal with single color transparency
	         spixels = new short[width*height*4];
	         short redMask = (short)((alpha[1]&0xff)|(alpha[0]&0xff)<<8);
	         short greenMask = (short)((alpha[3]&0xff)|(alpha[2]&0xff)<<8);;
	         short blueMask = (short)((alpha[5]&0xff)|(alpha[4]&0xff)<<8);

	         for(int i = 0, index = 0; i < pixBytes.length; index += 4) {
	             short red = (short)((pixBytes[i++]&0xff)<<8|(pixBytes[i++]&0xff));
	             short green = (short)((pixBytes[i++]&0xff)<<8|(pixBytes[i++]&0xff));
	             short blue = (short)((pixBytes[i++]&0xff)<<8|(pixBytes[i++]&0xff));
	             spixels[index] = red;
	             spixels[index + 1] = green;
	             spixels[index + 2] = blue;
	             if(spixels[index] == redMask && spixels[index + 1] == greenMask && spixels[index + 2] == blueMask) {
	                 spixels[index + 3] = (short)0x0000;                               
	             } else {
	                 spixels[index + 3] = (short)0xffff;
	             }
	         }
	     } else
	         spixels = toShortArray(pixBytes, true);

	     return spixels;         
	 }
	
	public static short[] toShortArray(byte[] data, int offset, int len, boolean bigEndian) {

	    ByteBuffer byteBuffer = ByteBuffer.wrap(data, offset, len);

	    if (bigEndian) {
	        byteBuffer.order(ByteOrder.BIG_ENDIAN);
	    } else {
	        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
	    }

	    ShortBuffer shortBuf = byteBuffer.asShortBuffer();
	    short[] array = new short[shortBuf.remaining()];
	    shortBuf.get(array);

	    return array;
	}*/
}
