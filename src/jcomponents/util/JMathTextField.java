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
package jcomponents.util;

import java.awt.Color;

import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import maths.Operation;
import maths.OperationCompiler;
import maths.data.RealDoubleOperation;
import maths.exception.OperationParseException;

public class JMathTextField extends JTextField implements DocumentListener{
	private Operation op;
	/**
	 * 
	 */
	private static final long serialVersionUID = -6035312556457085309L;

	public JMathTextField(String name, int arg){
		super(name, arg);
    	getDocument().addDocumentListener(this);
    	this.update((DocumentEvent)null);
	}

	public JMathTextField(double value)
	{
		super(Double.toString(value));
		getDocument().addDocumentListener(this);
		op = new RealDoubleOperation(value);
	}
	
	public JMathTextField(Operation value)
	{
		super(value.toString());
		getDocument().addDocumentListener(this);
		op = value;
	}
	
	public void set(double value)
	{
		setText(String.valueOf(value));
		op = new RealDoubleOperation(value);
	}
	
	public JMathTextField(String name){
		super(name);
    	getDocument().addDocumentListener(this);
    	this.update((DocumentEvent)null);
	}

	public final Operation get()
	{
		return op;
	}
	
	public JMathTextField() {
		this(null, 0);
	}
	
	public void update(DocumentEvent de){
		try {
			op = OperationCompiler.compile(getText());
			setBackground(Color.WHITE);
		} catch (OperationParseException e) {
			op = null;
			setBackground(Color.PINK);
		}
	}

	@Override
	public void changedUpdate(DocumentEvent e) {
		update(e);
	}

	@Override
	public void insertUpdate(DocumentEvent e) {
		update(e);
	}

	@Override
	public void removeUpdate(DocumentEvent e) {
		update(e);
	}
}
