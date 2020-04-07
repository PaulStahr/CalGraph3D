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

import java.awt.image.BufferedImage;
import java.io.IOException;

import com.jogamp.opengl.GL2;

import opengl.GlTextureHandler;
import opengl.ImageLoader;
import opengl.GlTextureHandler.GlTexture;

public class JoglTextureUtil {
    private static final int faces[] = {
    		GL2.GL_TEXTURE_CUBE_MAP_POSITIVE_X,
    		GL2.GL_TEXTURE_CUBE_MAP_NEGATIVE_X, 
    		GL2.GL_TEXTURE_CUBE_MAP_POSITIVE_Y,
    		GL2.GL_TEXTURE_CUBE_MAP_NEGATIVE_Y, 
    		GL2.GL_TEXTURE_CUBE_MAP_POSITIVE_Z,
    		GL2.GL_TEXTURE_CUBE_MAP_NEGATIVE_Z};
    
    public static final int getCorrespondingGlFormat(ImageLoader.TargetFormat format)
	{
		switch(format)
		{
			case R8G8B8:  	return GL2.GL_RGB;
			case R8G8B8A8:	return GL2.GL_RGBA;
			case A8:	  	return GL2.GL_ALPHA;
			case S8:		return GL2.GL_ALPHA;
			default:throw new RuntimeException("Unsupported Format: "  + format);
		}
	}
	
	
public static void setTexParameters(
		int target,
		int clampS,
		int clampT,
		int magFilter,
		int minFilter,
		int mode,
		GL2 gl)
{
    gl.glTexParameteri(target, GL2.GL_TEXTURE_WRAP_S, clampS);
    gl.glTexParameteri(target, GL2.GL_TEXTURE_WRAP_T, clampT);
    gl.glTexParameteri(target, GL2.GL_TEXTURE_MAG_FILTER, magFilter);
    gl.glTexParameteri(target, GL2.GL_TEXTURE_MIN_FILTER, minFilter);
    gl.glTexEnvf(GL2.GL_TEXTURE_ENV, GL2.GL_TEXTURE_ENV_MODE, mode);
}

public static int getCupeMapFace(int i) {
	return faces[i];
}

public static GlTexture getTexture(
		BufferedImage image,
		int clampS,
		int clampT,
		int magFilter,
		int minFilter,
		int env_mode,
		ImageLoader imgLoader,
		ImageLoader.TargetFormat tFormat,
		int format,
		GlTextureHandler texHandler,
		GL2 gl)
{
    gl.glPixelStorei(GL2.GL_UNPACK_ALIGNMENT, 1);
	imgLoader.loadImage(image, tFormat);
    GlTexture tex = texHandler.getTexture(format, GL2.GL_TEXTURE_2D);
    tex.setBounds(imgLoader.getWidth(), imgLoader.getHeight());
    tex.bind();
    JoglTextureUtil.setTexParameters(GL2.GL_TEXTURE_2D, clampS, clampT, magFilter, minFilter, env_mode, gl);
    gl.glTexImage2D(GL2.GL_TEXTURE_2D, 0, format, imgLoader.getWidth(), imgLoader.getHeight(), 0, format, GL2.GL_UNSIGNED_BYTE, imgLoader.getData());
    
 	return tex;
}

public static GlTexture getTexture (
		String filename[], 
		int clampS,
		int clampT,
		int magFilter,
		int minFilter,
		int mode,
		final ImageLoader imgLoader,
		ImageLoader.TargetFormat tFormat,
		int format,
		GlTextureHandler texHandler,
		GL2 gl) throws IOException
{
    gl.glPixelStorei(GL2.GL_UNPACK_ALIGNMENT, 1);
    imgLoader.loadImage(filename[0]);
	GlTexture tex = texHandler.getTexture(format, GL2.GL_TEXTURE_2D);
	tex.setBounds(imgLoader.getWidth(), imgLoader.getHeight());
	setTexParameters(
			GL2.GL_TEXTURE_CUBE_MAP,
			clampS,
			clampT,
			magFilter,
			minFilter,
			GL2.GL_TEXTURE_BINDING_CUBE_MAP,
			gl);
   
    for(int i = 0; i < 6; i++)
    {
    	if (i != 0)
    	{
    		imgLoader.loadImage(filename[i]);
    	}
    	gl.glTexImage2D(JoglTextureUtil.getCupeMapFace(i), 0, format, imgLoader.getWidth(), imgLoader.getHeight(), 0, format, GL2.GL_UNSIGNED_BYTE, imgLoader.getData());

    }
 	return tex;
}
}
