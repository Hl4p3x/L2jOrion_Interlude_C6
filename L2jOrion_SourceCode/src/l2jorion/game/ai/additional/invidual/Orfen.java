package l2jorion.game.ai.additional.invidual;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import l2jorion.Config;
import l2jorion.game.ai.CtrlIntention;
import l2jorion.game.datatables.SkillTable;
import l2jorion.game.managers.GrandBossManager;
import l2jorion.game.model.actor.instance.L2GrandBossInstance;
import l2jorion.game.model.actor.instance.L2NpcInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.entity.Announcements;
import l2jorion.game.model.quest.Quest;
import l2jorion.game.network.serverpackets.PlaySound;
import l2jorion.game.templates.StatsSet;
import l2jorion.log.Log;
import l2jorion.util.random.Rnd;

public class Orfen extends Quest implements Runnable
{
	private static final int ORFEN = 29014;
	private static final int LIVE = 0;
	private static final int DEAD = 1;
	
	private boolean FirstAttacked = false;
	private boolean Teleported = false;
	protected long _respawnEnd;
	private final SimpleDateFormat date = new SimpleDateFormat("H:mm:ss yyyy/MM/dd");
	
	L2GrandBossInstance orfen = null;
	
	enum Event
	{
		ORFEN_SPAWN,
		ORFEN_REFRESH,
		ORFEN_RETURN
	}
	
	public Orfen(int questId, String name, String descr)
	{
		super(questId, name, descr);
		
		StatsSet info = GrandBossManager.getInstance().getStatsSet(ORFEN);
		Integer status = GrandBossManager.getInstance().getBossStatus(ORFEN);
		
		addEventId(ORFEN, Quest.QuestEventType.ON_KILL);
		addEventId(ORFEN, Quest.QuestEventType.ON_ATTACK);
		
		switch (status)
		{
			case DEAD:
			{
				long temp = info.getLong("respawn_time") - Calendar.getInstance().getTimeInMillis();
				if (temp > 0)
				{
					startQuestTimer("ORFEN_SPAWN", temp, null, null);
				}
				else
				{
					int loc_x = 55024;
					int loc_y = 17368;
					int loc_z = -5412;
					int heading = 0;
					
					orfen = (L2GrandBossInstance) addSpawn(ORFEN, loc_x, loc_y, loc_z, heading, false, 0);
					if (Config.ANNOUNCE_TO_ALL_SPAWN_RB)
					{
						Announcements.getInstance().announceWithServerName("The Grand boss " + orfen.getName() + " spawned in world.");
					}
					GrandBossManager.getInstance().setBossStatus(ORFEN, LIVE);
					GrandBossManager.getInstance().addBoss(orfen);
					
					if (Config.L2JMOD_CHAMPION_ENABLE)
					{
						if (Config.L2JMOD_CHAMP_RAID_BOSSES && Config.L2JMOD_CHAMPION_FREQUENCY > 0 && orfen.getLevel() >= Config.L2JMOD_CHAMP_MIN_LVL && orfen.getLevel() <= Config.L2JMOD_CHAMP_MAX_LVL)
						{
							orfen.setChampion(false);
							int random = Rnd.get(100);
							if (random <= Config.L2JMOD_CHAMPION_FREQUENCY)
							{
								orfen.setChampion(true);
							}
						}
					}
				}
			}
				break;
			case LIVE:
			{
				/*
				 * int loc_x = info.getInteger("loc_x"); int loc_y = info.getInteger("loc_y"); int loc_z = info.getInteger("loc_z"); int heading = info.getInteger("heading");
				 */
				
				int loc_x = 55024;
				int loc_y = 17368;
				int loc_z = -5412;
				int heading = 0;
				
				int hp = info.getInteger("currentHP");
				int mp = info.getInteger("currentMP");
				orfen = (L2GrandBossInstance) addSpawn(ORFEN, loc_x, loc_y, loc_z, heading, false, 0);
				if (Config.ANNOUNCE_TO_ALL_SPAWN_RB)
				{
					Announcements.getInstance().announceWithServerName("The Grand boss " + orfen.getName() + " spawned in world.");
				}
				GrandBossManager.getInstance().addBoss(orfen);
				orfen.setCurrentHpMp(hp, mp);
			}
				break;
			default:
			{
				int loc_x = 55024;
				int loc_y = 17368;
				int loc_z = -5412;
				int heading = 0;
				
				orfen = (L2GrandBossInstance) addSpawn(ORFEN, loc_x, loc_y, loc_z, heading, false, 0);
				if (Config.ANNOUNCE_TO_ALL_SPAWN_RB)
				{
					Announcements.getInstance().announceWithServerName("The Grand boss " + orfen.getName() + " spawned in world.");
				}
				GrandBossManager.getInstance().setBossStatus(ORFEN, LIVE);
				GrandBossManager.getInstance().addBoss(orfen);
			}
		}
	}
	
	@Override
	public String onAdvEvent(String event, L2NpcInstance npc, L2PcInstance player)
	{
		Event event_enum = Event.valueOf(event.toUpperCase());
		
		switch (event_enum)
		{
			case ORFEN_SPAWN:
			{
				int loc_x = 55024;
				int loc_y = 17368;
				int loc_z = -5412;
				int heading = 0;
				
				orfen = (L2GrandBossInstance) addSpawn(ORFEN, loc_x, loc_y, loc_z, heading, false, 0);
				if (Config.ANNOUNCE_TO_ALL_SPAWN_RB)
				{
					Announcements.getInstance().announceWithServerName("The Grand boss " + orfen.getName() + " spawned in world.");
				}
				GrandBossManager.getInstance().setBossStatus(ORFEN, LIVE);
				GrandBossManager.getInstance().addBoss(orfen);
			}
				break;
			case ORFEN_REFRESH:
			{
				if (npc == null || npc.getSpawn() == null)
				{
					cancelQuestTimer("ORFEN_REFRESH", npc, null);
					break;
				}
				
				double saved_hp = -1;
				
				if (npc.getNpcId() == ORFEN && !npc.getSpawn().is_customBossInstance())
				{
					saved_hp = GrandBossManager.getInstance().getStatsSet(ORFEN).getDouble("currentHP");
					
					if (saved_hp < npc.getCurrentHp())
					{
						npc.setCurrentHp(saved_hp);
						GrandBossManager.getInstance().getStatsSet(ORFEN).set("currentHP", npc.getMaxHp());
					}
				}
				
				if ((Teleported && npc.getCurrentHp() > npc.getMaxHp() * 0.95))
				{
					cancelQuestTimer("ORFEN_REFRESH", npc, null);
					startQuestTimer("ORFEN_RETURN", 10000, npc, null);
				}
				else
				{ // restart the refresh scheduling
					startQuestTimer("ORFEN_REFRESH", 10000, npc, null);
				}
				
			}
				break;
			case ORFEN_RETURN:
			{
				if (npc == null || npc.getSpawn() == null)
				{
					break;
				}
				
				this.Teleported = false;
				this.FirstAttacked = false;
				npc.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
				npc.getSpawn().setLocx(55024);
				npc.getSpawn().setLocy(17368);
				npc.getSpawn().setLocz(-5412);
				npc.teleToLocation(55024, 17368, -5412, false);
			}
				break;
			default:
			{
				LOG.info("ORFEN: Not defined event: " + event + "!");
			}
		}
		
		return super.onAdvEvent(event, npc, player);
	}
	
	@Override
	public String onAttack(L2NpcInstance npc, L2PcInstance attacker, int damage, boolean isPet)
	{
		int npcId = npc.getNpcId();
		if (npcId == ORFEN)
		{
			if (FirstAttacked)
			{
				if ((npc.getCurrentHp() - damage) < (npc.getMaxHp() / 2) && !Teleported)
				{
					GrandBossManager.getInstance().getStatsSet(ORFEN).set("currentHP", npc.getCurrentHp());
					npc.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
					Teleported = true;
					npc.getSpawn().setLocx(43577);
					npc.getSpawn().setLocy(15985);
					npc.getSpawn().setLocz(-4396);
					npc.teleToLocation(43577, 15985, -4396, false);
					startQuestTimer("ORFEN_REFRESH", 10000, npc, null);
				}
				else if (npc.isInsideRadius(attacker, 1000, false, false) && !npc.isInsideRadius(attacker, 300, false, false) && Rnd.get(10) == 0)
				{
					attacker.teleToLocation(npc.getX(), npc.getY(), npc.getZ());
					npc.setTarget(attacker);
					npc.doCast(SkillTable.getInstance().getInfo(4064, 1));
				}
			}
			else
			{
				FirstAttacked = true;
			}
		}
		
		return super.onAttack(npc, attacker, damage, isPet);
	}
	
	@Override
	public String onKill(L2NpcInstance npc, L2PcInstance killer, boolean isPet)
	{
		if (npc.getNpcId() == ORFEN)
		{
			npc.broadcastPacket(new PlaySound(1, "BS02_D", 1, npc.getObjectId(), npc.getX(), npc.getY(), npc.getZ()));
			
			if (!npc.getSpawn().is_customBossInstance())
			{
				GrandBossManager.getInstance().setBossStatus(ORFEN, DEAD);
				
				long respawnTime = (Config.ORFEN_RESP_FIRST + (Config.ORFEN_RESP_SECOND == 0 ? 0 : Rnd.get(1, Config.ORFEN_RESP_SECOND))) * 3600000;
				final int days = Config.ORFEN_FIX_TIME_D * 24;
				
				Calendar time = Calendar.getInstance();
				time.add(Calendar.HOUR, days);
				time.set(Calendar.HOUR_OF_DAY, Config.ORFEN_FIX_TIME_H);
				time.set(Calendar.MINUTE, Rnd.get(0, Config.ORFEN_FIX_TIME_M));
				time.set(Calendar.SECOND, Rnd.get(0, Config.ORFEN_FIX_TIME_S));
				
				long _respawnEnd = time.getTimeInMillis();
				long _respawn = time.getTimeInMillis() - Calendar.getInstance().getTimeInMillis();
				
				StatsSet info = GrandBossManager.getInstance().getStatsSet(ORFEN);
				GregorianCalendar gc = (GregorianCalendar) Calendar.getInstance();
				gc.clear();
				
				if (Config.ORFEN_FIX_TIME)
				{
					startQuestTimer("ORFEN_SPAWN", _respawn, null, null);
					info.set("respawn_time", _respawnEnd);
					gc.setTimeInMillis(_respawnEnd);
				}
				else
				{
					startQuestTimer("ORFEN_SPAWN", respawnTime, null, null);
					info.set("respawn_time", (System.currentTimeMillis() + respawnTime));
					gc.setTimeInMillis((System.currentTimeMillis() + respawnTime));
				}
				
				cancelQuestTimer("ORFEN_REFRESH", npc, null);
				
				info.set("killed_time", "" + date.format(new Date(System.currentTimeMillis())));
				info.set("next_respawn", DateFormat.getDateTimeInstance().format(gc.getTime()));
				GrandBossManager.getInstance().setStatsSet(ORFEN, info);
				
				String text = "Orfen killed. Next respawn: " + DateFormat.getDateTimeInstance().format(gc.getTime());
				Log.add(text, "GrandBosses");
			}
		}
		
		return super.onKill(npc, killer, isPet);
	}
	
	@Override
	public void run()
	{
	}
}