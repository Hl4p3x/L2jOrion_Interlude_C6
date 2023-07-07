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

import java.util.Calendar;

import l2jorion.game.datatables.sql.ClanTable;
import l2jorion.game.managers.CHSiegeManager;
import l2jorion.game.model.L2Clan;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.entity.ClanHall;
import l2jorion.game.model.entity.siege.Castle;
import l2jorion.game.network.PacketServer;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;

public class SiegeInfo extends PacketServer
{
	private static Logger LOG = LoggerFactory.getLogger(SiegeInfo.class);
	
	private static final String _S__C9_SIEGEINFO = "[S] c9 SiegeInfo";
	
	private Castle _castle;
	private ClanHall _hall;
	
	public SiegeInfo(Castle castle)
	{
		_castle = castle;
	}
	
	public SiegeInfo(ClanHall hall)
	{
		_hall = hall;
	}
	
	@Override
	protected final void writeImpl()
	{
		final L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null)
		{
			return;
		}
		
		writeC(0xc9);
		if (_castle != null)
		{
			writeD(_castle.getCastleId());
			writeD(_castle.getOwnerId() == activeChar.getClanId() && activeChar.isClanLeader() ? 0x01 : 0x00);
			writeD(_castle.getOwnerId());
			if (_castle.getOwnerId() > 0)
			{
				final L2Clan owner = ClanTable.getInstance().getClan(_castle.getOwnerId());
				if (owner != null)
				{
					writeS(owner.getName()); // Clan Name
					writeS(owner.getLeaderName()); // Clan Leader Name
					writeD(owner.getAllyId()); // Ally ID
					writeS(owner.getAllyName()); // Ally Name
				}
				else
				{
					LOG.warn("Null owner for castle: " + _castle.getName());
				}
			}
			else
			{
				writeS("NPC"); // Clan Name
				writeS(""); // Clan Leader Name
				writeD(0); // Ally ID
				writeS(""); // Ally Name
			}
			
			writeD((int) (Calendar.getInstance().getTimeInMillis() / 1000));
			writeD((int) (_castle.getSiege().getSiegeDate().getTimeInMillis() / 1000));
			writeD(0x00); // number of choices?
		}
		else
		{
			writeD(_hall.getClanHallId());
			
			final int ownerId = _hall.getOwnerId();
			writeD(ownerId == activeChar.getClanId() && activeChar.isClanLeader() ? 0x01 : 0x00);
			writeD(ownerId);
			if (_hall.getOwnerId() > 0)
			{
				final L2Clan owner = ClanTable.getInstance().getClan(_hall.getOwnerId());
				if (owner != null)
				{
					writeS(owner.getName()); // Clan Name
					writeS(owner.getLeaderName()); // Clan Leader Name
					writeD(owner.getAllyId()); // Ally ID
					writeS(owner.getAllyName()); // Ally Name
				}
				else
				{
					LOG.warn("Null owner for castle: " + _castle.getName());
				}
			}
			else
			{
				writeS("NPC"); // Clan Name
				writeS(""); // Clan Leader Name
				writeD(0); // Ally ID
				writeS(""); // Ally Name
			}
			
			writeD((int) (Calendar.getInstance().getTimeInMillis() / 1000));
			writeD((int) ((CHSiegeManager.getInstance().getSiegableHall(_hall.getClanHallId()).getNextSiegeTime()) / 1000));
			writeD(0x00); // number of choices?
		}
	}
	
	@Override
	public String getType()
	{
		return _S__C9_SIEGEINFO;
	}
	
}
