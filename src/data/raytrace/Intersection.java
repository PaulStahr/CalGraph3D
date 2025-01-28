package data.raytrace;

import geometry.Vector3d;

public class Intersection{
	public final Vector3d position = new Vector3d();
	public final Vector3d normal = new Vector3d();
	public OpticalObject object;
	public double distance;
	double c;
	double textureX, textureY;
	public int faceIndex;
}