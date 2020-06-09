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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.imageio.ImageIO;

import org.jdom2.JDOMException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import data.DataHandler;
import data.Options;
import data.raytrace.StackPositionProcessor.Mode;
import io.raytrace.SceneIO;
import jcomponents.raytrace.RaySimulationGui;
import jcomponents.raytrace.TextureView;
import logging.LockbackUtil;
import maths.Controller;
import maths.Operation;
import maths.Operation.CalculationController;
import maths.OperationCompiler;
import maths.exception.OperationParseException;
import util.IOUtil;
import util.StringUtils;

public class RaytraceCommandLine {
	private static final Logger logger = LoggerFactory.getLogger(RaytraceCommandLine.class);
	ArrayList<String> split = new ArrayList<String>();
	ParseUtil parser = new ParseUtil();
	CalculationController control = new Controller();

	public static class ExecEnv
	{
		public final File scriptDir;
		
		public ExecEnv(File scriptDir)
		{
			this.scriptDir = scriptDir;
		}
	}
	
	private void exec_impl(
			String command,
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
		switch (split.get(0))
		{
			case "help":
			{
				out.write("load\nmodify\nstp");
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
					System.out.println(split.get(i));
				}
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
				}
				else
				{
					Options.set(split.get(1), split.get(2));
				}
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
						default: throw new RuntimeException("Unknown Command");
					}
				}
				break;
			}
			case "stp":
			{
				for (int i = 0; i < split.size(); ++i)
				{
					System.out.println(i + " " + split.get(i));
						
				}
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
			//default:	Logger.error("Unknown command", split.get(0));
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
		exec_impl(command, out, variables);
	}
	
	public void run(InputStream in, BufferedWriter out, List<String> variables, ExecEnv env) throws IOException
	{
		System.out.println("Variables: " + variables);
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
