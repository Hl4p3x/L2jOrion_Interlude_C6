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
import l2jorion.game.datatables.GmListTable;
import l2jorion.game.handler.IAdminCommandHandler;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;

/**
 * This class handles following admin commands: - gm = turns gm mode on/off
 * @version $Revision: 1.2.4.4 $ $Date: 2005/04/11 10:06:06 $
 */
public class AdminGm implements IAdminCommandHandler
{
	private static Logger LOG = LoggerFactory.getLogger(AdminGm.class);
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_gm"
	};
	
	@Override
	public boolean useAdminCommand(final String command, final L2PcInstance activeChar)
	{
		/*
		 * if(!AdminCommandAccessRights.getInstance().hasAccess(command, activeChar.getAccessLevel())){ return false; } if(Config.GMAUDIT) { Logger _logAudit = Logger.getLogger("gmaudit"); LogRecord record = new LogRecord(Level.INFO, command); record.setParameters(new Object[] { "GM: " +
		 * activeChar.getName(), " to target [" + activeChar.getTarget() + "] " }); _logAudit.LOGGER(record); }
		 */
		
		if (command.equals("admin_gm"))
		{
			handleGm(activeChar);
		}
		
		return true;
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
	
	private void handleGm(final L2PcInstance activeChar)
	{
		if (activeChar.isGM())
		{
			GmListTable.getInstance().deleteGm(activeChar);
			// activeChar.setIsGM(false);
			
			activeChar.sendMessage("You no longer have GM status.");
			
			if (Config.DEBUG)
			{
				LOG.debug("GM: " + activeChar.getName() + "(" + activeChar.getObjectId() + ") turned his GM status off");
			}
		}
		else
		{
			GmListTable.getInstance().addGm(activeChar, false);
			// activeChar.setIsGM(true);
			
			activeChar.sendMessage("You now have GM status.");
			
			if (Config.DEBUG)
			{
				LOG.debug("GM: " + activeChar.getName() + "(" + activeChar.getObjectId() + ") turned his GM status on");
			}
		}
	}
}
