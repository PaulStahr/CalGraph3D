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
import maths.data.ArrayOperation;
import maths.data.Characters;
import maths.exception.ExceptionOperation;

public class TransposeOperation extends Operation {
	private static final Operation ARRAY_OF_EMPTY_ARRAY[] = {ArrayOperation.EMPTY_ARRAY_OPERATION};
	final Operation a;
	
	public TransposeOperation(Operation a){
		if ((this.a = a) == null)
			throw new NullPointerException();
	}
	
	
	@Override
	public final Operation calculate(VariableAmount object, CalculationController control) {
		return calculate(a.calculate(object, control));
	}

	
	public static final Operation calculate(final Operation op){
		if (op.isArray()){
			int width = op.size();
			if (width == 0)
				return ArrayOperation.getInstance(ARRAY_OF_EMPTY_ARRAY);
			if (op.get(0).isArray()){
				int height = op.get(0).size();
				for (int i=0;i<op.size();i++)
					if (!(op.get(i).isArray()) || op.get(i).size() != height)
						return new ExceptionOperation("Not translatable");
				return new ArrayOperation.MatrixCreator(width, height){
					
					@Override
					public Operation get(int width, int height) {
						return op.get(height).get(width);
					}
				}.getArray();
			}
		}
		return new TransposeOperation(op);
	}
	
    
	@Override
	public final StringBuilder toString(Print type, StringBuilder stringBuilder){
        if (a.getPriority()<getPriority())
        	return a.toString(type, stringBuilder.append(Characters.HIGH_T).append('(')).append(')');
        else
            return a.toString(type, stringBuilder.append(Characters.HIGH_T));
    }

    
	@Override
	public int getPriority(){
        return 7;
    }

	
	@Override
	public int size() {
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
	public Operation getInstance(List<Operation> subclasses) {
		return new TransposeOperation(subclasses.get(0));
	}
}
