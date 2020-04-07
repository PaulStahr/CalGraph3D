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
import data.raytrace.GuiOpticalSurfaceObject;
import data.raytrace.MeshObject;
import data.raytrace.OpticalSurfaceObject;
import data.raytrace.ParseUtil;
import data.raytrace.RaytraceScene;
import data.raytrace.OpticalObject.SCENE_OBJECT_COLUMN_TYPE;
import data.raytrace.RaytraceScene.RaySimulationObject;
import data.raytrace.raygen.RayGenerator;
import geometry.Matrix4d;
import geometry.Vector2d;
import geometry.Vector3d;
import geometry.Geometry.NearestPointCalculator;
import io.DataPlotter;
import io.Drawer.GraphicsDrawer;
import maths.data.ArrayOperation;
import maths.exception.OperationParseException;
import util.JFrameUtils;
import util.RunnableRunner;
import util.data.DoubleArrayList;

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
			final OpticalSurfaceObject endpoint = (OpticalSurfaceObject)scene.forceStartpoint;
			if (endpoint == null)
			{
				buttonCalculate.setEnabled(true);
				JOptionPane.showMessageDialog(this, "No endobject");
				return;
			}
			final int raycount = Integer.parseInt(textFieldRaycount.getText());
			final int width = 400, height = 400;
			final float values[] = new float[width * height];
			final int pixelCount[] = new int[width * height];
			if (scene.getActiveLightCount() == 0)
			{
				buttonCalculate.setEnabled(true);
				JOptionPane.showMessageDialog(this, "No active Light");
				return;
			}
			final GuiOpticalSurfaceObject lightSource = (GuiOpticalSurfaceObject)scene.getActiveLight(0);
			final double multElevation = lightSource.getMaxArcOpen() / 100;
			final double avaragedByIncomingArc[] = new double[100];
			final double elevations[] = new double[100];
			final double avarageCountPerArc[] = new double[100];
			final double focalDistance[] = new double[100];
			final double focalHitpointDistance[] = new double[100];
			final double surfaceElevation[] = new double[100];
			final int acceptedRayCount[] = new int[100];
			final double surfaceElevationVariance[] = new double[100];
			final double azimuths[][] = new double[100][];
			final int startIndex[] = new int[101];
			final boolean threeDim = checkBoxThreeD.isSelected(); 
			for (int i = 0; i < 100; ++i)
			{
				double elevation = i * multElevation;
				elevations[i] = elevation;
				int jsteps = threeDim ? 1 + (int)(Math.sin(elevation) * 100) : 2;
				azimuths[i] = new double[jsteps];
				double jstep = Math.PI * 2 / jsteps;
				
				for (int j = 0; j < jsteps; ++j)
				{
					azimuths[i][j] = j * jstep;
				}
				startIndex[i + 1] = startIndex[i] + jsteps;
			}
			final double vertices[] = new double[startIndex[startIndex.length - 1] * 3];
			final int maxBounces = 10;
			DataHandler.runnableRunner.runParallel(new RunnableRunner.ParallelRangeRunnable() {
				
				@Override
				public void run(int from, int to) {
					Vector3d position = new Vector3d();
					Vector3d direction = new Vector3d();
					Vector2d tc = new Vector2d();
					RaySimulationData rsd = new RaySimulationData(raycount, false);
					RaySimulationObject currentRay = new RaySimulationObject();
					Vector3d weightPoint = new Vector3d();
					RayGenerator gen = new RayGenerator();
					gen.threeDimensional = true;
					gen.setSource(lightSource);
					DoubleArrayList dal = new DoubleArrayList();
					
					NearestPointCalculator npc = new NearestPointCalculator(3);
					Vector3d focalPoint = new Vector3d();
					for (int i = from; i < to; ++i)
					{
						int countPerArc = 0;
						dal.clear();
						for (int j = 0; j < azimuths[i].length; ++j)
						{
							Arrays.fill(rsd.lastObject, null);
							weightPoint.set(0,0,0);
							gen.setArcs(elevations[i], azimuths[i][j]);
							scene.calculateRays(0, raycount, raycount, gen, 0, 0, null, null, rsd.endpoints, rsd.enddirs, null, null, rsd.accepted, rsd.bounces, rsd.lastObject, maxBounces, false, currentRay, RaytraceScene.UNACCEPTED_DELETE);
							
							int count = 0;
							npc.reset();
							for (int k = 0; k < raycount; ++k)
							{
								if (rsd.lastObject[k] == endpoint && rsd.accepted[k] == RaytraceScene.STATUS_ACCEPTED)
								{
									weightPoint.add(rsd.endpoints, k * 3);
									npc.addPoint(rsd.endpoints, rsd.enddirs, k * 3);
									++count;
								}
							}
							npc.calculate();
							npc.get(focalPoint);
							focalPoint.write(vertices, (startIndex[i] + j) * 3);
							focalDistance[i] += Math.sqrt(endpoint.midpoint.distanceQ(focalPoint));
							countPerArc += count;
							weightPoint.multiply(1/(double)count);
							double variance = 0;
							for (int k = 0; k < raycount; ++k)
							{
								if (rsd.lastObject[k] == endpoint && rsd.accepted[k] == RaytraceScene.STATUS_ACCEPTED)
								{
									focalHitpointDistance[i] += Math.sqrt(focalPoint.distanceQ(rsd.endpoints, k * 3));
									double dist = weightPoint.distanceQ(rsd.endpoints, k * 3);
									variance += dist;
									avaragedByIncomingArc[i] += dist;
								}
							}
							if (count == 0)
							{
								continue;
							}
							variance = Math.sqrt(variance / count);
							System.out.print(new StringBuilder().append('(').append(variance).append(' ').append(count).append(')'));

							for (int k = 0; k < raycount; ++k)
							{
								if (rsd.lastObject[k] == endpoint && rsd.accepted[k] == RaytraceScene.STATUS_ACCEPTED)
								{							
									position.set(rsd.endpoints, k * 3);
									direction.set(rsd.enddirs,  k * 3);
									endpoint.getTextureCoordinates(position, direction, tc);
									double diffx = tc.x - 0.5, diffy = tc.y - 0.5;
									dal.add(Math.sqrt(diffx * diffx + diffy * diffy) * (2 * Math.PI));
									//System.out.println(position + ' ' + tc);
									int x = (int)(tc.x * width);
									int y = (int)(tc.y * height);
									int pixelIndex = y * width + x;
									++pixelCount[pixelIndex];
									values[pixelIndex] += variance;
									++acceptedRayCount[i];
								}
							}
							avaragedByIncomingArc[i] += variance;
						}
						focalHitpointDistance[i] /= acceptedRayCount[i];
						avaragedByIncomingArc[i] = Math.sqrt(avaragedByIncomingArc[i] / countPerArc);
						avarageCountPerArc[i] = (double)countPerArc / azimuths[i].length;
						focalDistance[i] /= azimuths[i].length;
						surfaceElevation[i] = dal.sum() / acceptedRayCount[i];
						if (acceptedRayCount[i] != dal.size())
						{
							throw new RuntimeException();
						}
						surfaceElevationVariance[i] = Math.sqrt(dal.diffSumQ(surfaceElevation[i]) / dal.size());
						System.out.println();
					}
				}
				
				@Override
				public void finished()
				{
					buttonCalculate.setEnabled(true);
					BufferedImage img = new BufferedImage(width, height,BufferedImage.TYPE_INT_ARGB);
					double min = Double.POSITIVE_INFINITY, max = Double.NEGATIVE_INFINITY;
					for (int i = 0; i < values.length; ++i)
					{
						if (values[i] != 0)
						{
							double val = values[i] / pixelCount[i];
							min = Math.min(min, val);
							max = Math.max(max, val);
						}
					}
					System.out.println(new StringBuilder().append(min).append(' ').append(max));
					System.out.println("Elevation " + Arrays.toString(elevations));
					System.out.println("SurfaceElevation " + Arrays.toString(surfaceElevation));
					System.out.println("Variance " + Arrays.toString(avaragedByIncomingArc));
					System.out.println("CountPerArc " + Arrays.toString(avarageCountPerArc));
					System.out.println("FocalDistance " + Arrays.toString(focalDistance));
					
					try
					{
						FileWriter out = new FileWriter("FocusAnalysis");
						BufferedWriter outBuf = new BufferedWriter(out);
						util.IOUtil.writeColumnTable(
								new String[]{"Elevation", "SurfaceElevation", "SurfaceElevationVariance", "Variance", "CountPerArc", "FocalDistance", "HitpointDistance"},
								new Object[] {elevations, surfaceElevation, surfaceElevationVariance, avaragedByIncomingArc, avarageCountPerArc, focalDistance, focalHitpointDistance}, outBuf);
						outBuf.close();
						out.close();
					}catch(IOException ex)
					{
						logger.error("Can't write file", ex);
					}
					int faces[] = new int[startIndex[startIndex.length - 1] * 6 - (azimuths[0].length + azimuths[azimuths.length - 1].length) * 3] ;
					for (int i = 0, f = 0; i < azimuths.length - 1; ++i)
					{
						double azimuthsl[] = azimuths[i];
						double azimuthsu[] = azimuths[i + 1];
						int sil = startIndex[i];
						int siu = startIndex[i + 1];
						int j = 0,k = 0;
						while (j < azimuthsl.length && k < azimuthsu.length)
						{
							if (azimuthsl[j] < azimuthsu[k])
							{
								faces[f++] = sil + j;
								faces[f++] = sil + (++j) % azimuthsl.length;
								faces[f++] = siu + k;
							}
							else
							{
								faces[f++] = sil + j;
								faces[f++] = siu + k;
								faces[f++] = siu + (++k) % azimuthsu.length;
							}
						}
						if (j == azimuthsl.length)
						{
							while (k < azimuthsu.length)
							{
								faces[f++] = sil;
								faces[f++] = siu + k;
								faces[f++] = siu + (++k) % azimuthsu.length;
							}
						}
						if (k == azimuthsu.length)
						{
							while (j < azimuthsl.length)
							{
								faces[f++] = sil + j;
								faces[f++] = sil + (++j) % azimuthsl.length;
								faces[f++] = siu;
							}
						}
					}
					ParseUtil parser = new ParseUtil();
					MeshObject mo = new MeshObject(scene.vs, parser);
					mo.setData(vertices, faces, null);
					if (!threeDim)
					{
						int lines[] = new int[2 * vertices.length / 3 - 4];
						for (int i = 0; i < lines.length / 2; ++i)
						{
							lines[i * 2] = i;
							lines[i * 2 + 1] = i + 2;
						}
						mo.setLines(lines);
					}
					try {
						mo.setValue(SCENE_OBJECT_COLUMN_TYPE.TRANSFORMATION, new Matrix4d(1), scene.vs, null);
					} catch (OperationParseException e) {
						logger.error("Can't set Value", e);
					}
					scene.add(mo);
					
					DataHandler.globalVariables.setGlobal("FocusArcs", new ArrayOperation(elevations));
					DataHandler.globalVariables.setGlobal("FocusVariance", new ArrayOperation(avaragedByIncomingArc));
					DataHandler.globalVariables.setGlobal("FocusDistance", new ArrayOperation(focalDistance));
					for (int y = 0; y < height; ++y)
					{
						for (int x = 0; x < width; ++x)
						{
							int pixelIndex = y * width + x;
							int pc = pixelCount[pixelIndex];
							if (pc != 0)
							{
								//System.out.println(x + ' ; + y + ' ' + values[pixelIndex] + '/' + pc);
								float val = values[pixelIndex];
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
					BufferedImage img2 = new BufferedImage(width, height,BufferedImage.TYPE_INT_ARGB);
					dp.addPlot(elevations, avaragedByIncomingArc, "Focus", Color.BLACK);
					try {
						dp.plot(new GraphicsDrawer(img2.getGraphics()), new Rectangle2D.Double(0, 0, width, height));
						TextureView tv2 = new TextureView(img2);
						tv2.setVisible(true);
					} catch (IOException e1) {
						logger.error("Can't visualize graph");
					}
				}
			}, "Focus Heatmap", null, 0, 100, 10);
			
			
		}
	}
	
	
}
