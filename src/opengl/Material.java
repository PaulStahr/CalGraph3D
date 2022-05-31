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

import java.awt.Color;
import java.nio.FloatBuffer;

import opengl.GlTextureHandler.GlTexture;
import opengl.Light.Type;
import util.Buffers;
/**
* @author  Paul Stahr
* @version 04.02.2012
*/

public final class Material
{
    private static final float MULT_TO_FLOAT = 1f/255f;

    public String Name;
    public float Ns; //defines the hard of specular
    private final FloatBuffer ambient;// = Buffers.createFloatBuffer(0.0f, 0.0f, 0.0f, 1.0f);
    private final FloatBuffer diffuse;// = Buffers.createFloatBuffer(0.0f, 0.0f, 0.0f, 1.0f);
    private final FloatBuffer specular;// = Buffers.createFloatBuffer(0.0f, 0.0f, 0.0f, 1.0f);
    private final FloatBuffer emission;// = Buffers.createFloatBuffer(0.0f, 0.0f, 0.0f, 1.0f);
    public final FloatBuffer ambientReadOnly;// = ambient.asReadOnlyBuffer();
    public final FloatBuffer diffuseReadOnly;// = diffuse.asReadOnlyBuffer();
    public final FloatBuffer specularReadOnly;// = specular.asReadOnlyBuffer();
    public final FloatBuffer emissionReadOnly;// = emission.asReadOnlyBuffer();
    public float Ni;
    public int illum;
    public GlTexture texture;

    public Material()
    {
    	FloatBuffer fb[] = Buffers.createFloatBuffer(4, 4);
    	ambientReadOnly = (ambient = fb[0]).asReadOnlyBuffer();
    	diffuseReadOnly = (diffuse = fb[1]).asReadOnlyBuffer();
    	specularReadOnly = (specular = fb[2]).asReadOnlyBuffer();
    	emissionReadOnly = (emission = fb[3]).asReadOnlyBuffer();
    }

    public static Material[] create(int count)
    {
    	Material res[] = new Material[count];
    	FloatBuffer fb[] = Buffers.createFloatBuffer(4,4 * count);
    	for (int i = 0; i < count; ++i)
    	{
    		int index = i * 4;
    		res[i] = new Material(fb[index], fb[index + 1], fb[index + 2], fb[index + 3]);
    	}
    	return res;
    }

    private Material(FloatBuffer ambient, FloatBuffer diffuse, FloatBuffer specular, FloatBuffer emission)
    {
    	ambientReadOnly = (this.ambient = ambient).asReadOnlyBuffer();
    	diffuseReadOnly = (this.diffuse = diffuse).asReadOnlyBuffer();
    	specularReadOnly = (this.specular = specular).asReadOnlyBuffer();
    	emissionReadOnly = (this.emission = emission).asReadOnlyBuffer();
    }

    public final void set(boolean ambient, boolean diffuse, boolean specular, boolean emission, final float r, final float g, final float b, final float a)
    {
    	if (ambient)   {this.ambient.put(0,r).put(1,g).put(2,b).put(3,a);}
    	if (diffuse)   {this.diffuse.put(0,r).put(1,g).put(2,b).put(3,a);}
    	if (specular)  {this.specular.put(0,r).put(1,g).put(2,b).put(3,a);}
    	if (emission)  {this.emission.put(0,r).put(1,g).put(2,b).put(3,a);}
    }

    public final void set(byte type, final float r, final float g, final float b, final float a){
        FloatBuffer buf;
        switch (type){
            case Type.AMBIENT: 	buf = ambient; break;
            case Type.DIFFUSE: 	buf = diffuse; break;
            case Type.SPECULAR:	buf = specular; break;
            case Type.EMISSION: buf = emission; break;
            default:		throw new IllegalArgumentException();
        }
        buf.put(0,r).put(1,g).put(2,b).put(3,a);
    }

    public final void set(byte type, final Color color){
    	int rgb = color.getRGB();
        set(type, ((rgb>>16)&0xFF)*MULT_TO_FLOAT, ((rgb>>8)&0xFF)*MULT_TO_FLOAT, (rgb&0xFF)*MULT_TO_FLOAT, ((rgb>>24)&0xFF)*MULT_TO_FLOAT);
    }

    public final Color get (byte type){
    	FloatBuffer fb;
        switch (type){
            case Type.AMBIENT: 	fb = ambient;break;
            case Type.DIFFUSE: 	fb = diffuse;break;
            case Type.SPECULAR: fb = specular;break;
            case Type.EMISSION: fb = emission;break;
            default: throw new IllegalArgumentException();
        }
        return new Color(fb.get(0), fb.get(1), fb.get(2), fb.get(3));
    }

    public final void set (GlTexture texture){
    	this.texture = texture;
    }

	public final void set(boolean b, boolean c, boolean d, boolean e, Color color) {
    	int rgb = color.getRGB();
        set(b, c, d, e, ((rgb>>16)&0xFF)*MULT_TO_FLOAT, ((rgb>>8)&0xFF)*MULT_TO_FLOAT, (rgb&0xFF)*MULT_TO_FLOAT, ((rgb>>24)&0xFF)*MULT_TO_FLOAT);
	}
}
