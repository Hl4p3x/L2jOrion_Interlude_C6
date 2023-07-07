package l2jorion.game.handler.admin;

import l2jorion.Config;
import l2jorion.game.ai.CtrlIntention;
import l2jorion.game.datatables.sql.ClanTable;
import l2jorion.game.handler.IAdminCommandHandler;
import l2jorion.game.managers.GrandBossManager;
import l2jorion.game.model.L2Clan;
import l2jorion.game.model.L2Party;
import l2jorion.game.model.L2World;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.zone.type.L2BossZone;

public class AdminMassRecall implements IAdminCommandHandler
{
	private static String[] _adminCommands =
	{
		"admin_recallclan",
		"admin_recallparty",
		"admin_recallally"
	};
	
	@Override
	public boolean useAdminCommand(final String command, final L2PcInstance activeChar)
	{
		if (command.startsWith("admin_recallclan"))
		{
			try
			{
				String val = command.substring(17).trim();
				
				L2Clan clan = ClanTable.getInstance().getClanByName(val);
				
				if (clan == null)
				{
					activeChar.sendMessage("This clan doesn't exists.");
					return true;
				}
				
				val = null;
				L2PcInstance[] m = clan.getOnlineMembers("");
				
				L2BossZone _Zone = GrandBossManager.getInstance().getZone(activeChar.getX(), activeChar.getY(), activeChar.getZ());
				for (final L2PcInstance element : m)
				{
					if (_Zone != null)
					{
						_Zone.allowPlayerEntry(element, 30);
					}
					Teleport(element, activeChar.getX(), activeChar.getY(), activeChar.getZ(), "Admin is teleporting your clan.");
				}
				activeChar.sendMessage(clan + " is teleporting...");
			}
			catch (final Exception e)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
				{
					e.printStackTrace();
				}
				
				activeChar.sendMessage("Error in recallclan command.");
			}
		}
		else if (command.startsWith("admin_recallally"))
		{
			try
			{
				String val = command.substring(17).trim();
				L2Clan clan = ClanTable.getInstance().getClanByName(val);
				
				if (clan == null)
				{
					activeChar.sendMessage("This clan doesn't exists.");
					return true;
				}
				
				final int ally = clan.getAllyId();
				
				if (ally == 0)
				{
					
					L2PcInstance[] m = clan.getOnlineMembers("");
					L2BossZone _Zone = GrandBossManager.getInstance().getZone(activeChar.getX(), activeChar.getY(), activeChar.getZ());
					for (final L2PcInstance element : m)
					{
						if (_Zone != null)
						{
							_Zone.allowPlayerEntry(element, 30);
						}
						Teleport(element, activeChar.getX(), activeChar.getY(), activeChar.getZ(), "Admin is teleporting your ally.");
					}
				}
				else
				{
					for (final L2Clan aclan : ClanTable.getInstance().getClans())
					{
						if (aclan.getAllyId() == ally)
						{
							L2PcInstance[] m = aclan.getOnlineMembers("");
							L2BossZone _Zone = GrandBossManager.getInstance().getZone(activeChar.getX(), activeChar.getY(), activeChar.getZ());
							for (final L2PcInstance element : m)
							{
								if (_Zone != null)
								{
									_Zone.allowPlayerEntry(element, 30);
								}
								Teleport(element, activeChar.getX(), activeChar.getY(), activeChar.getZ(), "Admin is teleporting your ally.");
							}
						}
					}
				}
				activeChar.sendMessage("Ally is teleporting...");
				clan = null;
				val = null;
			}
			catch (final Exception e)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
				{
					e.printStackTrace();
				}
				
				activeChar.sendMessage("Error in recallally command.");
			}
		}
		else if (command.startsWith("admin_recallparty"))
		{
			try
			{
				String val = command.substring(18).trim();
				L2PcInstance player = L2World.getInstance().getPlayer(val);
				
				if (player == null)
				{
					activeChar.sendMessage("Target error.");
					return true;
				}
				
				if (!player.isInParty())
				{
					activeChar.sendMessage("Player is not in party.");
					return true;
				}
				
				L2Party p = player.getParty();
				L2BossZone _Zone = GrandBossManager.getInstance().getZone(activeChar.getX(), activeChar.getY(), activeChar.getZ());
				for (final L2PcInstance ppl : p.getPartyMembers())
				{
					if (_Zone != null)
					{
						_Zone.allowPlayerEntry(ppl, 30);
					}
					Teleport(ppl, activeChar.getX(), activeChar.getY(), activeChar.getZ(), "Admin is teleporting your party.");
				}
				activeChar.sendMessage("Party is teleporting...");
				
			}
			catch (final Exception e)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
				{
					e.printStackTrace();
				}
				
				activeChar.sendMessage("Error in recallparty command.");
			}
		}
		return true;
	}
	
	private void Teleport(final L2PcInstance player, final int X, final int Y, final int Z, final String Message)
	{
		player.sendMessage(Message);
		player.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
		player.teleToLocation(X, Y, Z, true);
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return _adminCommands;
	}
}
