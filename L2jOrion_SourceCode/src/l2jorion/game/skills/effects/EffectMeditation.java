package l2jorion.game.skills.effects;

import l2jorion.game.model.L2Effect;
import l2jorion.game.skills.Env;

public class EffectMeditation extends L2Effect
{
	public EffectMeditation(final Env env, final EffectTemplate template)
	{
		super(env, template);
	}
	
	@Override
	public EffectType getEffectType()
	{
		return L2Effect.EffectType.MEDITATION;
	}
	
	@Override
	public void onStart()
	{
		_effected.block();
		_effected.setMeditated(true);
	}
	
	@Override
	public void onExit()
	{
		_effected.unblock();
		_effected.setMeditated(false);
	}
	
	@Override
	public boolean onActionTime()
	{
		return false;
	}
}
