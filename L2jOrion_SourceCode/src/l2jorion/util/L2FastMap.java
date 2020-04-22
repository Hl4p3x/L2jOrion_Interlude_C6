/* L2jOrion Project - www.l2jorion.com 
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
package l2jorion.util;

import javolution.util.FastMap;

/**
 * @author Julian
 * @param <K>
 * @param <V>
 */
public class L2FastMap<K, V> extends FastMap<K, V>
{
	static final long serialVersionUID = 1L;
	
	public interface I2ForEach<K, V>
	{
		public boolean forEach(K key, V obj);
		
		public FastMap.Entry<K, V> getNext(FastMap.Entry<K, V> priv);
	}
	
	public final boolean ForEach(final I2ForEach<K, V> func, final boolean sync)
	{
		if (sync)
		{
			synchronized (this)
			{
				return forEachP(func);
			}
		}
		return forEachP(func);
	}
	
	private boolean forEachP(final I2ForEach<K, V> func)
	{
		for (FastMap.Entry<K, V> e = head(), end = tail(); (e = func.getNext(e)) != end;)
			if (!func.forEach(e.getKey(), e.getValue()))
				return false;
		return true;
	}
}
