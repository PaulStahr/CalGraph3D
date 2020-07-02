package data.raytrace;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import data.ObjectAttachmentContainer;
import data.raytrace.GuiOpticalSurfaceObject.ANCHOR_POINT_ENUM;
import data.raytrace.RaySimulation.AlphaCalculation;
import data.raytrace.RaySimulation.MaterialType;
import data.raytrace.RaySimulation.SurfaceType;
import geometry.Matrix4d;
import geometry.Vector3d;
import maths.Operation;
import maths.VariableAmount;
import maths.VariableStack.VariableObserver;
import maths.algorithm.OperationCalculate;
import maths.exception.OperationParseException;
import util.ArrayTools;
import util.ArrayTools.UnmodifiableArrayList;
import util.ArrayUtil;
import util.data.SortedIntegerArrayList;
import util.data.UniqueObjects;

public abstract class OpticalObject {
	public static final OpticalObject[] EMPTY_ARRAY = new OpticalObject[0];
	private static int createdObjects = 0;
	public final int iid = createdObjects++;
	public String id = "";
	public boolean active = true;
	protected boolean isUpdating = false;
	public final Vector3d midpoint = new Vector3d();
	public final ObjectAttachmentContainer attachements = new ObjectAttachmentContainer();
	public String successorArray[];
	public String predessorArray[];
	public OpticalSurfaceObject surfaceSuccessor[] = OpticalSurfaceObject.EMPTY_SURFACE_ARRAY;
	public OpticalSurfaceObject surfacePredessor[] = OpticalSurfaceObject.EMPTY_SURFACE_ARRAY;
	public OpticalVolumeObject volumeSuccessor[] = OpticalVolumeObject.EMPTY_VOLUME_ARRAY;
	public OpticalVolumeObject volumePredessor[] = OpticalVolumeObject.EMPTY_VOLUME_ARRAY;
	public MeshObject meshSuccessor[] = MeshObject.EMPTY_MESH_ARRAY;
	public MeshObject meshPredessor[] = MeshObject.EMPTY_MESH_ARRAY;
	public OpticalObject successor[] = EMPTY_ARRAY;
	public OpticalObject predessor[] = EMPTY_ARRAY;
	public abstract Intersection getIntersection(Vector3d position, Vector3d direction, Intersection intersection, double lowerBound, double upperBound);
	protected int includedVariableIds[] = UniqueObjects.EMPTY_INT_ARRAY;
	protected byte includedVariableTypes[] = UniqueObjects.EMPTY_BYTE_ARRAY;
	protected int includedVariableCount = 0;
	private final SortedIntegerArrayList al = new SortedIntegerArrayList();
	private static final Logger logger = LoggerFactory.getLogger(OpticalObject.class);
	private int oldModCount = 0;
	private int modCount = 0;
	
	private final ArrayList<DataChangeListener> dataChangeListeners = new ArrayList<>();

	public final void addDataChangeListener(DataChangeListener tcl) {
		dataChangeListeners.add(tcl);
	}
	
	public final void removeDataChangeListener(DataChangeListener tcl) {
		dataChangeListeners.remove(tcl);
	}
	
	public final void modified()
	{
		++modCount;
	}
	
	public final int modCount()
	{
		return modCount;
	}
	
	
	protected final void updateIds(byte ordinal, Operation op)
	{
		removeType(ordinal);
		if (op != null)
		{
			OperationCalculate.getVariables(op, al);
			addIds(al, ordinal);
			al.clear();
		}
	}
	
	public final void triggerModificationEvents() 
	{
		if (oldModCount  != modCount)
		{
			oldModCount = modCount;
			for (int i = 0; i < dataChangeListeners.size(); ++i)
			{
				dataChangeListeners.get(i).dataChanged(this);
			}
			//DataHandler.globalVariables.setGlobal(getId() + "_data", new ArrayOperation(data));
		}
	}
	
	public static class COLUMN_TYPES
	{
		SCENE_OBJECT_COLUMN_TYPE cols[];
	    private final String[] columnNames;
	    private final String[] visibleColumnNames;
		private final SCENE_OBJECT_COLUMN_TYPE[] visibleCols;
		public COLUMN_TYPES(SCENE_OBJECT_COLUMN_TYPE cols[], SCENE_OBJECT_COLUMN_TYPE visibleCols[])
		{
			this.cols = cols;
			columnNames = new String[cols.length];
			this.visibleCols = visibleCols;
			visibleColumnNames = new String[visibleCols.length];
			for (int i = 0; i < cols.length; ++i)
	    	{
	    		columnNames[i] = cols[i].name;
	    	}
	    	for (int i = 0; i < visibleColumnNames.length; ++i)
	    	{
	    		visibleColumnNames[i] = visibleCols[i].name;
	    	}
	    }

		public int getVisibleColumnNumber(SCENE_OBJECT_COLUMN_TYPE col) {
			return ArrayUtil.linearSearch(visibleCols, col);
		}
	    
		public int getColumnNumber(SCENE_OBJECT_COLUMN_TYPE col) {
			return ArrayUtil.linearSearch(cols, col);
		}
	    
	    public final int colSize()
	    {
	    	return cols.length;
	    }
	    
	    public final SCENE_OBJECT_COLUMN_TYPE getCol(int index)
	    {
	    	return cols[index];
	    }
	    
	    public final int visibleColsSize()
	    {
	    	return visibleCols.length;
	    }
	    
		public String[] getVisibleColumnNames() {
			return visibleColumnNames.clone();
		}
	    
	    public final SCENE_OBJECT_COLUMN_TYPE getVisibleCol(int index)
	    {
	    	return visibleCols[index];
	    } 
	}
	
	public static final byte TYPE_TEXTFIELD = 0, TYPE_CHECKBOX = 1, TYPE_COMBOBOX = 2, TYPE_BUTTON = 3, TYPE_COLOR = 4;
	
	public enum SCENE_OBJECT_COLUMN_TYPE{
		ID("Id", TYPE_TEXTFIELD, "Unnamed", null),
		ACTIVE("Visible", TYPE_CHECKBOX, true, null),
		PATH("Path", TYPE_TEXTFIELD, "", null),
		POSITION("Position", TYPE_TEXTFIELD, new Vector3d(), null),
		TRANSFORMATION("Transformation", TYPE_TEXTFIELD, new Matrix4d(1), null),
		DIRECTION("Direction", TYPE_TEXTFIELD, new Vector3d(1,0,0), null),
		LOAD("Load", TYPE_BUTTON, "Load", null),
		VIEW("View", TYPE_BUTTON, "View", null),
		OPEN("Open", TYPE_BUTTON, "Open", null),
		SAVE("Save", TYPE_BUTTON, "Save", null),
		SAVE_TO("SaveTo", TYPE_BUTTON, "SaveTo", null),
		FRAME("Frame", TYPE_TEXTFIELD, "undef", null),
		ANCHOR_POINT("AnchorPoint", TYPE_COMBOBOX, ANCHOR_POINT_ENUM.NORMAL_INTERSECTION.name, ANCHOR_POINT_ENUM.names()),
		MATERIAL("Material", TYPE_COMBOBOX, MaterialType.REFLECTION.name, MaterialType.names()),
		SURFACE("Surface", TYPE_COMBOBOX, SurfaceType.FLAT.name, SurfaceType.names()),
		MAXRADIUS("Radius", TYPE_TEXTFIELD, 10, null),
		MINRADIUS("MinimumRadius", TYPE_TEXTFIELD,0,null),
		IOR0("Ior0", TYPE_TEXTFIELD, 1, null),
		IOR1("Ior1", TYPE_TEXTFIELD, 1, null),
		DIFFUSE("Diffuse", TYPE_TEXTFIELD, 0, null),
		TRACED_RAYS("TracedRays", TYPE_TEXTFIELD, 100, null),
		UNTRACED_RAYS("UntracedRays", TYPE_TEXTFIELD, 10000, null),
		BIDIRECTIONAL("Bidirectional", TYPE_CHECKBOX, false, null),
		SMOOTH("Smooth", TYPE_CHECKBOX, false, null),
		INVERT_INOUT("InvertInsideOutside", TYPE_CHECKBOX, false, null),
		CONIC_CONSTANT("ConicConstant", TYPE_TEXTFIELD, 1, null),
		PREVIOUS_OBJECTS("PreviousObjects", TYPE_TEXTFIELD, null, null),
		FOLLOWING_OBJECTS("FollowingObjects", TYPE_TEXTFIELD, null, null),
		VOLUME_SCALING("VolumeScaling", TYPE_TEXTFIELD, 1000, null),
		MAX_STEPS("MaxSteps", TYPE_TEXTFIELD, 8000, null),
		COLOR("Color", TYPE_COLOR, new int[] {255,255,255,255}, null),
		TEXTURE_OBJECT("TextureObject", TYPE_TEXTFIELD, null, null),
		TEXTURE_MAPPING("TextureMapping", TYPE_COMBOBOX, TextureMapping.SPHERICAL.name, TextureMapping.names()),
		INVERT_NORMAL("InvertNormal", TYPE_CHECKBOX, false, null),
		ALPHA_CALCULATION("AlphaCalculation", TYPE_COMBOBOX, AlphaCalculation.MULT.name, AlphaCalculation.names()),
		INNER_POINT_TRAJECTORY_COUNT("InnerPoints", TYPE_TEXTFIELD, 10, null),
		DELETE("Delete", TYPE_BUTTON, "Delete", null),
		ALPHA_TO_RADIUS("AlphaToRadius", TYPE_CHECKBOX, false, null);
		
	    private static final SCENE_OBJECT_COLUMN_TYPE ct[] = SCENE_OBJECT_COLUMN_TYPE.values();
	    private static final String[] columnNames = new String[SCENE_OBJECT_COLUMN_TYPE.ct.length];
	    
	    public static final int size()
	    {
	    	return ct.length;
	    }
	    
	    public static final SCENE_OBJECT_COLUMN_TYPE get(int index)
	    {
	    	return ct[index];
	    }
	    
		public final String name;
		public final Class<?> cl;
		public final byte optionType;
		public final Object defaultValue;
		public final UnmodifiableArrayList<String> possibleValues;
		
		private SCENE_OBJECT_COLUMN_TYPE(String name, byte optionType, Object defaultValue, String possibleValues[]) {
			this.name = name;
			this.optionType = optionType;
			switch (optionType)
			{
				case TYPE_CHECKBOX:
					this.cl = Boolean.class;
					break;
				case TYPE_COLOR:
				case TYPE_TEXTFIELD:
				case TYPE_COMBOBOX:
				case TYPE_BUTTON:
					this.cl = String.class;
					break;
				default:
					throw new IllegalArgumentException();
			}
			this.possibleValues = possibleValues == null || possibleValues.length == 0 ? UniqueObjects.EMPTY_STRING_LIST : ArrayTools.unmodifiableList(possibleValues);
			this.defaultValue = defaultValue;
		}
		static {
			for (int i = 0; i < ct.length; ++i)
	    	{
	    		columnNames[i] = ct[i].name;
	    	}
		}
		
		public static SCENE_OBJECT_COLUMN_TYPE getByName(String name) {
			for (int i = 0; i < columnNames.length; ++i)
			{
				if (columnNames[i].equals(name))
				{
					return ct[i];
				}
			}
			return null;
		}
	};
	

	protected void addId(int id, byte type)
	{
		if (includedVariableCount == includedVariableTypes.length)
		{
			includedVariableIds = Arrays.copyOf(includedVariableIds, includedVariableIds.length * 2 + 1);
			includedVariableTypes = Arrays.copyOf(includedVariableTypes, includedVariableTypes.length * 2 + 1);
		}
		includedVariableTypes[includedVariableCount] = type;
		includedVariableIds[includedVariableCount] = id;
		++includedVariableCount;
	}
	
	protected void removeType(byte stt)
	{
		int write = 0;
		for (int read = 0; read < includedVariableCount; ++read)
		{
			if (includedVariableTypes[read] != stt)
			{
				includedVariableTypes[write] = includedVariableTypes[read];
				includedVariableIds[write] = includedVariableIds[read];
				++write;
			}
		}
		includedVariableCount = write;
	}
	
	public final void setValues(List<SCENE_OBJECT_COLUMN_TYPE> ct, List<? extends Object> o, VariableAmount va, ParseUtil parser)
	{
		isUpdating = true;
		if (ct.size() != o.size())
		{
			throw new ArrayIndexOutOfBoundsException();
		}
		for (int i = 0; i < ct.size(); ++i)
		{
			try {
				setValue(ct.get(i), o.get(i), va, parser);
			} catch (NumberFormatException | OperationParseException e) {
				logger.error("Can't read number", e);
			}catch (NullPointerException | IllegalArgumentException e){
				logger.error("Can't set property " + SCENE_OBJECT_COLUMN_TYPE.get(i) + '-' + '>' + o.get(i), e);
			}
			
		}
		isUpdating = false;
		valueChanged(null, parser);
	}
	
	public abstract void setValue(SCENE_OBJECT_COLUMN_TYPE ct, Object o, VariableAmount va, ParseUtil parser) throws OperationParseException, NumberFormatException;
	
	public abstract void valueChanged(SCENE_OBJECT_COLUMN_TYPE ct, ParseUtil parser) ;
	
	protected void addIds(SortedIntegerArrayList id, byte type)
	{
		if (includedVariableCount + id.size() > includedVariableTypes.length)
		{
			int newLength = Math.max( includedVariableIds.length * 2 + 1, includedVariableCount + id.size());
			includedVariableIds   = Arrays.copyOf(includedVariableIds,   newLength);
			includedVariableTypes = Arrays.copyOf(includedVariableTypes, newLength);
		}
		Arrays.fill(includedVariableTypes, includedVariableCount, includedVariableCount + id.size(), type);
		for (int i = 0; i < id.size(); ++i, ++includedVariableCount)
		{
			includedVariableIds[includedVariableCount] = id.getI(i);
		}
	}
	
	public final String getId()
	{
		return id;
	}

	@Override
	public String toString()
	{
		return id;
	}
	
	
	public final void setValues(Object o[], VariableAmount va, ParseUtil parser)
	{
		isUpdating = true;
		COLUMN_TYPES types = getTypes();
		if (o.length != types.colSize())
		{
			throw new ArrayIndexOutOfBoundsException();
		}
		for (int i = 0; i < types.colSize(); ++i)
		{
			try {
				setValue(types.getCol(i), o[i], va, parser);
			} catch (NumberFormatException | OperationParseException e) {
				logger.error("Can't read number", e);
			}catch (NullPointerException e){
				logger.error("Can't set property " + SCENE_OBJECT_COLUMN_TYPE.get(i) + '-' + '>' + o[i], e);
			}
		
		}
		isUpdating = false;
		valueChanged(null, parser);
	}
	
	
	public final void setValues(SCENE_OBJECT_COLUMN_TYPE ct[], Object o[], VariableAmount va, ParseUtil parser)
	{
		isUpdating = true;
		if (ct.length != o.length)
		{
			throw new ArrayIndexOutOfBoundsException();
		}
		for (int i = 0; i < ct.length; ++i)
		{
			try {
				setValue(ct[i], o[i], va, parser);
			} catch (OperationParseException e) {
				logger.error("Can't read math expression",e);
			} catch (NumberFormatException e) {
				logger.error("Can't read number", e);
			}
		}
		isUpdating = false;
		valueChanged(null, parser);
	}

	public COLUMN_TYPES getTypes() {return null;}

	public Object getValue(SCENE_OBJECT_COLUMN_TYPE ct) {return null;}
	
	public abstract void updateValue(SCENE_OBJECT_COLUMN_TYPE sct, VariableAmount variables, ParseUtil parser) throws OperationParseException;

	public final void update(VariableObserver.PendendList pendend, VariableAmount variables, ParseUtil parser) {
		for (int j = 0; j < includedVariableCount; ++j)
		{
			if (pendend.contains(includedVariableIds[j]))
			{
				SCENE_OBJECT_COLUMN_TYPE sct = SCENE_OBJECT_COLUMN_TYPE.get(includedVariableTypes[j]);
				try {
					updateValue(sct, variables, parser);
				} catch (OperationParseException e) {
					logger.error("Can't update Object Variable", e);
				}
			}
		}				
	}

	public abstract OpticalObject copy(VariableAmount va, ParseUtil parser);
	
	public void read(OpticalObject in, VariableAmount va, ParseUtil parser) {

		COLUMN_TYPES ct = in.getTypes();
		for (int i = 0; i < ct.colSize(); ++i)
		{
			SCENE_OBJECT_COLUMN_TYPE col = ct.getCol(i);
			try {
				setValue(col, in.getValue(col), va, parser);
			} catch (OperationParseException e) {
				logger.error("Can't copy attribute " + col, e);
			}
		}
	}
}
