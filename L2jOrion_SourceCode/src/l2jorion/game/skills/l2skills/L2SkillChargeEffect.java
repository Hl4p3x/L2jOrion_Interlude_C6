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

package l2jorion.game.skills.l2skills;

import l2jorion.game.model.L2Character;
import l2jorion.game.model.L2Object;
import l2jorion.game.model.L2Skill;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.EtcStatusUpdate;
import l2jorion.game.network.serverpackets.SystemMessage;
import l2jorion.game.skills.effects.EffectCharge;
import l2jorion.game.templates.StatsSet;

public class L2SkillChargeEffect extends L2Skill
{
	final int chargeSkillId;
	
	public L2SkillChargeEffect(final StatsSet set)
	{
		super(set);
		chargeSkillId = set.getInteger("charge_skill_id");
	}
	
	@Override
	public boolean checkCondition(final L2Character activeChar, final L2Object target, final boolean itemOrWeapon)
	{
		if (activeChar instanceof L2PcInstance)
		{
			final L2PcInstance player = (L2PcInstance) activeChar;
			final EffectCharge e = (EffectCharge) player.getFirstEffect(chargeSkillId);
			if (e == null || e.numCharges < getNumCharges())
			{
				final SystemMessage sm = new SystemMessage(SystemMessageId.S1_CANNOT_BE_USED);
				sm.addSkillName(getId());
				activeChar.sendPacket(sm);
				return false;
			}
		}
		return super.checkCondition(activeChar, target, itemOrWeapon);
	}
	
	@Override
	public void useSkill(final L2Character activeChar, final L2Object[] targets)
	{
		if (activeChar.isAlikeDead())
		{
			return;
		}
		
		// get the effect
		final EffectCharge effect = (EffectCharge) activeChar.getFirstEffect(chargeSkillId);
		if (effect == null || effect.numCharges < getNumCharges())
		{
			final SystemMessage sm = new SystemMessage(SystemMessageId.S1_CANNOT_BE_USED);
			sm.addSkillName(getId());
			activeChar.sendPacket(sm);
			return;
		}
		
		// decrease?
		effect.numCharges -= getNumCharges();
		
		// update icons
		// activeChar.updateEffectIcons();
		
		// maybe exit? no charge
		if (effect.numCharges == 0)
		{
			effect.exit(false);
		}
		
		// apply effects
		if (hasEffects())
		{
			for (final L2Object target : targets)
			{
				getEffects(activeChar, (L2Character) target, false, false, false);
			}
		}
		
		if (activeChar instanceof L2PcInstance)
		{
			activeChar.sendPacket(new EtcStatusUpdate((L2PcInstance) activeChar));
		}
	}
}
