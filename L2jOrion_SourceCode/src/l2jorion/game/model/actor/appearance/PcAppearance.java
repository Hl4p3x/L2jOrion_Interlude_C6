/*
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
package l2jorion.game.model.actor.appearance;

import l2jorion.game.model.actor.instance.L2PcInstance;

public class PcAppearance
{
	private L2PcInstance _owner;
	
	private byte _face;
	private byte _hairColor;
	private byte _hairStyle;
	private boolean _sex;
	private String _visibleName;
	private boolean _invisible = false;
	
	/** The hexadecimal Color of players name (white is 0xFFFFFF) */
	private int _nameColor = 0xFFFFFF;
	/** The hexadecimal Color of players title (white is 0xFFFF77) */
	private int _titleColor = 0xFFFF77;
	
	public PcAppearance(byte Face, byte HColor, byte HStyle, boolean Sex)
	{
		_face = Face;
		_hairColor = HColor;
		_hairStyle = HStyle;
		_sex = Sex;
	}
	
	public final byte getFace()
	{
		return _face;
	}
	
	public final String getVisibleName()
	{
		if (_visibleName == null)
		{
			_visibleName = getOwner().getName();
		}
		return _visibleName;
	}
	
	public final void setFace(int value)
	{
		_face = (byte) value;
	}
	
	public final byte getHairColor()
	{
		return _hairColor;
	}
	
	public final void setHairColor(int value)
	{
		_hairColor = (byte) value;
	}
	
	public final byte getHairStyle()
	{
		return _hairStyle;
	}
	
	public final void setHairStyle(int value)
	{
		_hairStyle = (byte) value;
	}
	
	public final boolean getSex()
	{
		return _sex;
	}
	
	public final void setSex(boolean isfemale)
	{
		_sex = isfemale;
	}
	
	public void setInvisible()
	{
		_invisible = true;
	}
	
	public void setVisible()
	{
		_invisible = false;
	}
	
	public boolean getInvisible()
	{
		return _invisible;
	}
	
	public int getNameColor()
	{
		return _nameColor;
	}
	
	public void setNameColor(final int nameColor)
	{
		_nameColor = nameColor;
	}
	
	public void setNameColor(final int red, final int green, final int blue)
	{
		_nameColor = (red & 0xFF) + ((green & 0xFF) << 8) + ((blue & 0xFF) << 16);
	}
	
	public int getTitleColor()
	{
		return _titleColor;
	}
	
	public void setTitleColor(int titleColor)
	{
		_titleColor = titleColor;
	}
	
	public void setTitleColor(int red, int green, int blue)
	{
		_titleColor = (red & 0xFF) + ((green & 0xFF) << 8) + ((blue & 0xFF) << 16);
	}
	
	public L2PcInstance getOwner()
	{
		return _owner;
	}
}
