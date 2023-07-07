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
package l2jorion.game.model.multisell;

public class MultiSellIngredient
{
	private int _itemId, _itemCount, _enchantmentLevel;
	private boolean _isTaxIngredient, _mantainIngredient;
	
	public MultiSellIngredient(final int itemId, final int itemCount, final boolean isTaxIngredient, final boolean mantainIngredient)
	{
		this(itemId, itemCount, 0, isTaxIngredient, mantainIngredient);
	}
	
	public MultiSellIngredient(final int itemId, final int itemCount, final int enchantmentLevel, final boolean isTaxIngredient, final boolean mantainIngredient)
	{
		setItemId(itemId);
		setItemCount(itemCount);
		setEnchantmentLevel(enchantmentLevel);
		setIsTaxIngredient(isTaxIngredient);
		setMantainIngredient(mantainIngredient);
	}
	
	public MultiSellIngredient(final MultiSellIngredient e)
	{
		_itemId = e.getItemId();
		_itemCount = e.getItemCount();
		_enchantmentLevel = e.getEnchantmentLevel();
		_isTaxIngredient = e.isTaxIngredient();
		_mantainIngredient = e.getMantainIngredient();
	}
	
	/**
	 * @param itemId The itemId to set.
	 */
	public void setItemId(final int itemId)
	{
		_itemId = itemId;
	}
	
	/**
	 * @return Returns the itemId.
	 */
	public int getItemId()
	{
		return _itemId;
	}
	
	/**
	 * @param itemCount The itemCount to set.
	 */
	public void setItemCount(final int itemCount)
	{
		_itemCount = itemCount;
	}
	
	/**
	 * @return Returns the itemCount.
	 */
	public int getItemCount()
	{
		return _itemCount;
	}
	
	/**
	 * @param enchantmentLevel
	 */
	public void setEnchantmentLevel(final int enchantmentLevel)
	{
		_enchantmentLevel = enchantmentLevel;
	}
	
	/**
	 * @return Returns the itemCount.
	 */
	public int getEnchantmentLevel()
	{
		return _enchantmentLevel;
	}
	
	public void setIsTaxIngredient(final boolean isTaxIngredient)
	{
		_isTaxIngredient = isTaxIngredient;
	}
	
	public boolean isTaxIngredient()
	{
		return _isTaxIngredient;
	}
	
	public void setMantainIngredient(final boolean mantainIngredient)
	{
		_mantainIngredient = mantainIngredient;
	}
	
	public boolean getMantainIngredient()
	{
		return _mantainIngredient;
	}
}
