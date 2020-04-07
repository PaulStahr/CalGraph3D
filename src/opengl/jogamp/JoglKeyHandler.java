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
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import scene.OpenGlKeyHandler;

/** 
* @author  Paul Stahr
* @version 26.02.2012
*/
public class JoglKeyHandler implements KeyListener, OpenGlKeyHandler{
	private final boolean keys[] = new boolean[1024];
	//private int modifier = 0;
	//private final Component source;
	
	
	
	public JoglKeyHandler(Component source){
		//this.source = source;
		source.addKeyListener(this);
	}
	
	public final boolean isKeyDown(int key){
		return keys[key];
	}
	
	/*public final void doIO(){
		while (Keyboard.next()){
			final int key = Keyboard.getEventKey();
			final boolean down = Keyboard.getEventKeyState();
			if (keys[key]!=down){
				keys[key]= down;
				updateModifier();
			}
			EventQueue.invokeLater(new KeyEventRunnable(new KeyEvent(source, down ? KeyEvent.KEY_PRESSED : KeyEvent.KEY_RELEASED, Keyboard.getEventNanoseconds(), modifier, getJavaKey(key), Keyboard.getEventCharacter())));	
		}
	}*/
	
	/*private final class KeyEventRunnable implements Runnable{
		private final KeyEvent ke;
		public KeyEventRunnable(KeyEvent ke){
			this.ke = ke;
		}
		
		@Override
		public void run(){
			source.dispatchEvent(ke);
			Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(ke);
		}
	}*/

	@Override
	public void keyPressed(KeyEvent e) {
		keys[e.getKeyCode()] = true;
	}

	@Override
	public void keyReleased(KeyEvent e) {
		keys[e.getKeyCode()] = false;
	}

	@Override
	public void keyTyped(KeyEvent e) {}

	@Override
	public boolean isJavaKeyDown(int keyDown) {
		return keys[keyDown];
	};

}
