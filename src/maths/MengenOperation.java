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

import maths.algorithm.Calculate;
import maths.data.Characters;
import maths.data.RealLongOperation;

/** 
* @author  Paul Stahr
* @version 04.02.2012
*/
public abstract class MengenOperation extends Operation
{	
	
	@Override
	public int size() {
		return 0;
	}

	@Override
	public Operation get(int index) {
		throw new ArrayIndexOutOfBoundsException(index);
	}
    
	@Override
	public Operation calculate(VariableAmount object, CalculationController control){
        return this;
    }

	@Override
	public Operation getInstance(List<Operation> subclasses) {
		if (subclasses.size() != 0)
		{
			throw new ArrayIndexOutOfBoundsException();
		}
		return this;
	}
	public static final MengenOperation F = new MengenOperation() {
		
 		@Override
		public final int isElementOf(Operation element){
            if (element.isRealIntegerNumber())
                return Calculate.isFibunacci(element.longValue()) ? 1 : -1;
            if (element.isRealFloatingNumber())
                return element.isIntegral() && Calculate.isFibunacci(element.longValue()) ? 1 : -1;
            return 0;
		}
		
 		public final boolean isArray(){
 			return true;
 		}
		
		@Override
		public final StringBuilder toString(Print type, StringBuilder stringBuilder) {
			return stringBuilder.append(Characters.FIBUNACCI);
		}
	};
	
	/**
     * Nat\u00FCrliche Zahlen.
     */
    public static final MengenOperation N = new MengenOperation(){
            
			@Override
			public int isElementOf(Operation element){
                if (element.isRealIntegerNumber())
                    return element.isPositive() ? 1 : -1;
                if (element.isRealFloatingNumber())
                    return element.isIntegral() && element.doubleValue() > 0 ? 1 : -1;
                return 0;
            }

            
			@Override
			public final StringBuilder toString(Print type, StringBuilder stringBuilder){
                return stringBuilder.append(Characters.N);
            }
        };
    /**
     * Ganze Zahlen.
     */
    public static final MengenOperation Z = new MengenOperation(){
            
			@Override
			public int isElementOf(Operation element){
                if (element.isRealIntegerNumber())
                    return 1;
                if (element.isRealFloatingNumber())
                    return element.isIntegral() ? 1 : -1;
                return 0;
            }

            
			@Override
			public final StringBuilder toString(Print type, StringBuilder stringBuilder){
                return stringBuilder.append(Characters.Z);
            }
        };
    /**
     * Rationale Zahlen.
     */
    public static final MengenOperation Q = new MengenOperation(){
            
			@Override
			public final int isElementOf(Operation element){
                if (element.isRealRationalNumber())
                    return 1;
                return 0;
            }

            
			@Override
			public final StringBuilder toString(Print type, StringBuilder stringBuilder){
                return stringBuilder.append(Characters.Q);
            }
        };
    /**
     * Reelle Zahlen.
     */
    public static final MengenOperation R = new MengenOperation(){
            
			@Override
			public final int isElementOf(Operation element){
                if (element.isRealIntegerNumber())
                    return 1;
                if (element.isRealFloatingNumber())
                    return element.doubleValue() == Double.NEGATIVE_INFINITY || element.doubleValue() == Double.POSITIVE_INFINITY || element.isNaN() ? -1 : 1;
                return element.isPrimitive()? -1 : 0;
            }

            
			@Override
			public final StringBuilder toString(Print type, StringBuilder stringBuilder){
                return stringBuilder.append(Characters.R);
            }
        };
    /**
     * Komplexe Zahlen.
     */
    public static final MengenOperation C = new MengenOperation(){
            
			@Override
			public int isElementOf(Operation element){
				if (element.isPrimitive()){
	                if (element.isComplexIntegerNumber())
	                    return 1;
	                if (element.isRealFloatingNumber())
	                    return element.doubleValue() == Double.NEGATIVE_INFINITY || element.doubleValue() == Double.POSITIVE_INFINITY || element.isNaN() ? -1 : 1;
	                if (element.isComplexFloatingNumber()){
	                	double re = element.doubleValue(), im = element.doubleValueImag();
	                	return re == Double.NEGATIVE_INFINITY || re == Double.POSITIVE_INFINITY || Double.isNaN(re) || im == Double.NEGATIVE_INFINITY || im == Double.POSITIVE_INFINITY || Double.isNaN(im) ? -1 : 1;
	                }
	                return -1;
				}
                return 0;
            }

            
			@Override
			public final StringBuilder toString(Print type, StringBuilder stringBuilder){
                return stringBuilder.append(Characters.C);
            }
        };
    /**
     * Primzahlen.
     */
    public static final MengenOperation P = new MengenOperation(){
            
			@Override
			public int isElementOf(Operation element){
                if (element.isRealIntegerNumber())
                    return Calculate.isPrime(element.longValue()) ? 1 : -1;
                if (element.isRealFloatingNumber())
                    return element.isIntegral() && Calculate.isPrime(element.longValue()) ? 1 : -1;
                return 0;
			}
			
			public Operation get(int index){
				return new RealLongOperation(Calculate.getPrime(index));
			}

            
			@Override
			public final StringBuilder toString(Print type, StringBuilder stringBuilder){
                return stringBuilder.append(Characters.P);
            }
        };

    /**
     * \u00FCberpr\u00FCft ob ein Element teil der Menge ist.
     * @param element das Element das \u00FCberpr\u00FCft werden soll
     * @return isElement -1 = false, 0 = unklar, 1 = true
     */
    public abstract int isElementOf (Operation element);
}
