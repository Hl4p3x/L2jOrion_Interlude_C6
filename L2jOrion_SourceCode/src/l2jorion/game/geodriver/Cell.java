/*
 * Copyright (C) 2004-2015 L2J Server
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
package l2jorion.game.geodriver;

public final class Cell
{
	/** East NSWE flag */
	public static final byte NSWE_EAST = 1 << 0;
	/** West NSWE flag */
	public static final byte NSWE_WEST = 1 << 1;
	/** South NSWE flag */
	public static final byte NSWE_SOUTH = 1 << 2;
	/** North NSWE flag */
	public static final byte NSWE_NORTH = 1 << 3;
	
	/** North-East NSWE flags */
	public static final byte NSWE_NORTH_EAST = NSWE_NORTH | NSWE_EAST;
	/** North-West NSWE flags */
	public static final byte NSWE_NORTH_WEST = NSWE_NORTH | NSWE_WEST;
	/** South-East NSWE flags */
	public static final byte NSWE_SOUTH_EAST = NSWE_SOUTH | NSWE_EAST;
	/** South-West NSWE flags */
	public static final byte NSWE_SOUTH_WEST = NSWE_SOUTH | NSWE_WEST;
	
	/** All directions NSWE flags */
	public static final byte NSWE_ALL = NSWE_EAST | NSWE_WEST | NSWE_SOUTH | NSWE_NORTH;
	
	private Cell()
	{
	}
}
