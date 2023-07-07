/* L2jOrion Project - www.l2jorion.com 
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

import l2jorion.game.model.L2ClanMember;
import l2jorion.game.network.PacketServer;

public class PledgeReceiveMemberInfo extends PacketServer
{
	private static final String _S__FE_3D_PLEDGERECEIVEMEMBERINFO = "[S] FE:3D PledgeReceiveMemberInfo";
	
	private final L2ClanMember _member;
	
	public PledgeReceiveMemberInfo(final L2ClanMember member)
	{
		_member = member;
	}
	
	@Override
	protected void writeImpl()
	{
		writeC(0xfe);
		writeH(0x3d);
		
		writeD(_member.getPledgeType());
		writeS(_member.getName());
		writeS(_member.getTitle()); // title
		writeD(_member.getPowerGrade()); // power
		
		// clan or subpledge name
		if (_member.getPledgeType() != 0)
		{
			writeS(_member.getClan().getSubPledge(_member.getPledgeType()).getName());
		}
		else
		{
			writeS(_member.getClan().getName());
		}
		
		writeS(_member.getApprenticeOrSponsorName()); // name of this member's apprentice/sponsor
	}
	
	@Override
	public String getType()
	{
		return _S__FE_3D_PLEDGERECEIVEMEMBERINFO;
	}
}
