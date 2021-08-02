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

import l2jorion.game.cache.HtmCache;
import l2jorion.game.handler.ICustomByPassHandler;
import l2jorion.game.handler.IVoicedCommandHandler;
import l2jorion.game.model.L2World;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.network.serverpackets.ExShowScreenMessage;
import l2jorion.game.network.serverpackets.GMViewCharacterInfo;
import l2jorion.game.network.serverpackets.GMViewItemList;
import l2jorion.game.network.serverpackets.GMViewSkillInfo;
import l2jorion.game.network.serverpackets.GMViewWarehouseWithdrawList;
import l2jorion.game.network.serverpackets.NpcHtmlMessage;
import l2jorion.game.network.serverpackets.PlaySound;

public class Watch implements IVoicedCommandHandler, ICustomByPassHandler
{
	private static String[] _voicedCommands =
	{
		"watch"
	};
	
	@Override
	public boolean useVoicedCommand(String command, L2PcInstance activeChar, String parameters)
	{
		if (command.equalsIgnoreCase("watch"))
		{
			if (activeChar.getPremiumService() == 0)
			{
				activeChar.sendMessage("You're not The Premium Account.");
				activeChar.sendPacket(new ExShowScreenMessage("You're not The Premium Account.", 1000, 2, false));
				activeChar.sendPacket(new PlaySound("ItemSound3.sys_impossible"));
				return true;
			}
			
			showCommand(activeChar);
		}
		return true;
	}
	
	private void showCommand(L2PcInstance player)
	{
		NpcHtmlMessage htm = new NpcHtmlMessage(player.getLastQuestNpcObject());
		String text = HtmCache.getInstance().getHtm("data/html/mods/watch.htm");
		htm.setHtml(text);
		player.sendPacket(htm);
	}
	
	@Override
	public String[] getByPassCommands()
	{
		return new String[]
		{
			"watch_info",
			"watch_inventory",
			"watch_skills",
			"watch_warehouse"
		};
	}
	
	private enum CommandEnum
	{
		watch_info,
		watch_inventory,
		watch_skills,
		watch_warehouse
	}
	
	@Override
	public void handleCommand(String command, L2PcInstance player, String parameters)
	{
		CommandEnum comm = CommandEnum.valueOf(command);
		
		if (comm == null)
		{
			return;
		}
		
		L2PcInstance target = (L2PcInstance) player.getTarget();
		
		if (target == null)
		{
			if (parameters != null)
			{
				target = L2World.getInstance().getPlayer(parameters);
				if (target == null)
				{
					player.sendMessage("Player not found.");
					player.sendPacket(new ExShowScreenMessage("Player not found.", 2000, 0x02, false));
					return;
				}
			}
		}
		
		switch (comm)
		{
			case watch_info:
				player.sendPacket(new GMViewCharacterInfo(target));
				break;
			case watch_inventory:
				player.sendPacket(new GMViewItemList(target));
				break;
			case watch_skills:
				player.sendPacket(new GMViewSkillInfo(target));
				break;
			case watch_warehouse:
				player.sendPacket(new GMViewWarehouseWithdrawList(target));
				break;
		}
	}
	
	@Override
	public String[] getVoicedCommandList()
	{
		return _voicedCommands;
	}
}
