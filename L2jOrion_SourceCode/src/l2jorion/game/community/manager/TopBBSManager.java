package l2jorion.game.community.manager;

import java.util.StringTokenizer;

import l2jorion.game.community.CommunityBoard;
import l2jorion.game.model.actor.instance.L2PcInstance;

public class TopBBSManager extends BaseBBSManager
{
	protected TopBBSManager()
	{
	}
	
	@Override
	public void parseCmd(String command, L2PcInstance player)
	{
		if (command.equals("_bbshome"))
		{
			CommunityBoard.getInstance().addBypass(player, "Top", command);
			
			loadStaticHtm("index.htm", player);
		}
		else if (command.startsWith("_bbshome;"))
		{
			final StringTokenizer st = new StringTokenizer(command, ";");
			st.nextToken();
			
			loadStaticHtm(st.nextToken(), player);
		}
		else
		{
			super.parseCmd(command, player);
		}
	}
	
	@Override
	protected String getFolder()
	{
		return "top/";
	}
	
	public static TopBBSManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final TopBBSManager INSTANCE = new TopBBSManager();
	}
}