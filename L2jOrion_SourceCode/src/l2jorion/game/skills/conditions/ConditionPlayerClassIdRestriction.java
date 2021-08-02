/*
 * L2jOrion Project - www.l2jorion.com 
 * 
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package l2jorion.game.skills.conditions;

import java.util.ArrayList;

import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.skills.Env;

public class ConditionPlayerClassIdRestriction extends Condition
{
	private final ArrayList<Integer> _classIds;
	
	public ConditionPlayerClassIdRestriction(final ArrayList<Integer> classId)
	{
		_classIds = classId;
	}
	
	@Override
	public boolean testImpl(final Env env)
	{
		if (!(env.player instanceof L2PcInstance))
		{
			return false;
		}
		return (_classIds.contains(((L2PcInstance) env.player).getClassId().getId()));
	}
}