/*
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
package l2jorion.game.handler.user;

import l2jorion.Config;
import l2jorion.game.ai.CtrlIntention;
import l2jorion.game.controllers.GameTimeController;
import l2jorion.game.datatables.csv.MapRegionTable;
import l2jorion.game.handler.IUserCommandHandler;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.entity.event.CTF;
import l2jorion.game.model.entity.event.DM;
import l2jorion.game.model.entity.event.TvT;
import l2jorion.game.model.entity.event.VIP;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.MagicSkillUser;
import l2jorion.game.network.serverpackets.PlaySound;
import l2jorion.game.network.serverpackets.SetupGauge;
import l2jorion.game.network.serverpackets.SystemMessage;
import l2jorion.game.thread.ThreadPoolManager;

public class Escape implements IUserCommandHandler
{
	private static final int[] COMMAND_IDS =
	{
		52
	};
	
	@Override
	public boolean useUserCommand(int id, L2PcInstance activeChar)
	{
		if (activeChar.isFestivalParticipant())
		{
			activeChar.sendMessage("You may not use an escape command in a festival.");
			return false;
		}
		
		if (activeChar._inEventTvT && TvT.is_started())
		{
			activeChar.sendMessage("You may not use an escape skill in TvT.");
			return false;
		}
		
		if (activeChar._inEventCTF && CTF.is_started())
		{
			activeChar.sendMessage("You may not use an escape skill in CTF.");
			return false;
		}
		
		if (activeChar._inEventDM && DM.is_started())
		{
			activeChar.sendMessage("You may not use an escape skill in DM.");
			return false;
		}
		
		if (activeChar._inEventVIP && VIP._started)
		{
			activeChar.sendMessage("You may not use an escape skill in VIP.");
			return false;
		}
		
		if (activeChar.isInJail())
		{
			activeChar.sendMessage("You can not escape from jail.");
			return false;
		}
		
		if (activeChar.isInFunEvent())
		{
			activeChar.sendMessage("You may not escape from an Event.");
			return false;
		}
		
		if (activeChar.inObserverMode())
		{
			activeChar.sendMessage("You may not escape during Observer mode.");
			return false;
		}
		
		if (activeChar.isSitting())
		{
			activeChar.sendMessage("You may not escape when you sitting.");
			return false;
		}
		
		if (activeChar.isCastingNow() || activeChar.isOutOfControl() || activeChar.isMovementDisabled() || activeChar.isMuted() || activeChar.isAlikeDead() || activeChar.isInOlympiadMode())
		{
			activeChar.sendPacket(SystemMessageId.NO_UNSTUCK_PLEASE_SEND_PETITION);
			return false;
		}
		
		int unstuckTimer = activeChar.getAccessLevel().isGm() ? 1000 : Config.UNSTUCK_INTERVAL * 1000;
		if (unstuckTimer < 300000)
		{
			if (!Config.RON_CUSTOM)
			{
				activeChar.sendPacket(new PlaySound("systemmsg_e.809"));
			}
			
			SystemMessage sm = new SystemMessage(SystemMessageId.S1_S2);
			SystemMessage sm2 = new SystemMessage(SystemMessageId.S1_S2);
			
			if (unstuckTimer < 60000)
			{
				sm.addString("You are stuck. You will be transported to the nearest village in " + unstuckTimer / 1000 + " seconds.");
				sm2.addString("You use Escape: " + unstuckTimer / 1000 + " seconds.");
			}
			else
			{
				sm.addString("You are stuck. You will be transported to the nearest village in  " + unstuckTimer / 60000 + " minutes.");
				sm2.addString("You use Escape: " + unstuckTimer / 60000 + " minutes.");
			}
			
			activeChar.sendPacket(sm);
			activeChar.sendPacket(sm2);
		}
		else
		{
			activeChar.sendPacket(SystemMessageId.STUCK_TRANSPORT_IN_FIVE_MINUTES);
			activeChar.sendPacket(new SystemMessage(SystemMessageId.S1_S2).addString("You use Escape: 5 minutes."));
		}
		
		activeChar.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
		
		activeChar.setTarget(activeChar);
		activeChar.broadcastPacket(new MagicSkillUser(activeChar, 1050, 1, unstuckTimer, 0));
		activeChar.sendPacket(new SetupGauge(0, unstuckTimer));
		activeChar.setTarget(null);
		
		activeChar.setSkillCast(ThreadPoolManager.getInstance().scheduleEffect(new EscapeFinalizer(activeChar), unstuckTimer));
		activeChar.setSkillCastEndTime(10 + GameTimeController.getInstance().getGameTicks() + unstuckTimer / GameTimeController.MILLIS_IN_TICK);
		
		return true;
	}
	
	static class EscapeFinalizer implements Runnable
	{
		private L2PcInstance _activeChar;
		
		EscapeFinalizer(L2PcInstance activeChar)
		{
			_activeChar = activeChar;
		}
		
		@Override
		public void run()
		{
			if (_activeChar.isDead())
			{
				return;
			}
			
			_activeChar.setIsIn7sDungeon(false);
			
			try
			{
				if (_activeChar.getKarma() > 0 && Config.ALT_KARMA_TELEPORT_TO_FLORAN)
				{
					_activeChar.teleToLocation(17836, 170178, -3507, true); // Floran
					return;
				}
				
				_activeChar.teleToLocation(MapRegionTable.TeleportWhereType.Town);
			}
			catch (Throwable e)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
				{
					e.printStackTrace();
				}
			}
		}
	}
	
	@Override
	public int[] getUserCommandList()
	{
		return COMMAND_IDS;
	}
}
