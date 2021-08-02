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

import l2jorion.game.handler.IAdminCommandHandler;
import l2jorion.game.managers.PacketsLoggerManager;
import l2jorion.game.model.L2World;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.SystemMessage;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;

public class AdminCharSupervision implements IAdminCommandHandler
{
	protected static final Logger LOG = LoggerFactory.getLogger(AdminCharSupervision.class);
	
	private static String[] ADMIN_COMMANDS =
	{
		"admin_start_monitor_char",
		"admin_stop_monitor_char",
		"admin_block_char_packet",
		"admin_restore_char_packet"
	};
	
	private enum CommandEnum
	{
		admin_start_monitor_char,
		admin_stop_monitor_char,
		admin_block_char_packet,
		admin_restore_char_packet
		
	}
	
	@Override
	public boolean useAdminCommand(final String command, final L2PcInstance activeChar)
	{
		final StringTokenizer st = new StringTokenizer(command);
		
		final CommandEnum comm = CommandEnum.valueOf(st.nextToken());
		
		if (comm == null)
			return false;
		
		switch (comm)
		{
			case admin_block_char_packet:
			{
				
				String val = "";
				
				if (st.hasMoreTokens())
				{
					
					while (st.hasMoreTokens())
					{
						if (val.equals(""))
						{
							val = st.nextToken();
						}
						else
						{
							val += " " + st.nextToken();
						}
					}
					
				}
				else
				{
					activeChar.sendMessage("Usage: //admin_block_char_packet <char_name> <packet_op_code1>,<packet_op_code2>");
					return false;
				}
				
				final String[] charName_packet = val.split(" ");
				
				if (charName_packet.length < 2)
				{
					activeChar.sendMessage("Usage: //admin_block_char_packet <char_name> <packet_op_code1>,<packet_op_code2>");
					return false;
				}
				
				final L2PcInstance target = L2World.getInstance().getPlayer(charName_packet[0]);
				
				if (target != null)
				{
					PacketsLoggerManager.getInstance().blockCharacterPacket(target.getName(), charName_packet[1]);
					return true;
				}
				
				activeChar.sendPacket(new SystemMessage(SystemMessageId.CHARACTER_DOES_NOT_EXIST));
				return false;
			}
			case admin_restore_char_packet:
			{
				
				String val = "";
				
				if (st.hasMoreTokens())
				{
					
					while (st.hasMoreTokens())
					{
						if (val.equals(""))
						{
							val = st.nextToken();
						}
						else
						{
							val += " " + st.nextToken();
						}
					}
					
				}
				else
				{
					activeChar.sendMessage("Usage: //admin_restore_char_packet <char_name> <packet_op_code1>,<packet_op_code2>");
					return false;
				}
				
				final String[] charName_packet = val.split(" ");
				
				if (charName_packet.length < 2)
				{
					activeChar.sendMessage("Usage: //admin_restore_char_packet <char_name> <packet_op_code1>,<packet_op_code2>");
					return false;
				}
				
				final L2PcInstance target = L2World.getInstance().getPlayer(charName_packet[0]);
				
				if (target != null)
				{
					PacketsLoggerManager.getInstance().restoreCharacterPacket(target.getName(), charName_packet[1]);
					return true;
				}
				
				activeChar.sendPacket(new SystemMessage(SystemMessageId.CHARACTER_DOES_NOT_EXIST));
				return false;
			}
			case admin_start_monitor_char:
			{
				
				String val = "";
				
				if (st.hasMoreTokens())
				{
					
					while (st.hasMoreTokens())
					{
						if (val.equals(""))
						{
							val = st.nextToken();
						}
						else
						{
							val += " " + st.nextToken();
						}
					}
					
				}
				else
				{
					activeChar.sendMessage("Usage: //start_monitor_char <char_name>");
					return false;
				}
				
				final L2PcInstance target = L2World.getInstance().getPlayer(val.trim());
				
				if (target != null)
				{
					PacketsLoggerManager.getInstance().startCharacterPacketsMonitoring(target.getName());
					return true;
				}
				
				activeChar.sendPacket(new SystemMessage(SystemMessageId.CHARACTER_DOES_NOT_EXIST));
				return false;
			}
			case admin_stop_monitor_char:
			{
				
				String val = "";
				
				if (st.hasMoreTokens())
				{
					
					while (st.hasMoreTokens())
					{
						if (val.equals(""))
						{
							val = st.nextToken();
						}
						else
						{
							val += " " + st.nextToken();
						}
					}
					
				}
				else
				{
					activeChar.sendMessage("Usage: //stop_monitor_char <char_name>");
					return false;
				}
				
				final L2PcInstance target = L2World.getInstance().getPlayer(val.trim());
				
				if (target != null)
				{
					PacketsLoggerManager.getInstance().stopCharacterPacketsMonitoring(target.getName());
					return true;
				}
				
				activeChar.sendPacket(new SystemMessage(SystemMessageId.CHARACTER_DOES_NOT_EXIST));
				return false;
				
			}
			
		}
		
		return false;
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}
