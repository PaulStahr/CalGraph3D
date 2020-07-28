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
import maths.algorithm.OperationCalculate;
import maths.data.RealLongOperation;
import maths.exception.ArrayIndexOutOfBoundsExceptionOperation;
import maths.exception.ExceptionOperation;
import maths.functions.atomic.AdditionOperation;
import maths.functions.atomic.DivisionOperation;
import maths.functions.atomic.MultiplicationOperation;
import maths.functions.atomic.PowerOperation;
import maths.functions.atomic.SubtractionOperation;
import maths.variable.UserVariableOperation;
import maths.variable.Variable;
import maths.variable.VariableAmount;
import maths.variable.VariableStack;

public class TaylorPolynomOperation extends FunctionOperation {
    public final Operation function, differentations, point, variables;

    public TaylorPolynomOperation (Operation function, Operation differentations, Operation variables, Operation point){
    	if ((this.function = function) == null || (this.differentations = differentations) == null || (this.variables = variables) == null || (this.point = point) == null)
    		throw new NullPointerException();
    }	
	@Override
	public Operation calculate(VariableAmount object,CalculationController control) {
		return calculate(function.calculate(object, control), differentations.calculate(object, control), variables.calculate(object, control), point.calculate(object, control), object, control);
	}
	
    public static Operation calculate(Operation function, Operation differentations, Operation variable, Operation position, VariableAmount object, CalculationController control){
    	calc:{
    		Variable v[];
    		UserVariableOperation uvo[];
    		if (variable.isArray()){
        		v = new Variable[variable.size()];
        		uvo = new UserVariableOperation[v.length];
        		for (int i=0;i<v.length;i++){
        			Operation op = variable.get(i);
        			if (!(op instanceof UserVariableOperation))
        				return new ExceptionOperation("Not a variable Name");
        			uvo[i] = (UserVariableOperation)op;
        			v[i] = new Variable(uvo[i].nameObject);
        		}
        	}else if (variable instanceof UserVariableOperation){
        		uvo = new UserVariableOperation[]{(UserVariableOperation)variable};
        		v = new Variable[]{new Variable(uvo[0].nameObject)};
    		}else{
    			break calc;
    		}

			if (position.isArray()){
        		if (v.length != position.size())
        			return new ArrayIndexOutOfBoundsExceptionOperation();
        		for (int i=0;i<v.length;i++)
        			v[i].setValue(position.get(i));
			}else if(position.isComplexFloatingNumber()){
				if (v.length != 1)
					return new ArrayIndexOutOfBoundsExceptionOperation();
				v[0].setValue(position);
			}else{
				break calc;
			}
        	if (!differentations.isRealIntegerNumber()){
        		break calc;
        	}
    		long grad = differentations.longValue();
    		VariableStack stack = new VariableStack(v, object);
    		return calculatePolynom(function, uvo, v, stack, null, control, 0, grad, RealLongOperation.POSITIVE_ONE);
    	
    	}

        Operation erg = OperationCalculate.standardCalculations(function, variable);
        if (erg != null)
        	return erg;
        return new TaylorPolynomOperation(function, differentations, variable, position);
    }

    private static final Operation calculatePolynom(Operation diff, UserVariableOperation uvo[], Variable v[], VariableStack stack, Operation mult, CalculationController control, int index, long grad, Operation divide){
    	if (control.getStopFlag())
    		return new ExceptionOperation("stopped");
    	Operation erg = null;
    	for (int i=0;i<grad;++i){
    		Operation nextMult = mult;
    		if (i > 0){
    			divide = MultiplicationOperation.calculate(divide, new RealLongOperation(i), control);
    			Operation pow = PowerOperation.getInstance(SubtractionOperation.calculate(uvo[index], v[index].getValue(), control), new RealLongOperation(i));	
    			nextMult = mult == null ? pow : MultiplicationOperation.calculate(nextMult, pow , control);
    		}
    		
    		Operation add;
    		if (index < uvo.length -1)
    			add = calculatePolynom(diff, uvo, v, stack, nextMult, control, index + 1, grad - i, divide);
    		else{
    			add = DivisionOperation.calculate(diff.calculate(stack, control), divide, control);
    			if (nextMult != null)
    				add = MultiplicationOperation.calculate(add, nextMult, control);
    		}erg = erg == null ? add : AdditionOperation.calculate(erg, add, control);	
    		diff = DifferentiationOperation.calculate(diff, uvo[index], control);
    	}
    	return erg;
    }
    
	@Override
	public int size() {
		return 4;
	}

	@Override
	public Operation get(int index) {
		switch (index) {
		case 0:return function;
		case 1:return differentations;
		case 2:return variables;
		case 3:return point;
		default:
			throw new IndexOutOfBoundsException();
		}
	}

	@Override
	public String getFunctionName() {
		return "taylor_polynom";
	}

	@Override
	public Operation getInstance(List<Operation> subclasses) {
		return new TaylorPolynomOperation(subclasses.get(0), subclasses.get(1), subclasses.get(2), subclasses.get(3));
	}
}
