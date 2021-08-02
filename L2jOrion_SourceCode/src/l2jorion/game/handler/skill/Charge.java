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
package l2jorion.game.handler.skill;

import l2jorion.game.handler.ISkillHandler;
import l2jorion.game.model.L2Character;
import l2jorion.game.model.L2Effect;
import l2jorion.game.model.L2Object;
import l2jorion.game.model.L2Skill;
import l2jorion.game.model.L2Skill.SkillType;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;

/**
 * This class ...
 * @version $Revision: 1.1.2.2.2.9 $ $Date: 2005/04/04 19:08:01 $
 */

public class Charge implements ISkillHandler
{
	static Logger LOG = LoggerFactory.getLogger(Charge.class);
	
	/*
	 * (non-Javadoc)
	 * @see l2jorion.game.handler.IItemHandler#useItem(l2jorion.game.model.L2PcInstance, l2jorion.game.model.L2ItemInstance)
	 */
	private static final SkillType[] SKILL_IDS = {
	/* SkillType.CHARGE */
	};
	
	@Override
	public void useSkill(final L2Character activeChar, final L2Skill skill, final L2Object[] targets)
	{
		
		for (final L2Object target1 : targets)
		{
			if (!(target1 instanceof L2PcInstance))
				continue;
			L2PcInstance target = (L2PcInstance) target1;
			skill.getEffects(activeChar, target, false, false, false);
			target = null;
		}
		// self Effect :]
		
		L2Effect effect = activeChar.getFirstEffect(skill.getId());
		if (effect != null && effect.isSelfEffect())
		{
			// Replace old effect with new one.
			effect.exit(false);
		}
		skill.getEffectsSelf(activeChar);
		
		effect = null;
	}
	
	@Override
	public SkillType[] getSkillIds()
	{
		return SKILL_IDS;
	}
}
