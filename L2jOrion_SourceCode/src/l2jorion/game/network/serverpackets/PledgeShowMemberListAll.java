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
import l2jorion.game.model.L2Clan.SubPledge;
import l2jorion.game.model.L2ClanMember;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.network.PacketServer;

public class PledgeShowMemberListAll extends PacketServer
{
	private static final String _S__68_PLEDGESHOWMEMBERLISTALL = "[S] 53 PledgeShowMemberListAll";
	
	private final L2Clan _clan;
	private final L2PcInstance _activeChar;
	private final L2ClanMember[] _members;
	private int _pledgeType;
	
	public PledgeShowMemberListAll(final L2Clan clan, final L2PcInstance activeChar)
	{
		_clan = clan;
		_activeChar = activeChar;
		_members = _clan.getMembers();
	}
	
	@Override
	protected final void writeImpl()
	{
		
		_pledgeType = 0;
		writePledge(0);
		
		final SubPledge[] subPledge = _clan.getAllSubPledges();
		for (final SubPledge element : subPledge)
		{
			_activeChar.sendPacket(new PledgeReceiveSubPledgeCreated(element));
		}
		
		for (final L2ClanMember m : _members)
		{
			if (m.getPledgeType() == 0)
			{
				continue;
			}
			_activeChar.sendPacket(new PledgeShowMemberListAdd(m));
		}
		
		// unless this is sent sometimes, the client doesn't recognise the player as the leader
		_activeChar.sendPacket(new UserInfo(_activeChar));
		
	}
	
	void writePledge(final int mainOrSubpledge)
	{
		final int TOP = ClanTable.getInstance().getTopRate(_clan.getClanId());
		
		writeC(0x53);
		
		writeD(mainOrSubpledge); // c5 main clan 0 or any subpledge 1?
		writeD(_clan.getClanId());
		writeD(_pledgeType); // c5 - possibly pledge type?
		writeS(_clan.getName());
		writeS(_clan.getLeaderName());
		
		writeD(_clan.getCrestId()); // crest id .. is used again
		writeD(_clan.getLevel());
		writeD(_clan.getHasCastle());
		writeD(_clan.getHasHideout());
		writeD(TOP);
		writeD(_clan.getReputationScore()); // was activechar lvl
		writeD(0); // 0
		writeD(0); // 0
		
		writeD(_clan.getAllyId());
		writeS(_clan.getAllyName());
		writeD(_clan.getAllyCrestId());
		writeD(_clan.isAtWar());
		writeD(_clan.getSubPledgeMembersCount(_pledgeType));
		
		int yellow;
		for (final L2ClanMember m : _members)
		{
			if (m.getPledgeType() != _pledgeType)
			{
				continue;
			}
			if (m.getPledgeType() == -1)
			{
				yellow = m.getSponsor() != 0 ? 1 : 0;
			}
			else if (m.getPlayerInstance() != null)
			{
				yellow = m.getPlayerInstance().isClanLeader() ? 1 : 0;
			}
			else
			{
				yellow = 0;
			}
			writeS(m.getName());
			writeD(m.getLevel());
			writeD(m.getClassId());
			writeD(0);
			writeD(m.getObjectId());
			writeD(m.isOnline() ? 1 : 0);
			writeD(yellow);
		}
	}
	
	@Override
	public String getType()
	{
		return _S__68_PLEDGESHOWMEMBERLISTALL;
	}
	
}
