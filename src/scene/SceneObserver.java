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

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import scene.object.SceneObject;
import scene.object.SceneObject.UpdateKind;
import util.ListTools;
import util.keyfunction.KeyFunctionInt;

public final class SceneObserver{
	private int vertexCount = 0, faceCount = 0;
	private final Scene scene;
	private int updateCount = 0;
	private SceneObjectInformation sceneObjectInfo[] = new SceneObjectInformation[1];
	private int sceneObjectInfoCount = 0;
	private final ArrayList<SceneObjectInformation> removedObjects = new ArrayList<SceneObjectInformation>();
	private final ArrayList<SceneObjectInformation> addedObjects = new ArrayList<SceneObjectInformation>();
	private boolean update;
	private static final KeyFunctionInt<SceneObjectInformation> keyFunction = new KeyFunctionInt<SceneObjectInformation>() {
		@Override
		public int getKey(SceneObjectInformation obj) {
			return obj.id;
		}
	};
	
	private static final Comparator<SceneObjectInformation> idComparator = new Comparator<SceneObjectInformation>() {

		@Override
		public int compare(SceneObjectInformation o1, SceneObjectInformation o2) {
			return o2.id - o1.id;
		}
	};
	
	public synchronized void reset()
	{
		sceneObjectInfo = new SceneObjectInformation[1];
		sceneObjectInfoCount = 0;
		removedObjects.clear();
		addedObjects.clear();
		updateCount = 0;
	}
	
	public static class SceneObjectInformation{
		private final int id;
		private final WeakReference<SceneObject> ref;
		private Object attachedObject;
		private boolean marked = false;
		private int updateCount[] = new int[UpdateKind.size];
		private int objectUpdateBitmask;
		
		private SceneObjectInformation(SceneObject so){
			this.id = so.getId();
			ref = new WeakReference<SceneObject>(so);
		}
		
		public SceneObject getSceneObject()
		{
			return ref.get();
		}
		
		public void attachObject(Object o)
		{
			this.attachedObject = o;
		}
		
		public Object getAttachedObject()
		{
			return attachedObject;
		}

		public final void repoll() {
			SceneObject so = ref.get();
			if (so != null)
			{
				objectUpdateBitmask = so.getUpdateBitmask(updateCount, objectUpdateBitmask);
			}
		}
		
		private final void poll()
		{
			SceneObject so = ref.get();
			if (so != null)
			{
				objectUpdateBitmask = so.getUpdateBitmask(updateCount, 0);
			}
		}

		public boolean objectUpdatedBitmask(int updateKind)
		{
			return (objectUpdateBitmask & updateKind) != 0;
		}
		
		public boolean objectUpdated(int updateKind) {
			return (objectUpdateBitmask & (1 << updateKind)) != 0;
		}
	}
	
	private SceneObjectInformation getSceneObjectInformation(SceneObject so)
	{
		int pos = ListTools.binarySearch(sceneObjectInfo, 0, sceneObjectInfoCount, so.getId(), keyFunction);
		if (pos < 0)
			return null;
		return sceneObjectInfo[pos];
	}

	/*private void addSceneObjectInformation(SceneObjectInformation so)
	{
		int pos = ListTools.binarySearch(sceneObjectInfo, 0, sceneObjectInfoCount, so.id, keyFunction);
		if (pos < 0)
		{
			if (sceneObjectInfo.length == sceneObjectInfoCount)
			{
				sceneObjectInfo = Arrays.copyOf(sceneObjectInfo, sceneObjectInfo.length * 2);
			}
			System.arraycopy(sceneObjectInfo, -pos, sceneObjectInfo, 1 - pos, sceneObjectInfoCount + pos);
			sceneObjectInfo[-pos] = so;
		}
	}*/
	
	public final int getSceneObjectCount()
	{
		return sceneObjectInfoCount;
	}
	
	public SceneObjectInformation getSceneObjectInfo(int index)
	{
		return sceneObjectInfo[index];
	}
	
	public SceneObserver(Scene scene)
	{
		this.scene = scene;		
	}
	
	public final int getVertexCount()
	{
		return vertexCount;
	}
	
	public final int getFaceCount()
	{
		return faceCount;
	}
	
	public void poll()
	{
		addedObjects.clear();
		removedObjects.clear();
		faceCount = 0;
		vertexCount = 0;
		synchronized(scene)
		{
			scene.removeDestroyed();
			int tmp = scene.getRequestedUpdateCount();
			update = tmp != updateCount;
			updateCount = tmp;
			for (int i = 0; i < scene.getObjectCount(); ++i)
			{
				SceneObject so = scene.getObject(i);
				if (so.isVisible())
				{
					faceCount += so.getFaceCount();
					vertexCount += so.getVertexCount();
					SceneObjectInformation info = getSceneObjectInformation(so);
					if (info == null)
					{
						addedObjects.add(new SceneObjectInformation(so));
						update = true;
					}
					else
					{
						info.marked = true;
						info.poll();
						if (info.objectUpdated(UpdateKind.ALL))
						{
							update = true;
						}
					}
				}
				
			}
		}
		addedObjects.sort(idComparator);
		removedObjects.sort(idComparator);
		int writeIndex = 0;
		for (int i = 0; i < sceneObjectInfoCount;++i)
		{
			
			sceneObjectInfo[writeIndex] = sceneObjectInfo[i];
			if (sceneObjectInfo[writeIndex].marked)
			{
				sceneObjectInfo[writeIndex].marked = false;
				++writeIndex;
			}
			else
			{
				removedObjects.add(sceneObjectInfo[writeIndex]);
			}
		}
		if (writeIndex + addedObjects.size() > sceneObjectInfo.length)
		{
			sceneObjectInfo = Arrays.copyOf(sceneObjectInfo, Math.max(sceneObjectInfo.length * 2, writeIndex + addedObjects.size()));
		}
		for (int i = writeIndex - 1, j = addedObjects.size() - 1; j >= 0;)
		{
			if (i != -1 && addedObjects.get(j).id < sceneObjectInfo[i].id)
			{
				sceneObjectInfo[i+j + 1] = sceneObjectInfo[i];
				--i;
			}
			else
			{
				sceneObjectInfo[i+j + 1] = addedObjects.get(j);	
				--j;
			}
		}
		sceneObjectInfoCount = writeIndex + addedObjects.size();
	}
	
	public boolean sceneUpdated()
	{
		return update;
	}
	
	public Scene getScene()
	{
		return scene;
	}
	
	public StringBuilder printSceneInformation(StringBuilder strB)
	{
		strB.append("AddedObjects: {");
		for (int i = 0; i < addedObjects.size(); ++i)
		{
			if (i != 0)
			{
				strB.append(',');
			}
			strB.append(addedObjects.get(i).id);
		}
		strB.append("}\nRemovedObjects:{");
		for (int i = 0; i < removedObjects.size(); ++i)
		{
			if (i != 0)
			{
				strB.append(',');
			}
			strB.append(removedObjects.get(i).id);
		}
		strB.append("}\nCorrentObjects:{");
		for (int i = 0; i < sceneObjectInfoCount; ++i)
		{
			if (i != 0)
			{
				strB.append(',');
			}
			strB.append(sceneObjectInfo[i].id);
		}
		strB.append('}');
		return strB;
	}
}
