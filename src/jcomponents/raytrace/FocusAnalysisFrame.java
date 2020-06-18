package jcomponents.raytrace;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import data.DataHandler;
import data.raytrace.FocusAnalysis;
import data.raytrace.GuiOpticalSurfaceObject;
import data.raytrace.MeshObject;
import data.raytrace.OpticalSurfaceObject;
import data.raytrace.RaytraceScene;
import io.DataPlotter;
import io.Drawer.GraphicsDrawer;
import maths.data.ArrayOperation;
import util.JFrameUtils;

public class FocusAnalysisFrame extends JFrame  implements ActionListener{
	/**
	 * 
	 */
	private static final long serialVersionUID = 4728377778187484647L;
	private static final Logger logger = LoggerFactory.getLogger(FocusAnalysisFrame.class);
	private final RaytraceScene scene;
	private final JCheckBox checkBoxThreeD = new JCheckBox("Three dimensional");
	private final JTextField textFieldRaycount = new JTextField("Raycount");
	private final JButton buttonCalculate = new JButton("Calculate");
	
	public FocusAnalysisFrame(RaytraceScene scene)
	{
		this.scene = scene;
		add(checkBoxThreeD);
		add(textFieldRaycount);
		textFieldRaycount.setText(Integer.toString(10000));
		buttonCalculate.addActionListener(this);
		add(buttonCalculate);
		setLayout(JFrameUtils.SINGLE_COLUMN_LAYOUT);
		setSize(200, 100);
	}


	@Override
	public void actionPerformed(ActionEvent arg0) {
		Object source = arg0.getSource();
		if (source == buttonCalculate)
		{
			buttonCalculate.setEnabled(false);
			final FocusAnalysis fc = new FocusAnalysis();
			fc.destination = (OpticalSurfaceObject)scene.forceStartpoint;
			if (fc.destination == null)
			{
				buttonCalculate.setEnabled(true);
				JOptionPane.showMessageDialog(this, "No endobject");
				return;
			}
			fc.raycount = Integer.parseInt(textFieldRaycount.getText());
			fc.width = 400; fc.height = 400;
			fc.scene = scene;

			if (scene.getActiveLightCount() == 0)
			{
				buttonCalculate.setEnabled(true);
				JOptionPane.showMessageDialog(this, "No active Light");
				return;
			}
			fc.lightSource = (GuiOpticalSurfaceObject)scene.getActiveLight(0);
			
			fc.threeDim = checkBoxThreeD.isSelected(); 
			fc.setFinishRunnable(new Runnable() {
				@Override
				public void run()
				{
					buttonCalculate.setEnabled(true);
					BufferedImage img = new BufferedImage(fc.width, fc.height,BufferedImage.TYPE_INT_ARGB);
					double min = Double.POSITIVE_INFINITY, max = Double.NEGATIVE_INFINITY;
					for (int i = 0; i < fc.pixelVariance.length; ++i)
					{
						if (fc.pixelVariance[i] != 0)
						{
							double val = fc.pixelVariance[i] / fc.pixelCount[i];
							min = Math.min(min, val);
							max = Math.max(max, val);
						}
					}
					System.out.println(new StringBuilder().append(min).append(' ').append(max));
					System.out.println("Elevation " + Arrays.toString(fc.sourceElevations));
					System.out.println("SurfaceElevation " + Arrays.toString(fc.destinationElevationAveraged));
					System.out.println("Variance " + Arrays.toString(fc.destinationEucledeanVariance));
					System.out.println("CountPerArc " + Arrays.toString(fc.acceptedRatio));
					System.out.println("FocalDistance " + Arrays.toString(fc.focalDistances));
					
					try
					{
						FileWriter out = new FileWriter("FocusAnalysis");
						BufferedWriter outBuf = new BufferedWriter(out);
						util.IOUtil.writeColumnTable(
								new String[]{"Elevation", "SurfaceElevation", "SurfaceElevationVariance", "Variance", "CountPerArc", "FocalDistance", "HitpointDistance"},
								new Object[] {fc.sourceElevations, fc.destinationElevationAveraged, fc.destinationElevationVariance, fc.destinationEucledeanVariance, fc.acceptedRatio, fc.focalDistances, fc.focalHitpointDistances}, outBuf);
						outBuf.close();
						out.close();
					}catch(IOException ex)
					{
						logger.error("Can't write file", ex);
					}
					
					MeshObject mo = fc.createMeshObject();
					scene.add(mo);
					
					DataHandler.globalVariables.setGlobal("FocusArcs", new ArrayOperation(fc.sourceElevations));
					DataHandler.globalVariables.setGlobal("FocusVariance", new ArrayOperation(fc.destinationEucledeanVariance));
					DataHandler.globalVariables.setGlobal("FocusDistance", new ArrayOperation(fc.focalDistances));
					for (int y = 0; y < fc.height; ++y)
					{
						for (int x = 0; x < fc.width; ++x)
						{
							int pixelIndex = y * fc.width + x;
							int pc = fc.pixelCount[pixelIndex];
							if (pc != 0)
							{
								//System.out.println(x + ' ; + y + ' ' + values[pixelIndex] + '/' + pc);
								float val = fc.pixelVariance[pixelIndex];
								int value = (int)(255 * (val / pc) / max);
								value = Math.min(255, value);
								img.setRGB(x, y, (value << 8) | ((255 - value) << 16) | (Math.min(0xFF, pc) << 24));
								//img.setRGB(x, y, (int)(Math.random() * Integer.MAX_VALUE));
							}
						}
					}
					//img.getGraphics().drawArc(width / 4, height / 4, width / 2, height / 2, 0, 360);
					TextureView tv = new TextureView(img);
					tv.setVisible(true);
					DataPlotter dp = new DataPlotter();
					BufferedImage img2 = new BufferedImage(fc.width, fc.height,BufferedImage.TYPE_INT_ARGB);
					dp.addPlot(fc.sourceElevations, fc.destinationEucledeanVariance, "Focus", Color.BLACK);
					try {
						dp.plot(new GraphicsDrawer(img2.getGraphics()), new Rectangle2D.Double(0, 0, fc.width, fc.height));
						TextureView tv2 = new TextureView(img2);
						tv2.setVisible(true);
					} catch (IOException e1) {
						logger.error("Can't visualize graph");
					}
				}
			});
			fc.run();
			
		}
	}
	
	
}
