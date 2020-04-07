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

import maths.MengenOperation;
import maths.Operation;
import maths.VariableAmount;
import maths.data.BooleanOperation;
import maths.data.Characters;

/** 
* @author  Paul Stahr
* @version 04.02.2012
*/
public final class IsSubsetOperation extends LinkingOperation
{
    public final Operation a, b;

    public IsSubsetOperation (Operation a, Operation b){
    	if ((this.a = a) == null || (this.b = b) == null)
    		throw new NullPointerException();
    }

    public static Operation calculate(Operation a, Operation b){
    	if (a instanceof MengenOperation && b instanceof MengenOperation){
    		final int aCount, bCount;
    		if (a == MengenOperation.P)		aCount = 0;
    		else if (a == MengenOperation.N)aCount = 1;
    		else if (a == MengenOperation.Z)aCount = 2;
    		else if (a == MengenOperation.Q)aCount = 3;
    		else if (a == MengenOperation.R)aCount = 4;
    		else if (a == MengenOperation.C)aCount = 5;
    		else							aCount = -1;
    		if (b == MengenOperation.P)		bCount = 0;
    		else if (b == MengenOperation.N)bCount = 1;
    		else if (b == MengenOperation.Z)bCount = 2;
    		else if (b == MengenOperation.Q)bCount = 3;
    		else if (b == MengenOperation.R)bCount = 4;
    		else if (b == MengenOperation.C)bCount = 5;
    		else							bCount = -1;
    		if (aCount != -1 && bCount != -1)
    			return BooleanOperation.get(aCount <= bCount);
    		
    	}
        return new IsSubsetOperation(a,b);
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
	public int getPriority(){
        return 2;
    }

	
	@Override
	public char getChar() {
		return Characters.SUBSET;
	}
	
	
	@Override
	public Operation getInstance(List<Operation> subclasses) {
		return new IsSubsetOperation(subclasses.get(0), subclasses.get(1));
	}
}
