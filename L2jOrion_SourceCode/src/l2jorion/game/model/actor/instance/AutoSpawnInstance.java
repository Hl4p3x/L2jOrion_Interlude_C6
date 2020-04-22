package l2jorion.game.model.actor.instance;

import java.util.List;

import javolution.util.FastList;
import l2jorion.Config;
import l2jorion.game.model.Location;
import l2jorion.game.model.spawn.L2Spawn;

public class AutoSpawnInstance
{
	public int _objectId;
	protected int _spawnIndex;
	
	public int _npcId;
	public int _initDelay;
	public int _resDelay;
	public int _desDelay;
	public long _respawnTime;
	
	public int _spawnCount = 1;
	public int _lastLocIndex = -1;
	private final List<L2NpcInstance> _npcList = new FastList<>();
	private final List<Location> _locList = new FastList<>();
	private boolean _spawnActive;
	private boolean _randomSpawn = false;
	private boolean _broadcastAnnouncement = false;
	
	public AutoSpawnInstance(int npcId, int initDelay, int respawnDelay, int despawnDelay, long respawnTime)
	{
		_npcId = npcId;
		_initDelay = initDelay;
		_resDelay = respawnDelay;
		_desDelay = despawnDelay;
		_respawnTime = respawnTime;
	}
	
	public void setSpawnActive(final boolean activeValue)
	{
		_spawnActive = activeValue;
	}
	
	public boolean addNpcInstance(final L2NpcInstance npcInst)
	{
		return _npcList.add(npcInst);
	}
	
	public boolean removeNpcInstance(final L2NpcInstance npcInst)
	{
		return _npcList.remove(npcInst);
	}
	
	public int getObjectId()
	{
		return _objectId;
	}
	
	public int getInitialDelay()
	{
		return _initDelay;
	}
	
	public int getRespawnDelay()
	{
		return _resDelay;
	}
	
	public int getDespawnDelay()
	{
		return _desDelay;
	}
	
	public long getRespawnTime()
	{
		return _respawnTime;
	}
	
	public int getNpcId()
	{
		return _npcId;
	}
	
	public int getSpawnCount()
	{
		return _spawnCount;
	}
	
	public Location[] getLocationList()
	{
		return _locList.toArray(new Location[_locList.size()]);
	}
	
	public L2NpcInstance[] getNPCInstanceList()
	{
		L2NpcInstance[] ret;
		
		synchronized (_npcList)
		{
			ret = new L2NpcInstance[_npcList.size()];
			_npcList.toArray(ret);
		}
		
		return ret;
	}
	
	public L2Spawn[] getSpawns()
	{
		final List<L2Spawn> npcSpawns = new FastList<>();
		
		for (final L2NpcInstance npcInst : _npcList)
		{
			npcSpawns.add(npcInst.getSpawn());
		}
		
		return npcSpawns.toArray(new L2Spawn[npcSpawns.size()]);
	}
	
	public void setSpawnCount(final int spawnCount)
	{
		_spawnCount = spawnCount;
	}
	
	public void setRandomSpawn(final boolean randValue)
	{
		_randomSpawn = randValue;
	}
	
	public void setBroadcast(final boolean broadcastValue)
	{
		_broadcastAnnouncement = broadcastValue;
	}
	
	public boolean isSpawnActive()
	{
		return _spawnActive;
	}
	
	public boolean isRandomSpawn()
	{
		return _randomSpawn;
	}
	
	public boolean isBroadcasting()
	{
		return _broadcastAnnouncement;
	}
	
	public boolean addSpawnLocation(final int x, final int y, final int z, final int heading)
	{
		return _locList.add(new Location(x, y, z, heading));
	}
	
	public boolean addSpawnLocation(final int[] spawnLoc)
	{
		if (spawnLoc.length != 3)
			return false;
		
		return addSpawnLocation(spawnLoc[0], spawnLoc[1], spawnLoc[2], -1);
	}
	
	public Location removeSpawnLocation(final int locIndex)
	{
		try
		{
			return _locList.remove(locIndex);
		}
		catch (final IndexOutOfBoundsException e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
				e.printStackTrace();
			
			return null;
		}
	}
}