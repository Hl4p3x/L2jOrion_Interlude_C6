package l2jorion.game.skills.effects;

import l2jorion.game.model.L2Effect;
import l2jorion.game.skills.Env;

public class EffectDebuff extends L2Effect
{
	
	public EffectDebuff(final Env env, final EffectTemplate template)
	{
		super(env, template);
	}
	
	@Override
	public EffectType getEffectType()
	{
		return EffectType.DEBUFF;
	}
	
	@Override
	public boolean onActionTime()
	{
		// stop effect
		return false;
	}
}
