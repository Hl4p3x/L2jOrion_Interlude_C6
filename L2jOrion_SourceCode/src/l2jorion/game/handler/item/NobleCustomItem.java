// Noble Custom Item , Created By Stefoulis15
// Added From Stefoulis15 Into The Core.
// Visit www.MaxCheaters.com For Support 
// Source File Name:   NobleCustomItem.java
// Modded by programmos, sword dev

package l2jorion.game.handler.item;

import l2jorion.Config;
import l2jorion.game.handler.IItemHandler;
import l2jorion.game.model.actor.instance.L2ItemInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.actor.instance.L2PlayableInstance;
import l2jorion.game.network.serverpackets.ExShowScreenMessage;
import l2jorion.game.network.serverpackets.PlaySound;

public class NobleCustomItem implements IItemHandler
{
	@Override
	public void useItem(L2PlayableInstance playable, L2ItemInstance item)
	{
		if (Config.NOBLE_CUSTOM_ITEMS)
		{
			if (!(playable instanceof L2PcInstance))
			{
				return;
			}
			
			L2PcInstance player = (L2PcInstance) playable;
			
			if (player.isInOlympiadMode())
			{
				player.sendMessage("This item can't be used on The Olympiad game.");
				return;
			}
			
			if (player.isNoble())
			{
				player.sendMessage("You're already The Nobless!");
				return;
			}
			
			player.setNoble(true);
			player.sendMessage("Congratulations! You've got The Nobless status.");
			player.sendPacket(new ExShowScreenMessage("Congratulations! You've got The Nobless status.", 4000, 0x02, false));
			PlaySound playSound = new PlaySound("ItemSound.quest_fanfare_1");
			player.sendPacket(playSound);
			player.broadcastUserInfo();
			player.destroyItem("Consume", item.getObjectId(), 1, null, false);
			player.addItem("quest", 7694, 1, player, true);
		}
	}
	
	@Override
	public int[] getItemIds()
	{
		return ITEM_IDS;
	}
	
	private static final int ITEM_IDS[] =
	{
		Config.NOOBLE_CUSTOM_ITEM_ID
	};
	
}
