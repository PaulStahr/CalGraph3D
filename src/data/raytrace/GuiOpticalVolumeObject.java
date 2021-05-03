package data.raytrace;

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.JMenu;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import data.DataHandler;
import geometry.Matrixd;
import jcomponents.raytrace.TextureView;
import jcomponents.raytrace.Volume;
import maths.Controller;
import maths.data.ArrayOperation;
import maths.exception.OperationParseException;
import maths.variable.VariableAmount;
import util.ArrayUtil;

public class GuiOpticalVolumeObject extends OpticalVolumeObject {
    public static final GuiOpticalVolumeObject EMPTY_VOLUME_ARRAY[] = new GuiOpticalVolumeObject[0];
	private final StringBuilder strB = new StringBuilder();
	public String positionStr;
	public String transformationStr;
	private static final Controller controll = new Controller(); 
	private final ArrayList<OpticalVolumeObjectChangeListener> changeListeners = new ArrayList<>();
	private static final Logger logger = LoggerFactory.getLogger(GuiOpticalVolumeObject.class);
	private String colorStr;
	private String successorStr;
	private String predessorStr;
	public static final COLUMN_TYPES TYPES = new COLUMN_TYPES(
			new SCENE_OBJECT_COLUMN_TYPE[]{
					SCENE_OBJECT_COLUMN_TYPE.ID,
					SCENE_OBJECT_COLUMN_TYPE.ACTIVE,
					SCENE_OBJECT_COLUMN_TYPE.LOAD,
					SCENE_OBJECT_COLUMN_TYPE.VIEW,
					SCENE_OBJECT_COLUMN_TYPE.POSITION,
					SCENE_OBJECT_COLUMN_TYPE.TRANSFORMATION,
					SCENE_OBJECT_COLUMN_TYPE.COLOR,
					SCENE_OBJECT_COLUMN_TYPE.PREVIOUS_OBJECTS,
					SCENE_OBJECT_COLUMN_TYPE.FOLLOWING_OBJECTS,
					SCENE_OBJECT_COLUMN_TYPE.VOLUME_SCALING,
					SCENE_OBJECT_COLUMN_TYPE.MAX_STEPS,
					SCENE_OBJECT_COLUMN_TYPE.INNER_POINT_TRAJECTORY_COUNT,
					SCENE_OBJECT_COLUMN_TYPE.DELETE},
			new SCENE_OBJECT_COLUMN_TYPE[]{
					SCENE_OBJECT_COLUMN_TYPE.ID,
					SCENE_OBJECT_COLUMN_TYPE.ACTIVE,
					SCENE_OBJECT_COLUMN_TYPE.LOAD,
					SCENE_OBJECT_COLUMN_TYPE.VIEW,
					SCENE_OBJECT_COLUMN_TYPE.POSITION,
					SCENE_OBJECT_COLUMN_TYPE.TRANSFORMATION,
					SCENE_OBJECT_COLUMN_TYPE.COLOR,
					SCENE_OBJECT_COLUMN_TYPE.DELETE});

	@Override
	public COLUMN_TYPES getTypes()
	{
		return TYPES;
	}
	
	public static interface OpticalVolumeObjectChangeListener
	{
		public void valueChanged(GuiOpticalVolumeObject object, SCENE_OBJECT_COLUMN_TYPE ct);
	}
	
	public GuiOpticalVolumeObject(Object[] volumeRow, VariableAmount va, ParseUtil parser) {
		setValues(volumeRow, va, parser);
	}
	
	public GuiOpticalVolumeObject(VariableAmount va, ParseUtil parser) {setValues(defaultValues, va, parser);}

	private static final Object defaultValues[] = new Object[TYPES.colSize()];
	static
	{
		for (int i = 0; i < defaultValues.length; ++i)
		{
			defaultValues[i] = TYPES.getCol(i).defaultValue;
		}
	}
	public GuiOpticalVolumeObject(ArrayList<SCENE_OBJECT_COLUMN_TYPE> vctList, ArrayList<? extends Object> valueList, VariableAmount va, ParseUtil parser) {
		super();
		setValues(vctList, valueList, va, parser);
	}

	public void addChangeListener(OpticalVolumeObjectChangeListener r)     {changeListeners.add(r);}
	public void removeChangeListener(OpticalVolumeObjectChangeListener r)  {changeListeners.remove(r);}
	
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
	public void readBinaryFile(String file) throws IOException
	{
		super.readBinaryFile(file);
		strB.setLength(0);
		transformationStr = new ArrayOperation((Matrixd)unitVolumeToGlobal).toString(strB).toString();
		strB.setLength(0);
		positionStr = new ArrayOperation(midpoint).toString(strB).toString();
		valueChanged(SCENE_OBJECT_COLUMN_TYPE.TRANSFORMATION, null);
		valueChanged(SCENE_OBJECT_COLUMN_TYPE.POSITION, null);
		modified();
	}

	@Override
	public void readDycom(String file)
	{
		super.readDycom(file);
		strB.setLength(0);
		transformationStr = new ArrayOperation((Matrixd)unitVolumeToGlobal).toString(strB).toString();
		strB.setLength(0);
		positionStr = new ArrayOperation(midpoint).toString(strB).toString();
		valueChanged(SCENE_OBJECT_COLUMN_TYPE.TRANSFORMATION, null);
		valueChanged(SCENE_OBJECT_COLUMN_TYPE.POSITION, null);
		modified();
	}

	@Override
	public void updateValue(SCENE_OBJECT_COLUMN_TYPE ct, VariableAmount variables, ParseUtil parser) throws OperationParseException
	{
		try {
			parser.op = null;
			switch (ct)
			{
			case ACTIVE:break;
			case DELETE:break;
			case LOAD:break;
			case ID:break;
			case POSITION:
			{
				parser.parsePositionString(positionStr, midpoint, variables, controll);
				DataHandler.globalVariables.setGlobal(id.concat("_pos"), parser.op);
	    		unitVolumeToGlobal.setCol(3,midpoint);
	    		applyMatrix();
				break;
			}
			case MAX_STEPS:break;
			case VOLUME_SCALING:break;
			case COLOR:
			{
				color = parser.parseColor(colorStr, variables, controll);
				break;
			}
			case TRANSFORMATION:
			{
				parser.parseMat(transformationStr, unitVolumeToGlobal, variables, controll);
				applyMatrix();
				break;
			}
			case PREVIOUS_OBJECTS:break;
			case FOLLOWING_OBJECTS:break;
			case INNER_POINT_TRAJECTORY_COUNT:break;
			default:break;
			}
			updateIds((byte)ct.ordinal(), parser.op);
			valueChanged(ct, null);
		} catch (OperationParseException e) {
			logger.error("Can't read math expression",e);
		} catch (NumberFormatException e) {
			logger.error("Can't read number");
		}
		parser.reset();
	}

	private class VolumeView extends TextureView implements ChangeListener
	{
		private static final long serialVersionUID = -6532309028252938768L;
		JSlider sliderFrame = new JSlider();
		public VolumeView()
		{
			super(new BufferedImage(vol.width, vol.height, BufferedImage.TYPE_INT_ARGB));
			JMenu menu = new JMenu("Volume");
			addMenu(menu);
			menu.add(sliderFrame);
			sliderFrame.setMinimum(0);
			sliderFrame.setMaximum(vol.depth - 1);
			sliderFrame.addChangeListener(this);
		}

		private void updateGraphic()
		{
			int width = vol.width, height = vol.height, depth = vol.depth;
			float data[] = vol.data;
			float minMax[] = ArrayUtil.minMax(data);
			int pixel[] = new int[4];
			BufferedImage bi = getImage();
			if (bi.getWidth() != width || bi.getHeight() != height)
			{
				bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
				setImage(bi);
			}
			WritableRaster raster = bi.getRaster();
			int layer = sliderFrame.getValue();
			pixel[3] = 255;
			if (layer < 0 || layer >= depth) {throw new ArrayIndexOutOfBoundsException(layer);}
			float range = minMax[1] - minMax[0];
			if (range == 0)
			{
				bi.getGraphics().clearRect(0, 0, width, height);
			}
			else
			{
				for (int y = 0; y < height; ++y)
				{
					for (int x = 0; x < width; ++x)
					{
						Arrays.fill(pixel, 0, 3, (int)((data[x + width * (y + height * layer)] - minMax[0]) * 255 / range));
						raster.setPixel(x, y, pixel);
					}
				}
			}
			repaint();
		}

		@Override
		public void stateChanged(ChangeEvent arg0) {
			updateGraphic();
		}
	}

	public void view()
	{
		/*if (dcm != null && false)
		{
			dcm.show();
		}
		else
		{*/
			VolumeView vv = new VolumeView();
			vv.setVisible(true);
		//}
	}

	@Override
	public void setValue(SCENE_OBJECT_COLUMN_TYPE ct, Object o, VariableAmount variables, ParseUtil parser) throws OperationParseException
	{
		try {
			switch (ct)
			{
			case ACTIVE:active = ParseUtil.parseBoolean(o);break;
			case DELETE:break;
			case LOAD:break;
			case ID:id = ParseUtil.parseString(o);break;
			case POSITION:
			{
				parser.parsePositionString(o, midpoint, variables, controll);
				positionStr = parser.str;
				DataHandler.globalVariables.setGlobal(id.concat("_pos"), parser.op);
	    		unitVolumeToGlobal.setCol(3,midpoint);
	    		applyMatrix();
				break;
			}
			case COLOR:
			{
				color = parser.parseColor(o, variables, controll);
				colorStr = parser.str;
				break;
			}
			case TRANSFORMATION:
			{
				parser.parseMat(o, unitVolumeToGlobal, variables, controll);
				transformationStr = parser.str;
				applyMatrix();
				break;
			}
			case FOLLOWING_OBJECTS:
				successorArray = parser.parseStringArray(o, controll);
				successorStr = parser.str;break;
			case PREVIOUS_OBJECTS:
				predessorArray= parser.parseStringArray(o, controll);
				predessorStr = parser.str;break;
			case MAX_STEPS:maxSteps = ParseUtil.parseInteger(o);break;
			case INNER_POINT_TRAJECTORY_COUNT: numInnerTrajectoryPoints = ParseUtil.parseInteger(o);break;
			case VOLUME_SCALING: volumeScaling = ParseUtil.parseDouble(o);applyMatrix();break;
			default:logger.warn("Unknown Option " + ct);break;
			}
			updateIds((byte)ct.ordinal(), parser.op);
			valueChanged(ct, null);
		} catch (OperationParseException e) {
			logger.error("Can't read math expression",e);
		} catch (NumberFormatException e) {
			logger.error("Can't read number");
		}
		parser.reset();
	}

	@Override
	public Object getValue(SCENE_OBJECT_COLUMN_TYPE ct)
	{
		switch (ct)
		{
		case ACTIVE:		return active;
		case DELETE:		break;
		case LOAD:			break;
		case ID:			return id;
		case POSITION:		return positionStr;
		case TRANSFORMATION:return transformationStr;
		case VIEW:			break;
		case COLOR:			return colorStr;
		case FOLLOWING_OBJECTS: return successorStr;
		case PREVIOUS_OBJECTS: return predessorStr;
		case MAX_STEPS:		return maxSteps;
		case INNER_POINT_TRAJECTORY_COUNT: return numInnerTrajectoryPoints;
		case VOLUME_SCALING:return volumeScaling;
		default:			throw new IllegalArgumentException(ct.name);		
		}
		return null;
	}

	@Override
	public final void read(OpticalObject obj, VariableAmount va, ParseUtil parser)
	{
		super.read(obj, va, parser);
		if (obj instanceof GuiOpticalVolumeObject)
		{
			GuiOpticalVolumeObject ovo = (GuiOpticalVolumeObject) obj;
			this.vol = new Volume(ovo.vol);
			this.dcm = ovo.dcm;
			this.vs = ovo.vs;
			this.ip = ovo.ip;
		}
	}

	@Override
	public final GuiOpticalVolumeObject copy(VariableAmount va, ParseUtil parser) {
		GuiOpticalVolumeObject res = new GuiOpticalVolumeObject(va, parser);
		res.read(this, va, parser);
		return res;
	}
}
