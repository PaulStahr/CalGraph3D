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
import maths.data.BooleanOperation;
import maths.data.Characters;

/**
* @author  Paul Stahr
* @version 04.02.2012
*/
public final class EqualsOperation extends LinkingOperation
{
    public final Operation a, b;

    public EqualsOperation (Operation a, Operation b){
    	if ((this.a = a) == null || (this.b = b) == null)
    		throw new NullPointerException();
    }

    public static final Operation calculate(Operation a, Operation b){
        if (a.isComplexIntegerNumber() && b.isComplexIntegerNumber())
        	return BooleanOperation.get(a.longValue() == b.longValue() && a.longValueImag() == b.longValueImag()); 
        if (a.isComplexRationalNumber() && b.isComplexRationalNumber())
        	return BooleanOperation.get(a.longNumeratorValue() == b.longNumeratorValue() && a.longDenumeratorValueImag() == b.longDenumeratorValueImag()); 
        if (a.isComplexFloatingNumber() && b.isComplexFloatingNumber())
        	return BooleanOperation.get(a.doubleValue() == b.doubleValue() && a.doubleValueImag() == b.doubleValueImag());
        if (a.isString() && b.isString())
            return BooleanOperation.get(a.stringValue().equals(b.stringValue()));
        if (a.isBoolean() && b.isBoolean())
        	return BooleanOperation.get(a.booleanValue()==b.booleanValue());
        if (a.equals(b))
        	return BooleanOperation.TRUE;
        Operation erg = OperationCalculate.standardCalculations(a, b);
        if (erg != null)
        	return erg;
       return new EqualsOperation(a, b);
    }

    
	@Override
	public final Operation calculate (VariableAmount object, CalculationController control){
        return calculate (a.calculate(object, control), b.calculate(object, control));
	}
	
	@Override
	public final int size() {
		return 2;
	}

	
	@Override
	public final Operation get(int index) {
		switch (index){
			case 0: return a;
			case 1: return b;
			default:throw new ArrayIndexOutOfBoundsException(index);
		}
	}
	

    
	@Override
	public final int getPriority(){
        return 2;
    }

	
	@Override
	public char getChar() {
		return Characters.EQ;
	}
	
	
	@Override
	public Operation getInstance(List<Operation> subclasses) {
		return new EqualsOperation(subclasses.get(0), subclasses.get(1));
	}
}
