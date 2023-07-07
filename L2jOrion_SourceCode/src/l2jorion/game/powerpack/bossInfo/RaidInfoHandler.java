/*
 * L2jOrion Project - www.l2jorion.com 
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307, USA.
 *
 * http://www.gnu.org/copyleft/gpl.html
 */
package l2jorion.game.powerpack.bossInfo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.StringTokenizer;

import javolution.text.TextBuilder;
import l2jorion.Config;
import l2jorion.game.cache.HtmCache;
import l2jorion.game.datatables.sql.NpcTable;
import l2jorion.game.handler.ICustomByPassHandler;
import l2jorion.game.handler.IVoicedCommandHandler;
import l2jorion.game.managers.GrandBossManager;
import l2jorion.game.managers.RaidBossSpawnManager;
import l2jorion.game.model.actor.instance.L2GrandBossInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.actor.instance.L2RaidBossInstance;
import l2jorion.game.model.zone.ZoneId;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.ActionFailed;
import l2jorion.game.network.serverpackets.CreatureSay;
import l2jorion.game.network.serverpackets.ExShowScreenMessage;
import l2jorion.game.network.serverpackets.NpcHtmlMessage;
import l2jorion.game.network.serverpackets.PlaySound;
import l2jorion.game.network.serverpackets.ShowMiniMap;
import l2jorion.game.network.serverpackets.SystemMessage;
import l2jorion.game.powerpack.PowerPackConfig;
import l2jorion.game.templates.L2NpcTemplate;
import l2jorion.game.templates.StatsSet;
import l2jorion.game.thread.ThreadPoolManager;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;
import l2jorion.util.CloseUtil;
import l2jorion.util.database.L2DatabaseFactory;

public class RaidInfoHandler implements IVoicedCommandHandler, ICustomByPassHandler
{
	private static Logger LOG = LoggerFactory.getLogger(RaidInfoHandler.class);
	
	private String ROOT = "data/html/mods/boss/";
	private static boolean open = false;
	public int seconds = 1840;
	
	@Override
	public String[] getVoicedCommandList()
	{
		return new String[]
		{
			"boss"
		};
	}
	
	@Override
	public boolean useVoicedCommand(String command, L2PcInstance player, String target)
	{
		if (player == null)
		{
			return false;
		}
		
		if (Config.L2LIMIT_CUSTOM)
		{
			if (player.getPremiumService() == 0 && !player.isInsideZone(ZoneId.ZONE_PEACE))
			{
				String msg = null;
				msg = "You can't use this command outside town.";
				player.sendMessage(msg);
				player.sendPacket(new ExShowScreenMessage(msg, 2000, 2, false));
				player.sendPacket(new PlaySound("ItemSound3.sys_impossible"));
				return false;
			}
		}
		
		if (PowerPackConfig.RESPAWN_BOSS_ONLY_FOR_LORD)
		{
			if (player.getClan() == null || player.getClan() != null && !player.getClan().hasCastle() || player.getClan() != null && !player.getClan().hasHideout())
			{
				player.sendMessage("This command is only for casle or clanhall owners.");
				return true;
			}
		}
		
		if (command.equalsIgnoreCase("boss"))
		{
			showHtm(player);
			open = false;
			return false;
		}
		
		return true;
	}
	
	private void showHtm(L2PcInstance player)
	{
		NpcHtmlMessage htm = new NpcHtmlMessage(1);
		String text = HtmCache.getInstance().getHtm(ROOT + "index.htm");
		htm.setHtml(text);
		player.sendPacket(htm);
	}
	
	private void showRbListHtm(L2PcInstance player)
	{
		NpcHtmlMessage htm = new NpcHtmlMessage(1);
		String text = HtmCache.getInstance().getHtm(ROOT + "rb_list.htm");
		htm.setHtml(text);
		
		if (player.GetSelectedBoss().size() > 0)
		{
			htm.replace("%selected%", "<font color=3399ff>" + player.GetSelectedBoss().get(0) + "</font> <a action=\"bypass -h custom_bosses_rb_list_unselect\">Unselect</a>");
		}
		else
		{
			htm.replace("%selected%", "-");
		}
		
		player.sendPacket(htm);
	}
	
	@Override
	public String[] getByPassCommands()
	{
		return new String[]
		{
			"bosses_gb_list",
			"bosses_rb_list",
			"bosses_rb_list_unselect",
			"bosses_rb_bylevels",
			"view_raid_boss",
			"view_epic_boss",
			"bosses_rb_loc",
			"bosses_index"
		};
	}
	
	private enum CommandEnum
	{
		bosses_gb_list,
		bosses_rb_list,
		bosses_rb_list_unselect,
		bosses_rb_bylevels,
		view_raid_boss,
		view_epic_boss,
		bosses_rb_loc,
		bosses_index
	}
	
	@Override
	public void handleCommand(String command, L2PcInstance player, String parameters)
	{
		StringTokenizer st = new StringTokenizer(command);
		StringTokenizer st2 = new StringTokenizer(parameters, " ");
		StringTokenizer st3 = new StringTokenizer(parameters, "_");
		
		CommandEnum comm = CommandEnum.valueOf(st.nextToken());
		
		if (comm == null)
		{
			return;
		}
		
		switch (comm)
		{
			case bosses_gb_list:
			{
				sendGrandBossesInfo(player);
				break;
			}
			case bosses_rb_list:
			{
				showRbListHtm(player);
				break;
			}
			case bosses_rb_list_unselect:
			{
				player.GetSelectedBoss().clear();
				player.getRadar().removeAllMarkers();
				showRbListHtm(player);
				break;
			}
			case bosses_rb_bylevels:
			{
				String val = "";
				String val2 = "";
				String val3 = "";
				
				if (st2.hasMoreTokens())
				{
					val = st2.nextToken();
					val2 = st2.nextToken();
					val3 = st2.nextToken();
					
					try
					{
						final int minLv = Integer.parseInt(val);
						final int maxLv = Integer.parseInt(val2);
						final int page = Integer.parseInt(val3);
						
						sendRaidBossesInfo(player, minLv, maxLv, page);
						return;
					}
					catch (final NumberFormatException e)
					{
					}
				}
				break;
			}
			case view_epic_boss:
			{
				String x = "";
				String y = "";
				String z = "";
				
				if (st2.hasMoreTokens())
				{
					x = st2.nextToken();
					y = st2.nextToken();
					z = st2.nextToken();
					
					try
					{
						final int x1 = Integer.parseInt(x);
						final int y1 = Integer.parseInt(y);
						final int z1 = Integer.parseInt(z);
						
						doObserve(player, x1, y1, z1, 50);
						return;
					}
					catch (final NumberFormatException e)
					{
					}
				}
				break;
			}
			case view_raid_boss:
			{
				String x = "";
				String y = "";
				String z = "";
				
				if (st2.hasMoreTokens())
				{
					x = st2.nextToken();
					y = st2.nextToken();
					z = st2.nextToken();
					
					try
					{
						final int x1 = Integer.parseInt(x);
						final int y1 = Integer.parseInt(y);
						final int z1 = Integer.parseInt(z);
						
						doObserve(player, x1, y1, z1, 15);
						return;
					}
					catch (final NumberFormatException e)
					{
					}
				}
				break;
			}
			case bosses_rb_loc:
			{
				String x = "";
				String y = "";
				String z = "";
				String name = "";
				
				if (st2.hasMoreTokens())
				{
					x = st2.nextToken();
					y = st2.nextToken();
					z = st2.nextToken();
					name = st3.nextToken();
					
					int locx = Integer.parseInt(x);
					int locy = Integer.parseInt(y);
					int locz = Integer.parseInt(z);
					String locname = name;
					int substring = String.valueOf(locx).length() + String.valueOf(locy).length() + String.valueOf(locz).length() + 3;
					
					player.getRadar().removeAllMarkers();
					player.getRadar().addMarker(locx, locy, locz);
					
					if (!open)
					{
						player.sendPacket(new ShowMiniMap(1665));
						open = true;
					}
					
					player.GetSelectedBoss().clear();
					
					player.GetSelectedBoss().add(locname.substring(substring));
					ThreadPoolManager.getInstance().scheduleGeneral(new loadMarkers(player), 500);
				}
				break;
			}
			case bosses_index:
			{
				showHtm(player);
				open = false;
				break;
			}
		}
	}
	
	private void sendGrandBossesInfo(L2PcInstance activeChar)
	{
		NpcHtmlMessage htm = new NpcHtmlMessage(5);
		htm.setFile(ROOT + "gb_list.htm");
		TextBuilder t = new TextBuilder();
		
		int color = 1;
		for (int boss : PowerPackConfig.RAID_INFO_IDS_LIST)
		{
			String name = "";
			
			StatsSet info = GrandBossManager.getInstance().getStatsSet(boss);
			L2GrandBossInstance grand_boss = GrandBossManager.getInstance().getBoss(boss);
			int x = 0;
			int y = 0;
			int z = 0;
			
			L2NpcTemplate template = null;
			if ((template = NpcTable.getInstance().getTemplate(boss)) != null)
			{
				name = template.getName();
			}
			else
			{
				LOG.warn("Raid Boss with ID " + boss + " is not defined into NpcTable");
				continue;
			}
			
			long delay = info.getLong("respawn_time");
			
			if (grand_boss != null && grand_boss.isChampion())
			{
				name = "<font color=\"ff0000\">" + name + "</font>";
			}
			
			name = name + "&nbsp;<font color=\"ffff00\">" + template.getLevel() + "</font>";
			
			if (grand_boss != null && grand_boss.getSpawn() != null)
			{
				x = grand_boss.getSpawn().getLocx();
				y = grand_boss.getSpawn().getLocy();
				z = grand_boss.getSpawn().getLocz();
			}
			else
			{
				x = info.getInteger("loc_x");
				y = info.getInteger("loc_y");
				z = info.getInteger("loc_z");
			}
			
			if (color == 1)
			{
				t.append("<table width=300 border=0 bgcolor=000000><tr>");
				t.append("<td width=150>" + name + "</td><td width=110>" + (Config.RON_CUSTOM ? "" : (delay <= System.currentTimeMillis() ? "<font color=\"009900\">Alive</font>" : "<font color=\"FF0000\">Dead: " + GetGrandBossKilledTime(boss)) + "</font></td>") + //
					"" + (Config.RON_CUSTOM ? "<td width=40>[<a action=\"bypass custom_view_epic_boss " + x + " " + y + " " + z + "\">View</a>] <font color=LEVEL>50</font> PCB Points</td>" : "") + "");
				t.append("</tr></table>");
				color = 2;
			}
			else
			{
				t.append("<table width=300 border=0><tr>");
				t.append("<td width=150>" + name + "</td><td width=110>" + (Config.RON_CUSTOM ? "" : (delay <= System.currentTimeMillis() ? "<font color=\"009900\">Alive</font>" : "<font color=\"FF0000\">Dead: " + GetGrandBossKilledTime(boss)) + "</font></td>") + //
					"" + (Config.RON_CUSTOM ? "<td width=40>[<a action=\"bypass custom_view_epic_boss " + x + " " + y + " " + z + "\">View</a>] <font color=LEVEL>50</font> PCB Points</td>" : "") + "");
				t.append("</tr></table>");
				color = 1;
			}
		}
		htm.replace("%bosses%", t.toString());
		activeChar.sendPacket(htm);
	}
	
	private void sendRaidBossesInfo(L2PcInstance activeChar, int minLv, int maxLv, int page)
	{
		final Collection<L2RaidBossInstance> rBosses = RaidBossSpawnManager._bossesForCommand.values();
		
		RaidBossSpawnManager.BOSSES_LIST.clear();
		
		for (final L2RaidBossInstance actual_boss : rBosses)
		{
			if ((actual_boss.getLevel() >= minLv && actual_boss.getLevel() <= maxLv))
			{
				RaidBossSpawnManager.BOSSES_LIST.add(actual_boss);
			}
		}
		
		RaidBossSpawnManager.BOSSES_LIST.sort((o1, o2) -> String.valueOf(o1.getLevel()).compareTo(String.valueOf(o2.getLevel())));
		
		L2RaidBossInstance[] bossses = RaidBossSpawnManager.BOSSES_LIST.toArray(new L2RaidBossInstance[RaidBossSpawnManager.BOSSES_LIST.size()]);
		
		final int MaxCharactersPerPage = 15;
		int MaxPages = bossses.length / MaxCharactersPerPage;
		
		if (bossses.length > MaxCharactersPerPage * MaxPages)
		{
			MaxPages++;
		}
		
		if (page > MaxPages)
		{
			page = MaxPages;
		}
		
		final int CharactersStart = MaxCharactersPerPage * page;
		int CharactersEnd = bossses.length;
		
		if (CharactersEnd - CharactersStart > MaxCharactersPerPage)
		{
			CharactersEnd = CharactersStart + MaxCharactersPerPage;
		}
		
		NpcHtmlMessage html = new NpcHtmlMessage(6);
		html.setFile(ROOT + "rb_list_bylevels.htm");
		TextBuilder th = new TextBuilder();
		
		int count = CharactersStart;
		int color = 1;
		for (int i = CharactersStart; i < CharactersEnd; i++)
		{
			count++;
			
			String name = "";
			int rboss = bossses[i].getNpcId();
			L2NpcTemplate template = NpcTable.getInstance().getTemplate(rboss);
			StatsSet rInfo = RaidBossSpawnManager.getInstance().getStatsSet(rboss);
			L2RaidBossInstance raid_boss = RaidBossSpawnManager.getInstance().getBoss(rboss);
			long delay = 0;
			String deadTime = "";
			int x = 0;
			int y = 0;
			int z = 0;
			
			if (rInfo != null)
			{
				name = template.getName();
				
				String locname = template.getName();
				
				if (name.length() >= 16)
				{
					name = name.substring(0, 16) + "...";
				}
				
				if (raid_boss != null && raid_boss.isChampion())
				{
					name = "<font color=\"ff0000\">" + name + "</font>";
				}
				
				name = name + "&nbsp;<font color=\"ffff00\">" + template.getLevel() + "</font>";
				
				if (template.aggroRange > 0)
				{
					name = name + "<font color=\"ff0000\">*</font>";
				}
				
				delay = rInfo.getLong("respawnTime");
				deadTime = GetRaidBossKilledTime(rboss);
				
				if (raid_boss != null && raid_boss.getSpawn() != null)
				{
					x = raid_boss.getSpawn().getLocx();
					y = raid_boss.getSpawn().getLocy();
					z = raid_boss.getSpawn().getLocz();
				}
				else
				{
					x = rInfo.getInteger("loc_x");
					y = rInfo.getInteger("loc_y");
					z = rInfo.getInteger("loc_z");
				}
				
				if (color == 1)
				{
					th.append("<table width=300 border=0 bgcolor=000000><tr>");
					th.append("<td width=20>" + count + ".</td><td width=130><a action=\"bypass custom_bosses_rb_loc " + x + " " + y + " " + z + " " + locname + "\">" + name + "</a></td>"
						+ (Config.RON_CUSTOM ? "" : "<td width=110>" + (delay <= System.currentTimeMillis() ? "<font color=\"009900\">Alive</font>" : "<font color=\"FF0000\">Dead: " + deadTime + "</font>") + "</td>") //
						+ (Config.RON_CUSTOM ? "<td width=120>[<a action=\"bypass custom_view_raid_boss " + x + " " + y + " " + z + "\">View</a>] <font color=LEVEL>15</font> PCB Points</td>" : "") + "");
					th.append("</tr></table>");
					color = 2;
				}
				else
				{
					th.append("<table width=300 border=0><tr>");
					th.append("<td width=20>" + count + ".</td><td width=130><a action=\"bypass custom_bosses_rb_loc " + x + " " + y + " " + z + " " + locname + "\">" + name + "</a></td>"
						+ (Config.RON_CUSTOM ? "" : "<td width=110>" + (delay <= System.currentTimeMillis() ? "<font color=\"009900\">Alive</font>" : "<font color=\"FF0000\">Dead: " + deadTime + "</font>") + "</td>") //
						+ (Config.RON_CUSTOM ? "<td width=120>[<a action=\"bypass custom_view_raid_boss " + x + " " + y + " " + z + "\">View</a>] <font color=LEVEL>15</font> PCB Points</td>" : "") + "");
					th.append("</tr></table>");
					color = 1;
				}
			}
		}
		
		html.replace("%raidbosses%", th.toString());
		
		th.clear();
		
		th.append("<center><table><tr>");
		for (int x = 0; x < MaxPages; x++)
		{
			final int pagenr = x + 1;
			if (page == x)
			{
				th.append("<td width=20>[" + pagenr + "]&nbsp;&nbsp;</td>");
			}
			else
			{
				th.append("<td width=20><a action=\"bypass -h custom_bosses_rb_bylevels " + minLv + " " + maxLv + " " + x + "\">[" + pagenr + "]</a>&nbsp;&nbsp;</td>");
			}
		}
		th.append("</tr></table></center>");
		
		html.replace("%pages%", th.toString());
		
		activeChar.sendPacket(html);
	}
	
	public static String GetGrandBossKilledTime(int BossId)
	{
		Connection con = null;
		String killed_time = "";
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT killed_time FROM grandboss_data WHERE boss_id=?");
			statement.setInt(1, BossId);
			ResultSet rset = statement.executeQuery();
			rset.next();
			killed_time = rset.getString("killed_time");
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
		
		return killed_time;
	}
	
	public static String GetRaidBossKilledTime(int BossId)
	{
		Connection con = null;
		String killed_time = "";
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT killed_time FROM raidboss_spawnlist WHERE boss_id=?");
			statement.setInt(1, BossId);
			ResultSet rset = statement.executeQuery();
			rset.next();
			killed_time = rset.getString("killed_time");
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
		
		return killed_time;
	}
	
	public static class loadMarkers implements Runnable
	{
		private final L2PcInstance _me;
		
		public loadMarkers(L2PcInstance me)
		{
			_me = me;
		}
		
		@Override
		public void run()
		{
			try
			{
				_me.getRadar().loadMarkers();
				_me.sendPacket(new CreatureSay(0, 17, "Your selected Boss", _me.GetSelectedBoss().get(0)));
			}
			catch (final Throwable t)
			{
			}
		}
	}
	
	private void doObserve(L2PcInstance player, int x, int y, int z, int price)
	{
		if (player.getPcBangScore() < price)
		{
			player.sendMessage("You don't have enough PC Bang Points. Required:" + price);
			player.sendPacket(new ExShowScreenMessage("You don't have enough PC Bang Points. Required:" + price, 1000, 2, false));
			player.sendPacket(new PlaySound("ItemSound3.sys_impossible"));
			return;
		}
		
		player.reducePcBangScore(price);
		player.sendPacket(new SystemMessage(SystemMessageId.USING_S1_PCPOINT).addNumber(price));
		
		player.enterObserverMode(x, y, z);
		player.setBossTaskNull();
		if (player.getBossTask() == null)
		{
			player.setBossObserve(true);
			player.sendMessage("Time left: 30 minutes");
			player.sendPacket(new ExShowScreenMessage("Time left: 30 minutes", 5000, 3, false));
			player.startBossTask();
		}
		
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	public void bossObserveTimer(L2PcInstance player)
	{
		if (seconds > 0)
		{
			seconds--;
		}
		
		switch (seconds)
		{
			case 1900:
			case 1840:
			case 1780:
			case 1720:
			case 1660:
			case 1600:
			case 1540:
			case 1480:
			case 1420:
			case 1380:
			case 1320:
			case 1260:
			case 1200:
			case 1140:
			case 1080:
			case 1020:
			case 960:
			case 900:
			case 840:
			case 780:
			case 720:
			case 660:
			case 600:
			case 540:
			case 480:
			case 420:
			case 360:
			case 300:
			case 240:
			case 180:
			case 120:
				player.sendMessage("Time left: " + (seconds / 60) + " minutes");
				player.sendPacket(new ExShowScreenMessage("Time left: " + (seconds / 60) + " minutes", 5000, 3, false));
				break;
			case 60:
				player.sendMessage("Time left: " + (seconds / 60) + " minute");
				player.sendPacket(new ExShowScreenMessage("Time left: " + (seconds / 60) + " minute", 5000, 3, false));
				break;
			case 30:
			case 15:
			case 10:
			case 3:
			case 2:
				player.sendMessage("Time left: " + (seconds) + " seconds");
				player.sendPacket(new ExShowScreenMessage("Time left: " + (seconds) + " seconds", 5000, 3, false));
				break;
			case 1:
				player.sendMessage("Time left: " + (seconds) + " seconds");
				player.sendPacket(new ExShowScreenMessage("Time left: " + (seconds) + " seconds", 5000, 3, false));
				break;
			case 0:
				player.leaveObserverMode();
				player.setBossObserve(false);
				player.setBossTaskNull();
				player.sendMessage("Teleporting back");
				player.sendPacket(new ExShowScreenMessage("Teleporting back", 2000, 2, false));
				break;
		}
	}
	
	public static RaidInfoHandler getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final RaidInfoHandler INSTANCE = new RaidInfoHandler();
	}
}
