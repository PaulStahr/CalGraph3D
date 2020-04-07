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
public final class SubtractionOperation extends LinkingOperation
{
    public final Operation a, b;

    public SubtractionOperation (Operation a, Operation b){
    	if ((this.a = a) == null || (this.b = b) == null)
    		throw new NullPointerException();
    }

    public static final Operation calculate (final Operation a, final Operation b, final CalculationController control){
        if (a.isRealIntegerNumber() && b.isRealIntegerNumber()){
            final long al = a.longValue(), bl = b.longValue(), erg = al - bl;
            if (Calculate.subtractionOverflowTest(al, bl, erg))
                return new RealLongOperation (erg);
            return new RealDoubleOperation((double)al - bl);
        }
        if (a.isRealRationalNumber() && b.isRealRationalNumber()){
        	final long an = a.longNumeratorValue(), ad = a.longDenumeratorValue();
        	final long bn = b.longNumeratorValue(), bd = b.longDenumeratorValue();
        	final long kgv = Calculate.kgv(ad, bd);
           	if (kgv != -1){
	        	final long lmult = kgv/ad, rmult = kgv/bd;
	        	final long aen = an*lmult, ben = bn*rmult;
	        	if (aen/lmult == an || ben/rmult == bn){
	        		final long erg = aen - ben;
	                if (Calculate.subtractionOverflowTest(aen, ben, erg))
	                	return RealRationalOperation.getInstance(erg, kgv);
                    return new RealDoubleOperation(((double)aen - ben)/kgv);
	        	}
        	}
    		return new RealDoubleOperation((double)an/ad - (double)bn/bd);
        }
        if (a.isRealFloatingNumber() && b.isRealFloatingNumber())
            return new RealDoubleOperation(a.doubleValue() - b.doubleValue());
        if (a.isComplexIntegerNumber() && b.isComplexIntegerNumber()){
            final long ar = a.longValue(), ai=a.longValueImag(), br = b.longValue(), bi=b.longValueImag();
            final long realErg = ar-br, imagErg = ai-bi;
            if (Calculate.subtractionOverflowTest(ar, br, realErg) && Calculate.subtractionOverflowTest(ai, bi, imagErg))
            	return ComplexLongOperation.get(realErg, imagErg);
            return ComplexDoubleOperation.get(ar-br,ai-bi);
        }
        if (a.isComplexFloatingNumber() && b.isComplexFloatingNumber()){
        	return ComplexDoubleOperation.get(a.doubleValue() - b.doubleValue(), a.doubleValueImag() - b.doubleValueImag());
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
        if (b.isComplexFloatingNumber()){
        	if (b.isZero())
        		return a;
        	if (b.isRealFloatingNumber() && b.isNegative())
        		return AdditionOperation.calculate(a, b.getNegative(), control);
        }
        if (a.isComplexFloatingNumber()){
        	if (a.isZero())
                return NegativeOperation.calculate(b, control);
        }
        if (b instanceof MultiplicationOperation){
        	Operation sub0 = b.get(0), sub1 = b.get(1);
        	if (sub0 instanceof NegativeOperation)
        		return AdditionOperation.calculate(a, MultiplicationOperation.calculate(sub0.get(0), sub1, control), control);
        	if (sub1 instanceof NegativeOperation)
        		return AdditionOperation.calculate(a, MultiplicationOperation.calculate(sub1.get(0), sub0, control), control);
        }
        if (a.equals(b))
        	return RealLongOperation.ZERO;
        Operation erg = OperationCalculate.standardCalculations(a, b);
        if (erg != null)
        	return erg;
        final OperationCalculate.OperationList up = control.getOperationList(), down = control.getOperationList();
        OperationCalculate.fillWithAddUpAndDowns(a, up, down);
        OperationCalculate.fillWithAddUpAndDowns(b, down, up);
        if (up.size()+down.size()>2 || down.size()==2 || up.size() == 2)
        	return OperationCalculate.calculateBigSubtraction(up, down, control);
        if (control != null){
			control.returnToChached(up);
	        control.returnToChached(down);
        }
        return new SubtractionOperation(a, b);
    }

    
	@Override
	public final Operation calculate (final VariableAmount object, CalculationController control){
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
        return 4;
    }

	
	@Override
	public char getChar() {
		return Characters.SUB;
	}
	
	@Override
	public Operation getInstance(List<Operation> subclasses) {
		return new SubtractionOperation(subclasses.get(0), subclasses.get(1));
	}
}
