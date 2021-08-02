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
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.ExShowScreenMessage;
import l2jorion.game.network.serverpackets.InventoryUpdate;
import l2jorion.game.network.serverpackets.PlaySound;
import l2jorion.game.network.serverpackets.SystemMessage;

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
			
			L2PcInstance activeChar = (L2PcInstance) playable;
			
			if (activeChar.isInOlympiadMode())
			{
				activeChar.sendMessage("This item can't be used on The Olympiad game.");
			}
			
			if (activeChar.isNoble())
			{
				activeChar.sendMessage("You're already The Nobless!");
			}
			else
			{
				activeChar.setNoble(true);
				activeChar.sendMessage("Congratulations! You've got The Nobless status");
				activeChar.sendPacket(new ExShowScreenMessage("Congratulations! You've got The Nobless status", 4000, 0x02, false));
				PlaySound playSound = new PlaySound("ItemSound.quest_fanfare_1");
				activeChar.sendPacket(playSound);
				activeChar.broadcastUserInfo();
				playable.destroyItem("Consume", item.getObjectId(), 1, null, false);
				
				L2ItemInstance newitem = activeChar.getInventory().addItem("Tiara", 7694, 1, activeChar, null);
				InventoryUpdate playerIU = new InventoryUpdate();
				playerIU.addItem(newitem);
				activeChar.sendPacket(playerIU);
				SystemMessage sm;
				sm = new SystemMessage(SystemMessageId.EARNED_ITEM);
				sm.addItemName(7694);
				activeChar.sendPacket(sm);
			}
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
