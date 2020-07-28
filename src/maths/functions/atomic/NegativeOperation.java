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
package maths.functions.atomic;


import java.util.List;

import maths.Operation;
import maths.variable.VariableAmount;
import maths.algorithm.OperationCalculate;
import maths.data.ArrayOperation;
import maths.data.Characters;

/** 
* @author  Paul Stahr
* @version 04.02.2012
*/
public final class NegativeOperation extends Operation
{
    public final Operation a;

    public NegativeOperation (Operation a){
    	if ((this.a = a) == null)
    		throw new NullPointerException();
    }

    public static final Operation calculate (final Operation a, final CalculationController control){
    	if (a.isComplexFloatingNumber())
    		return a.getNegative();
    	switch(a.size()){
    		case 1:
    	        if (a instanceof NegativeOperation)
    	        	return a.get(0);
    	        break;
    		case 2:
	        	Operation sub0 = a.get(0), sub1 = a.get(1);
	        	if (a instanceof SubtractionOperation)
    	        	return SubtractionOperation.calculate(sub1, sub0, control);
    	        if (a instanceof MultiplicationOperation){
    	        	if (sub0 instanceof NegativeOperation)
    	        		return MultiplicationOperation.calculate(sub0.get(0), sub1, control);
    	        	if (sub1 instanceof NegativeOperation)
    	        		return MultiplicationOperation.calculate(sub1.get(0), sub0, control);
    	        }
    	        break;
    	}
        if (a.isArray()){
        	return new ArrayOperation.ArrayCreator(a.size()){
				@Override
				public final Operation get(int index) {
					return calculate(a.get(index), control);
				}
        	}.getArray();
        }
        final OperationCalculate.OperationList up = control.getOperationList(), down = control.getOperationList();
        OperationCalculate.fillWithAddUpAndDowns(a, up, down);
        if (up.size()+down.size()>2)
 	        return OperationCalculate.calculateBigSubtraction(up, down, control);
        if (control != null){
			control.returnToChached(up);
	        control.returnToChached(down);
        }
        return new NegativeOperation(a);
    }
    
	@Override
	public Operation calculate (VariableAmount object, CalculationController control){
        return calculate(a.calculate(object, control), control);
    }
   
	@Override
	public final StringBuilder toString(Print type, StringBuilder stringBuilder){
        if (a.getPriority()<=getPriority())
            return a.toString(type, stringBuilder.append(Characters.SUB).append('(')).append(')');
        else
            return a.toString(type, stringBuilder.append(Characters.SUB));
    }

	@Override
	public final int size() {
		return 1;
	}
	
	public final Operation getNegative(){
		return a;
	}

	@Override
	public final Operation get(int index) {
		switch (index){
			case 0: return a;
			default:throw new ArrayIndexOutOfBoundsException(index);
		}
	}
    
	@Override
	public int getPriority(){
        return 4;
    }
	
	
	@Override
	public Operation getInstance(List<Operation> subclasses) {
		return new NegativeOperation(subclasses.get(0));
	}
}
