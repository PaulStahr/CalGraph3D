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
import java.io.FileNotFoundException;
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
import geometry.Geometry;
import geometry.Matrix4d;
import geometry.Vector3d;
import geometry.Vector4d;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.DICOM;
import jcomponents.raytrace.Volume;
import maths.Armadillo;
import maths.Controller;
import maths.Operation;
import maths.algorithm.OperationCalculate;
import maths.data.StringId;
import maths.variable.Variable;
import maths.variable.VariableStack;
import util.ArrayUtil;
import util.Buffers;
import util.Interpolator;
import util.JFrameUtils;
import util.StringUtils;
import util.data.DoubleArrayList;
import util.data.IntegerArrayList;
import util.data.SortedIntegerArrayList;

public abstract class OpticalVolumeObject extends OpticalObject{
	public static final OpticalVolumeObject EMPTY_VOLUME_ARRAY[] = new OpticalVolumeObject[0];
	private static final Logger logger = LoggerFactory.getLogger(OpticalVolumeObject.class);

	private static final boolean loadIfExists(String library)
	{
	    if (!new File(library).exists())
	    {
	        return false;
	    }
	    System.load(library);
	    return true;
	}

	private static boolean native_raytrace = false;
	static
	{
		try
		{
			if (!(loadIfExists("/usr/lib/x86_64-linux-gnu/libcudart.so")
		       || loadIfExists("/usr/local/cuda-8.0/lib64/libcudart.so")
		       || loadIfExists("/usr/lib/nvidia-cuda-toolkit/lib64/libcudart.so")
		       || loadIfExists("/usr/lib/x86_64-linux-gnu/libcudart.so")))
			{
                JFrameUtils.logErrorAndShow("libcudart not found, fallback to noncuda-version", new FileNotFoundException(), logger);
                DataHandler.loadLib("raytracer_java.so");
			}
			else// if (!(loadIfExists("/usr/lib/cuda_raytrace_java.so")))
			{
                DataHandler.loadLib("raytracer_java_cuda.so");
			}
			logger.debug("Raytracer loaded successfully");
			native_raytrace = true;
		}catch(UnsatisfiedLinkError | IOException | NullPointerException e)
		{
			JFrameUtils.logErrorAndShow("Can't load cuda-raytracer", e, logger);
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
		}

		public VolumeRaytraceOptions()
		{
			pointer = new_options();
		}

		public int getLoglevel()                      {return get_option_valuei(pointer, VolumeRaytraceOptionType.LOGLEVEL.id);}
		public void setLoglevel(int value)            {set_option_valuei(pointer, VolumeRaytraceOptionType.LOGLEVEL.id, value);}
		public boolean getWriteInstance()             {return get_option_valueb(pointer, VolumeRaytraceOptionType.WRITE_INSTANCE.id);}
		public void setWriteInstance(boolean value)   {set_option_valueb(pointer, VolumeRaytraceOptionType.WRITE_INSTANCE.id, value);}

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

	public static native long new_options();
    public static native void delete_options(long pointer);

    public static native long new_instance(IntBuffer bounds, IntBuffer ior, IntBuffer transculency, long opt_pt);
	public static native long new_instance(IntBuffer bounds, FloatBuffer ior, IntBuffer transculency, long opt_pt);
    public static native void delete_instance(long pointer);

	public static native int       get_option_valuei(long pointer, long id);
	public static native void      set_option_valuei(long pointer, long id, int value);
	public static native boolean   get_option_valueb(long pointer, long id);
	public static native void      set_option_valueb(long pointer, long id, boolean value);

	public static native void trace_rays(long pointer, IntBuffer start_position, ShortBuffer start_direction, IntBuffer end_iteration, FloatBuffer scale, float minimum_brightness, int iterations, boolean trace_path, IntBuffer path, long option_pointer);

	public static native void trace_rays(long pointer, IntBuffer start_position, FloatBuffer start_direction, IntBuffer end_iteration, FloatBuffer scale, float minimum_brightness, int iterations, boolean trace_path, IntBuffer path, long option_pointer);

	public final Matrix4d unitVolumeToGlobal = new Matrix4d();
	public final Matrix4d globalToUnitVolume = new Matrix4d();
	public final Matrix4d cudaCubesToGlobal = new Matrix4d();
	public final Matrix4d globalToCudaCubes = new Matrix4d();
	private final Matrix4d cubesToGlobal = new Matrix4d();
	private final Matrix4d globalTolattice = new Matrix4d();
	public final Vector3d unitVolumeToGlobalRows[] = new Vector3d[] {new Vector3d(), new Vector3d(), new Vector3d()};
	protected Volume vol = new Volume(0, 0, 0);
	//public double inverserowdotprods[] = new double[3];
    private Vector3d spacing = new Vector3d(Double.NaN, Double.NaN, Double.NaN);
    private Vector3d spacingInf = new Vector3d(Double.NaN, Double.NaN, Double.NaN);
	VolumeScene vs;
	public DICOM dcm = null;
	public ImagePlus ip;
	public Color color = Color.BLACK;
	private final FloatBuffer scale = Buffers.createFloatBuffer(3);
	protected int maxSteps = 8000;
	public double volumeScaling = 1000;
	public double backshift = 0.1;
    private final float[] refMinMax = new float[2];
	private static class VolumeScene
	{
		private long pointer;
		private AtomicInteger running = new AtomicInteger();
		boolean destroy = false;

		private VolumeScene(IntBuffer bounds, IntBuffer ior, IntBuffer translucency, VolumeRaytraceOptions opt)   {pointer = new_instance(bounds, ior, translucency, opt.pointer);}
		private VolumeScene(IntBuffer bounds, FloatBuffer ior, IntBuffer translucency, VolumeRaytraceOptions opt) {pointer = new_instance(bounds, ior, translucency, opt.pointer);}

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
            //System.out.println(Buffers.toString(start_position));
            //System.out.println(Buffers.toString(start_direction));
			trace_rays(pointer, start_position, start_direction, end_iteration, scale, minimum_brightness, iterations, path != null, path, options.pointer);
            //System.out.println(Buffers.toString(end_iteration));
			//System.out.println(Buffers.toString(start_position));
            //System.out.println(Buffers.toString(start_direction));
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
		globalToCudaCubes.set(globalToUnitVolume);
		cudaCubesToGlobal.set(unitVolumeToGlobal);
		globalToCudaCubes.postTranslate(1, 1, 1);
		cudaCubesToGlobal.preTranslate(-1, -1, -1);
		double mult = 0.5 * 0x10000;
		double xScale = mult * (width - 3), yScale = mult * (height - 3), zScale = mult * (depth - 3);
		globalToCudaCubes.postScale(xScale, yScale, zScale);
		cudaCubesToGlobal.preScale(1/xScale, 1/yScale, 1/zScale);
		cudaCubesToGlobal.getColDot3(spacing);
		spacing.sqrt();
		spacing.multiply(0x10000 * 0.5);
		spacingInf.set(1f/spacing.x,1f/spacing.y,1f/spacing.z);
		globalToCudaCubes.postTranslate(0x10000, 0x10000, 0x10000);
		cudaCubesToGlobal.preTranslate(-0x10000, -0x10000, -0x10000);
		cubesToGlobal.set(unitVolumeToGlobal);
        double divx = 1. / (width - 3), divy = 1. / (height - 3), divz = 1. / (depth - 3);
        cubesToGlobal.preScale(divx, divy, divz);
        cubesToGlobal.preTranslate(1 - width, 1 - height, 1 - depth);
        cubesToGlobal.preScale(2,2,2);
		scale.put(0,(float)(volumeScaling * spacingInf.z)).put(1,(float)(volumeScaling * spacingInf.y)).put(2,(float)(volumeScaling * spacingInf.x));
		modified();
	}

	static class VolumeCalculationEnvironment{
		private final Variable xVar = new Variable("x");
		private final Variable yVar = new Variable("y");
		private final Variable zVar = new Variable("z");
		private final Variable xLocalVar = new Variable("lx");
		private final Variable yLocalVar = new Variable("ly");
		private final Variable zLocalVar = new Variable("lz");
		private final Variable indexVar = new Variable("index");
		@SuppressWarnings("unused")
        private final VariableStack vs;
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
	    long time = System.nanoTime();
		int width = vol.width, height = vol.height, depth = vol.depth;
		float data[] = vol.data;
		int translucency[] = vol.translucency;
		Vector3d position = new Vector3d();
		SortedIntegerArrayList ial = new SortedIntegerArrayList();
		if (operationIOR != null)			{OperationCalculate.getVariables(operationIOR, ial);}
		if (operationTranslucency != null)	{OperationCalculate.getVariables(operationTranslucency, ial);}
		if (givenValueOperation != null)	{OperationCalculate.getVariables(givenValueOperation, ial);}
		if (isGivenOperation != null)		{OperationCalculate.getVariables(isGivenOperation, ial);}
		ArrayList<OpticalSurfaceObject> tmpList = new ArrayList<>();
		for (int i = 0; i < oso.length; ++i)
		{
			if (ial.contains(StringId.getIdIfExist(oso[i].getId())))
			{
				tmpList.add(oso[i]);
			}
		}
		float minMaxDat[] = ArrayUtil.minMax(data, new float[2]);
		int minMaxTrans[] = ArrayUtil.minMax(translucency, new int[2]);

		oso = tmpList.toArray(new OpticalSurfaceObject[tmpList.size()]);
		final double values[][] = new double[oso.length][width * height * depth];

		for (int z = 0, index = 0; z < depth; ++z)
		{
			for (int y = 0; y < height; ++y)
			{
				for (int x = 0; x <width; ++x, ++index)
				{
                    this.cubesToGlobal.rdotAffine(x, y, z, position);
			        for (int i = 0; i < oso.length; ++i)
			        {
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
		Variable minTransVar = new Variable("tmin", minMaxTrans[0]);
		vs.addLocal(minTransVar);
		Variable maxTransVar = new Variable("tmax", minMaxTrans[1]);
		vs.addLocal(maxTransVar);
		Variable minDataVar = new Variable("dmin", minMaxDat[0]);
		vs.addLocal(minDataVar);
		Variable maxDataVar = new Variable("dmax", minMaxDat[1]);
		vs.addLocal(maxDataVar);
		VolumeCalculationEnvironment vce = new VolumeCalculationEnvironment(vs, this.cubesToGlobal, width, height, depth);

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
                        vce.setPosition(index, x, y, z);
						for (int i = 0; i < oso.length; ++i)
						{
							eval[i].setValue(values[i][index]);
						}
						datVar.setValue(data[index]);
						transVar.setValue(translucency[index]);

						equalityOperationResult[index] = givenValueOperation.calculate(vs, control).doubleValue();
						notGivenIndices[index] =  isGivenOperation.calculate(vs, control).booleanValue() ? -1 : notGivenCount++;
					}
				}
			}
			Armadillo.solveDiffusionEquation(width, height, depth, equalityOperationResult, notGivenIndices, notGivenCount);
			vs.add(equalityOperationResVar = new Variable("lres"));
			double eqLimits[] = ArrayUtil.minMax(equalityOperationResult, new double[2]);
			Variable minEqVar = new Variable("eqmin", eqLimits[0]);
			vs.addLocal(minEqVar);
			Variable maxEqVar = new Variable("eqmax", eqLimits[1]);
			vs.addLocal(maxEqVar);
			logger.debug(new StringBuilder().append('(').append(eqLimits[0]).append(',').append(eqLimits[1]).append(')').toString());
		}
		double min = Double.POSITIVE_INFINITY, max = Double.NEGATIVE_INFINITY;
		datVar.setValue((Operation)null);
		transVar.setValue((Operation)null);
		for (int i = 0; i < eval.length; ++i){eval[i].setValue((Operation)null);}
        control.calculateLoop(false);
        control.calculateRandom(false);
        control.connectEmptyVariables(true);
        if (operationIOR != null) {operationIOR.calculate(vs, control);}
        if (operationTranslucency != null) {operationTranslucency = operationTranslucency.calculate(vs, control);}
        control.calculateLoop(true);
        control.calculateRandom(true);
        System.out.println((System.nanoTime() - time)/1000000000f);

		for (int z = 0, index = 0; z < depth; ++z)
		{
			for (int y = 0; y < height; ++y)
			{
				for (int x = 0; x < width; ++x, ++index)
				{
                    vce.setPosition(index, x, y, z);
					for (int i = 0; i < oso.length; ++i)
					{
                        eval[i].setValue(values[i][index]);
					}
					datVar.setValue(data[index]);
					transVar.setValue(translucency[index]);
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
						if (value > (long)Integer.MAX_VALUE - (long)Integer.MIN_VALUE)
						{
						    value = (long)Integer.MAX_VALUE - (long)Integer.MIN_VALUE;
						}
						else if (value < 0)
						{
						    value = 0;
						}
						translucency[index] = (int)value;
					}
				}
			}
		}
		logger.debug(new StringBuilder().append('(').append(min).append(',').append(max).append(')').append(' ').append((System.nanoTime() - time) / 1000000000f).toString());
		ArrayUtil.minMax(vol.data, refMinMax);
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
		Vector4d v0 = new Vector4d(), v1 = new Vector4d();
        unitVolumeToGlobal.getCol(0, v0);
        unitVolumeToGlobal.getCol(2, v1);
        unitVolumeToGlobal.setCol(2, v0);
        unitVolumeToGlobal.setCol(0, v1);

		Vector3d vec = new Vector3d();
		unitVolumeToGlobal.getColDot3(vec);
		vec.sqrt();
		System.out.println(vec);
		vol = new Volume(inBuf);
		unitVolumeToGlobal.preScale((vol.width - 1)* 0.5, (vol.height - 1) * 0.5, (vol.depth - 1) * 0.5);
        applyMatrix();
        vs = null;
		inBuf.close();
		stream.close();
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
            int width = is.getWidth(), height = is.getHeight(), depth = is.getSize();
	        setSize(width, height, depth);
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
				v2.setNorm(depth * spacing.z);
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
		synchronized(vertexPositions)
		{
    		for (int index = 0; index < vertexPositions.size(); index += 3)
    		{
    			cubesToGlobal.rdotAffine(vertexPositions, index, vertices, index);
    		}
		}
	}

	public int[] getMeshFaces(int faces[])
	{
	    synchronized(vertexPositions)
	    {
    		if (faces == null || faces.length != faceIndices.size()){return faceIndices.toArrayI();}
    		faceIndices.write(faces, 0);
    		return faces;
	    }
	}

	private final DoubleArrayList vertexPositions = new DoubleArrayList();
	private final IntegerArrayList faceIndices = new IntegerArrayList();
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
	    synchronized(vertexPositions)
	    {
    		vertexPositions.clear();
    		faceIndices.clear();
    		ArrayUtil.minMax(vol.data, refMinMax);
            Geometry.volumeToMesh(vol.data, vol.width, vol.height, vol.depth, (refMinMax[0] * 0.75 + refMinMax[1] * 0.25), faceIndices, vertexPositions);
            Geometry.volumeToMesh(vol.data, vol.width, vol.height, vol.depth, (refMinMax[0] + refMinMax[1]) * 0.5, faceIndices, vertexPositions);
            Geometry.volumeToMesh(vol.data, vol.width, vol.height, vol.depth, (refMinMax[0] * 0.25+ refMinMax[1] * 0.75), faceIndices, vertexPositions);
            Geometry.volumeToMesh(vol.data, vol.width, vol.height, vol.depth, (refMinMax[0] * 0.1+ refMinMax[1] * 0.9), faceIndices, vertexPositions);
            Geometry.collapseShortEdges(vertexPositions, faceIndices, 0.001);
	    }
	}

	public float[] getVolumeVertices(float vertices[])
	{
		int width = vol.width, height = vol.height, depth = vol.depth;
		int num_vertices = width * height * depth;
		if (vertices == null || vertices.length != num_vertices * 3){vertices = new float[num_vertices * 3];}
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

    public float getRefractiveIndex(double x, double y, double z) {
        double tx = globalToCudaCubes.rdotAffineX(x,y,z) / 0x10000;
        double ty = globalToCudaCubes.rdotAffineY(x,y,z) / 0x10000;
        double tz = globalToCudaCubes.rdotAffineZ(x,y,z) / 0x10000;
        if (tx < 0 || tx > vol.width || ty < 0 || ty > vol.height || tz < 0 || tz > vol.depth)
        {
            return -1;
        }
        return Interpolator.interpolatePoint(tx, ty, tz, vol.data, vol.width, vol.height, vol.depth);
    }


    public float getOpacity(double x, double y, double z) {
        double tx = globalToCudaCubes.rdotAffineX(x,y,z) / 0x10000;
        double ty = globalToCudaCubes.rdotAffineY(x,y,z) / 0x10000;
        double tz = globalToCudaCubes.rdotAffineZ(x,y,z) / 0x10000;
        if (tx < 0 || tx > vol.width || ty < 0 || ty > vol.height || tz < 0 || tz > vol.depth)
        {
            return -1;
        }
        return Interpolator.interpolateUnsignedPoint(tx, ty, tz, vol.translucency, vol.width, vol.height, vol.depth);
    }

	public float[] getVolumeColor(float color[])
	{
		int num_vertices = vol.width * vol.height * vol.depth;
        color = ArrayUtil.setToLength(color, num_vertices);
		ArrayUtil.minMax(vol.data, refMinMax);
		double mult = 1. / (refMinMax[1] - refMinMax[0]);
		double add = -refMinMax[0] * mult;
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
		Arrays.fill(vol.data, 1);
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
				if (!(value > 0)){throw new RuntimeException("Refractive-index underflow:" + value + "<=" + 0);}
	            ior.put(i,value * 0x100);
			}
            if (native_raytrace)
			{
				VolumeRaytraceOptions opt = new VolumeRaytraceOptions();
				opt.setWriteInstance(raytraceWriteInstance);
				opt.setLoglevel(raytraceLoglevel);
				if (vs != null){vs.destroyLazy();}
				vs = res = new VolumeScene(bounds, ior, translucency, opt);
			}
		}
		return res;
	}

    private void initOptions() {
        if (options == null)
        {
            options = new VolumeRaytraceOptions();
        }
        options.setWriteInstance(raytraceWriteInstance);
        options.setLoglevel(raytraceLoglevel);
    }

	public void calculateRays(Object position, Object direction, int directionBegin, int directionEnd, OpticalObject object[], int objectBegin, Object path, int iteration[])
	{
		if (!native_raytrace){return;}
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
					globalToCudaCubes.rdot(direction, i, tmp);
					tmp.multiply(spacing);
					tmp.setNorm(0.5);
					Buffers.putRev(startDirection, tmp, writeIndex);
					globalToCudaCubes.rdotAffine(position, i, tmp);
        			startPosition.put(writeIndex,     clip((int)tmp.z, 0x10000, maxZ));
					startPosition.put(writeIndex + 1, clip((int)tmp.y, 0x10000, maxY));
					startPosition.put(writeIndex + 2, clip((int)tmp.x, 0x10000, maxX));
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
					tmp.multiply(spacingInf);
                    double factor = (backshift*0x10000)/tmp.norm();
                    double x = tmp.x, y = tmp.y, z = tmp.z;
					cudaCubesToGlobal.rdot(tmp, direction, i);
             		Buffers.getRev(startPosition, tmp, readIndex);
					tmp.add(x, y, z, -factor);
					cudaCubesToGlobal.rdotAffine(tmp, position, i);
					if (iteration != null){iteration[j] = endIteration.get(readIndex / 3);}
					if (path != null)
					{
						for (int k = 0; k < maxSteps; ++k)
						{
							Buffers.getRev(pathBuffer, tmp, readIndex * maxSteps + (maxSteps - k - 1) * 3);
							cudaCubesToGlobal.rdotAffine(tmp, path, (j * maxSteps + k) * 3);
						}
					}
					readIndex += 3;
				}
			}
		}catch(Throwable e){logger.error("Error in volume tracing", e);}
	}

	public void calculateRays(float position[], float direction[], int iteration[], int fromIndex, int toIndex)
	{
		if (!native_raytrace){return;}
        int count = (toIndex - fromIndex) / 3;
        try {
			IntBuffer startPosition = Buffers.createIntBuffer(count * 3);
			FloatBuffer startDirection = Buffers.createFloatBuffer(count * 3);
			IntBuffer endIteration = Buffers.createIntBuffer(count);
			Vector3d tmp = new Vector3d();
			int maxX = 0x10000 * vol.width - 0x20001, maxY = 0x10000 * vol.height - 0x20001, maxZ = 0x10000 * vol.depth - 0x20001;
			for (int i = fromIndex, writeIndex = 0; i < toIndex; i += 3, writeIndex += 3)
			{
				globalToCudaCubes.rdot(direction, i, tmp);
                tmp.multiply(spacing);
				tmp.setNorm(0.5);
				Buffers.putRev(startDirection, tmp, writeIndex);
				globalToCudaCubes.rdotAffine(position, i, tmp);
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
                tmp.multiply(spacingInf);
                double factor = (backshift*0x10000)/tmp.norm();
                double x = tmp.x, y = tmp.y, z = tmp.z;
				cudaCubesToGlobal.rdot(tmp, direction, i);
				Buffers.getRev(startPosition, tmp, readIndex);
                tmp.add(x, y, z, -factor);
				cudaCubesToGlobal.rdotAffine(tmp);
				tmp.write(position, i);
			}
		}catch(Throwable e){logger.error("Error in volume tracing", e);}
	}

	/*public void calculateRays2(float position[], float direction[], int iteration[], int fromIndex, int toIndex)
	{
		if (!native_raytrace){return;}
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
				tmp.setNorm(0x3FFF);
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
	}*/

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

	public Volume getVolume() {return vol;}

    public float getRefractiveMin() {return refMinMax[0];}

    public float getRefractiveMax() {return refMinMax[1];}
}
