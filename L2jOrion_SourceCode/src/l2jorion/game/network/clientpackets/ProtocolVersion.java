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

import l2jorion.Config;
import l2jorion.game.network.PacketClient;
import l2jorion.game.network.PacketServer;
import l2jorion.game.network.serverpackets.KeyPacket;
import l2jorion.game.network.serverpackets.SendStatus;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;

public final class ProtocolVersion extends PacketClient
{
	static Logger LOG = LoggerFactory.getLogger(ProtocolVersion.class);
	
	private int _version;
	
	@Override
	protected void readImpl()
	{
		_version = readH(); // readD(); ?
	}
	
	@Override
	protected void runImpl()
	{
		if (_version == 65534 || _version == -2) // Ping
		{
			getClient().close((PacketServer) null);
		}
		else if (_version == 65533 || _version == -3) // RWHO
		{
			getClient().close(new SendStatus());
		}
		else if (_version < Config.MIN_PROTOCOL_REVISION || _version > Config.MAX_PROTOCOL_REVISION)
		{
			LOG.info("Client: " + getClient() + " -> Protocol Revision: " + _version + " is invalid. Minimum is " + Config.MIN_PROTOCOL_REVISION + " and Maximum is " + Config.MAX_PROTOCOL_REVISION + " are supported. Closing connection.");
			LOG.warn("Wrong Protocol Version " + _version);
			getClient().setProtocolOk(false);
			getClient().sendPacket(new KeyPacket(null, 0));
		}
		else
		{
			getClient().setRevision(_version);
			getClient().setProtocolOk(true);
			getClient().sendPacket(new KeyPacket(getClient().enableCrypt(), 1));
		}
	}
	
	@Override
	public String getType()
	{
		return "[C] 00 ProtocolVersion";
	}
}