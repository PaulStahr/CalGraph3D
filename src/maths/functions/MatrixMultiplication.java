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
import maths.algorithm.OperationCalculate;
import maths.variable.VariableAmount;
import maths.data.ArrayOperation;
import maths.data.Characters;
import maths.data.RealDoubleOperation;
import maths.functions.atomic.AdditionOperation;
import maths.functions.atomic.LinkingOperation;
import maths.functions.atomic.MultiplicationOperation;

/** 
* @author  Paul Stahr
* @version 04.02.2012
*/
public final class MatrixMultiplication extends LinkingOperation
{
    private final Operation a, b;

    public MatrixMultiplication (Operation a, Operation b){
    	if ((this.a = a) == null || (this.b = b) == null)
    		throw new NullPointerException();
    }

    public static Operation calculate (Operation a, Operation b, final CalculationController control){
        if (a.isArray() && b.isArray()){
		    final Operation am[][] = OperationCalculate.toOperationArray2(a), bm[][] = OperationCalculate.toOperationArray2(b);
		    if (am != null && bm != null){
		        final int height = am.length, width = bm[0].length, deth = bm.length;
		        if (deth == 0)
		        	return RealDoubleOperation.NaN;
		        return new ArrayOperation.MatrixCreator(width, height){
					@Override
					public Operation get(int x, int y) {
		                Operation elem = MultiplicationOperation.calculate(am[x][0], bm[0][y], control);
		                for (int k=1;k<deth;k++)
		                    elem = AdditionOperation.calculate(elem, MultiplicationOperation.calculate(am[x][k], bm[k][y], control), control);
		                return elem;
					}
		        }.getArray();
		    }
        }
        Operation erg = OperationCalculate.standardCalculations(a, b);
        if (erg != null)
        	return erg;
        return new MatrixMultiplication(a, b);
    }
    
	@Override
	public Operation calculate (VariableAmount object, CalculationController control){
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
	public int getPriority(){
        return 5;
    }

	
	@Override
	public char getChar() {
		return Characters.MULT_MAT;
	}

	@Override
	public Operation getInstance(List<Operation> subclasses) {
		return new MatrixMultiplication(subclasses.get(0), subclasses.get(1));
	}
}
