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

public interface IRegion
{
	public static final int REGION_BLOCKS_X = 256;
	public static final int REGION_BLOCKS_Y = 256;
	
	public static final int REGION_BLOCKS = REGION_BLOCKS_X * REGION_BLOCKS_Y;
	
	/** Cells in a region on the x axis */
	public static final int REGION_CELLS_X = REGION_BLOCKS_X * IBlock.BLOCK_CELLS_X;
	/** Cells in a regioin on the y axis */
	public static final int REGION_CELLS_Y = REGION_BLOCKS_Y * IBlock.BLOCK_CELLS_Y;
	/** Cells in a region */
	public static final int REGION_CELLS = REGION_CELLS_X * REGION_CELLS_Y;
	
	boolean checkNearestNswe(int geoX, int geoY, int worldZ, int nswe);
	
	int getNearestZ(int geoX, int geoY, int worldZ);
	
	int getNextLowerZ(int geoX, int geoY, int worldZ);
	
	int getNextHigherZ(int geoX, int geoY, int worldZ);
	
	boolean hasGeo();
}
