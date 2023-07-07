package l2jorion.game.util;

import l2jorion.game.geo.geodriver.Cell;

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