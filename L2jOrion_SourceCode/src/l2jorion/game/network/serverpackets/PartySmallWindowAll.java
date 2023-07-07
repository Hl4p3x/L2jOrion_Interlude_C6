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

import l2jorion.game.model.L2Party;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.network.PacketServer;

public final class PartySmallWindowAll extends PacketServer
{
	private static final String _S__63_PARTYSMALLWINDOWALL = "[S] 4e PartySmallWindowAll";
	
	private final L2Party _party;
	private final L2PcInstance _exclude;
	private final int _dist, _LeaderOID;
	
	public PartySmallWindowAll(final L2PcInstance exclude, final L2Party party)
	{
		_exclude = exclude;
		_party = party;
		_LeaderOID = _party.getPartyLeaderOID();
		_dist = _party.getLootDistribution();
	}
	
	@Override
	protected final void writeImpl()
	{
		writeC(0x4e);
		writeD(_LeaderOID);
		writeD(_dist);
		writeD(_party.getMemberCount() - 1);
		
		for (final L2PcInstance member : _party.getPartyMembers())
		{
			if ((member != null) && (member != _exclude))
			{
				writeD(member.getObjectId());
				writeS(member.getName());
				
				writeD((int) member.getCurrentCp()); // c4
				writeD(member.getMaxCp()); // c4
				
				writeD((int) member.getCurrentHp());
				writeD(member.getMaxHp());
				writeD((int) member.getCurrentMp());
				writeD(member.getMaxMp());
				writeD(member.getLevel());
				writeD(member.getClassId().getId());
				writeD(0);
				writeD(member.getRace().ordinal());
			}
		}
	}
	
	@Override
	public String getType()
	{
		return _S__63_PARTYSMALLWINDOWALL;
	}
}