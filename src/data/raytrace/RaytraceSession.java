package data.raytrace;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import util.ListTools;

public class RaytraceSession {
	public static interface CommandExecutionListener
	{
		public void commandExecuted(String command);
	}
	
	public final ArrayList<WeakReference<CommandExecutionListener> > listener = new ArrayList<>();
	
	public void commandExecuted(String command)
	{
		int write = 0;
		for (int read = 0; read < listener.size(); ++read)
		{
			WeakReference<CommandExecutionListener> ref = listener.get(read);
			CommandExecutionListener cel = ref.get();
			if (cel != null)
			{
				cel.commandExecuted(command);
			}
			listener.set(write++, ref);
		}
		ListTools.removeRange(listener, write, listener.size());
	}

	public void addListener(CommandExecutionListener l) {
		listener.add(new WeakReference<RaytraceSession.CommandExecutionListener>(l));
	}

	public void removeListener(CommandExecutionListener l) {
		ListTools.removeAll(listener, l);
	}
}
