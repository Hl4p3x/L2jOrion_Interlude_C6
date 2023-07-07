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

import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.network.PacketServer;

public class MoveOnVehicle extends PacketServer
{
	private static final String _S__71_MOVEONVEICLE = "[S] 71 MoveOnVehicle";
	
	private final int _id;
	private final int _x, _y, _z;
	private final L2PcInstance _activeChar;
	
	public MoveOnVehicle(final int vehicleID, final L2PcInstance player, final int x, final int y, final int z)
	{
		_id = vehicleID;
		_activeChar = player;
		_x = x;
		_y = y;
		_z = z;
	}
	
	@Override
	protected final void writeImpl()
	{
		writeC(0x71);
		
		writeD(_activeChar.getObjectId());
		writeD(_id);
		writeD(_x);
		writeD(_y);
		writeD(_z);
		writeD(_activeChar.getX());
		writeD(_activeChar.getY());
		writeD(_activeChar.getZ());
	}
	
	@Override
	public String getType()
	{
		return _S__71_MOVEONVEICLE;
	}
}
