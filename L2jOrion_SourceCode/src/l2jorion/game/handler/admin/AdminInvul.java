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
import l2jorion.game.model.L2Object;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;

/**
 * This class handles following admin commands: - invul = turns invulnerability on/off
 * @version $Revision: 1.2.4.4 $ $Date: 2007/07/31 10:06:02 $
 */
public class AdminInvul implements IAdminCommandHandler
{
	private static Logger LOG = LoggerFactory.getLogger(AdminInvul.class);
	
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_invul",
		"admin_setinvul"
	};
	
	@Override
	public boolean useAdminCommand(final String command, final L2PcInstance activeChar)
	{
		if (command.equals("admin_invul"))
		{
			handleInvul(activeChar);
		}
		
		if (command.equals("admin_setinvul"))
		{
			L2Object target = activeChar.getTarget();
			
			if (target instanceof L2PcInstance)
			{
				handleInvul((L2PcInstance) target);
			}
		}
		
		return true;
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
	
	private void handleInvul(final L2PcInstance activeChar)
	{
		String text;
		if (activeChar.isInvul())
		{
			activeChar.setIsInvul(false);
			text = activeChar.getName() + " is now mortal";
			if (Config.DEBUG)
			{
				LOG.debug("GM: Gm removed invul mode from character " + activeChar.getName() + "(" + activeChar.getObjectId() + ")");
			}
		}
		else
		{
			activeChar.setIsInvul(true);
			text = activeChar.getName() + " is now invulnerable";
			if (Config.DEBUG)
			{
				LOG.debug("GM: Gm activated invul mode for character " + activeChar.getName() + "(" + activeChar.getObjectId() + ")");
			}
		}
		
		activeChar.sendMessage(text);
		GmListTable.broadcastMessageToGMs("Warn: " + text +"");
	}
}
