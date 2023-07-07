/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package l2jorion.game.taskmanager.tasks;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import l2jorion.game.datatables.sql.ItemTable;
import l2jorion.game.idfactory.IdFactory;
import l2jorion.game.model.L2World;
import l2jorion.game.model.actor.instance.L2ItemInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.ExShowScreenMessage;
import l2jorion.game.network.serverpackets.ItemList;
import l2jorion.game.network.serverpackets.PlaySound;
import l2jorion.game.network.serverpackets.SystemMessage;
import l2jorion.game.templates.L2Item;
import l2jorion.game.thread.ThreadPoolManager;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;
import l2jorion.util.CloseUtil;
import l2jorion.util.database.DatabaseUtils;
import l2jorion.util.database.L2DatabaseFactory;

public final class TaskItemDonate implements Runnable
{
	private static final Logger LOG = LoggerFactory.getLogger(TaskItemDonate.class);
	
	public TaskItemDonate()
	{
		ThreadPoolManager.getInstance().scheduleEffectAtFixedRate(this, 1000, 3000);
	}
	
	@Override
	public final void run()
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			Statement st = con.createStatement();
			ResultSet rset = st.executeQuery("SELECT * FROM items_donate");
			
			while (rset.next())
			{
				int paymentId = rset.getInt("id");
				int ownerId = rset.getInt("owner_id");
				
				L2PcInstance player = L2World.getInstance().getPlayer(ownerId);
				if (player != null)
				{
					int itemId = rset.getInt("item_id");
					int itemCount = rset.getInt("count");
					int itemEnchant = rset.getInt("enchant_level");
					
					createItem(player, itemId, itemCount, itemEnchant);
					
					DatabaseUtils.set("DELETE FROM items_donate WHERE id=?", paymentId);
				}
			}
			
			rset.close();
			st.close();
		}
		catch (SQLException e)
		{
			LOG.warn(getClass().getSimpleName() + ": Couldn't get donate item.");
		}
		finally
		{
			CloseUtil.close(con);
		}
	}
	
	private static void createItem(L2PcInstance target, int itemId, int itemCount, int enchantLevel)
	{
		final L2Item template = ItemTable.getInstance().getTemplate(itemId);
		
		if (template == null)
		{
			target.sendMessage("This item doesn't exist.");
			return;
		}
		
		L2ItemInstance item = new L2ItemInstance(IdFactory.getInstance().getNextId(), itemId);
		L2World.getInstance().storeObject(item);
		item.setCount(itemCount);
		item.setEnchantLevel(enchantLevel);
		
		target.getInventory().addItem("DonateItem", item, target, null);
		
		target.sendMessage("Donation items transferred to your character.. Thank you, " + target.getName() + "!");
		target.sendPacket(new ExShowScreenMessage("Donation items transferred to your character. Thank you, " + target.getName() + "!", 2000, 2, false));
		target.sendPacket(new PlaySound("ItemSound3.sys_exchange_success"));
		
		if (enchantLevel > 0)
		{
			target.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.EARNED_ITEM).addNumber(enchantLevel).addItemName(itemId));
		}
		else
		{
			target.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.EARNED_S2_S1_S).addItemName(itemId).addNumber(itemCount));
		}
		
		target.sendPacket(new ItemList(target, true));
	}
	
	public static TaskItemDonate getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected final static TaskItemDonate _instance = new TaskItemDonate();
	}
}