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
package l2jorion.game.network.clientpackets;

import l2jorion.Config;
import l2jorion.game.managers.CursedWeaponsManager;
import l2jorion.game.model.ClanWarehouse;
import l2jorion.game.model.ItemContainer;
import l2jorion.game.model.actor.instance.L2FolkInstance;
import l2jorion.game.model.actor.instance.L2ItemInstance;
import l2jorion.game.model.actor.instance.L2NpcInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.network.PacketClient;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.ActionFailed;
import l2jorion.game.network.serverpackets.EnchantResult;
import l2jorion.game.network.serverpackets.InventoryUpdate;
import l2jorion.game.network.serverpackets.ItemList;
import l2jorion.game.network.serverpackets.StatusUpdate;
import l2jorion.game.network.serverpackets.SystemMessage;
import l2jorion.game.powerpack.PowerPackConfig;
import l2jorion.game.templates.L2EtcItemType;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;

public final class SendWareHouseDepositList extends PacketClient
{
	private static Logger LOG = LoggerFactory.getLogger(SendWareHouseDepositList.class);
	
	private int _count;
	private int[] _items;
	
	@Override
	protected void readImpl()
	{
		_count = readD();
		
		// check packet list size
		if (_count < 0 || _count * 8 > _buf.remaining() || _count > Config.MAX_ITEM_IN_PACKET)
		{
			_count = 0;
		}
		
		_items = new int[_count * 2];
		for (int i = 0; i < _count; i++)
		{
			final int objectId = readD();
			_items[i * 2 + 0] = objectId;
			final long cnt = readD();
			
			if (cnt > Integer.MAX_VALUE || cnt < 0)
			{
				_count = 0;
				_items = null;
				return;
			}
			
			_items[i * 2 + 1] = (int) cnt;
		}
	}
	
	@Override
	protected void runImpl()
	{
		if (_items == null)
		{
			return;
		}
		
		final L2PcInstance player = getClient().getActiveChar();
		if (player == null)
		{
			return;
		}
		
		final ItemContainer warehouse = player.getActiveWarehouse();
		
		if (!PowerPackConfig.GMSHOP_USECOMMAND && warehouse == null)
		{
			return;
		}
		
		final L2FolkInstance manager = player.getLastFolkNPC();
		
		if (!PowerPackConfig.GMSHOP_USECOMMAND)
		{
			if (manager == null || !player.isInsideRadius(manager, L2NpcInstance.INTERACTION_DISTANCE, false, false))
			{
				return;
			}
		}
		
		if (!getClient().getFloodProtectors().getTransaction().tryPerformAction("deposit"))
		{
			player.sendMessage("You depositing items too fast.");
			return;
		}
		
		if (player.getPrivateStoreType() != 0)
		{
			player.sendMessage("You can't deposit items when you are trading.");
			return;
		}
		
		// Like L2OFF you can't confirm a deposit when you are in trade.
		if (player.getActiveTradeList() != null)
		{
			player.sendMessage("You can't deposit items when you are trading.");
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (player.isCastingNow() || player.isCastingPotionNow())
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (warehouse instanceof ClanWarehouse && !player.getAccessLevel().allowTransaction())
		{
			player.sendMessage("Unsufficient privileges.");
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (player.isDead())
		{
			player.sendMessage("You can't deposit items while you are dead.");
			player.sendPacket(ActionFailed.STATIC_PACKET);
		}
		
		// Alt game - Karma punishment
		if (!Config.ALT_GAME_KARMA_PLAYER_CAN_USE_WAREHOUSE && player.getKarma() > 0)
		{
			return;
		}
		
		// Like L2OFF enchant window must close
		if (player.getActiveEnchantItem() != null)
		{
			sendPacket(new SystemMessage(SystemMessageId.ENCHANT_SCROLL_CANCELLED));
			player.sendPacket(new EnchantResult(0));
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		// Freight price from config or normal price per item slot (30)
		final int fee = _count * Config.ALT_WAREHOUSE_DEPOSIT_PRICE;
		int currentAdena = player.getAdena();
		int slots = 0;
		
		for (int i = 0; i < _count; i++)
		{
			final int objectId = _items[i * 2 + 0];
			final int count = _items[i * 2 + 1];
			
			// Check validity of requested item
			final L2ItemInstance item = player.checkItemManipulation(objectId, count, "deposit");
			if (item == null)
			{
				LOG.warn("Error depositing a warehouse object for char " + player.getName() + " (validity check)");
				_items[i * 2 + 0] = 0;
				_items[i * 2 + 1] = 0;
				continue;
			}
			
			if (warehouse instanceof ClanWarehouse && !item.isTradeable() || item.getItemType() == L2EtcItemType.QUEST)
			{
				return;
			}
			
			if (player.getLevel() < Config.PROTECTED_START_ITEMS_LVL && Config.LIST_PROTECTED_START_ITEMS.contains(item.getItemId()))
			{
				return;
			}
			
			// Calculate needed adena and slots
			if (item.getItemId() == 57)
			{
				currentAdena -= count;
			}
			
			if (!item.isStackable())
			{
				slots += count;
			}
			else if (warehouse.getItemByItemId(item.getItemId()) == null)
			{
				slots++;
			}
		}
		
		// Item Max Limit Check
		if (!warehouse.validateCapacity(slots))
		{
			sendPacket(new SystemMessage(SystemMessageId.YOU_HAVE_EXCEEDED_QUANTITY_THAT_CAN_BE_INPUTTED));
			return;
		}
		
		// Check if enough adena and charge the fee
		if (currentAdena < fee || !player.reduceAdena("Warehouse", fee, player.getLastFolkNPC(), false))
		{
			sendPacket(new SystemMessage(SystemMessageId.YOU_NOT_ENOUGH_ADENA));
			return;
		}
		
		// Proceed to the transfer
		final InventoryUpdate playerIU = Config.FORCE_INVENTORY_UPDATE ? null : new InventoryUpdate();
		for (int i = 0; i < _count; i++)
		{
			final int objectId = _items[i * 2 + 0];
			final int count = _items[i * 2 + 1];
			
			// check for an invalid item
			if (objectId == 0 && count == 0)
			{
				continue;
			}
			
			final L2ItemInstance oldItem = player.getInventory().getItemByObjectId(objectId);
			if (oldItem == null)
			{
				LOG.warn("Error depositing a warehouse object for char " + player.getName() + " (olditem == null)");
				continue;
			}
			
			final int itemId = oldItem.getItemId();
			
			if (itemId >= 6611 && itemId <= 6621 || itemId == 6842)
			{
				continue;
			}
			
			if (CursedWeaponsManager.getInstance().isCursed(itemId))
			{
				LOG.warn(player.getName() + " try to deposit Cursed Weapon on wherehouse.");
				continue;
			}
			
			final L2ItemInstance newItem = player.getInventory().transferItem("WarehouseDeposit", objectId, count, warehouse, player, player.getLastFolkNPC());
			if (newItem == null)
			{
				LOG.warn("Error depositing a warehouse object for char " + player.getName() + " (newitem == null)");
				continue;
			}
			
			if (playerIU != null)
			{
				if (oldItem.getCount() > 0 && oldItem != newItem)
				{
					playerIU.addModifiedItem(oldItem);
				}
				else
				{
					playerIU.addRemovedItem(oldItem);
				}
			}
		}
		
		// Send updated item list to the player
		if (playerIU != null)
		{
			player.sendPacket(playerIU);
		}
		else
		{
			player.sendPacket(new ItemList(player, false));
		}
		
		// Update current load status on player
		final StatusUpdate su = new StatusUpdate(player.getObjectId());
		su.addAttribute(StatusUpdate.CUR_LOAD, player.getCurrentLoad());
		player.sendPacket(su);
	}
	
	@Override
	public String getType()
	{
		return "[C] 31 SendWareHouseDepositList";
	}
}
