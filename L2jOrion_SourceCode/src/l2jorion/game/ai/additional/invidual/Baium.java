package l2jorion.game.ai.additional.invidual;

import static l2jorion.game.ai.CtrlIntention.AI_INTENTION_IDLE;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import l2jorion.Config;
import l2jorion.game.ai.CtrlIntention;
import l2jorion.game.datatables.SkillTable;
import l2jorion.game.geo.GeoData;
import l2jorion.game.managers.GrandBossManager;
import l2jorion.game.model.L2Attackable;
import l2jorion.game.model.L2Character;
import l2jorion.game.model.L2Object;
import l2jorion.game.model.L2Skill;
import l2jorion.game.model.actor.instance.L2GrandBossInstance;
import l2jorion.game.model.actor.instance.L2MonsterInstance;
import l2jorion.game.model.actor.instance.L2NpcInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.actor.instance.L2PetInstance;
import l2jorion.game.model.actor.instance.L2PlayableInstance;
import l2jorion.game.model.quest.Quest;
import l2jorion.game.model.zone.type.L2BossZone;
import l2jorion.game.network.serverpackets.Earthquake;
import l2jorion.game.network.serverpackets.PlaySound;
import l2jorion.game.network.serverpackets.SocialAction;
import l2jorion.game.templates.StatsSet;
import l2jorion.game.thread.ThreadPoolManager;
import l2jorion.game.util.Util;
import l2jorion.log.Log;
import l2jorion.util.random.Rnd;

public class Baium extends Quest implements Runnable
{
	private L2Character _actualVictim;
	private L2PcInstance _waker;
	
	private static final int STONE_BAIUM = 29025;
	private static final int LIVE_BAIUM = 29020;
	private static final int ARCHANGEL = 29021;
	private static final int ANGELIC_VORTEX = 31862;
	
	// Baium status tracking
	private static final byte ASLEEP = 0; // baium is in the stone version, waiting to be woken up. Entry is unlocked.
	private static final byte AWAKE = 1; // baium is awake and fighting. Entry is locked.
	private static final byte DEAD = 2; // baium has been killed and has not yet spawned. Entry is locked.
	
	private final SimpleDateFormat date = new SimpleDateFormat("H:mm:ss yyyy/MM/dd");
	
	// Archangels spawns
	private final static int ANGEL_LOCATION[][] =
	{
		{
			114239,
			17168,
			10080,
			63544
		},
		{
			115780,
			15564,
			10080,
			13620
		},
		{
			114880,
			16236,
			10080,
			5400
		},
		{
			115168,
			17200,
			10080,
			0
		},
		{
			115792,
			16608,
			10080,
			0
		},
	};
	
	protected long _LastAttackVsBaiumTime = 0;
	private final List<L2NpcInstance> _Minions = new ArrayList<>(5);
	protected L2BossZone _Zone;
	
	public Baium(int questId, String name, String descr)
	{
		super(questId, name, descr);
		
		int[] mob =
		{
			LIVE_BAIUM
		};
		this.registerMobs(mob);
		
		// Quest NPC starter initialization
		addStartNpc(STONE_BAIUM);
		addStartNpc(ANGELIC_VORTEX);
		addTalkId(STONE_BAIUM);
		addTalkId(ANGELIC_VORTEX);
		
		_Zone = GrandBossManager.getInstance().getZone(113100, 14500, 10077);
		
		StatsSet info = GrandBossManager.getInstance().getStatsSet(LIVE_BAIUM);
		Integer status = GrandBossManager.getInstance().getBossStatus(LIVE_BAIUM);
		
		if (status == DEAD)
		{
			// load the unlock date and time for baium from DB
			long temp = (info.getLong("respawn_time") - System.currentTimeMillis());
			if (temp > 0)
			{
				// The time has not yet expired. Mark Baium as currently locked (dead).
				startQuestTimer("baium_unlock", temp, null, null, false);
			}
			else
			{
				// The time has expired while the server was offline. Spawn the stone-baium as ASLEEP.
				addSpawn(STONE_BAIUM, 116033, 17447, 10104, 40188, false, 0);
				GrandBossManager.getInstance().setBossStatus(LIVE_BAIUM, ASLEEP);
			}
		}
		else if (status == AWAKE)
		{
			final int loc_x = info.getInteger("loc_x");
			final int loc_y = info.getInteger("loc_y");
			final int loc_z = info.getInteger("loc_z");
			final int heading = info.getInteger("heading");
			final int hp = info.getInteger("currentHP");
			final int mp = info.getInteger("currentMP");
			
			L2GrandBossInstance baium = (L2GrandBossInstance) addSpawn(LIVE_BAIUM, loc_x, loc_y, loc_z, heading, false, 0);
			GrandBossManager.getInstance().addBoss(baium);
			
			baium.setCurrentHpMp(hp, mp);
			baium.setRunning();
			
			// start monitoring baium's inactivity
			_LastAttackVsBaiumTime = System.currentTimeMillis();
			startQuestTimer("baium_despawn", 60000, baium, null, true);
			startQuestTimer("skill_range", 2000, baium, null, true);
			
			// Spawns angels
			for (int[] element : ANGEL_LOCATION)
			{
				L2NpcInstance angel = addSpawn(ARCHANGEL, element[0], element[1], element[2], element[3], false, 0);
				angel.setRunning();
				angel.setIsInvul(true);
				_Minions.add(angel);
			}
			
			// Angels AI
			startQuestTimer("angels_aggro_reconsider", 5000, null, null, true);
		}
		else
		{
			addSpawn(STONE_BAIUM, 116033, 17447, 10104, 40188, false, 0);
		}
	}
	
	@Override
	public String onAdvEvent(String event, L2NpcInstance npc, L2PcInstance player)
	{
		if ((npc != null) && (npc.getNpcId() == LIVE_BAIUM))
		{
			if (event.equalsIgnoreCase("skill_range"))
			{
				callSkillAI(npc);
			}
			else if (event.equalsIgnoreCase("baium_neck"))
			{
				npc.broadcastPacket(new SocialAction(npc.getObjectId(), 3));
			}
			else if (event.equalsIgnoreCase("sacrifice_waker"))
			{
				if (_waker != null)
				{
					
					// 60% to die.
					if (Rnd.get(100) < 60)
					{
						_waker.getStatus().setCurrentHp(0);
						_waker.doDie(npc);
					}
				}
			}
			else if (event.equalsIgnoreCase("baium_roar"))
			{
				// Roar animation
				npc.broadcastPacket(new SocialAction(npc.getObjectId(), 1));
				
				// Spawn angels
				for (int[] element : ANGEL_LOCATION)
				{
					L2NpcInstance angel = addSpawn(ARCHANGEL, element[0], element[1], element[2], element[3], false, 0);
					angel.setRunning();
					angel.setIsInvul(true);
					_Minions.add(angel);
				}
				
				// Angels AI
				startQuestTimer("angels_aggro_reconsider", 5000, null, null, true);
			}
			// despawn the live baium after 30 minutes of inactivity
			// also check if the players are cheating, having pulled Baium outside his zone...
			else if (event.equalsIgnoreCase("baium_despawn"))
			{
				// just in case the zone reference has been lost (somehow...), restore the reference
				if (_Zone == null)
				{
					_Zone = GrandBossManager.getInstance().getZone(113100, 14500, 10077);
				}
				
				if (_LastAttackVsBaiumTime + Config.BAIUM_SLEEP * 1000 < System.currentTimeMillis())
				{
					// despawn the live-baium
					npc.deleteMe();
					
					// Unspawn angels
					for (L2NpcInstance minion : _Minions)
					{
						if (minion != null)
						{
							minion.getSpawn().stopRespawn();
							minion.deleteMe();
						}
					}
					_Minions.clear();
					
					addSpawn(STONE_BAIUM, 116033, 17447, 10104, 40188, false, 0); // spawn stone-baium
					GrandBossManager.getInstance().setBossStatus(LIVE_BAIUM, ASLEEP); // Baium isn't awaken anymore
					_Zone.oustAllPlayers();
					cancelQuestTimer("baium_despawn", npc, null);
				}
				else if (((_LastAttackVsBaiumTime + 300000) < System.currentTimeMillis()) && (npc.getCurrentHp() < ((npc.getMaxHp() * 3) / 4.0)))
				{
					npc.setTarget(npc);
					L2Skill skill = SkillTable.getInstance().getInfo(4135, 1);
					npc.doCast(skill);
				}
				else if (!_Zone.isInsideZone(npc))
				{
					npc.teleToLocation(116033, 17447, 10104);
				}
			}
		}
		else if (event.equalsIgnoreCase("baium_unlock"))
		{
			GrandBossManager.getInstance().setBossStatus(LIVE_BAIUM, ASLEEP);
			addSpawn(STONE_BAIUM, 116033, 17447, 10104, 40188, false, 0);
		}
		else if (event.equalsIgnoreCase("angels_aggro_reconsider"))
		{
			boolean updateTarget = false; // Update or no the target
			
			for (L2NpcInstance minion : _Minions)
			{
				L2Attackable angel = ((L2Attackable) minion);
				if (angel == null)
				{
					continue;
				}
				
				L2Character victim = angel.getMostHated();
				
				if (Rnd.get(100) < 10)
				{
					updateTarget = true;
				}
				else
				{
					if (victim != null) // Target is a unarmed player ; clean aggro.
					{
						if ((victim instanceof L2PcInstance) && (victim.getActiveWeaponInstance() == null))
						{
							angel.stopHating(victim); // Clean the aggro number of previous victim.
							updateTarget = true;
						}
					}
					else
					{
						// No target currently.
						updateTarget = true;
					}
				}
				
				if (updateTarget)
				{
					L2Character newVictim = getRandomTargetArchangel(minion);
					if ((newVictim != null) && (victim != newVictim))
					{
						angel.setIsRunning(true);
						angel.addDamageHate(newVictim, 0, 999);
						angel.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, newVictim);
					}
				}
			}
		}
		return super.onAdvEvent(event, npc, player);
	}
	
	@Override
	public String onTalk(L2NpcInstance npc, L2PcInstance player)
	{
		int npcId = npc.getNpcId();
		String htmltext = null;
		
		if (_Zone == null)
		{
			_Zone = GrandBossManager.getInstance().getZone(113100, 14500, 10077);
			
			if (_Zone == null)
			{
				return "<html><body>Angelic Vortex:<br>You may not enter while admin disabled this zone.</body></html>";
			}
		}
		
		Integer status = GrandBossManager.getInstance().getBossStatus(LIVE_BAIUM);
		
		if (npcId == STONE_BAIUM && status == ASLEEP)
		{
			if (_Zone.isPlayerAllowed(player))
			{
				GrandBossManager.getInstance().setBossStatus(LIVE_BAIUM, AWAKE);
				npc.deleteMe();
				
				L2GrandBossInstance baium = (L2GrandBossInstance) addSpawn(LIVE_BAIUM, npc);
				GrandBossManager.getInstance().addBoss(baium);
				
				// Baium is stuck for the following time : 35secs
				ThreadPoolManager.getInstance().scheduleGeneral(new Runnable()
				{
					@Override
					public void run()
					{
						baium.setIsInvul(false);
						baium.getAttackByList().addAll(_Zone.getCharactersInside());
						
						// Start monitoring baium's inactivity and activate the AI
						_LastAttackVsBaiumTime = System.currentTimeMillis();
						startQuestTimer("baium_despawn", 60000, baium, null, true);
						startQuestTimer("skill_range", 2000, baium, null, true);
					}
				}, 10000L);
				
				// First animation
				baium.setIsInvul(true);
				baium.setRunning();
				baium.broadcastPacket(new SocialAction(baium.getObjectId(), 2));
				baium.broadcastPacket(new Earthquake(baium.getX(), baium.getY(), baium.getZ(), 40, 10));
				
				_waker = player;
				
				// Second animation, waker sacrifice, followed by angels spawn and third animation.
				startQuestTimer("baium_neck", 13000, baium, null, false);
				startQuestTimer("sacrifice_waker", 1000, baium, null, false);
				startQuestTimer("baium_roar", 15000, baium, null, false);
			}
			else
			{
				htmltext = "Conditions are not right to wake up Baium";
			}
		}
		else if (npcId == ANGELIC_VORTEX)
		{
			if (player.isFlying())
			{
				return "<html><body>Angelic Vortex:<br>You may not enter while flying a wyvern</body></html>";
			}
			if ((GrandBossManager.getInstance().getBossStatus(LIVE_BAIUM) == ASLEEP) && player.getQuestState("baium").getQuestItemsCount(4295) > 0) // bloody fabric
			{
				player.getQuestState("baium").takeItems(4295, 1);
				// allow entry for the player for the next 30 secs (more than enough time for the TP to happen)
				// Note: this just means 30secs to get in, no limits on how long it takes before we get out.
				_Zone.allowPlayerEntry(player, 30);
				player.teleToLocation(113100, 14500, 10077);
			}
			else
			{
				npc.showChatWindow(player, 1);
			}
		}
		return htmltext;
	}
	
	@Override
	public String onAttack(L2NpcInstance npc, L2PcInstance attacker, int damage, boolean isPet)
	{
		if (!_Zone.isInsideZone(attacker))
		{
			attacker.reduceCurrentHp(attacker.getCurrentHp(), attacker, false);
			return super.onAttack(npc, attacker, damage, isPet);
		}
		
		if (npc.isInvul())
		{
			npc.getAI().setIntention(AI_INTENTION_IDLE);
			return super.onAttack(npc, attacker, damage, isPet);
		}
		
		if (npc.getNpcId() == LIVE_BAIUM && !npc.isInvul())
		{
			if (attacker.getMountType() == 1)
			{
				L2Skill skill = SkillTable.getInstance().getInfo(4258, 1);
				if (attacker.getFirstEffect(skill) == null)
				{
					npc.setTarget(attacker);
					npc.doCast(skill);
				}
			}
			// update a variable with the last action against baium
			_LastAttackVsBaiumTime = System.currentTimeMillis();
		}
		
		return super.onAttack(npc, attacker, damage, isPet);
	}
	
	@Override
	public String onKill(L2NpcInstance npc, L2PcInstance killer, boolean isPet)
	{
		if (npc.getNpcId() == LIVE_BAIUM)
		{
			npc.broadcastPacket(new PlaySound(1, "BS01_D", 1, npc.getObjectId(), npc.getX(), npc.getY(), npc.getZ()));
			
			if (!npc.getSpawn().is_customBossInstance())
			{
				GrandBossManager.getInstance().setBossStatus(LIVE_BAIUM, DEAD);
				cancelQuestTimer("baium_despawn", npc, null);
				addSpawn(29055, 115203, 16620, 10078, 0, false, 900000);
				
				long respawnTime = (Config.BAIUM_RESP_FIRST + (Config.BAIUM_RESP_SECOND == 0 ? 0 : Rnd.get(1, Config.BAIUM_RESP_SECOND))) * 3600000;
				final int days = Config.BAIUM_FIX_TIME_D * 24;
				
				Calendar time = Calendar.getInstance();
				time.add(Calendar.HOUR, days);
				time.set(Calendar.HOUR_OF_DAY, Config.BAIUM_FIX_TIME_H);
				time.set(Calendar.MINUTE, Rnd.get(0, Config.BAIUM_FIX_TIME_M));
				time.set(Calendar.SECOND, Rnd.get(0, Config.BAIUM_FIX_TIME_S));
				
				long _respawnEnd = time.getTimeInMillis();
				long _respawn = time.getTimeInMillis() - Calendar.getInstance().getTimeInMillis();
				
				GregorianCalendar gc = (GregorianCalendar) Calendar.getInstance();
				gc.clear();
				
				StatsSet info = GrandBossManager.getInstance().getStatsSet(LIVE_BAIUM);
				
				if (Config.BAIUM_FIX_TIME)
				{
					startQuestTimer("baium_unlock", _respawn, null, null);
					info.set("respawn_time", _respawnEnd);
					gc.setTimeInMillis(_respawnEnd);
				}
				else
				{
					startQuestTimer("baium_unlock", respawnTime, null, null);
					info.set("respawn_time", (System.currentTimeMillis() + respawnTime));
					gc.setTimeInMillis((System.currentTimeMillis() + respawnTime));
				}
				
				String text = "Baium killed. Next respawn: " + DateFormat.getDateTimeInstance().format(gc.getTime());
				Log.add(text, "GrandBosses");
				info.set("killed_time", "" + date.format(new Date(System.currentTimeMillis())));
				info.set("next_respawn", DateFormat.getDateTimeInstance().format(gc.getTime()));
				
				GrandBossManager.getInstance().setStatsSet(LIVE_BAIUM, info);
				
				// Unspawn angels.
				synchronized (_Minions)
				{
					if (_Minions.size() > 0)
					{
						for (L2NpcInstance minion : _Minions)
						{
							
							minion.getSpawn().stopRespawn();
							minion.deleteMe();
						}
						_Minions.clear();
					}
				}
			}
		}
		
		// Clean Baium AI
		cancelQuestTimer("skill_range", npc, null);
		
		// Clean angels AI
		cancelQuestTimer("angels_aggro_reconsider", null, null);
		
		return super.onKill(npc, killer, isPet);
	}
	
	/**
	 * This method allows to select a random target, and is used both for Baium and angels.
	 * @param npc to check.
	 * @return the random target.
	 */
	private L2Character getRandomTargetBaium(L2NpcInstance npc)
	{
		int npcId = npc.getNpcId();
		
		List<L2Character> result = new ArrayList<>();
		Collection<L2Object> objs = npc.getKnownList().getKnownObjects().values();
		for (L2Object obj : objs)
		{
			// Case of Archangels, they can hit Baium.
			if ((obj instanceof L2GrandBossInstance) && (npcId == ARCHANGEL))
			{
				result.add((L2Character) obj);
			}
			else if (obj instanceof L2PlayableInstance)
			{
				if (((L2Character) obj).isDead() || !(GeoData.getInstance().canSeeTarget(npc, obj)))
				{
					continue;
				}
				
				if (obj instanceof L2PcInstance)
				{
					if (((L2PcInstance) obj).getAppearance().getInvisible())
					{
						continue;
					}
					
					if ((npcId == ARCHANGEL) && (((L2PcInstance) obj).getActiveWeaponInstance() == null))
					{
						continue;
					}
				}
				else if (obj instanceof L2PetInstance)
				{
					continue;
				}
				
				result.add((L2Character) obj);
			}
		}
		
		// If there's no players available, Baium and Angels are hitting each other.
		if (result.isEmpty())
		{
			if (npcId == LIVE_BAIUM) // Case of Baium. Angels should never be without target.
			{
				for (L2NpcInstance minion : _Minions)
				{
					if (minion != null)
					{
						result.add(minion);
					}
				}
			}
		}
		return (result.isEmpty()) ? null : result.get(Rnd.get(result.size()));
	}
	
	private L2Character getRandomTargetArchangel(L2NpcInstance npc)
	{
		List<L2Character> result = new ArrayList<>();
		Collection<L2Object> objs = npc.getKnownList().getKnownObjects().values();
		for (L2Object obj : objs)
		{
			// Case of Archangels, they can hit Baium.
			if (obj instanceof L2GrandBossInstance)
			{
				result.add((L2Character) obj);
			}
			else if (obj instanceof L2PlayableInstance)
			{
				if (((L2Character) obj).isDead() || !(GeoData.getInstance().canSeeTarget(npc, obj)))
				{
					continue;
				}
				
				if (obj instanceof L2PcInstance)
				{
					if (!((L2PcInstance) obj).getAppearance().getInvisible())
					{
						continue;
					}
					
					if ((((L2PcInstance) obj).getActiveWeaponInstance() == null))
					{
						continue;
					}
				}
				else if (obj instanceof L2PetInstance)
				{
					continue;
				}
				result.add((L2Character) obj);
			}
		}
		return (result.isEmpty()) ? null : result.get(Rnd.get(result.size()));
	}
	
	/**
	 * That method checks if angels are near.
	 * @param npc : baium.
	 * @return the number of angels surrounding the target.
	 */
	private int getSurroundingAngelsNumber(L2NpcInstance npc)
	{
		int count = 0;
		
		for (L2Object obj : npc.getKnownList().getKnownObjects().values())
		{
			if (obj instanceof L2MonsterInstance)
			{
				if (((L2NpcInstance) obj).getNpcId() == ARCHANGEL)
				{
					if (Util.checkIfInRange(600, npc, obj, true))
					{
						count++;
					}
				}
			}
		}
		return count;
	}
	
	/**
	 * The personal casting AI for Baium.
	 * @param npc baium, basically...
	 */
	private void callSkillAI(L2NpcInstance npc)
	{
		if (npc.isInvul() || npc.isCastingNow())
		{
			return;
		}
		
		// Pickup a target if no or dead victim. If Baium was hitting an angel, 15% luck he reconsiders his target. 10% luck he decides to reconsiders his target.
		if ((_actualVictim == null) || _actualVictim.isDead() || !(npc.getKnownList().knowsObject(_actualVictim)) || ((_actualVictim instanceof L2MonsterInstance) && (Rnd.get(100) < 15)) || (Rnd.get(10) == 0))
		{
			_actualVictim = getRandomTargetBaium(npc);
		}
		
		// If result is null, return directly.
		if (_actualVictim == null)
		{
			return;
		}
		
		if (Rnd.get(100) < 30)
		{
			final L2Skill skill = SkillTable.getInstance().getInfo(getRandomSkill(npc), 1);
			// Adapt the skill range, because Baium is fat.
			if (Util.checkIfInRange(skill.getCastRange() + npc.getCollisionRadius(), npc, _actualVictim, true))
			{
				npc.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
				npc.setTarget(skill.getId() == 4135 ? npc : _actualVictim);
				npc.doCast(skill);
			}
			else
			{
				npc.getAI().setIntention(CtrlIntention.AI_INTENTION_FOLLOW, _actualVictim, null);
			}
		}
		else
		{
			if (Util.checkIfInRange(40 + npc.getCollisionRadius(), npc, _actualVictim, true))
			{
				npc.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, _actualVictim);
			}
			else
			{
				npc.getAI().setIntention(CtrlIntention.AI_INTENTION_FOLLOW, _actualVictim, null);
			}
		}
		
	}
	
	/**
	 * Pick a random skill through that list.<br>
	 * If Baium feels surrounded, he will use AoE skills. Same behavior if he is near 2+ angels.<br>
	 * @param npc baium
	 * @return a usable skillId
	 */
	private int getRandomSkill(L2NpcInstance npc)
	{
		// Baium's selfheal. It happens exceptionaly.
		if (npc.getCurrentHp() < (npc.getMaxHp() / 10))
		{
			if (Rnd.get(10000) == 777)
			{
				return 4135;
			}
		}
		
		int skill = 4127; // Default attack if nothing is possible.
		final int chance = Rnd.get(100); // Remember, it's 0 to 99, not 1 to 100.
		
		// If Baium feels surrounded or see 2+ angels, he unleashes his wrath upon heads :).
		if ((Util.getPlayersCountInRadius(600, npc, false) >= 20) || (getSurroundingAngelsNumber(npc) >= 2))
		{
			if (chance < 25)
			{
				skill = 4130;
			}
			else if ((chance >= 25) && (chance < 50))
			{
				skill = 4131;
			}
			else if ((chance >= 50) && (chance < 75))
			{
				skill = 4128;
			}
			else if ((chance >= 75) && (chance < 100))
			{
				skill = 4129;
			}
		}
		else
		{
			if (npc.getCurrentHp() > ((npc.getMaxHp() * 3) / 4)) // > 75%
			{
				if (chance < 10)
				{
					skill = 4128;
				}
				else if ((chance >= 10) && (chance < 20))
				{
					skill = 4129;
				}
			}
			else if (npc.getCurrentHp() > ((npc.getMaxHp() * 2) / 4)) // > 50%
			{
				if (chance < 10)
				{
					skill = 4131;
				}
				else if ((chance >= 10) && (chance < 20))
				{
					skill = 4128;
				}
				else if ((chance >= 20) && (chance < 30))
				{
					skill = 4129;
				}
			}
			else if (npc.getCurrentHp() > (npc.getMaxHp() / 4)) // > 25%
			{
				if (chance < 10)
				{
					skill = 4130;
				}
				else if ((chance >= 10) && (chance < 20))
				{
					skill = 4131;
				}
				else if ((chance >= 20) && (chance < 30))
				{
					skill = 4128;
				}
				else if ((chance >= 30) && (chance < 40))
				{
					skill = 4129;
				}
			}
			else
			// < 25%
			{
				if (chance < 10)
				{
					skill = 4130;
				}
				else if ((chance >= 10) && (chance < 20))
				{
					skill = 4131;
				}
				else if ((chance >= 20) && (chance < 30))
				{
					skill = 4128;
				}
				else if ((chance >= 30) && (chance < 40))
				{
					skill = 4129;
				}
			}
		}
		return skill;
	}
	
	@Override
	public void run()
	{
	}
}