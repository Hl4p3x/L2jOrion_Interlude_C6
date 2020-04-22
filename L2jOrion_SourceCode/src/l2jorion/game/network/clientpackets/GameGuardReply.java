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
package l2jorion.game.network.clientpackets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import l2jorion.Config;
import l2jorion.crypt.nProtect;

public class GameGuardReply extends L2GameClientPacket
{
	private final int[] _reply = new int[4];
	private static final Logger LOG = LoggerFactory.getLogger(GameGuardReply.class);
	
	@Override
	protected void readImpl()
	{
		_reply[0] = readD();
		_reply[1] = readD();
		_reply[2] = readD();
		_reply[3] = readD();
	}
	
	@Override
	protected void runImpl()
	{
		if (!nProtect.getInstance().checkGameGuardRepy(getClient(), _reply))
			return;
		
		// L2jOrion cannot be reached with GameGuard: L2Net notification --> Close Client connection
		if (Config.GAMEGUARD_L2NET_CHECK)
		{
			getClient().closeNow();
			LOG.warn("Player with account name " + getClient().accountName + " kicked to use L2Net ");
			return;
		}
		
		getClient().setGameGuardOk(true);
	}
	
	@Override
	public String getType()
	{
		return "[C] CA GameGuardReply";
	}
}