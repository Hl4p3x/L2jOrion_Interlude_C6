package l2jorion.game.community;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javolution.util.FastMap;
import l2jorion.Config;
import l2jorion.game.community.manager.BaseBBSManager;
import l2jorion.game.community.manager.ClanBBSManager;
import l2jorion.game.community.manager.FavoriteBBSManager;
import l2jorion.game.community.manager.FriendsBBSManager;
import l2jorion.game.community.manager.MailBBSManager;
import l2jorion.game.community.manager.PostBBSManager;
import l2jorion.game.community.manager.RegionBBSManager;
import l2jorion.game.community.manager.TopBBSManager;
import l2jorion.game.community.manager.TopicBBSManager;
import l2jorion.game.handler.IBBSHandler;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.network.L2GameClient;
import l2jorion.game.network.SystemMessageId;

public class CommunityBoard
{
	private Map<String, IBBSHandler> _handlers;
	private final Map<Integer, String> _bypasses = new ConcurrentHashMap<>();
	
	public CommunityBoard()
	{
		_handlers = new FastMap<>();
	}
	
	public void registerBBSHandler(IBBSHandler handler)
	{
		for (String s : handler.getBBSCommands())
		{
			_handlers.put(s, handler);
		}
	}
	
	public void handleCommands(L2GameClient client, String command)
	{
		final L2PcInstance player = client.getActiveChar();
		if (player == null)
		{
			return;
		}
		
		if (Config.COMMUNITY_TYPE.equals("off"))
		{
			player.sendPacket(SystemMessageId.CB_OFFLINE);
			return;
		}
		
		if (command.startsWith("_bbshome"))
		{
			TopBBSManager.getInstance().parseCmd(command, player);
		}
		else if (command.startsWith("_bbsgetfav") || command.startsWith("bbs_add_fav") || command.startsWith("_bbsdelfav_"))
		{
			FavoriteBBSManager.getInstance().parseCmd(command, player);
		}
		else if (command.startsWith("_bbsloc"))
		{
			RegionBBSManager.getInstance().parseCmd(command, player);
		}
		else if (command.startsWith("_bbsclan"))
		{
			ClanBBSManager.getInstance().parseCmd(command, player);
		}
		else if (command.startsWith("_bbsmemo"))
		{
			TopicBBSManager.getInstance().parseCmd(command, player);
		}
		else if (command.startsWith("_bbsmail") || command.equals("_maillist_0_1_0_"))
		{
			MailBBSManager.getInstance().parseCmd(command, player);
		}
		else if (command.startsWith("_friend") || command.startsWith("_block"))
		{
			FriendsBBSManager.getInstance().parseCmd(command, player);
		}
		else if (command.startsWith("_bbstopics"))
		{
			TopicBBSManager.getInstance().parseCmd(command, player);
		}
		else if (command.startsWith("_bbsposts"))
		{
			PostBBSManager.getInstance().parseCmd(command, player);
		}
		else
		{
			BaseBBSManager.separateAndSend("<html><body><br><br><center>The command: " + command + " isn't implemented.</center></body></html>", player);
		}
	}
	
	public void handleWriteCommands(L2GameClient client, String url, String arg1, String arg2, String arg3, String arg4, String arg5)
	{
		final L2PcInstance player = client.getActiveChar();
		if (player == null)
		{
			return;
		}
		
		if (Config.COMMUNITY_TYPE.equals("off"))
		{
			player.sendPacket(SystemMessageId.CB_OFFLINE);
			return;
		}
		
		if (url.equals("Topic"))
		{
			TopicBBSManager.getInstance().parseWrite(arg1, arg2, arg3, arg4, arg5, player);
		}
		else if (url.equals("Post"))
		{
			PostBBSManager.getInstance().parseWrite(arg1, arg2, arg3, arg4, arg5, player);
		}
		else if (url.equals("_bbsloc"))
		{
			RegionBBSManager.getInstance().parseWrite(arg1, arg2, arg3, arg4, arg5, player);
		}
		else if (url.equals("_bbsclan"))
		{
			ClanBBSManager.getInstance().parseWrite(arg1, arg2, arg3, arg4, arg5, player);
		}
		else if (url.equals("Mail"))
		{
			MailBBSManager.getInstance().parseWrite(arg1, arg2, arg3, arg4, arg5, player);
		}
		else if (url.equals("Clan"))
		{
			ClanBBSManager.getInstance().parseWrite(arg1, arg2, arg3, arg4, arg5, player);
		}
		else if (url.equals("_friend"))
		{
			FriendsBBSManager.getInstance().parseWrite(arg1, arg2, arg3, arg4, arg5, player);
		}
		else
		{
			BaseBBSManager.separateAndSend("<html><body><br><br><center>The command: " + url + " isn't implemented.</center></body></html>", player);
		}
	}
	
	public void addBypass(L2PcInstance player, String title, String bypass)
	{
		_bypasses.put(player.getObjectId(), title + "&" + bypass);
	}
	
	public String removeBypass(L2PcInstance player)
	{
		return _bypasses.remove(player.getObjectId());
	}
	
	public Map<Integer, String> getAllBypass()
	{
		return _bypasses;
	}
	
	public static CommunityBoard getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final CommunityBoard INSTANCE = new CommunityBoard();
	}
}