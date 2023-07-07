package l2jorion.game.handler;

import l2jorion.game.model.actor.instance.L2PcInstance;

public interface IVoicedCommandHandler
{
	public String[] getVoicedCommandList();
	
	public boolean useVoicedCommand(String command, L2PcInstance activeChar, String target);
}
