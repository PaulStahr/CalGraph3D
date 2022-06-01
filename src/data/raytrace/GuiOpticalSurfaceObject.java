/*******************************************************************************
 * Copyright (c) 2019 Paul Stahr
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 ******************************************************************************/
package data.raytrace;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import data.raytrace.RaySimulation.MaterialType;
import data.raytrace.RaySimulation.SurfaceType;
import geometry.Vector3d;
import maths.Controller;
import maths.exception.OperationParseException;
import maths.variable.VariableAmount;

public class GuiOpticalSurfaceObject extends OpticalSurfaceObject {
	public static final COLUMN_TYPES TYPES = new COLUMN_TYPES(new SCENE_OBJECT_COLUMN_TYPE[]{
			SCENE_OBJECT_COLUMN_TYPE.ID,
			SCENE_OBJECT_COLUMN_TYPE.ACTIVE,
			SCENE_OBJECT_COLUMN_TYPE.POSITION,
    		SCENE_OBJECT_COLUMN_TYPE.DIRECTION,
    		SCENE_OBJECT_COLUMN_TYPE.ANCHOR_POINT,
    		SCENE_OBJECT_COLUMN_TYPE.MATERIAL,
    		SCENE_OBJECT_COLUMN_TYPE.SURFACE,
    		SCENE_OBJECT_COLUMN_TYPE.MAXRADIUS,
    		SCENE_OBJECT_COLUMN_TYPE.MINRADIUS,
    		SCENE_OBJECT_COLUMN_TYPE.IOR0,
    		SCENE_OBJECT_COLUMN_TYPE.IOR1,
    		SCENE_OBJECT_COLUMN_TYPE.DIFFUSE,
    		SCENE_OBJECT_COLUMN_TYPE.TRACED_RAYS,
    		SCENE_OBJECT_COLUMN_TYPE.UNTRACED_RAYS,
    		SCENE_OBJECT_COLUMN_TYPE.BIDIRECTIONAL,
    		SCENE_OBJECT_COLUMN_TYPE.INVERT_INOUT,
    		SCENE_OBJECT_COLUMN_TYPE.CONIC_CONSTANT,
    		SCENE_OBJECT_COLUMN_TYPE.PREVIOUS_OBJECTS,
    		SCENE_OBJECT_COLUMN_TYPE.FOLLOWING_OBJECTS,
    		SCENE_OBJECT_COLUMN_TYPE.COLOR,
    		SCENE_OBJECT_COLUMN_TYPE.TEXTURE_OBJECT,
    		SCENE_OBJECT_COLUMN_TYPE.TEXTURE_MAPPING,
    		SCENE_OBJECT_COLUMN_TYPE.INVERT_NORMAL,
    		SCENE_OBJECT_COLUMN_TYPE.ALPHA_TO_RADIUS,
    		SCENE_OBJECT_COLUMN_TYPE.DELETE}, new SCENE_OBJECT_COLUMN_TYPE[]{
    				SCENE_OBJECT_COLUMN_TYPE.ID,
    				SCENE_OBJECT_COLUMN_TYPE.ACTIVE,
    				SCENE_OBJECT_COLUMN_TYPE.POSITION,
    				SCENE_OBJECT_COLUMN_TYPE.DIRECTION,
    				SCENE_OBJECT_COLUMN_TYPE.MATERIAL,
    				SCENE_OBJECT_COLUMN_TYPE.SURFACE,
    				SCENE_OBJECT_COLUMN_TYPE.MAXRADIUS,
    				SCENE_OBJECT_COLUMN_TYPE.IOR0,
    				SCENE_OBJECT_COLUMN_TYPE.IOR1,
    				SCENE_OBJECT_COLUMN_TYPE.DIFFUSE,
    				SCENE_OBJECT_COLUMN_TYPE.DELETE});

	@Override
	public final COLUMN_TYPES getTypes()
	{
		return TYPES;
	}

	public enum ANCHOR_POINT_ENUM{
		NORMAL_INTERSECTION("Normal Intersection"), LENSE_CURVE("Lense Curve"), MIRROR_FOCAL("Mirror Focal");
		public final String name;
		private static final ANCHOR_POINT_ENUM ap[] = values();

		private ANCHOR_POINT_ENUM(String name)
		{
			this.name = name;
		}

		public static String[] names (){
			 String res[] = new String[ap.length];
			 for (int i = 0; i < res.length; ++i)
			 {
				 res[i] = ap[i].name;
			 }
			 return res;
		 }

		public static ANCHOR_POINT_ENUM getByName(String name) {
			for (ANCHOR_POINT_ENUM current : ap)
			{
				if (name.equals(current.name))
				{
					return current;
				}
			}
			return ANCHOR_POINT_ENUM.valueOf(name);
		}

		public static ANCHOR_POINT_ENUM get(Object o)
		{
			if (o instanceof String)
			{
				ANCHOR_POINT_ENUM tmp = ANCHOR_POINT_ENUM.getByName((String)o);
				if (tmp != null)
				{
					return tmp;
				}
			}
			else if (o instanceof ANCHOR_POINT_ENUM)
			{
				return (ANCHOR_POINT_ENUM)o;
			}
			throw new IllegalArgumentException("Class:" + o.getClass());
		}


		@Override
		public String toString()
		{
			return name;
		}
	}

	public static final GuiOpticalSurfaceObject EMPTY_SURFACE_ARRAY[] = new GuiOpticalSurfaceObject[0];

	private static final Controller controll = new Controller();
	private static final Logger logger = LoggerFactory.getLogger(GuiOpticalSurfaceObject.class);

	private static final Object defaultValues[] = new Object[TYPES.colSize()];
	static
	{
		for (int i = 0; i < defaultValues.length; ++i)
		{
			defaultValues[i] = TYPES.getCol(i).defaultValue;
		}
	}
	String positionStr;
	String directionStr;
	String ior0Str;
	String ior1Str;
	String maxRadiusStr;
	String minRadiusStr;
	String conicConstantStr;
	String diffuseStr;
	private String successorStr;
	private String predessorStr;
	private String endObjectStr;
	int modCount = 0;
	private final ArrayList<OpticalSurfaceObjectChangeListener> changeListeners = new ArrayList<>();
	public final Vector3d position = new Vector3d();
	public static interface OpticalSurfaceObjectChangeListener
	{
		public void valueChanged(GuiOpticalSurfaceObject object, SCENE_OBJECT_COLUMN_TYPE ct);
	}

	public GuiOpticalSurfaceObject(VariableAmount va, ParseUtil parser) {

		setValues(defaultValues, va, parser);
	}

	public GuiOpticalSurfaceObject(Object content[], VariableAmount va, ParseUtil parser)
	{
		setValues(content, va, parser);
	}

	public GuiOpticalSurfaceObject(SCENE_OBJECT_COLUMN_TYPE coltype[], Object content[], VariableAmount va, ParseUtil parser)
	{
		setValues(defaultValues, va, parser);
		setValues(coltype, content, va, parser);
	}


	public GuiOpticalSurfaceObject(List<SCENE_OBJECT_COLUMN_TYPE> coltype, List<? extends Object> content, VariableAmount va, ParseUtil parser)
	{
		setValues(defaultValues, va, parser);
		setValues(coltype, content, va, parser);
	}

	@Override
	public void valueChanged(SCENE_OBJECT_COLUMN_TYPE ct, ParseUtil parser)
	{
		++modCount;
		if (!isUpdating)
		{
			isUpdating = true;
			update();
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

	public void addChangeListener(OpticalSurfaceObjectChangeListener r)
	{
		changeListeners.add(r);
	}

	public void removeChangeListener(OpticalSurfaceObjectChangeListener r)
	{
		changeListeners.remove(r);
	}
	private ANCHOR_POINT_ENUM anchorPoint = ANCHOR_POINT_ENUM.NORMAL_INTERSECTION;
	private String colorStr;

	@Override
	public void updateValue(SCENE_OBJECT_COLUMN_TYPE ct, VariableAmount variables, ParseUtil parser) throws OperationParseException
	{
		switch (ct)
		{
			case ACTIVE:break;
			case DELETE:break;
			case DIFFUSE:diffuse = parser.parseDoubleString(diffuseStr, variables, controll);break;
			case DIRECTION:
			{
			    parser.parsePositionString(directionStr, direction, variables, controll);
                updateMidpoint();
                break;
			}
			case ID:break;
			case IOR0:ior0 = parser.parseOperationString(ior0Str, variables, controll);break;
			case IOR1:ior1 = parser.parseOperationString(ior1Str, variables, controll);break;
			case MATERIAL:break;
			case ANCHOR_POINT:break;
			case POSITION:
			{
				parser.parsePositionString(positionStr, position, variables, controll);
				updateMidpoint();
				break;
			}
			case MAXRADIUS:maxRadiusGeometric = parser.parseDoubleString(maxRadiusStr, variables, controll);break;
			case MINRADIUS:minRadiusGeometric = parser.parseDoubleString(minRadiusStr, variables, controll);break;
			case SURFACE:break;
			case TRACED_RAYS:break;
			case UNTRACED_RAYS:break;
			case BIDIRECTIONAL:break;
			case INVERT_INOUT:break;
			case ALPHA_TO_RADIUS: break;
			case CONIC_CONSTANT:conicConstant = parser.parseDoubleString(conicConstantStr, variables, controll);break;
			case FOLLOWING_OBJECTS:	successorArray = parser.parseStringArray(successorStr, controll);break;
			case PREVIOUS_OBJECTS:predessorArray= parser.parseStringArray(predessorStr, controll);break;
			case END_OBJECTS: endObjectArray = parser.parseStringArray(endObjectStr, controll);break;
			case COLOR:color = parser.parseColor(colorStr, variables, controll);break;
			case TEXTURE_OBJECT:ParseUtil.parseString(textureObjectStr);break;
			case TEXTURE_MAPPING:break;
			case INVERT_NORMAL:break;
			default:
				break;
		}
		if (ct.vname != null)
		{
		    v.set(ct.vname, parser.op);
		}
		valueChanged(ct, parser);
		parser.reset();
	}

	@Override
	public void setValue(SCENE_OBJECT_COLUMN_TYPE ct, Object o, VariableAmount variables, ParseUtil parser) throws OperationParseException
	{
		switch (ct)
		{
			case ACTIVE:active = ParseUtil.parseBoolean(o);break;
			case DELETE:break;
			case DIFFUSE:
				diffuse = parser.parseDoubleString(o, variables, controll);
				diffuseStr = parser.str;
				break;
			case DIRECTION:
				parser.parsePositionString(o, direction, variables, controll);
				directionStr = parser.str;
				updateMidpoint();
				break;
			case ID:
				setId(ParseUtil.parseString(o));
				break;
			case IOR0:
				ior0 = parser.parseOperationString(o, variables, controll);
				ior0Str = parser.str;
				break;
			case IOR1:
				ior1 = parser.parseOperationString(o, variables, controll);
				ior1Str = parser.str;
				break;
			case MATERIAL:materialType = MaterialType.get(o);break;
			case ANCHOR_POINT:
			{
				anchorPoint = ANCHOR_POINT_ENUM.get(o);
				updateMidpoint();
				break;
			}
			case POSITION:
				parser.parsePositionString(o, position, variables, controll);
				updateMidpoint();
				positionStr = parser.str;
				break;
			case MAXRADIUS:
				maxRadiusGeometric = parser.parseDoubleString(o, variables, controll);
				maxRadiusStr = parser.str;
				break;
			case MINRADIUS:
				minRadiusGeometric = parser.parseDoubleString(o, variables, controll);
				minRadiusStr = parser.str;
				break;
			case SURFACE:
				if (o instanceof String)
				{
					SurfaceType tmp = SurfaceType.getByName((String)o);
					if (tmp != null)
					{
						surf = tmp;
					}
				}
				else if (o instanceof SurfaceType)
				{
					surf = (SurfaceType)o;
				}
				else
				{
					throw new IllegalArgumentException("Class:" + o.getClass());
				}
				break;
			case TRACED_RAYS:numTracedRays = ParseUtil.parseInteger(o);break;
			case UNTRACED_RAYS:numUntracedRays = ParseUtil.parseInteger(o);break;
			case BIDIRECTIONAL:bidirectional = ParseUtil.parseBoolean(o);break;
			case INVERT_INOUT:invertInsideOutside = ParseUtil.parseBoolean(o);break;
			case CONIC_CONSTANT:
				conicConstant = parser.parseDoubleString(o, variables, controll);
				conicConstantStr = parser.str;
				break;
			case FOLLOWING_OBJECTS:
				successorArray = parser.parseStringArray(o, controll);
				successorStr = parser.str;break;
			case PREVIOUS_OBJECTS:
				predessorArray= parser.parseStringArray(o, controll);
				predessorStr = parser.str;break;
			case COLOR:
				color = parser.parseColor(o, variables, controll);
				colorStr = parser.str;
				break;
			case ALPHA_TO_RADIUS:
				alphaAsRadius = ParseUtil.parseBoolean(o);
				break;
			case TEXTURE_OBJECT:textureObjectStr = ParseUtil.parseString(o);break;
			case TEXTURE_MAPPING:
				if (o instanceof String)
				{
					TextureMapping tmp = TextureMapping.getByName((String)o);
					if (tmp != null)
					{
						textureMapping = tmp;
					}
				}
				else if (o instanceof TextureMapping)
				{
					textureMapping = (TextureMapping)o;
				}
				else
				{
					throw new IllegalArgumentException("Class:" + o.getClass());
				}
				break;
			case INVERT_NORMAL:invertNormal = ParseUtil.parseBoolean(o);break;
			default:
				break;

			}
		updateIds((byte)ct.ordinal(), parser.op);
        if (ct.vname != null)
        {
            v.set(ct.vname, parser.op);
        }
		valueChanged(ct, parser);
		parser.reset();

	}

	public final void updateMidpoint() {
		switch(anchorPoint) {
			case LENSE_CURVE:midpoint.setDiff(position, direction);break;
			case MIRROR_FOCAL:midpoint.set(position, direction, 0.5);break;
			case NORMAL_INTERSECTION:midpoint.set(position);break;
			default:break;
		}
	}

	@Override
	public Object getValue(SCENE_OBJECT_COLUMN_TYPE ct)
	{
		switch (ct)
		{
		case ACTIVE:				return active;
		case DELETE:				break;
		case DIFFUSE:				return diffuseStr;
		case DIRECTION:				return directionStr;
		case ID:					return getId();
		case IOR0:					return ior0Str;
		case IOR1:					return ior1Str;
		case MATERIAL:				return materialType;
		case POSITION:				return positionStr;
		case MAXRADIUS:				return maxRadiusStr;
		case MINRADIUS:				return minRadiusStr;
		case SURFACE:				return surf;
		case TRACED_RAYS:			return numTracedRays;
		case UNTRACED_RAYS:			return numUntracedRays;
		case BIDIRECTIONAL:			return bidirectional;
		case INVERT_INOUT:			return invertInsideOutside;
		case CONIC_CONSTANT:		return conicConstantStr;
		case ANCHOR_POINT:			return anchorPoint;
		case PREVIOUS_OBJECTS:		return predessorStr;
		case FOLLOWING_OBJECTS:		return successorStr;
		case COLOR:					return colorStr;
		case ALPHA_TO_RADIUS:		return alphaAsRadius;
		case TEXTURE_OBJECT:		return textureObjectStr;
		case TEXTURE_MAPPING:		return textureMapping;
		case INVERT_NORMAL:			return invertNormal;
		default:					throw new IllegalArgumentException();
		}
		return null;
	}

	@Override
	public final OpticalObject copy(VariableAmount va, ParseUtil parser) {
		GuiOpticalSurfaceObject res = new GuiOpticalSurfaceObject(va, parser);
		for (int i = 0; i < TYPES.colSize(); ++i)
		{
			SCENE_OBJECT_COLUMN_TYPE col = TYPES.getCol(i);
			try {
				res.setValue(col, getValue(col), va, parser);
			} catch (OperationParseException e) {
				logger.error("Can't copy attribute " + col, e);
			}
		}
		return res;
	}
}