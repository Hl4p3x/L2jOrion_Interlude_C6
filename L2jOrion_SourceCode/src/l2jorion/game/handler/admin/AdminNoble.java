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
import l2jorion.game.network.serverpackets.SocialAction;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;
import l2jorion.util.CloseUtil;
import l2jorion.util.database.L2DatabaseFactory;

public class AdminNoble implements IAdminCommandHandler
{
	private static String[] ADMIN_COMMANDS =
	{
		"admin_setnoble"
	};
	
	protected static final Logger LOG = LoggerFactory.getLogger(AdminNoble.class);
	
	@Override
	public boolean useAdminCommand(final String command, final L2PcInstance activeChar)
	{
		
		if (activeChar == null)
		{
			return false;
		}
		
		if (command.startsWith("admin_setnoble"))
		{
			L2Object target = activeChar.getTarget();
			
			if (target instanceof L2PcInstance)
			{
				L2PcInstance targetPlayer = (L2PcInstance) target;
				
				final boolean newNoble = !targetPlayer.isNoble();
				
				if (newNoble)
				{
					targetPlayer.setNoble(true);
					targetPlayer.sendMessage("You are now a noblesse.");
					updateDatabase(targetPlayer, true);
					sendMessages(true, targetPlayer, activeChar, true, true);
					targetPlayer.broadcastPacket(new SocialAction(targetPlayer.getObjectId(), 16));
				}
				else
				{
					targetPlayer.setNoble(false);
					targetPlayer.sendMessage("You are no longer a noblesse.");
					updateDatabase(targetPlayer, false);
					sendMessages(false, targetPlayer, activeChar, true, true);
				}
			}
			else
			{
				activeChar.sendMessage("Impossible to set a non Player Target as noble.");
				LOG.info("GM: " + activeChar.getName() + " is trying to set a non Player Target as noble.");
				return false;
			}
		}
		
		return true;
	}
	
	private void sendMessages(final boolean forNewNoble, final L2PcInstance player, final L2PcInstance gm, final boolean announce, final boolean notifyGmList)
	{
		if (forNewNoble)
		{
			player.sendMessage(gm.getName() + " has granted Noble Status from you!");
			gm.sendMessage("You've granted Noble Status from " + player.getName());
			
			if (notifyGmList)
			{
				GmListTable.broadcastMessageToGMs("Warn: " + gm.getName() + " has set " + player.getName() + " as Noble !");
			}
		}
		else
		{
			player.sendMessage(gm.getName() + " has revoked Noble Status for you!");
			gm.sendMessage("You've revoked Noble Status for " + player.getName());
			
			if (notifyGmList)
			{
				GmListTable.broadcastMessageToGMs("Warn: " + gm.getName() + " has removed Noble Status of player" + player.getName());
			}
		}
	}
	
	private void updateDatabase(final L2PcInstance player, final boolean newNoble)
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
			PreparedStatement stmt = con.prepareStatement(newNoble ? INSERT_DATA : DEL_DATA);
			
			// if it is a new donator insert proper data
			// --------------------------------------------
			if (newNoble)
			{
				
				stmt.setInt(1, player.getObjectId());
				stmt.setString(2, player.getName());
				stmt.setInt(3, player.isHero() ? 1 : 0);
				stmt.setInt(4, 1);
				stmt.setInt(5, player.isDonator() ? 1 : 0);
				stmt.execute();
				stmt.close();
			}
			else
			// deletes from database
			{
				stmt.setInt(1, player.getObjectId());
				stmt.execute();
				stmt.close();
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
	String DEL_DATA = "UPDATE characters_custom_data SET noble = 0 WHERE obj_Id=?";
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}
