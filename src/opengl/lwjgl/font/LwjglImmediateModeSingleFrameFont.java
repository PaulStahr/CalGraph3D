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
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.geom.Dimension2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.imageio.ImageIO;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import com.jogamp.opengl.GL2;

import opengl.GlTextureHandler.GlTexture;
import opengl.font.GlTextRenderer;
import opengl.lwjgl.LwjglTextureUtil;
import opengl.GlTextureHandler;
import opengl.ImageLoader;

final class LwjglImmediateModeSingleFrameFont implements GlTextRenderer{
	private final ImageFrameData images[];
	private final ImageFrameData errorChar;
	private final GlTexture texture;
	private float scale = 0;
 	
	public LwjglImmediateModeSingleFrameFont(Font font, GlTextureHandler textureHandler){
    	int width = 0;
			FontMetrics fm = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).getGraphics().getFontMetrics(font);
			final int height = fm.getHeight(), ascend = -fm.getAscent();
			int widths[] = new int[256];
    	for (char i=0;i<256;i++)
    		if (font.canDisplay(i))
	        		width += widths[i] = fm.charWidth(i);
    	images = new ImageFrameData[256];
		BufferedImage tex = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics g = tex.getGraphics();
  		g.setColor(Color.WHITE);
		g.setFont(font);
		int x = 0;
		final char charArray[] = new char[1];
    	for (char i=0;i<256;i++){
    		final int charWidth = widths[i];
			if (charWidth != 0){
    			charArray[0] = i;
    			g.drawChars(charArray, 0, 1, x, -ascend);
    			images[i] = new ImageFrameData(x, 0, charWidth, height, width, height);        		
    			x += charWidth;
			}
    	}
       	errorChar = images.length <= '?' ? null : images['?'];
    	final ImageLoader imgLoader = new ImageLoader();
    	texture = LwjglTextureUtil.getTexture (
        				tex,
        				GL12.GL_CLAMP_TO_EDGE,
        				GL12.GL_CLAMP_TO_EDGE,
        				GL11.GL_LINEAR,
        				GL11.GL_LINEAR,
        				GL11.GL_MODULATE,
        				imgLoader,
        				ImageLoader.TargetFormat.S8,
        				GL2.GL_ALPHA,
        				textureHandler);
		g.dispose();
	}
	
    public LwjglImmediateModeSingleFrameFont (ZipInputStream inStream, GlTextureHandler textureHandler, ImageLoader imgLoader) throws IOException{
    	ZipEntry entry;
		BufferedImage bufImages[] = new BufferedImage[128];
    	int width = 0, height = 0, maxIndex = 0;
    	while ((entry=inStream.getNextEntry())!= null){
            final String name = entry.getName();
    		try{
    			final int index =Integer.parseInt(name.substring(0, name.indexOf('.')));
    			if (index > bufImages.length)
    				bufImages = Arrays.copyOf(bufImages, Math.max(bufImages.length*2, index));
    			if (index > maxIndex)
    				maxIndex = index;
    			BufferedImage img = bufImages[index] = ImageIO.read(inStream);
    			width += img.getWidth();
       			if (height < img.getHeight())
    				height = img.getHeight();
     		}catch(NumberFormatException e){}
    	}
    	images = new ImageFrameData[maxIndex];
		BufferedImage tex = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
		Graphics g = tex.getGraphics();
		int x = 0;
    	for (int i=0;i<bufImages.length;i++){
    		BufferedImage bufi = bufImages[i];
    		if (bufi != null){
    			g.drawImage(bufi, x, 0, null);
    			images[i] = new ImageFrameData(x, 0, bufi.getWidth(), bufi.getHeight(), width, height);
    			x += bufi.getWidth();
    		}
    	}
       	errorChar = images.length <= '?' ? null : images['?'];
    	texture = LwjglTextureUtil.getTexture (
				tex,
				GL12.GL_CLAMP_TO_EDGE,
				GL12.GL_CLAMP_TO_EDGE,
				GL11.GL_LINEAR,
				GL11.GL_LINEAR,
				GL11.GL_MODULATE,
				imgLoader,
				ImageLoader.TargetFormat.S8,
				GL2.GL_ALPHA,
				textureHandler);
    }
	
	
	public LwjglImmediateModeSingleFrameFont(String directory, GlTextureHandler textureHandler, ImageLoader imgLoader) throws IOException {
		BufferedImage bufImages[] = new BufferedImage[128];
		File dir = new File(directory);
        final File files[] = dir.listFiles();
    	int width = 0, height = 0, maxIndex = 0;
        for (File file : files){
            final String name = file.getName();
            try {
    			final int index =Integer.parseInt(name.substring(0, name.indexOf('.')));
    			if (index > maxIndex)
    				maxIndex = index;
    			if (index > bufImages.length)
       				bufImages = Arrays.copyOf(bufImages, Math.max(bufImages.length*2, index));
     			 BufferedImage img = bufImages[index] = ImageIO.read(file);
     			 width += img.getWidth();
     			 if (height < img.getHeight())
     				height = img.getHeight();
            }catch (NumberFormatException e){}catch (FileNotFoundException e){}
        }
    	images = new ImageFrameData[maxIndex];
		BufferedImage tex = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
		Graphics g = tex.getGraphics();
		int x = 0;
    	for (int i=0;i<images.length;i++){
    		BufferedImage bufi =bufImages[i];
    		if (bufi != null){
    			g.drawImage(bufi, x, 0, null);
    			images[i] = new ImageFrameData(x, 0, bufi.getWidth(), bufi.getHeight(), width, height);
    			x += bufi.getWidth();
    		}
    	}
       	errorChar = images.length <= '?' ? null : images['?'];
    	texture = LwjglTextureUtil.getTexture (
				tex,
				GL12.GL_CLAMP_TO_EDGE,
				GL12.GL_CLAMP_TO_EDGE,
				GL11.GL_LINEAR,
				GL11.GL_LINEAR,
				GL11.GL_MODULATE,
				imgLoader,
				ImageLoader.TargetFormat.S8,
				GL2.GL_ALPHA,
				textureHandler);    		
	}

	public final void draw(char c, float x, float y, float scale){
    	if (c == '\n')
    		return;
        float top = y, bottom=top-scale;
    	final ImageFrameData tex;
    	if (c < images.length && images[c] != null)
    		tex = images[c];
    	else if (errorChar == null)
    		return;
    	else
    		tex = errorChar;
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(tex.rx0, tex.ry0);
        GL11.glVertex2f(x, bottom);
        GL11.glTexCoord2f(tex.rx0, tex.ry1);
        GL11.glVertex2f(x, top);
        x += tex.aspect*scale;
        GL11.glTexCoord2f(tex.rx1, tex.ry1);
        GL11.glVertex2f(x, top);
        GL11.glTexCoord2f(tex.rx1, tex.ry0);
        GL11.glVertex2f(x, bottom);
        GL11.glEnd();
	}
	
	@Override
	public void draw(CharSequence text, float x, float y, byte horizontalPosition, byte verticalPosition) {
        if (text == null)
            throw new NullPointerException();
        float top = y, bottom=top-scale;
        GL11.glBegin(GL11.GL_QUADS);
        for (int i=0;i<text.length();i++){
        	final int c = text.charAt(i);
        	final ImageFrameData tex;
        	if (c == '\n'){
        		bottom = (top = bottom) - scale;
        		x = 0;
        		continue;
        	}else if (c < images.length && images[c] != null)
        		tex = images[c];
        	else if (errorChar == null)
        		continue;
        	else
        		tex = errorChar;
            GL11.glTexCoord2f(tex.rx0, tex.ry0);
            GL11.glVertex2f(x, bottom);
            GL11.glTexCoord2f(tex.rx0, tex.ry1);
            GL11.glVertex2f(x, top);
            x+=tex.aspect*scale;
            GL11.glTexCoord2f(tex.rx1, tex.ry1);
            GL11.glVertex2f(x, top);
            GL11.glTexCoord2f(tex.rx1, tex.ry0);
            GL11.glVertex2f(x, bottom);
        }
        GL11.glEnd();
	}
	
	private static final class ImageFrameData{
		@SuppressWarnings("unused")
		public final int ax0, ay0, ax1, ay1, aWidth, aHeight;
		@SuppressWarnings("unused")
		public final float aspect, rx0, ry0, rx1, ry1, rWidth, rHeight;
		public ImageFrameData(int x, int y, int width, int height, int fullWidth, int fullHeight){
			this.ax1 = (this.ax0 = x) + (this.aWidth = width);
			this.ay1 = (this.ay0 = y) + (this.aHeight = height);
			aspect = (float)width/height;
			rx1 = (rx0 = (float)x/fullWidth) + (rWidth = (float)width/fullWidth);
			ry1 = (ry0 = (float)y/fullHeight) + (rHeight = (float)height/fullHeight);
		}
	}

	@Override
	public void beginRendering() {
    	GL11.glEnable(GL11.GL_TEXTURE_2D);
        texture.bind();
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
		scale  = size;
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
