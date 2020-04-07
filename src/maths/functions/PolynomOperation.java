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
import maths.VariableAmount;
import maths.data.Characters;
import maths.data.RealLongOperation;

public class PolynomOperation extends Operation{
	private final int exponent[];
	private final Operation polynom[];
	private final Operation variable;
	
	public PolynomOperation(PolynomOperation po, Operation variable){
		exponent = po.exponent;
		polynom = po.polynom;
		this.variable = variable;
	}
	
	public PolynomOperation(int exponents[], Operation polynom[], Operation variable){
		if (exponents.length != polynom.length)
			throw new IllegalArgumentException("Exponents and Polynom must have same size");
		
		this.exponent = new int[exponents.length];
		this.polynom = new Operation[exponents.length];
		for (int i=0;i<exponents.length;i++){
			if (i==0){
				if (exponents[0] < 0)
					throw new IllegalArgumentException("Exponents must be positive");
			}else{
				if (exponents[i] <= exponents[i-1])
					throw new IllegalArgumentException("Exponents must be in the correct order");
			}
				
			this.exponent[i] = exponents[i];
			if (!((this.polynom[i] = polynom[i]).isComplexFloatingNumber()))
				throw new IllegalArgumentException("Not a polynom");
		}
		this.variable = variable;
	}
	
	
	@Override
	public Operation calculate(VariableAmount object, CalculationController control) {
		Operation op = variable.calculate(object, control);
		if (op.isComplexFloatingNumber()){
			return calculate(polynom, exponent, op);
		}
		return new PolynomOperation(this, op);
	}
	
	private static final Operation calculate(Operation polynom[], int exponent[], Operation variable){
		Operation erg = RealLongOperation.ZERO;
		for (int i=exponent.length-1;i>=0;i--){
			
		}
		return erg;
	}
	
	
	@Override
	public StringBuilder toString(Print type, StringBuilder stringBuilder) {
		for (int i=0;i<polynom.length;i++){
			if (i != 0)
				stringBuilder.append(Characters.ADD);
			polynom[i].toString(stringBuilder);
			if (exponent[i] != 0)
				stringBuilder.append(Characters.toHighString(String.valueOf(exponent[i])));
		}
		return stringBuilder;
	}
	
	
	@Override
	public int size() {
		return polynom.length * 2 + 1;
	}
	
	@Override
	public Operation get(int index) {
		if (index == 0)
		{
			return variable;
		}
		if (index < polynom.length + 1)
		{
			return polynom[index - 1];
		}
		if (index < polynom.length * 2 + 1)
		{
			return new RealLongOperation(exponent[index - polynom.length - 1]);
		}
		throw new ArrayIndexOutOfBoundsException(index);
	}

	@Override
	public Operation getInstance(List<Operation> subclasses) {
		int length = subclasses.size() / 2;
		if (length * 2 + 1 != subclasses.size())
		{
			throw new IllegalArgumentException();
		}
		Operation variable = subclasses.get(0);
		int exponent[] = new int[length];
		Operation polynom[] = new Operation[length];
		for (int i=0;i<length;i++){
			polynom[i] = subclasses.get(i + 1);
			exponent[i] = (int)subclasses.get(i + length + 1).longValue();
		}
		return new PolynomOperation(exponent, polynom, variable);
	}
}
