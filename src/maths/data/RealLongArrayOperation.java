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
package maths.data;

import java.util.List;

import maths.MengenOperation;
import maths.Operation;
import maths.variable.VariableAmount;
import maths.functions.atomic.EqualsOperation;

public class RealLongArrayOperation extends MengenOperation {
	private final long data[];
	
	public RealLongArrayOperation(long init[])
	{
		data = init.clone();
	}
	
	public RealLongArrayOperation(List<Operation> subclasses)
	{
		data = new long[subclasses.size()];
		for (int i = 0;i<subclasses.size();++i)
		{
			data[i] = subclasses.get(i).longValue();
		}
	}
	
	@Override
	public Operation getInstance(List<Operation> subclasses) {
		return new RealLongArrayOperation(subclasses);
	}

	@Override
	public Operation calculate(VariableAmount object, CalculationController control) {
		return this;
	}

	@Override
	public StringBuilder toString(Print type, StringBuilder stringBuilder) {
		if (data.length == 0)
    		return stringBuilder.append('{').append('}');
        stringBuilder.append('{').append(String.valueOf(data[0]));
        for (int i=1;i<data.length;i++)
        	stringBuilder.append(',').append(String.valueOf(data[i]));
        return stringBuilder.append('}');
	}
	
	@Override
	public int size() {
		return data.length;
	}

	
	@Override
	public Operation get(int index) {
		return new RealLongOperation(data[index]);
	}
	
	
	@Override
	public boolean isPrimitive(){
		return true;
	}
	
	public boolean isArray()
	{
		return true;
	}

	@Override
	public final int isElementOf(Operation element){
        boolean isNaN = false;
        if (element.isIntegral())
        {
        	long value = element.longValue();
        	for (long elem : data){
        		if (elem == value)
        		{
        			return 1;
        		}  
        	}
        	return -1;
        }
        for (long unterklasse : data){
            final Operation erg = EqualsOperation.calculate(element, new RealLongOperation(unterklasse));
            if (!(erg.isBoolean()))
            	isNaN = true;
            else if (erg.booleanValue())
                return 1;
        }
        return isNaN ? 0 : -1;
    }
}
