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
import maths.algorithm.Calculate;


/** 
* @author  Paul Stahr
* @version 04.02.2012
*/
public final class ComplexRationalOperation extends Operation
{
    public final long numeratorReal, denumeratorReal, numeratorImag, denumeratorImag;

    public static final Operation getInstance(long numeratorReal, long denumeratorReal, long numeratorImag, long denumeratorImag){
    	if (numeratorImag == 0)
    		return RealRationalOperation.getInstance(numeratorReal, denumeratorReal);
    	if (denumeratorReal < 0){
    		denumeratorReal =- denumeratorReal;
    		numeratorReal =- numeratorReal;
    	}
    	if (denumeratorImag < 0){
    		denumeratorImag =- denumeratorImag;
    		numeratorImag =- numeratorImag;
    	}
    	if (numeratorReal == 0){
        	final long ggtImag = Calculate.ggtUnchecked(numeratorImag < 0 ? -numeratorImag : numeratorImag, denumeratorImag);
    		return ggtImag == denumeratorImag ? ComplexLongOperation.get(0, numeratorImag/ggtImag) : new ComplexRationalOperation(0, 1, numeratorImag/ggtImag, denumeratorImag/ggtImag);
    	}
    	final long ggtReal = Calculate.ggtUnchecked(numeratorReal < 0 ? -numeratorReal : numeratorReal, denumeratorReal);
    	final long ggtImag = Calculate.ggtUnchecked(numeratorImag < 0 ? -numeratorImag : numeratorImag, denumeratorImag);
    	if (ggtReal == denumeratorReal && ggtImag == denumeratorImag)
    		return ComplexLongOperation.get(numeratorReal/ggtReal, numeratorImag/ggtImag);
    	return new ComplexRationalOperation(numeratorReal/ggtReal, denumeratorReal/ggtReal, numeratorImag/ggtImag, denumeratorImag/ggtImag);
    }
    
    private ComplexRationalOperation (long numeratorReal, long denumeratorReal, long numeratorImag, long denumeratorImag){
    	this.numeratorReal = numeratorReal;
    	this.denumeratorReal = denumeratorReal;
    	this.numeratorImag = numeratorImag;
    	this.denumeratorImag = denumeratorImag;
    }
	
    @Override
	public int getTypeBitmask(){
		return BITMASK_RATIONAL_COMPLEX | BITMASK_FLOAT_COMPLEX;
	}
    
	@Override
	public final boolean isComplexFloatingNumber(){
		return true;
	}
	
	public boolean isComplexRationalNumber() {
		return true;
	}
	
	@Override
	public final double doubleValue(){
        return (double)numeratorReal/denumeratorReal;
    }
    
	@Override
	public final long longValue(){
        return numeratorReal/denumeratorReal;
    }

    public long longNumeratorValue(){
        return numeratorReal;
    }

    public long longDenumeratorValue(){
        return denumeratorReal;
    }
    
    public long longNumeratorValueImag(){
        return numeratorImag;
    }

    public long longDenumeratorValueImag(){
        return denumeratorImag;
    }
    
	@Override
	public final double doubleValueImag(){
        return (double)numeratorImag/denumeratorImag;
    }

	@Override
	public final long longValueImag(){
        return numeratorImag/denumeratorImag;
    }
    
	@Override
	public final ComplexRationalOperation calculate (VariableAmount object, CalculationController control){
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
	    		if (numeratorReal != 0){
	    			stringBuilder.append(numeratorReal).append('/').append(denumeratorReal);
	    			if (numeratorImag > 0)
		    			stringBuilder.append('+');
	    		}
	    		if (numeratorImag < 0)
	    			stringBuilder.append('-');
	    		return stringBuilder.append(Characters.I).append(Characters.MULT).append(Math.abs(numeratorImag)).append(Characters.DIV).append(denumeratorImag);
	    	}case LATEX:{
		         return stringBuilder.append("\\frac{").append(numeratorReal).append("}{").append(denumeratorReal).append('}');   		
	    	}default:{
	    		throw new IllegalArgumentException();
	    	}
    	}
    }    
    
	@Override
	public Operation getNegative() {
		return new ComplexRationalOperation(-numeratorReal,denumeratorReal, -denumeratorReal, denumeratorImag);
	}
	
	public final Operation getInvers(){
		if (numeratorReal > Integer.MAX_VALUE || numeratorReal < -Integer.MAX_VALUE || denumeratorReal > Integer.MAX_VALUE || denumeratorReal < -Integer.MAX_VALUE || numeratorImag > Integer.MAX_VALUE || numeratorImag < -Integer.MAX_VALUE || denumeratorImag > Integer.MAX_VALUE || denumeratorImag < -Integer.MAX_VALUE){
	    	final double real = (double)numeratorReal / denumeratorReal, imag = (double)numeratorImag / denumeratorImag; 
			final double mult = 1/(real*real+imag*imag);
	    	return ComplexDoubleOperation.get(real*mult, -imag*mult);			
		}
		final long nrq = numeratorReal * numeratorReal, drq = denumeratorReal * denumeratorReal, niq = numeratorImag * numeratorImag, diq = denumeratorImag * denumeratorImag, multR = numeratorReal * denumeratorReal, multI = numeratorImag * denumeratorImag;
		final long div1 = nrq * diq, div2 = drq * niq, numR = multR * diq, numI = multI * drq;
		if (div1 / nrq != diq || div2 / drq != niq || numR / multR != diq || numI / multI != drq){
			final double mult = 1/((double)nrq * diq+(double)drq * niq);
	    	return ComplexDoubleOperation.get((double)diq*multR*mult, (double)-drq*multI*mult);			
		}
		final long divisor = div1 + div2;
		if (Calculate.additionOverflowTest(div1, div2, divisor))
			return getInstance(numR, divisor, -numI, divisor);
		final double mult = 1/((double)div1 + div2);
		return ComplexDoubleOperation.get(numR*mult, -numI*mult);
	}
	
	@Override
	public boolean isZero() {
		return numeratorReal == 0.0;
	}

	@Override
	public boolean isIntegral(){
		return false;
	}
	
	@Override
	public final int getPriority(){
        return 4;
    }
	
	@Override
	public final boolean isPositive(){
		return numeratorReal > 0;
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
		if (op.isRealFloatingNumber())
			return false;
		if (op.isComplexRationalNumber())
			return op.longNumeratorValue() ==numeratorReal && op.longDenumeratorValue() == denumeratorReal && op.longNumeratorValueImag() == longNumeratorValueImag() && op.longDenumeratorValueImag() == longDenumeratorValueImag();
		return op.doubleValue() == (double)numeratorReal/denumeratorReal && op.doubleValueImag() == (double)numeratorImag/denumeratorImag;
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
