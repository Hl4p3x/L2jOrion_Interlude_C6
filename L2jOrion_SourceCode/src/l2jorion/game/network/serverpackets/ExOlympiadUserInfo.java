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

public class ExOlympiadUserInfo extends PacketServer
{
	private static final String _S__FE_29_OLYMPIADUSERINFO = "[S] FE:29 ExOlympiadUserInfo";
	
	private int _side;
	private int _objectId;
	private String _name;
	private int _classId;
	private int _curHp;
	private int _maxHp;
	private int _curCp;
	private int _maxCp;
	
	public ExOlympiadUserInfo(L2PcInstance player)
	{
		_side = player.getOlympiadSide();
		_objectId = player.getObjectId();
		_name = player.getName();
		_classId = player.getClassId().getId();
		_curHp = (int) player.getCurrentHp();
		_maxHp = player.getMaxHp();
		_curCp = (int) player.getCurrentCp();
		_maxCp = player.getMaxCp();
	}
	
	@Override
	protected final void writeImpl()
	{
		writeC(0xfe);
		writeH(0x29);
		writeC(_side);
		writeD(_objectId);
		writeS(_name);
		writeD(_classId);
		writeD(_curHp);
		writeD(_maxHp);
		writeD(_curCp);
		writeD(_maxCp);
	}
	
	@Override
	public String getType()
	{
		return _S__FE_29_OLYMPIADUSERINFO;
	}
}
