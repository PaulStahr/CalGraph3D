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
package maths.functions.atomic;


import java.util.List;

import maths.Operation;
import maths.variable.VariableAmount;
import maths.algorithm.OperationCalculate;
import maths.data.ArrayOperation;
import maths.data.BooleanOperation;
import maths.data.Characters;
import maths.data.RealDoubleOperation;
import maths.functions.UnequalsOperation;

/** 
* @author  Paul Stahr
* @version 04.02.2012
*/
public final class NotOperation extends Operation
{
    public final Operation a;

    public NotOperation (final Operation operation){
    	if ((a = operation) == null)
    		throw new NullPointerException();
    }

    public static final Operation calculate(final Operation a){
        if (a.isBoolean())
            return a.booleanValue() ? BooleanOperation.FALSE:BooleanOperation.TRUE;
        if (a.isArray()){
        	return new ArrayOperation.ArrayCreator(a.size()){
				@Override
				public final Operation get(int index) {
					return calculate(a.get(index));
				}
        	}.getArray();
        }
        if (a instanceof EqualsOperation)
        	return new UnequalsOperation(a.get(0), a.get(1));
        if (a instanceof UnequalsOperation)
        	return new EqualsOperation(a.get(0), a.get(1));
        if (a.isPrimitive() && !a.isComplexIntegerNumber() && !a.isArray())
        	return RealDoubleOperation.NaN;
        Operation tmp = OperationCalculate.standardCalculations(a);
        if (tmp != null)
        	return tmp;
        return new NotOperation(a);
    }

    
	@Override
	public final Operation calculate (VariableAmount object, CalculationController control){
        return calculate (a.calculate(object, control));
    }
	@Override
	public final int size() {
		return 1;
	}

	
	@Override
	public final Operation get(int index) {
		switch (index){
			case 0: return a;
			default:throw new ArrayIndexOutOfBoundsException(index);
		}
	}
    
    
	@Override
	public final StringBuilder toString(Print type, StringBuilder stringBuilder){
        if (a.getPriority()<getPriority())
        	return a.toString(type, stringBuilder.append(Characters.NOT).append('(')).append(')');
        else
            return a.toString(type, stringBuilder.append(Characters.NOT));
    }

    
	@Override
	public int getPriority(){
        return 8;
    }
	
	
	@Override
	public Operation getInstance(List<Operation> subclasses) {
		return new NotOperation(subclasses.get(0));
	}
}
