package l2jorion.game.community.manager;

import java.util.StringTokenizer;

import l2jorion.game.cache.HtmCache;
import l2jorion.game.community.CommunityBoardManager;
import l2jorion.game.datatables.sql.ClanTable;
import l2jorion.game.handler.ICommunityBoardHandler;
import l2jorion.game.model.L2Clan;
import l2jorion.game.model.L2ClanMember;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.network.SystemMessageId;
import l2jorion.util.StringUtil;

public class ClanBBSManager extends BaseBBSManager implements ICommunityBoardHandler
{
	@Override
	public void parseCmd(String command, L2PcInstance player)
	{
		if (command.equalsIgnoreCase("_bbsclan"))
		{
			CommunityBoardManager.getInstance().addBypass(player, "Clan", command);
			
			if (player.getClan() == null)
			{
				sendClanList(player, 1, "", "");
			}
			else
			{
				sendClanDetails(player, player.getClan().getClanId());
			}
		}
		else if (command.startsWith("_bbsclan"))
		{
			final StringTokenizer st = new StringTokenizer(command, ";");
			st.nextToken();
			
			final String clanCommand = st.nextToken();
			if (clanCommand.equalsIgnoreCase("clan"))
			{
				CommunityBoardManager.getInstance().addBypass(player, "Clans List", command);
				
				sendClanList(player, Integer.parseInt(st.nextToken()), "", "");
			}
			else if (clanCommand.equalsIgnoreCase("home"))
			{
				CommunityBoardManager.getInstance().addBypass(player, "Clan Home", command);
				
				sendClanDetails(player, Integer.parseInt(st.nextToken()));
			}
			else if (clanCommand.equalsIgnoreCase("announce"))
			{
				sendClanAnnounce(player, Integer.parseInt(st.nextToken()));
			}
			else if (clanCommand.equalsIgnoreCase("mail"))
			{
				CommunityBoardManager.getInstance().addBypass(player, "Clan Mail", command);
				
				sendClanMail(player, Integer.parseInt(st.nextToken()));
			}
			else if (clanCommand.equalsIgnoreCase("management"))
			{
				CommunityBoardManager.getInstance().addBypass(player, "Clan Management", command);
				
				sendClanManagement(player, Integer.parseInt(st.nextToken()));
			}
			else if (clanCommand.equalsIgnoreCase("notice"))
			{
				CommunityBoardManager.getInstance().addBypass(player, "Clan Notice", command);
				
				if (st.hasMoreTokens())
				{
					final String noticeCommand = st.nextToken();
					if (!noticeCommand.isEmpty() && player.getClan() != null)
					{
						player.getClan().setNoticeEnabledAndStore(Boolean.parseBoolean(noticeCommand));
					}
				}
				sendClanNotice(player, player.getClanId());
			}
		}
		else
		{
			super.parseCmd(command, player);
		}
	}
	
	@Override
	public void parseWrite(String ar1, String ar2, String ar3, String ar4, String ar5, L2PcInstance player)
	{
		if (ar1.equalsIgnoreCase("intro"))
		{
			if (Integer.valueOf(ar2) != player.getClanId())
			{
				return;
			}
			
			final L2Clan clan = ClanTable.getInstance().getClan(player.getClanId());
			if (clan == null)
			{
				return;
			}
			
			clan.setIntroduction(ar3, true);
			sendClanManagement(player, Integer.valueOf(ar2));
		}
		else if (ar1.startsWith("Search"))
		{
			final StringTokenizer st = new StringTokenizer(ar1, ";");
			st.nextToken();
			
			sendClanList(player, 1, ar4, ar5);
		}
		else if (ar1.equals("notice"))
		{
			final L2Clan clan = player.getClan();
			if (clan != null)
			{
				clan.setNoticeAndStore(ar4);
				sendClanNotice(player, player.getClanId());
			}
		}
		else if (ar1.equalsIgnoreCase("mail"))
		{
			if (Integer.valueOf(ar2) != player.getClanId())
			{
				return;
			}
			
			final L2Clan clan = ClanTable.getInstance().getClan(player.getClanId());
			if (clan == null)
			{
				return;
			}
			
			// Retrieve clans members, and store them under a String.
			final StringBuilder members = new StringBuilder();
			
			for (L2ClanMember member : clan.getMembers())
			{
				if (members.length() > 0)
				{
					members.append(";");
				}
				
				members.append(member.getName());
			}
			MailBBSManager.getInstance().sendMail(members.toString(), ar4, ar5, player);
			sendClanDetails(player, player.getClanId());
		}
		else
		{
			super.parseWrite(ar1, ar2, ar3, ar4, ar5, player);
		}
	}
	
	@Override
	protected String getFolder()
	{
		return "clan/";
	}
	
	private static void sendClanMail(L2PcInstance player, int clanId)
	{
		final L2Clan clan = ClanTable.getInstance().getClan(clanId);
		if (clan == null)
		{
			return;
		}
		
		if (player.getClanId() != clanId || !player.isClanLeader())
		{
			player.sendPacket(SystemMessageId.ONLY_THE_CLAN_LEADER_IS_ENABLED);
			sendClanList(player, 1, "", "");
			return;
		}
		
		String content = HtmCache.getInstance().getHtm(CB_PATH + "clan/clanhome-mail.htm");
		content = content.replaceAll("%clanid%", Integer.toString(clanId));
		content = content.replaceAll("%clanName%", clan.getName());
		separateAndSend(content, player);
	}
	
	private static void sendClanAnnounce(L2PcInstance player, int clanId)
	{
		final L2Clan clan = ClanTable.getInstance().getClan(clanId);
		if (clan == null)
		{
			return;
		}
		
		String content = HtmCache.getInstance().getHtm(CB_PATH + "clan/clanhome-announcement.htm");
		content = content.replaceAll("%clanid%", Integer.toString(clanId));
		content = content.replace("%clan_name%", clan.getName());
		content = content.replace("%notice_text%", clan.getNotice().replaceAll("\r\n", "<br>").replaceAll("action", "").replaceAll("bypass", ""));
		
		send1001(content, player);
		send1002(player, clan.getIntroduction(), "", "");
	}
	
	private static void sendClanManagement(L2PcInstance player, int clanId)
	{
		final L2Clan clan = ClanTable.getInstance().getClan(clanId);
		if (clan == null)
		{
			return;
		}
		
		if (player.getClanId() != clanId || !player.isClanLeader())
		{
			player.sendPacket(SystemMessageId.ONLY_THE_CLAN_LEADER_IS_ENABLED);
			sendClanList(player, 1, "", "");
			return;
		}
		
		String content = HtmCache.getInstance().getHtm(CB_PATH + "clan/clanhome-management.htm");
		content = content.replaceAll("%clanid%", Integer.toString(clan.getClanId()));
		send1001(content, player);
		send1002(player, clan.getIntroduction(), "", "");
	}
	
	private static void sendClanNotice(L2PcInstance player, int clanId)
	{
		final L2Clan clan = ClanTable.getInstance().getClan(clanId);
		if (clan == null || player.getClanId() != clanId)
		{
			return;
		}
		
		if (clan.getLevel() < 2)
		{
			player.sendPacket(SystemMessageId.NO_CB_IN_MY_CLAN);
			sendClanList(player, 1, "", "");
			return;
		}
		
		String content = HtmCache.getInstance().getHtm(CB_PATH + "clan/clanhome-notice.htm");
		content = content.replaceAll("%clanid%", Integer.toString(clan.getClanId()));
		content = content.replace("%enabled%", "" + String.valueOf(clan.isNoticeEnabled()) + "");
		content = content.replace("%flag%", String.valueOf(!clan.isNoticeEnabled()));
		send1001(content, player);
		send1002(player, clan.getNotice(), "", "");
	}
	
	private static void sendClanList(L2PcInstance player, int index, String type, String search)
	{
		boolean byName = type.equalsIgnoreCase("Name");
		
		String content = HtmCache.getInstance().getHtm(CB_PATH + "clan/clanlist.htm");
		final StringBuilder sb = new StringBuilder();
		
		final L2Clan clan = player.getClan();
		if (clan != null)
		{
			StringUtil.append(sb, "<table width=627><tr><td><a action=\"bypass _bbsclan;home;", clan.getClanId(), "\">[GO TO MY CLAN]</a></td></tr></table>");
		}
		
		content = content.replace("%homebar%", sb.toString());
		
		if (index < 1)
		{
			index = 1;
		}
		
		// Cleanup sb.
		sb.setLength(0);
		
		// List of clans.
		int searchPages = 0;
		int i = 0;
		for (L2Clan cl : ClanTable.getInstance().getClans())
		{
			if (i >= (index * 15))
			{
				break;
			}
			
			if (!search.equals(""))
			{
				if (byName)
				{
					if (cl.getName().toLowerCase().contains(search.toLowerCase()))
					{
						if (i++ >= ((index - 1) * 15))
						{
							searchPages++;
							StringUtil.append(sb, "<table width=627><tr><td width=5></td><td width=150 align=center><a action=\"bypass _bbsclan;home;", cl.getClanId(), "\">", cl.getName(), "</a></td><td width=150 align=center>", cl.getLeaderName(), "</td><td width=100 align=center>", cl.getLevel(), "</td><td width=200 align=center>", cl.getMembersCount(), "</td><td width=5></td></tr></table><br1><img src=\"L2UI.Squaregray\" width=627 height=1><br1>");
						}
					}
				}
				else
				{
					if (cl.getLeaderName().toLowerCase().contains(search.toLowerCase()))
					{
						if (i++ >= ((index - 1) * 15))
						{
							searchPages++;
							StringUtil.append(sb, "<table width=627><tr><td width=5></td><td width=150 align=center><a action=\"bypass _bbsclan;home;", cl.getClanId(), "\">", cl.getName(), "</a></td><td width=150 align=center>", cl.getLeaderName(), "</td><td width=100 align=center>", cl.getLevel(), "</td><td width=200 align=center>", cl.getMembersCount(), "</td><td width=5></td></tr></table><br1><img src=\"L2UI.Squaregray\" width=627 height=1><br1>");
						}
					}
				}
			}
			else
			{
				if (i++ >= ((index - 1) * 15))
				{
					StringUtil.append(sb, "<table width=627><tr><td width=5></td><td width=150 align=center><a action=\"bypass _bbsclan;home;", cl.getClanId(), "\">", cl.getName(), "</a></td><td width=150 align=center>", cl.getLeaderName(), "</td><td width=100 align=center>", cl.getLevel(), "</td><td width=200 align=center>", cl.getMembersCount(), "</td><td width=5></td></tr></table><br1><img src=\"L2UI.Squaregray\" width=627 height=1><br1>");
				}
			}
			
		}
		sb.append("<table><tr>");
		
		if (index == 1)
		{
			sb.append("<td><button action=\"\" back=\"l2ui_ch3.prev1_down\" fore=\"l2ui_ch3.prev1\" width=16 height=16></td>");
		}
		else
		{
			StringUtil.append(sb, "<td><button action=\"bypass _bbsclan;clan;", index - 1, "\" back=\"l2ui_ch3.prev1_down\" fore=\"l2ui_ch3.prev1\" width=16 height=16 ></td>");
		}
		
		int pageNumber = 1;
		
		if (!search.equals(""))
		{
			pageNumber = searchPages / 15;
			if (pageNumber * 15 != searchPages)
			{
				pageNumber++;
			}
		}
		else
		{
			pageNumber = ClanTable.getInstance().getClansCount() / 15;
			if (pageNumber * 15 != ClanTable.getInstance().getClansCount())
			{
				pageNumber++;
			}
		}
		
		for (i = 1; i <= pageNumber; i++)
		{
			if (i == index)
			{
				StringUtil.append(sb, "<td>", i, "&nbsp;</td>");
			}
			else
			{
				StringUtil.append(sb, "<td><a action=\"bypass _bbsclan;clan;", i, "\">", i, "</a>&nbsp;</td>");
			}
		}
		
		if (index == pageNumber)
		{
			sb.append("<td><button action=\"\" back=\"l2ui_ch3.next1_down\" fore=\"l2ui_ch3.next1\" width=16 height=16></td>");
		}
		else
		{
			StringUtil.append(sb, "<td><button action=\"bypass _bbsclan;clan;", index + 1, "\" back=\"l2ui_ch3.next1_down\" fore=\"l2ui_ch3.next1\" width=16 height=16 ></td>");
		}
		
		sb.append("</tr></table>");
		
		content = content.replace("%clanlist%", sb.toString());
		separateAndSend(content, player);
	}
	
	private static void sendClanDetails(L2PcInstance player, int clanId)
	{
		final L2Clan clan = ClanTable.getInstance().getClan(clanId);
		if (clan == null)
		{
			return;
		}
		
		if (clan.getLevel() < 2)
		{
			player.sendPacket(SystemMessageId.NO_CB_IN_MY_CLAN);
			sendClanList(player, 1, "", "");
			return;
		}
		
		String content;
		if (player.getClanId() != clanId)
		{
			content = HtmCache.getInstance().getHtm(CB_PATH + "clan/clanhome.htm");
		}
		else if (player.isClanLeader())
		{
			content = HtmCache.getInstance().getHtm(CB_PATH + "clan/clanhome-leader.htm");
		}
		else
		{
			content = HtmCache.getInstance().getHtm(CB_PATH + "clan/clanhome-member.htm");
		}
		
		content = content.replaceAll("%clanid%", Integer.toString(clan.getClanId()));
		content = content.replace("%clanIntro%", clan.getIntroduction());
		content = content.replace("%clanName%", clan.getName());
		content = content.replace("%clanLvL%", Integer.toString(clan.getLevel()));
		content = content.replace("%clanMembers%", Integer.toString(clan.getMembersCount()));
		content = content.replaceAll("%clanLeader%", clan.getLeaderName());
		content = content.replace("%allyName%", (clan.getAllyId() > 0) ? clan.getAllyName() : "");
		separateAndSend(content, player);
	}
	
	public static ClanBBSManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final ClanBBSManager INSTANCE = new ClanBBSManager();
	}
	
	@Override
	public String[] getBypassBbsCommands()
	{
		return new String[]
		{
			"_bbsclan"
		};
	}
	
	@Override
	public void handleCommand(String command, L2PcInstance player, String params)
	{
		parseCmd(command, player);
	}
}