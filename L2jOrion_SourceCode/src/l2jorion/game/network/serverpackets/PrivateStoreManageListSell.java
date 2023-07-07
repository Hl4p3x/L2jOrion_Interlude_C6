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

import l2jorion.game.idfactory.IdFactory;
import l2jorion.game.model.L2Skill;
import l2jorion.game.model.L2World;
import l2jorion.game.model.TradeList;
import l2jorion.game.model.actor.instance.L2ItemInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.network.PacketServer;

public class PrivateStoreManageListSell extends PacketServer
{
	private static final String _S__B3_PRIVATESELLLISTSELL = "[S] 9a PrivateSellListSell";
	
	private final int _objId;
	private int _playerAdena;
	private final boolean _packageSale;
	
	private TradeList.TradeItem[] _itemList;
	private TradeList.TradeItem[] _sellList;
	private int _sellItemId;
	
	private boolean _buffs = false;
	
	public PrivateStoreManageListSell(L2PcInstance player)
	{
		_objId = player.getObjectId();
		_playerAdena = player.getAdena();
		player.getSellList().updateItems();
		_packageSale = player.getSellList().isPackaged();
		
		player.getSellList().setBuffer(false);
		player.getSellList().setSellBuyItemId(57);
		_sellItemId = 57;
		
		_itemList = player.getInventory().getAvailableItems(player.getSellList());
		_sellList = player.getSellList().getItems();
	}
	
	public PrivateStoreManageListSell(L2PcInstance player, int sellItemId)
	{
		_objId = player.getObjectId();
		
		_playerAdena = player.getItemCount(sellItemId, -1);
		
		player.getSellList().updateItems();
		_packageSale = player.getSellList().isPackaged();
		
		player.getSellList().setBuffer(false);
		player.getSellList().setSellBuyItemId(sellItemId);
		_sellItemId = sellItemId;
		
		_itemList = player.getInventory().getAvailableItems(player.getSellList());
		_sellList = player.getSellList().getItems();
	}
	
	public PrivateStoreManageListSell(L2PcInstance player, boolean buffs, int sellItemId)
	{
		_objId = player.getObjectId();
		
		_playerAdena = player.getItemCount(sellItemId, -1);
		
		_packageSale = player.getSellList().isPackaged();
		_buffs = buffs;
		
		player.getSellList().setSellBuyItemId(sellItemId);
		
		_sellItemId = sellItemId;
		
		_itemList = getAvailableItems(player, player.getSellList());
		
		_sellList = player.getSellList().getItems();
	}
	
	public TradeList.TradeItem[] getAvailableItems(L2PcInstance player, TradeList tradeList)
	{
		tradeList.setBuffer(true);
		
		player.getSellList().list.clear();
		
		for (L2Skill skill : tradeList.getBuffs())
		{
			L2ItemInstance item = new L2ItemInstance(IdFactory.getInstance().getNextId(), skill.getId());
			
			item.setEnchantLevel(skill.getLevel());
			L2World.getInstance().storeObject(item);
			
			final TradeList.TradeItem adjItem = tradeList.adjustFakeItem(item);
			if (adjItem != null)
			{
				adjItem.setId(skill.getId());
				adjItem.setCount(1);
				adjItem.setObjectId(item.getObjectId());
				adjItem.setEnchant(skill.getLevel());
				
				player.getSellList().list.add(adjItem);
			}
		}
		
		return player.getSellList().list.toArray(new TradeList.TradeItem[player.getSellList().list.size()]);
	}
	
	@Override
	protected final void writeImpl()
	{
		writeC(0x9a);
		
		// section 1
		writeD(_objId);
		
		if (_buffs)
		{
			writeD(_packageSale ? 2 : 3);
		}
		else
		{
			writeD(_packageSale ? 1 : 0);
		}
		
		writeD(_playerAdena);
		
		// section2
		if (_buffs)
		{
			writeD(_itemList.length);
			for (final TradeList.TradeItem item : _itemList)
			{
				writeD(0); // item type
				writeD(item.getObjectId()); // obj id
				writeD(item.getId()); // skill id
				writeD(1); // count
				writeH(0x00);
				writeH(item.getEnchant()); // skill level
				writeH(0x00);
				writeD(_sellItemId); // sell item id
				writeD(item.getPrice()); // store price
			}
		}
		else
		{
			writeD(_itemList.length); // for potential sells
			for (final TradeList.TradeItem item : _itemList)
			{
				writeD(item.getItem().getType2());
				writeD(item.getObjectId());
				writeD(item.getItem().getItemId());
				writeD(item.getCount()); // count
				writeH(0x00);
				writeH(item.getEnchant());// enchant lvl
				writeH(0x00);
				// writeD(item.getItem().getBodyPart());
				writeD(_sellItemId); // sell item id
				writeD(item.getPrice()); // store price
			}
		}
		
		// section 3
		if (_buffs)
		{
			writeD(_sellList.length);
			for (final TradeList.TradeItem item : _sellList)
			{
				writeD(0);
				writeD(item.getObjectId());
				writeD(item.getId());
				writeD(1); // count
				writeH(0x00);
				writeH(item.getEnchant());// enchant lvl
				writeH(0x00);
				writeD(_sellItemId); // sell item id
				writeD(item.getPrice());// your price
				writeD(item.getItem().getReferencePrice()); // store price
			}
		}
		else
		{
			writeD(_sellList.length);
			for (final TradeList.TradeItem item : _sellList)
			{
				writeD(item.getItem().getType2());
				writeD(item.getObjectId());
				writeD(item.getItem().getItemId());
				writeD(item.getCount());
				writeH(0x00);
				writeH(item.getEnchant());// enchant lvl
				writeH(0x00);
				// writeD(item.getItem().getBodyPart());
				writeD(_sellItemId); // sell item id
				writeD(item.getPrice());// your price
				writeD(item.getItem().getReferencePrice()); // store price
			}
		}
		
	}
	
	@Override
	public String getType()
	{
		return _S__B3_PRIVATESELLLISTSELL;
	}
}