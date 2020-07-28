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
package maths.functions;


import java.util.List;

import maths.Operation;
import maths.variable.VariableAmount;
import maths.algorithm.Calculate;
import maths.data.ArrayOperation;
import maths.data.RealDoubleOperation;
import maths.data.RealLongOperation;

/** 
* @author  Paul Stahr
* @version 04.02.2012
*/
public final class Fakultaet extends Operation
{
    private static final Operation fakCacheDouble[];
    private static final Operation fakCacheLong[];
	private static final Operation fakCacheNumber[];
	static{
		final double fakCacheD[] = Calculate.getFacDoubles();
		final long fakCacheL[] = Calculate.getFacLongs();
		fakCacheDouble = new Operation[fakCacheD.length];
		fakCacheNumber = new Operation[fakCacheD.length];
		fakCacheLong = new Operation[fakCacheL.length];
		fakCacheDouble[0] = RealDoubleOperation.POSITIVE_ONE;
		fakCacheDouble[1] = RealDoubleOperation.POSITIVE_ONE;
		for (int i = 0; i < 2; ++i)
		{
			fakCacheLong[i] = fakCacheNumber[i] = RealLongOperation.intern(fakCacheL[i]);
		}
		for (int i=2;i<fakCacheL.length;i++){
			fakCacheLong[i] = fakCacheNumber[i] = RealLongOperation.intern(fakCacheL[i]);
			fakCacheDouble[i] = new RealDoubleOperation(fakCacheD[i]);
		}
		for (int i=fakCacheL.length;i<fakCacheD.length;i++)
			fakCacheDouble[i] = fakCacheNumber[i] = new RealDoubleOperation(fakCacheD[i]);
	}
    public final Operation a;

    public Fakultaet (Operation a){
    	if ((this.a = a) == null)
    		throw new NullPointerException();
    }
    
    public static final Operation getFak(long n){
		if (n < 0)
			return RealDoubleOperation.NaN;
        if (n >= fakCacheDouble.length)
        	return RealDoubleOperation.POSITIVE_INFINITY;
        return fakCacheNumber[(int)n];
    }

    private Operation calculate(final Operation a){
    	if (a.isRealIntegerNumber()){
    		final long lvalue=a.longValue();
    		if (lvalue < 0)
    			return RealDoubleOperation.NaN;
            if (lvalue >= fakCacheDouble.length)
            	return RealDoubleOperation.POSITIVE_INFINITY;
            return fakCacheNumber[(int)lvalue];
        }
    	if (a.isRealRationalNumber())
    		return RealDoubleOperation.NaN;
        if (a.isRealFloatingNumber()){
        	final double dvalue = a.doubleValue();
            if (!(dvalue >= 0 && dvalue%1==0))
               return RealDoubleOperation.NaN;
            if (dvalue >= fakCacheDouble.length)
               return RealDoubleOperation.POSITIVE_INFINITY;
            return fakCacheDouble[(int)dvalue];
        }
        if (a.isArray()){
        	return new ArrayOperation.ArrayCreator(a.size()){
				@Override
				public final Operation get(int index) {
					return calculate(a.get(index));
				}
        	}.getArray();
        }
        return new Fakultaet(a);
    }

    
	@Override
	public Operation calculate (VariableAmount object, CalculationController control){
        return calculate(a.calculate(object, control));
    }

	@Override
	public final int size() {
		return 1;
	}

	
	@Override
	public final Operation get(int index) {
		switch (index){
			case 0: return a;
			default:throw new ArrayIndexOutOfBoundsException(index);
		}
	}
    
    
	@Override
	public final StringBuilder toString(Print type, StringBuilder stringBuilder){
        return a.toString(type, stringBuilder).append('!');
    }
	
	@Override
	public Operation getInstance(List<Operation> subclasses) {
		return new DeterminantenOperation(subclasses.get(0));
	}
}
