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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import maths.Operation;
import maths.OperationCompiler;
import maths.VariableAmount;
import maths.data.ArrayOperation;
import maths.exception.ExceptionOperation;
import maths.exception.OperationParseException;
import maths.functions.FunctionOperation;

public class ReadCsvOperation extends FunctionOperation{
	final Operation a;
	final Operation seperator;
	public ReadCsvOperation(final Operation a, final Operation seperator){
		if ((this.a = a) == null || (this.seperator = seperator) == null)
			throw new NullPointerException();
	}
	
	@Override
	public Operation calculate(VariableAmount object, CalculationController control) {
		final Operation a = this.a.calculate(object, control);
		if (!(a.isString()))
			return new ReadCsvOperation(a, seperator);
		final String value = a.stringValue();
		final File file = new File(value);
		final int index = value.lastIndexOf('.');
		if (index == -1)
			return new ExceptionOperation("File Type not detectable");
		if (!file.exists())
			return new ExceptionOperation("File doesn't exist");
		try
		{
			final FileReader reader = new FileReader(file);
			final BufferedReader inBuf = new BufferedReader(reader);
			String line;
			final char seperationChars[][] = new char[seperator.size()][];
			for (int i = 0; i < seperator.size(); ++i)
			{
				Operation column = seperator.get(i);
				seperationChars[i] = new char[column.size()];
				for (int j = 0; j < column.size(); ++j)
				{
					seperationChars[i][j] = (char)column.get(j).longValue();
				}
				Arrays.sort(seperationChars[i]);
			}
			@SuppressWarnings("unchecked")
			ArrayList<Operation> operationStack[] = new ArrayList[seperationChars.length];
			for (int i = 0; i < operationStack.length; ++i)
			{
				operationStack[i] = new  ArrayList<>();
			}
			StringBuilder strB = new StringBuilder();
			while ((line = inBuf.readLine()) != null)
			{
				for (int i = 0; i <= line.length(); ++i)
				{
					char c = i == line.length() ? '\n' : line.charAt(i);
					parseChar:{
						for (int j = 0; j < seperationChars.length; ++j)
						{
							if (Arrays.binarySearch(seperationChars[j], c) >= 0)
							{
								try {
									operationStack[operationStack.length - 1].add(OperationCompiler.compile(strB));
								} catch (OperationParseException e) {
									operationStack[operationStack.length - 1].add(new ExceptionOperation(e.toString()));
								}
								strB.setLength(0);
								for (int k = operationStack.length - 2; k >= j; --k)
								{
									operationStack[k].add(new ArrayOperation(operationStack[k+1]));
									operationStack[k+1].clear();
								}
								break parseChar;
							}
						}
						strB.append(c);
					}
				}
			}
			inBuf.close();
			reader.close();
			return new ArrayOperation(operationStack[0]);
		}catch(IOException e){
			return new ExceptionOperation(e.toString());
		}
	}
	
	@Override
	public final int size() {
		return 2;
	}
	
	@Override
	public final Operation get(int index) {
		switch (index){
			case 0: return a;
			case 1: return seperator;
			default:throw new ArrayIndexOutOfBoundsException(index);
		}
	}

	@Override
	public String getFunctionName() {
		return "readcsv";
	}

	@Override
	public Operation getInstance(List<Operation> subclasses) {
		return new ReadCsvOperation(subclasses.get(0), subclasses.get(1));
	}
}
