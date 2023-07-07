/*
 * $Header: MultiSellList.java, 2/08/2005 14:21:01 luisantonioa Exp $
 *
 * $Author: luisantonioa $
 * $Date: 2/08/2005 14:21:01 $
 * $Revision: 1 $
 * $Log: MultiSellList.java,v $
 * Revision 1  2/08/2005 14:21:01  luisantonioa
 * Added copyright notice
 *
 *
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
package l2jorion.game.network.serverpackets;

import l2jorion.game.datatables.sql.ItemTable;
import l2jorion.game.model.multisell.MultiSellEntry;
import l2jorion.game.model.multisell.MultiSellIngredient;
import l2jorion.game.model.multisell.MultiSellListContainer;
import l2jorion.game.network.PacketServer;

public class MultiSellList extends PacketServer
{
	private static final String _S__D0_MULTISELLLIST = "[S] D0 MultiSellList";
	
	protected int _listId, _page, _finished;
	protected MultiSellListContainer _list;
	
	public MultiSellList(final MultiSellListContainer list, final int page, final int finished)
	{
		_list = list;
		_listId = list.getListId();
		_page = page;
		_finished = finished;
	}
	
	@Override
	protected void writeImpl()
	{
		writeC(0xd0);
		writeD(_listId); // list id
		writeD(_page); // page
		writeD(_finished); // finished
		writeD(0x28); // size of pages
		writeD(_list == null ? 0 : _list.getEntries().size()); // list lenght
		
		if (_list != null)
		{
			for (final MultiSellEntry ent : _list.getEntries())
			{
				writeD(ent.getEntryId());
				writeD(0x00); // C6
				writeD(0x00); // C6
				writeC(1);
				writeH(ent.getProducts().size());
				writeH(ent.getIngredients().size());
				
				for (final MultiSellIngredient i : ent.getProducts())
				{
					writeH(i.getItemId());
					writeD(ItemTable.getInstance().getTemplate(i.getItemId()).getBodyPart());
					writeH(ItemTable.getInstance().getTemplate(i.getItemId()).getType2());
					writeD(i.getItemCount());
					writeH(i.getEnchantmentLevel()); // enchtant lvl
					writeD(0x00); // C6
					writeD(0x00); // C6
				}
				
				for (final MultiSellIngredient i : ent.getIngredients())
				{
					final int item = i.getItemId();
					int typeE = 65335;
					if (item != 65336 && item != 65436)
					{
						typeE = ItemTable.getInstance().getTemplate(i.getItemId()).getType2();
					}
					
					writeH(item); // ID
					writeH(typeE);
					writeD(i.getItemCount()); // Count
					writeH(i.getEnchantmentLevel()); // Enchant Level
					writeD(0x00); // C6
					writeD(0x00); // C6
				}
			}
		}
	}
	
	@Override
	public String getType()
	{
		return _S__D0_MULTISELLLIST;
	}
	
}