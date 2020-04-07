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
package maths.functions.conversion;


import java.util.AbstractList;
import java.util.List;

import maths.Operation;
import maths.VariableAmount;
import maths.algorithm.OperationCalculate;
import maths.data.ArrayOperation;
import maths.data.CharacterOperation;
import maths.functions.FunctionOperation;

/** 
* @author  Paul Stahr
* @version 04.02.2012
*/
public class ToCharacterOperation extends FunctionOperation
{
    public final Operation a;

    public ToCharacterOperation (Operation a){
    	if ((this.a = a) == null)
    		throw new NullPointerException();
    }

    public static Operation calculate(final Operation a){
        if (a instanceof CharacterOperation)
            return a;
        if (a.isRealFloatingNumber())
            return CharacterOperation.getInstance((char)a.longValue());
        if (a.isString()){
            final String str = a.stringValue();
            return new ArrayOperation(new AbstractList<Operation>() {
				@Override
				public int size() {
					return str.length();
				}
				
				@Override
				public Operation get(int index) {
					return CharacterOperation.getInstance(str.charAt(index));
				}
			});
        }
        if (a.isArray()){
        	return new ArrayOperation(new AbstractList<Operation>() {
				@Override
				public int size() {
					return a.size();
				}
				
				@Override
				public Operation get(int index) {
					return calculate(a.get(index));
				}
			});
        }
        Operation tmp = OperationCalculate.standardCalculations(a);
        if (tmp != null)
        	return tmp;
        return new ToCharacterOperation(a);
    }        

    
	@Override
	public Operation calculate (VariableAmount object, CalculationController control){
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
		return "char";
	}

	@Override
	public Operation getInstance(List<Operation> subclasses) {
		return new ToCharacterOperation(subclasses.get(0));
	}
}
