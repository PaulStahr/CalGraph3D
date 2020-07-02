package opengl.font;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.geom.Dimension2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.nio.FloatBuffer;

import util.Buffers;
import util.StringUtils;

public abstract class AbstractBufferedDynamicSingleFrameFont implements GlTextRenderer{
 	protected final FloatBuffer fBuf;
	protected float scale;
	protected float realFontSize;
	protected float realMaxLineHeight;
	protected float fontSize;
	protected float maxLineHeight;
 	protected final float charToWidth[] = new float[0x10000];
 	private float lineSpacing = 1;
 	protected final int rows, cols;
 	protected int vertexCount;
 	protected final Font font;
 	protected final FontMetrics fm;
 	protected final char charArray[] = new char[1];
	protected Color color;

 	public AbstractBufferedDynamicSingleFrameFont(Font font) {
 		this.font = font;
 		fm = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).getGraphics().getFontMetrics(font);
 		fBuf = Buffers.createFloatBuffer(0x10000);
		rows = 16;
		cols = 16;
		
	}
 	
	protected static final int roundUp(double d){
		return (int)(-Math.nextUp(-d) + 1);
	}

	@Override
	public final void draw(CharSequence text, float x, float y, byte horizontalPosition, byte verticalPosition){
		if (text == null)
            throw new NullPointerException();
        switch(verticalPosition){
			case BOTTOM:
				break;
			case MIDDLE:
				y += getHeight(text) / 2;
				break;
			case TOP:
				y += getHeight(text);
				break;
			default:
				break;
        }
        float vY0 = y;
		synchronized(text){
	        int lastIndex = 0, nextIndex = StringUtils.indexOf(text, 0, text.length(), '\n');
	        if (nextIndex == -1)
	        	nextIndex = text.length();
	        while (lastIndex < text.length()){
	        	float width = getWidth(text, lastIndex, nextIndex);
	        	float vX1 = x;
	                     	
	        	switch (horizontalPosition){
					case LEFT:
						break;
					case MIDDLE:
						vX1 -= width / 2;
						break;
					case RIGHT:
						vX1 -= width;
						break;
					default:
						break;
	        	}
	        
	            for (int i=lastIndex;i<nextIndex;i++){
	            	final char c = text.charAt(i);
	            	
	            	ImageFrameData tex = getTexData(c);
	            	if (tex == null){
	            		flush();
	            		tex = getTexData(c);
	            	}
	            	
	            	if ((vertexCount + 6) * 6 >= fBuf.capacity())
	            	{
	            		flush();
	            	}
	            		
	            	if (tex != null){
	            		float vX0 = vX1;
	            	    vX1 += charToWidth[c]*scale;
	            	    float vY1 = vY0 - maxLineHeight;
	            	    float tX0 = tex.x;
	            	    float tX1 = tex.x + charToWidth[c];
	            	    float tY0 = tex.y;
	            	    float tY1 = tex.y + realMaxLineHeight;
	            	
	            		final int bufferPosition = vertexCount * 4;
	            		fBuf.put(bufferPosition, vX0).put(bufferPosition + 1, vY0);
	            	    fBuf.put(bufferPosition + 2, tX0).put(bufferPosition + 3, tY0);
	            	    fBuf.put(bufferPosition + 4, vX0).put(bufferPosition + 5, vY1);
	            	    fBuf.put(bufferPosition + 6, tX0).put(bufferPosition + 7, tY1);
	                    fBuf.put(bufferPosition + 8, vX1).put(bufferPosition + 9, vY1);
	            	    fBuf.put(bufferPosition + 10, tX1).put(bufferPosition + 11, tY1);

	            	    fBuf.put(bufferPosition + 12, vX1).put(bufferPosition + 13, vY1);
	            	    fBuf.put(bufferPosition + 14, tX1).put(bufferPosition + 15, tY1);
	                    fBuf.put(bufferPosition + 16, vX1).put(bufferPosition + 17, vY0);
	            	    fBuf.put(bufferPosition + 18, tX1).put(bufferPosition + 19, tY0);
	            	    fBuf.put(bufferPosition + 20, vX0).put(bufferPosition + 21, vY0);
	            	    fBuf.put(bufferPosition + 22, tX0).put(bufferPosition + 23, tY0);
	            	    vertexCount += 6;
	            	}
	            }
	    		vY0 -= fontSize * lineSpacing;
	    		lastIndex = nextIndex + 1;
	    		nextIndex = StringUtils.indexOf(text, lastIndex, text.length(), '\n');
	    		if (nextIndex == -1)
	    			nextIndex = text.length();
	        }
		}
   	}

	@Override
	public void draw(CharSequence text, double x, double y,	byte horizontalPosition, byte verticalPosition) {
		draw(text, (float)x, (float)y, horizontalPosition, verticalPosition);
	}
	
	protected abstract void flush();
	
	protected abstract ImageFrameData getTexData(char c);
	
	private final float getWidth(char c){
		if (Double.isNaN(charToWidth[c])){
			if (font.canDisplay(c)){
				charArray[0] = c;
				Rectangle2D b = font.getStringBounds(charArray, 0, 1, fm.getFontRenderContext());
				charToWidth[c] = (float)b.getWidth();
			}else{
				charToWidth[c] = 0;
			}
		}
		return charToWidth[c];
	}
	
	public float getHeight(CharSequence text){
		if (text == null)
			return 0;
		return getHeight(text, 0, text.length());
	}
	
	public float getHeight(CharSequence text, int start, int end){
		if (text == null || start == end)
			return 0;
		int lineCount = 0;
		for (int i=start;i<end;++i){
			if (text.charAt(i) == '\n'){
				++lineCount;
			}
		}
		return lineCount * fontSize * lineSpacing + maxLineHeight;		
	}
	
	public float getWidth(CharSequence text){
		if (text == null)
			return 0;
		return getWidth(text, 0, text.length());
	}
	
	public float getWidth(CharSequence text, int start, int end){
		if (text == null)
			return 0;
		float width = 0, maxWidth = 0;
		for (int i=start;i<end;i++){
        	final char c = text.charAt(i);
        	if (c == '\n'){
        		maxWidth = Math.max(width, maxWidth);
        		width = 0;
        	}else if (font.canDisplay(c)){
        		width += getWidth(c);
        	}
        }
		return Math.max(maxWidth, width) * fontSize / realFontSize;	
	}
	
	@Override
	public void getDimension(CharSequence text, Dimension2D dim) {
		if (text == null)
			return;
		int lines = 0;
		float width = 0, maxWidth = 0;
		for (int i=0;i<text.length();i++){
        	final char c = text.charAt(i);
        	if (c == '\n'){
        		maxWidth = Math.max(width, maxWidth);
        		width = 0;
        		++ lines;
        	}else if (font.canDisplay(c)){
        		width += getWidth(c);
        	}
        }
		dim.setSize(Math.max(maxWidth, width) * fontSize / realFontSize, lines * fontSize * lineSpacing + maxLineHeight);
	}

	@Override
	public void setFontSize(float size) {
		this.fontSize = size;
		this.maxLineHeight = size * realMaxLineHeight / realFontSize;
        this.scale = fontSize / realFontSize;
	}

	@Override
	public void setLineSpacing(double height) {
		this.lineSpacing = (float)height;
	}
	
	public void setFontHeight(float size){
		this.maxLineHeight = size;		
		this.fontSize = size * realFontSize / realMaxLineHeight;
	}	
	
	@Override
	public void setColor(Color c) {
		if (this.color != null && !c.equals(color)){
			return;
		}
		flush();
		this.color = c;
	}
	
	
	@Override
	public void finalize() {}
}
