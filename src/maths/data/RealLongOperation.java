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


import java.util.ArrayList;
import java.util.List;

import maths.Operation;
import maths.VariableAmount;



/** 
* @author  Paul Stahr
* @version 04.02.2012
*/
public final class RealLongOperation extends Operation
{
    private static RealLongOperation cache[][] = new RealLongOperation[0x10][];
    private static final int max_cache_line_length = 8;
    private static int cachelength_min_1 = cache.length-1;
	private static int maxCacheLength = 0x10000;
	public static final RealLongOperation MAX_VALUE     = intern(Long.MAX_VALUE);
    public static final RealLongOperation POSITIVE_TWO  = intern(2);
    public static final RealLongOperation POSITIVE_ONE  = intern(1);
    public static final RealLongOperation ZERO          = intern(0);
    public static final RealLongOperation NEGATIVE_ONE  = intern(-1);
    public static final RealLongOperation MIN_VALUE     = intern(Long.MIN_VALUE);
    private final long value;
    
    public RealLongOperation (long rechnung){
        value = rechnung;
    }

    public RealLongOperation (String number){
    	if (number == null)
    		throw new NullPointerException();
        value = Long.parseLong (number);
    }
    
    @Override
	public final int getTypeBitmask(){
		return BITMASK_INT_REAL | BITMASK_RATIONAL_REAL | BITMASK_FLOAT_REAL | BITMASK_INT_COMPLEX | BITMASK_RATIONAL_COMPLEX | BITMASK_FLOAT_COMPLEX;
	}
    
	@Override
	public final boolean isRealFloatingNumber(){
		return true;
	}
	
	@Override
	public final boolean isRealRationalNumber(){
		return true;
	}
	
	@Override
	public final boolean isRealIntegerNumber(){
		return true;
	}
	
	@Override
	public final boolean isComplexFloatingNumber(){
		return true;
	}
	
	@Override
	public final boolean isComplexIntegerNumber(){
		return true;
	}
	
	@Override
	public final boolean isComplexRationalNumber(){
		return true;
	}
	
	@Override
	public final Operation getNegative(){
		return new RealLongOperation(-value);
	}
	
	public final Operation getInvers(){
    	return RealRationalOperation.getInvers(value);
	}
	
    public static final RealLongOperation valueOf(String s, int radix){
    	if (s == null || radix < Character.MIN_RADIX || radix > Character.MAX_RADIX || s.length() == 0)
    		return null;
        long result = 0, limit = -Long.MAX_VALUE;
        boolean negative = false;
        int i, len = s.length(), digit;
 
        switch (s.charAt(0)){
         	case '-':
                negative = true;
                limit = Long.MIN_VALUE;
         	case '+':
         		if (len == 1)
         			return null;
         		i = 1;
         		break;
         	default:
         		i=0;
        }
        for (long multmin = limit / radix; i < len; result -= digit)
            if ((digit = Character.digit(s.charAt(i++),radix)) < 0 || result < multmin || (result *= radix) < limit + digit) 
                 return null;
        return new RealLongOperation(negative ? result : -result);
    }
       
    public static final RealLongOperation valueOf(String s, int begin, int end, int radix){
    	if (s == null || radix < Character.MIN_RADIX || radix > Character.MAX_RADIX || begin == end)
    		return null;
        long result = 0, limit = -Long.MAX_VALUE;
        boolean negative = false;
        int i, len = end - begin, digit;
 
        switch (s.charAt(begin)){
         	case '-':
                negative = true;
                limit = Long.MIN_VALUE;
         	case '+':
         		if (len == 1)
         			return null;
         		i = 1;
         		break;
         	default:
         		i=0;
        }
        for (long multmin = limit / radix; i < len; result -= digit)
            if ((digit = Character.digit(s.charAt(i++ + begin),radix)) < 0 || result < multmin || (result *= radix) < limit + digit) 
                 return null;
        return new RealLongOperation(negative ? result : -result);
    }

    public static final RealLongOperation valueOfHighNumber(CharSequence s, int begin, int end, int radix){
    	if (s == null || radix < Character.MIN_RADIX || radix > Character.MAX_RADIX || begin == end)
    		return null;
        long result = 0, limit = -Long.MAX_VALUE;
        boolean negative = false;
        int i, len = end - begin, digit;
 
        switch (s.charAt(begin)){
         	case Characters.HIGH_SUB:
                negative = true;
                limit = Long.MIN_VALUE;
         	case Characters.HIGH_PLUS:
         		if (len == 1)
         			return null;
         		i = 1;
         		break;
         	default:
         		i=0;
        }
        for (long multmin = limit / radix; i < len; result -= digit)
            if ((digit = Characters.digit(s.charAt(i++ + begin),radix)) < 0 || result < multmin || (result *= radix) < limit + digit) 
                 return null;
        return new RealLongOperation(negative ? result : -result);	
    }
    
    public static final RealLongOperation valueOf(char[] s, int begin, int end, int radix){
    	if (s == null || radix < Character.MIN_RADIX || radix > Character.MAX_RADIX || begin == end)
    		return null;
        long result = 0, limit = -Long.MAX_VALUE;
        boolean negative = false;
        int i, len = end - begin, digit;
 
        switch (s[begin]){
         	case '-':
                negative = true;
                limit = Long.MIN_VALUE;
         	case '+':
         		if (len == 1)
         			return null;
         		i = 1;
         		break;
         	default:
         		i=0;
        }
        for (long multmin = limit / radix; i < len; result -= digit)
            if ((digit = Character.digit(s[i++ + begin],radix)) < 0 || result < multmin || (result *= radix) < limit + digit) 
                 return null;
        return new RealLongOperation(negative ? result : -result);
    }
       
   private static final int binarySearch(RealLongOperation[] line, long value){
		int low = 0, high = line.length-1;
		while (low<=high){
			final int middle=(low+high)>>1;
			long c = value - line[middle].value;
			if (c>0)
				low = middle+1;
			else if (c==0)
				return middle;
			else
				high = middle-1;
		}
		return -1-low;
   }
   
   private static RealLongOperation[][] expandCache(RealLongOperation cache[][]){
	   RealLongOperation newCache[][] = new RealLongOperation[cache.length * 2][];
	   ArrayList<RealLongOperation> a = new ArrayList<RealLongOperation>(), b = new ArrayList<RealLongOperation>();
	   for (int i=0;i<cache.length;++i){
		   a.clear();
		   b.clear();
		   RealLongOperation line[] = cache[i];
		   if (line != null){
			   for (RealLongOperation rlo : line){
				   if ((rlo.longValue() & newCache.length) == 0){
					   a.add(rlo);
				   }else{
					   b.add(rlo);
				   }
			   }
			   if (a.size() != 0)
				   newCache[i] = a.toArray(new RealLongOperation[a.size()]);
			   if (b.size() != 0)
				   newCache[i + cache.length] = b.toArray(new RealLongOperation[b.size()]);
		   }
	   }
	   return newCache;
   }
    
    private static synchronized final RealLongOperation insert(RealLongOperation rlo){
    	long value = rlo.longValue();
    	while (true){
	    	RealLongOperation line[] = cache[cachelength_min_1 & (int)value];
	    	if (line == null){
	    		cache[cachelength_min_1 & (int)value] = new RealLongOperation[]{rlo};
	        	return rlo;
	    	}
	    	int index = binarySearch(line, value);
	    	if (index >= 0)
	    		return line[index];
	    	if (line.length >= max_cache_line_length && cache.length * 2 <= maxCacheLength ){
	    		cache = expandCache(cache);
	    		cachelength_min_1 = cache.length - 1;
	    		continue;
	    	}
	    	index = -index - 1;
	    	RealLongOperation newLine[] = new RealLongOperation[line.length+1];
	    	System.arraycopy(line, 0, newLine, 0, index);
	    	newLine[index] = rlo;
	    	System.arraycopy(line, index, newLine, index+1, line.length-index);
	    	cache[cachelength_min_1 & (int)value] = newLine;
	        return rlo;
    	}
    }
    
	public final RealLongOperation intern(){
        RealLongOperation line[] = cache[cachelength_min_1 & (int)value];
        if (line == null){
        	return insert(this);
        }
        int index = binarySearch(line, value);
        if (index < 0)
        	return insert(this);
        return line[index];		
    }

    public static final RealLongOperation intern(final long value){
        RealLongOperation line[] = cache[cachelength_min_1 & (int)value];
        if (line == null){
        	return insert(new RealLongOperation(value));
        }
        int index = binarySearch(line, value);
        if (index < 0)
        	return insert(new RealLongOperation(value));
        return line[index];		

    }
    
	@Override
	public final double doubleValue(){
        return value;
    }

	public final long longNumeratorValue(){
        return value;
    }

    public final long longDenumeratorValue(){
        return 1;
    }
    
    public long longNumeratorValueImag(){
        return 0;
    }

    public long longDenumeratorValueImag(){
        return 1;
    }	
    
    @Override
	public final long longValue(){
        return value;
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
	public final RealLongOperation calculate (final VariableAmount object, CalculationController control){
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
        return stringBuilder.append(value);
    }
    
	@Override
	public final String toString(){
    	return Long.toString(value);
    }
    	
	public final boolean isZero() {
		return value==0;
	}
	
	public final boolean isNaN() {
		return false;
	}
	
	public final boolean isIntegral(){
		return true;
	}
	
	public final boolean isPositive(){
		return value > 0;
	}
	
	@Override
	public final boolean isNegative(){
		return value < 0;
	}
    
	@Override
	public final boolean isPrimitive(){
        return true;
    }
	
	@Override
	public final boolean equals(Object obj){
		if (!(obj instanceof Operation))
			return false;
		Operation op = (Operation)obj;
		if (op.isRealIntegerNumber())
			return op.longValue() == value;
		if (op.isRealFloatingNumber())
			return op.doubleValue() == value;
		return false;
	}
	
	public final int hashCode(){
		return (int)value;
	}

	@Override
	public final Operation getInstance(List<Operation> subclasses) {
		return this;
	}
}
