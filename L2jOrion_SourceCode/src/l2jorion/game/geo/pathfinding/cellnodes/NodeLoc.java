package l2jorion.game.geo.pathfinding.cellnodes;

import l2jorion.game.geo.GeoData;
import l2jorion.game.geo.geodriver.Cell;
import l2jorion.game.geo.pathfinding.AbstractNodeLoc;

public class NodeLoc extends AbstractNodeLoc
{
	private int _x;
	private int _y;
	private boolean _goNorth;
	private boolean _goEast;
	private boolean _goSouth;
	private boolean _goWest;
	private int _geoHeight;
	
	public NodeLoc(int x, int y, int z)
	{
		set(x, y, z);
	}
	
	public void set(int x, int y, int z)
	{
		_x = x;
		_y = y;
		_goNorth = GeoData.getInstance().checkNearestNswe(x, y, z, Cell.NSWE_NORTH);
		_goEast = GeoData.getInstance().checkNearestNswe(x, y, z, Cell.NSWE_EAST);
		_goSouth = GeoData.getInstance().checkNearestNswe(x, y, z, Cell.NSWE_SOUTH);
		_goWest = GeoData.getInstance().checkNearestNswe(x, y, z, Cell.NSWE_WEST);
		_geoHeight = GeoData.getInstance().getNearestZ(x, y, z);
	}
	
	public boolean canGoNorth()
	{
		return _goNorth;
	}
	
	public boolean canGoEast()
	{
		return _goEast;
	}
	
	public boolean canGoSouth()
	{
		return _goSouth;
	}
	
	public boolean canGoWest()
	{
		return _goWest;
	}
	
	public boolean canGoNone()
	{
		return !canGoNorth() && !canGoEast() && !canGoSouth() && !canGoWest();
	}
	
	public boolean canGoAll()
	{
		return canGoNorth() && canGoEast() && canGoSouth() && canGoWest();
	}
	
	@Override
	public int getX()
	{
		return GeoData.getInstance().getWorldX(_x);
	}
	
	@Override
	public int getY()
	{
		return GeoData.getInstance().getWorldY(_y);
	}
	
	@Override
	public int getZ()
	{
		return _geoHeight;
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
		
		int nswe = 0;
		if (canGoNorth())
		{
			nswe |= Cell.NSWE_NORTH;
		}
		if (canGoEast())
		{
			nswe |= Cell.NSWE_EAST;
		}
		if (canGoSouth())
		{
			nswe |= Cell.NSWE_SOUTH;
		}
		if (canGoWest())
		{
			nswe |= Cell.NSWE_WEST;
		}
		
		result = (prime * result) + (((_geoHeight & 0xFFFF) << 1) | nswe);
		return result;
		// return super.hashCode();
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
		if (!(obj instanceof NodeLoc))
		{
			return false;
		}
		final NodeLoc other = (NodeLoc) obj;
		if (_x != other._x)
		{
			return false;
		}
		if (_y != other._y)
		{
			return false;
		}
		if (_goNorth != other._goNorth)
		{
			return false;
		}
		if (_goEast != other._goEast)
		{
			return false;
		}
		if (_goSouth != other._goSouth)
		{
			return false;
		}
		if (_goWest != other._goWest)
		{
			return false;
		}
		if (_geoHeight != other._geoHeight)
		{
			return false;
		}
		return true;
	}
}
