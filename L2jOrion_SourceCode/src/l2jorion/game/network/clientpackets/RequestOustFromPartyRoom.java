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

import l2jorion.game.datatables.csv.MapRegionTable;
import l2jorion.game.model.L2World;
import l2jorion.game.model.PartyMatchRoom;
import l2jorion.game.model.PartyMatchRoomList;
import l2jorion.game.model.PartyMatchWaitingList;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.ExClosePartyRoom;
import l2jorion.game.network.serverpackets.PartyMatchList;
import l2jorion.game.network.serverpackets.SystemMessage;

public final class RequestOustFromPartyRoom extends L2GameClientPacket
{
	
	private int _charid;
	
	@Override
	protected void readImpl()
	{
		_charid = readD();
	}
	
	@Override
	protected void runImpl()
	{
		
		final L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null)
		{
			return;
		}
		
		final L2PcInstance member = L2World.getInstance().getPlayer(_charid);
		if (member == null)
		{
			return;
		}
		
		final PartyMatchRoom _room = PartyMatchRoomList.getInstance().getPlayerRoom(member);
		if (_room == null)
		{
			return;
		}
		
		if (_room.getOwner() != activeChar)
		{
			return;
		}
		
		if (activeChar.isInParty() && member.isInParty() && activeChar.getParty().getPartyLeaderOID() == member.getParty().getPartyLeaderOID())
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.CANNOT_DISMISS_PARTY_MEMBER));
		}
		else
		{
			_room.deleteMember(member);
			member.setPartyRoom(0);
			
			// Close the PartyRoom window
			member.sendPacket(new ExClosePartyRoom());
			
			// Add player back on waiting list
			PartyMatchWaitingList.getInstance().addPlayer(member);
			
			// Send Room list
			final int _loc = MapRegionTable.getInstance().getMapRegion(member).getBbs();
			member.sendPacket(new PartyMatchList(member, 0, _loc, member.getLevel()));
			
			// Clean player's LFP title
			member.broadcastUserInfo();
			
			member.sendPacket(new SystemMessage(SystemMessageId.OUSTED_FROM_PARTY_ROOM));
		}
		
	}
	
	@Override
	public String getType()
	{
		
		return "[C] D0:01 RequestOustFromPartyRoom";
		
	}
}