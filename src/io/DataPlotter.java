package io;

import java.awt.Color;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.ArrayList;

import util.ArrayUtil;

public class DataPlotter {
	ArrayList<Object> objects = new ArrayList<>();
	private class PlotObject
	{
		public PlotObject(double x[], double y[], String name, Color col)
		{
			this.x = x;
			this.y = y;
			this.name = name;
			this.col = col;
		}
		
		double x[];
		double y[];
		String name;
		Color col;
	}
	
	
	public DataPlotter()
	{
		
	}
	
	public void addPlot(double x[], double y[], String name, Color color)
	{
		objects.add(new PlotObject(x, y, name, color));
	}
	
	public void getBounts(Rectangle2D bounds)
	{
		double minX = Double.POSITIVE_INFINITY, maxX = Double.NEGATIVE_INFINITY;
		double minY = Double.POSITIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;
		for (int i = 0; i < objects.size(); ++i)
		{
			Object current = objects.get(i);
			if (current instanceof PlotObject)
			{
				PlotObject po = (PlotObject)current;
				minX = Math.min(minX, ArrayUtil.min(po.x));
				maxX = Math.max(maxX, ArrayUtil.max(po.x));
				minY = Math.min(minX, ArrayUtil.min(po.y));
				maxY = Math.max(maxX, ArrayUtil.max(po.y));
			}
		}
		bounds.setFrameFromDiagonal(minX, minY, maxX, maxY);
	}
	
	public void plot(Drawer drawer, Rectangle2D drawBounds) throws IOException
	{
		Rectangle2D bounds = new Rectangle2D.Double();
		getBounts(bounds);
		double gap = 5;
		double minX = bounds.getMinX();
		double minY = bounds.getMinY();
		double invWidth = 1 / bounds.getWidth();
		double invHeight = 1 / bounds.getHeight();
		double drawMinX = drawBounds.getMinX() + gap;
		double drawWidth = drawBounds.getWidth() - 2 * gap;
		double drawMinY = drawBounds.getMinY() + gap;
		double drawHeight = drawBounds.getHeight() - 2 * gap;
		for (int i = 0; i < objects.size(); ++i)
		{
			Object current = objects.get(i);
			if (current instanceof PlotObject)
			{
				PlotObject po = (PlotObject)current;
				drawer.setColor(po.col);
				for (int j = 0; j < po.x.length; ++j)
				{
					drawer.pushPoint(((po.x[j] - minX) * invWidth) * drawWidth + drawMinX, ((po.y[j] - minY) * invHeight) * drawHeight + drawMinY);
				}
				drawer.drawString(po.name, 10, 10);
			}
			drawer.drawPolyLine();
			drawer.drawString(Double.toString(minX), 0, 10);
			drawer.drawString(Double.toString(minY), 0, 20);
			drawer.drawString(Double.toString(bounds.getMaxX()), drawBounds.getMaxX()-50, 10);
			drawer.drawString(Double.toString(bounds.getMaxY()), 10, drawBounds.getMaxY()-10);
		}
		
	}
}
