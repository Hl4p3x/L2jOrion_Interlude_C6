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

import l2jorion.game.model.L2Object;
import l2jorion.game.network.PacketServer;

public class TeleportToLocation extends PacketServer
{
	private static final String _S__38_TELEPORTTOLOCATION = "[S] 28 TeleportToLocation";
	
	private final int _targetObjId;
	private final int _x;
	private final int _y;
	private final int _z;
	private boolean _isFastTeleport;
	
	public TeleportToLocation(L2Object obj, int x, int y, int z, int heading, boolean isFastTeleport)
	{
		_targetObjId = obj.getObjectId();
		_x = x;
		_y = y;
		_z = z;
		_isFastTeleport = isFastTeleport;
	}
	
	@Override
	protected final void writeImpl()
	{
		writeC(0x28);
		
		writeD(_targetObjId);
		writeD(_x);
		writeD(_y);
		writeD(_z);
		writeD(_isFastTeleport ? 1 : 0); // 0 - black screen, 1 - no black screen
	}
	
	@Override
	public String getType()
	{
		return _S__38_TELEPORTTOLOCATION;
	}
}
