package l2jorion.game.skills.conditions;

import l2jorion.game.model.L2Character;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.skills.Env;
import l2jorion.game.skills.Stats;

public class ConditionTargetWeight extends Condition
{
	private final int _weight;
	
	public ConditionTargetWeight(final int weight)
	{
		_weight = weight;
	}
	
	@Override
	public boolean testImpl(final Env env)
	{
		final L2Character targetObj = env.getTarget();
		if ((targetObj != null) && targetObj.isPlayer())
		{
			final L2PcInstance target = targetObj.getActingPlayer();
			if (!target.getDietMode() && (target.getMaxLoad() > 0))
			{
				final int weightproc = (int) (((target.getCurrentLoad() - target.calcStat(Stats.WEIGHT_PENALTY, 1, target, null)) * 100) / target.getMaxLoad());
				return (weightproc < _weight);
			}
		}
		return false;
	}
}