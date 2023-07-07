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
package l2jorion.game.managers;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import l2jorion.log.Log;

public class PacketsLoggerManager
{
	private final List<String> _monitored_characters = new ArrayList<>();
	
	private final Hashtable<String, List<String>> _character_blocked_packets = new Hashtable<>();
	
	protected PacketsLoggerManager()
	{
		_character_blocked_packets.clear();
		_monitored_characters.clear();
	}
	
	public void startCharacterPacketsMonitoring(final String character)
	{
		
		if (!_monitored_characters.contains(character))
		{
			_monitored_characters.add(character);
		}
		
	}
	
	public void stopCharacterPacketsMonitoring(final String character)
	{
		
		if (_monitored_characters.contains(character))
		{
			_monitored_characters.remove(character);
		}
		
	}
	
	public void blockCharacterPacket(final String character, final String packet)
	{
		
		List<String> blocked_packets = _character_blocked_packets.get(character);
		if (blocked_packets == null)
		{
			blocked_packets = new ArrayList<>();
		}
		
		if (!blocked_packets.contains(packet))
		{
			blocked_packets.add(packet);
		}
		_character_blocked_packets.put(character, blocked_packets);
		
	}
	
	public void restoreCharacterPacket(final String character, final String packet)
	{
		
		final List<String> blocked_packets = _character_blocked_packets.get(character);
		if (blocked_packets != null)
		{
			
			if (blocked_packets.contains(packet))
			{
				blocked_packets.remove(packet);
			}
			
			_character_blocked_packets.put(character, blocked_packets);
			
		}
		
	}
	
	public boolean isCharacterMonitored(final String character)
	{
		return _monitored_characters.contains(character);
	}
	
	public boolean isCharacterPacketBlocked(final String character, final String packet)
	{
		
		final List<String> blocked_packets = _character_blocked_packets.get(character);
		if (blocked_packets != null)
		{
			
			if (blocked_packets.contains(packet))
			{
				return true;
			}
			
		}
		
		return false;
		
	}
	
	public void logCharacterPacket(final String character, final String packet)
	{
		
		Log.add("[Character: " + character + "] has sent [Packet: " + packet + "]", character + "_packets");
		
	}
	
	public static PacketsLoggerManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		public static final PacketsLoggerManager _instance = new PacketsLoggerManager();
	}
}
