/*
 * L2jOrion Project - www.l2jorion.com 
 * 
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
 */
package l2jorion.game.ai;

import static l2jorion.game.ai.CtrlIntention.AI_INTENTION_ACTIVE;
import static l2jorion.game.ai.CtrlIntention.AI_INTENTION_ATTACK;
import static l2jorion.game.ai.CtrlIntention.AI_INTENTION_IDLE;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import l2jorion.game.controllers.GameTimeController;
import l2jorion.game.geo.GeoData;
import l2jorion.game.model.L2Attackable;
import l2jorion.game.model.L2Character;
import l2jorion.game.model.L2Object;
import l2jorion.game.model.L2Skill;
import l2jorion.game.model.L2Skill.SkillType;
import l2jorion.game.model.L2Summon;
import l2jorion.game.model.actor.instance.L2DoorInstance;
import l2jorion.game.model.actor.instance.L2NpcInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.actor.instance.L2PlayableInstance;
import l2jorion.game.model.actor.instance.L2SiegeGuardInstance;
import l2jorion.game.thread.ThreadPoolManager;
import l2jorion.game.util.Util;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;
import l2jorion.util.random.Rnd;

public class L2SiegeGuardAI extends L2CharacterAI implements Runnable
{
	protected static final Logger LOG = LoggerFactory.getLogger(L2SiegeGuardAI.class);
	
	private final List<Integer> _allied = new ArrayList<>();
	
	private static final int MAX_ATTACK_TIMEOUT = 300;
	private Future<?> _aiTask;
	private final SelfAnalysis _selfAnalysis = new SelfAnalysis();
	private int _attackTimeout;
	private int _globalAggro;
	private boolean _thinking;
	private final int _attackRange;
	
	public L2SiegeGuardAI(L2Character creature)
	{
		super(creature);
		
		_selfAnalysis.init();
		_attackTimeout = Integer.MAX_VALUE;
		_globalAggro = -10; // 10 seconds timeout of ATTACK after respawn
		_attackRange = ((L2Attackable) _actor).getPhysicalAttackRange();
	}
	
	@Override
	public void run()
	{
		// Launch actions corresponding to the Event Think
		onEvtThink();
	}
	
	public List<Integer> getAlly()
	{
		return _allied;
	}
	
	/**
	 * <B><U> Actor is a L2GuardInstance</U> :</B>
	 * <ul>
	 * <li>The target isn't a Folk or a Door</li>
	 * <li>The target isn't dead, isn't invulnerable, isn't in silent moving mode AND too far (>100)</li>
	 * <li>The target is in the actor Aggro range and is at the same height</li>
	 * <li>The L2PcInstance target has karma (=PK)</li>
	 * <li>The L2MonsterInstance target is aggressive</li>
	 * </ul>
	 * <B><U> Actor is a L2SiegeGuardInstance</U> :</B>
	 * <ul>
	 * <li>The target isn't a Folk or a Door</li>
	 * <li>The target isn't dead, isn't invulnerable, isn't in silent moving mode AND too far (>100)</li>
	 * <li>The target is in the actor Aggro range and is at the same height</li>
	 * <li>A siege is in progress</li>
	 * <li>The L2PcInstance target isn't a Defender</li>
	 * </ul>
	 * <B><U> Actor is a L2FriendlyMobInstance</U> :</B>
	 * <ul>
	 * <li>The target isn't a Folk, a Door or another L2NpcInstance</li>
	 * <li>The target isn't dead, isn't invulnerable, isn't in silent moving mode AND too far (>100)</li>
	 * <li>The target is in the actor Aggro range and is at the same height</li>
	 * <li>The L2PcInstance target has karma (=PK)</li>
	 * </ul>
	 * <B><U> Actor is a L2MonsterInstance</U> :</B>
	 * <ul>
	 * <li>The target isn't a Folk, a Door or another L2NpcInstance</li>
	 * <li>The target isn't dead, isn't invulnerable, isn't in silent moving mode AND too far (>100)</li>
	 * <li>The target is in the actor Aggro range and is at the same height</li>
	 * <li>The actor is Aggressive</li>
	 * </ul>
	 * @param target The targeted L2Object
	 * @return True if the target is autoattackable (depends on the actor type).
	 */
	protected boolean autoAttackCondition(L2Character target)
	{
		// Check if the target isn't another guard, folk or a door
		if ((target == null) || target.isAlikeDead() || (target instanceof L2SiegeGuardInstance) || (target instanceof L2NpcInstance) || (target instanceof L2DoorInstance))
		{
			return false;
		}
		
		// Check if the target is a player or a summon
		if (target instanceof L2PlayableInstance)
		{
			final L2PcInstance player = target.getActingPlayer();
			
			// Check if the target isn't GM on hide mode.
			if (player.isGM() && player.getAppearance().getInvisible())
			{
				return false;
			}
			
			// Check if the target isn't in silent move mode AND too far
			if (player.isSilentMoving() && !_actor.isInsideRadius(player, 250, false, false))
			{
				return false;
			}
			
			// Get the owner if the target is a summon and player is near the NPC.
			if (target instanceof L2Summon)
			{
				if (_actor.isInsideRadius(player, 1000, true, false))
				{
					target = player;
				}
			}
		}
		
		if (_allied.contains(target.getObjectId()))
		{
			return false;
		}
		
		// Los Check Here
		return (_actor.isAutoAttackable(target) && GeoData.getInstance().canSeeTarget(_actor, target));
	}
	
	/**
	 * Set the Intention of this L2CharacterAI and create an AI Task executed every 1s (call onEvtThink method) for this L2Attackable.<BR>
	 * <BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : If actor _knowPlayer isn't EMPTY, AI_INTENTION_IDLE will be change in AI_INTENTION_ACTIVE</B></FONT><BR>
	 * <BR>
	 * @param intention The new Intention to set to the AI
	 * @param arg0 The first parameter of the Intention
	 * @param arg1 The second parameter of the Intention
	 */
	@Override
	public synchronized void changeIntention(CtrlIntention intention, Object arg0, Object arg1)
	{
		if (intention == AI_INTENTION_IDLE) // active becomes idle if only a summon is present
		{
			// Check if actor is not dead
			if (!_actor.isAlikeDead())
			{
				// If its _knownPlayer isn't empty set the Intention to AI_INTENTION_ACTIVE
				if (_actor.getKnownList().getKnownPlayers().size() > 0)
				{
					// Set the Intention of this L2AttackableAI to intention
					super.changeIntention(AI_INTENTION_ACTIVE, arg0, arg1);
					
					// If not idle - create an AI task (schedule onEvtThink repeatedly)
					if (_aiTask == null)
					{
						_aiTask = ThreadPoolManager.getInstance().scheduleAiAtFixedRate(this, 1000, 1000);
					}
					
					return;
				}
			}
			// Set the Intention of this L2AttackableAI to AI_INTENTION_IDLE
			super.changeIntention(AI_INTENTION_IDLE, null, null);
			
			// Stop AI task and detach AI from NPC
			stopAITask();
		}
		
		// Set the Intention of this L2AttackableAI to intention
		super.changeIntention(intention, arg0, arg1);
		
		// If not idle - create an AI task (schedule onEvtThink repeatedly)
		if (_aiTask == null)
		{
			_aiTask = ThreadPoolManager.getInstance().scheduleAiAtFixedRate(this, 1000, 1000);
		}
	}
	
	/**
	 * Manage the Attack Intention :
	 * <ul>
	 * <li>Stop current Attack (if necessary)</li>
	 * <li>Calculate attack timeout</li>
	 * <li>Start a new Attack and Launch Think Event.</li>
	 * </ul>
	 * @param target The L2Character to attack
	 */
	@Override
	protected void onIntentionAttack(L2Character target)
	{
		// Calculate the attack timeout
		_attackTimeout = MAX_ATTACK_TIMEOUT + GameTimeController.getInstance().getGameTicks();
		
		super.onIntentionAttack(target);
	}
	
	/**
	 * Manage AI standard thinks of a L2Attackable (called by onEvtThink).
	 * <ul>
	 * <li>Update every 1s the _globalAggro counter to come close to 0</li>
	 * <li>If the actor is Aggressive and can attack, add all autoAttackable L2Character in its Aggro Range to its _aggroList, chose a target and order to attack it</li>
	 * <li>If the actor can't attack, order to it to return to its home location</li>
	 * </ul>
	 */
	private void thinkActive()
	{
		// Update every 1s the _globalAggro counter to come close to 0
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
		
		// Add all autoAttackable L2Character in L2Attackable Aggro Range to its _aggroList with 0 damage and 1 hate
		// A L2Attackable isn't aggressive during 10s after its spawn because _globalAggro is set to -10
		if (_globalAggro >= 0)
		{
			final L2Attackable npc = (L2Attackable) _actor;
			for (L2Character target : npc.getKnownList().getKnownCharactersInRadius(_attackRange))
			{
				if (target == null)
				{
					continue;
				}
				
				if (autoAttackCondition(target)) // check aggression
				{
					// Get the hate level of the L2Attackable against this target, and add the attacker to the L2Attackable _aggroList
					int hating = npc.getHating(target);
					if (hating == 0)
					{
						npc.addDamageHate(target, 0, 1);
					}
				}
			}
			
			// Chose a target from its aggroList
			final L2Character hated = (_actor.isConfused()) ? getAttackTarget() : npc.getMostHated();
			
			// Order to the L2Attackable to attack the target
			if (hated != null)
			{
				// Get the hate level of the L2Attackable against this L2Character target contained in _aggroList
				int aggro = npc.getHating(hated);
				
				if ((aggro + _globalAggro) > 0)
				{
					// Set the L2Character movement type to run and send Server->Client packet ChangeMoveType to all others L2PcInstance
					if (!_actor.isRunning())
					{
						_actor.setRunning();
					}
					
					// Set the AI Intention to AI_INTENTION_ATTACK
					setIntention(CtrlIntention.AI_INTENTION_ATTACK, hated, null);
				}
				return;
			}
			
		}
		// Order to the L2SiegeGuardInstance to return to its home location because there's no target to attack
		((L2SiegeGuardInstance) _actor).returnHome();
	}
	
	/**
	 * Manage AI attack thinks of a L2Attackable (called by onEvtThink).
	 * <ul>
	 * <li>Update the attack timeout if actor is running</li>
	 * <li>If target is dead or timeout is expired, stop this attack and set the Intention to AI_INTENTION_ACTIVE</li>
	 * <li>Call all L2Object of its Faction inside the Faction Range</li>
	 * <li>Chose a target and order to attack it with magic skill or physical attack</li>
	 * </ul>
	 * TODO: Manage casting rules to healer mobs (like Ant Nurses)
	 */
	private void thinkAttack()
	{
		if (_attackTimeout < GameTimeController.getInstance().getGameTicks())
		{
			// Check if the actor is running
			if (_actor.isRunning())
			{
				// Set the actor movement type to walk and send Server->Client packet ChangeMoveType to all others L2PcInstance
				_actor.setWalking();
				
				// Calculate a new attack timeout
				_attackTimeout = MAX_ATTACK_TIMEOUT + GameTimeController.getInstance().getGameTicks();
			}
		}
		
		// Check if target is dead or if timeout is expired to stop this attack
		final L2Character attackTarget = getAttackTarget();
		if ((attackTarget == null) || attackTarget.isAlikeDead() || (_attackTimeout < GameTimeController.getInstance().getGameTicks()))
		{
			// Stop hating this target after the attack timeout or if target is dead
			if (attackTarget != null)
			{
				((L2Attackable) _actor).stopHating(attackTarget);
			}
			
			// Cancel target and timeout
			_attackTimeout = Integer.MAX_VALUE;
			setAttackTarget(null);
			
			// Set the AI Intention to AI_INTENTION_ACTIVE
			setIntention(AI_INTENTION_ACTIVE, null, null);
			
			_actor.setWalking();
			return;
		}
		
		factionNotifyAndSupport();
		attackPrepare();
	}
	
	private final void factionNotifyAndSupport()
	{
		// Verify if the actor exists.
		if (_actor == null)
		{
			return;
		}
		
		// Verify his faction.
		final String factionId = ((L2NpcInstance) _actor).getFactionId();
		if (factionId == null)
		{
			return;
		}
		
		// Verify if the target exists or is invul.
		final L2Character target = getAttackTarget();
		if ((target == null) || target.isInvul())
		{
			return;
		}
		
		// Go through all L2Object that belong to its faction
		for (L2Character cha : _actor.getKnownList().getKnownCharactersInRadius(1000))
		{
			if (cha == null)
			{
				continue;
			}
			
			if (!(cha instanceof L2NpcInstance))
			{
				if (_selfAnalysis.hasHealOrResurrect && (cha instanceof L2PcInstance) && (((L2NpcInstance) _actor).getCastle().getSiege().checkIsDefender(((L2PcInstance) cha).getClan())))
				{
					// heal friends
					if (!_actor.isAttackingDisabled() && (cha.getCurrentHp() < (cha.getMaxHp() * 0.6)) && (_actor.getCurrentHp() > (_actor.getMaxHp() / 2)) && (_actor.getCurrentMp() > (_actor.getMaxMp() / 2)) && cha.isInCombat())
					{
						for (L2Skill sk : _selfAnalysis.healSkills)
						{
							if (_actor.getCurrentMp() < sk.getMpConsume())
							{
								continue;
							}
							
							if (_actor.isSkillDisabled(sk))
							{
								continue;
							}
							
							if (!Util.checkIfInRange(sk.getCastRange(), _actor, cha, true))
							{
								continue;
							}
							
							if (Rnd.get(100) < 5)
							{
								continue;
							}
							
							if (!GeoData.getInstance().canSeeTarget(_actor, cha))
							{
								break;
							}
							
							L2Object OldTarget = _actor.getTarget();
							_actor.setTarget(cha);
							clientStopMoving(null);
							_actor.doCast(sk);
							_actor.setTarget(OldTarget);
							return;
						}
					}
				}
				continue;
			}
			
			L2NpcInstance npc = (L2NpcInstance) cha;
			
			if (!factionId.equals(npc.getFactionId()))
			{
				continue;
			}
			
			if (!npc.isDead() && (Math.abs(target.getZ() - npc.getZ()) < 600) && ((npc.getAI()._intention == CtrlIntention.AI_INTENTION_IDLE) || (npc.getAI()._intention == CtrlIntention.AI_INTENTION_ACTIVE)) && target.isInsideRadius(npc, 1500, true, false) && GeoData.getInstance().canSeeTarget(npc, target))
			{
				// Notify the L2Object AI with EVT_AGGRESSION
				npc.getAI().notifyEvent(CtrlEvent.EVT_AGGRESSION, getAttackTarget(), 1);
				return;
			}
			
			// heal friends
			if (_selfAnalysis.hasHealOrResurrect && !_actor.isAttackingDisabled() && (npc.getCurrentHp() < (npc.getMaxHp() * 0.6)) && (_actor.getCurrentHp() > (_actor.getMaxHp() / 2)) && (_actor.getCurrentMp() > (_actor.getMaxMp() / 2)) && npc.isInCombat())
			{
				for (L2Skill sk : _selfAnalysis.healSkills)
				{
					if (_actor.getCurrentMp() < sk.getMpConsume())
					{
						continue;
					}
					
					if (_actor.isSkillDisabled(sk))
					{
						continue;
					}
					
					if (!Util.checkIfInRange(sk.getCastRange(), _actor, npc, true))
					{
						continue;
					}
					
					if (Rnd.get(100) < 5)
					{
						continue;
					}
					
					if (!GeoData.getInstance().canSeeTarget(_actor, npc))
					{
						break;
					}
					
					L2Object OldTarget = _actor.getTarget();
					_actor.setTarget(npc);
					clientStopMoving(null);
					_actor.doCast(sk);
					_actor.setTarget(OldTarget);
					return;
				}
			}
		}
	}
	
	/**
	 * Prepare NPC attacks.<br>
	 * The NPC will try to cast spells or attack his target in melee if his AI isn't considered as an healer.
	 */
	private void attackPrepare()
	{
		if (_actor == null)
		{
			return;
		}
		
		L2Character attackTarget = getAttackTarget();
		if (attackTarget == null)
		{
			_actor.setTarget(null);
			setIntention(AI_INTENTION_IDLE, null, null);
			return;
		}
		
		final L2SiegeGuardInstance sGuard = (L2SiegeGuardInstance) _actor;
		
		// never attack defenders
		if ((attackTarget instanceof L2PcInstance) && sGuard.getCastle().getSiege().checkIsDefender(((L2PcInstance) attackTarget).getClan()))
		{
			sGuard.stopHating(attackTarget);
			_actor.setTarget(null);
			setIntention(AI_INTENTION_IDLE, null, null);
			return;
		}
		
		// If actor can't see target, don't attack anymore.
		if (!GeoData.getInstance().canSeeTarget(_actor, attackTarget))
		{
			sGuard.stopHating(attackTarget);
			_actor.setTarget(null);
			setIntention(AI_INTENTION_IDLE, null, null);
			return;
		}
		
		// Get all informations needed to choose between physical or magical attack
		double dist = _actor.getPlanDistanceSq(attackTarget.getX(), attackTarget.getY());
		int range = _actor.getPhysicalAttackRange() + _actor.getTemplate().getCollisionRadius() + _attackTarget.getTemplate().getCollisionRadius();
		if (attackTarget.isMoving())
		{
			range += 50;
		}
		
		_actor.setTarget(attackTarget);
		
		// Check if the actor isn't muted and if it is far from target
		if (!_actor.isMuted() && (dist > (range * range)))
		{
			castASpell(dist, false);
			
			// Check if the L2SiegeGuardInstance is attacking, knows the target and can't run
			if (!(_actor.isAttackingNow()) && (_actor.getRunSpeed() == 0) && (_actor.getKnownList().knowsObject(attackTarget)))
			{
				// Cancel the target
				_actor.getKnownList().removeKnownObject(attackTarget);
				_actor.setTarget(null);
				setIntention(AI_INTENTION_IDLE, null, null);
			}
			else
			{
				double dx = _actor.getX() - attackTarget.getX();
				double dy = _actor.getY() - attackTarget.getY();
				double dz = _actor.getZ() - attackTarget.getZ();
				double homeX = attackTarget.getX() - sGuard.getSpawn().getLocx();
				double homeY = attackTarget.getY() - sGuard.getSpawn().getLocy();
				
				// Check if the L2SiegeGuardInstance isn't too far from it's home location
				if ((((dx * dx) + (dy * dy)) > 10000) && (((homeX * homeX) + (homeY * homeY)) > 3240000) // 1800 * 1800
					&& (_actor.getKnownList().knowsObject(attackTarget)))
				{
					// Cancel the target
					_actor.getKnownList().removeKnownObject(attackTarget);
					_actor.setTarget(null);
					setIntention(AI_INTENTION_IDLE, null, null);
				}
				else
				// Move the actor to Pawn server side AND client side by sending Server->Client packet MoveToPawn (broadcast)
				{
					// Temporary hack for preventing guards jumping off towers,
					// before replacing this with effective geodata checks and AI modification
					if ((dz * dz) < (170 * 170)) // normally 130 if guard z coordinates correct
					{
						if (_selfAnalysis.isHealer)
						{
							return;
						}
						
						if (_selfAnalysis.isMage)
						{
							range = _selfAnalysis.maxCastRange - 50;
						}
						
						if (attackTarget.isMoving())
						{
							moveToPawn(attackTarget, range - 70);
						}
						else
						{
							moveToPawn(attackTarget, range);
						}
					}
				}
			}
			return;
		}
		
		// If the actor is muted and far from target, just "move to pawn"
		if (_actor.isMuted() && (dist > (range * range)) && !_selfAnalysis.isHealer)
		{
			// Temporary hack for preventing guards jumping off towers,
			// before replacing this with effective geodata checks and AI modification
			double dz = _actor.getZ() - attackTarget.getZ();
			if ((dz * dz) < (170 * 170)) // normally 130 if guard z coordinates correct
			{
				if (_selfAnalysis.isMage)
				{
					range = _selfAnalysis.maxCastRange - 50;
				}
				
				if (attackTarget.isMoving())
				{
					moveToPawn(attackTarget, range - 70);
				}
				else
				{
					moveToPawn(attackTarget, range);
				}
			}
			return;
		}
		
		// If this is close enough to attack
		if (dist <= (range * range))
		{
			// Force mobs to attack anybody if confused
			final L2Character hated = (_actor.isConfused()) ? attackTarget : ((L2Attackable) _actor).getMostHated();
			if (hated == null)
			{
				setIntention(AI_INTENTION_ACTIVE, null, null);
				return;
			}
			
			if (hated != attackTarget)
			{
				attackTarget = hated;
			}
			
			_attackTimeout = MAX_ATTACK_TIMEOUT + GameTimeController.getInstance().getGameTicks();
			
			// check for close combat skills && heal/buff skills
			if (!_actor.isMuted() && (Rnd.get(100) <= 5))
			{
				castASpell(dist, true);
			}
			
			// Finally, do the physical attack itself
			if (!_selfAnalysis.isHealer)
			{
				_actor.doAttack(attackTarget);
			}
		}
	}
	
	private void castASpell(double dist, boolean lowDistance)
	{
		if (_actor == null)
		{
			return;
		}
		
		final L2Skill[] skills = _actor.getAllSkills();
		for (L2Skill sk : skills)
		{
			if (!sk.isPassive() && !_actor.isSkillDisabled(sk) && (_actor.getCurrentMp() >= _actor.getStat().getMpConsume(sk)))
			{
				final int castRange = sk.getCastRange();
				if (((dist <= (castRange * castRange)) && (castRange > 70)) || (lowDistance && ((castRange * castRange) >= dist)))
				{
					L2Object oldTarget = _actor.getTarget();
					if ((sk.getSkillType() == SkillType.BUFF) || (sk.getSkillType() == SkillType.HEAL))
					{
						boolean useSkillSelf = true;
						if ((sk.getSkillType() == SkillType.HEAL) && (_actor.getCurrentHp() > (int) (_actor.getMaxHp() / 1.5)))
						{
							useSkillSelf = false;
							break;
						}
						
						if (sk.getSkillType() == SkillType.BUFF)
						{
							if (_actor.getFirstEffect(sk) != null)
							{
								useSkillSelf = false;
							}
						}
						
						if (useSkillSelf)
						{
							_actor.setTarget(_actor);
						}
					}
					
					clientStopMoving(null);
					_actor.doCast(sk);
					_actor.setTarget(oldTarget);
					return;
				}
			}
		}
	}
	
	/**
	 * Manage AI thinking actions of a L2Attackable.
	 */
	@Override
	protected void onEvtThink()
	{
		// Check if the thinking action is already in progress
		if (_thinking || _actor.isCastingNow() || _actor.isAllSkillsDisabled())
		{
			return;
		}
		
		// Start thinking action
		_thinking = true;
		
		try
		{
			// Manage AI thinks of a L2Attackable
			if (getIntention() == AI_INTENTION_ACTIVE)
			{
				thinkActive();
			}
			else if (getIntention() == AI_INTENTION_ATTACK)
			{
				thinkAttack();
			}
		}
		finally
		{
			// Stop thinking action
			_thinking = false;
		}
	}
	
	/**
	 * Launch actions corresponding to the Event Attacked.
	 * <ul>
	 * <li>Init the attack : Calculate the attack timeout, Set the _globalAggro to 0, Add the attacker to the actor _aggroList</li>
	 * <li>Set the L2Character movement type to run and send Server->Client packet ChangeMoveType to all others L2PcInstance</li>
	 * <li>Set the Intention to AI_INTENTION_ATTACK</li>
	 * </ul>
	 * @param attacker The L2Character that attacks the actor
	 */
	@Override
	protected void onEvtAttacked(L2Character attacker)
	{
		// Calculate the attack timeout
		_attackTimeout = MAX_ATTACK_TIMEOUT + GameTimeController.getInstance().getGameTicks();
		
		// Set the _globalAggro to 0 to permit attack even just after spawn
		if (_globalAggro < 0)
		{
			_globalAggro = 0;
		}
		
		// Add the attacker to the _aggroList of the actor
		((L2Attackable) _actor).addDamageHate(attacker, 0, 1);
		
		// Set the L2Character movement type to run and send Server->Client packet ChangeMoveType to all others L2PcInstance
		if (!_actor.isRunning())
		{
			_actor.setRunning();
		}
		
		// Set the Intention to AI_INTENTION_ATTACK
		if (getIntention() != AI_INTENTION_ATTACK)
		{
			setIntention(CtrlIntention.AI_INTENTION_ATTACK, attacker, null);
		}
		
		super.onEvtAttacked(attacker);
	}
	
	/**
	 * Launch actions corresponding to the Event Aggression.
	 * <ul>
	 * <li>Add the target to the actor _aggroList or update hate if already present</li>
	 * <li>Set the actor Intention to AI_INTENTION_ATTACK (if actor is L2GuardInstance check if it isn't too far from its home location)</li>
	 * </ul>
	 * @param target The L2Character that attacks
	 * @param aggro The value of hate to add to the actor against the target
	 */
	@Override
	protected void onEvtAggression(L2Character target, int aggro)
	{
		if (_actor == null)
		{
			return;
		}
		
		final L2Attackable me = (L2Attackable) _actor;
		
		if (target != null)
		{
			// Add the target to the actor _aggroList or update hate if already present
			me.addDamageHate(target, 0, aggro);
			
			// Get the hate of the actor against the target
			aggro = me.getHating(target);
			
			if (aggro <= 0)
			{
				if (me.getMostHated() == null)
				{
					_globalAggro = -25;
					me.clearAggroList();
					setIntention(AI_INTENTION_IDLE, null, null);
				}
				return;
			}
			
			// Set the actor AI Intention to AI_INTENTION_ATTACK
			if (getIntention() != CtrlIntention.AI_INTENTION_ATTACK)
			{
				// Set the L2Character movement type to run and send Server->Client packet ChangeMoveType to all others L2PcInstance
				if (!_actor.isRunning())
				{
					_actor.setRunning();
				}
				
				L2SiegeGuardInstance sGuard = (L2SiegeGuardInstance) _actor;
				double homeX = target.getX() - sGuard.getSpawn().getLocx();
				double homeY = target.getY() - sGuard.getSpawn().getLocy();
				
				// Check if the L2SiegeGuardInstance is not too far from its home location
				if (((homeX * homeX) + (homeY * homeY)) < 3240000)
				{
					setIntention(CtrlIntention.AI_INTENTION_ATTACK, target, null);
				}
			}
		}
		else
		{
			// currently only for setting lower general aggro
			if (aggro >= 0)
			{
				return;
			}
			
			final L2Character mostHated = me.getMostHated();
			if (mostHated == null)
			{
				_globalAggro = -25;
				return;
			}
			
			for (L2Character aggroed : me.getAggroList().keySet())
			{
				me.addDamageHate(aggroed, 0, aggro);
			}
			
			aggro = me.getHating(mostHated);
			if (aggro <= 0)
			{
				_globalAggro = -25;
				me.clearAggroList();
				setIntention(AI_INTENTION_IDLE, null, null);
			}
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
		_actor.detachAI();
		super.stopAITask();
	}
}
