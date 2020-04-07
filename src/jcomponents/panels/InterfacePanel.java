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

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import data.ProgramIcons;
import util.JFrameUtils;
import util.SaveLineCreator;

/** 
* @author  Paul Stahr
* @version 04.02.2012
*/
public abstract class InterfacePanel extends JPanel implements MouseListener
{
    /**
     * 
     */
    private static final long serialVersionUID 		= -1812262885758841137L;
    private static final Dimension dim = new Dimension(315, 25);
    
    private final JLabel labelArrow				= new JLabel(ProgramIcons.arrowRight);
    private final JLabel labelUp				= new JLabel(ProgramIcons.triangleUp);
    private final JLabel labelDown				= new JLabel(ProgramIcons.triangleDown);
    private final JLabel labelRemove			= new JLabel(ProgramIcons.iconDelete);
    protected final JPanel content              = new JPanel();
    
    private boolean isExtended;

    public InterfacePanel(String title){
    	setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));
    	
        final JPanel panelTitle             = new JPanel(JFrameUtils.LEFT_FLOW_LAYOUT);
        final JLabel labelTitle             = new JLabel();
        panelTitle.setBackground(Color.GRAY);
        add(panelTitle);
        
        labelArrow.addMouseListener(this);
        labelUp.addMouseListener(this);
        labelDown.addMouseListener(this);
        labelRemove.addMouseListener(this);
        labelTitle.setText(title);       
        
        panelTitle.add(labelArrow);
        panelTitle.add(labelUp);
        panelTitle.add(labelDown);
        panelTitle.add(labelTitle);
        panelTitle.add(labelRemove);
        
        panelTitle.setPreferredSize(dim);
        panelTitle.setSize(dim);

        content.setVisible(false);
        add(content);
    }

    public boolean isExtended(){
        return isExtended;
    }

    public void setExtended(boolean extended){
    	content.setVisible(isExtended = extended);
        labelArrow.setIcon(isExtended ? ProgramIcons.arrowDown : ProgramIcons.arrowRight);
    }
    
    public final StringBuilder getContent(StringBuilder erg){
        SaveLineCreator.appendSaveLine("type", getType(), erg);
        SaveLineCreator.appendSaveLine("extended", isExtended(), erg);
        getExtendedContent(erg);
        return erg;
    }

    public final String getContent(){
    	return getContent(new StringBuilder()).toString();
    }
    
    protected abstract String getType();
    
    protected abstract StringBuilder getExtendedContent(StringBuilder strBuilder);

    public void setContent(Iterable<String> content, SaveLineCreator saveLineCreator){
    	for (String line : content){
        	final SaveLineCreator.SaveObject saveObject = saveLineCreator.getSaveObject(line);
        	if (saveObject != null){
                final String variable = saveObject.variable;
 	            if (variable.equals("extended"))
	                setExtended(Boolean.parseBoolean(saveObject.value));
	            else
	                setVariable(variable, saveObject.value);
        	}    		
    	}
    }
    
    public void setContent(String content, int begin, int end, SaveLineCreator saveLineCreator){
    	int startIndex = begin;
        while (startIndex < end){
            int index = content.indexOf('\n', startIndex);
            if (index == -1)
            {
            	index = end;
            }
        	final SaveLineCreator.SaveObject saveObject = saveLineCreator.getSaveObject(content, startIndex, index);
        	if (saveObject != null){
                final String variable = saveObject.variable;
	            if (variable.equals("extended"))
	                setExtended(Boolean.parseBoolean(saveObject.value));
	            else
	                setVariable(variable, saveObject.value);
        	}
        	startIndex = index + 1;
        }
    }

    public abstract void setVariable(String variable, String value);

    public void reset(){}
    
    public void destroy(){}

    @Override
	public void mouseReleased (MouseEvent me){
		Object source = me.getSource();
		if (source == labelArrow)
		{
	    	setExtended(!isExtended);			
		}
    }
    
	@Override
	public void mousePressed (MouseEvent me){
		Object source = me.getSource();
		if (source == labelArrow)
		{
			labelArrow.setIcon(ProgramIcons.arrowRightDown);
		}
		else if (source == labelUp || source == labelDown)
		{
			Container container = getParent();
            if (!(container instanceof JPanel))
                return;
            int pos = container.getComponentZOrder(this);
            if (source == labelUp)
            {
                if (--pos < 0)
                	return;
            }
            else
            {
            	if (++pos >= container.getComponentCount())
                	return;
            }
            container.setComponentZOrder(this, pos);
            ((JPanel)container).revalidate();
		}
		else if (source == labelRemove)
		{
			Container container = getParent();
			container.remove(this);
	    	destroy();
	    	container.revalidate();
	    	container.repaint();
		}
    }
    
	@Override
	public void mouseClicked(MouseEvent e) {}

	@Override
	public void mouseEntered(MouseEvent e) {}

	@Override
	public void mouseExited(MouseEvent e) {}
}
