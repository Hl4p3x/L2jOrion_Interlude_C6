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

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import l2jorion.game.ai.CtrlIntention;
import l2jorion.game.datatables.SkillTable;
import l2jorion.game.datatables.sql.ClanTable;
import l2jorion.game.datatables.sql.NpcTable;
import l2jorion.game.model.L2Clan;
import l2jorion.game.model.actor.instance.L2NpcInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.entity.siege.hallsiege.ClanHallSiegeEngine;
import l2jorion.game.network.clientpackets.Say2;

public final class DevastatedCastle extends ClanHallSiegeEngine
{
	private static final int GUSTAV = 35410;
	private static final int MIKHAIL = 35409;
	private static final int DIETRICH = 35408;
	private static final double GUSTAV_TRIGGER_HP = NpcTable.getInstance().getTemplate(GUSTAV).getBaseHpMax() / 12;
	
	private static Map<Integer, Integer> _damageToGustav = new HashMap<>();
	
	public DevastatedCastle()
	{
		super(DevastatedCastle.class.getSimpleName(), "conquerablehalls", DEVASTATED_CASTLE);
		
		int[] mob = {GUSTAV,MIKHAIL,DIETRICH};
		registerMobs(mob);
		
		addKillId(GUSTAV);
		addSpawnId(MIKHAIL);
		addSpawnId(DIETRICH);
		addAttackId(GUSTAV);
	}
	
	@Override
	public String onSpawn(L2NpcInstance npc)
	{
		if (npc.getNpcId() == MIKHAIL)
		{
			broadcastNpcSay(npc, Say2.SHOUT, "Glory to Aden, the Kingdom of the Lion! Glory to Sir Gustav, our immortal lord!");
		}
		else if (npc.getNpcId() == DIETRICH)
		{
			broadcastNpcSay(npc, Say2.SHOUT, "Soldiers of Gustav, go forth and destroy the invaders!");
		}
		return super.onSpawn(npc);
	}
	
	@Override
	public String onAttack(L2NpcInstance npc, L2PcInstance attacker, int damage, boolean isSummon)
	{
		if (!_hall.isInSiege())
		{
			return null;
		}
		
		synchronized (this)
		{
			final L2Clan clan = attacker.getClan();
			
			if ((clan != null) && checkIsAttacker(clan))
			{
				final int id = clan.getClanId();
				if (_damageToGustav.containsKey(id))
				{
					int newDamage = _damageToGustav.get(id);
					newDamage += damage;
					_damageToGustav.put(id, newDamage);
				}
				else
				{
					_damageToGustav.put(id, damage);
				}
			}
			
			if (npc.getNpcId() == GUSTAV)
			{
				if ((npc.getCurrentHp() < (GUSTAV_TRIGGER_HP / 12)) && (npc.getAI().getIntention() != CtrlIntention.AI_INTENTION_CAST))
				{
					broadcastNpcSay(npc, Say2.SHOUT, "This is unbelievable! Have I really been defeated? I shall return and take your head!");
					npc.getAI().setIntention(CtrlIntention.AI_INTENTION_CAST, SkillTable.getInstance().getInfo(4020, 1), npc);
				}
			}
		}
		return super.onAttack(npc, attacker, damage, isSummon);
	}
	
	@Override
	public String onKill(L2NpcInstance npc, L2PcInstance killer, boolean isSummon)
	{
		if (!_hall.isInSiege())
		{
			return null;
		}
		
		_missionAccomplished = true;
		
		if (npc.getNpcId() == GUSTAV)
		{
			synchronized (this)
			{
				cancelSiegeTask();
				endSiege();
			}
		}
		
		return super.onKill(npc, killer, isSummon);
	}
	
	@Override
	public L2Clan getWinner()
	{
		int counter = 0;
		int damagest = 0;
		for (Entry<Integer, Integer> e : _damageToGustav.entrySet())
		{
			final int damage = e.getValue();
			if (damage > counter)
			{
				counter = damage;
				damagest = e.getKey();
			}
		}
		return ClanTable.getInstance().getClan(damagest);
	}
	
	public static void load()
	{
		new DevastatedCastle();
	}
}