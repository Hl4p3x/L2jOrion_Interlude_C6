/*
 * L2jLifeDrain Project - www.lifedrain.net 
 * 
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
package l2jorion.game.handler.admin;

import static l2jorion.game.ai.CtrlIntention.AI_INTENTION_IDLE;

import java.util.StringTokenizer;

import l2jorion.game.ai.phantom.phantomPlayers;
import l2jorion.game.handler.IAdminCommandHandler;
import l2jorion.game.model.L2World;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.network.serverpackets.ExShowScreenMessage;
import l2jorion.game.network.serverpackets.NpcHtmlMessage;
import l2jorion.util.random.Rnd;

public class AdminPhantom implements IAdminCommandHandler
{
	//private static final Logger LOG = LoggerFactory.getLogger(phantomPlayers.class);
	
	private boolean reload = false;
	
	
	private static String[] ADMIN_COMMANDS =
	{
		"admin_phantom",
		"admin_phantom_Stop",
		"admin_phantom_Start",
		"admin_phantom_Reload",
		"admin_phantom_Reset",
		"admin_phantom_Recall",
		"admin_phantom_Kill",
		"admin_phantom_Res",
		"admin_phantom_Give_Flag",
		"admin_phantom_Remove_Flag",
		"admin_phantom_spawn",
	};
	
	private enum CommandEnum
	{
		admin_phantom,
		admin_phantom_Stop,
		admin_phantom_Start,
		admin_phantom_Reload,
		admin_phantom_Reset,
		admin_phantom_Recall,
		admin_phantom_Kill,
		admin_phantom_Res,
		admin_phantom_Give_Flag,
		admin_phantom_Remove_Flag,
		admin_phantom_spawn,
	}
	
	@Override
	public boolean useAdminCommand(String command, L2PcInstance activeChar)
	{
		StringTokenizer st = new StringTokenizer(command);
		CommandEnum comm = CommandEnum.valueOf(st.nextToken());
		
		if (comm == null)
		{
			return false;
		}
		
		switch (comm)
		{
			case admin_phantom:
				htm(activeChar, null, "phantom.htm");
				break;
			case admin_phantom_Stop:
			{
				int count = 0;
				for (L2PcInstance phantom : L2World.getInstance().getAllPlayers().values())
				{
					if (phantom != null && phantom.isPhantom() && phantom.getPhantomAI() != null)
					{
						count++;
						phantom.stopPhantomAI();
					}
				}
				activeChar.sendMessage("Phantoms stopped: "+count);
				activeChar.sendPacket(new ExShowScreenMessage("Phantoms stopped: "+count, 3000, 2, true));
				htm(activeChar, null, "phantom.htm");
				break;
			}
			case admin_phantom_Start:
			{
				int count = 0;
				for (L2PcInstance phantom : L2World.getInstance().getAllPlayers().values())
				{
					if (phantom != null && phantom.isPhantom() && phantom.getPhantomAI() == null)
					{
						count++;
						phantom.startPhantomAI();
					}
				}
				activeChar.sendMessage("Phantoms started: "+count);
				activeChar.sendPacket(new ExShowScreenMessage("Phantoms started: "+count, 3000, 2, true));
				htm(activeChar, null, "phantom.htm");
				break;
			}
			case admin_phantom_Reset:
			{
				int count = 0;
				for (L2PcInstance phantom : L2World.getInstance().getAllPlayers().values())
				{
					if (phantom != null && phantom.isPhantom())
					{
						count++;
						phantom.getPhantomTargetList().clear();
						phantom.getAI().setIntention(AI_INTENTION_IDLE);
					}
				}
				activeChar.sendMessage(count+" Phantoms cache cleaned");
				activeChar.sendPacket(new ExShowScreenMessage(count+" Phantoms cache cleaned", 3000, 2, true));
				htm(activeChar, null, "phantom.htm");
				break;
			}
			case admin_phantom_Recall:
			{
				int count = 0;
				activeChar.sendMessage("Phantoms is teleporting... ");
				for (L2PcInstance phantom : L2World.getInstance().getAllPlayers().values())
				{
					if (phantom != null && phantom.isPhantom())
					{
						count++;
						phantom.teleToLocation(activeChar.getX()+Rnd.get(-300,300), activeChar.getY()+Rnd.get(-300,300), activeChar.getZ(), true);
						phantom.setMoveToPawn(true);
						phantom.getAI().setIntention(AI_INTENTION_IDLE);
					}
				}
				
				activeChar.sendMessage("Phantoms teleported: "+count);
				activeChar.sendPacket(new ExShowScreenMessage("Phantoms teleported: "+count, 3000, 2, true));
				htm(activeChar, null, "phantom.htm");
				break;
			}
			case admin_phantom_Kill:
			{
				int count = 0;
				for (L2PcInstance phantom : L2World.getInstance().getAllPlayers().values())
				{
					if (phantom != null && phantom.isPhantom() && !phantom.isDead())
					{
						count++;
						phantom.stopAllEffects();
						phantom.reduceCurrentHp(phantom.getMaxHp() + phantom.getMaxCp() + 1, activeChar);
					}
				}
				
				activeChar.sendMessage("Phantoms killed: "+count);
				activeChar.sendPacket(new ExShowScreenMessage("Phantoms killed: "+count, 3000, 2, true));
				htm(activeChar, null, "phantom.htm");
				break;
			}
			case admin_phantom_Res:
			{
				int count = 0;
				for (L2PcInstance phantom : L2World.getInstance().getAllPlayers().values())
				{
					if (phantom != null && phantom.isPhantom() && phantom.isDead())
					{
						count++;
						phantom.restoreExp(100.0);
						phantom.doRevive();
					}
				}
				
				activeChar.sendMessage("Phantoms resurrected: "+count);
				activeChar.sendPacket(new ExShowScreenMessage("Phantoms resurrected: "+count, 3000, 2, true));
				htm(activeChar, null, "phantom.htm");
				break;
			}
			case admin_phantom_Give_Flag:
			{
				int count = 0;
				for (L2PcInstance phantom : L2World.getInstance().getAllPlayers().values())
				{
					if (phantom != null && phantom.isPhantom() && !phantom.isDead())
					{
						count++;
						phantom.updatePvPFlag(1);
					}
				}
				
				activeChar.sendMessage("Phantoms have got pvp flag: "+count);
				activeChar.sendPacket(new ExShowScreenMessage("Phantoms have got pvp flag: "+count, 3000, 2, true));
				htm(activeChar, null, "phantom.htm");
				break;
			}
			case admin_phantom_Remove_Flag:
			{
				int count = 0;
				for (L2PcInstance phantom : L2World.getInstance().getAllPlayers().values())
				{
					if (phantom != null && phantom.isPhantom() && !phantom.isDead())
					{
						count++;
						phantom.updatePvPFlag(0);
					}
				}
				
				activeChar.sendMessage("Removed pvp flag from: "+count+" Phantoms");
				activeChar.sendPacket(new ExShowScreenMessage("Removed pvp flag from: "+count+" Phantoms", 3000, 2, true));
				htm(activeChar, null, "phantom.htm");
				break;
			}
			case admin_phantom_Reload:
			{
				int count = 0;
				
				for (L2PcInstance phantom : L2World.getInstance().getAllPlayers().values())
				{
					if (phantom != null && phantom.isPhantom())
					{
						count++;
						phantom.deleteMe();
					}
				}
				
				/*if (!phantomPlayers.targetListMages.isEmpty())
				{
					phantomPlayers.targetListMages.clear();
				}
				
				if (!phantomPlayers.targetListFighters.isEmpty())
				{
					phantomPlayers.targetListFighters.clear();
				}*/
				
				activeChar.sendMessage("Phantoms removed: "+count);
				activeChar.sendPacket(new ExShowScreenMessage("Phantoms removed: "+count, 3000, 2, true));
				phantomPlayers.LoadedPhantoms = 0;
				
				reload = true;
				phantomPlayers.getInstance().loadPhantomSystem(activeChar, reload, 0, 0);
				htm(activeChar, null, "phantom.htm");
				break;
			}
			case admin_phantom_spawn:
			{
				reload = false;
				int count = 50;
				int grade = 5;
				phantomPlayers.getInstance().loadPhantomSystem(activeChar, reload, count, grade);
				htm(activeChar, null, "phantom.htm");
				break;
			}
		}
		
		return false;
	}
	
	public static void htm(L2PcInstance activeChar, L2PcInstance player, String filename)
	{
		NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
		adminReply.setFile("data/html/admin/" + filename);
		
		adminReply.replace("%limit%", ""+phantomPlayers._PhantomsLimit);
		
		adminReply.replace("%total%", ""+phantomPlayers._phantoms.size());
		adminReply.replace("%loaded%", ""+phantomPlayers.LoadedPhantoms);
		
		int available = Math.abs(phantomPlayers._phantoms.size() - phantomPlayers.LoadedPhantoms);
		
		adminReply.replace("%available%", ""+available);
		
		adminReply.replace("%locations%", ""+phantomPlayers._PhantomsRandomLoc.size());
		adminReply.replace("%chats%", ""+phantomPlayers._PhantomsRandomPhrasesCount);
		adminReply.replace("%sets%", ""+phantomPlayers._setsCount);
		
		activeChar.sendPacket(adminReply);
	}

	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}