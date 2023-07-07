/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package l2jorion.game.model.actor.instance;

import l2jorion.Config;
import l2jorion.game.enums.AchType;
import l2jorion.game.managers.GrandBossManager;
import l2jorion.game.managers.RaidBossPointsManager;
import l2jorion.game.model.L2Character;
import l2jorion.game.model.L2Summon;
import l2jorion.game.model.spawn.L2Spawn;
import l2jorion.game.model.zone.ZoneId;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.PlaySound;
import l2jorion.game.network.serverpackets.SystemMessage;
import l2jorion.game.templates.L2NpcTemplate;
import l2jorion.game.thread.ThreadPoolManager;
import l2jorion.util.random.Rnd;

public final class L2GrandBossInstance extends L2MonsterInstance
{
	private static final int BOSS_MAINTENANCE_INTERVAL = 20000;
	
	public L2GrandBossInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
		setIsRaid(true);
	}
	
	@Override
	protected int getMaintenanceInterval()
	{
		return BOSS_MAINTENANCE_INTERVAL;
	}
	
	@Override
	public boolean doDie(L2Character killer)
	{
		if (!super.doDie(killer))
		{
			return false;
		}
		
		AchType type = null;
		switch (getNpcId())
		{
			case 29019:
			case 29066:
			case 29067:
			case 29068:
				type = AchType.ANTHARAS;
				break;
			case 29001:
				type = AchType.QUEEN_ANT;
				break;
			case 29006:
				type = AchType.CORE;
				break;
			case 29014:
				type = AchType.ORFEN;
				break;
			case 29022:
				type = AchType.ZAKEN;
				break;
			case 29020:
				type = AchType.BAIUM;
				break;
			case 29028:
				type = AchType.VALAKAS;
				break;
			case 29047:
				type = AchType.HALISHA;
				break;
		}
		
		L2PcInstance player = null;
		
		if (killer instanceof L2PcInstance)
		{
			player = (L2PcInstance) killer;
		}
		else if (killer instanceof L2Summon)
		{
			player = ((L2Summon) killer).getOwner();
		}
		
		if (player != null)
		{
			SystemMessage msg = new SystemMessage(SystemMessageId.RAID_WAS_SUCCESSFUL);
			broadcastPacket(new PlaySound("systemmsg_e.1209"));
			broadcastPacket(msg);
			
			if (Config.L2LIMIT_CUSTOM)
			{
				if (getLevel() >= 40)
				{
					if (player.getClan() != null)
					{
						player.getClan().setReputationScore(player.getClan().getReputationScore() + Rnd.get(50, 100), true);
					}
					player.addItem("AutoLoot", 6392, 1, this, true);
				}
			}
			
			if (player.getParty() != null)
			{
				for (L2PcInstance member : player.getParty().getPartyMembers())
				{
					RaidBossPointsManager.addPoints(member, getNpcId(), (getLevel() / 2) + Rnd.get(-5, 5));
					member.getAchievement().increase(AchType.GRANDBOSS);
					if (type != null)
					{
						member.getAchievement().increase(type);
					}
					
					// Daily
					member.getAchievement().increase(AchType.DAILY_BOSS, 1, true, true, true, getNpcId());
					
					if (Config.L2LIMIT_CUSTOM)
					{
						member.setWeeklyBoardRaidPoints(member.getWeeklyBoardRaidPoints() + ((getLevel() / 2) + Rnd.get(-5, 5)));
					}
				}
			}
			else
			{
				RaidBossPointsManager.addPoints(player, getNpcId(), (getLevel() / 2) + Rnd.get(-5, 5));
				player.getAchievement().increase(AchType.GRANDBOSS);
				if (type != null)
				{
					player.getAchievement().increase(type);
				}
				
				// Daily
				player.getAchievement().increase(AchType.DAILY_BOSS, 1, true, true, true, getNpcId());
				
				if (Config.L2LIMIT_CUSTOM)
				{
					player.setWeeklyBoardRaidPoints(player.getWeeklyBoardRaidPoints() + ((getLevel() / 2) + Rnd.get(-5, 5)));
				}
			}
		}
		return true;
	}
	
	@Override
	public void onSpawn()
	{
		super.onSpawn();
		
		if (!this.getSpawn().is_customBossInstance())
		{
			GrandBossManager.getInstance().addBoss(this);
		}
	}
	
	@Override
	protected void manageMaintenance()
	{
		_minionList.spawnMinions();
		
		if (_minionMaintainTask == null)
		{
			_minionMaintainTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(() ->
			{
				L2Spawn bossSpawn = getSpawn();
				
				int rb_lock_range = Config.RBLOCKRAGE;
				
				if (Config.RBS_SPECIFIC_LOCK_RAGE.get(bossSpawn.getNpcid()) != null)
				{
					rb_lock_range = Config.RBS_SPECIFIC_LOCK_RAGE.get(bossSpawn.getNpcid());
				}
				
				if (Config.RBS_SPECIFIC_LOCK_RAGE.get(bossSpawn.getNpcid()) != null && Config.RBS_SPECIFIC_LOCK_RAGE.get(bossSpawn.getNpcid()).intValue() == 1)
				{
					if (!isInsideZone(ZoneId.ZONE_BOSS) && getSpawn() != null)
					{
						teleToLocation(getSpawn().getLocx(), getSpawn().getLocy(), getSpawn().getLocz(), false);
					}
				}
				
				if (rb_lock_range >= 100 && !isInsideRadius(bossSpawn.getLocx(), bossSpawn.getLocy(), bossSpawn.getLocz(), rb_lock_range, true, false))
				{
					teleToLocation(bossSpawn.getLocx(), bossSpawn.getLocy(), bossSpawn.getLocz(), false);
					
					if (Config.HEAL_GRANDBOSS)
					{
						healFull();
					}
				}
				
				_minionList.maintainMinions();
			}, 1000, getMaintenanceInterval());
		}
	}
	
	public void healFull()
	{
		super.setCurrentHp(super.getMaxHp());
		super.setCurrentMp(super.getMaxMp());
	}
	
	@Override
	public boolean isMonster()
	{
		return false;
	}
}