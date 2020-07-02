package jcomponents.raytrace;

import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferedImage;
import java.lang.ref.WeakReference;

import javax.swing.JPanel;

import data.raytrace.DataChangeListener;
import data.raytrace.OpticalObject;
import geometry.Vector2d;

public class ImageComponent extends JPanel implements MouseMotionListener, MouseWheelListener, MouseListener{
	/**
	 * 
	 */
	private static final long serialVersionUID = 4680367482840226760L;
	
	
	private static class TextureChangeListener implements Runnable{
		final WeakReference<ImageComponent> ref;
		
		public TextureChangeListener(ImageComponent comp)
		{
			ref = comp.ref;
		}
		
		@Override
		public void run() {
			ImageComponent comp = ref.get();
			if (comp != null)
			{
				comp.repaint();
			}
		}
	}
	
	public static abstract class AbstractTextureChangeListener implements Runnable, DataChangeListener{
		final WeakReference<ImageComponent> ref;
		
		public AbstractTextureChangeListener(ImageComponent comp)
		{
			ref = comp.ref;
		}
		
		public abstract void dataChanged(ImageComponent comp);
		

		@Override
		public void dataChanged(OpticalObject source) {
			run();
		}
		
		@Override
		public void run() {
			ImageComponent comp = ref.get();
			if (comp != null)
			{
				dataChanged(comp);
				comp.repaint();
			}
		}
	}
	
	public final WeakReference<ImageComponent> ref = new WeakReference<ImageComponent>(this);
	public final TextureChangeListener tcl = new TextureChangeListener(this);
	private final Vector2d paintOffset = new Vector2d();
	private Vector2d originalPaintOffset = new Vector2d();
	int startX, startY;
	BufferedImage image;
	public double scale = 1;
	@Override
	protected synchronized void paintComponent( Graphics g )
	{
		super.paintComponent(g);
		if (image == null)
		{
			g.drawString("Null", 10, 10);
			return;
		}
		double x = getWidth() / (2 * scale) + paintOffset.x, y = getHeight() / (2 * scale) + paintOffset.y;
    	int dx1 = 0, sx1 = 0, dx2 = getWidth(), sx2 = image.getWidth();
		int dy1 = 0, sy1 = 0, dy2 = getHeight(), sy2 = image.getHeight();
		dx1 = (int)(x * scale);
		dx2 = (int)((image.getWidth() + x) * scale);
		dy1 = (int)(y * scale);
		dy2 = (int)((image.getHeight() + y) * scale);
		g.drawImage(image, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, null);
	}
	
	@Override
	public void mouseReleased(MouseEvent e) {}
	
	@Override
	public void mousePressed(MouseEvent e) {
		originalPaintOffset.set(paintOffset);
		startX = e.getX();
		startY = e.getY();
	}
	
		
	@Override
	public void mouseExited(MouseEvent e) {}
	@Override
	public void mouseEntered(MouseEvent e) {}
	@Override
	public void mouseClicked(MouseEvent e) {}
	@Override
	public void mouseMoved(MouseEvent e) {}
	
	@Override
	public void mouseDragged(MouseEvent e) {
		paintOffset.set(originalPaintOffset);
		paintOffset.x += (e.getX() - startX) / scale;
		paintOffset.y += (e.getY() - startY) / scale;
		repaint();
	}
	
	@Override
	public void mouseWheelMoved(MouseWheelEvent e) {
		scale *= Math.exp(-0.01*e.getUnitsToScroll());
		repaint();
	}

	public ImageComponent(BufferedImage image) {
		this.image = image;
		addMouseListener(this);
		addMouseMotionListener(this);
		addMouseWheelListener(this);
	}

	public ImageComponent()
	{
		this(null);
	}

	public void setImage(BufferedImage image) {
		this.image = image;
		repaint();
	}

	public void fitScaling() {
		paintOffset.set(-image.getWidth() / 2,-image.getHeight() / 2);
		scale = Math.min((double)getWidth() / image.getWidth(), (double)getHeight() / image.getHeight());
		repaint();
	}
}