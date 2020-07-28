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
import maths.algorithm.OperationCalculate;
import maths.data.ArrayOperation;
import maths.exception.ArrayIndexOutOfBoundsExceptionOperation;

public class RowReducedEchelonFormOperation extends FunctionOperation {
	public final Operation a;
	
	public RowReducedEchelonFormOperation(Operation a){
    	if ((this.a = a) == null)
    		throw new NullPointerException();
	}
	
	public static final Operation calculate(Operation a, final CalculationController control) {
		Operation mat[][] = OperationCalculate.toOperationArray2(a);
		if (mat != null){
			if (mat.length == 0)
				return ArrayOperation.getInstance(mat);
			final int size = mat[0].length;
			for (int i=1;i<mat.length;i++)
				if (size != mat[i].length)
					return new ArrayIndexOutOfBoundsExceptionOperation();
			Calculate.toRREF(mat, control);
			return ArrayOperation.getInstance(mat);
		}
		Operation erg = OperationCalculate.standardCalculations(a);
		if (erg != null)
			return erg;
		return new RowReducedEchelonFormOperation(a);
	}

	
	@Override
	public Operation calculate(VariableAmount object, CalculationController control) {
		return calculate(a.calculate(object, control), control);
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
	public String getFunctionName() {
		return "rref";
	}
	
	@Override
	public Operation getInstance(List<Operation> subclasses) {
		return new RowReducedEchelonFormOperation(subclasses.get(0));
	}
}
