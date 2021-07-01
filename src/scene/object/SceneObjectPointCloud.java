package scene.object;

import geometry.FloatVectorObject;

public class SceneObjectPointCloud extends SceneObject {
	private FloatVectorObject vertex = new FloatVectorObject();
	private float vectorField[] = new float[30000];
	private int resetCount[] = new int[10000];
	private int dimX, dimY, dimZ;
	//private double minX, maxX, minY, maxY, minZ, maxZ;
	double recreationRate = 0.001;
	private int usedMemory;
	//private int vertexCount;

	public int getVertexLineLength()
	{
		return 10;
	}
	
	private Thread th = new Thread()
	{
		public void run()
		{
			while(true)
			{
				update();
			}
		}
	};

	public SceneObjectPointCloud()
	{
		th.start();
	}
	
	public boolean setSize(int dimX, int dimY, int dimZ)
	{
		if (this.dimX == dimX && this.dimY == dimY && this.dimZ == dimZ)
            return true;
    	final int limit = getMemoryLimit(), usedMemory = dimX*dimY*dimZ;
        if (dimX < 1 || dimY < 1 || dimZ < 1 || (limit != -1 && usedMemory> limit*0x10000)){
        	reset();
            return false;        	
        }
        try{
        	int vertexCount = dimX * dimY * dimZ;
        	FloatVectorObject vertex = new FloatVectorObject(vertexCount);
        	
        	synchronized(this){
	        	this.usedMemory = usedMemory;
	            this.dimX = dimX;
	            this.dimY = dimY;
	            this.dimZ = dimZ;
	            //this.vertexCount = vertexCount;
	            this.vertex = vertex;
	            update(UpdateKind.DIMENSION);
        	}
            return true;
    	}catch(OutOfMemoryError e){
    		reset();
    		throw e;
    	}
	}
	
	public float[] getVectorField() {
		return vectorField;
	}
	
	public int[] getResetCount() {
		return resetCount;
	}
	
	public void reset() {}
	
	public void update()
	{
		
		float verticesX[];
		float verticesY[];
		float verticesZ[];
		int dimX, dimY, dimZ;
		synchronized(this)
		{
			verticesX = vertex.x;
			verticesY = vertex.y;
			verticesZ = vertex.z;
			dimX = this.dimX;
			dimY = this.dimY;
			dimZ = this.dimZ;
		}
		for (int i = 0; i < vectorField.length; i += 3)
		{
			float x = vectorField[i];
			float y = vectorField[i + 1];
			float z = vectorField[i + 2];
			int xi = (int)x;
			int yi = (int)y;
			int zi = (int)z;
			if (xi >= 0 && xi < dimX && yi >= 0 && yi < dimY && zi >= 0 && zi < dimZ && Math.random() > recreationRate)
			{
				int index = (zi * dimY + yi) * dimX + xi;
				vectorField[i] = x + verticesX[index];
				vectorField[i + 1] = y + verticesY[index];
				vectorField[i + 2] = z + verticesZ[index];
			}
			else
			{
				vectorField[i] = (float)Math.random() * dimX;
				vectorField[i + 1] = (float)Math.random() * dimY;
				vectorField[i + 2] = (float)Math.random() * dimZ;
				++resetCount[i/3];
			}	
		}
		try {
			Thread.sleep(10);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public float[] getVerticesX(){
    	return vertex.x;
    }
    
    public float[] getVerticesY(){
    	return vertex.y;
    }
    
    public float[] getVerticesZ(){
    	return vertex.z;
    }
    

	@Override
	public boolean isCachable() {
		return false;
	}

	@Override
	public boolean drawTypeAllowed(byte dt) {
		switch (dt)
		{
		case DrawType.DOTS:return true;
		case DrawType.LINES:
		case DrawType.LINE_STRICLES:
		case DrawType.SOLID:
		case DrawType.SOLID_SMOOTH:return false;
		default:
			throw new IllegalArgumentException();
		}
	}

	@Override
	public boolean hasNormals() {
		return false;
	}

	@Override
	public int getVertexCount() {
		return resetCount.length;
	}

	@Override
	public int getFaceCount() {
		return 0;
	}

	@Override
	public int getUsedMemory() {
		return usedMemory;
	}

}
