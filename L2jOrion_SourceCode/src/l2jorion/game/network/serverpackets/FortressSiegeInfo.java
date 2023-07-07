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

import java.util.Calendar;

import l2jorion.game.datatables.sql.ClanTable;
import l2jorion.game.model.L2Clan;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.entity.siege.Fort;
import l2jorion.game.network.PacketServer;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;

public class FortressSiegeInfo extends PacketServer
{
	private static final String _S__C9_SIEGEINFO = "[S] c9 SiegeInfo";
	
	private static Logger LOG = LoggerFactory.getLogger(FortressSiegeInfo.class);
	
	private final Fort _fort;
	
	public FortressSiegeInfo(final Fort fort)
	{
		_fort = fort;
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
		writeD(_fort.getFortId());
		writeD(_fort.getOwnerId() == activeChar.getClanId() && activeChar.isClanLeader() ? 0x01 : 0x00);
		writeD(_fort.getOwnerId());
		if (_fort.getOwnerId() > 0)
		{
			final L2Clan owner = ClanTable.getInstance().getClan(_fort.getOwnerId());
			if (owner != null)
			{
				writeS(owner.getName()); // Clan Name
				writeS(owner.getLeaderName()); // Clan Leader Name
				writeD(owner.getAllyId()); // Ally ID
				writeS(owner.getAllyName()); // Ally Name
			}
			else
			{
				LOG.warn("Null owner for fort: " + _fort.getName());
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
		writeD((int) (_fort.getSiege().getSiegeDate().getTimeInMillis() / 1000));
		writeD(0x00); // number of choices?
	}
	
	@Override
	public String getType()
	{
		return _S__C9_SIEGEINFO;
	}
	
}
