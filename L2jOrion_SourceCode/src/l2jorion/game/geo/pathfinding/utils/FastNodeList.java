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
package l2jorion.game.geo.pathfinding.utils;

import java.util.ArrayList;

import l2jorion.game.geo.pathfinding.AbstractNode;

public class FastNodeList
{
	private final ArrayList<AbstractNode<?>> _list;
	
	public FastNodeList(int size)
	{
		_list = new ArrayList<>(size);
	}
	
	public void add(AbstractNode<?> n)
	{
		_list.add(n);
	}
	
	public boolean contains(AbstractNode<?> n)
	{
		return _list.contains(n);
	}
	
	public boolean containsRev(AbstractNode<?> n)
	{
		return _list.lastIndexOf(n) != -1;
	}
}
