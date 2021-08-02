package l2jorion.game.skills.conditions;

import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.skills.Env;
import l2jorion.game.skills.Stats;

public class ConditionPlayerWeight extends Condition
{
	private final int _weight;
	
	public ConditionPlayerWeight(final int weight)
	{
		_weight = weight;
	}
	
	@Override
	public boolean testImpl(final Env env)
	{
		final L2PcInstance player = env.getPlayer();
		if ((player != null) && (player.getMaxLoad() > 0))
		{
			final int weightproc = (int) (((player.getCurrentLoad() - player.calcStat(Stats.WEIGHT_PENALTY, 1, player, null)) * 100) / player.getMaxLoad());
			return (weightproc < _weight) || player.getDietMode();
		}
		return true;
	}
}