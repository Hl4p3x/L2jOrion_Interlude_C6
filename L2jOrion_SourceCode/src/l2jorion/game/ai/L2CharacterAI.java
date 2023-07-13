package l2jorion.game.ai;

import static l2jorion.game.ai.CtrlIntention.AI_INTENTION_ACTIVE;
import static l2jorion.game.ai.CtrlIntention.AI_INTENTION_ATTACK;
import static l2jorion.game.ai.CtrlIntention.AI_INTENTION_CAST;
import static l2jorion.game.ai.CtrlIntention.AI_INTENTION_FOLLOW;
import static l2jorion.game.ai.CtrlIntention.AI_INTENTION_IDLE;
import static l2jorion.game.ai.CtrlIntention.AI_INTENTION_INTERACT;
import static l2jorion.game.ai.CtrlIntention.AI_INTENTION_MOVE_TO;
import static l2jorion.game.ai.CtrlIntention.AI_INTENTION_PICK_UP;
import static l2jorion.game.ai.CtrlIntention.AI_INTENTION_REST;

import java.util.ArrayList;
import java.util.List;

import l2jorion.bots.FakePlayer;
import l2jorion.game.geo.GeoData;
import l2jorion.game.model.L2Attackable;
import l2jorion.game.model.L2Character;
import l2jorion.game.model.L2Effect;
import l2jorion.game.model.L2Object;
import l2jorion.game.model.L2Skill;
import l2jorion.game.model.L2Skill.SkillType;
import l2jorion.game.model.Location;
import l2jorion.game.model.actor.instance.L2DoorInstance;
import l2jorion.game.model.actor.instance.L2ItemInstance;
import l2jorion.game.model.actor.instance.L2ItemInstance.ItemLocation;
import l2jorion.game.model.actor.instance.L2NpcInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.actor.instance.L2PlayableInstance;
import l2jorion.game.model.zone.ZoneId;
import l2jorion.game.network.serverpackets.AutoAttackStop;
import l2jorion.game.taskmanager.AttackStanceTaskManager;
import l2jorion.game.util.Util;

public class L2CharacterAI extends AbstractAI
{
	public static class IntentionCommand
	{
		protected final CtrlIntention _crtlIntention;
		protected final Object _arg0, _arg1;
		
		protected IntentionCommand(CtrlIntention pIntention, Object pArg0, Object pArg1)
		{
			_crtlIntention = pIntention;
			_arg0 = pArg0;
			_arg1 = pArg1;
		}
		
		public CtrlIntention getCtrlIntention()
		{
			return _crtlIntention;
		}
	}
	
	@Override
	protected void onEvtAttacked(L2Character attacker)
	{
		if (_actor instanceof FakePlayer)
		{
			_actor.setTarget(attacker);
		}
		
		clientStartAutoAttack();
	}
	
	public L2CharacterAI(L2Character creature)
	{
		super(creature);
	}
	
	@Override
	protected void onIntentionIdle()
	{
		changeIntention(AI_INTENTION_IDLE, null, null);
		
		setTarget(null);
		clientStopMoving(null);
		
		clientStopAutoAttack();
	}
	
	protected void onIntentionActive(final L2Character target)
	{
		if (target instanceof L2PcInstance && _actor instanceof L2PcInstance)
		{
			if (((L2PcInstance) _actor).getKarma() > 0 && _actor.getLevel() - target.getLevel() >= 10 && ((L2PlayableInstance) target).getProtectionBlessing() && !target.isInsideZone(ZoneId.ZONE_PVP))
			{
				clientActionFailed();
				return;
			}
		}
		
		if (getIntention() != AI_INTENTION_ACTIVE)
		{
			changeIntention(AI_INTENTION_ACTIVE, null, null);
			
			setTarget(null);
			clientStopMoving(null);
			clientStopAutoAttack();
			
			if (_actor instanceof L2Attackable)
			{
				((L2NpcInstance) _actor).startRandomAnimationTimer();
			}
			
			onEvtThink();
		}
	}
	
	@Override
	protected void onIntentionRest()
	{
		setIntention(AI_INTENTION_IDLE);
	}
	
	@Override
	protected void onIntentionAttack(L2Character target)
	{
		if (target == null)
		{
			clientActionFailed();
			return;
		}
		
		if (getIntention() == AI_INTENTION_REST)
		{
			clientActionFailed();
			return;
		}
		
		if (_actor.isAllSkillsDisabled() || _actor.isCastingNow() || _actor.isAfraid())
		{
			clientActionFailed();
			return;
		}
		
		if (getIntention() == AI_INTENTION_ATTACK)
		{
			if (getTarget() != target)
			{
				setTarget(target);
				stopFollow();
				notifyEvent(CtrlEvent.EVT_THINK);
			}
			else
			{
				notifyEvent(CtrlEvent.EVT_THINK); // to avoid stuck
				clientActionFailed();
			}
		}
		else
		{
			changeIntention(AI_INTENTION_ATTACK, target, null);
			setTarget(target);
			stopFollow();
			notifyEvent(CtrlEvent.EVT_THINK);
		}
	}
	
	@Override
	protected void onIntentionCast(L2Skill skill, L2Object target)
	{
		if (getIntention() == AI_INTENTION_REST && skill.isMagic())
		{
			clientActionFailed();
			return;
		}
		
		if (_actor.isAllSkillsDisabled() && !skill.isPotion())
		{
			clientActionFailed();
			return;
		}
		
		if (_actor.isMuted() && skill.isMagic())
		{
			clientActionFailed();
			return;
		}
		
		if (target instanceof L2PcInstance && _actor instanceof L2PcInstance)
		{
			if (((L2PcInstance) _actor).getKarma() > 0 && _actor.getLevel() - ((L2PcInstance) target).getLevel() >= 10 && ((L2PlayableInstance) target).getProtectionBlessing() && !((L2Character) target).isInsideZone(ZoneId.ZONE_PVP))
			{
				clientActionFailed();
				return;
			}
		}
		
		setTarget(target);
		
		if (skill.getHitTime() > 50)
		{
			_actor.abortAttack();
		}
		
		_skill = skill;
		
		changeIntention(AI_INTENTION_CAST, skill, target);
		
		notifyEvent(CtrlEvent.EVT_THINK);
	}
	
	protected void changeIntentionToCast(L2Skill skill, L2Object target)
	{
		if ((getIntention() == AI_INTENTION_REST) && skill.isMagic())
		{
			clientActionFailed();
			return;
		}
		
		setTarget(target);
		
		_skill = skill;
		
		changeIntention(AI_INTENTION_CAST, skill, target);
		
		notifyEvent(CtrlEvent.EVT_THINK);
	}
	
	@Override
	protected void onIntentionMoveTo(Location pos)
	{
		if (getIntention() == AI_INTENTION_REST)
		{
			clientActionFailed();
			return;
		}
		
		if (_actor.isAllSkillsDisabled() || _actor.isCastingNow())
		{
			clientActionFailed();
			return;
		}
		
		changeIntention(AI_INTENTION_MOVE_TO, pos, null);
		
		clientStopAutoAttack();
		
		_actor.abortAttack();
		
		moveTo(pos.getX(), pos.getY(), pos.getZ());
	}
	
	@Override
	protected void onIntentionFollow(final L2Character target)
	{
		if (getIntention() == AI_INTENTION_REST)
		{
			clientActionFailed();
			return;
		}
		
		if (_actor.isAllSkillsDisabled() || _actor.isCastingNow())
		{
			clientActionFailed();
			return;
		}
		
		if (_actor.isMovementDisabled())
		{
			clientActionFailed();
			return;
		}
		
		if (_actor.isImobilised() || _actor.isRooted())
		{
			clientActionFailed();
			return;
		}
		
		if (_actor.isDead())
		{
			clientActionFailed();
			return;
		}
		
		if (_actor == target)
		{
			clientActionFailed();
			return;
		}
		
		clientStopAutoAttack();
		
		changeIntention(AI_INTENTION_FOLLOW, target, null);
		
		startFollow(target);
	}
	
	@Override
	protected void onIntentionPickUp(final L2Object object)
	{
		if (getIntention() == AI_INTENTION_REST)
		{
			clientActionFailed();
			return;
		}
		
		if (_actor.isAllSkillsDisabled() || _actor.isCastingNow())
		{
			clientActionFailed();
			return;
		}
		
		if (object instanceof L2ItemInstance && ((L2ItemInstance) object).getLocation() != ItemLocation.VOID)
		{
			clientActionFailed();
			return;
		}
		
		clientStopAutoAttack();
		
		changeIntention(AI_INTENTION_PICK_UP, object, null);
		
		setTarget(object);
		
		if (object.getX() == 0 && object.getY() == 0)
		{
			final L2Character player_char = getActor();
			if (player_char instanceof L2PcInstance)
			{
				clientActionFailed();
				return;
			}
			object.setXYZ(getActor().getX(), getActor().getY(), getActor().getZ() + 5);
		}
		
		moveToPawn(object, 20);
	}
	
	@Override
	protected void onIntentionInteract(final L2Object object)
	{
		if (getIntention() == AI_INTENTION_REST)
		{
			clientActionFailed();
			return;
		}
		
		if (_actor.isCastingNow() || _actor.isAllSkillsDisabled())
		{
			clientActionFailed();
			return;
		}
		
		clientStopAutoAttack();
		
		if (getIntention() != AI_INTENTION_INTERACT)
		{
			changeIntention(AI_INTENTION_INTERACT, object, null);
			
			setTarget(object);
			
			moveToPawn(object, 60);
		}
	}
	
	@Override
	protected void onEvtThink()
	{
	}
	
	@Override
	protected void onEvtAggression(final L2Character target, final int aggro)
	{
	}
	
	@Override
	protected void onEvtStunned(final L2Character attacker)
	{
		// Stop the actor auto-attack client side by sending Server->Client packet AutoAttackStop (broadcast)
		_actor.broadcastPacket(new AutoAttackStop(_actor.getObjectId()));
		
		if (AttackStanceTaskManager.getInstance().getAttackStanceTask(_actor))
		{
			AttackStanceTaskManager.getInstance().removeAttackStanceTask(_actor);
		}
		
		// Stop Server AutoAttack also
		setAutoAttacking(false);
		
		// Stop the actor movement server side AND client side by sending Server->Client packet StopMove/StopRotation (broadcast)
		clientStopMoving(null);
		
		// Launch actions corresponding to the Event onAttacked (only for L2AttackableAI after the stunning periode)
		onEvtAttacked(attacker);
	}
	
	@Override
	protected void onEvtParalyzed(L2Character attacker)
	{
		_actor.broadcastPacket(new AutoAttackStop(_actor.getObjectId()));
		
		if (AttackStanceTaskManager.getInstance().getAttackStanceTask(_actor))
		{
			AttackStanceTaskManager.getInstance().removeAttackStanceTask(_actor);
		}
		
		// Stop Server AutoAttack also
		setAutoAttacking(false);
		
		// Stop the actor movement server side AND client side by sending Server->Client packet StopMove/StopRotation (broadcast)
		clientStopMoving(null);
		
		// Launch actions corresponding to the Event onAttacked (only for L2AttackableAI after the stunning periode)
		onEvtAttacked(attacker);
	}
	
	@Override
	protected void onEvtSleeping(final L2Character attacker)
	{
		_actor.broadcastPacket(new AutoAttackStop(_actor.getObjectId()));
		
		if (AttackStanceTaskManager.getInstance().getAttackStanceTask(_actor))
		{
			AttackStanceTaskManager.getInstance().removeAttackStanceTask(_actor);
		}
		
		setAutoAttacking(false);
		
		clientStopMoving(null);
	}
	
	@Override
	protected void onEvtRooted(final L2Character attacker)
	{
		clientStopMoving(null);
		
		onEvtAttacked(attacker);
	}
	
	@Override
	protected void onEvtConfused(final L2Character attacker)
	{
		clientStopMoving(null);
		
		onEvtAttacked(attacker);
	}
	
	@Override
	protected void onEvtMuted(final L2Character attacker)
	{
		onEvtAttacked(attacker);
	}
	
	@Override
	protected void onEvtReadyToAct()
	{
		onEvtThink();
	}
	
	@Override
	protected void onEvtUserCmd(final Object arg0, final Object arg1)
	{
	}
	
	@Override
	protected void onEvtArrived()
	{
		_actor.revalidateZone(true);
		
		if (_actor.moveToNextRoutePoint())
		{
			return;
		}
		
		clientStoppedMoving();
		
		if (getIntention() == AI_INTENTION_MOVE_TO)
		{
			setIntention(AI_INTENTION_ACTIVE);
		}
		
		onEvtThink();
	}
	
	@Override
	protected void onEvtArrivedRevalidate()
	{
		onEvtThink();
	}
	
	@Override
	protected void onEvtArrivedBlocked(final Location blocked_at_pos)
	{
		if ((getIntention() == AI_INTENTION_MOVE_TO) || (getIntention() == AI_INTENTION_CAST))
		{
			setIntention(AI_INTENTION_ACTIVE);
		}
		
		clientStopMoving(blocked_at_pos);
		
		onEvtThink();
	}
	
	@Override
	protected void onEvtForgetObject(final L2Object object)
	{
		if (getTarget() == object)
		{
			setTarget(null);
			
			setIntention(AI_INTENTION_ACTIVE);
		}
		
		if (getFollowTarget() == object)
		{
			clientStopMoving(null);
			
			stopFollow();
			
			setIntention(AI_INTENTION_ACTIVE);
		}
		
		if (_actor == object)
		{
			setTarget(null);
			
			stopFollow();
			
			clientStopMoving(null);
			
			changeIntention(AI_INTENTION_IDLE, null, null);
		}
	}
	
	@Override
	protected void onEvtCancel()
	{
		stopFollow();
		
		if (!AttackStanceTaskManager.getInstance().getAttackStanceTask(_actor))
		{
			_actor.broadcastPacket(new AutoAttackStop(_actor.getObjectId()));
		}
		
		onEvtThink();
	}
	
	@Override
	protected void onEvtDead()
	{
		stopAITask();
		
		clientNotifyDead();
		
		if (!(_actor instanceof L2PcInstance))
		{
			_actor.setWalking();
		}
	}
	
	@Override
	protected void onEvtFakeDeath()
	{
		stopFollow();
		
		clientStopMoving(null);
		
		setIntention(AI_INTENTION_IDLE);
		setTarget(null);
	}
	
	@Override
	protected void onEvtFinishCasting()
	{
	}
	
	public boolean maybeMoveToPawn(L2Object target, int offset)
	{
		if (target == null || offset < 0)
		{
			return false;
		}
		
		offset += _actor.getTemplate().getCollisionRadius();
		if (target instanceof L2Character)
		{
			offset += ((L2Character) target).getTemplate().getCollisionRadius();
		}
		
		final boolean needToMove;
		
		if (target.isDoor())
		{
			L2DoorInstance dor = (L2DoorInstance) target;
			int xPoint = 0;
			int yPoint = 0;
			for (int i : dor.getTemplate().getNodeX())
			{
				xPoint += i;
			}
			for (int i : dor.getTemplate().getNodeY())
			{
				yPoint += i;
			}
			xPoint /= 4;
			yPoint /= 4;
			needToMove = !_actor.isInsideRadius(xPoint, yPoint, dor.getTemplate().getNodeZ(), offset, false, false);
		}
		else
		{
			needToMove = !Util.checkIfInRange(offset, _actor, target, true);
		}
		
		if (needToMove)
		{
			if (getFollowTarget() != null)
			{
				// It will give +50 range if target moves
				if (!_actor.isInsideRadius(target, offset + 50, false, false))
				{
					return true;
				}
				
				stopFollow();
				return false;
			}
			
			if (_actor.isMovementDisabled())
			{
				if (_actor.getAI().getIntention() == CtrlIntention.AI_INTENTION_ATTACK)
				{
					_actor.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
				}
				
				return true;
			}
			
			if (!_actor.isRunning() && !(this instanceof L2PlayerAI) && !(this instanceof L2SummonAI))
			{
				_actor.setRunning();
			}
			
			stopFollow();
			
			if ((target instanceof L2Character) && !(target instanceof L2DoorInstance))
			{
				startFollow((L2Character) target, offset);
			}
			else
			{
				moveToPawn(target, offset);
			}
			
			return true;
		}
		
		if (getFollowTarget() != null)
		{
			stopFollow();
		}
		return false;
	}
	
	protected boolean checkTargetLostOrDead(L2Object target)
	{
		if (target == null || ((L2Character) target).isAlikeDead())
		{
			if (target != null && ((L2Character) target).isFakeDeath())
			{
				// target.stopFakeDeath(null);
				return false;
			}
			
			setIntention(AI_INTENTION_ACTIVE);
			
			return true;
		}
		return false;
	}
	
	protected boolean checkTargetLost(final L2Object target)
	{
		if (target instanceof L2PcInstance)
		{
			L2PcInstance target2 = (L2PcInstance) target;
			
			if (target2.isFakeDeath())
			{
				target2.stopFakeDeath(null);
				return false;
			}
		}
		
		if (target == null)
		{
			setIntention(AI_INTENTION_ACTIVE);
			return true;
		}
		
		return false;
	}
	
	@Override
	protected void onIntentionActive()
	{
		if (getIntention() != AI_INTENTION_ACTIVE)
		{
			changeIntention(AI_INTENTION_ACTIVE, null, null);
			
			setTarget(null);
			clientStopMoving(null);
			clientStopAutoAttack();
			
			if (_actor instanceof L2Attackable)
			{
				((L2NpcInstance) _actor).startRandomAnimationTimer();
			}
			
			onEvtThink();
		}
	}
	
	public IntentionCommand getNextIntention()
	{
		return null;
	}
	
	protected class SelfAnalysis
	{
		public boolean isMage = false;
		public boolean isBalanced;
		public boolean isArcher = false;
		public boolean isHealer = false;
		public boolean isFighter = false;
		public boolean cannotMoveOnLand = false;
		
		public int lastBuffTick = 0;
		public int lastDebuffTick = 0;
		
		public List<L2Skill> generalSkills = new ArrayList<>();
		public List<L2Skill> buffSkills = new ArrayList<>();
		public List<L2Skill> debuffSkills = new ArrayList<>();
		public List<L2Skill> cancelSkills = new ArrayList<>();
		public List<L2Skill> healSkills = new ArrayList<>();
		public List<L2Skill> generalDisablers = new ArrayList<>();
		public List<L2Skill> sleepSkills = new ArrayList<>();
		public List<L2Skill> rootSkills = new ArrayList<>();
		public List<L2Skill> muteSkills = new ArrayList<>();
		public List<L2Skill> resurrectSkills = new ArrayList<>();
		
		public boolean hasHealOrResurrect = false;
		public boolean hasLongRangeSkills = false;
		public boolean hasLongRangeDamageSkills = false;
		
		public int maxCastRange = 0;
		
		public SelfAnalysis()
		{
		}
		
		public void init()
		{
			switch (((L2NpcInstance) _actor).getAIData().getAiType())
			{
				case FIGHTER:
					isFighter = true;
					break;
				case MAGE:
					isMage = true;
					break;
				case CORPSE:
				case BALANCED:
					isBalanced = true;
					break;
				case ARCHER:
					isArcher = true;
					break;
				case HEALER:
					isHealer = true;
					break;
				default:
					isFighter = true;
					break;
			}
			
			// water movement analysis
			if (_actor instanceof L2NpcInstance)
			{
				int npcId = ((L2NpcInstance) _actor).getNpcId();
				
				switch (npcId)
				{
					case 20314: // great white shark
					case 20849: // Light Worm
						cannotMoveOnLand = true;
						break;
					default:
						cannotMoveOnLand = false;
						break;
				}
			}
			
			// skill analysis
			for (L2Skill sk : _actor.getAllSkills())
			{
				if (sk.isPassive())
				{
					continue;
				}
				
				int castRange = sk.getCastRange();
				boolean hasLongRangeDamageSkill = false;
				switch (sk.getSkillType())
				{
					case HEAL:
					case HEAL_PERCENT:
					case HEAL_STATIC:
					case BALANCE_LIFE:
					case HOT:
						healSkills.add(sk);
						hasHealOrResurrect = true;
						continue;
					case BUFF:
						buffSkills.add(sk);
						continue;
					case PARALYZE:
					case STUN:
						switch (sk.getId())
						{
							case 367:
							case 4111:
							case 4383:
							case 4616:
							case 4578:
								sleepSkills.add(sk);
								break;
							default:
								generalDisablers.add(sk);
								break;
						}
						break;
					case MUTE:
						muteSkills.add(sk);
						break;
					case SLEEP:
						sleepSkills.add(sk);
						break;
					case ROOT:
						rootSkills.add(sk);
						break;
					case FEAR:
					case CONFUSION:
					case DEBUFF:
						debuffSkills.add(sk);
						break;
					case CANCEL:
					case MAGE_BANE:
					case WARRIOR_BANE:
					case NEGATE:
						cancelSkills.add(sk);
						break;
					case RESURRECT:
						resurrectSkills.add(sk);
						hasHealOrResurrect = true;
						break;
					case NOTDONE:
					case COREDONE:
						continue;
					default:
						generalSkills.add(sk);
						hasLongRangeDamageSkill = true;
						break;
				}
				
				if (castRange > 70)
				{
					hasLongRangeSkills = true;
					if (hasLongRangeDamageSkill)
					{
						hasLongRangeDamageSkills = true;
					}
				}
				
				if (castRange > maxCastRange)
				{
					maxCastRange = castRange;
				}
			}
			
			// Because of missing skills, some mages/balanced cannot play like mages
			if (!hasLongRangeDamageSkills && isMage)
			{
				isBalanced = true;
				isMage = false;
				isFighter = false;
			}
			
			if (!hasLongRangeSkills && (isMage || isBalanced))
			{
				isBalanced = false;
				isMage = false;
				isFighter = true;
			}
			
			if (generalSkills.isEmpty() && isMage)
			{
				isBalanced = true;
				isMage = false;
			}
		}
	}
	
	public boolean canAura(L2Skill sk)
	{
		if ((sk.getTargetType() == L2Skill.SkillTargetType.TARGET_AURA) || (sk.getTargetType() == L2Skill.SkillTargetType.TARGET_BEHIND_AURA) || (sk.getTargetType() == L2Skill.SkillTargetType.TARGET_FRONT_AURA))
		{
			for (L2Object target : _actor.getKnownList().getKnownTypeInRadius(L2Character.class, sk.getSkillRadius()))
			{
				if (target == getTarget())
				{
					return true;
				}
			}
		}
		return false;
	}
	
	public boolean canAOE(L2Skill sk)
	{
		if ((sk.getSkillType() != SkillType.NEGATE) || (sk.getSkillType() != SkillType.CANCEL))
		{
			if ((sk.getTargetType() == L2Skill.SkillTargetType.TARGET_AURA) || (sk.getTargetType() == L2Skill.SkillTargetType.TARGET_BEHIND_AURA) || (sk.getTargetType() == L2Skill.SkillTargetType.TARGET_FRONT_AURA))
			{
				boolean cancast = true;
				for (L2Character target : _actor.getKnownList().getKnownTypeInRadius(L2Character.class, sk.getSkillRadius()))
				{
					if (!GeoData.getInstance().canSeeTarget(_actor, target))
					{
						continue;
					}
					
					if (target instanceof L2Attackable)
					{
						L2NpcInstance actor = ((L2NpcInstance) _actor);
						
						if ((actor.getFactionId() == null) || ((actor.getFactionId() == null) && (actor.getIsChaos() == 0)))
						{
							continue;
						}
					}
					
					if (target.getFirstEffect(sk) != null)
					{
						cancast = false;
					}
				}
				
				if (cancast)
				{
					return true;
				}
			}
			else if ((sk.getTargetType() == L2Skill.SkillTargetType.TARGET_AREA) || (sk.getTargetType() == L2Skill.SkillTargetType.TARGET_BEHIND_AREA) || (sk.getTargetType() == L2Skill.SkillTargetType.TARGET_FRONT_AREA))
			{
				boolean cancast = true;
				for (L2Character target : getTarget().getKnownList().getKnownTypeInRadius(L2Character.class, sk.getSkillRadius()))
				{
					if (!GeoData.getInstance().canSeeTarget(_actor, target) || (target == null))
					{
						continue;
					}
					if (target instanceof L2Attackable)
					{
						L2NpcInstance actor = ((L2NpcInstance) _actor);
						if ((actor.getFactionId() == null) || ((actor.getFactionId() == null) && (actor.getIsChaos() == 0)))
						{
							continue;
						}
					}
					L2Effect[] effects = target.getAllEffects();
					if (effects.length > 0)
					{
						cancast = true;
					}
				}
				if (cancast)
				{
					return true;
				}
			}
		}
		else
		{
			if ((sk.getTargetType() == L2Skill.SkillTargetType.TARGET_AURA) || (sk.getTargetType() == L2Skill.SkillTargetType.TARGET_BEHIND_AURA) || (sk.getTargetType() == L2Skill.SkillTargetType.TARGET_FRONT_AURA))
			{
				boolean cancast = false;
				for (L2Character target : _actor.getKnownList().getKnownTypeInRadius(L2Character.class, sk.getSkillRadius()))
				{
					if (!GeoData.getInstance().canSeeTarget(_actor, target))
					{
						continue;
					}
					
					if (target instanceof L2Attackable)
					{
						L2NpcInstance actor = ((L2NpcInstance) _actor);
						if ((actor.getFactionId() == null) || ((actor.getFactionId() == null) && (actor.getIsChaos() == 0)))
						{
							continue;
						}
					}
					L2Effect[] effects = target.getAllEffects();
					if (effects.length > 0)
					{
						cancast = true;
					}
				}
				if (cancast)
				{
					return true;
				}
			}
			else if ((sk.getTargetType() == L2Skill.SkillTargetType.TARGET_AREA) || (sk.getTargetType() == L2Skill.SkillTargetType.TARGET_BEHIND_AREA) || (sk.getTargetType() == L2Skill.SkillTargetType.TARGET_FRONT_AREA))
			{
				boolean cancast = true;
				for (L2Character target : getTarget().getKnownList().getKnownTypeInRadius(L2Character.class, sk.getSkillRadius()))
				{
					if (!GeoData.getInstance().canSeeTarget(_actor, target))
					{
						continue;
					}
					
					if (target instanceof L2Attackable)
					{
						L2NpcInstance actor = ((L2NpcInstance) _actor);
						if ((actor.getFactionId() == null) || ((actor.getFactionId() == null) && (actor.getIsChaos() == 0)))
						{
							continue;
						}
					}
					
					if (target.getFirstEffect(sk) != null)
					{
						cancast = false;
					}
				}
				
				if (cancast)
				{
					return true;
				}
			}
		}
		return false;
	}
	
	public boolean canParty(L2Skill sk)
	{
		if (sk.getTargetType() == L2Skill.SkillTargetType.TARGET_PARTY)
		{
			int count = 0;
			int ccount = 0;
			for (L2Character target : _actor.getKnownList().getKnownTypeInRadius(L2Character.class, sk.getSkillRadius()))
			{
				if (!(target instanceof L2Attackable) || !GeoData.getInstance().canSeeTarget(_actor, target))
				{
					continue;
				}
				
				L2NpcInstance targets = ((L2NpcInstance) target);
				L2NpcInstance actors = ((L2NpcInstance) _actor);
				
				if ((actors.getFactionId() != null) && targets.getFactionId().equals(actors.getFactionId()))
				{
					count++;
					
					if (target.getFirstEffect(sk) != null)
					{
						ccount++;
					}
				}
			}
			
			if (ccount < count)
			{
				return true;
			}
		}
		return false;
	}
	
	public boolean isParty(L2Skill sk)
	{
		if (sk.getTargetType() == L2Skill.SkillTargetType.TARGET_PARTY)
		{
			return true;
		}
		
		return false;
	}
}
