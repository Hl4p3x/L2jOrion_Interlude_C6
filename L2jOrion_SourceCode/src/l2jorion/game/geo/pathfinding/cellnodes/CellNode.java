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