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
package scene.object;

import geometry.Vector3f;
import util.data.UniqueObjects;

public class FloatVectorObject{
	public final float x[];
	public final float y[];
	public final float z[];
	
	public FloatVectorObject(){
		x = y = z = UniqueObjects.EMPTY_FLOAT_ARRAY;
	}
	
	public FloatVectorObject(int count){
		x = new float[count];
		y = new float[count];
		z = new float[count];
	}
	
	public final void setVector(int index, Vector3f vertex){
		x[index] = vertex.x;
		y[index] = vertex.y;
		z[index] = vertex.z;
	}	
	
	public final void setVector(int index, float xValue, float yValue, float zValue){
		x[index] = xValue;
		y[index] = yValue;
		z[index] = zValue;
	}
	
	public final Vector3f getVector(int index){
		return new Vector3f(x[index], y[index], z[index]);
	}
	
	public final void getVector(int index, Vector3f vector)
	{
		vector.x = x[index];
		vector.y = y[index];
		vector.z = z[index];
	}
	
	public final int size ()
	{
		return x.length;
	}
}
