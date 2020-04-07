package opengl.font;

public final class ImageFrameData{
	public int x, y;
	public int lastUsed;
	public char character;
	public ImageFrameData(int x, int y){
		this.x = x;
		this.y = y;
	}
	
	@Override
	public String toString(){
		return "char" + ':' + '\'' + character + "' pos:(" + x + ',' + y + ") lastUsed:" + lastUsed;
	}
}