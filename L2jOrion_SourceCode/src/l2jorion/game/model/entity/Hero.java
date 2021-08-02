/*
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
package l2jorion.game.model.entity;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import l2jorion.game.datatables.sql.CharNameTable;
import l2jorion.game.datatables.sql.CharTemplateTable;
import l2jorion.game.datatables.sql.ClanTable;
import l2jorion.game.datatables.sql.NpcTable;
import l2jorion.game.enums.AchType;
import l2jorion.game.managers.CastleManager;
import l2jorion.game.model.Inventory;
import l2jorion.game.model.L2Clan;
import l2jorion.game.model.L2World;
import l2jorion.game.model.actor.instance.L2ItemInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.entity.siege.Castle;
import l2jorion.game.model.olympiad.Olympiad;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.NpcHtmlMessage;
import l2jorion.game.network.serverpackets.PledgeShowInfoUpdate;
import l2jorion.game.network.serverpackets.SocialAction;
import l2jorion.game.network.serverpackets.SystemMessage;
import l2jorion.game.templates.L2NpcTemplate;
import l2jorion.game.templates.StatsSet;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;
import l2jorion.util.StringUtil;
import l2jorion.util.database.L2DatabaseFactory;

public class Hero
{
	protected static final Logger LOG = LoggerFactory.getLogger(Hero.class);
	
	private static final String GET_HEROES = "SELECT heroes.char_id, characters.char_name, heroes.class_id, heroes.count, heroes.played, heroes.active FROM heroes, characters WHERE characters.obj_Id = heroes.char_id AND heroes.played = 1";
	private static final String GET_ALL_HEROES = "SELECT heroes.char_id, characters.char_name, heroes.class_id, heroes.count, heroes.played, heroes.active FROM heroes, characters WHERE characters.obj_Id = heroes.char_id";
	private static final String UPDATE_ALL = "UPDATE heroes SET played = 0";
	private static final String INSERT_HERO = "INSERT INTO heroes (char_id, class_id, count, played, active) VALUES (?,?,?,?,?)";
	private static final String UPDATE_HERO = "UPDATE heroes SET count = ?, played = ?, active = ? WHERE char_id = ?";
	private static final String GET_CLAN_ALLY = "SELECT characters.clanid AS clanid, coalesce(clan_data.ally_Id, 0) AS allyId FROM characters LEFT JOIN clan_data ON clan_data.clan_id = characters.clanid WHERE characters.obj_Id = ?";
	
	private static final String DELETE_ITEMS = "DELETE FROM items WHERE item_id IN (6842, 6611, 6612, 6613, 6614, 6615, 6616, 6617, 6618, 6619, 6620, 6621) AND owner_id NOT IN (SELECT obj_Id FROM characters WHERE accesslevel > 0)";
	
	private static final String GET_DIARIES = "SELECT * FROM  heroes_diary WHERE char_id=? ORDER BY time ASC";
	private static final String UPDATE_DIARIES = "INSERT INTO heroes_diary (char_id, time, action, param) values(?,?,?,?)";
	
	public static final String COUNT = "count";
	public static final String PLAYED = "played";
	public static final String CLAN_NAME = "clan_name";
	public static final String CLAN_CREST = "clan_crest";
	public static final String ALLY_NAME = "ally_name";
	public static final String ALLY_CREST = "ally_crest";
	public static final String ACTIVE = "active";
	
	public static final int ACTION_RAID_KILLED = 1;
	public static final int ACTION_HERO_GAINED = 2;
	public static final int ACTION_CASTLE_TAKEN = 3;
	
	private final Map<Integer, StatsSet> _heroes = new HashMap<>();
	private final Map<Integer, StatsSet> _completeHeroes = new HashMap<>();
	
	private final Map<Integer, StatsSet> _heroCounts = new HashMap<>();
	private final Map<Integer, List<StatsSet>> _heroFights = new HashMap<>();
	private final List<StatsSet> _fights = new ArrayList<>();
	
	private final Map<Integer, List<StatsSet>> _heroDiaries = new HashMap<>();
	private final Map<Integer, String> _heroMessages = new HashMap<>();
	private final List<StatsSet> _diary = new ArrayList<>();
	
	protected Hero()
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			PreparedStatement statement = con.prepareStatement(GET_HEROES);
			ResultSet rset = statement.executeQuery();
			PreparedStatement statement2 = con.prepareStatement(GET_CLAN_ALLY);
			ResultSet rset2 = null;
			
			while (rset.next())
			{
				StatsSet hero = new StatsSet();
				int charId = rset.getInt(Olympiad.CHAR_ID);
				hero.set(Olympiad.CHAR_NAME, rset.getString(Olympiad.CHAR_NAME));
				hero.set(Olympiad.CLASS_ID, rset.getInt(Olympiad.CLASS_ID));
				hero.set(COUNT, rset.getInt(COUNT));
				hero.set(PLAYED, rset.getInt(PLAYED));
				hero.set(ACTIVE, rset.getInt(ACTIVE));
				
				loadFights(charId);
				loadDiary(charId);
				loadMessage(charId);
				
				statement2.setInt(1, charId);
				rset2 = statement2.executeQuery();
				
				if (rset2.next())
				{
					int clanId = rset2.getInt("clanid");
					int allyId = rset2.getInt("allyId");
					
					String clanName = "";
					String allyName = "";
					int clanCrest = 0;
					int allyCrest = 0;
					
					if (clanId > 0)
					{
						clanName = ClanTable.getInstance().getClan(clanId).getName();
						clanCrest = ClanTable.getInstance().getClan(clanId).getCrestId();
						
						if (allyId > 0)
						{
							allyName = ClanTable.getInstance().getClan(clanId).getAllyName();
							allyCrest = ClanTable.getInstance().getClan(clanId).getAllyCrestId();
						}
					}
					
					hero.set(CLAN_CREST, clanCrest);
					hero.set(CLAN_NAME, clanName);
					hero.set(ALLY_CREST, allyCrest);
					hero.set(ALLY_NAME, allyName);
				}
				
				rset2.close();
				statement2.clearParameters();
				
				_heroes.put(charId, hero);
			}
			
			rset.close();
			statement.close();
			
			statement = con.prepareStatement(GET_ALL_HEROES);
			rset = statement.executeQuery();
			
			while (rset.next())
			{
				StatsSet hero = new StatsSet();
				int charId = rset.getInt(Olympiad.CHAR_ID);
				hero.set(Olympiad.CHAR_NAME, rset.getString(Olympiad.CHAR_NAME));
				hero.set(Olympiad.CLASS_ID, rset.getInt(Olympiad.CLASS_ID));
				hero.set(COUNT, rset.getInt(COUNT));
				hero.set(PLAYED, rset.getInt(PLAYED));
				hero.set(ACTIVE, rset.getInt(ACTIVE));
				
				statement2.setInt(1, charId);
				rset2 = statement2.executeQuery();
				
				if (rset2.next())
				{
					int clanId = rset2.getInt("clanid");
					int allyId = rset2.getInt("allyId");
					
					String clanName = "";
					String allyName = "";
					int clanCrest = 0;
					int allyCrest = 0;
					
					if (clanId > 0)
					{
						clanName = ClanTable.getInstance().getClan(clanId).getName();
						clanCrest = ClanTable.getInstance().getClan(clanId).getCrestId();
						
						if (allyId > 0)
						{
							allyName = ClanTable.getInstance().getClan(clanId).getAllyName();
							allyCrest = ClanTable.getInstance().getClan(clanId).getAllyCrestId();
						}
					}
					
					hero.set(CLAN_CREST, clanCrest);
					hero.set(CLAN_NAME, clanName);
					hero.set(ALLY_CREST, allyCrest);
					hero.set(ALLY_NAME, allyName);
				}
				
				rset2.close();
				statement2.clearParameters();
				
				_completeHeroes.put(charId, hero);
			}
			
			statement2.close();
			rset.close();
			statement.close();
		}
		catch (SQLException e)
		{
			LOG.warn("Hero: Couldnt load heroes: " + e.getMessage(), e);
		}
		
		LOG.info("Hero: Loaded " + _heroes.size() + " heroes.");
		LOG.info("Hero: Loaded " + _completeHeroes.size() + " all time heroes.");
	}
	
	private static String calcFightTime(long fightTime)
	{
		String format = String.format("%%0%dd", 2);
		fightTime = fightTime / 1000;
		String seconds = String.format(format, fightTime % 60);
		String minutes = String.format(format, (fightTime % 3600) / 60);
		String time = minutes + ":" + seconds;
		return time;
	}
	
	/**
	 * Restore hero message from Db.
	 * @param charId
	 */
	public void loadMessage(int charId)
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			PreparedStatement statement = con.prepareStatement("SELECT message FROM heroes WHERE char_id=?");
			statement.setInt(1, charId);
			
			ResultSet rset = statement.executeQuery();
			rset.next();
			
			_heroMessages.put(charId, rset.getString("message"));
			
			rset.close();
			statement.close();
		}
		catch (SQLException e)
		{
			LOG.warn("Hero: Couldnt load hero message for char_id: " + charId, e);
		}
	}
	
	public void loadDiary(int charId)
	{
		int entries = 0;
		
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			PreparedStatement statement = con.prepareStatement(GET_DIARIES);
			statement.setInt(1, charId);
			
			ResultSet rset = statement.executeQuery();
			while (rset.next())
			{
				StatsSet entry = new StatsSet();
				
				long time = rset.getLong("time");
				int action = rset.getInt("action");
				int param = rset.getInt("param");
				
				entry.set("date", new SimpleDateFormat("yyyy-MM-dd HH").format(time));
				
				if (action == ACTION_RAID_KILLED)
				{
					L2NpcTemplate template = NpcTable.getInstance().getTemplate(param);
					if (template != null)
					{
						entry.set("action", template.getName() + " was defeated");
					}
				}
				else if (action == ACTION_HERO_GAINED)
				{
					entry.set("action", "Gained Hero status");
				}
				else if (action == ACTION_CASTLE_TAKEN)
				{
					Castle castle = CastleManager.getInstance().getCastleById(param);
					if (castle != null)
					{
						entry.set("action", castle.getName() + " Castle was successfuly taken");
					}
				}
				_diary.add(entry);
				
				entries++;
			}
			rset.close();
			statement.close();
			
			_heroDiaries.put(charId, _diary);
			
			LOG.info("Hero: Loaded " + entries + " diary entries for hero: " + CharNameTable.getInstance().getNameById(charId));
		}
		catch (SQLException e)
		{
			LOG.warn("Hero: Couldnt load hero diary for char_id: " + charId + ", " + e.getMessage(), e);
		}
	}
	
	public void loadFights(int charId)
	{
		StatsSet heroCountData = new StatsSet();
		
		Calendar data = Calendar.getInstance();
		data.set(Calendar.DAY_OF_MONTH, 1);
		data.set(Calendar.HOUR_OF_DAY, 0);
		data.set(Calendar.MINUTE, 0);
		data.set(Calendar.MILLISECOND, 0);
		
		long from = data.getTimeInMillis();
		int numberOfFights = 0;
		int victories = 0;
		int losses = 0;
		int draws = 0;
		
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			PreparedStatement statement = con.prepareStatement("SELECT * FROM olympiad_fights WHERE (charOneId=? OR charTwoId=?) AND start<? ORDER BY start ASC");
			statement.setInt(1, charId);
			statement.setInt(2, charId);
			statement.setLong(3, from);
			ResultSet rset = statement.executeQuery();
			
			while (rset.next())
			{
				int charOneId = rset.getInt("charOneId");
				int charOneClass = rset.getInt("charOneClass");
				int charTwoId = rset.getInt("charTwoId");
				int charTwoClass = rset.getInt("charTwoClass");
				int winner = rset.getInt("winner");
				long start = rset.getLong("start");
				int time = rset.getInt("time");
				int classed = rset.getInt("classed");
				
				if (charId == charOneId)
				{
					String name = CharNameTable.getInstance().getNameById(charTwoId);
					String cls = CharTemplateTable.getClassNameById(charTwoClass);
					
					if (name != null && cls != null)
					{
						StatsSet fight = new StatsSet();
						fight.set("oponent", name);
						fight.set("oponentclass", cls);
						
						fight.set("time", calcFightTime(time));
						fight.set("start", new SimpleDateFormat("yyyy-MM-dd HH:mm").format(start));
						
						fight.set("classed", classed);
						if (winner == 1)
						{
							fight.set("result", "<font color=\"00ff00\">victory</font>");
							victories++;
						}
						else if (winner == 2)
						{
							fight.set("result", "<font color=\"ff0000\">loss</font>");
							losses++;
						}
						else if (winner == 0)
						{
							fight.set("result", "<font color=\"ffff00\">draw</font>");
							draws++;
						}
						
						_fights.add(fight);
						
						numberOfFights++;
					}
				}
				else if (charId == charTwoId)
				{
					String name = CharNameTable.getInstance().getNameById(charOneId);
					String cls = CharTemplateTable.getClassNameById(charOneClass);
					
					if (name != null && cls != null)
					{
						StatsSet fight = new StatsSet();
						fight.set("oponent", name);
						fight.set("oponentclass", cls);
						
						fight.set("time", calcFightTime(time));
						fight.set("start", new SimpleDateFormat("yyyy-MM-dd HH:mm").format(start));
						
						fight.set("classed", classed);
						if (winner == 1)
						{
							fight.set("result", "<font color=\"ff0000\">loss</font>");
							losses++;
						}
						else if (winner == 2)
						{
							fight.set("result", "<font color=\"00ff00\">victory</font>");
							victories++;
						}
						else if (winner == 0)
						{
							fight.set("result", "<font color=\"ffff00\">draw</font>");
							draws++;
						}
						
						_fights.add(fight);
						
						numberOfFights++;
					}
				}
			}
			rset.close();
			statement.close();
			
			heroCountData.set("victory", victories);
			heroCountData.set("draw", draws);
			heroCountData.set("loss", losses);
			
			_heroCounts.put(charId, heroCountData);
			_heroFights.put(charId, _fights);
			
			LOG.info("Hero: Loaded " + numberOfFights + " fights for: " + CharNameTable.getInstance().getNameById(charId));
		}
		catch (SQLException e)
		{
			LOG.warn("Hero: Couldnt load hero fights history for char_id: " + charId + ", " + e.getMessage(), e);
		}
	}
	
	public Map<Integer, StatsSet> getHeroes()
	{
		return _heroes;
	}
	
	public int getHeroByClass(int classid)
	{
		if (!_heroes.isEmpty())
		{
			for (Map.Entry<Integer, StatsSet> heroEntry : _heroes.entrySet())
			{
				if (heroEntry.getValue().getInteger(Olympiad.CLASS_ID) == classid)
				{
					return heroEntry.getKey();
				}
			}
		}
		return 0;
	}
	
	public void resetData()
	{
		_heroDiaries.clear();
		_heroFights.clear();
		_heroCounts.clear();
		_heroMessages.clear();
	}
	
	public void showHeroDiary(L2PcInstance activeChar, int heroclass, int charid, int page)
	{
		if (!_heroDiaries.containsKey(charid))
		{
			return;
		}
		
		final int perpage = 10;
		
		List<StatsSet> mainList = _heroDiaries.get(charid);
		
		final NpcHtmlMessage html = new NpcHtmlMessage(0);
		html.setFile("data/html/olympiad/herodiary.htm");
		html.replace("%heroname%", CharNameTable.getInstance().getNameById(charid));
		html.replace("%message%", _heroMessages.get(charid));
		
		// html.disableValidation();
		
		if (!mainList.isEmpty())
		{
			List<StatsSet> list = new ArrayList<>();
			list.addAll(mainList);
			Collections.reverse(list);
			
			boolean color = true;
			int counter = 0;
			int breakat = 0;
			
			final StringBuilder sb = new StringBuilder(500);
			for (int i = ((page - 1) * perpage); i < list.size(); i++)
			{
				breakat = i;
				StatsSet _diaryentry = list.get(i);
				StringUtil.append(sb, "<tr><td>", ((color) ? "<table width=270 bgcolor=\"131210\">" : "<table width=270>"), "<tr><td width=270><font color=\"LEVEL\">", _diaryentry.getString("date"), ":xx</font></td></tr><tr><td width=270>", _diaryentry.getString("action"), "</td></tr><tr><td>&nbsp;</td></tr></table></td></tr>");
				color = !color;
				
				counter++;
				if (counter >= perpage)
				{
					break;
				}
			}
			
			if (breakat < (list.size() - 1))
			{
				html.replace("%buttprev%", "<button value=\"Prev\" action=\"bypass _diary?class=" + heroclass + "&page=" + (page + 1) + "\" width=60 height=25 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">");
			}
			else
			{
				html.replace("%buttprev%", "");
			}
			
			if (page > 1)
			{
				html.replace("%buttnext%", "<button value=\"Next\" action=\"bypass _diary?class=" + heroclass + "&page=" + (page - 1) + "\" width=60 height=25 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">");
			}
			else
			{
				html.replace("%buttnext%", "");
			}
			
			html.replace("%list%", sb.toString());
		}
		else
		{
			html.replace("%list%", "");
			html.replace("%buttprev%", "");
			html.replace("%buttnext%", "");
		}
		activeChar.sendPacket(html);
	}
	
	public void showHeroFights(L2PcInstance activeChar, int heroclass, int charid, int page)
	{
		if (!_heroFights.containsKey(charid))
		{
			return;
		}
		
		final int perpage = 20;
		int win = 0;
		int loss = 0;
		int draw = 0;
		
		List<StatsSet> list = _heroFights.get(charid);
		
		final NpcHtmlMessage html = new NpcHtmlMessage(0);
		html.setFile("data/html/olympiad/herohistory.htm");
		html.replace("%heroname%", CharNameTable.getInstance().getNameById(charid));
		
		// html.disableValidation();
		
		if (!list.isEmpty())
		{
			if (_heroCounts.containsKey(charid))
			{
				StatsSet _herocount = _heroCounts.get(charid);
				win = _herocount.getInteger("victory");
				loss = _herocount.getInteger("loss");
				draw = _herocount.getInteger("draw");
			}
			
			boolean color = true;
			int counter = 0;
			int breakat = 0;
			
			final StringBuilder sb = new StringBuilder(500);
			for (int i = ((page - 1) * perpage); i < list.size(); i++)
			{
				breakat = i;
				StatsSet fight = list.get(i);
				StringUtil.append(sb, "<tr><td>", ((color) ? "<table width=270 bgcolor=\"131210\">" : "<table width=270><tr><td width=220><font color=\"LEVEL\">"), fight.getString("start"), "</font>&nbsp;&nbsp;", fight.getString("result"), "</td><td width=50 align=right>", ((fight.getInteger("classed") > 0) ? "<font color=\"FFFF99\">cls</font>" : "<font color=\"999999\">non-cls<font>"), "</td></tr><tr><td width=220>vs ", fight.getString("oponent"), " (", fight.getString("oponentclass"), ")</td><td width=50 align=right>(", fight.getString("time"), ")</td></tr><tr><td colspan=2>&nbsp;</td></tr></table></td></tr>");
				color = !color;
				
				counter++;
				if (counter >= perpage)
				{
					break;
				}
			}
			
			if (breakat < (list.size() - 1))
			{
				html.replace("%buttprev%", "<button value=\"Prev\" action=\"bypass _match?class=" + heroclass + "&page=" + (page + 1) + "\" width=60 height=25 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">");
			}
			else
			{
				html.replace("%buttprev%", "");
			}
			
			if (page > 1)
			{
				html.replace("%buttnext%", "<button value=\"Next\" action=\"bypass _match?class=" + heroclass + "&page=" + (page - 1) + "\" width=60 height=25 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">");
			}
			else
			{
				html.replace("%buttnext%", "");
			}
			
			html.replace("%list%", sb.toString());
		}
		else
		{
			html.replace("%list%", "");
			html.replace("%buttprev%", "");
			html.replace("%buttnext%", "");
		}
		
		html.replace("%win%", win);
		html.replace("%draw%", draw);
		html.replace("%loos%", loss);
		
		activeChar.sendPacket(html);
	}
	
	public synchronized void computeNewHeroes(List<StatsSet> newHeroes)
	{
		updateHeroes(true);
		
		if (!_heroes.isEmpty())
		{
			for (StatsSet hero : _heroes.values())
			{
				String name = hero.getString(Olympiad.CHAR_NAME);
				
				L2PcInstance player = L2World.getInstance().getPlayer(name);
				if (player == null)
				{
					continue;
				}
				
				player.setHero(false);
				
				// Unequip hero items, if found.
				for (int i = 0; i < Inventory.PAPERDOLL_TOTALSLOTS; i++)
				{
					L2ItemInstance equippedItem = player.getInventory().getPaperdollItem(i);
					if (equippedItem != null && equippedItem.isHeroItem())
					{
						player.getInventory().unEquipItemInSlot(i);
					}
				}
				
				// Check inventory items.
				for (L2ItemInstance item : player.getInventory().getAvailableItems(false))
				{
					if (!item.isHeroItem())
					{
						continue;
					}
					
					player.destroyItem("Hero", item, null, true);
				}
				
				player.broadcastUserInfo();
			}
		}
		
		if (newHeroes.isEmpty())
		{
			_heroes.clear();
			return;
		}
		
		Map<Integer, StatsSet> heroes = new HashMap<>();
		
		for (StatsSet hero : newHeroes)
		{
			int charId = hero.getInteger(Olympiad.CHAR_ID);
			
			if (_completeHeroes.containsKey(charId))
			{
				StatsSet oldHero = _completeHeroes.get(charId);
				int count = oldHero.getInteger(COUNT);
				oldHero.set(COUNT, count + 1);
				oldHero.set(PLAYED, 1);
				oldHero.set(ACTIVE, 0);
				
				heroes.put(charId, oldHero);
			}
			else
			{
				StatsSet newHero = new StatsSet();
				newHero.set(Olympiad.CHAR_NAME, hero.getString(Olympiad.CHAR_NAME));
				newHero.set(Olympiad.CLASS_ID, hero.getInteger(Olympiad.CLASS_ID));
				newHero.set(COUNT, 1);
				newHero.set(PLAYED, 1);
				newHero.set(ACTIVE, 0);
				
				heroes.put(charId, newHero);
			}
		}
		
		deleteItemsInDb();
		
		_heroes.clear();
		_heroes.putAll(heroes);
		
		heroes.clear();
		
		updateHeroes(false);
	}
	
	public void updateHeroes(boolean setDefault)
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			if (setDefault)
			{
				PreparedStatement statement = con.prepareStatement(UPDATE_ALL);
				statement.execute();
				statement.close();
			}
			else
			{
				PreparedStatement statement;
				
				for (Map.Entry<Integer, StatsSet> heroEntry : _heroes.entrySet())
				{
					final int heroId = heroEntry.getKey();
					final StatsSet hero = heroEntry.getValue();
					
					if (!_completeHeroes.containsKey(heroId))
					{
						statement = con.prepareStatement(INSERT_HERO);
						statement.setInt(1, heroId);
						statement.setInt(2, hero.getInteger(Olympiad.CLASS_ID));
						statement.setInt(3, hero.getInteger(COUNT));
						statement.setInt(4, hero.getInteger(PLAYED));
						statement.setInt(5, hero.getInteger(ACTIVE));
						statement.execute();
						statement.close();
						
						statement = con.prepareStatement(GET_CLAN_ALLY);
						statement.setInt(1, heroId);
						ResultSet rset = statement.executeQuery();
						
						if (rset.next())
						{
							int clanId = rset.getInt("clanid");
							int allyId = rset.getInt("allyId");
							
							String clanName = "";
							String allyName = "";
							int clanCrest = 0;
							int allyCrest = 0;
							
							if (clanId > 0)
							{
								clanName = ClanTable.getInstance().getClan(clanId).getName();
								clanCrest = ClanTable.getInstance().getClan(clanId).getCrestId();
								
								if (allyId > 0)
								{
									allyName = ClanTable.getInstance().getClan(clanId).getAllyName();
									allyCrest = ClanTable.getInstance().getClan(clanId).getAllyCrestId();
								}
							}
							
							hero.set(CLAN_CREST, clanCrest);
							hero.set(CLAN_NAME, clanName);
							hero.set(ALLY_CREST, allyCrest);
							hero.set(ALLY_NAME, allyName);
						}
						
						rset.close();
						statement.close();
						
						_heroes.put(heroId, hero);
						_completeHeroes.put(heroId, hero);
					}
					else
					{
						statement = con.prepareStatement(UPDATE_HERO);
						statement.setInt(1, hero.getInteger(COUNT));
						statement.setInt(2, hero.getInteger(PLAYED));
						statement.setInt(3, hero.getInteger(ACTIVE));
						statement.setInt(4, heroId);
						statement.execute();
						statement.close();
					}
				}
			}
		}
		catch (SQLException e)
		{
			LOG.warn("Hero: Couldn't update heroes: " + e.getMessage(), e);
		}
	}
	
	public void setHeroGained(int charId)
	{
		setDiaryData(charId, ACTION_HERO_GAINED, 0);
	}
	
	public void setRBkilled(int charId, int npcId)
	{
		setDiaryData(charId, ACTION_RAID_KILLED, npcId);
		
		L2NpcTemplate template = NpcTable.getInstance().getTemplate(npcId);
		
		if (_heroDiaries.containsKey(charId) && template != null)
		{
			// Get Data
			List<StatsSet> list = _heroDiaries.get(charId);
			
			// Clear old data
			_heroDiaries.remove(charId);
			
			// Prepare new data
			StatsSet entry = new StatsSet();
			entry.set("date", new SimpleDateFormat("yyyy-MM-dd HH").format(System.currentTimeMillis()));
			entry.set("action", template.getName() + " was defeated");
			
			// Add to old list
			list.add(entry);
			
			// Put new list into diary
			_heroDiaries.put(charId, list);
		}
	}
	
	public void setCastleTaken(int charId, int castleId)
	{
		setDiaryData(charId, ACTION_CASTLE_TAKEN, castleId);
		
		Castle castle = CastleManager.getInstance().getCastleById(castleId);
		
		if (_heroDiaries.containsKey(charId) && castle != null)
		{
			// Get Data
			List<StatsSet> list = _heroDiaries.get(charId);
			
			// Clear old data
			_heroDiaries.remove(charId);
			
			// Prepare new data
			StatsSet entry = new StatsSet();
			entry.set("date", new SimpleDateFormat("yyyy-MM-dd HH").format(System.currentTimeMillis()));
			entry.set("action", castle.getName() + " Castle was successfuly taken");
			
			// Add to old list
			list.add(entry);
			
			// Put new list into diary
			_heroDiaries.put(charId, list);
		}
	}
	
	public void setDiaryData(int charId, int action, int param)
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			PreparedStatement statement = con.prepareStatement(UPDATE_DIARIES);
			statement.setInt(1, charId);
			statement.setLong(2, System.currentTimeMillis());
			statement.setInt(3, action);
			statement.setInt(4, param);
			statement.execute();
			statement.close();
		}
		catch (SQLException e)
		{
			LOG.error("Hero: SQL exception while saving DiaryData.", e);
		}
	}
	
	/**
	 * Set new hero message for hero
	 * @param player the player instance
	 * @param message String to set
	 */
	public void setHeroMessage(L2PcInstance player, String message)
	{
		_heroMessages.put(player.getObjectId(), message);
	}
	
	/**
	 * Update hero message in database
	 * @param charId character objid
	 */
	public void saveHeroMessage(int charId)
	{
		if (_heroMessages.get(charId) == null)
		{
			return;
		}
		
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			PreparedStatement statement = con.prepareStatement("UPDATE heroes SET message=? WHERE char_id=?;");
			statement.setString(1, _heroMessages.get(charId));
			statement.setInt(2, charId);
			statement.execute();
			statement.close();
		}
		catch (SQLException e)
		{
			LOG.error("Hero: SQL exception while saving HeroMessage.", e);
		}
	}
	
	private static void deleteItemsInDb()
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			PreparedStatement statement = con.prepareStatement(DELETE_ITEMS);
			statement.execute();
			statement.close();
		}
		catch (SQLException e)
		{
			LOG.warn("Hero: Couldn't delete items on db: " + e.getMessage(), e);
		}
	}
	
	/**
	 * Saving task for {@link Hero}<BR>
	 * Save all hero messages to DB.
	 */
	public void shutdown()
	{
		for (int charId : _heroMessages.keySet())
		{
			saveHeroMessage(charId);
		}
	}
	
	public boolean isActiveHero(int id)
	{
		final StatsSet entry = _heroes.get(id);
		
		return entry != null && entry.getInteger(ACTIVE) == 1;
	}
	
	public boolean isInactiveHero(int id)
	{
		final StatsSet entry = _heroes.get(id);
		
		return entry != null && entry.getInteger(ACTIVE) == 0;
	}
	
	public void activateHero(L2PcInstance player)
	{
		StatsSet hero = _heroes.get(player.getObjectId());
		hero.set(ACTIVE, 1);
		
		_heroes.put(player.getObjectId(), hero);
		
		player.setHero(true);
		player.broadcastPacket(new SocialAction(player.getObjectId(), 16));
		player.broadcastUserInfo();
		
		L2Clan clan = player.getClan();
		if (clan != null && clan.getLevel() >= 5)
		{
			String name = hero.getString("char_name");
			
			clan.addReputationScore(1000);
			clan.broadcastToOnlineMembers(new PledgeShowInfoUpdate(clan), SystemMessage.getSystemMessage(SystemMessageId.CLAN_MEMBER_S1_BECAME_HERO_AND_GAINED_S2_REPUTATION_POINTS).addString(name).addNumber(1000));
		}
		
		// Set Gained hero and reload data
		setHeroGained(player.getObjectId());
		loadFights(player.getObjectId());
		loadDiary(player.getObjectId());
		_heroMessages.put(player.getObjectId(), "");
		
		updateHeroes(false);
		player.getAchievement().increase(AchType.HEROIC);
	}
	
	public Map<Integer, StatsSet> getAllHeroes()
	{
		return _completeHeroes;
	}
	
	public static Hero getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final Hero _instance = new Hero();
	}
}