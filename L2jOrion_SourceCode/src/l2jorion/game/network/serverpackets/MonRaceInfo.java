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

import l2jorion.game.model.actor.instance.L2NpcInstance;
import l2jorion.game.network.PacketServer;

public class MonRaceInfo extends PacketServer
{
	private static final String _S__DD_MonRaceInfo = "[S] dd MonRaceInfo";
	
	private final int _unknown1;
	private final int _unknown2;
	private final L2NpcInstance[] _monsters;
	private final int[][] _speeds;
	
	public MonRaceInfo(final int unknown1, final int unknown2, final L2NpcInstance[] monsters, final int[][] speeds)
	{
		/*
		 * -1 0 to initial the race 0 15322 to start race 13765 -1 in middle of race -1 0 to end the race
		 */
		_unknown1 = unknown1;
		_unknown2 = unknown2;
		_monsters = monsters;
		_speeds = speeds;
	}
	
	// 0xf3;;EtcStatusUpdatePacket;ddddd
	
	@Override
	protected final void writeImpl()
	{
		writeC(0xdd);
		
		writeD(_unknown1);
		writeD(_unknown2);
		writeD(8);
		
		for (int i = 0; i < 8; i++)
		{
			writeD(_monsters[i].getObjectId()); // npcObjectID
			writeD(_monsters[i].getTemplate().npcId + 1000000); // npcID
			writeD(14107); // origin X
			writeD(181875 + 58 * (7 - i)); // origin Y
			writeD(-3566); // origin Z
			writeD(12080); // end X
			writeD(181875 + 58 * (7 - i)); // end Y
			writeD(-3566); // end Z
			writeF(_monsters[i].getTemplate().collisionHeight); // coll. height
			writeF(_monsters[i].getTemplate().collisionRadius); // coll. radius
			writeD(120); // ?? unknown
			// *
			for (int j = 0; j < 20; j++)
			{
				if (_unknown1 == 0)
				{
					writeC(_speeds[i][j]);
				}
				else
				{
					writeC(0);
				}
			}
			writeD(0);
		}
	}
	
	@Override
	public String getType()
	{
		return _S__DD_MonRaceInfo;
	}
}
