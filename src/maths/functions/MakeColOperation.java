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
import maths.variable.VariableAmount;
import maths.data.RealLongOperation;


public class MakeColOperation extends FunctionOperation {
	public final Operation a,b,c,d;
	
    public MakeColOperation (final Operation a, final Operation b, final Operation c, final Operation d){
    	if ((this.a = a) == null || (this.b = b) == null || (this.c = c) == null || (this.d = d) == null)
    		throw new NullPointerException();
    }
	
	@Override
	public String getFunctionName() {
		return "makecol";
	}
	
	private static final int getICol(double value){
		if (value <=0)
			return 0;
		if (value >=1)
			return 255;
		return (int)(value*256);
	}
	
	public static final Operation calculate(Operation a, Operation b, Operation c, Operation d){
		if (a.isRealFloatingNumber() && b.isRealFloatingNumber() && c.isRealFloatingNumber() && d.isRealFloatingNumber()){
			return new RealLongOperation(getICol(a.doubleValue())<<24 | getICol(b.doubleValue())<<16 | getICol(c.doubleValue())<<8 | getICol(d.doubleValue()));
		}
		Operation erg = OperationCalculate.standardCalculations(a,b,c,d);
		if (erg != null)
			return erg;
		return new MakeColOperation(a,b,c,d);
	}

	@Override
	public Operation calculate(VariableAmount object, CalculationController control) {
		return calculate(a.calculate(object, control), b.calculate(object, control), c.calculate(object, control), d.calculate(object, control));
	}

	@Override
	public int size() {
		return 4;
	}

	@Override	
	public final Operation get(int index) {
		switch (index){
			case 0: return a;
			case 1: return b;
			case 2: return c;
			case 3: return d;
			default:throw new ArrayIndexOutOfBoundsException(index);
		}
	}

	@Override
	public Operation getInstance(List<Operation> subclasses) {
		return new MakeColOperation(subclasses.get(0), subclasses.get(1), subclasses.get(2), subclasses.get(3));
	}
}
