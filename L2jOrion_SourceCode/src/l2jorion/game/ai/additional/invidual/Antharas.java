package l2jorion.game.ai.additional.invidual;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.concurrent.ScheduledFuture;

import l2jorion.Config;
import l2jorion.game.ai.CtrlIntention;
import l2jorion.game.datatables.sql.NpcTable;
import l2jorion.game.datatables.sql.SpawnTable;
import l2jorion.game.managers.GrandBossManager;
import l2jorion.game.model.L2Character;
import l2jorion.game.model.L2World;
import l2jorion.game.model.Location;
import l2jorion.game.model.actor.instance.L2GrandBossInstance;
import l2jorion.game.model.actor.instance.L2NpcInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.quest.Quest;
import l2jorion.game.model.spawn.L2Spawn;
import l2jorion.game.model.zone.type.L2BossZone;
import l2jorion.game.network.PacketServer;
import l2jorion.game.network.serverpackets.Earthquake;
import l2jorion.game.network.serverpackets.PlaySound;
import l2jorion.game.network.serverpackets.SpecialCamera;
import l2jorion.game.templates.L2NpcTemplate;
import l2jorion.game.templates.StatsSet;
import l2jorion.game.thread.ThreadPoolManager;
import l2jorion.log.Log;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;
import l2jorion.util.random.Rnd;

public class Antharas extends Quest implements Runnable
{
	protected static final Logger LOG = LoggerFactory.getLogger(Antharas.class);
	// config
	private static final int FWA_ACTIVITYTIMEOFANTHARAS = 120;
	private static final boolean FWA_MOVEATRANDOM = true;
	private static final boolean FWA_DOSERVEREARTHQUAKE = true;
	// Location of teleport cube.
	private final int _teleportCubeId = 31859;
	private final int _teleportCubeLocation[][] =
	{
		{
			177615,
			114941,
			-7709,
			0
		}
	};
	
	protected ArrayList<L2Spawn> _teleportCubeSpawn = new ArrayList<>();
	protected ArrayList<L2NpcInstance> _teleportCube = new ArrayList<>();
	
	// Spawn data of monsters.
	protected HashMap<Integer, L2Spawn> _monsterSpawn = new HashMap<>();
	
	// Instance of monsters.
	protected ArrayList<L2NpcInstance> _monsters = new ArrayList<>();
	protected L2GrandBossInstance _antharas = null;
	
	// Tasks.
	protected ScheduledFuture<?> _cubeSpawnTask = null;
	protected volatile ScheduledFuture<?> _monsterSpawnTask = null;
	protected ScheduledFuture<?> _activityCheckTask = null;
	protected ScheduledFuture<?> _socialTask = null;
	protected ScheduledFuture<?> _mobiliseTask = null;
	protected ScheduledFuture<?> _mobsSpawnTask = null;
	protected ScheduledFuture<?> _selfDestructionTask = null;
	protected ScheduledFuture<?> _moveAtRandomTask = null;
	protected ScheduledFuture<?> _movieTask = null;
	
	// Antharas Status Tracking :
	private static final int DORMANT = 0; // Antharas is spawned and no one has entered yet. Entry is unlocked
	private static final int WAITING = 1; // Antharas is spawend and someone has entered, triggering a 30 minute window for additional people to enter
	// before he unleashes his attack. Entry is unlocked
	private static final int FIGHTING = 2; // Antharas is engaged in battle, annihilating his foes. Entry is locked
	private static final int DEAD = 3; // Antharas has been killed. Entry is locked
	
	protected static long _LastAction = 0;
	protected long _respawnEnd;
	
	protected static L2BossZone _Zone;
	private final SimpleDateFormat date = new SimpleDateFormat("H:mm:ss yyyy/MM/dd");
	
	// Boss: Antharas
	public Antharas(int id, String name, String descr)
	{
		super(id, name, descr);
		int[] mob =
		{
			29019
		};
		this.registerMobs(mob);
		
		init();
	}
	
	// Initialize
	private void init()
	{
		// Setting spawn data of monsters.
		try
		{
			_Zone = GrandBossManager.getInstance().getZone(179700, 113800, -7709);
			L2NpcTemplate template1;
			L2Spawn tempSpawn;
			
			// Old Antharas
			template1 = NpcTable.getInstance().getTemplate(29019);
			tempSpawn = new L2Spawn(template1);
			tempSpawn.setLocx(181323);
			tempSpawn.setLocy(114850);
			tempSpawn.setLocz(-7623);
			tempSpawn.setHeading(32542);
			tempSpawn.setAmount(1);
			tempSpawn.setRespawnDelay(FWA_ACTIVITYTIMEOFANTHARAS * 2);
			SpawnTable.getInstance().addNewSpawn(tempSpawn, false);
			_monsterSpawn.put(29019, tempSpawn);
		}
		catch (Exception e)
		{
			LOG.warn(e.getMessage());
		}
		
		// Setting spawn data of teleport cube.
		try
		{
			L2NpcTemplate Cube = NpcTable.getInstance().getTemplate(_teleportCubeId);
			L2Spawn spawnDat;
			for (int[] element : _teleportCubeLocation)
			{
				spawnDat = new L2Spawn(Cube);
				spawnDat.setAmount(1);
				spawnDat.setLocx(element[0]);
				spawnDat.setLocy(element[1]);
				spawnDat.setLocz(element[2]);
				spawnDat.setHeading(element[3]);
				spawnDat.setRespawnDelay(60);
				spawnDat.setLocation(0);
				SpawnTable.getInstance().addNewSpawn(spawnDat, false);
				_teleportCubeSpawn.add(spawnDat);
			}
		}
		catch (Exception e)
		{
			LOG.warn(e.getMessage());
		}
		
		Integer status = GrandBossManager.getInstance().getBossStatus(29019);
		StatsSet info = GrandBossManager.getInstance().getStatsSet(29019);
		long respawnTime = info.getLong("respawn_time");
		
		if (status == DEAD)
		{
			ThreadPoolManager.getInstance().scheduleGeneral(new UnlockAntharas(29019), respawnTime - Calendar.getInstance().getTimeInMillis());
		}
		else if (status == WAITING)
		{
			if (status == DEAD && respawnTime <= Calendar.getInstance().getTimeInMillis())
			{
				// the time has already expired while the server was offline. Immediately spawn antharas in his cave.
				// also, the status needs to be changed to DORMANT
				GrandBossManager.getInstance().setBossStatus(29019, DORMANT);
				status = DORMANT;
			}
			setAntharasSpawnTask();
		}
		else if (status == FIGHTING)
		{
			int loc_x = info.getInteger("loc_x");
			int loc_y = info.getInteger("loc_y");
			int loc_z = info.getInteger("loc_z");
			int heading = info.getInteger("heading");
			int hp = info.getInteger("currentHP");
			int mp = info.getInteger("currentMP");
			_antharas = (L2GrandBossInstance) addSpawn(29019, loc_x, loc_y, loc_z, heading, false, 0);
			GrandBossManager.getInstance().addBoss(_antharas);
			_antharas.setCurrentHpMp(hp, mp);
			_LastAction = System.currentTimeMillis();
			// Start repeating timer to check for inactivity
			_activityCheckTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new CheckActivity(), 60000, 60000);
		}
	}
	
	// Do spawn teleport cube.
	public void spawnCube()
	{
		if (_mobsSpawnTask != null)
		{
			_mobsSpawnTask.cancel(true);
			_mobsSpawnTask = null;
		}
		if (_selfDestructionTask != null)
		{
			_selfDestructionTask.cancel(true);
			_selfDestructionTask = null;
		}
		if (_activityCheckTask != null)
		{
			_activityCheckTask.cancel(false);
			_activityCheckTask = null;
		}
		
		for (L2Spawn spawnDat : _teleportCubeSpawn)
		{
			_teleportCube.add(spawnDat.doSpawn());
		}
	}
	
	// Setting Antharas spawn task.
	public void setAntharasSpawnTask()
	{
		if (_monsterSpawnTask == null)
		{
			synchronized (this)
			{
				if (_monsterSpawnTask == null)
				{
					GrandBossManager.getInstance().setBossStatus(29019, WAITING);
					_monsterSpawnTask = ThreadPoolManager.getInstance().scheduleGeneral(new AntharasSpawn(1), 60000 * Config.ANTHARAS_WAIT_TIME);
				}
			}
		}
	}
	
	// Do spawn Antharas.
	private class AntharasSpawn implements Runnable
	{
		private int _taskId = 0;
		
		AntharasSpawn(int taskId)
		{
			_taskId = taskId;
			if (_Zone.getCharactersInside() != null)
			{
				_Zone.getCharactersInside();
			}
		}
		
		@Override
		public void run()
		{
			L2Spawn antharasSpawn = null;
			
			switch (_taskId)
			{
				case 1: // Spawn.
					// Strength of Antharas is decided by the number of players that
					// invaded the lair.
					_monsterSpawnTask.cancel(false);
					_monsterSpawnTask = null;
					
					// Do spawn.
					antharasSpawn = _monsterSpawn.get(29019);
					_antharas = (L2GrandBossInstance) antharasSpawn.doSpawn();
					GrandBossManager.getInstance().addBoss(_antharas);
					
					_monsters.add(_antharas);
					_antharas.setIsImobilised(true);
					
					GrandBossManager.getInstance().setBossStatus(29019, DORMANT);
					GrandBossManager.getInstance().setBossStatus(29019, FIGHTING);
					_LastAction = System.currentTimeMillis();
					// Start repeating timer to check for inactivity
					_activityCheckTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new CheckActivity(), 60000, 60000);
					
					// Set next task.
					if (_socialTask != null)
					{
						_socialTask.cancel(true);
						_socialTask = null;
					}
					_socialTask = ThreadPoolManager.getInstance().scheduleGeneral(new AntharasSpawn(2), 16);
					break;
				case 2:
					// Set camera.
					broadcastPacket(new SpecialCamera(_antharas.getObjectId(), 700, 13, -19, 0, 20000));
					
					// Set next task.
					if (_socialTask != null)
					{
						_socialTask.cancel(true);
						_socialTask = null;
					}
					_socialTask = ThreadPoolManager.getInstance().scheduleGeneral(new AntharasSpawn(3), 3000);
					break;
				
				case 3:
					// Do social.
					broadcastPacket(new SpecialCamera(_antharas.getObjectId(), 700, 13, 0, 6000, 20000));
					// Set next task.
					if (_socialTask != null)
					{
						_socialTask.cancel(true);
						_socialTask = null;
					}
					_socialTask = ThreadPoolManager.getInstance().scheduleGeneral(new AntharasSpawn(4), 10000);
					break;
				case 4:
					broadcastPacket(new SpecialCamera(_antharas.getObjectId(), 3700, 0, -3, 0, 10000));
					// Set next task.
					if (_socialTask != null)
					{
						_socialTask.cancel(true);
						_socialTask = null;
					}
					_socialTask = ThreadPoolManager.getInstance().scheduleGeneral(new AntharasSpawn(5), 200);
					break;
				
				case 5:
					// Do social.
					broadcastPacket(new SpecialCamera(_antharas.getObjectId(), 1100, 0, -3, 22000, 30000));
					// Set next task.
					if (_socialTask != null)
					{
						_socialTask.cancel(true);
						_socialTask = null;
					}
					_socialTask = ThreadPoolManager.getInstance().scheduleGeneral(new AntharasSpawn(6), 10800);
					break;
				
				case 6:
					// Set camera.
					broadcastPacket(new SpecialCamera(_antharas.getObjectId(), 1100, 0, -3, 300, 7000));
					// Set next task.
					if (_socialTask != null)
					{
						_socialTask.cancel(true);
						_socialTask = null;
					}
					_socialTask = ThreadPoolManager.getInstance().scheduleGeneral(new AntharasSpawn(7), 1900);
					break;
				
				case 7:
					_antharas.abortCast();
					
					_mobiliseTask = ThreadPoolManager.getInstance().scheduleGeneral(new SetMobilised(_antharas), 16);
					
					// Move at random.
					if (FWA_MOVEATRANDOM)
					{
						Location pos = new Location(Rnd.get(175000, 178500), Rnd.get(112400, 116000), -7707, 0);
						_moveAtRandomTask = ThreadPoolManager.getInstance().scheduleGeneral(new MoveAtRandom(_antharas, pos), 500);
					}
					
					if (_socialTask != null)
					{
						_socialTask.cancel(true);
						_socialTask = null;
					}
					break;
			}
		}
	}
	
	protected void broadcastPacket(PacketServer mov)
	{
		if (_Zone != null)
		{
			for (L2Character characters : _Zone.getCharactersInside())
			{
				if (characters instanceof L2PcInstance)
				{
					characters.sendPacket(mov);
				}
			}
		}
	}
	
	// At end of activity time.
	protected class CheckActivity implements Runnable
	{
		@Override
		public void run()
		{
			final Long temp = (System.currentTimeMillis() - _LastAction);
			if (temp > (Config.ANTHARAS_DESPAWN_TIME) * 60000)
			{
				GrandBossManager.getInstance().setBossStatus(_antharas.getNpcId(), DORMANT);
				setUnspawn();
			}
		}
	}
	
	// Clean Antharas's lair.
	public void setUnspawn()
	{
		// Eliminate players.
		_Zone.oustAllPlayers();
		
		// Not executed tasks is canceled.
		if (_cubeSpawnTask != null)
		{
			_cubeSpawnTask.cancel(true);
			_cubeSpawnTask = null;
		}
		if (_monsterSpawnTask != null)
		{
			_monsterSpawnTask.cancel(true);
			_monsterSpawnTask = null;
		}
		if (_activityCheckTask != null)
		{
			_activityCheckTask.cancel(false);
			_activityCheckTask = null;
		}
		if (_socialTask != null)
		{
			_socialTask.cancel(true);
			_socialTask = null;
		}
		if (_mobiliseTask != null)
		{
			_mobiliseTask.cancel(true);
			_mobiliseTask = null;
		}
		if (_mobsSpawnTask != null)
		{
			_mobsSpawnTask.cancel(true);
			_mobsSpawnTask = null;
		}
		if (_selfDestructionTask != null)
		{
			_selfDestructionTask.cancel(true);
			_selfDestructionTask = null;
		}
		if (_moveAtRandomTask != null)
		{
			_moveAtRandomTask.cancel(true);
			_moveAtRandomTask = null;
		}
		
		// Delete monsters.
		for (L2NpcInstance mob : _monsters)
		{
			mob.getSpawn().stopRespawn();
			mob.deleteMe();
		}
		_monsters.clear();
		
		// Delete teleport cube.
		for (L2NpcInstance cube : _teleportCube)
		{
			cube.getSpawn().stopRespawn();
			cube.deleteMe();
		}
		_teleportCube.clear();
	}
	
	// Do spawn teleport cube.
	private class CubeSpawn implements Runnable
	{
		private int _type;
		
		CubeSpawn(int type)
		{
			_type = type;
		}
		
		@Override
		public void run()
		{
			if (_type == 0)
			{
				spawnCube();
				_cubeSpawnTask = ThreadPoolManager.getInstance().scheduleGeneral(new CubeSpawn(1), 1800000);
			}
			else
			{
				setUnspawn();
			}
		}
	}
	
	// UnLock Antharas.
	private static class UnlockAntharas implements Runnable
	{
		private int _bossId;
		
		public UnlockAntharas(int bossId)
		{
			_bossId = bossId;
		}
		
		@Override
		public void run()
		{
			GrandBossManager.getInstance().setBossStatus(_bossId, DORMANT);
			if (FWA_DOSERVEREARTHQUAKE)
			{
				for (L2PcInstance p : L2World.getInstance().getAllPlayers().values())
				{
					p.broadcastPacket(new Earthquake(185708, 114298, -8221, 20, 10));
				}
			}
		}
	}
	
	// Action is enabled the boss.
	private class SetMobilised implements Runnable
	{
		private L2GrandBossInstance _boss;
		
		public SetMobilised(L2GrandBossInstance boss)
		{
			_boss = boss;
		}
		
		@Override
		public void run()
		{
			_boss.setIsImobilised(false);
			
			// When it is possible to act, a social action is canceled.
			if (_socialTask != null)
			{
				_socialTask.cancel(true);
				_socialTask = null;
			}
		}
	}
	
	// Move at random on after Antharas appears.
	private static class MoveAtRandom implements Runnable
	{
		private L2NpcInstance _npc;
		private Location _pos;
		
		public MoveAtRandom(L2NpcInstance npc, Location pos)
		{
			_npc = npc;
			_pos = pos;
		}
		
		@Override
		public void run()
		{
			_npc.getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, _pos);
		}
	}
	
	@Override
	public String onAttack(final L2NpcInstance npc, final L2PcInstance attacker, final int damage, final boolean isPet)
	{
		_LastAction = System.currentTimeMillis();
		return super.onAttack(npc, attacker, damage, isPet);
	}
	
	@Override
	public String onKill(L2NpcInstance npc, L2PcInstance killer, boolean isPet)
	{
		if (npc.getNpcId() == 29019)
		{
			npc.broadcastPacket(new PlaySound(1, "BS01_D", 1, npc.getObjectId(), npc.getX(), npc.getY(), npc.getZ()));
			
			if (!npc.getSpawn().is_customBossInstance())
			{
				GrandBossManager.getInstance().setBossStatus(npc.getNpcId(), DEAD);
				_cubeSpawnTask = ThreadPoolManager.getInstance().scheduleGeneral(new CubeSpawn(0), 5000);
				
				long respawnTime = (Config.ANTHARAS_RESP_FIRST + (Config.ANTHARAS_RESP_SECOND == 0 ? 0 : Rnd.get(1, Config.ANTHARAS_RESP_SECOND))) * 3600000;
				final int days = Config.ANTHARAS_FIX_TIME_D * 24;
				
				Calendar time = Calendar.getInstance();
				time.add(Calendar.HOUR, days);
				time.set(Calendar.HOUR_OF_DAY, Config.ANTHARAS_FIX_TIME_H);
				time.set(Calendar.MINUTE, Rnd.get(0, Config.ANTHARAS_FIX_TIME_M));
				time.set(Calendar.SECOND, Rnd.get(0, Config.ANTHARAS_FIX_TIME_S));
				
				long _respawnEnd = time.getTimeInMillis();
				long _respawn = time.getTimeInMillis() - Calendar.getInstance().getTimeInMillis();
				
				// also save the respawn time so that the info is maintained past reboots
				StatsSet info = GrandBossManager.getInstance().getStatsSet(npc.getNpcId());
				
				GregorianCalendar gc = (GregorianCalendar) Calendar.getInstance();
				gc.clear();
				
				if (Config.ANTHARAS_FIX_TIME)
				{
					ThreadPoolManager.getInstance().scheduleGeneral(new UnlockAntharas(npc.getNpcId()), _respawn);
					info.set("respawn_time", _respawnEnd);
					gc.setTimeInMillis(_respawnEnd);
				}
				else
				{
					ThreadPoolManager.getInstance().scheduleGeneral(new UnlockAntharas(npc.getNpcId()), respawnTime);
					info.set("respawn_time", (System.currentTimeMillis() + respawnTime));
					gc.setTimeInMillis((System.currentTimeMillis() + respawnTime));
				}
				
				info.set("killed_time", "" + date.format(new Date(System.currentTimeMillis())));
				info.set("next_respawn", DateFormat.getDateTimeInstance().format(gc.getTime()));
				GrandBossManager.getInstance().setStatsSet(npc.getNpcId(), info);
				String text = "Antharas killed. Next respawn: " + DateFormat.getDateTimeInstance().format(gc.getTime());
				Log.add(text, "GrandBosses");
			}
			
		}
		
		if (_monsters.contains(npc))
		{
			_monsters.remove(npc);
		}
		
		return super.onKill(npc, killer, isPet);
	}
	
	@Override
	public void run()
	{
	}
}