/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307, USA.
 *
 * http://www.gnu.org/copyleft/gpl.html
 */
package l2jorion.game.model;

public final class Location
{
	public static final Location DUMMY_LOC = new Location(0, 0, 0);
	
	public int _x;
	public int _y;
	public int _z;
	public int _heading;
	
	public Location(int x, int y, int z)
	{
		_x = x;
		_y = y;
		_z = z;
	}
	
	public Location(int x, int y, int z, int heading)
	{
		_x = x;
		_y = y;
		_z = z;
		_heading = heading;
	}
	
	public Location(L2Object obj)
	{
		_x = obj.getX();
		_y = obj.getY();
		_z = obj.getZ();
	}
	
	public Location(L2Character obj)
	{
		_x = obj.getX();
		_y = obj.getY();
		_z = obj.getZ();
		_heading = obj.getHeading();
	}
	
	public int getX()
	{
		return _x;
	}
	
	public int getY()
	{
		return _y;
	}
	
	public int getZ()
	{
		return _z;
	}
	
	public int getHeading()
	{
		return _heading;
	}
	
	public void setX(int x)
	{
		_x = x;
	}
	
	public void setY(int y)
	{
		_y = y;
	}
	
	public void setZ(int z)
	{
		_z = z;
	}
	
	public void setHeading(int head)
	{
		_heading = head;
	}
	
	public void setXYZ(int x, int y, int z)
	{
		_x = x;
		_y = y;
		_z = z;
	}
	
	public boolean equals(int x, int y, int z)
	{
		if (_x == x && _y == y && _z == z)
		{
			return true;
		}
		
		return false;
	}
	
	public int getInstanceId()
	{
		return 0;
	}
}