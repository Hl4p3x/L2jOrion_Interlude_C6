package l2jorion.game.ai.additional.invidual;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import l2jorion.Config;
import l2jorion.game.ai.CtrlIntention;
import l2jorion.game.datatables.SkillTable;
import l2jorion.game.managers.GrandBossManager;
import l2jorion.game.model.L2Attackable;
import l2jorion.game.model.actor.instance.L2GrandBossInstance;
import l2jorion.game.model.actor.instance.L2MonsterInstance;
import l2jorion.game.model.actor.instance.L2NpcInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.quest.Quest;
import l2jorion.game.model.zone.type.L2BossZone;
import l2jorion.game.network.serverpackets.PlaySound;
import l2jorion.game.network.serverpackets.SocialAction;
import l2jorion.game.templates.StatsSet;
import l2jorion.log.Log;
import l2jorion.util.random.Rnd;

public class QueenAnt extends Quest implements Runnable
{
	private static final int QUEEN = 29001;
	private static final int LARVA = 29002;
	private static final int NURSE = 29003;
	private static final int GUARD = 29004;
	private static final int ROYAL = 29005;
	
	private static final int LIVE = 0; // Queen Ant is spawned.
	private static final int DEAD = 1; // Queen Ant has been killed.
	
	protected L2BossZone _Zone;
	private L2MonsterInstance _larva = null;
	private L2MonsterInstance _queen = null;
	
	private final List<L2MonsterInstance> _Minions = new ArrayList<>();
	private final List<L2MonsterInstance> _Nurses = new ArrayList<>();
	
	protected long _respawnEnd;
	private final SimpleDateFormat date = new SimpleDateFormat("H:mm:ss yyyy/MM/dd");
	
	enum Event
	{
		QUEEN_SPAWN,
		CHECK_MINIONS_ZONE,
		ACTION,
		DESPAWN_MINIONS,
		SPAWN_ROYAL,
		NURSES_SPAWN,
		RESPAWN_ROYAL,
		RESPAWN_NURSE,
		LARVA_DESPAWN,
		HEAL
	}
	
	public QueenAnt(final int questId, final String name, final String descr)
	{
		super(questId, name, descr);
		
		final int[] mobs =
		{
			QUEEN,
			LARVA,
			NURSE,
			GUARD,
			ROYAL
		};
		
		for (final int mob : mobs)
		{
			addEventId(mob, Quest.QuestEventType.ON_KILL);
			addEventId(mob, Quest.QuestEventType.ON_SPAWN);
			addEventId(mob, Quest.QuestEventType.ON_ATTACK);
		}
		
		_Zone = GrandBossManager.getInstance().getZone(-21610, 181594, -5734);
		
		StatsSet info = GrandBossManager.getInstance().getStatsSet(QUEEN);
		Integer status = GrandBossManager.getInstance().getBossStatus(QUEEN);
		
		switch (status)
		{
			case DEAD:
			{
				final long temp = info.getLong("respawn_time") - Calendar.getInstance().getTimeInMillis();
				if (temp > 0)
				{
					startQuestTimer("QUEEN_SPAWN", temp, null, null);
				}
				else
				{
					final L2GrandBossInstance queen = (L2GrandBossInstance) addSpawn(QUEEN, -21610, 181594, -5734, 0, false, 0);
					GrandBossManager.getInstance().setBossStatus(QUEEN, LIVE);
					GrandBossManager.getInstance().addBoss(queen);
					spawnBoss(queen);
					
					if (Config.L2JMOD_CHAMPION_ENABLE)
					{
						if (Config.L2JMOD_CHAMP_RAID_BOSSES && Config.L2JMOD_CHAMPION_FREQUENCY > 0 && queen.getLevel() >= Config.L2JMOD_CHAMP_MIN_LVL && queen.getLevel() <= Config.L2JMOD_CHAMP_MAX_LVL)
						{
							queen.setChampion(false);
							int random = Rnd.get(100);
							if (random <= Config.L2JMOD_CHAMPION_FREQUENCY)
							{
								queen.setChampion(true);
							}
						}
					}
				}
			}
				break;
			case LIVE:
			{
				final int hp = info.getInteger("currentHP");
				final int mp = info.getInteger("currentMP");
				final L2GrandBossInstance queen = (L2GrandBossInstance) addSpawn(QUEEN, -21610, 181594, -5734, 0, false, 0);
				
				GrandBossManager.getInstance().addBoss(queen);
				queen.setCurrentHpMp(hp, mp);
				spawnBoss(queen);
			}
				break;
			default:
			{
				final L2GrandBossInstance queen = (L2GrandBossInstance) addSpawn(QUEEN, -21610, 181594, -5734, 0, false, 0);
				
				GrandBossManager.getInstance().setBossStatus(QUEEN, LIVE);
				GrandBossManager.getInstance().addBoss(queen);
				spawnBoss(queen);
			}
		}
	}
	
	private void spawnBoss(final L2GrandBossInstance npc)
	{
		startQuestTimer("ACTION", 10000, npc, null, true);
		npc.broadcastPacket(new PlaySound(1, "BS02_D", 1, npc.getObjectId(), npc.getX(), npc.getY(), npc.getZ()));
		startQuestTimer("SPAWN_ROYAL", 1000, npc, null);
		startQuestTimer("NURSES_SPAWN", 1000, npc, null);
		startQuestTimer("HEAL", 2000, null, null, true);
		
		_queen = npc;
		_larva = (L2MonsterInstance) addSpawn(LARVA, -21600, 179482, -5846, Rnd.get(360), false, 0);
		_larva.setIsUnkillable(true);
		_larva.setIsImobilised(true);
		_larva.setIsAttackDisabled(true);
	}
	
	@Override
	public String onAdvEvent(final String event, final L2NpcInstance npc, final L2PcInstance player)
	{
		final Event event_enum = Event.valueOf(event);
		
		switch (event_enum)
		{
			case QUEEN_SPAWN:
			{
				final L2GrandBossInstance queen = (L2GrandBossInstance) addSpawn(QUEEN, -21610, 181594, -5734, 0, false, 0);
				GrandBossManager.getInstance().setBossStatus(QUEEN, LIVE);
				GrandBossManager.getInstance().addBoss(queen);
				spawnBoss(queen);
			}
				break;
			case LARVA_DESPAWN:
			{
				_larva.deleteMe();
			}
				break;
			case NURSES_SPAWN:
			{
				final int radius = 250;
				for (int i = 0; i < 6; i++)
				{
					final int x = (int) (radius * Math.cos(i * 1.407));
					final int y = (int) (radius * Math.sin(i * 1.407));
					_Nurses.add((L2MonsterInstance) addSpawn(NURSE, npc.getX() + x, npc.getY() + y, npc.getZ(), 0, false, 0));
					_Nurses.get(i).setIsAttackDisabled(true);
					_Nurses.get(i).setIsRaidMinion(true);
				}
			}
				break;
			case SPAWN_ROYAL:
			{
				final int radius = 400;
				for (int i = 0; i < 8; i++)
				{
					final int x = (int) (radius * Math.cos(i * .7854));
					final int y = (int) (radius * Math.sin(i * .7854));
					_Minions.add((L2MonsterInstance) addSpawn(ROYAL, npc.getX() + x, npc.getY() + y, npc.getZ(), 0, false, 0));
					_Minions.get(i).setIsRaidMinion(true);
				}
			}
				break;
			case RESPAWN_ROYAL:
			{
				if (GrandBossManager.getInstance().getBossStatus(QUEEN) == LIVE)
				{
					_Minions.add((L2MonsterInstance) addSpawn(ROYAL, npc.getX(), npc.getY(), npc.getZ(), 0, true, 0));
				}
			}
				break;
			case RESPAWN_NURSE:
			{
				if (GrandBossManager.getInstance().getBossStatus(QUEEN) == LIVE)
				{
					_Nurses.add((L2MonsterInstance) addSpawn(NURSE, npc.getX(), npc.getY(), npc.getZ(), 0, true, 0));
				}
			}
				break;
			case DESPAWN_MINIONS:
			{
				for (int i = 0; i < _Minions.size(); i++)
				{
					final L2Attackable mob = _Minions.get(i);
					if (mob != null)
					{
						mob.deleteMe();
					}
				}
				for (int k = 0; k < _Nurses.size(); k++)
				{
					final L2MonsterInstance _nurse = _Nurses.get(k);
					if (_nurse != null)
					{
						_nurse.deleteMe();
					}
				}
				_Nurses.clear();
				_Minions.clear();
			}
				break;
			case CHECK_MINIONS_ZONE:
			{
				for (int i = 0; i < _Minions.size(); i++)
				{
					final L2Attackable mob = _Minions.get(i);
					
					if (mob != null && !_Zone.isInsideZone(mob))
					{
						mob.teleToLocation(npc.getX(), npc.getY(), npc.getZ());
					}
				}
				
				for (int i = 0; i < _Nurses.size(); i++)
				{
					final L2Attackable mob = _Nurses.get(i);
					
					if (mob != null && !_Zone.isInsideZone(mob))
					{
						mob.teleToLocation(npc.getX(), npc.getY(), npc.getZ());
					}
				}
			}
				break;
			case ACTION:
			{
				if (Rnd.get(3) == 0)
				{
					if (Rnd.get(2) == 0)
					{
						npc.broadcastPacket(new SocialAction(npc.getObjectId(), 3));
					}
					else
					{
						npc.broadcastPacket(new SocialAction(npc.getObjectId(), 4));
					}
				}
			}
				break;
			case HEAL:
			{
				boolean notCasting;
				final boolean larvaNeedHeal = _larva != null && _larva.getCurrentHp() < _larva.getMaxHp();
				final boolean queenNeedHeal = _queen != null && _queen.getCurrentHp() < _queen.getMaxHp();
				boolean nurseNeedHeal = false;
				
				for (final L2MonsterInstance nurse : _Nurses)
				{
					nurseNeedHeal = nurse != null && nurse.getCurrentHp() < nurse.getMaxHp();
					
					if (nurse == null || nurse.isDead() || nurse.isCastingNow())
					{
						continue;
					}
					
					notCasting = nurse.getAI().getIntention() != CtrlIntention.AI_INTENTION_CAST;
					
					if (larvaNeedHeal)
					{
						if (nurse.getTarget() != _larva || notCasting)
						{
							nurse.setTarget(_larva);
							if (!nurse.isCastingNow())
							{
								getIntoPosition(nurse, _larva);
							}
							if (nurse.isInsideRadius(_larva, 200, true, true))
							{
								nurse.getAI().clientStopMoving(null);
								if (!nurse.isCastingNow())
								{
									nurse.doCast(Rnd.nextBoolean() ? SkillTable.getInstance().getInfo(4020, 1) : SkillTable.getInstance().getInfo(4024, 1));
								}
							}
						}
						continue;
					}
					
					if (queenNeedHeal)
					{
						if (nurse.getTarget() != _queen || notCasting)
						{
							nurse.setTarget(_queen);
							if (!nurse.isCastingNow())
							{
								getIntoPosition(nurse, _queen);
							}
							if (nurse.isInsideRadius(_queen, 200, true, true))
							{
								nurse.getAI().clientStopMoving(null);
								if (!nurse.isCastingNow())
								{
									nurse.doCast(SkillTable.getInstance().getInfo(4020, 1));
								}
							}
						}
						continue;
					}
					
					if (nurseNeedHeal)
					{
						if (nurse.getTarget() != nurse || notCasting)
						{
							for (int k = 0; k < _Nurses.size(); k++)
							{
								_Nurses.get(k).setTarget(nurse);
								
								if (!_Nurses.get(k).isCastingNow())
								{
									getIntoPosition(_Nurses.get(k), nurse);
								}
								if (_Nurses.get(k).isInsideRadius(nurse, 200, true, true))
								{
									_Nurses.get(k).getAI().clientStopMoving(null);
									if (!_Nurses.get(k).isCastingNow())
									{
										_Nurses.get(k).doCast(SkillTable.getInstance().getInfo(4020, 1));
									}
								}
							}
						}
						continue;
					}
					
					if (notCasting && nurse.getTarget() != null)
					{
						nurse.setTarget(null);
					}
				}
			}
				break;
		}
		
		return super.onAdvEvent(event, npc, player);
	}
	
	@Override
	public String onSpawn(L2NpcInstance npc)
	{
		final L2Attackable mob = (L2Attackable) npc;
		switch (mob.getNpcId())
		{
			case NURSE:
				mob.setIsAttackDisabled(true);
			case LARVA:
			case ROYAL:
			case GUARD:
				mob.setIsRaidMinion(true);
				break;
		}
		return super.onSpawn(mob);
	}
	
	@Override
	public String onKill(L2NpcInstance npc, L2PcInstance killer, boolean isPet)
	{
		final int npcId = npc.getNpcId();
		final Integer status = GrandBossManager.getInstance().getBossStatus(QUEEN);
		
		if (npcId == QUEEN)
		{
			npc.broadcastPacket(new PlaySound(1, "BS02_D", 1, npc.getObjectId(), npc.getX(), npc.getY(), npc.getZ()));
			
			if (!npc.getSpawn().is_customBossInstance())
			{
				GrandBossManager.getInstance().setBossStatus(QUEEN, DEAD);
				
				long respawnTime = (Config.QA_RESP_FIRST + (Config.QA_RESP_SECOND == 0 ? 0 : Rnd.get(1, Config.QA_RESP_SECOND))) * 3600000;
				final int days = Config.QA_FIX_TIME_D * 24;
				
				Calendar time = Calendar.getInstance();
				time.add(Calendar.HOUR, days);
				time.set(Calendar.HOUR_OF_DAY, Config.QA_FIX_TIME_H);
				time.set(Calendar.MINUTE, Rnd.get(0, Config.QA_FIX_TIME_M));
				time.set(Calendar.SECOND, Rnd.get(0, Config.QA_FIX_TIME_S));
				
				long _respawnEnd = time.getTimeInMillis();
				long _respawn = time.getTimeInMillis() - Calendar.getInstance().getTimeInMillis();
				
				StatsSet info = GrandBossManager.getInstance().getStatsSet(QUEEN);
				GregorianCalendar gc = (GregorianCalendar) Calendar.getInstance();
				gc.clear();
				
				if (Config.QA_FIX_TIME)
				{
					startQuestTimer("QUEEN_SPAWN", _respawn, null, null);
					info.set("respawn_time", _respawnEnd);
					gc.setTimeInMillis(_respawnEnd);
				}
				else
				{
					startQuestTimer("QUEEN_SPAWN", respawnTime, null, null);
					info.set("respawn_time", (System.currentTimeMillis() + respawnTime));
					gc.setTimeInMillis((System.currentTimeMillis() + respawnTime));
				}
				
				startQuestTimer("LARVA_DESPAWN", 4 * 60 * 60 * 1000, null, null);
				startQuestTimer("DESPAWN_MINIONS", 5000, null, null);
				
				cancelQuestTimer("ACTION", npc, null);
				cancelQuestTimer("SPAWN_ROYAL", npc, null);
				cancelQuestTimer("NURSES_SPAWN", npc, null);
				cancelQuestTimer("CHECK_MINIONS_ZONE", npc, null);
				cancelQuestTimer("RESPAWN_ROYAL", npc, null);
				cancelQuestTimer("RESPAWN_NURSE", npc, null);
				cancelQuestTimer("HEAL", null, null);
				
				info.set("killed_time", "" + date.format(new Date(System.currentTimeMillis())));
				info.set("next_respawn", DateFormat.getDateTimeInstance().format(gc.getTime()));
				GrandBossManager.getInstance().setStatsSet(QUEEN, info);
				
				String text = "Queen Ant killed. Next respawn: " + DateFormat.getDateTimeInstance().format(gc.getTime());
				Log.add(text, "GrandBosses");
			}
		}
		else if (status == LIVE)
		{
			if (npcId == ROYAL || npcId == NURSE)
			{
				if (_Minions.contains(npc))
				{
					_Minions.remove(npc);
					startQuestTimer("RESPAWN_ROYAL", (Config.QA_RESP_ROYAL + Rnd.get(40)) * 1000, npc, null);
				}
				
				if (_Nurses.contains(npc))
				{
					_Nurses.remove(npc);
					startQuestTimer("RESPAWN_NURSE", Config.QA_RESP_NURSE * 1000, npc, null);
				}
			}
		}
		return super.onKill(npc, killer, isPet);
	}
	
	public void getIntoPosition(L2MonsterInstance nurse, L2MonsterInstance caller)
	{
		if (!nurse.isInsideRadius(caller, 200, true, true))
		{
			nurse.getAI().moveToPawn(caller, 200);
		}
	}
	
	@Override
	public void run()
	{
	}
}