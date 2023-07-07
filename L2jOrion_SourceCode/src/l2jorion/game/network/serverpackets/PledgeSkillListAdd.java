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

import l2jorion.game.network.PacketServer;

public class PledgeSkillListAdd extends PacketServer
{
	private static final String _S__FE_3A_PLEDGESKILLLISTADD = "[S] FE:3A PledgeSkillListAdd";
	
	private final int _id;
	private final int _lvl;
	
	public PledgeSkillListAdd(final int id, final int lvl)
	{
		_id = id;
		_lvl = lvl;
	}
	
	@Override
	protected void writeImpl()
	{
		writeC(0xfe);
		writeH(0x3a);
		
		writeD(_id);
		writeD(_lvl);
	}
	
	@Override
	public String getType()
	{
		return _S__FE_3A_PLEDGESKILLLISTADD;
	}
}
