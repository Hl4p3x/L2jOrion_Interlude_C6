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
package l2jorion.game.geo.pathfinding;

import java.util.List;

import l2jorion.game.geo.pathfinding.cellnodes.CellPathFinding;
import l2jorion.game.model.L2World;

public abstract class PathFinding
{
	public static PathFinding getInstance()
	{
		// if (Config.GEODATA)
		// {
		// Higher Memory Usage, Smaller Cpu Usage
		// return GeoPathFinding.getInstance();
		// }
		
		return CellPathFinding.getInstance();
	}
	
	public abstract boolean pathNodesExist(short regionoffset);
	
	public abstract List<AbstractNodeLoc> findPath(int x, int y, int z, int tx, int ty, int tz, int instanceId, boolean playable);
	
	public short getNodePos(int geo_pos)
	{
		return (short) (geo_pos >> 3);
	}
	
	public short getNodeBlock(int node_pos)
	{
		return (short) (node_pos % 256);
	}
	
	public byte getRegionX(int node_pos)
	{
		return (byte) ((node_pos >> 8) + L2World.TILE_X_MIN);
	}
	
	public byte getRegionY(int node_pos)
	{
		return (byte) ((node_pos >> 8) + L2World.TILE_Y_MIN);
	}
	
	public short getRegionOffset(byte rx, byte ry)
	{
		return (short) ((rx << 5) + ry);
	}
	
	/**
	 * Convert pathnode x to World x position
	 * @param node_x rx
	 * @return
	 */
	public int calculateWorldX(short node_x)
	{
		return L2World.MAP_MIN_X + (node_x * 128) + 48;
	}
	
	/**
	 * Convert pathnode y to World y position
	 * @param node_y
	 * @return
	 */
	public int calculateWorldY(short node_y)
	{
		return L2World.MAP_MIN_Y + (node_y * 128) + 48;
	}
	
	public String[] getStat()
	{
		return null;
	}
}
