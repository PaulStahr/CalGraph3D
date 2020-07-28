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

public final class CharacterOperation extends Operation
{
	private static final CharacterOperation co[][] = new CharacterOperation[0x100][];
    public final char value;
    
    public static final Operation getInstance(final char c){
    	Operation line[] = co[c>>8];
    	return line == null || line[c&0xFF] == null ? create(c) : line[c&0xFF];
    }
    
    private static final synchronized Operation create(final char c){
    	Operation line[] = co[c>>8];
    	if (line == null)
    		line = co[c>>8] = new CharacterOperation[0x100];
    	return line[c&0xFF] == null ? line[c&0xFF] = new CharacterOperation(c) : line[c&0xFF];
    }

    private CharacterOperation (final char c){
       	value = c;
    }

	@Override
	public int getTypeBitmask(){
		return BITMASK_INT_REAL | BITMASK_RATIONAL_REAL | BITMASK_FLOAT_REAL | BITMASK_INT_COMPLEX | BITMASK_RATIONAL_COMPLEX | BITMASK_FLOAT_COMPLEX | BITMASK_CHARACTER;
	}

    
	@Override
	public final boolean isRealFloatingNumber(){
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
	public final boolean isCharacter(){
		return true;
	}
	
	@Override
	public final double doubleValue(){
        return value;
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
	public final CharacterOperation calculate (VariableAmount object, CalculationController control){
        return this;
    }

    
	@Override
	public final StringBuilder toString(Print type, StringBuilder stringBuilder){
		stringBuilder.append('\'');
		if (value == '\n')
			stringBuilder.append('\\').append('n');
		else if (value == '\t')
			stringBuilder.append('\\').append('t');
		else
			stringBuilder.append(value);
    	return stringBuilder.append('\'');
    }
    
	/*@Override
	public final String toString(){
		if (value == '\n')
			return "'\\n'";
    	return String.valueOf(new char[]{'\'',value,'\''});
	}*/
	
	@Override
	public final int size() {
		return 0;
	}

	
	@Override
	public final Operation get(int index) {
		throw new ArrayIndexOutOfBoundsException(index);
	}

    public final boolean isConstant (){
        return true;
    }

	
	@Override
	public Operation getNegative() {
		return new RealLongOperation(-value);
	}
	
	@Override
	public Operation getInvers(){
		if(value == 1)
			return this;
		return RealRationalOperation.getInvers(value);
	}
	
	@Override
	public boolean isZero() {
		return value == 0;
	}
	
	@Override
	public boolean isNaN() {
		return false;
	}
	
	@Override
	public final boolean equals(Object o){
		if (!(o instanceof Operation))
			return false;
		Operation op = (Operation)o;
		return op.isIntegral() && op.longValue()==value && op.longValueImag() == 0;
	}
	
    
	@Override
	public final boolean isPrimitive(){
        return true;
    }
	
	@Override
	public final boolean isPositive(){
		return value != 0;
	}

	@Override
	public final boolean isNegative(){
		return false;
	}
    
	@Override
	public final boolean isIntegral(){
		return true;
	}

	@Override
	public final Operation getInstance(List<Operation> subclasses) {
		return this;
	}
}
