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

import l2jorion.game.ai.CtrlIntention;
import l2jorion.game.controllers.GameTimeController;
import l2jorion.game.handler.IVoicedCommandHandler;
import l2jorion.game.model.L2Party;
import l2jorion.game.model.Location;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.zone.ZoneId;
import l2jorion.game.network.serverpackets.MagicSkillUser;
import l2jorion.game.network.serverpackets.SetupGauge;
import l2jorion.game.thread.ThreadPoolManager;

/**
 * @author Vilmis
 */
public class PartyTeleport implements IVoicedCommandHandler
{
	private class EscapeFinalizer implements Runnable
	{
		L2PcInstance _player;
		Location _tp;
		
		public EscapeFinalizer(L2PcInstance player, Location loc)
		{
			_player = player;
			_tp = loc;
		}
		
		@Override
		public void run()
		{
			_player.enableAllSkills();
			_player.teleToLocation(_tp.getX(), _tp.getY(), _tp.getZ(), true);
		}
		
	}
	
	private Location[] locations =
	{
		new Location(9775, 15638, -4569), // delven
		new Location(115094, -178194, -896), // dwarven
		new Location(46925, 51512, -2972), // elven
		new Location(-45130, -112450, -240), // orc
		new Location(-84214, 244562, -3730), // tisland
		new Location(83367, 147968, -3405), // giran
		new Location(147923, -55322, -2728), // goddard
		new Location(146833, 25816, -2013), // aden
		new Location(43815, -47742, -797), // rune
		new Location(-12697, 122747, -3117), // gludio
		new Location(111387, 219357, -3546), // heine
		new Location(15636, 142976, -2706), // dion
		new Location(87110, -143351, -1293), // shuttgart
		new Location(82902, 53241, -1496), // oren
		new Location(117050, 76869, -2704), // hunters
		new Location(-80882, 149736, -3038) // gludin
	};
	
	private static String[] _voicedCommands =
	{
		"delven",
		"dwarven",
		"elven",
		"orc",
		"tisland",
		"giran",
		"goddard",
		"aden",
		"rune",
		"gludio",
		"heine",
		"dion",
		"shuttgart",
		"oren",
		"hunters",
		"gludin",
	};
	
	@Override
	public String[] getVoicedCommandList()
	{
		return _voicedCommands;
	}
	
	@Override
	public boolean useVoicedCommand(String command, L2PcInstance player, String target)
	{
		if (player == null)
		{
			return false;
		}
		
		Location loc = null;
		
		switch (command)
		{
			case "delven":
				loc = locations[0];
				break;
			case "dwarven":
				loc = locations[1];
				break;
			case "elven":
				loc = locations[2];
				break;
			case "orc":
				loc = locations[3];
				break;
			case "tisland":
				loc = locations[4];
				break;
			case "giran":
				loc = locations[5];
				break;
			case "goddard":
				loc = locations[6];
				break;
			case "aden":
				loc = locations[7];
				break;
			case "rune":
				loc = locations[8];
				break;
			case "gludio":
				loc = locations[9];
				break;
			case "heine":
				loc = locations[10];
				break;
			case "dion":
				loc = locations[11];
				break;
			case "shuttgart":
				loc = locations[12];
				break;
			case "oren":
				loc = locations[13];
				break;
			case "hunters":
				loc = locations[14];
				break;
			case "gludin":
				loc = locations[15];
				break;
		}
		
		if (loc == null)
		{
			return false;
		}
		
		L2Party party = player.getParty();
		int unstuckTimer = 10000;
		
		if (party != null)
		{
			if (player.getPremiumService() == 0)
			{
				player.sendMessage("You're not premium account.");
				return false;
			}
			
			if (!player.getParty().isLeader(player))
			{
				player.sendMessage("You're not a leader of party.");
				return false;
			}
			
			for (L2PcInstance member : party.getPartyMembers())
			{
				if (member == null)
				{
					continue;
				}
				
				if (!(member.isInsideZone(ZoneId.ZONE_PEACE)))
				{
					member.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
					
					member.setTarget(member);
					member.broadcastPacket(new MagicSkillUser(member, 1050, 1, unstuckTimer, 0));
					member.sendPacket(new SetupGauge(0, unstuckTimer));
					member.setTarget(null);
					
					member.setSkillCast(ThreadPoolManager.getInstance().scheduleEffect(new EscapeFinalizer(member, loc), unstuckTimer));
					member.setSkillCastEndTime(10 + GameTimeController.getInstance().getGameTicks() + unstuckTimer / GameTimeController.MILLIS_IN_TICK);
				}
				else
				{
					member.teleToLocation(loc, true);
				}
				
				if (party.isLeader(member))
				{
					player.sendMessage("Town commands:");
					player.sendMessage(".delven .dwarven .elven .orc .tisland .giran .goddard .aden .rune");
					player.sendMessage(".gludio .heine .dion .shuttgart .oren .hunters .gludin");
				}
				else
				{
					member.sendMessage("Your party leader is teleporting you.");
				}
			}
			
			return false;
		}
		
		// Not party
		if (unstuckTimer > 0 && !(player.isInsideZone(ZoneId.ZONE_PEACE)))
		{
			player.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
			
			player.setTarget(player);
			player.broadcastPacket(new MagicSkillUser(player, 1050, 1, unstuckTimer, 0));
			player.sendPacket(new SetupGauge(0, unstuckTimer));
			player.setTarget(null);
			
			player.setSkillCast(ThreadPoolManager.getInstance().scheduleEffect(new EscapeFinalizer(player, loc), unstuckTimer));
			player.setSkillCastEndTime(10 + GameTimeController.getInstance().getGameTicks() + unstuckTimer / GameTimeController.MILLIS_IN_TICK);
			return false;
		}
		
		player.teleToLocation(loc, true);
		
		player.sendMessage("Town commands:");
		player.sendMessage(".delven .dwarven .elven .orc .tisland .giran .goddard .aden .rune");
		player.sendMessage(".gludio .heine .dion .shuttgart .oren .hunters .gludin");
		return true;
	}
}
