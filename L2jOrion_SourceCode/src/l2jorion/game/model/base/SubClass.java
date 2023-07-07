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
package l2jorion.game.model.base;

import l2jorion.Config;
import l2jorion.game.datatables.xml.ExperienceData;

public final class SubClass
{
	private PlayerClass _class;
	private long _exp = ExperienceData.getInstance().getExpForLevel(Config.BASE_SUBCLASS_LEVEL);
	private int _sp = 0;
	private int _level = Config.BASE_SUBCLASS_LEVEL;
	private int _classIndex = 1;
	
	public SubClass(final int classId, final long exp, final int sp, final byte level, final int classIndex)
	{
		_class = PlayerClass.values()[classId];
		_exp = exp;
		_sp = sp;
		_level = level;
		_classIndex = classIndex;
	}
	
	public SubClass(final int classId, final int classIndex)
	{
		// Used for defining a sub class using default values for XP, SP and player level.
		_class = PlayerClass.values()[classId];
		_classIndex = classIndex;
	}
	
	public SubClass()
	{
		// Used for specifying ALL attributes of a sub class directly,
		// using the preset default values.
	}
	
	public PlayerClass getClassDefinition()
	{
		return _class;
	}
	
	public int getClassId()
	{
		return _class.ordinal();
	}
	
	public long getExp()
	{
		return _exp;
	}
	
	public int getSp()
	{
		return _sp;
	}
	
	public int getLevel()
	{
		return _level;
	}
	
	public int getClassIndex()
	{
		return _classIndex;
	}
	
	public void setClassId(final int classId)
	{
		_class = PlayerClass.values()[classId];
	}
	
	public void setExp(long expValue)
	{
		if (expValue > ExperienceData.getInstance().getExpForLevel(Config.MAX_SUBCLASS_LEVEL))
		{
			expValue = ExperienceData.getInstance().getExpForLevel(Config.MAX_SUBCLASS_LEVEL);
		}
		
		_exp = expValue;
	}
	
	public void setSp(final int spValue)
	{
		_sp = spValue;
	}
	
	public void setClassIndex(final int classIndex)
	{
		_classIndex = classIndex;
	}
	
	public void setLevel(int levelValue)
	{
		if (levelValue > Config.MAX_SUBCLASS_LEVEL - 1)
		{
			levelValue = Config.MAX_SUBCLASS_LEVEL - 1;
		}
		else if (levelValue < Config.BASE_SUBCLASS_LEVEL)
		{
			levelValue = Config.BASE_SUBCLASS_LEVEL;
		}
		
		_level = levelValue;
	}
	
	public void incLevel()
	{
		if (getLevel() == Config.MAX_SUBCLASS_LEVEL - 1)
		{
			return;
		}
		_level++;
		setExp(ExperienceData.getInstance().getExpForLevel(getLevel()));
	}
	
	public void decLevel()
	{
		if (getLevel() == Config.BASE_SUBCLASS_LEVEL)
		{
			return;
		}
		
		_level--;
		setExp(ExperienceData.getInstance().getExpForLevel(getLevel()));
	}
}
