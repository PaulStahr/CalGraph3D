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
import maths.VariableAmount;
import maths.algorithm.Calculate;
import maths.algorithm.OperationCalculate;
import maths.data.ArrayOperation;
import maths.data.Characters;
import maths.data.ComplexDoubleOperation;
import maths.data.ComplexLongOperation;
import maths.data.RealDoubleOperation;
import maths.data.RealLongOperation;
import maths.data.RealRationalOperation;
import maths.exception.ArrayIndexOutOfBoundsExceptionOperation;

/** 
* @author  Paul Stahr
* @version 04.02.2012
*/
public class MultiplicationOperation extends LinkingOperation
{
    public final Operation a, b;

    public MultiplicationOperation (final Operation a, final Operation b){
    	if ((this.a = a) == null || (this.b = b) == null)
    		throw new NullPointerException();
    }

    public static final Operation calculate (final Operation a, final Operation b, final CalculationController control){
        if (a.isRealIntegerNumber() && b.isRealIntegerNumber()){
            final long al = a.longValue(), bl = b.longValue();
            if (bl == 0)
                return RealLongOperation.ZERO;
            final long erg = al * bl;
            return erg / bl == al ? new RealLongOperation(erg) : new RealDoubleOperation((double)al*bl);
        }
        if (a.isRealRationalNumber() && b.isRealRationalNumber()){
        	final long numerator = a.longNumeratorValue() * b.longNumeratorValue();
        	final long denumerator = a.longDenumeratorValue() * b.longDenumeratorValue();
        	if (denumerator == 0)
        		return RealDoubleOperation.NaN;
        	if (numerator == 0)
        		return RealLongOperation.ZERO;
        	if (numerator / a.longNumeratorValue() == b.longNumeratorValue() && denumerator / a.longDenumeratorValue() == b.longDenumeratorValue())
        		return RealRationalOperation.getInstance(numerator, denumerator);
        }
        if (a.isRealFloatingNumber() && b.isRealFloatingNumber()){
        	return new RealDoubleOperation(a.doubleValue()*b.doubleValue());
        }
        if (a.isComplexIntegerNumber() && b.isComplexIntegerNumber()){
            final long ar = a.longValue(), ai = a.longValueImag(), br = b.longValue(), bi = b.longValueImag();
            if (ar == 0){
            	if (ai == 0)
            		return RealLongOperation.ZERO;
              	final long r1 = ai * bi, i1 = ai * br;
              	if (r1 / ai == bi && i1 / ai == br)
              		return ComplexLongOperation.get(-r1, i1);
            }else{
            	final long r0 = ar * br, i0 = ar * bi;
              	if (r0 / ar == br && i0 / ar == bi){
	              	if (ai == 0){
	              		return ComplexLongOperation.get(-r0, i0);
	            	}else{
		            	final long r1 = ai * bi, i1 = ai * br;
		              	if (r1 / ai == bi && i1 / ai == br){
		                    final long real = r0-r1, imag = i0 + i1;
		                    if (Calculate.subtractionOverflowTest(r0, r1, real) && Calculate.additionOverflowTest(i0, i1, imag))
		                        return ComplexLongOperation.get(real, imag);
		              	}
	            	}
              	}
            }                  		
            return ComplexDoubleOperation.get((double) ar * br - (double)ai * bi,(double) ar * bi + (double)ai * br);
        }
        if (a.isComplexFloatingNumber() && b.isComplexFloatingNumber()){
            final double ar = a.doubleValue(), ai = a.doubleValueImag(), br = b.doubleValue(), bi = b.doubleValueImag();
            return ComplexDoubleOperation.get(ar * br - ai * bi, ar * bi + ai * br);
        }
        if (a.isArray() && b.isArray()){
        	if (a.size() != b.size())
        		return new ArrayIndexOutOfBoundsExceptionOperation();
           	return new ArrayOperation.ArrayCreator(a.size()){
   				@Override
				public final Operation get(int index) {
   					return calculate(a.get(index), b.get(index), control);
   				}
           	}.getArray();
        }
        Operation erg;
        
        if ((erg = twoSideCalculate(a, b, control)) != null || (erg = twoSideCalculate(b, a, control)) != null || (erg = OperationCalculate.standardCalculations(a, b))!=null)
        	return erg;
        if (a instanceof PowerOperation && b instanceof PowerOperation && a.get(0).equals(b.get(0)))
            return PowerOperation.getInstance(a.get(0), AdditionOperation.calculate(a.get(1), b.get(1), control));        		
        if (a.equals(b))
        	return PowerOperation.calculate(a, RealLongOperation.POSITIVE_TWO, control);
        
        final OperationCalculate.OperationList up = control.getOperationList(), down = control.getOperationList();
        OperationCalculate.fillWithMultUpAndDowns(a, up, down);
        OperationCalculate.fillWithMultUpAndDowns(b, up, down);
        if (up.size()+down.size()>2)
	        return OperationCalculate.calculateBigDivision(up, down, control);
        if (control != null){
			control.returnToChached(up);
	        control.returnToChached(down);
        }
        return new MultiplicationOperation(a, b);
    }

    
	@Override
	public Operation calculate (VariableAmount object, CalculationController control){
        return calculate(a.calculate(object, control), b.calculate(object, control), control);
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
    private static final Operation twoSideCalculate(final Operation a, final Operation b, final CalculationController control){
        if (a.isArray() && b.isComplexFloatingNumber()){
        	return new ArrayOperation.ArrayCreator(a.size()){
				@Override
				public final Operation get(int index) {
					return calculate(a.get(index), b, control);
				}
        	}.getArray();
        }
    	if (a.isComplexFloatingNumber() && a.isZero())
    		return RealLongOperation.ZERO;
    	if (a.isRealFloatingNumber()){
        	if (a.doubleValue() == 1)
        		return b;
        	if (a.doubleValue() == -1)
        		return NegativeOperation.calculate(b, control);
        	if (b instanceof NegativeOperation)
        		return calculate(a.getNegative(), b.get(0), control);
        }
        if (b instanceof PowerOperation && b.get(0).equals(a))
        	return PowerOperation.getInstance(a, AdditionOperation.calculate(RealLongOperation.POSITIVE_ONE, b.get(1), control));
        return null;
    }
    
    
	@Override
	public final StringBuilder toString (Print type, StringBuilder stringBuilder){
    	switch (type){
    		case CALGRAPH:{
    	        return super.toString(type, stringBuilder);	
    		}case LATEX:{
    	        if (a.getPriority()<getPriority())
    	        	a.toString(type, stringBuilder.append('(')).append(')');
    	        else
    	        	a.toString(type, stringBuilder);
    	        stringBuilder.append("\\cdot ");
    	        if (b.getPriority()<getPriority())
    	        	b.toString(type, stringBuilder.append('(')).append(')');
    	        else
    	        	b.toString(type, stringBuilder);
    	        return stringBuilder;      			    			
    		}case OPEN_OFFICE:{
    	        if (a.getPriority()<getPriority())
    	        	a.toString(type, stringBuilder.append('(')).append(')');
    	        else
    	        	a.toString(type, stringBuilder);
    	        stringBuilder.append(" cdot ");
    	        if (b.getPriority()<getPriority())
    	        	b.toString(type, stringBuilder.append('(')).append(')');
    	        else
    	        	b.toString(type, stringBuilder);
    	        return stringBuilder;      			    			   			
    		}default: throw new IllegalArgumentException();
    	}
    }

    
	@Override
	public final int getPriority(){
        return 5;
    }

	
	@Override
	public char getChar() {
		return Characters.MULT;
	}
	
	
	@Override
	public Operation getInstance(List<Operation> subclasses) {
		return new MultiplicationOperation(subclasses.get(0), subclasses.get(1));
	}
}
