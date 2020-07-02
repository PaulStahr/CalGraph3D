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
package jcomponents.raytrace;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import data.raytrace.GuiOpticalSurfaceObject;
import data.raytrace.GuiOpticalSurfaceObject.OpticalSurfaceObjectChangeListener;
import data.raytrace.GuiOpticalVolumeObject;
import data.raytrace.GuiOpticalVolumeObject.OpticalVolumeObjectChangeListener;
import data.raytrace.MeshObject;
import data.raytrace.MeshObject.MeshObjectChangeListener;
import data.raytrace.OpticalObject;
import data.raytrace.OpticalObject.COLUMN_TYPES;
import data.raytrace.OpticalObject.SCENE_OBJECT_COLUMN_TYPE;
import data.raytrace.ParseUtil;
import maths.VariableAmount;
import maths.exception.OperationParseException;

public class OpticalObjectPanel extends JPanel implements OpticalSurfaceObjectChangeListener, OpticalVolumeObjectChangeListener, MeshObjectChangeListener, ItemListener, DocumentListener{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1955804638549283703L;
	private static final Logger logger = LoggerFactory.getLogger(OpticalObjectPanel.class);
	private final JLabel labelTableNames[] = new JLabel[GuiOpticalSurfaceObject.TYPES.colSize()];
	private final JComponent componentTableSelection[] = new JComponent[GuiOpticalSurfaceObject.TYPES.colSize()];
	private OpticalObject guiOpticalObject;
	private boolean isUpdating = false;
	private VariableAmount va;

	public final OpticalObject getOpticalObject()
	{
		return guiOpticalObject;
	}
	
	@Override
	public void valueChanged(final GuiOpticalSurfaceObject object, final SCENE_OBJECT_COLUMN_TYPE ct) {
		if (isUpdating && EventQueue.isDispatchThread())
		{
			return;
		}
		if (object == guiOpticalObject)
		{
			EventQueue.invokeLater(new Runnable()
			{
				@Override
				public void run()
				{
					isUpdating = true;
					if (ct == null)
					{
						for (int i = 0; i < GuiOpticalSurfaceObject.TYPES.colSize(); ++i)
						{
							try
							{
								readValue(GuiOpticalSurfaceObject.TYPES.getCol(i));
							}catch(Exception e)
							{
								logger.error("Can't read value", e);
							}
						}
					}
					else
					{
						readValue(ct);
					}
					isUpdating = false;
				}
			});
		}
	}

	@Override
	public void valueChanged(final MeshObject object, final SCENE_OBJECT_COLUMN_TYPE ct) {
		if (isUpdating && EventQueue.isDispatchThread())
		{
			return;
		}
		if (object == guiOpticalObject)
		{
			EventQueue.invokeLater(new Runnable()
			{
				@Override
				public void run()
				{
					isUpdating = true;
					if (ct == null)
					{
						for (int i = 0; i < MeshObject.TYPES.colSize(); ++i)
						{
							try
							{
								readValue(MeshObject.TYPES.getCol(i));
							}catch(Exception e)
							{
								logger.error("Can't read value", e);
							}
						}
					}
					else
					{
						readValue(ct);
					}
					isUpdating = false;
				}
			});
		}
	}

	@Override
	public void valueChanged(final GuiOpticalVolumeObject object, final SCENE_OBJECT_COLUMN_TYPE ct) {
		if (isUpdating && EventQueue.isDispatchThread())
		{
			return;
		}
		if (object == guiOpticalObject)
		{
			EventQueue.invokeLater(new Runnable()
			{
				@Override
				public void run()
				{
					COLUMN_TYPES columnTypes = object.getTypes();
					isUpdating = true;
					if (ct == null)
					{
						for (int i = 0; i < columnTypes.colSize(); ++i)
						{
							try
							{
								readValue(columnTypes.getCol(i));
							}catch(Exception e)
							{
								logger.error("Can't read value", e);
							}
						}
					}
					else
					{
						readValue(ct);
					}
					isUpdating = false;
				}
			});
		}
	}

	private void readValue(SCENE_OBJECT_COLUMN_TYPE ct)
	{
		Object value = guiOpticalObject.getValue(ct);
		if (value != null)
		{
			Component comp = componentTableSelection[guiOpticalObject.getTypes().getColumnNumber(ct)];
			if(comp instanceof JTextField)
			{
				((JTextField)comp).setText(value.toString());
			}
			else if (comp instanceof JComboBox<?>)
			{
				((JComboBox<?>)comp).setSelectedItem(value.toString());
			}
			else if (comp instanceof JCheckBox)
			{
				((JCheckBox)comp).setSelected((Boolean)value);
			}
		}
	}
	
	public void setGuiOpticalObject(OpticalObject goo)
	{
		if (this.guiOpticalObject != null)
		{
			if (this.guiOpticalObject instanceof GuiOpticalSurfaceObject)
			{
				((GuiOpticalSurfaceObject)this.guiOpticalObject).removeChangeListener(this);
			}
			else if (this.guiOpticalObject instanceof GuiOpticalVolumeObject)
			{
				((GuiOpticalVolumeObject)this.guiOpticalObject).removeChangeListener(this);
			}
			else if (this.guiOpticalObject instanceof MeshObject)
			{
				((MeshObject)this.guiOpticalObject).removeChangeListener(this);
			}
		}
		this.guiOpticalObject = goo;
		if (goo != null)
		{
			if (goo instanceof GuiOpticalSurfaceObject)
			{
				((GuiOpticalSurfaceObject)goo).addChangeListener(this);
				valueChanged((GuiOpticalSurfaceObject)goo, null);
			}
			else if (goo instanceof GuiOpticalVolumeObject)
			{
				((GuiOpticalVolumeObject)goo).addChangeListener(this);
				valueChanged((GuiOpticalVolumeObject)goo, null);
			}
			else if (goo instanceof MeshObject)
			{
				((MeshObject)goo).addChangeListener(this);
				valueChanged((MeshObject)goo, null);
			}
		}
	}
	
	private int getIndex (Object comp)
	{
		for (int i = 0; i < componentTableSelection.length; ++i)
		{
			if (componentTableSelection[i] == comp)
			{
				return i;
			}
		}
		return -1;
	}
	
	@Override
	public void itemStateChanged(ItemEvent e) {
		if (!EventQueue.isDispatchThread())
		{
			throw new RuntimeException("Can only be called by dispatch thread");
		}
		if (!isUpdating && guiOpticalObject != null)
		{
			Object source = e.getSource();
			int index = getIndex(source);
			SCENE_OBJECT_COLUMN_TYPE current = guiOpticalObject.getTypes().getCol(index);
			ParseUtil parser = new ParseUtil();
			
			if (source instanceof JComboBox<?>)
			{
				JComboBox<?> comboBox = (JComboBox<?>)e.getSource();
				
				isUpdating = true;
				try {
					guiOpticalObject.setValue(current, comboBox.getSelectedItem(), va, parser);
				} catch (OperationParseException ex) {
					logger.error("Can't read math expression",ex);
				} catch (NumberFormatException ex) {
					logger.error("Can't read number", ex);
				}
				isUpdating = false;
			}
			else if (source instanceof JCheckBox)
			{
				JCheckBox checkBox = (JCheckBox)source;
				if (!isUpdating && guiOpticalObject != null)
				{
					isUpdating = true;
					try {
						guiOpticalObject.setValue(current, checkBox.isSelected(), va, parser);
					} catch (OperationParseException ex) {
						logger.error("Can't read math expression",ex);
					} catch (NumberFormatException ex) {
						logger.error("Can't read number", ex);
					}
					isUpdating = false;
				}
			}
		}
	}
	
	public OpticalObjectPanel(OpticalObject goo, VariableAmount va)
	{
		final GroupLayout layout = new GroupLayout(this);
		this.va = va;
		setLayout(layout);
		
		int numCols = 3;
		
		GroupLayout.Group horizontalGroup = layout.createSequentialGroup();
		GroupLayout.Group verticalGroup = layout.createSequentialGroup();
		COLUMN_TYPES ct = goo.getTypes();
		GroupLayout.ParallelGroup rows[] = new GroupLayout.ParallelGroup[ct.colSize()];
		GroupLayout.ParallelGroup cols[] = new GroupLayout.ParallelGroup[numCols * 2];
		
		for (int i = 0; i < rows.length; ++i)
		{
			verticalGroup.addGroup(rows[i] =  layout.createParallelGroup());
		}
		
		for (int i = 0; i < cols.length; ++i)
		{
			horizontalGroup.addGroup(cols[i] =  layout.createParallelGroup());
		}
		
		layout.setHorizontalGroup(horizontalGroup);
		layout.setVerticalGroup(verticalGroup);
		
		for (int i = 0; i < ct.colSize(); ++i)
		{
			final SCENE_OBJECT_COLUMN_TYPE current = ct.getCol(i);
			labelTableNames[i] = new JLabel(current.name);
			switch(current.optionType)
			{
			case OpticalObject.TYPE_BUTTON:
				componentTableSelection[i] = new JButton();
				break;
			case OpticalObject.TYPE_CHECKBOX:
				final JCheckBox checkBox = new JCheckBox();
				componentTableSelection[i] = checkBox;
				checkBox.addItemListener(this);
				break;
			case OpticalObject.TYPE_COMBOBOX:
				final JComboBox<String> comboBox = new JComboBox<String>(current.possibleValues.toArray(new String[current.possibleValues.size()]));
				componentTableSelection[i] = comboBox;
				comboBox.addItemListener(this);
				break;
			case OpticalObject.TYPE_TEXTFIELD:
			case OpticalObject.TYPE_COLOR:
				final JTextField textField = new JTextField();
				componentTableSelection[i] = textField;
				textField.getDocument().addDocumentListener(this);
				break;
			default:
				break;
			}
			int percolumn = (ct.colSize() + numCols - 1) / numCols;
			int column = i/ percolumn;
			rows[i % percolumn].addComponent(labelTableNames[i], Alignment.CENTER);
			cols[column * 2].addComponent(labelTableNames[i]);
			rows[i % percolumn].addComponent(componentTableSelection[i]);
			cols[column * 2 + 1].addComponent(componentTableSelection[i]);
		}
		setGuiOpticalObject(goo);
		setMaximumSize(new Dimension(1000000000, getMinimumSize().height));
        //setMinimumSize(getSize());
	}
	
	public void dispose()
	{
		if (guiOpticalObject != null)
		{
			setGuiOpticalObject(null);
		}
	}
	private final ParseUtil parser = new ParseUtil();

	@Override
	public void changedUpdate(DocumentEvent arg0) {
		if (guiOpticalObject == null || isUpdating)
		{
			return;
		}
		if (!EventQueue.isDispatchThread())
		{
			throw new RuntimeException("Can only be called by dispatch thread");
		}
		Document doc = arg0.getDocument();
		for (int i = 0; i < componentTableSelection.length; ++i)
		{
			if (componentTableSelection[i] instanceof JTextField)
			{
				JTextField tf = (JTextField)componentTableSelection[i];
				if (tf.getDocument() == doc)
				{
					isUpdating = true;
					try
					{
						guiOpticalObject.setValue(guiOpticalObject.getTypes().getCol(i), tf.getText(), OpticalObjectPanel.this.va, parser);
						tf.setBackground(Color.WHITE);
					} catch (OperationParseException e) {
						tf.setBackground(Color.PINK);
					} catch (NumberFormatException e) {
						tf.setBackground(Color.PINK);
					} catch (Exception e) {
						tf.setBackground(Color.PINK);
					}
					isUpdating = false;
				}
			}
		}
		
	}

	@Override
	public void insertUpdate(DocumentEvent arg0) {changedUpdate(arg0);}

	@Override
	public void removeUpdate(DocumentEvent arg0) {changedUpdate(arg0);}
}