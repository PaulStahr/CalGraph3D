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
import maths.variable.VariableAmount;
import maths.algorithm.Calculate;
import maths.algorithm.OperationCalculate;
import maths.data.ArrayOperation;
import maths.data.Characters;
import maths.data.ComplexDoubleOperation;
import maths.data.ComplexLongOperation;
import maths.data.ComplexRationalOperation;
import maths.data.RealDoubleOperation;
import maths.data.RealLongOperation;
import maths.data.RealRationalOperation;
import maths.exception.ArrayIndexOutOfBoundsExceptionOperation;

/** 
* @author  Paul Stahr
* @version 04.02.2012
*/

public final class AdditionOperation extends LinkingOperation
{
    public final Operation a, b;

    public AdditionOperation (final Operation a, final Operation b){
    	if ((this.a = a) == null || (this.b = b) == null)
    		throw new NullPointerException();
    }

    public static final Operation calculate(final Operation a, final Operation b, final CalculationController control){
    	if (a.isRealIntegerNumber() && b.isRealIntegerNumber()){
            final long al = a.longValue(), bl = b.longValue(), erg = al + bl;
            if (Calculate.additionOverflowTest(al, bl, erg))
                return new RealLongOperation(erg);
            return new RealDoubleOperation((double)al + bl);
        }
        if (a.isRealRationalNumber() && b.isRealRationalNumber()){
        	final long an = a.longNumeratorValue(), ad = a.longDenumeratorValue();
        	final long bn = b.longNumeratorValue(), bd = b.longDenumeratorValue();
        	final long kgv = Calculate.kgvUnchecked(ad, bd);
        	if (kgv != -1){
	        	final long lmult = kgv/ad, rmult = kgv/bd;
	        	final long aen = an*lmult, ben = bn*rmult;
	        	if (aen/lmult == an && ben/rmult == bn){
	        		final long erg = aen + ben;
	                if (Calculate.additionOverflowTest(aen, ben, erg))
	                	return RealRationalOperation.getInstance(erg, kgv);
                    return new RealDoubleOperation(((double)aen + ben)/kgv);
	        	}
        	}
    		return new RealDoubleOperation((double)an/ad + (double)bn/bd);
        }
    	if (a.isRealFloatingNumber() && b.isRealFloatingNumber()){
            return new RealDoubleOperation(a.doubleValue() + b.doubleValue());
        }
        if (a.isComplexIntegerNumber() && b.isComplexIntegerNumber()){
            final long ar = a.longValue(), ai=a.longValueImag(), br = b.longValue(), bi=b.longValueImag();
            final long ergR = ar + br, ergI = ai + bi;
            if (Calculate.additionOverflowTest(ar, br, ergR) && Calculate.additionOverflowTest(ai, bi, ergI))
            	return ComplexLongOperation.get(ergR, ergI);
            return ComplexDoubleOperation.get((double)ar + br, (double)ai + bi);
        }
        if (a.isComplexRationalNumber() && b.isComplexRationalNumber()){
        	final long anr = a.longNumeratorValue(), adr = a.longDenumeratorValue(), ani = a.longNumeratorValueImag(), adi = a.longDenumeratorValueImag();
        	final long bnr = b.longNumeratorValue(), bdr = b.longDenumeratorValue(), bni = b.longNumeratorValueImag(), bdi = b.longDenumeratorValueImag();
        	final long kgvr = Calculate.kgvUnchecked(adr, bdr), kgvi = Calculate.kgvUnchecked(adi, bdi);
        	if (kgvr != -1 && kgvi != -1){
	        	final long lmultr = kgvr/adr, rmultr = kgvr/bdr, lmulti = kgvi/adi, rmulti = kgvi/bdi;
	        	final long aenr = anr*lmultr, benr = bnr*rmultr, aeni = ani*lmulti, beni = bni*rmulti;
	        	if (aenr/lmultr == anr && benr/rmultr == bnr && aeni/lmulti == ani && beni/rmulti == bni){
	        		final long ergr = aenr + benr, ergi = aeni + beni;
	                if (Calculate.additionOverflowTest(aenr, benr, ergr) && Calculate.additionOverflowTest(aeni, beni, ergi))
	                	return ComplexRationalOperation.getInstance(ergr, kgvr, ergi, kgvi);
                    return ComplexDoubleOperation.get(((double)aenr + benr)/kgvr, ((double)aeni + beni)/kgvi);
	        	}
        	}
    		return ComplexDoubleOperation.get((double)anr/adr + (double)bnr/bdr, (double)ani/adi + (double)bni/bdi);
        }
        if (a.isComplexFloatingNumber() && b.isComplexFloatingNumber()){
        	return ComplexDoubleOperation.get(a.doubleValue() + b.doubleValue(), a.doubleValueImag() + b.doubleValueImag());
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
        if (a.equals(b))
        	return MultiplicationOperation.calculate(a, RealLongOperation.POSITIVE_TWO, control);
        Operation erg = twoSideCalculate(a, b, control);
        if (erg != null || (erg = twoSideCalculate(b, a, control)) != null || (erg = OperationCalculate.standardCalculations(a, b))!=null)
        	return erg;
        if (a instanceof MultiplicationOperation && b instanceof MultiplicationOperation){
        	Operation a0 = a.get(0), a1 = a.get(1), b0 = b.get(0), b1 = b.get(1);
        	if (!(a0.isComplexFloatingNumber())){
        		Operation tmp = a0;
        		a0 = a1;
        		a1 = tmp;
        	}
        	if (!(b0.isComplexFloatingNumber())){
        		Operation tmp = b0;
        		b0 = b1;
        		b1 = tmp;
        	}
        	if (a0.isComplexFloatingNumber() && b0.isComplexFloatingNumber()){
        		if (a1.equals(b1)){
        			return MultiplicationOperation.calculate(calculate(a0, b0, control), b1, control);
        		}
        	}
        	
        }
        final OperationCalculate.OperationList up = control.getOperationList(), down = control.getOperationList();
        OperationCalculate.fillWithAddUpAndDowns(a, up, down);
        OperationCalculate.fillWithAddUpAndDowns(b, up, down);
        if (up.size()+down.size()>2)
	        return OperationCalculate.calculateBigSubtraction(up, down, control);
        if (control != null){
			control.returnToChached(up);
	        control.returnToChached(down);
        }
        return new AdditionOperation(a, b);
    }
    
    /*public static final Operation calculate(final Operation a, final Operation b, final CalculationController control){
    	int type = a.getTypeBitmask() & b.getTypeBitmask();
    	if ((type & BITMASK_INT_REAL) != 0){
            final long al = a.longValue(), bl = b.longValue(), erg = al + bl;
            if (Calculate.additionOverflowTest(al, bl, erg))
                return new RealLongOperation(erg);
            return new RealDoubleOperation((double)al + bl);
        }
    	if ((type & BITMASK_RATIONAL_REAL) != 0){
        	final long an = a.longNumeratorValue(), ad = a.longDenumeratorValue();
        	final long bn = b.longNumeratorValue(), bd = b.longDenumeratorValue();
        	final long kgv = Calculate.kgvUnchecked(ad, bd);
        	if (kgv != -1){
	        	final long lmult = kgv/ad, rmult = kgv/bd;
	        	final long aen = an*lmult, ben = bn*rmult;
	        	if (aen/lmult == an && ben/rmult == bn){
	        		final long erg = aen + ben;
	                if (Calculate.additionOverflowTest(aen, ben, erg))
	                	return RealRationalOperation.getInstance(erg, kgv);
                    return new RealDoubleOperation(((double)aen + ben)/kgv);
	        	}
        	}
    		return new RealDoubleOperation((double)an/ad + (double)bn/bd);
        }
    	if ((type & BITMASK_FLOAT_REAL) != 0){
            return new RealDoubleOperation(a.doubleValue() + b.doubleValue());
        }
    	if ((type & BITMASK_INT_COMPLEX) != 0){
            final long ar = a.longValue(), ai=a.longValueImag(), br = b.longValue(), bi=b.longValueImag();
            final long ergR = ar + br, ergI = ai + bi;
            if (Calculate.additionOverflowTest(ar, br, ergR) && Calculate.additionOverflowTest(ai, bi, ergI))
            	return ComplexLongOperation.get(ergR, ergI);
            return ComplexDoubleOperation.get((double)ar + br, (double)ai + bi);
        }
    	if ((type & BITMASK_RATIONAL_COMPLEX) != 0){
        	final long anr = a.longNumeratorValue(), adr = a.longDenumeratorValue(), ani = a.longNumeratorValueImag(), adi = a.longDenumeratorValueImag();
        	final long bnr = b.longNumeratorValue(), bdr = b.longDenumeratorValue(), bni = b.longNumeratorValueImag(), bdi = b.longDenumeratorValueImag();
        	final long kgvr = Calculate.kgvUnchecked(adr, bdr), kgvi = Calculate.kgvUnchecked(adi, bdi);
        	if (kgvr != -1 && kgvi != -1){
	        	final long lmultr = kgvr/adr, rmultr = kgvr/bdr, lmulti = kgvi/adi, rmulti = kgvi/bdi;
	        	final long aenr = anr*lmultr, benr = bnr*rmultr, aeni = ani*lmulti, beni = bni*rmulti;
	        	if (aenr/lmultr == anr && benr/rmultr == bnr && aeni/lmulti == ani && beni/rmulti == bni){
	        		final long ergr = aenr + benr, ergi = aeni + beni;
	                if (Calculate.additionOverflowTest(aenr, benr, ergr) && Calculate.additionOverflowTest(aeni, beni, ergi))
	                	return ComplexRationalOperation.getInstance(ergr, kgvr, ergi, kgvi);
                    return ComplexDoubleOperation.get(((double)aenr + benr)/kgvr, ((double)aeni + beni)/kgvi);
	        	}
        	}
    		return ComplexDoubleOperation.get((double)anr/adr + (double)bnr/bdr, (double)ani/adi + (double)bni/bdi);
        }
    	if ((type & BITMASK_FLOAT_COMPLEX) != 0){
        	return ComplexDoubleOperation.get(a.doubleValue() + b.doubleValue(), a.doubleValueImag() + b.doubleValueImag());
        }
        if (a.isArray() && b.isArray()){
        	if (a.subClassCount() != b.subClassCount())
        		return new ArrayIndexOutOfBoundsExceptionOperation();
        	return new ArrayOperation.ArrayCreator(a.subClassCount()){
				
				@Override
				public final Operation get(int index) {
					return calculate(a.get(index), b.get(index), control);
				}
        	}.getArray();
        }
        if (a.equals(b))
        	return MultiplicationOperation.calculate(a, RealLongOperation.POSITIVE_TWO, control);
        Operation erg = twoSideCalculate(a, b, control);
        if (erg != null || (erg = twoSideCalculate(b, a, control)) != null || (erg = OperationCalculate.standardCalculations(a, b))!=null)
        	return erg;
        if (a instanceof MultiplicationOperation && b instanceof MultiplicationOperation){
        	Operation a0 = a.get(0), a1 = a.get(1), b0 = b.get(0), b1 = b.get(1);
        	if (!(a0.isComplexFloatingNumber())){
        		Operation tmp = a0;
        		a0 = a1;
        		a1 = tmp;
        	}
        	if (!(b0.isComplexFloatingNumber())){
        		Operation tmp = b0;
        		b0 = b1;
        		b1 = tmp;
        	}
        	if (a0.isComplexFloatingNumber() && b0.isComplexFloatingNumber()){
        		if (a1.equals(b1)){
        			return MultiplicationOperation.calculate(calculate(a0, b0, control), b1, control);
        		}
        	}
        	
        }
        final OperationCalculate.OperationList up = control.getOperationList(), down = control.getOperationList();
        OperationCalculate.fillWithAddUpAndDowns(a, up, down);
        OperationCalculate.fillWithAddUpAndDowns(b, up, down);
        if (up.size()+down.size()>2)
	        return OperationCalculate.calculateBigSubtraction(up, down, control);
        if (control != null){
			control.returnToChached(up);
	        control.returnToChached(down);
        }
        return new AdditionOperation(a, b);
    }*/
    
    private static final Operation twoSideCalculate(final Operation a, final Operation b, final CalculationController control){
        if (a.isComplexFloatingNumber() && a.isZero())
            return b;
        if (a.isArray() && b.isComplexFloatingNumber()){
        	return new ArrayOperation.ArrayCreator(a.size()){
        		@Override
				public final Operation get(int index) {
					return calculate(a.get(index), b, control);
				}
        	}.getArray();
        }
    	if (a instanceof MultiplicationOperation){
    		Operation sub0 = a.get(0), sub1 = a.get(1);
    		if (sub0.equals(b))
    			return MultiplicationOperation.calculate(calculate(RealLongOperation.POSITIVE_ONE, sub1, control), sub0, control);
    		if (sub1.equals(b))
    			return MultiplicationOperation.calculate(calculate(RealLongOperation.POSITIVE_ONE, sub0, control), sub1, control);
        	if (sub0 instanceof NegativeOperation)
        		return SubtractionOperation.calculate(b, MultiplicationOperation.calculate(sub0.get(0), sub1, control), control);
        	if (sub1 instanceof NegativeOperation)
        		return SubtractionOperation.calculate(b, MultiplicationOperation.calculate(sub1.get(0), sub0, control), control);
    	}
        if (a.isRealFloatingNumber() && a.isNegative())
        	return SubtractionOperation.calculate(b,a.getNegative(), control);
        if (a instanceof NegativeOperation)
        	return SubtractionOperation.calculate(b, a.get(0), control);
    	return null;
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
	public final int getPriority(){
        return 4;
    }
	
	@Override
	public char getChar() {
		return Characters.ADD;
	}

	@Override
	public Operation getInstance(List<Operation> subclasses) {
		return new AdditionOperation(subclasses.get(0), subclasses.get(1));
	}
}
