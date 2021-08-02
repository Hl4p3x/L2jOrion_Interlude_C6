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

public class AuthRequest extends GameServerBasePacket
{
	public AuthRequest(final int id, final boolean acceptAlternate, final byte[] hexid, final String externalHost, final String internalHost, final int port, final boolean reserveHost, final int maxplayer)
	{
		writeC(0x01);
		writeC(id);
		writeC(acceptAlternate ? 0x01 : 0x00);
		writeC(reserveHost ? 0x01 : 0x00);
		writeS(externalHost);
		writeS(internalHost);
		writeH(port);
		writeD(maxplayer);
		writeD(hexid.length);
		writeB(hexid);
	}
	
	@Override
	public byte[] getContent()
	{
		return getBytes();
	}
}
