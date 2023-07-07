package l2jorion.game.handler.admin;

import l2jorion.game.handler.IAdminCommandHandler;
import l2jorion.game.model.L2World;
import l2jorion.game.model.actor.instance.L2PcInstance;

public class AdminChangeAccessLevel implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_changelvl"
	};
	
	@Override
	public boolean useAdminCommand(final String command, final L2PcInstance activeChar)
	{
		handleChangeLevel(command, activeChar);
		return true;
	}
	
	private void handleChangeLevel(final String command, final L2PcInstance activeChar)
	{
		if (activeChar == null)
		{
			return;
		}
		
		String[] parts = command.split(" ");
		
		if (parts.length == 2)
		{
			final int lvl = Integer.parseInt(parts[1]);
			
			if (activeChar.getTarget() instanceof L2PcInstance)
			{
				if (lvl == 0)
				{
					((L2PcInstance) activeChar.getTarget()).getAppearance().setNameColor(Integer.decode(new StringBuilder().append("0x").append("FFFFFF").toString()).intValue());
					((L2PcInstance) activeChar.getTarget()).getAppearance().setTitleColor(Integer.decode(new StringBuilder().append("0x").append("FFFF77").toString()).intValue());
					((L2PcInstance) activeChar.getTarget()).broadcastUserInfo();
				}
				((L2PcInstance) activeChar.getTarget()).setAccessLevel(lvl);
				activeChar.sendMessage("You have changed the access level of player " + activeChar.getTarget().getName() + " to " + lvl + " .");
			}
		}
		else if (parts.length == 3)
		{
			final int lvl = Integer.parseInt(parts[2]);
			
			final L2PcInstance player = L2World.getInstance().getPlayer(parts[1]);
			
			if (player != null)
			{
				player.setAccessLevel(lvl);
				activeChar.sendMessage("You have changed the access level of player " + activeChar.getTarget().getName() + " to " + lvl + " .");
			}
		}
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}
