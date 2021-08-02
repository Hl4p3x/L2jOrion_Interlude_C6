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

import l2jorion.game.controllers.GameTimeController;
import l2jorion.game.datatables.sql.ClanTable;
import l2jorion.game.model.L2Clan;
import l2jorion.game.model.actor.instance.L2NpcInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.entity.siege.hallsiege.ClanHallSiegeEngine;
import l2jorion.game.network.clientpackets.Say2;
import l2jorion.game.thread.ThreadPoolManager;

public final class FortressOfTheDead extends ClanHallSiegeEngine
{
	private static final int LIDIA = 35629;
	private static final int ALFRED = 35630;
	private static final int GISELLE = 35631;
	
	private static Map<Integer, Integer> _damageToLidia = new HashMap<>();
	
	public FortressOfTheDead()
	{
		super(FortressOfTheDead.class.getSimpleName(), "conquerablehalls", FORTRESS_OF_DEAD);
		
		addKillId(LIDIA);
		addKillId(ALFRED);
		addKillId(GISELLE);
		
		addSpawnId(LIDIA);
		addSpawnId(ALFRED);
		addSpawnId(GISELLE);
		
		addAttackId(LIDIA);
	}
	
	@Override
	public String onSpawn(L2NpcInstance npc)
	{
		if (npc.getNpcId() == LIDIA)
		{
			broadcastNpcSay(npc, Say2.SHOUT, "Hmm, those who are not of the bloodline are coming this way to take over the castle?!  Humph!  The bitter grudges of the dead.  You must not make light of their power!");
		}
		else if (npc.getNpcId() == ALFRED)
		{
			broadcastNpcSay(npc, Say2.SHOUT, "Heh Heh... I see that the feast has begun! Be wary! The curse of the Hellmann family has poisoned this land!");
		}
		else if (npc.getNpcId() == GISELLE)
		{
			broadcastNpcSay(npc, Say2.SHOUT, "Arise, my faithful servants! You, my people who have inherited the blood.  It is the calling of my daughter.  The feast of blood will now begin!");
		}
		return null;
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
				if ((id > 0) && _damageToLidia.containsKey(id))
				{
					int newDamage = _damageToLidia.get(id);
					newDamage += damage;
					_damageToLidia.put(id, newDamage);
				}
				else
				{
					_damageToLidia.put(id, damage);
				}
			}
		}
		return null;
	}
	
	@Override
	public String onKill(L2NpcInstance npc, L2PcInstance killer, boolean isSummon)
	{
		if (!_hall.isInSiege())
		{
			return null;
		}
		
		final int npcId = npc.getNpcId();
		
		if ((npcId == ALFRED) || (npcId == GISELLE))
		{
			broadcastNpcSay(npc, Say2.SHOUT, "Aargh...!  If I die, then the magic force field of blood will...!");
		}
		if (npcId == LIDIA)
		{
			broadcastNpcSay(npc, Say2.SHOUT, "Grarr! For the next 2 minutes or so, the game arena are will be cleaned. Throw any items you don't need to the floor now.");
			_missionAccomplished = true;
			synchronized (this)
			{
				cancelSiegeTask();
				endSiege();
			}
		}
		
		return null;
	}
	
	@Override
	public L2Clan getWinner()
	{
		int counter = 0;
		int damagest = 0;
		for (Entry<Integer, Integer> e : _damageToLidia.entrySet())
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
	
	@Override
	public void startSiege()
	{
		// Siege must start at night
		int hoursLeft = (GameTimeController.getInstance().getGameTime() / 60) % 24;
		
		if ((hoursLeft < 0) || (hoursLeft > 6))
		{
			cancelSiegeTask();
			long scheduleTime = (24 - hoursLeft) * 10 * 60000;
			_siegeTask = ThreadPoolManager.getInstance().scheduleGeneral(new SiegeStarts(), scheduleTime);
		}
		else
		{
			super.startSiege();
		}
	}
	
	public static void load()
	{
		new FortressOfTheDead();
	}
}
