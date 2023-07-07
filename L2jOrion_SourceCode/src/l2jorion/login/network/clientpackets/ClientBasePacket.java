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
package l2jorion.login.network.clientpackets;

public abstract class ClientBasePacket
{
	private final byte[] _decrypt;
	private int _off;
	
	public ClientBasePacket(final byte[] decrypt)
	{
		_decrypt = decrypt;
		_off = 1; // skip packet type id
	}
	
	public int readD()
	{
		int result = _decrypt[_off++] & 0xff;
		result |= _decrypt[_off++] << 8 & 0xff00;
		result |= _decrypt[_off++] << 0x10 & 0xff0000;
		result |= _decrypt[_off++] << 0x18 & 0xff000000;
		return result;
	}
	
	public int readC()
	{
		final int result = _decrypt[_off++] & 0xff;
		return result;
	}
	
	public int readH()
	{
		int result = _decrypt[_off++] & 0xff;
		result |= _decrypt[_off++] << 8 & 0xff00;
		return result;
	}
	
	public double readF()
	{
		long result = _decrypt[_off++] & 0xff;
		result |= _decrypt[_off++] << 8 & 0xff00;
		result |= _decrypt[_off++] << 0x10 & 0xff0000;
		result |= _decrypt[_off++] << 0x18 & 0xff000000;
		result |= _decrypt[_off++] << 0x20 & 0xff00000000L;
		result |= _decrypt[_off++] << 0x28 & 0xff0000000000L;
		result |= _decrypt[_off++] << 0x30 & 0xff000000000000L;
		result |= _decrypt[_off++] << 0x38 & 0xff00000000000000L;
		return Double.longBitsToDouble(result);
	}
	
	public String readS()
	{
		String result = null;
		try
		{
			result = new String(_decrypt, _off, _decrypt.length - _off, "UTF-16LE");
			result = result.substring(0, result.indexOf(0x00));
			_off += result.length() * 2 + 2;
		}
		catch (final Exception e)
		{
			e.printStackTrace();
		}
		return result;
	}
	
	public final byte[] readB(final int length)
	{
		final byte[] result = new byte[length];
		for (int i = 0; i < length; i++)
		{
			result[i] = _decrypt[_off + i];
		}
		_off += length;
		return result;
	}
}
