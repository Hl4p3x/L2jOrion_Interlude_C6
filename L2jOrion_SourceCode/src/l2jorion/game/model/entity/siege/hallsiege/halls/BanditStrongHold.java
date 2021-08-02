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

import java.util.Collection;

import l2jorion.game.cache.HtmCache;
import l2jorion.game.managers.ZoneManager;
import l2jorion.game.model.Location;
import l2jorion.game.model.entity.siege.hallsiege.flagwar.FlagWar;
import l2jorion.game.model.zone.type.L2ResidenceHallTeleportZone;

public final class BanditStrongHold extends FlagWar
{
	static
	{
		PREFIX = "data/html/SiegableHall/BanditStronghold/";
		
		ROYAL_FLAG = 35422;
		FLAG_RED = 35423;
		FLAG_YELLOW = 35424;
		FLAG_GREEN = 35425;
		FLAG_BLUE = 35426;
		FLAG_PURPLE = 35427;
		
		ALLY_1 = 35428;
		ALLY_2 = 35429;
		ALLY_3 = 35430;
		ALLY_4 = 35431;
		ALLY_5 = 35432;
		
		TELEPORT_1 = 35560;
		
		MESSENGER = 35437;
		
		OUTTER_DOORS_TO_OPEN[0] = 22170001;
		OUTTER_DOORS_TO_OPEN[1] = 22170002;
		
		INNER_DOORS_TO_OPEN[0] = 22170003;
		INNER_DOORS_TO_OPEN[1] = 22170004;
		
		FLAG_COORDS[0] = new Location(83699, -17468, -1774, 19048);
		FLAG_COORDS[1] = new Location(82053, -17060, -1784, 5432);
		FLAG_COORDS[2] = new Location(82142, -15528, -1799, 58792);
		FLAG_COORDS[3] = new Location(83544, -15266, -1770, 44976);
		FLAG_COORDS[4] = new Location(84609, -16041, -1769, 35816);
		FLAG_COORDS[5] = new Location(81981, -15708, -1858, 60392);
		FLAG_COORDS[6] = new Location(84375, -17060, -1860, 27712);
		
		Collection<L2ResidenceHallTeleportZone> zoneList = ZoneManager.getInstance().getAllZones(L2ResidenceHallTeleportZone.class);
		
		for (L2ResidenceHallTeleportZone teleZone : zoneList)
		{
			if (teleZone.getResidenceId() != BANDIT_STRONGHOLD)
			{
				continue;
			}
			
			int id = teleZone.getResidenceZoneId();
			
			if ((id < 0) || (id >= 6))
			{
				continue;
			}
			
			TELE_ZONES[id] = teleZone;
		}
		
		QUEST_REWARD = 5009;
		CENTER = new Location(82882, -16280, -1894, 0);
	}
	
	@Override
	public void reloadOptions()
	{
		PREFIX = "data/html/SiegableHall/BanditStronghold/";
		
		ROYAL_FLAG = 35422;
		FLAG_RED = 35423;
		FLAG_YELLOW = 35424;
		FLAG_GREEN = 35425;
		FLAG_BLUE = 35426;
		FLAG_PURPLE = 35427;
		
		ALLY_1 = 35428;
		ALLY_2 = 35429;
		ALLY_3 = 35430;
		ALLY_4 = 35431;
		ALLY_5 = 35432;
		
		TELEPORT_1 = 35560;
		
		MESSENGER = 35437;
		
		OUTTER_DOORS_TO_OPEN[0] = 22170001;
		OUTTER_DOORS_TO_OPEN[1] = 22170002;
		
		INNER_DOORS_TO_OPEN[0] = 22170003;
		INNER_DOORS_TO_OPEN[1] = 22170004;
		
		FLAG_COORDS[0] = new Location(83699, -17468, -1774, 19048);
		FLAG_COORDS[1] = new Location(82053, -17060, -1784, 5432);
		FLAG_COORDS[2] = new Location(82142, -15528, -1799, 58792);
		FLAG_COORDS[3] = new Location(83544, -15266, -1770, 44976);
		FLAG_COORDS[4] = new Location(84609, -16041, -1769, 35816);
		FLAG_COORDS[5] = new Location(81981, -15708, -1858, 60392);
		FLAG_COORDS[6] = new Location(84375, -17060, -1860, 27712);
		
		Collection<L2ResidenceHallTeleportZone> zoneList = ZoneManager.getInstance().getAllZones(L2ResidenceHallTeleportZone.class);
		
		for (L2ResidenceHallTeleportZone teleZone : zoneList)
		{
			if (teleZone.getResidenceId() != BANDIT_STRONGHOLD)
			{
				continue;
			}
			
			int id = teleZone.getResidenceZoneId();
			
			if ((id < 0) || (id >= 6))
			{
				continue;
			}
			
			TELE_ZONES[id] = teleZone;
		}
		
		QUEST_REWARD = 5009;
		CENTER = new Location(82882, -16280, -1894, 0);
	}
	
	private BanditStrongHold()
	{
		super(BanditStrongHold.class.getSimpleName(), BANDIT_STRONGHOLD);
	}
	
	@Override
	public String getFlagHtml(int flag)
	{
		String result = null;
		
		switch (flag)
		{
			case 35423:
				result = PREFIX + "messenger_flag1.htm";
				break;
			case 35424:
				result = PREFIX + "messenger_flag2.htm";
				break;
			case 35425:
				result = PREFIX + "messenger_flag3.htm";
				break;
			case 35426:
				result = PREFIX + "messenger_flag4.htm";
				break;
			case 35427:
				result = PREFIX + "messenger_flag5.htm";
				break;
		}
		
		return HtmCache.getInstance().getHtm(result);
	}
	
	@Override
	public String getAllyHtml(int ally)
	{
		String result = null;
		
		switch (ally)
		{
			case 35428:
				result = PREFIX + "messenger_ally1result.htm";
				break;
			case 35429:
				result = PREFIX + "messenger_ally2result.htm";
				break;
			case 35430:
				result = PREFIX + "messenger_ally3result.htm";
				break;
			case 35431:
				result = PREFIX + "messenger_ally4result.htm";
				break;
			case 35432:
				result = PREFIX + "messenger_ally5result.htm";
				break;
		}
		
		return HtmCache.getInstance().getHtm(result);
	}
	
	public static void load()
	{
		new BanditStrongHold();
	}
}