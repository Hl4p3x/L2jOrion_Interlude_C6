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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Map;

import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

import javolution.util.FastMap;
import l2jorion.Config;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;

public class CompiledScriptCache implements Serializable
{
	/**
	 * Version 1
	 */
	private static final long serialVersionUID = 1L;
	
	private static final Logger LOG = LoggerFactory.getLogger(CompiledScriptCache.class);
	
	private final Map<String, CompiledScriptHolder> _compiledScriptCache = new FastMap<>();
	private transient boolean _modified = false;
	
	public CompiledScript loadCompiledScript(final ScriptEngine engine, final File file) throws ScriptException
	{
		final int len = L2ScriptEngineManager.SCRIPT_FOLDER.getPath().length() + 1;
		final String relativeName = file.getPath().substring(len);
		
		final CompiledScriptHolder csh = _compiledScriptCache.get(relativeName);
		if (csh != null && csh.matches(file))
		{
			if (Config.DEBUG)
			{
				LOG.debug("Reusing cached compiled script: " + file);
			}
			return csh.getCompiledScript();
		}
		
		if (Config.DEBUG)
		{
			LOG.info("Compiling script: " + file);
		}
		
		final Compilable eng = (Compilable) engine;
		FileInputStream fis = null;
		
		BufferedReader buff = null;
		InputStreamReader isr = null;
		CompiledScript cs = null;
		
		try
		{
			
			fis = new FileInputStream(file);
			isr = new InputStreamReader(fis);
			buff = new BufferedReader(isr);
			
			// TODO lock file
			cs = eng.compile(buff);
			if (cs instanceof Serializable)
			{
				synchronized (_compiledScriptCache)
				{
					_compiledScriptCache.put(relativeName, new CompiledScriptHolder(cs, file));
					_modified = true;
				}
			}
			
		}
		catch (final IOException e)
		{
			
			e.printStackTrace();
			
		}
		finally
		{
			
			if (buff != null)
				try
				{
					buff.close();
				}
				catch (final IOException e)
				{
					e.printStackTrace();
				}
			if (isr != null)
				try
				{
					isr.close();
				}
				catch (final IOException e)
				{
					e.printStackTrace();
				}
			if (fis != null)
				try
				{
					fis.close();
				}
				catch (final IOException e)
				{
					e.printStackTrace();
				}
		}
		
		return cs;
	}
	
	public boolean isModified()
	{
		return _modified;
	}
	
	public void purge()
	{
		synchronized (_compiledScriptCache)
		{
			for (final String path : _compiledScriptCache.keySet())
			{
				final File file = new File(L2ScriptEngineManager.SCRIPT_FOLDER, path);
				if (!file.isFile())
				{
					_compiledScriptCache.remove(path);
					_modified = true;
				}
			}
		}
	}
	
	public void save()
	{
		synchronized (_compiledScriptCache)
		{
			File file = null;
			FileOutputStream out = null;
			ObjectOutputStream oos = null;
			
			try
			{
				file = new File(L2ScriptEngineManager.SCRIPT_FOLDER, "CompiledScripts.cache");
				out = new FileOutputStream(file);
				oos = new ObjectOutputStream(out);
				oos.writeObject(this);
				_modified = false;
			}
			catch (final FileNotFoundException e)
			{
				e.printStackTrace();
			}
			catch (final IOException e)
			{
				e.printStackTrace();
				
			}
			finally
			{
				
				if (oos != null)
					try
					{
						oos.close();
					}
					catch (final IOException e)
					{
						e.printStackTrace();
					}
				
				if (out != null)
					try
					{
						out.close();
					}
					catch (final IOException e)
					{
						e.printStackTrace();
					}
				
			}
			
		}
	}
}
