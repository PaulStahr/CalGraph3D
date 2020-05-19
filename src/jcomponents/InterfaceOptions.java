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
import java.awt.EventQueue;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

import javax.swing.ButtonGroup;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.UIManager.LookAndFeelInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import data.DataHandler;
import data.Options;
import data.Options.OptionTreeInnerNode;
import data.ProgramIcons;
import jcomponents.util.JColorPickPanel;
import jcomponents.util.JComponentSingletonInstantiator;
import opengl.RenderingAlgorithms;
import scene.Scene;
import util.JFrameUtils;
import util.SaveLineCreator;
import util.SaveLineCreator.SaveObject;
/** 
* @author  Paul Stahr
* @version 04.02.2012
*/
public class InterfaceOptions extends JFrame implements ActionListener
{
    private static final long serialVersionUID = -8212955713307107978L;
	private static final Logger logger = LoggerFactory.getLogger(InterfaceOptions.class);
	public static final JComponentSingletonInstantiator<InterfaceOptions> instantiator = new JComponentSingletonInstantiator<InterfaceOptions>(InterfaceOptions.class);
	
    private final JTabbedPane tapPane = new JTabbedPane();

    private final JPanelVision panelVision = new JPanelVision();
    private final JPanelLightEnvirontment panelLightEnvironment = new JPanelLightEnvirontment();
    private final JPanelExtended panelExtended = new JPanelExtended();
    
    private final JButton buttonOkay             = new JButton("OK");
    private final JButton buttonAccept           = new JButton("\u00DCbernehmen");
    private final JButton buttonCancel           = new JButton("Abbrechen");

    public static abstract class OptionPanel extends JPanel
    {
    	/**
		 * 
		 */
		private static final long serialVersionUID = 1529318382175030509L;

		public abstract void save();
		
		public abstract void load();
    }
    
    public static final ActionListener getOpenWindowListener()
    {
    	return instantiator;
    }
    
    public static final synchronized JFrame getInstance(){
    	return instantiator.get();
    }
    
    @Override
	public void actionPerformed(ActionEvent e){
    	Object source = e.getSource();
    	if (source == buttonOkay)
    	{
    		acceptChanges();
    		dispose();
    	}
    	else if (source == buttonAccept)
    	{
            acceptChanges();
    	}
    }
    
    public void addOptionPanel(String name, OptionPanel op)
    {
    	if (!JFrameUtils.contains(tapPane, op))
    	{
    		tapPane.add(name, op);
    	}
    }
    
    public InterfaceOptions(){
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.weightx =1;
        gbc.weighty =1;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridwidth = GridBagConstraints.REMAINDER;        
        add(tapPane, gbc);

        tapPane.add("Bild",panelVision);
        tapPane.add("Licht/Umgebung", panelLightEnvironment);
        tapPane.add("Erweitert", panelExtended);
                
        buttonOkay.addActionListener(this);
        gbc = new GridBagConstraints();
        gbc.weightx =1;
        gbc.weighty =0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5,5,5,5);
        add(buttonOkay, gbc);

        buttonAccept.addActionListener(this);
        add(buttonAccept, gbc);

        buttonCancel.addActionListener(JFrameUtils.closeParentWindowListener);
        add(buttonCancel, gbc);

        pack();
        setBounds(100, 100, 460, 500);
        setResizable(true);
        setIconImage(ProgramIcons.settingsIcon.getImage());
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        DataHandler.addToUpdateTree(this);
    }
        
    private void acceptChanges(){
    	for (int i = 0; i < tapPane.getComponentCount(); ++i)
    	{
    		((OptionPanel)tapPane.getComponent(i)).save();
    	}
    }

    
	@Override
	public void setVisible(boolean vis){
    	if (vis){
	    	try{
	    		for (int i = 0; i < tapPane.getComponentCount(); ++i)
	        	{
	        		((OptionPanel)tapPane.getComponent(i)).load();
	        	}
	    	}catch (Exception e){
	    		logger.error("Can't read all Options", InterfaceOptions.class, e);
	    	}
    	}
        super.setVisible(vis);
    }
    
    private static class JPanelLightEnvirontment extends OptionPanel implements ActionListener{
		private static final long serialVersionUID = -5747937235747043452L;
	    private final ButtonGroup buttonGroupLightEnvironment = new ButtonGroup();
	    private final JRadioButton radioButtonUseLights = new JRadioButton("Lichter benutzen");
	    private final JRadioButton radioButtonUseCubeMap = new JRadioButton("CubeMap benutzen");
	    private final JLabel labelCubemap = new JLabel("Cubemap");
	    private final JButton buttonCubemap = new JButton();
	    private final JPanelLights panelLight = new JPanelLights();
	    private final JPanel panelEnvironment = new JPanel();

	    @Override
		public void actionPerformed(ActionEvent ae){
	    	Object source = ae.getSource();
	    	if (source == radioButtonUseLights)
	    	{
	    		setUseCubemap(false);
	    	}
	    	else if (source == radioButtonUseCubeMap)
	    	{
	    		setUseCubemap(true);
	    	}
	    	else if (source == buttonCubemap)
			{
	    		JFileChooser fileChooser = new JFileChooser();
    			fileChooser.setCurrentDirectory(new File(buttonCubemap.getText()));
    	        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                if(fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) 
                	buttonCubemap.setText(fileChooser.getSelectedFile().getPath());
			}
        }
	    
		public JPanelLightEnvirontment(){
            GroupLayout layout = new GroupLayout(this);
            setLayout(layout);

            buttonGroupLightEnvironment.add(radioButtonUseCubeMap);
            buttonCubemap.addActionListener(this);

            panelEnvironment.setLayout(JFrameUtils.LEFT_FLOW_LAYOUT);
            panelEnvironment.add(labelCubemap);
            panelEnvironment.add(buttonCubemap);

            radioButtonUseLights.addActionListener(this);
            buttonGroupLightEnvironment.add(radioButtonUseLights);
            radioButtonUseCubeMap.addActionListener(this);

            layout.setHorizontalGroup(layout.createParallelGroup()
            	.addGroup(layout.createSequentialGroup()
            		.addComponent(radioButtonUseLights)
            		.addComponent(radioButtonUseCubeMap)
            	).addComponent(panelLight)
            	.addComponent(panelEnvironment)
            );
            layout.setVerticalGroup(layout.createSequentialGroup()
            	.addGroup(layout.createParallelGroup()
            		.addComponent(radioButtonUseLights)
            		.addComponent(radioButtonUseCubeMap)
            	).addGroup(layout.createParallelGroup()
            		.addComponent(panelLight)
            		.addComponent(panelEnvironment)
            	)
            ); 		
    	}
		
		@Override
		public void load(){
			OptionTreeInnerNode node = Options.getInnerNode("scene");
			OptionTreeInnerNode environment = Options.getInnerNode(node, "environment");
			byte environment_type = Options.getByte(environment, "type", Scene.LIGHTS);
	        radioButtonUseCubeMap.setSelected(environment_type == Scene.REFLECTION_MAP);
	        radioButtonUseLights.setSelected(environment_type != Scene.REFLECTION_MAP);
	        buttonCubemap.setText(Options.getString(node, "cubemap", ""));
	        panelLight.load();
	        setUseCubemap(environment_type == Scene.REFLECTION_MAP);
		}
		
		@Override
		public void save(){
			OptionTreeInnerNode node = Options.getInnerNode("scene");
			OptionTreeInnerNode environment = Options.getInnerNode(node, "environment");
			Options.set(environment, "type", radioButtonUseCubeMap.isSelected() ? Scene.REFLECTION_MAP : Scene.LIGHTS);
	    	Options.set(node, "cubemap", buttonCubemap.getText());
	    	Options.triggerUpdates();
	    	panelLight.save();
		}
		
	    private void setUseCubemap(boolean value){
	        panelLight.setVisible(!value);
	        panelEnvironment.setVisible(value);    	
	    }
    }
    
    private static class JPanelExtended extends OptionPanel{
		private static final long	serialVersionUID		= 2378716257092306946L;
		private final JLabel 		labelLimitMemory		= new JLabel("Begrenze Speicher");
        private final JComboBox<String> comboBoxLimitMemory = new JComboBox<String>(new String[]{"Nein", "128 MB", "256 MB","512 MB", "1024 MB", "2048 MB"});
        private final JLabel		labelCheckForUpdate		= new JLabel("Automatisch auf Updates pr\u00FCfen");
        private final JCheckBox		checkBoxCheckForUpdate	= new JCheckBox();
        private final JLabel		labelLimitGraphMemory	= new JLabel("Begrenze Speicher pro Graph");
        private final JComboBox<String>	comboBoxLimitGraphMemory = new JComboBox<String>(new String[]{"Nein", "128 MB", "256 MB","512 MB", "1024 MB", "2048 MB"});
        private final JLabel		labelGlobalUpdate		= new JLabel("Globaler update Interval");
        private final JTextField	textFieldGlobalUpdate	= new JTextField();
        private final JLabel		labelGraphUpdate		= new JLabel("Graph update Interval");
        private final JTextField	textFieldGraphUpdate= new JTextField();
        private final JLabel		labelInterfaceUpdate	= new JLabel("Interface update Interval");
        private final JTextField	textFieldInterfaceUpdate= new JTextField();
        private static final int	limitMemorySteps[]		= {-1,128000000,256000000,512000000,1024000000,2048000000};
        private static final int	limitGraphMemorySteps[]= {-1,128000000,256000000,512000000,1024000000,2048000000};
        
        public JPanelExtended(){
        	setLayout(JFrameUtils.DOUBLE_COLUMN_LAUYOUT);
        	String toolTipText = "<html><body>Dies ist keine Maximalgrenze f\u00FCr den Speicher des Programmes.<br />\u00DCberschreitet der ben\u00F6tigte Speicher f\u00FCr die aktuellen Objekte diese Grenze, dann wird die jvm darum gebeten den Garbage-Collector zu starten.<br />Der Speicher den die jvm resertviert kann jedoch deutlich gr\u00F6\u00DFer sein.</body></html>";
        	labelLimitMemory.setToolTipText(toolTipText);
        	add(labelLimitMemory);
        	comboBoxLimitMemory.setToolTipText(toolTipText);
        	add(comboBoxLimitMemory);
        	toolTipText= "Graphen die gr\u00F6\u00DFer sind als die Angegebene Maximalgr\u00F6\u00DFe werden nicht berechnet.";
        	labelLimitGraphMemory.setToolTipText(toolTipText);
        	add(labelLimitGraphMemory);
        	comboBoxLimitGraphMemory.setToolTipText(toolTipText);
        	add(comboBoxLimitGraphMemory);
        	add(labelCheckForUpdate);
        	add(checkBoxCheckForUpdate);
        	add(labelGlobalUpdate);
        	add(textFieldGlobalUpdate);
        	add(labelGraphUpdate);
        	add(textFieldGraphUpdate);
        	add(labelInterfaceUpdate);
        	add(textFieldInterfaceUpdate);
        }
        
        @Override
		public void load(){
	        int tmp = Options.getInteger("limit_memory", -1);
    		comboBoxLimitMemory.setSelectedIndex(0);
	        for(int i=0;i<limitMemorySteps.length;i++){
	        	if (limitMemorySteps[i] == tmp){
	        		comboBoxLimitMemory.setSelectedIndex(i);
	        		break;
	        	}
	        }
	        tmp = Options.getInteger("limit_graph_memory", -1);
	        comboBoxLimitGraphMemory.setSelectedIndex(1);
	        for(int i=0;i<limitGraphMemorySteps.length;i++){
	        	if (limitGraphMemorySteps[i] == tmp){
	        		comboBoxLimitGraphMemory.setSelectedIndex(i);
	        		break;
	        	}
	        }
	        checkBoxCheckForUpdate.setSelected(Options.getBoolean("auto_check_updates", false));
        }
        
        @Override
		public void save(){
        	Options.set("limit_memory", limitMemorySteps[comboBoxLimitMemory.getSelectedIndex()]);
        	Options.set("limit_graph_memory", limitGraphMemorySteps[comboBoxLimitGraphMemory.getSelectedIndex()]);
        	Options.set("auto_check_updates", checkBoxCheckForUpdate.isSelected());
        	Options.triggerUpdates();
        }
    }

    private static class JPanelVision extends OptionPanel{
		private static final long serialVersionUID = -8205199122505119901L;
		private final JLabel labelRenderingApi		= new JLabel("Rendering api");
		private final JComboBox<RenderingAlgorithms> comboBoxRendererApi = new JComboBox<RenderingAlgorithms>(RenderingAlgorithms.values());
		private final JLabel textEnergyMode			= new JLabel("Energiemodus");
        private final JComboBox<String> comboBoxEnergyMode	= new JComboBox<String>(new String[]{"Maximale Leistung", "Feste Bildrate", "Minimaler Energieverbrauch"});
        private final JLabel labelQuality			= new JLabel("Qualit\u00E4t");
        private final JComboBox<String> comboBoxQuality		= new JComboBox<String>(new String[]{"Hoch", "Mittel", "Niedrig"});
        private final JLabel textShowFramerate		= new JLabel("Framerate anzeigen");
        private final JCheckBox checkBoxShowFramerate= new JCheckBox();
        private final JLabel textMultisampling		= new JLabel("Multisampling");
        private final JComboBox<String> comboBoxMultisampling= new JComboBox<String>(new String[]{"OFF", "2X", "4X", "8X", "16X"});
        private final JLabel textBackgroundColor	= new JLabel("Hintergrundfarbe");
        private final JColorPickPanel colorPickBackgroundColor = new JColorPickPanel();
        private final JLabel textShowGridLines		= new JLabel("Gitterlinien anzeigen");
        private final JCheckBox checkBoxShowGridLines= new JCheckBox();
        private final JLabel textGridLinesColor		= new JLabel("Vordergrundfarbe");
        private final JColorPickPanel colorPickGridLinesColor = new JColorPickPanel();
        private final JLabel textLineWidth			= new JLabel("Linienbreite");
        private final JTextField textFieldLineWidth	= new JTextField();
        private final JLabel labelLayoutManager		= new JLabel("UI-Manager");
        private final JComboBox<String> comboBoxLayoutManager= new JComboBox<String>();
        private final JLabel labelStereo			= new JLabel("Stereoskopie");
        private final JCheckBox checkBoxStereo		= new JCheckBox();
        private final JLabel labelStereoStrength	= new JLabel("St\u00E4rke-Stereoskopie");
        private final JSlider sliderStereoStrength	= new JSlider(0, 100);
        private final JLabel labelStereoMoved		= new JLabel("Entfernung-Stereoskopie");
        private final JSlider sliderStereoMoved		= new JSlider(0, 100);
        
        public JPanelVision(){
            setLayout(new GridLayout(-1,2,5,5));

            add(labelRenderingApi);
            add(comboBoxRendererApi);
            String toolTipText = "<html><body>Kann verstellt werden um ein fl\u00FCssigeres Bild zu bekommen oder um Strom zu sparen.<br />Bei einer CPU mit nur einem Kern ist der Stromsparmodus empfohlen.</html></body>";
            textEnergyMode.setToolTipText(toolTipText);
            add(textEnergyMode);
            comboBoxEnergyMode.setToolTipText(toolTipText);
            add(comboBoxEnergyMode);
            add(labelQuality);
            add(comboBoxQuality);
            add(textShowFramerate);
            add(checkBoxShowFramerate);
            toolTipText = "<html><head></head><body>Sorgt f\u00FCr eine bessere Kantengl\u00E4ttung.<br />\u00C4nderungen werden erst nach einem Neustart \u00FCbernommen.</body></html>";
            textMultisampling.setToolTipText(toolTipText);
            add(textMultisampling);
            comboBoxMultisampling.setToolTipText(toolTipText);
            add(comboBoxMultisampling);
            add(textBackgroundColor);
            add(colorPickBackgroundColor);
            add(textShowGridLines);
            add(checkBoxShowGridLines);
            add(textGridLinesColor);
            add(colorPickGridLinesColor);
            add(textLineWidth);
            add(textFieldLineWidth);
            add(labelLayoutManager);
    		for (LookAndFeelInfo lookAndFeelInfo : DataHandler.lookAndFeelInfo)
    			comboBoxLayoutManager.addItem(lookAndFeelInfo.getName());
            add(comboBoxLayoutManager);
            add(labelStereo);
            add(checkBoxStereo);
            add(labelStereoStrength);
            add(sliderStereoStrength);
            add(labelStereoMoved);
            add(sliderStereoMoved);  	
        }
        
        @Override
		public void load(){
        	comboBoxRendererApi.setSelectedItem(RenderingAlgorithms.valueOf(Options.getString("rendering_api")));
    		final String layout_manager = Options.getString("layout_manager","");
    		for (int i=0;i<DataHandler.lookAndFeelInfo.length;i++){
    			if (DataHandler.lookAndFeelInfo[i].getClassName().equals(layout_manager)){
    				comboBoxLayoutManager.setSelectedIndex(i);
    				break;
    			}
    		}
 	        switch (Options.getByte("energy_mode", Scene.MODE_SYNCHRONIZE)){
	    		case Scene.MODE_MAX_SPEED : comboBoxEnergyMode.setSelectedIndex(0);break;
	    		case Scene.MODE_SYNCHRONIZE : comboBoxEnergyMode.setSelectedIndex(1);break;
	        	case Scene.MODE_MIN_ENERGY : comboBoxEnergyMode.setSelectedIndex(2);break;
	        	default : throw new RuntimeException();
	        }
 	        checkBoxShowFramerate.setSelected(Options.getBoolean("show_framerate", false));
 	        checkBoxShowGridLines.setSelected(Options.getBoolean("show_grid_lines", true));
	        for (int i=0, mult = Options.getInteger("multisampling");i<5;i++){
	        	if (mult == 1<<i){
	        		comboBoxMultisampling.setSelectedIndex(i);
	        		break;
	        	}
	        }
	        colorPickBackgroundColor.setBackground(Options.getColor("background", Color.WHITE));
	        colorPickGridLinesColor.setBackground(Options.getColor("grid_lines_color", Color.BLACK));
	        OptionTreeInnerNode stereoskopie = Options.getInnerNode("stereoskopie");
	        checkBoxStereo.setSelected(Options.getBoolean(stereoskopie, "activated", false));
	        sliderStereoStrength.setValue((int)(Options.getFloat(stereoskopie, "strength", 0f)*10f));
	        sliderStereoMoved.setValue((int)(Options.getFloat(stereoskopie, "moved", 0f)*10f));
	        final String quality = Options.getString("quality", "middle");
	        if (quality.equals("high"))
	        	comboBoxQuality.setSelectedIndex(0);
	        else if (quality.equals("middle"))
	        	comboBoxQuality.setSelectedIndex(1);
	        else if (quality.equals("low"))
	        	comboBoxQuality.setSelectedIndex(2);
        }
        
        @Override
		public void save(){
        	byte energyMode;
            switch (comboBoxEnergyMode.getSelectedIndex()){
            	case 0: energyMode = Scene.MODE_MAX_SPEED; break;
            	case 1: energyMode = Scene.MODE_SYNCHRONIZE; break;
            	case 2: energyMode = Scene.MODE_MIN_ENERGY; break;
            	default: throw new RuntimeException();
            }
            Options.set("rendering_api", comboBoxRendererApi.getSelectedItem().toString());
            Options.set("multisampling",1 << comboBoxMultisampling.getSelectedIndex());
            Options.set("energy_mode", (int)energyMode);
            Options.set("background", colorPickBackgroundColor.getBackground());
            Options.set("grid_lines_color", colorPickGridLinesColor.getBackground());
            Options.set("show_framerate", checkBoxShowFramerate.isSelected());
            Options.set("show_grid_lines", checkBoxShowGridLines.isSelected());
            Options.triggerUpdates();
        	
        	EventQueue.invokeLater(new Runnable() {
				@Override
				public void run() {
		        	DataHandler.setLookAndFeel(DataHandler.lookAndFeelInfo[comboBoxLayoutManager.getSelectedIndex()]);
				}//TODO move to listener
			});
        	String quality;
        	
        	switch (comboBoxQuality.getSelectedIndex()){
	    		case 0: quality = "high";break;
	    		case 1: quality = "middle";break;
	    		case 2: quality = "low";break;
	        	default: throw new RuntimeException();
        	}
        	Options.set("layout_manager", DataHandler.lookAndFeelInfo[comboBoxLayoutManager.getSelectedIndex()].getClassName());
        	Options.OptionTreeInnerNode node = Options.getInnerNode("stereoskopie");
        	Options.set(node, "activated", checkBoxStereo.isSelected());
        	Options.set(node, "strength", sliderStereoStrength.getValue()/10f);
        	Options.set(node, "moved", sliderStereoMoved.getValue()/10f);
        	Options.set("quality", quality);
        	Options.triggerUpdates();
        }
    }
        
    private static class JPanelLights extends OptionPanel implements ActionListener{
		private static final long serialVersionUID = 325104130708851935L;
	    private final JLabel labelAmbient            = new JLabel("A");
	    private final JLabel labelDiffuse            = new JLabel("D");
	    private final JLabel labelSpecular           = new JLabel("S");
	    private final JLabel labelLightX             = new JLabel("X");
	    private final JLabel labelLightY             = new JLabel("Y");
	    private final JLabel labelLightZ             = new JLabel("Z");
		private final JCheckBox checkBoxLights[]     = new JCheckBox[8];
        private final JColorPickPanel colPickAmbientLights[] = new JColorPickPanel[8];
        private final JColorPickPanel colPickDiffuseLights[] = new JColorPickPanel[8];
        private final JColorPickPanel colPickSpecularLights[] = new JColorPickPanel[8];
        private final JTextField textFieldLightPositionX[] = new JTextField[8];
        private final JTextField textFieldLightPositionY[] = new JTextField[8];
        private final JTextField textFieldLightPositionZ[] = new JTextField[8];	
        private final JMenuBar comboBoxPreset			= new JMenuBar();
        private final JPanel panelLights				= new JPanel();
        private final LightConfiguration presets[] = new LightConfiguration[]{LightConfiguration.getInstance(DataHandler.getResource("/light_presets/colors.txt"), "Bunt")};

     
        private JPanelLights(){
        	GroupLayout layout = new GroupLayout(this);
            setLayout(layout);
            
            JMenu jmenu = new JMenu("Vorgabe w\u00E4hlen");
            comboBoxPreset.add(jmenu);
            for (LightConfiguration pr : presets){
            	JMenuItem item = new JMenuItem(pr.name);
            	jmenu.add(item);
            	item.addActionListener(this);
            }
            
        	panelLights.setLayout(new GridLayout(-1, 7, 5, 5));
            panelLights.add(new JLabel());
            panelLights.add(labelAmbient);
            panelLights.add(labelDiffuse);
            panelLights.add(labelSpecular);
            panelLights.add(labelLightX);
            panelLights.add(labelLightY);
            panelLights.add(labelLightZ);

        	for (int i=0;i<8;i++){
        		panelLights.add(checkBoxLights[i] = new JCheckBox(String.valueOf(i)));
        		checkBoxLights[i].addActionListener(this);
        		panelLights.add(colPickAmbientLights[i] = new JColorPickPanel());
        		panelLights.add(colPickDiffuseLights[i] = new JColorPickPanel());
        		panelLights.add(colPickSpecularLights[i] = new JColorPickPanel());
        		panelLights.add(textFieldLightPositionX[i] = new JTextField());
        		panelLights.add(textFieldLightPositionY[i] = new JTextField());
        		panelLights.add(textFieldLightPositionZ[i] = new JTextField());
        	}
        	
        	layout.setHorizontalGroup(layout.createParallelGroup()
                .addComponent(comboBoxPreset, 50, 5000, 10000)        			
        		.addComponent(panelLights)
        	);
        	
        	layout.setVerticalGroup(layout.createSequentialGroup()
               	.addComponent(comboBoxPreset, 30 ,30,30)        			
        		.addComponent(panelLights)
        	);
        }
        
        private void load(LightConfiguration preset){
        	for (int i=0;i<8;i++){
	        	enableLight(i, preset.activated[i]);
	        	colPickAmbientLights[i].setBackground(new Color(preset.ambient[i]));
	        	colPickDiffuseLights[i].setBackground(new Color(preset.diffuse[i]));
	        	colPickSpecularLights[i].setBackground(new Color(preset.specular[i]));
	        	textFieldLightPositionX[i].setText(String.valueOf(preset.xPos[i]));
	        	textFieldLightPositionY[i].setText(String.valueOf(preset.yPos[i]));
	        	textFieldLightPositionZ[i].setText(String.valueOf(preset.zPos[i]));
        	}
        }
        
        @Override
		public void load(){
        	StringBuilder stringBuilder = new StringBuilder(2);
        	stringBuilder.append('l');
    		Options.OptionTreeInnerNode node = Options.getInnerNode("light");
	        for (int i=0;i<8;i++){
	        	stringBuilder.setLength(1);
	        	stringBuilder.append(i);
	        	Options.OptionTreeInnerNode child = Options.getInnerNode(node, stringBuilder.toString());
	        	enableLight(i, Options.getBoolean(child, "activated", false));
	        	colPickAmbientLights[i].setBackground(Options.getColor(child, "ambient", Color.BLACK));
	        	colPickDiffuseLights[i].setBackground(Options.getColor(child, "diffuse", Color.BLACK));
	        	colPickSpecularLights[i].setBackground(Options.getColor(child, "specular", Color.BLACK));
	        	textFieldLightPositionX[i].setText(Float.toString(Options.getFloat(child, "xpos", 0f)));
	        	textFieldLightPositionY[i].setText(Float.toString(Options.getFloat(child, "ypos", 0f)));
	        	textFieldLightPositionZ[i].setText(Float.toString(Options.getFloat(child, "zpos", 0f)));
	        }
        }
        
		@Override
        public void save(){
	       	StringBuilder stringBuilder = new StringBuilder(2);
        	stringBuilder.append('l');
     		Options.OptionTreeInnerNode node = Options.getInnerNode("light");
            for (int i=0;i<8;i++){
            	stringBuilder.setLength(1);
	        	stringBuilder.append(i);
	        	Options.OptionTreeInnerNode child = Options.getInnerNode(node, stringBuilder.toString());
	        	Options.set(child, "activated", checkBoxLights[i].isSelected());
	        	Options.set(child, "ambient", colPickAmbientLights[i].getBackground());
	        	Options.set(child, "diffuse", colPickDiffuseLights[i].getBackground());
	        	Options.set(child, "specular", colPickSpecularLights[i].getBackground());
	        	Options.set(child, "xpos", Float.valueOf(textFieldLightPositionX[i].getText()));
	        	Options.set(child, "ypos", Float.valueOf(textFieldLightPositionY[i].getText()));
	        	Options.set(child, "zpos",  Float.valueOf(textFieldLightPositionZ[i].getText()));
            }
            Options.triggerUpdates();
        }

        private void enableLight(int light, boolean enabled){
        	checkBoxLights[light].setSelected(enabled);
            colPickAmbientLights[light].setEnabled(enabled);
            colPickDiffuseLights[light].setEnabled(enabled);
            colPickSpecularLights[light].setEnabled(enabled);
            textFieldLightPositionX[light].setEnabled(enabled);
            textFieldLightPositionY[light].setEnabled(enabled);
            textFieldLightPositionZ[light].setEnabled(enabled);        
        }

		@Override
		public void actionPerformed(ActionEvent arg0) {
			Object source = arg0.getSource();
			for (int i = 0; i < checkBoxLights.length; ++i)
			{
				if (checkBoxLights[i] == source)
				{
		            enableLight(i, checkBoxLights[i].isSelected());
		            return;
				}
			}
			if (source == comboBoxPreset)
			{
				for (LightConfiguration pr : presets){
					if (pr.name.equals(arg0.getActionCommand())){
						load (pr);
						return;
					}
				}
			}
		}
    }
    
    private static class LightConfiguration{
    	private final String name;
    	private final boolean activated[] = new boolean[8];
    	private final int ambient[] = new int[8];
    	private final int diffuse[] = new int[8];
    	private final int specular[] = new int[8];
    	private final float xPos[] = new float[8];
    	private final float yPos[] = new float[8];
    	private final float zPos[] = new float[8];
    	
    	private LightConfiguration (final InputStream inStream, String name) throws IOException{
    		this.name = name;
    		final InputStreamReader inStreamReader = new InputStreamReader(inStream);
    		final BufferedReader reader = new BufferedReader(inStreamReader);
    		String line;
    		final SaveLineCreator saveLineCreator = new SaveLineCreator();
			while ((line = reader.readLine())!= null){
				final SaveObject saveObject = saveLineCreator.getSaveObject(line);
				final String variable = saveObject.variable;
				if (variable.startsWith("light_") && variable.length() > 8 && variable.charAt(6)<='8' && variable.charAt(6) >= '0' && variable.charAt(7) == '_'){
					final int index = variable.charAt(6) - '0';
					final String option = variable.substring(8);
					final String value = saveObject.value;
					try{
						switch(option)
						{
							case "activated": activated[index] = Boolean.parseBoolean(value);break;
							case "ambient":   ambient[index]   = Integer.parseInt(value);break;
							case "diffuse":   diffuse[index]   = Integer.parseInt(value);break;
							case "specular":  specular[index]  = Integer.parseInt(value);break;
							case "xpos":  	  xPos[index]      = Float.parseFloat(value);break;
							case "ypos":      yPos[index]      = Float.parseFloat(value);break;
							case "zpos":      zPos[index]      = Float.parseFloat(value);break;
						}
					}catch(Exception e){
						logger.debug("Unreadable option line:" + line);
					}
				}
			}
    	}
    	
    	private static LightConfiguration getInstance(URL url, String name){
    		InputStream stream = null;
    		try{
    			stream = url.openStream();
    			return new LightConfiguration(stream, name);
    		}catch(IOException e){
    			return null;
    		}finally{
    			try {
					stream.close();
				} catch (IOException e) {}
    		}
    	}
    }
}
