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

import l2jorion.game.model.actor.instance.L2DoorInstance;
import l2jorion.game.network.PacketServer;

public class DoorInfo extends PacketServer
{
	private static final String _S__60_DOORINFO = "[S] 4c DoorInfo";
	
	private final int _staticObjectId;
	private final int _objectId;
	private final boolean _isTargetable;
	private final boolean _isClosed;
	private final int _maxHp;
	private final int _currentHp;
	private final boolean _showHp;
	private final int _damageGrade;
	
	public DoorInfo(L2DoorInstance door)
	{
		_staticObjectId = door.getDoorId();
		_objectId = door.getObjectId();
		_isTargetable = door.isTargetable();
		_isClosed = !door.getOpen();
		_maxHp = door.getMaxHp();
		_currentHp = (int) door.getCurrentHp();
		_showHp = door.isEnemy();
		_damageGrade = door.getDamage();
	}
	
	@Override
	protected final void writeImpl()
	{
		writeC(0x4c);
		writeD(_objectId);
		writeD(_staticObjectId);
		writeD((_showHp) ? 1 : 0);
		writeD(_isTargetable ? 1 : 0);
		writeD(_isClosed ? 0 : 1);
		writeD(_maxHp);
		writeD(_currentHp);
		writeD(0);
		writeD(_damageGrade);
	}
	
	@Override
	public String getType()
	{
		return _S__60_DOORINFO;
	}
}