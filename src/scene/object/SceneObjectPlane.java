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
package scene.object;
import geometry.*;

import java.awt.Image;
/** 
* @author  Paul Stahr
* @version 04.02.2012
*/
public class SceneObjectPlane extends SceneObject
{
    public SceneObjectPlane() {}

	private int permutation[] = null;
	private float distance[] = null;
	private FloatVectorObject vertex = null;
	private float textureCoordinates[] = null;
	private float vertexNormal[] = null;
	private float faceNormal[] = null;
	private boolean xCyclic = true, yCyclic = true;
    private int sizeX = 0, sizeY = 0;
    private int vertexCount = 0, faceCount = 0, usedMemory = 0;
	public boolean usePermutation = false, updatePermutation = true;
	//public int cameraPrositionChangeCount = 0;
    //public static boolean vbo, upda = true;
    private Image texture;
    
    /*static{
    	DataHandler.addGlobalKeyListener(new KeyListener() {
			
			@Override
			public void keyTyped(KeyEvent e) {
				if (e.getKeyChar() == 'b'){
					vbo = !vbo;
					System.out.println("vbo " + (vbo ? "" : "de") + "activated");
				}
				if (e.getKeyChar() == 'u'){
					upda = !upda;
					System.out.println("updatepbo " + (upda ? "" : "de") + "activated");					
				}
			}
			
			@Override
			public void keyReleased(KeyEvent e) {}
			
			@Override
			public void keyPressed(KeyEvent e) {}
		});
    }*/
	
    public Image getTexture(){
    	return texture;
    }
    
    public float[] getTextureCoordinates(){
    	return textureCoordinates;
    }
    
	public float[] getVertexNormals(){
		return vertexNormal;
	}
	
	public float[] getFaceNormals(){
		return faceNormal;
	}
	
	public FloatVectorObject getVertices(){
		return vertex;
	}
	
	@Override
	public final int getVertexCount(){
    	return vertexCount;
    }

	@Override
	public final int getFaceCount(){
    	return faceCount;
    }
	
	public void setTexture(Image img)
	{
		this.texture = img;
	}
   
	@Override
	public final int getUsedMemory(){
    	return usedMemory;
    }
    
 	public final boolean setSize(int x, int y){
        if (sizeX == x && sizeY == y)
            return true;
    	final int limit = getMemoryLimit(), usedMemory = x*y*36-(x+y)*12-12;
        if (x <= 1 || y <= 1 || (limit != -1 && usedMemory> limit*0x10000)){
        	reset();
            return false;        	
        }
        try{
        	int vertexCount = x*y;
        	int faceCount = (x - 1)*(y - 1);
        	FloatVectorObject vertex = new FloatVectorObject(vertexCount);
        	float[] vertexNormal = new float[3 * vertexCount];
        	float[] faceNormal = new float[3 * faceCount];
        	int permutation[] = null;
        	float distance[] = null;
            if (usePermutation){
    			permutation = new int[faceCount];
    			distance = new float[faceCount];
            }
        	synchronized(this){
	        	this.usedMemory = usedMemory;
	            this.sizeX = x;
	            this.sizeY = y;
	            this.faceCount = faceCount;
	            this.vertexCount = vertexCount;
	            this.vertex = vertex;
	            this.vertexNormal = vertexNormal;
	            this.faceNormal = faceNormal;
	            this.permutation = permutation;
	            this.distance = distance;
	            update(UpdateKind.DIMENSION);
        	}
            return true;
    	}catch(OutOfMemoryError e){
    		reset();
    		throw e;
    	}
    }
    
    private synchronized void reset(){
        usedMemory = sizeX = sizeY = 0;
        vertex = null;
        vertexNormal = faceNormal = null;
        permutation = null;
        distance = null;
        update(UpdateKind.DIMENSION);
    }
    
    public final float[] getVerticesX(){
        return vertex.x;
    }    

    public final float[] getVerticesY(){
        return vertex.y;
    }    

    public final float[] getVerticesZ(){
        return vertex.z;
    }

    private final int getVertexIndex(int x, int y){
    	return x*sizeY+y;
    }
    
    private final int getFaceIndex(int x, int y){
    	return x*(sizeY - 1)+y;
    }
    
    public Vector3f getVertex(int x, int y){
    	return vertex.getVector(getVertexIndex(x, y));
    }

    public void setVertex(int x, int y, Vector3f vertex){
    	this.vertex.setVector(getVertexIndex(x, y), vertex);      
    }

    public void setVertex (int x, int y, float xP, float yP, float zP){
    	vertex.setVector(getVertexIndex(x, y), xP, yP, zP);
    }

    public Vector3f getVertexNormal(int x, int y){
    	int index = getVertexIndex(x, y) * 3;
    	return new Vector3f(vertexNormal[index], vertexNormal[index + 1], vertexNormal[index + 2]);
    }

    public Vector3f getFaceNormal(int x, int y){
    	int index = getFaceIndex(x, y) * 3;
    	return new Vector3f(faceNormal[index], faceNormal[index + 1], faceNormal[index + 2]);
    }    
    
	@Override
	public final boolean hasNormals(){
    	return true;
    }

	@Override
	public final boolean isCachable() {
		return true;//!vbo;
	}

	public float[] getDistance() {
		return distance;
	}

	public int[] getPermutation() {
		return permutation;
	}

	public int getSizeX() {
		return sizeX;
	}

	public int getSizeY() {
		return sizeY;
	}

	public boolean isXCyclic() {
		return xCyclic;
	}
	
	public boolean isYCyclic() {
		return yCyclic;
	}
	
	@Override
	public boolean drawTypeAllowed(byte dt) {
		switch (dt)
		{
		case DrawType.DOTS: 		return true;
		case DrawType.LINES:		return true;
		case DrawType.LINE_STRICLES:return false;
		case DrawType.SOLID:		return true;
		case DrawType.SOLID_SMOOTH:	return true;
		default:
			throw new IllegalArgumentException();
		}
	}
}
