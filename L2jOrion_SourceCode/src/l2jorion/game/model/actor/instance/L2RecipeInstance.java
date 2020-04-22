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
package l2jorion.game.model.actor.instance;

/**
 * This class describes a RecipeList component (1 line of the recipe : Item-Quantity needed).
 */
public class L2RecipeInstance
{
	
	/** The Identifier of the item needed in the L2RecipeInstance. */
	private final int _itemId;
	
	/** The item quantity needed in the L2RecipeInstance. */
	private final int _quantity;
	
	/**
	 * Constructor of L2RecipeInstance (create a new line in a RecipeList).<BR>
	 * <BR>
	 * @param itemId the item id
	 * @param quantity the quantity
	 */
	public L2RecipeInstance(final int itemId, final int quantity)
	{
		_itemId = itemId;
		_quantity = quantity;
	}
	
	/**
	 * Return the Identifier of the L2RecipeInstance Item needed.<BR>
	 * <BR>
	 * @return the item id
	 */
	public int getItemId()
	{
		return _itemId;
	}
	
	/**
	 * Return the Item quantity needed of the L2RecipeInstance.<BR>
	 * <BR>
	 * @return the quantity
	 */
	public int getQuantity()
	{
		return _quantity;
	}
	
}
