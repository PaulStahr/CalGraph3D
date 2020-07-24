package jcomponents.raytrace;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.OutputStreamWriter;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JTextArea;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import data.DataHandler;
import data.raytrace.RaytraceCommandLine;
import data.raytrace.RaytraceCommandLine.ExecEnv;
import data.raytrace.RaytraceScene;
import data.raytrace.RaytraceSession;
import util.JFrameUtils;
import util.data.UniqueObjects;

public class MacroWindow extends JFrame implements ActionListener{
	/**
	 * 
	 */
	private static final long serialVersionUID = 8961055562270337081L;
	private static final Logger logger = LoggerFactory.getLogger(MacroWindow.class);
	public final JTextArea textArea = new JTextArea();
	private final JButton buttonRun = new JButton("Run");
	
	public MacroWindow(RaytraceScene scene, RaytraceSession session)
	{
		setLayout(JFrameUtils.SINGLE_COLUMN_LAYOUT);
		add(textArea);
		add(buttonRun);
		buttonRun.addActionListener(this);
		setSize(500, 500);
		setVisible(true);
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		final ByteArrayInputStream in = new ByteArrayInputStream(textArea.getText().getBytes());
		final RaytraceCommandLine rcl = new RaytraceCommandLine();
		buttonRun.setEnabled(false);
		DataHandler.runnableRunner.run(new Runnable() {
				@Override
				public void run() {
					try {
					rcl.run(in, new BufferedWriter(new OutputStreamWriter(System.out)),UniqueObjects.EMPTY_STRING_LIST, new ExecEnv(new File("")));
				} catch (Exception ex) {
					JFrameUtils.logErrorAndShow("Error in running Script", ex, logger);
				}
				buttonRun.setEnabled(true);
			}
		}, "Run Macro");
	}
}
