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

import java.nio.*;
import org.lwjgl.opengl.GL11;

import util.Buffers;
/** 
* @author  Paul Stahr
* @version 04.02.2012
*/

public abstract class GraphicCardInformations
{
    private int maxLights = -1;
    private int maxTextureSize = -1;

    private GraphicCardInformations(){}
    
    public void init(){
        final IntBuffer tmp = Buffers.createIntBuffer(16);
        GL11.glGetInteger(GL11.GL_MAX_LIGHTS, tmp);
        maxLights = tmp.get(0);
        GL11.glGetInteger(GL11.GL_MAX_TEXTURE_SIZE, tmp);
        maxTextureSize = tmp.get(0);
    }

    public final int getMaxLights(){
    	return maxLights;
    }

    public final int getMaxTextureSize(){
    	return maxTextureSize;
    }
}
