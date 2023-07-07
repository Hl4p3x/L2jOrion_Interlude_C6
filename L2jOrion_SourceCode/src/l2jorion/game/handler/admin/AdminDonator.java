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

import java.sql.Connection;
import java.sql.PreparedStatement;

import l2jorion.Config;
import l2jorion.game.datatables.GmListTable;
import l2jorion.game.handler.IAdminCommandHandler;
import l2jorion.game.model.L2Object;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.entity.Announcements;
import l2jorion.game.network.serverpackets.SocialAction;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;
import l2jorion.util.CloseUtil;
import l2jorion.util.database.L2DatabaseFactory;

public class AdminDonator implements IAdminCommandHandler
{
	private static String[] ADMIN_COMMANDS =
	{
		"admin_setdonator"
	};
	
	protected static final Logger LOG = LoggerFactory.getLogger(AdminDonator.class);
	
	@Override
	public boolean useAdminCommand(final String command, final L2PcInstance activeChar)
	{
		if (activeChar == null)
		{
			return false;
		}
		
		if (command.startsWith("admin_setdonator"))
		{
			L2Object target = activeChar.getTarget();
			
			if (target instanceof L2PcInstance)
			{
				L2PcInstance targetPlayer = (L2PcInstance) target;
				final boolean newDonator = !targetPlayer.isDonator();
				
				if (newDonator)
				{
					targetPlayer.setDonator(true);
					targetPlayer.updateNameTitleColor();
					updateDatabase(targetPlayer, true);
					sendMessages(true, targetPlayer, activeChar, false, true);
					targetPlayer.broadcastPacket(new SocialAction(targetPlayer.getObjectId(), 16));
					targetPlayer.broadcastUserInfo();
				}
				else
				{
					targetPlayer.setDonator(false);
					targetPlayer.updateNameTitleColor();
					updateDatabase(targetPlayer, false);
					sendMessages(false, targetPlayer, activeChar, false, true);
					targetPlayer.broadcastUserInfo();
				}
			}
			else
			{
				activeChar.sendMessage("Impossible to set a non Player Target as Donator.");
				LOG.info("GM: " + activeChar.getName() + " is trying to set a non Player Target as Donator.");
				
				return false;
			}
		}
		return true;
	}
	
	private void sendMessages(final boolean forNewDonator, final L2PcInstance player, final L2PcInstance gm, final boolean announce, final boolean notifyGmList)
	{
		if (forNewDonator)
		{
			player.sendMessage(gm.getName() + " has granted Donator Status for you!");
			gm.sendMessage("You've granted Donator Status for " + player.getName());
			
			if (announce)
			{
				Announcements.getInstance().announceToAll(player.getName() + " has received Donator Status!");
			}
			
			if (notifyGmList)
			{
				GmListTable.broadcastMessageToGMs("Warn: " + gm.getName() + " has set " + player.getName() + " as Donator !");
			}
		}
		else
		{
			player.sendMessage(gm.getName() + " has revoked Donator Status from you!");
			gm.sendMessage("You've revoked Donator Status from " + player.getName());
			
			if (announce)
			{
				Announcements.getInstance().announceToAll(player.getName() + " has lost Donator Status!");
			}
			
			if (notifyGmList)
			{
				GmListTable.broadcastMessageToGMs("Warn: " + gm.getName() + " has removed Donator Status of player" + player.getName());
			}
		}
	}
	
	private void updateDatabase(final L2PcInstance player, final boolean newDonator)
	{
		Connection con = null;
		try
		{
			// prevents any NPE.
			// ----------------
			if (player == null)
			{
				return;
			}
			
			// Database Connection
			// --------------------------------
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement stmt = con.prepareStatement(newDonator ? INSERT_DATA : DEL_DATA);
			
			// if it is a new donator insert proper data
			// --------------------------------------------
			if (newDonator)
			{
				stmt.setInt(1, player.getObjectId());
				stmt.setString(2, player.getName());
				stmt.setInt(3, player.isHero() ? 1 : 0);
				stmt.setInt(4, player.isNoble() ? 1 : 0);
				stmt.setInt(5, 1);
				stmt.execute();
				stmt.close();
				stmt = null;
			}
			else
			// deletes from database
			{
				stmt.setInt(1, player.getObjectId());
				stmt.execute();
				stmt.close();
				stmt = null;
			}
		}
		catch (final Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			
			LOG.error("Error: could not update database: ", e);
		}
		finally
		{
			CloseUtil.close(con);
		}
	}
	
	// Updates That Will be Executed by MySQL
	// ----------------------------------------
	String INSERT_DATA = "REPLACE INTO characters_custom_data (obj_Id, char_name, hero, noble, donator) VALUES (?,?,?,?,?)";
	String DEL_DATA = "UPDATE characters_custom_data SET donator = 0 WHERE obj_Id=?";
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}
