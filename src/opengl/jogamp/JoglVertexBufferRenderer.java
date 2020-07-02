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

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import com.jogamp.opengl.GL2;

import geometry.Geometry;
import geometry.Vector2f;
import opengl.BufferUtils;
import opengl.GlBufferObjectHandler;
import opengl.GlBufferObjectHandler.GlBufferObject;
import opengl.Material;
import opengl.rendering_algorithm.ObjectRenderer;
import scene.Scene;
import scene.SceneObserver.SceneObjectInformation;
import scene.object.SceneObject;
import scene.object.SceneObject.DrawType;
import scene.object.SceneObjectLine;
import scene.object.SceneObjectMesh;
import scene.object.SceneObjectPlane;
import scene.object.SceneObjectPointCloud;
import util.Buffers;

public class JoglVertexBufferRenderer extends ObjectRenderer{
	private FloatBuffer floatBuffer = Buffers.createFloatBuffer(12);
	private IntBuffer indexData = Buffers.createIntBuffer(0);
	private IntBuffer idBuffer = Buffers.createIntBuffer(1);
	private final GlBufferObjectHandler gboh;
	private final GlBufferObject glBufferObject;
	private final GL2 gl;
	private static class VertexBufferAttachement{
		private final GlBufferObject arrayBuffer;
		private final GlBufferObject indexBuffer;
		private int drawCount;
		public int[] localResetCount = null;
		
		private VertexBufferAttachement(GlBufferObject arrayBuffer, GlBufferObject indexBuffer)
		{
			this.arrayBuffer = arrayBuffer;
			this.indexBuffer = indexBuffer;
		}

		public int getDrawCount() {
			return drawCount;
		}
		
		public int incrementDrawCount()
		{
			return ++drawCount;
		}
	}
	
	public JoglVertexBufferRenderer(GL2 gl_, GlBufferObjectHandler gboh) {
		this.gl = gl_;
		this.gboh = gboh;
		glBufferObject = gboh.createBufferObject();
	}
	
	@Override
	public void renderQuad(){
   		floatBuffer = BufferUtils.fillWithQuad(floatBuffer);
        //TODO Weird size bug
	    glBufferObject.bind(GL2.GL_ARRAY_BUFFER);
	    gl.glBufferData(GL2.GL_ARRAY_BUFFER, floatBuffer.limit() * 4, floatBuffer, GL2.GL_STATIC_DRAW);
	    gl.glEnableClientState(GL2.GL_VERTEX_ARRAY);
	    gl.glEnableClientState(GL2.GL_NORMAL_ARRAY);
	    gl.glVertexPointer(3, GL2.GL_FLOAT, 12, 0);
	    gl.glNormalPointer(GL2.GL_FLOAT, 12, 0);
	    gl.glDrawArrays(GL2.GL_QUADS, 0, 4);
	    gl.glDisableClientState(GL2.GL_VERTEX_ARRAY);
	    gl.glDisableClientState(GL2.GL_NORMAL_ARRAY);
	}
	
	@Override
	public void drawLines2d(Vector2f vertices[])
	{
		floatBuffer = BufferUtils.fillWithVectorData(vertices, floatBuffer);
	    glBufferObject.bind(GL2.GL_ARRAY_BUFFER);
	    gl.glBufferData(GL2.GL_ARRAY_BUFFER, floatBuffer.limit() * 4, floatBuffer, GL2.GL_STATIC_DRAW);
	    gl.glEnableClientState(GL2.GL_VERTEX_ARRAY);
	    gl.glVertexPointer(2, GL2.GL_FLOAT, 8, 0);
	    gl.glDrawArrays(GL2.GL_LINES, 0, vertices.length);
	    gl.glDisableClientState(GL2.GL_VERTEX_ARRAY);		
	}

	@Override
	public void drawLines2d(float vertices[])
	{
		floatBuffer = BufferUtils.fillWithFloatData(vertices, floatBuffer);
	    glBufferObject.bind(GL2.GL_ARRAY_BUFFER);
	    gl.glBufferData(GL2.GL_ARRAY_BUFFER, floatBuffer.limit() * 4, floatBuffer, GL2.GL_STATIC_DRAW);
	    gl.glEnableClientState(GL2.GL_VERTEX_ARRAY);
	    gl.glVertexPointer(2, GL2.GL_FLOAT, 8, 0);
	    gl.glDrawArrays(GL2.GL_LINES, 0, vertices.length);
	    gl.glDisableClientState(GL2.GL_VERTEX_ARRAY);		
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
			JoglGlUtils.bindMaterial(m, gl);
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
				vba.indexBuffer.bind(GL2.GL_ELEMENT_ARRAY_BUFFER);
			}
			vba.arrayBuffer.bind(GL2.GL_ARRAY_BUFFER);
			if (obj instanceof SceneObjectLine)
	    	{
				SceneObjectLine line = (SceneObjectLine)obj;
				int size = line.getVertexCount();
				if (size == 0)
				{
					return;
				}
				boolean activateLater = gl.glIsEnabled(GL2.GL_LIGHTING);
				if (activateLater)
				{
					gl.glDisable(GL2.GL_LIGHTING);
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
					gl.glBufferData(GL2.GL_ARRAY_BUFFER, floatBuffer.limit() * 4, floatBuffer, GL2.GL_STATIC_DRAW);
				}
		    	if (drawType == DrawType.LINES){
		    		if (soi.objectUpdatedBitmask((1 << SceneObject.UpdateKind.DIMENSION) | (1 << SceneObject.UpdateKind.DRAW_TYPE)))
		    		{
		    			indexData = BufferUtils.fillWithLineIndexData(size, indexData);
						gl.glBufferData(GL2.GL_ELEMENT_ARRAY_BUFFER, indexData.limit() * 4, indexData, GL2.GL_STATIC_DRAW);
		    		}
		    		gl.glEnableClientState(GL2.GL_INDEX_ARRAY);
				}
		    	gl.glEnableClientState(GL2.GL_VERTEX_ARRAY);
		    	if (vertexColor != null)
	    		{
		    		gl.glEnableClientState(GL2.GL_COLOR_ARRAY);
		    		gl.glVertexPointer(3, GL2.GL_FLOAT, 28, 0);
		    		gl.glColorPointer(4, GL2.GL_FLOAT, 28, 12);
	    		}
		    	else
		    	{
		    		gl.glVertexPointer(3, GL2.GL_FLOAT, 12, 0);
		    	}
	    		switch (drawType)
				{
				  	case DrawType.DOTS:gl.glDrawArrays(GL2.GL_POINTS, 0, size);break;	 
				   	case DrawType.LINES: gl.glDrawElements(GL2.GL_LINES, size * 2 - 2, GL2.GL_UNSIGNED_INT, 0);break;
				   	case DrawType.LINE_STRICLES:gl.glDrawArrays(GL2.GL_LINES, 0, size);break;	
				   	default:
				   		break;
				}
				gl.glDisableClientState(GL2.GL_VERTEX_ARRAY);
				if (vertexColor != null)
				{
					gl.glDisableClientState(GL2.GL_COLOR_ARRAY);
				}
				if (drawType != DrawType.DOTS)
				{
					gl.glDisableClientState(GL2.GL_INDEX_ARRAY);
				}
				if (activateLater)
				{
					gl.glEnable(GL2.GL_LIGHTING);
				}
			}
			if (obj instanceof SceneObjectPointCloud)
	    	{
				int currentOffset = 0;
				SceneObjectPointCloud cloud = (SceneObjectPointCloud)obj;
//				if (updateVertices || true)
//				{
					int lineLength = cloud.getVertexLineLength();
					int vertexCount = cloud.getVertexCount();
					gl.glGetBufferParameteriv(GL2.GL_ARRAY_BUFFER, GL2.GL_BUFFER_SIZE, idBuffer);
					int currentBufferSize = idBuffer.get(0);
					if (currentBufferSize != lineLength * vertexCount * 12)
					{
						gl.glBufferData(GL2.GL_ARRAY_BUFFER, lineLength * vertexCount * 12, Buffers.createFloatBuffer(lineLength * vertexCount * 3),GL2.GL_STATIC_DRAW);
						indexData = BufferUtils.fillWithLines(vertexCount, lineLength, indexData);
						gl.glBufferData(GL2.GL_ELEMENT_ARRAY_BUFFER, indexData.limit(), indexData, GL2.GL_STATIC_DRAW);
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
					gl.glBufferSubData(GL2.GL_ARRAY_BUFFER, currentOffset * vertexCount * 12, floatBuffer.limit(), floatBuffer);
//				}
					gl.glEnableClientState(GL2.GL_VERTEX_ARRAY);
				gl.glVertexPointer(3, GL2.GL_FLOAT, 12, 0);
			    
				switch (drawType)
				{
				  	case DrawType.DOTS:gl.glDrawArrays(GL2.GL_POINTS, currentOffset * vertexCount, vertexCount);break;	 
				  	case DrawType.LINES:
						gl.glEnableClientState(GL2.GL_INDEX_ARRAY);
						if (currentOffset != 0)
						{
							gl.glDrawElements(GL2.GL_LINES, vertexCount * currentOffset * 2, GL2.GL_UNSIGNED_INT, 0);
						}
						if (currentOffset < lineLength - 1)
						{
							gl.glDrawElements(GL2.GL_LINES, (lineLength - currentOffset - 1) * vertexCount * 2, GL2.GL_UNSIGNED_INT, vertexCount * (currentOffset + 1) * 8);
						}
						gl.glDisableClientState(GL2.GL_INDEX_ARRAY);break;	
				  		
				  	default:
				   		break;
				}
				
				gl.glDisableClientState(GL2.GL_VERTEX_ARRAY);
				if (drawType != DrawType.DOTS)
				{
				  	gl.glDisableClientState(GL2.GL_INDEX_ARRAY);
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
					floatBuffer = BufferUtils.fillWithVertexAndNormalDataInterleaved(
							plane.getVerticesX(),
							plane.getVerticesY(), 
							plane.getVerticesZ(), 
							plane.getVertexNormals(),
							floatBuffer);
					gl.glBufferData(GL2.GL_ARRAY_BUFFER, floatBuffer.limit() * 4, floatBuffer, GL2.GL_STATIC_DRAW);
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
						gl.glBufferData(GL2.GL_ELEMENT_ARRAY_BUFFER, indexData.limit() * 4, indexData, GL2.GL_STATIC_DRAW);
					}
			    	gl.glEnableClientState(GL2.GL_INDEX_ARRAY);
			    }
			    gl.glEnableClientState(GL2.GL_VERTEX_ARRAY);
			    gl.glEnableClientState(GL2.GL_NORMAL_ARRAY);
			    
			    gl.glVertexPointer(3, GL2.GL_FLOAT, 24, 0);
			    gl.glNormalPointer(GL2.GL_FLOAT, 24, 12);
			    switch (drawType)
			    {
			    	case DrawType.DOTS:gl.glDrawArrays(GL2.GL_POINTS, 0, sizeX * sizeY);break;	 
			    	case DrawType.LINES: gl.glDrawElements(GL2.GL_LINES, (sizeX * sizeY * 2 - sizeX - sizeY) * 2, GL2.GL_UNSIGNED_INT, 0);break;
			    	case DrawType.SOLID:
			    	case DrawType.SOLID_SMOOTH:gl.glDrawElements(GL2.GL_QUADS, (sizeX - 1) * (sizeY - 1) * 4, GL2.GL_UNSIGNED_INT, 0);break;
			    	case DrawType.LINE_STRICLES:
					break;
				default:
					break;
			    }
			    
			    gl.glDisableClientState(GL2.GL_VERTEX_ARRAY);
			    gl.glDisableClientState(GL2.GL_NORMAL_ARRAY);
			    
			    if (drawType != DrawType.DOTS)
			    {
			    	gl.glDisableClientState(GL2.GL_INDEX_ARRAY);
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
					gl.glBufferData(GL2.GL_ARRAY_BUFFER, floatBuffer.limit() * 4, floatBuffer, GL2.GL_STATIC_DRAW);
				}
				if (drawType != DrawType.DOTS)
				{
					if (soi.objectUpdatedBitmask((1 << SceneObject.UpdateKind.DIMENSION) | (1 << SceneObject.UpdateKind.DRAW_TYPE))){
						if (drawType == DrawType.LINES || drawType == DrawType.SOLID || drawType == DrawType.SOLID_SMOOTH)
						{
							indexData = BufferUtils.fillWithIntData(mesh.getFaces(), indexData);
						}
						gl.glBufferData(GL2.GL_ELEMENT_ARRAY_BUFFER, indexData.limit() * 4, indexData, GL2.GL_STATIC_DRAW);
					}
					gl.glEnableClientState(GL2.GL_INDEX_ARRAY);
			    }
				gl.glEnableClientState(GL2.GL_VERTEX_ARRAY);
				gl.glEnableClientState(GL2.GL_NORMAL_ARRAY);
			    
			    gl.glVertexPointer(3, GL2.GL_FLOAT, 24, 0);
			    gl.glNormalPointer(GL2.GL_FLOAT, 24, 12);
			    switch (drawType)
			    {
			    	case DrawType.DOTS:gl.glDrawArrays(GL2.GL_POINTS, 0, vertices.length * 3);break;	 
			    	case DrawType.LINES: gl.glDrawElements(GL2.GL_LINES, faces.length, GL2.GL_UNSIGNED_INT, 0);break;
			    	case DrawType.SOLID:
			    	case DrawType.SOLID_SMOOTH:gl.glDrawElements(mesh.getType() == SceneObjectMesh.TRIANGLE ? GL2.GL_TRIANGLES : GL2.GL_QUADS, faces.length, GL2.GL_UNSIGNED_INT, 0);break;
			    	case DrawType.LINE_STRICLES:
					break;
				default:
					break;
			    }
			    
			    gl.glDisableClientState(GL2.GL_VERTEX_ARRAY);
			    gl.glDisableClientState(GL2.GL_NORMAL_ARRAY);
			    
			    if (drawType != DrawType.DOTS)
			    {
			    	gl.glDisableClientState(GL2.GL_INDEX_ARRAY);
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
