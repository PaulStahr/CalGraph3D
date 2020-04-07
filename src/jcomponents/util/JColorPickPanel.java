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
package jcomponents.util;

import java.awt.event.*;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.util.ArrayList;


/**
 * Dies ist ein Label, dass sich verh\u00E4lt wie ein Button und gemacht ist um eine Farbe zu w\u00E4hlen
 * 
 * @author Paul Stahr 
 * @version 04.02.2012
 */
public class JColorPickPanel extends JPanel implements MouseListener
{
	private static final long serialVersionUID = -2451553405680049378L;
	private static final Logger logger = LoggerFactory.getLogger(JColorPickPanel.class);
	private static final Border BORDER_RAISED = BorderFactory.createRaisedBevelBorder();
	private static final Border BORDER_LINE_GRAY = BorderFactory.createLineBorder(Color.LIGHT_GRAY);
	private static final Border BORDER_LOWERED = BorderFactory.createLoweredBevelBorder();
	
	private final ArrayList<ChangeListener> changeListener = new ArrayList<ChangeListener>(1);

	public JColorPickPanel(){
		this(Color.BLACK);
	}
	
	public JColorPickPanel(Color c){
        setBorder(BORDER_RAISED);
        addMouseListener(this); 
        setBackground(c);
    }
    
    public void addChangeListener(ChangeListener ch){
    	if (ch == null)
    		throw new NullPointerException();
    	changeListener.add(ch);
    }
    
    public void removeChangeListener(ChangeListener ch){
    	if (ch == null)
    		throw new NullPointerException();
    	changeListener.remove(ch);
    }
    
	@Override
	public void setEnabled(boolean enabled){
        super.setEnabled(enabled);
        setBorder(enabled ? BORDER_RAISED : BORDER_LINE_GRAY);
    }

	@Override
	public void mousePressed(MouseEvent me) { 
        if (!isEnabled())
        	return;
        setBorder(BORDER_LOWERED);
    }

	@Override
	public void mouseReleased(MouseEvent me) { 
        if (!isEnabled())
        	return;
        setBorder(BORDER_RAISED);
        final Color c = JColorChooser.showDialog(null, "Farbe w\u00E4hlen", getBackground());
        if (c == null)
         	return;
        setBackground(c);
        for (int i=0;i<changeListener.size();i++){
        	try{
        		changeListener.get(i).stateChanged(new ChangeEvent(this));
        	}catch(Exception e){
        		logger.error("Error at running listener", e);
        	}
        }
    }
	
	@Override
	public void mouseClicked(MouseEvent arg0) {}

	@Override
	public void mouseEntered(MouseEvent arg0) {}

	@Override
	public void mouseExited(MouseEvent arg0) {}
}