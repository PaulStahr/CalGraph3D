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
import maths.variable.VariableAmount;
import maths.algorithm.Calculate;
import maths.data.ArrayOperation;
import maths.data.RealDoubleOperation;
import maths.data.RealLongOperation;
import maths.exception.ExceptionOperation;
import maths.functions.atomic.NegativeOperation;


/** 
* @author  Paul Stahr
* @version 04.02.2012
*/

public final class AbsoluteOperation extends FunctionOperation
{
    public final Operation a;

    public AbsoluteOperation (final Operation a){
    	if ((this.a = a) == null)
    		throw new NullPointerException();
    }
    
    public static final Operation calculate (final Operation a){
        if (a.isRealFloatingNumber())
            return a.isPositive() ? a : a.getNegative();
        if (a.isComplexIntegerNumber()){
        	long real = a.longValue(), imag = a.longValueImag();
        	if (real == 0)
        		return new RealLongOperation(imag> 0 ? imag : -imag);
        	long realsq = a.longValue() * a.longValue(), imagsq = a.longValueImag() * a.longValueImag();
        	if (realsq /real != real || imagsq / imag != imag)
        		return new RealDoubleOperation(Math.sqrt((double)real * real + (double)imag * imag));
        	if (Long.MAX_VALUE - realsq < imagsq)
        		return new RealDoubleOperation(Math.sqrt((double)realsq + (double)imagsq));
        	long sqrt = Calculate.sqrt(realsq + imagsq);
        	return sqrt == -1 ? new RealDoubleOperation(Math.sqrt(realsq + imagsq)) : new RealLongOperation(sqrt);
        }
        if (a.isComplexFloatingNumber())
        	return new RealDoubleOperation(Math.sqrt(a.doubleValue() * a.doubleValue() + a.doubleValueImag() * a.doubleValueImag()));
        if (a.isArray()){
        	return new ArrayOperation.ArrayCreator(a.size()){
				@Override
				public final Operation get(int index) {
					return calculate(a.get(index));
				}
        	}.getArray();
        }
        if (a instanceof NegativeOperation)
        	return new AbsoluteOperation(a.get(0));
        if (a instanceof ExceptionOperation)
        	return a;
        if (a instanceof AbsoluteOperation)
        	return a;
        return new AbsoluteOperation(a);
    }

    
	@Override
	public final Operation calculate (final VariableAmount object, CalculationController control){
        return calculate(a.calculate(object, control));
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
	public final String getFunctionName(){
    	return "abs";
    }
	

	@Override
	public Operation getInstance(List<Operation> subclasses) {
		return new AbsoluteOperation(subclasses.get(0));
	}
}
