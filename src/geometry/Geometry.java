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
package geometry;

import java.awt.geom.Rectangle2D;
import java.text.ParseException;
import java.util.Arrays;

import maths.algorithm.Calculate;
import scene.object.FloatVectorObject;
import util.ArrayUtil;
import util.StringUtils;
import util.data.DoubleArrayList;
import util.data.IntegerArrayList;

public class Geometry {
	
	public static final class NearestPointCalculator
	{
		final int dim;
		final double mat[];
		int nCols;
		int count;
		public NearestPointCalculator(int dim)
		{
			this.dim = dim;
			this.nCols = dim + 1;
			this.mat = new double[dim * nCols];
		}
		
		public final void reset()
		{
			Arrays.fill(mat, 0);
			count = 0;
		}
		
		public final void addPoint(float positions[], float directions[], int i)
		{
			float dirlen = 0;
			float scalar = 0;
			for (int j = 0; j < dim; ++j)
			{
				int index = i + j;
				float dir = directions[index];
				dirlen += dir * dir;
				scalar += dir * positions[index];
			}
			scalar *= (dirlen = 1 / dirlen);
			for (int j = 0; j < dim; ++j)
			{
				int index = i + j;
				float dir = directions[index] * dirlen;
				int jc = j * nCols;
				mat[jc + dim] += scalar * directions[index] - positions[index];
				for (int k = 0; k <= j; ++k)
				{
					mat[jc + k] += dir * directions[i + k];
				}
			}
			++count;
		}
		
		public final void calculate()
		{
			for (int i = 0; i < mat.length; i += nCols + 1)
			{
				mat[i] -= count;
			}
			for (int i = 0; i < dim; ++i)
			{
				for (int j = 0; j < i; ++j)
				{
					mat[j * nCols + i] = mat[i * nCols + j];
				}
			}
			Calculate.toRREF(mat, dim);
		}
		
		public final double get(int index)
		{
			return mat[index * nCols + dim];
		}
		
		public final void get(Vectord vec)
		{
			for (int i = 0; i < dim; ++i)
			{
				vec.setElem(i, mat[i * nCols + dim]);
			}
		}
		
		public final void get(float[] res)
		{
			for (int i = 0; i < dim; ++i)
			{
				res[i] = (float)mat[i * nCols + dim];
			}
		}
	}
	
	public static final void parse(String str, Vectord vec) throws ParseException
	{
		if (str.charAt(0) != '(' || str.charAt(str.length() - 1) != ')')
		{
			throw new ParseException(str, 0);
		}
		int begin = 1; int end = str.indexOf(',');
		for (int index = 0; end != -1; ++index)
		{
			vec.setElem(index, StringUtils.parseDouble(str, begin, end));
			begin = end;
			end = str.indexOf(begin, ',');
		}
	}
	
	public static final void volumeToMesh(int[] data, int width, int height, int depth, double mid, IntegerArrayList faceIndices, DoubleArrayList vertexPositions)
	{
		int offsets[] = new int[8];
		int cubeOffsets[] = new int[8];
		int vertexIndices[] = new int[(width - 1) * (height - 1) * (depth - 1) * 3];
		Arrays.fill(vertexIndices, -1);
		boolean inside[] = new boolean[8];
		boolean visited[] = new boolean[8];
		boolean closedSet[] = new boolean[visited.length];
		for (int i = 0; i < 3; ++i)
		{
			for (int j = 0; j < (1 << i); ++j)
			{
				offsets[j + (1 << i)] = offsets[j] + (i == 0 ? 1 : i == 1 ? width : width * height);
				cubeOffsets[j + (1 << i)] = cubeOffsets[j] + (i == 0 ? 1 : i == 1 ? (width - 1): (width - 1)* (height-1));
			}
		}
		IntegerArrayList searchStack = new IntegerArrayList();
		
		for (int z = 0, index = 0; z < depth - 1; ++z, ++index)
		{
			for (int y = 0; y < height - 1; ++y, ++index)
			{
				for (int x = 0; x < width - 1; ++x, ++index)
				{
					index = x + (width * (y + height * z));
					boolean is_cutted = false;
					{
						boolean first = inside[0] = data[index] > mid;
						for (int bitmask = 1; bitmask < 8; ++bitmask)
						{
							is_cutted |= (inside[bitmask] = (data[index + offsets[bitmask]] > mid)) != first;
						}
					}
					if (is_cutted)
					{
						int cubeIndex = x + (width - 1) * (y + (height - 1) * z);
						Arrays.fill(visited, false);
						for (int bitmask = 0; bitmask < 8; ++bitmask)
						{
							Arrays.fill(closedSet, false);
							int innerVertex = bitmask;
							int inOutAxis = -1;
							if (inside[bitmask] && !visited[bitmask])
							{
								visited[bitmask] = closedSet[bitmask] = true;
								searchStack.add(bitmask);
								while(!searchStack.isEmpty())
								{
									int current = searchStack.pop();
									for (int axis = 0; axis < 3;++axis)
									{
										int neighbour = current ^ (1 << axis);
										
										if (inside[neighbour] && !visited[neighbour])
										{
											searchStack.add(neighbour);
											visited[neighbour] = closedSet[neighbour] = true;
										}
										if (!inside[neighbour])
										{
											innerVertex = current;
											inOutAxis = axis;
										}
									}
								}
							}
							if (inOutAxis == -1)
							{
								continue;
							}
							int facePoint0 = -1, facePoint1 = -1;
							final int startInnerVertex = innerVertex;
							final int startInOutAxis = inOutAxis;
							while(true)
							{
								final int outerVertex = innerVertex ^ (1 << inOutAxis);
								int min = innerVertex & outerVertex;
								int max = innerVertex | outerVertex;
								int vIndex = (cubeIndex + cubeOffsets[min]) * 3 + inOutAxis;
								int facePoint2 = vertexIndices[vIndex];
								if (facePoint2 == -1)
								{
									double xp = x + ((min >> 0) & 1);
									double yp = y + ((min >> 1) & 1);
									double zp = z + ((min >> 2) & 1);
									double v0 = data[index + offsets[min]]- mid, v1 = data[index + offsets[max]] - mid;
									double alpha = v0 / (v0 - v1);
									if (alpha < 0 || alpha > 1)
									{
										throw new RuntimeException("alpha not in range: " + alpha);
									}
									if (alpha <= 0.00001) //clip near to lattice values to remove small triangles
									{
										vIndex = (cubeIndex + cubeOffsets[min]) * 3;
										facePoint2 = vertexIndices[vIndex];
									}
									if (alpha >= 0.99999) //clip near to lattice values to remove small triangles
									{
										vIndex = (cubeIndex + cubeOffsets[max]) * 3;
										facePoint2 = vertexIndices[vIndex];
									}
									if (facePoint2 == -1)
									{
										facePoint2 = vertexIndices[vIndex] = vertexPositions.size() / 3;
										vertexPositions.add(xp, yp, zp);
										int addIndex = vertexPositions.size() - 3 + inOutAxis;
										vertexPositions.set(addIndex, vertexPositions.getD(addIndex) + alpha);
									}
								}
								if (facePoint0 != -1)
								{
									if (facePoint2 != facePoint1 && facePoint0 != facePoint1 && facePoint0 != facePoint2)
									{
										faceIndices.add(facePoint0, facePoint1, facePoint2);
									}
								}
								else
								{
									facePoint0 = facePoint1;
								}
								facePoint1 = facePoint2;
								
								final int newAxis = (((innerVertex ^ (innerVertex >> 1) ^ (innerVertex >> 2)) & 1) + inOutAxis + 1)%3;
								final int newInner = innerVertex ^ (1 << newAxis);
								final int newOuter = outerVertex ^ (1 << newAxis);
								if (closedSet[newInner] && !closedSet[newOuter] && (newInner != startInnerVertex || inOutAxis != startInOutAxis))
								{
									innerVertex = newInner;
								}
								else if (!closedSet[newInner] && (innerVertex != startInnerVertex || newAxis != startInOutAxis))
								{
									inOutAxis = newAxis;
								}
								else if (closedSet[newOuter] && (newOuter != startInnerVertex || newAxis != startInOutAxis))
								{
									innerVertex = newOuter;
									inOutAxis = newAxis;
								}
								else
								{
									break;
								}
							}
						}
					}
				}
			}
		}
		System.out.println("meshsize: " + vertexPositions.size() / 3 + " " + faceIndices.size() / 3 + " cutpoint " + mid);
	}
/*	public static final void volumeToMesh(int[] data, int width, int height, int depth, double low, double high, IntegerArrayList faceIndices, DoubleArrayList vertexPositions)
	{
		low = ArrayUtil.min(data);
		high = ArrayUtil.max(data);
		System.out.println("range: " + low + " " + high);
		double mid = (low + high) * 0.5;
		int offsets[] = new int[8];
		int cubeOffsets[] = new int[8];
		int vertexIndices[] = new int[(width - 1) * (height - 1) * (depth - 1) * 3];
		Arrays.fill(vertexIndices, -1);
		boolean inside[] = new boolean[8];
		boolean visited[] = new boolean[8];
		for (int i = 0; i < 3; ++i)
		{
			for (int j = 0; j < (1 << i); ++j)
			{
				offsets[j + (1 << i)] = offsets[j] + (i == 0 ? 1 : i == 1 ? width : width * height);
				cubeOffsets[j + (1 << i)] = offsets[j] + (i == 0 ? 1 : i == 1 ? (width - 1): (width - 1)* (height-1));
			}
		}
		IntegerArrayList searchStack = new IntegerArrayList();
		
		for (int z = 0; z < depth - 1; ++z)
		{
			for (int y = 0; y < height - 1; ++y)
			{
				for (int x = 0; x < width - 1; ++x)
				{
					int index = x + width * (y + height * z);
					int cubeIndex = x + (width - 1) * (y + (height - 1) * z);
					boolean is_cutted = false;
					{
						boolean first = inside[0] = data[index] > mid;
						for (int bitmask = 1; bitmask < 8; ++bitmask)
						{
							is_cutted |= (inside[bitmask] = (data[index + offsets[bitmask]] > mid)) != first;
						}
					}
					if (!is_cutted)
					{
						continue;
					}
					System.out.println("found cube " + x + " " + y + " " + z);
					
					boolean visited2[] = new boolean[24];
					for (int bitmask = 0; bitmask < 8; ++bitmask)
					{
						if (inside[bitmask])
						{
							for (int axis = 0; axis < 3; ++axis)
							{
								int min = bitmask & (1 << axis);
								if (!inside[bitmask ^ (1 << axis)] && !visited2[min * 3 + axis])
								{
									visited2[min * 3 + axis] = true;
									
									
								}
							}
						}
					}
					
					
					Arrays.fill(visited, false);
					for (int bitmask = 0; bitmask < 8; ++bitmask)
					{
						if (inside[bitmask] && !visited[bitmask])
						{
							int facePoint0 = -1, facePoint1 = -1;
							visited[bitmask] = true;
							searchStack.add(bitmask);
							while(!searchStack.isEmpty())
							{
								int current = searchStack.pop();
								System.out.println("search from " + current);
								for (int axis = 0; axis < 3;++axis)
								{
									int neighbour = current ^ (1 << axis);
									if (!inside[neighbour])
									{
										int min = current & neighbour;
										int max = current | neighbour;
										System.out.println(current + " " + neighbour + " " + min + " " + max);
										int facePoint2 = vertexIndices[(cubeIndex + cubeOffsets[min]) * 3 + axis];
										if (facePoint2 == -1)
										{
											System.out.println("data " + data[index + offsets[min]] + " " + data[index + offsets[max]] + " mid: " + mid	);
											double xp = x + ((min >> 0) & 1);
											double yp = y + ((min >> 1) & 1);
											double zp = z + ((min >> 2) & 1);
											double v0 = data[index + offsets[min]]- mid, v1 = data[index + offsets[max]] - mid;
											double alpha = v0 / (v0 - v1);
											System.out.println("v " + v0 + " " + v1 + " " + alpha);
											if (alpha < 0 || alpha > 1)
											{
												throw new RuntimeException("alpha not in range: " + alpha);
											}
											switch(axis)
											{
												case 0: xp += alpha;break;
												case 1: yp += alpha;break;
												case 2: zp += alpha;break;
											}
											facePoint2 = vertexIndices[(cubeIndex + cubeOffsets[min]) * 3 + axis] = vertexPositions.size() / 3;
											System.out.println("add vertex " + xp + " " + yp + " " + zp);
											vertexPositions.add(xp);
											vertexPositions.add(yp);
											vertexPositions.add(zp);
										}
										if (facePoint0 != -1)
										{
											System.out.println("add face " + facePoint0 + " " + facePoint1 + " " + facePoint2);
											faceIndices.add(facePoint0);
											faceIndices.add(facePoint1);
											faceIndices.add(facePoint2);
										}
										facePoint0 = facePoint1;
										facePoint1 = facePoint2;
									}
									if (inside[neighbour] && !visited[neighbour])
									{
										searchStack.add(neighbour);
										visited[neighbour] = true;
									}
								}
							}
						}
					}
				}
			}
		}
	}
*/	
	public static final void parse(String str, Vectorf vec) throws ParseException
	{
		if (str.charAt(0) != '(' || str.charAt(str.length() - 1) != ')')
		{
			throw new ParseException(str, 0);
		}
		int begin = 1; int end = str.indexOf(',');
		for (int index = 0; end != -1; ++index)
		{
			vec.setElem(index, StringUtils.parseFloat(str, begin, end));
			begin = end;
			end = str.indexOf(begin, ',');
		}
	}
	
	public static final void parse(String str, Vectori vec) throws ParseException
	{
		if (str.charAt(0) != '(' || str.charAt(str.length() - 1) != ')')
		{
			throw new ParseException(str, 0);
		}
		int begin = 1; int end = str.indexOf(',');
		for (int index = 0; end != -1; ++index)
		{
			vec.setElem(index, StringUtils.parseInt(str, begin, end, 10));
			begin = end;
			end = str.indexOf(begin, ',');
		}
	}
	
    public final static void calcFaceNormals (int sizeX, int sizeY, FloatVectorObject vertex, float faceNormal[]){
		if (sizeX * sizeY != vertex.size()){
    		throw new ArrayIndexOutOfBoundsException("Vertex Count doesn't match the size: " + sizeX + ',' + sizeY + ") vertexCount: " + vertex.size());
    	}

		if ((sizeX - 1) * (sizeY - 1) != faceNormal.length * 3){
    		throw new ArrayIndexOutOfBoundsException("vertexNormal Count doesn't match the size: " + sizeX + ',' + sizeY + ") vertexNormalCount: " + vertex.size());
    	}
		final float vertexX[] = vertex.x, vertexY[] = vertex.y, vertexZ[] = vertex.z;
		int normalIndex = 0;
    	int index00 = 0;
        for (int i=1;i<sizeX;i++){
            for (int j=1;j<sizeY;j++){
            	final int index10 = index00 + sizeX;
            	final int index01 = index00 + 1, index11 = index10 + 1;
                final float ax = vertexX[index11] - vertexX[index00];
                final float ay = vertexY[index11] - vertexY[index00];
                final float az = vertexZ[index11] - vertexZ[index00];
                
                final float bx = vertexX[index01] - vertexX[index10];
                final float by = vertexY[index01] - vertexY[index10];
                final float bz = vertexZ[index01] - vertexZ[index10];

                final float xn=ay*bz-az*by;
                final float yn=az*bx-ax*bz;
                final float zn=ax*by-ay*bx;
                final float len = 1/(float)Math.sqrt(xn * xn + yn * yn + zn * zn);

                faceNormal[normalIndex]=xn*len;
                faceNormal[normalIndex + 1]=yn*len;
                faceNormal[normalIndex + 2]=zn*len;
                normalIndex += 3;
                ++index00;
            }
            ++index00;
        }
    }
    
    public final static void calcVertexNormals2(final int sizeX, final int sizeY, FloatVectorObject vertex, FloatVectorObject vertexNormal, boolean xCyclic, boolean yCyclic){
		if (sizeX * sizeY != vertex.size()){
    		throw new ArrayIndexOutOfBoundsException("Vertex Count doesn't match the size: (" + sizeX + ',' + sizeY + ") vertexCount: " + vertex.size());
    	}

		if (sizeX * sizeY != vertexNormal.size()){
    		throw new ArrayIndexOutOfBoundsException("vertexNormal Count doesn't match the size: (" + sizeX + ',' + sizeY + ") vertexNormalCount: " + vertex.size());
    	}
    	
    	final float vertexX[] = vertex.x, vertexY[] = vertex.y, vertexZ[] = vertex.z;
		final float vertexNormalX[] = vertexNormal.x, vertexNormalY[] = vertexNormal.y, vertexNormalZ[] = vertexNormal.z;
    	final int sizeXMin1 = sizeX-1, sizeYMin1 = sizeY-1;
    	for (int i=0;i<sizeX;i++){
            final int middleIndexX = i*sizeY;
            final int lowIndexX = i==0 ? (xCyclic ? sizeY * sizeXMin1 : 0) : middleIndexX - sizeY;
            final int highIndexX = i== sizeXMin1 ? (xCyclic ? 0 : middleIndexX) : middleIndexX + sizeY;
            
    		for (int j=0;j<sizeY;j++){
    			/**
    			 * Die Indices sind folgendermassen verteilt:
    			 * 			i-1		i		i+1
    			 * 	j-1				01
    			 * 	j		10		11		12
    			 * 	j+1				21
    			 * Dabei sind die verschobenen Werte nach oben und unten begrentzt
    			 */
                final int index11 = middleIndexX + j;
                final int index01 = lowIndexX + j, index21 = highIndexX + j;
                final int index10 = middleIndexX + (j==0 ? 0 : j-1);
                final int index12 = middleIndexX + (j==sizeYMin1 ? sizeYMin1 : j+1);
                   
                final float ax = vertexX[index01] - vertexX[index21];
                final float ay = vertexY[index01] - vertexY[index21];
                final float az = vertexZ[index01] - vertexZ[index21];
                final float bx = vertexX[index10] - vertexX[index12];
                final float by = vertexY[index10] - vertexY[index12];
                final float bz = vertexZ[index10] - vertexZ[index12];

                final float xn=ay*bz-az*by;
                final float yn=az*bx-ax*bz;
                final float zn=ax*by-ay*bx;
                final float len = 1/(float)Math.sqrt(xn * xn + yn * yn + zn * zn);

                vertexNormalX[index11]=xn*len;
                vertexNormalY[index11]=yn*len;
                vertexNormalZ[index11]=zn*len;    		
            }
    	}
    }
    
    public static final void add(double data[], int begin, int end, int stride, Vectord vec)
    {
    	for(; begin < end; begin += stride)
    	{
    		vec.add(data, begin);
    	}
    }

    public final static void calcQuadMeshVertexNormals(float vertices[], int faces[], float vertexNormals[])
    {
		Arrays.fill(vertexNormals, 0);
    	for (int face = 0; face < faces.length; face += 4)
    	{
            final int index00 = 3 * faces[face];
            final int index01 = 3 * faces[face + 1];
            final int index11 = 3 * faces[face + 2];
            final int index10 = 3 * faces[face + 3];
            final float ax = vertices[index00] - vertices[index11];
            final float ay = vertices[index00 + 1] - vertices[index11 + 1];
            final float az = vertices[index00 + 2] - vertices[index11 + 2];
            final float bx = vertices[index10] - vertices[index01];
            final float by = vertices[index10 + 1] - vertices[index01 + 1];
            final float bz = vertices[index10 + 2] - vertices[index01 + 2];

            final float xn=ay*bz-az*by;
            final float yn=az*bx-ax*bz;
            final float zn=ax*by-ay*bx;
            final float len = 1/(float)Math.sqrt(xn * xn + yn * yn + zn * zn);
            
            for (int i = face; i < face + 4; ++i)
            {
            	int index = faces[i] * 3;
		        vertexNormals[index] += xn * len;
		        vertexNormals[index + 1] += yn * len;
		        vertexNormals[index + 2] += zn * len;
            }
        }
    	for (int vertex = 0; vertex < vertexNormals.length; vertex += 3)
    	{
    		float xn = vertexNormals[vertex];
    		float yn = vertexNormals[vertex + 1];
    		float zn = vertexNormals[vertex + 2];
    		final float len = 1/(float)Math.sqrt(xn * xn + yn * yn + zn * zn);
    		vertexNormals[vertex] = xn * len;
    		vertexNormals[vertex + 1] = yn * len;
    		vertexNormals[vertex + 2] = zn * len;
    	}
    }
    
    
    
    public static final double crossq(double x1, double y1, double z1, double x2, double y2, double z2)
    {
    	final double x = y1 * z2 - z1 * y2;
    	final double y = z1 * x2 - x1 * z2;
    	final double z = x1 * y2 - y1 * x2;
    	return x * x + y * y + z * z;
    }
    
    public static final double crossdot(double x1, double y1, double z1, double x2, double y2, double z2, double n0, double n1, double n2)
    {
    	return n0 * (y1 * z2 - z1 * y2) + n1 * (z1 * x2 - x1 * z2) + n2 * (x1 * y2 - y1 * x2);
    }
    
    public final static void calcTriangleMeshVertexNormals(float vertices[], int faces[], float vertexNormals[])
    {
		Arrays.fill(vertexNormals, 0);
    	for (int face = 0; face < faces.length; face += 3)
    	{
            final int index00 = 3 * faces[face];
            final int index01 = 3 * faces[face + 1];
            final int index10 = 3 * faces[face + 2];
            final float ax = vertices[index10] - vertices[index00];
            final float ay = vertices[index10 + 1] - vertices[index00 + 1];
            final float az = vertices[index10 + 2] - vertices[index00 + 2];
            final float bx = vertices[index01] - vertices[index00];
            final float by = vertices[index01 + 1] - vertices[index00 + 1];
            final float bz = vertices[index01 + 2] - vertices[index00 + 2];

            final float xn=ay*bz-az*by;
            final float yn=az*bx-ax*bz;
            final float zn=ax*by-ay*bx;
            final float len = 1/(float)Math.sqrt(xn * xn + yn * yn + zn * zn);
            
            for (int i = face; i < face + 3; ++i)
            {
            	int index = faces[i] * 3;
		        vertexNormals[index] += xn * len;
		        vertexNormals[index + 1] += yn * len;
		        vertexNormals[index + 2] += zn * len;
            }
        }
    	for (int vertex = 0; vertex < vertexNormals.length; vertex += 3)
    	{
    		float xn = vertexNormals[vertex];
    		float yn = vertexNormals[vertex + 1];
    		float zn = vertexNormals[vertex + 2];
    		final float len = 1/(float)Math.sqrt(xn * xn + yn * yn + zn * zn);
    		vertexNormals[vertex] = xn * len;
    		vertexNormals[vertex + 1] = yn * len;
    		vertexNormals[vertex + 2] = zn * len;
    	}
    }
    	
    public final static void calcTriangleMeshVertexFaceNormals(double vertices[], int faces[], double vertexNormals[], double faceNormals[])
    {
		Arrays.fill(vertexNormals, 0);
    	for (int face = 0; face < faces.length; face += 3)
    	{
            final int index00 = 3 * faces[face];
            final int index01 = 3 * faces[face + 1];
            final int index10 = 3 * faces[face + 2];
            final double ax = vertices[index10] - vertices[index00];
            final double ay = vertices[index10 + 1] - vertices[index00 + 1];
            final double az = vertices[index10 + 2] - vertices[index00 + 2];
            final double bx = vertices[index01] - vertices[index00];
            final double by = vertices[index01 + 1] - vertices[index00 + 1];
            final double bz = vertices[index01 + 2] - vertices[index00 + 2];

            double xn=ay*bz-az*by;
            double yn=az*bx-ax*bz;
            double zn=ax*by-ay*bx;

            faceNormals[face] = xn;
            faceNormals[face + 1] = yn;
            faceNormals[face + 2] = zn;
            
            /*final double len = 1/Math.sqrt(xn * xn + yn * yn + zn * zn);
            
            xn *= len;
            yn *= len;
            zn *= len;
            */
            
            for (int i = face; i < face + 3; ++i)
            {
            	int index = faces[i] * 3;
		        vertexNormals[index] += xn;
		        vertexNormals[index + 1] += yn;
		        vertexNormals[index + 2] += zn;
            }
        }
    	for (int vertex = 0; vertex < vertexNormals.length; vertex += 3)
    	{
    		double xn = vertexNormals[vertex];
    		double yn = vertexNormals[vertex + 1];
    		double zn = vertexNormals[vertex + 2];
    		final double len = 1/Math.sqrt(xn * xn + yn * yn + zn * zn);
    		vertexNormals[vertex] = xn * len;
    		vertexNormals[vertex + 1] = yn * len;
    		vertexNormals[vertex + 2] = zn * len;
    	}
    }

    public final static void calcVertexNormals(final int sizeX, final int sizeY, FloatVectorObject vertex, FloatVectorObject vertexNormal, boolean xCyclic, boolean yCyclic){
		if (sizeX * sizeY != vertex.size()){
    		throw new ArrayIndexOutOfBoundsException("Vertex Count doesn't match the size: (" + sizeX + ',' + sizeY + ") vertexCount: " + vertex.size());
    	}

		if (sizeX * sizeY != vertexNormal.size()){
    		throw new ArrayIndexOutOfBoundsException("vertexNormal Count doesn't match the size: (" + sizeX + ',' + sizeY + ") vertexNormalCount: " + vertex.size());
    	}
    	
    	final float vertexX[] = vertex.x, vertexY[] = vertex.y, vertexZ[] = vertex.z;
		final float vertexNormalX[] = vertexNormal.x, vertexNormalY[] = vertexNormal.y, vertexNormalZ[] = vertexNormal.z;
    	final int sizeXMin1 = sizeX-1, sizeYMin1 = sizeY-1;
    	for (int i=0;i<sizeX;i++){
            final int middleIndexX = i*sizeY;
            final int lowIndexX = i==0 ? (xCyclic ? sizeY * sizeXMin1 : 0) : middleIndexX - sizeY;
            final int highIndexX = i== sizeXMin1 ? (xCyclic ? 0 : middleIndexX) : middleIndexX + sizeY;
            
    		for (int j=0;j<sizeY;j++){
    			/**
    			 * Die Indices sind folgendermassen verteilt:
    			 * 			i-1		i		i+1
    			 * 	j-1				01
    			 * 	j		10		11		12
    			 * 	j+1				21
    			 * Dabei sind die verschobenen Werte nach oben und unten begrentzt
    			 */
                final int index11 = middleIndexX + j;
                final int index01 = lowIndexX + j;
                final int index21 = highIndexX + j;
                final int index10 = index11 + (j==0 ? (yCyclic ? sizeYMin1 : 0)  : -1);
                final int index12 = index11 + (j==sizeYMin1 ? (yCyclic ? -sizeYMin1 : 0) : 1);
                final float ax = vertexX[index01] - vertexX[index21];
                final float ay = vertexY[index01] - vertexY[index21];
                final float az = vertexZ[index01] - vertexZ[index21];
                final float bx = vertexX[index10] - vertexX[index12];
                final float by = vertexY[index10] - vertexY[index12];
                final float bz = vertexZ[index10] - vertexZ[index12];

                final float xn=ay*bz-az*by;
                final float yn=az*bx-ax*bz;
                final float zn=ax*by-ay*bx;
                final float len = 1/(float)Math.sqrt(xn * xn + yn * yn + zn * zn);

                vertexNormalX[index11]=xn*len;
                vertexNormalY[index11]=yn*len;
                vertexNormalZ[index11]=zn*len;    		
            }
    	}
    }
    
    public final static void calcVertexNormals(final int sizeX, final int sizeY, FloatVectorObject vertex, float vertexNormal[], boolean xCyclic, boolean yCyclic){
 		if (sizeX * sizeY != vertex.size()){
     		throw new ArrayIndexOutOfBoundsException("Vertex Count doesn't match the size: (" + sizeX + ',' + sizeY + ") vertexCount: " + vertex.size());
     	}
 		if (sizeX * sizeY * 3 != vertexNormal.length){
     		throw new ArrayIndexOutOfBoundsException("vertexNormal Count doesn't match the size: (" + sizeX + ',' + sizeY + ") vertexNormalCount: " + vertex.size());
     	}
     	
     	final float vertexX[] = vertex.x, vertexY[] = vertex.y, vertexZ[] = vertex.z;
     	final int sizeXMin1 = sizeX-1, sizeYMin1 = sizeY-1;
     	for (int i=0;i<sizeX;i++){
             final int middleIndexX = i*sizeY;
             final int lowIndexX = i==0 ? (xCyclic ? sizeY * sizeXMin1 : 0) : middleIndexX - sizeY;
             final int highIndexX = i== sizeXMin1 ? (xCyclic ? 0 : middleIndexX) : middleIndexX + sizeY;
             
     		for (int j=0;j<sizeY;j++){
     			/**
     			 * Die Indices sind folgendermassen verteilt:
     			 * 			i-1		i		i+1
     			 * 	j-1				01
     			 * 	j		10		11		12
     			 * 	j+1				21
     			 * Dabei sind die verschobenen Werte nach oben und unten begrentzt
     			 */
                 final int index11 = middleIndexX + j;
                 final int index01 = lowIndexX + j;
                 final int index21 = highIndexX + j;
                 final int index10 = index11 + (j==0 ? (yCyclic ? sizeYMin1 : 0)  : -1);
                 final int index12 = index11 + (j==sizeYMin1 ? (yCyclic ? -sizeYMin1 : 0) : 1);
                 final float ax = vertexX[index01] - vertexX[index21];
                 final float ay = vertexY[index01] - vertexY[index21];
                 final float az = vertexZ[index01] - vertexZ[index21];
                 final float bx = vertexX[index10] - vertexX[index12];
                 final float by = vertexY[index10] - vertexY[index12];
                 final float bz = vertexZ[index10] - vertexZ[index12];

                 final float xn=ay*bz-az*by;
                 final float yn=az*bx-ax*bz;
                 final float zn=ax*by-ay*bx;
                 final float len = 1/(float)Math.sqrt(xn * xn + yn * yn + zn * zn);

                 vertexNormal[index11 * 3]=xn*len;
                 vertexNormal[index11 * 3 + 1]=yn*len;
                 vertexNormal[index11 * 3 + 2]=zn*len;    		
             }
     	}
     }
    public final static void calcVertexNormals(final int sizeX, final int sizeY, float vertex[], float vertexNormal[], boolean xCyclic, boolean yCyclic){
 		if (sizeX * sizeY * 3!= vertex.length){
     		throw new ArrayIndexOutOfBoundsException("Vertex Count doesn't match the size: (" + sizeX + ',' + sizeY + ") vertexCount: " +vertex.length/ 3);
     	}
 		if (sizeX * sizeY * 3 != vertexNormal.length){
     		throw new ArrayIndexOutOfBoundsException("vertexNormal Count doesn't match the size: (" + sizeX + ',' + sizeY + ") vertexNormalCount: " + vertex.length / 3);
     	}
     	
     	final int sizeXMin1 = (sizeX-1) * 3, sizeYMin1 = (sizeY-1) * 3;
     	for (int i=0;i<sizeX * 3;i += 3){
             final int middleIndexX = i*sizeY;
             final int lowIndexX = i==0 ? (xCyclic ? sizeY * sizeXMin1 : 0) : middleIndexX - sizeY * 3;
             final int highIndexX = i== sizeXMin1 ? (xCyclic ? 0 : middleIndexX) : middleIndexX + sizeY * 3;
             
     		for (int j=0;j<sizeY * 3;j += 3){
     			/**
     			 * Die Indices sind folgendermassen verteilt:
     			 * 			i-1		i		i+1
     			 * 	j-1				01
     			 * 	j		10		11		12
     			 * 	j+1				21
     			 * Dabei sind die verschobenen Werte nach oben und unten begrentzt
     			 */
                 final int index11 = (middleIndexX + j );
                 final int index01 = (lowIndexX + j);
                 final int index21 = (highIndexX + j);
                 final int index10 = index11 + (j==0 ? (yCyclic ? sizeYMin1 : 0)  : -3);
                 final int index12 = index11 + (j==sizeYMin1 ? (yCyclic ? -sizeYMin1 : 0) : 3);
                 final float ax = vertex[index01    ] - vertex[index21];
                 final float ay = vertex[index01 + 1] - vertex[index21 + 1];
                 final float az = vertex[index01 + 2] - vertex[index21 + 2];
                 final float bx = vertex[index10    ] - vertex[index12];
                 final float by = vertex[index10 + 1] - vertex[index12 + 1];
                 final float zn=ax*by-ay*bx;
                 final float bz = vertex[index10 + 2] - vertex[index12 + 2];

                 final float xn=ay*bz-az*by;
                 final float yn=az*bx-ax*bz;
                 final float len = 1/(float)Math.sqrt(xn * xn + yn * yn + zn * zn);

                 vertexNormal[index11]=xn*len;
                 vertexNormal[index11 + 1]=yn*len;
                 vertexNormal[index11 + 2]=zn*len;    		
             }
     	}
     }

    public static boolean cropToRectangle(Vector3d v0, Vector3d v1, Rectangle2D rect)
    {
    	double minX = rect.getMinX() + 1, maxX = rect.getMaxX() - 1;
    	double minY = rect.getMinY() + 1, maxY = rect.getMaxY() - 1;
    	double begin = 0;
    	double end = 0;
    	double diffX = v1.x - v0.x;
    	double diffY = v1.y - v0.y;
    	if (v0.x < minX)
		{
			begin = Math.max(begin,(minX - v0.x) / diffX);
		}
		else if (v0.x > maxX)
		{
			begin = Math.max(begin,(maxX - v0.x) / diffX);
		}
		if (v0.y < minY)
		{
			begin = Math.max(begin,(minY-v0.y) / diffY);
		}
		else if (v0.y > maxY)
		{
			begin = Math.max(begin,(maxY - v0.y) / diffY);
		}
		if (v1.x < minX)
		{
			end = Math.max(end, (v1.x - minX) / diffX);
		}
		else if (v1.x > maxX)
		{
			end = Math.max(end, (v1.x - maxX) / diffX);
		}
		if (v1.y < minY)
		{
			end = Math.max(end,(v1.y - minY) / diffY);
		}
		else if (v1.y > maxY)
		{
			end = Math.max(end, (v1.y - maxY) / diffY);
		}
		if (begin + end< 1)
		{
			v0.x += diffX * begin;
			v0.y += diffY * begin;
			v1.x -= diffX * end;
			v1.y -= diffY * end;
			return true;
		}
		return false;
    }
    
    public static boolean cropToRectangle(Vector2d v0, Vector2d v1, Rectangle2D rect)
    {
    	double minX = rect.getMinX() + 1, maxX = rect.getMaxX() - 1;
    	double minY = rect.getMinY() + 1, maxY = rect.getMaxY() - 1;
    	double begin = 0;
    	double end = 0;
    	double diffX = v1.x - v0.x;
    	double diffY = v1.y - v0.y;
    	if (Double.isNaN(diffX) || Double.isNaN(diffY))
    	{
    		return false;
    	}
    	if (v0.x < minX)
		{
			begin = Math.max(begin,(minX - v0.x) / diffX);
		}
		else if (v0.x > maxX)
		{
			begin = Math.max(begin,(maxX - v0.x) / diffX);
		}
		if (v0.y < minY)
		{
			begin = Math.max(begin,(minY-v0.y) / diffY);
		}
		else if (v0.y > maxY)
		{
			begin = Math.max(begin,(maxY - v0.y) / diffY);
		}
		if (v1.x < minX)
		{
			end = Math.max(end, (v1.x - minX) / diffX);
		}
		else if (v1.x > maxX)
		{
			end = Math.max(end, (v1.x - maxX) / diffX);
		}
		if (v1.y < minY)
		{
			end = Math.max(end,(v1.y - minY) / diffY);
		}
		else if (v1.y > maxY)
		{
			end = Math.max(end, (v1.y - maxY) / diffY);
		}
		if (begin + end< 1)
		{
			v0.x += diffX * begin;
			v0.y += diffY * begin;
			v1.x -= diffX * end;
			v1.y -= diffY * end;
			return true;
		}
		return false;
    }
    
    public static final void rotQuatToMatrix(Vector4d vec, Matrix3d mat)
    {
    	double s = 2 / vec.dot();
    	double vy = s * vec.y, vz = s * vec.z, vw = s * vec.w;
    	mat.x0 = 1 - vy * vec.y;
    	mat.y1 = 1 - vz * vec.z;
    	mat.z2 = 1 - vw * vec.w;
    	double syz = vec.y * vz, syw = vec.y * vw, szw = vec.z * vw;
    	double axw = vec.x * vw, axz = vec.x * vz, axy = vec.x * vy;
    	
    	mat.x1 = syz - axw;
    	mat.x2 = syw + axz;
    	mat.y2 = szw - axy;
    	mat.x1 = syz + axw;
    	mat.x2 = syw - axz;
    	mat.y2 = szw + axy;
    }
    
    public static final void rotMatrixToQuat(Matrix3d mat, Vector4d vec) {}
    
    public static final void getOrthorgonalVectors(Vector3d in, Vector3d n0, Vector3d n1)
    {
		Geometry.getOrthorgonalVector(in, n0);
		double length = Math.sqrt(in.dot());
		n0.setLength(length);
		n1.cross(in, n0);
		n1.setLength(length);
    }
    
    public static double[] getVarianceOnSphere(float[] position, float[] direction, byte[] accepted, Vector3d linePosition, Vector3d lineDirection, double evaluation_points[], double[] result) 
    {
		int num_vectors = position.length / 3;
		double divide = 1. / ArrayUtil.countNonzero(accepted);
    	double dotprod[] = new double[2 * num_vectors];
       	for (int i = 0; i < num_vectors ; ++i)
    	{
    		if (accepted[i] != 0)
    		{
	    		double xp = position[i * 3] - linePosition.x;
	    		double yp = position[i * 3 + 1] - linePosition.y;
	    		double zp = position[i * 3 + 2] - linePosition.z;
    			dotprod[i * 2] = xp * xp + yp * yp + zp * zp; //c = direction[i * 2] - planeScalar
    			dotprod[i * 2 + 1] = direction[i * 3] * xp + direction[i * 3 + 1] * yp + direction[i * 3 + 2] * zp; //b = dotprod[i * 2 + 1]
    		}
    	}
       	//System.out.println("dotprod:" + Arrays.toString(dotprod));
       	
       	
       	for (int j = 0; j < result.length; ++j)
    	{
    		double variance = 0;
    		double planeScalar = evaluation_points[j];
    		planeScalar *= planeScalar;
    		//System.out.print("alpha " + i + ":");
    		for (int i = 0; i < num_vectors; ++i)
    		{
     			if (accepted[i] != 0)
    			{
     				double b = dotprod[i * 2 + 1];
     				double c = dotprod[i * 2] - planeScalar;
					double alpha = -b + Math.sqrt(b * b - c);
					double xn = position[i * 3]     - linePosition.x + direction[i * 3]     * alpha;
					double yn = position[i * 3 + 1] - linePosition.y + direction[i * 3 + 1] * alpha;
					double zn = position[i * 3 + 2] - linePosition.z + direction[i * 3 + 2] * alpha;
					//System.out.print((lineDirection.dot(xn, yn, zn) / Math.sqrt(planeScalar)) + " ");
					//System.out.print(alpha + " ");
					
					double arc = Math.acos(Math.min(lineDirection.dot(xn, yn, zn) / evaluation_points[j],1));
					if (Double.isNaN(arc))
					{
						System.out.println(lineDirection.dot(xn, yn, zn) / evaluation_points[j]);
						System.out.println(j + " " + i);
					}
					variance += arc * arc;
    			}
			}
    		//System.out.println();
    		result[j] = variance * divide;
    	}
    	return result;
    }
    
	public static double[] getVariance(float[] position, float[] direction, byte[] accepted, Vector3d linePosition, Vector3d lineDirection, double[] distances, double[] result) {
		/*java("geometry.Geometry","","getVariance",{{0,0,0},{1,0,0},{1},{0,0,0},{1,0,0},0,1,{0,0,0}})*/
		
		System.out.println("position:" + Arrays.toString(position));
		System.out.println("direction:" + Arrays.toString(direction));
		System.out.println("accepted:" + Arrays.toString(accepted));
		int num_vectors = position.length / 3;
		double divide = 1. / ArrayUtil.countNonzero(accepted);
    	double dotprod[] = new double[2 * num_vectors];
    	for (int i = 0; i < num_vectors ; ++i)
    	{
    		if (accepted[i] != 0)
    		{
	    		double xp = linePosition.x - position[i * 3];
	    		double yp = linePosition.y - position[i * 3 + 1];
	    		double zp = linePosition.z - position[i * 3 + 2];
	    		dotprod[i * 2]     = lineDirection.dot(xp, yp, zp);
	    		dotprod[i * 2 + 1] = lineDirection.dot(direction, i * 3);
    		}
    	}
    	System.out.println("dotprod:" + Arrays.toString(dotprod));
    	for (int j = 0; j < result.length; ++j)
    	{
    		double variance = 0;
    		double planeScalar = distances[j];
    		for (int i = 0; i < num_vectors; ++i)
    		{
    			if (accepted[i] != 0)
    			{
	    			double t = (dotprod[i * 2] + planeScalar) / dotprod[i * 2 + 1];
	    			double xDiff = t * direction[i * 3]     + position[i * 3]     - linePosition.x - lineDirection.x * planeScalar;
	    			double yDiff = t * direction[i * 3 + 1] + position[i * 3 + 1] - linePosition.y - lineDirection.y * planeScalar;
	    			double zDiff = t * direction[i * 3 + 2] + position[i * 3 + 2] - linePosition.z - lineDirection.z * planeScalar;
	    			variance += xDiff * xDiff + yDiff * yDiff + zDiff * zDiff;
    			}
    		}
    		result[j] = variance * divide;
    	}
    	return result;
	}
    
    public static double[] getVariance(double position[], double direction[], Vector3d linePosition, Vector3d lineDirection, double begin, double end, double result[])
    {
    	int num_vectors = position.length / 3;
    	double dotprod[] = new double[2 * num_vectors];
    	double divide = 1. / num_vectors;
    	for (int i = 0; i < num_vectors ; ++i)
    	{
    		double xp = position[i * 3] - linePosition.x;
    		double yp = position[i * 3] - linePosition.y;
    		double zp = position[i * 3] - linePosition.z;
    		dotprod[i * 2] = lineDirection.dot(xp, yp, zp);
    		dotprod[i * 2 + 1] = lineDirection.dot(direction, i * 3);
    	}
    	for (int i = 0; i < result.length; ++i)
    	{
    		double variance = 0;
    		double planeScalar = begin + i * (end - begin);
    		for (int j = 0; j < num_vectors; ++j)
    		{
    			double t = (dotprod[j * 2] - planeScalar) / dotprod[j * 2 + 1];
    			double xDiff = t * direction[j * 3]     + position[j * 3]     - linePosition.x;
    			double yDiff = t * direction[j * 3 + 1] + position[j * 3 + 1] - linePosition.y;
    			double zDiff = t * direction[j * 3 + 2] + position[j * 3 + 2] - linePosition.z;
    			variance += xDiff * xDiff + yDiff * yDiff + zDiff * zDiff;
    		}
    		result[i] = variance * divide;
    	}
    	return result;
    }
    
    public static final void getOrthorgonalMatrix(Vector3d in, Matrixd out) {
    	double dirlength = Math.sqrt(in.dot());
    	double qx = in.x * in.x;
		double qy = in.y * in.y;
		double qz = in.z * in.z;
		
		double lqx = qy + qz;
		double lqy = qx + qz;
		double lqz = qx + qy;
		
		double n0x, n0y, n0z;
		
		if (lqx >= lqy && lqx >= lqz)
		{
			n0x = 0; n0y = -in.z; n0z = in.y;
		}
		else if (lqy >= lqx && lqy >= lqz)
		{
			n0x = -in.z; n0y = 0; n0z = in.x;
		}
		else
		{
			n0x = -in.y; n0y = in.x; n0z =0;
		}
		double mult = dirlength / Math.sqrt(n0x * n0x + n0y * n0y + n0z * n0z);
		n0x *= mult; n0y *= mult; n0z *= mult;
		double n1x = in.y * n0z - in.z * n0y;
    	double n1y = in.z * n0x - in.x * n0z;
    	double n1z = in.x * n0y - in.y * n0x;
    	mult = dirlength / Math.sqrt(n1x * n1x + n1y * n1y + n1z * n1z);
    	n1x *= mult; n1y *= mult; n1z *= mult;
    	if (out instanceof Matrix3d)
    	{
    		((Matrix3d)out).set(n0x, n0y, n0z, n1x, n1y, n1z, in.x, in.y, in.z);
    	}
    	if (out instanceof Matrix4d)
    	{
    		((Matrix4d)out).set(n0x, n0y, n0z, n1x, n1y, n1z, in.x, in.y, in.z);
    	}
    }
    
    public static final void getOrthorgonalVector(Vector3d in, Vector3d out) {
    	double qx = in.x * in.x;
		double qy = in.y * in.y;
		double qz = in.z * in.z;
		
		double lqx = qy + qz;
		double lqy = qx + qz;
		double lqz = qx + qy;
		
		if (lqx >= lqy && lqx >= lqz)
		{
			out.set(0, -in.z, in.y);
			return;
		}
		if (lqy >= lqx && lqy >= lqz)
		{
			out.set(-in.z, 0, in.x);
			return;
		}
		out.set(-in.y, in.x,0);
		return;
    }

	public static final float distanceQ(float[] data0, int data0begin, float[] data1, int data1begin, int count) {
		float res = 0;
		for (int i = 0; i < count; ++i)
		{
			final float diff = data0[data0begin + i] - data1[data1begin + i];
			res += diff * diff;
		}
		return res;
	}
	
	public static final void getRotationFromTo(Vector3d from, Vector3d to, Matrix3d m)
	{
		double cos = 1 / from.dot();
		double scale = to.dot() * cos;
		cos *= from.dot(to);
		double sin = Math.sqrt(scale - cos * cos);
		/*Crossproduct*/
		double nx = from.y * to.z - from.z * to.y;
		double ny = from.z * to.x - from.x * to.z;
		double nz = from.x * to.y - from.y * to.x;
		double len = 1 / Math.sqrt(nx * nx + ny * ny + nz * nz);
		nx *= len; ny *= len; nz *= len;
		
		double onemincos = Math.sqrt(scale) - cos;
		m.x0 = (nx * nx * onemincos + cos);
		m.y1 = (ny * ny * onemincos + cos);
		m.z2 = (nz * nz * onemincos + cos);
		/*Symmetric part*/
		double sx = ny * nz * onemincos;
		double sy = nx * nz * onemincos;
		double sz = nx * ny * onemincos;
		/*Asymmetric part*/
		double asx = nx * sin;
		double asy = ny * sin;
		double asz = nz * sin;
		m.x1 = sz + asz;
		m.y0 = sz - asz;
		m.x2 = sy - asy;
		m.z0 = sy + asy;
		m.y2 = sx - asx;
		m.z1 = sx + asx;
	}

	public static void toCart(Vector3d vec, double azimuth, double elevation, double distance) {
		vec.x = distance * Math.sin(elevation);
		double cos = Math.cos(elevation);
		vec.y = distance * cos * Math.sin(azimuth);
		vec.z = distance * cos * Math.cos(azimuth);	
	}

}
