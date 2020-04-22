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
package l2jorion.game.geodriver.blocks;

import java.nio.ByteBuffer;

import l2jorion.game.geodriver.IBlock;

public final class ComplexBlock implements IBlock
{
	private final short[] _data;
	
	public ComplexBlock(ByteBuffer bb)
	{
		_data = new short[IBlock.BLOCK_CELLS];
		for (int cellOffset = 0; cellOffset < IBlock.BLOCK_CELLS; cellOffset++)
		{
			_data[cellOffset] = bb.getShort();
		}
	}
	
	private short _getCellData(int geoX, int geoY)
	{
		return _data[((geoX % IBlock.BLOCK_CELLS_X) * IBlock.BLOCK_CELLS_Y) + (geoY % IBlock.BLOCK_CELLS_Y)];
	}
	
	private byte _getCellNSWE(int geoX, int geoY)
	{
		return (byte) (_getCellData(geoX, geoY) & 0x000F);
	}
	
	private int _getCellHeight(int geoX, int geoY)
	{
		short height = (short) (_getCellData(geoX, geoY) & 0x0FFF0);
		return height >> 1;
	}
	
	@Override
	public boolean checkNearestNswe(int geoX, int geoY, int worldZ, int nswe)
	{
		return (_getCellNSWE(geoX, geoY) & nswe) == nswe;
	}
	
	@Override
	public int getNearestZ(int geoX, int geoY, int worldZ)
	{
		return _getCellHeight(geoX, geoY);
	}
	
	@Override
	public int getNextLowerZ(int geoX, int geoY, int worldZ)
	{
		int cellHeight = _getCellHeight(geoX, geoY);
		return cellHeight <= worldZ ? cellHeight : worldZ;
	}
	
	@Override
	public int getNextHigherZ(int geoX, int geoY, int worldZ)
	{
		int cellHeight = _getCellHeight(geoX, geoY);
		return cellHeight >= worldZ ? cellHeight : worldZ;
	}
}
