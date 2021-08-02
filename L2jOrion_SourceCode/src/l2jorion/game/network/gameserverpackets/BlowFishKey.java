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
package l2jorion.game.network.gameserverpackets;

import java.security.GeneralSecurityException;
import java.security.interfaces.RSAPublicKey;

import javax.crypto.Cipher;

import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;

public class BlowFishKey extends GameServerBasePacket
{
	private static Logger LOG = LoggerFactory.getLogger(BlowFishKey.class);
	
	public BlowFishKey(final byte[] blowfishKey, final RSAPublicKey publicKey)
	{
		writeC(0x00);
		try
		{
			final Cipher rsaCipher = Cipher.getInstance("RSA/ECB/nopadding");
			rsaCipher.init(Cipher.ENCRYPT_MODE, publicKey);
			final byte[] encrypted = rsaCipher.doFinal(blowfishKey);
			writeD(encrypted.length);
			writeB(encrypted);
		}
		catch (final GeneralSecurityException e)
		{
			LOG.warn("Error While encrypting blowfish key for transmision (Crypt error)");
			e.printStackTrace();
		}
		
	}
	
	@Override
	public byte[] getContent()
	{
		return getBytes();
	}
}
