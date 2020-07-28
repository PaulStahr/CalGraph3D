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
import maths.algorithm.OperationCalculate;
import maths.data.ArrayOperation;
import maths.data.RealLongOperation;
import maths.functions.atomic.AdditionOperation;
import maths.functions.atomic.MultiplicationOperation;
import maths.functions.atomic.PowerOperation.SquareOperation;
import maths.functions.atomic.SubtractionOperation;
import maths.functions.hyperbolic.CosinusOperation;
import maths.functions.hyperbolic.SinusOperation;

public class CreateRotationOperation extends FunctionOperation {
	public final Operation a;
	public final Operation b;
	
	public CreateRotationOperation(Operation a, Operation b){
    	if ((this.a = a) == null || (this.b = b) == null)
    		throw new NullPointerException();
	}
	
	public static final Operation calculate(Operation a, Operation b, final CalculationController control) {
		if (a instanceof ArrayOperation)
		{
			Operation vector[] = ((ArrayOperation)a).toArray();
			Operation cos = CosinusOperation.calculate(b);
			Operation sin = SinusOperation.calculate(b);
			Operation onecos = SubtractionOperation.calculate(RealLongOperation.POSITIVE_ONE, cos, control);
			Operation res[][] = new Operation[3][3];
			for (int i = 0; i < 3; ++i)
			{
				res[i][i] = AdditionOperation.calculate(MultiplicationOperation.calculate(SquareOperation.calculate(vector[i], control), onecos, control), cos, control);
				for (int j = 0; j < i; ++j)
				{
					Operation left = MultiplicationOperation.calculate(MultiplicationOperation.calculate(vector[i], vector[j], control), onecos, control);
					Operation add = MultiplicationOperation.calculate(vector[3 - i - j], sin, control);
					res[i][j] = AdditionOperation.calculate(left, add, control);
					res[j][i] = SubtractionOperation.calculate(left, add, control);
				}
			}
			Operation tmp = res[1][2];
			res[1][2] = res[2][1];
			res[2][1] = tmp;
			return ArrayOperation.getInstance(res);
		}
		Operation erg = OperationCalculate.standardCalculations(a, b);
		if (erg != null)
			return erg;
		return new CreateRotationOperation(a, b);
	}

	
	@Override
	public Operation calculate(VariableAmount object, CalculationController control) {
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
	public String getFunctionName() {
		return "rotmat";
	}	
	@Override
	public Operation getInstance(List<Operation> subclasses) {
		return new CreateRotationOperation(subclasses.get(0), subclasses.get(1));
	}
}
