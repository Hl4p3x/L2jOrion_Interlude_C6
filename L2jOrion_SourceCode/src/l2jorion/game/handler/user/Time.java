/* * This program is free software; you can redistribute it and/or modify
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
package l2jorion.game.handler.user;

import java.text.SimpleDateFormat;
import java.util.Date;

import l2jorion.game.GameServer;
import l2jorion.game.controllers.GameTimeController;
import l2jorion.game.handler.IUserCommandHandler;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.SystemMessage;

public class Time implements IUserCommandHandler
{
	private final SimpleDateFormat fmt = new SimpleDateFormat("H:mm:ss");
	private final SimpleDateFormat df = new SimpleDateFormat("dd MMMM, E, yyyy");
	private static final int[] COMMAND_IDS =
	{
		77
	};
	
	@Override
	public boolean useUserCommand(int id, L2PcInstance activeChar)
	{
		if (COMMAND_IDS[0] != id)
		{
			return false;
		}
		
		int time = GameTimeController.getInstance().getGameTime();
		String h = "" + time / 60 % 24;
		String m;
		
		if (time % 60 < 10)
		{
			m = "0" + time % 60;
		}
		else
		{
			m = "" + time % 60;
		}
		
		activeChar.sendMessage("--------------------------------------------------------------------------------");
		activeChar.sendMessage("Server date: " + df.format(new Date(System.currentTimeMillis())));
		activeChar.sendMessage("Server time: " + fmt.format(new Date(System.currentTimeMillis())));
		activeChar.sendMessage("Server re-started: "+ GameServer.dateTimeServerRestarted);
		activeChar.sendMessage("");
		
		SystemMessage sm;
		if (GameTimeController.getInstance().isNight())
		{
			sm = new SystemMessage(SystemMessageId.TIME_S1_S2_IN_THE_NIGHT);
			sm.addString(h);
			sm.addString(m);
		}
		else
		{
			sm = new SystemMessage(SystemMessageId.TIME_S1_S2_IN_THE_DAY);
			sm.addString(h);
			sm.addString(m);
		}
		activeChar.sendPacket(sm);
		activeChar.sendMessage("--------------------------------------------------------------------------------");
		return true;
	}

	@Override
	public int[] getUserCommandList()
	{
		return COMMAND_IDS;
	}
}
