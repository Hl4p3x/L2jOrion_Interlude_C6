/*
 * L2jOrion Project - www.l2jorion.com 
 * 
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package l2jorion.game.handler.admin;

import java.util.StringTokenizer;

import javolution.text.TextBuilder;
import l2jorion.game.datatables.sql.ClanTable;
import l2jorion.game.handler.IAdminCommandHandler;
import l2jorion.game.managers.FortManager;
import l2jorion.game.model.L2Clan;
import l2jorion.game.model.L2Object;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.entity.siege.Fort;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.NpcHtmlMessage;
import l2jorion.game.network.serverpackets.SystemMessage;

/**
 * This class handles all siege commands: Todo: change the class name, and neaten it up
 * @author programmos
 */
public class AdminFortSiege implements IAdminCommandHandler
{
	// private static Logger LOG = LoggerFactory.getLogger(AdminFortSiege.class);
	
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_fortsiege",
		"admin_add_fortattacker",
		"admin_add_fortdefender",
		"admin_add_fortguard",
		"admin_list_fortsiege_clans",
		"admin_clear_fortsiege_list",
		"admin_move_fortdefenders",
		"admin_spawn_fortdoors",
		"admin_endfortsiege",
		"admin_startfortsiege",
		"admin_setfort",
		"admin_removefort"
	};
	
	@Override
	public boolean useAdminCommand(String command, final L2PcInstance activeChar)
	{
		/*
		 * if(!AdminCommandAccessRights.getInstance().hasAccess(command, activeChar.getAccessLevel())){ return false; } if(Config.GMAUDIT) { Logger _logAudit = Logger.getLogger("gmaudit"); LogRecord record = new LogRecord(Level.INFO, command); record.setParameters(new Object[] { "GM: " +
		 * activeChar.getName(), " to target [" + activeChar.getTarget() + "] " }); _logAudit.LOGGER(record); }
		 */
		
		final StringTokenizer st = new StringTokenizer(command, " ");
		command = st.nextToken(); // Get actual command
		
		// Get fort
		Fort fort = null;
		
		if (st.hasMoreTokens())
		{
			fort = FortManager.getInstance().getFort(st.nextToken());
		}
		
		// Get fort
		// String val = "";
		//
		// if(st.hasMoreTokens())
		// {
		// val = st.nextToken();
		// }
		//
		// val = null;
		
		// No fort specified
		if (fort == null || fort.getFortId() < 0)
		{
			showFortSelectPage(activeChar);
		}
		else
		{
			L2Object target = activeChar.getTarget();
			L2PcInstance player = null;
			
			if (target instanceof L2PcInstance)
			{
				player = (L2PcInstance) target;
			}
			
			target = null;
			
			if (command.equalsIgnoreCase("admin_add_fortattacker"))
			{
				if (player == null)
				{
					activeChar.sendPacket(new SystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
				}
				else
				{
					fort.getSiege().registerAttacker(player, true);
				}
			}
			else if (command.equalsIgnoreCase("admin_add_fortdefender"))
			{
				if (player == null)
				{
					activeChar.sendPacket(new SystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
				}
				else
				{
					fort.getSiege().registerDefender(player, true);
				}
			}
			// FIXME
			// else if (command.equalsIgnoreCase("admin_add_guard"))
			// {
			// try
			// {
			// int npcId = Integer.parseInt(val);
			// fort.getSiege().getFortSiegeGuardManager().addFortSiegeGuard(activeChar, npcId);
			// }
			// catch (Exception e)
			// {
			// activeChar.sendMessage("Usage: //add_guard npcId");
			// }
			// }
			else if (command.equalsIgnoreCase("admin_clear_fortsiege_list"))
			{
				fort.getSiege().clearSiegeClan();
			}
			else if (command.equalsIgnoreCase("admin_endfortsiege"))
			{
				fort.getSiege().endSiege();
			}
			else if (command.equalsIgnoreCase("admin_list_fortsiege_clans"))
			{
				fort.getSiege().listRegisterClan(activeChar);
				
				return true;
			}
			else if (command.equalsIgnoreCase("admin_move_fortdefenders"))
			{
				activeChar.sendMessage("Not implemented yet.");
			}
			else if (command.equalsIgnoreCase("admin_setfort"))
			{
				if (player == null || player.getClan() == null)
				{
					activeChar.sendPacket(new SystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
				}
				else
				{
					fort.setOwner(player.getClan());
				}
			}
			else if (command.equalsIgnoreCase("admin_removefort"))
			{
				L2Clan clan = ClanTable.getInstance().getClan(fort.getOwnerId());
				
				if (clan != null)
				{
					fort.removeOwner(clan);
				}
				else
				{
					activeChar.sendMessage("Unable to remove fort");
				}
				
				clan = null;
			}
			else if (command.equalsIgnoreCase("admin_spawn_fortdoors"))
			{
				fort.spawnDoor();
			}
			else if (command.equalsIgnoreCase("admin_startfortsiege"))
			{
				fort.getSiege().startSiege();
			}
			
			showFortSiegePage(activeChar, fort.getName());
			
			player = null;
		}
		
		return true;
	}
	
	private void showFortSelectPage(final L2PcInstance activeChar)
	{
		int i = 0;
		
		NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
		adminReply.setFile("data/html/admin/forts.htm");
		TextBuilder cList = new TextBuilder();
		
		for (final Fort fort : FortManager.getInstance().getForts())
		{
			if (fort != null)
			{
				String name = fort.getName();
				cList.append("<td fixwidth=90><a action=\"bypass -h admin_fortsiege " + name + "\">" + name + "</a></td>");
				name = null;
				i++;
			}
			
			if (i > 2)
			{
				cList.append("</tr><tr>");
				i = 0;
			}
		}
		
		adminReply.replace("%forts%", cList.toString());
		activeChar.sendPacket(adminReply);
		
		cList = null;
		adminReply = null;
	}
	
	private void showFortSiegePage(final L2PcInstance activeChar, final String fortName)
	{
		NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
		adminReply.setFile("data/html/admin/fort.htm");
		adminReply.replace("%fortName%", fortName);
		activeChar.sendPacket(adminReply);
		
		adminReply = null;
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
	
}
