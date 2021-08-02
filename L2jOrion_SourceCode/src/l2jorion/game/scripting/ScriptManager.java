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

public abstract class ScriptManager<S extends ManagedScript>
{
	public abstract Iterable<S> getAllManagedScripts();
	
	public boolean reload(final S ms)
	{
		return ms.reload();
	}
	
	public boolean unload(final S ms)
	{
		return ms.unload();
	}
	
	public void setActive(final S ms, final boolean status)
	{
		ms.setActive(status);
	}
	
	public abstract String getScriptManagerName();
}
