/*
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
package l2jorion.game.network.clientpackets;

import l2jorion.Config;
import l2jorion.game.model.TradeList;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.zone.ZoneId;
import l2jorion.game.network.PacketClient;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.ActionFailed;
import l2jorion.game.network.serverpackets.PrivateStoreManageListBuy;
import l2jorion.game.network.serverpackets.PrivateStoreMsgBuy;
import l2jorion.game.network.serverpackets.SystemMessage;

public final class SetPrivateStoreListBuy extends PacketClient
{
	private int _count;
	private int[] _items; // count * 3
	
	@Override
	protected void readImpl()
	{
		_count = readD();
		
		if (_count <= 0 || _count * 12 > _buf.remaining() || _count > Config.MAX_ITEM_IN_PACKET)
		{
			_count = 0;
			_items = null;
			return;
		}
		
		_items = new int[_count * 4];
		
		for (int x = 0; x < _count; x++)
		{
			int itemId = readD();
			_items[x * 4 + 0] = itemId;
			_items[(x * 4 + 3)] = readH();
			
			readH();
			long cnt = readD();
			
			if (cnt > Integer.MAX_VALUE || cnt < 0)
			{
				_count = 0;
				_items = null;
				return;
			}
			
			_items[x * 4 + 1] = (int) cnt;
			int price = readD();
			_items[x * 4 + 2] = price;
		}
	}
	
	@Override
	protected void runImpl()
	{
		L2PcInstance player = getClient().getActiveChar();
		if (player == null)
		{
			return;
		}
		
		if (!player.getAccessLevel().allowTransaction())
		{
			player.sendPacket(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT);
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (player.isCastingNow() || player.isCastingPotionNow() || player.isMovementDisabled() || player.inObserverMode() || player.getActiveEnchantItem() != null)
		{
			player.sendMessage("You cannot start store now.");
			player.sendPacket(new PrivateStoreManageListBuy(player));
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (Config.RON_CUSTOM && !player.isInsideZone(ZoneId.ZONE_PEACE))
		{
			player.sendPacket(new PrivateStoreManageListBuy(player));
			player.sendPacket(SystemMessageId.NO_PRIVATE_STORE_HERE);
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (player.isInsideZone(ZoneId.ZONE_NOSTORE))
		{
			player.sendPacket(new PrivateStoreManageListBuy(player));
			player.sendPacket(SystemMessageId.NO_PRIVATE_STORE_HERE);
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (player.getLevel() <= Config.MIN_LEVEL_FOR_TRADE)
		{
			player.sendPacket(new PrivateStoreManageListBuy(player));
			player.sendMessage("This action requires minimum " + (Config.MIN_LEVEL_FOR_TRADE + 1) + " level.");
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		TradeList tradeList = player.getBuyList();
		tradeList.clear();
		
		int cost = 0;
		
		for (int i = 0; i < _count; i++)
		{
			int itemId = _items[i * 4 + 0];
			int count = _items[i * 4 + 1];
			int price = _items[i * 4 + 2];
			int enchant = _items[i * 4 + 3];
			
			cost += count * price;
			if (cost > Integer.MAX_VALUE)
			{
				player.sendPacket(new SystemMessage(SystemMessageId.YOU_HAVE_EXCEEDED_QUANTITY_THAT_CAN_BE_INPUTTED));
				player.sendPacket(new PrivateStoreManageListBuy(player));
				return;
			}
			tradeList.addItemByItemId(itemId, count, price, enchant);
		}
		
		if (_count <= 0)
		{
			player.setPrivateStoreType(L2PcInstance.STORE_PRIVATE_NONE);
			player.broadcastUserInfo();
			return;
		}
		
		if (player.isProcessingTransaction())
		{
			player.sendMessage("Store mode are disable while trading.");
			player.sendPacket(new PrivateStoreManageListBuy(player));
			return;
		}
		
		// Check maximum number of allowed slots for pvt shops
		if (_count > player.GetPrivateBuyStoreLimit())
		{
			player.sendPacket(new PrivateStoreManageListBuy(player));
			player.sendPacket(new SystemMessage(SystemMessageId.YOU_HAVE_EXCEEDED_QUANTITY_THAT_CAN_BE_INPUTTED));
			return;
		}
		
		if (Config.RON_CUSTOM)
		{
			// Check for available funds
			if (player.getInventory().getItemByItemId(tradeList.getSellBuyItemId()) == null || cost > player.getInventory().getItemByItemId(tradeList.getSellBuyItemId()).getCount() || cost <= 0)
			{
				player.sendPacket(new PrivateStoreManageListBuy(player));
				player.sendPacket(new SystemMessage(SystemMessageId.THE_PURCHASE_PRICE_IS_HIGHER_THAN_MONEY));
				return;
			}
			
			player.sitDown();
			player.setPrivateStoreType(L2PcInstance.STORE_PRIVATE_BUY);
			player.broadcastUserInfo();
			player.broadcastPacket(new PrivateStoreMsgBuy(player));
			return;
		}
		
		// Check for available funds
		if (cost > player.getAdena() || cost <= 0)
		{
			player.sendPacket(new PrivateStoreManageListBuy(player));
			player.sendPacket(new SystemMessage(SystemMessageId.THE_PURCHASE_PRICE_IS_HIGHER_THAN_MONEY));
			return;
		}
		
		player.sitDown();
		player.setPrivateStoreType(L2PcInstance.STORE_PRIVATE_BUY);
		player.broadcastUserInfo();
		player.broadcastPacket(new PrivateStoreMsgBuy(player));
	}
	
	@Override
	public String getType()
	{
		return "[C] 91 SetPrivateStoreListBuy";
	}
}