package util;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImageSaver implements Runnable
{
	Logger logger = LoggerFactory.getLogger(ImageSaver.class);
	BufferedImage img;
	File file;
	public ImageSaver(BufferedImage img, File file)
	{
		this.img = img;
		this.file = file;
	}
	
	@Override
	public void run()
	{
		try {
			ImageIO.write(img, "png", file);
		} catch (IOException e) {
			logger.error("Can't write image", e);
		}
	}
}