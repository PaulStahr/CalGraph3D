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

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;

import javax.swing.GroupLayout;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import data.DataHandler;
import jcomponents.util.JMathTextField;
import maths.Controller;
import maths.Operation;
import maths.variable.Variable;
import maths.variable.VariableStack;
import util.SaveLineCreator;
/** 
* @author  Paul Stahr
* @version 04.02.2012
*/
public class CodePadPanel extends InterfacePanel implements KeyListener, Runnable
{
    /**
     * Dieses Panel stellt ein Codepad mit Geschichte dar
     */
    private static final long serialVersionUID = -5450280421369454662L;
	private static final SimpleAttributeSet SET_OPERATION = new SimpleAttributeSet();
	private static final SimpleAttributeSet SET_RESULT= new SimpleAttributeSet();
	private static final Logger logger = LoggerFactory.getLogger(CodePadPanel.class);
    private final JTextArea textAreaHistory 	= new JTextArea();
    private final JScrollPane scrollPaneHistory = new JScrollPane(textAreaHistory);
    private final JMathTextField textFieldCodeLine 	= new JMathTextField();
 	private final ArrayList<QueObject> arrayList = new ArrayList<QueObject>();
 	private int calculationPosition = 0;
    private int currentCommand = -1;
    private boolean running = false;
    private final Controller control = new Controller();
    private final Variable ans = new Variable("ans");
    private final VariableStack stack;
	private final StringBuilder stringBuilder = new StringBuilder();
	@Override
	public final void run(){
		while (arrayList.size()>calculationPosition){
			try{
    			final QueObject qo = arrayList.get(calculationPosition++);
    			String commandStr = qo.command;
    			if (commandStr.equals("clear"))
    			{
    				textAreaHistory.setText("");
    			}
    			stringBuilder.setLength(0);
    			stringBuilder.append(commandStr);
    			
    			for (int i = 0; i < commandStr.length(); ++i)
    			{
    				if (stringBuilder.charAt(i) == '\n')
    				{
    					stringBuilder.setCharAt(i, ' ');
    				}
    			}
    			stringBuilder.append('\n');
    			append(stringBuilder.toString(), SET_OPERATION);
    			Operation res = qo.operation.calculate(stack, control);
    			control.setStopFlag(false);
    			stringBuilder.setLength(0);
    			res.toString(stringBuilder.append('=')).append('\n');
    			if (stringBuilder.length() > 1000)
    			{
    				stringBuilder.setLength(1000);
    				stringBuilder.append('.').append('.').append('.');
    			}
				append(stringBuilder.toString(), SET_RESULT);      
                textAreaHistory.setCaretPosition(textAreaHistory.getDocument().getLength());
                ans.setValue(res);
			}catch(Exception e){
				logger.error("Error at calculating: " + e);
				e.printStackTrace();
			}
		}
		running = false;
	}
    
    static{
		StyleConstants.setAlignment(SET_OPERATION, StyleConstants.ALIGN_RIGHT);
		StyleConstants.setAlignment(SET_RESULT, StyleConstants.ALIGN_RIGHT);
    }
    
    
    @Override
	public void keyReleased(KeyEvent ke){}

    
    @Override
	public void keyPressed(KeyEvent ke){
    	if (ke.isControlDown()){
        	if (ke.isShiftDown()){
        		if (ke.getKeyCode() == KeyEvent.VK_C){
        			control.setStopFlag(true);
        		}
        	}
    	}
    		
    	switch (ke.getKeyCode()){
            case KeyEvent.VK_UP:{
            	currentCommand--;
            	setCommandHistory();
                break;
            }case KeyEvent.VK_DOWN:{
            	currentCommand++;
            	setCommandHistory();
                break;
            }case KeyEvent.VK_ENTER:{
            	Operation operation = textFieldCodeLine.get();
            	if (operation == null)
            		break;
            	currentCommand = Integer.MAX_VALUE/2;     
            	arrayList.add(new QueObject(textFieldCodeLine.getText(), ke, operation));
            	if (!running)
            		DataHandler.runnableRunner.run(this, "Code Pad");
                textFieldCodeLine.setText("");
                break;
            }
        }
    }

    
    @Override
	public void keyTyped(KeyEvent e){}
    public CodePadPanel(VariableStack stack){
        super("Code Pad");
        this.stack = stack;
		control.calculateLoop(true);
		control.calculateRandom(true);
		stack.setLocal(ans);
		GroupLayout layout = new GroupLayout(content);
        content.setLayout(layout);

        textAreaHistory.setEditable(false);

        textFieldCodeLine.addKeyListener(this);
        
        layout.setVerticalGroup(layout.createSequentialGroup()
            .addComponent(scrollPaneHistory, 100, 100, 100)
            .addComponent(textFieldCodeLine, 25, 25, 25)
        );

        layout.setHorizontalGroup(layout.createParallelGroup()
            .addComponent(scrollPaneHistory, 315, 315, 315)
            .addComponent(textFieldCodeLine, 315, 315, 315)
        );
    }

    private void setCommandHistory(){
    	if (currentCommand<0)
    		currentCommand = 0;
    	if (currentCommand>= arrayList.size())
    		currentCommand = arrayList.size()-1;
    	if (arrayList.size()!=0)	
    		textFieldCodeLine.setText(arrayList.get(currentCommand).command);
    }
    
    
	@Override
	public StringBuilder getExtendedContent(StringBuilder content){
        SaveLineCreator.appendSaveLine("history", textAreaHistory.getText(), content);
        SaveLineCreator.appendSaveLine("code_line", textFieldCodeLine.getText(), content);
        return content;
    }

    
	@Override
	public void setVariable(String variable, String value){
        if (variable.equals("history"))         textAreaHistory.setText(value);
        else if (variable.equals("code_line"))  textFieldCodeLine.setText(value);
    }

	private final void append(String text, AttributeSet set){
		final Document doc = textAreaHistory.getDocument();
	    try {
	    	doc.insertString(doc.getLength(), text, set);
	    }catch (BadLocationException e) {
    		e.printStackTrace();
    	}
	}
    
    
	@Override
	public void reset(){
        textAreaHistory.setText("");
        textFieldCodeLine.setText("");
    }    
    
    
	@Override
	protected String getType(){
    	return "codepad";
    }

	private static class QueObject{
		private final Operation operation;
		private final String command;
		//private final KeyEvent event;
    	public QueObject(String command, KeyEvent event, Operation operation){
    		this.command = command;
    		//this.event = event;
    		this.operation = operation;
    	}
    }
	
	
	@Override
	public final void destroy(){
		control.setStopFlag(true);
	}
}
