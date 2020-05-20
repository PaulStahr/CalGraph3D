package data.raytrace;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.jdom2.JDOMException;

import data.DataHandler;
import data.raytrace.StackPositionProcessor.Mode;
import io.raytrace.SceneIO;
import jcomponents.raytrace.RaySimulationGui;
import maths.Controller;
import maths.Operation;
import maths.Operation.CalculationController;
import maths.OperationCompiler;
import maths.exception.OperationParseException;
import util.StringUtils;

public class RaytraceCommandLine {
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
		StringUtils.split_in_args(split, command, 0, command.length(), new StringBuilder());
		switch (split.get(0))
		{
			case "help":
			{
				out.write("load\nmodify\nstp");
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
				RaySimulationGui gui = new RaySimulationGui();
				FileInputStream fis = new FileInputStream(split.get(1));
				try {
					SceneIO.loadScene(fis, gui.scene, gui);
				} catch (JDOMException e) {
					out.write(e.toString());
				}
				finally
				{
					fis.close();
				}
				gui.setVisible(true);
			}
			case "run":
			{
				FileInputStream inStream = new FileInputStream(split.get(1));
				RaytraceCommandLine rcmd = new RaytraceCommandLine();
				ExecEnv subenv = new ExecEnv(new File(split.get(1)));
				rcmd.run(inStream, out, split.subList(1, split.size()), subenv);
				inStream.close();
			}
			case "surface":
			{
				RaytraceScene scene = RaytraceScene.getScene(split.get(1));
				if (scene == null)
				{
					out.write(new NullPointerException("Scene not found").toString());
				}
				else
				{
					OpticalObject obj = scene.getSurfaceObject(split.get(1));
					if (obj != null)
					{
						try {
							obj.setValue(OpticalObject.SCENE_OBJECT_COLUMN_TYPE.getByName(split.get(2)), split.get(3), scene.vs, parser);
						} catch (NumberFormatException | OperationParseException e) {
							out.write(e.toString());
						}
					}
				}
				break;
			}
			case "volume":
			{
				RaytraceScene scene = RaytraceScene.getScene(split.get(1));
				if (scene == null)
				{
					out.write(new NullPointerException("Scene not found").toString());
				}
				else
				{
					OpticalObject obj = scene.getVolumeObject(split.get(1));
					if (obj != null)
					{
						try {
							obj.setValue(OpticalObject.SCENE_OBJECT_COLUMN_TYPE.getByName(split.get(2)), split.get(3), scene.vs, parser);
						} catch (NumberFormatException | OperationParseException e) {
							out.write(e.toString());
						}
					}
				}
				break;
			}
			case "texture":
			{
				RaytraceScene scene = RaytraceScene.getScene(split.get(1));
				if (scene == null)
				{
					out.write(new NullPointerException("Scene not found").toString());
				}
				else
				{
					OpticalObject obj = scene.getTexture(split.get(1));
					if (obj == null)
					{
						out.write(new NullPointerException("Object not found").toString());
					}
					else
					{
						try {
							obj.setValue(OpticalObject.SCENE_OBJECT_COLUMN_TYPE.getByName(split.get(2)), split.get(3), scene.vs, parser);
						} catch (NumberFormatException | OperationParseException e) {
							out.write(e.toString());
						}
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
					out.write("<scene> <scale> <position_input> <surface_compensation> <output_folder> <mode> <evaluation_texture> <evaluation_object> <range_begin> <range_end> <num_rays> <resolution> <light_source> <position_output> <backward>");
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
				}
				break;
			}
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
