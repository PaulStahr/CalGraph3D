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

import javax.swing.*;


import maths.Operation;
import maths.VariableAmount;
import maths.data.StringOperation;
import util.JFrameUtils;

import java.awt.event.*;
import java.util.List;

/** 
* @author  Paul Stahr
* @version 04.02.2012
*/
public class RequestTimeoutOperation extends FunctionOperation {
	public final Operation question, timeout;
	
	public RequestTimeoutOperation (Operation question, Operation timeout){
		if ((this.question = question) == null || (this.timeout = timeout) == null)
			throw new NullPointerException();
	}

	
	@Override
	public Operation calculate(final VariableAmount object, CalculationController control) {
		final JFrame frame = new JFrame();
		final Operation a = question.calculate(object, control);
		final Operation b = timeout.calculate(object, control);
		if (!(a.isString() && b.isComplexFloatingNumber()))
			return new RequestTimeoutOperation(a, b);
		final double timeout = (b.doubleValue() * 1000);
		final JLabel label = new JLabel(a.stringValue());
		final JTextField textField = new JTextField();
		final JButton button = new JButton("OK");
		frame.setLayout(JFrameUtils.SINGLE_ROW_LAYOUT);
		
		frame.add(label);
		frame.add(textField);
		button.addActionListener(
			new ActionListener(){
				@Override
				public void actionPerformed(ActionEvent ae){
					synchronized (frame) {
						frame.notifyAll();
					}
				}
			}
		);
		frame.add(button);
		
		frame.setTitle("Abfrage");
		frame.setBounds(500,500,400,100);
		frame.setVisible(true);

		synchronized (frame) {
			try{
				frame.wait(timeout < 0 ? 0 : timeout > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int)timeout);
			}catch(Exception e){}
		}
		frame.dispose();
		return new StringOperation(textField.getText());
	}

	@Override
	public final int size() {
		return 2;
	}

	
	@Override
	public final Operation get(int index) {
		switch (index){
			case 0: return question;
			case 1: return timeout;
			default:throw new ArrayIndexOutOfBoundsException(index);
		}
	}
    
	
	@Override
	public String getFunctionName() {
		return "request";
	}

	@Override
	public Operation getInstance(List<Operation> subclasses) {
		return new RequestTimeoutOperation(subclasses.get(0), subclasses.get(1));
	}
}
