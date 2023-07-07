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
import l2jorion.game.model.L2Clan;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.network.PacketClient;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.PledgeShowMemberListDelete;
import l2jorion.game.network.serverpackets.SystemMessage;

public final class RequestWithdrawalPledge extends PacketClient
{
	@Override
	protected void readImpl()
	{
		
	}
	
	@Override
	protected void runImpl()
	{
		final L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null)
		{
			return;
		}
		
		if (activeChar.getClan() == null)
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.YOU_ARE_NOT_A_CLAN_MEMBER));
			return;
		}
		
		if (activeChar.isClanLeader())
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.CLAN_LEADER_CANNOT_WITHDRAW));
			return;
		}
		
		if (activeChar.isInCombat())
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.YOU_CANNOT_LEAVE_DURING_COMBAT));
			return;
		}
		
		final L2Clan clan = activeChar.getClan();
		
		clan.removeClanMember(activeChar.getName(), System.currentTimeMillis() + Config.ALT_CLAN_JOIN_DAYS * 86400000L); // 24*60*60*1000 = 86400000
		
		SystemMessage sm = new SystemMessage(SystemMessageId.S1_HAS_WITHDRAWN_FROM_THE_CLAN);
		sm.addString(activeChar.getName());
		clan.broadcastToOnlineMembers(sm);
		
		// Remove the Player From the Member list
		clan.broadcastToOnlineMembers(new PledgeShowMemberListDelete(activeChar.getName()));
		
		activeChar.sendPacket(new SystemMessage(SystemMessageId.YOU_HAVE_WITHDRAWN_FROM_CLAN));
		activeChar.sendPacket(new SystemMessage(SystemMessageId.YOU_MUST_WAIT_BEFORE_JOINING_ANOTHER_CLAN));
		activeChar.setActiveWarehouse(null);
	}
	
	@Override
	public String getType()
	{
		return "[C] 26 RequestWithdrawalPledge";
	}
}