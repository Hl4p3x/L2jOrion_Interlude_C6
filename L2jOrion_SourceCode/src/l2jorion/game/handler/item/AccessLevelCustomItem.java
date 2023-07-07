package l2jorion.game.handler.item;

import l2jorion.Config;
import l2jorion.game.handler.IItemHandler;
import l2jorion.game.model.actor.instance.L2ItemInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.actor.instance.L2PlayableInstance;
import l2jorion.game.network.serverpackets.ExShowScreenMessage;
import l2jorion.game.network.serverpackets.PlaySound;
import l2jorion.game.network.serverpackets.UserInfo;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;

public class AccessLevelCustomItem implements IItemHandler
{
	protected static final Logger LOG = LoggerFactory.getLogger(AccessLevelCustomItem.class);
	
	@Override
	public void useItem(L2PlayableInstance playable, L2ItemInstance item)
	{
		if (Config.ACCESS_CUSTOM_ITEMS)
		{
			if (!(playable instanceof L2PcInstance))
			{
				return;
			}
			
			L2PcInstance activeChar = (L2PcInstance) playable;
			
			activeChar.setAccessLevel(10);
			activeChar.sendMessage("Congratulation! You've got an access!");
			activeChar.sendPacket(new ExShowScreenMessage("Congratulation! You've got an access!", 4000, 0x07, false));
			PlaySound playSound = new PlaySound("ItemSound.quest_fanfare_1");
			activeChar.sendPacket(playSound);
			activeChar.sendPacket(new UserInfo(activeChar));
			activeChar.broadcastUserInfo();
			playable.destroyItem("Consume", item.getObjectId(), 1, null, false);
		}
	}
	
	@Override
	public int[] getItemIds()
	{
		return ITEM_IDS;
	}
	
	private static final int ITEM_IDS[] =
	{
		Config.ACCESS_CUSTOM_ITEM_ID
	};
}