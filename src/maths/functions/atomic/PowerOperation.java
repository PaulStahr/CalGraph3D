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
import maths.data.RealDoubleOperation;
import maths.data.RealLongOperation;
import maths.data.RealRationalOperation;
import maths.exception.ArrayIndexOutOfBoundsExceptionOperation;
import maths.functions.AbsoluteOperation;

/**
 * is raising the first argument to the power of the second
 */

public class PowerOperation extends LinkingOperation
{
    private static final double PI_HALF_POS = Math.PI/2, PI_HALF_NEG = -Math.PI/2;
	private static final double HALF_SQUARE_OF_TWO = Math.sqrt(0.5);
    public final Operation a, b;

    private PowerOperation (Operation a, Operation b){
    	if ((this.a = a) == null || (this.b = b) == null)
    		throw new NullPointerException();
    }
    
    public static final PowerOperation getInstance(Operation a, Operation b){
    	if (a== null || b == null)
    		throw new NullPointerException();
    	if (a.isRealFloatingNumber()){
    		final double dvalue = a.doubleValue();
    		if (dvalue == Math.E)
    			return new ExponentOperation(b);
    	}
    	if (b.isRealIntegerNumber()){
    		final long value = b.longValue();
    		if (value == -1)
    			return new InversOperation(a);
    		if (value == 2)
    			return new SquareOperation(a);
    	}else if (b.isRealRationalNumber()){
    		final long numerator = b.longNumeratorValue(), denumerator = b.longDenumeratorValue();
    		if (numerator == 1){
    			if (denumerator == 3)
        			return new CubeRootOperation(a);
    			if (denumerator == 2)
        			return new SquareRootOperation(a);
    		}
    		if (denumerator == 1){
    			if (numerator == -1)
        			return new InversOperation(a);
    			if (numerator == 2)
        			return new SquareOperation(a);    		
    		}
    	}else if (b.isRealFloatingNumber()){
    		final double dvalue = b.doubleValue();
    		if (dvalue == 1./3.)
    			return new CubeRootOperation(a);
    		if (dvalue == 0.5)
    			return new SquareRootOperation(a);
    		if (dvalue == -1)
    			return new InversOperation(a);
    		if (dvalue == 2)
    			return new SquareOperation(a);
    	}
    	return new PowerOperation(a, b);
    }
    
    public static final Operation calculate (final Operation a, final Operation b, final CalculationController control){
    	if (a.isRealIntegerNumber() && b.isRealIntegerNumber()){
    		return pow(a.longValue(), b.longValue());
    	}
        if (a.isRealFloatingNumber()){
            if (b.isRealIntegerNumber())
                return new RealDoubleOperation(Calculate.pow(a.doubleValue(), b.longValue()));
            if (b.isRealFloatingNumber()){
            	final double va = a.doubleValue();
            	if (va > 0){
            		return new RealDoubleOperation(Math.pow(va, b.doubleValue()));
            	}else{
            		final double vb = b.doubleValue();
            		final double pi_b = Math.PI * vb, pow = Math.pow(-va, vb);
            		return ComplexDoubleOperation.get(Math.cos(pi_b) * pow, Math.sin(pi_b) * pow);
            	}
            }
        }
        if (a.isComplexFloatingNumber() && b.isRealIntegerNumber() && b.longValue() >= 0){
        	long exp = b.longValue();
        	double xr = a.doubleValue(), xi = a.doubleValueImag();
        	double re=1, im=0;
        	if ((exp & 1) == 1){
        		re = xr; im = xi;
        	}
        	exp >>= 1;
        	while (exp != 0){
        		double tmp = xr*xi*2;
        		xr = xr*xr-xi*xi;
        		xi = tmp;
        		if ((exp & 1) == 1){
        			tmp = re*xr -im*xi;
        			im = re*xi+im*xr;
        			re = tmp;
        		}
        		exp >>= 1;
        	}
        	return ComplexDoubleOperation.get(re, im);
        }
        if (a.isRealFloatingNumber() && b.isComplexFloatingNumber()){
        	final double ar = a.doubleValue(), br = b.doubleValue(), bi = b.doubleValueImag();
        	final double pytl = Math.log(ar*ar)*0.5;
        	final double same_inner_part = pytl * bi;
        	final double same_outer_part = Math.exp(pytl * br);
        	return ComplexDoubleOperation.get(Math.cos(same_inner_part)*same_outer_part, Math.sin(same_inner_part)*same_outer_part);
        }
        if (a.isComplexFloatingNumber() && b.isRealFloatingNumber()){
        	final double ar = a.doubleValue(), ai = a.doubleValueImag(), br = b.doubleValue();
        	final double same_inner_part = ((ai > 0 ? PI_HALF_POS : PI_HALF_NEG)-Math.atan2(ar,ai)) * br;
        	final double same_outer_part = Math.pow(ai*ai+ar*ar, 0.5 * br);
        	return ComplexDoubleOperation.get(Math.cos(same_inner_part)*same_outer_part, Math.sin(same_inner_part)*same_outer_part);
        }
        if (a.isComplexFloatingNumber() && b.isComplexFloatingNumber()){
        	final double ar = a.doubleValue(), ai = a.doubleValueImag(), br = b.doubleValue(), bi = b.doubleValueImag();
        	final double pytl = Math.log(ai*ai+ar*ar)*0.5, tan_sign = (ai > 0 ? PI_HALF_POS : PI_HALF_NEG)-Math.atan2(ar,ai);
        	final double same_inner_part = pytl * bi + tan_sign * br;
        	final double same_outer_part = Math.exp(pytl * br - tan_sign * bi);
        	return ComplexDoubleOperation.get(Math.cos(same_inner_part)*same_outer_part, Math.sin(same_inner_part)*same_outer_part);
        }
        
        if (b.isRealFloatingNumber()){
            if (b.doubleValue() == 1)
                return a;
            if (b.doubleValue() == 0)
            	return RealLongOperation.POSITIVE_ONE;
            if (b.doubleValue()%2==0 && a instanceof NegativeOperation)
            	return PowerOperation.getInstance(a.get(0), b);            
            if (a instanceof PowerOperation && a.get(1).isComplexFloatingNumber())
            	return PowerOperation.getInstance(a.get(0), MultiplicationOperation.calculate(b,a.get(1), control));
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
        return PowerOperation.getInstance (a, b);
    }

    private static final Operation pow(final long x, final long exp){
    	if (exp < 3){
        	if (exp < 0){
        		Operation o = pow(x,-exp);
        		return o.isRealIntegerNumber() ? RealRationalOperation.getInstance(1, o.longValue()) :  new RealDoubleOperation(1/o.doubleValue());
        	}
        	if (exp == 0)
        		return RealLongOperation.POSITIVE_ONE;
        	if (exp == 1)
        		return new RealDoubleOperation(x);
        	if (exp == 2){
        		if (x < -Integer.MAX_VALUE || x > Integer.MAX_VALUE)
            		return new RealDoubleOperation((double)x * (double) x);
    			return new RealLongOperation(x*x);
        	}
    	}
    	if (x == 0)
    		return RealLongOperation.POSITIVE_ONE;
    	long stelle=-1;
    	while ((exp >> ++stelle)!=0);
    	stelle = 1 << (stelle-1);
    	long ergl = x ;
    	/*Laeuft manchmal endlos*/
    	while (true){
    		long nextNumber = ergl;
    		if ((stelle>>=1)==0)
    			return new RealLongOperation(ergl);
    		if (nextNumber > Integer.MAX_VALUE || nextNumber < -Integer.MAX_VALUE)
    			break;
    		nextNumber *= nextNumber;
			if ((exp&stelle)!=0 && nextNumber != (nextNumber *= x)/x)
				break;
    		ergl=nextNumber;
    	}
    	double ergd = ergl;
    	do
    		ergd*=(exp&stelle)==0 ? ergd : ergd*x;
    	while ((stelle>>=1)!=0);
    	return new RealDoubleOperation(ergd);    	
    }
    
	@Override
	public Operation calculate (VariableAmount object, CalculationController control){
        return calculate (a.calculate(object, control), b.calculate(object, control), control);  
    }
   
	@Override
	public StringBuilder toString(final Print type, StringBuilder stringBuilder){
    	switch(type){
    		case CALGRAPH:{
    			toString(stringBuilder, a, a.getPriority()<=getPriority(), type);
    	        if (b.isRealIntegerNumber())
    	        	return stringBuilder.append(Characters.toHighString(String.valueOf(b.longValue())));
    	        stringBuilder.append(Characters.POW);
    	        return toString(stringBuilder, b, b.getPriority()<=getPriority(), type);     			
    		}case LATEX:
    		case OPEN_OFFICE:{
    			return b.toString(type, a.toString(type, stringBuilder).append('^').append('{')).append('}');    			
    		}default:
    			throw new IllegalArgumentException();
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
	public int getPriority(){
        return 6;
    }

    
	@Override
	public boolean needClip(int subClass){
    	return get(subClass).getPriority() <= getPriority();
    }
    
	
	@Override
	public char getChar() {
		return Characters.POW;
	}
	
	public static final class InversOperation extends PowerOperation{
		public InversOperation(Operation a){
			super(a, RealLongOperation.NEGATIVE_ONE);
		}
		
	    
		@Override
		public Operation calculate (VariableAmount object, CalculationController control){
	        return calculate (a.calculate(object, control), null);  
	    }
		
	    public static final Operation calculate (Operation a, CalculationController control){
	        if (a.isComplexFloatingNumber())
	        	return a.getInvers();
	    	return calculate (a, RealLongOperation.NEGATIVE_ONE, control);  
	    }
	}
	
	public static final class SquareRootOperation extends PowerOperation{
		public SquareRootOperation (Operation a){
			super(a, RealRationalOperation.POSITIVE_HALF);
	    }

	    public static final Operation calculate(final Operation a){
	    	if (a.isRealIntegerNumber()){
	    		final long val = a.longValue();
	    		if (val < 0){
	        		final long erg = Calculate.sqrt(-val);
	        		return erg == -1 ? ComplexDoubleOperation.get(0, Math.sqrt(-val)) : ComplexLongOperation.get(0, erg);
	    		}   			
	    		final long erg = Calculate.sqrt(val);
	    		return erg == -1 ? new RealDoubleOperation(Math.sqrt(val)) : new RealLongOperation(erg);
	    	}
	    	if (a.isRealRationalNumber()){
	    		final long numerator = a.longNumeratorValue(), denumerator = a.longDenumeratorValue();
	    		if (numerator < 0){
	    			final long sqrtnum = Calculate.sqrt(-numerator), sqrtde = Calculate.sqrt(denumerator);
	    			return sqrtnum == -1 || sqrtde == -1 ? ComplexDoubleOperation.get(0, Math.sqrt(-(double)numerator/denumerator)) :ComplexLongOperation.get(0, sqrtnum/sqrtde); 
	    		}
				final long sqrtnum = Calculate.sqrt(numerator), sqrtde = Calculate.sqrt(denumerator);
				return sqrtnum == -1 || sqrtde == -1 ? new RealDoubleOperation(Math.sqrt((double)numerator/denumerator)) :RealRationalOperation.getInstance(sqrtnum,sqrtde);     		
	    	}
	        if (a.isRealFloatingNumber()){
	        	final double val = a.doubleValue();
	        	if (val < 0)
	        		return ComplexDoubleOperation.get(0, Math.sqrt(-val));
	            return new RealDoubleOperation(Math.sqrt(val));
	        }
	        if (a.isComplexFloatingNumber()){
	        	final double ar = a.doubleValue(), ai = a.doubleValueImag();
	        	final double abs_value = Math.sqrt(ar*ar+ai*ai);
	        	return ComplexDoubleOperation.get(HALF_SQUARE_OF_TWO * Math.sqrt(abs_value+ar), (ai > 0 ? HALF_SQUARE_OF_TWO : -HALF_SQUARE_OF_TWO) * Math.sqrt(abs_value-ar));
	        }
	        if (a instanceof PowerOperation && a.get(1).isComplexFloatingNumber() && a.get(1).doubleValue() == 2)
	        	return new AbsoluteOperation(a.get(0));
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
	        return new SquareRootOperation(a);
	    }

	    
		@Override
		public final Operation calculate (VariableAmount object, CalculationController control){
	        return calculate(a.calculate(object, control));
	    }
	    
		@Override
		public final StringBuilder toString(Print type, StringBuilder stringBuilder){
	    	switch(type){
	    		case CALGRAPH:return a.toString(type, stringBuilder.append("sqrt(")).append(')');
	    		case LATEX:return a.toString(type, stringBuilder.append("\\sqrt{")).append('}');
	    		case OPEN_OFFICE:return a.toString(type, stringBuilder.append("sqrt{")).append('}');
	    		default: throw new IllegalArgumentException();
	     	}
	    }
	}
	
	public static final class CubeRootOperation extends PowerOperation{
	    public CubeRootOperation (Operation a){
	    	super(a, RealRationalOperation.POSITIVE_THIRDS);
	    }
	
	    public static Operation calculate(final Operation a, final CalculationController control){
	        if (a.isRealFloatingNumber())
	            return new RealDoubleOperation(Math.cbrt(a.doubleValue()));
	        if (a.isArray()){
	        	return new ArrayOperation.ArrayCreator(a.size()){
					@Override
					public final Operation get(int index) {
						return calculate(a.get(index), control);
					}
	        	}.getArray();
	        }
	        return PowerOperation.calculate(a, RealRationalOperation.POSITIVE_THIRDS, control);
	    }
	
	    
		@Override
		public Operation calculate (VariableAmount object, CalculationController control){
	        return calculate(a.calculate(object, control), control);
	    }

		public final StringBuilder toString(Print type, StringBuilder stringBuilder){
    		return a.toString(type, stringBuilder.append("cbrt(")).append(')');
	    }
	}
	
	public static final class SquareOperation extends PowerOperation{
		private SquareOperation(Operation a){
			super(a, RealLongOperation.POSITIVE_TWO);
		}
	    
		@Override
		public Operation calculate (VariableAmount object, CalculationController control){
	        return calculate (a.calculate(object, control), null);  
	    }
		
	    public static final Operation calculate (Operation a, CalculationController control){
	        if (a.isRealIntegerNumber()){
	        	long val = a.longValue();
	        	if (val == 0)
	        		return RealLongOperation.ZERO;
	        	long erg = val * val;
	        	if (erg / val == val)
	        		return new RealLongOperation(erg);
	        	return new RealDoubleOperation((double)val * (double)val);
	        }
	        if (a.isRealRationalNumber())
	        {
	        	long numeratorInput = a.longNumeratorValue();
	        	if (numeratorInput == 0)
	        		return RealLongOperation.ZERO;
	        	long numeratorErg = numeratorInput * numeratorInput;
	        	long denumeratorInput = a.longDenumeratorValue();
	        	long denumeratorErg = denumeratorInput * denumeratorInput;
	        	if (numeratorErg / numeratorInput == numeratorInput && denumeratorErg / denumeratorInput == denumeratorInput)
	        	{
	        		return RealRationalOperation.getInstance(numeratorErg, denumeratorErg);
	        	}
	        	double frac = (double)numeratorInput / (double) denumeratorInput;
	        	return new RealDoubleOperation(frac * frac);
	        }
	        if (a.isRealFloatingNumber())
	        	return new RealDoubleOperation(a.doubleValue() * a.doubleValue());
	        if (a.isComplexFloatingNumber()){
	        	final double ar = a.doubleValue(), ai = a.doubleValueImag();
	        	return ComplexDoubleOperation.get(ar*ar-ai*ai, ar*ai);
	        }
	    	return calculate (a, RealLongOperation.POSITIVE_TWO, control);  
	    }
	}
	
	public static final class ExponentOperation extends PowerOperation{
		public ExponentOperation(Operation b){
			super(RealDoubleOperation.E, b);
		}
	    
		@Override
		public Operation calculate (VariableAmount object, CalculationController control){
	        return calculate (b.calculate(object, control), null);  
	    }
		
	    public static final Operation calculate (Operation b, CalculationController control){
	        if (b.isRealIntegerNumber()){
	        	long value = b.longValue();
	        	if (value == 0)
	        		return RealLongOperation.POSITIVE_ONE;
	        	return new RealDoubleOperation(Math.exp(value));
	        }
	        if (b.isRealFloatingNumber())
	        	return new RealDoubleOperation(Math.exp(b.doubleValue()));
	        if (b.isComplexFloatingNumber()){
	        	final double aexp = Math.exp(b.doubleValue()), ai = b.doubleValueImag();
	        	return ComplexDoubleOperation.get(Math.cos(ai)*aexp, Math.sin(ai)*aexp);
	        }
	    	return calculate (RealDoubleOperation.E, b, control);  
	    }
	}
	
	
	@Override
	public Operation getInstance(List<Operation> subclasses) {
		return getInstance(subclasses.get(0), subclasses.get(1));
	}
}
