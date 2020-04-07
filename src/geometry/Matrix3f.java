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

public class Matrix3f {
	public float v00, v10, v20, v01, v11, v21, v02, v12, v22;
	
	public Matrix3f(float x0, float x1, float x2, float y0, float y1, float y2, float z0, float z1, float z2){
		this.v00 = x0;
		this.v10 = x1;
		this.v20 = x2;
		this.v01 = y0;
		this.v11 = y1;
		this.v21 = y2;
		this.v02 = z0;
		this.v12 = z1;
		this.v22 = z2;
	}
	
	public Matrix3f(){
		identity();
	}
	
	public final void identity(){
		v00 = v11 = v22 = 1f;
		v10 = v20 = v01 = v21 = v02 = v12 = 0f;
	}
	
	public final void diagonal(float diagonal){
		v00 = v11 = v22 = diagonal;
		v10 = v20 = v01 = v21 = v02 = v12 = 0f;
	}
	
    /**
     * Dreht die Matrix um eine Drehung
     */
    public final void rotateXYZEuler(Rotation3 rotation){
    	rotateRadiansX(rotation.getXRadians());
    	rotateRadiansY(rotation.getYRadians());
    	rotateRadiansZ(rotation.getZRadians());
    }
    
	public void rotateZYXEuler(Rotation3 rotation) {
    	rotateRadiansZ(rotation.getZRadians());
    	rotateRadiansY(rotation.getYRadians());
    	rotateRadiansX(rotation.getXRadians());
	}
	
    /**
     * Dreht die Matrix um eine Drehung
     */
    public final void rotateReverseXYZEuler(Rotation3 rotation){
    	rotateRadiansZ(-rotation.getZRadians());
    	rotateRadiansY(-rotation.getYRadians());
    	rotateRadiansX(-rotation.getXRadians());
    }
    
    public final void rotateRadiansX(double x){
        final float sin = (float)Math.sin(x), cos = (float)Math.cos(x);
        float tmp;
        tmp = v20 * sin + v10 * cos;
        v20 = v20 * cos - v10 * sin;
        v10 = tmp;
        tmp = v21 * sin + v11 * cos;
        v21 = v21 * cos - v11 * sin;
        v11 = tmp;
        tmp = v22 * sin + v12 * cos;
        v22 = v22 * cos - v12 * sin;
        v12 = tmp;
    }
	
    public final void rotateRadiansY(double x){
        final float sin = (float)Math.sin(x), cos = (float)Math.cos(x);
        float tmp;
        tmp = v20 * sin + v00 * cos;
        v20 = v20 * cos - v00 * sin;
        v00 = tmp;
        tmp = v21 * sin + v01 * cos;
        v21 = v21 * cos - v01 * sin;
        v01 = tmp;
        tmp = v22 * sin + v02 * cos;
        v22 = v22 * cos - v02 * sin;
        v02 = tmp;
    }
	
    public final void rotateRadiansZ(double x){
    	final float sin = (float)Math.sin(x), cos = (float)Math.cos(x);
        float tmp;
        tmp = v10 * sin + v00 * cos;
        v10 = v10 * cos - v00 * sin;
        v00 = tmp;
        tmp = v11 * sin + v01 * cos;
        v11 = v11 * cos - v01 * sin;
        v01 = tmp;
        tmp = v12 * sin + v02 * cos;
        v12 = v12 * cos - v02 * sin;
        v02 = tmp;
    }
		
	public void transform(Vector3f v){
		final float x = v.x, y = v.y, z = v.z;
		v.x = v00 * x + v01 * y + v02 * z;
		v.y = v10 * x + v11 * y + v12 * z;
		v.z = v20 * x + v21 * y + v22 * z;
	}
	
	public void fillWithRow(Vector3f v, int line){
		switch(line){
			case 0:v.x = v00;v.y = v01;v.z = v02;break;
			case 1:v.x = v10;v.y = v11;v.z = v12;break;
			case 2:v.x = v20;v.y = v21;v.z = v22;break;
			default:throw new ArrayIndexOutOfBoundsException(line);
		}
	}
	
	public void fillWithColumn(Vector3f v, int line){
		switch(line){
			case 0:v.x = v00;v.y = v10;v.z = v20;break;
			case 1:v.x = v01;v.y = v11;v.z = v21;break;
			case 2:v.x = v02;v.y = v12;v.z = v22;break;
			default:throw new ArrayIndexOutOfBoundsException(line);
		}
	}
	
	public void fillWithColumns(Vector2f v[])
	{
		v[0].x = v00;v[0].y = v10;
		v[1].x = v01;v[1].y = v11;
		v[2].x = v02;v[2].y = v12;		
	}
	
	public void fillWithColumns2d(float v[])
	{
		v[0] = v00;v[1] = v10;
		v[2] = v01;v[3] = v11;
		v[4] = v02;v[5] = v12;		
	}
	
	public void fillWithColumn(Vector2f v, int line){
		switch(line){
			case 0:v.x = v00;v.y = v10;break;
			case 1:v.x = v01;v.y = v11;break;
			case 2:v.x = v02;v.y = v12;break;
			default:throw new ArrayIndexOutOfBoundsException(line);
		}
	}
	
	public final float getNewX(float x, float y, float z){
		return v00 * x + v01 * y + v02 * z;
	}

	public final float getNewY(float x, float y, float z){
		return v10 * x + v11 * y + v12 * z;
	}

	public final float getNewZ(float x, float y, float z){
		return v20 * x + v21 * y + v22 * z;
	}
	
	public final void set(int x, int y, float value){
		switch(x*3 + y){
			case 0:v00=value;break;
			case 1:v01=value;break;
			case 2:v02=value;break;
			case 3:v10=value;break;
			case 4:v11=value;break;
			case 5:v12=value;break;
			case 6:v20=value;break;
			case 7:v21=value;break;
			case 8:v22=value;break;
		}
	}
}
