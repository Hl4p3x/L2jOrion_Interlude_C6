/*
 * L2jOrion Project - www.l2jorion.com 
 * 
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package l2jorion.game.network.serverpackets;

import l2jorion.game.datatables.sql.ClanTable;
import l2jorion.game.model.L2Clan;
import l2jorion.game.model.L2SiegeClan;
import l2jorion.game.model.entity.siege.Fort;

/**
 * Populates the Siege Attacker List in the SiegeInfo Window<BR>
 * <BR>
 * packet type id 0xca<BR>
 * format: cddddddd + dSSdddSSd<BR>
 * <BR>
 * c = ca<BR>
 * d = FortID<BR>
 * d = unknow (0x00)<BR>
 * d = unknow (0x01)<BR>
 * d = unknow (0x00)<BR>
 * d = Number of Attackers Clans?<BR>
 * d = Number of Attackers Clans<BR>
 * { //repeats<BR>
 * d = ClanID<BR>
 * S = ClanName<BR>
 * S = ClanLeaderName<BR>
 * d = ClanCrestID<BR>
 * d = signed time (seconds)<BR>
 * d = AllyID<BR>
 * S = AllyName<BR>
 * S = AllyLeaderName<BR>
 * d = AllyCrestID<BR>
 * @author programmos, scoria dev
 */
public final class FortSiegeAttackerList extends L2GameServerPacket
{
	private static final String _S__CA_SiegeAttackerList = "[S] ca SiegeAttackerList";
	
	private final Fort _fort;
	
	public FortSiegeAttackerList(final Fort fort)
	{
		_fort = fort;
	}
	
	@Override
	protected final void writeImpl()
	{
		writeC(0xca);
		writeD(_fort.getFortId());
		writeD(0x00); // 0
		writeD(0x01); // 1
		writeD(0x00); // 0
		final int size = _fort.getSiege().getAttackerClans().size();
		if (size > 0)
		{
			L2Clan clan;
			
			writeD(size);
			writeD(size);
			for (final L2SiegeClan siegeclan : _fort.getSiege().getAttackerClans())
			{
				clan = ClanTable.getInstance().getClan(siegeclan.getClanId());
				if (clan == null)
				{
					continue;
				}
				
				writeD(clan.getClanId());
				writeS(clan.getName());
				writeS(clan.getLeaderName());
				writeD(clan.getCrestId());
				writeD(0x00); // signed time (seconds) (not storated by L2J)
				writeD(clan.getAllyId());
				writeS(clan.getAllyName());
				writeS(""); // AllyLeaderName
				writeD(clan.getAllyCrestId());
			}
		}
		else
		{
			writeD(0x00);
			writeD(0x00);
		}
	}
	
	@Override
	public String getType()
	{
		return _S__CA_SiegeAttackerList;
	}
	
}
