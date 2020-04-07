package util.data;

import util.ArrayTools;

public class UniqueObjects {
	public static final Class<?> EMPTY_CLASS_ARRAY[] = new Class[0];
	public static final String EMPTY_STRING_ARRAY[] = new String[0];
	public static final Object EMPTY_OJECT_ARRAY[] = new Object[0];
	public static final int EMPTY_INT_ARRAY[] = new int[0];
	public static final char EMPTY_CHAR_ARRAY[] = new char[0];
	public static final byte EMPTY_BYTE_ARRAY[] = new byte[0];
	public static final boolean EMPTY_BOOLEAN_ARRAY[] = new boolean[0];
	public static final float EMPTY_FLOAT_ARRAY[] = new float[0];
	public static final double EMPTY_DOUBLE_ARRAY[] = new double[0];
	public static final ArrayTools.UnmodifiableArrayList<String> EMPTY_STRING_LIST = ArrayTools.unmodifiableList(EMPTY_STRING_ARRAY);
}
