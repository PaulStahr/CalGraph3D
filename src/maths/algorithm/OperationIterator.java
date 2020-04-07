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

import maths.Operation;
import maths.UserVariableOperation;
import maths.Variable;
import maths.VariableAmount;
import maths.VariableStack;
import maths.data.StringId.StringIdObject;

public abstract class OperationIterator {
	private final OperationIteratorInterface iterator;
	
	public OperationIterator (Operation op){
		if (!(op.isArray())){
			iterator = null;
			return;
		}
		switch(op.size()){
			case 1:{
				if(op.get(0).isRealIntegerNumber())
					iterator = new Class1Iterator(op.get(0).longValue());
				else
					iterator = null;break;
			}case 2:{
				if(op.get(0) instanceof UserVariableOperation && op.get(1).isRealIntegerNumber())
					iterator = new Class2Iterator(op.get(1).longValue(), ((UserVariableOperation)op.get(0)).nameObject);
				else
					iterator = null;break;
			}case 3:{
				if(op.get(0) instanceof UserVariableOperation && op.get(1).isRealIntegerNumber() && op.get(2).isRealIntegerNumber())
					iterator = new Class3Iterator(op.get(1).longValue(), op.get(2).longValue(), ((UserVariableOperation)op.get(0)).nameObject);
				else
					iterator = null;break;
			}case 4:{
				if(op.get(0) instanceof UserVariableOperation && op.get(1).isRealIntegerNumber() && op.get(2).isRealIntegerNumber() && op.get(3).isRealIntegerNumber())
					iterator = new Class4Iterator(op.get(1).longValue(), op.get(2).longValue(), op.get(3).longValue(), ((UserVariableOperation)op.get(0)).nameObject);
				else
					iterator = null;break;
			}default:{
				iterator = null;
			}
		}
	}
	
	public final long getRuns(){
		return iterator.getRuns();
	}
	
	public final boolean isValid(){
		return iterator != null;
	}
	
	public abstract Operation getErg();

	public void run(VariableAmount va){
		VariableStack vs = va == null ? new VariableStack() : va instanceof VariableStack ? (VariableStack)va : new VariableStack(va);
		iterator.run(vs);
	}
	
	protected abstract void calculate(VariableAmount va, long i);
	
	private interface OperationIteratorInterface{
		public abstract void run(VariableAmount vs);

		public abstract long getRuns();
	}
	
	private final class Class1Iterator implements OperationIteratorInterface{
		private final long runs;
		private Class1Iterator(	long runs){
			this.runs = runs;
		}
		@Override
		public final void run(VariableAmount stack) {
			VariableStack st = new VariableStack(stack);
			for (long i=0;i<runs;i++){
				calculate(st, i);
			}
		}
		@Override
		public final long getRuns() {
			return runs;
		}
	}

	private final class Class2Iterator implements OperationIteratorInterface{
		private final long runs;
		private final StringIdObject name;
		private Class2Iterator(	long runs, StringIdObject name){
			this.runs = runs;
			this.name = name;
		}
		@Override
		public final void run(VariableAmount stack) {
			VariableStack st = new VariableStack(stack);
			Variable v = new Variable(name);
			st.add(v);
			for (long i=0;i<runs;i++){
				v.setValue(i+1);
				calculate(st,i);
			}
		}
		@Override
		public final long getRuns() {
			return runs;
		}
	}
	
	private final class Class3Iterator implements OperationIteratorInterface{
		private final long start, runs;
		private final StringIdObject name;
		private Class3Iterator(long start,long end, StringIdObject name){
			this.runs = end-(this.start = start)+1;
			this.name = name;
		}
		@Override
		public final void run(VariableAmount stack) {
			VariableStack st = new VariableStack(stack);
			Variable v = new Variable(name);
			st.add(v);
			for (long i=0;i<runs;i++){
				v.setValue(i+start);
				calculate(st, i);
			}
		}
		@Override
		public final long getRuns() {
			return runs;
		}
	}
	
	private final class Class4Iterator implements OperationIteratorInterface{
		private final long start, step, runs;
		private final StringIdObject name;
		private Class4Iterator(long start,long end, long step, StringIdObject name){
			this.runs = (end-(this.start = start))/(this.step = step);
			this.name = name;
		}
		@Override
		public final void run(VariableAmount stack) {
			VariableStack st = new VariableStack(1, stack);
			Variable v = new Variable(name);
			st.add(v);
			for (long i=0, value = start;i<runs;value+=step, i++){
				v.setValue(value);
				calculate(st, i);
			}
		}
		@Override
		public final long getRuns() {
			return runs;
		}
	}
}
