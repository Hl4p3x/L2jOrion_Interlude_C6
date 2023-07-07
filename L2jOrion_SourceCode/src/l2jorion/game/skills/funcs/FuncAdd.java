package l2jorion.game.skills.funcs;

import l2jorion.game.skills.Env;
import l2jorion.game.skills.Stats;

public class FuncAdd extends Func
{
	private final Lambda _lambda;
	
	public FuncAdd(final Stats pStat, final int pOrder, final Object owner, final Lambda lambda)
	{
		super(pStat, pOrder, owner);
		_lambda = lambda;
	}
	
	@Override
	public void calc(final Env env)
	{
		if (cond == null || cond.test(env))
		{
			env.value += _lambda.calc(env);
		}
	}
}
