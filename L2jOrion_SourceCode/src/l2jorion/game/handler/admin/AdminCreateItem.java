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
package l2jorion.game.handler.admin;

import java.util.StringTokenizer;

import l2jorion.game.datatables.sql.ItemTable;
import l2jorion.game.handler.IAdminCommandHandler;
import l2jorion.game.model.L2World;
import l2jorion.game.model.actor.instance.L2ItemInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.InventoryUpdate;
import l2jorion.game.network.serverpackets.ItemList;
import l2jorion.game.network.serverpackets.SystemMessage;
import l2jorion.game.templates.L2Item;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;

public class AdminCreateItem implements IAdminCommandHandler
{
	private static Logger LOG = LoggerFactory.getLogger(AdminCreateItem.class);
	
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_itemcreate",
		"admin_create_item",
		"admin_mass_create",
		"admin_clear_inventory"
	};
	
	private enum CommandEnum
	{
		admin_itemcreate,
		admin_create_item,
		admin_mass_create,
		admin_clear_inventory
	}
	
	@Override
	public boolean useAdminCommand(String command, L2PcInstance activeChar)
	{
		StringTokenizer st = new StringTokenizer(command);
		
		CommandEnum comm = CommandEnum.valueOf(st.nextToken());
		
		if (comm == null)
		{
			return false;
		}
		
		switch (comm)
		{
			case admin_itemcreate:
				
				AdminHelpPage.showHelpPage(activeChar, "itemcreation.htm");
				return true;
			
			case admin_create_item:
				
				if (st.hasMoreTokens())
				{
					if (st.countTokens() == 2)
					{
						String id = st.nextToken();
						String num = st.nextToken();
						
						int idval = 0;
						int numval = 0;
						
						try
						{
							idval = Integer.parseInt(id);
							numval = Integer.parseInt(num);
						}
						catch (NumberFormatException e)
						{
							
							activeChar.sendMessage("Usage: //itemcreate <itemId> (number value > 0) [amount] (number value > 0)");
							return false;
						}
						
						if (idval > 0 && numval > 0)
						{
							createItem(activeChar, idval, numval);
							return true;
						}
						activeChar.sendMessage("Usage: //itemcreate <itemId> (number value > 0) [amount] (number value > 0)");
						return false;
					}
					else if (st.countTokens() == 1)
					{
						String id = st.nextToken();
						int idval = 0;
						
						try
						{
							idval = Integer.parseInt(id);
							
						}
						catch (NumberFormatException e)
						{
							
							activeChar.sendMessage("Usage: //itemcreate <itemId> (number value > 0) [amount] (number value > 0)");
							return false;
						}
						
						if (idval > 0)
						{
							createItem(activeChar, idval, 1);
							return true;
						}
						activeChar.sendMessage("Usage: //itemcreate <itemId> (number value > 0) [amount] (number value > 0)");
						return false;
					}
				}
				else
				{
					AdminHelpPage.showHelpPage(activeChar, "itemcreation.htm");
					activeChar.sendMessage("Usage: //itemcreate <itemId> [amount]");
					return true;
				}
				AdminHelpPage.showHelpPage(activeChar, "itemcreation.htm");
				return true;
			case admin_mass_create:
				
				if (st.hasMoreTokens())
				{
					if (st.countTokens() == 2)
					{
						String id = st.nextToken();
						String num = st.nextToken();
						
						int idval = 0;
						int numval = 0;
						
						try
						{
							idval = Integer.parseInt(id);
							numval = Integer.parseInt(num);
						}
						catch (NumberFormatException e)
						{
							activeChar.sendMessage("Usage: //mass_create <itemId> <amount>");
							return false;
						}
						
						if (idval > 0 && numval > 0)
						{
							massCreateItem(activeChar, idval, numval);
							return true;
						}
						activeChar.sendMessage("Usage: //mass_create <itemId> <amount>");
						return false;
					}
					else if (st.countTokens() == 1)
					{
						String id = st.nextToken();
						int idval = 0;
						
						try
						{
							idval = Integer.parseInt(id);
							
						}
						catch (NumberFormatException e)
						{
							activeChar.sendMessage("Usage: //mass_create <itemId> <amount>");
							return false;
						}
						
						if (idval > 0)
						{
							massCreateItem(activeChar, idval, 1);
							return true;
						}
						activeChar.sendMessage("Usage: //mass_create <itemId> <amount>");
						return false;
					}
				}
				AdminHelpPage.showHelpPage(activeChar, "itemcreation.htm");
				return true;
			
			case admin_clear_inventory:
				removeAllItems(activeChar);
				return true;
			
			default:
				return false;
		}
		
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
	
	private void createItem(L2PcInstance activeChar, int id, int num)
	{
		if (num > 20)
		{
			L2Item template = ItemTable.getInstance().getTemplate(id);
			
			if (template != null && !template.isStackable())
			{
				activeChar.sendMessage("This item does not stack - creation aborted.");
				AdminHelpPage.showHelpPage(activeChar, "itemcreation.htm");
				return;
			}
		}
		
		L2PcInstance Player = null;
		
		if (activeChar.getTarget() != null)
		{
			if (activeChar.getTarget() instanceof L2PcInstance)
			{
				if (activeChar.getAccessLevel().getLevel() > 0 && activeChar.getAccessLevel().getLevel() < 5)
				{
					Player = (L2PcInstance) activeChar.getTarget();
				}
				else
				{
					activeChar.sendMessage("You have not right to create item on another player");
					AdminHelpPage.showHelpPage(activeChar, "itemcreation.htm");
					return;
				}
			}
			else
			{
				activeChar.sendMessage("You can add an item only to a character.");
				AdminHelpPage.showHelpPage(activeChar, "itemcreation.htm");
				return;
			}
		}
		
		if (Player == null)
		{
			activeChar.setTarget(activeChar);
			Player = activeChar;
		}
		
		Player.getInventory().addItem("Admin", id, num, Player, null);
		
		ItemList il = new ItemList(Player, true);
		Player.sendPacket(il);
		
		if (activeChar.getName().equalsIgnoreCase(Player.getName()))
		{
			activeChar.sendMessage("You have spawned: " + L2Item.getItemNameById(id) + " (" + id + ") " + num + " item(s)  in your inventory.");
		}
		else
		{
			activeChar.sendMessage("You have spawned: " + L2Item.getItemNameById(id) + " (" + id + ") " + num + " item(s)  in your inventory.");
			Player.sendMessage("Admin has spawned: " + L2Item.getItemNameById(id) + "  " + num + " item(s)  in your inventory.");
		}
		AdminHelpPage.showHelpPage(activeChar, "itemcreation.htm");
	}
	
	private void massCreateItem(L2PcInstance activeChar, int id, int num)
	{
		if (num > 20)
		{
			L2Item template = ItemTable.getInstance().getTemplate(id);
			if (template != null && !template.isStackable())
			{
				activeChar.sendMessage("This item does not stack - Creation aborted.");
				AdminHelpPage.showHelpPage(activeChar, "itemcreation.htm");
				return;
			}
		}
		
		int i = 0;
		L2ItemInstance item = null;
		for (L2PcInstance player : L2World.getInstance().getAllPlayers().values())
		{
			player.sendMessage("Admin is rewarding all online players.");
			player.sendMessage("Admin rewarded you: " + L2Item.getItemNameById(id) + " " + num + " item(s)  in your inventory.");
			
			item = player.getInventory().addItem("Admin", id, num, null, null);
			
			InventoryUpdate iu = new InventoryUpdate();
			iu.addItem(item);
			player.sendPacket(iu);
			
			SystemMessage sm = new SystemMessage(SystemMessageId.YOU_PICKED_UP_S1_S2);
			sm.addItemName(item.getItemId());
			sm.addNumber(num);
			player.sendPacket(sm);
			i++;
		}
		
		activeChar.sendMessage("Mass-created items in the inventory of " + i + " player(s).");
		LOG.info("GM " + activeChar.getName() + " mass_created item Id: " + id + " (" + num + ")");
		
		AdminHelpPage.showHelpPage(activeChar, "itemcreation.htm");
	}
	
	private void removeAllItems(L2PcInstance activeChar)
	{
		L2PcInstance Player = (L2PcInstance) activeChar.getTarget();
		
		if (Player == null)
		{
			activeChar.sendMessage("No target.");
			return;
		}
		
		for (L2ItemInstance item : Player.getInventory().getItems())
		{
			if (item.getLocation() == L2ItemInstance.ItemLocation.INVENTORY)
			{
				Player.getInventory().destroyItem("Destroy", item.getObjectId(), item.getCount(), Player, null);
			}
		}
		
		activeChar.sendPacket(new ItemList(activeChar, false));
		
		activeChar.sendMessage(Player.getName() + "'s inventory has been cleaned.");
		
		AdminHelpPage.showHelpPage(activeChar, "itemcreation.htm");
	}
}
