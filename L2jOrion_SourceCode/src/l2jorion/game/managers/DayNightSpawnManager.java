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
package l2jorion.game.managers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javolution.util.FastMap;
import l2jorion.game.controllers.GameTimeController;
import l2jorion.game.datatables.SkillTable;
import l2jorion.game.model.L2Skill;
import l2jorion.game.model.L2World;
import l2jorion.game.model.actor.instance.L2NpcInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.actor.instance.L2RaidBossInstance;
import l2jorion.game.model.spawn.L2Spawn;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.SystemMessage;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;

public class DayNightSpawnManager
{
	private static Logger LOG = LoggerFactory.getLogger(DayNightSpawnManager.class);
	
	private static DayNightSpawnManager _instance;
	
	private List<L2Spawn> _dayCreatures;
	private List<L2Spawn> _nightCreatures;
	private static Map<L2Spawn, L2RaidBossInstance> _bosses;
	
	public static DayNightSpawnManager getInstance()
	{
		if (_instance == null)
		{
			_instance = new DayNightSpawnManager();
		}
		
		return _instance;
	}
	
	private DayNightSpawnManager()
	{
		_dayCreatures = new ArrayList<>();
		_nightCreatures = new ArrayList<>();
		_bosses = new FastMap<>();
	}
	
	public void addDayCreature(L2Spawn spawnDat)
	{
		_dayCreatures.add(spawnDat);
	}
	
	public void addNightCreature(L2Spawn spawnDat)
	{
		_nightCreatures.add(spawnDat);
	}
	
	public void spawnDayCreatures()
	{
		spawnCreatures(_nightCreatures, _dayCreatures, "night", "day");
	}
	
	public void spawnNightCreatures()
	{
		spawnCreatures(_dayCreatures, _nightCreatures, "day", "night");
	}
	
	/*
	 * Manage Spawn/Respawn Arg 1 : Map with L2NpcInstance must be unspawned Arg 2 : Map with L2NpcInstance must be spawned Arg 3 : String for log info for unspawned L2NpcInstance Arg 4 : String for log info for spawned L2NpcInstance
	 */
	private void spawnCreatures(List<L2Spawn> unSpawnCreatures, List<L2Spawn> spawnCreatures, String UnspawnLogInfo, String SpawnLogInfo)
	{
		try
		{
			if (!unSpawnCreatures.isEmpty())
			{
				int i = 0;
				for (L2Spawn spawn : unSpawnCreatures)
				{
					if (spawn == null)
					{
						continue;
					}
					
					spawn.stopRespawn();
					L2NpcInstance last = spawn.getLastSpawn();
					if (last != null)
					{
						last.deleteMe();
						i++;
					}
					spawn.decreaseCount(null);
				}
				LOG.info("DayNightSpawnManager: Removed " + i + " " + UnspawnLogInfo + " creatures");
			}
			
			int i = 0;
			for (L2Spawn spawnDat : spawnCreatures)
			{
				if (spawnDat == null)
				{
					continue;
				}
				
				spawnDat.startRespawn();
				spawnDat.doSpawn();
				i++;
			}
			
			LOG.info("DayNightSpawnManager: Spawned " + i + " " + SpawnLogInfo + " creatures");
		}
		catch (Exception e)
		{
			LOG.warn("Error while spawning creatures: " + e.getMessage(), e);
		}
	}
	
	private void changeMode(int mode)
	{
		if (_nightCreatures.size() == 0 && _dayCreatures.size() == 0)
		{
			return;
		}
		
		switch (mode)
		{
			case 0:
				spawnDayCreatures();
				specialNightBoss(0);
				ShadowSenseMsg(0);
				break;
			case 1:
				spawnNightCreatures();
				specialNightBoss(1);
				ShadowSenseMsg(1);
				break;
			default:
				LOG.warn("DayNightSpawnManager: Wrong mode sent");
				break;
		}
	}
	
	public void notifyChangeMode()
	{
		try
		{
			if (GameTimeController.getInstance().isNight())
			{
				changeMode(1);
			}
			else
			{
				changeMode(0);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public void DataSave()
	{
		_nightCreatures.clear();
		_dayCreatures.clear();
		_bosses.clear();
	}
	
	private void specialNightBoss(int mode)
	{
		try
		{
			for (L2Spawn spawn : _bosses.keySet())
			{
				L2RaidBossInstance boss = _bosses.get(spawn);
				
				if (boss == null && mode == 1)
				{
					boss = (L2RaidBossInstance) spawn.doSpawn();
					RaidBossSpawnManager.getInstance().notifySpawnNightBoss(boss);
					_bosses.remove(spawn);
					_bosses.put(spawn, boss);
					continue;
				}
				
				if (boss == null && mode == 0)
				{
					continue;
				}
				
				if ((boss != null) && (boss.getNpcId() == 25328) && boss.getRaidStatus().equals(RaidBossSpawnManager.StatusEnum.ALIVE))
				{
					handleHellmans(boss, mode);
				}
				
				return;
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	private static void handleHellmans(L2RaidBossInstance boss, int mode)
	{
		switch (mode)
		{
			case 0:
				boss.deleteMe();
				LOG.info("DayNightSpawnManager: Deleting Hellman raid boss");
				break;
			case 1:
				boss.spawnMe();
				LOG.info("DayNightSpawnManager: Spawning Hellman raid boss");
				break;
		}
	}
	
	private void ShadowSenseMsg(int mode)
	{
		final L2Skill skill = SkillTable.getInstance().getInfo(294, 1);
		if (skill == null)
		{
			return;
		}
		
		final SystemMessageId msg = (mode == 1 ? SystemMessageId.NIGHT_EFFECT_APPLIES : SystemMessageId.DAY_EFFECT_DISAPPEARS);
		final Collection<L2PcInstance> pls = L2World.getInstance().getAllPlayers().values();
		for (L2PcInstance onlinePlayer : pls)
		{
			if (onlinePlayer.getRace().ordinal() == 2 && onlinePlayer.getSkillLevel(294) > 0)
			{
				SystemMessage sm = SystemMessage.getSystemMessage(msg);
				sm.addSkillName(294);
				onlinePlayer.sendPacket(sm);
			}
		}
	}
	
	public L2RaidBossInstance handleBoss(L2Spawn spawnDat)
	{
		if (_bosses.containsKey(spawnDat))
		{
			return _bosses.get(spawnDat);
		}
		
		if (GameTimeController.getInstance().isNight())
		{
			L2RaidBossInstance raidboss = (L2RaidBossInstance) spawnDat.doSpawn();
			_bosses.put(spawnDat, raidboss);
			
			return raidboss;
		}
		
		_bosses.put(spawnDat, null);
		return null;
	}
}
