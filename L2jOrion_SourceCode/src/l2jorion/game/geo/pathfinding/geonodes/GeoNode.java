package l2jorion.game.geo.pathfinding.geonodes;

import l2jorion.game.geo.pathfinding.AbstractNode;

public class GeoNode extends AbstractNode<GeoNodeLoc>
{
	private final int _neighborsIdx;
	private short _cost;
	private GeoNode[] _neighbors;
	
	public GeoNode(GeoNodeLoc Loc, int Neighbors_idx)
	{
		super(Loc);
		_neighborsIdx = Neighbors_idx;
	}
	
	public short getCost()
	{
		return _cost;
	}
	
	public void setCost(int cost)
	{
		_cost = (short) cost;
	}
	
	public GeoNode[] getNeighbors()
	{
		return _neighbors;
	}
	
	public void attachNeighbors(GeoNode[] neighbors)
	{
		_neighbors = neighbors;
	}
	
	public int getNeighborsIdx()
	{
		return _neighborsIdx;
	}
}
