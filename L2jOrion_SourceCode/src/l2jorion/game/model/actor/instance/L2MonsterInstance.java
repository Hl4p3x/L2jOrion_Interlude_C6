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
package l2jorion.game.model.actor.instance;

import java.util.List;
import java.util.concurrent.ScheduledFuture;

import l2jorion.Config;
import l2jorion.game.controllers.GameTimeController;
import l2jorion.game.enums.AchType;
import l2jorion.game.model.L2Attackable;
import l2jorion.game.model.L2Character;
import l2jorion.game.model.actor.knownlist.MonsterKnownList;
import l2jorion.game.model.spawn.AutoSpawn;
import l2jorion.game.model.spawn.L2Spawn;
import l2jorion.game.network.serverpackets.SocialAction;
import l2jorion.game.templates.L2NpcTemplate;
import l2jorion.game.thread.ThreadPoolManager;
import l2jorion.game.util.MinionList;
import l2jorion.util.random.Rnd;

public class L2MonsterInstance extends L2Attackable
{
	protected final MinionList _minionList;
	
	protected ScheduledFuture<?> _minionMaintainTask = null;
	
	private static final int MONSTER_MAINTENANCE_INTERVAL = 1000;
	
	public L2MonsterInstance(final int objectId, final L2NpcTemplate template)
	{
		super(objectId, template);
		getKnownList();
		_minionList = new MinionList(this);
	}
	
	@Override
	public final MonsterKnownList getKnownList()
	{
		if (super.getKnownList() == null || !(super.getKnownList() instanceof MonsterKnownList))
		{
			setKnownList(new MonsterKnownList(this));
		}
		return (MonsterKnownList) super.getKnownList();
	}
	
	/**
	 * Return home.
	 */
	@Override
	public void returnHome()
	{
		ThreadPoolManager.getInstance().scheduleAi(new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					L2Spawn mobSpawn = getSpawn();
					if ((!isRaid()) && (!isInCombat()) && (!isAlikeDead()) && (!isDead()) && (mobSpawn != null) && (!isInsideRadius(mobSpawn.getLocx(), mobSpawn.getLocy(), Config.MAX_DRIFT_RANGE * 3, false)))
					{
						teleToLocation(mobSpawn.getLocx(), mobSpawn.getLocy(), mobSpawn.getLocz(), false);
					}
				}
				catch (Exception e)
				{
					LOG.info("returnHome() ID:" + getNpcId() + " NAME:" + getName() + "", e);
				}
				
			}
		}, Config.MONSTER_RETURN_DELAY * 1000);
	}
	
	/**
	 * Return True if the attacker is not another L2MonsterInstance.<BR>
	 * <BR>
	 * @param attacker the attacker
	 * @return true, if is auto attackable
	 */
	@Override
	public boolean isAutoAttackable(final L2Character attacker)
	{
		if (attacker instanceof L2MonsterInstance)
		{
			return false;
		}
		
		return !isEventMob;
	}
	
	@Override
	public boolean isAggressive()
	{
		return getTemplate().aggroRange > 0 && !isEventMob;
	}
	
	@Override
	public boolean isBatNightMode()
	{
		String npcClass = getTemplate().getStatsSet().getString("jClass").toLowerCase();
		boolean isNight = GameTimeController.getInstance().isNight();
		
		if (isNight)
		{
			if (npcClass.contains("monster.vampire_bat"))
			{
				return true;
			}
		}
		
		return false;
	}
	
	@Override
	public void onSpawn()
	{
		if (getTemplate().getMinionData() != null)
		{
			try
			{
				for (final L2MinionInstance minion : getSpawnedMinions())
				{
					if (minion == null)
					{
						continue;
					}
					
					getSpawnedMinions().remove(minion);
					minion.deleteMe();
				}
				_minionList.clearRespawnList();
				
				manageMinions();
			}
			catch (final NullPointerException e)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
				{
					e.printStackTrace();
				}
			}
			
			switch (getTemplate().npcId)
			{
				case 12372: // baium
				{
					SocialAction sa = new SocialAction(getObjectId(), 2);
					broadcastPacket(sa);
				}
			}
		}
		
		super.onSpawn();
	}
	
	protected int getMaintenanceInterval()
	{
		return MONSTER_MAINTENANCE_INTERVAL;
	}
	
	protected void manageMinions()
	{
		_minionMaintainTask = ThreadPoolManager.getInstance().scheduleGeneral(new Runnable()
		{
			@Override
			public void run()
			{
				_minionList.spawnMinions();
			}
		}, getMaintenanceInterval());
	}
	
	public void callMinions()
	{
		if (_minionList.hasMinions())
		{
			for (final L2MinionInstance minion : _minionList.getSpawnedMinions())
			{
				// Get actual coords of the minion and check to see if it's too far away from this L2MonsterInstance
				if (!isInsideRadius(minion, 200, false, false))
				{
					// Get the coords of the master to use as a base to move the minion to
					final int masterX = getX();
					final int masterY = getY();
					final int masterZ = getZ();
					
					// Calculate a new random coord for the minion based on the master's coord
					int minionX = masterX + Rnd.nextInt(401) - 200;
					int minionY = masterY + Rnd.nextInt(401) - 200;
					final int minionZ = masterZ;
					while (minionX != masterX + 30 && minionX != masterX - 30 || minionY != masterY + 30 && minionY != masterY - 30)
					{
						minionX = masterX + Rnd.nextInt(401) - 200;
						minionY = masterY + Rnd.nextInt(401) - 200;
					}
					
					// Move the minion to the new coords
					if (!minion.isInCombat() && !minion.isDead() && !minion.isMovementDisabled())
					{
						minion.moveToLocation(minionX, minionY, minionZ, 0);
					}
				}
			}
		}
	}
	
	public void callMinionsToAssist(final L2Character attacker)
	{
		if (_minionList.hasMinions())
		{
			for (final L2MinionInstance minion : _minionList.getSpawnedMinions())
			{
				if (minion != null && !minion.isDead())
				{
					if (this instanceof L2RaidBossInstance)
					{
						minion.addDamage(attacker, 100);
					}
					else
					{
						minion.addDamage(attacker, 1);
					}
				}
			}
		}
	}
	
	@Override
	public boolean doDie(final L2Character killer)
	{
		if (!super.doDie(killer))
		{
			return false;
		}
		
		// Check auto spawn instance
		AutoSpawn.updateStatus(this, true);
		
		if (_minionMaintainTask != null)
		{
			_minionMaintainTask.cancel(true); // doesn't do it?
		}
		
		final L2PcInstance player = killer.getActingPlayer();
		if (player != null && (player.getLevel() - getLevel() <= 10) && !(this instanceof L2RaidBossInstance) && !(this instanceof L2GrandBossInstance))
		{
			player.getAchievement().increase(getName().equals("Tyrannosaurus") ? AchType.TYRANNOSAURUS : isChampion() ? AchType.MONSTER_CHAMPION : AchType.MONSTER);
		}
		
		if (this instanceof L2RaidBossInstance)
		{
			deleteSpawnedMinions();
		}
		return true;
	}
	
	/**
	 * Gets the spawned minions.
	 * @return the spawned minions
	 */
	public List<L2MinionInstance> getSpawnedMinions()
	{
		return _minionList.getSpawnedMinions();
	}
	
	/**
	 * Gets the total spawned minions instances.
	 * @return the total spawned minions instances
	 */
	public int getTotalSpawnedMinionsInstances()
	{
		return _minionList.countSpawnedMinions();
	}
	
	/**
	 * Gets the total spawned minions groups.
	 * @return the total spawned minions groups
	 */
	public int getTotalSpawnedMinionsGroups()
	{
		return _minionList.lazyCountSpawnedMinionsGroups();
	}
	
	/**
	 * Notify minion died.
	 * @param minion the minion
	 */
	public void notifyMinionDied(final L2MinionInstance minion)
	{
		_minionList.moveMinionToRespawnList(minion);
	}
	
	/**
	 * Notify minion spawned.
	 * @param minion the minion
	 */
	public void notifyMinionSpawned(final L2MinionInstance minion)
	{
		_minionList.addSpawnedMinion(minion);
	}
	
	/**
	 * Checks for minions.
	 * @return true, if successful
	 */
	public boolean hasMinions()
	{
		return _minionList.hasMinions();
	}
	
	@Override
	public void addDamageHate(final L2Character attacker, final int damage, final int aggro)
	{
		super.addDamageHate(attacker, damage, aggro);
	}
	
	@Override
	public void deleteMe()
	{
		if (hasMinions())
		{
			if (_minionMaintainTask != null)
			{
				_minionMaintainTask.cancel(true);
			}
			
			deleteSpawnedMinions();
		}
		
		super.deleteMe();
	}
	
	/**
	 * Delete spawned minions.
	 */
	public void deleteSpawnedMinions()
	{
		for (final L2MinionInstance minion : getSpawnedMinions())
		{
			if (minion == null)
			{
				continue;
			}
			minion.abortAttack();
			minion.abortCast();
			minion.deleteMe();
			getSpawnedMinions().remove(minion);
		}
		_minionList.clearRespawnList();
	}
	
	public boolean giveRaidCurse()
	{
		return (isRaidMinion() && (getLeader() != null)) ? ((L2MonsterInstance) getLeader()).giveRaidCurse() : giveRaidCurse();
	}
}
