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


import util.Buffers;

import java.awt.Color;
import java.nio.FloatBuffer;
/** 
* @author  Paul Stahr
* @version 04.02.2012
*/
public final class Light
{
	public static class Type{public static final byte AMBIENT = 0, DIFFUSE = 1, SPECULAR = 2, EMISSION = 3, POSITION = 4;}
	public static class Attenuation{public static final byte CONSTANT = 0, LINEAR = 1;}
    private static final float MULT_TO_FLOAT = 1f/255f;

    public int attenuation;
    public float fallOff;
    private final FloatBuffer ambient;// = Buffers.createFloatBuffer(0.0f, 0.0f, 0.0f, 1.0f);
    private final FloatBuffer diffuse;// = Buffers.createFloatBuffer(0.0f, 0.0f, 0.0f, 1.0f);
    private final FloatBuffer specular;// = Buffers.createFloatBuffer(0.0f, 0.0f, 0.0f, 1.0f);
    private final FloatBuffer position;// = Buffers.createFloatBuffer(0.0f, 0.0f, 0.0f, 1.0f);
    public final FloatBuffer ambientReadOnly;// = ambient.asReadOnlyBuffer();
    public final FloatBuffer diffuseReadOnly;// = diffuse.asReadOnlyBuffer();
    public final FloatBuffer specularReadOnly;// = specular.asReadOnlyBuffer();
    public final FloatBuffer positionReadOnly;// = position.asReadOnlyBuffer();
    
    public boolean enabled = false;
    
    public Light(){
    	FloatBuffer fb[] = Buffers.createFloatBuffer(4, 4);
    	ambientReadOnly = (ambient = fb[0]).asReadOnlyBuffer();
    	diffuseReadOnly = (diffuse = fb[1]).asReadOnlyBuffer();
    	specularReadOnly = (specular = fb[2]).asReadOnlyBuffer();
    	positionReadOnly = (position = fb[3]).asReadOnlyBuffer();
    }
    
    public static Light[] create(int count)
    {
    	Light res[] = new Light[count];
    	FloatBuffer fb[] = Buffers.createFloatBuffer(4,4 * count);
    	for (int i = 0; i < count; ++i)
    	{
    		int index = i * 4;
    		res[i] = new Light(fb[index], fb[index + 1], fb[index + 2], fb[index + 3]);
    	}
    	return res;
    }
    
    private Light(FloatBuffer ambient, FloatBuffer diffuse, FloatBuffer specular, FloatBuffer emission)
    {
    	ambientReadOnly = (this.ambient = ambient).asReadOnlyBuffer();
    	diffuseReadOnly = (this.diffuse = diffuse).asReadOnlyBuffer();
    	specularReadOnly = (this.specular = specular).asReadOnlyBuffer();
    	positionReadOnly = (this.position = emission).asReadOnlyBuffer();
    }
           
    public final void set(float r, float g, float b, float a, byte type){
    	final FloatBuffer fb;
        switch (type){
            case Type.AMBIENT: 	fb = ambient; break;
            case Type.DIFFUSE: 	fb = diffuse; break;
            case Type.SPECULAR: fb = specular; break;
            case Type.POSITION: fb = position; break;
            default:		throw new IllegalArgumentException();
        }
        fb.put(0,r).put(1,g).put(2,b).put(3,a);        
    }
    
    public final void set(Color color, byte type){
    	int rgb = color.getRGB();
        set(((rgb>>16)&0xFF)*MULT_TO_FLOAT, ((rgb>>8)&0xFF)*MULT_TO_FLOAT, (rgb&0xFF)*MULT_TO_FLOAT, ((rgb>>24)&0xFF)*MULT_TO_FLOAT, type);
    }
    
    public final Color get (byte type){
    	final FloatBuffer fb;
        switch (type){
            case Type.AMBIENT: 	fb = ambient;break;
            case Type.DIFFUSE: 	fb = diffuse;break;
            case Type.SPECULAR: fb = specular;break;
            case Type.POSITION: fb = position;break;
            default:		throw new IllegalArgumentException();
        }
        return new Color(fb.get(0), fb.get(1), fb.get(2), fb.get(3));
    }
}
