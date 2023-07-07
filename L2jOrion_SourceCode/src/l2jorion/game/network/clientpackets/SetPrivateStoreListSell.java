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
import l2jorion.game.network.serverpackets.PrivateStoreManageListSell;
import l2jorion.game.network.serverpackets.PrivateStoreMsgSell;
import l2jorion.game.network.serverpackets.SystemMessage;
import l2jorion.game.util.Util;

public class SetPrivateStoreListSell extends PacketClient
{
	private int _count;
	private boolean _packageSale;
	private int[] _items; // count * 3
	
	@Override
	protected void readImpl()
	{
		_packageSale = readD() == 1;
		_count = readD();
		
		if (_count <= 0 || _count * 12 > _buf.remaining() || _count > Config.MAX_ITEM_IN_PACKET)
		{
			_count = 0;
			_items = null;
			return;
		}
		
		_items = new int[_count * 3];
		
		for (int x = 0; x < _count; x++)
		{
			int objectId = readD();
			_items[x * 3 + 0] = objectId;
			long cnt = readD();
			
			if (cnt > Integer.MAX_VALUE || cnt < 0)
			{
				_count = 0;
				_items = null;
				return;
			}
			
			_items[x * 3 + 1] = (int) cnt;
			int price = readD();
			_items[x * 3 + 2] = price;
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
			player.sendPacket(new PrivateStoreManageListSell(player));
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (Config.RON_CUSTOM && !player.isInsideZone(ZoneId.ZONE_PEACE))
		{
			player.sendPacket(new PrivateStoreManageListSell(player));
			player.sendPacket(SystemMessageId.NO_PRIVATE_STORE_HERE);
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (player.isInsideZone(ZoneId.ZONE_NOSTORE))
		{
			player.sendPacket(new PrivateStoreManageListSell(player));
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
		
		TradeList tradeList = player.getSellList();
		tradeList.clear();
		tradeList.setPackaged(_packageSale);
		
		long totalCost = player.getAdena();
		for (int i = 0; i < _count; i++)
		{
			int objectId = _items[i * 3 + 0];
			int count = _items[i * 3 + 1];
			int price = _items[i * 3 + 2];
			
			if (price <= 0)
			{
				String msgErr = "[SetPrivateStoreListSell] player " + getClient().getActiveChar().getName() + " tried an overflow exploit (use PHX), ban this player!";
				Util.handleIllegalPlayerAction(getClient().getActiveChar(), msgErr, Config.DEFAULT_PUNISH);
				_count = 0;
				_items = null;
				return;
			}
			
			totalCost += price;
			if (totalCost > Integer.MAX_VALUE)
			{
				player.sendPacket(new PrivateStoreManageListSell(player));
				player.sendPacket(new SystemMessage(SystemMessageId.YOU_HAVE_EXCEEDED_QUANTITY_THAT_CAN_BE_INPUTTED));
				return;
			}
			
			if (tradeList.isBuffer())
			{
				tradeList.addBuff(objectId, 1, price);
			}
			else
			{
				tradeList.addItem(objectId, count, price);
			}
		}
		
		if (_count <= 0)
		{
			player.setPrivateStoreType(L2PcInstance.STORE_PRIVATE_NONE);
			player.broadcastUserInfo();
			return;
		}
		
		if (player.isProcessingTransaction())
		{
			player.sendPacket(new PrivateStoreManageListSell(player));
			player.sendMessage("Store mode are disable while trading.");
			return;
		}
		
		// Check maximum number of allowed slots for pvt shops
		if (!tradeList.isBuffer())
		{
			if (_count > player.GetPrivateSellStoreLimit())
			{
				player.sendPacket(new PrivateStoreManageListSell(player));
				player.sendPacket(new SystemMessage(SystemMessageId.YOU_HAVE_EXCEEDED_QUANTITY_THAT_CAN_BE_INPUTTED));
				return;
			}
		}
		
		player.sitDown();
		
		if (_packageSale)
		{
			player.setPrivateStoreType(L2PcInstance.STORE_PRIVATE_PACKAGE_SELL);
		}
		else
		{
			player.setPrivateStoreType(L2PcInstance.STORE_PRIVATE_SELL);
		}
		
		player.broadcastUserInfo();
		player.broadcastPacket(new PrivateStoreMsgSell(player));
	}
	
	@Override
	public String getType()
	{
		return "[C] 74 SetPrivateStoreListSell";
	}
}