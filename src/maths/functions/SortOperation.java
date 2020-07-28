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

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import maths.Operation;
import maths.variable.VariableAmount;
import maths.data.ArrayOperation;
import maths.data.BooleanOperation;
import maths.data.RealDoubleOperation;
import maths.functions.atomic.HigherOperation;

public class SortOperation extends FunctionOperation {
	public final Operation a;
	private static final Comparator<Operation> operationComperator = new Comparator<Operation>() {
		
		@Override
		public int compare(Operation arg0, Operation arg1) {
			Operation erg = HigherOperation.calculate(arg0, arg1);
			return erg == BooleanOperation.TRUE ? 1 : erg == BooleanOperation.FALSE ? -1 : 0;
		}
	};
	
	public SortOperation(Operation a){
    	if ((this.a = a) == null)
    		throw new NullPointerException();
	}
	
	public static final Operation calculate(Operation a){
		if (!(a.isArray()) || !((ArrayOperation)a).isPrimitive())
			return new SortOperation(a);
		final Operation data[] = ((ArrayOperation)a).toArray();
		for (Operation op:data)
			if (!(op.isRealFloatingNumber()))
				return RealDoubleOperation.NaN;
		Arrays.sort(data, operationComperator);
		return ArrayOperation.getInstance(data);
	}
	
	
	@Override
	public Operation calculate(VariableAmount object, CalculationController control) {
		return calculate(a.calculate(object, control));
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
		return "sort";
	}
	
	@Override
	public Operation getInstance(List<Operation> subclasses) {
		return new SortOperation(subclasses.get(0));
	}
}
