package l2jorion.game.community.manager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import l2jorion.game.GameServer;
import l2jorion.game.cache.HtmCache;
import l2jorion.game.datatables.sql.CharTemplateTable;
import l2jorion.game.datatables.sql.ClanTable;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.network.serverpackets.ShowBoard;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;

public abstract class BaseBBSManager
{
	protected static Logger LOG = LoggerFactory.getLogger(BaseBBSManager.class);
	
	private final SimpleDateFormat fmt = new SimpleDateFormat("H:mm:ss");
	public static final String CB_PATH = "data/html/CommunityBoard/";
	
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
		separateAndSend(html, player, false);
	}
	
	public static void separateAndSend(String html, L2PcInstance player, boolean specMenu)
	{
		if (html == null || player == null)
		{
			return;
		}
		
		// html = ImagesCache.getInstance().sendUsedImages(html, player);
		
		if (specMenu)
		{
			String content = HtmCache.getInstance().getHtm(CB_PATH + "top/menu.htm").replace("%menu%", html);
			html = content;
		}
		
		if (html.length() < 4090)
		{
			player.sendPacket(new ShowBoard(html, "101", player));
			player.sendPacket(new ShowBoard(null, "102", player));
			player.sendPacket(new ShowBoard(null, "103", player));
		}
		else if (html.length() < 8180)
		{
			player.sendPacket(new ShowBoard(html.substring(0, 4090), "101", player));
			player.sendPacket(new ShowBoard(html.substring(4090, html.length()), "102", player));
			player.sendPacket(new ShowBoard(null, "103", player));
		}
		else if (html.length() < 12270)
		{
			player.sendPacket(new ShowBoard(html.substring(0, 4090), "101", player));
			player.sendPacket(new ShowBoard(html.substring(4090, 8180), "102", player));
			player.sendPacket(new ShowBoard(html.substring(8180, html.length()), "103", player));
		}
	}
	
	protected static void send1001(String html, L2PcInstance player)
	{
		if (html.length() < 8180)
		{
			player.sendPacket(new ShowBoard(html, "1001", player));
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
		content = content.replace("%favorites%", String.valueOf(player.getAllFavorites() != null ? player.getAllFavoritesCount() : 0));
		content = content.replace("%clans%", String.valueOf(ClanTable.getInstance().getClans() != null ? ClanTable.getInstance().getClansCount() : 0));
		content = content.replace("%name%", String.valueOf(player.getName()));
		content = content.replace("%clan%", String.valueOf(player.getClan() != null ? player.getClan().getName() : "-"));
		content = content.replace("%class%", String.valueOf(CharTemplateTable.getClassNameById(player.getActiveClass())));
		content = content.replace("%nobless%", String.valueOf(player.isNoble() ? "Yes" : "No"));
		content = content.replace("%level%", String.valueOf(player.getLevel()));
		content = content.replace("%ip%", String.valueOf(player.getClient().getConnection().getInetAddress() != null ? player.getClient().getConnection().getInetAddress() : "-"));
		
		long millis = player.getOnlineTime() * 1000;
		
		content = content.replace("%onlinetime%", String.valueOf(String.format("%02dh %02dm %02ds", TimeUnit.MILLISECONDS.toHours(millis), TimeUnit.MILLISECONDS.toMinutes(millis) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis)), TimeUnit.MILLISECONDS.toSeconds(millis)
			- TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis)))));
		
		content = content.replace("%servertime%", String.valueOf(fmt.format(new Date(System.currentTimeMillis()))));
		content = content.replace("%restart%", String.valueOf(GameServer.dateTimeServerRestarted));
		
		separateAndSend(content, player);
	}
	
	protected String getFolder()
	{
		return "";
	}
}