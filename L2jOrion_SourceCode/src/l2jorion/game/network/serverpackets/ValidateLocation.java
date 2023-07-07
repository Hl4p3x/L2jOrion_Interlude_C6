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

import l2jorion.game.model.L2Character;
import l2jorion.game.network.PacketServer;

public class ValidateLocation extends PacketServer
{
	private static final String _S__76_SETTOLOCATION = "[S] 61 ValidateLocation";
	
	private int _charObjId;
	private int _x, _y, _z, _heading;
	
	public ValidateLocation(L2Character cha)
	{
		_charObjId = cha.getObjectId();
		_x = cha.getX();
		_y = cha.getY();
		_z = cha.getZ();
		_heading = cha.getHeading();
	}
	
	public ValidateLocation(L2Character cha, boolean client)
	{
		_charObjId = cha.getObjectId();
		_x = cha.getClientX();
		_y = cha.getClientY();
		_z = cha.getClientZ();
		_heading = cha.getClientHeading();
	}
	
	@Override
	protected final void writeImpl()
	{
		writeC(0x61);
		writeD(_charObjId);
		writeD(_x);
		writeD(_y);
		writeD(_z);
		writeD(_heading);
	}
	
	@Override
	public String getType()
	{
		return _S__76_SETTOLOCATION;
	}
}