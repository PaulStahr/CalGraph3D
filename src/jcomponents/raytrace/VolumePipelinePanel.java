package jcomponents.raytrace;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;

import data.DataHandler;
import data.ProgramIcons;
import data.raytrace.OpticalVolumeObject;
import data.raytrace.RaytraceScene;
import data.raytrace.RaytraceScene.SceneChangeListener;
import data.raytrace.VolumePipeline;
import data.raytrace.VolumePipeline.CalculationCalcuationStep;
import data.raytrace.VolumePipeline.CalculationStep;
import data.raytrace.VolumePipeline.GenerationCalculationStep;
import jcomponents.util.JMathTextField;
import util.JFrameUtils;

public class VolumePipelinePanel extends JPanel implements ActionListener, SceneChangeListener, AncestorListener, ItemListener{
	/**
	 * 
	 */
	//private static final Logger logger = LoggerFactory.getLogger(VolumePipelinePanel.class);
	private static final long serialVersionUID = -4575960474407342758L;
	private final JComboBox<OpticalVolumeObject> volumes = new JComboBox<>();
	private final JPanel pipelinePanel = new JPanel();
	private final JButton buttonAddCalculationStep = new JButton("Add Calculation Step");
	private final JButton buttonAddSolveStep = new JButton("Add Solve Step");
	private final JButton buttonCalculate = new JButton("Calculate");
	private final JButton buttonSave = new JButton("Save");
	private final JCheckBox checkBoxAutoUpdate = new JCheckBox("Auto update");
	private final JCheckBox checkBoxRunAtStartup = new JCheckBox("Update at startup");
	public final VolumePipeline pipeline;
    
    private final Runnable updateRunnable = new Runnable() {
		
		@Override
		public void run() {
			if (EventQueue.isDispatchThread())
			{	
				synchronized(this)
				{
					JFrameUtils.compareAndSetEnabled(buttonCalculate, !pipeline.isCalculating());
					int currentCalculatingStep = pipeline.getCurrentCalculatingStep();
					for (int i = 0; i < pipeline.steps.size(); ++i)
					{
						Color col = i < currentCalculatingStep ? Color.GREEN : i == currentCalculatingStep ? Color.YELLOW : Color.RED;
						CalculationStep step = pipeline.steps.get(i);
						if (i < pipelinePanel.getComponentCount())
						{
							Component comp = pipelinePanel.getComponent(i);
							if (comp instanceof CalculationPipelineStepPanel && step instanceof CalculationCalcuationStep)
							{
								CalculationPipelineStepPanel spsp = (CalculationPipelineStepPanel)comp;
								CalculationCalcuationStep ccs = (CalculationCalcuationStep)step; 
								spsp.set(ccs);
								JFrameUtils.compareAndSetBackground(comp, col);
								continue;
							}
							if (comp instanceof GeneratePipelineStepPanel && step instanceof GenerationCalculationStep)
							{
								GeneratePipelineStepPanel spsp = (GeneratePipelineStepPanel)comp;
								GenerationCalculationStep ccs = (GenerationCalculationStep)step; 
								spsp.set(ccs);
								JFrameUtils.compareAndSetBackground(comp, col);
								continue;
							}
						}
						Component comp = null;
						if (step instanceof CalculationCalcuationStep)
						{
							CalculationCalcuationStep ccs = (CalculationCalcuationStep)step; 
							comp = new CalculationPipelineStepPanel(ccs);
						}
						if (step instanceof GenerationCalculationStep)
						{
							GenerationCalculationStep ccs = (GenerationCalculationStep)step; 
							comp = new GeneratePipelineStepPanel(ccs);
						}
						JFrameUtils.compareAndSetBackground(comp, col);
						while (i < pipelinePanel.getComponentCount())
						{
							pipelinePanel.remove(i);
						}
						pipelinePanel.add(comp);
					}
					pipelinePanel.revalidate();
					JFrameUtils.compareAndSetSelectedItem(volumes,pipeline.ovo);
					JFrameUtils.compareAndSetSelected(checkBoxAutoUpdate, pipeline.getAutoUpdate());
					JFrameUtils.compareAndSetSelected(checkBoxRunAtStartup, pipeline.calcuteAtCreation);
				}
			}
			else
			{
				EventQueue.invokeLater(this);
			}
		}
	};
	
	public OpticalVolumeObject getSelectedVolume()
    {
    	return (OpticalVolumeObject)volumes.getSelectedItem();
    }
    
    public void setSelectedVolume(OpticalVolumeObject volume)
    {
    	volumes.setSelectedItem(volume);
    }
	
	public VolumePipelinePanel(RaytraceScene scene)
	{
		pipeline = new VolumePipeline(scene);
		updateVolumes();
		scene.add(this);
		GroupLayout layout = new GroupLayout(this);
		this.setLayout(layout);
		
		layout.setHorizontalGroup(layout.createParallelGroup().addGroup(
				layout.createSequentialGroup().addComponent(volumes).addComponent(buttonAddCalculationStep).addComponent(buttonAddSolveStep).addComponent(buttonCalculate).addComponent(buttonSave).addComponent(checkBoxRunAtStartup).addComponent(checkBoxAutoUpdate)).addComponent(pipelinePanel));
		layout.setVerticalGroup(layout.createSequentialGroup().addGroup(
				layout.createParallelGroup().addComponent(volumes).addComponent(buttonAddCalculationStep).addComponent(buttonAddSolveStep).addComponent(buttonCalculate).addComponent(buttonSave).addComponent(checkBoxRunAtStartup).addComponent(checkBoxAutoUpdate)).addComponent(pipelinePanel));
		buttonAddCalculationStep.addActionListener(this);
		buttonCalculate.addActionListener(this);
		buttonAddSolveStep.addActionListener(this);
		checkBoxAutoUpdate.addItemListener(this);
		buttonSave.addActionListener(this);
		pipelinePanel.setLayout(JFrameUtils.SINGLE_COLUMN_LAYOUT);
		addAncestorListener(this);
		setMinimumSize(new Dimension(100, 50));
		pipeline.addListener(updateRunnable);
		volumes.addItemListener(this);
	}
	
	public void addStep(PipelineStepPanel step)
	{
		pipelinePanel.add(step);
		pipelinePanel.revalidate();
	}
	
	@Override
	public void itemStateChanged(ItemEvent arg0) {
		Object source = arg0.getSource();
		if (Thread.holdsLock(this)) {return;}
		synchronized(this)
		{
			if (source == checkBoxAutoUpdate)			{pipeline.setAutoUpdate(checkBoxAutoUpdate.isSelected());}
			else if (source == checkBoxRunAtStartup)	{pipeline.calcuteAtCreation = checkBoxRunAtStartup.isSelected();}
			else if (source == volumes)					{pipeline.ovo = (OpticalVolumeObject)volumes.getSelectedItem();}
		}
	}
	
	void updatePipeline()
	{
		pipeline.steps.clear();
		for (int i = 0; i < pipelinePanel.getComponentCount(); ++i)
		{
			Component comp = pipelinePanel.getComponent(i);
			if (comp instanceof CalculationPipelineStepPanel)
			{
				CalculationPipelineStepPanel panel = (CalculationPipelineStepPanel)comp;
				pipeline.steps.add(new CalculationCalcuationStep(panel.textFieldFormularIOR.getText(), panel.textFieldFormularTranslucency.getText(), panel.textFieldFormularGivenValues.getText(), panel.textFieldFormularIsGiven.getText()));
			}
			else if (comp instanceof GeneratePipelineStepPanel)
			{
				GeneratePipelineStepPanel panel = (GeneratePipelineStepPanel)comp;
				pipeline.steps.add(new GenerationCalculationStep(panel.textFieldFormularSize.getText()));
			}
		}
	}
	
	public class PipelineStepPanel extends JPanel implements MouseListener
	{
		/**
		 * 
		 */
		private static final long serialVersionUID = 4983281394482169139L;
		private JLabel labelUp = new JLabel(ProgramIcons.iconUp);
		private JLabel labelDown = new JLabel(ProgramIcons.iconDown);
		private JLabel labelDelete = new JLabel(ProgramIcons.iconDelete);
		
		public PipelineStepPanel()
		{
			setLayout(new BoxLayout(this,BoxLayout.X_AXIS));
			add(labelUp);
			add(labelDown);
			add(labelDelete);
			labelDown.addMouseListener(this);
			labelUp.addMouseListener(this);
			labelDelete.addMouseListener(this);
		}

		@Override
		public void mouseClicked(MouseEvent arg0) {
			Object source = arg0.getSource();
			if (source == labelUp)
			{
				int order = pipelinePanel.getComponentZOrder(this) - 1;
				if (order >= 0)
				{
					pipelinePanel.setComponentZOrder(this, order);
					pipelinePanel.revalidate();
					updatePipeline();
				}
			}
			else if(source == labelDown)
			{
				int order = pipelinePanel.getComponentZOrder(this) + 1;
				if (order < pipelinePanel.getComponentCount())
				{
					pipelinePanel.setComponentZOrder(this, order);
					pipelinePanel.revalidate();
					updatePipeline();
				}
			}
			else if (source == labelDelete)
			{
				pipelinePanel.remove(this);
				pipelinePanel.revalidate();
				updatePipeline();
			}
		}

		@Override
		public void mouseEntered(MouseEvent arg0) {}

		@Override
		public void mouseExited(MouseEvent arg0) {}

		@Override
		public void mousePressed(MouseEvent arg0) {}

		@Override
		public void mouseReleased(MouseEvent arg0) {}
	}
	
	
	
	public class CalculationPipelineStepPanel extends PipelineStepPanel
	{
		private static final long serialVersionUID = 8916693145915958928L;
		public final JMathTextField textFieldFormularIOR = new JMathTextField();
		public final JMathTextField textFieldFormularTranslucency = new JMathTextField();
		public final JMathTextField textFieldFormularGivenValues = new JMathTextField();
		public final JMathTextField textFieldFormularIsGiven = new JMathTextField();
		
		public CalculationPipelineStepPanel()
		{
			add(textFieldFormularIOR);
			add(textFieldFormularTranslucency);
			add(textFieldFormularGivenValues);
			add(textFieldFormularIsGiven);
			
		}
		
		
		public void set(CalculationCalcuationStep ccs) {
			JFrameUtils.compareAndSetText(textFieldFormularIOR, ccs.ior);
			JFrameUtils.compareAndSetText(textFieldFormularTranslucency, ccs.translucency);
			JFrameUtils.compareAndSetText(textFieldFormularIsGiven, ccs.isGiven);
			JFrameUtils.compareAndSetText(textFieldFormularGivenValues, ccs.givenValues);
		}


		public CalculationPipelineStepPanel(String formularIOR, String formularTranslucency, String formularGivenValues, String formularIsGiven)
		{
			this();
			textFieldFormularIOR.setText(formularIOR);
			textFieldFormularTranslucency.setText(formularTranslucency);
			textFieldFormularGivenValues.setText(formularGivenValues);
			textFieldFormularIsGiven.setText(formularIsGiven);
			
			//textFieldFormular.setPreferredSize(new Dimension(1000, textFieldFormular.getPreferredSize().height));
		}

		public CalculationPipelineStepPanel(CalculationCalcuationStep ccs) {
			this();
			set(ccs);
		}
	}
	
	public class GeneratePipelineStepPanel extends PipelineStepPanel
	{
		private static final long serialVersionUID = -5723507440204688606L;
		public final JMathTextField textFieldFormularSize = new JMathTextField();

		public GeneratePipelineStepPanel()
		{
			add(textFieldFormularSize);
		}
		
		public GeneratePipelineStepPanel(String formular)
		{
			this();
			textFieldFormularSize.setText(formular);
		}

		public GeneratePipelineStepPanel(GenerationCalculationStep ccs) {
			this();
			set(ccs);
		}

		private void set(GenerationCalculationStep ccs) {
			JFrameUtils.compareAndSetText(textFieldFormularSize, ccs.size);
		}
	}
	@Override
	public void actionPerformed(ActionEvent arg0) {
		Object source = arg0.getSource();
		if (source == buttonAddCalculationStep)				{pipelinePanel.add(new CalculationPipelineStepPanel());	pipelinePanel.revalidate();}
		else if (source == buttonAddSolveStep)				{pipelinePanel.add(new GeneratePipelineStepPanel());	pipelinePanel.revalidate();}
		else if (source == buttonCalculate)					{pipeline.updateVariableIds();DataHandler.runnableRunner.run(pipeline, "Volume Pipeline");}
		else if (source == buttonSave)						{updatePipeline();}
	}
	
	private void updateVolumes()
	{
		Object current = volumes.getSelectedItem();
		volumes.setModel(new DefaultComboBoxModel<OpticalVolumeObject>(pipeline.scene.volumeObjectList.toArray(new OpticalVolumeObject[pipeline.scene.volumeObjectList.size()])));
		volumes.setSelectedItem(current);		
	}
	
	@Override
	public void valueChanged(byte ct, Object o) {
		if (ct == RaytraceScene.OBJECT_ADD || ct == RaytraceScene.OBJECT_REMOVE)
		{
			updateVolumes();
		}
	}
	@Override
	public void ancestorAdded(AncestorEvent arg0) {}
	@Override
	public void ancestorMoved(AncestorEvent arg0) {}
	@Override
	public void ancestorRemoved(AncestorEvent arg0) {
		pipeline.scene.remove(this);
	}

	public PipelineStepPanel[] getSteps() {
		return JFrameUtils.getComponents(pipelinePanel, new PipelineStepPanel[pipelinePanel.getComponentCount()]);
	}
}
