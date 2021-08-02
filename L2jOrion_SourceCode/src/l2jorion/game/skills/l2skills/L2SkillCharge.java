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

public class L2SkillCharge extends L2Skill
{
	public L2SkillCharge(final StatsSet set)
	{
		super(set);
	}
	
	@Override
	public boolean checkCondition(final L2Character activeChar, final L2Object target, final boolean itemOrWeapon)
	{
		if (activeChar instanceof L2PcInstance)
		{
			final EffectCharge e = (EffectCharge) activeChar.getFirstEffect(this);
			if ((e != null) && (e.numCharges >= getNumCharges()))
			{
				activeChar.sendPacket(new SystemMessage(SystemMessageId.FORCE_MAXLEVEL_REACHED));
				final SystemMessage sm = new SystemMessage(SystemMessageId.S1_CANNOT_BE_USED);
				sm.addSkillName(getId());
				activeChar.sendPacket(sm);
				return false;
			}
		}
		return super.checkCondition(activeChar, target, itemOrWeapon);
	}
	
	@Override
	public void useSkill(final L2Character caster, final L2Object[] targets)
	{
		if (caster.isAlikeDead())
		{
			return;
		}
		
		// get the effect
		EffectCharge effect = null;
		if (caster instanceof L2PcInstance)
		{
			effect = ((L2PcInstance) caster).getChargeEffect();
		}
		else
		{
			effect = (EffectCharge) caster.getFirstEffect(this);
		}
		
		if (effect != null)
		{
			if (effect.numCharges < getNumCharges())
			{
				effect.numCharges++;
				if (caster instanceof L2PcInstance)
				{
					caster.sendPacket(new EtcStatusUpdate((L2PcInstance) caster));
					final SystemMessage sm = new SystemMessage(SystemMessageId.FORCE_INCREASED_TO_S1);
					sm.addNumber(effect.numCharges);
					caster.sendPacket(sm);
				}
			}
			else
			{
				final SystemMessage sm = new SystemMessage(SystemMessageId.FORCE_MAXIMUM);
				caster.sendPacket(sm);
			}
			return;
		}
		getEffects(caster, caster, false, false, false);
		getEffectsSelf(caster);
	}
}
