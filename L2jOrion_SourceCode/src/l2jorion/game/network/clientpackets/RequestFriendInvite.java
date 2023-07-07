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
package l2jorion.game.network.clientpackets;

import l2jorion.game.model.BlockList;
import l2jorion.game.model.L2World;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.network.PacketClient;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.AskJoinFriend;
import l2jorion.game.network.serverpackets.SystemMessage;

public final class RequestFriendInvite extends PacketClient
{
	private String _name;
	
	@Override
	protected void readImpl()
	{
		_name = readS();
	}
	
	@Override
	protected void runImpl()
	{
		SystemMessage sm;
		final L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null)
		{
			return;
		}
		
		final L2PcInstance friend = L2World.getInstance().getPlayer(_name);
		
		// can't use friend invite for locating invisible characters
		if (friend == null || friend.isOnline() == 0 || friend.getAppearance().getInvisible())
		{
			// Target is not found in the game.
			activeChar.sendPacket(SystemMessageId.THE_USER_YOU_REQUESTED_IS_NOT_IN_GAME);
			return;
		}
		
		if (friend == activeChar)
		{
			// You cannot add yourself to your own friend list.
			activeChar.sendPacket(SystemMessageId.YOU_CANNOT_ADD_YOURSELF_TO_OWN_FRIEND_LIST);
			return;
		}
		
		if (BlockList.isBlocked(activeChar, friend))
		{
			activeChar.sendMessage("You have blocked " + _name + ".");
			return;
		}
		
		if (BlockList.isBlocked(friend, activeChar))
		{
			activeChar.sendMessage("You are in " + _name + "'s block list.");
			return;
		}
		if (activeChar.getFriendList().contains(friend.getObjectId()))
		{
			// Player already is in your friendlist
			sm = new SystemMessage(SystemMessageId.S1_ALREADY_IN_FRIENDS_LIST);
			sm.addString(_name);
			activeChar.sendPacket(sm);
			return;
		}
		if (!friend.isProcessingRequest())
		{
			// requets to become friend
			activeChar.onTransactionRequest(friend);
			sm = new SystemMessage(SystemMessageId.S1_REQUESTED_TO_BECOME_FRIENDS);
			sm.addString(activeChar.getName());
			AskJoinFriend ajf = new AskJoinFriend(activeChar.getName());
			friend.sendPacket(ajf);
		}
		else
		{
			sm = new SystemMessage(SystemMessageId.S1_IS_BUSY_TRY_LATER);
		}
		
		friend.sendPacket(sm);
	}
	
	@Override
	public String getType()
	{
		return "[C] 5E RequestFriendInvite";
	}
}