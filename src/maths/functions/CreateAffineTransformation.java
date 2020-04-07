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
import maths.data.RealLongOperation;

public final class CreateAffineTransformation extends FunctionOperation {
	public final Operation a;
	public final Operation b;
	
	public CreateAffineTransformation(Operation a, Operation b){
    	if ((this.a = a) == null || (this.b = b) == null)
    		throw new NullPointerException();
	}
	
	public static final Operation calculate(Operation a, Operation b, final CalculationController control) {
		Operation mat[][] = OperationCalculate.toOperationArray2(a);
		if (mat != null && b instanceof ArrayOperation)
		{
			Operation res[][] = new Operation[4][4];
			for (int i = 0; i < 3; ++i)
			{
				System.arraycopy(mat[i], 0, res[i], 0, 3);
				res[3][i] = RealLongOperation.ZERO;
				res[i][3] = b.get(i);
			}
			res[3][3] = RealLongOperation.POSITIVE_ONE;
			return ArrayOperation.getInstance(res);
		}
		Operation erg = OperationCalculate.standardCalculations(a, b);
		if (erg != null)
			return erg;
		return new CreateAffineTransformation(a, b);
	}

	
	@Override
	public final Operation calculate(VariableAmount object, CalculationController control) {
		return calculate(a.calculate(object, control), b.calculate(object, control), control);
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
	public final String getFunctionName() {
		return "affine";
	}	
	@Override
	public final Operation getInstance(List<Operation> subclasses) {
		return new CreateAffineTransformation(subclasses.get(0), subclasses.get(1));
	}
}
