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

import data.DataHandler;
import data.ProgramIcons;
import jcomponents.util.JMathTextField;
import jcomponents.util.MathDocumentListener;
import maths.Controller;
import maths.variable.Variable;
import maths.variable.VariableAmount;
import util.SaveLineCreator;
import util.TimedUpdateHandler;

/** 
* @author  Paul Stahr
* @version 04.02.2012
*/
public class AnimationPanel extends InterfacePanel implements TimedUpdateHandler, ActionListener, DocumentListener
{
    /**
     * Stellt ein panel dar, mit dem es m\u00F6glich ist animationen zu machen
     */
    private static final long serialVersionUID = -2600464359453163933L;
    private final JToggleButton buttonPlayBackward  = new JToggleButton(ProgramIcons.imgPlayBackward);
    private final JToggleButton buttonStop          = new JToggleButton(ProgramIcons.imgStop);
    private final JToggleButton buttonPlayForward   = new JToggleButton(ProgramIcons.imgPlayForward);
    private final JTextField textFieldVariable      = new JTextField();
    private final JTextField textFieldValue         = new JTextField();
    private final JLabel labelSpeed                 = new JLabel("Geschwindigkeit = ");
    public final JMathTextField textFieldSpeed      = new JMathTextField();
    private final VariableAmount variables;
    private boolean forward;
    private boolean running;
    private double speed;
    private double addPerInterval;
    private double startValue;
    private int time;
    private double value;
    private Variable variable;
    private final Controller controller = new Controller();
	@Override
	public final int getUpdateInterval(){
		return 10;
	}
	
	@Override
	public final void update() {
		value = startValue + (time += forward ? 10 : -10)*addPerInterval;
		if (isExtended()){
			textFieldValue.setText(Double.toString(value));	
		}
        if (variable != null)
            variable.setValue (value);
	}

    private final DocumentListener changedTextFieldSpeed = new MathDocumentListener(textFieldSpeed)
    {	
		@Override
		public void update(DocumentEvent de) {
			super.update(de);
			try{
				speed = get().calculate(variables, controller).doubleValue();
	        }catch (Exception e){
	            speed = 0.0;
            }
            addPerInterval = speed/1000;
		}
	};

	@Override
	public void actionPerformed(ActionEvent e) {
		Object source = e.getSource();
		if (source == buttonPlayBackward)
		{
			forward = false;
            setRunning(true);
		}
		else if (source == buttonStop)
		{
			setRunning(false);
		}
		else if (source == buttonPlayForward)
		{
			forward = true;
            setRunning(true);
		}
	}

	public AnimationPanel(VariableAmount variables){
        super("Animation");
        this.variables = variables;
        final JLabel labelEquals                = new JLabel("=");
        GroupLayout layout = new GroupLayout(content);
        content.setLayout(layout);

        final ButtonGroup buttonGroubPlay       = new ButtonGroup();
        buttonPlayBackward.addActionListener(this);
        buttonGroubPlay.add(buttonPlayBackward);
        buttonStop.addActionListener(this);
        buttonGroubPlay.add(buttonStop);
        buttonPlayForward.addActionListener(this);
        buttonGroubPlay.add(buttonPlayForward);
        textFieldSpeed.getDocument().addDocumentListener(changedTextFieldSpeed);
        textFieldVariable.getDocument().addDocumentListener(this);
        textFieldValue.getDocument().addDocumentListener(this);
        
        layout.setHorizontalGroup(
        	layout.createParallelGroup()
        		.addGroup(layout.createSequentialGroup()
	        		.addComponent(buttonPlayBackward, 25, 25, 25)
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
    	       		.addComponent(buttonPlayBackward, 25, 25, 25)
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
    		variable = (running = run) ? variables.get(textFieldVariable.getText()) : null;
    		textFieldValue.setEditable(!run);
    		textFieldVariable.setEditable(!run);
    		if (run){
                time = 0;
                startValue = value;
    			DataHandler.timedUpdater.add(this);
    			textFieldValue.getDocument().removeDocumentListener(this);
    		}else{
    			DataHandler.timedUpdater.remove(this);
    			textFieldValue.getDocument().addDocumentListener(this);
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
	public void setExtended(boolean extended){
    	if (isExtended() == extended)
    		return;
    	super.setExtended(extended);
		textFieldValue.setText(Double.toString(value));	
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

	@Override
	public void changedUpdate(DocumentEvent de) {
		Document doc = de.getDocument();
		if (doc == textFieldVariable.getDocument())
		{
			textFieldVariable.setBackground(Variable.isValidName(textFieldVariable.getText()) || textFieldVariable.getText().length() == 0 ? Color.WHITE : Color.RED);
		}
		else if (doc == textFieldValue)
		{
			try{
                value = Double.parseDouble(textFieldValue.getText());
                textFieldValue.setBackground(Color.white);
            }catch (Exception e){
                value = Double.NaN;
                textFieldValue.setBackground(textFieldValue.getText().length()==0 ? Color.white : Color.red);
            }
            Variable v = variables.get(textFieldVariable.getText());
            if (v != null)
                v.setValue (value);
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
}
