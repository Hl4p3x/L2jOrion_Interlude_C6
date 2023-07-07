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
import java.util.StringTokenizer;

import l2jorion.Config;
import l2jorion.game.handler.IAdminCommandHandler;
import l2jorion.game.model.L2Character;
import l2jorion.game.model.L2Object;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.network.serverpackets.ExShowScreenMessage;
import l2jorion.game.network.serverpackets.PlaySound;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;
import l2jorion.util.CloseUtil;
import l2jorion.util.database.L2DatabaseFactory;

public class AdminPremium implements IAdminCommandHandler
{
	private String INSERT_DATA = "REPLACE INTO account_premium (account_name, premium_service, enddate) VALUES (?,?,?)";
	
	private static String[] ADMIN_COMMANDS =
	{
		"admin_setpremium"
	};
	
	protected static final Logger LOG = LoggerFactory.getLogger(AdminPremium.class);
	
	@Override
	public boolean useAdminCommand(final String command, final L2PcInstance activeChar)
	{
		if (activeChar == null)
		{
			return false;
		}
		
		if (command.startsWith("admin_setpremium"))
		{
			final StringTokenizer st = new StringTokenizer(command);
			st.nextToken();
			
			final String days = st.nextToken();
			
			L2Object target = activeChar.getTarget();
			
			if (target instanceof L2PcInstance)
			{
				if (Integer.parseInt(days) == 0)
				{
					((L2PcInstance) target).setPremiumService(0);
					((L2PcInstance) target).setPremiumExpire(0);
					((L2PcInstance) target).sendMessage("The Premium account status was removed.");
					((L2Character) target).sendPacket(new ExShowScreenMessage("The Premium account status was removed.", 4000, 0x02, false));
					return false;
				}
				
				((L2PcInstance) target).setPremiumService(1);
				updateDatabase(((L2PcInstance) target), Integer.parseInt(days) * 24L * 60L * 60L * 1000L);
				((L2PcInstance) target).sendMessage("Congratulations! You're The Premium account now.");
				((L2Character) target).sendPacket(new ExShowScreenMessage("Congratulations! You're The Premium account now.", 4000, 0x02, false));
				PlaySound playSound = new PlaySound("ItemSound.quest_fanfare_1");
				activeChar.sendPacket(playSound);
				
				if (Config.PREMIUM_NAME_COLOR_ENABLED && activeChar.getPremiumService() >= 1)
				{
					activeChar.getAppearance().setTitleColor(Config.PREMIUM_TITLE_COLOR);
				}
				
				if (Config.PREMIUM_BUFF_MULTIPLIER > 0)
				{
					((L2PcInstance) target).restoreEffects();
				}
				((L2PcInstance) target).broadcastUserInfo();
			}
		}
		
		return true;
	}
	
	private void updateDatabase(L2PcInstance player, long premiumTime)
	{
		Connection con = null;
		try
		{
			if (player == null)
			{
				return;
			}
			
			player.setPremiumExpire(System.currentTimeMillis() + premiumTime);
			
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement stmt = con.prepareStatement(INSERT_DATA);
			
			stmt.setString(1, player.getAccountName());
			stmt.setInt(2, 1);
			stmt.setLong(3, premiumTime == 0 ? 0 : System.currentTimeMillis() + premiumTime);
			stmt.execute();
			stmt.close();
		}
		catch (Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			
			LOG.warn("Error: could not update database: ", e);
		}
		finally
		{
			CloseUtil.close(con);
		}
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}
