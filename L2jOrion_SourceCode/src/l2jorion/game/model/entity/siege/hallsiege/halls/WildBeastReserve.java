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

import l2jorion.game.managers.ZoneManager;
import l2jorion.game.model.Location;
import l2jorion.game.model.entity.siege.hallsiege.flagwar.FlagWar;
import l2jorion.game.model.zone.type.L2ResidenceHallTeleportZone;

public final class WildBeastReserve extends FlagWar
{
	static
	{
		PREFIX = "data/html/SiegableHall/WildBeastReserve/";
		
		ROYAL_FLAG = 35606;
		FLAG_RED = 35607; // White flag
		FLAG_YELLOW = 35608; // Red flag
		FLAG_GREEN = 35609; // Blue flag
		FLAG_BLUE = 35610; // Green flag
		FLAG_PURPLE = 35611; // Black flag
		
		ALLY_1 = 35618;
		ALLY_2 = 35619;
		ALLY_3 = 35620;
		ALLY_4 = 35621;
		ALLY_5 = 35622;
		
		TELEPORT_1 = 35612;
		
		MESSENGER = 35627;
		
		OUTTER_DOORS_TO_OPEN[0] = 21150003;
		OUTTER_DOORS_TO_OPEN[1] = 21150004;
		
		INNER_DOORS_TO_OPEN[0] = 21150001;
		INNER_DOORS_TO_OPEN[1] = 21150002;
		
		FLAG_COORDS[0] = new Location(56963, -92211, -1303, 60611);
		FLAG_COORDS[1] = new Location(58090, -91641, -1303, 47274);
		FLAG_COORDS[2] = new Location(58908, -92556, -1303, 34450);
		FLAG_COORDS[3] = new Location(58336, -93600, -1303, 21100);
		FLAG_COORDS[4] = new Location(57152, -93360, -1303, 8400);
		FLAG_COORDS[5] = new Location(59116, -93251, -1302, 31000);
		FLAG_COORDS[6] = new Location(56432, -92864, -1303, 64000);
		
		Collection<L2ResidenceHallTeleportZone> zoneList = ZoneManager.getInstance().getAllZones(L2ResidenceHallTeleportZone.class);
		
		for (L2ResidenceHallTeleportZone teleZone : zoneList)
		{
			if (teleZone.getResidenceId() != BEAST_FARM)
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
		
		QUEST_REWARD = 0;
		CENTER = new Location(57762, -92696, -1359, 0);
	}
	
	@Override
	public void reloadOptions()
	{
		PREFIX = "data/html/SiegableHall/WildBeastReserve/";
		
		ROYAL_FLAG = 35606;
		FLAG_RED = 35607; // White flag
		FLAG_YELLOW = 35608; // Red flag
		FLAG_GREEN = 35609; // Blue flag
		FLAG_BLUE = 35610; // Green flag
		FLAG_PURPLE = 35611; // Black flag
		
		ALLY_1 = 35618;
		ALLY_2 = 35619;
		ALLY_3 = 35620;
		ALLY_4 = 35621;
		ALLY_5 = 35622;
		
		TELEPORT_1 = 35612;
		
		MESSENGER = 35627;
		
		OUTTER_DOORS_TO_OPEN[0] = 21150003;
		OUTTER_DOORS_TO_OPEN[1] = 21150004;
		
		INNER_DOORS_TO_OPEN[0] = 21150001;
		INNER_DOORS_TO_OPEN[1] = 21150002;
		
		FLAG_COORDS[0] = new Location(56963, -92211, -1303, 60611);
		FLAG_COORDS[1] = new Location(58090, -91641, -1303, 47274);
		FLAG_COORDS[2] = new Location(58908, -92556, -1303, 34450);
		FLAG_COORDS[3] = new Location(58336, -93600, -1303, 21100);
		FLAG_COORDS[4] = new Location(57152, -93360, -1303, 8400);
		FLAG_COORDS[5] = new Location(59116, -93251, -1302, 31000);
		FLAG_COORDS[6] = new Location(56432, -92864, -1303, 64000);
		
		Collection<L2ResidenceHallTeleportZone> zoneList = ZoneManager.getInstance().getAllZones(L2ResidenceHallTeleportZone.class);
		
		for (L2ResidenceHallTeleportZone teleZone : zoneList)
		{
			if (teleZone.getResidenceId() != BEAST_FARM)
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
		
		QUEST_REWARD = 0;
		CENTER = new Location(57762, -92696, -1359, 0);
	}
	
	private WildBeastReserve()
	{
		super(WildBeastReserve.class.getSimpleName(), BEAST_FARM);
	}
	
	@Override
	public String getFlagHtml(int flag)
	{
		String result = null;
		
		switch (flag)
		{
			case 35607:
				result = PREFIX + "messenger_flag1.htm";
				break;
			case 35608:
				result = PREFIX + "messenger_flag2.htm";
				break;
			case 35609:
				result = PREFIX + "messenger_flag3.htm";
				break;
			case 35610:
				result = PREFIX + "messenger_flag4.htm";
				break;
			case 35611:
				result = PREFIX + "messenger_flag5.htm";
				break;
		}
		
		return result;
	}
	
	@Override
	public String getAllyHtml(int ally)
	{
		String result = null;
		
		switch (ally)
		{
			case 35618:
				result = PREFIX + "messenger_ally1result.htm";
				break;
			case 35619:
				result = PREFIX + "messenger_ally2result.htm";
				break;
			case 35620:
				result = PREFIX + "messenger_ally3result.htm";
				break;
			case 35621:
				result = PREFIX + "messenger_ally4result.htm";
				break;
			case 35622:
				result = PREFIX + "messenger_ally5result.htm";
				break;
		}
		
		return result;
	}
	
	@Override
	public boolean canPayRegistration()
	{
		return false;
	}
	
	public static void load()
	{
		new WildBeastReserve();
	}
}
