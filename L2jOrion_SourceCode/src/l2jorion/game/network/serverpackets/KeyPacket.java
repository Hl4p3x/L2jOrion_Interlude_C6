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

import org.strixplatform.StrixPlatform;
import org.strixplatform.utils.StrixClientData;

import l2jorion.Config;

public final class KeyPacket extends L2GameServerPacket
{
	private static final String _S__01_KEYPACKET = "[S] 01 KeyPacket";
	
	private byte[] _key;
	private final StrixClientData _clientData;
	
	public KeyPacket(final byte[] key)
	{
		_key = key;
		_clientData = null;
	}
	
	public KeyPacket(final byte[] key, final StrixClientData clientData)
	{
		_key = key;
		_clientData = clientData;
		
		if (_key == null)
		{
			// just to fix null
			LOG.info("New fake key sent");
			_key = new byte[260];
		}
	}
	
	@Override
	public void writeImpl()
	{
		writeC(0x00);
		
		if (Config.STRIX_PROTECTION)
		{
			if (StrixPlatform.getInstance().isBackNotificationEnabled() && _clientData != null)
			{
				writeC(_clientData.getServerResponse().ordinal());
			}
		}
		
		writeC(0x01);
		writeB(_key);
		writeD(0x01);
		writeD(0x01);
	}
	
	@Override
	public String getType()
	{
		return _S__01_KEYPACKET;
	}
	
}
