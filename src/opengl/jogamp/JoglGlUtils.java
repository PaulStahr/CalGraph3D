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

import geometry.Rotation3;
import opengl.Camera;
import opengl.Light;
import opengl.LightGroup;
import opengl.Material;
import opengl.rendering_algorithm.ObjectRenderer;
import scene.SceneObserver;
import scene.SceneObserver.SceneObjectInformation;
import scene.object.SceneObject;

import java.awt.Color;
import java.nio.FloatBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jogamp.opengl.GL2;


public class JoglGlUtils {
	private static final float BYTE_TO_FLOAT = 1f / 255f;
	private static final Logger logger = LoggerFactory.getLogger(JoglGlUtils.class);
    private static final int GL_LIGHTS[] = {GL2.GL_LIGHT0, GL2.GL_LIGHT1, GL2.GL_LIGHT2, GL2.GL_LIGHT3, GL2.GL_LIGHT4, GL2.GL_LIGHT5, GL2.GL_LIGHT6, GL2.GL_LIGHT7};
    public static final void rotateEulerZYX(Rotation3 rotation, GL2 gl){
    	gl.glRotatef(rotation.getZDegrees(),0.0f,0.0f,1.0f);     
    	gl.glRotatef(rotation.getYDegrees(),0.0f,1.0f,0.0f);
    	gl.glRotatef(rotation.getXDegrees(),1.0f,0.0f,0.0f);    	
    }

	public static final void rotateEulerXYZ(Rotation3 rotation, GL2 gl) {
		gl.glRotatef(rotation.getXDegrees(),1.0f,0.0f,0.0f);
		gl.glRotatef(rotation.getYDegrees(),0.0f,1.0f,0.0f);
        gl.glRotatef(rotation.getZDegrees(),0.0f,0.0f,1.0f);
	}
	
	public static final void rotateReverseEulerXYZ(Rotation3 rotation, GL2 gl) {
		gl.glRotatef(-rotation.getXDegrees(),1.0f,0.0f,0.0f);
		gl.glRotatef(-rotation.getYDegrees(),0.0f,1.0f,0.0f);
		gl.glRotatef(-rotation.getZDegrees(),0.0f,0.0f,1.0f); 	
    }
	
	public static final void setEnabledGlPositions(LightGroup lg, GL2 gl){
	    for (int i=0;i<lg.size();i++)
	    	if (lg.get(i).enabled)
	            gl.glLightfv(GL_LIGHTS[i],GL2.GL_POSITION,lg.get(i).positionReadOnly);
		
	}
	
	public static final void setGlPositions(LightGroup lg, GL2 gl){
	    for (int i=0;i<lg.size();i++)
            gl.glLightfv(GL_LIGHTS[i],GL2.GL_POSITION,lg.get(i).positionReadOnly);
	}
	
	public static final void setLights(LightGroup lg, GL2 gl)
	{
		for (int i = 0; i < lg.size(); ++i)
		{
			Light l = lg.get(i);
			int id = GL_LIGHTS[i];
            l.attenuation = GL2.GL_CONSTANT_ATTENUATION;
            gl.glLightfv(id,GL2.GL_AMBIENT,l.ambientReadOnly);
            gl.glLightfv(id,GL2.GL_DIFFUSE,l.diffuseReadOnly);
            gl.glLightfv(id,GL2.GL_SPECULAR,l.specularReadOnly);
            gl.glLightf(id, l.attenuation, l.fallOff);
            if (l.enabled)
            	gl.glEnable(id);
            else
            	gl.glDisable(id);
		}
	}

    public static final void reSizeGLScene(Camera camera, GL2 gl) {
        gl.glViewport(0,0,camera.getWidth(),camera.getHeight());
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glLoadIdentity();
        perspectiveGL(camera.getAngle(),camera.getAspect(),0.01,10000.0, gl);
        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glLoadIdentity();
    }

    public static final void perspectiveGL(double fovY, double aspect, double zNear, double zFar, GL2 gl){
        final double fH = Math.tan(fovY*(Math.PI/360)) * zNear;
        final double fW = fH * aspect;  
        gl.glFrustum(-fW, fW, -fH, fH, zNear, zFar);
    }
	
	public static final void renderGlObjects(SceneObserver scene, boolean with_normals, ObjectRenderer objectRenderer, int environmentType, GL2 gl)
	{
		boolean alpha_objects = false;
        for (int i = 0; i < scene.getSceneObjectCount(); ++i)
        {
        	SceneObjectInformation soi = scene.getSceneObjectInfo(i);
        	SceneObject obj = soi.getSceneObject();
        	if (obj != null && obj.isVisible() && (obj.hasNormals() == with_normals))
        	{
        		try
        		{
        			if (obj.lightMaterial.diffuseReadOnly.get(3) < 1 && with_normals)
            		{
            			alpha_objects = true;
            		}
        			else
        			{
        				objectRenderer.renderObject(soi, environmentType);
        			}
        		}catch (Exception e)
        		{
        			logger.error("Error at rendering object " + obj.getId(), e);
        		}
    		}
        }
        if (alpha_objects)
        {
        	gl.glDepthMask(false);
    		gl.glEnable(GL2.GL_BLEND);
    	    for (int i = 0; i < scene.getSceneObjectCount(); ++i)
            {
            	SceneObjectInformation soi = scene.getSceneObjectInfo(i);
            	SceneObject obj = soi.getSceneObject();
            	if (obj != null && obj.isVisible() && (obj.hasNormals() == with_normals))
            	{
            		try
            		{
            			if (soi.getSceneObject().lightMaterial.diffuseReadOnly.get(3) < 1)
                		{
            				objectRenderer.renderObject(soi, environmentType);
            			}
            		}catch (Exception e)
            		{
            			logger.error("Error at rendering object", e);
            		}
        		}
            }
        	gl.glDepthMask(true);
    		gl.glDisable(GL2.GL_BLEND);        	
        }
	}
	
	public static final void setMaterialColor(boolean ambient, boolean diffuse, boolean specular, boolean emission, FloatBuffer color, GL2 gl)
	{
		if (ambient)
		{
			gl.glMaterialfv(GL2.GL_FRONT_AND_BACK, GL2.GL_AMBIENT, color);
		}
		if (diffuse)
		{
			gl.glMaterialfv(GL2.GL_FRONT_AND_BACK, GL2.GL_DIFFUSE, color);			
		}
		if (specular)
		{
			gl.glMaterialfv(GL2.GL_FRONT_AND_BACK, GL2.GL_SPECULAR, color);
		}
		if (emission)
		{
			gl.glMaterialfv(GL2.GL_FRONT_AND_BACK, GL2.GL_EMISSION, color);
		}
	}
	
	public static final void bindMaterial(Material m, GL2 gl)
    {
        gl.glMaterialfv(GL2.GL_FRONT_AND_BACK, GL2.GL_AMBIENT, 	m.ambientReadOnly);
        gl.glMaterialfv(GL2.GL_FRONT_AND_BACK, GL2.GL_DIFFUSE, 	m.diffuseReadOnly);
        gl.glMaterialfv(GL2.GL_FRONT_AND_BACK, GL2.GL_SPECULAR, m.specularReadOnly);
        gl.glMaterialfv(GL2.GL_FRONT_AND_BACK, GL2.GL_EMISSION, m.emissionReadOnly);
        gl.glMaterialf(GL2.GL_FRONT_AND_BACK, GL2.GL_SHININESS, m.Ns);
        if (m.texture != null)
            m.texture.bind();
    }
	
	public static final void colorC(Color c, GL2 gl){
		gl.glColor4f(c.getRed() * BYTE_TO_FLOAT, c.getGreen() * BYTE_TO_FLOAT, c.getBlue() * BYTE_TO_FLOAT, c.getAlpha() * BYTE_TO_FLOAT);
	}
}
