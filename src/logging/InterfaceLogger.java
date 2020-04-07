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
package logging;

import java.util.ArrayList;
import java.util.List;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

public class InterfaceLogger<E> extends AppenderBase<ILoggingEvent>{
	private static final List<ILoggingEvent> logLines = new ArrayList<ILoggingEvent>();
    public static final List<LoggingListener> loggingListener = new ArrayList<LoggingListener>();

    public static void addLoggingListener(LoggingListener ll){
    	synchronized(loggingListener){
    		loggingListener.add(ll);
    	}
    	for (int i=0;i<logLines.size();i++){
    		updateLog(ll,logLines.get(i));
    	}
    }
    
    public static void removeLoggingListener(LoggingListener ll){
    	synchronized(loggingListener){
    		loggingListener.remove(ll);
    	}
    }
    
	@Override
	protected void append(ILoggingEvent eventObject) {
		logLines.add(eventObject);
		updateLogs(eventObject);
	}
	
	private static void updateLog(LoggingListener ll, ILoggingEvent eventObject){
		try{
			ll.append(eventObject);
		}catch(Exception e){}
	}
	
	private static void updateLogs(ILoggingEvent eventObject){
    	for (int i=0;i<loggingListener.size();i++){
    		updateLog(loggingListener.get(i), eventObject);
    	}
 	}
	
	public static interface LoggingListener {
		public void append(ILoggingEvent ile);
	}

}
