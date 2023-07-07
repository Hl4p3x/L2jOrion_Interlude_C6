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

import l2jorion.game.network.PacketServer;

public class SpecialCamera extends PacketServer
{
	private static final String _S__C7_SPECIALCAMERA = "[S] C7 SpecialCamera";
	
	private final int _id;
	private final int _dist;
	private final int _yaw;
	private final int _pitch;
	private final int _time;
	private final int _duration;
	
	public SpecialCamera(final int id, final int dist, final int yaw, final int pitch, final int time, final int duration)
	{
		_id = id;
		_dist = dist;
		_yaw = yaw;
		_pitch = pitch;
		_time = time;
		_duration = duration;
	}
	
	@Override
	public void writeImpl()
	{
		writeC(0xc7);
		writeD(_id);
		writeD(_dist);
		writeD(_yaw);
		writeD(_pitch);
		writeD(_time);
		writeD(_duration);
	}
	
	@Override
	public String getType()
	{
		return _S__C7_SPECIALCAMERA;
	}
}
