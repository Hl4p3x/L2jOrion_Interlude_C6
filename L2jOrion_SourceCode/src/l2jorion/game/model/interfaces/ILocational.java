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
package l2jorion.game.model.interfaces;

/**
 * Object world location storage interface.
 * @author xban1x
 */
public interface ILocational
{
	/**
	 * Gets the X coordinate of this object.
	 * @return the X coordinate
	 */
	public int getX();
	
	/**
	 * Gets the Y coordinate of this object.
	 * @return the current Y coordinate
	 */
	public int getY();
	
	/**
	 * Gets the Z coordinate of this object.
	 * @return the current Z coordinate
	 */
	public int getZ();
	
	/**
	 * Gets the heading of this object.
	 * @return the current heading
	 */
	public int getHeading();
	
	/**
	 * Gets the instance zone ID of this object.
	 * @return the ID of the instance zone this object is currently in (0 - not in any instance)
	 */
	public int getInstanceId();
	
	/**
	 * Gets this object's location.
	 * @return a {@link ILocational} object containing the current position of this object
	 */
	public ILocational getLocation();
}
