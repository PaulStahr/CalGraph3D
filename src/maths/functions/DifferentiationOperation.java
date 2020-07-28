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
import maths.data.ArrayOperation;
import maths.data.RealLongOperation;
import maths.data.StringId;
import maths.functions.atomic.AdditionOperation;
import maths.functions.atomic.DivisionOperation;
import maths.functions.atomic.HigherOperation;
import maths.functions.atomic.LowerOperation;
import maths.functions.atomic.MultiplicationOperation;
import maths.functions.atomic.NegativeOperation;
import maths.functions.atomic.PowerOperation;
import maths.functions.atomic.PowerOperation.SquareOperation;
import maths.functions.atomic.SubtractionOperation;
import maths.functions.hyperbolic.ArcTangensOperation;
import maths.functions.hyperbolic.CosinusHyperbolicOperation;
import maths.functions.hyperbolic.CosinusOperation;
import maths.functions.hyperbolic.SinusHyperbolicOperation;
import maths.functions.hyperbolic.SinusOperation;
import maths.functions.hyperbolic.TangensOperation;
import maths.functions.interators.SumIteratorOperation;
import maths.variable.UserVariableOperation;
import maths.variable.VariableAmount;


public class DifferentiationOperation extends FunctionOperation {
	public final Operation a, b;
	
	public DifferentiationOperation(Operation a, Operation b){
    	if ((this.a = a) == null || (this.b = b) == null)
    		throw new NullPointerException();
	}
	
	
	@Override
	public Operation calculate(VariableAmount object, CalculationController control) {
		return calculate(a.calculate(object, control), b.calculate(object, control), control);
	}

	public static final Operation calculate(Operation a, Operation b, CalculationController control){
		if (!(b instanceof UserVariableOperation)){
			Operation erg = OperationCalculate.standardCalculations(a, b);
			if (erg != null)
				return erg;
			return new DifferentiationOperation(a,b);
		}
		return calculate(a, ((UserVariableOperation)b).nameId, control);
	}
	
	private static final Operation calculate(final Operation a, final int nameId, final CalculationController control){
		if (a.isArray())
			return new ArrayOperation.ArrayCreator(a.size()){
				@Override
				public Operation get(int index) {
					return calculate(a.get(index), nameId, control);
				}
			}.getArray();
		switch(a.size()){
			case 0:{
				if (a instanceof UserVariableOperation)
					return ((UserVariableOperation)a).nameId == nameId ? RealLongOperation.POSITIVE_ONE : RealLongOperation.ZERO;
				if (a.isComplexFloatingNumber())
					return RealLongOperation.ZERO;
				break;
			}case 1:{
				final Operation s0 = a.get(0);
				if (a instanceof SinusOperation)
					return MultiplicationOperation.calculate(CosinusOperation.calculate(s0), calculate(s0,nameId, control), control);
				if (a instanceof CosinusOperation)
					return MultiplicationOperation.calculate(NegativeOperation.calculate(SinusOperation.calculate(s0), control), calculate(s0,nameId, control), control);
				if (a instanceof TangensOperation)
					return DivisionOperation.calculate(calculate(s0, nameId, control), SquareOperation.calculate(CosinusOperation.calculate(s0), control), control);
				if (a instanceof ArcTangensOperation)
					return DivisionOperation.calculate(calculate(s0, nameId, control), AdditionOperation.calculate(SquareOperation.calculate(s0, control), RealLongOperation.POSITIVE_ONE, control), control);
				if (a instanceof SinusHyperbolicOperation)
					return MultiplicationOperation.calculate(s0, CosinusHyperbolicOperation.calculate(a), control);
				if (a instanceof CosinusHyperbolicOperation)
					return MultiplicationOperation.calculate(s0, SinusHyperbolicOperation.calculate(a), control);
				if (a instanceof NegativeOperation)
					return NegativeOperation.calculate(calculate(s0, nameId, control), control);
				if (a instanceof AbsoluteOperation)
					return MultiplicationOperation.calculate(SignOperation.calculate(s0), calculate(s0,nameId, control), control);			
				if (a instanceof SinusHyperbolicOperation)
					return MultiplicationOperation.calculate(CosinusHyperbolicOperation.calculate(s0), calculate(s0,nameId, control), control);
				if (a instanceof CosinusHyperbolicOperation)
					return MultiplicationOperation.calculate(SinusHyperbolicOperation.calculate(s0), calculate(s0,nameId, control), control);
				if (a instanceof LogarithmOperation)
					return DivisionOperation.calculate(calculate(s0, nameId, control), s0, control);
				if (a instanceof NormOperation)
					return DivisionOperation.calculate(MultiplicationOperation.calculate(s0, calculate(s0, nameId, control), control), NormOperation.calculate(s0), control);
				break;
			}case 2:{
				final Operation s0 = a.get(0), s1 = a.get(1);
				if (a instanceof MultiplicationOperation)
					return AdditionOperation.calculate(MultiplicationOperation.calculate(calculate(s0, nameId, control), s1, control), MultiplicationOperation.calculate(s0, calculate(s1, nameId, control), control), control);
				if (a instanceof DivisionOperation)
					return DivisionOperation.calculate(SubtractionOperation.calculate(MultiplicationOperation.calculate(calculate(s0, nameId, control), s1, control), MultiplicationOperation.calculate(s0, calculate(s1, nameId, control), control), control), PowerOperation.calculate(s1, RealLongOperation.POSITIVE_TWO, control), control);
				if (a instanceof PowerOperation.SquareRootOperation)
					return DivisionOperation.calculate(calculate(s0, nameId, control), MultiplicationOperation.calculate(RealLongOperation.POSITIVE_TWO, a, control), control);
				if (a instanceof PowerOperation.ExponentOperation)
					return MultiplicationOperation.calculate(calculate(s1, nameId, control), a, control);
				if (a instanceof PowerOperation){
					if (s1.isComplexFloatingNumber())
						return MultiplicationOperation.calculate(MultiplicationOperation.calculate(s1, PowerOperation.calculate(s0, SubtractionOperation.calculate(s1,RealLongOperation.POSITIVE_ONE, control), control), control), calculate(s0,nameId, control), control);
					return MultiplicationOperation.calculate(calculate(MultiplicationOperation.calculate(s1, LogarithmOperation.calculate(s0), control), nameId, control), a, control);				
				}
				if (a instanceof AdditionOperation)
					return AdditionOperation.calculate(calculate(s0, nameId, control), calculate(s1, nameId, control), control);
				if (a instanceof SubtractionOperation)
					return SubtractionOperation.calculate(calculate(s0, nameId, control), calculate(s1, nameId, control), control);
				if (a instanceof MaximumOperation)
					return new IfOperation(HigherOperation.calculate(s0, s1), calculate(s0, nameId, control), calculate(s1, nameId, control));
				if (a instanceof MinimumOperation)
					return new IfOperation(LowerOperation.calculate(s0, s1), calculate(s0, nameId, control), calculate(s1, nameId, control));
				//if (a instanceof DifferentiationOperation)
					
				break;
			}case 3:{
				final Operation s0 = a.get(0), s1 = a.get(1), s2 = a.get(2);
				if (a instanceof NumericIfOperation)
					return new IfOperation(s0, calculate(s1, nameId, control), calculate(s2, nameId, control));
				if (a instanceof SumIteratorOperation)
					return new SumIteratorOperation(calculate(s0, nameId, control), s1);
				break;
			}
		}
		return new DifferentiationOperation(a,new UserVariableOperation(StringId.getStringAndId(nameId)));
	}
	
	
	@Override
	public String getFunctionName() {
		return "diff";
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
	public Operation getInstance(List<Operation> subclasses) {
		return new DifferentiationOperation(subclasses.get(0), subclasses.get(1));
	}
}
