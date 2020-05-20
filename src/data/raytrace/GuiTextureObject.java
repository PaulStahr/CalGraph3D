package data.raytrace;

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import data.VideoImageSupplier;
import geometry.Matrix3d;
import geometry.Vector3d;
import ij.ImagePlus;
import maths.Controller;
import maths.VariableAmount;
import maths.exception.OperationParseException;
import util.StringUtils;

public class GuiTextureObject extends OpticalObject{

    public static final COLUMN_TYPES TYPES = new COLUMN_TYPES(new SCENE_OBJECT_COLUMN_TYPE[]{SCENE_OBJECT_COLUMN_TYPE.ID,SCENE_OBJECT_COLUMN_TYPE.ACTIVE,SCENE_OBJECT_COLUMN_TYPE.PATH,SCENE_OBJECT_COLUMN_TYPE.POSITION,SCENE_OBJECT_COLUMN_TYPE.DIRECTION,SCENE_OBJECT_COLUMN_TYPE.FRAME,SCENE_OBJECT_COLUMN_TYPE.OPEN,SCENE_OBJECT_COLUMN_TYPE.LOAD,SCENE_OBJECT_COLUMN_TYPE.SAVE,SCENE_OBJECT_COLUMN_TYPE.SAVE_TO,SCENE_OBJECT_COLUMN_TYPE.VIEW,SCENE_OBJECT_COLUMN_TYPE.DELETE}, new SCENE_OBJECT_COLUMN_TYPE[]{SCENE_OBJECT_COLUMN_TYPE.ID, SCENE_OBJECT_COLUMN_TYPE.ACTIVE, SCENE_OBJECT_COLUMN_TYPE.POSITION, SCENE_OBJECT_COLUMN_TYPE.DIRECTION, SCENE_OBJECT_COLUMN_TYPE.TRANSFORMATION, SCENE_OBJECT_COLUMN_TYPE.FRAME, SCENE_OBJECT_COLUMN_TYPE.PATH, SCENE_OBJECT_COLUMN_TYPE.OPEN, SCENE_OBJECT_COLUMN_TYPE.LOAD, SCENE_OBJECT_COLUMN_TYPE.SAVE, SCENE_OBJECT_COLUMN_TYPE.SAVE_TO, SCENE_OBJECT_COLUMN_TYPE.VIEW, SCENE_OBJECT_COLUMN_TYPE.DELETE});
    
    @Override
    public final COLUMN_TYPES getTypes()
    {
    	return TYPES;
    }
    
    public static interface TextureObjectChangeListener
	{
		public void valueChanged(GuiTextureObject object, SCENE_OBJECT_COLUMN_TYPE ct);
	}
	
	private static final Logger logger = LoggerFactory.getLogger(GuiTextureObject.class);
	private static final Object defaultValues[] = new Object[TYPES.colSize()];
	public static final GuiTextureObject[] EMPTY_TEXTTURE_ARRAY = new GuiTextureObject[0];
	static
	{
		for (int i = 0; i < defaultValues.length; ++i)
		{
			defaultValues[i] = TYPES.getCol(i).defaultValue;
		}
	}
	private File filepath;
	private String filepathString;
	public BufferedImage image;
	public WritableRaster raster;
	private final ArrayList<TextureObjectChangeListener> changeListeners = new ArrayList<>();
	public final Vector3d direction = new Vector3d();
	private String positionStr;
	private final Controller controll = new Controller();
	private String directionStr;
	private VideoImageSupplier imageObject;
	private int frameNumber;
	private String frameString = "undef";
	public final Matrix3d mat = new Matrix3d();
	private Object transformationStr;

	public GuiTextureObject(Object[] content, VariableAmount va, ParseUtil parser) {
		setValues(content, va, parser);
	}
	
	public GuiTextureObject(VariableAmount va, ParseUtil parser) {
		setValues(defaultValues, va, parser);
	}

	public GuiTextureObject(ArrayList<SCENE_OBJECT_COLUMN_TYPE> tctList, ArrayList<? extends Object> valueList, VariableAmount va, ParseUtil parser) {
		setValues(defaultValues, va, parser);
		setValues(tctList, valueList, va, parser);
	}
	
	public void getColor(double x, double y, int result[]) 
	{
		
		int xi = (int)(mat.transformAffineX(x,y) * raster.getWidth());
		int yi = (int)(mat.transformAffineY(x,y) * raster.getHeight());
		if (xi < 0 || xi >= raster.getWidth() || yi < 0 || yi >= raster.getHeight())
		{
			Arrays.fill(result, 0);
		}
		else
		{
			raster.getPixel(xi, yi, result);
		}
	}
	
	
	public void getColor(double x, double y, float result[]) 
	{
		//TODO interpolate
		int xi = (int)(mat.transformAffineX(x,y) * raster.getWidth());
		int yi = (int)(mat.transformAffineY(x,y) * raster.getHeight());
		if (xi < 0 || xi >= raster.getWidth() || yi < 0 || yi >= raster.getHeight())
		{
			Arrays.fill(result, 0);
		}
		else
		{
			raster.getPixel(xi, yi, result);
		}
	}
	
	@Override
	public void valueChanged(SCENE_OBJECT_COLUMN_TYPE ct, ParseUtil parser)
	{
		if (!isUpdating)
		{
			isUpdating = true;
			for (int i = 0; i < changeListeners.size(); ++i)
			{
				try {
					changeListeners.get(i).valueChanged(this, ct);
				}catch(Exception e)
				{
					logger.error("Exception at invoking change Listener", e);
				}
			}
			isUpdating = false;
		}
	}
	
	@Override
	public void updateValue(SCENE_OBJECT_COLUMN_TYPE ct, VariableAmount variables, ParseUtil parser) throws OperationParseException
	{
		boolean reload = false;
		switch (ct)
		{
			case DELETE:break;
			case ID:break;
			case DIRECTION:parser.parsePositionString(directionStr, this.direction, variables, controll);break;
			case POSITION:parser.parsePositionString(positionStr, this.direction, variables, controll);break;
			case FRAME:
				if (imageObject == null)
				{
					image = null;
					raster = null;
				}
				else
				{
				int tmp = parser.parseIntegerString(frameString, variables, controll);
					if (tmp != frameNumber)
					{
						frameNumber = tmp;
						try {
							image = imageObject.getFrame(frameNumber);
							raster = image.getRaster();
							modified();
							triggerModificationEvents();
						} catch (IOException e) {
							logger.error("Can't load image", e);
						}
					}
				}
				break;
			case LOAD:break;
			case OPEN:break;
			case PATH:
				filepath = new File(parser.parseString(filepathString, variables, controll));
				reload = true;
				break;
			case TRANSFORMATION:
			{
				parser.parseMat(transformationStr, mat, variables, controll);
				break;
			}
			case SAVE:break;
			case VIEW:break;
			case ACTIVE:break;
			default:break;
		}
		
		valueChanged(ct, parser);
		parser.reset();
		if (reload)
		{
			try {
				load(variables, parser);
			} catch (IOException e) {
				logger.error("Can't load image \"" + filepath + '\"', e);
			} 
		}
	}	
	
	@Override
	public void setValue(SCENE_OBJECT_COLUMN_TYPE ct, Object o, VariableAmount variables, ParseUtil parser) throws OperationParseException
	{
		boolean reload = false;
		switch (ct)
		{
			case DELETE:break;
			case ID:id = ParseUtil.parseString(o);break;
			case DIRECTION: 
				parser.parsePositionString(o, this.direction, variables, controll);
				this.directionStr = parser.str;
				break;
			case POSITION:
				parser.parsePositionString(o, this.midpoint, variables, controll);
				this.positionStr = parser.str;
				break;
			case FRAME:
				int tmp = frameNumber;
				frameNumber = parser.parseIntegerString(o, variables, controll);
				if (tmp != frameNumber && imageObject != null)
				{
					try {
						image = imageObject.getFrame(frameNumber);
						raster = image.getRaster();
						modified();
						triggerModificationEvents();
					} catch (IOException e) {
						logger.error("Can't load image", e);
					}
				}
				frameString  = parser.str;
				break;
			case LOAD:break;
			case OPEN:break;
			case PATH:
				filepath = new File(parser.parseString(o, variables, controll));
				filepathString = parser.str;
				reload = true;
				break;
			case TRANSFORMATION:
			{
				parser.parseMat(o, mat, variables, controll);
				transformationStr = parser.str;
				break;
			}
			case SAVE:break;
			case VIEW:break;
			case ACTIVE:active = ParseUtil.parseBoolean(o);break;
			default:break;
		}
		
		updateIds((byte)ct.ordinal(), parser.op);
		valueChanged(ct, parser);
		parser.reset();
		if (reload)
		{
			try {
				load(variables, parser);
			} catch (IOException e) {
				logger.error("Can't load image \"" + filepath + '\"', e);
			} 
		}
	}

	@Override
	public Object getValue(SCENE_OBJECT_COLUMN_TYPE visibleCol) {
		switch (visibleCol)
		{
			case DELETE:	return "Delete";
			case ID:		return id;
			case POSITION:	return positionStr;
			case DIRECTION:	return directionStr;
			case LOAD:		return "Load";
			case OPEN:		return "Open";
			case FRAME:		return frameString;
			case PATH:		return filepathString;
			case VIEW:		return "View";
			case SAVE:		return "Save";
			case TRANSFORMATION: return transformationStr;
			case SAVE_TO:	return SCENE_OBJECT_COLUMN_TYPE.SAVE_TO.defaultValue;
			case ACTIVE:	return active;
			default:		throw new IllegalArgumentException();
		}
	}

	public void removeChangeListener(TextureObjectChangeListener toc) {
		changeListeners.remove(toc);
	}
	
	public void addChangeListener(TextureObjectChangeListener tcl) {
		changeListeners.add(tcl);
	}


	public void load(VariableAmount variable, ParseUtil parser) throws IOException, OperationParseException {
		load(filepath == null ? null : filepath, variable, parser);
	}
	
	public void load(File file, VariableAmount variables, ParseUtil parser) throws IOException, OperationParseException{
		if (file == null)
		{
			image = null;
			raster = null;
		}
		else if (file.isFile())
		{
			String fileType = StringUtils.getFileType(file.getName());
			
			if (fileType.equals("avi") || fileType.equals("mjpeg"))
			{
				ImagePlus ip = ij.plugin.AVI_Reader.open(file.getPath(), true);
				ij.io.Opener.setOpenUsingPlugins(true);
				if (ip == null)
				{
					throw new NullPointerException();
				}
				imageObject = new VideoImageSupplier.ImagePlusVideo(ip);
			}
			else
			{
				imageObject = new VideoImageSupplier.StaticImage(ImageIO.read(file));
			}
		}
		else if (file.isDirectory())
		{
			File f[] = file.listFiles();
			Arrays.sort(f);
			ArrayList<File> al = new ArrayList<>();
			String formatNames[] = ImageIO.getReaderFormatNames();
			Arrays.sort(formatNames);
			for (int i = 0; i < f.length; ++i)
			{
				if (Arrays.binarySearch(formatNames, StringUtils.getFileType(f[i].getName())) >= 0)
				{
					al.add(f[i]);
				}
			}
			imageObject = new VideoImageSupplier.ImageFileList(al.toArray(new File[al.size()]));
		}
		else
		{
			imageObject = null;
		}
		frameNumber = -1;
		updateValue(SCENE_OBJECT_COLUMN_TYPE.FRAME, variables, parser);
		modified();
		triggerModificationEvents();		
	}

	public void save() throws IOException
	{
		//image.setData(raster);
		String path = filepath.getAbsolutePath();
		if (!ImageIO.write(image, path.substring(path.lastIndexOf('.')+1), filepath))
		{
			logger.error("Image write returned false");
		}
	}


	@Override
	public Intersection getIntersection(Vector3d position, Vector3d direction,
			Intersection intersection, double lowerBound, double upperBound) {
		return null;
	}

	public void setImage(BufferedImage image) {
		this.image = image;
		this.raster = image.getRaster();
	}

	public void saveTo(File file) throws IOException {
		String filepath = file.getPath();
		if (!ImageIO.write(image, filepath.substring(filepath.lastIndexOf('.')+1), file))
		{
			logger.error("Image write returned false");
		}
	}
	
	@Override
	public final OpticalObject copy(VariableAmount va, ParseUtil parser) {
		GuiTextureObject res = new GuiTextureObject(va, parser);
		res.read(this, va, parser);
		return res;
	}

	public File getFile() {
		return filepath;
	}
}
