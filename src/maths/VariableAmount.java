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

import maths.data.StringId;

/** 
* @author  Paul Stahr
* @version 04.02.2012
*/
public interface VariableAmount {
	public Variable get(String name);
	
	public void replaceAddGlobal(Variable v);
	
	public boolean assign(Variable v);
	
	public void setGlobal(String name, Operation value);
	
	public void setGlobal(StringId.StringIdObject name, Operation a);

	public Variable assign(int nameId, Operation a);

	public Variable setLocal(StringId.StringIdObject name, Operation value);
	
	public Variable setLocal(String name, Operation a);

	public Variable assignAddLocal(StringId.StringIdObject name, Operation a);

	public void replaceAddLocal(Variable var);

	public boolean add(Variable v);
	
	public boolean del(Variable v);
	
	public void clear();
	
	public boolean delById(int nameId);

	public int modCount();

	public int size();

	public Variable getById(int nameId);

	public Variable getById(int nameId, int operandCount);

	public Variable assignAddGlobal(Variable v);
}
