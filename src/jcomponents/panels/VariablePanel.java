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

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListModel;
import javax.swing.event.ListDataListener;

import data.DataHandler;
import jcomponents.util.JMathTextField;
import maths.variable.Variable;
import maths.variable.VariableStack;
import util.JFrameUtils;
import util.TimedUpdateHandler;
/** 
* @author  Paul Stahr
* @version 04.02.2012
*/
public class VariablePanel extends InterfacePanel implements TimedUpdateHandler, KeyListener, MouseListener
{
    /**
     * Ein Panel 
     */
    private static final long serialVersionUID = 8085419683734839392L;
    private final JList<String> variableList = new JList<String>();
    private final StringBuilder stringBuilder = new StringBuilder();
	private String variables[] = new String[1];
	private final VariableStack va;
    private int changeCounts[] = new int[1];
    private int size = 0;
    private int vChangeCount = -1;
    private VariableListModel model = new VariableListModel();
	@Override
	public int getUpdateInterval(){
		return 20;
	}
	
	@Override
	public void update() {
		final int newCount = va.modCount();
    	if (vChangeCount == newCount)
    		return;
    	vChangeCount = newCount;
    	final int newLength = va.size();
    	final boolean changeHigh = size != newLength;
    	if (changeHigh){
    		if (variables.length < newLength){
    			final int newSize = Math.max(variables.length*2, newLength);
	        	variables = Arrays.copyOf(variables, newSize);
	        	changeCounts = Arrays.copyOf(changeCounts, newSize);
    		}else if (newLength<size){
    			Arrays.fill(variables, newLength, size, null);
    		}
    		size = newLength;
    	}
        for (int i=0;i<size;i++){
        	try{
	        	final Variable v = va.get(i);
	        	if (v == null){
	        		variables[i] = "error";
	        		changeCounts[i] = -1;
	        	}else if (v.modCount() != changeCounts[i]){
	            	changeCounts[i] = v.modCount();
	            	stringBuilder.setLength(0);
	               	variables[i] = v.toString(stringBuilder).toString();
	        	}
        	}catch(Exception e){
        		variables[i] = "error";
        		changeCounts[i] = -1;
        	}
        	
        }
        if (changeHigh){
            final int height = variableList.getPreferredSize().height+10;
            content.setPreferredSize(new Dimension(315, height > 200 ? 200 : height < 50 ? 50 : height));
        	content.revalidate();
        }
        for (int i=0;i<model.dataListener.size();i++)
        	model.dataListener.get(i).contentsChanged(null);
     }
	
	@Override
	public void keyReleased(KeyEvent e)
	{
		int index = variableList.getSelectedIndex();
		Variable v = va.get(index);
		if (v != null){
			new VariableJFrame(v).setVisible(true);
		}	
	}

	
	@Override
	public void keyPressed(KeyEvent e){
		final int keyCode = e.getKeyCode();
		switch (keyCode){
			case KeyEvent.VK_DELETE:
			case KeyEvent.VK_BACK_SPACE:
			{
				int index = variableList.getSelectedIndex();
				va.del(va.get(index));
				break;
			}
			case KeyEvent.VK_ENTER:
				int index = variableList.getSelectedIndex();
				Variable v = va.get(index);
				if (v != null){
					new VariableJFrame(v).setVisible(true);
				}
		}
	}

	@Override
	public void keyTyped(KeyEvent e){}     

    public VariablePanel(VariableStack variables){
        super("Variablen");
        this.va = variables;
        content.setLayout(JFrameUtils.SINGLE_COLUMN_LAYOUT);
        variableList.setModel(model);
        variableList.addKeyListener(this);
        final JScrollPane variableScrollPane = new JScrollPane(variableList);
        content.add(variableScrollPane);
        content.setPreferredSize(new Dimension(315, 50));
    }

    
	@Override
	public StringBuilder getExtendedContent(StringBuilder content){
        return content;
    }
    
    
	@Override
	protected String getType(){
    	return "variable";
    }
    
    
	@Override
	public void setExtended(boolean extended){
    	if (isExtended() == extended)
    		return;
    	super.setExtended(extended);
    	if (extended){
    		DataHandler.timedUpdater.add(this);
    	}else{
    		DataHandler.timedUpdater.remove(this);    		
    	}
    }
    
    
	@Override
	public void setVariable(String variable, String value){}
    
    private class VariableListModel implements ListModel<String> {
    	private final ArrayList<ListDataListener> dataListener = new ArrayList<ListDataListener>();
        
		@Override
		public void addListDataListener(ListDataListener l) {
			dataListener.add(l);
		}

		
		@Override
		public String getElementAt(int index) {
			String erg = index >= 0 && index < size ? variables[index] : "error";
			return erg == null ? "error" : erg;
		}

		
		@Override
		public int getSize() {
			return size;
		}

		
		@Override
		public void removeListDataListener(ListDataListener l) {
			dataListener.remove(l);
		}
	}
    
	@Override
	public void mouseClicked(MouseEvent e) {
		super.mouseClicked(e);
		Object source = e.getSource();
		if (source == variableList)
		{
			if (e.getClickCount() == 2)
			{
				int index = variableList.getSelectedIndex();
				Variable v = va.get(index);
				if (v != null){
					new VariableJFrame(v).setVisible(true);
				}
			}
		}
	}

    
	@Override
	public void destroy(){
    	setExtended(false);
    }
	
	private static class VariableJFrame extends JFrame implements ActionListener{
		/**
		 * 
		 */
		private static final long serialVersionUID = 6967750217565655572L;
		private final JLabel labelName = new JLabel("Name");
		private final JTextField textFieldName = new JTextField();
		private final JLabel labelValue = new JLabel("Value");
		private final JMathTextField textFieldValue = new JMathTextField();
		private final JButton buttonAccept = new JButton("Accept");
		private final JButton buttonCancel = new JButton("Cancel");
		private final Variable v;
		public VariableJFrame(Variable v){
			this.v = v;
			textFieldName.setText(v.nameObject.string);
			textFieldValue.setText(v.stringValue());
			
			buttonAccept.addActionListener(this);
			buttonCancel.addActionListener(JFrameUtils.closeParentWindowListener);
			
			setLayout(JFrameUtils.DOUBLE_COLUMN_LAUYOUT);
			add(labelName);
			add(textFieldName);
			add(labelValue);
			add(textFieldValue);
			add(buttonAccept);
			add(buttonCancel);
			pack();
		}
		
		@Override
		public void actionPerformed(ActionEvent e) {
			Object source = e.getSource();
			if (source == buttonAccept)
			{
				v.setValue(textFieldValue.get());
			}
		}
	}
}
