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
package opengl.jogamp.font;


import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jogamp.opengl.GL2;

import opengl.GlBufferObjectHandler;
import opengl.GlBufferObjectHandler.GlBufferObject;
import opengl.GlTextureHandler;
import opengl.GlTextureHandler.GlTexture;
import opengl.ImageLoader;
import opengl.ImageLoader.TargetFormat;
import opengl.font.AbstractBufferedDynamicSingleFrameFont;
import opengl.font.ImageFrameData;
import opengl.jogamp.JoglGlUtils;
import opengl.jogamp.JoglTextureUtil;

public class JoglBufferedDynamicSingleFrameFont extends AbstractBufferedDynamicSingleFrameFont{
	private static final Logger logger = LoggerFactory.getLogger(JoglBufferedDynamicSingleFrameFont.class);
	private static final int GL_INTERNAL_FORMAT = GL2.GL_ALPHA;
	private static final int GL_PIXEL_FORMAT = GL2.GL_ALPHA;
	private static final int AWT_TEXTURE_FORMAT = BufferedImage.TYPE_BYTE_GRAY;
	private static boolean printTime = false;
	private final ImageFrameData images[];
	private GlTexture texture;
	//private boolean mipmap;
 	private final int charToImage[] = new int[0x10000];
 	private Graphics graphics;
	private final int gap = 5;

 	private int drawnBufferCount = 0;
 	private BufferedImage tex;
	private final GlBufferObject objectBuffer;
	private ImageLoader imageLoader;
	private final GL2 gl;
	
	public JoglBufferedDynamicSingleFrameFont(
			Font font,
			boolean mipmap,
			GlTextureHandler textureHandler,
			GlBufferObjectHandler bufferHandler,
			GL2 gl) throws IOException{
		super(font);
		//this.mipmap = mipmap;
		this.gl = gl;
		realFontSize = font.getSize2D();
		Arrays.fill(charToImage, -1);
		objectBuffer = bufferHandler.createBufferObject();
	  
		FontRenderContext frc = fm.getFontRenderContext();
		Rectangle2D maxBounds = font.getMaxCharBounds(frc);
		realMaxLineHeight = (float)maxBounds.getHeight();
		int charWidth = roundUp(maxBounds.getWidth()) + gap;
		int charHeight = roundUp(maxBounds.getHeight()) + gap;
	
		int texWidth = charWidth * cols + gap;
		int texHeight = charHeight * rows + gap;

		this.tex = new BufferedImage(charWidth, charHeight, AWT_TEXTURE_FORMAT);
		this.graphics = this.tex.getGraphics();
		this.graphics.setFont(font);
		BufferedImage tex = new BufferedImage(texWidth, texHeight, AWT_TEXTURE_FORMAT);

    	images = new ImageFrameData[rows * cols];
		Graphics g = tex.getGraphics();
  		g.setColor(Color.WHITE);
		g.setFont(font);
		
		Arrays.fill(charToWidth, Float.NaN);
		int ascend = fm.getAscent();
		char currentChar = 0;
		
    	for (int i=0;i<rows;i++){
    		final int y = i * charHeight + gap;
    		for (int j=0;j<cols;++j){
    			final int x = j * charWidth + gap;
    			while (!font.canDisplay(currentChar))
    				++currentChar;
    			charArray[0] = currentChar;
    			g.drawChars(charArray, 0, 1, x , y + ascend);
    			ImageFrameData ifd = new ImageFrameData(x, y);
      			ifd.character = currentChar;
       			ifd.lastUsed = Integer.MIN_VALUE;
      			images[i * cols + j] = ifd;		
       			charToImage[currentChar] = i * cols + j;
    			charToWidth[currentChar] = (float)font.getStringBounds(charArray, 0, 1, frc).getWidth();
       			++currentChar;
    		}
    	}

	    imageLoader = new ImageLoader();
	    texture = JoglTextureUtil.getTexture(
	    		tex,
        		GL2.GL_CLAMP_TO_EDGE,
        		GL2.GL_CLAMP_TO_EDGE,
        		mipmap ? GL2.GL_LINEAR_MIPMAP_LINEAR : GL2.GL_LINEAR,
        		GL2.GL_LINEAR,
        		GL2.GL_MODULATE,
        		imageLoader,
        		TargetFormat.S8,
        		GL2.GL_ALPHA,
        		textureHandler,
        		gl);
    	g.dispose(); 
	}
	
	@Override
	protected final ImageFrameData getTexData(char c){
		if (charToImage[c] != -1){
			ImageFrameData ifd = images[charToImage[c]];
			ifd.lastUsed = drawnBufferCount;
			return ifd;
		}
		long time = System.nanoTime();
		if (!font.canDisplay(c))
			return null;
		graphics.setColor(Color.BLACK);
		graphics.fillRect(0, 0, tex.getWidth(), tex.getHeight());
		graphics.setColor(Color.WHITE);
		charArray[0] = c;
		graphics.drawChars(charArray, 0, 1, 0, fm.getAscent());
		int min = drawnBufferCount, pos = -1;
		for (int i=0;i<images.length;++i){
			if (images[i].lastUsed < min){
				min = images[i].lastUsed;
				pos = i;
			}
		}
		
		
		if (pos == -1)
			return null;
		
		ImageFrameData toUse = images[pos];
		
		charToImage[toUse.character] = -1;
    	charToImage[c] = pos;
    	toUse.lastUsed = drawnBufferCount;
		toUse.character = c;
        
		imageLoader.loadImage(tex, TargetFormat.S8);
	    
		gl.glTexSubImage2D(
				GL2.GL_TEXTURE_2D,
				0,
				toUse.x,
				toUse.y,
				tex.getWidth(),
				tex.getHeight(),
				GL_INTERNAL_FORMAT,
				GL_PIXEL_FORMAT,
				imageLoader.getData());
		
		if (printTime)
			logger.debug("Loaded character:" + c + " time: " + (System.nanoTime() - time)/1000000000f + 's');
		return toUse;
	}
	
	public void flush(){
		++drawnBufferCount;
		if (color != null){
			JoglGlUtils.colorC(color, gl);
		}

		if (vertexCount != 0){
			fBuf.limit(vertexCount * 4);
			gl.glBufferData(GL2.GL_ARRAY_BUFFER, fBuf.limit() * 4, fBuf, GL2.GL_DYNAMIC_DRAW);
			gl.glDrawArrays(GL2.GL_TRIANGLES, 0, vertexCount);
			fBuf.limit(fBuf.capacity());
		}
	    vertexCount = 0;
	}

	@Override
	public void beginRendering() {
		gl.glEnable(GL2.GL_BLEND);
        //GL2.glDisable(GL2.GL_BLEND);
        gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
        gl.glMatrixMode(GL2.GL_TEXTURE);
        gl.glPushMatrix();
        gl.glScalef(1f / texture.getWidth(), -1f / texture.getHeight(), 1);
        gl.glTranslatef(0, -texture.getHeight(), 0);
        /*if (!texture.getMustFlipVertically()){
        	gl.glScalef(1, -1, 1);
        	gl.glTranslatef(0, -texture.getHeight(), 0);
        }*/
        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glEnable(GL2.GL_TEXTURE_2D);
        gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_S,GL2.GL_CLAMP_TO_BORDER);
		gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_T,GL2.GL_CLAMP_TO_BORDER);
		texture.bind();
		objectBuffer.bind(GL2.GL_ARRAY_BUFFER);
		gl.glEnableClientState(GL2.GL_VERTEX_ARRAY);
		gl.glEnableClientState(GL2.GL_TEXTURE_COORD_ARRAY);
		gl.glVertexPointer(2, GL2.GL_FLOAT, 16, 0);
		gl.glTexCoordPointer(2, GL2.GL_FLOAT, 16, 8);

	}

	@Override
	public void endRendering() {
		flush();
		gl.glDisable(GL2.GL_TEXTURE_2D);
		gl.glDisableClientState(GL2.GL_VERTEX_ARRAY);
		gl.glDisableClientState(GL2.GL_TEXTURE_COORD_ARRAY);

		gl.glMatrixMode(GL2.GL_TEXTURE);
		gl.glPopMatrix();
		gl.glMatrixMode(GL2.GL_MODELVIEW);
	}

	public void finalize() {}
}
