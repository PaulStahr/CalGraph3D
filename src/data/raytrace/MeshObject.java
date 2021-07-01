package data.raytrace;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import data.DataHandler;
import data.raytrace.RaySimulation.MaterialType;
import geometry.Geometry;
import geometry.Matrix4d;
import geometry.Vector2d;
import geometry.Vector3d;
import io.ObjectImporter;
import maths.Controller;
import maths.algorithm.DoubleMatrixUtil;
import maths.exception.OperationParseException;
import maths.variable.VariableAmount;
import util.data.DoubleArrayList;
import util.data.IntegerArrayList;
import util.data.SortedIntegerArrayList;
import util.data.UniqueObjects;

public class MeshObject extends SurfaceObject {
	public static final MeshObject EMPTY_MESH_ARRAY[] = new MeshObject[0];
	private int faces[] = UniqueObjects.EMPTY_INT_ARRAY;
	private int lines[] = UniqueObjects.EMPTY_INT_ARRAY;
	private double vertices[] = UniqueObjects.EMPTY_DOUBLE_ARRAY;
	private double vertexNormals[] = UniqueObjects.EMPTY_DOUBLE_ARRAY;
	private double faceNormals[] = UniqueObjects.EMPTY_DOUBLE_ARRAY;
	private double faceNormalsInversedLength[] = UniqueObjects.EMPTY_DOUBLE_ARRAY;
	private double textureCoordinates[] = UniqueObjects.EMPTY_DOUBLE_ARRAY;
	private String positionStr;
	private final Controller controll = new Controller();
	private String transformationStr;
	private String colorStr;
	private static final Logger logger = LoggerFactory.getLogger(MeshObject.class);
	public final Matrix4d meshToGlobal = new Matrix4d();
	public final Matrix4d globalToMesh = new Matrix4d();
	//public double inverserowdotprods[] = new double[3];
	private final ArrayList<MeshObjectChangeListener> changeListeners = new ArrayList<>();
	private final Vector3d weightPoint = new Vector3d();
	private double radiusQ;
	private double normalizedIncrementingArea[] = UniqueObjects.EMPTY_DOUBLE_ARRAY;
	public boolean smooth;

	public static interface MeshObjectChangeListener
	{
		public void valueChanged(MeshObject object, SCENE_OBJECT_COLUMN_TYPE ct);
	}

	public static final COLUMN_TYPES TYPES = new COLUMN_TYPES(new SCENE_OBJECT_COLUMN_TYPE[]{
			SCENE_OBJECT_COLUMN_TYPE.ID,
			SCENE_OBJECT_COLUMN_TYPE.ACTIVE,
			SCENE_OBJECT_COLUMN_TYPE.POSITION,
			SCENE_OBJECT_COLUMN_TYPE.TRANSFORMATION,
			SCENE_OBJECT_COLUMN_TYPE.MATERIAL,
			SCENE_OBJECT_COLUMN_TYPE.COLOR,
			SCENE_OBJECT_COLUMN_TYPE.TEXTURE_OBJECT,
			SCENE_OBJECT_COLUMN_TYPE.OPEN,
			SCENE_OBJECT_COLUMN_TYPE.SAVE_TO,
			SCENE_OBJECT_COLUMN_TYPE.TRACED_RAYS,
			SCENE_OBJECT_COLUMN_TYPE.UNTRACED_RAYS,
			SCENE_OBJECT_COLUMN_TYPE.DIFFUSE,
			SCENE_OBJECT_COLUMN_TYPE.BIDIRECTIONAL,
			SCENE_OBJECT_COLUMN_TYPE.SMOOTH,
			SCENE_OBJECT_COLUMN_TYPE.INVERT_INOUT,
			SCENE_OBJECT_COLUMN_TYPE.DELETE},
			new SCENE_OBJECT_COLUMN_TYPE[]{SCENE_OBJECT_COLUMN_TYPE.ID,
			SCENE_OBJECT_COLUMN_TYPE.ACTIVE,
			SCENE_OBJECT_COLUMN_TYPE.POSITION,
			SCENE_OBJECT_COLUMN_TYPE.TRANSFORMATION,
			SCENE_OBJECT_COLUMN_TYPE.MATERIAL,
			SCENE_OBJECT_COLUMN_TYPE.COLOR,
			SCENE_OBJECT_COLUMN_TYPE.OPEN,
			SCENE_OBJECT_COLUMN_TYPE.SAVE_TO,
			SCENE_OBJECT_COLUMN_TYPE.TRACED_RAYS,
			SCENE_OBJECT_COLUMN_TYPE.UNTRACED_RAYS,
			SCENE_OBJECT_COLUMN_TYPE.DIFFUSE,
			SCENE_OBJECT_COLUMN_TYPE.BIDIRECTIONAL,
			SCENE_OBJECT_COLUMN_TYPE.SMOOTH,
			SCENE_OBJECT_COLUMN_TYPE.INVERT_INOUT,
			SCENE_OBJECT_COLUMN_TYPE.DELETE});

	@Override
	public COLUMN_TYPES getTypes()
	{
		return TYPES;
	}

	private static final Object defaultValues[] = new Object[TYPES.colSize()];
	static
	{
		for (int i = 0; i < defaultValues.length; ++i)
		{
			defaultValues[i] = TYPES.getCol(i).defaultValue;
		}
	}

	public MeshObject(VariableAmount va, ParseUtil parser)
	{
		setValues(defaultValues, va, parser);
	}

	public MeshObject(ArrayList<SCENE_OBJECT_COLUMN_TYPE> vctList, ArrayList<? extends Object> valueList, VariableAmount va, ParseUtil parser) {
		super();
		setValues(vctList, valueList, va, parser);
	}

	private static final int binarySearch(double[] a, int low, int hi, double key)
	{
		if (low > hi)
		    throw new IllegalArgumentException("The start index " + low + " is higher than the finish index " + hi + '.');
		if (low < 0 || hi > a.length)
	    throw new ArrayIndexOutOfBoundsException("One of the indices is out of bounds.");
		// Must use Double.compare to take into account NaN, +-0.
		int mid = 0;
		while (low <= hi)
		{
			mid = (low + hi) >>> 1;
			final int r = Double.compare(a[mid], key);
			if (r == 0)
				return mid;
			else if (r > 0)
				hi = mid - 1;
			else
	        // This gets the insertion point right on the last loop
	        low = ++mid;
	    }
		return mid;
	}

	public final int getFace(double incArea)
	{
		return binarySearch(normalizedIncrementingArea, 0, normalizedIncrementingArea.length - 1, incArea);
	}

	public final void getPoint(int face, double alpha, double beta, Vector3d position)
	{
		face *= 3;
		int f0 = 3 * faces[face], f1 = 3 * faces[face + 1], f2 = 3 * faces[face + 2];
		double gamma = 1 - alpha - beta;
		position.x = vertices[f0]     * gamma + vertices[f1]     * alpha + vertices[f2]     * beta;
		position.y = vertices[f0 + 1] * gamma + vertices[f1 + 1] * alpha + vertices[f2 + 1] * beta;
		position.z = vertices[f0 + 2] * gamma + vertices[f1 + 2] * alpha + vertices[f2 + 2] * beta;
		meshToGlobal.rdotAffine(position);
	}

	public final void getNormal(int face, double alpha, double beta, Vector3d direction)
	{
		face *= 3;
		int f0 = 3 * faces[face], f1 = 3 * faces[face + 1], f2 = 3 * faces[face + 2];
		double gamma = 1 - alpha - beta;
		direction.x = vertexNormals[f0]     * gamma + vertexNormals[f1]     * alpha + vertexNormals[f2]     * beta;
		direction.y = vertexNormals[f0 + 1] * gamma + vertexNormals[f1 + 1] * alpha + vertexNormals[f2 + 1] * beta;
		direction.z = vertexNormals[f0 + 2] * gamma + vertexNormals[f1 + 2] * alpha + vertexNormals[f2 + 2] * beta;
		globalToMesh.ldot(direction);
	}

	public final void getTextureCoord(int face, double alpha, double beta, Vector2d position)
	{
		face *= 3;
		int f0 = 2 * faces[face], f1 = 2 * faces[face + 1], f2 = 2 * faces[face + 2];
		double gamma = 1 - alpha - beta;
		position.x = textureCoordinates[f0]     * gamma + textureCoordinates[f1]     * alpha + textureCoordinates[f2]     * beta;
		position.y = textureCoordinates[f0 + 1] * gamma + textureCoordinates[f1 + 1] * alpha + textureCoordinates[f2 + 1] * beta;
	}

	public final void getNormal(int face, Vector3d direction)
	{
		direction.set(faceNormals, face * 3);
	}

	@Override
	public Intersection getIntersection(Vector3d position, Vector3d direction, Intersection intersection, double lowerBound, double upperBound) {
		//Geometry.calcTriangleMeshVertexFaceNormals(vertices, faces, vertexNormals, faceNormals);
		final double px = globalToMesh.rdotAffineX(position.x, position.y, position.z);
		final double py = globalToMesh.rdotAffineY(position.x, position.y, position.z);
		final double pz = globalToMesh.rdotAffineZ(position.x, position.y, position.z);
		double dx = globalToMesh.rdotX(direction.x, direction.y, direction.z);
		double dy = globalToMesh.rdotY(direction.x, direction.y, direction.z);
		double dz = globalToMesh.rdotZ(direction.x, direction.y, direction.z);
		double invDirLen = Math.sqrt(dx * dx + dy * dy + dz * dz);
		lowerBound *= invDirLen;
		upperBound *= invDirLen;
		invDirLen = 1 / invDirLen;
		dx *= invDirLen;
		dy *= invDirLen;
		dz *= invDirLen;

		{
			double x = px - weightPoint.x, y = py - weightPoint.y, z = pz - weightPoint.z;
			double c = x * x + y * y + z * z - this.radiusQ;
			double b = x * dx + y * dy + z * dz;
			double sqrt = b * b - c;
			if (sqrt < 0)
			{
				return null;
			}
			sqrt = Math.sqrt(sqrt);
			if (lowerBound > sqrt - b ||  - sqrt - b > upperBound)
			{
				return null;
			}
		}
		for (int i = 0; i < faces.length; i += 3)
		{
			final int v0 = faces[i] * 3;
			final double xfn = faceNormalsInversedLength[i], yfn = faceNormalsInversedLength[i + 1], zfn = faceNormalsInversedLength[i + 2];
			final double x0 = vertices[v0], y0 = vertices[v0 + 1], z0 = vertices[v0 + 2];
			final double pxr = x0 - px, pyr = y0 - py, pzr = z0 - pz; //position relative
			final double dist = (pxr * xfn + pyr * yfn + pzr * zfn) / (xfn * dx + yfn * dy + zfn * dz);
			if (lowerBound > dist || dist > upperBound)
			{
				continue;
			}
			final double xir = dist * dx - pxr, yir = dist * dy - pyr, zir = dist * dz - pzr;
			final double cx = zir * yfn - yir * zfn, cy = xir * zfn - zir * xfn, cz = yir * xfn - xir * yfn;

			final int v1 = faces[i + 1] * 3;
			final double u1 = cx * (vertices[v1] - x0) + cy * (vertices[v1 + 1] - y0) + cz * (vertices[v1 + 2] - z0);
			if (u1 < 0 || u1 > 1)
			{
				continue;
			}
			final int v2 = faces[i + 2] * 3;
			final double u2 = -(cx * (vertices[v2] - x0) + cy * (vertices[v2 + 1] - y0) + cz * (vertices[v2 + 2] - z0));
			if (u2 < 0 || u1 + u2 > 1)
			{
				continue;
			}
			intersection.position.set(xir + x0, yir + y0, zir + z0);
			{
				final double u0 = 1 - u1 - u2;
				intersection.normal.x = u2 * vertexNormals[v1]     + u1 * vertexNormals[v2]     + u0 * vertexNormals[v0];
				intersection.normal.y = u2 * vertexNormals[v1 + 1] + u1 * vertexNormals[v2 + 1] + u0 * vertexNormals[v0 + 1];
				intersection.normal.z = u2 * vertexNormals[v1 + 2] + u1 * vertexNormals[v2 + 2] + u0 * vertexNormals[v0 + 2];
				if (textureCoordinates != null)
				{
					final int v0t = v0 * 2 / 3, v1t = v1 * 2 / 3, v2t = v2 * 2 / 3;
					intersection.textureX = u2 * textureCoordinates[v1t]     + u1 * textureCoordinates[v2t]     + u0 * textureCoordinates[v0t];
					intersection.textureY = u2 * textureCoordinates[v1t + 1] + u1 * textureCoordinates[v2t + 1] + u0 * textureCoordinates[v0t + 1];
				}
			}
			//intersection.normal.set(xfn, yfn, zfn);
			upperBound = dist;
			intersection.object = this;
			intersection.faceIndex = i / 3;
		}
		if (intersection.object == this)
		{
			meshToGlobal.rdotAffine(intersection.position);
			meshToGlobal.rdot(intersection.normal);
			intersection.distance = upperBound * invDirLen;
			return intersection;
		}
		return null;
	}

	public void applyMatrix()
	{
		globalToMesh.invert(meshToGlobal);
		meshToGlobal.getCol(3, midpoint);
	}

	@Override
	public void setValue(SCENE_OBJECT_COLUMN_TYPE ct, Object o, VariableAmount variables, ParseUtil parser) throws OperationParseException
	{
		try {
			switch (ct)
			{
			case ACTIVE:active = ParseUtil.parseBoolean(o);break;
			case DELETE:break;
			case ID:id = ParseUtil.parseString(o);break;
			case POSITION:
			{
				parser.parsePositionString(o, midpoint, variables, controll);
				positionStr = parser.str;
				DataHandler.globalVariables.setGlobal(id.concat("_pos"), parser.op);
	    		meshToGlobal.setCol(3,midpoint);
	    		applyMatrix();
				break;
			}
			case COLOR:
			{
				color = parser.parseColor(o, variables, controll);
				colorStr = parser.str;
				break;
			}
			case TRACED_RAYS:numTracedRays = ParseUtil.parseInteger(o);break;
			case UNTRACED_RAYS:numUntracedRays = ParseUtil.parseInteger(o);break;
			case BIDIRECTIONAL:bidirectional = ParseUtil.parseBoolean(o);break;
			case DIFFUSE:diffuse = ParseUtil.parseDouble(o);break;
			case INVERT_INOUT:invertInsideOutside = ParseUtil.parseBoolean(o);break;
			case TRANSFORMATION:
			{
				parser.parseMat(o, meshToGlobal, variables, controll);
				transformationStr = parser.str;
				applyMatrix();
				break;
			}
			case MATERIAL:materialType = MaterialType.get(o);break;
			case SMOOTH:smooth = ParseUtil.parseBoolean(o);break;
			case TEXTURE_OBJECT:textureObjectStr = ParseUtil.parseString(o);break;
			default:break;
			}
			updateIds((byte)ct.ordinal(), parser.op);
			valueChanged(ct, parser);
		} catch (OperationParseException e) {
			logger.error("Can't read math expression",e);
		} catch (NumberFormatException e) {
			logger.error("Can't read number", e);
		}
		parser.reset();
	}

	@Override
	public void updateValue(SCENE_OBJECT_COLUMN_TYPE ct, VariableAmount variables, ParseUtil parser) throws OperationParseException
	{
		try {
			parser.op = null;
			switch (ct)
			{
			case ACTIVE:break;
			case DELETE:break;
			case ID:break;
			case POSITION:
			{
				parser.parsePositionString(positionStr, midpoint, variables, controll);
				DataHandler.globalVariables.setGlobal(id.concat("_pos"), parser.op);
	    		meshToGlobal.setCol(3,midpoint);
	    		applyMatrix();
				break;
			}
			case COLOR:
			{
				color = parser.parseColor(colorStr, variables, controll);
				break;
			}
			case TRACED_RAYS:break;
			case UNTRACED_RAYS:break;
			case BIDIRECTIONAL:break;
			case DIFFUSE:break;
			case INVERT_INOUT:break;
			case TRANSFORMATION:
			{
				parser.parseMat(transformationStr, meshToGlobal, variables, controll);
				applyMatrix();
				break;
			}
			case MATERIAL:break;
			case TEXTURE_OBJECT:ParseUtil.parseString(textureObjectStr);break;
			case SMOOTH:break;
			default:break;
			}
			updateIds((byte)ct.ordinal(), parser.op);
			valueChanged(ct, parser);
		} catch (OperationParseException e) {
			logger.error("Can't read math expression",e);
		} catch (NumberFormatException e) {
			logger.error("Can't read number", e);
		}
		parser.reset();
	}


	@Override
	public Object getValue(SCENE_OBJECT_COLUMN_TYPE ct)
	{
		switch (ct)
		{
		case ACTIVE:		return active;
		case DELETE:		break;
		case ID:			return id;
		case POSITION:		return positionStr;
		case TRANSFORMATION:return transformationStr;
		case MATERIAL:		return materialType;
		case DIFFUSE:		return diffuse;
		case TRACED_RAYS:	return numTracedRays;
		case UNTRACED_RAYS:	return numUntracedRays;
		case BIDIRECTIONAL:	return bidirectional;
		case INVERT_INOUT:	return invertInsideOutside;
		case COLOR:			return colorStr;
		case SMOOTH:		return smooth;
		case TEXTURE_OBJECT:		return textureObjectStr;
		case SAVE_TO:		break;
		case OPEN:			break;
		default:			throw new IllegalArgumentException(ct.name);
		}
		return null;
	}

	public void removeChangeListener(MeshObjectChangeListener toc) {
		changeListeners.remove(toc);
	}

	public void addDataChangeListener(MeshObjectChangeListener tcl) {
		changeListeners.add(tcl);
	}

	public void addChangeListener(MeshObjectChangeListener tcl) {
		changeListeners.add(tcl);
	}

	@Override
	public void valueChanged(SCENE_OBJECT_COLUMN_TYPE ct, ParseUtil parser)
	{
		if (!isUpdating )
		{
			isUpdating = true;
			for (int i = 0; i < changeListeners.size(); ++i)
			{
				try {
					changeListeners.get(i).valueChanged(this, ct);
				}catch(Exception e)
				{
					logger.error("Exception at invoking change Listener", e);
				}
			}
			isUpdating = false;
		}
	}

	public void setLines(int lines[])
	{
		this.lines = lines;
	}

	public void setData(double vertices[], int faces[], double textureCoordinates[])
	{
		this.textureCoordinates = textureCoordinates;
		this.vertices = vertices;
		weightPoint.set(0,0,0);
		radiusQ = 0;
		for (int i = 0; i < vertices.length; i += 3)
		{
			weightPoint.add(vertices, i);
		}
		weightPoint.multiply(1. / vertices.length);
		for (int i = 0; i < vertices.length; i += 3)
		{
			radiusQ = Math.max(radiusQ, weightPoint.distanceQ(vertices, i));
		}
		this.faces = faces;
		if (faces.length % 3 != 0)
		{
			throw new RuntimeException("Face-Array-Length must be multiple of three");
		}
		vertexNormals = new double[vertices.length];
		faceNormals = new double[faces.length];
		SortedIntegerArrayList sial[] = new SortedIntegerArrayList[vertices.length / 3];
		for (int i = 0; i < sial.length; ++i)
		{
			sial[i] = new SortedIntegerArrayList();
		}
		for (int i = 0; i < faces.length; i += 3)
		{
			int v0 = faces[i], v1 = faces[i + 1], v2 = faces[i + 2];
			sial[Math.min(v0,v1)].add(Math.max(v0,v1));
			sial[Math.min(v1,v2)].add(Math.max(v1,v2));
			sial[Math.min(v2,v0)].add(Math.max(v2,v0));
		}
		int count = 0;
		for (SortedIntegerArrayList current : sial)
		{
			count += current.size();
		}
		lines = new int[count * 2];
		count = 0;
		for (int i = 0; i < sial.length; ++i)
		{
			SortedIntegerArrayList current = sial[i];
			for (int j = 0; j < current.size(); ++j)
			{
				lines[count++] = i;
				lines[count++] = current.getI(j);
			}
		}
		/*lines = new int[faces.length * 2];
		for (int i = 0, li = 0; i < faces.length; i += 3)
		{
			lines[li++] = faces[i];
			lines[li++] = faces[i + 1];
			lines[li++] = faces[i + 1];
			lines[li++] = faces[i + 2];
			lines[li++] = faces[i + 2];
			lines[li++] = faces[i];
		}*/
		Geometry.calcTriangleMeshVertexFaceNormals(vertices, faces, vertexNormals, faceNormals);
		faceNormalsInversedLength = new double[faceNormals.length];
		normalizedIncrementingArea = new double[faces.length / 3];
		double sum = 0;
		for (int i = 0; i < faceNormals.length; i += 3)
		{
			double x = faceNormals[i], y = faceNormals[i + 1], z = faceNormals[i + 2];
			double len = x * x + y * y + z * z;
			sum += (normalizedIncrementingArea[i/3] = Math.sqrt(len));
			len = 1 / len;
			faceNormalsInversedLength[i] = x * len; faceNormalsInversedLength[i + 1] = y * len; faceNormalsInversedLength[i + 2] = z * len;
		}

		DoubleMatrixUtil.multiply(normalizedIncrementingArea, 0, normalizedIncrementingArea.length, 1/sum);
		DoubleMatrixUtil.partialSum(normalizedIncrementingArea, 0, normalizedIncrementingArea.length);
		modified();
		triggerModificationEvents();
	}

	public void load(String filepath) throws IOException
	{
		FileInputStream is = new FileInputStream(filepath);
		IntegerArrayList faceList = new IntegerArrayList();
		IntegerArrayList textureCoordIndexList = new IntegerArrayList();
		DoubleArrayList vertexList = new DoubleArrayList();
		DoubleArrayList textureCoordList = new DoubleArrayList();
		ObjectImporter.importTriangleWavefront(is, faceList, vertexList, textureCoordIndexList, textureCoordList);
		setData(vertexList.toArrayD(), faceList.toArrayI(), textureCoordList.size() * 3 == vertexList.size() * 2 ? textureCoordList.toArrayD() : null);
		is.close();
		//Geometry.calcTriangleMeshVertexFaceOrthorgonals(vertices, faces, vertexNormals, faceNormals);
	}

	public final int getMeshVertexLength()
	{
		return this.vertices.length;
	}

	public void getMeshVertices(float vertices[])
	{
		for (int i = 0; i < this.vertices.length; i += 3)
		{
			meshToGlobal.rdotAffine(this.vertices, i, vertices, i);
		}
	}

	public int[] getMeshFaces() {
		return faces;
	}

	public int[] getLines() {
		return lines;
	}

	public void saveTo(File file) throws IOException{
		OutputStream os = new FileOutputStream(file);
		ObjectImporter.exportTriangleWavefront(os, id, faces, vertices, textureCoordinates);
		os.close();
	}

	@Override
	public void read(OpticalObject other, VariableAmount va, ParseUtil parser)
	{
		super.read(other, va, parser);
		if (other instanceof MeshObject)
		{
			MeshObject m = (MeshObject)other;
			setData(m.vertices, m.faces, m.textureCoordinates);
		}
	}

	@Override
	public final OpticalObject copy(VariableAmount va, ParseUtil parser) {
		MeshObject res = new MeshObject(va, parser);
		res.read(this, va, parser);
		return res;
	}

	@Override
	public void getTextureCoordinates(Vector3d position, Vector3d direction, Vector2d v3) {
		throw new RuntimeException("not implemented");
	}

	@Override
	public void densityCompensation(int trWidth, int trHeight, int[] imageColorArray, int channels, int stride) {
		throw new RuntimeException("not implemented");
	}
}
