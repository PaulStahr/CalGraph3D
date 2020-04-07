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
import maths.VariableAmount;
import maths.algorithm.OperationCalculate;
import maths.data.ArrayOperation;
import maths.exception.ArrayIndexOutOfBoundsExceptionOperation;

/** 
* @author  Paul Stahr
* @version 04.02.2012
*/
public final class MinimumOperation extends FunctionOperation
{
    public final Operation a, b;

    public MinimumOperation (Operation operation0, Operation operation1){
    	if ((a = operation0) == null || (b = operation1) == null)
    		throw new NullPointerException();
    }
    
    public static Operation calculate (final Operation a, final Operation b){
        if (a.isRealIntegerNumber() && b.isRealIntegerNumber())
            return a.longValue() < b.longValue() ? a : b;
        if (a.isRealFloatingNumber()&&b.isRealFloatingNumber() && !Double.isNaN(a.doubleValue()) && !Double.isNaN(b.doubleValue()))
            return a.doubleValue() < b.doubleValue() ? a : b;
        if (a.isArray() && b.isArray()){
        	if (a.size() != b.size())
        		return new ArrayIndexOutOfBoundsExceptionOperation();
           	return new ArrayOperation.ArrayCreator(a.size()){
    			
    			@Override
				public final Operation get(int index) {
    				return calculate(a.get(index), b.get(index));
    			}
           	}.getArray();
        }
        Operation erg = twoSideCalculate(a, b);
        if (erg != null || (erg = twoSideCalculate(b, a)) != null || (erg = OperationCalculate.standardCalculations(a, b))!=null)
        	return erg;
        return new MinimumOperation (a, b);        
    }
    
    private static final Operation twoSideCalculate(final Operation a, final Operation b){
    	if (b.isRealFloatingNumber()){
    		if (b.doubleValue() == Double.POSITIVE_INFINITY)
    			return a;
	        if (a.isArray() && b.isRealFloatingNumber()){
            	return new ArrayOperation.ArrayCreator(a.size()){
    				
    				@Override
					public final Operation get(int index) {
    					return calculate(a.get(index), b);
    				}
            	}.getArray();
            }
        }
        return null;
    }

    
	@Override
	public Operation calculate (VariableAmount object, CalculationController control){
        return calculate(a.calculate(object, control),b.calculate(object, control));
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
	public String getFunctionName() {
		return "min";
	}

	@Override
	public Operation getInstance(List<Operation> subclasses) {
		return new MinimumOperation(subclasses.get(0), subclasses.get(1));
	}
}
