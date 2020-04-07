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

import java.awt.Component;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import scene.OpenGlMouseHandler;

public class JoglMouseHandler implements OpenGlMouseHandler, MouseListener , MouseMotionListener, MouseWheelListener{
	//private final Component source;
	private int x, y, rot;
	private boolean buttonDown[] = new boolean[3];
	
	public JoglMouseHandler(Component source) {
		//this.source = source;
		source.addMouseListener(this);
		source.addMouseMotionListener(this);
		source.addMouseWheelListener(this);
	}

	@Override
	public boolean isButtonDown(int i) {
		return buttonDown[i];
	}

	@Override
	public int getX() {
		return x;
	}

	@Override
	public int getY() {
		return y;
	}

	@Override
	public int getDWheel() {
		return rot * rot * rot;
	}

	@Override
	public void mouseClicked(MouseEvent e) {}

	@Override
	public void mouseEntered(MouseEvent e) {}

	@Override
	public void mouseExited(MouseEvent e) {}

	@Override
	public void mousePressed(MouseEvent e) {
		x = e.getX();
		y = e.getComponent().getHeight() - e.getY() - 1;
		switch (e.getButton())
		{
			case MouseEvent.BUTTON1:buttonDown[0] = true;break;
			case MouseEvent.BUTTON3:buttonDown[1] = true;break;
			case MouseEvent.BUTTON2:buttonDown[2] = true;break;
			
		}
		
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		x = e.getX();
		y = e.getComponent().getHeight() - e.getY() - 1;
		switch (e.getButton())
		{
			case MouseEvent.BUTTON1:buttonDown[0] = false;break;
			case MouseEvent.BUTTON3:buttonDown[1] = false;break;
			case MouseEvent.BUTTON2:buttonDown[2] = false;break;
			
		}
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		x = e.getX();
		y = e.getComponent().getHeight() - e.getY() - 1;
	}

	@Override
	public void mouseMoved(MouseEvent e) {
		x = e.getX();
		y = e.getComponent().getHeight() - e.getY() - 1;
	}

	@Override
	public void mouseWheelMoved(MouseWheelEvent arg0) {
		rot -= arg0.getWheelRotation();
	}

}
