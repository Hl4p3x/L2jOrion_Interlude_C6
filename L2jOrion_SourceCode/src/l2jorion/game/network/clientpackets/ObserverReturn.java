/*
 * L2jOrion Project - www.l2jorion.com 
 * 
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package l2jorion.game.network.clientpackets;

import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.network.PacketClient;
import l2jorion.game.network.serverpackets.ExShowScreenMessage;

public final class ObserverReturn extends PacketClient
{
	@Override
	protected void readImpl()
	{
	}
	
	@Override
	protected void runImpl()
	{
		final L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null)
		{
			return;
		}
		
		if (activeChar.inObserverMode())
		{
			activeChar.leaveObserverMode();
			
			if (activeChar.isBossObserve())
			{
				activeChar.setBossObserve(false);
				activeChar.setBossTaskNull();
				
				activeChar.sendMessage("Teleporting back");
				activeChar.sendPacket(new ExShowScreenMessage("Teleporting back", 2000, 2, false));
			}
		}
	}
	
	@Override
	public String getType()
	{
		return "[C] b8 ObserverReturn";
	}
}