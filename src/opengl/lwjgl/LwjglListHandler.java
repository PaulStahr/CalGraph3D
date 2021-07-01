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

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Arrays;

import org.lwjgl.opengl.GL11;

import util.data.UniqueObjects;

public class LwjglListHandler {
	private int freeLists[] = UniqueObjects.EMPTY_INT_ARRAY;
	private int freeListCount = 0;
	private final ReferenceQueue<GlList> queue = new ReferenceQueue<>();

	public LwjglListHandler(){
		this(10);
	}

	public LwjglListHandler(int initalLists){
		 allocateLists(initalLists);
	}

	public final void allocateLists(final int count){
		int id = GL11.glGenLists(count);
		if (freeLists.length < freeListCount + count)
			freeLists = Arrays.copyOf(freeLists, freeListCount + count);
		for (int i=0;i<count;++i,freeListCount++)
			freeLists[freeListCount] = id + i;
	}

	public final void removeUnused(){
		while(true){
			Reference<? extends GlList> list = queue.poll();
			if (list == null)
				return;
			ListContainer lc = (ListContainer)list;
			if (!lc.isDestroyed){
				freeLists[freeListCount] = lc.id;
				++freeListCount;
				lc.isDestroyed = true;
			}
		}
	}

	public final GlList getFreeList(){
		if (freeListCount == 0)
			removeUnused();
		if (freeListCount == 0)
			allocateLists(5);
		--freeListCount;
		GlList glList = new GlList(freeLists[freeListCount]);
		glList.lc = new ListContainer(glList, queue);
		return glList;
	}

	private static final class ListContainer extends WeakReference<GlList>{
		final int id;
		boolean isDestroyed = false;
		private ListContainer(GlList glList, ReferenceQueue<GlList> queue){
			super(glList, queue);
			this.id = glList.id;
		}
	}

	public static final class GlList{
		private final int id;
		private ListContainer lc;
		private boolean isFilled;
		private boolean isRecording;
		private boolean isDestroyed;
		private boolean isProtected;
		private boolean shouldBeDestroyed;

		private GlList(int id){
			this.id = id;
		}

		public final void startRecord(int mode){
			if (isDestroyed)
				throw new RuntimeException("List was destryed");
			if (isRecording)
				throw new RuntimeException("List is alredy recording");
			GL11.glNewList(id, mode);
			isRecording = true;
		}

		public final void stopRecord(){
			if (isDestroyed)
				throw new RuntimeException("List was destroyed");
			if (!isRecording)
				throw new RuntimeException("List isn't recording");
			GL11.glEndList();
			isFilled = true;
			isRecording = false;
		}

		public final boolean isRecording(){
			return isRecording;
		}

		public final boolean isFilled(){
			return isFilled;
		}

		public final boolean call(){
			if (isDestroyed)
				throw new RuntimeException("List was destroyed");
			if (!isFilled)
				return false;
	        GL11.glCallList(id);
	        return true;
		}

		public final void destroy(){
			if (isRecording)
			{
				throw new RuntimeException("List is recording");
			}
			if (!isProtected){
				isDestroyed = true;
				isFilled = false;
				lc.enqueue();
			}else
				shouldBeDestroyed = true;
		}

		public final boolean isDestroyed(){
			return isDestroyed;
		}

		/**
		 * Sorgt daf\u00FCr, dass eine Liste nicht als destroyed markiert werden kann
		 * @param isProtected
		 */
		public final void setProtected(boolean isProtected){
			this.isProtected = isProtected;
			if (!isProtected && shouldBeDestroyed)
				destroy();
		}

		public final boolean isProtected(){
			return isProtected;
		}
	}
}
