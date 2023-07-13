package l2jorion.game.ai.additional.invidual;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import l2jorion.Config;
import l2jorion.game.managers.GrandBossManager;
import l2jorion.game.model.L2Attackable;
import l2jorion.game.model.actor.instance.L2GrandBossInstance;
import l2jorion.game.model.actor.instance.L2NpcInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.entity.Announcements;
import l2jorion.game.model.quest.Quest;
import l2jorion.game.network.serverpackets.CreatureSay;
import l2jorion.game.network.serverpackets.PlaySound;
import l2jorion.game.templates.StatsSet;
import l2jorion.log.Log;
import l2jorion.util.random.Rnd;

public class Core extends Quest implements Runnable
{
	private static final int CORE = 29006;
	private static final int DEATH_KNIGHT = 29007;
	private static final int DOOM_WRAITH = 29008;
	private static final int SUSCEPTOR = 29011;
	// CORE Status Tracking :
	private static final byte ALIVE = 0; // Core is spawned.
	private static final byte DEAD = 1; // Core has been killed.
	
	private static boolean _FirstAttacked;
	protected long _respawnEnd;
	private final SimpleDateFormat date = new SimpleDateFormat("H:mm:ss yyyy/MM/dd");
	
	List<L2Attackable> _minions = new CopyOnWriteArrayList<>();
	
	public Core(int id, String name, String descr)
	{
		super(id, name, descr);
		
		int[] mobs =
		{
			CORE,
			DEATH_KNIGHT,
			DOOM_WRAITH,
			SUSCEPTOR
		};
		
		for (int mob : mobs)
		{
			addEventId(mob, Quest.QuestEventType.ON_KILL);
			addEventId(mob, Quest.QuestEventType.ON_SPAWN);
			addEventId(mob, Quest.QuestEventType.ON_ATTACK);
		}
		
		_FirstAttacked = false;
		StatsSet info = GrandBossManager.getInstance().getStatsSet(CORE);
		
		Integer status = GrandBossManager.getInstance().getBossStatus(CORE);
		
		if (status == DEAD)
		{
			// load the unlock date and time for Core from DB
			long temp = info.getLong("respawn_time") - Calendar.getInstance().getTimeInMillis();
			if (temp > 0)
			{
				startQuestTimer("core_unlock", temp, null, null);
			}
			else
			{
				// the time has already expired while the server was offline. Immediately spawn Core.
				L2GrandBossInstance core = (L2GrandBossInstance) addSpawn(CORE, 17726, 108915, -6500, 0, false, 0);
				if (Config.ANNOUNCE_TO_ALL_SPAWN_RB)
				{
					Announcements.getInstance().announceWithServerName("The Grand boss " + core.getName() + " spawned in world.");
				}
				GrandBossManager.getInstance().setBossStatus(CORE, ALIVE);
				spawnBoss(core);
				
				if (Config.L2JMOD_CHAMPION_ENABLE)
				{
					if (Config.L2JMOD_CHAMP_RAID_BOSSES && Config.L2JMOD_CHAMPION_FREQUENCY > 0 && core.getLevel() >= Config.L2JMOD_CHAMP_MIN_LVL && core.getLevel() <= Config.L2JMOD_CHAMP_MAX_LVL)
					{
						core.setChampion(false);
						int random = Rnd.get(100);
						if (random <= Config.L2JMOD_CHAMPION_FREQUENCY)
						{
							core.setChampion(true);
						}
					}
				}
			}
		}
		else
		{
			String test = loadGlobalQuestVar("Core_Attacked");
			if (test.equalsIgnoreCase("true"))
			{
				_FirstAttacked = true;
			}
			
			L2GrandBossInstance core = (L2GrandBossInstance) addSpawn(CORE, 17726, 108915, -6500, 0, false, 0);
			if (Config.ANNOUNCE_TO_ALL_SPAWN_RB)
			{
				Announcements.getInstance().announceToAll("Raid boss " + core.getName() + " spawned in world.");
			}
			spawnBoss(core);
		}
	}
	
	@Override
	public void saveGlobalData()
	{
		String val = "" + _FirstAttacked;
		saveGlobalQuestVar("Core_Attacked", val);
	}
	
	@Override
	public String onAdvEvent(String event, L2NpcInstance npc, L2PcInstance player)
	{
		Integer status = GrandBossManager.getInstance().getBossStatus(CORE);
		
		if (event.equalsIgnoreCase("core_unlock"))
		{
			L2GrandBossInstance core = (L2GrandBossInstance) addSpawn(CORE, 17726, 108915, -6500, 0, false, 0);
			if (Config.ANNOUNCE_TO_ALL_SPAWN_RB)
			{
				Announcements.getInstance().announceToAll("Raid boss " + core.getName() + " spawned in world.");
			}
			GrandBossManager.getInstance().setBossStatus(CORE, ALIVE);
			spawnBoss(core);
		}
		else if (status == null)
		{
			LOG.warn("GrandBoss with Id " + CORE + " has not valid status into GrandBossManager");
			
		}
		else if (event.equalsIgnoreCase("spawn_minion") && status == ALIVE)
		{
			_minions.add((L2Attackable) addSpawn(npc.getNpcId(), npc.getX(), npc.getY(), npc.getZ(), npc.getHeading(), false, 0));
		}
		else if (event.equalsIgnoreCase("despawn_minions"))
		{
			for (int i = 0; i < _minions.size(); i++)
			{
				L2Attackable mob = _minions.get(i);
				if (mob != null)
				{
					mob.decayMe();
				}
			}
			_minions.clear();
		}
		return super.onAdvEvent(event, npc, player);
	}
	
	@Override
	public String onAttack(L2NpcInstance npc, L2PcInstance attacker, int damage, boolean isPet)
	{
		if (npc.getNpcId() == CORE)
		{
			if (_FirstAttacked)
			{
				if (Rnd.get(100) == 0)
				{
					npc.broadcastPacket(new CreatureSay(npc.getObjectId(), 0, npc.getName(), "Removing intruders."));
				}
			}
			else
			{
				_FirstAttacked = true;
				npc.broadcastPacket(new CreatureSay(npc.getObjectId(), 0, npc.getName(), "A non-permitted target has been discovered."));
				npc.broadcastPacket(new CreatureSay(npc.getObjectId(), 0, npc.getName(), "Starting intruder removal system."));
			}
		}
		return super.onAttack(npc, attacker, damage, isPet);
	}
	
	@Override
	public String onKill(L2NpcInstance npc, L2PcInstance killer, boolean isPet)
	{
		int npcId = npc.getNpcId();
		String name = npc.getName();
		if (npcId == CORE)
		{
			int objId = npc.getObjectId();
			npc.broadcastPacket(new PlaySound(1, "BS02_D", 1, objId, npc.getX(), npc.getY(), npc.getZ()));
			npc.broadcastPacket(new CreatureSay(objId, 0, name, "A fatal error has occurred."));
			npc.broadcastPacket(new CreatureSay(objId, 0, name, "System is being shut down..."));
			npc.broadcastPacket(new CreatureSay(objId, 0, name, "......"));
			_FirstAttacked = false;
			
			if (!npc.getSpawn().is_customBossInstance())
			{
				GrandBossManager.getInstance().setBossStatus(CORE, DEAD);
				addSpawn(31842, 16502, 110165, -6394, 0, false, 900000);
				addSpawn(31842, 18948, 110166, -6397, 0, false, 900000);
				
				long respawnTime = (Config.CORE_RESP_FIRST + (Config.CORE_RESP_SECOND == 0 ? 0 : Rnd.get(1, Config.CORE_RESP_SECOND))) * 3600000;
				final int days = Config.CORE_FIX_TIME_D * 24;
				
				Calendar time = Calendar.getInstance();
				time.add(Calendar.HOUR, days);
				time.set(Calendar.HOUR_OF_DAY, Config.CORE_FIX_TIME_H);
				time.set(Calendar.MINUTE, Rnd.get(0, Config.CORE_FIX_TIME_M));
				time.set(Calendar.SECOND, Rnd.get(0, Config.CORE_FIX_TIME_S));
				
				long _respawnEnd = time.getTimeInMillis();
				long _respawn = time.getTimeInMillis() - Calendar.getInstance().getTimeInMillis();
				
				StatsSet info = GrandBossManager.getInstance().getStatsSet(CORE);
				GregorianCalendar gc = (GregorianCalendar) Calendar.getInstance();
				gc.clear();
				
				if (Config.CORE_FIX_TIME)
				{
					startQuestTimer("core_unlock", _respawn, null, null);
					info.set("respawn_time", _respawnEnd);
					gc.setTimeInMillis(_respawnEnd);
				}
				else
				{
					startQuestTimer("core_unlock", respawnTime, null, null);
					info.set("respawn_time", (System.currentTimeMillis() + respawnTime));
					gc.setTimeInMillis((System.currentTimeMillis() + respawnTime));
				}
				
				startQuestTimer("despawn_minions", 20000, null, null);
				
				info.set("killed_time", "" + date.format(new Date(System.currentTimeMillis())));
				info.set("next_respawn", DateFormat.getDateTimeInstance().format(gc.getTime()));
				GrandBossManager.getInstance().setStatsSet(CORE, info);
				String text = "Core killed. Next respawn: " + DateFormat.getDateTimeInstance().format(gc.getTime());
				Log.add(text, "GrandBosses");
				
			}
			
		}
		else
		{
			
			Integer status = GrandBossManager.getInstance().getBossStatus(CORE);
			
			if (status == ALIVE && _minions.contains(npc))
			{
				_minions.remove(npc);
				startQuestTimer("spawn_minion", Config.CORE_RESP_MINION * 1000, npc, null);
			}
		}
		
		return super.onKill(npc, killer, isPet);
	}
	
	@Override
	public String onSpawn(L2NpcInstance npc)
	{
		final L2Attackable mob = (L2Attackable) npc;
		switch (mob.getNpcId())
		{
			case DEATH_KNIGHT:
			case DOOM_WRAITH:
			case SUSCEPTOR:
				mob.setIsRaidMinion(true);
				break;
		}
		return super.onSpawn(mob);
	}
	
	public void spawnBoss(L2GrandBossInstance npc)
	{
		GrandBossManager.getInstance().addBoss(npc);
		npc.broadcastPacket(new PlaySound(1, "BS01_A", 1, npc.getObjectId(), npc.getX(), npc.getY(), npc.getZ()));
		
		// Spawn minions
		for (int i = 0; i < 10; i++)
		{
			int x = 16800 + i * 200;
			_minions.add((L2Attackable) addSpawn(DEATH_KNIGHT, x, 110000, npc.getZ(), 280 + Rnd.get(40), false, 0));
		}
		
		for (int i = 0; i < 4; i++)
		{
			int x2 = 16800 + i * 600;
			_minions.add((L2Attackable) addSpawn(DOOM_WRAITH, x2, 109300, npc.getZ(), 280 + Rnd.get(40), false, 0));
		}
		
		for (int i = 0; i < 4; i++)
		{
			int x = 16800 + i * 450;
			_minions.add((L2Attackable) addSpawn(SUSCEPTOR, x, 110300, npc.getZ(), 280 + Rnd.get(40), false, 0));
		}
	}
	
	@Override
	public void run()
	{
	}
}
