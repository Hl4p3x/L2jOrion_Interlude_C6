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
