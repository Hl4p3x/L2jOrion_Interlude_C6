package l2jorion.game.handler.admin;

import java.util.StringTokenizer;

import l2jorion.Config;
import l2jorion.game.datatables.GmListTable;
import l2jorion.game.handler.AutoAnnouncementHandler;
import l2jorion.game.handler.IAdminCommandHandler;
import l2jorion.game.model.L2World;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.entity.Announcements;
import l2jorion.game.network.clientpackets.Say2;
import l2jorion.game.network.serverpackets.CreatureSay;
import l2jorion.game.util.Broadcast;

public class AdminAnnouncements implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_list_announcements",
		"admin_reload_announcements",
		"admin_announce_announcements",
		"admin_add_announcement",
		"admin_del_announcement",
		"admin_announce",
		"admin_gmchat2",
		"admin_critannounce",
		"admin_announce_menu",
		"admin_list_autoannouncements",
		"admin_add_autoannouncement",
		"admin_del_autoannouncement",
		"admin_autoannounce"
	};
	
	private enum CommandEnum
	{
		admin_list_announcements,
		admin_reload_announcements,
		admin_announce_announcements,
		admin_add_announcement,
		admin_del_announcement,
		admin_announce,
		admin_gmchat2,
		admin_critannounce,
		admin_announce_menu,
		admin_list_autoannouncements,
		admin_add_autoannouncement,
		admin_del_autoannouncement,
		admin_autoannounce
	}
	
	@Override
	public boolean useAdminCommand(String command, L2PcInstance activeChar)
	{
		StringTokenizer st = new StringTokenizer(command);
		
		String comm_s = st.nextToken();
		String text = "";
		int index = 0;
		
		CommandEnum comm = CommandEnum.valueOf(comm_s);
		
		if (comm == null)
		{
			return false;
		}
		
		switch (comm)
		{
			case admin_list_announcements:
				Announcements.getInstance().listAnnouncements(activeChar);
				return true;
			case admin_reload_announcements:
				Announcements.getInstance().loadAnnouncements();
				Announcements.getInstance().listAnnouncements(activeChar);
				return true;
			case admin_announce_menu:
				
				if (st.hasMoreTokens())
				{
					text = command.replace(comm_s + " ", "");
				}
				
				if (!text.equals(""))
				{
					Announcements.getInstance().gameAnnounceToAll(text);
				}
				Announcements.getInstance().listAnnouncements(activeChar);
				return true;
			case admin_announce_announcements:
				for (L2PcInstance player : L2World.getInstance().getAllPlayers().values())
				{
					if (Config.ANNOUNCE_NEW_STYLE)
					{
						Announcements.getInstance().showAnnouncementsNewStyle(player);
					}
					else
					{
						Announcements.getInstance().showAnnouncements(player);
					}
				}
				
				Announcements.getInstance().listAnnouncements(activeChar);
				return true;
			case admin_add_announcement:
				
				if (st.hasMoreTokens())
				{
					text = command.replace(comm_s + " ", "");
				}
				
				if (!text.equals(""))
				{
					Announcements.getInstance().addAnnouncement(text);
					Announcements.getInstance().listAnnouncements(activeChar);
					return true;
				}
				activeChar.sendMessage("You cannot announce Empty message");
				return false;
			
			case admin_del_announcement:
				
				if (st.hasMoreTokens())
				{
					String index_s = st.nextToken();
					
					try
					{
						index = Integer.parseInt(index_s);
					}
					catch (NumberFormatException e)
					{
						activeChar.sendMessage("Usage: //del_announcement <index> (number >=0)");
					}
				}
				
				if (index >= 0)
				{
					Announcements.getInstance().delAnnouncement(index);
					Announcements.getInstance().listAnnouncements(activeChar);
					return true;
				}
				activeChar.sendMessage("Usage: //del_announcement <index> (number >=0)");
				return false;
			case admin_announce:
				if (st.hasMoreTokens())
				{
					
					text = command.replace(comm_s + " ", "");
					// text = st.nextToken();
				}
				
				if (!text.equals(""))
				{
					Announcements.getInstance().adminMsg(text, activeChar);
				}
				Announcements.getInstance().listAnnouncements(activeChar);
				return true;
			case admin_gmchat2:
				int offset = 0;
				
				String text4;
				if (command.contains("menu"))
				{
					offset = 17;
				}
				else
				{
					offset = 13;
				}
				
				text4 = command.substring(offset);
				CreatureSay cs4 = new CreatureSay(0, 9, activeChar.getName(), text4);
				GmListTable.broadcastToGMs(cs4);
				Announcements.getInstance().listAnnouncements(activeChar);
				return true;
			case admin_critannounce:
				String text1 = command.substring(19);
				if (Config.GM_CRITANNOUNCER_NAME && text1.length() > 0)
				{
					text1 = activeChar.getName() + ": " + text1;
				}
				
				CreatureSay cs = new CreatureSay(activeChar.getObjectId(), Say2.CRITICAL_ANNOUNCE, "", "--------------------------------------------------------------------------------");
				CreatureSay cs1 = new CreatureSay(activeChar.getObjectId(), Say2.CRITICAL_ANNOUNCE, "", text1);
				CreatureSay cs3 = new CreatureSay(activeChar.getObjectId(), Say2.CRITICAL_ANNOUNCE, "", "--------------------------------------------------------------------------------");
				Broadcast.toAllOnlinePlayers(cs);
				Broadcast.toAllOnlinePlayers(cs1);
				Broadcast.toAllOnlinePlayers(cs3);
				Announcements.getInstance().listAnnouncements(activeChar);
				return true;
			
			case admin_list_autoannouncements:
				AutoAnnouncementHandler.getInstance().listAutoAnnouncements(activeChar);
				return true;
			
			case admin_add_autoannouncement:
				
				if (st.hasMoreTokens())
				{
					
					int delay = 0;
					
					try
					{
						delay = Integer.parseInt(st.nextToken().trim());
						
					}
					catch (NumberFormatException e)
					{
						
						activeChar.sendMessage("Usage: //add_autoannouncement <delay> (Seconds > 30) <Announcements>");
						return false;
						
					}
					
					if (st.hasMoreTokens())
					{
						
						String autoAnnounce = st.nextToken();
						
						if (delay > 30)
						{
							while (st.hasMoreTokens())
							{
								autoAnnounce = autoAnnounce + " " + st.nextToken();
							}
							
							AutoAnnouncementHandler.getInstance().registerAnnouncment(autoAnnounce, delay);
							AutoAnnouncementHandler.getInstance().listAutoAnnouncements(activeChar);
							
							return true;
							
						}
						activeChar.sendMessage("Usage: //add_autoannouncement <delay> (Seconds > 30) <Announcements>");
						return false;
					}
					activeChar.sendMessage("Usage: //add_autoannouncement <delay> (Seconds > 30) <Announcements>");
					return false;
				}
				activeChar.sendMessage("Usage: //add_autoannouncement <delay> (Seconds > 30) <Announcements>");
				return false;
			case admin_del_autoannouncement:
				if (st.hasMoreTokens())
				{
					
					try
					{
						index = Integer.parseInt(st.nextToken());
						
					}
					catch (NumberFormatException e)
					{
						
						activeChar.sendMessage("Usage: //del_autoannouncement <index> (number >= 0)");
						return false;
						
					}
					
					if (index >= 0)
					{
						
						AutoAnnouncementHandler.getInstance().removeAnnouncement(index);
						AutoAnnouncementHandler.getInstance().listAutoAnnouncements(activeChar);
						
					}
					else
					{
						activeChar.sendMessage("Usage: //del_autoannouncement <index> (number >= 0)");
						return false;
						
					}
				}
				else
				{
					activeChar.sendMessage("Usage: //del_autoannouncement <index> (number >= 0)");
					return false;
				}
			case admin_autoannounce:
				AutoAnnouncementHandler.getInstance().listAutoAnnouncements(activeChar);
				return true;
		}
		
		comm = null;
		return false;
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}