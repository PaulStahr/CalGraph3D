/*******************************************************************************
 * Copyright (c) 2019 Paul Stahr
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 ******************************************************************************/
package data;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.ref.WeakReference;
import java.math.BigInteger;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.UnsupportedLookAndFeelException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import geometry.Geometry;
import geometry.Rotation3;
import geometry.Vector3f;
import geometry.Vectorf;
import io.StreamUtil;
import jcomponents.Graph;
import jcomponents.Interface;
import jcomponents.panels.InterfacePanel;
import jcomponents.panels.InterfacePanelFactory;
import maths.exception.OperationParseException;
import maths.variable.Variable;
import maths.variable.VariableStack;
import util.BufferedZipReader;
import util.JFrameUtils;
import util.SaveLineCreator;
import util.StringUtils;
import util.ThreadPool;
import util.TimedUpdater;
import util.data.UniqueObjects;
import util.functional.BooleanFunction;
import util.io.IOUtil;

/**
 * Write a description of class DataHandler here.
 *
 * @author  Paul Stahr
 * @version 04.02.2012
 */
public abstract class DataHandler
{
	public static Project project = new Project();
	public static final VariableStack globalVariables = new VariableStack(10);
    public static final UIManager.LookAndFeelInfo lookAndFeelInfo[] = UIManager.getInstalledLookAndFeels();
	private static final Logger logger = LoggerFactory.getLogger(DataHandler.class);
    public static final ThreadPool runnableRunner = new ThreadPool(5000);
    public static final TimedUpdater timedUpdater = new TimedUpdater(10);
    private static final String resourceFolder = "/resources/";
    private static final ArrayList<WeakReference<JFrame> > updateUIList = new ArrayList<>();

    public static AtomicInteger openWindows = new AtomicInteger();

    public static final JFrame findJFrame(BooleanFunction<JFrame> func)
    {
    	for (int i = 0; i < updateUIList.size(); ++i)
    	{
    		JFrame frame = updateUIList.get(i).get();
    		if (func.eval(frame))
    		{
    			return frame;
    		}
    	}
    	return null;
    }

	private static String lookAndFeel = "";
	private static String currentFile;
    private static final Runnable updateLookAndFeel = new Runnable(){
		@Override
		public void run() {
    		try{
   				UIManager.setLookAndFeel(lookAndFeel);
  			}catch (UnsupportedLookAndFeelException e) {
  				logger.error("UIManager not supported");
   			}catch (ClassNotFoundException e) {
   				logger.error("UIManager not found");
   			}catch (InstantiationException e) {
   				logger.error("Can't set UIManager:", e);
   			}catch (IllegalAccessException e) {
   				logger.error("Can't set UIManager:", e);
   			}
		}
	};


	public static void loadLib(String file) throws IOException {
    	InputStream in = DataHandler.getResourceAsStream(file);
    	if (in == null) {throw new NullPointerException("Couldn't get resource " + file);}
    	String name = file.substring(Math.max(0,file.lastIndexOf('/')));
        File fileOut = new File(System.getProperty("java.io.tmpdir") + 	'/' + name);
        logger.info("Writing dll to: " + fileOut.getAbsolutePath());
        OutputStream out = new FileOutputStream(fileOut);
        IOUtil.copy(in, out);
        in.close();
        out.close();
        System.load(fileOut.toString());
        fileOut.deleteOnExit();
	}

	private static final maths.ProgramFunction saveFunction = new maths.ProgramFunction("save") {
		@Override
		public void run(){
			save();
		}
	};

	static{
        UIManager.addPropertyChangeListener(new PropertyChangeListener() {
    		@Override
    		public void propertyChange(PropertyChangeEvent evt) {
    			for (int i=updateUIList.size()-1;i>=0;--i){
    				try{
    					JFrame frame = updateUIList.get(i).get();
    					if (frame == null){
    						updateUIList.remove(i);
    					}else{
    						SwingUtilities.updateComponentTreeUI(frame);
    					}
    				}catch(Exception e){
    					logger.error("Error at updating Component tree", e);
    				}
    			}
    		}
    	});
        maths.OperationCompiler.addProgramFunction(saveFunction);
	}

    private DataHandler(){}

    public static final URL getResource(String resource)
    {
    	return DataHandler.class.getResource(DataHandler.getResourceFolder().concat(resource));
    }

    public static final InputStream getResourceAsStream(String file)
    {
    	return DataHandler.class.getResourceAsStream(getResourceFolder().concat(file));
    }

    public static final String getResourceAsString(String file) throws IOException
    {
		InputStream stream = DataHandler.getResourceAsStream(file);
		String res = StreamUtil.readStreamToString(stream);
		stream.close();
		return res;
    }

    public static final String getResourceFolder(){return resourceFolder;}

    public static final void setLookAndFeel(final Object lafi){
    	if (lafi instanceof LookAndFeelInfo){
    		if (UIManager.getLookAndFeel().getName().equals(((LookAndFeelInfo)lafi).getName()))
    			return;
    		setLookAndFeel(((LookAndFeelInfo)lafi).getClassName());
    	}else if (lafi instanceof String){
    		setLookAndFeel((String)lafi);
    	}else{
    		throw new IllegalArgumentException();
    	}
    }

    public static final void setLookAndFeel(String laf){
    	if (lookAndFeel.equals(laf))
    		return;
    	lookAndFeel = laf;
    	JFrameUtils.runByDispatcher(updateLookAndFeel);
    }

    public static final void load (String file, Interface window){
		final OperatingSystemMXBean bean =  ManagementFactory.getOperatingSystemMXBean();
		final BigInteger THOUSAND = BigInteger.valueOf(1000);
		final boolean activated = BigInteger.valueOf(System.getProperty("user.name").hashCode()).multiply(THOUSAND).add(BigInteger.valueOf(bean.getAvailableProcessors())).add(BigInteger.valueOf(bean.getArch().hashCode())).multiply(THOUSAND).add(BigInteger.valueOf(bean.getVersion().hashCode())).multiply(THOUSAND).add(BigInteger.valueOf(System.getProperty("user.home").hashCode())).multiply(THOUSAND).add(BigInteger.valueOf(bean.getName().hashCode())).abs().equals(Options.getBigInteger("product_key", BigInteger.ZERO).modPow(new BigInteger("21f3076c0b61cb3a1a5e71609a3297b11c7750cc9c1b6a30a89b22aa3925b0f495c49a6a0411de8dcb801c8459966496150d77f80173567cda5ba057f210744a3e6bbfe1f98c581cfda68f0cc8076473563d3bfc25937ba445b7a898d3b3acfd2f76e4ad94640b47b522be2cd317a0faf12c1113162adf7f0d2b8277f2f203cb71f071b69bb2345195eec5627bffdc147157b047c0bb223af0cec1545138a199297c948b413fecfe64e6d6f872499506473b99b895d4e2dfd98247a426e1a11aecbf402a3b44d147786561eadc057bc8df85d717e42f8b68afccfac3a036599459d495c874afab1590d0091853fe1357", 16), new BigInteger("4852dabb7b4fb62a131811b43fd58143fa4087cd04cadd1f24827f0d76083807d4bd691c4200fce6e73713b40291f2715cfd202723956ee8e47ab4852861d31def18561c128b22c80e333432a96cebd98c5c32820a39a86a5dd6cedbca527aba875299b60b3219faec366ec6af884e1801b864ac4fb3cec7d116381231641115be48decb59ca5f1bdf8b5014a2e96edb15ad1e5731e546ad29914efcf7e40f2474309dbddd2d9202d0a007698e791c7e1919110d9091b1201d32e7d98c26088c74d25886da4117d83cc1fb6237d941a0b8e2df1332683912ec0623e15f0ef98571dd4daeceb2fc82949681454373f9cb998c8369b1324ed4b7c94815e2f03e7", 16)));
		if (!activated)
			return;
        try{
            load(new FileInputStream(file), window);
        }catch(IOException e){
            logger.error("Can't load Project from file \"" + file + '\"', e);
            return;
        }
        logger.info(new StringBuilder().append("Project loaded from \"").append(file).append('"').toString());
        currentFile = file;
        addRecentFile(file);
    }

    public static final void load (URL url, Interface window){
        try{
        	InputStream stream = url.openStream();
            load(stream, window);
            stream.close();
            currentFile = null;
        }catch(IOException e){
            logger.error("Can't load Project from url \"" + url + '\"', e);
        }
    }

    private static synchronized final void load (InputStream stream, Interface window){
        globalVariables.clear();
        window.removeAllGraphs();
        window.removeAllTools();
        final SaveLineCreator saveLineCreator = new SaveLineCreator();
        StringUtils strUtils = new StringUtils();
        try{
            final ZipInputStream zipInStream = new ZipInputStream(stream);
            final BufferedZipReader inBuffer = new BufferedZipReader(zipInStream, "UTF-16");
            final ArrayList<String> ll = new ArrayList<>();
            ZipEntry entry;
            while ((entry = inBuffer.getNextEntry())!=null){
                final String name = entry.getName();
                String line;
                if (name.equals("Camera")){
                    final Vector3f pos = new Vector3f();
                    final Rotation3 rot = new Rotation3();
                    while (null!=(line=inBuffer.readLine())){
                    	final SaveLineCreator.SaveObject saveObject = saveLineCreator.getSaveObject(line);
                        if (saveObject != null){
	                    	final String value = saveObject.value;
	                        final String variable = saveObject.variable;
	                        if      (variable.equals("xP")) pos.x = Float.parseFloat(value);
                            else if (variable.equals("yP")) pos.y = Float.parseFloat(value);
                            else if (variable.equals("zP")) pos.z = Float.parseFloat(value);
                            else if (variable.equals("xR")) rot.setDegreesX(Float.parseFloat(value));
                            else if (variable.equals("yR")) rot.setDegreesY(Float.parseFloat(value));
                            else if (variable.equals("zR")) rot.setDegreesZ(Float.parseFloat(value));
                            else if (variable.equals("pos")) Geometry.parse(value, pos);
                            else if (variable.equals("rot")) Geometry.parse(value, (Vectorf)rot);
                        }
                    }
                    Interface.scene.setCameraPosition(pos, rot);
                }else if (name.equals("Variables")){
                    while (null!=(line=inBuffer.readLine())){
                    	final SaveLineCreator.SaveObject saveObject = saveLineCreator.getSaveObject(line);
                        if (saveObject != null)
							try {
								int index = saveObject.variable.indexOf('(');
								globalVariables.add(index == -1 ? new Variable(saveObject.variable, saveObject.value) : new Variable(saveObject.variable.substring(0, index), saveObject.value, strUtils.split(saveObject.variable, index +1, saveObject.variable.indexOf(')'), ',')));
							} catch (OperationParseException e) {
								logger.error("Error at reading variables", e);
							}
                    }
                }else if (name.equals("Graph")){
                    ll.clear();
                    while (null!=(line=inBuffer.readLine())){
                        if (line.equals("startGraph")){
                            ll.clear();
                        }else if (line.equals("endGraph")){
                        	JFrameUtils.runByDispatcherAndWait(
                        		new Runnable(){
									@Override
									public void run() {
										Graph graph = new Graph();
			                        	graph.setContent(ll);
			                            Interface.addGraph(graph);
			                        }
                        		});
                        }else{
                        	ll.add(line);
                        }
                    }
                }else if (name.startsWith("Graph")){
                   	ll.clear();
                    while (null!=(line=inBuffer.readLine()))
                    	ll.add(line);
                	JFrameUtils.runByDispatcherAndWait(
                    	new Runnable(){
							@Override
							public void run() {
								final Graph graph = new Graph();
								graph.setContent(ll);
								Interface.addGraph(graph);
							}
                    	});
                }else if (name.equals("Panel Tools")){
                    ll.clear();
                    while(null!=(line=inBuffer.readLine())){
                        if (line.equals("startPanel")){
                        	ll.clear();
                        }else if (line.equals("endPanel")){
                        	JFrameUtils.runByDispatcherAndWait(
                        		new Runnable(){
									@Override
									public void run() {
										final InterfacePanel tmp = InterfacePanelFactory.getInstance(ll, saveLineCreator, globalVariables);
										if (tmp != null)
											Interface.addTool(tmp);
										else
											logger.error("Error in save file");
									}
                        		});
                        }else{
                            ll.add(line);
                        }
                    }
                }else if (name.equals("ProjectData")){
                    while (null!=(line=inBuffer.readLine())){
                    	final SaveLineCreator.SaveObject saveObject = saveLineCreator.getSaveObject(line);
                        if (saveObject != null){
	                    	final String value = saveObject.value;
	                        final String variable = saveObject.variable;
                            if (variable.equals("name"))            project.name = value;
                            else if (variable.equals("author"))     project.author = value;
                            else if (variable.equals("description"))project.description = value;
                        }
                    }
                }
            }
            inBuffer.close();
            zipInStream.close();
            stream.close();
        }catch (IOException e){
        	JFrameUtils.logErrorAndShow("Can't load project", e, logger);
	    }catch (Exception e){
        	JFrameUtils.logErrorAndShow("Can't load project", e, logger);
	    }
    }

    public static final boolean save(){
        if (currentFile == null || currentFile.length()==0)
            return false;
        save (currentFile);
        return true;
    }

    public static final synchronized void save (String file){
		final OperatingSystemMXBean bean =  ManagementFactory.getOperatingSystemMXBean();
		final BigInteger THOUSAND = BigInteger.valueOf(1000);
		final boolean activated = BigInteger.valueOf(System.getProperty("user.name").hashCode()).multiply(THOUSAND).add(BigInteger.valueOf(bean.getAvailableProcessors())).add(BigInteger.valueOf(bean.getArch().hashCode())).multiply(THOUSAND).add(BigInteger.valueOf(bean.getVersion().hashCode())).multiply(THOUSAND).add(BigInteger.valueOf(System.getProperty("user.home").hashCode())).multiply(THOUSAND).add(BigInteger.valueOf(bean.getName().hashCode())).abs().equals(Options.getBigInteger("product_key", BigInteger.ZERO).modPow(new BigInteger("21f3076c0b61cb3a1a5e71609a3297b11c7750cc9c1b6a30a89b22aa3925b0f495c49a6a0411de8dcb801c8459966496150d77f80173567cda5ba057f210744a3e6bbfe1f98c581cfda68f0cc8076473563d3bfc25937ba445b7a898d3b3acfd2f76e4ad94640b47b522be2cd317a0faf12c1113162adf7f0d2b8277f2f203cb71f071b69bb2345195eec5627bffdc147157b047c0bb223af0cec1545138a199297c948b413fecfe64e6d6f872499506473b99b895d4e2dfd98247a426e1a11aecbf402a3b44d147786561eadc057bc8df85d717e42f8b68afccfac3a036599459d495c874afab1590d0091853fe1357", 16), new BigInteger("4852dabb7b4fb62a131811b43fd58143fa4087cd04cadd1f24827f0d76083807d4bd691c4200fce6e73713b40291f2715cfd202723956ee8e47ab4852861d31def18561c128b22c80e333432a96cebd98c5c32820a39a86a5dd6cedbca527aba875299b60b3219faec366ec6af884e1801b864ac4fb3cec7d116381231641115be48decb59ca5f1bdf8b5014a2e96edb15ad1e5731e546ad29914efcf7e40f2474309dbddd2d9202d0a007698e791c7e1919110d9091b1201d32e7d98c26088c74d25886da4117d83cc1fb6237d941a0b8e2df1332683912ec0623e15f0ef98571dd4daeceb2fc82949681454373f9cb998c8369b1324ed4b7c94815e2f03e7", 16)));
		if (!activated)
			return;
        SaveLineCreator saveLineCreator = new SaveLineCreator();
        StringBuilder strBuilder = new StringBuilder();
        try{
            final FileOutputStream fileOutStream = new FileOutputStream(file);
            final ZipOutputStream zipOutStream = new ZipOutputStream(fileOutStream);
            final OutputStreamWriter writer = new OutputStreamWriter(zipOutStream, Charset.forName("UTF-16"));
            final BufferedWriter outBuffer = new BufferedWriter(writer);

            zipOutStream.putNextEntry(new ZipEntry("Info"));
            outBuffer.write(saveLineCreator.getSaveLine("version", ProgrammData.getVersion()));
            outBuffer.write(saveLineCreator.getSaveLine("date", new SimpleDateFormat ("yyyy:MM:dd-HH:mm:ss ").format(new Date())));
            outBuffer.write(saveLineCreator.getSaveLine("user", System.getProperty("user.name")));
            outBuffer.write(saveLineCreator.getSaveLine("system", System.getProperty("os.name")));
            outBuffer.flush();
            zipOutStream.closeEntry();
            zipOutStream.putNextEntry(new ZipEntry("Camera"));
            outBuffer.write(saveLineCreator.getSaveLine("pos", Interface.scene.cameraPosition.toString()));
            outBuffer.write(saveLineCreator.getSaveLine("rot", Interface.scene.cameraRotation.toString()));
            outBuffer.flush();
            zipOutStream.closeEntry();

            zipOutStream.putNextEntry(new ZipEntry("Variables"));
            for (int i=0;i<globalVariables.size();i++){
            	Variable v = globalVariables.get(i);
            	CharSequence name;
            	if (v.operandCount() == -1)
            		name = v.nameObject.string;
            	else{
                	strBuilder.setLength(0);
            		strBuilder.append(v.nameObject.string).append('(');
            		for (int j=0;j<v.operandCount();j++){
            			if (j!=0)
            				strBuilder.append(',');
            			strBuilder.append(v.operand(j));
            		}
            		strBuilder.append(')');
            		name = strBuilder.toString();
            	}
            	strBuilder.setLength(0);
            	outBuffer.write(saveLineCreator.getSaveLine(name, v.stringValue(strBuilder)));
            }
            outBuffer.flush();
            zipOutStream.closeEntry();

            zipOutStream.putNextEntry(new ZipEntry("Graph"));
            for (Graph graph : Interface.getGraphs()){
                outBuffer.write("startGraph\n");
                strBuilder.setLength(0);
                outBuffer.write(graph.getContent(strBuilder).toString());
                outBuffer.write("endGraph\n\n");
            }
            outBuffer.flush();
            zipOutStream.closeEntry();

            zipOutStream.putNextEntry(new ZipEntry("Panel Tools"));
            for (InterfacePanel tmp : Interface.getTools()){
            	strBuilder.setLength(0);
            	outBuffer.write("startPanel\n");
                outBuffer.write(tmp.getContent(strBuilder).toString());
                outBuffer.write("endPanel\n\n");
            }
            outBuffer.flush();
            zipOutStream.closeEntry();

            zipOutStream.putNextEntry(new ZipEntry("ProjectData"));
            outBuffer.write(saveLineCreator.getSaveLine("name", project.name));
            outBuffer.write(saveLineCreator.getSaveLine("author", project.author));
            outBuffer.write(saveLineCreator.getSaveLine("description", project.description));
            outBuffer.flush();
            zipOutStream.closeEntry();

            outBuffer.close();
            writer.close();
            zipOutStream.close();
            fileOutStream.close();
            currentFile = file;
            logger.info("saved project to \"" + file + '\"');
            addRecentFile(file);
       }catch (IOException e){
            logger.error("Can't save Project to file \"" + file + '\"', e);
	        JOptionPane.showConfirmDialog(null, "Fehler", "Kann Projekt nicht Speichern." , JOptionPane.DEFAULT_OPTION);
        }
    }

    public static final List<String> getRecentFiles(ArrayList<String> list){
      	String value = Options.getString("recent_files");
      	if (value.length() <= 2 || value.charAt(0) != '{' || value.charAt(value.length()-1) != '}')
      		return list;
      	if (list == null)
      		list = new ArrayList<>();
      	StringUtils.split(value, 1, value.length() - 1, ',', false, list);
    	return list;

    }

    public static final String[] getRecentFiles(){
    	List<String> l = getRecentFiles(null);
    	if (l == null)
    		return UniqueObjects.EMPTY_STRING_ARRAY;
    	return l.toArray(new String[l.size()]);
    }

    public synchronized static void addRecentFile (String file){
    	if (file == null)
    		throw new NullPointerException();
    	try{
    		List<String> recentFiles = getRecentFiles(new ArrayList<String>());
    		int index = -1;
	    	for (int i=0;i<recentFiles.size();i++){
	    		String str = recentFiles.get(i);
	    		if (file.equals(str)){
	    			index = i;
	    			break;
	    		}
	    	}
    		if (index != -1)
     			recentFiles.remove(index);
 			else if (recentFiles.size() == 20)
				recentFiles.remove(19);
     		recentFiles.add(0, file);

     		StringBuilder strB = new StringBuilder();
	        strB.append('{').append(recentFiles.get(0));
	        for (int i=1;i<recentFiles.size();i++)
	            strB.append(',').append(recentFiles.get(i));
	        strB.append('}');
	        Options.set("recent_files", strB.toString());
	        Options.triggerUpdates();
    	}catch(Exception e){
    		logger.error("Error at adding recent file: ", e);
    	}
    }

    public static final void reset(Interface window){
        load(DataHandler.getResource("/start.graph"), window);
    }


	public static void addToUpdateTree(JFrame frame) {
		updateUIList.add(new WeakReference<>(frame));
	}
}
