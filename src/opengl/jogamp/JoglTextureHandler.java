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
package opengl.jogamp;


import java.nio.IntBuffer;

import com.jogamp.opengl.GL2;

import opengl.GlTextureHandler;
import util.Buffers;

public class JoglTextureHandler extends GlTextureHandler{
	private final IntBuffer iBuf = Buffers.createIntBuffer(1);
	private final GL2 gl;

	public JoglTextureHandler(GL2 gl)
	{
		this.gl = gl;
	}
	@Override
	protected final int generate()
	{
		 gl.glGenTextures(1, iBuf);
		 return iBuf.get(0);
	}
	
	@Override
	protected final void bind(int type, int id) {
		gl.glBindTexture(type, id);
	}

	@Override
	protected final void remove(int id)
	{
		iBuf.put(0, id);
		gl.glDeleteTextures(1, iBuf);
	}
}
