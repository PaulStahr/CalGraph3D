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

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import maths.Operation;
import maths.algorithm.OperationCalculate;
import maths.data.BooleanOperation;
import maths.data.CharacterOperation;
import maths.exception.ExceptionOperation;
import maths.functions.FunctionOperation;
import maths.variable.VariableAmount;

public class WriteCsvOperation extends FunctionOperation {
	public final Operation a, b, csep, rsep;
	
	public WriteCsvOperation(Operation a, Operation b){
		this(a, b, CharacterOperation.getInstance(' '), CharacterOperation.getInstance('\n'));
	}

	public WriteCsvOperation(Operation a, Operation b, Operation csep, Operation rsep)
	{
		if ((this.a = a)==null || (this.b = b)==null || (this.csep = csep)==null || (this.rsep = rsep)==null)
			throw new NullPointerException();
	}
	
	public static final Operation calculate(Operation a, Operation b, Operation csep, Operation rsep){
		if (a.isString() && csep.isCharacter() && rsep.isCharacter()){
			final String path = a.stringValue();
			char csepChar = (char)csep.longValue();
			char rsepChar = (char)rsep.longValue();
			try {
				FileWriter fw = new FileWriter(path);
				BufferedWriter outBuf = new BufferedWriter(fw);
				StringBuilder strB = new StringBuilder();
				if (b.isArray())
				{
					for (int i = 0; i < b.size(); ++i)
					{
						Operation op = b.get(i);
						if (op.isArray())
						{
							if (op.size() != 0)
							{
								op.get(0).toString(strB);
								for (int j = 0; j < op.size(); ++j)
								{
									op.get(j).toString(strB.append(csepChar));
								}
							}
						}
						else
						{
							op.toString(strB);
						}
						strB.append(rsepChar);
						outBuf.append(strB);
						strB.setLength(0);
					}
				}
				else
				{
					b.toString(strB);
					outBuf.append(strB);
				}
				outBuf.close();
				fw.close();
				return BooleanOperation.TRUE;
			} catch (IOException e) {
				return new ExceptionOperation(e.getMessage());
			}
		}
		Operation erg = OperationCalculate.standardCalculations(a,b);
		if (erg != null)
			return erg;
		return new WriteCsvOperation(a, b);		
	}
	
	
	@Override
	public Operation calculate(VariableAmount object, CalculationController control) {
		return calculate(a.calculate(object, control), b.calculate(object, control), csep.calculate(object, control), rsep.calculate(object, control));
	}

	@Override
	public final int size() {
		return 4;
	}

	
	@Override
	public final Operation get(int index) {
		switch (index){
			case 0: return a;
			case 1: return b;
			case 2: return csep;
			case 3: return rsep;
			default:throw new ArrayIndexOutOfBoundsException(index);
		}
	}    
	
	@Override
	public String getFunctionName() {
		return "writecsv";
	}

	@Override
	public Operation getInstance(List<Operation> subclasses) {
		return new WriteCsvOperation(subclasses.get(0), subclasses.get(1), subclasses.get(2), subclasses.get(3));
	}
}
