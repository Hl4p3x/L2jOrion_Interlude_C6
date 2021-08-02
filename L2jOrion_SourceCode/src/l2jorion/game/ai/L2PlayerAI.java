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
import static l2jorion.game.ai.CtrlIntention.AI_INTENTION_IDLE;
import static l2jorion.game.ai.CtrlIntention.AI_INTENTION_INTERACT;
import static l2jorion.game.ai.CtrlIntention.AI_INTENTION_PICK_UP;
import static l2jorion.game.ai.CtrlIntention.AI_INTENTION_REST;

import java.util.EmptyStackException;
import java.util.Stack;

import l2jorion.Config;
import l2jorion.game.model.L2Character;
import l2jorion.game.model.L2Object;
import l2jorion.game.model.Location;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.actor.instance.L2StaticObjectInstance;

public class L2PlayerAI extends L2CharacterAI
{
	private boolean _thinking;
	
	class IntentionCommand
	{
		protected CtrlIntention _crtlIntention;
		protected Object _arg0, _arg1;
		
		protected IntentionCommand(final CtrlIntention pIntention, final Object pArg0, final Object pArg1)
		{
			_crtlIntention = pIntention;
			_arg0 = pArg0;
			_arg1 = pArg1;
		}
	}
	
	private final Stack<IntentionCommand> _interuptedIntentions = new Stack<>();
	
	private synchronized Stack<IntentionCommand> getInterruptedIntentions()
	{
		return _interuptedIntentions;
	}
	
	public L2PlayerAI(L2PcInstance creature)
	{
		super(creature);
	}
	
	@Override
	public synchronized void changeIntention(final CtrlIntention intention, final Object arg0, final Object arg1)
	{
		if (intention != AI_INTENTION_CAST)
		{
			super.changeIntention(intention, arg0, arg1);
			return;
		}
		
		final CtrlIntention _intention = getIntention();
		final Object _intentionArg0 = get_intentionArg0();
		final Object _intentionArg1 = get_intentionArg1();
		
		// do nothing if next intention is same as current one.
		if (intention == _intention && arg0 == _intentionArg0 && arg1 == _intentionArg1)
		{
			super.changeIntention(intention, arg0, arg1);
			return;
		}
		
		// push current intention to stack
		getInterruptedIntentions().push(new IntentionCommand(_intention, _intentionArg0, _intentionArg1));
		
		super.changeIntention(intention, arg0, arg1);
	}
	
	@Override
	protected void onEvtFinishCasting()
	{
		// forget interupted actions after offensive skill
		if (_skill != null && _skill.isOffensive())
		{
			getInterruptedIntentions().clear();
		}
		
		if (getIntention() == AI_INTENTION_CAST)
		{
			if (!getInterruptedIntentions().isEmpty())
			{
				IntentionCommand cmd = null;
				try
				{
					cmd = getInterruptedIntentions().pop();
				}
				catch (final EmptyStackException ese)
				{
					if (Config.ENABLE_ALL_EXCEPTIONS)
					{
						ese.printStackTrace();
					}
				}
				
				if (cmd != null && cmd._crtlIntention != AI_INTENTION_CAST) // previous state shouldn't be casting
				{
					setIntention(cmd._crtlIntention, cmd._arg0, cmd._arg1);
				}
				else
				{
					setIntention(AI_INTENTION_IDLE);
				}
			}
			else
			{
				// set intention to idle if skill doesn't change intention.
				setIntention(AI_INTENTION_IDLE);
			}
		}
	}
	
	@Override
	protected void onIntentionRest()
	{
		if (getIntention() != AI_INTENTION_REST)
		{
			changeIntention(AI_INTENTION_REST, null, null);
			setTarget(null);
			
			if (getAttackTarget() != null)
			{
				setAttackTarget(null);
			}
			
			clientStopMoving(null);
		}
	}
	
	@Override
	public void clientStopMoving(Location pos)
	{
		super.clientStopMoving(pos);
		final L2PcInstance _player = (L2PcInstance) _actor;
		if (_player.getPosticipateSit())
		{
			_player.sitDown();
		}
	}
	
	@Override
	protected void onIntentionActive()
	{
		setIntention(AI_INTENTION_IDLE);
	}
	
	@Override
	protected void clientNotifyDead()
	{
		_clientMovingToPawnOffset = 0;
		_clientMoving = false;
		
		super.clientNotifyDead();
	}
	
	private void thinkAttack()
	{
		L2Character target = getAttackTarget();
		
		if (target == null)
		{
			return;
		}
		
		if (checkTargetLostOrDead(target))
		{
			setAttackTarget(null);
			return;
		}
		
		if (maybeMoveToPawn(target, _actor.getPhysicalAttackRange()))
		{
			return;
		}
		
		clientStopMoving(null);
		
		_actor.doAttack(target);
	}
	
	private void thinkCast()
	{
		final L2Character target = getCastTarget();
		
		if (checkTargetLost(target))
		{
			if (_skill.isOffensive() && getAttackTarget() != null)
			{
				setCastTarget(null);
			}
			return;
		}
		
		if (target != null)
		{
			if (maybeMoveToPawn(target, _actor.getMagicalAttackRange(_skill)))
			{
				return;
			}
		}
		
		if (_skill.getHitTime() > 50)
		{
			clientStopMoving(null);
		}
		
		final L2Object oldTarget = _actor.getTarget();
		
		if (oldTarget != null)
		{
			if (target != null && oldTarget != target)
			{
				_actor.setTarget(getCastTarget());
			}
			
			_actor.doCast(_skill);
			
			if (target != null && oldTarget != target)
			{
				_actor.setTarget(oldTarget);
			}
		}
		else
		{
			_actor.doCast(_skill);
		}
		
		return;
	}
	
	private void thinkPickUp()
	{
		if (_actor.isAllSkillsDisabled() || _actor.isCastingNow())
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
		
		_actor.getActingPlayer().doPickupItem(target);
		
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
		
		if (!(target instanceof L2StaticObjectInstance))
		{
			_actor.getActingPlayer().doInteract((L2Character) target);
		}
		
		setIntention(AI_INTENTION_IDLE);
		return;
	}
	
	@Override
	protected void onEvtThink()
	{
		if (_thinking || _actor.isAllSkillsDisabled())
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
}
