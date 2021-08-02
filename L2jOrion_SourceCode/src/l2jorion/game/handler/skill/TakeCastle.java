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
import l2jorion.game.model.actor.instance.L2ArtefactInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.entity.siege.Castle;
import l2jorion.game.model.entity.siege.Fort;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.SystemMessage;
import l2jorion.game.util.Util;

public class TakeCastle implements ISkillHandler
{
	private static final SkillType[] SKILL_IDS =
	{
		SkillType.TAKECASTLE
	};
	
	@Override
	public void useSkill(final L2Character activeChar, final L2Skill skill, final L2Object[] targets)
	{
		if (activeChar == null || !(activeChar instanceof L2PcInstance))
		{
			return;
		}
		
		L2PcInstance player = (L2PcInstance) activeChar;
		
		if (player.getClan() == null || player.getClan().getLeaderId() != player.getObjectId())
		{
			return;
		}
		
		Castle castle = CastleManager.getInstance().getCastle(player);
		Fort fort = FortManager.getInstance().getFort(player);
		
		if (castle != null && fort == null)
		{
			if (!checkIfOkToCastSealOfRule(player, castle, true))
			{
				return;
			}
			
		}
		else if (fort != null && castle == null)
		{
			if (!checkIfOkToCastFlagDisplay(player, fort, true))
			{
				return;
			}
		}
		
		try
		{
			if ((castle != null) && (targets[0] instanceof L2ArtefactInstance))
			{
				castle.Engrave(player.getClan(), targets[0].getObjectId());
			}
			else if (fort != null)
			{
				fort.EndOfSiege(player.getClan());
			}
		}
		catch (final Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
		}
	}
	
	@Override
	public SkillType[] getSkillIds()
	{
		return SKILL_IDS;
	}
	
	public static boolean checkIfOkToCastSealOfRule(final L2Character activeChar, final boolean isCheckOnly)
	{
		final Castle castle = CastleManager.getInstance().getCastle(activeChar);
		final Fort fort = FortManager.getInstance().getFort(activeChar);
		
		if (fort != null && castle == null)
		{
			return checkIfOkToCastFlagDisplay(activeChar, fort, isCheckOnly);
		}
		
		return checkIfOkToCastSealOfRule(activeChar, castle, isCheckOnly);
	}
	
	public static boolean checkIfOkToCastSealOfRule(final L2Character activeChar, final Castle castle, final boolean isCheckOnly)
	{
		if (activeChar == null || !(activeChar instanceof L2PcInstance))
		{
			return false;
		}
		
		SystemMessage sm = new SystemMessage(SystemMessageId.S1_S2);
		L2PcInstance player = (L2PcInstance) activeChar;
		
		if (castle == null || castle.getCastleId() <= 0)
		{
			sm.addString("You must be on castle ground to use this skill");
		}
		else if (player.getTarget() == null || !(player.getTarget() instanceof L2ArtefactInstance))
		{
			sm.addString("You can only use this skill on an artifact");
		}
		else if (!castle.getSiege().getIsInProgress())
		{
			sm.addString("You can only use this skill during a siege.");
		}
		else if (!Util.checkIfInRange(200, player, player.getTarget(), true))
		{
			sm.addString("You are not in range of the artifact.");
		}
		else if (castle.getSiege().getAttackerClan(player.getClan()) == null)
		{
			sm.addString("You must be an attacker to use this skill");
		}
		else
		{
			if (!isCheckOnly)
			{
				castle.getSiege().announceToPlayer("Clan " + player.getClan().getName() + " has begun to engrave the ruler.", true);
			}
			return true;
		}
		
		if (!isCheckOnly)
		{
			player.sendPacket(sm);
		}
		
		return false;
	}
	
	public static boolean checkIfOkToCastFlagDisplay(final L2Character activeChar, final boolean isCheckOnly)
	{
		return checkIfOkToCastFlagDisplay(activeChar, FortManager.getInstance().getFort(activeChar), isCheckOnly);
	}
	
	public static boolean checkIfOkToCastFlagDisplay(final L2Character activeChar, final Fort fort, final boolean isCheckOnly)
	{
		if (activeChar == null || !(activeChar instanceof L2PcInstance))
		{
			return false;
		}
		
		SystemMessage sm = new SystemMessage(SystemMessageId.S1_S2);
		L2PcInstance player = (L2PcInstance) activeChar;
		
		if (fort == null || fort.getFortId() <= 0)
		{
			sm.addString("You must be on fort ground to use this skill");
		}
		else if (player.getTarget() == null && !(player.getTarget() instanceof L2ArtefactInstance))
		{
			sm.addString("You can only use this skill on an flagpole");
		}
		else if (!fort.getSiege().getIsInProgress())
		{
			sm.addString("You can only use this skill during a siege.");
		}
		else if (!Util.checkIfInRange(200, player, player.getTarget(), true))
		{
			sm.addString("You are not in range of the flagpole.");
		}
		else if (fort.getSiege().getAttackerClan(player.getClan()) == null)
		{
			sm.addString("You must be an attacker to use this skill");
		}
		else
		{
			if (!isCheckOnly)
			{
				fort.getSiege().announceToPlayer("Clan " + player.getClan().getName() + " has begun to raise flag.", true);
			}
			
			return true;
		}
		
		if (!isCheckOnly)
		{
			player.sendPacket(sm);
		}
		
		return false;
	}
}
