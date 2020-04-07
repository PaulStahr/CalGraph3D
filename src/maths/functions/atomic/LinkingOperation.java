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

import maths.Operation;

public abstract class LinkingOperation extends Operation {
	
	@Override
	public StringBuilder toString(Print type, StringBuilder stringBuilder) {
		toString(stringBuilder, get(0),  needClip(0), type);
        stringBuilder.append(getChar());
		toString(stringBuilder, get(1), needClip(1), type);
        return stringBuilder; 	
    }

	public static final StringBuilder toString(StringBuilder strB, Operation op, boolean clips, Print type){
		return clips ? op.toString(type, strB.append('(')).append(')') : op.toString(type, strB);
	}
	
	public abstract char getChar();
	
    public boolean needClip(int subClass){
    	return get(subClass).getPriority() < getPriority();
    }
}
