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
		// null;
	}
	
	@Override
	protected void onIntentionActive()
	{
		// null;
	}
	
	@Override
	protected void onIntentionRest()
	{
		// null;
	}
	
	@Override
	protected void onIntentionAttack(final L2Character target)
	{
		// null;
	}
	
	@Override
	protected void onIntentionCast(final L2Skill skill, final L2Object target)
	{
		// null;
	}
	
	@Override
	protected void onIntentionMoveTo(final Location destination)
	{
		// null;
	}
	
	@Override
	protected void onIntentionFollow(final L2Character target)
	{
		// null;
	}
	
	@Override
	protected void onIntentionPickUp(final L2Object item)
	{
		// null;
	}
	
	@Override
	protected void onIntentionInteract(final L2Object object)
	{
		// null;
	}
	
	@Override
	protected void onEvtThink()
	{
		// null;
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
		// null;
	}
	
	@Override
	protected void onEvtStunned(final L2Character attacker)
	{
		// null;
	}
	
	@Override
	protected void onEvtSleeping(final L2Character attacker)
	{
		// null;
	}
	
	@Override
	protected void onEvtRooted(final L2Character attacker)
	{
		// null;
	}
	
	@Override
	protected void onEvtReadyToAct()
	{
		// null;
	}
	
	@Override
	protected void onEvtUserCmd(final Object arg0, final Object arg1)
	{
		// null;
	}
	
	@Override
	protected void onEvtArrived()
	{
		// null;
	}
	
	@Override
	protected void onEvtArrivedRevalidate()
	{
		// null;
	}
	
	@Override
	protected void onEvtArrivedBlocked(final Location blocked_at_pos)
	{
		// null;
	}
	
	@Override
	protected void onEvtForgetObject(final L2Object object)
	{
		// null;
	}
	
	@Override
	protected void onEvtCancel()
	{
		// null;
	}
	
	@Override
	protected void onEvtDead()
	{
		// null;
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
