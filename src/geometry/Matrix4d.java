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
package geometry;

import maths.algorithm.Calculate;
import util.data.DoubleArrayList;
import util.data.DoubleList;

public final class Matrix4d implements Matrixd, DoubleList{
	public double x0, y0, z0, w0;
	public double x1, y1, z1, w1;
	public double x2, y2, z2, w2;
	public double x3, y3, z3, w3;

	public Matrix4d(){this(1,0,0,0,0,1,0,0,0,0,1,0,0,0,0,1);}
	
	public Matrix4d(double diag){this(diag,0,0,0,0,diag,0,0,0,0,diag,0,0,0,0,diag);}
	
	public Matrix4d(double x0, double x1, double x2, double x3, double y0, double y1, double y2, double y3, double z0, double z1, double z2, double z3, double w0, double w1, double w2, double w3){
		this.x0 = x0;this.x1 = x1;this.x2 = x2;this.x3 = x3;
		this.y0 = y0;this.y1 = y1;this.y2 = y2;this.y3 = y3;
		this.z0 = z0;this.z1 = z1;this.z2 = z2;this.z3 = z3;
		this.w0 = w0;this.w1 = w1;this.w2 = w2;this.w3 = w3;
	}
	
	public final void set(Matrix3d mat)
	{
		this.x0 = mat.x0;this.x1 = mat.x1;this.x2 = mat.x2;
		this.y0 = mat.y0;this.y1 = mat.y1;this.y2 = mat.y2;
		this.z0 = mat.z0;this.z1 = mat.z1;this.z2 = mat.z2;
	}
	
	public final void set(double x0, double x1, double x2, double x3, double y0, double y1, double y2, double y3, double z0, double z1, double z2, double z3, double w0, double w1, double w2, double w3){
		this.x0 = x0;this.x1 = x1;this.x2 = x2;this.x3 = x3;
		this.y0 = y0;this.y1 = y1;this.y2 = y2;this.y3 = y3;
		this.z0 = z0;this.z1 = z1;this.z2 = z2;this.z3 = z3;
		this.w0 = w0;this.w1 = w1;this.w2 = w2;this.w3 = w3;
	}
	
	public final void invert(Matrix4d read)
	{
		double [] mat = new double[32];
		read.getColMajor(mat, 0, 8);
		mat[4] = mat[13] = mat[22] = mat[31] = 1;
		Calculate.toRREF(mat, 4);
		setColMajor(mat, 4, 8);
	}
	
	public final void getRow(int row, Vector3d vec)
	{
		switch (row)
		{
			case 0:vec.set(x0, y0, z0);return;
			case 1:vec.set(x1, y1, z1);return;
			case 2:vec.set(x2, y2, z2);return;
			case 3:vec.set(x3, y3, z3);return;
			default: throw new ArrayIndexOutOfBoundsException(row);
		}
	}
	
	public final void getCol(int col, Vector3d vec)
	{
		switch (col)
		{
			case 0: vec.set(x0, x1, x2);return;
			case 1: vec.set(y0, y1, y2);return;
			case 2: vec.set(z0, z1, z2);return;
			case 3: vec.set(w0, w1, w2);return;
			default: throw new ArrayIndexOutOfBoundsException(col);
		}
	}
	
	public final void setRow(int row, Vector3d vec)
	{
		switch (row)
		{
			case 0:x0 = vec.x; y0 = vec.y; z0 = vec.z;return;
			case 1:x1 = vec.x; y1 = vec.y; z1 = vec.z;return;
			case 2:x2 = vec.x; y2 = vec.y; z2 = vec.z;return;
			case 3:x3 = vec.x; y3 = vec.y; z3 = vec.z;return;
			default: throw new ArrayIndexOutOfBoundsException(row);
		}
	}
	
	public final void setCol(int col, Vector3d vec)
	{
		switch (col)
		{
			case 0: x0 = vec.x; x1 = vec.y; x2 = vec.z;return;
			case 1: y0 = vec.x; y1 = vec.y; y2 = vec.z;return;
			case 2: z0 = vec.x; z1 = vec.y; z2 = vec.z;return;
			case 3: w0 = vec.x; w1 = vec.y; w2 = vec.z;return;
			default: throw new ArrayIndexOutOfBoundsException(col);
		}
	}
	
	public final void setCols(Vector3d x, Vector3d y, Vector3d z, Vector3d w)
	{
			x0 = x.x; x1 = x.y; x2 = x.z;
			y0 = y.x; y1 = y.y; y2 = y.z;
			z0 = z.x; z1 = z.y; z2 = z.z;
			w0 = w.x; w1 = w.y; w2 = w.z;
	}
	
	public final void setColMajor(final double mat[][]){
		x0 = mat[0][0]; x1 = mat[0][1]; x2 = mat[0][2]; x3 = mat[0][3];
		y0 = mat[1][0]; y1 = mat[1][1]; y2 = mat[1][2]; y3 = mat[1][3];
		z0 = mat[2][0]; z1 = mat[2][1]; z2 = mat[2][2]; z3 = mat[2][3];
		w0 = mat[3][0]; w1 = mat[3][1]; w2 = mat[3][2]; w3 = mat[3][3];
	}
	
	public final void setColMajor(final double mat[]){
		x0 = mat[0];  x1 = mat[1];  x2 = mat[2];  x3 = mat[3];
		y0 = mat[4];  y1 = mat[5];  y2 = mat[6];  y3 = mat[7];
		z0 = mat[8];  z1 = mat[9];  z2 = mat[10]; z3 = mat[11];
		w0 = mat[12]; w1 = mat[13]; w2 = mat[14]; w3 = mat[15];
	}
	
	public final void setColMajor(final double mat[][], int row, int col){
		x0 = mat[0 + row][col]; x1 = mat[0 + row][1 + col]; x2 = mat[0 + row][2 + col]; x3 = mat[0 + row][3 + col];
		y0 = mat[1 + row][col]; y1 = mat[1 + row][1 + col]; y2 = mat[1 + row][2 + col]; y3 = mat[1 + row][3 + col];
		z0 = mat[2 + row][col]; z1 = mat[2 + row][1 + col]; z2 = mat[2 + row][2 + col]; z3 = mat[2 + row][3 + col];
		w0 = mat[3 + row][col]; w1 = mat[3 + row][1 + col]; w2 = mat[3 + row][2 + col]; w3 = mat[3 + row][3 + col];
	}
	
	public final void setColMajor(final double mat[], int pos, int stride){
		x0 = mat[pos]; x1 = mat[pos+1]; x2 = mat[pos+2]; x3 = mat[pos+3];pos += stride;
		y0 = mat[pos]; y1 = mat[pos+1]; y2 = mat[pos+2]; y3 = mat[pos+3];pos += stride;
		z0 = mat[pos]; z1 = mat[pos+1]; z2 = mat[pos+2]; z3 = mat[pos+3];pos += stride;
		w0 = mat[pos]; w1 = mat[pos+1]; w2 = mat[pos+2]; w3 = mat[pos+3];
	}
	
	public final void getColMajor(final double mat[][]){
		mat[0][0] = x0; mat[0][1] = x1; mat[0][2] = x2; mat[0][3] = x3;
		mat[1][0] = y0; mat[1][1] = y1; mat[1][2] = y2; mat[1][3] = y3;
		mat[2][0] = z0; mat[2][1] = z1; mat[2][2] = z2; mat[2][3] = z3;
		mat[3][0] = w0; mat[3][1] = w1; mat[3][2] = w2; mat[3][3] = w3;
	}
	
	public final void getColMajor(final double mat[]){
		mat[0]  = x0; mat[1] = x1;  mat[2]  = x2; mat[3] = x3;
		mat[4]  = y0; mat[5] = y1;  mat[6]  = y2; mat[7] = y3;
		mat[8]  = z0; mat[9] = z1;  mat[10] = z2; mat[11] = z3;
		mat[12] = w0; mat[13] = w1; mat[14] = w2; mat[15] = w3;
	}
	
	public final void getColMajor(final double mat[], int begin, int stride){
		mat[begin] = x0; mat[begin+1] = x1; mat[begin+2] = x2; mat[begin+3] = x3;begin += stride;
		mat[begin] = y0; mat[begin+1] = y1; mat[begin+2] = y2; mat[begin+3] = y3;begin += stride;
		mat[begin] = z0; mat[begin+1] = z1; mat[begin+2] = z2; mat[begin+3] = z3;begin += stride;
		mat[begin] = w0; mat[begin+1] = w1; mat[begin+2] = w2; mat[begin+3] = w3;
	}
	
	public final void setRowMajor(final double mat[][]){
		x0 = mat[0][0]; x1 = mat[1][0]; x2 = mat[2][0]; x3 = mat[3][0];
		y0 = mat[0][1]; y1 = mat[1][1]; y2 = mat[2][1]; y3 = mat[3][1];
		z0 = mat[0][2]; z1 = mat[1][2]; z2 = mat[2][2]; z3 = mat[3][2];
		w0 = mat[0][3]; w1 = mat[1][3]; w2 = mat[2][3]; w3 = mat[3][3];
	}
	
	public final void setRowMajor(final double mat[]){
		x0 = mat[0]; x1 = mat[4]; x2 = mat[8]; x3 = mat[12];
		y0 = mat[1]; y1 = mat[5]; y2 = mat[9]; y3 = mat[13];
		z0 = mat[2]; z1 = mat[6]; z2 = mat[10]; z3 = mat[14];
		w0 = mat[3]; w1 = mat[7]; w2 = mat[11]; w3 = mat[15];
	}
	
	public final void setRowMajor(final double mat[][], int row, int col){
		x0 = mat[row][0 + col]; x1 = mat[1 + row][0 + col]; x2 = mat[2 + row][0 + col]; x3 = mat[3 + row][0 + col];
		y0 = mat[row][1 + col]; y1 = mat[1 + row][1 + col]; y2 = mat[2 + row][1 + col]; y3 = mat[3 + row][1 + col];
		z0 = mat[row][2 + col]; z1 = mat[1 + row][2 + col]; z2 = mat[2 + row][2 + col]; z3 = mat[3 + row][2 + col];
		w0 = mat[row][3 + col]; w1 = mat[1 + row][3 + col]; w2 = mat[2 + row][3 + col]; w3 = mat[3 + row][3 + col];
	}
	
	public final void getRowMajor(final double mat[][]){
		mat[0][0] = x0; mat[1][0] = x1; mat[2][0] = x2; mat[3][0] = x3;
		mat[0][1] = y0; mat[1][1] = y1; mat[2][1] = y2; mat[3][1] = y3;
		mat[0][2] = z0; mat[1][2] = z1; mat[2][2] = z2; mat[3][2] = z3;
		mat[0][3] = w0; mat[1][3] = w1; mat[2][3] = w2; mat[3][3] = w3;
	}
	
	//public final void transform(Rotation3 rot){
		//double radX = rot.getXRadians();
		//double radY = rot.getXRadians();
		//double radZ = rot.getXRadians();
		
	//}
	
	public final void transformAffine(Vector3f v){
		final float x = v.x, y = v.y, z = v.z;
		v.x = (float)(x0 * x + y0 * y + z0 * z + w0);
		v.y = (float)(x1 * x + y1 * y + z1 * z + w1);
		v.z = (float)(x2 * x + y2 * y + z2 * z + w2);
	}
	
	public final double transformX(double x, double y, double z, double w){return x0 * x + y0 * y + z0 * z + w0 * w;}
	public final double transformY(double x, double y, double z, double w){return x1 * x + y1 * y + z1 * z + w1 * w;}
	public final double transformZ(double x, double y, double z, double w){return x2 * x + y2 * y + z2 * z + w2 * w;}
	public final double transformW(double x, double y, double z, double w){return x3 * x + y3 * y + z3 * z + w3 * w;}
	public final double transformAffineX(double x, double y, double z){return x0 * x + y0 * y + z0 * z + w0;}
	public final double transformAffineY(double x, double y, double z){return x1 * x + y1 * y + z1 * z + w1;}
	public final double transformAffineZ(double x, double y, double z){return x2 * x + y2 * y + z2 * z + w2;}
	public final double transformAffineW(double x, double y, double z){return x3 * x + y3 * y + z3 * z + w3;}
	public final double transformX(double x, double y, double z){return x0 * x + y0 * y + z0 * z;}
	public final double transformY(double x, double y, double z){return x1 * x + y1 * y + z1 * z;}
	public final double transformZ(double x, double y, double z){return x2 * x + y2 * y + z2 * z;}
	public final double transformW(double x, double y, double z){return x3 * x + y3 * y + z3 * z;}
	
	public final void affineLeftTranslate(double x, double y, double z)
	{
		w0 += x * x0 + y * y0 + z * z0;
		w1 += x * x1 + y * y1 + z * z1;
		w2 += x * x2 + y * y2 + z * z2;
	}
	
	public final void affineRightTranslate(double x, double y, double z){w0 += x;w1 += y;w2 += z;}
	
	public final void affineLeftScale(double s)
	{
		x0 *= s; y0 *= s; z0 *= s;
		x1 *= s; y1 *= s; z1 *= s;
		x2 *= s; y2 *= s; z2 *= s;
	}

	public final void affineLeftScale(double x, double y, double z) {
		x0 *= x; y0 *= y; z0 *= z;
		x1 *= x; y1 *= y; z1 *= z;
		x2 *= x; y2 *= y; z2 *= z;
	}

	public final void affineRightScale(double s)
	{
		x0 *= s; y0 *= s; z0 *= s; w0 *= s;
		x1 *= s; y1 *= s; z1 *= s; w1 *= s;
		x2 *= s; y2 *= s; z2 *= s; w2 *= s;
	}

	public final void affineRightScale(double x, double y, double z)
	{
		x0 *= x; y0 *= x; z0 *= x; w0 *= x;
		x1 *= y; y1 *= y; z1 *= y; w1 *= y;
		x2 *= z; y2 *= z; z2 *= z; w2 *= z;
	}

	public final void transform(Vector4d vector){
		final double x = vector.x, y = vector.y, z = vector.z, w = vector.w;
		vector.x = x0 * x + y0 * y + z0 * z + w0 * w;
		vector.y = x1 * x + y1 * y + z1 * z + w1 * w;
		vector.z = x2 * x + y2 * y + z2 * z + w2 * w;
		vector.w = x3 * x + y3 * y + z3 * z + w3 * w;
	}

	public final void transformAffine(Vector3d vector){
		final double x = vector.x, y = vector.y, z = vector.z;
		vector.x = x0 * x + y0 * y + z0 * z + w0;
		vector.y = x1 * x + y1 * y + z1 * z + w1;
		vector.z = x2 * x + y2 * y + z2 * z + w2;
	}
	
	public final void transform(Vector3d vector)
	{
		final double x = vector.x, y = vector.y, z = vector.z;
		vector.x = x0 * x + y0 * y + z0 * z;
		vector.y = x1 * x + y1 * y + z1 * z;
		vector.z = x2 * x + y2 * y + z2 * z;
	}
	
	public final void transformAffine(Vector3d vector, Vector3d out){
		final double x = vector.x, y = vector.y, z = vector.z;
		out.x = x0 * x + y0 * y + z0 * z + w0;
		out.y = x1 * x + y1 * y + z1 * z + w1;
		out.z = x2 * x + y2 * y + z2 * z + w2;
	}
	
	public final void transformAffine(Vector3d vector, float out[], int index){
		transformAffine(vector.x, vector.y, vector.z, out, index);
	}
	
	public final void transformAffine(double x, double y, double z, float out[], int index){
		out[index]   = (float)(x0 * x + y0 * y + z0 * z + w0);
		out[++index] = (float)(x1 * x + y1 * y + z1 * z + w1);
		out[++index] = (float)(x2 * x + y2 * y + z2 * z + w2);
	}
	
	public final void transformAffine(DoubleArrayList in, int inIndex, float[] out, int outIndex) {
		double x = in.getD(inIndex), y = in.getD(++inIndex), z = in.getD(++inIndex);
		out[outIndex]   = (float)(x0 * x + y0 * y + z0 * z + w0);
		out[++outIndex] = (float)(x1 * x + y1 * y + z1 * z + w1);
		out[++outIndex] = (float)(x2 * x + y2 * y + z2 * z + w2);
	}
	
	public final void transform(double x, double y, double z, double w, float out[], int index){
		out[index]   = (float)(x0 * x + y0 * y + z0 * z + w0 * w);
		out[++index] = (float)(x1 * x + y1 * y + z1 * z + w1 * w);
		out[++index] = (float)(x2 * x + y2 * y + z2 * z + w2 * w);
	}
	
	@Override
	public final String toString(){
		StringBuilder strB = new StringBuilder(24);
		strB.append(x0).append(' ').append(y0).append(' ').append(z0).append(' ').append(w0).append('\n');
		strB.append(x1).append(' ').append(y1).append(' ').append(z1).append(' ').append(w1).append('\n');
		strB.append(x2).append(' ').append(y2).append(' ').append(z2).append(' ').append(w2).append('\n');
		strB.append(x3).append(' ').append(y3).append(' ').append(z3).append(' ').append(w3).append('\n');
		return strB.toString();
	}
	
	public final void setElem(int i, double value)
	{
		switch(i) {
		case 0: x0 = value;return;case 1: x1 = value;return;case 2: x2 = value;return;case 3: x3=value;return;
		case 4: y0 = value;return;case 5: y1 = value;return;case 6: y2 = value;return;case 7: y3=value;return;
		case 8: z0 = value;return;case 9: z1 = value;return;case 10:z2 = value;return;case 11:z3=value;return;
		case 12:w0 = value;return;case 13:w1 = value;return;case 14:w2 = value;return;case 15:w3=value;return;
		}
		throw new ArrayIndexOutOfBoundsException(i);
	}
	
	public final void set(int x, int y, double value){
		switch(x){
			case 0:switch(y){case 0:x0 = value;return;case 1:x1 = value;return;case 2:x2 = value;return;case 3:x3 = value;return;default: throw new ArrayIndexOutOfBoundsException(y);}
			case 1:switch(y){case 0:y0 = value;return;case 1:y1 = value;return;case 2:y2 = value;return;case 3:y3 = value;return;default: throw new ArrayIndexOutOfBoundsException(y);}
			case 2:switch(y){case 0:z0 = value;return;case 1:z1 = value;return;case 2:z2 = value;return;case 3:z3 = value;return;default: throw new ArrayIndexOutOfBoundsException(y);}
			case 3:switch(y){case 0:w0 = value;return;case 1:w1 = value;return;case 2:w2 = value;return;case 3:w3 = value;return;default: throw new ArrayIndexOutOfBoundsException(y);}
		}
		throw new ArrayIndexOutOfBoundsException(x);
	}

	public final double get(int x, int y) {
		switch(x){
			case 0:switch(y){case 0:return x0;case 1:return x1;case 2:return x2;case 3:return x3;default: throw new ArrayIndexOutOfBoundsException(y);}
			case 1:switch(y){case 0:return y0;case 1:return y1;case 2:return y2;case 3:return y3;default: throw new ArrayIndexOutOfBoundsException(y);}
			case 2:switch(y){case 0:return z0;case 1:return z1;case 2:return z2;case 3:return z3;default: throw new ArrayIndexOutOfBoundsException(y);}
			case 3:switch(y){case 0:return w0;case 1:return w1;case 2:return w2;case 3:return w3;default: throw new ArrayIndexOutOfBoundsException(y);}
		}
		throw new ArrayIndexOutOfBoundsException(x);
	}

	public final void set(Matrix4d o) {
		this.x0 = o.x0;this.x1 = o.x1;this.x2 = o.x2;this.x3 = o.x3;
		this.y0 = o.y0;this.y1 = o.y1;this.y2 = o.y2;this.y3 = o.y3;
		this.z0 = o.z0;this.z1 = o.z1;this.z2 = o.z2;this.z3 = o.z3;
		this.w0 = o.w0;this.w1 = o.w1;this.w2 = o.w2;this.w3 = o.w3;
	}

	@Override
	public final int size() {
		return 16;
	}
	
	public final int rows()
	{
		return 4;
	}
	
	public final int cols()
	{
		return 4;
	}

	@Override
	public final double getD(int index) {
		switch(index)
		{
		case 0: return x0; case 1: return x1; case 2: return x2; case 3: return x3;
		case 4: return y0; case 5: return y1; case 6: return y2; case 7: return y3;
		case 8: return z0; case 9: return z1; case 10:return z2; case 11:return z3;
		case 12:return w0; case 13:return w1; case 14:return w2; case 15:return w3;
		default:throw new ArrayIndexOutOfBoundsException(index);
		}
	}
	
	public final void dotl(Matrix4d lhs)
	{
		double x = lhs.x0 * x0 + lhs.x1 * y0 + lhs.x2 * z0 + lhs.x3 * w0;
		double y = lhs.y0 * x0 + lhs.y1 * y0 + lhs.y2 * z0 + lhs.y3 * w0;
		double z = lhs.z0 * x0 + lhs.z1 * y0 + lhs.z2 * z0 + lhs.z3 * w0;
		double w = lhs.w0 * x0 + lhs.w1 * y0 + lhs.w2 * z0 + lhs.w3 * w0;
                            x0 = x;       y0 = y;       z0 = z;       w0 = w;
	    	   x = lhs.x0 * x1 + lhs.x1 * y1 + lhs.x2 * z1 + lhs.x3 * w1;
		       y = lhs.y0 * x1 + lhs.y1 * y1 + lhs.y2 * z1 + lhs.y3 * w1;
		       z = lhs.z0 * x1 + lhs.z1 * y1 + lhs.z2 * z1 + lhs.z3 * w1;
		       w = lhs.w0 * x1 + lhs.w1 * y1 + lhs.w2 * z1 + lhs.w3 * w1;
                            x1 = x;       y1 = y;       z1 = z;       w1 = w;
		       x = lhs.x0 * x2 + lhs.x1 * y2 + lhs.x2 * z2 + lhs.x3 * w2;
		       y = lhs.y0 * x2 + lhs.y1 * y2 + lhs.y2 * z2 + lhs.y3 * w2;
		       z = lhs.z0 * x2 + lhs.z1 * y2 + lhs.z2 * z2 + lhs.z3 * w2;
		       w = lhs.w0 * x2 + lhs.w1 * y2 + lhs.w2 * z2 + lhs.w3 * w2;
                            x2 = x;       y2 = y;       z2 = z;       w2 = w;
		       x = lhs.x0 * x3 + lhs.x1 * y3 + lhs.x2 * z3 + lhs.x3 * w3;
		       y = lhs.y0 * x3 + lhs.y1 * y3 + lhs.y2 * z3 + lhs.y3 * w3;
		       z = lhs.z0 * x3 + lhs.z1 * y3 + lhs.z2 * z3 + lhs.z3 * w3;
		       w = lhs.w0 * x3 + lhs.w1 * y3 + lhs.w2 * z3 + lhs.w3 * w3;
                            x3 = x;       y3 = y;       z3 = z;       w3 = w;
	}
	
	public final void dotr(Matrix4d rhs)
	{
		double v0 = x0 * rhs.x0 + x1 * rhs.y0 + x2 * rhs.z0 + x3 * rhs.w0;
		double v1 = x0 * rhs.x1 + x1 * rhs.y1 + x2 * rhs.z1 + x3 * rhs.w1;
		double v2 = x0 * rhs.x2 + x1 * rhs.y2 + x2 * rhs.z2 + x3 * rhs.w2;
		double v3 = x0 * rhs.x3 + x1 * rhs.y3 + x2 * rhs.z3 + x3 * rhs.w3;
                    x0 = v0;      x1 = v1;      x2 = v2;      x3 = v3;
               v0 = y0 * rhs.x0 + y1 * rhs.y0 + y2 * rhs.z0 + y3 * rhs.w0;
		       v1 = y0 * rhs.x1 + y1 * rhs.y1 + y2 * rhs.z1 + y3 * rhs.w1;
		       v2 = y0 * rhs.x2 + y1 * rhs.y2 + y2 * rhs.z2 + y3 * rhs.w2;
		       v3 = y0 * rhs.x3 + y1 * rhs.y3 + y2 * rhs.z3 + y3 * rhs.w3;
                    y0 = v0;      y1 = v1;      y2 = v2;      y3 = v3;
               v0 = z0 * rhs.x0 + z1 * rhs.y0 + z2 * rhs.z0 + z3 * rhs.w0;
			   v1 = z0 * rhs.x1 + z1 * rhs.y1 + z2 * rhs.z1 + z3 * rhs.w1;
		       v2 = z0 * rhs.x2 + z1 * rhs.y2 + z2 * rhs.z2 + z3 * rhs.w2;
		       v3 = z0 * rhs.x3 + z1 * rhs.y3 + z2 * rhs.z3 + z3 * rhs.w3;
                    z0 = v0;      z1 = v1;      z2 = v2;      z3 = v3;
		       v0 = w0 * rhs.x0 + w1 * rhs.y0 + w2 * rhs.z0 + w3 * rhs.w0;
		       v1 = w0 * rhs.x1 + w1 * rhs.y1 + w2 * rhs.z1 + w3 * rhs.w1;
		       v2 = w0 * rhs.x2 + w1 * rhs.y2 + w2 * rhs.z2 + w3 * rhs.w2;
		       v3 = w0 * rhs.x3 + w1 * rhs.y3 + w2 * rhs.z3 + w3 * rhs.w3;
                    w0 = v0;      w1 = v1;      w2 = v2;      w3 = v3;
	}

	public final void dot(Matrix4d lhs, Matrix4d rhs) {
		if (lhs == this)
		{
			dotl(rhs);
			return;
		}
		if (rhs == this)
		{
			dotr(lhs);
			return;
		}
		x0 = lhs.x0 * rhs.x0 + lhs.x1 * rhs.y0 + lhs.x2 * rhs.z0 + lhs.x3 * rhs.w0;
		x1 = lhs.x0 * rhs.x1 + lhs.x1 * rhs.y1 + lhs.x2 * rhs.z1 + lhs.x3 * rhs.w1;
		x2 = lhs.x0 * rhs.x2 + lhs.x1 * rhs.y2 + lhs.x2 * rhs.z2 + lhs.x3 * rhs.w2;
		x3 = lhs.x0 * rhs.x3 + lhs.x1 * rhs.y3 + lhs.x2 * rhs.z3 + lhs.x3 * rhs.w3;
		y0 = lhs.y0 * rhs.x0 + lhs.y1 * rhs.y0 + lhs.y2 * rhs.z0 + lhs.y3 * rhs.w0;
		y1 = lhs.y0 * rhs.x1 + lhs.y1 * rhs.y1 + lhs.y2 * rhs.z1 + lhs.y3 * rhs.w1;
		y2 = lhs.y0 * rhs.x2 + lhs.y1 * rhs.y2 + lhs.y2 * rhs.z2 + lhs.y3 * rhs.w2;
		y3 = lhs.y0 * rhs.x3 + lhs.y1 * rhs.y3 + lhs.y2 * rhs.z3 + lhs.y3 * rhs.w3;
		x0 = lhs.z0 * rhs.x0 + lhs.z1 * rhs.y0 + lhs.z2 * rhs.z0 + lhs.z3 * rhs.w0;
		x1 = lhs.z0 * rhs.x1 + lhs.z1 * rhs.y1 + lhs.z2 * rhs.z1 + lhs.z3 * rhs.w1;
		x2 = lhs.z0 * rhs.x2 + lhs.z1 * rhs.y2 + lhs.z2 * rhs.z2 + lhs.z3 * rhs.w2;
		x3 = lhs.z0 * rhs.x3 + lhs.z1 * rhs.y3 + lhs.z2 * rhs.z3 + lhs.z3 * rhs.w3;
		x0 = lhs.w0 * rhs.x0 + lhs.w1 * rhs.y0 + lhs.w2 * rhs.z0 + lhs.w3 * rhs.w0;
		x1 = lhs.w0 * rhs.x1 + lhs.w1 * rhs.y1 + lhs.w2 * rhs.z1 + lhs.w3 * rhs.w1;
		x2 = lhs.w0 * rhs.x2 + lhs.w1 * rhs.y2 + lhs.w2 * rhs.z2 + lhs.w3 * rhs.w2;
		x3 = lhs.w0 * rhs.x3 + lhs.w1 * rhs.y3 + lhs.w2 * rhs.z3 + lhs.w3 * rhs.w3;
	}

	@Override
	public void set(Matrixd o) {
		if (o instanceof Matrix4d)
		{
			set((Matrix4d) o);
		}
		else
		{
			int cols = Math.min(o.cols(), cols()), rows = Math.min(o.rows(), rows());
			for (int i = 0; i < rows; ++i)
			{
				for (int j = 0; j < cols; ++j)
				{
					set(i, j, o.get(i, j));
				}
			}
		}
	}

	public final void set(double x0, double x1, double x2, double y0, double y1, double y2, double z0, double z1, double z2){
		this.x0 = x0;this.x1 = x1;this.x2 = x2;
		this.y0 = y0;this.y1 = y1;this.y2 = y2;
		this.z0 = z0;this.z1 = z1;this.z2 = z2;
	}

}
