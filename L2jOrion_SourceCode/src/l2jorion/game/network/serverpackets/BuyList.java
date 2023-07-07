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

public final class BuyList extends PacketServer
{
	private static final String _S__1D_BUYLIST = "[S] 11 BuyList";
	
	private final int _listId;
	private final L2ItemInstance[] _list;
	private final int _money;
	private double _taxRate = 0;
	
	public BuyList(final L2TradeList list, final int currentMoney)
	{
		_listId = list.getListId();
		final List<L2ItemInstance> lst = list.getItems();
		_list = lst.toArray(new L2ItemInstance[lst.size()]);
		_money = currentMoney;
	}
	
	public BuyList(final L2TradeList list, final int currentMoney, final double taxRate)
	{
		_listId = list.getListId();
		final List<L2ItemInstance> lst = list.getItems();
		_list = lst.toArray(new L2ItemInstance[lst.size()]);
		_money = currentMoney;
		_taxRate = taxRate;
	}
	
	public BuyList(final List<L2ItemInstance> lst, final int listId, final int currentMoney)
	{
		_listId = listId;
		_list = lst.toArray(new L2ItemInstance[lst.size()]);
		_money = currentMoney;
	}
	
	@Override
	protected final void writeImpl()
	{
		writeC(0x11);
		writeD(_money); // current money
		writeD(_listId);
		
		writeH(_list.length);
		
		for (final L2ItemInstance item : _list)
		{
			if (item.getCount() > 0 || item.getCount() == -1)
			{
				writeH(item.getItem().getType1()); // item type1
				writeD(item.getObjectId());
				writeD(item.getItemId());
				if (item.getCount() < 0)
				{
					writeD(0x00); // max amount of items that a player can buy at a time (with this itemid)
				}
				else
				{
					writeD(item.getCount());
				}
				writeH(item.getItem().getType2()); // item type2
				writeH(0x00); // ?
				
				if (item.getItem().getType1() != L2Item.TYPE1_ITEM_QUESTITEM_ADENA)
				{
					writeD(item.getItem().getBodyPart()); // rev 415 slot 0006-lr.ear 0008-neck 0030-lr.finger 0040-head 0080-?? 0100-l.hand 0200-gloves 0400-chest 0800-pants 1000-feet 2000-?? 4000-r.hand 8000-r.hand
					writeH(item.getEnchantLevel()); // enchant level
					writeH(0x00); // ?
					writeH(0x00);
				}
				else
				{
					writeD(0x00); // rev 415 slot 0006-lr.ear 0008-neck 0030-lr.finger 0040-head 0080-?? 0100-l.hand 0200-gloves 0400-chest 0800-pants 1000-feet 2000-?? 4000-r.hand 8000-r.hand
					writeH(0x00); // enchant level
					writeH(0x00); // ?
					writeH(0x00);
				}
				
				if (item.getItemId() >= 3960 && item.getItemId() <= 4026)
				{
					writeD((int) (item.getPriceToSell() * Config.RATE_SIEGE_GUARDS_PRICE * (1 + _taxRate)));
				}
				else
				{
					writeD((int) (item.getPriceToSell() * (1 + _taxRate)));
				}
			}
		}
	}
	
	@Override
	public String getType()
	{
		return _S__1D_BUYLIST;
	}
}
