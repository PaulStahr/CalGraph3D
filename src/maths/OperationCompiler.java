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
package maths;
	
import java.util.ArrayList;
import java.util.Arrays;

import maths.algorithm.MathematicStringUtil;
import maths.data.ArrayOperation;
import maths.data.BooleanOperation;
import maths.data.CharacterOperation;
import maths.data.Characters;
import maths.data.ComplexLongOperation;
import maths.data.RealDoubleOperation;
import maths.data.RealLongOperation;
import maths.data.StringOperation;
import maths.exception.OperationParseException;
import maths.functions.AbsoluteOperation;
import maths.functions.ArgumentOperation;
import maths.functions.ArrayIndexOperation;
import maths.functions.BinomCdfOperation;
import maths.functions.BinomPdfOperation;
import maths.functions.CallProgramFunction;
import maths.functions.CholeskyOperation;
import maths.functions.ConjugateOperation;
import maths.functions.CreateAffineTransformation;
import maths.functions.CreateRotationOperation;
import maths.functions.CrossProductOperation;
import maths.functions.CumSumOperation;
import maths.functions.DeterminantenOperation;
import maths.functions.DifferentiationOperation;
import maths.functions.Fakultaet;
import maths.functions.ForOperation;
import maths.functions.FormatOperation;
import maths.functions.GgtOperation;
import maths.functions.IfOperation;
import maths.functions.ImaginaryPartOperation;
import maths.functions.JavaCommand;
import maths.functions.KardOperation;
import maths.functions.KgVOperation;
import maths.functions.LogarithmOperation;
import maths.functions.MakeColOperation;
import maths.functions.MatrixMultiplication;
import maths.functions.MaximumOfArray;
import maths.functions.MaximumOperation;
import maths.functions.MinimumOfArray;
import maths.functions.MinimumOperation;
import maths.functions.Modulo;
import maths.functions.MultipleDifferentiationOperation;
import maths.functions.NcrOperation;
import maths.functions.NormOperation;
import maths.functions.NprOperation;
import maths.functions.NumericIfOperation;
import maths.functions.NumericIntegralOperation;
import maths.functions.RandomListOperation;
import maths.functions.RandomMatrixOperation;
import maths.functions.RealPartOperation;
import maths.functions.RoundOperation;
import maths.functions.RowReducedEchelonFormOperation;
import maths.functions.SignOperation;
import maths.functions.SkalarProductOpertion;
import maths.functions.SleepOperation;
import maths.functions.SolveOperation;
import maths.functions.SortOperation;
import maths.functions.SumOfArrayOperation;
import maths.functions.TaylorPolynomOperation;
import maths.functions.TransposeOperation;
import maths.functions.UnequalsOperation;
import maths.functions.WhileOperation;
import maths.functions.atomic.AdditionOperation;
import maths.functions.atomic.AndOperation;
import maths.functions.atomic.ConcatOperation;
import maths.functions.atomic.DivisionOperation;
import maths.functions.atomic.EqualsOperation;
import maths.functions.atomic.HigherEqualsOperation;
import maths.functions.atomic.HigherOperation;
import maths.functions.atomic.IsElementOfOperation;
import maths.functions.atomic.IsNotElementOfOperation;
import maths.functions.atomic.IsNotSubsetOperation;
import maths.functions.atomic.IsSubsetOperation;
import maths.functions.atomic.LowerEqualsOperation;
import maths.functions.atomic.LowerOperation;
import maths.functions.atomic.MultiplicationOperation;
import maths.functions.atomic.NegativeOperation;
import maths.functions.atomic.NotOperation;
import maths.functions.atomic.OrOperation;
import maths.functions.atomic.PowerOperation;
import maths.functions.atomic.SubtractionOperation;
import maths.functions.conversion.CharToStringOperation;
import maths.functions.conversion.ToCharacterOperation;
import maths.functions.conversion.ToDoubleOperation;
import maths.functions.conversion.ToExpressionOperation;
import maths.functions.conversion.ToLongOperation;
import maths.functions.conversion.ToStringOperation;
import maths.functions.hyperbolic.ArcCosinusOperation;
import maths.functions.hyperbolic.ArcSinusOperation;
import maths.functions.hyperbolic.ArcTangensOperation;
import maths.functions.hyperbolic.CosinusHyperbolicOperation;
import maths.functions.hyperbolic.CosinusOperation;
import maths.functions.hyperbolic.SinusHyperbolicOperation;
import maths.functions.hyperbolic.SinusOperation;
import maths.functions.hyperbolic.TangensHyperbolicOperation;
import maths.functions.hyperbolic.TangensOperation;
import maths.functions.interators.CreateListOperation;
import maths.functions.interators.ProductIteratorOperation;
import maths.functions.interators.SumIteratorOperation;
import maths.functions.io.ReadCsvOperation;
import maths.functions.io.ReadOperation;
import maths.functions.io.RequestOperation;
import maths.functions.io.RequestTimeoutOperation;
import maths.functions.io.WriteCsvOperation;
import maths.functions.io.WriteOperation;
import maths.functions.variable.DefineOperation;
import maths.functions.variable.DeleteOperation;
import maths.functions.variable.SetOperation;
import maths.variable.UserVariableOperation;
import maths.variable.Variable;
import util.StringUtils;
import util.data.IntegerArrayList;


/** 
* @author  Paul Stahr
* @version 04.02.2012
*/
public final class OperationCompiler
{
    private static final char calculationCharacters[];
    private static final byte calculationCharactersPrirority[];
	private static ArrayList<ProgramFunction> programFunction = new ArrayList<ProgramFunction>();
    
	static{
	    final char characters[][] = {{Characters.END_COMMAND},
				{Characters.SET},
				{Characters.AND, Characters.OR},
				{Characters.EQ,Characters.NOT_EQ, Characters.ELEM_OF, Characters.SUBSET},
				{Characters.LOW_EQ, Characters.HIGH_EQ, Characters.LOW,Characters.HIGH},
				{Characters.ADD,Characters.SUB,Characters.CONCAT},
				{Characters.MULT_SKAL, Characters.MULT_MAT, Characters.MULT_CROSS, Characters.MULT,Characters.DIV,Characters.MOD},
				{Characters.POW},
				{Characters.HIGH_T},
				{Characters.NOT}};
		int length = 0;
		for (char c[] : characters)
			length += c.length;
		calculationCharacters = new char[length];
		calculationCharactersPrirority = new byte[length];
		length = 0;
		for (char c[]:characters){
			System.arraycopy(c,0,calculationCharacters, length, c.length);
			length += c.length;
		}
		Arrays.sort(calculationCharacters);
		for (byte j=0;j<characters.length;j++)
			for (char c :characters[j])
				calculationCharactersPrirority[Arrays.binarySearch(calculationCharacters, c)] = j;
	}

	public static final boolean isCalculationChar(char c){
		return Arrays.binarySearch(calculationCharacters, c)>=0;
	}

	/**
     * Gibt eine Operation zurueck
     * @param b der Wert den man haben will 
     * @return die Boolean Operation
     */
    public static final BooleanOperation get (boolean b){
        return b ? BooleanOperation.TRUE : BooleanOperation.FALSE;
    }

    public static final RealLongOperation get (long rechnung){
        return new RealLongOperation (rechnung);
    }

    public static final RealDoubleOperation get (double rechnung){
        return new RealDoubleOperation (rechnung);
    }
    
    public static void addProgramFunction(ProgramFunction pf){
    	if (pf == null)
    		throw new NullPointerException();
    	programFunction.add(pf);
    }
    
    public static final StringBuilder toString(StringBuilder strB, double values[])
    {
    	
    	strB.append('{');
    	if (values.length != 0)
    	{
    		strB.append(values[0]);
    		for (int i = 1; i < values.length; ++i)
    		{
    			strB.append(',').append(values[i]);
    		}
    	}
    	return strB.append('}');
    }
    
    public static final StringBuilder toString(StringBuilder strB, long values[])
    {
    	
    	strB.append('{');
    	if (values.length != 0)
    	{
    		strB.append(values[0]);
    		for (int i = 1; i < values.length; ++i)
    		{
    			strB.append(',').append(values[i]);
    		}
    	}
    	return strB.append('}');
    }
    
    public static final StringBuilder toString(StringBuilder strB, int values[])
    {
    	
    	strB.append('{');
    	if (values.length != 0)
    	{
    		strB.append(values[0]);
    		for (int i = 1; i < values.length; ++i)
    		{
    			strB.append(',').append(values[i]);
    		}
    	}
    	return strB.append('}');
    }
    
	/*private static enum ParseCondition{
		VARIABLE_NAME, FUNCTION_NAME, START;
	}
	
	private static class OperationWrapper{
		private final int prirority;
		private final CharSequence str;
		
		private OperationWrapper(int prirority, CharSequence str){
			this.prirority = prirority;
			this.str = str;
		}
	}*/
	
    /*public static final Operation compile2(CharSequence str) throws OperationParseException{
    	if (str == null)
    		throw new NullPointerException();
    	if (str.length() == 0)
    		throw new OperationParseException("");
    	int deth = 0;
    	ParseCondition pc = ParseCondition.START;
    	ArrayList<OperationWrapper> al = new ArrayList<OperationWrapper>();
    	for (int i=0;i<str.length();i++){
    		final char charAt = str.charAt(i);
    		switch (pc){
    			case START:
    				
    		}
    	}
    }*/
    
    public static class CompileOptions
    {
    	boolean interpret_latex = true;
    }
    
    public static final Operation compile(CharSequence str, CompileOptions opt) throws OperationParseException{
    	return compile(str, 0, str.length(),opt);
    }
    
    public static final Operation compile(CharSequence str) throws OperationParseException{
    	return compile(str, 0, str.length(), new CompileOptions());
    }
    
    public static final Operation compile(CharSequence str, int begin, int end) throws OperationParseException{
    	return compile(str, begin, end, new CompileOptions());
    }

    public static final Operation compile(CharSequence str, int begin, int end, CompileOptions opt) throws OperationParseException{
    	if (str == null)
    		throw new NullPointerException();
    	if (end-begin == 0)
    		throw new OperationParseException("");
    	final StringBuilder res = new StringBuilder(end - begin);
    	boolean isStr = false, isChar = false;
    	for (int i=begin;i<end;i++){
     		final char charAt = str.charAt(i);
    		switch (charAt){
    			case '\\':{
    				res.append(charAt);
    				if (++i >= str.length())
        				throw new OperationParseException(str.subSequence(begin, end));    					
    				res.append(str.charAt(i));
    				continue;
    			}case '"':{
    				if (!isChar)
    					isStr = !isStr;
    				break;
    			}case '\'':{
    				if (!isStr)
    					isChar = !isChar;
    				break;
    			}case ' ':
    			case '\n':{
    				if (!isStr && !isChar)
    					continue;
    				break;
    			}
    		}
   			res.append(charAt);
    	}
        return compileRek(res.toString(), 0, res.length(), opt);
    }

    private static final Operation compileRek (final String str, final int begin, final int end, final CompileOptions opt) throws OperationParseException{
    	if (begin == end)
    	{
            throw new OperationParseException(str);
    	}
    	final int length = end - begin;
        final char firstChar = str.charAt(begin), lastChar = str.charAt(end-1);
        switch (length){
        	case 1:{
	            switch(firstChar){
	                case Characters.INFTY	: return RealDoubleOperation.POSITIVE_INFINITY;
	                case Characters.PI		: return RealDoubleOperation.PI;
	                case Characters.EULER	: return RealDoubleOperation.E;
	                case Characters.C		: return MengenOperation.C;
	                case Characters.N		: return MengenOperation.N;
	                case Characters.P		: return MengenOperation.P;
	                case Characters.Q		: return MengenOperation.Q;
	                case Characters.R		: return MengenOperation.R;
	                case Characters.Z		: return MengenOperation.Z;
	                case Characters.I		: return ComplexLongOperation.POSITIVE_ONE_I;
	                case Characters.FIBUNACCI: return MengenOperation.F;
	            }
	            break;
        	}case 2:{
        		if (firstChar == '-' && lastChar == Characters.INFTY)
        			return RealDoubleOperation.NEGATIVE_INFINITY;
        		break;
        	}case 3:{
            	if (firstChar == '\'' && lastChar =='\'')
           			return CharacterOperation.getInstance(str.charAt(begin + 1));
        		if (opt.interpret_latex && firstChar == '\\' && str.charAt(begin + 1) == 'p' && lastChar == 'i')
        			return RealDoubleOperation.PI;
           		break;
        	}case 4:{
        		if (firstChar == '\'' && lastChar == '\'' && str.charAt(begin + 1) == '\\')
    			{
    				switch(str.charAt(begin + 2))
    				{
    					case 'n' : return CharacterOperation.getInstance('\n');
    					case 't' : return CharacterOperation.getInstance('\t');
    				}
    			}   			
        		if (StringUtils.equals(str, begin, end, "true"))
    	            return BooleanOperation.TRUE;
    	       break;
        	}case 5:{
                if (StringUtils.equals(str, begin, end, "false"))
                    return BooleanOperation.FALSE;
                if (StringUtils.equals(str, begin, end, "undef"))
                	return RealDoubleOperation.NaN;
                break;
        	}case 6:{
        		if (opt.interpret_latex && firstChar == '\\' && StringUtils.equals(str, begin + 1, end, "euler"))
                    return RealDoubleOperation.E;
        	}
        }
    	RealLongOperation lo = end - begin >= 2 && firstChar == '0' && str.charAt(begin + 1) == 'x' ? RealLongOperation.valueOf(str, 2 + begin, end, 16) : RealLongOperation.valueOf(str, begin, end, 10);
        if (lo != null)
        	return lo;       
        try{
        	if (MathematicStringUtil.isDouble(str, begin, end))
        		return new RealDoubleOperation(Double.parseDouble(str.substring(begin, end)));
        } catch (NumberFormatException e){}
        	
        final int indexOfCharacter = MathematicStringUtil.lastIndexOf (str, begin, end, calculationCharacters, calculationCharactersPrirority);
        if (indexOfCharacter != -1){
        	final char c1 = str.charAt(indexOfCharacter), c0;
        	final Operation o = (c1 == Characters.ADD || c1 == Characters.SUB) && indexOfCharacter != begin && isCalculationChar(c0 = str.charAt(indexOfCharacter - 1)) ? 
        			  get(c0, str, begin, indexOfCharacter-1, str, indexOfCharacter, end, opt) 
        			: get (c1, str, begin, indexOfCharacter, str, indexOfCharacter + 1, end, opt);
        	if (o == null)
        		throw new OperationParseException(str);
        	return o;
        }

        if (Characters.isHighNumber(lastChar)){
        	for (int i=end-2;i>=begin;i--)
        		if (!Characters.isHighNumber(str.charAt(i)))
        		{
        			if (Characters.HIGH_SUB == str.charAt(i) || Characters.HIGH_PLUS == str.charAt(i))
        			{
        				--i;
        			}
        			return PowerOperation.getInstance(compileRek(str, begin,i+1, opt), RealLongOperation.valueOfHighNumber(str, i + 1, end, 10));
        		}
        	throw new OperationParseException(str);
        }
        	
        switch(lastChar){
	    	case '!':{
	    		return new Fakultaet(compileRek(str, begin ,end - 1, opt));
	    	}case ']':{
	    		final int index = MathematicStringUtil.lastIndexOf(str, begin, end, '[');//TODO
	            if (index != -1 && MathematicStringUtil.onlyBrackets(str, index, end, '[', ']'))
	                return new ArrayIndexOperation(compileRek(str, begin, index, opt), compileRek(str, index + 1, end - 1, opt));
	            break;
	    	}case ')':{
		        if (MathematicStringUtil.onlyBrackets(str, begin, end, '(', ')'))
		            return compileRek(str, begin + 1, end - 1, opt);
		        if (MathematicStringUtil.isFunction(str, begin, end)){
		        	final int indexOfBracket = StringUtils.indexOf(str, begin, end, '(');
		            final String functionName = str.substring(begin, indexOfBracket);
		            final Operation parameter[];
		            if (indexOfBracket + 2 < end)
		            {
		            	final IntegerArrayList ial = new IntegerArrayList(3);
			            MathematicStringUtil.split(str, indexOfBracket+1,end-1, ',', ial);
			            parameter = new Operation[ial.size() + 1];    	
			            
			            for (int i = 0; i < parameter.length;i++)
			            {
			            	int i0 = i == 0 ? indexOfBracket+1 : ial.getI(i - 1) + 1;
			            	int i1 = i == ial.size() ? end - 1 : ial.getI(i);
			                parameter[i] = compileRek(str, i0, i1, opt);
			            }
			        }
		            else
		            {
		            	parameter = Operation.EMPTY_OPERATION_ARRAY;
		            }

		            switch (parameter.length){
		                case 0:{
		                	switch(functionName){
		                		case "rand":   return SystemFunctions.random;
		                		case "gc":     return SystemFunctions.gc;
		                		case "exit":   return SystemFunctions.exit;
		                	}
	                		for (ProgramFunction pf : programFunction)
	                			if (functionName.equals(pf.name))
	                				return pf;
		                    break;
		                } case 1:{
		                	final Operation a = parameter[0];
		                	switch (functionName){
		                		case "abs":		return new AbsoluteOperation(a);
		                		case "acos":	return new ArcCosinusOperation(a);
		                		case "arg":		return new ArgumentOperation(a);
		                		case "asin":	return new ArcSinusOperation(a);
		                		case "atan":	return new ArcTangensOperation(a);
		                		case "cholesky":return new CholeskyOperation(a);
		                		case "conjugate":return new ConjugateOperation(a);
		                        case "cos":    	return new CosinusOperation(a);
		                        case "cosh":   	return new CosinusHyperbolicOperation(a);
		                        case "compile":	return new ToExpressionOperation (a);
		                        case "char":   	return new ToCharacterOperation (a);
		                        case "cbrt":   	return new PowerOperation.CubeRootOperation(a);
		                        case "chartostring":return new CharToStringOperation(a);
		                        case "cumsum":	return new CumSumOperation(a);
		                        case "delete": 	return new DeleteOperation(a);
		                        case "define": 	return new DefineOperation(a);
		                        case "det":		return new DeterminantenOperation(a);
		                        case "exp":		return new PowerOperation.ExponentOperation(a);
		                        case "float":  	return new ToDoubleOperation (a);
				                case "ifn":    	return new NumericIfOperation(a);
		                        case "int":    	return new ToLongOperation (a);
		                        case "imag":	return new ImaginaryPartOperation(a);
		                        case "java":	return new JavaCommand(a);
		                        case "kard":   	return new KardOperation(a);
		                        case "log":		return new LogarithmOperation(a);
		                        case "min":		return new MinimumOfArray(a);
		                        case "max":		return new MaximumOfArray(a);
		                        case "norm": 	return new NormOperation(a);
		                        case "program":	return ProgramOperation.getInstance(a);
		                		case "range":	return new CreateListOperation(new UserVariableOperation("tmp"), ArrayOperation.getInstance(new Operation[] {new UserVariableOperation("tmp"), RealLongOperation.ZERO, a}));
		                        case "round":	return new RoundOperation(a);
		                        case "randlist":return new RandomListOperation(a);
		                        case "request":	return new RequestOperation(a);
		                        case "read":	return new ReadOperation(a);
		                        case "rref":	return new RowReducedEchelonFormOperation(a);
		                        case "real":	return new RealPartOperation(a);
		                        case "string":	return new ToStringOperation (a);
		                        case "sum":    	return new SumOfArrayOperation(a);
		                        case "sqrt":   	return new PowerOperation.SquareRootOperation(a);                			
		                        case "sin":    	return new SinusOperation(a);
		                        case "sinh":	return new SinusHyperbolicOperation(a);
		                        case "sign":	return new SignOperation(a);
		                        case "sort":	return new SortOperation(a);
		                        case "sleep":	return new SleepOperation(a);
		                        case "tan":    	return new TangensOperation(a);
		                        case "tanh":   	return new TangensHyperbolicOperation(a);
		                        case "transpose":return new TransposeOperation(a);
		                	}
		                    break;
		                } case 2:{
		                	final Operation a = parameter[0], b = parameter[1];
		                	switch(functionName){
		                		case "diff"		:return new DifferentiationOperation(a, b);
		                		case "format"	:return new FormatOperation(a, b);
		                		case "ggt"		:return new GgtOperation(a, b);
		                		case "kgv"		:return new KgVOperation(a, b);
		                		case "list"		:return new CreateListOperation(a,b);
		                		case "min"		:return new MinimumOperation (a, b);
		                		case "max"		:return new MaximumOperation (a, b);
		                		case "ncr"		:return new NcrOperation(a, b);
		                		case "npr"		:return new NprOperation(a, b);
		                		case "prod"		:return new ProductIteratorOperation(a,b);
		                		case "range"	:return new CreateListOperation(new UserVariableOperation("tmp"), ArrayOperation.getInstance(new Operation[] {new UserVariableOperation("tmp"), a, b}));
		                		case "request"	:return new RequestTimeoutOperation(a, b);
		                		case "randmat"	:return new RandomMatrixOperation(a, b);
		                		case "rotmat"	:return new CreateRotationOperation(a, b);
		                        case "affine"	:return new CreateAffineTransformation(a, b);
		                        case "readcsv"	:return new ReadCsvOperation(a, b);
		                        case "writecsv" :return new WriteCsvOperation(a, b);
		                		case "set":{
		                        	if (a instanceof UserVariableOperation || a instanceof UserFunctionOperation || a instanceof ArrayIndexOperation)
		                        		return new SetOperation(b, a);
		                        	throw new OperationParseException(str);
		                        }case "solve":return new SolveOperation(a, b);
				                case "sum":	return new SumIteratorOperation(a, b);
		                        case "while":	return new WhileOperation(a, b);
		                        case "write":	return new WriteOperation(a, b);
		                	}
		                    break;
		                } case 3:{
		                  	final Operation a = parameter[0], b = parameter[1], c=parameter[2];
		                  	switch(functionName){
				                case "if":    return new IfOperation(a, b, c);
				                case "binompdf":return new BinomPdfOperation(a, b, c);
				                case "binomcdf":return new BinomCdfOperation(a, b, c);
				                case "diff":	return new MultipleDifferentiationOperation(a, b, c);
                            }
		                    break;
		                }case 4:{
		                  	final Operation a = parameter[0], b = parameter[1], c=parameter[2], d=parameter[3];
		                  	switch(functionName){
		                  		case "for":    return new ForOperation(a, b, c, d);
		                  		case "makecol":return new MakeColOperation(a, b, c, d);
		                  		case "taylor_polynom":return new TaylorPolynomOperation(a, b, c, d);
		                  		case "nint":return new NumericIntegralOperation(a, b, c, d);
				                case "java":  return new CallProgramFunction(a, b, c, d);
		                        case "writecsv" :return new WriteCsvOperation(a, b, c, d);
		                  	}
		                  	break;
		                }
		            }
		            return new UserFunctionOperation(functionName, parameter);
		        }
		        break;
	    	}case '"':{
		        if (MathematicStringUtil.justString(str, begin, end)){
		        	final Operation o = StringOperation.getInstance(str, 1 + begin, end - 1);
		        	if (o == null)
		        		throw new OperationParseException(str);
		        	return o;
		        }
		        break;
	    	}case '}':{
		        if (MathematicStringUtil.onlyBrackets(str, begin, end, '{', '}')){
		        	final IntegerArrayList ial = new IntegerArrayList();
		        	if (begin + 2 == end)
		        	{
		        		return ArrayOperation.EMPTY_ARRAY_OPERATION;
		        	}
		            MathematicStringUtil.split(str, begin + 1, end - 1, ',', ial);
		            final Operation operations[] = new Operation[ial.size() + 1];
		            for (int i=0;i<operations.length;i++) {
		            	int i0 = i == 0 ? begin+1 : ial.getI(i - 1) + 1;
		            	int i1 = i == ial.size() ? end - 1 : ial.getI(i);
		                operations[i] = compileRek(str, i0, i1, opt);
		            }
		            return ArrayOperation.getInstance(operations);
		        }
		        break;
	    	}default:{
	            if (Variable.isValidName(str, begin, end))
	                return new UserVariableOperation (str, begin, end);	    		
	    	}
	    }
        throw new OperationParseException(str);
    }
    
    private static final Operation get(char character, String operand0, int begin0, int end0, String operand1, int begin1, int end1, CompileOptions opt) throws OperationParseException{
        final Operation b = compileRek(operand1, begin1, end1, opt);
        if (begin0 != end0){
            final Operation a = compileRek(operand0, begin0, end0, opt);
            switch (character){
                case Characters.EQ      	:return new EqualsOperation(a,b);
                case Characters.NOT_EQ  	:return new UnequalsOperation(a,b);
                case Characters.LOW     	:return new LowerOperation(a,b);
                case Characters.HIGH   		:return new HigherOperation(a,b);
                case Characters.LOW_EQ  	:return new LowerEqualsOperation(a,b);
                case Characters.HIGH_EQ 	:return new HigherEqualsOperation(a,b);
                case Characters.ELEM_OF 	:return new IsElementOfOperation(a,b);
                case Characters.NOT_ELEM_OF	:return new IsNotElementOfOperation(a,b);
                case Characters.AND   		:return new AndOperation(a,b);
                case Characters.OR  		:return new OrOperation(a,b);
                case Characters.SET   		:return new SetOperation(a, b);
                case Characters.ADD     	:return new AdditionOperation(a,b);
                case Characters.SUB     	:return new SubtractionOperation(a,b);
                case Characters.CONCAT  	:return new ConcatOperation(a,b);
                case Characters.MULT		:return new MultiplicationOperation(a,b);
                case Characters.DIV			:return new DivisionOperation(a,b);
                case Characters.MOD     	:return new Modulo(a,b);
                case Characters.POW     	:return PowerOperation.getInstance(a,b);
                case Characters.MULT_MAT   	:return new MatrixMultiplication(a,b);
                case Characters.MULT_CROSS 	:return new CrossProductOperation(a, b);
                case Characters.SUBSET		:return new IsSubsetOperation(a, b);
                case Characters.NOT_SUBSET	:return new IsNotSubsetOperation(a, b);
                case Characters.MULT_SKAL	:return new SkalarProductOpertion(a, b);
                case Characters.END_COMMAND	:return new CommandOperation(a,b);
            }
        }else{
            switch (character){
                case Characters.ADD		:return b;
                case Characters.SUB		:return new NegativeOperation(b);
                case Characters.NOT   	:return new NotOperation(b);
                case Characters.HIGH_T	:return new TransposeOperation(b);
            }
        }
        return null;
    }

	public static final int parseInt(String text, int def) {
		try {
			return Integer.parseInt(text);
		}catch(NumberFormatException e){
			return def;
		}
	}

	public static Operation compile(String str, Operation standart) {
		try
		{
			return compile(str);
		}catch(Exception e)
		{
			return standart;
		}
	}
}
