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
package maths.functions.interators;


import java.util.List;

import maths.Operation;
import maths.variable.VariableAmount;
import maths.algorithm.OperationCalculate;
import maths.algorithm.OperationIterator;
import maths.data.RealLongOperation;
import maths.functions.FunctionOperation;
import maths.functions.atomic.AdditionOperation;

public class SumIteratorOperation extends FunctionOperation {
	public final Operation a, b;
	
	public SumIteratorOperation(Operation a, Operation b){
		if ((this.a = a) == null || (this.b = b) == null)
			throw new NullPointerException();
	}
	
	public static final Operation calculate(final Operation a, Operation b, VariableAmount va, final CalculationController control){
		Operation erg = OperationCalculate.standardCalculations(a, b);
		if (erg != null)
			return erg;
		if (control == null || control.calculateLoop()){
			OperationIterator oi = new OperationIterator(b){
				private Operation erg = RealLongOperation.ZERO;
				@Override
				protected void calculate(VariableAmount va, long i) {
					erg = AdditionOperation.calculate(erg,a.calculate(va, control), control);
				}
	
				@Override
				public Operation getErg() {
					return erg;
				}
			};
			if (!oi.isValid())
				return new ProductIteratorOperation(a, b);
			oi.run(va);
			return oi.getErg();
		}
		return new SumIteratorOperation(a, b);
	}
	
	
	@Override
	public final Operation calculate(VariableAmount object, CalculationController control) {
		return calculate(a.calculate(object, control), b.calculate(object, control), object, control);
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
	public final StringBuilder toString (Print type, StringBuilder stringBuilder){
    	switch (type){
    		case CALGRAPH:
    		case OPEN_OFFICE:{
    	        return super.toString(type, stringBuilder);	
    		//}case LATEX:{
    			//return c.toString(type, b.toString(type, a.toString(type, stringBuilder.append("\\sum\\limits_{")).append("}^{")).append("}{")).append('}');
    		}default: throw new IllegalArgumentException();
    	}
    }

    
	@Override
	public String getFunctionName() {
		return "sum";
	}

	@Override
	public Operation getInstance(List<Operation> subclasses) {
		return new SumIteratorOperation(subclasses.get(0), subclasses.get(1));
	}
}
