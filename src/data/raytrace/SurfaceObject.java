package data.raytrace;

import java.awt.Color;

import data.raytrace.RaySimulation.AlphaCalculation;
import geometry.Vector2d;
import geometry.Vector3d;
import maths.Operation;
import maths.data.RealLongOperation;

public abstract class SurfaceObject extends OpticalObject{
	public double iorq = 1;
	public double inviorq = 1;
	public double diffuse = 0;
	public Color color = Color.BLACK;
	public Operation ior0 = RealLongOperation.POSITIVE_ONE;
	public Operation ior1 = RealLongOperation.POSITIVE_ONE;
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
		double ior0 = this.ior0.doubleValue();
		double ior1 = this.ior1.doubleValue();
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

    public abstract void densityCompensation(int trWidth, int trHeight, long[] imageColorArray, int channels, int stride);
}
