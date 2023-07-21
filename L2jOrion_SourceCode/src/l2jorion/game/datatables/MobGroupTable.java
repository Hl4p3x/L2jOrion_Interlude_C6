package l2jorion.game.datatables;

import java.util.HashMap;
import java.util.Map;

import l2jorion.game.model.MobGroup;
import l2jorion.game.model.actor.instance.L2ControllableMobInstance;

public class MobGroupTable
{
	public static final int FOLLOW_RANGE = 300;
	public static final int RANDOM_RANGE = 300;
	
	private final Map<Integer, MobGroup> _groupMap;
	
	protected MobGroupTable()
	{
		_groupMap = new HashMap<>();
	}
	
	public void addGroup(int groupKey, MobGroup group)
	{
		_groupMap.put(groupKey, group);
	}
	
	public MobGroup getGroup(int groupKey)
	{
		return _groupMap.get(groupKey);
	}
	
	public int getGroupCount()
	{
		return _groupMap.size();
	}
	
	public MobGroup getGroupForMob(L2ControllableMobInstance mobInst)
	{
		for (MobGroup mobGroup : _groupMap.values())
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
	
	public boolean removeGroup(int groupKey)
	{
		return _groupMap.remove(groupKey) != null;
	}
	
	public static MobGroupTable getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final MobGroupTable INSTANCE = new MobGroupTable();
	}
}