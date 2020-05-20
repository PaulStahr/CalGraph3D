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
package maths.functions.variable;

import java.util.ArrayList;
import java.util.List;

import maths.Operation;
import maths.UserFunctionOperation;
import maths.UserVariableOperation;
import maths.Variable;
import maths.VariableAmount;
import maths.data.ArrayOperation;
import maths.data.Characters;
import maths.data.StringId;
import maths.data.StringId.StringIdObject;
import maths.exception.ExceptionOperation;
import maths.functions.ArrayIndexOperation;
import maths.functions.atomic.LinkingOperation;

/** 
* @author  Paul Stahr
* @version 04.02.2012
*/
public final class SetOperation extends LinkingOperation
{
    public final Operation a, b;
    public final int nameId;
    public final StringId.StringIdObject nameObject;
    private final Operation indexes[];

    public SetOperation (Operation a, Operation b){
    	if ((this.a = a) == null || b == null)
    		throw new NullPointerException();
    	if (b instanceof ArrayIndexOperation){
        	ArrayList<Operation> al = new ArrayList<Operation>();
    		while (b instanceof ArrayIndexOperation){
    			al.add(((ArrayIndexOperation)b).index);
    			b = ((ArrayIndexOperation)b).array;
    		}
    		if (!(b instanceof UserVariableOperation))
    			throw new IllegalArgumentException();
    		indexes = al.toArray(new Operation[al.size()]);
    	}else{
    		indexes = null;
    	}
    	if (b instanceof UserVariableOperation){
    		nameObject = ((UserVariableOperation)b).nameObject;
    	}else if (b instanceof UserFunctionOperation){
    		nameObject = ((UserFunctionOperation)b).nameObject;
    	}else{
    		throw new IllegalArgumentException(b.getClass().toString());
    	}
    	nameId = nameObject.id;
    	this.b = b;
    }

    public SetOperation (Operation a, Operation b, String name, Operation indexes[]){
    	this(a, b, StringId.getStringAndId(name), indexes);
    }

    
	public SetOperation(Operation a, Operation b, StringIdObject name, Operation indexes[]) {
    	if ((this.a = a) == null || (this.b = b) == null)
    		throw new NullPointerException();
    	this.nameObject = name;
    	this.nameId = nameObject.id;
    	this.indexes = indexes;		
	}

	@Override
	public Operation calculate (VariableAmount object, CalculationController control){
    	Operation a = this.a.calculate(object, control);
    	if (indexes != null){
    		final Variable v = object.getById(nameId);
    		if (v != null){
    			Operation erg = v.set(indexes, object, a, control);
    			if (erg != null)
    				return erg;
    		}
    		Operation newIndexes[] = new Operation[indexes.length];
    		for (int i=0;i<newIndexes.length;i++)
    			if ((newIndexes[i] = indexes[i].calculate(object, control)) instanceof ExceptionOperation)
    				return newIndexes[i];
    		return new SetOperation(a,b,nameObject,newIndexes);
    	}
        if (b instanceof UserVariableOperation){
           	if (a.isArray())
        		a = new ArrayOperation((ArrayOperation)a);
           	object.assignAddLocal(nameObject, a);
            return  a;
        }
        if (b instanceof UserFunctionOperation){
        	StringId.StringIdObject ops[] = new StringId.StringIdObject[b.size()];
        	for (int i=0;i<ops.length;i++){
         		if (!(b.get(i) instanceof UserVariableOperation))
        			return new ExceptionOperation("Nur Variablen erlaubt");
        		ops[i] = ((UserVariableOperation)b.get(i)).nameObject;
        	}
        	object.replaceAddLocal(new Variable(nameObject, a, ops));
            return  a;
        }
        return new SetOperation(a,b);
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
	public final int getPriority(){
        return 0;
    }

	
	@Override
	public final char getChar() {
		return Characters.SET;
	}
	
	
	@Override
	public StringBuilder toString(Print type, StringBuilder stringBuilder) {
		super.toString(type, stringBuilder);
		for (int i=0;i<indexes.length;i++)
			indexes[i].toString(type, stringBuilder.append('[')).append(']');
		return stringBuilder;
	}

	@Override
	public Operation getInstance(List<Operation> subclasses) {
		return new SetOperation(subclasses.get(0), subclasses.get(1));
	}
}
