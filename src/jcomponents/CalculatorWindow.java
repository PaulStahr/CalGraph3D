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

import javax.swing.*;

import data.DataHandler;
import jcomponents.util.JComponentSingletonInstantiator;
import maths.Controller;
import maths.Operation;
import maths.Operation.Print;
import maths.OperationCompiler;
import maths.exception.OperationParseException;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.io.IOException;

/** 
* @author  Paul Stahr
* @version 04.02.2012
*/
public class CalculatorWindow extends JFrame implements KeyListener, WindowFocusListener, MouseListener
{
    private static final long serialVersionUID = 7111299315666304724L;

	private static final JComponentSingletonInstantiator<CalculatorWindow> instantiator = new JComponentSingletonInstantiator<CalculatorWindow>(CalculatorWindow.class);
    private final JTextField textFieldAnzeige = new JTextField();
    private final JToggleButton buttonLatex = new JToggleButton("Tex");
    private final JToggleButton buttonReset = new JToggleButton("C");
    private final JToggleButton buttonNumbers[] = new JToggleButton[10];
    private final JToggleButton buttonPoint = new JToggleButton(".");
    private final JToggleButton buttonMult = new JToggleButton("*");
    private final JToggleButton buttonDiv = new JToggleButton("/");
    private final JToggleButton buttonAdd = new JToggleButton("+");
    private final JToggleButton buttonSub = new JToggleButton("-");
    private final JToggleButton buttonClibOn = new JToggleButton("(");
    private final JToggleButton buttonClibTo = new JToggleButton(")");
    private final JToggleButton buttonRes = new JToggleButton("\u2714");
    private final Controller control = new Controller();
    private final StringBuilder strB = new StringBuilder();

    public static final ActionListener getOpenWindowListener()
    {
    	return instantiator;
    }
    
	@Override
	public void keyPressed(KeyEvent ke) {
		if (ke.isControlDown()){
			switch (ke.getKeyCode()){
				case KeyEvent.VK_A:{
					textFieldAnzeige.setSelectionStart(0);
					textFieldAnzeige.setSelectionEnd(textFieldAnzeige.getText().length());
					break;
				}case KeyEvent.VK_C:{
					Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(textFieldAnzeige.getText()), null);
					break;
				}case KeyEvent.VK_V:{
            		final Clipboard systemClipboard = Toolkit.getDefaultToolkit().getSystemClipboard(); 
            		final Transferable transferData = systemClipboard.getContents( null ); 
        			try{
        				for(DataFlavor dataFlavor : transferData.getTransferDataFlavors()){ 
        					Object content = transferData.getTransferData(dataFlavor); 
        					if (content instanceof String){
        						insertAtCaret((String)content);
        						break;
        					}
        				}
        			}catch(IOException e){}
        			catch(UnsupportedFlavorException e){}
				}
			}
			return;
		}

		switch (ke.getKeyCode()){				
			case KeyEvent.VK_RIGHT:{
				final int cPos = textFieldAnzeige.getCaretPosition();
				if (cPos >= textFieldAnzeige.getText().length())
					return;
				textFieldAnzeige.setCaretPosition(textFieldAnzeige.getCaretPosition() + 1);
				break;
			}case KeyEvent.VK_LEFT:{
				final int cPos = textFieldAnzeige.getCaretPosition();
				if (cPos <= 0)
					return;
				textFieldAnzeige.setCaretPosition(textFieldAnzeige.getCaretPosition() - 1);
			}
		}
		if(!ke.isActionKey())
			setButtonSelected(ke.getKeyChar(), true);	
	}
	
	@Override
	public void keyReleased(KeyEvent ke) {
		if(!ke.isActionKey())
			setButtonSelected(ke.getKeyChar(), false);	
	}
	
	private void setButtonSelected(char c, boolean selected){
		if (c <= '9' && c >= '0'){
			buttonNumbers[c - '0'].setSelected(selected);
		}else{
			switch(c){
				case '+':buttonAdd.setSelected(selected);break;
				case '-':buttonSub.setSelected(selected);break;
				case '*':buttonMult.setSelected(selected);break;
				case '/':buttonDiv.setSelected(selected);break;
				case '\n':buttonRes.setSelected(selected);break;
				case '.':buttonPoint.setSelected(selected);break;
			}
		}
	}

	@Override
	public void keyTyped(KeyEvent ke) {
		if (ke.isControlDown())
			return;
		switch (ke.getKeyChar()){
			case 27:{
				return;
			}case '\n':{
				calculate();
				break;
			}case 8:{
				final int sPos = textFieldAnzeige.getSelectionStart();
				final int ePos = textFieldAnzeige.getSelectionEnd();
				final String text = textFieldAnzeige.getText();
				if (sPos != ePos){
					textFieldAnzeige.setText(text.substring(0, sPos).concat(text.substring(ePos, text.length())));
					return;
				}
				final int cPos = textFieldAnzeige.getCaretPosition();
				if (cPos < 1)
					break;
				textFieldAnzeige.setText(text.substring(0, cPos - 1).concat(text.substring(cPos)));
				textFieldAnzeige.setCaretPosition(cPos - 1);
				break;
			}case 127:{
				final int sPos = textFieldAnzeige.getSelectionStart();
				final int ePos = textFieldAnzeige.getSelectionEnd();
				final String text = textFieldAnzeige.getText();
				if (sPos != ePos){
					textFieldAnzeige.setText(text.substring(0, sPos).concat(text.substring(ePos, text.length())));
					return;
				}
				final int cPos = textFieldAnzeige.getCaretPosition();
				if (cPos < 0 || cPos >= text.length())
					break;
				textFieldAnzeige.setText(text.substring(0, cPos).concat(text.substring(cPos + 1)));
				textFieldAnzeige.setCaretPosition(cPos);
				break;					
			}default:{
				insertAtCaret(String.valueOf(ke.getKeyChar()));
				break;
			}
		}
	}

    private final void calculate(){
		try {
			strB.setLength(0);
			OperationCompiler.compile(textFieldAnzeige.getText()).calculate(DataHandler.globalVariables, control).toString(strB);
			textFieldAnzeige.setText(strB.toString());
		} catch (OperationParseException e) {}   	
    }
    
    private final void insertAtCaret(String str){
		final String text = textFieldAnzeige.getText();
		final int sPos = textFieldAnzeige.getSelectionStart();
		final int ePos = textFieldAnzeige.getSelectionEnd();
		strB.setLength(0);
		strB.append(text.substring(0, sPos)).append(str).append(text.substring(ePos));
		textFieldAnzeige.setText(strB.toString());
		textFieldAnzeige.setCaretPosition(sPos + str.length());    	
    }
   
    public static final synchronized JFrame getInstance(){
    	return instantiator.get();
    }
    
    public CalculatorWindow(){
    	final int width = 50, height = 40, gaps = 2;
        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        setFocusable(true);
        requestFocus();
        addKeyListener(this);
        textFieldAnzeige.getCaret().setVisible(true);
        textFieldAnzeige.getCaret().setSelectionVisible(true);
        textFieldAnzeige.setEditable(false);
        //textFieldAnzeige.getDocument().addDocumentListener(dl);
        for (int i=0;i<buttonNumbers.length;i++)
        	buttonNumbers[i] = new JToggleButton(String.valueOf((char)(i+'0')).intern());
        
        layout.setHorizontalGroup(layout.createParallelGroup()
            .addComponent(textFieldAnzeige, 0, 0, 10000)
        	.addGroup(layout.createSequentialGroup()
        		.addComponent(buttonReset, width, width, width)
        		.addGap(gaps).addComponent(buttonClibOn, width, width, width)
        		.addGap(gaps).addComponent(buttonClibTo, width, width, width)
        		.addGap(gaps).addComponent(buttonLatex, width, width, width)
        	).addGroup(layout.createSequentialGroup()
        		.addComponent(buttonNumbers[7], width, width, width)
        		.addGap(gaps).addComponent(buttonNumbers[8], width, width, width)
        		.addGap(gaps).addComponent(buttonNumbers[9], width, width, width)
        		.addGap(gaps).addComponent(buttonAdd, width, width, width)
        	).addGroup(layout.createSequentialGroup()
        		.addComponent(buttonNumbers[4], width, width, width)
        		.addGap(gaps).addComponent(buttonNumbers[5], width, width, width)
        		.addGap(gaps).addComponent(buttonNumbers[6], width, width, width)
        		.addGap(gaps).addComponent(buttonSub, width, width, width)
        	).addGroup(layout.createSequentialGroup()
        		.addComponent(buttonNumbers[1], width, width, width)
        		.addGap(gaps).addComponent(buttonNumbers[2], width, width, width)
        		.addGap(gaps).addComponent(buttonNumbers[3], width, width, width)
        		.addGap(gaps).addComponent(buttonMult, width, width, width)
        	).addGroup(layout.createSequentialGroup()
                .addComponent(buttonPoint, width, width, width)
               	.addGap(gaps).addComponent(buttonNumbers[0], width, width, width)
               	.addGap(gaps).addComponent(buttonRes, width, width, width)
            	.addGap(gaps).addComponent(buttonDiv, width, width, width)
        	)
        );
        
        layout.setVerticalGroup(layout.createSequentialGroup()
            .addComponent(textFieldAnzeige, height, height, height)
        	.addGap(gaps).addGroup(layout.createParallelGroup()
            	.addComponent(buttonReset, height, height, height)
            	.addComponent(buttonClibOn, height, height, height)
            	.addComponent(buttonClibTo, height, height, height)
            	.addComponent(buttonLatex, height, height, height)
           	).addGap(gaps).addGroup(layout.createParallelGroup()
           		.addComponent(buttonNumbers[7], height, height, height)
           		.addComponent(buttonNumbers[8], height, height, height)
           		.addComponent(buttonNumbers[9], height, height, height)
        		.addComponent(buttonAdd, height, height, height)
           	).addGap(gaps).addGroup(layout.createParallelGroup()
           		.addComponent(buttonNumbers[4], height, height, height)
           		.addComponent(buttonNumbers[5], height, height, height)
           		.addComponent(buttonNumbers[6], height, height, height)
        		.addComponent(buttonSub, height, height, height)
           	).addGap(gaps).addGroup(layout.createParallelGroup()
           		.addComponent(buttonNumbers[1], height, height, height)
           		.addComponent(buttonNumbers[2], height, height, height)
           		.addComponent(buttonNumbers[3], height, height, height)
        		.addComponent(buttonMult, height, height, height)
           	).addGap(gaps).addGroup(layout.createParallelGroup()
               	.addComponent(buttonPoint, height, height, height)
           		.addComponent(buttonNumbers[0], height, height, height)
           		.addComponent(buttonRes, height, height, height)
        		.addComponent(buttonDiv, height, height, height)
          	)
        );
     
        for (Component c : getContentPane().getComponents()){
        	c.setFocusable(false);
        	c.addMouseListener(this);
        }
       	
        setTitle("Taschenrechner");
        setResizable(false);
        pack();
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        
		DataHandler.addToUpdateTree(this);

		addWindowFocusListener(this);
    }
    
    @Override
	public void mousePressed(MouseEvent me) {
		final Object o = me.getSource();
		if (!(o instanceof JToggleButton))
			return;
		final JToggleButton tb = (JToggleButton)o;
		tb.setSelected(true);			
	}

	
	@Override
	public void mouseReleased(MouseEvent me) {
		final Object o = me.getSource();
		if (!(o instanceof JToggleButton))
			return;
		final JToggleButton tb = (JToggleButton)o;
		tb.setSelected(false);
		if (tb == buttonRes){
			calculate();
			return;
		}
		if (tb == buttonReset){
			textFieldAnzeige.setText("");
			return;
		}
		if (tb == buttonLatex){
			String text = textFieldAnzeige.getText();
			try {
				Operation op = OperationCompiler.compile(text);
				strB.setLength(0);
	    		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(op.toString(Print.LATEX, strB).toString()), null);
			} catch (OperationParseException e) {
			}
			return;
		}
		insertAtCaret(tb.getText());
	}
    
    @Override
	public void windowLostFocus(WindowEvent arg0) {}
	
	
	@Override
	public void windowGainedFocus(WindowEvent arg0) {
		requestFocus();
	}

	@Override
	public void mouseClicked(MouseEvent arg0) {}

	@Override
	public void mouseEntered(MouseEvent arg0) {}

	@Override
	public void mouseExited(MouseEvent arg0) {}
}
