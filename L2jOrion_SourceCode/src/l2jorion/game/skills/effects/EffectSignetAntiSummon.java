/* L2jOrion Project - www.l2jorion.com 
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307, USA.
 *
 * http://www.gnu.org/copyleft/gpl.html
 */
package l2jorion.game.skills.effects;

import l2jorion.game.ai.CtrlEvent;
import l2jorion.game.model.L2Character;
import l2jorion.game.model.L2Effect;
import l2jorion.game.model.actor.instance.L2EffectPointInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.actor.instance.L2PlayableInstance;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.SystemMessage;
import l2jorion.game.skills.Env;

public final class EffectSignetAntiSummon extends L2Effect
{
	private L2EffectPointInstance _actor;
	
	public EffectSignetAntiSummon(final Env env, final EffectTemplate template)
	{
		super(env, template);
	}
	
	@Override
	public EffectType getEffectType()
	{
		return EffectType.SIGNET_GROUND;
	}
	
	@Override
	public void onStart()
	{
		_actor = (L2EffectPointInstance) getEffected();
	}
	
	@Override
	public boolean onActionTime()
	{
		if (getCount() == getTotalCount() - 1)
		{
			return true; // do nothing first time
		}
		final int mpConsume = getSkill().getMpConsume();
		
		for (final L2Character cha : _actor.getKnownList().getKnownCharactersInRadius(getSkill().getSkillRadius()))
		{
			if (cha == null)
			{
				continue;
			}
			
			if (cha instanceof L2PlayableInstance)
			{
				final L2PcInstance owner = (L2PcInstance) cha;
				if (owner.getPet() != null)
				{
					if (mpConsume > getEffector().getStatus().getCurrentMp())
					{
						getEffector().sendPacket(new SystemMessage(SystemMessageId.SKILL_REMOVED_DUE_LACK_MP));
						return false;
					}
					
					getEffector().reduceCurrentMp(mpConsume);
					
					owner.getPet().unSummon(owner);
					owner.getAI().notifyEvent(CtrlEvent.EVT_ATTACKED, getEffector());
				}
			}
		}
		return true;
	}
	
	@Override
	public void onExit()
	{
		if (_actor != null)
		{
			_actor.deleteMe();
		}
	}
}
