package l2jorion.game.skills.funcs;

import l2jorion.game.skills.Env;
import l2jorion.game.skills.Stats;
import l2jorion.game.skills.conditions.Condition;

public abstract class Func
{
	public final Stats stat;
	public final int order;
	public final Object funcOwner;
	public Condition cond;
	
	public Func(final Stats pStat, final int pOrder, final Object owner)
	{
		stat = pStat;
		order = pOrder;
		funcOwner = owner;
	}
	
	public void setCondition(final Condition pCond)
	{
		cond = pCond;
	}
	
	public abstract void calc(Env env);
}
