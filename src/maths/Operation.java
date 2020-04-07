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

import maths.algorithm.OperationCalculate.OperationList;

/** 
* @author  Paul Stahr
* @version 04.02.2012
*/
public abstract class Operation
{
	public static enum Print{
		CALGRAPH, LATEX, OPEN_OFFICE;
	}
	
	public static final short BITMASK_INT_REAL = 0x1, BITMASK_RATIONAL_REAL = 0x2, BITMASK_FLOAT_REAL = 0x4, BITMASK_INT_COMPLEX = 0x8, BITMASK_RATIONAL_COMPLEX = 0x10, BITMASK_FLOAT_COMPLEX = 0x20, BITMASK_BOOLEAN = 0x40, BITMASK_CHARACTER = 0x80, BITMASK_STRING = 0x100; 
	
	public int getTypeBitmask(){
		return 0;
		/*return (isRealIntegerNumber() ? BITMASK_INT_REAL : 0)
				| (isRealRationalNumber() ? BITMASK_RATIONAL_REAL : 0)
				| (isRealFloatingNumber() ? BITMASK_FLOAT_REAL : 0)
				| (isComplexIntegerNumber() ? BITMASK_INT_COMPLEX : 0)
				| (isComplexRationalNumber() ? BITMASK_RATIONAL_COMPLEX : 0)
				| (isComplexFloatingNumber() ? BITMASK_FLOAT_COMPLEX : 0)
				| (isBoolean() ? BITMASK_BOOLEAN : 0)
				| (isCharacter() ? BITMASK_CHARACTER : 0)
				| (isString() ? BITMASK_STRING : 0);*/
	}
//		return BITMASK_INT_REAL | BITMASK_RATIONAL_REAL | BITMASK_FLOAT_REAL | BITMASK_INT_COMPLEX | BITMASK_RATIONAL_COMPLEX | BITMASK_FLOAT_COMPLEX | BITMASK_BOOLEAN | BITMASK_CHARACTER | BITMASK_STRING;

	public boolean isRealFloatingNumber(){
		return false;
	}
	
	public boolean isRealIntegerNumber(){
		return false;
	}
	
	public boolean isRealRationalNumber(){
		return false;
	}
	
	public boolean isComplexFloatingNumber(){
		return false;
	}
	
	public boolean isComplexRationalNumber() {
		return false;
	}
	
	public boolean isComplexIntegerNumber(){
		return false;
	}
	
	public boolean isString(){
		return false;
	}
	
	public boolean isArray(){
		return false;
	}
	
	public boolean isBoolean(){
		return false;
	}
	
	public boolean isCharacter(){
		return false;
	}
	
    public boolean isZero(){
    	return false;
    }
    
    public boolean isNaN(){
    	return false;
    }
    
    public boolean isIntegral(){
    	return false;
    }
        
    public boolean isPositive(){
		throw new RuntimeException("Not a number");
    }
    
	public boolean isNegative() {
		throw new RuntimeException("Not a number");
	}
    
	public Operation getNegative(){
		throw new RuntimeException("Not a number");
	}
	
	public Operation getInvers(){
		throw new RuntimeException("Not a number");
	}
	
    /**
     * Gibt den double Wert der Operation zur\u00FCck
     */
    public double doubleValue(){
        return Double.NaN;
    }

    /**
     * Gibt den imaginären double-Wert der Operation zurück
     */
    public double doubleValueImag(){
    	return Double.NaN;
    }
    
    public long longNumeratorValue(){
        throw new RuntimeException("This is not a long");
    }

    public long longDenumeratorValue(){
        throw new RuntimeException("This is not a long");
    }
    
    public long longNumeratorValueImag(){
        throw new RuntimeException("This is not a long");
    }

    public long longDenumeratorValueImag(){
        throw new RuntimeException("This is not a long");
    }
    


    /**
     * Gibt den long Wert der Operation zur\u00FCck
     */
    public long longValue(){
        throw new RuntimeException(getClass().getName() + " is not a long");
    }
    

    /**
     * Gibt den imaginären long Wert der Operation zur\u00FCck
     */
    public long longValueImag(){
        throw new RuntimeException("This is not a long");
    }

    /**
     * Gibt den boolean Wert der Operation zur\u00FCck
     */
    public boolean booleanValue(){
        throw new RuntimeException(getClass() + " is not a boolean");
    }

    public String stringValue(){
        throw new RuntimeException("This is not a String");
    }
    
    public abstract Operation getInstance(List<Operation> subclasses);
    
    public abstract Operation calculate (VariableAmount object, CalculationController control);

    /**
     * Erzeugt einen String aus der Operation.
     * Wird der String wieder zu einer Operation kompiliert so entsteht die gleiche Operation
     * @return String rechnung
     */
    
	@Override
	public String toString(){
    	return toString(Print.CALGRAPH);
    }

    public String toString(Print type){
    	return toString(type, new StringBuilder()).toString();    	
    }
    
    /**
     * Die Rechnung wird an den \u00FCbergebenen StringBuilder angeh\u00E4ngt
     * @param stringBuilder
     * @return
     */
    public StringBuilder toString(StringBuilder stringBuilder){
    	return toString(Print.CALGRAPH, stringBuilder);
    }
    
    /**
     * Die Rechnung wird an den \u00FCbergebenen StringBuilder angeh\u00E4ngt
     * @param type
     * @param stringBuilder
     * @return
     */
    public abstract StringBuilder toString (Print type, StringBuilder stringBuilder);
    
    /**
     * Returns the priority of the operation.
     * This is important for correct braces
     */
    public int getPriority(){
        return 8;
    }

    /**
     * Gibt zur\u00FCck ob dies ein Primitiver Datentyp ist.
     * Ein primitiver datentyp gibt sich durch calculate() und reduce() immer nur selbst zurueck.
     * Primitive Datentypen sind: LongOperation, CharacterOperation, DoubleOperation, BooleanOperation, StringOperation
     */
    public boolean isPrimitive(){
        return false;
    }
    /**
     * Gibt zur&uuml;ck ob die beiden Rechnungen gleich sind.
     * Gibt true zurueck, nur wenn die beiden Rechnungen sicher gleich sind. Sonst immer false.
     */
   
    @Override
	public boolean equals(Object object){
    	if (getClass() != object.getClass())
    		return false;
        Operation op = ((Operation)object);
        if (size() != op.size())
        	return false;
        for (int i=0;i<size();i++)
        	if (!get(i).equals(op.get(i)))
        		return false;
        return true;
    }
    
    public static final Operation[] EMPTY_OPERATION_ARRAY = new Operation[0];
    
    public static interface CalculationController{
    	public boolean getStopFlag();
    	
    	public void returnToChached(OperationList ol);
    	
    	public OperationList getOperationList();
    	
    	public boolean calculateRandom();
    	
    	public boolean calculateLoop();

		public boolean connectEmptyVariables();
		
		public VariableAmount getVariables();
    }
    
    public abstract int size();
    
    public abstract Operation get(int index);
}
