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
package jcomponents.panels;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import maths.Operation;
import maths.ProgramOperation;
import maths.Variable;
import maths.VariableAmount;
import maths.exception.OperationParseException;
import util.JFrameUtils;

public class ProgramEditorPanel extends InterfacePanel implements ActionListener{
	private final JButton buttonSave = new JButton("Speichern");
	private final JButton buttonLoad = new JButton("Laden");
	private final JTextArea textArea = new JTextArea();
	private final JScrollPane scrollPane = new JScrollPane(textArea);
	private final LoadWindow loadWindow = new LoadWindow();
	private final SaveWindow saveWindow = new SaveWindow();
	private final VariableAmount stack;
	
	public ProgramEditorPanel(VariableAmount stack) {
		super("Programm Editor");
		this.stack = stack;
		GroupLayout layout = new GroupLayout(content);
		content.setLayout(layout);
		
		layout.setHorizontalGroup(layout.createParallelGroup()
			.addGroup(layout.createSequentialGroup()
				.addComponent(buttonLoad)
				.addComponent(buttonSave)
			).addComponent(scrollPane)
		);
		
		layout.setVerticalGroup(layout.createSequentialGroup()
			.addGroup(layout.createParallelGroup()
				.addComponent(buttonLoad)
				.addComponent(buttonSave)
			).addComponent(scrollPane)
		);
		
		buttonLoad.addActionListener(this);
		buttonSave.addActionListener(this);
		textArea.setEditable(true);
	}


	@Override
	public void actionPerformed(ActionEvent ae) {
		Object source = ae.getSource();
		if (source == buttonLoad)
		{
			loadWindow.setVisible(true);
		}
		else if (source == buttonSave)
		{
			saveWindow.setVisible(true);
		}
	}
	
	private static final long serialVersionUID = -5353668563013970851L;

	
	@Override
	protected String getType() {
		return "program_editor";
	}

	
	@Override
	protected StringBuilder getExtendedContent(StringBuilder content) {
		return content;
	}

	
	@Override
	public void setVariable(String variable, String value) {
		
	}
	
	private class LoadWindow extends JDialog implements ActionListener{
		private static final long serialVersionUID = -6869232037487284604L;
		private JLabel label = new JLabel("Bitte den Namen einer Variable eingeben");
		private JTextField textFieldName = new JTextField();
		private JButton buttonOk = new JButton("Ok");
		private JButton buttonCancel = new JButton("Abbrechen");
		private JPanel panel = new JPanel();
		
		public LoadWindow(){
			setLayout(JFrameUtils.SINGLE_COLUMN_LAYOUT);
			GroupLayout layout = new GroupLayout(panel);
			panel.setLayout(layout);
			layout.setHorizontalGroup(layout.createParallelGroup()
				.addComponent(label)
				.addComponent(textArea)
				.addGroup(layout.createSequentialGroup()
					.addComponent(buttonOk)
					.addComponent(buttonCancel)
				)
			);
			
			layout.setVerticalGroup(layout.createSequentialGroup()
				.addComponent(label)
				.addComponent(textArea)
				.addGroup(layout.createParallelGroup()
					.addComponent(buttonOk)
					.addComponent(buttonCancel)
				)
			);
			
			buttonOk.addActionListener(this);
			buttonCancel.addActionListener(this);
			add(panel);
			setTitle("Laden");
			pack();
			setResizable(false);
		}
		
		@Override
		public void actionPerformed(ActionEvent e)
		{
			Object source = e.getSource();
			
			if (source == buttonOk)
			{
				Operation op = stack.get(textFieldName.getText()).getValue();
				if (!(op instanceof ProgramOperation && ((ProgramOperation)op).a.isString()))
					return;
				textArea.setText(((ProgramOperation)op).a.stringValue());
				
				dispose();
			} else if (source == buttonCancel)
			{
				dispose();
			}
		}
	}
	
	private class SaveWindow extends JDialog implements ActionListener{
		private static final long serialVersionUID = 4905208597730613149L;
		private final JLabel label = new JLabel("Bitte einen Variablennamen eingeben");
		private final JTextField textFieldName = new JTextField();
		private final JButton buttonOk = new JButton("OK");
		private final JButton buttonCancel = new JButton("Abbrechen");
		private final JPanel panel = new JPanel();
		public SaveWindow(){
			setLayout(JFrameUtils.SINGLE_COLUMN_LAYOUT);
			GroupLayout layout = new GroupLayout(panel);
			panel.setLayout(layout);
			layout.setHorizontalGroup(layout.createParallelGroup()
				.addComponent(label)
				.addComponent(textFieldName)
				.addGroup(layout.createSequentialGroup()
					.addComponent(buttonOk)
					.addComponent(buttonCancel)
				)
			);
			
			layout.setVerticalGroup(layout.createSequentialGroup()
				.addComponent(label)
				.addComponent(textFieldName)
				.addGroup(layout.createParallelGroup()
					.addComponent(buttonOk)
					.addComponent(buttonCancel)
				)
			);
			
			buttonOk.addActionListener(this);
			buttonCancel.addActionListener(this);
			add(panel);
			setTitle("Speichern");
			pack();
			setResizable(false);
		}
		
		@Override
		public void actionPerformed(ActionEvent ae){
			Object source = ae.getSource();
			if (source == buttonOk)
			{
				try {
					stack.replaceAddGlobal(new Variable(textFieldName.getText(), "program(\"" + textArea.getText() + '\"' + ')'));
				} catch (OperationParseException e) {
					e.printStackTrace();
				}
				dispose();
			}
			else if (source == buttonCancel)
			{
				dispose();
			}
		}
	}
}

