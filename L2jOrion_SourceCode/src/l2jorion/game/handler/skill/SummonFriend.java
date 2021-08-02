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
import l2jorion.game.network.serverpackets.ConfirmDlg;
import l2jorion.game.network.serverpackets.SystemMessage;
import l2jorion.game.util.Util;

public class SummonFriend implements ISkillHandler
{
	private static final SkillType[] SKILL_IDS =
	{
		SkillType.SUMMON_FRIEND
	};
	
	@Override
	public void useSkill(final L2Character activeChar, final L2Skill skill, final L2Object[] targets)
	{
		boolean bss = activeChar.checkBss();
		boolean sps = activeChar.checkSps();
		
		if (!(activeChar instanceof L2PcInstance))
		{
			return;
		}
		
		L2PcInstance activePlayer = (L2PcInstance) activeChar;
		
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
		
		if (!L2PcInstance.checkSummonerStatus(activePlayer))
		{
			return;
		}
		
		if (activePlayer._inEvent)
		{
			activePlayer.sendMessage("You cannot use this skill in Event.");
			return;
		}
		if (activePlayer._inEventCTF && CTF.is_started())
		{
			activePlayer.sendMessage("You cannot use this skill in Event.");
			return;
		}
		if (activePlayer._inEventDM && DM.is_started())
		{
			activePlayer.sendMessage("You cannot use this skill in Event.");
			return;
		}
		if (activePlayer._inEventTvT && TvT.is_started())
		{
			activePlayer.sendMessage("You cannot use this skill in Event.");
			return;
		}
		if (activePlayer._inEventVIP && VIP._started)
		{
			activePlayer.sendMessage("You cannot use this skill in Event.");
			return;
		}
		
		if (activePlayer.isInsideZone(ZoneId.ZONE_NOSUMMONFRIEND))
		{
			activePlayer.sendPacket(new SystemMessage(SystemMessageId.YOU_MAY_NOT_SUMMON_FROM_YOUR_CURRENT_LOCATION));
			return;
		}
		
		try
		{
			for (final L2Object obj : targets)
			{
				if (!(obj instanceof L2Character))
				{
					continue;
				}
				
				L2Character target = (L2Character) obj;
				if (activeChar == target)
				{
					continue;
				}
				
				if (target instanceof L2PcInstance)
				{
					L2PcInstance targetChar = (L2PcInstance) target;
					
					if (!L2PcInstance.checkSummonTargetStatus(targetChar, activePlayer))
					{
						continue;
					}
					
					if (targetChar._inEvent)
					{
						targetChar.sendMessage("You cannot use this skill in a Event.");
						return;
					}
					if (targetChar._inEventCTF)
					{
						targetChar.sendMessage("You cannot use this skill in a Event.");
						return;
					}
					if (targetChar._inEventDM)
					{
						targetChar.sendMessage("You cannot use this skill in a Event.");
						return;
					}
					if (targetChar._inEventTvT)
					{
						targetChar.sendMessage("You cannot use this skill in a Event.");
						return;
					}
					if (targetChar._inEventVIP)
					{
						targetChar.sendMessage("You cannot use this skill in a Event.");
						return;
					}
					
					// Requires a Summoning Crystal
					if ((targetChar.getInventory().getItemByItemId(8615) == null) && (skill.getId() != 1429))
					{
						((L2PcInstance) activeChar).sendMessage("Your target cannot be summoned while he hasn't got a Summoning Crystal.");
						targetChar.sendMessage("You cannot be summoned while you haven't got a Summoning Crystal.");
						continue;
					}
					
					if (!Util.checkIfInRange(100, activeChar, target, false))
					{
						if (!targetChar.teleportRequest((L2PcInstance) activeChar, skill))
						{
							final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S1_ALREADY_SUMMONED);
							sm.addString(target.getName());
							activeChar.sendPacket(sm);
							continue;
						}
						
						if (skill.getId() == 1403)
						{
							final ConfirmDlg confirm = new ConfirmDlg(SystemMessageId.S1_WISHES_TO_SUMMON_YOU_FROM_S2_DO_YOU_ACCEPT.getId());
							confirm.addString(activeChar.getName());
							confirm.addZoneName(activeChar.getX(), activeChar.getY(), activeChar.getZ());
							confirm.addTime(30000);
							confirm.addRequesterId(activeChar.getObjectId());
							targetChar.sendPacket(confirm);
						}
						else
						{
							L2PcInstance.teleToTarget(targetChar, (L2PcInstance) activeChar, skill);
							targetChar.teleportRequest(null, null);
						}
					}
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