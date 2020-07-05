package jcomponents.raytrace;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.io.IOException;

import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import data.DataHandler;
import data.raytrace.GuiOpticalSurfaceObject;
import data.raytrace.GuiOpticalSurfaceObject.OpticalSurfaceObjectChangeListener;
import data.raytrace.GuiOpticalVolumeObject;
import data.raytrace.GuiOpticalVolumeObject.OpticalVolumeObjectChangeListener;
import data.raytrace.MeshObject;
import data.raytrace.MeshObject.MeshObjectChangeListener;
import data.raytrace.OpticalObject.SCENE_OBJECT_COLUMN_TYPE;
import data.raytrace.PropertyOnLineCalculator;
import data.raytrace.RaytraceScene;
import geometry.Vector3d;
import io.Drawer;
import jcomponents.RecentFileList;
import jcomponents.util.JMathTextField;
import maths.Controller;
import maths.Operation;
import maths.algorithm.OperationCalculate;
import util.JFrameUtils;
import util.StringUtils;

public class PropertyOnLineVisualization extends JFrame implements ActionListener, ItemListener{
	/**
	 * 
	 */
	private static final long serialVersionUID = 2118109937042606456L;
	private static final Logger logger = LoggerFactory.getLogger(PropertyOnLineVisualization.class);
	private final LineIntersectionVisalizationPanel visualization= new LineIntersectionVisalizationPanel();
	private final JMenuItem menuItemSave = new JMenuItem("Save");
	private final JLabel labelDirection = new JLabel("Direction");
	private final JMathTextField textFieldDirection = new JMathTextField("{1,0,0}");
	private final JLabel labelPosition = new JLabel("Position");
	private final JMathTextField textFieldPosition = new JMathTextField("{0,0,0}");
	private final JLabel labelRange = new JLabel("Range");
	private final JMathTextField textFieldRange = new JMathTextField(" {0,1}");
	private final JPanel panelSettings = new JPanel();
	private final JCheckBoxMenuItem menuItemAutoUpdate = new JCheckBoxMenuItem("Auto update");
	private final JCheckBox checkBoxSphereArc = new JCheckBox("Sphere Arc");
	private final JComboBox<String> comboBoxMode = new JComboBox<String>(new String[] {"Rafractive Index", "Divergence"});
	public final PropertyOnLineCalculator polc;
	
	private void addTo(JMenu menu, JMenuItem item)
	{
		menu.add(item);
		item.addActionListener(this);
	}
	
	public PropertyOnLineVisualization(RaytraceScene scene)
	{
		polc = new PropertyOnLineCalculator(scene);
		panelSettings.setLayout(JFrameUtils.SINGLE_ROW_LAYOUT);
		panelSettings.add(comboBoxMode);
		comboBoxMode.addItemListener(this);
		panelSettings.add(labelPosition);
		panelSettings.add(textFieldPosition);
		panelSettings.add(labelDirection);
		panelSettings.add(textFieldDirection);
		panelSettings.add(labelRange);
		panelSettings.add(textFieldRange);
		panelSettings.add(checkBoxSphereArc);
		JMenuBar menubar = new JMenuBar();
		this.visualization.scene = scene;
		this.setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
		add(panelSettings);
		panelSettings.setPreferredSize(new Dimension(1000000, 25));
		panelSettings.setMaximumSize(new Dimension(1000000, 25));
		add(visualization);
		setJMenuBar(menubar);
		JMenu menuFile = new JMenu("File");
		menubar.add(menuFile);
		addTo(menuFile, menuItemSave);
		addTo(menuFile, menuItemAutoUpdate);
		setSize(500,300);
	}

	@Override
	public void itemStateChanged(ItemEvent event) {
		Object source = event.getSource();
		if (source == comboBoxMode)
		{
			visualization.repaint();
		}
	}
	
	@Override
	public void actionPerformed(ActionEvent ae)
	{
		Object source = ae.getSource();
		if (source == menuItemSave)
		{
			JFileChooser fileChooser= new JFileChooser();
            JPanel panel = new JPanel();
            panel.add(new RecentFileList(fileChooser, DataHandler.getRecentFiles()));
            JCheckBox checkBox = new JCheckBox("Export Data Objects"); 
            panel.add(checkBox);
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
            fileChooser.setAccessory(panel);
            if (fileChooser.showSaveDialog(null) != JFileChooser.APPROVE_OPTION)
            {
            	return;
            }
            File file = fileChooser.getSelectedFile();
            DataHandler.addRecentFile(file.getAbsolutePath());
            String type = StringUtils.getFileType(file.getName());
            if (type.equals("dat") || type.equals("csv"))
        	{
            	try {
					StringUtils.writeTapSeperated(polc.dataPoints, file, 2);
				} catch (IOException ex) {
					JFrameUtils.logErrorAndShow("Can't save to File", ex, logger);
				}
        	}
		}
		else if (source == menuItemAutoUpdate)
		{
			visualization.setAutoUpdate(menuItemAutoUpdate.isSelected());
		}
	}
	
	private class LineIntersectionVisalizationPanel extends JPanel implements OpticalSurfaceObjectChangeListener, OpticalVolumeObjectChangeListener, MeshObjectChangeListener
	{
		/**
		 * 
		 */
		private static final long serialVersionUID = 236757326914804667L;
		private final Vector3d position = new Vector3d(0,0,0);
		private final Vector3d direction = new Vector3d(1,0,0);
		Controller controll = new Controller();
		private final Drawer.GraphicsDrawer drawer = new Drawer.GraphicsDrawer(null);
		RaytraceScene scene;
		
		public LineIntersectionVisalizationPanel()
		{
			
		}

		public void setAutoUpdate(boolean selected) {
			if (selected)
			{
				scene.addObjectChangeListener((OpticalSurfaceObjectChangeListener)this);
				scene.addObjectChangeListener((OpticalVolumeObjectChangeListener)this);
				scene.addObjectChangeListener((MeshObjectChangeListener)this);
			}
			else
			{
				scene.removeObjectChangeListener((OpticalSurfaceObjectChangeListener)this);
				scene.removeObjectChangeListener((OpticalVolumeObjectChangeListener)this);
				scene.removeObjectChangeListener((MeshObjectChangeListener)this);				
			}
		}

		@Override
		public void paintComponent(Graphics g)
		{
			super.paintComponent(g);
			OperationCalculate.toDoubleArray(textFieldPosition.get().calculate(scene.vs, controll), position);
			OperationCalculate.toDoubleArray(textFieldDirection.get().calculate(scene.vs, controll), direction);
			position.add(direction, -0.00001);
			drawer.setOutput(g);
			Operation op = textFieldRange.get().calculate(scene.vs, controll);
			double rangeBegin = op.get(0).doubleValue();
			double rangeEnd = op.get(1).doubleValue();
			polc.paint(position, direction, rangeBegin, rangeEnd, comboBoxMode.getSelectedIndex(), checkBoxSphereArc.isSelected(), drawer);
		}

		@Override
		public void valueChanged(GuiOpticalSurfaceObject object, SCENE_OBJECT_COLUMN_TYPE ct) {
			repaint();
		}

		@Override
		public void valueChanged(MeshObject object, SCENE_OBJECT_COLUMN_TYPE ct) {
			repaint();
		}

		@Override
		public void valueChanged(GuiOpticalVolumeObject object, SCENE_OBJECT_COLUMN_TYPE ct) {
			repaint();
		}
	}

}
