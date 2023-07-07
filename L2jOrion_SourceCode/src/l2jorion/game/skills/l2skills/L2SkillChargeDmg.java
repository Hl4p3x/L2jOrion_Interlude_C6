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

import java.util.ArrayList;
import java.util.List;

import l2jorion.game.model.L2Character;
import l2jorion.game.model.L2Effect;
import l2jorion.game.model.L2Object;
import l2jorion.game.model.L2Skill;
import l2jorion.game.model.actor.instance.L2ItemInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.EtcStatusUpdate;
import l2jorion.game.network.serverpackets.SystemMessage;
import l2jorion.game.skills.BaseStats;
import l2jorion.game.skills.Formulas;
import l2jorion.game.skills.effects.EffectCharge;
import l2jorion.game.templates.L2WeaponType;
import l2jorion.game.templates.StatsSet;

public class L2SkillChargeDmg extends L2Skill
{
	final int chargeSkillId;
	
	public L2SkillChargeDmg(final StatsSet set)
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
	public void useSkill(final L2Character caster, final L2Object[] targets)
	{
		if (caster.isAlikeDead())
		{
			return;
		}
		
		// get the effect
		final EffectCharge effect = (EffectCharge) caster.getFirstEffect(chargeSkillId);
		if (effect == null || effect.numCharges < getNumCharges())
		{
			final SystemMessage sm = new SystemMessage(SystemMessageId.S1_CANNOT_BE_USED);
			sm.addSkillName(getId());
			caster.sendPacket(sm);
			return;
		}
		
		double modifier = 0;
		modifier = (effect.getLevel() - getNumCharges()) * 0.33;
		
		if (getTargetType() != SkillTargetType.TARGET_AREA && getTargetType() != SkillTargetType.TARGET_MULTIFACE)
		{
			effect.numCharges -= getNumCharges();
		}
		
		if (caster instanceof L2PcInstance)
		{
			caster.sendPacket(new EtcStatusUpdate((L2PcInstance) caster));
		}
		
		if (effect.numCharges == 0)
		{
			effect.exit(false);
		}
		
		// Calculate targets based on vegeance
		List<L2Object> target_s = new ArrayList<>();
		for (L2Object _target : targets)
		{
			target_s.add(_target);
			L2Character target = (L2Character) _target;
			
			if (target.vengeanceSkill(this))
			{
				target_s.add(caster);
			}
		}
		
		final boolean ss = caster.checkSs();
		
		for (final L2Object target2 : target_s)
		{
			final L2ItemInstance weapon = caster.getActiveWeaponInstance();
			final L2Character target = (L2Character) target2;
			
			if (target.isAlikeDead())
			{
				continue;
			}
			
			final boolean shld = Formulas.calcShldUse(caster, target);
			final boolean soul = (weapon != null && weapon.getChargedSoulshot() == L2ItemInstance.CHARGED_SOULSHOT && weapon.getItemType() != L2WeaponType.DAGGER);
			boolean crit = false;
			
			if (this.getBaseCritRate() > 0)
			{
				crit = Formulas.calcCrit(this.getBaseCritRate() * 10 * BaseStats.STR.calcBonus(caster));
			}
			
			// damage calculation
			int damage = (int) Formulas.calcPhysDam(caster, target, this, shld, false, false, soul);
			
			// Like L2OFF damage calculation crit is static 2x
			if (crit)
			{
				damage *= 2;
			}
			
			if (damage > 0)
			{
				double finalDamage = damage;
				finalDamage = finalDamage + (modifier * finalDamage);
				target.reduceCurrentHp(finalDamage, caster);
				caster.sendDamageMessage(target, (int) finalDamage, false, crit, false);
			}
			else
			{
				caster.sendDamageMessage(target, 0, false, false, true);
			}
		}
		
		if (ss)
		{
			caster.removeSs();
		}
		
		// effect self :]
		final L2Effect seffect = caster.getFirstEffect(getId());
		if (seffect != null && seffect.isSelfEffect())
		{
			// Replace old effect with new one.
			seffect.exit(false);
		}
		// cast self effect if any
		getEffectsSelf(caster);
	}
}