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

import java.util.ArrayList;

import geometry.Rotation3;
import geometry.Vector3f;
import scene.object.SceneObject;

public class Scene{
	public static final byte LIGHTS = 3, NORMAL_MAP = 4, REFLECTION_MAP = 5;
    public static final byte MODE_MAX_SPEED = 0, MODE_SYNCHRONIZE = 1, MODE_MIN_ENERGY = 2;
	private final ArrayList<SceneObject> sceneObjects = new ArrayList<SceneObject>();
	public int requestedUpdateCount = 0;
	public String cupemap;
    public final Vector3f cameraPosition = new Vector3f();
    public final Rotation3 cameraRotation = new Rotation3();;

	public void addGlObject(SceneObject obj)
	{
        sceneObjects.add(obj);
	}
	
	public void removeGlObject(SceneObject obj)
	{
        sceneObjects.remove(obj);
	}
	
	public SceneObject getObject(int index)
	{
		return sceneObjects.get(index);
	}
	
	public int getObjectCount()
	{
		return sceneObjects.size();
	}
	
	public void getObjects(ArrayList<SceneObject> objects)
	{
		objects.clear();
		objects.addAll(sceneObjects);
	}
	
	public int getRequestedUpdateCount()
	{
		return requestedUpdateCount;
	}

	public void repaint() {
		++requestedUpdateCount;
	}
	
	public void removeDestroyed()
	{
		int writeIndex = 0;
		for (int i = 0; i < sceneObjects.size(); ++i)
		{
			SceneObject so = sceneObjects.get(i);
			if (!so.isDestroyed())
			{
				sceneObjects.set(writeIndex++, sceneObjects.get(i));
			}
		}
		for (int i = sceneObjects.size() - 1; i >= writeIndex; --i)
		{
			sceneObjects.remove(i);
		}
	}
	
	public void getCameraPosition(Vector3f position, Rotation3 rotation)
	{
		position.set(this.cameraPosition);
		rotation.set(this.cameraRotation);
	}

	public void setCameraPosition(Vector3f position, Rotation3 rotation)
	{
		this.cameraPosition.set(position);
		this.cameraRotation.set(rotation);
		repaint();
	}
}
