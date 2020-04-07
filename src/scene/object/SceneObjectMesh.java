package scene.object;

public final class SceneObjectMesh extends SceneObject {
	private int faces[];
	private float vertices[];
	private float normals[];
	public static final byte TRIANGLE = 1, QUAD = 2;
	private byte type;

	public SceneObjectMesh(byte type) {
		this(0,0, type);
	}
	
	public byte getType()
	{
		return type;
	}
	
	public SceneObjectMesh(int vertexCount, int faceCount, byte type)
	{
		faces = new int[faceCount * 4];
		vertices = new float[vertexCount * 3];
		normals = new float[vertexCount * 3];
		this.type = type;
		update(UpdateKind.DIMENSION);
	}
	
	public void setData(float vertices[], int faces[])
	{
		if (vertices.length == this.vertices.length)
		{
			int oldfacesLength = this.faces.length;
			this.faces = faces;
			this.vertices = vertices;
			if (faces.length != oldfacesLength)
			{
				update(UpdateKind.DIMENSION);
			}
		}
		else
		{
			synchronized(this)
			{
				this.faces = faces;
				this.vertices = vertices;
				this.normals = new float[vertices.length];
				update(UpdateKind.DIMENSION);
			}
		}
	}
	
	public boolean setSize(int vertexCount, int faceCount)
	{
		if (faces.length == faceCount * 4 && vertices.length == vertexCount * 3)
		{
			return true;
		}
		synchronized(this)
		{
			if (faces.length != faceCount * 4)
			{
				faces = new int[faceCount * 4];
			}
			if (vertices.length != vertexCount * 3)
			{
				vertices = new float[vertexCount * 3];
				normals = new float[vertexCount * 3];
			}
			update(UpdateKind.DIMENSION);
			return true;
		}
	}

	@Override
	public boolean isCachable() {
		return true;
	}

	public float[] getVertices()
	{
		return vertices;
	}
	
	public int[] getFaces()
	{
		return faces;
	}
	
	public float[] getNormals()
	{
		return normals;
	}
	
	@Override
	public boolean drawTypeAllowed(byte dt) {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public boolean hasNormals() {
		return true;
	}

	@Override
	public int getVertexCount() {
		return vertices.length / 3;
	}

	@Override
	public int getFaceCount() {
		return faces.length / 4;
	}

	@Override
	public int getUsedMemory() {
		return (faces.length + vertices.length) * 4;
	}

}
