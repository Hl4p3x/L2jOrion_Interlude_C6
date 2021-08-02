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
package l2jorion.game.datatables.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javolution.util.FastList;
import javolution.util.FastMap;
import l2jorion.Config;
import l2jorion.game.cache.InfoCache;
import l2jorion.game.datatables.SkillTable;
import l2jorion.game.model.L2DropCategory;
import l2jorion.game.model.L2DropData;
import l2jorion.game.model.L2MinionData;
import l2jorion.game.model.L2NpcAIData;
import l2jorion.game.model.L2Skill;
import l2jorion.game.model.base.ClassId;
import l2jorion.game.skills.BaseStats;
import l2jorion.game.skills.Stats;
import l2jorion.game.templates.L2NpcTemplate;
import l2jorion.game.templates.StatsSet;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;
import l2jorion.util.CloseUtil;
import l2jorion.util.database.DatabaseUtils;
import l2jorion.util.database.L2DatabaseFactory;

public class NpcTable
{
	private final static Logger LOG = LoggerFactory.getLogger(NpcTable.class);
	
	private static NpcTable _instance;
	
	private final Map<Integer, L2NpcTemplate> npcs;
	
	private boolean _initialized = false;
	
	public static NpcTable getInstance()
	{
		if (_instance == null)
		{
			_instance = new NpcTable();
		}
		
		return _instance;
	}
	
	private NpcTable()
	{
		npcs = new FastMap<>();
		
		restoreNpcData();
		
		LOG.info("NpcTable: Loaded " + npcs.size() + " npc templates");
		
		loadNpcsAI(0);
	}
	
	private void restoreNpcData()
	{
		Connection con = null;
		
		try
		{
			PreparedStatement statement;
			
			try
			{
				con = L2DatabaseFactory.getInstance().getConnection();
				statement = con.prepareStatement("SELECT " + L2DatabaseFactory.getInstance().safetyString(new String[]
				{
					"id",
					"idTemplate",
					"name",
					"serverSideName",
					"title",
					"serverSideTitle",
					"class",
					"collision_radius",
					"collision_height",
					"level",
					"sex",
					"type",
					"attackrange",
					"hp",
					"mp",
					"hpreg",
					"mpreg",
					"str",
					"con",
					"dex",
					"int",
					"wit",
					"men",
					"exp",
					"sp",
					"patk",
					"pdef",
					"matk",
					"mdef",
					"atkspd",
					"aggro",
					"matkspd",
					"rhand",
					"lhand",
					"armor",
					"walkspd",
					"runspd",
					"faction_id",
					"faction_range",
					"isUndead",
					"absorb_level",
					"absorb_type",
					"DropHerb"
				}) + " FROM npc");
				final ResultSet npcdata = statement.executeQuery();
				fillNpcTable(npcdata, false);
				npcdata.close();
				DatabaseUtils.close(statement);
			}
			catch (final Exception e)
			{
				LOG.error("NPCTable: Error creating NPC table", e);
			}
			
			if (Config.CUSTOM_NPC_TABLE)
			{
				try
				{
					if (con == null)
					{
						con = L2DatabaseFactory.getInstance().getConnection();
					}
					statement = con.prepareStatement("SELECT " + L2DatabaseFactory.getInstance().safetyString(new String[]
					{
						"id",
						"idTemplate",
						"name",
						"serverSideName",
						"title",
						"serverSideTitle",
						"class",
						"collision_radius",
						"collision_height",
						"level",
						"sex",
						"type",
						"attackrange",
						"hp",
						"mp",
						"hpreg",
						"mpreg",
						"str",
						"con",
						"dex",
						"int",
						"wit",
						"men",
						"exp",
						"sp",
						"patk",
						"pdef",
						"matk",
						"mdef",
						"atkspd",
						"aggro",
						"matkspd",
						"rhand",
						"lhand",
						"armor",
						"walkspd",
						"runspd",
						"faction_id",
						"faction_range",
						"isUndead",
						"absorb_level",
						"absorb_type",
						"DropHerb"
					}) + " FROM custom_npc");
					final ResultSet npcdata = statement.executeQuery();
					fillNpcTable(npcdata, true);
					npcdata.close();
					DatabaseUtils.close(statement);
				}
				catch (final Exception e)
				{
					LOG.error("NPCTable: Error creating custom NPC table", e);
				}
			}
			
			try
			{
				if (con == null)
				{
					con = L2DatabaseFactory.getInstance().getConnection();
				}
				statement = con.prepareStatement("SELECT npcid, skillid, level FROM npcskills");
				final ResultSet npcskills = statement.executeQuery();
				L2NpcTemplate npcDat = null;
				L2Skill npcSkill = null;
				
				while (npcskills.next())
				{
					final int mobId = npcskills.getInt("npcid");
					npcDat = npcs.get(mobId);
					
					if (npcDat == null)
					{
						continue;
					}
					
					final int skillId = npcskills.getInt("skillid");
					final int level = npcskills.getInt("level");
					
					if (npcDat.race == null && skillId == 4416)
					{
						npcDat.setRace(level);
						continue;
					}
					
					npcSkill = SkillTable.getInstance().getInfo(skillId, level);
					
					if (npcSkill == null)
					{
						continue;
					}
					
					npcDat.addSkill(npcSkill);
				}
				
				npcskills.close();
				DatabaseUtils.close(statement);
			}
			catch (final Exception e)
			{
				LOG.error("NPCTable: Error reading NPC skills table", e);
			}
			
			if (Config.CUSTOM_DROPLIST_TABLE)
			{
				try
				{
					if (con == null)
					{
						con = L2DatabaseFactory.getInstance().getConnection();
					}
					statement = con.prepareStatement("SELECT " + L2DatabaseFactory.getInstance().safetyString(new String[]
					{
						"mobId",
						"itemId",
						"min",
						"max",
						"category",
						"chance",
						"enchantMin",
						"enchantMax"
					}) + " FROM custom_droplist ORDER BY mobId, chance DESC");
					final ResultSet dropData = statement.executeQuery();
					
					int cCount = 0;
					
					while (dropData.next())
					{
						final int mobId = dropData.getInt("mobId");
						final L2NpcTemplate npcDat = npcs.get(mobId);
						
						if (npcDat == null)
						{
							LOG.warn("NPCTable: CUSTOM DROPLIST No npc correlating with id: " + mobId);
							continue;
						}
						
						final L2DropData dropDat = new L2DropData();
						
						dropDat.setItemId(dropData.getInt("itemId"));
						dropDat.setMinDrop(dropData.getInt("min"));
						dropDat.setMaxDrop(dropData.getInt("max"));
						dropDat.setChance(dropData.getInt("chance"));
						dropDat.setMinEnchant(dropData.getInt("enchantMin"));
						dropDat.setMaxEnchant(dropData.getInt("enchantMax"));
						final int category = dropData.getInt("category");
						
						npcDat.addDropData(dropDat, category);
						cCount++;
					}
					dropData.close();
					DatabaseUtils.close(statement);
					
					if (cCount > 0)
					{
						LOG.info("CustomDropList: Added " + cCount + " custom droplist");
					}
					
					if (Config.ENABLE_CACHE_INFO)
					{
						FillDropList();
					}
				}
				catch (final Exception e)
				{
					LOG.error("NPCTable: Error reading NPC CUSTOM drop data", e);
				}
			}
			
			try
			{
				if (con == null)
				{
					con = L2DatabaseFactory.getInstance().getConnection();
				}
				statement = con.prepareStatement("SELECT " + L2DatabaseFactory.getInstance().safetyString(new String[]
				{
					"mobId",
					"itemId",
					"min",
					"max",
					"category",
					"chance",
					"enchantMin",
					"enchantMax"
				}) + " FROM droplist ORDER BY mobId, chance DESC");
				final ResultSet dropData = statement.executeQuery();
				L2DropData dropDat = null;
				L2NpcTemplate npcDat = null;
				
				while (dropData.next())
				{
					final int mobId = dropData.getInt("mobId");
					npcDat = npcs.get(mobId);
					
					if (npcDat == null)
					{
						LOG.info("NPCTable: No npc correlating with id: " + mobId);
						continue;
					}
					
					dropDat = new L2DropData();
					
					dropDat.setItemId(dropData.getInt("itemId"));
					dropDat.setMinDrop(dropData.getInt("min"));
					dropDat.setMaxDrop(dropData.getInt("max"));
					dropDat.setChance(dropData.getInt("chance"));
					dropDat.setMinEnchant(dropData.getInt("enchantMin"));
					dropDat.setMaxEnchant(dropData.getInt("enchantMax"));
					final int category = dropData.getInt("category");
					
					npcDat.addDropData(dropDat, category);
				}
				
				dropData.close();
				DatabaseUtils.close(statement);
			}
			catch (final Exception e)
			{
				LOG.error("NPCTable: Error reading NPC drop data", e);
			}
			
			try
			{
				if (con == null)
				{
					con = L2DatabaseFactory.getInstance().getConnection();
				}
				statement = con.prepareStatement("SELECT " + L2DatabaseFactory.getInstance().safetyString(new String[]
				{
					"npc_id",
					"class_id"
				}) + " FROM skill_learn");
				final ResultSet learndata = statement.executeQuery();
				
				while (learndata.next())
				{
					final int npcId = learndata.getInt("npc_id");
					final int classId = learndata.getInt("class_id");
					
					final L2NpcTemplate npc = getTemplate(npcId);
					if (npc == null)
					{
						LOG.warn("NPCTable: Error getting NPC template ID " + npcId + " while trying to load skill trainer data.");
						continue;
					}
					
					if (classId >= ClassId.values().length)
					{
						LOG.warn("NPCTable: Error defining learning data for NPC " + npcId + ": specified classId " + classId + " is higher then max one " + (ClassId.values().length - 1) + " specified into ClassID Enum --> check your Database to be complient with it");
						continue;
					}
					
					npc.addTeachInfo(ClassId.values()[classId]);
				}
				
				learndata.close();
				DatabaseUtils.close(statement);
			}
			catch (final Exception e)
			{
				LOG.error("NPCTable: Error reading NPC trainer data", e);
			}
			
			try
			{
				if (con == null)
				{
					con = L2DatabaseFactory.getInstance().getConnection();
				}
				statement = con.prepareStatement("SELECT " + L2DatabaseFactory.getInstance().safetyString(new String[]
				{
					"boss_id",
					"minion_id",
					"amount_min",
					"amount_max"
				}) + " FROM minions");
				final ResultSet minionData = statement.executeQuery();
				L2MinionData minionDat = null;
				L2NpcTemplate npcDat = null;
				int cnt = 0;
				
				while (minionData.next())
				{
					final int raidId = minionData.getInt("boss_id");
					
					npcDat = npcs.get(raidId);
					minionDat = new L2MinionData();
					minionDat.setMinionId(minionData.getInt("minion_id"));
					minionDat.setAmountMin(minionData.getInt("amount_min"));
					minionDat.setAmountMax(minionData.getInt("amount_max"));
					npcDat.addRaidData(minionDat);
					cnt++;
				}
				
				minionData.close();
				DatabaseUtils.close(statement);
				LOG.info("NpcTable: Loaded " + cnt + " minions");
			}
			catch (final Exception e)
			{
				LOG.info("Error loading minion data");
				e.printStackTrace();
			}
		}
		finally
		{
			CloseUtil.close(con);
		}
		
		_initialized = true;
	}
	
	private void fillNpcTable(final ResultSet NpcData, final boolean custom) throws Exception
	{
		while (NpcData.next())
		{
			final StatsSet npcDat = new StatsSet();
			
			final int id = NpcData.getInt("id");
			
			npcDat.set("npcId", id);
			npcDat.set("idTemplate", NpcData.getInt("idTemplate"));
			
			// Level: for special bosses could be different
			int level = 0;
			float diff = 0; // difference between setted value and retail one
			boolean minion = false;
			
			switch (id)
			{
				case 29002: // and minions
				case 29003:
				case 29004:
				case 29005:
					minion = true;
				case 29001:// queenAnt
				{
					if (Config.QA_LEVEL > 0)
					{
						diff = Config.QA_LEVEL - NpcData.getInt("level");
						level = Config.QA_LEVEL;
					}
					else
					{
						level = NpcData.getInt("level");
					}
					
				}
					break;
				case 29022:
				{ // zaken
					
					if (Config.ZAKEN_LEVEL > 0)
					{
						diff = Config.ZAKEN_LEVEL - NpcData.getInt("level");
						level = Config.ZAKEN_LEVEL;
					}
					else
					{
						level = NpcData.getInt("level");
					}
					
				}
					break;
				case 29015: // and minions
				case 29016:
				case 29017:
				case 29018:
					minion = true;
				case 29014:// orfen
				{
					
					if (Config.ORFEN_LEVEL > 0)
					{
						diff = Config.ORFEN_LEVEL - NpcData.getInt("level");
						level = Config.ORFEN_LEVEL;
					}
					else
					{
						level = NpcData.getInt("level");
					}
					
				}
					break;
				case 29007: // and minions
				case 29008:
				case 290011:
					minion = true;
				case 29006: // core
				{
					
					if (Config.CORE_LEVEL > 0)
					{
						diff = Config.CORE_LEVEL - NpcData.getInt("level");
						level = Config.CORE_LEVEL;
					}
					else
					{
						level = NpcData.getInt("level");
					}
					
				}
					break;
				default:
				{
					level = NpcData.getInt("level");
				}
			}
			
			npcDat.set("level", level);
			npcDat.set("jClass", NpcData.getString("class"));
			
			npcDat.set("baseShldDef", 0);
			npcDat.set("baseShldRate", 0);
			npcDat.set("baseCritRate", 4);
			
			npcDat.set("name", NpcData.getString("name"));
			npcDat.set("serverSideName", NpcData.getBoolean("serverSideName"));
			// npcDat.set("name", "");
			npcDat.set("title", NpcData.getString("title"));
			npcDat.set("serverSideTitle", NpcData.getBoolean("serverSideTitle"));
			npcDat.set("collision_radius", NpcData.getDouble("collision_radius"));
			npcDat.set("collision_height", NpcData.getDouble("collision_height"));
			npcDat.set("sex", NpcData.getString("sex"));
			npcDat.set("type", NpcData.getString("type"));
			npcDat.set("baseAtkRange", NpcData.getInt("attackrange"));
			
			// BOSS POWER CHANGES
			double multi_value = 1;
			
			if (diff >= 15)
			{ // means that there is level customization
				multi_value = multi_value * (diff / 10);
			}
			else if (diff > 0 && diff < 15)
			{
				multi_value = multi_value + (diff / 10);
			}
			
			if (minion)
			{
				multi_value = multi_value * Config.LEVEL_DIFF_MULTIPLIER_MINION; // allow to increase the power of a value
				// that for example, at 40 diff levels is
				// equal to
				// value = ((40/10)*0.8) = 3,2 --> 220 % more
			}
			else
			{
				
				switch (id)
				{
					case 29001:
					{// queenAnt
						
						if (Config.QA_POWER_MULTIPLIER > 0)
						{
							multi_value = multi_value * Config.QA_POWER_MULTIPLIER;
						}
						
					}
						break;
					case 29022:
					{ // zaken
						
						if (Config.ZAKEN_POWER_MULTIPLIER > 0)
						{
							multi_value = multi_value * Config.ZAKEN_POWER_MULTIPLIER;
						}
						
					}
						break;
					case 29014:
					{// orfen
						
						if (Config.ORFEN_POWER_MULTIPLIER > 0)
						{
							multi_value = multi_value * Config.ORFEN_POWER_MULTIPLIER;
						}
						
					}
						break;
					case 29006:
					{ // core
						
						if (Config.CORE_POWER_MULTIPLIER > 0)
						{
							multi_value = multi_value * Config.CORE_POWER_MULTIPLIER;
						}
						
					}
						break;
					case 29019:
					{ // antharas
						
						if (Config.ANTHARAS_POWER_MULTIPLIER > 0)
						{
							multi_value = multi_value * Config.ANTHARAS_POWER_MULTIPLIER;
						}
						
					}
						break;
					case 29028:
					{ // valakas
						
						if (Config.VALAKAS_POWER_MULTIPLIER > 0)
						{
							multi_value = multi_value * Config.VALAKAS_POWER_MULTIPLIER;
						}
						
					}
						break;
					case 29020:
					{ // baium
						
						if (Config.BAIUM_POWER_MULTIPLIER > 0)
						{
							multi_value = multi_value * Config.BAIUM_POWER_MULTIPLIER;
						}
						
					}
						break;
					case 29045:
					{ // frintezza
						
						if (Config.FRINTEZZA_POWER_MULTIPLIER > 0)
						{
							multi_value = multi_value * Config.FRINTEZZA_POWER_MULTIPLIER;
						}
						
					}
						break;
					default:
					{
					}
				}
				
			}
			
			npcDat.set("rewardExp", NpcData.getInt("exp") * multi_value);
			npcDat.set("rewardSp", NpcData.getInt("sp") * multi_value);
			npcDat.set("basePAtkSpd", NpcData.getInt("atkspd") * multi_value);
			npcDat.set("baseMAtkSpd", NpcData.getInt("matkspd") * multi_value);
			npcDat.set("baseHpMax", NpcData.getInt("hp") * multi_value);
			npcDat.set("baseMpMax", NpcData.getInt("mp") * multi_value);
			npcDat.set("baseHpReg", (int) NpcData.getFloat("hpreg") * multi_value > 0 ? NpcData.getFloat("hpreg") : 1.5 + (level - 1) / 10.0);
			npcDat.set("baseMpReg", (int) NpcData.getFloat("mpreg") * multi_value > 0 ? NpcData.getFloat("mpreg") : 0.9 + 0.3 * (level - 1) / 10.0);
			npcDat.set("basePAtk", NpcData.getInt("patk") * multi_value);
			npcDat.set("basePDef", NpcData.getInt("pdef") * multi_value);
			npcDat.set("baseMAtk", NpcData.getInt("matk") * multi_value);
			npcDat.set("baseMDef", NpcData.getInt("mdef") * multi_value);
			
			npcDat.set("aggroRange", NpcData.getInt("aggro"));
			npcDat.set("rhand", NpcData.getInt("rhand"));
			npcDat.set("lhand", NpcData.getInt("lhand"));
			npcDat.set("armor", NpcData.getInt("armor"));
			npcDat.set("baseWalkSpd", NpcData.getInt("walkspd"));
			npcDat.set("baseRunSpd", NpcData.getInt("runspd"));
			
			// constants, until we have stats in DB
			// constants, until we have stats in DB
			npcDat.safeSet("baseSTR", NpcData.getInt("str"), 0, BaseStats.MAX_STAT_VALUE, "Loading npc template id: " + NpcData.getInt("idTemplate"));
			npcDat.safeSet("baseCON", NpcData.getInt("con"), 0, BaseStats.MAX_STAT_VALUE, "Loading npc template id: " + NpcData.getInt("idTemplate"));
			npcDat.safeSet("baseDEX", NpcData.getInt("dex"), 0, BaseStats.MAX_STAT_VALUE, "Loading npc template id: " + NpcData.getInt("idTemplate"));
			npcDat.safeSet("baseINT", NpcData.getInt("int"), 0, BaseStats.MAX_STAT_VALUE, "Loading npc template id: " + NpcData.getInt("idTemplate"));
			npcDat.safeSet("baseWIT", NpcData.getInt("wit"), 0, BaseStats.MAX_STAT_VALUE, "Loading npc template id: " + NpcData.getInt("idTemplate"));
			npcDat.safeSet("baseMEN", NpcData.getInt("men"), 0, BaseStats.MAX_STAT_VALUE, "Loading npc template id: " + NpcData.getInt("idTemplate"));
			
			npcDat.set("baseCpMax", 0);
			
			npcDat.set("factionId", NpcData.getString("faction_id"));
			npcDat.set("factionRange", NpcData.getInt("faction_range"));
			
			npcDat.set("isUndead", NpcData.getString("isUndead"));
			
			npcDat.set("absorb_level", NpcData.getString("absorb_level"));
			npcDat.set("absorb_type", NpcData.getString("absorb_type"));
			npcDat.set("DropHerb", NpcData.getInt("DropHerb"));
			
			final L2NpcTemplate template = new L2NpcTemplate(npcDat, custom);
			template.addVulnerability(Stats.BOW_WPN_VULN, 1);
			template.addVulnerability(Stats.BLUNT_WPN_VULN, 1);
			template.addVulnerability(Stats.DAGGER_WPN_VULN, 1);
			
			npcs.put(id, template);
		}
	}
	
	public void loadNpcsAI(int id)
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = null;
			
			if (id > 0)
			{
				statement = con.prepareStatement("SELECT * FROM npc_ai_data WHERE npc_id = ?");
				statement.setInt(1, id);
			}
			else
			{
				statement = con.prepareStatement("SELECT * FROM npc_ai_data ORDER BY npc_id");
			}
			
			ResultSet rset = statement.executeQuery();
			
			L2NpcAIData npcAIDat = null;
			L2NpcTemplate npcDat = null;
			
			int cnt = 0;
			
			while (rset.next())
			{
				int npcId = rset.getInt("npc_id");
				
				npcDat = npcs.get(npcId);
				
				if (npcDat == null)
				{
					LOG.error("NPCTable: AI Data Error with id : " + npcId);
					continue;
				}
				
				npcAIDat = new L2NpcAIData();
				npcAIDat.setMinSkillChance(rset.getInt("min_skill_chance"));
				npcAIDat.setMaxSkillChance(rset.getInt("max_skill_chance"));
				npcAIDat.setPrimaryAttack(rset.getInt("primary_attack"));
				npcAIDat.setCanMove(rset.getInt("can_move"));
				npcAIDat.setShortRangeSkill(rset.getInt("minrangeskill"));
				npcAIDat.setShortRangeChance(rset.getInt("minrangechance"));
				npcAIDat.setLongRangeSkill(rset.getInt("maxrangeskill"));
				npcAIDat.setLongRangeChance(rset.getInt("maxrangechance"));
				npcAIDat.setSoulShot(rset.getInt("soulshot"));
				npcAIDat.setSpiritShot(rset.getInt("spiritshot"));
				npcAIDat.setSpiritShotChance(rset.getInt("spschance"));
				npcAIDat.setSoulShotChance(rset.getInt("sschance"));
				npcAIDat.setIsChaos(rset.getInt("is_chaos"));
				npcAIDat.setAggro(rset.getInt("aggro"));
				npcAIDat.setClan(rset.getString("clan"));
				npcAIDat.setClanRange(rset.getInt("clan_range"));
				npcAIDat.setEnemyClan(rset.getString("enemy_clan"));
				npcAIDat.setEnemyRange(rset.getInt("enemy_range"));
				npcAIDat.setAi(rset.getString("ai_type"));
				
				npcDat.setAIData(npcAIDat);
				cnt++;
			}
			
			rset.close();
			statement.close();
			
			LOG.info("NpcTable: Loaded " + cnt + " npc ai data");
		}
		catch (Exception e)
		{
			LOG.warn("NPCTable: Error reading NPC AI Data: " + e.getMessage(), e);
		}
	}
	
	public void reloadNpc(final int id)
	{
		Connection con = null;
		
		try
		{
			// save a copy of the old data
			final L2NpcTemplate old = getTemplate(id);
			final Map<Integer, L2Skill> skills = new FastMap<>();
			
			skills.putAll(old.getSkills());
			
			final FastList<L2DropCategory> categories = new FastList<>();
			
			if (old.getDropData() != null)
			{
				categories.addAll(old.getDropData());
			}
			final ClassId[] classIds = old.getTeachInfo().clone();
			
			final List<L2MinionData> minions = new FastList<>();
			
			if (old.getMinionData() != null)
			{
				minions.addAll(old.getMinionData());
			}
			
			// reload the NPC base data
			con = L2DatabaseFactory.getInstance().getConnection();
			
			if (old.isCustom())
			{
				
				final PreparedStatement st = con.prepareStatement("SELECT " + L2DatabaseFactory.getInstance().safetyString(new String[]
				{
					"id",
					"idTemplate",
					"name",
					"serverSideName",
					"title",
					"serverSideTitle",
					"class",
					"collision_radius",
					"collision_height",
					"level",
					"sex",
					"type",
					"attackrange",
					"hp",
					"mp",
					"hpreg",
					"mpreg",
					"str",
					"con",
					"dex",
					"int",
					"wit",
					"men",
					"exp",
					"sp",
					"patk",
					"pdef",
					"matk",
					"mdef",
					"atkspd",
					"aggro",
					"matkspd",
					"rhand",
					"lhand",
					"armor",
					"walkspd",
					"runspd",
					"faction_id",
					"faction_range",
					"isUndead",
					"absorb_level",
					"absorb_type",
					"DropHerb"
				}) + " FROM custom_npc WHERE id=?");
				st.setInt(1, id);
				final ResultSet rs = st.executeQuery();
				fillNpcTable(rs, true);
				rs.close();
				st.close();
				
			}
			else
			{
				
				final PreparedStatement st = con.prepareStatement("SELECT " + L2DatabaseFactory.getInstance().safetyString(new String[]
				{
					"id",
					"idTemplate",
					"name",
					"serverSideName",
					"title",
					"serverSideTitle",
					"class",
					"collision_radius",
					"collision_height",
					"level",
					"sex",
					"type",
					"attackrange",
					"hp",
					"mp",
					"hpreg",
					"mpreg",
					"str",
					"con",
					"dex",
					"int",
					"wit",
					"men",
					"exp",
					"sp",
					"patk",
					"pdef",
					"matk",
					"mdef",
					"atkspd",
					"aggro",
					"matkspd",
					"rhand",
					"lhand",
					"armor",
					"walkspd",
					"runspd",
					"faction_id",
					"faction_range",
					"isUndead",
					"absorb_level",
					"absorb_type",
					"DropHerb"
				}) + " FROM npc WHERE id=?");
				st.setInt(1, id);
				final ResultSet rs = st.executeQuery();
				fillNpcTable(rs, false);
				rs.close();
				st.close();
				
			}
			
			// restore additional data from saved copy
			final L2NpcTemplate created = getTemplate(id);
			
			for (final L2Skill skill : skills.values())
			{
				created.addSkill(skill);
			}
			
			for (final ClassId classId : classIds)
			{
				created.addTeachInfo(classId);
			}
			
			for (final L2MinionData minion : minions)
			{
				created.addRaidData(minion);
			}
		}
		catch (final Exception e)
		{
			LOG.error("NPCTable: Could not reload data for NPC " + " " + id, e);
		}
		finally
		{
			CloseUtil.close(con);
		}
	}
	
	// just wrapper
	public void reloadAllNpc()
	{
		restoreNpcData();
		loadNpcsAI(0);
	}
	
	public void saveNpc(final StatsSet npc)
	{
		Connection con = null;
		
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			final Map<String, Object> set = npc.getSet();
			
			String name = "";
			String values = "";
			
			final L2NpcTemplate old = getTemplate(npc.getInteger("npcId"));
			
			for (final Object obj : set.keySet())
			{
				name = (String) obj;
				
				if (!name.equalsIgnoreCase("npcId"))
				{
					if (values != "")
					{
						values += ", ";
					}
					
					values += name + " = '" + set.get(name) + "'";
				}
			}
			
			PreparedStatement statement = null;
			if (old.isCustom())
			{
				statement = con.prepareStatement("UPDATE custom_npc SET " + values + " WHERE id = ?");
				
			}
			else
			{
				statement = con.prepareStatement("UPDATE npc SET " + values + " WHERE id = ?");
				
			}
			statement.setInt(1, npc.getInteger("npcId"));
			statement.execute();
			DatabaseUtils.close(statement);
			statement = null;
		}
		catch (final Exception e)
		{
			LOG.error("NPCTable: Could not store new NPC data in database", e);
		}
		finally
		{
			CloseUtil.close(con);
		}
	}
	
	public boolean isInitialized()
	{
		return _initialized;
	}
	
	public void replaceTemplate(final L2NpcTemplate npc)
	{
		npcs.put(npc.npcId, npc);
	}
	
	public L2NpcTemplate getTemplate(final int id)
	{
		return npcs.get(id);
	}
	
	public L2NpcTemplate getTemplateByName(final String name)
	{
		for (final L2NpcTemplate npcTemplate : npcs.values())
		{
			String name1 = npcTemplate.name.toLowerCase();
			String name2 = name.toLowerCase();
			
			if (name1.equalsIgnoreCase(name2))
			{
				return npcTemplate;
			}
		}
		
		return null;
	}
	
	public L2NpcTemplate[] getAllOfLevel(final int lvl)
	{
		final List<L2NpcTemplate> list = new FastList<>();
		
		for (final L2NpcTemplate t : npcs.values())
		{
			if (t.level == lvl)
			{
				list.add(t);
			}
		}
		
		return list.toArray(new L2NpcTemplate[list.size()]);
	}
	
	public L2NpcTemplate[] getAllMonstersOfLevel(final int lvl)
	{
		final List<L2NpcTemplate> list = new FastList<>();
		
		for (final L2NpcTemplate t : npcs.values())
		{
			if (t.level == lvl && "L2Monster".equals(t.type))
			{
				list.add(t);
			}
		}
		
		return list.toArray(new L2NpcTemplate[list.size()]);
	}
	
	public L2NpcTemplate[] getAllNpcStartingWith(final String letter)
	{
		final List<L2NpcTemplate> list = new FastList<>();
		
		for (final L2NpcTemplate t : npcs.values())
		{
			if (t.name.startsWith(letter) && "L2Npc".equals(t.type))
			{
				list.add(t);
			}
		}
		
		return list.toArray(new L2NpcTemplate[list.size()]);
	}
	
	/**
	 * @param classType
	 * @return
	 */
	public Set<Integer> getAllNpcOfClassType(final String classType)
	{
		return null;
	}
	
	/**
	 * @param clazz
	 * @return
	 */
	public Set<Integer> getAllNpcOfL2jClass(final Class<?> clazz)
	{
		return null;
	}
	
	/**
	 * @param aiType
	 * @return
	 */
	public Set<Integer> getAllNpcOfAiType(final String aiType)
	{
		return null;
	}
	
	public Map<Integer, L2NpcTemplate> getAllTemplates()
	{
		return npcs;
	}
	
	public void FillDropList()
	{
		for (final L2NpcTemplate npc : npcs.values())
		{
			InfoCache.addToDroplistCache(npc.npcId, npc.getAllDropData());
		}
		
		LOG.info("Players droplist was cached");
	}
	
	public List<L2NpcTemplate> getAllOfLevel(int... lvls)
	{
		final List<L2NpcTemplate> list = new FastList<>();
		for (int lvl : lvls)
		{
			for (L2NpcTemplate t : npcs.values())
			{
				if (t.getLevel() == lvl)
				{
					list.add(t);
				}
			}
		}
		return list;
	}
	
}
