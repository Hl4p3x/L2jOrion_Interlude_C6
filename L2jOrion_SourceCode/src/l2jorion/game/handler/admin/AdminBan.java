package l2jorion.game.handler.admin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.StringTokenizer;

import l2jorion.Config;
import l2jorion.bots.FakePlayer;
import l2jorion.game.handler.IAdminCommandHandler;
import l2jorion.game.model.L2World;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.entity.Announcements;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.ServerClose;
import l2jorion.game.network.serverpackets.SystemMessage;
import l2jorion.game.thread.LoginServerThread;
import l2jorion.game.util.GMAudit;
import l2jorion.util.CloseUtil;
import l2jorion.util.database.L2DatabaseFactory;

public class AdminBan implements IAdminCommandHandler
{
	private final SimpleDateFormat punishment_date = new SimpleDateFormat("dd/MM/yyyy H:mm:ss");
	
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_ban", // returns ban commands
		"admin_ban_acc",
		"admin_ban_char",
		"admin_banchat",
		"admin_unban", // returns unban commands
		"admin_unban_acc",
		"admin_unban_char",
		"admin_unbanchat",
		"admin_jail",
		"admin_unjail"
	};
	
	@Override
	public boolean useAdminCommand(String command, L2PcInstance activeChar)
	{
		String when = punishment_date.format(new Date(System.currentTimeMillis()));
		
		StringTokenizer st = new StringTokenizer(command);
		st.nextToken();
		
		String targetPlayerName = "";
		L2PcInstance targetPlayer = null;
		int duration = Config.DEFAULT_PUNISH_PARAM;
		String reason = "-";
		
		if (st.hasMoreTokens())
		{
			targetPlayerName = st.nextToken();
			targetPlayer = L2World.getInstance().getPlayer(targetPlayerName);
		}
		else
		{
			if (activeChar.getTarget() != null && activeChar.getTarget() instanceof L2PcInstance)
			{
				targetPlayer = (L2PcInstance) activeChar.getTarget();
			}
		}
		
		if (st.hasMoreTokens())
		{
			duration = Integer.parseInt(st.nextToken());
			
			if (st.hasMoreTokens())
			{
				reason = st.nextToken();
			}
		}
		
		if (command.startsWith("admin_ban ") || command.equalsIgnoreCase("admin_ban"))
		{
			activeChar.sendMessage("Available ban commands: //ban_acc, //ban_char, //ban_chat");
			return false;
		}
		else if (command.startsWith("admin_ban_acc"))
		{
			String text = "SYS: Punished: " + targetPlayerName + " | Reason: " + reason.substring(1) + " | Type: Account ban | Time: Permanent";
			
			if (targetPlayer == null && targetPlayerName.equals(""))
			{
				activeChar.sendMessage("Usage: //ban_acc <account_name> <reason>(if none, target char's account gets banned)");
				return false;
			}
			else if (targetPlayer != null && targetPlayer.equals(activeChar))
			{
				activeChar.sendPacket(new SystemMessage(SystemMessageId.CANNOT_USE_ON_YOURSELF));
				return false;
			}
			else if (targetPlayer != null && targetPlayer.isInOfflineMode())
			{
				targetPlayer.setPunishLevel(L2PcInstance.PunishLevel.ACC, 0);
				activeChar.sendMessage("Ban request sent for account " + targetPlayer.getAccountName());
				targetPlayer.deleteMe();
				auditAction(command, activeChar, targetPlayer.getAccountName());
				activeChar.PunishmentTable(targetPlayerName, reason, "Account ban", "Permanent", when, activeChar.getName());
				Announcements.getInstance().gameAnnounceToAll(text);
			}
			else if (targetPlayer == null)
			{
				LoginServerThread.getInstance().sendAccessLevel(targetPlayerName, -100);
				activeChar.sendMessage("Ban request sent for account " + targetPlayerName);
				auditAction(command, activeChar, targetPlayerName);
				activeChar.PunishmentTable(targetPlayerName, reason, "Account ban", "Permanent", when, activeChar.getName());
				Announcements.getInstance().gameAnnounceToAll(text);
			}
			else
			{
				targetPlayer.setPunishLevel(L2PcInstance.PunishLevel.ACC, 0);
				activeChar.sendMessage("Account " + targetPlayer.getAccountName() + " banned.");
				auditAction(command, activeChar, targetPlayer.getAccountName());
				activeChar.PunishmentTable(targetPlayerName, reason, "Account ban", "Permanent", when, activeChar.getName());
				Announcements.getInstance().gameAnnounceToAll(text);
			}
		}
		else if (command.startsWith("admin_ban_char"))
		{
			String text = "SYS: Punished: " + targetPlayerName + " | Reason: " + reason.substring(1) + " | Type: Character ban | Time: Permanent";
			
			if (targetPlayer == null && targetPlayerName.equals(""))
			{
				activeChar.sendMessage("Usage: //ban_char <char_name> <reason>(if none, target char is banned)");
				return false;
			}
			else if (targetPlayer != null && targetPlayer.equals(activeChar))
			{
				activeChar.sendPacket(new SystemMessage(SystemMessageId.CANNOT_USE_ON_YOURSELF));
				return false;
			}
			else if (targetPlayer != null && targetPlayer.isInOfflineMode())
			{
				auditAction(command, activeChar, (targetPlayer.getName()));
				targetPlayer.deleteMe();
				activeChar.PunishmentTable(targetPlayerName, reason, "Character ban", "Permanent", when, activeChar.getName());
				Announcements.getInstance().gameAnnounceToAll(text);
				return changeCharAccessLevel(targetPlayer, targetPlayerName, activeChar, -100);
			}
			else
			{
				auditAction(command, activeChar, (targetPlayer == null ? targetPlayerName : targetPlayer.getName()));
				activeChar.PunishmentTable(targetPlayerName, reason, "Character ban", "Permanent", when, activeChar.getName());
				Announcements.getInstance().gameAnnounceToAll(text);
				return changeCharAccessLevel(targetPlayer, targetPlayerName, activeChar, -100);
			}
		}
		else if (command.startsWith("admin_banchat"))
		{
			String text = "SYS: Punished: " + targetPlayerName + " | Reason: " + reason.substring(1) + " | Type: Chat ban | Time: " + duration + "min.";
			
			if (targetPlayer == null && targetPlayerName.equals(""))
			{
				activeChar.sendMessage("Usage: //banchat <char_name> [penalty_minutes] [reason]");
				return false;
			}
			if (targetPlayer != null)
			{
				if (targetPlayer.getPunishLevel().value() > 0)
				{
					activeChar.sendMessage(targetPlayer.getName() + " is already jailed or banned.");
					return false;
				}
				
				String banLengthStr = "";
				
				targetPlayer.setPunishLevel(L2PcInstance.PunishLevel.CHAT, duration);
				
				if (duration > 0)
				{
					banLengthStr = " for " + duration + " minutes";
				}
				
				activeChar.sendMessage(targetPlayer.getName() + " is now chat banned" + banLengthStr + ".");
				auditAction(command, activeChar, targetPlayer.getName());
				activeChar.PunishmentTable(targetPlayerName, reason, "Chat ban", "" + duration + "min.", "" + when, "" + activeChar.getName() + "");
				Announcements.getInstance().gameAnnounceToAll(text);
			}
			else
			{
				banChatOfflinePlayer(activeChar, targetPlayerName, duration, true);
				auditAction(command, activeChar, targetPlayerName);
				activeChar.PunishmentTable(targetPlayerName, reason, "Chat ban", "" + duration + "min.", "" + when, "" + activeChar.getName() + "");
				Announcements.getInstance().gameAnnounceToAll(text);
			}
		}
		else if (command.startsWith("admin_unbanchat"))
		{
			if (targetPlayer == null && targetPlayerName.equals(""))
			{
				activeChar.sendMessage("Usage: //unbanchat <char_name>");
				return false;
			}
			if (targetPlayer != null)
			{
				if (targetPlayer.isChatBanned())
				{
					targetPlayer.setPunishLevel(L2PcInstance.PunishLevel.NONE, 0);
					activeChar.sendMessage(targetPlayer.getName() + "'s chat ban has now been lifted.");
					auditAction(command, activeChar, targetPlayer.getName());
				}
				else
				{
					activeChar.sendMessage(targetPlayer.getName() + " is not currently chat banned.");
				}
			}
			else
			{
				banChatOfflinePlayer(activeChar, targetPlayerName, 0, false);
				auditAction(command, activeChar, targetPlayerName);
			}
		}
		else if (command.startsWith("admin_unban ") || command.equalsIgnoreCase("admin_unban"))
		{
			activeChar.sendMessage("Available unban commands: //unban_acc, //unban_char, //unban_chat");
			return false;
		}
		else if (command.startsWith("admin_unban_acc"))
		{
			// Need to check admin_unban_menu command as well in AdminMenu.java handler.
			
			if (targetPlayer != null)
			{
				activeChar.sendMessage(targetPlayer.getName() + " is currently online so must not be banned.");
				return false;
			}
			else if (!targetPlayerName.equals(""))
			{
				LoginServerThread.getInstance().sendAccessLevel(targetPlayerName, 0);
				activeChar.sendMessage("Unban request sent for account " + targetPlayerName);
				auditAction(command, activeChar, targetPlayerName);
			}
			else
			{
				activeChar.sendMessage("Usage: //unban_acc <account_name>");
				return false;
			}
		}
		else if (command.startsWith("admin_unban_char"))
		{
			if (targetPlayer == null && targetPlayerName.equals(""))
			{
				activeChar.sendMessage("Usage: //unban_char <char_name>");
				return false;
			}
			else if (targetPlayer != null)
			{
				activeChar.sendMessage(targetPlayer.getName() + " is currently online so must not be banned.");
				return false;
			}
			else
			{
				auditAction(command, activeChar, targetPlayerName);
				return changeCharAccessLevel(null, targetPlayerName, activeChar, 0);
			}
		}
		else if (command.startsWith("admin_jail"))
		{
			if (targetPlayer == null && targetPlayerName.equals(""))
			{
				activeChar.sendMessage("Usage: //jail <charname> [penalty_minutes] [reason](if no name is given, selected target is jailed indefinitely)");
				return false;
			}
			
			if (targetPlayer != null)
			{
				String text = "SYS: Punished: " + targetPlayer.getName() + " | Reason: " + reason + " | Type: Jail | Time: " + duration + "min.";
				
				targetPlayer.setPunishLevel(L2PcInstance.PunishLevel.JAIL, duration);
				
				if (!(targetPlayer instanceof FakePlayer))
				{
					activeChar.sendMessage("Character " + targetPlayer.getName() + " jailed for " + (duration > 0 ? duration + " minutes." : "ever!"));
					auditAction(command, activeChar, targetPlayer.getName());
					activeChar.PunishmentTable(targetPlayerName, reason, "Jail", "" + duration + "min.", when, activeChar.getName());
					Announcements.getInstance().gameAnnounceToAll(text);
				}
				
				if (targetPlayer.getParty() != null)
				{
					targetPlayer.getParty().removePartyMember(targetPlayer);
				}
			}
			else
			{
				String text = "SYS: Punished: " + targetPlayerName + " | Reason: " + reason + " | Type: Jail | Time: " + duration + "min.";
				
				jailOfflinePlayer(activeChar, targetPlayerName, duration);
				auditAction(command, activeChar, targetPlayerName);
				activeChar.PunishmentTable(targetPlayerName, reason, "Jail", "" + duration + "min.", when, activeChar.getName());
				Announcements.getInstance().gameAnnounceToAll(text);
			}
		}
		else if (command.startsWith("admin_unjail"))
		{
			if (targetPlayer == null && targetPlayerName.equals(""))
			{
				activeChar.sendMessage("Usage: //unjail <charname> (If no name is given target is used)");
				return false;
			}
			else if (targetPlayer != null)
			{
				targetPlayer.setPunishLevel(L2PcInstance.PunishLevel.NONE, 0);
				activeChar.sendMessage("Character " + targetPlayer.getName() + " removed from jail");
				auditAction(command, activeChar, targetPlayer.getName());
			}
			else
			{
				unjailOfflinePlayer(activeChar, targetPlayerName);
				auditAction(command, activeChar, targetPlayerName);
			}
		}
		return true;
	}
	
	private void auditAction(String fullCommand, L2PcInstance activeChar, String target)
	{
		if (!Config.GMAUDIT)
		{
			return;
		}
		
		String[] command = fullCommand.split(" ");
		
		GMAudit.auditGMAction(activeChar.getName() + " [" + activeChar.getObjectId() + "]", command[0], (target.equals("") ? "no-target" : target), (command.length > 2 ? command[2] : ""));
	}
	
	private void banChatOfflinePlayer(L2PcInstance activeChar, String name, int delay, boolean ban)
	{
		Connection con = null;
		int level = 0;
		long value = 0;
		if (ban)
		{
			level = L2PcInstance.PunishLevel.CHAT.value();
			value = (delay > 0 ? delay * 60000L : 60000);
		}
		else
		{
			level = L2PcInstance.PunishLevel.NONE.value();
			value = 0;
		}
		
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			
			PreparedStatement statement = con.prepareStatement("UPDATE characters SET punish_level=?, punish_timer=? WHERE char_name=?");
			statement.setInt(1, level);
			statement.setLong(2, value);
			statement.setString(3, name);
			
			statement.execute();
			int count = statement.getUpdateCount();
			statement.close();
			
			if (count == 0)
			{
				activeChar.sendMessage("Character not found!");
			}
			else if (ban)
			{
				activeChar.sendMessage("Character " + name + " chat-banned for " + (delay > 0 ? delay + " minutes." : "ever!"));
			}
			else
			{
				activeChar.sendMessage("Character " + name + "'s chat-banned lifted");
			}
		}
		catch (SQLException se)
		{
			activeChar.sendMessage("SQLException while chat-banning targetPlayerName");
			if (Config.DEBUG)
			{
				se.printStackTrace();
			}
		}
		finally
		{
			CloseUtil.close(con);
		}
	}
	
	private void jailOfflinePlayer(L2PcInstance activeChar, String name, int delay)
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("UPDATE characters SET x=?, y=?, z=?, punish_level=?, punish_timer=? WHERE char_name=?");
			statement.setInt(1, -114356);
			statement.setInt(2, -249645);
			statement.setInt(3, -2984);
			statement.setInt(4, L2PcInstance.PunishLevel.JAIL.value());
			statement.setLong(5, (delay > 0 ? delay * 60000L : 0));
			statement.setString(6, name);
			
			statement.execute();
			int count = statement.getUpdateCount();
			statement.close();
			
			if (count == 0)
			{
				activeChar.sendMessage("Character not found!");
			}
			else
			{
				activeChar.sendMessage("Character " + name + " jailed for " + (delay > 0 ? delay + " minutes." : "ever!"));
			}
		}
		catch (SQLException se)
		{
			activeChar.sendMessage("SQLException while jailing targetPlayerName");
			if (Config.DEBUG)
			{
				se.printStackTrace();
			}
		}
		finally
		{
			CloseUtil.close(con);
		}
	}
	
	private void unjailOfflinePlayer(L2PcInstance activeChar, String name)
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("UPDATE characters SET x=?, y=?, z=?, punish_level=?, punish_timer=? WHERE char_name=?");
			statement.setInt(1, 17836);
			statement.setInt(2, 170178);
			statement.setInt(3, -3507);
			statement.setInt(4, 0);
			statement.setLong(5, 0);
			statement.setString(6, name);
			statement.execute();
			int count = statement.getUpdateCount();
			statement.close();
			if (count == 0)
			{
				activeChar.sendMessage("Character not found!");
			}
			else
			{
				activeChar.sendMessage("Character " + name + " removed from jail");
			}
		}
		catch (SQLException se)
		{
			activeChar.sendMessage("SQLException while jailing targetPlayerName");
			if (Config.DEBUG)
			{
				se.printStackTrace();
			}
		}
		finally
		{
			CloseUtil.close(con);
		}
	}
	
	private boolean changeCharAccessLevel(L2PcInstance targetPlayer, String targetPlayerName, L2PcInstance activeChar, int lvl)
	{
		boolean output = false;
		
		if (targetPlayer != null)
		{
			targetPlayer.setAccessLevel(lvl);
			targetPlayer.sendMessage("Your character has been banned. Contact the administrator for more information.");
			
			try
			{
				// Save targetPlayerName status
				targetPlayer.store();
				
				// Player Disconnect like L2OFF, no client crash.
				if (targetPlayer.getClient() != null)
				{
					targetPlayer.getClient().sendPacket(ServerClose.STATIC_PACKET);
					targetPlayer.getClient().setActiveChar(null);
					targetPlayer.setClient(null);
				}
			}
			catch (Throwable t)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
				{
					t.printStackTrace();
				}
			}
			
			targetPlayer.deleteMe();
			
			activeChar.sendMessage("The character " + targetPlayer.getName() + " has been banned now.");
			
			output = true;
		}
		else
		{
			
			Connection con = null;
			try
			{
				con = L2DatabaseFactory.getInstance().getConnection();
				PreparedStatement statement = con.prepareStatement("UPDATE characters SET accesslevel=? WHERE char_name=?");
				statement.setInt(1, lvl);
				statement.setString(2, targetPlayerName);
				statement.execute();
				int count = statement.getUpdateCount();
				statement.close();
				if (count == 0)
				{
					activeChar.sendMessage("Character not found or access level unaltered.");
				}
				else
				{
					activeChar.sendMessage(targetPlayerName + " now has an access level of " + lvl);
					output = true;
					
				}
			}
			catch (SQLException se)
			{
				activeChar.sendMessage("SQLException while changing character's access level");
				if (Config.DEBUG)
				{
					se.printStackTrace();
				}
			}
			finally
			{
				CloseUtil.close(con);
			}
		}
		return output;
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}