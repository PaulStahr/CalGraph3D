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
package maths.variable;

import java.util.AbstractList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import maths.Operation;
import maths.OperationCompiler;
import maths.Operation.CalculationController;
import maths.algorithm.OperationCalculate;
import maths.data.ArrayOperation;
import maths.data.BooleanOperation;
import maths.data.RealDoubleOperation;
import maths.data.RealLongOperation;
import maths.data.StringId;
import maths.data.StringId.StringIdObject;
import maths.exception.ArrayIndexOutOfBoundsExceptionOperation;
import maths.exception.ExceptionOperation;
import maths.exception.OperationParseException;
import util.ArrayTools;
import util.data.SortedIntegerArrayList;
import util.data.SortedIntegerArrayList.ReadOnlySortedIntegerArrayList;

/** 
* @author  Paul Stahr
* @version 04.02.2012
*/
public final class Variable implements Comparable<Variable>{
	private static final Logger logger = LoggerFactory.getLogger(Variable.class);
    public static final Comparator<Variable> idComperator = new Comparator<Variable>(){
		@Override
		public int compare(Variable arg0, Variable arg1) {
			return arg0.nameId == arg1.nameId ? arg0.operands.length - arg1.operands.length : arg0.nameId-arg1.nameId;
		}
    };
	
    public final int nameId;
    public final StringId.StringIdObject nameObject;
    private int modCount = (int)(Math.random()*Integer.MAX_VALUE);
    private Operation value;
    private VariableListener variableListeners[] = VariableListener.EMPTY_VARIABLE_LISTENER_ARRAY;
    private int variableListenerCount = 0;
    private final StringId.StringIdObject operands[];
    private ReadOnlySortedIntegerArrayList includedVariables = SortedIntegerArrayList.EMPTY_LIST;
    private int includedVariablesChCount = 0;
    public final UserVariableOperationInserted inserted;
 
    public Variable(String name){
    	this (name, (Operation)null);
    }
    
    public Variable(String name, int value){
    	this(name, new RealLongOperation(value));
     }

    public Variable(String name, float value){
    	this (name, new RealDoubleOperation(value));
    }

    public Variable(String name, double value){
    	this (name, new RealDoubleOperation(value));
    }

    public Variable(String name, String value) throws OperationParseException{
    	this (name, value == null || value.equals("") ? null : OperationCompiler.compile(value));
    }

    public Variable(StringId.StringIdObject name, Operation value){
    	this(name, value, null);  	
    }
    
	public Variable(StringIdObject name) {
		this(name, null);
	}

    public Variable(String name, Operation value){
    	this(StringId.getStringAndId(name), value);
    }
        
    public Variable(String name, String value, StringId.StringIdObject operands[]) throws OperationParseException{
    	this (StringId.getStringAndId(name), OperationCompiler.compile(value), operands);
    }

    public Variable(String name, String value, String operands[])throws OperationParseException{
    	this (StringId.getStringAndId(name), OperationCompiler.compile(value), StringId.getStringAndId(operands));
	}

    
    public Variable(String name, Operation value, StringId.StringIdObject operands[]){
    	this(StringId.getStringAndId(name), value, operands);
    }

	public Variable(String name, Operation value, String operands[]) {
		this(StringId.getStringAndId(name), value, StringId.getStringAndId(operands));
	}

    public Variable(StringIdObject name, Operation value, StringId.StringIdObject operands[]) {
        if (!isValidName((this.nameObject = name).string))
            throw new IllegalArgumentException("Invalid variablename: " + name);
    	this.nameId = name.id;
        this.value = value;
        if (operands != null){
	        this.operands = new StringId.StringIdObject[operands.length];
	        for (int i=0;i<operands.length;i++)
	        	if ((this.operands[i] = operands[i])==null)
	        		throw new NullPointerException();
        }else{
        	this.operands = null;
        }
        inserted = new UserVariableOperationInserted(this);
	}
    
    public final void removeVariableListener(VariableListener vl)
    {
    	if (vl == null)
    	{
    		throw new NullPointerException();
    	}
    	variableListenerCount = ArrayTools.remove(variableListeners, variableListenerCount, vl);
    }

	public final void addVariableListener(VariableListener vl){
    	if (vl == null)
    		throw new IllegalArgumentException();
    	variableListeners = ArrayTools.add(variableListeners, variableListenerCount++, vl);
    }
    
    public final boolean removeAllVariableListener(VariableListener vl){
       	if (vl == null)
    		throw new IllegalArgumentException();
       	int tmp = variableListenerCount;
       	variableListenerCount = ArrayTools.remove(variableListeners, variableListenerCount, vl);
       	return tmp != variableListenerCount;
    }

    public final String stringValue(){
        return value == null ? null : value.toString();
    }

    public final StringBuilder stringValue(StringBuilder strBuilder){
        return value.toString(strBuilder);
    }

    public final void setValue(boolean value){
        this.value = BooleanOperation.get(value);
        valueChanged();
    }

    public final void setValue(long value){
        this.value = new RealLongOperation(value);
        valueChanged();
    }

    public final void setValue(double value){
        this.value = new RealDoubleOperation(value);
        valueChanged();
    }

    public final void setValue(String value) throws OperationParseException{
        this.value = OperationCompiler.compile(value);
        valueChanged();
    }

    public final void setValue(Operation value){
        this.value = value;
        valueChanged();
    }

    private final void valueChanged(){
        modCount++;
        for (int i=0;i<variableListenerCount;++i){
          	try{
           		variableListeners[i].variableChanged();
           	}catch(Exception e){
           		logger.error(e.toString(), e);
           	}
        }
    }

    public final Operation reduce (VariableAmount object, CalculationController control){
        if (value == null || operands != null)
            return null;
        return value.calculate(object, control);
    }

    public final Operation calculate(VariableAmount object, CalculationController control){
        if (value == null || operands != null)
            return null;
        return value.calculate(object, control);
    }
    
    public boolean set (int index, Operation o){
    	if (!(value.isArray()))
    		return false;
    	value = ((ArrayOperation)value).set(index, o);
    	valueChanged();
    	return true;
    }
    
    public final Operation set(Operation indexes[], VariableAmount vars, Operation o, CalculationController control){
    	Operation sub = value;
    	for (int i=0;true;i++){
    		if (!(sub.isArray()))
    			return new ExceptionOperation("Not an Array");
    		Operation calced = indexes[i].calculate(vars, control);
    		if (!(calced.isRealFloatingNumber())){
    			if (calced instanceof ExceptionOperation)
    				return calced;
    			return null;
    		}
    		final long value = calced.longValue();
			if (value < 0 || value > sub.size())
				return new ArrayIndexOutOfBoundsExceptionOperation(value);
			if (i == indexes.length-1){
				((ArrayOperation)sub).set((int)value, o);
				valueChanged();
				return o;
			}else{
				sub = sub.get((int)value);
			}
    	}
    }
    
    public final boolean set(int indexes[], Operation o){
     	Operation sub=value;
    	for (int i=indexes.length-1;i>0;i--){
    		final int index = indexes[i];
    		if (index == -1)
    			continue;
    		if (!(sub.isArray()))
  				return false;
    		sub = sub.get(index);
    	}
		if (!(sub.isArray()))
			return false;
		value = ((ArrayOperation)sub).set(indexes[0],o);
    	valueChanged();
    	return true;
    }
    
    public final boolean set(List<Integer> indexes, Operation o){
     	Operation sub=value;
    	for (int i=indexes.size()-1;i>0;i--){
    		if (!(sub.isArray()))
  				return false;
    		sub = sub.get(indexes.get(i));
    	}
		if (!(sub.isArray()))
				return false;
		value = ((ArrayOperation)sub).set(indexes.get(0),o);
    	valueChanged();
    	return true;
    }
    
    public final Operation calculate(final VariableAmount object, final Operation operandValues[], final CalculationController control){
    	if (value == null || this.operands == null || operandValues.length != this.operands.length)
    		return null;
    	
    	VariableStack stack = new VariableStack(new AbstractList<Variable>() {
			@Override
			public Variable get(int index) {
				return new Variable(operands[index], operandValues[index].calculate(object, control));
			}

			@Override
			public int size() {
				return operandValues.length;
			}
		}, object);

    	try{
	    	return value.calculate(stack, control);
    	}catch(RuntimeException e){
        	throw e;
    	}
    }

    public final Operation reduce(final VariableAmount object, final Operation operandValues[], final CalculationController control){
    	if (value == null || this.operands == null || operands.length != this.operands.length)
    		return null;
 
    	VariableStack stack = new VariableStack(new AbstractList<Variable>() {
			@Override
			public Variable get(int index) {
				return new Variable(operands[index], operandValues[index].calculate(object, control));
			}

			@Override
			public int size() {
				return operandValues.length;
			}
		}, object);

    	try{
	    	return value.calculate(stack, control);
    	}catch(RuntimeException e){
        	throw e;
    	}
    }

    public static final boolean isValidName(String name){
        if (name==null || name.length()==0||!(Character.isLetter(name.charAt(0))|| name.charAt(0) == '_'))
            return false;

        for (int i=1;i<name.length();i++)
            if (!Character.isLetterOrDigit(name.charAt(i))&&name.charAt(i)!='_')
                return false;

        return true;
    }
 
    public static final boolean isValidName(CharSequence name, int begin, int end){
        if (name==null || begin == end||!(Character.isLetter(name.charAt(begin))|| name.charAt(begin) == '_'))
            return false;

        for (int i=1 + begin;i<end;i++)
            if (!Character.isLetterOrDigit(name.charAt(i))&&name.charAt(i)!='_')
                return false;

        return true;
    }
    
    public final Operation getValue(){
    	return value;
    }
    
	@Override
	public final String toString(){
    	return toString(new StringBuilder()).toString();
    }
    
    public final StringBuilder toString(StringBuilder strb){
    	strb.append(nameObject.string);
    	if (operands != null){
    		strb.append('(');
    		if (operands.length != 0){
    			strb.append(operands[0]);
    			for (int i=1;i<operands.length;i++)
    				strb.append(',').append(operands[i]);
    		}
    		strb.append(')');
    	}
    	strb.append('\u2192');
    	return value == null ? strb.append("null") : value.toString(strb);
    }
    
    public int operandCount(){
    	return operands == null ? -1 : operands.length;
    }
    
    public StringId.StringIdObject operand(int index){
    	return operands[index];
    }
    
    public final ReadOnlySortedIntegerArrayList getPendentVariables(){
    	if (includedVariablesChCount!=modCount){
    		if (value == null || value.isPrimitive()){
    			includedVariables = SortedIntegerArrayList.EMPTY_LIST;
    		}else{
	    		SortedIntegerArrayList list = OperationCalculate.getVariables(value, new SortedIntegerArrayList());
	    		if (list.isEmpty()){
	    			includedVariables = SortedIntegerArrayList.EMPTY_LIST;
	    		}else if (operands != null){
	        		int operandsSorted[] = StringId.getIds(operands);
	        		Arrays.sort(operandsSorted);
	        		for (int i = 0; i < operandsSorted.length; ++i)
	        		{
	        			list.removeObject(operandsSorted[i]);
	        		}
	    	    	includedVariables = list.readOnly();
	    		}else{
	    	    	includedVariables = list.readOnly();    			
	    		}
    		}
	    	includedVariablesChCount = modCount;
    	}
   		return includedVariables;
   	}
    
    public final int modCount(){
    	return modCount;
    }

	@Override
	public int compareTo(Variable o) {
		int erg = nameObject.string.compareTo(o.nameObject.string);
		if (erg != 0)
			return erg;
		return operands.length - o.operands.length;
	}
}
