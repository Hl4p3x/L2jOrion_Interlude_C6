/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package l2jorion.game.handler.voice;

import l2jorion.game.datatables.sql.ItemTable;
import l2jorion.game.handler.IVoicedCommandHandler;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.entity.event.CTF;

public class Event_CTF implements IVoicedCommandHandler
{
	private static final String[] VOICED_COMMANDS = { "ctfjoin", "ctfleave", "ctfinfo" };

	@Override
	public boolean useVoicedCommand(String command, L2PcInstance activeChar, String target)
	{
		if(command.startsWith("ctfjoin"))
		{
			JoinCTF(activeChar);
		}
		else if(command.startsWith("ctfleave"))
		{
			LeaveCTF(activeChar);
		}

		else if(command.startsWith("ctfinfo"))
		{
			CTFinfo(activeChar);
		}

		return true;
	}

	@Override
	public String[] getVoicedCommandList()
	{
		return VOICED_COMMANDS;
	}

	public boolean JoinCTF (L2PcInstance activeChar)
	{
		if(activeChar == null)
		{
			return false;
		}

		if(!CTF.is_joining())
		{
			activeChar.sendMessage("There is no CTF Event in progress.");
			return false;
		}
		else if(CTF.is_joining() && activeChar._inEventCTF)
		{
			activeChar.sendMessage("You are already registered.");
			return false;
		}
		else if(activeChar.isCursedWeaponEquipped())
		{
			activeChar.sendMessage("You are not allowed to participate to the event because you are holding a Cursed Weapon.");
			return false;
		}
		else if(activeChar.isInOlympiadMode())
		{
			activeChar.sendMessage("You are not allowed to participate to the event because you are in Olympiad.");
			return false;
		}
		else if(activeChar.getLevel() < CTF.get_minlvl())
		{
			activeChar.sendMessage("You are not allowed to participate to the event because your level is too low.");
			return false;
		}
		else if(activeChar.getLevel() > CTF.get_maxlvl())
		{
			activeChar.sendMessage("You are not allowed to participate to the event because your level is too high.");
			return false;
		}
		else if(activeChar.getKarma() > 0)
		{
			activeChar.sendMessage("You are not allowed to participate to the event because you have Karma.");
			return false;
		}
		else if(CTF.is_teleport() || CTF.is_started())
		{
			activeChar.sendMessage("CTF Event registration period is over. You can't register now.");
			return false;
		}
		else
		{
			activeChar.sendMessage("Your participation in the CTF event has been approved.");
			CTF.addPlayer(activeChar,"");
			return true;
		}
	}

	public boolean LeaveCTF (L2PcInstance activeChar)
	{
		if(activeChar == null)
		{
			return false;
		}

		if(!CTF.is_joining())
		{
			activeChar.sendMessage("There is no CTF Event in progress.");
			return false;
		}
		else if((CTF.is_teleport() || CTF.is_started()) && activeChar._inEventCTF)
		{
			activeChar.sendMessage("You can not leave now because CTF event has started.");
			return false;
		}
		else if(CTF.is_joining() && !activeChar._inEventCTF)
		{
			activeChar.sendMessage("You aren't registered in the CTF Event.");
			return false;
		}
		else
		{
			CTF.removePlayer(activeChar);
			return true;
		}
	}
	public boolean CTFinfo (L2PcInstance activeChar)
	{
		if(activeChar == null)
		{
			return false;
		}

		if(!CTF.is_joining())
		{
			activeChar.sendMessage("There is no CTF Event in progress.");
			return false;
		}
		else if(CTF.is_teleport() || CTF.is_started())
		{
			activeChar.sendMessage("I can't provide you this info. Command available only in joining period.");
			return false;
		}
		else
		{
			if(CTF._playersShuffle.size() == 1)
			{
				activeChar.sendMessage("There is " + CTF._playersShuffle.size() + " player participating in this event.");
				activeChar.sendMessage("Reward: " + CTF.get_rewardAmount() + " " + ItemTable.getInstance().getTemplate(CTF.get_rewardId()).getName()+ " !");
				activeChar.sendMessage("Player Min lvl: " + CTF.get_minlvl() + ".");
				activeChar.sendMessage("Player Max lvl: " + CTF.get_maxlvl() + ".");
			}
			else
			{
				activeChar.sendMessage("There are " + CTF._playersShuffle.size() + " players participating in this event.");
				activeChar.sendMessage("Reward: " + CTF.get_rewardAmount() + " " + ItemTable.getInstance().getTemplate(CTF.get_rewardId()).getName()+ " !");
				activeChar.sendMessage("Player Min lvl: " + CTF.get_minlvl() + ".");
				activeChar.sendMessage("Player Max lvl: " + CTF.get_maxlvl() + ".");
			}
			return true;
		}
	}
}