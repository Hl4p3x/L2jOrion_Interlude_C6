package l2jorion.game.handler;

import l2jorion.game.model.actor.instance.L2ItemInstance;
import l2jorion.game.model.actor.instance.L2PlayableInstance;

public interface IItemHandler
{
	public void useItem(L2PlayableInstance playable, L2ItemInstance item);
	
	public int[] getItemIds();
}
