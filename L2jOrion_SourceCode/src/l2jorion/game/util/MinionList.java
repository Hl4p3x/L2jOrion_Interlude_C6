/*
 *
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
package l2jorion.game.util;

import java.util.List;
import java.util.Set;

import javolution.util.FastList;
import javolution.util.FastMap;
import javolution.util.FastSet;
import l2jorion.Config;
import l2jorion.game.datatables.sql.NpcTable;
import l2jorion.game.idfactory.IdFactory;
import l2jorion.game.model.L2Character;
import l2jorion.game.model.L2MinionData;
import l2jorion.game.model.actor.instance.L2MinionInstance;
import l2jorion.game.model.actor.instance.L2MonsterInstance;
import l2jorion.game.templates.L2NpcTemplate;
import l2jorion.game.thread.ThreadPoolManager;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;
import l2jorion.util.random.Rnd;

public class MinionList
{
	private static Logger LOG = LoggerFactory.getLogger(L2MonsterInstance.class);
	
	private final List<L2MinionInstance> minionReferences;
	protected FastMap<Long, Integer> _respawnTasks = new FastMap<Long, Integer>().shared();
	private final L2MonsterInstance master;
	
	public MinionList(final L2MonsterInstance pMaster)
	{
		minionReferences = new FastList<>();
		master = pMaster;
	}
	
	public int countSpawnedMinions()
	{
		synchronized (minionReferences)
		{
			return minionReferences.size();
		}
	}
	
	public int countSpawnedMinionsById(final int minionId)
	{
		int count = 0;
		synchronized (minionReferences)
		{
			for (final L2MinionInstance minion : getSpawnedMinions())
			{
				if (minion.getNpcId() == minionId)
				{
					count++;
				}
			}
		}
		return count;
	}
	
	public boolean hasMinions()
	{
		return getSpawnedMinions().size() > 0;
	}
	
	public List<L2MinionInstance> getSpawnedMinions()
	{
		return minionReferences;
	}
	
	public void addSpawnedMinion(final L2MinionInstance minion)
	{
		synchronized (minionReferences)
		{
			minionReferences.add(minion);
		}
	}
	
	public int lazyCountSpawnedMinionsGroups()
	{
		final Set<Integer> seenGroups = new FastSet<>();
		for (final L2MinionInstance minion : getSpawnedMinions())
		{
			seenGroups.add(minion.getNpcId());
		}
		return seenGroups.size();
	}
	
	public void removeSpawnedMinion(final L2MinionInstance minion)
	{
		synchronized (minionReferences)
		{
			minionReferences.remove(minion);
		}
	}
	
	public void moveMinionToRespawnList(final L2MinionInstance minion)
	{
		final Long current = System.currentTimeMillis();
		synchronized (minionReferences)
		{
			minionReferences.remove(minion);
			
			if (_respawnTasks.get(current) == null)
			{
				_respawnTasks.put(current, minion.getNpcId());
			}
			else
			{
				for (int i = 1; i < 30; i++)
				{
					if (_respawnTasks.get(current + i) == null)
					{
						_respawnTasks.put(current + i, minion.getNpcId());
						break;
					}
				}
			}
		}
	}
	
	public void clearRespawnList()
	{
		_respawnTasks.clear();
	}
	
	public void maintainMinions()
	{
		if (master == null || master.isAlikeDead())
		{
			return;
		}
		
		final Long current = System.currentTimeMillis();
		
		if (_respawnTasks != null)
		{
			for (final long deathTime : _respawnTasks.keySet())
			{
				double delay = Config.RAID_MINION_RESPAWN_TIMER;
				
				if (_respawnTasks.containsValue(27189)) // fairy tree minion
				{
					delay = 20000;
				}
				
				if (current - deathTime > delay)
				{
					spawnSingleMinion(_respawnTasks.get(deathTime));
					_respawnTasks.remove(deathTime);
				}
			}
		}
	}
	
	public void spawnMinions()
	{
		if (master == null || master.isAlikeDead())
		{
			return;
		}
		
		final List<L2MinionData> minions = master.getTemplate().getMinionData();
		
		synchronized (minionReferences)
		{
			int minionCount, minionId, minionsToSpawn;
			
			for (final L2MinionData minion : minions)
			{
				minionCount = minion.getAmount();
				minionId = minion.getMinionId();
				
				minionsToSpawn = minionCount - countSpawnedMinionsById(minionId);
				
				for (int i = 0; i < minionsToSpawn; i++)
				{
					spawnSingleMinion(minionId);
				}
			}
		}
	}
	
	public void spawnSingleMinion(final int minionid)
	{
		// Get the template of the Minion to spawn
		final L2NpcTemplate minionTemplate = NpcTable.getInstance().getTemplate(minionid);
		
		// Create and Init the Minion and generate its Identifier
		final L2MinionInstance monster = new L2MinionInstance(IdFactory.getInstance().getNextId(), minionTemplate);
		
		// Set the Minion HP, MP and Heading
		monster.setCurrentHpMp(monster.getMaxHp(), monster.getMaxMp());
		monster.setHeading(master.getHeading());
		
		// Set the Minion leader to this RaidBoss
		monster.setLeader(master);
		
		// Init the position of the Minion and add it in the world as a visible object
		int spawnConstant;
		final int randSpawnLim = 170;
		int randPlusMin = 1;
		spawnConstant = Rnd.nextInt(randSpawnLim);
		// randomize +/-
		randPlusMin = Rnd.nextInt(2);
		if (randPlusMin == 1)
		{
			spawnConstant *= -1;
		}
		
		final int newX = master.getX() + spawnConstant;
		spawnConstant = Rnd.nextInt(randSpawnLim);
		// randomize +/-
		randPlusMin = Rnd.nextInt(2);
		
		if (randPlusMin == 1)
		{
			spawnConstant *= -1;
		}
		
		final int newY = master.getY() + spawnConstant;
		
		monster.spawnMe(newX, newY, master.getZ());
		
		if (minionid == 27189)
		{
			monster.startAbnormalEffect(L2Character.ABNORMAL_EFFECT_MAGIC_CIRCLE);
			ThreadPoolManager.getInstance().scheduleGeneral(() ->
			{
				monster.stopAbnormalEffect(L2Character.ABNORMAL_EFFECT_MAGIC_CIRCLE);
			}, 1500);
			
		}
		
		if (Config.DEBUG)
		{
			LOG.debug("Spawned minion template " + minionTemplate.npcId + " with objid: " + monster.getObjectId() + " to boss " + master.getObjectId() + " ,at: " + monster.getX() + " x, " + monster.getY() + " y, " + monster.getZ() + " z");
		}
	}
}
