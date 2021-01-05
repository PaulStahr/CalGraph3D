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
package maths.functions;


import java.util.List;

import maths.Operation;
import maths.algorithm.OperationCalculate;
import maths.data.CharacterOperation;
import maths.data.MapOperation;
import maths.exception.ArrayIndexOutOfBoundsExceptionOperation;
import maths.variable.VariableAmount;

/** 
* @author  Paul Stahr
* @version 04.02.2012
*/

public final class ArrayIndexOperation extends Operation
{
    public final Operation array, index;

    public ArrayIndexOperation (Operation a, Operation b){
    	if ((array = a) == null || (index = b) == null)
    		throw new NullPointerException();
    }

    public static final Operation calculate(Operation a, Operation b){
    	if (b.isIntegral()){
	     	long index = b.longValue();
	    	if (index < 0)
	    		return new ArrayIndexOutOfBoundsExceptionOperation(index);
	        if (a.isArray())
	            return index >= a.size() ? new ArrayIndexOutOfBoundsExceptionOperation(index) : a.get((int)index);
	        if (a.isString())
	            return index >= a.stringValue().length() ? new ArrayIndexOutOfBoundsExceptionOperation(index) : CharacterOperation.getInstance(a.stringValue().charAt((int)index));
    	}
    	if (a instanceof MapOperation && b.isPrimitive()) {
    		return ((MapOperation)a).get(b);
    	}
        return new ArrayIndexOperation(a,b);
    }
    
    
	@Override
	public final Operation calculate (VariableAmount object, CalculationController control){
        Operation b = index.calculate(object, control), array = this.array;
        if (b.isIntegral()){
         	long index = b.longValue();
        	if (index < 0)
        		return new ArrayIndexOutOfBoundsExceptionOperation(index);
            if (array.isArray())
                return index >= array.size() ? new ArrayIndexOutOfBoundsExceptionOperation(index) : array.get((int)index).calculate(object, control);
            if (array.isString())
                return index >= array.stringValue().length() ? new ArrayIndexOutOfBoundsExceptionOperation(index) : CharacterOperation.getInstance(array.stringValue().charAt((int)index));
            array = array.calculate(object, control);
            if (array.isArray())
                return index >= array.size() ? new ArrayIndexOutOfBoundsExceptionOperation(index) : array.get((int)index);
            if (array.isString())
                return index >= array.stringValue().length() ? new ArrayIndexOutOfBoundsExceptionOperation(index) : CharacterOperation.getInstance(array.stringValue().charAt((int)index));
        }else if (array instanceof MapOperation && b.isPrimitive()){
        	return ((MapOperation)array).get(b);
        }else
        {
        	array = array.calculate(object, control);
        }
		Operation erg = OperationCalculate.standardCalculations(array, b);
		if (erg != null)
			return erg;
        return new ArrayIndexOperation(array,b);
    }

	@Override
	public final int size() {return 2;}

	
	@Override
	public final Operation get(int index) {
		switch (index){
			case 0: return array;
			case 1: return this.index;
			default:throw new ArrayIndexOutOfBoundsException(index);
		}
	}
    
	@Override
	public final StringBuilder toString (Print type, StringBuilder stringBuilder){
		return index.toString(type, array.toString(type, stringBuilder).append('[')).append(']');
    }

	@Override
	public Operation getInstance(List<Operation> subclasses) {return new ArrayIndexOperation(subclasses.get(0), subclasses.get(1));}
}
