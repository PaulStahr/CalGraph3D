package maths.functions;

import java.util.List;

import maths.Operation;
import maths.VariableAmount;

public class NormOperation extends FunctionOperation {
	Operation a;
	
	@Override
	public String getFunctionName() {
		return "norm";
	}

    public NormOperation (Operation a){
    	if ((this.a = a) == null)
    		throw new NullPointerException();
    }

	@Override
	public Operation getInstance(List<Operation> subclasses) {
		return new NormOperation(subclasses.get(0));
	}
	
    public static final Operation calculate (final Operation a){
        return new NormOperation(a);
    }


	@Override
	public Operation calculate(VariableAmount object, CalculationController control) {
		return this;
	}

	@Override
	public int size() {
		return 1;
	}

	@Override
	public Operation get(int index) {
		switch (index){
		case 0: return a;
		default:throw new ArrayIndexOutOfBoundsException(index);
	}
	}

}
