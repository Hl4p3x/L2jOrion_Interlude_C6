/*
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

import l2jorion.game.datatables.sql.CharNameTable;
import l2jorion.game.model.L2World;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.network.PacketClient;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.SystemMessage;

public final class RequestFriendList extends PacketClient
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
		
		SystemMessage sm;
		
		activeChar.sendPacket(SystemMessageId.FRIEND_LIST_HEAD);
		
		L2PcInstance friend = null;
		for (int id : activeChar.getFriendList())
		{
			String friendName = CharNameTable.getInstance().getNameById(id);
			if (friendName == null)
			{
				continue;
			}
			
			friend = L2World.getInstance().getPlayer(friendName);
			
			// Currently offline
			if (friend == null || friend.isOnline() == 0)
			{
				sm = SystemMessage.getSystemMessage(SystemMessageId.S1_OFFLINE).addString(friendName);
			}
			// Currently online
			else
			{
				sm = SystemMessage.getSystemMessage(SystemMessageId.S1_ONLINE).addString(friendName);
			}
			activeChar.sendPacket(sm);
		}
		activeChar.sendPacket(SystemMessageId.FRIEND_LIST_FOOT);
	}
	
	@Override
	public String getType()
	{
		return "[C] 60 RequestFriendList";
	}
}