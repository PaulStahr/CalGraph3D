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
package maths.functions.io;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.imageio.ImageIO;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import maths.Operation;
import maths.OperationCompiler;
import maths.OperationCompiler.CompileOptions;
import maths.VariableAmount;
import maths.data.ArrayOperation;
import maths.data.RealLongOperation;
import maths.data.StringOperation;
import maths.exception.ExceptionOperation;
import maths.exception.OperationParseException;
import maths.functions.FunctionOperation;
import util.StringUtils;

public class ReadOperation extends FunctionOperation{
	final Operation a;
	
	public ReadOperation(final Operation a){
		if ((this.a = a) == null)
			throw new NullPointerException();
	}
	
	@Override
	public Operation calculate(VariableAmount object, CalculationController control) {
		final Operation a = this.a.calculate(object, control);
		if (!(a.isString()))
			return new ReadOperation(a);
		final String value = a.stringValue();
		final File file = new File(value);
		final int index = value.lastIndexOf('.');
		if (index == -1)
			return new ExceptionOperation("File Type not detectable");
		if (!file.exists())
			return new ExceptionOperation("File doesn't exist");
		final String end = value.substring(index+1).toLowerCase();
		if (end.equals("bmp")||end.equals("jpg")||end.equals("png")){			
			try {
				final BufferedImage bi = ImageIO.read(file);
				return new ArrayOperation.MatrixCreator(bi.getWidth(), bi.getHeight()){
					
					@Override
					public Operation get(int y, int x) {
						return new RealLongOperation(bi.getRGB(x, y));
					}
				}.getArray();
			} catch (IOException e) {
				return new ExceptionOperation(e.toString());
			}
		}
		if (end.equals("txt")){
			try{
				final FileReader reader = new FileReader(file);
				char data[] = new char[1024];
				int read = 0, offset = 0;
				while ((read = reader.read(data, offset, data.length-offset))>0)
	            	if ((offset+=read)*2 == data.length)
	            		data = Arrays.copyOf(data, data.length*2);
				reader.close();
		        return new StringOperation(new String(data,0,offset));
			}catch(IOException e){
				return new ExceptionOperation(e.toString());
			}
		}
		if (end.equals("dat") || end.equals("csv"))
		{
			try
			{
				final FileReader reader = new FileReader(file);
				final BufferedReader inBuf = new BufferedReader(reader);
				String line;
				final ArrayList<Operation> lines = new ArrayList<Operation>();
				StringUtils.StringSplitIterator2 splitIterator = new StringUtils.StringSplitIterator2();
				final ArrayList<Operation> operationList = new ArrayList<Operation>();
				final char seperators[] = new char[] {' ','\t',',',';'};
				CompileOptions cp = new CompileOptions();
				while ((line = inBuf.readLine()) != null)
				{
					splitIterator.reset(line, 0, line.length(), seperators, false);
					while (splitIterator.hasNext())
					{//set(csv,read("/media/paul/Data1/Caesar/Scripts/rotations.csv"))
						try {
							operationList.add(OperationCompiler.compile(splitIterator, cp));
						} catch (OperationParseException e) {
							operationList.add(new ExceptionOperation(e.toString()));
						}
						splitIterator.increment();
					}
					lines.add(new ArrayOperation(operationList));
					operationList.clear();
				}
				inBuf.close();
				reader.close();
				return new ArrayOperation(lines);
			}catch(IOException e){
				return new ExceptionOperation(e.toString());
			}
		}
		if (end.equals("wav") || end.equals("mp3")){
			try {
				AudioInputStream in = AudioSystem.getAudioInputStream(file);
				AudioFormat baseFormat = in.getFormat();
				AudioFormat decodedFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,baseFormat.getSampleRate(), 16, baseFormat.getChannels(),baseFormat.getChannels() * 2, baseFormat.getSampleRate(),false);
				AudioInputStream din = AudioSystem.getAudioInputStream(decodedFormat, in);
				byte buf[] = new byte[2048];
				int read, offset = 0;
				final ArrayList<RealLongOperation> list= new ArrayList<RealLongOperation>();	
				while ((read = din.read(buf, offset, buf.length))>0){
					for (int i=0;i+1<read;i+=2)
						list.add(RealLongOperation.intern(buf[i]%0xFF + (8>>(buf[i+1]&0xFF))));
					if (read%2 != 0){
						offset = 1;
						buf[0] = buf[read-1];
					}else{
						offset = 0;
					}				
				}
				return new ArrayOperation(list);
			} catch (UnsupportedAudioFileException e) {
				e.printStackTrace();
				return new ExceptionOperation("Unsopported Audio File");
			} catch (IOException e) {
				return  new ExceptionOperation(e.toString());
			}
		}
		return new ExceptionOperation("File Type not known");
	}
	
	@Override
	public final int size() {
		return 1;
	}
	
	@Override
	public final Operation get(int index) {
		switch (index){
			case 0: return a;
			default:throw new ArrayIndexOutOfBoundsException(index);
		}
	}

	@Override
	public String getFunctionName() {
		return "read";
	}

	@Override
	public Operation getInstance(List<Operation> subclasses) {
		return new ReadOperation(subclasses.get(0));
	}
}
