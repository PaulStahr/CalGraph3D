package util;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

public class ClassFactory
{

	File file;
	FileWriter fileWriter;
	JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
	List<File> fileList;
	DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();
	StandardJavaFileManager compileFileManager = compiler.getStandardFileManager(null, null, null);
	StandardJavaFileManager diagnosticsFileManager = compiler.getStandardFileManager(diagnostics, null, null);
	Iterable<? extends JavaFileObject> units;
	String code;

	public ClassFactory(String filename) throws IOException
	{
		this(new File(filename));
	}

	public ClassFactory(File file) throws IOException
	{
		this.file = file;
		fileWriter = new FileWriter(file);
		fileList = Arrays.asList(file);
		units = compileFileManager.getJavaFileObjectsFromFiles(fileList);
	}

	public void setCode(String code) throws IOException
	{
		fileWriter.write(code);
		fileWriter.close();
	}

	public void writeCode(String code) throws IOException
	{
		fileWriter.write(code);
	}

	public void closeCodeWriter() throws IOException
	{
		fileWriter.close();
	}

	public boolean compile() throws IOException
	{
		boolean isCodeOK = isCodeOK();
		if(isCodeOK)
		{
			compiler.getTask(null, null, null, null, null, units).call();

			diagnosticsFileManager.close();
			compileFileManager.close();
		}

		return isCodeOK;
	}

	public boolean isCodeOK()
	{
		diagnosticsFileManager = compiler.getStandardFileManager(diagnostics, null, null);
		compiler.getTask(null, diagnosticsFileManager, diagnostics, null, null, units).call();

		boolean codeOK = true;

		for (Diagnostic<?> diagnostic : diagnostics.getDiagnostics())
		{
			System.err.println("Error on line " + diagnostic.getLineNumber() + " in " + diagnostic.getSource());
			System.err.println(diagnostic.getMessage(Locale.GERMANY));
			codeOK = false;
		}
		
		return codeOK;
	}

	public List<Diagnostic<? extends JavaFileObject>> getErrors()
	{
		return diagnostics.getDiagnostics();
	}

	public Class<?> getCompiledClass() throws ClassNotFoundException
	{
		//ClassLoader classLoader = ClassLoader.getSystemClassLoader();
		//return classLoader.loadClass(file.getAbsolutePath().replace(".java", ".class"));
		
		URLClassLoader urlClassLoader;
		try {
			urlClassLoader = new URLClassLoader(new URL[] { new URL("file:" + file.getAbsolutePath().replace(".java", ".class")) });
			Class<?> res = urlClassLoader.loadClass("Test");
			urlClassLoader.close();
			return res;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		return null;
	}

	public Object invokeMethod(String methodName, Class<?>[] parameterTypes, Object[] args) throws ClassNotFoundException, InstantiationException, IllegalAccessException, SecurityException, NoSuchMethodException, IllegalArgumentException, InvocationTargetException
	{
		if (parameterTypes == null)
		{
			parameterTypes = new Class[0];
		}

		if (args == null)
		{
			args = new Object[0];
		}

		Class<?> c = getCompiledClass();
		Object classInstance = c.newInstance();

		Method method = c.getMethod(methodName, parameterTypes);

		return method.invoke(classInstance, args);

	}

	public Method[] getMethods() throws SecurityException, ClassNotFoundException
	{
		return getCompiledClass().getMethods();
	}
	
	public static Object invokeCommand(String command)
	{
		try
		{
			ClassFactory classFactory = new ClassFactory("Test.java");
			StringBuilder strB = new StringBuilder();
			strB.append("public class Test{public void run(){");
			strB.append(command);
			strB.append(";}}");
			
			classFactory.setCode(strB.toString());
			boolean compileSucces = classFactory.compile();
			
			if(compileSucces)
			{
				return classFactory.invokeMethod("run", null, null);
			}
			
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		return null;
	}
	
	public static void test()
	{
		try
		{
			ClassFactory classFactory = new ClassFactory("Test.java");
			classFactory.setCode("public class Test{public void printHello(){System.err.println(\"Hello World!\");}}");
			
			boolean compileSucces = classFactory.compile();
			
			if(compileSucces)
			{
				classFactory.invokeMethod("printHello", null, null);
			}
			
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}		
	}
}

