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
package l2jorion.login.network.gameserverpackets;

import l2jorion.game.datatables.GameServerTable;
import l2jorion.game.datatables.GameServerTable.GameServerInfo;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;
import l2jorion.login.network.clientpackets.ClientBasePacket;

public class ServerStatus extends ClientBasePacket
{
	protected static Logger LOG = LoggerFactory.getLogger(ServerStatus.class);
	
	public static final String[] STATUS_STRING =
	{
		"Auto",
		"Good",
		"Normal",
		"Full",
		"Down",
		"Gm Only"
	};
	
	public static final int SERVER_LIST_STATUS = 0x01;
	public static final int SERVER_LIST_CLOCK = 0x02;
	public static final int SERVER_LIST_SQUARE_BRACKET = 0x03;
	public static final int MAX_PLAYERS = 0x04;
	public static final int TEST_SERVER = 0x05;
	
	public static final int STATUS_AUTO = 0x00;
	public static final int STATUS_GOOD = 0x01;
	public static final int STATUS_NORMAL = 0x02;
	public static final int STATUS_FULL = 0x03;
	public static final int STATUS_DOWN = 0x04;
	public static final int STATUS_GM_ONLY = 0x05;
	
	public static final int ON = 0x01;
	public static final int OFF = 0x00;
	
	public ServerStatus(final byte[] decrypt, final int serverId)
	{
		super(decrypt);
		
		GameServerInfo gsi = GameServerTable.getInstance().getRegisteredGameServerById(serverId);
		if (gsi != null)
		{
			final int size = readD();
			
			for (int i = 0; i < size; i++)
			{
				final int type = readD();
				final int value = readD();
				
				switch (type)
				{
					case SERVER_LIST_STATUS:
						gsi.setStatus(value);
						break;
					case SERVER_LIST_CLOCK:
						gsi.setShowingClock(value == ON);
						break;
					case SERVER_LIST_SQUARE_BRACKET:
						gsi.setShowingBrackets(value == ON);
						break;
					case TEST_SERVER:
						gsi.setTestServer(value == ON);
						break;
					case MAX_PLAYERS:
						gsi.setMaxPlayers(value);
						break;
				}
			}
		}
	}
}
