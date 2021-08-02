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
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.SystemMessage;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;

/**
 * This class handles following admin commands:
 * <ul>
 * <li>admin_unblockip</li>
 * </ul>
 * @version $Revision: 1.3.2.6.2.4 $ $Date: 2005/04/11 10:06:06 $
 */
public class AdminUnblockIp implements IAdminCommandHandler
{
	private static final Logger LOG = LoggerFactory.getLogger(AdminTeleport.class);
	
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_unblockip"
	};
	
	/*
	 * (non-Javadoc)
	 * @see l2jorion.game.handler.IAdminCommandHandler#useAdminCommand(java.lang.String, l2jorion.game.model.L2PcInstance)
	 */
	@Override
	public boolean useAdminCommand(final String command, final L2PcInstance activeChar)
	{
		/*
		 * if(!AdminCommandAccessRights.getInstance().hasAccess(command, activeChar.getAccessLevel())){ return false; } if(Config.GMAUDIT) { Logger _logAudit = Logger.getLogger("gmaudit"); LogRecord record = new LogRecord(Level.INFO, command); record.setParameters(new Object[] { "GM: " +
		 * activeChar.getName(), " to target [" + activeChar.getTarget() + "] " }); _logAudit.LOGGER(record); }
		 */
		
		if (command.startsWith("admin_unblockip "))
		{
			try
			{
				String ipAddress = command.substring(16);
				
				if (unblockIp(ipAddress, activeChar))
				{
					SystemMessage sm = new SystemMessage(SystemMessageId.S1_S2);
					sm.addString("Removed IP " + ipAddress + " from blocklist!");
					activeChar.sendPacket(sm);
					sm = null;
				}
				
				ipAddress = null;
			}
			catch (final StringIndexOutOfBoundsException e)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
					e.printStackTrace();
				
				// Send syntax to the user
				SystemMessage sm = new SystemMessage(SystemMessageId.S1_S2);
				sm.addString("Usage mode: //unblockip <ip>");
				activeChar.sendPacket(sm);
				sm = null;
			}
		}
		
		return true;
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
	
	private boolean unblockIp(final String ipAddress, final L2PcInstance activeChar)
	{
		// LoginServerThread.getInstance().unBlockip(ipAddress);
		LOG.warn("IP removed by GM " + activeChar.getName());
		
		return true;
	}
	
}
