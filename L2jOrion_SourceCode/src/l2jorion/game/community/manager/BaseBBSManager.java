package l2jorion.game.community.manager;

import java.util.ArrayList;
import java.util.List;

import l2jorion.game.cache.HtmCache;
import l2jorion.game.datatables.sql.ClanTable;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.network.serverpackets.ShowBoard;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;

public abstract class BaseBBSManager
{
	protected static Logger LOG = LoggerFactory.getLogger(BaseBBSManager.class);
	
	protected static final String CB_PATH = "data/html/CommunityBoard/";
	
	public void parseCmd(String command, L2PcInstance player)
	{
		separateAndSend("<html><body><br><br><center>The command: " + command + " isn't implemented.</center></body></html>", player);
	}
	
	public void parseWrite(String ar1, String ar2, String ar3, String ar4, String ar5, L2PcInstance player)
	{
		separateAndSend("<html><body><br><br><center>The command: " + ar1 + " isn't implemented.</center></body></html>", player);
	}
	
	public static void separateAndSend(String html, L2PcInstance player)
	{
		if (html == null || player == null)
		{
			return;
		}
		
		if (html.length() < 4090)
		{
			player.sendPacket(new ShowBoard(html, "101"));
			player.sendPacket(ShowBoard.STATIC_SHOWBOARD_102);
			player.sendPacket(ShowBoard.STATIC_SHOWBOARD_103);
		}
		else if (html.length() < 8180)
		{
			player.sendPacket(new ShowBoard(html.substring(0, 4090), "101"));
			player.sendPacket(new ShowBoard(html.substring(4090, html.length()), "102"));
			player.sendPacket(ShowBoard.STATIC_SHOWBOARD_103);
		}
		else if (html.length() < 12270)
		{
			player.sendPacket(new ShowBoard(html.substring(0, 4090), "101"));
			player.sendPacket(new ShowBoard(html.substring(4090, 8180), "102"));
			player.sendPacket(new ShowBoard(html.substring(8180, html.length()), "103"));
		}
	}
	
	protected static void send1001(String html, L2PcInstance player)
	{
		if (html.length() < 8180)
		{
			player.sendPacket(new ShowBoard(html, "1001"));
		}
	}
	
	protected static void send1002(L2PcInstance player)
	{
		send1002(player, " ", " ", "0");
	}
	
	protected static void send1002(L2PcInstance player, String string, String string2, String string3)
	{
		final List<String> params = new ArrayList<>();
		params.add("0");
		params.add("0");
		params.add("0");
		params.add("0");
		params.add("0");
		params.add("0");
		params.add(player.getName());
		params.add(Integer.toString(player.getObjectId()));
		params.add(player.getAccountName());
		params.add("9");
		params.add(string2);
		params.add(string2);
		params.add(string);
		params.add(string3);
		params.add(string3);
		params.add("0");
		params.add("0");
		player.sendPacket(new ShowBoard(params));
	}
	
	protected void loadStaticHtm(String file, L2PcInstance player)
	{
		String content = HtmCache.getInstance().getHtm(CB_PATH + getFolder() + file);
		
		FavoriteBBSManager.getInstance().loadFavorites(player);
		content = content.replace("%favorites%", ""+(player.getAllFavorites() != null ?  player.getAllFavoritesCount() : 0));
		content = content.replace("%clans%", ""+(ClanTable.getInstance().getClans() != null ?  ClanTable.getInstance().getClansCount() : 0));
		
		separateAndSend(content, player);
	}
	
	protected String getFolder()
	{
		return "";
	}
}