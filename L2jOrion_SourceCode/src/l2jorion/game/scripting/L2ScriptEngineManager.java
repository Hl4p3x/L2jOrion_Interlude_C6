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
import java.io.InvalidClassException;
import java.io.LineNumberReader;
import java.io.ObjectInputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleScriptContext;

import javolution.util.FastMap;

import l2jorion.Config;
import l2jorion.game.script.jython.JythonScriptEngine;
import l2jorion.game.thread.ThreadPoolManager;
import l2jorion.util.Util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class L2ScriptEngineManager
{
	private static final Logger LOG = LoggerFactory.getLogger(L2ScriptEngineManager.class);
	
	public final static File SCRIPT_FOLDER = new File(Config.DATAPACK_ROOT.getAbsolutePath(), "data/scripts");
	
	private final Map<String, ScriptEngine> _nameEngines = new FastMap<>();
	private final Map<String, ScriptEngine> _extEngines = new FastMap<>();
	private final List<ScriptManager<?>> _scriptManagers = new LinkedList<>();
	
	private final CompiledScriptCache _cache;
	
	private File _currentLoadingScript;
	
	protected L2ScriptEngineManager()
	{
		final ScriptEngineManager scriptEngineManager = new ScriptEngineManager();
		final List<ScriptEngineFactory> factories = scriptEngineManager.getEngineFactories();
		
		if (Config.SCRIPT_CACHE)
		{
			_cache = loadCompiledScriptCache();
		}
		else
		{
			_cache = null;
		}
		
		ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new CleaneCache(), 43200000, 43200000);
		
		Util.printSection("Initializing Script Engine Manager");
		
		for (final ScriptEngineFactory factory : factories)
		{
			try
			{
				final ScriptEngine engine = factory.getScriptEngine();
				boolean reg = false;
				for (final String name : factory.getNames())
				{
					final ScriptEngine existentEngine = _nameEngines.get(name);
					
					if (existentEngine != null)
					{
						final double engineVer = Double.parseDouble(factory.getEngineVersion());
						final double existentEngVer = Double.parseDouble(existentEngine.getFactory().getEngineVersion());
						
						if (engineVer <= existentEngVer)
						{
							continue;
						}
					}
					
					reg = true;
					_nameEngines.put(name, engine);
				}
				
				if (reg)
				{
					LOG.info("Script Engine: " + factory.getEngineName() + " " + factory.getEngineVersion() + " - Language: " + factory.getLanguageName() + " - Language Version: " + factory.getLanguageVersion());
				}
				
				for (final String ext : factory.getExtensions())
				{
					if (!ext.equals("java") || factory.getLanguageName().equals("java"))
					{
						_extEngines.put(ext, engine);
					}
				}
			}
			catch (final Exception e)
			{
				LOG.warn("Failed initializing factory. ");
				e.printStackTrace();
			}
		}
		
		preConfigure();
	}
	
	private void preConfigure()
	{
		// Jython sys.path
		final String dataPackDirForwardSlashes = SCRIPT_FOLDER.getPath().replaceAll("\\\\", "/");
		final String configScript = "import sys;sys.path.insert(0,'" + dataPackDirForwardSlashes + "');";
		try
		{
			this.eval("jython", configScript);
		}
		catch (final ScriptException e)
		{
			LOG.error("Failed preconfiguring jython", e);
		}
	}
	
	private ScriptEngine getEngineByName(final String name)
	{
		return _nameEngines.get(name);
	}
	
	private ScriptEngine getEngineByExtension(final String ext)
	{
		return _extEngines.get(ext);
	}
	
	public void executeScriptsList(final File list) throws IllegalArgumentException
	{
		if (list.isFile())
		{
			FileInputStream reader = null;
			InputStreamReader buff = null;
			LineNumberReader lnr = null;
			
			try
			{
				reader = new FileInputStream(list);
				buff = new InputStreamReader(reader);
				lnr = new LineNumberReader(buff);
				
				String line;
				File file;
				
				while ((line = lnr.readLine()) != null)
				{
					final String[] parts = line.trim().split("#");
					
					if (parts.length > 0 && !parts[0].startsWith("#") && parts[0].length() > 0)
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
						
						file = new File(SCRIPT_FOLDER, line);
						
						if (file.isDirectory() && parts[0].endsWith("/**"))
						{
							this.executeAllScriptsInDirectory(file, true, 32);
						}
						else if (file.isDirectory() && parts[0].endsWith("/*"))
						{
							this.executeAllScriptsInDirectory(file);
						}
						else if (file.isFile())
						{
							try
							{
								this.executeScript(file);
							}
							catch (final ScriptException e)
							{
								if (Config.ENABLE_ALL_EXCEPTIONS)
									e.printStackTrace();
								
								reportScriptFileError(file, e);
							}
						}
						else
						{
							LOG.warn("Failed loading: (" + file.getCanonicalPath() + ") @ " + list.getName() + ":" + lnr.getLineNumber() + " - Reason: doesnt exists or is not a file.");
						}
					}
				}
				
			}
			catch (final IOException e)
			{
				e.printStackTrace();
				
			}
			finally
			{
				if (lnr != null)
					try
					{
						lnr.close();
					}
					catch (final Exception e1)
					{
						e1.printStackTrace();
					}
				
				if (buff != null)
					try
					{
						buff.close();
					}
					catch (final Exception e1)
					{
						e1.printStackTrace();
					}
				
				if (reader != null)
					try
					{
						reader.close();
					}
					catch (final Exception e1)
					{
						e1.printStackTrace();
					}
				
			}
			
		}
		else
			throw new IllegalArgumentException("Argument must be an file containing a list of scripts to be loaded");
	}
	
	public void executeAllScriptsInDirectory(final File dir)
	{
		this.executeAllScriptsInDirectory(dir, false, 0);
	}
	
	public void executeAllScriptsInDirectory(final File dir, final boolean recurseDown, final int maxDepth)
	{
		this.executeAllScriptsInDirectory(dir, recurseDown, maxDepth, 0);
	}
	
	private void executeAllScriptsInDirectory(final File dir, final boolean recurseDown, final int maxDepth, final int currentDepth)
	{
		if (dir.isDirectory())
		{
			for (final File file : dir.listFiles())
			{
				if (file.isDirectory() && recurseDown && maxDepth > currentDepth)
				{
					if (Config.SCRIPT_DEBUG)
					{
						LOG.info("Entering folder: " + file.getName());
					}
					this.executeAllScriptsInDirectory(file, recurseDown, maxDepth, currentDepth + 1);
				}
				else if (file.isFile())
				{
					try
					{
						final String name = file.getName();
						final int lastIndex = name.lastIndexOf('.');
						String extension;
						if (lastIndex != -1)
						{
							extension = name.substring(lastIndex + 1);
							final ScriptEngine engine = getEngineByExtension(extension);
							if (engine != null)
							{
								this.executeScript(engine, file);
							}
						}
					}
					catch (final ScriptException e)
					{
						if (Config.ENABLE_ALL_EXCEPTIONS)
							e.printStackTrace();
						
						reportScriptFileError(file, e);
						// e.printStackTrace();
					}
				}
			}
		}
		else
			throw new IllegalArgumentException("The argument directory either doesnt exists or is not an directory.");
	}
	
	public CompiledScriptCache getCompiledScriptCache()
	{
		return _cache;
	}
	
	public CompiledScriptCache loadCompiledScriptCache()
	{
		CompiledScriptCache cache = null;
		
		if (Config.SCRIPT_CACHE)
		{
			final File file = new File(SCRIPT_FOLDER, "CompiledScripts.cache");
			if (file.isFile())
			{
				FileInputStream fis = null;
				ObjectInputStream ois = null;
				try
				{
					fis = new FileInputStream(file);
					ois = new ObjectInputStream(fis);
					cache = (CompiledScriptCache) ois.readObject();
				}
				catch (final InvalidClassException e)
				{
					if (Config.ENABLE_ALL_EXCEPTIONS)
						e.printStackTrace();
					LOG.error("Failed loading Compiled Scripts Cache, invalid class (Possibly outdated).", e);
				}
				catch (final IOException e)
				{
					if (Config.ENABLE_ALL_EXCEPTIONS)
						e.printStackTrace();
					LOG.error("Failed loading Compiled Scripts Cache from file.", e);
				}
				catch (final ClassNotFoundException e)
				{
					if (Config.ENABLE_ALL_EXCEPTIONS)
						e.printStackTrace();
					LOG.error("Failed loading Compiled Scripts Cache, class not found.", e);
				}
				finally
				{
					if (ois != null)
						try
						{
							ois.close();
						}
						catch (final Exception e)
						{
							e.printStackTrace();
						}
					if (fis != null)
						try
						{
							fis.close();
						}
						catch (final Exception e)
						{
							e.printStackTrace();
						}
				}
				
			}
			
			if (cache == null)
			{
				cache = new CompiledScriptCache();
			}
			
		}
		
		return cache;
	}
	
	protected class CleaneCache implements Runnable
	{
		@Override
		public void run()
		{
			
		}
	}
	
	public void executeScript(final File file) throws ScriptException
	{
		final String name = file.getName();
		final int lastIndex = name.lastIndexOf('.');
		String extension;
		if (lastIndex != -1)
		{
			extension = name.substring(lastIndex + 1);
		}
		else
			throw new ScriptException("Script file (" + name + ") doesnt has an extension that identifies the ScriptEngine to be used.");
		
		final ScriptEngine engine = getEngineByExtension(extension);
		if (engine == null)
			throw new ScriptException("No engine registered for extension (" + extension + ")");
		
		executeScript(engine, file);
	}
	
	public void executeScript(final String engineName, final File file) throws ScriptException
	{
		final ScriptEngine engine = getEngineByName(engineName);
		
		if (engine == null)
			throw new ScriptException("No engine registered with name (" + engineName + ")");
		
		executeScript(engine, file);
	}
	
	public void executeScript(final ScriptEngine engine, final File file) throws ScriptException
	{
		
		FileInputStream reader = null;
		InputStreamReader buff = null;
		BufferedReader lnr = null;
		
		try
		{
			reader = new FileInputStream(file);
			buff = new InputStreamReader(reader);
			lnr = new BufferedReader(buff);
			
			if (Config.SCRIPT_DEBUG)
			{
				LOG.info("Loading Script: " + file.getAbsolutePath());
			}
			
			if (Config.SCRIPT_ERROR_LOG)
			{
				final String name = file.getAbsolutePath() + ".error.LOGGER";
				final File errorLogger = new File(name);
				if (errorLogger.isFile())
				{
					errorLogger.delete();
				}
			}
			
			if (engine instanceof Compilable && Config.SCRIPT_ALLOW_COMPILATION)
			{
				final ScriptContext context = new SimpleScriptContext();
				
				context.setAttribute("mainClass", getClassForFile(file).replace('/', '.').replace('\\', '.'), ScriptContext.ENGINE_SCOPE);
				context.setAttribute(ScriptEngine.FILENAME, file.getName(), ScriptContext.ENGINE_SCOPE);
				context.setAttribute("classpath", SCRIPT_FOLDER.getAbsolutePath(), ScriptContext.ENGINE_SCOPE);
				context.setAttribute("sourcepath", SCRIPT_FOLDER.getAbsolutePath(), ScriptContext.ENGINE_SCOPE);
				context.setAttribute(JythonScriptEngine.JYTHON_ENGINE_INSTANCE, engine, ScriptContext.ENGINE_SCOPE);
				
				setCurrentLoadingScript(file);
				final ScriptContext ctx = engine.getContext();
				try
				{
					engine.setContext(context);
					if (Config.SCRIPT_CACHE)
					{
						final CompiledScript cs = _cache.loadCompiledScript(engine, file);
						cs.eval(context);
					}
					else
					{
						final Compilable eng = (Compilable) engine;
						final CompiledScript cs = eng.compile(lnr);
						cs.eval(context);
					}
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
				final ScriptContext context = new SimpleScriptContext();
				context.setAttribute("mainClass", getClassForFile(file).replace('/', '.').replace('\\', '.'), ScriptContext.ENGINE_SCOPE);
				context.setAttribute(ScriptEngine.FILENAME, file.getName(), ScriptContext.ENGINE_SCOPE);
				context.setAttribute("classpath", SCRIPT_FOLDER.getAbsolutePath(), ScriptContext.ENGINE_SCOPE);
				context.setAttribute("sourcepath", SCRIPT_FOLDER.getAbsolutePath(), ScriptContext.ENGINE_SCOPE);
				setCurrentLoadingScript(file);
				try
				{
					engine.eval(lnr, context);
				}
				finally
				{
					setCurrentLoadingScript(null);
					engine.getContext().removeAttribute(ScriptEngine.FILENAME, ScriptContext.ENGINE_SCOPE);
					engine.getContext().removeAttribute("mainClass", ScriptContext.ENGINE_SCOPE);
				}
				
			}
			
		}
		catch (final IOException e)
		{
			
			e.printStackTrace();
			
		}
		finally
		{
			if (lnr != null)
				try
				{
					lnr.close();
				}
				catch (final Exception e1)
				{
					e1.printStackTrace();
				}
			
			if (buff != null)
				try
				{
					buff.close();
				}
				catch (final Exception e1)
				{
					e1.printStackTrace();
				}
			
			if (reader != null)
				try
				{
					reader.close();
				}
				catch (final Exception e1)
				{
					e1.printStackTrace();
				}
			
		}
		
	}
	
	public static String getClassForFile(final File script)
	{
		final String path = script.getAbsolutePath();
		final String scpPath = SCRIPT_FOLDER.getAbsolutePath();
		if (path.startsWith(scpPath))
		{
			final int idx = path.lastIndexOf('.');
			return path.substring(scpPath.length() + 1, idx);
		}
		return null;
	}
	
	public ScriptContext getScriptContext(final ScriptEngine engine)
	{
		return engine.getContext();
	}
	
	public ScriptContext getScriptContext(final String engineName)
	{
		final ScriptEngine engine = getEngineByName(engineName);
		if (engine == null)
			throw new IllegalStateException("No engine registered with name (" + engineName + ")");
		return getScriptContext(engine);
	}
	
	public Object eval(final ScriptEngine engine, final String script, final ScriptContext context) throws ScriptException
	{
		if (engine instanceof Compilable && Config.SCRIPT_ALLOW_COMPILATION)
		{
			final Compilable eng = (Compilable) engine;
			final CompiledScript cs = eng.compile(script);
			return context != null ? cs.eval(context) : cs.eval();
		}
		return context != null ? engine.eval(script, context) : engine.eval(script);
	}
	
	public Object eval(final String engineName, final String script) throws ScriptException
	{
		return this.eval(engineName, script, null);
	}
	
	public Object eval(final String engineName, final String script, final ScriptContext context) throws ScriptException
	{
		final ScriptEngine engine = getEngineByName(engineName);
		if (engine == null)
			throw new ScriptException("No engine registered with name (" + engineName + ")");
		return eval(engine, script, context);
	}
	
	public Object eval(final ScriptEngine engine, final String script) throws ScriptException
	{
		return eval(engine, script, null);
	}
	
	public void reportScriptFileError(final File script, final ScriptException e)
	{
		final String dir = script.getParent();
		final String name = script.getName() + ".error.LOGGER";
		if (dir != null)
		{
			final File file = new File(dir + "/" + name);
			FileOutputStream fos = null;
			try
			{
				if (!file.exists())
				{
					file.createNewFile();
				}
				
				fos = new FileOutputStream(file);
				final String errorHeader = "Error on: " + file.getCanonicalPath() + "\r\nLine: " + e.getLineNumber() + " - Column: " + e.getColumnNumber() + "\r\n\r\n";
				fos.write(errorHeader.getBytes());
				fos.write(e.getMessage().getBytes());
				LOG.warn("Failed executing script: " + script.getAbsolutePath() + ". See " + file.getName() + " for details.");
			}
			catch (final IOException ioe)
			{
				LOG.warn("Failed executing script: " + script.getAbsolutePath() + "\r\n" + e.getMessage() + "Additionally failed when trying to write an error report on script directory. Reason: " + ioe.getMessage());
				ioe.printStackTrace();
			}
			finally
			{
				if (fos != null)
					try
					{
						fos.close();
					}
					catch (final Exception e1)
					{
						e1.printStackTrace();
					}
			}
		}
		else
		{
			LOG.warn("Failed executing script: " + script.getAbsolutePath() + "\r\n" + e.getMessage() + "Additionally failed when trying to write an error report on script directory.");
		}
	}
	
	public void registerScriptManager(final ScriptManager<?> manager)
	{
		_scriptManagers.add(manager);
	}
	
	public void removeScriptManager(final ScriptManager<?> manager)
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
	protected void setCurrentLoadingScript(final File currentLoadingScript)
	{
		_currentLoadingScript = currentLoadingScript;
	}
	
	/**
	 * @return Returns the currentLoadingScript.
	 */
	protected File getCurrentLoadScript()
	{
		return _currentLoadingScript;
	}
	
	public static L2ScriptEngineManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final L2ScriptEngineManager _instance = new L2ScriptEngineManager();
	}
}
