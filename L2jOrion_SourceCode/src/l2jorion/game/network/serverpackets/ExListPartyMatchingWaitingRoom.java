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

import javolution.util.FastList;
import l2jorion.game.model.PartyMatchRoom;
import l2jorion.game.model.PartyMatchRoomList;
import l2jorion.game.model.PartyMatchWaitingList;
import l2jorion.game.model.actor.instance.L2PcInstance;

public class ExListPartyMatchingWaitingRoom extends L2GameServerPacket
{
	private final L2PcInstance _activeChar;
	
	@SuppressWarnings("unused")
	private final int _page;
	private final int _minlvl;
	private final int _maxlvl;
	private final int _mode;
	private final FastList<L2PcInstance> _members;
	
	public ExListPartyMatchingWaitingRoom(final L2PcInstance player, final int page, final int minlvl, final int maxlvl, final int mode)
	{
		_activeChar = player;
		_page = page;
		_minlvl = minlvl;
		_maxlvl = maxlvl;
		_mode = mode;
		_members = new FastList<>();
	}
	
	@Override
	protected void writeImpl()
	{
		writeC(0xfe);
		writeH(0x35);
		
		// If the mode is 0 and the activeChar isn't the PartyRoom leader, return an empty list.
		if (_mode == 0)
		{
			// Retrieve the activeChar PartyMatchRoom
			final PartyMatchRoom _room = PartyMatchRoomList.getInstance().getRoom(_activeChar.getPartyRoom());
			if (_room != null && _room.getOwner() != null && !_room.getOwner().equals(_activeChar))
			{
				writeD(0);
				writeD(0);
				return;
			}
		}
		
		for (final L2PcInstance cha : PartyMatchWaitingList.getInstance().getPlayers())
		{
			// Don't add yourself in the list
			if (cha == null || cha == _activeChar)
			{
				continue;
			}
			
			if (!cha.isPartyWaiting())
			{
				PartyMatchWaitingList.getInstance().removePlayer(cha);
				continue;
			}
			
			if ((cha.getLevel() < _minlvl) || (cha.getLevel() > _maxlvl))
			{
				continue;
			}
			
			_members.add(cha);
		}
		
		int _count = 0;
		final int _size = _members.size();
		
		writeD(1);
		writeD(_size);
		while (_size > _count)
		{
			writeS(_members.get(_count).getName());
			writeD(_members.get(_count).getActiveClass());
			writeD(_members.get(_count).getLevel());
			_count++;
		}
	}
	
	@Override
	public String getType()
	{
		return "[S] FE:35 ExListPartyMatchingWaitingRoom";
	}
}