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
package l2jorion.game.handler.admin;

import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import l2jorion.Config;
import l2jorion.game.datatables.SkillTable;
import l2jorion.game.handler.IAdminCommandHandler;
import l2jorion.game.model.L2Character;
import l2jorion.game.model.L2Object;
import l2jorion.game.model.L2Skill;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.network.serverpackets.MagicSkillUser;
import l2jorion.game.thread.ThreadPoolManager;

public class AdminTest implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_test",
		"admin_stats",
		"admin_stats_stp",
		"admin_stats_tp",
		"admin_mcrit",
		"admin_addbufftest",
		"admin_skill_test",
		"admin_mp",
		"admin_known",
		"admin_oly_obs_mode",
		"admin_obs_mode"
	};
	
	@Override
	public boolean useAdminCommand(final String command, final L2PcInstance activeChar)
	{
		if (command.equals("admin_stats"))
		{
			for (final String line : ThreadPoolManager.getInstance().getStats())
			{
				activeChar.sendMessage(line);
			}
		}
		if (command.equals("admin_stats_stp"))
		{
			for (final String line : ThreadPoolManager.getInstance().getStatsSTP())
			{
				activeChar.sendMessage(line);
			}
		}
		if (command.equals("admin_stats_tp"))
		{
			for (final String line : ThreadPoolManager.getInstance().getStatsTP())
			{
				activeChar.sendMessage(line);
			}
		}
		if (command.equals("admin_mcrit"))
		{
			final L2Character target = (L2Character) activeChar.getTarget();
			
			activeChar.sendMessage("Activechar Mcrit " + activeChar.getMCriticalHit(null, null));
			activeChar.sendMessage("Activechar baseMCritRate " + activeChar.getTemplate().baseMCritRate);
			
			if (target != null)
			{
				activeChar.sendMessage("Target Mcrit " + target.getMCriticalHit(null, null));
				activeChar.sendMessage("Target baseMCritRate " + target.getTemplate().baseMCritRate);
			}
		}
		if (command.equals("admin_addbufftest"))
		{
			final L2Character target = (L2Character) activeChar.getTarget();
			activeChar.sendMessage("cast");
			
			final L2Skill skill = SkillTable.getInstance().getInfo(1085, 3);
			
			if (target != null)
			{
				activeChar.sendMessage("target locked");
				
				for (int i = 0; i < 100;)
				{
					if (activeChar.isCastingNow())
					{
						continue;
					}
					
					activeChar.sendMessage("Casting " + i);
					activeChar.useMagic(skill, false, false);
					i++;
				}
			}
		}
		else if (command.startsWith("admin_skill_test"))
		{
			try
			{
				StringTokenizer st = new StringTokenizer(command);
				st.nextToken();
				
				final int id = Integer.parseInt(st.nextToken());
				
				adminTestSkill(activeChar, id);
				
				st = null;
			}
			catch (NumberFormatException | NoSuchElementException e)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
				{
					e.printStackTrace();
				}
				
				activeChar.sendMessage("Command format is //skill_test <ID>");
			}
		}
		else if (command.startsWith("admin_oly_obs_mode"))
		{
			if (!activeChar.inObserverMode())
			{
				activeChar.enterOlympiadObserverMode(-1);
			}
			else
			{
				activeChar.leaveOlympiadObserverMode();
			}
		}
		else if (command.startsWith("admin_obs_mode"))
		{
			if (!activeChar.inObserverMode())
			{
				activeChar.enterObserverMode(activeChar.getX(), activeChar.getY(), activeChar.getZ());
			}
			else
			{
				activeChar.leaveObserverMode();
			}
		}
		return true;
	}
	
	private void adminTestSkill(final L2PcInstance activeChar, final int id)
	{
		L2Character player;
		L2Object target = activeChar.getTarget();
		
		if (target == null || !(target instanceof L2Character))
		{
			player = activeChar;
		}
		else
		{
			player = (L2Character) target;
		}
		
		player.broadcastPacket(new MagicSkillUser(activeChar, player, id, 1, 1, 1));
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}