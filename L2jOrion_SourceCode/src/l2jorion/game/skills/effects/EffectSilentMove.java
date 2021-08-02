/*
 * L2jOrion Project - www.l2jorion.com 
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

import l2jorion.game.model.L2Character;
import l2jorion.game.model.L2Effect;
import l2jorion.game.model.L2Skill.SkillType;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.SystemMessage;
import l2jorion.game.skills.Env;

final class EffectSilentMove extends L2Effect
{
	public EffectSilentMove(final Env env, final EffectTemplate template)
	{
		super(env, template);
	}
	
	@Override
	public void onStart()
	{
		super.onStart();
		
		final L2Character effected = getEffected();
		if (effected instanceof L2PcInstance)
		{
			((L2PcInstance) effected).setSilentMoving(true);
		}
	}
	
	@Override
	public void onExit()
	{
		super.onExit();
		
		final L2Character effected = getEffected();
		if (effected instanceof L2PcInstance)
		{
			((L2PcInstance) effected).setSilentMoving(false);
		}
	}
	
	@Override
	public EffectType getEffectType()
	{
		return EffectType.SILENT_MOVE;
	}
	
	@Override
	public boolean onActionTime()
	{
		// Only cont skills shouldn't end
		if (getSkill().getSkillType() != SkillType.CONT)
		{
			return false;
		}
		
		if (getEffected().isDead())
		{
			return false;
		}
		
		final double manaDam = calc();
		
		if (manaDam > getEffected().getCurrentMp())
		{
			final SystemMessage sm = new SystemMessage(SystemMessageId.SKILL_REMOVED_DUE_LACK_MP);
			getEffected().sendPacket(sm);
			return false;
		}
		
		getEffected().reduceCurrentMp(manaDam);
		return true;
	}
}