package l2jorion.bots.ai.addon;

import l2jorion.bots.FakePlayer;
import l2jorion.game.model.actor.instance.L2ItemInstance;

public interface IConsumableSpender
{
	default void handleConsumable(FakePlayer fakePlayer, int consumableId)
	{
		if (fakePlayer.getInventory().getItemByItemId(consumableId) != null)
		{
			if (fakePlayer.getInventory().getItemByItemId(consumableId).getCount() <= 20)
			{
				fakePlayer.getInventory().addItem("", consumableId, 500, fakePlayer, null);
			}
		}
		else
		{
			fakePlayer.getInventory().addItem("", consumableId, 500, fakePlayer, null);
			L2ItemInstance consumable = fakePlayer.getInventory().getItemByItemId(consumableId);
			if (consumable.isEquipable())
			{
				fakePlayer.getInventory().equipItem(consumable);
			}
		}
	}
}
