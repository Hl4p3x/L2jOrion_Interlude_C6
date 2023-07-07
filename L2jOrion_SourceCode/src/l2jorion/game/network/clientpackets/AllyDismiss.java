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
package l2jorion.game.network.clientpackets;

import l2jorion.Config;
import l2jorion.game.datatables.sql.ClanTable;
import l2jorion.game.model.L2Clan;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.network.PacketClient;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.SystemMessage;

public final class AllyDismiss extends PacketClient
{
	private String _clanName;
	
	@Override
	protected void readImpl()
	{
		_clanName = readS();
	}
	
	@Override
	protected void runImpl()
	{
		if (_clanName == null)
			return;
		
		final L2PcInstance player = getClient().getActiveChar();
		
		if (player == null)
			return;
		
		if (player.getClan() == null)
		{
			player.sendPacket(new SystemMessage(SystemMessageId.YOU_ARE_NOT_A_CLAN_MEMBER));
			return;
		}
		
		final L2Clan leaderClan = player.getClan();
		
		if (leaderClan.getAllyId() == 0)
		{
			player.sendPacket(new SystemMessage(SystemMessageId.NO_CURRENT_ALLIANCES));
			return;
		}
		
		if (!player.isClanLeader() || leaderClan.getClanId() != leaderClan.getAllyId())
		{
			player.sendPacket(new SystemMessage(SystemMessageId.FEATURE_ONLY_FOR_ALLIANCE_LEADER));
			return;
		}
		
		final L2Clan clan = ClanTable.getInstance().getClanByName(_clanName);
		
		if (clan == null)
		{
			player.sendPacket(new SystemMessage(SystemMessageId.CLAN_DOESNT_EXISTS));
			return;
		}
		
		if (clan.getClanId() == leaderClan.getClanId())
		{
			player.sendPacket(new SystemMessage(SystemMessageId.ALLIANCE_LEADER_CANT_WITHDRAW));
			return;
		}
		
		if (clan.getAllyId() != leaderClan.getAllyId())
		{
			player.sendPacket(new SystemMessage(SystemMessageId.DIFFERENT_ALLIANCE));
			return;
		}
		
		final long currentTime = System.currentTimeMillis();
		
		leaderClan.setAllyPenaltyExpiryTime(currentTime + Config.ALT_ACCEPT_CLAN_DAYS_WHEN_DISMISSED * 86400000L, L2Clan.PENALTY_TYPE_DISMISS_CLAN); // 24*60*60*1000 = 86400000
		
		leaderClan.updateClanInDB();
		
		clan.setAllyId(0);
		clan.setAllyName(null);
		clan.setAllyPenaltyExpiryTime(currentTime + Config.ALT_ALLY_JOIN_DAYS_WHEN_DISMISSED * 86400000L, L2Clan.PENALTY_TYPE_CLAN_DISMISSED); // 24*60*60*1000 = 86400000
		clan.changeAllyCrest(0, true);
		clan.updateClanInDB();
		
		player.sendPacket(new SystemMessage(SystemMessageId.YOU_HAVE_EXPELED_A_CLAN));
	}
	
	@Override
	public String getType()
	{
		return "[C] 85 AllyDismiss";
	}
}