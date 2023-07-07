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
package l2jorion.game.handler.admin;

import java.util.StringTokenizer;

import l2jorion.Config;
import l2jorion.game.handler.IAdminCommandHandler;
import l2jorion.game.model.L2World;
import l2jorion.game.model.actor.instance.L2PcInstance;

public class AdminMassControl implements IAdminCommandHandler
{
	
	private static String[] ADMIN_COMMANDS =
	{
		"admin_masskill",
		"admin_massress"
	};
	
	@Override
	public boolean useAdminCommand(final String command, final L2PcInstance activeChar)
	{
		if (command.startsWith("admin_mass"))
		{
			try
			{
				StringTokenizer st = new StringTokenizer(command);
				st.nextToken();
				
				if (st.nextToken().equalsIgnoreCase("kill"))
				{
					int counter = 0;
					
					for (final L2PcInstance player : L2World.getInstance().getAllPlayers().values())
					{
						if (!player.isGM())
						{
							counter++;
							player.getStatus().setCurrentHp(0);
							player.doDie(player);
							activeChar.sendMessage("You've Killed " + counter + " players.");
						}
					}
				}
				else if (st.nextToken().equalsIgnoreCase("ress"))
				{
					int counter = 0;
					
					for (final L2PcInstance player : L2World.getInstance().getAllPlayers().values())
					{
						if (!player.isGM() && player.isDead())
						{
							counter++;
							player.doRevive();
							activeChar.sendMessage("You've Ressurected " + counter + " players.");
						}
					}
				}
				
				st = null;
			}
			catch (final Exception ex)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
				{
					ex.printStackTrace();
				}
			}
		}
		
		return true;
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}
