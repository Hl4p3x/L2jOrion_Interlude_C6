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
import l2jorion.game.datatables.csv.MapRegionTable;
import l2jorion.game.handler.ISkillHandler;
import l2jorion.game.model.L2Character;
import l2jorion.game.model.L2Object;
import l2jorion.game.model.L2Skill;
import l2jorion.game.model.L2Skill.SkillType;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.entity.event.CTF;
import l2jorion.game.model.entity.event.DM;
import l2jorion.game.model.entity.event.TvT;
import l2jorion.game.model.entity.event.VIP;
import l2jorion.game.model.zone.ZoneId;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.SystemMessage;

public class Recall implements ISkillHandler
{
	private static final SkillType[] SKILL_IDS =
	{
		SkillType.RECALL
	};
	
	@Override
	public void useSkill(final L2Character activeChar, final L2Skill skill, final L2Object[] targets)
	{
		try
		{
			if (activeChar instanceof L2PcInstance)
			{
				final L2PcInstance player = (L2PcInstance) activeChar;
				
				if (player.isInOlympiadMode())
				{
					player.sendPacket(new SystemMessage(SystemMessageId.YOU_MAY_NOT_SUMMON_FROM_YOUR_CURRENT_LOCATION));
					return;
				}
				
				if (player.isInsideZone(ZoneId.ZONE_PVP) || player.isInsideZone(ZoneId.ZONE_SIEGE) || player.isInsideZone(ZoneId.ZONE_NOSUMMONFRIEND) || player.isInsideZone(ZoneId.ZONE_JAIL))
				{
					player.sendPacket(new SystemMessage(SystemMessageId.YOU_MAY_NOT_SUMMON_FROM_YOUR_CURRENT_LOCATION));
					return;
				}
			}
			
			for (final L2Object target1 : targets)
			{
				if (!(target1 instanceof L2Character))
				{
					continue;
				}
				
				L2Character target = (L2Character) target1;
				
				if (target instanceof L2PcInstance)
				{
					final L2PcInstance targetChar = (L2PcInstance) target;
					
					if ((targetChar._inEventCTF && CTF.is_started()) || (targetChar._inEventTvT && TvT.is_started()) || (targetChar._inEventDM && DM.is_started()) || (targetChar._inEventVIP && VIP._started))
					{
						targetChar.sendMessage("You can't use escape skill in Event.");
						continue;
					}
					
					if (targetChar.isInDuel())
					{
						targetChar.sendPacket(SystemMessage.sendString("You can't use escape skills during a duel."));
						continue;
					}
					
					if (targetChar.isAlikeDead())
					{
						SystemMessage sm = new SystemMessage(SystemMessageId.S1_IS_DEAD_AT_THE_MOMENT_AND_CANNOT_BE_SUMMONED);
						sm.addString(targetChar.getName());
						activeChar.sendPacket(sm);
						continue;
					}
					
					if (targetChar.isInStoreMode())
					{
						SystemMessage sm = new SystemMessage(SystemMessageId.S1_CURRENTLY_TRADING_OR_OPERATING_PRIVATE_STORE_AND_CANNOT_BE_SUMMONED);
						sm.addString(targetChar.getName());
						activeChar.sendPacket(sm);
						continue;
					}
					
					if (targetChar.isInOlympiadMode())
					{
						activeChar.sendPacket(new SystemMessage(SystemMessageId.YOU_CANNOT_SUMMON_PLAYERS_WHO_ARE_IN_OLYMPIAD));
						continue;
					}
					
					if (targetChar.isFestivalParticipant() || targetChar.isInsideZone(ZoneId.ZONE_PVP) || targetChar.isInsideZone(ZoneId.ZONE_JAIL) || targetChar.isInsideZone(ZoneId.ZONE_NOSUMMONFRIEND))
					{
						activeChar.sendPacket(new SystemMessage(SystemMessageId.YOUR_TARGET_IS_IN_AN_AREA_WHICH_BLOCKS_SUMMONING));
						continue;
					}
				}
				
				target.teleToLocation(MapRegionTable.TeleportWhereType.Town);
			}
			
			if (skill.isMagic() && skill.useSpiritShot())
			{
				if (activeChar.checkBss())
				{
					activeChar.removeBss();
				}
				if (activeChar.checkSps())
				{
					activeChar.removeSps();
				}
			}
			else if (skill.useSoulShot())
			{
				if (activeChar.checkSs())
				{
					activeChar.removeSs();
				}
			}
		}
		catch (final Throwable e)
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
}