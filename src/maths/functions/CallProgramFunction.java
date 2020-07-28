package maths.functions;

import java.awt.Color;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import geometry.Matrixd;
import geometry.Vector2d;
import geometry.Vector3d;
import geometry.Vector4d;
import maths.Operation;
import maths.variable.VariableAmount;
import maths.algorithm.OperationCalculate;
import maths.data.ArrayOperation;
import maths.data.BooleanOperation;
import maths.data.CharacterOperation;
import maths.data.RealDoubleOperation;
import maths.data.RealLongOperation;
import maths.data.StringOperation;
import maths.exception.ExceptionOperation;
import util.ArrayTools;
import util.StringUtils;
import util.data.DoubleList;
import util.data.IntegerList;
import util.data.UniqueObjects;

public class CallProgramFunction extends FunctionOperation{
	private Operation a;
	private Operation c;
	private Operation d;
	private Operation b;
	private final boolean nameChecked;
	private final Object obj;
	private final Class<?> cl;
	private final Method m[];
	
	public CallProgramFunction(Operation a, Operation b, Operation c, Operation d){
    	if ((this.a = a) == null || ((this.b = b) == null) || (this.c = c) == null || (this.d = d) == null)
    		throw new NullPointerException();
    	this.nameChecked = false;
    	this.m = null;
    	this.obj = null;
    	this.cl = null;
	}
	
	public CallProgramFunction(Operation a, Operation b, Operation c, Operation d, boolean nameChecked, Class<?> cl, Object obj, Method m[]){
    	if ((this.a = a) == null || ((this.b = b) == null) || (this.c = c) == null || (this.d = d) == null)
    		throw new NullPointerException();
    	this.nameChecked = nameChecked;
    	this.cl = cl;
    	this.obj = obj;
    	this.m = m;
	}
	
	@Override
	public String getFunctionName() {
		return "java";
	}

	@Override
	public Operation getInstance(List<Operation> subclasses) {
		return new CallProgramFunction(subclasses.get(0), subclasses.get(1), subclasses.get(2), subclasses.get(3)); 
	}
	
	public static final Operation calculate(Operation a, Operation b, Operation c, Operation d, boolean nameChecked, Class<?> cl, Object obj, Method m[])
	{
		if (a.isString() && cl == null && m == null)
		{
			try {
				cl = Class.forName(a.stringValue());
			} catch (ClassNotFoundException e) {
				return new ExceptionOperation("Class \"" + a.stringValue()+ "\" not found");
			}
		}
		if (b.isString() && cl != null && m == null)
		{
			if (b.stringValue().equals(""))
			{	
				m = cl.getMethods();
			}
			else
			{
				ArrayList<String> al = new ArrayList<>();
				StringUtils.split(b.stringValue(), '.', al);
				for (int j = 0; j < al.size(); ++j)
				{
					Field f;
					try {
						f = cl.getField(al.get(j));
					} catch (NoSuchFieldException | SecurityException e) {
						return new ExceptionOperation(e.toString());
					}
					try {
						obj = f.get(obj);
					} catch (IllegalArgumentException | IllegalAccessException e) {
						return new ExceptionOperation(e.toString());
					}
					cl=obj.getClass();
				}
				m = cl.getMethods();
			}
		}
		if (c.isString() && m != null && !nameChecked)
		{
			String methodName = c.stringValue();
			Method selected[] = new Method[1];
			int count = 0;
			for (int i = 0; i < m.length; ++i)
			{
				if (methodName.equals(m[i].getName()))
				{
					selected = ArrayTools.add(selected, count++, m[i]);
				}
			}
			if (selected.length != count)
			{
				selected = Arrays.copyOf(selected, count);
			}
			m = selected;
			nameChecked = true;
		}
		if (m == null || !nameChecked || !d.isPrimitive())
		{
			return new CallProgramFunction(a, b, c, d, nameChecked, cl, obj, m);
		}
		
		Object arguments[] = UniqueObjects.EMPTY_OJECT_ARRAY;
		if (d.size() != 0)
		{
			arguments = new Object[d.size()];
		}
		for (Method method: m)
		{
			checkMethod:
			{
				Type[] types = method.getGenericParameterTypes();
				if (types.length != d.size())
				{
					break checkMethod;
				}
				for (int i = 0; i < types.length; ++i)
				{
					Type type = types[i];
					Operation sub = d.get(i);
					if (type == int.class || type == Integer.class)
					{
						if (!sub.isIntegral())
						{
							break checkMethod;
						}
						arguments[i] = (int)sub.longValue();
					}
					else if (type == long.class || type == Long.class)
					{
						if (!sub.isIntegral())
						{
							break checkMethod;
						}
						arguments[i] = sub.longValue();						
					}
					else if (type == float.class || type == Float.class)
					{
						if (!sub.isRealFloatingNumber())
						{
							break checkMethod;
						}
						arguments[i] = (float)sub.doubleValue();						
					}
					else if (types[i] == double.class || types[i] == Double.class)
					{
						if (!sub.isRealFloatingNumber())
						{
							break checkMethod;
						}
						arguments[i] = sub.doubleValue();						
					}
					else if (type == boolean.class || type == Boolean.class)
					{
						if (!sub.isBoolean())
						{
							break checkMethod;
						}
						arguments[i] = sub.booleanValue();						
					}	
					else if (type == String.class)
					{
						if (!sub.isString())
						{
							break checkMethod;
						}
						arguments[i] = sub.stringValue();						
					}
					else if (type == Vector2d.class)
					{
						if (!(sub.isArray() && sub.isPrimitive() && sub.size() == 2))
						{
							break checkMethod;
						}
						arguments[i] = new Vector2d(sub.get(0).doubleValue(), sub.get(1).doubleValue());
					}
					else if (type == Vector3d.class)
					{
						if (!(sub.isArray() && sub.isPrimitive() && sub.size() == 3))
						{
							break checkMethod;
						}
						arguments[i] = new Vector3d(sub.get(0).doubleValue(),sub.get(1).doubleValue(),sub.get(2).doubleValue());
					}
					else if (type == Vector4d.class)
					{
						if (!(sub.isArray() && sub.isPrimitive() && sub.size() == 4))
						{
							break checkMethod;
						}
						arguments[i] = new Vector4d(sub.get(0).doubleValue(),sub.get(1).doubleValue(),sub.get(2).doubleValue(), sub.get(3).doubleValue());
					}
					else if (type == boolean[].class)
					{
						if (!(sub.isArray() && sub.isPrimitive()))
						{
							break checkMethod;
						}
						boolean data[] = new boolean[sub.size()];
						for (int j = 0; j < data.length; ++j)
						{
							data[j] = sub.get(j).booleanValue();
						}
						arguments[i] = data;
					}
					else if (type == int[].class)
					{
						if (!(sub.isArray() && sub.isPrimitive()))
						{
							break checkMethod;
						}
						int data[] = new int[sub.size()];
						for (int j = 0; j < data.length; ++j)
						{
							data[j] = (int)sub.get(j).longValue();
						}
						arguments[i] = data;
					}
					else if (type == byte[].class)
					{
						if (!(sub.isArray() && sub.isPrimitive()))
						{
							break checkMethod;
						}
						byte data[] = new byte[sub.size()];
						for (int j = 0; j < data.length; ++j)
						{
							data[j] = (byte)sub.get(j).longValue();
						}
						arguments[i] = data;
					}
					else if (type == double[].class)
					{
						if (!(sub.isArray() && sub.isPrimitive()))
						{
							break checkMethod;
						}
						double data[] = new double[sub.size()];
						for (int j = 0; j < data.length; ++j)
						{
							data[j] = sub.get(j).doubleValue();
						}
						arguments[i] = data;
					}
					else if (type == float[].class)
					{
						if (!(sub.isArray() && sub.isPrimitive()))
						{
							break checkMethod;
						}
						float data[] = new float[sub.size()];
						for (int j = 0; j < data.length; ++j)
						{
							data[j] = (float)sub.get(j).doubleValue();
						}
						arguments[i] = data;
					}
					else if (type == long[].class)
					{
						if (!(sub.isArray() && sub.isPrimitive()))
						{
							break checkMethod;
						}
						long data[] = new long[sub.size()];
						for (int j = 0; j < data.length; ++j)
						{
							data[j] = sub.get(j).longValue();
						}
						arguments[i] = data;
					}
					else if (type == String[].class)
					{
						if (!(sub.isArray() && sub.isPrimitive()))
						{
							break checkMethod;
						}
						String data[] = new String[sub.size()];
						for (int j = 0; j < data.length; ++j)
						{
							data[j] = sub.get(j).stringValue();
						}
						arguments[i] = OperationCalculate.toStringArray(sub);
					}
				}
				Object ret;
				try {
					method.setAccessible(true);
					ret = method.invoke(obj, arguments);
				} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
					return new ExceptionOperation(e.toString());
				}
				if (ret instanceof Integer)
				{
					return new RealLongOperation((int)ret);
				}
				if (ret instanceof Long)
				{
					return new RealLongOperation((long)ret);
				}
				if (ret instanceof Double)
				{
					return new RealDoubleOperation((double)ret);
				}
				if (ret instanceof Float)
				{
					return new RealDoubleOperation((float)ret);
				}				
				if (ret instanceof String)
				{
					return new StringOperation((String)ret);
				}
				if (ret instanceof Boolean)
				{
					return BooleanOperation.get((boolean)ret);
				}
				if (ret instanceof Character)
				{
					return CharacterOperation.getInstance((char)ret);
				}
				if (ret instanceof Matrixd)
				{
					return new ArrayOperation((Matrixd)ret);
				}
				if (ret instanceof DoubleList)
				{
					return new ArrayOperation((DoubleList)ret);
				}
				if (ret instanceof IntegerList)
				{
					return new ArrayOperation((IntegerList)ret);
				}
				if (ret instanceof int[])
				{
					return new ArrayOperation((int[])ret);
				}
				if (ret instanceof float[])
				{
					return new ArrayOperation((float[])ret);
				}
				if (ret instanceof boolean[])
				{
					return new ArrayOperation((boolean[])ret);
				}
				if (ret instanceof char[])
				{
					return new ArrayOperation((char[])ret);
				}
				if (ret instanceof long[])
				{
					return new ArrayOperation((long[])ret);
				}
				if (ret instanceof String[])
				{
					return new ArrayOperation((String[])ret);
				}
				if (ret instanceof byte[])
				{
					return new ArrayOperation((byte[])ret);
				}
				if (ret instanceof Matrixd)
				{
					return new ArrayOperation((Matrixd)ret);
				}
				if (ret instanceof double[])
				{
					return new ArrayOperation((double[])ret);
				}
				if (ret instanceof Color)
				{
					return new RealLongOperation(((Color)ret).getRGB());
				}
				if (ret instanceof Operation)
				{
					return (Operation)ret;
				}
				return new StringOperation(String.valueOf(ret));
			}
		}
		return new ExceptionOperation("Method not found");
	}

	@Override
	public Operation calculate(VariableAmount object, CalculationController control) {
		return calculate (a.calculate(object, control), b.calculate(object, control), c.calculate(object, control), d.calculate(object, control), nameChecked, cl, obj, m);
	}

	@Override
	public int size() {
		return 3;
	}

	@Override
	public Operation get(int index) {
		switch (index){
		case 0: return a;
		case 1: return c;
		case 2: return d;
		default:throw new ArrayIndexOutOfBoundsException(index);
	}
	}

}
