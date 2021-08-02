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
package l2jorion.game.network.clientpackets;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.NumberFormat;
import java.util.StringTokenizer;

import javolution.text.TextBuilder;
import l2jorion.Config;
import l2jorion.game.community.CommunityBoard;
import l2jorion.game.datatables.SkillTable;
import l2jorion.game.datatables.sql.AdminCommandAccessRights;
import l2jorion.game.datatables.sql.ItemTable;
import l2jorion.game.handler.AdminCommandHandler;
import l2jorion.game.handler.IAdminCommandHandler;
import l2jorion.game.handler.custom.CustomBypassHandler;
import l2jorion.game.handler.voice.DressMe;
import l2jorion.game.handler.voice.Vote;
import l2jorion.game.handler.vote.Brasil;
import l2jorion.game.handler.vote.Hopzone;
import l2jorion.game.handler.vote.L2TopGr;
import l2jorion.game.handler.vote.L2TopOnline;
import l2jorion.game.handler.vote.Network;
import l2jorion.game.handler.vote.Topzone;
import l2jorion.game.model.Inventory;
import l2jorion.game.model.L2DropCategory;
import l2jorion.game.model.L2DropData;
import l2jorion.game.model.L2Object;
import l2jorion.game.model.L2Skill;
import l2jorion.game.model.L2World;
import l2jorion.game.model.actor.instance.L2ClassMasterInstance;
import l2jorion.game.model.actor.instance.L2GrandBossInstance;
import l2jorion.game.model.actor.instance.L2ItemInstance;
import l2jorion.game.model.actor.instance.L2MinionInstance;
import l2jorion.game.model.actor.instance.L2NpcInstance;
import l2jorion.game.model.actor.instance.L2OlympiadManagerInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.actor.instance.L2RaidBossInstance;
import l2jorion.game.model.actor.instance.L2SymbolMakerInstance;
import l2jorion.game.model.custom.DressMeData;
import l2jorion.game.model.entity.event.CTF;
import l2jorion.game.model.entity.event.DM;
import l2jorion.game.model.entity.event.L2Event;
import l2jorion.game.model.entity.event.TvT;
import l2jorion.game.model.entity.event.VIP;
import l2jorion.game.model.olympiad.OlympiadManager;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.ActionFailed;
import l2jorion.game.network.serverpackets.ExShowScreenMessage;
import l2jorion.game.network.serverpackets.NpcHtmlMessage;
import l2jorion.game.network.serverpackets.OpenUrl;
import l2jorion.game.network.serverpackets.PlaySound;
import l2jorion.game.network.serverpackets.SystemMessage;
import l2jorion.game.templates.L2Item;
import l2jorion.game.util.GMAudit;
import l2jorion.game.util.Util;
import l2jorion.log.Log;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;
import l2jorion.util.CloseUtil;
import l2jorion.util.database.L2DatabaseFactory;

public final class RequestBypassToServer extends L2GameClientPacket
{
	private static Logger LOG = LoggerFactory.getLogger(RequestBypassToServer.class.getName());
	
	private String _command;
	
	@Override
	protected void readImpl()
	{
		_command = readS();
	}
	
	@Override
	protected void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();
		
		if (activeChar == null)
		{
			return;
		}
		
		if (!getClient().getFloodProtectors().getServerBypass().tryPerformAction(_command))
		{
			activeChar.sendMessage("You're doing that too fast.");
			return;
		}
		
		try
		{
			if (_command.startsWith("admin_"))
			{
				String command;
				if (_command.indexOf(" ") != -1)
				{
					command = _command.substring(0, _command.indexOf(" "));
				}
				else
				{
					command = _command;
				}
				
				IAdminCommandHandler ach = AdminCommandHandler.getInstance().getAdminCommandHandler(command);
				
				if (ach == null)
				{
					if (activeChar.isGM())
					{
						activeChar.sendMessage("The command " + command + " does not exist.");
					}
					String text = "No handler registered for admin command '" + command + "'";
					Log.add(text, "Wrong_admin_commands");
					return;
				}
				
				if (!AdminCommandAccessRights.getInstance().hasAccess(command, activeChar.getAccessLevel()))
				{
					activeChar.sendMessage("You don't have the access right to use this command.");
					if (Config.DEBUG)
					{
						LOG.warn("Character " + activeChar.getName() + " tried to use admin command " + command + ", but doesn't have access to it!");
					}
					return;
				}
				
				if (Config.GMAUDIT)
				{
					GMAudit.auditGMAction(activeChar.getName() + " [" + activeChar.getObjectId() + "]", command, (activeChar.getTarget() != null ? activeChar.getTarget().getName() : "no-target"), _command.replace(command, ""));
				}
				
				ach.useAdminCommand(_command, activeChar);
			}
			else if (_command.startsWith("answer "))
			{
				String answer = _command.substring(7);
				switch (answer)
				{
					case "100001":
						activeChar.checkAnswer(100001);
						break;
					case "100002":
						activeChar.checkAnswer(100002);
						break;
					case "100003":
						activeChar.checkAnswer(100003);
						break;
					case "100004":
						activeChar.checkAnswer(100004);
						break;
					case "100005":
						activeChar.checkAnswer(100005);
						break;
				}
			}
			else if (_command.startsWith("buffspage"))
			{
				String[] val = _command.split(" ");
				String x = val[1];
				int page = Integer.parseInt(x);
				
				L2PcInstance target = null;
				if (activeChar.getTarget() instanceof L2PcInstance)
				{
					target = (L2PcInstance) activeChar.getTarget();
				}
				
				if (target == null)
				{
					return;
				}
				
				activeChar.sellBuffsMenu(target, page);
			}
			else if (_command.startsWith("buff"))
			{
				if (!Config.SELLBUFF_SYSTEM)
				{
					return;
				}
				
				String[] val = _command.split(" ");
				String x = val[1];
				String y = val[2];
				String p = val[3];
				
				int id = Integer.parseInt(x);
				int lvl = Integer.parseInt(y);
				int page = Integer.parseInt(p);
				
				L2PcInstance target = null;
				
				if (activeChar.getTarget() instanceof L2PcInstance)
				{
					target = (L2PcInstance) activeChar.getTarget();
				}
				
				if (target == null)
				{
					return;
				}
				
				if (activeChar.getInventory().getItemByItemId(57) == null || activeChar.getInventory().getItemByItemId(57).getCount() < ((L2PcInstance) activeChar.getTarget()).getBuffPrize())
				{
					activeChar.sendMessage("You don't have enough adena.");
					return;
				}
				
				try
				{
					boolean isOK = false;
					String buff = null;
					String buff2 = null;
					
					L2Skill bufferSkill = SkillTable.getInstance().getInfo(id, lvl);
					
					for (L2Skill targetSkill : target.getAllSkills())
					{
						buff = "" + targetSkill.getId();
						buff2 = "" + bufferSkill.getId();
						
						if (buff2.contains(buff))
						{
							isOK = true;
						}
					}
					
					if (isOK)
					{
						bufferSkill.getEffects(activeChar, activeChar);
						SystemMessage sm = new SystemMessage(SystemMessageId.YOU_FEEL_S1_EFFECT);
						sm.addSkillName(id);
						activeChar.sendPacket(sm);
						activeChar.reduceAdena("Buff", target.getBuffPrize(), activeChar, true);
						target.getInventory().addItem("", 57, target.getBuffPrize(), target, null);
					}
					
					activeChar.sellBuffsMenu(target, page);
					
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}
			else if (_command.startsWith("actr"))
			{
				String l = _command.substring(5);
				
				int p = 0;
				
				p = Integer.parseInt(l);
				
				if (p == 0)
				{
					return;
				}
				
				if (p > 2000000000)
				{
					activeChar.sendMessage("You have too much adena for buffs selling.");
					return;
				}
				
				activeChar.setBuffPrize(p);
				activeChar.sitDown();
				activeChar.setTeam(1);
				activeChar.setSellBuff(true);
				activeChar.setOldTitle(activeChar.getTitle());
				activeChar.setTitle(Config.SELLBUFF_TITLE);
				activeChar.broadcastUserInfo();
				activeChar.broadcastTitleInfo();
			}
			else if (_command.startsWith("player_help "))
			{
				playerHelp(activeChar, _command.substring(12));
			}
			else if (_command.startsWith("open_url "))
			{
				activeChar.sendPacket(new OpenUrl(_command.substring(9)));
			}
			else if (_command.startsWith("vote "))
			{
				Vote.restoreVotedData(activeChar, activeChar.getClient().getConnection().getInetAddress().getHostAddress());
				
				String voteSiteName = _command.substring(5);
				switch (voteSiteName)
				{
					case "hopzone":
						if (activeChar.eligibleToVoteHop())
						{
							Hopzone voteHop = new Hopzone();
							if (voteHop.hasVoted(activeChar))
							{
								voteHop.updateDB(activeChar, "last_hop_vote");
								voteHop.setVoted(activeChar);
								voteHop.reward(activeChar);
							}
							else
							{
								activeChar.sendMessage("You didn't vote yet.");
								Vote.showHtm(activeChar);
							}
						}
						else
						{
							Vote.showHtm(activeChar);
						}
						break;
					case "topzone":
						if (activeChar.eligibleToVoteTop())
						{
							Topzone voteTop = new Topzone();
							if (voteTop.hasVoted(activeChar))
							{
								voteTop.updateDB(activeChar, "last_top_vote");
								voteTop.setVoted(activeChar);
								voteTop.reward(activeChar);
							}
							else
							{
								activeChar.sendMessage("You didn't vote yet.");
								Vote.showHtm(activeChar);
							}
						}
						else
						{
							Vote.showHtm(activeChar);
						}
						break;
					case "network":
						if (activeChar.eligibleToVoteNet())
						{
							Network voteNet = new Network();
							if (voteNet.hasVoted(activeChar))
							{
								voteNet.updateDB(activeChar, "last_net_vote");
								voteNet.setVoted(activeChar);
								voteNet.reward(activeChar);
							}
							else
							{
								activeChar.sendMessage("You didn't vote yet.");
								Vote.showHtm(activeChar);
							}
						}
						else
						{
							Vote.showHtm(activeChar);
						}
						break;
					case "brasil":
						if (activeChar.eligibleToVoteBra())
						{
							Brasil voteBra = new Brasil();
							if (voteBra.hasVoted(activeChar))
							{
								voteBra.updateDB(activeChar, "last_bra_vote");
								voteBra.setVoted(activeChar);
								voteBra.reward(activeChar);
							}
							else
							{
								activeChar.sendMessage("You didn't vote yet.");
								Vote.showHtm(activeChar);
							}
						}
						else
						{
							Vote.showHtm(activeChar);
						}
						break;
					case "L2TopGr":
						if (activeChar.eligibleToVoteL2TopGr())
						{
							L2TopGr vote = new L2TopGr();
							if (vote.hasVoted(activeChar))
							{
								vote.updateDB(activeChar, "last_l2topgr");
								vote.setVoted(activeChar);
								vote.reward(activeChar);
							}
							else
							{
								activeChar.sendMessage("You didn't vote yet.");
								Vote.showHtm(activeChar);
							}
						}
						else
						{
							Vote.showHtm(activeChar);
						}
						break;
					case "L2TopOnline":
						if (activeChar.eligibleToVoteL2TopOnline())
						{
							L2TopOnline vote = new L2TopOnline();
							if (vote.hasVoted(activeChar))
							{
								vote.updateDB(activeChar, "last_l2toponline");
								vote.setVoted(activeChar);
								vote.reward(activeChar);
							}
							else
							{
								activeChar.sendMessage("You didn't vote yet.");
								Vote.showHtm(activeChar);
							}
						}
						else
						{
							Vote.showHtm(activeChar);
						}
						break;
				}
			}
			else if (_command.startsWith("npc_"))
			{
				if (!activeChar.validateBypass(_command))
				{
					return;
				}
				
				int endOfId = _command.indexOf('_', 5);
				String id;
				
				if (endOfId > 0)
				{
					id = _command.substring(4, endOfId);
				}
				else
				{
					id = _command.substring(4);
				}
				
				try
				{
					L2Object object = L2World.getInstance().findObject(Integer.parseInt(id));
					
					if (_command.substring(endOfId + 1).startsWith("event_participate"))
					{
						L2Event.inscribePlayer(activeChar);
					}
					else if (_command.substring(endOfId + 1).startsWith("tvt_player_join "))
					{
						String teamName = _command.substring(endOfId + 1).substring(16);
						
						if (TvT.is_joining())
						{
							TvT.addPlayer(activeChar, teamName);
						}
						else
						{
							activeChar.sendMessage("The event is already started. You can not join now!");
						}
					}
					
					else if (_command.substring(endOfId + 1).startsWith("tvt_player_leave"))
					{
						if (TvT.is_joining())
						{
							TvT.removePlayer(activeChar);
						}
						else
						{
							activeChar.sendMessage("The event is already started. You can not leave now!");
						}
					}
					
					else if (_command.substring(endOfId + 1).startsWith("dmevent_player_join"))
					{
						if (DM.is_joining())
						{
							DM.addPlayer(activeChar);
						}
						else
						{
							activeChar.sendMessage("The event is already started. You can't join now!");
						}
					}
					
					else if (_command.substring(endOfId + 1).startsWith("dmevent_player_leave"))
					{
						if (DM.is_joining())
						{
							DM.removePlayer(activeChar);
						}
						else
						{
							activeChar.sendMessage("The event is already started. You can't leave now!");
						}
					}
					
					else if (_command.substring(endOfId + 1).startsWith("ctf_player_join "))
					{
						String teamName = _command.substring(endOfId + 1).substring(16);
						if (CTF.is_joining())
						{
							CTF.addPlayer(activeChar, teamName);
						}
						else
						{
							activeChar.sendMessage("The event is already started. You can't join now!");
						}
					}
					
					else if (_command.substring(endOfId + 1).startsWith("ctf_player_leave"))
					{
						if (CTF.is_joining())
						{
							CTF.removePlayer(activeChar);
						}
						else
						{
							activeChar.sendMessage("The event is already started. You can't leave now!");
						}
					}
					
					if (_command.substring(endOfId + 1).startsWith("vip_joinVIPTeam"))
					{
						VIP.addPlayerVIP(activeChar);
					}
					
					if (_command.substring(endOfId + 1).startsWith("vip_joinNotVIPTeam"))
					{
						VIP.addPlayerNotVIP(activeChar);
					}
					
					if (_command.substring(endOfId + 1).startsWith("vip_finishVIP"))
					{
						VIP.vipWin(activeChar);
					}
					
					if (_command.substring(endOfId + 1).startsWith("event_participate"))
					{
						L2Event.inscribePlayer(activeChar);
					}
					
					else if ((Config.ALLOW_CLASS_MASTERS && Config.ALLOW_REMOTE_CLASS_MASTERS && object instanceof L2ClassMasterInstance) || (object instanceof L2NpcInstance && endOfId > 0 && Util.checkIfInRange(L2NpcInstance.INTERACTION_DISTANCE, activeChar, object, true)))
					{
						((L2NpcInstance) object).onBypassFeedback(activeChar, _command.substring(endOfId + 1));
					}
					
					activeChar.sendPacket(ActionFailed.STATIC_PACKET);
				}
				catch (NumberFormatException nfe)
				{
					if (Config.ENABLE_ALL_EXCEPTIONS)
					{
						nfe.printStackTrace();
					}
					
				}
			}
			// Draw a Symbol
			else if (_command.equals("Draw"))
			{
				L2Object object = activeChar.getTarget();
				if (object instanceof L2NpcInstance)
				{
					((L2SymbolMakerInstance) object).onBypassFeedback(activeChar, _command);
				}
			}
			else if (_command.equals("RemoveList"))
			{
				L2Object object = activeChar.getTarget();
				if (object instanceof L2NpcInstance)
				{
					((L2SymbolMakerInstance) object).onBypassFeedback(activeChar, _command);
				}
			}
			else if (_command.equals("Remove "))
			{
				L2Object object = activeChar.getTarget();
				
				if (object instanceof L2NpcInstance)
				{
					((L2SymbolMakerInstance) object).onBypassFeedback(activeChar, _command);
				}
			}
			else if (_command.startsWith("manor_menu_select?"))
			{
				L2Object object = activeChar.getTarget();
				if (object instanceof L2NpcInstance)
				{
					((L2NpcInstance) object).onBypassFeedback(activeChar, _command);
				}
			}
			else if (_command.startsWith("bbs_") || _command.startsWith("_bbs") || _command.startsWith("_friend") || _command.startsWith("_mail") || _command.startsWith("_block"))
			{
				CommunityBoard.getInstance().handleCommands(getClient(), _command);
			}
			else if (_command.startsWith("Quest "))
			{
				if (!activeChar.validateBypass(_command))
				{
					return;
				}
				
				L2PcInstance player = getClient().getActiveChar();
				if (player == null)
				{
					return;
				}
				
				String p = _command.substring(6).trim();
				int idx = p.indexOf(' ');
				
				if (idx < 0)
				{
					player.processQuestEvent(p, "");
				}
				else
				{
					player.processQuestEvent(p.substring(0, idx), p.substring(idx).trim());
				}
			}
			else if (_command.startsWith("submitpin"))
			{
				try
				{
					String value = _command.substring(8);
					StringTokenizer s = new StringTokenizer(value, " ");
					int pin = activeChar.getPin();
					
					Connection con = null;
					try
					{
						pin = Integer.parseInt(s.nextToken());
						if (Integer.toString(pin).length() != 4)
						{
							activeChar.sendMessage("You have to fill the pin box with 4 numbers. Not more, not less.");
							activeChar.showCreatePinHtml();
							return;
						}
						con = L2DatabaseFactory.getInstance().getConnection();
						PreparedStatement statement = con.prepareStatement("UPDATE characters SET pin=? WHERE obj_Id=?");
						statement.setInt(1, pin);
						statement.setInt(2, activeChar.getObjectId());
						statement.execute();
						statement.close();
						
						activeChar.setPincheck(false);
						activeChar.updatePincheck();
						activeChar.sendMessage("You successfully submitted your pin code. You will need it in order to login.");
						activeChar.sendMessage("Your Pin Code is: " + pin);
						activeChar.setIsImobilised(false);
						activeChar.setIsSubmitingPin(false);
						statement = null;
					}
					catch (Exception e)
					{
						e.printStackTrace();
						activeChar.sendMessage("The Pin Code must be 4 numbers.");
						activeChar.showCreatePinHtml();
					}
				}
				catch (Exception e)
				{
					e.printStackTrace();
					activeChar.sendMessage("The Pin Code must be 4 numbers.");
					activeChar.showCreatePinHtml();
				}
				
			}
			else if (_command.startsWith("enterpin"))
			{
				try
				{
					String value = _command.substring(8);
					StringTokenizer s = new StringTokenizer(value, " ");
					int dapin = 0;
					int pin = 0;
					
					dapin = Integer.parseInt(s.nextToken());
					Connection con = null;
					try
					{
						con = L2DatabaseFactory.getInstance().getConnection();
						PreparedStatement statement = con.prepareStatement("SELECT pin FROM characters WHERE obj_Id=?");
						statement.setInt(1, activeChar.getObjectId());
						
						ResultSet rset = statement.executeQuery();
						
						while (rset.next())
						{
							pin = rset.getInt("pin");
						}
						statement.execute();
						statement.close();
						statement = null;
						if (pin == dapin)
						{
							activeChar.sendMessage("Pin Code Authenticated Successfully. You are now free to move.");
							activeChar.setIsImobilised(false);
							activeChar.setIsSubmitingPin(false);
						}
						else
						{
							activeChar.sendMessage("Pin Code does not match with the submitted one. You will now get disconnected!");
							Util.handleIllegalPlayerAction(activeChar, activeChar.getName() + " wrong Pin Code.", Config.DEFAULT_PUNISH);
						}
					}
					catch (Exception e)
					{
						e.printStackTrace();
						activeChar.sendMessage("The Pin Code must be 4 numbers.");
						activeChar.showEnterPinHtml();
					}
					finally
					{
						CloseUtil.close(con);
					}
				}
				catch (Exception e)
				{
					e.printStackTrace();
					activeChar.sendMessage("The Pin Code MUST be 4 numbers.");
					activeChar.showEnterPinHtml();
				}
			}
			else if (_command.startsWith("custom_"))
			{
				L2PcInstance player = getClient().getActiveChar();
				CustomBypassHandler.getInstance().handleBypass(player, _command);
			}
			else if (_command.startsWith("arenachange")) // change
			{
				final boolean isManager = activeChar.getTarget() != null && activeChar.getTarget() instanceof L2OlympiadManagerInstance;
				if (!isManager)
				{
					// Without npc, command can be used only in observer mode on arena
					if (!activeChar.inObserverMode() || activeChar.isInOlympiadMode() || activeChar.getOlympiadGameId() < 0)
					{
						return;
					}
				}
				
				if (OlympiadManager.getInstance().isRegisteredInComp(activeChar))
				{
					activeChar.sendPacket(SystemMessageId.WHILE_YOU_ARE_ON_THE_WAITING_LIST_YOU_ARE_NOT_ALLOWED_TO_WATCH_THE_GAME);
					return;
				}
				
				final int arenaId = Integer.parseInt(_command.substring(12).trim());
				activeChar.enterOlympiadObserverMode(arenaId);
			}
			else if (_command.startsWith("dropInfo"))
			{
				final String[] val = _command.split(" ");
				
				int index = Integer.parseInt(val[1]);
				if (index < 1)
				{
					index = 1;
				}
				
				String type = val[2];
				int maxPages = 9;
				int count = 0;
				int i = 0;
				int color = 1;
				
				L2NpcInstance target = (L2NpcInstance) activeChar.getTarget();
				
				if (target != null)
				{
					
					NpcHtmlMessage html = new NpcHtmlMessage(1);
					html.setFile("data/html/dropInfo.htm");
					TextBuilder html1 = new TextBuilder();
					
					html.replace("%title%", type);
					
					for (L2DropCategory cat : target.getTemplate().getDropData())
					{
						if (type.equals("Drop"))
						{
							if (cat.isSweep())
							{
								continue;
							}
						}
						
						if (type.equals("Spoil"))
						{
							if (!cat.isSweep())
							{
								continue;
							}
						}
						
						for (L2DropData drop : cat.getAllDrops())
						{
							
							if (drop == null || ItemTable.getInstance().getTemplate(drop.getItemId()) == null)
							{
								continue;
							}
							
							count++;
							
							if (i >= (index * maxPages))
							{
								continue;
							}
							
							String name = ItemTable.getInstance().getTemplate(drop.getItemId()).getName();
							
							if (name.length() >= 45)
							{
								name = name.substring(0, 45) + "...";
							}
							
							float chance = Float.parseFloat("" + drop.getChance()) / 1000000 * 100;
							
							if (target instanceof L2RaidBossInstance)
							{
								chance *= Config.ITEMS_RAID;
							}
							else if (target instanceof L2GrandBossInstance)
							{
								chance *= Config.ITEMS_BOSS;
							}
							else if (target instanceof L2MinionInstance)
							{
								chance *= Config.ITEMS_MINON;
							}
							else
							{
								chance *= Config.RATE_DROP_ITEMS;
								
								if (activeChar.getPremiumService() == 1)
								{
									chance *= Config.PREMIUM_DROP_RATE;
								}
							}
							
							if (chance >= 100)
							{
								chance = 100;
							}
							
							L2Item item = ItemTable.getInstance().getTemplate(drop.getItemId());
							String itemIcon = item.getItemIcon(drop.getItemId());
							String formatedIntAndChance = formatIntAndColor(chance);
							String dropCount = (drop.getMaxDrop() > 1 ? "<font color=LEVEL>" + Util.formatItem(drop.getMinDrop()) + "</font>-<font color=LEVEL>" + Util.formatItem(drop.getMaxDrop()) + "</font>" : "<font color=LEVEL>" + Util.formatItem(drop.getMinDrop()) + "</font>");
							
							if (i++ >= ((index - 1) * maxPages))
							{
								if (color == 1)
								{
									html1.append("<table border=0 cellspacing=\"2\" cellpadding=\"0\" width=\"300\" bgcolor=\"000000\">");
									color = 2;
								}
								else
								{
									html1.append("<table border=0 cellspacing=\"2\" cellpadding=\"0\" width=\"300\">");
									color = 1;
								}
								html1.append("<tr>");
								html1.append("<td><img src=\"" + itemIcon + "\" width=32 height=32></td>");
								html1.append("<td width=260><font color=\"ae6c51\">" + name + "</font><br1> Chance: " + formatedIntAndChance + " Count: " + dropCount + "</td>");
								html1.append("</tr>");
								html1.append("</table>");
							}
						}
					}
					
					html1.append("<br><table cellspacing=\"2\" cellpadding=\"0\"><tr>");
					if (index == 1)
					{
						html1.append("<td><button action=\"\" back=\"l2ui_ch3.prev1_down\" fore=\"l2ui_ch3.prev1\" width=16 height=16></td>");
					}
					else
					{
						html1.append("<td><button action=\"bypass dropInfo " + (index - 1) + " " + type + "\" back=\"l2ui_ch3.prev1_down\" fore=\"l2ui_ch3.prev1\" width=16 height=16></td>");
					}
					
					int pageNumber = count / maxPages;
					if (pageNumber * maxPages != count)
					{
						pageNumber++;
					}
					
					for (i = 1; i <= pageNumber; i++)
					{
						if (i == index)
						{
							html1.append("<td width=\"13\"><center>" + i + "</center></td>");
						}
						else
						{
							html1.append("<td width=\"13\"><center><a action=\"bypass dropInfo " + i + " " + type + "\">" + i + "</a></center></td>");
						}
					}
					
					if (index == pageNumber)
					{
						html1.append("<td><button action=\"\" back=\"l2ui_ch3.next1_down\" fore=\"l2ui_ch3.next1\" width=16 height=16></td>");
					}
					else
					{
						html1.append("<td><button action=\"bypass dropInfo " + (index + 1) + " " + type + "\" back=\"l2ui_ch3.next1_down\" fore=\"l2ui_ch3.next1\" width=16 height=16></td>");
					}
					html1.append("</tr></table>");
					
					html.replace("%drop%", html1.toString());
					activeChar.sendPacket(html);
				}
			}
			else if (_command.equals("bp_changedressmestatus"))
			{
				if (activeChar.isDressMeEnabled())
				{
					activeChar.setDressMeEnabled(false);
					activeChar.broadcastUserInfo();
				}
				else
				{
					activeChar.setDressMeEnabled(true);
					activeChar.broadcastUserInfo();
				}
				
				DressMe.sendMainWindow(activeChar);
			}
			else if (_command.startsWith("bp_editWindow"))
			{
				String bp = _command.substring(14);
				StringTokenizer st = new StringTokenizer(bp);
				
				sendEditWindow(activeChar, st.nextToken());
			}
			else if (_command.startsWith("bp_setpart"))
			{
				String bp = _command.substring(11);
				StringTokenizer st = new StringTokenizer(bp);
				
				String part = st.nextToken();
				String type = st.nextToken();
				
				setPart(activeChar, part, type);
			}
			else if (_command.startsWith("bp_gettarget"))
			{
				String bp = _command.substring(13);
				StringTokenizer st = new StringTokenizer(bp);
				
				String part = st.nextToken();
				
				stealTarget(activeChar, part);
			}
			else if (_command.equals("bp_main"))
			{
				DressMe.sendMainWindow(activeChar);
			}
		}
		catch (Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			
			LOG.warn("Bad RequestBypassToServer: ", e);
		}
	}
	
	private void playerHelp(L2PcInstance activeChar, String path)
	{
		if (path.indexOf("..") != -1)
		{
			return;
		}
		
		String filename = "data/html/help/" + path;
		NpcHtmlMessage html = new NpcHtmlMessage(1);
		html.setFile(filename);
		activeChar.sendPacket(html);
	}
	
	private String formatIntAndColor(float chance)
	{
		NumberFormat defaultFormat = NumberFormat.getNumberInstance();
		defaultFormat.setMinimumFractionDigits(2);
		if (chance < 1)
		{
			defaultFormat.setMinimumFractionDigits(4);
		}
		
		String formatedChance = defaultFormat.format(chance);
		
		if (chance >= 90)
		{
			formatedChance = "<font color=\"00ff00\">" + formatedChance + "%</font>";
		}
		else if (chance < 90 && chance >= 50)
		{
			formatedChance = "<font color=\"009900\">" + formatedChance + "%</font>";
		}
		else if (chance < 50 && chance >= 20)
		{
			formatedChance = "<font color=\"LEVEL\">" + formatedChance + "%</font>";
		}
		else if (chance < 20 && chance >= 10)
		{
			formatedChance = "<font color=\"d99d07\">" + formatedChance + "%</font>";
		}
		else if (chance < 10 && chance >= 5)
		{
			formatedChance = "<font color=\"ed3737\">" + formatedChance + "%</font>";
		}
		else
		{
			formatedChance = "<font color=\"ff0000\">" + formatedChance + "%</font>";
		}
		
		return formatedChance;
	}
	
	public void stealTarget(L2PcInstance p, String part)
	{
		if (p.getTarget() == null || !(p.getTarget() instanceof L2PcInstance))
		{
			p.sendMessage("Invalid target.");
			return;
		}
		
		L2PcInstance t = (L2PcInstance) p.getTarget();
		
		if (p.getDressMeData() == null)
		{
			DressMeData dmd = new DressMeData();
			p.setDressMeData(dmd);
		}
		
		boolean returnMain = false;
		
		switch (part)
		{
			case "chest":
			{
				if (t.getInventory().getPaperdollItem(Inventory.PAPERDOLL_CHEST) == null)
				{
					p.getDressMeData().setChestId(0);
				}
				else
				{
					p.getDressMeData().setChestId(t.getInventory().getPaperdollItem(Inventory.PAPERDOLL_CHEST).getItemId());
				}
				break;
			}
			case "legs":
			{
				if (t.getInventory().getPaperdollItem(Inventory.PAPERDOLL_LEGS) == null)
				{
					p.getDressMeData().setLegsId(0);
				}
				else
				{
					p.getDressMeData().setLegsId(t.getInventory().getPaperdollItem(Inventory.PAPERDOLL_LEGS).getItemId());
				}
				break;
			}
			case "gloves":
			{
				if (t.getInventory().getPaperdollItem(Inventory.PAPERDOLL_GLOVES) == null)
				{
					p.getDressMeData().setGlovesId(0);
				}
				else
				{
					p.getDressMeData().setGlovesId(t.getInventory().getPaperdollItem(Inventory.PAPERDOLL_GLOVES).getItemId());
				}
				break;
			}
			case "boots":
			{
				if (t.getInventory().getPaperdollItem(Inventory.PAPERDOLL_FEET) == null)
				{
					p.getDressMeData().setBootsId(0);
				}
				else
				{
					p.getDressMeData().setBootsId(t.getInventory().getPaperdollItem(Inventory.PAPERDOLL_FEET).getItemId());
				}
				break;
			}
			case "weap":
			{
				if (t.getInventory().getPaperdollItem(Inventory.PAPERDOLL_RHAND) == null)
				{
					p.getDressMeData().setWeapId(0);
				}
				else
				{
					p.getDressMeData().setWeapId(t.getInventory().getPaperdollItem(Inventory.PAPERDOLL_RHAND).getItemId());
				}
				break;
			}
			case "all":
			{
				if (t.getInventory().getPaperdollItem(Inventory.PAPERDOLL_CHEST) == null)
				{
					p.getDressMeData().setChestId(0);
				}
				else
				{
					p.getDressMeData().setChestId(t.getInventory().getPaperdollItem(Inventory.PAPERDOLL_CHEST).getItemId());
				}
				if (t.getInventory().getPaperdollItem(Inventory.PAPERDOLL_LEGS) == null)
				{
					p.getDressMeData().setLegsId(0);
				}
				else
				{
					p.getDressMeData().setLegsId(t.getInventory().getPaperdollItem(Inventory.PAPERDOLL_LEGS).getItemId());
				}
				if (t.getInventory().getPaperdollItem(Inventory.PAPERDOLL_GLOVES) == null)
				{
					p.getDressMeData().setGlovesId(0);
				}
				else
				{
					p.getDressMeData().setGlovesId(t.getInventory().getPaperdollItem(Inventory.PAPERDOLL_GLOVES).getItemId());
				}
				if (t.getInventory().getPaperdollItem(Inventory.PAPERDOLL_FEET) == null)
				{
					p.getDressMeData().setBootsId(0);
				}
				else
				{
					p.getDressMeData().setBootsId(t.getInventory().getPaperdollItem(Inventory.PAPERDOLL_FEET).getItemId());
				}
				if (t.getInventory().getPaperdollItem(Inventory.PAPERDOLL_RHAND) == null)
				{
					p.getDressMeData().setWeapId(0);
				}
				else
				{
					p.getDressMeData().setWeapId(t.getInventory().getPaperdollItem(Inventory.PAPERDOLL_RHAND).getItemId());
				}
				returnMain = true;
				break;
			}
		}
		
		p.broadcastUserInfo();
		if (!returnMain)
		{
			sendEditWindow(p, part);
		}
		else
		{
			DressMe.sendMainWindow(p);
		}
	}
	
	public String getItemNameById(int itemId)
	{
		L2Item item = ItemTable.getInstance().getTemplate(itemId);
		
		String itemName = "NoName";
		
		if (itemId != 0)
		{
			itemName = item.getName();
		}
		
		return itemName;
	}
	
	public void setPart(L2PcInstance p, String part, String type)
	{
		if (p.getDressMeData() == null)
		{
			DressMeData dmd = new DressMeData();
			p.setDressMeData(dmd);
		}
		
		if (Config.ALLOW_DRESS_ME_FOR_ITEM)
		{
			final int currency = Config.DRESS_ME_ITEM_ID;
			final L2ItemInstance item = p.getInventory().getItemByItemId(currency);
			
			if (item == null || item.getCount() < Config.DRESS_ME_ITEM_COUNT)
			{
				p.sendMessage("You don't have enough " + getItemNameById(currency) + ".");
				p.sendPacket(new ExShowScreenMessage("You don't have enough " + getItemNameById(currency) + ".", 3000, 2, false));
				p.sendPacket(new PlaySound("ItemSound3.sys_impossible"));
				return;
			}
			
			p.destroyItem("Consume", item.getObjectId(), Config.DRESS_ME_ITEM_COUNT, null, true);
		}
		
		if (Config.ALLOW_DRESS_ME_FOR_PREMIUM)
		{
			if (p.getPremiumService() == 0)
			{
				p.sendMessage("You're not The Premium Account.");
				p.sendPacket(new ExShowScreenMessage("You're not The Premium account.", 3000, 2, false));
				p.sendPacket(new PlaySound("ItemSound3.sys_impossible"));
				return;
			}
		}
		
		switch (part)
		{
			case "chest":
			{
				if (Config.DRESS_ME_CHESTS.keySet().contains(type))
				{
					p.getDressMeData().setChestId(Config.DRESS_ME_CHESTS.get(type));
				}
				
				break;
			}
			case "legs":
			{
				if (Config.DRESS_ME_LEGS.keySet().contains(type))
				{
					p.getDressMeData().setLegsId(Config.DRESS_ME_LEGS.get(type));
				}
				
				break;
			}
			case "gloves":
			{
				if (Config.DRESS_ME_GLOVES.keySet().contains(type))
				{
					p.getDressMeData().setGlovesId(Config.DRESS_ME_GLOVES.get(type));
				}
				
				break;
			}
			case "boots":
			{
				if (Config.DRESS_ME_BOOTS.keySet().contains(type))
				{
					p.getDressMeData().setBootsId(Config.DRESS_ME_BOOTS.get(type));
				}
				
				break;
			}
			case "weap":
			{
				if (Config.DRESS_ME_WEAPONS.keySet().contains(type))
				{
					p.getDressMeData().setWeapId(Config.DRESS_ME_WEAPONS.get(type));
				}
				
				break;
			}
		}
		
		p.broadcastUserInfo();
		sendEditWindow(p, part);
	}
	
	public void sendEditWindow(L2PcInstance p, String part)
	{
		NpcHtmlMessage htm = new NpcHtmlMessage(0);
		htm.setFile("./data/html/custom/dressme/edit.htm");
		htm.replace("%part%", part);
		
		switch (part)
		{
			case "chest":
			{
				if (p.getDressMeData() == null)
				{
					htm.replace("%partinfo%", "You have no custom chest.");
				}
				else
				{
					htm.replace("%partinfo%", p.getDressMeData().getChestId() == 0 ? "You have no custom chest." : ItemTable.getInstance().getTemplate(p.getDressMeData().getChestId()).getName());
				}
				String temp = "";
				for (String s : Config.DRESS_ME_CHESTS.keySet())
				{
					temp += s + ";";
				}
				htm.replace("%dropboxdata%", temp);
				break;
			}
			case "legs":
			{
				if (p.getDressMeData() == null)
				{
					htm.replace("%partinfo%", "You have no custom legs.");
				}
				else
				{
					htm.replace("%partinfo%", p.getDressMeData().getLegsId() == 0 ? "You have no custom legs." : ItemTable.getInstance().getTemplate(p.getDressMeData().getLegsId()).getName());
				}
				String temp = "";
				for (String s : Config.DRESS_ME_LEGS.keySet())
				{
					temp += s + ";";
				}
				htm.replace("%dropboxdata%", temp);
				break;
			}
			case "gloves":
			{
				if (p.getDressMeData() == null)
				{
					htm.replace("%partinfo%", "You have no custom gloves.");
				}
				else
				{
					htm.replace("%partinfo%", p.getDressMeData().getGlovesId() == 0 ? "You have no custom gloves." : ItemTable.getInstance().getTemplate(p.getDressMeData().getGlovesId()).getName());
				}
				String temp = "";
				for (String s : Config.DRESS_ME_GLOVES.keySet())
				{
					temp += s + ";";
				}
				htm.replace("%dropboxdata%", temp);
				break;
			}
			case "boots":
			{
				if (p.getDressMeData() == null)
				{
					htm.replace("%partinfo%", "You have no custom boots.");
				}
				else
				{
					htm.replace("%partinfo%", p.getDressMeData().getBootsId() == 0 ? "You have no custom boots." : ItemTable.getInstance().getTemplate(p.getDressMeData().getBootsId()).getName());
				}
				String temp = "";
				for (String s : Config.DRESS_ME_BOOTS.keySet())
				{
					temp += s + ";";
				}
				htm.replace("%dropboxdata%", temp);
				break;
			}
			case "weap":
			{
				if (p.getDressMeData() == null)
				{
					htm.replace("%partinfo%", "You have no custom weapon.");
				}
				else
				{
					htm.replace("%partinfo%", p.getDressMeData().getWeapId() == 0 ? "You have no custom weapon." : ItemTable.getInstance().getTemplate(p.getDressMeData().getWeapId()).getName());
				}
				String temp = "";
				for (String s : Config.DRESS_ME_WEAPONS.keySet())
				{
					temp += s + ";";
				}
				htm.replace("%dropboxdata%", temp);
				break;
			}
		}
		
		p.sendPacket(htm);
	}
	
	@Override
	public String getType()
	{
		return "[C] 21 RequestBypassToServer";
	}
}
