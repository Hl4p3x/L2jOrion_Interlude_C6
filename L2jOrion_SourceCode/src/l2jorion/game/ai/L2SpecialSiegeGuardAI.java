package l2jorion.game.ai;

import java.util.ArrayList;
import java.util.List;

import l2jorion.game.model.L2Character;
import l2jorion.game.model.actor.instance.L2SiegeGuardInstance;

public final class L2SpecialSiegeGuardAI extends L2SiegeGuardAI
{
	private final List<Integer> _allied = new ArrayList<>();
	
	public L2SpecialSiegeGuardAI(L2SiegeGuardInstance creature)
	{
		super(creature);
	}
	
	@Override
	public List<Integer> getAlly()
	{
		return _allied;
	}
	
	@Override
	protected boolean autoAttackCondition(L2Character target)
	{
		if (_allied.contains(target.getObjectId()))
		{
			return false;
		}
		
		return super.autoAttackCondition(target);
	}
}
