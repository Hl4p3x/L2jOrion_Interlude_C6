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
 * www.l2jorion.com
 */
package l2jorion.game;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.logging.LogManager;

import OrionGuard.ProtectionMain;
import l2jguard.Protection;
import l2jorion.Config;
import l2jorion.ConfigLoader;
import l2jorion.ServerType;
import l2jorion.game.ai.additional.Antharas;
import l2jorion.game.ai.additional.Baium;
import l2jorion.game.ai.additional.Barakiel;
import l2jorion.game.ai.additional.Benom;
import l2jorion.game.ai.additional.Core;
import l2jorion.game.ai.additional.FairyTrees;
import l2jorion.game.ai.additional.Frintezza;
import l2jorion.game.ai.additional.Frozen;
import l2jorion.game.ai.additional.Golkonda;
import l2jorion.game.ai.additional.Gordon;
import l2jorion.game.ai.additional.Hallate;
import l2jorion.game.ai.additional.IceFairySirra;
import l2jorion.game.ai.additional.InterludeTutorial;
import l2jorion.game.ai.additional.Kernon;
import l2jorion.game.ai.additional.Monastery;
import l2jorion.game.ai.additional.Orfen;
import l2jorion.game.ai.additional.QueenAnt;
import l2jorion.game.ai.additional.SummonMinions;
import l2jorion.game.ai.additional.Transform;
import l2jorion.game.ai.additional.Valakas;
import l2jorion.game.ai.additional.VanHalter;
import l2jorion.game.ai.additional.VarkaKetraAlly;
import l2jorion.game.ai.additional.Zaken;
import l2jorion.game.ai.additional.ZombieGatekeepers;
import l2jorion.game.ai.phantom.phantomPlayers;
import l2jorion.game.boat.routes.BoatGiranTalking;
import l2jorion.game.boat.routes.BoatGludinRune;
import l2jorion.game.boat.routes.BoatInnadrilTour;
import l2jorion.game.boat.routes.BoatRunePrimeval;
import l2jorion.game.boat.routes.BoatTalkingGludin;
import l2jorion.game.cache.CrestCache;
import l2jorion.game.cache.HtmCache;
import l2jorion.game.community.manager.ForumsBBSManager;
import l2jorion.game.controllers.GameTimeController;
import l2jorion.game.controllers.RecipeController;
import l2jorion.game.controllers.TradeController;
import l2jorion.game.datatables.GmListTable;
import l2jorion.game.datatables.HeroSkillTable;
import l2jorion.game.datatables.NobleSkillTable;
import l2jorion.game.datatables.OfflineTradeTable;
import l2jorion.game.datatables.SkillTable;
import l2jorion.game.datatables.csv.DoorTable;
import l2jorion.game.datatables.csv.ExtractableItemsData;
import l2jorion.game.datatables.csv.FishTable;
import l2jorion.game.datatables.csv.HennaTable;
import l2jorion.game.datatables.csv.MapRegionTable;
import l2jorion.game.datatables.csv.NpcWalkerRoutesTable;
import l2jorion.game.datatables.csv.PetItemsData;
import l2jorion.game.datatables.csv.RecipeTable;
import l2jorion.game.datatables.csv.StaticObjects;
import l2jorion.game.datatables.sql.AccessLevels;
import l2jorion.game.datatables.sql.AdminCommandAccessRights;
import l2jorion.game.datatables.sql.ArmorSetsTable;
import l2jorion.game.datatables.sql.CharNameTable;
import l2jorion.game.datatables.sql.CharTemplateTable;
import l2jorion.game.datatables.sql.ClanTable;
import l2jorion.game.datatables.sql.CustomArmorSetsTable;
import l2jorion.game.datatables.sql.HelperBuffTable;
import l2jorion.game.datatables.sql.HennaTreeTable;
import l2jorion.game.datatables.sql.ItemTable;
import l2jorion.game.datatables.sql.L2PetDataTable;
import l2jorion.game.datatables.sql.LevelUpData;
import l2jorion.game.datatables.sql.NpcTable;
import l2jorion.game.datatables.sql.SkillSpellbookTable;
import l2jorion.game.datatables.sql.SkillTreeTable;
import l2jorion.game.datatables.sql.TeleportLocationTable;
import l2jorion.game.datatables.xml.AugmentScrollData;
import l2jorion.game.datatables.xml.AugmentationData;
import l2jorion.game.datatables.xml.ExperienceData;
import l2jorion.game.geo.GeoData;
import l2jorion.game.geo.pathfinding.PathFinding;
import l2jorion.game.handler.AdminCommandHandler;
import l2jorion.game.handler.AutoAnnouncementHandler;
import l2jorion.game.handler.AutoChatHandler;
import l2jorion.game.handler.ItemHandler;
import l2jorion.game.handler.SkillHandler;
import l2jorion.game.handler.UserCommandHandler;
import l2jorion.game.handler.VoicedCommandHandler;
import l2jorion.game.idfactory.IdFactory;
import l2jorion.game.managers.AchievementManager;
import l2jorion.game.managers.AuctionManager;
import l2jorion.game.managers.AutoSaveManager;
import l2jorion.game.managers.CHSiegeManager;
import l2jorion.game.managers.CastleManager;
import l2jorion.game.managers.CastleManorManager;
import l2jorion.game.managers.ClanHallManager;
import l2jorion.game.managers.ClassDamageManager;
import l2jorion.game.managers.CoupleManager;
import l2jorion.game.managers.CrownManager;
import l2jorion.game.managers.CursedWeaponsManager;
import l2jorion.game.managers.DayNightSpawnManager;
import l2jorion.game.managers.DimensionalRiftManager;
import l2jorion.game.managers.DuelManager;
import l2jorion.game.managers.FortManager;
import l2jorion.game.managers.FortSiegeManager;
import l2jorion.game.managers.FourSepulchersManager;
import l2jorion.game.managers.GrandBossManager;
import l2jorion.game.managers.ItemsOnGroundManager;
import l2jorion.game.managers.MercTicketManager;
import l2jorion.game.managers.PetitionManager;
import l2jorion.game.managers.QuestManager;
import l2jorion.game.managers.RaidBossPointsManager;
import l2jorion.game.managers.RaidBossSpawnManager;
import l2jorion.game.managers.SiegeManager;
import l2jorion.game.managers.ZoneManager;
import l2jorion.game.model.L2Manor;
import l2jorion.game.model.L2World;
import l2jorion.game.model.PartyMatchRoomList;
import l2jorion.game.model.PartyMatchWaitingList;
import l2jorion.game.model.entity.Announcements;
import l2jorion.game.model.entity.Hero;
import l2jorion.game.model.entity.MonsterRace;
import l2jorion.game.model.entity.event.manager.EventManager;
import l2jorion.game.model.entity.event.tournament.Arena2x2;
import l2jorion.game.model.entity.event.tournament.Arena4x4;
import l2jorion.game.model.entity.event.tournament.Arena9x9;
import l2jorion.game.model.entity.sevensigns.SevenSigns;
import l2jorion.game.model.entity.sevensigns.SevenSignsFestival;
import l2jorion.game.model.entity.siege.hallsiege.halls.BanditStrongHold;
import l2jorion.game.model.entity.siege.hallsiege.halls.DevastatedCastle;
import l2jorion.game.model.entity.siege.hallsiege.halls.FortressOfResistance;
import l2jorion.game.model.entity.siege.hallsiege.halls.FortressOfTheDead;
import l2jorion.game.model.entity.siege.hallsiege.halls.RainbowSpringsChateau;
import l2jorion.game.model.entity.siege.hallsiege.halls.WildBeastReserve;
import l2jorion.game.model.multisell.L2Multisell;
import l2jorion.game.model.olympiad.Olympiad;
import l2jorion.game.model.olympiad.OlympiadGameManager;
import l2jorion.game.model.quest.Quest;
import l2jorion.game.model.spawn.AutoSpawn;
import l2jorion.game.network.L2GameClient;
import l2jorion.game.network.L2GamePacketHandler;
import l2jorion.game.powerpack.PowerPack;
import l2jorion.game.script.EventDroplist;
import l2jorion.game.script.faenor.FaenorScriptEngine;
import l2jorion.game.scripting.L2ScriptEngineManager;
import l2jorion.game.taskmanager.KnownListUpdateTaskManager;
import l2jorion.game.taskmanager.RandomZoneTaskManager;
import l2jorion.game.taskmanager.TaskManager;
import l2jorion.game.taskmanager.tasks.TaskItemDonate;
import l2jorion.game.templates.L2Item;
import l2jorion.game.thread.LoginServerThread;
import l2jorion.game.thread.ThreadPoolManager;
import l2jorion.game.thread.daemons.DeadLockDetector;
import l2jorion.game.thread.daemons.ItemsAutoDestroy;
import l2jorion.game.thread.daemons.PcPoint;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;
import l2jorion.mmocore.SelectorConfig;
import l2jorion.mmocore.SelectorThread;
import l2jorion.status.Status;
import l2jorion.util.IPv4Filter;
import l2jorion.util.Memory;
import l2jorion.util.Util;
import l2jorion.util.database.L2DatabaseFactory;

public class GameServer
{
	private static final Logger LOG = LoggerFactory.getLogger(GameServer.class);
	private static final String LOG_FOLDER = "log";
	
	private final SelectorThread<L2GameClient> _selectorThread;
	private final L2GamePacketHandler _gamePacketHandler;
	private final DeadLockDetector _deadDetectThread;
	
	public static GameServer gameServer;
	public static final Calendar dateTimeServerStarted = Calendar.getInstance();
	public static final String dateTimeServerRestarted = new SimpleDateFormat("dd MMMM, E, yyyy, H:mm:ss").format(new Date(System.currentTimeMillis()));
	
	public static void main(String[] args) throws Exception
	{
		ServerType.serverMode = ServerType.MODE_GAMESERVER;
		
		final File logFolderBase = new File(LOG_FOLDER);
		logFolderBase.mkdir();
		
		new File("log/game").mkdirs();
		
		try (InputStream is = new FileInputStream(new File(ConfigLoader.LOG_CONF_FILE)))
		{
			LogManager.getLogManager().readConfiguration(is);
		}
		
		Config.load();
		
		ThreadPoolManager.getInstance();
		
		L2ScriptEngineManager.getInstance();
		
		if (Config.L2JGUARD_PROTECTION)
		{
			Util.printSection("L2JGuard");
			Protection.Init();
		}
		
		L2DatabaseFactory.getInstance();
		
		gameServer = new GameServer();
		
		if (Config.IS_TELNET_ENABLED)
		{
			Util.printSection("Telnet");
			new Status(ServerType.serverMode).start();
		}
	}
	
	public GameServer() throws Exception
	{
		long serverLoadStart = System.currentTimeMillis();
		
		new File(Config.DATAPACK_ROOT, "data/crests").mkdirs();
		new File(Config.DATAPACK_ROOT, "data/faenor").mkdirs();
		new File(Config.DATAPACK_ROOT, "data/geodata").mkdirs();
		
		Util.printSection("World");
		HtmCache.getInstance();
		
		if (!IdFactory.getInstance().isInitialized())
		{
			LOG.info("Could not read object IDs from DB. Please Check Your Data.");
			throw new Exception("Could not initialize the ID factory");
		}
		
		GameTimeController.init();
		L2Item.LoadAllIcons();
		L2World.getInstance();
		MapRegionTable.getInstance();
		Announcements.getInstance();
		AutoAnnouncementHandler.getInstance();
		StaticObjects.getInstance();
		TeleportLocationTable.getInstance();
		PartyMatchWaitingList.getInstance();
		PartyMatchRoomList.getInstance();
		CharNameTable.getInstance();
		ExperienceData.getInstance();
		DuelManager.getInstance();
		
		if (Config.ENABLE_CLASS_DAMAGES)
		{
			ClassDamageManager.loadConfig();
		}
		
		if (Config.AUTOSAVE_INITIAL_TIME > 0)
		{
			AutoSaveManager.getInstance().startAutoSaveManager();
		}
		
		if (Config.COMMUNITY_TYPE.equals("full"))
		{
			ForumsBBSManager.getInstance().initRoot();
		}
		
		Util.printSection("Skills");
		if (!SkillTable.getInstance().isInitialized())
		{
			LOG.info("Could not find the extraced files. Please check your data.");
			throw new Exception("Could not initialize the skill table");
		}
		
		SkillTreeTable.getInstance();
		SkillSpellbookTable.getInstance();
		NobleSkillTable.getInstance();
		HeroSkillTable.getInstance();
		
		Util.printSection("Items");
		if (!ItemTable.getInstance().isInitialized())
		{
			LOG.info("Could not find the extraced files. Please check your data.");
			throw new Exception("Could not initialize the item table");
		}
		ArmorSetsTable.getInstance();
		if (Config.CUSTOM_ARMORSETS_TABLE)
		{
			CustomArmorSetsTable.getInstance();
		}
		ExtractableItemsData.getInstance();
		PetItemsData.getInstance();
		if (Config.ALLOWFISHING)
		{
			FishTable.getInstance();
		}
		
		TradeController.getInstance();
		L2Multisell.getInstance();
		
		AugmentScrollData.getInstance();
		
		Util.printSection("Npcs");
		NpcWalkerRoutesTable.getInstance().load();
		if (!NpcTable.getInstance().isInitialized())
		{
			LOG.info("Could not find the extraced files. Please check your data.");
			throw new Exception("Could not initialize the npc table");
		}
		
		CharTemplateTable.getInstance();
		LevelUpData.getInstance();
		
		if (!HennaTable.getInstance().isInitialized())
		{
			throw new Exception("Could not initialize the Henna Table");
		}
		
		if (!HennaTreeTable.getInstance().isInitialized())
		{
			throw new Exception("Could not initialize the Henna Tree Table");
		}
		
		if (!HelperBuffTable.getInstance().isInitialized())
		{
			throw new Exception("Could not initialize the Helper Buff Table");
		}
		
		Util.printSection("Geodata");
		if (Config.GEODATA)
		{
			GeoData.getInstance();
			PathFinding.getInstance();
		}
		else
		{
			LOG.info("Geodata: Disabled");
		}
		
		Util.printSection("Clans");
		ClanTable.getInstance();
		CrestCache.getInstance();
		
		Util.printSection("Clan Halls");
		CHSiegeManager.getInstance();
		ClanHallManager.getInstance();
		AuctionManager.getInstance();
		
		Util.printSection("Doors");
		DoorTable.getInstance();
		
		Util.printSection("Zones");
		ZoneManager.getInstance();
		CastleManager.getInstance();
		SiegeManager.getInstance();
		FortManager.getInstance();
		FortSiegeManager.getInstance();
		CrownManager.getInstance();
		
		if (!Config.ALT_DEV_NO_RB)
		{
			RaidBossSpawnManager.getInstance().load();
			GrandBossManager.getInstance().init();
			GrandBossManager.getInstance().initZones();
			RaidBossPointsManager.init();
		}
		else
		{
			LOG.info("Bosses: disabled");
		}
		
		DayNightSpawnManager.getInstance().notifyChangeMode();
		
		Util.printSection("Dimensional Rift");
		DimensionalRiftManager.getInstance();
		
		Util.printSection("Misc");
		RecipeTable.getInstance();
		RecipeController.getInstance();
		EventDroplist.getInstance();
		AugmentationData.getInstance();
		MonsterRace.getInstance();
		MercTicketManager.getInstance();
		PetitionManager.getInstance();
		CursedWeaponsManager.getInstance();
		TaskManager.getInstance();
		L2PetDataTable.getInstance().loadPetsData();
		
		if (Config.SAVE_DROPPED_ITEM)
		{
			ItemsOnGroundManager.getInstance();
		}
		
		if (Config.AUTODESTROY_ITEM_AFTER > 0 || Config.HERB_AUTO_DESTROY_TIME > 0)
		{
			ItemsAutoDestroy.getInstance();
		}
		
		if (Config.ALLOW_BOAT)
		{
			// Boats
			BoatGiranTalking.load();
			BoatGludinRune.load();
			BoatInnadrilTour.load();
			BoatRunePrimeval.load();
			BoatTalkingGludin.load();
		}
		
		Util.printSection("Manor");
		L2Manor.getInstance();
		CastleManorManager.getInstance();
		
		Util.printSection("4 Sepulchers");
		FourSepulchersManager.getInstance().load();
		
		Util.printSection("7 Signs");
		SevenSigns.getInstance();
		SevenSignsFestival.getInstance();
		AutoSpawn.getInstance();
		AutoChatHandler.getInstance();
		
		Util.printSection("Clan Hall Sieges");
		DevastatedCastle.load();
		FortressOfResistance.load();
		FortressOfTheDead.load();
		RainbowSpringsChateau.load();
		
		BanditStrongHold.load();
		// CompetitionfortheBanditStronghold.load();
		
		WildBeastReserve.load();
		
		Util.printSection("Olympiad & Heros");
		OlympiadGameManager.getInstance();
		Olympiad.getInstance();
		Hero.getInstance();
		
		Util.printSection("Access Levels");
		AccessLevels.getInstance();
		AdminCommandAccessRights.getInstance();
		GmListTable.getInstance();
		
		Util.printSection("Handlers");
		ItemHandler.getInstance();
		SkillHandler.getInstance();
		AdminCommandHandler.getInstance();
		UserCommandHandler.getInstance();
		VoicedCommandHandler.getInstance();
		
		LOG.info("AutoChatHandler: Loaded " + AutoChatHandler.getInstance().size() + " handlers");
		LOG.info("AutoSpawnHandler: Loaded " + AutoSpawn.getInstance().size() + " handlers");
		
		Util.printSection("Scripts");
		QuestManager.getInstance();
		
		// Donate Items
		TaskItemDonate.getInstance();
		
		if (!Config.ALT_DEV_NO_AI)
		{
			ThreadPoolManager.getInstance().scheduleAi(new Antharas(-1, "antharas", "ai"), 0);
			ThreadPoolManager.getInstance().scheduleAi(new Baium(-1, "baium", "ai"), 0);
			ThreadPoolManager.getInstance().scheduleAi(new Core(-1, "core", "ai"), 0);
			ThreadPoolManager.getInstance().scheduleAi(new QueenAnt(-1, "queen_ant", "ai"), 0);
			ThreadPoolManager.getInstance().scheduleAi(new VanHalter(-1, "vanhalter", "ai"), 0);
			ThreadPoolManager.getInstance().scheduleAi(new Gordon(-1, "Gordon", "ai"), 0);
			ThreadPoolManager.getInstance().scheduleAi(new Monastery(-1, "monastery", "ai"), 0);
			ThreadPoolManager.getInstance().scheduleAi(new Transform(-1, "transform", "ai"), 0);
			ThreadPoolManager.getInstance().scheduleAi(new FairyTrees(-1, "FairyTrees", "ai"), 0);
			ThreadPoolManager.getInstance().scheduleAi(new SummonMinions(-1, "SummonMinions", "ai"), 0);
			ThreadPoolManager.getInstance().scheduleAi(new ZombieGatekeepers(-1, "ZombieGatekeepers", "ai"), 0);
			ThreadPoolManager.getInstance().scheduleAi(new IceFairySirra(-1, "IceFairySirra", "ai"), 0);
			ThreadPoolManager.getInstance().scheduleAi(new Golkonda(-1, "Golkonda", "ai"), 0);
			ThreadPoolManager.getInstance().scheduleAi(new Hallate(-1, "Hallate", "ai"), 0);
			ThreadPoolManager.getInstance().scheduleAi(new Kernon(-1, "Kernon", "ai"), 0);
			ThreadPoolManager.getInstance().scheduleAi(new VarkaKetraAlly(-1, "Varka Ketra Ally", "ai"), 0);
			ThreadPoolManager.getInstance().scheduleAi(new Barakiel(-1, "Barakiel", "ai"), 0);
			ThreadPoolManager.getInstance().scheduleAi(new Orfen(-1, "Orfen", "ai"), 0);
			ThreadPoolManager.getInstance().scheduleAi(new Zaken(-1, "Zaken", "ai"), 0);
			ThreadPoolManager.getInstance().scheduleAi(new Frintezza(-1, "Frintezza", "ai"), 0);
			ThreadPoolManager.getInstance().scheduleAi(new Valakas(-1, "valakas", "ai"), 0);
			ThreadPoolManager.getInstance().scheduleAi(new Benom(-1, "Benom", "ai"), 0);
			ThreadPoolManager.getInstance().scheduleAi(new InterludeTutorial(-1, "Tutorial", "ai"), 0);
			ThreadPoolManager.getInstance().scheduleAi(new Frozen(-1, "Frozen", "ai"), 0);
			
			LOG.info("GameServer: Additional scripts loaded");
		}
		
		if (!Config.ALT_DEV_NO_SCRIPT)
		{
			try
			{
				L2ScriptEngineManager.getInstance().executeScriptList(new File(Config.DATAPACK_ROOT, "config/scripts.cfg"));
				FaenorScriptEngine.getInstance();
			}
			catch (IOException ioe)
			{
				LOG.error("{}: Failed loading scripts.cfg.", getClass().getSimpleName());
			}
			
			FaenorScriptEngine.getInstance();
			
			if (!Config.ALT_DEV_NO_QUESTS)
			{
				QuestManager.getInstance().report();
			}
		}
		else
		{
			LOG.info("Scripts: disabled");
		}
		
		Quest.LoadInit();
		
		Util.printSection("Mods");
		if (Config.ACHIEVEMENT_ENABLE)
		{
			AchievementManager.getInstance();
		}
		
		if (Config.L2JMOD_ALLOW_WEDDING)
		{
			CoupleManager.getInstance();
		}
		
		if (Config.PCB_ENABLE)
		{
			ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(PcPoint.getInstance(), Config.PCB_INTERVAL * 1000, Config.PCB_INTERVAL * 1000);
		}
		
		if (Config.POWERPAK_ENABLED)
		{
			PowerPack.getInstance();
		}
		
		EventManager.getInstance().startEventRegistration();
		
		if (EventManager.TVT_EVENT_ENABLED || EventManager.CTF_EVENT_ENABLED || EventManager.DM_EVENT_ENABLED)
		{
			if (EventManager.TVT_EVENT_ENABLED)
			{
				LOG.info("TVT Event - Enabled");
			}
			if (EventManager.CTF_EVENT_ENABLED)
			{
				LOG.info("CTF Event - Enabled");
			}
			if (EventManager.DM_EVENT_ENABLED)
			{
				LOG.info("DM Event - Enabled");
			}
		}
		
		// TournamentSpawner.getInstance();
		if (Config.ARENA_EVENT_ENABLED_2X2)
		{
			ThreadPoolManager.getInstance().scheduleGeneral(Arena2x2.getInstance(), 0);
		}
		
		if (Config.ARENA_EVENT_ENABLED_4X4)
		{
			ThreadPoolManager.getInstance().scheduleGeneral(Arena4x4.getInstance(), 0);
		}
		
		if (Config.ARENA_EVENT_ENABLED_9X9)
		{
			ThreadPoolManager.getInstance().scheduleGeneral(Arena9x9.getInstance(), 0);
		}
		
		Runtime.getRuntime().addShutdownHook(Shutdown.getInstance());
		
		Util.printSection("Offline trade");
		if ((Config.OFFLINE_TRADE_ENABLE || Config.OFFLINE_CRAFT_ENABLE) && Config.RESTORE_OFFLINERS)
		{
			OfflineTradeTable.restoreOfflineTraders();
		}
		
		Util.printSection("System");
		LOG.info("OS: " + Util.getOSName() + " (" + Util.getOSVersion() + ") " + Util.getOSArch());
		LOG.info("CPU: " + Util.getAvailableProcessors());
		LOG.info("Memory: " + Memory.getUsedMemory() + "/" + Memory.getTotalMemory() + " MB");
		
		if (Config.ALLOW_PHANTOM_PLAYERS)
		{
			Util.printSection("Phantom system");
			phantomPlayers.init();
		}
		
		KnownListUpdateTaskManager.getInstance();
		
		if (Config.ALLOW_RANDOM_PVP_ZONE)
		{
			RandomZoneTaskManager.getInstance();
		}
		
		if (Config.DEADLOCK_DETECTOR)
		{
			_deadDetectThread = new DeadLockDetector();
			_deadDetectThread.setDaemon(true);
			_deadDetectThread.start();
		}
		else
		{
			_deadDetectThread = null;
		}
		
		System.gc();
		
		Util.printSection("Status");
		LOG.info("Players limit: " + Config.MAXIMUM_ONLINE_USERS);
		LOG.info("Loaded in: " + (System.currentTimeMillis() - serverLoadStart) / 1000 + " seconds");
		
		LoginServerThread.getInstance().start();
		
		final SelectorConfig sc = new SelectorConfig();
		sc.MAX_READ_PER_PASS = Config.MMO_MAX_READ_PER_PASS;
		sc.MAX_SEND_PER_PASS = Config.MMO_MAX_SEND_PER_PASS;
		sc.SLEEP_TIME = Config.MMO_SELECTOR_SLEEP_TIME;
		sc.HELPER_BUFFER_COUNT = Config.MMO_HELPER_BUFFER_COUNT;
		sc.TCP_NODELAY = Config.MMO_TCP_NODELAY;
		
		_gamePacketHandler = new L2GamePacketHandler();
		_selectorThread = new SelectorThread<>(sc, _gamePacketHandler, _gamePacketHandler, _gamePacketHandler, new IPv4Filter());
		
		InetAddress bindAddress = null;
		if (!Config.GAMESERVER_HOSTNAME.equals("*"))
		{
			try
			{
				bindAddress = InetAddress.getByName(Config.GAMESERVER_HOSTNAME);
			}
			catch (UnknownHostException e1)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
				{
					e1.printStackTrace();
				}
				
				LOG.warn("The GameServer bind address is invalid, using all avaliable IPs. Reason: ", e1);
			}
		}
		
		try
		{
			_selectorThread.openServerSocket(bindAddress, Config.PORT_GAME);
			_selectorThread.start();
		}
		catch (IOException e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			
			LOG.error("Failed to open server socket. Reason: ", e);
			System.exit(1);
		}
	}
	
	public SelectorThread<L2GameClient> getSelectorThread()
	{
		return _selectorThread;
	}
	
	public L2GamePacketHandler getL2GamePacketHandler()
	{
		return _gamePacketHandler;
	}
	
	public DeadLockDetector getDeadLockDetectorThread()
	{
		return _deadDetectThread;
	}
}