package io;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import geometry.Vector3i;
import util.StringUtils;
import util.data.DoubleArrayList;
import util.data.IntegerArrayList;

public class ObjectImporter {
	/*private static int indexOfOrEnd(String s, char c)
	{
		int i = s.indexOf(c);
		return i < 0 ? s.length() : i;
	}*/
	


	public static void exportTriangleWavefront(OutputStream os, String name, int faces[], double vertices[], double textureCoords[]) throws IOException
	{
		StringBuilder strB = new StringBuilder();
		char chBuf[] = new char[1024];
		OutputStreamWriter writer = new OutputStreamWriter(os);
		BufferedWriter outBuf = new BufferedWriter(writer);
		StringUtils.writeAndReset(outBuf, strB.append('o').append(' ').append(name), chBuf);
		outBuf.newLine();
		for (int i = 0; i < vertices.length; i += 3)
		{
			StringUtils.writeAndReset(outBuf, strB.append('v').append(' ').append(vertices[i]).append(' ').append(vertices[i + 1]).append(' ').append(vertices[i + 2]), chBuf);
			outBuf.newLine();
		}
		for (int i = 0; i < faces.length; i += 3)
		{
			StringUtils.writeAndReset(outBuf, strB.append('f').append(' ').append(faces[i] + 1).append(' ').append(faces[i + 1] + 1).append(' ').append(faces[i + 2] + 1), chBuf);
			outBuf.newLine();
		}
		if (textureCoords != null)
		{
			for (int i = 0; i < textureCoords.length; i += 2)
			{
				StringUtils.writeAndReset(outBuf, strB.append('v').append('t').append(' ').append(textureCoords[i]).append(' ').append(textureCoords[i + 1]), chBuf);
				outBuf.newLine();
			}	
		}
		outBuf.close();
		writer.close();
	}

	private static final void parseFacepoint(String line, int begin, int end, Vector3i dat)
	{
		int sep0 = StringUtils.indexOfOrEnd(line, begin, end, '/');
		dat.x = StringUtils.parseInt(line, begin, sep0, 10);
		if (sep0 != end)
		{
			int sep1 = StringUtils.indexOfOrEnd(line, sep0 + 1, end, '/');
			dat.y = StringUtils.parseInt(line, sep0 + 1, sep1, 10);
			if (sep1 != end)
			{
				dat.z = StringUtils.parseInt(line, sep1 + 1, end, 10);
			}
		}
	}
	
	public static void importTriangleWavefront(InputStream is, IntegerArrayList faces, DoubleArrayList vertices, IntegerArrayList textureCoordIndexList, DoubleArrayList textureCoords) throws IOException
	{
		InputStreamReader reader = new InputStreamReader(is);
		BufferedReader inBuf = new BufferedReader(reader);
		String line;
		IntegerArrayList ial = new IntegerArrayList();
		Vector3i vec = new Vector3i();
		while((line = inBuf.readLine()) != null)
		{
			StringUtils.split(line, 0,  line.length(), ' ', false, ial);
			switch(ial.getI(1) - ial.getI(0))
			{
				case 1:
					switch(line.charAt(ial.getI(0)))
					{
						case 'f':
						{
							parseFacepoint(line, ial.getI(2), ial.getI(3), vec);
							final int v0 = vec.x - 1;
							final int t0 = vec.y - 1;
							parseFacepoint(line, ial.getI(4), ial.getI(5), vec);
							final int v1 = vec.x - 1;
							final int t1 = vec.y - 1;
							parseFacepoint(line, ial.getI(6), ial.getI(7), vec);
							final int v2 = vec.x - 1;
							final int t2 = vec.y - 1;
							faces.add(v0);faces.add(v1); faces.add(v2);
							if (t0 != -1 && t1 != -1 && t2 != -1)
							{
								textureCoordIndexList.add(t0);textureCoordIndexList.add(t1); textureCoordIndexList.add(t2);
							}
							if (ial.size() > 8)
							{
								parseFacepoint(line, ial.getI(8), ial.getI(9), vec);
								final int v3 = vec.x - 1;
								final int t3 = vec.y - 1;
								faces.add(v2);faces.add(v3); faces.add(v0);
								if (t2 != -1 && t3 != -1 && t0 != -1)
								{
									textureCoordIndexList.add(t2);textureCoordIndexList.add(t3); textureCoordIndexList.add(t0);
								}
							}
							
							break;
						}
						case 'v':
						{
							for (int j = 2; j < 8; j += 2)
							{
								final double x = Double.parseDouble(line.substring(ial.getI(j), ial.getI(j + 1)));
								vertices.add(x);
							}
							break;
						}
					}
					break;	
				case 2:
				{
					if (line.charAt(ial.getI(0)) == 'v' && line.charAt(ial.getI(1)) == 't')
					{
						double x = Double.parseDouble(line.substring(ial.getI(2), ial.getI(3)));
						double y = Double.parseDouble(line.substring(ial.getI(4), ial.getI(5)));
						textureCoords.add(x); textureCoords.add(y);
						break;
					}
				}
			}
			ial.clear();
		}
	}
}
