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

public class ChangeWaitType extends PacketServer
{
	private static final String _S__3F_CHANGEWAITTYPE = "[S] 2F ChangeWaitType";
	
	private final int _charObjId;
	private final int _moveType;
	private final int _x, _y, _z;
	
	public static final int WT_SITTING = 0;
	public static final int WT_STANDING = 1;
	public static final int WT_START_FAKEDEATH = 2;
	public static final int WT_STOP_FAKEDEATH = 3;
	
	public ChangeWaitType(final L2Character character, final int newMoveType)
	{
		_charObjId = character.getObjectId();
		_moveType = newMoveType;
		
		_x = character.getX();
		_y = character.getY();
		_z = character.getZ();
	}
	
	@Override
	protected final void writeImpl()
	{
		writeC(0x2f);
		writeD(_charObjId);
		writeD(_moveType);
		writeD(_x);
		writeD(_y);
		writeD(_z);
	}
	
	@Override
	public String getType()
	{
		return _S__3F_CHANGEWAITTYPE;
	}
}
