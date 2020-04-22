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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import javolution.text.TextBuilder;
import l2jorion.Config;
import l2jorion.game.handler.IAdminCommandHandler;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.network.serverpackets.NpcHtmlMessage;
import l2jorion.util.CloseUtil;
import l2jorion.util.database.DatabaseUtils;
import l2jorion.util.database.L2DatabaseFactory;

/**
 * @author Blaze
 */
public class AdminWalker implements IAdminCommandHandler
{
	private static int _npcid = 0;
	private static int _point = 1;
	private static String _text = "";
	private static int _mode = 1;
	private static int _routeid = 0;
	
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_walker_setmessage",
		"admin_walker_menu",
		"admin_walker_setnpc",
		"admin_walker_setpoint",
		"admin_walker_setmode",
		"admin_walker_addpoint"
	};
	
	@Override
	public boolean useAdminCommand(final String command, final L2PcInstance activeChar)
	{
		/*
		 * if(!AdminCommandAccessRights.getInstance().hasAccess(command, activeChar.getAccessLevel())){ return false; } if(Config.GMAUDIT) { Logger _logAudit = Logger.getLogger("gmaudit"); LogRecord record = new LogRecord(Level.INFO, command); record.setParameters(new Object[] { "GM: " +
		 * activeChar.getName(), " to target [" + activeChar.getTarget() + "] " }); _logAudit.LOGGER(record); }
		 */
		
		try
		{
			String[] parts = command.split(" ");
			
			if (command.startsWith("admin_walker_menu"))
			{
				mainMenu(activeChar);
			}
			else if (command.startsWith("admin_walker_setnpc "))
			{
				try
				{
					_npcid = Integer.parseInt(parts[1]);
					
					Connection con = null;
					
					try
					{
						con = L2DatabaseFactory.getInstance().getConnection();
						PreparedStatement statement = con.prepareStatement("SELECT `route_id` FROM `walker_routes` WHERE `npc_id` = " + _npcid + ";");
						final ResultSet rset = statement.executeQuery();
						
						if (rset.next())
						{
							activeChar.sendMessage("Such NPC already was, we add routes");
							_routeid = rset.getInt("route_id");
						}
						else
						{
							statement = con.prepareStatement("SELECT MAX(`route_id`) AS max FROM `walker_routes`;");
							ResultSet rset1 = statement.executeQuery();
							
							if (rset1.next())
							{
								_routeid = rset1.getInt("max") + 1;
							}
							
							rset1.close();
							rset1 = null;
						}
						
						DatabaseUtils.close(statement);
						statement = null;
					}
					catch (final Exception e)
					{
						if (Config.ENABLE_ALL_EXCEPTIONS)
							e.printStackTrace();
					}
					finally
					{
						CloseUtil.close(con);
						
					}
					
					_point = 1;
				}
				catch (final NumberFormatException e)
				{
					if (Config.ENABLE_ALL_EXCEPTIONS)
						e.printStackTrace();
					
					activeChar.sendMessage("The incorrect identifier");
				}
				
				mainMenu(activeChar);
			}
			else if (command.startsWith("admin_walker_addpoint"))
			{
				addMenu(activeChar);
			}
			else if (command.startsWith("admin_walker_setmode"))
			{
				if (_mode == 1)
				{
					_mode = 0;
				}
				else
				{
					_mode = 1;
				}
				
				addMenu(activeChar);
			}
			else if (command.startsWith("admin_walker_setmessage"))
			{
				_text = command.substring(24);
				addMenu(activeChar);
			}
			else if (command.startsWith("admin_walker_setpoint"))
			{
				setPoint(activeChar.getX(), activeChar.getY(), activeChar.getZ());
				_point++;
				addMenu(activeChar);
			}
			
			parts = null;
		}
		catch (final Exception e)
		{
			e.printStackTrace();
		}
		return true;
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
	
	private void setPoint(final int x, final int y, final int z)
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement;
			
			if (_text.isEmpty())
			{
				statement = con.prepareStatement("INSERT INTO `lineage`.`walker_routes` (`route_id` ,`npc_id` ,`move_point` ,`chatText` ,`move_x` ,`move_y` ,`move_z` ,`delay` ,`running` )" + "VALUES (?, ?, ?, NULL, ?, ?, ?, ?, ?);");
				statement.setInt(1, _routeid);
				statement.setInt(2, _npcid);
				statement.setInt(3, _point);
				statement.setInt(4, x);
				statement.setInt(5, y);
				statement.setInt(6, z);
				statement.setInt(7, 0);
				statement.setInt(8, _mode);
			}
			else
			{
				statement = con.prepareStatement("INSERT INTO `lineage`.`walker_routes` (`route_id` ,`npc_id` ,`move_point` ,`chatText` ,`move_x` ,`move_y` ,`move_z` ,`delay` ,`running` )" + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?);");
				statement.setInt(1, _routeid);
				statement.setInt(2, _npcid);
				statement.setInt(3, _point);
				statement.setString(4, _text);
				statement.setInt(5, x);
				statement.setInt(6, y);
				statement.setInt(7, z);
				statement.setInt(8, 0);
				statement.setInt(9, _mode);
			}
			statement.execute();
			DatabaseUtils.close(statement);
		}
		catch (final Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
				e.printStackTrace();
		}
		finally
		{
			CloseUtil.close(con);
		}
	}
	
	/**
	 * @param activeChar
	 */
	private void mainMenu(final L2PcInstance activeChar)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(activeChar.getObjectId());
		final TextBuilder sb = new TextBuilder();
		sb.append("<html><body><font color=\"009900\">Editing Walkers</font><br>");
		sb.append("<br>");
		sb.append("Is chosen NPCID: " + _npcid + "<br>");
		sb.append("Number of the current point: " + _point + "<br>");
		sb.append("<edit var=\"id\" width=80 height=15><br>");
		sb.append("<button value=\"New NPC\" action=\"bypass -h admin_walker_setnpc $id\" width=80 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"><br>");
		sb.append("<button value=\"To establish a point\" action=\"bypass -h admin_walker_addpoint width=80 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"><br>");
		sb.append("</body></html>");
		html.setHtml(sb.toString());
		activeChar.sendPacket(html);
	}
	
	/**
	 * @param activeChar
	 */
	private void addMenu(final L2PcInstance activeChar)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(activeChar.getObjectId());
		final TextBuilder sb = new TextBuilder();
		sb.append("<html><body><font color=\"009900\">Editing Walkers</font><br>");
		sb.append("<br>");
		sb.append("Is chosen NPCID: " + _npcid + "<br>");
		sb.append("Number of the current point: " + _point + "<br>");
		sb.append("Number of the current route: " + _routeid + "<br>");
		
		if (_mode == 1)
		{
			sb.append("Mode: Run<br>");
		}
		else
		{
			sb.append("Mode: Step<br>");
		}
		
		if (_text.isEmpty())
		{
			sb.append("The phrase is not established<br>");
		}
		else
		{
			sb.append("The phrase: " + _text + "<br>");
		}
		
		sb.append("<edit var=\"id\" width=80 height=15><br>");
		sb.append("<button value=\"To establish a phrase\" action=\"bypass -h admin_walker_setmessage $id width=80 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"><br>");
		sb.append("<button value=\"To add a point\" action=\"bypass -h admin_walker_setpoint width=80 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"><br>");
		sb.append("<button value=\"To change a mode\" action=\"bypass -h admin_walker_setmode width=80 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"><br>");
		sb.append("<button value=\"The main menu\" action=\"bypass -h admin_walker_menu width=80 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"><br>");
		sb.append("</body></html>");
		html.setHtml(sb.toString());
		activeChar.sendPacket(html);
	}
}
