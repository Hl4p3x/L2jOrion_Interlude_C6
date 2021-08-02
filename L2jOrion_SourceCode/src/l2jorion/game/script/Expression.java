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

import javax.script.ScriptContext;

import l2jorion.game.scripting.L2ScriptEngineManager;

public class Expression
{
	private final ScriptContext _context;
	
	public static Object eval(final String lang, final String code)
	{
		try
		{
			return L2ScriptEngineManager.getInstance().eval(lang, code);
		}
		catch (final Exception e)
		{
			e.printStackTrace();
			return null;
		}
	}
	
	public static Object eval(final ScriptContext context, final String lang, final String code)
	{
		try
		{
			return L2ScriptEngineManager.getInstance().eval(lang, code, context);
		}
		catch (final Exception e)
		{
			e.printStackTrace();
			return null;
		}
	}
	
	public static Expression create(final ScriptContext context/* , String lang, String code */)
	{
		try
		{
			return new Expression(context/* , lang, code */);
		}
		catch (final Exception e)
		{
			e.printStackTrace();
			return null;
		}
	}
	
	private Expression(final ScriptContext pContext/* , String pLang, String pCode */)
	{
		_context = pContext;
	}
	
	public <T> void addDynamicVariable(final String name, final T value)
	{
		try
		{
			_context.setAttribute(name, value, ScriptContext.ENGINE_SCOPE);
		}
		catch (final Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public void removeDynamicVariable(final String name)
	{
		try
		{
			_context.removeAttribute(name, ScriptContext.ENGINE_SCOPE);
		}
		catch (final Exception e)
		{
			e.printStackTrace();
		}
	}
	
}
