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
public final class StringOperation extends Operation implements CharSequence
{
    private final String value;
    public StringOperation (boolean value){
        this.value = String.valueOf(value);
    }

    public StringOperation (long value){
        this.value = String.valueOf(value);
    }

    public StringOperation (double value){
        this.value = String.valueOf(value);
    }

    public StringOperation (String value){
    	if ((this.value = value) == null)
    		throw new NullPointerException();
     }
    
    public static final StringOperation getInstance(String value, int begin, int end){
    	if (value == null)
    		throw new NullPointerException();
    	final int length = value.length();
    	final StringBuilder strBuilder = new StringBuilder(length);
    	for (int i = begin; i < end; i++){
    		final char c = value.charAt(i);
    		if (c == '\\'){
    			if (++i > length)
    				return null;
    			switch(value.charAt(i)){
    				case '"':	strBuilder.append('"');break;
    				case 'n':	strBuilder.append('\n');break;
    				case '\\':	strBuilder.append('\\');break;
    				default : return null;
    			}
    		}else{
    			strBuilder.append(c);
    		}
    	}
    	return new StringOperation(strBuilder.toString());    	
    }
    
    @Override
	public int getTypeBitmask(){
		return BITMASK_STRING;
	}

	@Override
	public final boolean isString(){
		return true;
	}
	    
	@Override
	public StringOperation calculate (VariableAmount object, CalculationController control){
        return this;
    }
    
	@Override
	public final StringBuilder toString(Print type, StringBuilder stringBuilder){
    	stringBuilder.append('"');
    	for (int i=0, length = value.length();i<length;i++){
    		final char c = value.charAt(i);
    		switch (c){
   				case '"': stringBuilder.append('\\').append('"');break;
   				case '\\': stringBuilder.append('\\').append('\\');break;
   				case '\n': stringBuilder.append('\\').append('n');break;
    			default: stringBuilder.append(c);break;
    		}
    	}
    	return stringBuilder.append('"');
    }
    
    
	@Override
	public final String stringValue(){
    	return value;
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
	public final boolean equals(Object obj){
		if (!(obj instanceof Operation))
			return false;
		Operation op = (Operation)obj;
    	return op.isString() && op.stringValue().equals(value);
    }
    
    
	@Override
	public boolean isPrimitive(){
        return true;
    }

	@Override
	public final char charAt(int index) {
		return value.charAt(index);
	}

	@Override
	public final int length() {
		return value.length();
	}

	@Override
	public final CharSequence subSequence(int beginIndex, int endIndex) {
		return new StringOperation(value.substring(beginIndex, endIndex));
	}

	@Override
	public Operation getInstance(List<Operation> subclasses) {
		return this;
	}
}
