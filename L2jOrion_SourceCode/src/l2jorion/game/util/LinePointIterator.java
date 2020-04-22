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
package l2jorion.game.util;

public final class LinePointIterator
{
	// src is moved towards dst in next()
	private int _srcX;
	private int _srcY;
	private final int _dstX;
	private final int _dstY;
	
	private final long _dx;
	private final long _dy;
	private final long _sx;
	private final long _sy;
	private long _error;
	
	private boolean _first;
	
	public LinePointIterator(int srcX, int srcY, int dstX, int dstY)
	{
		_srcX = srcX;
		_srcY = srcY;
		_dstX = dstX;
		_dstY = dstY;
		_dx = Math.abs((long) dstX - srcX);
		_dy = Math.abs((long) dstY - srcY);
		_sx = srcX < dstX ? 1 : -1;
		_sy = srcY < dstY ? 1 : -1;
		
		if (_dx >= _dy)
		{
			_error = _dx / 2;
		}
		else
		{
			_error = _dy / 2;
		}
		
		_first = true;
	}
	
	public boolean next()
	{
		if (_first)
		{
			_first = false;
			return true;
		}
		else if (_dx >= _dy)
		{
			if (_srcX != _dstX)
			{
				_srcX += _sx;
				
				_error += _dy;
				if (_error >= _dx)
				{
					_srcY += _sy;
					_error -= _dx;
				}
				
				return true;
			}
		}
		else
		{
			if (_srcY != _dstY)
			{
				_srcY += _sy;
				
				_error += _dx;
				if (_error >= _dy)
				{
					_srcX += _sx;
					_error -= _dy;
				}
				
				return true;
			}
		}
		
		return false;
	}
	
	public int x()
	{
		return _srcX;
	}
	
	public int y()
	{
		return _srcY;
	}
}
