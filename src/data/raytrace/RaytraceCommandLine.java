package data.raytrace;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import org.jdom2.JDOMException;

import data.raytrace.StackPositionProcessor.Mode;
import jcomponents.raytrace.RaySimulationGui;
import maths.OperationCompiler;
import maths.VariableStack;
import maths.exception.OperationParseException;
import util.StringUtils;

public class RaytraceCommandLine {
	ArrayList<String> split = new ArrayList<String>();
	VariableStack vs = new VariableStack();
	ParseUtil parser = new ParseUtil();
	private InputStream in;
	private OutputStream out;

	public void exec(String command, BufferedWriter out) throws IOException
	{
		StringUtils.split(command, ' ', split);
		switch (split.get(0))
		{
			case "help":
			{
				out.write("load\nmodify\nstp");
			}
			case "load":
			{
				RaySimulationGui gui = new RaySimulationGui();
				FileInputStream fis = new FileInputStream(split.get(1));
				try {
					gui.loadScene(fis);
				} catch (JDOMException e) {
					out.write(e.toString());
				}
				finally
				{
					fis.close();
				}
				gui.setVisible(true);
			}
			case "modify":
			{
				RaytraceScene scene = RaytraceScene.getScene(split.get(1));
				if (scene == null)
				{
					out.write(new NullPointerException("Scene not found").toString());
				}
				else
				{
					OpticalObject obj = scene.getOpticalObject(split.get(1));
					if (obj != null)
					{
						try {
							obj.setValue(OpticalObject.SCENE_OBJECT_COLUMN_TYPE.getByName(split.get(2)), split.get(3), vs, parser);
						} catch (NumberFormatException | OperationParseException e) {
							out.write(e.toString());
						}
					}
				}
				break;
			}
			case "stp":
			{
				if (split.get(1).equals("--help"))
				{
					out.write("<scene> <scale> <position_input> <surface_compensation> <output_folder> <mode> <evaluation_texture> <evaluation_object> <range_begin> <range_end> <num_rays> <resolution> <light_source> <position_output> <backward>");
				}
				Runnable updateProgressRunnable = new Runnable() {
					@Override
					public void run() {
						
					}
				};
				RaytraceScene scene = RaytraceScene.getScene(split.get(1));
				double scale = Double.NaN;
				try
				{
					scale = Double.parseDouble(split.get(2));
				}catch(Exception e) {}
				StackPositionProcessor spp = new StackPositionProcessor();
				AtomicInteger progress = new AtomicInteger();
				try {
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
					updateProgressRunnable,
					Integer.parseInt(split.get(11)),
					OperationCompiler.compile(split.get(12)),
					scene.getOpticalObject(split.get(13)),
					split.get(14),
					Boolean.parseBoolean(split.get(15)));
				} catch (NumberFormatException | OperationParseException e) {
					out.write(e.toString());
				}
			}
		}
		split.clear();
	}
	
	public void run() throws IOException
	{
		InputStreamReader reader = new InputStreamReader(in);
		OutputStreamWriter writer = new OutputStreamWriter(out);
		BufferedReader inBuf = new BufferedReader(reader);
		BufferedWriter outBuf = new BufferedWriter(writer);
		String line;
		while ((line = inBuf.readLine()) != null)
		{
			exec(line, outBuf);
			if (line.equals("exit"))
			{
				return;
			}
		}
	}
	
	public RaytraceCommandLine(InputStream in, OutputStream out)
	{
		this.in = in;
		this.out = out;
	}
}
