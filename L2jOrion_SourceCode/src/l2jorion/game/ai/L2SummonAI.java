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

import l2jorion.game.model.L2Character;
import l2jorion.game.model.L2Object;
import l2jorion.game.model.L2Summon;
import l2jorion.game.model.L2Character.AIAccessor;

public class L2SummonAI extends L2CharacterAI
{
	
	private boolean _thinking; // to prevent recursive thinking
	private L2Summon summon;
	
	public L2SummonAI(final AIAccessor accessor)
	{
		super(accessor);
	}
	
	@Override
	protected void onIntentionIdle()
	{
		stopFollow();
		onIntentionActive();
	}
	
	@Override
	protected void onIntentionActive()
	{
		L2Summon summon = (L2Summon) _actor;
		
		if (summon.getFollowStatus())
		{
			setIntention(AI_INTENTION_FOLLOW, summon.getOwner());
		}
		else
		{
			super.onIntentionActive();
		}
		
		summon = null;
	}
	
	private void thinkAttack()
	{
		summon = (L2Summon) _actor;
		L2Object target = null;
		target = summon.getTarget();
		
		// Like L2OFF if the target is dead the summon must go back to his owner
		if (target != null && summon != null && ((L2Character) target).isDead())
			summon.setFollowStatus(true);
		
		if (checkTargetLostOrDead(getAttackTarget()))
		{
			setAttackTarget(null);
			return;
		}
		
		if (maybeMoveToPawn(getAttackTarget(), _actor.getPhysicalAttackRange()))
			return;
		
		clientStopMoving(null);
		_accessor.doAttack(getAttackTarget());
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
		
		if (maybeMoveToPawn(target, _actor.getMagicalAttackRange(_skill)))
			return;
		
		clientStopMoving(null);
		summon.setFollowStatus(false);
		
		setIntention(AI_INTENTION_IDLE);
		_accessor.doCast(_skill);
	}
	
	private void thinkPickUp()
	{
		if (_actor.isAllSkillsDisabled())
			return;
		
		final L2Object target = getTarget();
		
		if (checkTargetLost(target))
			return;
		
		if (maybeMoveToPawn(target, 36))
			return;
		
		setIntention(AI_INTENTION_IDLE);
		((L2Summon.AIAccessor) _accessor).doPickupItem(target);
		
		return;
	}
	
	private void thinkInteract()
	{
		if (_actor.isAllSkillsDisabled())
			return;
		
		final L2Object target = getTarget();
		
		if (checkTargetLost(target))
			return;
		
		if (maybeMoveToPawn(target, 36))
			return;
		
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
		final L2Summon summon = (L2Summon) _actor;
		L2Object target = null;
		target = summon.getTarget();
		
		if (target == null)
			return;
		
		if (summon.getAI().getIntention() != AI_INTENTION_ATTACK)
			summon.setFollowStatus(true);
		
		else if (((L2Character) target).isDead())
			summon.setFollowStatus(true);
		
		super.onEvtFinishCasting();
	}
}