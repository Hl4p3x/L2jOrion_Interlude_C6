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

import l2jorion.game.model.L2Clan;
import l2jorion.game.model.L2Skill;
import l2jorion.game.network.PacketServer;

public class PledgeSkillList extends PacketServer
{
	private static final String _S__FE_39_PLEDGESKILLLIST = "[S] FE:39 PledgeSkillList";
	
	private final L2Clan _clan;
	
	public PledgeSkillList(final L2Clan clan)
	{
		_clan = clan;
	}
	
	@Override
	protected void writeImpl()
	{
		final L2Skill[] skills = _clan.getAllSkills();
		
		writeC(0xfe);
		writeH(0x39);
		writeD(skills.length);
		for (final L2Skill sk : skills)
		{
			writeD(sk.getId());
			writeD(sk.getLevel());
		}
	}
	
	@Override
	public String getType()
	{
		return _S__FE_39_PLEDGESKILLLIST;
	}
}
