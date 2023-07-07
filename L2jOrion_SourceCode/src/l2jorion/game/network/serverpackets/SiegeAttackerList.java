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
import l2jorion.game.model.L2SiegeClan;
import l2jorion.game.model.entity.siege.Castle;
import l2jorion.game.model.entity.siege.hallsiege.SiegableHall;
import l2jorion.game.network.PacketServer;

public class SiegeAttackerList extends PacketServer
{
	private static final String _S__CA_SiegeAttackerList = "[S] ca SiegeAttackerList";
	
	private Castle _castle;
	private SiegableHall _hall;
	
	public SiegeAttackerList(Castle castle)
	{
		_castle = castle;
	}
	
	public SiegeAttackerList(SiegableHall hall)
	{
		_hall = hall;
	}
	
	@Override
	protected final void writeImpl()
	{
		writeC(0xca);
		if (_castle != null)
		{
			writeD(_castle.getCastleId());
			writeD(0x00); // 0
			writeD(0x01); // 1
			writeD(0x00); // 0
			final int size = _castle.getSiege().getAttackerClans().size();
			if (size > 0)
			{
				L2Clan clan;
				
				writeD(size);
				writeD(size);
				for (final L2SiegeClan siegeclan : _castle.getSiege().getAttackerClans())
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
					writeD(0x00);
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
		else
		{
			writeD(_hall.getClanHallId());
			writeD(0x00); // 0
			writeD(0x01); // 1
			writeD(0x00); // 0
			final int size = _hall.getSiege().getAttackerClans().size();
			if (size > 0)
			{
				L2Clan clan;
				
				writeD(size);
				writeD(size);
				for (final L2SiegeClan siegeclan : _hall.getSiege().getAttackerClans())
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
					writeD(0x00);
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
	}
	
	@Override
	public String getType()
	{
		return _S__CA_SiegeAttackerList;
	}
	
}
