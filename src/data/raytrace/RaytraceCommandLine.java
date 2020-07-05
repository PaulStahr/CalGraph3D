package data.raytrace;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.imageio.ImageIO;
import javax.swing.JFrame;

import org.jdom2.JDOMException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import data.DataHandler;
import data.Options;
import data.raytrace.OpticalObject.SCENE_OBJECT_COLUMN_TYPE;
import data.raytrace.StackPositionProcessor.Mode;
import geometry.Vector3d;
import io.Drawer.SvgDrawer;
import io.raytrace.SceneIO;
import jcomponents.raytrace.RaySimulationGui;
import jcomponents.raytrace.TextureView;
import logging.LockbackUtil;
import maths.Controller;
import maths.Operation;
import maths.Operation.CalculationController;
import maths.OperationCompiler;
import maths.algorithm.OperationCalculate;
import maths.exception.OperationParseException;
import util.IOUtil;
import util.JFrameUtils;
import util.StringUtils;
import util.functional.BooleanFunction;
import util.stream.NullOutputStream;

public class RaytraceCommandLine {
	private static final Logger logger = LoggerFactory.getLogger(RaytraceCommandLine.class);
	ArrayList<String> split = new ArrayList<String>();
	ParseUtil parser = new ParseUtil();
	CalculationController control = new Controller();
	public static enum InterpreterState{
		TRUE_IF, FALSE_IF;
	}

	public static class ExecEnv
	{
		public final File scriptDir;
		public final ArrayList<InterpreterState> interpreterState = new ArrayList<>();
		
		public ExecEnv(File scriptDir)
		{
			this.scriptDir = scriptDir;
		}

		public boolean codeActive() {
			for (int i = 0; i < interpreterState.size(); ++i)
			{
				if (interpreterState.get(i) == InterpreterState.FALSE_IF)
				{
					return false;
				}
			}
			return true;
		}
	}
	
	private void exec_impl(
			String command,
			ExecEnv env,
			BufferedWriter out,
			List<String> variables
	) throws IOException
	{
		if (command.equals(""))
		{
			return;
		}
		if (logger.isDebugEnabled()) {logger.debug("Running command " + command);}
		StringUtils.split_in_args(split, command, 0, command.length(), new StringBuilder());
		int interpreterStackSize = env.interpreterState.size();
		
		if (split.get(0).equals("if"))
		{
			String exp = split.get(1);
			if (exp.equals("true") || exp.equals("1"))
			{
				env.interpreterState.add(InterpreterState.TRUE_IF);
			}
			else if (exp.equals("false") || exp.equals("0"))
			{
				env.interpreterState.add(InterpreterState.FALSE_IF);
			}
			else
			{
	            throw new RuntimeException("Can't interpret " + exp + " as boolean expression");				
			}
		}
		else if (split.get(0).equals("else"))
		{
		    if (interpreterStackSize == 0)
	        {
	            throw new RuntimeException("error, wrong stack state");
	        }
			InterpreterState interpreterState = env.interpreterState.get(interpreterStackSize - 1);
	        if (interpreterState == InterpreterState.TRUE_IF)
	        {
	            env.interpreterState.set(interpreterStackSize - 1, InterpreterState.FALSE_IF);
	        }
	        else if (interpreterState == InterpreterState.FALSE_IF)
	        {
	            env.interpreterState.set(interpreterStackSize - 1, InterpreterState.TRUE_IF);
	        }
	        else
	        {
	            throw new RuntimeException("error, wrong stack state");
	        }
	    }
		else if (split.get(0).equals("endif"))
		{
			env.interpreterState.remove(interpreterStackSize - 1);
		}
		else if (env.codeActive())
		{
			switch (split.get(0))
			{
				case "help":
				{
					out.write("load\nmodify\nstp");
					break;
				}
				case "opt":
				{
					String key = split.get(1);
					if (split.size() < 2)
					{
						Options.OptionTreeNode node = Options.getNode(key);
						out.write(node.toString());
						out.flush();
					}
					else
					{
						Options.set(key, split.get(2));
						Options.triggerUpdates();
					}
					break;
				}
				case "math":
				{
					for (int i = 1; i < split.size(); ++i)
					{
						try {
							Operation op = OperationCompiler.compile(split.get(i));
							op.calculate(DataHandler.globalVariables, control);
						} catch (OperationParseException e) {
							out.write("Error, Parsing operation");
						}
					}
					break;
				}
				case "load":
				{
					RaySimulationGui gui = new RaySimulationGui(new RaytraceScene(split.get(1)));
					FileInputStream fis = new FileInputStream(split.get(2));
					try {
						gui.clear();
						SceneIO.loadScene(fis, gui.scene, gui);
					} catch (JDOMException e) {
						out.write(e.toString());
					}
					finally
					{
						fis.close();
					}
					gui.setVisible(true);
					break;
				}
				case "echo":
				{
					for (int i = 1; i < split.size(); ++i)
					{
						out.write(split.get(i));
					}
					out.flush();
					break;
				}
				case "run":
				{
					FileInputStream inStream = new FileInputStream(split.get(1));
					RaytraceCommandLine rcmd = new RaytraceCommandLine();
					ExecEnv subenv = new ExecEnv(new File(split.get(1)).getParentFile());
					rcmd.run(inStream, out, split.subList(1, split.size()), subenv);
					inStream.close();
					break;
				}
				case "surface":
				{
					if (split.size() < 4)
					{
						throw new RuntimeException("Not enough arguments " + command);
					}
					RaytraceScene scene = RaytraceScene.getScene(split.get(1));
					if (scene == null)
					{
						out.write(new NullPointerException("Scene not found").toString());
						out.flush();
					}
					else
					{
						OpticalObject obj = scene.getSurfaceObject(split.get(2));
						if (obj == null)
						{
							out.write(new NullPointerException("Object not found").toString());
							out.flush();
						}
						else
						{
							try {
								obj.setValue(OpticalObject.SCENE_OBJECT_COLUMN_TYPE.getByName(split.get(3)), split.get(4), scene.vs, parser);
							} catch (NumberFormatException | OperationParseException e) {
								out.write(e.toString());
								out.flush();
							}
						}
					}
					break;
				}
				case "volume":
				{
					if (split.size() < 4)
					{
						throw new RuntimeException("Not enough arguments " + command);
					}
					RaytraceScene scene = RaytraceScene.getScene(split.get(1));
					if (scene == null)
					{
						out.write(new NullPointerException("Scene not found").toString());
						out.flush();
					}
					else
					{
						OpticalObject obj = scene.getVolumeObject(split.get(2));
						if (obj == null)
						{
							out.write(new NullPointerException("Object not found").toString());
							out.flush();
						}
						else
						{
							try {
								obj.setValue(OpticalObject.SCENE_OBJECT_COLUMN_TYPE.getByName(split.get(3)), split.get(4), scene.vs, parser);
							} catch (NumberFormatException | OperationParseException e) {
								out.write(e.toString());
								out.flush();
							}
						}
					}
					break;
				}
				case "texture":
				{
					if (split.size() < 4)
					{
						throw new RuntimeException("Not enough arguments " + command);
					}
					RaytraceScene scene = RaytraceScene.getScene(split.get(1));
					if (scene == null)
					{
						out.write(new NullPointerException("Scene not found").toString());
						out.flush();
					}
					else
					{
						OpticalObject obj = scene.getTexture(split.get(2));
						if (obj == null)
						{
							out.write(new NullPointerException("Object not found").toString());
							out.flush();
						}
						else
						{
							try {
								obj.setValue(OpticalObject.SCENE_OBJECT_COLUMN_TYPE.getByName(split.get(3)), split.get(4), scene.vs, parser);
							} catch (NumberFormatException | OperationParseException e) {
								out.write(e.toString());
								out.flush();
							}
						}
					}
					break;
				}
				case "loglevel":
				{
					if (split.size() > 1)
					{
						LockbackUtil.setLoglevel(split.get(1));
					}
					else
					{
						ArrayList<String> tmp = new ArrayList<String>();
						LockbackUtil.getLogLevel(tmp);
						for (int i = 0; i < tmp.size(); ++i)
						{
							out.write(tmp.get(i));
						}
					}
					break;
				}
				case "sleep":
				{
					try
					{
						Thread.sleep(Integer.parseInt(split.get(1)));
					}catch(InterruptedException e) {}
					break;
				}
				case "option":
				{
					if (split.size() == 2)
					{
						out.write(Options.getNode(split.get(1)).toString());
						out.flush();
					}
					else
					{
						Options.set(split.get(1), split.get(2));
					}
				}
				case "window":
				{
					final RaytraceScene scene = RaytraceScene.getScene(split.get(1));
					if (scene == null)
					{
						out.write(new NullPointerException("Scene not found").toString());
						out.flush();
					}
					else
					{
						RaySimulationGui gui = (RaySimulationGui)DataHandler.findJFrame(new BooleanFunction<JFrame>() {
							@Override
							public boolean eval(JFrame obj) {
								return obj instanceof RaySimulationGui && ((RaySimulationGui)obj).scene == scene;
							}
						});
						if (split.size() < 3)
						{
							out.write(Double.toString(gui.paintOffset.x) + ' ' + Double.toString(gui.paintOffset.y) + ' ' + Double.toString(gui.panelVisualization.scale));
							out.flush();
						}
						else
						{
							gui.paintOffset.set(Double.parseDouble(split.get(2)), Double.parseDouble(split.get(3)));
							gui.panelVisualization.scale = Double.parseDouble(split.get(4));
							gui.panelVisualization.repaint();
						}
					}
					break;
				}
				case "wait":
				{
					switch(split.get(2))
					{
						case "volumes":{
							final RaytraceScene scene = RaytraceScene.getScene(split.get(2));
							if (scene == null)
							{
								out.write(new NullPointerException("Scene " + split.get(2) + " not found").toString());
								out.flush();
							}
							else
							{
								for (int i = 0; i < scene.volumePipelines.size(); ++i)
								{
									VolumePipeline vp = scene.volumePipelines.get(i);
									while (vp.isCalculating())
									{
										try {
											Thread.sleep(10);
										} catch (InterruptedException e) {
											// TODO Auto-generated catch block
											e.printStackTrace();
										}
									}
								}
							}
						}
					}
				}
				case "linevisualisation":
				{
					final RaytraceScene scene = RaytraceScene.getScene(split.get(1));
					if (scene == null)
					{
						out.write(new NullPointerException("Scene " + split.get(1) + " not found").toString());
						out.flush();
					}
					else
					{
						try {
							PropertyOnLineCalculator polc = new PropertyOnLineCalculator(scene);
							Vector3d position = new Vector3d();
							Vector3d direction = new Vector3d();
							Controller controll = new Controller();
							OperationCalculate.toDoubleArray(OperationCompiler.compile(split.get(2)).calculate(scene.vs, controll), position);
							OperationCalculate.toDoubleArray(OperationCompiler.compile(split.get(3)).calculate(scene.vs, controll), direction);
							BufferedWriter outBuf = new BufferedWriter(new OutputStreamWriter(new NullOutputStream()));
							SvgDrawer drawer = new SvgDrawer(outBuf);
							Operation op = OperationCompiler.compile(split.get(4)).calculate(scene.vs, controll);
							double rangeBegin = op.get(0).doubleValue();
							double rangeEnd = op.get(1).doubleValue();
							polc.paint(position, direction, rangeBegin, rangeEnd, Integer.parseInt(split.get(5)), Boolean.parseBoolean(split.get(6)), drawer);
							String type = StringUtils.getFileType(split.get(7));
				            if (type.equals("dat") || type.equals("csv"))
				        	{
				            	File file = new File(split.get(7));
				            	file.getParentFile().mkdirs();
				            	try {
									StringUtils.writeTapSeperated(polc.dataPoints, file , 2);
								} catch (IOException ex) {
									JFrameUtils.logErrorAndShow("Can't save to File", ex, logger);
								}
				        	}
						} catch (OperationParseException e) {
							throw new RuntimeException(e);
						}
					}
					break;
				}
				case "screenshot":
				{
					final RaytraceScene scene = RaytraceScene.getScene(split.get(1));
					if (scene == null)
					{
						out.write(new NullPointerException("Scene not found").toString());
						out.flush();
					}
					else
					{
						RaySimulationGui gui = (RaySimulationGui)DataHandler.findJFrame(new BooleanFunction<JFrame>() {
							@Override
							public boolean eval(JFrame obj) {
								return obj instanceof RaySimulationGui && ((RaySimulationGui)obj).scene == scene;
							}
						});
	
						int width = Integer.parseInt(split.get(2));
						int height = Integer.parseInt(split.get(3));
						File file = new File(split.get(4));
						file.getParentFile().mkdirs();
						String filepath = file.getAbsolutePath();
						if (filepath.endsWith("svg"))
						{
							try {
								FileWriter writer = new FileWriter(file);
								BufferedWriter outBuf = new BufferedWriter(writer);
								SvgDrawer drawer = new SvgDrawer(outBuf);
								drawer.beginDocument(width, height);
								gui.panelVisualization.paintComponent(drawer);
								drawer.endDocument();
								outBuf.close();
								writer.close();
							} catch (IOException e1) {
								throw new RuntimeException("Can't save Screensot", e1);
							}
						}
						else
						{
							BufferedImage im = new BufferedImage(gui.currentVisualization.getWidth(), gui.currentVisualization.getHeight(), BufferedImage.TYPE_INT_ARGB);
							gui.currentVisualization.paint(im.getGraphics());
							try {
								ImageIO.write(im, filepath.substring(filepath.indexOf('.') + 1), file);
							} catch (IOException e1) {
								throw new RuntimeException("Can't save Screensot", e1);
							}
						}
					}
					break;
				}
				case "focus_analysis":
				{
					if (split.get(1).equals("--help"))
					{
						out.write("<scene> <source> <destination> <raycount> <threedim> <elevations> (tableout <file>)");
						out.flush();
						break;
					}
					final FocusAnalysis fc = new FocusAnalysis();
					RaytraceScene scene = RaytraceScene.getScene(split.get(1));
					fc.lightSource = scene.getActiveEmissionObject(split.get(2));
					fc.destination = scene.getActiveSurfaceObject(split.get(3));
					fc.raycount = Integer.parseInt(split.get(4));
					fc.width = 512;
					fc.height = 512;
					fc.threeDim = Boolean.parseBoolean(split.get(5)); 
					fc.numElevations = Integer.parseInt(split.get(6));
					fc.scene = scene;
					fc.wait = true;
					fc.run();
					for (int i = 7; i < split.size(); ++i)
					{
						switch(split.get(i))
						{
							case "tableout" : 
							{
								FileWriter writer = new FileWriter(split.get(i + 1));
								BufferedWriter outBuf = new BufferedWriter(writer);System.out.println("Elevation " + Arrays.toString(fc.sourceElevations));
								IOUtil.writeColumnTable(new String[] 	{"elevation", 			"destination_elevation_average", "destination_elevation_variance", 	"destination_eucledean_variance", 	"accepted_ratio", 	"focal_distance", 	"hitpoint_distance"}, 
										new Object[] 					{fc.sourceElevations, 	fc.destinationElevationAveraged, fc.destinationElevationVariance, 	fc.destinationEucledeanVariance,	fc.acceptedRatio, 	fc.focalDistances, 	fc.focalHitpointDistances}, outBuf);
								outBuf.close();
								writer.close();
								++i;
								break;
							}
							case "mesh" :
							{
								MeshObject mesh = fc.createMeshObject();
								try {
									mesh.setValue(SCENE_OBJECT_COLUMN_TYPE.ID, split.get(i + 1), scene.vs, parser);
								} catch (OperationParseException e) {
									logger.error("Can't set Mesh id", e);
								}
								scene.add(mesh);
								++i;
								break;
							}
							default: throw new RuntimeException("Unknown Command");
						}
					}
					break;
				}
				case "stp":
				{
					for (int i = 0; i < split.size(); ++i)
					{
						out.write(i + " " + split.get(i));
					}
					out.flush();
					if (split.get(1).equals("--help"))
					{
						out.write("<scene> <scale> <position_input> <surface_compensation> <output_folder> <mode> <evaluation_texture> <evaluation_object> <range_begin> <range_end> <num_rays> <resolution> <light_source> <position_output> <backward> --output <output> --nfactor <noralization factor>");
						out.flush();
						break;
					}
					RaytraceScene scene = RaytraceScene.getScene(split.get(1));
					double scale = Double.NaN;
					try
					{
						scale = Double.parseDouble(split.get(2));
					}catch(Exception e) {}
					StackPositionProcessor spp = new StackPositionProcessor();
					AtomicInteger progress = new AtomicInteger();
					try {
						spp.isRunning = true;
						spp.evaluate(
						scene,
						scale,
						split.get(3),
						Boolean.parseBoolean(split.get(4)),
						split.get(5),
						progress,
						Mode.valueOf(split.get(6)),
						scene.getTexture(split.get(7)),
						scene.getSurfaceObject(split.get(8)),
						split.get(9),
						split.get(10),
						null,
						Integer.parseInt(split.get(11)),
						OperationCompiler.compile(split.get(12)),
						scene.getOpticalObject(split.get(13)),
						split.get(14),
						Boolean.parseBoolean(split.get(15)),
						null);
					} catch (NumberFormatException | OperationParseException e) {
						out.write(e.toString());
						out.flush();
					}
					BufferedImage img = spp.getImg();
					for (int i = 15; i < split.size(); ++i)
					{
						String str = split.get(i);
						if (str.length() > 2 && str.charAt(0) == '-' && str.charAt(1) == '-')
						{
							switch(str.substring(2))
							{
								case "output":
								{
									String filename = split.get(++i);
									File file = new File(filename);
									file.getParentFile().mkdirs();
									ImageIO.write(img, filename.substring(filename.lastIndexOf('.') + 1), file);
									break;
								}
								case "show":
								{
									TextureView tv = new TextureView(spp.getImg());
									tv.setVisible(true);
									break;
								}
								case "nfactor":
								{
									String filename = split.get(++i);
									File file = new File(filename);
									file.getParentFile().mkdirs();
									FileWriter writer = new FileWriter(file);
									BufferedWriter outBuf = new BufferedWriter(writer);
									outBuf.write(String.valueOf(spp.getNormalizationFactor()));
									outBuf.close();
									writer.close();
									break;
								}
								default: throw new IllegalArgumentException();
							}
						}
					}
					break;
				}
				case "exit":System.exit(0);break;
				case "":	break;
				default:	logger.error("Unknown command" + split.get(0));
			}
		}
		split.clear();
	}
	
	public void exec(
		String command,
		BufferedWriter out,
		List<String> variables,
		ExecEnv env
	)throws IOException
	{
		StringBuilder strB = new StringBuilder();
		int hashIndex = command.indexOf('#');
		if (hashIndex != -1)
		{
			command = command.substring(0, hashIndex);
		}
		for (int i = 0; i < variables.size(); ++i)
		{
			strB.append('$').append('{').append(i).append('}');
			command = command.replace(strB.toString(), variables.get(i));
			strB.setLength(0);
		}
		command = command.replace("${sdir}", env.scriptDir.getAbsolutePath());
		exec_impl(command, env, out, variables);
	}
	
	public void run(InputStream in, BufferedWriter out, List<String> variables, ExecEnv env) throws IOException
	{
		InputStreamReader reader = new InputStreamReader(in);
		BufferedReader inBuf = new BufferedReader(reader);
		String line;
		while ((line = inBuf.readLine()) != null)
		{
			exec(line, out, variables, env);
			if (line.equals("exit"))
			{
				return;
			}
		}
	}
}
