/*
 * Copyright 2006 Sun Microsystems, Inc. All rights reserved. 
 * Use is subject to license terms.
 *
 * Redistribution and use in source and binary forms, with or without modification, are 
 * permitted provided that the following conditions are met: Redistributions of source code 
 * must retain the above copyright notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright notice, this list of 
 * conditions and the following disclaimer in the documentation and/or other materials 
 * provided with the distribution. Neither the name of the Sun Microsystems nor the names of 
 * is contributors may be used to endorse or promote products derived from this software 
 * without specific prior written permission. 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS
 * OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY 
 * AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER 
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR 
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR 
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON 
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package l2jorion.game.script.jython;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Reader;
import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import javax.script.AbstractScriptEngine;
import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.Invocable;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptException;
import javax.script.SimpleBindings;

import org.python.compiler.Module;
import org.python.core.BytecodeLoader;
import org.python.core.Py;
import org.python.core.PyCode;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.core.PySystemState;
import org.python.core.__builtin__;
import org.python.core.parser;

public class JythonScriptEngine extends AbstractScriptEngine implements Compilable, Invocable
{
	// my factory, may be null
	private ScriptEngineFactory factory;
	
	// my scope -- associated with the default context
	private PyObject myScope;
	
	public static final String JYTHON_COMPILE_MODE = "com.sun.script.jython.comp.mode";
	public static final String JYTHON_ENGINE_INSTANCE = "l2jorion.game.script.jython";
	
	private static ThreadLocal<PySystemState> systemState;
	
	static
	{
		PySystemState.initialize();
		systemState = new ThreadLocal<>();
	}
	
	public JythonScriptEngine()
	{
		myScope = newScope(context);
	}
	
	// my implementation for CompiledScript
	private static class JythonCompiledScript extends CompiledScript implements Serializable
	{
		
		private static final long serialVersionUID = 1L;
		
		// my compiled code
		private transient PyCode code;
		private transient JythonScriptEngine _engine;
		private final String _name;
		private final byte _data[];
		private final String _filename;
		
		JythonCompiledScript(JythonScriptEngine engine, String name, byte data[], String filename)
		{
			_engine = engine;
			_name = name;
			_data = data;
			_filename = filename;
			code = BytecodeLoader.makeCode(name, data, filename);
		}
		
		@Override
		public ScriptEngine getEngine()
		{
			return _engine;
		}
		
		@Override
		public Object eval(ScriptContext ctx) throws ScriptException
		{
			if (code == null)
			{
				code = BytecodeLoader.makeCode(_name, _data, _filename);
			}
			if (_engine == null)
			{
				_engine = (JythonScriptEngine) ctx.getAttribute(JYTHON_ENGINE_INSTANCE);
				if (_engine == null)
				{
					throw new IllegalStateException("Cannot eval an deserialized ScriptContext without passing an jython engine instance through the ScriptContext");
				}
			}
			return _engine.evalCode(code, ctx);
		}
	}
	
	// Compilable methods
	@Override
	public CompiledScript compile(String script) throws ScriptException
	{
		return compileSerializableScript(script, context);
	}
	
	@Override
	public CompiledScript compile(Reader reader) throws ScriptException
	{
		return compile(readFully(reader));
	}
	
	// Invocable methods
	@Override
	public Object invokeFunction(String name, Object... args) throws NoSuchMethodException
	{
		return invokeImpl(null, name, args);
	}
	
	@Override
	public Object invokeMethod(Object obj, String name, Object... args) throws NoSuchMethodException
	{
		if (obj == null)
		{
			throw new IllegalArgumentException("script object is null");
		}
		return invokeImpl(obj, name, args);
	}
	
	protected Object invokeImpl(Object obj, String name, Object... args) throws NoSuchMethodException
	{
		if (name == null)
		{
			throw new NullPointerException("method name is null");
		}
		
		setSystemState();
		
		PyObject thiz;
		if (obj instanceof PyObject)
		{
			thiz = (PyObject) obj;
		}
		else if (obj == null)
		{
			thiz = myScope;
		}
		else
		{
			thiz = java2py(obj);
		}
		
		PyObject func = thiz.__findattr__(name);
		if (((func == null) || !func.isCallable()) && (thiz == myScope))
		{
			systemState.get();
			// lookup in built-in functions. This way
			// user can call invoke built-in functions.
			PyObject builtins = PySystemState.builtins;
			func = builtins.__finditem__(name);
		}
		
		if ((func == null) || !func.isCallable())
		{
			throw new NoSuchMethodException(name);
		}
		
		PyObject res = func.__call__(wrapArguments(args));
		return py2java(res);
	}
	
	@Override
	public <T> T getInterface(Object obj, Class<T> clazz)
	{
		if (obj == null)
		{
			throw new IllegalArgumentException("script object is null");
		}
		return makeInterface(obj, clazz);
	}
	
	@Override
	public <T> T getInterface(Class<T> clazz)
	{
		return makeInterface(null, clazz);
	}
	
	@SuppressWarnings("unchecked")
	private <T> T makeInterface(Object obj, Class<T> clazz)
	{
		if ((clazz == null) || !clazz.isInterface())
		{
			throw new IllegalArgumentException("interface Class expected");
		}
		
		final Object thiz = obj;
		return (T) Proxy.newProxyInstance(clazz.getClassLoader(), new Class[]
		{
			clazz
		}, new InvocationHandler()
		{
			@Override
			public Object invoke(Object proxy, Method m, Object args[]) throws Throwable
			{
				Object res = invokeImpl(thiz, m.getName(), args);
				return py2java(java2py(res), m.getReturnType());
			}
		});
	}
	
	@Override
	public Object eval(String str, ScriptContext ctx) throws ScriptException
	{
		PyCode code = compileScript(str, ctx);
		return evalCode(code, ctx);
	}
	
	@Override
	public Object eval(Reader reader, ScriptContext ctx) throws ScriptException
	{
		return eval(readFully(reader), ctx);
	}
	
	@Override
	public ScriptEngineFactory getFactory()
	{
		synchronized (this)
		{
			if (factory == null)
			{
				factory = new JythonScriptEngineFactory();
			}
		}
		return factory;
	}
	
	@Override
	public Bindings createBindings()
	{
		return new SimpleBindings();
	}
	
	@Override
	public void setContext(ScriptContext ctx)
	{
		super.setContext(ctx);
		// update myScope to keep it in-sync
		myScope = newScope(context);
	}
	
	// package-private methods
	void setFactory(ScriptEngineFactory factory)
	{
		this.factory = factory;
	}
	
	static PyObject java2py(Object javaObj)
	{
		return Py.java2py(javaObj);
	}
	
	static Object py2java(PyObject pyObj, Class<?> type)
	{
		return (pyObj == null) ? null : pyObj.__tojava__(type);
	}
	
	static Object py2java(PyObject pyObj)
	{
		return py2java(pyObj, Object.class);
	}
	
	static PyObject[] wrapArguments(Object args[])
	{
		if (args == null)
		{
			return new PyObject[0];
		}
		
		PyObject res[] = new PyObject[args.length];
		for (int i = 0; i < args.length; i++)
		{
			res[i] = java2py(args[i]);
		}
		
		return res;
	}
	
	// internals only below this point
	private PyObject getJythonScope(ScriptContext ctx)
	{
		if (ctx == context)
		{
			return myScope;
		}
		return newScope(ctx);
	}
	
	private PyObject newScope(ScriptContext ctx)
	{
		return new JythonScope(this, ctx);
	}
	
	private void setSystemState()
	{
		if (systemState.get() == null)
		{
			// we entering into this thread for the first time.
			PySystemState newState = new PySystemState();
			ClassLoader cl = Thread.currentThread().getContextClassLoader();
			newState.setClassLoader(cl);
			systemState.set(newState);
			Py.setSystemState(newState);
		}
	}
	
	private PyCode compileScript(String script, ScriptContext ctx) throws ScriptException
	{
		try
		{
			setSystemState();
			
			String fileName = (String) ctx.getAttribute(ScriptEngine.FILENAME);
			if (fileName == null)
			{
				fileName = "<unknown>";
			}
			
			/*
			 * Jython parser seems to have 3 input modes (called compile "kind") These are "single", "eval" and "exec". I don't clearly understand the difference. But, with "eval" and "exec" certain features are not working. For eg. with "eval" assignments are not working. I've used "exec". But,
			 * that is customizable by special attribute.
			 */
			String mode = (String) ctx.getAttribute(JYTHON_COMPILE_MODE);
			if (mode == null)
			{
				mode = "exec";
			}
			
			return __builtin__.compile(script, fileName, mode);
		}
		catch (Exception exp)
		{
			throw new ScriptException(exp);
		}
	}
	
	private JythonCompiledScript compileSerializableScript(String script, ScriptContext ctx) throws ScriptException
	{
		try
		{
			setSystemState();
			
			String fileName = (String) ctx.getAttribute(ScriptEngine.FILENAME);
			if (fileName == null)
			{
				fileName = "<unknown>";
			}
			
			/*
			 * Jython parser seems to have 3 input modes (called compile "kind") These are "single", "eval" and "exec". I don't clearly understand the difference. But, with "eval" and "exec" certain features are not working. For eg. with "eval" assignments are not working. I've used "exec". But,
			 * that is customizable by special attribute.
			 */
			String mode = (String) ctx.getAttribute(JYTHON_COMPILE_MODE);
			if (mode == null)
			{
				mode = "exec";
			}
			
			org.python.parser.ast.modType node = parser.parse(new ByteArrayInputStream(PyString.to_bytes(script + "\n\n")), mode, fileName, Py.getCompilerFlags());
			ByteArrayOutputStream ostream = new ByteArrayOutputStream();
			String name = "org.python.pycode.serializable._pyx" + System.currentTimeMillis();
			Module.compile(node, ostream, name, fileName, true, false, false, Py.getCompilerFlags());
			return new JythonCompiledScript(this, name, ostream.toByteArray(), fileName);
		}
		catch (Exception exp)
		{
			throw new ScriptException(exp);
		}
		
	}
	
	protected Object evalCode(PyCode code, ScriptContext ctx) throws ScriptException
	{
		try
		{
			PyObject res;
			setSystemState();
			PyObject scope = getJythonScope(ctx);
			res = Py.runCode(code, scope, scope);
			return res.__tojava__(Object.class);
		}
		catch (Exception exp)
		{
			throw new ScriptException(exp);
		}
	}
	
	private String readFully(Reader reader) throws ScriptException
	{
		char arr[] = new char[8 * 1024]; // 8K at a time
		StringBuilder buf = new StringBuilder();
		int numChars;
		try
		{
			while ((numChars = reader.read(arr, 0, arr.length)) > 0)
			{
				buf.append(arr, 0, numChars);
			}
		}
		catch (IOException exp)
		{
			throw new ScriptException(exp);
		}
		return buf.toString();
	}
}
