package data.raytrace;

import java.awt.Color;

import data.raytrace.RaySimulation.AlphaCalculation;
import data.raytrace.RaySimulation.MaterialType;
import geometry.Vector2d;
import geometry.Vector3d;

public abstract class SurfaceObject extends OpticalObject{
	public MaterialType materialType = MaterialType.ABSORBATION;
	public double iorq = 1;
	public double inviorq = 1;
	public double diffuse = 0;
	public Color color = Color.BLACK;
	public double ior0 = 1;
	public double ior1 = 1;
	public double ior = 1;
	public double invior = 1;
	public boolean invertNormal;
	public String textureObjectStr;
	public GuiTextureObject textureObject;
	public boolean bidirectional;
	public int numTracedRays;
	public int numUntracedRays;
	public boolean invertInsideOutside = false;
	public AlphaCalculation alphaCalculation = AlphaCalculation.MULT;

	public final void updateIOR()
	{
		if (invertNormal)
		{
			ior = ior1 / ior0;
			invior = ior0 / ior1;
		}
		else
		{
			ior = ior0 / ior1;
			invior = ior1 / ior0;
		}
		iorq = ior * ior - 1;
		inviorq = invior * invior - 1;
	}

	public abstract void getTextureCoordinates(Vector3d position, Vector3d direction, Vector2d v3);

	public abstract void densityCompensation(int trWidth, int trHeight, int[] imageColorArray, int channels, int stride);
}
