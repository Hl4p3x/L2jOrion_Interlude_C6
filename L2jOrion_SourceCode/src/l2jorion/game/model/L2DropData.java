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
package l2jorion.game.model;

import java.util.Arrays;

public class L2DropData
{
	public static final int MAX_CHANCE = 1000000;
	
	private int _itemId;
	private int _minDrop;
	private int _maxDrop;
	private int _chance;
	private int _enchantMin;
	private int _enchantMax;
	private int _category;
	private String _questID = null;
	private String[] _stateID = null;
	
	public int getItemId()
	{
		return _itemId;
	}
	
	public void setItemId(final int itemId)
	{
		_itemId = itemId;
	}
	
	public int getMinDrop()
	{
		return _minDrop;
	}
	
	public int getMaxDrop()
	{
		return _maxDrop;
	}
	
	public int getChance()
	{
		return _chance;
	}
	
	public int getMinEnchant()
	{
		return _enchantMin;
	}
	
	public int getMaxEnchant()
	{
		return _enchantMax;
	}
	
	public void setMinDrop(final int mindrop)
	{
		_minDrop = mindrop;
	}
	
	public void setMaxDrop(final int maxdrop)
	{
		_maxDrop = maxdrop;
	}
	
	public void setChance(final int chance)
	{
		_chance = chance;
	}
	
	public void setMinEnchant(final int minEnchant)
	{
		_enchantMin = minEnchant;
	}
	
	public void setMaxEnchant(final int maxEnchant)
	{
		_enchantMax = maxEnchant;
	}
	
	public void setCategory(final int cat)
	{
		_category = cat;
	}
	
	public int getCategory()
	{
		return _category;
	}
	
	public String[] getStateIDs()
	{
		return _stateID;
	}
	
	public void addStates(final String[] list)
	{
		_stateID = list;
	}
	
	public String getQuestID()
	{
		return _questID;
	}
	
	public void setQuestID(final String questID)
	{
		_questID = questID;
	}
	
	public boolean isQuestDrop()
	{
		return _questID != null && _stateID != null;
	}
	
	@Override
	public String toString()
	{
		String out = "ItemID: " + getItemId() + " Min: " + getMinDrop() + " Max: " + getMaxDrop() + " Chance: " + getChance() / 10000.0 + "%";
		if (isQuestDrop())
		{
			out += " QuestID: " + getQuestID() + " StateID's: " + Arrays.toString(getStateIDs());
		}
		
		return out;
	}
	
	@Override
	public boolean equals(final Object o)
	{
		if (o instanceof L2DropData)
		{
			final L2DropData drop = (L2DropData) o;
			return drop.getItemId() == getItemId();
		}
		return false;
	}
	
	@Override
	public int hashCode()
	{
		return getItemId();
	}
}
