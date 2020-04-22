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
package l2jorion.game.datatables;

import java.util.Map;

import javolution.util.FastMap;
import l2jorion.game.model.MobGroup;
import l2jorion.game.model.actor.instance.L2ControllableMobInstance;

/**
 * @author littlecrow
 */
public class MobGroupTable
{
	private static MobGroupTable _instance;
	private final Map<Integer, MobGroup> _groupMap;
	
	public static final int FOLLOW_RANGE = 300;
	public static final int RANDOM_RANGE = 300;
	
	public MobGroupTable()
	{
		_groupMap = new FastMap<>();
	}
	
	public static MobGroupTable getInstance()
	{
		if (_instance == null)
		{
			_instance = new MobGroupTable();
		}
		
		return _instance;
	}
	
	public void addGroup(final int groupKey, final MobGroup group)
	{
		_groupMap.put(groupKey, group);
	}
	
	public MobGroup getGroup(final int groupKey)
	{
		return _groupMap.get(groupKey);
	}
	
	public int getGroupCount()
	{
		return _groupMap.size();
	}
	
	public MobGroup getGroupForMob(final L2ControllableMobInstance mobInst)
	{
		for (final MobGroup mobGroup : _groupMap.values())
			if (mobGroup.isGroupMember(mobInst))
				return mobGroup;
		
		return null;
	}
	
	public MobGroup[] getGroups()
	{
		return _groupMap.values().toArray(new MobGroup[getGroupCount()]);
	}
	
	public boolean removeGroup(final int groupKey)
	{
		return _groupMap.remove(groupKey) != null;
	}
}
