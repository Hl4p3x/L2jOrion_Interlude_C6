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
import static l2jorion.game.ai.CtrlIntention.AI_INTENTION_CAST;
import static l2jorion.game.ai.CtrlIntention.AI_INTENTION_FOLLOW;
import static l2jorion.game.ai.CtrlIntention.AI_INTENTION_IDLE;
import static l2jorion.game.ai.CtrlIntention.AI_INTENTION_INTERACT;
import static l2jorion.game.ai.CtrlIntention.AI_INTENTION_MOVE_TO;
import static l2jorion.game.ai.CtrlIntention.AI_INTENTION_PICK_UP;
import static l2jorion.game.ai.CtrlIntention.AI_INTENTION_REST;

import java.util.List;

import javolution.util.FastList;
import l2jorion.game.ai.L2PlayerAI.IntentionCommand;
import l2jorion.game.geo.GeoData;
import l2jorion.game.model.Inventory;
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
import l2jorion.game.model.entity.Announcements;
import l2jorion.game.model.zone.ZoneId;
import l2jorion.game.network.serverpackets.AutoAttackStop;
import l2jorion.game.taskmanager.AttackStanceTaskManager;
import l2jorion.game.templates.L2Weapon;
import l2jorion.game.templates.L2WeaponType;
import l2jorion.game.util.Util;
import l2jorion.util.random.Rnd;

public class L2CharacterAI extends AbstractAI
{
	@Override
	protected void onEvtAttacked(L2Character attacker)
	{
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
		
		setCastTarget(null);
		setAttackTarget(null);
		
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
			
			setCastTarget(null);
			setAttackTarget(null);
			
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
		
		if (_actor instanceof L2PcInstance && _actor.getTarget() != null && _actor.getTarget() != target)
		{
			target = (L2Character) _actor.getTarget();
		}
		
		if (_actor.isAllSkillsDisabled() || _actor.isCastingNow() || _actor.isAfraid())
		{
			clientActionFailed();
			return;
		}
		
		if (getIntention() == AI_INTENTION_ATTACK)
		{
			if (getAttackTarget() != target)
			{
				setAttackTarget(target);
				
				stopFollow();
				
				notifyEvent(CtrlEvent.EVT_THINK, null);
			}
			else
			{
				clientActionFailed();
			}
		}
		else
		{
			changeIntention(AI_INTENTION_ATTACK, target, null);
			
			setAttackTarget(target);
			
			stopFollow();
			
			notifyEvent(CtrlEvent.EVT_THINK, null);
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
		
		setCastTarget((L2Character) target);
		
		if (skill.getHitTime() > 50)
		{
			_actor.abortAttack();
		}
		
		_skill = skill;
		
		changeIntention(AI_INTENTION_CAST, skill, target);
		
		notifyEvent(CtrlEvent.EVT_THINK, null);
	}
	
	protected void changeIntentionToCast(L2Skill skill, L2Object target)
	{
		if ((getIntention() == AI_INTENTION_REST) && skill.isMagic())
		{
			clientActionFailed();
			return;
		}
		
		setCastTarget((L2Character) target);
		
		_skill = skill;
		
		changeIntention(AI_INTENTION_CAST, skill, target);
		
		notifyEvent(CtrlEvent.EVT_THINK, null);
	}
	
	@Override
	protected void onIntentionMoveTo(Location pos)
	{
		if (getIntention() == AI_INTENTION_REST)
		{
			clientActionFailed();
			return;
		}
		
		if (_actor instanceof L2PcInstance && (_actor.isAttackingNow() || _actor.isCastingNow()) && !_actor.isMoving())
		{
			final L2PcInstance player = (L2PcInstance) _actor;
			if (!player.isMovingTaskDefined())
			{
				player.defineNewMovingTask(pos);
			}
			else
			{
				player.modifyMovingTask(pos);
			}
			
			clientActionFailed();
			return;
		}
		
		changeIntention(AI_INTENTION_MOVE_TO, pos, null);
		
		clientStopAutoAttack();
		
		if (_actor instanceof L2PcInstance)
		{
			final L2ItemInstance rhand = ((L2PcInstance) _actor).getInventory().getPaperdollItem(Inventory.PAPERDOLL_RHAND);
			if ((rhand != null && rhand.getItemType() == L2WeaponType.BOW))
			{
				if (!_actor.isAttackingNow())
				{
					_actor.abortAttack();
				}
			}
			else
			{
				_actor.abortAttack();
			}
		}
		else
		{
			_actor.abortAttack();
		}
		
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
			
			moveToPawn(object, 40);
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
	
	/**
	 * Launch actions corresponding to the Event Sleeping.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Stop the actor auto-attack client side by sending Server->Client packet AutoAttackStop (broadcast)</li>
	 * <li>Stop the actor movement server side AND client side by sending Server->Client packet StopMove/StopRotation (broadcast)</li>
	 * <li>Break an attack and send Server->Client ActionFailed packet and a System Message to the L2Character</li>
	 * <li>Break a cast and send Server->Client ActionFailed packet and a System Message to the L2Character</li><BR>
	 * <BR>
	 */
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
	
	/**
	 * Launch actions corresponding to the Event Rooted.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Stop the actor movement server side AND client side by sending Server->Client packet StopMove/StopRotation (broadcast)</li>
	 * <li>Launch actions corresponding to the Event onAttacked</li><BR>
	 * <BR>
	 */
	@Override
	protected void onEvtRooted(final L2Character attacker)
	{
		clientStopMoving(null);
		
		onEvtAttacked(attacker);
	}
	
	/**
	 * Launch actions corresponding to the Event Confused.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Stop the actor movement server side AND client side by sending Server->Client packet StopMove/StopRotation (broadcast)</li>
	 * <li>Launch actions corresponding to the Event onAttacked</li><BR>
	 * <BR>
	 */
	@Override
	protected void onEvtConfused(final L2Character attacker)
	{
		clientStopMoving(null);
		
		onEvtAttacked(attacker);
	}
	
	/**
	 * Launch actions corresponding to the Event Muted.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Break a cast and send Server->Client ActionFailed packet and a System Message to the L2Character</li><BR>
	 * <BR>
	 */
	@Override
	protected void onEvtMuted(final L2Character attacker)
	{
		onEvtAttacked(attacker);
	}
	
	/**
	 * Launch actions corresponding to the Event ReadyToAct.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Launch actions corresponding to the Event Think</li><BR>
	 * <BR>
	 */
	@Override
	protected void onEvtReadyToAct()
	{
		// Launch actions corresponding to the Event Think
		onEvtThink();
	}
	
	/**
	 * Do nothing.<BR>
	 * <BR>
	 */
	@Override
	protected void onEvtUserCmd(final Object arg0, final Object arg1)
	{
		// do nothing
	}
	
	/**
	 * Launch actions corresponding to the Event Arrived.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>If the Intention was AI_INTENTION_MOVE_TO, set the Intention to AI_INTENTION_ACTIVE</li>
	 * <li>Launch actions corresponding to the Event Think</li><BR>
	 * <BR>
	 */
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
	
	/**
	 * Launch actions corresponding to the Event ArrivedRevalidate.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Launch actions corresponding to the Event Think</li><BR>
	 * <BR>
	 */
	@Override
	protected void onEvtArrivedRevalidate()
	{
		// Launch actions corresponding to the Event Think
		onEvtThink();
	}
	
	/**
	 * Launch actions corresponding to the Event ArrivedBlocked.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Stop the actor movement server side AND client side by sending Server->Client packet StopMove/StopRotation (broadcast)</li>
	 * <li>If the Intention was AI_INTENTION_MOVE_TO, set the Intention to AI_INTENTION_ACTIVE</li>
	 * <li>Launch actions corresponding to the Event Think</li><BR>
	 * <BR>
	 */
	@Override
	protected void onEvtArrivedBlocked(final Location blocked_at_pos)
	{
		// If the Intention was AI_INTENTION_MOVE_TO, set the Intention to AI_INTENTION_ACTIVE
		if ((getIntention() == AI_INTENTION_MOVE_TO) || (getIntention() == AI_INTENTION_CAST))
		{
			setIntention(AI_INTENTION_ACTIVE);
		}
		
		// Stop the actor movement server side AND client side by sending Server->Client packet StopMove/StopRotation (broadcast)
		clientStopMoving(blocked_at_pos);
		
		// Launch actions corresponding to the Event Think
		onEvtThink();
	}
	
	/**
	 * Launch actions corresponding to the Event ForgetObject.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>If the object was targeted and the Intention was AI_INTENTION_INTERACT or AI_INTENTION_PICK_UP, set the Intention to AI_INTENTION_ACTIVE</li>
	 * <li>If the object was targeted to attack, stop the auto-attack, cancel target and set the Intention to AI_INTENTION_ACTIVE</li>
	 * <li>If the object was targeted to cast, cancel target and set the Intention to AI_INTENTION_ACTIVE</li>
	 * <li>If the object was targeted to follow, stop the movement, cancel AI Follow Task and set the Intention to AI_INTENTION_ACTIVE</li>
	 * <li>If the targeted object was the actor , cancel AI target, stop AI Follow Task, stop the movement and set the Intention to AI_INTENTION_IDLE</li><BR>
	 * <BR>
	 */
	@Override
	protected void onEvtForgetObject(final L2Object object)
	{
		// If the object was targeted and the Intention was AI_INTENTION_INTERACT or AI_INTENTION_PICK_UP, set the Intention to AI_INTENTION_ACTIVE
		if (getTarget() == object)
		{
			setTarget(null);
			
			if (getIntention() == AI_INTENTION_INTERACT)
			{
				setIntention(AI_INTENTION_ACTIVE);
			}
			else if (getIntention() == AI_INTENTION_PICK_UP)
			{
				setIntention(AI_INTENTION_ACTIVE);
			}
		}
		
		// Check if the object was targeted to attack
		if (getAttackTarget() == object)
		{
			// Cancel attack target
			setAttackTarget(null);
			
			// Set the Intention of this AbstractAI to AI_INTENTION_ACTIVE
			setIntention(AI_INTENTION_ACTIVE);
		}
		
		// Check if the object was targeted to cast
		if (getCastTarget() == object)
		{
			// Cancel cast target
			setCastTarget(null);
			
			// Set the Intention of this AbstractAI to AI_INTENTION_ACTIVE
			setIntention(AI_INTENTION_ACTIVE);
		}
		
		// Check if the object was targeted to follow
		if (getFollowTarget() == object)
		{
			// Stop the actor movement server side AND client side by sending Server->Client packet StopMove/StopRotation (broadcast)
			clientStopMoving(null);
			
			// Stop an AI Follow Task
			stopFollow();
			
			// Set the Intention of this AbstractAI to AI_INTENTION_ACTIVE
			setIntention(AI_INTENTION_ACTIVE);
		}
		
		// Check if the targeted object was the actor
		if (_actor == object)
		{
			// Cancel AI target
			setTarget(null);
			setAttackTarget(null);
			setCastTarget(null);
			
			// Stop an AI Follow Task
			stopFollow();
			
			// Stop the actor movement server side AND client side by sending Server->Client packet StopMove/StopRotation (broadcast)
			clientStopMoving(null);
			
			// Set the Intention of this AbstractAI to AI_INTENTION_IDLE
			changeIntention(AI_INTENTION_IDLE, null, null);
		}
	}
	
	/**
	 * Launch actions corresponding to the Event Cancel.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Stop an AI Follow Task</li>
	 * <li>Launch actions corresponding to the Event Think</li><BR>
	 * <BR>
	 */
	@Override
	protected void onEvtCancel()
	{
		// Stop an AI Follow Task
		stopFollow();
		
		if (!AttackStanceTaskManager.getInstance().getAttackStanceTask(_actor))
		{
			_actor.broadcastPacket(new AutoAttackStop(_actor.getObjectId()));
		}
		
		// Launch actions corresponding to the Event Think
		onEvtThink();
	}
	
	/**
	 * Launch actions corresponding to the Event Dead.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Stop an AI Follow Task</li>
	 * <li>Kill the actor client side by sending Server->Client packet AutoAttackStop, StopMove/StopRotation, Die (broadcast)</li><BR>
	 * <BR>
	 */
	@Override
	protected void onEvtDead()
	{
		// Stop an AI Follow Task
		stopAITask();
		
		// Kill the actor client side by sending Server->Client packet AutoAttackStop, StopMove/StopRotation, Die (broadcast)
		clientNotifyDead();
		
		if (!(_actor instanceof L2PcInstance))
		{
			_actor.setWalking();
		}
	}
	
	/**
	 * Launch actions corresponding to the Event Fake Death.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Stop an AI Follow Task</li>
	 */
	@Override
	protected void onEvtFakeDeath()
	{
		// Stop an AI Follow Task
		stopFollow();
		
		// Stop the actor movement and send Server->Client packet StopMove/StopRotation (broadcast)
		clientStopMoving(null);
		
		// Init AI
		setIntention(AI_INTENTION_IDLE);
		setTarget(null);
		setCastTarget(null);
		setAttackTarget(null);
	}
	
	/**
	 * Do nothing.<BR>
	 * <BR>
	 */
	@Override
	protected void onEvtFinishCasting()
	{
		// do nothing
	}
	
	/**
	 * Manage the Move to Pawn action in function of the distance and of the Interact area.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Get the distance between the current position of the L2Character and the target (x,y)</li>
	 * <li>If the distance > offset+20, move the actor (by running) to Pawn server side AND client side by sending Server->Client packet MoveToPawn (broadcast)</li>
	 * <li>If the distance <= offset+20, Stop the actor movement server side AND client side by sending Server->Client packet StopMove/StopRotation (broadcast)</li><BR>
	 * <BR>
	 * <B><U> Example of use </U> :</B><BR>
	 * <BR>
	 * <li>L2PLayerAI, L2SummonAI</li><BR>
	 * <BR>
	 * @param target The targeted L2Object
	 * @param offset The Interact area radius
	 * @return True if a movement must be done
	 */
	protected boolean maybeMoveToPawn(L2Object target, int offset)
	{
		// Get the distance between the current position of the L2Character and the target (x,y)
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
			// needToMove = !_actor.isInsideRadius(target, offset, false, false);
			needToMove = !Util.checkIfInRange(offset, _actor, target, false);
		}
		
		if (needToMove)
		{
			if (getFollowTarget() != null)
			{
				// int foffset = offset + (((L2Character) target).isMoving() ? 100 : 0);
				if (!_actor.isInsideRadius(target, offset, true, false))
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
					if (_actor instanceof L2PcInstance && _actor.charIsGM())
					{
						Announcements GM = Announcements.getInstance();
						GM.sys("You can't move.");
					}
					
					_actor.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
				}
				
				return true;
			}
			
			// If not running, set the L2Character movement type to run and send Server->Client packet ChangeMoveType to all others L2PcInstance
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
	
	/**
	 * Modify current Intention and actions if the target is lost or dead.<BR>
	 * <BR>
	 * <B><U> Actions</U> : <I>If the target is lost or dead</I></B><BR>
	 * <BR>
	 * <li>Stop the actor auto-attack client side by sending Server->Client packet AutoAttackStop (broadcast)</li>
	 * <li><BR>
	 * Stop the actor movement server side AND client side by sending Server->Client packet StopMove/StopRotation<BR>
	 * (broadcast)</li>
	 * <li>Set the Intention of this AbstractAI to AI_INTENTION_ACTIVE</li><BR>
	 * <BR>
	 * <B><U> Example of use </U> :</B><BR>
	 * <BR>
	 * <li>L2PLayerAI, L2SummonAI</li><BR>
	 * <BR>
	 * @param target The targeted L2Object
	 * @return True if the target is lost or dead (false if fakedeath)
	 */
	protected boolean checkTargetLostOrDead(final L2Character target)
	{
		if (target == null || target.isAlikeDead())
		{
			// check if player is fakedeath
			if (target != null && target.isFakeDeath())
			{
				// target.stopFakeDeath(null);
				return false;
			}
			
			// Set the Intention of this AbstractAI to AI_INTENTION_ACTIVE
			setIntention(AI_INTENTION_ACTIVE);
			
			return true;
		}
		return false;
	}
	
	/**
	 * Modify current Intention and actions if the target is lost.<BR>
	 * <BR>
	 * <B><U> Actions</U> : <I>If the target is lost</I></B><BR>
	 * <BR>
	 * <li>Stop the actor auto-attack client side by sending Server->Client packet AutoAttackStop (broadcast)</li>
	 * <li>Stop the actor movement server side AND client side by sending Server->Client packet StopMove/StopRotation (broadcast)</li>
	 * <li>Set the Intention of this AbstractAI to AI_INTENTION_ACTIVE</li><BR>
	 * <BR>
	 * <B><U> Example of use </U> :</B><BR>
	 * <BR>
	 * <li>L2PLayerAI, L2SummonAI</li><BR>
	 * <BR>
	 * @param target The targeted L2Object
	 * @return True if the target is lost
	 */
	protected boolean checkTargetLost(final L2Object target)
	{
		// check if player is fakedeath
		if (target instanceof L2PcInstance)
		{
			L2PcInstance target2 = (L2PcInstance) target; // convert object to chara
			
			if (target2.isFakeDeath())
			{
				target2.stopFakeDeath(null);
				return false;
			}
		}
		
		if (target == null)
		{
			// Set the Intention of this AbstractAI to AI_INTENTION_ACTIVE
			setIntention(AI_INTENTION_ACTIVE);
			return true;
		}
		
		return false;
	}
	
	/**
	 * @see l2jorion.game.ai.AbstractAI#onIntentionActive()
	 */
	@Override
	protected void onIntentionActive()
	{
		// Check if the Intention is not already Active
		if (getIntention() != AI_INTENTION_ACTIVE)
		{
			// Set the AI Intention to AI_INTENTION_ACTIVE
			changeIntention(AI_INTENTION_ACTIVE, null, null);
			
			// Init cast and attack target
			setCastTarget(null);
			setAttackTarget(null);
			
			// Stop the actor movement server side AND client side by sending Server->Client packet StopMove/StopRotation (broadcast)
			clientStopMoving(null);
			
			// Stop the actor auto-attack client side by sending Server->Client packet AutoAttackStop (broadcast)
			clientStopAutoAttack();
			
			// Also enable random animations for this L2Character if allowed
			// This is only for mobs - town npcs are handled in their constructor
			if (_actor instanceof L2Attackable)
			{
				((L2NpcInstance) _actor).startRandomAnimationTimer();
			}
			
			// Launch the Think Event
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
		
		public List<L2Skill> generalSkills = new FastList<>();
		public List<L2Skill> buffSkills = new FastList<>();
		public List<L2Skill> debuffSkills = new FastList<>();
		public List<L2Skill> cancelSkills = new FastList<>();
		public List<L2Skill> healSkills = new FastList<>();
		public List<L2Skill> generalDisablers = new FastList<>();
		public List<L2Skill> sleepSkills = new FastList<>();
		public List<L2Skill> rootSkills = new FastList<>();
		public List<L2Skill> muteSkills = new FastList<>();
		public List<L2Skill> resurrectSkills = new FastList<>();
		
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
						continue; // won't be considered something for fighting
					case BUFF:
						buffSkills.add(sk);
						continue; // won't be considered something for fighting
					case PARALYZE:
					case STUN:
						// hardcoding petrification until improvements are made to
						// EffectTemplate... petrification is totally different for
						// AI than paralyze
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
						continue; // won't be considered something for fighting
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
	
	protected class TargetAnalysis
	{
		public L2Character character;
		public boolean isMage;
		public boolean isBalanced;
		public boolean isArcher;
		public boolean isFighter;
		public boolean isCanceled;
		public boolean isSlower;
		public boolean isMagicResistant;
		
		public TargetAnalysis()
		{
		}
		
		public void update(L2Character target)
		{
			// update status once in 4 seconds
			if ((target == character) && (Rnd.get(100) > 25))
			{
				return;
			}
			character = target;
			if (target == null)
			{
				return;
			}
			isMage = false;
			isBalanced = false;
			isArcher = false;
			isFighter = false;
			isCanceled = false;
			
			if (target.getMAtk(null, null) > (1.5 * target.getPAtk(null)))
			{
				isMage = true;
			}
			else if (((target.getPAtk(null) * 0.8) < target.getMAtk(null, null)) || ((target.getMAtk(null, null) * 0.8) > target.getPAtk(null)))
			{
				isBalanced = true;
			}
			else
			{
				L2Weapon weapon = target.getActiveWeaponItem();
				if ((weapon != null) && (weapon.getItemType() == L2WeaponType.BOW))
				{
					isArcher = true;
				}
				else
				{
					isFighter = true;
				}
			}
			
			if (target.getRunSpeed() < (_actor.getRunSpeed() - 3))
			{
				isSlower = true;
			}
			else
			{
				isSlower = false;
			}
			
			if ((target.getMDef(null, null) * 1.2) > _actor.getMAtk(null, null))
			{
				isMagicResistant = true;
			}
			else
			{
				isMagicResistant = false;
			}
			
			if (target.getBuffCount() < 4)
			{
				isCanceled = true;
			}
		}
	}
	
	public boolean canAura(L2Skill sk)
	{
		if ((sk.getTargetType() == L2Skill.SkillTargetType.TARGET_AURA) || (sk.getTargetType() == L2Skill.SkillTargetType.TARGET_BEHIND_AURA) || (sk.getTargetType() == L2Skill.SkillTargetType.TARGET_FRONT_AURA))
		{
			for (L2Object target : _actor.getKnownList().getKnownTypeInRadius(L2Character.class, sk.getSkillRadius()))
			{
				if (target == getAttackTarget())
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
						L2NpcInstance targets = ((L2NpcInstance) target);
						L2NpcInstance actors = ((L2NpcInstance) _actor);
						
						if ((targets.getEnemyClan() == null) || (actors.getFactionId() == null) || !targets.getEnemyClan().equals(actors.getFactionId()) || ((actors.getFactionId() == null) && (actors.getIsChaos() == 0)))
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
				for (L2Character target : getAttackTarget().getKnownList().getKnownTypeInRadius(L2Character.class, sk.getSkillRadius()))
				{
					if (!GeoData.getInstance().canSeeTarget(_actor, target) || (target == null))
					{
						continue;
					}
					if (target instanceof L2Attackable)
					{
						L2NpcInstance targets = ((L2NpcInstance) target);
						L2NpcInstance actors = ((L2NpcInstance) _actor);
						if ((targets.getEnemyClan() == null) || (actors.getFactionId() == null) || !targets.getEnemyClan().equals(actors.getFactionId()) || ((actors.getFactionId() == null) && (actors.getIsChaos() == 0)))
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
						L2NpcInstance targets = ((L2NpcInstance) target);
						L2NpcInstance actors = ((L2NpcInstance) _actor);
						if ((targets.getEnemyClan() == null) || (actors.getFactionId() == null) || !targets.getEnemyClan().equals(actors.getFactionId()) || ((actors.getFactionId() == null) && (actors.getIsChaos() == 0)))
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
				for (L2Character target : getAttackTarget().getKnownList().getKnownTypeInRadius(L2Character.class, sk.getSkillRadius()))
				{
					if (!GeoData.getInstance().canSeeTarget(_actor, target))
					{
						continue;
					}
					
					if (target instanceof L2Attackable)
					{
						L2NpcInstance targets = ((L2NpcInstance) target);
						L2NpcInstance actors = ((L2NpcInstance) _actor);
						if ((targets.getEnemyClan() == null) || (actors.getFactionId() == null) || !targets.getEnemyClan().equals(actors.getFactionId()) || ((actors.getFactionId() == null) && (actors.getIsChaos() == 0)))
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
	
	public static class CastTask implements Runnable
	{
		private final L2Character _activeChar;
		private final L2Object _target;
		private final L2Skill _skill;
		
		public CastTask(L2Character actor, L2Skill skill, L2Object target)
		{
			_activeChar = actor;
			_target = target;
			_skill = skill;
		}
		
		@Override
		public void run()
		{
			if (_activeChar.isAttackingNow())
			{
				_activeChar.abortAttack();
			}
			_activeChar.getAI().changeIntentionToCast(_skill, _target);
		}
	}
}
