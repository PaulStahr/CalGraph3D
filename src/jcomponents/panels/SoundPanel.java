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

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ButtonGroup;
import javax.swing.GroupLayout;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;

import util.SaveLineCreator;
import util.TimedUpdateHandler;
import data.DataHandler;
import data.ProgramIcons;
import maths.Variable;

/** 
* @author  Paul Stahr
* @version 04.02.2012
*/
public class SoundPanel extends InterfacePanel implements ActionListener, DocumentListener
{
    /**
     * Stellt ein panel dar, mit dem es m\u00F6glich ist animationen zu machen
     */
    private static final long serialVersionUID = -2600464359453163933L;
    //private final JToggleButton buttonPlayBackward  = new JToggleButton(ProgramIcons.imgPlayBackward);
    private final JToggleButton buttonStop          = new JToggleButton(ProgramIcons.imgStop);
    private final JToggleButton buttonPlayForward   = new JToggleButton(ProgramIcons.imgPlayForward);
    private final JTextField textFieldVariable      = new JTextField();
    private final JLabel labelEquals                = new JLabel("=");
    private final JTextField textFieldValue         = new JTextField();
    private final JLabel labelSpeed                 = new JLabel("Geschwindigkeit = ");
    public final JTextField textFieldSpeed          = new JTextField();
    private boolean forward;
    private boolean running;
    private double speed;
    private double addPerInterval;
    private double startValue;
    private int mult;
    private double value;
    private Variable variable;
    private final TimedUpdateHandler timedUpdateHandler = new TimedUpdateHandler() {
		
		@Override
		public int getUpdateInterval(){
			return 10;
		}
		
		
		@Override
		public void update() {
    		textFieldValue.setText(Double.toString(value = startValue + (mult += forward ? 10 : -10)*addPerInterval));
	        if (variable != null)
	            variable.setValue (value);
		}
	};
	

	@Override
	public void changedUpdate(DocumentEvent e) {
		Document doc = e.getDocument();
		if (doc == textFieldSpeed.getDocument())
		{
			try{
	            speed = Double.parseDouble(textFieldSpeed.getText());
	            textFieldSpeed.setBackground(Color.WHITE);
	        }catch (Exception ex){
	            speed = 0.0;
	            textFieldSpeed.setBackground(textFieldSpeed.getText().length()==0 ? Color.WHITE : Color.RED);
	        }
	        addPerInterval = speed/1000;
		}
		else if (doc == textFieldValue)
		{
		    try{
                value = Double.parseDouble(textFieldValue.getText());
                textFieldValue.setBackground(Color.WHITE);
            }catch (Exception ex){
                value = Double.NaN;
                textFieldValue.setBackground(textFieldValue.getText().length()==0 ? Color.WHITE : Color.RED);
            }
            Variable v = DataHandler.globalVariables.get(textFieldVariable.getText());
            if (v != null)
                v.setValue (value);
		}
		else if (doc == textFieldVariable.getDocument())
		{
            textFieldVariable.setBackground(Variable.isValidName(textFieldVariable.getText()) || textFieldVariable.getText().length() == 0 ? Color.white : Color.red);			
		}
	}

	@Override
	public void insertUpdate(DocumentEvent e) {
		changedUpdate(e);		
	}

	@Override
	public void removeUpdate(DocumentEvent e) {
		changedUpdate(e);
	}

	@Override
	public final void actionPerformed(ActionEvent ae)
	{
		Object source = ae.getSource();
		if (source == buttonStop)
		{
			setRunning(false);
		}
		else if (source == buttonPlayForward)
		{
			forward = true;
            setRunning(true);
		}
	}
	
    public SoundPanel(){
        super("Sound");
        final ButtonGroup buttonGroubPlay       = new ButtonGroup();
        GroupLayout layout = new GroupLayout(content);
        content.setLayout(layout);
        buttonStop.addActionListener(this);
        buttonGroubPlay.add(buttonStop);
        buttonPlayForward.addActionListener(this);
        buttonGroubPlay.add(buttonPlayForward);
        textFieldSpeed.getDocument().addDocumentListener(this);
        textFieldVariable.getDocument().addDocumentListener(this);
        textFieldValue.getDocument().addDocumentListener(this);
        
        layout.setHorizontalGroup(
        	layout.createParallelGroup()
        		.addGroup(layout.createSequentialGroup()
	        		.addComponent(buttonStop, 25, 25, 25)
	        		.addComponent(buttonPlayForward, 25, 25, 25)
	        		.addComponent(labelSpeed)
	        		.addComponent(textFieldSpeed)
	        	).addGroup(layout.createSequentialGroup()
	        		.addComponent(textFieldVariable, 50, 50, 1000)
	        		.addComponent(labelEquals)
	        		.addComponent(textFieldValue, 50, 50, 1000)
	        	)
        );
        
        layout.setVerticalGroup(
            layout.createSequentialGroup()
            	.addGroup(layout.createParallelGroup()
    	       		.addComponent(buttonStop, 25, 25, 25)
    	       		.addComponent(buttonPlayForward, 25, 25, 25)
    	       		.addComponent(labelSpeed, 25, 25, 25)
    	       		.addComponent(textFieldSpeed, 25, 25, 25)
    	       	).addGap(5).addGroup(layout.createParallelGroup()
    	       		.addComponent(textFieldVariable, 25, 25, 25)
    	       		.addComponent(labelEquals, 25, 25, 25)
    	       		.addComponent(textFieldValue, 25, 25, 25)
    	       	)
        );
    }
    
    private void setRunning(boolean run){
    	if (running == run)
    		return;
    	synchronized(this){
    		variable = (running = run) ? DataHandler.globalVariables.get(textFieldVariable.getText()) : null;
    		textFieldValue.setEditable(!run);
    		textFieldVariable.setEditable(!run);
            if (run){
                mult = 0;
                startValue = value;
                textFieldValue.getDocument().addDocumentListener(this);
    			DataHandler.timedUpdater.add(timedUpdateHandler);
    		}else{
    			textFieldValue.getDocument().removeDocumentListener(this);
    			DataHandler.timedUpdater.remove(timedUpdateHandler);
    		}
    	}
    }

    
	@Override
	public StringBuilder getExtendedContent(StringBuilder content){
        SaveLineCreator.appendSaveLine("variable", textFieldVariable.getText(), content);
        SaveLineCreator.appendSaveLine("speed", textFieldSpeed.getText(), content);
        SaveLineCreator.appendSaveLine("value", textFieldValue.getText(), content);
        return content;
    }
    
    
	@Override
	protected String getType(){
    	return "animation";
    }

    
	@Override
	public void setVariable(String variable, String value){
        if (variable.equals("variable")) textFieldVariable.setText(value);
        else if (variable.equals("speed")) textFieldSpeed.setText(value);
        else if (variable.equals("value"))textFieldValue.setText(value);
    }

    
	@Override
	public void reset(){
    	setRunning(false);
        textFieldVariable.setText("");
        textFieldSpeed.setText("");
        textFieldValue.setText("");
    }
    
    
	@Override
	public void destroy(){
    	setRunning(false);
    }
}
