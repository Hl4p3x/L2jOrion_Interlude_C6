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

public class MultiSellListContainer
{
	private int _listId;
	private boolean _applyTaxes = false;
	private boolean _maintainEnchantment = false;
	private String _npcId;
	
	List<MultiSellEntry> _entriesC;
	
	public MultiSellListContainer()
	{
		_entriesC = new FastList<>();
	}
	
	public void setNpcId(final String id)
	{
		_npcId = id;
	}
	
	public String getNpcId()
	{
		return _npcId;
	}
	
	public void setListId(final int listId)
	{
		_listId = listId;
	}
	
	public void setApplyTaxes(final boolean applyTaxes)
	{
		_applyTaxes = applyTaxes;
	}
	
	public void setMaintainEnchantment(final boolean maintainEnchantment)
	{
		_maintainEnchantment = maintainEnchantment;
	}
	
	/**
	 * @return Returns the listId.
	 */
	public int getListId()
	{
		return _listId;
	}
	
	public boolean getApplyTaxes()
	{
		return _applyTaxes;
	}
	
	public boolean getMaintainEnchantment()
	{
		return _maintainEnchantment;
	}
	
	public void addEntry(final MultiSellEntry e)
	{
		_entriesC.add(e);
	}
	
	public List<MultiSellEntry> getEntries()
	{
		return _entriesC;
	}
}
