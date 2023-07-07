package l2jorion.game.handler;

import l2jorion.game.model.actor.instance.L2PcInstance;

public interface ICommunityBoardHandler
{
	public String[] getBypassBbsCommands();
	
	public void handleCommand(String command, L2PcInstance activeChar, String params);
}
