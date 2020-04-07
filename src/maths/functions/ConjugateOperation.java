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
import maths.data.ComplexDoubleOperation;
import maths.data.ComplexLongOperation;
import maths.data.ComplexRationalOperation;

public class ConjugateOperation extends FunctionOperation {
    public final Operation a;

    public ConjugateOperation (Operation a){
    	if ((this.a = a) == null)
    		throw new NullPointerException();
    }
    
	@Override
	public Operation calculate(VariableAmount object, CalculationController control) {
		return calculate(a.calculate(object, control));
	}
	
    public static final Operation calculate(final Operation a){
        if (a.isRealFloatingNumber())
            return a;
        if (a.isComplexIntegerNumber())
        	return ComplexLongOperation.get(a.longValue(), -a.longValueImag());
        if (a.isComplexRationalNumber())
        	return ComplexRationalOperation.getInstance(a.longNumeratorValue(), a.longDenumeratorValue(), -a.longNumeratorValue(), a.longDenumeratorValueImag());
        if (a.isComplexFloatingNumber())
        	return ComplexDoubleOperation.get(a.doubleValue(), -a.doubleValueImag());
        if (a.isArray()){
        	return new ArrayOperation.ArrayCreator(a.size()){
				
				@Override
				public final Operation get(int index) {
					return calculate(a.get(index));
				}
        	}.getArray();
        }
        Operation erg = OperationCalculate.standardCalculations(a);
        if (erg != null)
        	return erg;
        return new ConjugateOperation (a);
    }
    	
	@Override
	public int size() {
		return 1;
	}

	@Override
	public Operation get(int index) {
		if (index != 0)
			throw new ArrayIndexOutOfBoundsException(index);
		return a;
	}

	@Override
	public String getFunctionName() {
		return "conjugate";
	}
	
	@Override
	public Operation getInstance(List<Operation> subclasses) {
		return new ConjugateOperation(subclasses.get(0));
	}
}
