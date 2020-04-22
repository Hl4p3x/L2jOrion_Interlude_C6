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
package l2jorion.game.skills;

import l2jorion.game.datatables.SkillTable;
import l2jorion.game.model.L2Skill;

public final class SkillHolder
{
	private final int _skillId;
	private final int _skillLvl;
	
	public SkillHolder(final int skillId, final int skillLvl)
	{
		_skillId = skillId;
		_skillLvl = skillLvl;
	}
	
	public SkillHolder(final L2Skill skill)
	{
		_skillId = skill.getId();
		_skillLvl = skill.getLevel();
	}
	
	public final int getSkillId()
	{
		return _skillId;
	}
	
	public final int getSkillLvl()
	{
		return _skillLvl;
	}
	
	public final L2Skill getSkill()
	{
		return SkillTable.getInstance().getInfo(_skillId, _skillLvl);
	}
}