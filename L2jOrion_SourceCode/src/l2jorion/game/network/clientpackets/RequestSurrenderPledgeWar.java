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
package l2jorion.game.network.clientpackets;

import l2jorion.game.datatables.sql.ClanTable;
import l2jorion.game.model.L2Clan;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.network.PacketClient;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.ActionFailed;
import l2jorion.game.network.serverpackets.SystemMessage;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;

public final class RequestSurrenderPledgeWar extends PacketClient
{
	private static Logger LOG = LoggerFactory.getLogger(RequestSurrenderPledgeWar.class);
	
	private String _pledgeName;
	private L2Clan _clan;
	private L2PcInstance _activeChar;
	
	@Override
	protected void readImpl()
	{
		_pledgeName = readS();
	}
	
	@Override
	protected void runImpl()
	{
		_activeChar = getClient().getActiveChar();
		if (_activeChar == null)
			return;
		
		_clan = _activeChar.getClan();
		if (_clan == null)
			return;
		
		final L2Clan clan = ClanTable.getInstance().getClanByName(_pledgeName);
		if (clan == null)
		{
			_activeChar.sendMessage("No such clan.");
			_activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		LOG.info("RequestSurrenderPledgeWar by " + getClient().getActiveChar().getClan().getName() + " with " + _pledgeName);
		
		if (!_clan.isAtWarWith(clan.getClanId()))
		{
			_activeChar.sendMessage("You aren't at war with this clan.");
			_activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		SystemMessage msg = new SystemMessage(SystemMessageId.YOU_HAVE_SURRENDERED_TO_THE_S1_CLAN);
		msg.addString(_pledgeName);
		_activeChar.sendPacket(msg);
		msg = null;
		_activeChar.deathPenalty(false);
		ClanTable.getInstance().deleteclanswars(_clan.getClanId(), clan.getClanId());
		/*
		 * L2PcInstance leader = L2World.getInstance().getPlayer(clan.getLeaderName()); if(leader != null && leader.isOnline() == 0) { player.sendMessage("Clan leader isn't online."); player.sendPacket(ActionFailed.STATIC_PACKET); return; } if (leader.isTransactionInProgress()) { SystemMessage sm =
		 * new SystemMessage(SystemMessageId.S1_IS_BUSY_TRY_LATER); sm.addString(leader.getName()); player.sendPacket(sm); return; } leader.setTransactionRequester(player); player.setTransactionRequester(leader); leader.sendPacket(new SurrenderPledgeWar(_clan.getName(),player.getName()));
		 */
	}
	
	@Override
	public String getType()
	{
		return "[C] 51 RequestSurrenderPledgeWar";
	}
}
