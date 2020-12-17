package data.raytrace.raygen;

import java.util.Random;

import data.raytrace.MeshObject;
import data.raytrace.OpticalObject;
import data.raytrace.OpticalSurfaceObject;
import data.raytrace.OpticalVolumeObject;
import data.raytrace.RaytraceScene;
import geometry.Geometry;
import geometry.Vector2d;
import geometry.Vector3d;

public class RayGenerator extends AbstractRayGenerator{
	private OpticalObject source;
	private int modCount;
	public boolean threeDimensional;
	private final Vector3d v0 = new Vector3d();
	private final Vector3d v1 = new Vector3d();
	private double arcOpen;
	private double cosArcOpen;
	private double radiusRadiusGeomRatio;
	double elevation = Double.NaN, azimuth = Double.NaN;
	public Random rand;
	boolean ignoreRandom = false;
	
	public RayGenerator(RayGenerator gen) {
		this.source = gen.source;
		this.threeDimensional = gen.threeDimensional;
		this.v0.set(gen.v0);
		this.v1.set(gen.v1);
		this.arcOpen = gen.arcOpen;
		this.elevation = gen.elevation;
		this.azimuth = gen.azimuth;
	}

	public RayGenerator() {}

	@Override
	public OpticalObject getSource()
	{
		return source;
	}
	
	private synchronized void init()
	{
		if (source instanceof OpticalSurfaceObject)
		{
			OpticalSurfaceObject surf = (OpticalSurfaceObject)source;
			Vector3d direction = surf.direction;
			double radius = surf.directionLength;
			radiusRadiusGeomRatio = surf.directionLength / surf.maxRadiusGeometric;
			switch (surf.surf)
			{
				case FLAT:
					if (threeDimensional)
					{
						Geometry.getOrthorgonalVector(direction, v0);
						//v0.setLength(directionLength);
						v1.cross(direction, v0);
						v1.setNorm(surf.maxRadiusGeometric);
					}
					else
					{
						v0.set(direction);
						v0.rotateRadiansZ(Math.PI * 0.5);
					}
					v0.setNorm(surf.maxRadiusGeometric);
					break;
				case HYPERBOLIC:
					break;
				case PARABOLIC:
					break;
				case SPHERICAL:
					if (surf.maxRadiusGeometric / radius < 1)
					{
						arcOpen = Math.asin(surf.maxRadiusGeometric / radius);	
					}
					else
					{
						arcOpen = Math.PI - Math.asin(radiusRadiusGeomRatio);
					}
					//radius *= scale;
					//g.drawArc((int)(x - radius), (int)(y - radius), (int)(radius *2), (int)(radius *2), (int)(arcDir - arcOpen), (int)(arcOpen * 2));
					
					if (threeDimensional)
					{
						Geometry.getOrthorgonalVector(direction, v0);
						v1.cross(direction, v0);
						v1.setNorm(surf.directionLength);
					}
					else
					{
						v0.set(direction);
						v0.rotateRadiansZ(Math.PI * 0.5);
					}
					v0.setNorm(surf.directionLength);
			case CUSTOM:
				break;
			case CYLINDER:
				break;
			default:
				break;	 
			}
			cosArcOpen = (1 - Math.cos(arcOpen));
		}
	}
	
	private double rand()
	{
		return ignoreRandom ? 0.5 : rand == null ? Math.random() : rand.nextDouble();
	}
	
	@Override
	public void generate(int index, int numrays, Vector3d position, Vector3d direction, Vector2d textureCoordinate, int color[])
	{
		if (source.modCount() != modCount)
		{
			modCount = source.modCount();
			init();
		}
		double diffuse = 0;
		if (source instanceof OpticalSurfaceObject)
		{
			OpticalSurfaceObject surf = (OpticalSurfaceObject)source;
			diffuse = surf.diffuse;
			switch (surf.surf)
			{
				case FLAT:
				{
					direction.set(surf.direction);
					if (threeDimensional)
					{
						double alpha, beta;
						do
						{
							alpha = rand() - 0.5;
							beta = rand() - 0.5;
						}while(alpha * alpha + beta * beta > 0.25);
						position.set(surf.midpoint, v0, alpha * 2, v1, beta * 2);
					}
					else
					{
						double alpha =numrays <= 1 ? 0 : (double) (index * 2 - numrays + 1) / (numrays - 1);
						position.set(surf.midpoint, v0, alpha);
					}
					break;
				}
				case HYPERBOLIC:
					break;
				case PARABOLIC:
					break;
				case SPHERICAL:
				{
					while(true)
					{
						if (threeDimensional)
						{
							double elevation = this.elevation;
							if (Double.isNaN(elevation))
							{
								elevation = Math.acos(1 - rand() * cosArcOpen);
							}
							double azimuth = this.azimuth;
							if (Double.isNaN(azimuth))
							{
								azimuth = rand() * (2 * Math.PI);
							}
							double sin =  Math.sin(elevation);
							direction.set(surf.direction, Math.cos(elevation), v0, sin * Math.sin(azimuth), v1, sin * Math.cos(azimuth));
						}
						else
						{
							double alpha = numrays <= 1 ? 0 : (((double) (index * 2 - numrays + 1) / (numrays - 1))* arcOpen);
							direction.set(surf.direction, Math.cos(alpha), v0, Math.sin(alpha));
						}	
						position.setAdd(surf.midpoint, direction);
						RaytraceScene.readColor(surf, position, direction, textureCoordinate, color);
						/*if (surf.alphaAsMask || true)//TODO
						{
							if (color[3] == 255)
							{
								continue;
							}
						}*/
						if (surf.alphaAsRadius)
						{
							diffuse *= Math.sqrt(255d / color[3]);
							position.set(surf.midpoint, direction, color[3] / 255.);
						}
						break;
					}
					break;
				}
				default:
					break;
			}
			if (surf.invertNormal)
			{
				direction.invert();
			}
		}
		else if (source instanceof MeshObject)
		{
			MeshObject mesh = (MeshObject)source;
			int face = mesh.getFace(rand());
			diffuse = mesh.diffuse;
			double alpha = rand(), beta = rand();
			if (alpha + beta > 1)
			{
				alpha = 1 - alpha;
				beta = 1 - beta;
			}
			mesh.getPoint(face, alpha, beta, position);
			if (mesh.smooth)
			{
				mesh.getNormal(face, alpha, beta, direction);
			}
			else
			{
				mesh.getNormal(face, direction);
			}
			mesh.getTextureCoord(face, alpha, beta, textureCoordinate);
			//if (mesh.invertNormal)
			{
				direction.invert();
			}
		}
		if (diffuse != 0)
		{
			direction.normalize();
			/*Use Householder transformation H=I-2/(v^t*v)*v*v^t with v=dir-e_3 mirror the distribution from the x-y-plane to the v-orthorgonal plane*/
			if (threeDimensional || source instanceof MeshObject)
			{	
				double w = rand() * diffuse * diffuse;
				double rho = rand() * (Math.PI * 2);
				double xf = Math.sqrt(w)*Math.cos(rho);
				double yf = Math.sqrt(w)*Math.sin(rho);
				double zf = Math.sqrt(1-w);
				direction.z -= 1;
				double dot = 2 / direction.dot() * direction.dot(xf,yf,zf);
				direction.x = xf - dot * direction.x;
				direction.y = yf - dot * direction.y;
				direction.z = zf - dot * direction.z;
			}
			else
			{
				double w  = (2 * rand() - 1) * diffuse;
				double h  = Math.sqrt(1 - w * w);
				double x = direction.x * h + direction.y * w;
				double y = direction.y * h - direction.x * w;
				direction.x = x;
				direction.y = y;
				direction.z *= h;
			}
		}
	}
	
    public void generate(int beginIndex, int endIndex, int numrays, double startpoints[], double startdirs[], double textureCoordinates[], Vector3d position, Vector3d direction, Vector2d textureCoordinate, int color[])
    {
		for (int j = beginIndex; j < endIndex; ++j)
		{
			generate(j, numrays, position, direction, textureCoordinate, color);
			int index = j * 3;
			direction.write(startdirs, index);
			position.write(startpoints, index);
			textureCoordinate.write(textureCoordinates, j * 2);
		}
   }

	public void setArcs(double elevation, double azimuth) {
		this.elevation = elevation;
		this.azimuth = azimuth;
	}

	@Override
	public OpticalVolumeObject[] getSuccessorVolumes() {
		return source.volumeSuccessor;
	}

	@Override
	public OpticalSurfaceObject[] getSuccessorSurfaces() {
		return source.surfaceSuccessor;
	}

	public void setSource(OpticalObject opticalObject) {
		source = opticalObject;
		modCount = -1;
	}
}
