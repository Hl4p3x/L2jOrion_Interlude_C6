/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package l2jorion.game.network.serverpackets;

import l2jorion.game.datatables.sql.CharNameTable;
import l2jorion.game.model.L2World;
import l2jorion.game.network.PacketServer;

public class FriendStatus extends PacketServer
{
	private final boolean _online;
	private final int _objid;
	private final String _name;
	
	public FriendStatus(int objId)
	{
		_objid = objId;
		_name = CharNameTable.getInstance().getNameById(objId);
		_online = L2World.getInstance().getPlayer(objId) != null;
	}
	
	@Override
	protected final void writeImpl()
	{
		writeC(0x7b);
		writeD(_online ? 1 : 0);
		writeS(_name);
		writeD(_objid);
	}
	
	@Override
	public String getType()
	{
		return "[C] 5E FriendStatus";
	}
}