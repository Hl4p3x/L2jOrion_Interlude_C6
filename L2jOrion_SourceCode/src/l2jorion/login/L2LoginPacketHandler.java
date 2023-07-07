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
package l2jorion.login;

import java.nio.ByteBuffer;

import l2jorion.Config;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;
import l2jorion.login.L2LoginClient.LoginClientState;
import l2jorion.login.network.clientpackets.AuthGameGuard;
import l2jorion.login.network.clientpackets.RequestAuthLogin;
import l2jorion.login.network.clientpackets.RequestServerList;
import l2jorion.login.network.clientpackets.RequestServerLogin;
import l2jorion.mmocore.IPacketHandler;
import l2jorion.mmocore.ReceivablePacket;

public final class L2LoginPacketHandler implements IPacketHandler<L2LoginClient>
{
	private final Logger LOG = LoggerFactory.getLogger(L2LoginPacketHandler.class);
	
	@Override
	public ReceivablePacket<L2LoginClient> handlePacket(final ByteBuffer buf, final L2LoginClient client)
	{
		final int opcode = buf.get() & 0xFF;
		
		ReceivablePacket<L2LoginClient> packet = null;
		LoginClientState state = client.getState();
		
		if (Config.PACKET_HANDLER_DEBUG)
		{
			LOG.info("Packet: " + Integer.toHexString(opcode) + " on State: " + state.name() + " Client: " + client.toString(), "LoginPacketsLog");
		}
		
		switch (state)
		{
			case CONNECTED:
				if (opcode == 0x07)
				{
					packet = new AuthGameGuard();
				}
				else
				{
					debugOpcode(opcode, state);
				}
				break;
			case AUTHED_GG:
				if (opcode == 0x00)
				{
					packet = new RequestAuthLogin();
				}
				else
				{
					debugOpcode(opcode, state);
				}
				break;
			case AUTHED_LOGIN:
				if (opcode == 0x05)
				{
					packet = new RequestServerList();
				}
				else if (opcode == 0x02)
				{
					packet = new RequestServerLogin();
				}
				else
				{
					debugOpcode(opcode, state);
				}
				break;
		}
		return packet;
	}
	
	private void debugOpcode(final int opcode, final LoginClientState state)
	{
		LOG.warn("Unknown Opcode: " + opcode + " for state: " + state.name());
	}
}