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
import maths.algorithm.OperationCalculate;

/** 
* @author  Paul Stahr
* @version 04.02.2012
*/

public final class IfOperation extends FunctionOperation
{
    public final Operation a,b,c;

    public IfOperation (Operation a, Operation b, Operation c){
    	if ((this.a = a) == null || (this.b = b) == null || (this.c = c) == null)
    		throw new NullPointerException();
     }

    
	@Override
	public final Operation calculate (VariableAmount object, CalculationController control){
        final Operation a = this.a.calculate(object, control);
        if (a.isBoolean())
            return (a.booleanValue() ? b : c).calculate(object, control);
        Operation erg = OperationCalculate.standardCalculations(a);
        if (erg != null)
        	return erg;
        return new IfOperation (a, b.calculate(object, control), c.calculate(object, control));
    }

   	@Override
	public String getFunctionName() {
		return "if";
	}
	
    
	@Override
	public final int size(){
    	return 3;
    }
    
    
	@Override
	public final Operation get(int index){
    	switch (index){
    		case 0:return a;
    		case 1:return b;
    		case 2:return c;
    		default: throw new ArrayIndexOutOfBoundsException(index);
    	}
    }

	@Override
	public Operation getInstance(List<Operation> subclasses) {
		return new IfOperation(subclasses.get(0), subclasses.get(1), subclasses.get(2));
	}
}
