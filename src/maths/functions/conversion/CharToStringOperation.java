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


import java.util.List;

import maths.Operation;
import maths.variable.VariableAmount;
import maths.data.StringOperation;
import maths.functions.FunctionOperation;

/** 
* @author  Paul Stahr
* @version 04.02.2012
*/

public class CharToStringOperation extends FunctionOperation
{
    private final Operation a;

    public CharToStringOperation (Operation a){
    	if ((this.a = a) == null)
    		throw new NullPointerException();
     }

    public static final Operation calculate(Operation a){
        if (a.isArray() && a.isPrimitive()){
            final char erg[] = new char[a.size()];
            for (int i=0;i<a.size();i++){
            	Operation elem = a.get(i);
                if (elem.isIntegral())
                    erg[i] = (char)elem.longValue();
                else
                    return new CharToStringOperation(a);
            }
            return new StringOperation(String.valueOf(erg));
        }

        return new CharToStringOperation(a);
    }        

    
	@Override
	public final Operation calculate (VariableAmount object, CalculationController control){
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
		return "chartostring";
	}	
	@Override
	public Operation getInstance(List<Operation> subclasses) {
		return new CharToStringOperation(subclasses.get(0));
	}
}
