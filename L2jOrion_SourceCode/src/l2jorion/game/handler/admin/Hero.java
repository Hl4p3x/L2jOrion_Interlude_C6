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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import l2jorion.game.datatables.GmListTable;
import l2jorion.game.handler.IAdminCommandHandler;
import l2jorion.game.model.L2Object;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.network.serverpackets.SocialAction;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;
import l2jorion.util.CloseUtil;
import l2jorion.util.database.L2DatabaseFactory;

/**
 * L2Scoria
 **/
public class Hero implements IAdminCommandHandler
{
	private static String[] ADMIN_COMMANDS =
	{
		"admin_sethero"
	};
	
	private final static Logger LOG = LoggerFactory.getLogger(Hero.class.getName());
	
	@Override
	public boolean useAdminCommand(String command, L2PcInstance activeChar)
	{
		
		if (command.startsWith("admin_sethero"))
		{
			String[] cmdParams = command.split(" ");
			
			long heroTime = 0;
			if (cmdParams.length > 1)
			{
				try
				{
					heroTime = Integer.parseInt(cmdParams[1]) * 24L * 60L * 60L * 1000L;
				}
				catch (NumberFormatException nfe)
				{
					// None
				}
			}
			
			L2Object target = activeChar.getTarget();
			
			if (target.isPlayer())
			{
				L2PcInstance targetPlayer = (L2PcInstance) target;
				boolean newHero = !targetPlayer.isHero();
				
				if (newHero)
				{
					targetPlayer.setHero(true);
					targetPlayer.sendMessage("You are now a hero.");
					updateDatabase(targetPlayer, true, heroTime);
					sendMessages(true, targetPlayer, activeChar, true, true);
					targetPlayer.broadcastPacket(new SocialAction(targetPlayer.getObjectId(), 16));
					targetPlayer.broadcastUserInfo();
				}
				else
				{
					targetPlayer.setHero(false);
					targetPlayer.sendMessage("You are no longer a hero.");
					updateDatabase(targetPlayer, false, 0);
					sendMessages(false, targetPlayer, activeChar, true, true);
					targetPlayer.broadcastUserInfo();
				}
			}
			else
			{
				activeChar.sendMessage("Impossible to set a non Player Target as hero.");
				
				return false;
			}
		}
		return true;
	}
	
	private void sendMessages(boolean fornewHero, L2PcInstance player, L2PcInstance gm, boolean announce, boolean notifyGmList)
	{
		if (fornewHero)
		{
			player.sendMessage(gm.getName() + " has granted Hero Status for you!");
			gm.sendMessage("You've granted Hero Status for " + player.getName());
			
			if (notifyGmList)
			{
				GmListTable.broadcastMessageToGMs("Warn: " + gm.getName() + " has set " + player.getName() + " as Hero !");
			}
		}
		else
		{
			player.sendMessage(gm.getName() + " has revoked Hero Status from you!");
			gm.sendMessage("You've revoked Hero Status from " + player.getName());
			
			if (notifyGmList)
			{
				GmListTable.broadcastMessageToGMs("Warn: " + gm.getName() + " has removed Hero Status of player " + player.getName());
			}
		}
	}
	
	/**
	 * @param player
	 * @param newHero
	 * @param heroTime
	 */
	private void updateDatabase(L2PcInstance player, boolean newHero, long heroTime)
	{
		Connection con = null;
		try
		{
			if (player == null)
			{
				return;
			}
			
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT * FROM characters_custom_data WHERE obj_Id=?");
			statement.setInt(1, player.getObjectId());
			ResultSet result = statement.executeQuery();
			
			if (result.next())
			{
				PreparedStatement stmt = con.prepareStatement(newHero ? UPDATE_DATA : DEL_DATA);
				if (newHero)
				{
					stmt.setLong(1, heroTime == 0 ? 0 : System.currentTimeMillis() + heroTime);
					stmt.setInt(2, player.getObjectId());
					stmt.execute();
				}
				else
				{
					stmt.setInt(1, player.getObjectId());
					stmt.execute();
				}
				stmt.close();
				stmt = null;
			}
			else
			{
				if (newHero)
				{
					PreparedStatement stmt = con.prepareStatement(INSERT_DATA);
					stmt.setInt(1, player.getObjectId());
					stmt.setString(2, player.getName());
					stmt.setInt(3, 1);
					stmt.setInt(4, 1);
					stmt.setLong(5, heroTime == 0 ? 0 : System.currentTimeMillis() + heroTime);
					stmt.execute();
					stmt.close();
					stmt = null;
				}
			}
			result.close();
			statement.close();
			
			result = null;
			statement = null;
		}
		catch (Exception e)
		{
			LOG.warn("Error: could not update database: " + e);
		}
		finally
		{
			try
			{
				CloseUtil.close(con);
			}
			catch (Exception e)
			{
			}
			con = null;
		}
	}
	
	// Updates That Will be Executed by MySQL
	// ----------------------------------------
	String INSERT_DATA = "INSERT INTO characters_custom_data (obj_Id, char_name, noble, hero, hero_end_date) VALUES (?,?,?,?,?)";
	String UPDATE_DATA = "UPDATE characters_custom_data SET noble=1, hero=1, hero_end_date=? WHERE obj_Id=?";
	String DEL_DATA = "UPDATE characters_custom_data SET hero = 0, hero_end_date=0 WHERE obj_Id=?";
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}
