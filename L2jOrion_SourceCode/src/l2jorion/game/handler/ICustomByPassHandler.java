package l2jorion.game.handler;

import l2jorion.game.model.actor.instance.L2PcInstance;

public interface ICustomByPassHandler
{
	public String[] getByPassCommands();
	
	public void handleCommand(String command, L2PcInstance player, String parameters);
}
