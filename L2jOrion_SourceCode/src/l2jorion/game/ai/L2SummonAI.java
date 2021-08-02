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

import static l2jorion.game.ai.CtrlIntention.AI_INTENTION_ATTACK;
import static l2jorion.game.ai.CtrlIntention.AI_INTENTION_CAST;
import static l2jorion.game.ai.CtrlIntention.AI_INTENTION_FOLLOW;
import static l2jorion.game.ai.CtrlIntention.AI_INTENTION_IDLE;
import static l2jorion.game.ai.CtrlIntention.AI_INTENTION_INTERACT;
import static l2jorion.game.ai.CtrlIntention.AI_INTENTION_PICK_UP;

import l2jorion.game.geo.GeoData;
import l2jorion.game.model.L2Character;
import l2jorion.game.model.L2Object;
import l2jorion.game.model.L2Skill;
import l2jorion.game.model.L2Summon;
import l2jorion.util.random.Rnd;

public class L2SummonAI extends L2CharacterAI
{
	private static final int AVOID_RADIUS = 70;
	
	private boolean _thinking;
	private volatile boolean _startFollow = ((L2Summon) _actor).getFollowStatus();
	private L2Character _lastAttack = null;
	
	private volatile boolean _startAvoid = false;
	
	public L2SummonAI(L2Summon creature)
	{
		super(creature);
	}
	
	@Override
	protected void onIntentionIdle()
	{
		stopFollow();
		_startFollow = false;
		onIntentionActive();
	}
	
	@Override
	protected void onIntentionActive()
	{
		L2Summon summon = (L2Summon) _actor;
		
		if (_startFollow)
		{
			setIntention(AI_INTENTION_FOLLOW, summon.getOwner());
		}
		else
		{
			super.onIntentionActive();
		}
	}
	
	private void thinkAttack()
	{
		if (checkTargetLostOrDead(getAttackTarget()))
		{
			setAttackTarget(null);
			return;
		}
		
		if (maybeMoveToPawn(getAttackTarget(), _actor.getPhysicalAttackRange()))
		{
			return;
		}
		
		clientStopMoving(null);
		
		_actor.doAttack(getAttackTarget());
		
		return;
	}
	
	private void thinkCast()
	{
		L2Summon summon = (L2Summon) _actor;
		
		final L2Character target = getCastTarget();
		if (checkTargetLost(target))
		{
			setCastTarget(null);
			return;
		}
		
		boolean val = _startFollow;
		if (maybeMoveToPawn(target, _actor.getMagicalAttackRange(_skill)))
		{
			return;
		}
		
		clientStopMoving(null);
		setIntention(AI_INTENTION_IDLE);
		summon.setFollowStatus(false);
		_startFollow = val;
		_actor.doCast(_skill);
	}
	
	private void thinkPickUp()
	{
		if (_actor.isAllSkillsDisabled())
		{
			return;
		}
		
		final L2Object target = getTarget();
		
		if (checkTargetLost(target))
		{
			return;
		}
		
		if (maybeMoveToPawn(target, 36))
		{
			return;
		}
		
		setIntention(AI_INTENTION_IDLE);
		
		((L2Summon) _actor).doPickupItem(target);
		
		return;
	}
	
	private void thinkInteract()
	{
		if (_actor.isAllSkillsDisabled())
		{
			return;
		}
		
		final L2Object target = getTarget();
		
		if (checkTargetLost(target))
		{
			return;
		}
		
		if (maybeMoveToPawn(target, 36))
		{
			return;
		}
		
		setIntention(AI_INTENTION_IDLE);
		
		return;
	}
	
	@Override
	protected void onEvtThink()
	{
		if (_thinking || _actor.isCastingNow() || _actor.isAllSkillsDisabled())
		{
			return;
		}
		
		_thinking = true;
		
		try
		{
			if (getIntention() == AI_INTENTION_ATTACK)
			{
				thinkAttack();
			}
			else if (getIntention() == AI_INTENTION_CAST)
			{
				thinkCast();
			}
			else if (getIntention() == AI_INTENTION_PICK_UP)
			{
				thinkPickUp();
			}
			else if (getIntention() == AI_INTENTION_INTERACT)
			{
				thinkInteract();
			}
		}
		finally
		{
			_thinking = false;
		}
	}
	
	@Override
	protected void onEvtFinishCasting()
	{
		if (_lastAttack == null)
		{
			((L2Summon) _actor).setFollowStatus(_startFollow);
		}
		else
		{
			setIntention(CtrlIntention.AI_INTENTION_ATTACK, _lastAttack);
			_lastAttack = null;
		}
	}
	
	@Override
	protected void onEvtAttacked(L2Character attacker)
	{
		super.onEvtAttacked(attacker);
		
		avoidAttack(attacker);
	}
	
	private void avoidAttack(L2Character attacker)
	{
		_startAvoid = true;
		
		if (!checkCondition())
		{
			_startAvoid = false;
		}
		
		if (_startAvoid)
		{
			final int ownerX = ((L2Summon) _actor).getOwner().getX();
			final int ownerY = ((L2Summon) _actor).getOwner().getY();
			final double angle = Math.toRadians(Rnd.get(-90, 90)) + Math.atan2(ownerY - _actor.getY(), ownerX - _actor.getX());
			
			final int targetX = ownerX + (int) (AVOID_RADIUS * Math.cos(angle));
			final int targetY = ownerY + (int) (AVOID_RADIUS * Math.sin(angle));
			if (GeoData.getInstance().canMove(_actor.getX(), _actor.getY(), _actor.getZ(), targetX, targetY, _actor.getZ(), _actor.getInstanceId()))
			{
				moveTo(targetX, targetY, _actor.getZ());
			}
		}
	}
	
	private boolean checkCondition()
	{
		if (_clientMoving || _actor.isDead() || _actor.isMovementDisabled() || _actor.isCastingNow() || _actor.isAttackingNow())
		{
			return false;
		}
		
		return true;
	}
	
	@Override
	protected void onIntentionCast(L2Skill skill, L2Object target)
	{
		if (getIntention() == AI_INTENTION_ATTACK)
		{
			_lastAttack = getAttackTarget();
		}
		else
		{
			_lastAttack = null;
		}
		
		super.onIntentionCast(skill, target);
	}
	
	public void notifyFollowStatusChange()
	{
		_startFollow = !_startFollow;
		switch (getIntention())
		{
			case AI_INTENTION_ACTIVE:
			case AI_INTENTION_FOLLOW:
			case AI_INTENTION_IDLE:
			case AI_INTENTION_MOVE_TO:
			case AI_INTENTION_PICK_UP:
				((L2Summon) _actor).setFollowStatus(_startFollow);
		}
	}
	
	public void setStartFollowController(boolean val)
	{
		_startFollow = val;
	}
}