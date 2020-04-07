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
import maths.functions.FunctionOperation;

public class UserFunctionOperation extends FunctionOperation {
    public final int nameId;
    public final StringId.StringIdObject nameObject;
	private final Operation operands[];
	
	public UserFunctionOperation(StringId.StringIdObject name, Operation operands[]){
		if (!Variable.isValidName((nameObject= name).string))
            throw new RuntimeException ("Name not supported");    	
    	nameId = name.id;
		this.operands = new Operation[operands.length];
		for (int i=0;i<operands.length;i++)
			if ((this.operands[i] = operands[i]) == null)
				throw new NullPointerException();
	}
	
	public UserFunctionOperation(String name, Operation operands[]){
		this(StringId.getStringAndId(name), operands);
	}
	
	@Override
	public String getFunctionName() {
		return nameObject.string;
	}

	
	@Override
	public Operation calculate(VariableAmount object, CalculationController control) {
        if (object == null)
            return this;
        Variable variable = object.getById(nameId, operands.length);
        Operation op[] = new Operation[operands.length];
    	boolean insert = variable!=null && operands.length == variable.operandCount();
    	for (int i=0;i<op.length;i++)
    		if (!((op[i] = operands[i].calculate(object, control)).isPrimitive() || op[i] instanceof UserVariableOperation))
    			insert = false;
    	
    	if (insert){
        	VariableStack stack = new VariableStack(object);
        	try{
    	    	for (int i=0;i<operands.length;i++){
    	    		Variable v = new Variable(variable.operand(i), op[i]);
    	    		stack.addLocal(v);
    	    	}
    	    	return variable.getValue().calculate(stack, control);
        	}catch(RuntimeException e){
            	throw e;
        	}
    	}else{
    		return new UserFunctionOperation(nameObject, op);
    	}

    }
	
	@Override
	public final int size() {
		return operands.length;
	}
	
	@Override
	public final Operation get(int index) {
		return operands[index];
	}

	@Override
	public Operation getInstance(List<Operation> subclasses) {
		return this;
	}
}
