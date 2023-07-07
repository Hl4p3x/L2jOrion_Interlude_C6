package l2jorion.game.geo.pathfinding.geonodes;

import l2jorion.game.geo.pathfinding.AbstractNodeLoc;
import l2jorion.game.model.L2World;

public class GeoNodeLoc extends AbstractNodeLoc
{
	private final short _x;
	private final short _y;
	private final short _z;
	
	public GeoNodeLoc(short x, short y, short z)
	{
		_x = x;
		_y = y;
		_z = z;
	}
	
	@Override
	public int getX()
	{
		return L2World.MAP_MIN_X + (_x * 128) + 48;
	}
	
	@Override
	public int getY()
	{
		return L2World.MAP_MIN_Y + (_y * 128) + 48;
	}
	
	@Override
	public int getZ()
	{
		return _z;
	}
	
	@Override
	public void setZ(short z)
	{
		//
	}
	
	@Override
	public int getNodeX()
	{
		return _x;
	}
	
	@Override
	public int getNodeY()
	{
		return _y;
	}
	
	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = (prime * result) + _x;
		result = (prime * result) + _y;
		result = (prime * result) + _z;
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
		if (!(obj instanceof GeoNodeLoc))
		{
			return false;
		}
		final GeoNodeLoc other = (GeoNodeLoc) obj;
		if (_x != other._x)
		{
			return false;
		}
		if (_y != other._y)
		{
			return false;
		}
		if (_z != other._z)
		{
			return false;
		}
		return true;
	}
}
