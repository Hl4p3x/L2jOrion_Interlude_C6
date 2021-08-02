package l2jorion.game.handler.admin;

import java.util.StringTokenizer;

import l2jorion.game.handler.IAdminCommandHandler;
import l2jorion.game.model.L2World;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.network.serverpackets.NpcHtmlMessage;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;

public class AdminFakeOnline implements IAdminCommandHandler
{
	protected static final Logger LOG = LoggerFactory.getLogger(AdminFakeOnline.class);
	public static int valueplus = 0;
	public static int valueminus = 0;
	
	private static String[] ADMIN_COMMANDS =
	{
		"admin_fakemenu",
		"admin_fakemenuplus",
		"admin_fakemenuminus"
	};
	
	private enum CommandEnum
	{
		admin_fakemenu,
		admin_fakemenuplus,
		admin_fakemenuminus
	}
	
	@Override
	public boolean useAdminCommand(final String command, final L2PcInstance activeChar)
	{
		StringTokenizer st = new StringTokenizer(command);
		final CommandEnum comm = CommandEnum.valueOf(st.nextToken());
		
		if (comm == null)
		{
			return false;
		}
		
		switch (comm)
		{
			case admin_fakemenu:
			{
				fakemenu(activeChar);
				return true;
			}
			case admin_fakemenuplus:
			{
				String val = "";
				
				if (st.hasMoreTokens())
				{
					val = st.nextToken();
					
					try
					{
						valueplus = Integer.parseInt(val);
					}
					catch (final NumberFormatException e)
					{
						activeChar.sendMessage("Usage: //fakemenuplus <value>");
						fakemenu(activeChar);
						return true;
					}
				}
				else
				{
					activeChar.sendMessage("Usage: //fakemenuplus <value>)");
					fakemenu(activeChar);
					return true;
				}

				activeChar.sendMessage("Plus: " + valueplus);
				
				fakemenu(activeChar);
				return true;
			}
		}
		return false;
	}
	
	
	private void fakemenu(final L2PcInstance activeChar) throws IllegalArgumentException
	{
		int online = L2World.getInstance().getAllPlayers().values().size();
		NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
		adminReply.setFile("data/html/admin/fakeonline.htm");
		adminReply.replace("%online%", online);
		activeChar.sendPacket(adminReply);

		adminReply = null;
	}

	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}
	
