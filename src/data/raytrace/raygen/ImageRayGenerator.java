package data.raytrace.raygen;

import java.util.Arrays;

import data.raytrace.MeshObject;
import data.raytrace.OpticalSurfaceObject;
import data.raytrace.OpticalVolumeObject;
import data.raytrace.TextureMapping;
import geometry.Rotation3;
import geometry.Vector2d;
import geometry.Vector3d;

public class ImageRayGenerator extends AbstractRayGenerator{
	public int width;
	public int height;
	public Vector3d position = new Vector3d();
	public Rotation3 rotation = new Rotation3();
	public double arpertureSize;
	public double focalDistance;
	public OpticalVolumeObject[] volumeSuccessor = OpticalVolumeObject.EMPTY_VOLUME_ARRAY;
	public MeshObject[] meshSuccessor = MeshObject.EMPTY_MESH_ARRAY;
	public  OpticalSurfaceObject[] surfaceSuccessor = OpticalSurfaceObject.EMPTY_SURFACE_ARRAY;
	public TextureMapping mapping = TextureMapping.SPHERICAL;
	
	public ImageRayGenerator() {}
	
	@Override
	public void generate(int index, int numrays, Vector3d position, Vector3d direction, Vector2d textureCoordinate, int color[])
	{
		//index /= 3;
		int y = index / width;
		int x = index % width;
		
		position.set(this.position);
		//mapping = TextureMapping.FISHEYE_EQUIDISTANT;//TODO
		mapping = null;
		if (mapping == null)
		{
			direction.set((double)(x - width / 2) / width, (double)(height / 2 - y) / width, -1);
		}
		else
		{
			mapping.mapTexToCart((double)(x) / width, (double)(y) / height, direction);
			direction.invert();
		}
		direction.rotateXYZEuler(rotation);
		
		Arrays.fill(color, 0);
	}
	
	public OpticalVolumeObject[] getSuccessorVolumes()
	{
		return volumeSuccessor;
	}
	
	public MeshObject[] getSuccessorMeshes()
	{
		return meshSuccessor;
	}	
	
	public void setSuccessorSurfaces(OpticalSurfaceObject[] surfaces)
	{
		this.surfaceSuccessor = surfaces;
	}

	public OpticalSurfaceObject[] getSuccessorSurfaces() {
		return surfaceSuccessor;
	}
}
