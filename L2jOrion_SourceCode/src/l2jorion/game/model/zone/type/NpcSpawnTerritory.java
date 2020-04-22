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

import java.util.List;

import l2jorion.game.model.zone.L2ZoneForm;

public class NpcSpawnTerritory
{
	private final String _name;
	private final L2ZoneForm _territory;
	@SuppressWarnings("unused")
	private List<L2ZoneForm> _bannedTerritories; // TODO: Implement it
	
	public NpcSpawnTerritory(String name, L2ZoneForm territory)
	{
		_name = name;
		_territory = territory;
	}
	
	public String getName()
	{
		return _name;
	}
	
	/*public int[] getRandomPoint()
	{
		return _territory.getRandomPoint();
	}*/
	
	public boolean isInsideZone(int x, int y, int z)
	{
		return _territory.isInsideZone(x, y, z);
	}
	
	/*public void visualizeZone(int z)
	{
		_territory.visualizeZone(z);
	}*/
}