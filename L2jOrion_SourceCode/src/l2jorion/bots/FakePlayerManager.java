package l2jorion.bots;

import java.util.List;
import java.util.stream.Collectors;

import l2jorion.Config;
import l2jorion.bots.ai.StanderAI;
import l2jorion.bots.ai.WalkerAI;
import l2jorion.bots.helpers.FakeHelpers;
import l2jorion.bots.model.FarmLocation;
import l2jorion.bots.model.WalkNode;
import l2jorion.bots.xml.botClanList;
import l2jorion.bots.xml.botFarm;
import l2jorion.bots.xml.botRandomWalk;
import l2jorion.game.datatables.sql.ClanTable;
import l2jorion.game.managers.CastleManager;
import l2jorion.game.model.L2Character;
import l2jorion.game.model.L2Clan;
import l2jorion.game.model.L2World;
import l2jorion.game.model.Location;
import l2jorion.game.model.actor.instance.L2MonsterInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.entity.siege.Castle;
import l2jorion.game.model.entity.siege.Siege;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.JoinPledge;
import l2jorion.game.network.serverpackets.PledgeShowInfoUpdate;
import l2jorion.game.network.serverpackets.PledgeShowMemberListAdd;
import l2jorion.game.network.serverpackets.PledgeShowMemberListAll;
import l2jorion.game.network.serverpackets.PledgeShowMemberListUpdate;
import l2jorion.game.network.serverpackets.StopMove;
import l2jorion.game.network.serverpackets.SystemMessage;
import l2jorion.game.network.serverpackets.ValidateLocation;
import l2jorion.game.taskmanager.RandomZoneTaskManager;
import l2jorion.game.thread.ThreadPoolManager;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;
import l2jorion.util.random.Rnd;

public enum FakePlayerManager
{
	INSTANCE;
	
	protected final Logger LOG = LoggerFactory.getLogger(FakePlayerManager.class);
	
	protected WalkNode _currentWalkNode;
	protected FarmLocation _currentFarmLoc;
	private int totalClans = botClanList.getInstance().getBotClanOptions().size();
	
	private FakePlayerManager()
	{
	}
	
	public void initialise()
	{
		FakePlayerNameManager.INSTANCE.initialise();
		FakePlayerTaskManager.INSTANCE.initialise();
		
		if (Config.BOTS_COUNT_NEWBIE_ZONES > 0)
		{
			loadBotsToNewbieZones();
		}
		
		if (Config.BOTS_COUNT_RANDOM_TOWNS > 0)
		{
			loadBotsToRandomTown();
		}
		
		if (Config.ALLOW_RANDOM_PVP_ZONE && Config.BOTS_COUNT_PVP_ZONE > 0)
		{
			loadBotsToRandomPvpZone();
		}
		
		if (Config.BOTS_COUNT_FARM_ZONE > 0)
		{
			loadBotsToRandomFarmZone();
		}
	}
	
	public FakePlayer spawnPlayer(int x, int y, int z)
	{
		return spawnPlayer(x, y, z, 81, 3, false, false, false);
	}
	
	public FakePlayer spawnPlayer(int x, int y, int z, int level, int classNumber, boolean pvpZone, boolean walker, boolean farmer)
	{
		FakePlayer activeChar = FakeHelpers.createRandomFakePlayer(level, classNumber, pvpZone, farmer);
		
		L2World.getInstance().addPlayerToWorld(activeChar);
		
		if (pvpZone)
		{
			activeChar.setInstanceId(Config.PVP_ZONE_INSTANCE_ID);
		}
		
		// handlePlayerClanOnSpawn(activeChar);
		
		if (Config.PLAYER_SPAWN_PROTECTION > 0)
		{
			activeChar.setProtection(true);
		}
		
		if (level == 1)
		{
			switch (level)
			{
				case 1:
				{
					if (Config.CHAR_TITLE)
					{
						activeChar.setTitle(Config.ADD_CHAR_TITLE);
					}
					
					x = activeChar.getBaseTemplate().spawnX;
					y = activeChar.getBaseTemplate().spawnY;
					z = activeChar.getBaseTemplate().spawnZ;
				}
			}
		}
		
		activeChar.setHeading(Rnd.nextInt(61794));
		activeChar.broadcastPacket(new ValidateLocation(activeChar));
		activeChar.broadcastPacket(new StopMove(activeChar));
		activeChar.spawnMe(x, y, z);
		
		activeChar.onPlayerEnter();
		
		// if (!activeChar.isGM() && (!activeChar.isInSiege() || activeChar.getSiegeState() < 2) && activeChar.isInsideZone(ZoneId.ZONE_SIEGE))
		// {
		// activeChar.teleToLocation(TeleportWhereType.Town);
		// }
		
		activeChar.heal();
		
		if (walker)
		{
			if (Config.BOTS_WALKER_CHANCE_CREATE_CLAN > Rnd.get(100))
			{
				if (totalClans != 0)
				{
					createRandomClan(activeChar, totalClans);
					totalClans--;
				}
			}
			
			if (Config.BOTS_WALKER_CHANCE_JOIN_CLAN > Rnd.get(100))
			{
				if (ClanTable.getInstance().getBotClans() != null)
				{
					if (activeChar.getClan() == null)
					{
						joinClan(activeChar);
					}
				}
			}
		}
		
		if (farmer)
		{
			if (Config.BOTS_FARMER_CHANCE_CREATE_CLAN > Rnd.get(100))
			{
				if (totalClans != 0)
				{
					createRandomClan(activeChar, totalClans);
					totalClans--;
				}
			}
			
			if (Config.BOTS_FARMER_CHANCE_JOIN_CLAN > Rnd.get(100))
			{
				if (ClanTable.getInstance().getBotClans() != null)
				{
					if (activeChar.getClan() == null)
					{
						joinClan(activeChar);
					}
				}
			}
		}
		
		activeChar.setOnlineStatus(true);
		
		return activeChar;
	}
	
	public void loadBotsToNewbieZones()
	{
		new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				ThreadPoolManager.getInstance().scheduleAi(new loadBotsToNewbieZonestask(), 1000);
			}
		}).start();
	}
	
	private class loadBotsToNewbieZonestask implements Runnable
	{
		public loadBotsToNewbieZonestask()
		{
		}
		
		@Override
		public void run()
		{
			int LoadedBots = 0;
			
			LOG.info("BotEngine: spawning bots to Newbie Zones...");
			
			while (LoadedBots < Config.BOTS_COUNT_NEWBIE_ZONES)
			{
				FakePlayer fakePlayer = FakePlayerManager.INSTANCE.spawnPlayer(0, 0, 0, 1, 1, false, false, false);
				fakePlayer.setBotMode(1);
				fakePlayer.setTargetClass(L2MonsterInstance.class);
				fakePlayer.setTargetRange(300);
				fakePlayer.assignDefaultAI();
				
				try
				{
					Thread.sleep(Config.BOTS_NEWBIE_ZONES_SPAWN_DELAY);
				}
				catch (InterruptedException e)
				{
				}
				
				LoadedBots++;
			}
			
			LOG.info("BotEngine: spawned " + LoadedBots + " bots to Newbie Zones");
		}
	}
	
	public void loadBotsToRandomTown()
	{
		new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				ThreadPoolManager.getInstance().scheduleAi(new loadBotsToRandomTownTask(), 1000);
			}
		}).start();
	}
	
	private class loadBotsToRandomTownTask implements Runnable
	{
		public loadBotsToRandomTownTask()
		{
		}
		
		@Override
		public void run()
		{
			int LoadedBots = 0;
			int x = 0, y = 0;
			LOG.info("BotEngine: spawning bots to random town...");
			
			while (LoadedBots < Config.BOTS_COUNT_RANDOM_TOWNS)
			{
				int rndLevel = 81;
				int rndClassId = 3;
				
				if (Config.BOTS_CHANCE_FOR_NEWBIE_WALK > Rnd.get(100))
				{
					rndLevel = 1;
					rndClassId = 1;
				}
				
				int townId = Rnd.get(1, botRandomWalk.getInstance().getLastTownId());
				
				_currentWalkNode = (WalkNode) botRandomWalk.getInstance().getWalkNode(townId).toArray()[Rnd.get(0, botRandomWalk.getInstance().getWalkNode(townId).size() - 1)];
				
				x = _currentWalkNode.getX();
				y = _currentWalkNode.getY();
				
				x += Rnd.get(-Config.BOTS_RANDOM_MAX_OFFSET, Config.BOTS_RANDOM_MAX_OFFSET);
				y += Rnd.get(-Config.BOTS_RANDOM_MAX_OFFSET, Config.BOTS_RANDOM_MAX_OFFSET);
				
				FakePlayer fakePlayer = FakePlayerManager.INSTANCE.spawnPlayer(x, y, _currentWalkNode.getZ(), rndLevel, rndClassId, false, true, false);
				fakePlayer.setBotMode(2);
				
				if (Config.BOTS_CHANCE_FOR_WALK > Rnd.get(100))
				{
					fakePlayer.setTownId(townId);
					fakePlayer.setFakeAi(new WalkerAI(fakePlayer));
				}
				else
				{
					fakePlayer.setFakeAi(new StanderAI(fakePlayer));
				}
				
				try
				{
					Thread.sleep(Config.BOTS_RANDOM_TOWNS_SPAWN_DELAY);
				}
				catch (InterruptedException e)
				{
				}
				
				LoadedBots++;
			}
			
			LOG.info("BotEngine: spawned " + LoadedBots + " bots to random town");
		}
	}
	
	public void loadBotsToRandomPvpZone()
	{
		new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				ThreadPoolManager.getInstance().scheduleAi(new loadBotsToRandomPvpZoneTask(), 1000);
			}
		}).start();
	}
	
	private class loadBotsToRandomPvpZoneTask implements Runnable
	{
		public loadBotsToRandomPvpZoneTask()
		{
		}
		
		@Override
		public void run()
		{
			int LoadedBots = 0;
			
			LOG.info("BotEngine: spawning bots to PvP Zone...");
			
			while (LoadedBots < Config.BOTS_COUNT_PVP_ZONE)
			{
				Location location = RandomZoneTaskManager.getInstance().getCurrentZone().getLoc();
				FakePlayer fakePlayer = FakePlayerManager.INSTANCE.spawnPlayer(location.getX(), location.getY(), location.getZ(), 81, 3, true, false, false);
				fakePlayer.setBotMode(3);
				fakePlayer.setTargetClass(L2PcInstance.class);
				fakePlayer.setTargetRange(Config.BOTS_PVP_ZONE_ATTACK_RANGE);
				fakePlayer.assignDefaultAI();
				
				try
				{
					Thread.sleep(Config.BOTS_PVP_ZONE_SPAWN_DELAY);
				}
				catch (InterruptedException e)
				{
				}
				
				LoadedBots++;
			}
			
			LOG.info("BotEngine: spawned " + LoadedBots + " bots to PvP Zone");
		}
	}
	
	public void loadBotsToRandomFarmZone()
	{
		new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				ThreadPoolManager.getInstance().scheduleAi(new loadBotsToRandomFarmZoneTask(), 1000);
			}
		}).start();
	}
	
	private class loadBotsToRandomFarmZoneTask implements Runnable
	{
		public loadBotsToRandomFarmZoneTask()
		{
		}
		
		@Override
		public void run()
		{
			int LoadedBots = 0;
			
			LOG.info("BotEngine: spawning bots to random farm zone...");
			
			while (LoadedBots < Config.BOTS_COUNT_FARM_ZONE)
			{
				int zoneId = Rnd.get(1, botFarm.getInstance().getLastZoneId());
				int rndOffsetX = Rnd.get(-Config.RESPAWN_RANDOM_MAX_OFFSET, Config.RESPAWN_RANDOM_MAX_OFFSET);
				int rndOffsetY = Rnd.get(-Config.RESPAWN_RANDOM_MAX_OFFSET, Config.RESPAWN_RANDOM_MAX_OFFSET);
				_currentFarmLoc = (FarmLocation) botFarm.getInstance().getFarmNode(zoneId).toArray()[Rnd.get(0, botFarm.getInstance().getFarmNode(zoneId).size() - 1)];
				FakePlayer fakePlayer = FakePlayerManager.INSTANCE.spawnPlayer(_currentFarmLoc.getX() + rndOffsetX, _currentFarmLoc.getY() + rndOffsetY, _currentFarmLoc.getZ(), 81, 3, false, false, true);
				fakePlayer.setDistance(_currentFarmLoc);
				
				if (_currentFarmLoc.getType().equals("peace"))
				{
					fakePlayer.setBotMode(5);
				}
				else
				{
					fakePlayer.setBotMode(4);
				}
				
				fakePlayer.setZoneId(zoneId);
				fakePlayer.setTargetClass(L2Character.class);
				fakePlayer.setTargetRange(300);
				fakePlayer.setMaxTargetRange(3500);
				fakePlayer.assignDefaultAI();
				
				try
				{
					Thread.sleep(Config.BOTS_FARM_ZONE_SPAWN_DELAY);
				}
				catch (InterruptedException e)
				{
				}
				
				LoadedBots++;
			}
			
			LOG.info("BotEngine: spawned " + LoadedBots + " bots to random farm zone");
		}
	}
	
	public void despawnFakePlayer(int objectId)
	{
		L2PcInstance player = L2World.getInstance().getPlayer(objectId);
		if (player instanceof FakePlayer)
		{
			FakePlayer fakePlayer = (FakePlayer) player;
			fakePlayer.despawnPlayer();
		}
	}
	
	public static void handlePlayerClanOnSpawn(FakePlayer activeChar)
	{
		final L2Clan clan = activeChar.getClan();
		if (clan != null)
		{
			clan.getClanMember(activeChar.getObjectId()).setPlayerInstance(activeChar);
			
			final SystemMessage msg = SystemMessage.getSystemMessage(SystemMessageId.CLAN_MEMBER_S1_LOGGED_IN).addCharName(activeChar);
			final PledgeShowMemberListUpdate update = new PledgeShowMemberListUpdate(activeChar);
			
			// Send packets to others members.
			for (L2PcInstance member : clan.getOnlineMembers(""))
			{
				if (member == activeChar)
				{
					continue;
				}
				
				member.sendPacket(msg);
				member.sendPacket(update);
			}
			
			for (Castle castle : CastleManager.getInstance().getCastles())
			{
				final Siege siege = castle.getSiege();
				if (!siege.getIsInProgress())
				{
					continue;
				}
				
				if (siege.checkIsAttacker(activeChar.getClan()))
				{
					activeChar.setSiegeState((byte) 1);
				}
				else if (siege.checkIsDefender(activeChar.getClan()))
				{
					activeChar.setSiegeState((byte) 2);
				}
			}
		}
	}
	
	public static void createRandomClan(FakePlayer activeChar, int clanId)
	{
		String clanName = botClanList.getInstance().getBotClan(clanId).getClanName();
		ClanTable.getInstance().createClan(activeChar, clanName, false);
		if (activeChar != null && activeChar.getClan() != null)
		{
			activeChar.getClan().changeLevel(8);
			activeChar.getClan().setCrestId(botClanList.getInstance().getBotClan(clanId).getCrestId());
			activeChar.getClan().setHasCrest(true);
		}
		
	}
	
	public static void joinClan(FakePlayer activeChar)
	{
		final L2Clan[] clans = ClanTable.getInstance().getBotClans();
		L2Clan clan = clans[Rnd.get(0, ClanTable.getInstance().getBotClans().length - 1)];
		
		if (clan != null)
		{
			final JoinPledge jp = new JoinPledge(clan.getClanId());
			activeChar.sendPacket(jp);
			
			activeChar.setPowerGrade(6); // new member starts at 5, not confirmed
			clan.addClanMember(activeChar);
			activeChar.setClanPrivileges(activeChar.getClan().getRankPrivs(activeChar.getPowerGrade()));
			
			// activeChar.sendPacket(new SystemMessage(SystemMessageId.ENTERED_THE_CLAN));
			// final SystemMessage sm = new SystemMessage(SystemMessageId.S1_HAS_JOINED_CLAN);
			// sm.addString(activeChar.getName());
			// clan.broadcastToOnlineMembers(sm);
			
			clan.broadcastToOtherOnlineMembers(new PledgeShowMemberListAdd(activeChar), activeChar);
			clan.broadcastToOnlineMembers(new PledgeShowInfoUpdate(clan));
			
			// this activates the clan tab on the new member
			activeChar.sendPacket(new PledgeShowMemberListAll(clan, activeChar));
			activeChar.setClanJoinExpiryTime(0);
			activeChar.broadcastUserInfo();
		}
	}
	
	public int getFakePlayersCount()
	{
		return getFakePlayers().size();
	}
	
	public List<FakePlayer> getFakePlayers()
	{
		return L2World.getInstance().getPlayers().stream().filter(x -> x != null && x.isOnline() == 1 && x instanceof FakePlayer).map(x -> (FakePlayer) x).collect(Collectors.toList());
	}
}
