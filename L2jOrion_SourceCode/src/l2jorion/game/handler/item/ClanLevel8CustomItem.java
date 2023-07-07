package l2jorion.game.handler.item;

import l2jorion.game.handler.IItemHandler;
import l2jorion.game.model.actor.instance.L2ItemInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.actor.instance.L2PlayableInstance;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;

public class ClanLevel8CustomItem implements IItemHandler
{
	protected static final Logger LOG = LoggerFactory.getLogger(ClanLevel8CustomItem.class);
	
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
		
		int level = player.getClan().getLevel();
		if (level >= 8)
		{
			player.sendMessage("Your clan is already level: 8.");
			return;
		}
		
		player.destroyItem("Consume", item.getObjectId(), 1, null, true);
		if (level >= 0 && level < 9)
		{
			player.getClan().changeLevel(8);
			player.sendMessage("Increased level up to 8 for the clan: " + player.getClan().getName());
			return;
		}
	}
	
	@Override
	public int[] getItemIds()
	{
		return ITEM_IDS;
	}
	
	private static final int ITEM_IDS[] =
	{
		10013
	};
}
