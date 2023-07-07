package l2jorion.game.handler.admin;

import java.util.StringTokenizer;

import l2jorion.Config;
import l2jorion.game.datatables.GmListTable;
import l2jorion.game.datatables.xml.AugmentationScrollData;
import l2jorion.game.handler.IAdminCommandHandler;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.olympiad.Olympiad;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.EtcStatusUpdate;
import l2jorion.game.network.serverpackets.SystemMessage;
import l2jorion.log.Log;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;

public class AdminAdmin implements IAdminCommandHandler
{
	private static Logger LOG = LoggerFactory.getLogger(AdminAdmin.class);
	
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
		"admin_loc2",
		"admin_loc3",
		"admin_loc4"
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
		admin_loc2,
		admin_loc3,
		admin_loc4
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
					activeChar.setMessageRefusal(false);
					activeChar.sendPacket(new SystemMessage(SystemMessageId.MESSAGE_ACCEPTANCE_MODE));
					activeChar.sendPacket(new EtcStatusUpdate(activeChar));
				}
				else
				{
					activeChar.setMessageRefusal(true);
					activeChar.sendPacket(new SystemMessage(SystemMessageId.MESSAGE_REFUSAL_MODE));
					activeChar.sendPacket(new EtcStatusUpdate(activeChar));
				}
				return true;
			case admin_trade:
				if (activeChar.getTradeRefusal())
				{
					activeChar.setTradeRefusal(false);
					activeChar.sendMessage("Trade on.");
				}
				else
				{
					activeChar.setTradeRefusal(true);
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
				
				activeChar.refreshOverloaded();
				return true;
				
			}
			case admin_augment:
				AugmentationScrollData.getInstance().reload();
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
					activeChar.sendMessage("Usage: //set parameter = value");
					return false;
				}
				return true;
			case admin_loc:
			{
				activeChar.sendMessage("Loc added to file: game/AdminlocLog.txt");
				final String text = "<node X=\"" + activeChar.getX() + "\" Y=\"" + activeChar.getY() + "\" />";
				Log.addLocLog(text, "AdminlocLog");
				return true;
			}
			case admin_loc2:
			{
				activeChar.sendMessage("Loc added to file: game/AdminlocLog.txt");
				final String text = activeChar.getX() + "," + activeChar.getY() + "," + activeChar.getZ() + ";";
				Log.addLocLog(text, "AdminlocLog");
				return true;
			}
			case admin_loc3:
			{
				activeChar.sendMessage("Loc added to file: game/AdminlocLog.txt");
				final String text = "<node X=\"" + activeChar.getX() + "\" Y=\"" + activeChar.getY() + "\" Z=\"" + activeChar.getZ() + "\" iterations=\"20\"/>";
				Log.addLocLog(text, "AdminlocLog");
				return true;
			}
			case admin_loc4:
			{
				activeChar.sendMessage("Loc added to file: game/AdminlocLog.txt");
				final String text = "<node X=\"" + activeChar.getX() + "\" Y=\"" + activeChar.getY() + "\" Z=\"" + activeChar.getZ() + "\"/>";
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