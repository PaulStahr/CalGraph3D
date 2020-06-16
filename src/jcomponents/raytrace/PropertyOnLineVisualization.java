package jcomponents.raytrace;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

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
import data.raytrace.Intersection;
import data.raytrace.MeshObject;
import data.raytrace.MeshObject.MeshObjectChangeListener;
import data.raytrace.OpticalObject;
import data.raytrace.OpticalObject.SCENE_OBJECT_COLUMN_TYPE;
import data.raytrace.OpticalSurfaceObject;
import data.raytrace.OpticalVolumeObject;
import data.raytrace.RaytraceScene;
import data.raytrace.RaytraceScene.RaySimulationObject;
import data.raytrace.raygen.RayGenerator;
import geometry.Geometry;
import geometry.Vector3d;
import io.Drawer;
import jcomponents.RecentFileList;
import jcomponents.util.JMathTextField;
import maths.Controller;
import maths.Operation;
import maths.algorithm.OperationCalculate;
import maths.data.ArrayOperation;
import util.ArrayUtil;
import util.JFrameUtils;
import util.StringUtils;
import util.data.DoubleArrayList;

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
	
	private void addTo(JMenu menu, JMenuItem item)
	{
		menu.add(item);
		item.addActionListener(this);
	}
	
	public PropertyOnLineVisualization(RaytraceScene scene)
	{
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
            if (type.equals("dat"))
        	{
            	try {
					StringUtils.writeTapSeperated(visualization.dataPoints, file, 2);
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
		private final Drawer.GraphicsDrawer drawer = new Drawer.GraphicsDrawer(null);
		private final Intersection intersection = new Intersection();
		private final DoubleArrayList dataPoints = new DoubleArrayList();
		RaytraceScene scene;
		private double rangeBegin, rangeEnd;
		
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
			Controller controll = new Controller();
			dataPoints.clear();
			drawer.setOutput(g);
			drawer.setColor(Color.BLACK);
			//ArrayList<IntersectionPoint> list = new ArrayList<>();
			ArrayList<OpticalObject> objects = new ArrayList<>();
			DoubleArrayList dal = new DoubleArrayList();
			OpticalObject nextSurfaces[] = scene.getActiveSurfaces();
			OpticalObject nextVolumes[] = scene.getActiveVolumes();
			OpticalObject nextMeshes[] = scene.getActiveMeshes();
			OperationCalculate.toDoubleArray(textFieldPosition.get().calculate(scene.vs, controll), position);
			OperationCalculate.toDoubleArray(textFieldDirection.get().calculate(scene.vs, controll), direction);
			Operation op = textFieldRange.get().calculate(scene.vs, controll);
			rangeBegin = op.get(0).doubleValue();
			rangeEnd = op.get(1).doubleValue();
			intersection.distance = 1e-10;
			switch (comboBoxMode.getSelectedIndex())
			{
				case 0:
				{
					while(true)
					{
						double lastDist = intersection.distance;
						intersection.distance = Double.POSITIVE_INFINITY;
						intersection.object = null;
						RaytraceScene.getNextIntersection(
									position,
									direction,
									intersection,
									lastDist + 1e-10,
									nextSurfaces,
									nextVolumes,
									nextMeshes);
						 dal.add(intersection.distance);
						 //dal.add(direction.dot(position, intersection.position));
						 OpticalObject object = intersection.object;
						 //System.out.print(object);
						 //System.out.print(" " + intersection.distance + " ");
						 if (object == null || objects.size() > 100)
						 {
							 break;
						 }
						 objects.add(intersection.object);
						 nextSurfaces = object.surfaceSuccessor;
						 nextVolumes = object.volumeSuccessor;
						 nextMeshes = object.meshSuccessor;
					}
					//System.out.println();
					
					for (int i = 0; i < objects.size(); ++i)
					{
						OpticalObject obj = objects.get(i);
						if (obj instanceof OpticalSurfaceObject)
						{
							OpticalSurfaceObject oso = (OpticalSurfaceObject)obj;
							//System.out.print("(" + (dal.getD(i) + 500) + "," + oso.ior0 + ")");
							double ior0, ior1;
							if (oso.direction.dot(direction) < 0)
							{
								ior0 = oso.ior0.doubleValue();
								ior1 = oso.ior1.doubleValue();
							}
							else
							{
								ior0 = oso.ior1.doubleValue();
								ior1 = oso.ior0.doubleValue();
							}
							dataPoints.add(dal.getD(i));
							dataPoints.add(ior0);
							dataPoints.add(dal.getD(i));
							dataPoints.add(ior1);
							ior0 = (2 - ior0) * getHeight() / 2;
							ior1 = (2 - ior1) * getHeight() / 2;
							drawer.pushPoint(dal.getD(i)/4 + 500, ior0);
							drawer.pushPoint(dal.getD(i)/4 + 500, ior1);
						}
						else if (obj instanceof OpticalVolumeObject)
						{
							OpticalVolumeObject ovo = (OpticalVolumeObject)obj;
							for (int j = 0; j < 100; ++j)
							{
								double mult = (j / 99.) * (dal.getD(i + 1) - dal.getD(i)) + dal.getD(i);
								double ior = (double)ovo.getRefractiveIndex(position.x + direction.x * mult, position.y + direction.y * mult, position.z + direction.z * mult) / 0x10000;
								dataPoints.add(mult);
								dataPoints.add(ior);
								drawer.pushPoint(mult / 4 + 500, (2 - ior) * getHeight() / 2);
							}
						}
					}
					try {
						drawer.drawPolyLine();
					} catch (IOException ex) {
						JFrameUtils.logErrorAndShow("Can't update Visualisation", ex, logger);
					}
				break;
				}
				case 1:
				{
					int num_rays = 1000;
					int num_evaluations = 200;
					ArrayList<OpticalObject> oso = new ArrayList<>();
					scene.getActiveLights(oso);
					RayGenerator gen = new RayGenerator();
					RaySimulationObject rso = new RaySimulationObject();
					RaySimulationData rsd = new RaySimulationData(num_rays, false);
					for (int i = 0; i < oso.size(); ++i)
					{
						gen.threeDimensional = true;
						gen.setSource(oso.get(i));
						scene.calculateRays(0, num_rays, num_rays, gen, 0, 0, null, null, rsd.endpoints, rsd.enddirs, rsd.endcolor, null, rsd.accepted, rsd.bounces, rsd.lastObject, 10, false, rso, RaytraceScene.UNACCEPTED_MARK);
						
						double distances[] = ArrayUtil.fillEquidistant(rangeBegin, rangeEnd, new double[num_evaluations]);
						
	
						double result[] = checkBoxSphereArc.isSelected() ? Geometry.getVarianceOnSphere(rsd.endpoints, rsd.enddirs, rsd.accepted, position, direction, distances, new double[num_evaluations])
								 : Geometry.getVariance(rsd.endpoints, rsd.enddirs, rsd.accepted, position, direction, distances, new double[num_evaluations]);
						ArrayUtil.sqrt(result, 0, result.length);
						double width = getWidth(), height = getHeight();
						double max = ArrayUtil.max(result);
						double multY = height / max;
						double multX = width / result.length;
						dataPoints.clear();
						for (int j = 0; j < result.length; ++j)
						{
							dataPoints.add(distances[j]);
							dataPoints.add(result[j]);
							drawer.pushPoint(j * multX, height - multY * result[j]);
						}
						try {
							drawer.drawPolyLine();
						} catch (IOException ex) {
							JFrameUtils.logErrorAndShow("Can't update Visualisation", ex, logger);
						}
						DataHandler.globalVariables.setGlobal("divergence", new ArrayOperation(result));
						System.out.println("result" + Arrays.toString(result));
					}
					break;
				}
			}
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
		
		/*private static class IntersectionPoint implements Comparable<IntersectionPoint>{
			public final double pos;
			public final OpticalObject obj;
			
			public IntersectionPoint(double pos, OpticalObject obj)
			{
				this.pos= pos;
				this.obj = obj;
			}
			@Override
			public int compareTo(IntersectionPoint o) {
				return Calculate.signum(this.pos - o.pos);
			}
		}*/
	}

}
