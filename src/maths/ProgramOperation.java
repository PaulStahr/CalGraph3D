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

import java.util.ArrayList;
import java.util.List;

import maths.data.BooleanOperation;
import maths.exception.ExceptionOperation;
import maths.exception.OperationParseException;
import maths.functions.FunctionOperation;
import util.ArrayTools;
import util.StringUtils;

public class ProgramOperation extends FunctionOperation {
	private static final byte IF = 1;
	private static final byte ELSE = 2;
	private static final byte ENDIF = 3;
	private static final byte WHILE = 4;
	private static final byte ENDWHILE = 5;
	private static final byte RETURN = 6;
	private static final byte FOR = 7;
	private static final byte ENDFOR = 8;
	private static final byte SCIP = 9;

	public final Operation a;
	private final Item top;

	public static Operation getInstance(Operation a){
		if (a == null)
			throw new NullPointerException();
		try {
			return new ProgramOperation(a);
		} catch (InterpretException e) {
			return new ExceptionOperation("Fehler beim \u00FCbersetzen in Zeile " + e.line);
		}
	}
	
	public ProgramOperation(Operation a) throws InterpretException{
		if ((this.a = a).isString()){
			top = interpret(a.stringValue());
		}else{
			top = null;
		}
	}
	
	private static byte getType(String line){
		if (line.length() == 0)
			return SCIP;
		if (line.equals("else"))
			return ELSE;
		if (line.equals("endif"))
			return ENDIF;
		if (line.equals("endwhile"))
			return ENDWHILE;
		if (line.equals("endfor"))
			return ENDFOR;
		if (line.charAt(line.length() - 1) == ')')
		{
			if (line.startsWith("while("))
				return WHILE;
			if (line.startsWith("if("))
				return IF;
			if (line.startsWith("return("))
				return RETURN;
			if (line.startsWith("for("))
				return FOR;
		}
		return -1;
	}

	//program("set(erg,0)\nset(oldErg,10000)\nset(c,x+y*ⅈ)\nwhile(abs(erg)<10˄abs(erg-oldErg)>0.1)\nset(oldErg,erg)\nset(erg,erg²+c)\nendwhile\nreturn(ifn(abs(erg)<10,1,0))")
	private static Item interpret(String str) throws InterpretException{
		str = str.replace("\r","");
		ArrayList<String> al = new ArrayList<>();
		StringUtils.split(str, 0, str.length(), '\n', true, al);
		final String lines[] = al.toArray(new String[al.size()]);
		final byte types[] = new byte[lines.length];
		final Operation ops[] = new Operation[lines.length];
		byte stack[] = new byte[0];
		int size = 0;
		for (int i=0;i<lines.length;i++){
			String line = lines[i];
			int j = 0;
			while (j<line.length() && line.charAt(j)==' ')
				j++;
			lines[i] = line = line.substring(j);
			types[i] = getType(line);
			switch(types[i]){
				case IF:{
					stack = ArrayTools.push_back(stack, size, IF);
					break;
				}case ELSE:{
					if (stack[size - 1] != IF)
						throw new InterpretException(i);
					stack[size - 1] = ELSE;
					break;
				}case ENDIF:{
					final int top = stack[--size];
					if (top != IF && top != ELSE)
						throw new InterpretException(i);
					break;
				}case WHILE:{
					stack = ArrayTools.push_back(stack, size, WHILE);
					break;
				}case ENDWHILE:{
					if (stack[--size]!= WHILE)
						throw new InterpretException(i);
					break;
				}case RETURN:{
					break;
				}case SCIP:{
					break;
				}default:{
					try {
						ops[i] = OperationCompiler.compile(line);
					} catch (OperationParseException e) {
						throw new InterpretException(i);
					}
				}
			}
		}
		return interpret(0, lines.length, lines, types, ops);
		
	}
	
	private static Item interpret(final int start, final int end, final String lines[], final byte lineTypes[], final Operation lineOps[]) throws InterpretException{
		if (end-start == 1){
			String line = lines[start];
			if (lineTypes[start] == -1)
				try {
					return new StatementItem(OperationCompiler.compile(line), start);
				} catch (OperationParseException e) {
					throw new InterpretException(start);
				}
			if (lineTypes[start] == 6)
				try {
					return new ReturnItem(OperationCompiler.compile(line, 7,line.length()-1), start);
				} catch (OperationParseException e) {
					throw new InterpretException(start);
				}
		}
			
		ArrayList<Item> statements = new ArrayList<Item>();
		int rek=0;
		int lastBegin=-1;
		int elseIndex = -1;
		for (int i=start;i<end;i++){
			final String line = lines[i];
			try{
				switch (lineTypes[i]){
					case IF:{
						if (rek++ == 0)
							lastBegin = i;
						break;
					}case ENDIF:{
						if (--rek == 0){
							if (lastBegin == start && i+1==end){
								if (elseIndex == -1)
									return new IfItem(OperationCompiler.compile(lines[start], 3, lines[start].length()-1), interpret(start+1, end-1, lines, lineTypes, lineOps), start);
								return new IfElseItem(OperationCompiler.compile(lines[start], 3, lines[start].length()-1), interpret(start+1, elseIndex, lines, lineTypes, lineOps), interpret(elseIndex+1, end-1, lines, lineTypes, lineOps), start);
							}
							statements.add(interpret(lastBegin,i+1, lines, lineTypes, lineOps));
						}
						break;
					}case WHILE:{
						if (rek++ == 0)
							lastBegin = i;
						break;
					}case ENDWHILE:{
						if (--rek == 0){
							if (lastBegin == start && i+1==end){
								return new WhileItem(OperationCompiler.compile(lines[start], 6, lines[start].length()-1), interpret(start+1, end-1, lines, lineTypes, lineOps), start);
							}
							statements.add(interpret(lastBegin,i+1, lines, lineTypes, lineOps));
						}
						break;
					}case ELSE:{
						if (rek == 1)
							elseIndex = i;
						break;
					}case RETURN:{
						if (rek == 0)
							statements.add(new ReturnItem(OperationCompiler.compile(line, 7,line.length()-1), start));
						break;
					}case SCIP:{
						break;
					}default:{
						if (rek==0 && line.length() != 0)
							statements.add(new StatementItem(lineOps[i], i));
						break;
					}
				}
			}catch(Exception e){
				throw new InterpretException(i);
			}
		}
		if (statements.size()==1)
			return statements.get(0);
		return new Statements(statements.toArray(new Item[statements.size()]));
	}
	
	
	@Override
	public Operation calculate(VariableAmount object, CalculationController control) {
		Item top = this.top;
		if (top == null){
			final Operation a = this.a.calculate(object, control);
			if (!(a.isString()))
				return getInstance(a);
			try {
				top = interpret(a.stringValue());
			} catch (InterpretException e) {
				return new ExceptionOperation("Error in Code line " + e.line);
			}
		}
		VariableStack vs = new VariableStack(object);
		try{
			Operation op = top.calculate(vs, control);
			return op == null ? BooleanOperation.TRUE : op;
		}catch(Exception e){
			throw e;
		}
	}

	@Override
	public String getFunctionName() {
		return "program";
	}

	@Override
	public final int size() {
		return 1;
	}
	
	@Override
	public final Operation get(int index) {
		switch (index){
			case 0: return a;
			default:throw new ArrayIndexOutOfBoundsException(index);
		}
	}

	private static abstract class Item{
		protected final int line;
		public Item(int line)
		{
			this.line = line;
		}
		protected abstract Operation calculate(VariableStack vs, CalculationController control);
	}
	
	private static class IfElseItem extends Item{
		private final Operation question;
		private final Item a, b;
		
		private IfElseItem(Operation question, Item a, Item b, int ifLine){
			super(ifLine);
			this.question = question;
			this.a = a;
			this.b = b;
		}
		
		@Override
		protected Operation calculate(VariableStack vs, CalculationController control) {
			Operation op = question.calculate(vs, control);
			if (op == BooleanOperation.TRUE)
				return a.calculate(vs, control);
			if (op == BooleanOperation.FALSE)
				return b.calculate(vs, control);
			return new ExceptionOperation("Weder true noch false in Abfrage in Zeile:" + line);
		}
	}
	
	private static class IfItem extends Item{
		private final Operation question;
		private final Item a;
		
		private IfItem(Operation question, Item a, int ifLine){
			super(ifLine);
			this.question = question;
			this.a = a;
		}
		
		@Override
		protected Operation calculate(VariableStack vs, CalculationController control) {
			Operation op = question.calculate(vs, control);
			if (op == BooleanOperation.TRUE)
				return a.calculate(vs, control);
			if (op == BooleanOperation.FALSE)
				return null;
			return new ExceptionOperation("Weder true noch false in Abfrage in Zeile:" + line);
		}
	}	
	
	private static class WhileItem extends Item{
		private final Operation question;
		private final Item a;
		
		private WhileItem(Operation question, Item a, int whileLine){
			super(whileLine);
			this.question = question;
			this.a = a;
		}
		
		@Override
		protected Operation calculate(VariableStack vs, CalculationController control) {
			while (control == null || !control.getStopFlag()){
				Operation op = question.calculate(vs, control);
				if (op == BooleanOperation.FALSE)
					return null;
				if (op == BooleanOperation.TRUE){
					Operation op2 = a.calculate(vs, control);
					if (op2 != null)
						return op2;		
				}else{
					return new ExceptionOperation("Weder true noch false in Abfrage int Zeile: " + line);
				}
			}
			return new ExceptionOperation("Stopped");
		}		
	}
	
	private static class StatementItem extends Item{
		private final Operation o;
		private StatementItem (Operation o, int line){
			super(line);
			this.o = o;
		}
		
		@Override
		protected Operation calculate(VariableStack vs, CalculationController control) {
			o.calculate(vs, control);
			return null;
		}
	}
	
	private static class ReturnItem extends Item{
		private final Operation o;
		private ReturnItem (Operation o, int line){
			super(line);
			this.o = o;
		}
		
		@Override
		protected Operation calculate(VariableStack vs, CalculationController control) {
			return o.calculate(vs, control);
		}		
	}
	
	private static class Statements extends Item{
		public final Item statements[];
		
		private Statements(Item statements[]){
			super(-1);
			this.statements = statements;
		}

		
		@Override
		protected Operation calculate(VariableStack vs, CalculationController control) {
			for (Item item :statements){
				Operation erg = item.calculate(vs, control);
				if (erg != null)
					return erg;
			}
			return null;
		}
	}
	
	private static class InterpretException extends Exception{
		private static final long serialVersionUID = -4934729782718089690L;
		public final int line;
		
		public InterpretException(int line){
			this.line = line;
		}
	}

	@Override
	public Operation getInstance(List<Operation> subclasses) {
		return getInstance(subclasses.get(0));
	}
}
