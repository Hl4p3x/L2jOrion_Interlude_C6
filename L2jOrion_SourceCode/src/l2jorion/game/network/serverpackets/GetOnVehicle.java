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

import l2jorion.game.model.Location;
import l2jorion.game.network.PacketServer;

public class GetOnVehicle extends PacketServer
{
	private final int _charObjId;
	private final int _boatObjId;
	private final Location _pos;
	
	public GetOnVehicle(int charObjId, int boatObjId, Location point3d)
	{
		_charObjId = charObjId;
		_boatObjId = boatObjId;
		_pos = point3d;
	}
	
	@Override
	protected void writeImpl()
	{
		writeC(0x5C);
		writeD(_charObjId);
		writeD(_boatObjId);
		writeD(_pos.getX());
		writeD(_pos.getY());
		writeD(_pos.getZ());
	}
	
	@Override
	public String getType()
	{
		return "[S] 5C GetOnVehicle";
	}
	
}
