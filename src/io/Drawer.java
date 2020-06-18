package io;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Arrays;

import geometry.Line2d;
import util.StringUtils;
import util.data.DoubleArrayList;
import util.data.UniqueObjects;

public abstract class Drawer {
    private static final double GRAD_TO_RADIANS = Math.PI / 180;

    public abstract void fillCircle(double x, double y, double radius) throws IOException;
    
	public abstract int numQueuedPoints();

	public abstract void getClipBounds(Rectangle bounds);

	public abstract void drawLine(double x0, double y0, double x1, double y1) throws IOException;
	
	public abstract void pushPoint(double x, double y);
	
	public abstract void drawPolyLine() throws IOException;
	
	public abstract void drawArc(double x, double y, double width, double height, double startAngle, double arcAngle) throws IOException;
	
	public abstract void setPointNumber(int i);

	public abstract void setPoint(double d, double e, int i);
	
	public abstract void setColor(Color c);
	
	public abstract void drawChars(char data[], int offset, int length, double x, double y) throws IOException;

	public abstract void drawChars(CharSequence data, int offset, int length, double x, double y) throws IOException;

	public abstract void drawChar(char data, double x, double y) throws IOException;
	
	public void drawString(String str, int offset, int length, double x, double y) throws IOException
	{
		drawChars(str.toCharArray(), offset, length, x, y);
	}
	
	public void drawString(String str, double x, double y) throws IOException
	{
		drawString(str, 0, str.length(), x, y);
	}
	
    public static class SvgDrawer extends Drawer
    {
    	final private StringBuilder strB = new StringBuilder();
    	private BufferedWriter outBuf;
    	private char chBuf[] = new char[1024];
    	private final DoubleArrayList dal = new DoubleArrayList();
    	private Color col = Color.BLACK;
    	private int width, height;
    	private Line2d line = new Line2d();
		private boolean clip = true;
    	
    	public SvgDrawer(BufferedWriter outBuf)
    	{
    		this.outBuf = outBuf;
    	}
    	
    	public void setOutput(BufferedWriter outBuf)
    	{
    		this.outBuf = outBuf;
    	}
    	
    	public void beginDocument(int width, int height) throws IOException
    	{
    		chBuf = StringUtils.writeAndReset(outBuf, strB.append("<svg width=\"").append(width).append("\" height=\"").append(height).append("\" xmlns=\"http://www.w3.org/2000/svg\" version=\"1.1\">"), chBuf);
    		outBuf.newLine();
    		this.width = width;
    		this.height = height;
    	}
    	
    	public void endDocument() throws IOException
    	{
    		outBuf.write("</svg>");
    		outBuf.newLine();
    	}
    	
		@Override
    	public void drawChars(char data[], int offset, int length, double x, double y) throws IOException
    	{
      		if (Double.isNaN(x) || Double.isNaN(y)){return;}
			chBuf = StringUtils.writeAndReset(outBuf, strB.append("<text x=\"").append(x).append("\" y=\"").append(y).append("\" writing-mode=\"lr\">"), chBuf);
			outBuf.write(data, offset, length);
		    outBuf.write("</text>");
    	}
		
		@Override
    	public void drawChars(CharSequence data, int offset, int length, double x, double y) throws IOException
    	{
      		if (Double.isNaN(x) || Double.isNaN(y)){return;}
			chBuf = StringUtils.writeAndReset(outBuf, strB.append("<text x=\"").append(x).append("\" y=\"").append(y).append("\" writing-mode=\"lr\">"), chBuf);
			for (int i = 0; i < data.length(); ++i)
			{
				outBuf.write(data.charAt(i));
			}
		    outBuf.write("</text>");
    	}
		
		@Override
    	public void drawChar(char data, double x, double y) throws IOException
    	{
      		if (Double.isNaN(x) || Double.isNaN(y)){return;}
 			chBuf = StringUtils.writeAndReset(outBuf, strB.append("<text x=\"").append(x).append("\" y=\"").append(y).append("\" writing-mode=\"lr\">"), chBuf);
			outBuf.write(data);
		    outBuf.write("</text>");
    	}
    	
    	@Override
		public void fillCircle(double x, double y, double radius) throws IOException
    	{
       		if (Double.isNaN(x) || Double.isNaN(y) || Double.isNaN(radius)){return;}
    		chBuf = StringUtils.writeAndReset(outBuf, toString(strB.append("<circle cx=\"").append(x).append("\" cy=\"").append(y).append("\" r=\"").append(radius).append("\" fill=\""), col).append("\" fill-opacity=\"").append(col.getAlpha() / 255.).append("\" />"), chBuf);
    	 	outBuf.newLine();
    	}

       	@Override
		public void drawLine(double x0, double y0, double x1, double y1) throws IOException
       	{
       		if (Double.isNaN(x0) || Double.isNaN(y0) || Double.isNaN(x1) || Double.isNaN(y1)){return;}
       		line.set(x0, y0, x1, y1);
       		if (line.cropToRectangle(0, width, 0, height))
       		{
       			chBuf = StringUtils.writeAndReset(outBuf, toString(strB.append("<line x1=\"").append(line.x0).append("\" y1=\"").append(line.y0).append("\" x2=\"").append(line.x1).append("\" y2=\"").append(line.y1).append("\" stroke=\""), col).append("\" stroke-opacity=\"").append(col.getAlpha() / 255.).append("\" />"), chBuf);
       			outBuf.newLine();
       		}
       	}
       	
       	@Override
		public void pushPoint(double x, double y)
       	{
       		dal.add(x);
       		dal.add(y);
       	}
       	
       	@Override
       	public final int numQueuedPoints()
       	{
       		return dal.size() / 2;
       	}
       	
       	/*private static final void putValue(char tmp[], int pos, int col)
       	{
       		tmp[pos] = Character.forDigit(col / 16, 16);
       		tmp[pos + 1] = Character.forDigit(col % 16, 16);
       		
       	}*/
       	
       	private static final StringBuilder putValue(StringBuilder strB, int col)
       	{
       		return strB.append(Character.forDigit(col / 16, 16)).append(Character.forDigit(col % 16, 16));
       		
       	}
       	
       	/*private static final char[] toString(Color col, char tmp[])
       	{	
       		if (tmp.length < 7)
   			{
       			tmp = new char[7];
   			}
       		tmp[0] = '#';
       		putValue(tmp, 1, col.getRed());
       		putValue(tmp, 3, col.getGreen());
       		putValue(tmp, 5, col.getBlue());
       		return tmp;
       	}*/

       	private static final StringBuilder toString(StringBuilder strB, Color col)
       	{	
       		strB.append('#');
       		putValue(strB, col.getRed());
       		putValue(strB, col.getGreen());
       		putValue(strB, col.getBlue());
       		return strB;
       	}

		@Override
		public void drawPolyLine() throws IOException {
			if (dal.size() < 4)
			{
				dal.clear();
				return;
			}
			strB.append("<path d=\"");
			if (clip)
			{
				double x0 = dal.get(0), y0 = dal.get(1);
				boolean in0 = x0 >= 0 && x0 <= width && y0 >= 0 && y0 <= height;
				for (int i = 0; i < dal.size() - 2; i += 2)
				{
					double x1 = dal.get(i + 2), y1 = dal.get(i + 3);
					boolean in1 = x1 >= 0 && x1 <= width && y1 >= 0 && y1 <= height;
					boolean intersects = true;
					line.set(x0, y0, x1, y1);
					if (!in0 || !in1)
					{
						intersects = line.cropToRectangle(0, width, 0, height);
					}
					if (in0)
					{
						if (i == 0)
						{
							StringUtils.writeAndReset(outBuf, strB.append('M').append(' ').append(line.x0).append(',').append(line.y0), chBuf);
						}
						StringUtils.writeAndReset(outBuf, strB.append(' ').append('L').append(' ').append(line.x1).append(',').append(line.y1), chBuf);
					}
					else if (in1 || intersects)
					{
						StringUtils.writeAndReset(outBuf, strB.append(' ').append('M').append(' ').append(line.x0).append(',').append(line.y0), chBuf);
						StringUtils.writeAndReset(outBuf, strB.append(' ').append('L').append(' ').append(line.x1).append(',').append(line.y1), chBuf);
					}
					in0 = in1;
					x0 = x1;
					y0 = y1;
				}
			}
			else
			{
				chBuf = StringUtils.writeAndReset(outBuf, strB.append('M').append(' ').append(dal.getD(0)).append(',').append(dal.getD(1)), chBuf);
				for (int i = 2; i < dal.size(); i += 2)
				{
					chBuf = StringUtils.writeAndReset(outBuf, strB.append(' ').append('L').append(' ').append(dal.getD(i)).append(',').append(dal.getD(i + 1)), chBuf);
				}

			}
			if (strB.length() == 0)
			{
				chBuf = StringUtils.writeAndReset(outBuf, toString(strB.append("\" stroke=\""), col).append("\" stroke-opacity=\"").append(col.getAlpha() / 255.).append("\" fill=\"none\" />"), chBuf);
				outBuf.newLine();
			}
			else
			{
				strB.setLength(0);
			}
			dal.clear();				
		}

		@Override
		public void drawArc(double x, double y, double width, double height, double startAngle, double arcAngle) throws IOException {
			double xMid = x + width * 0.5;
			double yMid = y + height * 0.5;
			int steps = (int)(arcAngle * 0.2);
			double step = arcAngle / steps * GRAD_TO_RADIANS;
			startAngle *= GRAD_TO_RADIANS;
			width *= 0.5;
			height *= 0.5;
			
			strB.append("<path d=\"");
			if (clip)
			{
				double x0 = xMid + Math.cos(startAngle) * width, y0 = yMid + Math.sin(startAngle) * height;
				boolean in0 = x0 >= 0 && x0 <= this.width && y0 >= 0 && y0 <= this.height;
				for (int i = 1; i <= steps; ++i)
				{
					double arc = step * i + startAngle;
					double x1 = xMid + Math.cos(arc) * width, y1 = yMid + Math.sin(arc) * height;
					boolean in1 = x1 >= 0 && x1 <= this.width && y1 >= 0 && y1 <= this.height;
					boolean intersects = true;
					line.set(x0, y0, x1, y1);
					if (!in0 || !in1)
					{
						intersects = line.cropToRectangle(0, this.width, 0, this.height);
					}
					if (in0)
					{
						if (i == 1)
						{
							StringUtils.writeAndReset(outBuf, strB.append('M').append(' ').append(line.x0).append(',').append(line.y0), chBuf);
						}
						StringUtils.writeAndReset(outBuf, strB.append(' ').append('L').append(' ').append(line.x1).append(',').append(line.y1), chBuf);
					}
					else if (in1 || intersects)
					{
						StringUtils.writeAndReset(outBuf, strB.append(' ').append('M').append(' ').append(line.x0).append(',').append(line.y0), chBuf);
						StringUtils.writeAndReset(outBuf, strB.append(' ').append('L').append(' ').append(line.x1).append(',').append(line.y1), chBuf);
					}
					in0 = in1;
					x0 = x1;
					y0 = y1;
				}
			}
			else
			{
				chBuf = StringUtils.writeAndReset(outBuf, strB.append('M').append(xMid + Math.cos(startAngle) * width).append(',').append(yMid + Math.sin(startAngle) * height), chBuf);
				for (int i = 1; i <= steps; ++i)
				{
					double arc = step * i + startAngle;
					chBuf = StringUtils.writeAndReset(outBuf, strB.append(' ').append('L').append(' ').append(xMid + Math.cos(arc) * width).append(',').append(yMid + Math.sin(arc) * height), chBuf);
				}
			}
			if (strB.length() == 0)
			{
				chBuf = StringUtils.writeAndReset(outBuf, toString(strB.append("\" stroke=\""), col).append("\" stroke-opacity=\"").append(col.getAlpha() / 255.).append("\" fill=\"none\" />"), chBuf);
				outBuf.newLine();
			}
			else
			{
				strB.setLength(0);
			}
	 	}

		@Override
		public void setPointNumber(int count) {
			dal.setSize(count * 2);
		}

		@Override
		public void setPoint(double x, double y, int i) {
			dal.set(i * 2, x);
			dal.set(i * 2 + 1, y);
		}

		@Override
		public void setColor(Color c) {
			this.col = c;
		}

		@Override
		public void getClipBounds(Rectangle bounds) {
			bounds.setBounds(0, 0, width, height);
		}
		
		@Override
		public int getWidth() {
			return width;
		}

		@Override
		public int getHeight() {
			return height;
		}
    }

	
    public static class GraphicsDrawer extends Drawer
    {
    	Graphics g;
    	int index = 0;
    	int xPoints[] = UniqueObjects.EMPTY_INT_ARRAY;
    	int yPoints[] = UniqueObjects.EMPTY_INT_ARRAY;
    	private char charArray[] = new char[1];
    	private Line2d line = new Line2d();
    	private Rectangle rect = new Rectangle();
    	double points[] = UniqueObjects.EMPTY_DOUBLE_ARRAY;
		Path2D path = new Path2D.Double();
    	int width, height;
    	
    	public GraphicsDrawer(Graphics g)
    	{
    		this(g, 0);
    	}
    	
    	public Graphics getOutput()
    	{
    		return g;
    	}
    	
    	public GraphicsDrawer(Graphics g, int initArrayLength)
    	{
    		this.g = g;
    		xPoints = new int[initArrayLength];
    		yPoints = new int[initArrayLength];
    		if (g != null)
    		{
    			g.getClipBounds(rect);
    			width = rect.width;
    			height = rect.height;
    		}
    	}
    	
    	public void setOutput(Graphics g)
    	{
    		this.g = g;
    		g.getClipBounds(rect);
			width = rect.width;
			height = rect.height;
    	}
    	
		@Override
    	public void drawChars(char data[], int offset, int length, double x, double y)
    	{
    		g.drawChars(data, offset, length, (int)x, (int)y);
    	}

		@Override
    	public void drawChars(CharSequence data, int offset, int length, double x, double y)
    	{
			if (charArray.length < length)
			{
				charArray = new char[length - offset];
			}
			for (int i = 0; i < length; ++i)
			{
				charArray[i] = data.charAt(offset + i);
			}
    		g.drawChars(charArray, 0, length, (int)x, (int)y);
    	}

		@Override
    	public void drawChar(char data, double x, double y)
    	{
			charArray[0] = data;
    		g.drawChars(charArray, 0, 1, (int)x, (int)y);
    	}

		@Override
    	public void fillCircle(double x, double y, double radius) throws IOException
    	{
    		g.fillArc((int)(x-radius), (int)(y-radius), (int)(radius * 2), (int)(radius * 2), 0, 360);
    	}
    	
		private final Line2D line2d = new Line2D.Double();
		@Override
    	public void drawLine(double x0, double y0, double x1, double y1) throws IOException
    	{
			line.set(x0, y0, x1, y1);
			if (line.cropToRectangle(0, width, 0, height))
			{
				Graphics2D g2d = (Graphics2D)g;
				line2d.setLine(x0, y0, x1, y1);
				g2d.draw(line2d);
				//g.drawLine((int)line.x0, (int)line.y0, (int)line.x1, (int)line.y1);
			}
    	}

		@Override
		public void pushPoint(double x, double y) {
			if (index * 2 + 1 >= points.length)
			{
				points = Arrays.copyOf(points, points.length * 2 + 2);
			}
			points[index * 2] = x;
			points[index * 2 + 1] = y;
			++index;
		}
		
		@Override
		public final int numQueuedPoints()
		{
			return index;
		}

		@Override
		public void drawPolyLine() throws IOException{
			if (points.length == 0)
			{
				return;
			}

			if (g instanceof Graphics2D)
			{
				Graphics2D g2d = (Graphics2D)g;
				//System.out.println(Arrays.toString(points));
				path.moveTo(points[0], points[1]);
				for (int i = 1; i < index; ++i)
				{
					path.lineTo(points[i * 2], points[i * 2 + 1]);
				}
				g2d.draw(path);
				path.reset();
			}
			else
			{
				int writeIndex = 0;
				if (xPoints.length * 2 < points.length)
				{
					xPoints = new int[points.length / 2];
					yPoints = new int[points.length / 2];
				}
				for (int i = 0; i < index; ++i)
				{
					if (points[i * 2] < 0 || points[i * 2] >= width || points[i * 2 + 1] < 0 || points[i * 2 + 1] >= height)
					{
						if (i != 0)
						{
							line.x0 = points[i * 2 - 2];
							line.y0 = points[i * 2 - 1];
							line.x1 = points[i * 2];
							line.y1 = points[i * 2 + 1];
							line.cropToRectangle(rect);
							xPoints[writeIndex] = (int)line.x0;
							yPoints[writeIndex] = (int)line.y0;
							g.drawPolyline(xPoints, yPoints, index);
						}
					}
					xPoints[writeIndex] = (int)points[i * 2];
					yPoints[writeIndex] = (int)points[i * 2 + 1];
					++writeIndex;
				}
			}
			index = 0;
		}

		@Override
		public void drawArc(double x, double y, double width, double height, double startAngle, double arcAngle) throws IOException {
			g.drawArc((int)x, (int)y, (int)width, (int)height, (int)startAngle, (int)arcAngle);
		}

		@Override
		public void setPointNumber(int count) {
			index = count;
			if (xPoints.length < index || yPoints.length < index)
			{
				xPoints = Arrays.copyOf(xPoints, index);
				yPoints = Arrays.copyOf(yPoints, index);
			}
		}

		@Override
		public void setPoint(double x, double y, int i) {
			xPoints[i] = (int)x;
			yPoints[i] = (int)y;
		}

		@Override
		public void setColor(Color c) {
			g.setColor(c);
		}

		@Override
		public void getClipBounds(Rectangle bounds) {
			g.getClipBounds(bounds);
		}
		
		@Override
		public int getWidth() {
			return width;
		}

		@Override
		public int getHeight() {
			return height;
		}
    }

    public abstract int getWidth();
    
	public abstract int getHeight();
}
