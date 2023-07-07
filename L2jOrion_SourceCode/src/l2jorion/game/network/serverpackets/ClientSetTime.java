/*
 * L2jOrion Project - www.l2jorion.com 
 * 
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package l2jorion.game.network.serverpackets;

import l2jorion.game.controllers.GameTimeController;
import l2jorion.game.network.PacketServer;

public class ClientSetTime extends PacketServer
{
	private static final String _S__EC_CLIENTSETTIME = "[S] f2 ClientSetTime";
	
	@Override
	protected final void writeImpl()
	{
		writeC(0xEC);
		writeD(GameTimeController.getInstance().getGameTime()); // time in client minutes
		writeD(6); // constant to match the server time( this determines the speed of the client clock)
	}
	
	@Override
	public String getType()
	{
		return _S__EC_CLIENTSETTIME;
	}
}
