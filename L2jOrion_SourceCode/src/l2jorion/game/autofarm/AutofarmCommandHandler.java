package l2jorion.game.autofarm;

import l2jorion.game.handler.ICustomByPassHandler;
import l2jorion.game.handler.IVoicedCommandHandler;
import l2jorion.game.model.Location;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.network.serverpackets.NpcHtmlMessage;

public class AutofarmCommandHandler implements ICustomByPassHandler, IVoicedCommandHandler
{
	
	@Override
	public String[] getVoicedCommandList()
	{
		return new String[]
		{
			"farm",
		};
	}
	
	private void showhtm(L2PcInstance player)
	{
		NpcHtmlMessage htm = new NpcHtmlMessage(1);
		htm.setFile("data/html/mods/autofarm/index.htm");
		htm.replace("%mode%", String.valueOf(player.getAutoFarmMode()));
		htm.replace("%radius%", String.valueOf(player.getAutoFarmRadius()));
		player.sendPacket(htm);
	}
	
	@Override
	public boolean useVoicedCommand(String command, L2PcInstance activeChar, String target)
	{
		switch (command)
		{
			case "farm":
				showhtm(activeChar);
				break;
		}
		
		return false;
	}
	
	@Override
	public String[] getByPassCommands()
	{
		return new String[]
		{
			"autofarm",
			"autofarm_stop"
		};
	}
	
	private enum CommandEnum
	{
		autofarm,
		autofarm_stop
	}
	
	@Override
	public void handleCommand(String command, L2PcInstance player, String parameters)
	{
		CommandEnum comm = CommandEnum.valueOf(command);
		
		if (comm == null)
		{
			return;
		}
		
		switch (comm)
		{
			case autofarm:
			{
				String[] cmd = parameters.split(" ");
				
				if (cmd.length < 2 || cmd[1].isEmpty())
				{
					player.sendMessage("Please enter the radius number.");
					showhtm(player);
					return;
				}
				
				if (Integer.parseInt(cmd[1]) > 3000)
				{
					player.sendMessage("Radius can not be bigger than 2000.");
					showhtm(player);
					return;
				}
				
				if (Integer.parseInt(cmd[1]) < 200)
				{
					player.sendMessage("Radius can not be lower than 200.");
					showhtm(player);
					return;
				}
				
				player.setAutoFarmMode(cmd[0]);
				player.setAutoFarmRadius(Integer.parseInt(cmd[1]));
				player.setAutoFarmDistance(new Location(player.getX(), player.getY(), player.getZ()));
				AutofarmManager.INSTANCE.startFarm(player);
				showhtm(player);
				break;
			}
			case autofarm_stop:
			{
				player.setAutoFarmMode("None");
				player.setAutoFarmRadius(0);
				AutofarmManager.INSTANCE.stopFarm(player);
				showhtm(player);
				break;
			}
		}
	}
}
