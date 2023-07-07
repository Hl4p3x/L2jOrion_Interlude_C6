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
package l2jorion.game.network.clientpackets;

import l2jorion.Config;
import l2jorion.game.datatables.sql.ClanTable;
import l2jorion.game.model.L2Clan;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.network.PacketClient;
import l2jorion.game.network.serverpackets.PledgeInfo;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;

public final class RequestPledgeInfo extends PacketClient
{
	private static Logger LOG = LoggerFactory.getLogger(RequestPledgeInfo.class);
	
	private int clanId;
	
	@Override
	protected void readImpl()
	{
		clanId = readD();
	}
	
	@Override
	protected void runImpl()
	{
		if (Config.DEBUG)
		{
			LOG.debug("infos for clan " + clanId + " requested");
		}
		
		final L2PcInstance activeChar = getClient().getActiveChar();
		final L2Clan clan = ClanTable.getInstance().getClan(clanId);
		
		if (activeChar == null)
			return;
		
		if (clan == null)
		{
			if (Config.DEBUG && clanId > 0)
			{
				LOG.debug("Clan data for clanId " + clanId + " is missing for player " + activeChar.getName());
			}
			return; // we have no clan data ?!? should not happen
		}
		activeChar.sendPacket(new PledgeInfo(clan));
	}
	
	@Override
	public String getType()
	{
		return "[C] 66 RequestPledgeInfo";
	}
}
