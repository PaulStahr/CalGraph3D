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
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.ContainerEvent;
import java.awt.event.ContainerListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultCellEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.GroupLayout;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;

import org.jdom2.JDOMException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import data.DataHandler;
import data.ObjectAttachmentContainer;
import data.Options;
import data.raytrace.DataChangeListener;
import data.raytrace.GuiOpticalSurfaceObject;
import data.raytrace.GuiOpticalVolumeObject;
import data.raytrace.GuiTextureObject;
import data.raytrace.MeshObject;
import data.raytrace.MeshObject.MeshObjectChangeListener;
import data.raytrace.OpticalObject;
import data.raytrace.OpticalObject.COLUMN_TYPES;
import data.raytrace.OpticalObject.SCENE_OBJECT_COLUMN_TYPE;
import data.raytrace.OpticalSurfaceObject;
import data.raytrace.OpticalVolumeObject;
import data.raytrace.ParseUtil;
import data.raytrace.RaySimulation.MaterialType;
import data.raytrace.RaySimulation.SurfaceType;
import data.raytrace.RaytraceScene;
import data.raytrace.RaytraceScene.RaySimulationObject;
import data.raytrace.RaytraceSession;
import data.raytrace.RaytraceSession.CommandExecutionListener;
import data.raytrace.SurfaceObject;
import data.raytrace.TextureMapping;
import data.raytrace.raygen.ImageRayGenerator;
import data.raytrace.raygen.RayGenerator;
import geometry.Geometry.NearestPointCalculator;
import geometry.Vector2d;
import geometry.Vector3d;
import io.Drawer;
import io.Drawer.GraphicsDrawer;
import io.Drawer.SvgDrawer;
import io.ObjectExporter;
import io.raytrace.SceneIO;
import jcomponents.Interface;
import jcomponents.InterfaceOptions;
import jcomponents.LicenseMenu;
import jcomponents.RecentFileList;
import jcomponents.panels.CodePadPanel;
import jcomponents.panels.SliderPanel;
import jcomponents.panels.VariablePanel;
import jcomponents.util.ButtonColumn;
import jcomponents.util.JFileChooserRecentFiles;
import jcomponents.util.JMathTextField;
import jcomponents.util.StandartFCFileFilter;
import maths.Controller;
import maths.Operation;
import maths.algorithm.Calculate;
import maths.algorithm.DoubleMatrixUtil;
import maths.data.ArrayOperation;
import maths.exception.OperationParseException;
import opengl.Camera;
import opengl.jogamp.JoglCanvas;
import scene.OpenGlKeyHandler;
import scene.OpenGlMouseHandler;
import scene.Scene;
import scene.SimpleCameraListener;
import scene.object.SceneObject;
import scene.object.SceneObject.DrawType;
import scene.object.SceneObject.UpdateKind;
import scene.object.SceneObjectLine;
import scene.object.SceneObjectMesh;
import util.ArrayUtil;
import util.JFrameUtils;
import util.RunnableRunner;
import util.RunnableRunner.ParallelRangeRunnable;
import util.StringUtils;
import util.TimedUpdateHandler;
import util.data.UniqueObjects;

/** 
* @author  Paul Stahr
* @version 04.02.2012
*/
public class RaySimulationGui extends JFrame implements GuiTextureObject.TextureObjectChangeListener, GuiOpticalVolumeObject.OpticalVolumeObjectChangeListener, GuiOpticalSurfaceObject.OpticalSurfaceObjectChangeListener, MeshObjectChangeListener, ActionListener, ItemListener, RaytraceScene.SceneChangeListener, TableModelListener, ListSelectionListener, WindowListener, DataChangeListener, ContainerListener, CommandExecutionListener
{
	private static final long serialVersionUID = -8104574201311985756L;
	private static final Logger logger = LoggerFactory.getLogger(RaySimulationGui.class);
	private static final int glObjectAttachementId = ObjectAttachmentContainer.getId();  
	
	private static final class TableModel extends DefaultTableModel
	{
		/**
		 * 
		 */
		private static final long serialVersionUID = -3379232940335572529L;
		private final COLUMN_TYPES types;
		
		public TableModel(COLUMN_TYPES types)
		{
			super(new Object[1][types.visibleColsSize()], types.getVisibleColumnNames());
			this.types = types;
		}
		
		@Override
        public final Class<?> getColumnClass(int columnIndex) {
            return types.getVisibleCol(columnIndex).cl;
        }
	}
	
	
	private final DefaultTableModel tableModelSurfaces = new TableModel(GuiOpticalSurfaceObject.TYPES);
	private final DefaultTableModel tableModelVolumes = new TableModel(GuiOpticalVolumeObject.TYPES);
	private final DefaultTableModel tableModelTextures= new TableModel(GuiTextureObject.TYPES);	
	private final DefaultTableModel tableModelMeshes= new TableModel(MeshObject.TYPES);	
	public final JTable tableSurfaces = new JTable(tableModelSurfaces);
	public final JTable tableVolumes = new JTable(tableModelVolumes);
	public final JTable tableTextures = new JTable(tableModelTextures);
	public final JTable tableMeshes = new JTable(tableModelMeshes);
	private final JScrollPane scrollPaneSurfaces = new JScrollPane(tableSurfaces);
    private final JScrollPane scrollPaneVolumes = new JScrollPane(tableVolumes);
	private final JScrollPane scrollPaneTextures = new JScrollPane(tableTextures);
	private final JScrollPane scrollPaneMeshes = new JScrollPane(tableMeshes);
	public final JTextArea textAreaProjectInformation = new JTextArea();
    int maxBounces = 20;
    public final VisualizationPanel panelVisualization	= new VisualizationPanel();
    public final RaytraceScene scene = new RaytraceScene("Unnamed");
    private final JMenu menuFile = new JMenu("File");
    private final JMenu menuEdit = new JMenu("Edit");
    private final JMenu menuAdd = new JMenu("Add");
    private final JMenu menuTools = new JMenu("Tools");
    private final JMenu menuItemExtras = new JMenu("Extras");
    private final JMenuItem menuItemNew = new JMenuItem("New");
    private final JMenuItem menuItemClear = new JMenuItem("Clear");
    private final JMenuItem menuItemOpen = new JMenuItem("Open");
    private final JMenuItem menuItemAppend = new JMenuItem("Append");
    private final JMenuItem menuItemSave = new JMenuItem("Save");
    private final JMenuItem menuItemSaveTo = new JMenuItem("Save to");
    private final JMenuItem menuItemExportTo = new JMenuItem("Export to");
    private final JMenuItem menuItemSpeedTest = new JMenuItem("SpeedTest");
    private final JMenuItem menuItemCopy = new JMenuItem("Copy");
    private final JMenuItem menuItemCut = new JMenuItem("Cut");
    private final JMenuItem menuItemPaste = new JMenuItem("Paste");
    private final JMenuItem menuItemAddSurface = new JMenuItem("Surface");
    private final JMenuItem menuItemAddTexture = new JMenuItem("Texture");
    private final JMenuItem menuItemAddVolume = new JMenuItem("Volume");
    private final JMenuItem menuItemAddMesh = new JMenuItem("Mesh");
    private final JMenuItem menuItemAddSlider = new JMenuItem("Slider");
    private final JMenuItem menuItemAddCodePad = new JMenuItem("Codepad");
    private final JMenuItem menuItemAddVariablePanel = new JMenuItem("VariablePanel");
    private final JMenuItem menuItemStackPositionProcessor = new JMenuItem("Stack Position Processor");
    private final JMenuItem menuItemCalculateFocuseHeatMap = new JMenuItem("Focus Heat Map");
    private final JMenuItem menuItemParameterAnalysis = new JMenuItem("Error Analysis");
    private final JMenuItem menuItemRefractiveIndexVisualisation = new JMenuItem("Refractive Index Visualisation");
    private final JMenuItem menuItemOptions = new JMenuItem("Options");
    private final JMenuItem menuItemCalGraph = new JMenuItem("CalGraph");
    private final JMenuItem menuItemTakeScreenshot = new JMenuItem("Take Screenshot");
    private final JMenuItem menuItemVolumePipeline = new JMenuItem("Volume Pipeline");
    private final JMenuItem menuItemPointCloudVisualisation = new JMenuItem("Point Cloud Visualization");
    private final JMenuItem menuItemDivergenceVisualisation = new JMenuItem("Divergence Visualization");
    private final JCheckBoxMenuItem menuItemRecordMacro = new JCheckBoxMenuItem("Record Macro");
    private final JMenuItem menuItemRunMacro = new JMenuItem("Run Macro");
    private final LicenseMenu licenseMenu		= new LicenseMenu("Lizenzen");
	public final JPanel panelTools = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 5));
	private final SceneObjectLine sceneObjectTrajectories = new SceneObjectLine();
	private final SceneObjectLine sceneObjectEndpoints = new SceneObjectLine();
	private final JLabel labelAcceptedFraction = new JLabel();
    private static final FileFilter xmlFileFilter = new StandartFCFileFilter("XML-Markup file", "xml", true);
    private final Vector2d paintOffset = new Vector2d();
    private final JToggleButton toggleButtonTwoD = new JToggleButton("2D");
    private final JToggleButton toggleButtonThreeD = new JToggleButton("3D");
    private final JToggleButton toggleButtonRaytrace = new JToggleButton("Raytraced");
    private final JToggleButton toggleButtonTraceRays = new JToggleButton("Trace Rays");
    private final ParseUtil parser = new ParseUtil();
    private final GuiTextureObject previewTexture = new GuiTextureObject(scene.vs, parser);
    private final JTabbedPane tapPane = new JTabbedPane();
    private volatile boolean isUpdating = false;
    private boolean advancedView = false;
	public final Scene glScene = Interface.scene;
	public File currentSceneFile;
	private static boolean drawAnchorPoints = true, drawDirectionVector = true, drawFocalpoints = true, drawMeasure = true;
	private static int optionModCount = 0;
	private int oldOptionModCount = 0;
	private static float dScale = 1;
	private final RaytraceSession session = new RaytraceSession();
	static{
		Options.addModificationListener(new Runnable() {
			
			@Override
			public void run() {
				optionModCount = Options.modCount();
				Options.OptionTreeInnerNode raytrace = Options.getInnerNode("raytrace");
				Options.OptionTreeInnerNode visible = Options.getInnerNode(raytrace, "visible");
				drawAnchorPoints = Options.getBoolean(visible, "anchor");
				drawDirectionVector = Options.getBoolean(visible, "direction");
				drawFocalpoints = Options.getBoolean(visible, "focalpoint");
				drawMeasure = Options.getBoolean(visible, "measure");
				dScale = Options.getFloat(raytrace, "dscale");
				int invisibleAlpha = Options.getInteger(raytrace, "invisible_alpha");
				LENSE_INVISIBLE = new Color(0,0,0,invisibleAlpha);
				EMISSION_INVISIBLE = new Color(0,0,0, invisibleAlpha);
				int rayAlpha = Options.getInteger(raytrace, "ray_alpha");
				RAY_RED = new Color(255,0,0,rayAlpha);
				RAY_BLACK = new Color(0,0,0,rayAlpha);				
			}
		});
	}
	
	public final TimedUpdateHandler rayUpdateHandler = new TimedUpdateHandler() {
    	
		private final Vector3d tmp = new Vector3d();
		
		@Override
		public final synchronized void update() {
			if (toggleButtonTraceRays.isSelected())
			{
				DataHandler.runnableRunner.run(untracedRayRunnable, false);
			}
			if (optionModCount != oldOptionModCount)
			{
				oldOptionModCount = optionModCount;
				panelVisualization.repaint();
				if (panelVisualization != currentVisualization)
				{
					currentVisualization.repaint();
				}
			}
			if (currentVisualization instanceof RaytraceVisualization)
			{
				RaytraceVisualization rv = (RaytraceVisualization)currentVisualization;
				rv.cameraListener.run();
				ImageRayGenerator gen = rv.cvr.gen;
				tmp.set(glScene.cameraPosition);
				tmp.multiply(1 / dScale);
				if (!(gen.position.equals(tmp) && glScene.cameraRotation.equals(gen.rotation)))
				{
					rv.repaint();
				}
			}
		}
		
		
		@Override
		public final int getUpdateInterval() {
			return 10;
		}
	};
	
	public static final String env = "Env";
	public static final String no = "No";
	public void load(JComboBox<Object> box, Object o) {
		if (o instanceof OpticalObject)
		{
			box.setSelectedItem(o);
		}
		else if (o == scene)
		{
			box.setSelectedItem(env);
		}
		else if (o == null)
		{
			box.setSelectedItem(no);
		}
	}
	private final ArrayList<Object> al = new ArrayList<>();
	
	private void updateBoundComboBoxes()
	{
		al.add(no);
		al.add(env);
		al.addAll(scene.surfaceObjectList);
		al.addAll(scene.volumeObjectList);
		Object data[] = al.toArray(new Object[al.size()]);
		al.clear();
		updateBox(scenePanel.forceEndpoint, data);
		updateBox(scenePanel.forceStartpoint, data);
	}
    
	private static void updateBox(JComboBox<Object> comboBox, Object data[])
	{
		Object selected = comboBox.getSelectedItem();
		comboBox.setModel(new DefaultComboBoxModel<Object>(data));
		comboBox.setSelectedItem(selected);

	}
	
	private void updateTextureComboBoxes()
	{
		Object textures[] = scene.textureObjectList.toArray(new Object[scene.textureObjectList.size()]);
		updateBox(scenePanel.comboBoxWritableEnvironment, textures);
		updateBox(scenePanel.comboBoxRenderToTexture, textures);
	}
	
	private final <E extends OpticalObject>void setTableValues(SCENE_OBJECT_COLUMN_TYPE ct, ArrayList<E> list, E current, JTable table)
	{
		try
		{
			COLUMN_TYPES types = current.getTypes();
			int colBegin = ct == null ? 0 : types.getVisibleColumnNumber(ct);
			int colEnd = ct == null ? types.visibleColsSize() : (colBegin + 1);
			if (colBegin >= 0)
			{
				int rowBegin = current == null ? 0 : list.indexOf(current);
				int rowEnd = current == null ? list.size() : (rowBegin + 1);
				for (int j = colBegin; j < colEnd; ++j)
				{
					SCENE_OBJECT_COLUMN_TYPE soc = types.getVisibleCol(j);
					for (int i = rowBegin; i < rowEnd; ++i)
					{
						table.setValueAt(list.get(i).getValue(soc), i, j);
					}
	    		}
			}
		}
		catch(Exception e)
		{
			logger.error("Error setting values", e);
		}
	}
	
	@Override
	public void valueChanged(final GuiOpticalSurfaceObject object, final SCENE_OBJECT_COLUMN_TYPE ct) {
		DataHandler.runnableRunner.run(new Runnable() {
			@Override
			public void run() {
				int rowBegin = object == null ? 0 : scene.surfaceObjectList.indexOf(object);
				int rowEnd = object == null ? scene.surfaceObjectList.size() : (rowBegin + 1);
				for (int i = rowBegin; i < rowEnd; ++i)
				{
					GuiOpticalSurfaceObject current = scene.surfaceObjectList.get(i);
					SceneObjectMesh currentMesh = (SceneObjectMesh)current.attachements.get(glObjectAttachementId);
					float vertices[] = currentMesh.getVertices();
					vertices = ArrayUtil.setToLength(vertices, current.getMeshVertexCount(16, 8) * 3);
					current.getMeshVertices(16, 8, vertices);
					int faces[] = current.getMeshFaces(16, 8, currentMesh.getFaces());
					DoubleMatrixUtil.multiply(vertices, dScale, 0, vertices.length);
					currentMesh.setData(vertices, faces);
					currentMesh.lightMaterial.set(true, true, true, false, current.color);
					currentMesh.reflectionMaterial.set(false, false, false, true, current.color);
					currentMesh.setVisible(current.active);
					currentMesh.update(UpdateKind.DATA);
				}	
			}
		}, "Update Mesh");
		updateTableValues(ct, scene.surfaceObjectList, object, tableSurfaces);
		panelVisualization.repaint();
		if (panelVisualization != currentVisualization)
		{
			currentVisualization.repaint();
		}
		DataHandler.runnableRunner.run(untracedRayRunnable, false);
		
	}
	    
	@Override
	public void valueChanged(final GuiOpticalVolumeObject object,final SCENE_OBJECT_COLUMN_TYPE ct) {
		DataHandler.runnableRunner.run(new Runnable() {
			@Override
			public void run() {
				final int rowBegin = object == null ? 0 : scene.volumeObjectList.indexOf(object);
				final int rowEnd = object == null ? scene.volumeObjectList.size() : (rowBegin + 1);
				for (int i = rowBegin; i < rowEnd; ++i)
				{
					dataChanged(scene.volumeObjectList.get(i));
				}
			}
		}, "Update Mesh");
		updateTableValues(ct, scene.volumeObjectList, object, tableVolumes);		
		repaintVisualizations();
	}

	private void repaintVisualizations()
	{
		panelVisualization.repaint();
		if (panelVisualization != currentVisualization)
		{
			currentVisualization.repaint();
		}
		DataHandler.runnableRunner.run(untracedRayRunnable, false);
	}
	
	private final <E extends OpticalObject>void updateTableValues(final SCENE_OBJECT_COLUMN_TYPE ct, final ArrayList<E> list, final E object, final JTable table)
	{
		if (isUpdating && SwingUtilities.isEventDispatchThread())
		{
			return;
		}
		EventQueue.invokeLater(new Runnable() {
			@Override
			public void run()
			{
				isUpdating = true;
				setTableValues(ct, list, object, table);
				isUpdating = false;
			}
		});
	}
	
	@Override
	public void valueChanged(final GuiTextureObject object, final SCENE_OBJECT_COLUMN_TYPE ct) {
		updateTableValues(ct, scene.textureObjectList, object, tableTextures);
		repaintVisualizations();
	}
	
	@Override
	public void valueChanged(final MeshObject object, SCENE_OBJECT_COLUMN_TYPE ct) 
	{
		updateTableValues(ct, scene.meshObjectList, object, tableMeshes);
		
		DataHandler.runnableRunner.run(new Runnable() {
			@Override
			public void run() {
			int rowBegin = object == null ? 0 : scene.meshObjectList.indexOf(object);
			int rowEnd = object == null ? scene.meshObjectList.size() : (rowBegin + 1);
			for (int i = rowBegin; i < rowEnd; ++i)
			{
				MeshObject current = scene.meshObjectList.get(i);
				SceneObjectMesh currentMesh = (SceneObjectMesh)current.attachements.get(glObjectAttachementId);
				if (currentMesh != null)
				{
		    		currentMesh.lightMaterial.set(true, true, true, false, current.color);
		    		currentMesh.reflectionMaterial.set(false, false, false, true, current.color);
		    		dataChanged(current);
		    		
				}
			}
		}}, "Update Mesh");
		repaintVisualizations();
	}

    private final AbstractAction tableAction = new AbstractAction() {
    	private static final long serialVersionUID = 3980835476835695337L;
			@Override
			public void actionPerformed(ActionEvent e)
 	    {
			ButtonColumn.TableButtonActionEvent event = (ButtonColumn.TableButtonActionEvent)e;
			Object tableSource = event.getSource();
			int col = event.getCol();
			int row = event.getRow();
			if (tableSource == tableModelSurfaces)
			{
				if (GuiOpticalSurfaceObject.TYPES.getVisibleCol(col) == SCENE_OBJECT_COLUMN_TYPE.DELETE)
				{
					GuiOpticalSurfaceObject toRemove = scene.surfaceObjectList.get(row);
		 	        scene.remove(toRemove);
		 	    }
			}
			else if (tableSource == tableModelVolumes)
			{
				SCENE_OBJECT_COLUMN_TYPE type = GuiOpticalVolumeObject.TYPES.getVisibleCol(col);
				if (type == SCENE_OBJECT_COLUMN_TYPE.LOAD)
				{
		 	        JFileChooser fileChooser= new JFileChooserRecentFiles();
		 	        if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION)
		 	        {
		 	        	String path = fileChooser.getSelectedFile().getAbsolutePath();
		 	        	GuiOpticalVolumeObject volume = scene.volumeObjectList.get(row);
		 	        	String fileType = StringUtils.getFileType(path);
		 	        	if (fileType.equals("dcm"))
		 	        	{
		 	        		volume.readDycom(path);
		 	        	}
		 	        	else if (fileType.equals("avi"))
		 	        	{
		 	        		volume.readAvi(path);
		 	        	}
		 	        	else
		 	        	{
		 	        		try {
								volume.readBinaryFile(path);
							} catch (IOException e1) {
								logger.error("Can't read binary file " + path, e);
							}
		 	        	}
		 	        }
				}
				else if (type == SCENE_OBJECT_COLUMN_TYPE.VIEW)
				{
		 	        scene.volumeObjectList.get(row).view();
				}
				else if (type == SCENE_OBJECT_COLUMN_TYPE.DELETE)
				{
		 	        scene.remove(scene.volumeObjectList.get(row));
		 	    }
			}
			else if (tableSource == tableModelTextures)
			{
				SCENE_OBJECT_COLUMN_TYPE type = GuiTextureObject.TYPES.getVisibleCol(col);
				if (type == SCENE_OBJECT_COLUMN_TYPE.DELETE)
				{
		 	        scene.remove(scene.textureObjectList.get(row));
		 	    }
				else if (type == SCENE_OBJECT_COLUMN_TYPE.OPEN)
				{
					GuiTextureObject current = scene.textureObjectList.get(row);
					
					JFileChooser fileChooser= new JFileChooserRecentFiles(current.getFile());
					if(fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION)
		            {
						try {
							current.setValue(SCENE_OBJECT_COLUMN_TYPE.PATH, fileChooser.getSelectedFile().getAbsolutePath(), scene.vs, parser);
						} catch (OperationParseException e1) {
							logger.error("Can't set filepath");
						}
						if (current.image == null)
						{
							try {
								current.load(scene.vs, parser);
							} catch (IOException | OperationParseException ex) {
								logger.error("Can't load image " + current.getFile(), ex);
							}
						}
		            }
				}else if (type == SCENE_OBJECT_COLUMN_TYPE.LOAD)
				{
					try {
						scene.textureObjectList.get(row).load(scene.vs, parser);
					} catch (IOException | OperationParseException e1) {
						logger.error("can't load texture", e);
						
					}
				}else if (type == SCENE_OBJECT_COLUMN_TYPE.SAVE)
				{
					try {
						scene.textureObjectList.get(row).save();
					} catch (IOException e1) {
						logger.error("Can't save texture", e);
					}
				}else if (type == SCENE_OBJECT_COLUMN_TYPE.SAVE_TO)
				{
					GuiTextureObject current = scene.textureObjectList.get(row);
					
					JFileChooser fileChooser= new JFileChooserRecentFiles(current.getFile().getAbsolutePath());
					if(fileChooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION)
		            {
						if (current.image != null)
						{
							try {
								current.saveTo(fileChooser.getSelectedFile());
							} catch (IOException ex) {
								JFrameUtils.logErrorAndShow("Can't save texture", ex, logger);
							}
						}
						try {
							current.setValue(SCENE_OBJECT_COLUMN_TYPE.PATH, fileChooser.getSelectedFile(), scene.vs, parser);
						} catch (OperationParseException ex) {
							JFrameUtils.logErrorAndShow("Unexpected parse exception", ex, logger);
						}
		            }
				}else if (type == SCENE_OBJECT_COLUMN_TYPE.VIEW)
				{
					final GuiTextureObject current = scene.textureObjectList.get(row);
					TextureView tv = new TextureView(current.image);
					final DataChangeListener tcl= new ImageComponent.AbstractTextureChangeListener(tv.getImageComponent()) {
						
						@Override
						public void dataChanged(ImageComponent comp) {
							comp.setImage(current.image);
						}

					};
					current.addDataChangeListener(tcl);
					tv.addWindowListener(new WindowAdapter() {
					    @Override
					    public void windowClosed(java.awt.event.WindowEvent windowEvent) {
					       	current.removeDataChangeListener(tcl);
					    }
					});
					tv.setVisible(true);
				}
			}
			else if (tableSource == tableModelMeshes)
			{
				if (MeshObject.TYPES.getVisibleCol(col) == SCENE_OBJECT_COLUMN_TYPE.DELETE)
				{
		 	        scene.remove(scene.meshObjectList.get(row));
		 	    }
				
			}
 	    }
    };
    
    private void visibleErrorMessage(String text, Exception e)
    {
    	JFrameUtils.logErrorAndShow(text, e, logger);
    }
    
 	private final ButtonColumn surfaceDeleteColumn 	= new ButtonColumn(tableSurfaces,tableAction, 	GuiOpticalSurfaceObject.TYPES.getVisibleColumnNumber(SCENE_OBJECT_COLUMN_TYPE.DELETE));
 	private final ButtonColumn volumeDeleteColumn 	= new ButtonColumn(tableVolumes,tableAction, 	GuiOpticalVolumeObject.TYPES.getVisibleColumnNumber(SCENE_OBJECT_COLUMN_TYPE.DELETE));
 	private final ButtonColumn volumeLoadColumn 	= new ButtonColumn(tableVolumes,tableAction, 	GuiOpticalVolumeObject.TYPES.getVisibleColumnNumber(SCENE_OBJECT_COLUMN_TYPE.LOAD));
 	private final ButtonColumn volumeViewColumn 	= new ButtonColumn(tableVolumes,tableAction, 	GuiOpticalVolumeObject.TYPES.getVisibleColumnNumber(SCENE_OBJECT_COLUMN_TYPE.VIEW));
 	private final ButtonColumn texturOpenColumn	 	= new ButtonColumn(tableTextures,tableAction,	GuiTextureObject.TYPES.getVisibleColumnNumber(SCENE_OBJECT_COLUMN_TYPE.OPEN));
 	private final ButtonColumn texturLoadColumn 	= new ButtonColumn(tableTextures,tableAction,	GuiTextureObject.TYPES.getVisibleColumnNumber(SCENE_OBJECT_COLUMN_TYPE.LOAD));
 	private final ButtonColumn texturSaveColumn 	= new ButtonColumn(tableTextures,tableAction,	GuiTextureObject.TYPES.getVisibleColumnNumber(SCENE_OBJECT_COLUMN_TYPE.SAVE));
 	private final ButtonColumn texturSaveToColumn 	= new ButtonColumn(tableTextures,tableAction,	GuiTextureObject.TYPES.getVisibleColumnNumber(SCENE_OBJECT_COLUMN_TYPE.SAVE_TO));
 	private final ButtonColumn texturViewColumn 	= new ButtonColumn(tableTextures,tableAction,	 GuiTextureObject.TYPES.getVisibleColumnNumber(SCENE_OBJECT_COLUMN_TYPE.VIEW));
 	private final ButtonColumn texturDeleteColumn 	= new ButtonColumn(tableTextures,tableAction, 	GuiTextureObject.TYPES.getVisibleColumnNumber(SCENE_OBJECT_COLUMN_TYPE.DELETE));
 	private final ButtonColumn meshDeleteColumn 	= new ButtonColumn(tableMeshes,tableAction, 	MeshObject.TYPES.getVisibleColumnNumber(SCENE_OBJECT_COLUMN_TYPE.DELETE));
 	private final ButtonColumn meshOpenColumn 		= new ButtonColumn(tableMeshes,tableAction, 	MeshObject.TYPES.getVisibleColumnNumber(SCENE_OBJECT_COLUMN_TYPE.OPEN));
 	private final ButtonColumn meshSaveColumn 		= new ButtonColumn(tableMeshes,tableAction, 	MeshObject.TYPES.getVisibleColumnNumber(SCENE_OBJECT_COLUMN_TYPE.SAVE_TO));
	public VolumePipelines volumePipelines;
	private final JPanel selectedObjectPanel = new JPanel();
	private final GroupLayout layout;

    @Override
	public void actionPerformed(ActionEvent e) {
		Object source = e.getSource();
		if (source == menuItemAddSurface)
		{
			GuiOpticalSurfaceObject goo = new GuiOpticalSurfaceObject(scene.vs, parser);
			goo.id = new StringBuilder().append('S').append(scene.surfaceObjectList.size()).toString();
			scene.add(goo);
		}
		else if (source == menuItemAddVolume)
		{
			GuiOpticalVolumeObject ovo = new GuiOpticalVolumeObject(scene.vs, parser);
			JFileChooser fileChooser= new JFileChooserRecentFiles();
			//fileChooser.setCurrentDirectory(new File(buttonEnvironment.getText()));
			if(fileChooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION)
			{
				String filePath = fileChooser.getSelectedFile().getAbsolutePath();
				if (filePath.endsWith("dcm"))
 	        	{
					ovo.readDycom(filePath);
 	        	}
 	        	else
 	        	{
 	        		try {
 	        			ovo.readBinaryFile(filePath);
					} catch (IOException e1) {
						logger.error("Can't read binary file " + filePath, e);
					}
 	        	}
			}
			ovo.id = new StringBuilder().append('V').append(scene.volumeObjectList.size()).toString();
			scene.add(ovo);
		}
		else if (source == menuItemAddMesh)
		{
			MeshObject mesh = new MeshObject(scene.vs, parser);
			JFileChooser fileChooser = new JFileChooserRecentFiles();
			if(fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION)
			{
				String filepath = fileChooser.getSelectedFile().toString();
				try {
					mesh.load(filepath);
				} catch (Exception e1) {
					visibleErrorMessage("Can't read mesh", e1);
					return;
				} 
			}
			mesh.id = new StringBuilder().append('M').append(scene.meshObjectList.size()).toString();
			scene.add(mesh);
		}
		else if (source == menuItemAddTexture)
		{
			GuiTextureObject to = new GuiTextureObject(scene.vs, parser);
			JFileChooser fileChooser= new JFileChooserRecentFiles();
			if(fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION)
			{
				try {
					to.setValue(SCENE_OBJECT_COLUMN_TYPE.PATH, fileChooser.getSelectedFile().getAbsolutePath(), scene.vs, parser);
				} catch (OperationParseException ex) {
			    	JFrameUtils.logErrorAndShow("Can't read image", ex, logger);
				}
				try {
					to.load(scene.vs, parser);
				} catch (IOException | OperationParseException e1) {
					visibleErrorMessage("Can't read image", e1);
				}
			}
			to.id = new StringBuilder().append('T').append(scene.textureObjectList.size()).toString();
			scene.add(to);
		}
		else if (source == menuItemAddSlider)
		{
			panelTools.add(new SliderPanel(scene.vs));
		}
		else if (source == menuItemAddCodePad)
		{
			panelTools.add(new CodePadPanel(scene.vs));
		}
		else if (source == menuItemAddVariablePanel)
		{
			panelTools.add(new VariablePanel(scene.vs));
		}
		else if (source == menuItemClear)
		{
			clear();
		}
		else if (source == menuItemNew)
		{
			new RaySimulationGui().setVisible(true);
		}
		else if (source == menuItemAppend || source == menuItemOpen)
		{
			try
			{
				JFileChooser fileChooser= new JFileChooserRecentFiles();
	            Options.OptionTreeInnerNode raytrace = Options.getInnerNode("raytrace");
	            String lastDirectory = Options.getString(raytrace, "lastdir", null);
	            if (lastDirectory != null)
	            {
	            	fileChooser.setCurrentDirectory(new File(lastDirectory));
	            }
	            if(fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION)
	            {
	             	File file = fileChooser.getSelectedFile();
	             	if (!file.exists())
	             	{
	             		JOptionPane.showMessageDialog(this, "File not found");
	             		return;
	             	}
	             	String fileType = StringUtils.getFileType(file.getName());
	             	String imageFormatNames[] = ImageIO.getReaderFormatNames();
	             	Arrays.sort(imageFormatNames);
	             	if (Arrays.binarySearch(imageFormatNames, fileType) >= 0)
             		{
	             		new TextureView(ImageIO.read(file)).setVisible(true);
             		}
	             	else if (fileType.equals("xml"))
	             	{
		             	FileInputStream in = new FileInputStream(file);
		              	if (source == menuItemOpen)
		             	{
		             		 clear();
		             	}
		                SceneIO.loadScene(in, scene, this);
		                in.close();
		                String path = file.getAbsolutePath();
		                File dir = new File(path.substring(0, path.lastIndexOf('.')));
		                if (dir.exists())
		                {
							for (int i = 0; i < scene.textureObjectList.size(); ++i)
							{
		                 		GuiTextureObject current = scene.textureObjectList.get(i);
		                 		File imageFile = new File(dir.getAbsolutePath() + '/' + current.id + '.' + "png");
		                 		if (imageFile.exists())
		                 		{
		                 			current.load(imageFile, scene.vs, parser);
		                 		}
		                 	}
							for (int i = 0; i < scene.volumeObjectList.size(); ++i)
							{
								GuiOpticalVolumeObject current = scene.volumeObjectList.get(i);
								File volumeFile = new File(dir.getAbsolutePath() + '/' + current.id + '.' + "blob");
								if (volumeFile.exists())
								{
									current.readBinaryFile(volumeFile.getPath());
								}
							}
							for (int i = 0; i < scene.meshObjectList.size(); ++i)
							{
								MeshObject current = scene.meshObjectList.get(i);
								File meshFile = new File(dir.getAbsolutePath() + '/' + current.id + '.' + "obj");
								if (meshFile.exists())
								{
									current.load(meshFile.getPath());
								}
							}
		                }
		                if (source == menuItemOpen)
		                {
		                	currentSceneFile = file;
		                }
		            }
		            Options.set(raytrace, "lastdir", fileChooser.getCurrentDirectory().toString());
		            Options.triggerUpdates();
	            }
			}catch (Exception ex)
			{
				visibleErrorMessage("Can't load file", ex);
			}
		}
		else if (source == menuItemSave || source == menuItemSaveTo)
		{
			File file = currentSceneFile;
			if (source == menuItemSaveTo || file == null)
			{
				JFileChooser fileChooser= new JFileChooser();
                JPanel panel = new JPanel();
                panel.add(new RecentFileList(fileChooser, DataHandler.getRecentFiles()));
                JCheckBox checkBox = new JCheckBox("Export Data Objects"); 
                panel.add(checkBox);
                panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
                fileChooser.setAccessory(panel);
             	Options.OptionTreeInnerNode raytrace = Options.getInnerNode("raytrace");
                String lastDirectory = Options.getString(raytrace, "lastdir", null);
                if (lastDirectory != null)
                {
                	fileChooser.setCurrentDirectory(new File(lastDirectory));
                }
                fileChooser.setFileFilter(xmlFileFilter);
                if (fileChooser.showSaveDialog(null) != JFileChooser.APPROVE_OPTION)
                {
                	return;
                }
                file = fileChooser.getSelectedFile();
                DataHandler.addRecentFile(file.getAbsolutePath());
             	if (checkBox.isSelected())
         		{
             		String path = file.getAbsolutePath();
             		File dir = new File(path.substring(0, path.lastIndexOf('.'))); 
             		dir.mkdir();
             		for (int i = 0; i < scene.textureObjectList.size(); ++i)
             		{
             			GuiTextureObject current = scene.textureObjectList.get(i); 
             			try {
							current.saveTo(new File(dir.getAbsolutePath() + '/' + current.id + '.' + "png"));
						} catch (IOException e1) {
							visibleErrorMessage("Can't save image file", e1);
						}
             		}
             		for (int i = 0; i < scene.meshObjectList.size(); ++i)
             		{
             			MeshObject current = scene.meshObjectList.get(i);
             			try {
             				current.saveTo(new File(dir.getAbsolutePath() + '/' + current.id + '.' + "obj"));
             			} catch (IOException e1) {
							visibleErrorMessage("Can't save mesh file", e1);
						}
             		}
             		for (int i = 0; i < scene.volumeObjectList.size(); ++i)
             		{
             			GuiOpticalVolumeObject current = scene.volumeObjectList.get(i);
             			try {
             				current.writeBinaryFile(new File(dir.getAbsolutePath() + '/' + current.id + '.' + "blob"));
             			} catch (IOException e1) {
							visibleErrorMessage("Can't save mesh file", e1);
						}
             		}
         		}
         		Options.set(raytrace, "lastdir", fileChooser.getCurrentDirectory().toString());
         		Options.triggerUpdates();
			}
			try
			{
				FileOutputStream out = new FileOutputStream(file);
				SceneIO.saveScene(out, false, scene, this);
				out.close();
				currentSceneFile = file;
			}catch (Exception ex)
			{
				visibleErrorMessage("Can't save to file", ex);
			}
		}
		else if (source == menuItemExportTo)
		{
			JFileChooser fileChooser= new JFileChooser();
            JPanel panel = new JPanel();
            panel.add(new RecentFileList(fileChooser, DataHandler.getRecentFiles()));
            JCheckBox checkBox = new JCheckBox("Export Disabled Objects"); 
            panel.add(checkBox);
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
            fileChooser.setAccessory(panel);
         	Options.OptionTreeInnerNode raytrace = Options.getInnerNode("raytrace");
            String lastDirectory = Options.getString(raytrace, "lastdir", null);
            if (lastDirectory != null)
            {
            	fileChooser.setCurrentDirectory(new File(lastDirectory));
            }
            fileChooser.setFileFilter(xmlFileFilter);
            if (fileChooser.showSaveDialog(null) != JFileChooser.APPROVE_OPTION)
            {
            	return;
            }
            File file = fileChooser.getSelectedFile();
            DataHandler.addRecentFile(file.getAbsolutePath());
            boolean exportInvisible = checkBox.isSelected();
     		String path = file.getAbsolutePath();
     		File dir = new File(path.substring(0, path.lastIndexOf('.'))); 
     		dir.mkdir();
     		for (int i = 0; i < glScene.getObjectCount(); ++i)
     		{
     			SceneObject obj = glScene.getObject(i);
     			if (exportInvisible || obj.isVisible())
     			{
         			try {
						ObjectExporter.export(ObjectExporter.OBJ, dir.getAbsolutePath() + '/' + i + '.' + "obj", obj);
					} catch (IOException e1) {
						visibleErrorMessage("Can't export object", e1);
					}
     			}
     		}
     		Options.set(raytrace, "lastdir", fileChooser.getCurrentDirectory().toString());
     		Options.triggerUpdates();
		}
		else if (source == menuItemStackPositionProcessor)
		{
			new StackPositionProcessorWindow(scene, session).setVisible(true);
		}
		else if (source == menuItemParameterAnalysis)
		{
			try {
				new ErrorAnalysisFrame(scene).setVisible(true);
					
			}catch (Exception ex)
			{
				ex.printStackTrace();
				throw ex;
			}
		}
		else if (source == menuItemRefractiveIndexVisualisation)
		{
			new PropertyOnLineVisualization(scene).setVisible(true);
		}
		else if (source == menuItemSpeedTest)
		{
			final RayGenerator gen = new RayGenerator();
			final Vector3d startpos = new Vector3d();
			final Vector3d startdir = new Vector3d();
			final RaySimulationObject rayObject = new RaySimulationObject();
			final int color[] = new int[4];
			long time = 0;
			for (int run = -100; run < 100000; ++run)
			{
				if (run ==0)
				{
					time = System.nanoTime();						
				}
				gen.threeDimensional = false;
				scene.updateScene();
				for (int i = 0; i < scene.getActiveLightCount(); ++i)
				{
					final OpticalSurfaceObject lightSource = (OpticalSurfaceObject)scene.getActiveLight(i);
					gen.setSource(lightSource);
					for (int j = 0; j < lightSource.numTracedRays; ++j)
					{
						gen.generate(j, lightSource.numTracedRays, startpos, startdir, rayObject.v3, color);
						for (int dir = 0; dir < (lightSource.bidirectional ? 2 : 1); ++dir)
						{
							if (dir == 0)
							{
								rayObject.direction.set(startdir);
							}
							else
							{
								rayObject.direction.invert(startdir);
							}
							rayObject.position.set(startpos);
							scene.calculateRay(rayObject, maxBounces, null, 0, null, null, null, null, null, 0);
						}
					}
				}
			}
			logger.info("Needed time: " + (System.nanoTime() - time)/1000000000.);
			JOptionPane.showMessageDialog(null, "Needed time: " + (System.nanoTime() - time)/1000000000.);
		}
		else if (source == toggleButtonThreeD || source == toggleButtonTwoD || source == toggleButtonRaytrace)
		{
			updateThreeD();
		}
		else if (source == menuItemCalGraph)
		{
			Interface interfaceObject = Interface.getInstance();
			DataHandler.reset(interfaceObject);
			interfaceObject.setVisible(true);
		}
		else if (source == menuItemTakeScreenshot)
		{
			JFileChooser fileChooser= new JFileChooserRecentFiles();
			JMathTextField textFieldSize = new JMathTextField(new ArrayOperation(new int[] {panelVisualization.getWidth(), panelVisualization.getHeight()}));
			fileChooser.setAccessory(textFieldSize);
			if(fileChooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION)
            {
				File selectedFile = fileChooser.getSelectedFile();
				String filepath = selectedFile.getAbsolutePath();
				if (filepath.endsWith("svg"))
				{
					try {
						FileWriter writer = new FileWriter(selectedFile);
						BufferedWriter outBuf = new BufferedWriter(writer);
						SvgDrawer drawer = new SvgDrawer(outBuf);
						Operation op = textFieldSize.get().calculate(scene.vs, new Controller());
						drawer.beginDocument((int)op.get(0).longValue(), (int)op.get(1).longValue());
						panelVisualization.paintComponent(drawer);
						drawer.endDocument();
						outBuf.close();
						writer.close();
					} catch (IOException e1) {
						visibleErrorMessage("Can't save Screensot", e1);
					}
				}
				else
				{
					BufferedImage im = new BufferedImage(currentVisualization.getWidth(), currentVisualization.getHeight(), BufferedImage.TYPE_INT_ARGB);
					currentVisualization.paint(im.getGraphics());
					try {
						ImageIO.write(im, filepath.substring(filepath.indexOf('.') + 1), selectedFile);
					} catch (IOException e1) {
						visibleErrorMessage("Can't save Screensot", e1);
					}
				}
		    }
		}
		else if (source == menuItemCalculateFocuseHeatMap)
		{
			new FocusAnalysisFrame(scene).setVisible(true);
		}
		else if (source == menuItemOptions)
		{
			InterfaceOptions optionFrame = InterfaceOptions.instantiator.get();
			//TODO
			optionFrame.addOptionPanel("Raytrace", RaytraceOptions.instantiator.get());
			optionFrame.setVisible(true);
		}
		else if (source == menuItemVolumePipeline)
		{
			volumePipelines.setVisible(true);
		}
		else if(source == menuItemCopy)
		{
			copyClipboard();
		}
		else if (source == menuItemCut)
		{
			cutClipboard();
		}
		else if (source == menuItemPaste)
		{
			pasteClipboard();
		}
		else if (source == menuItemPointCloudVisualisation)
		{
			JFileChooser fileChooser= new JFileChooserRecentFiles();
			if(fileChooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION)
			{
				File f = fileChooser.getSelectedFile();
				try {
					new PointCloudVisualization(f).setVisible(true);
				} catch (IOException e1) {
					logger.error("Can't read file", e1);
				}
			}
		}
		else if (source == menuItemDivergenceVisualisation)
		{
			/*int num_rays = 1;
			ArrayList<OpticalSurfaceObject> oso = new ArrayList<>();
			getSelectedLights(oso);
			RayGenerator gen = new RayGenerator();
			RaySimulationObject rso = new RaySimulationObject();
			RaySimulationData rsd = new RaySimulationData(num_rays, false);
			for (int i = 0; i < oso.size(); ++i)
			{
				gen.threeDimensional = true;
				gen.setSource(oso.get(i));
				scene.calculateRays(0, num_rays, num_rays, gen, 0, rsd.endpoints, rsd.enddirs, rsd.endcolor, null, rsd.accepted, rsd.bounces, rsd.lastObject, 10, false, rso, RaytraceScene.UNACCEPTED_MARK);
				
				double result[] = Geometry.getVariance(rsd.endpoints, rsd.enddirs, rsd.accepted, new Vector3d(0,0,0), new Vector3d(1,0,0), distances, new double[3]);
				ArrayUtil.sqrt(result, 0, result.length);
				DataHandler.globalVariables.setGlobal("divergence", new ArrayOperation(result));
				
				System.out.println(Arrays.toString(result));
			}*/	
		}
		else if (source == menuItemRecordMacro)
		{
			if (menuItemRecordMacro.isSelected())
			{
				session.addListener(this);				
			}
			else
			{
				session.removeListener(this);
				MacroWindow mw = new MacroWindow(scene, session);
				StringBuilder strB = new StringBuilder();
				for (int i = 0; i < executedCommands.size(); ++i)
				{
					strB.append(executedCommands.get(i)).append('\n');
				}
				mw.textArea.setText(strB.toString());
				executedCommands.clear();
				mw.setVisible(true);
			}
		}
		else if (source == menuItemRunMacro)
		{
			MacroWindow mw = new MacroWindow(scene, session);
			mw.setVisible(true);
		}
	}
    
    private final ArrayList<String> executedCommands = new ArrayList<String>();
    
    @Override
	public void commandExecuted(String command) {
    	executedCommands.add(command);
    }
     
    public void getSelectedLights(ArrayList<OpticalSurfaceObject> objects)
    {
    	for (int i = 0; i < tableSurfaces.getRowCount(); ++i)
		{
    		if (tableSurfaces.isRowSelected(i) && scene.surfaceObjectList.get(i).active)
    		{
    			objects.add(scene.surfaceObjectList.get(i));
    		}
		}
    }
    
    private void copyClipboard() {
    	try
		{
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			SceneIO.saveScene(out, true, scene, this);
			Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(out.toString()), null);
			out.close();
		}catch(IOException ex)
		{
			visibleErrorMessage("Error at copying", ex);
		}
	}
    
    private void cutClipboard()
    {
    	copyClipboard();
    	//TODO delete selected items
    }
    
    private void pasteClipboard()
    {
    	final Clipboard systemClipboard = Toolkit.getDefaultToolkit().getSystemClipboard(); 
		final Transferable transferData = systemClipboard.getContents( null ); 
		try{
			for(DataFlavor dataFlavor : transferData.getTransferDataFlavors()){ 
				Object content = transferData.getTransferData(dataFlavor); 
				
				if (content instanceof String){ 
					
					ByteArrayInputStream stream = new ByteArrayInputStream(((String)content).getBytes());
					SceneIO.loadScene(stream, scene, this);
					stream.close();
					break;
				}
			}
		}catch(IOException ex){}
		catch(UnsupportedFlavorException ex){} catch (JDOMException e1) {
			visibleErrorMessage("Error at copying", e1);
		}
    }

	public void clear()
    {
		currentSceneFile = null;
		scene.clear();
		panelTools.removeAll();
		volumePipelines.removeAllPipelines();
    }
    
    private final<E extends OpticalObject> void tablechanged(TableModelEvent e, JTable table, ArrayList<E> list, int colBegin, int rowBegin, int rowEnd, DefaultTableModel tableModel, COLUMN_TYPES types)
    {
    	int colEnd = e.getColumn() == TableModelEvent.ALL_COLUMNS ? table.getColumnCount() : (e.getColumn() + 1);
		for (int col = colBegin; col < colEnd; ++col)
		{
			for (int row = rowBegin; row < rowEnd; ++row)
			{
				try {
					list.get(row).setValue(types.getVisibleCol(col), tableModel.getValueAt(row, col), scene.vs, parser);
				} catch (OperationParseException ex) {
					logger.error("Can't read math expression",ex);
				} catch (NumberFormatException ex) {
					logger.error("Can't read number", ex);
				}
			}
		}
    }
    
    @Override
	public void tableChanged(TableModelEvent e) {
    	if (!EventQueue.isDispatchThread())
    	{
    		throw new RuntimeException("Table Changes only allowed by dispatchment thread");
    	}
       	if (!isUpdating)
		{
    		int rowBegin = e.getFirstRow();
        	if (rowBegin == TableModelEvent.HEADER_ROW)
        	{
        		return;
        	}
        	isUpdating = true;
 			Object source = e.getSource();
    		int colBegin = e.getColumn() == TableModelEvent.ALL_COLUMNS ? 0 : e.getColumn();
        	int rowEnd = e.getLastRow() + 1;
			if (source == tableModelSurfaces)
			{
				tablechanged(e, tableSurfaces, scene.surfaceObjectList, colBegin, rowBegin, rowEnd, tableModelSurfaces, GuiOpticalSurfaceObject.TYPES);
			}else if (source == tableModelVolumes)
			{
				tablechanged(e, tableVolumes, scene.volumeObjectList, colBegin, rowBegin, rowEnd, tableModelVolumes, GuiOpticalVolumeObject.TYPES);
			}else if (source == tableModelTextures)
			{
				tablechanged(e, tableTextures, scene.textureObjectList, colBegin, rowBegin, rowEnd, tableModelTextures, GuiTextureObject.TYPES);
			}else if (source == tableModelMeshes)
			{
				tablechanged(e, tableMeshes, scene.meshObjectList, colBegin, rowBegin, rowEnd, tableModelMeshes, MeshObject.TYPES);				
			}
			isUpdating = false;
		}
	}
     
	@Override
	public void valueChanged(ListSelectionEvent e) {
		updateAdvanced();				
	}
	
	@Override
	public void componentRemoved(ContainerEvent e) {
		if (panelTools.getComponentCount() == 0)
		{
			panelTools.setVisible(false);
		}
	}
	
	@Override
	public void componentAdded(ContainerEvent e) {
		if (panelTools.getComponentCount() != 0)
		{
			panelTools.setVisible(true);
			panelTools.revalidate();
		}
	}
	
	private class ScenePanel extends JPanel implements ActionListener, ItemListener, DocumentListener
	{
		private static final long serialVersionUID = -5262653872299747841L;
		private final JLabel labelVerifyRefractionIndices = new JLabel("Verify refraction Indices");
	    private final JCheckBox checkBoxVerifyRefractionIndices = new JCheckBox();
	    private final JLabel labelEnvironment = new JLabel("Read Environment");
	    private final JTextField textFieldEnvironment = new JTextField();
	    private final JLabel labelWritableEnvironment = new JLabel("Write Environment");
	    private final JComboBox<Object> comboBoxWritableEnvironment = new JComboBox<Object>();
	    private final JLabel labelRenderToTexture = new JLabel("RenderToTexture");
	    private final JComboBox<Object> comboBoxRenderToTexture = new JComboBox<Object>();
	    private final JLabel labelForceStartpoint = new JLabel("ForceStartpoint");
		private final JComboBox<Object> forceStartpoint = new JComboBox<Object>();
		private final JLabel labelForceEndpoint = new JLabel("Force Endpoint");
		private final JComboBox<Object> forceEndpoint = new JComboBox<Object>();
		private final JLabel labelBounds = new JLabel("Bounds");
		private final JMathTextField textFieldBounds = new JMathTextField();
		private final JLabel labelTextureMapping = new JLabel("Texture Mapping");
		private final JComboBox<TextureMapping> comboBoxTextureMapping = new JComboBox<TextureMapping>(TextureMapping.values());
		private final JLabel startingObjects = new JLabel("Camera starting Objects");
		private final JMathTextField cameraStartingObjects = new JMathTextField();
		
		public ScenePanel()
		{
			setLayout(JFrameUtils.DOUBLE_COLUMN_LAUYOUT);
		    checkBoxVerifyRefractionIndices.addActionListener(this);
			forceStartpoint.addItemListener(this);
			forceEndpoint.addItemListener(this);
			comboBoxRenderToTexture.addItemListener(this);
			comboBoxTextureMapping.addItemListener(this);
			textFieldEnvironment.getDocument().addDocumentListener(this);
			comboBoxWritableEnvironment.addItemListener(this);
			cameraStartingObjects.getDocument().addDocumentListener(this);
			add(labelEnvironment);
			add(textFieldEnvironment);
			add(labelWritableEnvironment);
			add(comboBoxWritableEnvironment);
			add(labelTextureMapping);
			add(comboBoxTextureMapping);
			add(labelRenderToTexture);
			add(comboBoxRenderToTexture);
			add(labelForceStartpoint);
			add(forceStartpoint);
			add(labelForceEndpoint);
			add(forceEndpoint);
			add(labelVerifyRefractionIndices);
			add(checkBoxVerifyRefractionIndices);
			add(labelBounds);
			add(textFieldBounds);
			add(startingObjects);
			add(cameraStartingObjects);
		}
		
		@Override
		public void actionPerformed(ActionEvent ae)
		{
			Object source = ae.getSource();
			if (source == checkBoxVerifyRefractionIndices) {
				scene.setVerifyRefractionIndices(checkBoxVerifyRefractionIndices.isSelected());
			}
		}
		
		@Override
		public void itemStateChanged(ItemEvent ie)
		{
			Object source = ie.getSource();
			if (source instanceof JComboBox)
			{
				if (!EventQueue.isDispatchThread())
		    	{
		    		throw new RuntimeException("Table Changes only allowed by dispatchment thread");
		    	}
				if (!isUpdating)
				{
					Object selected = ((JComboBox<?>)source).getSelectedItem();
					isUpdating = true;
					if (source == forceStartpoint)
					{
						scene.setForceStartpoint(selected);
					}
					else if (source == forceEndpoint)
					{
						scene.setForceEndpoint(forceEndpoint.getSelectedItem());
					}
					else if (source == comboBoxWritableEnvironment)
					{
						scene.setWritableEnvironmentTexture(selected == null ? null : selected.toString());
					}
					else if (source == comboBoxRenderToTexture)
					{						
						scene.setRenderToTexture(selected == null ? null : selected.toString());
					}	
					else if (source == comboBoxTextureMapping)
					{
						scene.setTextureMapping((TextureMapping)comboBoxTextureMapping.getSelectedItem());
					}
					isUpdating = false;
				}
			}
		}

		@Override
		public void changedUpdate(DocumentEvent de) {
			javax.swing.text.Document doc = de.getDocument();
			if (!EventQueue.isDispatchThread())
	    	{
	    		throw new RuntimeException("Table Changes only allowed by dispatchment thread");
	    	}
			if (!isUpdating)
			{
				if (doc == textFieldEnvironment.getDocument())
				{
					isUpdating = true;
					scene.setEnvironmentTexture(textFieldEnvironment.getText());
					isUpdating = false;
				}
				else if (doc == cameraStartingObjects.getDocument())
				{
					Operation op = cameraStartingObjects.get();
					ParseUtil parser = new ParseUtil();
					Controller controll = new Controller();
					try {
						scene.setCameraStartObjects(parser.parseStringArray(op, controll));
					} catch (OperationParseException e) {
						logger.error("Can't parse expression", e);
					}
				}
			}
		}

		@Override
		public void insertUpdate(DocumentEvent arg0) {
			changedUpdate(arg0);
		}

		@Override
		public void removeUpdate(DocumentEvent arg0) {
			changedUpdate(arg0);
		}
	}
	
	
	private final ScenePanel scenePanel = new ScenePanel();
     
	private void addTo(JMenu menu, JMenuItem toAdd)
	{
		menu.add(toAdd);
		toAdd.addActionListener(this);
	}
	
    public RaySimulationGui(){
        currentVisualization = panelVisualization;
    	JMenuBar menuBar = new JMenuBar();
    	++DataHandler.openWindows;
    	addWindowListener(this);
    	DataHandler.timedUpdater.add(rayUpdateHandler);
        setLayout(JFrameUtils.SINGLE_COLUMN_LAYOUT);
        glScene.addGlObject(sceneObjectTrajectories);
        glScene.addGlObject(sceneObjectEndpoints);
        sceneObjectEndpoints.setVisible(true);
        sceneObjectEndpoints.setVertexColorActivated(true);
        sceneObjectTrajectories.setVisible(true);
        sceneObjectEndpoints.setDrawType(DrawType.DOTS);
        sceneObjectTrajectories.setDrawType(DrawType.LINES);
    	
     	menuItemNew.setMnemonic('N');
    	menuItemClear.setMnemonic('C');
    	menuItemAppend.setMnemonic('A');
    	menuItemOpen.setMnemonic('O');
    	menuItemSave.setMnemonic('S');
    	menuItemExportTo.setMnemonic('E');
    	
    	menuItemCopy.setMnemonic('C');
    	menuItemCut.setMnemonic('X');
    	menuItemPaste.setMnemonic('P');
    	
    	menuFile.setMnemonic('F');
    	addTo(menuFile,menuItemNew);
    	addTo(menuFile,menuItemClear);
    	addTo(menuFile,menuItemOpen);
    	addTo(menuFile,menuItemAppend);
    	addTo(menuFile,menuItemSave);
    	addTo(menuFile,menuItemSaveTo);
    	addTo(menuFile,menuItemExportTo);
    	addTo(menuFile,menuItemSpeedTest);
    	addTo(menuEdit,menuItemCopy);
    	addTo(menuEdit,menuItemCut);
    	addTo(menuEdit,menuItemPaste);
    	addTo(menuAdd,menuItemAddSurface);
    	addTo(menuAdd,menuItemAddTexture);
    	addTo(menuAdd,menuItemAddVolume);
    	addTo(menuAdd,menuItemAddMesh);
    	addTo(menuAdd,menuItemAddSlider);
    	addTo(menuAdd,menuItemAddCodePad);
    	addTo(menuAdd,menuItemAddVariablePanel);
    	addTo(menuTools,menuItemCalculateFocuseHeatMap);
    	addTo(menuTools,menuItemStackPositionProcessor);
    	addTo(menuTools,menuItemParameterAnalysis);
    	addTo(menuTools,menuItemRefractiveIndexVisualisation);
    	addTo(menuItemExtras,menuItemOptions);
    	addTo(menuItemExtras,menuItemCalGraph);
    	addTo(menuItemExtras,menuItemTakeScreenshot);
    	addTo(menuItemExtras,menuItemVolumePipeline);
    	addTo(menuItemExtras,menuItemPointCloudVisualisation);
    	addTo(menuItemExtras,menuItemDivergenceVisualisation);
    	addTo(menuItemExtras,menuItemRecordMacro);
    	addTo(menuItemExtras,menuItemRunMacro);
    	menuItemExtras.add(licenseMenu);
    	menuBar.add(menuFile);
    	menuBar.add(menuEdit);
    	menuBar.add(menuAdd);
    	menuBar.add(menuTools);
    	menuBar.add(menuItemExtras);
    	volumePipelines = new VolumePipelines(scene);
    	selectedObjectPanel.setLayout(new BoxLayout(selectedObjectPanel, BoxLayout.Y_AXIS));
    	panelTools.setVisible(false);
    	panelTools.addContainerListener(this);
        
        setBounds(10,10, 505, 330);
        setTitle("Projekteinstellungen");
        setResizable(true);
        setMinimumSize(getPreferredSize());
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        DataHandler.addToUpdateTree(this);
		
		toggleButtonTwoD.addActionListener(this);
		toggleButtonThreeD.addActionListener(this);
		toggleButtonRaytrace.addActionListener(this);
		tableSurfaces.getSelectionModel().addListSelectionListener(this);
		tableVolumes.getSelectionModel().addListSelectionListener(this);
		tableMeshes.getSelectionModel().addListSelectionListener(this);
		tableTextures.getSelectionModel().addListSelectionListener(this);
		tableSurfaces.getModel().addTableModelListener(this);
		tableModelVolumes.addTableModelListener(this);
		tableModelTextures.addTableModelListener(this);
		tableModelMeshes.addTableModelListener(this);
		
		scene.add(this);
		
		scene.addObjectChangeListener((GuiTextureObject.TextureObjectChangeListener)this);
		scene.addObjectChangeListener((GuiOpticalVolumeObject.OpticalVolumeObjectChangeListener)this);
		scene.addObjectChangeListener((GuiOpticalSurfaceObject.OpticalSurfaceObjectChangeListener)this);
		scene.addObjectChangeListener((MeshObject.MeshObjectChangeListener)this);

        GuiOpticalVolumeObject ovo = new GuiOpticalVolumeObject(scene.vs, parser);
        ovo.readDycom("/media/paul/Data1/Caesar/HawkScans/stacked/m006-stacked.dcm");
        scene.add(ovo);
		scene.add(new GuiOpticalSurfaceObject(new SCENE_OBJECT_COLUMN_TYPE[] {SCENE_OBJECT_COLUMN_TYPE.ID, SCENE_OBJECT_COLUMN_TYPE.POSITION, SCENE_OBJECT_COLUMN_TYPE.DIRECTION, SCENE_OBJECT_COLUMN_TYPE.MATERIAL, SCENE_OBJECT_COLUMN_TYPE.SURFACE, SCENE_OBJECT_COLUMN_TYPE.MAXRADIUS}, new Object[]{ "Mirror",new Vector3d(400,200,0),new double[]{10,5,0},MaterialType.REFLECTION,SurfaceType.FLAT,100 }, scene.vs, parser));
        scene.add(new GuiOpticalSurfaceObject(new SCENE_OBJECT_COLUMN_TYPE[] {SCENE_OBJECT_COLUMN_TYPE.ID, SCENE_OBJECT_COLUMN_TYPE.POSITION, SCENE_OBJECT_COLUMN_TYPE.DIRECTION, SCENE_OBJECT_COLUMN_TYPE.MATERIAL, SCENE_OBJECT_COLUMN_TYPE.SURFACE, SCENE_OBJECT_COLUMN_TYPE.MAXRADIUS}, new Object[]{ "Wall",new double[]{0,10,0},new double[]{10,0,0},MaterialType.ABSORBATION,SurfaceType.FLAT,100}, scene.vs, parser));
		scene.add(new GuiOpticalSurfaceObject(new SCENE_OBJECT_COLUMN_TYPE[] {SCENE_OBJECT_COLUMN_TYPE.ID, SCENE_OBJECT_COLUMN_TYPE.POSITION, SCENE_OBJECT_COLUMN_TYPE.DIRECTION, SCENE_OBJECT_COLUMN_TYPE.MATERIAL, SCENE_OBJECT_COLUMN_TYPE.SURFACE, SCENE_OBJECT_COLUMN_TYPE.MAXRADIUS}, new Object[]{ "Lense",new double[]{200,200,0},new double[]{100,0,0},MaterialType.ABSORBATION,SurfaceType.HYPERBOLIC ,100}, scene.vs, parser));
		scene.add(new GuiOpticalSurfaceObject(new SCENE_OBJECT_COLUMN_TYPE[] {SCENE_OBJECT_COLUMN_TYPE.ID, SCENE_OBJECT_COLUMN_TYPE.POSITION, SCENE_OBJECT_COLUMN_TYPE.DIRECTION, SCENE_OBJECT_COLUMN_TYPE.MATERIAL, SCENE_OBJECT_COLUMN_TYPE.SURFACE, SCENE_OBJECT_COLUMN_TYPE.MAXRADIUS, SCENE_OBJECT_COLUMN_TYPE.TRACED_RAYS, SCENE_OBJECT_COLUMN_TYPE.UNTRACED_RAYS}, new Object[]{ "Emission",new double[]{-50,0,0},new double[]{50,0,0},MaterialType.EMISSION,SurfaceType.FLAT ,50, 100, 0}, scene.vs, parser));
		surfaceDeleteColumn.setMnemonic(KeyEvent.VK_D);
		volumeDeleteColumn.setMnemonic(KeyEvent.VK_D);
		volumeLoadColumn.setMnemonic(KeyEvent.VK_D);
		volumeViewColumn.setMnemonic(KeyEvent.VK_D);
		texturOpenColumn.setMnemonic(KeyEvent.VK_D);	
		texturLoadColumn.setMnemonic(KeyEvent.VK_D);	
		texturSaveColumn.setMnemonic(KeyEvent.VK_D);	
		texturSaveToColumn.setMnemonic(KeyEvent.VK_D);
		texturViewColumn.setMnemonic(KeyEvent.VK_D);	
		texturDeleteColumn.setMnemonic(KeyEvent.VK_D);

		tapPane.add(scrollPaneSurfaces, "Surfaces");
		tapPane.add(scrollPaneMeshes, "Meshes");
		tapPane.add(scrollPaneVolumes, "Volumes");
		tapPane.add(scrollPaneTextures, "Textures");
		tapPane.add(scenePanel, "Scene");
		tapPane.add(textAreaProjectInformation,"Description");
		
		layout = new GroupLayout(getContentPane());
    	//layout = new GroupLayout(content);
        getContentPane().setLayout(layout);
		GroupLayout.Group horizontalGroup = layout.createParallelGroup();
    	GroupLayout.Group verticalGroup = layout.createSequentialGroup();
    	horizontalGroup
			.addComponent(tapPane)
			.addGroup(layout.createSequentialGroup()
				.addComponent(toggleButtonTwoD)
				.addComponent(toggleButtonThreeD)
				.addComponent(toggleButtonRaytrace)
				.addComponent(toggleButtonTraceRays)
				.addComponent(labelAcceptedFraction))
			.addGroup(layout.createSequentialGroup()
				.addComponent(panelVisualization, 0, 0, 100000)
				.addComponent(panelTools, 315, 315, 315))
			.addComponent(selectedObjectPanel);
    	
    	verticalGroup
	    	.addComponent(tapPane, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
	    	.addGroup(layout.createParallelGroup()
	    	.addComponent(toggleButtonTwoD, 25, 25, 25)
	    	.addComponent(toggleButtonThreeD, 25, 25, 25)
	    	.addComponent(toggleButtonRaytrace, 25, 25, 25)
	    	.addComponent(toggleButtonTraceRays, 25, 25, 25)
	    	.addComponent(labelAcceptedFraction, 25, 25, 25))
	    	.addGroup(layout.createParallelGroup()
    			.addComponent(panelVisualization)
    			.addComponent(panelTools))
	    	.addComponent(selectedObjectPanel);
     	layout.setHorizontalGroup(horizontalGroup);
        layout.setVerticalGroup(verticalGroup);
		
        ButtonGroup bg = new ButtonGroup();
        toggleButtonTwoD.setSelected(true);
        bg.add(toggleButtonTwoD);
        bg.add(toggleButtonThreeD);
        bg.add(toggleButtonRaytrace);
        
        setJMenuBar(menuBar);
        EventQueue.invokeLater(new Runnable()
		{
        	@Override
			public void run()
        	{
        		updateAllTables();
        	}
    	});
    }
    
     @Override
	public void itemStateChanged(ItemEvent e) {
		
	}
     
     @Override
	public void valueChanged(final byte ct, final Object o) {
    	 
	 	switch(ct)
		{
	 	//TODO
			case RaytraceScene.OBJECT_ADD:
				if (((OpticalObject)o).attachements.get(glObjectAttachementId) != null)
				{
					throw new RuntimeException();
				}
				SceneObject so = null;
				if (o instanceof GuiOpticalVolumeObject)
				{
					//so = new SceneObjectLine();
					so = new SceneObjectMesh(SceneObjectMesh.TRIANGLE);
				}
				if (o instanceof OpticalSurfaceObject || o instanceof MeshObject)
				{
					so = new SceneObjectMesh(o instanceof MeshObject ? SceneObjectMesh.TRIANGLE : SceneObjectMesh.QUAD);
				}
				if (so != null)
				{
					so.setVisible(true);
					so.setDrawType(DrawType.SOLID_SMOOTH);
					so.lightMaterial.set(true, true, true, false, Color.WHITE);
					so.lightMaterial.set(false, false, false, true, Color.BLACK);
					so.reflectionMaterial.set(true, true, true, false, Color.BLACK);
					so.reflectionMaterial.set(false, false, false, true, Color.WHITE);
					so.reflectionMaterial.Ns = so.lightMaterial.Ns = 100;
		    		glScene.addGlObject(so);
					((OpticalObject)o).addDataChangeListener(this);
		    		((OpticalObject)o).attachements.attachObject(so, glObjectAttachementId);
					dataChanged((OpticalObject)o);
				}
				
				break;
			case RaytraceScene.OBJECT_REMOVE:
				Object attachement = ((OpticalObject) o).attachements.get(glObjectAttachementId);
				if (attachement != null)
				{
					glScene.removeGlObject((SceneObject)attachement);
				}
				if (o instanceof GuiOpticalVolumeObject)
				{
					((GuiOpticalVolumeObject)o).removeDataChangeListener(this);
				}
				break;
			default:
				break;
		}
	 	
		if (!isUpdating || !EventQueue.isDispatchThread())
		{
			EventQueue.invokeLater(new Runnable()
			{
				@Override
				public void run()
				{
					isUpdating = true;
					if (ct == RaytraceScene.OBJECT_ADD || ct == RaytraceScene.OBJECT_REMOVE)
				 	{
				 		if (o instanceof SurfaceObject || o instanceof OpticalVolumeObject || o == null)
						{
							updateBoundComboBoxes();
				 		}
				    	if (o instanceof GuiTextureObject || o == null)
				    	{
				    		updateTextureComboBoxes();
				    	}
				 	}
					if (ct == -1)
					{
						scenePanel.textFieldEnvironment.setText(scene.environmentTextureString);
						scenePanel.comboBoxWritableEnvironment.setSelectedItem(scene.writableEnvironmentTexture);
						scenePanel.comboBoxRenderToTexture.setSelectedItem(scene.renderToTextureObject);
						load(scenePanel.forceEndpoint, scene.forceEndpoint);
						load(scenePanel.forceStartpoint, scene.forceStartpoint);
			 			updateAllTables();
					}
					else
					{
						switch(ct)
						{
							case RaytraceScene.ENVIRONMENT_TEXTURE:			scenePanel.textFieldEnvironment.setText(scene.environmentTextureString);							break;
							case RaytraceScene.WRITABLE_ENVIRONMENT_TEXTURE:scenePanel.comboBoxWritableEnvironment.setSelectedItem(scene.writableEnvironmentTexture);			break;
							case RaytraceScene.RENDER_TO_TEXTURE:			scenePanel.comboBoxRenderToTexture.setSelectedItem(scene.renderToTextureObject);					break;
							case RaytraceScene.FORCE_ENDPOINT:				load(scenePanel.forceEndpoint, scene.forceEndpoint);												break;
							case RaytraceScene.FORCE_STARTPOINT:			load(scenePanel.forceStartpoint, scene.forceStartpoint);											break;
							case RaytraceScene.ENVIRONMENT_MAPPING:			scenePanel.comboBoxTextureMapping.setSelectedItem(scene.environment_mapping);						break;
							case RaytraceScene.VERIFY_REFRACTION_INDICES:	scenePanel.checkBoxVerifyRefractionIndices.setSelected(scene.isVerifyRefractionIndexActivated());	break;
							default:break;
						}
					}
					if (ct == RaytraceScene.OBJECT_REMOVE || ct == RaytraceScene.OBJECT_ADD)
					{
						if (o instanceof OpticalSurfaceObject)
						{
							updateSurfaceTable();
						}
						else if (o instanceof OpticalVolumeObject)
						{
							updateVolumeTable();
						}
						else if (o instanceof GuiTextureObject)
						{
							updateTextureTable();
						}
						else if (o instanceof MeshObject)
						{
							updateMeshTable();
						}
					}
					isUpdating = false;
				}
			});
		}
		if (ct == RaytraceScene.OBJECT_ADD)
		{
			if (o instanceof GuiOpticalSurfaceObject)
			{
				valueChanged((GuiOpticalSurfaceObject)o, null);
			}
			else if (o instanceof GuiOpticalVolumeObject)
			{
				valueChanged((GuiOpticalVolumeObject)o, null);
			}
		}
		panelVisualization.repaint();
		if (panelVisualization != currentVisualization)
		{
			currentVisualization.repaint();
		}
		DataHandler.runnableRunner.run(untracedRayRunnable, false);
	}
     
    private Component currentVisualization;
    private void updateThreeD()
    {
    	if (toggleButtonThreeD.isSelected())
        {
        	if (!(currentVisualization instanceof JoglCanvas))
        	{
        		JoglCanvas canvasVisualization = JoglCanvas.getInstance(glScene);
        		layout.replace(currentVisualization, canvasVisualization);
        		currentVisualization = canvasVisualization;
        	}
        }
        else if (toggleButtonTwoD.isSelected())
        {
        	if (!(currentVisualization instanceof VisualizationPanel))
        	{
	    		layout.replace(currentVisualization, panelVisualization);
	    		currentVisualization = panelVisualization;
        	}
        }
        else if (toggleButtonRaytrace.isSelected())
        {
        	if (!(currentVisualization instanceof RaytraceVisualization))
        	{
        		RaytraceVisualization tmp = new RaytraceVisualization();
	    		layout.replace(currentVisualization, tmp);
	    		currentVisualization = tmp;
        	}
        }
        else
        {
        	JPanel tmp = new JPanel();
        	layout.replace(currentVisualization, tmp);
        	currentVisualization = tmp;
        }
    }
     
    private class RaytraceVisualization extends JComponent implements MouseListener, MouseMotionListener, KeyListener, TimedUpdateHandler, ComponentListener, MouseWheelListener
    {
    	/**
		 * 
		 */
		private static final long serialVersionUID = -6243329713456426333L;
		Camera camera = new Camera();
		OpenGlInputHandler oih = new OpenGlInputHandler();
    	public SimpleCameraListener cameraListener = new SimpleCameraListener(oih, oih, glScene, camera);
    	boolean triggeredUpdate = false;
    	int mouseX, mouseY;
   //	   			glScene.setCameraPosition(camera.position, camera.rotation);
 	
    	private class OpenGlInputHandler implements OpenGlKeyHandler, OpenGlMouseHandler 
    	{
    		@Override
			public int getX()
        	{
        		return mouseX;
        	}
    		
        	@Override
			public int getY()
        	{
        		return mouseY;
        	}
        	
        	@Override
    		public boolean isButtonDown(int i) {return mButtons[i];}

    		@Override
    		public int getDWheel() {
    			int tmp = wheel;
    			wheel = 0;
    			return tmp;
    		}

    		@Override
    		public boolean isJavaKeyDown(int keyDown) {return keys[keyDown];}
    	}
    	
    	@Override
		public void update() {
			cameraListener.run();
		}
    	
    	
		
		@Override
		public int getUpdateInterval() {
			return 10;
		}
    	
    	public RaytraceVisualization()
    	{
        	setFocusable(true);
        	addMouseListener(this);
        	addMouseMotionListener(this);
        	addKeyListener(this);
        	addComponentListener(this);
        	addMouseWheelListener(this);
        	DataHandler.timedUpdater.add(this);
    	}
    	
    	RaytraceScene.CameraViewRunnable cvr = scene.new CameraViewRunnable() {
    		@Override
    		public void finished()
    		{
    			super.finished();
    			triggeredUpdate = true;
    			repaint();
    		}
    	};


		@Override
    	public synchronized void paintComponent(Graphics g)
    	{
			super.paintComponent(g);
			if (previewTexture.image != null)
			{
				g.drawImage(previewTexture.image,0, 0, previewTexture.image.getWidth(), previewTexture.image.getHeight(), null);
			}
			if (triggeredUpdate)
			{
	    		triggeredUpdate = false;
			}
			else
			{
				int width = getWidth();
				int height = getHeight();
				if (previewTexture.image == null || previewTexture.image.getWidth() != width || previewTexture.image.getHeight() != getHeight())
				{
					previewTexture.setImage(new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB));
				}
	   			cvr.gto = previewTexture;
				cvr.gen.position.set(glScene.cameraPosition);
	    		cvr.gen.position.multiply(1/dScale);
	   			cvr.gen.rotation.set(glScene.cameraRotation);
   				DataHandler.runnableRunner.run(cvr, false);
			}
    	}
		boolean keys[] = new boolean[1024];
		boolean mButtons[] = new boolean[4];
		private int wheel;

		@Override
		public void keyPressed(KeyEvent e) {keys[e.getKeyCode()] = true;}

		@Override
		public void keyReleased(KeyEvent e) {keys[e.getKeyCode()] = false;}

		@Override
		public void keyTyped(KeyEvent e) {}

		@Override
		public void mouseClicked(MouseEvent e) {}

		@Override
		public void mouseEntered(MouseEvent e) {}

		@Override
		public void mouseExited(MouseEvent e) {}

		@Override
		public void mousePressed(MouseEvent e) {System.out.println(e);mButtons[e.getButton() - 1] = true;
		mouseX = e.getX();
		mouseY = e.getY();}

		@Override
		public void mouseReleased(MouseEvent e) {mButtons[e.getButton() - 1] = false;}

		@Override
		public void mouseDragged(MouseEvent e) {mouseX = e.getX();
		mouseY = e.getY();}

		@Override
		public void mouseMoved(MouseEvent arg0) {}



		@Override
		public void componentHidden(ComponentEvent arg0) {}

		@Override
		public void componentMoved(ComponentEvent arg0) {}

		@Override
		public void componentResized(ComponentEvent arg0) {
			System.out.println(getWidth());
			camera.setSize(getWidth(), getHeight());
		}
		
		@Override
		public void componentShown(ComponentEvent arg0) {}



		@Override
		public void mouseWheelMoved(MouseWheelEvent e) {
			if (e.getScrollType() == MouseWheelEvent.WHEEL_UNIT_SCROLL)
			{
				wheel += e.getWheelRotation() * e.getScrollAmount();
			}
			else
			{
				wheel += e.getUnitsToScroll();
			}
		}
    }
    
    private void updateAdvanced()
    {
        selectedObjectPanel.removeAll();
    	
        if (advancedView)
    	{
        	int selectedRows[] = tableSurfaces.getSelectedRows();
            for (int i = 0; i < selectedRows.length; ++i)
	    	{
            	OpticalObjectPanel oop =new OpticalObjectPanel(scene.surfaceObjectList.get(selectedRows[i]), scene.vs);
	        	selectedObjectPanel.add(oop);
			}
            selectedRows = tableVolumes.getSelectedRows();
            for (int i = 0; i < selectedRows.length; ++i)
	    	{
            	OpticalObjectPanel oop =new OpticalObjectPanel(scene.volumeObjectList.get(selectedRows[i]), scene.vs);
	        	selectedObjectPanel.add(oop);
			}
            selectedRows = tableMeshes.getSelectedRows();
            for (int i = 0; i < selectedRows.length; ++i)
	    	{
            	OpticalObjectPanel oop =new OpticalObjectPanel(scene.meshObjectList.get(selectedRows[i]), scene.vs);
	        	selectedObjectPanel.add(oop);
			}
            selectedRows = tableTextures.getSelectedRows();
            for (int selected : selectedRows)
            {
            	OpticalObjectPanel oop =new OpticalObjectPanel(scene.textureObjectList.get(selected), scene.vs);
	        	selectedObjectPanel.add(oop);
            }
    	}
        selectedObjectPanel.revalidate();
    }
    
    public void updateAllTables()
    {
    	updateSurfaceTable();
		updateVolumeTable();
		updateTextureTable();
		updateMeshTable();
    }
    
    private static final DefaultCellEditor checkBoxCellEditor = new DefaultCellEditor(new JCheckBox()); 
    
    private final <E extends OpticalObject> void updateTable(JTable table, JScrollPane scrollPane, ArrayList<E> objectList, COLUMN_TYPES types, DefaultTableModel tm, ButtonColumn ...buttonColumn)
    {
    	Object[][] rowData = new Object[objectList.size()][types.visibleColsSize()];
    	for (int i = 0; i < rowData.length; ++i)
    	{
    		OpticalObject obj = objectList.get(i);
    		for (int j = 0; j < types.visibleColsSize();++j)
    		{
    			Object value = obj.getValue(types.getVisibleCol(j));
    			if (value instanceof Boolean)
    			{
    				rowData[i][j] = value;
    			}
    			else if (value == null)
    			{
    				rowData[i][j] = null;
    			}
    			else
    			{
    				rowData[i][j] = String.valueOf(value);
    			}
    		}
    	}

    	tm.setDataVector(rowData, types.getVisibleColumnNames());
		for (int i = 0; i < types.visibleColsSize(); ++i)
		{
			SCENE_OBJECT_COLUMN_TYPE current = types.getVisibleCol(i); 
			TableColumn column = table.getColumnModel().getColumn(i);
		  	if (current.optionType == OpticalObject.TYPE_COMBOBOX)
		 	{
		     	JComboBox<String> comboBox = new JComboBox<String>(current.possibleValues.toArray(new String[current.possibleValues.size()]));
		     	column.setCellEditor(new DefaultCellEditor(comboBox));
		 	}else if (current.optionType == OpticalObject.TYPE_CHECKBOX)
		 	{
		 		column.setCellEditor(checkBoxCellEditor);
		 	}
		}

		for (ButtonColumn bc : buttonColumn)
		{
			bc.setTable(table);
		}
		Dimension dim = table.getPreferredSize();
		scrollPane.setPreferredSize(new Dimension(dim.width, dim.height + table.getTableHeader().getPreferredSize().height + 8));
    }
    
    void updateSurfaceTable()
    {
    	updateTable(tableSurfaces, scrollPaneSurfaces, scene.surfaceObjectList, GuiOpticalSurfaceObject.TYPES, tableModelSurfaces, surfaceDeleteColumn);
    	
    }
    
    void updateVolumeTable()
    {
    	updateTable(tableVolumes, scrollPaneVolumes, scene.volumeObjectList, GuiOpticalVolumeObject.TYPES, tableModelVolumes, volumeDeleteColumn, volumeLoadColumn, volumeViewColumn);
    }
    
    void updateTextureTable()
    {
    	updateTable(tableTextures, scrollPaneTextures, scene.textureObjectList, GuiTextureObject.TYPES, tableModelTextures, texturOpenColumn,texturLoadColumn, texturSaveColumn, texturSaveToColumn, texturViewColumn, texturDeleteColumn);
    }
	    
    void updateMeshTable()
    {
    	updateTable(tableMeshes, scrollPaneMeshes, scene.meshObjectList, MeshObject.TYPES, tableModelMeshes, meshOpenColumn, meshSaveColumn, meshDeleteColumn);
    }
    
    private static float[] drawVolume(OpticalVolumeObject v, Drawer g, Vector2d translation, double scale, float vertices[]) throws IOException
    {
    	/*
    	vertices = ArrayUtil.ensureLength(vertices, v.numMeshVertices() * 3);
		v.getMeshVertices(vertices);
		for (int i = 0; i < 8; ++i)
		{
			int x0 = (int)((vertices[i * 3] + translation.x) * scale);
			int y0 = (int)((vertices[i * 3 + 1] + translation.y) * scale);
			
			for (int b = 0; b < 3; ++b)
			{
				int j = i | (1 << b);
				if (j < 8 && j != i)
				{
					int x1 = (int)((vertices[j * 3] + translation.x) * scale);
					int y1 = (int)((vertices[j * 3 + 1] + translation.y) * scale);
					g.drawLine(x0,y0,x1,y1);
				}
			}
		}*/
		return vertices;
    }
    
    private static float[] drawMesh(MeshObject mesh, Drawer g, Vector2d translation, double scale, float vertices[]) throws IOException
    {
    	vertices = ArrayUtil.ensureLength(vertices, mesh.getMeshVertexLength());
		mesh.getMeshVertices(vertices);
		int lines[] = mesh.getLines();
		for (int i = 0; i < lines.length; i += 2)
		{
			int l0 = lines[i] * 3;
			int l1 = lines[i + 1] * 3;
			/*if (vertices[l0 + 2] < -1 || vertices[l0 + 2] > 1 || vertices[l1 + 2] < -1 || vertices[l1 + 2] > 1)
			{
				continue;
			}*/
			double x0 = (vertices[l0] + translation.x) * scale;
			double y0 = (vertices[l0 + 1] + translation.y) * scale;
			double x1 = (vertices[l1] + translation.x) * scale;
			double y1 = (vertices[l1 + 1] + translation.y) * scale;
			g.drawLine(x0,y0,x1,y1);
		}
		return vertices;
    }
        
    private static void drawSurface(OpticalSurfaceObject l, Drawer draw, Vector3d vec, Vector2d translation, double scale) throws IOException
    {
    	Vector3d midpoint = l.midpoint;
		Vector3d direction = l.direction;
		double x = (midpoint.x + translation.x) * scale;
	 	double y = (midpoint.y + translation.y) * scale;
	 	if (drawAnchorPoints)
	 	{
	 		draw.fillCircle(x, y, 3);
	 	}
	 	if (drawDirectionVector)
	 	{
	 		double xscale = direction.x * scale;
	 		double yscale = direction.y * scale;
	 		double headx = x + xscale;
	 		double heady = y + yscale;
	 		draw.drawLine(x, y, headx, heady);
	 		double sub = (xscale - yscale) * 0.3, add = (xscale + yscale) * 0.3;
	 		draw.drawLine(headx, heady, headx - add, heady + sub);
	 		draw.drawLine(headx, heady, headx - sub, heady - add);
	 	}
 		switch (l.surf)
		{
			case FLAT:
			{
				vec.set(direction);
				vec.rotateRadiansZ(Math.PI * 0.5);
				double rmax = l.maxRadiusGeometric * scale;
				vec.setLength(rmax);
				
				if (l.minRadiusGeometric == 0)
				{
					draw.drawLine(x + vec.x, y + vec.y, x - vec.x, y - vec.y);
				}
				else
				{
					double rmin = l.minRadiusGeometric * scale / rmax;
					double xdiff = vec.x * rmin, ydiff = vec.y * rmin;
					draw.drawLine((int)(x + xdiff), (int)(y + ydiff), (int)(x + vec.x), (int)(y + vec.y));
					draw.drawLine((int)(x - xdiff), (int)(y - ydiff), (int)(x - vec.x), (int)(y - vec.y));
				}
				break;
			}
			case HYPERBOLIC:
			{
				vec.set(direction);
				vec.rotateRadiansZ(Math.PI * 0.5);
				vec.setLength(l.maxRadiusGeometric * scale / 16);
				x += direction.x * scale;
				y += direction.y * scale;
				double mult = scale;
				double multToRad = l.radiusGeometricQ * l.invDirectionLengthQ /256;
				for (int i = -16; i <= 16; ++i)
				{
					double heigh = mult * (Math.sqrt(i * i * multToRad + 1) - 1);
					draw.pushPoint(x + i * vec.x - heigh * direction.x, y + i * vec.y - heigh * direction.y);
				}
				
				draw.drawPolyLine();
				break;
			}
			case PARABOLIC:
			{
				vec.set(direction);
				vec.rotateRadiansZ(Math.PI * 0.5);
				vec.setLength(l.maxRadiusGeometric * scale / 16);
				x += direction.x * scale;
				y += direction.y * scale;
				double mult = 0.5 * scale * l.radiusGeometricQ * l.invDirectionLengthQ/256;
				
				for (int i = -16; i <= 16; ++i)
				{
					double heigh = mult * (i * i);
					draw.pushPoint(x + i * vec.x - heigh * direction.x, y + i * vec.y - heigh * direction.y);
				}
				
				draw.drawPolyLine();
				break;
			}
			case SPHERICAL:
			{
				double arcDir = Math.atan2(direction.x, direction.y) * (180 / Math.PI) - 90;
				double maxArcOpen = l.getMaxArcOpen() * (180 / Math.PI);
				double radius = scale * l.directionLength;
				if (l.minRadiusGeometric == 0)
				{
					draw.drawArc(x - radius, y - radius, radius *2, radius *2, arcDir - maxArcOpen, maxArcOpen * 2);
				}
				else
				{
					double minArcOpen = l.getMinArcOpen() * (180 / Math.PI);
					
					draw.drawArc(x - radius, y - radius, radius *2, radius *2, arcDir - maxArcOpen, maxArcOpen - minArcOpen);
					draw.drawArc(x - radius, y - radius, radius *2, radius *2, arcDir + minArcOpen, maxArcOpen - minArcOpen);
				}
				break;
			}
			case CUSTOM:
			{
				vec.set(direction);
				vec.rotateRadiansZ(Math.PI * 0.5);
				vec.setLength(scale * l.directionLength);
				x += direction.x * scale;
				y += direction.y * scale;
				double tmp0 = (1 + l.conicConstant);
				double dx = direction.x * scale, dy = direction.y * scale;
				
				
				double dotProdLowerBound = (l.getDotProdLowerBound() + 1) / 16;
				double dotProdUpperBound = (l.getDotProdUpperBound() + 1) / 16;
				draw.setPointNumber(33);
				for (int i = 0; i <= 16; ++i)
				{
					double r = (dotProdLowerBound * (16 - i) + dotProdUpperBound * i);
					double z =  Math.sqrt(r * (2 - tmp0 * r));
					
					draw.setPoint(x + z * vec.x - r * dx, y + z * vec.y - r * dy, 16 - i);
					draw.setPoint(x - z * vec.x - r * dx, y - z * vec.y - r * dy, 16 + i);
				}
				draw.drawPolyLine();
				break;
			}
			case CYLINDER:
			{
				vec.set(direction);
				vec.rotateRadiansZ(Math.PI * 0.5);
				vec.multiply(scale);
				double minMult = l.minRadiusGeometric * l.invDirectionLength * scale;
				double minDirx = direction.x * minMult;
				double minDiry = direction.y * minMult;
				double maxMult = l.maxRadiusGeometric * l.invDirectionLength * scale;
				double dirx = direction.x * maxMult;
				double diry = direction.y * maxMult;
				draw.drawLine(x-vec.x+minDirx, y-vec.y+minDiry, x-vec.x+dirx, y-vec.y+diry);
				draw.drawLine(x+vec.x+minDirx, y+vec.y+minDiry, x+vec.x+dirx, y+vec.y+diry);
				break;
			}
			default:
				break;
		 
		 }
    }
    
   
    
    private static Color LENSE_INVISIBLE = new Color(0,0,0,0x40);
    private static Color EMISSION_INVISIBLE = new Color(0,0,0xFF,0x40);
	private static Color RAY_BLACK = new Color(0xFF,0,0,0x40);
	private static Color RAY_RED = new Color(0xFF,0,0,0x40);

    public class VisualizationPanel extends JComponent implements MouseListener, MouseMotionListener, MouseWheelListener, KeyListener
    {
    	 /**
		 * 
		 */
		private static final long serialVersionUID = -5720573203358723523L;
		private int selectedDrag = -1;
		private int selectedAdvanced = -1;
		private int startX;
		private int startY;
		public double scale = 1;
	    private Vector3d originalPosition;
		private Vector3d originalDirection;
		private double originalGeometricRadius = Double.NaN;
		private double newGeometricRadius = Double.NaN;
		private Vector3d newPosition;
		private Vector3d newDirection;
		private Vector2d originalPaintOffset;
		@Override
		public void mouseReleased(MouseEvent e) {
			originalPosition = null;
			originalDirection = null;
			originalGeometricRadius = Double.NaN;
			selectedDrag = -1;
		}
		
		@Override
		public void mousePressed(MouseEvent e) {
			grabFocus();
			selectedAdvanced = selectedDrag = getLense((startX = e.getX()) , (startY = e.getY()) );
			advancedView |= selectedAdvanced >= 0;
			if (selectedDrag >= 0)
			{
				OpticalObject selectedObject;
				if (selectedDrag < tableSurfaces.getRowCount())
				{
					selectedObject = scene.surfaceObjectList.get(selectedDrag);
					tableSurfaces.setRowSelectionInterval(selectedDrag, selectedDrag);
				}
				else if (selectedDrag < tableSurfaces.getRowCount() + tableVolumes.getRowCount())
				{
					int row = selectedDrag - tableSurfaces.getRowCount();
					selectedObject = scene.volumeObjectList.get(row);
					tableVolumes.setRowSelectionInterval(row, row);
				}
				else
				{
					int row = selectedDrag - tableSurfaces.getRowCount() - tableVolumes.getRowCount();
					selectedObject = scene.meshObjectList.get(row);
					tableMeshes.setRowSelectionInterval(row, row);
				}
				switch (e.getButton())
				{
					case 1:
					{
						
						if (selectedObject instanceof GuiOpticalSurfaceObject)
						{
							originalPosition = new Vector3d(((GuiOpticalSurfaceObject)selectedObject).position);
						}
						else if (selectedObject instanceof GuiOpticalVolumeObject || selectedObject instanceof MeshObject)
						{
							originalPosition = new Vector3d(selectedObject.midpoint);
						}
						newPosition = new Vector3d(originalPosition);
						break;
					}
					case 2:
					{
						originalGeometricRadius = scene.surfaceObjectList.get(selectedDrag).maxRadiusGeometric;
						newGeometricRadius = originalGeometricRadius;
						break;
					}
					case 3:
					{
						originalDirection = new Vector3d(scene.surfaceObjectList.get(selectedDrag).direction);
						newDirection = new Vector3d(originalDirection);
					   	break;
					}
				}
			}
			else
			{
				originalPaintOffset = new Vector2d(paintOffset);
			}
		}
		
		public int getLense(int ex, int ey)
	    {
	    	try
		    	{
		    	BufferedImage image = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
				Graphics g = image.getGraphics();
				Vector3d vec = new Vector3d();
				g.setClip(0, 0, image.getWidth(), image.getHeight());
				panelVisualization.updateGlobalPaintOffset();
				GraphicsDrawer gd = new GraphicsDrawer(g, 33);
				int count = 1;
				for (int i = 0; i < scene.surfaceObjectList.size(); ++i, ++count)
				{
					gd.setColor(new Color(count, count, count));
					drawSurface(scene.surfaceObjectList.get(i), gd, vec, panelVisualization.globalPaintOffset, scale);
				}
				float vertices[] = UniqueObjects.EMPTY_FLOAT_ARRAY;
				for (int i = 0; i < scene.volumeObjectList.size(); ++i, ++count)
				{
					gd.setColor(new Color(count, count, count));
					vertices = drawVolume(scene.volumeObjectList.get(i), gd, panelVisualization.globalPaintOffset, scale, vertices);
				}
				for (int i = 0; i < scene.meshObjectList.size(); ++i, ++count)
				{
					gd.setColor(new Color(count, count, count));
					vertices = drawMesh(scene.meshObjectList.get(i), gd, panelVisualization.globalPaintOffset, scale, vertices);
				}
				int mindist = Integer.MAX_VALUE;
				int rownumber = 0;
				ColorModel cm = image.getColorModel();
				for (int x = -10; x < 11; ++x)
				{
					for (int y = -10; y <11; ++y)
					{
						int dist = x * x + y * y;
						if (dist < mindist && x + ex >= 0 && x + ex < image.getWidth() && y + ey >= 0 && y + ey < image.getHeight())
						{
							int value = cm.getRed(image.getRGB(x + ex, y + ey));
							if (value != 0)
							{
								mindist = dist;
								rownumber = value;
							}
						}
					}
				}
				return rownumber-1;
	    	}
			catch(IOException e)
			{
				visibleErrorMessage("Unexpeted IO Error", e);
			}
	    	return -1;
	    }	
	
		@Override
		public void mouseExited(MouseEvent e) {}
		
		@Override
		public void mouseEntered(MouseEvent e) {}
		
		@Override
		public void mouseClicked(MouseEvent e) {
			if (!(advancedView&=selectedAdvanced >= 0))
			{
				if (tableSurfaces.getRowCount() != 0)
				{
					tableSurfaces.removeRowSelectionInterval(0, tableSurfaces.getRowCount()-1);
				}
				if (tableMeshes.getRowCount() != 0)
				{
					tableMeshes.removeRowSelectionInterval(0, tableMeshes.getRowCount()-1);
				}
				if (tableVolumes.getRowCount() != 0)
				{
					tableVolumes.removeRowSelectionInterval(0, tableVolumes.getRowCount()-1);
				}
			}
		}
		
		@Override
		public void mouseMoved(MouseEvent e) {}
		
		@Override
		public void mouseDragged(MouseEvent e) {
			if (selectedDrag >= 0)
			{
				if (originalPosition != null)
				{
					newPosition.set(originalPosition);
					newPosition.x += (e.getX() - startX) / scale;
					newPosition.y += (e.getY() - startY) / scale;
					try {
						if (selectedDrag < scene.surfaceObjectList.size())
						{
							scene.surfaceObjectList.get(selectedDrag).setValue(SCENE_OBJECT_COLUMN_TYPE.POSITION, newPosition, scene.vs, parser);
						}
						else if (selectedDrag < scene.surfaceObjectList.size() + scene.volumeObjectList.size())
						{
							int row = selectedDrag - scene.surfaceObjectList.size();
							scene.volumeObjectList.get(row).setValue(SCENE_OBJECT_COLUMN_TYPE.POSITION, newPosition, scene.vs, parser);							
						}
						else if (selectedDrag < scene.surfaceObjectList.size() + scene.volumeObjectList.size() + scene.meshObjectList.size())
						{
							int row = selectedDrag - scene.surfaceObjectList.size() - scene.volumeObjectList.size();
							scene.meshObjectList.get(row).setValue(SCENE_OBJECT_COLUMN_TYPE.POSITION, newPosition, scene.vs, parser);							
						}
					} catch (OperationParseException ex) {
						logger.error("Can't read math expression",ex);
					} catch (NumberFormatException ex) {
						logger.error("Can't read number", ex);
					}
				}	
				
				if (!Double.isNaN(originalGeometricRadius))
				{
					newGeometricRadius = originalGeometricRadius;
					newGeometricRadius += (e.getY() - startY) / scale;
					try {
						scene.surfaceObjectList.get(selectedDrag).setValue(SCENE_OBJECT_COLUMN_TYPE.MAXRADIUS, newGeometricRadius, scene.vs, parser);
					} catch (OperationParseException ex) {
						logger.error("Can't read math expression",ex);
					} catch (NumberFormatException ex) {
						logger.error("Can't read number", ex);
					}
				}
				
				if (originalDirection != null)
				{
					newDirection.set(originalDirection);
					newDirection.x += (e.getX() - startX) / scale;
					newDirection.y += (e.getY() - startY) / scale;
					try {
						scene.surfaceObjectList.get(selectedDrag).setValue(SCENE_OBJECT_COLUMN_TYPE.DIRECTION, newDirection, scene.vs, parser);
					} catch (OperationParseException ex) {
						logger.error("Can't read math expression",ex);
					} catch (NumberFormatException ex) {
						logger.error("Can't read number", ex);
					}
				}
			}
			if (selectedDrag < 0 && originalPaintOffset != null)
			{
				paintOffset.set(originalPaintOffset);
				paintOffset.x += (e.getX() - startX) / scale;
				paintOffset.y += (e.getY() - startY) / scale;
				repaint();
			}
		}
		
		@Override
		public void mouseWheelMoved(MouseWheelEvent e) {
			scale *= Math.exp(-0.02*e.getUnitsToScroll());
			repaint();
		}
		
		public VisualizationPanel() {
			setFocusable(true);
			addMouseListener(this);
			addMouseMotionListener(this);
			addMouseWheelListener(this);
			addKeyListener(this);
		}
		
		private double trajectory[] = UniqueObjects.EMPTY_DOUBLE_ARRAY;
		private final Vector3d v0 = new Vector3d();
		private final Rectangle bounds = new Rectangle();
		private final RaySimulationObject rayObject = new RaySimulationObject();
		public final Vector2d globalPaintOffset = new Vector2d();
		private final GraphicsDrawer gd = new GraphicsDrawer(null, 33);
		private float volumeVertices[] = UniqueObjects.EMPTY_FLOAT_ARRAY;
		private float endpos[] = UniqueObjects.EMPTY_FLOAT_ARRAY;
		private float enddir[] = UniqueObjects.EMPTY_FLOAT_ARRAY;
		private int bounces[] = UniqueObjects.EMPTY_INT_ARRAY;
		private byte accepted[] = UniqueObjects.EMPTY_BYTE_ARRAY;
		private float endpointColors[] = UniqueObjects.EMPTY_FLOAT_ARRAY;
		private OpticalObject endObject[] = OpticalObject.EMPTY_ARRAY;
		private final RayGenerator gen = new RayGenerator();
		private final StringBuilder strB = new StringBuilder();
		private final NearestPointCalculator npc = new NearestPointCalculator(3);
		
		public void paintComponent(Drawer gd) throws IOException
		{
			if (gd instanceof Drawer.GraphicsDrawer)
			{
				super.paintComponent(((Drawer.GraphicsDrawer)gd).getOutput());
			}
			scene.updateScene();
			updateGlobalPaintOffset();
			
			if (gd instanceof Drawer.GraphicsDrawer)
			{
				Graphics g = ((Drawer.GraphicsDrawer)gd).getOutput();
				for (int i = 0; i < scene.textureObjectList.size(); ++i)
				{
					GuiTextureObject current = scene.textureObjectList.get(i);
					double x = (current.midpoint.x + globalPaintOffset.x) * scale;
				 	double y = (current.midpoint.y + globalPaintOffset.y) * scale;
					if (current.active && !current.midpoint.containsNaN() && !current.direction.containsNaN() && current.image != null)
					{
						g.drawImage(current.image, (int)x, (int)y, (int)(x + current.direction.x * scale), (int)(y+ current.direction.y * scale), 0, 0, current.image.getWidth(), current.image.getHeight(), null);
					}
				}
			}
			for (int i = 0; i < scene.surfaceObjectList.size(); ++i)
			{
				GuiOpticalSurfaceObject current = scene.surfaceObjectList.get(i);
				gd.setColor(current.materialType == MaterialType.EMISSION ? (current.active ? Color.BLUE : EMISSION_INVISIBLE) : (current.active ? Color.BLACK : LENSE_INVISIBLE));
				drawSurface(current, gd, v0, globalPaintOffset, scale);
			}
			gd.setColor(Color.BLACK);
			for (int i = 0; i < scene.volumeObjectList.size(); ++i)
			{
				volumeVertices = drawVolume(scene.volumeObjectList.get(i), gd, globalPaintOffset, scale, volumeVertices);
			}
			for (int i = 0; i < scene.meshObjectList.size(); ++i)
			{
				MeshObject current = scene.meshObjectList.get(i);
				gd.setColor(current.active ? Color.BLACK : LENSE_INVISIBLE);
				volumeVertices = drawMesh(current, gd, globalPaintOffset, scale, volumeVertices);
			}
			int numTracedRays = 0;
			int maxTracedRays = 0;
			for (int i = 0; i < scene.getActiveLightCount(); ++i)
			{
				SurfaceObject goo = (SurfaceObject)scene.getActiveLight(i);
				numTracedRays += goo.numTracedRays * (goo.bidirectional ? 2 : 1);
				maxTracedRays = Math.max(maxTracedRays, goo.numTracedRays);
			}
			sceneObjectTrajectories.setSize((maxBounces + 2) * numTracedRays);
			sceneObjectTrajectories.setVertexColorActivated(true);
			final float sceneTrajectories[] = sceneObjectTrajectories.getVertices();
			final float sceneTrajectorieColors[] = sceneObjectTrajectories.getVertexColor();
			gd.getClipBounds(bounds);
			gd.setColor(LENSE_INVISIBLE);
			int sceneTrajectoryWriteIndex = 0;
			gen.threeDimensional = false;

			for (int i = 0; i < scene.getActiveLightCount(); ++i)
			{
				final SurfaceObject source = (SurfaceObject)scene.getActiveLight(i);
				gen.setSource(source);
				
				int bidirCount = source.numTracedRays * (source.bidirectional ? 2 : 1);
				if (trajectory.length < (maxBounces * 3 + 6) * bidirCount)
				{
					trajectory = new double[(maxBounces * 3 + 6) * bidirCount];
				}
				if (enddir.length < bidirCount * 3)
				{
					enddir = new float[bidirCount * 3];
					endpos = new float[bidirCount * 3];
					endpointColors = new float[bidirCount * 4];
					endObject = new OpticalObject[bidirCount];
				}
				if (accepted.length < source.numTracedRays)
				{
					accepted = new byte[source.numTracedRays];
					bounces = new int[source.numTracedRays];
				}
				Arrays.fill(trajectory, 0, (maxBounces * 3 + 6) * source.numTracedRays * (source.bidirectional ? 2 : 1), Double.NaN);
				scene.calculateRays(0, source.numTracedRays, source.numTracedRays, gen, 0, 0, null, null, endpos, enddir, endpointColors, trajectory, accepted, bounces, endObject, maxBounces, source.bidirectional, rayObject, RaytraceScene.UNACCEPTED_MARK);
				if (drawFocalpoints)
				{
					for (int j = 0; j < source.numTracedRays; ++j)
					{
						if (accepted[j] == RaytraceScene.STATUS_ACCEPTED)
						{
							npc.addPoint(endpos, enddir, j * 3);
						}
					}
					npc.calculate();
					gd.setColor(Color.BLACK);
					gd.drawArc((npc.get(0) + globalPaintOffset.x) * scale-5, (npc.get(1) + globalPaintOffset.y) * scale-5, 10, 10, 0, 360);
					npc.reset();
				}
				for (int j = 0, trajectoryIndex = 0; j < source.numTracedRays; ++j)
				{
					gd.setColor(accepted[j] == RaytraceScene.STATUS_ACCEPTED ? RAY_BLACK : RAY_RED);
					for (int dir = 0; dir < (source.bidirectional ? 2 : 1); ++dir)
					{
						v0.set(trajectory, trajectoryIndex);
						v0.add(globalPaintOffset);
						v0.multiply(scale);
						gd.pushPoint(v0.x, v0.y);
						for (int b = 3 + trajectoryIndex; b < maxBounces * 3 + 6 + trajectoryIndex; b += 3)
						{
							v0.set(trajectory, b);
							if (Double.isNaN(v0.x))
							{
								break;
							}
							v0.add(globalPaintOffset);
							v0.multiply(scale);
							gd.pushPoint(v0.x, v0.y);
						}
						gd.drawPolyLine();
						for (int b = trajectoryIndex; b < maxBounces * 3+6 + trajectoryIndex; b += 3)
						{
							sceneTrajectories[sceneTrajectoryWriteIndex*3] = (float)trajectory[b] * dScale;
							sceneTrajectories[sceneTrajectoryWriteIndex*3+1] = (float)trajectory[b + 1] * dScale;
							sceneTrajectories[sceneTrajectoryWriteIndex*3+2] = (float)trajectory[b + 2] * dScale;
							if (sceneTrajectorieColors != null)
							{
								if (accepted[j] == RaytraceScene.STATUS_ACCEPTED)
								{
									sceneTrajectorieColors[sceneTrajectoryWriteIndex*4] = endpointColors[j * 4];
									sceneTrajectorieColors[sceneTrajectoryWriteIndex*4 + 1] = endpointColors[j * 4 + 1];
									sceneTrajectorieColors[sceneTrajectoryWriteIndex*4 + 2] = endpointColors[j * 4 + 2];
									sceneTrajectorieColors[sceneTrajectoryWriteIndex*4 + 3] = 1;
								}
								else
								{
									sceneTrajectorieColors[sceneTrajectoryWriteIndex*4] = 0;
									sceneTrajectorieColors[sceneTrajectoryWriteIndex*4 + 1] = 0;
									sceneTrajectorieColors[sceneTrajectoryWriteIndex*4 + 2] = 0;
									sceneTrajectorieColors[sceneTrajectoryWriteIndex*4 + 3] = 1;
								}
							}
							++sceneTrajectoryWriteIndex;
							if (Double.isNaN(trajectory[b]) || Double.isNaN(trajectory[b + 1]) || Double.isNaN(trajectory[b + 2]))
							{
								break;
							}
						}
						trajectoryIndex += maxBounces * 3 + 6;
					}
				}
			}
			if (drawMeasure)
			{
				gd.setColor(Color.BLACK);
				int power = 0;
				int maxWidth = 200;
				double invscale = maxWidth / scale;
				if (invscale < 1)
				{
					while(getScale(power) >= invscale)
					{
						--power;
					}
				}
				else
				{
					while(getScale(power + 1) < invscale)
					{
						++power;
					}
				}
				double len = getScale(power) * scale * 0.5;
				double x0 = maxWidth / 2 + 10 - len;
				double x1 = maxWidth / 2 + 10 + len;
				gd.drawLine(x0, 20, x1, 20);
				gd.drawLine(x0, 10, x0, 30);
				gd.drawLine(x1, 10, x1, 30);
				strB.append(getScale(power));
				gd.drawChars(strB, 0, strB.length(), maxWidth / 2 - 10, 15);
				strB.setLength(0);
			}
			Arrays.fill(sceneTrajectories, sceneTrajectoryWriteIndex * 3, sceneTrajectories.length, Float.NaN);
			sceneObjectTrajectories.update(UpdateKind.DATA);
		}
		
		private double getScale(int pow)
		{
			pow += 3000000;
			int diff = pow / 3 - 1000000;
			switch (pow % 3)
			{
				case 0: return 1 * Calculate.pow(10., diff);
				case 1: return 2 * Calculate.pow(10., diff);
				case 2: return 5 * Calculate.pow(10., diff);
				default: throw new RuntimeException();
			}
		}
		
		@Override
		protected synchronized void paintComponent( Graphics g )
    	{
			try {
				gd.setOutput(g);
				paintComponent(gd);
	    	}catch (IOException e)
			{
				logger.error("Unexpected IO Error", e);
			}catch(Exception e)
			{
				logger.error("Unexpected Error", e);
			}
    	}

	    private void updateGlobalPaintOffset()
	    {
	    	globalPaintOffset.set(panelVisualization.getWidth() * 0.5, panelVisualization.getHeight() * 0.5);
	    	globalPaintOffset.multiply(1./scale);
	    	globalPaintOffset.add(paintOffset);
		}

		@Override
		public void keyPressed(KeyEvent e) {
			if (e.isControlDown())
			{
				switch (e.getKeyCode())
				{
					case KeyEvent.VK_C:copyClipboard();break;
					case KeyEvent.VK_X:cutClipboard();break;
					case KeyEvent.VK_V:pasteClipboard();break;
						
				}
			}
		}

		@Override
		public void keyReleased(KeyEvent e) {}

		@Override
		public void keyTyped(KeyEvent e) {}
    }
    
    /*private static void fill(Object[] row, GuiOpticalObject goo)
    {
    	for (int i = 0; i < COLUMN_TYPE.size(); ++i)
    	{
    		row[i] = goo.getValue(COLUMN_TYPE.get(i));
    	}
    }*/
    
	/*public static BufferedImage toBufferedImage(Image img)
	{
	    if (img instanceof BufferedImage)
	    {
	        return (BufferedImage) img;
	    }
	    BufferedImage bimage = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB);
	    Graphics2D bGr = bimage.createGraphics();
	    bGr.drawImage(img, 0, 0, null);
	    bGr.dispose();
	    return bimage;
	}*/

	
	private final RunnableRunner.RunnableObject untracedRayRunnable = new RunnableRunner.RunnableObject("UntracedRays", null){
		private float enddirs[] = UniqueObjects.EMPTY_FLOAT_ARRAY;
		private final RunnableRunner.ThreadLocal<RaySimulationObject> rso = DataHandler.runnableRunner.new ThreadLocal<RaySimulationObject>();
		private final AtomicInteger startPointIndexCount = new AtomicInteger(0);
		private final RayGenerator gen = new RayGenerator();
		private byte accepted[] = UniqueObjects.EMPTY_BYTE_ARRAY;
		private int bounces[] = UniqueObjects.EMPTY_INT_ARRAY;
		private OpticalObject endObject[] = OpticalObject.EMPTY_ARRAY;
		private final AtomicInteger notAcceptedCount = new AtomicInteger(0);
		ParallelRangeRunner prr = new ParallelRangeRunner();
		
		class ParallelRangeRunner implements ParallelRangeRunnable
		{
			boolean bidirectional = false;
			float endpoints[];
			float endpointColor[];
			int numUntracedRays;
			
			@Override
			public void run(int from, int to) {
				RaySimulationObject currentRay = rso.get();
				if (currentRay == null)
				{
					rso.set(currentRay = new RaySimulationObject());
				}
				int writeSize = (to - from) * (bidirectional ? 2 : 1);
				int endpointIndex = startPointIndexCount.getAndAdd(writeSize);
				notAcceptedCount.addAndGet(scene.calculateRays(from, to, numUntracedRays, gen, from, endpointIndex, null, null, endpoints, enddirs, endpointColor, null, accepted, bounces, endObject, maxBounces, bidirectional, currentRay, RaytraceScene.UNACCEPTED_RECALCULATE));
				DoubleMatrixUtil.multiply(endpoints, dScale, endpointIndex * 3, (endpointIndex + writeSize) * 3);
				//Arrays.fill(endpoints, acceptedCount * 3, (endpointIndex + writeSize) * 3, Float.NaN);
			}
			
			@Override
			public void finished() {}
		}
		
		@Override
		public void run(){
			scene.updateScene();
			scene.cameraViewRunnable.gto = scene.renderToTextureObject;
    		scene.cameraViewRunnable.gen.position.set(glScene.cameraPosition);
   			scene.cameraViewRunnable.gen.position.multiply(100);
   			scene.cameraViewRunnable.gen.rotation.set(glScene.cameraRotation);
			DataHandler.runnableRunner.run(scene.cameraViewRunnable, false);
    		try{
    			notAcceptedCount.set(0);
    			int numUntracedRays = 0;
    			int maxUntracedRays = 0;
    			for (int i = 0; i < scene.getActiveLightCount(); ++i)
    			{
    				SurfaceObject goo = (SurfaceObject)scene.getActiveLight(i);
    				numUntracedRays += goo.numUntracedRays * (goo.bidirectional ? 2 : 1);
    				maxUntracedRays = Math.max(maxUntracedRays, numUntracedRays);
    			}
    			if (enddirs.length != maxUntracedRays * 3)
    			{
    				enddirs = new float[maxUntracedRays * 3];
    				accepted = new byte[maxUntracedRays];
    				bounces = new int[maxUntracedRays];
    				endObject = new OpticalObject[maxUntracedRays];
    			}
    			sceneObjectEndpoints.setSize(numUntracedRays);
    			final float endpoints[] = sceneObjectEndpoints.getVertices();
    			final float endpointColor[] = sceneObjectEndpoints.getVertexColor();
    			
				gen.threeDimensional = true;
    			for (int i = 0; i < scene.getActiveLightCount(); ++i)
    			{
    				final SurfaceObject source = (SurfaceObject)scene.getActiveLight(i);
    				startPointIndexCount.set(0);
    				prr.bidirectional = source.bidirectional;
    				prr.numUntracedRays = source.numUntracedRays;
    				prr.endpoints = endpoints;
    				prr.endpointColor = endpointColor;
       				gen.setSource(source);
    				DataHandler.runnableRunner.runParallelAndWait(prr, "Raytrace", null, 0, source.numUntracedRays, 100000);
    			}
    			for (int i = 0; i < scene.textureObjectList.size(); ++i)
    			{
    				scene.textureObjectList.get(i).triggerModificationEvents();
    			}
    			labelAcceptedFraction.setText(Double.toString((numUntracedRays) / (double)(numUntracedRays + notAcceptedCount.get())));
    			sceneObjectEndpoints.update(UpdateKind.DATA);
            }catch (Exception e){
            	logger.error("Exception at tracing rays", e);
            }
        }
    };
	
	@Override
	public void dispose()
	{
		sceneObjectEndpoints.destroy();
		sceneObjectTrajectories.destroy();
		scene.clear();
		super.dispose();
	}

	@Override
	public void windowActivated(WindowEvent arg0) {}
	@Override
	public void windowClosed(WindowEvent arg0) {
		if (--DataHandler.openWindows == 0)
		{
			System.exit(0);
		}
	}

	@Override
	public void windowClosing(WindowEvent arg0) {}

	@Override
	public void windowDeactivated(WindowEvent arg0) {}

	@Override
	public void windowDeiconified(WindowEvent arg0) {}

	@Override
	public void windowIconified(WindowEvent arg0) {}

	@Override
	public void windowOpened(WindowEvent arg0) {}

	@Override
	public void dataChanged(OpticalObject source) {
		System.out.println("Changed: " + source);
		if (source instanceof GuiOpticalVolumeObject)
		{
			GuiOpticalVolumeObject current = (GuiOpticalVolumeObject)source;
			Object attachement = current.attachements.get(glObjectAttachementId);
			if (attachement != null)
			{	
				if (attachement instanceof SceneObjectMesh)
				{
					SceneObjectMesh currentMesh = (SceneObjectMesh)attachement;
					float vertices[] = currentMesh.getVertices();
					
					currentMesh.setDrawType(SceneObject.DrawType.SOLID);
					vertices = ArrayUtil.setToLength(vertices, current.numMeshVertices() * 3);
					current.getMeshVertices(vertices);
					int faces[] = current.getMeshFaces(currentMesh.getFaces());
					DoubleMatrixUtil.multiply(vertices, dScale, 0, vertices.length);
		    		currentMesh.lightMaterial.set(true, true, true, false, current.color);
		    		currentMesh.reflectionMaterial.set(false, false, false, true, current.color);
		    		currentMesh.setData(vertices, faces);
		    		currentMesh.update(UpdateKind.DATA);
				}
				else if (attachement instanceof SceneObjectLine)
				{
					SceneObjectLine line = (SceneObjectLine)attachement;
					float vertices[] = current.getVolumeVertices(line.getVertices());
					DoubleMatrixUtil.multiply(vertices, dScale, 0, vertices.length);
					float colors[] = current.getVolumeColor(line.getVertexColor());
					line.setData(vertices, colors);
					line.setVisible(true);
					line.update(UpdateKind.DATA);
					line.setDrawType(SceneObject.DrawType.DOTS);
				}
			}
			panelVisualization.repaint();
			if (panelVisualization != currentVisualization)
			{
				currentVisualization.repaint();
			}
			DataHandler.runnableRunner.run(untracedRayRunnable, false);
		}
		else if (source instanceof MeshObject)
		{
			MeshObject current = (MeshObject)source;
			Object attachement = current.attachements.get(glObjectAttachementId);
			SceneObjectMesh currentMesh = (SceneObjectMesh)attachement;
			float vertices[] = currentMesh.getVertices();
			vertices = ArrayUtil.setToLength(vertices, current.getMeshVertexLength());
			current.getMeshVertices(vertices);
			int faces[] = current.getMeshFaces();
			DoubleMatrixUtil.multiply(vertices, dScale, 0, vertices.length);
			currentMesh.setData(vertices, faces);
    		currentMesh.update(UpdateKind.DATA);

		}
	}
}
