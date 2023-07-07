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

import l2jorion.game.model.actor.instance.L2BoatInstance;
import l2jorion.game.network.PacketServer;

public class OnVehicleCheckLocation extends PacketServer
{
	private final L2BoatInstance _boat;
	private final int _x;
	private final int _y;
	private final int _z;
	
	/**
	 * @param instance
	 * @param x
	 * @param y
	 * @param z
	 */
	public OnVehicleCheckLocation(final L2BoatInstance instance, final int x, final int y, final int z)
	{
		_boat = instance;
		_x = x;
		_y = y;
		_z = z;
	}
	
	@Override
	protected void writeImpl()
	{
		
		writeC(0x5b);
		writeD(_boat.getObjectId());
		writeD(_x);
		writeD(_y);
		writeD(_z);
		writeD(_boat.getPosition().getHeading());
	}
	
	@Override
	public String getType()
	{
		return null;
	}
	
}
