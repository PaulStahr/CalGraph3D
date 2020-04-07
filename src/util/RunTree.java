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

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.ArrayList;

public final class RunTree{
	public static final byte STATE_BUILDING = 0;
	public static final byte STATE_RUNNING = 2;
	public static final byte STATE_FINISHED = 3;
	public static final byte STATE_WAITING = 4;
	private static final RunTreeItem EMPTY_RUNTREE_ITEM_ARRAY[] = new RunTreeItem[0];

	private final ArrayList<RunTreeItem> itemList = new ArrayList<RunTreeItem>();
	private int treeState = STATE_BUILDING;
	private final RunnableRunner runnableRunner;
	
	public RunTree(RunnableRunner runnableRunner){
		this.runnableRunner = runnableRunner;
	}
		
	public final void run(){
		if (treeState != STATE_BUILDING)
			throw new RuntimeException();
		treeState = STATE_RUNNING;
		RunnableItem item;
		for (int i=0;i<itemList.size();i++){
			if ((item = itemList.get(i).getInvocationRunnable())!=null){
				runnableRunner.run(item.runnable, item.name, item.ueh);
			}
		}
	}
	
	public final void runAndWait() throws InterruptedException{
		run();
		synchronized(itemList){
			itemList.wait();
		}
	}
	
	public final RunTreeItem addRunnable(Runnable r, String name){
		return addRunnable(r, name, null, new RunTreeItem[0]);
	}
	
	public final RunTreeItem addRunnable(Runnable r, String name, UncaughtExceptionHandler ueh){
		return addRunnable(r, name, ueh, new RunTreeItem[0]);
	}
	
	public final RunTreeItem addRunnable(Runnable r, String name, RunTreeItem... mustBeReady){
		return addRunnable(r, name, null, mustBeReady);
	}

	public final RunTreeItem addRunnable(Runnable r, String name, UncaughtExceptionHandler ueh, RunTreeItem... mustBeReady){
		if (treeState != STATE_BUILDING)
			throw new RuntimeException();
		RunTreeItem erg = new RunTreeItem(r, name, mustBeReady, this, ueh);
		itemList.add(erg);
		return erg;
	}

	public final class RunTreeItem{
		private final RunTreeItem[] mustBeReady;
		private final Runnable runnable;
		private final String name;
		private final ArrayList<RunTreeItem> invokeWhenReady = new ArrayList<RunTreeItem>();
		private byte runnableState = STATE_WAITING;
		private final RunTree runTree;
		private final UncaughtExceptionHandler ueh;
		
		private RunTreeItem(Runnable r, String name, RunTreeItem mustBeReady[], RunTree runTree, UncaughtExceptionHandler ueh){
			this.runnable = r;
			this.name = name;
			this.runTree = runTree;
			this.ueh = ueh;
			this.mustBeReady = mustBeReady == null ? EMPTY_RUNTREE_ITEM_ARRAY : mustBeReady;
			for (RunTreeItem item : this.mustBeReady)
				item.invokeWhenReady.add(this);
		}
		
		public final byte getState(){
			return runnableState;
		}
		
		/**
		 * Gibt ein RunnableItem oder null zurueck.
		 * Wenn ein RunnableItem zurueckgegeben wird muss dieses ausgefuehrt werden. Ein erneuter aufruf dieser Methode wird einen Nullzeiger zuruechgeben!
		 */
		private final synchronized RunnableItem getInvocationRunnable(){
			if (runnableState != STATE_WAITING)
				return null;
			for (RunTreeItem item : mustBeReady)
				if (item.getState()!=STATE_FINISHED)
					return null;
			runnableState = STATE_RUNNING;
			return new RunnableItem(name, 
				new Runnable(){
					@Override
					public void run(){
						try{
							runnable.run();
						}catch(Exception e){
							ueh.uncaughtException(Thread.currentThread(), e);
						}
						runnableState = STATE_FINISHED;
						RunnableItem item, firstItem = null;
						for (int i=0;i<invokeWhenReady.size();i++){
							if ((item = invokeWhenReady.get(i).getInvocationRunnable())!= null){
								if (firstItem == null)
									firstItem = item;
								else
									runnableRunner.run(item.runnable, item.name, item.ueh);
							}
						}
						finished:{
							for (int i=0;i<itemList.size();i++)
								if (itemList.get(i).runnableState != STATE_FINISHED)
									break finished;
							runTree.treeState = STATE_FINISHED;
							synchronized (itemList) {
								itemList.notifyAll();
							}
						}
						if (firstItem != null){
							Thread.currentThread().setName(firstItem.name);
							try{
								firstItem.runnable.run();
							}catch(Exception e){
								firstItem.ueh.uncaughtException(Thread.currentThread(), e);
							}
						}
					}
				},
			ueh);
		}
	}

	public final int getState() {
		return treeState;
	}
	
	private class RunnableItem{
		public final String name;
		public final Runnable runnable;
		public final UncaughtExceptionHandler ueh;
		
		public RunnableItem(String name, Runnable runnable, UncaughtExceptionHandler ueh){
			this.name = name;
			this.runnable = runnable;
			this.ueh = ueh;
		}
	}
}
