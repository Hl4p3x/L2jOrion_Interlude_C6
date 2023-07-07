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
package l2jorion.game.network.serverpackets;

import java.util.List;

import l2jorion.Config;
import l2jorion.game.model.L2TradeList;
import l2jorion.game.model.actor.instance.L2ItemInstance;
import l2jorion.game.network.PacketServer;
import l2jorion.game.templates.L2Item;

public class WearList extends PacketServer
{
	private static final String _S__EF_WEARLIST = "[S] EF WearList";
	
	private final int _listId;
	private final L2ItemInstance[] _list;
	private final int _money;
	private int _expertise;
	
	public WearList(final L2TradeList list, final int currentMoney, final int expertiseIndex)
	{
		_listId = list.getListId();
		final List<L2ItemInstance> lst = list.getItems();
		_list = lst.toArray(new L2ItemInstance[lst.size()]);
		_money = currentMoney;
		_expertise = expertiseIndex;
	}
	
	public WearList(final List<L2ItemInstance> lst, final int listId, final int currentMoney)
	{
		_listId = listId;
		_list = lst.toArray(new L2ItemInstance[lst.size()]);
		_money = currentMoney;
	}
	
	@Override
	protected final void writeImpl()
	{
		writeC(0xef);
		writeC(0xc0);
		writeC(0x13);
		writeC(0x00);
		writeC(0x00);
		writeD(_money);
		writeD(_listId);
		
		int newlength = 0;
		for (final L2ItemInstance item : _list)
		{
			if (item.getItem().getCrystalType() <= _expertise && item.isEquipable())
			{
				newlength++;
			}
		}
		writeH(newlength);
		
		for (final L2ItemInstance item : _list)
		{
			if (item.getItem().getCrystalType() <= _expertise && item.isEquipable())
			{
				writeD(item.getItemId());
				writeH(item.getItem().getType2()); // item type2
				
				if (item.getItem().getType1() != L2Item.TYPE1_ITEM_QUESTITEM_ADENA)
				{
					writeH(item.getItem().getBodyPart());
				}
				else
				{
					writeH(0x00);
				}
				
				writeD(Config.WEAR_PRICE);
			}
		}
	}
	
	@Override
	public String getType()
	{
		return _S__EF_WEARLIST;
	}
}
