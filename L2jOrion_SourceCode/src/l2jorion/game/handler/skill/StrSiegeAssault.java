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

import l2jorion.Config;
import l2jorion.game.handler.ISkillHandler;
import l2jorion.game.managers.CastleManager;
import l2jorion.game.managers.FortManager;
import l2jorion.game.model.L2Character;
import l2jorion.game.model.L2Object;
import l2jorion.game.model.L2Skill;
import l2jorion.game.model.L2Skill.SkillType;
import l2jorion.game.model.actor.instance.L2DoorInstance;
import l2jorion.game.model.actor.instance.L2ItemInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.entity.siege.Castle;
import l2jorion.game.model.entity.siege.Fort;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.SystemMessage;
import l2jorion.game.skills.Formulas;
import l2jorion.game.templates.L2WeaponType;

public class StrSiegeAssault implements ISkillHandler
{
	private static final SkillType[] SKILL_IDS =
	{
		SkillType.STRSIEGEASSAULT
	};
	
	@Override
	public void useSkill(final L2Character activeChar, final L2Skill skill, final L2Object[] targets)
	{
		
		if (activeChar == null || !(activeChar instanceof L2PcInstance))
		{
			return;
		}
		
		L2PcInstance player = (L2PcInstance) activeChar;
		
		if (!activeChar.isRiding())
		{
			return;
		}
		
		if (!(player.getTarget() instanceof L2DoorInstance))
		{
			return;
		}
		
		Castle castle = CastleManager.getInstance().getCastle(player);
		Fort fort = FortManager.getInstance().getFort(player);
		if ((castle == null) && (fort == null))
		{
			return;
		}
		
		if (castle != null)
		{
			if (!checkIfOkToUseStriderSiegeAssault(player, castle, true))
			{
				return;
			}
		}
		else
		{
			if (!checkIfOkToUseStriderSiegeAssault(player, fort, true))
			{
				return;
			}
		}
		
		castle = null;
		fort = null;
		
		try
		{
			L2ItemInstance itemToTake = player.getInventory().getItemByItemId(skill.getItemConsumeId());
			
			if (!player.destroyItem("Consume", itemToTake.getObjectId(), skill.getItemConsume(), null, true))
			{
				return;
			}
			
			itemToTake = null;
			
			// damage calculation
			int damage = 0;
			
			for (final L2Object target2 : targets)
			{
				if (target2 == null)
				{
					continue;
				}
				
				L2Character target = (L2Character) target2;
				L2ItemInstance weapon = activeChar.getActiveWeaponInstance();
				if (activeChar instanceof L2PcInstance && target instanceof L2PcInstance && target.isAlikeDead() && target.isFakeDeath())
				{
					target.stopFakeDeath(null);
				}
				else if (target.isAlikeDead())
				{
					continue;
				}
				
				final boolean dual = activeChar.isUsingDualWeapon();
				final boolean shld = Formulas.calcShldUse(activeChar, target);
				final boolean crit = Formulas.calcCrit(activeChar.getCriticalHit(target, skill));
				final boolean soul = (weapon != null && weapon.getChargedSoulshot() == L2ItemInstance.CHARGED_SOULSHOT && weapon.getItemType() != L2WeaponType.DAGGER);
				
				if (!crit && (skill.getCondition() & L2Skill.COND_CRIT) != 0)
				{
					damage = 0;
				}
				else
				{
					damage = (int) Formulas.calcPhysDam(activeChar, target, skill, shld, crit, dual, soul);
				}
				
				if (damage > 0)
				{
					target.reduceCurrentHp(damage, activeChar);
					if (soul && weapon != null)
					{
						weapon.setChargedSoulshot(L2ItemInstance.CHARGED_NONE);
					}
					
					activeChar.sendDamageMessage(target, damage, false, false, false);
					
				}
				else
				{
					activeChar.sendPacket(SystemMessage.sendString(skill.getName() + " failed."));
				}
			}
		}
		catch (final Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			
			player.sendMessage("Error using siege assault:" + e);
		}
		player = null;
	}
	
	@Override
	public SkillType[] getSkillIds()
	{
		return SKILL_IDS;
	}
	
	/**
	 * Return true if character clan place a flag<BR>
	 * <BR>
	 * @param activeChar The L2Character of the character placing the flag
	 * @param isCheckOnly if false, it will send a notification to the player telling him why it failed
	 * @return
	 */
	public static boolean checkIfOkToUseStriderSiegeAssault(final L2Character activeChar, final boolean isCheckOnly)
	{
		final Castle castle = CastleManager.getInstance().getCastle(activeChar);
		final Fort fort = FortManager.getInstance().getFort(activeChar);
		
		if ((castle == null) && (fort == null))
		{
			return false;
		}
		
		if (castle != null)
		{
			return checkIfOkToUseStriderSiegeAssault(activeChar, castle, isCheckOnly);
		}
		return checkIfOkToUseStriderSiegeAssault(activeChar, fort, isCheckOnly);
	}
	
	public static boolean checkIfOkToUseStriderSiegeAssault(final L2Character activeChar, final Castle castle, final boolean isCheckOnly)
	{
		if (activeChar == null || !(activeChar instanceof L2PcInstance))
		{
			return false;
		}
		
		SystemMessage sm = new SystemMessage(SystemMessageId.S1_S2);
		L2PcInstance player = (L2PcInstance) activeChar;
		
		if (castle == null || castle.getCastleId() <= 0)
		{
			sm.addString("You must be on castle ground to use strider siege assault");
		}
		else if (!castle.getSiege().getIsInProgress())
		{
			sm.addString("You can only use strider siege assault during a siege.");
		}
		else if (!(player.getTarget() instanceof L2DoorInstance))
		{
			sm.addString("You can only use strider siege assault on doors and walls.");
		}
		else if (!activeChar.isRiding())
		{
			sm.addString("You can only use strider siege assault when on strider.");
		}
		else
		{
			return true;
		}
		
		if (!isCheckOnly)
		{
			player.sendPacket(sm);
		}
		
		return false;
	}
	
	public static boolean checkIfOkToUseStriderSiegeAssault(final L2Character activeChar, final Fort fort, final boolean isCheckOnly)
	{
		if (activeChar == null || !(activeChar instanceof L2PcInstance))
		{
			return false;
		}
		
		SystemMessage sm = new SystemMessage(SystemMessageId.S1_S2);
		L2PcInstance player = (L2PcInstance) activeChar;
		
		if (fort == null || fort.getFortId() <= 0)
		{
			sm.addString("You must be on fort ground to use strider siege assault");
		}
		else if (!fort.getSiege().getIsInProgress())
		{
			sm.addString("You can only use strider siege assault during a siege.");
		}
		else if (!(player.getTarget() instanceof L2DoorInstance))
		{
			sm.addString("You can only use strider siege assault on doors and walls.");
		}
		else if (!activeChar.isRiding())
		{
			sm.addString("You can only use strider siege assault when on strider.");
		}
		else
		{
			return true;
		}
		
		if (!isCheckOnly)
		{
			player.sendPacket(sm);
			player = null;
			sm = null;
		}
		
		player = null;
		sm = null;
		
		return false;
	}
}
