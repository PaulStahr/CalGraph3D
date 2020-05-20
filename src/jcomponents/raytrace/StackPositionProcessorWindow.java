package jcomponents.raytrace;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.DefaultComboBoxModel;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.swing.JToggleButton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import data.DataHandler;
import data.raytrace.GuiOpticalSurfaceObject;
import data.raytrace.GuiTextureObject;
import data.raytrace.OpticalObject;
import data.raytrace.RaytraceScene;
import data.raytrace.RaytraceSession;
import data.raytrace.StackPositionProcessor;
import data.raytrace.StackPositionProcessor.Mode;
import data.raytrace.SurfaceObject;
import data.raytrace.components.SurfaceComboBox;
import data.raytrace.components.TextureComboBox;
import geometry.Rotation3;
import geometry.Vector3f;
import jcomponents.Interface;
import jcomponents.util.JMathTextField;
import maths.exception.OperationParseException;
import util.IOUtil;
import util.JFrameUtils;
import util.StringUtils;
import util.TimedUpdateHandler;
import util.data.DoubleArrayList;

public class StackPositionProcessorWindow extends JFrame implements ActionListener, TimedUpdateHandler{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1554159079180249250L;
	private static final Logger logger = LoggerFactory.getLogger(JFrame.class);
	
	private final JLabel labelPositionInput = new JLabel("Position input");
	private final JButton buttonPositionInput = new JButton();
	private final JLabel labelOutputFolder = new JLabel("Image Output");
	private final JButton buttonOutputFolder = new JButton();
	private final JLabel labelPositionOutput = new JLabel("PositionOutput");
	private final JButton buttonPositionOutput = new JButton();
	private final JLabel labelLightSource = new JLabel("Light source");
	private final JLabel labelEvaluationObject = new JLabel("Evaluation object");
	private final JLabel labelRange = new JLabel("Frame Range");
	private final JTextField textFieldRangeBegin = new JTextField();
	private final JTextField textFieldRangeEnd = new JTextField();
	private final JLabel labelScale = new JLabel("Scale");
	private final JTextField textFieldScale = new JTextField();
	private final SurfaceComboBox comboBoxlightSource;
	private final SurfaceComboBox comboBoxEvaluationObject;
	private final TextureComboBox comboBoxEvaluationTexture;
	private final JLabel labelNumRays = new JLabel("NumRays");
	private final JTextField textFieldNumRays = new JTextField("100");
	private final JLabel labelMode = new JLabel("Mode");
	private final JComboBox<Mode> comboBoxMode = new JComboBox<>(Mode.values());
	private final JLabel labelPostSurfaceCompensation = new JLabel("SurfaceCompensation");
	private final JCheckBox comboBoxPostSurfaceCompensation = new JCheckBox();
	private final JProgressBar progressBar = new JProgressBar();
	private final JButton buttonProgress = new JButton("Progress");
	private final JButton buttonPrintTrajectory = new JButton("Show Trajectory");
	private final JButton buttonPrintDensity = new JButton("Show Density");
	private final JToggleButton toggleButtonRecordPath = new JToggleButton("Record Path");
	private final JLabel labelResolution = new JLabel("Resolution");
	private final JMathTextField textFieldResolution = new JMathTextField("{1024,1024}");
	private final JCheckBox checkBoxBackward = new JCheckBox("Backward");
	private final RaytraceScene scene;
	private final AtomicInteger progress = new AtomicInteger(0);
	StackPositionProcessor spp = new StackPositionProcessor();
	private final Runnable updateProgressBarRunnable = new Runnable()//TODO atomic test
	{
		@Override
		public void run()
		{
			if (progressBar.getMaximum() != spp.getProgressMax())
			{
				progressBar.setMaximum(spp.getProgressMax());
			}
			progressBar.setValue(progress.get());
		}
	};
	
	@Override
	public void actionPerformed(ActionEvent e) {
		Object source = e.getSource();
		if (source == buttonProgress)
		{
			if (spp.isRunning)
			{
				spp.isRunning = false;
			}
			else
			{
				spp.isRunning = true;
				buttonProgress.setText("Stop");
				DataHandler.runnableRunner.run(new Runnable() {
					@Override
					public void run() {
						try{
							evaluate();
						}catch(final Exception e)
						{
							logger.error("Exception in evaluation", e);
							JFrameUtils.logErrorAndShow("Exception in evaluation", e, logger);
						}
						spp.isRunning = false;
						buttonProgress.setText("Progress");
					}
				}, "StackPositionProcessor");
			}
		}
		else if (source == buttonPrintTrajectory || source == buttonPrintDensity)
		{
			try {
				DoubleArrayList dal = IOUtil.readPositionFile(buttonPositionInput.getText());
				PointCloudVisualization pcv = new PointCloudVisualization(dal);
				pcv.setVisible(true);
				
			} catch (IOException ex) {
				logger.error("Can't read position file", ex);
			}
		}
		else if (source == toggleButtonRecordPath)
		{
			if (toggleButtonRecordPath.isSelected())
			{
				dal.clear();
				DataHandler.timedUpdater.add(this);
			}
			else
			{
				DataHandler.timedUpdater.remove(this);
				try
				{
					StringUtils.writeTapSeperated(dal, new File(buttonPositionInput.getText()), 6);
				}catch (IOException ex)
				{
					logger.error("Can't record path", ex);
				}
			}
		}
	}
	
	private final DoubleArrayList dal = new DoubleArrayList();
	private RaytraceSession session;
	
	@Override
	public void update() {
		Vector3f pos = Interface.scene.cameraPosition;
		Rotation3 rot = Interface.scene.cameraRotation;
		dal.add(pos);
		dal.add(rot.getXRadians());
		dal.add(rot.getYRadians());
		dal.add(rot.getZRadians());
	}
	
	@Override
	public int getUpdateInterval() {
		return 50;
	}
	
	public StackPositionProcessorWindow(final RaytraceScene scene, RaytraceSession session)
	{
		this.scene = scene;
		this.session = session;
		GroupLayout layout = new GroupLayout(getContentPane());
		getContentPane().setLayout(layout);
		comboBoxEvaluationTexture = new TextureComboBox(scene);
		comboBoxEvaluationTexture.setModel(new DefaultComboBoxModel<GuiTextureObject>(scene.textureObjectList.toArray(new GuiTextureObject[scene.textureObjectList.size()])));
		comboBoxlightSource = new SurfaceComboBox(scene, true);
		comboBoxlightSource.setModel(new DefaultComboBoxModel<OpticalObject>(scene.cloneActiveLights()));
		comboBoxEvaluationObject = new SurfaceComboBox(scene, false);
		comboBoxEvaluationObject.setModel(new DefaultComboBoxModel<OpticalObject>(scene.getActiveSurfaces()));
		
		labelPositionInput.setToolTipText("Position file with <azimuth>tap<elevation>");
		buttonPositionInput.setToolTipText(labelPositionInput.getToolTipText());
		
		labelOutputFolder.setToolTipText("Folder to write the selected texture");
		buttonOutputFolder.setToolTipText(labelOutputFolder.getToolTipText());
		comboBoxEvaluationTexture.setToolTipText(labelOutputFolder.getToolTipText());
		
		buttonProgress.setToolTipText("Start Processing");
		
		labelRange.setToolTipText("Leave empty for calculating all frames");
		textFieldRangeBegin.setToolTipText(labelRange.getToolTipText());
		textFieldRangeEnd.setToolTipText(labelRange.getToolTipText());
		
		labelPositionOutput.setToolTipText("Write out texture coordinates to a text File");
		buttonPositionOutput.setToolTipText(labelPositionOutput.getToolTipText());
		
		labelMode.setToolTipText("Array: Create an image for each timeframe, SINGLE: Create one image for all frames, CAMERA_TRACK: Create a raytraced image for each frame");
		
		layout.setHorizontalGroup(
			layout.createParallelGroup().addGroup(
					layout.createSequentialGroup().addGroup(
							layout.createParallelGroup()
							.addComponent(labelPositionInput)
							.addComponent(labelScale)
							.addComponent(labelRange)
							.addComponent(labelOutputFolder)
							.addComponent(labelResolution)
							.addComponent(labelPositionOutput)
							.addComponent(labelLightSource)
							.addComponent(labelEvaluationObject)
							.addComponent(labelNumRays)
							.addComponent(labelPostSurfaceCompensation)
							.addComponent(labelMode))
							.addGroup(layout.createParallelGroup()
							.addComponent(buttonPositionInput)
							.addComponent(textFieldScale)
							.addGroup(layout.createSequentialGroup().addComponent(textFieldRangeBegin).addComponent(textFieldRangeEnd))
							.addGroup(layout.createSequentialGroup().addComponent(buttonOutputFolder).addComponent(comboBoxEvaluationTexture))
							.addComponent(textFieldResolution)
							.addComponent(buttonPositionOutput)
							.addComponent(comboBoxlightSource)
							.addComponent(comboBoxEvaluationObject)
							.addComponent(textFieldNumRays)
							.addComponent(comboBoxPostSurfaceCompensation)
							.addComponent(comboBoxMode)))
			.addComponent(checkBoxBackward)
			.addComponent(progressBar).addGroup(layout.createSequentialGroup()
							.addComponent(buttonProgress).addComponent(buttonPrintTrajectory).addComponent(buttonPrintDensity).addComponent(toggleButtonRecordPath)));
		
		layout.setVerticalGroup(
				layout.createSequentialGroup()
					.addGroup(layout.createParallelGroup().addComponent(labelPositionInput, Alignment.CENTER).addComponent(buttonPositionInput, Alignment.CENTER))
					.addGroup(layout.createParallelGroup().addComponent(labelScale, Alignment.CENTER).addComponent(textFieldScale, Alignment.CENTER))
					.addGroup(layout.createParallelGroup().addComponent(labelRange, Alignment.CENTER).addComponent(textFieldRangeBegin, Alignment.CENTER).addComponent(textFieldRangeEnd, Alignment.CENTER))
					.addGroup(layout.createParallelGroup().addComponent(labelOutputFolder, Alignment.CENTER).addComponent(buttonOutputFolder, Alignment.CENTER).addComponent(comboBoxEvaluationTexture, Alignment.CENTER))
					.addGroup(layout.createParallelGroup().addComponent(labelResolution, Alignment.CENTER).addComponent(textFieldResolution, Alignment.CENTER))
					.addGroup(layout.createParallelGroup().addComponent(labelPositionOutput, Alignment.CENTER).addComponent(buttonPositionOutput, Alignment.CENTER))
					.addGroup(layout.createParallelGroup().addComponent(labelLightSource, Alignment.CENTER).addComponent(comboBoxlightSource, Alignment.CENTER))
					.addGroup(layout.createParallelGroup().addComponent(labelEvaluationObject, Alignment.CENTER).addComponent(comboBoxEvaluationObject, Alignment.CENTER))
					.addGroup(layout.createParallelGroup().addComponent(labelNumRays, Alignment.CENTER).addComponent(textFieldNumRays, Alignment.CENTER))
					.addGroup(layout.createParallelGroup().addComponent(labelPostSurfaceCompensation, Alignment.CENTER).addComponent(comboBoxPostSurfaceCompensation, Alignment.CENTER))
					.addGroup(layout.createParallelGroup().addComponent(labelMode, Alignment.CENTER).addComponent(comboBoxMode, Alignment.CENTER))
					.addComponent(checkBoxBackward)
					.addComponent(progressBar)
					.addGroup(layout.createParallelGroup().addComponent(buttonProgress).addComponent(buttonPrintTrajectory).addComponent(buttonPrintDensity).addComponent(toggleButtonRecordPath)));
		
		buttonPositionInput.addActionListener(JFrameUtils.selectFileButtonActionListener);
		buttonOutputFolder.addActionListener(JFrameUtils.selectFolderButtonActionListener);
		buttonPositionOutput.addActionListener(JFrameUtils.selectFileButtonActionListener);
		
		buttonProgress.addActionListener(this);
		buttonPrintTrajectory.addActionListener(this);
		buttonPrintDensity.addActionListener(this);
		toggleButtonRecordPath.addActionListener(this);
		final StackPositionProcessorWindow frame = this;
		
		addWindowListener(new WindowAdapter() {
		    @Override
		    public void windowClosed(java.awt.event.WindowEvent windowEvent) {
		        scene.removeObjectChangeListener((GuiTextureObject.TextureObjectChangeListener)frame);
		        scene.removeObjectChangeListener((GuiOpticalSurfaceObject.OpticalSurfaceObjectChangeListener)frame);
		    }
		});
		
		setSize(450, 500);
	}
	
	public void evaluate() throws OperationParseException
	{
		double tmpd = Double.NaN;
		try
		{
			tmpd = Double.parseDouble(textFieldScale.getText());
		}catch(Exception e) {}
		final double scale = tmpd;
		spp.evaluate(
				scene,
				scale,
				buttonPositionInput.getText(),
				comboBoxPostSurfaceCompensation.isSelected(),
				buttonOutputFolder.getText(),
				progress,
				(Mode) comboBoxMode.getSelectedItem(),
				(GuiTextureObject) comboBoxEvaluationTexture.getSelectedItem(),
				(SurfaceObject)comboBoxEvaluationObject.getSelectedItem(),
				textFieldRangeBegin.getText(),
				textFieldRangeEnd.getText(),
				updateProgressBarRunnable,
				Integer.parseInt(textFieldNumRays.getText()),
				textFieldResolution.get(),
				(OpticalObject)comboBoxlightSource.getSelectedItem(),
				buttonPositionOutput.getText(),
				checkBoxBackward.isSelected(),
				session);
	}
	

}
