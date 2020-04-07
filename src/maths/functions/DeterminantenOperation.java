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

import maths.data.RealLongOperation;
import maths.functions.FunctionOperation;
import maths.functions.atomic.AdditionOperation;
import maths.functions.atomic.MultiplicationOperation;

import maths.Operation;
import maths.VariableAmount;

public class DeterminantenOperation extends FunctionOperation {
	public final Operation a;
	
	public DeterminantenOperation (final Operation a){
    	if ((this.a = a) == null)
    		throw new NullPointerException();
    }
	
	
	@Override
	public String getFunctionName() {
		return "det";
	}

	
	@Override
	public Operation calculate(VariableAmount object, CalculationController control) {
		return calculate(a.calculate(object, control), control);
	}

	public static final Operation calculate(Operation a, CalculationController control){
		if (!(a.isArray()))
			return new DeterminantenOperation(a);
		final int length = a.size();
		List<Permutation> p = Permutation.getPermutationList(length);
		Operation erg = RealLongOperation.ZERO;
		for (int i=0;i<p.size();i++){
			Permutation perm = p.get(i);
			Operation summand = perm.signum() == 1 ? RealLongOperation.POSITIVE_ONE : RealLongOperation.NEGATIVE_ONE;
			for (int j=0;j<length;j++)
				summand = MultiplicationOperation.calculate(summand, a.get(j).get(perm.get(j)), control);
			erg = AdditionOperation.calculate(erg, summand, control);
		}
		return erg;
	}

	
	@Override
	public int size() {
		return 1;
	}

	
	@Override
	public Operation get(int index) {
		switch(index){
			case 0:return a;
			default:throw new ArrayIndexOutOfBoundsException(index);
		}
	}
	
	@Override
	public Operation getInstance(List<Operation> subclasses) {
		return new DeterminantenOperation(subclasses.get(0));
	}
}
