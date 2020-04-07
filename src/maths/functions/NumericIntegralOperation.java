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
import maths.UserVariableOperation;
import maths.Variable;
import maths.VariableAmount;
import maths.VariableStack;
import maths.data.RealDoubleOperation;
import maths.data.RealLongOperation;
import maths.functions.atomic.AdditionOperation;
import maths.functions.atomic.DivisionOperation;
import maths.functions.atomic.MultiplicationOperation;
import maths.functions.atomic.SubtractionOperation;

public class NumericIntegralOperation extends FunctionOperation {
	private final Operation a, b, c, d;
	
	public NumericIntegralOperation(Operation a, Operation b, Operation c, Operation d){
		if ((this.a = a) == null || (this.b = b) == null || (this.c = c) == null || (this.d = d) == null){
			throw new NullPointerException();
		}
	}
	
	public static Operation calculate(Operation a, Operation b, Operation c, Operation d, VariableAmount va, CalculationController control){
		if (!(d instanceof UserVariableOperation && b.isRealFloatingNumber() && c.isRealFloatingNumber())){
			return new NumericIntegralOperation(a, b, c, d);
		}
		if (Double.isInfinite(c.doubleValue()) || Double.isInfinite(a.doubleValue())){
			if (Double.isInfinite(c.doubleValue()) && Double.isInfinite(a.doubleValue())){
				
			}else{
				
			}
		}else{
			int n = 1000;
			VariableStack st = new VariableStack(va);
			Variable v = new Variable(((UserVariableOperation)d).nameObject);
			st.add(v);
			Operation sum = RealLongOperation.ZERO;
			for (int i=0;i<n;++i){
				Operation pos = AdditionOperation.calculate(
						MultiplicationOperation.calculate(b, new RealDoubleOperation((double)i/n), control),
						MultiplicationOperation.calculate(c, new RealDoubleOperation((double)(n-i)/n), control),						
						control);
				v.setValue(pos);
				
				sum = AdditionOperation.calculate(sum, a.calculate(st, control), control);
			}
			sum = DivisionOperation.calculate(sum, new RealLongOperation(n), control);
			sum = MultiplicationOperation.calculate(SubtractionOperation.calculate(c, b, control), sum, control);
			return sum;
		}
		return null;
	}
	
	@Override
	public Operation calculate(VariableAmount object, CalculationController control) {
		return calculate(a.calculate(object, control), b.calculate(object, control), c.calculate(object, control), d, object, control);
	}

	@Override
	public int size() {
		return 4;
	}

	@Override
	public Operation get(int index) {
		switch(index){
			case 0:return a;
			case 1:return b;
			case 2:return c;
			case 3:return d;
			default: throw new IndexOutOfBoundsException();
		}
	}
	@Override
	public String getFunctionName() {
		return "nint";
	}

	@Override
	public Operation getInstance(List<Operation> subclasses) {
		return new NumericIntegralOperation(subclasses.get(0), subclasses.get(1), subclasses.get(2), subclasses.get(3));
	}

}
