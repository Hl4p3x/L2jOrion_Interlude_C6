/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package l2jorion.game.network.clientpackets;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import l2jguard.HwidConfig;
import l2jguard.hwidmanager.HwidManager;
import l2jorion.Config;
import l2jorion.game.GameServer;
import l2jorion.game.community.manager.MailBBSManager;
import l2jorion.game.controllers.GameTimeController;
import l2jorion.game.datatables.GmListTable;
import l2jorion.game.datatables.SkillTable;
import l2jorion.game.datatables.csv.MapRegionTable;
import l2jorion.game.datatables.sql.AdminCommandAccessRights;
import l2jorion.game.datatables.sql.CharNameTable;
import l2jorion.game.handler.custom.CustomWorldHandler;
import l2jorion.game.handler.item.Potions;
import l2jorion.game.handler.voice.Vote;
import l2jorion.game.managers.CastleManager;
import l2jorion.game.managers.ClanHallManager;
import l2jorion.game.managers.CoupleManager;
import l2jorion.game.managers.CrownManager;
import l2jorion.game.managers.DimensionalRiftManager;
import l2jorion.game.managers.FortSiegeManager;
import l2jorion.game.managers.PetitionManager;
import l2jorion.game.managers.SiegeManager;
import l2jorion.game.model.Inventory;
import l2jorion.game.model.L2Character;
import l2jorion.game.model.L2Clan;
import l2jorion.game.model.L2Skill;
import l2jorion.game.model.L2World;
import l2jorion.game.model.actor.instance.L2ClassMasterInstance;
import l2jorion.game.model.actor.instance.L2ItemInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.base.ClassLevel;
import l2jorion.game.model.base.PlayerClass;
import l2jorion.game.model.entity.Announcements;
import l2jorion.game.model.entity.ClanHall;
import l2jorion.game.model.entity.Hero;
import l2jorion.game.model.entity.Hitman;
import l2jorion.game.model.entity.Wedding;
import l2jorion.game.model.entity.event.CTF;
import l2jorion.game.model.entity.event.DM;
import l2jorion.game.model.entity.event.L2Event;
import l2jorion.game.model.entity.event.TvT;
import l2jorion.game.model.entity.sevensigns.SevenSigns;
import l2jorion.game.model.entity.siege.Castle;
import l2jorion.game.model.entity.siege.FortSiege;
import l2jorion.game.model.entity.siege.Siege;
import l2jorion.game.model.olympiad.Olympiad;
import l2jorion.game.model.quest.Quest;
import l2jorion.game.model.quest.QuestState;
import l2jorion.game.model.zone.ZoneId;
import l2jorion.game.network.L2GameClient.GameClientState;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.ActionFailed;
import l2jorion.game.network.serverpackets.ClientSetTime;
import l2jorion.game.network.serverpackets.CreatureSay;
import l2jorion.game.network.serverpackets.Die;
import l2jorion.game.network.serverpackets.Earthquake;
import l2jorion.game.network.serverpackets.EtcStatusUpdate;
import l2jorion.game.network.serverpackets.ExAutoSoulShot;
import l2jorion.game.network.serverpackets.ExMailArrived;
import l2jorion.game.network.serverpackets.ExRedSky;
import l2jorion.game.network.serverpackets.ExShowScreenMessage;
import l2jorion.game.network.serverpackets.ExStorageMaxCount;
import l2jorion.game.network.serverpackets.FriendList;
import l2jorion.game.network.serverpackets.HennaInfo;
import l2jorion.game.network.serverpackets.InventoryUpdate;
import l2jorion.game.network.serverpackets.ItemList;
import l2jorion.game.network.serverpackets.MagicSkillUser;
import l2jorion.game.network.serverpackets.NpcHtmlMessage;
import l2jorion.game.network.serverpackets.PlaySound;
import l2jorion.game.network.serverpackets.PledgeShowMemberListAll;
import l2jorion.game.network.serverpackets.PledgeShowMemberListUpdate;
import l2jorion.game.network.serverpackets.PledgeSkillList;
import l2jorion.game.network.serverpackets.PledgeStatusChanged;
import l2jorion.game.network.serverpackets.QuestList;
import l2jorion.game.network.serverpackets.ShortCutInit;
import l2jorion.game.network.serverpackets.SignsSky;
import l2jorion.game.network.serverpackets.StopMove;
import l2jorion.game.network.serverpackets.SystemMessage;
import l2jorion.game.network.serverpackets.UserInfo;
import l2jorion.game.network.serverpackets.ValidateLocation;
import l2jorion.game.powerpack.PowerPackConfig;
import l2jorion.game.powerpack.buffer.BuffTable;
import l2jorion.game.thread.ThreadPoolManager;
import l2jorion.game.util.Util;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;
import l2jorion.util.CloseUtil;
import l2jorion.util.database.L2DatabaseFactory;

public class EnterWorld extends L2GameClientPacket
{
	private static Logger LOG = LoggerFactory.getLogger(EnterWorld.class);
	
	private final SimpleDateFormat fmt = new SimpleDateFormat("H:mm:ss");
	private final SimpleDateFormat df = new SimpleDateFormat("dd MMMM, E, yyyy");
	private static final int MANA_POT_CD = (int) Config.MANA_POT_CD;
	private static final int HEALING_POT_CD = (int) Config.HEALING_POT_CD;
	private static final int CP_POT_CD = (int) Config.CP_POT_CD;
	
	@Override
	protected void readImpl()
	{
	}
	
	@Override
	protected void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();
		
		if (activeChar == null)
		{
			LOG.warn("EnterWorld: activeChar is null.");
			getClient().closeNow();
			return;
		}
		
		getClient().setState(GameClientState.IN_GAME);
		
		activeChar.setLocked(true);
		activeChar.SetIsEnteringToWorld(true);
		activeChar.setOnlineStatus(true);
		activeChar.setRunning();
		
		activeChar.broadcastKarma();
		
		if (Config.L2JGUARD_PROTECTION)
		{
			HwidManager.getInstance().validBox(activeChar, HwidConfig.PROTECT_WINDOWS_COUNT, L2World.getInstance().getAllPlayers().values(), true);
		}
		
		activeChar.broadcastPacket(new ValidateLocation(activeChar));
		activeChar.broadcastPacket(new StopMove(activeChar));
		
		if (L2World.getInstance().findObject(activeChar.getObjectId()) != null)
		{
			if (Config.DEBUG)
			{
				LOG.warn("DEBUG " + getType() + ": User already exist in OID map! User " + activeChar.getName() + " is character clone.");
			}
		}
		
		if (!activeChar.isGM() && !activeChar.isDonator() && Config.CHECK_NAME_ON_LOGIN)
		{
			if (activeChar.getName().length() < 3 || activeChar.getName().length() > 16 || !Util.isAlphaNumeric(activeChar.getName()) || !isValidName(activeChar.getName()))
			{
				LOG.warn("Charname: " + activeChar.getName() + " is invalid. EnterWorld failed.");
				getClient().closeNow();
				return;
			}
		}
		
		if (Config.ALLOW_HITMAN_GDE)
		{
			Hitman.getInstance().onEnterWorld(activeChar);
		}
		
		if (Config.L2JMOD_ALLOW_WEDDING)
		{
			engage(activeChar);
			notifyPartner(activeChar, activeChar.getPartnerId());
		}
		
		EnterGM(activeChar);
		
		if (Config.PLAYER_SPAWN_PROTECTION > 0)
		{
			activeChar.setProtection(true);
		}
		
		activeChar.spawnMe(activeChar.getX(), activeChar.getY(), activeChar.getZ());
		
		if (SevenSigns.getInstance().isSealValidationPeriod())
		{
			sendPacket(SignsSky.Sky());
		}
		
		// Buff and Status icons
		if (Config.STORE_SKILL_COOLTIME)
		{
			activeChar.restoreEffects();
		}
		
		// Apply augmentation boni for equipped items
		for (L2ItemInstance temp : activeChar.getInventory().getAugmentedItems())
		{
			if (temp != null && temp.isEquipped())
			{
				temp.getAugmentation().applyBoni(activeChar);
			}
		}
		
		if (L2Event.active && L2Event.connectionLossData.containsKey(activeChar.getName()) && L2Event.isOnEvent(activeChar))
		{
			L2Event.restoreChar(activeChar);
		}
		else if (L2Event.connectionLossData.containsKey(activeChar.getName()))
		{
			L2Event.restoreAndTeleChar(activeChar);
		}
		
		if (Config.MAX_ITEM_ENCHANT_KICK > 0)
		{
			for (L2ItemInstance i : activeChar.getInventory().getItems())
			{
				if (!activeChar.isGM())
				{
					if (i.isEquipable())
					{
						if (i.getEnchantLevel() > Config.MAX_ITEM_ENCHANT_KICK)
						{
							activeChar.getInventory().destroyItem(null, i, activeChar, null);
							activeChar.sendMessage("You have over enchanted items you will be kicked from server!");
							activeChar.sendMessage("Respect our server rules.");
							sendPacket(new ExShowScreenMessage(" You have an over enchanted item, you will be kicked from server! ", 6000));
							Util.handleIllegalPlayerAction(activeChar, "Player " + activeChar.getName() + " has Overenchanted  item! Kicked! ", Config.DEFAULT_PUNISH);
							LOG.info("#### ATTENTION ####");
							LOG.info(i + " item has been removed from " + activeChar);
						}
					}
				}
			}
		}
		
		// Welcome to Lineage II
		sendPacket(new SystemMessage(SystemMessageId.WELCOME_TO_LINEAGE));
		
		SevenSigns.getInstance().sendCurrentPeriodMsg(activeChar);
		
		if (Config.ANNOUNCE_NEW_STYLE)
		{
			Announcements.getInstance().showAnnouncementsNewStyle(activeChar);
		}
		else
		{
			Announcements.getInstance().showAnnouncements(activeChar);
		}
		
		// Check for crowns
		CrownManager.getInstance().checkCrowns(activeChar);
		
		// Check player skills
		if (Config.CHECK_SKILLS_ON_ENTER && !Config.ALT_GAME_SKILL_LEARN)
		{
			activeChar.checkAllowedSkills();
		}
		
		PetitionManager.getInstance().checkPetitionMessages(activeChar);
		
		if (activeChar.getClanId() != 0 && activeChar.getClan() != null)
		{
			sendPacket(new PledgeShowMemberListAll(activeChar.getClan(), activeChar));
			sendPacket(new PledgeStatusChanged(activeChar.getClan()));
		}
		
		if (activeChar.isAlikeDead())
		{
			sendPacket(new Die(activeChar)); // No broadcast needed since the player will already spawn dead to others
			ExRedSky packet = new ExRedSky(777);
			sendPacket(packet);
			PlaySound death_music = new PlaySound(1, "nade", 0, 0, 0, 0, 0);
			sendPacket(death_music);
		}
		
		if (Config.ALLOW_WATER)
		{
			activeChar.checkWaterState();
		}
		
		if (Hero.getInstance().getHeroes() != null && Hero.getInstance().getHeroes().containsKey(activeChar.getObjectId()))
		{
			activeChar.setHero(true);
		}
		
		setPledgeClass(activeChar);
		notifyClanMembers(activeChar);
		notifySponsorOrApprentice(activeChar);
		
		activeChar.onPlayerEnter();
		
		if (Config.PCB_ENABLE)
		{
			activeChar.showPcBangWindow();
		}
		
		if (Config.ANNOUNCE_CASTLE_LORDS)
		{
			notifyCastleOwner(activeChar);
		}
		
		if (Olympiad.getInstance().playerInStadia(activeChar))
		{
			activeChar.teleToLocation(MapRegionTable.TeleportWhereType.Town);
			activeChar.sendMessage("You have been teleported to the nearest town due to you being in an Olympiad Stadium.");
		}
		
		if (DimensionalRiftManager.getInstance().checkIfInRiftZone(activeChar.getX(), activeChar.getY(), activeChar.getZ(), false))
		{
			DimensionalRiftManager.getInstance().teleportToWaitingRoom(activeChar);
		}
		
		if (activeChar.getClanJoinExpiryTime() > System.currentTimeMillis())
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.CLAN_MEMBERSHIP_TERMINATED));
		}
		
		if (activeChar.getClan() != null)
		{
			activeChar.sendPacket(new PledgeSkillList(activeChar.getClan()));
			for (Siege siege : SiegeManager.getInstance().getSieges())
			{
				if (!siege.getIsInProgress())
				{
					continue;
				}
				
				if (siege.checkIsAttacker(activeChar.getClan()))
				{
					activeChar.setSiegeState((byte) 1);
					break;
				}
				else if (siege.checkIsDefender(activeChar.getClan()))
				{
					activeChar.setSiegeState((byte) 2);
					break;
				}
			}
			
			for (FortSiege fortsiege : FortSiegeManager.getInstance().getSieges())
			{
				if (!fortsiege.getIsInProgress())
				{
					continue;
				}
				
				if (fortsiege.checkIsAttacker(activeChar.getClan()))
				{
					activeChar.setSiegeState((byte) 1);
					break;
				}
				else if (fortsiege.checkIsDefender(activeChar.getClan()))
				{
					activeChar.setSiegeState((byte) 2);
					break;
				}
			}
			
			// Add message at connexion if clanHall not paid. Possibly this is custom...
			ClanHall clanHall = ClanHallManager.getInstance().getClanHallByOwner(activeChar.getClan());
			if (clanHall != null)
			{
				if (!clanHall.getPaid())
				{
					activeChar.sendPacket(new SystemMessage(SystemMessageId.PAYMENT_FOR_YOUR_CLAN_HALL_HAS_NOT_BEEN_MADE_PLEASE_MAKE_PAYMENT_TO_YOUR_CLAN_WAREHOUSE_BY_S1_TOMORROW));
				}
			}
		}
		
		if (!activeChar.isGM() && activeChar.getSiegeState() < 2 && activeChar.isInsideZone(ZoneId.ZONE_SIEGE))
		{
			// Attacker or spectator logging in to a siege zone. Actually should be checked for inside castle only?
			activeChar.teleToLocation(MapRegionTable.TeleportWhereType.Town);
			activeChar.sendMessage("You have been teleported to the nearest town due to you being in siege zone.");
		}
		
		if (Config.REBIRTH_ENABLE)
		{
			CustomWorldHandler.getInstance().enterWorld(activeChar);
		}
		
		if (TvT._savePlayers.contains(activeChar.getName()))
		{
			TvT.addDisconnectedPlayer(activeChar);
		}
		
		if (CTF._savePlayers.contains(activeChar.getName()))
		{
			CTF.addDisconnectedPlayer(activeChar);
		}
		
		if (DM._savePlayers.contains(activeChar.getName()))
		{
			DM.addDisconnectedPlayer(activeChar);
		}
		
		if (Config.ALLOW_DUALBOX)
		{
			if (!activeChar.checkMultiBox())
			{
				activeChar.sendMessage("I'm sorry, but multibox is not allowed here.");
				activeChar.logout();
			}
		}
		
		activeChar.sendPacket(new ItemList(activeChar, false));
		activeChar.getMacroses().sendUpdate();
		activeChar.sendPacket(new ShortCutInit(activeChar));
		
		enterInstance(activeChar);
		
		if (Config.ALLOW_CLASS_MASTERS && Config.ALLOW_REMOTE_CLASS_MASTERS)
		{
			final L2ClassMasterInstance master_instance = L2ClassMasterInstance.getInstance();
			if (master_instance != null)
			{
				ClassLevel lvlnow = PlayerClass.values()[activeChar.getClassId().getId()].getLevel();
				
				if (activeChar.getLevel() >= 20 && lvlnow == ClassLevel.First)
				{
					L2ClassMasterInstance.getInstance().onTable(activeChar);
				}
				else if (activeChar.getLevel() >= 40 && lvlnow == ClassLevel.Second)
				{
					L2ClassMasterInstance.getInstance().onTable(activeChar);
				}
				else if (activeChar.getLevel() >= 76 && lvlnow == ClassLevel.Third)
				{
					L2ClassMasterInstance.getInstance().onTable(activeChar);
				}
			}
		}
		
		// Apply night/day bonus on skill Shadow Sense
		if (activeChar.getRace().ordinal() == 2)
		{
			final L2Skill skill = SkillTable.getInstance().getInfo(294, 1);
			if (skill != null && activeChar.getSkillLevel(294) == 1)
			{
				if (GameTimeController.getInstance().isNight())
				{
					SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.NIGHT_EFFECT_APPLIES);
					sm.addSkillName(294);
					sendPacket(sm);
				}
				else
				{
					SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.DAY_EFFECT_DISAPPEARS);
					sm.addSkillName(294);
					sendPacket(sm);
				}
			}
		}
		
		// NPCBuffer
		if (PowerPackConfig.BUFFER_ENABLED)
		{
			BuffTable.getInstance().onPlayerLogin(activeChar.getObjectId());
		}
		
		// Elrokian Trap like L2OFF
		L2ItemInstance rhand = activeChar.getInventory().getPaperdollItem(Inventory.PAPERDOLL_RHAND);
		if (rhand != null && rhand.getItemId() == 8763)
		{
			activeChar.addSkill(SkillTable.getInstance().getInfo(3626, 1));
			activeChar.addSkill(SkillTable.getInstance().getInfo(3627, 1));
			activeChar.addSkill(SkillTable.getInstance().getInfo(3628, 1));
		}
		else
		{
			activeChar.removeSkill(3626, true);
			activeChar.removeSkill(3627, true);
			activeChar.removeSkill(3628, true);
		}
		
		if (Config.TOMASZ_B_CUSTOM)
		{
			L2ItemInstance cloak = activeChar.getInventory().getPaperdollItem(Inventory.PAPERDOLL_UNDER);
			if (cloak != null && cloak.getItemId() == 10107)
			{
				activeChar.startAbnormalEffect(L2Character.ABNORMAL_EFFECT_FLAME);
			}
		}
		
		// Apply death penalty
		activeChar.restoreDeathPenaltyBuffLevel();
		
		Quest.playerEnter(activeChar);
		activeChar.sendPacket(new QuestList());
		
		if (Config.LOAD_TUTORIAL)
		{
			loadTutorial(activeChar);
		}
		
		ColorSystem(activeChar);
		
		activeChar.updatePunishState();
		// Custom status
		activeChar.restoreCustomStatus();
		
		activeChar.sendPacket(new ClientSetTime());
		activeChar.sendPacket(new HennaInfo(activeChar));
		activeChar.sendPacket(new FriendList(activeChar));
		
		activeChar.sendPacket(new ExStorageMaxCount(activeChar));
		activeChar.sendPacket(new EtcStatusUpdate(activeChar));
		
		// Send all skills to char
		activeChar.sendSkillList();
		
		// Reload inventory to give SA skill
		activeChar.getInventory().reloadEquippedItems();
		
		activeChar.sendPacket(new UserInfo(activeChar));
		
		// Close lock at login
		activeChar.SetIsEnteringToWorld(false);
		activeChar.setLocked(false);
		
		// Just in case to avoid stuck on enter to world
		activeChar.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	private boolean isValidName(String text)
	{
		boolean result = true;
		String test = text;
		Pattern pattern;
		
		try
		{
			pattern = Pattern.compile(Config.CNAME_TEMPLATE);
		}
		catch (PatternSyntaxException e) // case of illegal pattern
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			
			LOG.warn(getType() + ": character name pattern of config is wrong.");
			pattern = Pattern.compile(".*");
		}
		
		Matcher regexp = pattern.matcher(test);
		if (!regexp.matches())
		{
			result = false;
		}
		
		return result;
	}
	
	private void EnterGM(L2PcInstance activeChar)
	{
		if (activeChar.isGM())
		{
			if (Config.GM_SPECIAL_EFFECT)
			{
				activeChar.broadcastPacket(new Earthquake(activeChar.getX(), activeChar.getY(), activeChar.getZ(), 50, 4));
			}
			
			if (Config.SHOW_GM_LOGIN)
			{
				Announcements.getInstance().announceToAll("GM " + activeChar.getName() + " has logged on.");
			}
			
			if (Config.GM_STARTUP_INVULNERABLE && AdminCommandAccessRights.getInstance().hasAccess("admin_invul", activeChar.getAccessLevel()))
			{
				activeChar.setIsInvul(true);
			}
			
			if (Config.GM_STARTUP_INVISIBLE && AdminCommandAccessRights.getInstance().hasAccess("admin_invisible", activeChar.getAccessLevel()))
			{
				activeChar.getAppearance().setInvisible();
			}
			
			if (Config.GM_STARTUP_SILENCE && AdminCommandAccessRights.getInstance().hasAccess("admin_silence", activeChar.getAccessLevel()))
			{
				activeChar.setMessageRefusal(0);
			}
			
			if (Config.GM_STARTUP_AUTO_LIST && AdminCommandAccessRights.getInstance().hasAccess("admin_gmliston", activeChar.getAccessLevel()))
			{
				GmListTable.getInstance().addGm(activeChar, false);
			}
			else
			{
				GmListTable.getInstance().addGm(activeChar, true);
			}
			
			activeChar.updateGmNameTitleColor();
		}
	}
	
	private void enterInstance(L2PcInstance activeChar)
	{
		if (Config.ALT_SERVER_NAME_ENABLED)
		{
			activeChar.sendPacket(new ExShowScreenMessage(1, 3, 2, false, 0, 1, 1, false, 4000, true, Config.ALT_SERVER_TEXT));
		}
		
		if (activeChar.getFirstLog())
		{
			if (Config.NEW_PLAYER_EFFECT)
			{
				L2Skill skill = SkillTable.getInstance().getInfo(2025, 1);
				if (skill != null)
				{
					MagicSkillUser MSU = new MagicSkillUser(activeChar, activeChar, 2025, 1, 1, 0);
					activeChar.sendPacket(MSU);
					activeChar.broadcastPacket(MSU);
					activeChar.useMagic(skill, false, false);
				}
			}
			
			// Heal
			activeChar.getStatus().setCurrentHpMp(activeChar.getMaxHp(), activeChar.getMaxMp());
			activeChar.getStatus().setCurrentCp(activeChar.getMaxCp());
			
			if (activeChar.getInventory().getItemByItemId(1835) != null)
			{
				activeChar.addAutoSoulShot(1835);
				activeChar.rechargeAutoSoulShot(true, true, false);
				activeChar.sendPacket(new ExAutoSoulShot(1835, 1));
			}
			
			if (activeChar.getInventory().getItemByItemId(3947) != null)
			{
				activeChar.addAutoSoulShot(3947);
				activeChar.rechargeAutoSoulShot(true, true, false);
				activeChar.sendPacket(new ExAutoSoulShot(3947, 1));
			}
			
			// Potions
			if (activeChar.getInventory().getItemByItemId(5592) != null)
			{
				activeChar.sendPacket(new ExAutoSoulShot(5592, 1));
				activeChar.setAutoPot(5592, ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new AutoPot(5592, activeChar, Float.parseFloat("0.95")), 1000, CP_POT_CD * 1000), true);
			}
			if (activeChar.getInventory().getItemByItemId(1539) != null)
			{
				activeChar.sendPacket(new ExAutoSoulShot(1539, 1));
				activeChar.setAutoPot(1539, ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new AutoPot(1539, activeChar, Float.parseFloat("0.95")), 1000, HEALING_POT_CD * 1000), true);
			}
			if (activeChar.getInventory().getItemByItemId(728) != null)
			{
				activeChar.sendPacket(new ExAutoSoulShot(728, 1));
				activeChar.setAutoPot(728, ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new AutoPot(728, activeChar, Float.parseFloat("0.70")), 1000, MANA_POT_CD * 1000), true);
			}
			
			activeChar.setFirstLog(false);
			activeChar.updateFirstLog();
		}
		
		if (Config.WELCOME_HTM && isValidName(activeChar.getName()))
		{
			String Welcome_Path = "data/html/welcome.htm";
			File mainText = new File(Config.DATAPACK_ROOT, Welcome_Path);
			if (mainText.exists())
			{
				NpcHtmlMessage html = new NpcHtmlMessage(1);
				html.setFile(Welcome_Path);
				html.replace("%name%", activeChar.getName());
				sendPacket(html);
			}
		}
		
		if ((activeChar.getClan() != null) && activeChar.getClan().isNoticeEnabled())
		{
			String clanNotice = "data/html/clanNotice.htm";
			File mainText = new File(Config.DATAPACK_ROOT, clanNotice);
			if (mainText.exists())
			{
				NpcHtmlMessage html = new NpcHtmlMessage(1);
				html.setFile(clanNotice);
				html.replace("%clan_name%", activeChar.getClan().getName());
				html.replace("%notice_text%", activeChar.getClan().getNotice().replaceAll("\r\n", "<br>"));
				sendPacket(html);
			}
		}
		
		if (Config.PM_MESSAGE_ON_START)
		{
			activeChar.sendPacket(new CreatureSay(2, Say2.HERO_VOICE, Config.PM_TEXT1, Config.PM_SERVER_NAME));
			activeChar.sendPacket(new CreatureSay(15, Say2.PARTYROOM_COMMANDER, activeChar.getName(), Config.PM_TEXT2));
		}
		
		if (Config.SERVER_TIME_ON_START)
		{
			int t = GameTimeController.getInstance().getGameTime();
			String h = "" + t / 60 % 24;
			String m;
			if (t % 60 < 10)
			{
				m = "0" + t % 60;
			}
			else
			{
				m = "" + t % 60;
			}
			
			activeChar.sendMessage("--------------------------------------------------------------------------------");
			activeChar.sendMessage("Server date: " + df.format(new Date(System.currentTimeMillis())));
			activeChar.sendMessage("Server time: " + fmt.format(new Date(System.currentTimeMillis())));
			activeChar.sendMessage("Server re-started: " + GameServer.dateTimeServerRestarted + "");
			activeChar.sendMessage("");
			activeChar.sendMessage(GameTimeController.getInstance().isNight() ? "Game time: " + h + ":" + m + " in the night." : "Game time: " + h + ":" + m + " in the day.");
			activeChar.sendMessage("--------------------------------------------------------------------------------");
		}
		
		if (MailBBSManager.getInstance().checkUnreadMail(activeChar) > 0)
		{
			activeChar.sendPacket(SystemMessageId.NEW_MAIL);
			activeChar.sendPacket(new PlaySound("systemmsg_e.1233"));
			activeChar.sendPacket(ExMailArrived.STATIC_PACKET);
		}
		
		if (Config.ALLOW_PREMIUM_ON_START && CharNameTable.getInstance().accountCharNumber(getClient().getAccountName()) == 1 && activeChar.getLevel() == 1)
		{
			if (activeChar.getPremiumService() == 0)
			{
				activeChar.setPremiumService(1);
				updateDatabase(activeChar, Config.HOW_MANY_DAYS * 24L * 60L * 60L * 1000L);
				if (Config.PREMIUM_NAME_COLOR_ENABLED && activeChar.getPremiumService() == 1)
				{
					activeChar.getAppearance().setTitleColor(Config.PREMIUM_TITLE_COLOR);
				}
				activeChar.sendMessage("Congratulations! You're The Premium account now.");
			}
		}
		
		if (Config.ALLOW_NOBLE_ON_START && CharNameTable.getInstance().accountCharNumber(getClient().getAccountName()) == 1 && activeChar.getLevel() == 1)
		{
			if (!(activeChar.isNoble()))
			{
				activeChar.setNoble(true);
				activeChar.sendMessage("Congratulations! You've got The Noblesse status.");
				PlaySound playSound = new PlaySound("ItemSound.quest_fanfare_1");
				activeChar.sendPacket(playSound);
				
				L2ItemInstance newitem = activeChar.getInventory().addItem("Tiara", 7694, 1, activeChar, null);
				InventoryUpdate playerIU = new InventoryUpdate();
				playerIU.addItem(newitem);
				activeChar.sendPacket(playerIU);
				SystemMessage sm;
				sm = new SystemMessage(SystemMessageId.EARNED_ITEM);
				sm.addItemName(7694);
				activeChar.sendPacket(sm);
			}
		}
		
		if (Config.POP_UP_VOTE_MENU && activeChar.getClient() != null)
		{
			try
			{
				String playerIP = activeChar.getClient().getConnection().getInetAddress().getHostAddress();
				Vote.restoreVotedData(activeChar, playerIP);
			}
			catch (Exception e)
			{
				LOG.warn("EnterWorld: Could not restore vote data for player: " + activeChar.getName() + " IP: " + activeChar.getClient().getConnection().getInetAddress().getHostAddress() + "." + e);
			}
			
			if (activeChar.eligibleToVoteHop() || activeChar.eligibleToVoteTop() || activeChar.eligibleToVoteNet() || activeChar.eligibleToVoteBra())
			{
				Vote.showHtm(activeChar);
			}
		}
		
		if (Config.BETASERVER && !activeChar.isGM())
		{
			activeChar.addSkill(SkillTable.getInstance().getInfo(7029, 4), true);
			activeChar.sendMessage("Super Haste Lv 4 added for better testing.");
		}
		
		if (Config.ONLINE_PLAYERS_ON_LOGIN)
		{
			activeChar.sendPacket(new ExShowScreenMessage(1, 11111, 3, false, 0, 0, 0, true, 6000, false, " Players online: " + L2World.getInstance().getAllPlayersCount()));
			activeChar.sendMessage("--------------------------------------------------------------------------------");
			activeChar.sendMessage("Players online: " + L2World.getInstance().getAllPlayersCount());
			activeChar.sendMessage("--------------------------------------------------------------------------------");
		}
	}
	
	private void updateDatabase(L2PcInstance player, long premiumTime)
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
		}
		catch (Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			
			LOG.warn("Error: could not update database: ", e);
		}
		finally
		{
			CloseUtil.close(con);
		}
	}
	
	private void ColorSystem(L2PcInstance activeChar)
	{
		if (activeChar.getPvpKills() >= Config.PVP_AMOUNT1 && Config.PVP_COLOR_SYSTEM_ENABLED)
		{
			activeChar.updatePvPColor(activeChar.getPvpKills());
		}
		if (activeChar.getPkKills() >= Config.PK_AMOUNT1 && Config.PK_COLOR_SYSTEM_ENABLED)
		{
			activeChar.updatePkColor(activeChar.getPkKills());
		}
		
		if (activeChar.getPvpKills() >= Config.PVP_AMOUNT2 && Config.PVP_COLOR_SYSTEM_ENABLED)
		{
			activeChar.updatePvPColor(activeChar.getPvpKills());
		}
		if (activeChar.getPkKills() >= Config.PK_AMOUNT2 && Config.PK_COLOR_SYSTEM_ENABLED)
		{
			activeChar.updatePkColor(activeChar.getPkKills());
		}
		
		if (activeChar.getPvpKills() >= Config.PVP_AMOUNT3 && Config.PVP_COLOR_SYSTEM_ENABLED)
		{
			activeChar.updatePvPColor(activeChar.getPvpKills());
		}
		if (activeChar.getPkKills() >= Config.PK_AMOUNT3 && Config.PK_COLOR_SYSTEM_ENABLED)
		{
			activeChar.updatePkColor(activeChar.getPkKills());
		}
		
		if (activeChar.getPvpKills() >= Config.PVP_AMOUNT4 && Config.PVP_COLOR_SYSTEM_ENABLED)
		{
			activeChar.updatePvPColor(activeChar.getPvpKills());
		}
		if (activeChar.getPkKills() >= Config.PK_AMOUNT4 && Config.PK_COLOR_SYSTEM_ENABLED)
		{
			activeChar.updatePkColor(activeChar.getPkKills());
		}
		
		if (activeChar.getPvpKills() >= Config.PVP_AMOUNT5 && Config.PVP_COLOR_SYSTEM_ENABLED)
		{
			activeChar.updatePvPColor(activeChar.getPvpKills());
		}
		if (activeChar.getPkKills() >= Config.PK_AMOUNT5 && Config.PK_COLOR_SYSTEM_ENABLED)
		{
			activeChar.updatePkColor(activeChar.getPkKills());
		}
		
		// Apply color settings to clan leader when entering
		if (activeChar.getClan() != null && activeChar.isClanLeader() && Config.CLAN_LEADER_COLOR_ENABLED && activeChar.getClan().getLevel() >= Config.CLAN_LEADER_COLOR_CLAN_LEVEL)
		{
			if (Config.CLAN_LEADER_COLORED == 1)
			{
				activeChar.getAppearance().setNameColor(Config.CLAN_LEADER_COLOR);
			}
			else
			{
				activeChar.getAppearance().setTitleColor(Config.CLAN_LEADER_COLOR);
			}
		}
		
		activeChar.updateNameTitleColor();
	}
	
	private void engage(L2PcInstance cha)
	{
		int _chaid = cha.getObjectId();
		
		for (Wedding cl : CoupleManager.getInstance().getCouples())
		{
			if (cl.getPlayer1Id() == _chaid || cl.getPlayer2Id() == _chaid)
			{
				if (cl.getMaried())
				{
					cha.setMarried(true);
					cha.setmarriedType(cl.getType());
				}
				
				cha.setCoupleId(cl.getId());
				
				if (cl.getPlayer1Id() == _chaid)
				{
					cha.setPartnerId(cl.getPlayer2Id());
				}
				else
				{
					cha.setPartnerId(cl.getPlayer1Id());
				}
			}
		}
	}
	
	private void notifyPartner(L2PcInstance cha, int partnerId)
	{
		if (cha.getPartnerId() != 0)
		{
			L2PcInstance partner = null;
			
			if (L2World.getInstance().findObject(cha.getPartnerId()) instanceof L2PcInstance)
			{
				partner = (L2PcInstance) L2World.getInstance().findObject(cha.getPartnerId());
			}
			
			if (partner != null)
			{
				partner.sendMessage("Your partner has logged in.");
			}
		}
	}
	
	private void notifyClanMembers(L2PcInstance activeChar)
	{
		L2Clan clan = activeChar.getClan();
		if (clan != null)
		{
			clan.getClanMember(activeChar.getObjectId()).setPlayerInstance(activeChar);
			clan.broadcastToOtherOnlineMembers(new SystemMessage(SystemMessageId.CLAN_MEMBER_S1_LOGGED_IN).addString(activeChar.getName()), activeChar);
			clan.broadcastToOtherOnlineMembers(new PledgeShowMemberListUpdate(activeChar), activeChar);
		}
	}
	
	private void notifySponsorOrApprentice(L2PcInstance activeChar)
	{
		if (activeChar.getSponsor() != 0)
		{
			L2PcInstance sponsor = (L2PcInstance) L2World.getInstance().findObject(activeChar.getSponsor());
			if (sponsor != null)
			{
				sponsor.sendPacket(new SystemMessage(SystemMessageId.YOUR_APPRENTICE_S1_HAS_LOGGED_IN).addString(activeChar.getName()));
			}
		}
		else if (activeChar.getApprentice() != 0)
		{
			L2PcInstance apprentice = (L2PcInstance) L2World.getInstance().findObject(activeChar.getApprentice());
			if (apprentice != null)
			{
				apprentice.sendPacket(new SystemMessage(SystemMessageId.YOUR_SPONSOR_S1_HAS_LOGGED_IN).addString(activeChar.getName()));
			}
		}
	}
	
	private void loadTutorial(L2PcInstance player)
	{
		QuestState qs = player.getQuestState("255_Tutorial");
		if (qs != null)
		{
			qs.getQuest().notifyEvent("UC", null, player);
		}
	}
	
	private void setPledgeClass(L2PcInstance activeChar)
	{
		int pledgeClass = 0;
		
		if (activeChar.getClan() != null)
		{
			pledgeClass = activeChar.getClan().getClanMember(activeChar.getObjectId()).calculatePledgeClass(activeChar);
		}
		
		if (activeChar.isNoble() && pledgeClass < 5)
		{
			pledgeClass = 5;
		}
		
		if (activeChar.isHero())
		{
			pledgeClass = 8;
		}
		
		activeChar.setPledgeClass(pledgeClass);
	}
	
	private void notifyCastleOwner(L2PcInstance activeChar)
	{
		L2Clan clan = activeChar.getClan();
		if (clan != null)
		{
			if (clan.getHasCastle() > 0)
			{
				Castle castle = CastleManager.getInstance().getCastleById(clan.getHasCastle());
				if ((castle != null) && (activeChar.getObjectId() == clan.getLeaderId()))
				{
					Announcements.getInstance().announceToAll("Lord " + activeChar.getName() + " Ruler Of " + castle.getName() + " Castle has logged in.");
				}
			}
		}
	}
	
	private class AutoPot implements Runnable
	{
		private int _id;
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
			}
			
			switch (_id)
			{
				case 728:
				{
					if (!_activeChar.isInvul() && (_activeChar.getInventory().getItemByItemId(728) != null) && _activeChar.getCurrentMp() < _pTime * _activeChar.getMaxMp())
					{
						MagicSkillUser msu = new MagicSkillUser(_activeChar, _activeChar, 2279, 2, 0, 100);
						_activeChar.broadcastPacket(msu);
						
						Potions is = new Potions();
						is.useItem(_activeChar, _activeChar.getInventory().getItemByItemId(728));
					}
					break;
				}
				case 1539:
				case 1540:
				case 1060:
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
			}
		}
	}
	
	@Override
	public String getType()
	{
		return "[C] 03 EnterWorld";
	}
}