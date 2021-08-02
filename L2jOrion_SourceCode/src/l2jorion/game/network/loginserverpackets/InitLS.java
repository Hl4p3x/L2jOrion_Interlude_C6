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
package l2jorion.game.network.loginserverpackets;

public class InitLS extends LoginServerBasePacket
{
	private final int _rev;
	private final byte[] _key;
	
	public int getRevision()
	{
		return _rev;
	}
	
	public byte[] getRSAKey()
	{
		return _key;
	}
	
	public InitLS(final byte[] decrypt)
	{
		super(decrypt);
		_rev = readD();
		final int size = readD();
		_key = readB(size);
	}
	
}
