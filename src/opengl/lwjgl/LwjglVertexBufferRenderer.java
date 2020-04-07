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

import geometry.Geometry;
import geometry.Vector2f;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;

import opengl.BufferUtils;
import opengl.GlBufferObjectHandler;
import opengl.Material;
import opengl.GlBufferObjectHandler.GlBufferObject;
import opengl.rendering_algorithm.ObjectRenderer;
import scene.Scene;
import scene.SceneObserver.SceneObjectInformation;
import scene.object.SceneObject;
import scene.object.SceneObjectLine;
import scene.object.SceneObjectMesh;
import scene.object.SceneObject.DrawType;
import scene.object.SceneObjectPlane;
import scene.object.SceneObjectPointCloud;
import util.Buffers;

public class LwjglVertexBufferRenderer extends ObjectRenderer{
	private FloatBuffer floatBuffer = Buffers.createFloatBuffer(0);
	private IntBuffer indexData = Buffers.createIntBuffer(0);
	private final GlBufferObjectHandler gboh;
	private final GlBufferObject glBufferObject;
	private static class VertexBufferAttachement{
		private final GlBufferObject arrayBuffer;
		private final GlBufferObject indexBuffer;
		private int drawCount;
		public int[] localResetCount = null;
		
		public int getDrawCount()
		{
			return drawCount;
		}
		
		public int incrementDrawCount()
		{
			return ++drawCount;
		}
		
		
		private VertexBufferAttachement(GlBufferObject arrayBuffer, GlBufferObject indexBuffer)
		{
			this.arrayBuffer = arrayBuffer;
			this.indexBuffer = indexBuffer;
		}
	}
	
	public LwjglVertexBufferRenderer(LwjglBufferObjectHandler gboh) {
		this.gboh = gboh;
		glBufferObject = gboh.createBufferObject();
	}
	
	public void renderQuad(){
		floatBuffer = BufferUtils.fillWithQuad(floatBuffer);
	    glBufferObject.bind(GL15.GL_ARRAY_BUFFER);
	    GL15.glBufferData(GL15.GL_ARRAY_BUFFER, floatBuffer, GL15.GL_STREAM_DRAW);
	    GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
	    GL11.glEnableClientState(GL11.GL_NORMAL_ARRAY);
	    GL11.glVertexPointer(3, GL11.GL_FLOAT, 12, 0);
	    GL11.glNormalPointer(GL11.GL_FLOAT, 12, 0);
		GL11.glDrawArrays(GL11.GL_QUADS, 0, 4);
	    GL11.glDisableClientState(GL11.GL_VERTEX_ARRAY);
	    GL11.glDisableClientState(GL11.GL_NORMAL_ARRAY);
	}
	
	public void drawLines2d(Vector2f vertices[])
	{
		floatBuffer = BufferUtils.fillWithVectorData(vertices, floatBuffer);
	    glBufferObject.bind(GL15.GL_ARRAY_BUFFER);
	    GL15.glBufferData(GL15.GL_ARRAY_BUFFER, floatBuffer, GL15.GL_STREAM_DRAW);
	    GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
	    GL11.glVertexPointer(2, GL11.GL_FLOAT, 8, 0);
		GL11.glDrawArrays(GL11.GL_LINES, 0, vertices.length);
	    GL11.glDisableClientState(GL11.GL_VERTEX_ARRAY);		
	}
	
	public void drawLines2d(float vertices[])
	{
		floatBuffer = BufferUtils.fillWithFloatData(vertices, floatBuffer);
	    glBufferObject.bind(GL15.GL_ARRAY_BUFFER);
	    GL15.glBufferData(GL15.GL_ARRAY_BUFFER, floatBuffer, GL15.GL_STREAM_DRAW);
	    GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
	    GL11.glVertexPointer(2, GL11.GL_FLOAT, 8, 0);
		GL11.glDrawArrays(GL11.GL_LINES, 0, vertices.length);
	    GL11.glDisableClientState(GL11.GL_VERTEX_ARRAY);		
	}
	
	@Override
	public void renderObject(SceneObjectInformation soi, int type) {
		SceneObject obj = soi.getSceneObject();
		if (obj == null)
		{
			return;
		}
		synchronized(obj){
			soi.repoll();
	        Material m = (type == Scene.NORMAL_MAP || type == Scene.REFLECTION_MAP ? obj.getReflectionMaterial() : obj.getLightMaterial());	       
			LwjglGlUtils.bindMaterial(m);
			byte drawType = obj.getDrawType();
			VertexBufferAttachement vba = (VertexBufferAttachement)soi.getAttachedObject();
			boolean updateVertices = soi.objectUpdatedBitmask((1 << SceneObject.UpdateKind.DATA) | (1 << SceneObject.UpdateKind.DIMENSION));
			if (vba == null)
			{
				vba = new VertexBufferAttachement(gboh.createBufferObject(), gboh.createBufferObject());
				soi.attachObject(vba);
				updateVertices = true;
			}
			if (drawType != DrawType.DOTS)
			{
				vba.indexBuffer.bind(GL15.GL_ELEMENT_ARRAY_BUFFER);
			}
			vba.arrayBuffer.bind(GL15.GL_ARRAY_BUFFER);
			if (obj instanceof SceneObjectLine)
	    	{
				SceneObjectLine line = (SceneObjectLine)obj;
				int size = line.getVertexCount();
		    	if (size == 0)
				{
					return;
				}
				boolean activateLater = GL11.glIsEnabled(GL11.GL_LIGHTING);
				if (activateLater)
				{
					GL11.glDisable(GL11.GL_LIGHTING);
				}
				float[] vertices = line.getVertices();
				float[] vertexColor = line.getVertexColor();
				if (vertices.length != size * 3)
				{
					throw new RuntimeException();
				}
				if (updateVertices)
				{
					if (vertexColor != null)
					{
						floatBuffer = BufferUtils.fillWithVertexAndColorData(vertices,vertexColor,floatBuffer);
					}
					else
					{
						floatBuffer = BufferUtils.fillWithFloatData(vertices,floatBuffer);
					}
					GL15.glBufferData(GL15.GL_ARRAY_BUFFER, floatBuffer, GL15.GL_STATIC_DRAW);
				}
			    if (drawType == DrawType.LINES){
		    		if (soi.objectUpdatedBitmask((1 << SceneObject.UpdateKind.DIMENSION) | (1 << SceneObject.UpdateKind.DRAW_TYPE)))
		    		{
		    			indexData = BufferUtils.fillWithLineIndexData(size, indexData);
						GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, indexData, GL15.GL_STATIC_DRAW);
		    		}
					GL11.glEnableClientState(GL11.GL_INDEX_ARRAY);
				}
			    GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
		    	if (vertexColor != null)
	    		{
		    		GL11.glEnableClientState(GL11.GL_COLOR_ARRAY);
		    		GL11.glVertexPointer(3, GL11.GL_FLOAT, 28, 0);
		    		GL11.glColorPointer(4, GL11.GL_FLOAT, 28, 12);
	    		}
		    	else
		    	{
		    		GL11.glVertexPointer(3, GL11.GL_FLOAT, 12, 0);
		    	}
		    	switch (drawType)
				{
				  	case DrawType.DOTS:GL11.glDrawArrays(GL11.GL_POINTS, 0, size);break;
				   	case DrawType.LINES: GL11.glDrawElements(GL11.GL_LINES, size * 2 - 2, GL11.GL_UNSIGNED_INT, 0);break;
				   	case DrawType.LINE_STRICLES:GL11.glDrawArrays(GL11.GL_LINES, 0, size);break;	
				   	default:
				   		break;
				}
		    	GL11.glDisableClientState(GL11.GL_VERTEX_ARRAY);
				if (vertexColor != null)
		    	{
		    		GL11.glDisableClientState(GL11.GL_COLOR_ARRAY);
		    	}
				if (drawType != DrawType.DOTS)
				{
				  	GL11.glDisableClientState(GL11.GL_INDEX_ARRAY);
				}
				if (activateLater)
				{
					GL11.glEnable(GL11.GL_LIGHTING);
				}
			}
			else if (obj instanceof SceneObjectPointCloud)
	    	{
				int currentOffset = 0;
				SceneObjectPointCloud cloud = (SceneObjectPointCloud)obj;
//				if (updateVertices || true)
//				{
					int lineLength = cloud.getVertexLineLength();
					int vertexCount = cloud.getVertexCount();

					int currentBufferSize=GL15.glGetBufferParameteri(GL15.GL_ARRAY_BUFFER, GL15.GL_BUFFER_SIZE);
					if (currentBufferSize != lineLength * vertexCount * 12)
					{
						GL15.glBufferData(GL15.GL_ARRAY_BUFFER, Buffers.createFloatBuffer(lineLength * vertexCount * 3),GL15.GL_STATIC_DRAW);
						indexData = BufferUtils.fillWithLines(vertexCount, lineLength, indexData);
						GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, indexData, GL15.GL_STATIC_DRAW);
					}
					currentOffset = vba.getDrawCount() % lineLength;
					vba.incrementDrawCount();
					floatBuffer = BufferUtils.fillWithFloatData(cloud.getVectorField(),floatBuffer);
					int objectResetCount[] = cloud.getResetCount();
					if ( vba.localResetCount == null ||  vba.localResetCount.length != objectResetCount.length)
					{
						 vba.localResetCount = objectResetCount.clone();
					}
					else
					{
						int localResetCount[] = vba.localResetCount;
						for (int i = 0; i < localResetCount.length; ++i) {
							if (localResetCount[i] != objectResetCount[i])
							{
								localResetCount[i] = objectResetCount[i];
								floatBuffer.put(i * 3, Float.NaN);
								floatBuffer.put(i * 3 + 1, Float.NaN);
								floatBuffer.put(i * 3 + 2, Float.NaN);
							}
						}
					}
					GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, currentOffset * vertexCount * 12, floatBuffer);
//				}
				GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
			    GL11.glVertexPointer(3, GL11.GL_FLOAT, 12, 0);
			    
				switch (drawType)
				{
				  	case DrawType.DOTS:GL11.glDrawArrays(GL11.GL_POINTS, currentOffset * vertexCount, vertexCount);break;	 
				  	case DrawType.LINES:
						GL11.glEnableClientState(GL11.GL_INDEX_ARRAY);
						if (currentOffset != 0)
						{
							GL11.glDrawElements(GL11.GL_LINES, vertexCount * currentOffset * 2, GL11.GL_UNSIGNED_INT, 0);
						}
						if (currentOffset < lineLength - 1)
						{
							GL11.glDrawElements(GL11.GL_LINES, (lineLength - currentOffset - 1) * vertexCount * 2, GL11.GL_UNSIGNED_INT, vertexCount * (currentOffset + 1) * 8);
						}
						GL11.glDisableClientState(GL11.GL_INDEX_ARRAY);break;	
				  		
				  	default:
				   		break;
				}
				
				GL11.glDisableClientState(GL11.GL_VERTEX_ARRAY);
				if (drawType != DrawType.DOTS)
				{
				  	GL11.glDisableClientState(GL11.GL_INDEX_ARRAY);
				}
			}
	    	else if (obj instanceof SceneObjectPlane)
			{
				SceneObjectPlane plane = (SceneObjectPlane)obj;
				final int sizeX = plane.getSizeX();
				final int sizeY = plane.getSizeY();

				if (updateVertices)
				{
					Geometry.calcVertexNormals(
							plane.getSizeX(),
							plane.getSizeY(),
							plane.getVertices(),
							plane.getVertexNormals(),
							plane.isXCyclic(),
							plane.isYCyclic());
					
					/*if (floatBuffer.limit() == plane.getVertexCount() * 6)
					{
						ByteBuffer test = GL15.glMapBuffer(GL15.GL_ARRAY_BUFFER, GL15.GL_WRITE_ONLY, plane.getVertexCount() * 48, null);
						BufferUtils.fillWithVertexAndNormalDataInterleaved(
								plane.getVerticesX(),
								plane.getVerticesY(), 
								plane.getVerticesZ(), 
								plane.getVertexNormals(),
								test.asFloatBuffer());
						GL15.glUnmapBuffer(GL15.GL_ARRAY_BUFFER);
					}
					else
					{*/
						floatBuffer = BufferUtils.fillWithVertexAndNormalDataInterleaved(
								plane.getVerticesX(),
								plane.getVerticesY(), 
								plane.getVerticesZ(), 
								plane.getVertexNormals(),
								floatBuffer);
						GL15.glBufferData(GL15.GL_ARRAY_BUFFER, floatBuffer, GL15.GL_STATIC_DRAW);
					//}
				}
				if (drawType != DrawType.DOTS)
				{
					if ((soi.objectUpdated(SceneObject.UpdateKind.DIMENSION)
					  || soi.objectUpdated(SceneObject.UpdateKind.DRAW_TYPE))){
						if (drawType == DrawType.LINES)
						{
							indexData = BufferUtils.fillWithLineIndexData(sizeX, sizeY, indexData);
						}
						else if (drawType == DrawType.SOLID || drawType == DrawType.SOLID_SMOOTH)
						{
							indexData = BufferUtils.fillWithPlaneIndexData(sizeX, sizeY, indexData);
						}
						GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, indexData, GL15.GL_STATIC_DRAW);
					}
			    	GL11.glEnableClientState(GL11.GL_INDEX_ARRAY);
			    }
			    GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
			    GL11.glEnableClientState(GL11.GL_NORMAL_ARRAY);
			    
			    GL11.glVertexPointer(3, GL11.GL_FLOAT, 24, 0);
			    GL11.glNormalPointer(GL11.GL_FLOAT, 24, 12);
			    switch (drawType)
			    {
			    	case DrawType.DOTS:GL11.glDrawArrays(GL11.GL_POINTS, 0, sizeX * sizeY);break;	 
			    	case DrawType.LINES: GL11.glDrawElements(GL11.GL_LINES, (sizeX * sizeY * 2 - sizeX - sizeY) * 2, GL11.GL_UNSIGNED_INT, 0);break;
			    	case DrawType.SOLID:
			    	case DrawType.SOLID_SMOOTH:GL11.glDrawElements(GL11.GL_QUADS, (sizeX - 1) * (sizeY - 1) * 4, GL11.GL_UNSIGNED_INT, 0);break;
			    	case DrawType.LINE_STRICLES:
					break;
				default:
					break;
			    }
			    
			    GL11.glDisableClientState(GL11.GL_VERTEX_ARRAY);
			    GL11.glDisableClientState(GL11.GL_NORMAL_ARRAY);
			    
			    if (drawType != DrawType.DOTS)
			    {
			    	GL11.glDisableClientState(GL11.GL_INDEX_ARRAY);
			    }
			}
	    	else if (obj instanceof SceneObjectMesh)
	    	{
	    		SceneObjectMesh mesh = (SceneObjectMesh)obj;

	    		float vertices[] = mesh.getVertices();
	    		int faces[] = mesh.getFaces();
	    		float normals[] = mesh.getNormals();
	    		if (updateVertices)
				{
	    			switch (mesh.getType())
	    			{
	    				case SceneObjectMesh.TRIANGLE:Geometry.calcTriangleMeshVertexNormals(vertices,faces,normals);break;
	    				case SceneObjectMesh.QUAD:Geometry.calcQuadMeshVertexNormals(vertices,faces,normals);break;
	    				default: throw new IllegalArgumentException("Unknown type: " + mesh.getType());
	    			}
	    			floatBuffer = BufferUtils.fillWithVertexAndNormalDataInterleaved(vertices, normals, floatBuffer);
					GL15.glBufferData(GL15.GL_ARRAY_BUFFER, floatBuffer, GL15.GL_STATIC_DRAW);
				}
				if (drawType != DrawType.DOTS)
				{
					if (soi.objectUpdatedBitmask((1 << SceneObject.UpdateKind.DIMENSION) | (1 << SceneObject.UpdateKind.DRAW_TYPE))){
						if (drawType == DrawType.LINES || drawType == DrawType.SOLID || drawType == DrawType.SOLID_SMOOTH)
						{
							indexData = BufferUtils.fillWithIntData(faces, indexData);
						}
						GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, indexData, GL15.GL_STATIC_DRAW);
					}
			    	GL11.glEnableClientState(GL11.GL_INDEX_ARRAY);
			    }
			    GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
			    GL11.glEnableClientState(GL11.GL_NORMAL_ARRAY);
			    
			    GL11.glVertexPointer(3, GL11.GL_FLOAT, 24, 0);
			    GL11.glNormalPointer(GL11.GL_FLOAT, 24, 12);
			    switch (drawType)
			    {
			    	case DrawType.DOTS:GL11.glDrawArrays(GL11.GL_POINTS, 0, vertices.length * 3);break;	 
			    	case DrawType.LINES: GL11.glDrawElements(GL11.GL_LINES, faces.length, GL11.GL_UNSIGNED_INT, 0);break;
			    	case DrawType.SOLID:
			    	case DrawType.SOLID_SMOOTH:GL11.glDrawElements(mesh.getType() == SceneObjectMesh.TRIANGLE ? GL11.GL_TRIANGLES : GL11.GL_QUADS, faces.length, GL11.GL_UNSIGNED_INT, 0);break;
			    	case DrawType.LINE_STRICLES:
					break;
				default:
					break;
			    }
			    
			    GL11.glDisableClientState(GL11.GL_VERTEX_ARRAY);
			    GL11.glDisableClientState(GL11.GL_NORMAL_ARRAY);
			    
			    if (drawType != DrawType.DOTS)
			    {
			    	GL11.glDisableClientState(GL11.GL_INDEX_ARRAY);
			    }
			}


		}
	}

	@Override
	public final void poll() 
	{
		gboh.poll();
	}
}
