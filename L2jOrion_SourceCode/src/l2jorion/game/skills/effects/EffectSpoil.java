package l2jorion.game.skills.effects;

import l2jorion.game.ai.CtrlEvent;
import l2jorion.game.model.L2Effect;
import l2jorion.game.model.actor.instance.L2MonsterInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.SystemMessage;
import l2jorion.game.skills.Env;
import l2jorion.game.skills.Formulas;

public class EffectSpoil extends L2Effect
{
	public EffectSpoil(Env env, EffectTemplate template)
	{
		super(env, template);
	}
	
	@Override
	public EffectType getEffectType()
	{
		return EffectType.SPOIL;
	}
	
	@Override
	public void onStart()
	{
		if (!(getEffector() instanceof L2PcInstance))
		{
			return;
		}
		
		if (!(getEffected() instanceof L2MonsterInstance))
		{
			return;
		}
		
		final L2MonsterInstance target = (L2MonsterInstance) getEffected();
		if (target.isDead())
		{
			return;
		}
		
		if (target.getSpoilerId() != 0)
		{
			getEffector().sendPacket(SystemMessage.getSystemMessage(SystemMessageId.ALREADY_SPOILED));
			return;
		}
		
		if (Formulas.calcMagicSuccess(getEffector(), target, getSkill()))
		{
			target.setSpoilerId(getEffector().getObjectId());
			getEffector().sendPacket(SystemMessage.getSystemMessage(SystemMessageId.SPOIL_SUCCESS));
		}
		
		target.getAI().notifyEvent(CtrlEvent.EVT_ATTACKED, getEffector());
		return;
	}
	
	@Override
	public boolean onActionTime()
	{
		return false;
	}
}