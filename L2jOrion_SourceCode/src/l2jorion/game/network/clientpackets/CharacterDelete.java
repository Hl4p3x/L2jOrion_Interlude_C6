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

import l2jorion.Config;
import l2jorion.game.GameServer;
import l2jorion.game.network.PacketClient;
import l2jorion.game.network.serverpackets.CharDeleteFail;
import l2jorion.game.network.serverpackets.CharDeleteOk;
import l2jorion.game.network.serverpackets.CharSelectInfo;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;

public final class CharacterDelete extends PacketClient
{
	private static Logger LOG = LoggerFactory.getLogger(CharacterDelete.class);
	private int _charSlot;
	
	@Override
	protected void readImpl()
	{
		_charSlot = readD();
	}
	
	@Override
	protected void runImpl()
	{
		
		if (!getClient().getFloodProtectors().getCharacterSelect().tryPerformAction("CharacterDelete"))
			return;
		
		if (Config.DEBUG)
			LOG.debug("DEBUG " + getType() + ": deleting slot:" + _charSlot);
		
		try
		{
			final byte answer = getClient().markToDeleteChar(_charSlot);
			switch (answer)
			{
				default:
				case -1: // Error
					break;
				case 0: // Success!
					sendPacket(new CharDeleteOk());
					break;
				case 1:
					sendPacket(new CharDeleteFail(CharDeleteFail.REASON_YOU_MAY_NOT_DELETE_CLAN_MEMBER));
					break;
				case 2:
					sendPacket(new CharDeleteFail(CharDeleteFail.REASON_CLAN_LEADERS_MAY_NOT_BE_DELETED));
					break;
			}
		}
		catch (final Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
				e.printStackTrace();
			
			LOG.error("ERROR " + getType() + ":", e);
		}
		
		// Before the char selection, check shutdown status
		if (GameServer.gameServer.getSelectorThread().isShutdown())
		{
			getClient().closeNow();
			return;
		}
		
		final CharSelectInfo cl = new CharSelectInfo(getClient().getAccountName(), getClient().getSessionId().playOkID1, 0);
		sendPacket(cl);
		getClient().setCharSelection(cl.getCharInfo());
	}
	
	@Override
	public String getType()
	{
		return "[C] 0C CharacterDelete";
	}
}
