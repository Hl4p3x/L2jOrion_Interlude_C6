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

import l2jorion.game.datatables.sql.ItemTable;
import l2jorion.game.handler.IVoicedCommandHandler;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.network.serverpackets.NpcHtmlMessage;

public class DressMe implements IVoicedCommandHandler
{
	private static final String[] VOICED_COMMANDS =
	{
		"dressme"
	};
	
	@Override
	public boolean useVoicedCommand(String command, L2PcInstance activeChar, String parameter)
	{
		if (command.equals("dressme"))
		{
			sendMainWindow(activeChar);
		}
		
		return true;
	}
	
	public static void sendMainWindow(L2PcInstance activeChar)
	{
		NpcHtmlMessage htm = new NpcHtmlMessage(0);
		htm.setFile("./data/html/custom/dressme/main.htm");
		htm.replace("%enabled%", activeChar.isDressMeEnabled() ? "enabled" : "disabled");
		
		if (activeChar.getDressMeData() == null)
		{
			htm.replace("%chestinfo%", "You have no custom chest.");
			htm.replace("%legsinfo%", "You have no custom legs.");
			htm.replace("%bootsinfo%", "You have no custom boots.");
			htm.replace("%glovesinfo%", "You have no custom gloves.");
			htm.replace("%weapinfo%", "You have no custom weapon.");
		}
		else
		{
			htm.replace("%chestinfo%", activeChar.getDressMeData().getChestId() == 0 ? "You have no custom chest." : ItemTable.getInstance().getTemplate(activeChar.getDressMeData().getChestId()).getName());
			htm.replace("%legsinfo%", activeChar.getDressMeData().getLegsId() == 0 ? "You have no custom legs." : ItemTable.getInstance().getTemplate(activeChar.getDressMeData().getLegsId()).getName());
			htm.replace("%bootsinfo%", activeChar.getDressMeData().getBootsId() == 0 ? "You have no custom boots." : ItemTable.getInstance().getTemplate(activeChar.getDressMeData().getBootsId()).getName());
			htm.replace("%glovesinfo%", activeChar.getDressMeData().getGlovesId() == 0 ? "You have no custom gloves." : ItemTable.getInstance().getTemplate(activeChar.getDressMeData().getGlovesId()).getName());
			htm.replace("%weapinfo%", activeChar.getDressMeData().getWeapId() == 0 ? "You have no custom weapon." : ItemTable.getInstance().getTemplate(activeChar.getDressMeData().getWeapId()).getName());
		}
		
		activeChar.sendPacket(htm);
	}
	
	@Override
	public String[] getVoicedCommandList()
	{
		return VOICED_COMMANDS;
	}
}