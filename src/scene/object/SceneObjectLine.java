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

/** 
* @author  Paul Stahr
* @version 04.02.2012
*/
public class SceneObjectLine extends SceneObject
{
	private float vertices[] = UniqueObjects.EMPTY_FLOAT_ARRAY;
	private float vertexColor[] = null;
    int size = 0, usedMemory = 0;
    int renderRangeBegin = 0;
    int renderRangeEnd = 0;
    
    public SceneObjectLine() {
	}
    
    public void setVertexColorActivated(boolean active)
    {
    	if (active)
    	{
    		if (vertexColor == null || vertexColor.length != size * 4)
    		{
    			vertexColor = new float[size * 4];
    		}
    	}
    	else
    	{
    		vertexColor = null;
    	}
	}
    
    public float[] getVertices(){
    	return vertices;
    }
    
    public float[] getVertexColor()
    {
    	return vertexColor;
    }
    
    public final void setRenderRange(int begin, int end)
    {
    	this.renderRangeBegin = begin;
    	this.renderRangeEnd = end;
    }
    
    public final void setData(float vertices[], float color[])
    {
    	if (color != null && vertices.length * 4 != color.length * 3)
    	{
    		throw new ArrayIndexOutOfBoundsException();
    	}
    	synchronized(this)
    	{
    		this.vertices = vertices;
    		this.vertexColor = color;
    		size = vertices.length / 3;
    	}
    }
    
    public final boolean setSize(int size){
    	if (size > 100000)
		{
			System.out.println(new StringBuilder().append(size).append(' ').append(this).toString());
		}
    	if (size == this.size)
    		return true;
    	synchronized (this) {
    		if (size < 0){
                vertices = null;
                this.size = this.usedMemory = 0;
                return false;
        	}
    		if (vertexColor != null)
    		{
    			vertexColor = new float[size * 4];
    		}
        	vertices = new float[size * 3];
    		update(UpdateKind.DIMENSION);
    		this.usedMemory = (this.size = size)*12;			
    		setRenderRange(0, size);
		}
		return true;
    }

    public void setVertex (int index, Vector3f vector){
    	vector.write(vertices, index * 3);
    }

    public void setVertex (int index, float xP, float yP, float zP){
    	vertices[index * 3] = xP;
    	vertices[index * 3 + 1] = yP;
    	vertices[index * 3 + 2] = zP;
    }

    
	@Override
	public final int getVertexCount(){
		/*if (size > 100000)
		{
			System.out.println(size + ' ' + this + ' ' + getId());
		}*/
        return size;
    }

    
	@Override
	public final int getFaceCount(){
        return 0;
    }

    
	@Override
	public final int getUsedMemory(){
        return usedMemory;
    }
    
    
	@Override
	public boolean hasNormals(){
    	return false;
    }

	
	@Override
	public final boolean isCachable() {
		return false;
	}

	public int size() {
		return vertices.length / 3;
	}

	@Override
	public boolean drawTypeAllowed(byte dt) {
		switch (dt)
		{
		case DrawType.DOTS:
		case DrawType.LINES:
		case DrawType.LINE_STRICLES:
			return true;
		case DrawType.SOLID:
		case DrawType.SOLID_SMOOTH:
			return false;
		default:
			throw new IllegalArgumentException();
		}
	}
}
