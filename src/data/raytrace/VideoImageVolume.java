package data.raytrace;

import java.awt.image.BufferedImage;
import java.io.IOException;

import data.VideoImageSupplier;
import util.data.DoubleList;

public class VideoImageVolume implements DoubleList{

    private VideoImageSupplier vis;
    private final int width, height, begin, end;
    int rgbArray[];

    public VideoImageVolume(VideoImageSupplier vis, int begin, int end) throws IOException
    {
        this.vis = vis;
        BufferedImage bi = vis.getFrame(begin);
        width = bi.getWidth();
        height = bi.getHeight();
        rgbArray = new int[width * height];
        this.begin = begin;
        this.end = end;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getDepth() {
        return end - begin;
    }

    @Override
    public int size() {
        return getWidth() * getHeight() * getDepth();
    }

    @Override
    public double getD(int index) {
        try {
            BufferedImage bi = vis.getFrame(index / (width * height));
            return bi.getRGB(index % width, (index / width) % height);
        } catch (IOException e) {
            return Double.NaN;
        }
    }

    @Override
    public double getD(long index) {
        try {
            BufferedImage bi = vis.getFrame((int)(index / (width * height)));
            return bi.getRaster().getSample((int)(index % width), (int)((index / width) % height),0);
            //int rgb = bi.getRGB((int)(index % width), (int)((index / width) % height));
            //return  (rgb >> 16) & 0x000000FF;
        } catch (IOException e) {
            return Double.NaN;
        }
    }

    @Override
    public void setElem(int index, double value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public double[] toArray(double[] data, int offset, long begin, long end) {
        if (begin == end){return data;}
        try {
            int imageSize = width * height;
            int beginFrame = (int)(begin / imageSize);
            int endFrame = (int)((end + imageSize - 1) / imageSize);
            for (int frame = beginFrame; frame < endFrame; ++frame)
            {
                BufferedImage bi = vis.getFrame(frame);
                bi.getRaster().getSamples(0, 0, width, height, 0, rgbArray);
                int beginPixel = (int)(Math.max(begin, (long)frame * imageSize) % imageSize);
                int endPixel = (int)((Math.min(end, (long)(frame + 1) * imageSize) - 1) % imageSize + 1);
                for (int i = beginPixel; i < endPixel; ++i)
                {
                    data[offset++] = rgbArray[i];
                }
            }
            return data;
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public float[] toArray(float[] data, int offset, long begin, long end) {
        if (begin == end){return data;}
        try {
            int imageSize = width * height;
            int beginFrame = (int)(begin / imageSize);
            int endFrame = (int)((end + imageSize - 1) / imageSize);
            for (int frame = beginFrame; frame < endFrame; ++frame)
            {
                BufferedImage bi = vis.getFrame(frame);
                bi.getRaster().getSamples(0, 0, width, height, 0, rgbArray);
                int beginPixel = (int)(Math.max(begin, (long)frame * imageSize) % imageSize);
                int endPixel = (int)((Math.min(end, (long)(frame + 1) * imageSize) - 1) % imageSize + 1);
                for (int i = beginPixel; i < endPixel; ++i)
                {
                    data[offset++] = rgbArray[i];
                }
            }
            return data;
        } catch (IOException e) {
            return null;
        }
    }
}
