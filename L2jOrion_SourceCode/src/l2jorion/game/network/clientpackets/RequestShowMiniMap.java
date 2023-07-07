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
package l2jorion.game.network.clientpackets;

import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.network.PacketClient;
import l2jorion.game.network.serverpackets.ShowMiniMap;
import l2jorion.game.powerpack.bossInfo.RaidInfoHandler;
import l2jorion.game.thread.ThreadPoolManager;

public final class RequestShowMiniMap extends PacketClient
{
	@Override
	protected void readImpl()
	{
	}
	
	@Override
	protected final void runImpl()
	{
		final L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null)
		{
			return;
		}
		
		activeChar.sendPacket(new ShowMiniMap(1665));
		if (activeChar.getRadar().getMarkers().size() > 0)
		{
			ThreadPoolManager.getInstance().scheduleGeneral(new RaidInfoHandler.loadMarkers(activeChar), 500);
		}
	}
	
	@Override
	public String getType()
	{
		return "[C] cd RequestShowMiniMap";
	}
}
