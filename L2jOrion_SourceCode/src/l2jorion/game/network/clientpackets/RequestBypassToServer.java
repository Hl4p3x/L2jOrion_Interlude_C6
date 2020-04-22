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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javolution.text.TextBuilder;
import javolution.util.FastList;
import l2jorion.Config;
import l2jorion.game.community.CommunityBoard;
import l2jorion.game.datatables.SkillTable;
import l2jorion.game.datatables.sql.AdminCommandAccessRights;
import l2jorion.game.datatables.sql.ItemTable;
import l2jorion.game.handler.AdminCommandHandler;
import l2jorion.game.handler.IAdminCommandHandler;
import l2jorion.game.handler.custom.CustomBypassHandler;
import l2jorion.game.handler.voiced.Vote;
import l2jorion.game.handler.vote.VoteBrasil;
import l2jorion.game.handler.vote.VoteHopzone;
import l2jorion.game.handler.vote.VoteNetwork;
import l2jorion.game.handler.vote.VoteTopzone;
import l2jorion.game.model.L2DropCategory;
import l2jorion.game.model.L2DropData;
import l2jorion.game.model.L2Object;
import l2jorion.game.model.L2Skill;
import l2jorion.game.model.L2World;
import l2jorion.game.model.actor.instance.L2ClassMasterInstance;
import l2jorion.game.model.actor.instance.L2GrandBossInstance;
import l2jorion.game.model.actor.instance.L2MinionInstance;
import l2jorion.game.model.actor.instance.L2NpcInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.actor.instance.L2RaidBossInstance;
import l2jorion.game.model.actor.instance.L2SymbolMakerInstance;
import l2jorion.game.model.entity.event.CTF;
import l2jorion.game.model.entity.event.DM;
import l2jorion.game.model.entity.event.L2Event;
import l2jorion.game.model.entity.event.TvT;
import l2jorion.game.model.entity.event.VIP;
import l2jorion.game.model.entity.olympiad.Olympiad;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.ActionFailed;
import l2jorion.game.network.serverpackets.NpcHtmlMessage;
import l2jorion.game.network.serverpackets.SystemMessage;
import l2jorion.game.util.GMAudit;
import l2jorion.game.util.Util;
import l2jorion.log.Log;
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
			return;
		
		if (!getClient().getFloodProtectors().getServerBypass().tryPerformAction(_command))
		{
			activeChar.sendMessage("You're doing that too fast!");
			return;
		}
		
		try
		{
			if (_command.startsWith("admin_"))
			{
				String command;
				if(_command.indexOf(" ") != -1)
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
						activeChar.sendMessage("The command " + command + " does not exists!");
					}
					String text = "No handler registered for admin command '" + command + "'";
					Log.add(text, "Wrong_admin_commands");
					return;
				}
				
				if (!AdminCommandAccessRights.getInstance().hasAccess(command, activeChar.getAccessLevel()))
				{
					activeChar.sendMessage("You don't have the access right to use this command!");
					if (Config.DEBUG)
					{
						LOG.warn("Character " + activeChar.getName() + " tried to use admin command " + command + ", but doesn't have access to it!");
					}
					return;
				}
				
				if (Config.GMAUDIT)
				{
					GMAudit.auditGMAction(activeChar.getName()+" ["+activeChar.getObjectId()+"]", command, (activeChar.getTarget() != null ? activeChar.getTarget().getName():"no-target"),_command.replace(command, ""));
				}
				
				ach.useAdminCommand(_command, activeChar);
			}
			//check answer
			else if (_command.startsWith("answer "))
			{
				String answer = _command.substring(7);
				switch(answer)
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
			else if(_command.startsWith("buffspage"))
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
			else if(_command.startsWith("buff"))
			{
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
					L2Skill s = SkillTable.getInstance().getInfo(id, lvl);
					s.getEffects(activeChar, activeChar);
					SystemMessage sm = new SystemMessage(SystemMessageId.YOU_FEEL_S1_EFFECT);
					sm.addSkillName(id);
					activeChar.sendPacket(sm);
					
					activeChar.reduceAdena("Buff", target.getBuffPrize(), activeChar, true);
					
					target.getInventory().addItem("", 57, target.getBuffPrize(), target, null);
					
					activeChar.sellBuffsMenu(target, page);
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}
			}
			else if(_command.startsWith("actr"))
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
				//activeChar.setOldNameColor(activeChar.getAppearance().getNameColor());
				//activeChar.getAppearance().setNameColor(99, 22, 11);
				activeChar.setTitle(Config.SELLBUFF_TITLE);
				activeChar.broadcastUserInfo();
				activeChar.broadcastTitleInfo();
			}
			else if(_command.startsWith("player_help "))
			{
				playerHelp(activeChar, _command.substring(12));
			}
			else if (_command.startsWith("vote "))
 			{
				Vote.restoreVotedData(activeChar, activeChar.getClient().getConnection().getInetAddress().getHostAddress());
				
 				String voteSiteName = _command.substring(5);
 				switch(voteSiteName)
 				{
 					case "hopzone":
 							if (activeChar.eligibleToVoteHop())
 							{
 								VoteHopzone voteHop = new VoteHopzone();
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
 								VoteTopzone voteTop = new VoteTopzone();
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
 								VoteNetwork voteNet = new VoteNetwork();
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
								VoteBrasil voteBra = new VoteBrasil();
								if(voteBra.hasVoted(activeChar))
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
					
					if(_command.substring(endOfId + 1).startsWith("event_participate"))
					{
						L2Event.inscribePlayer(activeChar);
					}
					else if(_command.substring(endOfId + 1).startsWith("tvt_player_join "))
					{
						String teamName = _command.substring(endOfId + 1).substring(16);
						
						if(TvT.is_joining())
						{
							TvT.addPlayer(activeChar, teamName);
						}
						else
						{
							activeChar.sendMessage("The event is already started. You can not join now!");
						}
					}
					
					else if(_command.substring(endOfId + 1).startsWith("tvt_player_leave"))
					{
						if(TvT.is_joining())
						{
							TvT.removePlayer(activeChar);
						}
						else
						{
							activeChar.sendMessage("The event is already started. You can not leave now!");
						}
					}
					
					else if(_command.substring(endOfId+1).startsWith("dmevent_player_join"))
					{
						if(DM.is_joining())
							DM.addPlayer(activeChar);
						else
							activeChar.sendMessage("The event is already started. You can't join now!");
					}
					
					else if(_command.substring(endOfId+1).startsWith("dmevent_player_leave"))
					{
						if(DM.is_joining())
							DM.removePlayer(activeChar);
						else
							activeChar.sendMessage("The event is already started. You can't leave now!");
					}
					
					else if(_command.substring(endOfId+1).startsWith("ctf_player_join "))
					{
						String teamName = _command.substring(endOfId+1).substring(16);
						if(CTF.is_joining())
							CTF.addPlayer(activeChar, teamName);
						else
							activeChar.sendMessage("The event is already started. You can't join now!");
					}
					
					else if(_command.substring(endOfId+1).startsWith("ctf_player_leave"))
					{
						if(CTF.is_joining())
							CTF.removePlayer(activeChar);
						else
							activeChar.sendMessage("The event is already started. You can't leave now!");
					}
					
					if(_command.substring(endOfId+1).startsWith("vip_joinVIPTeam"))
					{
							VIP.addPlayerVIP(activeChar);
					}
					
					if(_command.substring(endOfId+1).startsWith("vip_joinNotVIPTeam"))
					{
							VIP.addPlayerNotVIP(activeChar);
					}
					
					if(_command.substring(endOfId+1).startsWith("vip_finishVIP"))
					{
							VIP.vipWin(activeChar);
					}
					
					if(_command.substring(endOfId+1).startsWith("event_participate"))
					{
						L2Event.inscribePlayer(activeChar);
					}
					
					else if((Config.ALLOW_CLASS_MASTERS && 
						Config.ALLOW_REMOTE_CLASS_MASTERS && 
						object instanceof L2ClassMasterInstance)
						|| (object instanceof L2NpcInstance && endOfId > 0 
							&& Util.checkIfInRange(L2NpcInstance.INTERACTION_DISTANCE, activeChar, object, true)))
					{
						((L2NpcInstance) object).onBypassFeedback(activeChar, _command.substring(endOfId + 1));
					}
					
					activeChar.sendPacket(ActionFailed.STATIC_PACKET);
				}
				catch(NumberFormatException nfe)
				{
					if(Config.ENABLE_ALL_EXCEPTIONS)
						nfe.printStackTrace();
					
				}
			}
			//	Draw a Symbol
			else if(_command.equals("Draw"))
			{
				L2Object object = activeChar.getTarget();
				if(object instanceof L2NpcInstance)
				{
					((L2SymbolMakerInstance) object).onBypassFeedback(activeChar, _command);
				}
			}
			else if(_command.equals("RemoveList"))
			{
				L2Object object = activeChar.getTarget();
				if(object instanceof L2NpcInstance)
				{
					((L2SymbolMakerInstance) object).onBypassFeedback(activeChar, _command);
				}
			}
			else if(_command.equals("Remove "))
			{
				L2Object object = activeChar.getTarget();

				if(object instanceof L2NpcInstance)
				{
					((L2SymbolMakerInstance) object).onBypassFeedback(activeChar, _command);
				}
			}
			// Navigate throught Manor windows
			else if(_command.startsWith("manor_menu_select?"))
			{
				L2Object object = activeChar.getTarget();
				if(object instanceof L2NpcInstance)
				{
					((L2NpcInstance) object).onBypassFeedback(activeChar, _command);
				}
			}
			else if (_command.startsWith("bbs_") || _command.startsWith("_bbs") || _command.startsWith("_friend") || _command.startsWith("_mail") || _command.startsWith("_block"))
			{
				CommunityBoard.getInstance().handleCommands(getClient(), _command);
			}
			else if(_command.startsWith("Quest "))
			{
				if(!activeChar.validateBypass(_command))
					return;
				
				L2PcInstance player = getClient().getActiveChar();
				if(player == null)
					return;
				
				String p = _command.substring(6).trim();
				int idx = p.indexOf(' ');
				
				if(idx < 0)
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
							LOG.warn("test5");
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
							activeChar.sendMessage("Your Pin Code is: " +pin);
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
						con = null;
					}
				}
				catch (Exception e)
				{
					e.printStackTrace();
					activeChar.sendMessage("The Pin Code MUST be 4 numbers.");
					activeChar.showEnterPinHtml();
				}
			}
			else if(_command.startsWith("custom_"))
			{
				L2PcInstance player = getClient().getActiveChar();
				CustomBypassHandler.getInstance().handleBypass(player, _command);
			}
			else if (_command.startsWith("OlympiadArenaChange"))
			{
				Olympiad.bypassChangeArena(_command, activeChar);
			}
			else if (_command.startsWith("dropInfo"))
			{
				L2NpcInstance target = (L2NpcInstance) activeChar.getTarget();
				int color = 1;
				int color2 = 1;
				if (target != null)
				{
					NpcHtmlMessage html = new NpcHtmlMessage(0);
					TextBuilder html1 = new TextBuilder("<html><body>");
					
					html1.append("<center><font color=\"3399ff\">Drop</font></center>");
					
					html1.append("<img src=\"l2ui.squaregray\" width=\"295\" height=\"1\" align=center>");
					for (final L2DropCategory cat : target.getTemplate().getDropData())
					{
						final FastList<L2DropData> drops = cat.getAllDrops();
						if (drops != null && !cat.isSweep())
						{
							for (final L2DropData drop : drops)
							{
								if (drop == null || ItemTable.getInstance().getTemplate(drop.getItemId()) == null)
								{
									continue;
								}
								
								String name = ItemTable.getInstance().getTemplate(drop.getItemId()).getName();
								
								if (name.length() >= 39)
								{
									name = name.substring(0,39)+"...";
								}
								
								float chance = Float.parseFloat(""+drop.getChance()) / 1000000 * 100;
								
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
								
								NumberFormat defaultFormat = NumberFormat.getNumberInstance();
								defaultFormat.setMinimumFractionDigits(4);
								String chance2 = defaultFormat.format(chance);
								
								if (color == 1)
								{
									html1.append("<table border=0 cellspacing=\"2\" cellpadding=\"0\" width=\"300\" bgcolor=\"000000\">");
									html1.append("<tr><td width=240>" + name + " (<font color=LEVEL>"+drop.getMinDrop()+"</font>-<font color=LEVEL>"+drop.getMaxDrop()+"</font>)</td><td width=60>"+chance2+"%</td></tr>");
									html1.append("</table>");
									html1.append("<img src=\"l2ui.squaregray\" width=\"295\" height=\"1\" align=center>");
									color = 2;
								}
								else
								{
									html1.append("<table border=0 cellspacing=\"2\" cellpadding=\"0\" width=\"300\">");
									html1.append("<tr><td width=240>" + name + " (<font color=LEVEL>"+drop.getMinDrop()+"</font>-<font color=LEVEL>"+drop.getMaxDrop()+"</font>)</td><td width=60>"+chance2+"%</td></tr>");
									html1.append("</table>");
									html1.append("<img src=\"l2ui.squaregray\" width=\"295\" height=\"1\" align=center>");
									color = 1;
								}
							}
						}
					}
					
					
					html1.append("<br><center><font color=\"ff0000\">Spoil</font></center>");
					html1.append("<img src=\"l2ui.squaregray\" width=\"295\" height=\"1\" align=center>");
					for (final L2DropCategory cat : target.getTemplate().getDropData())
					{
						final FastList<L2DropData> drops = cat.getAllDrops();
						if (drops != null && cat.isSweep())
						{
							for (final L2DropData drop : drops)
							{
								if (drop == null || ItemTable.getInstance().getTemplate(drop.getItemId()) == null)
								{
									continue;
								}
								
								String name = ItemTable.getInstance().getTemplate(drop.getItemId()).getName();
								if (name.length() >= 39)
								{
									name = name.substring(0,39)+"...";
								}
								
								float chance = Float.parseFloat(""+drop.getChance()) / 1000000 * 100;
								
								if (target instanceof L2RaidBossInstance)
								{
									chance *= Config.SPOIL_RAID;
								}
								else if (target instanceof L2GrandBossInstance)
								{
									chance *= Config.SPOIL_BOSS;
								}
								else if (target instanceof L2MinionInstance)
								{
									chance *= Config.SPOIL_MINON;
								}
								else
								{
									chance *= Config.RATE_DROP_SPOIL;
									
									if (activeChar.getPremiumService() == 1)
									{
										chance *= Config.PREMIUM_SPOIL_RATE;
									}
								}
								
								if (chance >= 100)
								{
									chance = 100;
								}
								
								NumberFormat defaultFormat = NumberFormat.getNumberInstance();
								defaultFormat.setMinimumFractionDigits(4);
								String chance2 = defaultFormat.format(chance);
								
								if (color2 == 1)
								{
									html1.append("<table border=0 cellspacing=\"2\" cellpadding=\"0\" width=\"300\" bgcolor=\"000000\">");
									html1.append("<tr><td width=240>" + name + " (<font color=LEVEL>"+drop.getMinDrop()+"</font>-<font color=LEVEL>"+drop.getMaxDrop()+"</font>)</td><td width=60>"+chance2+"%</td></tr>");
									html1.append("</table>");
									html1.append("<img src=\"l2ui.squaregray\" width=\"295\" height=\"1\" align=center>");
									color2 = 2;
								}
								else
								{
									html1.append("<table border=0 cellspacing=\"2\" cellpadding=\"0\" width=\"300\">");
									html1.append("<tr><td width=240>" + name + " (<font color=LEVEL>"+drop.getMinDrop()+"</font>-<font color=LEVEL>"+drop.getMaxDrop()+"</font>)</td><td width=60>"+chance2+"%</td></tr>");
									html1.append("</table>");
									html1.append("<img src=\"l2ui.squaregray\" width=\"295\" height=\"1\" align=center>");
									color2 = 1;
								}
							}
						}
					}
					
					html.setHtml(html1.toString());
					activeChar.sendPacket(html);
				}
			}
		}
		catch(Exception e)
		{
			if(Config.ENABLE_ALL_EXCEPTIONS)
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
	
	@Override
	public String getType()
	{
		return "[C] 21 RequestBypassToServer";
	}
}
