/*******************************************************************************
 * Copyright (c) 2019 Paul Stahr
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 ******************************************************************************/
package data.raytrace;

import data.raytrace.RaySimulation.SurfaceType;
import geometry.Geometry;
import geometry.Matrix4d;
import geometry.Vector2d;
import geometry.Vector3d;
import opengl.BufferUtils;

public abstract class OpticalSurfaceObject extends SurfaceObject{
	public static final OpticalSurfaceObject EMPTY_SURFACE_ARRAY[] = new OpticalSurfaceObject[0];
	public double abbeNumber = 1;
	public double conicConstant = 1;
	public final Vector3d direction = new Vector3d();
	public final Vector3d directionNormalized = new Vector3d();
	public SurfaceType surf = SurfaceType.FLAT;
	public double maxRadiusGeometric = 1;
	public double radiusGeometricQ = 1;
	public double minRadiusGeometric = 0;
	public double minRadiusGeometricQ = 0;
	public double directionLength;
	public double directionLengthQ;
	public double invDirectionLengthQ;
	private final Matrix4d matGlobalToSurface = new Matrix4d(1);
	private final Matrix4d matSurfaceToGlobal = new Matrix4d(1);
	public double invDirectionLength;
	private double dotProdUpperBound;
	private double dotProdLowerBound;
	private double dotProdUpperBound2;
	private double dotProdLowerBound2;
	private double maxArcOpen;
	private double minArcOpen;
	public TextureMapping textureMapping = TextureMapping.SPHERICAL;
	public boolean alphaAsRadius;
	private boolean mapLocal = true;
	public boolean alphaAsMask;
	public OpticalSurfaceObject() {}

	public double getMaxArcOpen()
	{
		return maxArcOpen;
	}

	public double getMinArcOpen()
	{
		return minArcOpen;
	}

	public double getDotProdUpperBound()
	{
		return dotProdUpperBound;
	}

	public double getDotProdLowerBound()
	{
		return dotProdLowerBound;
	}

	@Override
	public void getTextureCoordinates(Vector3d position, Vector3d dir, Vector2d out)
	{
		if (mapLocal)
		{
			double x = position.x, y = position.y, z = position.z;
			double tmp0 = matGlobalToSurface.rdotAffineX(x, y, z);
			double tmp1 = matGlobalToSurface.rdotAffineY(x, y, z);
			double tmp2 = matGlobalToSurface.rdotAffineZ(x, y, z);
			textureMapping.mapCartToTex(tmp0, tmp1, tmp2, out);
		}
		else
		{
			double dirx = position.x - midpoint.x, diry = position.y - midpoint.y, dirz = position.z - midpoint.z;
			textureMapping.mapCartToTex(dirx, diry, dirz, out);
		}
	}



	public void update()
	{
		directionLengthQ = this.direction.dot();
		directionLength = Math.sqrt(directionLengthQ);
		invDirectionLengthQ = 1 / directionLengthQ;
		invDirectionLength = 1 / directionLength;
		directionNormalized.set(direction, invDirectionLength);
		updateIOR();
		radiusGeometricQ = maxRadiusGeometric * maxRadiusGeometric;
		minRadiusGeometricQ = minRadiusGeometric * minRadiusGeometric;
		double minRatio = minRadiusGeometricQ * invDirectionLengthQ;
		double maxRatio = radiusGeometricQ * invDirectionLengthQ;
		switch (surf)
		{
		case CUSTOM:
		{
			double tmp = 1 - (1 + conicConstant) * minRatio;
			dotProdLowerBound = minRatio / (1 + (tmp > 0 ? Math.sqrt(tmp) : -Math.sqrt(-tmp))) - 1;
			tmp = 1 - (1 + conicConstant) * maxRatio;
			dotProdUpperBound = maxRatio / (1 + (tmp > 0 ? Math.sqrt(tmp) : -Math.sqrt(-Math.max(tmp, -1)))) - 1;
			if ((dotProdUpperBound + 1) * (1 + conicConstant) > 2)
			{
				dotProdUpperBound = 2 / (1 + conicConstant) - 1;
			}
			break;
		}
		case CYLINDER:
			dotProdLowerBound = -minRatio;// * directionLength / minRadiusGeometric;
			dotProdUpperBound = -maxRatio;// * directionLength / radiusGeometric;
			dotProdUpperBound2 = -maxRadiusGeometric;
			dotProdLowerBound2 = -minRadiusGeometric;
			break;
		case FLAT:
			break;
		case HYPERBOLIC:
			dotProdLowerBound = Math.sqrt(1 + minRatio)-2;
			dotProdUpperBound = Math.sqrt(1 + maxRatio)-2;
			break;
		case PARABOLIC:
			dotProdLowerBound = 0.5 * minRatio-1;
			dotProdUpperBound = 0.5 * maxRatio-1;
			break;
		case SPHERICAL:
			if (maxRadiusGeometric < directionLength)
			{
				dotProdUpperBound = Math.sqrt(1 - maxRatio);
				maxArcOpen = Math.asin(maxRadiusGeometric * invDirectionLength);
			}
			else
			{
				maxArcOpen = Math.PI - Math.asin(directionLength / maxRadiusGeometric);
				dotProdUpperBound = -Math.sqrt(1 - 1 / maxRatio);
			}
			//dotProdUpperBound = Math.cos(arcOpen) * this.directionLengthQ;
			if (minRadiusGeometric < directionLength)
			{
				minArcOpen = Math.asin(minRadiusGeometric * invDirectionLength);
			}
			else
			{
				minArcOpen = Math.PI - Math.asin(directionLength / minRadiusGeometric);
			}
			dotProdLowerBound = Math.cos(minArcOpen);
			//double tmp = (this.directionLengthQ - this.radiusGeometricQ) * this.directionLengthQ;
			//dotProdBound = (tmp > 0 ? Math.sqrt(tmp) : -Math.sqrt(-tmp));
			break;
		default:
			break;

		}

		Geometry.getOrthorgonalZMatrix(direction, matSurfaceToGlobal); //Creates a matrix with mat*e1=direction and mat*e2 and mat*e3 orthorgonal
		matSurfaceToGlobal.setCol(3, midpoint);
		matGlobalToSurface.invert(matSurfaceToGlobal);
		if (surf != SurfaceType.CYLINDER)
		{
			dotProdLowerBound2 = dotProdLowerBound * directionLength;
			dotProdUpperBound2 = dotProdUpperBound * directionLength;
			if (surf != SurfaceType.SPHERICAL)
			{
				dotProdLowerBound2 += directionLength;
				dotProdUpperBound2 += directionLength;
			}
		}
	}

	public static final double asinh(double x)
	{
		return Math.log(x + Math.sqrt(x*x + 1.0));
	}

	public final int getMeshVertexCount(int latitudes, int longitudes)
	{
		return longitudes * latitudes + (surf == SurfaceType.CYLINDER || minRadiusGeometric > 0 ? 0 : 1 - latitudes);
	}

	public void getMeshVertices(int latitudes, int longitudes, float vertices[])
	{
		int index = 0;
		double z = 0;
		double multiply = 1. / (longitudes - 1);
		double add = 0;
		switch (surf)
		{
			case FLAT:
				multiply *= (maxRadiusGeometric - minRadiusGeometric) * this.invDirectionLength;
				add = minRadiusGeometric * this.invDirectionLength;
				break;
			case SPHERICAL:
				add = minArcOpen;
				multiply *= maxArcOpen - minArcOpen;
				break;
			case HYPERBOLIC:
			case PARABOLIC:
				add = minRadiusGeometric;
				multiply *= (maxRadiusGeometric - minRadiusGeometric) * this.invDirectionLength;
				break;
			case CUSTOM:
				multiply *= dotProdUpperBound  - dotProdLowerBound;
				add = dotProdLowerBound + 1;
				break;
			case CYLINDER:
				add = minRadiusGeometric * this.invDirectionLength;
				multiply *= (maxRadiusGeometric - minRadiusGeometric) * this.invDirectionLength;
				break;
			default:
				break;
		}
		double s = Math.PI * 2 / latitudes;
		for (int ri = 0; ri < longitudes; ++ri)
		{
			double r = multiply * ri + add;
			switch (surf)
			{
				case FLAT:
					break;
				case HYPERBOLIC:
					z = 2-Math.sqrt(r * r + 1);
					break;
				case PARABOLIC:
					z = 1 - r * r * 0.5;
					break;
				case SPHERICAL:
					z = Math.cos(r);
					r = Math.sin(r);
					break;
				case CUSTOM:
					z = 1 - r;
					r = Math.sqrt(r * (2 - r * (1 + conicConstant)));
					break;
				case CYLINDER:
					z = r;
					r = 1;
				default:
					break;
			}
			if (ri != 0 || surf == SurfaceType.CYLINDER || minRadiusGeometric > 0)
			{
				for (int rhoi = 0; rhoi < latitudes; ++rhoi, index += 3)
				{
					double rho = rhoi * s;
					matSurfaceToGlobal.rdotAffine(r * Math.sin(rho), r * Math.cos(rho), z, vertices, index);
				}
			}
			else
			{
				matSurfaceToGlobal.rdotAffine(0, 0,z, vertices, index);
				index += 3;
			}
		}
	}

	public int[] getMeshFaces(int latitudes, int longitudes, int faces[])
	{
		if (surf == SurfaceType.CYLINDER || minRadiusGeometric > 0)
		{
			return BufferUtils.fillWithCylinderIndexData(latitudes, longitudes, faces);
		}
		return BufferUtils.fillWithRadialIndexData(latitudes, longitudes, faces);
	}

	public double evaluate_inner_outer(Vector3d position)
	{
		double x = position.x - this.midpoint.x;
		double y = position.y - this.midpoint.y;
		double z = position.z - this.midpoint.z;

		switch (surf)
		{
        /*case HYPERBOLIC:
            z = 2-Math.sqrt(r * r + 1);
            break;
        case PARABOLIC:
            z = 1 - r * r * 0.5;
            break;
        case SPHERICAL:
            z = Math.cos(r);
            r = Math.sin(r);
            break;
        case CUSTOM:
            z = 1 - r;
            r = Math.sqrt(r * (2 - r * (1 + conicConstant)));*/

			case FLAT:
			{
				return -this.directionNormalized.dot(x, y, z);
			}
			case SPHERICAL:
			{
				return (x * x + y * y + z * z) / this.directionLengthQ - 1;
			}
			case CUSTOM:
			{
			    double mdir = this.directionNormalized.dot(x, y, z);
			    double dirdot = directionLength - mdir;
			    return dirdot * (2 * directionLength - dirdot * (1 + conicConstant)) - x * x - y * y - z * z + mdir * mdir;
			}
			case CYLINDER:
			{
				double dotprod = this.directionNormalized.dot(x, y, z);
				double dist = this.directionNormalized.distanceQ(dotprod, x, y, z);
				return Math.max(dotprod + dotProdUpperBound2, Math.max(- (dotProdLowerBound2 + dotprod), dist - directionLengthQ));
			}
    		case HYPERBOLIC:
    		{
    		    double dirdot = this.directionNormalized.dot(x, y, z);
    		    return 2 * directionLength - dirdot - Math.sqrt(x * x + y * y + z * z - dirdot * dirdot + directionLengthQ);
    		}
    		case PARABOLIC:
    		{
    		    double dirdot = this.directionNormalized.dot(x,y,z) - directionLength;
    		    return x * x + y * y + z * z - dirdot * dirdot - directionLengthQ;
    		}
		default:
			break;

		}
		return Double.NaN;
	}

	@Override
	public Intersection getIntersection(Vector3d position, Vector3d direction, Intersection intersection, double lowerBound, double upperBound)
	{
		final double x = position.x - this.midpoint.x, y = position.y - this.midpoint.y, z = position.z - this.midpoint.z;
		switch (surf)
		{
			case FLAT:
			{
				double alpha = -this.direction.dot(x,y,z) / this.direction.dot(direction);
				if (lowerBound < alpha && alpha < upperBound)
				{
					//double distanceQ = tmp0.dot() + (directiondot *alpha + 2 * tmp0.dot(direction))*alpha;
					double distanceQ = direction.distanceQ(-alpha, x, y, z);
					if ((this.minRadiusGeometricQ < distanceQ && distanceQ < this.radiusGeometricQ) != invertInsideOutside)
					{
						intersection.position.set(position, direction,alpha);
						intersection.normal.set(this.direction);
						intersection.distance = alpha;
						intersection.object = this;
						return intersection;
					}
				}
				break;
			}
			case HYPERBOLIC:
			{
				double dirproj = this.directionLength-this.directionNormalized.dot(x,y,z);
				double c = x * x + y * y + z * z - 2 * dirproj * dirproj - this.directionLengthQ;
				double scal = this.directionNormalized.dot(direction);
				double b = direction.dot(x,y,z) + 2 * scal * dirproj;
				double a = 1 / (1 - 2 * scal * scal);
				c *= a;
				b *= a;
				double sqrt = Math.sqrt(b*b-c);
				do {
					double alpha = -b- sqrt;
					if (lowerBound < alpha && alpha < upperBound)
					{
						double dotProd = dirproj - scal * alpha;
						if (this.dotProdLowerBound2 <= dotProd && dotProd <= this.dotProdUpperBound2 )
						{
							double dax = direction.x * alpha, day = direction.y * alpha, daz = direction.z * alpha;
							intersection.position.setAdd(position, dax, day, daz);
							intersection.normal.set(x + dax,y + day ,z + daz, this.directionNormalized,2 * dotProd);
							intersection.object = this;
							intersection.distance = alpha;
							return intersection;
						}
					}
				}while ((sqrt=-sqrt) < 0);
				break;
			}
			case PARABOLIC:
			{
				double dirproj = this.directionLength-this.directionNormalized.dot(x,y,z);
				double c = x * x + y * y + z * z - this.directionLengthQ - dirproj * dirproj;
				double scal = this.directionNormalized.dot(direction);
				double b = direction.dot(x,y,z) + scal * dirproj;
				double a = 1 / (1 - scal * scal);
				c *= a;
				b *= a;
				double sqrt = Math.sqrt(b*b-c);
				do {
					double alpha = -b- sqrt;
					if (lowerBound < alpha && alpha < upperBound)
					{
						double dotProd = dirproj - scal * alpha;
						if (this.dotProdLowerBound2 <= dotProd && dotProd <= this.dotProdUpperBound2)
						{
							double dax = direction.x * alpha, day = direction.y * alpha, daz = direction.z * alpha;
							intersection.position.setAdd(position, dax, day, daz);
							intersection.normal.set(x + dax,y + day,z + daz, this.directionNormalized,dotProd);
							intersection.object = this;
							intersection.distance = alpha;
							return intersection;
						}
					}
				}while ((sqrt=-sqrt) < 0);
				break;
			}
			case CUSTOM:
			{
				double dirproj = this.directionLength - this.directionNormalized.dot(x,y,z);
				double c = x * x + y * y + z * z + this.conicConstant * dirproj * dirproj - this.directionLengthQ;
				double scal = this.directionNormalized.dot(direction);
				double b = direction.dot(x,y,z) - this.conicConstant * scal * dirproj;
				double a = 1 / (1 + this.conicConstant * scal * scal);

				c *= a;
				b *= a;
				double sqrt = Math.sqrt(b*b-c);
				do {
					double alpha = -b- sqrt;
					if (lowerBound < alpha && alpha < upperBound)
					{
						double dotProd = dirproj - scal * alpha;
						if (dotProd <= dotProdUpperBound2 && dotProd>= dotProdLowerBound2)
						{
							intersection.position.set(position, direction,alpha);
							intersection.normal.set(intersection.position, this.midpoint, this.directionNormalized,-this.conicConstant * dotProd);
							intersection.object = this;
							intersection.distance = alpha;
							return intersection;
						}
					}
				}while ((sqrt=-sqrt) < 0);
				break;
			}
			case SPHERICAL:
			{
				double c = x * x + y * y + z * z - this.directionLengthQ;
				double b = direction.dot(x,y,z);
				double sqrt = b * b - c;
				if (sqrt >= 0)
				{
					sqrt = Math.sqrt(sqrt);
					double dirproj = this.directionNormalized.dot(x,y,z);
					double scal = this.directionNormalized.dot(direction);
					do {
						double alpha = -b - sqrt;
						if (lowerBound < alpha && alpha < upperBound)
						{
							//intersection.normal.set(tmp0, direction,alpha);
							//double dotProd = intersection.normal.dot(this.directionNormalized);
							double dotProd = dirproj + scal * alpha;

							if (dotProd >= dotProdUpperBound2 && dotProd <= dotProdLowerBound2)
							{
								double dax = direction.x * alpha, day = direction.y * alpha, daz = direction.z * alpha;
								intersection.position.setAdd(position, dax, day, daz);
								intersection.normal.set(x + dax,y + day,z + daz);
								intersection.object = this;
								intersection.distance = alpha;
								return intersection;
							}
						}
					}while ((sqrt=-sqrt) < 0);
				}
				break;
			}
			case CYLINDER:
			{
				double dirproj = -this.directionNormalized.dot(x,y,z);
				double c = x * x + y * y + z * z - dirproj * dirproj - this.directionLengthQ;
				double scal = this.directionNormalized.dot(direction);
				double b = direction.dot(x,y,z) + dirproj * scal;
				double a = 1 / (1 - scal * scal);
				c *= a;
				b *= a;
				double sqrt = Math.sqrt(b*b-c);
				do {
					double alpha = -b- sqrt;
					if (lowerBound < alpha && alpha < upperBound)
					{
						double dotProd = dirproj - scal * alpha;
						if (dotProd >= dotProdUpperBound2 && dotProd<= dotProdLowerBound2)
						{
							intersection.position.set(position, direction,alpha);
							intersection.normal.set(intersection.position, this.midpoint, this.directionNormalized, dotProd);
							intersection.object = this;
							intersection.distance = alpha;
							return intersection;
						}
					}
				}while ((sqrt=-sqrt) < 0);
				break;
			}
			default:
				break;

		}
		return null;
	}

	@Override
	public void densityCompensation(int width, int height, int imageColorArray[], int channels, int stride)
	{
		textureMapping.densityCompensation(width, height, imageColorArray, channels, stride);
	}

	public void inverseDensityCompensation(int width, int height, int imageColorArray[], int channels, int stride)
	{
		textureMapping.inverseDensityCompensation(width, height, imageColorArray, channels, stride);
	}
}