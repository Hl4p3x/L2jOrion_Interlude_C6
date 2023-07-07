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

import java.util.List;

import l2jorion.game.model.Location;
import l2jorion.game.network.PacketServer;

public class ExCursedWeaponLocation extends PacketServer
{
	private static final String _S__FE_46_EXCURSEDWEAPONLOCATION = "[S] FE:46 ExCursedWeaponLocation";
	
	private final List<CursedWeaponInfo> _cursedWeaponInfo;
	
	public ExCursedWeaponLocation(final List<CursedWeaponInfo> cursedWeaponInfo)
	{
		_cursedWeaponInfo = cursedWeaponInfo;
	}
	
	@Override
	protected void writeImpl()
	{
		writeC(0xfe);
		writeH(0x46);
		
		if (!_cursedWeaponInfo.isEmpty())
		{
			writeD(_cursedWeaponInfo.size());
			for (final CursedWeaponInfo w : _cursedWeaponInfo)
			{
				writeD(w.id);
				writeD(w.activated);
				
				writeD(w.pos.getX());
				writeD(w.pos.getY());
				writeD(w.pos.getZ());
			}
		}
		else
		{
			writeD(0);
			writeD(0);
		}
	}
	
	public static class CursedWeaponInfo
	{
		public Location pos;
		public int id;
		public int activated; // 0 - not activated ? 1 - activated
		
		public CursedWeaponInfo(final Location p, final int ID, final int status)
		{
			pos = p;
			id = ID;
			activated = status;
		}
	}
	
	@Override
	public String getType()
	{
		return _S__FE_46_EXCURSEDWEAPONLOCATION;
	}
	
}
