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
package l2jorion.game.skills.l2skills;

import l2jorion.game.datatables.sql.NpcTable;
import l2jorion.game.idfactory.IdFactory;
import l2jorion.game.model.L2Character;
import l2jorion.game.model.L2Object;
import l2jorion.game.model.L2Skill;
import l2jorion.game.model.L2World;
import l2jorion.game.model.Location;
import l2jorion.game.model.actor.instance.L2EffectPointInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.templates.L2NpcTemplate;
import l2jorion.game.templates.StatsSet;

public final class L2SkillSignet extends L2Skill
{
	private final int _effectNpcId;
	public int effectId;
	
	public L2SkillSignet(final StatsSet set)
	{
		super(set);
		
		_effectNpcId = set.getInteger("effectNpcId", -1);
		effectId = set.getInteger("effectId", -1);
	}
	
	@Override
	public void useSkill(final L2Character caster, final L2Object[] targets)
	{
		if (caster.isAlikeDead())
		{
			return;
		}
		
		final L2NpcTemplate template = NpcTable.getInstance().getTemplate(_effectNpcId);
		
		final L2EffectPointInstance effectPoint = new L2EffectPointInstance(IdFactory.getInstance().getNextId(), template, caster);
		
		effectPoint.getStatus().setCurrentHp(effectPoint.getMaxHp());
		effectPoint.getStatus().setCurrentMp(effectPoint.getMaxMp());
		
		L2World.getInstance().storeObject(effectPoint);
		
		int x = caster.getX();
		int y = caster.getY();
		int z = caster.getZ();
		
		if (caster instanceof L2PcInstance && getTargetType() == L2Skill.SkillTargetType.TARGET_GROUND)
		{
			final Location wordPosition = ((L2PcInstance) caster).getCurrentSkillWorldPosition();
			
			if (wordPosition != null)
			{
				x = wordPosition.getX();
				y = wordPosition.getY();
				z = wordPosition.getZ();
			}
		}
		
		getEffects(caster, effectPoint, false, false, false);
		
		effectPoint.setIsInvul(true);
		effectPoint.spawnMe(x, y, z);
	}
}