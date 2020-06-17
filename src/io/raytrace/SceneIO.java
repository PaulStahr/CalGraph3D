package io.raytrace;

import java.awt.Component;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.ParseException;
import java.util.ArrayList;

import javax.swing.JTable;

import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import data.raytrace.GuiOpticalSurfaceObject;
import data.raytrace.GuiOpticalVolumeObject;
import data.raytrace.GuiTextureObject;
import data.raytrace.MeshObject;
import data.raytrace.OpticalObject;
import data.raytrace.OpticalObject.COLUMN_TYPES;
import data.raytrace.OpticalObject.SCENE_OBJECT_COLUMN_TYPE;
import data.raytrace.OpticalVolumeObject;
import data.raytrace.ParseUtil;
import data.raytrace.RaytraceScene;
import data.raytrace.TextureMapping;
import data.raytrace.VolumePipeline;
import data.raytrace.VolumePipeline.CalculationCalcuationStep;
import data.raytrace.VolumePipeline.CalculationStep;
import data.raytrace.VolumePipeline.GenerationCalculationStep;
import geometry.Geometry;
import jcomponents.panels.InterfacePanel;
import jcomponents.panels.InterfacePanelFactory;
import jcomponents.raytrace.RaySimulationGui;
import jcomponents.raytrace.VolumePipelinePanel;
import maths.Controller;
import maths.OperationCompiler;
import maths.Variable;
import maths.data.Characters;
import maths.exception.OperationParseException;
import maths.functions.TransposeOperation;
import util.JFrameUtils;

public class SceneIO {
	private static final Logger logger = LoggerFactory.getLogger(SceneIO.class);
	private static final int version = 1;
    
    private static final void readXMLValues(Element elem, ArrayList<SCENE_OBJECT_COLUMN_TYPE> ctList, ArrayList<String> valueList)
    {
    	ctList.clear();
		valueList.clear();
		for (Attribute attribute : elem.getAttributes())
		{
			SCENE_OBJECT_COLUMN_TYPE ct = SCENE_OBJECT_COLUMN_TYPE.getByName(attribute.getName());
			if (ct != null)
			{
				ctList.add(ct);
				valueList.add(attribute.getValue());
			}
		}
	}
	
    public static void loadScene(InputStream in, RaytraceScene scene, final RaySimulationGui gui) throws JDOMException, IOException
    {
        ParseUtil parser = new ParseUtil();
    	Document doc = new SAXBuilder().build(in);
    	Element root = doc.getRootElement();
    	ArrayList<SCENE_OBJECT_COLUMN_TYPE> ctList = new ArrayList<SCENE_OBJECT_COLUMN_TYPE>();
    	
    	ArrayList<String> valueList = new ArrayList<>();
    	String versionString = root.getAttributeValue("version");
    	int version = versionString == null ? -1 : Integer.parseInt(versionString);
    	for (Element elem : root.getChildren())
    	{
    		switch( elem.getName())
    		{
    			case "row":
    			case "surface":	readXMLValues(elem, ctList, valueList);	scene.add(new GuiOpticalSurfaceObject(	ctList, valueList, scene.vs, parser));break;
    			case "volume":	readXMLValues(elem, ctList, valueList);	scene.add(new GuiOpticalVolumeObject(	ctList, valueList, scene.vs, parser));break;
    			case "texture":	readXMLValues(elem, ctList, valueList);
    			if (version < 0)
    			{
    				int index = ctList.indexOf(SCENE_OBJECT_COLUMN_TYPE.PATH);
    				valueList.set(index, "\"" + valueList.get(index) + "\"");
    			}														scene.add(new GuiTextureObject(	ctList, valueList, scene.vs, parser));break;
    			case "mesh":	readXMLValues(elem, ctList, valueList);	scene.add(new MeshObject(				ctList, valueList, scene.vs, parser));break;
    			case "Raybounds":
    				for (Attribute attribute : elem.getAttributes())
        			{
    					switch (attribute.getName())
    					{
    						case "Start":
    						case "Begin": scene.setForceStartpoint(attribute.getValue()); break;
    						case "End"	: scene.setForceEndpoint(attribute.getValue()); break;
    				    	default		: logger.warn("Unknown optio" + attribute.getName());
    					}
        			}
    				break;
    			case "Environment":
    				for (Attribute attribute : elem.getAttributes())
        			{
        				String attributeName = attribute.getName();
        				String attributeValue = attribute.getValue();
        				try
        				{
        					switch(attributeName)
        					{
        						case "Read":					scene.setEnvironmentTexture(attributeValue);break;
        						case "Write":					scene.setEnvironmentTexture(attributeValue);break;
        						case "RenderToTexture":			scene.setRenderToTexture(attributeValue);break;
        						case "VerifyRefractionIndex": 	scene.setVerifyRefractionIndices(Boolean.parseBoolean(attributeValue));break;
        						case "Mapping":					scene.setTextureMapping(TextureMapping.getByName(attributeValue));break;
            					default:						logger.warn("Unknown option "+ attribute.getName());
    	    				}
        				}catch(IllegalArgumentException e)
        				{
        					logger.error("Can't set property " + attributeName + '-' + '>' + attributeValue, e);
        				}
        			}
    				break;
    			case "Tool":gui.panelTools.add(InterfacePanelFactory.getInstance(elem.getText(), scene.vs));break;
    			case "Pipeline":
    				VolumePipeline pipeline = gui.volumePipelines.addPipeline().pipeline;
    				
        			for (Element child : elem.getChildren())
        			{
        				switch (child.getName())
        				{
        					case "Generate":			pipeline.steps.add(new VolumePipeline.GenerationCalculationStep(child.getAttributeValue("Bounds")));break;
        					case "Calculate":			pipeline.steps.add(new VolumePipeline.CalculationCalcuationStep(child.getAttributeValue("Ior"), child.getAttributeValue("Translucency"), child.getAttributeValue("EqValue"), child.getAttributeValue("EqGiven")));break;
        					default:					logger.warn("Unknown option " + child.getName());
        				}
        			}
        			for (Attribute attr : elem.getAttributes())
        			{
        				switch (attr.getName())
        				{
        					case "Volume":				pipeline.ovo = scene.getVolumeObject(attr.getValue());break;
        					case "AutoUpdate":			pipeline.setAutoUpdate(Boolean.parseBoolean(attr.getValue()));break;
        					case "CalculateAtStartup":	pipeline.calcuteAtCreation = Boolean.parseBoolean(attr.getValue());break;
        					default:					logger.warn("Unknown option "+ attr.getName());
        				}
        			}
        			pipeline.updateState();
        			break;
    			case "Author":		scene.author = elem.getText();break;
    			case "Epsilon":		scene.epsilon = Double.parseDouble(elem.getText());break;
    			case "Description": gui.textAreaProjectInformation.setText(elem.getValue());break;
    			case "Variables": 
        			for (Element child : elem.getChildren())
        			{
        				try {
    						scene.vs.add(new Variable(child.getName(), child.getText()));
    					} catch (OperationParseException e) {
    						logger.error("Can't parse variable " + child.getName(), e);
    					}
        			}
        			break;
    			case "Gui":
    				for (Attribute attr : elem.getAttributes())
        			{
        				try {
    	    				switch(attr.getName())
    	    				{
    	    				case "Position":	Geometry.parse(attr.getValue(), gui.paintOffset);break;
    	    				case "Scale":		gui.panelVisualization.scale = Double.valueOf(attr.getValue());break;
    	    				}
        				}catch(ParseException pe)
        				{
        					logger.error("Can't parse attribute " + attr.getName(), pe);
        				}
        			}
    				break;
    			default:	logger.warn("Unknown File entry " + elem.getName());
    		}
    	}
    	if (version < 1)
    	{
    		Controller control = new Controller();
    		for (OpticalVolumeObject volume : scene.volumeObjectList)
    		{
    			GuiOpticalVolumeObject gov = (GuiOpticalVolumeObject)volume;
    			try {
					gov.setValue(SCENE_OBJECT_COLUMN_TYPE.TRANSFORMATION, new TransposeOperation(OperationCompiler.compile(gov.transformationStr)).calculate(scene.vs, control), scene.vs, parser);
				} catch (OperationParseException e) {
					try {
						gov.setValue(SCENE_OBJECT_COLUMN_TYPE.TRANSFORMATION, new StringBuilder().append(Characters.HIGH_T).append('(').append(gov.transformationStr).append(')').toString(), scene.vs, parser);
					} catch (OperationParseException e1) {
						logger.error("Can't load file in fallback-mode");
					}
				}
    		}
    	}
    	JFrameUtils.runByDispatcher(new Runnable() {
    		@Override
			public void run() {
            	gui.updateAllTables();    			
    		}
    	});
    }
    
    private static final void writeXmlValues(OpticalObject oso, Element elem) {
    	
    	COLUMN_TYPES types = oso.getTypes();
		for (int j = 0; j < types.colSize(); ++j)
		{
			SCENE_OBJECT_COLUMN_TYPE ct = types.getCol(j);
			elem.setAttribute(ct.name, String.valueOf(oso.getValue(ct)));
		}
    }
    
    private static final <E extends OpticalObject> void writeXmlList(Element root, ArrayList<E> list, JTable table, String elemName, boolean onlySelected)
    {
    	for (int i = 0; i < list.size(); ++i)
    	{
    		if (!onlySelected || table.isRowSelected(i))
    		{
    			Element elem = new Element(elemName);
    			writeXmlValues(list.get(i), elem);
				root.addContent(elem);
    		}
    	}
    }
    
   	public static void saveScene(OutputStream out, boolean onlySelected, RaytraceScene scene, RaySimulationGui gui) throws IOException
    {
    	Document doc = new Document();
    	Element root = new Element("scene");
    	doc.setRootElement(root); 
    	root.setAttribute("version", Integer.toString(version));
    	writeXmlList(root, scene.surfaceObjectList, gui.tableSurfaces, "surface", onlySelected);
    	writeXmlList(root, scene.volumeObjectList,  gui.tableVolumes, "volume", onlySelected);
    	writeXmlList(root, scene.textureObjectList, gui.tableTextures, "texture", onlySelected);
    	writeXmlList(root, scene.meshObjectList, 	gui.tableMeshes, "mesh", onlySelected);
    	if (!onlySelected)
    	{
	    	Element elem = new Element("Raybounds");
	    	elem.setAttribute("Start", scene.getForceStartpointStr());
	    	elem.setAttribute("End", scene.getForceEndpointStr());
	    	root.addContent(elem);
	    	root.addContent(new Element("Description", gui.textAreaProjectInformation.getText()));
	    	elem = new Element("Environment");
	    	elem.setAttribute("Read", scene.environmentTextureString == null ? "" : scene.environmentTextureString);
	    	elem.setAttribute("Write", scene.writableEnvironmentTextureString == null ? "" : scene.writableEnvironmentTextureString);
	    	elem.setAttribute("RenderToTexture", scene.renderToTextureString == null ? "" : scene.renderToTextureString);
	    	elem.setAttribute("VerifyRefractionIndex", Boolean.toString(scene.isVerifyRefractionIndexActivated()));
	    	elem.setAttribute("Mapping", scene.environment_mapping.name);
	    	root.addContent(elem);
	    	for (int i = 0; i < gui.panelTools.getComponentCount(); ++i)
	    	{
	    		elem = new Element("Tool");
	    		Component comp = gui.panelTools.getComponent(i);
	    		if (comp instanceof InterfacePanel)
				{
	    			InterfacePanel ip = (InterfacePanel)comp;
	    			elem.setText(ip.getContent());		
				}
	    		root.addContent(elem);
	    	}
	    	for (VolumePipelinePanel vp : gui.volumePipelines.getPipelines())
	    	{
	    		VolumePipeline pipeline = vp.pipeline;
	    		elem = new Element("Pipeline");
	    		for (CalculationStep step : pipeline.steps)
	    		{
	    			if (step instanceof GenerationCalculationStep)
	    			{
	    				GenerationCalculationStep gps = (GenerationCalculationStep)step;
	    				Element child = new Element("Generate");
	    				child.setAttribute("Bounds", gps.size);
	    				elem.addContent(child);
	    			}
	    			else if (step instanceof CalculationCalcuationStep)
					{
	    				CalculationCalcuationStep cps = (CalculationCalcuationStep)step;
	        			Element child = new Element("Calculate");
	    				child.setAttribute("Ior", cps.ior);
	    				child.setAttribute("Translucency", cps.translucency);
	    				child.setAttribute("EqValue", cps.givenValues);
	    				child.setAttribute("EqGiven", cps.isGiven);
	    				elem.addContent(child);
					}
	    		}
	    		elem.setAttribute("AutoUpdate", Boolean.toString(pipeline.getAutoUpdate()));
	    		elem.setAttribute("CalculateAtStartup", Boolean.toString(pipeline.calcuteAtCreation));
	    		if (pipeline.ovo != null)
	    		{
	    			elem.setAttribute("Volume", pipeline.ovo.id);
	    		}
    			root.addContent(elem);
	    	}
	    	root.addContent(new Element("Author").setText(scene.author));
	    	root.addContent(new Element("Epsilon").setText(Double.toString(scene.epsilon)));
    		elem = new Element("Variables");
	    	for (int i = 0; i < scene.vs.sizeLocal(); ++i)
	    	{
	    		Variable v = scene.vs.get(i);
	    		elem.addContent(new Element(v.nameObject.string).setText(v.stringValue()));
	    	}
	    	root.addContent(elem);
	    	elem = new Element("Gui");
	    	elem.setAttribute("Position", gui.paintOffset.toString());
	    	elem.setAttribute("Scale", Double.toString(gui.panelVisualization.scale));
    	}
    	new XMLOutputter(Format.getPrettyFormat()).output(doc, out);
    }

}
