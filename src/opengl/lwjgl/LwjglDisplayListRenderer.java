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

import org.lwjgl.opengl.GL11;

import geometry.Vector2f;
import geometry.Vector3f;
import jcomponents.Interface;
import opengl.rendering_algorithm.ObjectRenderer;
import opengl.Material;
import opengl.lwjgl.LwjglListHandler.GlList;
import scene.Scene;
import scene.SceneObserver.SceneObjectInformation;
import scene.object.SceneObject;
import scene.object.SceneObjectLine;
import scene.object.SceneObjectPlane;
import scene.object.SceneObjectVektor;
import scene.object.SceneObject.DrawType;

public class LwjglDisplayListRenderer extends ObjectRenderer {
	private final LwjglListHandler glListHandler;
	public LwjglDisplayListRenderer() {
		this.glListHandler = new LwjglListHandler(100);
	}
	
	public void removeObject(SceneObjectInformation soi)
	{
		GlList glList = (GlList) soi.getAttachedObject();
		if (glList != null)
		{
			glList.destroy();
		}
	}
	
	public void renderQuad()
	{
		final float length = 0.69336127435063470484335227478596f;
	    GL11.glBegin(GL11.GL_QUADS);
	    for (int i=0;i<4;i++){
	    	final float x = i==1 || i==2 ? -length : length, y = i>=2 ? -length : length;
	        GL11.glNormal3f(x, y, -length);
	        GL11.glVertex3f(x, y, -length);
	    }
	    GL11.glEnd();
	}
	
	public void drawLines2d(Vector2f[] vertices) {
	     GL11.glBegin(GL11.GL_LINES);
	     for (Vector2f v : vertices){
        	 GL11.glVertex2f(v.x, v.y);
	     }
	     GL11.glEnd();
	}
	
	public void drawLines2d(float[] vertices) {
	     GL11.glBegin(GL11.GL_LINES);
	     for (int i = 0; i < vertices.length; i += 2){
        	 GL11.glVertex2f(vertices[i], vertices[i+1]);
	     }
	     GL11.glEnd();
	}
	
	@Override
	public void renderObject(SceneObjectInformation soi, int type) {
		SceneObject obj = soi.getSceneObject();
		if (obj == null)
		{
			return;
		}
		//soi.poll();
        Material m = (type == Scene.NORMAL_MAP || type == Scene.REFLECTION_MAP ? obj.getReflectionMaterial() : obj.getLightMaterial());

		LwjglGlUtils.bindMaterial(m);
		GlList glList = null;
		if (obj.isCachable())
		{
	    	glList = (GlList) soi.getAttachedObject();
			if (glList == null)
			{
				glList = glListHandler.getFreeList();
				glList.setProtected(true);
				soi.attachObject(glList);
			}
			else
			{
				glList.setProtected(true);
				if (!soi.objectUpdated(SceneObject.UpdateKind.DATA) && glList.call()){
	    			glList.setProtected(false);
	    			return;
				}
			}
			glList.startRecord(GL11.GL_COMPILE);
		}
		if (obj instanceof SceneObjectPlane)
		{
			SceneObjectPlane plane = (SceneObjectPlane)obj;
			RenderGlObjectImmediateMode.render(plane.getSizeX(),
								  plane.getSizeY(),
								  plane.getVertices(),
								  plane.getVertexNormals(),
								  plane.getFaceNormals(),
								  plane.getPermutation(),
								  plane.getDistance(),
								  obj.getDrawType(),
								  Interface.scene.cameraPosition,
								  plane.usePermutation,
								  plane.updatePermutation,
								  plane.isXCyclic(),
								  plane.isYCyclic());
		}
		else if (obj instanceof SceneObjectLine)
		{
			SceneObjectLine line = (SceneObjectLine)obj;
			if (line.size() == 0)
	            return;
	        int openGlDrawMode = -1;
	        switch (obj.getDrawType())
	        {
	        	case DrawType.DOTS:
	        		openGlDrawMode = GL11.GL_POINTS;
	        		break;
	        	case DrawType.LINES:
	        		openGlDrawMode = GL11.GL_LINE_STRIP;
	        		break;
	        	case DrawType.LINE_STRICLES:
	        		openGlDrawMode = GL11.GL_LINES;
	        		break;
	        	default:
	        		return;
	        }
	        GL11.glNormal3f(0.0f, 0.0f, 1.0f);
		    GL11.glBegin(openGlDrawMode);
        	RenderGlObjectImmediateMode.callPoints(line.getVertices());
            GL11.glEnd();		
		}
		else if (obj instanceof SceneObjectVektor)
		{
			SceneObjectVektor vektor = (SceneObjectVektor)obj;
	        GL11.glNormal3f(0.0f, 0.0f, 1.0f);
	        Vector3f dif = new Vector3f(vektor.end);
	        dif.sub(vektor.start);
	        GL11.glBegin(GL11.GL_LINES);
	        GL11.glVertex3f(vektor.end.x, vektor.start.y, vektor.start.z);
	        GL11.glVertex3f(vektor.end.x, vektor.end.y, vektor.end.z);
	        GL11.glVertex3f(vektor.end.x-(dif.y+dif.x)/10, vektor.end.y-(dif.y-dif.x)/10, vektor.end.z-dif.z/10);
	        GL11.glVertex3f(vektor.end.x, vektor.end.y, vektor.end.z);
	        GL11.glVertex3f(vektor.end.x-(dif.x-dif.y)/10, vektor.end.y-(dif.x+dif.y)/10, vektor.end.z-dif.z/10);
	        GL11.glVertex3f(vektor.end.x, vektor.end.y, vektor.end.z);
	        GL11.glEnd();
		}
		if (glList != null)
		{
			glList.stopRecord();
			glList.call();
			glList.setProtected(false);
		}
	}

	@Override
	public void poll() {
		glListHandler.removeUnused();
	}	
}
