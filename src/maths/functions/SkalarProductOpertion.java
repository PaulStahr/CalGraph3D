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
import maths.data.Characters;
import maths.data.RealLongOperation;
import maths.exception.ArrayIndexOutOfBoundsExceptionOperation;
import maths.exception.IllegalArgumentExceptionOperation;
import maths.functions.atomic.AdditionOperation;
import maths.functions.atomic.LinkingOperation;
import maths.functions.atomic.MultiplicationOperation;

/** 
* @author  Paul Stahr
* @version 26.02.2012
*/
public class SkalarProductOpertion extends LinkingOperation {
	public final Operation a,b;
	
	public SkalarProductOpertion(Operation a, Operation b){
    	if ((this.a = a) == null || (this.b = b) == null)
    		throw new NullPointerException();
    }
	
	
	@Override
	public Operation calculate(VariableAmount object, CalculationController control) {
		return calculate(a.calculate(object, control), b.calculate(object, control), control);
	}

	public static Operation calculate(Operation a, Operation b, final CalculationController control){
		if (a.isArray() && b.isArray()){
			if (a.size() != b.size())
				return new ArrayIndexOutOfBoundsExceptionOperation();
			if (a.size() == 0)
				return RealLongOperation.ZERO;
			Operation res = RealLongOperation.ZERO;
			for (int i=0;i<a.size();i++){
				Operation tmp0 = a.get(i), tmp1 = b.get(i);
				if (tmp0.isComplexFloatingNumber() && tmp1.isComplexFloatingNumber())
				{
					res = AdditionOperation.calculate(res, MultiplicationOperation.calculate(tmp0, tmp1, control), control);					
				}
				else
				{
					if (!(tmp0.isArray() || tmp1.isArray()))
						return new IllegalArgumentExceptionOperation();
					if (tmp0.size() != 1 || tmp1.size() != 1)
						return new ArrayIndexOutOfBoundsExceptionOperation();
					res = AdditionOperation.calculate(res, MultiplicationOperation.calculate(tmp0.get(0), tmp1.get(0), control), control);
				}
			}
			return res;
		}
		Operation erg = OperationCalculate.standardCalculations(a, b);
		if (erg != null)
			return erg;
		return new SkalarProductOpertion(a, b);
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
	public char getChar() {
		return Characters.MULT_SKAL;
	}
	
	@Override
	public Operation getInstance(List<Operation> subclasses) {
		return new SkalarProductOpertion(subclasses.get(0), subclasses.get(1));
	}
}
