package l2jorion.game.handler.item;

import l2jorion.game.handler.IItemHandler;
import l2jorion.game.model.actor.instance.L2ItemInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.actor.instance.L2PlayableInstance;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.ChooseInventoryItem;

public class AugmentItem implements IItemHandler
{
	private static final int[] ITEM_IDS =
	{
		9422,
		9423,
		9424,
		9425,
		9426,
		9427,
		9428,
		9429,
		9430,
		9431,
		9432,
		9431,
		9432,
		9433,
		9434,
		9435,
		9436,
		9437,
		9438,
		9439,
		9440,
		9441,
		9442,
		9443,
		9444,
		9445,
		9446,
		9447,
		9448,
		9449,
		9450,
		9451,
		9452,
		9453,
		9454,
		9455,
		9456,
		9457,
		9458,
		9459,
		9460,
		9461,
		9462,
		9463,
		9464,
		9465,
		9466,
		9467,
		9468,
		9469,
		9470,
		9471,
		9472,
		9473,
		9474,
		9475,
		9476,
		9477,
		9478,
		9479,
		9480
	};
	
	@Override
	public void useItem(final L2PlayableInstance playable, final L2ItemInstance item)
	{
		if (!(playable instanceof L2PcInstance))
		{
			return;
		}
		
		final L2PcInstance activeChar = (L2PcInstance) playable;
		if (activeChar.isCastingNow())
		{
			return;
		}
		
		if (activeChar.getActiveEnchantItem() == null)
		{
			activeChar.sendPacket(SystemMessageId.SELECT_THE_ITEM_TO_BE_AUGMENTED);
		}
		
		activeChar.setActiveEnchantItem(item);
		activeChar.sendPacket(new ChooseInventoryItem(item.getItemId()));
	}
	
	@Override
	public int[] getItemIds()
	{
		return ITEM_IDS;
	}
}