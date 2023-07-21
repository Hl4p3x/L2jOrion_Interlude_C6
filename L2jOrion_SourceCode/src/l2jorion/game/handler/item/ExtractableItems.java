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
package l2jorion.game.handler.item;

import l2jorion.game.datatables.sql.ItemTable;
import l2jorion.game.datatables.xml.ExtractableItemsData;
import l2jorion.game.enums.AchType;
import l2jorion.game.handler.IItemHandler;
import l2jorion.game.model.L2ExtractableItem;
import l2jorion.game.model.L2ExtractableProductItem;
import l2jorion.game.model.actor.instance.L2ItemInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.actor.instance.L2PlayableInstance;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.InventoryUpdate;
import l2jorion.game.network.serverpackets.SystemMessage;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;
import l2jorion.util.random.Rnd;

public class ExtractableItems implements IItemHandler
{
	private static Logger LOG = LoggerFactory.getLogger(ItemTable.class);
	
	private static final int[] ITEM_IDS = ExtractableItemsData.getInstance().itemIDs();
	
	@Override
	public void useItem(L2PlayableInstance playable, L2ItemInstance item)
	{
		if (!(playable instanceof L2PcInstance))
		{
			return;
		}
		
		final L2PcInstance activeChar = (L2PcInstance) playable;
		final int itemId = item.getItemId();
		final L2ExtractableItem extractable = ExtractableItemsData.getInstance().getExtractableItem(itemId);
		if (extractable == null)
		{
			return;
		}
		
		// Destroy item first.
		activeChar.destroyItemByItemId("Extract", itemId, 1, activeChar.getTarget(), true);
		int createItemId = 0;
		int createAmount = 0;
		float chanceFrom = 0;
		final float random = Rnd.get(100);
		for (L2ExtractableProductItem expi : extractable.getProductItems())
		{
			final float chance = expi.getChance();
			if ((random >= chanceFrom) && (random <= (chance + chanceFrom)))
			{
				createItemId = expi.getId();
				createAmount = expi.getAmmount();
				break;
			}
			chanceFrom += chance;
		}
		
		if (createItemId == 0)
		{
			activeChar.sendMessage("Nothing happened.");
			return;
		}
		
		if (createItemId > 0)
		{
			if (ItemTable.getInstance().createDummyItem(createItemId) == null)
			{
				LOG.warn("createItemID " + createItemId + " doesn't have template!");
				activeChar.sendMessage("Nothing happened.");
				return;
			}
			
			if (ItemTable.getInstance().createDummyItem(createItemId).isStackable())
			{
				final int existingCount = activeChar.getInventory().getInventoryItemCount(createItemId, -1);
				final L2ItemInstance extractedItem = activeChar.getInventory().addItem("Extract", createItemId, createAmount, activeChar, item);
				
				// Send inventory update packet
				if (existingCount > 0)
				{
					final InventoryUpdate playerIU = new InventoryUpdate();
					playerIU.addModifiedItem(extractedItem);
					activeChar.sendPacket(playerIU);
				}
			}
			else
			{
				for (int i = 0; i < createAmount; i++)
				{
					activeChar.addItem("Extract", createItemId, 1, item, false);
				}
			}
			
			SystemMessage sm;
			if (createAmount > 1)
			{
				sm = new SystemMessage(SystemMessageId.EARNED_S2_S1_S);
				sm.addItemName(createItemId);
				sm.addNumber(createAmount);
			}
			else
			{
				sm = new SystemMessage(SystemMessageId.EARNED_ITEM);
				sm.addItemName(createItemId);
			}
			activeChar.sendPacket(sm);
			
			// Daily
			activeChar.getAchievement().increase(AchType.DAILY_OPEN_BOX, 1, true, true, true, itemId);
		}
		else
		{
			activeChar.sendMessage("Nothing happened.");
		}
	}
	
	@Override
	public int[] getItemIds()
	{
		return ITEM_IDS;
	}
}