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
package l2jorion.game.skills.conditions;

import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.skills.Env;

public class ConditionPlayerState extends Condition
{
	public enum CheckPlayerState
	{
		RESTING,
		MOVING,
		RUNNING,
		FLYING,
		BEHIND,
		FRONT,
		SIDE
	}
	
	private final CheckPlayerState _check;
	private final boolean _required;
	
	public ConditionPlayerState(final CheckPlayerState check, final boolean required)
	{
		_check = check;
		_required = required;
	}
	
	@Override
	public boolean testImpl(final Env env)
	{
		switch (_check)
		{
			case RESTING:
				if (env.player instanceof L2PcInstance)
				{
					return ((L2PcInstance) env.player).isSitting() == _required;
				}
				return !_required;
			case MOVING:
				return env.player.isMoving() == _required;
			case RUNNING:
				return env.player.isMoving() == _required && env.player.isRunning() == _required;
			case FLYING:
				return env.player.isFlying() == _required;
			case BEHIND:
				return env.player.isBehindTarget() == _required;
			case FRONT:
				return env.player.isFrontTarget() == _required;
			case SIDE:
				return env.player.isSideTarget() == _required;
		}
		return !_required;
	}
}