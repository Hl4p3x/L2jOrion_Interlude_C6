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
import l2jorion.game.model.L2Object;
import l2jorion.game.model.L2Skill;
import l2jorion.game.model.L2Skill.SkillType;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.actor.instance.L2PlayableInstance;
import l2jorion.game.skills.Formulas;

/*
 * Just a quick draft to support Wrath skill. Missing angle based calculation etc.
 */

public class CpDam implements ISkillHandler
{
	// private static Logger LOG = LoggerFactory.getLogger(Mdam.class);
	
	/*
	 * (non-Javadoc)
	 * @see l2jorion.game.handler.IItemHandler#useItem(l2jorion.game.model.L2PcInstance, l2jorion.game.model.L2ItemInstance)
	 */
	private static final SkillType[] SKILL_IDS =
	{
		SkillType.CPDAM
	};
	
	/*
	 * (non-Javadoc)
	 * @see l2jorion.game.handler.IItemHandler#useItem(l2jorion.game.model.L2PcInstance, l2jorion.game.model.L2ItemInstance)
	 */
	@Override
	public void useSkill(final L2Character activeChar, final L2Skill skill, final L2Object[] targets)
	{
		if (!(activeChar instanceof L2PlayableInstance))
		{
			// no cp damages for not playable instances
			return;
		}
		
		if (activeChar.isAlikeDead())
			return;
		
		/*
		 * boolean ss = false; boolean sps = false; boolean bss = false; L2ItemInstance weaponInst = activeChar.getActiveWeaponInstance(); if(weaponInst != null) { if(skill.isMagic()) { if(weaponInst.getChargedSpiritshot() == L2ItemInstance.CHARGED_BLESSED_SPIRITSHOT) { bss = true; } else
		 * if(weaponInst.getChargedSpiritshot() == L2ItemInstance.CHARGED_SPIRITSHOT) { sps = true; } } else if(weaponInst.getChargedSoulshot() == L2ItemInstance.CHARGED_SOULSHOT) { ss = true; } } // If there is no weapon equipped, check for an active summon. else if(activeChar instanceof L2Summon)
		 * { L2Summon activeSummon = (L2Summon) activeChar; if(activeSummon.getChargedSpiritShot() == L2ItemInstance.CHARGED_BLESSED_SPIRITSHOT) { bss = true; activeSummon.setChargedSpiritShot(L2ItemInstance.CHARGED_NONE); } else if(activeSummon.getChargedSpiritShot() ==
		 * L2ItemInstance.CHARGED_SPIRITSHOT) { ss = true; activeSummon.setChargedSpiritShot(L2ItemInstance.CHARGED_NONE); } activeSummon = null; } weaponInst = null;
		 */
		final boolean bss = activeChar.checkBss();
		final boolean sps = activeChar.checkSps();
		final boolean ss = activeChar.checkSs();
		
		for (final L2Object target2 : targets)
		{
			if (target2 == null)
				continue;
			
			L2Character target = (L2Character) target2;
			
			if (activeChar instanceof L2PcInstance && target instanceof L2PcInstance && target.isAlikeDead() && target.isFakeDeath())
			{
				target.stopFakeDeath(null);
			}
			else if (target.isAlikeDead())
			{
				continue;
			}
			
			if (target.isInvul())
			{
				continue;
			}
			
			if (!Formulas.getInstance().calcSkillSuccess(activeChar, target, skill, ss, sps, bss))
				return;
			
			final int damage = (int) (target.getCurrentCp() * (1 - skill.getPower()));
			
			// Manage attack or cast break of the target (calculating rate, sending message...)
			if (!target.isRaid() && Formulas.calcAtkBreak(target, damage))
			{
				target.breakAttack();
				target.breakCast();
			}
			skill.getEffects(activeChar, target, ss, sps, bss);
			activeChar.sendDamageMessage(target, damage, false, false, false);
			target.setCurrentCp(target.getCurrentCp() - damage);
			
			target = null;
		}
		
		if (skill.isMagic())
		{
			if (bss)
			{
				activeChar.removeBss();
			}
			else if (sps)
			{
				activeChar.removeSps();
			}
			
		}
		else
		{
			
			activeChar.removeSs();
			
		}
		
	}
	
	@Override
	public SkillType[] getSkillIds()
	{
		return SKILL_IDS;
	}
}
