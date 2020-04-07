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
import maths.VariableStack;
import maths.data.RealDoubleOperation;
import maths.exception.ExceptionOperation;

public class ForOperation extends FunctionOperation {
    public final Operation a, b, c, d;

    public ForOperation (final Operation a, final Operation b, final Operation c, final Operation d){
    	if ((this.a = a) == null || (this.b = b) == null || (this.c = c) == null || (this.d = d) == null)
    		throw new NullPointerException();
    }

	@Override
	public String getFunctionName() {
		return "for";
	}

	@Override
	public final Operation calculate(VariableAmount object, CalculationController control) {
		if (control.calculateLoop()){
			final VariableStack vs = new VariableStack(object);
			a.calculate(vs, control);
			while(control == null || !control.getStopFlag()){
				final Operation op = b.calculate(object, control);
				if (op.isBoolean()){
					if (!op.booleanValue())
						return RealDoubleOperation.NaN;
				}else{
					return new ExceptionOperation("Test ergab weder wahr noch falsch");
				}
				d.calculate(vs, control);
				c.calculate(vs, control);
			}
			return new ExceptionOperation("Stopped");
		}
		return this;
	}

	@Override
	public int size() {
		return 4;
	}

	@Override
	
	public final Operation get(int index) {
		switch (index){
			case 0: return a;
			case 1: return b;
			case 2: return c;
			case 3: return d;
			default:throw new ArrayIndexOutOfBoundsException(index);
		}
	}

	@Override
	public Operation getInstance(List<Operation> subclasses) {
		return new ForOperation(subclasses.get(0), subclasses.get(1), subclasses.get(2), subclasses.get(3));
	}
}
