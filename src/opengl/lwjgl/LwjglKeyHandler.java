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
import java.awt.Toolkit;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.awt.AWTGLCanvas;

import scene.OpenGlKeyHandler;

/**
* @author  Paul Stahr
* @version 26.02.2012
*/
public class LwjglKeyHandler implements OpenGlKeyHandler{
	private final boolean keys[] = new boolean[512];
	private static final int lwjglToJavaKey[] = new int[512];
	private static final int javaToLwjglKey[] = new int[512];
	private int modifier = 0;
	private final AWTGLCanvas source;

	static{
		lwjglToJavaKey[GLFW.GLFW_KEY_LEFT_CONTROL]=	KeyEvent.VK_CONTROL;
		lwjglToJavaKey[GLFW.GLFW_KEY_RIGHT_CONTROL]=	KeyEvent.VK_CONTROL;
		lwjglToJavaKey[GLFW.GLFW_KEY_LEFT_SHIFT]=	KeyEvent.VK_SHIFT;
		lwjglToJavaKey[GLFW.GLFW_KEY_RIGHT_SHIFT]=	KeyEvent.VK_SHIFT;
		lwjglToJavaKey[GLFW.GLFW_KEY_F1]=		KeyEvent.VK_F1;
		lwjglToJavaKey[GLFW.GLFW_KEY_F2]=		KeyEvent.VK_F2;
		lwjglToJavaKey[GLFW.GLFW_KEY_F3]=		KeyEvent.VK_F3;
		lwjglToJavaKey[GLFW.GLFW_KEY_F4]=		KeyEvent.VK_F4;
		lwjglToJavaKey[GLFW.GLFW_KEY_F5]=		KeyEvent.VK_F5;
		lwjglToJavaKey[GLFW.GLFW_KEY_F6]=		KeyEvent.VK_F6;
		lwjglToJavaKey[GLFW.GLFW_KEY_F7]=		KeyEvent.VK_F7;
		lwjglToJavaKey[GLFW.GLFW_KEY_F8]=		KeyEvent.VK_F8;
		lwjglToJavaKey[GLFW.GLFW_KEY_F9]=		KeyEvent.VK_F9;
		lwjglToJavaKey[GLFW.GLFW_KEY_F10]=		KeyEvent.VK_F10;
		lwjglToJavaKey[GLFW.GLFW_KEY_F11]=		KeyEvent.VK_F11;
		lwjglToJavaKey[GLFW.GLFW_KEY_F12]=		KeyEvent.VK_F12;
		lwjglToJavaKey[GLFW.GLFW_KEY_F13]=		KeyEvent.VK_F13;
		lwjglToJavaKey[GLFW.GLFW_KEY_F14]=		KeyEvent.VK_F14;
		lwjglToJavaKey[GLFW.GLFW_KEY_F15]=		KeyEvent.VK_F15;
		lwjglToJavaKey[GLFW.GLFW_KEY_0]=	 		KeyEvent.VK_0;
		lwjglToJavaKey[GLFW.GLFW_KEY_1]=			KeyEvent.VK_1;
		lwjglToJavaKey[GLFW.GLFW_KEY_2]=			KeyEvent.VK_2;
		lwjglToJavaKey[GLFW.GLFW_KEY_3]=			KeyEvent.VK_3;
		lwjglToJavaKey[GLFW.GLFW_KEY_4]=			KeyEvent.VK_4;
		lwjglToJavaKey[GLFW.GLFW_KEY_5]=			KeyEvent.VK_5;
		lwjglToJavaKey[GLFW.GLFW_KEY_6]=			KeyEvent.VK_6;
		lwjglToJavaKey[GLFW.GLFW_KEY_7]=			KeyEvent.VK_7;
		lwjglToJavaKey[GLFW.GLFW_KEY_8]=			KeyEvent.VK_8;
		lwjglToJavaKey[GLFW.GLFW_KEY_9]=			KeyEvent.VK_9;
		lwjglToJavaKey[GLFW.GLFW_KEY_KP_0]=	KeyEvent.VK_NUMPAD0;
		lwjglToJavaKey[GLFW.GLFW_KEY_KP_1]=	KeyEvent.VK_NUMPAD1;
		lwjglToJavaKey[GLFW.GLFW_KEY_KP_2]=	KeyEvent.VK_NUMPAD2;
		lwjglToJavaKey[GLFW.GLFW_KEY_KP_3]=	KeyEvent.VK_NUMPAD3;
		lwjglToJavaKey[GLFW.GLFW_KEY_KP_4]=	KeyEvent.VK_NUMPAD4;
		lwjglToJavaKey[GLFW.GLFW_KEY_KP_5]=	KeyEvent.VK_NUMPAD5;
		lwjglToJavaKey[GLFW.GLFW_KEY_KP_6]=	KeyEvent.VK_NUMPAD6;
		lwjglToJavaKey[GLFW.GLFW_KEY_KP_7]=	KeyEvent.VK_NUMPAD7;
		lwjglToJavaKey[GLFW.GLFW_KEY_KP_8]=	KeyEvent.VK_NUMPAD8;
		lwjglToJavaKey[GLFW.GLFW_KEY_KP_9]=	KeyEvent.VK_NUMPAD9;
		lwjglToJavaKey[GLFW.GLFW_KEY_A]=			KeyEvent.VK_A;
		lwjglToJavaKey[GLFW.GLFW_KEY_B]=			KeyEvent.VK_B;
		lwjglToJavaKey[GLFW.GLFW_KEY_C]=			KeyEvent.VK_C;
		lwjglToJavaKey[GLFW.GLFW_KEY_D]=			KeyEvent.VK_D;
		lwjglToJavaKey[GLFW.GLFW_KEY_E]=			KeyEvent.VK_E;
		lwjglToJavaKey[GLFW.GLFW_KEY_F]=			KeyEvent.VK_F;
		lwjglToJavaKey[GLFW.GLFW_KEY_G]=			KeyEvent.VK_G;
		lwjglToJavaKey[GLFW.GLFW_KEY_H]=			KeyEvent.VK_H;
		lwjglToJavaKey[GLFW.GLFW_KEY_I]=			KeyEvent.VK_I;
		lwjglToJavaKey[GLFW.GLFW_KEY_J]=			KeyEvent.VK_J;
		lwjglToJavaKey[GLFW.GLFW_KEY_K]=			KeyEvent.VK_K;
		lwjglToJavaKey[GLFW.GLFW_KEY_L]=			KeyEvent.VK_L;
		lwjglToJavaKey[GLFW.GLFW_KEY_M]=			KeyEvent.VK_M;
		lwjglToJavaKey[GLFW.GLFW_KEY_N]=			KeyEvent.VK_N;
		lwjglToJavaKey[GLFW.GLFW_KEY_O]=			KeyEvent.VK_O;
		lwjglToJavaKey[GLFW.GLFW_KEY_P]=			KeyEvent.VK_P;
		lwjglToJavaKey[GLFW.GLFW_KEY_Q]=			KeyEvent.VK_Q;
		lwjglToJavaKey[GLFW.GLFW_KEY_R]=			KeyEvent.VK_R;
		lwjglToJavaKey[GLFW.GLFW_KEY_S]=			KeyEvent.VK_S;
		lwjglToJavaKey[GLFW.GLFW_KEY_T]=			KeyEvent.VK_T;
		lwjglToJavaKey[GLFW.GLFW_KEY_U]=			KeyEvent.VK_U;
		lwjglToJavaKey[GLFW.GLFW_KEY_V]=			KeyEvent.VK_V;
		lwjglToJavaKey[GLFW.GLFW_KEY_W]=			KeyEvent.VK_W;
		lwjglToJavaKey[GLFW.GLFW_KEY_X]=			KeyEvent.VK_X;
		lwjglToJavaKey[GLFW.GLFW_KEY_Y]=			KeyEvent.VK_Y;
		lwjglToJavaKey[GLFW.GLFW_KEY_Z]=			KeyEvent.VK_Z;
		lwjglToJavaKey[GLFW.GLFW_KEY_LEFT]=		KeyEvent.VK_LEFT;
		lwjglToJavaKey[GLFW.GLFW_KEY_RIGHT]=		KeyEvent.VK_RIGHT;
		lwjglToJavaKey[GLFW.GLFW_KEY_DOWN]=		KeyEvent.VK_DOWN;
		lwjglToJavaKey[GLFW.GLFW_KEY_UP]=		KeyEvent.VK_UP;
		lwjglToJavaKey[GLFW.GLFW_KEY_MINUS]=		KeyEvent.VK_MINUS;
		lwjglToJavaKey[GLFW.GLFW_KEY_KP_SUBTRACT]=	KeyEvent.VK_SUBTRACT;
		lwjglToJavaKey[GLFW.GLFW_KEY_KP_ADD]=		KeyEvent.VK_ADD;
		for (int i = 0; i < lwjglToJavaKey.length; ++i)
		{
			javaToLwjglKey[Math.min(lwjglToJavaKey[i], javaToLwjglKey.length - 1)] = i;
		}
	}

	public LwjglKeyHandler(AWTGLCanvas source){
		this.source = source;
	}

	@Override
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
		if (keys[GLFW.GLFW_KEY_LEFT_SHIFT] || keys[GLFW.GLFW_KEY_RIGHT_SHIFT])
			modifier |= InputEvent.SHIFT_DOWN_MASK;
		if (keys[GLFW.GLFW_KEY_LEFT_CONTROL] || keys[GLFW.GLFW_KEY_RIGHT_CONTROL])
			modifier |= InputEvent.CTRL_DOWN_MASK;
		if (keys[GLFW.GLFW_KEY_LEFT_ALT])
			modifier |= InputEvent.ALT_DOWN_MASK;
		if (keys[GLFW.GLFW_KEY_RIGHT_ALT])
			modifier |= InputEvent.ALT_GRAPH_DOWN_MASK;
	}

	public final void doIO(){
		/*while (Keyboard.next()){
			final int key = Keyboard.getEventKey();
			final boolean down = Keyboard.getEventKeyState();
			if (keys[key]!=down){
				keys[key]= down;
				updateModifier();
			}
			EventQueue.invokeLater(new KeyEventRunnable(source, down ? KeyEvent.KEY_PRESSED : KeyEvent.KEY_RELEASED, Keyboard.getEventNanoseconds(), modifier, getJavaKey(key), Keyboard.getEventCharacter()));
		}*/
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
	}
}
