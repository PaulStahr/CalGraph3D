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
package jcomponents;

import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.lang.ref.WeakReference;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.text.Document;

import data.DataHandler;
import jcomponents.util.JComponentSingletonInstantiator;
import jcomponents.util.OpenWindowListener;
import maths.Operation;
import maths.OperationCompiler;
import maths.data.ArrayOperation;
import maths.exception.OperationParseException;
import maths.functions.atomic.AdditionOperation;
import util.JFrameUtils;
import util.JFrameUtils.DocumentChangeListener;

/** 
* @author  Paul Stahr
* @version 04.02.2012
*/
@SuppressWarnings("unused")
public class TransformationWindow extends JFrame implements ActionListener{
	private static final long serialVersionUID = 1L;
	private final JTextField textFieldAxisAngle	= new JTextField();
	private final JButton buttonRotate 			= new JButton("Rotiere");
	private final JButton buttonScale			= new JButton("Scale");
	private final JButton buttonTranslate		= new JButton("Translate");
	private final JButton buttonSimplify		= new JButton("Vereinfachen");
	private final JTextField textFieldsMatrix[][]= new JTextField[4][4];
	private final Operation ops[][]				= new Operation[4][4];
	private final JPanel panelMatrix			= new JPanel();
	private final JTextField textFieldMatrix	= new JTextField();
	private final JButton buttonClose			= new JButton("OK");
	private final JButton buttonCopy			= new JButton("Kopieren");
	private final JButton buttonReset			= new JButton("Reset");
	private static final JComponentSingletonInstantiator<JFrame> instantiator = new JComponentSingletonInstantiator<JFrame>(LogWindow.class);
	private final JFrameUtils.DocumentChangeListener setFromTextFieldsListener = new JFrameUtils.DocumentChangeListener() {
		@Override
		public void update(DocumentEvent de) {
			setFromTextFields();
		}
	};
	private final TranslateWindow translateWindow = new TranslateWindow();
	private boolean updating = false;
	
    public static final ActionListener getOpenWindowListener()
    {
    	return instantiator;
    }
    
	@Override
	public void actionPerformed(ActionEvent arg0) {
		Object source = arg0.getSource();
		if (source == buttonRotate)
		{
			
		}
		else if (source == buttonScale)
		{
			
		}
		else if (source == buttonTranslate)
		{
			translateWindow.setVisible(true);
		}
		else if (source == buttonCopy)
		{
			Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(textFieldMatrix.getText()), null);
		}
		else if (source == buttonReset)
		{
			textFieldMatrix.setText("{{1,0,0,0},{0,1,0,0},{0,0,1,0},{0,0,0,1}}");
		}
	}
	
	public TransformationWindow(){
		GroupLayout layout = new GroupLayout(getContentPane());
		setLayout(layout);
		
		layout.setVerticalGroup(layout.createSequentialGroup()
			.addGroup(layout.createParallelGroup()
				.addComponent(buttonTranslate)
				.addComponent(buttonTranslate)
				.addComponent(buttonScale)
			).addComponent(panelMatrix)
			.addComponent(textFieldMatrix)
			.addGroup(layout.createParallelGroup()
				.addComponent(buttonCopy)
				.addComponent(buttonReset)
				.addComponent(buttonClose)
		));
		
		layout.setHorizontalGroup(layout.createParallelGroup()
			.addGroup(layout.createSequentialGroup()
				.addComponent(buttonTranslate)
				.addComponent(buttonTranslate)
				.addComponent(buttonScale)
			).addComponent(panelMatrix)
			.addComponent(textFieldMatrix)
			.addGroup(layout.createSequentialGroup()
				.addComponent(buttonCopy)
				.addComponent(buttonReset)
				.addComponent(buttonClose)
		));
		buttonRotate.addActionListener(this);
		buttonScale.addActionListener(this);
		buttonTranslate.addActionListener(this);
		panelMatrix.setLayout(new GridLayout(4,4));
		for (int i=0;i<4;i++){
			for (int j=0;j<4;j++){
				JTextField jtf = new JTextField();
				panelMatrix.add(textFieldsMatrix[i][j] = jtf);
				jtf.getDocument().addDocumentListener(setFromTextFieldsListener);
			}
		}
		textFieldMatrix.getDocument().addDocumentListener(new JFrameUtils.DocumentChangeListener() {
			@Override
			public void update(DocumentEvent de) {
				setFromTextField();
			}
		});
		
		buttonClose.addActionListener(JFrameUtils.closeParentWindowListener);
		buttonCopy.addActionListener(this);
		buttonReset.addActionListener(this);
		pack();
		setBounds(100, 100, 500, 500);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		DataHandler.addToUpdateTree(this);
	}
	
	private final void setFromOps(){
		updating = true;
		StringBuilder strB = new StringBuilder();
		textFieldMatrix.setText(ArrayOperation.getInstance(ops).toString(strB).toString());
		
		for (int i=0;i<ops.length;i++){
			for (int j=0;j<ops[i].length;j++){
				strB.setLength(0);
				textFieldsMatrix[i][j].setText(ops[i][j].toString(strB).toString());
			}
		}
		updating = false;
	}
	
	private final void setFromTextField(){
		if (updating)
			return;
		updating = true;
		fill:{
			try {
				Operation op = OperationCompiler.compile(textFieldMatrix.getText());
				if (!(op.isArray()) || ((ArrayOperation)op).length != 4)
					break fill;
				StringBuilder strB = new StringBuilder();
				for (int i=0;i<4;i++){
					Operation op2 = ((ArrayOperation)op).get(i);
					if (!(op2.isArray()) || ((ArrayOperation)op2).length != 4)
						break fill;
					for (int j=0;j<4;j++){
						strB.setLength(0);
						JTextField tf = textFieldsMatrix[i][j];
						tf.setText((ops[i][j] = ((ArrayOperation)op2).get(j)).toString(strB).toString());
						tf.setBackground(Color.WHITE);
					}	
				}
			} catch (OperationParseException e) {
				break fill;
			}
			textFieldMatrix.setBackground(Color.WHITE);
			updating = false;
			return;
		}
		textFieldMatrix.setBackground(Color.PINK);
		updating = false;
		return;		
	}
	
	private final void setFromTextFields(){
		if (updating)
			return;
		updating = true;
		boolean error = false;
		for (int i=0;i<4;i++){
			for (int j=0;j<4;j++){
				JTextField tf = textFieldsMatrix[i][j];
				try {
					ops[i][j] = OperationCompiler.compile(tf.getText());
					tf.setBackground(Color.WHITE);
				} catch (OperationParseException e) {
					tf.setBackground(Color.PINK);
					error = true;
				}
			}
		}
		if (!error){
			textFieldMatrix.setText(ArrayOperation.getInstance(ops).toString());
			textFieldMatrix.setBackground(Color.WHITE);
		}
		updating = false;		
	}
	
	private class TranslateWindow extends JFrame implements ActionListener{
		private static final long serialVersionUID = -3747734766651226925L;
		private final JLabel labelX = new JLabel("X");
		private final JLabel labelY = new JLabel("Y");
		private final JLabel labelZ = new JLabel("Z");
		private final JTextField textFieldX = new JTextField();
		private final JTextField textFieldY = new JTextField();
		private final JTextField textFieldZ = new JTextField();
		private final JButton buttonOk = new JButton("Ok");
		private final JButton buttonCancel = new JButton("Abrechen");
		private Operation x, y, z;
		
		
		private TranslateWindow(){
			DocumentChangeListener dl = new JFrameUtils.DocumentChangeListener() {					
				
				@Override
				public void update(DocumentEvent de) {
					Document source = de.getDocument();
					if (source == textFieldX.getDocument())
					{
						try{
							x= OperationCompiler.compile(textFieldX.getText());
							textFieldX.setBackground(Color.WHITE);
						}catch(OperationParseException e){
							textFieldX.setBackground(Color.PINK);
							x = null;
						}
						updateOkButton();
					}
					else if (source == textFieldY.getDocument())
					{
						try{
							y= OperationCompiler.compile(textFieldY.getText());
							textFieldY.setBackground(Color.WHITE);
						}catch(OperationParseException e){
							textFieldY.setBackground(Color.PINK);
							y = null;
						}
						updateOkButton();
					}
					else if (source == textFieldZ.getDocument())
					{
						try{
							z= OperationCompiler.compile(textFieldZ.getText());
							textFieldZ.setBackground(Color.WHITE);
						}catch(OperationParseException e){
							textFieldZ.setBackground(Color.PINK);
							z = null;
						}
						updateOkButton();
					}
				}
			};
			setLayout(new GridLayout(-1,2));
			add(labelX);
			textFieldX.getDocument().addDocumentListener(dl);
			add(textFieldX);
			add(labelY);
			textFieldY.getDocument().addDocumentListener(dl);
			add(textFieldY);
			add(labelZ);
			textFieldZ.getDocument().addDocumentListener(dl);
			add(textFieldZ);
			buttonOk.addActionListener(this);
			add(buttonOk);
			buttonCancel.addActionListener(JFrameUtils.closeParentWindowListener);
			add(buttonCancel);
			pack();
			setMinimumSize(getPreferredSize());
		}
		
		private void updateOkButton(){
			buttonOk.setEnabled(x!=null&&y!=null&&z!=null);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			ops[0][3] = AdditionOperation.calculate(ops[0][3], x, null);
			ops[1][3] = AdditionOperation.calculate(ops[1][3], y, null);
			ops[2][3] = AdditionOperation.calculate(ops[2][3], z, null);
			setFromOps();
		}
	}
}
