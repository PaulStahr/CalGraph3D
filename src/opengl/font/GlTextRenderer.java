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
package opengl.font;

import java.awt.Color;
import java.awt.geom.Dimension2D;

public interface GlTextRenderer{
	public static final byte LEFT = -1, MIDDLE = 0, RIGHT = 1, TOP = -1, BOTTOM = 1;
	
	public void beginRendering();
	
	public void endRendering();
	
	public void setColor(Color c);
	
	public void setFontSize(float size);
	
	public void setLineSpacing(double height);
	
	public void getDimension(CharSequence str, Dimension2D dim);
	
	public void draw(CharSequence text, float x, float y, byte hhorizontalPosition, byte verticalPosition);
	
	public void draw(CharSequence text, double x, double y, byte hhorizontalPosition, byte verticalPosition);

	public void finalize();
}
