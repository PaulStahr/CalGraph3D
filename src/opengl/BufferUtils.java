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
package opengl;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import geometry.Vector2f;
import util.Buffers;

public class BufferUtils {
	public static final FloatBuffer fillWithVertexAndNormalDataInterleaved(
			final float vertexX[],
			final float vertexY[],
			final float vertexZ[],
			final float vertexNormalX[],
			final float vertexNormalY[],
			final float vertexNormalZ[],
			FloatBuffer vertexData)
	{
		final int vertexCount = vertexX.length;
		vertexData = Buffers.ensureCapacity(vertexCount * 6, vertexData);
		for (int i=0;i<vertexCount;++i){
			final int pos = i * 6;
			vertexData.put(pos    , vertexX[i]);
			vertexData.put(pos + 1, vertexY[i]);
			vertexData.put(pos + 2, vertexZ[i]);
			vertexData.put(pos + 3, vertexNormalX[i]);
			vertexData.put(pos + 4, vertexNormalY[i]);
			vertexData.put(pos + 5, vertexNormalZ[i]);
		}
		return vertexData;
	}
	
	public static final FloatBuffer fillWithVertexAndNormalDataInterleaved(
			final float vertexX[],
			final float vertexY[],
			final float vertexZ[],
			final float vertexNormal[],
			FloatBuffer vertexData)
	{
		final int vertexCount = vertexX.length;
		vertexData = Buffers.ensureCapacity(vertexCount * 6, vertexData);
		for (int i=0;i<vertexCount;++i){
			final int pos = i * 6;
			vertexData.put(pos    , vertexX[i]);
			vertexData.put(pos + 1, vertexY[i]);
			vertexData.put(pos + 2, vertexZ[i]);
			vertexData.put(pos + 3, vertexNormal[i * 3]);
			vertexData.put(pos + 4, vertexNormal[i * 3 + 1]);
			vertexData.put(pos + 5, vertexNormal[i * 3 + 2]);
		}
		return vertexData;
	}
	
	public static final FloatBuffer fillWithVertexAndNormalDataInterleaved(
			final float vertices[],
			final float normals[],
			FloatBuffer vertexData)
	{
		vertexData = Buffers.ensureCapacity(vertices.length * 2, vertexData);
		for (int i=0;i<vertices.length;i += 3){
			final int pos = i * 2;
			vertexData.put(pos    , vertices[i]);
			vertexData.put(pos + 1, vertices[i + 1]);
			vertexData.put(pos + 2, vertices[i + 2]);
			vertexData.put(pos + 3, normals[i]);
			vertexData.put(pos + 4, normals[i + 1]);
			vertexData.put(pos + 5, normals[i + 2]);
		}
		return vertexData;
	}
	
	public static final FloatBuffer fillWithQuad(FloatBuffer floatBuffer)
	{
		floatBuffer = Buffers.ensureCapacity(12, floatBuffer);
        final float length = 0.69336127435063470484335227478596f;
		int index = 0;
	    for (int i=0;i<4;i++){
        	final float x = i==1 || i==2 ? -length : length, y = i>=2 ? -length : length;
        	floatBuffer.put(index++, x);
        	floatBuffer.put(index++, y);
        	floatBuffer.put(index++, -length);
        }
	    return floatBuffer;
	}
	
	public static final FloatBuffer fillWithFloatData(float data[], FloatBuffer floatBuffer)
	{
		floatBuffer = Buffers.ensureCapacity(data.length, floatBuffer);
		for (int i = 0; i < data.length; ++i)
		{
			floatBuffer.put(i, data[i]);
		}
		return floatBuffer;
	}
	
	public static final IntBuffer fillWithIntData(int data[], IntBuffer indexData)
	{
		indexData = Buffers.ensureCapacity(data.length, indexData);
		for (int i = 0; i < data.length; ++i)
		{
			indexData.put(i, data[i]);
		}
		return indexData;
	}
	
	public static final FloatBuffer fillWithVertexData(
			final float vertexX[],
			final float vertexY[],
			final float vertexZ[],
			FloatBuffer vertexData)
	{
		final int vertexCount = vertexX.length;
		vertexData = Buffers.ensureCapacity(vertexCount * 3, vertexData);
		for (int i=0;i<vertexCount;++i){
			final int pos = i * 3;
			vertexData.put(pos    , vertexX[i]);
			vertexData.put(pos + 1, vertexY[i]);
			vertexData.put(pos + 2, vertexZ[i]);
		}
		return vertexData;
	}
	
	public static final FloatBuffer fillWithVertexAndColorData(
			final float vertexX[],
			final float vertexY[],
			final float vertexZ[],
			final float color[],
			FloatBuffer vertexData)
	{
		final int vertexCount = vertexX.length;
		vertexData = Buffers.ensureCapacity(vertexCount * 7, vertexData);
		for (int i=0;i<vertexCount;++i){
			final int pos = i * 7;
			vertexData.put(pos    , vertexX[i]);
			vertexData.put(pos + 1, vertexY[i]);
			vertexData.put(pos + 2, vertexZ[i]);
			vertexData.put(pos + 3, color[i* 4]);
			vertexData.put(pos + 4, color[i* 4 + 1]);
			vertexData.put(pos + 5, color[i* 4 + 2]);
			vertexData.put(pos + 6, color[i* 4 + 3]);
		}
		return vertexData;
	}
	
	public static final FloatBuffer fillWithVertexAndColorData(
			final float vertices[],
			final float color[],
			FloatBuffer vertexData)
	{
		final int vertexCount = vertices.length / 3;
		vertexData = Buffers.ensureCapacity(vertexCount * 7, vertexData);
		for (int i=0;i<vertexCount;++i){
			final int pos = i * 7;
			vertexData.put(pos    , vertices[i * 3]);
			vertexData.put(pos + 1, vertices[i * 3 + 1]);
			vertexData.put(pos + 2, vertices[i * 3 + 2]);
			vertexData.put(pos + 3, color[i* 4]);
			vertexData.put(pos + 4, color[i* 4 + 1]);
			vertexData.put(pos + 5, color[i* 4 + 2]);
			vertexData.put(pos + 6, color[i* 4 + 3]);
		}
		return vertexData;
	}
	
	public static final FloatBuffer fillWithVertexAndColorData2(
			final float vertices[],
			final float color[],
			FloatBuffer vertexData)
	{
		final int vertexCount = vertices.length / 3;
		vertexData = Buffers.ensureCapacity(vertexCount * 7, vertexData);
		int pos = -1;
		int vpos = -1;
		int cpos = -1;
		while (vpos < vertices.length - 1){
			vertexData.put(++pos, vertices[++vpos]);
			vertexData.put(++pos, vertices[++vpos]);
			vertexData.put(++pos, vertices[++vpos]);
			vertexData.put(++pos, color[++cpos]);
			vertexData.put(++pos, color[++cpos]);
			vertexData.put(++pos, color[++cpos]);
			vertexData.put(++pos, color[++cpos]);
		}
		return vertexData;
	}
	
	public static final int[] fillWithRadialIndexData(int latitudes, int longitudes, int faces[])
	{
		int num_faces = latitudes * (4 * longitudes - 6);
		if (faces == null || faces.length != num_faces)
		{
			faces = new int[num_faces];
		}
		int index = 0;
		for (int j = 0; j < latitudes; j += 2)
		{
			faces[index++] = 0;
			faces[index++] = (j + 2)%latitudes + 1;
			faces[index++] = j + 2;
			faces[index++] = j + 1;
		}
		for (int i = 0; i < longitudes - 2; ++i)
		{
			int index0 = i * latitudes + 1;
			int index1 = index0 + latitudes;
			for (int j = 0; j < latitudes; ++j)
			{
				faces[index++] = index0 + j;
				faces[index++] = index0 + (j + 1) % latitudes;
				faces[index++] = index1 + (j + 1) % latitudes;
				faces[index++] = index1 + j;
			}
		}
		
		return faces;
	}
	
	public static final int[] fillWithCylinderIndexData(int latitudes, int longitudes, int faces[])
	{
		int num_faces = latitudes * (4 * longitudes - 4);
		if (faces == null || faces.length != num_faces)
		{
			faces = new int[num_faces];
		}
		int index = 0;
		for (int i = 0; i < longitudes - 1; ++i)
		{
			int index0 = i * latitudes;
			int index1 = index0 + latitudes;;
			for (int j = 0; j < latitudes; ++j)
			{
				int jj = (j + 1) % latitudes;
				faces[index++] = index0 + j;
				faces[index++] = index0 + jj;
				faces[index++] = index1 + jj;
				faces[index++] = index1 + j;
			}
		}
		return faces;
	}
	
	public static final IntBuffer fillWithPlaneIndexData(int sizeX, int sizeY, IntBuffer indexData)
	{
		indexData = Buffers.ensureCapacity((sizeX - 1) * (sizeY - 1) * 4, indexData);
		int index = 0;
		int v0 = 0;
		for (int i=0;i<sizeX - 1;i++){
			for (int j=0;j<sizeY - 1;++j){
				indexData.put(index,     v0);
				indexData.put(index + 1, v0 + sizeY);
				indexData.put(index + 2, v0 + sizeY + 1);
				indexData.put(index + 3, v0 + 1);
	        	index += 4;
	        	++v0;
			}
			++v0;
		}
		return indexData;
	}
	
	public static final IntBuffer fillWithPlaneIndexData(int sizeX, int sizeY, int permutation[], IntBuffer indexData)
	{
		indexData = Buffers.ensureCapacity((sizeX - 1) * (sizeY - 1) * 4, indexData);
		int index = 0;
		int v0 = 0;
		for (int i=0;i<sizeX - 1;i++){
			for (int j=0;j<sizeY - 1;++j){
				int face = permutation[v0];
				indexData.put(index,     face);
				indexData.put(index + 1, face + sizeY);
				indexData.put(index + 2, face + sizeY + 1);
				indexData.put(index + 3, face + 1);
	        	index += 4;
	        	++v0;
			}
			++v0;
		}
		return indexData;
	}
	
	public static final IntBuffer fillWithLineIndexData(int sizeX, int sizeY, IntBuffer indexData){
		indexData = Buffers.ensureCapacity((sizeX * sizeY * 2 - sizeX - sizeY) * 2, indexData);
		int index = 0;
		for (int i=0;i<sizeX;i++){
			for (int j=0;j<sizeY - 1;++j){
				indexData.put(index++,     i * sizeY + j);
				indexData.put(index++, i * sizeY + j + 1);
			}
		}
		for (int i=0;i<sizeX - 1;++i){
			for (int j=0;j<sizeY;++j){
				indexData.put(index++,     i * sizeY + j);
				indexData.put(index++, (i + 1) * sizeY + j);
			}
		}
		return indexData;
	}
	
	public static final IntBuffer fillWithLineIndexData(int size, IntBuffer indexData){
		indexData = Buffers.ensureCapacity(size * 2 - 2, indexData);
		indexData.put(0, 0);
		for (int i=1;i<size - 1;++i){
			indexData.put(i * 2 - 1,     i);
			indexData.put(i * 2, i);
		}
		indexData.put(size * 2 - 3, size - 1);
		return indexData;
	}
	
	public static final FloatBuffer fillWithVectorData(Vector2f vertices[], FloatBuffer floatBuffer)
	{
		floatBuffer = Buffers.ensureCapacity(vertices.length * 2, floatBuffer);
	    for (int i=0;i<vertices.length;i++){
        	floatBuffer.put(i * 2, vertices[i].x);
        	floatBuffer.put(i * 2 + 1, vertices[i].y);
        }
	    return floatBuffer;
	}

	public static IntBuffer fillWithLines(int vertexCount, int lineLength, IntBuffer indexData) {
		indexData = Buffers.ensureCapacity(vertexCount * lineLength * 2,indexData);
		int index = 0;
		for (int j = 0; j < lineLength; ++j)
		{
			for (int i = 0; i < vertexCount; ++i)
			{
				indexData.put(index++, vertexCount * j + i);
				indexData.put(index++, vertexCount * ((j + 1)%lineLength) + i);
			}
		}
		return indexData;
	}
}
