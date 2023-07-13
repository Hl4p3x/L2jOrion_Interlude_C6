package l2jorion.game.ai;

import static l2jorion.game.ai.CtrlIntention.AI_INTENTION_ACTIVE;
import static l2jorion.game.ai.CtrlIntention.AI_INTENTION_ATTACK;
import static l2jorion.game.ai.CtrlIntention.AI_INTENTION_IDLE;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;

import l2jorion.Config;
import l2jorion.game.controllers.GameTimeController;
import l2jorion.game.datatables.sql.NpcTable;
import l2jorion.game.datatables.sql.TerritoryTable;
import l2jorion.game.geo.GeoData;
import l2jorion.game.managers.DimensionalRiftManager;
import l2jorion.game.model.L2Attackable;
import l2jorion.game.model.L2Character;
import l2jorion.game.model.L2Effect.EffectType;
import l2jorion.game.model.L2Object;
import l2jorion.game.model.L2Skill;
import l2jorion.game.model.L2Skill.SkillTargetType;
import l2jorion.game.model.L2Skill.SkillType;
import l2jorion.game.model.L2Summon;
import l2jorion.game.model.Location;
import l2jorion.game.model.actor.instance.L2ChestInstance;
import l2jorion.game.model.actor.instance.L2DoorInstance;
import l2jorion.game.model.actor.instance.L2FestivalMonsterInstance;
import l2jorion.game.model.actor.instance.L2FolkInstance;
import l2jorion.game.model.actor.instance.L2FriendlyMobInstance;
import l2jorion.game.model.actor.instance.L2GrandBossInstance;
import l2jorion.game.model.actor.instance.L2GuardInstance;
import l2jorion.game.model.actor.instance.L2MinionInstance;
import l2jorion.game.model.actor.instance.L2MonsterInstance;
import l2jorion.game.model.actor.instance.L2NpcInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.actor.instance.L2PlayableInstance;
import l2jorion.game.model.actor.instance.L2RaidBossInstance;
import l2jorion.game.model.actor.instance.L2RiftInvaderInstance;
import l2jorion.game.model.entity.Announcements;
import l2jorion.game.model.quest.Quest;
import l2jorion.game.templates.L2NpcTemplate;
import l2jorion.game.templates.L2NpcTemplate.AIType;
import l2jorion.game.thread.ThreadPoolManager;
import l2jorion.game.util.Util;
import l2jorion.util.random.Rnd;

public class L2AttackableAI extends L2CharacterAI implements Runnable
{
	private static int _attacked;
	private static Set<Integer> _tracking = new HashSet<>();
	
	private static final int RANDOM_WALK_RATE = 30;
	private static final int MAX_ATTACK_TIMEOUT = 1500;
	private static final int RUN_TIMEOUT = 1200;
	
	private Future<?> _aiTask;
	private int _attackTimeout;
	private int _runTimeout;
	
	private int _globalAggro;
	
	private boolean _thinking;
	
	private int chaostime = 0;
	int lastBuffTick;
	
	private final L2NpcTemplate _skillrender;
	
	private List<L2Skill> shortRangeSkills = new ArrayList<>();
	private List<L2Skill> longRangeSkills = new ArrayList<>();
	
	public L2AttackableAI(L2Character creature)
	{
		super(creature);
		
		_skillrender = NpcTable.getInstance().getTemplate(getActiveChar().getTemplate().getNpcId());
		
		_attackTimeout = Integer.MAX_VALUE;
		_runTimeout = Integer.MAX_VALUE;
		_globalAggro = -5; // default: -5 timeout of ATTACK after respawn
	}
	
	@Override
	public void run()
	{
		onEvtThink();
	}
	
	private boolean autoAttackCondition(L2Character target)
	{
		if (target == null || _actor == null)
		{
			return false;
		}
		
		L2Attackable creature = (L2Attackable) _actor;
		
		if (target instanceof L2FolkInstance || target instanceof L2DoorInstance)
		{
			return false;
		}
		
		if (creature.isMinion())
		{
			if (!target.isInsideRadius(creature.getLeader().getSpawn().getLocx(), creature.getLeader().getSpawn().getLocy(), creature.getAggroRange(), false))
			{
				return false;
			}
		}
		
		if (creature.getSpawn() != null)
		{
			if (!target.isInsideRadius(creature.getSpawn().getLocx(), creature.getSpawn().getLocy(), creature.getAggroRange(), false))
			{
				return false;
			}
		}
		
		if (target.isAlikeDead() || !creature.isInsideRadius(target, creature.getAggroRange(), false, false) || Math.abs(creature.getZ() - target.getZ()) > 300)
		{
			return false;
		}
		
		// Check if the target is a L2PcInstance
		if (target instanceof L2PcInstance)
		{
			L2PcInstance targetPlayer = (L2PcInstance) target;
			
			if (!(creature instanceof L2RaidBossInstance) && targetPlayer.isSilentMoving())
			{
				return false;
			}
			
			if (targetPlayer.isInvul() && targetPlayer.isGM() || targetPlayer.isInOfflineMode() || targetPlayer.isRecentFakeDeath())
			{
				return false;
			}
			
			if (creature.getFactionId() != null && creature.getFactionId().equals("varka") && targetPlayer.isAlliedWithVarka())
			{
				return false;
			}
			
			if (creature.getFactionId() != null && creature.getFactionId().equals("ketra") && targetPlayer.isAlliedWithKetra())
			{
				return false;
			}
			
			if (Config.GGAMES_EU_CUSTOM)
			{
				if (!targetPlayer.isMoving())
				{
					return false;
				}
			}
			
			if (targetPlayer.isInParty() && targetPlayer.getParty().isInDimensionalRift())
			{
				byte riftType = targetPlayer.getParty().getDimensionalRift().getType();
				byte riftRoom = targetPlayer.getParty().getDimensionalRift().getCurrentRoom();
				
				if (creature instanceof L2RiftInvaderInstance && !DimensionalRiftManager.getInstance().getRoom(riftType, riftRoom).checkIfInZone(creature.getX(), creature.getY(), creature.getZ()))
				{
					return false;
				}
			}
		}
		// Check if the target is a L2Summon
		if (target instanceof L2Summon)
		{
			L2PcInstance owner = ((L2Summon) target).getOwner();
			if (owner != null)
			{
				if (owner.isGM() && (owner.isInvul() || !owner.getAccessLevel().canTakeAggro()))
				{
					return false;
				}
				
				if (creature.getFactionId() != null && creature.getFactionId() == "varka" && owner.isAlliedWithVarka())
				{
					return false;
				}
				
				if (creature.getFactionId() != null && creature.getFactionId() == "ketra" && owner.isAlliedWithKetra())
				{
					return false;
				}
			}
		}
		
		// Check if the actor is a L2GuardInstance
		if (_actor instanceof L2GuardInstance)
		{
			// Check if the L2PcInstance target has karma
			if (target instanceof L2PcInstance && ((L2PcInstance) target).getKarma() > 0)
			{
				return GeoData.getInstance().canSeeTarget(creature, target);
			}
			
			if (Config.L2UNLIMITED_CUSTOM)
			{
				if (target instanceof L2PcInstance && ((L2PcInstance) target).getPvpFlag() != 0)
				{
					return GeoData.getInstance().canSeeTarget(creature, target);
				}
			}
			
			// Check if the L2MonsterInstance target is aggressive
			if (target instanceof L2MonsterInstance && Config.ALLOW_GUARDS)
			{
				return ((L2MonsterInstance) target).isAggressive() && GeoData.getInstance().canSeeTarget(creature, target);
			}
			
			return false;
		}
		else if (_actor instanceof L2FriendlyMobInstance)
		{
			if (target instanceof L2NpcInstance)
			{
				return false;
			}
			
			if (target instanceof L2PcInstance && ((L2PcInstance) target).getKarma() > 0)
			{
				return GeoData.getInstance().canSeeTarget(creature, target);
			}
			
			return false;
		}
		else
		{
			// Cant attack in the same faction
			if (target instanceof L2Attackable)
			{
				if (((L2Attackable) _actor).getFactionId() != null && ((L2Attackable) _actor).getFactionId().equals(((L2Attackable) target).getFactionId()))
				{
					return false;
				}
				
				if (!target.isAutoAttackable(_actor))
				{
					return false;
				}
			}
			
			// Check if the target isn't another L2NpcInstance
			if (target instanceof L2Attackable || target instanceof L2NpcInstance)
			{
				return false;
			}
			
			if (L2Character.isInsidePeaceZone(creature, target))
			{
				return false;
			}
			
			return (creature.isAggressive() || creature.isBatNightMode()) && GeoData.getInstance().canSeeTarget(creature, target);
		}
	}
	
	@Override
	protected void onEvtDead()
	{
		stopAITask();
		
		super.onEvtDead();
	}
	
	@Override
	synchronized void changeIntention(CtrlIntention intention, Object arg0, Object arg1)
	{
		if (intention == AI_INTENTION_IDLE || intention == AI_INTENTION_ACTIVE)
		{
			if (!_actor.isAlikeDead())
			{
				L2Attackable npc = (L2Attackable) _actor;
				
				if (npc.getKnownList().getKnownPlayers().size() > 0)
				{
					intention = AI_INTENTION_ACTIVE;
				}
			}
			
			if (intention == AI_INTENTION_IDLE)
			{
				super.changeIntention(AI_INTENTION_IDLE, null, null);
				
				stopAITask();
				
				// Cancel the AI
				_actor.detachAI();
				
				return;
			}
		}
		
		super.changeIntention(intention, arg0, arg1);
		
		if (!_actor.isAlikeDead())
		{
			startAITask();
		}
	}
	
	public void startAITask()
	{
		if (_aiTask == null)
		{
			_aiTask = ThreadPoolManager.getInstance().scheduleAiAtFixedRate(this, 1000, 1000);
		}
	}
	
	@Override
	public void stopAITask()
	{
		if (_aiTask != null)
		{
			_aiTask.cancel(false);
			_aiTask = null;
		}
		
		super.stopAITask();
	}
	
	@Override
	protected void onIntentionAttack(L2Character target)
	{
		L2Attackable npc = getActiveChar();
		
		_attackTimeout = MAX_ATTACK_TIMEOUT + GameTimeController.getInstance().getGameTicks();
		_runTimeout = RUN_TIMEOUT + GameTimeController.getInstance().getGameTicks();
		
		if (!_skillrender.getBuffSkills().isEmpty())
		{
			if (Rnd.get(100) < Rnd.get(npc.getMinSkillChance(), npc.getMaxSkillChance()))
			{
				for (L2Skill sk : _skillrender.getBuffSkills())
				{
					if (cast(sk))
					{
						break;
					}
				}
			}
		}
		
		super.onIntentionAttack(target);
	}
	
	protected void thinkCast()
	{
		if (checkTargetLost(getTarget()))
		{
			setTarget(null);
			return;
		}
		
		if (maybeMoveToPawn(getTarget(), _actor.getMagicalAttackRange(_skill)))
		{
			return;
		}
		
		clientStopMoving(null);
		setIntention(AI_INTENTION_ACTIVE);
		_actor.doCast(_skill);
	}
	
	private void thinkActive()
	{
		L2Attackable npc = (L2Attackable) _actor;
		
		if (_globalAggro != 0)
		{
			if (_globalAggro < 0)
			{
				_globalAggro++;
			}
			else
			{
				_globalAggro--;
			}
		}
		
		if (_globalAggro >= 0)
		{
			for (L2Object obj : npc.getKnownList().getKnownObjects().values())
			{
				if (obj == null || !(obj instanceof L2Character))
				{
					continue;
				}
				
				L2Character target = (L2Character) obj;
				
				if (npc instanceof L2FestivalMonsterInstance && obj instanceof L2PcInstance)
				{
					L2PcInstance targetPlayer = (L2PcInstance) obj;
					
					if (!targetPlayer.isFestivalParticipant())
					{
						continue;
					}
				}
				
				if (autoAttackCondition(target)) // check aggression
				{
					int hating = npc.getHating(target);
					
					if (hating == 0)
					{
						switch (npc.getNpcId())
						{
							// MOS creatures
							case 22124:
							case 22125:
							case 22126:
							case 22127:
							case 22129:
							{
								npc.addDamageHate(target, 0, 0);
								break;
							}
							default:
							{
								npc.addDamageHate(target, 0, 1);
								break;
							}
						}
					}
				}
			}
			
			// Lets give some buffs for active mobs
			if (!_skillrender.getBuffSkills().isEmpty())
			{
				if (Rnd.get(100) < Rnd.get(npc.getMinSkillChance(), npc.getMaxSkillChance()))
				{
					for (L2Skill sk : _skillrender.getBuffSkills())
					{
						if (cast(sk))
						{
							break;
						}
					}
				}
			}
			
			// Chose a target from its aggroList
			L2Character hated;
			
			if (_actor.isConfused())
			{
				hated = (L2Character) getTarget();
			}
			else
			{
				hated = npc.getMostHated();
			}
			
			// Order to the L2Attackable to attack the target
			if (hated != null)
			{
				int aggro = npc.getHating(hated);
				
				if (aggro + _globalAggro > 0)
				{
					if (_actor.isMoving())
					{
						_actor.stopMove(null);
					}
					
					if (!_actor.isRunning())
					{
						_actor.setRunning();
					}
					
					setIntention(CtrlIntention.AI_INTENTION_ATTACK, hated);
				}
				return;
			}
		}
		
		// If this is a festival monster, then it remains in the same location.
		if (_actor instanceof L2FestivalMonsterInstance)
		{
			return;
		}
		
		// Minions following leader
		if (_actor instanceof L2MinionInstance && ((L2MinionInstance) _actor).getLeader() != null)
		{
			int offset;
			
			// for Raids - need correction
			if (_actor.isRaid())
			{
				offset = 500;
			}
			else
			{
				// for normal minions
				offset = 200;
			}
			
			if (((L2MinionInstance) _actor).getLeader().isInCombat())
			{
				offset = 60;
			}
			
			if (((L2MinionInstance) _actor).getLeader().isRunning())
			{
				_actor.setRunning();
			}
			else
			{
				_actor.setWalking();
			}
			
			if (_actor.getPlanDistanceSq(((L2MinionInstance) _actor).getLeader()) > offset * offset)
			{
				int x1, y1, z1;
				
				x1 = ((L2MinionInstance) _actor).getLeader().getX() + Rnd.nextInt((offset - 30) * 2) - (offset - 30);
				y1 = ((L2MinionInstance) _actor).getLeader().getY() + Rnd.nextInt((offset - 30) * 2) - (offset - 30);
				z1 = ((L2MinionInstance) _actor).getLeader().getZ();
				
				moveTo(x1, y1, z1);
				return;
			}
		}
		
		// Order to the L2MonsterInstance to random walk (1/100)
		else if (!(npc instanceof L2ChestInstance) && !(npc instanceof L2GuardInstance) && npc.getSpawn() != null && Rnd.nextInt(RANDOM_WALK_RATE) == 0 && npc.getSpawn().isRandomWalk())
		{
			int x1 = 0, y1 = 0, z1 = 0;
			final int range = Config.MAX_DRIFT_RANGE;
			
			// If NPC with random coord in territory
			if (npc.getSpawn().getLocx() == 0 && npc.getSpawn().getLocy() == 0)
			{
				// If NPC with random fixed coord, don't move
				if (TerritoryTable.getInstance().getProcMax(npc.getSpawn().getLocation()) > 0)
				{
					return;
				}
				
				// Calculate a destination point in the spawn area
				final Location location = TerritoryTable.getInstance().getRandomPoint(npc.getSpawn().getLocation());
				
				// Set the calculated position of the L2NpcInstance
				if (location != null)
				{
					x1 = location.getX();
					y1 = location.getY();
					z1 = location.getZ();
				}
				
				// Calculate the distance between the current position of the L2Character and the target (x,y)
				double distance2 = _actor.getPlanDistanceSq(x1, y1);
				
				if (distance2 > ((range + range) * (range + range)))
				{
					npc.setisReturningToSpawnPoint(true);
					float delay = (float) Math.sqrt(distance2) / range;
					x1 = npc.getX() + (int) ((x1 - npc.getX()) / delay);
					y1 = npc.getY() + (int) ((y1 - npc.getY()) / delay);
				}
				else
				{
					npc.setisReturningToSpawnPoint(false);
				}
			}
			else
			{
				x1 = npc.getSpawn().getLocx();
				y1 = npc.getSpawn().getLocy();
				z1 = npc.getSpawn().getLocz();
				
				if (!npc.isInsideRadius(x1, y1, 0, range * 3, false, false))
				{
					npc.setisReturningToSpawnPoint(true);
				}
				else
				{
					int deltaX = Rnd.nextInt(range * 2);
					int deltaY = Rnd.get(deltaX, range * 2);
					deltaY = (int) Math.sqrt((deltaY * deltaY) - (deltaX * deltaX));
					x1 = (deltaX + x1) - range;
					y1 = (deltaY + y1) - range;
					z1 = npc.getZ();
				}
			}
			
			if (Config.MONSTER_RETURN_DELAY > 0 && npc instanceof L2MonsterInstance && !npc.isAlikeDead() && !npc.isDead() && npc.getSpawn() != null && !npc.isInsideRadius(npc.getSpawn().getLocx(), npc.getSpawn().getLocy(), range * 3, false))
			{
				npc.returnHome();
			}
			
			if (npc.isReturningToSpawnPoint())
			{
				try
				{
					moveTo(x1, y1, z1);
				}
				catch (Exception e)
				{
					LOG.warn("Something wrong on isReturningToSpawnPoint: " + npc.getName() + "(" + npc.getNpcId() + ")");
				}
			}
			else
			{
				if (GeoData.getInstance().canMove(npc.getX(), npc.getY(), npc.getZ(), x1, y1, z1, npc.getInstanceId()))
				{
					if (_actor.isRunning())
					{
						_actor.setWalking();
					}
					
					moveTo(x1, y1, z1);
				}
			}
		}
	}
	
	private void thinkAttack()
	{
		L2Attackable npc = getActiveChar();
		L2Character originalAttackTarget = (L2Character) getTarget();
		final int npcObjId = npc.getObjectId();
		
		if (npc.isCastingNow())
		{
			return;
		}
		
		if (npc.isAttackingDisabled())
		{
			return;
		}
		
		if (Config.ANNOUNCE_BOSS_UNDER_ATTACK && npc instanceof L2RaidBossInstance)
		{
			if (originalAttackTarget instanceof L2PlayableInstance)
			{
				if (!_tracking.contains(npcObjId))
				{
					_tracking.add(npcObjId);
					_attacked = npcObjId;
				}
				
				if (_attacked == npcObjId)
				{
					_attacked = 0;
					
					if (((L2PcInstance) originalAttackTarget).getClan() != null)
					{
						Announcements.getInstance().announceWithServerName("The Raid Boss " + npc.getName() + "  is under attack! First hit: " + originalAttackTarget.getName() + " Clan: " + ((L2PcInstance) originalAttackTarget).getClan().getName());
					}
					else
					{
						Announcements.getInstance().announceWithServerName("The Raid Boss " + npc.getName() + "  is under attack! First hit: " + originalAttackTarget.getName() + " Clan: -");
					}
				}
			}
		}
		
		if (_runTimeout < GameTimeController.getInstance().getGameTicks())
		{
			npc.setWalking();
		}
		else
		{
			npc.setRunning();
		}
		
		if (originalAttackTarget == null || originalAttackTarget.isAlikeDead() || _attackTimeout < GameTimeController.getInstance().getGameTicks())
		{
			if (originalAttackTarget != null)
			{
				npc.stopHating(originalAttackTarget);
			}
			
			if (Config.ANNOUNCE_BOSS_UNDER_ATTACK && npc instanceof L2RaidBossInstance)
			{
				_tracking.remove(npcObjId);
				_attacked = npcObjId;
			}
			
			setIntention(AI_INTENTION_ACTIVE);
			npc.setWalking();
			return;
		}
		
		L2Character mostHate = npc.getMostHated();
		
		if (mostHate == null)
		{
			setIntention(AI_INTENTION_ACTIVE);
			return;
		}
		
		if (npc.isConfused())
		{
			mostHate = originalAttackTarget;
		}
		
		setTarget(mostHate);
		npc.setTarget(mostHate);
		
		if (!_skillrender.getSuicideSkills().isEmpty() && ((int) ((npc.getCurrentHp() / npc.getMaxHp()) * 100) < 30))
		{
			final L2Skill skill = _skillrender.getSuicideSkills().get(Rnd.get(_skillrender.getSuicideSkills().size()));
			if (Util.checkIfInRange(skill.getSkillRadius(), getActiveChar(), mostHate, false) && (Rnd.get(100) < Rnd.get(npc.getMinSkillChance(), npc.getMaxSkillChance())))
			{
				if (cast(skill))
				{
					return;
				}
				
				for (L2Skill sk : _skillrender.getSuicideSkills())
				{
					if (cast(sk))
					{
						return;
					}
				}
			}
		}
		
		final int collision = npc.getTemplate().getCollisionRadius();
		final int combinedCollision = collision + mostHate.getTemplate().getCollisionRadius();
		
		if (!npc.isMovementDisabled() && npc.isInCombat() && (Rnd.nextInt(100) <= 20))
		{
			for (L2Object nearby : npc.getKnownList().getKnownObjects().values())
			{
				if ((nearby instanceof L2Attackable) && npc.isInsideRadius(nearby, collision, false, false) && (nearby != mostHate))
				{
					int newX = combinedCollision + Rnd.get(40);
					if (Rnd.nextBoolean())
					{
						newX = mostHate.getX() + newX;
					}
					else
					{
						newX = mostHate.getX() - newX;
					}
					
					int newY = combinedCollision + Rnd.get(40);
					if (Rnd.nextBoolean())
					{
						newY = mostHate.getY() + newY;
					}
					else
					{
						newY = mostHate.getY() - newY;
					}
					
					int newZ = npc.getZ();
					
					if (!npc.isInsideRadius(newX, newY, newZ, collision, false, false))
					{
						if (GeoData.getInstance().canMove(npc.getX(), npc.getY(), npc.getZ(), newX, newY, newZ, npc.getInstanceId()))
						{
							moveTo(newX, newY, newZ);
						}
					}
					return;
				}
			}
		}
		
		if (!npc.isMovementDisabled() && (npc.getAiType() == AIType.ARCHER))
		{
			double distance2 = npc.getPlanDistanceSq(mostHate.getX(), mostHate.getY());
			
			if (Math.sqrt(distance2) <= 60 + combinedCollision)
			{
				int chance = 50;
				if (Rnd.get(100) <= chance)
				{
					int posX = npc.getX();
					int posY = npc.getY();
					int posZ = npc.getZ() + 30;
					
					if (mostHate.getX() < posX)
					{
						posX = posX + 300;
					}
					else
					{
						posX = posX - 300;
					}
					
					if (mostHate.getY() < posY)
					{
						posY = posY + 300;
					}
					else
					{
						posY = posY - 300;
					}
					
					if (GeoData.getInstance().canMove(npc.getX(), npc.getY(), npc.getZ(), posX, posY, posZ, npc.getInstanceId()))
					{
						setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, new Location(posX, posY, posZ, 0));
					}
					return;
				}
			}
		}
		
		// Boss/Raid Minion Target Reconsider
		if (npc.isRaid() || npc.isRaidMinion())
		{
			chaostime++;
			if (npc instanceof L2RaidBossInstance)
			{
				if (!((L2MonsterInstance) npc).hasMinions())
				{
					if ((chaostime > Config.RAID_CHAOS_TIME) && (Rnd.get(100) <= (100 - ((npc.getCurrentHp() * 100) / npc.getMaxHp()))))
					{
						aggroReconsider();
						chaostime = 0;
						return;
					}
				}
				else
				{
					if ((chaostime > Config.RAID_CHAOS_TIME) && (Rnd.get(100) <= (100 - ((npc.getCurrentHp() * 200) / npc.getMaxHp()))))
					{
						aggroReconsider();
						chaostime = 0;
						return;
					}
				}
			}
			else if (npc instanceof L2GrandBossInstance)
			{
				if (chaostime > Config.GRAND_CHAOS_TIME)
				{
					double chaosRate = 100 - ((npc.getCurrentHp() * 300) / npc.getMaxHp());
					if (((chaosRate <= 10) && (Rnd.get(100) <= 10)) || ((chaosRate > 10) && (Rnd.get(100) <= chaosRate)))
					{
						aggroReconsider();
						chaostime = 0;
						return;
					}
				}
			}
			else
			{
				if ((chaostime > Config.MINION_CHAOS_TIME) && (Rnd.get(100) <= (100 - ((npc.getCurrentHp() * 200) / npc.getMaxHp()))))
				{
					aggroReconsider();
					chaostime = 0;
					return;
				}
			}
		}
		
		// Heal Condition
		if (!_skillrender.getHealSkills().isEmpty())
		{
			double percentage = (npc.getCurrentHp() / npc.getMaxHp()) * 100;
			
			// First priority is to heal leader (if npc is a minion).
			if (npc.isMinion())
			{
				L2Character leader = ((L2MinionInstance) npc).getLeader();
				if ((leader != null) && !leader.isDead() && (Rnd.get(100) > ((leader.getCurrentHp() / leader.getMaxHp()) * 100)))
				{
					for (L2Skill sk : _skillrender.getHealSkills())
					{
						if (sk.getTargetType() == SkillTargetType.TARGET_SELF)
						{
							continue;
						}
						
						if (!checkSkillCastConditions(sk))
						{
							continue;
						}
						
						if (!Util.checkIfInRange((sk.getCastRange() + collision + leader.getTemplate().getCollisionRadius()), npc, leader, false) && !isParty(sk) && !npc.isMovementDisabled())
						{
							moveToPawn(leader, sk.getCastRange() + collision + leader.getTemplate().getCollisionRadius());
							return;
						}
						
						if (GeoData.getInstance().canSeeTarget(npc, leader))
						{
							clientStopMoving(null);
							npc.setTarget(leader);
							npc.doCast(sk);
							return;
						}
					}
				}
			}
			
			// Second priority is to heal himself.
			if (Rnd.get(100) < ((100 - percentage) / 3))
			{
				for (L2Skill sk : _skillrender.getHealSkills())
				{
					if (!checkSkillCastConditions(sk))
					{
						continue;
					}
					
					clientStopMoving(null);
					npc.setTarget(npc);
					npc.doCast(sk);
					return;
				}
			}
			
			for (L2Skill sk : _skillrender.getHealSkills())
			{
				if (!checkSkillCastConditions(sk))
				{
					continue;
				}
				
				if (sk.getTargetType() == SkillTargetType.TARGET_ONE)
				{
					for (L2Character obj : npc.getKnownList().getKnownTypeInRadius(L2Character.class, sk.getCastRange() + collision))
					{
						if (!(obj instanceof L2Attackable) || obj.isDead())
						{
							continue;
						}
						
						L2Attackable targets = ((L2Attackable) obj);
						if ((npc.getFactionId() != null) && !npc.getFactionId().equals(targets.getFactionId()))
						{
							continue;
						}
						
						percentage = (targets.getCurrentHp() / targets.getMaxHp()) * 100;
						if (Rnd.get(100) < ((100 - percentage) / 10))
						{
							if (GeoData.getInstance().canSeeTarget(npc, targets))
							{
								clientStopMoving(null);
								npc.setTarget(obj);
								npc.doCast(sk);
								return;
							}
						}
					}
					
					if (isParty(sk))
					{
						clientStopMoving(null);
						npc.doCast(sk);
						return;
					}
				}
			}
		}
		
		if (!npc.isMovementDisabled() && !npc.isMoving() && (npc.getAiType() == AIType.HEALER))
		{
			double distance2 = npc.getPlanDistanceSq(originalAttackTarget.getX(), originalAttackTarget.getY());
			if (Math.sqrt(distance2) <= 60 + combinedCollision)
			{
				int chance = 50;
				if (Rnd.get(100) <= chance)
				{
					int posX = npc.getX();
					int posY = npc.getY();
					int posZ = npc.getZ() + 30;
					
					if (originalAttackTarget.getX() < posX)
					{
						posX = posX + 300;
					}
					else
					{
						posX = posX - 300;
					}
					
					if (originalAttackTarget.getY() < posY)
					{
						posY = posY + 300;
					}
					else
					{
						posY = posY - 300;
					}
					
					if (GeoData.getInstance().canMove(npc.getX(), npc.getY(), npc.getZ(), posX, posY, posZ, npc.getInstanceId()))
					{
						setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, new Location(posX, posY, posZ, 0));
					}
					return;
				}
			}
			
			return;
		}
		
		double dist = Math.sqrt(npc.getPlanDistanceSq(mostHate.getX(), mostHate.getY()));
		int dist2 = (int) dist - collision;
		int range = npc.getPhysicalAttackRange() + combinedCollision;
		
		if (mostHate.isMoving())
		{
			range = range + 50;
			if (npc.isMoving())
			{
				range = range + 50;
			}
		}
		
		// Immobilise Condition
		if ((npc.isMovementDisabled() && ((dist > range) || mostHate.isMoving())) || ((dist > range) && mostHate.isMoving() || !GeoData.getInstance().canSeeTarget(npc, mostHate)))
		{
			movementDisable();
			return;
		}
		
		// General Skill Use
		if (!_skillrender.getGeneralSkills().isEmpty())
		{
			if (Rnd.get(100) < Rnd.get(npc.getMinSkillChance(), npc.getMaxSkillChance()))
			{
				L2Skill skills = _skillrender.getGeneralSkills().get(Rnd.get(_skillrender.getGeneralSkills().size()));
				if (cast(skills))
				{
					return;
				}
				
				for (L2Skill sk : _skillrender.getGeneralSkills())
				{
					if (cast(sk))
					{
						return;
					}
				}
			}
			
			// Long/Short Range skill Usage
			if (npc.hasLongRangeSkill() || npc.hasShortRangeSkill())
			{
				final List<L2Skill> longRangeSkillsList = longRangeSkillRender();
				if (!longRangeSkillsList.isEmpty() && npc.hasLongRangeSkill() && (dist2 > 150) && (Rnd.get(100) <= npc.getLongRangeSkillChance()))
				{
					final L2Skill longRangeSkill = longRangeSkillsList.get(Rnd.get(longRangeSkillsList.size()));
					if (cast(longRangeSkill))
					{
						return;
					}
					
					for (L2Skill sk : longRangeSkills)
					{
						if (cast(sk))
						{
							return;
						}
					}
				}
				
				final List<L2Skill> shortRangeSkillsList = shortRangeSkillRender();
				if (!shortRangeSkillsList.isEmpty() && npc.hasShortRangeSkill() && (dist2 <= 150) && (Rnd.get(100) <= npc.getShortRangeSkillChance()))
				{
					final L2Skill shortRangeSkill = shortRangeSkillsList.get(Rnd.get(shortRangeSkillsList.size()));
					if (cast(shortRangeSkill))
					{
						return;
					}
					
					for (L2Skill sk : shortRangeSkills)
					{
						if (cast(sk))
						{
							return;
						}
					}
				}
			}
		}
		
		if (maybeMoveToPawn(getTarget(), npc.getPhysicalAttackRange()))
		{
			return;
		}
		
		_attackTimeout = MAX_ATTACK_TIMEOUT + GameTimeController.getInstance().getGameTicks();
		_runTimeout = RUN_TIMEOUT + GameTimeController.getInstance().getGameTicks();
		
		clientStopMoving(null);
		melee(npc.getPrimaryAttack());
	}
	
	private void melee(int type)
	{
		if (type != 0)
		{
			switch (type)
			{
				case -1:
					if (_skillrender.getGeneralSkills() != null)
					{
						for (L2Skill sk : _skillrender.getGeneralSkills())
						{
							if (cast(sk))
							{
								return;
							}
						}
					}
					break;
				
				case 1:
					for (L2Skill sk : _skillrender.getAtkSkills())
					{
						if (cast(sk))
						{
							return;
						}
					}
					break;
				
				default:
					for (L2Skill sk : _skillrender.getGeneralSkills())
					{
						if (sk.getId() == getActiveChar().getPrimaryAttack())
						{
							if (cast(sk))
							{
								return;
							}
						}
					}
					break;
			}
		}
		
		_actor.doAttack((L2Character) getTarget());
	}
	
	@Override
	protected void onEvtThink()
	{
		// Check if the actor can't use skills and if a thinking action isn't already in progress
		if (_thinking || _actor.isAllSkillsDisabled())
		{
			return;
		}
		
		// Start thinking action
		_thinking = true;
		
		try
		{
			// Manage AI thoughts
			switch (getIntention())
			{
				case AI_INTENTION_ACTIVE:
					thinkActive();
					break;
				case AI_INTENTION_ATTACK:
					thinkAttack();
					break;
				case AI_INTENTION_CAST:
					thinkCast();
					break;
			}
		}
		finally
		{
			// Stop thinking action
			_thinking = false;
		}
	}
	
	@Override
	protected void onEvtAttacked(L2Character attacker)
	{
		L2Attackable npc = (L2Attackable) _actor;
		L2Character originalAttackTarget = attacker;// getTarget();
		
		// Calculate the attack timeout
		_attackTimeout = MAX_ATTACK_TIMEOUT + GameTimeController.getInstance().getGameTicks();
		_runTimeout = RUN_TIMEOUT + GameTimeController.getInstance().getGameTicks();
		
		if (_globalAggro < 0)
		{
			_globalAggro = 0;
		}
		
		npc.addDamageHate(attacker, 0, 1);
		
		if (!npc.isRunning())
		{
			npc.setRunning();
		}
		
		if (getIntention() != AI_INTENTION_ATTACK)
		{
			setIntention(CtrlIntention.AI_INTENTION_ATTACK, attacker);
		}
		else if (npc.getMostHated() != getTarget())
		{
			setIntention(CtrlIntention.AI_INTENTION_ATTACK, attacker);
		}
		
		final int collision_r = npc.getTemplate().getCollisionRadius();
		String factionId = npc.getFactionId();
		if (factionId != null && !factionId.isEmpty())
		{
			int factionRange = npc.getFactionRange() + collision_r;
			for (L2Object obj : npc.getKnownList().getKnownCharactersInRadius(factionRange))
			{
				if (obj instanceof L2NpcInstance)
				{
					L2NpcInstance called = (L2NpcInstance) obj;
					
					final String npcfaction = called.getFactionId();
					if ((npcfaction == null) || npcfaction.isEmpty())
					{
						continue;
					}
					
					boolean sevenSignFaction = false;
					
					// Catacomb mobs should assist lilim and nephilim other than dungeon
					if ("c_dungeon_clan".equals(factionId) && ("c_dungeon_lilim".equals(npcfaction) || "c_dungeon_nephi".equals(npcfaction)))
					{
						sevenSignFaction = true;
					}
					else if ("c_dungeon_lilim".equals(factionId) && "c_dungeon_clan".equals(npcfaction))
					{
						sevenSignFaction = true;
					}
					else if ("c_dungeon_nephi".equals(factionId) && "c_dungeon_clan".equals(npcfaction))
					{
						sevenSignFaction = true;
					}
					
					// lower levels
					if ("c_dungeon_clan".equals(factionId) && ("c_dungeon_lith".equals(npcfaction) || "c_dungeon_gigant".equals(npcfaction)))
					{
						sevenSignFaction = true;
					}
					else if ("c_dungeon_lith".equals(factionId) && "c_dungeon_clan".equals(npcfaction))
					{
						sevenSignFaction = true;
					}
					else if ("c_dungeon_gigant".equals(factionId) && "c_dungeon_clan".equals(npcfaction))
					{
						sevenSignFaction = true;
					}
					
					if (!factionId.equals(npcfaction) && !sevenSignFaction)
					{
						continue;
					}
					
					// Check if the L2Object is inside the Faction Range of the actor
					if (npc.isInsideRadius(called, factionRange, true, false) && called.hasAI() && GeoData.getInstance().canSeeTarget(npc, called))
					{
						if (originalAttackTarget instanceof L2PlayableInstance)
						{
							Quest[] quests = called.getTemplate().getEventQuests(Quest.QuestEventType.ON_FACTION_CALL);
							if (quests != null)
							{
								L2PcInstance player = originalAttackTarget.getActingPlayer();
								boolean isSummon = originalAttackTarget instanceof L2Summon;
								for (Quest quest : quests)
								{
									quest.notifyFactionCall(called, npc, player, isSummon);
								}
							}
						}
						else if ((called instanceof L2Attackable) && (called.getAI().getIntention() != CtrlIntention.AI_INTENTION_ATTACK))
						{
							((L2Attackable) called).addDamageHate(originalAttackTarget, 0, npc.getHating(originalAttackTarget));
							called.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, originalAttackTarget);
						}
					}
				}
			}
		}
		super.onEvtAttacked(attacker);
	}
	
	@Override
	protected void onEvtAggression(L2Character target, int aggro)
	{
		L2Attackable me = (L2Attackable) _actor;
		
		if (target != null)
		{
			me.addDamageHate(target, 0, aggro);
			
			if (getIntention() != CtrlIntention.AI_INTENTION_ATTACK)
			{
				if (!_actor.isRunning())
				{
					_actor.setRunning();
				}
				setIntention(CtrlIntention.AI_INTENTION_ATTACK, target);
			}
		}
	}
	
	@Override
	protected void onIntentionActive()
	{
		// Cancel attack timeout
		_attackTimeout = Integer.MAX_VALUE;
		_runTimeout = Integer.MAX_VALUE;
		
		super.onIntentionActive();
	}
	
	public void setGlobalAggro(int value)
	{
		_globalAggro = value;
	}
	
	public L2Attackable getActiveChar()
	{
		return (L2Attackable) _actor;
	}
	
	// XXX cast
	private boolean cast(L2Skill sk)
	{
		if (sk == null)
		{
			return false;
		}
		
		final L2Attackable caster = getActiveChar();
		
		if (!checkSkillCastConditions(sk))
		{
			return false;
		}
		
		if (caster.isCastingNow())
		{
			return false;
		}
		
		if (getTarget() == null)
		{
			if (caster.getMostHated() != null)
			{
				setTarget(caster.getMostHated());
			}
		}
		
		switch (sk.getSkillType())
		{
			case BUFF:
			{
				if (caster.getFirstEffect(sk) == null)
				{
					clientStopMoving(null);
					caster.setTarget(caster);
					caster.doCast(sk);
					return true;
				}
			}
		}
		
		L2Character attackTarget = (L2Character) getTarget();
		if (attackTarget == null)
		{
			return false;
		}
		
		double dist = Math.sqrt(caster.getPlanDistanceSq(attackTarget.getX(), attackTarget.getY()));
		double dist2 = dist - attackTarget.getTemplate().getCollisionRadius();
		double range = caster.getPhysicalAttackRange() + caster.getTemplate().getCollisionRadius() + attackTarget.getTemplate().getCollisionRadius();
		double srange = sk.getCastRange() + caster.getTemplate().getCollisionRadius();
		
		if (attackTarget.isMoving())
		{
			dist2 = dist2 - 30;
		}
		
		switch (sk.getSkillType())
		{
			case BUFF:
			{
				if (caster.getFirstEffect(sk) == null)
				{
					clientStopMoving(null);
					caster.setTarget(caster);
					caster.doCast(sk);
					return true;
				}
				
				// If actor already have buff, start looking at others same faction mob to cast
				if (sk.getTargetType() == SkillTargetType.TARGET_SELF)
				{
					return false;
				}
				
				if (sk.getTargetType() == SkillTargetType.TARGET_ONE)
				{
					L2Character target = effectTargetReconsider(sk, true);
					if (target != null)
					{
						clientStopMoving(null);
						L2Object targets = attackTarget;
						caster.setTarget(target);
						caster.doCast(sk);
						caster.setTarget(targets);
						return true;
					}
				}
				
				if (canParty(sk))
				{
					clientStopMoving(null);
					L2Object targets = attackTarget;
					caster.setTarget(caster);
					caster.doCast(sk);
					caster.setTarget(targets);
					return true;
				}
				break;
			}
			
			case HEAL:
			case HOT:
			case HEAL_PERCENT:
			case HEAL_STATIC:
			case BALANCE_LIFE:
			{
				double percentage = (caster.getCurrentHp() / caster.getMaxHp()) * 100;
				if (caster.isMinion() && (sk.getTargetType() != SkillTargetType.TARGET_SELF))
				{
					L2Character leader = ((L2MinionInstance) caster).getLeader();
					if ((leader != null) && !leader.isDead() && (Rnd.get(100) > ((leader.getCurrentHp() / leader.getMaxHp()) * 100)))
					{
						if (!Util.checkIfInRange((sk.getCastRange() + caster.getTemplate().getCollisionRadius() + leader.getTemplate().getCollisionRadius()), caster, leader, false) && !isParty(sk) && !caster.isMovementDisabled())
						{
							moveToPawn(leader, sk.getCastRange() + caster.getTemplate().getCollisionRadius() + leader.getTemplate().getCollisionRadius());
						}
						
						if (GeoData.getInstance().canSeeTarget(caster, leader))
						{
							clientStopMoving(null);
							caster.setTarget(leader);
							caster.doCast(sk);
							return true;
						}
					}
				}
				
				if (Rnd.get(100) < ((100 - percentage) / 3))
				{
					clientStopMoving(null);
					caster.setTarget(caster);
					caster.doCast(sk);
					return true;
				}
				
				if (sk.getTargetType() == SkillTargetType.TARGET_ONE)
				{
					for (L2Character obj : caster.getKnownList().getKnownTypeInRadius(L2Character.class, sk.getCastRange() + caster.getTemplate().getCollisionRadius()))
					{
						if (!(obj instanceof L2Attackable) || obj.isDead())
						{
							continue;
						}
						
						L2Attackable targets = ((L2Attackable) obj);
						if ((caster.getFactionId() != null) && !caster.getFactionId().equals(targets.getFactionId()))
						{
							continue;
						}
						
						percentage = (targets.getCurrentHp() / targets.getMaxHp()) * 100;
						if (Rnd.get(100) < ((100 - percentage) / 10))
						{
							if (GeoData.getInstance().canSeeTarget(caster, targets))
							{
								clientStopMoving(null);
								caster.setTarget(obj);
								caster.doCast(sk);
								return true;
							}
						}
					}
				}
				
				if (isParty(sk))
				{
					for (L2Character obj : caster.getKnownList().getKnownTypeInRadius(L2Character.class, sk.getSkillRadius() + caster.getTemplate().getCollisionRadius()))
					{
						if (!(obj instanceof L2Attackable))
						{
							continue;
						}
						
						L2NpcInstance targets = ((L2NpcInstance) obj);
						if ((caster.getFactionId() != null) && targets.getFactionId().equals(caster.getFactionId()))
						{
							if ((obj.getCurrentHp() < obj.getMaxHp()) && (Rnd.get(100) <= 20))
							{
								clientStopMoving(null);
								caster.setTarget(caster);
								caster.doCast(sk);
								return true;
							}
						}
					}
				}
				break;
			}
			
			case DEBUFF:
			case POISON:
			case DOT:
			case MDOT:
			case BLEED:
			{
				if (GeoData.getInstance().canSeeTarget(caster, attackTarget) && !canAOE(sk) && !attackTarget.isDead() && (dist2 <= srange))
				{
					if (attackTarget.getFirstEffect(sk) == null)
					{
						clientStopMoving(null);
						caster.doCast(sk);
						return true;
					}
				}
				else if (canAOE(sk))
				{
					if ((sk.getTargetType() == SkillTargetType.TARGET_AURA) || (sk.getTargetType() == SkillTargetType.TARGET_BEHIND_AURA) || (sk.getTargetType() == SkillTargetType.TARGET_FRONT_AURA))
					{
						clientStopMoving(null);
						caster.doCast(sk);
						return true;
					}
					
					if (((sk.getTargetType() == SkillTargetType.TARGET_AREA) || (sk.getTargetType() == SkillTargetType.TARGET_BEHIND_AREA) || (sk.getTargetType() == SkillTargetType.TARGET_FRONT_AREA)) && GeoData.getInstance().canSeeTarget(caster, attackTarget) && !attackTarget.isDead()
						&& (dist2 <= srange))
					{
						clientStopMoving(null);
						caster.doCast(sk);
						return true;
					}
				}
				else if (sk.getTargetType() == SkillTargetType.TARGET_ONE)
				{
					L2Character target = effectTargetReconsider(sk, false);
					if (target != null)
					{
						clientStopMoving(null);
						caster.doCast(sk);
						return true;
					}
				}
				break;
			}
			
			case SLEEP:
			{
				if (sk.getTargetType() == SkillTargetType.TARGET_ONE)
				{
					if (!attackTarget.isDead() && (dist2 <= srange))
					{
						if ((dist2 > range) || attackTarget.isMoving())
						{
							if (attackTarget.getFirstEffect(sk) == null)
							{
								clientStopMoving(null);
								caster.doCast(sk);
								return true;
							}
						}
					}
					
					L2Character target = effectTargetReconsider(sk, false);
					if (target != null)
					{
						clientStopMoving(null);
						caster.doCast(sk);
						return true;
					}
				}
				else if (canAOE(sk))
				{
					if ((sk.getTargetType() == SkillTargetType.TARGET_AURA) || (sk.getTargetType() == SkillTargetType.TARGET_BEHIND_AURA) || (sk.getTargetType() == SkillTargetType.TARGET_FRONT_AURA))
					{
						clientStopMoving(null);
						caster.doCast(sk);
						return true;
					}
					
					if (((sk.getTargetType() == SkillTargetType.TARGET_AREA) || (sk.getTargetType() == SkillTargetType.TARGET_BEHIND_AREA) || (sk.getTargetType() == SkillTargetType.TARGET_FRONT_AREA)) && GeoData.getInstance().canSeeTarget(caster, attackTarget) && !attackTarget.isDead()
						&& (dist2 <= srange))
					{
						clientStopMoving(null);
						caster.doCast(sk);
						return true;
					}
				}
				break;
			}
			
			case ROOT:
			case STUN:
			case PARALYZE:
			{
				if (GeoData.getInstance().canSeeTarget(caster, attackTarget) && !canAOE(sk) && (dist2 <= srange))
				{
					if (attackTarget.getFirstEffect(sk) == null)
					{
						clientStopMoving(null);
						caster.doCast(sk);
						return true;
					}
				}
				else if (canAOE(sk))
				{
					if ((sk.getTargetType() == SkillTargetType.TARGET_AURA) || (sk.getTargetType() == SkillTargetType.TARGET_BEHIND_AURA) || (sk.getTargetType() == SkillTargetType.TARGET_FRONT_AURA))
					{
						clientStopMoving(null);
						caster.doCast(sk);
						return true;
					}
					else if (((sk.getTargetType() == SkillTargetType.TARGET_AREA) || (sk.getTargetType() == SkillTargetType.TARGET_BEHIND_AREA) || (sk.getTargetType() == SkillTargetType.TARGET_FRONT_AREA)) && GeoData.getInstance().canSeeTarget(caster, attackTarget) && !attackTarget.isDead()
						&& (dist2 <= srange))
					{
						clientStopMoving(null);
						caster.doCast(sk);
						return true;
					}
				}
				else if (sk.getTargetType() == SkillTargetType.TARGET_ONE)
				{
					L2Character target = effectTargetReconsider(sk, false);
					if (target != null)
					{
						clientStopMoving(null);
						caster.doCast(sk);
						return true;
					}
				}
				break;
			}
			
			case MUTE:
			case FEAR:
			{
				if (GeoData.getInstance().canSeeTarget(caster, attackTarget) && !canAOE(sk) && (dist2 <= srange))
				{
					if (attackTarget.getFirstEffect(sk) == null)
					{
						clientStopMoving(null);
						caster.doCast(sk);
						return true;
					}
				}
				else if (canAOE(sk))
				{
					if ((sk.getTargetType() == SkillTargetType.TARGET_AURA) || (sk.getTargetType() == SkillTargetType.TARGET_BEHIND_AURA) || (sk.getTargetType() == SkillTargetType.TARGET_FRONT_AURA))
					{
						clientStopMoving(null);
						caster.doCast(sk);
						return true;
					}
					
					if (((sk.getTargetType() == SkillTargetType.TARGET_AREA) || (sk.getTargetType() == SkillTargetType.TARGET_BEHIND_AREA) || (sk.getTargetType() == SkillTargetType.TARGET_FRONT_AREA)) && GeoData.getInstance().canSeeTarget(caster, attackTarget) && !attackTarget.isDead()
						&& (dist2 <= srange))
					{
						clientStopMoving(null);
						caster.doCast(sk);
						return true;
					}
				}
				else if (sk.getTargetType() == SkillTargetType.TARGET_ONE)
				{
					L2Character target = effectTargetReconsider(sk, false);
					if (target != null)
					{
						clientStopMoving(null);
						caster.doCast(sk);
						return true;
					}
				}
				break;
			}
			
			case CANCEL:
			case NEGATE:
			{
				if (sk.getTargetType() == SkillTargetType.TARGET_ONE)
				{
					if ((attackTarget.getFirstEffect(EffectType.BUFF) != null) && GeoData.getInstance().canSeeTarget(caster, attackTarget) && !attackTarget.isDead() && (dist2 <= srange))
					{
						clientStopMoving(null);
						caster.doCast(sk);
						return true;
					}
					
					L2Character target = effectTargetReconsider(sk, false);
					if (target != null)
					{
						clientStopMoving(null);
						L2Object targets = attackTarget;
						caster.setTarget(target);
						caster.doCast(sk);
						caster.setTarget(targets);
						return true;
					}
				}
				else if (canAOE(sk))
				{
					if (((sk.getTargetType() == SkillTargetType.TARGET_AURA) || (sk.getTargetType() == SkillTargetType.TARGET_BEHIND_AURA) || (sk.getTargetType() == SkillTargetType.TARGET_FRONT_AURA)) && GeoData.getInstance().canSeeTarget(caster, attackTarget))
					{
						clientStopMoving(null);
						caster.doCast(sk);
						return true;
					}
					else if (((sk.getTargetType() == SkillTargetType.TARGET_AREA) || (sk.getTargetType() == SkillTargetType.TARGET_BEHIND_AREA) || (sk.getTargetType() == SkillTargetType.TARGET_FRONT_AREA)) && GeoData.getInstance().canSeeTarget(caster, attackTarget) && !attackTarget.isDead()
						&& (dist2 <= srange))
					{
						clientStopMoving(null);
						caster.doCast(sk);
						return true;
					}
				}
				break;
			}
			
			default:
			{
				if (!canAura(sk))
				{
					if (GeoData.getInstance().canSeeTarget(caster, attackTarget) && !attackTarget.isDead() && (dist2 <= srange))
					{
						clientStopMoving(null);
						caster.doCast(sk);
						return true;
					}
					
					L2Character target = skillTargetReconsider(sk);
					if (target != null)
					{
						clientStopMoving(null);
						L2Object targets = attackTarget;
						caster.setTarget(target);
						caster.doCast(sk);
						caster.setTarget(targets);
						return true;
					}
				}
				else
				{
					clientStopMoving(null);
					caster.doCast(sk);
					return true;
				}
				break;
			}
		}
		
		return false;
	}
	
	// XXX movementDisable()
	private void movementDisable()
	{
		final L2Attackable npc = getActiveChar();
		if (npc == null)
		{
			return;
		}
		
		final L2Character victim = (L2Character) getTarget();
		if (victim == null)
		{
			setIntention(AI_INTENTION_ACTIVE);
			return;
		}
		
		if (npc.getTarget() == null)
		{
			npc.setTarget(victim);
		}
		
		double dist = Math.sqrt(npc.getPlanDistanceSq(victim.getX(), victim.getY()));
		double dist2 = dist - npc.getTemplate().getCollisionRadius();
		int range = npc.getPhysicalAttackRange() + npc.getTemplate().getCollisionRadius() + victim.getTemplate().getCollisionRadius();
		
		if (victim.isMoving())
		{
			dist = dist - 30;
			if (npc.isMoving())
			{
				dist = dist - 50;
			}
		}
		
		// Check if activeChar has any skill
		if (!_skillrender.getGeneralSkills().isEmpty())
		{
			// Try to stop the target or disable the target as priority
			int random = Rnd.get(100);
			if ((random < 2) && !_skillrender.getImmobilizeSkills().isEmpty() && !victim.isImobilised())
			{
				for (L2Skill sk : _skillrender.getImmobilizeSkills())
				{
					if (!checkSkillCastConditions(sk) || (((sk.getCastRange() + npc.getTemplate().getCollisionRadius() + victim.getTemplate().getCollisionRadius()) <= dist2) && !canAura(sk)))
					{
						continue;
					}
					
					if (!GeoData.getInstance().canSeeTarget(npc, victim))
					{
						continue;
					}
					
					if (victim.getFirstEffect(sk) == null)
					{
						clientStopMoving(null);
						npc.doCast(sk);
						return;
					}
				}
			}
			
			// Same as above, but with Mute/FEAR etc....
			if ((random < 5) && !_skillrender.getCostOverTimeSkills().isEmpty())
			{
				for (L2Skill sk : _skillrender.getCostOverTimeSkills())
				{
					if (!checkSkillCastConditions(sk) || (((sk.getCastRange() + npc.getTemplate().getCollisionRadius() + victim.getTemplate().getCollisionRadius()) <= dist2) && !canAura(sk)))
					{
						continue;
					}
					
					if (!GeoData.getInstance().canSeeTarget(npc, victim))
					{
						continue;
					}
					
					if (victim.getFirstEffect(sk) == null)
					{
						clientStopMoving(null);
						npc.doCast(sk);
						return;
					}
				}
			}
			
			// Try to debuff target
			if ((random < 8) && !_skillrender.getDebuffSkills().isEmpty())
			{
				for (L2Skill sk : _skillrender.getDebuffSkills())
				{
					if (!checkSkillCastConditions(sk) || (((sk.getCastRange() + npc.getTemplate().getCollisionRadius() + victim.getTemplate().getCollisionRadius()) <= dist2) && !canAura(sk)))
					{
						continue;
					}
					
					if (!GeoData.getInstance().canSeeTarget(npc, victim))
					{
						continue;
					}
					
					if (victim.getFirstEffect(sk) == null)
					{
						clientStopMoving(null);
						npc.doCast(sk);
						return;
					}
				}
			}
			
			// Try to debuff target ; side effect skill like CANCEL or NEGATE
			if ((random < 9) && !_skillrender.getNegativeSkills().isEmpty())
			{
				for (L2Skill sk : _skillrender.getNegativeSkills())
				{
					if (!checkSkillCastConditions(sk) || (((sk.getCastRange() + npc.getTemplate().getCollisionRadius() + victim.getTemplate().getCollisionRadius()) <= dist2) && !canAura(sk)))
					{
						continue;
					}
					
					if (!GeoData.getInstance().canSeeTarget(npc, victim))
					{
						continue;
					}
					
					if (victim.getFirstEffect(EffectType.BUFF) != null)
					{
						clientStopMoving(null);
						npc.doCast(sk);
						return;
					}
				}
			}
			
			// Start ATK SKILL when nothing can be done
			if (!_skillrender.getAtkSkills().isEmpty() && (npc.isMovementDisabled() || (npc.getAiType() == AIType.MAGE) || (npc.getAiType() == AIType.HEALER)))
			{
				for (L2Skill sk : _skillrender.getAtkSkills())
				{
					if (!checkSkillCastConditions(sk) || (((sk.getCastRange() + npc.getTemplate().getCollisionRadius() + victim.getTemplate().getCollisionRadius()) <= dist2) && !canAura(sk)))
					{
						continue;
					}
					
					if (!GeoData.getInstance().canSeeTarget(npc, victim))
					{
						continue;
					}
					
					clientStopMoving(null);
					npc.doCast(sk);
					return;
				}
			}
		}
		
		if (npc.isMovementDisabled())
		{
			targetReconsider();
			return;
		}
		
		if ((dist > range) || !GeoData.getInstance().canSeeTarget(npc, victim))
		{
			if (victim.isMoving())
			{
				range -= 100;
			}
			
			if (range < 5)
			{
				range = 5;
			}
			
			moveToPawn(victim, range);
			return;
		}
		
		melee(npc.getPrimaryAttack());
	}
	
	private boolean checkSkillCastConditions(L2Skill skill)
	{
		// Not enough MP.
		if (skill.getMpConsume() >= getActiveChar().getCurrentMp())
		{
			return false;
		}
		
		// Character is in "skill disabled" mode.
		if (getActiveChar().isSkillDisabled(skill))
		{
			return false;
		}
		
		// Is a magic skill and character is magically muted or is a physical skill and character is physically muted.
		if ((skill.isMagic() && getActiveChar().isMuted()) || getActiveChar().isPsychicalMuted())
		{
			return false;
		}
		
		return true;
	}
	
	// XXX skillTargetReconsider
	private L2Character skillTargetReconsider(L2Skill sk)
	{
		double dist = 0;
		double dist2 = 0;
		int range = 0;
		L2Attackable actor = getActiveChar();
		
		if (actor.getHateList() != null)
		{
			for (L2Character obj : actor.getHateList())
			{
				if (obj == null || obj.isDead() || !GeoData.getInstance().canSeeTarget(actor, obj))
				{
					continue;
				}
				
				// actor.setTarget(getTarget());
				// actor.setTarget(obj);
				dist = Math.sqrt(actor.getPlanDistanceSq(obj.getX(), obj.getY()));
				dist2 = dist - actor.getTemplate().getCollisionRadius();
				range = sk.getCastRange() + actor.getTemplate().getCollisionRadius() + obj.getTemplate().getCollisionRadius();
				
				if (dist2 <= range)
				{
					return obj;
				}
			}
		}
		
		if (!(actor instanceof L2GuardInstance))
		{
			for (L2Character target : actor.getKnownList().getKnownType(L2Character.class))
			{
				// actor.setTarget(target);
				dist = Math.sqrt(actor.getPlanDistanceSq(target.getX(), target.getY()));
				dist2 = dist;
				range = sk.getCastRange() + actor.getTemplate().getCollisionRadius() + target.getTemplate().getCollisionRadius();
				
				if (target.isDead() || !GeoData.getInstance().canSeeTarget(actor, target) || (dist2 > range))
				{
					continue;
				}
				
				if (target instanceof L2PcInstance)
				{
					return target;
				}
				
				if (target instanceof L2Attackable)
				{
					if ((actor.getFactionId() != null) && actor.getFactionId().equals(((L2Attackable) target).getFactionId()))
					{
						continue;
					}
					
					if (actor.getIsChaos() != 0)
					{
						if ((((L2Attackable) target).getFactionId() != null) && ((L2Attackable) target).getFactionId().equals(actor.getFactionId()))
						{
							continue;
						}
						
						return target;
					}
				}
				
				if (target instanceof L2Summon)
				{
					return target;
				}
			}
		}
		return null;
	}
	
	// XXX effectTargetReconsider
	private L2Character effectTargetReconsider(L2Skill sk, boolean positive)
	{
		if (sk == null)
		{
			return null;
		}
		
		L2Attackable actor = getActiveChar();
		
		if ((sk.getSkillType() != SkillType.NEGATE) || (sk.getSkillType() != SkillType.CANCEL))
		{
			if (!positive)
			{
				double dist = 0;
				double dist2 = 0;
				int range = 0;
				
				for (L2Character obj : actor.getAttackByList())
				{
					if ((obj == null) || obj.isDead() || !GeoData.getInstance().canSeeTarget(actor, obj) || (obj == getTarget()))
					{
						continue;
					}
					
					// actor.setTarget(obj);
					dist = Math.sqrt(actor.getPlanDistanceSq(obj.getX(), obj.getY()));
					dist2 = dist - actor.getTemplate().getCollisionRadius();
					range = sk.getCastRange() + actor.getTemplate().getCollisionRadius() + obj.getTemplate().getCollisionRadius();
					if (obj.isMoving())
					{
						dist2 = dist2 - 70;
					}
					
					if (dist2 <= range)
					{
						if (((L2Character) getTarget()).getFirstEffect(sk) == null)
						{
							return obj;
						}
					}
				}
				
				// If there is nearby Target with aggro, start going on random target that is attackable
				for (L2Character obj : actor.getKnownList().getKnownTypeInRadius(L2Character.class, range))
				{
					if (obj.isDead() || !GeoData.getInstance().canSeeTarget(actor, obj))
					{
						continue;
					}
					
					// actor.setTarget(obj);
					dist = Math.sqrt(actor.getPlanDistanceSq(obj.getX(), obj.getY()));
					dist2 = dist;
					range = sk.getCastRange() + actor.getTemplate().getCollisionRadius() + obj.getTemplate().getCollisionRadius();
					if (obj.isMoving())
					{
						dist2 = dist2 - 70;
					}
					
					if ((obj instanceof L2PcInstance) || (obj instanceof L2Summon))
					{
						if (dist2 <= range)
						{
							if (((L2Character) getTarget()).getFirstEffect(sk) == null)
							{
								return obj;
							}
						}
					}
				}
			}
			else if (positive)
			{
				double dist = 0;
				double dist2 = 0;
				int range = 0;
				
				for (L2Character obj : actor.getKnownList().getKnownTypeInRadius(L2Character.class, range))
				{
					if (obj.isDead() || !(obj instanceof L2Attackable) || !GeoData.getInstance().canSeeTarget(actor, obj))
					{
						continue;
					}
					
					L2Attackable targets = ((L2Attackable) obj);
					if ((actor.getFactionId() != null) && actor.getFactionId().equals(targets.getFactionId()))
					{
						continue;
					}
					
					actor.setTarget(obj);
					dist = Math.sqrt(actor.getPlanDistanceSq(obj.getX(), obj.getY()));
					dist2 = dist - actor.getTemplate().getCollisionRadius();
					range = sk.getCastRange() + actor.getTemplate().getCollisionRadius() + obj.getTemplate().getCollisionRadius();
					if (obj.isMoving())
					{
						dist2 = dist2 - 70;
					}
					
					if (dist2 <= range)
					{
						if (obj.getFirstEffect(sk) == null)
						{
							return obj;
						}
					}
				}
			}
			return null;
		}
		
		double dist = 0;
		double dist2 = 0;
		int range = 0;
		range = sk.getCastRange() + actor.getTemplate().getCollisionRadius() + ((L2Character) getTarget()).getTemplate().getCollisionRadius();
		
		for (L2Character obj : actor.getKnownList().getKnownTypeInRadius(L2Character.class, range))
		{
			if (obj.isDead() || !GeoData.getInstance().canSeeTarget(actor, obj))
			{
				continue;
			}
			
			// actor.setTarget(obj);
			dist = Math.sqrt(actor.getPlanDistanceSq(obj.getX(), obj.getY()));
			dist2 = dist - actor.getTemplate().getCollisionRadius();
			range = sk.getCastRange() + actor.getTemplate().getCollisionRadius() + obj.getTemplate().getCollisionRadius();
			if (obj.isMoving())
			{
				dist2 = dist2 - 70;
			}
			
			if ((obj instanceof L2PcInstance) || (obj instanceof L2Summon))
			{
				if (dist2 <= range)
				{
					if (((L2Character) getTarget()).getFirstEffect(EffectType.BUFF) != null)
					{
						return obj;
					}
				}
			}
		}
		return null;
	}
	
	// XXX targetReconsider() if movement - disabled
	private void targetReconsider()
	{
		double dist = 0;
		double dist2 = 0;
		int range = 0;
		L2Attackable actor = getActiveChar();
		
		L2Character MostHate = actor.getMostHated();
		
		if (actor.getHateList() != null)
		{
			for (L2Character obj : actor.getHateList())
			{
				if ((obj == null) || obj.isDead() || !GeoData.getInstance().canSeeTarget(actor, obj) || (obj != MostHate) || (obj == actor))
				{
					continue;
				}
				
				dist = Math.sqrt(actor.getPlanDistanceSq(obj.getX(), obj.getY()));
				dist2 = dist - actor.getTemplate().getCollisionRadius();
				range = actor.getPhysicalAttackRange() + actor.getTemplate().getCollisionRadius() + obj.getTemplate().getCollisionRadius();
				if (obj.isMoving())
				{
					dist2 = dist2 - 70;
				}
				
				if (dist2 <= range)
				{
					if (MostHate != null)
					{
						actor.addDamageHate(obj, 0, actor.getHating(MostHate));
					}
					else
					{
						actor.addDamageHate(obj, 0, 2000);
					}
					
					actor.setTarget(obj);
					setTarget(obj);
					return;
				}
			}
		}
		
		if (!(actor instanceof L2GuardInstance))
		{
			for (L2Character target : actor.getKnownList().getKnownType(L2Character.class))
			{
				if (target.isDead() || !GeoData.getInstance().canSeeTarget(actor, target) || (target != MostHate) || (target == actor) || (target == getTarget()))
				{
					continue;
				}
				
				if (target instanceof L2PcInstance)
				{
					if (MostHate != null)
					{
						actor.addDamageHate(target, 0, actor.getHating(MostHate));
					}
					else
					{
						actor.addDamageHate(target, 0, 2000);
					}
					
					actor.setTarget(target);
					setTarget(target);
				}
				else if (target instanceof L2Attackable)
				{
					if (actor.getIsChaos() != 0)
					{
						if ((((L2Attackable) target).getFactionId() != null) && ((L2Attackable) target).getFactionId().equals(actor.getFactionId()))
						{
							continue;
						}
						
						if (MostHate != null)
						{
							actor.addDamageHate(target, 0, actor.getHating(MostHate));
						}
						else
						{
							actor.addDamageHate(target, 0, 2000);
						}
						
						actor.setTarget(target);
						setTarget(target);
					}
				}
				else if (target instanceof L2Summon)
				{
					if (MostHate != null)
					{
						actor.addDamageHate(target, 0, actor.getHating(MostHate));
					}
					else
					{
						actor.addDamageHate(target, 0, 2000);
					}
					
					actor.setTarget(target);
					setTarget(target);
				}
			}
		}
	}
	
	/// XXX aggroReconsider() For Bosses and Minions
	private void aggroReconsider()
	{
		L2Attackable actor = getActiveChar();
		L2Character MostHate = actor.getMostHated();
		
		if (actor.getHateList() != null)
		{
			int rand = Rnd.get(actor.getHateList().size());
			int count = 0;
			
			for (L2Character obj : actor.getHateList())
			{
				if (count < rand)
				{
					count++;
					continue;
				}
				
				if ((obj == null) || obj.isDead() || !GeoData.getInstance().canSeeTarget(actor, obj) || (obj == actor))
				{
					continue;
				}
				
				// actor.setTarget(obj);
				
				if (MostHate != null)
				{
					actor.addDamageHate(obj, 0, actor.getHating(MostHate));
				}
				else
				{
					actor.addDamageHate(obj, 0, 2000);
				}
				
				actor.setTarget(obj);
				setTarget(obj);
				return;
			}
		}
		
		if (!(actor instanceof L2GuardInstance))
		{
			for (L2Character target : actor.getKnownList().getKnownType(L2Character.class))
			{
				if (target.isDead() || !GeoData.getInstance().canSeeTarget(actor, target) || (target != MostHate) || (target == actor))
				{
					continue;
				}
				
				if (target instanceof L2PcInstance)
				{
					if ((MostHate != null) && !MostHate.isDead())
					{
						actor.addDamageHate(target, 0, actor.getHating(MostHate));
					}
					else
					{
						actor.addDamageHate(target, 0, 2000);
					}
					
					actor.setTarget(target);
					setTarget(target);
				}
				else if (target instanceof L2Attackable)
				{
					if (actor.getIsChaos() != 0)
					{
						if ((((L2Attackable) target).getFactionId() != null) && ((L2Attackable) target).getFactionId().equals(actor.getFactionId()))
						{
							continue;
						}
						
						if (MostHate != null)
						{
							actor.addDamageHate(target, 0, actor.getHating(MostHate));
						}
						else
						{
							actor.addDamageHate(target, 0, 2000);
						}
						
						actor.setTarget(target);
						setTarget(target);
					}
				}
				else if (target instanceof L2Summon)
				{
					if (MostHate != null)
					{
						actor.addDamageHate(target, 0, actor.getHating(MostHate));
					}
					else
					{
						actor.addDamageHate(target, 0, 2000);
					}
					
					actor.setTarget(target);
					setTarget(target);
				}
			}
		}
	}
	
	private List<L2Skill> longRangeSkillRender()
	{
		longRangeSkills = _skillrender.getLongRangeSkills();
		if (longRangeSkills.isEmpty())
		{
			longRangeSkills = getActiveChar().getLongRangeSkill();
		}
		
		return longRangeSkills;
	}
	
	private List<L2Skill> shortRangeSkillRender()
	{
		shortRangeSkills = _skillrender.getShortRangeSkills();
		if (shortRangeSkills.isEmpty())
		{
			shortRangeSkills = getActiveChar().getShortRangeSkill();
		}
		
		return shortRangeSkills;
	}
}