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

public abstract class GlBufferObjectHandler
{
	protected abstract int genBuffer();
	protected abstract void bindBuffer(int arget, int id);
	private final IntegerArrayList removedBuffers = new IntegerArrayList();
	protected abstract void deleteBuffer(int id);
	
	public GlBufferObject createBufferObject() {
		return new GlBufferObject(genBuffer());
	}
	
	public final void poll()
	{
		synchronized (removedBuffers) {
			for (int i = 0; i < removedBuffers.size(); ++i){
				int id = removedBuffers.getI(i);
				if (id != -1)
				{
					deleteBuffer(id);
				}
			}
			removedBuffers.clear();
		}	
	}
	
	public class GlBufferObject {
		private int id;
	
		private GlBufferObject(int id)
		{
			this.id = id;
		}
		
		public final void bind(int target)
		{
			if (id == -1)
			{
				throw new RuntimeException("Bufferobject already destroyed");
			}
			bindBuffer(target, id);
		}
		
		public void destroy()
		{
			if (id != -1)
			{
				synchronized (removedBuffers) 
				{
					removedBuffers.add(id);
				}
				id = -1;
			}
		}
		
		public void finalize()
		{
			if (id != -1)
			{
				synchronized(removedBuffers)
				{
					removedBuffers.add(id);
				}
				id = -1;
			}
		}
	}
}
