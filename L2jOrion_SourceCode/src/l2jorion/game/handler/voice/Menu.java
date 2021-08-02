package l2jorion.game.handler.voice;

import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.StringTokenizer;

import l2jorion.Config;
import l2jorion.crypt.Base64;
import l2jorion.game.cache.HtmCache;
import l2jorion.game.handler.ICustomByPassHandler;
import l2jorion.game.handler.IVoicedCommandHandler;
import l2jorion.game.handler.item.Potions;
import l2jorion.game.model.actor.instance.L2ClassMasterInstance;
import l2jorion.game.model.actor.instance.L2ItemInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.zone.ZoneId;
import l2jorion.game.network.serverpackets.ExAutoSoulShot;
import l2jorion.game.network.serverpackets.ExShowScreenMessage;
import l2jorion.game.network.serverpackets.MagicSkillUser;
import l2jorion.game.network.serverpackets.NpcHtmlMessage;
import l2jorion.game.network.serverpackets.PlaySound;
import l2jorion.game.network.serverpackets.UserInfo;
import l2jorion.game.thread.ThreadPoolManager;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;
import l2jorion.util.CloseUtil;
import l2jorion.util.database.L2DatabaseFactory;

public class Menu implements IVoicedCommandHandler, ICustomByPassHandler
{
	protected static Logger LOG = LoggerFactory.getLogger(Menu.class);
	
	private static final String[] VOICED_COMMANDS =
	{
		"control",
		"ap",
		"menu",
		"class"
	};
	
	private static final float MP = (float) 0.70;
	private static final float HP = (float) 0.95;
	private static final float CP = (float) 0.95;
	
	private static final float MANA_POT_CD = Config.MANA_POT_CD;
	private static final int HEALING_POT_CD = (int) Config.HEALING_POT_CD;
	private static final int CP_POT_CD = (int) Config.CP_POT_CD;
	
	private static int activeMp;
	private static boolean activatedMp = false;
	
	private static int activeHp;
	private static boolean activatedHp = false;
	
	private static int activeCp;
	private static boolean activatedCp = false;
	
	final int currency = Config.CUSTOM_ITEM_ID;
	
	private long time;
	String str = "";
	
	private String on = "<table border=0 bgcolor=00ff00><tr><td width=12 height=16></td></tr></table>";
	private String off = "<table border=0 bgcolor=ff0000><tr><td width=12 height=16></td></tr></table>";
	
	@Override
	public boolean useVoicedCommand(String command, L2PcInstance player, String target)
	{
		if (player == null)
		{
			return false;
		}
		
		if (command.equalsIgnoreCase("menu"))
		{
			if (Config.MENU_NEW_STYLE)
			{
				showHtm3(player);
			}
			else
			{
				showHtm(player);
			}
		}
		if (command.equalsIgnoreCase("control"))
		{
			showHtm(player);
		}
		if (command.equalsIgnoreCase("ap"))
		{
			showHtm2(player);
		}
		else if (command.equalsIgnoreCase("class"))
		{
			if (Config.ALLOW_CLASS_MASTERS && Config.ALLOW_REMOTE_CLASS_MASTERS)
			{
				L2ClassMasterInstance master_instance = L2ClassMasterInstance.getInstance();
				if (master_instance != null)
				{
					L2ClassMasterInstance.getInstance().onTable(player);
				}
			}
		}
		return true;
	}
	
	private void showHtm(L2PcInstance player)
	{
		NpcHtmlMessage htm = new NpcHtmlMessage(player.getLastQuestNpcObject());
		String text = HtmCache.getInstance().getHtm("data/html/menu/menu.htm");
		htm.setHtml(text);
		
		if (Config.MENU_NEW_STYLE)
		{
			on = "<img src=\"panels.on\" width=\"16\" height=\"16\">";
			off = "<img src=\"panels.off\" width=\"16\" height=\"16\">";
		}
		
		if (Config.USE_PREMIUMSERVICE)
		{
			if (player.getPremiumService() == 0)
			{
				htm.replace("%exptime%", "<font color=ff0000>Not activated</font>");
			}
			else if (player.getPremiumService() == 1)
			{
				getExpTimePremium(player.getAccountName());
				
				String datePremium = new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date(time));
				String todayDate = new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date());
				
				SimpleDateFormat days = new SimpleDateFormat("yyyy-MM-dd HH:mm");
				
				Calendar cal1 = new GregorianCalendar();
				Calendar cal2 = new GregorianCalendar();
				
				Date date;
				try
				{
					date = days.parse(todayDate);
					cal1.setTime(date);
					date = days.parse(datePremium);
					cal2.setTime(date);
					
					htm.replace("%exptime%", "<font color=00FF00>" + datePremium + "</font> (<font color=00FF00>" + daysBetween(cal1.getTime(), cal2.getTime()) + "</font> days left)");
					
				}
				catch (ParseException e)
				{
					e.printStackTrace();
				}
			}
		}
		else
		{
			htm.replace("%exptime%", "<font color=ff0000>Not activated</font>");
		}
		
		if (player.getExpOn())
		{
			htm.replace("%gainexp%", on);
			htm.replace("%gainexp1%", "ON");
			htm.replace("%nr%", "0");
		}
		else
		{
			htm.replace("%gainexp%", off);
			htm.replace("%gainexp1%", "OFF");
			htm.replace("%nr%", "1");
		}
		if (player.getTitleOn())
		{
			htm.replace("%titlestatus%", on);
			htm.replace("%titlestatus1%", "ON");
			htm.replace("%nr1%", "0");
		}
		else
		{
			htm.replace("%titlestatus%", off);
			htm.replace("%titlestatus1%", "OFF");
			htm.replace("%nr1%", "1");
		}
		if (player.getBlockAllBuffs())
		{
			htm.replace("%blockbuff%", on);
			htm.replace("%blockbuff1%", "ON");
			htm.replace("%nr2%", "0");
		}
		else
		{
			htm.replace("%blockbuff%", off);
			htm.replace("%blockbuff1%", "OFF");
			htm.replace("%nr2%", "1");
		}
		if (player.getAutoLootEnabled())
		{
			htm.replace("%autoloot%", on);
			htm.replace("%autoloot1%", "ON");
			htm.replace("%nr3%", "1");
		}
		else
		{
			htm.replace("%autoloot%", off);
			htm.replace("%autoloot1%", "OFF");
			htm.replace("%nr3%", "0");
		}
		if (player.getAutoLootHerbs())
		{
			htm.replace("%autolootherbs%", on);
			htm.replace("%autolootherbs1%", "ON");
			htm.replace("%nr4%", "0");
		}
		else
		{
			htm.replace("%autolootherbs%", off);
			htm.replace("%autolootherbs1%", "OFF");
			htm.replace("%nr4%", "1");
		}
		if (player.getTradeRefusal())
		{
			htm.replace("%trade%", off);
			htm.replace("%trade1%", "OFF");
			htm.replace("%nr5%", "0");
		}
		else
		{
			htm.replace("%trade%", on);
			htm.replace("%trade1%", "ON");
			htm.replace("%nr5%", "1");
		}
		if (player.getMessageRefusal())
		{
			htm.replace("%pm%", off);
			htm.replace("%pm1%", "OFF");
			htm.replace("%nr6%", "0");
		}
		else
		{
			htm.replace("%pm%", on);
			htm.replace("%pm1%", "ON");
			htm.replace("%nr6%", "1");
		}
		if (player.getIpBlock())
		{
			htm.replace("%ip%", on);
			htm.replace("%ip1%", "ON");
			htm.replace("%nr7%", "0");
		}
		else
		{
			htm.replace("%ip%", off);
			htm.replace("%ip1%", "OFF");
			htm.replace("%nr7%", "1");
		}
		if (player.getScreentxt())
		{
			htm.replace("%screentxt%", on);
			htm.replace("%screentxt1%", "ON");
			htm.replace("%nr8%", "1");
		}
		else
		{
			htm.replace("%screentxt%", off);
			htm.replace("%screentxt1%", "OFF");
			htm.replace("%nr8%", "0");
		}
		if (player.isAutoPot(728) || player.isAutoPot(726))
		{
			htm.replace("%mp%", on);
			htm.replace("%mp1%", "ON");
			htm.replace("%nr9%", "0");
		}
		else
		{
			htm.replace("%mp%", off);
			htm.replace("%mp1%", "OFF");
			htm.replace("%nr9%", "1");
		}
		if (player.isAutoPot(1539) || player.isAutoPot(1060) || player.isAutoPot(1061))
		{
			htm.replace("%hp%", on);
			htm.replace("%hp1%", "ON");
			htm.replace("%nr10%", "0");
		}
		else
		{
			htm.replace("%hp%", off);
			htm.replace("%hp1%", "OFF");
			htm.replace("%nr10%", "1");
		}
		if (player.isAutoPot(5592) || player.isAutoPot(5591))
		{
			htm.replace("%cp%", on);
			htm.replace("%cp1%", "ON");
			htm.replace("%nr11%", "0");
		}
		else
		{
			htm.replace("%cp%", off);
			htm.replace("%cp1%", "OFF");
			htm.replace("%nr11%", "1");
		}
		if (player.getGlow())
		{
			htm.replace("%glow%", off);
			htm.replace("%glow1%", "OFF");
			htm.replace("%nr12%", "0");
		}
		else
		{
			htm.replace("%glow%", on);
			htm.replace("%glow1%", "ON");
			htm.replace("%nr12%", "1");
		}
		if (player.getTeleport())
		{
			htm.replace("%teleport%", on);
			htm.replace("%teleport1%", "ON");
			htm.replace("%nr13%", "1");
		}
		else
		{
			htm.replace("%teleport%", off);
			htm.replace("%teleport1%", "OFF");
			htm.replace("%nr13%", "0");
		}
		
		if (player.getEffects())
		{
			htm.replace("%effects%", on);
			htm.replace("%effects1%", "ON");
			htm.replace("%nr14%", "1");
		}
		else
		{
			htm.replace("%effects%", off);
			htm.replace("%effects1%", "OFF");
			htm.replace("%nr14%", "0");
		}
		
		htm.replace("%pvp%", String.valueOf(player.getPvpKills()));
		htm.replace("%pk%", String.valueOf(player.getPkKills()));
		player.sendPacket(htm);
	}
	
	private void showHtm2(L2PcInstance player)
	{
		NpcHtmlMessage htm = new NpcHtmlMessage(player.getLastQuestNpcObject());
		String text = HtmCache.getInstance().getHtm("data/html/menu/menu2.htm");
		htm.setHtml(text);
		
		if (Config.MENU_NEW_STYLE)
		{
			on = "<img src=\"panels.on\" width=\"16\" height=\"16\">";
			off = "<img src=\"panels.off\" width=\"16\" height=\"16\">";
		}
		
		if (Config.USE_PREMIUMSERVICE)
		{
			if (player.getPremiumService() == 0)
			{
				htm.replace("%exptime%", "<font color=ff0000>Not activated</font>");
			}
			else if (player.getPremiumService() == 1)
			{
				getExpTimePremium(player.getAccountName());
				
				String datePremium = new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date(time));
				String todayDate = new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date());
				
				SimpleDateFormat days = new SimpleDateFormat("yyyy-MM-dd HH:mm");
				
				Calendar cal1 = new GregorianCalendar();
				Calendar cal2 = new GregorianCalendar();
				
				Date date;
				try
				{
					date = days.parse(todayDate);
					cal1.setTime(date);
					date = days.parse(datePremium);
					cal2.setTime(date);
					
					htm.replace("%exptime%", "<font color=00FF00>" + datePremium + "</font> (<font color=00FF00>" + daysBetween(cal1.getTime(), cal2.getTime()) + "</font> days left)");
					
				}
				catch (ParseException e)
				{
					e.printStackTrace();
				}
			}
		}
		else
		{
			htm.replace("%exptime%", "<font color=ff0000>Not activated</font>");
		}
		if (player.isAutoPot(728) || player.isAutoPot(726))
		{
			htm.replace("%mp%", on);
			htm.replace("%mp1%", "ON");
			htm.replace("%nr9%", "0");
		}
		else
		{
			htm.replace("%mp%", off);
			htm.replace("%mp1%", "OFF");
			htm.replace("%nr9%", "1");
		}
		if (player.isAutoPot(1539) || player.isAutoPot(1060) || player.isAutoPot(1061))
		{
			htm.replace("%hp%", on);
			htm.replace("%hp1%", "ON");
			htm.replace("%nr10%", "0");
		}
		else
		{
			htm.replace("%hp%", off);
			htm.replace("%hp1%", "OFF");
			htm.replace("%nr10%", "1");
		}
		if (player.isAutoPot(5592) || player.isAutoPot(5591))
		{
			htm.replace("%cp%", on);
			htm.replace("%cp1%", "ON");
			htm.replace("%nr11%", "0");
		}
		else
		{
			htm.replace("%cp%", off);
			htm.replace("%cp1%", "OFF");
			htm.replace("%nr11%", "1");
		}
		player.sendPacket(htm);
	}
	
	private void showHtm3(L2PcInstance player)
	{
		NpcHtmlMessage htm = new NpcHtmlMessage(player.getLastQuestNpcObject());
		String text = HtmCache.getInstance().getHtm("data/html/menu/menu3.htm");
		htm.setHtml(text);
		
		if (Config.MENU_NEW_STYLE)
		{
			on = "<img src=\"panels.on\" width=\"16\" height=\"16\">";
			off = "<img src=\"panels.off\" width=\"16\" height=\"16\">";
		}
		
		if (Config.USE_PREMIUMSERVICE)
		{
			if (player.getPremiumService() == 0)
			{
				htm.replace("%exptime%", "<font color=ff0000>Not activated</font>");
			}
			else if (player.getPremiumService() == 1)
			{
				getExpTimePremium(player.getAccountName());
				
				String datePremium = new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date(time));
				String todayDate = new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date());
				
				SimpleDateFormat days = new SimpleDateFormat("yyyy-MM-dd HH:mm");
				
				Calendar cal1 = new GregorianCalendar();
				Calendar cal2 = new GregorianCalendar();
				
				Date date;
				try
				{
					date = days.parse(todayDate);
					cal1.setTime(date);
					date = days.parse(datePremium);
					cal2.setTime(date);
					
					htm.replace("%exptime%", "<font color=00FF00>" + datePremium + "</font> (<font color=00FF00>" + daysBetween(cal1.getTime(), cal2.getTime()) + "</font> days left)");
					
				}
				catch (ParseException e)
				{
					e.printStackTrace();
				}
			}
			if (player.getIpBlock())
			{
				htm.replace("%ip%", on);
				htm.replace("%ip1%", "ON");
				htm.replace("%nr7%", "0");
			}
			else
			{
				htm.replace("%ip%", off);
				htm.replace("%ip1%", "OFF");
				htm.replace("%nr7%", "1");
			}
		}
		else
		{
			htm.replace("%exptime%", "<font color=ff0000>Not activated</font>");
		}
		
		player.sendPacket(htm);
	}
	
	private void showHtmPremium(L2PcInstance player)
	{
		NpcHtmlMessage htm = new NpcHtmlMessage(player.getLastQuestNpcObject());
		String text = HtmCache.getInstance().getHtm("data/html/menu/premium.htm");
		htm.setHtml(text);
		player.sendPacket(htm);
	}
	
	private void showPassHtm(L2PcInstance player)
	{
		NpcHtmlMessage htm = new NpcHtmlMessage(player.getLastQuestNpcObject());
		String text = HtmCache.getInstance().getHtm("data/html/menu/changepass.htm");
		htm.setHtml(text);
		player.sendPacket(htm);
	}
	
	public void ipblockdel(L2PcInstance player)
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement preparedstatement = con.prepareStatement("SELECT * FROM " + Config.LOGINSERVER_DB + ".accounts WHERE login=?");
			preparedstatement.setString(1, player.getAccountName());
			ResultSet resultset = preparedstatement.executeQuery();
			resultset.next();
			PreparedStatement preparedstatement1 = con.prepareStatement("UPDATE " + Config.LOGINSERVER_DB + ".accounts SET IPBlock = 0 WHERE login=?");
			preparedstatement1.setString(1, player.getAccountName());
			preparedstatement1.execute();
		}
		catch (Exception e)
		{
		}
		finally
		{
			CloseUtil.close(con);
		}
	}
	
	public void ipblockadd(L2PcInstance player)
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement preparedstatement = con.prepareStatement("SELECT * FROM " + Config.LOGINSERVER_DB + ".accounts WHERE login=?");
			preparedstatement.setString(1, player.getAccountName());
			ResultSet resultset = preparedstatement.executeQuery();
			resultset.next();
			PreparedStatement preparedstatement2 = con.prepareStatement("UPDATE " + Config.LOGINSERVER_DB + ".accounts SET IPBlock = 1 WHERE login=?");
			preparedstatement2.setString(1, player.getAccountName());
			preparedstatement2.execute();
		}
		catch (Exception e)
		{
		}
		finally
		{
			CloseUtil.close(con);
		}
	}
	
	private void getExpTimePremium(String accName)
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT enddate FROM account_premium WHERE account_name=?");
			statement.setString(1, accName);
			ResultSet rset = statement.executeQuery();
			while (rset.next())
			{
				time = rset.getLong("enddate");
			}
			rset.close();
			statement.close();
		}
		catch (Exception e)
		{
		}
		finally
		{
			CloseUtil.close(con);
		}
	}
	
	private void showRepairHtm(L2PcInstance player)
	{
		NpcHtmlMessage htm = new NpcHtmlMessage(player.getLastQuestNpcObject());
		String text = HtmCache.getInstance().getHtm("data/html/menu/repair.htm");
		htm.setHtml(text);
		htm.replace("%acc_chars%", getCharList(player));
		player.sendPacket(htm);
	}
	
	private String getCharList(L2PcInstance activeChar)
	{
		String result = "";
		String repCharAcc = activeChar.getAccountName();
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT char_name FROM characters WHERE account_name=?");
			statement.setString(1, repCharAcc);
			ResultSet rset = statement.executeQuery();
			while (rset.next())
			{
				if (activeChar.getName().compareTo(rset.getString(1)) != 0)
				{
					result += rset.getString(1) + ";";
				}
			}
			rset.close();
			statement.close();
		}
		catch (SQLException e)
		{
			e.printStackTrace();
		}
		finally
		{
			CloseUtil.close(con);
		}
		return result;
	}
	
	private boolean checkAcc(L2PcInstance activeChar, String repairChar)
	{
		boolean result = false;
		String repCharAcc = "";
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT account_name FROM characters WHERE char_name=?");
			statement.setString(1, repairChar);
			ResultSet rset = statement.executeQuery();
			if (rset.next())
			{
				repCharAcc = rset.getString(1);
			}
			rset.close();
			statement.close();
			
		}
		catch (SQLException e)
		{
			e.printStackTrace();
		}
		finally
		{
			CloseUtil.close(con);
			con = null;
		}
		
		if (activeChar.getAccountName().compareTo(repCharAcc) == 0)
		{
			result = true;
		}
		
		return result;
	}
	
	private boolean checkPunish(L2PcInstance activeChar, String repairChar)
	{
		boolean result = false;
		int accessLevel = 0;
		int repCharJail = 0;
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			
			PreparedStatement statement = con.prepareStatement("SELECT accesslevel,punish_level FROM characters WHERE char_name=?");
			statement.setString(1, repairChar);
			ResultSet rset = statement.executeQuery();
			if (rset.next())
			{
				accessLevel = rset.getInt(1);
				repCharJail = rset.getInt(2);
			}
			rset.close();
			statement.close();
			
		}
		catch (SQLException e)
		{
			e.printStackTrace();
		}
		finally
		{
			CloseUtil.close(con);
			con = null;
		}
		
		if (repCharJail == 1 || accessLevel < 0)
		{
			result = true;
		}
		
		return result;
	}
	
	private boolean checkKarma(L2PcInstance activeChar, String repairChar)
	{
		boolean result = false;
		int repCharKarma = 0;
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT karma FROM characters WHERE char_name=?");
			statement.setString(1, repairChar);
			ResultSet rset = statement.executeQuery();
			if (rset.next())
			{
				repCharKarma = rset.getInt(1);
			}
			rset.close();
			statement.close();
		}
		catch (SQLException e)
		{
			e.printStackTrace();
		}
		finally
		{
			CloseUtil.close(con);
			con = null;
		}
		if (repCharKarma > 0)
		{
			result = true;
		}
		return result;
	}
	
	private boolean checkChar(L2PcInstance activeChar, String repairChar)
	{
		boolean result = false;
		if (activeChar.getName().compareTo(repairChar) == 0)
		{
			result = true;
		}
		return result;
	}
	
	private void repairBadCharacter(String charName)
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			
			PreparedStatement statement;
			statement = con.prepareStatement("SELECT obj_Id FROM characters WHERE char_name=?");
			statement.setString(1, charName);
			ResultSet rset = statement.executeQuery();
			
			int objId = 0;
			if (rset.next())
			{
				objId = rset.getInt(1);
			}
			rset.close();
			statement.close();
			if (objId == 0)
			{
				CloseUtil.close(con);
				con = null;
				return;
			}
			statement = con.prepareStatement("UPDATE characters SET x=17867, y=170259, z=-3503 WHERE obj_Id=?");
			statement.setInt(1, objId);
			statement.execute();
			statement.close();
			statement = con.prepareStatement("DELETE FROM character_shortcuts WHERE char_obj_id=?");
			statement.setInt(1, objId);
			statement.execute();
			statement.close();
			statement = con.prepareStatement("UPDATE items SET loc=\"INVENTORY\" WHERE owner_id=? AND loc=\"PAPERDOLL\"");
			statement.setInt(1, objId);
			statement.execute();
			statement.close();
		}
		catch (Exception e)
		{
		}
		finally
		{
			CloseUtil.close(con);
			
		}
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
			"menu_cp",
			"menu_hp",
			"menu_mp",
			"menu_cp2",
			"menu_hp2",
			"menu_mp2",
			"menu_exp",
			"menu_title",
			"menu_blockbuff",
			"menu_loot",
			"menu_lootherbs",
			"menu_trade",
			"menu_pm",
			"menu_ip",
			"menu_ip2",
			"menu_hwid",
			"menu_evt",
			"menu_prem",
			"menu_pass",
			"menu_pass_change",
			"menu_screentxt",
			"menu_repair",
			"menu_dorepair",
			"menu_glow",
			"menu_teleport",
			"menu_effects",
			"menu_back",
			"menu2",
			"menu3",
			"menu_premium",
			"menu_premium_set"
		};
	}
	
	private enum CommandEnum
	{
		menu_cp,
		menu_hp,
		menu_mp,
		menu_cp2,
		menu_hp2,
		menu_mp2,
		menu_exp,
		menu_title,
		menu_blockbuff,
		menu_loot,
		menu_lootherbs,
		menu_trade,
		menu_pm,
		menu_ip,
		menu_ip2,
		menu_hwid,
		menu_evt,
		menu_prem,
		menu_pass,
		menu_pass_change,
		menu_screentxt,
		menu_repair,
		menu_dorepair,
		menu_glow,
		menu_teleport,
		menu_effects,
		menu_back,
		menu2,
		menu3,
		menu_premium,
		menu_premium_set
	}
	
	@Override
	public void handleCommand(String command, L2PcInstance player, String parameters)
	{
		CommandEnum comm = CommandEnum.valueOf(command);
		
		if (comm == null)
		{
			return;
		}
		
		final L2ItemInstance item = player.getInventory().getItemByItemId(currency);
		
		switch (comm)
		{
			case menu_cp:
			{
				int flag = Integer.parseInt(parameters.trim());
				if (flag == 0)
				{
					if (player.isAutoPot(5592))
					{
						player.sendPacket(new ExAutoSoulShot(5592, 0));
						player.sendPacket(new ExShowScreenMessage("" + Config.ALT_Server_Menu_Name + ": Auto CP potions off.", 2000, 0x01, false));
						player.sendMessage("" + Config.ALT_Server_Menu_Name + ": Auto CP potions off.");
						player.setAutoPot(5592, null, false);
					}
				}
				else
				{
					if (player.getInventory().getItemByItemId(5592) != null)
					{
						if (player.getInventory().getItemByItemId(5592).getCount() > 1)
						{
							player.sendPacket(new ExAutoSoulShot(5592, 1));
							player.sendPacket(new ExShowScreenMessage("" + Config.ALT_Server_Menu_Name + ": Auto CP potions on.", 2000, 0x01, false));
							player.sendMessage("" + Config.ALT_Server_Menu_Name + ": Auto CP potions on.");
							player.setAutoPot(5592, ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new AutoPot(5592, player, CP), 1000, CP_POT_CD * 1000), true);
						}
						else
						{
							MagicSkillUser msu = new MagicSkillUser(player, player, 2166, 2, 0, 100);
							player.broadcastPacket(msu);
							
							Potions is = new Potions();
							is.useItem(player, player.getInventory().getItemByItemId(5592));
							player.destroyItem("Consume", player.getInventory().getItemByItemId(5592), null, false);
						}
					}
					else
					{
						player.sendPacket(new ExShowScreenMessage("" + Config.ALT_Server_Menu_Name + ": You don't have CP potions.", 2000, 0x01, false));
						player.sendMessage("" + Config.ALT_Server_Menu_Name + ": You don't have CP potions.");
					}
				}
				showHtm(player);
				return;
			}
			case menu_hp:
			{
				int flag = Integer.parseInt(parameters.trim());
				if (flag == 0)
				{
					if (player.isAutoPot(1539))
					{
						player.sendPacket(new ExAutoSoulShot(1539, 0));
						player.sendPacket(new ExShowScreenMessage("" + Config.ALT_Server_Menu_Name + ": Auto HP potions off.", 2000, 0x01, false));
						player.sendMessage("" + Config.ALT_Server_Menu_Name + ": Auto HP potions off.");
						player.setAutoPot(1539, null, false);
					}
				}
				else
				{
					if (player.getInventory().getItemByItemId(1539) != null)
					{
						if (player.getInventory().getItemByItemId(1539).getCount() > 1)
						{
							player.sendPacket(new ExAutoSoulShot(1539, 1));
							player.sendPacket(new ExShowScreenMessage("" + Config.ALT_Server_Menu_Name + ": Auto HP potions on.", 2000, 0x01, false));
							player.sendMessage("" + Config.ALT_Server_Menu_Name + ": Auto HP potions on.");
							player.setAutoPot(1539, ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new AutoPot(1539, player, HP), 1000, HEALING_POT_CD * 1000), true);
						}
						else
						{
							MagicSkillUser msu = new MagicSkillUser(player, player, 2037, 1, 0, 100);
							player.broadcastPacket(msu);
							
							Potions is = new Potions();
							is.useItem(player, player.getInventory().getItemByItemId(1539));
							player.destroyItem("Consume", player.getInventory().getItemByItemId(1539), null, false);
						}
					}
					else
					{
						player.sendPacket(new ExShowScreenMessage("" + Config.ALT_Server_Menu_Name + ": You don't have HP potions.", 2000, 0x01, false));
						player.sendMessage("" + Config.ALT_Server_Menu_Name + ": You don't have HP potions.");
					}
				}
				showHtm(player);
				return;
			}
			case menu_mp:
			{
				int flag = Integer.parseInt(parameters.trim());
				if (flag == 0)
				{
					if (player.isAutoPot(728))
					{
						player.sendPacket(new ExAutoSoulShot(728, 0));
						player.sendPacket(new ExShowScreenMessage("" + Config.ALT_Server_Menu_Name + ": Auto MP potions off.", 2000, 0x01, false));
						player.sendMessage("" + Config.ALT_Server_Menu_Name + ": Auto MP potions off.");
						player.setAutoPot(728, null, false);
					}
				}
				else
				{
					if (player.getInventory().getItemByItemId(728) != null)
					{
						if (player.getInventory().getItemByItemId(728).getCount() > 1)
						{
							player.sendPacket(new ExAutoSoulShot(728, 1));
							player.sendPacket(new ExShowScreenMessage("" + Config.ALT_Server_Menu_Name + ": Auto MP potions on.", 2000, 0x01, false));
							player.sendMessage("" + Config.ALT_Server_Menu_Name + ": Auto MP potions on.");
							player.setAutoPot(728, ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new AutoPot(728, player, MP), 1000, (long) (MANA_POT_CD * 1000)), true);
						}
						else
						{
							MagicSkillUser msu = new MagicSkillUser(player, player, 2005, 1, 0, 100);
							player.broadcastPacket(msu);
							
							Potions is = new Potions();
							is.useItem(player, player.getInventory().getItemByItemId(728));
							player.destroyItem("Consume", player.getInventory().getItemByItemId(728), null, false);
						}
					}
					else
					{
						player.sendPacket(new ExShowScreenMessage("" + Config.ALT_Server_Menu_Name + ": You don't have MP potions.", 2000, 0x01, false));
						player.sendMessage("" + Config.ALT_Server_Menu_Name + ": You don't have MP potions.");
					}
				}
				showHtm(player);
				return;
			}
			case menu_cp2:
			{
				L2ItemInstance cp1 = player.getInventory().getItemByItemId(5592);
				L2ItemInstance cp2 = player.getInventory().getItemByItemId(5591);
				activatedCp = false;
				
				if (cp1 != null && !activatedCp)
				{
					activeCp = 5592;
					activatedCp = true;
				}
				
				if (cp2 != null && !activatedCp)
				{
					activeCp = 5591;
					activatedCp = true;
				}
				
				String time = parameters.substring(1).trim();
				if (time.equals("") || (!time.matches("[1-9][0-9]*")) || time.length() > 2)
				{
					time = "95";
				}
				
				int flag = Integer.parseInt(parameters.substring(0, 1).trim());
				if (flag == 0)
				{
					if (player.isAutoPot(activeCp))
					{
						player.sendPacket(new ExAutoSoulShot(activeCp, 0));
						player.sendPacket(new ExShowScreenMessage("" + Config.ALT_Server_Menu_Name + ": Auto CP potions off.", 2000, 0x01, false));
						player.sendMessage("" + Config.ALT_Server_Menu_Name + ": Auto CP potions off.");
						player.setAutoPot(activeCp, null, false);
						activatedCp = false;
					}
				}
				else
				{
					if (player.getInventory().getItemByItemId(activeCp) != null && player.getInventory().getItemByItemId(activeCp).getCount() > 0)
					{
						player.sendPacket(new ExAutoSoulShot(activeCp, 1));
						player.sendPacket(new ExShowScreenMessage("" + Config.ALT_Server_Menu_Name + ": Auto CP potions on. Chosen: " + time + "%", 2000, 0x01, false));
						player.sendMessage("" + Config.ALT_Server_Menu_Name + ": Auto CP potions on. Chosen: " + time + "%");
						player.setAutoPot(activeCp, ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new AutoPot(activeCp, player, Float.parseFloat("0." + time)), 1000, CP_POT_CD * 1000), true);
					}
					else
					{
						player.sendPacket(new ExShowScreenMessage("" + Config.ALT_Server_Menu_Name + ": You don't have any CP potion.", 2000, 0x01, false));
						player.sendMessage("" + Config.ALT_Server_Menu_Name + ": You don't have any CP potion.");
					}
				}
				
				showHtm2(player);
				break;
			}
			case menu_hp2:
			{
				L2ItemInstance hp1 = player.getInventory().getItemByItemId(1539); // Greater Healing Potion
				L2ItemInstance hp2 = player.getInventory().getItemByItemId(1061); // Healing Potion
				L2ItemInstance hp3 = player.getInventory().getItemByItemId(1060); // Lesser Healing Potion
				// On start up must be always false
				activatedHp = false;
				
				if (hp1 != null && !activatedHp)
				{
					activeHp = 1539;
					activatedHp = true;
				}
				
				if (hp2 != null && !activatedHp)
				{
					activeHp = 1061;
					activatedHp = true;
				}
				
				if (hp3 != null && !activatedHp)
				{
					activeHp = 1060;
					activatedHp = true;
				}
				
				int flag = Integer.parseInt(parameters.substring(0, 1).trim());
				String time = parameters.substring(1).trim();
				
				if (time.equals("") || (!time.matches("[1-9][0-9]*")) || time.length() > 2)
				{
					time = "95";
				}
				
				if (flag == 0)
				{
					if (player.isAutoPot(activeHp))
					{
						player.sendPacket(new ExAutoSoulShot(activeHp, 0));
						player.sendPacket(new ExShowScreenMessage("" + Config.ALT_Server_Menu_Name + ": Auto HP potion off.", 2000, 0x01, false));
						player.sendMessage("" + Config.ALT_Server_Menu_Name + ": Auto HP potion off.");
						player.setAutoPot(activeHp, null, false);
						activatedHp = false;
					}
				}
				else
				{
					if (player.getInventory().getItemByItemId(activeHp) != null && player.getInventory().getItemByItemId(activeHp).getCount() > 0)
					{
						player.sendPacket(new ExAutoSoulShot(activeHp, 1));
						player.sendPacket(new ExShowScreenMessage("" + Config.ALT_Server_Menu_Name + ": Auto HP potion on. Chosen: " + time + "%", 2000, 0x01, false));
						player.sendMessage("" + Config.ALT_Server_Menu_Name + ": Auto HP potion on. Chosen: " + time + "%");
						player.setAutoPot(activeHp, ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new AutoPot(activeHp, player, Float.parseFloat("0." + time)), 1000, HEALING_POT_CD * 1000), true);
					}
					else
					{
						player.sendPacket(new ExShowScreenMessage("" + Config.ALT_Server_Menu_Name + ": You don't have any HP potion.", 2000, 0x01, false));
						player.sendMessage("" + Config.ALT_Server_Menu_Name + ": You don't have any HP potion.");
					}
				}
				
				showHtm2(player);
				break;
			}
			case menu_mp2:
			{
				L2ItemInstance mp1 = player.getInventory().getItemByItemId(728);
				L2ItemInstance mp2 = player.getInventory().getItemByItemId(726);
				activatedMp = false;
				
				if (mp1 != null && !activatedMp)
				{
					activeMp = 728;
					activatedMp = true;
				}
				
				if (mp2 != null && !activatedMp)
				{
					activeMp = 726;
					activatedMp = true;
				}
				
				String time = parameters.substring(1).trim();
				if (time.equals("") || (!time.matches("[1-9][0-9]*")) || time.length() > 2)
				{
					time = "70";
				}
				
				int flag = Integer.parseInt(parameters.substring(0, 1).trim());
				if (flag == 0)
				{
					if (player.isAutoPot(activeMp))
					{
						player.sendPacket(new ExAutoSoulShot(activeMp, 0));
						player.sendPacket(new ExShowScreenMessage("" + Config.ALT_Server_Menu_Name + ": Auto MP potion off.", 2000, 0x01, false));
						player.sendMessage("" + Config.ALT_Server_Menu_Name + ": Auto MP potion off.");
						player.setAutoPot(activeMp, null, false);
						activatedMp = false;
					}
				}
				else
				{
					if (player.getInventory().getItemByItemId(activeMp) != null && player.getInventory().getItemByItemId(activeMp).getCount() > 0)
					{
						player.sendPacket(new ExAutoSoulShot(activeMp, 1));
						player.sendPacket(new ExShowScreenMessage("" + Config.ALT_Server_Menu_Name + ": Auto MP potion on. Chosen: " + time + "%", 2000, 0x01, false));
						player.sendMessage("" + Config.ALT_Server_Menu_Name + ": Auto MP potion on. Chosen: " + time + "%");
						
						if (activeMp == 728)
						{
							player.setAutoPot(activeMp, ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new AutoPot(activeMp, player, Float.parseFloat("0." + time)), 1000, (long) (MANA_POT_CD * 1000)), true);
						}
						else if (activeMp == 726)
						{
							player.setAutoPot(activeMp, ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new AutoPot(activeMp, player, Float.parseFloat("0." + time)), 1000, 20 * 1000), true);
						}
					}
					else
					{
						player.sendPacket(new ExShowScreenMessage("" + Config.ALT_Server_Menu_Name + ": You don't have any MP potion.", 2000, 0x01, false));
						player.sendMessage("" + Config.ALT_Server_Menu_Name + ": You don't have any MP potion.");
					}
				}
				
				showHtm2(player);
				break;
			}
			case menu_exp:
			{
				int flag = Integer.parseInt(parameters.trim());
				if (flag == 0)
				{
					player.setExpOn(0);
					player.sendPacket(new ExShowScreenMessage("" + Config.ALT_Server_Menu_Name + ": Exp off.", 2000, 0x01, false));
					player.sendMessage("" + Config.ALT_Server_Menu_Name + ": Exp off.");
				}
				else
				{
					player.setExpOn(1);
					player.sendPacket(new ExShowScreenMessage("" + Config.ALT_Server_Menu_Name + ": Exp on.", 2000, 0x01, false));
					player.sendMessage("" + Config.ALT_Server_Menu_Name + ": Exp on.");
				}
				showHtm(player);
				return;
			}
			case menu_title:
			{
				int flag = Integer.parseInt(parameters.trim());
				if (flag == 0)
				{
					player.setTitleOn(0);
					player.setTitle("");
					player.broadcastTitleInfo();
					player.sendPacket(new ExShowScreenMessage("" + Config.ALT_Server_Menu_Name + ": Title off.", 2000, 0x01, false));
					player.sendMessage("" + Config.ALT_Server_Menu_Name + ": Title off.");
				}
				else
				{
					player.setTitleOn(1);
					player.sendPacket(new ExShowScreenMessage("" + Config.ALT_Server_Menu_Name + ": Title on.", 2000, 0x01, false));
					player.sendMessage("" + Config.ALT_Server_Menu_Name + ": Title on.");
				}
				showHtm(player);
				return;
			}
			case menu_blockbuff:
			{
				int flag = Integer.parseInt(parameters.trim());
				if (flag == 0)
				{
					player.setBlockAllBuffs(0);
					player.sendPacket(new ExShowScreenMessage("" + Config.ALT_Server_Menu_Name + ": Block buffs off.", 2000, 0x01, false));
					player.sendMessage("" + Config.ALT_Server_Menu_Name + ": Block buffs off.");
				}
				else
				{
					player.setBlockAllBuffs(1);
					player.sendPacket(new ExShowScreenMessage("" + Config.ALT_Server_Menu_Name + ": Block buffs on.", 2000, 0x01, false));
					player.sendMessage("" + Config.ALT_Server_Menu_Name + ": Block buffs on.");
				}
				showHtm(player);
				return;
			}
			case menu_loot:
			{
				int flag = Integer.parseInt(parameters.trim());
				if (flag == 0)
				{
					player.setAutoLootEnabled(1);
					player.sendPacket(new ExShowScreenMessage("" + Config.ALT_Server_Menu_Name + ": Auto pick up on.", 2000, 0x01, false));
					player.sendMessage("" + Config.ALT_Server_Menu_Name + ": Auto pick up on.");
				}
				else
				{
					player.setAutoLootEnabled(0);
					player.sendPacket(new ExShowScreenMessage("" + Config.ALT_Server_Menu_Name + ": Auto pick up off.", 2000, 0x01, false));
					player.sendMessage("" + Config.ALT_Server_Menu_Name + ": Auto pick up off.");
				}
				showHtm(player);
				return;
			}
			case menu_lootherbs:
			{
				int flag = Integer.parseInt(parameters.trim());
				if (flag == 0)
				{
					player.setAutoLootHerbs(0);
					player.sendPacket(new ExShowScreenMessage("" + Config.ALT_Server_Menu_Name + ": Auto loot herbs off.", 2000, 0x01, false));
					player.sendMessage("" + Config.ALT_Server_Menu_Name + ": Auto loot herbs off.");
				}
				else
				{
					player.setAutoLootHerbs(1);
					player.sendPacket(new ExShowScreenMessage("" + Config.ALT_Server_Menu_Name + ": Auto loot herbs on.", 2000, 0x01, false));
					player.sendMessage("" + Config.ALT_Server_Menu_Name + ": Auto loot herbs on.");
				}
				showHtm(player);
				return;
			}
			case menu_trade:
			{
				int flag = Integer.parseInt(parameters.trim());
				if (flag == 0)
				{
					player.setTradeRefusal(1);
					player.sendPacket(new ExShowScreenMessage("" + Config.ALT_Server_Menu_Name + ": Trade on.", 2000, 0x01, false));
					player.sendMessage("" + Config.ALT_Server_Menu_Name + ": Trade on.");
				}
				else
				{
					player.setTradeRefusal(0);
					player.sendPacket(new ExShowScreenMessage("" + Config.ALT_Server_Menu_Name + ": Trade off.", 2000, 0x01, false));
					player.sendMessage("" + Config.ALT_Server_Menu_Name + ": Trade off.");
				}
				showHtm(player);
				return;
			}
			case menu_pm:
			{
				int flag = Integer.parseInt(parameters.trim());
				if (flag == 0)
				{
					player.setMessageRefusal(1);
					player.sendPacket(new ExShowScreenMessage("" + Config.ALT_Server_Menu_Name + ": Private messages on.", 2000, 0x01, false));
					player.sendMessage("" + Config.ALT_Server_Menu_Name + ": Private messages on.");
				}
				else
				{
					player.setMessageRefusal(0);
					player.sendPacket(new ExShowScreenMessage("" + Config.ALT_Server_Menu_Name + ": Private messages off.", 2000, 0x01, false));
					player.sendMessage("" + Config.ALT_Server_Menu_Name + ": Private messages off.");
				}
				showHtm(player);
				return;
			}
			case menu_ip:
			{
				int flag = Integer.parseInt(parameters.trim());
				if (flag == 0)
				{
					ipblockdel(player);
					player.setIpBlock(false);
					player.sendPacket(new ExShowScreenMessage("" + Config.ALT_Server_Menu_Name + ": Account protection off.", 2000, 0x01, false));
					player.sendMessage("" + Config.ALT_Server_Menu_Name + ": Account protection off.");
				}
				else
				{
					ipblockadd(player);
					player.setIpBlock(true);
					player.sendPacket(new ExShowScreenMessage("" + Config.ALT_Server_Menu_Name + ": Account protection on.", 2000, 0x01, false));
					player.sendMessage("" + Config.ALT_Server_Menu_Name + ": Account protection on.");
				}
				showHtm(player);
				return;
			}
			case menu_screentxt:
			{
				int flag = Integer.parseInt(parameters.trim());
				if (flag == 0)
				{
					player.setScreentxt(0);
					player.sendPacket(new ExShowScreenMessage("" + Config.ALT_Server_Menu_Name + ": Screen messages on.", 2000, 0x01, false));
					player.sendMessage("" + Config.ALT_Server_Menu_Name + ": Screen messages on.");
				}
				else
				{
					player.setScreentxt(1);
					player.sendPacket(new ExShowScreenMessage("" + Config.ALT_Server_Menu_Name + ": Screen messages off.", 2000, 0x01, false));
					player.sendMessage("" + Config.ALT_Server_Menu_Name + ": Screen messages off.");
				}
				showHtm(player);
				return;
			}
			case menu_pass:
			{
				int flag = Integer.parseInt(parameters.trim());
				if (flag == 0)
				{
					showPassHtm(player);
					return;
				}
			}
			case menu_pass_change:
			{
				StringTokenizer st = new StringTokenizer(parameters);
				Connection con = null;
				try
				{
					String curpass = null, newpass = null, repeatnewpass = null;
					if (st.hasMoreTokens())
					{
						curpass = st.nextToken();
					}
					if (st.hasMoreTokens())
					{
						newpass = st.nextToken();
					}
					if (st.hasMoreTokens())
					{
						repeatnewpass = st.nextToken();
					}
					
					if (!((curpass == null) || (newpass == null) || (repeatnewpass == null)))
					{
						if (!newpass.equals(repeatnewpass))
						{
							player.sendMessage("The new password doesn't match with the repeated one.");
							player.sendPacket(new ExShowScreenMessage("The new password doesn't match with the repeated one.", 2000, 0x01, false));
							showPassHtm(player);
							return;
						}
						
						if (newpass.length() < 3)
						{
							player.sendMessage("The new password is shorter than 3 chars! Please try with a longer one.");
							player.sendPacket(new ExShowScreenMessage("The new password is shorter than 3 chars! Please try with a longer one.", 2000, 0x01, false));
							showPassHtm(player);
							return;
						}
						
						if (newpass.length() > 30)
						{
							player.sendMessage("The new password is longer than 30 chars! Please try with a shorter one.");
							player.sendPacket(new ExShowScreenMessage("The new password is longer than 30 chars! Please try with a shorter one.", 2000, 0x01, false));
							showPassHtm(player);
							return;
						}
						
						MessageDigest md = MessageDigest.getInstance("SHA");
						
						byte[] raw = curpass.getBytes("UTF-8");
						raw = md.digest(raw);
						String curpassEnc = Base64.encodeBytes(raw);
						String pass = null;
						int passUpdated = 0;
						
						con = L2DatabaseFactory.getInstance().getConnection();
						PreparedStatement statement = con.prepareStatement("SELECT password FROM accounts WHERE login=?");
						statement.setString(1, player.getAccountName());
						ResultSet rset = statement.executeQuery();
						if (rset.next())
						{
							pass = rset.getString("password");
						}
						rset.close();
						statement.close();
						con.close();
						
						if (curpassEnc.equals(pass))
						{
							byte[] password = newpass.getBytes("UTF-8");
							password = md.digest(password);
							// SQL connection
							Connection con2 = L2DatabaseFactory.getInstance().getConnection();
							PreparedStatement ps = con2.prepareStatement("UPDATE accounts SET password=? WHERE login=?");
							ps.setString(1, Base64.encodeBytes(password));
							ps.setString(2, player.getAccountName());
							passUpdated = ps.executeUpdate();
							ps.close();
							con2.close();
							
							LOG.info("Character " + player.getName() + " has changed his password from " + curpassEnc + " to " + Base64.encodeBytes(password));
							
							if (passUpdated > 0)
							{
								player.sendMessage("You have successfully changed your password.");
								player.sendPacket(new ExShowScreenMessage("You have successfully changed your password.", 2000, 0x01, false));
							}
							else
							{
								player.sendMessage("The password change was unsuccessful.");
								player.sendPacket(new ExShowScreenMessage("The password change was unsuccessful.", 2000, 0x01, false));
								showPassHtm(player);
							}
							
						}
						else
						{
							player.sendMessage("Current Password doesn't match with your new one.");
							player.sendPacket(new ExShowScreenMessage("Current Password doesn't match with your new one.", 2000, 0x01, false));
							showPassHtm(player);
						}
					}
					else
					{
						player.sendMessage("Invalid password data.");
						player.sendPacket(new ExShowScreenMessage("Invalid password data.", 2000, 0x01, false));
						showPassHtm(player);
					}
				}
				catch (Exception e)
				{
					player.sendMessage("A problem occured while changing password.");
					player.sendPacket(new ExShowScreenMessage("A problem occured while changing password.", 2000, 0x01, false));
					LOG.warn("", e);
				}
				
				return;
			}
			case menu_back:
			{
				int flag = Integer.parseInt(parameters.trim());
				if (flag == 0)
				{
					showHtm(player);
					return;
				}
			}
			case menu2:
			{
				showHtm2(player);
				return;
			}
			case menu3:
			{
				showHtm3(player);
				return;
			}
			case menu_repair:
			{
				int flag = Integer.parseInt(parameters.trim());
				if (flag == 0)
				{
					showRepairHtm(player);
					return;
				}
				
			}
			case menu_dorepair:
			{
				if (parameters == null || parameters.equals(""))
				{
					return;
				}
				
				if (checkAcc(player, parameters))
				{
					if (checkChar(player, parameters))
					{
						String htmContent = HtmCache.getInstance().getHtm("data/html/menu/repair-self.htm");
						NpcHtmlMessage npcHtmlMessage = new NpcHtmlMessage(5);
						npcHtmlMessage.setHtml(htmContent);
						player.sendPacket(npcHtmlMessage);
						return;
					}
					else if (checkPunish(player, parameters))
					{
						String htmContent = HtmCache.getInstance().getHtm("data/html/menu/repair-jail.htm");
						NpcHtmlMessage npcHtmlMessage = new NpcHtmlMessage(5);
						npcHtmlMessage.setHtml(htmContent);
						player.sendPacket(npcHtmlMessage);
						return;
					}
					else if (checkKarma(player, parameters))
					{
						player.sendMessage("Selected Char has Karma, cannot be repaired!");
						return;
					}
					else
					{
						repairBadCharacter(parameters);
						String htmContent = HtmCache.getInstance().getHtm("data/html/menu/repair-done.htm");
						NpcHtmlMessage npcHtmlMessage = new NpcHtmlMessage(5);
						npcHtmlMessage.setHtml(htmContent);
						player.sendPacket(npcHtmlMessage);
						return;
					}
				}
				
				String htmContent = HtmCache.getInstance().getHtm("data/html/menu/repair-error.htm");
				NpcHtmlMessage npcHtmlMessage = new NpcHtmlMessage(5);
				npcHtmlMessage.setHtml(htmContent);
				npcHtmlMessage.replace("%acc_chars%", getCharList(player));
				player.sendPacket(npcHtmlMessage);
				// showRepairHtm(player);
				return;
			}
			case menu_glow:
			{
				int flag = Integer.parseInt(parameters.trim());
				
				if (player._inEventTvT || player._inEventCTF || player._inEventDM)
				{
					player.sendMessage("You can't use it when you are in an event.");
					return;
				}
				
				if (player.isInCombat())
				{
					player.sendMessage("You can't use it when you are in combat.");
					return;
				}
				
				if (!player.isInsideZone(ZoneId.ZONE_PEACE))
				{
					player.sendMessage("You can't use it when you are not in peace zone.");
					return;
				}
				
				if (flag == 0)
				{
					player.setGlow(0);
					player.teleToLocation(player.getX(), player.getY(), player.getZ());
					player.sendPacket(new ExShowScreenMessage("" + Config.ALT_Server_Menu_Name + ": Auto teleport on.", 2000, 0x01, false));
					player.sendMessage("" + Config.ALT_Server_Menu_Name + ": Auto teleport on.");
				}
				else
				{
					player.setGlow(1);
					player.teleToLocation(player.getX(), player.getY(), player.getZ());
					player.sendPacket(new ExShowScreenMessage("" + Config.ALT_Server_Menu_Name + ": Auto teleport off.", 2000, 0x01, false));
					player.sendMessage("" + Config.ALT_Server_Menu_Name + ": Auto teleport off.");
				}
				showHtm(player);
				return;
			}
			case menu_teleport:
			{
				int flag = Integer.parseInt(parameters.trim());
				if (flag == 0)
				{
					player.setTeleport(1);
					player.sendPacket(new ExShowScreenMessage("" + Config.ALT_Server_Menu_Name + ": Auto correction on.", 2000, 0x01, false));
					player.sendMessage("" + Config.ALT_Server_Menu_Name + ": Auto correction on.");
				}
				else
				{
					player.setTeleport(0);
					player.sendPacket(new ExShowScreenMessage("" + Config.ALT_Server_Menu_Name + ": Auto correction off.", 2000, 0x01, false));
					player.sendMessage("" + Config.ALT_Server_Menu_Name + ": Auto correction off.");
				}
				showHtm(player);
				return;
			}
			case menu_effects:
			{
				int flag = Integer.parseInt(parameters.trim());
				if (flag == 0)
				{
					player.setEffects(1);
					player.sendPacket(new ExShowScreenMessage("" + Config.ALT_Server_Menu_Name + ": Effects on.", 2000, 0x01, false));
					player.sendMessage("" + Config.ALT_Server_Menu_Name + ": Hide effects on.");
				}
				else
				{
					player.setEffects(0);
					player.sendPacket(new ExShowScreenMessage("" + Config.ALT_Server_Menu_Name + ": Effects off.", 2000, 0x01, false));
					player.sendMessage("" + Config.ALT_Server_Menu_Name + ": Hide effects off.");
				}
				showHtm(player);
				return;
			}
			case menu_premium:
			{
				showHtmPremium(player);
				return;
			}
			case menu_premium_set:
			{
				int days = Integer.parseInt(parameters.substring(0, 2).trim());
				int price = Integer.parseInt(parameters.substring(2).trim());
				
				if (item == null || player.getInventory().getItemByItemId(currency).getCount() < price)
				{
					player.sendMessage("You don't have enough " + Config.ALT_SERVER_CUSTOM_ITEM_NAME + ".");
					return;
				}
				
				if (player.getPremiumService() == 1)
				{
					player.sendMessage("You already have The Premium Account!");
				}
				else
				{
					player.destroyItem("Consume", item.getObjectId(), price, null, true);
					player.setPremiumService(1);
					updateDatabasePremium(player, days * 24L * 60L * 60L * 1000L);
					player.sendMessage("Congratulation! You're The Premium account now.");
					player.sendPacket(new ExShowScreenMessage("Congratulation! You're The Premium account now.", 4000, 0x07, false));
					PlaySound playSound = new PlaySound("ItemSound.quest_fanfare_1");
					player.sendPacket(playSound);
					if (Config.PREMIUM_NAME_COLOR_ENABLED && player.getPremiumService() == 1)
					{
						player.getAppearance().setTitleColor(Config.PREMIUM_TITLE_COLOR);
					}
					player.sendPacket(new UserInfo(player));
					player.broadcastUserInfo();
				}
				showHtmPremium(player);
				return;
			}
			default:
				break;
		}
	}
	
	private class AutoPot implements Runnable
	{
		public int _id;
		private L2PcInstance _activeChar;
		private float _pTime;
		
		public AutoPot(int id, L2PcInstance activeChar, float pTime)
		{
			_id = id;
			_activeChar = activeChar;
			_pTime = pTime;
		}
		
		@Override
		public void run()
		{
			if (_activeChar.isDead() || _activeChar.isInOlympiadMode())
			{
				return;
			}
			
			try
			{
				if (_activeChar.getInventory().getItemByItemId(_id) == null)
				{
					_activeChar.sendPacket(new ExAutoSoulShot(_id, 0));
					_activeChar.setAutoPot(_id, null, false);
				}
			}
			catch (Exception e)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
				{
					e.printStackTrace();
				}
				
				LOG.warn("Error in menu command with potion: " + _id + " (Name:" + _activeChar.getName() + ") time:" + _pTime + " " + e);
			}
			
			switch (_id)
			{
				case 728:
				{
					if (!_activeChar.isInvul() && (_activeChar.getInventory().getItemByItemId(_id) != null) && _activeChar.getCurrentMp() < _pTime * _activeChar.getMaxMp())
					{
						MagicSkillUser msu = new MagicSkillUser(_activeChar, _activeChar, 2005, 1, 0, 100);
						_activeChar.broadcastPacket(msu);
						
						Potions is = new Potions();
						is.useItem(_activeChar, _activeChar.getInventory().getItemByItemId(_id));
					}
					break;
				}
				case 726:
				{
					if (!_activeChar.isInvul() && (_activeChar.getInventory().getItemByItemId(_id) != null) && _activeChar.getCurrentMp() < _pTime * _activeChar.getMaxMp())
					{
						MagicSkillUser msu = new MagicSkillUser(_activeChar, _activeChar, 2003, 1, 0, 100);
						_activeChar.broadcastPacket(msu);
						
						Potions is = new Potions();
						is.useItem(_activeChar, _activeChar.getInventory().getItemByItemId(_id));
					}
					break;
				}
				case 1539:
				{
					if (!_activeChar.isInvul() && (_activeChar.getInventory().getItemByItemId(_id) != null) && _activeChar.getCurrentHp() < _pTime * _activeChar.getMaxHp())
					{
						MagicSkillUser msu = new MagicSkillUser(_activeChar, _activeChar, 2037, 1, 0, 100);
						_activeChar.broadcastPacket(msu);
						
						Potions is = new Potions();
						is.useItem(_activeChar, _activeChar.getInventory().getItemByItemId(_id));
					}
					break;
				}
				case 1061:
				{
					if (!_activeChar.isInvul() && (_activeChar.getInventory().getItemByItemId(_id) != null) && _activeChar.getCurrentHp() < _pTime * _activeChar.getMaxHp())
					{
						MagicSkillUser msu = new MagicSkillUser(_activeChar, _activeChar, 2032, 1, 0, 100);
						_activeChar.broadcastPacket(msu);
						
						Potions is = new Potions();
						is.useItem(_activeChar, _activeChar.getInventory().getItemByItemId(_id));
					}
					break;
				}
				case 1060:
				{
					if (!_activeChar.isInvul() && (_activeChar.getInventory().getItemByItemId(_id) != null) && _activeChar.getCurrentHp() < _pTime * _activeChar.getMaxHp())
					{
						MagicSkillUser msu = new MagicSkillUser(_activeChar, _activeChar, 2031, 1, 0, 100);
						_activeChar.broadcastPacket(msu);
						
						Potions is = new Potions();
						is.useItem(_activeChar, _activeChar.getInventory().getItemByItemId(_id));
					}
					break;
				}
				case 5592:
				{
					if (!_activeChar.isInvul() && (_activeChar.getInventory().getItemByItemId(5592) != null) && _activeChar.getCurrentCp() < _pTime * _activeChar.getMaxCp())
					{
						MagicSkillUser msu = new MagicSkillUser(_activeChar, _activeChar, 2166, 2, 0, 100);
						_activeChar.broadcastPacket(msu);
						
						Potions is = new Potions();
						is.useItem(_activeChar, _activeChar.getInventory().getItemByItemId(5592));
					}
					break;
				}
				case 5591:
				{
					if (!_activeChar.isInvul() && (_activeChar.getInventory().getItemByItemId(5592) != null) && _activeChar.getCurrentCp() < _pTime * _activeChar.getMaxCp())
					{
						MagicSkillUser msu = new MagicSkillUser(_activeChar, _activeChar, 2166, 1, 0, 100);
						_activeChar.broadcastPacket(msu);
						
						Potions is = new Potions();
						is.useItem(_activeChar, _activeChar.getInventory().getItemByItemId(5592));
					}
					break;
				}
			}
			
			try
			{
				if (_activeChar.getInventory().getItemByItemId(_id) == null)
				{
					_activeChar.sendPacket(new ExAutoSoulShot(_id, 0));
					_activeChar.setAutoPot(_id, null, false);
					return;
				}
			}
			catch (Exception e)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
				{
					e.printStackTrace();
				}
				
				LOG.warn("Error in menu command with potion: " + _id + " (Name:" + _activeChar.getName() + ") time:" + _pTime + " " + e);
			}
		}
	}
	
	private void updateDatabasePremium(L2PcInstance player, long premiumTime)
	{
		Connection con = null;
		try
		{
			if (player == null)
			{
				return;
			}
			
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement stmt = con.prepareStatement("REPLACE INTO account_premium (account_name, premium_service, enddate) VALUES (?,?,?)");
			
			stmt.setString(1, player.getAccountName());
			stmt.setInt(2, 1);
			stmt.setLong(3, premiumTime == 0 ? 0 : System.currentTimeMillis() + premiumTime);
			stmt.execute();
			stmt.close();
			stmt = null;
		}
		catch (Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			
			LOG.warn("[MerchantInstance] Error: could not update database: ", e);
		}
		finally
		{
			CloseUtil.close(con);
		}
	}
	
	public int daysBetween(Date d1, Date d2)
	{
		return (int) ((d2.getTime() - d1.getTime()) / (1000 * 60 * 60 * 24));
	}
}