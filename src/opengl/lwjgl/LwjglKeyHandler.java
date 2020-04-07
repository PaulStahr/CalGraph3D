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

import java.awt.Component;
import java.awt.EventQueue;
import java.awt.Toolkit;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import org.lwjgl.input.Keyboard;

import scene.OpenGlKeyHandler;

/** 
* @author  Paul Stahr
* @version 26.02.2012
*/
public class LwjglKeyHandler implements OpenGlKeyHandler{
	private final boolean keys[] = new boolean[256];
	private static final int lwjglToJavaKey[] = new int[256];
	private static final int javaToLwjglKey[] = new int[512];
	private int modifier = 0;
	private final Component source;
	
	static{
		lwjglToJavaKey[Keyboard.KEY_LCONTROL]=	KeyEvent.VK_CONTROL;
		lwjglToJavaKey[Keyboard.KEY_RCONTROL]=	KeyEvent.VK_CONTROL;
		lwjglToJavaKey[Keyboard.KEY_LSHIFT]=	KeyEvent.VK_SHIFT;
		lwjglToJavaKey[Keyboard.KEY_RSHIFT]=	KeyEvent.VK_SHIFT;
		lwjglToJavaKey[Keyboard.KEY_F1]=		KeyEvent.VK_F1;
		lwjglToJavaKey[Keyboard.KEY_F2]=		KeyEvent.VK_F2;
		lwjglToJavaKey[Keyboard.KEY_F3]=		KeyEvent.VK_F3;
		lwjglToJavaKey[Keyboard.KEY_F4]=		KeyEvent.VK_F4;
		lwjglToJavaKey[Keyboard.KEY_F5]=		KeyEvent.VK_F5;
		lwjglToJavaKey[Keyboard.KEY_F6]=		KeyEvent.VK_F6;
		lwjglToJavaKey[Keyboard.KEY_F7]=		KeyEvent.VK_F7;
		lwjglToJavaKey[Keyboard.KEY_F8]=		KeyEvent.VK_F8;
		lwjglToJavaKey[Keyboard.KEY_F9]=		KeyEvent.VK_F9;
		lwjglToJavaKey[Keyboard.KEY_F10]=		KeyEvent.VK_F10;
		lwjglToJavaKey[Keyboard.KEY_F11]=		KeyEvent.VK_F11;
		lwjglToJavaKey[Keyboard.KEY_F12]=		KeyEvent.VK_F12;
		lwjglToJavaKey[Keyboard.KEY_F13]=		KeyEvent.VK_F13;
		lwjglToJavaKey[Keyboard.KEY_F14]=		KeyEvent.VK_F14;
		lwjglToJavaKey[Keyboard.KEY_F15]=		KeyEvent.VK_F15;
		lwjglToJavaKey[Keyboard.KEY_0]=	 		KeyEvent.VK_0;
		lwjglToJavaKey[Keyboard.KEY_1]=			KeyEvent.VK_1;
		lwjglToJavaKey[Keyboard.KEY_2]=			KeyEvent.VK_2;
		lwjglToJavaKey[Keyboard.KEY_3]=			KeyEvent.VK_3;
		lwjglToJavaKey[Keyboard.KEY_4]=			KeyEvent.VK_4;
		lwjglToJavaKey[Keyboard.KEY_5]=			KeyEvent.VK_5;
		lwjglToJavaKey[Keyboard.KEY_6]=			KeyEvent.VK_6;
		lwjglToJavaKey[Keyboard.KEY_7]=			KeyEvent.VK_7;
		lwjglToJavaKey[Keyboard.KEY_8]=			KeyEvent.VK_8;
		lwjglToJavaKey[Keyboard.KEY_9]=			KeyEvent.VK_9;
		lwjglToJavaKey[Keyboard.KEY_NUMPAD0]=	KeyEvent.VK_NUMPAD0;
		lwjglToJavaKey[Keyboard.KEY_NUMPAD1]=	KeyEvent.VK_NUMPAD1;
		lwjglToJavaKey[Keyboard.KEY_NUMPAD2]=	KeyEvent.VK_NUMPAD2;
		lwjglToJavaKey[Keyboard.KEY_NUMPAD3]=	KeyEvent.VK_NUMPAD3;
		lwjglToJavaKey[Keyboard.KEY_NUMPAD4]=	KeyEvent.VK_NUMPAD4;
		lwjglToJavaKey[Keyboard.KEY_NUMPAD5]=	KeyEvent.VK_NUMPAD5;
		lwjglToJavaKey[Keyboard.KEY_NUMPAD6]=	KeyEvent.VK_NUMPAD6;
		lwjglToJavaKey[Keyboard.KEY_NUMPAD7]=	KeyEvent.VK_NUMPAD7;
		lwjglToJavaKey[Keyboard.KEY_NUMPAD8]=	KeyEvent.VK_NUMPAD8;
		lwjglToJavaKey[Keyboard.KEY_NUMPAD9]=	KeyEvent.VK_NUMPAD9;
		lwjglToJavaKey[Keyboard.KEY_A]=			KeyEvent.VK_A;
		lwjglToJavaKey[Keyboard.KEY_B]=			KeyEvent.VK_B;
		lwjglToJavaKey[Keyboard.KEY_C]=			KeyEvent.VK_C;
		lwjglToJavaKey[Keyboard.KEY_D]=			KeyEvent.VK_D;
		lwjglToJavaKey[Keyboard.KEY_E]=			KeyEvent.VK_E;
		lwjglToJavaKey[Keyboard.KEY_F]=			KeyEvent.VK_F;
		lwjglToJavaKey[Keyboard.KEY_G]=			KeyEvent.VK_G;
		lwjglToJavaKey[Keyboard.KEY_H]=			KeyEvent.VK_H;
		lwjglToJavaKey[Keyboard.KEY_I]=			KeyEvent.VK_I;
		lwjglToJavaKey[Keyboard.KEY_J]=			KeyEvent.VK_J;
		lwjglToJavaKey[Keyboard.KEY_K]=			KeyEvent.VK_K;
		lwjglToJavaKey[Keyboard.KEY_L]=			KeyEvent.VK_L;
		lwjglToJavaKey[Keyboard.KEY_M]=			KeyEvent.VK_M;
		lwjglToJavaKey[Keyboard.KEY_N]=			KeyEvent.VK_N;
		lwjglToJavaKey[Keyboard.KEY_O]=			KeyEvent.VK_O;
		lwjglToJavaKey[Keyboard.KEY_P]=			KeyEvent.VK_P;
		lwjglToJavaKey[Keyboard.KEY_Q]=			KeyEvent.VK_Q;
		lwjglToJavaKey[Keyboard.KEY_R]=			KeyEvent.VK_R;
		lwjglToJavaKey[Keyboard.KEY_S]=			KeyEvent.VK_S;
		lwjglToJavaKey[Keyboard.KEY_T]=			KeyEvent.VK_T;
		lwjglToJavaKey[Keyboard.KEY_U]=			KeyEvent.VK_U;
		lwjglToJavaKey[Keyboard.KEY_V]=			KeyEvent.VK_V;
		lwjglToJavaKey[Keyboard.KEY_W]=			KeyEvent.VK_W;
		lwjglToJavaKey[Keyboard.KEY_X]=			KeyEvent.VK_X;
		lwjglToJavaKey[Keyboard.KEY_Y]=			KeyEvent.VK_Y;
		lwjglToJavaKey[Keyboard.KEY_Z]=			KeyEvent.VK_Z;
		lwjglToJavaKey[Keyboard.KEY_LEFT]=		KeyEvent.VK_LEFT;
		lwjglToJavaKey[Keyboard.KEY_RIGHT]=		KeyEvent.VK_RIGHT;
		lwjglToJavaKey[Keyboard.KEY_DOWN]=		KeyEvent.VK_DOWN;
		lwjglToJavaKey[Keyboard.KEY_UP]=		KeyEvent.VK_UP;
		lwjglToJavaKey[Keyboard.KEY_MINUS]=		KeyEvent.VK_MINUS;
		lwjglToJavaKey[Keyboard.KEY_SUBTRACT]=	KeyEvent.VK_SUBTRACT;
		lwjglToJavaKey[Keyboard.KEY_ADD]=		KeyEvent.VK_ADD;
		for (int i = 0; i < lwjglToJavaKey.length; ++i)
		{
			javaToLwjglKey[Math.min(lwjglToJavaKey[i], javaToLwjglKey.length - 1)] = i;
		}
	}
	
	public LwjglKeyHandler(Component source){
		this.source = source;
	}
	
	public final boolean isJavaKeyDown(int key)
	{
		if (key > javaToLwjglKey.length)
		{
			return false;
		}
		return isKeyDown(javaToLwjglKey[key]);
	}
	
	public final boolean isKeyDown(int key){
		return keys[key];
	}
	
	private final void updateModifier(){
		modifier = 0;
		if (keys[Keyboard.KEY_LSHIFT] || keys[Keyboard.KEY_RSHIFT])	
			modifier |= InputEvent.SHIFT_DOWN_MASK;
		if (keys[Keyboard.KEY_LCONTROL] || keys[Keyboard.KEY_RCONTROL])
			modifier |= InputEvent.CTRL_DOWN_MASK;
		if (keys[Keyboard.KEY_LMENU])
			modifier |= InputEvent.ALT_DOWN_MASK;
		if (keys[Keyboard.KEY_RMENU])
			modifier |= InputEvent.ALT_GRAPH_DOWN_MASK;
	}
	
	public final void doIO(){
		while (Keyboard.next()){
			final int key = Keyboard.getEventKey();
			final boolean down = Keyboard.getEventKeyState();
			if (keys[key]!=down){
				keys[key]= down;
				updateModifier();
			}
			EventQueue.invokeLater(new KeyEventRunnable(source, down ? KeyEvent.KEY_PRESSED : KeyEvent.KEY_RELEASED, Keyboard.getEventNanoseconds(), modifier, getJavaKey(key), Keyboard.getEventCharacter()));	
		}
	}
	
	private static final int getJavaKey(final int key){
		return lwjglToJavaKey[key];
	}
	
	private final class KeyEventRunnable extends KeyEvent implements Runnable{
		private static final long serialVersionUID = 3667816066993170179L;

		public KeyEventRunnable(Component source, int id, long when, int modifiers, int keyCode, char keyChar) {
			super(source, id, when, modifiers, keyCode, keyChar);
		}

		@Override
		public void run(){
			((Component)source).dispatchEvent(this);
			Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(this);
		}
	};
}
