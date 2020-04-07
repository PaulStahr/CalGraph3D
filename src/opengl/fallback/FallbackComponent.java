package opengl.fallback;

import java.awt.Canvas;
import java.awt.image.RenderedImage;

import opengl.OpenGlInterface;

public class FallbackComponent extends Canvas implements OpenGlInterface {

	/**
	 * 
	 */
	private static final long serialVersionUID = 527630882003281836L;

	@Override
	public void addPerFrameRunnable(Runnable runnable) {
		// TODO Auto-generated method stub

	}

	@Override
	public void removePerFrameRunnable(Runnable runnable) {
		// TODO Auto-generated method stub

	}

	@Override
	public RenderedImage getScreenshot() {
		return null;
	}

	@Override
	public void dispose() {
	}

}
