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


/** 
* @author  Paul Stahr
* @version 04.02.2012
*/
public final class ComplexDoubleOperation extends Operation
{
    public final double real, imag;

    public static final Operation get(final double real, final double imaginary){
    	return imaginary == 0 ? new RealDoubleOperation(real) : new ComplexDoubleOperation(real, imaginary);
    }
    
    private ComplexDoubleOperation (double real, double imaginary){
        this.real = real;
        this.imag = imaginary;
    }
    

    @Override
	public int getTypeBitmask(){
		return BITMASK_FLOAT_COMPLEX;
	}
    
	@Override
	public final boolean isComplexFloatingNumber(){
		return true;
	}
	
	@Override
	public final double doubleValue(){
        return real;
    }
    
	@Override
	public final long longValue(){
        return (long)real;
    }   
	
	@Override
	public final long longValueImag() {
		return (long)imag;
	}
	
	@Override
	public final double doubleValueImag() {
		return imag;
	}

    
	@Override
	public final Operation calculate (VariableAmount object, CalculationController control){
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
        if (Double.isNaN(real) || Double.isNaN(imag))
        	return stringBuilder.append("undef");
        if (real == 0 && imag == 0)
        	return stringBuilder.append("0.0");
        switch(type){
	    	case CALGRAPH:
	    	case OPEN_OFFICE:{
	    		if (real != 0){
	    			(new RealDoubleOperation(real)).toString(stringBuilder);
	    			if (imag > 0)
	    				stringBuilder.append(Characters.ADD);
	    		}
	    		if (imag != 0){
	    			if (imag == 1)
	    				stringBuilder.append(Characters.I);
	    			else if (imag == -1)
	    				stringBuilder.append(Characters.SUB).append(Characters.I);
	    			else 
	    				(new RealDoubleOperation(imag)).toString(stringBuilder).append(Characters.MULT).append(Characters.I);
	    		}
		        return stringBuilder;
	    	}case LATEX:{
		        if (real == Double.POSITIVE_INFINITY)
		            return stringBuilder.append("\\infty "); 
		        if (real == Double.NEGATIVE_INFINITY)
		            return stringBuilder.append(Characters.SUB).append("\\infty ");
		        if (real == Math.PI)
		            return stringBuilder.append("\\pi");
		        if (real == Math.E)
		            return stringBuilder.append(Characters.EULER);
		        return stringBuilder.append(real);	    		
	    	}default:{
	    		throw new IllegalArgumentException();
	    	}
    	}
    }

	
	@Override
	public final Operation getNegative() {
		return new ComplexDoubleOperation(-real, -imag);
	}

	public final Operation getInvers(){
    	final double mult = 1/(real*real+imag*imag);
    	return ComplexDoubleOperation.get(real*mult, -imag*mult);
	}
	
	@Override
	public final boolean isZero() {
		return real == 0.0 && imag == 0.0;
	}

	
	@Override
	public final boolean isNaN() {
		return Double.isNaN(real) && Double.isNaN(imag);
	}
	
	@Override
	public final boolean isIntegral(){
		return real%1==0 && imag%1==0;
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
	public final boolean equals(Object obj){
		if (!(obj instanceof Operation))
			return false;
		Operation op = (Operation)obj;
		return op.doubleValue() == real && op.doubleValueImag() == imag;
	}

	@Override
	public Operation getInstance(List<Operation> subclasses) {
		return this;
	}

}
