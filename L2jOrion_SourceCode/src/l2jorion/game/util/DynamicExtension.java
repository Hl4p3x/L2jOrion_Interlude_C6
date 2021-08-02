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

package l2jorion.game.util;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import l2jorion.Config;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;

public class DynamicExtension
{
	private static Logger LOG = LoggerFactory.getLogger(DynamicExtension.class.getCanonicalName());
	private JarClassLoader _classLoader;
	private static final String CONFIG = "config/other/extensions.ini";
	private Properties _prop;
	private ConcurrentHashMap<String, Object> _loadedExtensions;
	private static DynamicExtension _instance;
	private final ConcurrentHashMap<String, ExtensionFunction> _getters;
	private final ConcurrentHashMap<String, ExtensionFunction> _setters;
	
	/**
	 * create an instance of DynamicExtension this will be done by GameServer according to the altsettings.properties
	 */
	private DynamicExtension()
	{
		if (_instance == null)
		{
			_instance = this;
		}
		_getters = new ConcurrentHashMap<>();
		_setters = new ConcurrentHashMap<>();
		initExtensions();
	}
	
	/**
	 * get the singleton of DynamicInstance
	 * @return the singleton instance
	 */
	public static DynamicExtension getInstance()
	{
		if (_instance == null)
		{
			_instance = new DynamicExtension();
		}
		return _instance;
	}
	
	/**
	 * get an extension object by class name
	 * @param className he class name as defined in the extension properties
	 * @return the object or null if not found
	 */
	public Object getExtension(final String className)
	{
		return _loadedExtensions.get(className);
	}
	
	/**
	 * initialize all configured extensions
	 * @return
	 */
	public String initExtensions()
	{
		_prop = new Properties();
		String res = "";
		_loadedExtensions = new ConcurrentHashMap<>();
		
		FileInputStream fis = null;
		
		try
		{
			fis = new FileInputStream(CONFIG);
			_prop.load(fis);
		}
		catch (final FileNotFoundException ex)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				ex.printStackTrace();
			}
			
			LOG.info(ex.getMessage() + ": no extensions to load");
		}
		catch (final Exception ex)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				ex.printStackTrace();
			}
			
			LOG.warn("could not load properties", ex);
			
		}
		finally
		{
			
			if (fis != null)
			{
				try
				{
					fis.close();
				}
				catch (final IOException e)
				{
					e.printStackTrace();
				}
			}
		}
		
		_classLoader = new JarClassLoader();
		
		for (final Object o : _prop.keySet())
		{
			final String k = (String) o;
			
			if (k.endsWith("Class"))
			{
				res += initExtension(_prop.getProperty(k)) + "\n";
			}
		}
		return res;
	}
	
	/**
	 * init a named extension
	 * @param name the class name and optionally a jar file name delimited with a '@' if the jar file is not in the class path
	 * @return
	 */
	public String initExtension(final String name)
	{
		String className = name;
		final String[] p = name.split("@");
		String res = name + " loaded";
		
		if (p.length > 1)
		{
			_classLoader.addJarFile(p[1]);
			className = p[0];
		}
		
		if (_loadedExtensions.containsKey(className))
		{
			return "already loaded";
		}
		
		try
		{
			final Class<?> extension = Class.forName(className, true, _classLoader);
			final Object obj = extension;
			extension.getMethod("init", new Class[0]).invoke(obj, new Object[0]);
			LOG.info("Extension " + className + " loaded.");
			_loadedExtensions.put(className, obj);
		}
		catch (final Exception ex)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				ex.printStackTrace();
			}
			
			LOG.warn(name, ex);
			res = ex.toString();
		}
		return res;
	}
	
	/**
	 * create a new class loader which resets the cache (jar files and loaded classes) on next class loading request it will read the jar again
	 */
	protected void clearCache()
	{
		_classLoader = new JarClassLoader();
	}
	
	/**
	 * call unloadExtension() for all known extensions
	 * @return
	 */
	public String unloadExtensions()
	{
		String res = "";
		
		for (final String e : _loadedExtensions.keySet())
		{
			res += unloadExtension(e) + "\n";
		}
		return res;
	}
	
	/**
	 * get all loaded extensions
	 * @return a String array with the class names
	 */
	public String[] getExtensions()
	{
		final String[] l = new String[_loadedExtensions.size()];
		_loadedExtensions.keySet().toArray(l);
		return l;
	}
	
	/**
	 * unload a named extension
	 * @param name the class name and optionally a jar file name delimited with a '@'
	 * @return
	 */
	public String unloadExtension(final String name)
	{
		String className = name;
		final String[] p = name.split("@");
		
		if (p.length > 1)
		{
			_classLoader.addJarFile(p[1]);
			className = p[0];
		}
		
		String res = className + " unloaded";
		
		try
		{
			final Object obj = _loadedExtensions.get(className);
			final Class<?> extension = obj.getClass();
			_loadedExtensions.remove(className);
			extension.getMethod("unload", new Class[0]).invoke(obj, new Object[0]);
			LOG.info("Extension " + className + " unloaded.");
		}
		catch (final Exception ex)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				ex.printStackTrace();
			}
			
			LOG.warn("could not unload " + className, ex);
			res = ex.toString();
		}
		return res;
	}
	
	/**
	 * unloads all extensions, resets the cache and initializes all configured extensions
	 */
	public void reload()
	{
		unloadExtensions();
		clearCache();
		initExtensions();
	}
	
	/**
	 * unloads a named extension, resets the cache and initializes the extension
	 * @param name the class name and optionally a jar file name delimited with a '@' if the jar file is not in the class path
	 */
	public void reload(final String name)
	{
		unloadExtension(name);
		clearCache();
		initExtension(name);
	}
	
	/**
	 * register a getter function given a (hopefully) unique name
	 * @param name the name of the function
	 * @param function the ExtensionFunction implementation
	 */
	public void addGetter(final String name, final ExtensionFunction function)
	{
		_getters.put(name, function);
	}
	
	/**
	 * deregister a getter function
	 * @param name the name used for registering
	 */
	public void removeGetter(final String name)
	{
		_getters.remove(name);
	}
	
	/**
	 * call a getter function registered with DynamicExtension
	 * @param name the function name
	 * @param arg a function argument
	 * @return an object from the extension
	 */
	public Object get(final String name, final String arg)
	{
		final ExtensionFunction func = _getters.get(name);
		
		if (func != null)
		{
			return func.get(arg);
		}
		return "<none>";
	}
	
	/**
	 * register a setter function given a (hopefully) unique name
	 * @param name the name of the function
	 * @param function the ExtensionFunction implementation
	 */
	public void addSetter(final String name, final ExtensionFunction function)
	{
		_setters.put(name, function);
	}
	
	/**
	 * deregister a setter function
	 * @param name the name used for registering
	 */
	public void removeSetter(final String name)
	{
		_setters.remove(name);
	}
	
	/**
	 * call a setter function registered with DynamicExtension
	 * @param name the function name
	 * @param arg a function argument
	 * @param obj an object to set
	 */
	public void set(final String name, final String arg, final Object obj)
	{
		final ExtensionFunction func = _setters.get(name);
		
		if (func != null)
		{
			func.set(arg, obj);
		}
	}
	
	public JarClassLoader getClassLoader()
	{
		return _classLoader;
	}
}
