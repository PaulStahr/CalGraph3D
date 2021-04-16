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
import maths.algorithm.OperationCalculate;
import maths.variable.VariableAmount;
import maths.data.ArrayOperation;
import maths.data.ComplexDoubleOperation;
import maths.data.ComplexLongOperation;
import maths.data.RealDoubleOperation;
import maths.data.RealLongOperation;

/** 
* @author  Paul Stahr
* @version 04.02.2012
*/
public class RoundOperation extends FunctionOperation
{
    private final Operation a;

    public RoundOperation (Operation a){
    	if ((this.a = a) == null)
    		throw new NullPointerException();
    }

    public static Operation calculate(final Operation a){
        if (a.isComplexIntegerNumber())
            return a;
        if (a.isRealRationalNumber()){
        	final long num = a.longNumeratorValue(), denum = a.longDenumeratorValue();
        	final long erg = num / denum;
        	if (num - erg * denum > (denum-1)/2)
        		return new RealLongOperation(erg+1);
        	return new RealLongOperation(erg);
        }
        if (a.isRealFloatingNumber()){
        	final double erg = Math.round(a.doubleValue());
        	if (Long.MIN_VALUE <= erg && erg <= Long.MAX_VALUE)
        		return new RealLongOperation((long)erg);
        	else
        		return new RealDoubleOperation(erg);
        }
        if (a.isComplexRationalNumber()){
        	final long numr = a.longNumeratorValue(), denumr = a.longDenumeratorValue();
        	final long numi = a.longNumeratorValueImag(), denumi = a.longDenumeratorValueImag();
        	long ergr = numr / denumr, ergi = numi / denumi;
        	if (numr - ergr * denumr > (denumr-1)/2)
        		ergr++;
        	if (numi - ergi * denumi > (denumi-1)/2)
        		ergi++;
        	return ComplexLongOperation.get(ergr, ergi);        	
        }
        if (a.isComplexFloatingNumber()){
        	final double ergR = Math.round(a.doubleValue()), ergI=Math.round(a.doubleValueImag());
        	if (Long.MIN_VALUE <= ergR && ergR <= Long.MAX_VALUE && Long.MIN_VALUE <= ergI && ergI <= Long.MAX_VALUE)
        		return ComplexLongOperation.get((long)ergR, (long)ergI);
        	else
        		return ComplexDoubleOperation.get(ergR, ergI);
        }
        if (a.isArray()){
        	return new ArrayOperation.ArrayCreator(a.size()){				
				@Override
				public final Operation get(int index) {
					return calculate(a.get(index));
				}
        	}.getArray();
        }
        Operation erg = OperationCalculate.standardCalculations(a);
        if (erg != null)
        	return erg;
        return new RoundOperation(a);
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
		return "round";
	}

	@Override
	public Operation getInstance(List<Operation> subclasses) {
		return new RoundOperation(subclasses.get(0));
	}
}
