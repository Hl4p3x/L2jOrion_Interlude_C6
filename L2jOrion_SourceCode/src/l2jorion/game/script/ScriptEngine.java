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
package l2jorion.game.script;

import java.util.Hashtable;

import l2jorion.Config;
import l2jorion.game.script.faenor.FaenorInterface;

public class ScriptEngine
{
	protected EngineInterface _utils = FaenorInterface.getInstance();
	
	public static final Hashtable<String, ParserFactory> parserFactories = new Hashtable<>();
	
	protected static Parser createParser(final String name) throws ParserNotCreatedException
	{
		ParserFactory s = parserFactories.get(name);
		if (s == null)
		{
			try
			{
				Class.forName("l2jorion.game.script." + name);
				
				s = parserFactories.get(name);
				if (s == null)
				{
					throw new ParserNotCreatedException();
				}
			}
			catch (final ClassNotFoundException e)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
				{
					e.printStackTrace();
				}
				
				throw new ParserNotCreatedException();
			}
		}
		return s.create();
	}
}
