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

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;

import maths.Variable;
import maths.VariableAmount;
import util.SaveLineCreator;
import util.TimedUpdateHandler;
/** 
* @author  Paul Stahr
* @version 04.02.2012
*/
public class SliderPanel extends InterfacePanel implements ActionListener, ChangeListener, DocumentListener, TimedUpdateHandler
{
    /**
     * Ein Panel mit einem Slider um Variablen zu �ndern
     */
    private static final long serialVersionUID	= 4298663699345171238L;
    private final JTextField fieldMinValue      = new JTextField("0");
    private final JSlider slider                = new JSlider(0, 1000);
    private final JTextField fieldMaxValue      = new JTextField("10");
    private final JLabel labelValue             = new JLabel();
    private final JTextField fieldVariableName  = new JTextField();
    private final VariableAmount variables;
    private Variable v;
    private double value;
    private double min;
    private double max;
	private int vModCount;
	private boolean updating;

    @Override
	public void actionPerformed(ActionEvent e){
    	variables.add(new Variable(fieldVariableName.getText(), value));
    } 
    @Override
	public void stateChanged (ChangeEvent e){
    	updateValue();
    }
    
	public void update (DocumentEvent de){
		Document doc = de.getDocument();
		if (doc == fieldVariableName.getDocument())
		{
			String text = fieldVariableName.getText();
			boolean valid = text.length() == 0 || Variable.isValidName(text) ;
        	fieldVariableName.setBackground(valid ? Color.WHITE : Color.RED); 
        	v = valid ? variables.get(text) : null;
        }
		else if (doc == fieldMinValue.getDocument())
		{
			updateMin();
		}
		else if (doc == fieldMaxValue.getDocument())
		{
			updateMax();
		}
    }
    
    public SliderPanel(VariableAmount variables){
        super("Slider");
        this.variables = variables;
        final JButton buttonAcceptName      = new JButton("Variable hinzuf\u00FCgen");
        final JLabel labelEquals            = new JLabel("=");
        final GroupLayout layout = new GroupLayout(content);
        content.setLayout(layout);
        fieldMinValue.getDocument().addDocumentListener(this);
        slider.addChangeListener(this);
        fieldMaxValue.getDocument().addDocumentListener(this);
        fieldVariableName.getDocument().addDocumentListener(this);
        buttonAcceptName.addActionListener(this);

        layout.setVerticalGroup(layout.createSequentialGroup()
        	.addGroup(layout.createParallelGroup()
        		.addComponent(fieldMinValue, Alignment.CENTER)
        		.addComponent(slider, Alignment.CENTER)
        		.addComponent(fieldMaxValue, Alignment.CENTER)
        	).addGroup(layout.createParallelGroup()
        		.addComponent(fieldVariableName, Alignment.CENTER)
        		.addComponent(labelEquals, Alignment.CENTER)
        		.addComponent(labelValue, Alignment.CENTER)
        		.addComponent(buttonAcceptName, Alignment.CENTER)
        	)
        );
        
        layout.setHorizontalGroup(layout.createParallelGroup()
        	.addGroup(layout.createSequentialGroup()
        		.addComponent(fieldMinValue, 40, 40, 40)
        		.addGap(5).addComponent(slider)
        		.addGap(5).addComponent(fieldMaxValue, 40, 40, 40)
        	).addGroup(layout.createSequentialGroup()
        		.addComponent(fieldVariableName,10,10,150)
        		.addGap(5).addComponent(labelEquals, labelEquals.getPreferredSize().width, labelEquals.getPreferredSize().width, labelEquals.getPreferredSize().width)
        		.addGap(5).addComponent(labelValue, 10, 10, 40)
        		.addGap(5).addComponent(buttonAcceptName)
        	)
        );
        updateMin();
        updateMax();
        updateValue();
    }

    private void updateMin(){
        try{
            min = Double.parseDouble(fieldMinValue.getText());
            fieldMinValue.setBackground(Color.WHITE);
        }catch (NumberFormatException e){
            min = Double.NaN;
            fieldMinValue.setBackground(Color.RED);
        }
        updateValue();
    }
    
    private void updateMax(){
        try{
            max = Double.parseDouble(fieldMaxValue.getText());
            fieldMaxValue.setBackground(Color.WHITE);
        }catch (NumberFormatException e){
            max = Double.NaN;
            fieldMaxValue.setBackground(Color.RED);
        }
        updateValue();
    }
    
    private void updateValue(){
    	final int sValue = slider.getValue();
        labelValue.setText(Double.toString(value = (sValue * max + (1000 - sValue) * min) / 1000));
        
        final Variable variable = variables.get(fieldVariableName.getText());
        if (variable != null)
        {
        	if (updating || variable.getValue().doubleValue() == value)
            {
            	return;
            }
            variable.setValue(value);
            vModCount = variable.modCount();
        }
    }
    
    
	@Override
	public StringBuilder getExtendedContent(StringBuilder content){
        SaveLineCreator.appendSaveLine("min", fieldMinValue.getText(), content);
        SaveLineCreator.appendSaveLine("max", fieldMaxValue.getText(), content);
        SaveLineCreator.appendSaveLine("value", slider.getValue(), content);
        SaveLineCreator.appendSaveLine("variable_name", fieldVariableName.getText(), content);
        return content;
    }

    
	@Override
	protected String getType(){
    	return "slider";
    }

    
	@Override
	public void setVariable(String variable, String value){
        if (variable.equals("min"))					fieldMinValue.setText(value);
        else if (variable.equals("max"))            fieldMaxValue.setText(value);
        else if (variable.equals("value"))          slider.setValue(Integer.parseInt(value));
        else if (variable.equals("variable_name"))  fieldVariableName.setText(value);
    }

    
	@Override
	public void reset(){
        fieldMinValue.setText("");
        fieldMaxValue.setText("");
        fieldVariableName.setText("");
        slider.setValue(500);
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
	@Override
	public int getUpdateInterval() {
		return 20;
	}
	@Override
	public void update() {
		final int newModCount = v.modCount();
    	if (vModCount == newModCount)
    		return;
    	vModCount = newModCount;
    	if (v.getValue().doubleValue() != value)
    	{
    		value = v.getValue().doubleValue();
    		updating = true;
    		slider.setValue((int)((v.getValue().doubleValue() - min) * 100 / (max - min)));
    		updating = false;
    	}
	}
}
