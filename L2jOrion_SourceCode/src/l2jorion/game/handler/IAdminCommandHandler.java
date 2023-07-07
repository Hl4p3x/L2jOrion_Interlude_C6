package l2jorion.game.handler;

import l2jorion.game.model.actor.instance.L2PcInstance;

public interface IAdminCommandHandler
{
	public boolean useAdminCommand(String command, L2PcInstance activeChar);
	
	public String[] getAdminCommandList();
}
