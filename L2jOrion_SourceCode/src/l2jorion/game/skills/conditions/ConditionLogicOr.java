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

import l2jorion.game.skills.Env;

public class ConditionLogicOr extends Condition
{
	
	private static Condition[] _emptyConditions = new Condition[0];
	public Condition[] conditions = _emptyConditions;
	
	public void add(final Condition condition)
	{
		if (condition == null)
		{
			return;
		}
		if (getListener() != null)
		{
			condition.setListener(this);
		}
		final int len = conditions.length;
		final Condition[] tmp = new Condition[len + 1];
		System.arraycopy(conditions, 0, tmp, 0, len);
		tmp[len] = condition;
		conditions = tmp;
	}
	
	@Override
	void setListener(final ConditionListener listener)
	{
		if (listener != null)
		{
			for (final Condition c : conditions)
			{
				c.setListener(this);
			}
		}
		else
		{
			for (final Condition c : conditions)
			{
				c.setListener(null);
			}
		}
		super.setListener(listener);
	}
	
	@Override
	public boolean testImpl(final Env env)
	{
		for (final Condition c : conditions)
		{
			if (c.test(env))
			{
				return true;
			}
		}
		return false;
	}
	
}
