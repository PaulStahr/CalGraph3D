package data.raytrace.raygen;

import geometry.Vector2d;
import geometry.Vector3d;

public class ArrayRayGenerator extends AbstractRayGenerator{
	float positions[];
	float directions[];
	float textureCoordinates[];
	public ArrayRayGenerator(float positions[], float directions[], float textureCoordinates[])
	{
		this.positions = positions;
		this.directions = directions;
		this.textureCoordinates = textureCoordinates;
	}
	
	@Override
	public void generate(int index, int numrays, Vector3d position, Vector3d direction, Vector2d textureCoordinate, float[] color) {
		position.set(positions, index * 3);
		direction.set(directions, index * 3);
		textureCoordinate.set(textureCoordinates, index * 2);
	}
	
}
