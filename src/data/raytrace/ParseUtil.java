package data.raytrace;

import java.awt.Color;
import java.io.File;

import geometry.Matrixd;
import geometry.Vector3d;
import maths.Controller;
import maths.Operation;
import maths.OperationCompiler;
import maths.algorithm.OperationCalculate;
import maths.data.ArrayOperation;
import maths.data.RealDoubleOperation;
import maths.data.RealLongOperation;
import maths.exception.OperationParseException;
import maths.variable.VariableAmount;
import util.OperationGeometry;

public class ParseUtil {
	public String str;
	public Operation op;
	public boolean b;
	public double d;
	private final StringBuilder strB = new StringBuilder();
	
	
	public static final String parseString(Object o)
	{
		if (o == null || o.equals(""))
		{
			return null;
		}
		if (o instanceof String)
		{
			return (String)o;
		}
		throw new IllegalArgumentException("Class:" + o.getClass());
	}
	
	public static final boolean parseBoolean(Object o)
	{
		if (o instanceof String)
		{
			return Boolean.parseBoolean((String)o);
		}
		if (o instanceof Boolean)
		{
			return (boolean)o;
		}	
		throw new IllegalArgumentException("Class:" + o.getClass());
	}
	
	public static final int parseInteger(Object o)
	{
		if (o instanceof String)
		{
			return Integer.valueOf((String)o);
		}
		if (o instanceof Integer)
		{
			return (int)o;
		}
		throw new IllegalArgumentException("Class:" + o.getClass());
	}
	
	public final void parseMat(Object o, Matrixd mat, VariableAmount variables, Controller controll) throws OperationParseException
	{
		if (o instanceof double[][])
		{
			strB.setLength(0);
			str = (op = OperationCalculate.toList((double[][])o)).toString(strB).toString();
			mat.setRowMajor((double[][])o);
		}
		else if (o instanceof ArrayOperation)
		{
			op = (ArrayOperation)o;
			boolean success = OperationGeometry.parseMatRowMajor(op.calculate(variables, controll), mat);
			strB.setLength(0);
			str = op.toString(strB).toString();
			if (!success)
			{
				throw new OperationParseException(str);
			}
		}
		else if (o instanceof String)
		{
			op = OperationCompiler.compile((String)o);
			boolean success = OperationGeometry.parseMatRowMajor(op.calculate(variables, controll), mat);
			strB.setLength(0);
			str = op.toString(strB).toString();
			if (!success)
			{
				throw new OperationParseException(str);
			}
		}
		else if (o instanceof Matrixd)
		{
			mat.set((Matrixd)o);
			strB.setLength(0);
			str = (op = new ArrayOperation(mat)).toString(strB).toString();
		}
		else
		{
			throw new IllegalArgumentException("Class:" + o.getClass());
		}
	}
	
	public final String[] parseStringArray(Object o, Controller controll) throws OperationParseException
	{
		if (o == null || o.equals(""))
		{
			str = "";
			return null;
		}
		if (o instanceof String)
		{
			str = (String)o;
			return OperationCalculate.toStringArray(OperationCompiler.compile((String)o));
		}
		throw new IllegalArgumentException("Class:" + o.getClass());
	}
	
	public final File parseFileString(Object o, VariableAmount variables, Controller controll) throws OperationParseException
	{
		if (o == null || o.equals(""))
		{
			return null;
		}
		if (o instanceof Operation)
		{
			str = (op = (Operation)o).toString();
			Operation tmp = op.calculate(variables, controll);
			if (tmp.isString())
			{
				return new File(tmp.stringValue());
			}
			return null;
		}
		if (o instanceof String)
		{
			Operation tmp = (op = OperationCompiler.compile(str = (String)o)).calculate(variables, controll);
			if (tmp.isString())
			{
				return new File(tmp.stringValue());
			}
			return null;
		}
		throw new IllegalArgumentException("Class:" + o.getClass());
	}
	
	public final double parseDoubleString(Object o, VariableAmount variables, Controller controll) throws OperationParseException
	{
		if (o instanceof String)
		{
			return d=(op = OperationCompiler.compile(str = (String)o)).calculate(variables, controll).doubleValue();
		}
		if (o instanceof Double)
		{
			op=new RealDoubleOperation(d=(double)o);
			str = Double.toString(d);
			return d;
		}
		if (o instanceof Integer)
		{
			int value = (int)o;
			op = new RealLongOperation(value);
			str = Integer.toString(value);
			return d = value;
		}
		throw new IllegalArgumentException("Class:" + o.getClass());
	}
	
	public final Operation parseOperationString(Object o, VariableAmount variables, Controller controll) throws OperationParseException
	{
		if (o instanceof String)
		{
			(op = OperationCompiler.compile(str = (String)o)).calculate(variables, controll);
			d = op.doubleValue();
			return op;
		}
		if (o instanceof Double)
		{
			op=new RealDoubleOperation(d=(double)o);
			str = Double.toString(d);
			return op;
		}
		if (o instanceof Integer)
		{
			int value = (int)o;
			op = new RealLongOperation(value);
			str = Integer.toString(value);
			d = value;
			return op;
		}
		throw new IllegalArgumentException("Class:" + o.getClass());
	}
	
	public final int parseIntegerString(Object o, VariableAmount variables, Controller controll) throws OperationParseException
	{
		if (o instanceof String)
		{
			return (int)(d=(op = OperationCompiler.compile(str = (String)o)).calculate(variables, controll).doubleValue());
		}
		if (o instanceof Double)
		{
			op=new RealDoubleOperation(d=(double)o);
			str = Double.toString(d);
			return (int)d;
		}
		if (o instanceof Integer)
		{
			int value = (int)o;
			d = value;
			op = new RealLongOperation(value);
			str = Integer.toString(value);
			return value;
		}
		throw new IllegalArgumentException("Class:" + o.getClass());
	}
	
	public static final double parseDouble(Object o)
	{
		if (o instanceof String)
		{
			return Double.parseDouble((String)o);
		}
		if (o instanceof Double)
		{
			return (double)o;
		}
		if (o instanceof Integer)
		{
			return (int)o;
		}
		throw new IllegalArgumentException("Class:" + o.getClass());
	}
	
	public final Color parseColor(Object o, VariableAmount variables, Controller controll) throws OperationParseException
	{
		if (o instanceof Color)
		{
			Color col = (Color)o;
			strB.setLength(0);
			long components[] = new long[] {col.getRed(), col.getGreen(), col.getBlue(), col.getAlpha()};
			op = new ArrayOperation(components);
			str = OperationCompiler.toString(strB, components).toString();
			return col;
		}
		if (o instanceof String)
		{
			str = (String)o;
			double components[] = OperationCalculate.toDoubleArray((op = OperationCompiler.compile(str)).calculate(variables, controll), new double[4]);
			return new Color((int)components[0], (int)components[1], (int)components[2], (int)components[3]);
		}
		if (o instanceof double[])
		{
			double components[] = (double[])o;
			strB.setLength(0);
			str = OperationCompiler.toString(strB, components).toString();
			op = new ArrayOperation(components);
			return new Color((int)components[0], (int)components[1], (int)components[2], (int)components[3]);
		}
		if (o instanceof int[])
		{
			int components[] = (int[])o;
			strB.setLength(0);
			str = OperationCompiler.toString(strB, components).toString();
			op =  new ArrayOperation(components);
			return new Color(components[0], components[1], components[2], components[3]);
		}
		throw new IllegalArgumentException("Class:" + o.getClass());
	}
	
	public static final String parsePositiontring(Object o, Vector3d vec, StringBuilder strB, VariableAmount variables, Controller controll) throws OperationParseException
	{
		if (o instanceof String)
		{
			vec.set(Double.NaN, Double.NaN, Double.NaN);
			OperationCalculate.toDoubleArray(OperationCompiler.compile((String)o).calculate(variables, controll), vec);
			return (String)o;
		}
		if (o instanceof double[])
		{
			double direction[] = (double[])o;
			vec.set(direction, 0);
			strB.setLength(0);
			return new ArrayOperation(direction).toString(strB).toString();
		}
		if (o instanceof Vector3d)
		{
			Vector3d position = (Vector3d)o;
			vec.set(position);
			strB.setLength(0);
			Operation op=new ArrayOperation(position);
			return op.toString(strB).toString();
		}
		throw new IllegalArgumentException("Class:" + o.getClass());
	}
	
	public final Operation parsePositionString(Object o, Vector3d vec, VariableAmount variables, Controller controll) throws OperationParseException
	{
		if (o instanceof String)
		{
			vec.set(Double.NaN, Double.NaN, Double.NaN);
		    op = OperationCompiler.compile(str = (String)o);
		    OperationCalculate.toDoubleArray(op.calculate(variables, controll), vec);
			return op;
		}
		if (o instanceof double[])
		{
			double position[] = (double[])o;
			vec.set(position, 0);
			strB.setLength(0);
			op = new ArrayOperation(position);
			str = op.toString(strB).toString();
			return op;
		}
		if (o instanceof Vector3d)
		{
			Vector3d position = (Vector3d)o;
			vec.set(position);
			strB.setLength(0);
			op=new ArrayOperation(position);
			str = op.toString(strB).toString();
			return op;
		}		
		throw new IllegalArgumentException("Class:" + o.getClass());
	}

	public void reset() {
		str = null;
		op = null;
		b = false;
		d = Double.NaN;
		strB.setLength(0);
	}
}
