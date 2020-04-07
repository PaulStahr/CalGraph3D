package data.raytrace.raygen;

import data.raytrace.MeshObject;
import data.raytrace.OpticalObject;
import data.raytrace.OpticalSurfaceObject;
import data.raytrace.OpticalVolumeObject;
import geometry.Vector2d;
import geometry.Vector3d;

public abstract class AbstractRayGenerator {
	public abstract void generate(int index, int numrays, Vector3d position, Vector3d direction, Vector2d textureCoordinate, int color[]);
	
	public OpticalObject getSource()
	{
		return null;
	}
	
	public OpticalVolumeObject[] getSuccessorVolumes()
	{
		return null;
	}
	
	public MeshObject[] getSuccessorMeshes()
	{
		return null;
	}	

	public OpticalSurfaceObject[] getSuccessorSurfaces() {
		return null;
	}
}
