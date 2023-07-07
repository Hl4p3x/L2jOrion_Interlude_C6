package l2jorion.game.community.manager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.StringTokenizer;

import l2jorion.game.cache.HtmCache;
import l2jorion.game.handler.ICommunityBoardHandler;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.util.Util;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;
import l2jorion.util.CloseUtil;
import l2jorion.util.database.L2DatabaseFactory;

public class RankBBSManager extends BaseBBSManager implements ICommunityBoardHandler
{
	protected static Logger LOG = LoggerFactory.getLogger(RankBBSManager.class);
	
	public static final int[] items =
	{
		6656,
		6657,
		6658,
		6659,
		6660,
		6661,
		6662,
		8191,
		11248,
		11242
	};
	
	private static final String GET_DATA = "SELECT characters.char_name, characters.accesslevel, characters.title, characters.obj_Id, characters.clanid," + //
		"items.owner_id, items.item_id, items.enchant_level," + //
		"armor.name," + //
		"clan_data.clan_id, clan_data.clan_name, clan_data.ally_id, clan_data.ally_name " + //
		"FROM characters " + //
		"INNER JOIN items ON characters.obj_Id = items.owner_id " + //
		"INNER JOIN armor ON items.item_id = armor.item_id " + //
		"LEFT JOIN clan_data ON characters.clanid = clan_data.clan_id " + //
		"WHERE items.item_id = ?  AND characters.accesslevel = 0 " + //
		"ORDER BY items.enchant_level " + //
		"DESC";//
	
	@Override
	public void parseCmd(String command, L2PcInstance player)
	{
		String html = HtmCache.getInstance().getHtm("data/html/CommunityBoard/rank.htm");
		final StringTokenizer st = new StringTokenizer(command, " ");
		st.nextToken();
		
		int itemId = Integer.parseInt(st.nextToken());
		
		if (!Util.contains(items, itemId))
		{
			return;
		}
		
		if (command.startsWith("_bbsshowrank"))
		{
			StringBuilder sb = new StringBuilder();
			Connection con = null;
			int rank = 0;
			
			try
			{
				con = L2DatabaseFactory.getInstance().getConnection();
				PreparedStatement ps = con.prepareStatement(GET_DATA);
				ps.setInt(1, itemId);
				
				sb.append("<table border=\"0\" cellspacing=\"0\" cellpadding=\"0\" width=\"510\" height=\"20\">");
				
				try (ResultSet rs = ps.executeQuery())
				{
					while (rs.next())
					{
						rank++;
						
						String name = rs.getString("char_name");
						String item_name = rs.getString("name");
						int enchant_level = rs.getInt("enchant_level");
						String clan = rs.getString("clan_name");
						
						if (clan == null)
						{
							clan = "-";
						}
						
						sb.append("<tr>");
						sb.append("<td fixwidth=\"30\"> " + rank + "</td>");
						sb.append("<td fixwidth=\"150\"> " + name + "</td>");
						sb.append("<td fixwidth=\"180\"> " + item_name + " +" + enchant_level + "</td>");
						sb.append("<td fixwidth=\"150\" align=center> " + clan + "</td>");
						sb.append("</tr>");
					}
				}
				
				sb.append("<table>");
				
				html = html.replaceAll("%list%", sb.toString());
				separateAndSend(html, player);
			}
			catch (Exception e)
			{
				LOG.warn(RankBBSManager.class.getSimpleName() + ": " + e);
			}
			finally
			{
				CloseUtil.close(con);
			}
		}
		else
		{
			super.parseCmd(command, player);
		}
	}
	
	public static RankBBSManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final RankBBSManager INSTANCE = new RankBBSManager();
	}
	
	@Override
	public String[] getBypassBbsCommands()
	{
		return new String[]
		{
			"_bbsshowrank"
		};
	}
	
	@Override
	public void handleCommand(String command, L2PcInstance player, String params)
	{
		parseCmd(command, player);
	}
}
