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

import l2jorion.game.model.L2CommandChannel;
import l2jorion.game.model.L2Party;
import l2jorion.game.network.PacketServer;

public class ExMultiPartyCommandChannelInfo extends PacketServer
{
	private final L2CommandChannel _channel;
	
	public ExMultiPartyCommandChannelInfo(final L2CommandChannel channel)
	{
		_channel = channel;
	}
	
	@Override
	protected void writeImpl()
	{
		if (_channel == null)
		{
			return;
		}
		
		writeC(0xfe);
		writeH(0x30);
		
		writeS(_channel.getChannelLeader().getName());
		writeD(0); // Channel loot
		writeD(_channel.getMemberCount());
		
		writeD(_channel.getPartys().size());
		for (final L2Party p : _channel.getPartys())
		{
			writeS(p.getLeader().getName());
			writeD(p.getPartyLeaderOID());
			writeD(p.getMemberCount());
		}
	}
	
	@Override
	public String getType()
	{
		return "[S] FE:30 ExMultiPartyCommandChannelInfo";
	}
}