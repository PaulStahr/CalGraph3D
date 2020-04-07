package data;

import java.awt.AWTEvent;
import java.awt.EventQueue;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import util.ArrayTools;

public class GlobalEventListener extends EventQueue{
	private static final Logger logger = LoggerFactory.getLogger(GlobalEventListener.class);
	private static KeyListener kls[] = new KeyListener[0];
	private static MouseListener mls[] = new MouseListener[0];
	static
	{
		Toolkit.getDefaultToolkit().getSystemEventQueue().push(new GlobalEventListener()); 
	}
	
	public static void addGlobalMouseListener(MouseListener ml){
    	mls = ArrayTools.push_back(mls, ml);
    }
    
    public static void removeGlobalMouseListener(MouseListener ml){
    	mls = ArrayTools.delete(mls, ml);
    }
    
    public static void addGlobalKeyListener(KeyListener kl){
    	kls = ArrayTools.delete(kls, kl);
    }
    
    public static void removeGlobalKeyListener(KeyListener kl){
    	kls = ArrayTools.delete(kls, kl);
    }
	
	 @Override
		protected void dispatchEvent(AWTEvent event) {
         final String paramString = event.paramString();
         if (event instanceof MouseEvent){
     		MouseEvent me = (MouseEvent)event;
     		if (paramString.contains("MOUSE_PRESSED")){
                	for (int i=0;i<mls.length;i++){
             		try{
             			mls[i].mousePressed(me);
             		}catch(Exception e){
                     	logger.error("Exception at dispatching Event:", e);        
              		}
             	}
     		}
        	}
         if (event instanceof KeyEvent) {
             KeyEvent ke = (KeyEvent) event;
             if (paramString.contains("KEY_PRESSED,")) {
             	for (int i=0;i<kls.length;i++){
             		try{
             			kls[i].keyPressed(ke);
             		}catch(Exception e){
                     	logger.error("Exception at dispatching Event:", e);                        			
             		}
             	}
             }else if (paramString.contains("KEY_RELEASED,")){
             	for (int i=0;i<kls.length;i++){
             		try{
             			kls[i].keyReleased(ke);
             		}catch(Exception e){
                     	logger.error("Exception at dispatching Event:", e);                        			
             		}
             	}
             }else if (paramString.contains("KEY_TYPED,")){
             	for (int i=0;i<kls.length;i++){
             		try{
             			kls[i].keyTyped(ke);
             		}catch(Exception e){
                     	logger.error("Exception at dispatching Event:", e);                        			
             		}
             	}
             }
         }
         try{
         	super.dispatchEvent(event);
         }catch(ClassCastException e){
         	logger.error("Exception at dispatching Event:", e);
		}
     }
}
