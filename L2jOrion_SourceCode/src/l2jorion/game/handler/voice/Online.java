/*
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
package l2jorion.game.handler.voice;

import l2jorion.Config;
import l2jorion.game.handler.IVoicedCommandHandler;
import l2jorion.game.model.L2World;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.network.serverpackets.ExShowScreenMessage;

public class Online implements IVoicedCommandHandler
{
	private static String[] _voicedCommands =
	{
		"online"
	};
	
	@Override
	public boolean useVoicedCommand(String command, L2PcInstance activeChar, String parameters)
	{
		if (Config.ALLOW_ONLINE_VIEW_ONLY_FOR_GM && !activeChar.isGM())
		{
			return true;
		}
		
		if (command.equalsIgnoreCase("online"))
		{
			activeChar.sendPacket(new ExShowScreenMessage(1, 11111, 3, false, 0, 0, 0, true, 6000, false, " Players online: " + L2World.getInstance().getAllPlayersCount()));
			activeChar.sendMessage("--------------------------------------------------------------------------------");
			activeChar.sendMessage("Players online: " + L2World.getInstance().getAllPlayersCount());
			activeChar.sendMessage("--------------------------------------------------------------------------------");
		}
		return true;
	}
	
	@Override
	public String[] getVoicedCommandList()
	{
		return _voicedCommands;
	}
}
