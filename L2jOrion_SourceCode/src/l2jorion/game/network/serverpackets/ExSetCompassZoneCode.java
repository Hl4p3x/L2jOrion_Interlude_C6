/* L2jOrion Project - www.l2jorion.com 
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

/**
 * Format: ch d.
 * @author KenM
 */
public class ExSetCompassZoneCode extends L2GameServerPacket
{
	/** The Constant _S__FE_32_EXSETCOMPASSZONECODE. */
	private static final String _S__FE_32_EXSETCOMPASSZONECODE = "[S] FE:32 ExSetCompassZoneCode";
	
	/** The Constant SIEGEWARZONE1. */
	public static final int SIEGEWARZONE1 = 0x0A;
	
	/** The Constant SIEGEWARZONE2. */
	public static final int SIEGEWARZONE2 = 0x0B;
	
	/** The Constant PEACEZONE. */
	public static final int PEACEZONE = 0x0C;
	
	/** The Constant SEVENSIGNSZONE. */
	public static final int SEVENSIGNSZONE = 0x0D;
	
	/** The Constant PVPZONE. */
	public static final int PVPZONE = 0x0E;
	
	/** The Constant GENERALZONE. */
	public static final int GENERALZONE = 0x0F;
	
	/** The _zone type. */
	private final int _zoneType;
	
	/**
	 * Instantiates a new ex set compass zone code.
	 * @param val the val
	 */
	public ExSetCompassZoneCode(final int val)
	{
		_zoneType = val;
	}
	
	@Override
	protected void writeImpl()
	{
		writeC(0xFE);
		writeH(0x32);
		writeD(_zoneType);
	}
	
	/**
	 * Gets the type.
	 * @return the type
	 */
	@Override
	public String getType()
	{
		return _S__FE_32_EXSETCOMPASSZONECODE;
	}
}
