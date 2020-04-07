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
import maths.algorithm.Calculate;
import maths.algorithm.OperationCalculate;
import maths.data.RealDoubleOperation;
import maths.data.RealLongOperation;

/** 
* @author  Paul Stahr
* @version 04.02.2012
*/
public final class NprOperation extends FunctionOperation
{
    public final Operation a, b;

    public NprOperation (Operation a, Operation b){
    	if ((this.a = a) == null || (this.b = b) == null)
    		throw new NullPointerException();
    }

    public static Operation calculate(Operation a, Operation b){
        if (a.isComplexFloatingNumber() && b.isComplexFloatingNumber()){
        	if (!(a.isIntegral() && b.isIntegral()))
        		return new RealDoubleOperation(Double.NaN);
        	final long erg = Calculate.npr((int)a.longValue(), (int)b.longValue());
            return erg >= 0 ? new RealLongOperation(erg) : new RealDoubleOperation(Calculate.nprd((int)a.longValue(), (int)b.longValue()));
        }
       	Operation erg = OperationCalculate.standardCalculations(a, b);
    	if (erg != null)
    		return erg;
        return new NprOperation(a, b);
    }
    
    
	@Override
	public Operation calculate (VariableAmount object, CalculationController control){
        return calculate (a.calculate(object, control), b.calculate(object, control));
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
		return "npr";
	}

	@Override
	public Operation getInstance(List<Operation> subclasses) {
		return new NprOperation(subclasses.get(0), subclasses.get(1));
	}
	}
