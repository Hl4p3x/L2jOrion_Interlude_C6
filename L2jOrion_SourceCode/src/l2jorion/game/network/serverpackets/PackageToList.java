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

import java.util.Map;

import l2jorion.game.network.PacketServer;

public class PackageToList extends PacketServer
{
	private static final String _S__C2_PACKAGETOLIST = "[S] C2 PackageToList";
	private final Map<Integer, String> _players;
	
	// Lecter : i put a char list here, but i'm unsure these really are Pc. I duno how freight work tho...
	public PackageToList(final Map<Integer, String> players)
	{
		_players = players;
	}
	
	@Override
	protected void writeImpl()
	{
		writeC(0xC2);
		writeD(_players.size());
		for (final int objId : _players.keySet())
		{
			writeD(objId); // you told me char id, i guess this was object id?
			writeS(_players.get(objId));
		}
	}
	
	@Override
	public String getType()
	{
		return _S__C2_PACKAGETOLIST;
	}
}
