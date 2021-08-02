/*
 * Copyright (C) 2004-2013 L2J Server
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
package l2jorion.game.skills.holders;

import java.util.HashMap;
import java.util.Map;

import l2jorion.game.model.L2Skill;

public class PlayerSkillHolder implements ISkillsHolder
{
	private final Map<Integer, L2Skill> _skills = new HashMap<>();
	
	public PlayerSkillHolder(Map<Integer, L2Skill> map)
	{
		_skills.putAll(map);
	}
	
	@Override
	public Map<Integer, L2Skill> getSkills()
	{
		return _skills;
	}
	
	@Override
	public L2Skill addSkill(L2Skill skill)
	{
		return _skills.put(skill.getId(), skill);
	}
	
	@Override
	public int getSkillLevel(int skillId)
	{
		final L2Skill skill = getKnownSkill(skillId);
		return (skill == null) ? -1 : skill.getLevel();
	}
	
	@Override
	public L2Skill getKnownSkill(int skillId)
	{
		return _skills.get(skillId);
	}
}