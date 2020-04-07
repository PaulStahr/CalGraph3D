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
import maths.data.StringOperation;

public class FormatOperation extends FunctionOperation {
	public final Operation a, b;
	
	public FormatOperation(Operation a, Operation b){
		if ((this.a = a)==null || ((this.b = b) == null))
			throw new NullPointerException();
	}
	
	public static final Operation calculate(Operation a, Operation b){
		if (!(a.isString()))
			return a.isPrimitive() ? RealDoubleOperation.NaN : new FormatOperation(a, b);
		final Print type;
		switch(a.stringValue()){
			case "calgraph":type = Print.CALGRAPH;break;
			case "latex":type = Print.LATEX;break;
			case "open_office":type = Print.OPEN_OFFICE;break;
			default:return RealDoubleOperation.NaN;
		}
		return new StringOperation(b.toString(type));
	}
	
	
	@Override
	public Operation calculate(VariableAmount object, CalculationController control) {
		return calculate(a.calculate(object, control), b);
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
		return "format";
	}

	@Override
	public Operation getInstance(List<Operation> subclasses) {
		return new FormatOperation(subclasses.get(0), subclasses.get(1));
	}
}
