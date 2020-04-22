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
package l2jorion.game.network.serverpackets;

import javolution.util.FastList;
import l2jorion.game.model.PartyMatchRoom;
import l2jorion.game.model.PartyMatchRoomList;
import l2jorion.game.model.actor.instance.L2PcInstance;

public class PartyMatchList extends L2GameServerPacket
{
	private final L2PcInstance _cha;
	private final int _loc;
	private final int _lim;
	private final FastList<PartyMatchRoom> _rooms;
	
	public PartyMatchList(final L2PcInstance player, final int auto, final int location, final int limit)
	{
		_cha = player;
		_loc = location;
		_lim = limit;
		_rooms = new FastList<>();
	}
	
	@Override
	protected final void writeImpl()
	{
		if (getClient().getActiveChar() == null)
			return;
		
		for (final PartyMatchRoom room : PartyMatchRoomList.getInstance().getRooms())
		{
			if (room.getMembers() < 1 || room.getOwner() == null || room.getOwner().isOnline() == 0 || room.getOwner().getPartyRoom() != room.getId())
			{
				PartyMatchRoomList.getInstance().deleteRoom(room.getId());
				continue;
			}
			
			if (_loc > 0 && _loc != room.getLocation())
				continue;
			
			if (_lim == 0 && ((_cha.getLevel() < room.getMinLvl()) || (_cha.getLevel() > room.getMaxLvl())))
				continue;
			
			_rooms.add(room);
		}
		
		int count = 0;
		final int size = _rooms.size();
		
		writeC(0x96);
		if (size > 0)
			writeD(1);
		else
			writeD(0);
		
		writeD(_rooms.size());
		while (size > count)
		{
			writeD(_rooms.get(count).getId());
			writeS(_rooms.get(count).getTitle());
			writeD(_rooms.get(count).getLocation());
			writeD(_rooms.get(count).getMinLvl());
			writeD(_rooms.get(count).getMaxLvl());
			writeD(_rooms.get(count).getMembers());
			writeD(_rooms.get(count).getMaxMembers());
			writeS(_rooms.get(count).getOwner().getName());
			count++;
		}
	}
	
	@Override
	public String getType()
	{
		return "[S] 96 PartyMatchList";
	}
}