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

import l2jorion.game.datatables.sql.ClanTable;
import l2jorion.game.model.L2Clan;
import l2jorion.game.model.L2ClanMember;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.network.PacketServer;

public class GMViewPledgeInfo extends PacketServer
{
	private static final String _S__A9_GMVIEWPLEDGEINFO = "[S] 90 GMViewPledgeInfo";
	private final L2Clan _clan;
	private final L2PcInstance _activeChar;
	
	public GMViewPledgeInfo(final L2Clan clan, final L2PcInstance activeChar)
	{
		_clan = clan;
		_activeChar = activeChar;
	}
	
	@Override
	protected final void writeImpl()
	{
		final int TOP = ClanTable.getInstance().getTopRate(_clan.getClanId());
		writeC(0x90);
		writeS(_activeChar.getName());
		writeD(_clan.getClanId());
		writeD(0x00);
		writeS(_clan.getName());
		writeS(_clan.getLeaderName());
		writeD(_clan.getCrestId()); // -> no, it's no longer used (nuocnam) fix by game
		writeD(_clan.getLevel());
		writeD(_clan.getHasCastle());
		writeD(_clan.getHasHideout());
		writeD(TOP);
		writeD(_clan.getReputationScore());
		writeD(0);
		writeD(0);
		
		writeD(_clan.getAllyId()); // c2
		writeS(_clan.getAllyName()); // c2
		writeD(_clan.getAllyCrestId()); // c2
		writeD(_clan.isAtWar()); // c3
		
		final L2ClanMember[] members = _clan.getMembers();
		writeD(members.length);
		
		for (final L2ClanMember member : members)
		{
			writeS(member.getName());
			writeD(member.getLevel());
			writeD(member.getClassId());
			writeD(0);
			writeD(1);
			writeD(member.isOnline() ? member.getObjectId() : 0);
			writeD(0);
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see l2jorion.game.serverpackets.ServerBasePacket#getType()
	 */
	@Override
	public String getType()
	{
		return _S__A9_GMVIEWPLEDGEINFO;
	}
	
}
