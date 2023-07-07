package l2jorion.game.datatables;

import java.util.Map;

import javolution.util.FastMap;
import l2jorion.game.model.MobGroup;
import l2jorion.game.model.actor.instance.L2ControllableMobInstance;

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
		{
			if (mobGroup.isGroupMember(mobInst))
			{
				return mobGroup;
			}
		}
		
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
