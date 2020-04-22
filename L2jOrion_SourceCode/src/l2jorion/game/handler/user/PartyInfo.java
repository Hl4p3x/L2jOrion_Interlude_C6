/*
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
package l2jorion.game.handler.user;

import l2jorion.game.handler.IUserCommandHandler;
import l2jorion.game.model.L2Party;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.SystemMessage;

/**
 * Support for /partyinfo command Added by Tempy - 28 Jul 05
 */
public class PartyInfo implements IUserCommandHandler
{
	private static final int[] COMMAND_IDS =
	{
		81
	};

	@Override
	public boolean useUserCommand(int id, L2PcInstance activeChar)
	{
		if(id != COMMAND_IDS[0])
			return false;

		if(!activeChar.isInParty())
			return false;

		L2Party playerParty = activeChar.getParty();
		int memberCount = playerParty.getMemberCount();
		int lootDistribution = playerParty.getLootDistribution();
		String partyLeader = playerParty.getPartyMembers().get(0).getName();

		playerParty = null;

		activeChar.sendPacket(new SystemMessage(SystemMessageId.PARTY_INFORMATION));

		switch (lootDistribution)
		{
			case L2Party.ITEM_LOOTER:
				activeChar.sendPacket(new SystemMessage(SystemMessageId.LOOTING_FINDERS_KEEPERS));
				break;
			case L2Party.ITEM_ORDER:
				activeChar.sendPacket(new SystemMessage(SystemMessageId.LOOTING_BY_TURN));
				break;
			case L2Party.ITEM_ORDER_SPOIL:
				activeChar.sendPacket(new SystemMessage(SystemMessageId.LOOTING_BY_TURN_INCLUDE_SPOIL));
				break;
			case L2Party.ITEM_RANDOM:
				activeChar.sendPacket(new SystemMessage(SystemMessageId.LOOTING_RANDOM));
				break;
			case L2Party.ITEM_RANDOM_SPOIL:
				activeChar.sendPacket(new SystemMessage(SystemMessageId.LOOTING_RANDOM_INCLUDE_SPOIL));
				break;
		}
 	activeChar.sendPacket(new SystemMessage(SystemMessageId.PARTY_LEADER_S1).addString(partyLeader)); 
 	activeChar.sendMessage("Members: " + memberCount + "/9"); 
 	activeChar.sendPacket(new SystemMessage(SystemMessageId.FRIEND_LIST_FOOT)); 
	return true;
	}

	@Override
	public int[] getUserCommandList()
	{
		return COMMAND_IDS;
	}
}
