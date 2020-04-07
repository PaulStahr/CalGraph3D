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
package maths.functions;

import java.util.AbstractList;
import java.util.Arrays;
import java.util.List;

import maths.algorithm.Calculate;

public class Permutation {
	@SuppressWarnings("unchecked")
	private static List<Permutation> permutations[] = new List[2];
	
	static{
		permutations[0] = new AbstractList<Permutation>(){
			@Override
			public Permutation get(int index) {
				throw new ArrayIndexOutOfBoundsException(index);
			}

			
			@Override
			public int size() {
				return 0;
			}
		};
		
		permutations[1] = new AbstractList<Permutation>(){
			private Permutation p = new Permutation(new byte[]{0}, (byte)1);
			
			@Override
			public Permutation get(int index) {
				if (index == 0)
					return p;
				throw new ArrayIndexOutOfBoundsException(index);
			}

			
			@Override
			public int size() {
				return 1;
			}
		};
	}
	
	private byte data[];
	private byte signum;
	
	public static final List<Permutation> getPermutationList(int n){
		if (n >= permutations.length){
			setToSize(n+1);
		}
		return permutations[n];
	}
	
	private Permutation(byte data[], byte signum){
		this.data = data;
		this.signum = signum;
	}
	
	private static final synchronized void setToSize(int n){
		List<Permutation> newLists[]  = Arrays.copyOf(permutations,n);

		for (int i=permutations.length;i<n;i++){
			List<Permutation> oldList = newLists[i-1];
			final Permutation packIntoList[] = new Permutation[(int)Calculate.factLong(i)];
			for (int j=0;j<oldList.size();j++){
				byte oldData[] = oldList.get(j).data;
				byte oldSignum = oldList.get(j).signum;
				for (int k=0;k<i;k++){
					byte current[] = new byte[i];
					System.arraycopy(oldData, 0, current, 0, k);
					current[k] = (byte)(i-1);
					System.arraycopy(oldData, k, current, k+1, i-k-1);
					packIntoList[j*i+k] = new Permutation(current, (byte)(oldSignum * ((i - k) % 2 == 0 ? -1 : 1)));
				}
			}
			newLists[i] = new AbstractList<Permutation>(){

				
				@Override
				public Permutation get(int arg0) {
					return packIntoList[arg0];
				}

				
				@Override
				public int size() {
					return packIntoList.length;
				}
			};
		}
		permutations = newLists;
	}
	
	public int get(int n){
		return data[n];
	}
		
	public byte signum(){
		return signum;
	}
	
	public int length(){
		return data.length;
	}
}
