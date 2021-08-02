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
package l2jorion.game.handler.admin;

import l2jorion.Config;
import l2jorion.game.handler.IAdminCommandHandler;
import l2jorion.game.model.Inventory;
import l2jorion.game.model.L2Object;
import l2jorion.game.model.actor.instance.L2ItemInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.CharInfo;
import l2jorion.game.network.serverpackets.InventoryUpdate;
import l2jorion.game.network.serverpackets.SystemMessage;
import l2jorion.game.network.serverpackets.UserInfo;
import l2jorion.game.util.IllegalPlayerAction;
import l2jorion.game.util.Util;

/**
 * This class handles following admin commands: - enchant_armor
 * @version $Revision: 1.3.2.1.2.10 $ $Date: 2005/08/24 21:06:06 $
 */
public class AdminEnchant implements IAdminCommandHandler
{
	// private static Logger LOG = LoggerFactory.getLogger(AdminEnchant.class);
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_seteh",// 6
		"admin_setec",// 10
		"admin_seteg",// 9
		"admin_setel",// 11
		"admin_seteb",// 12
		"admin_setew",// 7
		"admin_setes",// 8
		"admin_setle",// 1
		"admin_setre",// 2
		"admin_setlf",// 4
		"admin_setrf",// 5
		"admin_seten",// 3
		"admin_setun",// 0
		"admin_setba",// 13
		"admin_enchant"
	};
	
	@Override
	public boolean useAdminCommand(final String command, final L2PcInstance activeChar)
	{
		if (command.equals("admin_enchant"))
		{
			showMainPage(activeChar);
		}
		else
		{
			int armorType = -1;
			
			if (command.startsWith("admin_seteh"))
			{
				armorType = Inventory.PAPERDOLL_HEAD;
			}
			else if (command.startsWith("admin_setec"))
			{
				armorType = Inventory.PAPERDOLL_CHEST;
			}
			else if (command.startsWith("admin_seteg"))
			{
				armorType = Inventory.PAPERDOLL_GLOVES;
			}
			else if (command.startsWith("admin_seteb"))
			{
				armorType = Inventory.PAPERDOLL_FEET;
			}
			else if (command.startsWith("admin_setel"))
			{
				armorType = Inventory.PAPERDOLL_LEGS;
			}
			else if (command.startsWith("admin_setew"))
			{
				armorType = Inventory.PAPERDOLL_RHAND;
			}
			else if (command.startsWith("admin_setes"))
			{
				armorType = Inventory.PAPERDOLL_LHAND;
			}
			else if (command.startsWith("admin_setle"))
			{
				armorType = Inventory.PAPERDOLL_LEAR;
			}
			else if (command.startsWith("admin_setre"))
			{
				armorType = Inventory.PAPERDOLL_REAR;
			}
			else if (command.startsWith("admin_setlf"))
			{
				armorType = Inventory.PAPERDOLL_LFINGER;
			}
			else if (command.startsWith("admin_setrf"))
			{
				armorType = Inventory.PAPERDOLL_RFINGER;
			}
			else if (command.startsWith("admin_seten"))
			{
				armorType = Inventory.PAPERDOLL_NECK;
			}
			else if (command.startsWith("admin_setun"))
			{
				armorType = Inventory.PAPERDOLL_UNDER;
			}
			else if (command.startsWith("admin_setba"))
			{
				armorType = Inventory.PAPERDOLL_BACK;
			}
			
			if (armorType != -1)
			{
				try
				{
					final int ench = Integer.parseInt(command.substring(12));
					
					// check value
					if (ench < 0 || ench > 99999999)
					{
						activeChar.sendMessage("You must set the enchant level to be between 0-99999.");
					}
					else
					{
						setEnchant(activeChar, ench, armorType);
					}
				}
				catch (final StringIndexOutOfBoundsException e)
				{
					if (Config.ENABLE_ALL_EXCEPTIONS)
						e.printStackTrace();
					
					activeChar.sendMessage("Please specify a new enchant value.");
				}
				catch (final NumberFormatException e)
				{
					if (Config.ENABLE_ALL_EXCEPTIONS)
						e.printStackTrace();
					
					activeChar.sendMessage("Please specify a valid new enchant value.");
				}
			}
			
			// show the enchant menu after an action
			showMainPage(activeChar);
		}
		
		return true;
	}
	
	private void setEnchant(final L2PcInstance activeChar, final int ench, final int armorType)
	{
		// get the target
		L2Object target = activeChar.getTarget();
		if (target == null)
		{
			target = activeChar;
		}
		
		L2PcInstance player = null;
		
		if (target instanceof L2PcInstance)
		{
			player = (L2PcInstance) target;
		}
		else
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.INCORRECT_TARGET));
			return;
		}
		
		target = null;
		
		// now we need to find the equipped weapon of the targeted character...
		int curEnchant = 0; // display purposes only
		
		L2ItemInstance itemInstance = null;
		
		// only attempt to enchant if there is a weapon equipped
		L2ItemInstance parmorInstance = player.getInventory().getPaperdollItem(armorType);
		
		if (parmorInstance != null && parmorInstance.getEquipSlot() == armorType)
		{
			itemInstance = parmorInstance;
		}
		else
		{
			// for bows and double handed weapons
			parmorInstance = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_LRHAND);
			
			if (parmorInstance != null && parmorInstance.getEquipSlot() == Inventory.PAPERDOLL_LRHAND)
			{
				itemInstance = parmorInstance;
			}
		}
		
		parmorInstance = null;
		
		if (itemInstance != null)
		{
			/**
			 * Protection against Corrupt GMs This protection will ban both GM and Edited char if a GM tries to enchant a NON GM player above the value specified in the file: other.properties (GMOverEnchant = XX)
			 */
			curEnchant = itemInstance.getEnchantLevel();
			
			if (Config.GM_OVER_ENCHANT != 0 && ench >= Config.GM_OVER_ENCHANT && !player.isGM())
			{
				player.sendMessage("A GM tried to overenchant you. You will both be banned.");
				Util.handleIllegalPlayerAction(player, "The player " + player.getName() + " has been edited. BAN!", IllegalPlayerAction.PUNISH_KICKBAN);
				activeChar.sendMessage("You tried to overenchant somebody. You will both be banned.");
				Util.handleIllegalPlayerAction(activeChar, "The GM " + activeChar.getName() + " has overenchanted the player " + player.getName() + ". BAN!", IllegalPlayerAction.PUNISH_KICKBAN);
			}
			else
			{
				// set enchant value
				player.getInventory().unEquipItemInSlotAndRecord(armorType);
				itemInstance.setEnchantLevel(ench);
				player.getInventory().equipItemAndRecord(itemInstance);
				
				// send packets
				InventoryUpdate iu = new InventoryUpdate();
				iu.addModifiedItem(itemInstance);
				player.sendPacket(iu);
				player.broadcastPacket(new CharInfo(player));
				player.sendPacket(new UserInfo(player));
				
				iu = null;
				
				// informations
				activeChar.sendMessage("Changed enchantment of " + player.getName() + "'s " + itemInstance.getItem().getName() + " from " + curEnchant + " to " + ench + ".");
				player.sendMessage("Admin has changed the enchantment of your " + itemInstance.getItem().getName() + " from " + curEnchant + " to " + ench + ".");
			}
		}
		
		player = null;
		itemInstance = null;
	}
	
	private void showMainPage(final L2PcInstance activeChar)
	{
		AdminHelpPage.showHelpPage(activeChar, "enchant.htm");
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
	
}
