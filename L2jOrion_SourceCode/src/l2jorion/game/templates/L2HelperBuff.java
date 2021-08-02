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
package l2jorion.game.templates;

public class L2HelperBuff
{
	/** Min level that the player must achieve to obtain this buff from Newbie Helper */
	private int _lowerLevel;
	
	/** Max level that the player mustn't exceed if it want to obtain this buff from Newbie Helper */
	private int _upperLevel;
	
	/** Identifier of the skill (buff) that the Newbie Helper must cast */
	private int _skillID;
	
	/** Level of the skill (buff) that the Newbie Helper must cast */
	private int _skillLevel;
	
	/**
	 * If True only Magus class will obtain this Buff <BR>
	 * If False only Fighter class will obtain this Buff
	 */
	private boolean _isMagicClass;
	
	/**
	 * Constructor of L2HelperBuff.<BR>
	 * <BR>
	 * @param set
	 */
	public L2HelperBuff(final StatsSet set)
	{
		
		_lowerLevel = set.getInteger("lowerLevel");
		_upperLevel = set.getInteger("upperLevel");
		_skillID = set.getInteger("skillID");
		_skillLevel = set.getInteger("skillLevel");
		
		if ("false".equals(set.getString("isMagicClass")))
		{
			_isMagicClass = false;
		}
		else
		{
			_isMagicClass = true;
		}
		
	}
	
	/**
	 * Returns the lower level that the L2PcInstance must achieve in order to obtain this buff
	 * @return int
	 */
	public int getLowerLevel()
	{
		return _lowerLevel;
	}
	
	/**
	 * Sets the lower level that the L2PcInstance must achieve in order to obtain this buff
	 * @param lowerLevel
	 */
	public void setLowerLevel(final int lowerLevel)
	{
		_lowerLevel = lowerLevel;
	}
	
	/**
	 * Returns the upper level that the L2PcInstance mustn't exceed in order to obtain this buff
	 * @return int
	 */
	public int getUpperLevel()
	{
		return _upperLevel;
	}
	
	/**
	 * Sets the upper level that the L2PcInstance mustn't exceed in order to obtain this buff
	 * @param upperLevel
	 */
	public void setUpperLevel(final int upperLevel)
	{
		_upperLevel = upperLevel;
	}
	
	/**
	 * Returns the ID of the buff that the L2PcInstance will receive
	 * @return int
	 */
	public int getSkillID()
	{
		return _skillID;
	}
	
	/**
	 * Sets the ID of the buff that the L2PcInstance will receive
	 * @param skillID
	 */
	public void setSkillID(final int skillID)
	{
		_skillID = skillID;
	}
	
	/**
	 * Returns the Level of the buff that the L2PcInstance will receive
	 * @return int
	 */
	public int getSkillLevel()
	{
		return _skillLevel;
	}
	
	/**
	 * Sets the Level of the buff that the L2PcInstance will receive
	 * @param skillLevel
	 */
	public void setSkillLevel(final int skillLevel)
	{
		_skillLevel = skillLevel;
	}
	
	/**
	 * Returns if this Buff can be cast on a fighter or a mystic
	 * @return boolean : False if it's a fighter class Buff
	 */
	public boolean isMagicClassBuff()
	{
		return _isMagicClass;
	}
	
	/**
	 * Sets if this Buff can be cast on a fighter or a mystic
	 * @param isMagicClass
	 */
	public void setIsMagicClass(final boolean isMagicClass)
	{
		_isMagicClass = isMagicClass;
	}
	
}
