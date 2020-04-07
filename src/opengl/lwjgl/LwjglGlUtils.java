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

import geometry.Rotation3;
import geometry.Vector3f;
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

import org.lwjgl.opengl.GL11;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LwjglGlUtils {
	private static final Logger logger = LoggerFactory.getLogger(LwjglGlUtils.class);
	private static final float BYTE_TO_FLOAT = 1f / 255f;
	private static final int GL_LIGHTS[] = {GL11.GL_LIGHT0, GL11.GL_LIGHT1, GL11.GL_LIGHT2, GL11.GL_LIGHT3, GL11.GL_LIGHT4, GL11.GL_LIGHT5, GL11.GL_LIGHT6, GL11.GL_LIGHT7};
    public static final void rotateEulerZYX(Rotation3 rotation){
        GL11.glRotatef(rotation.getZDegrees(),0.0f,0.0f,1.0f);     
        GL11.glRotatef(rotation.getYDegrees(),0.0f,1.0f,0.0f);
        GL11.glRotatef(rotation.getXDegrees(),1.0f,0.0f,0.0f);    	
    }
    
    public static final void setLocation(byte stereoskopiePos, float stereoSkopieRotation, float stereoSkopieMoved, Vector3f position, Rotation3 rotation){
        GL11.glLoadIdentity();
        if (stereoskopiePos != Camera.STEREOSCOPIE_MIDDLE){
            GL11.glTranslatef (stereoskopiePos * stereoSkopieRotation, 0.0f, 0.0f);
            GL11.glRotatef(stereoskopiePos * stereoSkopieMoved,0.0f,1.0f,0.0f);
        }
        LwjglGlUtils.rotateReverseEulerXYZ(rotation);

        GL11.glTranslatef(-position.x ,-position.y,-position.z);
    }

	public static final void rotateEulerXYZ(Rotation3 rotation) {
        GL11.glRotatef(rotation.getXDegrees(),1.0f,0.0f,0.0f);
        GL11.glRotatef(rotation.getYDegrees(),0.0f,1.0f,0.0f);
        GL11.glRotatef(rotation.getZDegrees(),0.0f,0.0f,1.0f);
	}
	
	public static final void rotateReverseEulerXYZ(Rotation3 rotation) {
		GL11.glRotatef(-rotation.getXDegrees(),1.0f,0.0f,0.0f);
        GL11.glRotatef(-rotation.getYDegrees(),0.0f,1.0f,0.0f);
        GL11.glRotatef(-rotation.getZDegrees(),0.0f,0.0f,1.0f); 	
    }
	
	public static final void setEnabledGlPositions(LightGroup lg){
	    for (int i=0;i<lg.size();i++)
	    	if (lg.get(i).enabled)
	            GL11.glLight(GL_LIGHTS[i],GL11.GL_POSITION,lg.get(i).positionReadOnly);
		
	}
	
	public static final void setGlPositions(LightGroup lg){
	    for (int i=0;i<lg.size();i++)
            GL11.glLight(GL_LIGHTS[i],GL11.GL_POSITION,lg.get(i).positionReadOnly);
	}
	
	public static final void setLights(LightGroup lg)
	{
		for (int i = 0; i < lg.size(); ++i)
		{
			Light l = lg.get(i);
			int id = GL_LIGHTS[i];
            l.attenuation = GL11.GL_CONSTANT_ATTENUATION;
            GL11.glLight(id,GL11.GL_AMBIENT,l.ambientReadOnly);
            GL11.glLight(id,GL11.GL_DIFFUSE,l.diffuseReadOnly);
            GL11.glLight(id,GL11.GL_SPECULAR,l.specularReadOnly);
            GL11.glLightf(id, l.attenuation, l.fallOff);
            if (l.enabled)
            	GL11.glEnable(id);
            else
            	GL11.glDisable(id);
		}
	}
	
    public static final void reSizeGLScene(Camera camera) {
        GL11.glViewport(0,0,camera.getWidth(),camera.getHeight());
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();
        LwjglGlUtils.perspectiveGL(camera.getAngle(),camera.getAspect(),0.01,10000.0);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glLoadIdentity();
    }

    public static final void perspectiveGL(double fovY, double aspect, double zNear, double zFar){
        final double fH = Math.tan(fovY*(Math.PI/360)) * zNear;
        final double fW = fH * aspect;  
        GL11.glFrustum(-fW, fW, -fH, fH, zNear, zFar);
    }
	
	public static final void renderGlObjects(SceneObserver scene, boolean with_normals, ObjectRenderer objectRenderer, int environmentType)
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
        			if (soi.getSceneObject().lightMaterial.diffuseReadOnly.get(3) < 1 && with_normals)
            		{
            			alpha_objects = true;
            		}
        			else
        			{
        				objectRenderer.renderObject(soi, environmentType);
        			}
        		}catch (Exception e)
        		{
        			logger.error("Error at rendering object", e);
        		}
    		}
        }
        if (alpha_objects)
        {
        	GL11.glDepthMask(false);
    		GL11.glEnable(GL11.GL_BLEND);
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
        	GL11.glDepthMask(true);
    		GL11.glDisable(GL11.GL_BLEND);        	
        }
	}
	
	public static final void setMaterialColor(boolean ambient, boolean diffuse, boolean specular, boolean emission, FloatBuffer color)
	{
		if (ambient)
		{
			GL11.glMaterial(GL11.GL_FRONT_AND_BACK, GL11.GL_AMBIENT, color);
		}
		if (diffuse)
		{
	        GL11.glMaterial(GL11.GL_FRONT_AND_BACK, GL11.GL_DIFFUSE, color);			
		}
		if (specular)
		{
			GL11.glMaterial(GL11.GL_FRONT_AND_BACK, GL11.GL_SPECULAR, color);
		}
		if (emission)
		{
			GL11.glMaterial(GL11.GL_FRONT_AND_BACK, GL11.GL_EMISSION, color);
		}
	}
	
	public static final void bindMaterial(Material m)
    {
        GL11.glMaterial(GL11.GL_FRONT_AND_BACK, GL11.GL_AMBIENT, 	m.ambientReadOnly);
        GL11.glMaterial(GL11.GL_FRONT_AND_BACK, GL11.GL_DIFFUSE, 	m.diffuseReadOnly);
        GL11.glMaterial(GL11.GL_FRONT_AND_BACK, GL11.GL_SPECULAR, m.specularReadOnly);
        GL11.glMaterial(GL11.GL_FRONT_AND_BACK, GL11.GL_EMISSION, m.emissionReadOnly);
        GL11.glMaterialf(GL11.GL_FRONT_AND_BACK, GL11.GL_SHININESS, m.Ns);
        if (m.texture != null)
            m.texture.bind();
    }
	
	public static final void colorC(Color c){
		GL11.glColor4f(c.getRed() * BYTE_TO_FLOAT, c.getGreen() * BYTE_TO_FLOAT, c.getBlue() * BYTE_TO_FLOAT, c.getAlpha() * BYTE_TO_FLOAT);
	}
	

}
