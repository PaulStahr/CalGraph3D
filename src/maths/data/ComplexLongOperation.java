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
import maths.variable.VariableAmount;


public class ComplexLongOperation extends Operation{
	public static final ComplexLongOperation POSITIVE_ONE_I = new ComplexLongOperation(0,1);
	
	public final long real, imag;
	
	public static final Operation get(final long real, final long imaginary){
		return imaginary == 0 ? new RealLongOperation(real) : new ComplexLongOperation(real, imaginary);
	}
	
    private ComplexLongOperation (long real, long imaginary){
        this.real = real;
        this.imag = imaginary;
    }

    private ComplexLongOperation (String real, String imaginary){
    	if (real == null || imaginary == null)
    		throw new NullPointerException();
    	this.real = Long.parseLong (real);
    	this.imag = Long.parseLong(imaginary);
    }
 
    @Override
	public int getTypeBitmask(){
		return BITMASK_INT_COMPLEX | BITMASK_RATIONAL_COMPLEX | BITMASK_FLOAT_COMPLEX;
	}
    
	@Override
	public final boolean isComplexFloatingNumber(){
		return true;
	}
	
	@Override
	public final boolean isComplexRationalNumber(){
		return true;
	}
	
	@Override
	public final boolean isComplexIntegerNumber(){
		return true;
	}
	
	@Override
	public final double doubleValue(){
        return real;
    }

    
	@Override
	public final long longValue(){
        return real;
    }

    
	@Override
	public final double doubleValueImag(){
        return imag;
    }

    
	@Override
	public final long longValueImag(){
        return imag;
    }

    public long longNumeratorValue(){
        return real;
    }

    public long longDenumeratorValue(){
        return 1;
    }
    
    public long longNumeratorValueImag(){
        return imag;
    }

    public long longDenumeratorValueImag(){
        return 1;
    }
    
	@Override
	public final Operation calculate (final VariableAmount object, CalculationController control){
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
    	if (real == 0 && imag == 0)
    		return stringBuilder.append('0');
    	if (real != 0){
    		stringBuilder.append(real);
    		if (imag > 0)
    			stringBuilder.append(Characters.ADD);
    	}
    	if (imag == 1)
    		stringBuilder.append(Characters.I);
    	else if (imag == -1)
    		stringBuilder.append(Characters.SUB).append(Characters.I);
    	else if (imag != 0)
    		stringBuilder.append(imag).append(Characters.MULT).append(Characters.I);
    	return stringBuilder;
     }
    
    
	@Override
	public String toString(){
    	return Long.toString(real);
    }
    
	
	@Override
	public Operation getNegative() {
		return new ComplexLongOperation(-real, -imag);
	}

	public Operation getInvers(){
    	if (real < -Integer.MAX_VALUE || real > Integer.MAX_VALUE || imag < -Integer.MAX_VALUE || imag> Integer.MAX_VALUE){
        	final double mult = 1/((double)real*real+(double)imag*imag);
        	return ComplexDoubleOperation.get(real*mult, -imag*mult);	        		
    	}
    	final long qa = real*real, qi = imag*imag;
    	long divisor = qa+qi;
    	if (divisor >= qa)
        	return ComplexRationalOperation.getInstance(real, divisor, -imag, divisor);
		final double mult = 1/((double)qa+qi);
		return ComplexDoubleOperation.get(real*mult, -imag*mult);
	}
	
	@Override
	public final boolean isZero() {
		return real==0 && imag == 0;
	}

	
	@Override
	public final boolean isNaN() {
		return false;
	}
	
	@Override
	public final boolean isIntegral(){
		return true;
	}
	
	@Override
	public final boolean isPositive(){
		return real > 0;
	}
	
    
	@Override
	public final boolean isPrimitive(){
        return true;
    }
	
	@Override
	public final int getPriority(){
        return 4;
    }
	
	@Override
	public boolean equals(Object obj){
		if (!(obj instanceof Operation))
			return false;
		Operation op = (Operation)obj;
		if (op.isRealIntegerNumber())
			return op.longValue() == real;
		if (op.isRealFloatingNumber())
			return op.doubleValue() == real;
		if (op.isComplexIntegerNumber())
			return op.longValue() == real && op.longValueImag() == imag;
		if (op.isComplexFloatingNumber())
			return op.doubleValue() == real && op.doubleValueImag() == imag;
		return false;
	}
	
	@Override
	public Operation getInstance(List<Operation> subclasses) {
		return this;
	}
}
