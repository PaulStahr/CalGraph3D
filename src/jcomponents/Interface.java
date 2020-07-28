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
package jcomponents;


import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.ref.WeakReference;
import java.math.BigInteger;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.GroupLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import javax.swing.filechooser.FileFilter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import data.DataHandler;
import data.GlobalEventListener;
import data.Options;
import data.ProgramIcons;
import data.ProgrammData;
import io.ObjectExporter;
import jcomponents.panels.InterfacePanel;
import jcomponents.panels.InterfacePanelFactory;
import jcomponents.util.AddToolMenuItem;
import jcomponents.util.StandartFCFileFilter;
import maths.variable.VariableStack;
import maths.variable.VariableStack.VariableObserver.PendendList;
import net.ProgramWebServers;
import opengl.OpenGlInterface;
import opengl.RenderingAlgorithms;
import opengl.fallback.FallbackComponent;
import opengl.jogamp.JoglCanvas;
import opengl.lwjgl.LwjglOpenGl;
import scene.Scene;
import util.JFrameUtils;
import util.RunnableSupplier;
import util.StringUtils;
import util.TimedUpdateHandler;
import util.data.SortedIntegerList;
/**
 * This class is the main window of the Project
 * 
 * @author Paul Stahr 
 * @version 1.2
 */
public class Interface extends JFrame implements Graph.GraphListener, ActionListener, Runnable, KeyListener, WindowListener
{   
    private static final long serialVersionUID = 7442248452534299766L;
	private static final Logger logger = LoggerFactory.getLogger(Interface.class);
    private final InterfaceMenuBar menuBar = new InterfaceMenuBar();
    private final JPanel panelTools = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 5));
    private final JButton buttonAddGraph = new JButton(ProgramIcons.addIcon);
	private final JButton buttonDelGraph = new JButton(ProgramIcons.subIcon);
    private final JPanel graphPanel = new JPanel(JFrameUtils.SINGLE_COLUMN_LAYOUT);
    private final JScrollPane graphsScrollPane = new JScrollPane(graphPanel);
    private static boolean activated;
    private final GroupLayout layout;
    private BigInteger product_key = null;
    private static final FileFilter graphFileFilter = new StandartFCFileFilter("Graph-Savefile", "graph", true);
    public static WeakReference<ProjectDataWindow> projectData = null;

   	@Override
	public void clickedUp(Graph graph) {
		setGraphIndex(graph, interfaceObject.graphPanel.getComponentZOrder(graph)-1);
	}
	
	
	@Override
	public void clickedRemove(Graph graph) {
		removeGraph(graph);
	}
	
	
	@Override
	public void clickedDown(Graph graph) {
		setGraphIndex(graph, interfaceObject.graphPanel.getComponentZOrder(graph)+1);
	}

	public static final Scene scene = new Scene();
    public OpenGlInterface openGl;
    private static int updateGraphInterval = -1;
    
    public static final class ListenerSingleton implements Runnable, ActionListener, KeyListener
    {
    	@Override
		public final void run() {
			updateGraphInterval = Options.getInteger("update_graph", 10);
		}
    	
    	@Override
		public final void actionPerformed(ActionEvent e){
            DataHandler.load(e.getActionCommand(), interfaceObject);
        }
    	
    	@Override
		public final void keyPressed(KeyEvent ke) {
            if (ke.isControlDown()){
                if (ke.isShiftDown()){
                    switch (ke.getKeyCode()){
                        case KeyEvent.VK_S:{
                        	JFileChooser fileChooser= new JFileChooser();
                            fileChooser.setFileFilter(graphFileFilter);
                            if(fileChooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION){
                                final String file = fileChooser.getSelectedFile().getPath();
                                DataHandler.save(StringUtils.setFileEnding(file, "graph"));
                            }
                        }
                    }
                }else{
                    switch (ke.getKeyCode()){
                        case KeyEvent.VK_S: save(); break;
                        case KeyEvent.VK_O:{
                        	JFileChooser fileChooser= new JFileChooser();
                            fileChooser.setFileFilter(graphFileFilter);
                            if(fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) 
                                DataHandler.load(fileChooser.getSelectedFile().getPath(), interfaceObject);
                        }
                    }
                }
            }
		}

		
		@Override
		public void keyReleased(KeyEvent arg0) {}

		
		@Override
		public void keyTyped(KeyEvent arg0) {}  	
    }
    
    private static final ListenerSingleton ls = new ListenerSingleton();
    
	static{
		Options.addInvokeModificationListener(ls);
	}
    private final TimedUpdateHandler graphUpdateHandler = new TimedUpdateHandler() {
    	
    	private final VariableStack.VariableObserver variableObserver= DataHandler.globalVariables.createVaribleObserver();
		private int modCount = -1;
		private final PendendList allChangedVariables = variableObserver.getPendentVariableList();
		
		@Override
		public void update() {
			if (DataHandler.globalVariables.modCount() != modCount){
				modCount = DataHandler.globalVariables.modCount();
				variableObserver.updateChanges();
				for (int k=0;k<graphPanel.getComponentCount();k++){
					Graph graph = (Graph)graphPanel.getComponent(k);
					final SortedIntegerList graphVars = graph.getPendentVariables();
					if (allChangedVariables.hasMatch(graphVars))
					{
						graph.updateGraph();
					}
					JLabel labelCalculating = graph.labelCalculating;
					Icon newIcon = graph.isRunning() ? ProgramIcons.iconCalculating : ProgramIcons.iconNotCalculating;
					if (labelCalculating.getIcon() != newIcon)
						labelCalculating.setIcon(newIcon);
				}
			}else{
				for (int k=0;k<graphPanel.getComponentCount();k++){
					Graph graph = (Graph)graphPanel.getComponent(k);
					JLabel labelCalculating = graph.labelCalculating;
					Icon newIcon = graph.isRunning() ? ProgramIcons.iconCalculating : ProgramIcons.iconNotCalculating;
					if (labelCalculating.getIcon() != newIcon)
						labelCalculating.setIcon(newIcon);
				}
			}
		}
		
		
		@Override
		public final int getUpdateInterval() {
			return updateGraphInterval;
		}
	};

    private static Interface interfaceObject;

    public static synchronized Interface getInstance(){
    	if (interfaceObject == null)
    		interfaceObject = new Interface();
    	return interfaceObject;
    }
    
    @Override
	public void actionPerformed(ActionEvent e) {
		Object source = e.getSource();
		if (source == buttonAddGraph)
		{
			final Graph graph = new Graph();
			addGraph(graph);
			graph.openWindow();
		}
		else if (source == buttonDelGraph)
		{
			Graph g = getSelectedGraph();
			if (g!= null)
				removeGraph(g);
		}
	}
    
    @Override
	public void keyReleased(KeyEvent e){}

    
    @Override
	public void keyPressed(KeyEvent e){
			final Graph g = getSelectedGraph();
			if (g != null){
        	switch (e.getKeyCode()){
        		case KeyEvent.VK_ENTER:{
    				g.openWindow();
        			break;
        		}
        		case KeyEvent.VK_DELETE:
        		case KeyEvent.VK_BACK_SPACE:{
    				removeGraph(g);
    				break;
        		}case KeyEvent.VK_UP:{
    				final int pos = graphPanel.getComponentZOrder(g) - 1;
    				if (pos > -1)
    					((Graph)graphPanel.getComponent(pos)).setSelected(true);
        			break;
        		}case KeyEvent.VK_DOWN:{
    				final int pos = graphPanel.getComponentZOrder(g) + 1;
    				if (pos < graphPanel.getComponentCount())
    					((Graph)graphPanel.getComponent(pos)).setSelected(true);
        			break;
        		}
        	}
		}
     }

    
    @Override
	public void keyTyped(KeyEvent e){}

    private Interface(){
    	switch (RenderingAlgorithms.valueOf(Options.getString("rendering_api", "LWJGL")))
    	{
    		case LWJGL:
    			openGl = new LwjglOpenGl(scene);
    			break;
    		case JOGAMP:
    			openGl = JoglCanvas.getInstance(scene);
    			break;
    		default:
    			openGl = new FallbackComponent();
    	}
    	
    	GlobalEventListener.addGlobalKeyListener(ls);

    	Component comp = openGl == null ? new JPanel() : (Component)openGl;
        updateActivation();
        layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        //splitPane.setLeftComponent(panelTools);
        //splitPane.setRightComponent(canvas);
        setJMenuBar(menuBar);
        layout.setVerticalGroup(layout.createSequentialGroup()
            .addGroup(layout.createParallelGroup()
            	.addComponent(panelTools)
            	.addGroup(layout.createSequentialGroup()
            		.addComponent(comp, 10, 100, 10000)
            		.addGroup(layout.createParallelGroup()
            			.addGroup(layout.createSequentialGroup()
            				.addComponent(buttonAddGraph,30,30,30)
            				.addComponent(buttonDelGraph,30,30,30)
            			).addComponent(graphsScrollPane, 100, 100, 100)
            		)
            	)
            )
        );
        
        layout.setHorizontalGroup(layout.createParallelGroup()
           	.addGroup(layout.createSequentialGroup()
           		.addComponent(panelTools,315,315,315)
           		.addGroup(layout.createParallelGroup()
           			.addComponent(comp, 10, 100, 10000)
            		.addGroup(layout.createSequentialGroup()
                		.addGroup(layout.createParallelGroup()
                    		.addComponent(buttonAddGraph,30,30,30)
                    		.addComponent(buttonDelGraph,30,30,30)
                    	).addComponent(graphsScrollPane)
            		)
           		)
           	)
        );
        //splitPane.setDividerLocation(0.5);
        //splitPane.setOneTouchExpandable(true);
        graphPanel.setFocusable(true);      
        graphPanel.addKeyListener(this);
        
        buttonAddGraph.setToolTipText("Einen neuen Graphen hinzuf\u00FCgen.");
        buttonAddGraph.addActionListener(this);       
        
        buttonDelGraph.setToolTipText("Den ausgew\u00E4hlten Grahpen l\u00F6schen.");
        buttonDelGraph.addActionListener(this);
         
        setMinimumSize(new Dimension(400, 300));
        setSize(900, 700);
        setTitle(ProgrammData.name);
        setResizable(true);
        
        setIconImage(ProgramIcons.logoIcon.getImage());
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
       	addWindowListener(this);
       	
       	DataHandler.addToUpdateTree(this);
    	DataHandler.timedUpdater.add(graphUpdateHandler);
		
		for (int i=0;i<menuBar.getComponentCount();i++){
			Component c = menuBar.getComponent(i);
			if (c instanceof JMenu)
				addRedrawListener((JMenu)c);
		}
		Options.addInvokeModificationListener(this);
    }
    
    @Override
	public void run() {
		OpenGlInterface tmp = openGl;
		switch (RenderingAlgorithms.valueOf(Options.getString("rendering_api", "LWJGL")))
    	{
    		case LWJGL:
    			if (!(openGl instanceof LwjglOpenGl))
    			{
    				openGl = new LwjglOpenGl(scene);
    			}
    			break;
    		case JOGAMP:
    			if (!(openGl instanceof JoglCanvas))
    			{
    				openGl = JoglCanvas.getInstance(scene);
    			}
    			break;
    		default:
    			if (!(openGl instanceof FallbackComponent))
    			{
    				openGl = new FallbackComponent();
    			}
    	}
		if (tmp != openGl)
		{
			layout.replace((Component)tmp, (Component)openGl);
		}
		BigInteger tmp_key = Options.getBigInteger("product_key", BigInteger.ZERO);
		if (tmp_key != null && !tmp_key.equals(product_key))
		{
			product_key = tmp_key;
			updateActivation();
		}
	}
    
    public final String runCommand(String line){
    	ArrayList<String> l = new ArrayList<String>();
    	boolean isStr = false;
    	int lastIndex = 0;
    	for (int i=0;i<line.length();i++){
    		switch(line.charAt(i)){
    			case '"':{
    				isStr = !isStr;
    			}case ' ':{
    				if (!isStr){
    					l.add(line.substring(lastIndex, i));
    					lastIndex = i+1;
    				}
    			}
    		}
    	}
    	l.add(line.substring(lastIndex, line.length()));
    	String command = l.get(0);
    	switch (l.size()){
	    	case 1:{
		    	if (command.equals("get_version"))
		    		return ProgrammData.getVersion();
		    	if (command.equals("get_graph_count"))
		    		return String.valueOf(graphCount());
	    	}case 2:{
	        	if (line.equals("get_graph"))
	        		return ((Graph)graphPanel.getComponent(Integer.parseInt(l.get(1)))).getContent();
	        	if (line.equals("get_tool"))
	        		return ((InterfacePanel)panelTools.getComponent(Integer.parseInt(l.get(1)))).getContent();
	    	}
    	}  	
    	return "command not found";
    }
    
    private static final void addRedrawListener(JMenu menu){
    	menu.addPropertyChangeListener("ancestor", JFrameUtils.sourceRedrawMenuItemPropertyListener);
    	for (int i=0;i<menu.getMenuComponentCount();i++){
    		Component c = menu.getMenuComponent(i);
    		if (c instanceof JMenu)
        		addRedrawListener((JMenu)c);
    	}
    }
    
    public final int graphCount(){
    	return graphPanel.getComponentCount();
    }
    
    public static void exit(){
        if (activated){
        	switch(JOptionPane.showConfirmDialog(null, "M\u00F6chten Sie dieses Projekt speichern bevor es geschlossen wird?","Terminierung", JOptionPane.YES_NO_CANCEL_OPTION)){
            	case JOptionPane.YES_OPTION : save();
            	case JOptionPane.NO_OPTION : Interface.interfaceObject.openGl.dispose();System.exit(0);break;
        	}
        }else{
           	switch(JOptionPane.showConfirmDialog(null, "Wirklich beenden?","Terminierung", JOptionPane.YES_NO_OPTION)){
              	case JOptionPane.YES_OPTION : Interface.interfaceObject.openGl.dispose(); System.exit(0);break;
               	case JOptionPane.NO_OPTION : break;
           	}
    	}    	
    }
    
    public final void updateActivation(){
		final OperatingSystemMXBean bean =  ManagementFactory.getOperatingSystemMXBean();
		final BigInteger THOUSAND = BigInteger.valueOf(1000);
		activated = BigInteger.valueOf(
				System.getProperty("user.name").hashCode())
				.multiply(THOUSAND)
				.add(BigInteger.valueOf(bean.getAvailableProcessors()))
				.add(BigInteger.valueOf(bean.getArch().hashCode()))
				.multiply(THOUSAND)
				.add(BigInteger.valueOf(bean.getVersion().hashCode()))
				.multiply(THOUSAND)
				.add(BigInteger.valueOf(System.getProperty("user.home").hashCode()))
				.multiply(THOUSAND)
				.add(BigInteger.valueOf(bean.getName().hashCode()))
				.abs()
				.equals(Options.getBigInteger("product_key", BigInteger.ZERO).modPow(new BigInteger("21f3076c0b61cb3a1a5e71609a3297b11c7750cc9c1b6a30a89b22aa3925b0f495c49a6a0411de8dcb801c8459966496150d77f80173567cda5ba057f210744a3e6bbfe1f98c581cfda68f0cc8076473563d3bfc25937ba445b7a898d3b3acfd2f76e4ad94640b47b522be2cd317a0faf12c1113162adf7f0d2b8277f2f203cb71f071b69bb2345195eec5627bffdc147157b047c0bb223af0cec1545138a199297c948b413fecfe64e6d6f872499506473b99b895d4e2dfd98247a426e1a11aecbf402a3b44d147786561eadc057bc8df85d717e42f8b68afccfac3a036599459d495c874afab1590d0091853fe1357", 16), new BigInteger("4852dabb7b4fb62a131811b43fd58143fa4087cd04cadd1f24827f0d76083807d4bd691c4200fce6e73713b40291f2715cfd202723956ee8e47ab4852861d31def18561c128b22c80e333432a96cebd98c5c32820a39a86a5dd6cedbca527aba875299b60b3219faec366ec6af884e1801b864ac4fb3cec7d116381231641115be48decb59ca5f1bdf8b5014a2e96edb15ad1e5731e546ad29914efcf7e40f2474309dbddd2d9202d0a007698e791c7e1919110d9091b1201d32e7d98c26088c74d25886da4117d83cc1fb6237d941a0b8e2df1332683912ec0623e15f0ef98571dd4daeceb2fc82949681454373f9cb998c8369b1324ed4b7c94815e2f03e7", 16)));	
        menuBar.updateActivation();
    }
    
    private static final void setGraphIndex(Graph g, int index){
    	final int count = interfaceObject.graphPanel.getComponentCount();
    	if (index < 0)
    		index = 0;
    	if (index >= count)
    		index = count-1;
    	interfaceObject.graphPanel.setComponentZOrder(g, index);
    	interfaceObject.graphPanel.revalidate();
    }

    public static final Graph[] getGraphs(){
        final Graph graph[] = new Graph[interfaceObject.graphPanel.getComponentCount()];
        for (int i=0;i<graph.length;i++)
        	graph[i] = (Graph) interfaceObject.graphPanel.getComponent(i);
        return graph;
    }

    public static final void addGraph(Graph graph){
    	if (graph == null)
    		throw new IllegalArgumentException();
    	graph.addGraphListener(Interface.interfaceObject);
        interfaceObject.graphPanel.add(graph);
        interfaceObject.graphPanel.validate();
    }
    
    public final void removeGraph(Graph graph){
    	if (graph == null)
    		throw new NullPointerException();
		final int pos = interfaceObject.graphPanel.getComponentZOrder(graph);
	   	interfaceObject.graphPanel.remove(graph);
	   	graph.destroy();
 		final int count = interfaceObject.graphPanel.getComponentCount();
		if (pos < count)
			((Graph)interfaceObject.graphPanel.getComponent(pos)).setSelected(true);
		else if (count > 0)
			((Graph)interfaceObject.graphPanel.getComponent(count - 1)).setSelected(true);
    	interfaceObject.graphPanel.validate();
    	interfaceObject.graphPanel.repaint();
    	interfaceObject.openGl.repaint();
    }

    public final void removeAllGraphs(){
    	for (Graph g : getGraphs())
    		g.destroy();
        interfaceObject.graphPanel.removeAll();
        interfaceObject.graphPanel.validate();
        interfaceObject.graphPanel.repaint();
    }

    public final Graph getSelectedGraph(){
        for (int i=0;i<interfaceObject.graphPanel.getComponentCount();i++){
        	Graph graph = (Graph)interfaceObject.graphPanel.getComponent(i);
            if (graph.isSelected())
                return graph;
        }
        return null;
    }

    public final void openWindow(){
    	setVisible(true);
        setLocationRelativeTo(null);
    }

    private static final void save(){
        if (DataHandler.save())
            return;
        JFileChooser fileChooser= new JFileChooser();
        fileChooser.setFileFilter(graphFileFilter);
        if(fileChooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION){
            final String file = fileChooser.getSelectedFile().getPath();
            DataHandler.save(StringUtils.setFileEnding(file, "graph"));
        }
    }
    
    public static final void addTool(InterfacePanel panel){
    	interfaceObject.panelTools.add(panel);
    	interfaceObject.panelTools.revalidate();
    }
    
    public final void removeTool(InterfacePanel panel){
    	panelTools.remove(panel);
    	panel.destroy();
    	panelTools.revalidate();
    	panelTools.repaint();
    }
    
    public final void removeAllTools(){
    	for (InterfacePanel tool : getTools())
    		tool.destroy();
    	interfaceObject.panelTools.removeAll();
    	interfaceObject.panelTools.revalidate();
    	interfaceObject.panelTools.repaint();
   }
    
    public static InterfacePanel[] getTools (){
     	final InterfacePanel p[] = new InterfacePanel[interfaceObject.panelTools.getComponentCount()];
    	for (int i=0;i<p.length;i++)
    		p[i] = (InterfacePanel) interfaceObject.panelTools.getComponent(i);
     	return p;
    }
    
    private class InterfaceMenuBar extends JMenuBar implements ActionListener{
		private static final long serialVersionUID		= 8614644295295574500L;		
		private final FileMenu fileMenu            		= new FileMenu("Datei");

        private final JMenu visionMenu               	= new JMenu("Ansicht");
        private final JMenuItem showToolsMenuItem    	= new JMenuItem("Tools ausblenden");
        private final JMenuItem showGraphMenuMenuItem	= new JMenuItem("Graphmen\u00FC ausblenden");
        private final JMenuItem showLogMenuItem			= new JMenuItem("Log anzeigen");
        
        private final EditMenu editMenu                	= new EditMenu("Bearbeiten");
        private final ExtraMenu extraMenu              	= new ExtraMenu("Extras");
        private final JMenuItem newVersionAviableMenuItem = new JMenuItem("Neue Version Verf\u00FCgbar!");
        private final JMenuItem activateProductMenuItem	= new JMenuItem("Produkt aktivieren");

		@Override
		public void actionPerformed(ActionEvent ae){
			Object source = ae.getSource();
			if (source == showToolsMenuItem)
			{
				final boolean visible = !interfaceObject.panelTools.isVisible();
	        	showToolsMenuItem.setText(visible ? "Tools ausblenden" : "Tools anzeigen");
	        	interfaceObject.panelTools.setVisible(visible);		
			}
			else if (source == showGraphMenuMenuItem)
			{
				final boolean visible = !interfaceObject.graphsScrollPane.isVisible();
            	showGraphMenuMenuItem.setText(visible ? "Graphmen\u00FC ausblenden" : "Graphmen\u00FC anzeigen");
            	interfaceObject.graphsScrollPane.setVisible(visible);
            	interfaceObject.buttonAddGraph.setVisible(visible);
            	interfaceObject.buttonDelGraph.setVisible(visible);
			}
        }   

        private InterfaceMenuBar(){
            showToolsMenuItem.addActionListener(this);
            visionMenu.add(showToolsMenuItem);
            showGraphMenuMenuItem.addActionListener(this);
            visionMenu.add(showGraphMenuMenuItem);

            showLogMenuItem.addActionListener(LogWindow.getOpenWindowListener());
	        visionMenu.add(showLogMenuItem);
	         
	        newVersionAviableMenuItem.setVisible(false);
	        newVersionAviableMenuItem.setForeground(Color.RED);
	        newVersionAviableMenuItem.addActionListener(CheckVersion.getOpenWindowListener());         
	        activateProductMenuItem.setForeground(Color.RED);
	        activateProductMenuItem.addActionListener(ActivateWindow.getOpenWindowListener());
	        add(fileMenu);
	        add(visionMenu);
	        add(editMenu);
	        add(extraMenu);
	        add(activateProductMenuItem);
	        add(newVersionAviableMenuItem);
	        
			if (Options.getBoolean("auto_check_updates", true)){
				DataHandler.runnableRunner.run(new Runnable(){
					@Override
					public void run(){
						try {
							ProgramWebServers.Changelog chLog = ProgramWebServers.getChangelog();
							if (chLog == null)
								return;
							String str = ProgramWebServers.getChangelog().version;
							if (str == null)
								return;
							if (ProgrammData.isNewer(str)){
								logger.info("new Version aviable: ".concat(str));
								newVersionAviableMenuItem.setVisible(true);
							}else{
								logger.info("Version is up to date");    						
							}
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}, "Check for Updates");
			}
	    }
	    
	    public void updateActivation(){
	    	fileMenu.updateActivation();
	       	activateProductMenuItem.setVisible(!activated);
	       	editMenu.updateActivation();
	    }
    }

    private class FileMenu extends JMenu implements ActionListener, MenuListener{
		private static final long serialVersionUID = -7736434672728160521L;
		private final JMenuItem newFileMenuItem      	= new JMenuItem("New...",           ProgramIcons.newFileIcon);
        private final JMenuItem openMenuItem         	= new JMenuItem("Open",      ProgramIcons.openFileIcon);
        private final JMenu openRecentMenu           	= new JMenu("Recently opened");
        private final JMenuItem recentFilesMenuItem[] 	= new JMenuItem[10];
        private final JMenu exampleMenu					= new JMenu("Examples");
        private final JMenuItem saveMenuItem         	= new JMenuItem("Save",        ProgramIcons.saveFileIcon);
        private final JMenuItem saveAtMenuItem       	= new JMenuItem("Save to...",     ProgramIcons.saveFileAtIcon);
        private final JMenu fileExportMenu          	= new JMenu("Export");
        private final JMenuItem exportToPictureMenuItem = new JMenuItem("Image");
        private final JMenuItem exportToOffMenuItem  	= new JMenuItem("DEC Object (OFF)");
        private final JMenuItem exportToObjMenuItem		= new JMenuItem("Wavefront (OBJ)");
        private final JMenuItem exitMenuItem         	= new JMenuItem("Beenden");

        private FileMenu(){
    		this(StringUtils.EMPTY);
    	}
        
        private class ExampleMenuItem extends JMenuItem implements ActionListener
        {
        	/**
			 * 
			 */
			private static final long serialVersionUID = 2834104418330789411L;
			private final String file;
        	
        	public ExampleMenuItem(String file) {
        		super (file.substring(0, file.lastIndexOf('.')));
				this.file = file;
				addActionListener(this);
			}
        	
			@Override
			public void actionPerformed(ActionEvent e) {
				DataHandler.load(DataHandler.getResource("example_projects/" + file), interfaceObject);
			}
        }
    	
        @Override
		public void menuSelected(MenuEvent arg0) {
			update();
		}
		
		@Override
		public void menuDeselected(MenuEvent arg0) {}
		
		@Override
		public void menuCanceled(MenuEvent arg0) {}
        
    	private FileMenu(String arg){
    		setText(arg);
            for (int i=0;i<recentFilesMenuItem.length;i++)
             	openRecentMenu.add(recentFilesMenuItem[i] = new JMenuItem()).addActionListener(ls);
        	
            add(newFileMenuItem).addActionListener(this);
            newFileMenuItem.setMnemonic('N');
            add(openMenuItem).setToolTipText("Ein gespeichertes Projekt \u00F6ffnen");
            openMenuItem.addActionListener(this);
            openMenuItem.setMnemonic('O');
            add(saveMenuItem).setToolTipText("Das Projekt in der aktuellen Datei speichern");
            saveMenuItem.addActionListener(this);
            saveMenuItem.setMnemonic('S');
            add(saveAtMenuItem).setToolTipText("Das projekt an einem bestimmten Ort speichern");
            saveAtMenuItem.addActionListener(this);
            fileExportMenu.add(exportToPictureMenuItem).setToolTipText("Die aktuelle Ansicht als Bild exportieren");
            fileExportMenu.setMnemonic('E');
            exportToPictureMenuItem.addActionListener(this);
            exportToPictureMenuItem.setMnemonic('I');
            fileExportMenu.add(exportToOffMenuItem).setToolTipText("Einen Graphen als 3D-Datei exportieren");
            exportToOffMenuItem.addActionListener(this);      
            fileExportMenu.add(exportToObjMenuItem).addActionListener(this);
 
            add(openRecentMenu).setIcon(ProgramIcons.historyIcon);
            openRecentMenu.addMenuListener(this);
            
            add(exampleMenu);
            add(fileExportMenu);
            add(exitMenuItem).addActionListener(this);
            
    		try {
        		URL url = DataHandler.getResource("example_projects/files.txt");
        		InputStream stream = url.openStream();
        		InputStreamReader reader = new InputStreamReader(stream);
				BufferedReader inBuf = new BufferedReader(reader);
	    		while (true){
	    			final String line = inBuf.readLine();
	    			if (line == null)
	    				break;
	    			if (line != "")
	    			{
		    			exampleMenu.add(new ExampleMenuItem(line));
	    			}
	    		}
	    		inBuf.close();
	    		reader.close();
	    		stream.close();
    		} catch (Exception e) {
    			logger.error("Can't read example files", e);
    		}
    	}
    	
    	private void updateActivation(){
            openMenuItem.setEnabled(activated);
            openRecentMenu.setEnabled(activated);
            saveMenuItem.setEnabled(activated);
            saveAtMenuItem.setEnabled(activated);
            fileExportMenu.setEnabled(activated);
    	}
    	
    	private void update(){
        	final List<String> recentFiles = DataHandler.getRecentFiles(new ArrayList<String>());
            for (int i=0;i<recentFiles.size();i++){
            	final JMenuItem menuItem = recentFilesMenuItem[i];
            	final String str = recentFiles.get(i);
            	final File file = new File (str);
            	final boolean valid = file.exists() && !file.isDirectory();
            	if (valid != menuItem.isEnabled()){
	        		menuItem.setIcon(valid ? null : ProgramIcons.deletedIcon);
	        		menuItem.setEnabled(valid);
            	}
        		menuItem.setVisible(true);
        		menuItem.setText(str);
            }
            for (int i=recentFiles.size();i<recentFilesMenuItem.length;i++)
            	recentFilesMenuItem[i].setVisible(false);
    	}

    	@Override
		public void actionPerformed(ActionEvent ae){
        	Object source = ae.getSource();
        	if (source == newFileMenuItem)
        	{
        		DataHandler.reset(interfaceObject);
        		System.gc();
        	}
        	else if (source == openMenuItem)
        	{
        		JFileChooser fileChooser= new JFileChooser();
                fileChooser.setFileFilter(graphFileFilter);
                if(fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) 
                    DataHandler.load(fileChooser.getSelectedFile().getPath(), interfaceObject);
        	}else if (ae.getSource() == saveMenuItem)
        	{
	             save();
            }
        	else if (source == saveAtMenuItem)
        	{
        		JFileChooser fileChooser= new JFileChooser();
                fileChooser.setFileFilter(graphFileFilter);
                if(fileChooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION){
                    DataHandler.save(StringUtils.setFileEnding(fileChooser.getSelectedFile().getPath(), "graph"));
                }
        	}else if (source == exportToPictureMenuItem)
        	{
        		final OperatingSystemMXBean bean =  ManagementFactory.getOperatingSystemMXBean();
        		final BigInteger THOUSAND = BigInteger.valueOf(1000);
        		final boolean activated = BigInteger
        				.valueOf(System.getProperty("user.name").hashCode())
        				.multiply(THOUSAND)
        				.add(BigInteger.valueOf(bean.getAvailableProcessors()))
        				.add(BigInteger.valueOf(bean.getArch().hashCode()))
        				.multiply(THOUSAND)
        				.add(BigInteger.valueOf(bean.getVersion().hashCode()))
        				.multiply(THOUSAND)
        				.add(BigInteger.valueOf(System.getProperty("user.home").hashCode()))
        				.multiply(THOUSAND)
        				.add(BigInteger.valueOf(bean.getName().hashCode()))
        				.abs()
        				.equals(Options.getBigInteger("product_key", BigInteger.ZERO).modPow(new BigInteger("21f3076c0b61cb3a1a5e71609a3297b11c7750cc9c1b6a30a89b22aa3925b0f495c49a6a0411de8dcb801c8459966496150d77f80173567cda5ba057f210744a3e6bbfe1f98c581cfda68f0cc8076473563d3bfc25937ba445b7a898d3b3acfd2f76e4ad94640b47b522be2cd317a0faf12c1113162adf7f0d2b8277f2f203cb71f071b69bb2345195eec5627bffdc147157b047c0bb223af0cec1545138a199297c948b413fecfe64e6d6f872499506473b99b895d4e2dfd98247a426e1a11aecbf402a3b44d147786561eadc057bc8df85d717e42f8b68afccfac3a036599459d495c874afab1590d0091853fe1357", 16), new BigInteger("4852dabb7b4fb62a131811b43fd58143fa4087cd04cadd1f24827f0d76083807d4bd691c4200fce6e73713b40291f2715cfd202723956ee8e47ab4852861d31def18561c128b22c80e333432a96cebd98c5c32820a39a86a5dd6cedbca527aba875299b60b3219faec366ec6af884e1801b864ac4fb3cec7d116381231641115be48decb59ca5f1bdf8b5014a2e96edb15ad1e5731e546ad29914efcf7e40f2474309dbddd2d9202d0a007698e791c7e1919110d9091b1201d32e7d98c26088c74d25886da4117d83cc1fb6237d941a0b8e2df1332683912ec0623e15f0ef98571dd4daeceb2fc82949681454373f9cb998c8369b1324ed4b7c94815e2f03e7", 16)));
        		if (!activated)
        			return;
        		JFileChooser fileChooser= new JFileChooser();
                fileChooser.setFileFilter(new StandartFCFileFilter("Bild", ImageIO.getWriterFormatNames(), true));
                if(fileChooser.showSaveDialog(null) != JFileChooser.APPROVE_OPTION)
                	return;
                final File file = fileChooser.getSelectedFile();
                String path = file.getPath();
                String formatName = StringUtils.getFileType(path);
                if (formatName == null){
                	formatName = "png";
                	path = path + '.' + formatName;
                }
                try{
                    if (!ImageIO.write(interfaceObject.openGl.getScreenshot(), formatName, new File(path))){
                    	logger.error("Can't write image, format \"" + formatName + "\" is not supported.", Interface.class);
                    	JOptionPane.showConfirmDialog(null, "Fehler", "Das Dateiformat \""+ formatName + "\" wird nicht unterst\u00FCtzt." , JOptionPane.DEFAULT_OPTION);
                	}
                }catch (IOException e){
                	logger.error("Can't write image:",e, Interface.class);
                	JOptionPane.showConfirmDialog(null, "Fehler", "Problem beim schreiben der Datei" , JOptionPane.DEFAULT_OPTION);
                }	
        	}
        	else if (source == exportToOffMenuItem)
        	{
        		JFileChooser fileChooser= new JFileChooser();
                fileChooser.setFileFilter(new StandartFCFileFilter("Open File Format", "off", true));
                if(fileChooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION){
                    final Graph graph = getSelectedGraph();
                    if (graph != null)
                    {
                    	try{
                    	ObjectExporter.export(
                    			ObjectExporter.OFF,
                    			StringUtils.setFileEnding(fileChooser.getSelectedFile().getPath(), "off"),
                    			graph.getGlObject());
                    	}catch(IOException e)
                    	{
                    		logger.error("IO-Error at exporting Object", e);                            		
                    	}
                    }
                }
        	}
        	else if (source == exportToObjMenuItem)
        	{
        		JFileChooser fileChooser= new JFileChooser();
            	fileChooser.setFileFilter(new StandartFCFileFilter("Object Format", "obj", true));
                if(fileChooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION){
                    final Graph graph = getSelectedGraph();
                    if (graph != null)
                    {
                    	try{
                    	ObjectExporter.export(
                    			ObjectExporter.OBJ,
                    			StringUtils.setFileEnding(fileChooser.getSelectedFile().getPath(), "obj"),
                    			graph.getGlObject());
                    	}catch(IOException e)
                    	{
                    		logger.error("IO-Error at exporting Object", e);
                    	}
                    }                      
                }
        	}
        	else if (source == exitMenuItem)
        	{
        		exit();
        	}
        }
    }
    
    private class EditMenu extends JMenu implements ActionListener{
		private static final long serialVersionUID = 3699845212654690226L;
		private final JMenuItem copyMenuItem		= new JMenuItem("Kopieren");
        private final JMenuItem cutMenuItem			= new JMenuItem("Ausschneiden");
        private final JMenuItem pasteMenuItem		= new JMenuItem("Einf\u00FCgen");
        private final AddToolMenu addToolMenu 		= new AddToolMenu("Panel Hinzuf\u00FCgen");
        private final JMenuItem projectOptionsMenuItem = new JMenuItem("Projekteinstellungen");

        private EditMenu(){
        	this(StringUtils.EMPTY);
        }
        
    	private EditMenu(String arg){
    		super(arg);
    		
            add(copyMenuItem).addActionListener(this);
            copyMenuItem.setMnemonic('C');
            add(cutMenuItem).addActionListener(this);
            cutMenuItem.setMnemonic('X');
            add(pasteMenuItem).addActionListener(this);
            pasteMenuItem.setMnemonic('P');
    		add(addToolMenu);
            add(projectOptionsMenuItem).addActionListener(this);
    	}
    	
    	public void updateActivation(){
            copyMenuItem.setEnabled(activated);
            cutMenuItem.setEnabled(activated);
            pasteMenuItem.setEnabled(activated);
    	}

		@Override
		public void actionPerformed(ActionEvent e) {
			Object source = e.getSource();
			if (source == copyMenuItem)
			{
				if (!activated)
					return;
				final Graph g = getSelectedGraph();
				if (g != null)
					Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(g.getContent()), null);
			}
			else if (source == cutMenuItem)
			{
				if (!activated)
					return;
				final Graph g = getSelectedGraph();
				if (g == null)
					return;
				Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(g.getContent()), null);
				removeGraph(g);
			}
			else if (source == pasteMenuItem)
			{
				if (!activated)
        			return;
        		final Clipboard systemClipboard = Toolkit.getDefaultToolkit().getSystemClipboard(); 
        		final Transferable transferData = systemClipboard.getContents( null ); 
    			try{
    				for(DataFlavor dataFlavor : transferData.getTransferDataFlavors()){ 
    					Object content = transferData.getTransferData(dataFlavor); 
    					if (content instanceof String){ 
    						final Graph g = new Graph();
    						g.setContent((String)content);
    						addGraph(g);
    						break;
    					}
    				}
    			}catch(IOException ex){}
    			catch(UnsupportedFlavorException ex){}
			}
			else if (source == projectOptionsMenuItem)
			{
				ProjectDataWindow window = projectData == null ? null : projectData.get();
				if (window == null){
					RunnableSupplier<ProjectDataWindow> supplier = new RunnableSupplier<ProjectDataWindow>() {
						@Override
						public void run() {
							set(new ProjectDataWindow(DataHandler.project));
						}
					};
		    		JFrameUtils.runByDispatcherAndWait(supplier);
		    		projectData = new WeakReference<ProjectDataWindow>(window = supplier.get());
		    	}
		    	window.setVisible(true);
			}
		}
    }
    
    private static class AddToolMenu extends JMenu implements ActionListener{
		private static final long serialVersionUID = -4848835494068400441L;
    	    	
    	private AddToolMenu(String name){
    		super(name);
    		
    		add(new AddToolMenuItem("Perspektive", InterfacePanelFactory.PERSPECTIVE));
    		add(new AddToolMenuItem("Code Pad", InterfacePanelFactory.CODE_PAD));
    		add(new AddToolMenuItem("Code Pad2", InterfacePanelFactory.CODE_PAD2));
    		add(new AddToolMenuItem("Slider", InterfacePanelFactory.SLIDER));
    		add(new AddToolMenuItem("Variablen", InterfacePanelFactory.VARIABLE));
    		add(new AddToolMenuItem("Animation", InterfacePanelFactory.ANIMATION));
    		add(new AddToolMenuItem("Programm Editor", InterfacePanelFactory.PROGRAM));
    		
    		for (int i = 0; i < getItemCount(); ++i)
    		{
    			((AddToolMenuItem)getItem(i)).addActionListener(this);
    		}
    	}

    	@Override
		public void actionPerformed(ActionEvent ae) {
    		AddToolMenuItem source = (AddToolMenuItem)ae.getSource();
			addTool(InterfacePanelFactory.create(source.panel, DataHandler.globalVariables));
		}

    }

    private static class ExtraMenu extends JMenu implements ActionListener{
		private static final long serialVersionUID = -8023329592087073437L;
		
		private final JMenuItem helpMenuItem		= new JMenuItem("Hilfe", 						ProgramIcons.iconHelp);
        private final JMenuItem versionMenuItem		= new JMenuItem("Auf Updates pr\u00FCfen",		ProgramIcons.webIcon);
        private final JMenuItem characterMenuItem	= new JMenuItem("Zeichentabelle");
        private final JMenuItem calculatorMenuItem	= new JMenuItem("Taschenrechner");
        private final JMenuItem matrixMenuItem		= new JMenuItem("Matrix-Erstellen");
        private final JMenuItem optionsMenuItem		= new JMenuItem("Einstellungen",				ProgramIcons.settingsIcon);
        private final LicenseMenu licenseMenu		= new LicenseMenu("Lizenzen");
        private final JMenuItem creditsMenuItem		= new JMenuItem("\u00DCber...");
    	
		private ExtraMenu (){
			this(StringUtils.EMPTY);
		}
		
		private ExtraMenu(String arg){
			super(arg);
           
            add(helpMenuItem).addActionListener(this);
            helpMenuItem.setMnemonic('H');
            add(characterMenuItem).addActionListener(CharacterTable.getOpenWindowListener());
            add(matrixMenuItem).addActionListener(TransformationWindow.getOpenWindowListener());
            add(calculatorMenuItem).addActionListener(CalculatorWindow.getOpenWindowListener());
            add(versionMenuItem).addActionListener(CheckVersion.getOpenWindowListener());
            add(optionsMenuItem).addActionListener(InterfaceOptions.getOpenWindowListener());
            add(licenseMenu);
            add(creditsMenuItem).addActionListener(Credits.getOpenWindowListener());  			
		}

		@Override
		public void actionPerformed(ActionEvent arg0) {
			Object source = arg0.getSource();
			if (source == helpMenuItem)
			{
				try{
	            	if (System.getProperty("os.name").toUpperCase().contains("NUX"))
	                	Desktop.getDesktop().open(new File("../data/dokumentation.pdf"));
	            	else
	            		Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler file:../data/dokumentation.pdf");
	            }catch(IOException e){
	            	logger.error("Can't open documentation", e);
	            }
			}
		}
    }

    @Override
	public void windowClosing(WindowEvent e) {
		exit();
	}
	
	@Override
	public void windowActivated(WindowEvent arg0) {
		openGl.repaint();
	}

	@Override
	public void windowClosed(WindowEvent e) {}

	@Override
	public void windowDeactivated(WindowEvent e) {}

	@Override
	public void windowDeiconified(WindowEvent e) {}

	@Override
	public void windowIconified(WindowEvent e) {}

	@Override
	public void windowOpened(WindowEvent e) {}
}
