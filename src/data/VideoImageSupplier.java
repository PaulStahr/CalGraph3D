package data;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Arrays;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ij.ImagePlus;
import util.ThreadPool.RunnableObject;

public interface VideoImageSupplier {
    public static final Logger logger = LoggerFactory.getLogger(VideoImageSupplier.class);
	public abstract BufferedImage getFrame(int frame) throws IOException;
	public abstract int count();

	public static class StaticImage implements VideoImageSupplier
	{
		BufferedImage img;

		public StaticImage(BufferedImage img)
		{
			this.img = img;
		}

		@Override
		public BufferedImage getFrame(int frame) {
			return img;
		}

		@Override
		public int count() {return 1;}
	}

	static abstract class DynamicImage implements VideoImageSupplier, Runnable
	{
		private volatile int lastLoaded = 0;
		private Object[] images;
	    final RunnableObject ro = new RunnableObject(this, "preload adjacent image", null);

		public DynamicImage(int length) {
			images = new WeakReference<?>[length];
		}

		@Override
		public BufferedImage getFrame(int frame) throws IOException
		{
			@SuppressWarnings("unchecked")
			WeakReference<BufferedImage> ref = (WeakReference<BufferedImage>)images[frame];
			lastLoaded = frame;
			BufferedImage res;
			if (ref != null && (res = ref.get()) != null)
			{
				preload_adjacent();
				return res;
			}
			res = load(frame);
			images[frame] = new WeakReference<>(res);
			preload_adjacent();
			return res;
		}

		@SuppressWarnings("unchecked")
        private void preload_adjacent() {
		    int frame = lastLoaded;
		    if (frame > 0) {
    		    WeakReference<BufferedImage> ref = (WeakReference<BufferedImage>)images[frame - 1];
                if (ref == null || ref.get() == null) {
                    DataHandler.runnableRunner.run(ro,false);
                    return;
                }
		    }
		    if (frame < images.length - 1)
            {
    		    WeakReference<BufferedImage> ref = (WeakReference<BufferedImage>)images[frame + 1];
                if (ref == null || ref.get() == null) {
                    DataHandler.runnableRunner.run(ro,false);
                    return;
                }
            }
		}

		@Override
		public void run()
		{
			try
			{
				int frame = lastLoaded;
				if (frame > 0)
				{
					@SuppressWarnings("unchecked")
					WeakReference<BufferedImage> ref = (WeakReference<BufferedImage>)images[frame - 1];
					if (ref == null || ref.get() == null)
					{
						images[frame - 1] = new WeakReference<>(load(frame - 1));
					}
				}
				if (frame < images.length - 1)
				{
					@SuppressWarnings("unchecked")
					WeakReference<BufferedImage> ref = (WeakReference<BufferedImage>)images[frame + 1];
					if (ref == null || ref.get() == null)
					{
						images[frame + 1] = new WeakReference<>(load(frame + 1));
					}
				}
			}catch(IOException e)
			{
				logger.error("Can't load image", e);
			}
		}

		public abstract BufferedImage load(int frame) throws IOException;
	}

	public static class ImagePlusVideo  extends DynamicImage
	{
		ImagePlus ip;

		public ImagePlusVideo(ImagePlus ip)
		{
			super(ip.getImageStackSize());
			this.ip = ip;
		}

		@Override
		public synchronized BufferedImage load(int frame)
		{
			ip.setT(frame);
			ip.setZ(frame);
			return ip.getBufferedImage();
		}

        @Override
        public int count() {
            return ip.getNSlices();
        }
	}

	public static class ImageFileList extends DynamicImage
	{
		private File files[];
		public ImageFileList(File files[])
		{
			super(files.length);
			this.files = files;
			if (logger.isDebugEnabled()) {logger.debug(Arrays.toString(files));}
		}

		@Override
		public BufferedImage load(int frame) throws IOException
		{
			return ImageIO.read(files[frame]);
		}

		@Override
        public int count()
		{
		    return files.length;
		}
	}
}
