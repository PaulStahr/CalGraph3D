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


/** 
* @author  Paul Stahr
* @version 04.02.2012
*/
public final class RealDoubleOperation extends Operation
{
    public static final RealDoubleOperation MAX_VALUE        	= new RealDoubleOperation(Double.MAX_VALUE);
    public static final RealDoubleOperation MIN_VALUE        	= new RealDoubleOperation(Double.MIN_VALUE);
    public static final RealDoubleOperation POSITIVE_INFINITY	= new RealDoubleOperation(Double.POSITIVE_INFINITY);
    public static final RealDoubleOperation NEGATIVE_INFINITY	= new RealDoubleOperation(Double.NEGATIVE_INFINITY);
    public static final RealDoubleOperation NaN              	= new RealDoubleOperation(Double.NaN);
    public static final RealDoubleOperation ZERO             	= new RealDoubleOperation(0.0);
    public static final RealDoubleOperation PI               	= new RealDoubleOperation(Math.PI);
    public static final RealDoubleOperation E                	= new RealDoubleOperation(Math.E);
	public static final RealDoubleOperation POSITIVE_ONE		= new RealDoubleOperation(1);
    public final double value;

    public RealDoubleOperation (double value){
        this.value = value;
    }

    @Override
	public final int getTypeBitmask(){
		return BITMASK_FLOAT_REAL | BITMASK_FLOAT_COMPLEX;
	}
    
	@Override
	public final boolean isRealFloatingNumber(){
		return true;
	}

	
	@Override
	public final boolean isComplexFloatingNumber(){
		return true;
	}
	
	@Override
	public final double doubleValue(){
        return value;
    }
	 
	@Override
	public final long longValue(){
        return (long)value;
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
	public final RealDoubleOperation calculate (VariableAmount object, CalculationController control){
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
	    		if (value > 0){
			        if (value == Double.POSITIVE_INFINITY)
			            return stringBuilder.append(Characters.INFTY);
			        if (value == Math.PI)
			            return stringBuilder.append(Characters.PI);
			        if (value == Math.E)
			            return stringBuilder.append(Characters.EULER);
	    		}else{
			        if (value == Double.NEGATIVE_INFINITY)
			            return stringBuilder.append(Characters.SUB).append(Characters.INFTY);
			        if (Double.isNaN(value))
			        	return stringBuilder.append("undef");
	    		}
		        return stringBuilder.append(value);
	    	}case LATEX:{
		        if (value == Double.POSITIVE_INFINITY)
		            return stringBuilder.append("\\infty "); 
		        if (value == Double.NEGATIVE_INFINITY)
		            return stringBuilder.append(Characters.SUB).append("\\infty ");
		        if (value == Math.PI)
		            return stringBuilder.append("\\pi");
		        if (value == Math.E)
		            return stringBuilder.append(Characters.EULER);
		        if (Double.isNaN(value))
		        	return stringBuilder.append("undef");
		        return stringBuilder.append(value);	    		
	    	}default:{
	    		throw new IllegalArgumentException();
	    	}
    	}
    }
    
    
	@Override
	public String toString(){
    	if (value > 0){
	        if (value == Double.POSITIVE_INFINITY)
	            return Characters.STR_INFTY;
	        if (value == Math.PI)
	            return Characters.STR_PI;
	        if (value == Math.E)
	            return Characters.STR_EULER;
    	}else{
            if (value == Double.NEGATIVE_INFINITY)
                return Characters.STR_NEG_INFTY;
            if (Double.isNaN(value))
            	return "undef";    		
    	}
        return Double.toString(value);
    }

	
	@Override
	public Operation getNegative() {
		return new RealDoubleOperation(-value);
	}
	
	public Operation getInvers(){
		return new RealDoubleOperation(1.0/value);
	}
	
	@Override
	public boolean isZero() {
		return value == 0.0;
	}

	
	@Override
	public boolean isNaN() {
		return Double.isNaN(value);
	}
	
	@Override
	public boolean isIntegral(){
		return value%1==0;
	}
	
	@Override
	public final boolean isPositive(){
		return value > 0;
	}
    
	@Override
	public final boolean isNegative(){
		return value < 0;
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
		return op.doubleValue() == value && op.doubleValueImag() == 0;
	}

	@Override
	public Operation getInstance(List<Operation> subclasses) {
		return this;
	}
}
