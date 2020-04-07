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

import java.awt.Color;
import java.awt.geom.Dimension2D;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.imageio.ImageIO;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import com.jogamp.opengl.GL2;

import opengl.GlTextureHandler;
import opengl.GlTextureHandler.GlTexture;
import opengl.font.GlTextRenderer;
import opengl.lwjgl.LwjglTextureUtil;
import opengl.ImageLoader;

final class LwjglImmediateModeMultiFrameFont implements GlTextRenderer{
    private final GlTexture zeichen[] = new GlTexture[256];
    private final GlTexture errorChar;
	private float scale;
    public LwjglImmediateModeMultiFrameFont (String directory, GlTextureHandler textureHandler, ImageLoader imgLoader){
        final File dir = new File (directory);
        if (!(dir.exists() && dir.isDirectory()))
            throw new RuntimeException("Can't load Font: Directory \"" + dir.getPath() + "\" not exists");
        final File files[] = dir.listFiles();
        for (File file : files){
            final String name = file.getName();
            try {
                zeichen[Integer.parseInt(name.substring(0, name.indexOf('.')))] 
                		= LwjglTextureUtil.getTexture (
                				ImageIO.read(file)
                				,GL12.GL_CLAMP_TO_EDGE,
                				GL12.GL_CLAMP_TO_EDGE,
                				GL11.GL_LINEAR,
                				GL11.GL_LINEAR,
                				GL11.GL_MODULATE,
                				imgLoader,
                				ImageLoader.TargetFormat.S8,
                				GL2.GL_ALPHA,
                				textureHandler);
            }catch (NumberFormatException e){}
            catch (FileNotFoundException e){}
            catch (IOException e){}
        }
       	errorChar = zeichen.length <= '?' ? null : zeichen['?'];
    }
    
    public LwjglImmediateModeMultiFrameFont (ZipInputStream stream, GlTextureHandler textureHandler, ImageLoader imgLoader) throws IOException{
    	ZipEntry entry;
    	while ((entry=stream.getNextEntry())!= null){
            final String name = entry.getName();
    		try{
    			zeichen[Integer.parseInt(name.substring(0, name.indexOf('.')))] 
                		= LwjglTextureUtil.getTexture (
                				ImageIO.read(stream)
                				,GL12.GL_CLAMP_TO_EDGE,
                				GL12.GL_CLAMP_TO_EDGE,
                				GL11.GL_LINEAR,
                				GL11.GL_LINEAR,
                				GL11.GL_MODULATE,
                				imgLoader,
                				ImageLoader.TargetFormat.S8,
                				GL2.GL_ALPHA,
                				textureHandler);
    		}catch(NumberFormatException e){}
    	}
       	errorChar = zeichen.length <= '?' ? null : zeichen['?'];
    }

	public void draw(char c, float x, float y, byte h, byte v) {
        float top = y, bottom=top-scale;
    	if (c == '\n')
    		return;
    	final GlTexture tex;
        if (c < zeichen.length && zeichen[c] != null)
        	tex = zeichen[c];
        else if (errorChar == null)
        	return;
        else
        	tex = errorChar;
        tex.bind();
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(0f, 0f);
        GL11.glVertex2f(x, bottom);
        GL11.glTexCoord2f(0f, 1f);
        GL11.glVertex2f(x, top);
        x+=tex.getAspect()*scale;
        GL11.glTexCoord2f(1f, 1f);
        GL11.glVertex2f(x, top);
        GL11.glTexCoord2f(1f, 0f);
        GL11.glVertex2f(x, bottom);
        GL11.glEnd();
    }
	
	@Override
	public void draw(CharSequence text, float x, float y, byte horizontalPosition, byte verticalPosition) {
        if (text == null)
            throw new NullPointerException();
        float top = y, bottom=top-scale;
        for (int i=0;i<text.length();i++){
        	final char c = text.charAt(i);
        	final GlTexture tex;
        	if (c == '\n'){
        		bottom =(top = bottom) - scale;
        		x = 0;
        		continue;
        	}else if (c < zeichen.length && zeichen[c] != null)
        		tex = zeichen[c];
        	else if (errorChar == null)
        		continue;
        	else
        		tex = errorChar;
            tex.bind();
            GL11.glBegin(GL11.GL_QUADS);
            GL11.glTexCoord2f(0f, 0f);
            GL11.glVertex2f(x, bottom);
            GL11.glTexCoord2f(0f, 1f);
            GL11.glVertex2f(x, top);
            x+=tex.getAspect()*scale;
            GL11.glTexCoord2f(1f, 1f);
            GL11.glVertex2f(x, top);
            GL11.glTexCoord2f(1f, 0f);
            GL11.glVertex2f(x, bottom);
            GL11.glEnd();
        }
    }

	@Override
	public void beginRendering() {
    	GL11.glEnable(GL11.GL_TEXTURE_2D);
	}

	@Override
	public void endRendering() {
    	GL11.glDisable(GL11.GL_TEXTURE_2D);
	}

	@Override
	public void setColor(Color c) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setFontSize(float size) {
		scale = size;
	}

	@Override
	public void setLineSpacing(double height) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void getDimension(CharSequence str, Dimension2D dim) {
		return;
	}

	@Override
	public void draw(CharSequence text, double x, double y, byte horizontalPosition, byte verticalPosition) {
		draw(text, (float)x, (float)y, horizontalPosition, verticalPosition);
	}
	
	public void finalize() {}
}
