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
package l2jorion.game.model.actor.instance;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import l2jorion.Config;
import l2jorion.game.model.entity.Hero;
import l2jorion.game.model.multisell.L2Multisell;
import l2jorion.game.model.olympiad.CompetitionType;
import l2jorion.game.model.olympiad.Olympiad;
import l2jorion.game.model.olympiad.OlympiadGameManager;
import l2jorion.game.model.olympiad.OlympiadGameTask;
import l2jorion.game.model.olympiad.OlympiadManager;
import l2jorion.game.network.serverpackets.ActionFailed;
import l2jorion.game.network.serverpackets.ExHeroList;
import l2jorion.game.network.serverpackets.NpcHtmlMessage;
import l2jorion.game.templates.L2NpcTemplate;
import l2jorion.util.StringUtil;

public class L2OlympiadManagerInstance extends L2FolkInstance
{
	// private static Logger LOG = LoggerFactory.getLogger(L2OlympiadManagerInstance.class);
	
	private static final List<L2OlympiadManagerInstance> _managers = new CopyOnWriteArrayList<>();
	
	private static final int GATE_PASS = Config.ALT_OLY_COMP_RITEM;
	
	public static List<L2OlympiadManagerInstance> getInstances()
	{
		return _managers;
	}
	
	public L2OlympiadManagerInstance(final int objectId, final L2NpcTemplate template)
	{
		super(objectId, template);
	}
	
	public String getHtmlPath(int npcId, int val)
	{
		// Only used by Olympiad managers. Monument of Heroes don't use "Chat" bypass.
		String filename = "noble";
		
		if (val > 0)
		{
			filename = "noble_" + val;
		}
		
		return filename + ".htm";
	}
	
	@Override
	public void showChatWindow(L2PcInstance player, int val)
	{
		int npcId = getTemplate().getNpcId();
		String filename = getHtmlPath(npcId, val);
		
		switch (npcId)
		{
			case 31688: // Olympiad managers
				if (player.isNoble() && val == 0)
				{
					filename = "noble_main.htm";
				}
				break;
			
			case 31690: // Monuments of Heroes
			case 31769:
			case 31770:
			case 31771:
			case 31772:
				if (player.isHero() || Hero.getInstance().isInactiveHero(player.getObjectId()))
				{
					filename = "hero_main.htm";
				}
				else
				{
					filename = "hero_main2.htm";
				}
				break;
		}
		
		final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile("data/html/olympiad/" + filename);
		
		// Hidden option for players who are in inactive mode.
		if (filename == "hero_main.htm")
		{
			String hiddenText = "";
			if (Hero.getInstance().isInactiveHero(player.getObjectId()))
			{
				hiddenText = "<a action=\"bypass -h npc_%objectId%_Olympiad 5\">\"I want to be a Hero.\"</a><br>";
			}
			
			html.replace("%hero%", hiddenText);
		}
		html.replace("%objectId%", getObjectId());
		player.sendPacket(html);
		
		// Send a Server->Client ActionFailed to the Player in order to avoid that the client wait another packet
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	@Override
	public void onBypassFeedback(final L2PcInstance player, final String command)
	{
		if (command.startsWith("OlympiadNoble"))
		{
			final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
			if (player.isCursedWeaponEquipped())
			{
				html.setFile(Olympiad.OLYMPIAD_HTML_PATH + "noble_cant_cw.htm");
				player.sendPacket(html);
				return;
			}
			
			if (player.getClassIndex() != 0)
			{
				html.setFile(Olympiad.OLYMPIAD_HTML_PATH + "noble_cant_sub.htm");
				html.replace("%objectId%", getObjectId());
				player.sendPacket(html);
				return;
			}
			
			if (!player.isNoble() || (player.getClassId().level() < 3))
			{
				html.setFile(Olympiad.OLYMPIAD_HTML_PATH + "noble_cant_thirdclass.htm");
				html.replace("%objectId%", getObjectId());
				player.sendPacket(html);
				return;
			}
			
			int val = Integer.parseInt(command.substring(14));
			switch (val)
			{
				case 1: // Unregister
					OlympiadManager.getInstance().unRegisterNoble(player);
					break;
				
				case 2: // Show waiting list
					final int nonClassed = OlympiadManager.getInstance().getRegisteredNonClassBased().size();
					final int classed = OlympiadManager.getInstance().getRegisteredClassBased().size();
					
					html.setFile(Olympiad.OLYMPIAD_HTML_PATH + "noble_registered.htm");
					html.replace("%listClassed%", classed);
					html.replace("%listNonClassed%", nonClassed);
					html.replace("%objectId%", getObjectId());
					player.sendPacket(html);
					break;
				
				case 3: // There are %points% Grand Olympiad points granted for this event.
					int points = Olympiad.getInstance().getNoblePoints(player.getObjectId());
					html.setFile(Olympiad.OLYMPIAD_HTML_PATH + "noble_points1.htm");
					html.replace("%points%", points);
					html.replace("%objectId%", getObjectId());
					player.sendPacket(html);
					break;
				
				case 4: // register non classed based
					OlympiadManager.getInstance().registerNoble(player, CompetitionType.NON_CLASSED);
					break;
				
				case 5: // register classed based
					OlympiadManager.getInstance().registerNoble(player, CompetitionType.CLASSED);
					break;
				
				case 6: // request tokens reward
					html.setFile(Olympiad.OLYMPIAD_HTML_PATH + ((Olympiad.getInstance().getNoblessePasses(player, false) > 0) ? "noble_settle.htm" : "noble_nopoints2.htm"));
					html.replace("%objectId%", getObjectId());
					player.sendPacket(html);
					break;
				
				case 7: // Rewards
					L2Multisell.getInstance().SeparateAndSend(102, player, false, getCastle().getTaxRate());
					break;
				
				case 10: // Give tokens to player
					player.addItem("Olympiad", GATE_PASS, Olympiad.getInstance().getNoblessePasses(player, true), player, true);
					break;
				
				default:
					break;
			}
		}
		else if (command.startsWith("Olympiad"))
		{
			int val = Integer.parseInt(command.substring(9, 10));
			
			final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
			switch (val)
			{
				case 2: // Show rank for a specific class, example >> Olympiad 1_88
					int classId = Integer.parseInt(command.substring(11));
					if (classId >= 88 && classId <= 118)
					{
						List<String> names = Olympiad.getInstance().getClassLeaderBoard(classId);
						html.setFile(Olympiad.OLYMPIAD_HTML_PATH + "noble_ranking.htm");
						
						int index = 1;
						for (String name : names)
						{
							html.replace("%place" + index + "%", index);
							html.replace("%rank" + index + "%", name);
							
							index++;
							if (index > 10)
							{
								break;
							}
						}
						
						for (; index <= 10; index++)
						{
							html.replace("%place" + index + "%", "");
							html.replace("%rank" + index + "%", "");
						}
						
						html.replace("%objectId%", getObjectId());
						player.sendPacket(html);
					}
					break;
				case 3: // Spectator overview
					html.setFile(Olympiad.OLYMPIAD_HTML_PATH + "olympiad_observe_list.htm");
					
					int i = 0;
					
					final StringBuilder sb = new StringBuilder(2000);
					for (OlympiadGameTask task : OlympiadGameManager.getInstance().getOlympiadTasks())
					{
						StringUtil.append(sb, "<a action=\"bypass arenachange ", i, "\">Arena ", ++i, "&nbsp;");
						
						if (task.isGameStarted())
						{
							if (task.isInTimerTime())
							{
								StringUtil.append(sb, "(&$907;)"); // Counting In Progress
							}
							else if (task.isBattleStarted())
							{
								StringUtil.append(sb, "(&$829;)"); // In Progress
							}
							else
							{
								StringUtil.append(sb, "(&$908;)"); // Terminate
							}
							
							StringUtil.append(sb, "&nbsp;", task.getGame().getPlayerNames()[0], "&nbsp; : &nbsp;", task.getGame().getPlayerNames()[1]);
						}
						else
						{
							StringUtil.append(sb, "(&$906;)</td><td>&nbsp;"); // Initial State
						}
						
						StringUtil.append(sb, "</a><br>");
					}
					html.replaceNM("%list%", sb.toString());
					html.replace("%objectId%", getObjectId());
					player.sendPacket(html);
					break;
				case 4: // Send heroes list.
					player.sendPacket(new ExHeroList());
					break;
				case 5: // Hero pending state.
					if (Hero.getInstance().isInactiveHero(player.getObjectId()))
					{
						html.setFile(Olympiad.OLYMPIAD_HTML_PATH + "hero_confirm.htm");
						html.replace("%objectId%", getObjectId());
						player.sendPacket(html);
					}
					break;
				case 6: // Hero confirm action.
					if (Hero.getInstance().isInactiveHero(player.getObjectId()))
					{
						if (player.isSubClassActive() || player.getLevel() < 76)
						{
							player.sendMessage("You may only become an hero on a main class whose level is 75 or more.");
							return;
						}
						
						Hero.getInstance().activateHero(player);
					}
					break;
				
				case 7: // Main panel
					html.setFile(Olympiad.OLYMPIAD_HTML_PATH + "hero_main.htm");
					
					String hiddenText = "";
					if (Hero.getInstance().isInactiveHero(player.getObjectId()))
					{
						hiddenText = "<a action=\"bypass -h npc_%objectId%_Olympiad 5\">\"I want to be a Hero.\"</a><br>";
					}
					
					html.replace("%hero%", hiddenText);
					html.replace("%objectId%", getObjectId());
					player.sendPacket(html);
					break;
				case 8: // Show rank for a specific class, example >> Olympiad 1_88
					int classId1 = Integer.parseInt(command.substring(11));
					if (classId1 >= 88 && classId1 <= 118)
					{
						List<String> names = Olympiad.getInstance().getClassLeaderBoardCustom(classId1);
						html.setFile(Olympiad.OLYMPIAD_HTML_PATH + "noble_ranking.htm");
						
						int index = 1;
						for (String name : names)
						{
							html.replace("%place" + index + "%", index);
							html.replace("%rank" + index + "%", name);
							
							index++;
							if (index > 10)
							{
								break;
							}
						}
						
						for (; index <= 10; index++)
						{
							html.replace("%place" + index + "%", "");
							html.replace("%rank" + index + "%", "");
						}
						
						html.replace("%objectId%", getObjectId());
						player.sendPacket(html);
					}
					break;
				default:
					break;
			}
		}
		else
		{
			super.onBypassFeedback(player, command);
		}
	}
	
	@Override
	public void onSpawn()
	{
		super.onSpawn();
		
		if (getNpcId() == 31688)
		{
			_managers.add(this);
		}
	}
	
	@Override
	public void onDecay()
	{
		_managers.remove(this);
		super.onDecay();
	}
}