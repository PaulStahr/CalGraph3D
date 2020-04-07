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
package scene;

import java.awt.event.KeyEvent;

import geometry.Rotation3;
import geometry.Vector3f;
import opengl.Camera;

public class SimpleCameraListener implements Runnable{
    private static final byte MOUSE_NO_CLICK = 0, MOUSE_LEFT_CLICK = 1, MOUSE_RIGHT_CLICK = 2, MOUSE_BOTH_CLICK = MOUSE_LEFT_CLICK | MOUSE_RIGHT_CLICK;
    private int clickedX, clickedY, mouseState;
    private final Vector3f oldCameraPosition = new Vector3f();
    private final Rotation3 oldCameraRotation = new Rotation3();
    private long time;
    private long rotSpeed, moveSpeed;
    private final Vector3f vector = new Vector3f();
	private final OpenGlKeyHandler keyHandler;
	private final OpenGlMouseHandler mouseHandler;
	private final Camera camera;
	private final Scene scene;
	
	public SimpleCameraListener(OpenGlKeyHandler keyboard, OpenGlMouseHandler mouseHandler, Scene scene, Camera camera) 
	{
		this.keyHandler = keyboard;
		this.mouseHandler = mouseHandler;
		this.camera = camera;
		this.scene = scene;
	}
	
	@Override
	public void run() {
		//Position cameraPosition = scene.cameraPosition;
		Vector3f cameraPosition = scene.cameraPosition;
		Rotation3 cameraRotation = scene.cameraRotation;
    	final long newTime = System.nanoTime();
        final long passedTime = newTime - time;
        time = newTime;
        final float moveRate = (1000000 + moveSpeed)*0.00000000000000002f*passedTime;
        final int rotRate = (int)(((1000000 + rotSpeed) * passedTime) / 100000000);
        boolean pressedRotate=false, pressedMove=false;
        vector.set(0f, 0f, 0f);
        if (keyHandler.isJavaKeyDown(KeyEvent.VK_LEFT)) {
            cameraRotation.addZInt(rotRate);
            pressedRotate = true;
        }
        if (keyHandler.isJavaKeyDown(KeyEvent.VK_RIGHT)) {
            cameraRotation.addZInt(-rotRate);
            pressedRotate = true;
        }
        if (keyHandler.isJavaKeyDown(KeyEvent.VK_DOWN)) {
            cameraRotation.addXInt(-rotRate);
            pressedRotate = true;
        }
        if (keyHandler.isJavaKeyDown(KeyEvent.VK_UP)) {
            cameraRotation.addXInt(rotRate);
            pressedRotate = true;
        }
        if (keyHandler.isJavaKeyDown(KeyEvent.VK_NUMPAD4)) {
            vector.x -=moveRate;
            pressedMove = true;
        }        
        if (keyHandler.isJavaKeyDown(KeyEvent.VK_NUMPAD6)) {
            vector.x += moveRate;
            pressedMove = true;
        }        
        if (keyHandler.isJavaKeyDown(KeyEvent.VK_NUMPAD8)) {
            vector.y += moveRate;
            pressedMove = true;
        }        
        if (keyHandler.isJavaKeyDown(KeyEvent.VK_NUMPAD2)) {
            vector.y -= moveRate;
            pressedMove = true;
        }
        if (keyHandler.isJavaKeyDown(KeyEvent.VK_ADD) || keyHandler.isJavaKeyDown(KeyEvent.VK_PLUS)){
            vector.z -= moveRate;
            pressedMove = true;
        }
        if (keyHandler.isJavaKeyDown(KeyEvent.VK_SUBTRACT) || keyHandler.isJavaKeyDown(KeyEvent.VK_MINUS)){
            vector.z += moveRate;
            pressedMove = true;
        }       
        if (pressedRotate || pressedMove)
        {
        	scene.repaint();
        }
       	moveSpeed=pressedMove ? Math.min(moveSpeed + passedTime, 3000000000l):0;
       	rotSpeed=pressedRotate ? Math.min(rotSpeed + passedTime, 3000000000l):0;
        final int wheel=mouseHandler.getDWheel();
        int mx = mouseHandler.getX(), my = camera.getHeight() - mouseHandler.getY();
        if (pressedMove || wheel!=0){
            if (wheel!=0){
            	final float distance = wheel*0.03f;
            	vector.add(distance * ((float)mx / camera.getWidth()-0.5f) * 0.5f, distance * ((float)my / camera.getHeight()-0.5f) * 0.5f, -distance);
            }
        	vector.rotateReverseXYZEuler(cameraRotation);
        	cameraPosition.add(vector);
        	scene.repaint();
        }
        boolean mouseButtonChanged = false;
        if (mouseHandler.isButtonDown(0) || mouseHandler.isButtonDown(2)){
        	if (mouseHandler.isButtonDown(1) || mouseHandler.isButtonDown(2)){
                if (mouseState != MOUSE_BOTH_CLICK){
                    mouseState = MOUSE_BOTH_CLICK;
                    mouseButtonChanged = true;
                }else{
                	cameraPosition.set(oldCameraPosition);
                    cameraRotation.set(oldCameraRotation);
                	vector.set((clickedX - mx)*camera.getAngle()/camera.getWidth(),(my - clickedY)*camera.getAngle()/camera.getHeight(),0);
                	vector.rotateReverseXYZEuler(cameraRotation);
                	cameraPosition.add(vector);
                	scene.repaint();
                }
            }else{
                if (mouseState != MOUSE_LEFT_CLICK){
                    mouseState = MOUSE_LEFT_CLICK;
                    mouseButtonChanged = true;
                }else{
                    cameraRotation.set(oldCameraRotation);
                    cameraRotation.addDegrees((my - clickedY)*camera.getAngle()/camera.getHeight(), 0, (mx - clickedX)*camera.getAngle()/camera.getWidth());
                	scene.repaint();
                }
            }
        }else{
            if (mouseHandler.isButtonDown(1)){
                if (mouseState != MOUSE_RIGHT_CLICK){
                    mouseState = MOUSE_RIGHT_CLICK;
                    mouseButtonChanged = true;
                }else{
                    vector.set(oldCameraPosition);
                    vector.rotateZYXEuler(oldCameraRotation);
                    cameraRotation.set(oldCameraRotation);
                    cameraRotation.addDegrees((my - clickedY)*camera.getAngle()/camera.getHeight()*5, 0, (mx- clickedX)*camera.getAngle()/camera.getWidth()*5);
                    vector.rotateReverseXYZEuler(cameraRotation);
                    cameraPosition.set(vector);
                	scene.repaint();
                }
            }else{
                if (mouseState != MOUSE_NO_CLICK){
                    mouseState = MOUSE_NO_CLICK;
                    mouseButtonChanged = true;
                }
            }
        }
        if (mouseButtonChanged){
            oldCameraPosition.set(cameraPosition);
            oldCameraRotation.set(cameraRotation);
            clickedX = mx;
            clickedY = my;        	
        }
   	}
}
