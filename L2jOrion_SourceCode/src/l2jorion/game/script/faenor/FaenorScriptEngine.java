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
package l2jorion.game.script.faenor;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.script.ScriptContext;

import org.w3c.dom.Node;

import l2jorion.Config;
import l2jorion.game.cache.filters.XMLFilter;
import l2jorion.game.script.Parser;
import l2jorion.game.script.ParserNotCreatedException;
import l2jorion.game.script.ScriptDocument;
import l2jorion.game.script.ScriptEngine;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;

public class FaenorScriptEngine extends ScriptEngine
{
	static Logger LOG = LoggerFactory.getLogger(FaenorScriptEngine.class);
	
	public static final String PACKAGE_DIRECTORY = "data/faenor/";
	
	protected FaenorScriptEngine()
	{
		final File packDirectory = new File(Config.DATAPACK_ROOT, PACKAGE_DIRECTORY);
		final File[] files = packDirectory.listFiles(new XMLFilter());
		if (files != null)
		{
			for (File file : files)
			{
				try (InputStream in = new FileInputStream(file))
				{
					parseScript(new ScriptDocument(file.getName(), in), null);
				}
				catch (IOException e)
				{
					LOG.warn(e.getMessage(), e);
				}
			}
		}
	}
	
	public void parseScript(ScriptDocument script, ScriptContext context)
	{
		Node node = script.getDocument().getFirstChild();
		String parserClass = "faenor.Faenor" + node.getNodeName() + "Parser";
		
		Parser parser = null;
		try
		{
			parser = createParser(parserClass);
		}
		catch (ParserNotCreatedException e)
		{
			LOG.info("ERROR: No parser registered for Script: " + parserClass + ": " + e.getMessage(), e);
		}
		
		if (parser == null)
		{
			LOG.warn("Unknown Script Type: " + script.getName());
			return;
		}
		
		try
		{
			parser.parseScript(node, context);
			LOG.info(getClass().getSimpleName() + ": Loaded  " + script.getName() + " successfully");
		}
		catch (Exception e)
		{
			LOG.warn("Script Parsing Failed: " + e.getMessage(), e);
		}
	}
	
	public static FaenorScriptEngine getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final FaenorScriptEngine _instance = new FaenorScriptEngine();
	}
}