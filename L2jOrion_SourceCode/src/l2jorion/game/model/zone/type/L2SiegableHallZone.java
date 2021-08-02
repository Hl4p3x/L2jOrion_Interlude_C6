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
package l2jorion.game.model.zone.type;

import java.util.ArrayList;
import java.util.List;

import l2jorion.game.model.Location;
import l2jorion.game.model.actor.instance.L2PcInstance;

public final class L2SiegableHallZone extends L2ClanHallZone
{
	private List<Location> _challengerLocations;
	
	public L2SiegableHallZone(int id)
	{
		super(id);
	}
	
	@Override
	public void parseLoc(int x, int y, int z, String type)
	{
		if ((type != null) && type.equals("challenger"))
		{
			if (_challengerLocations == null)
			{
				_challengerLocations = new ArrayList<>();
			}
			_challengerLocations.add(new Location(x, y, z));
		}
		else
		{
			super.parseLoc(x, y, z, type);
		}
	}
	
	public List<Location> getChallengerSpawns()
	{
		return _challengerLocations;
	}
	
	@Override
	public void banishNonSiegeParticipants()
	{
		for (L2PcInstance player : getPlayersInside())
		{
			if ((player != null) && player.isInHideoutSiege())
			{
				player.teleToLocation(getBanishSpawnLoc(), true);
			}
		}
	}
}
