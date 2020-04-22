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
package l2jorion.game.geo.pathfinding.cellnodes;

import l2jorion.game.geo.pathfinding.AbstractNode;

public class CellNode extends AbstractNode<NodeLoc>
{
	private CellNode _next = null;
	private boolean _isInUse = true;
	private float _cost = -1000;
	
	public CellNode(NodeLoc loc)
	{
		super(loc);
	}
	
	public boolean isInUse()
	{
		return _isInUse;
	}
	
	public void setInUse()
	{
		_isInUse = true;
	}
	
	public CellNode getNext()
	{
		return _next;
	}
	
	public void setNext(CellNode next)
	{
		_next = next;
	}
	
	public float getCost()
	{
		return _cost;
	}
	
	public void setCost(double cost)
	{
		_cost = (float) cost;
	}
	
	public void free()
	{
		setParent(null);
		_cost = -1000;
		_isInUse = false;
		_next = null;
	}
}