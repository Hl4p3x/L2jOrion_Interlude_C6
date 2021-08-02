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
import l2jorion.game.model.L2Effect;
import l2jorion.game.model.L2Object;
import l2jorion.game.model.L2Skill;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.SystemMessage;
import l2jorion.game.skills.Formulas;
import l2jorion.game.templates.StatsSet;

public class L2SkillElemental extends L2Skill
{
	private final int[] _seeds;
	private final boolean _seedAny;
	
	public L2SkillElemental(final StatsSet set)
	{
		super(set);
		
		_seeds = new int[3];
		_seeds[0] = set.getInteger("seed1", 0);
		_seeds[1] = set.getInteger("seed2", 0);
		_seeds[2] = set.getInteger("seed3", 0);
		
		if (set.getInteger("seed_any", 0) == 1)
		{
			_seedAny = true;
		}
		else
		{
			_seedAny = false;
		}
	}
	
	@Override
	public void useSkill(final L2Character activeChar, final L2Object[] targets)
	{
		if (activeChar.isAlikeDead())
		{
			return;
		}
		
		final boolean sps = activeChar.checkSps();
		final boolean bss = activeChar.checkBss();
		
		for (final L2Object target2 : targets)
		{
			final L2Character target = (L2Character) target2;
			if (target.isAlikeDead())
			{
				continue;
			}
			
			boolean charged = true;
			if (!_seedAny)
			{
				for (final int seed : _seeds)
				{
					if (seed != 0)
					{
						final L2Effect e = target.getFirstEffect(seed);
						if (e == null || !e.getInUse())
						{
							charged = false;
							break;
						}
					}
				}
			}
			else
			{
				charged = false;
				for (final int seed : _seeds)
				{
					if (seed != 0)
					{
						final L2Effect e = target.getFirstEffect(seed);
						if (e != null && e.getInUse())
						{
							charged = true;
							break;
						}
					}
				}
			}
			if (!charged)
			{
				final SystemMessage sm = new SystemMessage(SystemMessageId.S1_S2);
				sm.addString("Target is not charged by elements.");
				activeChar.sendPacket(sm);
				continue;
			}
			
			final boolean mcrit = Formulas.calcMCrit(activeChar.getMCriticalHit(target, this));
			
			final int damage = (int) Formulas.calcMagicDam(activeChar, target, this, sps, bss, mcrit);
			
			if (damage > 0)
			{
				target.reduceCurrentHp(damage, activeChar);
				
				// Manage attack or cast break of the target (calculating rate, sending message...)
				if (!target.isRaid() && Formulas.calcAtkBreak(target, damage))
				{
					target.breakAttack();
					target.breakCast();
				}
				
				activeChar.sendDamageMessage(target, damage, false, false, false);
				
			}
			
			// activate attacked effects, if any
			target.stopSkillEffects(getId());
			getEffects(activeChar, target, false, sps, bss);
		}
		
		if (bss)
		{
			activeChar.removeBss();
		}
		else if (sps)
		{
			activeChar.removeSps();
		}
		
	}
}
