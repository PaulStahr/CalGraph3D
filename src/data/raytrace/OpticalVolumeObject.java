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
package data.raytrace;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import data.DataHandler;
import data.Options;
import data.raytrace.RaySimulation.MaterialType;
import geometry.Geometry;
import geometry.Matrix4d;
import geometry.Vector3d;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.DICOM;
import jcomponents.raytrace.Volume;
import maths.Armadillo;
import maths.Controller;
import maths.Operation;
import maths.Variable;
import maths.VariableStack;
import maths.algorithm.OperationCalculate;
import maths.data.StringId;
import util.ArrayUtil;
import util.Buffers;
import util.StringUtils;
import util.data.DoubleArrayList;
import util.data.IntegerArrayList;
import util.data.SortedIntegerArrayList;

public abstract class OpticalVolumeObject extends OpticalObject{
	public MaterialType materialType = null;
	public static final OpticalVolumeObject EMPTY_VOLUME_ARRAY[] = new OpticalVolumeObject[0];
	private static final Logger logger = LoggerFactory.getLogger(OpticalVolumeObject.class);
	
	private static boolean native_raytrace = false;
	static
	{
		try
		{
			boolean cuda = true;
			if (new File("/usr/local/cuda-8.0/lib64/libcudart.so").exists())
			{
				System.load("/usr/local/cuda-8.0/lib64/libcudart.so");
			}
			else if (new File("/usr/lib/nvidia-cuda-toolkit/lib64/libcudart.so").exists())
			{
				System.load("/usr/lib/nvidia-cuda-toolkit/lib64/libcudart.so");
			}
			else if (new File("/usr/lib/x86_64-linux-gnu/libcudart.so").exists())
			{
				System.load("/usr/lib/x86_64-linux-gnu/libcudart.so");
			}
			else
			{
				logger.error("No cuda found");
				cuda = false;
			}
			
			if (cuda)
			{
				if (new File ("/usr/lib/cuda_raytrace_java.so").exists())
				{
					System.load("/usr/lib/cuda_raytrace_java.so");
				}
				else if (new File ("/media/paul/Data1/Caesar/Raytracer/cuda_raytrace_java.so").exists())
				{
					System.load("/media/paul/Data1/Caesar/Raytracer/cuda_raytrace_java.so");
				}
				else
				{
					DataHandler.loadLib("cuda_raytrace_java.so");
				}
				native_raytrace = true;
			}
		}catch(UnsatisfiedLinkError | IOException | NullPointerException e)
		{
			logger.error("Can't load cuda-raytracer", e);
		}
	}
	
	static class VolumeRaytraceOptions
	{
		private long pointer;
		enum VolumeRaytraceOptionType {
			LOGLEVEL(0), WRITE_INSTANCE(1);
			private final int id;
			VolumeRaytraceOptionType(int id) { this.id = id; }
		    public int getValue() { return id; }
		};
		
		public VolumeRaytraceOptions()
		{
			pointer = new_options();
		}
		
		public int getLoglevel()
		{
			return get_option_valuei(pointer, VolumeRaytraceOptionType.LOGLEVEL.id);
		}
		
		public void setLoglevel(int value)
		{
			set_option_valuei(pointer, VolumeRaytraceOptionType.LOGLEVEL.id, value);
		}
		
		public boolean getWriteInstance()
		{
			return get_option_valueb(pointer, VolumeRaytraceOptionType.WRITE_INSTANCE.id);
		}
		
		public void setWriteInstance(boolean value)
		{
			set_option_valueb(pointer, VolumeRaytraceOptionType.WRITE_INSTANCE.id, value);			
		}
		
		@Override
		protected void finalize()
		{
			if (pointer != 0)
			{
				delete_options(pointer);
				pointer = 0;
			}
		}
	}
	
	public static native long new_instance(IntBuffer bounds, IntBuffer ior, IntBuffer transculency, long opt_pt);

	public static native long new_options();
	
	public static native long new_instance(IntBuffer bounds, FloatBuffer ior, IntBuffer transculency, long opt_pt);
	
	public static native void delete_instance(long pointer);
	
	public static native void delete_options(long pointer);
	
	public static native int get_option_valuei(long pointer, long id);
	
	public static native void set_option_valuei(long pointer, long id, int value);
	
	public static native boolean get_option_valueb(long pointer, long id);
	
	public static native void set_option_valueb(long pointer, long id, boolean value);
	
	public static native void trace_rays(long pointer, IntBuffer start_position, ShortBuffer start_direction, IntBuffer end_iteration, FloatBuffer scale, float minimum_brightness, int iterations, boolean trace_path, IntBuffer path, long option_pointer);
	
	public static native void trace_rays(long pointer, IntBuffer start_position, FloatBuffer start_direction, IntBuffer end_iteration, FloatBuffer scale, float minimum_brightness, int iterations, boolean trace_path, IntBuffer path, long option_pointer);
	
	public final Matrix4d unitVolumeToGlobal = new Matrix4d();
	public final Matrix4d globalToUnitVolume = new Matrix4d();
	private final Matrix4d cudaLatticeToGlobal = new Matrix4d();
	private final Matrix4d globalToCudaLattice = new Matrix4d();
	private final Matrix4d latticeToGlobal = new Matrix4d();
	private final Matrix4d globalTolattice = new Matrix4d();
	public final Vector3d unitVolumeToGlobalRows[] = new Vector3d[] {new Vector3d(), new Vector3d(), new Vector3d()};
	protected Volume vol = new Volume(0, 0, 0);
	//public double inverserowdotprods[] = new double[3];
	private Vector3d spacing = new Vector3d(Double.NaN, Double.NaN, Double.NaN);
	VolumeScene vs;
	public DICOM dcm = null;
	public ImagePlus ip;
	public Color color = Color.BLACK;
	private final FloatBuffer scale = Buffers.createFloatBuffer(3);
	protected int maxSteps = 8000;
	public double volumeScaling = 1000;
	private static class VolumeScene
	{
		private long pointer;
		private AtomicInteger running = new AtomicInteger();
		boolean destroy = false;
		
		private VolumeScene(IntBuffer bounds, IntBuffer ior, IntBuffer translucency, VolumeRaytraceOptions opt)
		{
			pointer = new_instance(bounds, ior, translucency, opt.pointer);
		}
		
		private VolumeScene(IntBuffer bounds, FloatBuffer ior, IntBuffer translucency, VolumeRaytraceOptions opt)
		{
			pointer = new_instance(bounds, ior, translucency, opt.pointer);
		}
		
		public void traceRays(IntBuffer start_position, ShortBuffer start_direction, IntBuffer end_iteration, FloatBuffer scale, float minimum_brightness, int iterations, IntBuffer path, VolumeRaytraceOptions options)
		{
			running.incrementAndGet();
			trace_rays(pointer, start_position, start_direction, end_iteration, scale, minimum_brightness, iterations, path != null, path, options.pointer);
			if (running.decrementAndGet() == 0 && destroy)
			{
				finalize();
			}
		}
		
		public void traceRays(IntBuffer start_position, FloatBuffer start_direction, IntBuffer end_iteration, FloatBuffer scale, float minimum_brightness, int iterations, IntBuffer path, VolumeRaytraceOptions options)
		{
			running.incrementAndGet();
			trace_rays(pointer, start_position, start_direction, end_iteration, scale, minimum_brightness, iterations, path != null, path, options.pointer);
			if (running.decrementAndGet() == 0 && destroy)
			{
				finalize();
			}
		}
		
		public void destroyLazy()
		{
			if (running.get() == 0)
			{
				finalize();
			}
			else
			{
				destroy = true;
			}
		}
		
		@Override
		protected void finalize()
		{
			if (pointer != 0)
			{
				delete_instance(pointer);
				pointer = 0;
			}
		}
	}

	
	public OpticalVolumeObject()
	{
		setSize(10, 10, 10);
	}
	
	public void applyMatrix()
	{
		unitVolumeToGlobal.getCol(3, midpoint);
		globalToUnitVolume.invert(unitVolumeToGlobal);
		for (int i = 0; i < 3; ++i)
		{
			globalToUnitVolume.getRow(i, unitVolumeToGlobalRows[i]);
		}
		int width = vol.width, height = vol.height, depth = vol.depth;
		globalToCudaLattice.set(globalToUnitVolume);
		cudaLatticeToGlobal.set(unitVolumeToGlobal);
		globalToCudaLattice.postTranslate(1, 1, 1);
		cudaLatticeToGlobal.preTranslate(-1, -1, -1);
		double mult = 0.5 * 0x10000;
		double xScale = mult * (width - 3), yScale = mult * (height - 3), zScale = mult * (depth - 3);
		globalToCudaLattice.postScale(xScale, yScale, zScale);
		cudaLatticeToGlobal.preScale(1/xScale, 1/yScale, 1/zScale);
		cudaLatticeToGlobal.getColDot3(spacing);
		spacing.sqrt();
		spacing.multiply(0x10000 * 0.5);
		globalToCudaLattice.postTranslate(0x10000, 0x10000, 0x10000);
		cudaLatticeToGlobal.preTranslate(-0x10000, -0x10000, -0x10000);
		
		scale.put(0,(float)(volumeScaling / spacing.z)).put(1,(float)(volumeScaling / spacing.y)).put(2,(float)(volumeScaling / spacing.x));
		modified();
		//TODO spacing
	}
	
	static class VolumeCalculationEnvironment{
		Variable xVar = new Variable("x");
		Variable yVar = new Variable("y");
		Variable zVar = new Variable("z");
		Variable xLocalVar = new Variable("lx");
		Variable yLocalVar = new Variable("ly");
		Variable zLocalVar = new Variable("lz");
		Variable indexVar = new Variable("index");
		VariableStack vs;
		private Matrix4d latticeToGlobal;
		private int width;
		private int height;
		private int depth;
		public final Vector3d position = new Vector3d();
		
		public VolumeCalculationEnvironment(VariableStack vs, Matrix4d latticeToGlobal, int width, int height, int depth)
		{
			this.vs = vs;
			this.latticeToGlobal = latticeToGlobal;
			this.width = width;
			this.height = height;
			this.depth = depth;
			vs.addLocal(xVar);
			vs.addLocal(yVar);
			vs.addLocal(zVar);	
			vs.addLocal(xLocalVar);
			vs.addLocal(yLocalVar);
			vs.addLocal(zLocalVar);
			vs.addLocal(indexVar);
		}
		
		public void setPosition(int index, int x, int y, int z)
		{
			xLocalVar.setValue((double)x/width);
			yLocalVar.setValue((double)y/height);
			zLocalVar.setValue((double)z/depth);
			
			latticeToGlobal.rdotAffine(x,y,z, position);
			xVar.setValue(position.x);
			yVar.setValue(position.y);
			zVar.setValue(position.z);
			indexVar.setValue(index);
		}
	}
	
	public void editValues(OpticalSurfaceObject oso[], Operation operationIOR, Operation operationTranslucency, Operation givenValueOperation, Operation isGivenOperation, VariableStack variables, Volume vol)
	{
		int width = vol.width, height = vol.height, depth = vol.depth;
		float data[] = vol.data;
		int translucency[] = vol.translucency;
		Vector3d position = new Vector3d();
		final int heightp = height + 1;
		final int widthp = width + 1;
		SortedIntegerArrayList ial = new SortedIntegerArrayList();
		if (operationIOR != null)
		{
			OperationCalculate.getVariables(operationIOR, ial);
		}
		if (operationTranslucency != null)
		{
			OperationCalculate.getVariables(operationTranslucency, ial);
		}
		if (givenValueOperation != null)
		{
			OperationCalculate.getVariables(givenValueOperation, ial);
		}
		if (isGivenOperation != null)
		{
			OperationCalculate.getVariables(isGivenOperation, ial);
		}
		ArrayList<OpticalSurfaceObject> tmpList = new ArrayList<>();
		for (int i = 0; i < oso.length; ++i)
		{
			if (ial.contains(StringId.getIdIfExist(oso[i].getId())))
			{
				tmpList.add(oso[i]);
			}
		}
		float minDat = Integer.MAX_VALUE;
		float maxDat = Integer.MIN_VALUE;
		int minTrans = Integer.MAX_VALUE;
		int maxTrans = Integer.MIN_VALUE;
		
		for (int i = 0; i < data.length; ++i)
		{
			minDat = Math.min(minDat, data[i]);
			maxDat = Math.max(maxDat, data[i]);
			minTrans = Math.min(minTrans, translucency[i]);
			maxTrans = Math.max(maxTrans, translucency[i]);
		}
		
		double divx = 1. / (width - 3), divy = 1. / (height - 3), divz = 1. / (depth - 3);
		oso = tmpList.toArray(new OpticalSurfaceObject[tmpList.size()]);
		final double values[][] = new double[oso.length][widthp * heightp * (depth + 1)];
		
		latticeToGlobal.set(unitVolumeToGlobal);
		latticeToGlobal.preScale(divx, divy, divz);
		Matrix4d cubesToGlobal = new Matrix4d();
		cubesToGlobal.set(latticeToGlobal);
		cubesToGlobal.preTranslate(-width, -height, -depth);
		cubesToGlobal.preScale(2,2,2);
		latticeToGlobal.preTranslate(1 - width, 1 - height, 1 - depth);
		latticeToGlobal.preScale(2,2,2);
		for (int i = 0; i < oso.length; ++i)
		{
			for (int z = 0, index = 0; z <= depth; ++z)
			{
				for (int y = 0; y <= height; ++y)
				{
					for (int x = 0; x <= width; ++x, ++index)
					{
						cubesToGlobal.rdotAffine(x, y, z, position);
						values[i][index] = oso[i].evaluate_inner_outer(position);
					}
				}
			}
		}
		VariableStack vs = new VariableStack(variables);
		Variable datVar = new Variable("data");
		vs.addLocal(datVar);
		Variable transVar = new Variable("trans");
		vs.addLocal(transVar);
		Variable eval[] = new Variable[oso.length];
		for (int i = 0; i < oso.length; ++i)
		{
			vs.addLocal(eval[i] = new Variable(oso[i].getId()));
		}
		Variable minTransVar = new Variable("tmin");
		minTransVar.setValue(minTrans);
		vs.addLocal(minTransVar);
		Variable maxTransVar = new Variable("tmax");
		maxTransVar.setValue(maxTrans);
		vs.addLocal(maxTransVar);
		Variable minDataVar = new Variable("dmin");
		minDataVar.setValue(minDat);
		vs.addLocal(minDataVar);
		Variable maxDataVar = new Variable("dmax");
		maxDataVar.setValue(maxDat);
		vs.addLocal(maxDataVar);
		VolumeCalculationEnvironment vce = new VolumeCalculationEnvironment(vs, latticeToGlobal, width, height, depth);
		
		Controller control = new Controller();
		control.calculateRandom(true);
		ImageStack is = dcm == null ? null : dcm.getImageStack();
		double equalityOperationResult[] = null;
		Variable equalityOperationResVar = null;
		int notGivenIndices[] = new int[width * height * depth];
		int notGivenCount = 0;
		if (givenValueOperation != null && isGivenOperation != null)
		{
			equalityOperationResult = new double[width * height * depth];
			for (int z = 0, index = 0; z < depth; ++z)
			{
				for (int y = 0; y < height; ++y)
				{
					for (int x = 0; x < width; ++x, ++index)
					{
						for (int i = 0; i < oso.length; ++i)
						{
							double sum = 0;
							double absSum = 0;
							for (int corner = 0; corner < 8; ++corner)
							{
								final double value = values[i][((z + (corner & 1)) * heightp + y + ((corner & 2) >> 1)) * widthp + x + ((corner & 4) >> 2)];
								sum += value;
								absSum += Math.abs(value);
							}
							
							eval[i].setValue(sum / absSum);
						}
						datVar.setValue(data[index]);
						transVar.setValue(translucency[index]);
						
						vce.setPosition(index, x, y, z);
						equalityOperationResult[index] = givenValueOperation.calculate(vs, control).doubleValue();
						notGivenIndices[index] =  isGivenOperation.calculate(vs, control).booleanValue() ? -1 : notGivenCount++;
					}
				}
			}
			Armadillo.solveDiffusionEquation(width, height, depth, equalityOperationResult, notGivenIndices, notGivenCount);
			vs.add(equalityOperationResVar = new Variable("lres"));
			double minEq = Double.POSITIVE_INFINITY;
			double maxEq = Double.NEGATIVE_INFINITY;
			for (int i = 0; i < equalityOperationResult.length; ++i)
			{
				minEq = Math.min(minEq, equalityOperationResult[i]);
				maxEq = Math.max(maxEq, equalityOperationResult[i]);
			}
			Variable minEqVar = new Variable("eqmin");
			minEqVar.setValue(minEq);
			vs.addLocal(minEqVar);
			Variable maxEqVar = new Variable("eqmax");
			maxEqVar.setValue(maxEq);
			vs.addLocal(maxEqVar);
			System.out.println(new StringBuilder().append('(').append(minEq).append(',').append(maxEq).append(')'));
		}
		double min = Double.POSITIVE_INFINITY, max = Double.NEGATIVE_INFINITY;
		
		for (int z = 0, index = 0; z < depth; ++z)
		{
			for (int y = 0; y < height; ++y)
			{
				for (int x = 0; x < width; ++x, ++index)
				{
					for (int i = 0; i < oso.length; ++i)
					{
						double sum = 0;
						double absSum = 0;
						for (int corner = 0; corner < 8; ++corner)
						{
							final double value = values[i][((z + (corner & 1)) * heightp + y + ((corner & 2) >> 1)) * widthp + x + ((corner & 4) >> 2)];
							sum += value;
							absSum += Math.abs(value);
						}
						
						eval[i].setValue(sum / absSum);
					}
					datVar.setValue(data[index]);
					transVar.setValue(translucency[index]);
					vce.setPosition(index, x, y, z);
					if (equalityOperationResult != null)
					{
						equalityOperationResVar.setValue(equalityOperationResult[index]);
					}
					if (operationIOR != null)
					{
						double res = operationIOR.calculate(vs, control).doubleValue();
						data[index] = (float)res;
						if (is != null)
						{
							is.setVoxel(x, y, z, res);
						}
						min = Math.min(res, min);
						max = Math.max(res, max);
					}
					if (operationTranslucency != null)
					{
						long value = operationTranslucency.calculate(vs, control).longValue();
						if (value > (long)Integer.MAX_VALUE - (long)Integer.MIN_VALUE || value < 0)
						{
							throw new RuntimeException("Translucency value " + value + '-' + '>' + (double)value / ((long)Integer.MAX_VALUE - (long)Integer.MIN_VALUE) + " is out of range");
						}
						translucency[index] = (int)value;					
					}
				}
			}
		}
		System.out.println();
		System.out.println(new StringBuilder().append('(').append(min/0x10000).append(',').append(max/0x10000).append(')'));
		vol.modified();
	}
	
	public void writeBinaryFile(File file) throws IOException
	{
		OutputStream stream = new FileOutputStream(file);
		DataOutputStream outBuf = new DataOutputStream(stream);
		ArrayUtil.write(unitVolumeToGlobal, 0, unitVolumeToGlobal.size(), outBuf);
		vol.writeBinary(outBuf);
		outBuf.close();
		stream.close();
	}
	
	public void readBinaryFile(String file) throws IOException
	{
		InputStream stream = new FileInputStream(file);
		DataInputStream inBuf = new DataInputStream(stream);
		ArrayUtil.readDoubles(unitVolumeToGlobal, 0, unitVolumeToGlobal.size(), inBuf);

		for (int i = 0; i < 3; ++i)
		{
			spacing.setElem(i, Math.sqrt(unitVolumeToGlobal.getRowDot3(i)));
		}
		vol = new Volume(inBuf);
		unitVolumeToGlobal.preScale(vol.width, vol.height, vol.depth);
		inBuf.close();
		stream.close();
		applyMatrix();
		vs = null;
	}
	
	public void readAvi(String file)
	{
		int width = vol.width, height = vol.height, depth = vol.depth;
		ip = ij.plugin.AVI_Reader.open(file, false);
		setSize(ip.getWidth(), ip.getHeight(), ip.getNFrames());
		ImageStack is = ip.getImageStack();
		readVoxels(is, vol.data, width, height, depth);
		dcm = null;
	}
	
	public BufferedImage getSlice(int slice)
	{
		ip.setZ(slice);
		return ip.getBufferedImage();
	}
	
	public void readDycom(String file)
	{
		if (dcm == null)
		{
			 dcm = new DICOM();
		}
		dcm.close();
		dcm.open(file);
		if (dcm.getWidth()==0)
		{
	        IJ.log("Error opening image.dicom");
		}
		else
		{
	        ImageStack is = dcm.getImageStack();
	        setSize(is.getWidth(), is.getHeight(), is.getSize());
	        int width = vol.width, height = vol.height, depth = vol.depth;
	        readVoxels(is, vol.data, width, height, depth);
	        String properties = (String)dcm.getProperty("Info");
			StringReader reader = new StringReader(properties);
			BufferedReader inBuf = new BufferedReader(reader);
			String line;
			StringUtils strUtils = new StringUtils();
			Vector3d v0 = new Vector3d(), v1 = new Vector3d(), v2 = new Vector3d();
			DoubleArrayList dal = new DoubleArrayList();
			try {
				while((line = inBuf.readLine()) != null)
				{
					if (line.startsWith("0020,0032")) {
						readValues(line, strUtils, dal);
						midpoint.set(dal, 0);
						dal.clear();
					}
					else if (line.startsWith("0020,0037"))
					{
						readValues(line, strUtils, dal);
						v0.set(dal, 0);
						v1.set(dal, 3);
						v2.cross(v0, v1);
						dal.clear();
					}
					else if (line.startsWith("0018,0088"))
					{
						readValues(line, strUtils, dal);
						spacing.z = dal.getD(0);
						dal.clear();
					}
					else if (line.startsWith("0028,0030"))
					{
						readValues(line, strUtils, dal);
						spacing.x = dal.getD(0);
						spacing.y = dal.getD(1);
						dal.clear();
					}
				}
				v0.multiply(width * spacing.x);
				v1.multiply(height * spacing.y);
				v2.setLength(depth * spacing.z);
				unitVolumeToGlobal.setCols(v0, v1, v2, midpoint);
				applyMatrix();
				inBuf.close();
				reader.close();
				
			} catch (IOException e) {
				logger.error("Can't read property", e);
			}
	        
    		//readStringPoperty(dcm);
	    }
		vs = null;
	}

	private void readVoxels(ImageStack is, float[] data, int width, int height, int depth) {
		for (int z = 0, index = 0; z < depth; ++z)
		{
        	for (int y = 0; y < height; ++y)
        	{
    	        for (int x = 0; x < width; ++x)
    	        {
        			data[index++] = (float)is.getVoxel(x, y, z);
        		}
        	}
        }
	}

	public static void readValues(String line, StringUtils strUtils, DoubleArrayList dal)
	{
		String split[] = strUtils.split(line, line.indexOf(':')+1, line.length(),'\\');
		for (String item : split)
		{
			dal.add(Double.parseDouble(item));
		}
	}
	
	/*public final int numMeshVertices()
	{
		return 8;
	}*/
	
	/*public void getMeshVertices(float vertices[])
	{
		for (int i = 0, index = 0; i < 8; ++i)
		{
			mat.transformAffine(((i / 4) & 1) * 2 - 1, ((i / 2) & 1) * 2 - 1, (i & 1) * 2 - 1, vertices, index);
			index += 3;
		}
	}*/
	
	/*public int[] getMeshFaces(int faces[])
	{
		int num_faces = 24;
		if (faces == null || faces.length != num_faces)
		{
			faces = new int[num_faces];
		}
		int index = 0;
		for (int dim = 0; dim < 3; ++dim)
		{
			for (int side = 0; side < 2; ++side)
			{
				for (int edge = 0; edge < 4; ++edge)
				{
					int vIndex = 0;
					if (dim == 0)
					{
						vIndex += side;
					}
					vIndex *= 2;
					vIndex += edge / 2;
					if (dim == 1)
					{
						vIndex *= 2;
						vIndex += side;
					}
					vIndex *= 2;
					vIndex += ((edge + 1) / 2) % 2;
					if (dim == 2)
					{
						vIndex *= 2;
						vIndex += side;
					}
					faces[index++] = vIndex;
				}
			}
		}
		return faces;
	}*/
	

	
	public final int numMeshVertices()
	{
		updateMesh();
		return vertexPositions.size();
	}
	
	public void getMeshVertices(float vertices[])
	{
		Matrix4d tmp = new Matrix4d();
		tmp.set(unitVolumeToGlobal);
		tmp.preScale(1. / (vol.width - 3), 1. / (vol.height - 3), 1./(vol.depth - 3));
		for (int index = 0; index < vertexPositions.size(); )
		{
			unitVolumeToGlobal.rdotAffine(vertexPositions, index, vertices, index);
			index += 3;
		}
	}
	
	public int[] getMeshFaces(int faces[])
	{
		if (faces == null || faces.length != faceIndices.size())
		{
			faces = new int[faceIndices.size()];
		}
		faceIndices.write(faces, 0);
		return faces;
	}
	
	DoubleArrayList vertexPositions = new DoubleArrayList();//TODO private final
	IntegerArrayList faceIndices = new IntegerArrayList();
	private VolumeRaytraceOptions options;
	public int numInnerTrajectoryPoints = 10;
	private static int raytraceLoglevel;
	private static Boolean raytraceWriteInstance;
	private static Runnable optionRunnable = new Runnable()
	{
		@Override
		public void run() {
			Options.OptionTreeNode raytrace = Options.getNode("raytrace");
			raytraceLoglevel = Options.getInteger(raytrace, "loglevel");
			raytraceWriteInstance = Options.getBoolean(raytrace, "writeinstance");
		}

	};
	
	static {
		Options.addInvokeModificationListener(optionRunnable);
	}
	
	public void updateMesh()
	{
		vertexPositions.clear();
		faceIndices.clear();

		double low = ArrayUtil.min(vol.data);
		double high = ArrayUtil.max(vol.data);
		Geometry.volumeToMesh(vol.data, vol.width, vol.height, vol.depth, (low + high) / 2, faceIndices, vertexPositions);
	}
		
	public float[] getVolumeVertices(float vertices[])
	{
		int width = vol.width, height = vol.height, depth = vol.depth;
		int num_vertices = width * height * depth;
		if (vertices == null || vertices.length != num_vertices * 3)
		{
			vertices = new float[num_vertices * 3];
		}
		
		for (int z = 1 - depth, index = 0; z < depth + 1; z += 2)
		{
			for (int y = 1 - height; y < height + 1; y += 2)
			{
				for (int x = 1 - width; x < width + 1; x += 2, index += 3)
				{
					unitVolumeToGlobal.rdotAffine((double)x / (width - 3), (double)y / (height - 3), (double)z / (depth - 3), vertices, index);
				}
			}
		}
		return vertices;
	}
	
	public int getRefractiveIndex(double x, double y, double z) {
		double tx = globalToCudaLattice.rdotAffineX(x,y,z) / 0x10000;
		double ty = globalToCudaLattice.rdotAffineY(x,y,z) / 0x10000;
		double tz = globalToCudaLattice.rdotAffineZ(x,y,z) / 0x10000;
		return (int)jcomponents.util.ImageUtil.getSmoothedPixel(tx, ty, tz, vol.data, vol.width, vol.height, vol.depth);
	}
	
	public float[] getVolumeColor(float color[])
	{
		int num_vertices = vol.width * vol.height * vol.depth;
		if (color == null || color.length != num_vertices * 4)
		{
			color = new float[num_vertices * 4];
		}
		float min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
		for (float val : vol.data)
		{
			min = Math.min(val, min);
			max = Math.max(val, max);
		}
		double mult = 1. / (max - min);
		double add = -min * mult;
		for (int i = 0; i < num_vertices; ++i)
		{
			int index = i * 4;
			float p = (float)(vol.data[i] * mult + add);
			color[index] = p;
			color[index + 1] = 1 - p;
			color[index + 2] = 1;
			color[index + 3] = (float)vol.translucency[i] / 0x7FFFFFFF;
		}
		return color;
	}
	
	public void setSize(int width, int height, int depth)
	{
		if (vol.width == width && vol.height == height && vol.depth == depth)
		{
			return;
		}
		vol = new Volume(width, height, depth);
		Arrays.fill(vol.translucency, 0x7FFFFFFF);
		Arrays.fill(vol.data, 0x10000);
		applyMatrix();
		vs = null;
	}
	
	private static final int clip(int value, int min, int max)
	{
		return value <= min ? min : value >= max ? max : value;
	}
	
	
	private final VolumeScene getVolumeScene()
	{
		VolumeScene res = vs;
		if (res == null)
		{
			IntBuffer bounds = Buffers.createIntBuffer(3);
			bounds.put(0, vol.depth);
			bounds.put(1, vol.height);
			bounds.put(2, vol.width);
			IntBuffer translucency = Buffers.createIntBuffer(vol.translucency);
			//IntBuffer ior = Buffers.createIntBuffer(vol.data);
			FloatBuffer ior = Buffers.createFloatBuffer(vol.data.length);
			for (int i=0;i<vol.data.length;i++)
			{
				float value = vol.data[i];
				if (value <= 0)
				{
					throw new RuntimeException("Refractive-index underflow:" + value + "<=" + 0);
				}
	            ior.put(i,value/0x100);
			}
            if (native_raytrace)
			{
				VolumeRaytraceOptions opt = new VolumeRaytraceOptions();
				opt.setWriteInstance(raytraceWriteInstance);
				opt.setLoglevel(raytraceLoglevel);
				if (vs != null)
				{
					vs.destroyLazy();
				}
				vs = res = new VolumeScene(bounds, ior, translucency, opt);
			}
		}
		return res;
	}
	
	public void calculateRays(Object position, Object direction, int directionBegin, int directionEnd, OpticalObject object[], int objectBegin, Object path, int iteration[])
	{
		if (!native_raytrace)
		{
			return;
		}
		cudaLatticeToGlobal.invert(globalToCudaLattice);
		int count = ArrayUtil.count(object, objectBegin, objectBegin + (directionEnd - directionBegin) / 3, this);
		try {
			IntBuffer startPosition = Buffers.createIntBuffer(count * 3);
			//ShortBuffer startDirection = Buffers.createShortBuffer(count * 3);
			FloatBuffer startDirection = Buffers.createFloatBuffer(count * 3);
			IntBuffer endIteration = Buffers.createIntBuffer(count);
			Vector3d tmp = new Vector3d();
			int maxX = 0x10000 * vol.width - 0x20001, maxY = 0x10000 * vol.height - 0x20001, maxZ = 0x10000 * vol.depth - 0x20001;
			for (int i = directionBegin, writeIndex = 0, j = objectBegin; i < directionEnd; i += 3, ++j)
			{
				if (object[j] == this)
				{
					globalToCudaLattice.rdot(direction, i, tmp);
					//tmp.setLength(0x3FFF);
					tmp.multiply(spacing);
					tmp.setLength(0.5);
					Buffers.putRev(startDirection, tmp, writeIndex);
					globalToCudaLattice.rdotAffine(position, i, tmp);
					startPosition.put(writeIndex,     clip((int)tmp.z, 0x10000, maxZ));
					startPosition.put(writeIndex + 1, clip((int)tmp.y, 0x10000, maxY));
					startPosition.put(writeIndex + 2, clip((int)tmp.x, 0x10000, maxX));
					//Buffers.putRev(startPosition, tmp, writeIndex);
					writeIndex += 3;
				}
			}
			VolumeScene vs = getVolumeScene();
			initOptions();
			IntBuffer pathBuffer = path != null ? Buffers.createIntBuffer(maxSteps * 3 * count) : null;
			vs.traceRays(startPosition, startDirection, endIteration, scale, 0, maxSteps, pathBuffer, options);
			for (int i = directionBegin, readIndex = 0, j = 0; i < directionEnd; i += 3, ++j)
			{
				if (object[j] == this)
				{
					Buffers.getRev(startDirection, tmp, readIndex);
					tmp.divide(spacing);
					cudaLatticeToGlobal.rdot(tmp, direction, i);
					double x = tmp.x, y = tmp.y, z = tmp.z;
					Buffers.getRev(startPosition, tmp, readIndex);
					double factor = (10*0x1000)/Math.sqrt(x * x + y * y + z * z);
					tmp.x -= factor * x;tmp.y -= factor * y; tmp.z -= factor * z;
					cudaLatticeToGlobal.rdotAffine(tmp, position, i);
					if (iteration != null)
					{
						iteration[j] = endIteration.get(readIndex / 3);
					}
					if (path != null)
					{
						for (int k = 0; k < maxSteps; ++k)
						{
							Buffers.getRev(pathBuffer, tmp, readIndex * maxSteps + (maxSteps - k - 1) * 3);
							cudaLatticeToGlobal.rdotAffine(tmp, path, (j * maxSteps + k) * 3);
						}
					}
					readIndex += 3;
				}
			}
		}catch(Throwable e)
		{
			logger.error("Error in volume tracing", e);
		}
	}

	private void initOptions() {
		if (options == null)
		{
			options = new VolumeRaytraceOptions();
		}
		options.setWriteInstance(raytraceWriteInstance);
		options.setLoglevel(raytraceLoglevel);
	}

	public void calculateRays(float position[], float direction[], int iteration[], int fromIndex, int toIndex)
	{
		if (!native_raytrace)
		{
			return;
		}
		try {
			IntBuffer startPosition = Buffers.createIntBuffer(toIndex - fromIndex);
			FloatBuffer startDirection = Buffers.createFloatBuffer(toIndex - fromIndex);
			IntBuffer endIteration = Buffers.createIntBuffer((toIndex - fromIndex) / 3);
			Vector3d tmp = new Vector3d();
			int maxX = 0x10000 * vol.width - 0x20001, maxY = 0x10000 * vol.height - 0x20001, maxZ = 0x10000 * vol.depth - 0x20001;
			for (int i = fromIndex, writeIndex = 0; i < toIndex; i += 3, writeIndex += 3)
			{
				globalToCudaLattice.rdot(direction, i, tmp);
				tmp.setLength(0.5);
				Buffers.putRev(startDirection, tmp, writeIndex);
				globalToCudaLattice.rdotAffine(position, i, tmp);
				startPosition.put(writeIndex, 	  clip((int)tmp.z, 0x10000, maxZ));
				startPosition.put(writeIndex + 1, clip((int)tmp.y, 0x10000, maxY));
				startPosition.put(writeIndex + 2, clip((int)tmp.x, 0x10000, maxX));
			}
			VolumeScene vs = getVolumeScene();
			initOptions();
			IntBuffer path = null;
			vs.traceRays(startPosition, startDirection, endIteration, scale, 0, maxSteps, path, options);
			for (int i = fromIndex, readIndex = 0; i < toIndex; i += 3, readIndex += 3)
			{
				Buffers.getRev(startDirection, tmp, readIndex);
				cudaLatticeToGlobal.rdot(tmp, direction, i);
				Buffers.getRev(startPosition, tmp, readIndex);
				cudaLatticeToGlobal.rdotAffine(tmp);
				//tmp.add(direction, i, -5);
				//tmp.add(direction, i, -(float)0.001);
				tmp.write(position, i);
			}
		}catch(Throwable e)
		{
			logger.error("Error in volume tracing", e);
		}
	}
	
	public void calculateRays2(float position[], float direction[], int iteration[], int fromIndex, int toIndex)
	{
		if (!native_raytrace)
		{
			return;
		}
		try {
			IntBuffer startPosition = Buffers.createIntBuffer(toIndex - fromIndex);
			ShortBuffer startDirection = Buffers.createShortBuffer(toIndex - fromIndex);
			IntBuffer endIteration = Buffers.createIntBuffer((toIndex - fromIndex) / 3);
			Vector3d tmp = new Vector3d();
			int maxX = 0x10000 * vol.width - 0x20001, maxY = 0x10000 * vol.height - 0x20001, maxZ = 0x10000 * vol.depth - 0x20001;
			for (int i = fromIndex, writeIndex = 0; i < toIndex; i += 3, writeIndex += 3)
			{
				tmp.set(direction, i);
				globalToCudaLattice.rdot(tmp);
				tmp.setLength(0x3FFF);
				Buffers.putRev(startDirection, tmp, writeIndex);
				globalToCudaLattice.rdotAffine(position, i, tmp);
				startPosition.put(writeIndex, 	  clip((int)tmp.z, 0x10000, maxZ));
				startPosition.put(writeIndex + 1, clip((int)tmp.y, 0x10000, maxY));
				startPosition.put(writeIndex + 2, clip((int)tmp.x, 0x10000, maxX));
			}
			VolumeScene vs = getVolumeScene();
			initOptions();
			IntBuffer path = null;
			vs.traceRays(startPosition, startDirection, endIteration, scale, 0, maxSteps, path, options );
			for (int i = fromIndex, readIndex = 0; i < toIndex; i += 3, readIndex += 3)
			{
				Buffers.getRev(startDirection, tmp, readIndex);
				cudaLatticeToGlobal.rdot(tmp, direction, i);
				Buffers.getRev(startPosition, tmp, readIndex);
				cudaLatticeToGlobal.rdotAffine(tmp);
				tmp.add(direction, i, -5);
				tmp.write(position, i);
			}
		}catch(Throwable e)
		{
			logger.error("Error in volume tracing", e);
		}
	}
	
	@Override
	public Intersection getIntersection(Vector3d position, Vector3d direction, Intersection intersection, double lowerBound, double upperBound)
	{
		double x = this.midpoint.x - position.x, y = this.midpoint.y - position.y, z = this.midpoint.z - position.z;
		int mindir = -1;
		for (int i = 0; i < 3; ++i)
		{
			double dotprod=unitVolumeToGlobalRows[i].dot(direction);
			double offsetdot=unitVolumeToGlobalRows[i].dot(x,y,z);
			if (dotprod == 0)
			{
				if (Math.abs(offsetdot) < 1)
				{
					continue;
				}
				return null;
			}
			dotprod = 1 / dotprod;
			offsetdot *= dotprod;
			dotprod = Math.abs(dotprod);
			double alpha0 = offsetdot - dotprod;
			double alpha1 = offsetdot + dotprod;

			if (alpha0 > lowerBound)
			{
				lowerBound = alpha0;
				mindir = i;
			}
			upperBound = Math.min(upperBound, alpha1);
			if (lowerBound > upperBound)
			{
				return null;
			}
		}
		intersection.position.set(position, direction, lowerBound);
		//System.out.println(position + ' ' + direction + '*' + lowerBound + '=' + intersection.position);
		if (mindir != -1)
		{
			intersection.normal.set(unitVolumeToGlobalRows[mindir]);
		}
		intersection.object = this;
		intersection.distance = lowerBound;
		return intersection;
	}

	public void setVolume(Volume vol)
	{
		this.vol = vol;
		applyMatrix();
		vs = null;
	}
	
	public Volume getVolume() {
		return vol;
	}
}
