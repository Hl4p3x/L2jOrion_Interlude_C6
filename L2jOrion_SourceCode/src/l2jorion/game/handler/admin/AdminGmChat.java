package l2jorion.game.handler.admin;

import l2jorion.Config;
import l2jorion.game.datatables.GmListTable;
import l2jorion.game.handler.IAdminCommandHandler;
import l2jorion.game.model.L2Object;
import l2jorion.game.model.L2World;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.CreatureSay;
import l2jorion.game.network.serverpackets.SystemMessage;

public class AdminGmChat implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_gmchat",
		"admin_snoop",
		"admin_gmchat_menu"
	};
	
	@Override
	public boolean useAdminCommand(final String command, final L2PcInstance activeChar)
	{
		
		if (command.startsWith("admin_gmchat"))
		{
			handleGmChat(command, activeChar);
		}
		else if (command.startsWith("admin_snoop"))
		{
			snoop(command, activeChar);
		}
		
		if (command.startsWith("admin_gmchat_menu"))
		{
			AdminHelpPage.showHelpPage(activeChar, "main_menu.htm");
		}
		
		return true;
	}
	
	private void snoop(final String command, final L2PcInstance activeChar)
	{
		L2Object target = null;
		if (command.length() > 12)
		{
			target = L2World.getInstance().getPlayer(command.substring(12));
		}
		if (target == null)
		{
			target = activeChar.getTarget();
		}
		
		if (target == null)
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.YOU_MUST_SELECT_A_TARGET));
			return;
		}
		if (!(target instanceof L2PcInstance))
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.INCORRECT_TARGET));
			return;
		}
		L2PcInstance player = (L2PcInstance) target;
		player.addSnooper(activeChar);
		activeChar.addSnooped(player);
		
		target = null;
		player = null;
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
	
	/**
	 * @param command
	 * @param activeChar
	 */
	private void handleGmChat(final String command, final L2PcInstance activeChar)
	{
		try
		{
			int offset = 0;
			
			String text;
			
			if (command.contains("menu"))
			{
				offset = 17;
			}
			else
			{
				offset = 13;
			}
			
			text = command.substring(offset);
			CreatureSay cs = new CreatureSay(0, 9, activeChar.getName(), text);
			GmListTable.broadcastToGMs(cs);
			
			text = null;
			cs = null;
		}
		catch (final StringIndexOutOfBoundsException e)
		{
			// empty message.. ignore
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
		}
	}
}