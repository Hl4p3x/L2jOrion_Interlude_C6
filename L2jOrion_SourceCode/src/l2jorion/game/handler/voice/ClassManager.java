package l2jorion.game.handler.voice;

import l2jorion.Config;
import l2jorion.game.handler.ICustomByPassHandler;
import l2jorion.game.handler.IVoicedCommandHandler;
import l2jorion.game.model.actor.instance.L2ClassMasterInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;

public class ClassManager implements IVoicedCommandHandler, ICustomByPassHandler
{
	protected static Logger LOG = LoggerFactory.getLogger(ClassManager.class);
	
	private static final String[] VOICED_COMMANDS =
	{
		"class"
	};
	
	@Override
	public boolean useVoicedCommand(String command, L2PcInstance player, String target)
	{
		if (player == null)
		{
			return false;
		}
		
		if (command.equalsIgnoreCase("class"))
		{
			if (Config.ALLOW_CLASS_MASTERS && Config.ALLOW_REMOTE_CLASS_MASTERS)
			{
				L2ClassMasterInstance master_instance = L2ClassMasterInstance.getInstance();
				if (master_instance != null)
				{
					L2ClassMasterInstance.getInstance().mainTable(player);
				}
			}
		}
		return true;
	}
	
	@Override
	public String[] getVoicedCommandList()
	{
		return VOICED_COMMANDS;
	}
	
	@Override
	public String[] getByPassCommands()
	{
		return new String[]
		{
			"class_menu"
		};
	}
	
	private enum CommandEnum
	{
		class_menu
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
			case class_menu:
			{
				if (Config.ALLOW_CLASS_MASTERS)
				{
					L2ClassMasterInstance master_instance = L2ClassMasterInstance.getInstance();
					if (master_instance != null)
					{
						L2ClassMasterInstance.getInstance().mainTable(player);
					}
				}
				break;
			}
		}
	}
}