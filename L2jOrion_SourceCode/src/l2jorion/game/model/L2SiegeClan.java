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
package l2jorion.game.model;

import java.util.ArrayList;
import java.util.List;

import javolution.util.FastList;
import l2jorion.game.model.actor.instance.L2NpcInstance;

public class L2SiegeClan
{
	private int _clanId = 0;
	private List<L2NpcInstance> _flag = new ArrayList<>();
	private int _numFlagsAdded = 0;
	private SiegeClanType _type;
	
	public enum SiegeClanType
	{
		OWNER,
		DEFENDER,
		ATTACKER,
		DEFENDER_PENDING
	}
	
	public L2SiegeClan(final int clanId, final SiegeClanType type)
	{
		_clanId = clanId;
		_type = type;
	}
	
	public int getNumFlags()
	{
		return _numFlagsAdded;
	}
	
	public void addFlag(final L2NpcInstance flag)
	{
		_numFlagsAdded++;
		getFlag().add(flag);
	}
	
	public boolean removeFlag(final L2NpcInstance flag)
	{
		if (flag == null)
		{
			return false;
		}
		
		final boolean ret = getFlag().remove(flag);
		
		// flag.deleteMe();
		// check if null objects or dups remain in the list.
		// for some reason, this might be happening sometimes...
		// delete false duplicates: if this flag got deleted, delete its copies too.
		if (ret)
		{
			while (getFlag().remove(flag))
			{
				//
			}
		}
		
		// now delete nulls
		int n;
		boolean more = true;
		
		while (more)
		{
			more = false;
			n = getFlag().size();
			
			if (n > 0)
			{
				for (int i = 0; i < n; i++)
				{
					if (getFlag().get(i) == null)
					{
						getFlag().remove(i);
						more = true;
						break;
					}
				}
			}
		}
		
		_numFlagsAdded--; // remove flag count
		flag.deleteMe();
		return ret;
	}
	
	public void removeFlags()
	{
		for (final L2NpcInstance flag : getFlag())
		{
			removeFlag(flag);
		}
	}
	
	public final int getClanId()
	{
		return _clanId;
	}
	
	public final List<L2NpcInstance> getFlag()
	{
		if (_flag == null)
		{
			_flag = new FastList<>();
		}
		
		return _flag;
	}
	
	public SiegeClanType getType()
	{
		return _type;
	}
	
	public void setType(final SiegeClanType setType)
	{
		_type = setType;
	}
}