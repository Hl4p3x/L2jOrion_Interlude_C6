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

import l2jorion.game.cache.HtmCache;
import l2jorion.game.datatables.sql.ClanTable;
import l2jorion.game.model.L2Clan;
import l2jorion.game.model.actor.instance.L2NpcInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.entity.siege.hallsiege.ClanHallSiegeEngine;
import l2jorion.game.model.spawn.L2Spawn;
import l2jorion.game.network.serverpackets.NpcHtmlMessage;
import l2jorion.game.util.Util;

public final class FortressOfResistance extends ClanHallSiegeEngine
{
	private final int MESSENGER = 35382;
	private final int BLOODY_LORD_NURKA = 35375;
	
	private final Integer[] NURKA_COORDS_X =
	{
		Integer.valueOf(45109),
		Integer.valueOf(47653),
		Integer.valueOf(47247)
	};
	
	private final Integer[] NURKA_COORDS_Y =
	{
		Integer.valueOf(112124),
		Integer.valueOf(110816),
		Integer.valueOf(109396)
	};
	
	private final Integer[] NURKA_COORDS_Z =
	{
		Integer.valueOf(-1900),
		Integer.valueOf(-2110),
		Integer.valueOf(-2000)
	};
	
	private L2Spawn _nurka;
	private final Map<Integer, Long> _damageToNurka = new HashMap<>();
	private NpcHtmlMessage _messengerMsg;
	
	private FortressOfResistance()
	{
		super(FortressOfResistance.class.getSimpleName(), "conquerablehalls", FORTRESS_RESSISTANCE);
		
		addFirstTalkId(MESSENGER);
		addKillId(BLOODY_LORD_NURKA);
		addAttackId(BLOODY_LORD_NURKA);
		
		buildMessengerMessage();
		
		try
		{
			_nurka = new L2Spawn(BLOODY_LORD_NURKA);
			_nurka.setAmount(1);
			_nurka.setLocx(NURKA_COORDS_X[0]);
			_nurka.setLocy(NURKA_COORDS_Y[0]);
			_nurka.setLocz(NURKA_COORDS_Z[0]);
			_nurka.setRespawnDelay(10800);
		}
		catch (Exception e)
		{
			LOG.warn("{}: Couldnt set the Bloody Lord Nurka spawn!", getName(), e);
		}
	}
	
	private final void buildMessengerMessage()
	{
		String filename = HtmCache.getInstance().getHtm(null, "data/html/SiegableHall/FortressOfResistance/partisan_ordery_brakel001.htm");
		
		if (filename != null)
		{
			_messengerMsg = new NpcHtmlMessage(1);
			_messengerMsg.setHtml(filename);
			_messengerMsg.replace("%nextSiege%", Util.formatDate(_hall.getSiegeDate().getTime(), "yyyy-MM-dd HH:mm:ss"));
		}
	}
	
	@Override
	public String onFirstTalk(L2NpcInstance npc, L2PcInstance player)
	{
		if (_messengerMsg != null)
		{
			player.sendPacket(_messengerMsg);
		}
		return null;
	}
	
	@Override
	public String onAttack(L2NpcInstance npc, L2PcInstance player, int damage, boolean isSummon)
	{
		if (!_hall.isInSiege())
		{
			return null;
		}
		
		int clanId = player.getClanId();
		if (clanId > 0)
		{
			long clanDmg = (_damageToNurka.containsKey(clanId)) ? _damageToNurka.get(clanId) + damage : damage;
			_damageToNurka.put(clanId, clanDmg);
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
		
		_missionAccomplished = true;
		
		synchronized (this)
		{
			npc.getSpawn().stopRespawn();
			// npc.deleteMe();
			
			cancelSiegeTask();
			endSiege();
		}
		return null;
	}
	
	@Override
	public L2Clan getWinner()
	{
		int winnerId = 0;
		long counter = 0;
		for (Entry<Integer, Long> e : _damageToNurka.entrySet())
		{
			long dam = e.getValue();
			if (dam > counter)
			{
				winnerId = e.getKey();
				counter = dam;
			}
		}
		return ClanTable.getInstance().getClan(winnerId);
	}
	
	@Override
	public void onSiegeStarts()
	{
		_nurka.init();
	}
	
	@Override
	public void onSiegeEnds()
	{
		buildMessengerMessage();
	}
	
	public static void load()
	{
		new FortressOfResistance();
	}
}