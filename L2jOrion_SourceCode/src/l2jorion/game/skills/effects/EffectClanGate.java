/*
 * L2jOrion Project - www.l2jorion.com 
 * 
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package l2jorion.game.skills.effects;

import l2jorion.game.model.L2Character;
import l2jorion.game.model.L2Clan;
import l2jorion.game.model.L2Effect;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.SystemMessage;
import l2jorion.game.skills.Env;

public class EffectClanGate extends L2Effect
{
	public EffectClanGate(final Env env, final EffectTemplate template)
	{
		super(env, template);
	}
	
	@Override
	public void onStart()
	{
		getEffected().startAbnormalEffect(L2Character.ABNORMAL_EFFECT_MAGIC_CIRCLE);
		
		if (getEffected() instanceof L2PcInstance)
		{
			final L2Clan clan = ((L2PcInstance) getEffected()).getClan();
			if (clan != null)
			{
				final SystemMessage msg = new SystemMessage(SystemMessageId.THE_PORTAL_HAS_BEEN_CREATED);
				clan.broadcastToOtherOnlineMembers(msg, ((L2PcInstance) getEffected()));
			}
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
		getEffected().stopAbnormalEffect(L2Character.ABNORMAL_EFFECT_MAGIC_CIRCLE);
	}
	
	@Override
	public EffectType getEffectType()
	{
		return EffectType.CLAN_GATE;
	}
}