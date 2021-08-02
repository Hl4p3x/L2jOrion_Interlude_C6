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
package l2jorion.game.network.clientpackets;

import l2jorion.Config;
import l2jorion.game.datatables.GmListTable;
import l2jorion.game.datatables.sql.AdminCommandAccessRights;
import l2jorion.game.handler.AdminCommandHandler;
import l2jorion.game.handler.IAdminCommandHandler;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.util.GMAudit;
import l2jorion.log.Log;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;

public final class SendBypassBuildCmd extends L2GameClientPacket
{
	protected static final Logger LOG = LoggerFactory.getLogger(SendBypassBuildCmd.class);
	public final static int GM_MESSAGE = 9;
	public final static int ANNOUNCEMENT = 10;
	
	private String _command;
	
	@Override
	protected void readImpl()
	{
		_command = "admin_" + readS().trim();
	}
	
	@Override
	protected void runImpl()
	{
		final L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null)
		{
			return;
		}
		
		// Checks The Access and notify requester if requester access it not allowed for that command
		if (!AdminCommandAccessRights.getInstance().hasAccess(_command, activeChar.getAccessLevel()))
		{
			activeChar.sendMessage("You don't have the access right to use this command!");
			GmListTable.broadcastMessageToGMs("Character " + activeChar.getName() + " tried to use admin command " + _command + ", but doesn't have access to it!");
			return;
		}
		
		// gets the Handler of That Commmand
		final IAdminCommandHandler ach = AdminCommandHandler.getInstance().getAdminCommandHandler(_command);
		
		// if handler is valid we Audit and use else we notify in console.
		if (ach != null)
		{
			if (Config.GMAUDIT)
			{
				GMAudit.auditGMAction(activeChar.getName() + " [" + activeChar.getObjectId() + "]", _command, (activeChar.getTarget() != null ? activeChar.getTarget().getName() : "no-target"));
				
			}
			
			ach.useAdminCommand(_command, activeChar);
		}
		else
		{
			activeChar.sendMessage("The command " + _command + " doesn't exists!");
			// LOG.warn("No handler registered for admin command '" + _command + "'");
			String text = "No handler registered for admin command '" + _command + "'";
			Log.add(text, "Wrong_admin_commands");
			return;
		}
	}
	
	@Override
	public String getType()
	{
		return "[C] 5b SendBypassBuildCmd";
	}
}
