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
package maths;

import java.util.List;

import maths.data.StringId;

/** 
* @author  Paul Stahr
* @version 04.02.2012
*/
public final class UserVariableOperation extends Operation
{
    public final int nameId;
    public final StringId.StringIdObject nameObject;
    
    public UserVariableOperation(StringId.StringIdObject name){
        if (!Variable.isValidName((nameObject= name).string))
            throw new RuntimeException ("Name not supported");    	
    	nameId = name.id;
    }
    
    public UserVariableOperation (String name){
    	this(StringId.getStringAndId(name));
    }
    
    public UserVariableOperation (String name, int begin, int end){
    	this(StringId.getStringAndId(name, begin, end));
    }
    
	@Override
	public final Operation calculate (VariableAmount object, CalculationController control){
        if (object == null)
            return this;
        Variable variable = object.getById(nameId);
        if (variable==null)
            return this;
        Operation erg = variable.getValue();
        if (erg != null)
        {
        	return erg.calculate(object, control);
        }
        return control.connectEmptyVariables() ? new UserVariableOperationInserted(variable) : this;
    }

	@Override
	public final StringBuilder toString(Print type, StringBuilder stringBuilder){
        return stringBuilder.append(nameObject.string);
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
	public final String toString(){
    	return nameObject.string;
    }
    
	@Override
	public final boolean equals(Object o){
    	return o instanceof UserVariableOperation && ((UserVariableOperation)o).nameId == nameId;
    }
	
	@Override
	public Operation getInstance(List<Operation> subclasses) {
		return this;
	}
}
