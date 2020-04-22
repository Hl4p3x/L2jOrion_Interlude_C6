/*
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

import l2jorion.Config;
import l2jorion.game.managers.RaidBossPointsManager;
import l2jorion.game.managers.RaidBossSpawnManager;
import l2jorion.game.model.L2Character;
import l2jorion.game.model.L2Summon;
import l2jorion.game.model.entity.Announcements;
import l2jorion.game.model.spawn.AutoSpawn;
import l2jorion.game.model.spawn.L2Spawn;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.PlaySound;
import l2jorion.game.network.serverpackets.SystemMessage;
import l2jorion.game.templates.L2NpcTemplate;
import l2jorion.game.thread.ThreadPoolManager;
import l2jorion.util.random.Rnd;

public final class L2RaidBossInstance extends L2MonsterInstance
{
	private static final int RAIDBOSS_MAINTENANCE_INTERVAL = 20000; // 20 sec

	/** The _raid status. */
	private RaidBossSpawnManager.StatusEnum _raidStatus;

	/**
	 * Constructor of L2RaidBossInstance (use L2Character and L2NpcInstance constructor).<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Call the L2Character constructor to set the _template of the L2RaidBossInstance (copy skills from template to
	 * object and link _calculators to NPC_STD_CALCULATOR)</li> <li>Set the name of the L2RaidBossInstance</li> <li>
	 * Create a RandomAnimation Task that will be launched after the calculated delay if the server allow it</li><BR>
	 * <BR>
	 *
	 * @param objectId Identifier of the object to initialized
	 * @param template the template
	 */
	public L2RaidBossInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
		setIsRaid(true);
	}

	@Override
	protected int getMaintenanceInterval()
	{
		return RAIDBOSS_MAINTENANCE_INTERVAL;
	}
	
	@Override
	public boolean doDie(L2Character killer)
	{
		if (!super.doDie(killer))
		{
			return false;
		}
		
		L2PcInstance player = null;
		
		if(killer instanceof L2PcInstance)
		{
			player = (L2PcInstance) killer;
		}
		else if(killer instanceof L2Summon)
		{
			player = ((L2Summon) killer).getOwner();
		}
		
		SystemMessage msg = new SystemMessage(SystemMessageId.RAID_WAS_SUCCESSFUL);
		broadcastPacket(new PlaySound("systemmsg_e.1209"));
		broadcastPacket(msg);
		
		if (player != null && (getNpcId() != 22215 && getNpcId() != 22216 && getNpcId() != 22217 && getNpcId() != 22318 && getNpcId() != 22319))
		{
			if (Config.ANNOUNCE_RB_KILLER_INFO)
			{
				if (player.getClan() != null)
				{
					Announcements.getInstance().announceRB("The Raid Boss " + getName() + " was killed. Last hit: "+player.getName()+ " Clan: "+player.getClan().getName());
				}
				else
				{
					Announcements.getInstance().announceRB("The Raid Boss " + getName() + " was killed. Last hit: "+player.getName());
				}
			}
			
			if (player.getParty() != null)
			{
				for (L2PcInstance member : player.getParty().getPartyMembers())
				{
					RaidBossPointsManager.addPoints(member, getNpcId(), (getLevel() / 2) + Rnd.get(-5, 5));
				}
			}
			else
			{
				RaidBossPointsManager.addPoints(player, getNpcId(), (getLevel() / 2) + Rnd.get(-5, 5));
			}
		}
		
		if (!getSpawn().is_customBossInstance())
		{
			RaidBossSpawnManager.getInstance().updateStatus(this, true);
		}
		
		//Check auto spawn instance
		AutoSpawn.updateStatus(this, true);
		
		return true;
	}

	@Override
	protected void manageMinions()
	{
		_minionList.spawnMinions();
		_minionMaintainTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new Runnable()
		{
			@Override
			public void run()
			{
				// teleport raid boss home if it's too far from home location
				L2Spawn bossSpawn = getSpawn();
				int rb_lock_range = Config.RBLOCKRAGE;
				if (Config.RBS_SPECIFIC_LOCK_RAGE.get(bossSpawn.getNpcid()) != null)
				{
					rb_lock_range = Config.RBS_SPECIFIC_LOCK_RAGE.get(bossSpawn.getNpcid());
				}
				
				if (rb_lock_range != -1 && !isInsideRadius(bossSpawn.getLocx(), bossSpawn.getLocy(), bossSpawn.getLocz(), rb_lock_range, true, false))
				{
					teleToLocation(bossSpawn.getLocx(), bossSpawn.getLocy(), bossSpawn.getLocz(), false);
					if (Config.HEAL_RAIDBOSS)
						healFull();
				}
				
				_minionList.maintainMinions();
				bossSpawn = null;
			}
		}, 60000, getMaintenanceInterval());
	}

	/**
	 * Sets the raid status.
	 *
	 * @param status the new raid status
	 */
	public void setRaidStatus(RaidBossSpawnManager.StatusEnum status)
	{
		_raidStatus = status;
	}

	/**
	 * Gets the raid status.
	 *
	 * @return the raid status
	 */
	public RaidBossSpawnManager.StatusEnum getRaidStatus()
	{
		return _raidStatus;
	}
	
	public void healFull()
	{
		super.setCurrentHp(super.getMaxHp());
		super.setCurrentMp(super.getMaxMp());
	}
}
