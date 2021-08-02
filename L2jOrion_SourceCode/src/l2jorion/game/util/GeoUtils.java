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
package l2jorion.game.util;

import l2jorion.game.geodriver.Cell;

public final class GeoUtils
{
	public static int computeNswe(int lastX, int lastY, int x, int y)
	{
		if (x > lastX) // east
		{
			if (y > lastY)
			{
				return Cell.NSWE_SOUTH_EAST;// Direction.SOUTH_EAST;
			}
			else if (y < lastY)
			{
				return Cell.NSWE_NORTH_EAST;// Direction.NORTH_EAST;
			}
			else
			{
				return Cell.NSWE_EAST;// Direction.EAST;
			}
		}
		else if (x < lastX) // west
		{
			if (y > lastY)
			{
				return Cell.NSWE_SOUTH_WEST;// Direction.SOUTH_WEST;
			}
			else if (y < lastY)
			{
				return Cell.NSWE_NORTH_WEST;// Direction.NORTH_WEST;
			}
			else
			{
				return Cell.NSWE_WEST;// Direction.WEST;
			}
		}
		else
		// unchanged x
		{
			if (y > lastY)
			{
				return Cell.NSWE_SOUTH;// Direction.SOUTH;
			}
			else if (y < lastY)
			{
				return Cell.NSWE_NORTH;// Direction.NORTH;
			}
			else
			{
				throw new RuntimeException();
			}
		}
	}
}