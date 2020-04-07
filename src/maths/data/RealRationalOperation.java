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

import maths.Operation;
import maths.VariableAmount;
import maths.algorithm.Calculate;


/** 
* @author  Paul Stahr
* @version 04.02.2012
*/
public final class RealRationalOperation extends Operation
{
	public static final Operation POSITIVE_HALF = RealRationalOperation.getInstance(1,2);
	public static final Operation POSITIVE_THIRDS = RealRationalOperation.getInstance(1, 3);
	public final long numerator, denumerator;

    public static final Operation getInstance(long numerator, long denumerator){
    	if (denumerator < 0){
    		denumerator =- denumerator;
    		numerator =- numerator;
    	}
    	if (numerator == 0)
    		return RealLongOperation.ZERO;
    	final long ggt = Calculate.ggtUnchecked(numerator < 0 ? -numerator : numerator, denumerator);
    	if (ggt == denumerator)
    		return new RealLongOperation(numerator/ggt);
    	return new RealRationalOperation(numerator/ggt, denumerator/ggt);
    }
    
    public static final Operation getInvers(long l){
    	if (l>=0){
        	if (l==0)
        		return RealDoubleOperation.NaN;
        	if (l == 1)
        		return RealLongOperation.POSITIVE_ONE;
        	return new RealRationalOperation(1, l);
    	}else{
        	if (l == -1)
        		return RealLongOperation.NEGATIVE_ONE;
        	return new RealRationalOperation(-1, -l);
    	}
    }
    
    private RealRationalOperation (long numerator, long denumerator){
    	this.numerator = numerator;
    	this.denumerator = denumerator;
    }

    @Override
	public int getTypeBitmask(){
		return BITMASK_RATIONAL_REAL | BITMASK_FLOAT_REAL | BITMASK_RATIONAL_COMPLEX | BITMASK_FLOAT_COMPLEX;
	}
    
	@Override
	public final boolean isRealRationalNumber(){
		return true;
	}
	
	@Override
	public final boolean isRealFloatingNumber(){
		return true;
	}
	
	@Override
	public final boolean isComplexRationalNumber(){
		return true;
	}
	
	@Override
	public final boolean isComplexFloatingNumber(){
		return true;
	}
	
	@Override
	public final double doubleValue(){
        return (double)numerator/denumerator;
    }
    
	@Override
	public final long longValue(){
        return numerator/denumerator;
    }

    public final long longNumeratorValue(){
        return numerator;
    }

    public final long longDenumeratorValue(){
        return denumerator;
    }
    
    
    public final long longNumeratorValueImag(){
        return 0;
    }

    public final long longDenumeratorValueImag(){
        return 1;
    }

	@Override
	public final double doubleValueImag(){
        return 0;
    }
    
	@Override
	public final long longValueImag(){
        return 0;
    }
    
	@Override
	public final RealRationalOperation calculate (VariableAmount object, CalculationController control){
        return this;
    }      
	
	@Override
	public final int size() {
		return 0;
	}

	@Override
	public final Operation get(int index) {
		throw new ArrayIndexOutOfBoundsException(index);
	}
    
	@Override
	public final StringBuilder toString(Print type, StringBuilder stringBuilder){
    	switch(type){
	    	case CALGRAPH:
	    	case OPEN_OFFICE:{
	    		return stringBuilder.append(numerator).append('/').append(denumerator);
	    	}case LATEX:{
		         return stringBuilder.append("\\frac{").append(numerator).append("}{").append(denumerator).append('}');   		
	    	}default:{
	    		throw new IllegalArgumentException();
	    	}
    	}
    }    
    
	@Override
	public Operation getNegative() {
		return new RealRationalOperation(-numerator,denumerator);
	}

	public Operation getInvers(){
		if (numerator >= 0){
			if (numerator == 0)
				return RealDoubleOperation.NaN;
			if (numerator == 1)
				return new RealLongOperation(denumerator);
			return new RealRationalOperation(denumerator, numerator);
		}else{
			if (numerator == -1)
				return new RealLongOperation(-denumerator);
			return new RealRationalOperation(-denumerator, -numerator);
		}
	}
	
	@Override
	public boolean isZero() {
		return numerator == 0;
	}

	@Override
	public boolean isIntegral(){
		return false;
	}
	
	@Override
	public final boolean isPositive(){
		return numerator > 0;
	}
    
	@Override
	public final boolean isNegative(){
		return numerator < 0;
	}
    
	@Override
	public boolean isPrimitive(){
        return true;
    }
	
	@Override
	public boolean equals(Object obj){
		if (!(obj instanceof Operation))
			return false;
		Operation op = (Operation)obj;
		return op.doubleValue() == (double)numerator/denumerator && op.doubleValueImag() == 0;
	}

	@Override
	public boolean isNaN() {
		return false;
	}

	@Override
	public Operation getInstance(List<Operation> subclasses) {
		return this;
	}
}
