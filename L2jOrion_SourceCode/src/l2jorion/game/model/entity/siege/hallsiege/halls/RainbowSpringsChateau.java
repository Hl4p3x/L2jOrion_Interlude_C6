/*
 * Copyright (C) 2004-2016 L2J DataPack
 * 
 * This file is part of L2J DataPack.
 * 
 * L2J DataPack is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * L2J DataPack is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package l2jorion.game.model.entity.siege.hallsiege.halls;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

import l2jorion.Config;
import l2jorion.game.cache.HtmCache;
import l2jorion.game.datatables.csv.MapRegionTable.TeleportWhereType;
import l2jorion.game.datatables.sql.ClanTable;
import l2jorion.game.datatables.sql.SpawnTable;
import l2jorion.game.managers.CHSiegeManager;
import l2jorion.game.managers.ZoneManager;
import l2jorion.game.model.L2Character;
import l2jorion.game.model.L2Clan;
import l2jorion.game.model.L2Party;
import l2jorion.game.model.L2Skill;
import l2jorion.game.model.Location;
import l2jorion.game.model.actor.instance.L2ItemInstance;
import l2jorion.game.model.actor.instance.L2NpcInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.entity.siege.hallsiege.ClanHallSiegeEngine;
import l2jorion.game.model.entity.siege.hallsiege.SiegableHall;
import l2jorion.game.model.entity.siege.hallsiege.SiegeStatus;
import l2jorion.game.model.quest.Quest;
import l2jorion.game.model.quest.QuestState;
import l2jorion.game.model.spawn.L2Spawn;
import l2jorion.game.model.zone.type.L2ClanHallZone;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.clientpackets.Say2;
import l2jorion.game.network.serverpackets.NpcSay;
import l2jorion.game.network.serverpackets.SystemMessage;
import l2jorion.game.thread.ThreadPoolManager;
import l2jorion.game.util.Broadcast;
import l2jorion.game.util.Util;
import l2jorion.util.CloseUtil;
import l2jorion.util.database.L2DatabaseFactory;
import l2jorion.util.random.Rnd;

public final class RainbowSpringsChateau extends ClanHallSiegeEngine
{
	private static final int RAINBOW_SPRINGS = 62;
	
	private static final int WAR_DECREES = 8034;
	
	private static final int RAINBOW_NECTAR = 8030;
	private static final int RAINBOW_MWATER = 8031;
	private static final int RAINBOW_WATER = 8032;
	private static final int RAINBOW_SULFUR = 8033;
	
	private static final int MESSENGER = 35604;
	private static final int COORDINATOR = 35603;
	
	protected static final int[] CHESTS =
	{
		35593,
		35594,
		35595
	};
	
	private static final int[] YETIS =
	{
		35596,
		35597,
		35598,
		35599
	};
	
	private static final int[] GOURDS =
	{
		35588,
		35589,
		35590,
		35591
	};
	
	private static L2Spawn[] _gourds = new L2Spawn[4];
	private static L2Spawn[] _yetis = new L2Spawn[4];
	
	private static final Location[] ARENAS = new Location[]
	{
		new Location(151562, -127080, -2214), // Arena 1
		new Location(153141, -125335, -2214), // Arena 2
		new Location(153892, -127530, -2214), // Arena 3
		new Location(155657, -125752, -2214), // Arena 4
	};
	
	protected static final int[] LETTERS =
	{
		8035, // A
		8036, // B
		8037, // C
		8038, // D
		8039, // E
		8040, // F
		8041, // G
		8042, // H
		8043, // I
		8044, // J
		8045, // K
		8046, // L
		8047, // N
		8048, // O
		8049, // P
		8050, // R
		8051, // S
		8052, // T
		8053, // U
		8054, // W
		8055 // Y
	
	};
	
	private static final String[] _textPassages =
	{
		"Fight for Rainbow Springs!",
		"Are you a match for the Yetti?",
		"Did somebody order a knuckle sandwich?"
	};
	
	private static final L2Skill[] DEBUFFS = {};
	
	protected static Map<Integer, Integer> _warDecreesCount = new HashMap<>();
	protected static List<L2Clan> _acceptedClans = new ArrayList<>(4);
	private static Map<String, ArrayList<L2Clan>> _usedTextPassages = new HashMap<>();
	private static Map<L2Clan, Integer> _pendingItemToGet = new HashMap<>();
	
	protected static SiegableHall _rainbow;
	protected static ScheduledFuture<?> _nextSiege, _siegeEnd;
	private static String _registrationEnds;
	
	public RainbowSpringsChateau()
	{
		super(RainbowSpringsChateau.class.getSimpleName(), "conquerablehalls", RAINBOW_SPRINGS);
		
		addStartNpc(MESSENGER);
		addFirstTalkId(MESSENGER);
		addTalkId(MESSENGER);
		
		addFirstTalkId(COORDINATOR);
		addTalkId(COORDINATOR);
		
		addFirstTalkId(YETIS);
		addTalkId(YETIS);
		
		for (int mob : GOURDS)
		{
			addEventId(mob, Quest.QuestEventType.ON_SPAWN);
			addEventId(mob, Quest.QuestEventType.ON_KILL);
		}
		
		registerMobs(YETIS, QuestEventType.ON_ITEM_USE);
		
		loadAttackers();
		
		_rainbow = CHSiegeManager.getInstance().getSiegableHall(RAINBOW_SPRINGS);
		if (_rainbow != null)
		{
			long delay = _rainbow.getNextSiegeTime();
			// long delay = 10000;
			
			if (delay > -1)
			{
				setRegistrationEndString(delay - 3600000);
				
				_nextSiege = ThreadPoolManager.getInstance().scheduleGeneral(new SetFinalAttackers(), delay);
			}
			else
			{
				LOG.warn("CHSiegeManager: No Date setted for RainBow Springs Chateau Clan hall siege!. SIEGE CANCELED!");
			}
		}
	}
	
	@Override
	public String onFirstTalk(L2NpcInstance npc, L2PcInstance player)
	{
		QuestState st = player.getQuestState(RainbowSpringsChateau.class.getSimpleName());
		if (st == null)
		{
			st = newQuestState(player);
		}
		
		String html = null;
		final int npcId = npc.getNpcId();
		if (npcId == MESSENGER)
		{
			final String main = (_rainbow.getOwnerId() > 0) ? "messenger_yetti001.htm" : "messenger_yetti001a.htm";
			
			html = HtmCache.getInstance().getHtm("data/html/SiegableHall/RainbowSpringsChateau/" + main);
			
			html = html.replace("%time%", _registrationEnds);
			
			if (_rainbow.getOwnerId() > 0)
			{
				html = html.replace("%owner%", ClanTable.getInstance().getClan(_rainbow.getOwnerId()).getName());
			}
		}
		else if (npcId == COORDINATOR)
		{
			if (_rainbow.isInSiege())
			{
				html = HtmCache.getInstance().getHtm("data/html/SiegableHall/RainbowSpringsChateau/game_manager003.htm");
			}
			else
			{
				html = HtmCache.getInstance().getHtm("data/html/SiegableHall/RainbowSpringsChateau/game_manager001.htm");
			}
		}
		else if (Util.contains(YETIS, npcId))
		{
			if (_rainbow.isInSiege())
			{
				if (!player.isClanLeader())
				{
					html = HtmCache.getInstance().getHtm("data/html/SiegableHall/RainbowSpringsChateau/no_clan_leader.htm");
				}
				else
				{
					L2Clan clan = player.getClan();
					if (_acceptedClans.contains(clan))
					{
						int index = _acceptedClans.indexOf(clan);
						if (npcId == YETIS[index])
						{
							html = HtmCache.getInstance().getHtm("data/html/SiegableHall/RainbowSpringsChateau/yeti_main.htm");
						}
					}
				}
			}
		}
		player.setLastQuestNpcObject(npc.getObjectId());
		return html;
	}
	
	@Override
	public String onAdvEvent(String event, L2NpcInstance npc, L2PcInstance player)
	{
		String html = HtmCache.getInstance().getHtm("data/html/SiegableHall/RainbowSpringsChateau/" + event);
		
		final L2Clan clan = player.getClan();
		switch (npc.getNpcId())
		{
			case MESSENGER:
				switch (event)
				{
					case "register":
						if (!player.isClanLeader())
						{
							html = HtmCache.getInstance().getHtm("data/html/SiegableHall/RainbowSpringsChateau/messenger_yetti010.htm");
						}
						else if ((clan.getHasCastle() > 0) || (clan.getHasFort() > 0) || (clan.getHasHideout() > 0))
						{
							html = HtmCache.getInstance().getHtm("data/html/SiegableHall/RainbowSpringsChateau/messenger_yetti012.htm");
						}
						else if (!_rainbow.isRegistering())
						{
							html = HtmCache.getInstance().getHtm("data/html/SiegableHall/RainbowSpringsChateau/messenger_yetti014.htm");
						}
						else if (_warDecreesCount.containsKey(clan.getClanId()))
						{
							HtmCache.getInstance().getHtm("data/html/SiegableHall/RainbowSpringsChateau/messenger_yetti013.htm");
						}
						
						else if ((clan.getLevel() < 3) || (clan.getMembersCount() < 5))
						{
							html = HtmCache.getInstance().getHtm("data/html/SiegableHall/RainbowSpringsChateau/messenger_yetti011.htm");
						}
						
						else
						{
							final L2ItemInstance warDecrees = player.getInventory().getItemByItemId(WAR_DECREES);
							if (warDecrees == null)
							{
								html = HtmCache.getInstance().getHtm("data/html/SiegableHall/RainbowSpringsChateau/messenger_yetti008.htm");
							}
							else
							{
								int count = warDecrees.getCount();
								_warDecreesCount.put(clan.getClanId(), count);
								player.destroyItem("Rainbow Springs Registration", warDecrees, npc, true);
								addAttacker(clan.getClanId(), count);
								html = HtmCache.getInstance().getHtm("data/html/SiegableHall/RainbowSpringsChateau/messenger_yetti009.htm");
							}
						}
						break;
					case "cancel":
						if (!player.isClanLeader())
						{
							html = HtmCache.getInstance().getHtm("data/html/SiegableHall/RainbowSpringsChateau/messenger_yetti010.htm");
						}
						else if (!_warDecreesCount.containsKey(clan.getClanId()))
						{
							html = HtmCache.getInstance().getHtm("data/html/SiegableHall/RainbowSpringsChateau/messenger_yetti016.htm");
						}
						else if (!_rainbow.isRegistering())
						{
							html = HtmCache.getInstance().getHtm("data/html/SiegableHall/RainbowSpringsChateau/messenger_yetti017.htm");
						}
						else
						{
							removeAttacker(clan.getClanId());
							html = HtmCache.getInstance().getHtm("data/html/SiegableHall/RainbowSpringsChateau/messenger_yetti018.htm");
						}
						break;
					case "unregister":
						if (_rainbow.isRegistering())
						{
							if (_warDecreesCount.containsKey(clan.getClanId()))
							{
								player.addItem("Rainbow Spring unregister", WAR_DECREES, _warDecreesCount.get(clan.getClanId()) / 2, npc, true);
								
								_warDecreesCount.remove(clan.getClanId());
								html = HtmCache.getInstance().getHtm("data/html/SiegableHall/RainbowSpringsChateau/messenger_yetti019.htm");
							}
							else
							{
								html = HtmCache.getInstance().getHtm("data/html/SiegableHall/RainbowSpringsChateau/messenger_yetti020.htm");
							}
						}
						else if (_rainbow.isWaitingBattle())
						{
							_acceptedClans.remove(clan);
							html = HtmCache.getInstance().getHtm("data/html/SiegableHall/RainbowSpringsChateau/messenger_yetti020.htm");
						}
						break;
				}
				break;
			case COORDINATOR:
				if (event.equals("portToArena"))
				{
					final L2Party party = player.getParty();
					if (clan == null)
					{
						html = HtmCache.getInstance().getHtm("data/html/SiegableHall/RainbowSpringsChateau/game_manager009.htm");
					}
					else if (!player.isClanLeader())
					{
						html = HtmCache.getInstance().getHtm("data/html/SiegableHall/RainbowSpringsChateau/game_manager004.htm");
					}
					else if (!player.isInParty())
					{
						html = HtmCache.getInstance().getHtm("data/html/SiegableHall/RainbowSpringsChateau/game_manager005.htm");
					}
					else if (party.getPartyLeaderOID() != player.getObjectId())
					{
						html = HtmCache.getInstance().getHtm("data/html/SiegableHall/RainbowSpringsChateau/game_manager006.htm");
					}
					else
					{
						final int clanId = player.getClanId();
						boolean nonClanMemberInParty = false;
						for (L2PcInstance member : party.getPartyMembers())
						{
							if (member.getClanId() != clanId)
							{
								nonClanMemberInParty = true;
								break;
							}
						}
						
						if (nonClanMemberInParty)
						{
							html = HtmCache.getInstance().getHtm("data/html/SiegableHall/RainbowSpringsChateau/game_manager007.htm");
						}
						else if (party.getMemberCount() < 5)
						{
							html = HtmCache.getInstance().getHtm("data/html/SiegableHall/RainbowSpringsChateau/game_manager008.htm");
						}
						else if ((clan.getHasCastle() > 0) || (clan.getHasFort() > 0) || (clan.getHasHideout() > 0))
						{
							html = HtmCache.getInstance().getHtm("data/html/SiegableHall/RainbowSpringsChateau/game_manager010.htm");
						}
						else if (clan.getLevel() < Config.CHS_CLAN_MINLEVEL)
						{
							html = HtmCache.getInstance().getHtm("data/html/SiegableHall/RainbowSpringsChateau/game_manager011.htm");
						}
						else if (!_acceptedClans.contains(clan))
						{
							html = HtmCache.getInstance().getHtm("data/html/SiegableHall/RainbowSpringsChateau/game_manager014.htm");
						}
						else
						{
							portToArena(player, _acceptedClans.indexOf(clan));
						}
					}
				}
				break;
		}
		
		if (event.startsWith("enterText"))
		{
			if (!_acceptedClans.contains(clan))
			{
				return null;
			}
			
			String[] split = event.split("_ ");
			if (split.length < 2)
			{
				return null;
			}
			
			final String passage = split[1];
			
			if (!isValidPassage(passage))
			{
				return null;
			}
			
			if (_usedTextPassages.containsKey(passage))
			{
				ArrayList<L2Clan> list = _usedTextPassages.get(passage);
				
				if (list.contains(clan))
				{
					html = HtmCache.getInstance().getHtm("data/html/SiegableHall/RainbowSpringsChateau/yeti_passage_used.htm");
				}
				else
				{
					list.add(clan);
					synchronized (_pendingItemToGet)
					{
						if (_pendingItemToGet.containsKey(clan))
						{
							int left = _pendingItemToGet.get(clan);
							++left;
							_pendingItemToGet.put(clan, left);
						}
						else
						{
							_pendingItemToGet.put(clan, 1);
						}
					}
					html = HtmCache.getInstance().getHtm("data/html/SiegableHall/RainbowSpringsChateau/yeti_item_exchange.htm");
				}
			}
		}
		// TODO: Rewrite this to prevent exploits...
		else if (event.startsWith("getItem"))
		{
			if (!_pendingItemToGet.containsKey(clan))
			{
				html = HtmCache.getInstance().getHtm("data/html/SiegableHall/RainbowSpringsChateau/yeti_cannot_exchange.htm");
			}
			
			int left = _pendingItemToGet.get(clan);
			if (left > 0)
			{
				int itemId = Integer.parseInt(event.split("_")[1]);
				player.addItem("Rainbow Spring Chateau Siege", itemId, 1, npc, true);
				--left;
				_pendingItemToGet.put(clan, left);
				html = HtmCache.getInstance().getHtm("data/html/SiegableHall/RainbowSpringsChateau/yeti_main.htm");
			}
			else
			{
				html = HtmCache.getInstance().getHtm("data/html/SiegableHall/RainbowSpringsChateau/yeti_cannot_exchange.htm");
			}
		}
		
		return html;
	}
	
	@Override
	public String onSpawn(L2NpcInstance npc)
	{
		switch (npc.getNpcId())
		{
			case 35588:
			case 35589:
			case 35590:
			case 3559:
				npc.setChampion(false);
				npc.setIsImobilised(true);
				npc.setIsAttackDisabled(true);
				break;
		}
		return super.onSpawn(npc);
	}
	
	@Override
	public String onKill(L2NpcInstance npc, L2PcInstance killer, boolean isSummon)
	{
		if (!_rainbow.isInSiege())
		{
			return null;
		}
		
		final L2Clan clan = killer.getClan();
		if ((clan == null) || !_acceptedClans.contains(clan))
		{
			return null;
		}
		
		final int npcId = npc.getNpcId();
		final int index = _acceptedClans.indexOf(clan);
		
		if (npcId == CHESTS[index])
		{
			shoutRandomText(npc);
		}
		else if (npcId == GOURDS[index])
		{
			synchronized (this)
			{
				if (_siegeEnd != null)
				{
					_siegeEnd.cancel(false);
				}
				
				ThreadPoolManager.getInstance().scheduleGeneral(new SiegeEnd(clan), 3000);
			}
		}
		
		return null;
	}
	
	@Override
	public String onItemUse(L2NpcInstance npc, L2PcInstance player, L2ItemInstance item)
	{
		if (!_rainbow.isInSiege())
		{
			return null;
		}
		
		if ((npc == null))
		{
			return null;
		}
		
		if (!isYetiTarget(npc.getNpcId()))
		{
			return null;
		}
		
		final L2Clan clan = player.getClan();
		if ((clan == null) || !_acceptedClans.contains(clan))
		{
			return null;
		}
		
		// Nectar must spawn the enraged yeti. Dunno if it makes any other thing
		// Also, the items must execute:
		// - Reduce gourd hpb ( reduceGourdHp(int, L2PcInstance) )
		// - Cast debuffs on enemy clans ( castDebuffsOnEnemies(int) )
		// - Change arena gourds ( moveGourds() )
		// - Increase gourd hp ( increaseGourdHp(int) )
		final int itemId = item.getItemId();
		
		if (itemId == RAINBOW_NECTAR)
		{
			reduceGourdHp(_acceptedClans.indexOf(clan), player);
		}
		else if (itemId == RAINBOW_MWATER)
		{
			increaseGourdHp(_acceptedClans.indexOf(clan));
		}
		else if (itemId == RAINBOW_WATER)
		{
			moveGourds();
		}
		else if (itemId == RAINBOW_SULFUR)
		{
			castDebuffsOnEnemies(clan.getName());
		}
		return null;
	}
	
	private void portToArena(L2PcInstance leader, int arena)
	{
		if ((arena < 0) || (arena > 3))
		{
			LOG.warn("RainbowSptringChateau siege: Wrong arena ID passed {}!", arena);
			return;
		}
		for (L2PcInstance pc : leader.getParty().getPartyMembers())
		{
			if (pc != null)
			{
				pc.stopAllEffects();
				if (pc.getPet() != null)
				{
					pc.getPet().unSummon(pc);
				}
				pc.teleToLocation(ARENAS[arena]);
			}
		}
	}
	
	protected static void spawnGourds()
	{
		for (int i = 0; i < _acceptedClans.size(); i++)
		{
			if (_gourds[i] == null)
			{
				try
				{
					_gourds[i] = new L2Spawn(GOURDS[i]);
					_gourds[i].setLocx(ARENAS[i].getX() + 150);
					_gourds[i].setLocy(ARENAS[i].getY() + 150);
					_gourds[i].setLocz(ARENAS[i].getZ());
					_gourds[i].setHeading(1);
					_gourds[i].setRespawnDelay(300000);
					_gourds[i].setAmount(1);
				}
				catch (Exception e)
				{
					LOG.warn("Unable to spawn guard for clan index {}!", i, e);
				}
			}
			
			SpawnTable.getInstance().addNewSpawn(_gourds[i], false);
			_gourds[i].init();
		}
	}
	
	protected static void unSpawnGourds()
	{
		for (int i = 0; i < _acceptedClans.size(); i++)
		{
			_gourds[i].getLastSpawn().deleteMe();
			SpawnTable.getInstance().deleteSpawn(_gourds[i], false);
		}
	}
	
	protected static void spawnYetis()
	{
		for (int i = 0; i < _acceptedClans.size(); i++)
		{
			if (_yetis[i] == null)
			{
				try
				{
					_yetis[i] = new L2Spawn(YETIS[i]);
					_yetis[i].setLocx(ARENAS[i].getX());
					_yetis[i].setLocy(ARENAS[i].getY());
					_yetis[i].setLocz(ARENAS[i].getZ());
					_yetis[i].setHeading(1);
					_yetis[i].setAmount(1);
				}
				catch (Exception e)
				{
					LOG.warn("Unable to spawn yetis for clan index {}!", i, e);
				}
			}
			SpawnTable.getInstance().addNewSpawn(_yetis[i], false);
			_yetis[i].init();
		}
	}
	
	protected static void unSpawnYetis()
	{
		for (int i = 0; i < _acceptedClans.size(); i++)
		{
			_yetis[i].getLastSpawn().deleteMe();
			SpawnTable.getInstance().deleteSpawn(_yetis[i], false);
		}
	}
	
	private static void moveGourds()
	{
		L2Spawn[] tempArray = _gourds;
		int iterator = _acceptedClans.size();
		for (int i = 0; i < iterator; i++)
		{
			L2Spawn oldSpawn = _gourds[(iterator - 1) - i];
			L2Spawn curSpawn = tempArray[i];
			
			_gourds[(iterator - 1) - i] = curSpawn;
			
			curSpawn.getLastSpawn().teleToLocation(oldSpawn.getLocx(), oldSpawn.getLocy(), oldSpawn.getLocz());
		}
	}
	
	private static void reduceGourdHp(int index, L2PcInstance player)
	{
		L2Spawn gourd = _gourds[index];
		gourd.getLastSpawn().reduceCurrentHp(10000, player);
	}
	
	private static void increaseGourdHp(int index)
	{
		L2Spawn gourd = _gourds[index];
		L2NpcInstance gourdNpc = gourd.getLastSpawn();
		gourdNpc.setCurrentHp(gourdNpc.getCurrentHp() + 1000);
	}
	
	private static void castDebuffsOnEnemies(String myClan)
	{
		for (L2Character chr : ZoneManager.getInstance().getZoneById(112081).getCharactersInside())
		{
			if (chr != null && chr instanceof L2PcInstance)
			{
				L2PcInstance player = (L2PcInstance) chr;
				for (L2Skill sk : DEBUFFS)
				{
					if (myClan.contains(player.getClan().getName()))
					{
						continue;
					}
					
					sk.getEffects(chr, chr);
				}
			}
		}
	}
	
	private static void shoutRandomText(L2NpcInstance npc)
	{
		int length = _textPassages.length;
		
		if (_usedTextPassages.size() >= length)
		{
			return;
		}
		
		int randomPos = getRandom(length);
		String message = _textPassages[randomPos];
		
		if (_usedTextPassages.containsKey(message))
		{
			shoutRandomText(npc);
		}
		else
		{
			_usedTextPassages.put(message, new ArrayList<L2Clan>());
			int shout = Say2.SHOUT;
			int objId = npc.getObjectId();
			NpcSay say = new NpcSay(objId, shout, npc.getNpcId(), message);
			npc.broadcastPacket(say);
		}
	}
	
	private static boolean isValidPassage(String text)
	{
		for (String st : _textPassages)
		{
			if (st.equalsIgnoreCase(text))
			{
				return true;
			}
		}
		return false;
	}
	
	private static boolean isYetiTarget(int npcId)
	{
		return Util.contains(YETIS, npcId);
	}
	
	private static void removeAttacker(int clanId)
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement ps = con.prepareStatement("DELETE FROM rainbowsprings_attacker_list WHERE clanId = ?");
			
			ps.setInt(1, clanId);
			ps.execute();
		}
		catch (Exception e)
		{
			LOG.warn("{}: Unable to remove attacker clan ID {} from database!", RainbowSpringsChateau.class.getSigners(), clanId, e);
		}
		finally
		{
			CloseUtil.close(con);
		}
	}
	
	private static void addAttacker(int clanId, long count)
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement ps = con.prepareStatement("INSERT INTO rainbowsprings_attacker_list VALUES (?,?)");
			
			ps.setInt(1, clanId);
			ps.setLong(2, count);
			ps.execute();
		}
		catch (Exception e)
		{
			LOG.warn("{}: Unable add attakers for clan ID {} and count {}!", RainbowSpringsChateau.class.getSigners(), clanId, count, e);
		}
		finally
		{
			CloseUtil.close(con);
		}
	}
	
	@Override
	public void loadAttackers()
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			Statement s = con.createStatement();
			ResultSet rset = s.executeQuery("SELECT * FROM rainbowsprings_attacker_list");
			
			while (rset.next())
			{
				_warDecreesCount.put(rset.getInt("clanId"), rset.getInt("war_decrees_count"));
			}
		}
		catch (Exception e)
		{
			LOG.warn("{}: Unable load attakers!", RainbowSpringsChateau.class.getSigners(), e);
		}
		finally
		{
			CloseUtil.close(con);
		}
	}
	
	protected static void setRegistrationEndString(long time)
	{
		Calendar c = Calendar.getInstance();
		c.setTime(new Date(time));
		int year = c.get(Calendar.YEAR);
		int month = c.get(Calendar.MONTH) + 1;
		int day = c.get(Calendar.DAY_OF_MONTH);
		int hour = c.get(Calendar.HOUR);
		int mins = c.get(Calendar.MINUTE);
		
		_registrationEnds = year + "-" + month + "-" + day + " " + hour + (mins < 10 ? ":0" : ":") + mins;
	}
	
	public static void launchSiege()
	{
		_nextSiege.cancel(false);
		ThreadPoolManager.getInstance().executeAi(new SiegeStart());
	}
	
	@Override
	public void endSiege()
	{
		if (_siegeEnd != null)
		{
			_siegeEnd.cancel(false);
		}
		
		ThreadPoolManager.getInstance().executeAi(new SiegeEnd(null));
	}
	
	public static void updateAdminDate(long date)
	{
		if (_rainbow == null)
		{
			_rainbow = CHSiegeManager.getInstance().getSiegableHall(RAINBOW_SPRINGS);
		}
		
		_rainbow.setNextSiegeDate(date);
		if (_nextSiege != null)
		{
			_nextSiege.cancel(true);
		}
		date -= 3600000;
		setRegistrationEndString(date);
		_nextSiege = ThreadPoolManager.getInstance().scheduleGeneral(new SetFinalAttackers(), _rainbow.getNextSiegeTime());
	}
	
	protected static class SetFinalAttackers implements Runnable
	{
		@Override
		public void run()
		{
			if (_rainbow == null)
			{
				_rainbow = CHSiegeManager.getInstance().getSiegableHall(RAINBOW_SPRINGS);
			}
			
			int spotLeft = 4;
			
			// needs owner? (_rainbow.getOwnerId() > 0)
			if (_rainbow.getOwnerId() >= 0)
			{
				L2Clan owner = ClanTable.getInstance().getClan(_rainbow.getOwnerId());
				if (owner != null)
				{
					_rainbow.free();
					owner.setHasHideout(0);
					_acceptedClans.add(owner);
					--spotLeft;
				}
				
				L2Clan clan = null;
				for (int i = 0; i < spotLeft; i++)
				{
					long counter = 0;
					
					for (int clanId : _warDecreesCount.keySet())
					{
						L2Clan actingClan = ClanTable.getInstance().getClan(clanId);
						if ((actingClan == null) || (actingClan.getDissolvingExpiryTime() > 0))
						{
							_warDecreesCount.remove(clanId);
							continue;
						}
						
						final long count = _warDecreesCount.get(clanId);
						if (count > counter)
						{
							counter = count;
							clan = actingClan;
						}
					}
				}
				
				if ((clan != null) && (_acceptedClans.size() < 4))
				{
					_acceptedClans.add(clan);
					
					L2PcInstance leader = clan.getLeader().getPlayerInstance();
					if (leader != null)
					{
						leader.sendMessage("Your clan has been accepted to join the RainBow Srpings Chateau siege!");
					}
				}
				
				if (_acceptedClans.size() >= 2)
				// if (_acceptedClans.size() >= 1)
				{
					_nextSiege = ThreadPoolManager.getInstance().scheduleGeneral(new SiegeStart(), 3600000);
					// _nextSiege = ThreadPoolManager.getInstance().scheduleGeneral(new SiegeStart(), 10000);
					_rainbow.updateSiegeStatus(SiegeStatus.WAITING_BATTLE);
				}
				else
				{
					Broadcast.toAllOnlinePlayers("Rainbow Springs Chateau siege aborted due lack of population");
				}
			}
		}
	}
	
	protected static class SiegeStart implements Runnable
	{
		@Override
		public void run()
		{
			if (_rainbow == null)
			{
				_rainbow = CHSiegeManager.getInstance().getSiegableHall(RAINBOW_SPRINGS);
			}
			
			spawnGourds();
			spawnYetis();
			
			_rainbow.updateSiegeStatus(SiegeStatus.RUNNING);
			
			_siegeEnd = ThreadPoolManager.getInstance().scheduleGeneral(new SiegeEnd(null), _rainbow.getSiegeLenght() - 120000);
		}
	}
	
	public static L2Clan _winner;
	
	@Override
	public L2Clan getWinner()
	{
		return _winner;
	}
	
	private static class SiegeEnd implements Runnable
	{
		protected SiegeEnd(L2Clan winner)
		{
			_winner = winner;
		}
		
		@Override
		public void run()
		{
			if (_rainbow == null)
			{
				_rainbow = CHSiegeManager.getInstance().getSiegableHall(RAINBOW_SPRINGS);
			}
			
			SystemMessage end = SystemMessage.getSystemMessage(SystemMessageId.SIEGE_OF_S1_HAS_ENDED);
			end.addString(_rainbow.getName());
			Broadcast.toAllOnlinePlayers(end);
			
			SystemMessage finalMsg = null;
			if (_winner != null)
			{
				_rainbow.setOwner(_winner);
				
				_winner.setHasHideout(_rainbow.getId());
				finalMsg = SystemMessage.getSystemMessage(SystemMessageId.CLAN_S1_VICTORIOUS_OVER_S2_S_SIEGE);
				finalMsg.addString(_winner.getName());
				finalMsg.addString(_rainbow.getName());
				Broadcast.toAllOnlinePlayers(finalMsg);
			}
			
			ThreadPoolManager.getInstance().scheduleGeneral(new SetFinalAttackers(), _rainbow.getNextSiegeTime());
			
			setRegistrationEndString((_rainbow.getNextSiegeTime() + System.currentTimeMillis()) - 3600000);
			
			// Teleport out of the arenas is made 2 mins after game ends
			ThreadPoolManager.getInstance().scheduleGeneral(new TeleportBack(), 120000);
			
			_rainbow.updateSiegeStatus(SiegeStatus.REGISTERING);
			unSpawnGourds();
			unSpawnYetis();
		}
	}
	
	protected static class TeleportBack implements Runnable
	{
		@Override
		public void run()
		{
			final Collection<L2Character> chars = ZoneManager.getInstance().getZoneById(112081).getCharactersInside();
			for (L2Character chr : chars)
			{
				if (chr != null && chr instanceof L2PcInstance)
				{
					L2PcInstance player = (L2PcInstance) chr;
					if (_winner != null && player.getClan().getName().contains(_winner.getName()))
					{
						if (_rainbow == null)
						{
							_rainbow = CHSiegeManager.getInstance().getSiegableHall(RAINBOW_SPRINGS);
						}
						
						L2ClanHallZone zone = _rainbow.getZone();
						if (zone != null)
						{
							player.teleToLocation(zone.getSpawnLoc(), true);
						}
					}
					else
					{
						player.teleToLocation(TeleportWhereType.Town);
					}
				}
			}
		}
	}
	
	public static int getRandom(int max)
	{
		return Rnd.get(max);
	}
	
	public static void load()
	{
		new RainbowSpringsChateau();
	}
}
