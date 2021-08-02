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
 * [URL]http://www.gnu.org/copyleft/gpl.html[/URL]
 */
package l2jorion.game.handler.admin;

import java.util.StringTokenizer;

import l2jorion.Config;
import l2jorion.game.datatables.GmListTable;
import l2jorion.game.datatables.csv.MapRegionTable;
import l2jorion.game.datatables.xml.AugmentScrollData;
import l2jorion.game.handler.IAdminCommandHandler;
import l2jorion.game.managers.ZoneManager;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.base.Race;
import l2jorion.game.model.olympiad.Olympiad;
import l2jorion.game.model.zone.type.L2RespawnZone;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.SystemMessage;
import l2jorion.log.Log;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;

public class AdminAdmin implements IAdminCommandHandler
{
	private static Logger LOG = LoggerFactory.getLogger(AdminAdmin.class.getName());
	
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_admin",
		"admin_admin1",
		"admin_admin2",
		"admin_admin3",
		"admin_admin4",
		"admin_admin5",
		"admin_gmliston",
		"admin_gmlistoff",
		"admin_silence",
		"admin_diet",
		"admin_set",
		"admin_set_menu",
		"admin_set_mod",
		"admin_saveolymp",
		"admin_trade",
		"admin_manualhero",
		"admin_augment",
		"admin_loc",
		"admin_loc2"
	};
	
	private enum CommandEnum
	{
		admin_admin,
		admin_admin1,
		admin_admin2,
		admin_admin3,
		admin_admin4,
		admin_admin5,
		admin_gmliston,
		admin_gmlistoff,
		admin_silence,
		admin_diet,
		admin_set,
		admin_set_menu,
		admin_set_mod,
		admin_saveolymp,
		admin_trade,
		admin_manualhero,
		admin_augment,
		admin_loc,
		admin_loc2
	}
	
	@Override
	public boolean useAdminCommand(String command, L2PcInstance activeChar)
	{
		StringTokenizer st = new StringTokenizer(command);
		
		CommandEnum comm = CommandEnum.valueOf(st.nextToken());
		
		if (comm == null)
		{
			return false;
		}
		
		switch (comm)
		{
			case admin_admin:
			case admin_admin1:
			case admin_admin2:
			case admin_admin3:
			case admin_admin4:
			case admin_admin5:
				showMainPage(activeChar, command);
				return true;
			
			case admin_gmliston:
				GmListTable.getInstance().showGm(activeChar);
				activeChar.sendMessage("Registerd into gm list.");
				return true;
			
			case admin_gmlistoff:
				GmListTable.getInstance().hideGm(activeChar);
				activeChar.sendMessage("Removed from gm list.");
				return true;
			
			case admin_silence:
				if (activeChar.getMessageRefusal()) // already in message refusal mode
				{
					activeChar.setMessageRefusal(1);
					activeChar.sendPacket(new SystemMessage(SystemMessageId.MESSAGE_ACCEPTANCE_MODE));
				}
				else
				{
					activeChar.setMessageRefusal(0);
					activeChar.sendPacket(new SystemMessage(SystemMessageId.MESSAGE_REFUSAL_MODE));
				}
				return true;
			case admin_trade:
				if (activeChar.getTradeRefusal())
				{
					activeChar.setTradeRefusal(1);
					activeChar.sendMessage("Trade on.");
				}
				else
				{
					activeChar.setTradeRefusal(0);
					activeChar.sendMessage("Trade off.");
				}
				return true;
			case admin_saveolymp:
				
				Olympiad.getInstance().saveOlympiadStatus();
				activeChar.sendMessage("Olympiad stuff saved!");
				
				return true;
			case admin_manualhero:
				try
				{
					Olympiad.getInstance().manualSelectHeroes();
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
				
				activeChar.sendMessage("Heroes formed!");
				return true;
			case admin_diet:
			{
				
				boolean no_token = false;
				
				if (st.hasMoreTokens())
				{
					
					if (st.nextToken().equalsIgnoreCase("on"))
					{
						activeChar.setDietMode(true);
						activeChar.sendMessage("Diet mode on");
					}
					else if (st.nextToken().equalsIgnoreCase("off"))
					{
						activeChar.setDietMode(false);
						activeChar.sendMessage("Diet mode off");
					}
					
				}
				else
				{
					
					no_token = true;
					
				}
				
				if (no_token)
				{
					
					if (activeChar.getDietMode())
					{
						activeChar.setDietMode(false);
						activeChar.sendMessage("Diet mode off");
					}
					else
					{
						activeChar.setDietMode(true);
						activeChar.sendMessage("Diet mode on");
					}
					
				}
				
				st = null;
				activeChar.refreshOverloaded();
				return true;
				
			}
			case admin_augment:
				AugmentScrollData.getInstance().reload();
				activeChar.sendMessage("Augment scroll data has been reloaded.");
				return true;
			case admin_set:
				
				boolean no_token = false;
				
				String[] cmd = st.nextToken().split("_");
				
				if (cmd != null && cmd.length > 1)
				{
					if (st.hasMoreTokens())
					{
						
						String[] parameter = st.nextToken().split("=");
						
						if (parameter.length > 1)
						{
							
							String pName = parameter[0].trim();
							String pValue = parameter[1].trim();
							
							if (Config.setParameterValue(pName, pValue))
							{
								activeChar.sendMessage("parameter " + pName + " succesfully set to " + pValue);
							}
							else
							{
								activeChar.sendMessage("Invalid parameter!");
								no_token = true;
							}
							
						}
						else
						{
							no_token = true;
						}
					}
					
					if (cmd.length == 3)
					{
						if (cmd[2].equalsIgnoreCase("menu"))
						{
							AdminHelpPage.showHelpPage(activeChar, "settings.htm");
						}
						else if (cmd[2].equalsIgnoreCase("mod"))
						{
							AdminHelpPage.showHelpPage(activeChar, "mods_menu.htm");
						}
					}
					
				}
				else
				{
					no_token = true;
				}
				
				if (no_token)
				{
					activeChar.sendMessage("Usage: //set parameter=vaue");
					return false;
				}
				return true;
			case admin_loc:
			{
				int region;
				L2RespawnZone zone = ZoneManager.getInstance().getZone(activeChar, L2RespawnZone.class);
				if (zone != null)
				{
					region = MapRegionTable.getInstance().getRestartRegion(activeChar, zone.getAllRespawnPoints().get(Race.human)).getLocId();
				}
				else
				{
					region = MapRegionTable.getInstance().getMapRegionLocId(activeChar);
				}
				
				SystemMessageId msg;
				
				switch (region)
				{
					case 910:
						msg = SystemMessageId.LOC_TI_S1_S2_S3;
						break;
					case 911:
						msg = SystemMessageId.LOC_GLUDIN_S1_S2_S3;
						break;
					case 912:
						msg = SystemMessageId.LOC_GLUDIO_S1_S2_S3;
						break;
					case 914:
						msg = SystemMessageId.LOC_ELVEN_S1_S2_S3;
						break;
					case 915:
						msg = SystemMessageId.LOC_DARK_ELVEN_S1_S2_S3;
						break;
					case 916:
						msg = SystemMessageId.LOC_DION_S1_S2_S3;
						break;
					case 917:
						msg = SystemMessageId.LOC_FLORAN_S1_S2_S3;
						break;
					case 918:
						msg = SystemMessageId.LOC_GIRAN_S1_S2_S3;
						break;
					case 919:
						msg = SystemMessageId.LOC_GIRAN_HARBOR_S1_S2_S3;
						break;
					case 920:
						msg = SystemMessageId.LOC_ORC_S1_S2_S3;
						break;
					case 921:
						msg = SystemMessageId.LOC_DWARVEN_S1_S2_S3;
						break;
					case 922:
						msg = SystemMessageId.LOC_OREN_S1_S2_S3;
						break;
					case 923:
						msg = SystemMessageId.LOC_HUNTER_S1_S2_S3;
						break;
					case 924:
						msg = SystemMessageId.LOC_ADEN_S1_S2_S3;
						break;
					case 926:
						msg = SystemMessageId.LOC_HEINE_S1_S2_S3;
						break;
					case 1537:
						msg = SystemMessageId.LOC_RUNE_S1_S2_S3;
						break;
					case 1538:
						msg = SystemMessageId.LOC_GODDARD_S1_S2_S3;
						break;
					case 1714:
						msg = SystemMessageId.LOC_SCHUTTGART_S1_S2_S3;
						break;
					case 1924:
						msg = SystemMessageId.LOC_PRIMEVAL_ISLE_S1_S2_S3;
						break;
					default:
						msg = SystemMessageId.LOC_ADEN_S1_S2_S3;
				}
				
				SystemMessage sm = new SystemMessage(msg);
				sm.addNumber(activeChar.getX());
				sm.addNumber(activeChar.getY());
				sm.addNumber(activeChar.getZ());
				activeChar.sendPacket(sm);
				
				activeChar.sendMessage("Added to file.");
				final String text = "<node X=\"" + activeChar.getX() + "\" Y=\"" + activeChar.getY() + "\" />";
				Log.addLocLog(text, "AdminlocLog");
				return true;
			}
			case admin_loc2:
			{
				int region;
				L2RespawnZone zone = ZoneManager.getInstance().getZone(activeChar, L2RespawnZone.class);
				if (zone != null)
				{
					region = MapRegionTable.getInstance().getRestartRegion(activeChar, zone.getAllRespawnPoints().get(Race.human)).getLocId();
				}
				else
				{
					region = MapRegionTable.getInstance().getMapRegionLocId(activeChar);
				}
				
				SystemMessageId msg;
				
				switch (region)
				{
					case 910:
						msg = SystemMessageId.LOC_TI_S1_S2_S3;
						break;
					case 911:
						msg = SystemMessageId.LOC_GLUDIN_S1_S2_S3;
						break;
					case 912:
						msg = SystemMessageId.LOC_GLUDIO_S1_S2_S3;
						break;
					case 914:
						msg = SystemMessageId.LOC_ELVEN_S1_S2_S3;
						break;
					case 915:
						msg = SystemMessageId.LOC_DARK_ELVEN_S1_S2_S3;
						break;
					case 916:
						msg = SystemMessageId.LOC_DION_S1_S2_S3;
						break;
					case 917:
						msg = SystemMessageId.LOC_FLORAN_S1_S2_S3;
						break;
					case 918:
						msg = SystemMessageId.LOC_GIRAN_S1_S2_S3;
						break;
					case 919:
						msg = SystemMessageId.LOC_GIRAN_HARBOR_S1_S2_S3;
						break;
					case 920:
						msg = SystemMessageId.LOC_ORC_S1_S2_S3;
						break;
					case 921:
						msg = SystemMessageId.LOC_DWARVEN_S1_S2_S3;
						break;
					case 922:
						msg = SystemMessageId.LOC_OREN_S1_S2_S3;
						break;
					case 923:
						msg = SystemMessageId.LOC_HUNTER_S1_S2_S3;
						break;
					case 924:
						msg = SystemMessageId.LOC_ADEN_S1_S2_S3;
						break;
					case 926:
						msg = SystemMessageId.LOC_HEINE_S1_S2_S3;
						break;
					case 1537:
						msg = SystemMessageId.LOC_RUNE_S1_S2_S3;
						break;
					case 1538:
						msg = SystemMessageId.LOC_GODDARD_S1_S2_S3;
						break;
					case 1714:
						msg = SystemMessageId.LOC_SCHUTTGART_S1_S2_S3;
						break;
					case 1924:
						msg = SystemMessageId.LOC_PRIMEVAL_ISLE_S1_S2_S3;
						break;
					default:
						msg = SystemMessageId.LOC_ADEN_S1_S2_S3;
				}
				
				SystemMessage sm = new SystemMessage(msg);
				sm.addNumber(activeChar.getX());
				sm.addNumber(activeChar.getY());
				sm.addNumber(activeChar.getZ());
				activeChar.sendPacket(sm);
				
				activeChar.sendMessage("Added to file.");
				final String text = activeChar.getX() + "," + activeChar.getY() + "," + activeChar.getZ() + ";";
				Log.addLocLog(text, "AdminlocLog");
				return true;
			}
			default:
			{
				return false;
			}
		}
		
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
	
	private void showMainPage(L2PcInstance activeChar, String command)
	{
		int mode = 0;
		String filename = null;
		
		if (command != null && command.length() > 11)
		{
			String mode_s = command.substring(11);
			try
			{
				mode = Integer.parseInt(mode_s);
				
			}
			catch (NumberFormatException e)
			{
				if (Config.DEBUG)
				{
					LOG.warn("Impossible to parse to integer the string " + mode_s + ", exception " + e.getMessage());
				}
			}
		}
		
		switch (mode)
		{
			case 1:
				filename = "main";
				break;
			case 2:
				filename = "game";
				break;
			case 3:
				filename = "effects";
				break;
			case 4:
				filename = "server";
				break;
			case 5:
				filename = "mods";
				break;
			default:
				if (Config.GM_ADMIN_MENU_STYLE.equals("modern"))
				{
					filename = "main";
				}
				else
				{
					filename = "classic";
				}
				break;
		}
		
		AdminHelpPage.showHelpPage(activeChar, filename + "_menu.htm");
	}
}