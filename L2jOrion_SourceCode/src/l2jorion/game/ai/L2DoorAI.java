package l2jorion.game.ai;

import l2jorion.game.model.L2Character;
import l2jorion.game.model.L2Object;
import l2jorion.game.model.L2Skill;
import l2jorion.game.model.Location;
import l2jorion.game.model.actor.instance.L2DoorInstance;
import l2jorion.game.model.actor.instance.L2FortSiegeGuardInstance;
import l2jorion.game.model.actor.instance.L2SiegeGuardInstance;
import l2jorion.game.thread.ThreadPoolManager;

public class L2DoorAI extends L2CharacterAI
{
	
	public L2DoorAI(final L2DoorInstance creature)
	{
		super(creature);
	}
	
	@Override
	protected void onIntentionIdle()
	{
	}
	
	@Override
	protected void onIntentionActive()
	{
	}
	
	@Override
	protected void onIntentionRest()
	{
	}
	
	@Override
	protected void onIntentionAttack(final L2Character target)
	{
	}
	
	@Override
	protected void onIntentionCast(final L2Skill skill, final L2Object target)
	{
	}
	
	@Override
	protected void onIntentionMoveTo(final Location destination)
	{
	}
	
	@Override
	protected void onIntentionFollow(final L2Character target)
	{
	}
	
	@Override
	protected void onIntentionPickUp(final L2Object item)
	{
	}
	
	@Override
	protected void onIntentionInteract(final L2Object object)
	{
	}
	
	@Override
	protected void onEvtThink()
	{
	}
	
	@Override
	protected void onEvtAttacked(final L2Character attacker)
	{
		L2DoorInstance me = (L2DoorInstance) _actor;
		ThreadPoolManager.getInstance().executeTask(new onEventAttackedDoorTask(me, attacker));
	}
	
	@Override
	protected void onEvtAggression(final L2Character target, final int aggro)
	{
	}
	
	@Override
	protected void onEvtStunned(final L2Character attacker)
	{
	}
	
	@Override
	protected void onEvtSleeping(final L2Character attacker)
	{
	}
	
	@Override
	protected void onEvtRooted(final L2Character attacker)
	{
	}
	
	@Override
	protected void onEvtReadyToAct()
	{
	}
	
	@Override
	protected void onEvtUserCmd(final Object arg0, final Object arg1)
	{
	}
	
	@Override
	protected void onEvtArrived()
	{
	}
	
	@Override
	protected void onEvtArrivedRevalidate()
	{
	}
	
	@Override
	protected void onEvtArrivedBlocked(final Location blocked_at_pos)
	{
	}
	
	@Override
	protected void onEvtForgetObject(final L2Object object)
	{
	}
	
	@Override
	protected void onEvtCancel()
	{
	}
	
	@Override
	protected void onEvtDead()
	{
	}
	
	private class onEventAttackedDoorTask implements Runnable
	{
		private final L2DoorInstance _door;
		private final L2Character _attacker;
		
		public onEventAttackedDoorTask(final L2DoorInstance door, final L2Character attacker)
		{
			_door = door;
			_attacker = attacker;
		}
		
		@Override
		public void run()
		{
			for (final L2SiegeGuardInstance guard : _door.getKnownSiegeGuards())
			{
				if (guard != null && guard.getAI() != null && _actor.isInsideRadius(guard, guard.getFactionRange(), false, true) && Math.abs(_attacker.getZ() - guard.getZ()) < 200)
				{
					guard.getAI().notifyEvent(CtrlEvent.EVT_AGGRESSION, _attacker, 15);
				}
			}
			for (final L2FortSiegeGuardInstance guard : _door.getKnownFortSiegeGuards())
			{
				if (guard != null && guard.getAI() != null && _actor.isInsideRadius(guard, guard.getFactionRange(), false, true) && Math.abs(_attacker.getZ() - guard.getZ()) < 200)
				{
					guard.getAI().notifyEvent(CtrlEvent.EVT_AGGRESSION, _attacker, 15);
				}
			}
		}
	}
}
