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

import l2jorion.util.Point3D;

/**
 * Format: (ch) d[ddddd].
 * @author -Wooden-
 */
public class ExCursedWeaponLocation extends L2GameServerPacket
{
	
	/** The Constant _S__FE_46_EXCURSEDWEAPONLOCATION. */
	private static final String _S__FE_46_EXCURSEDWEAPONLOCATION = "[S] FE:46 ExCursedWeaponLocation";
	
	/** The _cursed weapon info. */
	private final List<CursedWeaponInfo> _cursedWeaponInfo;
	
	/**
	 * Instantiates a new ex cursed weapon location.
	 * @param cursedWeaponInfo the cursed weapon info
	 */
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
	
	/**
	 * Gets the type.
	 * @return the type
	 */
	@Override
	public String getType()
	{
		return _S__FE_46_EXCURSEDWEAPONLOCATION;
	}
	
	/**
	 * The Class CursedWeaponInfo.
	 */
	public static class CursedWeaponInfo
	{
		
		/** The pos. */
		public Point3D pos;
		
		/** The id. */
		public int id;
		
		/** The activated. */
		public int activated; // 0 - not activated ? 1 - activated
		
		/**
		 * Instantiates a new cursed weapon info.
		 * @param p the p
		 * @param ID the iD
		 * @param status the status
		 */
		public CursedWeaponInfo(final Point3D p, final int ID, final int status)
		{
			pos = p;
			id = ID;
			activated = status;
		}
	}
}
