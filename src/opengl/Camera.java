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

import java.awt.Rectangle;

import geometry.Rotation3;
import geometry.Vector3f;
/** 
* @author  Paul Stahr
* @version 04.02.2012
*/

public final class Camera
{
	public static final byte STEREOSCOPIE_LEFT = -1, STEREOSCOPIE_MIDDLE = 0, STEREOSCOPIE_RIGHT = 1;
    public Vector3f position;
    public Rotation3 rotation;
    private float angle = 45f;
    private double aspect;
    private int height=-1, width=-1;
	public byte stereoPosition = STEREOSCOPIE_MIDDLE;
	
	public Camera() {
		position = new Vector3f();
		rotation = new Rotation3();
    }
	
	public Camera(Vector3f position, Rotation3 rotation) {
		this.position = position;
		this.rotation = rotation;
    }	
	
	public void setAngle(float angle)
	{
		this.angle = angle;
	}
	
    public final void setSize(int x, int y){
        width = x;
        height = y;
        aspect = (double)width / height;
    }
    
    public final float getAngle(){
    	return angle;
    }

    public final double getAspect(){
    	return aspect;
    }
    
    public final void setSize(Rectangle r){
        setSize(r.width, r.height);
    }

    public final int getWidth(){
        return width;
    }

    public final int getHeight(){
        return height;
    }
}
