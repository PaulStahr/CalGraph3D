package io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import scene.object.SceneObjectMesh;

public class ObjectFileReader {
	public static void readFile(File f, SceneObjectMesh mesh) throws IOException {
		if (!f.exists())
		{
			throw new FileNotFoundException(f.toString());
		}
		
	}
}
