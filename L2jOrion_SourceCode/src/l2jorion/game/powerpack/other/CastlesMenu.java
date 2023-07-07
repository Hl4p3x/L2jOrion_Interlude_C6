package l2jorion.game.powerpack.other;

import java.util.StringTokenizer;

import l2jorion.game.handler.ICustomByPassHandler;
import l2jorion.game.managers.CastleManager;
import l2jorion.game.model.L2Clan;
import l2jorion.game.model.actor.instance.L2ItemInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.entity.siege.Castle;
import l2jorion.game.network.serverpackets.NpcHtmlMessage;

public class CastlesMenu implements ICustomByPassHandler
{
	@Override
	public String[] getByPassCommands()
	{
		return new String[]
		{
			"castles",
			"castle_info",
			"castle_set"
		};
	}
	
	private enum CommandEnum
	{
		castles,
		castle_info,
		castle_set
	}
	
	@Override
	public void handleCommand(String command, L2PcInstance player, String parameters)
	{
		CommandEnum comm = CommandEnum.valueOf(command);
		
		if (comm == null)
		{
			return;
		}
		
		StringTokenizer st = new StringTokenizer(parameters, " ");
		
		switch (comm)
		{
			case castles:
			{
				String filename = "data/html/gmshop/castles/castles.htm";
				NpcHtmlMessage html = new NpcHtmlMessage(1);
				html.setFile(filename);
				player.sendPacket(html);
				break;
			}
			case castle_info:
			{
				Castle castle = CastleManager.getInstance().getCastle(st.nextToken());
				castle.getSiege().listRegisterClan(player);
				break;
			}
			case castle_set:
				L2Clan clan = player.getClan();
				Castle castle = CastleManager.getInstance().getCastle(st.nextToken());
				
				if (clan == null)
				{
					player.sendMessage("You don't have a clan.");
					return;
				}
				
				L2ItemInstance GM = player.getInventory().getItemByItemId(6393);
				int clanPoints = player.getClan().getReputationScore();
				int pcBangPoints = player.getPcBangScore();
				
				if (GM == null || GM.getCount() < 1500)
				{
					player.sendMessage("You don't have enough Glittering Medals.");
					return;
				}
				
				if (clanPoints < 150000)
				{
					player.sendMessage("You don't have enough Clan Points.");
					return;
				}
				
				if (pcBangPoints < 1500)
				{
					player.sendMessage("You don't have enough PCB Points.");
					return;
				}
				
				if (castle.getOwnerId() != 0)
				{
					player.sendMessage(castle.getName() + " already has owner. You can't buy this castle.");
					return;
				}
				
				player.destroyItem("Consume", GM.getObjectId(), 1500, null, true);
				clan.setReputationScore(clan.getReputationScore() - 150000, true);
				player.reducePcBangScore(1500);
				castle.setOwner(clan);
				break;
		}
	}
}