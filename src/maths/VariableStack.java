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

import java.util.AbstractList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import maths.data.StringId;
import maths.data.StringId.StringIdObject;
import util.data.SortedIntegerArrayList.ReadOnlySortedIntegerArrayList;
import util.data.SortedIntegerList;
import util.data.UniqueObjects;

/** 
* @author  Paul Stahr
* @version 04.02.2012
*/
public class VariableStack implements VariableAmount, Iterable<Variable>, VariableListener
{
	private static final Logger logger = LoggerFactory.getLogger(VariableStack.class);
	private final VariableAmount parent;
	private int modCount =0;
    private Variable variable[];
    private int length=0;
    private static int currentId=0;
    private final int id = currentId++;

    @Override
	public final void variableChanged(){
    	++modCount;
    }

    /**
     * Erzeugt einen neuen Variablenhaufen mit der maximalen L\u00E4nge 100
     * Der aufruf ist identisch mit new VariablenHaufen(100)
     */
    public VariableStack (){
        this(null);
    }

    /**
     * Erzeugt einen neuen Variablenhaufen
     * @param length maximale L\u00E4nge
     */
    public VariableStack (int length){
        this(length,null);
    }
    
    /**
     * Erzeugt einen neuen Variablenhaufen mit der maximalen L\u00E4nge 100
     * Der aufruf ist identisch mit new VariablenHaufen(100)
     */
    public VariableStack (VariableAmount subStack){
        this(5, subStack);
    }

    /**
     * Erzeugt einen neuen Variablenhaufen
     * @param length maximale L\u00E4nge
     */
    public VariableStack (int length, VariableAmount object){
        variable = new Variable[length];
        this.parent = object;
    }
    
    public VariableStack(List<Variable> v, VariableAmount object){
    	length = v.size();
    	variable = new Variable[length];
    	for (int i=0;i<length;++i)
    		variable[i] = v.get(i);
    	Arrays.sort(variable, Variable.idComperator);
    	this.parent = object;    	
    }
    
    public VariableStack(Variable v[], VariableAmount object){
    	variable = Arrays.copyOf(v, v.length);
    	Arrays.sort(variable, Variable.idComperator);
    	length = variable.length;
    	this.parent = object;
    }

	@Override
	public final int modCount(){
    	return parent== null ? modCount : parent.modCount() + modCount;	
    }    

    /**
     * F\u00FCgt eine Variable hinzu
     * @param Die Variable
     * @return boolean hinzuf\u00FCgen erfolgreich
     */
    @Override
	public final boolean add (Variable v){
        if (v == null)
            throw new IllegalArgumentException();
        final int index = getIndexById(v.nameId);
        if (index >= 0)
        	return false;
        synchronized(this){
        	insertVar(v, -1-index);
        }
        return true;
    }
    
    public final boolean addLocal (Variable v){
        if (v == null)
            throw new IllegalArgumentException();
        final int index = getIndexById(v.nameId);
        if (index >= 0)
        	return false;
        synchronized(this){
        	insertVar(v, -1-index);
        }
        return true;
    }
    
    private final void insertVar(Variable v, int index){
    	if (index < 0 || index > length+1)
    		throw new ArrayIndexOutOfBoundsException();
    	if (length >= variable.length)
    		variable = Arrays.copyOf(variable, variable.length*2);
    	System.arraycopy(variable, index, variable, index+1, (length++)-index);
    	(variable[index] = v).addVariableListener(this);
        modCount ++;
    }

    @Override
	public void setGlobal(StringId.StringIdObject name, Operation value) {
    	if (parent != null){
    		parent.setGlobal(name, value);
    		return;
    	}
        final int index = getIndexById(name.id);
        if (index < 0){
        	synchronized(this){
        		insertVar(new Variable(name, value), -1-index);
        	}
	    }else{
	    	Variable v = variable[index];
	    	if (v != null)
	    		v.setValue(value);
	    	else
	    		logger.error("Null value at setting variable");
        }
    }
    
    @Override
	public Variable setLocal(StringId.StringIdObject name, Operation value) {
        final int index = getIndexById(name.id);
        if (index < 0){
    		Variable v = new Variable(name, value);
        	synchronized(this){
        		insertVar(v, -1-index);
        	}
        	return v;
	    }else{
	    	Variable v = variable[index];
	    	if (v != null)
	    		v.setValue(value);
	    	else
	    		logger.error("Null value at setting variable");
	    	return v;
        }
    }
    
    @Override
    public Variable setLocal(String name, Operation value)
    {
    	return setLocal(StringId.getStringAndId(name), value);
    }
    
	@Override
	public boolean assign(Variable arg) {
		final int index = getIndexById(arg.nameId);
    	if (index < 0)
    	{
    		return parent == null ? false : parent.assign(arg);
    	}
    	Variable v = variable[index];
    	if (v != null)
    	{
    		variable[index].removeAllVariableListener(this);
    		(variable[index] = v).addVariableListener(this);
    		modCount ++;
    	}
    	else
    		logger.error("Null value at setting variable");
    	return true;
	}
    
    @Override
 	public Variable assign(int nameId, Operation value) {
    	final int index = getIndexById(nameId);
    	if (index < 0)
    	{
    		return parent == null ? null : parent.assign(nameId, value);
    	}
    	Variable v = variable[index];
    	if (v != null)
    		v.setValue(value);
    	else
    		logger.error("Null value at setting variable");
    	return v;
    }
     
    @Override
 	public Variable assignAddLocal(StringId.StringIdObject name, Operation value) {
        final int index = getIndexById(name.id);
        if (index >= 0)
        {
	    	Variable v = variable[index];
	    	if (v != null)
	    		v.setValue(value);
	    	else
	    		logger.error("Null value at setting variable");
	    	return v;
        }
        if (parent != null)
        {
        	Variable v = parent.assign(name.id, value);
        	if (v != null)
        	{
        		return v;
        	}
        }
    	Variable v = new Variable(name, value);
    	synchronized(this){
     		insertVar(v, -1-index);
     	}
    	return v;
    }
    
    @Override
	public void replaceAddLocal(Variable arg) {
       final int index = getIndexById(arg.nameId);
       if (index >= 0)
       {
	    	Variable v = variable[index];
	    	if (v != null)
	    	{
	       		variable[index].removeAllVariableListener(this);
	    		(variable[index] = v).addVariableListener(this);
    		}
	    	else
    		{
	    		logger.error("Null value at setting variable");
    		}
       }    
       else if (parent == null || !parent.assign(arg))
       {
       		synchronized(this){
        		insertVar(arg, -1-index);
        	}
       }
   }
     
    @Override
	public void setGlobal(String name, Operation value){
    	setGlobal(StringId.getStringAndId(name), value);
    }
    
    /**
     * Fuegt die Variable hinzu oder ersetzt die alte
     */
    @Override
	public final void replaceAddGlobal (Variable v){
    	if (parent != null){
    		parent.replaceAddGlobal(v);
    		return;
    	}
        final int index = getIndexById(v.nameId);
        if (index < 0){
        	synchronized(this){
        		insertVar(v, -1-index);
        	}
	    }else{
	    	variable[index].removeAllVariableListener(this);
	        (variable[index] = v).addVariableListener(this);
	        modCount ++;
        }
    }
    
    /**
     * Fuegt die Variable hinzu oder ersetzt die alte
     */
    @Override
	public final Variable assignAddGlobal (Variable v){
    	if (parent != null){
    		return parent.assignAddGlobal(v);
    	}
        final int index = getIndexById(v.nameId);
        if (index < 0){
        	synchronized(this){
        		insertVar(v, -1-index);
        	}
        	return v;
	    }else{
	    	Variable inStack = variable[index];
	    	inStack.setValue(v.getValue());
	        modCount ++;
	        return inStack;
        }
    }
    
    /**
     * Fuegt die Variable hinzu oder ersetzt die alte
     */
    public synchronized void setLocal (Variable v){
        final int index = getIndexById(v.nameId);
        if (index < 0){
            insertVar(v, -1-index);
	    }else{
	    	variable[index].removeAllVariableListener(this);
	        (variable[index] = v).addVariableListener(this);
	        modCount ++;
        }
    }

    private final void del (int index){
    	if (index<0||index>=length)
    		throw new ArrayIndexOutOfBoundsException(index);
    	synchronized(this){
    		variable[index].removeAllVariableListener(this);
    		System.arraycopy(variable, index+1, variable, index, (length--)-index-1);
    		variable[length] = null;
    	}
        modCount ++;
    }            

    @Override
	public synchronized boolean delById (int id){
    	final int index = getIndexById(id);
    	if (index < 0)
    		return parent == null ? parent.delById(id) : false;
    	del(index);
    	return true;
    }

    @Override
	public synchronized boolean del (Variable v){
    	final int index = getIndexById(v.nameId);
    	if (index < 0 || variable[index]!= v)
    		return false;
    	del(index);
    	return true;
    }
    
    public final Variable get(int index){
    	return variable[index];
    }

    /**
     * Gibt zur\u00FCck wiviele Variablen aktuell gespeichert sind
     * @return anzahl der gespeicherten Variablen
     */
    @Override
	public final int size(){
        return parent == null ? length : parent.size() + length;
    }
    
    public final int sizeLocal(){
        return length;
    }
    
    public final Variable[] getVarsLocal(){
    	return Arrays.copyOf(variable, length);
    }
        
    public final Variable[] getVarsLocal(Variable v[]){
    	if (v.length < length)
    		return getVarsLocal();
    	System.arraycopy(variable, 0, v, 0, length);
    	return v;
    }
    

    private final int getIndexById (int nameId){
    	try{
			int low = 0, high = length-1;
			while (low<=high){
				final int middle=(low+high)/2;
				final int c =nameId - variable[middle].nameId;
				if (c>0)
					low = middle+1;
				else if (c==0)
					return middle;
				else
					high = middle-1;
			}
			return -1-low;
    	}catch(Exception e){
    		return getIndexById(nameId);
    	}
    }
    
    private final int getIndexById (int nameId, int operandCount){
		int low = 0, high = length-1;
		while (low<=high){
			final int middle=(low+high)>>1;
			int c = nameId - variable[middle].nameId;
			if (c==0)
				c = operandCount - variable[middle].operandCount();
			if (c>0)
				low = middle+1;
			else if (c==0)
				return middle;
			else
				high = middle-1;
		}
		return -1-low;
    }
    
    private final int getIndex (String name){
		for (int i=0;i<length;i++){
			if (name.equals(variable[i].nameObject.string))
				return i;
		}
		return -1;
    }
    
	@Override
	public final Variable getById(int nameId) {
    	final int index = getIndexById(nameId);
    	return index < 0 ? (parent != null ? parent.getById(nameId) : null) :  variable[index];
	}
    
	@Override
	public final Variable getById(int nameId, int operandCount) {
    	final int index = getIndexById(nameId, operandCount);
    	return index < 0 ? (parent != null ? parent.getById(nameId) : null) :  variable[index];
	}
    
    @Override
	public final Variable get (String name){
    	final int index = getIndex(name);
    	return index < 0 ? (parent != null ? parent.get(name) : null) :  variable[index];
    }

    public final Variable getLocal (String name){
    	final int index = getIndex(name);
    	return index < 0 ? null :  variable[index];
    }
    
	public Variable getLocal(StringIdObject stringIdObject) {
		return getLocalById(stringIdObject.id);
	}

	public final Variable getLocalById(int id) {
    	final int index = getIndexById(id);
    	return index < 0 ? null :  variable[index];		
	}
    
    @Override
	public synchronized void clear(){
    	if (length == 0)
    		return;
    	while (length>0){
    		variable[--length].removeAllVariableListener(this);
    		variable[length] = null;
    	}
    	modCount ++;
    }
    
    public final VariableObserver createVaribleObserver(){
    	return new VariableObserver();
    }
    
    public class VariableObserver{
    	private int oldChCount[] = UniqueObjects.EMPTY_INT_ARRAY;
    	private int newChCount[] = UniqueObjects.EMPTY_INT_ARRAY;
    	private int oldVariableNameId[] = UniqueObjects.EMPTY_INT_ARRAY;
    	private int newVariableNameId[] = UniqueObjects.EMPTY_INT_ARRAY;
    	private int oldLength = 0;
    	
    	private boolean pendent[] = UniqueObjects.EMPTY_BOOLEAN_ARRAY;
		private ReadOnlySortedIntegerArrayList included[] = new ReadOnlySortedIntegerArrayList[0];
    	private int changedVariablesId[] = UniqueObjects.EMPTY_INT_ARRAY;
    	private int dependentVariablesId[] = UniqueObjects.EMPTY_INT_ARRAY;
    	private int changedVariablesLength = 0;
    	private int dependentVariablesLength = 0;
    	
    	public final class ChangedList extends AbstractList<StringId.StringIdObject>
    	{
    		@Override
			public StringId.StringIdObject get(int index) {
				return StringId.getStringAndId(changedVariablesId[index]);
			}
    		
    		public int getId(int index)
    		{
    			return changedVariablesId[index];
    		}
			
			@Override
			public int size() {
				return changedVariablesLength;
			}
			
			@Override
			public boolean contains(Object o) {
				if (o instanceof StringIdObject)
					return 0<=Arrays.binarySearch(changedVariablesId, 0, changedVariablesLength, ((StringIdObject)o).id);
				return false;
			}
			
			public boolean contains(int id) {
				return 0 <= Arrays.binarySearch(changedVariablesId, 0, changedVariablesLength, id);
			}
    	}

    	public final class PendendList extends AbstractList<StringId.StringIdObject>
    	{
    		@Override
			public StringId.StringIdObject get(int index) {
				return StringId.getStringAndId(dependentVariablesId[index]);
			}
    		
    		public final int getId(int index)
    		{
    			return dependentVariablesId[index];
    		}
			
			@Override
			public int size() {
				return dependentVariablesLength;
			}

			public final boolean hasMatch(SortedIntegerList il)
			{
				return il.hasMatch(dependentVariablesId, dependentVariablesLength);
			}
			
			@Override
			public boolean contains(Object o) {
				if (o instanceof StringIdObject)
					return 0 <= Arrays.binarySearch(dependentVariablesId, 0, dependentVariablesLength, ((StringIdObject)o).id);
				return false;
			}
			
			public boolean contains(int id) {
				return 0 <= Arrays.binarySearch(dependentVariablesId, 0, dependentVariablesLength, id);
			}
    	}

    	private final ChangedList changedList =  new ChangedList();
		private final PendendList pendendList = new PendendList();
    	
    	private VariableObserver(){}
    	
		public boolean isPendend(int id) {
			return 0 <= Arrays.binarySearch(dependentVariablesId, 0, dependentVariablesLength, id);
		}
		
		public boolean isChanged(int id) {
			return 0 <= Arrays.binarySearch(changedVariablesId, 0, changedVariablesLength, id);
		}

		public final ChangedList getChangedVariableList(){
    		return changedList;
    	}
    	
    	public final PendendList getPendentVariableList(){
    		return pendendList;
    	}
    	
    	public final void updateChanges(){
    		int newLength = length;
    		
    		if (included.length != newLength)
    		{
    			included = new ReadOnlySortedIntegerArrayList[newLength];
    		}
    		if (newChCount.length < newLength){
        		newChCount = new int[newLength];
    			newVariableNameId = new int[newLength];
    		}
        	if (pendent.length < newLength)
        		pendent = new boolean[newLength];
        	if (dependentVariablesId.length<newLength+oldLength){
        		dependentVariablesId = new int[newLength+oldLength];
        		changedVariablesId = new int[newLength+oldLength];
        	}
    		for (int i=0;i<newLength;i++){
    			Variable v = variable[i];
    			if (v != null){
	    			newChCount[i] = v.getChCount();
	    			newVariableNameId[i] = v.nameId;
	        		included[i] = v.getPendentVariables();
    			}
    		}
    		
        	int chFillIndex = 0, peFillIndex = 0;
			for (int oldIndex=0, newIndex=0;;){
				/*Less variables then before*/
				if (newIndex==newLength){
					final int range =  oldLength-oldIndex;
					System.arraycopy(oldVariableNameId, oldIndex, changedVariablesId, chFillIndex, range);
					System.arraycopy(oldVariableNameId, oldIndex, dependentVariablesId, peFillIndex, range);
					chFillIndex += range;
					peFillIndex += range;
					break;
				}
				/*More variables then before*/
				if (oldIndex==oldLength){
					final int range =  newLength-newIndex;
					System.arraycopy(newVariableNameId, newIndex, changedVariablesId, chFillIndex, range);
					Arrays.fill(pendent, newIndex, newLength, true);
					chFillIndex += range;
					break;
				}
				final int comp = oldVariableNameId[oldIndex]-newVariableNameId[newIndex];
				if (comp < 0){
					dependentVariablesId[peFillIndex++] = changedVariablesId[chFillIndex++] = oldVariableNameId[oldIndex++];
				}else if (comp > 0){
					pendent[newIndex] = true;
					changedVariablesId[chFillIndex++] = newVariableNameId[newIndex++];
				}else{
					if (oldChCount[oldIndex] != newChCount[newIndex]){
						pendent[newIndex] = true;
						changedVariablesId[chFillIndex++] = newVariableNameId[newIndex];
					}
					oldIndex++;newIndex++;
				}
			}
        	Arrays.sort(changedVariablesId, 0, changedVariablesLength = chFillIndex);
        	for (int i=0;i<changedVariablesLength;i++)
    			addPendent(pendent, included, changedVariablesId[i], newLength);
        	
        	for (int i=0;i<newLength;i++){
        		if (pendent[i]){
        			pendent[i] = false;
        			dependentVariablesId[peFillIndex++] = newVariableNameId[i];
        		}
        	}
        	Arrays.sort(dependentVariablesId, 0, dependentVariablesLength = peFillIndex);
			oldLength = newLength;
    		int tmpi[] = newChCount;
    		newChCount = oldChCount;
    		oldChCount = tmpi;
    		tmpi = newVariableNameId;
    		newVariableNameId = oldVariableNameId;
    		oldVariableNameId = tmpi;
    	}
        
        private final int addPendent(boolean pendent[], ReadOnlySortedIntegerArrayList included[], int changedVariables2, int length){
        	int added = 0;
			for (int j=0;j<length;j++){
				if (!pendent[j]){
					ReadOnlySortedIntegerArrayList inc = included[j];
		    		for (int k = 0; k < included[j].size(); ++k)
		    		{
		    			if (inc.getI(k) == changedVariables2)
		    			{
			    			pendent[j]=true;
							added += addPendent(pendent, included, variable[j].nameId, length) + 1;
							break;
		    			}
		    		}
		    	}	
	    	}
			return added;
        }
    }

	
	@Override
	public Iterator<Variable> iterator() {
		return new Iterator<Variable>(){
			int item = 0;
			
			@Override
			public final boolean hasNext() {
				return item < length;
			}

			
			@Override
			public final Variable next() {
				return variable[item++];
			}

			
			@Override
			public final void remove() {
				throw new UnsupportedOperationException();
			}		
		};
	}
	
	@Override
	public String toString(){
		if (length == 0)
			return "[]";
		StringBuilder strB = variable[0].toString(new StringBuilder().append('['));
		for (int i=1;i<length;i++){
			Variable v = variable[i];
			if (v != null)
				v.toString(strB.append(','));
		}
		return strB.append(']').toString();
	}

	public final int getId() {
		return id;
	}

}
