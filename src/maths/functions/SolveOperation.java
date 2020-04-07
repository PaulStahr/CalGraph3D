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
import maths.UserVariableOperation;
import maths.VariableAmount;
import maths.data.RealLongOperation;
import maths.data.StringId;
import maths.data.StringId.StringIdObject;
import maths.functions.atomic.AdditionOperation;
import maths.functions.atomic.DivisionOperation;
import maths.functions.atomic.EqualsOperation;
import maths.functions.atomic.MultiplicationOperation;
import maths.functions.atomic.NegativeOperation;
import maths.functions.atomic.NotOperation;
import maths.functions.atomic.PowerOperation;
import maths.functions.atomic.SubtractionOperation;

/** 
* @author  Paul Stahr
* @version 04.02.2012
*/

public final class SolveOperation extends FunctionOperation {
	public final Operation equation, variable;
	
	public SolveOperation (Operation equation, Operation variable){
		if ((this.equation = equation) == null || (this.variable = variable)==null)
			throw new NullPointerException();
	}

	public SolveOperation (Operation equation, StringId.StringIdObject variable){
		if ((this.equation = equation) == null)
			throw new NullPointerException();
		this.variable = new UserVariableOperation(variable);
	}

	public SolveOperation (Operation equation, String variable){
		if ((this.equation = equation) == null)
			throw new NullPointerException();
		this.variable = new UserVariableOperation(variable);
	}

	
	public static Operation calculate(Operation equation, Operation variable, CalculationController control) {
		if (!(equation instanceof EqualsOperation && variable instanceof UserVariableOperation))
			return new SolveOperation(equation, variable);
		final StringId.StringIdObject variableName = ((UserVariableOperation)variable).nameObject;
		Operation left = equation.get(0),				right = equation.get(1);
		final int leftI = hasVariable(left, variableName), rightI = hasVariable(right, variableName);
		/*Variable nicht oder auf beiden Seiten gefunden*/
		if ((leftI == -1 && rightI == -1) || (leftI == 1 && rightI == 1))
			return new SolveOperation(equation, variable);
		/*Tauschen, falls Variable auf der rechten Seite*/
		if (rightI == 1){
			final Operation tmp = right;
			right = left;
			left = tmp;
		}
		while (true){
			boolean changed = false;
			switch (left.size()){
				case 0:{
					break;
				}case 1:{
					Operation subc0 = left.get(0);
					if (left instanceof NegativeOperation){
						left = subc0;
						right = NegativeOperation.calculate(right, control);
						changed = true;
					}else if (left instanceof PowerOperation.SquareRootOperation){
						right = PowerOperation.getInstance(right, RealLongOperation.POSITIVE_TWO).calculate(null, control);
						left = subc0;
						changed = true;
					}else if (left instanceof NotOperation){
						right = NotOperation.calculate(right);
						left = subc0;
						changed = true;
					}
					break;
				}case 2:{
					Operation subc0 = left.get(0), subc1 = left.get(1);
					final int subc0HasVariable = hasVariable(subc0, variableName), subc1HasVariable = hasVariable(subc1, variableName);
					
					if (left instanceof AdditionOperation){
						if (subc0HasVariable == -1){
							left = subc1;
							right = SubtractionOperation.calculate(right, subc0, control);
							changed = true;
						}else if(subc1HasVariable == -1){
							left = subc0;
							right = SubtractionOperation.calculate(right, subc1, control);					
							changed = true;
						}
					}else if (left instanceof SubtractionOperation){
						if (subc0HasVariable == -1){
							left = subc1;
							right = SubtractionOperation.calculate(subc0, right, control);
							changed = true;
						}else if(subc1HasVariable == -1){
							left = subc0;
							right = AdditionOperation.calculate(right, subc1, control);					
							changed = true;
						}
					}else if (left instanceof MultiplicationOperation){
						if (subc0HasVariable == -1){
							left = subc1;
							right = DivisionOperation.calculate(right, subc0, control);
							changed = true;
						}else if(subc1HasVariable == -1){
							left = subc0;
							right = DivisionOperation.calculate(right, subc1, control);					
							changed = true;
						}
					}else if (left instanceof DivisionOperation){
						if (subc0HasVariable == -1){
							left = subc1;
							right = DivisionOperation.calculate(subc0, right, control);
							changed = true;
						}else if(subc1HasVariable == -1){
							left = subc0;
							right = MultiplicationOperation.calculate(right, subc1, control);					
							changed = true;
						}
					}
					break;
				}
			}
			if (!changed){
				final Operation erg = new EqualsOperation(left, right);
				if (left instanceof UserVariableOperation)
					return erg;
				return new SolveOperation(erg, variable);
			}
		}
	}

	/**
	 * Gibt -1 bei nicht gefunden, 0 bei unsicher und 1 bei gefunden zurueck
	 * @param o
	 * @param variable
	 * @return ergebnis
	 */
	private static int hasVariable (Operation o, StringIdObject variable){
		switch (o.size()){
			case 0:{
				if (o instanceof UserVariableOperation)
					return variable == ((UserVariableOperation)o).nameObject? 1 : -1;
				else if (o.isPrimitive())
					return -1;
				break;
			}case 1:{
				if (o instanceof NegativeOperation || o instanceof PowerOperation.SquareRootOperation || o instanceof NotOperation)
					return hasVariable(o.get(0), variable);
				break;
			}case 2:{
				if (o instanceof AdditionOperation || o instanceof SubtractionOperation || o instanceof MultiplicationOperation || o instanceof DivisionOperation){
					int a = hasVariable(o.get(0), variable), b = hasVariable(o.get(1), variable);
					return a == 1 || b == 1 ? 1 : a == -1 && b == -1 ? -1 : 0;
				}
			}
		}
		return 0;
	}
	
	
	@Override
	public final int size() {
		return 2;
	}

	
	@Override
	public final Operation get(int index) {
		switch (index){
			case 0: return equation;
			case 1: return variable;
			default:throw new ArrayIndexOutOfBoundsException(index);
		}
	}
    
	
	@Override
	public String getFunctionName() {
		return "solve";
	}

	@Override
	public Operation calculate(VariableAmount object, CalculationController control) {
		return calculate(equation.calculate(object, control), variable, control);
	}

	@Override
	public Operation getInstance(List<Operation> subclasses) {
		return new SolveOperation(subclasses.get(0), subclasses.get(1));
	}
}
