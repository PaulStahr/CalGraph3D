package jcomponents.raytrace;

import data.raytrace.OpticalObject;

public class RaySimulationData
{
	public final float endpoints[];
	public final float enddirs[];
	public final byte accepted[];
	public final int bounces[];
	public final float endcolor[];
	public OpticalObject lastObject[];
	
	public RaySimulationData(int numRays, boolean bidir) {
		endpoints = new float[numRays * (bidir ? 6 : 3)];
		enddirs = new float[numRays * (bidir ? 6 : 3)];
		accepted = new byte[numRays];
		bounces = new int[numRays];
		endcolor = new float[numRays * 4];
		lastObject = new OpticalObject[numRays * (bidir ? 2 : 1)];
	}
}

