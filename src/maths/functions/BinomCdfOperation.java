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
import maths.data.RealDoubleOperation;
import maths.exception.ExceptionOperation;

public class BinomCdfOperation extends FunctionOperation {
	public final Operation a, b, c;
	
	public BinomCdfOperation(Operation a, Operation b, Operation c){
		if ((this.a = a) == null || (this.b = b)==null || (this.c = c) == null)
			throw new NullPointerException();
	}
	
	public static final Operation calculate(Operation a, Operation b, Operation c){
		if (a.isRealIntegerNumber() && b.isRealFloatingNumber() && c.isRealFloatingNumber()){
			final double bd = b.doubleValue();
			if (bd < 0 || bd > 1)
				return new ExceptionOperation("Wahrscheinlichkeit soll sein 0<=p<=1");
			final double erg = Calculate.binomCdf(a.longValue(), bd, c.longValue());
			return erg == -1 ? RealDoubleOperation.NaN : new RealDoubleOperation(erg);
		}
		Operation erg = OperationCalculate.standardCalculations(a, b, c);
		if (erg != null)
			return erg;
		return new BinomCdfOperation(a, b, c);
	}
	
	
	@Override
	public final Operation calculate(VariableAmount object, CalculationController control) {
		return calculate(a.calculate(object, control), b.calculate(object, control), c.calculate(object, control));
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
		return "binomcdf";
	}	
	
	@Override
	public Operation getInstance(List<Operation> subclasses) {
		return new BinomCdfOperation(subclasses.get(0), subclasses.get(1), subclasses.get(2));
	}
}
