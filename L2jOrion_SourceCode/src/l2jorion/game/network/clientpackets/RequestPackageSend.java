/* L2jOrion Project - www.l2jorion.com 
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

import java.util.ArrayList;
import java.util.List;

import l2jorion.Config;
import l2jorion.game.model.ItemContainer;
import l2jorion.game.model.L2World;
import l2jorion.game.model.PcFreight;
import l2jorion.game.model.actor.instance.L2FolkInstance;
import l2jorion.game.model.actor.instance.L2ItemInstance;
import l2jorion.game.model.actor.instance.L2NpcInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.network.PacketClient;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.ActionFailed;
import l2jorion.game.network.serverpackets.InventoryUpdate;
import l2jorion.game.network.serverpackets.ItemList;
import l2jorion.game.network.serverpackets.StatusUpdate;
import l2jorion.game.network.serverpackets.SystemMessage;
import l2jorion.game.powerpack.PowerPackConfig;
import l2jorion.game.templates.L2EtcItemType;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;

public final class RequestPackageSend extends PacketClient
{
	private static Logger LOG = LoggerFactory.getLogger(RequestPackageSend.class);
	private final List<Item> _items = new ArrayList<>();
	private int _objectID;
	private int _slot;
	
	@Override
	protected void readImpl()
	{
		_objectID = readD();
		_slot = readD();
		
		if (_slot < 0 || _slot > 80)
		{
			_slot = -1;
			return;
		}
		
		for (int i = 0; i < _slot; i++)
		{
			final int id = readD();
			final int count = readD();
			_items.add(new Item(id, count));
		}
	}
	
	@Override
	protected void runImpl()
	{
		if (_slot == -1 || _items == null || _items.isEmpty() || !Config.ALLOW_FREIGHT)
		{
			return;
		}
		
		final L2PcInstance player = getClient().getActiveChar();
		
		if (player == null)
		{
			return;
		}
		
		if (player.getObjectId() == _objectID)
		{
			return;
		}
		
		final L2PcInstance target = L2PcInstance.load(_objectID);
		
		if (player.getAccountChars().size() < 1)
		{
			return;
		}
		else if (!player.getAccountChars().containsKey(_objectID))
		{
			return;
		}
		
		if (L2World.getInstance().getPlayer(_objectID) != null)
		{
			return;
		}
		
		if (!getClient().getFloodProtectors().getTransaction().tryPerformAction("deposit"))
		{
			player.sendMessage("You depositing items too fast.");
			return;
		}
		
		final PcFreight freight = target.getFreight();
		player.setActiveWarehouse(freight);
		target.deleteMe();
		final ItemContainer warehouse = player.getActiveWarehouse();
		
		if (!PowerPackConfig.GMSHOP_USECOMMAND && warehouse == null)
		{
			return;
		}
		
		final L2FolkInstance manager = player.getLastFolkNPC();
		
		if (!PowerPackConfig.GMSHOP_USECOMMAND)
		{
			if ((manager == null || !player.isInsideRadius(manager, L2NpcInstance.INTERACTION_DISTANCE, false, false)) && !player.isGM())
			{
				return;
			}
		}
		
		if (warehouse instanceof PcFreight && !player.getAccessLevel().allowTransaction())
		{
			player.sendMessage("Unsufficient privileges.");
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		// Alt game - Karma punishment
		if (!Config.ALT_GAME_KARMA_PLAYER_CAN_USE_WAREHOUSE && player.getKarma() > 0)
		{
			return;
		}
		
		// Freight price from config or normal price per item slot (30)
		final int fee = _slot * Config.ALT_GAME_FREIGHT_PRICE;
		int currentAdena = player.getAdena();
		int slots = 0;
		
		for (final Item i : _items)
		{
			final int objectId = i.id;
			final int count = i.count;
			
			// Check validity of requested item
			final L2ItemInstance item = player.checkItemManipulation(objectId, count, "deposit");
			
			// Check if item is null
			if (item == null)
			{
				LOG.warn("Error depositing a warehouse object for char " + player.getName() + " (validity check)");
				i.id = 0;
				i.count = 0;
				continue;
			}
			
			// Fix exploit for trade Augmented weapon with freight
			if (!Config.ALLOW_FREIGHT_AUGMENTED && item.isAugmented())
			{
				LOG.warn("Error depositing a warehouse object for char " + player.getName() + " (item is augmented)");
				return;
			}
			
			if (!Config.UNTRADABLE_FOR_WAREHOUSE && !item.isTradeable() || item.getItemType() == L2EtcItemType.QUEST)
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
		for (final Item i : _items)
		{
			final int objectId = i.id;
			final int count = i.count;
			
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
			
			final L2ItemInstance newItem = player.getInventory().transferItem("Warehouse", objectId, count, warehouse, player, player.getLastFolkNPC());
			
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
		
		player.setActiveWarehouse(null);
	}
	
	private class Item
	{
		public int id;
		public int count;
		
		public Item(final int i, final int c)
		{
			id = i;
			count = c;
		}
	}
	
	@Override
	public String getType()
	{
		return "[C] 9F RequestPackageSend";
	}
}
