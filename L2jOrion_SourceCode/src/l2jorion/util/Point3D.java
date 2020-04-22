/*
 * $Header: Point3D.java, 19/07/2005 21:33:07 luisantonioa Exp $
 *
 * $Author: luisantonioa $
 * $Date: 19/07/2005 21:33:07 $
 * $Revision: 1 $
 * $Log: Point3D.java,v $
 * Revision 1  19/07/2005 21:33:07  luisantonioa
 * Added copyright notice
 *
 *
 * L2jOrion Project - www.l2jorion.com 
 * 
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
package l2jorion.util;

import java.io.Serializable;

public class Point3D implements Serializable
{
	private static final long serialVersionUID = 4638345252031872576L;
	
	private volatile int _x, _y, _z;
	
	public Point3D(int pX, int pY, int pZ)
	{
		_x = pX;
		_y = pY;
		_z = pZ;
	}
	
	@Override
	public String toString()
	{
		return "(" + _x + ", " + _y + ", " + _z + ")";
	}
	
	@Override
	public int hashCode()
	{
		return _x ^ _y ^ _z;
	}
	
	@Override
	public boolean equals(Object o)
	{
		if (o instanceof Point3D)
		{
			Point3D point3D = (Point3D) o;
			return ((point3D._x == _x) && (point3D._y == _y) && (point3D._z == _z));
		}
		return false;
	}
	
	public boolean equals(int pX, int pY, int pZ)
	{
		return (_x == pX) && (_y == pY) && (_z == pZ);
	}
	
	public int getX()
	{
		return _x;
	}
	
	public void setX(int pX)
	{
		_x = pX;
	}
	
	public int getY()
	{
		return _y;
	}
	
	public void setY(int pY)
	{
		_y = pY;
	}
	
	public int getZ()
	{
		return _z;
	}
	
	public void setZ(int pZ)
	{
		_z = pZ;
	}
	
	public void setXYZ(int pX, int pY, int pZ)
	{
		_x = pX;
		_y = pY;
		_z = pZ;
	}
}
