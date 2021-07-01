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
package debug;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;

import geometry.FloatVectorObject;
import geometry.Geometry;
import maths.Armadillo;
import maths.Controller;
import maths.Operation;
import maths.OperationCompiler;
import maths.algorithm.Calculate;
import maths.algorithm.OperationCalculate;
import maths.data.RealDoubleOperation;
import maths.data.RealRationalOperation;
import maths.data.StringId;
import maths.exception.OperationParseException;
import maths.functions.atomic.DivisionOperation;
import opengl.BufferUtils;
import util.Buffers;


public class SpeedTests {
	
	private static final void compare(int count, int runPerCount, Runnable ...run)
	{
		long time = 0;
		long sum[] = new long[run.length];
		for (int i = 0; i < count; ++i)
		{
			for (int j = 0; j < run.length; ++j)
			{
				for (int k = 0; k < runPerCount; ++k)
				{
					time = System.nanoTime();
					run[j].run();
					sum[j] += System.nanoTime() - time;
				}
			}
		}
		StringBuilder strB = new StringBuilder();
		for (int j = 0; j < run.length; ++j)
		{
			System.out.println(strB.append(j).append(':').append(' ').append(sum[j] / (count * 1000000000d)));
			strB.setLength(0);
		}
	}
	
	public static void test(){
		System.out.println("result: " + Arrays.toString(Armadillo.solveDiffusionEquation(4,1,1,new double[] {3,0,0,5}, new boolean[] {true, false, false, true})));
		
		
		for (int i = 0; i < 10; ++i)
		{
			compare(10000, 1000, new arrayAdditionTest());
		}
		//compare(100000,1000, new MultiplicationTest());
		//compare(100, 10, new ByteBufferPut0(), new ByteBufferPut1(), new ByteBufferPut2(), new ByteBufferPut3());
		//compare(100, 10, new FillFloatBuffer2(), new FillFloatBuffer());
		//VariableStack vs = new VariableStack();
		//compare(100,200000,new CallChangeListener(vs.variableListenerHeap), new CallChangeListener(vs));
		
	}
	
	public static class FillFloatBuffer implements Runnable{
		FloatBuffer fb = Buffers.createFloatBuffer(1);
		float vertices[] = new float[300000];
		float color[] = new float[400000];
		
		@Override
        public final void run()
		{
			BufferUtils.fillWithVertexAndColorData(vertices, color, fb);
		}
	}
	
	public static class FillFloatBuffer2 implements Runnable{
		FloatBuffer fb = Buffers.createFloatBuffer(1);
		float vertices[] = new float[300000];
		float color[] = new float[400000];
		
		@Override
        public final void run()
		{
			BufferUtils.fillWithVertexAndColorData2(vertices, color, fb);
		}
	}
	
	public static class ByteBufferPut0 implements Runnable{
		FloatBuffer fb = Buffers.createFloatBuffer(1000000);
		int count = fb.capacity() / 2;
		
		@Override
        public final void run()
		{
			int index = 0;
			for (int i =0; i < count; ++i)
			{
				fb.put(index++, i);
				fb.put(index++, i);
			}
		}
	}
	
	public static class ByteBufferPut1 implements Runnable{
		FloatBuffer fb = Buffers.createFloatBuffer(1000000);
		int count = fb.capacity() / 2;
		
		@Override
        public final void run()
		{
			fb.position(0);
			for (int i = 0; i < count; ++i)
			{
				fb.put(i);
				fb.put(i);
			}
		}
	}
	
	public static class ByteBufferPut2 implements Runnable{
		IntBuffer fb = Buffers.createIntBuffer(1000000);
		int array[] = new int[1000000];
		int count = fb.capacity() / 2;
		
		@Override
        public final void run()
		{
			int index = 0;
			for (int i = 0; i < count; ++i)
			{
				array[index++] = i;
				array[index++] = i;
			}
			fb.position(0);
			fb.put(array, 0, array.length);
		}
	}

	public static class ByteBufferPut3 implements Runnable{
		IntBuffer fb = Buffers.createIntBuffer(1000000);
		int array[] = new int[1000000];
		int count = fb.capacity() / 2;
		
		@Override
        public final void run()
		{
			int index = 0;
			for (int i = 0; i < count; ++i)
			{
				array[index++] = i;
				array[index++] = i;
			}
			fb.position(0);
			for (int i =0; i < array.length; ++i)
			{
				fb.put(i, array[i]);
			}
		}
	}



	
	public static class ObjectOrganicationTest implements Runnable
	{
		float verticesf[] = new float[300000];
		float normalsf[] = new float[300000];
		
		@Override
        public void run()
		{
			Geometry.calcVertexNormals(1000, 100, verticesf, normalsf, false, false);	
		}
	}
	
	public static class ObjectOrganicationTest2 implements Runnable
	{
		FloatVectorObject vertices = new FloatVectorObject(100000);
		FloatVectorObject normals = new FloatVectorObject(100000);
		@Override
        public void run()
		{	
			Geometry.calcVertexNormals(1000, 100, vertices, normals, false, false);
		}	
	}
	
	public static class GarbageCollectionTestFinalize implements Runnable{
		
		private static final ArrayList<Object> finalizedObjects = new ArrayList<Object>();
		
		private static class FinalizerObject extends Object{
			@Override
            public void finalize()
			{
				synchronized (finalizedObjects) {
					finalizedObjects.add(this);		
				}
			}
		}
		
		FinalizerObject array[] = new FinalizerObject[2000000];
		public GarbageCollectionTestFinalize()
		{
			for (int i = 0; i < array.length; ++i)
			{
				array[i] = new FinalizerObject();
			}
		}
		
		@Override
		public void run() {
			for (int j = 0; j < 500000; ++j)
			{
				array[j] = new FinalizerObject();
			}
			System.gc();
			synchronized (finalizedObjects) {
				for (int j = 0; j < finalizedObjects.size(); ++j)
				{
					if (finalizedObjects.get(j) == null)
					{
						throw new NullPointerException();
					}
				}
				finalizedObjects.clear();	
				finalizedObjects.trimToSize();
			}
		}
	}
	
	public static final void garbageCollectionTestReference()
	{
		ReferenceQueue<Object> referenceQueue = new ReferenceQueue<Object>();
		Object array[] = new Object[2000000];
		@SuppressWarnings("unchecked")
		Reference<Object> ref[] = new WeakReference[array.length];
		for (int i = 0; i < array.length; ++i)
		{
			array[i] = new Object();
			ref[i] = new WeakReference<Object>(array[i]);
		}
		for (int i = 0; i < 10; ++i)
		{
			for (int j = 0; j < 500000; ++j)
			{
				array[j] = new Object();
				ref[j] = new WeakReference<Object>(array[j]);				
			}
			long time = System.nanoTime();
			System.gc();
			System.out.println(i + ':' + (System.nanoTime() - time) / 1000000000.);
			while (referenceQueue.poll() != null);
		}
	}
	
	@SuppressWarnings("unused")
	private static class normalCalculationTest1 implements Runnable {
		FloatVectorObject fv0 = new FloatVectorObject(1000000);
		FloatVectorObject fv1 = new FloatVectorObject(1000000);
		
		@Override
        public void run()
		{
			Geometry.calcVertexNormals(1000, 1000, fv0, fv1, false, false);
		}
	}

	public static class normalCalculationTest2 implements Runnable {
		FloatVectorObject fv0 = new FloatVectorObject(1000000);
		FloatVectorObject fv1 = new FloatVectorObject(1000000);
		
		@Override
        public void run()
		{
				Geometry.calcVertexNormals2(1000, 1000, fv0, fv1, false, false);
		}
	}
	
	public static class additionTest
	{
		Operation op;
		float erg=0;
		public additionTest() throws OperationParseException
		{
			Calculate.init();
			op = OperationCompiler.compile("1.+2.");
		}
		public void run()
		{
			try {
				//Operation op = ComplexDoubleOperation.get(4, 6);
				//for (int i=0;i<4;i++)
					//op = new AdditionOperation(op, ComplexDoubleOperation.get(3, -6));
				//VariableHeap heap = new VariableHeap();
				Operation a = op.calculate(null, null);
				for (int j=0;j<a.size();j++){
					erg += a.doubleValue();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public static final void ggtTest(){
		for (int i=-1000;i<1000;i++){
			for (int j=-1000;j<1000;j++){
				if (Calculate.ggt(i, j)!= Calculate.ggt2(i, j)){
					System.out.println("error: ggt(" + i + ',' + j + ')' + '=' + Calculate.ggt(i, j) + '!' + '=' + Calculate.ggt2(i, j));
				}
			}
		}
	}
	
	public class ggtTest{
		long res = 0;
		public void run()
		{
			for (int i=0;i<2000;i++){
				for (int j=0;j<2000;j++){
					res += Calculate.ggt2(i, j);
				}
			}
		}
	}
	
	public static class HypotTest implements Runnable{
		double erg=0;
		
		@Override
        public void run()
		{
			final double x = Math.random(), y = Math.random();
			erg += Math.atan(x/y);			
		}
	}
	
	public static class HypotTest2 implements Runnable{
		double erg=0;
		
		@Override
        public void run()
		{
			final double x = Math.random(), y = Math.random();
			erg += Math.atan2(x,y);			
		}
	}
	
	public static class RationalNumber0 implements Runnable
	{
		double erg = 0;
		Operation op1[] = new Operation[100];
		public RationalNumber0()
		{
			Calculate.init();
			for (int i=0;i<op1.length;i++)
				op1[i]= new DivisionOperation(new RealDoubleOperation((double)304/43), new RealDoubleOperation((double)32/12));
		}
		
		@Override
        public void run()
		{
			for (int i=0;i<100000000;i++){
				erg += op1[i%op1.length].calculate(null, null).doubleValue();
			}
		}
	}
	
	public static class RationalNumber1 implements Runnable
	{
		double erg = 0;
		Operation op2[] = new Operation[100];
		public RationalNumber1()
		{
			for (int i=0;i<op2.length;i++)
				op2[i]= new DivisionOperation(RealRationalOperation.getInstance(304,43), RealRationalOperation.getInstance(32,12));
		}
		
		@Override
        public void run()
		{
			for (int i=0;i<100000000;i++){
				erg += op2[i%op2.length].calculate(null, null).doubleValue();
			}
		}
	}
	
	public static class doubleToString0 implements Runnable{
		StringBuilder res = new StringBuilder(100);
		double d = 60.005;

		@Override
        public void run()
		{
			for (int i=0;i<10000000;i++){
				res.setLength(0);
				SpeedTests.toString(d, res);
			}
		}
	}
	
	public static class doubleToString1 implements Runnable{ 
		StringBuilder erg = new StringBuilder(100);
		double d = 60.005;
		@Override
        public void run()
		{
			for (int i=0;i<10000000;i++){
				erg.setLength(0);
				erg.append(d);
			}
		}		
	}
	
	public static final StringBuilder toString(double d, StringBuilder str){
		long bits = Double.doubleToLongBits(d);
		int exponent = (int)((bits >> 52) & 0x7ffL);
		long m = exponent == 0 ? (bits & 0xfffffffffffffL) << 1 : (bits & 0xfffffffffffffL) | 0x10000000000000L;
		/*Number negative*/
		if ((bits >> 63) != 0)
			str.append('-');
		
		
		int e2 = exponent-1075;
		long erg = m;
		long pointDivisor = 1;
		for (int i=e2+10;i<0;i+=10){
			erg = (erg * 1000) >> 10;
			pointDivisor *= 1000;
		}
		
		erg >>= -e2 % 10;
			
		str.append(m>>-e2).append('.');
		long behindPoint = erg % pointDivisor;
		/*erzeuge fuehrende Nullern*/
		for (long i=10*behindPoint;i < pointDivisor;i*=10)
			str.append('0');
		/*Entferne endende Nullen*/
		while (behindPoint % 10 == 0)
			behindPoint /=10;
		return str.append(behindPoint);
	}
	
	public static void getVariableTest(){
		try {
			StringId.StringIdObject str0[] = {StringId.getStringAndId("a"), StringId.getStringAndId("b")};
			Arrays.sort(str0, StringId.idComparator);
			System.out.println(Arrays.toString(str0));
			OperationCompiler.compile("a+b+c+d+i+k+l+o+p+q+r+s+t");
			Operation op = OperationCompiler.compile("l+o+p+q+r+s+t+a+b+c+d+i+k");
			ArrayList<StringId.StringIdObject> erg = new ArrayList<StringId.StringIdObject>();
			for (int j=0;j<2;j++){
				long time = System.nanoTime();
				for (int i=0;i<10000000;i++){
					erg.clear();
					OperationCalculate.getVariables(op, erg);
				}
				System.out.println((System.nanoTime()-time)/1000000000f + "-->" + erg);		
			}			
			for (int j=0;j<2;j++){
				long time = System.nanoTime();
				for (int i=0;i<10000000;i++){
					synchronized(str0){
					erg.clear();
					OperationCalculate.getVariables(op, erg);
					}
				}
				System.out.println((System.nanoTime()-time)/1000000000f + "-->" + erg);		
			}
		} catch (OperationParseException e) {
			e.printStackTrace();
		}
	}
	
	public static class MultiplicationTest implements Runnable{
		double erg = 0;
		Controller control = new Controller();
		Operation op;
		
		public MultiplicationTest()
		{
			Calculate.init();
			try {
				op = OperationCompiler.compile("sqrt(2.0)");
			} catch (OperationParseException e) {
				e.printStackTrace();
			}	
		}
			
		@Override
        public void run()
		{
			erg += op.calculate(null, control).doubleValue();		
		}
	}
	
	public static class arrayAdditionTest implements Runnable{
		Operation op;
		long erg=0;
		public arrayAdditionTest()
		{
			Calculate.init();
			try {
				op = OperationCompiler.compile("{1.,2.,3.,4.,5.}+{6,7,8,9,0}");
			} catch (OperationParseException e) {
				e.printStackTrace();
			}	
		}
		
		@Override
        public void run()
		{
			Operation a = op.calculate(null, null);
			for (int j=0;j<a.size();j++){
				erg += a.get(j).longValue();
			}
		}		
	}
	
	public static class powTest0 implements Runnable{
		long erg=0;
		public powTest0()
		{
			Calculate.init();
		}
		
		@Override
        public void run()
		{
			for (long k=-100;k<100;k++)
				erg = erg+(Long)Calculate.pow(k, 6);		
		}
	}

	public static class powTest1 implements Runnable{
		long erg=0;
		public powTest1()
		{
			Calculate.init();
		}
		
		@Override
        public void run()
		{
			for (long k=-100;k<100;k++)
				erg = erg+(Long)Calculate.pow2(k, 6);		
		}
	}
}
