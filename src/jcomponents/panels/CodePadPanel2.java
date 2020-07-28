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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import data.DataHandler;
import maths.Controller;
import maths.Operation;
import maths.OperationCompiler;
import maths.exception.OperationParseException;
import maths.variable.Variable;
import maths.variable.VariableStack;
import util.JFrameUtils;
/** 
* @author  Paul Stahr
* @version 04.02.2012
*/
public class CodePadPanel2 extends InterfacePanel implements Runnable, KeyListener
{
    /**
     * Dieses Panel stellt ein Codepad mit Geschichte dar
     */
    private static final long serialVersionUID = -5450280421369454662L;
	private static Logger logger = LoggerFactory.getLogger(CodePadPanel.class);
	private final JPanel history = new JPanel();
    private final JScrollPane scrollPane = new JScrollPane(history);
  	private final ArrayList<QueObject> arrayList = new ArrayList<QueObject>();
 	private int calculationPosition = 0;
    private boolean running = false;
    private final Controller control = new Controller();
    private Variable ans = new Variable("ans");
    private final VariableStack variables;
	private StringBuilder stringBuilder = new StringBuilder();
	@Override
	public final void run(){
		while (arrayList.size()>calculationPosition){
			try{
    			final QueObject qo = arrayList.get(calculationPosition++);
    			if (qo.command.equals("clear"))
    			{
    				history.removeAll();
    			}
    			String erg;
    			try{
    				Operation op = OperationCompiler.compile(qo.command);
        			control.setStopFlag(false);
    				op = op.calculate(variables, control);
	                ans.setValue(op);
    				stringBuilder.setLength(0);
    				erg = op.toString(stringBuilder).toString();
    			}catch(OperationParseException e){
    				erg = e.toString();
    			}
    			int position = history.getComponentZOrder(qo.component);
    			JTextArea tf = new JTextArea(erg);
    			tf.addKeyListener(this);
    			history.add(tf);
    			history.setComponentZOrder(tf, position + 1);
    			history.revalidate();
			}catch(Exception e){
				logger.error("Error at calculating: " + e);
				e.printStackTrace();
			}
		}
		running = false;
	}
    
    @Override
	public void keyReleased(KeyEvent ke){}

    
    @Override
	public void keyPressed(KeyEvent ke){
    	Component c = ke.getComponent();
    	
    	if (ke.isControlDown()){
        	if (ke.isShiftDown()){
        		if (ke.getKeyCode() == KeyEvent.VK_C){
        			control.setStopFlag(true);
        		}
        	}
    	}
    		
    	switch (ke.getKeyCode()){
            case KeyEvent.VK_UP:{
            	//actualCommand--;
                break;
            }case KeyEvent.VK_DOWN:{
            	//actualCommand++;
                break;
            }case KeyEvent.VK_ENTER:{
            	if (ke.isShiftDown()){
                	if (c instanceof JTextArea){
                		String str = ((JTextArea)c).getText();
                		arrayList.add(new QueObject(str, ke, c));
                	}
                	if (!running)
                		DataHandler.runnableRunner.run(this, "Code Pad");
                	}
                break;
            }
        }
    }
    
    @Override
	public void keyTyped(KeyEvent e){}
    
    public CodePadPanel2(VariableStack variables){
        super("Code Pad2");
        this.variables = variables;
		control.calculateLoop(true);
		control.calculateRandom(true);
		variables.setLocal(ans);
        JTextArea textArea = new JTextArea("");
        textArea.addKeyListener(this);
        textArea.setEditable(true);
        history.add(textArea);
        history.setLayout(JFrameUtils.SINGLE_COLUMN_LAYOUT);
        content.setLayout(JFrameUtils.SINGLE_COLUMN_LAYOUT);
        content.setPreferredSize(new Dimension(315, 150));
        content.add(scrollPane);
    }

    
	@Override
	public StringBuilder getExtendedContent(StringBuilder content){
        //SaveLineCreator.appendSaveLine("history", textAreaHistory.getText(), content);
        //SaveLineCreator.appendSaveLine("code_line", textFieldCodeLine.getText(), content);
        return content;
    }

    
	@Override
	public void setVariable(String variable, String value){
        //if (variable.equals("history"))         textAreaHistory.setText(value);
        //else if (variable.equals("code_line"))  textFieldCodeLine.setText(value);
    }
    
	@Override
	public void reset(){
		history.removeAll();
	}    
    
    
	@Override
	protected String getType(){
    	return "codepad2";
    }

	private class QueObject{
		private final Component component;
		private final String command;
		//private final KeyEvent event;
    	public QueObject(String command, KeyEvent event, Component component){
    		this.command = command;
    		//this.event = event;
    		this.component = component;
    	}
    }
	
	
	@Override
	public final void destroy(){
		control.setStopFlag(true);
	}
}
