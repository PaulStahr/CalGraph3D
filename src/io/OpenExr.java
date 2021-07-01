package io;

import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferFloat;
import java.awt.image.PixelInterleavedSampleModel;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.File;
import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.util.tinyexr.EXRHeader;
import org.lwjgl.util.tinyexr.EXRImage;
import org.lwjgl.util.tinyexr.EXRVersion;
import org.lwjgl.util.tinyexr.TinyEXR;

import util.ArrayUtil;

public class OpenExr {
    public static BufferedImage read(File file) {
        EXRVersion exr_version = EXRVersion.create();

        String path = file.getPath();
        int ret = TinyEXR.ParseEXRVersionFromFile(exr_version, path);
        if (ret != TinyEXR.TINYEXR_SUCCESS) {
            System.out.println("Invalid EXR file: " + path);
            return null;
        }

        if (exr_version.multipart()) {
            System.out.println("Multipart unsupported");
            return null;
        }

        EXRHeader exr_header = EXRHeader.create();
        TinyEXR.InitEXRHeader(exr_header);

        PointerBuffer err = BufferUtils.createPointerBuffer(1);
        ret = TinyEXR.ParseEXRHeaderFromFile(exr_header, exr_version, path, err);
        if (ret != TinyEXR.TINYEXR_SUCCESS) {
            System.out.println("EXR parse error: " + err.get(0));
            return null;
        }

        // Read HALF channel as FLOAT. (Not needed, image is float)
        //for (int i = 0; i < exr_header.num_channels(); i++) {
        //    if (exr_header.pixel_types().get(i) == TINYEXR_PIXELTYPE_HALF) {
        //        exr_header.requested_pixel_types().put(i, TINYEXR_PIXELTYPE_FLOAT);
        //    }
        //}

        EXRImage exr_image = EXRImage.create();
        TinyEXR.InitEXRImage(exr_image);

        ret = TinyEXR.LoadEXRImageFromFile(exr_image, exr_header, path, err);
        if (ret != TinyEXR.TINYEXR_SUCCESS) {
            System.out.println("EXR load error: " + err.get(0));
            return null;
        }

        PointerBuffer images = exr_image.images();
        int w = exr_image.width();
        int h = exr_image.height();
        int c = exr_image.num_channels();

        System.out.println(w + " x " + h + " x " + c);

        FloatBuffer fb[] = new FloatBuffer[c];
        for (int i = 0; i < c; ++i)
        {
            fb[i] = images.getFloatBuffer(i, w * h);
        }
        int[] bandOffsets = new int[c];
        ArrayUtil.iota(bandOffsets);

        SampleModel sampleModel = new PixelInterleavedSampleModel(DataBuffer.TYPE_FLOAT, w, h, c, w * c, bandOffsets);
        float data[] = new float[c];

        DataBuffer buffer = new DataBufferFloat(w * h * c);
        WritableRaster raster = Raster.createWritableRaster(sampleModel, buffer, null);
        for (int y = 0, pos = 0; y < h; ++y)
        {
            for (int x = 0; x < w; ++x)
            {
                for(int i = 0; i < c; ++i)
                {
                    data[i] = fb[c-i-1].get(pos) * 1/255f;
                }
                ++pos;
                raster.setPixel(x, y, data);
            }
        }
         boolean alpha = c == 4;
        ColorSpace colorSpace = ColorSpace.getInstance(c == 1 ? ColorSpace.CS_GRAY : ColorSpace.CS_sRGB);
        ColorModel colorModel = new ComponentColorModel(colorSpace, alpha, false, alpha ? Transparency.TRANSLUCENT : Transparency.OPAQUE, DataBuffer.TYPE_FLOAT);
        BufferedImage result = new BufferedImage(colorModel, raster, colorModel.isAlphaPremultiplied(), null);
        return result;
    }
}
