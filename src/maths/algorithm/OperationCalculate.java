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
package maths.algorithm;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import maths.Controller;
import maths.Operation;
import maths.Operation.CalculationController;
import maths.UserFunctionOperation;
import maths.UserVariableOperation;
import maths.Variable;
import maths.data.ArrayOperation;
import maths.data.RealDoubleOperation;
import maths.data.RealLongOperation;
import maths.data.StringId;
import maths.data.StringId.StringIdObject;
import maths.exception.ExceptionOperation;
import maths.functions.FunctionOperation;
import maths.functions.atomic.AdditionOperation;
import maths.functions.atomic.DivisionOperation;
import maths.functions.atomic.MultiplicationOperation;
import maths.functions.atomic.NegativeOperation;
import maths.functions.atomic.PowerOperation;
import maths.functions.atomic.SubtractionOperation;
import util.data.BinaryTree;
import util.data.SortedIntegerArrayList;
import util.data.DoubleList;

public final class OperationCalculate {
	private static final Comparator<Operation> operationMultiplicationComparator = new Comparator<Operation>() {
		
		@Override
		public int compare(Operation a, Operation b) {
			final StringIdObject aname = getVarName(a);
			final StringIdObject bname = getVarName(b);
			if (aname != null || bname != null)
				return aname != null ? (bname != null ? bname.compareStringTo(aname) : 1) : -1;
			if (a.isString() || b.isString())
				return a.isString() ? (b.isString() ? a.stringValue().compareTo(b.stringValue()) : 1) : -1;
			if (a instanceof FunctionOperation || b instanceof FunctionOperation)
				return a instanceof FunctionOperation ? (b instanceof FunctionOperation ? ((FunctionOperation)a).getFunctionName().compareTo(((FunctionOperation)b).getFunctionName()) : 1) : -1;
			if (a.isComplexFloatingNumber() || b.isComplexFloatingNumber())
				return a.isComplexFloatingNumber() ? (b.isComplexFloatingNumber() ? 0 : 1) : -1;
			return 0;
		}
		
		private final StringIdObject getVarName(Operation o){
			return o instanceof UserVariableOperation ? ((UserVariableOperation)o).nameObject : o instanceof PowerOperation && o.get(0) instanceof UserVariableOperation ? ((UserVariableOperation)o.get(0)).nameObject : null;
		}
	};
	
	private static final Comparator<Operation> operationAdditionComparator = new Comparator<Operation>(){
		@Override
		public int compare(Operation a, Operation b){
			final StringIdObject aname = getVariable(a);
			final StringIdObject bname = getVariable(b);
			if (aname != null || bname != null)
				return aname != null ? (bname != null ? bname.compareStringTo(aname) : 1) : -1;
			if (a.isString())
				return b.isString() ? a.stringValue().compareTo(b.stringValue()) : -1;
			if (b.isString())
				return -1;
			if (a instanceof FunctionOperation)
				return b instanceof FunctionOperation ? ((FunctionOperation)a).getFunctionName().compareTo(((FunctionOperation)b).getFunctionName()) : 1;
			if (b instanceof FunctionOperation)
				return -1;
			if (a.isComplexFloatingNumber())
				return b.isComplexFloatingNumber() ? 0 : 1;
			if (b.isComplexFloatingNumber())
				return -1;
			return 0;
		}
		
		private final StringIdObject getVariable(Operation o){
			if (o instanceof UserVariableOperation)
				return ((UserVariableOperation)o).nameObject;
			if (o instanceof MultiplicationOperation){
				if (o.get(0) instanceof UserVariableOperation)
					return ((UserVariableOperation)o.get(0)).nameObject;
				if (o.get(1) instanceof UserVariableOperation)
					return ((UserVariableOperation)o.get(1)).nameObject;
			}
			return null;
		}
	};
	
	public static Operation calculateBigDivision(OperationList up, OperationList down, CalculationController control){
		Operation erg = calculateBigDivision(up.operations, up.size, down.operations, down.size, control);
        if (control != null){
			control.returnToChached(up);
	        control.returnToChached(down);
        }
		return erg;
	}
	
	public static final Operation calculateBigSubtraction(OperationList up, OperationList down, CalculationController control){
		Operation erg = calculateBigSubtraction(up.operations, up.size, down.operations, down.size, control);
        if (control != null){
			control.returnToChached(up);
	        control.returnToChached(down);
        }
		return erg;		
	}
	
	public static final Operation calculateBigSubtraction(Operation upArray[], int uLength, Operation downArray[], int dLength, CalculationController control){
			if (uLength > upArray.length || dLength > downArray.length)
				throw new IllegalArgumentException();
			uLength = calculateAdditionArray(upArray, uLength, control);
			dLength = calculateAdditionArray(downArray, dLength, control);
	        int i=0, j=0, ii=0, jj=0;
	        while(i < uLength && j < dLength){
	        	Operation up = upArray[i], down = downArray[j];
	        	final int comp = operationAdditionComparator.compare(up, down);
	        	if (comp == 0){
	        		final Operation op = SubtractionOperation.calculate(up, down, control);
	        		if (op instanceof SubtractionOperation){
		        		upArray[ii++] = up;        		
	        		downArray[jj++] = down;     
	        		i++;
        		}else{
	        		upArray[ii] = op;
	       		}
        		j++;
        	}else if (comp < 0){
        		upArray[ii++] = up;
        		i++;
        	}else if (comp > 0){
        		downArray[jj++] = down;
        		j++;
        	}
        }
        System.arraycopy(upArray, i, upArray, ii, uLength-i);
        System.arraycopy(downArray, j, downArray, jj, dLength-j);
        Operation erg = null;
        if ((ii += uLength-i) != 0){
        	if (upArray[0].isComplexFloatingNumber() && upArray[0].isZero()){
        		if (ii > 1)
        			erg = upArray[--ii];
            	while (ii > 1)
            		erg = new AdditionOperation(erg, upArray[--ii]);        		
        	}else{
            	erg = upArray[--ii];
            	while (ii > 0)
            		erg = new AdditionOperation(erg, upArray[--ii]);        		
        	}     		
        }
        if ((jj += dLength-j) != 0){
        	erg = erg == null ? new NegativeOperation(downArray[--jj]) : new SubtractionOperation(erg, downArray[--jj]);
        	while (jj != 0)
        		erg = new SubtractionOperation(erg, downArray[--jj]);
        }
        return erg;
	}
	
	public static final int nodeCount(Operation op){
		int erg = 1;
		for (int i=0;i<op.size();i++)
			erg += nodeCount(op.get(i));
		return erg;
	}
	
    public static final Operation[][] toOperationArray2 (Operation o){
        if (!(o.isArray()))
        	return null;
        ArrayOperation ao = ((ArrayOperation)o);
        final Operation erg[][] = new Operation[ao.length][];
        for (int i=0;i<ao.length;i++){
        	Operation op = ao.get(i);
        	if (!(op.isArray()))
        		return null;
    		erg[i] = ((ArrayOperation)op).toArray();	        		
        }
        return erg;
    }
    
    public static final int[] toIntArray(Operation o)
    {
    	if (!(o.isArray()))
    		return null;
    	int erg[] = new int[o.size()];
    	for (int i = 0; i < erg.length; ++i)
    	{
    		erg[i] = (int)o.get(i).longValue();
    	}
    	return erg;
    }
    
	public static String[] toStringArray(Operation o) {
		if (!(o.isArray()))
    		return null;
    	String erg[] = new String[o.size()];
    	for (int i = 0; i < erg.length; ++i)
    	{
    		erg[i] = o.get(i).stringValue();
    	}
    	return erg;
	}
    
    public static final double[] toDoubleArray(Operation o, double result[])
    {
    	if (!(o.isArray()))
    		return null;
    	if (result == null || o.size() != result.length)
    	{
    		result = new double[o.size()];
    	}
    	for (int i = 0; i < result.length; ++i)
    	{
    		result[i] = o.get(i).doubleValue();
    	}
    	return result;
    }
    
    public static final DoubleList toDoubleArray(Operation o, DoubleList result)
    {
    	if (!(o.isArray()))
    		return null;
    	for (int i = 0; i < o.size(); ++i)
    	{
    		result.setElem(i,o.get(i).doubleValue());
    	}
    	return result;
    }
    
    public static final ArrayOperation toList(final double[][] data){
		return new ArrayOperation(new AbstractList<Operation>(){
			@Override
			public Operation get(final int index) {
				return new ArrayOperation(data[index]);
			}

			@Override
			public int size() {
				return data.length;
			}
		});
	}
    
    private static class OperationListView extends AbstractList<Operation>{
    	OperationTreeItem children[];
		@Override
		public Operation get(int index) {
			return children[index].op;
		}

		@Override
		public int size() {
			return children.length;
		}
    	
    }
    
    private static class OperationTreeItem{
    	OperationTreeItem parent;
    	OperationTreeItem children[];
    	int refs = 1;
    	Operation op;
    	int parentIndex;
    	int size = 1;
    	int inverseDeth = 0;
    	public OperationTreeItem(Operation op, ArrayList<ArrayList<OperationTreeItem>> itemList) {
			this.op = op;
			children = new OperationTreeItem[op.size()];
			for (int i = 0; i < op.size(); ++i)
			{
				OperationTreeItem child = new OperationTreeItem(op.get(i), itemList);
				children[i] = child;
				size += child.size;
				inverseDeth = Math.max(child.inverseDeth + 1, inverseDeth);
				child.parent = this;
				child.parentIndex = i;
			}
			while (itemList.size() <= inverseDeth){
				itemList.add(new ArrayList<OperationTreeItem>());
			}
			itemList.get(inverseDeth).add(this);
		}
    	
    	public void rebuildOperations(OperationListView listView)
    	{
    		for (int i = 0; i < children.length; ++i)
    		{
    			children[i].rebuildOperations(listView);
    		}
    		listView.children = children;
    		op = op.getInstance(listView);
    	}
    	
    	public boolean equals(OperationTreeItem other)
    	{
    		if (this == other)
    		{
    			return true;
    		}
    		if (children.length != other.children.length || op.getClass() != other.op.getClass())
    		{
    			return false;
    		}
    		for (int i = 0; i < children.length; ++i)
    		{
    			if (children[i] != other.children[i])
    			{
    				return false;
    			}
    		}
    		return children.length != 0 || op.equals(other.op);
    	}
    	
    	private StringBuilder toString(StringBuilder strB, int depth){
    		for (int i = 0; i < depth; ++i)
    		{
    			strB.append(' ');
    		}
    		op.toString(strB);
    		strB.append('\n');
    		for (int i = 0; i < children.length; ++i)
    		{
    			children[i].toString(strB, depth + 1);
    		}
    		return strB;
    	}
    	
    	public String toString(){
    		return toString(new StringBuilder(), 0).toString();
    	}
    }
    
    public static Operation substitute(Operation o, ArrayList<Variable> substitutedVariables, ArrayList<Operation> substitutedOperations)
    {
    	ArrayList<ArrayList<OperationTreeItem>> itemList = new ArrayList<ArrayList<OperationTreeItem>>();
    	new OperationTreeItem(o, itemList);
    	for (int i = 0; i < itemList.size(); ++i)
    	{
    		ArrayList<OperationTreeItem> layer = itemList.get(i);
        		
    		for (int j = 0; j < layer.size() - 1; ++j)
    		{
    			OperationTreeItem oti0 = layer.get(j);
    			for (int k = j + 1; k < layer.size(); ++k)
    			{
    				OperationTreeItem oti1 = layer.get(k);
    				if (oti0.equals(oti1))
    				{
    					oti1.parent.children[oti1.parentIndex] = oti0;
    					++oti0.refs;
    					--oti1.refs;
    				}    					
    			}
    		}
    	}
    	int counter = 0;
    	for (int i = itemList.size() - 1; i >= 0; --i)
    	{
    		ArrayList<OperationTreeItem> layer = itemList.get(i);
    		boolean success = false;
    		for (int j = 0; j < layer.size() - 1; ++j)
    		{
    			OperationTreeItem oti = layer.get(j);
    			if (oti.refs > 1)
    			{
    				substitutedOperations.add(oti.op);
    				Variable v = new Variable("tmpvar" + counter);
    				substitutedVariables.add(v);
    				oti.op = new UserVariableOperation(v.nameObject);
    				success = true;
    			}
    		}
    		if (success)
    		{
    			itemList.get(itemList.size() - 1).get(0).rebuildOperations(new OperationListView());
    			return itemList.get(itemList.size() - 1).get(0).op;
    		}
    	}
    	return o;
    }
    
	public static final Operation calculateBigDivision(Operation upArray[], int uLength, Operation downArray[], int dLength, CalculationController control){
		if (uLength > upArray.length || dLength > downArray.length)
			throw new IllegalArgumentException();
        uLength = calculateMultiplicationArray(upArray, uLength, control);
        dLength = calculateMultiplicationArray(downArray, dLength, control);
        int i=0, j=0, ii=0, jj=0;
        while(i <uLength && j < dLength){
        	final Operation up = upArray[i], down = downArray[j];
        	final int comp = operationMultiplicationComparator.compare(up, down);
        	if (comp == 0){
        		final Operation op = DivisionOperation.calculate(up, down, control);
        		if (op instanceof DivisionOperation){
	        		upArray[ii++] = up;        		
	        		downArray[jj++] = down;     
	        		i++;
        		}else{
	        		upArray[ii] = op;
        		}
        		j++;	        			
        	}else if (comp < 0){
        		upArray[ii++] = up;
        		i++;
        	}else if (comp > 0){
        		downArray[jj++] = down;
        		j++;
        	}
        }
        System.arraycopy(upArray, i, upArray, ii, uLength-i);
        System.arraycopy(downArray, j, downArray, jj, dLength-j);
        Operation upOperation = null, downOperation = null;
        if ((ii += uLength-i) != 0){
        	if (upArray[0].isRealFloatingNumber() && upArray[0].doubleValue() == 1){
        		if (ii > 1)
        			upOperation = upArray[--ii];
            	while (ii > 1)
            		upOperation = new MultiplicationOperation(upOperation, upArray[--ii]);        		
        	}else{
        		upOperation = upArray[--ii];
            	while (ii > 0)
            		upOperation = new MultiplicationOperation(upOperation, upArray[--ii]);        		
        	}  		
        }
        if ((jj += dLength-j) != 0){
        	downOperation = downArray[--jj];
        	while (jj != 0)
        		downOperation = new MultiplicationOperation(downArray[--jj], downOperation);
        	return upOperation == null ? new PowerOperation.InversOperation(downOperation) : new DivisionOperation(upOperation, downOperation);
        }
        return upOperation == null ? RealLongOperation.POSITIVE_ONE : upOperation;
   	}
	
	private static final int calculateAdditionArray(Operation array[], int length, CalculationController control){
		if (length < 2)
    		return length;
    	Arrays.sort(array, 0, length, operationAdditionComparator);
        Operation last = array[0];
        int paste = 0;
        for (int read = 1; read<length;read++){
        	Operation act = array[read];
        	final int comp = operationAdditionComparator.compare(last, act);
        	if (comp == 0){
	        	Operation op = AdditionOperation.calculate(last, act, control);
	        	if (!(op instanceof AdditionOperation)){
	        		last = op;
	        		continue;
	        	}
        	}
    		array[paste++]=last;
    		last = act;        		
        }
		array[paste++]=last;
		if (paste != length)
			array[paste]=null;
		return paste;
	}
		
    private static final int calculateMultiplicationArray(Operation array[], int length, CalculationController control){
    	if (length < 2)
    		return length;
    	boolean neg = false;
    	for (int i=0;i<length;i++){
    		Operation op = array[i];
    		if (op instanceof NegativeOperation){
    			op = op.get(0);
    			neg =! neg;
    		}
    		if (op.isRealFloatingNumber() && !op.isPositive()){
    			op = op.getNegative();
    			neg =! neg;
    		}
    		array[i] = op;
    	}
        Arrays.sort(array, 0, length, operationMultiplicationComparator);
        Operation last = array[0];
        int paste = 0;
        /*Fehler beheben (1*x)*/
        for (int read=1; read<length;read++){
        	Operation act = array[read];
        	if (act.isRealFloatingNumber() && act.doubleValue() == 1){
        		last = act;
        		continue;
        	}
        	final int comp = operationMultiplicationComparator.compare(last, act);
        	if (comp == 0){
	        	Operation op = MultiplicationOperation.calculate(last, act, control);
	        	if (!(op instanceof MultiplicationOperation)){
	        		last = op;
	        		continue;
	        	}
        	}
    		array[paste++]=last;
    		last = act;    		
        }
		array[paste++]=last;
		if (paste != length)
			array[paste]=null;
		if (neg)
			array[0] = NegativeOperation.calculate(array[0], control);
		return paste;
    }
    
    private static class MultiplicationArray{
    	int length = 0;
    	Operation exponents[] = new Operation[4];
    	Operation operations[] = new Operation[4];
    }
    
    public static final void fillWithUpAndDowns(Operation op, MultiplicationArray ma, Operation exponent, Controller control){
    	if (op instanceof MultiplicationOperation){
    		fillWithUpAndDowns(op.get(0), ma, exponent, control);
    		fillWithUpAndDowns(op.get(1), ma, exponent, control);
    	}else if (op instanceof DivisionOperation){
    		fillWithUpAndDowns(op.get(0), ma, exponent, control);
    		fillWithUpAndDowns(op.get(1), ma, NegativeOperation.calculate(exponent, control), control);
    	}else if (op instanceof PowerOperation){
    		fillWithUpAndDowns(op.get(0), ma, MultiplicationOperation.calculate(exponent, op.get(1), control), control);
    	}else{
    		ma.operations[ma.length] = op;
    		ma.exponents[ma.length] = exponent;
    		ma.length++;
    	}
    }
    
    public static final void fillWithMultUpAndDowns(Operation a, OperationList up, OperationList down){  	
    	if (a instanceof MultiplicationOperation){
    		fillWithMultUpAndDowns(a.get(0), up, down);	
    		fillWithMultUpAndDowns(a.get(1), up, down);
    	}else if (a instanceof DivisionOperation){
    		fillWithMultUpAndDowns(a.get(0), up, down);	
    		fillWithMultUpAndDowns(a.get(1), down, up);
    	}else{
    		up.add(a);
    	}
    }
    
    public static final void fillWithAddUpAndDowns(Operation a, OperationList up, OperationList down){
    	if (a instanceof AdditionOperation){
    		fillWithAddUpAndDowns(a.get(0), up, down);	
    		fillWithAddUpAndDowns(a.get(1), up, down);
    	}else if (a instanceof SubtractionOperation){
    		fillWithAddUpAndDowns(a.get(0), up, down);	
    		fillWithAddUpAndDowns(a.get(1), down, up);
    	}else if (a instanceof NegativeOperation){
    		fillWithAddUpAndDowns(a.get(0), down, up);
    	}else{
    		up.add(a);
    	}
    }
    
    public static final StringId.StringIdObject[] getVariables(Operation op){
    	List<StringId.StringIdObject> list = getVariables(op, new ArrayList<StringId.StringIdObject>());
    	return list.toArray(new StringId.StringIdObject[list.size()]);
    }
    
    public static final int[] getVariableIds(Operation op){
    	SortedIntegerArrayList list = getVariables(op, new SortedIntegerArrayList());
    	return list.toArrayI();
    }
    
    public static final int[] getVariableIds(Operation ...op){
    	SortedIntegerArrayList list = getVariables(op, new SortedIntegerArrayList());
    	return list.toArrayI();
    }
      
    public static final List<StringId.StringIdObject> getVariables(Operation op, List<StringId.StringIdObject> list){
    	return getVariables(op, new BinaryTree<StringId.StringIdObject>(StringId.idComparator)).fill(list);
    }
    
    public static final List<StringId.StringIdObject> getVariables(Iterable<Operation> operations, List<StringId.StringIdObject> list){
    	BinaryTree<StringId.StringIdObject> tree = new BinaryTree<StringId.StringIdObject>(StringId.idComparator);
    	for (Operation op: operations)
    		getVariables(op, tree);
    	return tree.fill(list);
    }
        
    public static final List<StringId.StringIdObject> getVariables(Operation operations[], List<StringId.StringIdObject> list){
    	BinaryTree<StringId.StringIdObject> tree = new BinaryTree<StringId.StringIdObject>(StringId.idComparator);
    	for (Operation op: operations)
    		getVariables(op, tree);
    	return tree.fill(list);
    }
    
	public static final SortedIntegerArrayList getVariables(Operation operations[], SortedIntegerArrayList list){
		for (Operation op: operations)
			getVariables(op, list);
		return list;
	}
        
    private static final BinaryTree<StringId.StringIdObject> getVariables(Operation op, BinaryTree<StringId.StringIdObject> tree){
    	if (op instanceof UserVariableOperation)
    		tree.add(((UserVariableOperation)op).nameObject);
    	else if (op instanceof UserFunctionOperation)
    		tree.add(((UserFunctionOperation)op).nameObject);
    	else
    		for(int i=0;i<op.size();i++)
    			getVariables(op.get(i), tree);
    	return tree;
    }
    
    public static final SortedIntegerArrayList getVariables(Operation op, SortedIntegerArrayList list){
    	if (op instanceof UserVariableOperation) {
    		list.add(((UserVariableOperation)op).nameId);
    	}else if (op instanceof UserFunctionOperation) {
    		list.add(((UserFunctionOperation)op).nameId);
    		for(int i=0;i<op.size();i++)
    			getVariables(op.get(i), list);
    	}else if (op != null){
    		for(int i=0;i<op.size();i++)
    			getVariables(op.get(i), list);
    	}
    	return list;
    }
    
    public static class OperationList extends AbstractList<Operation>{
    	private static final int initLength = 5;
    	private int size=0;
    	private Operation operations[];

    	public OperationList(){
    		operations = new Operation[initLength];
    	}
    	
    	@Override
		public final boolean add(Operation op){
    		if (size == operations.length)
    			operations = Arrays.copyOf(operations, operations.length * 2 + 1);
    		operations[size++] = op;
    		return true;
    	}
    	
    	@Override
		public final int size(){
    		return size;
    	}

		@Override
		public void add(int index, Operation obj) {
    		if (size == operations.length)
    			operations = Arrays.copyOf(operations, operations.length * 2);
			System.arraycopy(operations, index, operations, index+1, size-index-1);
			operations[index]=obj;
		}

		@Override
		public void clear() {
			Arrays.fill(operations, 0, size, null);
			size = 0;
		}

		@Override
		public Operation get(int index) {
			return operations[index];
		}

		@Override
		public int indexOf(Object arg0) {
			for (int i=0;i<size;i++)
				if (operations[i] == arg0)
					return i;
			return -1;
		}
		
		@Override
		public boolean isEmpty() {
			return size == 0;
		}

		@Override
		public int lastIndexOf(Object obj) {
			for (int i=size;i>0;i++)
				if (operations[i] == obj)
					return i;
			return -1;
		}
		
		@Override
		public Operation remove(int position) {
			Operation erg = operations[position];
			System.arraycopy(operations, position+1, operations, position, size-position);
			return erg;
		}

		@Override
		public Operation set(int index, Operation arg1) {
			Operation erg = operations[index];
			operations[index] = arg1;
			return erg;
		}

		@Override
		public <T> T[] toArray(T[] array) {
    		if (array.length >= size)
    			return Arrays.copyOf(array, size);
 			System.arraycopy(operations, 0, array, 0, size);
			return array;    		
		}
    }
    
    public static final Operation standardCalculations(Operation a){
    	return a instanceof ExceptionOperation ? a :a.isNaN() ? RealDoubleOperation.NaN : null;
    }/*"a"+"b"+"a"*3*/
    
    public static final Operation standardCalculations(Operation a, Operation b){
        if (a instanceof ExceptionOperation)
        	return a;
        if (b instanceof ExceptionOperation)
        	return b;
        if (a.isNaN() || b.isNaN())
        	return RealDoubleOperation.NaN;
        return null;
    }

    public static final Operation standardCalculations(Operation a, Operation b, Operation c){
        if (a instanceof ExceptionOperation)
        	return a;
        if (b instanceof ExceptionOperation)
        	return b;
        if (c instanceof ExceptionOperation)
        	return c;
        if (a.isNaN() || b.isNaN()  || c.isNaN())
        	return RealDoubleOperation.NaN;
        return null;
    }

    public static final Operation standardCalculations(Operation a, Operation b, Operation c, Operation d){
        if (a instanceof ExceptionOperation)
        	return a;
        if (b instanceof ExceptionOperation)
        	return b;
        if (c instanceof ExceptionOperation)
        	return c;
        if (d instanceof ExceptionOperation)
        	return d;
        if (a.isNaN() || b.isNaN()  || c.isNaN() || d.isNaN())
        	return RealDoubleOperation.NaN;
        return null;
    }
}
