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
import l2jorion.game.model.actor.instance.L2ItemInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.network.serverpackets.ExShowScreenMessage;
import l2jorion.game.network.serverpackets.NpcHtmlMessage;
import l2jorion.game.network.serverpackets.PlaySound;
import l2jorion.game.powerpack.PowerPackConfig;

public class Bank implements IVoicedCommandHandler, ICustomByPassHandler
{
	private static String[] _voicedCommands =
	{
		"bank",
		"exchange"
	};
	
	@Override
	public boolean useVoicedCommand(String command, L2PcInstance player, String parameter)
	{
		if (player == null)
		{
			return false;
		}
		
		if (command.equalsIgnoreCase("bank") || command.equalsIgnoreCase("exchange"))
		{
			showHtm(player);
		}
		
		return true;
	}
	
	public static void showHtm(L2PcInstance player)
	{
		NpcHtmlMessage htm = new NpcHtmlMessage(player.getLastQuestNpcObject());
		String text = HtmCache.getInstance().getHtm("data/html/mods/bank.htm");
		htm.setHtml(text);
		player.sendPacket(htm);
	}
	
	@Override
	public String[] getVoicedCommandList()
	{
		return _voicedCommands;
	}
	
	@Override
	public String[] getByPassCommands()
	{
		return new String[]
		{
			"bank_main",
			"bank_deposit",
			"bank_withdraw",
			"bank_exchange",
			"bank_exchange_fa"
		};
	}
	
	private enum BysspassCmd
	{
		bank_main,
		bank_deposit,
		bank_withdraw,
		bank_exchange,
		bank_exchange_fa
	}
	
	@Override
	public void handleCommand(String command, L2PcInstance player, String parameters)
	{
		BysspassCmd comm = BysspassCmd.valueOf(command);
		
		if (comm == null)
		{
			return;
		}
		
		int goldbarId = PowerPackConfig.BANK_SYSTEM_DEPOSIT;
		L2ItemInstance goldbar = player.getInventory().getItemByItemId(goldbarId);
		int adenaId = PowerPackConfig.BANK_SYSTEM_WITHDRAW;
		L2ItemInstance adena = player.getInventory().getItemByItemId(adenaId);
		int withdrawCount = PowerPackConfig.BANK_SYSTEM_WITHDRAW_COUNT;
		
		L2ItemInstance blueSS = player.getInventory().getItemByItemId(6360);
		L2ItemInstance greenSS = player.getInventory().getItemByItemId(6361);
		L2ItemInstance redSS = player.getInventory().getItemByItemId(6362);
		
		L2ItemInstance inventoryAdena = player.getInventory().getItemByItemId(57);
		
		switch (comm)
		{
			case bank_main:
			{
				showHtm(player);
				break;
			}
			case bank_deposit:
			{
				if (adena != null)
				{
					if (adena.getCount() < withdrawCount)
					{
						player.sendMessage("You don't have enough Adena.");
						player.sendPacket(new ExShowScreenMessage("You don't have enough Adena.", 1000, 2, false));
						player.sendPacket(new PlaySound("ItemSound3.sys_impossible"));
						return;
					}
					
					if (adena.getCount() >= withdrawCount)
					{
						player.destroyItem("Consume", adena.getObjectId(), withdrawCount, null, true);
						
						player.sendMessage("Deposited.");
						player.sendPacket(new ExShowScreenMessage("Deposited.", 1000, 2, false));
						player.sendPacket(new PlaySound("ItemSound3.ItemSound3.sys_exchange_success"));
						player.addItem("Add", goldbarId, 1, null, true);
					}
				}
				
				showHtm(player);
				break;
			}
			case bank_withdraw:
			{
				if (adena != null)
				{
					if ((adena.getCount() + withdrawCount) == Integer.MAX_VALUE)
					{
						player.sendMessage("You can't exchange more.");
						player.sendPacket(new ExShowScreenMessage("You can't exchange more.", 1000, 2, false));
						player.sendPacket(new PlaySound("ItemSound3.sys_impossible"));
						return;
					}
				}
				
				if (goldbar != null)
				{
					if (goldbar.getCount() >= 1)
					{
						player.destroyItem("Consume", goldbar.getObjectId(), 1, null, true);
						player.sendMessage("Converted.");
						player.sendPacket(new ExShowScreenMessage("Converted.", 1000, 2, false));
						player.sendPacket(new PlaySound("ItemSound3.ItemSound3.sys_exchange_success"));
						player.addItem("Add", adenaId, withdrawCount, null, true);
					}
				}
				
				if (goldbar == null)
				{
					player.sendMessage("You don't have enough Gold Bar.");
					player.sendPacket(new ExShowScreenMessage("You don't have enough Gold Bar.", 1000, 2, false));
					player.sendPacket(new PlaySound("ItemSound3.sys_impossible"));
					return;
				}
				
				showHtm(player);
				break;
			}
			case bank_exchange:
			{
				if (blueSS != null)
				{
					player.addItem("Add", 5575, (blueSS.getCount() * 3), null, true);
					player.destroyItem("Consume", blueSS.getObjectId(), blueSS.getCount(), null, true);
				}
				
				if (greenSS != null)
				{
					player.addItem("Add", 5575, (greenSS.getCount() * 5), null, true);
					player.destroyItem("Consume", greenSS.getObjectId(), greenSS.getCount(), null, false);
				}
				
				if (redSS != null)
				{
					player.addItem("Add", 5575, (redSS.getCount() * 10), null, true);
					player.destroyItem("Consume", redSS.getObjectId(), redSS.getCount(), null, false);
				}
				
				if ((blueSS != null) || (greenSS != null) || (redSS != null))
				{
					player.sendMessage("Exchanged.");
					player.sendPacket(new ExShowScreenMessage("Exchanged.", 1000, 2, false));
					player.sendPacket(new PlaySound("ItemSound3.ItemSound3.sys_exchange_success"));
				}
				break;
			}
			case bank_exchange_fa:
			{
				if (inventoryAdena != null && inventoryAdena.getCount() > 350000000)
				{
					player.addItem("Add", 6673, 1, null, true);
					player.destroyItem("Consume", inventoryAdena.getObjectId(), 350000000, null, true);
					
					player.sendMessage("Exchanged.");
					player.sendPacket(new ExShowScreenMessage("Exchanged.", 1000, 2, false));
					player.sendPacket(new PlaySound("ItemSound3.ItemSound3.sys_exchange_success"));
				}
				else
				{
					player.sendMessage("You don't have enough Adena.");
					player.sendPacket(new ExShowScreenMessage("You don't have enough Adena.", 1000, 2, false));
					player.sendPacket(new PlaySound("ItemSound3.sys_impossible"));
				}
				break;
			}
		}
	}
}
