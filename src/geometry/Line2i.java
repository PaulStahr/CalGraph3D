package geometry;

public class Line2i {
	public int x0, y0, x1, y1;
	
	public void set(int x0, int y0, int x1, int y1)
	{
		this.x0 = x0;
		this.y0 = y0;
		this.x1 = x1;
		this.y1 = y1;
	}
	
	public boolean cropToRectangle(int minX, int maxX, int minY, int maxY)
    {
    	double begin = 0;
    	double end = 0;
    	double diffX = x1 - x0;
    	double diffY = y1 - y0;
    	if (x0 < minX)
		{
			begin = Math.max(begin,(minX - x0) / diffX);
		}
		else if (x0 > maxX)
		{
			begin = Math.max(begin,(maxX - x0) / diffX);
		}
		if (y0 < minY)
		{
			begin = Math.max(begin,(minY-y0) / diffY);
		}
		else if (y0 > maxY)
		{
			begin = Math.max(begin,(maxY - y0) / diffY);
		}
		if (x1 < minX)
		{
			end = Math.max(end, (x1 - minX) / diffX);
		}
		else if (x1 > maxX)
		{
			end = Math.max(end, (x1 - maxX) / diffX);
		}
		if (y1 < minY)
		{
			end = Math.max(end,(y1 - minY) / diffY);
		}
		else if (y1 > maxY)
		{
			end = Math.max(end, (y1 - maxY) / diffY);
		}
		if (begin + end< 1)
		{
			x0 += diffX * begin;
			y0 += diffY * begin;
			x1 -= diffX * end;
			y1 -= diffY * end;
			return true;
		}
		return false;
    }
	    

}
