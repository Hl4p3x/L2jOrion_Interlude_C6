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

import java.util.ArrayList;
import java.util.List;

import l2jorion.game.datatables.SkillTable;
import l2jorion.game.model.L2Character;
import l2jorion.game.model.L2Effect;
import l2jorion.game.model.L2Skill;
import l2jorion.game.model.actor.instance.L2EffectPointInstance;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.MagicSkillUser;
import l2jorion.game.network.serverpackets.SystemMessage;
import l2jorion.game.skills.Env;
import l2jorion.game.skills.l2skills.L2SkillSignet;
import l2jorion.game.skills.l2skills.L2SkillSignetCasttime;

public final class EffectSignet extends L2Effect
{
	private L2Skill _skill;
	private L2EffectPointInstance _actor;
	private boolean _srcInArena;
	
	public EffectSignet(final Env env, final EffectTemplate template)
	{
		super(env, template);
	}
	
	@Override
	public EffectType getEffectType()
	{
		return EffectType.SIGNET_EFFECT;
	}
	
	@Override
	public void onStart()
	{
		if (getSkill() instanceof L2SkillSignet)
		{
			_skill = SkillTable.getInstance().getInfo(((L2SkillSignet) getSkill()).effectId, getLevel());
		}
		else if (getSkill() instanceof L2SkillSignetCasttime)
		{
			_skill = SkillTable.getInstance().getInfo(((L2SkillSignetCasttime) getSkill()).effectId, getLevel());
		}
		
		_actor = (L2EffectPointInstance) getEffected();
		
		_srcInArena = getEffector().isInArena();
	}
	
	@Override
	public boolean onActionTime()
	{
		if (_skill == null)
		{
			return true;
		}
		
		int mpConsume = _skill.getMpConsume();
		
		if (mpConsume > getEffector().getCurrentMp())
		{
			getEffector().sendPacket(SystemMessage.getSystemMessage(SystemMessageId.SKILL_REMOVED_DUE_LACK_MP));
			return false;
		}
		
		getEffector().reduceCurrentMp(mpConsume);
		
		List<L2Character> targets = new ArrayList<>();
		
		for (final L2Character cha : _actor.getKnownList().getKnownCharactersInRadius(getSkill().getSkillRadius()))
		{
			if (_skill.isOffensive() && !L2Skill.checkForAreaOffensiveSkills(getEffector(), cha, _skill, _srcInArena))
			{
				continue;
			}
			
			_actor.broadcastPacket(new MagicSkillUser(_actor, cha, _skill.getId(), _skill.getLevel(), 0, 0));
			
			targets.add(cha);
		}
		
		if (!targets.isEmpty())
		{
			getEffector().callSkill(_skill, targets.toArray(new L2Character[targets.size()]));
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