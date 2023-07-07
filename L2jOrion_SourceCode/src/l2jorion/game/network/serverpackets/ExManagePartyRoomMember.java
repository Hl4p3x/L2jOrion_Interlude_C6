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

import l2jorion.game.model.PartyMatchRoom;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.network.PacketServer;

public class ExManagePartyRoomMember extends PacketServer
{
	private final L2PcInstance _activeChar;
	private final PartyMatchRoom _room;
	private final int _mode;
	
	public ExManagePartyRoomMember(final L2PcInstance player, final PartyMatchRoom room, final int mode)
	{
		_activeChar = player;
		_room = room;
		_mode = mode;
	}
	
	@Override
	protected void writeImpl()
	{
		writeC(0xfe);
		writeH(0x10);
		writeD(_mode);
		writeD(_activeChar.getObjectId());
		writeS(_activeChar.getName());
		writeD(_activeChar.getActiveClass());
		writeD(_activeChar.getLevel());
		writeD(_room.getLocation());
		if (_room.getOwner().equals(_activeChar))
		{
			writeD(1);
		}
		else
		{
			if ((_room.getOwner().isInParty() && _activeChar.isInParty()) && (_room.getOwner().getParty().getPartyLeaderOID() == _activeChar.getParty().getPartyLeaderOID()))
			{
				writeD(2);
			}
			else
			{
				writeD(0);
			}
		}
	}
	
	@Override
	public String getType()
	{
		return "[S] FE:10 ExManagePartyRoomMember";
	}
}