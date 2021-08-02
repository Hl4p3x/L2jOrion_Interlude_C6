/* L2jOrion Project - www.l2jorion.com 
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
/**
 coded by Balancer
 balancer@balancer.ru
 http://balancer.ru

 version 0.1, 2005-03-12
 */

package l2jorion.game.model;

import java.awt.Polygon;

import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;
import l2jorion.util.random.Rnd;

public class L2Territory
{
	private static Logger LOG = LoggerFactory.getLogger(L2Territory.class);
	
	protected class Point
	{
		protected int _x, _y, _zmin, _zmax, _proc;
		
		Point(final int x, final int y, final int zmin, final int zmax, final int proc)
		{
			_x = x;
			_y = y;
			_zmin = zmin;
			_zmax = zmax;
			_proc = proc;
		}
	}
	
	private Point[] _points;
	// private String _terr;
	private int _xMin;
	private int _xMax;
	private int _yMin;
	private int _yMax;
	private int _zMin;
	private int _zMax;
	private int _procMax;
	private final Polygon poly;
	
	public L2Territory(/* String string */)
	{
		poly = new Polygon();
		_points = new Point[0];
		// _terr = string;
		_xMin = 999999;
		_xMax = -999999;
		_yMin = 999999;
		_yMax = -999999;
		_zMin = 999999;
		_zMax = -999999;
		_procMax = 0;
	}
	
	public void add(final int x, final int y, final int zmin, final int zmax, final int proc)
	{
		Point[] newPoints = new Point[_points.length + 1];
		System.arraycopy(_points, 0, newPoints, 0, _points.length);
		newPoints[_points.length] = new Point(x, y, zmin, zmax, proc);
		_points = newPoints;
		
		poly.addPoint(x, y);
		
		if (x < _xMin)
		{
			_xMin = x;
		}
		
		if (y < _yMin)
		{
			_yMin = y;
		}
		
		if (x > _xMax)
		{
			_xMax = x;
		}
		
		if (y > _yMax)
		{
			_yMax = y;
		}
		
		if (zmin < _zMin)
		{
			_zMin = zmin;
		}
		
		if (zmax > _zMax)
		{
			_zMax = zmax;
		}
		
		_procMax += proc;
		
		newPoints = null;
	}
	
	public void print()
	{
		for (final Point p : _points)
		{
			LOG.info("(" + p._x + "," + p._y + ")");
		}
	}
	
	public boolean isIntersect(final int x, final int y, final Point p1, final Point p2)
	{
		final double dy1 = p1._y - y;
		final double dy2 = p2._y - y;
		
		if (Math.signum(dy1) == Math.signum(dy2))
			return false;
		
		final double dx1 = p1._x - x;
		final double dx2 = p2._x - x;
		
		if (dx1 >= 0 && dx2 >= 0)
			return true;
		
		if (dx1 < 0 && dx2 < 0)
			return false;
		
		final double dx0 = dy1 * (p1._x - p2._x) / (p1._y - p2._y);
		
		return dx0 <= dx1;
	}
	
	public boolean isInside(final int x, final int y)
	{
		return poly.contains(x, y);
	}
	
	public Location getRandomPoint()
	{
		if (_procMax > 0)
		{
			int pos = 0;
			int rnd = Rnd.nextInt(_procMax);
			for (Point p1 : _points)
			{
				pos += p1._proc;
				if (rnd <= pos)
				{
					return new Location(p1._x, p1._y, Rnd.get(p1._zmin, p1._zmax));
				}
			}
			
		}
		for (int i = 0; i < 100; i++)
		{
			int x = Rnd.get(_xMin, _xMax);
			int y = Rnd.get(_yMin, _yMax);
			if (isInside(x, y))
			{
				double curdistance = 0;
				int zmin = _zMin;
				for (Point p1 : _points)
				{
					double dx = p1._x - x;
					double dy = p1._y - y;
					double distance = Math.sqrt((dx * dx) + (dy * dy));
					if ((curdistance == 0) || (distance < curdistance))
					{
						curdistance = distance;
						zmin = p1._zmin;
					}
				}
				return new Location(x, y, Rnd.get(zmin, _zMax));
			}
		}
		LOG.warn("Can't make point for territory");
		return null;
	}
	
	public int getProcMax()
	{
		return _procMax;
	}
	
	public int getYmin()
	{
		return _yMin;
	}
	
	public int getXmax()
	{
		return _xMax;
	}
	
	public int getXmin()
	{
		return _xMin;
	}
	
	public int getYmax()
	{
		return _yMax;
	}
	
	public int getZmin()
	{
		return _zMin;
	}
	
	public int getZmax()
	{
		return _zMax;
	}
}
