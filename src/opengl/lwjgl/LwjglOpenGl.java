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
package opengl.lwjgl;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import javax.imageio.ImageIO;
import javax.swing.JOptionPane;

import org.lwjgl.Version;
import org.lwjgl.opengl.ARBTextureCubeMap;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL32;
import org.lwjgl.opengl.awt.AWTGLCanvas;
import org.lwjgl.opengl.awt.GLData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import data.Options;
import data.Options.OptionTreeInnerNode;
import debug.DebugText;
import geometry.Matrix3f;
import jcomponents.util.StandartIOFileFilter;
import opengl.Camera;
import opengl.FramesPerSecondCounter;
import opengl.GlColor;
import opengl.GlTextureHandler;
import opengl.GlTextureHandler.GlTexture;
import opengl.ImageLoader;
import opengl.ImageWriter;
import opengl.Light.Type;
import opengl.LightGroup;
import opengl.OpenGlInterface;
import opengl.font.GlTextRenderer;
import opengl.lwjgl.font.LwjglBufferedDynamicSingleFrameFont;
import opengl.rendering_algorithm.ObjectRenderer;
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
public final class LwjglOpenGl extends AWTGLCanvas implements OpenGlInterface, ComponentListener
{
	private static final long serialVersionUID = 3701098899415853243L;
    public static final int QUALITY_FASTEST = GL11.GL_FASTEST, QUALITY_DONT_CARE = GL11.GL_DONT_CARE, QUALITY_NICEST = GL11.GL_NICEST;
    private static final float MULT_TO_FCOL = 1f/255f;
	private static final Logger logger = LoggerFactory.getLogger(LwjglOpenGl.class);
    private byte environmentType = Scene.LIGHTS;
    private final FramesPerSecondCounter fpsc = new FramesPerSecondCounter();
    public final LightGroup lights = new LightGroup(8);
    private float[] gridLinesColor = new float[4];
    private Runnable[] logicRunnables = new Runnable[0];

    public final Camera camera = new Camera();
    private ObjectRenderer objectRenderer;
    private BufferedImage screenshot = null;
    private GlTexture cubemap = null;
    private GlTextRenderer font;
    private int repaint = 3;
    private boolean run, exit, resize;
    private boolean inited;
    private int counter = -1;
    private byte mode = Scene.MODE_MAX_SPEED;
    private int frequency = 60;
    private static boolean stereoSkopieActivated;
	private boolean showFramerate;
	private boolean makeScreenshot;
    private Thread thread;
    private final Matrix3f mat = new Matrix3f();
    private final StringBuilder stringBuilder = new StringBuilder();
    private LwjglKeyHandler keyHandler;
    private LwjglMouseHandler mouseHandler;
    private final float vertices[] = new float[12];
    private GlTextureHandler textureHandler = new LwjglTextureHandler();
	protected static float stereoSkopieStrength;
	protected static float stereoSkopieMoved;

    private final SceneObserver scene;
    private final ThreadPerformanceCounter perfC = new ThreadPerformanceCounter();
    private final float coordCrossPositions[] = new float[6];
    private SceneObject coordinateCross;
    private final Runnable runnable = new Runnable(){
    	@Override
		public void run(){
    	    System.out.println("run");
    	    run = true;
    		while (!isDisplayable())
    		{
    			synchronized (thread) {
    				try {
    					thread.wait(100);
    				} catch (InterruptedException e) {
    					logger.error("Unexpected Interrupt", e);
    				}
				}

    		}
    		LwjglOpenGl.this.render();
			if (!inited){
	        	logger.error("Can't init OpenGl");
	            JOptionPane.showConfirmDialog(null, "Fehler", "Kein OpenGl verf\u00FCgbar." , JOptionPane.DEFAULT_OPTION);
			}
			else
			{
	    		while (!exit)
	    		{
	    			//Display.processMessages();
	    			if (run)
	    			{
	    				try
	    				{
							++renderedFrameCount;
							controlGl();
							logicGl();
							scene.poll();
							if (mode == Scene.MODE_MAX_SPEED || mode == Scene.MODE_SYNCHRONIZE || repaint>0 || scene.sceneUpdated()){
								if (repaint > 0)
								{
									repaint--;
								}
				                LwjglOpenGl.this.render();
								//Display.update();
							}
							else
							{
				   				//Display.processMessages();
							}
				   			//if (mode == Scene.MODE_SYNCHRONIZE || mode == Scene.MODE_MIN_ENERGY)
								//Display.sync(frequency);
						}catch (Exception e){
							logger.error("Error in OpenGl loop: ", e);
						}
		    		}
	    		}
			}
			if (cubemap != null)
			{
				cubemap.finalize();
			}
			font.finalize();
			objectRenderer.finalize();
			perfC.stop();
			textureHandler.poll();
    		//Display.destroy();
    		thread = null;
    		synchronized(mat)
    		{
    			mat.notifyAll();
    		}
    	}
    };

    @Override
	public void componentHidden(ComponentEvent e){}

    @Override
	public void componentShown(ComponentEvent e){
    	if (!inited)
    	{
    		init();
    	}
    	repaint();
    }

    @Override
	public void componentMoved(ComponentEvent e){
    	repaint();
    }

    @Override
	public void componentResized(ComponentEvent e){
        resize = true;
    }
    private int renderedFrameCount;
	private int lastCameraUpdate;

    public LwjglOpenGl(){this(null);}

    private static GLData getGlData()
    {
        GLData data = new GLData();
        data.majorVersion = 3;
        data.minorVersion = 3;
        data.profile = GLData.Profile.COMPATIBILITY;
        data.api = GLData.API.GL;
        data.samples = 4;
        return data;
    }

    public LwjglOpenGl(Scene scene){
        super(getGlData());
        effective.profile = GLData.Profile.COMPATIBILITY;
        effective.majorVersion = 3;
    	this.scene = new SceneObserver(scene);
    	/*for (int i = 0; i < vertices.length; ++i)
    	{
    		if (i != 2 && i != 4)
    		{
    			vertices[i] = new Vector2f();
    		}
		}
    	vertices[4] = vertices[2] = vertices[0];*/

    	if (!inited)
    	{
    		init();
    	}
    }

    @Override
    public void dispose()
    {
    	exit=true;
    	while(thread != null)
    	{
	    	try {
	    		synchronized(mat)
	    		{
	    			mat.wait(1000);
	    		}
	    	} catch (InterruptedException e) {
				logger.error("Dispose Lock Interrupted",e);
			}
    	}
    }

    @Override
    public final void repaint(){
    	super.repaint();
    	repaint(1);
    }

    private final void repaint(final int times){
    	if (repaint < times)
    		repaint = times;
    }

    public final synchronized void start(){
    	run = true;
    	synchronized (thread) {
			thread.notify();
		}
    }

    public final synchronized void stop(){
    	run = false;
    	synchronized (thread) {
			thread.notify();
		}
    }

    public final synchronized void exit(){
    	exit = true;
    	synchronized (thread) {
			thread.notify();
		}
    }

    public final synchronized void init(){
    	if (thread != null)
    		return;
    	(thread = new Thread(runnable, "GL-Thread")).start();
    }

    @Override
    public final void initGL(){
    	addComponentListener(this);
        logger.debug("Initialising OpenGl");
        try{
            /*final  int samples = Options.getInteger("multisampling", 1);
           	int gotSamples = samples;
           	while (true){
                try{
                	if (gotSamples == 0)
            	   		GLFW.glfwCreateWindow(width, height, title, monitor, share);
                	else
                		Display.create(new PixelFormat(0, 24, 0, gotSamples));
                	break;
                }catch (Exception e){
                	if (gotSamples == 0){
            	   		logger.error("Can't create Display", e);
            	   		break;
                	}
                	gotSamples /= 2;
                }
            }*/
            keyHandler = new LwjglKeyHandler(this);
            mouseHandler = new LwjglMouseHandler(this);
            stringBuilder.setLength(0);
            GL.createCapabilities();
            logger.info(
            		stringBuilder.append("OpenGl Version:\"").append(effective.majorVersion).append('.').append(effective.minorVersion).append(" (Profile: ").append(effective.profile).append(')')
            					 .append("\" Graphic Card:\"").append(GL11.glGetString(GL11.GL_RENDERER))
            					 .append("\" Lwjgl Version:\"").append(Version.getVersion()).append('"').toString());
            /*if (samples != gotSamples){
            	logger.warn("Tryed to create Display with Multisampling = " + samples+ "X but got only " + gotSamples + 'X');
            	logger.debug("Probably the video driver doesn't support this");
            }*/
            camera.setSize(getBounds());
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            //GL11.glAlphaFunc(GL11.GL_GREATER, 0.01f);
            GL11.glLightModeli(GL11.GL_LIGHT_MODEL_LOCAL_VIEWER, GL11.GL_TRUE);
            GL11.glClearDepth(1.0f);
            GL11.glEnable(GL32.GL_TEXTURE_CUBE_MAP_SEAMLESS);
            //Mouse.create();
            frequency = 60;//Display.getDesktopDisplayMode().getFrequency();
            //objectRenderer = new LwjglDisplayListRenderer();
            LwjglBufferObjectHandler gboh = new LwjglBufferObjectHandler();
            objectRenderer = new LwjglVertexBufferRenderer(gboh);
            coordinateCross = SceneUtil.createCoordinateCross();
            coordinateCross.lightMaterial.set(true, true, true, true, Color.BLACK);
            coordinateCross.reflectionMaterial.set(true, true, true, true, Color.BLACK);
            coordinateCross.setVisible(true);
            scene.getScene().addGlObject(coordinateCross);
            coordinateCross.update(UpdateKind.DATA);
            this.addPerFrameRunnable(new SimpleCameraListener(keyHandler, mouseHandler, scene.getScene(), camera));
            try{
                //font = FontFactory.getInstance(new Font("CURIER", Font.BOLD, 20), FontFactory.FontType.SINGLETEXTURE, textureHandler);
                font = new LwjglBufferedDynamicSingleFrameFont(new Font("CURIER", Font.BOLD, 20), false, textureHandler, gboh);
            } catch (Exception e){
            	logger.error("Can't load glFont",e);
            }
            run = true;
        }catch (Exception e){
            logger.error("Exception at initalising OpenGl",e);
            return;
        }
        inited = true;
        logger.debug("OpenGl is inited");
    }

    private final void controlGl(){
        if (!inited)
        {
            return;
        }
    	if (/*Display.isCloseRequested() || */!isDisplayable())
        	exit = true;
    	textureHandler.poll();
        objectRenderer.poll();

        if (Options.modCount() != counter){
            counter = Options.modCount();
        	String tmp = Options.getString("quality", "middle");
        	final int quality = tmp.equals("high") ? GL11.GL_NICEST : tmp.equals("middle") ? GL11.GL_DONT_CARE : GL11.GL_FASTEST;
        	if (quality != GL11.GL_FASTEST)
        		GL11.glEnable(GL11.GL_DITHER);
        	else
        		GL11.glDisable(GL11.GL_DITHER);
            GL11.glHint(GL14.GL_GENERATE_MIPMAP_HINT,quality);
            GL11.glHint(GL11.GL_FOG_HINT,quality);
            GL11.glHint(GL11.GL_LINE_SMOOTH_HINT,quality);
            GL11.glHint(GL11.GL_PERSPECTIVE_CORRECTION_HINT,quality);
            GL11.glHint(GL11.GL_POINT_SMOOTH_HINT,quality);
            GL11.glHint(GL11.GL_POLYGON_SMOOTH_HINT,quality);
            GL11.glHint(GL13.GL_TEXTURE_COMPRESSION_HINT,quality);
            //Display.setVSyncEnabled(false);
            Options.OptionTreeInnerNode sceneNode = Options.getInnerNode("scene");
            tmp = Options.getString(sceneNode, "cubemap", null);
            if (tmp != null && scene != null && (!tmp.equals(scene.getScene().cupemap) || cubemap == null)){
                try{
                    final String paths[] = new String[6];
                    File folder = new File (scene.getScene().cupemap = tmp);
                    int count = 0;
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
                    cubemap = LwjglTextureUtil.getTexture(
                    		paths,
                    		GL12.GL_CLAMP_TO_EDGE,
                    		GL12.GL_CLAMP_TO_EDGE,
                    		GL11.GL_LINEAR_MIPMAP_NEAREST,
                    		GL11.GL_LINEAR,
                    		GL11.GL_MODULATE,
                    		imgLoader,
                    		ImageLoader.TargetFormat.R8G8B8,
                    		GL11.GL_RGB,
                    		textureHandler);
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
            GL11.glLightModeli(GL11.GL_LIGHT_MODEL_TWO_SIDE, useLights ? GL11.GL_TRUE : GL11.GL_FALSE);
            lights.useLights(useLights);
            lights.loadFromOptions();
            LwjglGlUtils.setLights(lights);
            int c = Options.getColor("background", Color.WHITE).getRGB();
            GL11.glClearColor(((c>>16)&0xFF)*MULT_TO_FCOL, ((c>>8)&0xFF)*MULT_TO_FCOL, (c&0xFF)*MULT_TO_FCOL, 0);
            GlColor.fill(gridLinesColor, Options.getColor("grid_lines_color", Color.BLACK));
            coordinateCross.reflectionMaterial.set(Type.EMISSION, Options.getColor("grid_lines_color", Color.BLACK));
            repaint();
        }
        if (makeScreenshot)
            takeScreenshotGl();
        if (resize){
            camera.setSize(getBounds());
            GL11.glViewport(0, 0, camera.getWidth(), camera.getHeight());
            resize = false;
            repaint(2);
        }
    }

    @Override
    public synchronized final BufferedImage getScreenshot(){
        makeScreenshot = true;
        synchronized(mat){
        	try {
        		mat.wait(10000);
			} catch (InterruptedException e) {}
        }
        return screenshot;
    }

    /*public void paintAll(Graphics g)
    {

    }*/

    private final void takeScreenshotGl(){
        final int width = camera.getWidth(), height = camera.getHeight();
        final ByteBuffer bb = ByteBuffer.allocateDirect(width * height * 4);
        GL11.glReadPixels(0, 0, width, height, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, bb);
        screenshot = ImageWriter.getImage(bb, width, height);
        makeScreenshot = false;
        synchronized (mat) {
        	mat.notifyAll();
		}
    }

    /**
     * Do all calculations, handle input, etc.
     */
    private final void logicGl(){
        keyHandler.doIO();
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

    @Override
    public void paintGL()
    {
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();
       	LwjglGlUtils.perspectiveGL(camera.getAngle(),camera.getAspect(),0.01,10000.0);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
		if (stereoSkopieActivated){
			GL11.glColorMask(true, false, false,true);
			camera.stereoPosition = Camera.STEREOSCOPIE_RIGHT;
			drawScene();
			GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
			GL11.glColorMask(false, true, true, true);
			camera.stereoPosition = Camera.STEREOSCOPIE_LEFT;
		}else{
			camera.stereoPosition = Camera.STEREOSCOPIE_MIDDLE;
		}
		drawScene();
		GL11.glColorMask(true, true, true, true);
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();
        GL11.glOrtho(0, camera.getWidth(), 0, camera.getHeight(), -10, 10);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glLoadIdentity();

        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_BLEND);

        final int cordCrossMiddleX = camera.getWidth() - 45, cordCrossMiddleY = camera.getHeight() - 45;
        if (coordinateCross.isVisible()){
            GL11.glColor4f(gridLinesColor[0], gridLinesColor[1], gridLinesColor[2], gridLinesColor[3]);

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
                perfC.fillWithSummary(stringBuilder);
                font.setFontSize(18);
                font.draw(stringBuilder ,0, camera.getHeight(), GlTextRenderer.LEFT, GlTextRenderer.BOTTOM);
        	}
        	font.endRendering();
        }
        GL11.glDisable(GL11.GL_BLEND);
        swapBuffers();
    }

    /**
     * Render the current frame
     */
    private final void drawScene(){
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);

        LwjglGlUtils.setLocation(camera.stereoPosition, stereoSkopieStrength, stereoSkopieMoved, camera.position, camera.rotation);
        LwjglGlUtils.setEnabledGlPositions(lights);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glPointSize(2.0f);
        GL11.glLineWidth(1.0f);
        LwjglGlUtils.renderGlObjects(scene, false, objectRenderer, environmentType);

        if (environmentType == Scene.REFLECTION_MAP || environmentType == Scene.NORMAL_MAP){
            GL11.glMatrixMode(GL11.GL_TEXTURE);
            GL11.glLoadIdentity();
            LwjglGlUtils.rotateEulerZYX(camera.rotation);
            GL11.glMatrixMode(GL11.GL_MODELVIEW);

            GL11.glTexEnvf(GL11.GL_TEXTURE_ENV,GL11. GL_TEXTURE_ENV_MODE, GL11.GL_MODULATE);
            cubemap.bind();

            GL11.glEnable(GL11.GL_TEXTURE_GEN_S);
            GL11.glEnable(GL11.GL_TEXTURE_GEN_T);
            GL11.glEnable(GL11.GL_TEXTURE_GEN_R);
            GL11.glEnable(ARBTextureCubeMap.GL_TEXTURE_CUBE_MAP_ARB);

            LwjglGlUtils.setMaterialColor(true, true, true, false, GlColor.COLOR_BLACK);
            LwjglGlUtils.setMaterialColor(false, false, false, true, GlColor.COLOR_WHITE);
            GL11.glMaterialf(GL11.GL_FRONT_AND_BACK, GL11.GL_SHININESS, 0.0f);

            GL11.glTexGeni(GL11.GL_S, GL11.GL_TEXTURE_GEN_MODE, ARBTextureCubeMap.GL_NORMAL_MAP_ARB);
            GL11.glTexGeni(GL11.GL_T, GL11.GL_TEXTURE_GEN_MODE, ARBTextureCubeMap.GL_NORMAL_MAP_ARB);
            GL11.glTexGeni(GL11.GL_R, GL11.GL_TEXTURE_GEN_MODE, ARBTextureCubeMap.GL_NORMAL_MAP_ARB);

            GL11.glDepthFunc(GL11.GL_ALWAYS);
            GL11.glDepthMask(false);
            GL11.glPushMatrix();
            GL11.glLoadIdentity();
            objectRenderer.renderQuad();
            GL11.glPopMatrix();

            final int environment_mode = environmentType == Scene.REFLECTION_MAP ? ARBTextureCubeMap.GL_REFLECTION_MAP_ARB : ARBTextureCubeMap.GL_NORMAL_MAP_ARB;
            GL11.glTexGeni(GL11.GL_S, GL11.GL_TEXTURE_GEN_MODE, environment_mode);
            GL11.glTexGeni(GL11.GL_T, GL11.GL_TEXTURE_GEN_MODE, environment_mode);
            GL11.glTexGeni(GL11.GL_R, GL11.GL_TEXTURE_GEN_MODE, environment_mode);
        }
        GL11.glDepthMask(true);
        GL11.glDepthFunc(GL11.GL_LEQUAL);
        LwjglGlUtils.renderGlObjects(scene, true, objectRenderer, environmentType);

        if (environmentType == Scene.REFLECTION_MAP || environmentType == Scene.NORMAL_MAP){
            GL11.glDisable(GL11.GL_TEXTURE_GEN_S);
            GL11.glDisable(GL11.GL_TEXTURE_GEN_T);
            GL11.glDisable(GL11.GL_TEXTURE_GEN_R);

            GL11.glDisable(ARBTextureCubeMap.GL_TEXTURE_CUBE_MAP_ARB);

            GL11.glMatrixMode(GL11.GL_TEXTURE);
            GL11.glLoadIdentity();
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
        }

    }

	@Override
	public void addPerFrameRunnable(Runnable runnable) {
		logicRunnables = ArrayTools.push_back(logicRunnables, runnable);
	}

	@Override
	public void removePerFrameRunnable(Runnable runnable) {
		logicRunnables = ArrayTools.deleteElem(logicRunnables, runnable);
	}
}
