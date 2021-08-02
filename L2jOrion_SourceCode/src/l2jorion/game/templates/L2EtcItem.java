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

public final class L2EtcItem extends L2Item
{
	/**
	 * Constructor for EtcItem.
	 * @see L2Item constructor
	 * @param type : L2EtcItemType designating the type of object Etc
	 * @param set : StatsSet designating the set of couples (key,value) for description of the Etc
	 */
	public L2EtcItem(final L2EtcItemType type, final StatsSet set)
	{
		super(type, set);
	}
	
	/**
	 * Returns the type of Etc Item
	 * @return L2EtcItemType
	 */
	@Override
	public L2EtcItemType getItemType()
	{
		return (L2EtcItemType) super._type;
	}
	
	/**
	 * Returns if the item is consumable
	 * @return boolean
	 */
	@Override
	public final boolean isConsumable()
	{
		return getItemType() == L2EtcItemType.SHOT || getItemType() == L2EtcItemType.POTION;
	}
	
	/**
	 * Returns the ID of the Etc item after applying the mask.
	 * @return int : ID of the EtcItem
	 */
	@Override
	public int getItemMask()
	{
		return getItemType().mask();
	}
	
}
