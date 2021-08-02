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
package l2jorion.game.model.actor.instance;

import static l2jorion.game.ai.CtrlIntention.AI_INTENTION_MOVE_TO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import javolution.text.TextBuilder;
import javolution.util.FastList;
import javolution.util.FastMap;
import javolution.util.FastSet;
import l2jorion.Config;
import l2jorion.game.ai.CtrlIntention;
import l2jorion.game.ai.L2CharacterAI;
import l2jorion.game.ai.L2PlayerAI;
import l2jorion.game.ai.phantom.phantomActions;
import l2jorion.game.cache.HtmCache;
import l2jorion.game.cache.WarehouseCacheManager;
import l2jorion.game.community.bb.Forum;
import l2jorion.game.community.manager.ForumsBBSManager;
import l2jorion.game.controllers.GameTimeController;
import l2jorion.game.controllers.RecipeController;
import l2jorion.game.datatables.AccessLevel;
import l2jorion.game.datatables.GmListTable;
import l2jorion.game.datatables.HeroSkillTable;
import l2jorion.game.datatables.NobleSkillTable;
import l2jorion.game.datatables.SkillTable;
import l2jorion.game.datatables.csv.FishTable;
import l2jorion.game.datatables.csv.HennaTable;
import l2jorion.game.datatables.csv.MapRegionTable;
import l2jorion.game.datatables.csv.RecipeTable;
import l2jorion.game.datatables.sql.AccessLevels;
import l2jorion.game.datatables.sql.CharTemplateTable;
import l2jorion.game.datatables.sql.ClanTable;
import l2jorion.game.datatables.sql.ItemTable;
import l2jorion.game.datatables.sql.NpcTable;
import l2jorion.game.datatables.sql.SkillTreeTable;
import l2jorion.game.datatables.xml.ExperienceData;
import l2jorion.game.enums.AchType;
import l2jorion.game.geo.GeoData;
import l2jorion.game.handler.IItemHandler;
import l2jorion.game.handler.ItemHandler;
import l2jorion.game.handler.admin.AdminEditChar;
import l2jorion.game.handler.skill.SiegeFlag;
import l2jorion.game.handler.skill.StrSiegeAssault;
import l2jorion.game.handler.skill.TakeCastle;
import l2jorion.game.managers.CastleManager;
import l2jorion.game.managers.CoupleManager;
import l2jorion.game.managers.CursedWeaponsManager;
import l2jorion.game.managers.DimensionalRiftManager;
import l2jorion.game.managers.DuelManager;
import l2jorion.game.managers.FortSiegeManager;
import l2jorion.game.managers.ItemsOnGroundManager;
import l2jorion.game.managers.QuestManager;
import l2jorion.game.managers.SiegeManager;
import l2jorion.game.managers.TownManager;
import l2jorion.game.model.Achievement;
import l2jorion.game.model.BlockList;
import l2jorion.game.model.FishData;
import l2jorion.game.model.Inventory;
import l2jorion.game.model.ItemContainer;
import l2jorion.game.model.L2Attackable;
import l2jorion.game.model.L2Character;
import l2jorion.game.model.L2Clan;
import l2jorion.game.model.L2ClanMember;
import l2jorion.game.model.L2Effect;
import l2jorion.game.model.L2Fishing;
import l2jorion.game.model.L2Macro;
import l2jorion.game.model.L2ManufactureList;
import l2jorion.game.model.L2Object;
import l2jorion.game.model.L2Party;
import l2jorion.game.model.L2Radar;
import l2jorion.game.model.L2RecipeList;
import l2jorion.game.model.L2Request;
import l2jorion.game.model.L2ShortCut;
import l2jorion.game.model.L2Skill;
import l2jorion.game.model.L2Skill.SkillTargetType;
import l2jorion.game.model.L2Skill.SkillType;
import l2jorion.game.model.L2SkillLearn;
import l2jorion.game.model.L2Summon;
import l2jorion.game.model.L2Vehicle;
import l2jorion.game.model.L2World;
import l2jorion.game.model.Location;
import l2jorion.game.model.MacroList;
import l2jorion.game.model.PartyMatchRoom;
import l2jorion.game.model.PartyMatchRoomList;
import l2jorion.game.model.PartyMatchWaitingList;
import l2jorion.game.model.PcFreight;
import l2jorion.game.model.PcInventory;
import l2jorion.game.model.PcWarehouse;
import l2jorion.game.model.PetInventory;
import l2jorion.game.model.ShortCuts;
import l2jorion.game.model.TimeStamp;
import l2jorion.game.model.TradeList;
import l2jorion.game.model.actor.appearance.PcAppearance;
import l2jorion.game.model.actor.knownlist.PcKnownList;
import l2jorion.game.model.actor.stat.PcStat;
import l2jorion.game.model.actor.status.PcStatus;
import l2jorion.game.model.base.ClassId;
import l2jorion.game.model.base.ClassLevel;
import l2jorion.game.model.base.PlayerClass;
import l2jorion.game.model.base.Race;
import l2jorion.game.model.base.SubClass;
import l2jorion.game.model.custom.DressMeData;
import l2jorion.game.model.entity.Announcements;
import l2jorion.game.model.entity.Duel;
import l2jorion.game.model.entity.Hitman;
import l2jorion.game.model.entity.Rebirth;
import l2jorion.game.model.entity.event.CTF;
import l2jorion.game.model.entity.event.DM;
import l2jorion.game.model.entity.event.L2Event;
import l2jorion.game.model.entity.event.TvT;
import l2jorion.game.model.entity.event.VIP;
import l2jorion.game.model.entity.sevensigns.SevenSigns;
import l2jorion.game.model.entity.sevensigns.SevenSignsFestival;
import l2jorion.game.model.entity.siege.Castle;
import l2jorion.game.model.entity.siege.FortSiege;
import l2jorion.game.model.entity.siege.Siege;
import l2jorion.game.model.extender.BaseExtender.EventType;
import l2jorion.game.model.olympiad.OlympiadGameManager;
import l2jorion.game.model.olympiad.OlympiadGameTask;
import l2jorion.game.model.olympiad.OlympiadManager;
import l2jorion.game.model.quest.Quest;
import l2jorion.game.model.quest.QuestState;
import l2jorion.game.model.zone.ZoneId;
import l2jorion.game.model.zone.type.L2TownZone;
import l2jorion.game.network.L2GameClient;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.clientpackets.Say2;
import l2jorion.game.network.serverpackets.ActionFailed;
import l2jorion.game.network.serverpackets.ChangeWaitType;
import l2jorion.game.network.serverpackets.CharInfo;
import l2jorion.game.network.serverpackets.ConfirmDlg;
import l2jorion.game.network.serverpackets.CreatureSay;
import l2jorion.game.network.serverpackets.EtcStatusUpdate;
import l2jorion.game.network.serverpackets.ExAutoSoulShot;
import l2jorion.game.network.serverpackets.ExDuelUpdateUserInfo;
import l2jorion.game.network.serverpackets.ExFishingEnd;
import l2jorion.game.network.serverpackets.ExFishingStart;
import l2jorion.game.network.serverpackets.ExOlympiadMode;
import l2jorion.game.network.serverpackets.ExPCCafePointInfo;
import l2jorion.game.network.serverpackets.ExRedSky;
import l2jorion.game.network.serverpackets.ExSetCompassZoneCode;
import l2jorion.game.network.serverpackets.ExShowScreenMessage;
import l2jorion.game.network.serverpackets.FriendList;
import l2jorion.game.network.serverpackets.GetOnVehicle;
import l2jorion.game.network.serverpackets.HennaInfo;
import l2jorion.game.network.serverpackets.InventoryUpdate;
import l2jorion.game.network.serverpackets.ItemList;
import l2jorion.game.network.serverpackets.L2GameServerPacket;
import l2jorion.game.network.serverpackets.LeaveWorld;
import l2jorion.game.network.serverpackets.MagicSkillCanceld;
import l2jorion.game.network.serverpackets.MyTargetSelected;
import l2jorion.game.network.serverpackets.NpcHtmlMessage;
import l2jorion.game.network.serverpackets.NpcInfo;
import l2jorion.game.network.serverpackets.ObservationMode;
import l2jorion.game.network.serverpackets.ObservationReturn;
import l2jorion.game.network.serverpackets.PartySmallWindowUpdate;
import l2jorion.game.network.serverpackets.PetInventoryUpdate;
import l2jorion.game.network.serverpackets.PlaySound;
import l2jorion.game.network.serverpackets.PledgeShowInfoUpdate;
import l2jorion.game.network.serverpackets.PledgeShowMemberListDelete;
import l2jorion.game.network.serverpackets.PledgeShowMemberListUpdate;
import l2jorion.game.network.serverpackets.PrivateStoreListBuy;
import l2jorion.game.network.serverpackets.PrivateStoreListSell;
import l2jorion.game.network.serverpackets.PrivateStoreMsgBuy;
import l2jorion.game.network.serverpackets.PrivateStoreMsgSell;
import l2jorion.game.network.serverpackets.QuestList;
import l2jorion.game.network.serverpackets.RecipeShopMsg;
import l2jorion.game.network.serverpackets.RecipeShopSellList;
import l2jorion.game.network.serverpackets.RelationChanged;
import l2jorion.game.network.serverpackets.Ride;
import l2jorion.game.network.serverpackets.SendTradeDone;
import l2jorion.game.network.serverpackets.SetupGauge;
import l2jorion.game.network.serverpackets.ShortBuffStatusUpdate;
import l2jorion.game.network.serverpackets.ShortCutInit;
import l2jorion.game.network.serverpackets.SkillCoolTime;
import l2jorion.game.network.serverpackets.SkillList;
import l2jorion.game.network.serverpackets.Snoop;
import l2jorion.game.network.serverpackets.SocialAction;
import l2jorion.game.network.serverpackets.StatusUpdate;
import l2jorion.game.network.serverpackets.StopMove;
import l2jorion.game.network.serverpackets.SystemMessage;
import l2jorion.game.network.serverpackets.TargetSelected;
import l2jorion.game.network.serverpackets.TargetUnselected;
import l2jorion.game.network.serverpackets.TitleUpdate;
import l2jorion.game.network.serverpackets.TradePressOtherOk;
import l2jorion.game.network.serverpackets.TradePressOwnOk;
import l2jorion.game.network.serverpackets.TradeStart;
import l2jorion.game.network.serverpackets.UserInfo;
import l2jorion.game.skills.BaseStats;
import l2jorion.game.skills.Formulas;
import l2jorion.game.skills.Stats;
import l2jorion.game.skills.effects.EffectCharge;
import l2jorion.game.skills.l2skills.L2SkillSummon;
import l2jorion.game.templates.L2Armor;
import l2jorion.game.templates.L2ArmorType;
import l2jorion.game.templates.L2EtcItemType;
import l2jorion.game.templates.L2Henna;
import l2jorion.game.templates.L2Item;
import l2jorion.game.templates.L2PcTemplate;
import l2jorion.game.templates.L2Weapon;
import l2jorion.game.templates.L2WeaponType;
import l2jorion.game.thread.LoginServerThread;
import l2jorion.game.thread.ThreadPoolManager;
import l2jorion.game.thread.daemons.ItemsAutoDestroy;
import l2jorion.game.util.Broadcast;
import l2jorion.game.util.FloodProtectors;
import l2jorion.game.util.IllegalPlayerAction;
import l2jorion.game.util.Util;
import l2jorion.log.Log;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;
import l2jorion.util.CloseUtil;
import l2jorion.util.RandomStringUtils;
import l2jorion.util.database.L2DatabaseFactory;
import l2jorion.util.random.Rnd;

public class L2PcInstance extends L2PlayableInstance
{
	protected static Logger LOG = LoggerFactory.getLogger(L2PcInstance.class);
	
	private List<String> _selectedBoss = new ArrayList<>();
	
	private boolean _expGainOn = true;
	private boolean _TitleOn = false;
	private boolean autoLootEnabled = false;
	private boolean autoLootHerbs = false;
	private boolean _tradeRefusal = false;
	private boolean _ipblock = false;
	private boolean _hwidblock = false;
	private boolean _screentxt = true;
	private boolean enchantGlow = false;
	private boolean teleportSystem = false;
	private boolean characterEffects = false;
	private boolean _messageRefusal = false;
	private boolean _pmSilenceMode = false;
	protected boolean _blockBuff = false;
	
	public boolean _isVIP = false, _inEventVIP = false, _isNotVIP = false, _isTheVIP = false;
	
	private final Location _lastLoc = new Location(0, 0, 0);
	
	private static boolean _votedForEvent = false;
	private static boolean _voteMenu = false;
	
	private FastSet<String> Favorites = new FastSet<>();
	
	private static final String UPDATE_CHARACTER = "UPDATE characters SET level=?,maxHp=?,curHp=?,maxCp=?,curCp=?,maxMp=?,curMp=?,str=?,con=?,dex=?,_int=?,men=?,wit=?,face=?,hairStyle=?,hairColor=?,heading=?,x=?,y=?,z=?,exp=?,expBeforeDeath=?,sp=?,karma=?,pvpkills=?,pkkills=?,rec_have=?,rec_left=?,clanid=?,maxload=?,race=?,classid=?,deletetime=?,title=?,accesslevel=?,online=?,isin7sdungeon=?,clan_privs=?,wantspeace=?,base_class=?,onlinetime=?,punish_level=?,punish_timer=?,newbie=?,nobless=?,power_grade=?,subpledge=?,last_recom_date=?,lvl_joined_academy=?,apprentice=?,sponsor=?,varka_ketra_ally=?,clan_join_expiry_time=?,clan_create_expiry_time=?,char_name=?,death_penalty_level=?,pc_point=?,name_color=?,title_color=?,aio=?,aio_end=?,hitman_target=?,autoloot=?,autoloot_herbs=? WHERE obj_id=?";
	private static final String RESTORE_CHARACTER = "SELECT account_name, obj_Id, char_name, level, maxHp, curHp, maxCp, curCp, maxMp, curMp, acc, crit, evasion, mAtk, mDef, mSpd, pAtk, pDef, pSpd, runSpd, walkSpd, str, con, dex, _int, men, wit, face, hairStyle, hairColor, sex, heading, x, y, z, movement_multiplier, attack_speed_multiplier, colRad, colHeight, exp, expBeforeDeath, sp, karma, pvpkills, pkkills, clanid, maxload, race, classid, deletetime, cancraft, title, rec_have, rec_left, accesslevel, online, char_slot, lastAccess, clan_privs, wantspeace, base_class, onlinetime, isin7sdungeon,punish_level,punish_timer,"
		+ "newbie, nobless, power_grade, subpledge, last_recom_date, lvl_joined_academy, apprentice, sponsor, varka_ketra_ally,clan_join_expiry_time,clan_create_expiry_time,death_penalty_level,pc_point"
		+ ",name_color,title_color,first_log,aio,aio_end,hitman_target,autoloot,autoloot_herbs FROM characters WHERE obj_id=?";
	
	private static final String UPDATE_CHARACTER2 = "UPDATE characters SET level=?,maxHp=?,curHp=?,maxCp=?,curCp=?,maxMp=?,curMp=?,str=?,con=?,dex=?,_int=?,men=?,wit=?,face=?,hairStyle=?,hairColor=?,heading=?,x=?,y=?,z=?,exp=?,expBeforeDeath=?,sp=?,karma=?,pvpkills=?,pkkills=?,rec_have=?,rec_left=?,clanid=?,maxload=?,race=?,classid=?,deletetime=?,title=?,accesslevel=?,online=?,isin7sdungeon=?,clan_privs=?,wantspeace=?,base_class=?,onlinetime=?,punish_level=?,punish_timer=?,newbie=?,nobless=?,power_grade=?,subpledge=?,last_recom_date=?,lvl_joined_academy=?,apprentice=?,sponsor=?,varka_ketra_ally=?,clan_join_expiry_time=?,clan_create_expiry_time=?,char_name=?,death_penalty_level=?,pc_point=?,name_color=?,title_color=?,aio=?,aio_end=?,hitman_target=?,autoloot=?,autoloot_herbs=?,fakeArmorObjectId=?,fakeArmorItemId=? WHERE obj_id=?";
	private static final String RESTORE_CHARACTER2 = "SELECT account_name, obj_Id, char_name, level, maxHp, curHp, maxCp, curCp, maxMp, curMp, acc, crit, evasion, mAtk, mDef, mSpd, pAtk, pDef, pSpd, runSpd, walkSpd, str, con, dex, _int, men, wit, face, hairStyle, hairColor, sex, heading, x, y, z, movement_multiplier, attack_speed_multiplier, colRad, colHeight, exp, expBeforeDeath, sp, karma, pvpkills, pkkills, clanid, maxload, race, classid, deletetime, cancraft, title, rec_have, rec_left, accesslevel, online, char_slot, lastAccess, clan_privs, wantspeace, base_class, onlinetime, isin7sdungeon,punish_level,punish_timer,"
		+ "newbie, nobless, power_grade, subpledge, last_recom_date, lvl_joined_academy, apprentice, sponsor, varka_ketra_ally,clan_join_expiry_time,clan_create_expiry_time,death_penalty_level,pc_point"
		+ ",name_color,title_color,first_log,aio,aio_end,hitman_target,autoloot,autoloot_herbs,fakeArmorObjectId FROM characters WHERE obj_id=?";
	
	private static final String STATUS_DATA_GET = "SELECT hero, noble, donator, hero_end_date FROM characters_custom_data WHERE obj_Id = ?";
	private static final String RESTORE_SKILLS_FOR_CHAR_ALT_SUBCLASS = "SELECT skill_id,skill_level FROM character_skills WHERE char_obj_id=? ORDER BY (skill_level+0)";
	private static final String RESTORE_CHAR_SUBCLASSES = "SELECT class_id,exp,sp,level,class_index FROM character_subclasses WHERE char_obj_id=? ORDER BY class_index ASC";
	private static final String ADD_CHAR_SUBCLASS = "INSERT INTO character_subclasses (char_obj_id,class_id,exp,sp,level,class_index) VALUES (?,?,?,?,?,?)";
	private static final String UPDATE_CHAR_SUBCLASS = "UPDATE character_subclasses SET exp=?,sp=?,level=?,class_id=? WHERE char_obj_id=? AND class_index =?";
	private static final String DELETE_CHAR_SUBCLASS = "DELETE FROM character_subclasses WHERE char_obj_id=? AND class_index=?";
	private static final String RESTORE_CHAR_HENNAS = "SELECT slot,symbol_id FROM character_hennas WHERE char_obj_id=? AND class_index=?";
	private static final String ADD_CHAR_HENNA = "INSERT INTO character_hennas (char_obj_id,symbol_id,slot,class_index) VALUES (?,?,?,?)";
	private static final String DELETE_CHAR_HENNA = "DELETE FROM character_hennas WHERE char_obj_id=? AND slot=? AND class_index=?";
	private static final String DELETE_CHAR_HENNAS = "DELETE FROM character_hennas WHERE char_obj_id=? AND class_index=?";
	private static final String DELETE_CHAR_SHORTCUTS = "DELETE FROM character_shortcuts WHERE char_obj_id=? AND class_index=?";
	private static final String RESTORE_CHAR_RECOMS = "SELECT char_id,target_id FROM character_recommends WHERE char_id=?";
	private static final String ADD_CHAR_RECOM = "INSERT INTO character_recommends (char_id,target_id) VALUES (?,?)";
	private static final String DELETE_CHAR_RECOMS = "DELETE FROM character_recommends WHERE char_id=?";
	private static final String RESTORE_SKILLS_FOR_CHAR = "SELECT skill_id,skill_level FROM character_skills WHERE char_obj_id=? AND class_index=?";
	private static final String ADD_NEW_SKILL = "INSERT INTO character_skills (char_obj_id,skill_id,skill_level,skill_name,class_index) VALUES (?,?,?,?,?)";
	private static final String UPDATE_CHARACTER_SKILL_LEVEL = "UPDATE character_skills SET skill_level=? WHERE skill_id=? AND char_obj_id=? AND class_index=?";
	private static final String DELETE_SKILL_FROM_CHAR = "DELETE FROM character_skills WHERE skill_id=? AND char_obj_id=? AND class_index=?";
	private static final String DELETE_CHAR_SKILLS = "DELETE FROM character_skills WHERE char_obj_id=? AND class_index=?";
	private static final String ADD_SKILL_SAVE = "INSERT INTO character_skills_save (char_obj_id,skill_id,skill_level,effect_count,effect_cur_time,reuse_delay,systime,restore_type,class_index,buff_index) VALUES (?,?,?,?,?,?,?,?,?,?)";
	private static final String RESTORE_SKILL_SAVE = "SELECT skill_id,skill_level,effect_count,effect_cur_time, reuse_delay, systime, restore_type FROM character_skills_save WHERE char_obj_id=? AND class_index=? ORDER BY buff_index ASC";
	private static final String AUTOLOOT_SET = "UPDATE characters SET autoloot=? WHERE obj_id=?";
	private static final String AUTOLOOT_GET = "SELECT autoloot FROM characters WHERE obj_id=?";
	private static final String AUTOHERBS_SET = "UPDATE characters SET autoloot_herbs=? WHERE obj_id=?";
	private static final String AUTOHERBS_GET = "SELECT autoloot_herbs FROM characters WHERE obj_id=?";
	private static final String BLOCKBUFF_SET = "UPDATE characters SET blockbuff=? WHERE obj_id=?";
	private static final String BLOCKBUFF_GET = "SELECT blockbuff FROM characters WHERE obj_id=?";
	private static final String DELETE_SKILL_SAVE = "DELETE FROM character_skills_save WHERE char_obj_id=? AND class_index=?";
	private static final String RESTORE_PREMIUMSERVICE = "SELECT premium_service,enddate FROM account_premium WHERE account_name=?";
	private static final String UPDATE_PREMIUMSERVICE = "UPDATE account_premium SET premium_service=?,enddate=? WHERE account_name=?";
	
	private Map<Integer, Future<?>> _autoPotTasks = new HashMap<>();
	
	public boolean isAutoPot(int id)
	{
		return _autoPotTasks.keySet().contains(id);
	}
	
	public void setAutoPot(int id, Future<?> task, boolean add)
	{
		if (add)
		{
			_autoPotTasks.put(id, task);
		}
		else
		{
			try
			{
				if (_autoPotTasks.get(id) != null)
				{
					_autoPotTasks.get(id).cancel(true);
					_autoPotTasks.remove(id);
				}
			}
			catch (Exception e)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
				{
					LOG.error("Error with potion: " + id + " (Name:" + getName() + ")");
					e.printStackTrace();
				}
			}
		}
	}
	
	public int _originalNameColourVIP, _originalKarmaVIP;
	
	private final SimpleDateFormat punishment_date = new SimpleDateFormat("dd/MM/yyyy H:mm:ss");
	String punishmentDate = punishment_date.format(new Date(System.currentTimeMillis()));
	
	public int counter = 0;
	
	private boolean _posticipateSit;
	
	protected boolean sittingTaskLaunched;
	
	private final long _instanceLoginTime;
	
	protected long TOGGLE_USE = 0;
	
	public int _active_boxes = -1;
	public List<String> active_boxes_characters = new ArrayList<>();
	
	public static final int REQUEST_TIMEOUT = 15;
	
	public static final int STORE_PRIVATE_NONE = 0;
	public static final int STORE_PRIVATE_SELL = 1;
	public static final int STORE_PRIVATE_BUY = 3;
	public static final int STORE_PRIVATE_MANUFACTURE = 5;
	public static final int STORE_PRIVATE_PACKAGE_SELL = 8;
	
	private static final int[] EXPERTISE_LEVELS =
	{
		SkillTreeTable.getInstance().getExpertiseLevel(0), // NONE
		SkillTreeTable.getInstance().getExpertiseLevel(1), // D
		SkillTreeTable.getInstance().getExpertiseLevel(2), // C
		SkillTreeTable.getInstance().getExpertiseLevel(3), // B
		SkillTreeTable.getInstance().getExpertiseLevel(4), // A
		SkillTreeTable.getInstance().getExpertiseLevel(5), // S
	};
	
	private static final int[] COMMON_CRAFT_LEVELS =
	{
		5,
		20,
		28,
		36,
		43,
		49,
		55,
		62
	};
	
	@Override
	public void doAttack(L2Character target)
	{
		if (isInsidePeaceZone(L2PcInstance.this, target))
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if ((TvT.is_teleport() && _inEventTvT) || (CTF.is_teleport() && _inEventCTF) || (DM.is_teleport() && _inEventDM))
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		// Pk protection config
		if (!isGM() && target instanceof L2PcInstance && ((L2PcInstance) target).getPvpFlag() == 0 && ((L2PcInstance) target).getKarma() == 0 && (getLevel() < Config.ALT_PLAYER_PROTECTION_LEVEL || target.getLevel() < Config.ALT_PLAYER_PROTECTION_LEVEL))
		{
			sendMessage("You can't hit a player that is lower level from you. Target's level: " + String.valueOf(Config.ALT_PLAYER_PROTECTION_LEVEL) + ".");
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (target instanceof L2PcInstance && ((L2PcInstance) target).getSiegeState() == 0 && (!checkAntiFarm((L2PcInstance) target)))
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		super.doAttack(target);
		
		// cancel the recent fake-death protection instantly if the player attacks or casts spells
		setRecentFakeDeath(false);
		
		synchronized (_cubics)
		{
			for (L2CubicInstance cubic : _cubics.values())
			{
				if (cubic.getId() != L2CubicInstance.LIFE_CUBIC)
				{
					cubic.doAction();
				}
			}
		}
	}
	
	@Override
	public void doCast(L2Skill skill)
	{
		// cancel the recent fake-death protection instantly if the player attacks or casts spells
		setRecentFakeDeath(false);
		
		// during teleport phase, players can't do any attack
		if ((TvT.is_teleport() && _inEventTvT) || (CTF.is_teleport() && _inEventCTF) || (DM.is_teleport() && _inEventDM))
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		super.doCast(skill);
	}
	
	private L2GameClient _client;
	
	private String _accountName;
	private long _deleteTimer;
	
	private boolean _isOnline = false;
	private long _onlineTime;
	private long _onlineBeginTime;
	private long _lastAccess;
	private long _uptime;
	
	private long _lastHopVote;
	private long _lastTopVote;
	private long _lastNetVote;
	private long _lastBraVote;
	private long _lastL2TopGr;
	private long _lastL2TopOnline;
	
	protected int _baseClass;
	protected int _activeClass;
	protected int _classIndex = 0;
	private boolean _first_log;
	private int pcBangPoint = 0;
	
	private Map<Integer, SubClass> _subClasses;
	
	private PcAppearance _appearance;
	
	private int _charId = 0x00030b7a;
	
	public boolean _pincheck;
	public int _pin;
	
	private long _expBeforeDeath;
	
	private int _karma;
	
	private int _pvpKills;
	
	private int _pkKills;
	
	private int _lastKill = 0;
	
	/** The count. */
	private int count = 0;
	
	/** The PvP Flag state of the L2PcInstance (0=White, 1=Purple). */
	private byte _pvpFlag;
	
	/** The Siege state of the L2PcInstance. */
	private byte _siegeState = 0;
	
	/** The _cur weight penalty. */
	private int _curWeightPenalty = 0;
	
	/** The _last compass zone. */
	private int _lastCompassZone; // the last compass zone update send to the client
	
	/** The _zone validate counter. */
	private byte _zoneValidateCounter = 4;
	
	private boolean _isInSiege;
	private boolean _isInHideoutSiege = false;
	/** The _is in7s dungeon. */
	private boolean _isIn7sDungeon = false;
	
	/** Special hero aura values. */
	private int heroConsecutiveKillCount = 0;
	
	/** The is pvp hero. */
	private boolean isPVPHero = false;
	
	/** The _original title color away. */
	public int _originalTitleColorAway;
	
	/** The _original title away. */
	public String _originalTitleAway;
	
	/** The _is aio. */
	private boolean _isAio = false;
	
	/** The _aio_end time. */
	private long _aio_endTime = 0;
	
	/** Event parameters. */
	public int eventX;
	
	/** The event y. */
	public int eventY;
	
	/** The event z. */
	public int eventZ;
	
	/** The event karma. */
	public int eventKarma;
	
	/** The event pvp kills. */
	public int eventPvpKills;
	
	/** The event pk kills. */
	public int eventPkKills;
	
	/** The event title. */
	public String eventTitle;
	
	/** The kills. */
	public List<String> kills = new LinkedList<>();
	
	/** The event sit forced. */
	public boolean eventSitForced = false;
	
	/** The at event. */
	public boolean atEvent = false;
	
	/** TvT Engine parameters. */
	public String _teamNameTvT, _originalTitleTvT;
	
	/** The _original karma tv t. */
	public int _originalNameColorTvT = 0, _countTvTkills, _countTvTdies, _originalKarmaTvT;
	
	/** The _in event tv t. */
	public boolean _inEventTvT = false;
	
	/** CTF Engine parameters. */
	public String _teamNameCTF, _teamNameHaveFlagCTF, _originalTitleCTF;
	
	/** The _count ct fflags. */
	public int _originalNameColorCTF = 0, _originalKarmaCTF, _countCTFflags, _countCTFkills;
	
	/** The _have flag ctf. */
	public boolean _inEventCTF = false, _haveFlagCTF = false;
	
	/** The _pos checker ctf. */
	public Future<?> _posCheckerCTF = null;
	
	/** DM Engine parameters. */
	public String _originalTitleDM;
	
	public int _countPVPkills = 0;
	/** The _original karma dm. */
	public int _originalNameColorDM = 0, _countDMkills, _originalKarmaDM;
	
	/** The _in event dm. */
	public boolean _inEventDM = false;
	
	/** The _correct word. */
	public int _correctWord = -1;
	
	/** The _stop kick bot task. */
	public boolean _stopKickBotTask = false;
	
	/** Event Engine parameters. */
	public int _originalNameColor, _countKills, _originalKarma, _eventKills;
	
	/** The _in event. */
	public boolean _inEvent = false;
	
	/** Duel. */
	private boolean _isInDuel = false;
	
	/** The _duel state. */
	private int _duelState = Duel.DUELSTATE_NODUEL;
	
	/** The _duel id. */
	private int _duelId = 0;
	
	/** The _no duel reason. */
	private SystemMessageId _noDuelReason = SystemMessageId.THERE_IS_NO_OPPONENT_TO_RECEIVE_YOUR_CHALLENGE_FOR_A_DUEL;
	
	/** Boat. */
	private L2Vehicle _vehicle = null;
	private Location _inVehiclePosition;
	
	/** The _mount type. */
	private int _mountType;
	
	/** Store object used to summon the strider you are mounting *. */
	private int _mountObjectID = 0;
	
	/** The _telemode. */
	public int _telemode = 0;
	
	/** The _is silent moving. */
	private int _isSilentMoving = 0;
	
	/** The _in crystallize. */
	private boolean _inCrystallize;
	
	/** The _in craft mode. */
	private boolean _inCraftMode;
	
	private final Map<Integer, L2RecipeList> _dwarvenRecipeBook = new FastMap<>();
	private final Map<Integer, L2RecipeList> _commonRecipeBook = new FastMap<>();
	
	/** True if the L2PcInstance is sitting. */
	private boolean _waitTypeSitting;
	
	/** True if the L2PcInstance is using the relax skill. */
	private boolean _relax;
	
	/** Location before entering Observer Mode. */
	private int _obsX;
	
	/** The _obs y. */
	private int _obsY;
	
	/** The _obs z. */
	private int _obsZ;
	
	/** Olympiad. */
	private boolean _observerMode = false;
	private boolean _inOlympiadMode = false;
	private boolean _OlympiadStart = false;
	private int _olympiadGameId = -1;
	private int _olympiadSide = -1;
	
	/** Stored from last ValidatePosition *. */
	private Location _lastClientPosition = new Location(0, 0, 0);
	
	/** The _last server position. */
	private Location _lastServerPosition = new Location(0, 0, 0);
	
	/** The number of recommandation obtained by the L2PcInstance. */
	private int _recomHave; // how much I was recommended by others
	
	/** The number of recommandation that the L2PcInstance can give. */
	private int _recomLeft; // how many recomendations I can give to others
	
	/** Date when recom points were updated last time. */
	private long _lastRecomUpdate;
	
	/** List with the recomendations that I've give. */
	private List<Integer> _recomChars = new FastList<>();
	
	private PcInventory _inventory = new PcInventory(this);
	
	/** The _warehouse. */
	private PcWarehouse _warehouse;
	
	/** The _freight. */
	private PcFreight _freight = new PcFreight(this);
	
	private int _privatestore;
	
	/** The _active trade list. */
	private TradeList _activeTradeList;
	
	/** The _active warehouse. */
	private ItemContainer _activeWarehouse;
	
	/** The _create list. */
	private L2ManufactureList _createList;
	
	/** The _sell list. */
	private TradeList _sellList;
	
	/** The _buy list. */
	private TradeList _buyList;
	
	/** True if the L2PcInstance is newbie. */
	private boolean _newbie;
	
	/** The _noble. */
	private boolean _noble = false;
	
	/** The _hero. */
	private boolean _hero = false;
	
	private boolean _sellbuff = false;
	private int _buffprize = 0;
	private String _oldtitle = "";
	private int _oldnamecolor = 0;
	
	/** The _donator. */
	private boolean _donator = false;
	
	/** The L2FolkInstance corresponding to the last Folk wich one the player talked. */
	private L2FolkInstance _lastFolkNpc = null;
	
	/** Last NPC Id talked on a quest. */
	private int _questNpcObject = 0;
	
	private int _party_find = 0;
	
	private int _fakeArmorObjectId;
	private int _fakeArmorItemId;
	
	public final List<Integer> _friendList = new ArrayList<>();
	private final List<Integer> _selectedFriendList = new ArrayList<>();
	
	// summon friend
	private SummonRequest _summonRequest = new SummonRequest();
	
	protected static class SummonRequest
	{
		private L2PcInstance _target = null;
		private L2Skill _skill = null;
		
		public void setTarget(L2PcInstance destination, L2Skill skill)
		{
			_target = destination;
			_skill = skill;
		}
		
		public L2PcInstance getTarget()
		{
			return _target;
		}
		
		public L2Skill getSkill()
		{
			return _skill;
		}
	}
	
	private Map<String, QuestState> _quests = new FastMap<>();
	
	private ShortCuts _shortCuts = new ShortCuts(this);
	
	private MacroList _macroses = new MacroList(this);
	
	private List<L2PcInstance> _snoopListener = new FastList<>();
	
	private List<L2PcInstance> _snoopedPlayer = new FastList<>();
	
	private ClassId _skillLearningClassId;
	
	// hennas
	private final L2HennaInstance[] _henna = new L2HennaInstance[3];
	private int _hennaSTR;
	private int _hennaINT;
	private int _hennaDEX;
	private int _hennaMEN;
	private int _hennaWIT;
	private int _hennaCON;
	
	private L2Summon _summon = null;
	private L2TamedBeastInstance _tamedBeast = null;
	
	// client radar
	private L2Radar _radar;
	
	// Clan related attributes
	private int _clanId = 0;
	private L2Clan _clan;
	private int _apprentice = 0;
	private int _sponsor = 0;
	private long _clanJoinExpiryTime;
	private long _clanCreateExpiryTime;
	private int _powerGrade = 0;
	private int _clanPrivileges = 0;
	private int _pledgeClass = 0;
	private int _pledgeType = 0;
	private int _lvlJoinedAcademy = 0;
	private int _wantsPeace = 0;
	private int _deathPenaltyBuffLevel = 0;
	
	private AccessLevel _accessLevel;
	
	private boolean _dietMode = false; // ignore weight penalty
	private boolean _exchangeRefusal = false; // Exchange refusal
	
	private L2Party _party;
	
	private long _lastAttackPacket = 0;
	
	private L2PcInstance _activeRequester;
	
	private long _requestExpireTime = 0;
	
	private L2Request _request = new L2Request(this);
	
	private L2ItemInstance _arrowItem;
	
	// Used for protection after teleport
	private long _protectEndTime = 0;
	
	public boolean isSpawnProtected()
	{
		return _protectEndTime > GameTimeController.getInstance().getGameTicks();
	}
	
	private long _teleportProtectEndTime = 0;
	
	public boolean isTeleportProtected()
	{
		return _teleportProtectEndTime > GameTimeController.getInstance().getGameTicks();
	}
	
	// protects a char from agro mobs when getting up from fake death
	private long _recentFakeDeathEndTime = 0;
	
	private L2Weapon _fistsWeaponItem;
	
	private final Map<Integer, String> _chars = new FastMap<>();
	
	/** The current higher Expertise of the L2PcInstance (None=0, D=1, C=2, B=3, A=4, S=5). */
	private int _expertiseIndex; // index in EXPERTISE_LEVELS
	
	/** The _expertise penalty. */
	private int _expertisePenalty = 0;
	
	/** The _heavy_mastery. */
	private boolean _heavy_mastery = false;
	
	/** The _light_mastery. */
	private boolean _light_mastery = false;
	
	/** The _robe_mastery. */
	private boolean _robe_mastery = false;
	
	/** The _mastery penalty. */
	private int _masteryPenalty = 0;
	
	/** The _active enchant item. */
	private L2ItemInstance _activeEnchantItem = null;
	
	/** The _inventory disable. */
	protected boolean _inventoryDisable = false;
	
	/** The _cubics. */
	protected Map<Integer, L2CubicInstance> _cubics = new FastMap<>();
	
	private Achievement _achievement = new Achievement(this);
	
	protected FastSet<Integer> _activeSoulShots = new FastSet<Integer>().shared();
	public final ReentrantLock soulShotLock = new ReentrantLock();
	
	/** The dialog. */
	public Quest dialog = null;
	
	/** new loto ticket *. */
	private int _loto[] = new int[5];
	/** new race ticket *. */
	private int _race[] = new int[2];
	
	/** The _block list. */
	private final BlockList _blockList = new BlockList(this);
	
	/** The _team. */
	private int _team = 0;
	
	/** lvl of alliance with ketra orcs or varka silenos, used in quests and aggro checks [-5,-1] varka, 0 neutral, [1,5] ketra. */
	private int _alliedVarkaKetra = 0;
	
	private int _hasCoupon = 0;
	
	/** The _fish combat. */
	private L2Fishing _fishCombat;
	
	/** The _fishing. */
	private boolean _fishing = false;
	
	private int _fishx = 0;
	private int _fishy = 0;
	private int _fishz = 0;
	
	/** The fish loc. */
	private Location _fishingLoc;
	
	/** The _task rent pet. */
	private ScheduledFuture<?> _taskRentPet;
	
	/** The _task water. */
	private ScheduledFuture<?> _taskWater;
	
	/** Bypass validations. */
	private List<String> _validBypass = new FastList<>();
	private List<String> _validBypass2 = new FastList<>();
	private List<String> _validLink = new FastList<>();
	
	/** The _forum mail. */
	private Forum _forumMail;
	
	/** The _forum memo. */
	private Forum _forumMemo;
	
	/** Current skill in use. */
	private SkillDat _currentSkill;
	private SkillDat _currentPetSkill;
	
	/** Skills queued because a skill is already in progress. */
	private SkillDat _queuedSkill;
	
	/* Flag to disable equipment/skills while wearing formal wear * */
	/** The _ is wearing formal wear. */
	private boolean _IsWearingFormalWear = false;
	
	/** The _current skill world position. */
	private Location _currentSkillWorldPosition;
	
	/** The _cursed weapon equiped id. */
	private int _cursedWeaponEquipedId = 0;
	
	/** The _revive requested. */
	private int _reviveRequested = 0;
	private double _revivePower = 0;
	private boolean _revivePet = false;
	
	private double _cpUpdateIncCheck = .0;
	private double _cpUpdateDecCheck = .0;
	private double _cpUpdateInterval = .0;
	private double _mpUpdateIncCheck = .0;
	private double _mpUpdateDecCheck = .0;
	private double _mpUpdateInterval = .0;
	
	// during fall validations will be disabled for 10 ms.
	private static final int FALLING_VALIDATION_DELAY = 10000;
	private long _fallingTimestamp = 0;
	
	/** The _is offline. */
	private boolean _isInOfflineMode = false;
	
	/** The _offline shop start. */
	private long _offlineShopStart = 0;
	
	public String _originalNameColorOffline = "FFFFFF";
	
	private int _mailPosition;
	
	private int _herbstask = 0;
	
	public class HerbTask implements Runnable
	{
		private String _process;
		private int _itemId;
		private int _count;
		private L2Object _reference;
		private boolean _sendMessage;
		
		HerbTask(String process, int itemId, int count, L2Object reference, boolean sendMessage)
		{
			_process = process;
			_itemId = itemId;
			_count = count;
			_reference = reference;
			_sendMessage = sendMessage;
		}
		
		@Override
		public void run()
		{
			try
			{
				addItem(_process, _itemId, _count, _reference, _sendMessage);
			}
			catch (Exception e)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
				{
					e.printStackTrace();
				}
			}
		}
	}
	
	// Wedding
	private boolean _married = false;
	private int _marriedType = 0;
	private int _partnerId = 0;
	private int _coupleId = 0;
	private boolean _engagerequest = false;
	private int _engageid = 0;
	private boolean _marryrequest = false;
	private boolean _marryaccepted = false;
	
	private int quakeSystem = 0;
	
	private boolean _isLocked = false;
	private boolean _isStored = false;
	
	public class SkillDat
	{
		private L2Skill _skill;
		private boolean _ctrlPressed;
		private boolean _shiftPressed;
		
		protected SkillDat(L2Skill skill, boolean ctrlPressed, boolean shiftPressed)
		{
			_skill = skill;
			_ctrlPressed = ctrlPressed;
			_shiftPressed = shiftPressed;
		}
		
		public boolean isCtrlPressed()
		{
			return _ctrlPressed;
		}
		
		public boolean isShiftPressed()
		{
			return _shiftPressed;
		}
		
		public L2Skill getSkill()
		{
			return _skill;
		}
		
		public int getSkillId()
		{
			return getSkill() != null ? getSkill().getId() : -1;
		}
	}
	
	public static L2PcInstance create(int objectId, L2PcTemplate template, String accountName, String name, byte hairStyle, byte hairColor, byte face, boolean sex)
	{
		// Create a new L2PcInstance with an account name
		PcAppearance app = new PcAppearance(face, hairColor, hairStyle, sex);
		L2PcInstance player = new L2PcInstance(objectId, template, accountName, app);
		
		// Set the name of the L2PcInstance
		player.setName(name);
		
		// Set the base class ID to that of the actual class ID.
		player.setBaseClass(player.getClassId());
		
		if (Config.ALT_GAME_NEW_CHAR_ALWAYS_IS_NEWBIE)
		{
			player.setNewbie(true);
		}
		
		// Add the player in the characters table of the database
		boolean ok = player.createDb();
		
		if (!ok)
		{
			return null;
		}
		
		return player;
	}
	
	public static L2PcInstance createDummyPlayer(int objectId, String name)
	{
		// Create a new L2PcInstance with an account name
		L2PcInstance player = new L2PcInstance(objectId);
		player.setName(name);
		
		return player;
	}
	
	public String getAccountName()
	{
		if (getClient() != null)
		{
			return getClient().getAccountName();
		}
		
		return _accountName;
	}
	
	public Map<Integer, String> getAccountChars()
	{
		return _chars;
	}
	
	public int getRelation(L2PcInstance target)
	{
		int result = 0;
		
		// karma and pvp may not be required
		if (getPvpFlag() != 0)
		{
			result |= RelationChanged.RELATION_PVP_FLAG;
		}
		
		if (getKarma() > 0)
		{
			result |= RelationChanged.RELATION_HAS_KARMA;
		}
		
		if (isClanLeader())
		{
			result |= RelationChanged.RELATION_LEADER;
		}
		
		if (getSiegeState() != 0)
		{
			result |= RelationChanged.RELATION_INSIEGE;
			if (getSiegeState() != target.getSiegeState())
			{
				result |= RelationChanged.RELATION_ENEMY;
			}
			else
			{
				result |= RelationChanged.RELATION_ALLY;
			}
			if (getSiegeState() == 1)
			{
				result |= RelationChanged.RELATION_ATTACKER;
			}
		}
		
		if (getClan() != null && target.getClan() != null)
		{
			if (target.getPledgeType() != L2Clan.SUBUNIT_ACADEMY && getPledgeType() != L2Clan.SUBUNIT_ACADEMY && target.getClan().isAtWarWith(getClan().getClanId()))
			{
				result |= RelationChanged.RELATION_1SIDED_WAR;
				if (getClan().isAtWarWith(target.getClan().getClanId()))
				{
					result |= RelationChanged.RELATION_MUTUAL_WAR;
				}
			}
		}
		return result;
	}
	
	public static L2PcInstance load(int objectId)
	{
		return restore(objectId);
	}
	
	private void initPcStatusUpdateValues()
	{
		_cpUpdateInterval = getMaxCp() / 352.0;
		_cpUpdateIncCheck = getMaxCp();
		_cpUpdateDecCheck = getMaxCp() - _cpUpdateInterval;
		_mpUpdateInterval = getMaxMp() / 352.0;
		_mpUpdateIncCheck = getMaxMp();
		_mpUpdateDecCheck = getMaxMp() - _mpUpdateInterval;
	}
	
	private L2PcInstance(int objectId, L2PcTemplate template, String accountName, PcAppearance app)
	{
		super(objectId, template);
		getKnownList(); // init knownlist
		getStat(); // init stats
		getStatus(); // init status
		super.initCharStatusUpdateValues();
		initPcStatusUpdateValues();
		
		_accountName = accountName;
		_appearance = app;
		
		// Create an AI
		getAI();
		
		// Create a L2Radar object
		_radar = new L2Radar(this);
		
		// Retrieve from the database all skills of this L2PcInstance and add them to _skills
		// Retrieve from the database all items of this L2PcInstance and add them to _inventory
		getInventory().restore();
		
		if (!Config.WAREHOUSE_CACHE)
		{
			getWarehouse();
		}
		
		getFreight().restore();
		
		_instanceLoginTime = System.currentTimeMillis();
	}
	
	private L2PcInstance(int objectId)
	{
		super(objectId, null);
		getKnownList(); // init knownlist
		getStat(); // init stats
		getStatus(); // init status
		super.initCharStatusUpdateValues();
		initPcStatusUpdateValues();
		
		_instanceLoginTime = System.currentTimeMillis();
	}
	
	@Override
	public final PcKnownList getKnownList()
	{
		if (super.getKnownList() == null || !(super.getKnownList() instanceof PcKnownList))
		{
			setKnownList(new PcKnownList(this));
		}
		return (PcKnownList) super.getKnownList();
	}
	
	@Override
	public final PcStat getStat()
	{
		if (super.getStat() == null || !(super.getStat() instanceof PcStat))
		{
			setStat(new PcStat(this));
		}
		return (PcStat) super.getStat();
	}
	
	@Override
	public final PcStatus getStatus()
	{
		if (super.getStatus() == null || !(super.getStatus() instanceof PcStatus))
		{
			setStatus(new PcStatus(this));
		}
		return (PcStatus) super.getStatus();
	}
	
	public final PcAppearance getAppearance()
	{
		return _appearance;
	}
	
	public final L2PcTemplate getBaseTemplate()
	{
		return CharTemplateTable.getInstance().getTemplate(_baseClass);
	}
	
	@Override
	public final L2PcTemplate getTemplate()
	{
		return (L2PcTemplate) super.getTemplate();
	}
	
	public void setTemplate(ClassId newclass)
	{
		super.setTemplate(CharTemplateTable.getInstance().getTemplate(newclass));
	}
	
	@Override
	protected L2CharacterAI initAI()
	{
		return new L2PlayerAI(this);
	}
	
	@Override
	public final int getLevel()
	{
		int level = getStat().getLevel();
		
		if (level == -1)
		{
			
			L2PcInstance local_char = restore(this.getObjectId());
			
			if (local_char != null)
			{
				level = local_char.getLevel();
			}
			
		}
		
		if (level < 0)
		{
			level = 1;
		}
		
		return level;
	}
	
	@Override
	public final String getLevels()
	{
		int level = getStat().getLevel();
		
		if (level == -1)
		{
			
			L2PcInstance local_char = restore(this.getObjectId());
			
			if (local_char != null)
			{
				level = local_char.getLevel();
			}
			
		}
		
		if (level < 0)
		{
			level = 1;
		}
		
		return "" + level;
	}
	
	/**
	 * Return the _newbie state of the L2PcInstance.<BR>
	 * <BR>
	 * @return true, if is newbie
	 */
	public boolean isNewbie()
	{
		return _newbie;
	}
	
	/**
	 * Set the _newbie state of the L2PcInstance.<BR>
	 * <BR>
	 * @param isNewbie The Identifier of the _newbie state<BR>
	 *            <BR>
	 */
	public void setNewbie(boolean isNewbie)
	{
		_newbie = isNewbie;
	}
	
	/**
	 * Sets the base class.
	 * @param baseClass the new base class
	 */
	public void setBaseClass(int baseClass)
	{
		_baseClass = baseClass;
	}
	
	/**
	 * Sets the base class.
	 * @param classId the new base class
	 */
	public void setBaseClass(ClassId classId)
	{
		_baseClass = classId.ordinal();
	}
	
	/**
	 * Checks if is in store mode.
	 * @return true, if is in store mode
	 */
	public boolean isInStoreMode()
	{
		return getPrivateStoreType() > 0;
	}
	
	/**
	 * Checks if is in craft mode.
	 * @return true, if is in craft mode
	 */
	public boolean isInCraftMode()
	{
		return _inCraftMode;
	}
	
	/**
	 * Checks if is in craft mode.
	 * @param b the b
	 */
	public void isInCraftMode(boolean b)
	{
		_inCraftMode = b;
	}
	
	/** The _kicked. */
	private boolean _kicked = false;
	
	/**
	 * Manage Logout Task.<BR>
	 * <BR>
	 * @param kicked the kicked
	 */
	public void logout(boolean kicked)
	{
		// prevent from player disconnect when in Event
		if (atEvent)
		{
			sendMessage("A superior power doesn't allow you to leave the event.");
			sendPacket(ActionFailed.STATIC_PACKET);
		}
		
		_kicked = kicked;
		
		closeNetConnection();
	}
	
	/**
	 * Checks if is kicked.
	 * @return true, if is kicked
	 */
	public boolean isKicked()
	{
		return _kicked;
	}
	
	/**
	 * Sets the kicked.
	 * @param value the new kicked
	 */
	public void setKicked(boolean value)
	{
		_kicked = value;
	}
	
	/**
	 * Manage Logout Task.<BR>
	 * <BR>
	 */
	public void logout()
	{
		logout(false);
	}
	
	/**
	 * Return a table containing all Common L2RecipeList of the L2PcInstance.<BR>
	 * <BR>
	 * @return the common recipe book
	 */
	public L2RecipeList[] getCommonRecipeBook()
	{
		return _commonRecipeBook.values().toArray(new L2RecipeList[_commonRecipeBook.values().size()]);
	}
	
	/**
	 * Return a table containing all Dwarf L2RecipeList of the L2PcInstance.<BR>
	 * <BR>
	 * @return the dwarven recipe book
	 */
	public L2RecipeList[] getDwarvenRecipeBook()
	{
		return _dwarvenRecipeBook.values().toArray(new L2RecipeList[_dwarvenRecipeBook.values().size()]);
	}
	
	/**
	 * Add a new L2RecipList to the table _commonrecipebook containing all L2RecipeList of the L2PcInstance <BR>
	 * <BR>
	 * .
	 * @param recipe The L2RecipeList to add to the _recipebook
	 * @param saveToDb
	 */
	public void registerCommonRecipeList(L2RecipeList recipe, boolean saveToDb)
	{
		_commonRecipeBook.put(recipe.getId(), recipe);
		
		if (saveToDb)
		{
			insertNewRecipeData(recipe.getId());
		}
	}
	
	/**
	 * Add a new L2RecipList to the table _recipebook containing all L2RecipeList of the L2PcInstance <BR>
	 * <BR>
	 * @param recipe The L2RecipeList to add to the _recipebook
	 * @param saveToDb
	 */
	public void registerDwarvenRecipeList(L2RecipeList recipe, boolean saveToDb)
	{
		_dwarvenRecipeBook.put(recipe.getId(), recipe);
		
		if (saveToDb)
		{
			insertNewRecipeData(recipe.getId());
		}
	}
	
	/**
	 * Checks for recipe list.
	 * @param recipeId the recipe id
	 * @return <b>TRUE</b> if player has the recipe on Common or Dwarven Recipe book else returns <b>FALSE</b>
	 */
	public boolean hasRecipeList(int recipeId)
	{
		if (_dwarvenRecipeBook.containsKey(recipeId))
		{
			return true;
		}
		else if (_commonRecipeBook.containsKey(recipeId))
		{
			return true;
		}
		else
		{
			return false;
		}
	}
	
	/**
	 * Tries to remove a L2RecipList from the table _DwarvenRecipeBook or from table _CommonRecipeBook, those table contain all L2RecipeList of the L2PcInstance <BR>
	 * <BR>
	 * .
	 * @param recipeId the recipe id
	 */
	public void unregisterRecipeList(int recipeId)
	{
		if (_dwarvenRecipeBook.remove(recipeId) != null)
		{
			deleteRecipeData(recipeId);
		}
		else if (_commonRecipeBook.remove(recipeId) != null)
		{
			deleteRecipeData(recipeId);
		}
		else
		{
			LOG.warn("Attempted to remove unknown RecipeList: " + recipeId);
		}
		
		L2ShortCut[] allShortCuts = getAllShortCuts();
		
		for (L2ShortCut sc : allShortCuts)
		{
			if (sc != null && sc.getId() == recipeId && sc.getType() == L2ShortCut.TYPE_RECIPE)
			{
				deleteShortCut(sc.getSlot(), sc.getPage());
			}
		}
		
		allShortCuts = null;
	}
	
	private void insertNewRecipeData(int recipeId)
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("DELETE FROM character_recipebook WHERE char_id=?");
			statement.setInt(1, getObjectId());
			statement.execute();
			statement.close();
			
			L2RecipeList[] recipes = getCommonRecipeBook();
			
			for (L2RecipeList recipe : recipes)
			{
				statement = con.prepareStatement("INSERT INTO character_recipebook (char_id, id, type) values(?,?,0)");
				statement.setInt(1, getObjectId());
				statement.setInt(2, recipe.getId());
				statement.execute();
				statement.close();
				statement = null;
			}
			
			recipes = getDwarvenRecipeBook();
			for (L2RecipeList recipe : recipes)
			{
				statement = con.prepareStatement("INSERT INTO character_recipebook (char_id, id, type) values(?,?,1)");
				statement.setInt(1, getObjectId());
				statement.setInt(2, recipe.getId());
				statement.execute();
				statement.close();
				statement = null;
			}
		}
		catch (SQLException e)
		{
			LOG.error("SQL exception while inserting recipe: " + recipeId + " from character " + getObjectId(), e);
		}
		finally
		{
			
			CloseUtil.close(con);
		}
	}
	
	private void deleteRecipeData(int recipeId)
	{
		Connection con = null;
		
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("DELETE FROM character_recipebook WHERE char_id=? AND id=?");
			statement.setInt(1, getObjectId());
			statement.setInt(2, recipeId);
			statement.execute();
			statement.close();
		}
		catch (SQLException e)
		{
			LOG.error("SQL exception while deleting recipe: " + recipeId + " from character " + getObjectId(), e);
		}
		finally
		{
			CloseUtil.close(con);
		}
	}
	
	/**
	 * Returns the Id for the last talked quest NPC.<BR>
	 * <BR>
	 * @return the last quest npc object
	 */
	public int getLastQuestNpcObject()
	{
		return _questNpcObject;
	}
	
	/**
	 * Sets the last quest npc object.
	 * @param npcId the new last quest npc object
	 */
	public void setLastQuestNpcObject(int npcId)
	{
		_questNpcObject = npcId;
	}
	
	/**
	 * Return the QuestState object corresponding to the quest name.<BR>
	 * <BR>
	 * @param name The name of the quest
	 * @return the quest state
	 */
	public QuestState getQuestState(String name)
	{
		for (QuestState qs : _quests.values())
		{
			if (name.equals(qs.getQuest().getName()))
			{
				return qs;
			}
		}
		return null;
	}
	
	/**
	 * Add a QuestState to the table _quest containing all quests began by the L2PcInstance.<BR>
	 * <BR>
	 * @param qs The QuestState to add to _quest
	 */
	public void setQuestState(QuestState qs)
	{
		_quests.put(qs.getQuestName(), qs);
	}
	
	/**
	 * Verify if the player has the quest state.
	 * @param quest the quest state to check
	 * @return {@code true} if the player has the quest state, {@code false} otherwise
	 */
	public boolean hasQuestState(String quest)
	{
		return _quests.containsKey(quest);
	}
	
	/**
	 * Remove a QuestState from the table _quest containing all quests began by the L2PcInstance.<BR>
	 * <BR>
	 * @param quest The name of the quest
	 */
	public void delQuestState(String quest)
	{
		_quests.remove(quest);
	}
	
	/**
	 * Adds the to quest state array.
	 * @param questStateArray the quest state array
	 * @param state the state
	 * @return the quest state[]
	 */
	private QuestState[] addToQuestStateArray(QuestState[] questStateArray, QuestState state)
	{
		int len = questStateArray.length;
		QuestState[] tmp = new QuestState[len + 1];
		for (int i = 0; i < len; i++)
		{
			tmp[i] = questStateArray[i];
		}
		tmp[len] = state;
		return tmp;
	}
	
	/**
	 * Return a table containing all Quest in progress from the table _quests.<BR>
	 * <BR>
	 * @return the all active quests
	 */
	public Quest[] getAllActiveQuests()
	{
		FastList<Quest> quests = new FastList<>();
		
		for (QuestState qs : _quests.values())
		{
			if (qs == null)
			{
				continue;
			}
			
			if (qs.getQuest().getQuestIntId() >= 1999)
			{
				continue;
			}
			
			// if (qs.isCompleted() && !Config.DEVELOPER)
			// {
			// continue;
			// }
			
			// if (!qs.isStarted() && !Config.DEVELOPER)
			// {
			// continue;
			// }
			
			quests.add(qs.getQuest());
		}
		
		return quests.toArray(new Quest[quests.size()]);
	}
	
	public Quest[] getAllActiveQuestsForNpc()
	{
		FastList<Quest> quests = new FastList<>();
		
		for (QuestState qs : _quests.values())
		{
			if (qs == null)
			{
				continue;
			}
			
			if (qs.getQuest().getQuestIntId() >= 1999)
			{
				continue;
			}
			
			// if (qs.isCompleted() && !Config.DEVELOPER)
			// {
			// continue;
			// }
			
			if (!qs.isStarted() && !Config.DEVELOPER)
			{
				continue;
			}
			
			quests.add(qs.getQuest());
		}
		
		return quests.toArray(new Quest[quests.size()]);
	}
	
	/**
	 * Return a table containing all QuestState to modify after a L2Attackable killing.<BR>
	 * <BR>
	 * @param npc the npc
	 * @return the quests for attacks
	 */
	public QuestState[] getQuestsForAttacks(L2NpcInstance npc)
	{
		// Create a QuestState table that will contain all QuestState to modify
		QuestState[] states = null;
		
		// Go through the QuestState of the L2PcInstance quests
		for (Quest quest : npc.getTemplate().getEventQuests(Quest.QuestEventType.ON_ATTACK))
		{
			// Check if the Identifier of the L2Attackable attck is needed for the current quest
			if (getQuestState(quest.getName()) != null)
			{
				// Copy the current L2PcInstance QuestState in the QuestState table
				if (states == null)
				{
					states = new QuestState[]
					{
						getQuestState(quest.getName())
					};
				}
				else
				{
					states = addToQuestStateArray(states, getQuestState(quest.getName()));
				}
			}
		}
		
		// Return a table containing all QuestState to modify
		return states;
	}
	
	/**
	 * Return a table containing all QuestState to modify after a L2Attackable killing.<BR>
	 * <BR>
	 * @param npc the npc
	 * @return the quests for kills
	 */
	public QuestState[] getQuestsForKills(L2NpcInstance npc)
	{
		// Create a QuestState table that will contain all QuestState to modify
		QuestState[] states = null;
		
		// Go through the QuestState of the L2PcInstance quests
		for (Quest quest : npc.getTemplate().getEventQuests(Quest.QuestEventType.ON_KILL))
		{
			// Check if the Identifier of the L2Attackable killed is needed for the current quest
			if (getQuestState(quest.getName()) != null)
			{
				// Copy the current L2PcInstance QuestState in the QuestState table
				if (states == null)
				{
					states = new QuestState[]
					{
						getQuestState(quest.getName())
					};
				}
				else
				{
					states = addToQuestStateArray(states, getQuestState(quest.getName()));
				}
			}
		}
		
		// Return a table containing all QuestState to modify
		return states;
	}
	
	/**
	 * Return a table containing all QuestState from the table _quests in which the L2PcInstance must talk to the NPC.<BR>
	 * <BR>
	 * @param npcId The Identifier of the NPC
	 * @return the quests for talk
	 */
	public QuestState[] getQuestsForTalk(int npcId)
	{
		// Create a QuestState table that will contain all QuestState to modify
		QuestState[] states = null;
		
		// Go through the QuestState of the L2PcInstance quests
		for (Quest quest : NpcTable.getInstance().getTemplate(npcId).getEventQuests(Quest.QuestEventType.QUEST_TALK))
		{
			if (quest != null)
			{
				// Copy the current L2PcInstance QuestState in the QuestState table
				if (getQuestState(quest.getName()) != null)
				{
					if (states == null)
					{
						states = new QuestState[]
						{
							getQuestState(quest.getName())
						};
					}
					else
					{
						states = addToQuestStateArray(states, getQuestState(quest.getName()));
					}
				}
			}
		}
		
		// Return a table containing all QuestState to modify
		return states;
	}
	
	public QuestState processQuestEvent(String quest, String event)
	{
		QuestState retval = null;
		if (event == null)
		{
			event = "";
		}
		
		if (!_quests.containsKey(quest))
		{
			return retval;
		}
		
		QuestState qs = getQuestState(quest);
		if (qs == null && event.length() == 0)
		{
			return retval;
		}
		
		if (qs == null)
		{
			Quest q = null;
			if (!Config.ALT_DEV_NO_QUESTS)
			{
				q = QuestManager.getInstance().getQuest(quest);
			}
			
			if (q == null)
			{
				return retval;
			}
			qs = q.newQuestState(this);
		}
		
		if (qs != null)
		{
			if (getLastQuestNpcObject() > 0)
			{
				L2Object object = L2World.getInstance().findObject(getLastQuestNpcObject());
				if (object instanceof L2NpcInstance && isInsideRadius(object, L2NpcInstance.INTERACTION_DISTANCE, false, false))
				{
					L2NpcInstance npc = (L2NpcInstance) object;
					QuestState[] states = getQuestsForTalk(npc.getNpcId());
					
					if (states != null)
					{
						for (QuestState state : states)
						{
							if (state.getQuest().getQuestIntId() == qs.getQuest().getQuestIntId() && !qs.isCompleted())
							{
								if (qs.getQuest().notifyEvent(event, npc, this))
								{
									showQuestWindow(quest, qs.getStateId());
								}
								
								retval = qs;
							}
						}
						sendPacket(new QuestList());
					}
				}
			}
		}
		
		return retval;
	}
	
	/**
	 * Show quest window.
	 * @param questId the quest id
	 * @param stateId the state id
	 */
	private void showQuestWindow(String questId, String stateId)
	{
		String path = "data/scripts/quests/" + questId + "/" + stateId + ".htm";
		String content = HtmCache.getInstance().getHtm(path);
		
		if (content != null)
		{
			NpcHtmlMessage npcReply = new NpcHtmlMessage(5);
			npcReply.setHtml(content);
			sendPacket(npcReply);
		}
		
		sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	/**
	 * Return a table containing all L2ShortCut of the L2PcInstance.<BR>
	 * <BR>
	 * @return the all short cuts
	 */
	public L2ShortCut[] getAllShortCuts()
	{
		return _shortCuts.getAllShortCuts();
	}
	
	/**
	 * Return the L2ShortCut of the L2PcInstance corresponding to the position (page-slot).<BR>
	 * <BR>
	 * @param slot The slot in wich the shortCuts is equiped
	 * @param page The page of shortCuts containing the slot
	 * @return the short cut
	 */
	public L2ShortCut getShortCut(int slot, int page)
	{
		return _shortCuts.getShortCut(slot, page);
	}
	
	/**
	 * Add a L2shortCut to the L2PcInstance _shortCuts<BR>
	 * <BR>
	 * .
	 * @param shortcut the shortcut
	 */
	public void registerShortCut(L2ShortCut shortcut)
	{
		_shortCuts.registerShortCut(shortcut);
	}
	
	/**
	 * Delete the L2ShortCut corresponding to the position (page-slot) from the L2PcInstance _shortCuts.<BR>
	 * <BR>
	 * @param slot the slot
	 * @param page the page
	 */
	public void deleteShortCut(int slot, int page)
	{
		_shortCuts.deleteShortCut(slot, page);
	}
	
	/**
	 * Add a L2Macro to the L2PcInstance _macroses<BR>
	 * <BR>
	 * .
	 * @param macro the macro
	 */
	public void registerMacro(L2Macro macro)
	{
		_macroses.registerMacro(macro);
	}
	
	/**
	 * Delete the L2Macro corresponding to the Identifier from the L2PcInstance _macroses.<BR>
	 * <BR>
	 * @param id the id
	 */
	public void deleteMacro(int id)
	{
		_macroses.deleteMacro(id);
	}
	
	/**
	 * Return all L2Macro of the L2PcInstance.<BR>
	 * <BR>
	 * @return the macroses
	 */
	public MacroList getMacroses()
	{
		return _macroses;
	}
	
	/**
	 * Set the siege state of the L2PcInstance.<BR>
	 * <BR>
	 * 1 = attacker, 2 = defender, 0 = not involved
	 * @param siegeState the new siege state
	 */
	public void setSiegeState(byte siegeState)
	{
		_siegeState = siegeState;
	}
	
	/**
	 * Get the siege state of the L2PcInstance.<BR>
	 * <BR>
	 * 1 = attacker, 2 = defender, 0 = not involved
	 * @return the siege state
	 */
	public byte getSiegeState()
	{
		return _siegeState;
	}
	
	/**
	 * Set the PvP Flag of the L2PcInstance.<BR>
	 * <BR>
	 * @param pvpFlag the new pvp flag
	 */
	public void setPvpFlag(int pvpFlag)
	{
		_pvpFlag = (byte) pvpFlag;
	}
	
	/**
	 * Gets the pvp flag.
	 * @return the pvp flag
	 */
	public byte getPvpFlag()
	{
		return _pvpFlag;
	}
	
	@Override
	public void updatePvPFlag(int value)
	{
		if (getPvpFlag() == value)
		{
			return;
		}
		
		setPvpFlag(value);
		
		sendPacket(new UserInfo(this));
		
		// If this player has a pet update the pets pvp flag as well
		if (getPet() != null)
		{
			sendPacket(new RelationChanged(getPet(), getRelation(this), false));
		}
		
		for (L2PcInstance target : getKnownList().getKnownPlayers().values())
		{
			if (target == null)
			{
				continue;
			}
			
			target.sendPacket(new RelationChanged(this, getRelation(this), isAutoAttackable(target)));
			if (getPet() != null)
			{
				target.sendPacket(new RelationChanged(getPet(), getRelation(this), isAutoAttackable(target)));
			}
		}
	}
	
	@Override
	public void revalidateZone(boolean force)
	{
		if (getWorldRegion() == null)
		{
			return;
		}
		
		if (force)
		{
			_zoneValidateCounter = 4;
		}
		else
		{
			_zoneValidateCounter--;
			if (_zoneValidateCounter < 0)
			{
				_zoneValidateCounter = 4;
			}
			else
			{
				return;
			}
		}
		
		getWorldRegion().revalidateZones(this);
		
		if (Config.ALLOW_WATER)
		{
			checkWaterState();
		}
		
		if (isInsideZone(ZoneId.ZONE_SIEGE))
		{
			if (_lastCompassZone == ExSetCompassZoneCode.SIEGEWARZONE2)
			{
				return;
			}
			
			_lastCompassZone = ExSetCompassZoneCode.SIEGEWARZONE2;
			ExSetCompassZoneCode cz = new ExSetCompassZoneCode(ExSetCompassZoneCode.SIEGEWARZONE2);
			sendPacket(cz);
		}
		else if (isInsideZone(ZoneId.ZONE_PVP))
		{
			if (_lastCompassZone == ExSetCompassZoneCode.PVPZONE)
			{
				return;
			}
			
			_lastCompassZone = ExSetCompassZoneCode.PVPZONE;
			ExSetCompassZoneCode cz = new ExSetCompassZoneCode(ExSetCompassZoneCode.PVPZONE);
			sendPacket(cz);
		}
		else if (isIn7sDungeon())
		{
			if (_lastCompassZone == ExSetCompassZoneCode.SEVENSIGNSZONE)
			{
				return;
			}
			
			_lastCompassZone = ExSetCompassZoneCode.SEVENSIGNSZONE;
			ExSetCompassZoneCode cz = new ExSetCompassZoneCode(ExSetCompassZoneCode.SEVENSIGNSZONE);
			sendPacket(cz);
		}
		else if (isInsideZone(ZoneId.ZONE_PEACE))
		{
			if (_lastCompassZone == ExSetCompassZoneCode.PEACEZONE)
			{
				return;
			}
			
			_lastCompassZone = ExSetCompassZoneCode.PEACEZONE;
			ExSetCompassZoneCode cz = new ExSetCompassZoneCode(ExSetCompassZoneCode.PEACEZONE);
			sendPacket(cz);
		}
		else
		{
			if (_lastCompassZone == ExSetCompassZoneCode.GENERALZONE)
			{
				return;
			}
			
			if (_lastCompassZone == ExSetCompassZoneCode.SIEGEWARZONE2)
			{
				updatePvPStatus();
			}
			
			_lastCompassZone = ExSetCompassZoneCode.GENERALZONE;
			ExSetCompassZoneCode cz = new ExSetCompassZoneCode(ExSetCompassZoneCode.GENERALZONE);
			sendPacket(cz);
		}
	}
	
	/**
	 * Return True if the L2PcInstance can Craft Dwarven Recipes.<BR>
	 * <BR>
	 * @return true, if successful
	 */
	public boolean hasDwarvenCraft()
	{
		return getSkillLevel(L2Skill.SKILL_CREATE_DWARVEN) >= 1;
	}
	
	/**
	 * Gets the dwarven craft.
	 * @return the dwarven craft
	 */
	public int getDwarvenCraft()
	{
		return getSkillLevel(L2Skill.SKILL_CREATE_DWARVEN);
	}
	
	/**
	 * Return True if the L2PcInstance can Craft Dwarven Recipes.<BR>
	 * <BR>
	 * @return true, if successful
	 */
	public boolean hasCommonCraft()
	{
		return getSkillLevel(L2Skill.SKILL_CREATE_COMMON) >= 1;
	}
	
	/**
	 * Gets the common craft.
	 * @return the common craft
	 */
	public int getCommonCraft()
	{
		return getSkillLevel(L2Skill.SKILL_CREATE_COMMON);
	}
	
	/**
	 * Return the PK counter of the L2PcInstance.<BR>
	 * <BR>
	 * @return the pk kills
	 */
	public int getPkKills()
	{
		return _pkKills;
	}
	
	/**
	 * Set the PK counter of the L2PcInstance.<BR>
	 * <BR>
	 * @param pkKills the new pk kills
	 */
	public void setPkKills(int pkKills)
	{
		_pkKills = pkKills;
	}
	
	/**
	 * Return the _deleteTimer of the L2PcInstance.<BR>
	 * <BR>
	 * @return the delete timer
	 */
	public long getDeleteTimer()
	{
		return _deleteTimer;
	}
	
	/**
	 * Set the _deleteTimer of the L2PcInstance.<BR>
	 * <BR>
	 * @param deleteTimer the new delete timer
	 */
	public void setDeleteTimer(long deleteTimer)
	{
		_deleteTimer = deleteTimer;
	}
	
	/**
	 * Return the current weight of the L2PcInstance.<BR>
	 * <BR>
	 * @return the current load
	 */
	public int getCurrentLoad()
	{
		return _inventory.getTotalWeight();
	}
	
	/**
	 * Return date of las update of recomPoints.
	 * @return the last recom update
	 */
	public long getLastRecomUpdate()
	{
		return _lastRecomUpdate;
	}
	
	/**
	 * Sets the last recom update.
	 * @param date the new last recom update
	 */
	public void setLastRecomUpdate(long date)
	{
		_lastRecomUpdate = date;
	}
	
	/**
	 * Return the number of recommandation obtained by the L2PcInstance.<BR>
	 * <BR>
	 * @return the recom have
	 */
	public int getRecomHave()
	{
		return _recomHave;
	}
	
	/**
	 * Increment the number of recommandation obtained by the L2PcInstance (Max : 255).<BR>
	 * <BR>
	 */
	protected void incRecomHave()
	{
		if (_recomHave < 255)
		{
			_recomHave++;
		}
	}
	
	/**
	 * Set the number of recommandation obtained by the L2PcInstance (Max : 255).<BR>
	 * <BR>
	 * @param value the new recom have
	 */
	public void setRecomHave(int value)
	{
		if (value > 255)
		{
			_recomHave = 255;
		}
		else if (value < 0)
		{
			_recomHave = 0;
		}
		else
		{
			_recomHave = value;
		}
	}
	
	/**
	 * Return the number of recommandation that the L2PcInstance can give.<BR>
	 * <BR>
	 * @return the recom left
	 */
	public int getRecomLeft()
	{
		return _recomLeft;
	}
	
	/**
	 * Increment the number of recommandation that the L2PcInstance can give.<BR>
	 * <BR>
	 */
	protected void decRecomLeft()
	{
		if (_recomLeft > 0)
		{
			_recomLeft--;
		}
	}
	
	/**
	 * Give recom.
	 * @param target the target
	 */
	public void giveRecom(L2PcInstance target)
	{
		if (Config.ALT_RECOMMEND)
		{
			Connection con = null;
			try
			{
				con = L2DatabaseFactory.getInstance().getConnection();
				PreparedStatement statement = con.prepareStatement(ADD_CHAR_RECOM);
				statement.setInt(1, getObjectId());
				statement.setInt(2, target.getObjectId());
				statement.execute();
				statement.close();
				statement = null;
			}
			catch (Exception e)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
				{
					LOG.error("could not update char recommendations:");
					e.printStackTrace();
				}
			}
			finally
			{
				CloseUtil.close(con);
			}
		}
		
		target.incRecomHave();
		decRecomLeft();
		_recomChars.add(target.getObjectId());
	}
	
	/**
	 * Can recom.
	 * @param target the target
	 * @return true, if successful
	 */
	public boolean canRecom(L2PcInstance target)
	{
		return !_recomChars.contains(target.getObjectId());
	}
	
	/**
	 * Set the exp of the L2PcInstance before a death.
	 * @param exp the new exp before death
	 */
	public void setExpBeforeDeath(long exp)
	{
		_expBeforeDeath = exp;
	}
	
	/**
	 * Gets the exp before death.
	 * @return the exp before death
	 */
	public long getExpBeforeDeath()
	{
		return _expBeforeDeath;
	}
	
	/**
	 * Return the Karma of the L2PcInstance.<BR>
	 * <BR>
	 * @return the karma
	 */
	public int getKarma()
	{
		return _karma;
	}
	
	/**
	 * Set the Karma of the L2PcInstance and send a Server->Client packet StatusUpdate (broadcast).<BR>
	 * <BR>
	 * @param karma the new karma
	 */
	public void setKarma(int karma)
	{
		if (!isCursedWeaponEquipped() && getAchievement().getCount(AchType.KARMA) < karma)
		{
			getAchievement().increase(AchType.KARMA, karma, false, false);
		}
		
		if (karma < 0)
		{
			karma = 0;
		}
		
		if (_karma == 0 && karma > 0)
		{
			for (L2Object object : getKnownList().getKnownObjects().values())
			{
				if (object == null || !(object instanceof L2GuardInstance))
				{
					continue;
				}
				
				if (((L2GuardInstance) object).getAI().getIntention() == CtrlIntention.AI_INTENTION_IDLE)
				{
					((L2GuardInstance) object).getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE, null);
				}
			}
		}
		else if (_karma > 0 && karma == 0)
		{
			// Send a Server->Client StatusUpdate packet with Karma and PvP Flag to the L2PcInstance and all L2PcInstance to inform (broadcast)
			setKarmaFlag(0);
		}
		
		// send message with new karma value
		// sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOUR_KARMA_HAS_BEEN_CHANGED_TO).addNumber(karma));
		sendPacket(new SystemMessage(SystemMessageId.YOUR_KARMA_HAS_BEEN_CHANGED_TO).addNumber(karma));
		
		_karma = karma;
		broadcastKarma();
	}
	
	/**
	 * Return the max weight that the L2PcInstance can load.<BR>
	 * <BR>
	 * @return the max load
	 */
	public int getMaxLoad()
	{
		// Weight Limit = (CON Modifier*69000)*Skills
		// Source http://l2p.bravehost.com/weightlimit.html (May 2007)
		// Fitted exponential curve to the data
		int con = getCON();
		if (con < 1)
		{
			return 31000;
		}
		
		if (con > 59)
		{
			return 176000;
		}
		
		double baseLoad = Math.floor(BaseStats.CON.calcBonus(this) * 69000 * Config.ALT_WEIGHT_LIMIT);
		return (int) calcStat(Stats.MAX_LOAD, baseLoad, this, null);
	}
	
	/**
	 * Gets the expertise penalty.
	 * @return the expertise penalty
	 */
	public int getExpertisePenalty()
	{
		return _expertisePenalty;
	}
	
	/**
	 * Gets the mastery penalty.
	 * @return the mastery penalty
	 */
	public int getMasteryPenalty()
	{
		return _masteryPenalty;
	}
	
	/**
	 * Gets the mastery weap penalty.
	 * @return the mastery weap penalty
	 */
	public int getMasteryWeapPenalty()
	{
		return _masteryWeapPenalty;
	}
	
	/**
	 * Gets the weight penalty.
	 * @return the weight penalty
	 */
	public int getWeightPenalty()
	{
		if (_dietMode)
		{
			return 0;
		}
		return _curWeightPenalty;
	}
	
	/**
	 * Update the overloaded status of the L2PcInstance.<BR>
	 * <BR>
	 */
	public void refreshOverloaded()
	{
		if (Config.DISABLE_WEIGHT_PENALTY)
		{
			setIsOverloaded(false);
		}
		else if (_dietMode)
		{
			setIsOverloaded(false);
			_curWeightPenalty = 0;
			super.removeSkill(getKnownSkill(4270));
			
			sendPacket(new EtcStatusUpdate(this));
			Broadcast.toKnownPlayers(this, new CharInfo(this));
		}
		else
		{
			int maxLoad = getMaxLoad();
			if (maxLoad > 0)
			{
				// setIsOverloaded(getCurrentLoad() > maxLoad);
				// int weightproc = getCurrentLoad() * 1000 / maxLoad;
				long weightproc = (long) ((getCurrentLoad() - calcStat(Stats.WEIGHT_PENALTY, 1, this, null)) * 1000 / maxLoad);
				int newWeightPenalty;
				
				if (weightproc < 500)
				{
					newWeightPenalty = 0;
				}
				else if (weightproc < 666)
				{
					newWeightPenalty = 1;
				}
				else if (weightproc < 800)
				{
					newWeightPenalty = 2;
				}
				else if (weightproc < 1000)
				{
					newWeightPenalty = 3;
				}
				else
				{
					newWeightPenalty = 4;
				}
				
				if (_curWeightPenalty != newWeightPenalty)
				{
					_curWeightPenalty = newWeightPenalty;
					if (newWeightPenalty > 0)
					{
						super.addSkill(SkillTable.getInstance().getInfo(4270, newWeightPenalty));
						sendSkillList(); // Fix visual bug
					}
					else
					{
						super.removeSkill(getKnownSkill(4270));
						sendSkillList(); // Fix visual bug
					}
					
					sendPacket(new EtcStatusUpdate(this));
					Broadcast.toKnownPlayers(this, new CharInfo(this));
				}
			}
		}
		
		sendPacket(new UserInfo(this));
	}
	
	/**
	 * Refresh mastery penality.
	 */
	public void refreshMasteryPenality()
	{
		if (!Config.MASTERY_PENALTY || this.getLevel() <= Config.LEVEL_TO_GET_PENALITY)
		{
			return;
		}
		
		_heavy_mastery = false;
		_light_mastery = false;
		_robe_mastery = false;
		
		L2Skill[] char_skills = this.getAllSkills();
		
		for (L2Skill actual_skill : char_skills)
		{
			if (actual_skill.getName().contains("Heavy Armor Mastery"))
			{
				_heavy_mastery = true;
			}
			
			if (actual_skill.getName().contains("Light Armor Mastery"))
			{
				_light_mastery = true;
			}
			
			if (actual_skill.getName().contains("Robe Mastery"))
			{
				_robe_mastery = true;
			}
		}
		
		int newMasteryPenalty = 0;
		
		if (!_heavy_mastery && !_light_mastery && !_robe_mastery)
		{
			// not completed 1st class transfer or not acquired yet the mastery skills
			newMasteryPenalty = 0;
		}
		else
		{
			for (L2ItemInstance item : getInventory().getItems())
			{
				if (item != null && item.isEquipped() && item.getItem() instanceof L2Armor)
				{
					// No penality for formal wear
					if (item.getItemId() == 6408)
					{
						continue;
					}
					
					L2Armor armor_item = (L2Armor) item.getItem();
					
					switch (armor_item.getItemType())
					{
						case HEAVY:
						{
							if (!_heavy_mastery)
							{
								newMasteryPenalty++;
							}
						}
							break;
						case LIGHT:
						{
							if (!_light_mastery)
							{
								newMasteryPenalty++;
							}
						}
							break;
						case MAGIC:
						{
							if (!_robe_mastery)
							{
								newMasteryPenalty++;
							}
						}
							break;
						default:
							break;
					}
				}
			}
		}
		
		if (_masteryPenalty != newMasteryPenalty)
		{
			int penalties = _masteryWeapPenalty + _expertisePenalty + newMasteryPenalty;
			
			if (penalties > 0)
			{
				super.addSkill(SkillTable.getInstance().getInfo(4267, 1)); // level used to be newPenalty
			}
			else
			{
				super.removeSkill(getKnownSkill(4267));
			}
			
			sendPacket(new EtcStatusUpdate(this));
			_masteryPenalty = newMasteryPenalty;
		}
	}
	
	/** The _blunt_mastery. */
	private boolean _blunt_mastery = false;
	
	/** The _pole_mastery. */
	private boolean _pole_mastery = false;
	
	/** The _dagger_mastery. */
	private boolean _dagger_mastery = false;
	
	/** The _sword_mastery. */
	private boolean _sword_mastery = false;
	
	/** The _bow_mastery. */
	private boolean _bow_mastery = false;
	
	/** The _fist_mastery. */
	private boolean _fist_mastery = false;
	
	/** The _dual_mastery. */
	private boolean _dual_mastery = false;
	
	/** The _2hands_mastery. */
	private boolean _2hands_mastery = false;
	
	/** The _mastery weap penalty. */
	private int _masteryWeapPenalty = 0;
	
	/**
	 * Refresh mastery weap penality.
	 */
	public void refreshMasteryWeapPenality()
	{
		if (!Config.MASTERY_WEAPON_PENALTY || this.getLevel() <= Config.LEVEL_TO_GET_WEAPON_PENALITY)
		{
			return;
		}
		
		_blunt_mastery = false;
		_bow_mastery = false;
		_dagger_mastery = false;
		_fist_mastery = false;
		_dual_mastery = false;
		_pole_mastery = false;
		_sword_mastery = false;
		_2hands_mastery = false;
		
		L2Skill[] char_skills = this.getAllSkills();
		
		for (L2Skill actual_skill : char_skills)
		{
			
			if (actual_skill.getName().contains("Sword Blunt Mastery"))
			{
				_sword_mastery = true;
				_blunt_mastery = true;
				continue;
			}
			
			if (actual_skill.getName().contains("Blunt Mastery"))
			{
				_blunt_mastery = true;
				continue;
			}
			
			if (actual_skill.getName().contains("Bow Mastery"))
			{
				_bow_mastery = true;
				continue;
			}
			
			if (actual_skill.getName().contains("Dagger Mastery"))
			{
				_dagger_mastery = true;
				continue;
			}
			
			if (actual_skill.getName().contains("Fist Mastery"))
			{
				_fist_mastery = true;
				continue;
			}
			
			if (actual_skill.getName().contains("Dual Weapon Mastery"))
			{
				_dual_mastery = true;
				continue;
			}
			
			if (actual_skill.getName().contains("Polearm Mastery"))
			{
				_pole_mastery = true;
				continue;
			}
			
			if (actual_skill.getName().contains("Two-handed Weapon Mastery"))
			{
				_2hands_mastery = true;
				continue;
			}
		}
		
		int newMasteryPenalty = 0;
		
		if (!_bow_mastery && !_blunt_mastery && !_dagger_mastery && !_fist_mastery && !_dual_mastery && !_pole_mastery && !_sword_mastery && !_2hands_mastery)
		{ // not completed 1st class transfer or not acquired yet the mastery skills
			newMasteryPenalty = 0;
		}
		else
		{
			for (L2ItemInstance item : getInventory().getItems())
			{
				if (item != null && item.isEquipped() && item.getItem() instanceof L2Weapon && !isCursedWeaponEquiped())
				{
					// No penality for cupid's bow
					if (item.isCupidBow())
					{
						continue;
					}
					
					L2Weapon weap_item = (L2Weapon) item.getItem();
					
					switch (weap_item.getItemType())
					{
						
						case BIGBLUNT:
						case BIGSWORD:
						{
							if (!_2hands_mastery)
							{
								newMasteryPenalty++;
							}
						}
							break;
						case BLUNT:
						{
							if (!_blunt_mastery)
							{
								newMasteryPenalty++;
							}
						}
							break;
						case BOW:
						{
							if (!_bow_mastery)
							{
								newMasteryPenalty++;
							}
						}
							break;
						case DAGGER:
						{
							if (!_dagger_mastery)
							{
								newMasteryPenalty++;
							}
						}
							break;
						case DUAL:
						{
							if (!_dual_mastery)
							{
								newMasteryPenalty++;
							}
						}
							break;
						case DUALFIST:
						case FIST:
						{
							if (!_fist_mastery)
							{
								newMasteryPenalty++;
							}
						}
							break;
						case POLE:
						{
							if (!_pole_mastery)
							{
								newMasteryPenalty++;
							}
						}
							break;
						case SWORD:
						{
							if (!_sword_mastery)
							{
								newMasteryPenalty++;
							}
						}
							break;
						default:
							break;
						
					}
				}
			}
			
		}
		
		if (_masteryWeapPenalty != newMasteryPenalty)
		{
			int penalties = _masteryPenalty + _expertisePenalty + newMasteryPenalty;
			
			if (penalties > 0)
			{
				super.addSkill(SkillTable.getInstance().getInfo(4267, 1)); // level used to be newPenalty
			}
			else
			{
				super.removeSkill(getKnownSkill(4267));
			}
			
			sendPacket(new EtcStatusUpdate(this));
			_masteryWeapPenalty = newMasteryPenalty;
		}
	}
	
	/**
	 * Refresh expertise penalty.
	 */
	public void refreshExpertisePenalty()
	{
		if (!Config.EXPERTISE_PENALTY || this.isPhantom())
		{
			return;
		}
		
		// This code works on principle that first 1-5 levels of penalty is for weapon and 6-10levels are for armor
		int intensityW = 0; // Default value
		int intensityA = 5; // Default value.
		int intensity = 0; // Level of grade penalty.
		
		for (final L2ItemInstance item : getInventory().getItems())
		{
			if (item != null && item.isEquipped()) // Checks if items equipped
			{
				
				final int crystaltype = item.getItem().getCrystalType(); // Gets grade of item
				// Checks if item crystal levels is above character levels and also if last penalty for weapon was lower.
				if (crystaltype > getExpertiseIndex() && item.isWeapon() && crystaltype > intensityW)
				{
					intensityW = crystaltype - getExpertiseIndex();
				}
				// Checks if equiped armor, accesories are above character level and adds each armor penalty.
				if (crystaltype > getExpertiseIndex() && !item.isWeapon())
				{
					intensityA += crystaltype - getExpertiseIndex();
				}
			}
		}
		
		if (intensityA == 5)// Means that there isn't armor penalty.
		{
			intensity = intensityW;
		}
		
		else
		{
			intensity = intensityW + intensityA;
		}
		
		// Checks if penalty is above maximum and sets it to maximum.
		if (intensity > 10)
		{
			intensity = 10;
		}
		
		if (getExpertisePenalty() != intensity)
		{
			int penalties = _masteryPenalty + _masteryWeapPenalty + intensity;
			if (penalties > 10) // Checks if penalties are out of bounds for skill level on XML
			{
				penalties = 10;
			}
			
			_expertisePenalty = intensity;
			
			if (penalties > 0)
			{
				super.addSkill(SkillTable.getInstance().getInfo(4267, intensity));
				sendSkillList();
			}
			else
			{
				super.removeSkill(getKnownSkill(4267));
				sendSkillList();
				_expertisePenalty = 0;
			}
		}
	}
	
	public void checkIfWeaponIsAllowed()
	{
		// Override for Gamemasters
		if (isGM())
		{
			return;
		}
		// Iterate through all effects currently on the character.
		for (L2Effect currenteffect : getAllEffects())
		{
			L2Skill effectSkill = currenteffect.getSkill();
			// Ignore all buff skills that are party related (ie. songs, dances) while still remaining weapon dependant on cast though.
			if (!effectSkill.isOffensive() && !(effectSkill.getTargetType() == SkillTargetType.TARGET_PARTY && effectSkill.getSkillType() == SkillType.BUFF))
			{
				// Check to rest to assure current effect meets weapon requirements.
				if (!effectSkill.getWeaponDependancy(this))
				{
					sendMessage(effectSkill.getName() + " cannot be used with this weapon.");
					if (Config.DEBUG)
					{
						LOG.info("   | Skill " + effectSkill.getName() + " has been disabled for (" + getName() + "); Reason: Incompatible Weapon Type.");
					}
					currenteffect.exit();
				}
			}
			continue;
		}
	}
	
	/**
	 * Check ss match.
	 * @param equipped the equipped
	 * @param unequipped the unequipped
	 */
	public void checkSSMatch(L2ItemInstance equipped, L2ItemInstance unequipped)
	{
		if (unequipped == null)
		{
			return;
		}
		
		if (unequipped.getItem().getType2() == L2Item.TYPE2_WEAPON && (equipped == null ? true : equipped.getItem().getCrystalType() != unequipped.getItem().getCrystalType()))
		{
			for (L2ItemInstance ss : getInventory().getItems())
			{
				int _itemId = ss.getItemId();
				if ((_itemId >= 2509 && _itemId <= 2514 || _itemId >= 3947 && _itemId <= 3952 || _itemId <= 1804 && _itemId >= 1808 || _itemId == 5789 || _itemId == 5790 || _itemId == 1835 || _itemId >= 10000 && _itemId <= 100011)
					&& ss.getItem().getCrystalType() == unequipped.getItem().getCrystalType())
				{
					sendPacket(new ExAutoSoulShot(_itemId, 0));
					
					SystemMessage sm = new SystemMessage(SystemMessageId.AUTO_USE_OF_S1_CANCELLED);
					sm.addString(ss.getItemName());
					sendPacket(sm);
				}
			}
		}
	}
	
	/**
	 * Return the the PvP Kills of the L2PcInstance (Number of player killed during a PvP).<BR>
	 * <BR>
	 * @return the pvp kills
	 */
	public int getPvpKills()
	{
		return _pvpKills;
	}
	
	/**
	 * Set the the PvP Kills of the L2PcInstance (Number of player killed during a PvP).<BR>
	 * <BR>
	 * @param pvpKills the new pvp kills
	 */
	public void setPvpKills(int pvpKills)
	{
		_pvpKills = pvpKills;
		
		/*
		 * // Set hero aura if pvp kills > 100 if (pvpKills > 100) { isPermaHero = true; setHeroAura(true); }
		 */
	}
	
	/**
	 * Set the template of the L2PcInstance.<BR>
	 * <BR>
	 * @param Id The Identifier of the L2PcTemplate to set to the L2PcInstance
	 */
	public void setClassId(int Id)
	{
		
		if (getLvlJoinedAcademy() != 0 && _clan != null && PlayerClass.values()[Id].getLevel() == ClassLevel.Third)
		{
			if (getLvlJoinedAcademy() <= 16)
			{
				_clan.setReputationScore(_clan.getReputationScore() + 400, true);
			}
			else if (getLvlJoinedAcademy() >= 39)
			{
				_clan.setReputationScore(_clan.getReputationScore() + 170, true);
			}
			else
			{
				_clan.setReputationScore(_clan.getReputationScore() + 400 - (getLvlJoinedAcademy() - 16) * 10, true);
			}
			
			_clan.broadcastToOnlineMembers(new PledgeShowInfoUpdate(_clan));
			setLvlJoinedAcademy(0);
			// oust pledge member from the academy, cuz he has finished his 2nd class transfer
			SystemMessage msg = new SystemMessage(SystemMessageId.CLAN_MEMBER_S1_EXPELLED);
			msg.addString(getName());
			_clan.broadcastToOnlineMembers(msg);
			_clan.broadcastToOnlineMembers(new PledgeShowMemberListDelete(getName()));
			_clan.removeClanMember(getName(), 0);
			sendPacket(new SystemMessage(SystemMessageId.ACADEMY_MEMBERSHIP_TERMINATED));
			
			// receive graduation gift
			getInventory().addItem("Gift", 8181, 1, this, null); // give academy circlet
			getAchievement().increase(AchType.ACADEMY);
			getInventory().updateDatabase(); // update database
		}
		if (isSubClassActive())
		{
			getSubClasses().get(_classIndex).setClassId(Id);
		}
		doCast(SkillTable.getInstance().getInfo(5103, 1));
		setClassTemplate(Id);
	}
	
	/**
	 * Return the Experience of the L2PcInstance.
	 * @return the exp
	 */
	public long getExp()
	{
		return getStat().getExp();
	}
	
	/**
	 * Sets the active enchant item.
	 * @param scroll the new active enchant item
	 */
	public void setActiveEnchantItem(L2ItemInstance scroll)
	{
		_activeEnchantItem = scroll;
	}
	
	/**
	 * Gets the active enchant item.
	 * @return the active enchant item
	 */
	public L2ItemInstance getActiveEnchantItem()
	{
		return _activeEnchantItem;
	}
	
	/**
	 * Set the fists weapon of the L2PcInstance (used when no weapon is equiped).<BR>
	 * <BR>
	 * @param weaponItem The fists L2Weapon to set to the L2PcInstance
	 */
	public void setFistsWeaponItem(L2Weapon weaponItem)
	{
		_fistsWeaponItem = weaponItem;
	}
	
	/**
	 * Return the fists weapon of the L2PcInstance (used when no weapon is equiped).<BR>
	 * <BR>
	 * @return the fists weapon item
	 */
	public L2Weapon getFistsWeaponItem()
	{
		return _fistsWeaponItem;
	}
	
	/**
	 * Return the fists weapon of the L2PcInstance Class (used when no weapon is equiped).<BR>
	 * <BR>
	 * @param classId the class id
	 * @return the l2 weapon
	 */
	public L2Weapon findFistsWeaponItem(int classId)
	{
		L2Weapon weaponItem = null;
		if (classId >= 0x00 && classId <= 0x09)
		{
			// human fighter fists
			L2Item temp = ItemTable.getInstance().getTemplate(246);
			weaponItem = (L2Weapon) temp;
			temp = null;
		}
		else if (classId >= 0x0a && classId <= 0x11)
		{
			// human mage fists
			L2Item temp = ItemTable.getInstance().getTemplate(251);
			weaponItem = (L2Weapon) temp;
			temp = null;
		}
		else if (classId >= 0x12 && classId <= 0x18)
		{
			// elven fighter fists
			L2Item temp = ItemTable.getInstance().getTemplate(244);
			weaponItem = (L2Weapon) temp;
			temp = null;
		}
		else if (classId >= 0x19 && classId <= 0x1e)
		{
			// elven mage fists
			L2Item temp = ItemTable.getInstance().getTemplate(249);
			weaponItem = (L2Weapon) temp;
			temp = null;
		}
		else if (classId >= 0x1f && classId <= 0x25)
		{
			// dark elven fighter fists
			L2Item temp = ItemTable.getInstance().getTemplate(245);
			weaponItem = (L2Weapon) temp;
			temp = null;
		}
		else if (classId >= 0x26 && classId <= 0x2b)
		{
			// dark elven mage fists
			L2Item temp = ItemTable.getInstance().getTemplate(250);
			weaponItem = (L2Weapon) temp;
			temp = null;
		}
		else if (classId >= 0x2c && classId <= 0x30)
		{
			// orc fighter fists
			L2Item temp = ItemTable.getInstance().getTemplate(248);
			weaponItem = (L2Weapon) temp;
			temp = null;
		}
		else if (classId >= 0x31 && classId <= 0x34)
		{
			// orc mage fists
			L2Item temp = ItemTable.getInstance().getTemplate(252);
			weaponItem = (L2Weapon) temp;
			temp = null;
		}
		else if (classId >= 0x35 && classId <= 0x39)
		{
			// dwarven fists
			L2Item temp = ItemTable.getInstance().getTemplate(247);
			weaponItem = (L2Weapon) temp;
			temp = null;
		}
		
		return weaponItem;
	}
	
	/**
	 * Give Expertise skill of this level and remove beginner Lucky skill.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Get the Level of the L2PcInstance</li>
	 * <li>If L2PcInstance Level is 5, remove beginner Lucky skill</li>
	 * <li>Add the Expertise skill corresponding to its Expertise level</li>
	 * <li>Update the overloaded status of the L2PcInstance</li><BR>
	 * <BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method DOESN'T give other free skills (SP needed = 0)</B></FONT><BR>
	 * <BR>
	 */
	public synchronized void rewardSkills()
	{
		rewardSkills(false);
	}
	
	public synchronized void rewardSkills(final boolean restore)
	{
		// Get the Level of the L2PcInstance
		int lvl = getLevel();
		
		// Remove beginner Lucky skill
		if (lvl >= 10)
		{
			L2Skill skill = SkillTable.getInstance().getInfo(194, 1);
			skill = removeSkill(skill);
		}
		
		// Calculate the current higher Expertise of the L2PcInstance
		for (int i = 0; i < EXPERTISE_LEVELS.length; i++)
		{
			if (lvl >= EXPERTISE_LEVELS[i])
			{
				setExpertiseIndex(i);
			}
		}
		
		// Add the Expertise skill corresponding to its Expertise level
		if (getExpertiseIndex() > 0)
		{
			L2Skill skill = SkillTable.getInstance().getInfo(239, getExpertiseIndex());
			addSkill(skill, !restore);
		}
		else
		{
			if (Config.DEBUG)
			{
				LOG.warn("No skills awarded at lvl: " + lvl);
			}
		}
		
		// Active skill dwarven craft
		if (getSkillLevel(1321) < 1 && getRace() == Race.dwarf)
		{
			L2Skill skill = SkillTable.getInstance().getInfo(1321, 1);
			addSkill(skill, !restore);
		}
		
		// Active skill common craft
		if (getSkillLevel(1322) < 1)
		{
			L2Skill skill = SkillTable.getInstance().getInfo(1322, 1);
			addSkill(skill, !restore);
		}
		
		for (int i = 0; i < COMMON_CRAFT_LEVELS.length; i++)
		{
			if (lvl >= COMMON_CRAFT_LEVELS[i] && getSkillLevel(1320) < i + 1)
			{
				L2Skill skill = SkillTable.getInstance().getInfo(1320, (i + 1));
				addSkill(skill, !restore);
			}
		}
		
		// Auto-Learn skills if activated
		if (Config.AUTO_LEARN_SKILLS && getLevel() <= Config.AUTO_LEARN_SKILLS_LVL)
		{
			giveAvailableSkills();
		}
		
		sendSkillList();
		
		if (_clan != null)
		{
			if (_clan.getLevel() > 3 && isClanLeader())
			{
				SiegeManager.getInstance().addSiegeSkills(this);
			}
		}
		
		// This function gets called on login, so not such a bad place to check weight
		refreshOverloaded(); // Update the overloaded status of the L2PcInstance
		refreshExpertisePenalty(); // Update the expertise status of the L2PcInstance
		refreshMasteryPenality();
		refreshMasteryWeapPenality();
	}
	
	/**
	 * Regive all skills which aren't saved to database, like Noble, Hero, Clan Skills<BR>
	 * <BR>
	 * .
	 */
	private synchronized void regiveTemporarySkills()
	{
		// Do not call this on enterworld or char load
		
		// Add noble skills if noble
		if (isNoble())
		{
			setNoble(true);
		}
		
		// Add Hero skills if hero
		if (isHero())
		{
			setHero(true);
		}
		
		// Add clan skills
		if (getClan() != null && getClan().getReputationScore() >= 0)
		{
			L2Skill[] skills = getClan().getAllSkills();
			for (L2Skill sk : skills)
			{
				if (sk.getMinPledgeClass() <= getPledgeClass())
				{
					addSkill(sk, false);
				}
			}
			skills = null;
		}
		
		// Reload passive skills from armors / jewels / weapons
		getInventory().reloadEquippedItems();
		
	}
	
	/**
	 * Give all available skills to the player.<br>
	 * <br>
	 */
	public void giveAvailableSkills()
	{
		int skillCounter = 0;
		
		Collection<L2Skill> skills = SkillTreeTable.getInstance().getAllAvailableSkills(this, getClassId());
		for (L2Skill sk : skills)
		{
			if (getSkillLevel(sk.getId()) == -1)
			{
				skillCounter++;
			}
			
			// Penality skill are not auto learn
			if (sk.getId() == 4267 || sk.getId() == 4270)
			{
				continue;
			}
			
			addSkill(sk, true);
		}
		
		sendMessage("You have learned " + skillCounter + " new skills.");
	}
	
	/**
	 * Set the Experience value of the L2PcInstance.
	 * @param exp the new exp
	 */
	public void setExp(long exp)
	{
		getStat().setExp(exp);
	}
	
	/**
	 * Return the Race object of the L2PcInstance.<BR>
	 * <BR>
	 * @return the race
	 */
	public Race getRace()
	{
		if (!isSubClassActive())
		{
			return getTemplate().race;
		}
		
		L2PcTemplate charTemp = CharTemplateTable.getInstance().getTemplate(_baseClass);
		return charTemp.race;
	}
	
	/**
	 * Gets the radar.
	 * @return the radar
	 */
	public L2Radar getRadar()
	{
		return _radar;
	}
	
	/**
	 * Return the SP amount of the L2PcInstance.
	 * @return the sp
	 */
	public int getSp()
	{
		return getStat().getSp();
	}
	
	/**
	 * Set the SP amount of the L2PcInstance.
	 * @param sp the new sp
	 */
	public void setSp(int sp)
	{
		super.getStat().setSp(sp);
	}
	
	/**
	 * Return true if this L2PcInstance is a clan leader in ownership of the passed castle.
	 * @param castleId the castle id
	 * @return true, if is castle lord
	 */
	public boolean isCastleLord(int castleId)
	{
		L2Clan clan = getClan();
		
		// player has clan and is the clan leader, check the castle info
		if (clan != null && clan.getLeader().getPlayerInstance() == this)
		{
			// if the clan has a castle and it is actually the queried castle, return true
			Castle castle = CastleManager.getInstance().getCastleByOwner(clan);
			if (castle != null && castle == CastleManager.getInstance().getCastleById(castleId))
			{
				castle = null;
				return true;
			}
			castle = null;
		}
		clan = null;
		return false;
	}
	
	/**
	 * Return the Clan Identifier of the L2PcInstance.<BR>
	 * <BR>
	 * @return the clan id
	 */
	public int getClanId()
	{
		return _clanId;
	}
	
	/**
	 * Return the Clan Crest Identifier of the L2PcInstance or 0.<BR>
	 * <BR>
	 * @return the clan crest id
	 */
	public int getClanCrestId()
	{
		if (_clan != null && _clan.hasCrest())
		{
			return _clan.getCrestId();
		}
		
		return 0;
	}
	
	/**
	 * Gets the clan crest large id.
	 * @return The Clan CrestLarge Identifier or 0
	 */
	public int getClanCrestLargeId()
	{
		if (_clan != null && _clan.hasCrestLarge())
		{
			return _clan.getCrestLargeId();
		}
		
		return 0;
	}
	
	/**
	 * Gets the clan join expiry time.
	 * @return the clan join expiry time
	 */
	public long getClanJoinExpiryTime()
	{
		return _clanJoinExpiryTime;
	}
	
	/**
	 * Sets the clan join expiry time.
	 * @param time the new clan join expiry time
	 */
	public void setClanJoinExpiryTime(long time)
	{
		_clanJoinExpiryTime = time;
	}
	
	/**
	 * Gets the clan create expiry time.
	 * @return the clan create expiry time
	 */
	public long getClanCreateExpiryTime()
	{
		return _clanCreateExpiryTime;
	}
	
	/**
	 * Sets the clan create expiry time.
	 * @param time the new clan create expiry time
	 */
	public void setClanCreateExpiryTime(long time)
	{
		_clanCreateExpiryTime = time;
	}
	
	/**
	 * Sets the online time.
	 * @param time the new online time
	 */
	public void setOnlineTime(long time)
	{
		_onlineTime = time;
		_onlineBeginTime = System.currentTimeMillis();
	}
	
	public long getOnlineTime()
	{
		return _onlineTime;
	}
	
	/**
	 * Return the PcInventory Inventory of the L2PcInstance contained in _inventory.<BR>
	 * <BR>
	 * @return the inventory
	 */
	public PcInventory getInventory()
	{
		return _inventory;
	}
	
	/**
	 * Delete a ShortCut of the L2PcInstance _shortCuts.<BR>
	 * <BR>
	 * @param objectId the object id
	 */
	public void removeItemFromShortCut(int objectId)
	{
		_shortCuts.deleteShortCutByObjectId(objectId);
	}
	
	// MOVING on attack TASK, L2OFF FIX
	/** The launched moving task. */
	protected MoveOnAttack launchedMovingTask = null;
	
	/** The _moving task defined. */
	protected Boolean _movingTaskDefined = false;
	
	/**
	 * MoveOnAttack Task.
	 */
	public class MoveOnAttack implements Runnable
	{
		
		/** The _player. */
		final L2PcInstance _player;
		
		/** The _pos. */
		Location _pos;
		
		/**
		 * Instantiates a new move on attack.
		 * @param player the player
		 * @param pos the pos
		 */
		public MoveOnAttack(L2PcInstance player, Location pos)
		{
			_player = player;
			_pos = pos;
		}
		
		@Override
		public void run()
		{
			synchronized (_movingTaskDefined)
			{
				launchedMovingTask = null;
				_movingTaskDefined = false;
			}
			// Set the Intention of this AbstractAI to AI_INTENTION_MOVE_TO
			_player.getAI().changeIntention(AI_INTENTION_MOVE_TO, _pos, null);
			
			// Stop the actor auto-attack client side by sending Server->Client packet AutoAttackStop (broadcast)
			_player.getAI().clientStopAutoAttack();
			
			// Abort the attack of the L2Character and send Server->Client ActionFailed packet
			_player.abortAttack();
			
			// Move the actor to Location (x,y,z) server side AND client side by sending Server->Client packet CharMoveToLocation (broadcast)
			_player.getAI().moveTo(_pos.getX(), _pos.getY(), _pos.getZ());
		}
		
		/**
		 * Sets the new position.
		 * @param pos the new new position
		 */
		public void setNewPosition(Location pos)
		{
			_pos = pos;
		}
	}
	
	/**
	 * Checks if is moving task defined.
	 * @return true, if is moving task defined
	 */
	public boolean isMovingTaskDefined()
	{
		return _movingTaskDefined;
	}
	
	public final void setMovingTaskDefined(boolean value)
	{
		_movingTaskDefined = value;
	}
	
	/**
	 * Define new moving task.
	 * @param pos the pos
	 */
	public void defineNewMovingTask(Location pos)
	{
		synchronized (_movingTaskDefined)
		{
			launchedMovingTask = new MoveOnAttack(this, pos);
			_movingTaskDefined = true;
		}
	}
	
	/**
	 * Modify moving task.
	 * @param pos the pos
	 */
	public void modifyMovingTask(Location pos)
	{
		synchronized (_movingTaskDefined)
		{
			
			if (!_movingTaskDefined)
			{
				return;
			}
			
			launchedMovingTask.setNewPosition(pos);
		}
	}
	
	public void startMovingTask()
	{
		synchronized (_movingTaskDefined)
		{
			if (!_movingTaskDefined)
			{
				return;
			}
			
			if ((isMoving() && isAttackingNow()))
			{
				return;
			}
			
			ThreadPoolManager.getInstance().executeTask(launchedMovingTask);
		}
	}
	
	/**
	 * Return True if the L2PcInstance is sitting.<BR>
	 * <BR>
	 * @return true, if is sitting
	 */
	public boolean isSitting()
	{
		return _waitTypeSitting || sittingTaskLaunched;
	}
	
	/**
	 * Return True if the L2PcInstance is sitting task launched.<BR>
	 * <BR>
	 * @return true, if is sitting task launched
	 */
	public boolean isSittingTaskLaunched()
	{
		return sittingTaskLaunched;
	}
	
	/**
	 * Set _waitTypeSitting to given value.
	 * @param state the new checks if is sitting
	 */
	public void setIsSitting(boolean state)
	{
		_waitTypeSitting = state;
	}
	
	/**
	 * Sets the posticipate sit.
	 * @param act the new posticipate sit
	 */
	public void setPosticipateSit(boolean act)
	{
		_posticipateSit = act;
	}
	
	/**
	 * Gets the posticipate sit.
	 * @return the posticipate sit
	 */
	public boolean getPosticipateSit()
	{
		return _posticipateSit;
	}
	
	/**
	 * Sit down the L2PcInstance, set the AI Intention to AI_INTENTION_REST and send a Server->Client ChangeWaitType packet (broadcast)<BR>
	 * <BR>
	 * .
	 */
	public void sitDown()
	{
		if (isFakeDeath())
		{
			stopFakeDeath(null);
		}
		
		if (isMoving()) // since you are moving and want sit down
						// the posticipate sitdown task will be always true
		{
			setPosticipateSit(true);
			return;
		}
		
		// we are going to sitdown, so posticipate is false
		setPosticipateSit(false);
		
		if (isCastingNow() && !_relax)
		{
			return;
		}
		
		if (sittingTaskLaunched)
		{
			// just return
			return;
		}
		
		if (!_waitTypeSitting && !isAttackingDisabled() && !isOutOfControl() && !isImobilised())
		{
			breakAttack();
			setIsSitting(true);
			broadcastPacket(new ChangeWaitType(this, ChangeWaitType.WT_SITTING));
			sittingTaskLaunched = true;
			// Schedule a sit down task to wait for the animation to finish
			ThreadPoolManager.getInstance().scheduleGeneral(new SitDownTask(this), 2500);
			setIsParalyzed(true);
		}
	}
	
	/**
	 * Sit down Task.
	 */
	class SitDownTask implements Runnable
	{
		
		/** The _player. */
		L2PcInstance _player;
		
		/** The this$0. */
		final L2PcInstance this$0;
		
		/**
		 * Instantiates a new sit down task.
		 * @param player the player
		 */
		SitDownTask(L2PcInstance player)
		{
			this$0 = L2PcInstance.this;
			_player = player;
		}
		
		/*
		 * (non-Javadoc)
		 * @see java.lang.Runnable#run()
		 */
		@Override
		public void run()
		{
			setIsSitting(true);
			_player.setIsParalyzed(false);
			sittingTaskLaunched = false;
			_player.getAI().setIntention(CtrlIntention.AI_INTENTION_REST);
		}
	}
	
	/**
	 * Stand up Task.
	 */
	class StandUpTask implements Runnable
	{
		
		/** The _player. */
		L2PcInstance _player;
		
		/**
		 * Instantiates a new stand up task.
		 * @param player the player
		 */
		StandUpTask(L2PcInstance player)
		{
			_player = player;
		}
		
		/*
		 * (non-Javadoc)
		 * @see java.lang.Runnable#run()
		 */
		@Override
		public void run()
		{
			_player.setIsSitting(false);
			_player.setIsImobilised(false);
			_player.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
		}
	}
	
	/**
	 * Stand up the L2PcInstance, set the AI Intention to AI_INTENTION_IDLE and send a Server->Client ChangeWaitType packet (broadcast)<BR>
	 * <BR>
	 * .
	 */
	public void standUp()
	{
		if (isSellBuff())
		{
			return;
		}
		
		if (isFakeDeath())
		{
			broadcastPacket(new ChangeWaitType(this, ChangeWaitType.WT_STANDING));
			// Schedule a stand up task to wait for the animation to finish
			setIsImobilised(true);
			ThreadPoolManager.getInstance().scheduleGeneral(new StandUpTask(this), 2000);
			stopFakeDeath(null);
		}
		
		if (sittingTaskLaunched)
		{
			return;
		}
		
		if (L2Event.active && eventSitForced)
		{
			sendMessage("A dark force beyond your mortal understanding makes your knees to shake when you try to stand up ...");
		}
		else if ((TvT.is_sitForced() && _inEventTvT) || (CTF.is_sitForced() && _inEventCTF) || (DM.is_sitForced() && _inEventDM))
		{
			sendMessage("The Admin/GM handle if you sit or stand in this match!");
		}
		else if (VIP._sitForced && _inEventVIP)
		{
			sendMessage("The Admin/GM handle if you sit or stand in this match!");
		}
		else if (_waitTypeSitting && !isInStoreMode() && !isAlikeDead())
		{
			if (_relax)
			{
				setRelax(false);
				stopEffects(L2Effect.EffectType.RELAXING);
			}
			
			broadcastPacket(new ChangeWaitType(this, ChangeWaitType.WT_STANDING));
			// Schedule a stand up task to wait for the animation to finish
			setIsImobilised(true);
			ThreadPoolManager.getInstance().scheduleGeneral(new StandUpTask(this), 2500);
			
		}
	}
	
	/**
	 * Set the value of the _relax value. Must be True if using skill Relax and False if not.
	 * @param val the new relax
	 */
	public void setRelax(boolean val)
	{
		_relax = val;
	}
	
	/**
	 * Return the PcWarehouse object of the L2PcInstance.<BR>
	 * <BR>
	 * @return the warehouse
	 */
	public PcWarehouse getWarehouse()
	{
		if (_warehouse == null)
		{
			_warehouse = new PcWarehouse(this);
			_warehouse.restore();
		}
		if (Config.WAREHOUSE_CACHE)
		{
			WarehouseCacheManager.getInstance().addCacheTask(this);
		}
		return _warehouse;
	}
	
	/**
	 * Free memory used by Warehouse.
	 */
	public void clearWarehouse()
	{
		if (_warehouse != null)
		{
			_warehouse.deleteMe();
		}
		_warehouse = null;
	}
	
	/**
	 * Return the PcFreight object of the L2PcInstance.<BR>
	 * <BR>
	 * @return the freight
	 */
	public PcFreight getFreight()
	{
		return _freight;
	}
	
	/**
	 * Return the Identifier of the L2PcInstance.<BR>
	 * <BR>
	 * @return the char id
	 */
	public int getCharId()
	{
		return _charId;
	}
	
	/**
	 * Set the Identifier of the L2PcInstance.<BR>
	 * <BR>
	 * @param charId the new char id
	 */
	public void setCharId(int charId)
	{
		_charId = charId;
	}
	
	/**
	 * Return the Adena amount of the L2PcInstance.<BR>
	 * <BR>
	 * @return the adena
	 */
	public int getAdena()
	{
		return _inventory.getAdena();
	}
	
	/**
	 * Return the Item amount of the L2PcInstance.<BR>
	 * <BR>
	 * @param itemId the item id
	 * @param enchantLevel the enchant level
	 * @return the item count
	 */
	public int getItemCount(int itemId, int enchantLevel)
	{
		return _inventory.getInventoryItemCount(itemId, enchantLevel);
	}
	
	public long getItemCount(int itemId)
	{
		return _inventory.getInventoryItemCount(itemId);
	}
	
	/**
	 * Return the Ancient Adena amount of the L2PcInstance.<BR>
	 * <BR>
	 * @return the ancient adena
	 */
	public int getAncientAdena()
	{
		return _inventory.getAncientAdena();
	}
	
	/**
	 * Add adena to Inventory of the L2PcInstance and send a Server->Client InventoryUpdate packet to the L2PcInstance.
	 * @param process : String Identifier of process triggering this action
	 * @param count : int Quantity of adena to be added
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage : boolean Specifies whether to send message to Client about this action
	 */
	public void addAdena(String process, int count, L2Object reference, boolean sendMessage)
	{
		if (count > 0)
		{
			if (_inventory.getAdena() == Integer.MAX_VALUE)
			{
				return;
			}
			else if (_inventory.getAdena() >= Integer.MAX_VALUE - count)
			{
				count = Integer.MAX_VALUE - _inventory.getAdena();
				_inventory.addAdena(process, count, this, reference);
			}
			else if (_inventory.getAdena() < Integer.MAX_VALUE - count)
			{
				_inventory.addAdena(process, count, this, reference);
			}
			if (sendMessage)
			{
				SystemMessage sm = new SystemMessage(SystemMessageId.EARNED_ADENA);
				sm.addNumber(count);
				sendPacket(sm);
			}
			
			// Send update packet
			if (!Config.FORCE_INVENTORY_UPDATE)
			{
				InventoryUpdate iu = new InventoryUpdate();
				iu.addItem(_inventory.getAdenaInstance());
				sendPacket(iu);
				iu = null;
			}
			else
			{
				sendPacket(new ItemList(this, false));
			}
		}
	}
	
	public void addMedal(String process, int count, L2Object reference, boolean sendMessage)
	{
		if (count > 0)
		{
			_inventory.addMedal(process, count, this, reference);
			if (sendMessage)
			{
				SystemMessage smsg;
				smsg = new SystemMessage(SystemMessageId.EARNED_S2_S1_S); // earned $s2$s1
				smsg.addItemName(6393);
				smsg.addNumber(count);
				sendPacket(smsg);
			}
			
			// Send update packet
			if (!Config.FORCE_INVENTORY_UPDATE)
			{
				InventoryUpdate iu = new InventoryUpdate();
				sendPacket(iu);
			}
			else
			{
				sendPacket(new ItemList(this, false));
			}
		}
	}
	
	/**
	 * Reduce adena in Inventory of the L2PcInstance and send a Server->Client InventoryUpdate packet to the L2PcInstance.
	 * @param process : String Identifier of process triggering this action
	 * @param count : int Quantity of adena to be reduced
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage : boolean Specifies whether to send message to Client about this action
	 * @return boolean informing if the action was successfull
	 */
	public boolean reduceAdena(String process, int count, L2Object reference, boolean sendMessage)
	{
		if (count > _inventory.getAdena())
		{
			if (sendMessage)
			{
				sendPacket(new SystemMessage(SystemMessageId.YOU_NOT_ENOUGH_ADENA));
			}
			return false;
		}
		
		if (count > 0)
		{
			L2ItemInstance adenaItem = _inventory.getAdenaInstance();
			_inventory.reduceAdena(process, count, this, reference);
			
			// Send update packet
			if (!Config.FORCE_INVENTORY_UPDATE)
			{
				InventoryUpdate iu = new InventoryUpdate();
				iu.addItem(adenaItem);
				sendPacket(iu);
			}
			else
			{
				sendPacket(new ItemList(this, false));
			}
			
			if (sendMessage)
			{
				SystemMessage sm = new SystemMessage(SystemMessageId.DISSAPEARED_ADENA);
				sm.addNumber(count);
				sendPacket(sm);
			}
		}
		return true;
	}
	
	/**
	 * Add ancient adena to Inventory of the L2PcInstance and send a Server->Client InventoryUpdate packet to the L2PcInstance.
	 * @param process : String Identifier of process triggering this action
	 * @param count : int Quantity of ancient adena to be added
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage : boolean Specifies whether to send message to Client about this action
	 */
	public void addAncientAdena(String process, int count, L2Object reference, boolean sendMessage)
	{
		if (sendMessage)
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.EARNED_S2_S1_S);
			sm.addItemName(PcInventory.ANCIENT_ADENA_ID);
			sm.addNumber(count);
			sendPacket(sm);
			sm = null;
		}
		
		if (count > 0)
		{
			_inventory.addAncientAdena(process, count, this, reference);
			
			if (!Config.FORCE_INVENTORY_UPDATE)
			{
				InventoryUpdate iu = new InventoryUpdate();
				iu.addItem(_inventory.getAncientAdenaInstance());
				sendPacket(iu);
				iu = null;
			}
			else
			{
				sendPacket(new ItemList(this, false));
			}
		}
	}
	
	/**
	 * Reduce ancient adena in Inventory of the L2PcInstance and send a Server->Client InventoryUpdate packet to the L2PcInstance.
	 * @param process : String Identifier of process triggering this action
	 * @param count : int Quantity of ancient adena to be reduced
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage : boolean Specifies whether to send message to Client about this action
	 * @return boolean informing if the action was successfull
	 */
	public boolean reduceAncientAdena(String process, int count, L2Object reference, boolean sendMessage)
	{
		if (count > getAncientAdena())
		{
			if (sendMessage)
			{
				sendPacket(new SystemMessage(SystemMessageId.YOU_NOT_ENOUGH_ADENA));
			}
			
			return false;
		}
		
		if (count > 0)
		{
			L2ItemInstance ancientAdenaItem = _inventory.getAncientAdenaInstance();
			_inventory.reduceAncientAdena(process, count, this, reference);
			
			if (!Config.FORCE_INVENTORY_UPDATE)
			{
				InventoryUpdate iu = new InventoryUpdate();
				iu.addItem(ancientAdenaItem);
				sendPacket(iu);
				iu = null;
			}
			else
			{
				sendPacket(new ItemList(this, false));
			}
			
			if (sendMessage)
			{
				SystemMessage sm = new SystemMessage(SystemMessageId.DISSAPEARED_ITEM);
				sm.addNumber(count);
				sm.addItemName(PcInventory.ANCIENT_ADENA_ID);
				sendPacket(sm);
				sm = null;
			}
			ancientAdenaItem = null;
		}
		
		return true;
	}
	
	/**
	 * Adds item to inventory and send a Server->Client InventoryUpdate packet to the L2PcInstance.
	 * @param process : String Identifier of process triggering this action
	 * @param item : L2ItemInstance to be added
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage : boolean Specifies whether to send message to Client about this action
	 */
	public void addItem(String process, L2ItemInstance item, L2Object reference, boolean sendMessage)
	{
		if (item.getCount() > 0)
		{
			// Sends message to client if requested
			if (sendMessage)
			{
				if (item.getCount() > 1)
				{
					if (item.isStackable() && !Config.MULTIPLE_ITEM_DROP)
					{
						SystemMessage sm = new SystemMessage(SystemMessageId.YOU_PICKED_UP_S1);
						sm.addItemName(item.getItemId());
						sendPacket(sm);
						sm = null;
					}
					else
					{
						SystemMessage sm = new SystemMessage(SystemMessageId.YOU_PICKED_UP_S1_S2);
						sm.addItemName(item.getItemId());
						sm.addNumber(item.getCount());
						sendPacket(sm);
						sm = null;
					}
					
				}
				else if (item.getEnchantLevel() > 0)
				{
					SystemMessage sm = new SystemMessage(SystemMessageId.YOU_PICKED_UP_A_S1_S2);
					sm.addNumber(item.getEnchantLevel());
					sm.addItemName(item.getItemId());
					sendPacket(sm);
					sm = null;
				}
				else
				{
					SystemMessage sm = new SystemMessage(SystemMessageId.YOU_PICKED_UP_S1);
					sm.addItemName(item.getItemId());
					sendPacket(sm);
					sm = null;
				}
			}
			
			// Add the item to inventory
			L2ItemInstance newitem = _inventory.addItem(process, item, this, reference);
			
			// Send inventory update packet
			if (!Config.FORCE_INVENTORY_UPDATE)
			{
				InventoryUpdate playerIU = new InventoryUpdate();
				playerIU.addItem(newitem);
				sendPacket(playerIU);
			}
			else
			{
				sendPacket(new ItemList(this, false));
			}
			
			// Update current load as well
			StatusUpdate su = new StatusUpdate(getObjectId());
			su.addAttribute(StatusUpdate.CUR_LOAD, getCurrentLoad());
			sendPacket(su);
			
			// If over capacity, Drop the item
			if (!isGM() && !_inventory.validateCapacity(0, item.isQuestItem()) && newitem.isDropable() && (!newitem.isStackable() || (newitem.getLastChange() != L2ItemInstance.MODIFIED)))
			{
				dropItem("InvDrop", newitem, null, true, true);
			}
			else if (CursedWeaponsManager.getInstance().isCursed(newitem.getItemId()))
			{
				CursedWeaponsManager.getInstance().activate(this, newitem);
			}
		}
		
		// If you pickup arrows.
		if (item.getItem().getItemType() == L2EtcItemType.ARROW)
		{
			// If a bow is equipped, try to equip them if no arrows is currently equipped.
			L2Weapon currentWeapon = getActiveWeaponItem();
			if (currentWeapon != null && currentWeapon.getItemType() == L2WeaponType.BOW && getInventory().getPaperdollItem(Inventory.PAPERDOLL_LHAND) == null)
			{
				checkAndEquipArrows();
			}
		}
	}
	
	/**
	 * Adds item to Inventory and send a Server->Client InventoryUpdate packet to the L2PcInstance.
	 * @param process : String Identifier of process triggering this action
	 * @param itemId : int Item Identifier of the item to be added
	 * @param count : int Quantity of items to be added
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage : boolean Specifies whether to send message to Client about this action
	 * @return
	 */
	public L2ItemInstance addItem(String process, int itemId, int count, L2Object reference, boolean sendMessage)
	{
		if (count > 0)
		{
			L2ItemInstance item = null;
			
			if (ItemTable.getInstance().getTemplate(itemId) != null)
			{
				item = ItemTable.getInstance().createDummyItem(itemId);
			}
			else
			{
				LOG.error("Item doesn't exist so cannot be added. Item ID: " + itemId);
				return null;
			}
			
			// Sends message to client if requested
			if (sendMessage && (!isCastingNow() && ItemTable.getInstance().createDummyItem(itemId).getItemType() == L2EtcItemType.HERB || ItemTable.getInstance().createDummyItem(itemId).getItemType() != L2EtcItemType.HERB))
			{
				if (count > 1)
				{
					if (process.equalsIgnoreCase("sweep") || process.equalsIgnoreCase("Quest"))
					{
						SystemMessage sm = new SystemMessage(SystemMessageId.EARNED_S2_S1_S);
						sm.addItemName(itemId);
						sm.addNumber(count);
						sendPacket(sm);
					}
					else
					{
						SystemMessage sm = new SystemMessage(SystemMessageId.YOU_PICKED_UP_S1_S2);
						sm.addItemName(itemId);
						sm.addNumber(count);
						sendPacket(sm);
					}
				}
				else
				{
					if (process.equalsIgnoreCase("sweep") || process.equalsIgnoreCase("Quest"))
					{
						SystemMessage sm = new SystemMessage(SystemMessageId.EARNED_ITEM);
						sm.addItemName(itemId);
						sendPacket(sm);
					}
					else
					{
						SystemMessage sm = new SystemMessage(SystemMessageId.YOU_PICKED_UP_S1);
						sm.addItemName(itemId);
						sendPacket(sm);
					}
				}
			}
			// Auto use herbs - autoloot
			if (item.getItemType() == L2EtcItemType.HERB) // If item is herb dont add it to inv
			{
				if (!isCastingNow() && !isCastingPotionNow())
				{
					L2ItemInstance herb = new L2ItemInstance(_charId, itemId);
					IItemHandler handler = ItemHandler.getInstance().getItemHandler(herb.getItemId());
					
					if (handler == null)
					{
						LOG.warn("No item handler registered for Herb - item ID " + herb.getItemId() + ".");
					}
					else
					{
						handler.useItem(this, herb);
						
						if (_herbstask >= 100)
						{
							_herbstask -= 100;
						}
					}
				}
				else
				{
					_herbstask += 100;
					ThreadPoolManager.getInstance().scheduleAi(new HerbTask(process, itemId, count, reference, sendMessage), _herbstask);
				}
			}
			else
			{
				// Add the item to inventory
				L2ItemInstance createdItem = _inventory.addItem(process, itemId, count, this, reference);
				
				// Send inventory update packet
				if (!Config.FORCE_INVENTORY_UPDATE)
				{
					InventoryUpdate playerIU = new InventoryUpdate();
					playerIU.addItem(createdItem);
					sendPacket(playerIU);
				}
				else
				{
					sendPacket(new ItemList(this, false));
				}
				
				// Update current load as well
				StatusUpdate su = new StatusUpdate(getObjectId());
				su.addAttribute(StatusUpdate.CUR_LOAD, getCurrentLoad());
				sendPacket(su);
				
				// If over capacity, drop the item
				if (!isGM() && !_inventory.validateCapacity(0, item.isQuestItem()) && createdItem.isDropable() && (!createdItem.isStackable() || (createdItem.getLastChange() != L2ItemInstance.MODIFIED)))
				{
					dropItem("InvDrop", createdItem, null, true, true);
				}
				if (CursedWeaponsManager.getInstance().isCursed(createdItem.getItemId()))
				{
					CursedWeaponsManager.getInstance().activate(this, createdItem);
				}
				
				return createdItem;
			}
		}
		return null;
	}
	
	public L2ItemInstance addItem(String process, int itemId, int count, int enchantLevel, L2Object reference, boolean sendMessage)
	{
		if (count > 0)
		{
			L2ItemInstance item = null;
			
			if (ItemTable.getInstance().getTemplate(itemId) != null)
			{
				item = ItemTable.getInstance().createDummyItem(itemId);
			}
			else
			{
				LOG.error("Item doesn't exist so cannot be added. Item ID: " + itemId);
				return null;
			}
			
			// Sends message to client if requested
			if (sendMessage && (!isCastingNow() && ItemTable.getInstance().createDummyItem(itemId).getItemType() == L2EtcItemType.HERB || ItemTable.getInstance().createDummyItem(itemId).getItemType() != L2EtcItemType.HERB))
			{
				if (count > 1)
				{
					if (process.equalsIgnoreCase("sweep") || process.equalsIgnoreCase("Quest"))
					{
						SystemMessage sm = new SystemMessage(SystemMessageId.EARNED_S2_S1_S);
						sm.addItemName(itemId);
						sm.addNumber(count);
						sendPacket(sm);
					}
					else
					{
						SystemMessage sm = new SystemMessage(SystemMessageId.YOU_PICKED_UP_S1_S2);
						sm.addItemName(itemId);
						sm.addNumber(count);
						sendPacket(sm);
					}
				}
				else
				{
					if (process.equalsIgnoreCase("sweep") || process.equalsIgnoreCase("Quest"))
					{
						SystemMessage sm = new SystemMessage(SystemMessageId.EARNED_ITEM);
						sm.addItemName(itemId);
						sendPacket(sm);
					}
					else
					{
						SystemMessage sm = new SystemMessage(SystemMessageId.YOU_PICKED_UP_S1);
						sm.addItemName(itemId);
						sendPacket(sm);
					}
				}
			}
			// Auto use herbs - autoloot
			if (item.getItemType() == L2EtcItemType.HERB) // If item is herb dont add it to iv :]
			{
				if (!isCastingNow() && !isCastingPotionNow())
				{
					L2ItemInstance herb = new L2ItemInstance(_charId, itemId);
					IItemHandler handler = ItemHandler.getInstance().getItemHandler(herb.getItemId());
					
					if (handler == null)
					{
						LOG.warn("No item handler registered for Herb - item ID " + herb.getItemId() + ".");
					}
					else
					{
						handler.useItem(this, herb);
						
						if (_herbstask >= 100)
						{
							_herbstask -= 100;
						}
					}
				}
				else
				{
					_herbstask += 100;
					ThreadPoolManager.getInstance().scheduleAi(new HerbTask(process, itemId, count, reference, sendMessage), _herbstask);
				}
			}
			else
			{
				// Add the item to inventory
				L2ItemInstance createdItem = _inventory.addItem(process, itemId, count, this, reference);
				
				if (enchantLevel > 0)
				{
					if (createdItem.getItem().getType2() == L2Item.TYPE2_WEAPON || createdItem.getItem().getType2() == L2Item.TYPE2_SHIELD_ARMOR || createdItem.getItem().getType2() == L2Item.TYPE2_ACCESSORY)
					{
						createdItem.setEnchantLevel(enchantLevel);
					}
				}
				
				// Send inventory update packet
				if (!Config.FORCE_INVENTORY_UPDATE)
				{
					InventoryUpdate playerIU = new InventoryUpdate();
					playerIU.addItem(createdItem);
					sendPacket(playerIU);
				}
				else
				{
					sendPacket(new ItemList(this, false));
				}
				
				// Update current load as well
				StatusUpdate su = new StatusUpdate(getObjectId());
				su.addAttribute(StatusUpdate.CUR_LOAD, getCurrentLoad());
				sendPacket(su);
				
				// If over capacity, drop the item
				if (!isGM() && !_inventory.validateCapacity(0, item.isQuestItem()) && createdItem.isDropable() && (!createdItem.isStackable() || (createdItem.getLastChange() != L2ItemInstance.MODIFIED)))
				{
					dropItem("InvDrop", createdItem, null, true, true);
				}
				
				if (CursedWeaponsManager.getInstance().isCursed(createdItem.getItemId()))
				{
					CursedWeaponsManager.getInstance().activate(this, createdItem);
				}
				
				return createdItem;
			}
		}
		return null;
	}
	
	/**
	 * Destroy item from inventory and send a Server->Client InventoryUpdate packet to the L2PcInstance.
	 * @param process : String Identifier of process triggering this action
	 * @param item : L2ItemInstance to be destroyed
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage : boolean Specifies whether to send message to Client about this action
	 * @return boolean informing if the action was successfull
	 */
	public boolean destroyItem(String process, L2ItemInstance item, L2Object reference, boolean sendMessage)
	{
		item = _inventory.destroyItem(process, item, this, reference);
		
		if (item == null)
		{
			if (sendMessage)
			{
				sendPacket(new SystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
			}
			
			return false;
		}
		
		// Send inventory update packet
		if (!Config.FORCE_INVENTORY_UPDATE)
		{
			InventoryUpdate playerIU = new InventoryUpdate();
			playerIU.addItem(item);
			sendPacket(playerIU);
			playerIU = null;
		}
		else
		{
			sendPacket(new ItemList(this, false));
		}
		
		// Update current load as well
		StatusUpdate su = new StatusUpdate(getObjectId());
		su.addAttribute(StatusUpdate.CUR_LOAD, getCurrentLoad());
		sendPacket(su);
		su = null;
		
		// Sends message to client if requested
		if (sendMessage)
		{
			if (count > 1)
			{
				SystemMessage sm = new SystemMessage(SystemMessageId.DISSAPEARED_ITEM);
				sm.addItemName(item.getItemId());
				sm.addNumber(count);
				sendPacket(sm);
				sm = null;
			}
			else
			{
				SystemMessage sm = new SystemMessage(SystemMessageId.S1_HAS_DISAPPEARED);
				sm.addItemName(item.getItemId());
				sendPacket(sm);
				sm = null;
			}
		}
		
		return true;
	}
	
	/**
	 * Destroys item from inventory and send a Server->Client InventoryUpdate packet to the L2PcInstance.
	 * @param process : String Identifier of process triggering this action
	 * @param count : int Quantity of items to be destroyed
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage : boolean Specifies whether to send message to Client about this action
	 * @return boolean informing if the action was successfull
	 */
	@Override
	public boolean destroyItem(String process, L2ItemInstance item, int count, L2Object reference, boolean sendMessage)
	{
		
		if (item == null || item.getCount() < count)
		{
			if (sendMessage)
			{
				sendPacket(new SystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
			}
			
			return false;
		}
		
		// Send inventory update packet
		if (!Config.FORCE_INVENTORY_UPDATE)
		{
			InventoryUpdate playerIU = new InventoryUpdate();
			playerIU.addItem(item);
			sendPacket(playerIU);
			playerIU = null;
		}
		else
		{
			sendPacket(new ItemList(this, false));
		}
		
		// Update current load as well
		StatusUpdate su = new StatusUpdate(getObjectId());
		su.addAttribute(StatusUpdate.CUR_LOAD, getCurrentLoad());
		sendPacket(su);
		su = null;
		
		// Sends message to client if requested
		if (sendMessage)
		{
			if (count > 1)
			{
				SystemMessage sm = new SystemMessage(SystemMessageId.DISSAPEARED_ITEM);
				sm.addItemName(item.getItemId());
				sm.addNumber(count);
				sendPacket(sm);
				sm = null;
			}
			else
			{
				SystemMessage sm = new SystemMessage(SystemMessageId.S1_HAS_DISAPPEARED);
				sm.addItemName(item.getItemId());
				sendPacket(sm);
				sm = null;
			}
		}
		item = null;
		
		return true;
	}
	
	@Override
	public boolean destroyItem(String process, int objectId, int count, L2Object reference, boolean sendMessage)
	{
		L2ItemInstance item = _inventory.getItemByObjectId(objectId);
		
		if (item == null || item.getCount() < count || _inventory.destroyItem(process, objectId, count, this, reference) == null)
		{
			if (sendMessage)
			{
				sendPacket(new SystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
			}
			
			return false;
		}
		
		// Send inventory update packet
		if (!Config.FORCE_INVENTORY_UPDATE)
		{
			InventoryUpdate playerIU = new InventoryUpdate();
			playerIU.addItem(item);
			sendPacket(playerIU);
			playerIU = null;
		}
		else
		{
			sendPacket(new ItemList(this, false));
		}
		
		// Update current load as well
		StatusUpdate su = new StatusUpdate(getObjectId());
		su.addAttribute(StatusUpdate.CUR_LOAD, getCurrentLoad());
		sendPacket(su);
		su = null;
		
		// Sends message to client if requested
		if (sendMessage)
		{
			if (count > 1)
			{
				SystemMessage sm = new SystemMessage(SystemMessageId.DISSAPEARED_ITEM);
				sm.addItemName(item.getItemId());
				sm.addNumber(count);
				sendPacket(sm);
				sm = null;
			}
			else
			{
				SystemMessage sm = new SystemMessage(SystemMessageId.S1_HAS_DISAPPEARED);
				sm.addItemName(item.getItemId());
				sendPacket(sm);
				sm = null;
			}
		}
		item = null;
		
		return true;
	}
	
	/**
	 * Destroys shots from inventory without logging and only occasional saving to database. Sends a Server->Client InventoryUpdate packet to the L2PcInstance.
	 * @param process : String Identifier of process triggering this action
	 * @param objectId : int Item Instance identifier of the item to be destroyed
	 * @param count : int Quantity of items to be destroyed
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage : boolean Specifies whether to send message to Client about this action
	 * @return boolean informing if the action was successfull
	 */
	public boolean destroyItemWithoutTrace(String process, int objectId, int count, L2Object reference, boolean sendMessage)
	{
		L2ItemInstance item = _inventory.getItemByObjectId(objectId);
		
		if (item == null || item.getCount() < count)
		{
			if (sendMessage)
			{
				sendPacket(new SystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
			}
			return false;
		}
		
		// Adjust item quantity
		if (item.getCount() > count)
		{
			synchronized (item)
			{
				item.changeCountWithoutTrace(process, -count, this, reference);
				item.setLastChange(L2ItemInstance.MODIFIED);
				
				// could do also without saving, but let's save approx 1 of 10
				if (GameTimeController.getInstance().getGameTicks() % 10 == 0)
				{
					item.updateDatabase();
				}
				_inventory.refreshWeight();
			}
		}
		else
		{
			// Destroy entire item and save to database
			_inventory.destroyItem(process, item, this, reference);
		}
		
		// Send inventory update packet
		if (!Config.FORCE_INVENTORY_UPDATE)
		{
			InventoryUpdate playerIU = new InventoryUpdate();
			playerIU.addItem(item);
			sendPacket(playerIU);
			playerIU = null;
		}
		else
		{
			sendPacket(new ItemList(this, false));
		}
		
		// Update current load as well
		StatusUpdate su = new StatusUpdate(getObjectId());
		su.addAttribute(StatusUpdate.CUR_LOAD, getCurrentLoad());
		sendPacket(su);
		su = null;
		
		// Sends message to client if requested
		if (sendMessage)
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.S1_HAS_DISAPPEARED);
			sm.addNumber(count);
			sm.addItemName(item.getItemId());
			sendPacket(sm);
			sm = null;
		}
		item = null;
		
		return true;
	}
	
	/**
	 * Destroy item from inventory by using its <B>itemId</B> and send a Server->Client InventoryUpdate packet to the L2PcInstance.
	 * @param process : String Identifier of process triggering this action
	 * @param itemId : int Item identifier of the item to be destroyed
	 * @param count : int Quantity of items to be destroyed
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage : boolean Specifies whether to send message to Client about this action
	 * @return boolean informing if the action was successfull
	 */
	@Override
	public boolean destroyItemByItemId(String process, int itemId, int count, L2Object reference, boolean sendMessage)
	{
		L2ItemInstance item = _inventory.getItemByItemId(itemId);
		
		if (item == null || item.getCount() < count || _inventory.destroyItemByItemId(process, itemId, count, this, reference) == null)
		{
			if (sendMessage)
			{
				sendPacket(new SystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
			}
			
			return false;
		}
		
		// Send inventory update packet
		if (!Config.FORCE_INVENTORY_UPDATE)
		{
			InventoryUpdate playerIU = new InventoryUpdate();
			playerIU.addItem(item);
			sendPacket(playerIU);
			playerIU = null;
		}
		else
		{
			sendPacket(new ItemList(this, false));
		}
		
		// Update current load as well
		StatusUpdate su = new StatusUpdate(getObjectId());
		su.addAttribute(StatusUpdate.CUR_LOAD, getCurrentLoad());
		sendPacket(su);
		su = null;
		
		// Sends message to client if requested
		if (sendMessage)
		{
			if (count > 1)
			{
				SystemMessage sm = new SystemMessage(SystemMessageId.DISSAPEARED_ITEM);
				sm.addItemName(item.getItemId());
				sm.addNumber(count);
				sendPacket(sm);
				sm = null;
			}
			else
			{
				SystemMessage sm = new SystemMessage(SystemMessageId.S1_HAS_DISAPPEARED);
				sm.addItemName(item.getItemId());
				sendPacket(sm);
				sm = null;
			}
		}
		item = null;
		
		return true;
	}
	
	/**
	 * Destroy all weared items from inventory and send a Server->Client InventoryUpdate packet to the L2PcInstance.
	 * @param process : String Identifier of process triggering this action
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage : boolean Specifies whether to send message to Client about this action
	 */
	public void destroyWearedItems(String process, L2Object reference, boolean sendMessage)
	{
		for (L2ItemInstance item : getInventory().getItems())
		{
			// Check if the item is a Try On item in order to remove it
			if (item.isWear())
			{
				if (item.isEquipped())
				{
					getInventory().unEquipItemInSlotAndRecord(item.getEquipSlot());
				}
				
				if (_inventory.destroyItem(process, item, this, reference) == null)
				{
					LOG.warn("Player " + getName() + " can't destroy weared item: " + item.getName() + "[ " + item.getObjectId() + " ]");
					continue;
				}
				
				// Send an Unequipped Message in system window of the player for each Item
				SystemMessage sm = new SystemMessage(SystemMessageId.S1_DISARMED);
				sm.addItemName(item.getItemId());
				sendPacket(sm);
			}
		}
		
		// Send the StatusUpdate Server->Client Packet to the player with new CUR_LOAD (0x0e) information
		StatusUpdate su = new StatusUpdate(getObjectId());
		su.addAttribute(StatusUpdate.CUR_LOAD, getCurrentLoad());
		sendPacket(su);
		
		sendPacket(new ItemList(getInventory().getItems(), true));
		
		broadcastUserInfo();
		
		sendMessage("Trying-on mode has ended.");
		
	}
	
	/**
	 * Transfers item to another ItemContainer and send a Server->Client InventoryUpdate packet to the L2PcInstance.
	 * @param process : String Identifier of process triggering this action
	 * @param objectId the object id
	 * @param count : int Quantity of items to be transfered
	 * @param target the target
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @return L2ItemInstance corresponding to the new item or the updated item in inventory
	 */
	public L2ItemInstance transferItem(String process, int objectId, int count, Inventory target, L2Object reference)
	{
		L2ItemInstance oldItem = checkItemManipulation(objectId, count, "transfer");
		if (oldItem == null)
		{
			return null;
		}
		
		L2ItemInstance newItem = getInventory().transferItem(process, objectId, count, target, this, reference);
		if (newItem == null)
		{
			return null;
		}
		
		// Send inventory update packet
		if (!Config.FORCE_INVENTORY_UPDATE)
		{
			InventoryUpdate playerIU = new InventoryUpdate();
			
			if (oldItem.getCount() > 0 && oldItem != newItem)
			{
				playerIU.addModifiedItem(oldItem);
			}
			else
			{
				playerIU.addRemovedItem(oldItem);
			}
			
			sendPacket(playerIU);
			playerIU = null;
		}
		else
		{
			sendPacket(new ItemList(this, false));
		}
		
		// Update current load as well
		StatusUpdate playerSU = new StatusUpdate(getObjectId());
		playerSU.addAttribute(StatusUpdate.CUR_LOAD, getCurrentLoad());
		sendPacket(playerSU);
		playerSU = null;
		
		// Send target update packet
		if (target instanceof PcInventory)
		{
			L2PcInstance targetPlayer = ((PcInventory) target).getOwner();
			
			if (!Config.FORCE_INVENTORY_UPDATE)
			{
				InventoryUpdate playerIU = new InventoryUpdate();
				
				if (newItem.getCount() > count)
				{
					playerIU.addModifiedItem(newItem);
				}
				else
				{
					playerIU.addNewItem(newItem);
				}
				
				targetPlayer.sendPacket(playerIU);
			}
			else
			{
				targetPlayer.sendPacket(new ItemList(targetPlayer, false));
			}
			
			// Update current load as well
			playerSU = new StatusUpdate(targetPlayer.getObjectId());
			playerSU.addAttribute(StatusUpdate.CUR_LOAD, targetPlayer.getCurrentLoad());
			targetPlayer.sendPacket(playerSU);
			targetPlayer = null;
			playerSU = null;
		}
		else if (target instanceof PetInventory)
		{
			PetInventoryUpdate petIU = new PetInventoryUpdate();
			
			if (newItem.getCount() > count)
			{
				petIU.addModifiedItem(newItem);
			}
			else
			{
				petIU.addNewItem(newItem);
			}
			
			((PetInventory) target).getOwner().getOwner().sendPacket(petIU);
			petIU = null;
		}
		oldItem = null;
		
		return newItem;
	}
	
	/**
	 * Drop item from inventory and send a Server->Client InventoryUpdate packet to the L2PcInstance.
	 * @param process : String Identifier of process triggering this action
	 * @param item : L2ItemInstance to be dropped
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage : boolean Specifies whether to send message to Client about this action
	 * @param protectItem the protect item
	 * @return boolean informing if the action was successfull
	 */
	public boolean dropItem(String process, L2ItemInstance item, L2Object reference, boolean sendMessage, boolean protectItem)
	{
		if (_freight.getItemByObjectId(item.getObjectId()) != null)
		{
			// Send a Server->Client ActionFailed to the L2PcInstance in order to avoid that the client wait another packet
			this.sendPacket(ActionFailed.STATIC_PACKET);
			
			Util.handleIllegalPlayerAction(this, "Warning!! Character " + this.getName() + " of account " + this.getAccountName() + " tried to drop Freight Items", IllegalPlayerAction.PUNISH_KICK);
			return false;
			
		}
		item = _inventory.dropItem(process, item, this, reference);
		
		if (item == null)
		{
			if (sendMessage)
			{
				sendPacket(new SystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
			}
			
			return false;
		}
		
		item.dropMe(this, getClientX() + Rnd.get(50) - 25, getClientY() + Rnd.get(50) - 25, getClientZ() + 20);
		
		if (Config.DESTROY_DROPPED_PLAYER_ITEM && !Config.LIST_PROTECTED_ITEMS.contains(item.getItemId()))
		{
			
			if (Config.AUTODESTROY_ITEM_AFTER > 0)
			{ // autodestroy enabled
				
				if (item.isEquipable() && Config.DESTROY_EQUIPABLE_PLAYER_ITEM || !item.isEquipable())
				{
					ItemsAutoDestroy.getInstance().addItem(item);
					item.setProtected(false);
				}
				else
				{
					item.setProtected(true);
				}
				
			}
			else
			{
				item.setProtected(true);
			}
			
		}
		else
		{
			item.setProtected(true);
			
		}
		
		if (protectItem)
		{
			item.getDropProtection().protect(this);
		}
		
		// Send inventory update packet
		if (!Config.FORCE_INVENTORY_UPDATE)
		{
			InventoryUpdate playerIU = new InventoryUpdate();
			playerIU.addItem(item);
			sendPacket(playerIU);
			playerIU = null;
		}
		else
		{
			sendPacket(new ItemList(this, false));
		}
		
		// Update current load as well
		StatusUpdate su = new StatusUpdate(getObjectId());
		su.addAttribute(StatusUpdate.CUR_LOAD, getCurrentLoad());
		sendPacket(su);
		su = null;
		
		// Sends message to client if requested
		if (sendMessage)
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.YOU_DROPPED_S1);
			sm.addItemName(item.getItemId());
			sendPacket(sm);
			sm = null;
		}
		
		return true;
	}
	
	/**
	 * Drop item from inventory by using its <B>objectID</B> and send a Server->Client InventoryUpdate packet to the L2PcInstance.
	 * @param process : String Identifier of process triggering this action
	 * @param objectId : int Item Instance identifier of the item to be dropped
	 * @param count : int Quantity of items to be dropped
	 * @param x : int coordinate for drop X
	 * @param y : int coordinate for drop Y
	 * @param z : int coordinate for drop Z
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage : boolean Specifies whether to send message to Client about this action
	 * @param protectItem the protect item
	 * @return L2ItemInstance corresponding to the new item or the updated item in inventory
	 */
	public L2ItemInstance dropItem(String process, int objectId, int count, int x, int y, int z, L2Object reference, boolean sendMessage, boolean protectItem)
	{
		
		if (_freight.getItemByObjectId(objectId) != null)
		{
			
			// Send a Server->Client ActionFailed to the L2PcInstance in order to avoid that the client wait another packet
			this.sendPacket(ActionFailed.STATIC_PACKET);
			
			Util.handleIllegalPlayerAction(this, "Warning!! Character " + this.getName() + " of account " + this.getAccountName() + " tried to drop Freight Items", IllegalPlayerAction.PUNISH_KICK);
			return null;
			
		}
		
		L2ItemInstance invitem = _inventory.getItemByObjectId(objectId);
		L2ItemInstance item = _inventory.dropItem(process, objectId, count, this, reference);
		
		if (item == null)
		{
			if (sendMessage)
			{
				sendPacket(new SystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
			}
			
			return null;
		}
		
		item.dropMe(this, x, y, z);
		
		if (Config.AUTODESTROY_ITEM_AFTER > 0 && Config.DESTROY_DROPPED_PLAYER_ITEM && !Config.LIST_PROTECTED_ITEMS.contains(item.getItemId()))
		{
			if (item.isEquipable() && Config.DESTROY_EQUIPABLE_PLAYER_ITEM || !item.isEquipable())
			{
				ItemsAutoDestroy.getInstance().addItem(item);
			}
		}
		if (Config.DESTROY_DROPPED_PLAYER_ITEM)
		{
			if (!item.isEquipable() || item.isEquipable() && Config.DESTROY_EQUIPABLE_PLAYER_ITEM)
			{
				item.setProtected(false);
			}
			else
			{
				item.setProtected(true);
			}
		}
		else
		{
			item.setProtected(true);
		}
		
		if (protectItem)
		{
			item.getDropProtection().protect(this);
		}
		
		// Send inventory update packet
		if (!Config.FORCE_INVENTORY_UPDATE)
		{
			InventoryUpdate playerIU = new InventoryUpdate();
			playerIU.addItem(invitem);
			sendPacket(playerIU);
			playerIU = null;
		}
		else
		{
			sendPacket(new ItemList(this, false));
		}
		
		// Update current load as well
		StatusUpdate su = new StatusUpdate(getObjectId());
		su.addAttribute(StatusUpdate.CUR_LOAD, getCurrentLoad());
		sendPacket(su);
		
		// Sends message to client if requested
		if (sendMessage)
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.YOU_DROPPED_S1);
			sm.addItemName(item.getItemId());
			sendPacket(sm);
		}
		
		return item;
	}
	
	/**
	 * Check item manipulation.
	 * @param objectId the object id
	 * @param count the count
	 * @param action the action
	 * @return the l2 item instance
	 */
	public L2ItemInstance checkItemManipulation(int objectId, int count, String action)
	{
		if (L2World.getInstance().findObject(objectId) == null)
		{
			LOG.warn(getObjectId() + ": player tried to " + action + " item not available in L2World.");
			return null;
		}
		
		L2ItemInstance item = getInventory().getItemByObjectId(objectId);
		
		if (item == null || item.getOwnerId() != getObjectId())
		{
			LOG.warn(getObjectId() + ": player tried to " + action + " item he is not owner of.");
			return null;
		}
		
		if (count < 0 || count > 1 && !item.isStackable())
		{
			LOG.warn(getObjectId() + ": player tried to " + action + " item with invalid count: " + count);
			return null;
		}
		
		if (count > item.getCount())
		{
			LOG.warn(getObjectId() + ": player tried to " + action + " more items than he owns.");
			return null;
		}
		
		// Pet is summoned and not the item that summoned the pet AND not the buggle from strider you're mounting
		if (getPet() != null && getPet().getControlItemId() == objectId || getMountObjectID() == objectId)
		{
			if (Config.DEBUG)
			{
				LOG.warn(getObjectId() + ": player tried to " + action + " item controling pet.");
			}
			
			return null;
		}
		
		if (getActiveEnchantItem() != null && getActiveEnchantItem().getObjectId() == objectId)
		{
			if (Config.DEBUG)
			{
				LOG.warn(getObjectId() + ":player tried to " + action + " an enchant scroll he was using.");
			}
			
			return null;
		}
		
		if (item.isWear())
		{
			return null;
		}
		
		return item;
	}
	
	/**
	 * Set _protectEndTime according settings.
	 * @param protect the new protection
	 */
	public void setProtection(boolean protect)
	{
		if (Config.DEVELOPER && (protect || _protectEndTime > 0))
		{
			LOG.info(getName() + ": Protection " + (protect ? "ON " + (GameTimeController.getInstance().getGameTicks() + Config.PLAYER_SPAWN_PROTECTION * GameTimeController.TICKS_PER_SECOND) : "OFF") + " (currently " + GameTimeController.getInstance().getGameTicks() + ")");
		}
		
		if (isInOlympiadMode())
		{
			return;
		}
		
		_protectEndTime = protect ? GameTimeController.getInstance().getGameTicks() + Config.PLAYER_SPAWN_PROTECTION * GameTimeController.TICKS_PER_SECOND : 0;
		
		if (protect)
		{
			ThreadPoolManager.getInstance().scheduleGeneral(new TeleportProtectionFinalizer(this), (Config.PLAYER_SPAWN_PROTECTION - 1) * 1000);
		}
	}
	
	/**
	 * Set _teleportProtectEndTime according settings.
	 * @param protect the new protection
	 */
	public void setTeleportProtection(boolean protect)
	{
		if (Config.DEVELOPER && (protect || _teleportProtectEndTime > 0))
		{
			LOG.warn(getName() + ": Tele Protection " + (protect ? "ON " + (GameTimeController.getInstance().getGameTicks() + Config.PLAYER_TELEPORT_PROTECTION * GameTimeController.TICKS_PER_SECOND) : "OFF") + " (currently " + GameTimeController.getInstance().getGameTicks() + ")");
		}
		
		_teleportProtectEndTime = protect ? GameTimeController.getInstance().getGameTicks() + Config.PLAYER_TELEPORT_PROTECTION * GameTimeController.TICKS_PER_SECOND : 0;
		
		if (protect)
		{
			ThreadPoolManager.getInstance().scheduleGeneral(new TeleportProtectionFinalizer(this), (Config.PLAYER_TELEPORT_PROTECTION - 1) * 1000);
		}
		
		if (Config.EFFECT_TELEPORT_PROTECTION)
		{
			if (protect)
			{
				startAbnormalEffect(2097152);
			}
			else if (!protect)
			{
				stopAbnormalEffect(2097152);
			}
		}
	}
	
	static class TeleportProtectionFinalizer implements Runnable
	{
		private L2PcInstance _activeChar;
		
		TeleportProtectionFinalizer(L2PcInstance activeChar)
		{
			_activeChar = activeChar;
		}
		
		@Override
		public void run()
		{
			try
			{
				if (_activeChar.isSpawnProtected())
				{
					_activeChar.sendMessage("The Spawn Protection has been removed.");
				}
				else if (_activeChar.isTeleportProtected())
				{
					_activeChar.sendMessage("The Teleport Spawn Protection has been removed.");
				}
				
				if (Config.PLAYER_SPAWN_PROTECTION > 0)
				{
					_activeChar.setProtection(false);
				}
				
				if (Config.PLAYER_TELEPORT_PROTECTION > 0)
				{
					_activeChar.setTeleportProtection(false);
				}
			}
			catch (Exception e)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
				{
					e.printStackTrace();
				}
			}
		}
	}
	
	public void setRecentFakeDeath(boolean protect)
	{
		_recentFakeDeathEndTime = protect ? GameTimeController.getInstance().getGameTicks() + Config.PLAYER_FAKEDEATH_UP_PROTECTION * GameTimeController.TICKS_PER_SECOND : 0;
	}
	
	public boolean isRecentFakeDeath()
	{
		return _recentFakeDeathEndTime > GameTimeController.getInstance().getGameTicks();
	}
	
	public L2GameClient getClient()
	{
		return _client;
	}
	
	public void setClient(L2GameClient client)
	{
		_client = client;
	}
	
	public void closeNetConnection()
	{
		if (_client != null)
		{
			_client.close(new LeaveWorld());
			setClient(null);
		}
	}
	
	@Override
	public void onAction(L2PcInstance player)
	{
		if (((TvT.is_started() || TvT.is_teleport()) && !Config.TVT_ALLOW_INTERFERENCE) || ((CTF.is_started() || CTF.is_teleport()) && !Config.CTF_ALLOW_INTERFERENCE) || ((DM.is_started() || DM.is_teleport()) && !Config.DM_ALLOW_INTERFERENCE))
		{
			if ((_inEventTvT && !player._inEventTvT) || (!_inEventTvT && player._inEventTvT))
			{
				player.sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
			else if ((_inEventCTF && !player._inEventCTF) || (!_inEventCTF && player._inEventCTF))
			{
				player.sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
			else if ((_inEventDM && !player._inEventDM) || (!_inEventDM && player._inEventDM))
			{
				player.sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
		}
		// Check if the L2PcInstance is confused
		if (player.isOutOfControl())
		{
			// Send a Server->Client packet ActionFailed to the player
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		// Check if the player already target this L2PcInstance
		if (player.getTarget() != this)
		{
			player.setTarget(this);
			player.sendPacket(new MyTargetSelected(getObjectId(), 0));
		}
		else
		{
			if (getPrivateStoreType() != 0 || isSellBuff())
			{
				// Notify the L2PcInstance AI with AI_INTENTION_INTERACT
				player.getAI().setIntention(CtrlIntention.AI_INTENTION_INTERACT, this);
			}
			else
			{
				if (isAutoAttackable(player))
				{
					if (Config.ALLOW_CHAR_KILL_PROTECT)
					{
						Siege siege = SiegeManager.getInstance().getSiege(player);
						
						if (siege != null && siege.getIsInProgress())
						{
							if (player.getLevel() > 20 && ((L2Character) player.getTarget()).getLevel() < 20)
							{
								player.sendMessage("Your target is not in your grade!");
								player.sendPacket(ActionFailed.STATIC_PACKET);
							}
							
							if (player.getLevel() > 40 && ((L2Character) player.getTarget()).getLevel() < 40)
							{
								player.sendMessage("Your target is not in your grade!");
								player.sendPacket(ActionFailed.STATIC_PACKET);
							}
							
							if (player.getLevel() > 52 && ((L2Character) player.getTarget()).getLevel() < 52)
							{
								player.sendMessage("Your target is not in your grade!");
								player.sendPacket(ActionFailed.STATIC_PACKET);
							}
							
							if (player.getLevel() > 61 && ((L2Character) player.getTarget()).getLevel() < 61)
							{
								player.sendMessage("Your target is not in your grade!");
								player.sendPacket(ActionFailed.STATIC_PACKET);
							}
							
							if (player.getLevel() > 76 && ((L2Character) player.getTarget()).getLevel() < 76)
							{
								player.sendMessage("Your target is not in your grade!");
								player.sendPacket(ActionFailed.STATIC_PACKET);
							}
							
							if (player.getLevel() < 20 && ((L2Character) player.getTarget()).getLevel() > 20)
							{
								player.sendMessage("Your target is not in your grade!");
								player.sendPacket(ActionFailed.STATIC_PACKET);
							}
							
							if (player.getLevel() < 40 && ((L2Character) player.getTarget()).getLevel() > 40)
							{
								player.sendMessage("Your target is not in your grade!");
								player.sendPacket(ActionFailed.STATIC_PACKET);
							}
							
							if (player.getLevel() < 52 && ((L2Character) player.getTarget()).getLevel() > 52)
							{
								player.sendMessage("Your target is not in your grade!");
								player.sendPacket(ActionFailed.STATIC_PACKET);
							}
							
							if (player.getLevel() < 61 && ((L2Character) player.getTarget()).getLevel() > 61)
							{
								player.sendMessage("Your target is not in your grade!");
								player.sendPacket(ActionFailed.STATIC_PACKET);
							}
							
							if (player.getLevel() < 76 && ((L2Character) player.getTarget()).getLevel() > 76)
							{
								player.sendMessage("Your target is not in your grade!");
								player.sendPacket(ActionFailed.STATIC_PACKET);
							}
						}
						siege = null;
					}
					// Player with lvl < 21 can't attack a cursed weapon holder
					// And a cursed weapon holder can't attack players with lvl < 21
					if (isCursedWeaponEquiped() && player.getLevel() < 21 || player.isCursedWeaponEquiped() && getLevel() < 21)
					{
						player.sendPacket(ActionFailed.STATIC_PACKET);
					}
					else
					{
						if (Config.GEODATA)
						{
							if (GeoData.getInstance().canSeeTarget(player, this))
							{
								player.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, this);
								player.onActionRequest();
							}
						}
						else
						{
							player.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, this);
							player.onActionRequest();
						}
					}
				}
				else
				{
					if (Config.GEODATA)
					{
						if (GeoData.getInstance().canSeeTarget(player, this))
						{
							player.getAI().setIntention(CtrlIntention.AI_INTENTION_FOLLOW, this);
						}
					}
					else
					{
						player.getAI().setIntention(CtrlIntention.AI_INTENTION_FOLLOW, this);
					}
					
					// avoids to stuck when clicking two or more times
					player.sendPacket(ActionFailed.STATIC_PACKET);
				}
			}
		}
	}
	
	@Override
	public void onActionShift(L2PcInstance player)
	{
		L2Weapon currentWeapon = player.getActiveWeaponItem();
		
		if (player.isGM())
		{
			if (this != player.getTarget())
			{
				player.setTarget(this);
				player.sendPacket(new MyTargetSelected(getObjectId(), 0));
				/*
				 * if (player != this) { player.sendPacket(new ValidateLocation(this)); }
				 */
			}
			else
			{
				AdminEditChar.gatherCharacterInfo(player, this, "charinfo.htm");
			}
		}
		else
		// Like L2OFF set the target of the L2PcInstance player
		{
			if (((TvT.is_started() || TvT.is_teleport()) && !Config.TVT_ALLOW_INTERFERENCE) || ((CTF.is_started() || CTF.is_teleport()) && !Config.CTF_ALLOW_INTERFERENCE) || ((DM.is_started() || DM.is_teleport()) && !Config.DM_ALLOW_INTERFERENCE))
			{
				if ((_inEventTvT && !player._inEventTvT) || (!_inEventTvT && player._inEventTvT))
				{
					player.sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}
				else if ((_inEventCTF && !player._inEventCTF) || (!_inEventCTF && player._inEventCTF))
				{
					player.sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}
				else if ((_inEventDM && !player._inEventDM) || (!_inEventDM && player._inEventDM))
				{
					player.sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}
			}
			// Check if the L2PcInstance is confused
			if (player.isOutOfControl())
			{
				// Send a Server->Client packet ActionFailed to the player
				player.sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
			
			// Check if the player already target this L2PcInstance
			if (player.getTarget() != this)
			{
				// Set the target of the player
				player.setTarget(this);
				
				// Send a Server->Client packet MyTargetSelected to the player
				// The color to display in the select window is White
				player.sendPacket(new MyTargetSelected(getObjectId(), 0));
				/*
				 * if (player != this) { player.sendPacket(new ValidateLocation(this)); }
				 */
			}
			else
			{
				/*
				 * if (player != this) { player.sendPacket(new ValidateLocation(this)); }
				 */
				// Check if this L2PcInstance has a Private Store
				if (getPrivateStoreType() != 0)
				{
					// Notify the L2PcInstance AI with AI_INTENTION_INTERACT
					player.getAI().setIntention(CtrlIntention.AI_INTENTION_INTERACT, this);
				}
				else
				{
					// Check if this L2PcInstance is autoAttackable
					// if (isAutoAttackable(player) || (player._inEventTvT && TvT._started) || (player._inEventCTF && CTF._started) || (player._inEventDM && DM._started) || (player._inEventVIP && VIP._started))
					if (isAutoAttackable(player))
					{
						
						if (Config.ALLOW_CHAR_KILL_PROTECT)
						{
							Siege siege = SiegeManager.getInstance().getSiege(player);
							
							if (siege != null && siege.getIsInProgress())
							{
								if (player.getLevel() > 20 && ((L2Character) player.getTarget()).getLevel() < 20)
								{
									player.sendMessage("Your target is not in your grade!");
									player.sendPacket(ActionFailed.STATIC_PACKET);
								}
								
								if (player.getLevel() > 40 && ((L2Character) player.getTarget()).getLevel() < 40)
								{
									player.sendMessage("Your target is not in your grade!");
									player.sendPacket(ActionFailed.STATIC_PACKET);
								}
								
								if (player.getLevel() > 52 && ((L2Character) player.getTarget()).getLevel() < 52)
								{
									player.sendMessage("Your target is not in your grade!");
									player.sendPacket(ActionFailed.STATIC_PACKET);
								}
								
								if (player.getLevel() > 61 && ((L2Character) player.getTarget()).getLevel() < 61)
								{
									player.sendMessage("Your target is not in your grade!");
									player.sendPacket(ActionFailed.STATIC_PACKET);
								}
								
								if (player.getLevel() > 76 && ((L2Character) player.getTarget()).getLevel() < 76)
								{
									player.sendMessage("Your target is not in your grade!");
									player.sendPacket(ActionFailed.STATIC_PACKET);
								}
								
								if (player.getLevel() < 20 && ((L2Character) player.getTarget()).getLevel() > 20)
								{
									player.sendMessage("Your target is not in your grade!");
									player.sendPacket(ActionFailed.STATIC_PACKET);
								}
								
								if (player.getLevel() < 40 && ((L2Character) player.getTarget()).getLevel() > 40)
								{
									player.sendMessage("Your target is not in your grade!");
									player.sendPacket(ActionFailed.STATIC_PACKET);
								}
								
								if (player.getLevel() < 52 && ((L2Character) player.getTarget()).getLevel() > 52)
								{
									player.sendMessage("Your target is not in your grade!");
									player.sendPacket(ActionFailed.STATIC_PACKET);
								}
								
								if (player.getLevel() < 61 && ((L2Character) player.getTarget()).getLevel() > 61)
								{
									player.sendMessage("Your target is not in your grade!");
									player.sendPacket(ActionFailed.STATIC_PACKET);
								}
								
								if (player.getLevel() < 76 && ((L2Character) player.getTarget()).getLevel() > 76)
								{
									player.sendMessage("Your target is not in your grade!");
									player.sendPacket(ActionFailed.STATIC_PACKET);
								}
							}
							siege = null;
						}
						// Player with lvl < 21 can't attack a cursed weapon holder
						// And a cursed weapon holder can't attack players with lvl < 21
						if (isCursedWeaponEquiped() && player.getLevel() < 21 || player.isCursedWeaponEquiped() && getLevel() < 21)
						{
							player.sendPacket(ActionFailed.STATIC_PACKET);
						}
						else
						{
							if (Config.GEODATA)
							{
								if (GeoData.getInstance().canSeeTarget(player, this))
								{
									// Calculate the distance between the L2PcInstance
									// Only archer can hit from long
									if (currentWeapon != null && currentWeapon.getItemType() == L2WeaponType.BOW)
									{
										player.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, this);
										player.onActionRequest();
									}
									else
									{
										player.sendPacket(ActionFailed.STATIC_PACKET);
									}
								}
							}
							else
							{
								// Calculate the distance between the L2PcInstance
								// Only archer can hit from long
								if (currentWeapon != null && currentWeapon.getItemType() == L2WeaponType.BOW)
								{
									player.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, this);
									player.onActionRequest();
								}
								else
								{
									player.sendPacket(ActionFailed.STATIC_PACKET);
								}
							}
						}
					}
					else
					{
						if (Config.GEODATA)
						{
							if (GeoData.getInstance().canSeeTarget(player, this))
							{
								// Calculate the distance between the L2PcInstance
								// Only archer can hit from long
								if (currentWeapon != null && currentWeapon.getItemType() == L2WeaponType.BOW)
								{
									player.getAI().setIntention(CtrlIntention.AI_INTENTION_FOLLOW, this);
								}
								else
								{
									player.sendPacket(ActionFailed.STATIC_PACKET);
								}
								
							}
						}
						else
						{
							// Calculate the distance between the L2PcInstance
							// Only archer can hit from long
							if (currentWeapon != null && currentWeapon.getItemType() == L2WeaponType.BOW)
							{
								player.getAI().setIntention(CtrlIntention.AI_INTENTION_FOLLOW, this);
							}
							else
							{
								player.sendPacket(ActionFailed.STATIC_PACKET);
							}
						}
					}
				}
			}
		}
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	/*
	 * (non-Javadoc)
	 * @see l2jorion.game.model.actor.instance.L2PlayableInstance#isInFunEvent()
	 */
	@Override
	public boolean isInFunEvent()
	{
		return (atEvent || isInStartedTVTEvent() || isInStartedDMEvent() || isInStartedCTFEvent() || isInStartedVIPEvent());
	}
	
	public boolean isInStartedTVTEvent()
	{
		return (TvT.is_started() && _inEventTvT);
	}
	
	public boolean isRegisteredInTVTEvent()
	{
		return _inEventTvT;
	}
	
	public boolean isInStartedDMEvent()
	{
		return (DM.is_started() && _inEventDM);
	}
	
	public boolean isRegisteredInDMEvent()
	{
		return _inEventDM;
	}
	
	public boolean isInStartedCTFEvent()
	{
		return (CTF.is_started() && _inEventCTF);
	}
	
	public boolean isRegisteredInCTFEvent()
	{
		return _inEventCTF;
	}
	
	public boolean isInStartedVIPEvent()
	{
		return (VIP._started && _inEventVIP);
	}
	
	public boolean isRegisteredInVIPEvent()
	{
		return _inEventVIP;
	}
	
	/**
	 * Checks if is registered in fun event.
	 * @return true, if is registered in fun event
	 */
	public boolean isRegisteredInFunEvent()
	{
		return (atEvent || (_inEventTvT) || (_inEventDM) || (_inEventCTF) || (_inEventVIP) || OlympiadManager.getInstance().isRegistered(this));
	}
	
	/**
	 * Are player offensive skills locked.
	 * @return true, if successful
	 */
	public boolean arePlayerOffensiveSkillsLocked()
	{
		return isInOlympiadMode() && !isOlympiadStart();
	}
	
	/**
	 * Returns true if cp update should be done, false if not.
	 * @param barPixels the bar pixels
	 * @return boolean
	 */
	private boolean needCpUpdate(int barPixels)
	{
		double currentCp = getCurrentCp();
		
		if (currentCp <= 1.0 || getMaxCp() < barPixels)
		{
			return true;
		}
		
		if (currentCp <= _cpUpdateDecCheck || currentCp >= _cpUpdateIncCheck)
		{
			if (currentCp == getMaxCp())
			{
				_cpUpdateIncCheck = currentCp + 1;
				_cpUpdateDecCheck = currentCp - _cpUpdateInterval;
			}
			else
			{
				double doubleMulti = currentCp / _cpUpdateInterval;
				int intMulti = (int) doubleMulti;
				
				_cpUpdateDecCheck = _cpUpdateInterval * (doubleMulti < intMulti ? intMulti-- : intMulti);
				_cpUpdateIncCheck = _cpUpdateDecCheck + _cpUpdateInterval;
			}
			
			return true;
		}
		
		return false;
	}
	
	/**
	 * Returns true if mp update should be done, false if not.
	 * @param barPixels the bar pixels
	 * @return boolean
	 */
	private boolean needMpUpdate(int barPixels)
	{
		double currentMp = getCurrentMp();
		
		if (currentMp <= 1.0 || getMaxMp() < barPixels)
		{
			return true;
		}
		
		if (currentMp <= _mpUpdateDecCheck || currentMp >= _mpUpdateIncCheck)
		{
			if (currentMp == getMaxMp())
			{
				_mpUpdateIncCheck = currentMp + 1;
				_mpUpdateDecCheck = currentMp - _mpUpdateInterval;
			}
			else
			{
				double doubleMulti = currentMp / _mpUpdateInterval;
				int intMulti = (int) doubleMulti;
				
				_mpUpdateDecCheck = _mpUpdateInterval * (doubleMulti < intMulti ? intMulti-- : intMulti);
				_mpUpdateIncCheck = _mpUpdateDecCheck + _mpUpdateInterval;
			}
			
			return true;
		}
		
		return false;
	}
	
	/**
	 * Send packet StatusUpdate with current HP,MP and CP to the L2PcInstance and only current HP, MP and Level to all other L2PcInstance of the Party.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Send the Server->Client packet StatusUpdate with current HP, MP and CP to this L2PcInstance</li><BR>
	 * <li>Send the Server->Client packet PartySmallWindowUpdate with current HP, MP and Level to all other L2PcInstance of the Party</li><BR>
	 * <BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method DOESN'T SEND current HP and MP to all L2PcInstance of the _statusListener</B></FONT><BR>
	 * <BR>
	 */
	@Override
	public void broadcastStatusUpdate()
	{
		if (Config.FORCE_COMPLETE_STATUS_UPDATE)
		{
			StatusUpdate su = new StatusUpdate(this);
			sendPacket(su);
		}
		else
		{
			StatusUpdate su = new StatusUpdate(getObjectId());
			su.addAttribute(StatusUpdate.CUR_HP, (int) getCurrentHp());
			su.addAttribute(StatusUpdate.CUR_MP, (int) getCurrentMp());
			su.addAttribute(StatusUpdate.CUR_CP, (int) getCurrentCp());
			su.addAttribute(StatusUpdate.MAX_CP, getMaxCp());
			sendPacket(su);
		}
		
		// Check if a party is in progress and party window update is usefull
		if (isInParty() && (needCpUpdate(352) || needHpUpdate(352) || needMpUpdate(352)))
		{
			getParty().broadcastToPartyMembers(this, new PartySmallWindowUpdate(this));
		}
		
		if (isInOlympiadMode() && isOlympiadStart() && (needCpUpdate(352) || needHpUpdate(352)))
		{
			final OlympiadGameTask game = OlympiadGameManager.getInstance().getOlympiadTask(getOlympiadGameId());
			if (game != null && game.isBattleStarted())
			{
				game.getZone().broadcastStatusUpdate(this);
			}
		}
		
		if (isInDuel())
		{
			ExDuelUpdateUserInfo update = new ExDuelUpdateUserInfo(this);
			DuelManager.getInstance().broadcastToOppositTeam(this, update);
		}
	}
	
	public void updatePvPColor(int pvpKillAmount)
	{
		if (Config.PVP_COLOR_SYSTEM_ENABLED)
		{
			if (isGM())
			{
				return;
			}
			
			if (isDonator())
			{
				return;
			}
			
			if (pvpKillAmount >= Config.PVP_AMOUNT1 && pvpKillAmount < Config.PVP_AMOUNT2)
			{
				getAppearance().setNameColor(Config.NAME_COLOR_FOR_PVP_AMOUNT1);
			}
			else if (pvpKillAmount >= Config.PVP_AMOUNT2 && pvpKillAmount < Config.PVP_AMOUNT3)
			{
				getAppearance().setNameColor(Config.NAME_COLOR_FOR_PVP_AMOUNT2);
			}
			else if (pvpKillAmount >= Config.PVP_AMOUNT3 && pvpKillAmount < Config.PVP_AMOUNT4)
			{
				getAppearance().setNameColor(Config.NAME_COLOR_FOR_PVP_AMOUNT3);
			}
			else if (pvpKillAmount >= Config.PVP_AMOUNT4 && pvpKillAmount < Config.PVP_AMOUNT5)
			{
				getAppearance().setNameColor(Config.NAME_COLOR_FOR_PVP_AMOUNT4);
			}
			else if (pvpKillAmount >= Config.PVP_AMOUNT5)
			{
				getAppearance().setNameColor(Config.NAME_COLOR_FOR_PVP_AMOUNT5);
			}
		}
	}
	
	public void updatePkColor(int pkKillAmount)
	{
		if (Config.PK_COLOR_SYSTEM_ENABLED)
		{
			// Check if the character has GM access and if so, let them be, like above.
			if (isGM())
			{
				return;
			}
			
			if (pkKillAmount >= Config.PK_AMOUNT1 && pkKillAmount < Config.PVP_AMOUNT2)
			{
				getAppearance().setTitleColor(Config.TITLE_COLOR_FOR_PK_AMOUNT1);
			}
			else if (pkKillAmount >= Config.PK_AMOUNT2 && pkKillAmount < Config.PVP_AMOUNT3)
			{
				getAppearance().setTitleColor(Config.TITLE_COLOR_FOR_PK_AMOUNT2);
			}
			else if (pkKillAmount >= Config.PK_AMOUNT3 && pkKillAmount < Config.PVP_AMOUNT4)
			{
				getAppearance().setTitleColor(Config.TITLE_COLOR_FOR_PK_AMOUNT3);
			}
			else if (pkKillAmount >= Config.PK_AMOUNT4 && pkKillAmount < Config.PVP_AMOUNT5)
			{
				getAppearance().setTitleColor(Config.TITLE_COLOR_FOR_PK_AMOUNT4);
			}
			else if (pkKillAmount >= Config.PK_AMOUNT5)
			{
				getAppearance().setTitleColor(Config.TITLE_COLOR_FOR_PK_AMOUNT5);
			}
		}
	}
	
	public final void broadcastUserInfo()
	{
		sendPacket(new UserInfo(this));
		Broadcast.toKnownPlayers(this, new CharInfo(this));
	}
	
	public final void broadcastTitleInfo()
	{
		sendPacket(new UserInfo(this));
		Broadcast.toKnownPlayers(this, new TitleUpdate(this));
	}
	
	public int getAllyId()
	{
		if (_clan == null)
		{
			return 0;
		}
		return _clan.getAllyId();
	}
	
	public int getAllyCrestId()
	{
		if (getClanId() == 0 || getClan() == null)
		{
			return 0;
		}
		if (getClan().getAllyId() == 0)
		{
			return 0;
		}
		return getClan().getAllyCrestId();
	}
	
	/**
	 * Manage Interact Task with another L2PcInstance.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>If the private store is a STORE_PRIVATE_SELL, send a Server->Client PrivateBuyListSell packet to the L2PcInstance</li>
	 * <li>If the private store is a STORE_PRIVATE_BUY, send a Server->Client PrivateBuyListBuy packet to the L2PcInstance</li>
	 * <li>If the private store is a STORE_PRIVATE_MANUFACTURE, send a Server->Client RecipeShopSellList packet to the L2PcInstance</li><BR>
	 * <BR>
	 * @param target The L2Character targeted
	 */
	public void doInteract(L2Character target)
	{
		if (target instanceof L2PcInstance)
		{
			L2PcInstance temp = (L2PcInstance) target;
			
			if (temp.isSellBuff())
			{
				sellBuffsMenu(temp, 0);
			}
			
			sendPacket(ActionFailed.STATIC_PACKET);
			
			switch (temp.getPrivateStoreType())
			{
				case STORE_PRIVATE_SELL:
				case STORE_PRIVATE_PACKAGE_SELL:
					sendPacket(new PrivateStoreListSell(this, temp));
					break;
				
				case STORE_PRIVATE_BUY:
					sendPacket(new PrivateStoreListBuy(this, temp));
					break;
				
				case STORE_PRIVATE_MANUFACTURE:
					sendPacket(new RecipeShopSellList(this, temp));
					break;
			}
		}
		else
		{
			if (target != null)
			{
				target.onAction(this);
			}
		}
	}
	
	/**
	 * Manage AutoLoot Task.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Send a System Message to the L2PcInstance : YOU_PICKED_UP_S1_ADENA or YOU_PICKED_UP_S1_S2</li>
	 * <li>Add the Item to the L2PcInstance inventory</li>
	 * <li>Send a Server->Client packet InventoryUpdate to this L2PcInstance with NewItem (use a new slot) or ModifiedItem (increase amount)</li>
	 * <li>Send a Server->Client packet StatusUpdate to this L2PcInstance with current weight</li><BR>
	 * <BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : If a Party is in progress, distribute Items between party members</B></FONT><BR>
	 * <BR>
	 * @param target The L2ItemInstance dropped
	 * @param item the item
	 */
	public void doAutoLoot(L2Attackable target, L2Attackable.RewardItem item)
	{
		if (isInParty())
		{
			getParty().distributeItem(this, item, false, target);
		}
		else if (item.getItemId() == 57)
		{
			addAdena("AutoLoot", item.getCount(), target, true);
		}
		else
		{
			addItem("AutoLoot", item.getItemId(), item.getCount(), item.getEnchantLevel(), target, true);
		}
	}
	
	/**
	 * Manage Pickup Task.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Send a Server->Client packet StopMove to this L2PcInstance</li>
	 * <li>Remove the L2ItemInstance from the world and send server->client GetItem packets</li>
	 * <li>Send a System Message to the L2PcInstance : YOU_PICKED_UP_S1_ADENA or YOU_PICKED_UP_S1_S2</li>
	 * <li>Add the Item to the L2PcInstance inventory</li>
	 * <li>Send a Server->Client packet InventoryUpdate to this L2PcInstance with NewItem (use a new slot) or ModifiedItem (increase amount)</li>
	 * <li>Send a Server->Client packet StatusUpdate to this L2PcInstance with current weight</li> <BR>
	 * <BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : If a Party is in progress, distribute Items between party members</B></FONT><BR>
	 * <BR>
	 * @param object The L2ItemInstance to pick up
	 */
	public void doPickupItem(L2Object object)
	{
		if (isAlikeDead() || isFakeDeath())
		{
			return;
		}
		
		// Set the AI Intention to AI_INTENTION_IDLE
		getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
		
		// Check if the L2Object to pick up is a L2ItemInstance
		if (!(object instanceof L2ItemInstance))
		{
			// dont try to pickup anything that is not an item :)
			LOG.warn(this + "trying to pickup wrong target." + getTarget());
			return;
		}
		
		L2ItemInstance target = (L2ItemInstance) object;
		
		// Send a Server->Client packet ActionFailed to this L2PcInstance
		sendPacket(ActionFailed.STATIC_PACKET);
		sendPacket(new StopMove(this));
		
		synchronized (target)
		{
			// Check if the target to pick up is visible
			if (!target.isVisible())
			{
				// Send a Server->Client packet ActionFailed to this L2PcInstance
				sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
			
			// Like L2OFF you can't pickup items with private store opened
			if (getPrivateStoreType() != 0)
			{
				// Send a Server->Client packet ActionFailed to this L2PcInstance
				sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
			
			if (!target.getDropProtection().tryPickUp(this) && target.getItemId() != 8190 && target.getItemId() != 8689)
			{
				sendPacket(ActionFailed.STATIC_PACKET);
				SystemMessage smsg = new SystemMessage(SystemMessageId.FAILED_TO_PICKUP_S1);
				smsg.addItemName(target.getItemId());
				sendPacket(smsg);
				return;
			}
			
			if ((isInParty() && getParty().getLootDistribution() == L2Party.ITEM_LOOTER || !isInParty()) && !_inventory.validateCapacity(target))
			{
				sendPacket(ActionFailed.STATIC_PACKET);
				sendPacket(new SystemMessage(SystemMessageId.SLOTS_FULL));
				return;
			}
			
			if (isInvul() && !isGM())
			{
				sendPacket(ActionFailed.STATIC_PACKET);
				SystemMessage smsg = new SystemMessage(SystemMessageId.FAILED_TO_PICKUP_S1);
				smsg.addItemName(target.getItemId());
				sendPacket(smsg);
				smsg = null;
				return;
			}
			
			if (target.getOwnerId() != 0 && target.getOwnerId() != getObjectId() && !isInLooterParty(target.getOwnerId()))
			{
				sendPacket(ActionFailed.STATIC_PACKET);
				
				if (target.getItemId() == 57)
				{
					SystemMessage smsg = new SystemMessage(SystemMessageId.FAILED_TO_PICKUP_S1_ADENA);
					smsg.addNumber(target.getCount());
					sendPacket(smsg);
					smsg = null;
				}
				else if (target.getCount() > 1)
				{
					SystemMessage smsg = new SystemMessage(SystemMessageId.FAILED_TO_PICKUP_S2_S1_S);
					smsg.addItemName(target.getItemId());
					smsg.addNumber(target.getCount());
					sendPacket(smsg);
					smsg = null;
				}
				else
				{
					SystemMessage smsg = new SystemMessage(SystemMessageId.FAILED_TO_PICKUP_S1);
					smsg.addItemName(target.getItemId());
					sendPacket(smsg);
					smsg = null;
				}
				return;
			}
			
			if (target.getItemId() == 57 && _inventory.getAdena() == Integer.MAX_VALUE)
			{
				sendMessage("You have reached the maximum amount of adena, please spend or deposit the adena so you may continue obtaining adena.");
				return;
			}
			
			if (target.getItemLootShedule() != null && (target.getOwnerId() == getObjectId() || isInLooterParty(target.getOwnerId())))
			{
				target.resetOwnerTimer();
			}
			
			// Fixed it's not possible pick up the object if you exceed the maximum weight.
			if (_inventory.getTotalWeight() + target.getItem().getWeight() * target.getCount() > getMaxLoad())
			{
				sendMessage("You have reached the maximun weight.");
				return;
			}
			
			// Remove the L2ItemInstance from the world and send server->client GetItem packets
			target.pickupMe(this);
			
			if (Config.SAVE_DROPPED_ITEM)
			{
				ItemsOnGroundManager.getInstance().removeObject(target);
			}
		}
		
		// Auto use herbs - pick up
		if (target.getItemType() == L2EtcItemType.HERB)
		{
			IItemHandler handler = ItemHandler.getInstance().getItemHandler(target.getItemId());
			if (handler == null)
			{
				LOG.warn("No item handler registered for item ID " + target.getItemId() + ".");
			}
			else
			{
				handler.useItem(this, target);
			}
			
			ItemTable.getInstance().destroyItem("Consume", target, this, null);
		}
		
		// Cursed Weapons are not distributed
		else if (CursedWeaponsManager.getInstance().isCursed(target.getItemId()))
		{
			addItem("Pickup", target, null, true);
		}
		else if (FortSiegeManager.getInstance().isCombat(target.getItemId()))
		{
			addItem("Pickup", target, null, true);
		}
		else
		{
			// if item is instance of L2ArmorType or L2WeaponType broadcast an "Attention" system message
			if (target.getItemType() instanceof L2ArmorType || target.getItemType() instanceof L2WeaponType || target.getItem() instanceof L2Armor || target.getItem() instanceof L2Weapon)
			{
				if (target.getEnchantLevel() > 0)
				{
					SystemMessage msg = new SystemMessage(SystemMessageId.ATTENTION_S1_PICKED_UP_S2_S3);
					msg.addString(getName());
					msg.addNumber(target.getEnchantLevel());
					msg.addItemName(target.getItemId());
					broadcastPacket(msg, 1400);
				}
				else
				{
					SystemMessage msg = new SystemMessage(SystemMessageId.ATTENTION_S1_PICKED_UP_S2);
					msg.addString(getName());
					msg.addItemName(target.getItemId());
					broadcastPacket(msg, 1400);
				}
			}
			
			// Check if a Party is in progress
			if (isInParty())
			{
				getParty().distributeItem(this, target);
			}
			else if (target.getItemId() == 57 && getInventory().getAdenaInstance() != null)
			{
				addAdena("Pickup", target.getCount(), null, true);
				ItemTable.getInstance().destroyItem("Pickup", target, this, null);
			}
			// Target is regular item
			else
			{
				addItem("Pickup", target, null, true);
				
				// Like L2OFF Auto-Equip arrows if player has a bow and player picks up arrows.
				if (target.getItem() != null && target.getItem().getItemType() == L2EtcItemType.ARROW)
				{
					checkAndEquipArrows();
				}
			}
		}
	}
	
	/**
	 * Set a target.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Remove the L2PcInstance from the _statusListener of the old target if it was a L2Character</li>
	 * <li>Add the L2PcInstance to the _statusListener of the new target if it's a L2Character</li>
	 * <li>Target the new L2Object (add the target to the L2PcInstance _target, _knownObject and L2PcInstance to _KnownObject of the L2Object)</li><BR>
	 * <BR>
	 * @param newTarget The L2Object to target
	 */
	@Override
	public void setTarget(L2Object newTarget)
	{
		// Check if the new target is visible
		if (newTarget != null && !newTarget.isVisible())
		{
			newTarget = null;
		}
		
		L2Object oldTarget = getTarget();
		
		if (oldTarget != null)
		{
			if (oldTarget.equals(newTarget))
			{
				return; // no target change
			}
		}
		
		// Prevents /target exploiting
		if (newTarget != null)
		{
			if (!(newTarget instanceof L2PcInstance) || !isInParty() || !((L2PcInstance) newTarget).isInParty() || getParty().getPartyLeaderOID() != ((L2PcInstance) newTarget).getParty().getPartyLeaderOID())
			{
				if (Math.abs(newTarget.getZ() - getZ()) > Config.DIFFERENT_Z_NEW_MOVIE)
				{
					newTarget = null;
				}
			}
		}
		
		if (!isGM())
		{
			// Can't target and attack festival monsters if not participant
			if (newTarget instanceof L2FestivalMonsterInstance && !isFestivalParticipant())
			{
				newTarget = null;
			}
			else if (isInParty() && getParty().isInDimensionalRift())
			{
				byte riftType = getParty().getDimensionalRift().getType();
				byte riftRoom = getParty().getDimensionalRift().getCurrentRoom();
				
				if (newTarget != null && !DimensionalRiftManager.getInstance().getRoom(riftType, riftRoom).checkIfInZone(newTarget.getX(), newTarget.getY(), newTarget.getZ()))
				{
					newTarget = null;
				}
			}
		}
		
		if (oldTarget != null)
		{
			// Remove the L2PcInstance from the _statusListener of the old target if it was a L2Character
			if (oldTarget instanceof L2Character)
			{
				((L2Character) oldTarget).removeStatusListener(this);
			}
		}
		
		// Add the L2PcInstance to the _statusListener of the new target if it's a L2Character
		if (newTarget != null && newTarget instanceof L2Character)
		{
			((L2Character) newTarget).addStatusListener(this);
			TargetSelected my = new TargetSelected(getObjectId(), newTarget.getObjectId(), getX(), getY(), getZ());
			
			// Send packet just to me and to party, not to any other that does not use the information
			if (!isInParty())
			{
				sendPacket(my);
			}
			else
			{
				_party.broadcastToPartyMembers(my);
			}
			broadcastPacket(my);
		}
		
		if (newTarget == null && getTarget() != null)
		{
			broadcastPacket(new TargetUnselected(this));
		}
		
		// Target the new L2Object (add the target to the L2PcInstance _target, _knownObject and L2PcInstance to _KnownObject of the L2Object)
		super.setTarget(newTarget);
	}
	
	public FastList<L2Skill> BuffsList = new FastList<>();
	
	public void sellBuffsMenu(L2PcInstance buffer, int page)
	{
		L2Skill[] skills = buffer.getAllSkills();
		for (L2Skill s : skills)
		{
			if (s == null)
			{
				continue;
			}
			
			if (!buffer.BuffsList.contains(s) && !Config.LIST_PROHIBITED_BUFFS.contains(s.getId()) && s.getSkillType() == SkillType.BUFF && s.isActive())
			{
				buffer.BuffsList.add(s);
			}
		}
		
		final int MaxBuffsPerPage = 8;
		
		L2Skill[] BuffsListForPlayers = buffer.BuffsList.toArray(new L2Skill[buffer.BuffsList.size()]);
		
		int MaxPages = BuffsListForPlayers.length / MaxBuffsPerPage;
		
		if (BuffsListForPlayers.length > MaxBuffsPerPage * MaxPages)
		{
			MaxPages++;
		}
		
		// Check if number of users changed
		if (page > MaxPages)
		{
			page = MaxPages;
		}
		
		final int BuffsStart = MaxBuffsPerPage * page;
		int BuffsEnd = BuffsListForPlayers.length;
		
		if (BuffsEnd - BuffsStart > MaxBuffsPerPage)
		{
			BuffsEnd = BuffsStart + MaxBuffsPerPage;
		}
		
		TextBuilder tb = new TextBuilder();
		NpcHtmlMessage n = new NpcHtmlMessage(0);
		
		tb.append("<html><body>");
		tb.append("<center>Hello, <font color=LEVEL>" + getName() + "</font>, choose from: <font color=009900>" + buffer.BuffsList.size() + "</font> buffs.</center>");
		tb.append("<br1><center>Each price: <font color=LEVEL>" + buffer.getBuffPrize() + "</font> Adena</center>");
		tb.append("<center><table><tr>");
		
		for (int i = BuffsStart; i < BuffsEnd; i++)
		{
			int skillId = BuffsListForPlayers[i].getId();
			String BuffId = String.valueOf(BuffsListForPlayers[i].getId());
			
			if (skillId < 1000)
			{
				BuffId = "0" + BuffId;
			}
			
			if (skillId < 100)
			{
				BuffId = "0" + BuffId;
			}
			
			tb.append("<td><button action=\"bypass -h buff " + BuffsListForPlayers[i].getId() + "\" width=32 height=32 back=\"Icon.skill" + BuffId + "\" fore=\"Icon.skill" + BuffId + "\"></td>");
			tb.append("<td><button value=\"" + BuffsListForPlayers[i].getName() + " (Lv " + BuffsListForPlayers[i].getLevel() + ")\" action=\"bypass -h buff " + BuffsListForPlayers[i].getId() + " " + BuffsListForPlayers[i].getLevel() + " " + page
				+ "\" width=134 height=21 back=\"L2UI_ch3.BigButton3_over\" fore=\"L2UI_ch3.BigButton3\"></td>");
			tb.append("<td><button action=\"bypass -h buff " + BuffsListForPlayers[i].getId() + "\" width=32 height=32 back=\"Icon.skill" + BuffId + "\" fore=\"Icon.skill" + BuffId + "\"></td></tr><tr>");
		}
		
		tb.append("</tr></table>");
		
		tb.append("<table><tr>");
		for (int x = 0; x < MaxPages; x++)
		{
			final int pagenr = x + 1;
			if (page == x)
			{
				tb.append("<td width=20>[" + pagenr + "]&nbsp;&nbsp;</td>");
			}
			else
			{
				tb.append("<td width=20><a action=\"bypass -h buffspage " + x + "\">[" + pagenr + "]</a>&nbsp;&nbsp;</td>");
			}
		}
		tb.append("</tr></table></center></body></html>");
		
		n.setHtml(tb.toString());
		sendPacket(n);
	}
	
	/**
	 * Return the active weapon instance (always equiped in the right hand).<BR>
	 * <BR>
	 * @return the active weapon instance
	 */
	@Override
	public L2ItemInstance getActiveWeaponInstance()
	{
		return getInventory().getPaperdollItem(Inventory.PAPERDOLL_RHAND);
	}
	
	/**
	 * Return the active weapon item (always equiped in the right hand).<BR>
	 * <BR>
	 * @return the active weapon item
	 */
	@Override
	public L2Weapon getActiveWeaponItem()
	{
		L2ItemInstance weapon = getActiveWeaponInstance();
		
		if (weapon == null)
		{
			return getFistsWeaponItem();
		}
		
		return (L2Weapon) weapon.getItem();
	}
	
	/**
	 * Gets the chest armor instance.
	 * @return the chest armor instance
	 */
	public L2ItemInstance getChestArmorInstance()
	{
		return getInventory().getPaperdollItem(Inventory.PAPERDOLL_CHEST);
	}
	
	/**
	 * Gets the legs armor instance.
	 * @return the legs armor instance
	 */
	public L2ItemInstance getLegsArmorInstance()
	{
		return getInventory().getPaperdollItem(Inventory.PAPERDOLL_LEGS);
	}
	
	/**
	 * Gets the active chest armor item.
	 * @return the active chest armor item
	 */
	public L2Armor getActiveChestArmorItem()
	{
		L2ItemInstance armor = getChestArmorInstance();
		
		if (armor == null)
		{
			return null;
		}
		
		return (L2Armor) armor.getItem();
	}
	
	/**
	 * Gets the active legs armor item.
	 * @return the active legs armor item
	 */
	public L2Armor getActiveLegsArmorItem()
	{
		L2ItemInstance legs = getLegsArmorInstance();
		
		if (legs == null)
		{
			return null;
		}
		
		return (L2Armor) legs.getItem();
	}
	
	/**
	 * Checks if is wearing heavy armor.
	 * @return true, if is wearing heavy armor
	 */
	public boolean isWearingHeavyArmor()
	{
		L2ItemInstance legs = getLegsArmorInstance();
		L2ItemInstance armor = getChestArmorInstance();
		
		if (armor != null && legs != null)
		{
			if ((L2ArmorType) legs.getItemType() == L2ArmorType.HEAVY && ((L2ArmorType) armor.getItemType() == L2ArmorType.HEAVY))
			{
				return true;
			}
		}
		if (armor != null)
		{
			if ((getInventory().getPaperdollItem(Inventory.PAPERDOLL_CHEST).getItem().getBodyPart() == L2Item.SLOT_FULL_ARMOR && (L2ArmorType) armor.getItemType() == L2ArmorType.HEAVY))
			{
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Checks if is wearing light armor.
	 * @return true, if is wearing light armor
	 */
	public boolean isWearingLightArmor()
	{
		L2ItemInstance legs = getLegsArmorInstance();
		L2ItemInstance armor = getChestArmorInstance();
		
		if (armor != null && legs != null)
		{
			if ((L2ArmorType) legs.getItemType() == L2ArmorType.LIGHT && ((L2ArmorType) armor.getItemType() == L2ArmorType.LIGHT))
			{
				return true;
			}
		}
		if (armor != null)
		{
			if ((getInventory().getPaperdollItem(Inventory.PAPERDOLL_CHEST).getItem().getBodyPart() == L2Item.SLOT_FULL_ARMOR && (L2ArmorType) armor.getItemType() == L2ArmorType.LIGHT))
			{
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Checks if is wearing magic armor.
	 * @return true, if is wearing magic armor
	 */
	public boolean isWearingMagicArmor()
	{
		L2ItemInstance legs = getLegsArmorInstance();
		L2ItemInstance armor = getChestArmorInstance();
		
		if (armor != null && legs != null)
		{
			if ((L2ArmorType) legs.getItemType() == L2ArmorType.MAGIC && ((L2ArmorType) armor.getItemType() == L2ArmorType.MAGIC))
			{
				return true;
			}
		}
		if (armor != null)
		{
			if ((getInventory().getPaperdollItem(Inventory.PAPERDOLL_CHEST).getItem().getBodyPart() == L2Item.SLOT_FULL_ARMOR && (L2ArmorType) armor.getItemType() == L2ArmorType.MAGIC))
			{
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Checks if is wearing formal wear.
	 * @return true, if is wearing formal wear
	 */
	public boolean isWearingFormalWear()
	{
		return _IsWearingFormalWear;
	}
	
	/**
	 * Sets the checks if is wearing formal wear.
	 * @param value the new checks if is wearing formal wear
	 */
	public void setIsWearingFormalWear(boolean value)
	{
		_IsWearingFormalWear = value;
	}
	
	/**
	 * Checks if is married.
	 * @return true, if is married
	 */
	public boolean isMarried()
	{
		return _married;
	}
	
	/**
	 * Sets the married.
	 * @param state the new married
	 */
	public void setMarried(boolean state)
	{
		_married = state;
	}
	
	/**
	 * Married type.
	 * @return the int
	 */
	public int marriedType()
	{
		return _marriedType;
	}
	
	/**
	 * Sets the married type.
	 * @param type the new married type
	 */
	public void setmarriedType(int type)
	{
		_marriedType = type;
	}
	
	/**
	 * Checks if is engage request.
	 * @return true, if is engage request
	 */
	public boolean isEngageRequest()
	{
		return _engagerequest;
	}
	
	public void setEngageRequest(boolean state, int playerid)
	{
		_engagerequest = state;
		_engageid = playerid;
	}
	
	public void setMaryRequest(boolean state)
	{
		_marryrequest = state;
	}
	
	/**
	 * Checks if is mary request.
	 * @return true, if is mary request
	 */
	public boolean isMaryRequest()
	{
		return _marryrequest;
	}
	
	/**
	 * Sets the marry accepted.
	 * @param state the new marry accepted
	 */
	public void setMarryAccepted(boolean state)
	{
		_marryaccepted = state;
	}
	
	/**
	 * Checks if is marry accepted.
	 * @return true, if is marry accepted
	 */
	public boolean isMarryAccepted()
	{
		return _marryaccepted;
	}
	
	/**
	 * Gets the engage id.
	 * @return the engage id
	 */
	public int getEngageId()
	{
		return _engageid;
	}
	
	/**
	 * Gets the partner id.
	 * @return the partner id
	 */
	public int getPartnerId()
	{
		return _partnerId;
	}
	
	/**
	 * Sets the partner id.
	 * @param partnerid the new partner id
	 */
	public void setPartnerId(int partnerid)
	{
		_partnerId = partnerid;
	}
	
	/**
	 * Gets the couple id.
	 * @return the couple id
	 */
	public int getCoupleId()
	{
		return _coupleId;
	}
	
	/**
	 * Sets the couple id.
	 * @param coupleId the new couple id
	 */
	public void setCoupleId(int coupleId)
	{
		_coupleId = coupleId;
	}
	
	public void EngageAnswer(int answer)
	{
		if (!_engagerequest)
		{
			return;
		}
		else if (_engageid == 0)
		{
			return;
		}
		else
		{
			L2PcInstance ptarget = (L2PcInstance) L2World.getInstance().findObject(_engageid);
			setEngageRequest(false, 0);
			if (ptarget != null)
			{
				if (answer == 1)
				{
					CoupleManager.getInstance().createCouple(ptarget, L2PcInstance.this);
					ptarget.sendMessage("Request to Engage has been ACCEPTED.");
				}
				else
				{
					ptarget.sendMessage("Request to Engage has been DENIED.");
				}
			}
		}
	}
	
	/**
	 * Return the secondary weapon instance (always equiped in the left hand).<BR>
	 * <BR>
	 * @return the secondary weapon instance
	 */
	@Override
	public L2ItemInstance getSecondaryWeaponInstance()
	{
		return getInventory().getPaperdollItem(Inventory.PAPERDOLL_LHAND);
	}
	
	/**
	 * Return the secondary weapon item (always equiped in the left hand) or the fists weapon.<BR>
	 * <BR>
	 * @return the secondary weapon item
	 */
	@Override
	public L2Weapon getSecondaryWeaponItem()
	{
		L2ItemInstance weapon = getSecondaryWeaponInstance();
		
		if (weapon == null)
		{
			return getFistsWeaponItem();
		}
		
		L2Item item = weapon.getItem();
		
		if (item instanceof L2Weapon)
		{
			return (L2Weapon) item;
		}
		
		return null;
	}
	
	/**
	 * Kill the L2Character, Apply Death Penalty, Manage gain/loss Karma and Item Drop.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Reduce the Experience of the L2PcInstance in function of the calculated Death Penalty</li>
	 * <li>If necessary, unsummon the Pet of the killed L2PcInstance</li>
	 * <li>Manage Karma gain for attacker and Karam loss for the killed L2PcInstance</li>
	 * <li>If the killed L2PcInstance has Karma, manage Drop Item</li>
	 * <li>Kill the L2PcInstance</li><BR>
	 * <BR>
	 * @param killer the killer
	 * @return true, if successful
	 */
	@Override
	public boolean doDie(L2Character killer)
	{
		getAchievement().increase(AchType.DEATHS);
		
		ExRedSky packet = new ExRedSky(777);
		sendPacket(packet);
		PlaySound death_music = new PlaySound(1, "nade", 0, 0, 0, 0, 0);
		sendPacket(death_music);
		
		int x1, y1, z1;
		x1 = getX();
		y1 = getY();
		z1 = getZ();
		L2TownZone Town;
		Town = TownManager.getInstance().getTown(x1, y1, z1);
		if (Config.TW_RESS_ON_DIE)
		{
			if (Town != null && isinTownWar())
			{
				if (Town.getTownId() == Config.TW_TOWN_ID && !Config.TW_ALL_TOWNS)
				{
					reviveRequest(this, null, false);
				}
				else if (Config.TW_ALL_TOWNS)
				{
					reviveRequest(this, null, false);
				}
			}
		}
		
		if (!super.doDie(killer))
		{
			return false;
		}
		
		Castle castle = null;
		if (getClan() != null)
		{
			castle = CastleManager.getInstance().getCastleByOwner(getClan());
			if (castle != null)
			{
				castle.destroyClanGate();
			}
		}
		
		if (killer != null)
		{
			final L2PcInstance pk = killer.getActingPlayer();
			if (pk != null)
			{
				if (Config.ENABLE_PK_INFO)
				{
					doPkInfo(pk);
				}
				
				if (atEvent)
				{
					pk.kills.add(getName());
				}
				
				if (Config.ALLOW_HITMAN_GDE)
				{
					Hitman.getInstance().onDeath(pk, this);
				}
				
				if (_inEventTvT && pk._inEventTvT)
				{
					if (TvT.is_teleport() || TvT.is_started())
					{
						if (!(pk._teamNameTvT.equals(_teamNameTvT)))
						{
							PlaySound ps = new PlaySound(0, "ItemSound.quest_itemget", 1, getObjectId(), getX(), getY(), getZ());
							_countTvTdies++;
							pk._countTvTkills++;
							pk.setTitle("Kills: " + pk._countTvTkills);
							pk.sendPacket(ps);
							TvT.setTeamKillsCount(pk._teamNameTvT, TvT.teamKillsCount(pk._teamNameTvT) + 1);
							pk.broadcastUserInfo();
						}
						else
						{
							pk.sendMessage("You are a teamkiller !!! Teamkills not counting.");
						}
						sendMessage("You will be revived and teleported to team spot in " + Config.TVT_REVIVE_DELAY / 1000 + " seconds!");
						ThreadPoolManager.getInstance().scheduleGeneral(new Runnable()
						{
							@Override
							public void run()
							{
								teleToLocation(TvT._teamsX.get(TvT._teams.indexOf(_teamNameTvT)) + Rnd.get(201) - 100, TvT._teamsY.get(TvT._teams.indexOf(_teamNameTvT)) + Rnd.get(201) - 100, TvT._teamsZ.get(TvT._teams.indexOf(_teamNameTvT)), false);
								doRevive();
							}
						}, Config.TVT_REVIVE_DELAY);
					}
				}
				else if (_inEventTvT)
				{
					if (TvT.is_teleport() || TvT.is_started())
					{
						sendMessage("You will be revived and teleported to team spot in " + Config.TVT_REVIVE_DELAY / 1000 + " seconds!");
						ThreadPoolManager.getInstance().scheduleGeneral(new Runnable()
						{
							
							@Override
							public void run()
							{
								teleToLocation(TvT._teamsX.get(TvT._teams.indexOf(_teamNameTvT)), TvT._teamsY.get(TvT._teams.indexOf(_teamNameTvT)), TvT._teamsZ.get(TvT._teams.indexOf(_teamNameTvT)), false);
								doRevive();
								broadcastPacket(new SocialAction(getObjectId(), 15));
							}
						}, Config.TVT_REVIVE_DELAY);
					}
				}
				else if (_inEventCTF)
				{
					if (CTF.is_teleport() || CTF.is_started())
					{
						pk._countCTFkills++;
						
						PlaySound ps = new PlaySound(0, "ItemSound.quest_itemget", 1, getObjectId(), getX(), getY(), getZ());
						pk.setTitle("Kills: " + pk._countCTFkills);
						pk.sendPacket(ps);
						pk.broadcastUserInfo();
						
						sendMessage("You will be revived and teleported to team flag in 10 seconds!");
						if (_haveFlagCTF)
						{
							removeCTFFlagOnDie();
						}
						ThreadPoolManager.getInstance().scheduleGeneral(new Runnable()
						{
							@Override
							public void run()
							{
								teleToLocation(CTF._teamsX.get(CTF._teams.indexOf(_teamNameCTF)), CTF._teamsY.get(CTF._teams.indexOf(_teamNameCTF)), CTF._teamsZ.get(CTF._teams.indexOf(_teamNameCTF)), false);
								doRevive();
							}
						}, 10000);
					}
				}
				else if (_inEventDM && pk._inEventDM)
				{
					if (DM.is_teleport() || DM.is_started())
					{
						pk._countDMkills++;
						PlaySound ps = new PlaySound(0, "ItemSound.quest_itemget", 1, getObjectId(), getX(), getY(), getZ());
						pk.setTitle("Kills: " + pk._countDMkills);
						pk.sendPacket(ps);
						pk.broadcastUserInfo();
						
						if (Config.DM_ENABLE_KILL_REWARD)
						{
							
							L2Item reward = ItemTable.getInstance().getTemplate(Config.DM_KILL_REWARD_ID);
							pk.getInventory().addItem("DM Kill Reward", Config.DM_KILL_REWARD_ID, Config.DM_KILL_REWARD_AMOUNT, this, null);
							pk.sendMessage("You have earned " + Config.DM_KILL_REWARD_AMOUNT + " item(s) of ID " + reward.getName() + ".");
							
						}
						
						sendMessage("You will be revived and teleported to spot in 20 seconds!");
						ThreadPoolManager.getInstance().scheduleGeneral(new Runnable()
						{
							
							@Override
							public void run()
							{
								Location p_loc = DM.get_playersSpawnLocation();
								teleToLocation(p_loc._x, p_loc._y, p_loc._z, false);
								doRevive();
							}
						}, Config.DM_REVIVE_DELAY);
					}
				}
				else if (_inEventDM)
				{
					if (DM.is_teleport() || DM.is_started())
					{
						sendMessage("You will be revived and teleported to spot in 10 seconds!");
						ThreadPoolManager.getInstance().scheduleGeneral(new Runnable()
						{
							@Override
							public void run()
							{
								Location players_loc = DM.get_playersSpawnLocation();
								teleToLocation(players_loc._x, players_loc._y, players_loc._z, false);
								doRevive();
							}
						}, 10000);
					}
				}
				else if (_inEventVIP && VIP._started)
				{
					if (_isTheVIP && !pk._inEventVIP)
					{
						Announcements.getInstance().announceToAll("VIP Killed by non-event character. VIP going back to initial spawn.");
						doRevive();
						teleToLocation(VIP._startX, VIP._startY, VIP._startZ);
						
					}
					else
					{
						if (_isTheVIP && pk._inEventVIP)
						{
							VIP.vipDied();
						}
						else
						{
							sendMessage("You will be revived and teleported to team spot in 20 seconds!");
							ThreadPoolManager.getInstance().scheduleGeneral(new Runnable()
							{
								
								@Override
								public void run()
								{
									doRevive();
									if (_isVIP)
									{
										teleToLocation(VIP._startX, VIP._startY, VIP._startZ);
									}
									else
									{
										teleToLocation(VIP._endX, VIP._endY, VIP._endZ);
									}
								}
							}, 20000);
						}
						
					}
					broadcastUserInfo();
				}
			}
			
			// Clear resurrect xp calculation
			setExpBeforeDeath(0);
			
			if (isCursedWeaponEquiped())
			{
				CursedWeaponsManager.getInstance().drop(_cursedWeaponEquipedId, killer);
			}
			else
			{
				if (pk == null || !pk.isCursedWeaponEquiped())
				{
					onDieDropItem(killer); // Check if any item should be dropped
					
					if (!(isInsideZone(ZoneId.ZONE_PVP) && !isInsideZone(ZoneId.ZONE_SIEGE)))
					{
						if ((pk != null) && pk.getClan() != null && getClan() != null && !isAcademyMember() && !pk.isAcademyMember() && _clan.isAtWarWith(pk.getClanId()) && pk.getClan().isAtWarWith(_clan.getClanId()))
						{
							if (killer instanceof L2PcInstance && getClan().getReputationScore() > 0)
							{
								pk.getClan().setReputationScore(((L2PcInstance) killer).getClan().getReputationScore() + 2, true);
								pk.getClan().broadcastToOnlineMembers(new PledgeShowInfoUpdate(pk.getClan())); // Update
																												// status
																												// to
																												// all
																												// members
							}
							
							if (pk.getClan().getReputationScore() > 0)
							{
								_clan.setReputationScore(_clan.getReputationScore() - 2, true);
								_clan.broadcastToOnlineMembers(new PledgeShowInfoUpdate(_clan)); // Update status to all members
							}
						}
						
						if (Config.ALT_GAME_DELEVEL)
						{
							// Reduce the Experience of the L2PcInstance in function of the calculated Death Penalty
							// NOTE: deathPenalty +- Exp will update karma
							if (!isLucky())
							{
								deathPenalty((pk != null && getClan() != null && pk.getClan() != null && pk.getClan().isAtWarWith(getClanId())));
							}
						}
						else
						{
							onDieUpdateKarma(); // Update karma if delevel is not allowed
						}
					}
				}
			}
		}
		
		// Unsummon Cubics
		unsummonAllCubics();
		
		if (_forceBuff != null)
		{
			abortCast();
		}
		
		for (
		
		L2Character character :
		
		getKnownList().getKnownCharacters())
		{
			if (character.getTarget() == this)
			{
				if (character.isCastingNow())
				{
					character.abortCast();
				}
			}
		}
		
		if (isInParty() && getParty().isInDimensionalRift())
		{
			getParty().getDimensionalRift().getDeadMemberList().add(this);
		}
		
		// calculate death penalty buff
		if (!isPhantom())
		{
			calculateDeathPenaltyBuffLevel(killer);
		}
		
		stopRentPet();
		stopWaterTask();
		quakeSystem = 0;
		
		// leave war legend aura if enabled
		heroConsecutiveKillCount = 0;
		if (Config.WAR_LEGEND_AURA && !_hero && isPVPHero)
		{
			setHeroAura(false);
			this.sendMessage("You leaved War Legend State");
		}
		
		// Refresh focus force like L2OFF
		sendPacket(new EtcStatusUpdate(this));
		
		// After dead mob check if the killer got a moving task actived
		if (killer instanceof L2PcInstance)
		{
			if (((L2PcInstance) killer).isMovingTaskDefined())
			{
				((L2PcInstance) killer).startMovingTask();
			}
		}
		
		return true;
	}
	
	public void removeCTFFlagOnDie()
	{
		CTF._flagsTaken.set(CTF._teams.indexOf(_teamNameHaveFlagCTF), false);
		CTF.spawnFlag(_teamNameHaveFlagCTF);
		CTF.removeFlagFromPlayer(this);
		broadcastUserInfo();
		_haveFlagCTF = false;
		Announcements.getInstance().gameAnnounceToAll(CTF.get_eventName() + "(CTF): " + _teamNameHaveFlagCTF + "'s flag returned.");
	}
	
	private void onDieDropItem(L2Character killer)
	{
		if (atEvent || (TvT.is_started() && _inEventTvT) || (DM.is_started() && _inEventDM) || (CTF.is_started() && _inEventCTF) || (VIP._started && _inEventVIP) || killer == null)
		{
			return;
		}
		
		if (getKarma() <= 0 && killer instanceof L2PcInstance && ((L2PcInstance) killer).getClan() != null && getClan() != null && ((L2PcInstance) killer).getClan().isAtWarWith(getClanId()))
		{
			return;
		}
		
		if (!isInsideZone(ZoneId.ZONE_PVP) && (!isGM() || Config.KARMA_DROP_GM))
		{
			boolean isKarmaDrop = false;
			boolean isKillerNpc = killer instanceof L2NpcInstance;
			int pkLimit = Config.KARMA_PK_LIMIT;
			
			int dropEquip = 0;
			int dropEquipWeapon = 0;
			int dropItem = 0;
			int dropLimit = 0;
			int dropPercent = 0;
			
			if (getKarma() > 0 && getPkKills() >= pkLimit)
			{
				isKarmaDrop = true;
				dropPercent = Config.KARMA_RATE_DROP;
				dropEquip = Config.KARMA_RATE_DROP_EQUIP;
				dropEquipWeapon = Config.KARMA_RATE_DROP_EQUIP_WEAPON;
				dropItem = Config.KARMA_RATE_DROP_ITEM;
				dropLimit = Config.KARMA_DROP_LIMIT;
			}
			else if (isKillerNpc && getLevel() > 4 && !isFestivalParticipant())
			{
				dropPercent = Config.PLAYER_RATE_DROP;
				dropEquip = Config.PLAYER_RATE_DROP_EQUIP;
				dropEquipWeapon = Config.PLAYER_RATE_DROP_EQUIP_WEAPON;
				dropItem = Config.PLAYER_RATE_DROP_ITEM;
				dropLimit = Config.PLAYER_DROP_LIMIT;
			}
			
			int dropCount = 0;
			while (dropPercent > 0 && Rnd.get(100) < dropPercent && dropCount < dropLimit)
			{
				int itemDropPercent = 0;
				List<Integer> nonDroppableList = new FastList<>();
				List<Integer> nonDroppableListPet = new FastList<>();
				
				nonDroppableList = Config.KARMA_LIST_NONDROPPABLE_ITEMS;
				nonDroppableListPet = Config.KARMA_LIST_NONDROPPABLE_ITEMS;
				
				for (L2ItemInstance itemDrop : getInventory().getItems())
				{
					// Don't drop
					if (itemDrop.isAugmented() || // Dont drop augmented items
						itemDrop.isShadowItem() || // Dont drop Shadow Items
						itemDrop.getItemId() == 57 || // Adena
						itemDrop.getItem().getType2() == L2Item.TYPE2_QUEST || // Quest Items
						nonDroppableList.contains(itemDrop.getItemId()) || // Item listed in the non droppable item list
						nonDroppableListPet.contains(itemDrop.getItemId()) || // Item listed in the non droppable pet item list
						getPet() != null && getPet().getControlItemId() == itemDrop.getItemId() // Control Item of active pet
					)
					{
						continue;
					}
					
					if (itemDrop.isEquipped())
					{
						// Set proper chance according to Item type of equipped Item
						itemDropPercent = itemDrop.getItem().getType2() == L2Item.TYPE2_WEAPON ? dropEquipWeapon : dropEquip;
						getInventory().unEquipItemInSlotAndRecord(itemDrop.getEquipSlot());
					}
					else
					{
						itemDropPercent = dropItem; // Item in inventory
					}
					
					// NOTE: Each time an item is dropped, the chance of another item being dropped gets lesser (dropCount * 2)
					if (Rnd.get(100) < itemDropPercent)
					{
						if (isKarmaDrop)
						{
							dropItem("DieDrop", itemDrop, killer, true, false);
							String text = getName() + " has karma and dropped id = " + itemDrop.getItemId() + ", count = " + itemDrop.getCount();
							Log.add(text, "karma_dieDrop");
						}
						else
						{
							dropItem("DieDrop", itemDrop, killer, true, true);
							String text = getName() + " dropped id = " + itemDrop.getItemId() + ", count = " + itemDrop.getCount();
							Log.add(text, "dieDrop");
						}
						
						dropCount++;
						break;
					}
				}
			}
		}
	}
	
	private void onDieUpdateKarma()
	{
		// Karma lose for server that does not allow delevel
		if (getKarma() > 0)
		{
			double karmaLost = Config.KARMA_LOST_BASE;
			karmaLost *= getLevel(); // multiply by char lvl
			karmaLost *= getLevel() / 100.0; // divide by 0.charLVL
			karmaLost = Math.round(karmaLost);
			if (karmaLost < 0)
			{
				karmaLost = 1;
			}
			
			// Decrease Karma of the L2PcInstance and Send it a Server->Client StatusUpdate packet with Karma and PvP Flag if necessary
			setKarma(getKarma() - (int) karmaLost);
		}
	}
	
	public void onKillUpdatePvPKarma(L2Character target)
	{
		if (target == null)
		{
			return;
		}
		
		if (!(target instanceof L2PlayableInstance))
		{
			return;
		}
		
		if ((_inEventCTF && CTF.is_started()) || (_inEventTvT && TvT.is_started()) || (_inEventVIP && VIP._started) || (_inEventDM && DM.is_started()))
		{
			return;
		}
		
		if (isCursedWeaponEquipped())
		{
			CursedWeaponsManager.getInstance().increaseKills(_cursedWeaponEquipedId);
			return;
		}
		
		L2PcInstance targetPlayer = null;
		
		if (target instanceof L2PcInstance)
		{
			targetPlayer = (L2PcInstance) target;
		}
		else if (target instanceof L2Summon)
		{
			targetPlayer = ((L2Summon) target).getOwner();
		}
		
		if (targetPlayer == null)
		{
			return; // Target player is null
		}
		
		if (targetPlayer == this)
		{
			targetPlayer = null;
			return; // Target player is self
		}
		
		if (isCursedWeaponEquiped())
		{
			CursedWeaponsManager.getInstance().increaseKills(_cursedWeaponEquipedId);
			return;
		}
		
		// If in duel and you kill (only can kill l2summon), do nothing
		if (isInDuel() && targetPlayer.isInDuel())
		{
			return;
		}
		
		// Tow War reward
		if (isinTownWar())
		{
			int x, y, z;
			x = getX();
			y = getY();
			z = getZ();
			L2TownZone Town;
			Town = TownManager.getInstance().getTown(x, y, z);
			if (Town != null)
			{
				if (Town.getTownId() == Config.TW_TOWN_ID && !Config.TW_ALL_TOWNS)
				{
					getInventory().addItem("TownWar", Config.TW_ITEM_ID, Config.TW_ITEM_AMOUNT, this, this);
					sendMessage("You received your prize for a town war kill!");
				}
				else if (Config.TW_ALL_TOWNS)
				{
					getInventory().addItem("TownWar", Config.TW_ITEM_ID, Config.TW_ITEM_AMOUNT, this, this);
					sendMessage("You received your prize for a town war kill!");
				}
			}
		}
		
		// If in Arena, do nothing
		if (isInsideZone(ZoneId.ZONE_PVP) || targetPlayer.isInsideZone(ZoneId.ZONE_PVP))
		{
			return;
		}
		
		// check anti-farm
		if (!checkAntiFarm(targetPlayer))
		{
			return;
		}
		
		if (Config.ANTI_FARM_SUMMON)
		{
			if (target instanceof L2SummonInstance)
			{
				return;
			}
		}
		
		// Check if it's pvp
		if (checkIfPvP(target))
		{
			if (target instanceof L2PcInstance && Config.ANNOUNCE_PVP_KILL)
			{
				SystemMessage sm = new SystemMessage(SystemMessageId.S1_HUNTED_PLAYER_S2_IN_S3);
				sm.addString(getName());
				sm.addString(target.getName());
				sm.addZoneName(getX(), getY(), getZ()); // Region Name
				for (L2PcInstance player : L2World.getInstance().getAllPlayers().values())
				{
					if (player == null)
					{
						continue;
					}
					
					player.sendPacket(sm);
				}
			}
			
			increasePvpKills();
		}
		else
		{
			// check about wars
			if (targetPlayer.getClan() != null && getClan() != null)
			{
				if (getClan().isAtWarWith(targetPlayer.getClanId()))
				{
					if (targetPlayer.getClan().isAtWarWith(getClanId()))
					{
						// 'Both way war' -> 'PvP Kill'
						increasePvpKills();
						
						if (Config.CLAN_WAR_REWARD)
						{
							addItem("AutoLoot", 6393, 1, null, true);
						}
						
						addItemReward(targetPlayer);
						return;
					}
				}
			}
			
			// 'No war' or 'One way war' -> 'Normal PK'
			if (!(_inEventTvT && TvT.is_started()) || !(_inEventCTF && CTF.is_started()) || !(_inEventVIP && VIP._started) || !(_inEventDM && DM.is_started()))
			{
				if (targetPlayer.getKarma() > 0) // Target player has karma
				{
					if (Config.KARMA_AWARD_PK_KILL)
					{
						increasePvpKills();
					}
				}
				else if (targetPlayer.getPvpFlag() == 0) // Target player doesn't have karma
				{
					if (target instanceof L2PcInstance && Config.ANNOUNCE_PK_KILL)
					{
						SystemMessage sm = new SystemMessage(SystemMessageId.S1_ASSASSINATED_PLAYER_S2_IN_S3);
						sm.addString(getName());
						sm.addString(target.getName());
						sm.addZoneName(getX(), getY(), getZ()); // Region Name
						for (L2PcInstance player : L2World.getInstance().getAllPlayers().values())
						{
							if (player == null)
							{
								continue;
							}
							
							player.sendPacket(sm);
						}
					}
					
					increasePkKillsAndKarma(target, targetPlayer.getLevel());
				}
				if (Config.ALLOW_HITMAN_GDE && Config.HITMAN_GDE_TAKE_KARMA && Hitman.getInstance().exists(targetPlayer.getObjectId()))
				{
					return;
				}
			}
		}
		
		if (_inEventDM && DM.is_started())
		{
			return;
		}
		
		if (targetPlayer.getObjectId() == _lastKill)
		{
			count += 1;
		}
		else
		{
			count = 1;
			_lastKill = targetPlayer.getObjectId();
		}
		
		if (Config.REWARD_PROTECT == 0 || count <= Config.REWARD_PROTECT)
		{
			addItemReward(targetPlayer);
		}
	}
	
	/**
	 * Check anti farm.
	 * @param targetPlayer the target player
	 * @return true, if successful
	 */
	public boolean checkAntiFarm(L2PcInstance targetPlayer)
	{
		
		if (Config.ANTI_FARM_ENABLED)
		{
			// Anti FARM Clan - Ally
			if (Config.ANTI_FARM_CLAN_ALLY_ENABLED && ((getClanId() > 0 && targetPlayer.getClanId() > 0 && getClanId() == targetPlayer.getClanId()) || (getAllyId() > 0 && targetPlayer.getAllyId() > 0 && getAllyId() == targetPlayer.getAllyId())))
			{
				this.sendMessage("Stop FARM! Farm is punishable with a Ban! Admin informed.");
				LOG.warn("PVP POINT FARM ATTEMPT, " + this.getName() + " and " + targetPlayer.getName() + ". CLAN or ALLY.");
				return false;
			}
			
			if (Config.ANTI_FARM_LVL_DIFF_ENABLED && (getLevel() > targetPlayer.getLevel() + Config.ANTI_FARM_MAX_LVL_DIFF || getLevel() < targetPlayer.getLevel() - Config.ANTI_FARM_MAX_LVL_DIFF))
			{
				sendMessage("You can't attack a player if level difference is bigger than: " + Config.ANTI_FARM_MAX_LVL_DIFF + "Lv.");
				return false;
			}
			
			// Anti FARM pdef < 300
			if (Config.ANTI_FARM_PDEF_DIFF_ENABLED && targetPlayer.getPDef(targetPlayer) < Config.ANTI_FARM_MAX_PDEF_DIFF)
			{
				this.sendMessage("Stop FARM! Farm is punishable with a Ban! Admin informed.");
				LOG.warn("PVP POINT FARM ATTEMPT, " + this.getName() + " and " + targetPlayer.getName() + ". MAX PDEF DIFF.");
				return false;
			}
			
			// Anti FARM p atk < 300
			if (Config.ANTI_FARM_PATK_DIFF_ENABLED && targetPlayer.getPAtk(targetPlayer) < Config.ANTI_FARM_MAX_PATK_DIFF)
			{
				this.sendMessage("Stop FARM! Farm is punishable with a Ban! Admin informed.");
				LOG.warn("PVP POINT FARM ATTEMPT, " + this.getName() + " and " + targetPlayer.getName() + ". MAX PATK DIFF.");
				return false;
			}
			
			// Anti FARM Party
			if (Config.ANTI_FARM_PARTY_ENABLED && this.getParty() != null && targetPlayer.getParty() != null && this.getParty().equals(targetPlayer.getParty()))
			{
				this.sendMessage("Stop FARM! Farm is punishable with a Ban! Admin informed.");
				LOG.warn("PVP POINT FARM ATTEMPT, " + this.getName() + " and " + targetPlayer.getName() + ". SAME PARTY.");
				return false;
			}
			
			// Anti FARM same Ip
			if (Config.ANTI_FARM_IP_ENABLED)
			{
				
				if (this.getClient() != null && targetPlayer.getClient() != null)
				{
					String ip1 = this.getClient().getConnection().getInetAddress().getHostAddress();
					String ip2 = targetPlayer.getClient().getConnection().getInetAddress().getHostAddress();
					
					if (ip1.equals(ip2))
					{
						this.sendMessage("Stop FARM! Farm is punishable with a Ban! Admin informed.");
						LOG.warn("PVP POINT FARM ATTEMPT: " + this.getName() + " and " + targetPlayer.getName() + ". SAME IP.");
						return false;
					}
				}
			}
			return true;
		}
		return true;
	}
	
	/**
	 * Adds the item reword.
	 * @param targetPlayer the target player
	 */
	private void addItemReward(L2PcInstance targetPlayer)
	{
		// IP check
		if (targetPlayer.getClient() != null && targetPlayer.getClient().getConnection() != null)
		{
			if (targetPlayer.getClient().getConnection().getInetAddress() != getClient().getConnection().getInetAddress())
			{
				
				if (targetPlayer.getKarma() > 0 || targetPlayer.getPvpFlag() > 0) // killing target pk or in pvp
				{
					if (Config.PVP_REWARD_ENABLED)
					{
						int item = Config.PVP_REWARD_ID;
						L2Item reward = ItemTable.getInstance().getTemplate(item);
						
						int amount = Config.PVP_REWARD_AMOUNT;
						
						getInventory().addItem("Winning PvP", Config.PVP_REWARD_ID, Config.PVP_REWARD_AMOUNT, this, null);
						sendMessage("You have earned " + amount + " item(s) of " + reward.getName() + ".");
					}
					
					if (!Config.FORCE_INVENTORY_UPDATE)
					{
						InventoryUpdate iu = new InventoryUpdate();
						iu.addItem(_inventory.getItemByItemId(Config.PVP_REWARD_ID));
						sendPacket(iu);
						iu = null;
					}
				}
				else
				// target is not pk and not in pvp ---> PK KILL
				{
					if (Config.PK_REWARD_ENABLED)
					{
						int item = Config.PK_REWARD_ID;
						L2Item reward = ItemTable.getInstance().getTemplate(item);
						int amount = Config.PK_REWARD_AMOUNT;
						getInventory().addItem("Winning PK", Config.PK_REWARD_ID, Config.PK_REWARD_AMOUNT, this, null);
						sendMessage("You have earned " + amount + " item(s) of " + reward.getName() + ".");
					}
					
					if (!Config.FORCE_INVENTORY_UPDATE)
					{
						InventoryUpdate iu = new InventoryUpdate();
						iu.addItem(_inventory.getItemByItemId(Config.PK_REWARD_ID));
						sendPacket(iu);
						iu = null;
					}
				}
			}
			else
			{
				this.sendMessage("Farm is punishable with Ban! Don't kill your Box!");
				LOG.warn("PVP POINT FARM ATTEMPT: " + this.getName() + " and " + targetPlayer.getName() + ". SAME IP.");
			}
		}
	}
	
	/**
	 * Increase the pvp kills count and send the info to the player.
	 */
	public void increasePvpKills()
	{
		// Add karma to attacker and increase its PK counter
		setPvpKills(getPvpKills() + 1);
		getAchievement().increase(AchType.PVP);
		
		// Increase the kill count for a special hero aura
		heroConsecutiveKillCount++;
		
		// If heroConsecutiveKillCount == 30 give hero aura
		if (heroConsecutiveKillCount == Config.KILLS_TO_GET_WAR_LEGEND_AURA && Config.WAR_LEGEND_AURA)
		{
			setHeroAura(true);
			Announcements.getInstance().gameAnnounceToAll(getName() + " becames War Legend with " + Config.KILLS_TO_GET_WAR_LEGEND_AURA + " PvP!!");
			
		}
		if (Config.PVPEXPSP_SYSTEM)
		{
			addExpAndSp(Config.ADD_EXP, Config.ADD_SP);
			{
				sendMessage("Earned Exp & SP for a pvp kill");
			}
		}
		
		if (getTitleOn())
		{
			updateTitle();
		}
		
		// Update the character's name color if they reached any of the 5 PvP levels.
		updatePvPColor(getPvpKills());
		broadcastUserInfo();
		
		if (Config.ALLOW_QUAKE_SYSTEM)
		{
			QuakeSystem();
		}
		
		// Send a Server->Client UserInfo packet to attacker with its Karma and PK Counter
		sendPacket(new UserInfo(this));
	}
	
	/**
	 * Quake system.
	 */
	public void QuakeSystem()
	{
		quakeSystem++;
		switch (quakeSystem)
		{
			case 3:
				Announcements.getInstance().pvpAnnounceToAll("" + getName() + " is Dominating! Killed 3 times in a row."); // 8D
				ExShowScreenMessage txt = new ExShowScreenMessage(1, 3, 1, false, 0, 0, 0, false, 3000, true, "" + getName() + " is Dominating!");
				PlaySound pvpmusic = new PlaySound(3, "dominating", 0, 0, 0, 0, 0);
				for (L2PcInstance player : L2World.getInstance().getAllPlayers().values())
				{
					if (player != null && player.isOnline() != 0 && (getScreentxt()))
					{
						player.sendPacket(txt);
						player.sendPacket(pvpmusic);
					}
				}
				break;
			case 5:
				Announcements.getInstance().pvpAnnounceToAll("" + getName() + " is on a Rampage! Killed 5 times in a row."); // 8D
				ExShowScreenMessage txt2 = new ExShowScreenMessage(1, 3, 1, false, 0, 0, 0, false, 3000, true, "" + getName() + " is on a Rampage!");
				PlaySound pvpmusic2 = new PlaySound(3, "rampage", 0, 0, 0, 0, 0);
				for (L2PcInstance player : L2World.getInstance().getAllPlayers().values())
				{
					if (player != null && player.isOnline() != 0 && (getScreentxt()))
					{
						player.sendPacket(txt2);
						player.sendPacket(pvpmusic2);
					}
				}
				break;
			case 7:
				Announcements.getInstance().pvpAnnounceToAll("" + getName() + " is on a Killing Spree! Killed 7 times in a row."); // 8D
				ExShowScreenMessage txt3 = new ExShowScreenMessage(1, 3, 1, false, 0, 0, 0, false, 3000, true, "" + getName() + " is on a Killing Spree!");
				PlaySound pvpmusic3 = new PlaySound(3, "killingspree", 0, 0, 0, 0, 0);
				for (L2PcInstance player : L2World.getInstance().getAllPlayers().values())
				{
					if (player != null && player.isOnline() != 0 && (getScreentxt()))
					{
						player.sendPacket(txt3);
						player.sendPacket(pvpmusic3);
					}
				}
				break;
			case 9:
				Announcements.getInstance().pvpAnnounceToAll("" + getName() + " is on a Monster Kill! Killed 9 times in a row."); // 8D
				ExShowScreenMessage txt4 = new ExShowScreenMessage(1, 3, 1, false, 0, 0, 0, false, 3000, true, "" + getName() + " is on a Monster Kill!");
				PlaySound pvpmusic4 = new PlaySound(3, "monsterkill", 0, 0, 0, 0, 0);
				for (L2PcInstance player : L2World.getInstance().getAllPlayers().values())
				{
					if (player != null && player.isOnline() != 0 && (getScreentxt()))
					{
						player.sendPacket(txt4);
						player.sendPacket(pvpmusic4);
					}
				}
				break;
			case 11:
				Announcements.getInstance().pvpAnnounceToAll("" + getName() + " is Unstoppable! Killed 11 times in a row."); // 8D
				ExShowScreenMessage txt5 = new ExShowScreenMessage(1, 3, 1, false, 0, 0, 0, false, 3000, true, "" + getName() + " is Unstoppable!");
				PlaySound pvpmusic5 = new PlaySound(3, "unstoppable", 0, 0, 0, 0, 0);
				for (L2PcInstance player : L2World.getInstance().getAllPlayers().values())
				{
					if (player != null && player.isOnline() != 0 && (getScreentxt()))
					{
						player.sendPacket(txt5);
						player.sendPacket(pvpmusic5);
					}
				}
				break;
			case 13:
				Announcements.getInstance().pvpAnnounceToAll("" + getName() + " is on Ultra Kill! Killed 13 times in a row."); // 8D
				ExShowScreenMessage txt6 = new ExShowScreenMessage(1, 3, 1, false, 0, 0, 0, false, 3000, true, "" + getName() + " is on Ultra Kill!");
				PlaySound pvpmusic6 = new PlaySound(3, "ultrakill", 0, 0, 0, 0, 0);
				for (L2PcInstance player : L2World.getInstance().getAllPlayers().values())
				{
					if (player != null && player.isOnline() != 0 && (getScreentxt()))
					{
						player.sendPacket(txt6);
						player.sendPacket(pvpmusic6);
					}
				}
				break;
			case 15:
				Announcements.getInstance().pvpAnnounceToAll("" + getName() + " is Godlike! Killed 15 times in a row."); // 8D
				ExShowScreenMessage txt7 = new ExShowScreenMessage(1, 3, 1, false, 0, 0, 0, false, 3000, true, "" + getName() + " is Godlike!");
				PlaySound pvpmusic7 = new PlaySound(3, "godlike", 0, 0, 0, 0, 0);
				for (L2PcInstance player : L2World.getInstance().getAllPlayers().values())
				{
					if (player != null && player.isOnline() != 0 && (getScreentxt()))
					{
						player.sendPacket(txt7);
						player.sendPacket(pvpmusic7);
					}
				}
				break;
			case 17:
				Announcements.getInstance().pvpAnnounceToAll("" + getName() + " is Wicked Sick! Killed 17 times in a row."); // 8D
				ExShowScreenMessage txt8 = new ExShowScreenMessage(1, 3, 1, false, 0, 0, 0, false, 3000, true, "" + getName() + " is Wicked Sick!");
				PlaySound pvpmusic8 = new PlaySound(3, "whickedsick", 0, 0, 0, 0, 0);
				for (L2PcInstance player : L2World.getInstance().getAllPlayers().values())
				{
					if (player != null && player.isOnline() != 0 && (getScreentxt()))
					{
						player.sendPacket(txt8);
						player.sendPacket(pvpmusic8);
					}
				}
				break;
			case 19:
				Announcements.getInstance().pvpAnnounceToAll("" + getName() + " is on a Ludricrous Kill! Killed 19 times in a row."); // 8D
				ExShowScreenMessage txt9 = new ExShowScreenMessage(1, 3, 1, false, 0, 0, 0, false, 3000, true, "" + getName() + " is on a Ludricrous Kill!");
				PlaySound pvpmusic9 = new PlaySound(3, "ludicrouskill", 0, 0, 0, 0, 0);
				for (L2PcInstance player : L2World.getInstance().getAllPlayers().values())
				{
					if (player != null && player.isOnline() != 0 && (getScreentxt()))
					{
						player.sendPacket(txt9);
						player.sendPacket(pvpmusic9);
					}
				}
				break;
			case 21:
				Announcements.getInstance().pvpAnnounceToAll("" + getName() + " is on Holy Shit! Killed 22 times in a row."); // 8D
				ExShowScreenMessage txt10 = new ExShowScreenMessage(1, 3, 1, false, 0, 0, 0, false, 3000, true, "" + getName() + " is on Holy Shit!");
				PlaySound pvpmusic10 = new PlaySound(3, "holyshit", 0, 0, 0, 0, 0);
				for (L2PcInstance player : L2World.getInstance().getAllPlayers().values())
				{
					if (player != null && player.isOnline() != 0 && (getScreentxt()))
					{
						player.sendPacket(txt10);
						player.sendPacket(pvpmusic10);
					}
				}
		}
	}
	
	/**
	 * Get info on pk's from pk table.
	 * @param PlayerWhoKilled the player who killed
	 */
	public void doPkInfo(L2PcInstance PlayerWhoKilled)
	{
		String killer = PlayerWhoKilled.getName();
		String killed = getName();
		int kills = 0;
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT kills FROM pkkills WHERE killerId=? AND killedId=?");
			statement.setString(1, killer);
			statement.setString(2, killed);
			final ResultSet rset = statement.executeQuery();
			if (rset.next())
			{
				kills = rset.getInt("kills");
			}
			rset.close();
			statement.close();
		}
		catch (Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
		}
		finally
		{
			CloseUtil.close(con);
		}
		
		if (kills >= 1)
		{
			kills++;
			String UPDATE_PKKILLS = "UPDATE pkkills SET kills=? WHERE killerId=? AND killedId=?";
			Connection conect = null;
			try
			{
				conect = L2DatabaseFactory.getInstance().getConnection();
				PreparedStatement statement = conect.prepareStatement(UPDATE_PKKILLS);
				statement.setInt(1, kills);
				statement.setString(2, killer);
				statement.setString(3, killed);
				statement.execute();
				statement.close();
			}
			catch (Exception e)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
				{
					LOG.error("Could not update pkKills:");
					e.printStackTrace();
				}
			}
			finally
			{
				CloseUtil.close(conect);
			}
			sendMessage("You have been killed " + kills + " times by " + PlayerWhoKilled.getName() + ".");
			PlayerWhoKilled.sendMessage("You have killed " + getName() + " " + kills + " times.");
		}
		else
		{
			String ADD_PKKILLS = "INSERT INTO pkkills (killerId,killedId,kills) VALUES (?,?,?)";
			Connection conect2 = null;
			try
			{
				conect2 = L2DatabaseFactory.getInstance().getConnection();
				PreparedStatement statement = conect2.prepareStatement(ADD_PKKILLS);
				statement.setString(1, killer);
				statement.setString(2, killed);
				statement.setInt(3, 1);
				statement.execute();
				statement.close();
			}
			catch (Exception e)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
				{
					LOG.error("Could not add pkKills:");
					e.printStackTrace();
				}
			}
			finally
			{
				CloseUtil.close(conect2);
			}
			sendMessage("This is the first time you have been killed by " + PlayerWhoKilled.getName() + ".");
			PlayerWhoKilled.sendMessage("You have killed " + getName() + " for the first time.");
		}
	}
	
	/**
	 * Increase pk count, karma and send the info to the player.
	 * @param target
	 * @param targLVL : level of the killed player
	 */
	public void increasePkKillsAndKarma(L2Character target, int targLVL)
	{
		if ((TvT.is_started() && _inEventTvT) || (DM.is_started() && _inEventDM) || (CTF.is_started() && _inEventCTF) || (VIP._started && _inEventVIP))
		{
			return;
		}
		
		int baseKarma = Config.KARMA_MIN_KARMA;
		int newKarma = baseKarma;
		int karmaLimit = Config.KARMA_MAX_KARMA;
		
		int pkLVL = getLevel();
		int pkPKCount = getPkKills();
		
		int lvlDiffMulti = 0;
		int pkCountMulti = 0;
		
		// Check if the attacker has a PK counter greater than 0
		if (pkPKCount > 0)
		{
			pkCountMulti = pkPKCount / 2;
		}
		else
		{
			pkCountMulti = 1;
		}
		
		if (pkCountMulti < 1)
		{
			pkCountMulti = 1;
		}
		
		// Calculate the level difference Multiplier between attacker and killed L2PcInstance
		if (pkLVL > targLVL)
		{
			lvlDiffMulti = pkLVL / targLVL;
		}
		else
		{
			lvlDiffMulti = 1;
		}
		
		if (lvlDiffMulti < 1)
		{
			lvlDiffMulti = 1;
		}
		
		// Calculate the new Karma of the attacker : newKarma = baseKarma*pkCountMulti*lvlDiffMulti
		newKarma *= pkCountMulti;
		newKarma *= lvlDiffMulti;
		
		// Make sure newKarma is less than karmaLimit and higher than baseKarma
		if (newKarma < baseKarma)
		{
			newKarma = baseKarma;
		}
		
		if (newKarma > karmaLimit)
		{
			newKarma = karmaLimit;
		}
		
		// Fix to prevent overflow (=> karma has a max value of 2 147 483 647)
		if (getKarma() > Integer.MAX_VALUE - newKarma)
		{
			newKarma = Integer.MAX_VALUE - getKarma();
		}
		
		// Add karma to attacker and increase its PK counter
		int x, y, z;
		x = getX();
		y = getY();
		z = getZ();
		
		// get local town
		L2TownZone Town = TownManager.getInstance().getTown(x, y, z);
		
		if (!(target instanceof L2Summon))
		{
			setPkKills(getPkKills() + 1);
			getAchievement().increase(AchType.PK);
			
			// disarm character with free weapon
			L2ItemInstance rhand = getInventory().getPaperdollItem(Inventory.PAPERDOLL_RHAND);
			if (rhand != null && rhand.getItem().getItemId() >= 7816 && rhand.getItem().getItemId() <= 7831)
			{
				L2ItemInstance[] unequipped = getInventory().unEquipItemInBodySlotAndRecord(rhand.getItem().getBodyPart());
				InventoryUpdate iu = new InventoryUpdate();
				for (L2ItemInstance element : unequipped)
				{
					iu.addModifiedItem(element);
				}
				sendPacket(iu);
			}
		}
		
		if (Town == null || (isinTownWar() && Config.TW_ALLOW_KARMA))
		{
			setKarma(getKarma() + newKarma);
		}
		
		if (Town != null && isinTownWar())
		{
			if (Town.getTownId() == Config.TW_TOWN_ID && !Config.TW_ALL_TOWNS)
			{
				getInventory().addItem("TownWar", Config.TW_ITEM_ID, Config.TW_ITEM_AMOUNT, this, this);
				sendMessage("You received your prize for a town war kill!");
			}
			else if (Config.TW_ALL_TOWNS && Town.getTownId() != 0)
			{
				getInventory().addItem("TownWar", Config.TW_ITEM_ID, Config.TW_ITEM_AMOUNT, this, this);
				sendMessage("You received your prize for a town war kill!");
			}
		}
		
		if (getTitleOn())
		{
			updateTitle();
		}
		
		// Update the character's title color if they reached any of the 5 PK levels.
		updatePkColor(getPkKills());
		broadcastUserInfo();
		
		// Send a Server->Client UserInfo packet to attacker with its Karma and PK Counter
		sendPacket(new UserInfo(this));
	}
	
	/**
	 * Calculate karma lost.
	 * @param exp the exp
	 * @return the int
	 */
	public int calculateKarmaLost(long exp)
	{
		// KARMA LOSS
		// When a PKer gets killed by another player or a L2MonsterInstance, it loses a certain amount of Karma based on their level.
		// this (with defaults) results in a level 1 losing about ~2 karma per death, and a lvl 70 loses about 11760 karma per death...
		// You lose karma as long as you were not in a pvp zone and you did not kill urself.
		// NOTE: exp for death (if delevel is allowed) is based on the players level
		
		long expGained = Math.abs(exp);
		expGained /= Config.KARMA_XP_DIVIDER;
		
		int karmaLost = 0;
		if (expGained > Integer.MAX_VALUE)
		{
			karmaLost = Integer.MAX_VALUE;
		}
		else
		{
			karmaLost = (int) expGained;
		}
		
		if (karmaLost < Config.KARMA_LOST_BASE)
		{
			karmaLost = Config.KARMA_LOST_BASE;
		}
		if (karmaLost > getKarma())
		{
			karmaLost = getKarma();
		}
		
		return karmaLost;
	}
	
	/**
	 * Update pvp status.
	 */
	public void updatePvPStatus()
	{
		if ((TvT.is_started() && _inEventTvT) || (CTF.is_started() && _inEventCTF) || (DM.is_started() && _inEventDM) || (VIP._started && _inEventVIP))
		{
			return;
		}
		
		if (isInsideZone(ZoneId.ZONE_PVP))
		{
			return;
		}
		
		setPvpFlagLasts(System.currentTimeMillis() + Config.PVP_NORMAL_TIME);
		
		if (getPvpFlag() == 0)
		{
			startPvPFlag();
		}
	}
	
	/**
	 * Update pvp status.
	 * @param target the target
	 */
	public void updatePvPStatus(L2Character target)
	{
		L2PcInstance player_target = null;
		
		if (target instanceof L2PcInstance)
		{
			player_target = (L2PcInstance) target;
		}
		else if (target instanceof L2Summon)
		{
			player_target = ((L2Summon) target).getOwner();
		}
		
		if (player_target == null)
		{
			return;
		}
		
		if (player_target == this)
		{
			player_target = null;
			return; // Target player is self
		}
		
		if ((TvT.is_started() && _inEventTvT && player_target._inEventTvT) || (DM.is_started() && _inEventDM && player_target._inEventDM) || (CTF.is_started() && _inEventCTF && player_target._inEventCTF) || (VIP._started && _inEventVIP && player_target._inEventVIP))
		{
			return;
		}
		
		if (isInDuel() && player_target.getDuelId() == getDuelId())
		{
			return;
		}
		
		if ((!isInsideZone(ZoneId.ZONE_PVP) || !player_target.isInsideZone(ZoneId.ZONE_PVP)) && player_target.getKarma() == 0)
		{
			if (checkIfPvP(player_target))
			{
				setPvpFlagLasts(System.currentTimeMillis() + Config.PVP_PVP_TIME);
			}
			else
			{
				setPvpFlagLasts(System.currentTimeMillis() + Config.PVP_NORMAL_TIME);
			}
			if (getPvpFlag() == 0)
			{
				startPvPFlag();
			}
		}
	}
	
	/**
	 * @return {@code true} if player has Lucky effect and is level 9 or less
	 */
	public boolean isLucky()
	{
		return (getLevel() <= 9) && getSkillLevel(L2Skill.SKILL_LUCKY) > 0;
	}
	
	/**
	 * Restore the specified % of experience this L2PcInstance has lost and sends a Server->Client StatusUpdate packet.<BR>
	 * <BR>
	 * @param restorePercent the restore percent
	 */
	public void restoreExp(double restorePercent)
	{
		if (getExpBeforeDeath() > 0)
		{
			// Restore the specified % of lost experience.
			getStat().addExp((int) Math.round((getExpBeforeDeath() - getExp()) * restorePercent / 100));
			setExpBeforeDeath(0);
		}
	}
	
	/**
	 * Reduce the Experience (and level if necessary) of the L2PcInstance in function of the calculated Death Penalty.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Calculate the Experience loss</li>
	 * <li>Set the value of _expBeforeDeath</li>
	 * <li>Set the new Experience value of the L2PcInstance and Decrease its level if necessary</li>
	 * <li>Send a Server->Client StatusUpdate packet with its new Experience</li><BR>
	 * <BR>
	 * @param atwar the atwar
	 */
	public void deathPenalty(boolean atwar)
	{
		// Get the level of the L2PcInstance
		final int lvl = getLevel();
		
		// The death steal you some Exp
		double percentLost = Config.DEATH_PENALTY_PERCENT_LOST; // standart 4% (lvl>20)
		
		if (getLevel() < 20)
		{
			percentLost = Config.DEATH_PENALTY_PERCENT_LOST2;
		}
		else if (getLevel() >= 20 && getLevel() < 40)
		{
			percentLost = Config.DEATH_PENALTY_PERCENT_LOST3;
		}
		else if (getLevel() >= 40 && getLevel() < 75)
		{
			percentLost = Config.DEATH_PENALTY_PERCENT_LOST4;
		}
		else if (getLevel() >= 75 && getLevel() < 81)
		{
			percentLost = Config.DEATH_PENALTY_PERCENT_LOST5;
		}
		
		if (getKarma() > 0)
		{
			percentLost *= Config.RATE_KARMA_EXP_LOST;
		}
		
		if (isFestivalParticipant() || atwar || isInsideZone(ZoneId.ZONE_SIEGE))
		{
			percentLost /= 4.0;
		}
		
		// Calculate the Experience loss
		long lostExp = 0;
		if (!atEvent && !(_inEventTvT && TvT.is_started()) && !(_inEventDM && DM.is_started()) && !(_inEventCTF && CTF.is_started()) && !(_inEventVIP && VIP._started))
		{
			final byte maxLvl = ExperienceData.getInstance().getMaxLevel();
			if (lvl < maxLvl)
			{
				lostExp = Math.round((getStat().getExpForLevel(lvl + 1) - getStat().getExpForLevel(lvl)) * percentLost / 100);
			}
			else
			{
				lostExp = Math.round((getStat().getExpForLevel(maxLvl) - getStat().getExpForLevel(maxLvl - 1)) * percentLost / 100);
			}
		}
		// Get the Experience before applying penalty
		setExpBeforeDeath(getExp());
		
		if (getCharmOfCourage())
		{
			if (getSiegeState() > 0 && isInsideZone(ZoneId.ZONE_SIEGE))
			{
				lostExp = 0;
			}
			setCharmOfCourage(false);
		}
		
		if (Config.DEBUG)
		{
			LOG.warn(" died and lost " + lostExp + " experience.");
		}
		
		// Set the new Experience value of the L2PcInstance
		getStat().addExp(-lostExp);
	}
	
	/**
	 * Manage the increase level task of a L2PcInstance (Max MP, Max MP, Recommandation, Expertise and beginner skills...).<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Send a Server->Client System Message to the L2PcInstance : YOU_INCREASED_YOUR_LEVEL</li>
	 * <li>Send a Server->Client packet StatusUpdate to the L2PcInstance with new LEVEL, MAX_HP and MAX_MP</li>
	 * <li>Set the current HP and MP of the L2PcInstance, Launch/Stop a HP/MP/CP Regeneration Task and send StatusUpdate packet to all other L2PcInstance to inform (exclusive broadcast)</li>
	 * <li>Recalculate the party level</li>
	 * <li>Recalculate the number of Recommandation that the L2PcInstance can give</li>
	 * <li>Give Expertise skill of this level and remove beginner Lucky skill</li><BR>
	 * <BR>
	 */
	public void increaseLevel()
	{
		// Set the current HP and MP of the L2Character, Launch/Stop a HP/MP/CP Regeneration Task and send StatusUpdate packet to all other L2PcInstance to inform (exclusive broadcast)
		setCurrentHpMp(getMaxHp(), getMaxMp());
		setCurrentCp(getMaxCp());
	}
	
	/**
	 * Stop the HP/MP/CP Regeneration task.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Set the RegenActive flag to False</li>
	 * <li>Stop the HP/MP/CP Regeneration task</li><BR>
	 * <BR>
	 */
	public void stopAllTimers()
	{
		stopHpMpRegeneration();
		stopWarnUserTakeBreak();
		stopWaterTask();
		stopRentPet();
		stopPvpRegTask();
		stopPunishTask(true);
		stopBotChecker();
		quakeSystem = 0;
	}
	
	/**
	 * Return the L2Summon of the L2PcInstance or null.<BR>
	 * <BR>
	 * @return the pet
	 */
	@Override
	public L2Summon getPet()
	{
		return _summon;
	}
	
	/**
	 * Set the L2Summon of the L2PcInstance.<BR>
	 * <BR>
	 * @param summon the new pet
	 */
	public void setPet(L2Summon summon)
	{
		_summon = summon;
	}
	
	/**
	 * Return the L2Summon of the L2PcInstance or null.<BR>
	 * <BR>
	 * @return the trained beast
	 */
	public L2TamedBeastInstance getTrainedBeast()
	{
		return _tamedBeast;
	}
	
	/**
	 * Set the L2Summon of the L2PcInstance.<BR>
	 * <BR>
	 * @param tamedBeast the new trained beast
	 */
	public void setTrainedBeast(L2TamedBeastInstance tamedBeast)
	{
		_tamedBeast = tamedBeast;
	}
	
	/**
	 * Return the L2PcInstance requester of a transaction (ex : FriendInvite, JoinAlly, JoinParty...).<BR>
	 * <BR>
	 * @return the request
	 */
	public L2Request getRequest()
	{
		return _request;
	}
	
	/**
	 * Set the L2PcInstance requester of a transaction (ex : FriendInvite, JoinAlly, JoinParty...).<BR>
	 * <BR>
	 * @param requester the new active requester
	 */
	public synchronized void setActiveRequester(L2PcInstance requester)
	{
		_activeRequester = requester;
	}
	
	/**
	 * Return the L2PcInstance requester of a transaction (ex : FriendInvite, JoinAlly, JoinParty...).<BR>
	 * <BR>
	 * @return the active requester
	 */
	public synchronized L2PcInstance getActiveRequester()
	{
		L2PcInstance requester = _activeRequester;
		if (requester != null)
		{
			if (requester.isRequestExpired() && _activeTradeList == null)
			{
				_activeRequester = null;
			}
		}
		return _activeRequester;
	}
	
	/**
	 * Return True if a transaction is in progress.<BR>
	 * <BR>
	 * @return true, if is processing request
	 */
	public boolean isProcessingRequest()
	{
		return _activeRequester != null || _requestExpireTime > GameTimeController.getInstance().getGameTicks();
	}
	
	/**
	 * Return True if a transaction is in progress.<BR>
	 * <BR>
	 * @return true, if is processing transaction
	 */
	public boolean isProcessingTransaction()
	{
		return _activeRequester != null || _activeTradeList != null || _requestExpireTime > GameTimeController.getInstance().getGameTicks();
	}
	
	/**
	 * Select the Warehouse to be used in next activity.<BR>
	 * <BR>
	 * @param partner the partner
	 */
	public void onTransactionRequest(L2PcInstance partner)
	{
		_requestExpireTime = GameTimeController.getInstance().getGameTicks() + REQUEST_TIMEOUT * GameTimeController.TICKS_PER_SECOND;
		if (partner != null)
		{
			partner.setActiveRequester(this);
		}
	}
	
	/**
	 * Select the Warehouse to be used in next activity.<BR>
	 * <BR>
	 */
	public void onTransactionResponse()
	{
		_requestExpireTime = 0;
	}
	
	/**
	 * Select the Warehouse to be used in next activity.<BR>
	 * <BR>
	 * @param warehouse the new active warehouse
	 */
	public void setActiveWarehouse(ItemContainer warehouse)
	{
		_activeWarehouse = warehouse;
	}
	
	/**
	 * Return active Warehouse.<BR>
	 * <BR>
	 * @return the active warehouse
	 */
	public ItemContainer getActiveWarehouse()
	{
		return _activeWarehouse;
	}
	
	/**
	 * Select the TradeList to be used in next activity.<BR>
	 * <BR>
	 * @param tradeList the new active trade list
	 */
	public void setActiveTradeList(TradeList tradeList)
	{
		_activeTradeList = tradeList;
	}
	
	/**
	 * Return active TradeList.<BR>
	 * <BR>
	 * @return the active trade list
	 */
	public TradeList getActiveTradeList()
	{
		return _activeTradeList;
	}
	
	/**
	 * On trade start.
	 * @param partner the partner
	 */
	public void onTradeStart(L2PcInstance partner)
	{
		_activeTradeList = new TradeList(this);
		_activeTradeList.setPartner(partner);
		
		SystemMessage msg = new SystemMessage(SystemMessageId.BEGIN_TRADE_WITH_S1);
		msg.addString(partner.getName());
		sendPacket(msg);
		sendPacket(new TradeStart(this));
	}
	
	/**
	 * On trade confirm.
	 * @param partner the partner
	 */
	public void onTradeConfirm(L2PcInstance partner)
	{
		SystemMessage msg = new SystemMessage(SystemMessageId.S1_CONFIRMED_TRADE);
		msg.addString(partner.getName());
		sendPacket(msg);
		partner.sendPacket(TradePressOwnOk.STATIC_PACKET);
		sendPacket(TradePressOtherOk.STATIC_PACKET);
	}
	
	/**
	 * On trade cancel.
	 * @param partner the partner
	 */
	public void onTradeCancel(L2PcInstance partner)
	{
		if (_activeTradeList == null)
		{
			return;
		}
		
		_activeTradeList.lock();
		_activeTradeList = null;
		
		sendPacket(new SendTradeDone(0));
		SystemMessage msg = new SystemMessage(SystemMessageId.S1_CANCELED_TRADE);
		msg.addString(partner.getName());
		sendPacket(msg);
		msg = null;
	}
	
	/**
	 * On trade finish.
	 * @param successfull the successfull
	 */
	public void onTradeFinish(boolean successfull)
	{
		_activeTradeList = null;
		sendPacket(new SendTradeDone(1));
		if (successfull)
		{
			sendPacket(new SystemMessage(SystemMessageId.TRADE_SUCCESSFUL));
		}
	}
	
	/**
	 * Start trade.
	 * @param partner the partner
	 */
	public void startTrade(L2PcInstance partner)
	{
		onTradeStart(partner);
		partner.onTradeStart(this);
	}
	
	/**
	 * Cancel active trade.
	 */
	public void cancelActiveTrade()
	{
		if (_activeTradeList == null)
		{
			return;
		}
		
		L2PcInstance partner = _activeTradeList.getPartner();
		if (partner != null)
		{
			partner.onTradeCancel(this);
			partner = null;
		}
		onTradeCancel(this);
	}
	
	/**
	 * Return the _createList object of the L2PcInstance.<BR>
	 * <BR>
	 * @return the creates the list
	 */
	public L2ManufactureList getCreateList()
	{
		return _createList;
	}
	
	/**
	 * Set the _createList object of the L2PcInstance.<BR>
	 * <BR>
	 * @param x the new creates the list
	 */
	public void setCreateList(L2ManufactureList x)
	{
		_createList = x;
	}
	
	/**
	 * Return the _sellList object of the L2PcInstance.<BR>
	 * <BR>
	 * @return the sell list
	 */
	public TradeList getSellList()
	{
		if (_sellList == null)
		{
			_sellList = new TradeList(this);
		}
		return _sellList;
	}
	
	/**
	 * Return the _buyList object of the L2PcInstance.<BR>
	 * <BR>
	 * @return the buy list
	 */
	public TradeList getBuyList()
	{
		if (_buyList == null)
		{
			_buyList = new TradeList(this);
		}
		return _buyList;
	}
	
	/**
	 * Set the Private Store type of the L2PcInstance.<BR>
	 * <BR>
	 * <B><U> Values </U> :</B><BR>
	 * <BR>
	 * <li>0 : STORE_PRIVATE_NONE</li>
	 * <li>1 : STORE_PRIVATE_SELL</li>
	 * <li>2 : sellmanage</li><BR>
	 * <li>3 : STORE_PRIVATE_BUY</li><BR>
	 * <li>4 : buymanage</li><BR>
	 * <li>5 : STORE_PRIVATE_MANUFACTURE</li><BR>
	 * @param type the new private store type
	 */
	public void setPrivateStoreType(int type)
	{
		_privatestore = type;
		
		if (_privatestore == STORE_PRIVATE_NONE && (getClient() == null || isInOfflineMode()))
		{
			/*
			 * if(this._originalNameColorOffline!=0) getAppearance().setNameColor(this._originalNameColorOffline); else getAppearance().setNameColor(_accessLevel.getNameColor());
			 */
			store();
			
			if (Config.OFFLINE_DISCONNECT_FINISHED)
			{
				deleteMe();
				
				if (getClient() != null)
				{
					getClient().setActiveChar(null); // prevent deleteMe from being called a second time on disconnection
				}
			}
		}
	}
	
	/**
	 * Return the Private Store type of the L2PcInstance.<BR>
	 * <BR>
	 * <B><U> Values </U> :</B><BR>
	 * <BR>
	 * <li>0 : STORE_PRIVATE_NONE</li>
	 * <li>1 : STORE_PRIVATE_SELL</li>
	 * <li>2 : sellmanage</li><BR>
	 * <li>3 : STORE_PRIVATE_BUY</li><BR>
	 * <li>4 : buymanage</li><BR>
	 * <li>5 : STORE_PRIVATE_MANUFACTURE</li><BR>
	 * @return the private store type
	 */
	public int getPrivateStoreType()
	{
		return _privatestore;
	}
	
	/**
	 * Set the _skillLearningClassId object of the L2PcInstance.<BR>
	 * <BR>
	 * @param classId the new skill learning class id
	 */
	public void setSkillLearningClassId(ClassId classId)
	{
		_skillLearningClassId = classId;
	}
	
	/**
	 * Return the _skillLearningClassId object of the L2PcInstance.<BR>
	 * <BR>
	 * @return the skill learning class id
	 */
	public ClassId getSkillLearningClassId()
	{
		return _skillLearningClassId;
	}
	
	/**
	 * Set the _clan object, _clanId, _clanLeader Flag and title of the L2PcInstance.<BR>
	 * <BR>
	 * @param clan the new clan
	 */
	public void setClan(L2Clan clan)
	{
		_clan = clan;
		setTitle("");
		
		if (clan == null)
		{
			_clanId = 0;
			_clanPrivileges = 0;
			_pledgeType = 0;
			_powerGrade = 0;
			_lvlJoinedAcademy = 0;
			_apprentice = 0;
			_sponsor = 0;
			return;
		}
		
		if (!clan.isMember(getName()))
		{
			// char has been kicked from clan
			setClan(null);
			return;
		}
		
		_clanId = clan.getClanId();
		
		// Add clan leader skills if clanleader
		if (isClanLeader() && clan.getLevel() >= 4)
		{
			addClanLeaderSkills(true);
		}
		else
		{
			addClanLeaderSkills(false);
		}
		
	}
	
	/**
	 * Return the _clan object of the L2PcInstance.<BR>
	 * <BR>
	 * @return the clan
	 */
	public L2Clan getClan()
	{
		return _clan;
	}
	
	/**
	 * Return True if the L2PcInstance is the leader of its clan.<BR>
	 * <BR>
	 * @return true, if is clan leader
	 */
	public boolean isClanLeader()
	{
		if (getClan() == null)
		{
			return false;
		}
		
		return getObjectId() == getClan().getLeaderId();
	}
	
	/**
	 * Reduce the number of arrows owned by the L2PcInstance and send it Server->Client Packet InventoryUpdate or ItemList (to unequip if the last arrow was consummed).<BR>
	 * <BR>
	 */
	@Override
	protected void reduceArrowCount()
	{
		L2ItemInstance arrows = null;
		
		if (Config.DONT_DESTROY_ARROWS)
		{
			arrows = getInventory().destroyItem("Consume", getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_LHAND), 0, this, null);
		}
		else
		{
			arrows = getInventory().destroyItem("Consume", getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_LHAND), 1, this, null);
		}
		
		if (Config.DEBUG)
		{
			LOG.warn("arrow count:" + (arrows == null ? 0 : arrows.getCount()));
		}
		
		if (arrows == null || arrows.getCount() == 0)
		{
			getInventory().unEquipItemInSlot(Inventory.PAPERDOLL_LHAND);
			_arrowItem = null;
			
			if (Config.DEBUG)
			{
				LOG.warn("removed arrows count");
			}
			
			sendPacket(new ItemList(this, false));
		}
		else
		{
			if (!Config.FORCE_INVENTORY_UPDATE)
			{
				InventoryUpdate iu = new InventoryUpdate();
				iu.addModifiedItem(arrows);
				sendPacket(iu);
			}
			else
			{
				sendPacket(new ItemList(this, false));
			}
		}
	}
	
	/**
	 * Equip arrows needed in left hand and send a Server->Client packet ItemList to the L2PcINstance then return True.<BR>
	 * <BR>
	 * @return true, if successful
	 */
	@Override
	protected boolean checkAndEquipArrows()
	{
		// Check if nothing is equiped in left hand
		if (getInventory().getPaperdollItem(Inventory.PAPERDOLL_LHAND) == null)
		{
			// Get the L2ItemInstance of the arrows needed for this bow
			_arrowItem = getInventory().findArrowForBow(getActiveWeaponItem());
			
			if (_arrowItem != null)
			{
				// Equip arrows needed in left hand
				getInventory().setPaperdollItem(Inventory.PAPERDOLL_LHAND, _arrowItem);
				
				// Send a Server->Client packet ItemList to this L2PcINstance to update left hand equipement
				ItemList il = new ItemList(this, false);
				sendPacket(il);
			}
		}
		else
		{
			// Get the L2ItemInstance of arrows equiped in left hand
			_arrowItem = getInventory().getPaperdollItem(Inventory.PAPERDOLL_LHAND);
		}
		
		return _arrowItem != null;
	}
	
	/**
	 * Disarm the player's weapon and shield.<BR>
	 * <BR>
	 * @return true, if successful
	 */
	public boolean disarmWeapons()
	{
		// Don't allow disarming a cursed weapon
		if (isCursedWeaponEquiped() && !getAccessLevel().isGm())
		{
			return false;
		}
		
		// Unequip the weapon
		L2ItemInstance wpn = getInventory().getPaperdollItem(Inventory.PAPERDOLL_RHAND);
		if (wpn == null)
		{
			wpn = getInventory().getPaperdollItem(Inventory.PAPERDOLL_LRHAND);
		}
		
		if (wpn != null)
		{
			if (wpn.isWear())
			{
				return false;
			}
			
			// Remove augementation boni on unequip
			if (wpn.isAugmented())
			{
				wpn.getAugmentation().removeBoni(this);
			}
			
			L2ItemInstance[] unequiped = getInventory().unEquipItemInBodySlotAndRecord(wpn.getItem().getBodyPart());
			InventoryUpdate iu = new InventoryUpdate();
			for (L2ItemInstance element : unequiped)
			{
				iu.addModifiedItem(element);
			}
			sendPacket(iu);
			
			abortAttack();
			broadcastUserInfo();
			
			// this can be 0 if the user pressed the right mousebutton twice very fast
			if (unequiped.length > 0)
			{
				SystemMessage sm = null;
				if (unequiped[0].getEnchantLevel() > 0)
				{
					sm = new SystemMessage(SystemMessageId.EQUIPMENT_S1_S2_REMOVED);
					sm.addNumber(unequiped[0].getEnchantLevel());
					sm.addItemName(unequiped[0].getItemId());
				}
				else
				{
					sm = new SystemMessage(SystemMessageId.S1_DISARMED);
					sm.addItemName(unequiped[0].getItemId());
				}
				sendPacket(sm);
			}
		}
		
		// Unequip the shield
		L2ItemInstance sld = getInventory().getPaperdollItem(Inventory.PAPERDOLL_LHAND);
		if (sld != null)
		{
			if (sld.isWear())
			{
				return false;
			}
			
			L2ItemInstance[] unequiped = getInventory().unEquipItemInBodySlotAndRecord(sld.getItem().getBodyPart());
			InventoryUpdate iu = new InventoryUpdate();
			for (L2ItemInstance element : unequiped)
			{
				iu.addModifiedItem(element);
			}
			sendPacket(iu);
			
			abortAttack();
			broadcastUserInfo();
			
			// this can be 0 if the user pressed the right mousebutton twice very fast
			if (unequiped.length > 0)
			{
				SystemMessage sm = null;
				if (unequiped[0].getEnchantLevel() > 0)
				{
					sm = new SystemMessage(SystemMessageId.EQUIPMENT_S1_S2_REMOVED);
					sm.addNumber(unequiped[0].getEnchantLevel());
					sm.addItemName(unequiped[0].getItemId());
				}
				else
				{
					sm = new SystemMessage(SystemMessageId.S1_DISARMED);
					sm.addItemName(unequiped[0].getItemId());
				}
				sendPacket(sm);
			}
		}
		return true;
	}
	
	/**
	 * Return True if the L2PcInstance use a dual weapon.<BR>
	 * <BR>
	 * @return true, if is using dual weapon
	 */
	@Override
	public boolean isUsingDualWeapon()
	{
		L2Weapon weaponItem = getActiveWeaponItem();
		if (weaponItem == null)
		{
			return false;
		}
		
		if (weaponItem.getItemType() == L2WeaponType.DUAL)
		{
			return true;
		}
		else if (weaponItem.getItemType() == L2WeaponType.DUALFIST)
		{
			return true;
		}
		else if (weaponItem.getItemId() == 248)
		{
			return true;
		}
		else if (weaponItem.getItemId() == 252)
		{
			return true;
		}
		else
		{
			return false;
		}
	}
	
	/**
	 * Sets the uptime.
	 * @param time the new uptime
	 */
	public void setUptime(long time)
	{
		_uptime = time;
	}
	
	/**
	 * Gets the uptime.
	 * @return the uptime
	 */
	public long getUptime()
	{
		return System.currentTimeMillis() - _uptime;
	}
	
	/**
	 * Return True if the L2PcInstance is invulnerable.<BR>
	 * <BR>
	 * @return true, if is invul
	 */
	@Override
	public boolean isInvul()
	{
		return _isInvul || _isTeleporting || _protectEndTime > GameTimeController.getInstance().getGameTicks() || _teleportProtectEndTime > GameTimeController.getInstance().getGameTicks();
	}
	
	/**
	 * Return True if the L2PcInstance has a Party in progress.<BR>
	 * <BR>
	 * @return true, if is in party
	 */
	@Override
	public boolean isInParty()
	{
		return _party != null;
	}
	
	/**
	 * Set the _party object of the L2PcInstance (without joining it).<BR>
	 * <BR>
	 * @param party the new party
	 */
	public void setParty(L2Party party)
	{
		_party = party;
	}
	
	/**
	 * Set the _party object of the L2PcInstance AND join it.<BR>
	 * <BR>
	 * @param party the party
	 */
	public void joinParty(L2Party party)
	{
		if (party == null)
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (party.getMemberCount() == 9)
		{
			sendPacket(new SystemMessage(SystemMessageId.PARTY_FULL));
			return;
		}
		
		if (party.getPartyMembers().contains(this))
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (party.getMemberCount() < 9)
		{
			_party = party;
			party.addPartyMember(this);
		}
	}
	
	/**
	 * Return true if the L2PcInstance is a GM.<BR>
	 * <BR>
	 * @return true, if is gM
	 */
	public boolean isGM()
	{
		return getAccessLevel().isGm();
	}
	
	/**
	 * Return true if the L2PcInstance is a Administrator.<BR>
	 * <BR>
	 * @return true, if is administrator
	 */
	public boolean isAdministrator()
	{
		return getAccessLevel().getLevel() == Config.MASTERACCESS_LEVEL;
	}
	
	/**
	 * Return true if the L2PcInstance is a User.<BR>
	 * <BR>
	 * @return true, if is user
	 */
	public boolean isUser()
	{
		return getAccessLevel().getLevel() == Config.USERACCESS_LEVEL;
	}
	
	/**
	 * Checks if is normal gm.
	 * @return true, if is normal gm
	 */
	public boolean isNormalGm()
	{
		return !isAdministrator() && !isUser();
	}
	
	/**
	 * Manage the Leave Party task of the L2PcInstance.<BR>
	 * <BR>
	 */
	public void leaveParty()
	{
		if (isInParty())
		{
			_party.removePartyMember(this);
			_party = null;
		}
	}
	
	@Override
	public L2Party getParty()
	{
		return _party;
	}
	
	public void setFirstLog(int first_log)
	{
		_first_log = false;
		if (first_log == 1)
		{
			_first_log = true;
		}
	}
	
	/**
	 * Sets the first log.
	 * @param first_log the new first log
	 */
	public void setFirstLog(boolean first_log)
	{
		_first_log = first_log;
	}
	
	public boolean getFirstLog()
	{
		return _first_log;
	}
	
	/**
	 * Manage a cancel cast task for the L2PcInstance.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Set the Intention of the AI to AI_INTENTION_IDLE</li>
	 * <li>Enable all skills (set _allSkillsDisabled to False)</li>
	 * <li>Send a Server->Client Packet MagicSkillCanceld to the L2PcInstance and all L2PcInstance in the _KnownPlayers of the L2Character (broadcast)</li><BR>
	 * <BR>
	 */
	public void cancelCastMagic()
	{
		getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
		
		enableAllSkills();
		
		MagicSkillCanceld msc = new MagicSkillCanceld(getObjectId());
		broadcastPacket(msc);
	}
	
	/**
	 * Set the _accessLevel of the L2PcInstance.<BR>
	 * <BR>
	 * @param level the new access level
	 */
	public void setAccessLevel(int level)
	{
		if (level == Config.MASTERACCESS_LEVEL)
		{
			// LOG.warn("Admin Login at "+ fmt.format(new Date(System.currentTimeMillis())) +" " + getName() + " logs in game with AccessLevel "+ level +".");
			String text = "Admi Login: " + getName() + " AccessLevel: " + level + "";
			Log.add(text, "AdminLogin");
			_accessLevel = AccessLevels.getInstance()._masterAccessLevel;
		}
		else if (level == Config.USERACCESS_LEVEL)
		{
			_accessLevel = AccessLevels.getInstance()._userAccessLevel;
		}
		else
		{
			if (level > 0)
			{
				// LOG.warn("GM Login at "+ fmt.format(new Date(System.currentTimeMillis())) +" " + getName() + " logs in game with AccessLevel "+ level +".");
				String text = "GM Login: " + getName() + " AccessLevel: " + level + "";
				Log.add(text, "AdminLogin");
			}
			AccessLevel accessLevel = AccessLevels.getInstance().getAccessLevel(level);
			
			if (accessLevel == null)
			{
				if (level < 0)
				{
					AccessLevels.getInstance().addBanAccessLevel(level);
					_accessLevel = AccessLevels.getInstance().getAccessLevel(level);
				}
				else
				{
					LOG.warn("Tried to set unregistered access level " + level + " to character " + getName() + ". Setting access level without privileges!");
					_accessLevel = AccessLevels.getInstance()._userAccessLevel;
				}
			}
			else
			{
				_accessLevel = accessLevel;
			}
			
			accessLevel = null;
		}
		
		if (_accessLevel != AccessLevels.getInstance()._userAccessLevel)
		{
			if (getAccessLevel().useNameColor())
			{
				getAppearance().setNameColor(_accessLevel.getNameColor());
			}
			if (getAccessLevel().useTitleColor())
			{
				getAppearance().setTitleColor(_accessLevel.getTitleColor());
			}
			broadcastUserInfo();
		}
	}
	
	/**
	 * Sets the account accesslevel.
	 * @param level the new account accesslevel
	 */
	public void setAccountAccesslevel(int level)
	{
		LoginServerThread.getInstance().sendAccessLevel(getAccountName(), level);
	}
	
	/**
	 * Return the _accessLevel of the L2PcInstance.<BR>
	 * <BR>
	 * @return the access level
	 */
	public AccessLevel getAccessLevel()
	{
		if (Config.EVERYBODY_HAS_ADMIN_RIGHTS)
		{
			return AccessLevels.getInstance()._masterAccessLevel;
		}
		else if (_accessLevel == null)
		{
			setAccessLevel(Config.USERACCESS_LEVEL);
		}
		return _accessLevel;
	}
	
	@Override
	public double getLevelMod()
	{
		return (100.0 - 11 + getLevel()) / 100.0;
	}
	
	/**
	 * Update Stats of the L2PcInstance client side by sending Server->Client packet UserInfo/StatusUpdate to this L2PcInstance and CharInfo/StatusUpdate to all L2PcInstance in its _KnownPlayers (broadcast).<BR>
	 * <BR>
	 * @param broadcastType the broadcast type
	 */
	public void updateAndBroadcastStatus(int broadcastType)
	{
		refreshOverloaded();
		refreshExpertisePenalty();
		// Send a Server->Client packet UserInfo to this L2PcInstance and CharInfo to all L2PcInstance in its _KnownPlayers (broadcast)
		if (broadcastType == 1)
		{
			sendPacket(new UserInfo(this));
		}
		
		if (broadcastType == 2)
		{
			broadcastUserInfo();
		}
	}
	
	/**
	 * Send a Server->Client StatusUpdate packet with Karma and PvP Flag to the L2PcInstance and all L2PcInstance to inform (broadcast).<BR>
	 * <BR>
	 * @param flag the new karma flag
	 */
	public void setKarmaFlag(final int flag)
	{
		sendPacket(new UserInfo(this));
		for (final L2PcInstance player : getKnownList().getKnownPlayers().values())
		{
			player.sendPacket(new RelationChanged(this, getRelation(player), isAutoAttackable(player)));
			
			if (getPet() != null)
			{
				getPet().broadcastPacket(new NpcInfo(getPet(), null));
			}
		}
	}
	
	/**
	 * Send a Server->Client StatusUpdate packet with Karma to the L2PcInstance and all L2PcInstance to inform (broadcast).<BR>
	 * <BR>
	 */
	public void broadcastKarma()
	{
		sendPacket(new UserInfo(this));
		for (final L2PcInstance player : getKnownList().getKnownPlayers().values())
		{
			if (player == null)
			{
				continue;
			}
			
			player.sendPacket(new RelationChanged(this, getRelation(player), isAutoAttackable(player)));
			
			if (getPet() != null)
			{
				getPet().broadcastPacket(new NpcInfo(getPet(), null));
			}
		}
	}
	
	/**
	 * Set the online Flag to True or False and update the characters table of the database with online status and lastAccess (called when login and logout).<BR>
	 * <BR>
	 * @param isOnline the new online status
	 */
	public void setOnlineStatus(boolean isOnline)
	{
		if (_isOnline != isOnline)
		{
			_isOnline = isOnline;
		}
		
		if (Config.ALLOW_HITMAN_GDE && Hitman.getInstance().exists(getObjectId()))
		{
			Hitman.getInstance().getTarget(getObjectId()).setOnline(isOnline);
		}
		
		// Update the characters table of the database with online status and lastAccess (called when login and logout)
		updateOnlineStatus();
	}
	
	/**
	 * Sets the checks if is in7s dungeon.
	 * @param isIn7sDungeon the new checks if is in7s dungeon
	 */
	public void setIsIn7sDungeon(boolean isIn7sDungeon)
	{
		if (_isIn7sDungeon != isIn7sDungeon)
		{
			_isIn7sDungeon = isIn7sDungeon;
		}
		
		updateIsIn7sDungeonStatus();
	}
	
	/**
	 * Update the characters table of the database with online status and lastAccess of this L2PcInstance (called when login and logout).<BR>
	 * <BR>
	 */
	public void updateOnlineStatus()
	{
		Connection con = null;
		
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("UPDATE characters SET online=?, lastAccess=? WHERE obj_id=?");
			statement.setInt(1, isOnline());
			statement.setLong(2, System.currentTimeMillis());
			statement.setInt(3, getObjectId());
			statement.execute();
			statement.close();
		}
		catch (Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				LOG.error("could not set char online status:");
				e.printStackTrace();
			}
		}
		finally
		{
			CloseUtil.close(con);
		}
	}
	
	/**
	 * Update is in7s dungeon status.
	 */
	public void updateIsIn7sDungeonStatus()
	{
		Connection con = null;
		
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("UPDATE characters SET isIn7sDungeon=?, lastAccess=? WHERE obj_id=?");
			statement.setInt(1, isIn7sDungeon() ? 1 : 0);
			statement.setLong(2, System.currentTimeMillis());
			statement.setInt(3, getObjectId());
			statement.execute();
			statement.close();
			statement = null;
		}
		catch (Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				LOG.error("could not set char isIn7sDungeon status:");
				e.printStackTrace();
			}
		}
		finally
		{
			CloseUtil.close(con);
		}
	}
	
	/**
	 * Update first log.
	 */
	public void updateFirstLog()
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("UPDATE characters SET first_log=? WHERE obj_id=?");
			
			int _fl;
			if (getFirstLog())
			{
				_fl = 1;
			}
			else
			{
				_fl = 0;
			}
			statement.setInt(1, _fl);
			statement.setInt(2, getObjectId());
			statement.execute();
			statement.close();
		}
		catch (Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				LOG.error("could not set char first login:");
				e.printStackTrace();
			}
		}
		finally
		{
			CloseUtil.close(con);
		}
	}
	
	/**
	 * Create a new player in the characters table of the database.<BR>
	 * <BR>
	 * @return true, if successful
	 */
	private boolean createDb()
	{
		boolean output = false;
		
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement;
			statement = con.prepareStatement("INSERT INTO characters " + "(account_name,obj_Id,char_name,level,maxHp,curHp,maxCp,curCp,maxMp,curMp," + "acc,crit,evasion,mAtk,mDef,mSpd,pAtk,pDef,pSpd,runSpd,walkSpd," + "str,con,dex,_int,men,wit,face,hairStyle,hairColor,sex,"
				+ "movement_multiplier,attack_speed_multiplier,colRad,colHeight," + "exp,sp,karma,pvpkills,pkkills,clanid,maxload,race,classid,deletetime," + "cancraft,title,accesslevel,online,isin7sdungeon,clan_privs,wantspeace," + "base_class,newbie,nobless,power_grade,last_recom_date"
				+ ",name_color,title_color,aio,aio_end,autoloot,autoloot_herbs) " + "values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
			statement.setString(1, _accountName);
			statement.setInt(2, getObjectId());
			statement.setString(3, getName());
			statement.setInt(4, getLevel());
			statement.setInt(5, getMaxHp());
			statement.setDouble(6, getCurrentHp());
			statement.setInt(7, getMaxCp());
			statement.setDouble(8, getCurrentCp());
			statement.setInt(9, getMaxMp());
			statement.setDouble(10, getCurrentMp());
			statement.setInt(11, getAccuracy());
			statement.setInt(12, getCriticalHit(null, null));
			statement.setInt(13, getEvasionRate(null));
			statement.setInt(14, getMAtk(null, null));
			statement.setInt(15, getMDef(null, null));
			statement.setInt(16, getMAtkSpd());
			statement.setInt(17, getPAtk(null));
			statement.setInt(18, getPDef(null));
			statement.setInt(19, getPAtkSpd());
			statement.setInt(20, getRunSpeed());
			statement.setInt(21, getWalkSpeed());
			statement.setInt(22, getSTR());
			statement.setInt(23, getCON());
			statement.setInt(24, getDEX());
			statement.setInt(25, getINT());
			statement.setInt(26, getMEN());
			statement.setInt(27, getWIT());
			statement.setInt(28, getAppearance().getFace());
			statement.setInt(29, getAppearance().getHairStyle());
			statement.setInt(30, getAppearance().getHairColor());
			statement.setInt(31, getAppearance().getSex() ? 1 : 0);
			statement.setDouble(32, 1/* getMovementMultiplier() */);
			statement.setDouble(33, 1/* getAttackSpeedMultiplier() */);
			statement.setDouble(34, getTemplate().collisionRadius/* getCollisionRadius() */);
			statement.setDouble(35, getTemplate().collisionHeight/* getCollisionHeight() */);
			statement.setLong(36, getExp());
			statement.setInt(37, getSp());
			statement.setInt(38, getKarma());
			statement.setInt(39, getPvpKills());
			statement.setInt(40, getPkKills());
			statement.setInt(41, getClanId());
			statement.setInt(42, getMaxLoad());
			statement.setInt(43, getRace().ordinal());
			statement.setInt(44, getClassId().getId());
			statement.setLong(45, getDeleteTimer());
			statement.setInt(46, hasDwarvenCraft() ? 1 : 0);
			statement.setString(47, getTitle());
			statement.setInt(48, getAccessLevel().getLevel());
			statement.setInt(49, isOnline());
			statement.setInt(50, isIn7sDungeon() ? 1 : 0);
			statement.setInt(51, getClanPrivileges());
			statement.setInt(52, getWantsPeace());
			statement.setInt(53, getBaseClass());
			statement.setInt(54, isNewbie() ? 1 : 0);
			statement.setInt(55, isNoble() ? 1 : 0);
			statement.setLong(56, 0);
			statement.setLong(57, System.currentTimeMillis());
			
			statement.setString(58, StringToHex(Integer.toHexString(getAppearance().getNameColor()).toUpperCase()));
			statement.setString(59, StringToHex(Integer.toHexString(getAppearance().getTitleColor()).toUpperCase()));
			statement.setInt(60, isAio() ? 1 : 0);
			statement.setLong(61, 0);
			
			if (Config.AUTO_LOOT)
			{
				statement.setInt(62, 1);
			}
			else
			{
				statement.setInt(62, 0);
			}
			
			if (Config.AUTO_LOOT_HERBS)
			{
				statement.setInt(63, 1);
			}
			else
			{
				statement.setInt(63, 0);
			}
			
			statement.executeUpdate();
			statement.close();
			statement = null;
			
			output = true;
		}
		catch (Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				LOG.error("Could not insert char data:");
				e.printStackTrace();
			}
		}
		finally
		{
			CloseUtil.close(con);
		}
		
		if (output)
		{
			String text = "Created new character : " + getName() + " for account: " + _accountName;
			Log.add(text, "New_chars");
		}
		
		return output;
	}
	
	/**
	 * Retrieve a L2PcInstance from the characters table of the database and add it in _allObjects of the L2world.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Retrieve the L2PcInstance from the characters table of the database</li>
	 * <li>Add the L2PcInstance object in _allObjects</li>
	 * <li>Set the x,y,z position of the L2PcInstance and make it invisible</li>
	 * <li>Update the overloaded status of the L2PcInstance</li><BR>
	 * <BR>
	 * @param objectId Identifier of the object to initialized
	 * @return The L2PcInstance loaded from the database
	 */
	private static L2PcInstance restore(int objectId)
	{
		L2PcInstance player = null;
		double curHp = 0;
		double curCp = 0;
		double curMp = 0;
		
		Connection con = null;
		try
		{
			// Retrieve the L2PcInstance from the characters table of the database
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement(RESTORE_CHARACTER);
			
			if (Config.FAKE_ARMORS)
			{
				statement = con.prepareStatement(RESTORE_CHARACTER2);
			}
			
			statement.setInt(1, objectId);
			ResultSet rset = statement.executeQuery();
			
			while (rset.next())
			{
				final int activeClassId = rset.getInt("classid");
				final boolean female = rset.getInt("sex") != 0;
				final L2PcTemplate template = CharTemplateTable.getInstance().getTemplate(activeClassId);
				PcAppearance app = new PcAppearance(rset.getByte("face"), rset.getByte("hairColor"), rset.getByte("hairStyle"), female);
				
				player = new L2PcInstance(objectId, template, rset.getString("account_name"), app);
				player.setIsPhantom(false);
				restorePremServiceData(player, rset.getString("account_name"));
				
				player.setName(rset.getString("char_name"));
				player._lastAccess = rset.getLong("lastAccess");
				
				player.getStat().setExp(rset.getLong("exp"));
				player.setExpBeforeDeath(rset.getLong("expBeforeDeath"));
				player.getStat().setLevel(rset.getByte("level"));
				player.getStat().setSp(rset.getInt("sp"));
				
				player.setWantsPeace(rset.getInt("wantspeace"));
				
				player.setHeading(rset.getInt("heading"));
				
				player.setKarma(rset.getInt("karma"));
				player.setPvpKills(rset.getInt("pvpkills"));
				player.setPkKills(rset.getInt("pkkills"));
				player.setOnlineTime(rset.getLong("onlinetime"));
				player.setNewbie(rset.getInt("newbie") == 1);
				player.setNoble(rset.getInt("nobless") == 1);
				player.setClanJoinExpiryTime(rset.getLong("clan_join_expiry_time"));
				player.setFirstLog(rset.getInt("first_log"));
				player.pcBangPoint = rset.getInt("pc_point");
				app = null;
				
				if (player.getClanJoinExpiryTime() < System.currentTimeMillis())
				{
					player.setClanJoinExpiryTime(0);
				}
				player.setClanCreateExpiryTime(rset.getLong("clan_create_expiry_time"));
				if (player.getClanCreateExpiryTime() < System.currentTimeMillis())
				{
					player.setClanCreateExpiryTime(0);
				}
				
				int clanId = rset.getInt("clanid");
				player.setPowerGrade((int) rset.getLong("power_grade"));
				player.setPledgeType(rset.getInt("subpledge"));
				player.setLastRecomUpdate(rset.getLong("last_recom_date"));
				
				if (clanId > 0)
				{
					player.setClan(ClanTable.getInstance().getClan(clanId));
				}
				
				if (player.getClan() != null)
				{
					if (player.getClan().getLeaderId() != player.getObjectId())
					{
						if (player.getPowerGrade() == 0)
						{
							player.setPowerGrade(6);
						}
						player.setClanPrivileges(player.getClan().getRankPrivs(player.getPowerGrade()));
					}
					else
					{
						player.setClanPrivileges(L2Clan.CP_ALL);
						player.setPowerGrade(1);
					}
				}
				else
				{
					player.setClanPrivileges(L2Clan.CP_NOTHING);
				}
				
				player.setDeleteTimer(rset.getLong("deletetime"));
				
				player.setTitle(rset.getString("title"));
				player.setAccessLevel(rset.getInt("accesslevel"));
				player.setFistsWeaponItem(player.findFistsWeaponItem(activeClassId));
				player.setUptime(System.currentTimeMillis());
				
				curHp = rset.getDouble("curHp");
				curCp = rset.getDouble("curCp");
				curMp = rset.getDouble("curMp");
				
				player.checkRecom(rset.getInt("rec_have"), rset.getInt("rec_left"));
				
				player._classIndex = 0;
				try
				{
					player.setBaseClass(rset.getInt("base_class"));
				}
				catch (Exception e)
				{
					if (Config.ENABLE_ALL_EXCEPTIONS)
					{
						e.printStackTrace();
					}
					
					player.setBaseClass(activeClassId);
				}
				
				// Restore Subclass Data (cannot be done earlier in function)
				if (restoreSubClassData(player))
				{
					if (activeClassId != player.getBaseClass())
					{
						for (SubClass subClass : player.getSubClasses().values())
						{
							if (subClass.getClassId() == activeClassId)
							{
								player._classIndex = subClass.getClassIndex();
							}
						}
					}
				}
				if (player.getClassIndex() == 0 && activeClassId != player.getBaseClass())
				{
					// Subclass in use but doesn't exist in DB -
					// a possible restart-while-modifysubclass cheat has been attempted.
					// Switching to use base class
					player.setClassId(player.getBaseClass());
					LOG.warn("Player " + player.getName() + " reverted to base class. Possibly has tried a relogin exploit while subclassing.");
				}
				else
				{
					player._activeClass = activeClassId;
				}
				
				player.setApprentice(rset.getInt("apprentice"));
				player.setSponsor(rset.getInt("sponsor"));
				player.setLvlJoinedAcademy(rset.getInt("lvl_joined_academy"));
				player.setIsIn7sDungeon(rset.getInt("isin7sdungeon") == 1 ? true : false);
				
				player.setPunishLevel(rset.getInt("punish_level"));
				if (player.getPunishLevel() != PunishLevel.NONE)
				{
					player.setPunishTimer(rset.getLong("punish_timer"));
				}
				else
				{
					player.setPunishTimer(0);
				}
				
				try
				{
					player.getAppearance().setNameColor(Integer.decode(new StringBuilder().append("0x").append(rset.getString("name_color")).toString()).intValue());
					player.getAppearance().setTitleColor(Integer.decode(new StringBuilder().append("0x").append(rset.getString("title_color")).toString()).intValue());
				}
				catch (Exception e)
				{
					if (Config.ENABLE_ALL_EXCEPTIONS)
					{
						e.printStackTrace();
					}
				}
				
				CursedWeaponsManager.getInstance().checkPlayer(player);
				
				player.setAllianceWithVarkaKetra(rset.getInt("varka_ketra_ally"));
				
				player.setDeathPenaltyBuffLevel(rset.getInt("death_penalty_level"));
				player.setAio(rset.getInt("aio") == 1 ? true : false);
				player.setAioEndTime(rset.getLong("aio_end"));
				player.setHitmanTarget(rset.getInt("hitman_target"));
				player.setAutoLootEnabled(rset.getInt("autoloot"));
				player.setAutoLootHerbs(rset.getInt("autoloot_herbs"));
				
				if (Config.FAKE_ARMORS)
				{
					final int fakeArmor = rset.getInt("fakeArmorObjectId");
					final L2ItemInstance armorItem = player.getInventory().getItemByObjectId(fakeArmor);
					player.setFakeArmorObjectId(armorItem != null ? fakeArmor : 0);
					player.setFakeArmorItemId(armorItem != null ? armorItem.getItemId() : 0);
				}
				
				// Set the x,y,z position of the L2PcInstance and make it invisible
				player.setXYZInvisible(rset.getInt("x"), rset.getInt("y"), rset.getInt("z"));
				
				// Retrieve the name and ID of the other characters assigned to this account.
				PreparedStatement stmt = con.prepareStatement("SELECT obj_Id, char_name FROM characters WHERE account_name=? AND obj_Id<>?");
				stmt.setString(1, player._accountName);
				stmt.setInt(2, objectId);
				ResultSet chars = stmt.executeQuery();
				
				while (chars.next())
				{
					Integer charId = chars.getInt("obj_Id");
					String charName = chars.getString("char_name");
					player._chars.put(charId, charName);
				}
				
				chars.close();
				stmt.close();
				
				break;
			}
			
			rset.close();
			statement.close();
			
			if (player == null)
			{
				return null;
			}
			
			// Retrieve from the database all secondary data of this L2PcInstance
			// and reward expertise/lucky skills if necessary.
			// Note that Clan, Noblesse and Hero skills are given separately and not here.
			player.restoreCharData();
			player.rewardSkills(true);
			
			// buff and status icons
			// if (Config.STORE_SKILL_COOLTIME)
			// player.restoreEffects();
			
			// Restore pet if exists in the world
			player.setPet(L2World.getInstance().getPet(player.getObjectId()));
			if (player.getPet() != null)
			{
				player.getPet().setOwner(player);
			}
			
			// Update the overloaded status of the L2PcInstance
			player.refreshOverloaded();
			
			player.restoreFriendList();
		}
		catch (Exception e)
		{
			LOG.error("Could not restore char data:");
			e.printStackTrace();
		}
		finally
		{
			CloseUtil.close(con);
		}
		
		if (player != null)
		{
			player.fireEvent(EventType.LOAD.name, (Object[]) null);
			
			try
			{
				Thread.sleep(100);
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
			}
			
			// once restored all the skill status, update current CP, MP and HP
			player.setCurrentHpDirect(curHp);
			player.setCurrentCpDirect(curCp);
			player.setCurrentMpDirect(curMp);
		}
		return player;
	}
	
	/**
	 * Gets the mail.
	 * @return the mail
	 */
	public Forum getMail()
	{
		if (_forumMail == null)
		{
			setMail(ForumsBBSManager.getInstance().getForumByName("MailRoot").getChildByName(getName()));
			
			if (_forumMail == null)
			{
				ForumsBBSManager.getInstance().createNewForum(getName(), ForumsBBSManager.getInstance().getForumByName("MailRoot"), Forum.MAIL, Forum.OWNERONLY, getObjectId());
				setMail(ForumsBBSManager.getInstance().getForumByName("MailRoot").getChildByName(getName()));
			}
		}
		
		return _forumMail;
	}
	
	/**
	 * Sets the mail.
	 * @param forum the new mail
	 */
	public void setMail(Forum forum)
	{
		_forumMail = forum;
	}
	
	/**
	 * Gets the memo.
	 * @return the memo
	 */
	public Forum getMemo()
	{
		if (_forumMemo == null)
		{
			setMemo(ForumsBBSManager.getInstance().getForumByName("MemoRoot").getChildByName(_accountName));
			
			if (_forumMemo == null)
			{
				ForumsBBSManager.getInstance().createNewForum(_accountName, ForumsBBSManager.getInstance().getForumByName("MemoRoot"), Forum.MEMO, Forum.OWNERONLY, getObjectId());
				setMemo(ForumsBBSManager.getInstance().getForumByName("MemoRoot").getChildByName(_accountName));
			}
		}
		
		return _forumMemo;
	}
	
	/**
	 * Sets the memo.
	 * @param forum the new memo
	 */
	public void setMemo(Forum forum)
	{
		_forumMemo = forum;
	}
	
	/**
	 * Restores sub-class data for the L2PcInstance, used to check the current class index for the character.
	 * @param player the player
	 * @return true, if successful
	 */
	private static boolean restoreSubClassData(L2PcInstance player)
	{
		Connection con = null;
		
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement(RESTORE_CHAR_SUBCLASSES);
			statement.setInt(1, player.getObjectId());
			
			ResultSet rset = statement.executeQuery();
			
			while (rset.next())
			{
				SubClass subClass = new SubClass();
				subClass.setClassId(rset.getInt("class_id"));
				subClass.setLevel(rset.getByte("level"));
				subClass.setExp(rset.getLong("exp"));
				subClass.setSp(rset.getInt("sp"));
				subClass.setClassIndex(rset.getInt("class_index"));
				
				// Enforce the correct indexing of _subClasses against their class indexes.
				player.getSubClasses().put(subClass.getClassIndex(), subClass);
			}
			
			statement.close();
			rset.close();
		}
		catch (Exception e)
		{
			LOG.warn("Could not restore classes for " + player.getName() + ": " + e);
			e.printStackTrace();
		}
		finally
		{
			CloseUtil.close(con);
		}
		
		return true;
	}
	
	/**
	 * Restores secondary data for the L2PcInstance, based on the current class index.
	 */
	private void restoreCharData()
	{
		// Retrieve from the database all skills of this L2PcInstance and add them to _skills.
		restoreSkills();
		
		// Retrieve from the database all macroses of this L2PcInstance and add them to _macroses.
		_macroses.restore();
		
		// Retrieve from the database all shortCuts of this L2PcInstance and add them to _shortCuts.
		_shortCuts.restore();
		
		// Retrieve from the database all henna of this L2PcInstance and add them to _henna.
		restoreHenna();
		
		// Retrieve from the database all recom data of this L2PcInstance and add to _recomChars.
		if (Config.ALT_RECOMMEND)
		{
			restoreRecom();
		}
		
		// Retrieve from the database the recipe book of this L2PcInstance.
		if (!isSubClassActive())
		{
			restoreRecipeBook();
		}
	}
	
	private void restoreRecipeBook()
	{
		Connection con = null;
		
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT id, type FROM character_recipebook WHERE char_id=?");
			statement.setInt(1, getObjectId());
			ResultSet rset = statement.executeQuery();
			
			L2RecipeList recipe;
			while (rset.next())
			{
				recipe = RecipeTable.getInstance().getRecipeList(rset.getInt("id") - 1);
				
				if (rset.getInt("type") == 1)
				{
					registerDwarvenRecipeList(recipe, false);
				}
				else
				{
					registerCommonRecipeList(recipe, false);
				}
			}
			
			rset.close();
			statement.close();
		}
		catch (Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				LOG.error("Could not restore recipe book data:");
				e.printStackTrace();
			}
		}
		finally
		{
			CloseUtil.close(con);
		}
	}
	
	/**
	 * Store.
	 * @param force the force
	 */
	public synchronized void store(boolean force)
	{
		storeCharBase();
		storeCharSub();
		
		// Dont store effect if the char was on Offline trade
		if (!isStored())
		{
			storeEffect(force);
		}
		
		// If char is in Offline trade, setStored must be true
		if (isInOfflineMode())
		{
			setStored(true);
		}
		else
		{
			setStored(false);
		}
	}
	
	public synchronized void store()
	{
		store(true);
	}
	
	private synchronized void storeCharBase()
	{
		Connection con = null;
		try
		{
			// Get the exp, level, and sp of base class to store in base table
			int currentClassIndex = getClassIndex();
			_classIndex = 0;
			long exp = getStat().getExp();
			int level = getStat().getLevel();
			int sp = getStat().getSp();
			_classIndex = currentClassIndex;
			
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement;
			
			// Update base class
			statement = con.prepareStatement(UPDATE_CHARACTER);
			
			if (Config.FAKE_ARMORS)
			{
				statement = con.prepareStatement(UPDATE_CHARACTER2);
			}
			
			statement.setInt(1, level);
			statement.setInt(2, getMaxHp());
			statement.setDouble(3, getCurrentHp());
			statement.setInt(4, getMaxCp());
			statement.setDouble(5, getCurrentCp());
			statement.setInt(6, getMaxMp());
			statement.setDouble(7, getCurrentMp());
			statement.setInt(8, getSTR());
			statement.setInt(9, getCON());
			statement.setInt(10, getDEX());
			statement.setInt(11, getINT());
			statement.setInt(12, getMEN());
			statement.setInt(13, getWIT());
			statement.setInt(14, getAppearance().getFace());
			statement.setInt(15, getAppearance().getHairStyle());
			statement.setInt(16, getAppearance().getHairColor());
			statement.setInt(17, getHeading());
			
			if (!inObserverMode())
			{
				statement.setInt(18, getX());
				statement.setInt(19, getY());
				statement.setInt(20, getZ());
			}
			else
			{
				statement.setInt(18, _lastLoc.getX());
				statement.setInt(19, _lastLoc.getY());
				statement.setInt(20, _lastLoc.getZ());
			}
			
			statement.setLong(21, exp);
			statement.setLong(22, getExpBeforeDeath());
			statement.setInt(23, sp);
			statement.setInt(24, getKarma());
			statement.setInt(25, getPvpKills());
			statement.setInt(26, getPkKills());
			statement.setInt(27, getRecomHave());
			statement.setInt(28, getRecomLeft());
			statement.setInt(29, getClanId());
			statement.setInt(30, getMaxLoad());
			statement.setInt(31, getRace().ordinal());
			
			statement.setInt(32, getClassId().getId());
			statement.setLong(33, getDeleteTimer());
			statement.setString(34, getTitle());
			statement.setInt(35, getAccessLevel().getLevel());
			
			if (isInOfflineMode() || isOnline() == 1)
			{
				statement.setInt(36, 1);
			}
			else
			{
				statement.setInt(36, isOnline());
			}
			
			statement.setInt(37, isIn7sDungeon() ? 1 : 0);
			statement.setInt(38, getClanPrivileges());
			statement.setInt(39, getWantsPeace());
			statement.setInt(40, getBaseClass());
			
			long totalOnlineTime = _onlineTime;
			
			if (_onlineBeginTime > 0 && !_isInOfflineMode)
			{
				totalOnlineTime += (System.currentTimeMillis() - _onlineBeginTime) / 1000;
			}
			
			statement.setLong(41, totalOnlineTime);
			statement.setInt(42, getPunishLevel().value());
			statement.setLong(43, getPunishTimer());
			statement.setInt(44, isNewbie() ? 1 : 0);
			statement.setInt(45, isNoble() ? 1 : 0);
			statement.setLong(46, getPowerGrade());
			statement.setInt(47, getPledgeType());
			statement.setLong(48, getLastRecomUpdate());
			statement.setInt(49, getLvlJoinedAcademy());
			statement.setLong(50, getApprentice());
			statement.setLong(51, getSponsor());
			statement.setInt(52, getAllianceWithVarkaKetra());
			statement.setLong(53, getClanJoinExpiryTime());
			statement.setLong(54, getClanCreateExpiryTime());
			statement.setString(55, getName());
			statement.setLong(56, getDeathPenaltyBuffLevel());
			statement.setInt(57, getPcBangScore());
			
			if (isInOfflineMode())
			{
				statement.setString(58, "" + _originalNameColorOffline);
				
			}
			else
			{
				statement.setString(58, StringToHex(Integer.toHexString(getAppearance().getNameColor()).toUpperCase()));
			}
			
			statement.setString(59, StringToHex(Integer.toHexString(getAppearance().getTitleColor()).toUpperCase()));
			
			statement.setInt(60, isAio() ? 1 : 0);
			statement.setLong(61, getAioEndTime());
			statement.setInt(62, getHitmanTarget());
			statement.setInt(63, getAutoLootEnabledFromDB());
			statement.setInt(64, getAutoLootHerbsEnabledFromDB());
			
			if (!Config.FAKE_ARMORS)
			{
				statement.setInt(65, getObjectId());
			}
			else
			{
				statement.setLong(65, getFakeArmorObjectId());
				statement.setLong(66, getFakeArmorItemId());
				statement.setInt(67, getObjectId());
			}
			
			statement.execute();
			statement.close();
		}
		catch (Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				LOG.error("Could not store char base data:");
				e.printStackTrace();
			}
		}
		finally
		{
			CloseUtil.close(con);
		}
	}
	
	private synchronized void storeCharSub()
	{
		Connection con = null;
		
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement;
			
			if (getTotalSubClasses() > 0)
			{
				for (SubClass subClass : getSubClasses().values())
				{
					statement = con.prepareStatement(UPDATE_CHAR_SUBCLASS);
					statement.setLong(1, subClass.getExp());
					statement.setInt(2, subClass.getSp());
					statement.setInt(3, subClass.getLevel());
					statement.setInt(4, subClass.getClassId());
					statement.setInt(5, getObjectId());
					statement.setInt(6, subClass.getClassIndex());
					
					statement.execute();
					statement.close();
				}
			}
		}
		catch (Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				LOG.error("Could not store sub class data for " + getName() + ":");
				e.printStackTrace();
			}
		}
		finally
		{
			CloseUtil.close(con);
		}
	}
	
	private synchronized void storeEffect(boolean storeEffects)
	{
		if (!Config.STORE_SKILL_COOLTIME)
		{
			return;
		}
		
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			
			// Delete all current stored effects for char to avoid dupe
			PreparedStatement statement = con.prepareStatement(DELETE_SKILL_SAVE);
			
			statement.setInt(1, getObjectId());
			statement.setInt(2, getClassIndex());
			statement.execute();
			statement.close();
			
			int buff_index = 0;
			
			final List<Integer> storedSkills = new FastList<>();
			
			// Store all effect data along with calulated remaining
			// reuse delays for matching skills. 'restore_type'= 0.
			statement = con.prepareStatement(ADD_SKILL_SAVE);
			
			for (L2Effect effect : getAllEffects())
			{
				if (effect == null)
				{
					continue;
				}
				
				boolean chargeSkill = false;
				
				switch (effect.getEffectType())
				{
					case HEAL_OVER_TIME:
					case COMBAT_POINT_HEAL_OVER_TIME:
						continue;
					case CHARGE:
						chargeSkill = true;
				}
				
				L2Skill skill = effect.getSkill();
				if (storedSkills.contains(skill.getReuseHashCode()))
				{
					continue;
				}
				
				storedSkills.add(skill.getReuseHashCode());
				
				if (!effect.isHerbEffect() && effect.getInUse() && !skill.isToggle())
				{
					statement.setInt(1, getObjectId());
					statement.setInt(2, skill.getId());
					
					if (!chargeSkill)
					{
						statement.setInt(3, skill.getLevel());
					}
					else
					{
						final EffectCharge e = (EffectCharge) getFirstEffect(L2Effect.EffectType.CHARGE);
						if (e != null)
						{
							statement.setInt(3, e.getLevel());
						}
					}
					
					statement.setInt(4, effect.getCount());
					statement.setInt(5, effect.getTime());
					
					if (hasSkillReuse(skill.getReuseHashCode()))
					{
						TimeStamp t = getSkillReuseTimeStamp(skill.getReuseHashCode());
						statement.setLong(6, t.hasNotPassed() ? t.getReuse() : 0);
						statement.setDouble(7, t.hasNotPassed() ? t.getStamp() : 0);
					}
					else
					{
						statement.setLong(6, 0);
						statement.setDouble(7, 0);
					}
					
					statement.setInt(8, 0);
					statement.setInt(9, getClassIndex());
					statement.setInt(10, ++buff_index);
					statement.execute();
				}
			}
			
			// Store the reuse delays of remaining skills which
			// lost effect but still under reuse delay. 'restore_type' 1.
			for (int hash : getSkillReuseTimeStamps().keySet())
			{
				if (storedSkills.contains(hash))
				{
					continue;
				}
				
				TimeStamp t = getSkillReuseTimeStamps().get(hash);
				if (t != null && t.hasNotPassed())
				{
					storedSkills.add(hash);
					
					statement.setInt(1, getObjectId());
					statement.setInt(2, t.getSkillId());
					statement.setInt(3, t.getSkillLvl());
					statement.setInt(4, -1);
					statement.setInt(5, -1);
					statement.setLong(6, t.getReuse());
					statement.setDouble(7, t.getStamp());
					statement.setInt(8, 1);
					statement.setInt(9, getClassIndex());
					statement.setInt(10, ++buff_index);
					statement.execute();
				}
			}
			statement.close();
		}
		catch (final Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				LOG.error("Could not store char effect data:");
				e.printStackTrace();
			}
		}
		finally
		{
			CloseUtil.close(con);
		}
	}
	
	/**
	 * Return True if the L2PcInstance is on line.<BR>
	 * <BR>
	 * @return the int
	 */
	public int isOnline()
	{
		return _isOnline ? 1 : 0;
	}
	
	/**
	 * Checks if is in7s dungeon.
	 * @return true, if is in7s dungeon
	 */
	public boolean isIn7sDungeon()
	{
		return _isIn7sDungeon;
	}
	
	/**
	 * Add a skill to the L2PcInstance _skills and its Func objects to the calculator set of the L2PcInstance and save update in the character_skills table of the database.<BR>
	 * <BR>
	 * <B><U> Concept</U> :</B><BR>
	 * <BR>
	 * All skills own by a L2PcInstance are identified in <B>_skills</B><BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Replace oldSkill by newSkill or Add the newSkill</li>
	 * <li>If an old skill has been replaced, remove all its Func objects of L2Character calculator set</li>
	 * <li>Add Func objects of newSkill to the calculator set of the L2Character</li><BR>
	 * <BR>
	 */
	private boolean _learningSkill = false;
	
	/**
	 * Adds the skill.
	 * @param newSkill the new skill
	 * @param store the store
	 * @return the l2 skill
	 */
	public synchronized L2Skill addSkill(L2Skill newSkill, boolean store)
	{
		_learningSkill = true;
		// Add a skill to the L2PcInstance _skills and its Func objects to the calculator set of the L2PcInstance
		L2Skill oldSkill = super.addSkill(newSkill);
		
		// Add or update a L2PcInstance skill in the character_skills table of the database
		if (store)
		{
			storeSkill(newSkill, oldSkill, -1);
		}
		
		_learningSkill = false;
		
		return oldSkill;
	}
	
	public boolean isLearningSkill()
	{
		return _learningSkill;
	}
	
	public L2Skill removeSkill(L2Skill skill, boolean store)
	{
		if (store)
		{
			return removeSkill(skill);
		}
		
		return super.removeSkill(skill);
	}
	
	/**
	 * Remove a skill from the L2Character and its Func objects from calculator set of the L2Character and save update in the character_skills table of the database.<BR>
	 * <BR>
	 * <B><U> Concept</U> :</B><BR>
	 * <BR>
	 * All skills own by a L2Character are identified in <B>_skills</B><BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Remove the skill from the L2Character _skills</li>
	 * <li>Remove all its Func objects from the L2Character calculator set</li><BR>
	 * <BR>
	 * <B><U> Overriden in </U> :</B><BR>
	 * <BR>
	 * <li>L2PcInstance : Save update in the character_skills table of the database</li><BR>
	 * <BR>
	 * @param skill The L2Skill to remove from the L2Character
	 * @return The L2Skill removed
	 */
	@Override
	public L2Skill removeSkill(L2Skill skill)
	{
		// Remove a skill from the L2Character and its Func objects from calculator set of the L2Character
		L2Skill oldSkill = super.removeSkill(skill);
		
		Connection con = null;
		
		try
		{
			// Remove or update a L2PcInstance skill from the character_skills table of the database
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement;
			
			if (oldSkill != null)
			{
				statement = con.prepareStatement(DELETE_SKILL_FROM_CHAR);
				statement.setInt(1, oldSkill.getId());
				statement.setInt(2, getObjectId());
				statement.setInt(3, getClassIndex());
				statement.execute();
				statement.close();
				statement = null;
			}
		}
		catch (Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				LOG.error("Error could not delete skill:");
				e.printStackTrace();
			}
		}
		finally
		{
			CloseUtil.close(con);
		}
		
		L2ShortCut[] allShortCuts = getAllShortCuts();
		
		for (L2ShortCut sc : allShortCuts)
		{
			if (sc != null && skill != null && sc.getId() == skill.getId() && sc.getType() == L2ShortCut.TYPE_SKILL)
			{
				deleteShortCut(sc.getSlot(), sc.getPage());
			}
		}
		allShortCuts = null;
		
		return oldSkill;
	}
	
	/**
	 * Add or update a L2PcInstance skill in the character_skills table of the database. <BR>
	 * <BR>
	 * If newClassIndex > -1, the skill will be stored with that class index, not the current one.
	 * @param newSkill the new skill
	 * @param oldSkill the old skill
	 * @param newClassIndex the new class index
	 */
	private void storeSkill(L2Skill newSkill, L2Skill oldSkill, int newClassIndex)
	{
		int classIndex = _classIndex;
		
		if (newClassIndex > -1)
		{
			classIndex = newClassIndex;
		}
		
		Connection con = null;
		
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement;
			
			if (oldSkill != null && newSkill != null)
			{
				statement = con.prepareStatement(UPDATE_CHARACTER_SKILL_LEVEL);
				statement.setInt(1, newSkill.getLevel());
				statement.setInt(2, oldSkill.getId());
				statement.setInt(3, getObjectId());
				statement.setInt(4, classIndex);
				statement.execute();
				statement.close();
			}
			else if (newSkill != null)
			{
				statement = con.prepareStatement(ADD_NEW_SKILL);
				statement.setInt(1, getObjectId());
				statement.setInt(2, newSkill.getId());
				statement.setInt(3, newSkill.getLevel());
				statement.setString(4, newSkill.getName());
				statement.setInt(5, classIndex);
				statement.execute();
				statement.close();
			}
			else
			{
				LOG.warn("could not store new skill. its NULL");
			}
		}
		catch (Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				LOG.error("Error could not store char skills:");
				e.printStackTrace();
			}
		}
		finally
		{
			CloseUtil.close(con);
		}
	}
	
	public void checkAllowedSkills()
	{
		boolean foundskill = false;
		boolean dontdeleteskill = false;
		
		if (!isGM())
		{
			Collection<L2SkillLearn> skillTree = SkillTreeTable.getInstance().getAllowedSkills(getClassId());
			// loop through all skills of player
			for (L2Skill skill : getAllSkills())
			{
				int skillid = skill.getId();
				int skilllevel = skill.getLevel();
				
				foundskill = false;
				dontdeleteskill = false;
				
				// loop through all skills in players skilltree
				for (L2SkillLearn temp : skillTree)
				{
					// if the skill was found and the level is possible to obtain for his class everything is ok
					if (temp.getId() == skillid)
					{
						foundskill = true;
						dontdeleteskill = false;
					}
					// enchant skills
					if (skilllevel >= 100)
					{
						foundskill = true;
						dontdeleteskill = true;
					}
				}
				
				// exclude noble skills
				// if (isNoble() && skillid >= 325 && skillid <= 397)
				// {
				// foundskill = true;
				// dontdeleteskill = true;
				// }
				
				if (isNoble() && skillid >= 1323 && skillid <= 1327)
				{
					foundskill = true;
					dontdeleteskill = true;
				}
				
				// exclude hero skills
				if (isHero() && skillid >= 395 && skillid <= 396)
				{
					foundskill = true;
					dontdeleteskill = true;
				}
				
				if (skillid >= 1405 && skillid <= 1405)
				{
					foundskill = true;
					dontdeleteskill = true;
				}
				
				if (isHero() && skillid >= 1374 && skillid <= 1376)
				{
					foundskill = true;
					dontdeleteskill = true;
				}
				
				// exclude cursed weapon skills
				if (isCursedWeaponEquiped() && skillid == CursedWeaponsManager.getInstance().getCursedWeapon(_cursedWeaponEquipedId).getSkillId())
				{
					foundskill = true;
					dontdeleteskill = true;
				}
				
				// exclude clan skills
				if (getClan() != null && skillid >= 370 && skillid <= 391)
				{
					foundskill = true;
					dontdeleteskill = true;
				}
				
				// exclude seal of ruler / build siege hq
				if (getClan() != null && (skillid == 246 || skillid == 247))
				{
					if (getClan().getLeaderId() == getObjectId())
					{
						foundskill = true;
						dontdeleteskill = true;
					}
				}
				
				// exclude fishing skills and common skills + dwarfen craft
				if (skillid >= 1312 && skillid <= 1322)
				{
					foundskill = true;
					dontdeleteskill = true;
				}
				
				if (skillid >= 1368 && skillid <= 1373)
				{
					foundskill = true;
					dontdeleteskill = true;
				}
				
				// exclude sa / enchant bonus / penality etc. skills
				if (skillid >= 3000 && skillid < 7000)
				{
					foundskill = true;
					dontdeleteskill = true;
				}
				
				// exclude Skills from AllowedSkills in options.in
				if (Config.ALLOWED_SKILLS_LIST.contains(skillid))
				{
					foundskill = true;
					dontdeleteskill = true;
				}
				
				// exclude Donator character
				if (isDonator())
				{
					foundskill = true;
					dontdeleteskill = true;
				}
				
				// remove skill and do a lil log message
				if (!foundskill)
				{
					removeSkill(skill);
					
					if (Config.DEBUG)
					{
						LOG.warn("Character " + getName() + " of Account " + getAccountName() + " got skill " + skill.getName() + ".. Removed!");
					}
				}
				
				if (!dontdeleteskill)
				{
					if (Config.DECREASE_SKILL_LEVEL)
					{
						int level = getSkillLevel(skillid);
						L2SkillLearn learn = SkillTreeTable.getInstance().getSkillLearnBySkillIdLevel(getClassId(), skillid, level);
						
						if (learn == null)
						{
							continue;
						}
						
						// player level is too low for such skill level
						if (getLevel() < (learn.getMinLevel() - 9))
						{
							deacreaseSkillLevelOrRemove(skillid);
						}
					}
				}
			}
			sendSkillList();
		}
	}
	
	private void deacreaseSkillLevelOrRemove(int id)
	{
		int nextLevel = -1;
		
		for (L2SkillLearn sl : SkillTreeTable.getInstance().getAllowedSkills(getClassId()))
		{
			if (sl.getId() == id && nextLevel < sl.getLevel() && getLevel() >= (sl.getMinLevel() - 9))
			{
				nextLevel = sl.getLevel();
			}
		}
		
		if (nextLevel == -1) // there is no lower skill
		{
			removeSkill(_skills.get(id), true);
		}
		else
		{
			addSkill(SkillTable.getInstance().getInfo(id, nextLevel), true);
		}
	}
	
	public synchronized void restoreSkills()
	{
		Connection con = null;
		
		try
		{
			if (!Config.KEEP_SUBCLASS_SKILLS)
			{
				// Retrieve all skills of this L2PcInstance from the database
				con = L2DatabaseFactory.getInstance().getConnection();
				PreparedStatement statement = con.prepareStatement(RESTORE_SKILLS_FOR_CHAR);
				statement.setInt(1, getObjectId());
				statement.setInt(2, getClassIndex());
				ResultSet rset = statement.executeQuery();
				
				// Go though the recordset of this SQL query
				while (rset.next())
				{
					int id = rset.getInt("skill_id");
					int level = rset.getInt("skill_level");
					
					if (id > 9000)
					{
						continue; // fake skills for base stats
					}
					
					// Create a L2Skill object for each record
					L2Skill skill = SkillTable.getInstance().getInfo(id, level);
					
					// Add the L2Skill object to the L2Character _skills and its Func objects to the calculator set of the L2Character
					super.addSkill(skill);
				}
				
				rset.close();
				statement.close();
			}
			else
			{
				// Retrieve all skills of this L2PcInstance from the database
				con = L2DatabaseFactory.getInstance().getConnection();
				PreparedStatement statement = con.prepareStatement(RESTORE_SKILLS_FOR_CHAR_ALT_SUBCLASS);
				statement.setInt(1, getObjectId());
				ResultSet rset = statement.executeQuery();
				
				// Go though the recordset of this SQL query
				while (rset.next())
				{
					int id = rset.getInt("skill_id");
					int level = rset.getInt("skill_level");
					
					if (id > 9000)
					{
						continue; // fake skills for base stats
					}
					
					// Create a L2Skill object for each record
					L2Skill skill = SkillTable.getInstance().getInfo(id, level);
					
					// Add the L2Skill object to the L2Character _skills and its Func objects to the calculator set of the L2Character
					super.addSkill(skill);
				}
				
				rset.close();
				statement.close();
			}
			
		}
		catch (Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				LOG.error("Could not restore character skills:");
				e.printStackTrace();
			}
		}
		finally
		{
			CloseUtil.close(con);
		}
	}
	
	public void restoreEffects()
	{
		restoreEffects(true);
	}
	
	/**
	 * Retrieve from the database all skill effects of this L2PcInstance and add them to the player.<BR>
	 * <BR>
	 * @param activateEffects
	 */
	public void restoreEffects(boolean activateEffects)
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement;
			ResultSet rset;
			
			statement = con.prepareStatement(RESTORE_SKILL_SAVE);
			statement.setInt(1, getObjectId());
			statement.setInt(2, getClassIndex());
			rset = statement.executeQuery();
			
			while (rset.next())
			{
				int effectCount = rset.getInt("effect_count");
				int effectCurTime = rset.getInt("effect_cur_time");
				long reuseDelay = rset.getLong("reuse_delay");
				long systime = rset.getLong("systime");
				int restoreType = rset.getInt("restore_type");
				int skillId = rset.getInt("skill_id");
				int skillLvl = rset.getInt("skill_level");
				
				final L2Skill skill = SkillTable.getInstance().getInfo(skillId, skillLvl);
				if (skill == null)
				{
					continue;
				}
				
				final long remainingTime = systime - System.currentTimeMillis();
				if (remainingTime > 0)
				{
					disableSkill(skill, remainingTime);
					addTimeStamp(skill, reuseDelay, systime);
				}
				
				if (restoreType > 0)
				{
					continue;
				}
				
				if (activateEffects)
				{
					for (L2Effect effect : skill.getEffects(this, this))
					{
						if (effect.getEffectType() == L2Effect.EffectType.CHARGE)
						{
							final EffectCharge e = (EffectCharge) getFirstEffect(L2Effect.EffectType.CHARGE);
							if (e != null)
							{
								e.setNumCharges(skillLvl);
							}
						}
						
						effect.setCount(effectCount);
						effect.setFirstTime(effectCurTime);
					}
				}
			}
			rset.close();
			statement.close();
			
			/**
			 * Restore Type 1 The remaining skills lost effect upon logout but were still under a high reuse delay.
			 */
			statement = con.prepareStatement(DELETE_SKILL_SAVE);
			statement.setInt(1, getObjectId());
			statement.setInt(2, getClassIndex());
			statement.executeUpdate();
			statement.close();
		}
		catch (final Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				LOG.error("Could not restore active effect data:");
				e.printStackTrace();
			}
		}
		finally
		{
			CloseUtil.close(con);
		}
		
		updateEffectIcons();
	}
	
	/**
	 * Retrieve from the database all Henna of this L2PcInstance, add them to _henna and calculate stats of the L2PcInstance.<BR>
	 * <BR>
	 */
	private void restoreHenna()
	{
		Connection con = null;
		
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement(RESTORE_CHAR_HENNAS);
			statement.setInt(1, getObjectId());
			statement.setInt(2, getClassIndex());
			ResultSet rset = statement.executeQuery();
			
			for (int i = 0; i < 3; i++)
			{
				_henna[i] = null;
			}
			
			while (rset.next())
			{
				int slot = rset.getInt("slot");
				
				if (slot < 1 || slot > 3)
				{
					continue;
				}
				
				int symbol_id = rset.getInt("symbol_id");
				
				L2HennaInstance sym = null;
				
				if (symbol_id != 0)
				{
					L2Henna tpl = HennaTable.getInstance().getTemplate(symbol_id);
					
					if (tpl != null)
					{
						sym = new L2HennaInstance(tpl);
						_henna[slot - 1] = sym;
						tpl = null;
						sym = null;
					}
				}
			}
			
			rset.close();
			statement.close();
			rset = null;
			statement = null;
		}
		catch (Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				LOG.error("could not restore henna:");
				e.printStackTrace();
			}
		}
		finally
		{
			CloseUtil.close(con);
		}
		
		// Calculate Henna modifiers of this L2PcInstance
		recalcHennaStats();
	}
	
	/**
	 * Retrieve from the database all Recommendation data of this L2PcInstance, add to _recomChars and calculate stats of the L2PcInstance.<BR>
	 * <BR>
	 */
	private void restoreRecom()
	{
		Connection con = null;
		
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement(RESTORE_CHAR_RECOMS);
			statement.setInt(1, getObjectId());
			ResultSet rset = statement.executeQuery();
			while (rset.next())
			{
				_recomChars.add(rset.getInt("target_id"));
			}
			
			rset.close();
			statement.close();
		}
		catch (Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				LOG.error("could not restore recommendations:");
				e.printStackTrace();
			}
		}
		finally
		{
			CloseUtil.close(con);
		}
	}
	
	/**
	 * Return the number of Henna empty slot of the L2PcInstance.<BR>
	 * <BR>
	 * @return the henna empty slots
	 */
	public int getHennaEmptySlots()
	{
		int totalSlots = 1 + getClassId().level();
		
		for (int i = 0; i < 3; i++)
		{
			if (_henna[i] != null)
			{
				totalSlots--;
			}
		}
		
		if (totalSlots <= 0)
		{
			return 0;
		}
		
		return totalSlots;
	}
	
	/**
	 * Remove a Henna of the L2PcInstance, save update in the character_hennas table of the database and send Server->Client HennaInfo/UserInfo packet to this L2PcInstance.<BR>
	 * <BR>
	 * @param slot the slot
	 * @return true, if successful
	 */
	public boolean removeHenna(int slot)
	{
		if (slot < 1 || slot > 3)
		{
			return false;
		}
		
		slot--;
		
		if (_henna[slot] == null)
		{
			return false;
		}
		
		L2HennaInstance henna = _henna[slot];
		_henna[slot] = null;
		
		Connection con = null;
		
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement(DELETE_CHAR_HENNA);
			statement.setInt(1, getObjectId());
			statement.setInt(2, slot + 1);
			statement.setInt(3, getClassIndex());
			statement.execute();
			statement.close();
			statement = null;
		}
		catch (Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				LOG.error("could not remove char henna:");
				e.printStackTrace();
			}
		}
		finally
		{
			CloseUtil.close(con);
		}
		
		// Calculate Henna modifiers of this L2PcInstance
		recalcHennaStats();
		
		// Send Server->Client HennaInfo packet to this L2PcInstance
		sendPacket(new HennaInfo(this));
		
		// Send Server->Client UserInfo packet to this L2PcInstance
		sendPacket(new UserInfo(this));
		
		// Add the recovered dyes to the player's inventory and notify them.
		getInventory().addItem("Henna", henna.getItemIdDye(), henna.getAmountDyeRequire() / 2, this, null);
		
		SystemMessage sm = new SystemMessage(SystemMessageId.EARNED_S2_S1_S);
		sm.addItemName(henna.getItemIdDye());
		sm.addNumber(henna.getAmountDyeRequire() / 2);
		sendPacket(sm);
		
		return true;
	}
	
	/**
	 * Add a Henna to the L2PcInstance, save update in the character_hennas table of the database and send Server->Client HennaInfo/UserInfo packet to this L2PcInstance.<BR>
	 * <BR>
	 * @param henna the henna
	 * @return true, if successful
	 */
	public boolean addHenna(L2HennaInstance henna)
	{
		if (getHennaEmptySlots() == 0)
		{
			sendMessage("You may not have more than three equipped symbols at a time.");
			return false;
		}
		
		// int slot = 0;
		for (int i = 0; i < 3; i++)
		{
			if (_henna[i] == null)
			{
				_henna[i] = henna;
				
				// Calculate Henna modifiers of this L2PcInstance
				recalcHennaStats();
				
				Connection con = null;
				
				try
				{
					con = L2DatabaseFactory.getInstance().getConnection();
					PreparedStatement statement = con.prepareStatement(ADD_CHAR_HENNA);
					statement.setInt(1, getObjectId());
					statement.setInt(2, henna.getSymbolId());
					statement.setInt(3, i + 1);
					statement.setInt(4, getClassIndex());
					statement.execute();
					statement.close();
				}
				catch (Exception e)
				{
					if (Config.ENABLE_ALL_EXCEPTIONS)
					{
						LOG.error("could not save char henna:");
						e.printStackTrace();
					}
				}
				finally
				{
					CloseUtil.close(con);
				}
				
				// Send Server->Client HennaInfo packet to this L2PcInstance
				HennaInfo hi = new HennaInfo(this);
				sendPacket(hi);
				
				// Send Server->Client UserInfo packet to this L2PcInstance
				UserInfo ui = new UserInfo(this);
				sendPacket(ui);
				
				getInventory().refreshWeight();
				
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Calculate Henna modifiers of this L2PcInstance.<BR>
	 * <BR>
	 */
	private void recalcHennaStats()
	{
		_hennaINT = 0;
		_hennaSTR = 0;
		_hennaCON = 0;
		_hennaMEN = 0;
		_hennaWIT = 0;
		_hennaDEX = 0;
		
		for (int i = 0; i < 3; i++)
		{
			if (_henna[i] == null)
			{
				continue;
			}
			_hennaINT += _henna[i].getStatINT();
			_hennaSTR += _henna[i].getStatSTR();
			_hennaMEN += _henna[i].getStatMEM();
			_hennaCON += _henna[i].getStatCON();
			_hennaWIT += _henna[i].getStatWIT();
			_hennaDEX += _henna[i].getStatDEX();
		}
		
		if (_hennaINT > 5)
		{
			_hennaINT = 5;
		}
		
		if (_hennaSTR > 5)
		{
			_hennaSTR = 5;
		}
		
		if (_hennaMEN > 5)
		{
			_hennaMEN = 5;
		}
		
		if (_hennaCON > 5)
		{
			_hennaCON = 5;
		}
		
		if (_hennaWIT > 5)
		{
			_hennaWIT = 5;
		}
		
		if (_hennaDEX > 5)
		{
			_hennaDEX = 5;
		}
	}
	
	/**
	 * Return the Henna of this L2PcInstance corresponding to the selected slot.<BR>
	 * <BR>
	 * @param slot the slot
	 * @return the hennas
	 */
	public L2HennaInstance getHennas(int slot)
	{
		if (slot < 1 || slot > 3)
		{
			return null;
		}
		
		return _henna[slot - 1];
	}
	
	/**
	 * Return the INT Henna modifier of this L2PcInstance.<BR>
	 * <BR>
	 * @return the henna stat int
	 */
	public int getHennaStatINT()
	{
		return _hennaINT;
	}
	
	/**
	 * Return the STR Henna modifier of this L2PcInstance.<BR>
	 * <BR>
	 * @return the henna stat str
	 */
	public int getHennaStatSTR()
	{
		return _hennaSTR;
	}
	
	/**
	 * Return the CON Henna modifier of this L2PcInstance.<BR>
	 * <BR>
	 * @return the henna stat con
	 */
	public int getHennaStatCON()
	{
		return _hennaCON;
	}
	
	/**
	 * Return the MEN Henna modifier of this L2PcInstance.<BR>
	 * <BR>
	 * @return the henna stat men
	 */
	public int getHennaStatMEN()
	{
		return _hennaMEN;
	}
	
	/**
	 * Return the WIT Henna modifier of this L2PcInstance.<BR>
	 * <BR>
	 * @return the henna stat wit
	 */
	public int getHennaStatWIT()
	{
		return _hennaWIT;
	}
	
	/**
	 * Return the DEX Henna modifier of this L2PcInstance.<BR>
	 * <BR>
	 * @return the henna stat dex
	 */
	public int getHennaStatDEX()
	{
		return _hennaDEX;
	}
	
	/**
	 * Return True if the L2PcInstance is autoAttackable.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Check if the attacker isn't the L2PcInstance Pet</li>
	 * <li>Check if the attacker is L2MonsterInstance</li>
	 * <li>If the attacker is a L2PcInstance, check if it is not in the same party</li>
	 * <li>Check if the L2PcInstance has Karma</li>
	 * <li>If the attacker is a L2PcInstance, check if it is not in the same siege clan (Attacker, Defender)</li> <BR>
	 * <BR>
	 * @param attacker the attacker
	 * @return true, if is auto attackable
	 */
	
	@Override
	public boolean isAutoAttackable(L2Character attacker)
	{
		if (attacker == this || attacker == getPet())
		{
			return false;
		}
		
		// Check if the attacker is a L2MonsterInstance
		if (attacker instanceof L2MonsterInstance)
		{
			return true;
		}
		
		// Check if the attacker is not in the same party, excluding duels like L2OFF
		if (getParty() != null && getParty().getPartyMembers().contains(attacker) && !(getDuelState() == Duel.DUELSTATE_DUELLING && getDuelId() == ((L2PcInstance) attacker).getDuelId()))
		{
			return false;
		}
		
		// Check if the attacker is in olympia and olympia start
		if (attacker instanceof L2PcInstance && ((L2PcInstance) attacker).isInOlympiadMode())
		{
			if (isInOlympiadMode() && isOlympiadStart() && ((L2PcInstance) attacker).getOlympiadGameId() == getOlympiadGameId())
			{
				if (isFakeDeath())
				{
					return false;
				}
				return true;
			}
			return false;
		}
		
		// Check if the attacker is not in the same clan, excluding duels like L2OFF
		if (getClan() != null && attacker != null && getClan().isMember(attacker.getName()) && !(getDuelState() == Duel.DUELSTATE_DUELLING && getDuelId() == ((L2PcInstance) attacker).getDuelId()))
		{
			return false;
		}
		
		// Ally check
		if (attacker instanceof L2PlayableInstance)
		{
			L2PcInstance player = null;
			if (attacker instanceof L2PcInstance)
			{
				player = (L2PcInstance) attacker;
			}
			else if (attacker instanceof L2Summon)
			{
				player = ((L2Summon) attacker).getOwner();
			}
			
			// Check if the attacker is not in the same ally, excluding duels like L2OFF
			if (player != null && getAllyId() != 0 && player.getAllyId() != 0 && getAllyId() == player.getAllyId() && !(getDuelState() == Duel.DUELSTATE_DUELLING && getDuelId() == player.getDuelId()))
			{
				return false;
			}
		}
		
		if (attacker instanceof L2PlayableInstance && isInFunEvent())
		{
			
			L2PcInstance player = null;
			if (attacker instanceof L2PcInstance)
			{
				player = (L2PcInstance) attacker;
			}
			else if (attacker instanceof L2Summon)
			{
				player = ((L2Summon) attacker).getOwner();
			}
			
			if (player != null)
			{
				
				if (player.isInFunEvent())
				{
					
					// checks for events
					if ((_inEventTvT && player._inEventTvT && TvT.is_started() && !_teamNameTvT.equals(player._teamNameTvT)) || (_inEventCTF && player._inEventCTF && CTF.is_started() && !_teamNameCTF.equals(player._teamNameCTF)) || (_inEventDM && player._inEventDM && DM.is_started())
						|| (_inEventVIP && player._inEventVIP && VIP._started))
					{
						return true;
					}
					return false;
				}
				return false;
			}
		}
		
		if (L2Character.isInsidePeaceZone(attacker, this))
		{
			return false;
		}
		
		// Check if the L2PcInstance has Karma
		if (getKarma() > 0 || getPvpFlag() > 0)
		{
			return true;
		}
		
		// Check if the attacker is a L2PcInstance
		if (attacker instanceof L2PcInstance)
		{
			// is AutoAttackable if both players are in the same duel and the duel is still going on
			if (getDuelState() == Duel.DUELSTATE_DUELLING && getDuelId() == ((L2PcInstance) attacker).getDuelId())
			{
				return true;
			}
			// Check if the L2PcInstance is in an arena or a siege area
			if (isInsideZone(ZoneId.ZONE_PVP) && ((L2PcInstance) attacker).isInsideZone(ZoneId.ZONE_PVP))
			{
				return true;
			}
			
			if (getClan() != null)
			{
				Siege siege = SiegeManager.getInstance().getSiege(getX(), getY(), getZ());
				FortSiege fortsiege = FortSiegeManager.getInstance().getSiege(getX(), getY(), getZ());
				if (siege != null)
				{
					// Check if a siege is in progress and if attacker and the L2PcInstance aren't in the Defender clan
					if (siege.checkIsDefender(((L2PcInstance) attacker).getClan()) && siege.checkIsDefender(getClan()))
					{
						siege = null;
						return false;
					}
					
					// Check if a siege is in progress and if attacker and the L2PcInstance aren't in the Attacker clan
					if (siege.checkIsAttacker(((L2PcInstance) attacker).getClan()) && siege.checkIsAttacker(getClan()))
					{
						siege = null;
						return false;
					}
				}
				if (fortsiege != null)
				{
					// Check if a siege is in progress and if attacker and the L2PcInstance aren't in the Defender clan
					if (fortsiege.checkIsDefender(((L2PcInstance) attacker).getClan()) && fortsiege.checkIsDefender(getClan()))
					{
						fortsiege = null;
						return false;
					}
					
					// Check if a siege is in progress and if attacker and the L2PcInstance aren't in the Attacker clan
					if (fortsiege.checkIsAttacker(((L2PcInstance) attacker).getClan()) && fortsiege.checkIsAttacker(getClan()))
					{
						fortsiege = null;
						return false;
					}
				}
				
				// Check if clan is at war
				if (getClan() != null && ((L2PcInstance) attacker).getClan() != null && getClan().isAtWarWith(((L2PcInstance) attacker).getClanId()) && getWantsPeace() == 0 && ((L2PcInstance) attacker).getWantsPeace() == 0 && !isAcademyMember())
				{
					return true;
				}
			}
			
		}
		else if (attacker instanceof L2SiegeGuardInstance)
		{
			if (getClan() != null)
			{
				Siege siege = SiegeManager.getInstance().getSiege(this);
				return siege != null && siege.checkIsAttacker(getClan());
			}
		}
		else if (attacker instanceof L2FortSiegeGuardInstance)
		{
			if (getClan() != null)
			{
				FortSiege fortsiege = FortSiegeManager.getInstance().getSiege(this);
				return fortsiege != null && fortsiege.checkIsAttacker(getClan());
			}
		}
		
		return false;
	}
	
	/**
	 * Check if the active L2Skill can be casted.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Check if the skill isn't toggle and is offensive</li>
	 * <li>Check if the target is in the skill cast range</li>
	 * <li>Check if the skill is Spoil type and if the target isn't already spoiled</li>
	 * <li>Check if the caster owns enought consummed Item, enough HP and MP to cast the skill</li>
	 * <li>Check if the caster isn't sitting</li>
	 * <li>Check if all skills are enabled and this skill is enabled</li><BR>
	 * <BR>
	 * <li>Check if the caster own the weapon needed</li><BR>
	 * <BR>
	 * <li>Check if the skill is active</li><BR>
	 * <BR>
	 * <li>Check if all casting conditions are completed</li><BR>
	 * <BR>
	 * <li>Notify the AI with AI_INTENTION_CAST and target</li><BR>
	 * <BR>
	 * @param skill The L2Skill to use
	 * @param ctrlPressed used to force ATTACK on players
	 * @param shiftPressed used to prevent movement, if not in range
	 */
	public void useMagic(L2Skill skill, boolean ctrlPressed, boolean shiftPressed)
	{
		// Conditions -----------------------
		if (skill == null || isTeleporting() || isDead() || (isCursedWeaponEquipped() && !skill.isDemonicSkill()) || skill.isPassive())
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (inObserverMode())
		{
			sendPacket(new SystemMessage(SystemMessageId.OBSERVERS_CANNOT_PARTICIPATE));
			abortCast();
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		// Check if the caster is sitting
		if (isSitting() && !skill.isPotion())
		{
			sendPacket(new SystemMessage(SystemMessageId.CANT_MOVE_SITTING));
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		// Can't use Hero and resurrect skills during Olympiad
		if (isInOlympiadMode() && (skill.isHeroSkill() || skill.getSkillType() == SkillType.RESURRECT))
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.THIS_SKILL_IS_NOT_AVAILABLE_FOR_THE_OLYMPIAD_EVENT);
			sendPacket(sm);
			return;
		}
		
		// Like L2OFF you can't use skills when you are attacking now
		if (isAttackingNow() && !skill.isPotion())
		{
			L2ItemInstance rhand = getInventory().getPaperdollItem(Inventory.PAPERDOLL_RHAND);
			if ((rhand != null && rhand.getItemType() == L2WeaponType.BOW))
			{
				return;
			}
		}
		
		// -------------------------------------
		
		// Queu skill
		if (isCastingNow())
		{
			SkillDat currentSkill = getCurrentSkill();
			if (currentSkill != null && skill.getId() == currentSkill.getSkillId())
			{
				sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
			
			// Create a new SkillDat object and queue it in the player _queuedSkill
			setQueuedSkill(skill, ctrlPressed, shiftPressed);
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		setCurrentSkill(skill, ctrlPressed, shiftPressed);
		
		if (getQueuedSkill() != null) // wiping out previous values, after casting has been aborted
		{
			setQueuedSkill(null, false, false);
		}
		
		int skill_id = skill.getId();
		// Check if the skill type is TOGGLE
		if (skill.isToggle())
		{
			// Like L2OFF you can't use fake death if you are mounted
			if (skill.getId() == 60 && isMounted())
			{
				return;
			}
			
			L2Effect effect = getFirstEffect(skill);
			
			// Like L2OFF toogle skills have little delay
			if (TOGGLE_USE != 0 && TOGGLE_USE + 400 > System.currentTimeMillis())
			{
				TOGGLE_USE = 0;
				sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
			
			TOGGLE_USE = System.currentTimeMillis();
			if (effect != null)
			{
				effect.exit(false);
				sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
		}
		
		// Check if it's okay to summon siege pets
		// siege golem (13), Wild Hog Cannon (299), Swoop Cannon (448)
		if ((skill_id == 13 || skill_id == 299 || skill_id == 448) && !SiegeManager.getInstance().checkIfOkToSummon(this, false) && !FortSiegeManager.getInstance().checkIfOkToSummon(this, false))
		{
			return;
		}
		
		// triggered skills cannot be used directly
		if (_triggeredSkills.size() > 0)
		{
			if (_triggeredSkills.get(skill.getId()) != null)
			{
				sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
		}
		
		if (isSkillDisabled(skill))
		{
			if (hasSkillReuse(skill.getReuseHashCode()))
			{
				int remainingTime = (int) (getSkillRemainingReuseTime(skill.getReuseHashCode()) / 1000);
				int hours = remainingTime / 3600;
				int minutes = (remainingTime % 3600) / 60;
				int seconds = (remainingTime % 60);
				
				if (hours > 0)
				{
					sendMessage("There are " + hours + " hour(s), " + minutes + " minute(s), and " + seconds + " second(s) remaining in " + skill.getName() + "'s re-use time.");
				}
				else if (minutes > 0)
				{
					sendMessage("There are " + minutes + " minute(s), " + seconds + " second(s) remaining in " + skill.getName() + "'s re-use time.");
				}
				else
				{
					sendMessage("There are " + seconds + " second(s) remaining in " + skill.getName() + "'s re-use time.");
				}
				
				SystemMessage sm = new SystemMessage(SystemMessageId.S1_PREPARED_FOR_REUSE);
				sm.addSkillName(skill.getId(), skill.getLevel());
				sendPacket(sm);
			}
			return;
		}
		
		L2Object target = null;
		
		SkillTargetType skillTargetType = skill.getTargetType();
		SkillType skillType = skill.getSkillType();
		Location worldPosition = getCurrentSkillWorldPosition();
		
		if (skillTargetType == SkillTargetType.TARGET_GROUND && worldPosition == null)
		{
			LOG.info("WorldPosition is null for skill: " + skill.getName() + ", player: " + getName() + ".");
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		switch (skillTargetType)
		{
			case TARGET_AREA_CORPSE_MOB:
				if (skillType == L2Skill.SkillType.SWEEP)
				{
					target = this;
					break;
				}
				target = getTarget();
				break;
			case TARGET_AURA:
			case TARGET_FRONT_AURA:
			case TARGET_BEHIND_AURA:
				// case TARGET_AREA_UNDEAD:
				// case TARGET_AREA_CORPSE_MOB:
			case TARGET_PARTY:
			case TARGET_ALLY:
			case TARGET_CORPSE_ALLY:
			case TARGET_CLAN:
			case TARGET_CORPSE_CLAN:
			case TARGET_GROUND:
			case TARGET_SELF:
			{
				target = this;
				break;
			}
			case TARGET_PET:
			{
				target = getPet();
				break;
			}
			default:
			{
				target = getTarget();
				break;
			}
		}
		
		// Check the validity of the target
		if (target == null)
		{
			sendPacket(new SystemMessage(SystemMessageId.TARGET_CANT_FOUND));
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		// Check if the spell consume an item
		if (skill.getItemConsume() > 0)
		{
			// Get the L2ItemInstance consumed by the spell
			L2ItemInstance requiredItems = getInventory().getItemByItemId(skill.getItemConsumeId());
			
			// Check if the caster owns enough consumed Item to cast
			if (requiredItems == null || requiredItems.getCount() < skill.getItemConsume())
			{
				// Checked: when a summon skill failed, server show required consume item count
				if (skillType == L2Skill.SkillType.SUMMON)
				{
					SystemMessage sm = new SystemMessage(SystemMessageId.SUMMONING_SERVITOR_COSTS_S2_S1);
					sm.addItemName(skill.getItemConsumeId());
					sm.addNumber(skill.getItemConsume());
					sendPacket(sm);
				}
				else
				{
					// Send a System Message to the caster
					sendPacket(new SystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
				}
				return;
			}
		}
		
		// Player can't heal rb config
		if (!Config.PLAYERS_CAN_HEAL_RB && !isGM() && (target instanceof L2RaidBossInstance || target instanceof L2GrandBossInstance) && (skill.getSkillType() == SkillType.HEAL || skill.getSkillType() == SkillType.HEAL_PERCENT))
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		// Player can't burn mana rb config
		if (!Config.PLAYERS_CAN_BURN_MANA_RB && !isGM() && (target instanceof L2RaidBossInstance || target instanceof L2GrandBossInstance) && (skill.getSkillType() == SkillType.MANADAM))
		{
			this.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (target instanceof L2NpcInstance && Config.DISABLE_ATTACK_NPC_TYPE)
		{
			String mobtype = ((L2NpcInstance) target).getTemplate().type;
			if (!Config.LIST_ALLOWED_NPC_TYPES.contains(mobtype))
			{
				SystemMessage sm = new SystemMessage(SystemMessageId.S1_S2);
				sm.addString("Npc type " + mobtype + " has protection and no attack allowed.");
				sendPacket(sm);
				sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
		}
		
		// Like L2OFF you can't heal random purple people without using CTRL
		SkillDat skilldat = getCurrentSkill();
		if (skilldat != null && skill.getSkillType() == SkillType.HEAL && !skilldat.isCtrlPressed() && target instanceof L2PcInstance && ((L2PcInstance) target).getPvpFlag() == 1 && this != target)
		{
			if ((getClanId() == 0 || ((L2PcInstance) target).getClanId() == 0) || (getClanId() != ((L2PcInstance) target).getClanId()))
			{
				if ((getAllyId() == 0 || ((L2PcInstance) target).getAllyId() == 0) || (getAllyId() != ((L2PcInstance) target).getAllyId()))
				{
					if ((getParty() == null || ((L2PcInstance) target).getParty() == null) || (!getParty().equals(((L2PcInstance) target).getParty())))
					{
						sendPacket(new SystemMessage(SystemMessageId.INCORRECT_TARGET));
						sendPacket(ActionFailed.STATIC_PACKET);
						return;
					}
				}
			}
		}
		
		// Are the target and the player in the same duel?
		if (isInDuel())
		{
			if (!(target instanceof L2PcInstance && ((L2PcInstance) target).getDuelId() == getDuelId()) && !(target instanceof L2SummonInstance && ((L2Summon) target).getOwner().getDuelId() == getDuelId()))
			{
				sendMessage("You cannot do this while duelling.");
				sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
		}
		
		// Pk protection config
		if (skill.isOffensive() && !isGM() && target instanceof L2PcInstance && ((L2PcInstance) target).getPvpFlag() == 0 && ((L2PcInstance) target).getKarma() == 0 && (getLevel() < Config.ALT_PLAYER_PROTECTION_LEVEL || ((L2PcInstance) target).getLevel() < Config.ALT_PLAYER_PROTECTION_LEVEL))
		{
			sendMessage("You can't hit a player that is lower level from you. Target's level: " + String.valueOf(Config.ALT_PLAYER_PROTECTION_LEVEL) + ".");
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (skill.isOffensive() && target != this && target instanceof L2PcInstance && !isCursedWeaponEquiped() && ((L2PcInstance) target).getSiegeState() == 0 && (!checkAntiFarm((L2PcInstance) target)))
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		// Check if all skills are disabled
		if (isAllSkillsDisabled() && !getAccessLevel().allowPeaceAttack())
		{
			// Send a Server->Client packet ActionFailed to the L2PcInstance
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		// prevent casting signets to peace zone
		if (skill.getSkillType() == SkillType.SIGNET || skill.getSkillType() == SkillType.SIGNET_CASTTIME)
		{
			if (isInsidePeaceZone(this))
			{
				SystemMessage sm = new SystemMessage(SystemMessageId.S1_CANNOT_BE_USED);
				sm.addSkillName(skill_id);
				sendPacket(sm);
				return;
			}
		}
		
		// Like L2OFF if you are mounted on wyvern you can't use own skills
		if (isFlying())
		{
			if (skill_id != 327 && skill_id != 4289 && !skill.isPotion())
			{
				sendMessage("You cannot use skills while riding a wyvern.");
				return;
			}
		}
		
		// Like L2OFF if you have a summon you can't summon another one (ignore cubics)
		if (skillType == L2Skill.SkillType.SUMMON && skill instanceof L2SkillSummon && !((L2SkillSummon) skill).isCubic())
		{
			if (getPet() != null || isMounted())
			{
				sendPacket(new SystemMessage(SystemMessageId.YOU_ALREADY_HAVE_A_PET));
				return;
			}
		}
		
		if (skill.getNumCharges() > 0 && skill.getSkillType() != SkillType.CHARGE && skill.getSkillType() != SkillType.CHARGEDAM && skill.getSkillType() != SkillType.CHARGE_EFFECT && skill.getSkillType() != SkillType.PDAM)
		{
			EffectCharge effect = (EffectCharge) getFirstEffect(L2Effect.EffectType.CHARGE);
			if (effect == null || effect.numCharges < skill.getNumCharges())
			{
				sendPacket(new SystemMessage(SystemMessageId.S1_PREPARED_FOR_REUSE));
				return;
			}
			effect.numCharges -= skill.getNumCharges();
			sendPacket(new EtcStatusUpdate(this));
			
			if (effect.numCharges == 0)
			{
				effect.exit(false);
			}
		}
		// ************************************* Check Casting Conditions *******************************************
		// Check if the caster own the weapon needed
		if (!skill.getWeaponDependancy(this))
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		// Check if all casting conditions are completed
		if (!skill.checkCondition(this, target, false))
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		// ************************************* Check Player State *******************************************
		// Check if the player use "Fake Death" skill
		if (isAlikeDead() && !skill.isPotion() && skill.getSkillType() != L2Skill.SkillType.FAKE_DEATH)
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		// Only fishing skills are available
		if (isFishing() && skillType != SkillType.PUMPING && skillType != SkillType.REELING && skillType != SkillType.FISHING)
		{
			sendPacket(new SystemMessage(SystemMessageId.ONLY_FISHING_SKILLS_NOW));
			return;
		}
		
		// ************************************* Check Skill Type *******************************************
		// Check if this is offensive magic skill
		if (skill.isOffensive())
		{
			boolean peace = isInsidePeaceZone(this, target);
			// Like L2OFF you can use cupid bow skills on peace zone
			// Like L2OFF people can use TARGET_AURE skills on peace zone
			if (peace && (skill.getId() != 3261 && skill.getId() != 3260 && skill.getId() != 3262 && skillTargetType != SkillTargetType.TARGET_AURA))
			{
				sendPacket(new SystemMessage(SystemMessageId.TARGET_IN_PEACEZONE));
				sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
			
			// if L2PcInstance is in Olympia and the match isn't already start, send a Server->Client packet ActionFailed
			if (isInOlympiadMode() && !isOlympiadStart() && skillTargetType != SkillTargetType.TARGET_AURA)
			{
				sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
			
			// Check if a Forced ATTACK is in progress on non-attackable target
			if (!target.isAutoAttackable(this) && !ctrlPressed)
			{
				switch (skillTargetType)
				{
					case TARGET_AURA:
					case TARGET_FRONT_AURA:
					case TARGET_BEHIND_AURA:
					case TARGET_AREA_UNDEAD:
					case TARGET_AREA_CORPSE_MOB:
					case TARGET_PARTY:
					case TARGET_ALLY:
					case TARGET_CORPSE_ALLY:
					case TARGET_CLAN:
					case TARGET_CORPSE_CLAN:
					case TARGET_GROUND:
					case TARGET_SELF:
					{
						break;
					}
					default:
					{
						if ((_inEventTvT && TvT.is_started()) || (_inEventDM && !DM.is_started()) || (_inEventCTF && !CTF.is_started()) || (_inEventVIP && !VIP._started))
						{
							sendPacket(ActionFailed.STATIC_PACKET);
							return;
						}
						
						sendPacket(ActionFailed.STATIC_PACKET);
						return;
					}
				}
			}
			// Check if the target is in the skill cast range
			if (shiftPressed)
			{
				// Calculate the distance between the L2PcInstance and the target
				if (skillTargetType == SkillTargetType.TARGET_GROUND)
				{
					if (!isInsideRadius(getCurrentSkillWorldPosition().getX(), getCurrentSkillWorldPosition().getY(), getCurrentSkillWorldPosition().getZ(), skill.getCastRange() + getTemplate().getCollisionRadius(), false, false))
					{
						sendPacket(SystemMessageId.TARGET_TOO_FAR);
						sendPacket(ActionFailed.STATIC_PACKET);
						return;
					}
				}
				else if (skill.getCastRange() > 0 && !isInsideRadius(target, skill.getCastRange() + getTemplate().collisionRadius, false, false)) // Calculate the distance between the L2PcInstance and the target
				{
					sendPacket(new SystemMessage(SystemMessageId.TARGET_TOO_FAR));
					sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}
			}
			else if (skillType == SkillType.SIGNET) // Check range for SIGNET skills
			{
				if (!isInsideRadius(getCurrentSkillWorldPosition().getX(), getCurrentSkillWorldPosition().getY(), getCurrentSkillWorldPosition().getZ(), skill.getCastRange() + getTemplate().getCollisionRadius(), false, false))
				{
					sendPacket(SystemMessageId.TARGET_TOO_FAR);
					sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}
			}
		}
		// Check if the skill is defensive
		if (!skill.isOffensive())
		{
			// check if the target is a monster and if force attack is set.. if not then we don't want to cast.
			if (target instanceof L2MonsterInstance && !ctrlPressed && skillTargetType != SkillTargetType.TARGET_PET && skillTargetType != SkillTargetType.TARGET_AURA && skillTargetType != SkillTargetType.TARGET_CLAN && skillTargetType != SkillTargetType.TARGET_SELF
				&& skillTargetType != SkillTargetType.TARGET_PARTY && skillTargetType != SkillTargetType.TARGET_ALLY && skillTargetType != SkillTargetType.TARGET_CORPSE_MOB && skillTargetType != SkillTargetType.TARGET_AREA_CORPSE_MOB && skillTargetType != SkillTargetType.TARGET_GROUND
				&& skillType != SkillType.BEAST_FEED && skillType != SkillType.DELUXE_KEY_UNLOCK && skillType != SkillType.UNLOCK)
			{
				sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
		}
		
		// Check if the skill is Spoil type and if the target isn't already spoiled
		if (skillType == SkillType.SPOIL)
		{
			if (!(target instanceof L2MonsterInstance))
			{
				sendPacket(new SystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
				sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
		}
		
		// Check if the skill is Sweep type and if conditions not apply
		if (skillType == SkillType.SWEEP && target instanceof L2Attackable)
		{
			int spoilerId = ((L2Attackable) target).getSpoilerId();
			
			if (((L2Attackable) target).isDead())
			{
				if (spoilerId == 0)
				{
					sendPacket(new SystemMessage(SystemMessageId.SWEEPER_FAILED_TARGET_NOT_SPOILED));
					sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}
				
				if (!isInLooterParty(spoilerId))
				{
					sendPacket(new SystemMessage(SystemMessageId.SWEEP_NOT_ALLOWED));
					sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}
			}
		}
		
		// Check if the skill is Drain Soul (Soul Crystals) and if the target is a MOB
		if (skillType == SkillType.DRAIN_SOUL)
		{
			if (!(target instanceof L2MonsterInstance))
			{
				sendPacket(new SystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
				sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
		}
		
		// Check if this is a Pvp skill and target isn't a non-flagged/non-karma player
		switch (skillTargetType)
		{
			case TARGET_PARTY:
			case TARGET_CORPSE_ALLY:
			case TARGET_ALLY:
			case TARGET_CLAN:
			case TARGET_AURA:
			case TARGET_SELF:
			case TARGET_GROUND:
				break;
			default:
				// if pvp skill is not allowed for given target
				if (!checkPvpSkill(target, skill) && !getAccessLevel().allowPeaceAttack() && (skill.getId() != 3261 && skill.getId() != 3260 && skill.getId() != 3262))
				{
					sendPacket(new SystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
					sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}
		}
		
		if (skillTargetType == SkillTargetType.TARGET_HOLY && !TakeCastle.checkIfOkToCastSealOfRule(this, false))
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			abortCast();
			return;
		}
		
		if (skillType == SkillType.SIEGEFLAG && !SiegeFlag.checkIfOkToPlaceFlag(this, false))
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			abortCast();
			return;
		}
		else if (skillType == SkillType.STRSIEGEASSAULT && !StrSiegeAssault.checkIfOkToUseStriderSiegeAssault(this, false))
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			abortCast();
			return;
		}
		
		if ((target instanceof L2GrandBossInstance) && ((L2GrandBossInstance) target).getNpcId() == 29022)
		{
			if (Math.abs(getClientZ() - target.getZ()) > 200)
			{
				sendPacket(new SystemMessage(SystemMessageId.CANT_SEE_TARGET));
				getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
				sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
		}
		
		// GeoData check
		if (skill.getCastRange() > 0 && !GeoData.getInstance().canSeeTarget(this, target))
		{
			sendPacket(new SystemMessage(SystemMessageId.CANT_SEE_TARGET));
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		// Like L2OFF after a skill the player must stop the movement, also with toggle
		// stopMove(null);
		
		// Check if the active L2Skill can be casted (ex : not sleeping...), Check if the target is correct and Notify the AI with AI_INTENTION_CAST and target
		getAI().setIntention(CtrlIntention.AI_INTENTION_CAST, skill, target);
	}
	
	public boolean isInLooterParty(int LooterId)
	{
		if (LooterId == getObjectId())
		{
			return true;
		}
		
		L2PcInstance looter = L2World.getInstance().getPlayer(LooterId);
		
		if (looter == null)
		{
			return false;
		}
		
		if (isInParty() && getParty().isInCommandChannel())
		{
			return getParty().getCommandChannel().getMembers().contains(looter);
		}
		
		if (isInParty())
		{
			return getParty().getPartyMembers().contains(looter);
		}
		
		return false;
	}
	
	public boolean checkPvpSkill(L2Object target, L2Skill skill)
	{
		return checkPvpSkill(target, skill, false);
	}
	
	public boolean checkPvpSkill(L2Object target, L2Skill skill, boolean srcIsSummon)
	{
		// Check if player and target are in events and on the same team.
		if (target instanceof L2PcInstance)
		{
			if (skill.isOffensive() && (_inEventTvT && ((L2PcInstance) target)._inEventTvT && TvT.is_started() && !_teamNameTvT.equals(((L2PcInstance) target)._teamNameTvT))
				|| (_inEventCTF && ((L2PcInstance) target)._inEventCTF && CTF.is_started() && !_teamNameCTF.equals(((L2PcInstance) target)._teamNameCTF)) || (_inEventDM && ((L2PcInstance) target)._inEventDM && DM.is_started()) || (_inEventVIP && ((L2PcInstance) target)._inEventVIP && VIP._started))
			{
				return true;
			}
			else if (isInFunEvent() && skill.isOffensive()) // same team return false
			{
				return false;
			}
		}
		
		// check for PC->PC Pvp status
		if (target instanceof L2Summon)
		{
			target = ((L2Summon) target).getOwner();
		}
		
		if (target != null && target != this && target instanceof L2PcInstance && !(isInDuel() && ((L2PcInstance) target).getDuelId() == getDuelId()) && !isInsideZone(ZoneId.ZONE_PVP) && !((L2PcInstance) target).isInsideZone(ZoneId.ZONE_PVP))
		{
			SkillDat skilldat = getCurrentSkill();
			SkillDat skilldatpet = getCurrentPetSkill();
			if (skill.isPvpSkill()) // pvp skill
			{
				if (getClan() != null && ((L2PcInstance) target).getClan() != null)
				{
					if (getClan().isAtWarWith(((L2PcInstance) target).getClan().getClanId()) && ((L2PcInstance) target).getClan().isAtWarWith(getClan().getClanId()))
					{
						return true; // in clan war player can attack whites even with sleep etc.
					}
				}
				if (((L2PcInstance) target).getPvpFlag() == 0 && // target's pvp flag is not set and
					((L2PcInstance) target).getKarma() == 0 // target has no karma
				)
				{
					return false;
				}
			}
			else if ((skilldat != null && !skilldat.isCtrlPressed() && skill.isOffensive() && !srcIsSummon) || (skilldatpet != null && !skilldatpet.isCtrlPressed() && skill.isOffensive() && srcIsSummon))
			{
				if (getClan() != null && ((L2PcInstance) target).getClan() != null)
				{
					if (getClan().isAtWarWith(((L2PcInstance) target).getClan().getClanId()) && ((L2PcInstance) target).getClan().isAtWarWith(getClan().getClanId()))
					{
						return true; // in clan war player can attack whites even without ctrl
					}
				}
				if (((L2PcInstance) target).getPvpFlag() == 0 && // target's pvp flag is not set and
					((L2PcInstance) target).getKarma() == 0 // target has no karma
				)
				{
					return false;
				}
			}
		}
		
		return true;
	}
	
	@Override
	public void consumeItem(int itemConsumeId, int itemCount)
	{
		if (itemConsumeId != 0 && itemCount != 0)
		{
			destroyItemByItemId("Consume", itemConsumeId, itemCount, null, true);
		}
	}
	
	public boolean isMageClass()
	{
		return getClassId().isMage();
	}
	
	public boolean isMounted()
	{
		return _mountType > 0;
	}
	
	public boolean checkLandingState()
	{
		// Check if char is in a no landing zone
		if (isInsideZone(ZoneId.ZONE_NOLANDING))
		{
			return true;
		}
		else
		// if this is a castle that is currently being sieged, and the rider is NOT a castle owner
		// he cannot land.
		// castle owner is the leader of the clan that owns the castle where the pc is
		if (isInsideZone(ZoneId.ZONE_SIEGE) && !(getClan() != null && CastleManager.getInstance().getCastle(this) == CastleManager.getInstance().getCastleByOwner(getClan()) && this == getClan().getLeader().getPlayerInstance()))
		{
			return true;
		}
		
		return false;
	}
	
	// returns false if the change of mount type fails.
	public boolean setMountType(int mountType)
	{
		if (checkLandingState() && mountType == 2)
		{
			return false;
		}
		
		switch (mountType)
		{
			case 0:
				setIsFlying(false);
				setIsRiding(false);
				break; // Dismounted
			case 1:
				setIsRiding(true);
				if (isNoble())
				{
					L2Skill striderAssaultSkill = SkillTable.getInstance().getInfo(325, 1);
					addSkill(striderAssaultSkill, false); // not saved to DB
				}
				break;
			case 2:
				setIsFlying(true);
				break; // Flying Wyvern
		}
		
		_mountType = mountType;
		
		UserInfo ui = new UserInfo(this);
		sendPacket(ui);
		return true;
	}
	
	public int getMountType()
	{
		return _mountType;
	}
	
	@Override
	public void updateAbnormalEffect()
	{
		broadcastUserInfo();
	}
	
	public void tempInvetoryDisable()
	{
		_inventoryDisable = true;
		
		ThreadPoolManager.getInstance().scheduleGeneral(new InventoryEnable(), 1500);
	}
	
	public boolean isInvetoryDisabled()
	{
		return _inventoryDisable;
	}
	
	class InventoryEnable implements Runnable
	{
		@Override
		public void run()
		{
			_inventoryDisable = false;
		}
	}
	
	public Map<Integer, L2CubicInstance> getCubics()
	{
		synchronized (_cubics)
		{
			Set<Integer> cubicsIds = _cubics.keySet();
			
			for (Integer id : cubicsIds)
			{
				if (id == null || _cubics.get(id) == null)
				{
					_cubics.remove(id);
				}
			}
			
			return _cubics;
		}
	}
	
	public void addCubic(int id, int level, double matk, int activationtime, int activationchance, int totalLifetime, boolean givenByOther)
	{
		L2CubicInstance cubic = new L2CubicInstance(this, id, level, (int) matk, activationtime, activationchance, totalLifetime, givenByOther);
		
		synchronized (_cubics)
		{
			_cubics.put(id, cubic);
		}
	}
	
	public void delCubic(int id)
	{
		synchronized (_cubics)
		{
			_cubics.remove(id);
		}
	}
	
	public L2CubicInstance getCubic(int id)
	{
		synchronized (_cubics)
		{
			return _cubics.get(id);
		}
	}
	
	public void unsummonAllCubics()
	{
		synchronized (_cubics)
		{
			
			if (_cubics.size() > 0)
			{
				for (L2CubicInstance cubic : _cubics.values())
				{
					cubic.stopAction();
					cubic.cancelDisappear();
				}
				
				_cubics.clear();
			}
			
		}
		
	}
	
	@Override
	public String toString()
	{
		return "player " + getName();
	}
	
	public int getEnchantEffect()
	{
		L2ItemInstance wpn = getActiveWeaponInstance();
		
		if (wpn == null)
		{
			return 0;
		}
		
		return Math.min(127, wpn.getEnchantLevel());
	}
	
	public void setLastFolkNPC(L2FolkInstance folkNpc)
	{
		_lastFolkNpc = folkNpc;
	}
	
	public L2FolkInstance getLastFolkNPC()
	{
		return _lastFolkNpc;
	}
	
	public void setSilentMoving(boolean flag)
	{
		if (flag)
		{
			_isSilentMoving++;
		}
		else
		{
			_isSilentMoving--;
		}
	}
	
	public boolean isSilentMoving()
	{
		return _isSilentMoving > 0;
	}
	
	public boolean isFestivalParticipant()
	{
		return SevenSignsFestival.getInstance().isPlayerParticipant(this);
	}
	
	public void addAutoSoulShot(int itemId)
	{
		_activeSoulShots.add(itemId);
	}
	
	public void removeAutoSoulShot(int itemId)
	{
		_activeSoulShots.remove(itemId);
	}
	
	public Set<Integer> getAutoSoulShot()
	{
		return _activeSoulShots;
	}
	
	public void rechargeAutoSoulShot(final boolean physical, final boolean magic, final boolean summon)
	{
		L2ItemInstance item;
		IItemHandler handler;
		
		if (_activeSoulShots == null || _activeSoulShots.isEmpty())
		{
			return;
		}
		
		for (final int itemId : _activeSoulShots)
		{
			item = getInventory().getItemByItemId(itemId);
			
			if (item != null)
			{
				if (magic)
				{
					if (!summon)
					{
						if (itemId == 10011 || itemId == 10005 || itemId == 10006 || itemId == 10007 || itemId == 10008 || itemId == 10009 || itemId == 2509 || itemId == 2510 || itemId == 2511 || itemId == 2512 || itemId == 2513 || itemId == 2514 || itemId == 3947 || itemId == 3948 || itemId == 3949
							|| itemId == 3950 || itemId == 3951 || itemId == 3952 || itemId == 5790)
						{
							handler = ItemHandler.getInstance().getItemHandler(itemId);
							if (handler != null)
							{
								handler.useItem(this, item);
							}
						}
					}
					else
					{
						if (itemId == 6646 || itemId == 6647)
						{
							handler = ItemHandler.getInstance().getItemHandler(itemId);
							if (handler != null)
							{
								handler.useItem(this, item);
							}
						}
					}
				}
				
				if (physical)
				{
					if (!summon)
					{
						if (itemId == 10010 || itemId == 10000 || itemId == 10001 || itemId == 10002 || itemId == 10003 || itemId == 10004 || itemId == 1463 || itemId == 1464 || itemId == 1465 || itemId == 1466 || itemId == 1467 || itemId == 1835 || itemId == 5789)
						{
							handler = ItemHandler.getInstance().getItemHandler(itemId);
							if (handler != null)
							{
								handler.useItem(this, item);
							}
						}
					}
					else
					{
						if (itemId == 6645)
						{
							handler = ItemHandler.getInstance().getItemHandler(itemId);
							if (handler != null)
							{
								handler.useItem(this, item);
							}
						}
					}
				}
			}
			else
			{
				removeAutoSoulShot(itemId);
			}
		}
	}
	
	/** The _task warn user take break. */
	private ScheduledFuture<?> _taskWarnUserTakeBreak;
	
	/**
	 * The Class WarnUserTakeBreak.
	 */
	class WarnUserTakeBreak implements Runnable
	{
		
		/*
		 * (non-Javadoc)
		 * @see java.lang.Runnable#run()
		 */
		@Override
		public void run()
		{
			if (isOnline() == 1)
			{
				SystemMessage msg = new SystemMessage(SystemMessageId.PLAYING_FOR_LONG_TIME);
				L2PcInstance.this.sendPacket(msg);
				msg = null;
			}
			else
			{
				stopWarnUserTakeBreak();
			}
		}
	}
	
	/**
	 * The Class RentPetTask.
	 */
	class RentPetTask implements Runnable
	{
		@Override
		public void run()
		{
			stopRentPet();
		}
	}
	
	/** The _taskforfish. */
	public ScheduledFuture<?> _taskforfish;
	
	/**
	 * The Class WaterTask.
	 */
	class WaterTask implements Runnable
	{
		@Override
		public void run()
		{
			double reduceHp = getMaxHp() / 100.0;
			
			if (reduceHp < 1)
			{
				reduceHp = 1;
			}
			
			reduceCurrentHp(reduceHp, L2PcInstance.this, false);
			SystemMessage sm = new SystemMessage(SystemMessageId.DROWN_DAMAGE_S1);
			sm.addNumber((int) reduceHp);
			sendPacket(sm);
		}
	}
	
	class LookingForFishTask implements Runnable
	{
		boolean _isNoob, _isUpperGrade;
		int _fishType, _fishGutsCheck, _gutsCheckTime;
		long _endTaskTime;
		
		protected LookingForFishTask(int fishWaitTime, int fishGutsCheck, int fishType, boolean isNoob, boolean isUpperGrade)
		{
			_fishGutsCheck = fishGutsCheck;
			_endTaskTime = System.currentTimeMillis() + fishWaitTime + 10000;
			_fishType = fishType;
			_isNoob = isNoob;
			_isUpperGrade = isUpperGrade;
		}
		
		@Override
		public void run()
		{
			if (System.currentTimeMillis() >= _endTaskTime)
			{
				EndFishing(false);
				return;
			}
			if (_fishType == -1)
			{
				return;
			}
			int check = Rnd.get(1000);
			if (_fishGutsCheck > check)
			{
				stopLookingForFishTask();
				StartFishCombat(_isNoob, _isUpperGrade);
			}
		}
		
	}
	
	public int getClanPrivileges()
	{
		return _clanPrivileges;
	}
	
	public void setClanPrivileges(int n)
	{
		_clanPrivileges = n;
	}
	
	public void setPledgeClass(int classId)
	{
		_pledgeClass = classId;
	}
	
	/**
	 * Gets the pledge class.
	 * @return the pledge class
	 */
	public int getPledgeClass()
	{
		return _pledgeClass;
	}
	
	/**
	 * Sets the pledge type.
	 * @param typeId the new pledge type
	 */
	public void setPledgeType(int typeId)
	{
		_pledgeType = typeId;
	}
	
	/**
	 * Gets the pledge type.
	 * @return the pledge type
	 */
	public int getPledgeType()
	{
		return _pledgeType;
	}
	
	/**
	 * Gets the apprentice.
	 * @return the apprentice
	 */
	public int getApprentice()
	{
		return _apprentice;
	}
	
	/**
	 * Sets the apprentice.
	 * @param apprentice_id the new apprentice
	 */
	public void setApprentice(int apprentice_id)
	{
		_apprentice = apprentice_id;
	}
	
	/**
	 * Gets the sponsor.
	 * @return the sponsor
	 */
	public int getSponsor()
	{
		return _sponsor;
	}
	
	/**
	 * Sets the sponsor.
	 * @param sponsor_id the new sponsor
	 */
	public void setSponsor(int sponsor_id)
	{
		_sponsor = sponsor_id;
	}
	
	/**
	 * Send message.
	 * @param message the message
	 */
	@Override
	public void sendMessage(String message)
	{
		sendPacket(SystemMessage.sendString(message));
	}
	
	public void removeAllSummons()
	{
		// Delete summons and pets
		if (getPet() != null)
		{
			getPet().unSummon(this);
		}
		
		// Delete trained beasts
		if (getTrainedBeast() != null)
		{
			L2TamedBeastInstance traindebeast = getTrainedBeast();
			traindebeast.stopAllEffects();
			
			traindebeast.doDespawn();
		}
		
		// Delete any form of cubics
		unsummonAllCubics();
	}
	
	public void setLastLocation()
	{
		_lastLoc.setXYZ(getX(), getY(), getZ());
	}
	
	public void unsetLastLocation()
	{
		_lastLoc.setXYZ(0, 0, 0);
	}
	
	public void enterObserverMode(int x, int y, int z)
	{
		removeAllSummons();
		
		if (getParty() != null)
		{
			getParty().removePartyMember(this, true);
		}
		
		if (isSitting())
		{
			standUp();
		}
		
		setLastLocation();
		
		_observerMode = true;
		
		setTarget(null);
		setIsInvul(true);
		getAppearance().setInvisible();
		setIsParalyzed(true);
		
		teleToLocation(x, y, z);
		sendPacket(new ObservationMode(x, y, z));
	}
	
	public void enterOlympiadObserverMode(int id)
	{
		final OlympiadGameTask task = OlympiadGameManager.getInstance().getOlympiadTask(id);
		if (task == null)
		{
			return;
		}
		
		removeAllSummons();
		
		if (getParty() != null)
		{
			getParty().removePartyMember(this, true);
		}
		
		_olympiadGameId = id;
		
		if (isSitting())
		{
			standUp();
		}
		
		// Don't override saved location if we jump from stadium to stadium.
		if (!inObserverMode())
		{
			setLastLocation();
		}
		
		_observerMode = true;
		
		setTarget(null);
		
		setIsInvul(true);
		getAppearance().setInvisible();
		
		teleToLocation(task.getZone().getSpawns().get(2), 0);
		sendPacket(new ExOlympiadMode(3));
	}
	
	public void leaveObserverMode()
	{
		if (getAI() != null)
		{
			getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
		}
		
		setTarget(null);
		
		if (!isGM())
		{
			getAppearance().setVisible();
			setIsInvul(false);
		}
		
		setIsParalyzed(false);
		
		teleToLocation(_lastLoc, false);
		sendPacket(new ObservationReturn(getPosition().getWorldPosition()));
		
		// Clear the location.
		unsetLastLocation();
		
		_observerMode = false;
	}
	
	public void leaveOlympiadObserverMode()
	{
		if (_olympiadGameId == -1)
		{
			return;
		}
		
		_olympiadGameId = -1;
		
		_observerMode = false;
		
		if (getAI() != null)
		{
			getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
		}
		
		setTarget(null);
		
		if (!isGM())
		{
			getAppearance().setVisible();
			setIsInvul(false);
		}
		
		sendPacket(new ExOlympiadMode(0));
		teleToLocation(_lastLoc, true);
		
		// Clear the location.
		unsetLastLocation();
	}
	
	private void createPSdb()
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("INSERT INTO account_premium (account_name,premium_service,enddate) values(?,?,?)");
			statement.setString(1, _accountName);
			statement.setInt(2, 0);
			statement.setLong(3, 0);
			statement.executeUpdate();
			statement.close();
		}
		catch (Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				LOG.error("PremiumService: Could not insert char data:");
				e.printStackTrace();
			}
			return;
		}
		finally
		{
			CloseUtil.close(con);
		}
	}
	
	private static void PStimeOver(String account)
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement(UPDATE_PREMIUMSERVICE);
			statement.setInt(1, 0);
			statement.setLong(2, 0);
			statement.setString(3, account);
			statement.execute();
			statement.close();
		}
		catch (SQLException e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				LOG.error("PremiumService: Could not increase data");
				e.printStackTrace();
			}
		}
		finally
		{
			CloseUtil.close(con);
		}
	}
	
	private static void restorePremServiceData(L2PcInstance player, String account)
	{
		boolean sucess = false;
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement(RESTORE_PREMIUMSERVICE);
			statement.setString(1, account);
			ResultSet rset = statement.executeQuery();
			while (rset.next())
			{
				sucess = true;
				if (Config.USE_PREMIUMSERVICE)
				{
					if (rset.getLong("enddate") <= System.currentTimeMillis())
					{
						PStimeOver(account);
						player.setPremiumService(0);
					}
					else
					{
						player.setPremiumService(rset.getInt("premium_service"));
					}
				}
				else
				{
					player.setPremiumService(0);
				}
			}
			statement.close();
			
		}
		catch (Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				LOG.error("PremiumService: Could not restore PremiumService data for:" + account + "");
				e.printStackTrace();
			}
		}
		finally
		{
			CloseUtil.close(con);
		}
		
		if (sucess == false)
		{
			player.createPSdb();
			player.setPremiumService(0);
		}
	}
	
	/**
	 * Update name title color.
	 */
	public void updateNameTitleColor()
	{
		if (Config.L2JMOD_WEDDING_COLORS)
		{
			if (isMarried())
			{
				if (marriedType() == 1)
				{
					getAppearance().setNameColor(Config.L2JMOD_WEDDING_NAME_COLOR_LESBO);
				}
				else if (marriedType() == 2)
				{
					getAppearance().setNameColor(Config.L2JMOD_WEDDING_NAME_COLOR_GEY);
				}
				else
				{
					getAppearance().setNameColor(Config.L2JMOD_WEDDING_NAME_COLOR_NORMAL);
				}
			}
		}
		/** Updates title and name color of a donator **/
		if (Config.PREMIUM_NAME_COLOR_ENABLED && getPremiumService() == 1)
		{
			getAppearance().setNameColor(Config.PREMIUM_NAME_COLOR);
		}
		if (Config.PREMIUM_TITLE_COLOR_ENABLED && getPremiumService() == 1)
		{
			getAppearance().setTitleColor(Config.PREMIUM_TITLE_COLOR);
		}
		
		/** Updates title and name color of a donator **/
		if (Config.PREMIUM_NAME_COLOR_ENABLED && getPremiumService() == 0)
		{
			getAppearance().setNameColor(0xFFFFFF);
		}
		if (Config.PREMIUM_TITLE_COLOR_ENABLED && getPremiumService() == 0)
		{
			getAppearance().setTitleColor(0xFFFF77);
		}
	}
	
	/**
	 * Update gm name title color.
	 */
	public void updateGmNameTitleColor()// KidZor: needs to be finished when Acces levels system is complite
	{
		// if this is a GM but has disabled his gM status, so we clear name / title
		if (isGM() && !hasGmStatusActive())
		{
			getAppearance().setNameColor(0xFFFFFF);
			getAppearance().setTitleColor(0xFFFF77);
		}
		
		// this is a GM but has GM status enabled, so we must set proper values
		else if (isGM() && hasGmStatusActive())
		{
			// Nick Updates
			if (getAccessLevel().useNameColor())
			{
				// this is a normal GM
				if (isNormalGm())
				{
					getAppearance().setNameColor(getAccessLevel().getNameColor());
				}
				else if (isAdministrator())
				{
					getAppearance().setNameColor(Config.MASTERACCESS_NAME_COLOR);
				}
			}
			else
			{
				getAppearance().setNameColor(0xFFFFFF);
			}
			
			// Title Updates
			if (getAccessLevel().useTitleColor())
			{
				// this is a normal GM
				if (isNormalGm())
				{
					getAppearance().setTitleColor(getAccessLevel().getTitleColor());
				}
				else if (isAdministrator())
				{
					getAppearance().setTitleColor(Config.MASTERACCESS_TITLE_COLOR);
				}
			}
			else
			{
				getAppearance().setTitleColor(0xFFFF77);
			}
		}
	}
	
	/**
	 * Sets the olympiad side.
	 * @param i the new olympiad side
	 */
	public void setOlympiadSide(int i)
	{
		_olympiadSide = i;
	}
	
	/**
	 * Gets the olympiad side.
	 * @return the olympiad side
	 */
	public int getOlympiadSide()
	{
		return _olympiadSide;
	}
	
	/**
	 * Sets the olympiad game id.
	 * @param id the new olympiad game id
	 */
	public void setOlympiadGameId(int id)
	{
		_olympiadGameId = id;
	}
	
	/**
	 * Gets the olympiad game id.
	 * @return the olympiad game id
	 */
	public int getOlympiadGameId()
	{
		return _olympiadGameId;
	}
	
	/**
	 * Gets the obs x.
	 * @return the obs x
	 */
	public int getObsX()
	{
		return _obsX;
	}
	
	/**
	 * Gets the obs y.
	 * @return the obs y
	 */
	public int getObsY()
	{
		return _obsY;
	}
	
	/**
	 * Gets the obs z.
	 * @return the obs z
	 */
	public int getObsZ()
	{
		return _obsZ;
	}
	
	public Location getLastLocation()
	{
		return _lastLoc;
	}
	
	/**
	 * In observer mode.
	 * @return true, if successful
	 */
	public boolean inObserverMode()
	{
		return _observerMode;
	}
	
	/**
	 * Gets the tele mode.
	 * @return the tele mode
	 */
	public int getTeleMode()
	{
		return _telemode;
	}
	
	/**
	 * Sets the tele mode.
	 * @param mode the new tele mode
	 */
	public void setTeleMode(int mode)
	{
		_telemode = mode;
	}
	
	/**
	 * Sets the loto.
	 * @param i the i
	 * @param val the val
	 */
	public void setLoto(int i, int val)
	{
		_loto[i] = val;
	}
	
	/**
	 * Gets the loto.
	 * @param i the i
	 * @return the loto
	 */
	public int getLoto(int i)
	{
		return _loto[i];
	}
	
	/**
	 * Sets the race.
	 * @param i the i
	 * @param val the val
	 */
	public void setRace(int i, int val)
	{
		_race[i] = val;
	}
	
	/**
	 * Gets the race.
	 * @param i the i
	 * @return the race
	 */
	public int getRace(int i)
	{
		return _race[i];
	}
	
	@Override
	public void sendPacket(L2GameServerPacket packet)
	{
		if (_client != null)
		{
			_client.sendPacket(packet);
		}
	}
	
	/**
	 * Send SystemMessage packet.<BR>
	 * <BR>
	 * @param id
	 */
	public void sendPacket(SystemMessageId id)
	{
		sendPacket(SystemMessage.getSystemMessage(id));
	}
	
	/**
	 * Sets the diet mode.
	 * @param mode the new diet mode
	 */
	public void setDietMode(boolean mode)
	{
		_dietMode = mode;
	}
	
	/**
	 * Gets the diet mode.
	 * @return the diet mode
	 */
	public boolean getDietMode()
	{
		return _dietMode;
	}
	
	public boolean isInPmSilenceMode()
	{
		return _pmSilenceMode;
	}
	
	public void setInPmSilenceMode(boolean mode)
	{
		_pmSilenceMode = mode;
		sendPacket(new EtcStatusUpdate(this));
	}
	
	public boolean isInRefusalMode()
	{
		return _messageRefusal;
	}
	
	public void setInRefusalMode(boolean mode)
	{
		_messageRefusal = mode;
		sendPacket(new EtcStatusUpdate(this));
	}
	
	/**
	 * Sets the exchange refusal.
	 * @param mode the new exchange refusal
	 */
	public void setExchangeRefusal(boolean mode)
	{
		_exchangeRefusal = mode;
	}
	
	/**
	 * Gets the exchange refusal.
	 * @return the exchange refusal
	 */
	public boolean getExchangeRefusal()
	{
		return _exchangeRefusal;
	}
	
	/**
	 * Gets the block list.
	 * @return the block list
	 */
	public BlockList getBlockList()
	{
		return _blockList;
	}
	
	/**
	 * Sets the hero aura.
	 * @param heroAura the new hero aura
	 */
	public void setHeroAura(boolean heroAura)
	{
		isPVPHero = heroAura;
		return;
	}
	
	/**
	 * Gets the checks if is pvp hero.
	 * @return the checks if is pvp hero
	 */
	public boolean getIsPVPHero()
	{
		return isPVPHero;
	}
	
	/**
	 * Gets the count.
	 * @return the count
	 */
	public int getCount()
	{
		String HERO_COUNT = "SELECT count FROM heroes WHERE char_id=?";
		int _count = 0;
		Connection con = null;
		
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement(HERO_COUNT);
			statement.setInt(1, getObjectId());
			ResultSet rset = statement.executeQuery();
			while (rset.next())
			{
				_count = rset.getInt("count");
			}
			
			rset.close();
			statement.close();
		}
		catch (Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
		}
		finally
		{
			CloseUtil.close(con);
		}
		
		if (_count != 0)
		{
			return _count;
		}
		return 0;
	}
	
	/**
	 * Reload pvp hero aura.
	 */
	public void reloadPVPHeroAura()
	{
		sendPacket(new UserInfo(this));
	}
	
	/**
	 * Sets the donator.
	 * @param value the new donator
	 */
	public void setDonator(boolean value)
	{
		_donator = value;
	}
	
	/**
	 * Checks if is donator.
	 * @return true, if is donator
	 */
	public boolean isDonator()
	{
		return _donator;
	}
	
	/**
	 * Sets the checks if is in olympiad mode.
	 * @param b the new checks if is in olympiad mode
	 */
	public void setIsInOlympiadMode(boolean b)
	{
		_inOlympiadMode = b;
	}
	
	/**
	 * Sets the checks if is olympiad start.
	 * @param b the new checks if is olympiad start
	 */
	public void setIsOlympiadStart(boolean b)
	{
		_OlympiadStart = b;
	}
	
	/**
	 * Checks if is olympiad start.
	 * @return true, if is olympiad start
	 */
	public boolean isOlympiadStart()
	{
		return _OlympiadStart;
	}
	
	/**
	 * Checks if is hero.
	 * @return true, if is hero
	 */
	public boolean isHero()
	{
		return _hero;
	}
	
	/**
	 * Checks if is in olympiad mode.
	 * @return true, if is in olympiad mode
	 */
	public boolean isInOlympiadMode()
	{
		return _inOlympiadMode;
	}
	
	/**
	 * Checks if is in duel.
	 * @return true, if is in duel
	 */
	public boolean isInDuel()
	{
		return _isInDuel;
	}
	
	/**
	 * Gets the duel id.
	 * @return the duel id
	 */
	public int getDuelId()
	{
		return _duelId;
	}
	
	/**
	 * Sets the duel state.
	 * @param mode the new duel state
	 */
	public void setDuelState(int mode)
	{
		_duelState = mode;
	}
	
	/**
	 * Gets the duel state.
	 * @return the duel state
	 */
	public int getDuelState()
	{
		return _duelState;
	}
	
	/**
	 * Sets the coupon.
	 * @param coupon the new coupon
	 */
	public void setCoupon(int coupon)
	{
		if (coupon >= 0 && coupon <= 3)
		{
			_hasCoupon = coupon;
		}
	}
	
	/**
	 * Adds the coupon.
	 * @param coupon the coupon
	 */
	public void addCoupon(int coupon)
	{
		if (coupon == 1 || coupon == 2 && !getCoupon(coupon - 1))
		{
			_hasCoupon += coupon;
		}
	}
	
	/**
	 * Gets the coupon.
	 * @param coupon the coupon
	 * @return the coupon
	 */
	public boolean getCoupon(int coupon)
	{
		return (_hasCoupon == 1 || _hasCoupon == 3) && coupon == 0 || (_hasCoupon == 2 || _hasCoupon == 3) && coupon == 1;
	}
	
	/**
	 * Sets up the duel state using a non 0 duelId.
	 * @param duelId 0=not in a duel
	 */
	public void setIsInDuel(int duelId)
	{
		if (duelId > 0)
		{
			_isInDuel = true;
			_duelState = Duel.DUELSTATE_DUELLING;
			_duelId = duelId;
		}
		else
		{
			if (_duelState == Duel.DUELSTATE_DEAD)
			{
				enableAllSkills();
				getStatus().startHpMpRegeneration();
			}
			_isInDuel = false;
			_duelState = Duel.DUELSTATE_NODUEL;
			_duelId = 0;
		}
	}
	
	/**
	 * This returns a SystemMessage stating why the player is not available for duelling.
	 * @return S1_CANNOT_DUEL... message
	 */
	public SystemMessage getNoDuelReason()
	{
		SystemMessage sm = new SystemMessage(_noDuelReason);
		sm.addString(getName());
		_noDuelReason = SystemMessageId.THERE_IS_NO_OPPONENT_TO_RECEIVE_YOUR_CHALLENGE_FOR_A_DUEL;
		return sm;
	}
	
	/**
	 * Checks if this player might join / start a duel. To get the reason use getNoDuelReason() after calling this function.
	 * @return true if the player might join/start a duel.
	 */
	public boolean canDuel()
	{
		if (isInCombat() || isInJail())
		{
			_noDuelReason = SystemMessageId.S1_CANNOT_DUEL_BECAUSE_S1_IS_CURRENTLY_ENGAGED_IN_BATTLE;
			return false;
		}
		if (isDead() || isAlikeDead() || getCurrentHp() < getMaxHp() / 2 || getCurrentMp() < getMaxMp() / 2)
		{
			_noDuelReason = SystemMessageId.S1_CANNOT_DUEL_BECAUSE_S1S_HP_OR_MP_IS_BELOW_50_PERCENT;
			return false;
		}
		if (isInDuel())
		{
			_noDuelReason = SystemMessageId.S1_CANNOT_DUEL_BECAUSE_S1_IS_ALREADY_ENGAGED_IN_A_DUEL;
			return false;
		}
		if (isInOlympiadMode())
		{
			_noDuelReason = SystemMessageId.S1_CANNOT_DUEL_BECAUSE_S1_IS_PARTICIPATING_IN_THE_OLYMPIAD;
			return false;
		}
		if (isCursedWeaponEquiped())
		{
			_noDuelReason = SystemMessageId.S1_CANNOT_DUEL_BECAUSE_S1_IS_IN_A_CHAOTIC_STATE;
			return false;
		}
		if (getPrivateStoreType() != STORE_PRIVATE_NONE)
		{
			_noDuelReason = SystemMessageId.S1_CANNOT_DUEL_BECAUSE_S1_IS_CURRENTLY_ENGAGED_IN_A_PRIVATE_STORE_OR_MANUFACTURE;
			return false;
		}
		if (isMounted() || isInBoat())
		{
			_noDuelReason = SystemMessageId.S1_CANNOT_DUEL_BECAUSE_S1_IS_CURRENTLY_RIDING_A_BOAT_WYVERN_OR_STRIDER;
			return false;
		}
		if (isFishing())
		{
			_noDuelReason = SystemMessageId.S1_CANNOT_DUEL_BECAUSE_S1_IS_CURRENTLY_FISHING;
			return false;
		}
		if (isInsideZone(ZoneId.ZONE_PVP) || isInsideZone(ZoneId.ZONE_PEACE) || isInsideZone(ZoneId.ZONE_SIEGE))
		{
			_noDuelReason = SystemMessageId.S1_CANNOT_MAKE_A_CHALLANGE_TO_A_DUEL_BECAUSE_S1_IS_CURRENTLY_IN_A_DUEL_PROHIBITED_AREA;
			return false;
		}
		return true;
	}
	
	public boolean isNoble()
	{
		return _noble;
	}
	
	public void setNoble(boolean val)
	{
		if (val)
		{
			getAchievement().increase(AchType.NOBLE);
			for (L2Skill s : NobleSkillTable.getInstance().GetNobleSkills())
			{
				addSkill(s, false); // Dont Save Noble skills to Sql
			}
		}
		else
		{
			for (L2Skill s : NobleSkillTable.getInstance().GetNobleSkills())
			{
				super.removeSkill(s); // Just Remove skills without deleting from Sql
			}
		}
		_noble = val;
		
		sendSkillList();
	}
	
	public void addClanLeaderSkills(boolean val)
	{
		if (val)
		{
			SiegeManager.getInstance().addSiegeSkills(this);
		}
		else
		{
			SiegeManager.getInstance().removeSiegeSkills(this);
		}
		sendSkillList();
	}
	
	public void setLvlJoinedAcademy(int lvl)
	{
		_lvlJoinedAcademy = lvl;
	}
	
	public int getLvlJoinedAcademy()
	{
		return _lvlJoinedAcademy;
	}
	
	/**
	 * Checks if is academy member.
	 * @return true, if is academy member
	 */
	public boolean isAcademyMember()
	{
		return _lvlJoinedAcademy > 0;
	}
	
	/**
	 * Sets the team.
	 * @param team the new team
	 */
	public void setTeam(int team)
	{
		_team = team;
	}
	
	/**
	 * Gets the team.
	 * @return the team
	 */
	public int getTeam()
	{
		return _team;
	}
	
	/**
	 * Sets the wants peace.
	 * @param wantsPeace the new wants peace
	 */
	public void setWantsPeace(int wantsPeace)
	{
		_wantsPeace = wantsPeace;
	}
	
	/**
	 * Gets the wants peace.
	 * @return the wants peace
	 */
	public int getWantsPeace()
	{
		return _wantsPeace;
	}
	
	/**
	 * Checks if is fishing.
	 * @return true, if is fishing
	 */
	public boolean isFishing()
	{
		return _fishing;
	}
	
	/**
	 * Sets the fishing.
	 * @param fishing the new fishing
	 */
	public void setFishing(boolean fishing)
	{
		_fishing = fishing;
	}
	
	/**
	 * Sets the alliance with varka ketra.
	 * @param sideAndLvlOfAlliance the new alliance with varka ketra
	 */
	public void setAllianceWithVarkaKetra(int sideAndLvlOfAlliance)
	{
		_alliedVarkaKetra = sideAndLvlOfAlliance;
	}
	
	/**
	 * Gets the alliance with varka ketra.
	 * @return the alliance with varka ketra
	 */
	public int getAllianceWithVarkaKetra()
	{
		return _alliedVarkaKetra;
	}
	
	/**
	 * Checks if is allied with varka.
	 * @return true, if is allied with varka
	 */
	public boolean isAlliedWithVarka()
	{
		return _alliedVarkaKetra < 0;
	}
	
	/**
	 * Checks if is allied with ketra.
	 * @return true, if is allied with ketra
	 */
	public boolean isAlliedWithKetra()
	{
		return _alliedVarkaKetra > 0;
	}
	
	public void sendSkillList()
	{
		boolean isDisabled = false;
		
		SkillList list = new SkillList();
		
		for (L2Skill skill : getAllSkills())
		{
			if (skill == null)
			{
				continue;
			}
			
			if (skill.getId() > 9000 && skill.getId() < 9007)
			{
				continue; // Fake skills to change base stats
			}
			
			if (skill.bestowed())
			{
				continue;
			}
			
			if (isCursedWeaponEquipped())
			{
				if (skill.isToggle())
				{
					L2Effect toggleEffect = getFirstEffect(skill.getId());
					if (toggleEffect != null)
					{
						toggleEffect.exit(false);
					}
				}
				isDisabled = !skill.isDemonicSkill();
			}
			
			// fix - learning toggle skills and auto potions/shots one enter to world
			if (skill.isToggle() && !isEnteringToWorld())
			{
				L2Effect toggleEffect = getFirstEffect(skill.getId());
				if (toggleEffect != null)
				{
					toggleEffect.exit(false);
					skill.getEffects(this, this, false, false, false);
				}
			}
			
			if (skill.isChance())
			{
				list.addSkill(skill.getId(), skill.getLevel(), skill.isChance(), isDisabled);
			}
			else
			{
				list.addSkill(skill.getId(), skill.getLevel(), skill.isPassive(), isDisabled);
			}
		}
		
		sendPacket(list);
	}
	
	/**
	 * 1. Add the specified class ID as a subclass (up to the maximum number of <b>three</b>) for this character.<BR>
	 * 2. This method no longer changes the active _classIndex of the player. This is only done by the calling of setActiveClass() method as that should be the only way to do so.
	 * @param classId the class id
	 * @param classIndex the class index
	 * @return boolean subclassAdded
	 */
	public synchronized boolean addSubClass(int classId, int classIndex)
	{
		// Remove shot automation
		Set<Integer> activeSoulShots = getAutoSoulShot();
		for (int itemId : activeSoulShots)
		{
			removeAutoSoulShot(itemId);
			ExAutoSoulShot atk = new ExAutoSoulShot(itemId, 0);
			sendPacket(atk);
		}
		// Reload skills from armors / jewels / weapons
		getInventory().reloadEquippedItems();
		
		// Remove Item RHAND
		if (Config.REMOVE_WEAPON_SUBCLASS)
		{
			L2ItemInstance rhand = getInventory().getPaperdollItem(Inventory.PAPERDOLL_RHAND);
			if (rhand != null)
			{
				L2ItemInstance[] unequipped = getInventory().unEquipItemInBodySlotAndRecord(rhand.getItem().getBodyPart());
				InventoryUpdate iu = new InventoryUpdate();
				for (L2ItemInstance element : unequipped)
				{
					iu.addModifiedItem(element);
				}
				sendPacket(iu);
			}
		}
		
		// Remove Item CHEST
		if (Config.REMOVE_CHEST_SUBCLASS)
		{
			L2ItemInstance chest = getInventory().getPaperdollItem(Inventory.PAPERDOLL_CHEST);
			if (chest != null)
			{
				L2ItemInstance[] unequipped = getInventory().unEquipItemInBodySlotAndRecord(chest.getItem().getBodyPart());
				InventoryUpdate iu = new InventoryUpdate();
				for (L2ItemInstance element : unequipped)
				{
					iu.addModifiedItem(element);
				}
				sendPacket(iu);
			}
		}
		
		// Remove Item LEG
		if (Config.REMOVE_LEG_SUBCLASS)
		{
			L2ItemInstance legs = getInventory().getPaperdollItem(Inventory.PAPERDOLL_LEGS);
			if (legs != null)
			{
				L2ItemInstance[] unequipped = getInventory().unEquipItemInBodySlotAndRecord(legs.getItem().getBodyPart());
				InventoryUpdate iu = new InventoryUpdate();
				for (L2ItemInstance element : unequipped)
				{
					iu.addModifiedItem(element);
				}
				sendPacket(iu);
			}
		}
		
		if (getTotalSubClasses() == Config.ALLOWED_SUBCLASS || classIndex == 0)
		{
			return false;
		}
		
		if (getSubClasses().containsKey(classIndex))
		{
			return false;
		}
		
		// Note: Never change _classIndex in any method other than setActiveClass().
		
		SubClass newClass = new SubClass();
		newClass.setClassId(classId);
		newClass.setClassIndex(classIndex);
		
		boolean output = false;
		
		Connection con = null;
		
		try
		{
			// Store the basic info about this new sub-class.
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement(ADD_CHAR_SUBCLASS);
			statement.setInt(1, getObjectId());
			statement.setInt(2, newClass.getClassId());
			statement.setLong(3, newClass.getExp());
			statement.setInt(4, newClass.getSp());
			statement.setInt(5, newClass.getLevel());
			statement.setInt(6, newClass.getClassIndex()); // <-- Added
			statement.execute();
			statement.close();
			statement = null;
			
			output = true;
		}
		catch (Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				LOG.error("WARNING: Could not add character sub class for " + getName() + ":");
				e.printStackTrace();
			}
		}
		finally
		{
			CloseUtil.close(con);
		}
		
		if (output)
		{
			
			// Commit after database INSERT incase exception is thrown.
			getSubClasses().put(newClass.getClassIndex(), newClass);
			
			if (Config.DEBUG)
			{
				LOG.info(getName() + " added class ID " + classId + " as a sub class at index " + classIndex + ".");
			}
			
			ClassId subTemplate = ClassId.values()[classId];
			Collection<L2SkillLearn> skillTree = SkillTreeTable.getInstance().getAllowedSkills(subTemplate);
			subTemplate = null;
			
			if (skillTree == null)
			{
				return true;
			}
			
			Map<Integer, L2Skill> prevSkillList = new FastMap<>();
			
			for (L2SkillLearn skillInfo : skillTree)
			{
				if (skillInfo.getMinLevel() <= 40)
				{
					final L2Skill prevSkill = prevSkillList.get(skillInfo.getId());
					final L2Skill newSkill = SkillTable.getInstance().getInfo(skillInfo.getId(), skillInfo.getLevel());
					
					if (newSkill == null || prevSkill != null && prevSkill.getLevel() > newSkill.getLevel())
					{
						continue;
					}
					
					prevSkillList.put(newSkill.getId(), newSkill);
					storeSkill(newSkill, prevSkill, classIndex);
				}
			}
			
			if (Config.DEBUG)
			{
				LOG.info(getName() + " was given " + getAllSkills().length + " skills for their new sub class.");
			}
			
		}
		
		return output;
	}
	
	/**
	 * 1. Completely erase all existance of the subClass linked to the classIndex.<BR>
	 * 2. Send over the newClassId to addSubClass()to create a new instance on this classIndex.<BR>
	 * 3. Upon Exception, revert the player to their BaseClass to avoid further problems.<BR>
	 * @param classIndex the class index
	 * @param newClassId the new class id
	 * @return boolean subclassAdded
	 */
	public boolean modifySubClass(int classIndex, int newClassId)
	{
		int oldClassId = getSubClasses().get(classIndex).getClassId();
		
		if (Config.DEBUG)
		{
			LOG.info(getName() + " has requested to modify sub class index " + classIndex + " from class ID " + oldClassId + " to " + newClassId + ".");
		}
		
		boolean output = false;
		
		Connection con = null;
		
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement;
			
			// Remove all henna info stored for this sub-class.
			statement = con.prepareStatement(DELETE_CHAR_HENNAS);
			statement.setInt(1, getObjectId());
			statement.setInt(2, classIndex);
			statement.execute();
			statement.close();
			
			// Remove all shortcuts info stored for this sub-class.
			statement = con.prepareStatement(DELETE_CHAR_SHORTCUTS);
			statement.setInt(1, getObjectId());
			statement.setInt(2, classIndex);
			statement.execute();
			statement.close();
			
			// Remove all effects info stored for this sub-class.
			statement = con.prepareStatement(DELETE_SKILL_SAVE);
			statement.setInt(1, getObjectId());
			statement.setInt(2, classIndex);
			statement.execute();
			statement.close();
			
			// Remove all skill info stored for this sub-class.
			statement = con.prepareStatement(DELETE_CHAR_SKILLS);
			statement.setInt(1, getObjectId());
			statement.setInt(2, classIndex);
			statement.execute();
			statement.close();
			
			// Remove all basic info stored about this sub-class.
			statement = con.prepareStatement(DELETE_CHAR_SUBCLASS);
			statement.setInt(1, getObjectId());
			statement.setInt(2, classIndex);
			statement.execute();
			statement.close();
			statement = null;
			
			output = true;
		}
		catch (Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				LOG.error("Could not modify sub class for " + getName() + " to class index " + classIndex + ":");
				e.printStackTrace();
			}
		}
		finally
		{
			CloseUtil.close(con);
		}
		
		getSubClasses().remove(classIndex);
		
		if (output)
		{
			return addSubClass(newClassId, classIndex);
		}
		return false;
	}
	
	/**
	 * Checks if is sub class active.
	 * @return true, if is sub class active
	 */
	public boolean isSubClassActive()
	{
		return _classIndex > 0;
	}
	
	/**
	 * Gets the sub classes.
	 * @return the sub classes
	 */
	public Map<Integer, SubClass> getSubClasses()
	{
		if (_subClasses == null)
		{
			_subClasses = new FastMap<>();
		}
		
		return _subClasses;
	}
	
	/**
	 * Gets the total sub classes.
	 * @return the total sub classes
	 */
	public int getTotalSubClasses()
	{
		return getSubClasses().size();
	}
	
	/**
	 * Gets the base class.
	 * @return the base class
	 */
	public int getBaseClass()
	{
		return _baseClass;
	}
	
	/**
	 * Return the ClassId object of the L2PcInstance contained in L2PcTemplate.<BR>
	 * <BR>
	 * @return the class id
	 */
	public ClassId getClassId()
	{
		return getTemplate().classId;
	}
	
	/**
	 * Gets the active class.
	 * @return the active class
	 */
	public synchronized int getActiveClass()
	{
		return _activeClass;
	}
	
	/**
	 * Gets the class index.
	 * @return the class index
	 */
	public int getClassIndex()
	{
		return _classIndex;
	}
	
	/**
	 * Sets the class template.
	 * @param classId the new class template
	 */
	private synchronized void setClassTemplate(int classId)
	{
		_activeClass = classId;
		
		L2PcTemplate t = CharTemplateTable.getInstance().getTemplate(classId);
		
		if (t == null)
		{
			LOG.error("Missing template for classId: " + classId);
			throw new Error();
		}
		
		// Set the template of the L2PcInstance
		setTemplate(t);
	}
	
	public boolean setActiveClass(int classIndex)
	{
		if (isInCombat() || getAI().getIntention() == CtrlIntention.AI_INTENTION_ATTACK)
		{
			sendMessage("You can't switch class in combat.");
			sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		
		if (_forceBuff != null)
		{
			abortCast();
		}
		
		store();
		
		getSkillReuseTimeStamps().clear();
		
		if (classIndex == 0)
		{
			setClassTemplate(getBaseClass());
		}
		else
		{
			try
			{
				setClassTemplate(getSubClasses().get(classIndex).getClassId());
			}
			catch (Exception e)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
				{
					LOG.error("Could not switch " + getName() + "'s sub class to class index " + classIndex + ":");
					e.printStackTrace();
				}
				
				return false;
			}
		}
		_classIndex = classIndex;
		
		if (isInParty())
		{
			getParty().recalculatePartyLevel();
		}
		
		if (getPet() != null && getPet() instanceof L2SummonInstance)
		{
			getPet().unSummon(this);
		}
		
		unsummonAllCubics();
		
		synchronized (getAllSkills())
		{
			for (L2Skill oldSkill : getAllSkills())
			{
				super.removeSkill(oldSkill);
			}
		}
		
		// Re-bind CursedWeapon passive.
		if (isCursedWeaponEquiped())
		{
			CursedWeaponsManager.getInstance().givePassive(_cursedWeaponEquipedId);
		}
		
		EffectCharge effect = getChargeEffect();
		if (effect != null)
		{
			effect.numCharges = 0;
			effect.exit(false);
		}
		
		stopAllEffects();
		
		if (isSubClassActive())
		{
			_dwarvenRecipeBook.clear();
			_commonRecipeBook.clear();
		}
		else
		{
			restoreRecipeBook();
		}
		
		// Restore any Death Penalty Buff
		restoreDeathPenaltyBuffLevel();
		
		restoreSkills();
		regiveTemporarySkills();
		rewardSkills();
		
		// Remove shot automation
		Set<Integer> activeSoulShots = getAutoSoulShot();
		for (int itemId : activeSoulShots)
		{
			removeAutoSoulShot(itemId);
			ExAutoSoulShot atk = new ExAutoSoulShot(itemId, 0);
			sendPacket(atk);
		}
		
		// Reload skills from armors / jewels / weapons
		getInventory().reloadEquippedItems();
		
		// Remove Item RHAND
		if (Config.REMOVE_WEAPON_SUBCLASS)
		{
			L2ItemInstance rhand = getInventory().getPaperdollItem(Inventory.PAPERDOLL_RHAND);
			if (rhand != null)
			{
				L2ItemInstance[] unequipped = getInventory().unEquipItemInBodySlotAndRecord(rhand.getItem().getBodyPart());
				InventoryUpdate iu = new InventoryUpdate();
				for (L2ItemInstance element : unequipped)
				{
					iu.addModifiedItem(element);
				}
				sendPacket(iu);
				if (rhand.isAugmented())
				{
					rhand.getAugmentation().removeBoni(this);
				}
			}
		}
		
		// Prevents some issues when changing between subclases that shares skills
		if (_disabledSkills != null && !_disabledSkills.isEmpty())
		{
			_disabledSkills.clear();
		}
		
		restoreEffects(Config.ALT_RESTORE_EFFECTS_ON_SUBCLASS_CHANGE);
		
		// Remove Item CHEST
		if (Config.REMOVE_CHEST_SUBCLASS)
		{
			L2ItemInstance chest = getInventory().getPaperdollItem(Inventory.PAPERDOLL_CHEST);
			if (chest != null)
			{
				L2ItemInstance[] unequipped = getInventory().unEquipItemInBodySlotAndRecord(chest.getItem().getBodyPart());
				InventoryUpdate iu = new InventoryUpdate();
				for (L2ItemInstance element : unequipped)
				{
					iu.addModifiedItem(element);
				}
				sendPacket(iu);
			}
		}
		
		// Remove Item LEG
		if (Config.REMOVE_LEG_SUBCLASS)
		{
			L2ItemInstance legs = getInventory().getPaperdollItem(Inventory.PAPERDOLL_LEGS);
			if (legs != null)
			{
				L2ItemInstance[] unequipped = getInventory().unEquipItemInBodySlotAndRecord(legs.getItem().getBodyPart());
				InventoryUpdate iu = new InventoryUpdate();
				for (L2ItemInstance element : unequipped)
				{
					iu.addModifiedItem(element);
				}
				sendPacket(iu);
			}
		}
		
		if (Config.CHECK_SKILLS_ON_ENTER && !Config.ALT_GAME_SKILL_LEARN)
		{
			checkAllowedSkills();
		}
		
		sendPacket(new EtcStatusUpdate(this));
		
		// if player has quest 422: Repent Your Sins, remove it
		QuestState st = getQuestState("422_RepentYourSins");
		if (st != null)
		{
			st.exitQuest(true);
		}
		
		for (int i = 0; i < 3; i++)
		{
			_henna[i] = null;
		}
		
		restoreHenna();
		sendPacket(new HennaInfo(this));
		
		if (getCurrentHp() > getMaxHp())
		{
			setCurrentHp(getMaxHp());
		}
		
		if (getCurrentMp() > getMaxMp())
		{
			setCurrentMp(getMaxMp());
		}
		
		if (getCurrentCp() > getMaxCp())
		{
			setCurrentCp(getMaxCp());
		}
		
		// Refresh player infos and update new status
		broadcastUserInfo();
		refreshOverloaded();
		refreshExpertisePenalty();
		refreshMasteryPenality();
		refreshMasteryWeapPenality();
		sendPacket(new UserInfo(this));
		sendPacket(new ItemList(this, false));
		getInventory().refreshWeight();
		
		// Clear resurrect xp calculation
		setExpBeforeDeath(0);
		_macroses.restore();
		_macroses.sendUpdate();
		_shortCuts.restore();
		
		sendPacket(new ShortCutInit(this));
		
		// Rebirth Caller - if player has any skills, they will be granted them.
		if (Config.REBIRTH_ENABLE)
		{
			Rebirth.getInstance().grantRebirthSkills(this);
		}
		
		broadcastPacket(new SocialAction(getObjectId(), 15));
		sendPacket(new SkillCoolTime(this));
		
		if (getClan() != null)
		{
			getClan().broadcastToOnlineMembers(new PledgeShowMemberListUpdate(this));
		}
		
		return true;
	}
	
	public void broadcastClassIcon()
	{
		// Update class icon in party and clan
		if (isInParty())
		{
			getParty().broadcastToPartyMembers(new PartySmallWindowUpdate(this));
		}
		
		if (getClan() != null)
		{
			getClan().broadcastToOnlineMembers(new PledgeShowMemberListUpdate(this));
		}
	}
	
	public void stopWarnUserTakeBreak()
	{
		if (_taskWarnUserTakeBreak != null)
		{
			_taskWarnUserTakeBreak.cancel(true);
			_taskWarnUserTakeBreak = null;
		}
	}
	
	public void startWarnUserTakeBreak()
	{
		if (_taskWarnUserTakeBreak == null)
		{
			_taskWarnUserTakeBreak = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new WarnUserTakeBreak(), 7200000, 7200000);
		}
	}
	
	private ScheduledFuture<?> _taskBotChecker;
	protected ScheduledFuture<?> _taskKickBot;
	
	class botChecker implements Runnable
	{
		@Override
		public void run()
		{
			if (Config.BOT_PROTECTOR_INCLUDE_ON.contains("COMBAT") && isInCombat())
			{
				startBotCheckerMain();
				return;
			}
			startBotCheckerMain();
		}
	}
	
	class kickBot implements Runnable
	{
		@Override
		public void run()
		{
			if (isOnline() == 1 && getPrivateStoreType() == 0)
			{
				for (int i = Config.BOT_PROTECTOR_WAIT_ANSVER; i >= 10; i -= 10)
				{
					if (_stopKickBotTask)
					{
						if (_taskKickBot != null)
						{
							_taskKickBot = null;
						}
						_stopKickBotTask = false;
						return;
					}
					
					sendMessage("You have " + i + " seconds to choose the answer.");
					try
					{
						Thread.sleep(10000);
					}
					catch (InterruptedException e)
					{
						if (Config.ENABLE_ALL_EXCEPTIONS)
						{
							e.printStackTrace();
						}
					}
				}
				
				if (_stopKickBotTask)
				{
					if (_taskKickBot != null)
					{
						_taskKickBot = null;
					}
					_stopKickBotTask = false;
					return;
				}
				
				startAbnormalEffect(0x020000);
				setIsParalyzed(true);
				stopMove(null);
				updatePvPFlag(1);
				
				sendPacket(new CreatureSay(2, Say2.TELL, "ANTI-BOT", "Hey, " + getName() + ""));
				sendPacket(new CreatureSay(2, Say2.TELL, "ANTI-BOT", "You've got punishment for no answer."));
				sendPacket(new CreatureSay(2, Say2.TELL, "ANTI-BOT", "Please restart your character."));
				
				PunishmentTable(getName(), "PARALYZE and PVP FLAG for no answer.", "Character effect", "-", punishmentDate, "ANTI-BOT");
			}
			else
			{
				if (_taskKickBot != null)
				{
					_taskKickBot = null;
				}
				_stopKickBotTask = false;
			}
		}
	}
	
	public void startBotChecker()
	{
		if (_taskBotChecker == null)
		{
			if (Config.QUESTION_LIST.size() != 0)
			{
				_taskBotChecker = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new botChecker(), Config.BOT_PROTECTOR_FIRST_CHECK * 60000, Rnd.get(Config.BOT_PROTECTOR_NEXT_CHECK1, Config.BOT_PROTECTOR_NEXT_CHECK2) * 60000);
			}
			else
			{
				LOG.warn("ATTENTION: Bot Checker is bad configured because config/questionwords.txt has 0 words of 6 to 15 keys");
			}
		}
	}
	
	public void startBotCheckerMain()
	{
		if (isOnline() == 1 && getPrivateStoreType() == 0 /* && !isGM() */ && !isInsideZone(ZoneId.ZONE_PEACE) && !isPhantom())
		{
			if (Config.BOT_PROTECTOR_INCLUDE_ON.contains("COMBAT") && !isInCombat())
			{
				stopBotChecker();
				return;
			}
			
			try
			{
				String text = HtmCache.getInstance().getHtm("data/html/custom/bot.htm");
				String word = Config.QUESTION_LIST.get(Rnd.get(Config.QUESTION_LIST.size()));
				String output;
				
				_correctWord = Rnd.get(5) + 1;
				
				text = text.replace("%Time%", Integer.toString(Config.BOT_PROTECTOR_WAIT_ANSVER));
				
				for (int i = 1; i <= 5; i++)
				{
					int green = Rnd.get(20, 99);
					
					if (i != _correctWord)
					{
						output = "<font color=000100>" + RandomStringUtils.random(word.length(), word) + "</font>";
					}
					else
					{
						output = "<font color=00" + green + "00>" + word + "</font>";
					}
					
					text = text.replace("%answer" + i + "%", output);
					
					if (i == _correctWord)
					{
						text = text.replace("%answer%", output);
					}
				}
				
				sendPacket(new NpcHtmlMessage(text));
				
				if (_taskKickBot == null)
				{
					_stopKickBotTask = false;
					_taskKickBot = ThreadPoolManager.getInstance().scheduleGeneral(new kickBot(), 10);
				}
			}
			catch (Exception e)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
				{
					e.printStackTrace();
				}
			}
		}
		else
		{
			stopBotChecker();
		}
	}
	
	public void stopBotChecker()
	{
		if (_taskBotChecker != null)
		{
			_taskBotChecker.cancel(true);
			_taskBotChecker = null;
		}
	}
	
	public void checkAnswer(int id)
	{
		if (id - 100000 == _correctWord)
		{
			_stopKickBotTask = true;
			sendMessage("The Answer is correct!");
		}
		else
		{
			startAbnormalEffect(0x020000);
			setIsParalyzed(true);
			stopMove(null);
			updatePvPFlag(1);
			
			sendPacket(new CreatureSay(2, Say2.TELL, "ANTI-BOT", "Hey, " + getName() + ""));
			sendPacket(new CreatureSay(2, Say2.TELL, "ANTI-BOT", "You've got punishment for wrong answer."));
			sendPacket(new CreatureSay(2, Say2.TELL, "ANTI-BOT", "Please restart your character."));
			
			PunishmentTable(getName(), "PARALYZE and PVP FLAG for wrong answer.", "Character effect", "-", punishmentDate, "ANTI-BOT");
			_stopKickBotTask = true;
		}
	}
	
	public void PunishmentTable(String name, String reason, String type, String time, String date, String punisher)
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("INSERT INTO punishments (name, reason, type, time, date, punisher) values(?,?,?,?,?,?)");
			statement.setString(1, name);
			statement.setString(2, reason);
			statement.setString(3, type);
			statement.setString(4, time);
			statement.setString(5, date);
			statement.setString(6, punisher);
			statement.execute();
			statement.close();
		}
		catch (SQLException e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
		}
		finally
		{
			CloseUtil.close(con);
		}
	}
	
	public void stopRentPet()
	{
		if (_taskRentPet != null)
		{
			// if the rent of a wyvern expires while over a flying zone, tp to down before unmounting
			if (checkLandingState() && getMountType() == 2)
			{
				teleToLocation(MapRegionTable.TeleportWhereType.Town);
			}
			
			if (setMountType(0)) // this should always be true now, since we teleported already
			{
				_taskRentPet.cancel(true);
				Ride dismount = new Ride(getObjectId(), Ride.ACTION_DISMOUNT, 0);
				sendPacket(dismount);
				broadcastPacket(dismount);
			}
		}
	}
	
	public void startRentPet(int seconds)
	{
		if (_taskRentPet == null)
		{
			_taskRentPet = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new RentPetTask(), seconds * 1000L, seconds * 1000L);
		}
	}
	
	public boolean isRentedPet()
	{
		if (_taskRentPet != null)
		{
			return true;
		}
		
		return false;
	}
	
	public void stopWaterTask()
	{
		if (_taskWater != null)
		{
			_taskWater.cancel(false);
			_taskWater = null;
			sendPacket(new SetupGauge(2, 0));
			// for catacombs...
			broadcastUserInfo();
		}
	}
	
	public void startWaterTask()
	{
		if (!isDead() && _taskWater == null)
		{
			int timeinwater = 86000;
			
			sendPacket(new SetupGauge(2, timeinwater));
			_taskWater = ThreadPoolManager.getInstance().scheduleEffectAtFixedRate(new WaterTask(), timeinwater, 1000);
		}
	}
	
	public boolean isInWater()
	{
		if (_taskWater != null)
		{
			return true;
		}
		
		return false;
	}
	
	public void checkWaterState()
	{
		if (isInsideZone(ZoneId.ZONE_WATER))
		{
			startWaterTask();
		}
		else
		{
			stopWaterTask();
		}
	}
	
	public void onPlayerEnter()
	{
		startWarnUserTakeBreak();
		
		if (Config.BOT_PROTECTOR)
		{
			startBotChecker();
		}
		
		if (SevenSigns.getInstance().isSealValidationPeriod() || SevenSigns.getInstance().isCompResultsPeriod())
		{
			if (!isGM() && isIn7sDungeon() && SevenSigns.getInstance().getPlayerCabal(this) != SevenSigns.getInstance().getCabalHighestScore())
			{
				teleToLocation(MapRegionTable.TeleportWhereType.Town);
				setIsIn7sDungeon(false);
				sendMessage("You have been teleported to the nearest town due to the beginning of the Seal Validation period.");
			}
		}
		else
		{
			if (!isGM() && isIn7sDungeon() && SevenSigns.getInstance().getPlayerCabal(this) == SevenSigns.CABAL_NULL)
			{
				teleToLocation(MapRegionTable.TeleportWhereType.Town);
				setIsIn7sDungeon(false);
				sendMessage("You have been teleported to the nearest town because you have not signed for any cabal.");
			}
		}
		
		if (_isInvul)
		{
			sendMessage("Entering world in Invulnerable mode.");
		}
		
		if (getAppearance().getInvisible())
		{
			sendMessage("Entering world in Invisible mode.");
		}
		
		if (getMessageRefusal())
		{
			sendMessage("Entering world in Message Refusal mode.");
		}
		
		revalidateZone(true);
		notifyFriends(true);
		
		if (Config.ACHIEVEMENT_ENABLE)
		{
			getAchievement().load();
		}
		
		if (isAutoPot(728))
		{
			sendPacket(new ExAutoSoulShot(728, 0));
			setAutoPot(728, null, false);
		}
		
		if (isAutoPot(726))
		{
			sendPacket(new ExAutoSoulShot(726, 0));
			setAutoPot(726, null, false);
		}
		
		if (isAutoPot(1539))
		{
			sendPacket(new ExAutoSoulShot(1539, 0));
			setAutoPot(1539, null, false);
		}
		
		if (isAutoPot(1060))
		{
			sendPacket(new ExAutoSoulShot(1060, 0));
			setAutoPot(1060, null, false);
		}
		
		if (isAutoPot(1061))
		{
			sendPacket(new ExAutoSoulShot(1061, 0));
			setAutoPot(1061, null, false);
		}
		
		if (isAutoPot(5592))
		{
			sendPacket(new ExAutoSoulShot(5592, 0));
			setAutoPot(5592, null, false);
		}
		
		if (isAutoPot(5591))
		{
			sendPacket(new ExAutoSoulShot(5591, 0));
			setAutoPot(5591, null, false);
		}
	}
	
	public long getLastAccess()
	{
		return _lastAccess;
	}
	
	private void checkRecom(int recsHave, int recsLeft)
	{
		Calendar check = Calendar.getInstance();
		check.setTimeInMillis(_lastRecomUpdate);
		check.add(Calendar.DAY_OF_MONTH, 1);
		
		Calendar min = Calendar.getInstance();
		
		_recomHave = recsHave;
		_recomLeft = recsLeft;
		
		if (getStat().getLevel() < 10 || check.after(min))
		{
			return;
		}
		
		restartRecom();
	}
	
	public void restartRecom()
	{
		if (Config.ALT_RECOMMEND)
		{
			Connection con = null;
			try
			{
				con = L2DatabaseFactory.getInstance().getConnection();
				PreparedStatement statement = con.prepareStatement(DELETE_CHAR_RECOMS);
				statement.setInt(1, getObjectId());
				statement.execute();
				statement.close();
				statement = null;
				
				_recomChars.clear();
			}
			catch (Exception e)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
				{
					LOG.error("could not clear char recommendations:");
					e.printStackTrace();
				}
			}
			finally
			{
				CloseUtil.close(con);
			}
		}
		
		if (getStat().getLevel() < 20)
		{
			_recomLeft = 3;
			_recomHave--;
		}
		else if (getStat().getLevel() < 40)
		{
			_recomLeft = 6;
			_recomHave -= 2;
		}
		else
		{
			_recomLeft = 9;
			_recomHave -= 3;
		}
		
		if (_recomHave < 0)
		{
			_recomHave = 0;
		}
		
		// If we have to update last update time, but it's now before 13, we should set it to yesterday
		Calendar update = Calendar.getInstance();
		if (update.get(Calendar.HOUR_OF_DAY) < 13)
		{
			update.add(Calendar.DAY_OF_MONTH, -1);
		}
		
		update.set(Calendar.HOUR_OF_DAY, 13);
		_lastRecomUpdate = update.getTimeInMillis();
	}
	
	@Override
	public void doRevive()
	{
		ExRedSky packet = new ExRedSky(0);
		sendPacket(packet);
		
		updateEffectIcons();
		
		sendPacket(new EtcStatusUpdate(this));
		
		_reviveRequested = 0;
		_revivePower = 0;
		
		if (isInParty() && getParty().isInDimensionalRift())
		{
			if (!DimensionalRiftManager.getInstance().checkIfInPeaceZone(getX(), getY(), getZ()))
			{
				getParty().getDimensionalRift().memberRessurected(this);
			}
		}
		
		if ((_inEventTvT && TvT.is_started() && Config.TVT_REVIVE_RECOVERY) || (_inEventCTF && CTF.is_started() && Config.CTF_REVIVE_RECOVERY) || (_inEventDM && DM.is_started() && Config.DM_REVIVE_RECOVERY))
		{
			getStatus().setCurrentHp(getMaxHp());
			getStatus().setCurrentMp(getMaxMp());
			getStatus().setCurrentCp(getMaxCp());
		}
		
		super.doRevive();
	}
	
	@Override
	public void doRevive(double revivePower)
	{
		restoreExp(revivePower);
		getAchievement().increase(AchType.RESSURECTED);
		doRevive();
	}
	
	public void reviveRequest(L2PcInstance Reviver, L2Skill skill, boolean Pet)
	{
		if (_reviveRequested == 1)
		{
			if (_revivePet == Pet)
			{
				Reviver.sendPacket(new SystemMessage(SystemMessageId.RES_HAS_ALREADY_BEEN_PROPOSED)); // Resurrection is already been proposed.
			}
			else
			{
				if (Pet)
				{
					Reviver.sendPacket(new SystemMessage(SystemMessageId.PET_CANNOT_RES)); // A pet cannot be resurrected while it's owner is in the process of resurrecting.
				}
				else
				{
					Reviver.sendPacket(new SystemMessage(SystemMessageId.MASTER_CANNOT_RES)); // While a pet is attempting to resurrect, it cannot help in resurrecting its master.
				}
			}
			return;
		}
		
		if (Pet && getPet() != null && getPet().isDead() || !Pet && isDead())
		{
			_reviveRequested = 1;
			if (isPhoenixBlessed())
			{
				_revivePower = 100;
			}
			else if (skill != null)
			{
				_revivePower = Formulas.getInstance().calculateSkillResurrectRestorePercent(skill.getPower(), Reviver);
			}
			else
			{
				_revivePower = 0;
			}
			_revivePet = Pet;
			
			ConfirmDlg dlg = new ConfirmDlg(SystemMessageId.RESSURECTION_REQUEST.getId());
			dlg.addString(Reviver.getName());
			sendPacket(dlg);
		}
	}
	
	public void reviveAnswer(int answer)
	{
		if (_reviveRequested != 1 || !isDead() && !_revivePet || _revivePet && getPet() != null && !getPet().isDead())
		{
			return;
		}
		
		// If character refuse a PhoenixBlessed autoress, cancel all buffs he had
		if (answer == 0 && isPhoenixBlessed())
		{
			stopPhoenixBlessing(null);
			stopAllEffects();
		}
		
		if (answer == 1)
		{
			if (!_revivePet)
			{
				if (_revivePower != 0)
				{
					doRevive(_revivePower);
				}
				else
				{
					doRevive();
				}
			}
			else if (getPet() != null)
			{
				if (_revivePower != 0)
				{
					getPet().doRevive(_revivePower);
				}
				else
				{
					getPet().doRevive();
				}
			}
		}
		_reviveRequested = 0;
		_revivePower = 0;
	}
	
	public boolean isReviveRequested()
	{
		return _reviveRequested == 1;
	}
	
	public boolean isRevivingPet()
	{
		return _revivePet;
	}
	
	public void removeReviving()
	{
		_reviveRequested = 0;
		_revivePower = 0;
	}
	
	public void onActionRequest()
	{
		if (isSpawnProtected())
		{
			sendMessage("The Spawn Protection has been removed.");
		}
		else if (isTeleportProtected())
		{
			sendMessage("The Teleport Spawn Protection has been removed.");
		}
		
		if (Config.PLAYER_SPAWN_PROTECTION > 0)
		{
			setProtection(false);
		}
		
		if (Config.PLAYER_TELEPORT_PROTECTION > 0)
		{
			setTeleportProtection(false);
		}
	}
	
	public void setExpertiseIndex(int expertiseIndex)
	{
		_expertiseIndex = expertiseIndex;
	}
	
	public int getExpertiseIndex()
	{
		return _expertiseIndex;
	}
	
	@Override
	public final void onTeleported()
	{
		super.onTeleported();
		
		if ((Config.PLAYER_TELEPORT_PROTECTION > 0) && !isInOlympiadMode() && !inObserverMode())
		{
			setTeleportProtection(true);
			sendMessage("The Teleport Protection flow through you.");
		}
		
		// Modify the position of the tamed beast if necessary (normal pets are handled by super...though
		// L2PcInstance is the only class that actually has pets!!! )
		if (getTrainedBeast() != null)
		{
			getTrainedBeast().getAI().stopFollow();
			getTrainedBeast().teleToLocation(getPosition().getX() + Rnd.get(-100, 100), getPosition().getY() + Rnd.get(-100, 100), getPosition().getZ(), false);
			getTrainedBeast().getAI().startFollow(this);
		}
		
		if (!inObserverMode())
		{
			broadcastUserInfo();
		}
		
		revalidateZone(true);
	}
	
	public void setLastClientPosition(int x, int y, int z)
	{
		_lastClientPosition.setXYZ(x, y, z);
	}
	
	public void setLastClientPosition(Location loc)
	{
		_lastClientPosition = loc;
	}
	
	public boolean checkLastClientPosition(int x, int y, int z)
	{
		return _lastClientPosition.equals(x, y, z);
	}
	
	public int getLastClientDistance(int x, int y, int z)
	{
		double dx = x - _lastClientPosition.getX();
		double dy = y - _lastClientPosition.getY();
		double dz = z - _lastClientPosition.getZ();
		
		return (int) Math.sqrt(dx * dx + dy * dy + dz * dz);
	}
	
	public void setLastServerPosition(int x, int y, int z)
	{
		_lastServerPosition.setXYZ(x, y, z);
	}
	
	public void setLastServerPosition(Location loc)
	{
		_lastServerPosition = loc;
	}
	
	public boolean checkLastServerPosition(int x, int y, int z)
	{
		return _lastServerPosition.equals(x, y, z);
	}
	
	public int getLastServerDistance(int x, int y, int z)
	{
		double dx = x - _lastServerPosition.getX();
		double dy = y - _lastServerPosition.getY();
		double dz = z - _lastServerPosition.getZ();
		
		return (int) Math.sqrt(dx * dx + dy * dy + dz * dz);
	}
	
	@Override
	public void addExpAndSp(long addToExp, int addToSp)
	{
		if (getExpOn())
		{
			getStat().addExpAndSp(addToExp, addToSp);
		}
		else
		{
			getStat().addExpAndSp(0, addToSp);
		}
	}
	
	public void removeExpAndSp(long removeExp, int removeSp)
	{
		getStat().removeExpAndSp(removeExp, removeSp);
	}
	
	@Override
	public void reduceCurrentHp(double i, L2Character attacker)
	{
		getStatus().reduceHp(i, attacker);
		
		// notify the tamed beast of attacks
		if (getTrainedBeast() != null)
		{
			getTrainedBeast().onOwnerGotAttacked(attacker);
		}
	}
	
	public boolean teleportRequest(L2PcInstance requester, L2Skill skill)
	{
		if (_summonRequest.getTarget() != null && requester != null)
		{
			return false;
		}
		_summonRequest.setTarget(requester, skill);
		return true;
	}
	
	public void teleportAnswer(int answer, int requesterId)
	{
		if (_summonRequest.getTarget() == null)
		{
			return;
		}
		
		if (answer == 1 && _summonRequest.getTarget().getObjectId() == requesterId)
		{
			teleToTarget(this, _summonRequest.getTarget(), _summonRequest.getSkill());
		}
		_summonRequest.setTarget(null, null);
	}
	
	/**
	 * Tele to target.
	 * @param targetChar the target char
	 * @param summonerChar the summoner char
	 * @param summonSkill the summon skill
	 */
	public static void teleToTarget(L2PcInstance targetChar, L2PcInstance summonerChar, L2Skill summonSkill)
	{
		if (targetChar == null || summonerChar == null || summonSkill == null)
		{
			return;
		}
		
		if (!checkSummonerStatus(summonerChar))
		{
			return;
		}
		
		if (!checkSummonTargetStatus(targetChar, summonerChar))
		{
			return;
		}
		
		int itemConsumeId = summonSkill.getTargetConsumeId();
		int itemConsumeCount = summonSkill.getTargetConsume();
		if (itemConsumeId != 0 && itemConsumeCount != 0)
		{
			if (targetChar.getInventory().getInventoryItemCount(itemConsumeId, 0) < itemConsumeCount)
			{
				SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S1_REQUIRED_FOR_SUMMONING);
				sm.addItemName(summonSkill.getTargetConsumeId());
				targetChar.sendPacket(sm);
				return;
			}
			targetChar.getInventory().destroyItemByItemId("Consume", itemConsumeId, itemConsumeCount, summonerChar, targetChar);
			SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S1_DISAPPEARED);
			sm.addItemName(summonSkill.getTargetConsumeId());
			targetChar.sendPacket(sm);
		}
		
		targetChar.teleToLocation(summonerChar.getX(), summonerChar.getY(), summonerChar.getZ(), summonerChar.getHeading(), true, false);
	}
	
	public static boolean checkSummonerStatus(L2PcInstance summonerChar)
	{
		if (summonerChar == null)
		{
			return false;
		}
		
		if (summonerChar.isFlying() || summonerChar.isMounted())
		{
			return false;
		}
		
		if (summonerChar.isInOlympiadMode() || summonerChar.inObserverMode() || summonerChar.isInsideZone(ZoneId.ZONE_NOSUMMONFRIEND))
		{
			summonerChar.sendPacket(SystemMessageId.YOU_MAY_NOT_SUMMON_FROM_YOUR_CURRENT_LOCATION);
			return false;
		}
		
		return true;
	}
	
	public static boolean checkSummonTargetStatus(L2Object target, L2PcInstance summonerChar)
	{
		if (target == null || !(target instanceof L2PcInstance))
		{
			return false;
		}
		
		L2PcInstance targetChar = (L2PcInstance) target;
		
		if (targetChar.isAlikeDead())
		{
			summonerChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_IS_DEAD_AT_THE_MOMENT_AND_CANNOT_BE_SUMMONED).addCharName(targetChar));
			return false;
		}
		
		if (targetChar.isInStoreMode())
		{
			summonerChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_CURRENTLY_TRADING_OR_OPERATING_PRIVATE_STORE_AND_CANNOT_BE_SUMMONED).addCharName(targetChar));
			return false;
		}
		
		if (targetChar.isRooted() || targetChar.isInCombat())
		{
			summonerChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_IS_ENGAGED_IN_COMBAT_AND_CANNOT_BE_SUMMONED).addCharName(targetChar));
			return false;
		}
		
		if (targetChar.isInOlympiadMode())
		{
			summonerChar.sendPacket(SystemMessageId.YOU_CANNOT_SUMMON_PLAYERS_WHO_ARE_IN_OLYMPIAD);
			return false;
		}
		
		if (targetChar.isFestivalParticipant() || targetChar.isMounted())
		{
			summonerChar.sendPacket(SystemMessageId.YOUR_TARGET_IS_IN_AN_AREA_WHICH_BLOCKS_SUMMONING);
			return false;
		}
		
		if (targetChar.inObserverMode() || targetChar.isInsideZone(ZoneId.ZONE_NOSUMMONFRIEND))
		{
			summonerChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_IN_SUMMON_BLOCKING_AREA).addCharName(targetChar));
			return false;
		}
		
		return true;
	}
	
	@Override
	public void reduceCurrentHp(double value, L2Character attacker, boolean awake)
	{
		getStatus().reduceHp(value, attacker, awake);
		
		if (getTrainedBeast() != null)
		{
			getTrainedBeast().onOwnerGotAttacked(attacker);
		}
	}
	
	public void broadcastSnoop(int type, String name, String _text)
	{
		if (_snoopListener.size() > 0)
		{
			Snoop sn = new Snoop(getObjectId(), getName(), type, name, _text);
			
			for (L2PcInstance pci : _snoopListener)
			{
				if (pci != null)
				{
					pci.sendPacket(sn);
				}
			}
		}
	}
	
	public void addSnooper(L2PcInstance pci)
	{
		if (!_snoopListener.contains(pci))
		{
			_snoopListener.add(pci);
		}
	}
	
	public void removeSnooper(L2PcInstance pci)
	{
		_snoopListener.remove(pci);
	}
	
	public void addSnooped(L2PcInstance pci)
	{
		if (!_snoopedPlayer.contains(pci))
		{
			_snoopedPlayer.add(pci);
		}
	}
	
	public void removeSnooped(L2PcInstance pci)
	{
		_snoopedPlayer.remove(pci);
	}
	
	public synchronized void addBypass(String bypass)
	{
		if (bypass == null)
		{
			return;
		}
		_validBypass.add(bypass);
	}
	
	public synchronized void addBypass2(String bypass)
	{
		if (bypass == null)
		{
			return;
		}
		_validBypass2.add(bypass);
	}
	
	public synchronized boolean validateBypass(String cmd)
	{
		if (!Config.BYPASS_VALIDATION)
		{
			return true;
		}
		
		for (String bp : _validBypass)
		{
			if (bp == null)
			{
				continue;
			}
			
			if (bp.equals(cmd))
			{
				return true;
			}
		}
		
		for (String bp : _validBypass2)
		{
			if (bp == null)
			{
				continue;
			}
			
			if (cmd.startsWith(bp))
			{
				return true;
			}
		}
		
		L2PcInstance player = getClient().getActiveChar();
		
		// We decided to put a kick because when a player is doing quest with a BOT he sends invalid bypass.
		Util.handleIllegalPlayerAction(player, " player [" + player.getName() + "] sent invalid bypass '" + cmd + "'", Config.DEFAULT_PUNISH);
		return false;
	}
	
	public boolean validateItemManipulationByItemId(int itemId, String action)
	{
		L2ItemInstance item = getInventory().getItemByItemId(itemId);
		
		if (item == null || item.getOwnerId() != getObjectId())
		{
			LOG.warn(getObjectId() + ": player tried to " + action + " item he is not owner of");
			return false;
		}
		if (getActiveEnchantItem() != null && getActiveEnchantItem().getItemId() == itemId)
		{
			LOG.warn(getObjectId() + ":player tried to " + action + " an enchant scroll he was using");
			return false;
		}
		
		if (CursedWeaponsManager.getInstance().isCursed(itemId))
		{
			// can not trade a cursed weapon
			return false;
		}
		
		if (item.isWear())
		{
			// cannot drop/trade wear-items
			return false;
		}
		
		return true;
	}
	
	public boolean validateItemManipulation(int objectId, String action)
	{
		L2ItemInstance item = getInventory().getItemByObjectId(objectId);
		
		if (item == null || item.getOwnerId() != getObjectId())
		{
			LOG.warn(getObjectId() + ": player tried to " + action + " item he is not owner of");
			return false;
		}
		
		// Pet is summoned and not the item that summoned the pet AND not the buggle from strider you're mounting
		if (getPet() != null && getPet().getControlItemId() == objectId || getMountObjectID() == objectId)
		{
			if (Config.DEBUG)
			{
				LOG.warn(getObjectId() + ": player tried to " + action + " item controling pet");
			}
			
			return false;
		}
		
		if (getActiveEnchantItem() != null && getActiveEnchantItem().getObjectId() == objectId)
		{
			if (Config.DEBUG)
			{
				LOG.warn(getObjectId() + ":player tried to " + action + " an enchant scroll he was using");
			}
			
			return false;
		}
		
		if (CursedWeaponsManager.getInstance().isCursed(item.getItemId()))
		{
			// can not trade a cursed weapon
			return false;
		}
		
		if (item.isWear())
		{
			// cannot drop/trade wear-items
			return false;
		}
		
		return true;
	}
	
	public synchronized void clearBypass()
	{
		_validBypass.clear();
		_validBypass2.clear();
	}
	
	public synchronized boolean validateLink(String cmd)
	{
		if (!Config.BYPASS_VALIDATION)
		{
			return true;
		}
		
		for (String bp : _validLink)
		{
			if (bp == null)
			{
				continue;
			}
			
			if (bp.equals(cmd))
			{
				return true;
			}
		}
		
		Util.handleIllegalPlayerAction(this, " player [" + getName() + "] sent invalid link '" + cmd + "'", Config.DEFAULT_PUNISH);
		return false;
	}
	
	public synchronized void clearLinks()
	{
		_validLink.clear();
	}
	
	public synchronized void addLink(String link)
	{
		if (link == null)
		{
			return;
		}
		_validLink.add(link);
	}
	
	public boolean isInBoat()
	{
		return (_vehicle != null) && _vehicle.isBoat();
	}
	
	public L2BoatInstance getBoat()
	{
		return (L2BoatInstance) _vehicle;
	}
	
	public L2Vehicle getVehicle()
	{
		return _vehicle;
	}
	
	public void setVehicle(L2Vehicle v)
	{
		if ((v == null) && (_vehicle != null))
		{
			_vehicle.removePassenger(this);
		}
		
		_vehicle = v;
	}
	
	/**
	 * @return
	 */
	public Location getInVehiclePosition()
	{
		return _inVehiclePosition;
	}
	
	public void setInVehiclePosition(Location pos)
	{
		_inVehiclePosition = pos;
	}
	
	/**
	 * Sets the in crystallize.
	 * @param inCrystallize the new in crystallize
	 */
	public void setInCrystallize(boolean inCrystallize)
	{
		_inCrystallize = inCrystallize;
	}
	
	/**
	 * Checks if is in crystallize.
	 * @return true, if is in crystallize
	 */
	public boolean isInCrystallize()
	{
		return _inCrystallize;
	}
	
	/**
	 * Manage the delete task of a L2PcInstance (Leave Party, Unsummon pet, Save its inventory in the database, Remove it from the world...).<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>If the L2PcInstance is in observer mode, set its position to its position before entering in observer mode</li>
	 * <li>Set the online Flag to True or False and update the characters table of the database with online status and lastAccess</li>
	 * <li>Stop the HP/MP/CP Regeneration task</li>
	 * <li>Cancel Crafting, Attak or Cast</li>
	 * <li>Remove the L2PcInstance from the world</li>
	 * <li>Stop Party and Unsummon Pet</li>
	 * <li>Update database with items in its inventory and remove them from the world</li>
	 * <li>Remove all L2Object from _knownObjects and _knownPlayer of the L2Character then cancel Attak or Cast and notify AI</li>
	 * <li>Close the connection with the client</li><BR>
	 * <BR>
	 */
	@Override
	public synchronized void deleteMe()
	{
		// Check if the L2PcInstance is in observer mode to set its position to its position before entering in observer mode
		if (inObserverMode())
		{
			setXYZInvisible(_lastLoc.getX(), _lastLoc.getY(), _lastLoc.getZ());
		}
		
		if (isTeleporting())
		{
			try
			{
				wait(2000);
			}
			catch (Exception e)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
				{
					e.printStackTrace();
				}
			}
			onTeleported();
		}
		
		Castle castle = null;
		if (getClan() != null)
		{
			castle = CastleManager.getInstance().getCastleByOwner(getClan());
			if (castle != null)
			{
				castle.destroyClanGate();
			}
		}
		
		// Set the online Flag to True or False and update the characters table of the database with online status and lastAccess (called when login and logout)
		try
		{
			setOnlineStatus(false);
		}
		catch (Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				LOG.error("deleteMe()", e);
			}
		}
		
		// Stop the HP/MP/CP Regeneration task (scheduled tasks)
		try
		{
			stopAllTimers();
		}
		catch (Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				LOG.error("deleteMe()", e);
			}
			
		}
		
		// Stop crafting, if in progress
		try
		{
			RecipeController.getInstance().requestMakeItemAbort(this);
		}
		catch (Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				LOG.error("deleteMe()", e);
			}
		}
		
		// Cancel Attak or Cast
		try
		{
			abortAttack();
			abortCast();
			setTarget(null);
		}
		catch (Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				LOG.error("deleteMe()", e);
			}
		}
		
		PartyMatchWaitingList.getInstance().removePlayer(this);
		if (_partyroom != 0)
		{
			PartyMatchRoom room = PartyMatchRoomList.getInstance().getRoom(_partyroom);
			if (room != null)
			{
				room.deleteMember(this);
			}
		}
		
		// Remove from world regions zones
		if (getWorldRegion() != null)
		{
			getWorldRegion().removeFromZones(this);
		}
		
		try
		{
			if (_forceBuff != null)
			{
				abortCast();
			}
			
			for (L2Character character : getKnownList().getKnownCharacters())
			{
				if (character.getForceBuff() != null && character.getForceBuff().getTarget() == this)
				{
					character.abortCast();
				}
			}
		}
		catch (Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				LOG.error("deleteMe()", e);
			}
		}
		
		// Remove the L2PcInstance from the world
		if (isVisible())
		{
			try
			{
				decayMe();
			}
			catch (Exception e)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
				{
					LOG.error("deleteMe()", e);
				}
			}
		}
		
		// If a Party is in progress, leave it
		if (isInParty())
		{
			try
			{
				leaveParty();
			}
			catch (Exception e)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
				{
					LOG.error("deleteMe()", e);
				}
			}
		}
		
		// If the L2PcInstance has Pet, unsummon it
		if (getPet() != null)
		{
			try
			{
				getPet().unSummon(this);
			}
			catch (Exception e)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
				{
					LOG.error("deleteMe()", e);
				}
			}
		}
		
		if (getClanId() != 0 && getClan() != null)
		{
			// set the status for pledge member list to OFFLINE
			try
			{
				L2ClanMember clanMember = getClan().getClanMember(getName());
				if (clanMember != null)
				{
					clanMember.setPlayerInstance(null);
				}
			}
			catch (Exception e)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
				{
					LOG.error("deleteMe()", e);
				}
			}
		}
		
		if (getActiveRequester() != null)
		{
			// deals with sudden exit in the middle of transaction
			setActiveRequester(null);
		}
		
		if (OlympiadManager.getInstance().isRegistered(this) || getOlympiadGameId() != -1)
		{
			OlympiadManager.getInstance().removeDisconnectedCompetitor(this);
		}
		
		// If the L2PcInstance is a GM, remove it from the GM List
		if (isGM())
		{
			try
			{
				GmListTable.getInstance().deleteGm(this);
			}
			catch (Exception e)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
				{
					LOG.error("deleteMe()", e);
				}
			}
		}
		
		// Update database with items in its inventory and remove them from the world
		try
		{
			getInventory().deleteMe();
		}
		catch (Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				LOG.error("deleteMe()", e);
			}
		}
		
		// Update database with items in its warehouse and remove them from the world
		try
		{
			clearWarehouse();
		}
		catch (Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				LOG.error("deleteMe()", e);
			}
		}
		
		if (Config.WAREHOUSE_CACHE)
		{
			WarehouseCacheManager.getInstance().remCacheTask(this);
		}
		
		// Update database with items in its freight and remove them from the world
		try
		{
			getFreight().deleteMe();
		}
		catch (Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				LOG.error("deleteMe()", e);
			}
		}
		
		// Remove all L2Object from _knownObjects and _knownPlayer of the L2Character then cancel Attak or Cast and notify AI
		try
		{
			getKnownList().removeAllKnownObjects();
		}
		catch (Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				LOG.error("deleteMe()", e);
			}
		}
		
		closeNetConnection();
		
		if (getClanId() > 0)
		{
			getClan().broadcastToOtherOnlineMembers(new PledgeShowMemberListUpdate(this), this);
		}
		
		for (L2PcInstance player : _snoopedPlayer)
		{
			player.removeSnooper(this);
		}
		
		for (L2PcInstance player : _snoopListener)
		{
			player.removeSnooped(this);
		}
		
		if (_chanceSkills != null)
		{
			_chanceSkills.setOwner(null);
			_chanceSkills = null;
		}
		
		notifyFriends(false);
		
		L2World.getInstance().removeObject(this);
		L2World.getInstance().removeFromAllPlayers(this); // force remove in case of crash during teleport
		
	}
	
	/** ShortBuff clearing Task */
	private ScheduledFuture<?> _shortBuffTask = null;
	
	private class ShortBuffTask implements Runnable
	{
		private L2PcInstance _player = null;
		
		public ShortBuffTask(L2PcInstance activeChar)
		{
			_player = activeChar;
		}
		
		@Override
		public void run()
		{
			if (_player == null)
			{
				return;
			}
			
			_player.sendPacket(new ShortBuffStatusUpdate(0, 0, 0));
		}
	}
	
	public void shortBuffStatusUpdate(int magicId, int level, int time)
	{
		if (_shortBuffTask != null)
		{
			_shortBuffTask.cancel(false);
			_shortBuffTask = null;
		}
		
		_shortBuffTask = ThreadPoolManager.getInstance().scheduleGeneral(new ShortBuffTask(this), 15000);
		
		sendPacket(new ShortBuffStatusUpdate(magicId, level, time));
	}
	
	public List<Integer> getFriendList()
	{
		return _friendList;
	}
	
	public void selectFriend(Integer friendId)
	{
		_selectedFriendList.add(friendId);
	}
	
	public void deselectFriend(Integer friendId)
	{
		_selectedFriendList.remove(friendId);
	}
	
	public List<Integer> getSelectedFriendList()
	{
		return _selectedFriendList;
	}
	
	private void restoreFriendList()
	{
		_friendList.clear();
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT friend_id FROM character_friends WHERE char_id = ? AND relation = 0");
			statement.setInt(1, getObjectId());
			ResultSet rset = statement.executeQuery();
			
			int friendId;
			while (rset.next())
			{
				friendId = rset.getInt("friend_id");
				if (friendId == getObjectId())
				{
					continue;
				}
				
				_friendList.add(friendId);
			}
			
			rset.close();
			statement.close();
		}
		catch (Exception e)
		{
			LOG.warn("Error found in " + getName() + "'s friendlist: " + e.getMessage(), e);
		}
		finally
		{
			CloseUtil.close(con);
		}
	}
	
	private void notifyFriends(boolean login)
	{
		for (int id : _friendList)
		{
			L2PcInstance friend = L2World.getInstance().getPlayer(id);
			
			if (friend != null) // friend logged in.
			{
				friend.sendPacket(new FriendList(friend));
				
				if (login)
				{
					friend.sendPacket(new SystemMessage(SystemMessageId.FRIEND_S1_HAS_LOGGED_IN).addString(this.getName()));
				}
			}
		}
	}
	
	/** The _fish. */
	private FishData _fish;
	
	/**
	 * Start fishing.
	 * @param _x
	 * @param _y
	 * @param _z
	 */
	public void startFishing(int _x, int _y, int _z)
	{
		stopMove(null);
		setIsImobilised(true);
		_fishing = true;
		_fishx = _x;
		_fishy = _y;
		_fishz = _z;
		broadcastUserInfo();
		// Starts fishing
		int lvl = GetRandomFishLvl();
		int group = GetRandomGroup();
		int type = GetRandomFishType(group);
		List<FishData> fishs = FishTable.getInstance().getfish(lvl, type, group);
		if (fishs == null || fishs.size() == 0)
		{
			sendMessage("Error - Fishes are not definied");
			EndFishing(false);
			return;
		}
		int check = Rnd.get(fishs.size());
		// Use a copy constructor else the fish data may be over-written below
		_fish = new FishData(fishs.get(check));
		fishs.clear();
		sendPacket(new SystemMessage(SystemMessageId.CAST_LINE_AND_START_FISHING));
		ExFishingStart efs = null;
		if (!GameTimeController.getInstance().isNight() && _lure.isNightLure())
		{
			_fish.setType(-1);
		}
		
		efs = new ExFishingStart(this, _fish.getType(), _x, _y, _z, _lure.isNightLure());
		broadcastPacket(efs);
		StartLookingForFishTask();
	}
	
	/**
	 * Stop looking for fish task.
	 */
	public void stopLookingForFishTask()
	{
		if (_taskforfish != null)
		{
			_taskforfish.cancel(false);
			_taskforfish = null;
		}
	}
	
	/**
	 * Start looking for fish task.
	 */
	public void StartLookingForFishTask()
	{
		if (!isDead() && _taskforfish == null)
		{
			int checkDelay = 0;
			boolean isNoob = false;
			boolean isUpperGrade = false;
			
			if (_lure != null)
			{
				int lureid = _lure.getItemId();
				isNoob = _fish.getGroup() == 0;
				isUpperGrade = _fish.getGroup() == 2;
				if (lureid == 6519 || lureid == 6522 || lureid == 6525 || lureid == 8505 || lureid == 8508 || lureid == 8511)
				{
					checkDelay = Math.round((float) (_fish.getGutsCheckTime() * 1.33));
				}
				else if (lureid == 6520 || lureid == 6523 || lureid == 6526 || lureid >= 8505 && lureid <= 8513 || lureid >= 7610 && lureid <= 7613 || lureid >= 7807 && lureid <= 7809 || lureid >= 8484 && lureid <= 8486)
				{
					checkDelay = Math.round((float) (_fish.getGutsCheckTime() * 1.00));
				}
				else if (lureid == 6521 || lureid == 6524 || lureid == 6527 || lureid == 8507 || lureid == 8510 || lureid == 8513)
				{
					checkDelay = Math.round((float) (_fish.getGutsCheckTime() * 0.66));
				}
			}
			_taskforfish = ThreadPoolManager.getInstance().scheduleEffectAtFixedRate(new LookingForFishTask(_fish.getWaitTime(), _fish.getFishGuts(), _fish.getType(), isNoob, isUpperGrade), 10000, checkDelay);
		}
	}
	
	/**
	 * Gets the random group.
	 * @return the int
	 */
	private int GetRandomGroup()
	{
		switch (_lure.getItemId())
		{
			case 7807: // green for beginners
			case 7808: // purple for beginners
			case 7809: // yellow for beginners
			case 8486: // prize-winning for beginners
				return 0;
			case 8485: // prize-winning luminous
			case 8506: // green luminous
			case 8509: // purple luminous
			case 8512: // yellow luminous
				return 2;
			default:
				return 1;
		}
	}
	
	/**
	 * Gets the random fish type.
	 * @param group the group
	 * @return the int
	 */
	private int GetRandomFishType(int group)
	{
		int check = Rnd.get(100);
		int type = 1;
		switch (group)
		{
			case 0: // fish for novices
				switch (_lure.getItemId())
				{
					case 7807: // green lure, preferred by fast-moving (nimble) fish (type 5)
						if (check <= 54)
						{
							type = 5;
						}
						else if (check <= 77)
						{
							type = 4;
						}
						else
						{
							type = 6;
						}
						break;
					case 7808: // purple lure, preferred by fat fish (type 4)
						if (check <= 54)
						{
							type = 4;
						}
						else if (check <= 77)
						{
							type = 6;
						}
						else
						{
							type = 5;
						}
						break;
					case 7809: // yellow lure, preferred by ugly fish (type 6)
						if (check <= 54)
						{
							type = 6;
						}
						else if (check <= 77)
						{
							type = 5;
						}
						else
						{
							type = 4;
						}
						break;
					case 8486: // prize-winning fishing lure for beginners
						if (check <= 33)
						{
							type = 4;
						}
						else if (check <= 66)
						{
							type = 5;
						}
						else
						{
							type = 6;
						}
						break;
				}
				break;
			case 1: // normal fish
				switch (_lure.getItemId())
				{
					case 7610:
					case 7611:
					case 7612:
					case 7613:
						type = 3;
						break;
					case 6519: // all theese lures (green) are prefered by fast-moving (nimble) fish (type 1)
					case 8505:
					case 6520:
					case 6521:
					case 8507:
						if (check <= 54)
						{
							type = 1;
						}
						else if (check <= 74)
						{
							type = 0;
						}
						else if (check <= 94)
						{
							type = 2;
						}
						else
						{
							type = 3;
						}
						break;
					case 6522: // all theese lures (purple) are prefered by fat fish (type 0)
					case 8508:
					case 6523:
					case 6524:
					case 8510:
						if (check <= 54)
						{
							type = 0;
						}
						else if (check <= 74)
						{
							type = 1;
						}
						else if (check <= 94)
						{
							type = 2;
						}
						else
						{
							type = 3;
						}
						break;
					case 6525: // all theese lures (yellow) are prefered by ugly fish (type 2)
					case 8511:
					case 6526:
					case 6527:
					case 8513:
						if (check <= 55)
						{
							type = 2;
						}
						else if (check <= 74)
						{
							type = 1;
						}
						else if (check <= 94)
						{
							type = 0;
						}
						else
						{
							type = 3;
						}
						break;
					case 8484: // prize-winning fishing lure
						if (check <= 33)
						{
							type = 0;
						}
						else if (check <= 66)
						{
							type = 1;
						}
						else
						{
							type = 2;
						}
						break;
				}
				break;
			case 2: // upper grade fish, luminous lure
				switch (_lure.getItemId())
				{
					case 8506: // green lure, preferred by fast-moving (nimble) fish (type 8)
						if (check <= 54)
						{
							type = 8;
						}
						else if (check <= 77)
						{
							type = 7;
						}
						else
						{
							type = 9;
						}
						break;
					case 8509: // purple lure, preferred by fat fish (type 7)
						if (check <= 54)
						{
							type = 7;
						}
						else if (check <= 77)
						{
							type = 9;
						}
						else
						{
							type = 8;
						}
						break;
					case 8512: // yellow lure, preferred by ugly fish (type 9)
						if (check <= 54)
						{
							type = 9;
						}
						else if (check <= 77)
						{
							type = 8;
						}
						else
						{
							type = 7;
						}
						break;
					case 8485: // prize-winning fishing lure
						if (check <= 33)
						{
							type = 7;
						}
						else if (check <= 66)
						{
							type = 8;
						}
						else
						{
							type = 9;
						}
						break;
				}
		}
		return type;
	}
	
	/**
	 * Gets the random fish lvl.
	 * @return the int
	 */
	private int GetRandomFishLvl()
	{
		L2Effect[] effects = getAllEffects();
		int skilllvl = getSkillLevel(1315);
		for (L2Effect e : effects)
		{
			if (e.getSkill().getId() == 2274)
			{
				skilllvl = (int) e.getSkill().getPower(this);
			}
		}
		if (skilllvl <= 0)
		{
			return 1;
		}
		int randomlvl;
		int check = Rnd.get(100);
		
		if (check <= 50)
		{
			randomlvl = skilllvl;
		}
		else if (check <= 85)
		{
			randomlvl = skilllvl - 1;
			if (randomlvl <= 0)
			{
				randomlvl = 1;
			}
		}
		else
		{
			randomlvl = skilllvl + 1;
			if (randomlvl > 27)
			{
				randomlvl = 27;
			}
		}
		effects = null;
		
		return randomlvl;
	}
	
	/**
	 * Start fish combat.
	 * @param isNoob the is noob
	 * @param isUpperGrade the is upper grade
	 */
	public void StartFishCombat(boolean isNoob, boolean isUpperGrade)
	{
		_fishCombat = new L2Fishing(this, _fish, isNoob, isUpperGrade);
	}
	
	/**
	 * End fishing.
	 * @param win the win
	 */
	public void EndFishing(boolean win)
	{
		ExFishingEnd efe = new ExFishingEnd(win, this);
		broadcastPacket(efe);
		efe = null;
		_fishing = false;
		_fishx = 0;
		_fishy = 0;
		_fishz = 0;
		broadcastUserInfo();
		
		if (_fishCombat == null)
		{
			sendPacket(new SystemMessage(SystemMessageId.BAIT_LOST_FISH_GOT_AWAY));
		}
		
		_fishCombat = null;
		_lure = null;
		_fishingLoc = null;
		// Ends fishing
		sendPacket(new SystemMessage(SystemMessageId.REEL_LINE_AND_STOP_FISHING));
		setIsImobilised(false);
		stopLookingForFishTask();
	}
	
	/**
	 * Gets the fish combat.
	 * @return the l2 fishing
	 */
	public L2Fishing GetFishCombat()
	{
		return _fishCombat;
	}
	
	public Location getFishingLoc()
	{
		return _fishingLoc;
	}
	
	public int getFishx()
	{
		return _fishx;
	}
	
	public int getFishy()
	{
		return _fishy;
	}
	
	public int getFishz()
	{
		return _fishz;
	}
	
	/**
	 * Sets the lure.
	 * @param lure the lure
	 */
	public void SetLure(L2ItemInstance lure)
	{
		_lure = lure;
	}
	
	/**
	 * Gets the lure.
	 * @return the l2 item instance
	 */
	public L2ItemInstance GetLure()
	{
		return _lure;
	}
	
	public void SetPartyFind(int find)
	{
		_party_find = find;
	}
	
	public int GetPartyFind()
	{
		return _party_find;
	}
	
	/**
	 * Gets the inventory limit.
	 * @return the int
	 */
	public int getInventoryLimit()
	{
		int ivlim;
		if (isGM())
		{
			ivlim = Config.INVENTORY_MAXIMUM_GM;
		}
		else if (getRace() == Race.dwarf)
		{
			ivlim = Config.INVENTORY_MAXIMUM_DWARF;
		}
		else
		{
			ivlim = Config.INVENTORY_MAXIMUM_NO_DWARF;
		}
		ivlim += (int) getStat().calcStat(Stats.INV_LIM, 0, null, null);
		
		return ivlim;
	}
	
	public static int getQuestInventoryLimit()
	{
		return Config.INVENTORY_MAXIMUM_QUEST_ITEMS;
	}
	
	/**
	 * Gets the ware house limit.
	 * @return the int
	 */
	public int GetWareHouseLimit()
	{
		int whlim;
		if (getRace() == Race.dwarf)
		{
			whlim = Config.WAREHOUSE_SLOTS_DWARF;
		}
		else
		{
			whlim = Config.WAREHOUSE_SLOTS_NO_DWARF;
		}
		whlim += (int) getStat().calcStat(Stats.WH_LIM, 0, null, null);
		
		return whlim;
	}
	
	/**
	 * Gets the private sell store limit.
	 * @return the int
	 */
	public int GetPrivateSellStoreLimit()
	{
		int pslim;
		if (getRace() == Race.dwarf)
		{
			pslim = Config.MAX_PVTSTORE_SLOTS_DWARF;
		}
		
		else
		{
			pslim = Config.MAX_PVTSTORE_SLOTS_OTHER;
		}
		pslim += (int) getStat().calcStat(Stats.P_SELL_LIM, 0, null, null);
		
		return pslim;
	}
	
	/**
	 * Gets the private buy store limit.
	 * @return the int
	 */
	public int GetPrivateBuyStoreLimit()
	{
		int pblim;
		if (getRace() == Race.dwarf)
		{
			pblim = Config.MAX_PVTSTORE_SLOTS_DWARF;
		}
		else
		{
			pblim = Config.MAX_PVTSTORE_SLOTS_OTHER;
		}
		pblim += (int) getStat().calcStat(Stats.P_BUY_LIM, 0, null, null);
		
		return pblim;
	}
	
	/**
	 * Gets the freight limit.
	 * @return the int
	 */
	public int GetFreightLimit()
	{
		return Config.FREIGHT_SLOTS + (int) getStat().calcStat(Stats.FREIGHT_LIM, 0, null, null);
	}
	
	/**
	 * Gets the dwarf recipe limit.
	 * @return the int
	 */
	public int GetDwarfRecipeLimit()
	{
		int recdlim = Config.DWARF_RECIPE_LIMIT;
		recdlim += (int) getStat().calcStat(Stats.REC_D_LIM, 0, null, null);
		return recdlim;
	}
	
	/**
	 * Gets the common recipe limit.
	 * @return the int
	 */
	public int GetCommonRecipeLimit()
	{
		int recclim = Config.COMMON_RECIPE_LIMIT;
		recclim += (int) getStat().calcStat(Stats.REC_C_LIM, 0, null, null);
		return recclim;
	}
	
	/**
	 * Sets the mount object id.
	 * @param newID the new mount object id
	 */
	public void setMountObjectID(int newID)
	{
		_mountObjectID = newID;
	}
	
	/**
	 * Gets the mount object id.
	 * @return the mount object id
	 */
	public int getMountObjectID()
	{
		return _mountObjectID;
	}
	
	/** The _lure. */
	private L2ItemInstance _lure = null;
	
	/**
	 * Get the current skill in use or return null.<BR>
	 * <BR>
	 * @return the current skill
	 */
	public SkillDat getCurrentSkill()
	{
		return _currentSkill;
	}
	
	/**
	 * Create a new SkillDat object and set the player _currentSkill.<BR>
	 * <BR>
	 * @param currentSkill the current skill
	 * @param ctrlPressed the ctrl pressed
	 * @param shiftPressed the shift pressed
	 */
	public void setCurrentSkill(L2Skill currentSkill, boolean ctrlPressed, boolean shiftPressed)
	{
		if (currentSkill == null)
		{
			if (Config.DEBUG)
			{
				LOG.info("Setting current skill: NULL for " + getName() + ".");
			}
			
			_currentSkill = null;
			return;
		}
		
		if (Config.DEBUG)
		{
			LOG.info("Setting current skill: " + currentSkill.getName() + " (ID: " + currentSkill.getId() + ") for " + getName() + ".");
		}
		
		_currentSkill = new SkillDat(currentSkill, ctrlPressed, shiftPressed);
	}
	
	/**
	 * Gets the queued skill.
	 * @return the queued skill
	 */
	public SkillDat getQueuedSkill()
	{
		return _queuedSkill;
	}
	
	/**
	 * Create a new SkillDat object and queue it in the player _queuedSkill.<BR>
	 * <BR>
	 * @param queuedSkill the queued skill
	 * @param ctrlPressed the ctrl pressed
	 * @param shiftPressed the shift pressed
	 */
	public void setQueuedSkill(L2Skill queuedSkill, boolean ctrlPressed, boolean shiftPressed)
	{
		if (queuedSkill == null)
		{
			if (Config.DEBUG)
			{
				LOG.info("Setting queued skill: NULL for " + getName() + ".");
			}
			
			_queuedSkill = null;
			return;
		}
		
		if (Config.DEBUG)
		{
			LOG.info("Setting queued skill: " + queuedSkill.getName() + " (ID: " + queuedSkill.getId() + ") for " + getName() + ".");
		}
		
		_queuedSkill = new SkillDat(queuedSkill, ctrlPressed, shiftPressed);
	}
	
	/**
	 * Gets the power grade.
	 * @return the power grade
	 */
	public int getPowerGrade()
	{
		return _powerGrade;
	}
	
	/**
	 * Sets the power grade.
	 * @param power the new power grade
	 */
	public void setPowerGrade(int power)
	{
		_powerGrade = power;
	}
	
	/**
	 * Checks if is cursed weapon equiped.
	 * @return true, if is cursed weapon equiped
	 */
	public boolean isCursedWeaponEquiped()
	{
		return _cursedWeaponEquipedId != 0;
	}
	
	/**
	 * Sets the cursed weapon equiped id.
	 * @param value the new cursed weapon equiped id
	 */
	public void setCursedWeaponEquipedId(int value)
	{
		_cursedWeaponEquipedId = value;
	}
	
	/**
	 * Gets the cursed weapon equiped id.
	 * @return the cursed weapon equiped id
	 */
	public int getCursedWeaponEquipedId()
	{
		return _cursedWeaponEquipedId;
	}
	
	/** The _charm of courage. */
	private boolean _charmOfCourage = false;
	
	/**
	 * Gets the charm of courage.
	 * @return the charm of courage
	 */
	public boolean getCharmOfCourage()
	{
		return _charmOfCourage;
	}
	
	/**
	 * Sets the charm of courage.
	 * @param val the new charm of courage
	 */
	public void setCharmOfCourage(boolean val)
	{
		_charmOfCourage = val;
		sendPacket(new EtcStatusUpdate(this));
	}
	
	/**
	 * Gets the death penalty buff level.
	 * @return the death penalty buff level
	 */
	public int getDeathPenaltyBuffLevel()
	{
		return _deathPenaltyBuffLevel;
	}
	
	/**
	 * Sets the death penalty buff level.
	 * @param level the new death penalty buff level
	 */
	public void setDeathPenaltyBuffLevel(int level)
	{
		_deathPenaltyBuffLevel = level;
	}
	
	/**
	 * Calculate death penalty buff level.
	 * @param killer the killer
	 */
	public void calculateDeathPenaltyBuffLevel(L2Character killer)
	{
		if (isLucky())
		{
			return;
		}
		
		if (Rnd.get(100) <= Config.DEATH_PENALTY_CHANCE && !(killer instanceof L2PcInstance) && !isGM() && !(getCharmOfLuck() && (killer instanceof L2GrandBossInstance || killer instanceof L2RaidBossInstance)) && !(isInsideZone(ZoneId.ZONE_PVP) || isInsideZone(ZoneId.ZONE_SIEGE)))
		{
			increaseDeathPenaltyBuffLevel();
		}
	}
	
	/**
	 * Increase death penalty buff level.
	 */
	public void increaseDeathPenaltyBuffLevel()
	{
		if (getDeathPenaltyBuffLevel() >= 15)
		{
			return;
		}
		
		if (getDeathPenaltyBuffLevel() != 0)
		{
			L2Skill skill = SkillTable.getInstance().getInfo(5076, getDeathPenaltyBuffLevel());
			
			if (skill != null)
			{
				removeSkill(skill, true);
				skill = null;
			}
		}
		
		_deathPenaltyBuffLevel++;
		
		addSkill(SkillTable.getInstance().getInfo(5076, getDeathPenaltyBuffLevel()), false);
		sendPacket(new EtcStatusUpdate(this));
		SystemMessage sm = new SystemMessage(SystemMessageId.DEATH_PENALTY_LEVEL_S1_ADDED);
		sm.addNumber(getDeathPenaltyBuffLevel());
		sendPacket(sm);
		sendSkillList();
	}
	
	/**
	 * Reduce death penalty buff level.
	 */
	public void reduceDeathPenaltyBuffLevel()
	{
		if (getDeathPenaltyBuffLevel() <= 0)
		{
			return;
		}
		
		L2Skill skill = SkillTable.getInstance().getInfo(5076, getDeathPenaltyBuffLevel());
		
		if (skill != null)
		{
			removeSkill(skill, true);
			skill = null;
			sendSkillList();
		}
		
		_deathPenaltyBuffLevel--;
		
		if (getDeathPenaltyBuffLevel() > 0)
		{
			addSkill(SkillTable.getInstance().getInfo(5076, getDeathPenaltyBuffLevel()), false);
			sendPacket(new EtcStatusUpdate(this));
			SystemMessage sm = new SystemMessage(SystemMessageId.DEATH_PENALTY_LEVEL_S1_ADDED);
			sm.addNumber(getDeathPenaltyBuffLevel());
			sendPacket(sm);
			sm = null;
			sendSkillList();
		}
		else
		{
			sendPacket(new EtcStatusUpdate(this));
			sendPacket(new SystemMessage(SystemMessageId.DEATH_PENALTY_LIFTED));
		}
	}
	
	/**
	 * restore all Custom Data hero/noble/donator.
	 */
	public void restoreCustomStatus()
	{
		if (Config.DEVELOPER)
		{
			LOG.info("Restoring character status " + getName() + " from database...");
		}
		
		int hero = 0;
		int noble = 0;
		int donator = 0;
		long hero_end = 0;
		
		Connection con = null;
		
		try
		{
			
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement(STATUS_DATA_GET);
			statement.setInt(1, getObjectId());
			
			ResultSet rset = statement.executeQuery();
			
			while (rset.next())
			{
				hero = rset.getInt("hero");
				noble = rset.getInt("noble");
				donator = rset.getInt("donator");
				hero_end = rset.getLong("hero_end_date");
			}
			rset.close();
			statement.close();
		}
		catch (Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				LOG.error("Error: could not restore char custom data info:");
				e.printStackTrace();
			}
		}
		finally
		{
			CloseUtil.close(con);
		}
		
		if (hero > 0 && (hero_end == 0 || hero_end > System.currentTimeMillis()))
		{
			setHero(true);
		}
		else
		{
			// delete wings of destiny
			destroyItem("HeroEnd", 6842, 1, null, false);
		}
		
		if (noble > 0)
		{
			setNoble(true);
		}
		
		if (donator > 0)
		{
			setDonator(true);
		}
	}
	
	/**
	 * Restore death penalty buff level.
	 */
	public void restoreDeathPenaltyBuffLevel()
	{
		L2Skill skill = SkillTable.getInstance().getInfo(5076, getDeathPenaltyBuffLevel());
		
		if (skill != null)
		{
			removeSkill(skill, true);
			skill = null;
		}
		
		if (getDeathPenaltyBuffLevel() > 0)
		{
			addSkill(SkillTable.getInstance().getInfo(5076, getDeathPenaltyBuffLevel()), false);
			SystemMessage sm = new SystemMessage(SystemMessageId.DEATH_PENALTY_LEVEL_S1_ADDED);
			sm.addNumber(getDeathPenaltyBuffLevel());
			sendPacket(sm);
			sm = null;
		}
		sendPacket(new EtcStatusUpdate(this));
	}
	
	@Override
	public final void sendDamageMessage(L2Character target, int damage, boolean mcrit, boolean pcrit, boolean miss)
	{
		// Check if hit is critical
		if (pcrit)
		{
			if (getScreentxt())
			{
				sendPacket(new SystemMessage(SystemMessageId.CRITICAL_HIT));
			}
			else
			{
				sendPacket(new SystemMessage(SystemMessageId.CRITICAL_HIT2));
			}
		}
		
		if (mcrit)
		{
			if (getScreentxt())
			{
				sendPacket(new SystemMessage(SystemMessageId.CRITICAL_HIT_MAGIC));
			}
			else
			{
				sendPacket(new SystemMessage(SystemMessageId.CRITICAL_HIT_MAGIC2));
			}
			
		}
		
		if (!miss)
		{
			if (target.isInvul())
			{
				if (target.isParalyzed())
				{
					sendPacket(SystemMessageId.OPPONENT_PETRIFIED);
				}
				else
				{
					sendPacket(SystemMessageId.ATTACK_WAS_BLOCKED);
				}
			}
			else
			{
				SystemMessage sm;
				if (getScreentxt())
				{
					sm = new SystemMessage(SystemMessageId.YOU_DID_S1_DMG);
				}
				else
				{
					sm = new SystemMessage(SystemMessageId.YOU_DID_S1_DMG2);
				}
				sm.addNumber(damage);
				sendPacket(sm);
			}
		}
		
		if (isInOlympiadMode() && target instanceof L2PcInstance && ((L2PcInstance) target).isInOlympiadMode() && ((L2PcInstance) target).getOlympiadGameId() == getOlympiadGameId())
		{
			OlympiadGameManager.getInstance().notifyCompetitorDamage(this, damage);
		}
	}
	
	/**
	 * Update title.
	 */
	public void updateTitle()
	{
		setTitlePvpPk("PvP: [" + getPvpKills() + "] PK: [" + getPkKills() + "]");
		broadcastTitleInfo();
	}
	
	public boolean isRequestExpired()
	{
		return !(_requestExpireTime > GameTimeController.getInstance().getGameTicks());
	}
	
	/** The _gm status. */
	boolean _gmStatus = true;
	
	/**
	 * Sets the gm status active.
	 * @param state the new gm status active
	 */
	public void setGmStatusActive(boolean state)
	{
		_gmStatus = state;
	}
	
	/**
	 * Checks for gm status active.
	 * @return true, if successful
	 */
	public boolean hasGmStatusActive()
	{
		return _gmStatus;
	}
	
	/** The _saymode. */
	public L2Object _saymode = null;
	
	/**
	 * Gets the say mode.
	 * @return the say mode
	 */
	public L2Object getSayMode()
	{
		return _saymode;
	}
	
	/**
	 * Sets the say mode.
	 * @param say the new say mode
	 */
	public void setSayMode(L2Object say)
	{
		_saymode = say;
	}
	
	/**
	 * Save event stats.
	 */
	public void saveEventStats()
	{
		_originalNameColor = getAppearance().getNameColor();
		_originalKarma = getKarma();
		_eventKills = 0;
	}
	
	/**
	 * Restore event stats.
	 */
	public void restoreEventStats()
	{
		getAppearance().setNameColor(_originalNameColor);
		setKarma(_originalKarma);
		_eventKills = 0;
	}
	
	/**
	 * Gets the current skill world position.
	 * @return the current skill world position
	 */
	public Location getCurrentSkillWorldPosition()
	{
		return _currentSkillWorldPosition;
	}
	
	/**
	 * Sets the current skill world position.
	 * @param worldPosition the new current skill world position
	 */
	public void setCurrentSkillWorldPosition(final Location worldPosition)
	{
		_currentSkillWorldPosition = worldPosition;
	}
	
	@Override
	public void enableSkill(L2Skill skill)
	{
		super.enableSkill(skill);
		removeTimeStamp(skill);
	}
	
	public boolean isCursedWeaponEquipped()
	{
		return _cursedWeaponEquipedId != 0;
	}
	
	/**
	 * Dismount.
	 * @return true, if successful
	 */
	public boolean dismount()
	{
		if (setMountType(0))
		{
			if (isFlying())
			{
				removeSkill(SkillTable.getInstance().getInfo(4289, 1));
			}
			
			Ride dismount = new Ride(getObjectId(), Ride.ACTION_DISMOUNT, 0);
			broadcastPacket(dismount);
			dismount = null;
			setMountObjectID(0);
			
			// Notify self and others about speed change
			broadcastUserInfo();
			return true;
		}
		return false;
	}
	
	/**
	 * Gets the pc bang score.
	 * @return the pc bang score
	 */
	public int getPcBangScore()
	{
		return pcBangPoint;
	}
	
	/**
	 * Reduce pc bang score.
	 * @param to the to
	 */
	public void reducePcBangScore(int to)
	{
		pcBangPoint -= to;
		updatePcBangWnd(to, false, false);
	}
	
	/**
	 * Adds the pc bang score.
	 * @param to the to
	 */
	public void addPcBangScore(int to)
	{
		pcBangPoint += to;
	}
	
	/**
	 * Update pc bang wnd.
	 * @param score the score
	 * @param add the add
	 * @param duble the duble
	 */
	public void updatePcBangWnd(int score, boolean add, boolean duble)
	{
		ExPCCafePointInfo wnd = new ExPCCafePointInfo(this, score, add, 24, duble);
		sendPacket(wnd);
	}
	
	/**
	 * Show pc bang window.
	 */
	public void showPcBangWindow()
	{
		ExPCCafePointInfo wnd = new ExPCCafePointInfo(this, 0, false, 24, false);
		sendPacket(wnd);
	}
	
	/**
	 * String to hex.
	 * @param color the color
	 * @return the string
	 */
	public String StringToHex(String color)
	{
		switch (color.length())
		{
			case 1:
				color = new StringBuilder().append("00000").append(color).toString();
				break;
			
			case 2:
				color = new StringBuilder().append("0000").append(color).toString();
				break;
			
			case 3:
				color = new StringBuilder().append("000").append(color).toString();
				break;
			
			case 4:
				color = new StringBuilder().append("00").append(color).toString();
				break;
			
			case 5:
				color = new StringBuilder().append('0').append(color).toString();
				break;
		}
		return color;
	}
	
	public String StringToHexForVote(String color)
	{
		switch (color.length())
		{
			case 1:
				color = new StringBuilder().append(color).append("00000").toString();
				break;
			
			case 2:
				color = new StringBuilder().append(color).append("0000").toString();
				break;
			
			case 3:
				color = new StringBuilder().append(color).append("000").toString();
				break;
			
			case 4:
				color = new StringBuilder().append(color).append("00").toString();
				break;
			
			case 5:
				color = new StringBuilder().append(color).append('0').toString();
				break;
		}
		return color;
	}
	
	/**
	 * Checks if is offline.
	 * @return true, if is offline
	 */
	public boolean isInOfflineMode()
	{
		return _isInOfflineMode;
	}
	
	/**
	 * Sets the offline.
	 * @param set the new offline
	 */
	public void setOfflineMode(final boolean set)
	{
		_isInOfflineMode = set;
	}
	
	/**
	 * Show teleport html.
	 */
	public void showTeleportHtml()
	{
		TextBuilder text = new TextBuilder();
		text.append("<html>");
		text.append("<body>");
		text.append("<title></title>");
		text.append("<center>");
		text.append("<img src=\"L2UI_CH3.herotower_deco\" width=256 height=32>");
		text.append("<br><br>");
		text.append("<table width=\"85%\"><tr><td>Your party leader, " + getParty().getLeader().getName() + ", requested a group teleport to raidboss. You have 30 seconds from this popup to teleport, or the teleport windows will close</td></tr></table><br>");
		text.append("<a action=\"bypass -h rbAnswear\">Port with my party</a><br>");
		text.append("<a action=\"bypass -h rbAnswearDenied\">Don't port</a><br1>");
		text.append("<center><img src=\"L2UI.SquareGray\" width=\"280\" height=\"1\">");
		text.append("<font color=\"999999\">Gates of Fire</font></center>");
		text.append("</body>");
		text.append("</html>");
		
		NpcHtmlMessage html = new NpcHtmlMessage(1);
		html.setHtml(text.toString());
		sendPacket(html);
	}
	
	/** The Dropzor. */
	String Dropzor = "Coin of Luck";
	
	/**
	 * Show raidboss info level40.
	 */
	public void showRaidbossInfoLevel40()
	{
		TextBuilder text = new TextBuilder();
		text.append("<html>");
		text.append("<body>");
		text.append("<title>Raidboss Level (40-45)</title>");
		text.append("<center>");
		text.append("<img src=\"L2UI_CH3.herotower_deco\" width=256 height=32>");
		text.append("</center>");
		text.append("<br><br>");
		text.append("<table width=\"85%\"><tr><td>Drop: " + Dropzor + "</td></tr></table>");
		text.append("Leto Chief Talkin (40)<br1>");
		text.append("Water Spirit Lian (40) <br1>");
		text.append("Shaman King Selu (40) <br1>");
		text.append("Gwindorr (40) <br1>");
		text.append("Icarus Sample 1 (40) <br1>");
		text.append("Fafurion's Page Sika (40) <br1>");
		text.append("Nakondas (40) <br1>");
		text.append("Road Scavenger Leader (40)<br1>");
		text.append("Wizard of Storm Teruk (40) <br1>");
		text.append("Water Couatle Ateka (40)<br1>");
		text.append("Crazy Mechanic Golem (43) <br1>");
		text.append("Earth Protector Panathen (43) <br1>");
		text.append("Thief Kelbar (44) <br1>");
		text.append("Timak Orc Chief Ranger (44) <br1>");
		text.append("Rotten Tree Repiro (44) <br1>");
		text.append("Dread Avenger Kraven (44) <br1>");
		text.append("Biconne of Blue Sky (45)<br1>");
		text.append("Evil Spirit Cyrion (45) <br1>");
		text.append("Iron Giant Totem (45) <br1>");
		text.append("Timak Orc Gosmos (45) <br1>");
		text.append("Shacram (45) <br1>");
		text.append("Fafurion's Henchman Istary (45) <br1>");
		text.append("<center><img src=\"L2UI.SquareGray\" width=\"280\" height=\"1\">");
		text.append("<font color=\"999999\">Gates of Fire</font></center>");
		text.append("</body>");
		text.append("</html>");
		NpcHtmlMessage html = new NpcHtmlMessage(1);
		html.setHtml(text.toString());
		sendPacket(html);
	}
	
	/**
	 * Show raidboss info level45.
	 */
	public void showRaidbossInfoLevel45()
	{
		TextBuilder text = new TextBuilder();
		text.append("<html>");
		text.append("<body>");
		text.append("<title>Raidboss Level (45-50)</title>");
		text.append("<center>");
		text.append("<img src=\"L2UI_CH3.herotower_deco\" width=256 height=32>");
		text.append("</center>");
		text.append("<br><br>");
		text.append("<table width=\"85%\"><tr><td>Drop: " + Dropzor + "</td></tr></table>");
		text.append("Necrosentinel Royal Guard (47) <br1>");
		text.append("Barion (47) <br1>");
		text.append("Orfen's Handmaiden (48) <br1>");
		text.append("King Tarlk (48) <br1>");
		text.append("Katu Van Leader Atui (49) <br1>");
		text.append("Mirror of Oblivion (49) <br1>");
		text.append("Karte (49) <br1>");
		text.append("Ghost of Peasant Leader (50) <br1>");
		text.append("Cursed Clara (50) <br1>");
		text.append("Carnage Lord Gato (50) <br1>");
		text.append("Fafurion's Henchman Istary (45) <br1>");
		text.append("<center><img src=\"L2UI.SquareGray\" width=\"280\" height=\"1\">");
		text.append("<font color=\"999999\">Gates of Fire</font></center>");
		text.append("</body>");
		text.append("</html>");
		NpcHtmlMessage html = new NpcHtmlMessage(1);
		html.setHtml(text.toString());
		sendPacket(html);
	}
	
	/**
	 * Show raidboss info level50.
	 */
	public void showRaidbossInfoLevel50()
	{
		TextBuilder text = new TextBuilder();
		text.append("<html>");
		text.append("<body>");
		text.append("<title>Raidboss Level (50-55)</title>");
		text.append("<center>");
		text.append("<img src=\"L2UI_CH3.herotower_deco\" width=256 height=32>");
		text.append("</center>");
		text.append("<br><br>");
		text.append("<table width=\"85%\"><tr><td>Drop: " + Dropzor + "</td></tr></table>");
		text.append("Verfa (51) <br1>");
		text.append("Deadman Ereve (51) <br1>");
		text.append("Captain of Red Flag Shaka (52) <br1>");
		text.append("Grave Robber Kim (52) <br1>");
		text.append("Paniel the Unicorn (54) <br1>");
		text.append("Bandit Leader Barda (55) <br1>");
		text.append("Eva's Spirit Niniel (55) <br1>");
		text.append("Beleth's Seer Sephia (55) <br1>");
		text.append("Pagan Watcher Cerberon (55) <br1>");
		text.append("Shaman King Selu (55) <br1>");
		text.append("Black Lily (55) <br1>");
		text.append("Ghost Knight Kabed (55) <br1>");
		text.append("Sorcerer Isirr (55) <br1>");
		text.append("Furious Thieles (55) <br1>");
		text.append("Enchanted Forest Watcher Ruell (55) <br1>");
		text.append("<center><img src=\"L2UI.SquareGray\" width=\"280\" height=\"1\">");
		text.append("<font color=\"999999\">Gates of Fire</font></center>");
		text.append("</body>");
		text.append("</html>");
		NpcHtmlMessage html = new NpcHtmlMessage(1);
		html.setHtml(text.toString());
		sendPacket(html);
	}
	
	/**
	 * Show raidboss info level55.
	 */
	public void showRaidbossInfoLevel55()
	{
		TextBuilder text = new TextBuilder();
		text.append("<html>");
		text.append("<body>");
		text.append("<title>Raidboss Level (55-60)</title>");
		text.append("<center>");
		text.append("<img src=\"L2UI_CH3.herotower_deco\" width=256 height=32>");
		text.append("</center>");
		text.append("<br><br>");
		text.append("<table width=\"85%\"><tr><td>Drop: " + Dropzor + "</td></tr></table>");
		text.append("Fairy Queen Timiniel (56) <br1>");
		text.append("Harit Guardian Garangky (56) <br1>");
		text.append("Refugee Hopeful Leo (56) <br1>");
		text.append("Timak Seer Ragoth (57) <br1>");
		text.append("Soulless Wild Boar (59) <br1>");
		text.append("Abyss Brukunt (59) <br1>");
		text.append("Giant Marpanak (60) <br1>");
		text.append("Ghost of the Well Lidia (60) <br1>");
		text.append("Guardian of the Statue of Giant Karum (60) <br1>");
		text.append("The 3rd Underwater Guardian (60) <br1>");
		text.append("Taik High Prefect Arak (60) <br1>");
		text.append("Ancient Weird Drake (60) <br1>");
		text.append("Lord Ishka (60) <br1>");
		text.append("<center><img src=\"L2UI.SquareGray\" width=\"280\" height=\"1\">");
		text.append("<font color=\"999999\">Gates of Fire</font></center>");
		text.append("</body>");
		text.append("</html>");
		NpcHtmlMessage html = new NpcHtmlMessage(1);
		html.setHtml(text.toString());
		sendPacket(html);
	}
	
	/**
	 * Show raidboss info level60.
	 */
	public void showRaidbossInfoLevel60()
	{
		TextBuilder text = new TextBuilder();
		text.append("<html>");
		text.append("<body>");
		text.append("<title>Raidboss Level (60-65)</title>");
		text.append("<center>");
		text.append("<img src=\"L2UI_CH3.herotower_deco\" width=256 height=32>");
		text.append("</center>");
		text.append("<br><br>");
		text.append("<table width=\"85%\"><tr><td>Drop: " + Dropzor + "</td></tr></table>");
		text.append("Roaring Lord Kastor (62) <br1>");
		text.append("Gorgolos (64) <br1>");
		text.append("Hekaton Prime (65) <br1>");
		text.append("Gargoyle Lord Tiphon (65) <br1>");
		text.append("Fierce Tiger King Angel (65) <br1>");
		text.append("Enmity Ghost Ramdal (65) <br1>");
		text.append("Rahha (65) <br1>");
		text.append("Shilen's Priest Hisilrome (65) <br1>");
		text.append("<center><img src=\"L2UI.SquareGray\" width=\"280\" height=\"1\">");
		text.append("<font color=\"999999\">Gates of Fire</font></center>");
		text.append("</body>");
		text.append("</html>");
		NpcHtmlMessage html = new NpcHtmlMessage(1);
		html.setHtml(text.toString());
		sendPacket(html);
	}
	
	/**
	 * Show raidboss info level65.
	 */
	public void showRaidbossInfoLevel65()
	{
		TextBuilder text = new TextBuilder();
		text.append("<html>");
		text.append("<body>");
		text.append("<title>Raidboss Level (65-70)</title>");
		text.append("<br><br>");
		text.append("<center>");
		text.append("<table width=\"85%\"><tr><td>Drop: " + Dropzor + "</td></tr></table>");
		text.append("</center>");
		text.append("Demon's Agent Falston (66) <br1>");
		text.append("Last Titan utenus (66) <br1>");
		text.append("Kernon's Faithful Servant Kelone (67) <br1>");
		text.append("Spirit of Andras, the Betrayer (69) <br1>");
		text.append("Bloody Priest Rudelto (69) <br1>");
		text.append("Shilen's Messenger Cabrio (70) <br1>");
		text.append("Anakim's Nemesis Zakaron (70) <br1>");
		text.append("Flame of Splendor Barakiel (70) <br1>");
		text.append("Roaring Skylancer (70) <br1>");
		text.append("Beast Lord Behemoth (70) <br1>");
		text.append("Palibati Queen Themis (70) <br1>");
		text.append("Fafurion''s Herald Lokness (70) <br1>");
		text.append("Meanas Anor (70) <br1>");
		text.append("Korim (70) <br1>");
		text.append("<center><img src=\"L2UI.SquareGray\" width=\"280\" height=\"1\">");
		text.append("<font color=\"999999\">Gates of Fire</font></center>");
		text.append("</body>");
		text.append("</html>");
		NpcHtmlMessage html = new NpcHtmlMessage(1);
		html.setHtml(text.toString());
		sendPacket(html);
	}
	
	/**
	 * Show raidboss info level70.
	 */
	public void showRaidbossInfoLevel70()
	{
		TextBuilder text = new TextBuilder();
		text.append("<html>");
		text.append("<body>");
		text.append("<title>Raidboss Level (70-75)</title>");
		text.append("<center>");
		text.append("<img src=\"L2UI_CH3.herotower_deco\" width=256 height=32>");
		text.append("</center>");
		text.append("<br><br>");
		text.append("<table width=\"85%\"><tr><td>Drop: " + Dropzor + "</td></tr></table>");
		text.append("Immortal Savior Mardil (71) <br1>");
		text.append("Vanor Chief Kandra (72) <br1>");
		text.append("Water Dragon Seer Sheshark (72) <br1>");
		text.append("Doom Blade Tanatos (72) <br1>");
		text.append("Death Lord Hallate (73) <br1>");
		text.append("Plague Golem (73) <br1>");
		text.append("Icicle Emperor Bumbalump (74) <br1>");
		text.append("Antharas Priest Cloe (74) <br1>");
		text.append("Krokian Padisha Sobekk (74) <br1>");
		text.append("<center><img src=\"L2UI.SquareGray\" width=\"280\" height=\"1\">");
		text.append("<font color=\"999999\">Gates of Fire</font></center>");
		text.append("</body>");
		text.append("</html>");
		NpcHtmlMessage html = new NpcHtmlMessage(1);
		html.setHtml(text.toString());
		sendPacket(html);
	}
	
	/** The isintwtown. */
	private boolean isintwtown = false;
	private boolean isenteringtoworld = false;
	private boolean _tempAccessToMultisell = false;
	
	/**
	 * Checks if is inside tw town.
	 * @return true, if is inside tw town
	 */
	public boolean isInsideTWTown()
	{
		if (isintwtown)
		{
			return true;
		}
		
		return false;
	}
	
	/**
	 * Sets the inside tw town.
	 * @param b the new inside tw town
	 */
	public void setInsideTWTown(boolean b)
	{
		isintwtown = true;
	}
	
	public boolean isEnteringToWorld()
	{
		if (isenteringtoworld)
		{
			return true;
		}
		
		return false;
	}
	
	public void SetIsEnteringToWorld(boolean b)
	{
		isenteringtoworld = b;
	}
	
	public void setTempAccessBuy(boolean b)
	{
		_tempAccessToMultisell = b;
	}
	
	public boolean hasTempAccessBuy()
	{
		if (_tempAccessToMultisell)
		{
			return true;
		}
		
		return false;
	}
	
	/**
	 * check if local player can make multibox and also refresh local boxes instances number.
	 * @return true, if successful
	 */
	public boolean checkMultiBox()
	{
		
		boolean output = true;
		
		int boxes_number = 0;
		List<String> active_boxes = new ArrayList<>();
		
		if (getClient() != null && getClient().getConnection() != null && !getClient().getConnection().isClosed() && getClient().getConnection().getInetAddress() != null)
		{
			
			String thisip = getClient().getConnection().getInetAddress().getHostAddress();
			final Collection<L2PcInstance> allPlayers = L2World.getInstance().getAllPlayers().values();
			for (L2PcInstance player : allPlayers)
			{
				if (player != null)
				{
					if (player.isOnline() == 1 && player.getClient() != null && player.getClient().getConnection() != null && !player.getClient().getConnection().isClosed() && player.getClient().getConnection().getInetAddress() != null && !player.getName().equals(this.getName()))
					{
						
						String ip = player.getClient().getConnection().getInetAddress().getHostAddress();
						if (thisip.equals(ip) && this != player)
						{
							if (!Config.ALLOW_DUALBOX)
							{
								output = false;
								break;
							}
							if (boxes_number + 1 > Config.ALLOWED_BOXES)
							{
								output = false;
								break;
							}
							boxes_number++;
							active_boxes.add(player.getName());
						}
					}
				}
			}
		}
		
		if (output)
		{
			_active_boxes = boxes_number + 1; // current number of boxes+this one
			if (!active_boxes.contains(this.getName()))
			{
				active_boxes.add(this.getName());
				
				this.active_boxes_characters = active_boxes;
			}
			refreshOtherBoxes();
		}
		return output;
	}
	
	/**
	 * increase active boxes number for local player and other boxer for same ip.
	 */
	public void refreshOtherBoxes()
	{
		if (getClient() != null && getClient().getConnection() != null && !getClient().getConnection().isClosed() && getClient().getConnection().getInetAddress() != null)
		{
			String thisip = getClient().getConnection().getInetAddress().getHostAddress();
			final Collection<L2PcInstance> allPlayers = L2World.getInstance().getAllPlayers().values();
			L2PcInstance[] players = allPlayers.toArray(new L2PcInstance[allPlayers.size()]);
			
			for (L2PcInstance player : players)
			{
				if (player != null && !player.isInOfflineMode())
				{
					if (player.getClient() != null && player.getClient().getConnection() != null && !player.getClient().getConnection().isClosed() && !player.getName().equals(this.getName()))
					{
						
						String ip = player.getClient().getConnection().getInetAddress().getHostAddress();
						if (thisip.equals(ip) && this != player)
						{
							player._active_boxes = _active_boxes;
							player.active_boxes_characters = active_boxes_characters;
						}
					}
				}
			}
		}
		
	}
	
	/**
	 * descrease active boxes number for local player and other boxer for same ip.
	 */
	public void decreaseBoxes()
	{
		_active_boxes = _active_boxes - 1;
		active_boxes_characters.remove(this.getName());
		refreshOtherBoxes();
	}
	
	/**
	 * Aio System Start.
	 * @return true, if is aio
	 */
	public boolean isAio()
	{
		return _isAio;
	}
	
	/**
	 * Sets the aio.
	 * @param val the new aio
	 */
	public void setAio(boolean val)
	{
		_isAio = val;
		
	}
	
	/**
	 * Sets the aio end time.
	 * @param val the new aio end time
	 */
	public void setAioEndTime(long val)
	{
		_aio_endTime = val;
	}
	
	/**
	 * Sets the end time.
	 * @param process the process
	 * @param val the val
	 */
	public void setEndTime(String process, int val)
	{
		if (val > 0)
		{
			long end_day;
			Calendar calendar = Calendar.getInstance();
			if (val >= 30)
			{
				while (val >= 30)
				{
					if (calendar.get(Calendar.MONTH) == 11)
					{
						calendar.roll(Calendar.YEAR, true);
					}
					calendar.roll(Calendar.MONTH, true);
					val -= 30;
				}
			}
			if (val < 30 && val > 0)
			{
				while (val > 0)
				{
					if (calendar.get(Calendar.DATE) == 28 && calendar.get(Calendar.MONTH) == 1)
					{
						calendar.roll(Calendar.MONTH, true);
					}
					if (calendar.get(Calendar.DATE) == 30)
					{
						if (calendar.get(Calendar.MONTH) == 11)
						{
							calendar.roll(Calendar.YEAR, true);
						}
						calendar.roll(Calendar.MONTH, true);
						
					}
					calendar.roll(Calendar.DATE, true);
					val--;
				}
			}
			
			end_day = calendar.getTimeInMillis();
			if (process.equals("aio"))
			{
				_aio_endTime = end_day;
			}
			else
			{
				LOG.info("process " + process + "no Known while try set end date");
				return;
			}
			Date dt = new Date(end_day);
			LOG.info("" + process + " end time for player " + getName() + " is " + dt);
		}
		else
		{
			if (process.equals("aio"))
			{
				_aio_endTime = 0;
			}
			else
			{
				LOG.info("process " + process + "no Known while try set end date");
				return;
			}
		}
	}
	
	/**
	 * Gets the aio end time.
	 * @return the aio end time
	 */
	public long getAioEndTime()
	{
		return _aio_endTime;
	}
	
	/**
	 * Gets the offline start time.
	 * @return the offline start time
	 */
	public long getOfflineStartTime()
	{
		return _offlineShopStart;
	}
	
	/**
	 * Sets the offline start time.
	 * @param time the new offline start time
	 */
	public void setOfflineStartTime(long time)
	{
		_offlineShopStart = time;
	}
	
	/**
	 * Return true if character falling now On the start of fall return false for correct coord sync !.
	 * @param z the z
	 * @return true, if is falling
	 */
	public final boolean isFalling(int z)
	{
		if (isDead() || isFlying() || isInFunEvent() || isInsideZone(ZoneId.ZONE_WATER))
		{
			return false;
		}
		
		if (System.currentTimeMillis() < _fallingTimestamp)
		{
			return true;
		}
		
		final int deltaZ = getZ() - z;
		if (deltaZ <= getBaseTemplate().getFallHeight())
		{
			return false;
		}
		
		final int damage = (int) Formulas.calcFallDam(this, deltaZ);
		if (damage > 0 && !isInvul())
		{
			reduceCurrentHp(Math.min(damage, getCurrentHp() - 1), null, false);
			sendPacket(new SystemMessage(SystemMessageId.FALL_DAMAGE_S1).addNumber(damage));
		}
		
		if (getTeleport())
		{
			if ((!isInsideZone(ZoneId.ZONE_BOSS)) && (!isInsideZone(ZoneId.ZONE_PEACE)))
			{
				int distance = 300;
				double angle = Util.convertHeadingToDegree(getHeading());
				double radian = Math.toRadians(angle);
				double sin = Math.sin(radian);
				double cos = Math.cos(radian);
				int x = (int) (getX() + (cos * distance));
				int y = (int) (getY() + (sin * distance));
				int z1 = getZ();
				teleToLocation(x, y, z1, getHeading(), false, true);
			}
		}
		
		setFalling();
		
		return false;
	}
	
	/**
	 * Set falling timestamp.
	 */
	public final void setFalling()
	{
		_fallingTimestamp = System.currentTimeMillis() + FALLING_VALIDATION_DELAY;
	}
	
	/** Previous coordinate sent to party in ValidatePosition *. */
	private Location _lastPartyPosition = new Location(0, 0, 0);
	
	/**
	 * Sets the last party position.
	 * @param x the x
	 * @param y the y
	 * @param z the z
	 */
	public void setLastPartyPosition(int x, int y, int z)
	{
		_lastPartyPosition.setXYZ(x, y, z);
	}
	
	/**
	 * Gets the last party position distance.
	 * @param x the x
	 * @param y the y
	 * @param z the z
	 * @return the last party position distance
	 */
	public int getLastPartyPositionDistance(int x, int y, int z)
	{
		double dx = (x - _lastPartyPosition.getX());
		double dy = (y - _lastPartyPosition.getY());
		double dz = (z - _lastPartyPosition.getZ());
		
		return (int) Math.sqrt(dx * dx + dy * dy + dz * dz);
	}
	
	public boolean isLocked()
	{
		return _isLocked;
	}
	
	public void setLocked(boolean a)
	{
		_isLocked = a;
	}
	
	public boolean isStored()
	{
		return _isStored;
	}
	
	public void setStored(boolean a)
	{
		_isStored = a;
	}
	
	private PunishLevel _punishLevel = PunishLevel.NONE;
	private long _punishTimer = 0;
	private ScheduledFuture<?> _punishTask;
	private Future<?> _waiterTask;
	
	public enum PunishLevel
	{
		NONE(0, ""),
		CHAT(1, "Chat Ban"),
		JAIL(2, "Jail"),
		CHAR(3, "Character Ban"),
		ACC(4, "Account Ban");
		
		private final int punValue;
		
		private final String punString;
		
		PunishLevel(int value, String string)
		{
			punValue = value;
			punString = string;
		}
		
		public int value()
		{
			return punValue;
		}
		
		public String string()
		{
			return punString;
		}
	}
	
	// open/close gates
	private GatesRequest _gatesRequest = new GatesRequest();
	
	public class GatesRequest
	{
		private L2DoorInstance _target = null;
		
		public void setTarget(L2DoorInstance door)
		{
			_target = door;
		}
		
		public L2DoorInstance getDoor()
		{
			return _target;
		}
	}
	
	public void gatesRequest(L2DoorInstance door)
	{
		_gatesRequest.setTarget(door);
	}
	
	public void gatesAnswer(int answer, int type)
	{
		if (_gatesRequest.getDoor() == null)
		{
			return;
		}
		
		if (answer == 1 && getTarget() == _gatesRequest.getDoor() && type == 1)
		{
			_gatesRequest.getDoor().openMe();
		}
		else if (answer == 1 && getTarget() == _gatesRequest.getDoor() && type == 0)
		{
			_gatesRequest.getDoor().closeMe();
		}
		
		_gatesRequest.setTarget(null);
	}
	
	public PunishLevel getPunishLevel()
	{
		return _punishLevel;
	}
	
	/**
	 * Checks if is in jail.
	 * @return True if player is jailed
	 */
	public boolean isInJail()
	{
		return _punishLevel == PunishLevel.JAIL;
	}
	
	/**
	 * Checks if is chat banned.
	 * @return True if player is chat banned
	 */
	public boolean isChatBanned()
	{
		return _punishLevel == PunishLevel.CHAT;
	}
	
	/**
	 * Sets the punish level.
	 * @param state the new punish level
	 */
	public void setPunishLevel(int state)
	{
		switch (state)
		{
			case 0:
			{
				_punishLevel = PunishLevel.NONE;
				break;
			}
			case 1:
			{
				_punishLevel = PunishLevel.CHAT;
				break;
			}
			case 2:
			{
				_punishLevel = PunishLevel.JAIL;
				break;
			}
			case 3:
			{
				_punishLevel = PunishLevel.CHAR;
				break;
			}
			case 4:
			{
				_punishLevel = PunishLevel.ACC;
				break;
			}
		}
	}
	
	/**
	 * Sets the punish level.
	 * @param state the state
	 * @param delayInMinutes the delay in minutes
	 */
	public void setPunishLevel(PunishLevel state, int delayInMinutes)
	{
		long delayInMilliseconds = delayInMinutes * 60000L;
		setPunishLevel(state, delayInMilliseconds);
	}
	
	/**
	 * Sets punish level for player based on delay.
	 * @param state the state
	 * @param delayInMilliseconds 0 - Indefinite
	 */
	public void setPunishLevel(PunishLevel state, long delayInMilliseconds)
	{
		switch (state)
		{
			case NONE: // Remove Punishments
			{
				switch (_punishLevel)
				{
					case CHAT:
					{
						_punishLevel = state;
						stopPunishTask(true);
						sendPacket(new EtcStatusUpdate(this));
						sendMessage("Chatting is now available.");
						sendPacket(new PlaySound("systemmsg_e.345"));
						delayInMilliseconds = 0;
						break;
					}
					case JAIL:
					{
						_punishLevel = state;
						// Open a Html message to inform the player
						NpcHtmlMessage htmlMsg = new NpcHtmlMessage(0);
						String jailInfos = HtmCache.getInstance().getHtm("data/html/jail_out.htm");
						
						if (jailInfos != null)
						{
							htmlMsg.setHtml(jailInfos);
						}
						else
						{
							htmlMsg.setHtml("<html><body>You are free for now, respect server rules!</body></html>");
						}
						
						sendPacket(htmlMsg);
						
						delayInMilliseconds = 0;
						
						stopPunishTask(true);
						teleToLocation(17836, 170178, -3507, true); // Floran
						break;
					}
					default:
						break;
				}
				break;
			}
			case CHAT: // Chat Ban
			{
				// not allow player to escape jail using chat ban
				if (_punishLevel == PunishLevel.JAIL)
				{
					break;
				}
				
				_punishLevel = state;
				_punishTimer = 0;
				
				sendPacket(new EtcStatusUpdate(this));
				// Remove the task if any
				stopPunishTask(false);
				
				if (delayInMilliseconds > 0)
				{
					_punishTimer = delayInMilliseconds;
					
					// start the countdown
					int minutes = (int) (delayInMilliseconds / 60000);
					_punishTask = ThreadPoolManager.getInstance().scheduleGeneral(new PunishTask(this), _punishTimer);
					sendMessage("You are chat banned for " + minutes + " minutes.");
				}
				else
				{
					sendMessage("You have been chat banned");
				}
				break;
				
			}
			case JAIL: // Jail Player
			{
				_punishLevel = state;
				_punishTimer = 0;
				// Remove the task if any
				stopPunishTask(false);
				
				if (delayInMilliseconds > 0)
				{
					_punishTimer = delayInMilliseconds; // Delay in milliseconds
					_punishTask = ThreadPoolManager.getInstance().scheduleGeneral(new PunishTask(this), _punishTimer);
					sendMessage("You are in jail for " + delayInMilliseconds / 60000 + " minutes.");
				}
				
				if (_inEventCTF)
				{
					CTF.onDisconnect(this);
				}
				else if (_inEventDM)
				{
					DM.onDisconnect(this);
				}
				else if (_inEventTvT)
				{
					TvT.onDisconnect(this);
				}
				else if (_inEventVIP)
				{
					VIP.onDisconnect(this);
				}
				
				if (OlympiadManager.getInstance().isRegistered(this))
				{
					OlympiadManager.getInstance().removeDisconnectedCompetitor(this);
				}
				
				// Open a Html message to inform the player
				NpcHtmlMessage htmlMsg = new NpcHtmlMessage(0);
				String jailInfos = HtmCache.getInstance().getHtm("data/html/jail_in.htm");
				
				if (jailInfos != null)
				{
					htmlMsg.setHtml(jailInfos);
				}
				else
				{
					htmlMsg.setHtml("<html><body>You have been put in jail by an admin.</body></html>");
				}
				
				sendPacket(htmlMsg);
				setInstanceId(0);
				setIsIn7sDungeon(false);
				teleToLocation(-114356, -249645, -2984, false); // Jail
				break;
			}
			case CHAR: // Ban Character
			{
				setAccessLevel(-100);
				logout();
				break;
			}
			case ACC: // Ban Account
			{
				setAccountAccesslevel(-100);
				logout();
				break;
			}
			default:
			{
				_punishLevel = state;
				break;
			}
		}
		
		// store in database
		storeCharBase();
		
		if (delayInMilliseconds > 0)
		{
			punishmentWaiter();
		}
	}
	
	public long getPunishTimer()
	{
		return _punishTimer;
	}
	
	public void setPunishTimer(long time)
	{
		_punishTimer = time;
	}
	
	protected class startWaiter implements Runnable
	{
		public startWaiter()
		{
		}
		
		@Override
		public void run()
		{
			waiterTime();
		}
	}
	
	private void punishmentWaiter()
	{
		if (_waiterTask == null)
		{
			_waiterTask = ThreadPoolManager.getInstance().scheduleAiAtFixedRate(new startWaiter(), 0, 1000);
		}
	}
	
	public void waiterTime()
	{
		int punishmentSeconds = 0;
		if (_punishTask != null)
		{
			punishmentSeconds = (int) (_punishTask.getDelay(TimeUnit.MILLISECONDS) / 1000);
		}
		
		if (_waiterTask != null && punishmentSeconds == 0)
		{
			_waiterTask.cancel(false);
			_waiterTask = null;
		}
		
		switch (punishmentSeconds)
		{
			case 7200:
				sendPacket(new ExShowScreenMessage("Punishment: " + getPunishLevel().string() + " time left: 2 hours", 2000, 0x03, false));
				sendMessage("Punishment: " + getPunishLevel().string() + " time left: 2 hours");
				break;
			case 3600: // 1 hour left
				sendPacket(new ExShowScreenMessage("Punishment: " + getPunishLevel().string() + " time left: 1 hour", 2000, 0x03, false));
				sendMessage("Punishment: " + getPunishLevel().string() + " time left: 1 hour");
				break;
			case 1800: // 30 minutes left
			case 900: // 15 minutes left
			case 600: // 10 minutes left
			case 300: // 5 minutes left
			case 240: // 4 minutes left
			case 180: // 3 minutes left
			case 120: // 2 minutes left
				sendPacket(new ExShowScreenMessage("Punishment: " + getPunishLevel().string() + " time left: " + punishmentSeconds / 60 + " minutes", 2000, 0x03, false));
				sendMessage("Punishment: " + getPunishLevel().string() + " time left: " + punishmentSeconds / 60 + " minutes");
				break;
			case 60: // 1 minute left
				sendPacket(new ExShowScreenMessage("Punishment: " + getPunishLevel().string() + " time left: " + punishmentSeconds / 60 + " minute", 2000, 0x03, false));
				sendMessage("Punishment: " + getPunishLevel().string() + " time left: " + punishmentSeconds / 60 + " minute");
				break;
			case 50: // 50 seconds left
			case 40: // 40 seconds left
			case 30: // 30 seconds left
			case 15: // 15 seconds left
			case 10: // 10 seconds left
			case 3: // 3 seconds left
			case 2: // 2 seconds left
				sendPacket(new ExShowScreenMessage("Punishment: " + getPunishLevel().string() + " time left: " + punishmentSeconds + " seconds", 2000, 0x03, false));
				sendMessage("Punishment: " + getPunishLevel().string() + " time left: " + punishmentSeconds + " seconds");
				break;
			case 1: // 1 seconds left
				sendPacket(new ExShowScreenMessage("Punishment: " + getPunishLevel().string() + " time left: " + punishmentSeconds + " second", 2000, 0x03, false));
				sendMessage("Punishment: " + getPunishLevel().string() + " time left: " + punishmentSeconds + " second");
				break;
		}
	}
	
	public void updatePunishState()
	{
		if (getPunishLevel() != PunishLevel.NONE)
		{
			if (_punishTimer > 0)
			{
				_punishTask = ThreadPoolManager.getInstance().scheduleGeneral(new PunishTask(this), _punishTimer);
				
				sendMessage("Punishment: " + getPunishLevel().string() + " time left: " + (_punishTimer / 60000) + " minutes.");
				
				punishmentWaiter();
			}
			
			if (getPunishLevel() == PunishLevel.JAIL)
			{
				// If player escaped, put him back in jail
				if (!isInsideZone(ZoneId.ZONE_JAIL))
				{
					teleToLocation(-114356, -249645, -2984, true);
				}
			}
		}
	}
	
	public void stopPunishTask(boolean save)
	{
		if (_punishTask != null)
		{
			if (save)
			{
				long delay = _punishTask.getDelay(TimeUnit.MILLISECONDS);
				
				if (delay < 0)
				{
					delay = 0;
				}
				
				setPunishTimer(delay);
			}
			_punishTask.cancel(false);
			
			// punishmentWaiter();
			// ThreadPoolManager.getInstance().removeGeneral((Runnable) _punishTask);
			_punishTask = null;
		}
	}
	
	/**
	 * The Class PunishTask.
	 */
	private class PunishTask implements Runnable
	{
		
		/** The _player. */
		L2PcInstance _player;
		
		/**
		 * Instantiates a new punish task.
		 * @param player the player
		 */
		protected PunishTask(L2PcInstance player)
		{
			_player = player;
		}
		
		@Override
		public void run()
		{
			_player.setPunishLevel(PunishLevel.NONE, 0);
		}
	}
	
	private HashMap<Integer, Long> confirmDlgRequests = new HashMap<>();
	
	public void addConfirmDlgRequestTime(int requestId, int time)
	
	{
		confirmDlgRequests.put(requestId, System.currentTimeMillis() + time + 2000);
	}
	
	public Long getConfirmDlgRequestTime(int requestId)
	{
		return confirmDlgRequests.get(requestId);
	}
	
	public void removeConfirmDlgRequestTime(int requestId)
	{
		confirmDlgRequests.remove(requestId);
	}
	
	/**
	 * Gets the flood protectors.
	 * @return the flood protectors
	 */
	public FloodProtectors getFloodProtectors()
	{
		return getClient().getFloodProtectors();
	}
	
	/**
	 * Test if player inventory is under 80% capaity.
	 * @param includeQuestInv
	 * @return true, if is inventory under80
	 */
	public boolean isInventoryUnder80(boolean includeQuestInv)
	{
		if (getInventory().getSize(false) <= (getInventoryLimit() * 0.8))
		{
			if (includeQuestInv)
			{
				if (getInventory().getSize(true) <= (getQuestInventoryLimit() * 0.8))
				{
					return true;
				}
			}
			else
			{
				return true;
			}
		}
		return false;
	}
	
	// Multisell
	/** The _current multi sell id. */
	private int _currentMultiSellId = -1;
	
	/**
	 * Gets the multi sell id.
	 * @return the multi sell id
	 */
	public final int getMultiSellId()
	{
		return _currentMultiSellId;
	}
	
	/**
	 * Sets the multi sell id.
	 * @param listid the new multi sell id
	 */
	public final void setMultiSellId(int listid)
	{
		_currentMultiSellId = listid;
	}
	
	/**
	 * Checks if is party waiting.
	 * @return true, if is party waiting
	 */
	public boolean isPartyWaiting()
	{
		return PartyMatchWaitingList.getInstance().getPlayers().contains(this);
	}
	
	// these values are only stored temporarily
	/** The _partyroom. */
	private int _partyroom = 0;
	
	/**
	 * Sets the party room.
	 * @param id the new party room
	 */
	public void setPartyRoom(int id)
	{
		_partyroom = id;
	}
	
	/**
	 * Gets the party room.
	 * @return the party room
	 */
	public int getPartyRoom()
	{
		return _partyroom;
	}
	
	/**
	 * Checks if is in party match room.
	 * @return true, if is in party match room
	 */
	public boolean isInPartyMatchRoom()
	{
		return _partyroom > 0;
	}
	
	/**
	 * Checks if is item equipped by item id.
	 * @param item_id the item_id
	 * @return true, if is item equipped by item id
	 */
	public boolean isItemEquippedByItemId(int item_id)
	{
		if (_inventory == null)
		{
			return false;
		}
		
		if (_inventory.getAllItemsByItemId(item_id) == null || _inventory.getAllItemsByItemId(item_id).length == 0)
		{
			return false;
		}
		
		return _inventory.checkIfEquipped(item_id);
	}
	
	/**
	 * Gets the _instance login time.
	 * @return the _instanceLoginTime
	 */
	public long get_instanceLoginTime()
	{
		return _instanceLoginTime;
	}
	
	/**
	 * Sets the sex db.
	 * @param player the player
	 * @param mode the mode
	 */
	public static void setSexDB(L2PcInstance player, int mode)
	{
		Connection con;
		if (player == null)
		{
			return;
		}
		con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("UPDATE characters SET sex=? WHERE obj_Id=?");
			statement.setInt(1, player.getAppearance().getSex() ? 1 : 0);
			statement.setInt(2, player.getObjectId());
			statement.execute();
			statement.close();
		}
		catch (Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				LOG.error("SetSex:  Could not store data:");
				e.printStackTrace();
			}
		}
		finally
		{
			CloseUtil.close(con);
		}
	}
	
	public void sendItems(boolean f)
	{
		sendPacket(new ItemList(this, f));
	}
	
	@Override
	public L2PcInstance getActingPlayer()
	{
		return this;
	}
	
	public long getLastAttackPacket()
	{
		return _lastAttackPacket;
	}
	
	public void setLastAttackPacket()
	{
		_lastAttackPacket = System.currentTimeMillis();
	}
	
	public void checkItemRestriction()
	{
		for (int i = 0; i < Inventory.PAPERDOLL_TOTALSLOTS; i++)
		{
			L2ItemInstance equippedItem = getInventory().getPaperdollItem(i);
			if (equippedItem != null && !equippedItem.checkOlympCondition())
			{
				if (equippedItem.isAugmented())
				{
					equippedItem.getAugmentation().removeBoni(this);
				}
				L2ItemInstance[] items = getInventory().unEquipItemInSlotAndRecord(i);
				if (equippedItem.isWear())
				{
					continue;
				}
				
				SystemMessage sm = null;
				if (equippedItem.getEnchantLevel() > 0)
				{
					sm = new SystemMessage(SystemMessageId.EQUIPMENT_S1_S2_REMOVED);
					sm.addNumber(equippedItem.getEnchantLevel());
					sm.addItemName(equippedItem.getItemId());
				}
				else
				{
					sm = new SystemMessage(SystemMessageId.S1_DISARMED);
					sm.addItemName(equippedItem.getItemId());
				}
				sendPacket(sm);
				InventoryUpdate iu = new InventoryUpdate();
				iu.addItems(Arrays.asList(items));
				sendPacket(iu);
				broadcastUserInfo();
			}
		}
	}
	
	public void setHero(boolean hero)
	{
		_hero = hero;
		if (_hero && _baseClass == _activeClass)
		{
			giveHeroSkills();
		}
		else if (getCount() >= Config.HERO_COUNT && _hero && Config.ALLOW_HERO_SUBSKILL)
		{
			giveHeroSkills();
		}
		else
		{
			removeHeroSkills();
		}
	}
	
	public void giveHeroSkills()
	{
		for (L2Skill s : HeroSkillTable.getHeroSkills())
		{
			addSkill(s, false); // Dont Save Hero skills to database
		}
		sendSkillList();
	}
	
	public void removeHeroSkills()
	{
		for (L2Skill s : HeroSkillTable.getHeroSkills())
		{
			super.removeSkill(s); // Just Remove skills from nonHero characters
		}
		sendSkillList();
	}
	
	/**
	 * Get the current pet skill in use or return null.<br>
	 * <br>
	 * @return
	 */
	public SkillDat getCurrentPetSkill()
	{
		return _currentPetSkill;
	}
	
	/**
	 * Create a new SkillDat object and set the player _currentPetSkill.<br>
	 * <br>
	 * @param currentSkill
	 * @param ctrlPressed
	 * @param shiftPressed
	 */
	
	public void setCurrentPetSkill(L2Skill currentSkill, boolean ctrlPressed, boolean shiftPressed)
	{
		if (currentSkill == null)
		{
			if (Config.DEBUG)
			{
				LOG.info("Setting current pet skill: NULL for " + getName() + ".");
			}
			
			_currentPetSkill = null;
			return;
		}
		
		if (Config.DEBUG)
		{
			LOG.info("Setting current Pet skill: " + currentSkill.getName() + " (ID: " + currentSkill.getId() + ") for " + getName() + ".");
		}
		
		_currentPetSkill = new SkillDat(currentSkill, ctrlPressed, shiftPressed);
	}
	
	public void setExpOn(int ExpOn)
	{
		if (ExpOn > 1 || ExpOn < 0)
		{
			ExpOn = 0;
		}
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("UPDATE characters SET gainexp=? WHERE obj_id=?");
			statement.setInt(1, ExpOn);
			statement.setInt(2, getObjectId());
			statement.execute();
			statement.close();
			statement = null;
			con.close();
			if (ExpOn == 1)
			{
				_expGainOn = true;
			}
			else
			{
				_expGainOn = false;
			}
		}
		catch (Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
		}
		finally
		{
			CloseUtil.close(con);
		}
	}
	
	public boolean getExpOn()
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT gainexp FROM characters WHERE obj_id=?");
			statement.setInt(1, getObjectId());
			ResultSet rset = statement.executeQuery();
			rset.next();
			int ExpOn = rset.getInt("gainexp");
			rset.close();
			statement.close();
			statement = null;
			rset = null;
			con.close();
			if (ExpOn == 1)
			{
				_expGainOn = true;
			}
			else
			{
				_expGainOn = false;
			}
		}
		catch (Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
		}
		finally
		{
			CloseUtil.close(con);
		}
		return _expGainOn;
	}
	
	public void setTitleOn(int TitleOn)
	{
		if (TitleOn > 1 || TitleOn < 0)
		{
			TitleOn = 0;
		}
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("UPDATE characters SET titlestatus=? WHERE obj_id=?");
			statement.setInt(1, TitleOn);
			statement.setInt(2, getObjectId());
			statement.execute();
			statement.close();
			statement = null;
			con.close();
			if (TitleOn == 1)
			{
				updateTitle();
				_TitleOn = true;
			}
			else
			{
				_TitleOn = false;
			}
		}
		catch (Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
		}
		finally
		{
			CloseUtil.close(con);
		}
	}
	
	public boolean getTitleOn()
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT titlestatus FROM characters WHERE obj_id=?");
			statement.setInt(1, getObjectId());
			ResultSet rset = statement.executeQuery();
			rset.next();
			int TitleOn = rset.getInt("titlestatus");
			rset.close();
			statement.close();
			statement = null;
			rset = null;
			con.close();
			if (TitleOn == 1)
			{
				updateTitle();
				_TitleOn = true;
			}
			else
			{
				_TitleOn = false;
			}
		}
		catch (Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
		}
		finally
		{
			CloseUtil.close(con);
		}
		return _TitleOn;
	}
	
	public void setBlockAllBuffs(int blockBuff)
	{
		if (blockBuff > 1 || blockBuff < 0)
		{
			blockBuff = 0;
		}
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement(BLOCKBUFF_SET);
			statement.setInt(1, blockBuff);
			statement.setInt(2, getObjectId());
			statement.execute();
			statement.close();
			statement = null;
			con.close();
			if (blockBuff == 1)
			{
				_blockBuff = true;
			}
			else
			{
				_blockBuff = false;
			}
		}
		catch (Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
		}
		finally
		{
			CloseUtil.close(con);
		}
	}
	
	public boolean getBlockAllBuffs()
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement(BLOCKBUFF_GET);
			statement.setInt(1, getObjectId());
			ResultSet rset = statement.executeQuery();
			rset.next();
			int blockBuff = rset.getInt("blockbuff");
			rset.close();
			statement.close();
			statement = null;
			rset = null;
			con.close();
			if (blockBuff == 1)
			{
				_blockBuff = true;
			}
			else
			{
				_blockBuff = false;
			}
		}
		catch (Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
		}
		finally
		{
			CloseUtil.close(con);
		}
		return _blockBuff;
	}
	
	public void setAutoLootEnabled(int autoLoot)
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement(AUTOLOOT_SET);
			statement.setInt(1, autoLoot);
			statement.setInt(2, getObjectId());
			statement.execute();
			statement.close();
			con.close();
			if (autoLoot == 0)
			{
				autoLootEnabled = false;
			}
			else
			{
				autoLootEnabled = true;
			}
		}
		catch (Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
		}
		finally
		{
			CloseUtil.close(con);
		}
	}
	
	public boolean getAutoLootEnabled()
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement(AUTOLOOT_GET);
			statement.setInt(1, getObjectId());
			ResultSet rset = statement.executeQuery();
			rset.next();
			int autoLoot = rset.getInt("autoloot");
			rset.close();
			statement.close();
			con.close();
			if (autoLoot == 0)
			{
				autoLootEnabled = false;
			}
			else
			{
				autoLootEnabled = true;
			}
		}
		catch (Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
		}
		finally
		{
			CloseUtil.close(con);
		}
		
		return autoLootEnabled;
	}
	
	public void setTeleport(int teleport)
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("UPDATE characters SET teleport=? WHERE obj_id=?");
			statement.setInt(1, teleport);
			statement.setInt(2, getObjectId());
			statement.execute();
			statement.close();
			con.close();
			if (teleport == 0)
			{
				teleportSystem = false;
			}
			else
			{
				teleportSystem = true;
			}
		}
		catch (Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
		}
		finally
		{
			CloseUtil.close(con);
		}
	}
	
	public boolean getTeleport()
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT teleport FROM characters WHERE obj_id=?");
			statement.setInt(1, getObjectId());
			ResultSet rset = statement.executeQuery();
			rset.next();
			int teleport = rset.getInt("teleport");
			rset.close();
			statement.close();
			con.close();
			if (teleport == 0)
			{
				teleportSystem = false;
			}
			else
			{
				teleportSystem = true;
			}
		}
		catch (Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
		}
		finally
		{
			CloseUtil.close(con);
		}
		return teleportSystem;
	}
	
	public void setEffects(int effects)
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("UPDATE characters SET effects=? WHERE obj_id=?");
			statement.setInt(1, effects);
			statement.setInt(2, getObjectId());
			statement.execute();
			statement.close();
			con.close();
			if (effects == 0)
			{
				characterEffects = false;
			}
			else
			{
				characterEffects = true;
			}
		}
		catch (Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
		}
		finally
		{
			CloseUtil.close(con);
		}
	}
	
	public boolean getEffects()
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT effects FROM characters WHERE obj_id=?");
			statement.setInt(1, getObjectId());
			ResultSet rset = statement.executeQuery();
			rset.next();
			int effects = rset.getInt("effects");
			rset.close();
			statement.close();
			con.close();
			if (effects == 0)
			{
				characterEffects = false;
			}
			else
			{
				characterEffects = true;
			}
		}
		catch (Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
		}
		finally
		{
			CloseUtil.close(con);
		}
		return characterEffects;
	}
	
	public void setGlow(int glow)
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("UPDATE characters SET glow=? WHERE obj_id=?");
			statement.setInt(1, glow);
			statement.setInt(2, getObjectId());
			statement.execute();
			statement.close();
			con.close();
			if (glow == 0)
			{
				enchantGlow = false;
			}
			else
			{
				enchantGlow = true;
			}
		}
		catch (Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
		}
		finally
		{
			CloseUtil.close(con);
		}
	}
	
	public boolean getGlow()
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT glow FROM characters WHERE obj_id=?");
			statement.setInt(1, getObjectId());
			ResultSet rset = statement.executeQuery();
			rset.next();
			int glow = rset.getInt("glow");
			rset.close();
			statement.close();
			con.close();
			if (glow == 0)
			{
				enchantGlow = false;
			}
			else
			{
				enchantGlow = true;
			}
		}
		catch (Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
		}
		finally
		{
			CloseUtil.close(con);
		}
		return enchantGlow;
	}
	
	public void setAutoLootHerbs(int autoLoot)
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement(AUTOHERBS_SET);
			statement.setInt(1, autoLoot);
			statement.setInt(2, getObjectId());
			statement.execute();
			statement.close();
			con.close();
			if (autoLoot == 0)
			{
				autoLootHerbs = false;
			}
			else
			{
				autoLootHerbs = true;
			}
		}
		catch (Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
		}
		finally
		{
			CloseUtil.close(con);
		}
	}
	
	public boolean getAutoLootHerbs()
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement(AUTOHERBS_GET);
			statement.setInt(1, getObjectId());
			ResultSet rset = statement.executeQuery();
			rset.next();
			int autoLoot = rset.getInt("autoloot_herbs");
			rset.close();
			statement.close();
			statement = null;
			rset = null;
			con.close();
			if (autoLoot == 0)
			{
				autoLootHerbs = false;
			}
			else
			{
				autoLootHerbs = true;
			}
		}
		catch (Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
		}
		finally
		{
			CloseUtil.close(con);
		}
		return autoLootHerbs;
	}
	
	public int getAutoLootEnabledFromDB()
	{
		if (autoLootEnabled)
		{
			return 1;
		}
		
		return 0;
	}
	
	public int getAutoLootHerbsEnabledFromDB()
	{
		if (autoLootHerbs)
		{
			return 1;
		}
		
		return 0;
	}
	
	public void setMessageRefusal(int messageRefusal)
	{
		if (messageRefusal > 1 || messageRefusal < 0)
		{
			messageRefusal = 0;
		}
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("UPDATE characters SET pm=? WHERE obj_id=?");
			statement.setInt(1, messageRefusal);
			statement.setInt(2, getObjectId());
			statement.execute();
			statement.close();
			statement = null;
			con.close();
			if (messageRefusal == 1)
			{
				_pmSilenceMode = false;
			}
			else
			{
				_pmSilenceMode = true;
			}
		}
		catch (Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
		}
		finally
		{
			CloseUtil.close(con);
		}
		sendPacket(new EtcStatusUpdate(this));
	}
	
	public boolean getMessageRefusal()
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT pm FROM characters WHERE obj_id=?");
			statement.setInt(1, getObjectId());
			ResultSet rset = statement.executeQuery();
			rset.next();
			int messageRefusal = rset.getInt("pm");
			rset.close();
			statement.close();
			statement = null;
			rset = null;
			con.close();
			if (messageRefusal == 1)
			{
				_pmSilenceMode = false;
			}
			else
			{
				_pmSilenceMode = true;
			}
		}
		catch (Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
		}
		finally
		{
			CloseUtil.close(con);
			con = null;
		}
		return _pmSilenceMode;
	}
	
	public void setIpBlock(boolean ipblock)
	{
		_ipblock = ipblock;
	}
	
	public boolean getIpBlock()
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT IPBlock FROM " + Config.LOGINSERVER_DB + ".accounts WHERE login=?");
			statement.setString(1, getAccountName());
			ResultSet rset = statement.executeQuery();
			rset.next();
			int ipblock = rset.getInt("IPBlock");
			rset.close();
			statement.close();
			statement = null;
			rset = null;
			con.close();
			if (ipblock == 1)
			{
				_ipblock = true;
			}
			else
			{
				_ipblock = false;
			}
		}
		catch (Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
		}
		finally
		{
			CloseUtil.close(con);
		}
		return _ipblock;
	}
	
	public void sethwidBlock(boolean hwidblock)
	{
		_hwidblock = hwidblock;
	}
	
	public boolean gethwidBlock()
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT HWIDBlockON FROM " + Config.LOGINSERVER_DB + ".accounts WHERE login=?");
			statement.setString(1, getAccountName());
			ResultSet rset = statement.executeQuery();
			rset.next();
			int hwidblock = rset.getInt("HWIDBlockON");
			rset.close();
			statement.close();
			statement = null;
			rset = null;
			con.close();
			if (hwidblock == 1)
			{
				_hwidblock = true;
			}
			else
			{
				_hwidblock = false;
			}
		}
		catch (Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
		}
		finally
		{
			CloseUtil.close(con);
		}
		return _hwidblock;
	}
	
	public void setTradeRefusal(int tradeRefusal)
	{
		if (tradeRefusal > 1 || tradeRefusal < 0)
		{
			tradeRefusal = 0;
		}
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("UPDATE characters SET trade=? WHERE obj_id=?");
			statement.setInt(1, tradeRefusal);
			statement.setInt(2, getObjectId());
			statement.execute();
			statement.close();
			statement = null;
			con.close();
			if (tradeRefusal == 1)
			{
				_tradeRefusal = false;
			}
			else
			{
				_tradeRefusal = true;
			}
		}
		catch (Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
		}
		finally
		{
			CloseUtil.close(con);
		}
	}
	
	public boolean getTradeRefusal()
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT trade FROM characters WHERE obj_id=?");
			statement.setInt(1, getObjectId());
			ResultSet rset = statement.executeQuery();
			rset.next();
			int tradeRefusal = rset.getInt("trade");
			rset.close();
			statement.close();
			statement = null;
			rset = null;
			con.close();
			if (tradeRefusal == 1)
			{
				_tradeRefusal = false;
			}
			else
			{
				_tradeRefusal = true;
			}
		}
		catch (Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
		}
		finally
		{
			CloseUtil.close(con);
		}
		return _tradeRefusal;
	}
	
	public void setScreentxt(int screentxt)
	{
		if (screentxt > 1 || screentxt < 0)
		{
			screentxt = 0;
		}
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("UPDATE characters SET screentxt=? WHERE obj_id=?");
			statement.setInt(1, screentxt);
			statement.setInt(2, getObjectId());
			statement.execute();
			statement.close();
			statement = null;
			con.close();
			if (screentxt == 1)
			{
				_screentxt = false;
			}
			else
			{
				_screentxt = true;
			}
		}
		catch (Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
		}
		finally
		{
			CloseUtil.close(con);
		}
	}
	
	@Override
	public boolean getScreentxt()
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT screentxt FROM characters WHERE obj_id=?");
			statement.setInt(1, getObjectId());
			ResultSet rset = statement.executeQuery();
			rset.next();
			int screentxt = rset.getInt("screentxt");
			rset.close();
			statement.close();
			statement = null;
			rset = null;
			con.close();
			if (screentxt == 1)
			{
				_screentxt = false;
			}
			else
			{
				_screentxt = true;
			}
		}
		catch (Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
		}
		finally
		{
			CloseUtil.close(con);
		}
		return _screentxt;
	}
	
	/**
	 * @return the mailPosition.
	 */
	public int getMailPosition()
	{
		return _mailPosition;
	}
	
	/**
	 * @param mailPosition The mailPosition to set.
	 */
	public void setMailPosition(int mailPosition)
	{
		_mailPosition = mailPosition;
	}
	
	private final List<Integer> _selectedBlocksList = new ArrayList<>();
	
	public void selectBlock(Integer friendId)
	{
		if (!_selectedBlocksList.contains(friendId))
		{
			_selectedBlocksList.add(friendId);
		}
	}
	
	public void deselectBlock(Integer friendId)
	{
		if (_selectedBlocksList.contains(friendId))
		{
			_selectedBlocksList.remove(friendId);
		}
	}
	
	public List<Integer> getSelectedBlocksList()
	{
		return _selectedBlocksList;
	}
	
	public void updatePincheck()
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("UPDATE characters SET pinsubmited=? WHERE obj_Id=?");
			int _pin;
			if (getPincheck())
			{
				_pin = 1;
			}
			else
			{
				_pin = 0;
			}
			statement.setInt(1, _pin);
			statement.setInt(2, getObjectId());
			statement.execute();
			statement.close();
		}
		catch (Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
		}
		finally
		{
			CloseUtil.close(con);
		}
	}
	
	public void setPincheck(boolean pincheck)
	{
		_pincheck = pincheck;
	}
	
	public void setPincheck(int pincheck)
	{
		_pincheck = false;
		if (pincheck == 1)
		{
			_pincheck = true;
		}
	}
	
	public boolean getPincheck()
	{
		return _pincheck;
	}
	
	public void setPin(int pin)
	{
		_pin = pin;
	}
	
	public int getPin()
	{
		return _pin;
	}
	
	public void showEnterPinHtml()
	{
		TextBuilder tb = new TextBuilder();
		NpcHtmlMessage html = new NpcHtmlMessage(1);
		
		tb.append("<html><head><title>Security System</title></head>");
		tb.append("<body>");
		tb.append("<center>");
		tb.append("<img src=\"Sek.cbui371\" width=\"280\" height=\"1\">");
		tb.append("<table width=\"280\" height=\"26\" bgcolor=\"000000\">");
		tb.append("<tr>");
		tb.append("<td align=\"center\"><font color=\"a7a6a6\">Enter Pin Code for security reasons.</td>");
		tb.append("</tr>");
		tb.append("</table>");
		tb.append("<img src=\"Sek.cbui371\" width=\"280\" height=\"1\">");
		tb.append("<br>");
		tb.append("<table width=\"280\" cellpadding=\"5\">");
		tb.append("<tr>");
		tb.append("<td width=\"45\" valign=\"top\" align=\"center\"><img src=\"Icon.etc_old_key_i02\" width=\"32\" height=\"32\"></td>");
		tb.append("<td valign=\"top\">Please enter your PIN:<edit var=\"dapin\" width=80 height=15></td>");
		tb.append("</tr>");
		tb.append("</table>");
		tb.append("<br>");
		tb.append("<button value=\"Submit\" action=\"bypass -h enterpin $dapin\" width=204 height=20>");
		tb.append("</center>");
		tb.append("</body></html>");
		
		html.setHtml(tb.toString());
		sendPacket(html);
	}
	
	public void showCreatePinHtml()
	{
		TextBuilder tb = new TextBuilder();
		NpcHtmlMessage html = new NpcHtmlMessage(1);
		
		tb.append("<html><head><title>Pin Code Creation</title></head>");
		tb.append("<body>");
		tb.append("<center>");
		tb.append("<img src=\"Sek.cbui371\" width=\"280\" height=\"1\">");
		tb.append("<table width=\"280\" height=\"26\" bgcolor=\"000000\">");
		tb.append("<tr>");
		tb.append("<td align=\"center\"><font color=\"a7a6a6\">Create Pin Code for security reasons.</td>");
		tb.append("</tr>");
		tb.append("</table>");
		tb.append("<img src=\"Sek.cbui371\" width=\"280\" height=\"1\">");
		tb.append("<br>");
		tb.append("<table width=\"280\" cellpadding=\"5\">");
		tb.append("<tr>");
		tb.append("<td width=\"45\" valign=\"top\" align=\"center\"><img src=\"Icon.etc_old_key_i02\" width=\"32\" height=\"32\"></td>");
		tb.append("<td valign=\"top\">Please enter your PIN:<edit var=\"pin\" width=80 height=15></td>");
		tb.append("</tr>");
		tb.append("</table>");
		tb.append("<br>");
		tb.append("<button value=\"Submit\" action=\"bypass -h submitpin $pin\" width=204 height=20>");
		tb.append("</center>");
		tb.append("</body></html>");
		
		html.setHtml(tb.toString());
		sendPacket(html);
	}
	
	public long getLastHopVote()
	{
		return _lastHopVote;
	}
	
	public long getLastTopVote()
	{
		return _lastTopVote;
	}
	
	public long getLastNetVote()
	{
		return _lastNetVote;
	}
	
	public long getLastBraVote()
	{
		return _lastBraVote;
	}
	
	public long getLastL2TopGr()
	{
		return _lastL2TopGr;
	}
	
	public long getLastL2TopOnline()
	{
		return _lastL2TopOnline;
	}
	
	public void setLastHopVote(long val)
	{
		_lastHopVote = val;
	}
	
	public void setLastTopVote(long val)
	{
		_lastTopVote = val;
	}
	
	public void setLastNetVote(long val)
	{
		_lastNetVote = val;
	}
	
	public void setLastBraVote(long val)
	{
		_lastBraVote = val;
	}
	
	public void setLastL2TopGr(long val)
	{
		_lastL2TopGr = val;
	}
	
	public void setLastL2TopOnline(long val)
	{
		_lastL2TopOnline = val;
	}
	
	public boolean eligibleToVoteHop()
	{
		return (getLastHopVote() + 43200000) < System.currentTimeMillis();
	}
	
	public boolean eligibleToVoteTop()
	{
		return (getLastTopVote() + 43200000) < System.currentTimeMillis();
	}
	
	public boolean eligibleToVoteNet()
	{
		return (getLastNetVote() + 43200000) < System.currentTimeMillis();
	}
	
	public boolean eligibleToVoteBra()
	{
		return (getLastBraVote() + 43200000) < System.currentTimeMillis();
	}
	
	public boolean eligibleToVoteL2TopGr()
	{
		return (getLastL2TopGr() + 43200000) < System.currentTimeMillis();
	}
	
	public boolean eligibleToVoteL2TopOnline()
	{
		return (getLastL2TopOnline() + 43200000) < System.currentTimeMillis();
	}
	
	public String getVoteCountdownHop()
	{
		long youCanVote = getLastHopVote() - (System.currentTimeMillis() - 43200000);
		return convertLongToCountdown(youCanVote);
	}
	
	public String getVoteCountdownTop()
	{
		long youCanVote = getLastTopVote() - (System.currentTimeMillis() - 43200000);
		return convertLongToCountdown(youCanVote);
	}
	
	public String getVoteCountdownNet()
	{
		long youCanVote = getLastNetVote() - (System.currentTimeMillis() - 43200000);
		return convertLongToCountdown(youCanVote);
	}
	
	public String getVoteCountdownBra()
	{
		long youCanVote = getLastBraVote() - (System.currentTimeMillis() - 43200000);
		return convertLongToCountdown(youCanVote);
	}
	
	public String getVoteCountdownL2TopGr()
	{
		long youCanVote = getLastL2TopGr() - (System.currentTimeMillis() - 43200000);
		return convertLongToCountdown(youCanVote);
	}
	
	public String getVoteCountdownL2TopOnline()
	{
		long youCanVote = getLastL2TopOnline() - (System.currentTimeMillis() - 43200000);
		return convertLongToCountdown(youCanVote);
	}
	
	public static String convertLongToCountdown(long youCanVote)
	{
		String h = String.format("%d", TimeUnit.MILLISECONDS.toHours(youCanVote));
		
		if (Integer.parseInt(h) < 10)
		{
			h = "0" + h;
		}
		String m = String.format("%d", TimeUnit.MILLISECONDS.toMinutes(youCanVote) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(youCanVote)));
		
		if (Integer.parseInt(m) < 10)
		{
			m = "0" + m;
		}
		
		String s = String.format("%d", TimeUnit.MILLISECONDS.toSeconds(youCanVote) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(youCanVote)));
		
		if (Integer.parseInt(s) < 10)
		{
			s = "0" + s;
		}
		
		return h + ":" + m + ":" + s;
	}
	
	@Override
	public void sendInfo(L2PcInstance activeChar)
	{
		if (!isGM() && _observerMode)
		{
			return;
		}
		
		if (isGM() && getAppearance().getInvisible() && !activeChar.isGM())
		{
			return;
		}
		
		activeChar.sendPacket(new CharInfo(this));
		
		if (isInBoat())
		{
			getPosition().setWorldPosition(getBoat().getPosition().getWorldPosition());
			activeChar.sendPacket(new GetOnVehicle(getObjectId(), getBoat().getObjectId(), getInVehiclePosition()));
		}
		
		int relation = getRelation(activeChar);
		if (getKnownList().getKnownRelations().get(activeChar.getObjectId()) != null && getKnownList().getKnownRelations().get(activeChar.getObjectId()) != relation)
		{
			activeChar.sendPacket(new RelationChanged(this, relation, activeChar.isAutoAttackable(this)));
		}
		
		if (isInStoreMode())
		{
			if (getPrivateStoreType() != STORE_PRIVATE_NONE)
			{
				switch (getPrivateStoreType())
				{
					case L2PcInstance.STORE_PRIVATE_SELL:
					case L2PcInstance.STORE_PRIVATE_PACKAGE_SELL:
						activeChar.sendPacket(new PrivateStoreMsgSell(this));
						break;
					case L2PcInstance.STORE_PRIVATE_BUY:
						activeChar.sendPacket(new PrivateStoreMsgBuy(this));
						break;
					case L2PcInstance.STORE_PRIVATE_MANUFACTURE:
						activeChar.sendPacket(new RecipeShopMsg(this));
						break;
				}
			}
		}
	}
	
	public void rndWalk()
	{
		int posX = getX();
		int posY = getY();
		int posZ = getZ();
		
		switch (Rnd.get(1, 8))
		{
			case 1:
				posX += 600;
				posY += 200;
				break;
			case 2:
				posX += 200;
				posY += 600;
				break;
			case 3:
				posX += 600;
				posY -= 200;
				break;
			case 4:
				posX += 200;
				posY -= 600;
				break;
			case 5:
				posX -= 600;
				posY += 200;
				break;
			case 6:
				posX -= 200;
				posY += 600;
				break;
			case 7:
				posX -= 200;
				posY -= 600;
				break;
			case 8:
				posX -= 600;
				posY -= 200;
		}
		
		getAI().moveTo(posX, posY, posZ);
	}
	
	public Location _phantomLoc = null;
	
	public void setPhantomLoc(int x, int y, int z, int heading)
	{
		_phantomLoc = new Location(x, y, z, heading);
	}
	
	public Location getPhantomLoc()
	{
		return _phantomLoc;
	}
	
	private List<L2Character> PhantomTargetList = new FastList<>();
	
	public List<L2Character> getPhantomTargetList()
	{
		return PhantomTargetList;
	}
	
	public static L2PcInstance restorePhantom(int objectId)
	{
		L2PcInstance player = null;
		Connection con = null;
		try
		{
			// Retrieve the L2PcInstance from the characters table of the database
			con = L2DatabaseFactory.getInstance().getConnection(false);
			PreparedStatement statement = con.prepareStatement(RESTORE_CHARACTER);
			statement.setInt(1, objectId);
			ResultSet rset = statement.executeQuery();
			
			while (rset.next())
			{
				final int activeClassId = rset.getInt("classid");
				final boolean female = rset.getInt("sex") != 0;
				final L2PcTemplate template = CharTemplateTable.getInstance().getTemplate(activeClassId);
				PcAppearance app = new PcAppearance(rset.getByte("face"), rset.getByte("hairColor"), rset.getByte("hairStyle"), female);
				
				player = new L2PcInstance(objectId, template, rset.getString("account_name"), app);
				player.setIsPhantom(true);
				restorePremServiceData(player, rset.getString("account_name"));
				
				player.setName(rset.getString("char_name"));
				player._lastAccess = rset.getLong("lastAccess");
				
				player.getStat().setExp(rset.getLong("exp"));
				player.setExpBeforeDeath(rset.getLong("expBeforeDeath"));
				
				player.getStat().setLevel(1);
				
				player.getStat().setSp(rset.getInt("sp"));
				
				player.setWantsPeace(rset.getInt("wantspeace"));
				
				player.setHeading(rset.getInt("heading"));
				
				player.setKarma(0);
				player.setPvpKills(0);
				player.setPkKills(0);
				player.setOnlineTime(0);
				player.setNewbie(rset.getInt("newbie") == 1);
				player.setNoble(rset.getInt("nobless") == 1);
				player.setClanJoinExpiryTime(rset.getLong("clan_join_expiry_time"));
				player.setFirstLog(rset.getInt("first_log"));
				player.pcBangPoint = rset.getInt("pc_point");
				
				if (player.getClanJoinExpiryTime() < System.currentTimeMillis())
				{
					player.setClanJoinExpiryTime(0);
				}
				player.setClanCreateExpiryTime(rset.getLong("clan_create_expiry_time"));
				if (player.getClanCreateExpiryTime() < System.currentTimeMillis())
				{
					player.setClanCreateExpiryTime(0);
				}
				
				int clanId = rset.getInt("clanid");
				player.setPowerGrade((int) rset.getLong("power_grade"));
				player.setPledgeType(rset.getInt("subpledge"));
				player.setLastRecomUpdate(rset.getLong("last_recom_date"));
				
				if (clanId > 0)
				{
					player.setClan(ClanTable.getInstance().getClan(clanId));
				}
				
				if (player.getClan() != null)
				{
					if (player.getClan().getLeaderId() != player.getObjectId())
					{
						if (player.getPowerGrade() == 0)
						{
							player.setPowerGrade(6);
						}
						player.setClanPrivileges(player.getClan().getRankPrivs(player.getPowerGrade()));
					}
					else
					{
						player.setClanPrivileges(L2Clan.CP_ALL);
						player.setPowerGrade(1);
					}
				}
				else
				{
					player.setClanPrivileges(L2Clan.CP_NOTHING);
				}
				
				player.setDeleteTimer(rset.getLong("deletetime"));
				player.setTitle(rset.getString("title"));
				player.setAccessLevel(rset.getInt("accesslevel"));
				player.setFistsWeaponItem(player.findFistsWeaponItem(activeClassId));
				player.setUptime(System.currentTimeMillis());
				
				player.getStatus().setCurrentHp(player.getMaxHp());
				player.getStatus().setCurrentMp(player.getMaxMp());
				player.getStatus().setCurrentCp(player.getMaxCp());
				
				player.checkRecom(rset.getInt("rec_have"), rset.getInt("rec_left"));
				
				if (player.getClassId().getRace() == Race.human)
				{
					if (player.getClassId().isMage())
					{
						player.setBaseClass(10);
						player.setClassTemplate(10);
					}
					else
					{
						player.setBaseClass(0);
						player.setClassTemplate(0);
					}
				}
				if (player.getClassId().getRace() == Race.elf)
				{
					if (player.getClassId().isMage())
					{
						player.setBaseClass(25);
						player.setClassTemplate(25);
					}
					else
					{
						player.setBaseClass(18);
						player.setClassTemplate(18);
					}
				}
				
				if (player.getClassId().getRace() == Race.darkelf)
				{
					if (player.getClassId().isMage())
					{
						player.setBaseClass(38);
						player.setClassTemplate(38);
					}
					else
					{
						player.setBaseClass(31);
						player.setClassTemplate(31);
					}
				}
				
				if (player.getClassId().getRace() == Race.orc)
				{
					if (player.getClassId().isMage())
					{
						player.setBaseClass(49);
						player.setClassTemplate(49);
					}
					else
					{
						player.setBaseClass(53);
						player.setClassTemplate(53);
					}
				}
				
				if (player.getClassId().getRace() == Race.dwarf)
				{
					player.setBaseClass(0);
					player.setClassTemplate(0);
				}
				/*
				 * player._classIndex = 0; try { player.setBaseClass(rset.getInt("base_class")); } catch (Exception e) { if (Config.ENABLE_ALL_EXCEPTIONS) e.printStackTrace(); player.setBaseClass(activeClassId); } // Restore Subclass Data (cannot be done earlier in function) if
				 * (restoreSubClassData(player)) { if (activeClassId != player.getBaseClass()) { for (SubClass subClass : player.getSubClasses().values()) if (subClass.getClassId() == activeClassId) { player._classIndex = subClass.getClassIndex(); } } } if (player.getClassIndex() == 0 &&
				 * activeClassId != player.getBaseClass()) { // Subclass in use but doesn't exist in DB - // a possible restart-while-modifysubclass cheat has been attempted. // Switching to use base class player.setClassId(player.getBaseClass()); LOGGER.warn("Player " + player.getName() +
				 * " reverted to base class. Possibly has tried a relogin exploit while subclassing."); } else { player._activeClass = activeClassId; }
				 */
				
				player.setApprentice(rset.getInt("apprentice"));
				player.setSponsor(rset.getInt("sponsor"));
				player.setLvlJoinedAcademy(rset.getInt("lvl_joined_academy"));
				player.setIsIn7sDungeon(rset.getInt("isin7sdungeon") == 1 ? true : false);
				
				player.setPunishLevel(rset.getInt("punish_level"));
				if (player.getPunishLevel() != PunishLevel.NONE)
				{
					player.setPunishTimer(rset.getLong("punish_timer"));
				}
				else
				{
					player.setPunishTimer(0);
				}
				
				try
				{
					player.getAppearance().setNameColor(Integer.decode(new StringBuilder().append("0x").append(rset.getString("name_color")).toString()).intValue());
					player.getAppearance().setTitleColor(Integer.decode(new StringBuilder().append("0x").append(rset.getString("title_color")).toString()).intValue());
				}
				catch (Exception e)
				{
					if (Config.ENABLE_ALL_EXCEPTIONS)
					{
						e.printStackTrace();
					}
				}
				
				// CursedWeaponsManager.getInstance().checkPlayer(player);
				
				// player.setAllianceWithVarkaKetra(rset.getInt("varka_ketra_ally"));
				
				// player.setDeathPenaltyBuffLevel(rset.getInt("death_penalty_level"));
				player.setAio(rset.getInt("aio") == 1 ? true : false);
				player.setAioEndTime(rset.getLong("aio_end"));
				// player.setHitmanTarget(rset.getInt("hitman_target"));
				player.setAutoLootEnabled(rset.getInt("autoloot"));
				player.setAutoLootHerbs(rset.getInt("autoloot_herbs"));
				
				player.restoreCharData();
				player.rewardSkills(true);
				player.refreshOverloaded();
			}
			rset.close();
			statement.close();
		}
		catch (Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				LOG.error("Could not restore char data:");
				e.printStackTrace();
			}
		}
		finally
		{
			CloseUtil.close(con);
		}
		return player;
	}
	
	private Future<?> _aiPhantomTask;
	
	public Future<?> getPhantomAI()
	{
		return _aiPhantomTask;
	}
	
	public void startPhantomAI()
	{
		_aiPhantomTask = ThreadPoolManager.getInstance().scheduleAiAtFixedRate(new PhantomAction(this), 1000, 1000);
	}
	
	public void stopPhantomAI()
	{
		if (_aiPhantomTask != null)
		{
			_aiPhantomTask.cancel(true);
			_aiPhantomTask = null;
		}
	}
	
	private class PhantomAction implements Runnable
	{
		L2PcInstance _phantom;
		
		public PhantomAction(L2PcInstance phantom)
		{
			_phantom = phantom;
		}
		
		@Override
		public void run()
		{
			// repeat actions
			phantomActions.getInstance().startAction(_phantom);
		}
	}
	
	private boolean _moveToPawn = true;
	
	public void setMoveToPawn(boolean val)
	{
		_moveToPawn = val;
	}
	
	public boolean getMoveToPawn()
	{
		return _moveToPawn;
	}
	
	private final FastList<L2Skill> _buffSkills = new FastList<>();
	private final FastList<L2Skill> _negativeSkills = new FastList<>();
	private final FastList<L2Skill> _debuffSkills = new FastList<>();
	private final FastList<L2Skill> _atkSkills = new FastList<>();
	private final FastList<L2Skill> _rootSkills = new FastList<>();
	private final FastList<L2Skill> _stunSkills = new FastList<>();
	private final FastList<L2Skill> _sleepSkills = new FastList<>();
	private final FastList<L2Skill> _paralyzeSkills = new FastList<>();
	private final FastList<L2Skill> _fossilSkills = new FastList<>();
	private final FastList<L2Skill> _immobilizeSkills = new FastList<>();
	private final FastList<L2Skill> _healSkills = new FastList<>();
	private final FastList<L2Skill> _dotSkills = new FastList<>();
	private final FastList<L2Skill> _cotSkills = new FastList<>();
	private final FastList<L2Skill> _universalSkills = new FastList<>();
	private final FastList<L2Skill> _manaSkills = new FastList<>();
	private final FastList<L2Skill> _longRangeSkills = new FastList<>();
	private final FastList<L2Skill> _shortRangeSkills = new FastList<>();
	private final FastList<L2Skill> _generalSkills = new FastList<>();
	private final FastList<L2Skill> _suicideSkills = new FastList<>();
	
	private final Map<Integer, L2Skill> _phantomSkills = new FastMap<>();
	
	public void addPhantomSkill(L2Skill skill)
	{
		if (!skill.isPassive())
		{
			if (skill.isSuicideAttack())
			{
				addSuicideSkill(skill);
			}
			else
			{
				addGeneralSkill(skill);
				switch (skill.getSkillType())
				{
					case BUFF:
						addBuffSkill(skill);
						break;
					
					case HEAL:
					case HOT:
					case HEAL_PERCENT:
					case HEAL_STATIC:
					case BALANCE_LIFE:
						addHealSkill(skill);
						break;
					
					case DEBUFF:
						addDebuffSkill(skill);
						addCOTSkill(skill);
						addRangeSkill(skill);
						break;
					
					case ROOT:
						addRootSkill(skill);
						addImmobilizeSkill(skill);
						addRangeSkill(skill);
						break;
					
					case SLEEP:
						addSleepSkill(skill);
						addImmobilizeSkill(skill);
						break;
					
					case STUN:
						addRootSkill(skill);
						addImmobilizeSkill(skill);
						addRangeSkill(skill);
						break;
					
					case PARALYZE:
						addParalyzeSkill(skill);
						addImmobilizeSkill(skill);
						addRangeSkill(skill);
						break;
					
					case PDAM:
					case MDAM:
					case BLOW:
					case DRAIN:
					case CHARGEDAM:
					case DEATHLINK:
					case MANADAM:
						addAtkSkill(skill);
						addUniversalSkill(skill);
						addRangeSkill(skill);
						break;
					
					case POISON:
					case DOT:
					case MDOT:
					case BLEED:
						addDOTSkill(skill);
						addRangeSkill(skill);
						break;
					
					case MUTE:
					case FEAR:
						addCOTSkill(skill);
						addRangeSkill(skill);
						break;
					
					case CANCEL:
					case NEGATE:
						addNegativeSkill(skill);
						addRangeSkill(skill);
						break;
					
					default:
						addUniversalSkill(skill);
						break;
				}
			}
		}
		_phantomSkills.put(skill.getId(), skill);
	}
	
	public Map<Integer, L2Skill> getPhantomSkills()
	{
		return _phantomSkills;
	}
	
	public void addBuffSkill(L2Skill skill)
	{
		_buffSkills.add(skill);
	}
	
	public void addHealSkill(L2Skill skill)
	{
		_healSkills.add(skill);
	}
	
	public void addAtkSkill(L2Skill skill)
	{
		_atkSkills.add(skill);
	}
	
	public void addDebuffSkill(L2Skill skill)
	{
		_debuffSkills.add(skill);
	}
	
	public void addRootSkill(L2Skill skill)
	{
		_rootSkills.add(skill);
	}
	
	public void addSleepSkill(L2Skill skill)
	{
		_sleepSkills.add(skill);
	}
	
	public void addStunSkill(L2Skill skill)
	{
		_stunSkills.add(skill);
	}
	
	public void addParalyzeSkill(L2Skill skill)
	{
		_paralyzeSkills.add(skill);
	}
	
	public void addFossilSkill(L2Skill skill)
	{
		_fossilSkills.add(skill);
	}
	
	public void addNegativeSkill(L2Skill skill)
	{
		_negativeSkills.add(skill);
	}
	
	public void addImmobilizeSkill(L2Skill skill)
	{
		_immobilizeSkills.add(skill);
	}
	
	public void addDOTSkill(L2Skill skill)
	{
		_dotSkills.add(skill);
	}
	
	public void addUniversalSkill(L2Skill skill)
	{
		_universalSkills.add(skill);
	}
	
	public void addCOTSkill(L2Skill skill)
	{
		_cotSkills.add(skill);
	}
	
	public void addManaHealSkill(L2Skill skill)
	{
		_manaSkills.add(skill);
	}
	
	public void addGeneralSkill(L2Skill skill)
	{
		_generalSkills.add(skill);
	}
	
	public void addRangeSkill(L2Skill skill)
	{
		if ((skill.getCastRange() <= 150) && (skill.getCastRange() > 0))
		{
			_shortRangeSkills.add(skill);
		}
		else if (skill.getCastRange() > 150)
		{
			_longRangeSkills.add(skill);
		}
	}
	
	public void addSuicideSkill(L2Skill skill)
	{
		_suicideSkills.add(skill);
	}
	
	public FastList<L2Skill> getUniversalSkills()
	{
		return _universalSkills;
	}
	
	public FastList<L2Skill> getSuicideSkills()
	{
		return _suicideSkills;
	}
	
	public FastList<L2Skill> getNegativeSkills()
	{
		return _negativeSkills;
	}
	
	public FastList<L2Skill> getImmobilizeSkills()
	{
		return _immobilizeSkills;
	}
	
	public FastList<L2Skill> getGeneralSkills()
	{
		return _generalSkills;
	}
	
	public FastList<L2Skill> getHealSkills()
	{
		return _healSkills;
	}
	
	public FastList<L2Skill> getCostOverTimeSkills()
	{
		return _cotSkills;
	}
	
	public FastList<L2Skill> getDebuffSkills()
	{
		return _debuffSkills;
	}
	
	public FastList<L2Skill> getBuffSkills()
	{
		return _buffSkills;
	}
	
	public FastList<L2Skill> getAtkSkills()
	{
		return _atkSkills;
	}
	
	/**
	 * @return the long range skills.
	 */
	public FastList<L2Skill> getLongRangeSkills()
	{
		return _longRangeSkills;
	}
	
	/**
	 * @return the short range skills.
	 */
	public FastList<L2Skill> getShortRangeSkills()
	{
		return _shortRangeSkills;
	}
	
	public List<String> GetSelectedBoss()
	{
		return _selectedBoss;
	}
	
	public boolean isSellBuff()
	{
		return _sellbuff;
	}
	
	public void setSellBuff(boolean j)
	{
		_sellbuff = j;
	}
	
	public int getBuffPrize()
	{
		return _buffprize;
	}
	
	public void setBuffPrize(int x)
	{
		_buffprize = x;
	}
	
	public String getOldTitle()
	{
		return _oldtitle;
	}
	
	public int getOldNameColor()
	{
		return _oldnamecolor;
	}
	
	public void setOldTitle(String title)
	{
		_oldtitle = title;
	}
	
	public void setOldNameColor(int color)
	{
		_oldnamecolor = color;
	}
	
	public Set<String> getAllFavorites()
	{
		return Favorites;
	}
	
	public int getAllFavoritesCount()
	{
		return Favorites.size();
	}
	
	public boolean isVotedForEvent()
	{
		return _votedForEvent;
	}
	
	public void setVotedForEvent(boolean v)
	{
		_votedForEvent = v;
	}
	
	public boolean isVoteMenuOn()
	{
		return _voteMenu;
	}
	
	public void setVoteMenuOn(boolean v)
	{
		_voteMenu = v;
	}
	
	public final String getHWid()
	{
		L2GameClient client = getClient();
		if (client != null)
		{
			return client.getHWID();
		}
		return null;
	}
	
	public void setIsInSiege(boolean b)
	{
		_isInSiege = b;
	}
	
	public boolean isInSiege()
	{
		return _isInSiege;
	}
	
	/**
	 * @param isInHideoutSiege sets the value of {@link #_isInHideoutSiege}.
	 */
	public void setIsInHideoutSiege(boolean isInHideoutSiege)
	{
		_isInHideoutSiege = isInHideoutSiege;
	}
	
	/**
	 * @return the value of {@link #_isInHideoutSiege}, {@code true} if the player is participing on a Hideout Siege, otherwise {@code false}.
	 */
	public boolean isInHideoutSiege()
	{
		return _isInHideoutSiege;
	}
	
	private DressMeData _dressmedata = null;
	
	public DressMeData getDressMeData()
	{
		return _dressmedata;
	}
	
	public void setDressMeData(DressMeData val)
	{
		_dressmedata = val;
	}
	
	private boolean _dressed = false;
	
	public boolean isDressMeEnabled()
	{
		return _dressed;
	}
	
	public void setDressMeEnabled(boolean val)
	{
		_dressed = val;
	}
	
	public void setFakeArmorObjectId(int objectId)
	{
		_fakeArmorObjectId = objectId;
	}
	
	public int getFakeArmorObjectId()
	{
		return _fakeArmorObjectId;
	}
	
	public void setFakeArmorItemId(int itemId)
	{
		_fakeArmorItemId = itemId;
	}
	
	public int getFakeArmorItemId()
	{
		return _fakeArmorItemId;
	}
	
	private boolean inArenaEvent = false;
	
	public void setInArenaEvent(boolean val)
	{
		inArenaEvent = val;
	}
	
	public boolean isInArenaEvent()
	{
		return inArenaEvent;
	}
	
	private boolean _ArenaAttack;
	
	public void setArenaAttack(boolean comm)
	{
		_ArenaAttack = comm;
	}
	
	public boolean isArenaAttack()
	{
		return _ArenaAttack;
	}
	
	private boolean _ArenaProtection;
	
	public void setArenaProtection(boolean comm)
	{
		_ArenaProtection = comm;
	}
	
	public boolean isArenaProtection()
	{
		return _ArenaProtection;
	}
	
	private boolean _Arena2x2;
	
	public void setArena2x2(boolean comm)
	{
		_Arena2x2 = comm;
	}
	
	public boolean isArena2x2()
	{
		return _Arena2x2;
	}
	
	public final Achievement getAchievement()
	{
		return _achievement;
	}
}