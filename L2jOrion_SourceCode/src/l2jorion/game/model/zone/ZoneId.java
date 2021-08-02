/*
 * Copyright (C) 2004-2016 L2J Server
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
package l2jorion.game.model.zone;

public enum ZoneId
{
	ZONE_PVP,
	ZONE_PEACE,
	ZONE_SIEGE,
	ZONE_MOTHERTREE,
	ZONE_CLANHALL,
	ZONE_UNUSED,
	ZONE_NOLANDING,
	ZONE_WATER,
	ZONE_JAIL,
	ZONE_MONSTERTRACK,
	ZONE_SWAMP,
	ZONE_NOSUMMONFRIEND,
	ZONE_OLY,
	ZONE_DANGERAREA,
	ZONE_NOSTORE,
	ZONE_BOSS,
	ZONE_HQ,
	ZONE_NORESTART,
	ZONE_RANDOM,
	ZONE_CASTLE;
	
	public static int getZoneCount()
	{
		return values().length;
	}
}
