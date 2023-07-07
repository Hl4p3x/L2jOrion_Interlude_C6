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

import l2jorion.game.model.L2Skill;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.network.PacketServer;

public class GMViewSkillInfo extends PacketServer
{
	private static final String _S__91_GMViewSkillInfo = "[S] 91 GMViewSkillInfo";
	private final L2PcInstance _activeChar;
	private L2Skill[] _skills;
	
	public GMViewSkillInfo(final L2PcInstance cha)
	{
		_activeChar = cha;
		_skills = _activeChar.getAllSkills();
		if (_skills.length == 0)
		{
			_skills = new L2Skill[0];
		}
	}
	
	@Override
	protected final void writeImpl()
	{
		writeC(0x91);
		writeS(_activeChar.getName());
		writeD(_skills.length);
		
		for (final L2Skill skill : _skills)
		{
			writeD(skill.isPassive() ? 1 : 0);
			writeD(skill.getLevel());
			writeD(skill.getId());
			writeC(0x00); // c5
		}
	}
	
	@Override
	public String getType()
	{
		return _S__91_GMViewSkillInfo;
	}
}
