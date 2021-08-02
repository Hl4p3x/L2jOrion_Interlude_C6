/*
 * Copyright (C) 2004-2016 L2J Server
 * 
 * This file is part of L2J Server.
 * 
 * L2J Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * L2J Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package l2jorion.game.model.actor.status;

import l2jorion.game.model.L2Attackable;
import l2jorion.game.model.L2Character;

public class AttackableStatus extends NpcStatus
{
	public AttackableStatus(L2Attackable activeChar)
	{
		super(activeChar);
	}
	
	@Override
	public final void reduceHp(double value, L2Character attacker)
	{
		reduceHp(value, attacker, true);
	}
	
	@Override
	public final void reduceHp(double value, L2Character attacker, boolean awake)
	{
		if (getActiveChar().isDead())
		{
			return;
		}
		
		if (value > 0)
		{
			if (getActiveChar().isOverhit())
			{
				getActiveChar().setOverhitValues(attacker, value);
			}
			else
			{
				getActiveChar().overhitEnabled(false);
			}
		}
		else
		{
			getActiveChar().overhitEnabled(false);
		}
		
		super.reduceHp(value, attacker, awake);
		
		if (!getActiveChar().isDead())
		{
			// And the attacker's hit didn't kill the mob, clear the over-hit flag
			getActiveChar().overhitEnabled(false);
		}
	}
	
	@Override
	public L2Attackable getActiveChar()
	{
		return (L2Attackable) super.getActiveChar();
	}
}