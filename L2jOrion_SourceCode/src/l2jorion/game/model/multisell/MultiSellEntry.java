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

import java.util.List;

import javolution.util.FastList;

/**
 * @author programmos
 */
public class MultiSellEntry
{
	private int _entryId;
	
	private final List<MultiSellIngredient> _products = new FastList<>();
	private final List<MultiSellIngredient> _ingredients = new FastList<>();
	
	/**
	 * @param entryId The entryId to set.
	 */
	public void setEntryId(final int entryId)
	{
		_entryId = entryId;
	}
	
	/**
	 * @return Returns the entryId.
	 */
	public int getEntryId()
	{
		return _entryId;
	}
	
	/**
	 * @param product The product to add.
	 */
	public void addProduct(final MultiSellIngredient product)
	{
		_products.add(product);
	}
	
	/**
	 * @return Returns the products.
	 */
	public List<MultiSellIngredient> getProducts()
	{
		return _products;
	}
	
	/**
	 * @param ingredient The ingredients to set.
	 */
	public void addIngredient(final MultiSellIngredient ingredient)
	{
		_ingredients.add(ingredient);
	}
	
	/**
	 * @return Returns the ingredients.
	 */
	public List<MultiSellIngredient> getIngredients()
	{
		return _ingredients;
	}
}
