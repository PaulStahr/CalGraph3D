package jcomponents.raytrace;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.Random;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToggleButton;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import data.raytrace.GuiOpticalSurfaceObject;
import data.raytrace.GuiOpticalSurfaceObject.OpticalSurfaceObjectChangeListener;
import data.raytrace.OpticalObject;
import data.raytrace.OpticalObject.SCENE_OBJECT_COLUMN_TYPE;
import data.raytrace.OpticalSurfaceObject;
import data.raytrace.RaytraceScene;
import data.raytrace.RaytraceScene.RaySimulationObject;
import data.raytrace.RaytraceScene.SceneChangeListener;
import data.raytrace.raygen.RayGenerator;
import geometry.Vector2d;
import geometry.Vector3d;
import jcomponents.util.JMathTextField;
import maths.Operation;
import maths.algorithm.Calculate;
import maths.data.RealDoubleOperation;
import util.data.UniqueObjects;

public class ErrorAnalysisFrame extends JFrame implements SceneChangeListener, ActionListener, DocumentListener,ItemListener, OpticalSurfaceObjectChangeListener, Runnable, WindowListener{
	private static final long serialVersionUID = -6872553544701310740L;
	
	private static class ArrowWindow extends JComponent
	{
		private static final long serialVersionUID = 8731322375600136675L;
		private final Rectangle clipBounds = new Rectangle();
		private float positions[] = UniqueObjects.EMPTY_FLOAT_ARRAY;
		private static final BasicStroke stroke = new BasicStroke(2);
		public double visualizationScale = Double.NaN;
		
		public ArrowWindow()
		{
			setBorder(lineBorder);
		}
		
		@Override
		public void paintComponent(Graphics gr)
		{
			Graphics2D g = (Graphics2D)gr;
			super.paintComponent(g);
			g.getClipBounds(clipBounds);
			g.setColor(Color.BLACK);
			g.fill(clipBounds);
			double centerX = clipBounds.getCenterX();
			double centerY = clipBounds.getCenterY();
			g.setStroke(stroke);
			
			double mult = visualizationScale;
			if (Double.isNaN(visualizationScale))
			{
				float min= positions[Calculate.min(positions)];
				float max = positions[Calculate.max(positions)];
				float range = Math.max(max, -min);
				float compRange = Math.min(clipBounds.width, clipBounds.height);
				mult = (compRange - 10) / range;
			}
			for (int i = 0; i < positions.length; i+=2)
			{
				g.setColor(colors[Math.min(i/2, colors.length - 1)]);
				int x = (int)(centerX + positions[i] * mult) - 5;
				int y = (int)(centerY + positions[i + 1] * mult) - 5;
				x = Math.max(clipBounds.x, Math.min(x, clipBounds.width + clipBounds.x - 5));
				y = Math.max(clipBounds.y, Math.min(y, clipBounds.height + clipBounds.y - 5));
				g.fillOval(x, y, 10, 10);
			}
		}
	}
	
	
	private final JLabel labelChooseSource = new JLabel("Source");
	private final JComboBox<GuiOpticalSurfaceObject> comboBoxSource = new JComboBox<>();
	private final JLabel labelEpsilon = new JLabel("Epsilon");
	private final JMathTextField textFieldEpsilon = new JMathTextField();
	private final JTable tableValues = new JTable();
	private final RaytraceScene scene;
	private final JToggleButton toggleButtonUseSurfaceCoordinates = new JToggleButton("Surface Coordinates");
	private final JToggleButton toggleButtonAutoUpdate = new JToggleButton("Auto Update");
	private final JButton buttonUpdate = new JButton("Update");
	private final JToggleButton toggleButtonAbsoluteDastances = new JToggleButton("Absolute Distances");
	private final JToggleButton toggleButtonRelative = new JToggleButton("Relative");
	private final JToggleButton toggleButtonLogarithmicPlot = new JToggleButton("Logarithmic");
	private final JLabel labelEvaluationMethod = new JLabel("Evaluation Method");
	private final JComboBox<String> comboBoxEvaluationMethod = new JComboBox<String>(new String[] {"One", "Avarage", "Maximum"});
	private final JLabel labelVisualizationScale = new JLabel("Scale");
	private final JMathTextField textFieldVisualizationScale = new JMathTextField("1");
	private RaySimulationObject rso = new RaySimulationObject();
	private RayGenerator gen = new RayGenerator();
	private JScrollPane scrollPanelValues = new JScrollPane(tableValues);
	private ArrowWindow arrowWindows[] = new ArrowWindow[0];
	private final JPanel panelArrowWindows = new JPanel();
	private static final Border lineBorder = BorderFactory.createLineBorder(Color.WHITE);
	private static final Color colors[] = new Color[] {Color.RED, Color.BLUE, Color.GREEN, Color.CYAN, Color.ORANGE, Color.MAGENTA, Color.PINK, Color.YELLOW, Color.WHITE, Color.GRAY};
	
	public ErrorAnalysisFrame(final RaytraceScene scene)
	{
		super("Error Analysis");
		panelArrowWindows.setLayout(new GridLayout(3,0));
		GroupLayout layout = new GroupLayout(getContentPane());
		getContentPane().setLayout(layout);
		
		layout.setHorizontalGroup(
			layout.createParallelGroup().addGroup(
				layout.createSequentialGroup()
				.addGroup(layout.createParallelGroup().addComponent(labelChooseSource).addComponent(labelEpsilon))
				.addGroup(layout.createParallelGroup().addComponent(comboBoxSource).addComponent(textFieldEpsilon))
				.addGroup(layout.createParallelGroup().addComponent(labelEvaluationMethod).addComponent(labelVisualizationScale))
				.addGroup(layout.createParallelGroup().addComponent(comboBoxEvaluationMethod).addComponent(textFieldVisualizationScale)))
			.addGroup(layout.createSequentialGroup().addComponent(toggleButtonAbsoluteDastances).addComponent(toggleButtonRelative).addComponent(toggleButtonUseSurfaceCoordinates).addComponent(toggleButtonLogarithmicPlot).addComponent(toggleButtonAutoUpdate).addComponent(buttonUpdate))
			.addComponent(scrollPanelValues).addComponent(panelArrowWindows));
		
		layout.setVerticalGroup(
			layout.createSequentialGroup()
			.addGroup(layout.createParallelGroup().addComponent(labelChooseSource).addComponent(comboBoxSource, 20, 20, 20).addComponent(labelEvaluationMethod).addComponent(comboBoxEvaluationMethod, 20, 20, 20))
			.addGroup(layout.createParallelGroup().addComponent(labelEpsilon).addComponent(textFieldEpsilon, 20, 20, 20).addComponent(labelVisualizationScale).addComponent(textFieldVisualizationScale, 20, 20, 20))
			.addGroup(layout.createParallelGroup().addComponent(toggleButtonAbsoluteDastances).addComponent(toggleButtonRelative).addComponent(toggleButtonUseSurfaceCoordinates).addComponent(toggleButtonLogarithmicPlot).addComponent(toggleButtonAutoUpdate).addComponent(buttonUpdate))
			.addComponent(scrollPanelValues, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE).addComponent(panelArrowWindows));
		textFieldEpsilon.getDocument().addDocumentListener(this);
		textFieldEpsilon.setText("0.00001");
		pack();
		setMaximumSize(getSize());
		this.scene = scene;
		scene.add(this);
		textFieldVisualizationScale.getDocument().addDocumentListener(this);
		buttonUpdate.addActionListener(this);
		comboBoxSource.addItemListener(this);
		toggleButtonUseSurfaceCoordinates.addActionListener(this);
		toggleButtonLogarithmicPlot.addActionListener(this);
		addWindowListener(this);
		scene.addObjectChangeListener(this);
		valueChanged((byte)-1, null);
		toggleButtonRelative.addActionListener(this);
		toggleButtonAbsoluteDastances.addActionListener(this);
		gen.rand = new Random();
		
		
	}
	
	private static class CellRenderer extends DefaultTableCellRenderer{
		private static final long serialVersionUID = 1730863456952577338L;

		{
		    // you need to set it to opaque
		    setOpaque(true);
		  }

		@Override
		public Component getTableCellRendererComponent(final JTable table,  final Object value, final boolean isSelected, final boolean hasFocus,  final int row, final int column) {
			Component tmp =  super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			if (column == 0)
			{
				tmp.setBackground(colors[Math.min(row, colors.length - 1)]);
			}
			else
			{
				tmp.setBackground(UIManager.getColor("TableHeader.background"));
			}
			return tmp;
		  }
	}
	
	@Override
	public void run()
	{
		String headlines[] = new String[] {"Object", "x", "y", "z", "len", "dirx", "diry", "dirz", "conic", "ior0", "ior1"};
		Object data[][] = new Object[scene.getActiveSurfaceCount() + 1][headlines.length];
		double endpoints[] = new double[3];
		float enddirs[] = new float[3];
		float[] sceneEndpointColor = new float[4];
		byte accepted[] = new byte[1];
		int bounces[] = new int[1];
		gen.setSource((GuiOpticalSurfaceObject)comboBoxSource.getSelectedItem());
		if (gen.getSource() == null)
		{
			return;
		}
		Vector3d endpoint = new Vector3d();
		boolean useSurfaceCoordinates = toggleButtonUseSurfaceCoordinates.isSelected();
		OpticalObject oo[] = new OpticalObject[1];
		scene.calculateRays(0, 1, 1, gen, 0, 0, null, null, endpoints, enddirs, sceneEndpointColor, null, accepted, bounces, oo, 10, false, rso, RaytraceScene.UNACCEPTED_MARK);
		boolean absoluteDistances = toggleButtonAbsoluteDastances.isSelected();
		boolean relative = toggleButtonRelative.isSelected();
		Vector2d coord = new Vector2d();
		int seed = (int)(Math.random() * Integer.MAX_VALUE);
		gen.rand.setSeed(seed);
		Operation scaleOp = textFieldVisualizationScale.get();
		double visualizationScale = scaleOp == null ? Double.NaN : scaleOp.doubleValue();
		if (useSurfaceCoordinates)
		{
			if (oo[0] == null)
			{
				endpoint.set(Double.NaN, Double.NaN,Double.NaN);
			}
			else
			{
				((OpticalSurfaceObject) oo[0]).getTextureCoordinates(rso.position, rso.direction, coord);
				endpoint.set(coord);
			}
		}
		else
		{
			endpoint.set(rso.position);
		}
		Vector3d tmpEndpoint = new Vector3d();
		
		Operation op = textFieldEpsilon.get();
		if (op == null)
		{
			return;
		}
		if (arrowWindows.length != headlines.length - 1)
		{
			panelArrowWindows.removeAll();
			arrowWindows = new ArrowWindow[headlines.length - 1];
			for (int i = 0; i < arrowWindows.length; ++i)
			{
				arrowWindows[i] = new ArrowWindow();
				panelArrowWindows.add(arrowWindows[i]);
			}
		}
		double epsilon = op.doubleValue();
		for (int j = 0; j < arrowWindows.length; ++j)
		{
			if (arrowWindows[j].positions.length != scene.getActiveSurfaceCount() * 2 + 2)
			{
				arrowWindows[j].positions = new float[scene.getActiveSurfaceCount() * 2 + 2];
			}
			arrowWindows[j].visualizationScale = visualizationScale;
		}
		for (int i = 0; i <= scene.getActiveSurfaceCount(); ++i)
		{
			GuiOpticalSurfaceObject current = (GuiOpticalSurfaceObject)(i ==scene.getActiveSurfaceCount() ? gen.getSource() : scene.getActiveSurface(i));
			data[i][0] = current.id;
			for (int j = 1; j < headlines.length; ++j)
			{
				Object originalValue = null;
				switch(j)
				{
				case 1:originalValue = current.midpoint.x;current.midpoint.x += epsilon;break;
				case 2:originalValue = current.midpoint.y;current.midpoint.y += epsilon;break;
				case 3:originalValue = current.midpoint.z;current.midpoint.z += epsilon;break;
				case 4:originalValue = new Vector3d(current.direction);current.direction.multiply(1 + (relative ? epsilon : current.invDirectionLength * epsilon));break;
				case 5:originalValue = current.direction.x;current.direction.x += relative ? current.directionLength * epsilon : epsilon;break;
				case 6:originalValue = current.direction.y;current.direction.y += relative ? current.directionLength * epsilon : epsilon;break;
				case 7:originalValue = current.direction.z;current.direction.z += relative ? current.directionLength * epsilon : epsilon;break;
				case 8:originalValue = current.conicConstant;current.conicConstant += relative ? current.conicConstant * epsilon : epsilon;break;
				case 9:originalValue = current.ior0;current.ior0 = new RealDoubleOperation(current.ior0.doubleValue() + (relative ? current.ior0.doubleValue() * epsilon : epsilon));break;
				case 10:originalValue = current.ior1;current.ior1 = new RealDoubleOperation(current.ior1.doubleValue() + (relative ? current.ior1.doubleValue() * epsilon : epsilon));break;
				}
				current.update();
								
				gen.rand.setSeed(seed);
				scene.calculateRays(0, 1, 1, gen, 0, 0,null, null, endpoints, enddirs, sceneEndpointColor, null, accepted, bounces, oo, 10, false, rso, RaytraceScene.UNACCEPTED_MARK);
				if (useSurfaceCoordinates)
				{
					if (oo[0] == null)
					{
						endpoint.set(Double.NaN, Double.NaN,Double.NaN);
					}
					else
					{
						((OpticalSurfaceObject) oo[0]).getTextureCoordinates(rso.position, rso.direction, coord);
						tmpEndpoint.set(coord);
					}
				}
				else
				{
					tmpEndpoint.set(rso.position);
				}
				
				tmpEndpoint.sub(endpoint);
				tmpEndpoint.multiply(1 / epsilon);
				
				if (toggleButtonLogarithmicPlot.isSelected())
				{
					arrowWindows[j - 1].positions[i*2] = (float)(Math.log(1 + Math.abs(tmpEndpoint.x)) * Math.signum(tmpEndpoint.x));
					arrowWindows[j - 1].positions[i*2 + 1] = (float)(Math.log(1 + Math.abs(tmpEndpoint.y)) * Math.signum(tmpEndpoint.y));	
				}
				else
				{
					arrowWindows[j - 1].positions[i*2] = (float)(tmpEndpoint.x);
					arrowWindows[j - 1].positions[i*2 + 1] = (float)(tmpEndpoint.y);
				}
				
				data[i][j] = absoluteDistances ? tmpEndpoint.norm() : tmpEndpoint.toString();
				switch(j)
				{
					case  1:current.midpoint.x = (double)originalValue;break;
					case  2:current.midpoint.y = (double)originalValue;break;
					case  3:current.midpoint.z = (double)originalValue;break;
					case  4:current.direction.set((Vector3d)originalValue);break;
					case  5:current.direction.x = (double)originalValue;break;
					case  6:current.direction.y = (double)originalValue;break;
					case  7:current.direction.z = (double)originalValue;break;
					case  8:current.conicConstant = (double)originalValue;break;
					case  9:current.ior0 = (Operation)originalValue;break;
					case 10:current.ior1 = (Operation)originalValue;break;
				}
				current.update();
			}
		}
		tableValues.setModel(new DefaultTableModel(data, headlines));
		CellRenderer cr = new CellRenderer();
		for (int i = 0; i < tableValues.getColumnCount(); ++i)
		{
			tableValues.getColumnModel().getColumn(i).setCellRenderer(cr);
		}
		Dimension dim = tableValues.getPreferredSize();
		scrollPanelValues.setPreferredSize(new Dimension(dim.width, dim.height + tableValues.getTableHeader().getPreferredSize().height + 8));
		panelArrowWindows.repaint();
	}
	
	private void update()
	{
		EventQueue.invokeLater(this);
	}

	@Override
	public void valueChanged(byte ct, Object o) {
		if (ct == -1 || (ct == RaytraceScene.OBJECT_ADD || ct == RaytraceScene.OBJECT_REMOVE && o instanceof OpticalSurfaceObject))
		{
			Object selected = comboBoxSource.getSelectedItem();
			GuiOpticalSurfaceObject oso[] = new GuiOpticalSurfaceObject[scene.getSurfaceCount()];
			for (int i = 0; i < oso.length; ++i)
			{
				oso[i] = scene.getSurfaceObject(i);
			}
			comboBoxSource.setModel(new DefaultComboBoxModel<GuiOpticalSurfaceObject>(oso));
			comboBoxSource.setSelectedItem(selected);
		}
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		Object source = arg0.getSource();
		if (source == buttonUpdate)
		{
			update();
		}
		else if (toggleButtonAutoUpdate.isSelected())
		{
			update();
		}
	}
	
	public void documentUpdate(DocumentEvent arg)
	{
		if (toggleButtonAutoUpdate.isSelected())
		{
			update();
		}
	}

	@Override
	public void changedUpdate(DocumentEvent arg0) {documentUpdate(arg0);}

	@Override
	public void insertUpdate(DocumentEvent arg0) {documentUpdate(arg0);}

	@Override
	public void removeUpdate(DocumentEvent arg0) {documentUpdate(arg0);}

	@Override
	public void itemStateChanged(ItemEvent arg0) {
		if (toggleButtonAutoUpdate.isSelected())
		{
			update();
		}
	}

	@Override
	public void valueChanged(GuiOpticalSurfaceObject object, SCENE_OBJECT_COLUMN_TYPE ct) {
		if (toggleButtonAutoUpdate.isSelected())
		{
			update();
		}
	}

	@Override
	public void windowActivated(WindowEvent arg0) {}

	@Override
	public void windowClosed(WindowEvent arg0) {
		scene.remove(this);
       	scene.removeObjectChangeListener(this);
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
}
