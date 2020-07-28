package maths.functions;

import java.util.List;

import maths.Operation;
import maths.variable.VariableAmount;
import util.ClassFactory;

public class JavaCommand extends FunctionOperation{
	Operation a;
	
	public JavaCommand(Operation a) {
		if ((this.a = a) == null)
    		throw new NullPointerException();
	}
	
	@Override
	public String getFunctionName() {
		return "java";
	}

	@Override
	public Operation getInstance(List<Operation> subclasses) {
		return new JavaCommand(subclasses.get(0));
	}

    public static final Operation calculate (final Operation a){
    	if (a.isString())
    	{
    		ClassFactory.invokeCommand(a.stringValue());
    	}
    	return new JavaCommand(a);
    }
	
    @Override
	public final Operation calculate (final VariableAmount object, CalculationController control){
        return calculate(a.calculate(object, control));
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
