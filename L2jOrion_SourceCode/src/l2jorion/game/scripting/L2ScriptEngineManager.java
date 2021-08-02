/*
 * L2jOrion Project - www.l2jorion.com 
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307, USA.
 *
 * http://www.gnu.org/copyleft/gpl.html
 */
package l2jorion.game.scripting;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleScriptContext;

import l2jorion.Config;
import l2jorion.game.script.jython.JythonScriptEngine;

public final class L2ScriptEngineManager
{
	private static final Logger _log = Logger.getLogger(L2ScriptEngineManager.class.getName());
	
	public static final File SCRIPT_FOLDER = new File(Config.DATAPACK_ROOT.getAbsolutePath(), "data/scripts");
	
	public static L2ScriptEngineManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private final Map<String, ScriptEngine> _nameEngines = new HashMap<>();
	private final Map<String, ScriptEngine> _extEngines = new HashMap<>();
	private final List<ScriptManager<?>> _scriptManagers = new LinkedList<>();
	
	private File _currentLoadingScript;
	
	private static final boolean VERBOSE_LOADING = Config.SCRIPT_DEBUG;
	
	private static final boolean ATTEMPT_COMPILATION = Config.SCRIPT_ALLOW_COMPILATION;
	
	private static final boolean PURGE_ERROR_LOG = Config.SCRIPT_ERROR_LOG;
	
	protected L2ScriptEngineManager()
	{
		ScriptEngineManager scriptEngineManager = new ScriptEngineManager();
		List<ScriptEngineFactory> factories = scriptEngineManager.getEngineFactories();
		
		for (ScriptEngineFactory factory : factories)
		{
			try
			{
				ScriptEngine engine = factory.getScriptEngine();
				boolean reg = false;
				for (String name : factory.getNames())
				{
					ScriptEngine existentEngine = _nameEngines.get(name);
					
					if (existentEngine != null)
					{
						double engineVer = Double.parseDouble(factory.getEngineVersion());
						double existentEngVer = Double.parseDouble(existentEngine.getFactory().getEngineVersion());
						
						if (engineVer <= existentEngVer)
						{
							continue;
						}
					}
					
					reg = false;
					_nameEngines.put(name, engine);
				}
				
				if (reg)
				{
					_log.info("Script Engine: " + factory.getEngineName() + " " + factory.getEngineVersion() + " - Language: " + factory.getLanguageName() + " - Language Version: " + factory.getLanguageVersion());
				}
				
				for (String ext : factory.getExtensions())
				{
					if (!ext.equals("java") || factory.getLanguageName().equals("java"))
					{
						_extEngines.put(ext, engine);
					}
				}
			}
			catch (Exception e)
			{
				_log.log(Level.WARNING, "Failed initializing factory: " + e.getMessage(), e);
			}
		}
		
		preConfigure();
	}
	
	private void preConfigure()
	{
		// Jython sys.path
		String dataPackDirForwardSlashes = SCRIPT_FOLDER.getPath().replaceAll("\\\\", "/");
		String configScript = "import sys;sys.path.insert(0,'" + dataPackDirForwardSlashes + "');";
		try
		{
			eval("jython", configScript);
		}
		catch (ScriptException e)
		{
			_log.severe("Failed preconfiguring jython: " + e.getMessage());
		}
	}
	
	private ScriptEngine getEngineByName(String name)
	{
		return _nameEngines.get(name);
	}
	
	private ScriptEngine getEngineByExtension(String ext)
	{
		return _extEngines.get(ext);
	}
	
	public void executeScriptList(File list) throws IOException
	{
		if (Config.ALT_DEV_NO_QUESTS)
		{
			return;
		}
		
		if (list.isFile())
		{
			try (FileInputStream fis = new FileInputStream(list);
				InputStreamReader isr = new InputStreamReader(fis);
				LineNumberReader lnr = new LineNumberReader(isr))
			{
				String line;
				while ((line = lnr.readLine()) != null)
				{
					String[] parts = line.trim().split("#");
					
					if ((parts.length > 0) && !parts[0].isEmpty() && (parts[0].charAt(0) != '#'))
					{
						line = parts[0];
						
						if (line.endsWith("/**"))
						{
							line = line.substring(0, line.length() - 3);
						}
						else if (line.endsWith("/*"))
						{
							line = line.substring(0, line.length() - 2);
						}
						
						File file = new File(SCRIPT_FOLDER, line);
						
						if (file.isDirectory() && parts[0].endsWith("/**"))
						{
							executeAllScriptsInDirectory(file, true, 32);
						}
						else if (file.isDirectory() && parts[0].endsWith("/*"))
						{
							executeAllScriptsInDirectory(file);
						}
						else if (file.isFile())
						{
							try
							{
								executeScript(file);
							}
							catch (ScriptException e)
							{
								reportScriptFileError(file, e);
							}
						}
						else
						{
							_log.warning("Failed loading: (" + file.getCanonicalPath() + ") @ " + list.getName() + ":" + lnr.getLineNumber() + " - Reason: doesnt exists or is not a file.");
						}
					}
				}
			}
		}
		else
		{
			throw new IllegalArgumentException("Argument must be an file containing a list of scripts to be loaded");
		}
	}
	
	public void executeAllScriptsInDirectory(File dir)
	{
		executeAllScriptsInDirectory(dir, false, 0);
	}
	
	public void executeAllScriptsInDirectory(File dir, boolean recurseDown, int maxDepth)
	{
		executeAllScriptsInDirectory(dir, recurseDown, maxDepth, 0);
	}
	
	private void executeAllScriptsInDirectory(File dir, boolean recurseDown, int maxDepth, int currentDepth)
	{
		if (dir.isDirectory())
		{
			final File[] files = dir.listFiles();
			if (files == null)
			{
				return;
			}
			
			for (File file : files)
			{
				if (file.isDirectory() && recurseDown && (maxDepth > currentDepth))
				{
					if (VERBOSE_LOADING)
					{
						_log.info("Entering folder: " + file.getName());
					}
					executeAllScriptsInDirectory(file, recurseDown, maxDepth, currentDepth + 1);
				}
				else if (file.isFile())
				{
					try
					{
						String name = file.getName();
						int lastIndex = name.lastIndexOf('.');
						String extension;
						if (lastIndex != -1)
						{
							extension = name.substring(lastIndex + 1);
							ScriptEngine engine = getEngineByExtension(extension);
							if (engine != null)
							{
								executeScript(engine, file);
							}
						}
					}
					catch (ScriptException e)
					{
						reportScriptFileError(file, e);
					}
				}
			}
		}
		else
		{
			throw new IllegalArgumentException("The argument directory either doesnt exists or is not an directory.");
		}
	}
	
	public void executeScript(File file) throws ScriptException
	{
		String name = file.getName();
		int lastIndex = name.lastIndexOf('.');
		String extension;
		if (lastIndex != -1)
		{
			extension = name.substring(lastIndex + 1);
		}
		else
		{
			throw new ScriptException("Script file (" + name + ") doesnt has an extension that identifies the ScriptEngine to be used.");
		}
		
		ScriptEngine engine = getEngineByExtension(extension);
		if (engine == null)
		{
			throw new ScriptException("No engine registered for extension (" + extension + ")");
		}
		executeScript(engine, file);
	}
	
	public void executeScript(String engineName, File file) throws ScriptException
	{
		ScriptEngine engine = getEngineByName(engineName);
		if (engine == null)
		{
			throw new ScriptException("No engine registered with name (" + engineName + ")");
		}
		executeScript(engine, file);
	}
	
	public void executeScript(ScriptEngine engine, File file) throws ScriptException
	{
		if (VERBOSE_LOADING)
		{
			_log.info("Loading Script: " + file.getAbsolutePath());
		}
		
		if (PURGE_ERROR_LOG)
		{
			String name = file.getAbsolutePath() + ".error.log";
			File errorLog = new File(name);
			if (errorLog.isFile())
			{
				errorLog.delete();
			}
		}
		
		final String relativeName = file.getAbsolutePath().substring(SCRIPT_FOLDER.getAbsolutePath().length() + 1).replace('\\', '/');
		try (FileInputStream fis = new FileInputStream(file);
			InputStreamReader isr = new InputStreamReader(fis);
			BufferedReader reader = new BufferedReader(isr))
		{
			if ((engine instanceof Compilable) && ATTEMPT_COMPILATION)
			{
				ScriptContext context = new SimpleScriptContext();
				context.setAttribute("mainClass", getClassForFile(file).replace('/', '.').replace('\\', '.'), ScriptContext.ENGINE_SCOPE);
				context.setAttribute(ScriptEngine.FILENAME, relativeName, ScriptContext.ENGINE_SCOPE);
				context.setAttribute("classpath", SCRIPT_FOLDER.getAbsolutePath(), ScriptContext.ENGINE_SCOPE);
				context.setAttribute("sourcepath", SCRIPT_FOLDER.getAbsolutePath(), ScriptContext.ENGINE_SCOPE);
				context.setAttribute(JythonScriptEngine.JYTHON_ENGINE_INSTANCE, engine, ScriptContext.ENGINE_SCOPE);
				
				setCurrentLoadingScript(file);
				ScriptContext ctx = engine.getContext();
				try
				{
					engine.setContext(context);
					Compilable eng = (Compilable) engine;
					CompiledScript cs = eng.compile(reader);
					cs.eval(context);
				}
				finally
				{
					engine.setContext(ctx);
					setCurrentLoadingScript(null);
					context.removeAttribute(ScriptEngine.FILENAME, ScriptContext.ENGINE_SCOPE);
					context.removeAttribute("mainClass", ScriptContext.ENGINE_SCOPE);
				}
			}
			else
			{
				ScriptContext context = new SimpleScriptContext();
				context.setAttribute("mainClass", getClassForFile(file).replace('/', '.').replace('\\', '.'), ScriptContext.ENGINE_SCOPE);
				context.setAttribute(ScriptEngine.FILENAME, relativeName, ScriptContext.ENGINE_SCOPE);
				context.setAttribute("classpath", SCRIPT_FOLDER.getAbsolutePath(), ScriptContext.ENGINE_SCOPE);
				context.setAttribute("sourcepath", SCRIPT_FOLDER.getAbsolutePath(), ScriptContext.ENGINE_SCOPE);
				setCurrentLoadingScript(file);
				try
				{
					engine.eval(reader, context);
				}
				finally
				{
					setCurrentLoadingScript(null);
					engine.getContext().removeAttribute(ScriptEngine.FILENAME, ScriptContext.ENGINE_SCOPE);
					engine.getContext().removeAttribute("mainClass", ScriptContext.ENGINE_SCOPE);
				}
				
			}
		}
		catch (IOException e)
		{
			_log.log(Level.WARNING, "Error executing script!", e);
		}
	}
	
	public static String getClassForFile(File script)
	{
		String path = script.getAbsolutePath();
		String scpPath = SCRIPT_FOLDER.getAbsolutePath();
		if (path.startsWith(scpPath))
		{
			int idx = path.lastIndexOf('.');
			return path.substring(scpPath.length() + 1, idx);
		}
		return null;
	}
	
	public ScriptContext getScriptContext(ScriptEngine engine)
	{
		return engine.getContext();
	}
	
	public ScriptContext getScriptContext(String engineName)
	{
		ScriptEngine engine = getEngineByName(engineName);
		if (engine == null)
		{
			throw new IllegalStateException("No engine registered with name (" + engineName + ")");
		}
		return getScriptContext(engine);
	}
	
	public Object eval(ScriptEngine engine, String script, ScriptContext context) throws ScriptException
	{
		if ((engine instanceof Compilable) && ATTEMPT_COMPILATION)
		{
			Compilable eng = (Compilable) engine;
			CompiledScript cs = eng.compile(script);
			return context != null ? cs.eval(context) : cs.eval();
		}
		return context != null ? engine.eval(script, context) : engine.eval(script);
	}
	
	public Object eval(String engineName, String script) throws ScriptException
	{
		return eval(engineName, script, null);
	}
	
	public Object eval(String engineName, String script, ScriptContext context) throws ScriptException
	{
		ScriptEngine engine = getEngineByName(engineName);
		if (engine == null)
		{
			throw new ScriptException("No engine registered with name (" + engineName + ")");
		}
		return eval(engine, script, context);
	}
	
	public Object eval(ScriptEngine engine, String script) throws ScriptException
	{
		return eval(engine, script, null);
	}
	
	public void reportScriptFileError(File script, ScriptException e)
	{
		String dir = script.getParent();
		String name = script.getName() + ".error.log";
		if (dir != null)
		{
			final File file = new File(dir + "/" + name);
			try (FileOutputStream fos = new FileOutputStream(file))
			{
				String errorHeader = "Error on: " + file.getCanonicalPath() + Config.EOL + "Line: " + e.getLineNumber() + " - Column: " + e.getColumnNumber() + Config.EOL + Config.EOL;
				fos.write(errorHeader.getBytes());
				fos.write(e.getMessage().getBytes());
				_log.warning("Failed executing script: " + script.getAbsolutePath() + ". See " + file.getName() + " for details.");
			}
			catch (IOException ioe)
			{
				_log.log(Level.WARNING, "Failed executing script: " + script.getAbsolutePath() + Config.EOL + e.getMessage() + "Additionally failed when trying to write an error report on script directory. Reason: " + ioe.getMessage(), ioe);
			}
		}
		else
		{
			_log.log(Level.WARNING, "Failed executing script: " + script.getAbsolutePath() + Config.EOL + e.getMessage() + "Additionally failed when trying to write an error report on script directory.", e);
		}
	}
	
	public void registerScriptManager(ScriptManager<?> manager)
	{
		_scriptManagers.add(manager);
	}
	
	public void removeScriptManager(ScriptManager<?> manager)
	{
		_scriptManagers.remove(manager);
	}
	
	public List<ScriptManager<?>> getScriptManagers()
	{
		return _scriptManagers;
		
	}
	
	/**
	 * @param currentLoadingScript The currentLoadingScript to set.
	 */
	protected void setCurrentLoadingScript(File currentLoadingScript)
	{
		_currentLoadingScript = currentLoadingScript;
	}
	
	/**
	 * @return Returns the currentLoadingScript.
	 */
	public File getCurrentLoadingScript()
	{
		return _currentLoadingScript;
	}
	
	private static class SingletonHolder
	{
		protected static final L2ScriptEngineManager _instance = new L2ScriptEngineManager();
	}
}
