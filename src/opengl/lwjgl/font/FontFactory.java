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
package opengl.lwjgl.font;

import java.awt.Font;
import java.io.IOException;
import java.util.zip.ZipInputStream;

import opengl.GlTextureHandler;
import opengl.ImageLoader;
import opengl.font.GlTextRenderer;


public class FontFactory {
	public static final byte SINGLETEXTURE = 0, MULTITEXTURE = 1;

    /**
     * Creates a Font from a folder structure. Has to be called by GlThread
	 * @param directory The directory of the Font
	 * @param type TextureType, Can be SINGLETEXTURE or MULTITEXTURE
	 * @param textureHandler The textureHandler of the glContext
	 * @param imgLoader An imageLoader
	 * @return The created FontRenderer
	 * @throws IOException Forwards any IOError that comes up from the imageLoader
	 */
	public static final GlTextRenderer getInstance(String directory, byte type, GlTextureHandler textureHandler, ImageLoader imgLoader) throws IOException{
	    	switch(type){
	    		case MULTITEXTURE: return new LwjglImmediateModeMultiFrameFont(directory, textureHandler, imgLoader);
	    		case SINGLETEXTURE: return new LwjglImmediateModeSingleFrameFont(directory, textureHandler, imgLoader);
	    		default: throw new IllegalArgumentException();
	    	}
	    }


	    public static final GlTextRenderer getInstance(ZipInputStream stream, byte type, GlTextureHandler textureHandler, ImageLoader imgLoader) throws IOException{
	    	switch(type){
				case MULTITEXTURE: return new LwjglImmediateModeMultiFrameFont(stream, textureHandler, imgLoader);
				case SINGLETEXTURE: return new LwjglImmediateModeSingleFrameFont(stream, textureHandler, imgLoader);
				default: throw new IllegalArgumentException();
			}
	    }

	    public static final GlTextRenderer getInstance(Font font, byte type, GlTextureHandler textureHandler) throws IOException{
	    	switch(type){
				//case MULTITEXTURE: return new MultiFrameFont(font);
				case SINGLETEXTURE: return new LwjglImmediateModeSingleFrameFont(font, textureHandler);
				default: throw new IllegalArgumentException();
			}
	    }
}
