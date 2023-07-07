/*
 * Copyright (C) 2004-2013 L2J Server
 * 
 * This file is part of L2J Server.
 * 
 * L2J Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * L2J Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package l2jorion.game.model.entity.event.tournament;

import java.util.List;

import l2jorion.Config;
import l2jorion.game.cache.HtmCache;
import l2jorion.game.datatables.sql.NpcTable;
import l2jorion.game.datatables.sql.SpawnTable;
import l2jorion.game.handler.ICustomByPassHandler;
import l2jorion.game.handler.IVoicedCommandHandler;
import l2jorion.game.model.L2Party;
import l2jorion.game.model.L2World;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.base.ClassId;
import l2jorion.game.model.entity.Announcements;
import l2jorion.game.model.entity.event.manager.EventTask;
import l2jorion.game.model.olympiad.OlympiadManager;
import l2jorion.game.model.spawn.L2Spawn;
import l2jorion.game.network.serverpackets.MagicSkillUser;
import l2jorion.game.network.serverpackets.NpcHtmlMessage;
import l2jorion.game.templates.L2Item;
import l2jorion.game.templates.L2NpcTemplate;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;

public class Tournament implements EventTask, IVoicedCommandHandler, ICustomByPassHandler
{
	private static Logger LOG = LoggerFactory.getLogger(Tournament.class);
	
	private static String _eventName = Config.TM_NAME;
	private static String _eventDesc = Config.TM_DESC;
	private static String _joiningLocationName = Config.TM_JOIN_LOC;
	
	private static L2Spawn _npcSpawn;
	
	private static boolean _joining = false, _aborted = false, _inProgress = false;
	
	private static int _npcId = Config.TM_NPCID;
	
	private static int _joinTime = Config.TM_EVENT_TIME;
	
	private String startEventTime;
	
	public static Tournament getNewInstance()
	{
		return new Tournament();
	}
	
	@Override
	public boolean useVoicedCommand(String command, L2PcInstance activeChar, String target)
	{
		if (activeChar == null)
		{
			return false;
		}
		
		if (command.equalsIgnoreCase("tmevent"))
		{
			showMessageWindow(activeChar);
			return true;
		}
		
		return false;
	}
	
	@Override
	public void handleCommand(String command, L2PcInstance player, String parameters)
	{
		if (!is_inProgress())
		{
			player.sendMessage("The event did not start yet.");
			return;
		}
		
		if (player._active_boxes > 1 && !Config.ALLOW_DUALBOX_EVENT)
		{
			final List<String> players_in_boxes = player.active_boxes_characters;
			
			if (players_in_boxes != null && players_in_boxes.size() > 1)
			{
				for (final String character_name : players_in_boxes)
				{
					final L2PcInstance plyr = L2World.getInstance().getPlayer(character_name);
					
					if (plyr != null && plyr.isInArenaEvent())
					{
						player.sendMessage("You already participated in event with another character!");
						return;
					}
				}
			}
		}
		
		if (parameters.startsWith("1x1_register"))
		{
			// checks
			if (player.isCursedWeaponEquipped() || player.inObserverMode() || player.isInStoreMode() || !player.isNoble() || player.getKarma() > 0)
			{
				player.sendMessage("You don't not have the necessary requirements.");
				return;
			}
			
			// oly checks
			if (player.isInOlympiadMode() || OlympiadManager.getInstance().isRegistered(player))
			{
				player.sendMessage("You are registered in the Olympiad.");
				return;
			}
			
			// event checks
			if (player.isInFunEvent())
			{
				player.sendMessage("You are registered in another event.");
				return;
			}
			
			if (player.getClassId() == ClassId.shillenElder || player.getClassId() == ClassId.shillienSaint || player.getClassId() == ClassId.bishop || player.getClassId() == ClassId.cardinal || player.getClassId() == ClassId.elder || player.getClassId() == ClassId.evaSaint)
			{
				player.sendMessage("Your class is not allowed in tournament.");
				return;
			}
			
			if (Arena1x1.getInstance().register(player))
			{
				player.sendMessage(player.getName() + " Bring up your sword! Your are registered!");
				
				player.setArenaProtection(true);
			}
			else
			{
				return;
			}
		}
		else if (parameters.startsWith("2x2_register"))
		{
			if (!player.isInParty())
			{
				player.sendMessage("You dont have a party.");
				return;
			}
			
			if (!player.getParty().isLeader(player))
			{
				player.sendMessage("You are not the party leader!");
				return;
			}
			
			L2PcInstance assist = player.getParty().getPartyMembers().get(1);
			
			// checks
			if (player.isCursedWeaponEquipped() || assist.isCursedWeaponEquipped() || player.inObserverMode() || assist.inObserverMode() || player.isInStoreMode() || assist.isInStoreMode() || !player.isNoble() || !assist.isNoble() || player.getKarma() > 0 || assist.getKarma() > 0)
			{
				player.sendMessage("You or your member does not have the necessary requirements.");
				assist.sendMessage("You or your member does not have the necessary requirements.");
				return;
			}
			
			// oly checks
			if (player.isInOlympiadMode() || assist.isInOlympiadMode() || OlympiadManager.getInstance().isRegistered(player) || OlympiadManager.getInstance().isRegistered(assist))
			{
				player.sendMessage("You or your member is registered in the Olympiad.");
				assist.sendMessage("You or your member is registered in the Olympiad.");
				return;
			}
			
			// event checks
			if (player.isInFunEvent() || assist.isInFunEvent())
			{
				player.sendMessage("You or your member is registered in another event.");
				assist.sendMessage("You or your member is registered in another event.");
				return;
			}
			
			if (assist.getClassId() == ClassId.shillenElder || assist.getClassId() == ClassId.shillienSaint || assist.getClassId() == ClassId.bishop || assist.getClassId() == ClassId.cardinal || assist.getClassId() == ClassId.elder || assist.getClassId() == ClassId.evaSaint)
			{
				assist.sendMessage("You or your member class is not allowed in tournament.");
				player.sendMessage("You or your member class is not allowed in tournament.");
				return;
			}
			
			if (player.getClassId() == ClassId.shillenElder || player.getClassId() == ClassId.shillienSaint || player.getClassId() == ClassId.bishop || player.getClassId() == ClassId.cardinal || player.getClassId() == ClassId.elder || player.getClassId() == ClassId.evaSaint)
			{
				assist.sendMessage("You or your member class is not allowed in tournament.");
				player.sendMessage("You or your member class is not allowed in tournament.");
				return;
			}
			
			if (player.getClassId().getId() == assist.getClassId().getId())
			{
				player.sendMessage("Same class partner are not allowed.");
				assist.sendMessage("Same class partner are not allowed.");
				return;
			}
			
			if (Arena2x2.getInstance().register(player, assist))
			{
				player.sendMessage(player.getName() + " Bring up your sword! Your party is registered!");
				assist.sendMessage(assist.getName() + " Bring up your sword! Your party is registered!");
				
				player.setArenaProtection(true);
				assist.setArenaProtection(true);
			}
			else
			{
				return;
			}
		}
		else if (parameters.startsWith("4x4_register"))
		{
			L2Party party = player.getParty();
			
			if (!player.isInParty())
			{
				player.sendMessage("You dont have a party.");
				return;
			}
			
			if (!player.getParty().isLeader(player))
			{
				player.sendMessage("You are not the party leader!");
				return;
			}
			
			if (party.getMemberCount() < 3)
			{
				player.sendMessage("You need party with at 3 members to register!");
				return;
			}
			
			// 4 Player + 1 Leader
			L2PcInstance assist = player.getParty().getPartyMembers().get(1);
			L2PcInstance assist2 = player.getParty().getPartyMembers().get(2);
			L2PcInstance assist3 = player.getParty().getPartyMembers().get(3);
			
			// checks
			if (player.isCursedWeaponEquipped() || assist.isCursedWeaponEquipped() || assist2.isCursedWeaponEquipped() || assist3.isCursedWeaponEquipped() || player.inObserverMode() || assist.inObserverMode() || assist2.inObserverMode() || assist3.inObserverMode() || player.isInStoreMode()
				|| assist.isInStoreMode() || assist2.isInStoreMode() || assist3.isInStoreMode() || !player.isNoble() || !assist.isNoble() || !assist2.isNoble() || !assist3.isNoble() || player.getKarma() > 0 || assist.getKarma() > 0 || assist2.getKarma() > 0 || assist3.getKarma() > 0)
			{
				player.sendMessage("You or your member does not have the necessary requirements.");
				assist.sendMessage("You or your member does not have the necessary requirements.");
				assist2.sendMessage("You or your member does not have the necessary requirements.");
				assist3.sendMessage("You or your member does not have the necessary requirements.");
				return;
			}
			
			// oly checks
			if (player.isInOlympiadMode() || assist.isInOlympiadMode() || OlympiadManager.getInstance().isRegistered(player) || OlympiadManager.getInstance().isRegistered(assist) || assist2.isInOlympiadMode() || assist3.isInOlympiadMode() || OlympiadManager.getInstance().isRegistered(assist2)
				|| OlympiadManager.getInstance().isRegistered(assist3))
			{
				player.sendMessage("You or your member is registered in the Olympiad.");
				assist.sendMessage("You or your member is registered in the Olympiad.");
				assist2.sendMessage("You or your member is registered in the Olympiad.");
				assist3.sendMessage("You or your member is registered in the Olympiad.");
				return;
			}
			
			// event checks
			if (player.isInFunEvent() || assist.isInFunEvent() || assist2.isInFunEvent() || assist3.isInFunEvent())
			{
				player.sendMessage("You or your member is registered in another event.");
				assist.sendMessage("You or your member is registered in another event.");
				assist2.sendMessage("You or your member is registered in another event.");
				assist3.sendMessage("You or your member is registered in another event.");
				return;
			}
			
			// class
			if (player.getClassId().getId() == assist.getClassId().getId() || player.getClassId().getId() == assist2.getClassId().getId() || player.getClassId().getId() == assist3.getClassId().getId() || assist.getClassId().getId() == assist2.getClassId().getId()
				|| assist.getClassId().getId() == assist3.getClassId().getId() || assist2.getClassId().getId() == assist.getClassId().getId() || assist2.getClassId().getId() == assist3.getClassId().getId())
			{
				player.sendMessage("Same class partner are not allowed.");
				assist.sendMessage("Same class partner are not allowed.");
				assist2.sendMessage("Same class partner are not allowed.");
				assist3.sendMessage("Same class partner are not allowed.");
				return;
			}
			
			// Register party
			if (Arena4x4.getInstance().register(player, assist, assist2, assist3))
			{
				player.sendMessage(player.getName() + " Bring up your sword! Your party is registered!");
				assist.sendMessage(assist.getName() + " Bring up your sword! Your party is registered!");
				assist2.sendMessage(assist2.getName() + " Bring up your sword! Your party is registered!");
				assist3.sendMessage(assist3.getName() + " Bring up your sword! Your party is registered!");
			}
			else
			{
				return;
			}
		}
		else if (parameters.startsWith("9x9_register"))
		{
			L2Party party = player.getParty();
			
			if (!player.isInParty())
			{
				player.sendMessage("You dont have a party.");
				return;
			}
			
			if (!player.getParty().isLeader(player))
			{
				player.sendMessage("You are not the party leader!");
				return;
			}
			
			if (party.getMemberCount() < 8)
			{
				player.sendMessage("You need party with at 9 members to register!");
				return;
			}
			
			// 8 Player + 1 Leader
			L2PcInstance assist1 = player.getParty().getPartyMembers().get(1);
			L2PcInstance assist2 = player.getParty().getPartyMembers().get(2);
			L2PcInstance assist3 = player.getParty().getPartyMembers().get(3);
			L2PcInstance assist4 = player.getParty().getPartyMembers().get(4);
			L2PcInstance assist5 = player.getParty().getPartyMembers().get(5);
			L2PcInstance assist6 = player.getParty().getPartyMembers().get(6);
			L2PcInstance assist7 = player.getParty().getPartyMembers().get(7);
			L2PcInstance assist8 = player.getParty().getPartyMembers().get(8);
			
			// checks
			if (player.isCursedWeaponEquipped() || assist1.isCursedWeaponEquipped() || assist2.isCursedWeaponEquipped() || assist3.isCursedWeaponEquipped() || assist4.isCursedWeaponEquipped() || assist5.isCursedWeaponEquipped() || assist6.isCursedWeaponEquipped() || assist7.isCursedWeaponEquipped()
				|| assist8.isCursedWeaponEquipped() || player.inObserverMode() || assist1.inObserverMode() || assist2.inObserverMode() || assist3.inObserverMode() || assist4.inObserverMode() || assist5.inObserverMode() || assist6.inObserverMode() || assist7.inObserverMode()
				|| assist8.inObserverMode() || player.isInStoreMode() || assist1.isInStoreMode() || assist2.isInStoreMode() || assist3.isInStoreMode() || assist4.isInStoreMode() || assist5.isInStoreMode() || assist6.isInStoreMode() || assist7.isInStoreMode() || assist8.isInStoreMode()
				|| !player.isNoble() || !assist1.isNoble() || !assist2.isNoble() || !assist3.isNoble() || !assist4.isNoble() || !assist5.isNoble() || !assist6.isNoble() || !assist7.isNoble() || !assist8.isNoble() || player.getKarma() > 0 || assist1.getKarma() > 0 || assist2.getKarma() > 0
				|| assist3.getKarma() > 0 || assist4.getKarma() > 0 || assist5.getKarma() > 0 || assist6.getKarma() > 0 || assist7.getKarma() > 0 || assist8.getKarma() > 0)
			{
				player.sendMessage("You or your member does not have the necessary requirements.");
				assist1.sendMessage("You or your member does not have the necessary requirements.");
				assist2.sendMessage("You or your member does not have the necessary requirements.");
				assist3.sendMessage("You or your member does not have the necessary requirements.");
				assist4.sendMessage("You or your member does not have the necessary requirements.");
				assist5.sendMessage("You or your member does not have the necessary requirements.");
				assist6.sendMessage("You or your member does not have the necessary requirements.");
				assist7.sendMessage("You or your member does not have the necessary requirements.");
				assist8.sendMessage("You or your member does not have the necessary requirements.");
				return;
			}
			
			// oly checks
			if (player.isInOlympiadMode() || assist1.isInOlympiadMode() || assist2.isInOlympiadMode() || assist3.isInOlympiadMode() || assist4.isInOlympiadMode() || assist5.isInOlympiadMode() || assist6.isInOlympiadMode() || assist7.isInOlympiadMode() || assist8.isInOlympiadMode()
				|| OlympiadManager.getInstance().isRegistered(player) || OlympiadManager.getInstance().isRegistered(assist1) || OlympiadManager.getInstance().isRegistered(assist2) || OlympiadManager.getInstance().isRegistered(assist3) || OlympiadManager.getInstance().isRegistered(assist4)
				|| OlympiadManager.getInstance().isRegistered(assist5) || OlympiadManager.getInstance().isRegistered(assist6) || OlympiadManager.getInstance().isRegistered(assist7) || OlympiadManager.getInstance().isRegistered(assist8))
			{
				player.sendMessage("You or your member is registered in the Olympiad.");
				assist1.sendMessage("You or your member is registered in the Olympiad.");
				assist2.sendMessage("You or your member is registered in the Olympiad.");
				assist3.sendMessage("You or your member is registered in the Olympiad.");
				assist4.sendMessage("You or your member is registered in the Olympiad.");
				assist5.sendMessage("You or your member is registered in the Olympiad.");
				assist6.sendMessage("You or your member is registered in the Olympiad.");
				assist7.sendMessage("You or your member is registered in the Olympiad.");
				assist8.sendMessage("You or your member is registered in the Olympiad.");
				return;
			}
			
			// event checks
			if (player.isInFunEvent() || assist1.isInFunEvent() || assist2.isInFunEvent() || assist3.isInFunEvent() || assist4.isInFunEvent() || assist5.isInFunEvent() || assist6.isInFunEvent() || assist7.isInFunEvent() || assist8.isInFunEvent())
			{
				player.sendMessage("You or your member is registered in another event.");
				assist1.sendMessage("You or your member is registered in another event.");
				assist2.sendMessage("You or your member is registered in another event.");
				assist3.sendMessage("You or your member is registered in another event.");
				assist4.sendMessage("You or your member is registered in another event.");
				assist5.sendMessage("You or your member is registered in another event.");
				assist6.sendMessage("You or your member is registered in another event.");
				assist7.sendMessage("You or your member is registered in another event.");
				assist8.sendMessage("You or your member is registered in another event.");
				return;
			}
			
			// Register party
			if (Arena9x9.getInstance().register(player, assist1, assist2, assist3, assist4, assist5, assist6, assist7, assist8))
			{
				player.sendMessage(player.getName() + " Bring up your sword! Your party is registered!");
				assist1.sendMessage(assist1.getName() + " Bring up your sword! Your party is registered!");
				assist2.sendMessage(assist2.getName() + " Bring up your sword! Your party is registered!");
				assist3.sendMessage(assist3.getName() + " Bring up your sword! Your party is registered!");
				assist4.sendMessage(assist4.getName() + " Bring up your sword! Your party is registered!");
				assist5.sendMessage(assist5.getName() + " Bring up your sword! Your party is registered!");
				assist6.sendMessage(assist6.getName() + " Bring up your sword! Your party is registered!");
				assist7.sendMessage(assist7.getName() + " Bring up your sword! Your party is registered!");
				assist8.sendMessage(assist8.getName() + " Bring up your sword! Your party is registered!");
			}
			else
			{
				return;
			}
		}
		else if (parameters.startsWith("remove"))
		{
			Arena2x2.getInstance().remove(player);
		}
	}
	
	public static void showMessageWindow(L2PcInstance player)
	{
		NpcHtmlMessage htm = new NpcHtmlMessage(player.getLastQuestNpcObject());
		String text = HtmCache.getInstance().getHtm("data/html/mods/tournament/main.htm");
		htm.setHtml(text);
		htm.replace("%reward1x1icon%", String.valueOf(L2Item.getItemIcon(Config.ARENA_REWARD_ID_1X1)));
		htm.replace("%reward1x1item%", String.valueOf(L2Item.getItemNameById(Config.ARENA_REWARD_ID_1X1)));
		htm.replace("%reward1x1itemcount%", String.valueOf(Config.ARENA_REWARD_COUNT_1X1));
		
		htm.replace("%reward2x2icon%", String.valueOf(L2Item.getItemIcon(Config.ARENA_REWARD_ID_2X2)));
		htm.replace("%reward2x2item%", String.valueOf(L2Item.getItemNameById(Config.ARENA_REWARD_ID_2X2)));
		htm.replace("%reward2x2itemcount%", String.valueOf(Config.ARENA_REWARD_COUNT_2X2));
		
		htm.replace("%reward4x4icon%", String.valueOf(L2Item.getItemIcon(Config.ARENA_REWARD_ID_4X4)));
		htm.replace("%reward4x4item%", String.valueOf(L2Item.getItemNameById(Config.ARENA_REWARD_ID_4X4)));
		htm.replace("%reward4x4itemcount%", String.valueOf(Config.ARENA_REWARD_COUNT_4X4));
		
		htm.replace("%reward9x9icon%", String.valueOf(L2Item.getItemIcon(Config.ARENA_REWARD_ID_9X9)));
		htm.replace("%reward9x9item%", String.valueOf(L2Item.getItemNameById(Config.ARENA_REWARD_ID_9X9)));
		htm.replace("%reward9x9itemcount%", String.valueOf(Config.ARENA_REWARD_COUNT_9X9));
		
		player.sendPacket(htm);
	}
	
	public void setEventStartTime(final String newTime)
	{
		startEventTime = newTime;
	}
	
	@Override
	public String getEventIdentifier()
	{
		return _eventName;
	}
	
	@Override
	public void run()
	{
		LOG.info("Tournament: Event notification start");
		eventOnceStart();
	}
	
	public static String get_eventName()
	{
		return _eventName;
	}
	
	@Override
	public String getEventStartTime()
	{
		return startEventTime;
	}
	
	public static void eventOnceStart()
	{
		if (startJoin() && !_aborted)
		{
			if (_joinTime > 0)
			{
				waiter(_joinTime * 60 * 1000); // minutes to join event
			}
			else if (_joinTime <= 0)
			{
				abortEvent();
				return;
			}
		}
	}
	
	public static void abortEvent()
	{
		if (!_joining)
		{
			return;
		}
		
		if (_joining)
		{
			unspawnEventNpc();
			cleanTM();
			_joining = false;
			_inProgress = false;
			Announcements.getInstance().gameAnnounceToAll(_eventName + ": The Event finished!");
			return;
		}
		
		_joining = false;
		_aborted = true;
		unspawnEventNpc();
		
		Announcements.getInstance().gameAnnounceToAll(_eventName + ": The Event finished!");
	}
	
	public static boolean startJoin()
	{
		_inProgress = true;
		_joining = true;
		spawnEventNpc();
		Announcements.getInstance().gameAnnounceToAll(_eventName + ": " + _eventDesc);
		Announcements.getInstance().gameAnnounceToAll(_eventName + ": Joinable in " + _joiningLocationName);
		Announcements.getInstance().gameAnnounceToAll(_eventName + ": Event time " + (Config.TM_EVENT_TIME) + " minutes");
		Announcements.getInstance().gameAnnounceToAll(_eventName + ": Event info command .tmevent");
		
		if (Config.L2UNLIMITED_CUSTOM)
		{
			for (L2PcInstance player : L2World.getInstance().getAllPlayers().values())
			{
				if (player != null)
				{
					if (player.isOnline() != 0)
					{
						showMessageWindow(player);
					}
				}
			}
		}
		
		return true;
	}
	
	private static void spawnEventNpc()
	{
		if (!Config.TM_AUTO_SPAWN_NPC)
		{
			return;
		}
		
		L2NpcTemplate tmpl = NpcTable.getInstance().getTemplate(_npcId);
		String[] coord = Config.TM_NPC_COORD.split(",");
		
		try
		{
			_npcSpawn = new L2Spawn(tmpl);
			
			_npcSpawn.setLocx(Integer.parseInt(coord[0]));
			_npcSpawn.setLocy(Integer.parseInt(coord[1]));
			_npcSpawn.setLocz(Integer.parseInt(coord[2]));
			_npcSpawn.setAmount(1);
			_npcSpawn.setHeading(Integer.parseInt(coord[3]));
			_npcSpawn.setRespawnDelay(1);
			
			SpawnTable.getInstance().addNewSpawn(_npcSpawn, false);
			
			_npcSpawn.init();
			_npcSpawn.getLastSpawn().getStatus().setCurrentHp(999999999);
			_npcSpawn.getLastSpawn().setTitle(_eventName);
			_npcSpawn.getLastSpawn()._isEventMobTM = true;
			_npcSpawn.getLastSpawn().isAggressive();
			_npcSpawn.getLastSpawn().decayMe();
			_npcSpawn.getLastSpawn().spawnMe(_npcSpawn.getLastSpawn().getX(), _npcSpawn.getLastSpawn().getY(), _npcSpawn.getLastSpawn().getZ());
			
			_npcSpawn.getLastSpawn().broadcastPacket(new MagicSkillUser(_npcSpawn.getLastSpawn(), _npcSpawn.getLastSpawn(), 1034, 1, 1, 1));
		}
		catch (Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			
			LOG.warn(_eventName + " Engine[spawnEventNpc(exception: " + e.getMessage());
		}
	}
	
	private static void unspawnEventNpc()
	{
		if (!Config.TM_AUTO_SPAWN_NPC)
		{
			return;
		}
		
		if (_npcSpawn == null || _npcSpawn.getLastSpawn() == null)
		{
			return;
		}
		
		_npcSpawn.getLastSpawn().deleteMe();
		_npcSpawn.stopRespawn();
		SpawnTable.getInstance().deleteSpawn(_npcSpawn, true);
	}
	
	private static void waiter(final long interval)
	{
		final long startWaiterTime = System.currentTimeMillis();
		int seconds = (int) (interval / 1000);
		
		while (startWaiterTime + interval > System.currentTimeMillis() && !_aborted)
		{
			seconds--; // Here because we don't want to see two time announce at the same time
			
			if (_joining)
			{
				switch (seconds)
				{
					case 3600: // 1 hour left
						if (_joining)
						{
							Announcements.getInstance().gameAnnounceToAll(_eventName + ": Joinable in " + _joiningLocationName + "!");
							Announcements.getInstance().gameAnnounceToAll(_eventName + ": " + seconds / 60 / 60 + " hour(s) till event finish!");
						}
						break;
					case 1800: // 30 minutes left
					case 900: // 15 minutes left
					case 600: // 10 minutes left
					case 300: // 5 minutes left
					case 240: // 4 minutes left
					case 180: // 3 minutes left
					case 120: // 2 minutes left
						if (_joining)
						{
							Announcements.getInstance().gameAnnounceToAll(_eventName + ": Joinable in " + _joiningLocationName + "!");
							Announcements.getInstance().gameAnnounceToAll(_eventName + ": " + seconds / 60 + " minutes till event finish!");
						}
						break;
					case 60: // 1 minute left
						
						if (_joining)
						{
							Announcements.getInstance().gameAnnounceToAll(_eventName + ": Joinable in " + _joiningLocationName + "!");
							Announcements.getInstance().gameAnnounceToAll(_eventName + ": " + seconds / 60 + " minute till event finish!");
						}
						
						break;
					case 30: // 30 seconds left
					case 15: // 15 seconds left
					case 10: // 10 seconds left
					case 3: // 3 seconds left
					case 2: // 2 seconds left
						if (_joining)
						{
							Announcements.getInstance().gameAnnounceToAll(_eventName + ": " + seconds + " seconds till event finish!");
						}
						
						break;
					case 1: // 1 seconds left
						if (_joining)
						{
							Announcements.getInstance().gameAnnounceToAll(_eventName + ": " + seconds + " second till event finish!");
						}
						break;
					case 0:
						abortEvent();
						break;
				}
			}
			
			final long startOneSecondWaiterStartTime = System.currentTimeMillis();
			
			// Only the try catch with Thread.sleep(1000) give bad countdown on high wait times
			while (startOneSecondWaiterStartTime + 1000 > System.currentTimeMillis())
			{
				try
				{
					Thread.sleep(1);
				}
				catch (final InterruptedException ie)
				{
					if (Config.ENABLE_ALL_EXCEPTIONS)
					{
						ie.printStackTrace();
					}
				}
			}
		}
	}
	
	public static void cleanTM()
	{
		_inProgress = false;
	}
	
	public static boolean is_inProgress()
	{
		return _inProgress;
	}
	
	@Override
	public String[] getByPassCommands()
	{
		return new String[]
		{
			"tm"
		};
	}
	
	@Override
	public String[] getVoicedCommandList()
	{
		return new String[]
		{
			"tmevent"
		};
	}
}