package geometry;

public class MeshTree {
	public double minX, minY, minZ;
	public double maxX, maxY, maxZ;
	MeshTreeNode root;
	
	

	public static class MeshTreeNode{
		public int faces[];
		public MeshTreeNode children[] = null;
	}
	
	public static MeshTree create(int faces[], double vertices[])
	{
		return new MeshTree();
	}
}
