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
package l2jorion.game.network.serverpackets;

import java.util.List;

import javolution.util.FastList;

public class AquireSkillList extends L2GameServerPacket
{
	public enum skillType
	{
		Usual,
		Fishing,
		Clan
	}
	
	private static final String _S__A3_AQUIRESKILLLIST = "[S] 8a AquireSkillList";
	
	private final List<Skill> _skills;
	private final skillType _fishingSkills;
	
	private class Skill
	{
		public int id;
		public int nextLevel;
		public int maxLevel;
		public int spCost;
		public int requirements;
		
		public Skill(final int pId, final int pNextLevel, final int pMaxLevel, final int pSpCost, final int pRequirements)
		{
			id = pId;
			nextLevel = pNextLevel;
			maxLevel = pMaxLevel;
			spCost = pSpCost;
			requirements = pRequirements;
		}
	}
	
	public AquireSkillList(final skillType type)
	{
		_skills = new FastList<>();
		_fishingSkills = type;
	}
	
	public void addSkill(final int id, final int nextLevel, final int maxLevel, final int spCost, final int requirements)
	{
		_skills.add(new Skill(id, nextLevel, maxLevel, spCost, requirements));
	}
	
	@Override
	protected final void writeImpl()
	{
		writeC(0x8a);
		writeD(_fishingSkills.ordinal()); // c4 : C5 : 0: usuall 1: fishing 2: clans
		writeD(_skills.size());
		
		for (final Skill temp : _skills)
		{
			writeD(temp.id);
			writeD(temp.nextLevel);
			writeD(temp.maxLevel);
			writeD(temp.spCost);
			writeD(temp.requirements);
		}
	}
	
	@Override
	public String getType()
	{
		return _S__A3_AQUIRESKILLLIST;
	}
}
