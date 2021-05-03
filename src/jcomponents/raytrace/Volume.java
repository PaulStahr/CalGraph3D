package jcomponents.raytrace;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import util.ArrayUtil;

public class Volume {
	public static final Volume[] EMPTY_VOLUME_ARRAY = new Volume[0];
	public final int width, height, depth;
	public final float data[];
	public final int translucency[];
	private int modCount = 0;
	
	public Volume(Volume vol)
	{
		this.width = vol.width;
		this.height = vol.height;
		this.depth = vol.depth;
		this.data = vol.data.clone();
		this.translucency = vol.translucency.clone();
	}
	
	public void read(Volume vol)
	{
		if (this.width != vol.width || this.height != vol.height || this.depth != vol.depth)
		{
			throw new ArrayIndexOutOfBoundsException();
		}
		System.arraycopy(vol.data, 0, this.data, 0, this.data.length);
		System.arraycopy(vol.translucency, 0, this.translucency, 0, this.translucency.length);
	}
	
	public Volume readOrClone(Volume vol)
	{
		if (this.width != vol.width || this.height != vol.height || this.depth != vol.depth)
		{
			return new Volume(vol);
		}
		System.arraycopy(vol.data, 0, this.data, 0, this.data.length);
		System.arraycopy(vol.translucency, 0, this.translucency, 0, this.translucency.length);
		return this;
	}
	
	public Volume(int width, int height, int depth)
	{
		this.width = width;
		this.height = height;
		this.depth = depth;
		data = new float[width * height * depth];
		translucency = new int[width * height * depth];
	}


	public Volume(DataInputStream inBuf) throws IOException {
		int depth = inBuf.readInt();
		int height = inBuf.readInt();
		int width = inBuf.readInt();
		long elems = (long)depth * (long)height;
		if (elems > Integer.MAX_VALUE) {throw new OutOfMemoryError("Can't allocate " + depth + '*' + height + '*' + width + " Elements");}
        elems = elems * width;
        if (elems > Integer.MAX_VALUE) {throw new OutOfMemoryError("Can't allocate " + depth + '*' + height + '*' + width + " Elements");}
		this.width = width;
		this.height = height;
		this.depth = depth;
		data = new float[width * height * depth];
		translucency = new int[width * height * depth];
		
		ArrayUtil.readFloats(data, 0, data.length, inBuf);
		ArrayUtil.readIntegers(translucency, 0,translucency.length, inBuf);
	}


	public void writeBinary(DataOutputStream outBuf) throws IOException {
		outBuf.writeInt(depth);
		outBuf.writeInt(height);
		outBuf.writeInt(width);
		ArrayUtil.write(data, 0, data.length, outBuf);
		ArrayUtil.write(translucency, 0, translucency.length, outBuf);
	}

	public void modified() {
		++modCount;	
	}
	
	public int modCount()
	{
		return modCount;
	}
	
}
