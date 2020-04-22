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

import java.awt.Color;

import l2jorion.game.geo.GeoData;
import l2jorion.game.geodriver.Cell;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.network.serverpackets.ExServerPrimitive;

public final class GeoUtils
{
	public static void debug2DLine(L2PcInstance player, int x, int y, int tx, int ty, int z)
	{
		int gx = GeoData.getInstance().getGeoX(x);
		int gy = GeoData.getInstance().getGeoY(y);
		
		int tgx = GeoData.getInstance().getGeoX(tx);
		int tgy = GeoData.getInstance().getGeoY(ty);
		
		ExServerPrimitive prim = new ExServerPrimitive("Debug2DLine", x, y, z);
		prim.addLine(Color.BLUE, GeoData.getInstance().getWorldX(gx), GeoData.getInstance().getWorldY(gy), z, GeoData.getInstance().getWorldX(tgx), GeoData.getInstance().getWorldY(tgy), z);
		
		LinePointIterator iter = new LinePointIterator(gx, gy, tgx, tgy);
		
		while (iter.next())
		{
			int wx = GeoData.getInstance().getWorldX(iter.x());
			int wy = GeoData.getInstance().getWorldY(iter.y());
			
			prim.addPoint(Color.RED, wx, wy, z);
		}
		player.sendPacket(prim);
	}
	
	public static void debug3DLine(L2PcInstance player, int x, int y, int z, int tx, int ty, int tz)
	{
		int gx = GeoData.getInstance().getGeoX(x);
		int gy = GeoData.getInstance().getGeoY(y);
		
		int tgx = GeoData.getInstance().getGeoX(tx);
		int tgy = GeoData.getInstance().getGeoY(ty);
		
		ExServerPrimitive prim = new ExServerPrimitive("Debug3DLine", x, y, z);
		prim.addLine(Color.BLUE, GeoData.getInstance().getWorldX(gx), GeoData.getInstance().getWorldY(gy), z, GeoData.getInstance().getWorldX(tgx), GeoData.getInstance().getWorldY(tgy), tz);
		
		LinePointIterator3D iter = new LinePointIterator3D(gx, gy, z, tgx, tgy, tz);
		iter.next();
		int prevX = iter.x();
		int prevY = iter.y();
		int wx = GeoData.getInstance().getWorldX(prevX);
		int wy = GeoData.getInstance().getWorldY(prevY);
		int wz = iter.z();
		prim.addPoint(Color.RED, wx, wy, wz);
		
		while (iter.next())
		{
			int curX = iter.x();
			int curY = iter.y();
			
			if ((curX != prevX) || (curY != prevY))
			{
				wx = GeoData.getInstance().getWorldX(curX);
				wy = GeoData.getInstance().getWorldY(curY);
				wz = iter.z();
				
				prim.addPoint(Color.RED, wx, wy, wz);
				
				prevX = curX;
				prevY = curY;
			}
		}
		player.sendPacket(prim);
	}
	
	private static Color getDirectionColor(int x, int y, int z, int nswe)
	{
		if (GeoData.getInstance().checkNearestNswe(x, y, z, nswe))
		{
			return Color.GREEN;
		}
		return Color.RED;
	}
	
	public static void debugGrid(L2PcInstance player)
	{
		int geoRadius = 10;
		int blocksPerPacket = 49;
		if (geoRadius < 0)
		{
			throw new IllegalArgumentException("geoRadius < 0");
		}
		
		int iBlock = blocksPerPacket;
		int iPacket = 0;
		
		ExServerPrimitive exsp = null;
		GeoData gd = GeoData.getInstance();
		int playerGx = gd.getGeoX(player.getX());
		int playerGy = gd.getGeoY(player.getY());
		for (int dx = -geoRadius; dx <= geoRadius; ++dx)
		{
			for (int dy = -geoRadius; dy <= geoRadius; ++dy)
			{
				if (iBlock >= blocksPerPacket)
				{
					iBlock = 0;
					if (exsp != null)
					{
						++iPacket;
						player.sendPacket(exsp);
					}
					exsp = new ExServerPrimitive("DebugGrid_" + iPacket, player.getX(), player.getY(), -16000);
				}
				
				if (exsp == null)
				{
					throw new IllegalStateException();
				}
				
				int gx = playerGx + dx;
				int gy = playerGy + dy;
				
				int x = gd.getWorldX(gx);
				int y = gd.getWorldY(gy);
				int z = gd.getNearestZ(gx, gy, player.getZ());
				
				// north arrow
				Color col = getDirectionColor(gx, gy, z, Cell.NSWE_NORTH);
				exsp.addLine(col, x - 1, y - 7, z, x + 1, y - 7, z);
				exsp.addLine(col, x - 2, y - 6, z, x + 2, y - 6, z);
				exsp.addLine(col, x - 3, y - 5, z, x + 3, y - 5, z);
				exsp.addLine(col, x - 4, y - 4, z, x + 4, y - 4, z);
				
				// east arrow
				col = getDirectionColor(gx, gy, z, Cell.NSWE_EAST);
				exsp.addLine(col, x + 7, y - 1, z, x + 7, y + 1, z);
				exsp.addLine(col, x + 6, y - 2, z, x + 6, y + 2, z);
				exsp.addLine(col, x + 5, y - 3, z, x + 5, y + 3, z);
				exsp.addLine(col, x + 4, y - 4, z, x + 4, y + 4, z);
				
				// south arrow
				col = getDirectionColor(gx, gy, z, Cell.NSWE_SOUTH);
				exsp.addLine(col, x - 1, y + 7, z, x + 1, y + 7, z);
				exsp.addLine(col, x - 2, y + 6, z, x + 2, y + 6, z);
				exsp.addLine(col, x - 3, y + 5, z, x + 3, y + 5, z);
				exsp.addLine(col, x - 4, y + 4, z, x + 4, y + 4, z);
				
				col = getDirectionColor(gx, gy, z, Cell.NSWE_WEST);
				exsp.addLine(col, x - 7, y - 1, z, x - 7, y + 1, z);
				exsp.addLine(col, x - 6, y - 2, z, x - 6, y + 2, z);
				exsp.addLine(col, x - 5, y - 3, z, x - 5, y + 3, z);
				exsp.addLine(col, x - 4, y - 4, z, x - 4, y + 4, z);
				
				++iBlock;
			}
		}
		
		player.sendPacket(exsp);
	}
	
	/**
	 * difference between x values: never above 1<br>
	 * difference between y values: never above 1
	 * @param lastX
	 * @param lastY
	 * @param x
	 * @param y
	 * @return
	 */
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