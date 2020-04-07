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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.GroupLayout.Alignment;

import data.DataHandler;
import jcomponents.Graph.GraphKind;
import jcomponents.util.JColorPickPanel;
import jcomponents.util.JMathTextField;

public class GraphWindow extends JFrame implements ActionListener{
    private static final long serialVersionUID = -2626118293074419749L;
    private static final String nameDrawTypeArray[] ={"Punkte", "Linien", "Linien Gestrichelt", "Fl\u00E4chen", "Fl\u00E4chen gegl\u00E4ttet"};    
    public final JLabel textKind = new JLabel("Art");
    public final JComboBox<GraphKind> listKind = new JComboBox<Graph.GraphKind>(Graph.GraphKind.values());
    public final JLabel textColor = new JLabel("Farbe");
    public final JColorPickPanel buttonColor = new JColorPickPanel();

    public final JLabel textDrawType = new JLabel("Ansicht");
    public final JComboBox<String> listDrawType = new JComboBox<String>(nameDrawTypeArray);

    public final JLabel textFunction0 = new JLabel();
    public final JLabel textFunction1 = new JLabel();
    public final JLabel textFunction2 = new JLabel();

    public final JMathTextField textFieldFunction0 = new JMathTextField();
    public final JMathTextField textFieldFunction1 = new JMathTextField();
    public final JMathTextField textFieldFunction2 = new JMathTextField();
    public final JMathTextField textFieldColor = new JMathTextField();

    public final JLabel textMin0 = new JLabel();
    public final JLabel textMax0 = new JLabel();
    public final JLabel textMin1 = new JLabel();
    public final JLabel textMax1 = new JLabel();

    public final JMathTextField textFieldMin0 = new JMathTextField();
    public final JMathTextField textFieldMax0 = new JMathTextField();
    public final JMathTextField textFieldMin1 = new JMathTextField();
    public final JMathTextField textFieldMax1 = new JMathTextField();

    public final JLabel textStep0 = new JLabel("Schrittl\u00E4nge x:");
    public final JLabel textStep1 = new JLabel("Schrittl\u00E4nge y:");

    public final JMathTextField textFieldStep0 = new JMathTextField();
    public final JMathTextField textFieldStep1 = new JMathTextField();

    public final JLabel textVisible = new JLabel("Sichtbar");
    public final JCheckBox checkBoxVisible = new JCheckBox();

    public final JLabel labelTransformation = new JLabel("Transform");
    public final JMathTextField textFieldTransformation = new JMathTextField();

    public final JLabel labelVertexColor = new JLabel("Vertex Farbe");
    public final JMathTextField textFieldVertexColor = new JMathTextField();
    
    public final JButton buttonOkay = new JButton("OK");
    public final JButton buttonAccept = new JButton("\u00DCbernehmen");
    public final JButton buttonAbort = new JButton("Abbrechen");

    public GraphKind tmpKind;
    @Override
	public void actionPerformed(ActionEvent ae){
    	Object source = ae.getSource();
    	if (source == listKind)
    	{
    		setWindowKind((GraphKind)listKind.getSelectedItem());
    	}
    }
    public GraphWindow(){
    	GroupLayout layout = new GroupLayout(getContentPane());
        setLayout(layout);

        listKind.addActionListener(this);
    	
    	final int gap = 5;
        layout.setVerticalGroup(layout.createSequentialGroup()
        	.addGap(gap).addGroup(layout.createParallelGroup()
        		.addComponent(textKind, Alignment.CENTER)
        		.addComponent(listKind, Alignment.CENTER)
        		.addComponent(textColor, Alignment.CENTER)
        		.addComponent(buttonColor, Alignment.CENTER, 15, 15, 15)
        	).addGap(gap).addGroup(layout.createParallelGroup()
        		.addComponent(textDrawType, Alignment.CENTER)
        		.addComponent(listDrawType, Alignment.CENTER)
        		.addComponent(textVisible, Alignment.CENTER)
        		.addComponent(checkBoxVisible, Alignment.CENTER)
        	).addGap(gap).addGroup(layout.createParallelGroup()
        		.addComponent(textFunction0, Alignment.CENTER)
        		.addComponent(textFieldFunction0, Alignment.CENTER)
        	).addGap(gap).addGroup(layout.createParallelGroup()
           		.addComponent(textFunction1, Alignment.CENTER)
           		.addComponent(textFieldFunction1, Alignment.CENTER)
        	).addGap(gap).addGroup(layout.createParallelGroup()
           		.addComponent(textFunction2, Alignment.CENTER)
           		.addComponent(textFieldFunction2, Alignment.CENTER)
        	).addGap(gap).addGroup(layout.createParallelGroup()
           		.addComponent(textColor, Alignment.CENTER)
           		.addComponent(textFieldColor, Alignment.CENTER)
        	).addGap(gap).addGroup(layout.createParallelGroup()
            	.addComponent(labelTransformation, Alignment.CENTER)
            	.addComponent(textFieldTransformation, Alignment.CENTER)
        	).addGap(gap).addGroup(layout.createParallelGroup()
        		.addComponent(textMin0, Alignment.CENTER)
        		.addComponent(textFieldMin0, Alignment.CENTER)
        		.addComponent(textMax0, Alignment.CENTER)
        		.addComponent(textFieldMax0, Alignment.CENTER)
        		.addComponent(textStep0, Alignment.CENTER)
        		.addComponent(textFieldStep0, Alignment.CENTER)
        	).addGap(gap).addGroup(layout.createParallelGroup()
           		.addComponent(textMin1, Alignment.CENTER)
           		.addComponent(textFieldMin1, Alignment.CENTER)
           		.addComponent(textMax1, Alignment.CENTER)
           		.addComponent(textFieldMax1, Alignment.CENTER)
           		.addComponent(textStep1, Alignment.CENTER)
           		.addComponent(textFieldStep1, Alignment.CENTER)
        	).addGap(gap).addGroup(layout.createParallelGroup()
        		.addComponent(buttonOkay, Alignment.CENTER)
        		.addComponent(buttonAccept, Alignment.CENTER)
        		.addComponent(buttonAbort, Alignment.CENTER)
        	).addGap(gap)
        );
        
        layout.setHorizontalGroup(layout.createSequentialGroup()
        	.addGap(gap).addGroup(
        		layout.createParallelGroup()
                .addGroup(layout.createSequentialGroup()
                	.addGroup(layout.createParallelGroup()
                		.addComponent(textKind)
                		.addComponent(textDrawType)
                	).addGap(gap).addGroup(layout.createParallelGroup()
                   		.addComponent(listKind)
                   		.addComponent(listDrawType)
                	).addGap(gap).addGroup(layout.createParallelGroup()
                   		.addComponent(textColor)
                   		.addComponent(textVisible)
                	).addGap(gap).addGroup(layout.createParallelGroup()
                   		.addComponent(buttonColor, 15, 15, 15)
                   		.addComponent(checkBoxVisible)
                	)
                ).addGroup(layout.createSequentialGroup()
                	.addGroup(layout.createParallelGroup()
                    	.addComponent(textFunction0)
                    	.addComponent(textFunction1)
                    	.addComponent(textFunction2)
                    	.addComponent(textColor)
                    	.addComponent(labelTransformation)
                	).addGap(gap).addGroup(layout.createParallelGroup()
                		.addComponent(textFieldFunction0)
                		.addComponent(textFieldFunction1)
                		.addComponent(textFieldFunction2)
                		.addComponent(textFieldColor)
                		.addComponent(textFieldTransformation)
                	)
                ).addGroup(layout.createSequentialGroup()
                	.addGroup(layout.createParallelGroup()
                		.addComponent(textMin0)
                		.addComponent(textMin1)
                	).addGap(gap).addGroup(layout.createParallelGroup()
                		.addComponent(textFieldMin0)
                		.addComponent(textFieldMin1)
                	).addGap(gap).addGroup(layout.createParallelGroup()
                		.addComponent(textMax0)
                		.addComponent(textMax1)
                	).addGap(gap).addGroup(layout.createParallelGroup()
                		.addComponent(textFieldMax0)
                		.addComponent(textFieldMax1)
                	).addGap(gap).addGroup(layout.createParallelGroup()
                		.addComponent(textStep0)
                		.addComponent(textStep1)
                	).addGap(gap).addGroup(layout.createParallelGroup()
                		.addComponent(textFieldStep0)
                		.addComponent(textFieldStep1)              		
                	)
                ).addGroup(layout.createSequentialGroup()
                	.addComponent(buttonOkay, buttonOkay.getPreferredSize().width, buttonOkay.getPreferredSize().width, 10000)
                	.addGap(gap).addComponent(buttonAccept, buttonAccept.getPreferredSize().width, buttonAccept.getPreferredSize().width, 10000)
                	.addGap(gap).addComponent(buttonAbort, buttonAbort.getPreferredSize().width, buttonAbort.getPreferredSize().width, 10000)
                )
        	).addGap(gap)
        );

        setTitle("Graph Einstellungen");
        setResizable(true);
        DataHandler.addToUpdateTree(this);
        
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }

    public void setWindowKind (GraphKind graphKind){
        textFunction0.setText           (graphKind.function0);
        textFieldFunction0.setEnabled   (graphKind.function0 != null);
        textFunction1.setText           (graphKind.function1);
        textFieldFunction1.setEnabled   (graphKind.function1 != null);
        textFunction2.setText           (graphKind.function2);
        textFieldFunction2.setEnabled   (graphKind.function2 != null);
        textMin0.setText                (graphKind.min0);
        textFieldMin0.setEnabled        (graphKind.min0 != null);
        textMax0.setText                (graphKind.max0);
        textFieldMax0.setEnabled        (graphKind.max0 != null);
        textStep0.setText               (graphKind.step0);
        textFieldStep0.setEnabled       (graphKind.step0 != null);
        textMin1.setText                (graphKind.min1);
        textFieldMin1.setEnabled        (graphKind.min1 != null);
        textMax1.setText                (graphKind.max1);
        textFieldMax1.setEnabled        (graphKind.max1 != null);
        textStep1.setText               (graphKind.step1);
        textFieldStep1.setEnabled       (graphKind.step1 != null);
        tmpKind=graphKind;
    }
    
    public void openWindow(){
    	if (!isVisible()){
            pack();
            setMinimumSize(getSize());
        }
        setVisible(true);
    }
}
