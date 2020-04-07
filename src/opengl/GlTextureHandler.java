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

import util.data.IntegerArrayList;


/** 
* @author  Paul Stahr
* @version 04.02.2012
*/
public abstract class GlTextureHandler
{
	public class GlTexture{
	    private int id;
	    private int width, height;
	    public final int type;
	    public final int target;
	    private float aspect;
	    
	    public GlTexture(int type, int target)
	    {
	    	this.id = generate();
	    	this.type = type;
	    	this.target = target;
	    }
	    
	    public final void setBounds(int width, int height)
	    {
	    	this.width = width;
	    	this.height = height;
	    	this.aspect = ((float)height) / width;
	    }
	    
	    public final int getWidth()
	    {
	    	return width;
	    }
	    
	    public final int getHeight()
	    {
	    	return height;
	    }
	    
	    public final float getAspect()
	    {
	    	return aspect;
	    }
	    
	    public final void bind()
	    {
	    	if (id == -1)
	    	{
	    		throw new RuntimeException("Texture already destroyed");
	    	}
	    	GlTextureHandler.this.bind(type, id);
	    }
	    
		@Override
		public final void finalize(){
	        if (id != -1){
	        	synchronized(destroyedTextureIds)
	        	{
	        		destroyedTextureIds.add(id);
		            id = -1;
	        	}
	        }
	    }
	}
    private final IntegerArrayList destroyedTextureIds = new IntegerArrayList();
    
	public final GlTexture getTexture(int type, int target)
	{
		return new GlTexture(type, target);
	}
	
    protected abstract void bind(int type, int id);
    
    protected abstract void remove(int id);

    public void poll()
    {
    	synchronized(destroyedTextureIds)
    	{
        	for (int i = 0; i < destroyedTextureIds.size(); ++i)
        	{
        		remove(destroyedTextureIds.getI(i));    		
        	}
        	destroyedTextureIds.clear();
    	}
    }
    
	protected abstract int generate();
}
