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

import java.text.NumberFormat;
import java.util.List;
import java.util.StringTokenizer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javolution.text.TextBuilder;
import l2jorion.Config;
import l2jorion.game.cache.HtmCache;
import l2jorion.game.community.CommunityBoardManager;
import l2jorion.game.datatables.SkillTable;
import l2jorion.game.datatables.sql.AdminCommandAccessRights;
import l2jorion.game.datatables.sql.ItemTable;
import l2jorion.game.datatables.xml.DressMeData;
import l2jorion.game.handler.AdminCommandHandler;
import l2jorion.game.handler.IAdminCommandHandler;
import l2jorion.game.handler.custom.CustomBypassHandler;
import l2jorion.game.handler.voice.Vote;
import l2jorion.game.handler.vote.Brasil;
import l2jorion.game.handler.vote.Hopzone;
import l2jorion.game.handler.vote.L2TopGr;
import l2jorion.game.handler.vote.L2TopOnline;
import l2jorion.game.handler.vote.Network;
import l2jorion.game.handler.vote.Topzone;
import l2jorion.game.managers.BypassManager.DecodedBypass;
import l2jorion.game.model.L2Character;
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
import l2jorion.game.model.base.SkinPackage;
import l2jorion.game.model.entity.event.CTF;
import l2jorion.game.model.entity.event.DM;
import l2jorion.game.model.entity.event.L2Event;
import l2jorion.game.model.entity.event.TvT;
import l2jorion.game.model.entity.event.VIP;
import l2jorion.game.model.olympiad.OlympiadManager;
import l2jorion.game.network.PacketClient;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.ActionFailed;
import l2jorion.game.network.serverpackets.ExShowScreenMessage;
import l2jorion.game.network.serverpackets.NpcHtmlMessage;
import l2jorion.game.network.serverpackets.OpenUrl;
import l2jorion.game.network.serverpackets.PlaySound;
import l2jorion.game.network.serverpackets.SystemMessage;
import l2jorion.game.templates.L2Item;
import l2jorion.game.templates.L2WeaponType;
import l2jorion.game.thread.ThreadPoolManager;
import l2jorion.game.util.GMAudit;
import l2jorion.game.util.Util;
import l2jorion.log.Log;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;

public final class RequestBypassToServer extends PacketClient
{
	private static Logger LOG = LoggerFactory.getLogger(RequestBypassToServer.class.getName());
	
	private DecodedBypass bp = null;
	
	@Override
	protected void readImpl()
	{
		String bypass = readS();
		if (!bypass.isEmpty())
		{
			bp = getClient().getActiveChar().decodeBypass(bypass);
		}
	}
	
	@Override
	protected void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null || bp == null)
		{
			return;
		}
		
		if (!getClient().getFloodProtectors().getServerBypass().tryPerformAction(bp.bypass))
		{
			activeChar.sendMessage("You're doing that too fast.");
			return;
		}
		
		try
		{
			if (bp.bypass.startsWith("admin_"))
			{
				String command;
				if (bp.bypass.indexOf(" ") != -1)
				{
					command = bp.bypass.substring(0, bp.bypass.indexOf(" "));
				}
				else
				{
					command = bp.bypass;
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
					return;
				}
				
				if (Config.GMAUDIT)
				{
					GMAudit.auditGMAction(activeChar.getName() + " [" + activeChar.getObjectId() + "]", command, (activeChar.getTarget() != null ? activeChar.getTarget().getName() : "no-target"), bp.bypass.replace(command, ""));
				}
				
				ach.useAdminCommand(bp.bypass, activeChar);
			}
			else if (bp.bypass.startsWith("answer "))
			{
				String answer = bp.bypass.substring(7);
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
			else if (bp.bypass.startsWith("buffspage"))
			{
				String[] val = bp.bypass.split(" ");
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
			else if (bp.bypass.startsWith("buff"))
			{
				if (!Config.SELLBUFF_SYSTEM)
				{
					return;
				}
				
				String[] val = bp.bypass.split(" ");
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
				
				if (activeChar.getInventory().getItemByItemId(57) == null || activeChar.getInventory().getItemByItemId(57).getCount() < target.getBuffPrize())
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
					
					// Gift of Seraphim
					L2Skill skill1 = SkillTable.getInstance().getInfo(4703, 13);
					// Blessing of Seraphim
					L2Skill skill2 = SkillTable.getInstance().getInfo(4702, 13);
					
					// Gift of Queen
					L2Skill skill3 = SkillTable.getInstance().getInfo(4700, 13);
					// Blessing of Queen
					L2Skill skill4 = SkillTable.getInstance().getInfo(4699, 13);
					
					if (target.BuffsList.contains(skill1) && target.BuffsList.contains(skill2) && (target.getClassId().getId() == 28 || target.getClassId().getId() == 104))
					{
						isOK = true;
					}
					
					if (target.BuffsList.contains(skill3) && target.BuffsList.contains(skill4) && (target.getClassId().getId() == 14 || target.getClassId().getId() == 96))
					{
						isOK = true;
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
			else if (bp.bypass.startsWith("actr"))
			{
				String l = bp.bypass.substring(5);
				
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
			else if (bp.bypass.startsWith("player_help "))
			{
				playerHelp(activeChar, bp.bypass.substring(12));
			}
			else if (bp.bypass.startsWith("open_url "))
			{
				activeChar.sendPacket(new OpenUrl(bp.bypass.substring(9)));
			}
			else if (bp.bypass.startsWith("vote "))
			{
				Vote.restoreVotedData(activeChar, activeChar.getClient().getConnection().getInetAddress().getHostAddress());
				
				String voteSiteName = bp.bypass.substring(5);
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
			else if (bp.bypass.startsWith("npc_"))
			{
				if (!activeChar.validateBypass(bp.bypass))
				{
					return;
				}
				
				int endOfId = bp.bypass.indexOf('_', 5);
				String id;
				
				if (endOfId > 0)
				{
					id = bp.bypass.substring(4, endOfId);
				}
				else
				{
					id = bp.bypass.substring(4);
				}
				
				try
				{
					L2Object object = L2World.getInstance().findObject(Integer.parseInt(id));
					
					if (bp.bypass.substring(endOfId + 1).startsWith("event_participate"))
					{
						L2Event.inscribePlayer(activeChar);
					}
					else if (bp.bypass.substring(endOfId + 1).startsWith("tvt_player_join "))
					{
						String teamName = bp.bypass.substring(endOfId + 1).substring(16);
						
						if (TvT.is_joining())
						{
							TvT.addPlayer(activeChar, teamName);
						}
						else
						{
							activeChar.sendMessage("The event is already started. You can not join now!");
						}
					}
					
					else if (bp.bypass.substring(endOfId + 1).startsWith("tvt_player_leave"))
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
					
					else if (bp.bypass.substring(endOfId + 1).startsWith("dmevent_player_join"))
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
					
					else if (bp.bypass.substring(endOfId + 1).startsWith("dmevent_player_leave"))
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
					
					else if (bp.bypass.substring(endOfId + 1).startsWith("ctf_player_join "))
					{
						String teamName = bp.bypass.substring(endOfId + 1).substring(16);
						if (CTF.is_joining())
						{
							CTF.addPlayer(activeChar, teamName);
						}
						else
						{
							activeChar.sendMessage("The event is already started. You can't join now!");
						}
					}
					
					else if (bp.bypass.substring(endOfId + 1).startsWith("ctf_player_leave"))
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
					
					if (bp.bypass.substring(endOfId + 1).startsWith("vip_joinVIPTeam"))
					{
						VIP.addPlayerVIP(activeChar);
					}
					
					if (bp.bypass.substring(endOfId + 1).startsWith("vip_joinNotVIPTeam"))
					{
						VIP.addPlayerNotVIP(activeChar);
					}
					
					if (bp.bypass.substring(endOfId + 1).startsWith("vip_finishVIP"))
					{
						VIP.vipWin(activeChar);
					}
					
					if (bp.bypass.substring(endOfId + 1).startsWith("event_participate"))
					{
						L2Event.inscribePlayer(activeChar);
					}
					
					else if ((Config.ALLOW_CLASS_MASTERS && Config.ALLOW_REMOTE_CLASS_MASTERS && object instanceof L2ClassMasterInstance) || (object instanceof L2NpcInstance && endOfId > 0 && Util.checkIfInRange(L2NpcInstance.INTERACTION_DISTANCE, activeChar, object, true)))
					{
						((L2NpcInstance) object).onBypassFeedback(activeChar, bp.bypass.substring(endOfId + 1));
					}
					
					activeChar.sendPacket(ActionFailed.STATIC_PACKET);
				}
				catch (NumberFormatException e)
				{
					LOG.info(this.getClass().getSimpleName() + ": (" + activeChar.getName() + ") error: " + e);
				}
			}
			else if (bp.bypass.equals("Draw"))
			{
				L2Object object = activeChar.getTarget();
				if (object instanceof L2NpcInstance)
				{
					((L2SymbolMakerInstance) object).onBypassFeedback(activeChar, bp.bypass);
				}
			}
			else if (bp.bypass.equals("RemoveList"))
			{
				L2Object object = activeChar.getTarget();
				if (object instanceof L2NpcInstance)
				{
					((L2SymbolMakerInstance) object).onBypassFeedback(activeChar, bp.bypass);
				}
			}
			else if (bp.bypass.equals("Remove "))
			{
				L2Object object = activeChar.getTarget();
				
				if (object instanceof L2NpcInstance)
				{
					((L2SymbolMakerInstance) object).onBypassFeedback(activeChar, bp.bypass);
				}
			}
			else if (bp.bypass.startsWith("manor_menu_select?"))
			{
				L2Object object = activeChar.getTarget();
				if (object instanceof L2NpcInstance)
				{
					((L2NpcInstance) object).onBypassFeedback(activeChar, bp.bypass);
				}
			}
			else if (bp.bypass.startsWith("bbs_") || bp.bypass.startsWith("_bbs") || bp.bypass.startsWith("_friend") || bp.bypass.startsWith("_mail") || bp.bypass.startsWith("_block"))
			{
				CommunityBoardManager.getInstance().onBypassCommand(getClient(), bp.bypass);
			}
			else if (bp.bypass.startsWith("Quest "))
			{
				if (!activeChar.validateBypass(bp.bypass))
				{
					return;
				}
				
				L2PcInstance player = getClient().getActiveChar();
				if (player == null)
				{
					return;
				}
				
				String p = bp.bypass.substring(6).trim();
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
			else if (bp.bypass.startsWith("custom_"))
			{
				L2PcInstance player = getClient().getActiveChar();
				CustomBypassHandler.getInstance().handleBypass(player, bp.bypass);
			}
			else if (bp.bypass.startsWith("arenachange")) // change
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
				
				final int arenaId = Integer.parseInt(bp.bypass.substring(12).trim());
				activeChar.enterOlympiadObserverMode(arenaId);
			}
			else if (bp.bypass.startsWith("dropInfo"))
			{
				if (activeChar.getTarget() == null || !(activeChar.getTarget() instanceof L2NpcInstance))
				{
					return;
				}
				
				final String[] val = bp.bypass.split(" ");
				
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
							
							float chance = drop.getChance();
							int deepBlueDrop = 1;
							int levelModifier = 0;
							
							if (Config.DEEPBLUE_DROP_RULES)
							{
								levelModifier = calculateLevelModifierForDrop(activeChar, target);
								
								if (levelModifier > 0)
								{
									deepBlueDrop = 3;
								}
								
								chance = ((chance - ((chance * levelModifier) / 100)) / deepBlueDrop);
							}
							
							if ((drop.getItemId() == 57 || drop.getItemId() >= 6360 && drop.getItemId() <= 6362))
							{
								// like l2off must be drop chance x1 no matter what
								chance *= 1;
							}
							else
							{
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
									chance *= Config.ITEMS_MINION;
								}
								else
								{
									chance *= Config.RATE_DROP_ITEMS;
									
									if (activeChar.getPremiumService() >= 1)
									{
										chance *= Config.PREMIUM_DROP_RATE;
									}
								}
							}
							
							int minDrop = drop.getMinDrop();
							int maxDrop = drop.getMaxDrop();
							
							if (drop.getItemId() >= 6360 && drop.getItemId() <= 6362)
							{
								minDrop *= Config.RATE_DROP_SEAL_STONES;
								maxDrop *= Config.RATE_DROP_SEAL_STONES;
								if (activeChar.getPremiumService() >= 1)
								{
									minDrop *= Config.PREMIUM_SS_RATE;
									maxDrop *= Config.PREMIUM_SS_RATE;
								}
							}
							else if (drop.getItemId() == 57)
							{
								if (target instanceof L2RaidBossInstance)
								{
									minDrop *= Config.ADENA_RAID;
									maxDrop *= Config.ADENA_RAID;
								}
								else if (target instanceof L2GrandBossInstance)
								{
									minDrop *= Config.ADENA_BOSS;
									maxDrop *= Config.ADENA_BOSS;
								}
								else if (target instanceof L2MinionInstance)
								{
									minDrop *= Config.ADENA_MINION;
									maxDrop *= Config.ADENA_MINION;
								}
								else
								{
									minDrop *= Config.RATE_DROP_ADENA;
									maxDrop *= Config.RATE_DROP_ADENA;
									
									if (activeChar.getPremiumService() >= 1)
									{
										minDrop *= Config.PREMIUM_ADENA_RATE;
										maxDrop *= Config.PREMIUM_ADENA_RATE;
									}
								}
							}
							
							String itemIcon = L2Item.getItemIcon(drop.getItemId());
							
							chance = (chance / 1000000) * 100;
							
							if (chance >= 100)
							{
								chance = 100;
							}
							
							if (Config.L2UNLIMITED_CUSTOM)
							{
								if (chance < 2)
								{
									chance = 2;
								}
							}
							else
							{
								if (chance < 0)
								{
									chance = 0;
								}
							}
							
							String formatedIntAndChance = formatIntAndColor(chance);
							
							String dropCount = (maxDrop > 1 ? "<font color=LEVEL>" + Util.formatItem(minDrop) + "</font>-<font color=LEVEL>" + Util.formatItem(maxDrop) + "</font>" : "<font color=LEVEL>" + Util.formatItem(minDrop) + "</font>");
							
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
			else if (bp.bypass.startsWith("dressme"))
			{
				if (!Config.ALLOW_DRESS_ME_IN_OLY && activeChar.isInOlympiadMode())
				{
					activeChar.sendMessage("DressMe can't be used on The Olympiad game.");
					return;
				}
				
				StringTokenizer st = new StringTokenizer(bp.bypass, " ");
				st.nextToken();
				if (!st.hasMoreTokens())
				{
					showDressMeMainPage(activeChar);
					return;
				}
				int page = Integer.parseInt(st.nextToken());
				
				if (!st.hasMoreTokens())
				{
					showDressMeMainPage(activeChar);
					return;
				}
				String next = st.nextToken();
				if (next.startsWith("skinlist"))
				{
					String type = st.nextToken();
					showSkinList(activeChar, type, page);
				}
				else if (next.startsWith("myskinlist"))
				{
					showMySkinList(activeChar, page);
				}
				if (next.equals("clean"))
				{
					String type = st.nextToken();
					
					if (activeChar.isTryingSkin())
					{
						activeChar.sendMessage("You can't do this while trying a skin.");
						activeChar.sendPacket(new ExShowScreenMessage("You can't do this while trying a skin.", 2000, 2, false));
						activeChar.sendPacket(new PlaySound("ItemSound3.sys_impossible"));
						showDressMeMainPage(activeChar);
						return;
					}
					
					switch (type.toLowerCase())
					{
						case "armor":
							activeChar.setArmorSkinOption(0);
							break;
						case "weapon":
							activeChar.setWeaponSkinOption(0);
							break;
						case "hair":
							activeChar.setHairSkinOption(0);
							break;
						case "face":
							activeChar.setFaceSkinOption(0);
							break;
					}
					
					activeChar.broadcastUserInfo();
					showMySkinList(activeChar, page);
				}
				else if (next.startsWith("buyskin"))
				{
					if (!st.hasMoreTokens())
					{
						showDressMeMainPage(activeChar);
						return;
					}
					
					int skinId = Integer.parseInt(st.nextToken());
					String type = st.nextToken();
					int itemId = Integer.parseInt(st.nextToken());
					
					SkinPackage sp = null;
					
					switch (type.toLowerCase())
					{
						case "armor":
							sp = DressMeData.getInstance().getArmorSkinsPackage(skinId);
							break;
						case "weapon":
							sp = DressMeData.getInstance().getWeaponSkinsPackage(skinId);
							
							if (activeChar.getActiveWeaponItem() == null)
							{
								activeChar.sendMessage("You can't buy this skin without a weapon.");
								activeChar.sendPacket(new ExShowScreenMessage("You can't buy this skin without a weapon.", 2000, 2, false));
								activeChar.sendPacket(new PlaySound("ItemSound3.sys_impossible"));
								showSkinList(activeChar, type, page);
								return;
							}
							
							L2ItemInstance skinWeapon = null;
							if (ItemTable.getInstance().getTemplate(itemId) != null)
							{
								skinWeapon = ItemTable.getInstance().createDummyItem(itemId);
								
								if (!checkWeapons(activeChar, skinWeapon, L2WeaponType.BOW, L2WeaponType.BOW) //
									|| !checkWeapons(activeChar, skinWeapon, L2WeaponType.SWORD, L2WeaponType.SWORD) //
									|| !checkWeapons(activeChar, skinWeapon, L2WeaponType.BLUNT, L2WeaponType.BLUNT) //
									|| !checkWeapons(activeChar, skinWeapon, L2WeaponType.DAGGER, L2WeaponType.DAGGER) //
									|| !checkWeapons(activeChar, skinWeapon, L2WeaponType.POLE, L2WeaponType.POLE) //
									|| !checkWeapons(activeChar, skinWeapon, L2WeaponType.DUAL, L2WeaponType.DUAL) //
									|| !checkWeapons(activeChar, skinWeapon, L2WeaponType.DUALFIST, L2WeaponType.DUALFIST) //
									|| !checkWeapons(activeChar, skinWeapon, L2WeaponType.BIGSWORD, L2WeaponType.BIGSWORD) //
									|| !checkWeapons(activeChar, skinWeapon, L2WeaponType.FIST, L2WeaponType.FIST) //
									|| !checkWeapons(activeChar, skinWeapon, L2WeaponType.BIGBLUNT, L2WeaponType.BIGBLUNT))
								{
									activeChar.sendMessage("This skin is not suitable for your weapon type.");
									activeChar.sendPacket(new ExShowScreenMessage("This skin is not suitable for your weapon type.", 2000, 2, false));
									activeChar.sendPacket(new PlaySound("ItemSound3.sys_impossible"));
									showSkinList(activeChar, type, page);
									return;
								}
							}
							break;
						case "hair":
							sp = DressMeData.getInstance().getHairSkinsPackage(skinId);
							break;
						case "face":
							sp = DressMeData.getInstance().getFaceSkinsPackage(skinId);
							break;
					}
					
					if (sp == null)
					{
						activeChar.sendMessage("There is no such skin.");
						activeChar.sendPacket(new ExShowScreenMessage("There is no such skin.", 2000, 2, false));
						activeChar.sendPacket(new PlaySound("ItemSound3.sys_impossible"));
						showSkinList(activeChar, type, page);
						return;
					}
					
					if ((Config.L2UNLIMITED_CUSTOM || Config.RON_CUSTOM) && activeChar.getPremiumService() >= 1)
					{
						activeChar.sendMessage("You have successfully purchased " + sp.getName() + " skin.");
						activeChar.sendPacket(new ExShowScreenMessage("You have successfully purchased " + sp.getName() + " skin.", 2000, 2, false));
						
						if (!Config.RON_CUSTOM)
						{
							activeChar.setIsTryingSkinPremium(true);
						}
						
						switch (type.toLowerCase())
						{
							case "armor":
								activeChar.setArmorSkinOption(skinId);
								break;
							case "weapon":
								activeChar.setWeaponSkinOption(skinId);
								break;
							case "hair":
								activeChar.setHairSkinOption(skinId);
								break;
							case "face":
								activeChar.setFaceSkinOption(skinId);
								break;
						}
						
						activeChar.broadcastUserInfo();
						showSkinList(activeChar, type, page);
						return;
					}
					
					if (activeChar.destroyItemByItemId("dressme", sp.getPriceId(), sp.getPriceCount(), activeChar, true))
					{
						activeChar.sendMessage("You have successfully purchased " + sp.getName() + " skin.");
						activeChar.sendPacket(new ExShowScreenMessage("You have successfully purchased " + sp.getName() + " skin.", 2000, 2, false));
						
						switch (type.toLowerCase())
						{
							case "armor":
								activeChar.buyArmorSkin(skinId);
								activeChar.setArmorSkinOption(skinId);
								break;
							case "weapon":
								activeChar.buyWeaponSkin(skinId);
								activeChar.setWeaponSkinOption(skinId);
								break;
							case "hair":
								activeChar.buyHairSkin(skinId);
								activeChar.setHairSkinOption(skinId);
								break;
							case "face":
								activeChar.buyFaceSkin(skinId);
								activeChar.setFaceSkinOption(skinId);
								break;
						}
						
						activeChar.broadcastUserInfo();
					}
					showSkinList(activeChar, type, page);
				}
				else if (next.startsWith("tryskin"))
				{
					int skinId = Integer.parseInt(st.nextToken());
					String type = st.nextToken();
					
					if (activeChar.isTryingSkin())
					{
						activeChar.sendMessage("You are already trying a skin.");
						activeChar.sendPacket(new ExShowScreenMessage("You are already trying a skin.", 2000, 2, false));
						activeChar.sendPacket(new PlaySound("ItemSound3.sys_impossible"));
						showSkinList(activeChar, type, page);
						return;
					}
					
					activeChar.setIsTryingSkin(true);
					
					int oldArmorSkinId = activeChar.getArmorSkinOption();
					int oldWeaponSkinId = activeChar.getWeaponSkinOption();
					int oldHairSkinId = activeChar.getHairSkinOption();
					int oldFaceSkinId = activeChar.getFaceSkinOption();
					
					switch (type.toLowerCase())
					{
						case "armor":
							activeChar.setArmorSkinOption(skinId);
							break;
						case "weapon":
							activeChar.setWeaponSkinOption(skinId);
							break;
						case "hair":
							activeChar.setHairSkinOption(skinId);
							break;
						case "face":
							activeChar.setFaceSkinOption(skinId);
							break;
					}
					
					activeChar.broadcastUserInfo();
					showSkinList(activeChar, type, page);
					
					ThreadPoolManager.getInstance().scheduleGeneral(() ->
					{
						switch (type.toLowerCase())
						{
							case "armor":
								activeChar.setArmorSkinOption(oldArmorSkinId);
								break;
							case "weapon":
								activeChar.setWeaponSkinOption(oldWeaponSkinId);
								break;
							case "hair":
								activeChar.setHairSkinOption(oldHairSkinId);
								break;
							case "face":
								activeChar.setFaceSkinOption(oldFaceSkinId);
								break;
						}
						
						activeChar.broadcastUserInfo();
						activeChar.setIsTryingSkin(false);
					}, 5000);
				}
				else if (next.startsWith("setskin"))
				{
					int id = Integer.parseInt(st.nextToken());
					String type = st.nextToken();
					int itemId = Integer.parseInt(st.nextToken());
					
					if (activeChar.isTryingSkin())
					{
						activeChar.sendMessage("You can't do this while trying skins.");
						activeChar.sendPacket(new ExShowScreenMessage("You can't do this while trying skins.", 2000, 2, false));
						activeChar.sendPacket(new PlaySound("ItemSound3.sys_impossible"));
						showMySkinList(activeChar, page);
						return;
					}
					
					if (type.toLowerCase().contains("armor") && activeChar.hasEquippedArmorSkin(String.valueOf(id)) || type.toLowerCase().contains("weapon") && activeChar.hasEquippedWeaponSkin(String.valueOf(id))
						|| type.toLowerCase().contains("hair") && activeChar.hasEquippedHairSkin(String.valueOf(id)) || type.toLowerCase().contains("face") && activeChar.hasEquippedFaceSkin(String.valueOf(id)))
					{
						activeChar.sendMessage("You are already equipped this skin.");
						activeChar.sendPacket(new ExShowScreenMessage("You are already equipped this skin.", 2000, 2, false));
						activeChar.sendPacket(new PlaySound("ItemSound3.sys_impossible"));
						showMySkinList(activeChar, page);
						return;
					}
					
					switch (type.toLowerCase())
					{
						case "armor":
							activeChar.setArmorSkinOption(id);
							break;
						case "weapon":
							if (activeChar.getActiveWeaponItem() == null)
							{
								activeChar.sendMessage("You can't use this skin without a weapon.");
								activeChar.sendPacket(new ExShowScreenMessage("You can't use this skin without a weapon.", 2000, 2, false));
								activeChar.sendPacket(new PlaySound("ItemSound3.sys_impossible"));
								showMySkinList(activeChar, page);
								return;
							}
							
							L2ItemInstance skinWeapon = null;
							if (ItemTable.getInstance().getTemplate(itemId) != null)
							{
								skinWeapon = ItemTable.getInstance().createDummyItem(itemId);
								
								if (!checkWeapons(activeChar, skinWeapon, L2WeaponType.BOW, L2WeaponType.BOW) //
									|| !checkWeapons(activeChar, skinWeapon, L2WeaponType.SWORD, L2WeaponType.SWORD) //
									|| !checkWeapons(activeChar, skinWeapon, L2WeaponType.BLUNT, L2WeaponType.BLUNT) //
									|| !checkWeapons(activeChar, skinWeapon, L2WeaponType.DAGGER, L2WeaponType.DAGGER) //
									|| !checkWeapons(activeChar, skinWeapon, L2WeaponType.POLE, L2WeaponType.POLE) //
									|| !checkWeapons(activeChar, skinWeapon, L2WeaponType.DUAL, L2WeaponType.DUAL) //
									|| !checkWeapons(activeChar, skinWeapon, L2WeaponType.DUALFIST, L2WeaponType.DUALFIST) //
									|| !checkWeapons(activeChar, skinWeapon, L2WeaponType.BIGSWORD, L2WeaponType.BIGSWORD) //
									|| !checkWeapons(activeChar, skinWeapon, L2WeaponType.FIST, L2WeaponType.FIST) //
									|| !checkWeapons(activeChar, skinWeapon, L2WeaponType.BIGBLUNT, L2WeaponType.BIGBLUNT))
								{
									activeChar.sendMessage("This skin is not suitable for your weapon type.");
									activeChar.sendPacket(new ExShowScreenMessage("This skin is not suitable for your weapon type.", 2000, 2, false));
									activeChar.sendPacket(new PlaySound("ItemSound3.sys_impossible"));
									showMySkinList(activeChar, page);
									return;
								}
								
								activeChar.setWeaponSkinOption(id);
							}
							break;
						case "hair":
							activeChar.setHairSkinOption(id);
							break;
						case "face":
							activeChar.setFaceSkinOption(id);
							break;
					}
					
					activeChar.broadcastUserInfo();
					showMySkinList(activeChar, page);
				}
			}
		}
		catch (Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			
			LOG.warn("RequestBypassToServer: ", e);
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
	
	public static void showDressMeMainPage(L2PcInstance player)
	{
		NpcHtmlMessage htm = new NpcHtmlMessage(1);
		String text = HtmCache.getInstance().getHtm("data/html/dressme/index.htm");
		htm.setHtml(text);
		player.sendPacket(htm);
	}
	
	private static void showSkinList(L2PcInstance player, String type, int page)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(1);
		html.setFile("data/html/dressme/allskins.htm");
		final int ITEMS_PER_PAGE = 8;
		
		int myPage = 1;
		int i = 0;
		int shown = 0;
		boolean hasMore = false;
		int itemId = 0;
		
		final StringBuilder sb = new StringBuilder();
		
		List<SkinPackage> tempList = null;
		switch (type.toLowerCase())
		{
			case "armor":
				tempList = DressMeData.getInstance().getArmorSkinOptions().values().stream().filter(s -> !player.hasArmorSkin(s.getId())).collect(Collectors.toList());
				break;
			case "weapon":
				tempList = DressMeData.getInstance().getWeaponSkinOptions().values().stream().filter(s -> !player.hasWeaponSkin(s.getId())).collect(Collectors.toList());
				break;
			case "hair":
				tempList = DressMeData.getInstance().getHairSkinOptions().values().stream().filter(s -> !player.hasHairSkin(s.getId())).collect(Collectors.toList());
				break;
			case "face":
				tempList = DressMeData.getInstance().getFaceSkinOptions().values().stream().filter(s -> !player.hasFaceSkin(s.getId())).collect(Collectors.toList());
				break;
		}
		
		if (tempList != null && !tempList.isEmpty())
		{
			for (SkinPackage sp : tempList)
			{
				if (sp == null)
				{
					continue;
				}
				
				if (shown == ITEMS_PER_PAGE)
				{
					hasMore = true;
					break;
				}
				
				if (myPage != page)
				{
					i++;
					if (i == ITEMS_PER_PAGE)
					{
						myPage++;
						i = 0;
					}
					continue;
				}
				
				if (shown == ITEMS_PER_PAGE)
				{
					hasMore = true;
					break;
				}
				
				switch (type.toLowerCase())
				{
					case "armor":
						itemId = sp.getChestId();
						break;
					case "weapon":
						itemId = sp.getWeaponId();
						break;
					case "hair":
						itemId = sp.getHairId();
						break;
					case "face":
						itemId = sp.getFaceId();
						break;
				}
				
				sb.append("<table border=0 cellspacing=0 cellpadding=2 height=36><tr>");
				sb.append("<td width=32 align=center>" + "<button width=32 height=32 back=" + L2Item.getItemIcon(itemId) + " fore=" + L2Item.getItemIcon(itemId) + ">" + "</td>");
				sb.append("<td width=124>" + sp.getName() + "<br1> <font color=999999>Price:</font> <font color=339966>" + L2Item.getItemNameById(sp.getPriceId()) + "</font> (<font color=LEVEL>" + sp.getPriceCount() + "</font>)</td>");
				sb.append("<td align=center width=65>" + "<button value=\"Buy\" action=\"bypass -h dressme " + page + " buyskin  " + sp.getId() + " " + type + " " + itemId + "\" width=65 height=19 back=L2UI_ch3.smallbutton2_over fore=L2UI_ch3.smallbutton2>" + "</td>");
				sb.append("<td align=center width=65>" + "<button value=\"Try\" action=\"bypass -h dressme " + page + " tryskin  " + sp.getId() + " " + type + "\" width=65 height=19 back=L2UI_ch3.smallbutton2_over fore=L2UI_ch3.smallbutton2>" + "</td>");
				
				sb.append("</tr></table>");
				sb.append("<img src=\"L2UI.Squaregray\" width=\"300\" height=\"1\">");
				shown++;
			}
		}
		
		sb.append("<table width=300><tr>");
		sb.append("<td align=center width=70>" + (page > 1 ? "<button value=\"< PREV\" action=\"bypass -h dressme " + (page - 1) + " skinlist " + type + "\" width=65 height=19 back=L2UI_ch3.smallbutton2_over fore=L2UI_ch3.smallbutton2>" : "") + "</td>");
		sb.append("<td align=center width=140>Page: " + page + "</td>");
		sb.append("<td align=center width=70>" + (hasMore ? "<button value=\"NEXT >\" action=\"bypass -h dressme " + (page + 1) + " skinlist " + type + "\" width=65 height=19 back=L2UI_ch3.smallbutton2_over fore=L2UI_ch3.smallbutton2>" : "") + "</td>");
		sb.append("</tr></table>");
		
		html.replace("%showList%", sb.toString());
		player.sendPacket(html);
	}
	
	private static void showMySkinList(L2PcInstance player, int page)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(1);
		html.setFile("data/html/dressme/myskins.htm");
		final int ITEMS_PER_PAGE = 8;
		int itemId = 0;
		
		int myPage = 1;
		int i = 0;
		int shown = 0;
		boolean hasMore = false;
		
		final StringBuilder sb = new StringBuilder();
		
		List<SkinPackage> armors = DressMeData.getInstance().getArmorSkinOptions().values().stream().filter(s -> player.hasArmorSkin(s.getId())).collect(Collectors.toList());
		List<SkinPackage> weapons = DressMeData.getInstance().getWeaponSkinOptions().values().stream().filter(s -> player.hasWeaponSkin(s.getId())).collect(Collectors.toList());
		List<SkinPackage> hairs = DressMeData.getInstance().getHairSkinOptions().values().stream().filter(s -> player.hasHairSkin(s.getId())).collect(Collectors.toList());
		List<SkinPackage> faces = DressMeData.getInstance().getFaceSkinOptions().values().stream().filter(s -> player.hasFaceSkin(s.getId())).collect(Collectors.toList());
		
		List<SkinPackage> list = Stream.concat(armors.stream(), weapons.stream()).collect(Collectors.toList());
		List<SkinPackage> list2 = Stream.concat(hairs.stream(), faces.stream()).collect(Collectors.toList());
		
		List<SkinPackage> allLists = Stream.concat(list.stream(), list2.stream()).collect(Collectors.toList());
		
		if (!allLists.isEmpty())
		{
			for (SkinPackage sp : allLists)
			{
				if (sp == null)
				{
					continue;
				}
				
				if (shown == ITEMS_PER_PAGE)
				{
					hasMore = true;
					break;
				}
				
				if (myPage != page)
				{
					i++;
					if (i == ITEMS_PER_PAGE)
					{
						myPage++;
						i = 0;
					}
					continue;
				}
				
				if (shown == ITEMS_PER_PAGE)
				{
					hasMore = true;
					break;
				}
				
				switch (sp.getType().toLowerCase())
				{
					case "armor":
						itemId = sp.getChestId();
						break;
					case "weapon":
						itemId = sp.getWeaponId();
						break;
					case "hair":
						itemId = sp.getHairId();
						break;
					case "face":
						itemId = sp.getFaceId();
						break;
				}
				
				sb.append("<table border=0 cellspacing=0 cellpadding=2 height=36><tr>");
				sb.append("<td width=32 align=center>" + "<button width=32 height=32 back=" + L2Item.getItemIcon(itemId) + " fore=" + L2Item.getItemIcon(itemId) + ">" + "</td>");
				sb.append("<td width=124>" + sp.getName() + "</td>");
				sb.append("<td align=center width=65>" + "<button value=\"Equip\" action=\"bypass -h dressme " + page + " setskin " + sp.getId() + " " + sp.getType() + " " + itemId + "\" width=65 height=19 back=L2UI_ch3.smallbutton2_over fore=L2UI_ch3.smallbutton2>" + "</td>");
				sb.append("<td align=center width=65>" + "<button value=\"Remove\" action=\"bypass -h dressme " + page + " clean " + sp.getType() + "\" width=65 height=19 back=L2UI_ch3.smallbutton2_over fore=L2UI_ch3.smallbutton2>" + "</td>");
				sb.append("</tr></table>");
				sb.append("<img src=\"L2UI.Squaregray\" width=\"300\" height=\"1\">");
				shown++;
			}
		}
		
		sb.append("<table width=300><tr>");
		sb.append("<td align=center width=70>" + (page > 1 ? "<button value=\"< PREV\" action=\"bypass -h dressme " + (page - 1) + " myskinlist\" width=65 height=19 back=L2UI_ch3.smallbutton2_over fore=L2UI_ch3.smallbutton2>" : "") + "</td>");
		sb.append("<td align=center width=140>Page: " + page + "</td>");
		sb.append("<td align=center width=70>" + (hasMore ? "<button value=\"NEXT >\" action=\"bypass -h dressme " + (page + 1) + " myskinlist\" width=65 height=19 back=L2UI_ch3.smallbutton2_over fore=L2UI_ch3.smallbutton2>" : "") + "</td>");
		sb.append("</tr></table>");
		
		html.replace("%showList%", sb.toString());
		player.sendPacket(html);
	}
	
	public boolean checkWeapons(L2PcInstance player, L2ItemInstance skin, L2WeaponType weapon1, L2WeaponType weapon2)
	{
		if (player.getActiveWeaponItem().getItemType() == weapon1 && skin.getItem().getItemType() != weapon2)
		{
			return false;
		}
		
		return true;
	}
	
	private int calculateLevelModifierForDrop(L2PcInstance lastAttacker, L2Character target)
	{
		if (Config.DEEPBLUE_DROP_RULES)
		{
			int highestLevel = lastAttacker.getLevel();
			
			// Check to prevent very high level player to nearly kill mob and let low level player do the last hit.
			if (target.getAttackByList() != null && !target.getAttackByList().isEmpty())
			{
				for (L2Character atkChar : target.getAttackByList())
				{
					if (atkChar != null && atkChar.getLevel() > highestLevel)
					{
						highestLevel = atkChar.getLevel();
					}
				}
			}
			
			if (highestLevel - 9 >= target.getLevel())
			{
				return ((highestLevel - (target.getLevel())) * 20); // Option before: ((highestLevel - (getLevel() + 8)) * 9);
			}
			
			if (highestLevel - 6 >= target.getLevel())
			{
				return ((highestLevel - (target.getLevel() + 4)) * 6);
			}
		}
		
		return 0;
	}
	
	@Override
	public String getType()
	{
		return "[C] 21 RequestBypassToServer";
	}
}
