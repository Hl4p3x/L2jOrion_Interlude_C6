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
package l2jorion.game.handler.admin;

import l2jguard.HwidConfig;
import l2jguard.hwidmanager.HWIDBan;
import l2jorion.game.handler.IAdminCommandHandler;
import l2jorion.game.model.L2World;
import l2jorion.game.model.actor.instance.L2PcInstance;

public class AdminBanHwid implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_hwid_ban",
		"admin_ban_hwid"
	};
	
	@Override
	public boolean useAdminCommand(String command, L2PcInstance activeChar)
	{
		if (!HwidConfig.ALLOW_GUARD_SYSTEM)
		{
			return false;
		}
		
		if (command.startsWith("admin_hwid_ban") || command.startsWith("admin_ban_hwid"))
		{
			String[] _command = command.split(" ");
			
			L2PcInstance getPlayer1 = L2World.getInstance().getPlayer(_command[1]);
			L2PcInstance getPlayer2 = (L2PcInstance) activeChar.getTarget();
			L2PcInstance target = getPlayer1 == null ? getPlayer2 : getPlayer1;
			
			if (target == null)
			{
				activeChar.sendMessage("Target is empty");
				return false;
			}
			
			HWIDBan.addHWIDBan(target.getClient());
			activeChar.sendMessage(target.getName() + " banned in HWID");
		}
		return true;
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}