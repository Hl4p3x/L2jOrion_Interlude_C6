package l2jorion.game.geo.pathfinding;

public abstract class AbstractNode<Loc extends AbstractNodeLoc>
{
	private Loc _loc;
	private AbstractNode<Loc> _parent;
	
	public AbstractNode(Loc loc)
	{
		_loc = loc;
	}
	
	public void setParent(AbstractNode<Loc> p)
	{
		_parent = p;
	}
	
	public AbstractNode<Loc> getParent()
	{
		return _parent;
	}
	
	public Loc getLoc()
	{
		return _loc;
	}
	
	public void setLoc(Loc l)
	{
		_loc = l;
	}
	
	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = (prime * result) + ((_loc == null) ? 0 : _loc.hashCode());
		return result;
	}
	
	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
		{
			return true;
		}
		if (obj == null)
		{
			return false;
		}
		if (!(obj instanceof AbstractNode))
		{
			return false;
		}
		final AbstractNode<?> other = (AbstractNode<?>) obj;
		if (_loc == null)
		{
			if (other._loc != null)
			{
				return false;
			}
		}
		else if (!_loc.equals(other._loc))
		{
			return false;
		}
		return true;
	}
}