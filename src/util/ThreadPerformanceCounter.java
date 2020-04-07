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
package util;

import java.lang.ref.WeakReference;
import java.util.Arrays;

import util.keyfunction.KeyFunctionLong;

public final class ThreadPerformanceCounter {
	private Thread thread;
	private boolean run;
	private ThreadCounter threadCounter[] = new ThreadCounter[1];
	private int length = 0;
	private final ThreadGroup threadGroup;
	private final int sleepTime = 10;
	private final Runnable runnable = new Runnable(){
		
		@Override
		public void run() {
			long time = System.nanoTime();
			Thread threads[] = new Thread[8];
			int activeCount = 0;
			while (run){
				final int oldCount = activeCount;
				while ((activeCount = threadGroup.enumerate(threads, true))>=threads.length)
					threads = new Thread[threads.length*2];
				if (activeCount < oldCount)
					Arrays.fill(threads, activeCount, oldCount, null);
				
				for (int i=0;i<activeCount;i++){
					final Thread th = threads[i];
					final long id = th.getId();
					final int index = getIndex(id);
					ThreadCounter thc;
					if (index >= 0)
						thc = threadCounter[index];
					else
						insertThreadCounter(thc = new ThreadCounter(th), -1-index);
					thc.putValue(th.getState() == Thread.State.RUNNABLE);
					thc.marked = true;
				}
				
				for (int i=0;i<length;i++)
					if (threadCounter[i].marked)
						threadCounter[i].marked = false;
					else
						removeThreadCounter(threadCounter[i--]);

				try{
					Thread.sleep(Math.max(((time += sleepTime * 1000000) - System.nanoTime()) / 1000000,0));
				}catch(InterruptedException e){}
			}
			thread = null;
		}
	};
	
	private final void insertThreadCounter(ThreadCounter thc, int index){
		if (threadCounter.length == length)
			threadCounter = Arrays.copyOf(threadCounter, threadCounter.length*2);
		System.arraycopy(threadCounter, index, threadCounter, index+1, (length++)-index);
		threadCounter[index] = thc;
	}
	
	private final void removeThreadCounter(ThreadCounter thc){
		int index = getIndex(thc.id);
		if (index < 0)
			return;
		System.arraycopy(threadCounter, index+1, threadCounter, index, --length-index);
		threadCounter[length] = null;
	}
	
	private static final KeyFunctionLong<ThreadCounter> keyFunction = new KeyFunctionLong<ThreadCounter>() {
		@Override
		public final long getKey(ThreadCounter threadCounter) {
			return threadCounter.id;
		}
	};
	

	
	private final int getIndex(long id){
		return ListTools.binarySearch(threadCounter, 0, length, id, keyFunction);
	}
	
	public ThreadPerformanceCounter(ThreadGroup threadGroup){
		this.threadGroup = threadGroup;
	}
	
	public ThreadPerformanceCounter(){
		ThreadGroup parent = Thread.currentThread().getThreadGroup();
		ThreadGroup tmp;
		while ((tmp = parent.getParent())!= null)
			parent = tmp;
		threadGroup = parent;
	}
	
	public final String[] getSummary(){
		String erg[] = new String[length];
		StringBuilder strB = new StringBuilder();
		for (int i=0;i<erg.length;i++){
			strB.setLength(0);
			erg[i] = strB.append(threadCounter[i].getName()).append('(').append(threadCounter[i].id).append(')').append(':').append(threadCounter[i].getRunningTimeInPercent()).append('%').toString();
		}
		return erg;
	}
	
	public final StringBuilder fillWithSummary(StringBuilder strBuilder){
		for (int i=0;i<length;i++)
			strBuilder.append(threadCounter[i].getName()).append('(').append(threadCounter[i].id).append(')').append(':').append(threadCounter[i].getRunningTimeInPercent()).append('%').append('\n');		
		return strBuilder;		
	}
	
	public final synchronized void start(){
		if (thread != null)
			return;
		run = true;
		thread = new Thread(runnable, "Thread performance Counter");
		thread.setPriority(Thread.MAX_PRIORITY);
		thread.start();
	}
	
	public final synchronized void stop(){
		run = false;
	}
	
	private static final class BooleanQueue{
		private final boolean data[];
		private int putPos = 0;
		private int readPos = 0;
		
		private BooleanQueue(int size){
			data = new boolean[size + 1];
		}
		
		public final void put(boolean b){
			if (size() == data.length -1)
				throw new ArrayIndexOutOfBoundsException();
			data[putPos] = b;
			if (++putPos == data.length)
				putPos = 0;
		}
		
		public final boolean pop(){
			if (readPos == putPos)
				throw new NullPointerException();
			final boolean val = data[readPos];
			if (++readPos == data.length)
				readPos = 0;
			return val;
		}
		
		public int size(){
			final int size = putPos - readPos;
			return size >= 0 ? size : size + data.length;
		}
	}
	
	private static final class ThreadCounter{
		private int trueCount = 0;
		private final long id;
		private BooleanQueue st = new BooleanQueue(100);
		private boolean marked = false;
		private WeakReference<Thread> thread;
		
		private ThreadCounter(Thread thread){
			this.id = thread.getId();
			this.thread = new WeakReference<Thread>(thread);
		}
		
		private final String getName(){
			Thread th = thread.get();
			return th == null ? null : th.getName();
		}
		
		public final void putValue(boolean val){
			if (st.size() == 100 && st.pop())
				trueCount--;
			st.put(val);
			if (val)
				trueCount++;
		}
		
		private final int getRunningTimeInPercent(){
			final int size = st.size();
			if (size == 0)
				return -1;
			return trueCount * 100 / size;
		}
	}
}
