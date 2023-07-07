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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import l2jorion.game.network.PacketServer;

public class SkillList extends PacketServer
{
	private static final String _S__6D_SKILLLIST = "[S] 58 SkillList";
	
	private final List<Skill> _skills = new ArrayList<>();
	
	static class Skill
	{
		public int id;
		public int level;
		public boolean passive;
		public boolean disabled;
		
		Skill(int pId, int pLevel, boolean pPassive, boolean pDisabled)
		{
			id = pId;
			level = pLevel;
			passive = pPassive;
			disabled = pDisabled;
		}
		
		public int getId()
		{
			return id;
		}
	}
	
	public void addSkill(int id, int level, boolean passive, boolean disabled)
	{
		_skills.add(new Skill(id, level, passive, disabled));
		// Let's sort skills by id
		Collections.sort(_skills, (sk1, sk2) -> sk1.getId() - sk2.getId());
	}
	
	@Override
	protected final void writeImpl()
	{
		writeC(0x58);
		writeD(_skills.size());
		
		for (Skill temp : _skills)
		{
			writeD(temp.passive ? 1 : 0);
			writeD(temp.level);
			writeD(temp.id);
			writeC(temp.disabled ? 1 : 0);
		}
	}
	
	@Override
	public String getType()
	{
		return _S__6D_SKILLLIST;
	}
}