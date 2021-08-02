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

public class ConditionTargetLevel extends Condition
{
	
	private final int _level;
	
	public ConditionTargetLevel(final int level)
	{
		_level = level;
	}
	
	@Override
	public boolean testImpl(final Env env)
	{
		if (env.target == null)
		{
			return false;
		}
		return env.target.getLevel() >= _level;
	}
}
