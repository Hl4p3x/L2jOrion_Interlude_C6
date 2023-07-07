package l2jorion.game.community.manager;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.StringTokenizer;

import l2jorion.game.cache.HtmCache;
import l2jorion.game.community.CommunityBoardManager;
import l2jorion.game.datatables.sql.ClanTable;
import l2jorion.game.handler.ICommunityBoardHandler;
import l2jorion.game.managers.CastleManager;
import l2jorion.game.managers.ClanHallManager;
import l2jorion.game.model.L2Clan;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.entity.ClanHall;
import l2jorion.game.model.entity.siege.Castle;
import l2jorion.util.StringUtil;

public class RegionBBSManager extends BaseBBSManager implements ICommunityBoardHandler
{
	@Override
	public void parseCmd(String command, L2PcInstance player)
	{
		if (command.equals("_bbsloc"))
		{
			CommunityBoardManager.getInstance().addBypass(player, "Region", command);
			
			showRegionsList(player);
		}
		else if (command.startsWith("_bbsloc"))
		{
			final StringTokenizer st = new StringTokenizer(command, ";");
			st.nextToken();
			
			showRegion(player, Integer.parseInt(st.nextToken()));
		}
		else
		{
			super.parseCmd(command, player);
		}
	}
	
	@Override
	protected String getFolder()
	{
		return "region/";
	}
	
	private static void showRegionsList(L2PcInstance player)
	{
		final String content = HtmCache.getInstance().getHtm(CB_PATH + "region/castlelist.htm");
		
		final StringBuilder sb = new StringBuilder(500);
		for (Castle castle : CastleManager.getInstance().getCastles())
		{
			final L2Clan owner = ClanTable.getInstance().getClan(castle.getOwnerId());
			
			StringUtil.append(sb, "<table><tr><td width=5></td><td width=175><a action=\"bypass _bbsloc;", castle.getCastleId(), "\">", castle.getName(), "</a></td><td width=160>", ((owner != null) ? "<a action=\"bypass _bbsclan;home;" + owner.getClanId() + "\">" + owner.getName()
				+ "</a>" : "None"), "</td><td width=160>", ((owner != null
					&& owner.getAllyId() > 0) ? owner.getAllyName() : "None"), "</td><td width=120>", ((owner != null) ? castle.getTaxPercent() : "0"), "</td><td width=5></td></tr></table><br1><img src=\"L2UI.Squaregray\" width=627 height=1><br1>");
		}
		separateAndSend(content.replace("%castleList%", sb.toString()), player);
	}
	
	private static void showRegion(L2PcInstance player, int castleId)
	{
		final Castle castle = CastleManager.getInstance().getCastleById(castleId);
		final L2Clan owner = ClanTable.getInstance().getClan(castle.getOwnerId());
		
		String content = HtmCache.getInstance().getHtm(CB_PATH + "region/castle.htm");
		
		content = content.replace("%castleName%", castle.getName());
		content = content.replace("%tax%", Integer.toString(castle.getTaxPercent()));
		content = content.replace("%lord%", ((owner != null) ? owner.getLeaderName() : "None"));
		content = content.replace("%clanName%", ((owner != null) ? "<a action=\"bypass _bbsclan;home;" + owner.getClanId() + "\">" + owner.getName() + "</a>" : "None"));
		content = content.replace("%allyName%", ((owner != null && owner.getAllyId() > 0) ? owner.getAllyName() : "None"));
		content = content.replace("%siegeDate%", new SimpleDateFormat("yyyy-MM-dd HH:mm").format(castle.getSiegeDate().getTimeInMillis()));
		
		final StringBuilder sb = new StringBuilder(200);
		
		final List<ClanHall> clanHalls = ClanHallManager.getInstance().getClanHallsByLocation(castle.getName());
		if (clanHalls != null && !clanHalls.isEmpty())
		{
			sb.append("<br><br><table width=627 bgcolor=111111><tr><td width=5></td><td width=200>Clan Hall Name</td><td width=200>Owning Clan</td><td width=200>Clan Leader Name</td><td width=5></td></tr></table><br1>");
			
			for (ClanHall ch : clanHalls)
			{
				final L2Clan chOwner = ClanTable.getInstance().getClan(ch.getOwnerId());
				
				StringUtil.append(sb, "<table><tr><td width=5></td><td width=200>", ch.getName(), "</td><td width=200>", ((chOwner != null) ? "<a action=\"bypass _bbsclan;home;" + chOwner.getClanId() + "\">" + chOwner.getName()
					+ "</a>" : "None"), "</td><td width=200>", ((chOwner != null) ? chOwner.getLeaderName() : "None"), "</td><td width=5></td></tr></table><br1><img src=\"L2UI.Squaregray\" width=627 height=1><br1>");
			}
		}
		separateAndSend(content.replace("%hallsList%", sb.toString()), player);
	}
	
	public static RegionBBSManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final RegionBBSManager INSTANCE = new RegionBBSManager();
	}
	
	@Override
	public String[] getBypassBbsCommands()
	{
		return new String[]
		{
			"_bbsloc"
		};
	}
	
	@Override
	public void handleCommand(String command, L2PcInstance player, String params)
	{
		parseCmd(command, player);
	}
}