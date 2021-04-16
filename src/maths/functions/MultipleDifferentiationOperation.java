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
import maths.variable.VariableAmount;
import maths.exception.ExceptionOperation;

public class MultipleDifferentiationOperation extends FunctionOperation {
	public final Operation a,b,c;
	
	public MultipleDifferentiationOperation(Operation a, Operation b, Operation c){
		if ((this.a = a) == null || (this.b = b) == null || (this.c = c) == null)
			throw new NullPointerException();
	}
	
	
	@Override
	public Operation calculate(VariableAmount object, CalculationController control) {
		return calculate(a.calculate(object, control), b.calculate(object, control), c.calculate(object, control), control);
	}

	public static final Operation calculate(Operation a, Operation b, Operation c, CalculationController control){
		if (c.isIntegral()){
			for (long l = c.longValue();l>0;l--){
				a = DifferentiationOperation.calculate(a, b, control);
				if (control != null && control.getStopFlag())
					return new ExceptionOperation("Berechnung anbgebrochen");
			}
			return a;
		}
		Operation tmp = OperationCalculate.standardCalculations(a, b, c);
		if (tmp != null)
			return tmp;
		return new MultipleDifferentiationOperation(a, b, c);
	}

	
	@Override
	public final int size() {
		return 3;
	}

	
	@Override
	public final Operation get(int index) {
		switch (index){
			case 0: return a;
			case 1: return b;
			case 2: return c;
			default:throw new ArrayIndexOutOfBoundsException(index);
		}
	}

	
	@Override
	public String getFunctionName() {
		return "diff";
	}

	@Override
	public Operation getInstance(List<Operation> subclasses) {
		return new MultipleDifferentiationOperation(subclasses.get(0), subclasses.get(1), subclasses.get(2));
	}
}
