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
package maths.functions.io;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import maths.Operation;
import maths.algorithm.OperationCalculate;
import maths.data.BooleanOperation;
import maths.data.RealDoubleOperation;
import maths.exception.ExceptionOperation;
import maths.functions.FunctionOperation;
import maths.variable.VariableAmount;

public class WriteOperation extends FunctionOperation {
	public final Operation a, b;
	
	public WriteOperation(Operation a, Operation b){
		if ((this.a = a)==null || (this.b = b)==null)
			throw new NullPointerException();
	}
	
	public static final Operation calculate(Operation a, Operation b){
		if (a.isString()){
			final String path = a.stringValue();
			final int index = path.lastIndexOf('.');
			if (index == -1)
				return new ExceptionOperation("You must define a file type");
			final String type = path.substring(index+1);
			if (type.equals("txt")){
				if (!(b.isString()))
					return b.isPrimitive() ? RealDoubleOperation.NaN : new WriteOperation(a, b);
				try {
					final FileWriter writer = new FileWriter(path);
					writer.write(b.stringValue());
					writer.close();
				} catch (IOException e) {
					return new ExceptionOperation(e.toString());
				}
				return BooleanOperation.TRUE;
			} else if (type.equals("jpg"))
			{
				//TODO
			}
		}
		Operation erg = OperationCalculate.standardCalculations(a,b);
		if (erg != null)
			return erg;
		return new WriteOperation(a, b);		
	}
	
	
	@Override
	public Operation calculate(VariableAmount object, CalculationController control) {
		return calculate(a.calculate(object, control), b.calculate(object, control));
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
		return "write";
	}

	@Override
	public Operation getInstance(List<Operation> subclasses) {
		return new WriteOperation(subclasses.get(0), subclasses.get(1));
	}
}
