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
package main;

/**
 * Write a description of class Main here.
 * 
 * @author Paul Stahr
 * @version 19.11.2011
 */

import java.awt.EventQueue;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;

import javax.swing.JFrame;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;
import data.DataHandler;
import data.Options;
import data.ProgrammData;
import data.raytrace.RaytraceCommandLine;
import debug.SpeedTests;
import jcomponents.ActivateWindow;
import jcomponents.CalculatorWindow;
import jcomponents.Interface;
import jcomponents.raytrace.RaySimulationGui;
import maths.algorithm.Calculate;
import util.RunTree;
import util.TimedUpdateHandler;

public class Main
{
	static {


		LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
		loggerContext.reset();
		JoranConfigurator configurator = new JoranConfigurator();
		InputStream configStream;
		try {
			configStream = DataHandler.getResourceAsStream("/logback.xml");
			configurator.setContext(loggerContext);
			configurator.doConfigure(configStream); // loads logback file
			configStream.close();
			} catch (IOException e) {
			e.printStackTrace();
		} catch (JoranException e) {
				e.printStackTrace();
			}
		


	}
	
    private static final String neededJavaVersion = "1.7";
	private static boolean activated;
	private static boolean forceActivateWindow = false;
	private static final Logger logger = LoggerFactory.getLogger(Main.class);

	//while(b,set(t,t+0.00001);set(i,int(rand()*(kard(a)-2))+1);set(j,int(rand()*(kard(a[i])-2))+1);set(a[i][j],(a[i-1][j]+a[i][j-1]+a[i+1][j]+a[i][j+1])*0.25+cos(t+(i+j)*0.1)*0.5+if(i<40˄i>30˄j<40˄j>30,c,0)))
	//while(b,while(j<100,set(a[i][j],a[i][j]+aa[i][j]);set(aa[i][j],aa[i][j]-a[i][j]*0.5);set(j,j+1));set(j,0);set(i,(i+1)%100))
    public static final void main (String args[]){
    	//Runtime.getRuntime().traceInstructions(false);
    	//Runtime.getRuntime().traceMethodCalls(false);
    	LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
    	StatusPrinter.print(lc);
    	    
    	String loadProject = null;
    	boolean loadNext = false;
    	boolean console = false;
    	boolean rayconsole = false;
    	boolean calculator = false;
    	boolean raysim = true;
		for (String arg : args){
			if (loadNext){
				loadProject = arg;
			}
			else if (arg.length() >= 2 && arg.charAt(0) == '-' && arg.charAt(1) == '-')
			{
				String sub = arg.substring(2);
				if (sub.equals("help")){
					try {
						InputStream input = DataHandler.getResourceAsStream("help" + '.' + "txt");
						InputStreamReader reader = new InputStreamReader(input);
						BufferedReader inBuf = new BufferedReader(reader);
						String line;
						while ((line = inBuf.readLine()) != null)
						{
							System.out.println(line.replace("$version", ProgrammData.getVersion()).replace("$name", ProgrammData.name));
						}
					} catch (IOException e) {logger.error("Can't print help", e);}
					System.exit(0);
				}else if (sub.equals("activate")){
					forceActivateWindow = true;
				}else if (sub.equals("speedtest")){
					try {
						SpeedTests.test();
					} catch (Exception e) {
						e.printStackTrace();
					}
					return;
				}else if (sub.equals("console")){
					console = true;
				}else if (sub.equals("rayconsole")){
					rayconsole = true;
				}else if (sub.equals("raysim")) {
					raysim = true;
				}else if (sub.equals("load")){
					loadNext = true;
				}else if (sub.equals("calculator")){
					calculator = true;
				}else if (sub.equals("calgraph")){
					raysim = false;
					
				}else if (sub.equals("version")){
					System.out.println("version" + ':' + ProgrammData.getVersion());
					System.exit(0);
				}else if (sub.equals("author")){
					for (String str:ProgrammData.authors)
						System.out.println(str);
					System.exit(0);
				}
					
			}
		}
		//SpeedTests.objectOrganicationTest();
		
		logger.info(new StringBuilder().append("Starting ").append(ProgrammData.name).append(" Version:\"").append(ProgrammData.getVersion()).append('"').toString());
		if (console)
		{
			Console.run();
		}
		else if (rayconsole)
		{
			init();
			try {
				RaytraceCommandLine rcl = new RaytraceCommandLine(System.in, System.out);
				rcl.run();
			} catch (IOException e) {
				logger.error("Error at executing Raytrace Command Line", e);
			}
		}
		else
		{
			new Thread() {
				@Override
				public void run() {
					Console.run();
				}
			}.start();
		}
		if (calculator){
			init();
			CalculatorWindow.getInstance().setVisible(true);
			return;
		}
		try{
			init();
    		if ((!activated && Options.getBoolean("show_actiavation_at_start", true)) || forceActivateWindow){
	    		JFrame frame = ActivateWindow.getInstance();
	    		frame.setVisible(true);
	    		while(frame.isVisible()){
		    		try{
		    			Thread.sleep(10);
		    		}catch(InterruptedException e){}
		    	}
	    	}
    		if (raysim)
    		{
    			RaySimulationGui gui = new RaySimulationGui();
    			gui.setVisible(true);
    		}
    		else
    		{
    			Interface interfaceInstance = Interface.getInstance();
    			interfaceInstance.setVisible(true);
    			if (loadProject != null)
    			{
    				DataHandler.load(loadProject, interfaceInstance);    
    			}
    			else
    			{
    				DataHandler.reset(interfaceInstance);
    			}
    		}
    	}catch(Exception e){
    		logger.error("Program must exit", e);
    		e.printStackTrace();
    		System.exit(-1);
    	}
    		
    }
    
    private static void init(){
    	final UncaughtExceptionHandler exceptionHandler = new UncaughtExceptionHandler(){
    		@Override
    		public void uncaughtException(Thread thr, Throwable e) {
    			logger.error("Can't initialise. Program must exit", e);
        		e.printStackTrace();
        		System.exit(-1);
    		}
    	};
    	
    	RunTree runTree = new RunTree(DataHandler.runnableRunner);
    	final RunTree.RunTreeItem checkVersion = runTree.addRunnable(new Runnable() {
			@Override
			public void run() {
		        final String java_version = System.getProperty(new String("java.version"));
		        logger.debug("System:\"" + System.getProperty("os.name") + "\" Java-version:\"" + java_version + '\"');
		        if (ProgrammData.getLongOfVersion(java_version)<ProgrammData.getLongOfVersion(neededJavaVersion))
		            logger.warn("java version should be >=" + neededJavaVersion + ", Expecting Problems");
			}
		}, "Check Version", exceptionHandler);
     	
    	runTree.addRunnable(new Runnable() {
			@Override
			public void run() {
		    	final OperatingSystemMXBean bean =  ManagementFactory.getOperatingSystemMXBean();
		    	final BigInteger THOUSAND = BigInteger.valueOf(1000);
		    	activated = BigInteger.valueOf(System.getProperty("user.name").hashCode()).multiply(THOUSAND).add(BigInteger.valueOf(bean.getAvailableProcessors())).add(BigInteger.valueOf(bean.getArch().hashCode())).multiply(THOUSAND).add(BigInteger.valueOf(bean.getVersion().hashCode())).multiply(THOUSAND).add(BigInteger.valueOf(System.getProperty("user.home").hashCode())).multiply(THOUSAND).add(BigInteger.valueOf(bean.getName().hashCode())).abs().equals(Options.getBigInteger("product_key", BigInteger.ZERO).modPow(new BigInteger("21f3076c0b61cb3a1a5e71609a3297b11c7750cc9c1b6a30a89b22aa3925b0f495c49a6a0411de8dcb801c8459966496150d77f80173567cda5ba057f210744a3e6bbfe1f98c581cfda68f0cc8076473563d3bfc25937ba445b7a898d3b3acfd2f76e4ad94640b47b522be2cd317a0faf12c1113162adf7f0d2b8277f2f203cb71f071b69bb2345195eec5627bffdc147157b047c0bb223af0cec1545138a199297c948b413fecfe64e6d6f872499506473b99b895d4e2dfd98247a426e1a11aecbf402a3b44d147786561eadc057bc8df85d717e42f8b68afccfac3a036599459d495c874afab1590d0091853fe1357", 16), new BigInteger("4852dabb7b4fb62a131811b43fd58143fa4087cd04cadd1f24827f0d76083807d4bd691c4200fce6e73713b40291f2715cfd202723956ee8e47ab4852861d31def18561c128b22c80e333432a96cebd98c5c32820a39a86a5dd6cedbca527aba875299b60b3219faec366ec6af884e1801b864ac4fb3cec7d116381231641115be48decb59ca5f1bdf8b5014a2e96edb15ad1e5731e546ad29914efcf7e40f2474309dbddd2d9202d0a007698e791c7e1919110d9091b1201d32e7d98c26088c74d25886da4117d83cc1fb6237d941a0b8e2df1332683912ec0623e15f0ef98571dd4daeceb2fc82949681454373f9cb998c8369b1324ed4b7c94815e2f03e7", 16)));
		    	logger.info(activated ? "Activated version" : "Not activated version");
			}
		}, "Check Activation", exceptionHandler, checkVersion);
    	
    	runTree.addRunnable(new Runnable() {
			@Override
			public void run() {
				Calculate.init();
			}
		}, "Init Calculate", exceptionHandler, checkVersion);

    	runTree.addRunnable(
    		new Runnable() {
	    		@Override
				public void run(){
		    		try {
						EventQueue.invokeAndWait(
							new Runnable(){
								@Override
								public void run(){
									final String layoutManager = Options.getString("layout_manager", null);
							    	if (layoutManager != null)
							    		DataHandler.setLookAndFeel(layoutManager);
								}
							}
						);
					} catch (InvocationTargetException e) {
						e.printStackTrace();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
	    		}
	    	},
	    "Init UI", exceptionHandler, checkVersion);

    	try {
			runTree.runAndWait();
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
    	//TODO gets removed
    	final TimedUpdateHandler updater = new TimedUpdateHandler(){
     		int optionCount = -1;
     		long limitMemory = -1;
     		private final Runtime runtime = Runtime.getRuntime();
    		
    		@Override
    		public int getUpdateInterval() {
    			return 1000;
    		}

    		@Override
    		public void update() {
    			if (optionCount != Options.modCount()){
    				optionCount = Options.modCount();
    				limitMemory = Options.getInteger("limit_memory", -1);
    			}			
    			if (limitMemory != -1 && runtime.totalMemory() - runtime.freeMemory() > limitMemory){
    				long usedMemory = runtime.totalMemory() - runtime.freeMemory();
    				System.gc();
    				logger.debug("Gc by Memory-limit, used:" + usedMemory / 1000000f + "MB -> " + (runtime.totalMemory() - runtime.freeMemory())/1000000f + "MB limited to:" + limitMemory / 1000000f + 'M' + 'B');
    			}
    		}
    	};
    	DataHandler.timedUpdater.add(updater);
    }
}
