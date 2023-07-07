package l2jorion.game.handler;

import l2jorion.game.model.actor.instance.L2PcInstance;

public interface IUserCommandHandler
{
	public boolean useUserCommand(int id, L2PcInstance activeChar);
	
	public int[] getUserCommandList();
}
