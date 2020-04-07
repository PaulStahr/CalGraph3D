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

import util.SaveLineCreator;
import maths.VariableStack;
/** 
* @author  Paul Stahr
* @version 04.02.2012
*/

public class InterfacePanelFactory {
	
	public static final byte CODE_PAD = 0, CODE_PAD2 = 1, SLIDER = 2, VARIABLE = 3, ANIMATION = 4, PERSPECTIVE = 5, PROGRAM = 6;
	
	
    public static InterfacePanel getInstance(Iterable<String> content, SaveLineCreator slc, VariableStack stack){
    	SaveLineCreator.SaveObject saveObject = slc.getSaveObject(content.iterator().next());
    		
        final InterfacePanel panel;
        	
        if (!saveObject.variable.equals("type") || (panel = create(saveObject.value, stack)) == null)
        	return null;

        panel.setContent(content, slc);
        return panel;
    }
    
    public static final InterfacePanel create(byte id, VariableStack stack)
    {
    	switch (id)
    	{
    		case 0:return new CodePadPanel(stack);
    		case 1:return new CodePadPanel2(stack);
    		case 2: return new SliderPanel(stack);
    		case 3:return new VariablePanel(stack);
    		case 4:return new AnimationPanel(stack);
    		case 5:return new PerspectivePanel();
    		case 6:return new ProgramEditorPanel(stack);
    		default: return null;
    	}
    }
    
    private static final InterfacePanel create(String name, VariableStack stack){
    	switch (name)
    	{
    		case "codepad":return new CodePadPanel(stack);
    		case "codepad2":return new CodePadPanel2(stack);
    		case "slider": return new SliderPanel(stack);
    		case "variable":return new VariablePanel(stack);
    		case "animation":return new AnimationPanel(stack);
    		case "perspective":return new PerspectivePanel();
    		case "program":return new ProgramEditorPanel(stack);
    		default: return null;
    	}
    }
    
    /*
     * Run This method only by Dispatcher
     */
    public static final InterfacePanel getInstance(String content, VariableStack stack){
        final int index = content.indexOf('\n');
        SaveLineCreator slc = new SaveLineCreator();
        SaveLineCreator.SaveObject saveObject = slc.getSaveObject(content, 0, index);
        final InterfacePanel panel;

        if (!saveObject.variable.equals("type") || (panel = create(saveObject.value, stack)) == null)
        	return null;
        
        panel.setContent(content, index + 1, content.length(), slc);
        return panel;
    }
}
