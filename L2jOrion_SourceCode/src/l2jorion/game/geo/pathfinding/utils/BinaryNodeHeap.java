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

import l2jorion.game.geo.pathfinding.geonodes.GeoNode;

public class BinaryNodeHeap
{
	private final GeoNode[] _list;
	private int _size;
	
	public BinaryNodeHeap(int size)
	{
		_list = new GeoNode[size + 1];
		_size = 0;
	}
	
	public void add(GeoNode n)
	{
		_size++;
		int pos = _size;
		_list[pos] = n;
		while (pos != 1)
		{
			int p2 = pos / 2;
			if (_list[pos].getCost() <= _list[p2].getCost())
			{
				GeoNode temp = _list[p2];
				_list[p2] = _list[pos];
				_list[pos] = temp;
				pos = p2;
			}
			else
			{
				break;
			}
		}
	}
	
	public GeoNode removeFirst()
	{
		GeoNode first = _list[1];
		_list[1] = _list[_size];
		_list[_size] = null;
		_size--;
		int pos = 1;
		int cpos;
		int dblcpos;
		GeoNode temp;
		while (true)
		{
			cpos = pos;
			dblcpos = cpos * 2;
			if ((dblcpos + 1) <= _size)
			{
				if (_list[cpos].getCost() >= _list[dblcpos].getCost())
				{
					pos = dblcpos;
				}
				if (_list[pos].getCost() >= _list[dblcpos + 1].getCost())
				{
					pos = dblcpos + 1;
				}
			}
			else if (dblcpos <= _size)
			{
				if (_list[cpos].getCost() >= _list[dblcpos].getCost())
				{
					pos = dblcpos;
				}
			}
			
			if (cpos != pos)
			{
				temp = _list[cpos];
				_list[cpos] = _list[pos];
				_list[pos] = temp;
			}
			else
			{
				break;
			}
		}
		return first;
	}
	
	public boolean contains(GeoNode n)
	{
		if (_size == 0)
		{
			return false;
		}
		for (int i = 1; i <= _size; i++)
		{
			if (_list[i].equals(n))
			{
				return true;
			}
		}
		return false;
	}
	
	public boolean isEmpty()
	{
		return _size == 0;
	}
}
