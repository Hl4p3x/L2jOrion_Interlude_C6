package l2jorion.game.ai;

import static l2jorion.game.ai.CtrlIntention.AI_INTENTION_ATTACK;
import static l2jorion.game.ai.CtrlIntention.AI_INTENTION_CAST;
import static l2jorion.game.ai.CtrlIntention.AI_INTENTION_IDLE;
import static l2jorion.game.ai.CtrlIntention.AI_INTENTION_INTERACT;
import static l2jorion.game.ai.CtrlIntention.AI_INTENTION_MOVE_TO;
import static l2jorion.game.ai.CtrlIntention.AI_INTENTION_PICK_UP;
import static l2jorion.game.ai.CtrlIntention.AI_INTENTION_REST;

import l2jorion.game.model.L2Character;
import l2jorion.game.model.L2Object;
import l2jorion.game.model.L2Skill;
import l2jorion.game.model.Location;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.actor.instance.L2StaticObjectInstance;
import l2jorion.game.model.entity.Duel;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.SystemMessage;

public class L2PlayerAI extends L2CharacterAI
{
	private boolean _thinking;
	
	private IntentionCommand _nextIntention = null;
	
	void saveNextIntention(CtrlIntention intention, Object arg0, Object arg1)
	{
		_nextIntention = new IntentionCommand(intention, arg0, arg1);
	}
	
	@Override
	public IntentionCommand getNextIntention()
	{
		return _nextIntention;
	}
	
	public L2PlayerAI(L2PcInstance creature)
	{
		super(creature);
	}
	
	@Override
	protected synchronized void changeIntention(CtrlIntention intention, Object arg0, Object arg1)
	{
		// if ((intention != AI_INTENTION_CAST) || ((arg0 != null) && ((L2Skill) arg0).isOffensive())) - OLD REMOVED
		// Forget next if it's not cast or it's cast and skill is toggle.
		if ((intention != AI_INTENTION_CAST) || ((arg0 != null) && !((L2Skill) arg0).isToggle()))
		{
			_nextIntention = null;
			super.changeIntention(intention, arg0, arg1);
			return;
		}
		
		// Do nothing if next intention is same as current one.
		if ((intention == _intention) && (arg0 == _intentionArg0) && (arg1 == _intentionArg1))
		{
			super.changeIntention(intention, arg0, arg1);
			return;
		}
		
		// Save current intention so it can be used after cast
		saveNextIntention(_intention, _intentionArg0, _intentionArg1);
		
		super.changeIntention(intention, arg0, arg1);
	}
	
	@Override
	protected void onEvtReadyToAct()
	{
		if (_nextIntention != null)
		{
			setIntention(_nextIntention._crtlIntention, _nextIntention._arg0, _nextIntention._arg1);
			_nextIntention = null;
		}
		
		super.onEvtReadyToAct();
	}
	
	@Override
	protected void onEvtCancel()
	{
		_nextIntention = null;
		super.onEvtCancel();
	}
	
	@Override
	protected void onEvtFinishCasting()
	{
		if (getIntention() == AI_INTENTION_CAST)
		{
			IntentionCommand nextIntention = _nextIntention;
			if (nextIntention != null)
			{
				if (nextIntention._crtlIntention != AI_INTENTION_CAST)
				{
					setIntention(nextIntention._crtlIntention, nextIntention._arg0, nextIntention._arg1);
				}
				else
				{
					setIntention(AI_INTENTION_IDLE);
				}
			}
			else
			{
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
			
			if (getTarget() != null)
			{
				setTarget(null);
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
	protected void onIntentionMoveTo(Location loc)
	{
		if (getIntention() == AI_INTENTION_REST)
		{
			clientActionFailed();
			return;
		}
		
		if (_actor.getActingPlayer().getDuelState() == Duel.DUELSTATE_DEAD)
		{
			clientActionFailed();
			_actor.getActingPlayer().sendPacket(SystemMessage.getSystemMessage(SystemMessageId.CANNOT_MOVE_FROZEN));
			return;
		}
		
		if (_actor.isAllSkillsDisabled() || _actor.isCastingNow() || _actor.isAttackingNow())
		{
			saveNextIntention(AI_INTENTION_MOVE_TO, loc, null);
			clientActionFailed();
			return;
		}
		
		changeIntention(AI_INTENTION_MOVE_TO, loc, null);
		
		clientStopAutoAttack();
		
		_actor.abortAttack();
		
		moveTo(loc.getX(), loc.getY(), loc.getZ());
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
		L2Character target = (L2Character) getTarget();
		if (target == null)
		{
			return;
		}
		
		if (checkTargetLostOrDead(target))
		{
			setTarget(null);
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
		L2Character target = (L2Character) getTarget();
		if (checkTargetLost(target))
		{
			if (_skill.isOffensive() && getTarget() != null)
			{
				setTarget(null);
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
		
		L2Object oldTarget = _actor.getTarget();
		if (oldTarget != null)
		{
			if (target != null && oldTarget != target)
			{
				_actor.setTarget(getTarget());
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
		
		if (!(target instanceof L2StaticObjectInstance))
		{
			_actor.getActingPlayer().doInteract((L2Character) target);
		}
		
		setIntention(AI_INTENTION_IDLE);
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
