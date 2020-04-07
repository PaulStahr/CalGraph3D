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
package opengl.jogamp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GL2ES1;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLES1;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.util.FPSAnimator;

import java.util.Arrays;

import javax.imageio.ImageIO;

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import data.Options;
import data.Options.OptionTreeInnerNode;
import debug.DebugText;
import geometry.Matrix3f;
import geometry.Rotation3;
import geometry.Vector3f;
import jcomponents.util.StandartIOFileFilter;
import opengl.Camera;
import opengl.FramesPerSecondCounter;
import opengl.GlColor;
import opengl.GlTextureHandler;
import opengl.GlTextureHandler.GlTexture;
import opengl.font.GlTextRenderer;
import opengl.jogamp.font.JoglBufferedDynamicSingleFrameFont;
import opengl.ImageLoader;
import opengl.ImageWriter;
import opengl.Light.Type;
import opengl.LightGroup;
import opengl.OpenGlInterface;
import opengl.rendering_algorithm.ObjectRenderer;
import scene.OpenGlMouseHandler;
import scene.Scene;
import scene.SceneObserver;
import scene.SceneUtil;
import scene.SimpleCameraListener;
import scene.object.SceneObject;
import scene.object.SceneObject.UpdateKind;
import util.ArrayTools;
import util.ThreadPerformanceCounter;
/** 
* @author  Paul Stahr
* @version 04.02.2012
*/
public final class JoglCanvas extends GLCanvas implements OpenGlInterface, HierarchyListener
{
	private static final long serialVersionUID = 6711177487257844206L;
    public static final int QUALITY_FASTEST = GL2ES1.GL_FASTEST, QUALITY_DONT_CARE = GL2ES1.GL_DONT_CARE, QUALITY_NICEST = GL2ES1.GL_NICEST;
    private static final float MULT_TO_FCOL = 1f/255f;
	private static final Logger logger = LoggerFactory.getLogger(JoglCanvas.class);
    private byte environmentType = Scene.LIGHTS;
    private final FramesPerSecondCounter fpsc = new FramesPerSecondCounter();
    public final LightGroup lights = new LightGroup(8);
    private float[] gridLinesColor = new float[4];
    private Runnable logicRunnables[] = new Runnable[0];
    
    public final Camera camera = new Camera();
    private ObjectRenderer objectRenderer;
    private BufferedImage screenshot = null;
    private GlTexture cubemap = null;
    private GlTextRenderer font;
    private int repaint = 3;
    private int counter = -1;
    private byte mode = Scene.MODE_MAX_SPEED;
    private boolean stereoSkopieActivated, showFramerate, makeScreenshot;
    private final Matrix3f mat = new Matrix3f();
    private final StringBuilder stringBuilder = new StringBuilder();
    private final float vertices[] = new float[12];

    private SceneObserver scene;
    private final ThreadPerformanceCounter perfC = new ThreadPerformanceCounter();
    private final float coordCrossPositions[] = new float[6];
    private SceneObject coordinateCross;
    private GlTextureHandler textureHandler;
	private int renderedFrameCount;
	private int lastCameraUpdate;
	private float stereoSkopieStrength;
	private float stereoSkopieMoved;
	private JoglKeyHandler keyHandler = new JoglKeyHandler(this);
	private OpenGlMouseHandler mouseHandler = new JoglMouseHandler(this);
	private FPSAnimator animator;
        
    public JoglCanvas(GLCapabilities glcapabilities){
    	this(null, glcapabilities);
    }
    
    public static final JoglCanvas getInstance(Scene scene)
    {
    	try{
	    	//GLProfile glprofile = GLProfile.getDefault();
			GLProfile profile = GLProfile.getDefault();
	    	GLCapabilities capabilities = new GLCapabilities(profile);
	    	capabilities.setSampleBuffers(true);
	    	capabilities.setNumSamples(4);
	    	return new JoglCanvas(scene, capabilities);
    	}catch(Error e)
    	{
    		e.printStackTrace();
    		return null;
    	}
    }
    
    private final GLEventListener eventListener = new GLEventListener() {
        
        @Override
        public void reshape( GLAutoDrawable glautodrawable, int x, int y, int width, int height ) {
            camera.setSize(getBounds());
            GL2 gl = glautodrawable.getGL().getGL2();
            gl.glViewport(0, 0, camera.getWidth(), camera.getHeight());
            repaint(2);

            //OneTriangle.setup( glautodrawable.getGL().getGL2(), width, height );
        }
        
        @Override
        public void init( GLAutoDrawable glautodrawable ) {
        	GL2 gl = glautodrawable.getGL().getGL2();
        	animator.start();
			initGl(gl);
        }
        
        @Override
        public void dispose( GLAutoDrawable glautodrawable ) {
        	logger.info("Exiting gl");
        	if (cubemap != null)
			{
				cubemap.finalize();
			}
			font.finalize();
			objectRenderer.finalize();
        	perfC.stop();
        	scene.reset();
        	counter = -1;
			textureHandler.poll();
			animator.stop();
        }
        
        @Override
        public void display( GLAutoDrawable glautodrawable ) {
        	GL2 gl = glautodrawable.getGL().getGL2();
			++renderedFrameCount;
			controlGl(gl);
			logicGl(gl);
			scene.poll();
			if (mode == Scene.MODE_MAX_SPEED || mode == Scene.MODE_SYNCHRONIZE || repaint>0 || scene.sceneUpdated()){
				if (repaint > 0)
				{
					repaint--;
				}
				drawGl(gl);
			}
        }
    };
    
    public JoglCanvas(Scene sc, GLCapabilities glcapabilities){
    	super(glcapabilities);
    	animator = new FPSAnimator(60);
    	animator.add(this);
    	this.scene = new SceneObserver(sc);
    	addGLEventListener(eventListener);
    	addHierarchyListener(this);
    }

    public final void repaint(){
    	repaint(1);
    }

    private final void repaint(final int times){
    	if (repaint < times)
    		repaint = times;
    }
    
    private final void initGl(final GL2 gl)
    {
        logger.debug("Starting to initialise OpenGl");
        textureHandler = new JoglTextureHandler(gl);
    	
        try{
            final  int samples = Options.getInteger("multisampling", 1);
           	int gotSamples = samples;

            //keyHandler = new KeyHandler(canvas);
            stringBuilder.setLength(0);
            logger.info(
            		stringBuilder.append("OpenGl Version:\"").append(gl.glGetString(GL2.GL_VERSION))
            		.append("\" Graphic Card:\"").append(gl.glGetString(GL2.GL_RENDERER)).append('"').toString());
            if (samples != gotSamples){
            	logger.warn("Tryed to create Display with Multisampling = " + samples+ "X but got only " + gotSamples + 'X');
            	logger.debug("Probably the video driver doesn't support this");
            }
            camera.setSize(getBounds());
            gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
            //gl.glAlphaFunc(GL2.GL_GREATER, 0.01f);
            gl.glLightModeli(GL2.GL_LIGHT_MODEL_LOCAL_VIEWER, GL2ES1.GL_TRUE);
            gl.glClearDepth(1.0f);
            gl.glEnable(GL2.GL_TEXTURE_CUBE_MAP_SEAMLESS);
            //Mouse.create();
            //frequency = gl.getSwa().getFrequency();
            //objectRenderer = new DisplayListRenderer(this, glListHandler);
            JoglBufferObjectHandler gboh = new JoglBufferObjectHandler(gl);
            objectRenderer = new JoglVertexBufferRenderer(gl, gboh);
            coordinateCross = SceneUtil.createCoordinateCross();
            coordinateCross.lightMaterial.set(true, true, true, true, Color.BLACK);
            coordinateCross.reflectionMaterial.set(true, true, true, true, Color.BLACK);
            coordinateCross.setVisible(true);
            scene.getScene().addGlObject(coordinateCross);
            coordinateCross.update(UpdateKind.DATA);
            this.addPerFrameRunnable(new SimpleCameraListener(keyHandler, mouseHandler , scene.getScene(), camera));
            try{
                //font = FontFactory.getInstance(new Fonverticest("CURIER", Font.BOLD, 20), FontFactory.FontType.SINGLETEXTURE, textureHandler);
                font = new JoglBufferedDynamicSingleFrameFont(new Font("CURIER", Font.BOLD, 20), false, textureHandler, gboh, gl);
            } catch (Exception e){
            	logger.error("Can't load glFont",e);
            } 
        }catch (Exception e){
            logger.error("Exception at initalising OpenGl",e);
            return;
        }
        logger.debug("OpenGl is inited");		        	
    }
    
    private final void controlGl(GL2 gl){
        textureHandler.poll();
        objectRenderer.poll();

        if (Options.modCount() != counter){
            counter = Options.modCount();
        	String tmp = Options.getString("quality", "middle");
        	final int quality = tmp.equals("high") ? GL2ES1.GL_NICEST : tmp.equals("middle") ? GL2.GL_DONT_CARE : GL2.GL_FASTEST;
        	if (quality != GL2.GL_FASTEST)
        		gl.glEnable(GL2.GL_DITHER);
        	else
        		gl.glDisable(GL2.GL_DITHER);
            gl.glHint(GL2ES1.GL_GENERATE_MIPMAP_HINT,quality);
            gl.glHint(GL2ES1.GL_FOG_HINT,quality);
            gl.glHint(GL2ES1.GL_LINE_SMOOTH_HINT,quality);
            gl.glHint(GL2ES1.GL_PERSPECTIVE_CORRECTION_HINT,quality);
            gl.glHint(GL2ES1.GL_POINT_SMOOTH_HINT,quality);
            gl.glHint(GL2.GL_POLYGON_SMOOTH_HINT,quality);
            gl.glHint(GL2.GL_TEXTURE_COMPRESSION_HINT,quality);
            Options.OptionTreeInnerNode sceneNode = Options.getInnerNode("cubemap");
            tmp = Options.getString(sceneNode, "cubemap", null);
            if (tmp != null && scene != null && !tmp.equals(scene.getScene().cupemap)){
            	try{
                    final String paths[] = new String[6];
                    
                    int count = 0;
                    File folder = new File (scene.getScene().cupemap = tmp);
                    if (!folder.isDirectory())
                    {
                    	throw new IOException('"' + tmp + "\" is not a folder");
                    }
                    for (final File file : folder.listFiles(new StandartIOFileFilter(ImageIO.getReaderFormatNames(), false)))
                    {
                        paths[count++] = file.getPath();
                    }
                    Arrays.sort(paths);
                    ImageLoader imgLoader = new ImageLoader();
                    cubemap = JoglTextureUtil.getTexture(
                    		paths,
                    		GL2.GL_CLAMP_TO_EDGE,
                    		GL2.GL_CLAMP_TO_EDGE,
                    		GL2.GL_LINEAR_MIPMAP_NEAREST,
                    		GL2.GL_LINEAR,
                    		GL2.GL_MODULATE,
                    		imgLoader,
                    		ImageLoader.TargetFormat.R8G8B8,
                    		GL2.GL_RGB,
                    		textureHandler,
                    		gl);              
                }catch (Exception e){
                	logger.error("CubeMap konnten nicht geladen werden", e);
                    cubemap = null;
                }
            }
            OptionTreeInnerNode stereoskopie = Options.getInnerNode("stereoskopie");
            stereoSkopieStrength = Options.getFloat(stereoskopie, "strength", 0f);
            stereoSkopieMoved = Options.getFloat(stereoskopie, "moved", 0f);
            stereoSkopieActivated = Options.getBoolean(stereoskopie, "activated", false);
            if (showFramerate = Options.getBoolean("show_framerate", false))
            	perfC.start();
            else
            	perfC.stop();
            coordinateCross.setVisible(Options.getBoolean("show_grid_lines", false));
            mode = Options.getByte("energy_mode", Scene.MODE_SYNCHRONIZE);
            OptionTreeInnerNode environment = Options.getInnerNode(sceneNode, "environment");
            environmentType = Options.getByte(environment, "type", Scene.LIGHTS);
            if (cubemap == null && (environmentType == Scene.NORMAL_MAP || environmentType == Scene.REFLECTION_MAP)){
            	environmentType = Scene.LIGHTS;
            	logger.error("Can't use reflection- or normalmap without a cubemap");
            }
            final boolean useLights = !(environmentType == Scene.NORMAL_MAP || environmentType == Scene.REFLECTION_MAP); 
            gl.glLightModeli(GL2ES1.GL_LIGHT_MODEL_TWO_SIDE, useLights ? GL2ES1.GL_TRUE : GL2ES1.GL_FALSE);
            lights.useLights(useLights);
            lights.loadFromOptions();
            JoglGlUtils.setLights(lights, gl);
            int c = Options.getColor("background", Color.WHITE).getRGB();
            gl.glClearColor(((c>>16)&0xFF)*MULT_TO_FCOL, ((c>>8)&0xFF)*MULT_TO_FCOL, (c&0xFF)*MULT_TO_FCOL, 0);
            GlColor.fill(gridLinesColor, Options.getColor("grid_lines_color", Color.BLACK));
            coordinateCross.reflectionMaterial.set(Type.EMISSION, Options.getColor("grid_lines_color", Color.BLACK));
            repaint();
        }
        if (makeScreenshot)
        {
        	try {
                takeScreenshotGl(gl);        		
        	}catch(Exception e)
        	{
        		logger.error("Error", e);
        	}
        }
   }

    public synchronized final BufferedImage getScreenshot(){
        synchronized(mat){
            makeScreenshot = true;
            repaint();
        	try {
        		mat.wait(10000);
			} catch (InterruptedException e) {}
        }
        return screenshot;
    }

    private final void takeScreenshotGl(GL2 gl){
    	System.out.println(Thread.currentThread().getName());
        final int width = camera.getWidth(), height = camera.getHeight();
        final ByteBuffer bb = ByteBuffer.allocateDirect(width * height*4);    
        gl.glReadPixels(0, 0, width, height, GL2ES1.GL_RGBA, GL2ES1.GL_UNSIGNED_BYTE, bb);
        screenshot = ImageWriter.getImage(bb, width, height);
        makeScreenshot = false;
        synchronized (mat) {
        	mat.notifyAll();
		}
    }
    
    @Override
    public void paint(Graphics graphics)
    {
    	synchronized(mat){
        	makeScreenshot = true;
            repaint();
            if (EventQueue.isDispatchThread())
            {
            	super.paint(graphics);
            }
            else
            {
            	try {
            		mat.wait(10000);
            	} catch (InterruptedException e) {}
            }
        }
    	graphics.drawImage(screenshot, 0, 0, null);
    }

    /**
     * Do all calculations, handle input, etc.
     * @param gl 
     */
    private final void logicGl(GL2 gl){
        //keyHandler.doIO();
        for (int i = 0; i < logicRunnables.length; ++i)
        {
        	logicRunnables[i].run();
        }
        scene.getScene().getCameraPosition(camera.position, camera.rotation);
    	lastCameraUpdate = renderedFrameCount;
  
        mat.diagonal(30);
        mat.rotateZYXEuler(camera.rotation);
        mat.fillWithColumns2d(coordCrossPositions);
    }
    
    public int renderedFrameCount(){
    	return renderedFrameCount;
    }
    
    public int lastCameraUpdate(){
    	return lastCameraUpdate;
    }
    
    public void drawGl(GL2 gl)
    {
    	gl.glMatrixMode(GL2.GL_PROJECTION);
    	gl.glLoadIdentity();        
       	JoglGlUtils.perspectiveGL(camera.getAngle(),camera.getAspect(),0.01,10000.0, gl);
       	gl.glMatrixMode(GL2.GL_MODELVIEW);
		if (stereoSkopieActivated){
			gl.glColorMask(true, false, false,true);
			camera.stereoPosition = Camera.STEREOSCOPIE_RIGHT;
			drawScene(gl);
			gl.glClear(GL2.GL_DEPTH_BUFFER_BIT);
			gl.glColorMask(false, true, true, true);
			camera.stereoPosition = Camera.STEREOSCOPIE_LEFT;
		}else{
			camera.stereoPosition = Camera.STEREOSCOPIE_MIDDLE;
			gl.glColorMask(true, true, true, true);						
		}
		drawScene(gl);
		gl.glColorMask(true, true, true, true);
		gl.glMatrixMode(GL2.GL_PROJECTION);
		gl.glLoadIdentity();
		gl.glOrtho(0, camera.getWidth(), 0, camera.getHeight(), -10, 10);
		gl.glMatrixMode(GL2.GL_MODELVIEW);
		gl.glLoadIdentity();
        
        gl.glDisable(GL2.GL_LIGHTING);
        gl.glDisable(GL2.GL_DEPTH_TEST);
        gl.glEnable(GL2.GL_BLEND);

        final int cordCrossMiddleX = camera.getWidth() - 45, cordCrossMiddleY = camera.getHeight() - 45;
        if (coordinateCross.isVisible()){
        	gl.glColor4f(gridLinesColor[0], gridLinesColor[1], gridLinesColor[2], gridLinesColor[3]);
 
        	for (int i=0;i<6;i+=2){
        		vertices[i * 2 + 0] = cordCrossMiddleX;
        		vertices[i * 2 + 1] = cordCrossMiddleY;
        		vertices[i * 2 + 2] = coordCrossPositions[i]+cordCrossMiddleX;
	        	vertices[i * 2 + 3] = coordCrossPositions[i + 1]+cordCrossMiddleY;
	        }
            objectRenderer.drawLines2d(vertices);
        }
        fpsc.poll();
        if (font != null){
        	font.setColor(Color.BLACK);
        	font.beginRendering();
        	if (coordinateCross.isVisible())
        	{
            	stringBuilder.setLength(1);
                for (int i=0;i<3;i++)
                {
                	stringBuilder.setCharAt(0, (char)('x' + i));
                   	font.draw(stringBuilder, vertices[i * 4 + 2] + 2, vertices[i * 4 + 3] + 3, GlTextRenderer.LEFT, GlTextRenderer.TOP);        	
                }
        	}
            if (showFramerate){
            	stringBuilder.setLength(0);
                DebugText.getDebugText(stringBuilder, scene);
                stringBuilder.append("FPS:").append(fpsc.getFPS()).append('\n').append("Pos: ");
                camera.position.toString(stringBuilder).append(" Rot: ");
                camera.rotation.toString(stringBuilder).append('\n');
                font.setFontSize(18);
                font.draw(perfC.fillWithSummary(stringBuilder) ,0, camera.getHeight(), GlTextRenderer.LEFT, GlTextRenderer.BOTTOM);
        	}
        	font.endRendering();
        }       
        gl.glDisable(GL2ES1.GL_BLEND);
    }
    
    public static final void setLocation(byte stereoskopiePos, float stereoSkopieRotation, float stereoSkopieMoved, Vector3f position, Rotation3 rotation, GL2 gl){
        gl.glLoadIdentity();
        if (stereoskopiePos != Camera.STEREOSCOPIE_MIDDLE){
            gl.glTranslatef (stereoskopiePos * stereoSkopieRotation, 0.0f, 0.0f);
            gl.glRotatef(stereoskopiePos * stereoSkopieMoved,0.0f,1.0f,0.0f);
        }
        JoglGlUtils.rotateReverseEulerXYZ(rotation, gl);

        gl.glTranslatef(-position.x ,-position.y,-position.z);
    }
    
    /**
     * Render the current frame
     */
    private final void drawScene(GL2 gl){    
    	gl.glClear(GL2ES1.GL_COLOR_BUFFER_BIT | GL2ES1.GL_DEPTH_BUFFER_BIT);

    	setLocation(camera.stereoPosition, stereoSkopieStrength, stereoSkopieMoved, camera.position, camera.rotation, gl);
        JoglGlUtils.setEnabledGlPositions(lights, gl);
        gl.glEnable(GL2.GL_DEPTH_TEST);
        gl.glEnable(GL2.GL_LIGHTING);
        gl.glPointSize(2.0f);
        gl.glLineWidth(1.0f);

        if (environmentType == Scene.REFLECTION_MAP || environmentType == Scene.NORMAL_MAP){
        	gl.glMatrixMode(GL2ES1.GL_TEXTURE);
            gl.glLoadIdentity();
            JoglGlUtils.rotateEulerZYX(camera.rotation, gl);
            gl.glMatrixMode(GL2ES1.GL_MODELVIEW);

            gl.glTexEnvf(GL2ES1.GL_TEXTURE_ENV,GL2ES1. GL_TEXTURE_ENV_MODE, GL2ES1.GL_MODULATE);
            cubemap.bind();

            gl.glEnable(GL2.GL_TEXTURE_GEN_S);
            gl.glEnable(GL2.GL_TEXTURE_GEN_T);
            gl.glEnable(GL2.GL_TEXTURE_GEN_R);
            gl.glEnable(GL2.GL_TEXTURE_CUBE_MAP);

            JoglGlUtils.setMaterialColor(true, true, true, false, GlColor.COLOR_BLACK, gl);
            JoglGlUtils.setMaterialColor(false, false, false, true, GlColor.COLOR_WHITE, gl);
            gl.glMaterialf(GL2.GL_FRONT_AND_BACK, GL2.GL_SHININESS, 0.0f);

            gl.glTexGeni(GL2.GL_S, GL2.GL_TEXTURE_GEN_MODE, GLES1.GL_NORMAL_MAP);
            gl.glTexGeni(GL2.GL_T, GL2.GL_TEXTURE_GEN_MODE, GLES1.GL_NORMAL_MAP);
            gl.glTexGeni(GL2.GL_R, GL2.GL_TEXTURE_GEN_MODE, GLES1.GL_NORMAL_MAP);
            
            gl.glDepthFunc(GL2.GL_ALWAYS);
            gl.glDepthMask(false);
            gl.glPushMatrix();
            gl.glLoadIdentity();
            objectRenderer.renderQuad();
            gl.glPopMatrix();
            
            final int environment_mode = environmentType == Scene.REFLECTION_MAP ? GLES1.GL_REFLECTION_MAP : GLES1.GL_NORMAL_MAP;
            gl.glTexGeni(GL2.GL_S, GL2.GL_TEXTURE_GEN_MODE, environment_mode);
            gl.glTexGeni(GL2.GL_T, GL2.GL_TEXTURE_GEN_MODE, environment_mode);
            gl.glTexGeni(GL2.GL_R, GL2.GL_TEXTURE_GEN_MODE, environment_mode);
        }
        gl.glDepthMask(true);
        gl.glDepthFunc(GL2.GL_LEQUAL);
        JoglGlUtils.renderGlObjects(scene, true, objectRenderer, environmentType, gl);
        
        if (environmentType == Scene.REFLECTION_MAP || environmentType == Scene.NORMAL_MAP){
        	gl.glDisable(GL2.GL_TEXTURE_GEN_S);
        	gl.glDisable(GL2.GL_TEXTURE_GEN_T);
        	gl.glDisable(GL2.GL_TEXTURE_GEN_R);

        	gl.glDisable(GL2.GL_TEXTURE_CUBE_MAP);

        	gl.glMatrixMode(GL2.GL_TEXTURE);
        	gl.glLoadIdentity();
            gl.glMatrixMode(GL2.GL_MODELVIEW);
        }

        JoglGlUtils.renderGlObjects(scene, false, objectRenderer, environmentType, gl);
    }
    
    public void setScene(Scene scene)
    {
    	this.scene = new SceneObserver(scene);
    }

	@Override
	public void addPerFrameRunnable(Runnable runnable) {
		logicRunnables = ArrayTools.push_back(logicRunnables, runnable);
	}

	@Override
	public void removePerFrameRunnable(Runnable runnable) {
		logicRunnables = ArrayTools.deleteElem(logicRunnables, runnable);
	}

	@Override
	public void dispose() {
		destroy();
	}

	@Override
	public void hierarchyChanged(HierarchyEvent e) {
		if ((e.getChangeFlags() & HierarchyEvent.DISPLAYABILITY_CHANGED)!=0 && !isDisplayable())
		{
			destroy();
		}
	}
}
