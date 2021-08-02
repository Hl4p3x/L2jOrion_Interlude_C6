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
import l2jorion.game.datatables.sql.NpcTable;
import l2jorion.game.handler.ISkillHandler;
import l2jorion.game.idfactory.IdFactory;
import l2jorion.game.managers.CastleManager;
import l2jorion.game.managers.FortManager;
import l2jorion.game.managers.FortSiegeManager;
import l2jorion.game.managers.SiegeManager;
import l2jorion.game.model.L2Character;
import l2jorion.game.model.L2Object;
import l2jorion.game.model.L2Skill;
import l2jorion.game.model.L2Skill.SkillType;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.actor.instance.L2SiegeFlagInstance;
import l2jorion.game.model.entity.siege.Castle;
import l2jorion.game.model.entity.siege.Fort;
import l2jorion.game.model.zone.ZoneId;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.SystemMessage;

public class SiegeFlag implements ISkillHandler
{
	private static final SkillType[] SKILL_IDS =
	{
		SkillType.SIEGEFLAG
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
		if ((castle == null) && (fort == null))
		{
			return;
		}
		
		if (castle != null)
		{
			if (!checkIfOkToPlaceFlag(player, castle, true))
			{
				return;
			}
		}
		else
		{
			if (!checkIfOkToPlaceFlag(player, fort, true))
			{
				return;
			}
		}
		
		try
		{
			// Spawn a new flag
			L2SiegeFlagInstance flag = new L2SiegeFlagInstance(player, IdFactory.getInstance().getNextId(), NpcTable.getInstance().getTemplate(35062));
			
			if (skill.is_advancedFlag())
			{
				flag.set_advanceFlag(true);
				flag.set_advanceMultiplier(skill.get_advancedMultiplier());
			}
			
			flag.setTitle(player.getClan().getName());
			flag.setCurrentHpMp(flag.getMaxHp(), flag.getMaxMp());
			flag.setHeading(player.getHeading());
			flag.spawnMe(player.getX(), player.getY(), player.getZ() + 50);
			
			if (castle != null)
			{
				castle.getSiege().getFlag(player.getClan()).add(flag);
			}
			else
			{
				fort.getSiege().getFlag(player.getClan()).add(flag);
			}
			
			flag = null;
		}
		catch (final Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			
			player.sendMessage("Error placing flag:" + e);
		}
	}
	
	@Override
	public SkillType[] getSkillIds()
	{
		return SKILL_IDS;
	}
	
	public static boolean checkIfOkToPlaceFlag(final L2Character activeChar, final boolean isCheckOnly)
	{
		final Castle castle = CastleManager.getInstance().getCastle(activeChar);
		final Fort fort = FortManager.getInstance().getFort(activeChar);
		if ((castle == null) && (fort == null))
		{
			return false;
		}
		
		if (castle != null)
		{
			return checkIfOkToPlaceFlag(activeChar, castle, isCheckOnly);
		}
		return checkIfOkToPlaceFlag(activeChar, fort, isCheckOnly);
	}
	
	public static boolean checkIfOkToPlaceFlag(final L2Character activeChar, final Castle castle, final boolean isCheckOnly)
	{
		if (activeChar == null || !(activeChar instanceof L2PcInstance))
		{
			return false;
		}
		
		final SystemMessage sm = new SystemMessage(SystemMessageId.S1_S2);
		final L2PcInstance player = (L2PcInstance) activeChar;
		
		if (castle == null || castle.getCastleId() <= 0)
		{
			sm.addString("You must be on castle ground to place a flag.");
		}
		else if (!castle.getSiege().getIsInProgress())
		{
			sm.addString("You can only place a flag during a siege.");
		}
		else if (castle.getSiege().getAttackerClan(player.getClan()) == null)
		{
			sm.addString("You must be an attacker to place a flag.");
		}
		else if (player.getClan() == null || !player.isClanLeader())
		{
			sm.addString("You must be a clan leader to place a flag.");
		}
		else if (castle.getSiege().getAttackerClan(player.getClan()).getNumFlags() >= SiegeManager.getInstance().getFlagMaxCount())
		{
			sm.addString("You have already placed the maximum number of flags possible.");
		}
		else if (!player.isInsideZone(ZoneId.ZONE_HQ))
		{
			sm.addString("You cannot place flag here.");
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
	
	public static boolean checkIfOkToPlaceFlag(final L2Character activeChar, final Fort fort, final boolean isCheckOnly)
	{
		if (activeChar == null || !(activeChar instanceof L2PcInstance))
		{
			return false;
		}
		
		final SystemMessage sm = new SystemMessage(SystemMessageId.S1_S2);
		final L2PcInstance player = (L2PcInstance) activeChar;
		
		if (fort == null || fort.getFortId() <= 0)
		{
			sm.addString("You must be on fort ground to place a flag");
		}
		else if (!fort.getSiege().getIsInProgress())
		{
			sm.addString("You can only place a flag during a siege.");
		}
		else if (fort.getSiege().getAttackerClan(player.getClan()) == null)
		{
			sm.addString("You must be an attacker to place a flag");
		}
		else if (player.getClan() == null || !player.isClanLeader())
		{
			sm.addString("You must be a clan leader to place a flag");
		}
		else if (fort.getSiege().getAttackerClan(player.getClan()).getNumFlags() >= FortSiegeManager.getInstance().getFlagMaxCount())
		{
			sm.addString("You have already placed the maximum number of flags possible");
		}
		else if (!player.isInsideZone(ZoneId.ZONE_HQ))
		{
			sm.addString("You cannot place flag here.");
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
}