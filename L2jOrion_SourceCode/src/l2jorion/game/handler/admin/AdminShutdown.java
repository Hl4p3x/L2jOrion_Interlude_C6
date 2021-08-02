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

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.StringTokenizer;

import l2jorion.Config;
import l2jorion.game.Shutdown;
import l2jorion.game.controllers.GameTimeController;
import l2jorion.game.handler.IAdminCommandHandler;
import l2jorion.game.model.L2World;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.network.serverpackets.NpcHtmlMessage;

public class AdminShutdown implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
			"admin_server_shutdown", "admin_server_restart", "admin_rr", "admin_server_abort"
	};

	private enum CommandEnum
	{
		admin_server_shutdown,
		admin_server_restart,
		admin_rr,
		admin_server_abort
	}
	
	@Override
	public boolean useAdminCommand(String command, L2PcInstance activeChar)
	{
		StringTokenizer st = new StringTokenizer(command);
		
		CommandEnum comm = CommandEnum.valueOf(st.nextToken());
		
		if(comm == null)
			return false;
		
		switch(comm)
		{
			case admin_server_shutdown:{
				
				if(st.hasMoreTokens()){
					
					String secs = st.nextToken();
					
					try
					{
						int val = Integer.parseInt(secs);
						
						if(val>=0){
							serverShutdown(activeChar, val, false);
							return true;
						}
						activeChar.sendMessage("Negative Value is not allowed");
						return false;
					}
					catch(StringIndexOutOfBoundsException e)
					{
						sendHtmlForm(activeChar);
						return false;
					}
					
				}
				sendHtmlForm(activeChar);
				return false;
				
				
			}
			case admin_server_restart:
			{
				if(st.hasMoreTokens()){
					
					String secs = st.nextToken();
					
					try
					{
						int val = Integer.parseInt(secs);
						
						if (val >= 0)
						{
							serverShutdown(activeChar, val, true);
							return true;
						}
						activeChar.sendMessage("Negative Value is not allowed");
						return false;
					}
					catch(StringIndexOutOfBoundsException e)
					{
						sendHtmlForm(activeChar);
						return false;
					}
				}
				sendHtmlForm(activeChar);
				return false;
			}
			case admin_rr:
			{
				if(st.hasMoreTokens())
				{
					String secs = st.nextToken();
					try
					{
						int val = Integer.parseInt(secs);
						
						if(val>=0){
							serverShutdown(activeChar, val, true);
							return true;
						}
						activeChar.sendMessage("Negative Value is not allowed");
						return false;
					}
					catch(StringIndexOutOfBoundsException e)
					{
						sendHtmlForm(activeChar);
						return false;
					}
				}
				sendHtmlForm(activeChar);
				return false;
			}
			case admin_server_abort:
			{
				serverAbort(activeChar);
				return true;
			}
		}
		return false;
		
	}

	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}

	private void sendHtmlForm(L2PcInstance activeChar)
	{
		NpcHtmlMessage adminReply = new NpcHtmlMessage(5);

		int t = GameTimeController.getInstance().getGameTime();
		int h = t / 60;
		int m = t % 60;

		SimpleDateFormat format = new SimpleDateFormat("h:mm a");
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.HOUR_OF_DAY, h);
		cal.set(Calendar.MINUTE, m);
		adminReply.setFile("data/html/admin/shutdown.htm");
		adminReply.replace("%count%", String.valueOf(L2World.getInstance().getAllPlayers().values().size()));
		adminReply.replace("%used%", String.valueOf(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()));
		adminReply.replace("%xp%", String.valueOf(Config.RATE_XP));
		adminReply.replace("%sp%", String.valueOf(Config.RATE_SP));
		adminReply.replace("%adena%", String.valueOf(Config.RATE_DROP_ADENA));
		adminReply.replace("%drop%", String.valueOf(Config.RATE_DROP_ITEMS));
		adminReply.replace("%time%", String.valueOf(format.format(cal.getTime())));
		activeChar.sendPacket(adminReply);

		adminReply = null;
		format = null;
		cal = null;
	}

	private void serverShutdown(L2PcInstance activeChar, int seconds, boolean restart)
	{
		Shutdown.getInstance().startShutdown(activeChar, seconds, restart);
	}

	private void serverAbort(L2PcInstance activeChar)
	{
		Shutdown.getInstance().abort(activeChar);
	}

}
