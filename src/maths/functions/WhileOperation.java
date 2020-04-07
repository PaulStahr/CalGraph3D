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
import maths.data.RealDoubleOperation;
import maths.exception.ExceptionOperation;

public class WhileOperation extends FunctionOperation {
    public final Operation a, b;

    public WhileOperation (Operation a, Operation b){
    	if ((this.a = a) == null || (this.b = b) == null)
    		throw new NullPointerException();
    }
	
	@Override
	public String getFunctionName() {
		return "while";
	}

	@Override
	public final Operation calculate(VariableAmount object, CalculationController control) {
		if (control == null || control.calculateLoop()){
			while(control == null || !control.getStopFlag()){
				Operation op = a.calculate(object, control);
				if (op.isBoolean()){
					if (!op.booleanValue())
						return RealDoubleOperation.NaN;
				}else{
					return new ExceptionOperation("Test ergab weder wahr noch falsch");
				}
				b.calculate(object, control);
			}
			return new ExceptionOperation("Stopped");
		}
		return this;
	}

	@Override
	public int size() {
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
	public Operation getInstance(List<Operation> subclasses) {
		return new WhileOperation(subclasses.get(0), subclasses.get(1));
	}
}
