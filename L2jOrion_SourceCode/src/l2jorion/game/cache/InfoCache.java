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
package l2jorion.game.cache;

import java.util.List;

import javolution.util.FastMap;
import l2jorion.game.model.L2DropData;

public class InfoCache
{
	private static final FastMap<Integer, List<L2DropData>> _droplistCache = new FastMap<>();
	
	public static void addToDroplistCache(final int id, final List<L2DropData> list)
	{
		_droplistCache.put(id, list);
	}
	
	public static List<L2DropData> getFromDroplistCache(final int id)
	{
		return _droplistCache.get(id);
	}
	
	public static void unload()
	{
		_droplistCache.clear();
	}
}
