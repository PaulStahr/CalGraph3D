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

import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;

import geometry.Rotation3;
import geometry.Vector3f;

import jcomponents.Interface;
import util.JFrameUtils;
/** 
 * Ein Panel mit Buttons f\u00FCr die Perspektive
* @author  Paul Stahr
* @version 04.02.2012
*/
public class PerspectivePanel extends InterfacePanel implements ActionListener
{
    private static final long serialVersionUID = -2277005862276455381L;
    private static final LayoutManager layout = JFrameUtils.DOUBLE_ROW_LAUYOUT;
    
    private final JButton buttonTop             = new JButton("Oben");
    private final JButton buttonBottom          = new JButton("Unten");
    private final JButton buttonFront           = new JButton("Vorne");
    private final JButton buttonBack            = new JButton("Hinten");
    private final JButton buttonLeft            = new JButton("Links");
    private final JButton buttonRight           = new JButton("Rechts");

	@Override
	public void actionPerformed(ActionEvent e) {
		Object source = e.getSource();
		Vector3f pos;
		Rotation3 rot;
		if (source == buttonTop)
		{
			pos = new Vector3f(0f,0f,10f);
			rot = new Rotation3(0f,0f,0f, true);
		}
		else if (source == buttonBottom)
		{
			pos = new Vector3f(0f,0f,-10f);
			rot = new Rotation3(180f,0f,0f, true);
		}
		else if (source == buttonFront)
		{
			pos = new Vector3f(10f,0f,0f);
			rot = new Rotation3(90f,0f,90f, true);
		}
		else if (source == buttonBack)
		{
			pos = new Vector3f(-10f,0f,0f);
			rot = new Rotation3(90f,0f,-90f, true);
		}
		else if(source == buttonLeft)
		{
			pos = new Vector3f(0f,-10f,0f);
			rot = new Rotation3(90f,0f,0f, true);
		}
		else if (source == buttonRight)
		{
			pos = new Vector3f(0f,10f,0f);
			rot = new Rotation3(90f,0f,180f, true);
		}
		else
		{
			throw new IllegalArgumentException();
		}
		Interface.scene.setCameraPosition(pos, rot);
	}

	public PerspectivePanel(){
        super("Perspektive");
        content.setLayout(layout);
        
        buttonTop.addActionListener(this);
        content.add(buttonTop);
        buttonFront.addActionListener(this);
        content.add(buttonFront);
        buttonLeft.addActionListener(this);
        content.add(buttonLeft);
        buttonBottom.addActionListener(this);
        content.add(buttonBottom);
        buttonBack.addActionListener(this);
        content.add(buttonBack);
        buttonRight.addActionListener(this);
        content.add(buttonRight);
    }
    
    
	@Override
	protected String getType(){
    	return "perspective";
    }

    
	@Override
	public StringBuilder getExtendedContent(StringBuilder content){
        return content;
    }

    
	@Override
	public final void setVariable(String variable, String value){}
}
