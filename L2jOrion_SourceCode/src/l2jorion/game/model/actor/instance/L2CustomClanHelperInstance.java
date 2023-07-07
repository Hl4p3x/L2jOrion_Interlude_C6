package l2jorion.game.model.actor.instance;

import java.util.ArrayList;
import java.util.Random;

import l2jorion.game.model.L2Clan;
import l2jorion.game.model.L2ClanMember;
import l2jorion.game.network.serverpackets.NpcHtmlMessage;
import l2jorion.game.network.serverpackets.PledgeShowInfoUpdate;
import l2jorion.game.templates.L2NpcTemplate;

public class L2CustomClanHelperInstance extends L2FolkInstance
{
	
	int EggIds[] =
	{
		8166,
		8169,
		8172,
		8163,
		8160,
		8176,
		8175
	};
	
	public L2CustomClanHelperInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
	}
	
	@Override
	public void showChatWindow(L2PcInstance player)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		
		html.setFile("data/html/custom/ClanHelper/clan_helper_main.htm");
		
		html.replace("%objectId%", getObjectId());
		player.sendPacket(html);
	}
	
	@Override
	public void onBypassFeedback(L2PcInstance player, String command)
	{
		if (command.startsWith("get_clan_help"))
		{
			rewardClan(player, command);
		}
		
		super.onBypassFeedback(player, command);
	}
	
	public void rewardClan(L2PcInstance player, String command)
	{
		String[] getrewardId = command.split(" ");
		
		int rewardId = Integer.valueOf(getrewardId[1]);
		
		if (!player.isClanLeader())
		{
			player.sendMessage("You are not clan leader.");
			
			return;
		}
		
		ArrayList<Integer> uniqueIps = new ArrayList<>();
		L2Clan clan = player.getClan();
		
		for (L2ClanMember member : clan.getMembers())
		{
			if (member.getPlayerInstance() == null || member.getPlayerInstance().getClient() == null)
			{
				continue;
			}
			
			int hash = member.getPlayerInstance().getIpAddress().hashCode();
			if (!uniqueIps.contains(hash))
			{
				uniqueIps.add(hash);
			}
		}
		
		switch (rewardId)
		{
			case 1:
			{
				if (uniqueIps.size() < 15)
				{
					player.sendMessage("Clan Helper: Your clan doesn't meet the requirements. All mebembers must be online.");
					return;
				}
				
				clan.setLevel(6);
				int points = 30000;
				clan.setReputationScore(clan.getReputationScore() + points, true);
				player.sendMessage("You " + (points > 0 ? "add " : "remove ") + Math.abs(points) + " points " + (points > 0 ? "to " : "from ") + clan.getName() + "'s reputation. Current scores are " + clan.getReputationScore());
				player.getClan().broadcastToOnlineMembers(new PledgeShowInfoUpdate(player.getClan()));
				
				Random random = new Random();
				int x = random.nextInt(EggIds.length);
				
				player.addItem("Clan Reward", EggIds[x], 15, player, true);
				player.sendMessage("Clan Helper: Your clan rewarded.");
				break;
			}
			case 2:
			{
				if (uniqueIps.size() < 25)
				{
					player.sendMessage("Clan Helper: Your clan doesn't meet the requirements. All mebembers must be online.");
					return;
				}
				
				clan.setLevel(7);
				int points = 40000;
				clan.setReputationScore(clan.getReputationScore() + points, true);
				player.sendMessage("You " + (points > 0 ? "add " : "remove ") + Math.abs(points) + " points " + (points > 0 ? "to " : "from ") + clan.getName() + "'s reputation. Current scores are " + clan.getReputationScore());
				player.getClan().broadcastToOnlineMembers(new PledgeShowInfoUpdate(player.getClan()));
				
				Random random = new Random();
				int x = random.nextInt(EggIds.length);
				
				player.addItem("Clan Reward", EggIds[x], 15, player, true);
				player.sendMessage("Clan Helper: Your clan rewarded.");
				break;
			}
		}
	}
}
