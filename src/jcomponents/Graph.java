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
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.ref.WeakReference;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import data.DataHandler;
import data.Options;
import data.ProgramIcons;
import geometry.Matrix4d;
import jcomponents.util.JColorPickPanel;
import maths.Controller;
import maths.Operation;
import maths.OperationCompiler;
import maths.UserVariableOperation;
import maths.Variable;
import maths.VariableStack;
import maths.algorithm.OperationCalculate;
import maths.data.ArrayOperation;
import maths.data.RealDoubleOperation;
import maths.data.StringId;
import maths.data.StringId.StringIdObject;
import maths.exception.OperationParseException;
import maths.functions.DifferentiationOperation;
import maths.functions.SolveOperation;
import maths.functions.atomic.EqualsOperation;
import maths.functions.atomic.SubtractionOperation;
import scene.object.SceneObject;
import scene.object.SceneObject.DrawType;
import scene.object.SceneObject.UpdateKind;
import scene.object.SceneObjectLine;
import scene.object.SceneObjectPlane;
import scene.object.SceneObjectPointCloud;
import scene.object.SceneObjectVektor;
import util.ArrayTools;
import util.JFrameUtils;
import util.OperationGeometry;
import util.RunnableRunner;
import util.SaveLineCreator;
import util.StringUtils;
import util.data.SortedIntegerArrayList;
import util.data.SortedIntegerArrayList.ReadOnlySortedIntegerArrayList;
import util.data.UniqueObjects;
/** 
* @author  Paul Stahr
* @version 04.02.2012
*/

public final class Graph extends JPanel implements MouseListener, ChangeListener
{
    private static final long serialVersionUID  = -1588581083839003784L;

    private static final GraphKind graphKinds[] = GraphKind.values();    
    private static final byte drawTypeArray[]	= DrawType.values();
	private static final Logger logger = LoggerFactory.getLogger(Graph.class);
    private static final StringBuilder stringBuilder = new StringBuilder();
    private static boolean activated;
    public static enum GraphKind{
    	GRAPH_2D_FUNCTION(	0,	"2D Funktion",		0,	"f(x)=",	null,		null,		"x\u2265",	"x\u2264",	"Step x",	null,		null,		null,		new String[]{"x"}),
    	GRAPH_2D_PARAMETRIC(1,	"2D Parametrisch",	0,	"x(t)=",	"y(t)=",	null,		"t\u2265",	"t\u2264",	"Step t",	null,		null,		null,		new String[]{"t"}),
    	GRAPH_2D_POLAR(		2,	"2D Polar",			0,	"r(t)=",	null,		null,		"t\u2265",	"t\u2264",	"Step t",	null,		null,		null,		GRAPH_2D_PARAMETRIC.variable),
    	GRAPH_2D_PLOT(		3, 	"2D Plot",			0,	"x=",		"y=",		null,		null,		null,		null,		null,		null,		null,		UniqueObjects.EMPTY_STRING_ARRAY),
    	GRAPH_3D_LINE(		4, 	"3D Linie",			0,	"x(t)=",	"y(t)=",	"z(t)=",	"t\u2265",	"t\u2264",	"Step t",	null,		null,		null,		GRAPH_2D_PARAMETRIC.variable),
    	GRAPH_3D_FUNCTION(	5, 	"3D Funktion",		1,	"f(x, y)=",	null,		null,		"x\u2265",	"x\u2264",	"Step x",	"y\u2265",	"y\u2264",	"Step y",	new String[]{"x","y"}),
    	GRAPH_3D_PARAMETRIC(6,	"3D Parametrisch",	1,	"x(u, v)=",	"y(u, v)=", "z(u, v)=", "u\u2265",	"u\u2264",	"Step u",	"v\u2265",	"v\u2264",	"Step v",	new String[]{"u","v"}),
    	GRAPH_3D_POLAR(		7,	"3D Polar",			1,	"f(phi,r)=",null, 		null, 		"phi\u2265","phi\u2264","Step phi",	"r\u2265",	"r\u2264",	"Step r",	new String[]{"phi","r"}),
    	GRAPH_3D_PLOT(		8,	"3D Plot",			0,	"x=",		"y=",		"z=", 		null,		null,		null,		null,		null,		null,		UniqueObjects.EMPTY_STRING_ARRAY),
    	GRAPH_3D_VEKTOR(	9,	"3D Vektor",		1,	"v start=",	"v ende=",	null, 		null,		null,		null,		null,		null,		null,		UniqueObjects.EMPTY_STRING_ARRAY),
    	GRAPH_3D_CARTESIAN(10,	"3D Kartesisch",	1,	"Gleichung:",null,		null, 		"a\u2265",	"a\u2264",	"Step a",	"b\u2265",	"b\u2264",	"Step b",	new String[]{"x","y","z"}),
    	GRAPH_3D_VECTORFIELD(11,"3d Vectorfeld",    2,  "x(x,y,z)" , "y(x,y,z)","z(x,y,z)", "x\u2265","x\u2264",	"Step x",	"y\u2265",	"y\u2264",	"Step y",	GRAPH_3D_CARTESIAN.variable);
    	
    	public final int id;
    	public final String name, function0, function1, function2, min0, max0, step0, min1, max1, step1;
    	public final int gLObjectType;
    	public final List<StringId.StringIdObject> variable;
    	@SuppressWarnings("unchecked")
		private GraphKind(
    			final int id,
    			final String name,
    			int gLObjectType,
    			final String function0,
    			final String function1,
    			final String function2,
    			final String min0,
    			final String max0,
    			final String step0,
    			final String min1,
    			final String max1,
    			final String step1,
    			final Object variable){
    		this.id = id;
    		this.name = name;
    		this.gLObjectType = gLObjectType;
    		this.function0 = function0;
    		this.function1 = function1;
    		this.function2 = function2;
    		this.max0 = max0;
    		this.max1 = max1;
    		this.min0 = min0;
    		this.min1 = min1;
    		this.step0 = step0;
    		this.step1 = step1;
    		if (variable instanceof String[])
    		{
	    		if (((String[])variable).length == 0)
	    			this.variable = StringId.EMPTY_LIST;
	    		else
	    			this.variable = ArrayTools.unmodifiableList(StringId.getStringAndId((String[])variable));
    		}
    		else if (variable instanceof ArrayTools.UnmodifiableArrayList)
    		{
    			this.variable = (ArrayTools.UnmodifiableArrayList<StringIdObject>)variable;
    		}
    		else
    		{
    			throw new IllegalArgumentException();
    		}
    	}  
    }
    public static interface GraphListener{
    	public void clickedUp(Graph graph);

    	public void clickedDown(Graph graph);

    	public void clickedRemove(Graph graph);
    }
    private Variable vars[];
    private final Matrix4d graphToGlobal = new Matrix4d();
    private SceneObject glObject = null;
    private Color color = Color.BLACK;
    private String function0=StringUtils.EMPTY, function1=StringUtils.EMPTY, function2=StringUtils.EMPTY, colorFunction = StringUtils.EMPTY, steps0="200", min0="-10", max0="10", steps1="200", min1="-10", max1="10", transformation = StringUtils.EMPTY;
    private Operation function0Op, function1Op, function2Op, colorOp, steps0Op, min0Op, max0Op, steps1Op, min1Op, max1Op, transformationOp;
    private ReadOnlySortedIntegerArrayList variables = SortedIntegerArrayList.EMPTY_LIST;
    private byte drawType = DrawType.DOTS;
    private boolean isSelected;
    private GraphKind kind;
    private boolean isVisible;
    private final VariableStack variableStack = new VariableStack(3,DataHandler.globalVariables);

    private final JColorPickPanel panelChooseColor  = new JColorPickPanel(color);
    private final JLabel labelChooseDrawType        = new JLabel(ProgramIcons.iconDots);
    private final JLabel labelChooseVisibility      = new JLabel(ProgramIcons.iconVisible);
    public final JLabel labelCalculating			= new JLabel(ProgramIcons.iconNotCalculating);
    private final JLabel labelUp                    = new JLabel(ProgramIcons.iconUp);
    private final JLabel labelDown                  = new JLabel(ProgramIcons.iconDown);
    private final JLabel labelDelete                = new JLabel(ProgramIcons.iconDelete);
    private final JLabel labelSummary               = new JLabel();
    private final Graph graph = this;
    private final ArrayList<GraphListener> graphListener = new ArrayList<Graph.GraphListener>(2);
    private final Controller controller = new Controller();
	private DoubleLine line0, line1;
    private WeakReference<GraphWindow> window = null;
    
    private final RunnableRunner.RunnableObject runnable = new RunnableRunner.RunnableObject("Graph", null){
        @Override
		public void run(){
    		try{
            	//long time = System.nanoTime();
                calcGraph(controller);
                //System.out.println("time:" + (System.nanoTime() - time)*0.000000001);
            }catch (Exception e){
            	logger.error("Exception at calculating Graph", e);
            }
        }
    };

    static{
    	updateActivation();
    }
    
    public final void updateGraph(){
		DataHandler.runnableRunner.run(runnable, false);
    }

    public static final void updateActivation(){
		final OperatingSystemMXBean bean =  ManagementFactory.getOperatingSystemMXBean();
		final BigInteger THOUSAND = BigInteger.valueOf(1000);
		activated = BigInteger
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
    }
    
    public boolean isRunning(){
    	return runnable.getState() == RunnableRunner.STATE_RUNNING;
    }
    
    public final GraphWindow getWindow(){
    	WeakReference<GraphWindow> window = this.window;
    	if (window == null)
    		return null;
    	GraphWindow w = window.get();
    	if (w == null)
    		this.window = null;
    	return w;
    }
    
	@Override
	public void mouseClicked(MouseEvent arg0) {}

	@Override
	public void mouseEntered(MouseEvent arg0) {}

	@Override
	public void mouseExited(MouseEvent arg0) {}

	@Override
	public void mousePressed (MouseEvent me){
		Object source = me.getSource();
		if (source == this)
		{
	        switch (me.getClickCount()){
	            case 1: setSelected(true);break;
	            case 2: openWindow();break;
	        }
			getParent().requestFocus();
		}
		else if (source == labelChooseDrawType)
		{
			int add = me.getButton() == 1 ? 1 : -1;
			int tmp = drawType;
			do{
				tmp = (tmp + add + drawTypeArray.length) % drawTypeArray.length; 
			}while (!drawTypeAllowed(drawTypeArray[tmp]));
            setDrawType(drawTypeArray[tmp]);
		}
		else if (source == labelChooseVisibility)
		{
            setGraphVisible(!isGraphVisible());        			
		}
		else if (source == labelCalculating)
		{
			updateGraph();
		}
		else if (source == labelUp)
		{
			for (int i=0;i<graphListener.size();i++){
        		try{
        			graphListener.get(i).clickedUp(graph);
        		}catch(Exception e){}
        	}
		}
		else if (source == labelDown)
		{
			for (int i=0;i<graphListener.size();i++){
        		try{
        			graphListener.get(i).clickedDown(graph);
        		}catch(Exception e){}
        	}
		}else if (source == labelDelete)
		{
			for (int i=0;i<graphListener.size();i++){
        		try{
        			graphListener.get(i).clickedRemove(graph);
        		}catch(Exception e){}
        	}
		}
    }
	
	@Override
	public void mouseReleased(MouseEvent arg0) {}

	@Override
	public void stateChanged(ChangeEvent e) {
        setColor(panelChooseColor.getBackground());
    }           
    
	private static final Dimension maximumSize = new Dimension(15,15);
	private static final Dimension preferredSize = new Dimension(100,15);
    public Graph(){
        setKind(GraphKind.GRAPH_2D_FUNCTION);
        setLayout(new BoxLayout(this,BoxLayout.X_AXIS));
        //setLayout(new FlowLayout());

        panelChooseColor.setMaximumSize(maximumSize);
        panelChooseColor.addChangeListener(this);
        add(panelChooseColor);

        labelChooseDrawType.addMouseListener(this);      
        add(labelChooseDrawType);
        labelChooseVisibility.addMouseListener(this);
        add(labelChooseVisibility);
        labelCalculating.addMouseListener(this);
        add(labelCalculating);
        labelUp.addMouseListener(this);
        add(labelUp);
        labelDown.addMouseListener(this);
        add(labelDown);
        labelDelete.addMouseListener(this);
        add(labelDelete);      
        add(labelSummary);
        addMouseListener(this);
               
        setPreferredSize(preferredSize);
        updateOperations();
        setGraphVisible(true);
    }
    
    public void addGraphListener(GraphListener gl){
    	graphListener.add(gl);
    }
    
    public void removeGraphListener(GraphListener gl){
    	graphListener.remove(gl);
    }
    
    public SceneObject getGlObject(){
    	return glObject;
    }

    public final boolean isSelected(){
        return isSelected;
    }

    public final void setSelected(boolean selected){
        if (isSelected == selected)
            return;
        if (selected)
        	for (Graph graph : Interface.getGraphs())
        		graph.setSelected(false);  
        isSelected = selected;
        final Color uic = UIManager.getColor(selected ? "List.selectionBackground" : "List.background");
        setBackground(uic != null ? uic : selected ? Color.BLUE : Color.WHITE);
    }
    
    private static final Operation compile(String str){
    	try {
			 return OperationCompiler.compile(str);
		} catch (OperationParseException e) {
			return RealDoubleOperation.NaN;
		}

    }
    
    private final void updateOperations(){
   		function0Op=kind.function0 == null	? RealDoubleOperation.NaN : compile(function0).calculate(null, controller);
 		function1Op=kind.function1 == null	? RealDoubleOperation.NaN : compile(function1).calculate(null, controller);
		function2Op=kind.function2 == null	? RealDoubleOperation.NaN : compile(function2).calculate(null, controller);
		colorOp    =						  							compile(colorFunction).calculate(null, controller);
		steps0Op=	kind.step0 == null		? RealDoubleOperation.NaN : compile(steps0).calculate(null, controller);
		min0Op=		kind.min0 == null		? RealDoubleOperation.NaN : compile(min0).calculate(null, controller);
		max0Op=		kind.max0 == null		? RealDoubleOperation.NaN : compile(max0).calculate(null, controller);
		steps1Op=	kind.step1 == null		? RealDoubleOperation.NaN : compile(steps1).calculate(null, controller);
		min1Op=		kind.min1 == null		? RealDoubleOperation.NaN : compile(min1).calculate(null, controller);
		max1Op=		kind.max1 == null		? RealDoubleOperation.NaN : compile(max1).calculate(null, controller);
		transformationOp=												compile(transformation).calculate(null, controller);
		
		SortedIntegerArrayList list = new SortedIntegerArrayList();
		OperationCalculate.getVariables(function0Op, list);
		OperationCalculate.getVariables(function1Op, list);
		OperationCalculate.getVariables(function2Op, list);
		OperationCalculate.getVariables(colorOp, list);
		OperationCalculate.getVariables(steps0Op, list);
		OperationCalculate.getVariables(min0Op, list);
		OperationCalculate.getVariables(max0Op, list);
		OperationCalculate.getVariables(steps1Op, list);
		OperationCalculate.getVariables(min1Op, list);
		OperationCalculate.getVariables(max1Op, list);
		OperationCalculate.getVariables(transformationOp, list);
		for (int i=0;i<kind.variable.size();++i)
   			list.removeObject(kind.variable.get(i).id);
   		variables = list.size() == 0 ? SortedIntegerArrayList.EMPTY_LIST : list.readOnly();
       	labelSummary.setText(getSummary());
        updateGraph();
    }
    
    public final void setContent(Iterable<String> content){
    	SaveLineCreator saveLineCreator = new SaveLineCreator();
    	for (String line : content){
        	final SaveLineCreator.SaveObject saveObject = saveLineCreator.getSaveObject(line);
        	if (saveObject != null){
                try{
                    setVariable(saveObject.variable, saveObject.value);
                }catch(Exception e){
                	logger.error("Can't read line:\"" + line + '\"' + e);
                }
        	}
    	}
    	updateOperations();
    }
    
    public int getDrawTypeIndex(int id){
    	for (int i=0;i<drawTypeArray.length;i++)
    		if (drawTypeArray[i] == id)
    			return i;
    	return -1;
    }
    
    private final void setVariable(String variable, String value){
    	if (variable.length()==0)
    		return;
    	switch(variable.charAt(0)){
	    	case 'c':{
	    		if (variable.equals("color")) setColor(new Color(Integer.parseInt(value)));
	    		else if (variable.equals("colorFunction")) colorFunction = value;
	    		break;
	    	}case 'd':{
	    		if (variable.equals("drawType"))setDrawType(drawTypeArray[getDrawTypeIndex(Integer.parseInt(value))]);
	    		break;
	    	}case 'f':{
	    		if (variable.length() == 2)
	    		{
	    			switch (variable.charAt(1))
	    			{
	    				case '0':function0 = value;break;
	    				case '1':function1 = value;break;
	    				case '2':function2 = value;break;
	    			}
	    		}
	            break;
	    	}case 'k':{
	            if      (variable.equals("kind"))setKind(getGraphKind(Integer.parseInt(value)));
	            break;
	    	}case 'm':{
	    		if 		(variable.equals("min0"))min0 = value;
	            else if (variable.equals("max0"))max0 = value;	                		
	            else if (variable.equals("min1"))min1 = value;
	            else if (variable.equals("max1"))max1 = value;
	    		break;
	    	}case 's':{
	            if (variable.equals("step0")){
	            	steps0 = '('+ max0 + '-' + min0 + ')' + '/' + '(' +value+")+1.00000001";
	            } else if (variable.equals("step1")){
	            	steps1 = '('+ max1 + '-' + min1 + ')' + '/' + '(' +value+")+1.00000001";
	            }
	            else if (variable.equals("steps0"))steps0= value;
	            else if (variable.equals("steps1"))steps1= value;
	            break;
	    	}case 't':{
	    		if (variable.equals("transformation"))transformation = value;
	    		break;
	    	}case 'v':{
	    		if (variable.equals("visible"))setGraphVisible(Boolean.parseBoolean(value));
	    		break;
	    	}
		}
    }
    
    public final void setContent(String content){
    	SaveLineCreator saveLineCreator = new SaveLineCreator();
    	int index=-1;
        do{
        	String line = content.substring(index+1, Math.max(content.length(), index = content.indexOf('\n', index+1)));
           	final SaveLineCreator.SaveObject saveObject = saveLineCreator.getSaveObject(line);
           	if (saveObject != null){
                try{
                    setVariable(saveObject.variable, saveObject.value);
	            }catch(Exception e){
	            	logger.error("Can't read line:\"" + line + '\"',e);
	            }
        	}
        }while(index!=-1);
        updateOperations();
    }

    public final StringBuilder getContent(StringBuilder erg){
    	if (activated){
	        SaveLineCreator.appendSaveLine("summary", getSummary(new StringBuilder()), erg);
	        SaveLineCreator.appendSaveLine("kind", kind.id, erg);
	        SaveLineCreator.appendSaveLine("f0", function0, erg);
	        SaveLineCreator.appendSaveLine("f1", function1, erg);
	        SaveLineCreator.appendSaveLine("f2", function2, erg);
	        SaveLineCreator.appendSaveLine("colorfunction", colorFunction, erg);
	        SaveLineCreator.appendSaveLine("min0", min0, erg);
	        SaveLineCreator.appendSaveLine("max0", max0, erg);
	        SaveLineCreator.appendSaveLine("steps0", steps0, erg);
	        SaveLineCreator.appendSaveLine("min1", min1, erg);
	        SaveLineCreator.appendSaveLine("max1", max1, erg);
	        SaveLineCreator.appendSaveLine("steps1", steps1, erg);
	        SaveLineCreator.appendSaveLine("drawType", glObject.getDrawType(), erg);
	        SaveLineCreator.appendSaveLine("color", color.getRGB(), erg);
	        SaveLineCreator.appendSaveLine("visible", isGraphVisible(), erg);
	        SaveLineCreator.appendSaveLine("transformation", transformation, erg);
    	}
        return erg;    	
    }
    
    public final String getContent(){
    	synchronized (stringBuilder) {
    		stringBuilder.setLength(0);
    	  	return getContent(stringBuilder).toString();		
		}
    }

    public final void closeWindow(){
    	GraphWindow window = getWindow();
    	if (window != null){
    		window.dispose();
    	}
    }
    
    public final boolean drawTypeAllowed(byte drawType){
    	if (kind.gLObjectType == 0 || kind.gLObjectType == 2)
    	{
    		return drawType == DrawType.DOTS
    		    || drawType == DrawType.LINES
    			|| drawType == DrawType.LINE_STRICLES;
    	}
    	else
    	{
    		return drawType == DrawType.DOTS
        		|| drawType == DrawType.LINES
    			|| drawType == DrawType.LINE_STRICLES
        		|| drawType == DrawType.SOLID
        		|| drawType == DrawType.SOLID_SMOOTH;
    	}
    }

    public final void setDrawType (byte drawType){
        ImageIcon icon;
        if (!drawTypeAllowed(drawType))
        {
        	throw new IllegalArgumentException();	
        }
        switch (drawType){
            case DrawType.DOTS:		icon = ProgramIcons.iconDots;break;
            case DrawType.LINES:		icon = ProgramIcons.iconLines;break;
            case DrawType.LINE_STRICLES:icon = ProgramIcons.iconLines;break;
            case DrawType.SOLID:		icon = ProgramIcons.iconSolid;break;
            case DrawType.SOLID_SMOOTH:icon = ProgramIcons.iconSmooth;break;    
            default:throw new IllegalArgumentException();
        }
        labelChooseDrawType.setIcon(icon);
        this.drawType = drawType;
        if (glObject != null)
        {
        	glObject.setDrawType(drawType);
            glObject.update(UpdateKind.MATERIAL);
        }
    	GraphWindow window = getWindow();
        if (window != null)
        	window.listDrawType.setSelectedIndex(getDrawTypeIndex(drawType));
    }

    public final byte getDrawType (){
        return drawType;
    }
    
    private final void setKind (GraphKind kind){
        this.kind = kind;
       	vars = new Variable[kind.variable.size()];
        for (int i=0;i<vars.length;i++)
        	vars[i]=new Variable(kind.variable.get(i));
        setColor(color);
    }

    public final boolean isGraphVisible (){
        return isVisible;
    }

    public final void setGraphVisible (boolean visible){
   		Icon icon = visible ? ProgramIcons.iconVisible : ProgramIcons.iconInvisible;
   		if (labelChooseVisibility.getIcon() != icon)
   			labelChooseVisibility.setIcon(icon);
   		if (glObject != null)
   		{
   	        glObject.setVisible(visible);   			
   		}
   		isVisible = visible;
    	GraphWindow window = getWindow();
        if (window != null)
        	window.checkBoxVisible.setSelected(visible);
    }

    public final Color getColor (){
        return color;
    }

    public final void setColor (Color c){
        color = c;
       	if (glObject == null)
    	{
    		return;
    	}
       	switch (kind.gLObjectType)
       	{
       		case 0:
       		case 2:
            glObject.lightMaterial.set(true, false, false, true, color);
            glObject.lightMaterial.set(false, true, true, false, Color.BLACK);
            break;
       	case 1:
            glObject.lightMaterial.set(true, true, true, false, color);
            glObject.lightMaterial.set(false, false, false, true, Color.BLACK);
            break;
            default:
            	throw new RuntimeException();            	
       	}
        glObject.reflectionMaterial.set(true, true, true, false, Color.BLACK);
        glObject.reflectionMaterial.set(false, false, false, true, color);
        glObject.reflectionMaterial.Ns = glObject.lightMaterial.Ns = 100;
        panelChooseColor.setBackground(color);
    	GraphWindow window = getWindow();
        if (window != null)
        	window.buttonColor.setBackground(color);
        glObject.update(UpdateKind.MATERIAL);
    }
    
    /**
     * Gibt Alle Variablennamen zur\u00FCck, die den Graphen ver\u00E4ndern k\u00F6nnen. Die Sortierung ist Alphabetisch und kein Name kommt doppelt vor
     * @return List<String> Unmodifizierbare List mit Variablen
     */
    public final ReadOnlySortedIntegerArrayList getPendentVariables(){
    	return variables;
    }
    
    private final boolean calcGraph(Controller control) throws OperationParseException{
    	//final long t0 = System.nanoTime();
    	control.calculateLoop(true);
    	control.calculateRandom(true);
        final double min0 = min0Op.calculate(DataHandler.globalVariables, control).doubleValue();
        final double max0 = max0Op.calculate(DataHandler.globalVariables, control).doubleValue();
        final int steps0 =(int)steps0Op.calculate(DataHandler.globalVariables, control).longValue();
        final double min1 = min1Op.calculate(DataHandler.globalVariables, control).doubleValue();
        final double max1 = max1Op.calculate(DataHandler.globalVariables, control).doubleValue();
        final int steps1 =(int)steps1Op.calculate(DataHandler.globalVariables, control).longValue();
        final boolean useMatrix = OperationGeometry.parseMatRowMajor(transformationOp.calculate(DataHandler.globalVariables, control), graphToGlobal);
        
        final GraphKind kind = this.kind;
    	control.calculateLoop(false);
    	control.calculateRandom(false);
        
        SceneObjectLine glObjectLine = null;
        SceneObjectPlane glObjectPlane = null;
        SceneObjectVektor glObjectVektor = null;
        SceneObjectPointCloud glObjectPointCloud = null;
        boolean updateCoordinates = false;
        
    	RealDoubleOperation opsVar0[] = null, opsVar1[] = null;
        if (kind == GraphKind.GRAPH_2D_FUNCTION || kind == GraphKind.GRAPH_2D_PARAMETRIC || kind == GraphKind.GRAPH_2D_POLAR || kind == GraphKind.GRAPH_3D_LINE || kind == GraphKind.GRAPH_3D_FUNCTION || kind == GraphKind.GRAPH_3D_PARAMETRIC || kind == GraphKind.GRAPH_3D_CARTESIAN || kind == GraphKind.GRAPH_3D_POLAR || kind == GraphKind.GRAPH_3D_VECTORFIELD){
        	if (steps0 < 1)
        		return false;
        	if (line0 == null || line0.min != min0 || line0.max != max0 || line0.size != steps0){
        		line0 = DoubleLine.get(min0, max0, steps0);
        		updateCoordinates = true;
	        }
        	opsVar0 = line0.data;
        }
        if (kind == GraphKind.GRAPH_3D_FUNCTION || kind == GraphKind.GRAPH_3D_PARAMETRIC || kind == GraphKind.GRAPH_3D_CARTESIAN || kind == GraphKind.GRAPH_3D_POLAR || kind == GraphKind.GRAPH_3D_VECTORFIELD){
	        if (steps0 < 2 || steps1 < 2)
	        	return false;
        	if (line1 == null || line1.min != min1 || line1.max != max1 || line1.size != steps1){
	        	line1 = DoubleLine.get(min1, max1, steps1);
	        	updateCoordinates = true;
        	}
        	opsVar1 = line1.data;
        }

        boolean changedGlObject = false;
        SceneObject oldObject = glObject;
	    try{
	    	switch (kind)
	    	{
	    		case GRAPH_2D_POLAR:
	    		case GRAPH_2D_FUNCTION:
	    		case GRAPH_2D_PARAMETRIC:
	    		case GRAPH_3D_LINE:
	    			if (!(glObject instanceof SceneObjectLine)){
			            glObject = new SceneObjectLine();
			            changedGlObject = true;
			    	}
			        glObjectLine = (SceneObjectLine)glObject;
		        	glObjectLine.setSize(steps0);
		        	break;
	    		case GRAPH_2D_PLOT:
	    		case GRAPH_3D_PLOT:
	    			if (!(glObject instanceof SceneObjectLine)){
			            glObject = new SceneObjectLine();
			            changedGlObject = true;
			        }
			        glObjectLine = (SceneObjectLine)glObject;
			        break;
	    		case GRAPH_3D_FUNCTION:
	    		case GRAPH_3D_PARAMETRIC:
	    		case GRAPH_3D_CARTESIAN:
	    		case GRAPH_3D_POLAR:
	    			if (!(glObject instanceof SceneObjectPlane)){
			            glObject = new SceneObjectPlane();
			            changedGlObject = true;
			        }
			        glObjectPlane = (SceneObjectPlane)glObject;
			        if (glObjectPlane.getSizeX() != steps0 || glObjectPlane.getSizeY() != steps1)
			        {
			        	glObjectPlane.setSize(steps0, steps1);
			        	updateCoordinates = true;
			        }
		        	break;
	    		case GRAPH_3D_VEKTOR:
	    			if (!(glObject instanceof SceneObjectVektor)){
			            glObject = new SceneObjectVektor();
			            changedGlObject = true;
			        }
			        glObjectVektor = (SceneObjectVektor)glObject;
	    			break;
	    		case GRAPH_3D_VECTORFIELD:
	    			if (!(glObject instanceof SceneObjectPointCloud)){
			            glObject = new SceneObjectPointCloud();
			            changedGlObject = true;
			        }
			        glObjectPointCloud = (SceneObjectPointCloud)glObject;
			        glObjectPointCloud.setSize(steps0, steps0, steps1);
	    			break;
	    	}
       }catch(OutOfMemoryError e){
        	logger.error("Not enough Memory", e);
        	return false;
        }
	    if (updateCoordinates)
	    {
	    	if (kind == GraphKind.GRAPH_3D_FUNCTION)
	    	{
            	final float vertexX[] = glObjectPlane.getVerticesX();
            	final float vertexY[] = glObjectPlane.getVerticesY();
		    	for (int i = 0, index = 0; i < steps0; ++i)
		    	{
		    		for (int j = 0; j < steps1; ++j, ++index)
		    		{
		    			vertexX[index]=(float)opsVar0[i].doubleValue();
		    			vertexY[index]=(float)opsVar1[j].doubleValue();
		    		}
		    	}
	    	}
	    	else if (kind == GraphKind.GRAPH_2D_FUNCTION)
	    	{
	    		/*for (int i=0;i<steps0;i++){
                    v0.setValue(opsVar0[i]);
                    final Operation tmp = operation0.calculate(variableStack, control);
                    vertices[i * 3] = (float)opsVar0[i].doubleValue();
                    vertices[i * 3 + 1] = (float)tmp.doubleValue();
                    vertices[i * 3 + 2] = (float)tmp.doubleValueImag();
                }*/
	    	}
	    }

        if (changedGlObject){
        	if (oldObject != null)
        	{
        		Interface.scene.removeGlObject(oldObject);
        	}
        	Interface.scene.addGlObject(glObject);
        	setColor(color);
            setGraphVisible(isVisible);
            glObject.setDrawType(drawType);
        }

        variableStack.clear();
        for (int i=0;i<vars.length;i++){
        	vars[i].setValue((Operation)null);
        	variableStack.addLocal(vars[i]);
        }
        final Variable v0 = vars.length > 0 ? vars[0] : null;
        final Variable v1 = vars.length > 1 ? vars[1] : null;
        final Variable v2 = vars.length > 2 ? vars[2] : null;

        Operation operation0 = function0Op.calculate(variableStack, control);
        Operation operation1 = function1Op.calculate(variableStack, control);
        Operation operation2 = function2Op.calculate(variableStack, control);
        Operation colorOp 	 = this.colorOp == null ? null : this.colorOp.calculate(variableStack, control);
    	//final long t1 = System.nanoTime();
        switch (kind){
            case GRAPH_2D_FUNCTION:{
            	control.calculateLoop(true);
            	control.calculateRandom(true);
                final float vertices[] = glObjectLine.getVertices();
                if (useMatrix){
                    for (int i=0;i<steps0;i++){
                    	final double x = opsVar0[i].value;
                        v0.setValue(opsVar0[i]);
                        final Operation tmp = operation0.calculate(variableStack, control);
                        final double re = tmp.doubleValue(), imag = tmp.doubleValueImag();
                        graphToGlobal.rdotAffine(x, re, imag, vertices, i * 3);                    }                	
                }else{
                    for (int i=0;i<steps0;i++){
                        v0.setValue(opsVar0[i]);
                        final Operation tmp = operation0.calculate(variableStack, control);
                        vertices[i * 3] = (float)opsVar0[i].doubleValue();
                        vertices[i * 3 + 1] = (float)tmp.doubleValue();
                        vertices[i * 3 + 2] = (float)tmp.doubleValueImag();
                    }
                }
            	control.calculateLoop(false);
            	control.calculateRandom(false);
                break;
            }case  GRAPH_2D_PARAMETRIC :{
            	control.calculateLoop(true);
            	control.calculateRandom(true);
                final float vertices[] = glObjectLine.getVertices();
                if (graphToGlobal != null){
                    for (int i=0;i<steps0;i++){
                        v0.setValue(opsVar0[i]);
                        final double tmp0 =operation0.calculate(variableStack, control).doubleValue(), tmp1 = operation1.calculate(variableStack, control).doubleValue();
                        graphToGlobal.rdotAffine(tmp0, tmp1, 0, vertices, i * 3);                    }            		
                }else{
                    for (int i=0;i<steps0;i++){
                        v0.setValue(opsVar0[i]);
                        vertices[i * 3] = (float)operation0.calculate(variableStack, control).doubleValue();
                        vertices[i * 3 + 1] = (float)operation1.calculate(variableStack, control).doubleValue();
                        vertices[i * 3 + 2] = 0f;
                    }
                }
            	control.calculateLoop(false);
            	control.calculateRandom(false);
                break;
           }case GRAPH_2D_POLAR:{
            	control.calculateLoop(true);
            	control.calculateRandom(true);
                final float vertices[] = glObjectLine.getVertices();
                if (useMatrix){
                    for (int i=0;i<steps0;i++){
                        v0.setValue(opsVar0[i]);
                        final double t = opsVar0[i].value;
                        final double result = operation0.calculate(variableStack, control).doubleValue(), x = Math.sin(t)*result, y = Math.cos(t)*result;
                        graphToGlobal.rdotAffine(x, y, 0, vertices, i * 3);
                    }
                }else{
                    for (int i=0;i<steps0;i++){
                        v0.setValue(opsVar0[i]);
                        final double t = opsVar0[i].value;
                        final double result = operation0.calculate(variableStack, control).doubleValue();
                        vertices[i * 3] = (float)(Math.sin(t)*result);
                        vertices[i * 3 + 1] = (float)(Math.cos(t)*result);
                        vertices[i * 3 + 2] = 0f;
                    }
                }
            	control.calculateLoop(false);
            	control.calculateRandom(false);
               break;
            }case GRAPH_2D_PLOT:{
            	control.calculateLoop(true);
            	control.calculateRandom(true);
                operation0 = operation0.calculate(variableStack, control);
                operation1 = operation1.calculate(variableStack, control);
                if (operation0.isArray() && operation1.isArray()){
                	if (operation0.size() == operation1.size()){
                		final float vertices[] = glObjectLine.getVertices();
                        final int length = operation0.size();
                		glObjectLine.setSize(length);        	
                		if (useMatrix){
                			for (int i=0;i<length;i++){
                				final double x = operation0.get(i).doubleValue(), y = operation1.get(i).doubleValue();
                                graphToGlobal.rdotAffine(x, y, 0, vertices, i * 3);                        
                            }
                		}else{
                			for (int i=0;i<length;i++){
                                vertices[i * 3]=(float)operation0.get(i).doubleValue();
                                vertices[i * 3 + 1]=(float)operation1.get(i).doubleValue();
                                vertices[i * 3 + 2]=0f;
                			}                			
                		}
                	}
                }
            	control.calculateLoop(false);
            	control.calculateRandom(false);
                break;
            }case GRAPH_3D_LINE:{
                final float vertices[] = glObjectLine.getVertices();
            	control.calculateLoop(true);
            	control.calculateRandom(true);
                if (useMatrix){
                    for (int i=0;i<steps0;i++){
                        v0.setValue(opsVar0[i]);
                        final double x = operation0.calculate(variableStack, control).doubleValue(), y = operation1.calculate(variableStack, control).doubleValue(), z = operation2.calculate(variableStack, control).doubleValue();
                        graphToGlobal.rdotAffine(x, y, z, vertices, i * 3);      			
                    }
                }else{
                	for (int i=0;i<steps0;i++){
                        v0.setValue(opsVar0[i]);
                        vertices[i * 3] = (float)operation0.calculate(variableStack, control).doubleValue();
                        vertices[i * 3 + 1] = (float)operation1.calculate(variableStack, control).doubleValue();
                        vertices[i * 3 + 2] = (float)operation2.calculate(variableStack, control).doubleValue();                	
                    }
                }
            	control.calculateLoop(false);
            	control.calculateRandom(false);
               break;
            }case GRAPH_3D_FUNCTION:{
            	final float vertexX[] = glObjectPlane.getVerticesX();
            	final float vertexY[] = glObjectPlane.getVerticesY();
            	final float vertexZ[] = glObjectPlane.getVerticesZ();
            	int index =0;
            	control.connectEmptyVariables(true);
                for (int i=0;i<steps0;i++){
                    v0.setValue(opsVar0[i]);
                    v1.setValue((Operation)null);
                    final Operation tmp0 = operation0.calculate(variableStack, control);
                    control.calculateLoop(true);
                	control.calculateRandom(true);
                    if (useMatrix){
                        final double x = opsVar0[i].doubleValue();
                        for (int j=0;j<steps1;j++, index++){
                          	final RealDoubleOperation op = opsVar1[j];
                            final double y = op.doubleValue();
                            v1.setValue(op);                            
                            final double z = tmp0.calculate(variableStack, control).doubleValue();
                            vertexX[index]=(float)graphToGlobal.rdotX(x, y, z, 1);
                            vertexY[index]=(float)graphToGlobal.rdotY(x, y, z, 1);
                            vertexZ[index]=(float)graphToGlobal.rdotZ(x, y, z, 1);
                        }
                    }else{
                        //final float x = (float)opsVar0[i].doubleValue();
                        for (int j=0;j<steps1;j++, index++){
                            v1.setValue(opsVar1[j]);
                            //vertexX[index]=x;
                            //vertexY[index]=(float)op.doubleValue();
                            vertexZ[index]=(float)tmp0.calculate(variableStack, control).doubleValue();
                        }
                    }              
                	control.calculateLoop(false);
                	control.calculateRandom(false);
               }
               control.connectEmptyVariables(false);
               break;
            }case GRAPH_3D_PARAMETRIC:{
                final float vertexX[] = glObjectPlane.getVerticesX();
                final float vertexY[] = glObjectPlane.getVerticesY();
                final float vertexZ[] = glObjectPlane.getVerticesZ();
                int index =0;
                for (int i=0;i<steps0;i++){
                    v0.setValue(opsVar0[i]);
                    v1.setValue((Operation)null);
                    final Operation tmp0 = operation0.calculate(variableStack, control);
                    final Operation tmp1 = operation1.calculate(variableStack, control);
                    final Operation tmp2 = operation2.calculate(variableStack, control);
                	control.calculateLoop(true);
                	control.calculateRandom(true);
                	if (useMatrix){
                        for (int j=0;j<steps1;j++, index++){
                            v1.setValue(opsVar1[j]);
                            final double x = tmp0.calculate(variableStack, control).doubleValue(), y = tmp1.calculate(variableStack, control).doubleValue(), z = tmp2.calculate(variableStack, control).doubleValue();
                            vertexX[index]=(float)graphToGlobal.rdotX(x, y, z, 1);
                            vertexY[index]=(float)graphToGlobal.rdotY(x, y, z, 1);
                            vertexZ[index]=(float)graphToGlobal.rdotZ(x, y, z, 1);
                        }
                    }else{
                        for (int j=0;j<steps1;j++, index++){
                            v1.setValue(opsVar1[j]);
                            vertexX[index]=(float)tmp0.calculate(variableStack, control).doubleValue();
                            vertexY[index]=(float)tmp1.calculate(variableStack, control).doubleValue();
                            vertexZ[index]=(float)tmp2.calculate(variableStack, control).doubleValue();
                        }
                    }
                	control.calculateLoop(false);
                	control.calculateRandom(false);
                }
                break;
            }case GRAPH_3D_PLOT:{
            	control.calculateRandom(true);
            	control.calculateLoop(true);
            	operation0 = operation0.calculate(variableStack, control);
                operation1 = operation1.calculate(variableStack, control);
                operation2 = operation2.calculate(variableStack, control);
                colorOp = colorOp.calculate(variableStack, control);
                if (operation0.isArray() && operation1.isArray() && operation0.size() == operation1.size() && operation1.size() == operation2.size()){
               		final int length = operation0.size();
               		glObjectLine.setSize(length);        	
                    final float vertices[] = glObjectLine.getVertices();
                	control.calculateLoop(true);
                	control.calculateRandom(true);
                    if (useMatrix){
            			for (int i=0;i<length;i++){         				
                            final double x = operation0.get(i).doubleValue(), y = operation1.get(i).doubleValue(), z = operation2.get(i).doubleValue();
                            graphToGlobal.rdotAffine(x, y, z, vertices, i * 3);                        
                        }
            		}else{
            			for (int i=0;i<length;i++){
        				    vertices[i * 3]=(float)operation0.get(i).doubleValue();
                            vertices[i * 3 + 1]=(float)operation1.get(i).doubleValue();
                            vertices[i * 3 + 2]=(float)operation2.get(i).doubleValue();
            			}                			
            		}
                	control.calculateLoop(false);
                	control.calculateRandom(false);
                }
                if (colorOp != null && colorOp.isArray())
                {
                	glObjectLine.setVertexColorActivated(true);
                	float vertexColor[] = glObjectLine.getVertexColor();
                	for (int i = 0; i < colorOp.size(); ++i)
                	{
                		Operation sub = colorOp.get(i);
                		if (sub instanceof ArrayOperation && sub.size() == 3)
            			{
                			for (int j = 0; j < sub.size(); ++j)
                			{
                				vertexColor[i * 4 +j] = (float)sub.get(j).doubleValue();
                			}
            			}
                	}
                }
            	control.calculateRandom(false);
            	control.calculateLoop(false);
                break;
            }case GRAPH_3D_VEKTOR:{
            	control.calculateLoop(true);
            	control.calculateRandom(true);
            	operation0 = operation0.calculate(variableStack, control);
                operation1 = operation1.calculate(variableStack, control);
                if (operation0.isArray() && operation1.isArray() && operation0.size() == 3 && operation1.size() == 3){
                	glObjectVektor.start.set(
                			(float)operation0.get(0).doubleValue(),
                			(float)operation0.get(1).doubleValue(),
                			(float)operation0.get(2).doubleValue());
                	glObjectVektor.end.set(
                			(float)operation1.get(0).doubleValue(),
                			(float)operation1.get(1).doubleValue(),
                			(float)operation1.get(2).doubleValue());
                }
            	control.calculateLoop(false);
            	control.calculateRandom(false);
                break;
            }case GRAPH_3D_CARTESIAN:{
                operation0 = operation0.calculate(variableStack, control);
                if (!(operation0 instanceof EqualsOperation))
                	return false;
                int hit=-1;
                StringId.StringIdObject hitString;
                Operation solved = null;
                do{
                	if (++hit == kind.variable.size())
                	{
     	               	Operation normalform = SubtractionOperation.calculate(solved.get(0).get(0), solved.get(0).get(1), control);
                		Operation diffX = DifferentiationOperation.calculate(normalform, new UserVariableOperation(kind.variable.get(0)), control);
                		Operation diffY = DifferentiationOperation.calculate(normalform, new UserVariableOperation(kind.variable.get(1)), control);
                		Operation diffZ = DifferentiationOperation.calculate(normalform, new UserVariableOperation(kind.variable.get(2)), control);
                		int index = 0;
                    	final float vertex0[] = glObjectPlane.getVerticesX();
                    	final float vertex1[] = glObjectPlane.getVerticesY();
                    	final float vertex2[] = glObjectPlane.getVerticesZ();
                    	double toRad0 = Math.PI / steps0;
                    	double toRad1 = Math.PI * 2. / steps1;
                		for (int i = 0; i < steps0; ++i)
                		{
                			for (int j = 0; j < steps1; ++j)
                			{
	                			double x = Math.sin(i * toRad0) * Math.sin(j * toRad1);
	           					double y = Math.sin(i * toRad0) * Math.cos(j * toRad1);
	           					double z = Math.cos(i * toRad0);
	                			for (int k = 0; k < 20; ++k)
	                			{
		                			v0.setValue(x);
		                			v1.setValue(y);
		                			v2.setValue(z);
		                			double gradX = diffX.calculate(variableStack, control).doubleValue();
		                			double gradY = diffY.calculate(variableStack, control).doubleValue();
		                			double gradZ = diffZ.calculate(variableStack, control).doubleValue();
		                			double normalize = normalform.calculate(variableStack, control).doubleValue()/(gradX * gradX + gradY * gradY + gradZ * gradZ);
		                			x -= gradX * normalize;
		                			y -= gradY * normalize;
		                			z -= gradZ * normalize;
	                			}
	                			vertex0[index] = (float)x;
	                			vertex1[index] = (float)y;
	                			vertex2[index] = (float)z;
	                			++index;
                			}
                		}
                		glObject.update(UpdateKind.DATA);
                		return true;        	                		
                	}
                	solved = SolveOperation.calculate(operation0, new UserVariableOperation(hitString = kind.variable.get(hit)), control);
                }while (!(solved instanceof EqualsOperation && solved.get(0) instanceof UserVariableOperation && ((UserVariableOperation)(solved.get(0))).nameObject == hitString));
                final Variable variable0 = hit == 0 ? vars[1] : vars[0];
                final Variable variable1 = hit == 2 ? vars[1] : vars[2];
                solved = solved.get(1);
                int index = 0;
                               
                for (int i=0;i<steps0;i++){
                    final double vl0 = opsVar0[i].value;
                	variable0.setValue(opsVar0[i]);
                    variable1.setValue((Operation)null);
                    final float vl0f = (float)vl0;
                    final Operation tmp0 = solved.calculate(variableStack, control);
                    
                	control.calculateLoop(true);
                	control.calculateRandom(true);
                    if (!useMatrix){
                    	final float vertex0[] = hit == 0 ? glObjectPlane.getVerticesY() : glObjectPlane.getVerticesX();
                    	final float vertex1[] = hit != 2 ? glObjectPlane.getVerticesZ() : glObjectPlane.getVerticesY();
                    	final float vertex2[] = hit == 2 ? glObjectPlane.getVerticesZ() : (hit == 0 ? glObjectPlane.getVerticesX() : glObjectPlane.getVerticesY());
                        for (int j=0;j<steps1;j++, index++){
                            variable1.setValue(opsVar1[j]);
                            vertex0[index]=vl0f;
                            vertex1[index]=(float)opsVar1[j].doubleValue();
                            vertex2[index]=(float)tmp0.calculate(variableStack, control).doubleValue();
                        }
                    }else{
                    	final float vertex0[] = glObjectPlane.getVerticesX();
                    	final float vertex1[] = glObjectPlane.getVerticesY();
                    	final float vertex2[] = glObjectPlane.getVerticesZ();
                    	switch(hit){
                    		case 2:{
		                        for (int j=0;j<steps1;j++, index++){
		                        	final double vl1 = opsVar1[j].value;
		                            variable1.setValue(opsVar1[j]);
		                            vertex0[index]=(float)graphToGlobal.rdotX(vl0, vl1, tmp0.calculate(variableStack, control).doubleValue(), 1);
		                            vertex1[index]=(float)graphToGlobal.rdotY(vl0, vl1, tmp0.calculate(variableStack, control).doubleValue(), 1);
		                            vertex2[index]=(float)graphToGlobal.rdotZ(vl0, vl1, tmp0.calculate(variableStack, control).doubleValue(), 1);
		                        }
		                        break;
                    		}case 1:{
                    			for (int j=0;j<steps1;j++, index++){
		                        	final double vl1 = opsVar1[j].value;
		                            variable1.setValue(opsVar1[j]);
                                    vertex0[index]=(float)graphToGlobal.rdotX(vl0, tmp0.calculate(variableStack, control).doubleValue(), vl1, 1);
                                    vertex1[index]=(float)graphToGlobal.rdotY(vl0, tmp0.calculate(variableStack, control).doubleValue(), vl1, 1);
                                    vertex2[index]=(float)graphToGlobal.rdotZ(vl0, tmp0.calculate(variableStack, control).doubleValue(), vl1, 1);
                    			}
                    			break;
                    		}case 0:{
                                for (int j=0;j<steps1;j++, index++){
		                        	final double vl1 = opsVar1[j].value;
		                            variable1.setValue(opsVar1[j]);
                                    vertex0[index]=(float)graphToGlobal.rdotX(tmp0.calculate(variableStack, control).doubleValue(), vl0, vl1, 1);
                                    vertex1[index]=(float)graphToGlobal.rdotY(tmp0.calculate(variableStack, control).doubleValue(), vl0, vl1, 1);
                                    vertex2[index]=(float)graphToGlobal.rdotZ(tmp0.calculate(variableStack, control).doubleValue(), vl0, vl1, 1);
                                }
                                break;
                    		}
                    	}
                    }
                	control.calculateLoop(false);
                	control.calculateRandom(false);
                }
                break;
            }case GRAPH_3D_POLAR:{
            	final float vertexX[] = glObjectPlane.getVerticesX();
            	final float vertexY[] = glObjectPlane.getVerticesY();
            	final float vertexZ[] = glObjectPlane.getVerticesZ();
                for (int i=0;i<steps0;i++){
                    v0.setValue(opsVar0[i]);
                    v1.setValue((Operation)null);
                    final double value0 = opsVar0[i].doubleValue();
                    final Operation tmp0 = operation0.calculate(variableStack, control);
                	control.calculateLoop(true);
                	control.calculateRandom(true);
                	int index = steps1 * i;
                    if (useMatrix){
                        for (int j=0;j<steps1;j++, index++){
                         	final RealDoubleOperation op = opsVar1[i];
                            final double value1 = op.value;
                            v1.setValue(op);                            
                            final double x = Math.cos(value0)*value1;
                            final double y = Math.sin(value0)*value1;
                            final double z = tmp0.calculate(variableStack, control).doubleValue();
                            vertexX[index]=(float)graphToGlobal.rdotX(x, y, z, 1);
                            vertexY[index]=(float)graphToGlobal.rdotY(x, y, z, 1);
                            vertexZ[index]=(float)graphToGlobal.rdotZ(x, y, z, 1);
                        }
                    }else{
                        for (int j=0;j<steps1;j++, index++){
                        	final RealDoubleOperation op = opsVar1[j];
                            v1.setValue(op);
                            final double value1 = op.doubleValue();
                            vertexX[index]=(float)(Math.cos(value0)*value1);
                            vertexY[index]=(float)(Math.sin(value0)*value1);
                            vertexZ[index]=(float)tmp0.calculate(variableStack, control).doubleValue();
                        }
                    }
                	control.calculateLoop(false);
                	control.calculateRandom(false);
                }
            	break;
            }case GRAPH_3D_VECTORFIELD:{
                final float vertexX[] = glObjectPointCloud.getVerticesX();
                final float vertexY[] = glObjectPointCloud.getVerticesY();
                final float vertexZ[] = glObjectPointCloud.getVerticesZ();
                int index =0;
                for (int i=0;i<steps0;i++){
                	if (opsVar0 == null)
                	{
                		throw new NullPointerException();
                	}
                	if (v0 == null)
                	{
                		throw new NullPointerException();
                	}
                    v0.setValue(opsVar0[i]);
                    v1.setValue((Operation)null);
                    v2.setValue((Operation)null);
                    final Operation tmp0 = operation0.calculate(variableStack, control);
                    final Operation tmp1 = operation1.calculate(variableStack, control);
                    final Operation tmp2 = operation2.calculate(variableStack, control);
                	for (int k = 0; k < steps0; ++k)
                	{
                		v1.setValue(opsVar0[k]);
                		v2.setValue((Operation)null);
	                    control.calculateLoop(true);
	                	control.calculateRandom(true);
	                	if (useMatrix){
	                        for (int j=0;j<steps1;j++, index++){
	                            v2.setValue(opsVar1[j]);
	                            final double x = tmp0.calculate(variableStack, control).doubleValue(), y = tmp1.calculate(variableStack, control).doubleValue(), z = tmp2.calculate(variableStack, control).doubleValue();
	                            vertexX[index]=(float)graphToGlobal.rdotX(x, y, z, 1);
	                            vertexY[index]=(float)graphToGlobal.rdotY(x, y, z, 1);
	                            vertexZ[index]=(float)graphToGlobal.rdotZ(x, y, z, 1);
	                        }
	                    }else{
	                        for (int j=0;j<steps1;j++, index++){
	                            v2.setValue(opsVar1[j]);
	                            vertexX[index]=(float)tmp0.calculate(variableStack, control).doubleValue();
	                            vertexY[index]=(float)tmp1.calculate(variableStack, control).doubleValue();
	                            vertexZ[index]=(float)tmp2.calculate(variableStack, control).doubleValue();
	                        }
	                    }
	                	control.calculateLoop(false);
	                	control.calculateRandom(false);
                	}
                }
                break;
            }
        }
    	/*final long t2 = System.nanoTime();
    	if (false){
    		//System.out.println((float)(t1-t0)/(t2-t0));
    		System.out.println((float)(t2-t0)/1000000);
    	}*/
        glObject.update(UpdateKind.DATA);
        return true;
    }
    
    private static final class DoubleLine{
    	private final double min, max, step, dist;
    	private final int size;
    	private final RealDoubleOperation data[];
    	private static final ArrayList<WeakReference<DoubleLine>> doubleLines = new ArrayList<WeakReference<DoubleLine>>();
    	
    	public final RealDoubleOperation getNearest(double d)
    	{
    		if (d <= min)
    		{
    			return data[0];
    		}
    		if (d >= max)
    		{
    			return data[data.length - 1];
    		}
    		return data[(int)(data.length * (d - min) / dist)];
    	}
    	
    	private DoubleLine(double min, double max, int size, boolean reuse){
        	this.step = (dist = ((this.max = max) - (this.min = min))) / ((this.size = size) - 1);
        	data = new RealDoubleOperation[size];
        	if (reuse)
        	{
        		for (int j = 0; j < doubleLines.size(); ++j)
        		{
        			DoubleLine dl = doubleLines.get(j).get();
        			if (dl != null)
        			{
		        		for (int i = 0; i < size; ++i)
		        		{
		        			if (data[i] == null)
		        			{
			        			double value = step * i + min;
			        			RealDoubleOperation op = dl.getNearest(value);
			        			if (op.value == value)
			        			{
			        				data[i] = op;
			        			}
		        			}
		        		}
	        		}
        		}
        		int number = 0;
    			for (int i = 0; i < size; ++i)
    			{
					if (data[i] == null)
					{
						data[i] = new RealDoubleOperation(step*i+min);
						++number;
					}
    			}
    			System.out.println("reused:" + (size - number) + '/' + size);
    		}
        	else
        	{
        		for (int i=0;i<size;++i)
        			data[i] = new RealDoubleOperation(step*i+min);
        	}
    	}
    	
    	public static final synchronized DoubleLine get(double min, double max, int size){
    		for (int i=0;i<doubleLines.size();++i){
    			DoubleLine line = doubleLines.get(i).get();
    			if (line == null){
    				doubleLines.remove(i);
    				--i;
    			}else if(line.min == min && line.max == max && line.size == size){
    				return line;
    			}
    		}
    		DoubleLine line = new DoubleLine(min, max, size, true);
    		doubleLines.add(new WeakReference<Graph.DoubleLine>(line));
    		return line;
    	}
    }
    
    private static final GraphKind getGraphKind(int id){
    	for (GraphKind gKind:graphKinds)
    		if (gKind.id == id)
    			return gKind;
    	throw new IllegalArgumentException();
    }
          
    public String getSummary(){
    	return getSummary(new StringBuilder()).toString();
    }
    
    public final StringBuilder getSummary(StringBuilder strB){
    	if (kind.function0 != null)
    		strB.append(kind.function0).append(function0).append(' ');
    	if (kind.function1 != null)
    		strB.append(kind.function1).append(function1).append(' ');
    	if (kind.function2 != null)
    		strB.append(kind.function2).append(function2).append(' ');
    	return strB;
    }

    public void destroy(){
    	closeWindow();
        glObject.destroy();
        Controller control = this.controller;
        if (control != null)
        	control.setStopFlag(true);
        glObject = null;
    }
    
    public void openWindow(){
    	GraphWindow window = getWindow();
    	if (window == null){
    		window = new GraphWindow();
    		this.window = new WeakReference<GraphWindow>(window);
    		final GraphWindow tmp = window;
    		ActionListener al = new ActionListener()
			{
    			@Override
				public void actionPerformed(ActionEvent ae){
    				Object source = ae.getSource();
    				if (source == tmp.buttonOkay)
    				{
    					saveWindow();
    					closeWindow();
    				}
    				else if (source == tmp.buttonAccept)
					{
    					saveWindow();
					}
                }
			};
            window.buttonColor.addChangeListener(
            	new ChangeListener(){
                     @Override
    				public void stateChanged(ChangeEvent e) {
                        setColor(tmp.buttonColor.getBackground());
                    }           
                }
            );
            
            window.buttonOkay.addActionListener(al);
            window.buttonAccept.addActionListener(al);
        	window.buttonAbort.addActionListener(JFrameUtils.closeParentWindowListener);
    	}
    	window.listKind.setSelectedItem(kind);
    	window.listDrawType.setSelectedIndex(getDrawTypeIndex(drawType));
    	window.checkBoxVisible.setSelected(isGraphVisible());
    	window.textFieldFunction0.setText(function0);
    	window.textFieldFunction1.setText(function1);
    	window.textFieldFunction2.setText(function2);
    	window.textFieldColor.setText(colorFunction);
    	window.textFieldMin0.setText(min0);
    	window.textFieldMax0.setText(max0);
    	window.textFieldMin1.setText(min1);
    	window.textFieldMax1.setText(max1);
    	window.textFieldStep0.setText(steps0);
    	window.textFieldStep1.setText(steps1);
    	window.textFieldTransformation.setText(transformation);
    	window.setWindowKind(kind);
    	window.buttonColor.setBackground(getColor());
    	window.checkBoxVisible.setSelected(glObject.isVisible());
    	window.listDrawType.setSelectedIndex(getDrawTypeIndex(drawType));
    	window.openWindow();
    }
    
    public void saveWindow(){
    	GraphWindow window = getWindow();
    	if (window == null)
    		return;
        setKind(window.tmpKind);
        function0 = window.textFieldFunction0.getText();
        function1 = window.textFieldFunction1.getText();
        function2 = window.textFieldFunction2.getText();
        colorFunction = window.textFieldColor.getText();
        min0 = window.textFieldMin0.getText();
        max0 = window.textFieldMax0.getText();
        steps0 = window.textFieldStep0.getText();
        min1 = window.textFieldMin1.getText();
        max1 = window.textFieldMax1.getText();
        steps1 = window.textFieldStep1.getText(); 
        transformation = window.textFieldTransformation.getText();
        setDrawType(drawTypeArray[window.listDrawType.getSelectedIndex()]);
        setGraphVisible(window.checkBoxVisible.isSelected());
        updateOperations();
    }
}
