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
package maths.data;


import java.util.AbstractList;
import java.util.Arrays;
import java.util.List;

import geometry.Matrixd;
import maths.MengenOperation;
import maths.Operation;
import maths.functions.atomic.EqualsOperation;
import maths.variable.VariableAmount;
import util.data.DoubleList;
import util.data.IntegerList;


/** 
* @author  Paul Stahr
* @version 04.02.2012
*/

public final class ArrayOperation extends MengenOperation
{
    private final Operation subClasses[];
    private int unprimitiveCount = 0;
    public final int length;
    
    public static final Operation EMPTY_ARRAY_OPERATION = new ArrayOperation(EMPTY_OPERATION_ARRAY);
    
    public static class ArrayCreator2
    {
    	Operation op[];
    	public final void create(int length)
    	{
    		op = new Operation[length];
    	}
    	
    	public final void set(int index, Operation op)
    	{
    		this.op[index] = op;
    	}
    	
    	public final ArrayOperation getArray()
    	{
    		 int unprimitiveCount = 0;
             for (int i=0;i<this.op.length;i++){
             	if (!(this.op[i]).isPrimitive())
             		unprimitiveCount++;
             }
    		return new ArrayOperation(this.op, unprimitiveCount);
    	}
    }

    public static abstract class ArrayCreator extends AbstractList<Operation>{
    	private final int length;
    	
    	public ArrayCreator(int length){
    		this.length = length;
    	}
    	
    	@Override
		public abstract Operation get(int index);
    
    	@Override
		public int size(){
    		return length;
    	}
    	
    	public ArrayOperation getArray(){
        	Operation operations[] = new Operation[length];
            int unprimitiveCount = 0;
            for (int i=0;i<length;i++){
            	if (!(operations[i] = get(i)).isPrimitive())
            		unprimitiveCount++;
            }
            return new ArrayOperation(operations, unprimitiveCount);
    	}
    }
    
    public static abstract class MatrixCreator{
    	private final int width, height;
    	
    	public MatrixCreator(int width, int height){
    		this.width = width;
    		this.height = height;
    	}
    	
    	public abstract Operation get(int width, int height);
    
    	public int width(){
    		return width;
    	}
    	
    	public int height(){
    		return height;
    	}
    	
    	public ArrayOperation getArray(){
    		Operation operation[] = new Operation[height];
            for (int i=0;i<height;i++){
                int unprimitiveCount = 0; 
            	Operation op[] = new Operation[width];
            	for (int j=0;j<width;j++){
                	if (!(op[j] = get(i, j)).isPrimitive())
                		unprimitiveCount ++;
            	}
            	operation[i] = new ArrayOperation(op, unprimitiveCount);
            }
            return new ArrayOperation(operation, height);
    	}
    }
    
    public ArrayOperation(int length, Operation op)
    {
    	subClasses = new Operation[length];
    	Arrays.fill(subClasses, op);
    	unprimitiveCount = op.isPrimitive() ? 0 : length;
    	this.length = length;
    }
    
    public static ArrayOperation getInstance(Operation operations[][]){
    	final Operation erg[] = new Operation[operations.length];
    	for (int i=0;i<erg.length;i++)
    		erg[i] = new ArrayOperation(operations[i]);
    	return new ArrayOperation(erg);
    }
    
    public static ArrayOperation getInstance(Operation[] a){
    	return new ArrayOperation(a.clone());
    }
    
	@Override
	public Operation getInstance(List<Operation> subclasses) {
		return new ArrayOperation(subclasses);
	}
    
    public ArrayOperation (ArrayOperation ao){
    	length = (subClasses = ao.subClasses.clone()).length;
    	unprimitiveCount = ao.unprimitiveCount;
    }
    
    public ArrayOperation(double op[])
    {
    	subClasses = new Operation[this.length = op.length];
    	for (int i = 0; i < length; ++i)
    	{
    		subClasses[i] = new RealDoubleOperation(op[i]);
    	}
    }    

    public ArrayOperation(float op[])
    {
    	subClasses = new Operation[this.length = op.length];
    	for (int i = 0; i < length; ++i)
    	{
    		subClasses[i] = new RealDoubleOperation(op[i]);
    	}
    }
    
    public ArrayOperation(DoubleList op)
    {
    	subClasses = new Operation[this.length = op.size()];
    	for (int i = 0; i < length; ++i)
    	{
    		subClasses[i] = new RealDoubleOperation(op.getD(i));
    	}
    }
    
    public ArrayOperation(IntegerList op)
    {
    	subClasses = new Operation[this.length = op.size()];
    	for (int i = 0; i < length; ++i)
    	{
    		subClasses[i] = new RealLongOperation(op.getI(i));
    	}
    }
    
    public ArrayOperation(Matrixd op)
    {
    	subClasses = new Operation[this.length = op.rows()];
    	for (int i = 0; i < op.rows(); ++i)
    	{
    		Operation row[] = new Operation[op.cols()];
    		for (int col = 0; col < row.length; ++col)
    		{
    			row[col] = new RealDoubleOperation(op.get(i, col));
    		}
    		subClasses[i] = new ArrayOperation(row, 0);
    	}
    }
    
    public ArrayOperation(long op[])
    {
    	subClasses = new Operation[this.length = op.length];
    	for (int i = 0; i < length; ++i)
    	{
    		subClasses[i] = new RealLongOperation(op[i]);
    	}
    }
    
    public ArrayOperation(byte op[])
    {
    	subClasses = new Operation[this.length = op.length];
    	for (int i = 0; i < length; ++i)
    	{
    		subClasses[i] = RealLongOperation.intern(op[i]);
    	}
    }
    
    public ArrayOperation(int op[])
    {
    	subClasses = new Operation[this.length = op.length];
    	for (int i = 0; i < length; ++i)
    	{
    		subClasses[i] = new RealLongOperation(op[i]);
    	}
    }
    
    public ArrayOperation(String op[])
    {
    	subClasses = new Operation[this.length = op.length];
    	for (int i = 0; i < length; ++i)
    	{
    		subClasses[i] = new StringOperation(op[i]);
    	}
    }
    
    public ArrayOperation(char op[])
    {
    	subClasses = new Operation[this.length = op.length];
    	for (int i = 0; i < length; ++i)
    	{
    		subClasses[i] = CharacterOperation.getInstance(op[i]);
    	}
    }
    
    public ArrayOperation(boolean op[])
    {
    	subClasses = new Operation[this.length = op.length];
    	for (int i = 0; i < length; ++i)
    	{
    		subClasses[i] = BooleanOperation.get(op[i]);
    	}
    }
    
    private ArrayOperation (Operation operations[]){
    	this.length = (subClasses = operations).length;
        for (Operation unterklasse : subClasses)
            if (!unterklasse.isPrimitive())
                unprimitiveCount++;
    }
    
    private ArrayOperation(Operation operations[], int unprimitiveCount){
    	this.length = (subClasses = operations).length;
        this.unprimitiveCount = unprimitiveCount;
    }

	public ArrayOperation(List<? extends Operation> list) {
		length = list.size();
		subClasses = new Operation[length];
        unprimitiveCount = 0;
        for (int i=0;i<length;i++){
        	if (!(subClasses[i] = list.get(i)).isPrimitive())
        		unprimitiveCount++;
        }
	}

	public ArrayOperation(double[][] data) {
		length = data.length;
		subClasses = new Operation[length];
		unprimitiveCount = 0;
		for (int i = 0; i < length; ++i)
		{
			subClasses[i] = new ArrayOperation(data[i]);
		}
	}

	@Override
	public final boolean isArray(){
		return true;
	}
	    
	@Override
	public final Operation calculate (VariableAmount object, CalculationController control){
        if (unprimitiveCount == 0)
            return this;
        final Operation erg[] = new Operation[subClasses.length];
        int unprimitive = 0;
        for (int i=0;i<erg.length;i++)
        	if (!(erg[i] = subClasses[i].calculate(object, control)).isPrimitive())
        		unprimitive++;
        return new ArrayOperation(erg, unprimitive);
    }

    public final Operation[] toArray(){
        return subClasses.clone();
    }

    public final Operation[] copySubClasses(Operation operations[]){
    	if (operations.length < length)
    		return toArray();
    	System.arraycopy(subClasses, 0, operations, 0, length);
    	return operations;
    }
    
    public final ArrayOperation set (int index, Operation op){
    	if (op.isPrimitive()){
    		if(!subClasses[index].isPrimitive())
    			unprimitiveCount --;
    	}else{
    		if (subClasses[index].isPrimitive())
    			unprimitiveCount ++;
    	}
        subClasses[index] = op;
        return this;
    }

    
	@Override
	public final int isElementOf(Operation element){
        boolean isNaN = false;
        for (Operation unterklasse : subClasses){
            final Operation erg = EqualsOperation.calculate(element, unterklasse);
            if (!(erg.isBoolean()))
            	isNaN = true;
            else if (erg.booleanValue())
                return 1;
        }
        return isNaN ? 0 : -1;
    }
    
    
	@Override
	public final StringBuilder toString (Print type, StringBuilder stringBuilder){
    	switch(type){
	    	case CALGRAPH:{
	        	if (subClasses.length == 0)
	        		return stringBuilder.append('{').append('}');
	            subClasses[0].toString(type, stringBuilder.append('{'));
	            for (int i=1;i<subClasses.length;i++)
	            	subClasses[i].toString(type, stringBuilder.append(','));
	            return stringBuilder.append('}');
	    	}case LATEX:{
	    		if (subClasses.length==0)
	        		return stringBuilder.append('\\').append('{').append('\\').append('}');
				if (subClasses[0].isArray() && ((ArrayOperation)subClasses[0]).length > 0){
					toMat:{
			    		int length=((ArrayOperation)subClasses[0]).length;
						if (length < 0)
							break toMat;
			    		for (int i=1;i<subClasses.length;i++)
			    			if (!(subClasses[i].isArray() && ((ArrayOperation)subClasses[i]).length == length))
			    				break toMat;
						stringBuilder.append('\\').append("begin").append('{').append("pmatrix").append('}');
						for (int i=0;i<subClasses.length;i++){
							if (i!=0)
								stringBuilder.append('\\').append('\\');
							((ArrayOperation)subClasses[i]).subClasses[0].toString(type, stringBuilder);
							for (int j=1;j<length;j++)
								((ArrayOperation)subClasses[i]).subClasses[j].toString(type, stringBuilder.append('&'));					
						}
						return stringBuilder.append('\\').append("end").append('{').append("pmatrix").append('}');
					}
				}
	            subClasses[0].toString(type, stringBuilder.append('\\').append('{'));
	            for (int i=1;i<subClasses.length;i++)
	            	subClasses[i].toString(type, stringBuilder.append(','));
	            return stringBuilder.append('\\').append('}');				
	    	}case OPEN_OFFICE:{
	    		if (subClasses.length==0)
	        		return stringBuilder.append('\\').append('{').append('\\').append('}');
				if (subClasses[0].isArray() && ((ArrayOperation)subClasses[0]).length > 0){
					toMat:{
			    		int length=((ArrayOperation)subClasses[0]).length;
						if (length < 0)
							break toMat;
			    		for (int i=1;i<subClasses.length;i++)
			    			if (!(subClasses[i].isArray() && ((ArrayOperation)subClasses[i]).length == length))
			    				break toMat;
						stringBuilder.append("left").append('(').append("matrix").append('{');
						for (int i=0;i<subClasses.length;i++){
							if (i!=0)
								stringBuilder.append('#').append('#');
							((ArrayOperation)subClasses[i]).subClasses[0].toString(type, stringBuilder);
							for (int j=1;j<length;j++)
								((ArrayOperation)subClasses[i]).subClasses[j].toString(type, stringBuilder.append('#'));
						}
						return stringBuilder.append('}').append("right").append(')');
					}
				}
	            subClasses[0].toString(type, stringBuilder.append('\\').append('{'));
	            for (int i=1;i<subClasses.length;i++)
	            	subClasses[i].toString(type, stringBuilder.append(','));
	            return stringBuilder.append('\\').append('}');
	        }default:{
	    		throw new IllegalArgumentException();
	    	}
    	}
    }
    
	
	@Override
	public int size() {
		return subClasses.length;
	}

	
	@Override
	public Operation get(int index) {
		return subClasses[index];
	}
	
	
	@Override
	public boolean isPrimitive(){
		return unprimitiveCount == 0;
	}
}
