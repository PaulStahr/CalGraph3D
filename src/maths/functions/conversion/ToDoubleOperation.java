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


import java.util.ArrayList;
import java.util.List;

import maths.Operation;
import maths.VariableAmount;
import maths.algorithm.OperationCalculate;
import maths.data.ArrayOperation;
import maths.data.ComplexDoubleOperation;
import maths.data.RealDoubleOperation;
import maths.data.RealLongOperation;
import maths.exception.ExceptionOperation;
import maths.functions.FunctionOperation;
import util.StringUtils;

/** 
* @author  Paul Stahr
* @version 04.02.2012
*/
public final class ToDoubleOperation extends FunctionOperation
{
    private final Operation a;

    public ToDoubleOperation (Operation a){
    	if ((this.a = a) == null)
    		throw new NullPointerException();
    }

    public static Operation calculate(final Operation a){
        if (a.isRealRationalNumber())
            return new RealDoubleOperation(a.doubleValue());
        if (a.isComplexRationalNumber())
            return ComplexDoubleOperation.get(a.doubleValue(), a.doubleValueImag());
        if (a.isComplexFloatingNumber())
            return a;
        if (a.isString())
        {
        	String value = a.stringValue();
        	if (value.indexOf('\n') >= 0)
    		{
        		ArrayList<String> lines = new ArrayList<>();
        		ArrayList<String> parts = new ArrayList<>();
        		StringUtils.split(value, 0, value.length(), '\n', false, lines);
        		ArrayOperation result = new ArrayOperation(lines.size(), RealLongOperation.ZERO);
        		for (int i = 0; i < lines.size(); ++i)
        		{//float(read("/media/paul/Data1/Caesar/MouseEyeModel/Models/FilesForPaul/left_eye.txt"))
        			String line = lines.get(i);
        			if (line.indexOf(' ') >= 0)
        			{
        				StringUtils.split(line, 0, line.length(), ' ', false, lines);
        				ArrayOperation opLine = new ArrayOperation(parts.size(), RealLongOperation.ZERO);
        				for (int j = 0; j < parts.size(); ++j)
        				{
        					opLine.set(j, new RealDoubleOperation(Double.parseDouble(parts.get(j))));
        				}
        				result.set(i, opLine);
        			}
        			else if (line.indexOf('\t') >= 0)
        			{
        				StringUtils.split(line, 0, line.length(), '\t', false, parts);
        				ArrayOperation opLine = new ArrayOperation(parts.size(), RealLongOperation.ZERO);
        				for (int j = 0; j < parts.size(); ++j)
        				{
        					opLine.set(j, new RealDoubleOperation(Double.parseDouble(parts.get(j))));
        				}
        				result.set(i, opLine);    				
        			}
        			else
        			{
        				result.set(i, new ArrayOperation(1, new RealDoubleOperation(Double.parseDouble(line))));
        			}
        			parts.clear();
        		}
        		return result;
    		}
            try{
                return new RealDoubleOperation(Double.parseDouble(value));
            }catch(Exception e){
            	return new ExceptionOperation("String-format not parsable");
            }
        }
        if (a.isArray()){
        	return new ArrayOperation.ArrayCreator(a.size()){
				
				@Override
				public final Operation get(int index) {
					return calculate(a.get(index));
				}
        	}.getArray();
        }
        Operation tmp = OperationCalculate.standardCalculations(a);
        if (tmp != null)
        	return tmp;
        return new ToDoubleOperation(a);
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
		return "float";
	}

	@Override
	public Operation getInstance(List<Operation> subclasses) {
		return new ToDoubleOperation(subclasses.get(0));
	}
}
