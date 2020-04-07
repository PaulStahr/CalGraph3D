package data;

import java.util.Arrays;

import util.ArrayTools;
import util.data.UniqueObjects;

public class ObjectAttachmentContainer {
	private Object objects[] = UniqueObjects.EMPTY_OJECT_ARRAY;
	private int ids[] = UniqueObjects.EMPTY_INT_ARRAY;
	private static int id = 0;
	
	public static int getId()
	{
		return id++;
	}
	
	public int attachObject(Object obj, int id)
	{
		int index = Arrays.binarySearch(ids, id);
		if (index < 0)
		{
			objects = ArrayTools.insert(objects, obj, -index - 1);
			ids = ArrayTools.insert(ids, id, -index - 1);
		}
		else
		{
			objects[index] = obj;
		}
		return id;
	}

	public int attachObject(Object obj)
	{
		ArrayTools.push_back(objects, obj);
		ArrayTools.push_back(ids, id);
		return id++;
	}
	
	public Object get(int id)
	{
		int index = Arrays.binarySearch(ids, id);
		return index < 0 ? null : objects[index];
	}

	public void detachObject(int id)
	{
		int index = Arrays.binarySearch(ids, id);
		if (index >= 0)
		{
			ids = ArrayTools.delete(ids, index);
			objects = ArrayTools.delete(objects, index);
		}
	}
}
