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
package opengl.lwjgl;

import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import org.lwjgl.opengl.awt.AWTGLCanvas;

import scene.OpenGlMouseHandler;

public class LwjglMouseHandler implements OpenGlMouseHandler, MouseListener {
    AWTGLCanvas canvas;
    int mouseButtonDown;

    public LwjglMouseHandler(AWTGLCanvas canvas)
    {
        this.canvas = canvas;
        canvas.addMouseListener(this);
    }

	@Override
	public final boolean isButtonDown(int button) {
		return (mouseButtonDown & (1 << button)) != 0;
	}

	@Override
	public final int getX() {
	    Point p = canvas.getMousePosition();
	    return p == null ? 0 : p.x;
	}

	@Override
	public final int getY() {
        Point p = canvas.getMousePosition();
        return p == null ? 0 : p.y;
	}

	@Override
	public final int getDWheel() {
		return 0;//Mouse.getDWheel();
	}

    @Override
    public void mouseClicked(MouseEvent e) {
        // TODO Auto-generated method stub

    }

    @Override
    public void mouseEntered(MouseEvent e) {
        // TODO Auto-generated method stub

    }

    @Override
    public void mouseExited(MouseEvent e) {
        // TODO Auto-generated method stub

    }

    @Override
    public void mousePressed(MouseEvent e) {
        mouseButtonDown |= 1 << e.getButton();
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        mouseButtonDown &= 0x7 ^ (1 << e.getButton());
    }

}
