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
package io;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import scene.object.SceneObject;
import scene.object.SceneObjectLine;
import scene.object.SceneObjectMesh;
import scene.object.SceneObjectPlane;
import util.StringUtils;

public class ObjectExporter {
	public static final byte OFF = 0, OBJ = 1;
	
	public static void export(byte type, String file, SceneObject so) throws IOException
	{
        char chBuf[] = new char[1024];
        final StringBuilder strBuilder = new StringBuilder();
		if (so instanceof SceneObjectPlane)
		{
			SceneObjectPlane plane = (SceneObjectPlane)so;
			final int sizeX = plane.getSizeX(), sizeY = plane.getSizeY();
			final float vertexX[] = plane.getVerticesX();
			final float vertexY[] = plane.getVerticesY();
			final float vertexZ[] = plane.getVerticesZ();
        	switch(type){
	        	case OFF:{
	                final FileWriter writer = new FileWriter (file);
	                final BufferedWriter outBuffer = new BufferedWriter (writer);
	                outBuffer.write ("OFF");
	                outBuffer.newLine();
	                strBuilder.append(plane.getVertexCount()).append(' ').append(plane.getFaceCount()).append(' ').append('0');
	                for (int i=0;i<sizeX;i++){
	                    for (int j=0;j<sizeY;j++){
	                        outBuffer.newLine();
	                        chBuf = StringUtils.writeAndReset(outBuffer, strBuilder
	                        		.append(vertexX[i*sizeY+j]).append(' ')
	                        		.append(vertexY[i*sizeY+j]).append(' ')
	                        		.append(vertexZ[i*sizeY+j]), chBuf);
	                    }
	                }
	                for (int i=0;i<sizeX-1;i++){
	                    for (int j=0;j<sizeY-1;j++){
	                        outBuffer.newLine();
	                        chBuf = StringUtils.writeAndReset(outBuffer, strBuilder.append('4').append(' ')
	                        		.append(i*sizeY+j).append(' ')
	                        		.append(i*sizeY+j+1).append(' ')
	                        		.append((i+1)*sizeY+j+1).append(' ')
	                        		.append((i+1)*sizeY+j), chBuf);
	                    }
		                if (plane.isYCyclic())
		                {
		                	chBuf = StringUtils.writeAndReset(outBuffer, strBuilder.append('4').append(' ')
	                        		.append(i*sizeY+sizeY-1).append(' ')
	                        		.append(i*sizeY+sizeY).append(' ')
	                        		.append((i+1)*sizeY+1).append(' ')
	                        		.append((i+1)*sizeY), chBuf);
		                }
	                }
	                if (plane.isXCyclic())
	                {
	                	if (plane.isYCyclic())
	                	{
	                		
	                	}
	                }
	                outBuffer.flush();
	                outBuffer.close();
	                writer.close();
	                break;
	        	}case OBJ:{
	                final FileWriter writer = new FileWriter (file);
	                final BufferedWriter outBuffer = new BufferedWriter (writer);
	                outBuffer.write("o graph");
	                for (int i=0;i<sizeX;i++){
	                    for (int j=0;j<sizeY;j++){
	                        outBuffer.newLine();
	                        chBuf = StringUtils.writeAndReset(outBuffer, strBuilder.
	                        		append('v').append(' ')
	                        		.append(vertexX[i*sizeY+j]).append(' ')
	                        		.append(vertexY[i*sizeY+j]).append(' ')
	                        		.append(vertexZ[i*sizeY+j]), chBuf);
	                    }
	                }
	                for (int i=0;i<sizeX-1;i++){
	                    for (int j=0;j<sizeY-1;j++){
	                        outBuffer.newLine();
	                        chBuf = StringUtils.writeAndReset(outBuffer, strBuilder.append('f').append(' ')
	                        		.append(i*sizeY+j+1).append(' ')
	                        		.append(i*sizeY+j+2).append(' ')
	                        		.append((i+1)*sizeY+j+2).append(' ')
	                        		.append((i+1)*sizeY+j+1), chBuf);
	                    }
	                }
	                outBuffer.flush();
	                outBuffer.close();
	                writer.close();
	                break;	        		
	        	}default:{
	        		throw new IllegalArgumentException();
	        	}
	        }
		}else if (so instanceof SceneObjectLine)
		{
			SceneObjectLine line = (SceneObjectLine)so;
			final float vertices[] = line.getVertices();
			final int size = line.getVertexCount();
	       	switch(type){
	        	case OFF:{
	        		final FileWriter writer = new FileWriter (file);
	                final BufferedWriter outBuffer = new BufferedWriter(writer);
	                outBuffer.write("OFF");
	                outBuffer.newLine();
	                strBuilder.setLength(0);
	                outBuffer.write(strBuilder.append(size).append(' ').append('0').append(' ').append('0').toString());
	                for (int i=0;i<size;i++){
	                    outBuffer.newLine();
	                    chBuf = StringUtils.writeAndReset(outBuffer, strBuilder
	                    		.append(vertices[i * 3]).append(' ')
	                    		.append(vertices[i * 3 + 1]).append(' ')
	                    		.append(vertices[i * 3 + 2]), chBuf);
	                }
	                outBuffer.flush();
	                outBuffer.close();
	                writer.close();
	                break;
	        	}case OBJ:{
	        		final FileWriter writer = new FileWriter (file);
	                final BufferedWriter outBuffer = new BufferedWriter(writer);
	                for (int i=0;i<size;i++){
	                    outBuffer.newLine();
	                    strBuilder.setLength(0);
	                    chBuf = StringUtils.writeAndReset(outBuffer, strBuilder
	                    		.append('v').append(' ')
	                    		.append(vertices[i * 3]).append(' ')
	                    		.append(vertices[i * 3 + 1]).append(' ')
	                    		.append(vertices[i * 3 + 2]), chBuf);
	                }
	                outBuffer.flush();
	                outBuffer.close();
	                writer.close();
	                break;
	          	}default:{
	          		throw new IllegalArgumentException();
	          	}
			}
		}
		else if (so instanceof SceneObjectMesh)
		{
			SceneObjectMesh mesh = (SceneObjectMesh)so;
			float vertices[] = mesh.getVertices();
			int faces[] = mesh.getFaces();
        	switch(type){
	        	case OFF:{
	                final FileWriter writer = new FileWriter (file);
	                final BufferedWriter outBuffer = new BufferedWriter (writer);
	                outBuffer.write ("OFF");
	                strBuilder.append(vertices.length / 3).append(' ').append(faces.length / 3).append(' ').append('0');
	                for (int i = 0; i < vertices.length; i += 3)
	                {
	                	outBuffer.newLine();
	                    chBuf = StringUtils.writeAndReset(outBuffer, strBuilder.append(vertices[i]).append(' ').append(vertices[i + 1]).append(' ').append(vertices[i + 2]), chBuf);
   	                }
	                if (mesh.getType() == SceneObjectMesh.TRIANGLE)
	                {
		                for (int i=0;i<faces.length;i += 3){
		                	outBuffer.newLine();
	                        chBuf = StringUtils.writeAndReset(outBuffer, strBuilder.append('3').append(' ').append(faces[i]).append(' ').append(faces[i+1]).append(' ').append(faces[i+2]), chBuf);
		                }
	                }
	                else if (mesh.getType() == SceneObjectMesh.QUAD)
	                {
	                	for (int i=0;i<faces.length;i += 4){
		                	outBuffer.newLine();
	                        chBuf = StringUtils.writeAndReset(outBuffer, strBuilder.append('3').append(' ').append(faces[i]).append(' ').append(faces[i+1]).append(' ').append(faces[i+2]).append(' ').append(faces[i+3]), chBuf);
		                }
	                }
	                outBuffer.flush();
	                outBuffer.close();
	                writer.close();
	                break;
	        	}case OBJ:{
	                final FileWriter writer = new FileWriter (file);
	                final BufferedWriter outBuffer = new BufferedWriter (writer);
	                outBuffer.write("o graph");
	                for (int i = 0; i < vertices.length; i += 3)
	                {
	                	outBuffer.newLine();
	                	chBuf = StringUtils.writeAndReset(outBuffer, strBuilder.append('v').append(' ').append(vertices[i]).append(' ').append(vertices[i + 1]).append(' ').append(vertices[i + 2]), chBuf);
   	                }
	                if (mesh.getType() == SceneObjectMesh.TRIANGLE)
	                {
			            for (int i=0;i<faces.length;i += 3){
		                	outBuffer.newLine();
	                        chBuf = StringUtils.writeAndReset(outBuffer, strBuilder.append('f').append(' ').append(faces[i] + 1).append(' ').append(faces[i+1] + 1).append(' ').append(faces[i+2] + 1), chBuf);
		                } 
		            }
	                else if (mesh.getType() == SceneObjectMesh.QUAD)
	                {
	                	for (int i=0;i<faces.length;i += 4){
	 		                outBuffer.newLine();
	 		                chBuf = StringUtils.writeAndReset(outBuffer, strBuilder.append('f').append(' ').append(faces[i] + 1).append(' ').append(faces[i+1] + 1).append(' ').append(faces[i+2] + 1).append(' ').append(faces[i+3] + 1), chBuf);
	                	}
	                }
	                else
	                {
	                	throw new IllegalArgumentException();
	                }
	                outBuffer.flush();
	                outBuffer.close();
	                writer.close();
	                break;	        		
	        	}default:{
	        		throw new IllegalArgumentException();
	        	}
        	}
		}
	}
}
