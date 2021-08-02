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

import java.nio.BufferUnderflowException;

import org.strixplatform.StrixPlatform;
import org.strixplatform.managers.ClientGameSessionManager;
import org.strixplatform.managers.ClientProtocolDataManager;
import org.strixplatform.utils.StrixClientData;

import l2jguard.HwidConfig;
import l2jguard.Protection;
import l2jorion.Config;
import l2jorion.game.network.serverpackets.KeyPacket;
import l2jorion.game.network.serverpackets.L2GameServerPacket;
import l2jorion.game.network.serverpackets.SendStatus;
import l2jorion.log.Log;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;

public final class ProtocolVersion extends L2GameClientPacket
{
	static Logger LOG = LoggerFactory.getLogger(ProtocolVersion.class);
	
	private int _version;
	
	private byte _data[];
	
	private String _hwidHdd = "NoHWID-HD";
	private String _hwidMac = "NoHWID-MAC";
	private String _hwidCPU = "NoHWID-CPU";
	
	private byte[] data;
	private int dataChecksum;
	
	@Override
	protected void readImpl()
	{
		try
		{
			_version = readD();
		}
		catch (BufferUnderflowException e)
		{
		}
		
		if (Config.L2JGUARD_PROTECTION)
		{
			if (Protection.isProtectionOn())
			{
				if (_buf.remaining() > 260)
				{
					_data = new byte[260];
					readB(_data);
					_hwidHdd = readS();
					_hwidMac = readS();
					_hwidCPU = readS();
				}
			}
			// else if (Protection.isProtectionOn())
			// {
			// getClient().close(new KeyPacket(getClient().enableCrypt()));
			// }
		}
		
		if (Config.STRIX_PROTECTION)
		{
			if (StrixPlatform.getInstance().isPlatformEnabled())
			{
				try
				{
					if (_buf.remaining() >= StrixPlatform.getInstance().getProtocolVersionDataSize())
					{
						data = new byte[StrixPlatform.getInstance().getClientDataSize()];
						readB(data);
						dataChecksum = readD();
					}
				}
				catch (final Exception e)
				{
					getClient().close(new KeyPacket(null));
					LOG.error("Client [IP=" + toString() + "] used unprotected client. Disconnect...");
					return;
				}
			}
		}
	}
	
	@Override
	protected void runImpl()
	{
		if (_version == 65534 || _version == -2) // Ping
		{
			getClient().close((L2GameServerPacket) null);
		}
		else if (_version == 65533 || _version == -3) // RWHO
		{
			getClient().close(new SendStatus());
		}
		else if (_version < Config.MIN_PROTOCOL_REVISION || _version > Config.MAX_PROTOCOL_REVISION)
		{
			String text = "Client: " + getClient().toString() + " -> Protocol Revision: " + _version + " is invalid. Minimum is " + Config.MIN_PROTOCOL_REVISION + " and Maximum is " + Config.MAX_PROTOCOL_REVISION + " are supported. Closing connection.";
			String text2 = "Wrong Protocol Version " + _version;
			Log.add(text, "WrongProtocolVersion");
			Log.add(text2, "WrongProtocolVersion");
			
			getClient().close((L2GameServerPacket) null);
		}
		else
		{
			if (Config.L2JGUARD_PROTECTION)
			{
				getClient().setRevision(_version);
				if (Protection.isProtectionOn())
				{
					if (_hwidHdd.equals("NoGuard") && _hwidMac.equals("NoGuard") && _hwidCPU.equals("NoGuard"))
					{
						LOG.info("HWID Status: No Client side dlls");
						getClient().close(new KeyPacket(getClient().enableCrypt()));
					}
					
					switch (HwidConfig.GET_CLIENT_HWID)
					{
						case 1:
							getClient().setHWID(_hwidHdd);
							break;
						case 2:
							getClient().setHWID(_hwidMac);
							break;
						case 3:
							getClient().setHWID(_hwidCPU);
							break;
					}
				}
			}
			
			if (Config.STRIX_PROTECTION)
			{
				if (data == null)
				{
					// getClient().close(new KeyPacket(null));
					getClient().close((L2GameServerPacket) null);
					LOG.error("Client [IP=" + getClient().toString() + "] used unprotected client. Disconnect...");
					return;
				}
				
				final StrixClientData clientData = ClientProtocolDataManager.getInstance().getDecodedData(data, dataChecksum);
				if (clientData != null)
				{
					if (!ClientGameSessionManager.getInstance().checkServerResponse(clientData))
					{
						getClient().close(new KeyPacket(null, clientData));
						return;
					}
					
					getClient().setStrixClientData(clientData);
					getClient().setRevision(_version);
					sendPacket(new KeyPacket(getClient().enableCrypt()));
					return;
				}
				
				LOG.error("Decode client data failed. See Strix-Platform log file. Disconnected client " + getClient().toString());
				// getClient().close(new KeyPacket(null));
				getClient().close((L2GameServerPacket) null);
				return;
			}
			
			getClient().sendPacket(new KeyPacket(getClient().enableCrypt()));
		}
	}
	
	@Override
	public String getType()
	{
		return "[C] 00 ProtocolVersion";
	}
}