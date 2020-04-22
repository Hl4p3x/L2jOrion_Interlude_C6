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
package l2jorion.game.network;

import java.util.Date;
import java.util.Map;

import javolution.xml.XMLFormat;
import javolution.xml.stream.XMLStreamException;

class PacketHistory
{
	protected Map<Class<?>, Long> _info;
	protected long _timeStamp;
	
	protected static final XMLFormat<PacketHistory> PACKET_HISTORY_XML = new XMLFormat<PacketHistory>(PacketHistory.class)
	{
		@Override
		public void read(final InputElement xml, final PacketHistory packetHistory) throws XMLStreamException
		{
			packetHistory._timeStamp = xml.getAttribute("time-stamp", 0);
			packetHistory._info = xml.<Map<Class<?>, Long>> get("info");
		}
		
		@Override
		public void write(final PacketHistory packetHistory, final OutputElement xml) throws XMLStreamException
		{
			xml.setAttribute("time-stamp", new Date(packetHistory._timeStamp).toString());
			
			for (final Class<?> cls : packetHistory._info.keySet())
			{
				xml.setAttribute(cls.getSimpleName(), packetHistory._info.get(cls));
			}
		}
	};
}
