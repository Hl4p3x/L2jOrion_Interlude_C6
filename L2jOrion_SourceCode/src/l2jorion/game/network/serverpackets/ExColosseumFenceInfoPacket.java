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

package l2jorion.game.network.serverpackets;

import l2jorion.game.model.actor.instance.L2FenceInstance;
import l2jorion.game.network.PacketServer;

public class ExColosseumFenceInfoPacket extends PacketServer
{
	private static final String _S__FE_03_EXCOLOSSEUMFENCEINFOPACKET = "[S] FE:03 ExColosseumFenceInfoPacket";
	private L2FenceInstance _fence;
	
	public ExColosseumFenceInfoPacket(L2FenceInstance fence)
	{
		_fence = fence;
	}
	
	@Override
	protected void writeImpl()
	{
		writeC(0xfe);
		writeH(0x09);
		
		writeD(_fence.getObjectId());
		writeD(_fence.getType());
		writeD(_fence.getX());
		writeD(_fence.getY());
		writeD(_fence.getZ());
		writeD(_fence.getWidth());
		writeD(_fence.getLength());
	}
	
	@Override
	public String getType()
	{
		return _S__FE_03_EXCOLOSSEUMFENCEINFOPACKET;
	}
}