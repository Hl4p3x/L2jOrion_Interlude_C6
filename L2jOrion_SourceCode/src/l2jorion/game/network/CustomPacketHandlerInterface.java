/*
 * $HeadURL: $
 *
 * $Author: $
 * $Date: $
 * $Revision: $
 *
 *
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
package l2jorion.game.network;

import java.nio.ByteBuffer;

import l2jorion.game.network.clientpackets.L2GameClientPacket;

public interface CustomPacketHandlerInterface
{
	
	/**
	 * interface for a custom packethandler to ckeck received packets PacketHandler will take care of the packet if this function returns null.
	 * @param data the packet
	 * @param client the ClientThread
	 * @return a ClientBasePacket if the packet has been processed, null otherwise
	 */
	public L2GameClientPacket handlePacket(ByteBuffer data, L2GameClient client);
}
