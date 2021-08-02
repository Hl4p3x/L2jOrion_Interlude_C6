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
package l2jorion.game.ai.additional;

import java.util.ArrayList;
import java.util.List;

import l2jorion.game.ai.CtrlIntention;
import l2jorion.game.cache.HtmCache;
import l2jorion.game.datatables.SkillTable;
import l2jorion.game.datatables.csv.MapRegionTable.TeleportWhereType;
import l2jorion.game.model.L2Attackable;
import l2jorion.game.model.L2Skill;
import l2jorion.game.model.Location;
import l2jorion.game.model.actor.instance.L2NpcInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.entity.siege.Siege;
import l2jorion.game.model.quest.Quest;
import l2jorion.game.model.zone.ZoneId;
import l2jorion.game.network.clientpackets.Say2;
import l2jorion.game.network.serverpackets.NpcSay;
import l2jorion.util.random.Rnd;

public final class Benom extends Quest implements Runnable
{
	private static final int BENOM = 29054;
	private static final int TELEPORT_CUBE = 13101;
	private static final int DUNGEON_KEEPER = 35506;
	
	private static final Location[] TARGET_TELEPORTS =
	{
		new Location(12860, -49158, 976),
		new Location(14878, -51339, 1024),
		new Location(15674, -49970, 864),
		new Location(15696, -48326, 864),
		new Location(14873, -46956, 1024),
		new Location(12157, -49135, -1088),
		new Location(12875, -46392, -288),
		new Location(14087, -46706, -288),
		new Location(14086, -51593, -288),
		new Location(12864, -51898, -288),
		new Location(15538, -49153, -1056),
		new Location(17001, -49149, -1064)
	};
	
	private static final int[] TARGET_TELEPORTS_OFFSET =
	{
		650,
		100,
		100,
		100,
		100,
		650,
		200,
		200,
		200,
		200,
		200,
		650
	};
	
	private static final Location TRHONE = new Location(11025, -49152, -537);
	private static final Location DUNGEON = new Location(11882, -49216, -3008);
	
	private Siege _siege;
	
	private L2NpcInstance _benom;
	
	private boolean _isPrisonOpened;
	
	private List<L2PcInstance> _targets = new ArrayList<>();
	
	public Benom(final int questId, final String name, final String descr)
	{
		super(questId, name, descr);
		
		final int[] mobs =
		{
			BENOM
		};
		
		_siege = addSiegeNotify(8);
		
		addStartNpc(DUNGEON_KEEPER, TELEPORT_CUBE);
		addTalkId(DUNGEON_KEEPER, TELEPORT_CUBE);
		
		for (final int mob : mobs)
		{
			addEventId(mob, Quest.QuestEventType.ON_AGGRO_RANGE_ENTER);
			addEventId(mob, Quest.QuestEventType.ON_SPELL_FINISHED);
			addEventId(mob, Quest.QuestEventType.ON_ATTACK);
			addEventId(mob, Quest.QuestEventType.ON_KILL);
		}
	}
	
	@Override
	public String onTalk(L2NpcInstance npc, L2PcInstance talker)
	{
		switch (npc.getNpcId())
		{
			case TELEPORT_CUBE:
				talker.teleToLocation(TeleportWhereType.Town);
				break;
			
			case DUNGEON_KEEPER:
				if (_isPrisonOpened)
				{
					talker.teleToLocation(12589, -49044, -3008, false);
				}
				else
				{
					return HtmCache.getInstance().getHtm("data/html/doormen/35506-2.htm");
				}
				break;
		}
		return super.onTalk(npc, talker);
	}
	
	@Override
	public String onAdvEvent(String event, L2NpcInstance npc, L2PcInstance player)
	{
		switch (event)
		{
			case "benom_spawn":
				_isPrisonOpened = true;
				
				_benom = addSpawn(BENOM, DUNGEON, false, 0);
				_benom.broadcastNpcSay("Who dares to covet the throne of our castle! Leave immediately or you will pay the price of your audacity with your very own blood!");
				break;
			
			case "tower_check":
				if (_siege.getControlTowerCount() < 2)
				{
					npc.teleToLocation(TRHONE, false);
					_siege.getCastle().getZone().broadcastPacket(new NpcSay(0, Say2.ALL, DUNGEON_KEEPER, "Oh no! The defenses have failed. It is too dangerous to remain inside the castle. Flee! Every man for himself!"));
					
					cancelQuestTimer("tower_check", npc, null);
					startQuestTimer("raid_check", 10000, npc, null, true);
				}
				break;
			
			case "raid_check":
				if (!npc.isInsideZone(ZoneId.ZONE_SIEGE) && !npc.isTeleporting())
				{
					npc.teleToLocation(TRHONE, false);
				}
				break;
		}
		return event;
	}
	
	@Override
	public String onAggroRangeEnter(L2NpcInstance npc, L2PcInstance player, boolean isPet)
	{
		if (isPet)
		{
			return super.onAggroRangeEnter(npc, player, isPet);
		}
		
		if (_targets.size() < 10 && Rnd.get(3) < 1)
		{
			_targets.add(player);
		}
		
		return super.onAggroRangeEnter(npc, player, isPet);
	}
	
	@Override
	public void onSiegeEvent()
	{
		// Don't go further if the castle isn't owned.
		if (_siege.getCastle().getOwnerId() <= 0)
		{
			return;
		}
		
		switch (_siege.getStatus())
		{
			case IN_PROGRESS:
				_isPrisonOpened = false;
				if (_benom != null && !_benom.isDead())
				{
					startQuestTimer("tower_check", 30000, _benom, null, true);
				}
				break;
			
			case REGISTRATION_OPENED:
				_isPrisonOpened = false;
				
				if (_benom != null)
				{
					cancelQuestTimer("tower_check", _benom, null);
					cancelQuestTimer("raid_check", _benom, null);
					
					_benom.deleteMe();
				}
				
				startQuestTimer("benom_spawn", _siege.getSiegeDate().getTimeInMillis() - 8640000 - System.currentTimeMillis(), null, null, false);
				break;
			
			case REGISTRATION_OVER:
				startQuestTimer("benom_spawn", 0, null, null, false);
				break;
		}
	}
	
	@Override
	public String onSpellFinished(L2NpcInstance npc, L2PcInstance player, L2Skill skill)
	{
		switch (skill.getId())
		{
			case 4995:
				teleportTarget(player);
				((L2Attackable) npc).stopHating(player);
				break;
			
			case 4996:
				teleportTarget(player);
				((L2Attackable) npc).stopHating(player);
				if (!_targets.isEmpty())
				{
					for (L2PcInstance target : _targets)
					{
						final long x = player.getX() - target.getX();
						final long y = player.getY() - target.getY();
						final long z = player.getZ() - target.getZ();
						final long range = 250;
						if (((x * x) + (y * y) + (z * z)) <= (range * range))
						{
							teleportTarget(target);
							((L2Attackable) npc).stopHating(target);
						}
					}
					_targets.clear();
				}
				break;
		}
		
		return null;
	}
	
	@Override
	public String onAttack(L2NpcInstance npc, L2PcInstance attacker, int damage, boolean isPet)
	{
		if (attacker != null)
		{
			if (Rnd.get(100) <= 25)
			{
				npc.setTarget(attacker);
				npc.doCast(SkillTable.getInstance().getInfo(4995, 1));
			}
			else if (!npc.isCastingNow())
			{
				if ((npc.getCurrentHp() < (npc.getMaxHp() / 3)) && Rnd.get(500) < 1)
				{
					npc.setTarget(attacker);
					npc.doCast(SkillTable.getInstance().getInfo(4996, 1));
				}
				else if (!npc.isInsideRadius(attacker, 300, true, false) && Rnd.get(100) < 1)
				{
					npc.setTarget(attacker);
					npc.doCast(SkillTable.getInstance().getInfo(4993, 1));
				}
				else if (Rnd.get(100) < 1)
				{
					npc.setTarget(attacker);
					npc.doCast(SkillTable.getInstance().getInfo(4994, 1));
				}
			}
		}
		return super.onAttack(npc, attacker, damage, isPet);
	}
	
	@Override
	public String onKill(L2NpcInstance npc, L2PcInstance killer, boolean isPet)
	{
		npc.broadcastNpcSay("It's not over yet... It won't be... over... like this... Never...");
		cancelQuestTimer("raid_check", npc, null);
		
		addSpawn(TELEPORT_CUBE, 12589, -49044, -3008, 0, false, 120000);
		
		return super.onKill(npc, killer, isPet);
	}
	
	private void teleportTarget(L2PcInstance player)
	{
		if ((player != null) && !player.isDead())
		{
			final int rnd = Rnd.get(11);
			player.teleToLocation(TARGET_TELEPORTS[rnd], TARGET_TELEPORTS_OFFSET[rnd]);
			player.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
		}
	}
	
	@Override
	public void run()
	{
	}
}
