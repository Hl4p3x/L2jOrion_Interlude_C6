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

import javolution.util.FastList;
import l2jorion.Config;
import l2jorion.game.model.ItemInfo;
import l2jorion.game.model.L2World;
import l2jorion.game.model.actor.instance.L2ItemInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.templates.L2Item;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;

public class InventoryUpdate extends L2GameServerPacket
{
	private static Logger LOG = LoggerFactory.getLogger(InventoryUpdate.class);
	
	private static final String _S__37_INVENTORYUPDATE = "[S] 27 InventoryUpdate";
	
	private final List<ItemInfo> _items;
	
	public InventoryUpdate()
	{
		_items = new FastList<>();
		if (Config.DEBUG)
		{
			showDebug();
		}
	}
	
	/**
	 * @param items
	 */
	public InventoryUpdate(final List<ItemInfo> items)
	{
		_items = items;
		if (Config.DEBUG)
		{
			showDebug();
		}
	}
	
	public void addItem(final L2ItemInstance item)
	{
		if (item != null)
		{
			_items.add(new ItemInfo(item));
		}
	}
	
	public void addNewItem(final L2ItemInstance item)
	{
		if (item != null)
		{
			_items.add(new ItemInfo(item, 1));
		}
	}
	
	public void addModifiedItem(final L2ItemInstance item)
	{
		if (item != null)
		{
			_items.add(new ItemInfo(item, 2));
		}
	}
	
	public void addRemovedItem(final L2ItemInstance item)
	{
		if (item != null)
		{
			_items.add(new ItemInfo(item, 3));
		}
	}
	
	public void addItems(final List<L2ItemInstance> items)
	{
		if (items != null)
		{
			for (final L2ItemInstance item : items)
			{
				if (item != null)
				{
					_items.add(new ItemInfo(item));
				}
			}
		}
	}
	
	private void showDebug()
	{
		for (final ItemInfo item : _items)
		{
			LOG.debug("oid:" + Integer.toHexString(item.getObjectId()) + " item:" + item.getItem().getName() + " last change:" + item.getChange());
		}
	}
	
	private static boolean isBodypart(L2Item item)
	{
		if (item.getBodyPart() == L2Item.SLOT_HEAD || item.getBodyPart() == L2Item.SLOT_FULL_ARMOR || item.getBodyPart() == L2Item.SLOT_CHEST || item.getBodyPart() == L2Item.SLOT_LEGS || item.getBodyPart() == L2Item.SLOT_GLOVES || item.getBodyPart() == L2Item.SLOT_FEET)
		{
			return true;
		}
		
		return false;
	}
	
	@Override
	protected final void writeImpl()
	{
		writeC(0x27);
		final int count = _items.size();
		writeH(count);
		for (final ItemInfo item : _items)
		{
			L2PcInstance activeChar = L2World.getInstance().getPlayer(item.getOwnerId());
			
			writeH(item.getChange()); // Update type : 01-add, 02-modify,
			writeH(item.getItem().getType1()); // Item Type 1 :
			writeD(item.getObjectId()); // ObjectId
			writeD(item.getItem().getItemId()); // ItemId
			writeD(item.getCount()); // Quantity
			writeH(item.getItem().getType2()); // Item Type 2 : 00-weapon,
			writeH(item.getCustomType1()); // Filler (always 0)
			
			// writeH(item.getEquipped()); // Equipped : 00-No, 01-yes
			// writeD(item.getItem().getBodyPart()); // Slot : 0006-lr.ear,
			
			if (activeChar != null)
			{
				if (activeChar.getFakeArmorObjectId() > 0)
				{
					if (item.getObjectId() == activeChar.getFakeArmorObjectId())
					{
						writeH(0x01);
					}
					else
					{
						writeH(item.getEquipped());
					}
					
					if (item.getObjectId() == activeChar.getFakeArmorObjectId())
					{
						writeD(item.getItem().isFakeArmor() ? L2Item.SLOT_ALLDRESS : item.getItem().getBodyPart());
					}
					else if (isBodypart(item.getItem()) && item.getEquipped() == 1 && activeChar.getFakeArmorObjectId() > 0)
					{
						writeD(99);
					}
					else
					{
						writeD(item.getItem().getBodyPart());
					}
				}
				else
				{
					writeH(item.getEquipped());
					writeD(item.getItem().getBodyPart());
				}
			}
			else
			{
				writeH(item.getEquipped());
				writeD(item.getItem().getBodyPart());
			}
			
			writeH(item.getEnchant()); // Enchant level (pet level shown in
			writeH(item.getCustomType2()); // Pet name exists or not shown
			writeD(item.getAugemtationBoni());
			writeD(item.getMana());
		}
	}
	
	@Override
	public String getType()
	{
		return _S__37_INVENTORYUPDATE;
	}
}