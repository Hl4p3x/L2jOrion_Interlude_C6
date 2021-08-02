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
package l2jorion.game.model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

import javolution.util.FastList;
import l2jorion.game.model.TradeList.TradeItem;
import l2jorion.game.model.actor.instance.L2ItemInstance;
import l2jorion.game.model.actor.instance.L2ItemInstance.ItemLocation;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.templates.L2EtcItemType;
import l2jorion.game.templates.L2Item;
import l2jorion.util.CloseUtil;
import l2jorion.util.database.L2DatabaseFactory;

public class PcInventory extends Inventory
{
	public static final int ADENA_ID = 57;
	public static final int LA_ID = 6673;
	public static final int ANCIENT_ADENA_ID = 5575;
	
	private final L2PcInstance _owner;
	private L2ItemInstance _adena;
	private L2ItemInstance _ancientAdena;
	private int _questSlots;
	
	public PcInventory(final L2PcInstance owner)
	{
		_owner = owner;
	}
	
	@Override
	public L2PcInstance getOwner()
	{
		return _owner;
	}
	
	@Override
	protected ItemLocation getBaseLocation()
	{
		return ItemLocation.INVENTORY;
	}
	
	@Override
	protected ItemLocation getEquipLocation()
	{
		return ItemLocation.PAPERDOLL;
	}
	
	public L2ItemInstance getAdenaInstance()
	{
		return _adena;
	}
	
	@Override
	public int getAdena()
	{
		return _adena != null ? _adena.getCount() : 0;
	}
	
	public L2ItemInstance getAncientAdenaInstance()
	{
		return _ancientAdena;
	}
	
	public int getAncientAdena()
	{
		return _ancientAdena != null ? _ancientAdena.getCount() : 0;
	}
	
	/**
	 * Returns the list of items in inventory available for transaction
	 * @param allowAdena
	 * @param allowAncientAdena
	 * @param allowEquipped
	 * @return L2ItemInstance : items in inventory
	 */
	public L2ItemInstance[] getUniqueItems(final boolean allowAdena, final boolean allowAncientAdena, final boolean allowEquipped)
	{
		return getUniqueItems(allowAdena, allowAncientAdena, true, allowEquipped);
	}
	
	public L2ItemInstance[] getUniqueItems(final boolean allowAdena, final boolean allowAncientAdena, final boolean onlyAvailable, final boolean allowEquipped)
	{
		final List<L2ItemInstance> list = new FastList<>();
		
		for (final L2ItemInstance item : _items)
		{
			if (!allowAdena && item.getItemId() == 57)
			{
				continue;
			}
			if (!allowAncientAdena && item.getItemId() == 5575)
			{
				continue;
			}
			
			boolean isDuplicate = false;
			
			for (final L2ItemInstance litem : list)
			{
				if (litem.getItemId() == item.getItemId() && item.isStackable()) // to duplicate more not stackable item
				{
					isDuplicate = true;
					break;
				}
			}
			if (!isDuplicate && (!onlyAvailable || item.getItem().isSellable() && item.isAvailable(getOwner(), false, allowEquipped)))
			{
				list.add(item);
			}
		}
		
		return list.toArray(new L2ItemInstance[list.size()]);
	}
	
	/**
	 * Returns the list of items in inventory available for transaction Allows an item to appear twice if and only if there is a difference in enchantment level.
	 * @param allowAdena
	 * @param allowAncientAdena
	 * @param allowEquipped
	 * @return L2ItemInstance : items in inventory
	 */
	public L2ItemInstance[] getUniqueItemsByEnchantLevel(final boolean allowAdena, final boolean allowAncientAdena, final boolean allowEquipped)
	{
		return getUniqueItemsByEnchantLevel(allowAdena, allowAncientAdena, true, allowEquipped);
	}
	
	public L2ItemInstance[] getUniqueItemsByEnchantLevel(final boolean allowAdena, final boolean allowAncientAdena, final boolean onlyAvailable, final boolean allowEquipped)
	{
		final List<L2ItemInstance> list = new FastList<>();
		
		for (final L2ItemInstance item : _items)
		{
			if (!allowAdena && item.getItemId() == 57)
			{
				continue;
			}
			if (!allowAncientAdena && item.getItemId() == 5575)
			{
				continue;
			}
			
			boolean isDuplicate = false;
			
			for (final L2ItemInstance litem : list)
			{
				if (litem.getItemId() == item.getItemId() && litem.getEnchantLevel() == item.getEnchantLevel())
				{
					isDuplicate = true;
					break;
				}
			}
			
			if (!isDuplicate && (!onlyAvailable || item.getItem().isSellable() && item.isAvailable(getOwner(), false, allowEquipped)))
			{
				list.add(item);
			}
		}
		
		return list.toArray(new L2ItemInstance[list.size()]);
	}
	
	/**
	 * Returns the list of all items in inventory that have a given item id.
	 * @param itemId
	 * @return L2ItemInstance[] : matching items from inventory
	 */
	public L2ItemInstance[] getAllItemsByItemId(final int itemId)
	{
		final List<L2ItemInstance> list = new FastList<>();
		
		for (final L2ItemInstance item : _items)
		{
			if (item.getItemId() == itemId)
			{
				list.add(item);
			}
		}
		
		return list.toArray(new L2ItemInstance[list.size()]);
	}
	
	/**
	 * Returns the list of all items in inventory that have a given item id AND a given enchantment level.
	 * @param itemId
	 * @param enchantment
	 * @return L2ItemInstance[] : matching items from inventory
	 */
	public L2ItemInstance[] getAllItemsByItemId(final int itemId, final int enchantment)
	{
		final List<L2ItemInstance> list = new FastList<>();
		
		for (final L2ItemInstance item : _items)
		{
			if (item.getItemId() == itemId && item.getEnchantLevel() == enchantment)
			{
				list.add(item);
			}
		}
		
		return list.toArray(new L2ItemInstance[list.size()]);
	}
	
	/**
	 * Returns the list of items in inventory available for transaction
	 * @param allowAdena
	 * @return L2ItemInstance : items in inventory
	 */
	public L2ItemInstance[] getAvailableItems(final boolean allowAdena)
	{
		final List<L2ItemInstance> list = new FastList<>();
		
		for (final L2ItemInstance item : _items)
		{
			if (item != null && item.isAvailable(getOwner(), allowAdena, false))
			{
				list.add(item);
			}
		}
		
		return list.toArray(new L2ItemInstance[list.size()]);
	}
	
	public L2ItemInstance[] getAvailableItemsForPackage(final boolean allowAdena)
	{
		final List<L2ItemInstance> list = new FastList<>();
		
		for (final L2ItemInstance item : _items)
		{
			if (item != null && item.isAvailableItemForPackage(getOwner(), allowAdena, false))
			{
				list.add(item);
			}
		}
		
		return list.toArray(new L2ItemInstance[list.size()]);
	}
	
	/**
	 * Get all augmented items
	 * @return
	 */
	public L2ItemInstance[] getAugmentedItems()
	{
		final List<L2ItemInstance> list = new FastList<>();
		
		for (final L2ItemInstance item : _items)
		{
			if (item != null && item.isAugmented())
			{
				list.add(item);
			}
		}
		
		return list.toArray(new L2ItemInstance[list.size()]);
	}
	
	/**
	 * Returns the list of items in inventory available for transaction adjusted by tradeList
	 * @param tradeList
	 * @return L2ItemInstance : items in inventory
	 */
	public TradeList.TradeItem[] getAvailableItems(final TradeList tradeList)
	{
		final List<TradeList.TradeItem> list = new FastList<>();
		
		for (final L2ItemInstance item : _items)
		{
			if (item != null && item.isAvailable(getOwner(), false, false))
			{
				if (_owner.getFakeArmorObjectId() == item.getObjectId())
				{
					continue;
				}
				
				final TradeList.TradeItem adjItem = tradeList.adjustAvailableItem(item);
				if (adjItem != null)
				{
					list.add(adjItem);
				}
			}
		}
		
		return list.toArray(new TradeList.TradeItem[list.size()]);
	}
	
	/**
	 * Adjust TradeItem according his status in inventory
	 * @param item : L2ItemInstance to be adjusted
	 * @param list
	 * @return
	 */
	public TradeItem adjustAvailableItem(final TradeItem item, final List<TradeList.TradeItem> list)
	{
		for (final L2ItemInstance adjItem : _items)
		{
			if (adjItem.isStackable())
			{
				
				if (adjItem.getItemId() == item.getItem().getItemId() && (adjItem.getEnchantLevel() == item.getEnchant()))
				{
					item.setObjectId(adjItem.getObjectId());
					
					if (adjItem.getCount() < item.getCount())
					{
						item.setCurCount(adjItem.getCount());
					}
					else
					{
						item.setCurCount(item.getCount());
					}
					
					return item;
				}
				
			}
			else
			{
				
				if (adjItem.getItemId() == item.getItem().getItemId() && (adjItem.getEnchantLevel() == item.getEnchant()))
				{
					boolean found = false;
					for (final TradeList.TradeItem actual : list)
					{
						
						if (actual.getObjectId() == adjItem.getObjectId())
						{
							found = true;
							break;
						}
						
					}
					
					if (found)
					{
						continue;
					}
					
					item.setObjectId(adjItem.getObjectId());
					if (adjItem.getCount() < item.getCount())
					{
						item.setCurCount(adjItem.getCount());
					}
					else
					{
						item.setCurCount(item.getCount());
					}
					return item;
				}
			}
		}
		item.setCurCount(0);
		return item;
	}
	
	/**
	 * Adds adena to PCInventory
	 * @param process : String Identifier of process triggering this action
	 * @param count : int Quantity of adena to be added
	 * @param actor : L2PcInstance Player requesting the item add
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 */
	public void addAdena(final String process, final int count, final L2PcInstance actor, final L2Object reference)
	{
		if (count > 0)
		{
			addItem(process, ADENA_ID, count, actor, reference);
		}
	}
	
	public void addMedal(final String process, final int count, final L2PcInstance actor, final L2Object reference)
	{
		if (count > 0)
		{
			addItem(process, 6393, count, actor, reference);
		}
	}
	
	public void addLA(final String process, final int count, final L2PcInstance actor, final L2Object reference)
	{
		if (count > 0)
		{
			addItem(process, LA_ID, count, actor, reference);
		}
	}
	
	/**
	 * Removes adena to PCInventory
	 * @param process : String Identifier of process triggering this action
	 * @param count : int Quantity of adena to be removed
	 * @param actor : L2PcInstance Player requesting the item add
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 */
	public void reduceAdena(final String process, final int count, final L2PcInstance actor, final L2Object reference)
	{
		if (count > 0)
		{
			destroyItemByItemId(process, ADENA_ID, count, actor, reference);
		}
	}
	
	/**
	 * Adds specified amount of ancient adena to player inventory.
	 * @param process : String Identifier of process triggering this action
	 * @param count : int Quantity of adena to be added
	 * @param actor : L2PcInstance Player requesting the item add
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 */
	public void addAncientAdena(final String process, final int count, final L2PcInstance actor, final L2Object reference)
	{
		if (count > 0)
		{
			addItem(process, ANCIENT_ADENA_ID, count, actor, reference);
		}
	}
	
	/**
	 * Removes specified amount of ancient adena from player inventory.
	 * @param process : String Identifier of process triggering this action
	 * @param count : int Quantity of adena to be removed
	 * @param actor : L2PcInstance Player requesting the item add
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 */
	public void reduceAncientAdena(final String process, final int count, final L2PcInstance actor, final L2Object reference)
	{
		if (count > 0)
		{
			destroyItemByItemId(process, ANCIENT_ADENA_ID, count, actor, reference);
		}
	}
	
	/**
	 * Adds item in inventory and checks _adena and _ancientAdena
	 * @param process : String Identifier of process triggering this action
	 * @param item : L2ItemInstance to be added
	 * @param actor : L2PcInstance Player requesting the item add
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @return L2ItemInstance corresponding to the new item or the updated item in inventory
	 */
	@Override
	public L2ItemInstance addItem(final String process, L2ItemInstance item, final L2PcInstance actor, final L2Object reference)
	{
		item = super.addItem(process, item, actor, reference);
		
		if (item != null && item.getItemId() == ADENA_ID && !item.equals(_adena))
		{
			_adena = item;
		}
		
		if (item != null && item.getItemId() == ANCIENT_ADENA_ID && !item.equals(_ancientAdena))
		{
			_ancientAdena = item;
		}
		
		return item;
	}
	
	@Override
	public L2ItemInstance addItem(final String process, final int itemId, final int count, final L2PcInstance actor, final L2Object reference)
	{
		final L2ItemInstance item = super.addItem(process, itemId, count, actor, reference);
		
		if (item != null && item.getItemId() == ADENA_ID && !item.equals(_adena))
		{
			_adena = item;
		}
		
		if (item != null && item.getItemId() == ANCIENT_ADENA_ID && !item.equals(_ancientAdena))
		{
			_ancientAdena = item;
		}
		
		return item;
	}
	
	@Override
	public void addItem(L2ItemInstance item)
	{
		synchronized (_items)
		{
			if (item.isQuestItem())
			{
				_questSlots++;
			}
			
			super.addItem(item);
		}
	}
	
	public int getSize(boolean quest)
	{
		if (quest)
		{
			return _questSlots;
		}
		
		return getSize() - _questSlots;
	}
	
	/**
	 * Transfers item to another inventory and checks _adena and _ancientAdena
	 * @param process : String Identifier of process triggering this action
	 * @param objectId : int Item Identifier of the item to be transfered
	 * @param count : int Quantity of items to be transfered
	 * @param actor : L2PcInstance Player requesting the item transfer
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @return L2ItemInstance corresponding to the new item or the updated item in inventory
	 */
	@Override
	public L2ItemInstance transferItem(final String process, final int objectId, final int count, final ItemContainer target, final L2PcInstance actor, final L2Object reference)
	{
		final L2ItemInstance item = super.transferItem(process, objectId, count, target, actor, reference);
		
		if (_adena != null && (_adena.getCount() <= 0 || _adena.getOwnerId() != getOwnerId()))
		{
			_adena = null;
		}
		
		if (_ancientAdena != null && (_ancientAdena.getCount() <= 0 || _ancientAdena.getOwnerId() != getOwnerId()))
		{
			_ancientAdena = null;
		}
		
		return item;
	}
	
	/**
	 * Destroy item from inventory and checks _adena and _ancientAdena
	 * @param process : String Identifier of process triggering this action
	 * @param item : L2ItemInstance to be destroyed
	 * @param actor : L2PcInstance Player requesting the item destroy
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @return L2ItemInstance corresponding to the destroyed item or the updated item in inventory
	 */
	@Override
	public L2ItemInstance destroyItem(final String process, L2ItemInstance item, final L2PcInstance actor, final L2Object reference)
	{
		item = super.destroyItem(process, item, actor, reference);
		
		if (_adena != null && _adena.getCount() <= 0)
		{
			_adena = null;
		}
		
		if (_ancientAdena != null && _ancientAdena.getCount() <= 0)
		{
			_ancientAdena = null;
		}
		
		return item;
	}
	
	/**
	 * Destroy item from inventory and checks _adena and _ancientAdena
	 * @param process : String Identifier of process triggering this action
	 * @param item : L2ItemInstance to be destroyed
	 * @param actor : L2PcInstance Player requesting the item destroy
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @return L2ItemInstance corresponding to the destroyed item or the updated item in inventory
	 */
	@Override
	public L2ItemInstance destroyItem(String process, L2ItemInstance item, int count, L2PcInstance actor, L2Object reference)
	{
		item = super.destroyItem(process, item, count, actor, reference);
		
		if ((_adena != null) && (_adena.getCount() <= 0))
		{
			_adena = null;
		}
		
		if ((_ancientAdena != null) && (_ancientAdena.getCount() <= 0))
		{
			_ancientAdena = null;
		}
		
		return item;
	}
	
	/**
	 * Destroys item from inventory and checks _adena and _ancientAdena
	 * @param process : String Identifier of process triggering this action
	 * @param objectId : int Item Instance identifier of the item to be destroyed
	 * @param count : int Quantity of items to be destroyed
	 * @param actor : L2PcInstance Player requesting the item destroy
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @return L2ItemInstance corresponding to the destroyed item or the updated item in inventory
	 */
	@Override
	public L2ItemInstance destroyItem(final String process, final int objectId, final int count, final L2PcInstance actor, final L2Object reference)
	{
		final L2ItemInstance item = super.destroyItem(process, objectId, count, actor, reference);
		
		if (_adena != null && _adena.getCount() <= 0)
		{
			_adena = null;
		}
		
		if (_ancientAdena != null && _ancientAdena.getCount() <= 0)
		{
			_ancientAdena = null;
		}
		
		return item;
	}
	
	/**
	 * Destroy item from inventory by using its <B>itemId</B> and checks _adena and _ancientAdena
	 * @param process : String Identifier of process triggering this action
	 * @param itemId : int Item identifier of the item to be destroyed
	 * @param count : int Quantity of items to be destroyed
	 * @param actor : L2PcInstance Player requesting the item destroy
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @return L2ItemInstance corresponding to the destroyed item or the updated item in inventory
	 */
	@Override
	public L2ItemInstance destroyItemByItemId(final String process, final int itemId, final int count, final L2PcInstance actor, final L2Object reference)
	{
		final L2ItemInstance item = super.destroyItemByItemId(process, itemId, count, actor, reference);
		
		if (_adena != null && _adena.getCount() <= 0)
		{
			_adena = null;
		}
		
		if (_ancientAdena != null && _ancientAdena.getCount() <= 0)
		{
			_ancientAdena = null;
		}
		
		return item;
	}
	
	/**
	 * Drop item from inventory and checks _adena and _ancientAdena
	 * @param process : String Identifier of process triggering this action
	 * @param item : L2ItemInstance to be dropped
	 * @param actor : L2PcInstance Player requesting the item drop
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @return L2ItemInstance corresponding to the destroyed item or the updated item in inventory
	 */
	@Override
	public L2ItemInstance dropItem(final String process, L2ItemInstance item, final L2PcInstance actor, final L2Object reference)
	{
		item = super.dropItem(process, item, actor, reference);
		
		if (_adena != null && (_adena.getCount() <= 0 || _adena.getOwnerId() != getOwnerId()))
		{
			_adena = null;
		}
		
		if (_ancientAdena != null && (_ancientAdena.getCount() <= 0 || _ancientAdena.getOwnerId() != getOwnerId()))
		{
			_ancientAdena = null;
		}
		
		return item;
	}
	
	/**
	 * Drop item from inventory by using its <B>objectID</B> and checks _adena and _ancientAdena
	 * @param process : String Identifier of process triggering this action
	 * @param objectId : int Item Instance identifier of the item to be dropped
	 * @param count : int Quantity of items to be dropped
	 * @param actor : L2PcInstance Player requesting the item drop
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @return L2ItemInstance corresponding to the destroyed item or the updated item in inventory
	 */
	@Override
	public L2ItemInstance dropItem(final String process, final int objectId, final int count, final L2PcInstance actor, final L2Object reference)
	{
		final L2ItemInstance item = super.dropItem(process, objectId, count, actor, reference);
		
		if (_adena != null && (_adena.getCount() <= 0 || _adena.getOwnerId() != getOwnerId()))
		{
			_adena = null;
		}
		
		if (_ancientAdena != null && (_ancientAdena.getCount() <= 0 || _ancientAdena.getOwnerId() != getOwnerId()))
		{
			_ancientAdena = null;
		}
		
		return item;
	}
	
	/**
	 * <b>Overloaded</b>, when removes item from inventory, remove also owner shortcuts.
	 * @param item : L2ItemInstance to be removed from inventory
	 */
	@Override
	protected void removeItem(final L2ItemInstance item)
	{
		// Removes any reference to the item from Shortcut bar
		getOwner().removeItemFromShortCut(item.getObjectId());
		
		// Removes active Enchant Scroll
		if (item.equals(getOwner().getActiveEnchantItem()))
		{
			getOwner().setActiveEnchantItem(null);
		}
		
		if (item.getItemId() == ADENA_ID)
		{
			_adena = null;
		}
		else if (item.getItemId() == ANCIENT_ADENA_ID)
		{
			_ancientAdena = null;
		}
		
		super.removeItem(item);
	}
	
	/**
	 * Refresh the weight of equipment loaded
	 */
	@Override
	public void refreshWeight()
	{
		super.refreshWeight();
		getOwner().refreshOverloaded();
	}
	
	/**
	 * Get back items in inventory from database
	 */
	@Override
	public void restore()
	{
		super.restore();
		_adena = getItemByItemId(ADENA_ID);
		_ancientAdena = getItemByItemId(ANCIENT_ADENA_ID);
	}
	
	public static int[][] restoreVisibleInventory(final int objectId)
	{
		final int[][] paperdoll = new int[0x12][3];
		Connection con = null;
		
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement2 = con.prepareStatement("SELECT object_id,item_id,loc_data,enchant_level FROM items WHERE owner_id=? AND loc='PAPERDOLL'");
			statement2.setInt(1, objectId);
			ResultSet invdata = statement2.executeQuery();
			
			while (invdata.next())
			{
				final int slot = invdata.getInt("loc_data");
				paperdoll[slot][0] = invdata.getInt("object_id");
				paperdoll[slot][1] = invdata.getInt("item_id");
				paperdoll[slot][2] = invdata.getInt("enchant_level");
			}
			
			invdata.close();
			statement2.close();
			invdata = null;
			statement2 = null;
		}
		catch (final Exception e)
		{
			LOG.warn("could not restore inventory:");
			e.printStackTrace();
		}
		finally
		{
			CloseUtil.close(con);
			con = null;
		}
		return paperdoll;
	}
	
	public boolean validateCapacity(final L2ItemInstance item)
	{
		int slots = 0;
		
		if (!(item.isStackable() && getItemByItemId(item.getItemId()) != null) && item.getItemType() != L2EtcItemType.HERB)
		{
			slots++;
		}
		
		return validateCapacity(slots);
	}
	
	public boolean validateCapacity(final List<L2ItemInstance> items)
	{
		int slots = 0;
		
		for (final L2ItemInstance item : items)
		{
			if (!(item.isStackable() && getItemByItemId(item.getItemId()) != null))
			{
				slots++;
			}
		}
		
		return validateCapacity(slots);
	}
	
	public boolean validateCapacityByItemId(final int ItemId)
	{
		int slots = 0;
		
		L2ItemInstance invItem = getItemByItemId(ItemId);
		
		if (!(invItem != null && invItem.isStackable()))
		{
			slots++;
		}
		
		invItem = null;
		
		return validateCapacity(slots);
	}
	
	@Override
	public boolean validateCapacity(int slots)
	{
		return validateCapacity(slots, false);
	}
	
	public boolean validateCapacity(int slots, boolean questItem)
	{
		if (!questItem)
		{
			return (((_items.size() - _questSlots) + slots) <= _owner.getInventoryLimit());
		}
		
		return (_questSlots + slots) <= L2PcInstance.getQuestInventoryLimit();
	}
	
	@Override
	public boolean validateWeight(final int weight)
	{
		return _totalWeight + weight <= _owner.getMaxLoad();
	}
	
	public boolean validateCapacity(final L2Item item)
	{
		
		int slots = 0;
		
		if (!(item.isStackable() && getItemByItemId(item.getItemId()) != null) && item.getItemType() != L2EtcItemType.HERB)
		{
			slots++;
		}
		
		return validateCapacity(slots);
	}
	
	public boolean checkIfEquipped(final int item_id)
	{
		final L2ItemInstance[] items = getAllItemsByItemId(item_id);
		
		if (items == null || items.length == 0)
		{
			return false;
		}
		
		for (final L2ItemInstance item : items)
		{
			
			if (item.isEquipped())
			{
				return true;
			}
			
		}
		
		return false;
	}
	
	public int checkHowManyEquipped(final int item_id)
	{
		
		final L2ItemInstance[] items = getAllItemsByItemId(item_id);
		
		if (items == null || items.length == 0)
		{
			return 0;
		}
		
		int count = 0;
		for (final L2ItemInstance item : items)
		{
			if (item.isEquipped())
			{
				count++;
			}
		}
		
		return count;
	}
}