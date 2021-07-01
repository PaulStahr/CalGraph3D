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

import geometry.FloatVectorObject;
import geometry.Geometry;
import geometry.Vector3f;

import org.lwjgl.opengl.GL11;

import scene.object.SceneObject.DrawType;

public class RenderGlObjectImmediateMode {
	public static final void callPoints(FloatVectorObject vertex){
		final int size = vertex.x.length;
		final float vertexX[] = vertex.x, vertexY[] = vertex.y, vertexZ[] = vertex.z;
        for (int i=0;i<size;i++)
            GL11.glVertex3f(vertexX[i], vertexY[i], vertexZ[i]);
	}
	
	public static final void callPoints(float vertices[]){
		for (int i=0;i<vertices.length;i+=3)
            GL11.glVertex3f(vertices[i], vertices[i+1], vertices[i+2]);
	}
	
	public static final void renderDots(int vektorCount, FloatVectorObject vertex, float vertexNormal[]){
		final float vertexX[] = vertex.x, vertexY[] = vertex.y, vertexZ[] = vertex.z;
		GL11.glShadeModel(GL11.GL_FLAT);
        GL11.glBegin(GL11.GL_POINTS);
        for (int i=0;i<vektorCount;i++){
            GL11.glNormal3f(vertexNormal[i * 3], vertexNormal[i * 3 + 1], vertexNormal[i * 3 + 2]);
            GL11.glVertex3f(vertexX[i], vertexY[i], vertexZ[i]);              
        }
        GL11.glEnd();
	}
	
	public static final void renderLines(int sizeX, int sizeY, FloatVectorObject vertex, float vertexNormal[]){
		final float vertexX[] = vertex.x, vertexY[] = vertex.y, vertexZ[] = vertex.z;
        GL11.glShadeModel(GL11.GL_SMOOTH);
        for (int i=0;i<sizeX;i++){
            GL11.glBegin(GL11.GL_LINE_STRIP);
            for (int j=0;j<sizeY;j++){
            	final int index = i*sizeY+j;
            	GL11.glNormal3f(vertexNormal[index * 3], vertexNormal[index * 3 + 1], vertexNormal[index * 3 + 2]);
            	GL11.glVertex3f(vertexX[index], vertexY[index], vertexZ[index]);	
            }
            GL11.glEnd();
        }
        for (int i=0;i<sizeY;i++){
            GL11.glBegin(GL11.GL_LINE_STRIP);
            for (int j=0;j<sizeX;j++){
            	final int index =j*sizeY + i;
            	GL11.glNormal3f(vertexNormal[index * 3], vertexNormal[index * 3 + 1], vertexNormal[index * 3 + 2]);
            	GL11.glVertex3f(vertexX[index], vertexY[index], vertexZ[index]);
            }
            GL11.glEnd();
        }
	}
	
	public static final void renderQuadsSolid(int sizeX, int sizeY, FloatVectorObject vertex, float faceNormal[]){
		final float vertexX[] = vertex.x, vertexY[] = vertex.y, vertexZ[] = vertex.z;
		final int sizeXMin1 = sizeX -1;
		GL11.glShadeModel(GL11.GL_FLAT);
        int normalIndex = 0;
    	int index0 = 0, index1 = sizeY;
        for (int i=0;i<sizeXMin1;i++){
            GL11.glBegin(GL11.GL_QUAD_STRIP); 
            GL11.glNormal3f(faceNormal[normalIndex], faceNormal[normalIndex + 1], faceNormal[normalIndex + 2]);
            GL11.glVertex3f(vertexX[index0],vertexY[index0],vertexZ[index0]);
            GL11.glVertex3f(vertexX[index1],vertexY[index1],vertexZ[index1]);
        	index0++;
        	index1++;
            for (int j=1;j<sizeY;j++, normalIndex+=3, index0++, index1++){
                GL11.glNormal3f(faceNormal[normalIndex], faceNormal[normalIndex + 1], faceNormal[normalIndex + 2]);
                GL11.glVertex3f(vertexX[index0],vertexY[index0],vertexZ[index0]);
                GL11.glVertex3f(vertexX[index1],vertexY[index1],vertexZ[index1]);
            }
            GL11.glEnd();
        }
	}

	private static final void renderQuadsSolid(int sizeX, int sizeY, FloatVectorObject vertex, float faceNormal[], int[] permutation) {
		final float vertexX[] = vertex.x, vertexY[] = vertex.y, vertexZ[] = vertex.z;
		final int sizeXMin1 = sizeX -1, sizeYMin1 = sizeY - 1;
		final int faceCount = sizeXMin1 * sizeYMin1;
		GL11.glShadeModel(GL11.GL_FLAT);
		GL11.glBegin(GL11.GL_QUADS);
    	for (int i=0;i<faceCount;++i){
    		int face = permutation[i];
     		int index = (face / sizeYMin1) + face;
     		face *= 3;
            GL11.glNormal3f(faceNormal[face], faceNormal[face + 1], faceNormal[face + 2]);
            GL11.glVertex3f(vertexX[index], vertexY[index], vertexZ[index]);
            index += sizeY;
            GL11.glVertex3f(vertexX[index], vertexY[index], vertexZ[index]);	  
            index += 1;
            GL11.glVertex3f(vertexX[index], vertexY[index], vertexZ[index]);
            index -= sizeY;
            GL11.glVertex3f(vertexX[index], vertexY[index], vertexZ[index]);
    	}
    	GL11.glEnd();
	}
	
	public static final void renderQuadsSmooth(int sizeX, int sizeY, FloatVectorObject vertex, float vertexNormal[]){
		final float vertexX[] = vertex.x, vertexY[] = vertex.y, vertexZ[] = vertex.z;
        GL11.glShadeModel(GL11.GL_SMOOTH);
        int index0 = 0, index1 = sizeY;
        for (int i=0;i<sizeX-1;i++){
            GL11.glBegin(GL11.GL_QUAD_STRIP); 
            for (int j=0;j<sizeY;j++, index0++, index1++){
                GL11.glNormal3f(vertexNormal[index0 * 3], vertexNormal[index0 * 3 + 1], vertexNormal[index0 * 3 + 2]);
                GL11.glVertex3f(vertexX[index0], vertexY[index0], vertexZ[index0]);		
                GL11.glNormal3f(vertexNormal[index1 * 3], vertexNormal[index1 * 3 + 1], vertexNormal[index1 * 3 + 2]);
                GL11.glVertex3f(vertexX[index1], vertexY[index1], vertexZ[index1]);		
            }
            GL11.glEnd();
        }	
	}
	
	public static final void renderQuadsSmooth(int sizeX, int sizeY, FloatVectorObject vertex, float vertexNormal[], int permutation[]){
		final float vertexX[] = vertex.x, vertexY[] = vertex.y, vertexZ[] = vertex.z;
    	final int sizeYMin1 = sizeY-1;
    	final int faceCount = (sizeX - 1) * sizeYMin1;
        GL11.glShadeModel(GL11.GL_SMOOTH);
		GL11.glBegin(GL11.GL_QUADS);
    	for (int i=0;i<faceCount;++i){
    		int face = permutation[i];
     		int index0 = (face / sizeYMin1) + face;
     		int index1 = index0 + sizeY;
     		int index2 = index0 + sizeY + 1;
     		int index3 = index0+1;
            GL11.glNormal3f(vertexNormal[index0 * 3], vertexNormal[index0 * 3 + 1], vertexNormal[index0 * 3 + 2]);
            GL11.glVertex3f(vertexX[index0], vertexY[index0], vertexZ[index0]);		
            GL11.glNormal3f(vertexNormal[index1 * 3], vertexNormal[index1 * 3 + 1], vertexNormal[index1 * 3 + 2]);
            GL11.glVertex3f(vertexX[index1], vertexY[index1], vertexZ[index1]);	                        
            GL11.glNormal3f(vertexNormal[index2 * 3], vertexNormal[index2 * 3 + 1], vertexNormal[index2 * 3 + 2]);
            GL11.glVertex3f(vertexX[index2], vertexY[index2], vertexZ[index2]);		
            GL11.glNormal3f(vertexNormal[index3 * 3], vertexNormal[index3 * 3 + 2], vertexNormal[index3 * 3 + 2]);
            GL11.glVertex3f(vertexX[index3], vertexY[index3], vertexZ[index3]);
    	}
    	GL11.glEnd();
	}
	
	public static final void render(
			int sizeX,
			int sizeY,
			FloatVectorObject vertex,
			float vertexNormal[],
			float faceNormal[],
			int permutation[],
			float distance[],
			byte drawType,
			Vector3f cameraPosition,
			boolean usePermutation,
			boolean updatePermutation, 
			boolean xCyclic,
			boolean yCyclic){
         	switch (drawType){
	            case DrawType.DOTS:{
	            	Geometry.calcVertexNormals(sizeX, sizeY, vertex, vertexNormal, xCyclic, yCyclic);
		            renderDots(sizeX*sizeY, vertex, vertexNormal);
	                break;
	            }case DrawType.LINES:{
	              	Geometry.calcVertexNormals(sizeX, sizeY, vertex, vertexNormal, xCyclic, yCyclic);
	              	renderLines(sizeX, sizeY, vertex, vertexNormal);
	                break;
	            }case DrawType.SOLID:{
	              	Geometry.calcFaceNormals(sizeX, sizeY, vertex, faceNormal);
	              	if (usePermutation){
	              		if (updatePermutation)
	              			calculatePermutation(cameraPosition, sizeX, sizeY, permutation, distance, vertex);
	                    renderQuadsSolid(sizeX, sizeY, vertex, faceNormal, permutation);              		
	              	}else{
	                    renderQuadsSolid(sizeX, sizeY, vertex, faceNormal);              		
	              	}
	                break;
	            }case DrawType.SOLID_SMOOTH:{
	            	Geometry.calcVertexNormals(sizeX, sizeY, vertex, vertexNormal, xCyclic, yCyclic);
	                GL11.glShadeModel(GL11.GL_SMOOTH);
	                if (usePermutation){
	                	if (updatePermutation)
	                		calculatePermutation(cameraPosition, sizeX, sizeY, permutation, distance, vertex);
	                	renderQuadsSmooth(sizeX, sizeY, vertex, vertexNormal, permutation);
	                }else{
	                	renderQuadsSmooth(sizeX, sizeY, vertex, vertexNormal);
	                }
	                break;
	            }
			case DrawType.LINE_STRICLES:
				break;
			default:
				break;
        }
	}

	public static final void calculatePermutation(Vector3f camera, int sizeX, int sizeY, int permutation[], float distance[], FloatVectorObject vertex){
		final float vertexX[] = vertex.x, vertexY[] = vertex.y, vertexZ[] = vertex.z;
		int sizeXMin1 = sizeX-1, sizeYMin1 = sizeY-1;
		int faceCount = sizeXMin1 * sizeYMin1;
		final float camX = camera.x, camY = camera.y, camZ = camera.z;
		for (int i=0;i<sizeXMin1;++i){
			int v = sizeY * i;
			int face = sizeYMin1 * i;
			for (int j=0;j<sizeYMin1;++j){
				//distance[face] = Math.abs(camX - vertexX[v]) + Math.abs(camY - vertexY[v]) + Math.abs(camZ - vertexZ[v]);
				final float xDiff = camX - vertexX[v];
				final float yDiff = camY - vertexY[v];
				final float zDiff = camZ - vertexZ[v];
				distance[face] = xDiff * xDiff + yDiff * yDiff + zDiff * zDiff;
				permutation[face] = face;
				++v;++face;
			}
		}
		calculatePermutation(permutation, distance, 0, faceCount-1);
	}

	public static final void calculatePermutation(final int permutation[], final float distance[], final int left, final int right) {
		if (left + 1 < right) {
			final float pivotDist = distance[permutation[(left + right)/2]];               
		    int i = left;
		    int j = right;
		    do {
		    	while (distance[permutation[i]] > pivotDist)
		    		++i;
		    	while (distance[permutation[j]] < pivotDist)
		    		--j;
		    	if (i <= j){
			    	final int tmp = permutation[i]; 
			        permutation[i] = permutation[j]; 
			        permutation[j] = tmp; 
			        ++i;--j;
		    	}
		    }while(i<=j);
		    
		    calculatePermutation(permutation, distance, left, j);
		    calculatePermutation(permutation, distance, i, right);
	   }
	}
}
