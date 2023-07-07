package l2jorion.game.handler.item;

import l2jorion.game.handler.IItemHandler;
import l2jorion.game.model.L2Clan;
import l2jorion.game.model.actor.instance.L2ItemInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.actor.instance.L2PlayableInstance;
import l2jorion.game.network.serverpackets.PledgeShowInfoUpdate;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;

public class ClanPointCustomItem implements IItemHandler
{
	protected static final Logger LOG = LoggerFactory.getLogger(ClanPointCustomItem.class);
	
	@Override
	public void useItem(L2PlayableInstance activeChar, L2ItemInstance item)
	{
		if (!(activeChar instanceof L2PcInstance))
		{
			return;
		}
		
		L2PcInstance player = (L2PcInstance) activeChar;
		
		if (player.getClan() == null)
		{
			player.sendMessage("You don't have a clan.");
			return;
		}
		
		int points = 100000;
		
		L2Clan clan = player.getClan();
		
		// if (clan.getLevel() < 5)
		// {
		// player.sendMessage("Your clan level must be higher than 4.");
		// return;
		// }
		
		player.destroyItem("Consume", item.getObjectId(), 1, null, true);
		clan.setReputationScore(clan.getReputationScore() + points, true);
		player.sendMessage("You " + (points > 0 ? "add " : "remove ") + Math.abs(points) + " points " + (points > 0 ? "to " : "from ") + clan.getName() + "'s reputation. Current scores are " + clan.getReputationScore());
		player.getClan().broadcastToOnlineMembers(new PledgeShowInfoUpdate(player.getClan()));
	}
	
	@Override
	public int[] getItemIds()
	{
		return ITEM_IDS;
	}
	
	private static final int ITEM_IDS[] =
	{
		10011
	};
}
