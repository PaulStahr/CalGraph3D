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

import data.Options;
import opengl.Material;
/** 
* @author  Paul Stahr
* @version 04.02.2012
*/
public abstract class SceneObject
{
	public static class UpdateKind{
		public static final byte DATA = 0, DIMENSION = 1, OBJECT_KIND = 2, MATERIAL = 3, DRAW_TYPE = 4, VISIBILITY = 5, ALL = 6, size = 7;
	}
	
	public static class DrawType{
		public static final byte DOTS = 0, LINES = 1, LINE_STRICLES = 2, SOLID = 3, SOLID_SMOOTH = 4;
		
		public static byte[] values()
		{
			return new byte[] {DOTS, LINES, LINE_STRICLES, SOLID, SOLID_SMOOTH};
		}
	}
    
	private boolean destroyed = false;
	private boolean visible = false;

    public final Material lightMaterial;
    public final Material reflectionMaterial;
    private static volatile int maxId = -1;
    private final int id;
    private byte drawType = DrawType.DOTS;
    private int updateCounts[] = new int[UpdateKind.size];
    private static int memoryLimit = -1, optionModCount = -1;

    public SceneObject(){
    	synchronized(SceneObject.class)
    	{
    		id = ++maxId;
    	}
    	Material materials[] = Material.create(2);
    	lightMaterial = materials[0];
    	reflectionMaterial = materials[1];
	}
    
	protected static final int getMemoryLimit(){
		if (optionModCount != Options.modCount()){
			memoryLimit = Options.getInteger("limit_graph_memory", -1);
			optionModCount = Options.modCount();
		}
		return memoryLimit;
	}
    
    public abstract boolean isCachable();
    
    public Material getReflectionMaterial()
    {
    	return reflectionMaterial;
    }
    
    public Material getLightMaterial()
    {
    	return lightMaterial;
    }

	public final boolean isVisible(){
		return visible;
	}
	
	public byte getDrawType() {
		return drawType;
	}
	
	public final void setVisible(boolean visible){
		if (destroyed || this.visible == visible)
			return;
		if (this.visible != visible)
		{
			this.visible = visible;
			update(UpdateKind.VISIBILITY);
		}
	}
	
	public abstract boolean drawTypeAllowed(byte dt);
	
	public final void destroy(){
		destroyed = true;
	}
	
	public final boolean isDestroyed(){
		return destroyed;
	}
	
	public abstract boolean hasNormals();

	public abstract int getVertexCount();
	
	public abstract int getFaceCount();
	
	public abstract int getUsedMemory();
	
	public int getId()
	{
		return id;
	}
	
	@Override
	public int hashCode()
	{
		return id;
	}
	
	public int getUpdateCount(byte updateKind)
	{
		return updateCounts[updateKind];
	}
	
	public final int getUpdateBitmask(int updateCount[], int res)
	{
		for (byte i = 0; i < UpdateKind.size; ++i)
		{
			int tmp = updateCounts[i];
			res |= (updateCount[i] == tmp ? 0 : 1) << i;
			updateCount[i] = tmp;
		}
		return res;
	}
	
	public void update(byte updateKind)
	{
		++updateCounts[updateKind];
		++updateCounts[UpdateKind.ALL];
	}

	public void setDrawType(byte drawType) {
		this.drawType = drawType;
		update(UpdateKind.DRAW_TYPE);
	}
}
