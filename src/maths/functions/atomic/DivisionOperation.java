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
import maths.algorithm.OperationCalculate;
import maths.variable.VariableAmount;
import maths.data.ArrayOperation;
import maths.data.Characters;
import maths.data.ComplexDoubleOperation;
import maths.data.RealDoubleOperation;
import maths.data.RealLongOperation;
import maths.data.RealRationalOperation;
import maths.exception.ArrayIndexOutOfBoundsExceptionOperation;
import maths.functions.hyperbolic.CosinusOperation;
import maths.functions.hyperbolic.SinusOperation;
import maths.functions.hyperbolic.TangensOperation;

/** 
* @author  Paul Stahr
* @version 04.02.2012
*/
public final class DivisionOperation extends LinkingOperation
{
    public final Operation a, b;

    public DivisionOperation (Operation a, Operation b){
    	if ((this.a = a) == null || (this.b = b) == null)
    		throw new NullPointerException();
    }

    public static final Operation calculate (final Operation a, final Operation b, final CalculationController control){
        if (a.isRealIntegerNumber() && b.isRealIntegerNumber() && b.longValue() != 0)
            return RealRationalOperation.getInstance(a.longValue(),b.longValue());
        if (a.isRealRationalNumber() && b.isRealRationalNumber()){
        	final long numerator = a.longNumeratorValue() * b.longDenumeratorValue();
        	final long denumerator = a.longDenumeratorValue() * b.longNumeratorValue();
        	if (denumerator == 0)
        		return RealDoubleOperation.NaN;
        	if (numerator == 0)
        		return RealLongOperation.ZERO;
        	if (numerator / a.longNumeratorValue() == b.longDenumeratorValue() && denumerator / a.longDenumeratorValue() == b.longNumeratorValue())
        		return RealRationalOperation.getInstance(numerator, denumerator);
        }
        if (a.isRealFloatingNumber() && b.isRealFloatingNumber())
            return b.isZero() ? RealDoubleOperation.NaN : new RealDoubleOperation(a.doubleValue()/b.doubleValue());
        if (a.isComplexFloatingNumber() && b.isComplexFloatingNumber()){
        	final double ar = a.doubleValue(), ai = a.doubleValueImag(), br = b.doubleValue(), bi = b.doubleValueImag();
        	final double invers = 1/(br * br + bi * bi);
        	final double re = (ar * br + ai * bi)*invers, im = (ai * br - ar * bi)*invers;
        	if (Double.isNaN(re) || Double.isNaN(im))
        		return RealDoubleOperation.NaN;
        	return ComplexDoubleOperation.get(re, im);
        }
        if (b.isRealFloatingNumber()){
            if (b.doubleValue() == 1)
                return a;
            if (b.doubleValue() == -1)
                return new NegativeOperation(a);     	
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
        if (a.isArray() && b.isComplexFloatingNumber()){
        	return new ArrayOperation.ArrayCreator(a.size()){
				
				@Override
				public final Operation get(int index) {
					return calculate(a.get(index), b, control);
				}
        	}.getArray();
        }
        if (a.isComplexFloatingNumber() && b.isArray()){
        	return new ArrayOperation.ArrayCreator(b.size()){
				
				@Override
				public final Operation get(int index) {
					return calculate(a, b.get(index), control);
				}
        	}.getArray();
        }
        Operation erg = OperationCalculate.standardCalculations(a, b);
        if (erg != null)
        	return erg;
        if (a instanceof SinusOperation && b instanceof CosinusOperation && a.get(0).equals(b.get(0)))
        	return new TangensOperation(a.get(0));
        if (a.equals(b))
        	return RealLongOperation.POSITIVE_ONE;
        if (b instanceof PowerOperation && a.equals(b.get(0)))
        	return PowerOperation.getInstance(a, SubtractionOperation.calculate(RealLongOperation.POSITIVE_ONE, b.get(1), control));
        if (a instanceof PowerOperation){
            if (a.get(0).equals(b))
                return PowerOperation.getInstance(a.get(0), SubtractionOperation.calculate(a.get(1), RealLongOperation.POSITIVE_ONE, control));
            if (b instanceof PowerOperation && a.get(0).equals(b.get(0)))
            	return PowerOperation.getInstance(a.get(0), SubtractionOperation.calculate(a.get(1), b.get(1), control));        	
        }
        final OperationCalculate.OperationList up = control.getOperationList(), down = control.getOperationList();
        OperationCalculate.fillWithMultUpAndDowns(a, up, down);
        OperationCalculate.fillWithMultUpAndDowns(b, down, up);
        if (up.size()+down.size()>2)
	        return OperationCalculate.calculateBigDivision(up, down, control);
        if (control != null){
			control.returnToChached(up);
	        control.returnToChached(down);
        }
        return new DivisionOperation(a, b);
    }

    
	@Override
	public final Operation calculate (VariableAmount object, CalculationController control){
        return calculate(a.calculate(object, control), b.calculate(object, control), control);
    }

    
	@Override
	public final StringBuilder toString (final Print type, StringBuilder stringBuilder){
    	switch (type){
    		case CALGRAPH:{
    			return super.toString(type, stringBuilder);
    		}case LATEX:{
    			return b.toString(type, a.toString(type, stringBuilder.append("\\frac{")).append("}{")).append('}');
    		}case OPEN_OFFICE:{
    			return b.toString(type, a.toString(type, stringBuilder.append('{')).append("}over{")).append('}');
    		}default:throw new IllegalArgumentException();
    	}
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
	public boolean needClip(int index){
		switch(index){
			case 0:return a.getPriority()<getPriority();
			case 1:return b.getPriority()<=getPriority();
			default:throw new ArrayIndexOutOfBoundsException(index);
		}
    }
    
    
	@Override
	public final int getPriority(){
        return 5;
    }

	
	@Override
	public char getChar() {
		return Characters.DIV;
	}
	
	@Override
	public Operation getInstance(List<Operation> subclasses) {
		return new DivisionOperation(subclasses.get(0), subclasses.get(1));
	}

}
