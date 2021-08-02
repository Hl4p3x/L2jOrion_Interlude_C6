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
package l2jorion.game.datatables;

import java.util.HashMap;
import java.util.Map;

import javolution.util.FastMap;
import l2jorion.game.model.L2Skill;
import l2jorion.game.skills.SkillsEngine;
import l2jorion.game.templates.L2WeaponType;

public class SkillTable
{
	private static SkillTable _instance;
	
	private final Map<Integer, L2Skill> _skills;
	private static final Map<Integer, Integer> _skillsMaxLevel = new HashMap<>();
	private final boolean _initialized = true;
	
	public static SkillTable getInstance()
	{
		if (_instance == null)
		{
			_instance = new SkillTable();
		}
		
		return _instance;
	}
	
	private SkillTable()
	{
		_skills = new FastMap<>();
		_skills.clear();
		SkillsEngine.getInstance().loadAllSkills(_skills);
		
		for (final L2Skill skill : _skills.values())
		{
			// Only non-enchanted skills
			final int skillLvl = skill.getLevel();
			if (skillLvl < 99)
			{
				final int skillId = skill.getId();
				final int maxLvl = getMaxLevel(skillId);
				
				if (skillLvl > maxLvl)
				{
					_skillsMaxLevel.put(skillId, skillLvl);
				}
			}
		}
	}
	
	public void reload()
	{
		_skills.clear();
		_skillsMaxLevel.clear();
		
		_instance = new SkillTable();
	}
	
	public boolean isInitialized()
	{
		return _initialized;
	}
	
	/**
	 * Provides the skill hash
	 * @param skill The L2Skill to be hashed
	 * @return SkillTable.getSkillHashCode(skill.getId(), skill.getLevel())
	 */
	public static int getSkillHashCode(final L2Skill skill)
	{
		return SkillTable.getSkillHashCode(skill.getId(), skill.getLevel());
	}
	
	/**
	 * Centralized method for easier change of the hashing sys
	 * @param skillId The Skill Id
	 * @param skillLevel The Skill Level
	 * @return The Skill hash number
	 */
	public static int getSkillHashCode(final int skillId, final int skillLevel)
	{
		return skillId * 1021 + skillLevel;
	}
	
	public L2Skill getInfo(final int skillId, final int level)
	{
		return _skills.get(SkillTable.getSkillHashCode(skillId, level));
	}
	
	public int getMaxLevel(final int magicId, int level)
	{
		L2Skill temp;
		
		while (level < 100)
		{
			level++;
			temp = _skills.get(SkillTable.getSkillHashCode(magicId, level));
			
			if (temp == null)
			{
				return level - 1;
			}
		}
		
		return level;
	}
	
	public int getMaxLevel(int skillId)
	{
		final Integer maxLevel = _skillsMaxLevel.get(skillId);
		return (maxLevel != null) ? maxLevel : 0;
	}
	
	private static final L2WeaponType[] weaponDbMasks =
	{
		L2WeaponType.ETC,
		L2WeaponType.BOW,
		L2WeaponType.POLE,
		L2WeaponType.DUALFIST,
		L2WeaponType.DUAL,
		L2WeaponType.BLUNT,
		L2WeaponType.SWORD,
		L2WeaponType.DAGGER,
		L2WeaponType.BIGSWORD,
		L2WeaponType.ROD,
		L2WeaponType.BIGBLUNT
	};
	
	public int calcWeaponsAllowed(final int mask)
	{
		if (mask == 0)
		{
			return 0;
		}
		
		int weaponsAllowed = 0;
		
		for (int i = 0; i < weaponDbMasks.length; i++)
		{
			if ((mask & 1 << i) != 0)
			{
				weaponsAllowed |= weaponDbMasks[i].mask();
			}
		}
		return weaponsAllowed;
	}
}
