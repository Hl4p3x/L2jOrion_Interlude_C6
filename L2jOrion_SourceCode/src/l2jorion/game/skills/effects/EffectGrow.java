package l2jorion.game.skills.effects;

import l2jorion.game.model.L2Character;
import l2jorion.game.model.L2Effect;
import l2jorion.game.model.actor.instance.L2NpcInstance;
import l2jorion.game.skills.Env;

public class EffectGrow extends L2Effect
{
	public EffectGrow(final Env env, final EffectTemplate template)
	{
		super(env, template);
	}
	
	@Override
	public EffectType getEffectType()
	{
		return EffectType.BUFF;
	}
	
	@Override
	public void onStart()
	{
		if (getEffected() instanceof L2NpcInstance)
		{
			
			L2NpcInstance npc = (L2NpcInstance) getEffected();
			
			npc.setCollisionRadius((int) (npc.getCollisionRadius() * 1.19));
			// npc.setCollisionHeight((int) (npc.getCollisionHeight() * 1.19));
			
			getEffected().startAbnormalEffect(L2Character.ABNORMAL_EFFECT_GROW);
		}
	}
	
	@Override
	public boolean onActionTime()
	{
		return false;
	}
	
	@Override
	public void onExit()
	{
		if (getEffected() instanceof L2NpcInstance)
		{
			L2NpcInstance npc = (L2NpcInstance) getEffected();
			
			npc.setCollisionRadius(npc.getTemplate().getCollisionRadius());
			// npc.setCollisionHeight(npc.getTemplate().getCollisionHeight());
			
			getEffected().stopAbnormalEffect(L2Character.ABNORMAL_EFFECT_GROW);
		}
	}
}
