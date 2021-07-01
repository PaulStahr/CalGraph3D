/*******************************************************************************
 * MIT License
 *
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
 * FITNESS FOR A PARTICULAR PURbufferPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 ******************************************************************************/
package opengl;

import java.awt.Color;
import java.nio.FloatBuffer;

import util.Buffers;
/**
* @author  Paul Stahr
* @version 04.02.2012
*/
public final class GlColor
{
    private static final float MULT_TO_FCOL = 1f/255f;
    public static final FloatBuffer COLOR_WHITE;
    public static final FloatBuffer COLOR_GREY;
    public static final FloatBuffer COLOR_BLACK;
    public static final FloatBuffer COLOR_RED;
    public static final FloatBuffer COLOR_YELLOW;
    public static final FloatBuffer COLOR_GREEN;
    public static final FloatBuffer COLOR_BLUE;

    static
    {
    	FloatBuffer fb[] = Buffers.createFloatBuffer(4, 7);
    	COLOR_WHITE =  Buffers.fillFloatBuffer(fb[0], 1f,1f,1f,1f).asReadOnlyBuffer();
    	COLOR_GREY  =  Buffers.fillFloatBuffer(fb[1], 0.5f, 0.5f, 0.5f, 1.0f).asReadOnlyBuffer();
        COLOR_BLACK =  Buffers.fillFloatBuffer(fb[2], 0.0f, 0.0f, 0.0f, 1.0f).asReadOnlyBuffer();
        COLOR_RED =    Buffers.fillFloatBuffer(fb[3], 1.0f, 0.0f, 0.0f, 1.0f).asReadOnlyBuffer();
        COLOR_YELLOW = Buffers.fillFloatBuffer(fb[4], 1.0f, 1.0f, 0.0f, 1.0f).asReadOnlyBuffer();
        COLOR_GREEN =  Buffers.fillFloatBuffer(fb[5], 0.0f, 1.0f, 0.0f, 1.0f).asReadOnlyBuffer();
        COLOR_BLUE =   Buffers.fillFloatBuffer(fb[6], 0.0f, 0.0f, 1.0f, 1.0f).asReadOnlyBuffer();
    }

    private GlColor(){}

    public static final FloatBuffer getReadOnlyColor(float ...data)
    {
    	return Buffers.createFloatBuffer(data).asReadOnlyBuffer();
    }

    public static final void fill(float f[], Color col){
    	final int c = col.getRGB();
    	if (f.length >= 3){
    		f[0] = ((c>>16)&0xFF)*MULT_TO_FCOL;
        	f[1] = ((c>>8)&0xFF)*MULT_TO_FCOL;
        	f[2] = (c&0xFF)*MULT_TO_FCOL;
    	}
    	if (f.length >= 4)
    		f[3] = ((c>>24)&0xFF)*MULT_TO_FCOL;
    }
}
