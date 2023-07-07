package l2jorion.game.powerpack.gatekeeper;

import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;

import javolution.text.TextBuilder;
import l2jorion.Config;
import l2jorion.game.ai.CtrlIntention;
import l2jorion.game.cache.HtmCache;
import l2jorion.game.community.manager.BaseBBSManager;
import l2jorion.game.community.manager.TopBBSManager;
import l2jorion.game.controllers.GameTimeController;
import l2jorion.game.datatables.sql.TeleportLocationTable;
import l2jorion.game.handler.ICommunityBoardHandler;
import l2jorion.game.handler.ICustomByPassHandler;
import l2jorion.game.handler.IVoicedCommandHandler;
import l2jorion.game.managers.CursedWeaponsManager;
import l2jorion.game.model.CursedWeapon;
import l2jorion.game.model.L2Party;
import l2jorion.game.model.L2TeleportLocation;
import l2jorion.game.model.actor.instance.L2NpcInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.actor.instance.L2TeleporterInstance;
import l2jorion.game.model.base.ClassId;
import l2jorion.game.model.entity.event.CTF;
import l2jorion.game.model.entity.event.DM;
import l2jorion.game.model.entity.event.TvT;
import l2jorion.game.model.zone.ZoneId;
import l2jorion.game.network.serverpackets.ExShowScreenMessage;
import l2jorion.game.network.serverpackets.MagicSkillUser;
import l2jorion.game.network.serverpackets.NpcHtmlMessage;
import l2jorion.game.network.serverpackets.PlaySound;
import l2jorion.game.network.serverpackets.SetupGauge;
import l2jorion.game.powerpack.PowerPackConfig;
import l2jorion.game.taskmanager.AttackStanceTaskManager;
import l2jorion.game.taskmanager.RandomZoneTaskManager;
import l2jorion.game.templates.L2Item;
import l2jorion.game.thread.ThreadPoolManager;

public class Gatekeeper implements IVoicedCommandHandler, ICustomByPassHandler, ICommunityBoardHandler
{
	private static final CursedWeaponsManager cursedWeaponsManager = CursedWeaponsManager.getInstance();
	private Map<Integer, String> _visitedPages = new ConcurrentHashMap<>();
	
	private class EscapeFinalizer implements Runnable
	{
		L2PcInstance _player;
		L2TeleportLocation _tp;
		
		public EscapeFinalizer(L2PcInstance player, L2TeleportLocation loc)
		{
			_player = player;
			_tp = loc;
		}
		
		@Override
		public void run()
		{
			_player.enableAllSkills();
			_player.teleToLocation(_tp.getLocX(), _tp.getLocY(), _tp.getLocZ(), true);
		}
		
	}
	
	@Override
	public String[] getVoicedCommandList()
	{
		return new String[]
		{
			PowerPackConfig.GLOBALGK_COMMAND
		};
	}
	
	private boolean checkAllowed(L2PcInstance activeChar)
	{
		if (activeChar.isGM())
		{
			return true;
		}
		
		String msg = null;
		
		if (Config.L2LIMIT_CUSTOM)
		{
			if (activeChar.getPremiumService() == 0 && !activeChar.isInsideZone(ZoneId.ZONE_PEACE))
			{
				msg = "You can't use Gatekeeper outside town.";
				activeChar.sendMessage(msg);
				activeChar.sendPacket(new ExShowScreenMessage(msg, 2000, 2, false));
				activeChar.sendPacket(new PlaySound("ItemSound3.sys_impossible"));
				return false;
			}
		}
		
		if (PowerPackConfig.GLOBALGK_EXCLUDE_ON.contains("PREMIUM") && activeChar.getPremiumService() == 0)
		{
			msg = "This feature is only available for The Premium Account.";
		}
		else if (activeChar.isSitting())
		{
			msg = "Can't use Gatekeeper when sitting.";
		}
		else if (PowerPackConfig.GLOBALGK_EXCLUDE_ON.contains("ALL"))
		{
			msg = "Gatekeeper is not available in this area.";
		}
		else if (PowerPackConfig.GLOBALGK_EXCLUDE_ON.contains("JAIL") && activeChar.isInJail())
		{
			msg = "Can't use Gatekeeper in the Jail";
		}
		else if (PowerPackConfig.GLOBALGK_EXCLUDE_ON.contains("CURSED") && activeChar.isCursedWeaponEquiped())
		{
			msg = "Can't use Gatekeeper with Cursed Weapon.";
		}
		else if (PowerPackConfig.GLOBALGK_EXCLUDE_ON.contains("ATTACK") && AttackStanceTaskManager.getInstance().getAttackStanceTask(activeChar))
		{
			msg = "Gatekeeper is not available during the battle.";
		}
		else if (PowerPackConfig.GLOBALGK_EXCLUDE_ON.contains("DUNGEON") && activeChar.isIn7sDungeon())
		{
			msg = "Gatekeeper is not available in the catacomb and necropolis.";
		}
		else if (PowerPackConfig.GLOBALGK_EXCLUDE_ON.contains("RB") && activeChar.isInsideZone(ZoneId.ZONE_BOSS))
		{
			msg = "Gatekeeper is not available in this area.";
		}
		else if (PowerPackConfig.GLOBALGK_EXCLUDE_ON.contains("PVP") && activeChar.isInsideZone(ZoneId.ZONE_PVP))
		{
			msg = "Gatekeeper is not available in this area.";
		}
		else if (PowerPackConfig.GLOBALGK_EXCLUDE_ON.contains("PEACE") && activeChar.isInsideZone(ZoneId.ZONE_PEACE))
		{
			msg = "Gatekeeper is not available in this area.";
		}
		else if (PowerPackConfig.GLOBALGK_EXCLUDE_ON.contains("SIEGE") && activeChar.isInsideZone(ZoneId.ZONE_SIEGE))
		{
			msg = "Gatekeeper is not available in this area.";
		}
		else if (PowerPackConfig.GLOBALGK_EXCLUDE_ON.contains("OLYMPIAD") && activeChar.isInOlympiadMode())
		{
			msg = "Gatekeeper is not available in Olympiad.";
		}
		else if (PowerPackConfig.GLOBALGK_EXCLUDE_ON.contains("TVT") && activeChar._inEventTvT && TvT.is_started())
		{
			msg = "Gatekeeper is not available in TVT.";
		}
		else if (PowerPackConfig.GLOBALGK_EXCLUDE_ON.contains("CTF") && activeChar._inEventCTF && CTF.is_started())
		{
			msg = "Gatekeeper is not available in CTF.";
		}
		else if (PowerPackConfig.GLOBALGK_EXCLUDE_ON.contains("DM") && activeChar._inEventDM && DM.is_started())
		{
			msg = "Gatekeeper is not available in DM.";
		}
		
		if (msg != null)
		{
			activeChar.sendMessage(msg);
			activeChar.sendPacket(new ExShowScreenMessage(msg, 2000, 2, false));
			activeChar.sendPacket(new PlaySound("ItemSound3.sys_impossible"));
		}
		
		return msg == null;
	}
	
	@Override
	public boolean useVoicedCommand(String command, L2PcInstance player, String params)
	{
		if (player == null)
		{
			return false;
		}
		
		if (player.isInStoreMode())
		{
			return false;
		}
		
		if (!checkAllowed(player))
		{
			return false;
		}
		
		if (command.startsWith(PowerPackConfig.GLOBALGK_COMMAND))
		{
			String index = "";
			if (params != null && params.length() != 0)
			{
				if (!params.equals("0"))
				{
					index = "-" + params;
				}
			}
			
			NpcHtmlMessage htm = new NpcHtmlMessage(player.getLastQuestNpcObject());
			String text = HtmCache.getInstance().getHtm("data/html/gatekeeper/gk" + index + ".htm");
			htm.setHtml(text);
			player.sendPacket(htm);
		}
		
		return false;
	}
	
	@Override
	public String[] getByPassCommands()
	{
		return new String[]
		{
			"dotele"
		};
	}
	
	@Override
	public void handleCommand(String command, L2PcInstance player, String parameters)
	{
		final StringTokenizer st = new StringTokenizer(parameters);
		st.nextToken();
		
		if (player == null)
		{
			return;
		}
		
		if (!checkAllowed(player))
		{
			return;
		}
		
		if (!player.isGM())
		{
			if (!PowerPackConfig.GLOBALGK_USEBBS && !PowerPackConfig.GLOBALGK_USECOMMAND)
			{
				L2NpcInstance gkNpc = null;
				
				if (player.getTarget() != null)
				{
					if (player.getTarget() instanceof L2NpcInstance)
					{
						gkNpc = (L2NpcInstance) player.getTarget();
						if (gkNpc.getTemplate().getNpcId() != PowerPackConfig.GLOBALGK_NPC_ID)
						{
							gkNpc = null;
						}
					}
					
					if (Config.RON_CUSTOM)
					{
						if (player.getTarget() instanceof L2TeleporterInstance)
						{
							gkNpc = (L2TeleporterInstance) player.getTarget();
						}
					}
				}
				
				if (gkNpc == null)
				{
					return;
				}
				
				if (!player.isInsideRadius(gkNpc, L2NpcInstance.INTERACTION_DISTANCE, false, false))
				{
					return;
				}
			}
		}
		
		int unstuckTimer = PowerPackConfig.GLOBALGK_TIMEOUT * 1000;
		
		if (player.isGM())
		{
			unstuckTimer = 0;
		}
		String htm = "gk";
		
		if (parameters.startsWith("goto"))
		{
			int locId = Integer.parseInt(parameters.substring(parameters.indexOf(" ") + 1).trim());
			L2TeleportLocation tpPoint = TeleportLocationTable.getInstance().getTemplate(locId);
			
			if (tpPoint != null)
			{
				if (PowerPackConfig.GLOBALGK_PRICE > 0 && player.getLevel() > Config.FREE_TELEPORT_UNTIL + 1)
				{
					if (player.getInventory().getAdena() < PowerPackConfig.GLOBALGK_PRICE)
					{
						player.sendMessage("You don't have enough Adena.");
						player.sendPacket(new ExShowScreenMessage("You don't have enough Adena.", 2000, 2, false));
						player.sendPacket(new PlaySound("ItemSound3.sys_impossible"));
						return;
					}
					player.reduceAdena("teleport", PowerPackConfig.GLOBALGK_PRICE, null, true);
				}
				
				if (PowerPackConfig.GLOBALGK_PRICE == -1 && player.getLevel() > Config.FREE_TELEPORT_UNTIL + 1)
				{
					if (player.getInventory().getAdena() < tpPoint.getPrice())
					{
						player.sendMessage("You don't have enough Adena.");
						player.sendPacket(new ExShowScreenMessage("You don't have enough Adena.", 2000, 2, false));
						player.sendPacket(new PlaySound("ItemSound3.sys_impossible"));
						return;
					}
					player.reduceAdena("teleport", tpPoint.getPrice(), null, true);
				}
				
				if (Config.L2LIMIT_CUSTOM)
				{
					if (player.isInParty() && player.getParty().isLeader(player))
					{
						L2Party party = player.getParty();
						int unstuckTimer2 = 10000;
						
						if (player.getPremiumService() == 0 && !player.isInsideZone(ZoneId.ZONE_PEACE))
						{
							player.teleToLocation(tpPoint.getLocX(), tpPoint.getLocY(), tpPoint.getLocZ(), true);
							return;
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
								member.broadcastPacket(new MagicSkillUser(member, 1050, 1, unstuckTimer2, 0));
								member.sendPacket(new SetupGauge(0, unstuckTimer2));
								member.setTarget(null);
								
								member.setSkillCast(ThreadPoolManager.getInstance().scheduleEffect(new EscapeFinalizer(member, tpPoint), unstuckTimer2));
								member.setSkillCastEndTime(10 + GameTimeController.getInstance().getGameTicks() + unstuckTimer2 / GameTimeController.MILLIS_IN_TICK);
							}
							else
							{
								member.teleToLocation(tpPoint.getLocX(), tpPoint.getLocY(), tpPoint.getLocZ(), true);
							}
							
							if (party.isLeader(member))
							{
								player.sendMessage("Your party members are teleporting follow you.");
							}
							else
							{
								member.sendMessage("Your party leader is teleporting you.");
							}
						}
						
						return;
					}
				}
				
				if (unstuckTimer > 0 && !(player.isInsideZone(ZoneId.ZONE_PEACE)))
				{
					player.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
					
					player.setTarget(player);
					player.broadcastPacket(new MagicSkillUser(player, 1050, 1, unstuckTimer, 0));
					player.sendPacket(new SetupGauge(0, unstuckTimer));
					player.setTarget(null);
					
					player.setSkillCast(ThreadPoolManager.getInstance().scheduleEffect(new EscapeFinalizer(player, tpPoint), unstuckTimer));
					player.setSkillCastEndTime(10 + GameTimeController.getInstance().getGameTicks() + unstuckTimer / GameTimeController.MILLIS_IN_TICK);
					
					return;
				}
				
				player.teleToLocation(tpPoint.getLocX(), tpPoint.getLocY(), tpPoint.getLocZ(), true);
				return;
			}
		}
		else if (parameters.startsWith("premium_goto"))
		{
			int locId = Integer.parseInt(parameters.substring(parameters.indexOf(" ") + 1).trim());
			L2TeleportLocation tpPoint = TeleportLocationTable.getInstance().getTemplate(locId);
			
			if (tpPoint != null)
			{
				if (PowerPackConfig.GLOBALGK_PRICE > 0 && player.getLevel() > Config.FREE_TELEPORT_UNTIL + 1)
				{
					if (player.getInventory().getAdena() < PowerPackConfig.GLOBALGK_PRICE)
					{
						player.sendMessage("You don't have enough Adena.");
						player.sendPacket(new ExShowScreenMessage("You don't have enough Adena.", 2000, 2, false));
						player.sendPacket(new PlaySound("ItemSound3.sys_impossible"));
						return;
					}
					player.reduceAdena("teleport", PowerPackConfig.GLOBALGK_PRICE, null, true);
				}
				
				if (PowerPackConfig.GLOBALGK_PRICE == -1 && player.getLevel() > Config.FREE_TELEPORT_UNTIL + 1)
				{
					if (player.getInventory().getAdena() < tpPoint.getPrice())
					{
						player.sendMessage("You don't have enough Adena.");
						player.sendPacket(new ExShowScreenMessage("You don't have enough Adena.", 2000, 2, false));
						player.sendPacket(new PlaySound("ItemSound3.sys_impossible"));
						return;
					}
					player.reduceAdena("teleport", tpPoint.getPrice(), null, true);
				}
				
				if (player.getPremiumService() == 0)
				{
					player.sendMessage("You're not The Premium account.");
					return;
				}
				
				if (Config.L2LIMIT_CUSTOM)
				{
					if (player.isInParty() && player.getParty().isLeader(player))
					{
						L2Party party = player.getParty();
						int unstuckTimer2 = 10000;
						
						if (player.getPremiumService() == 0 && !player.isInsideZone(ZoneId.ZONE_PEACE))
						{
							player.teleToLocation(tpPoint.getLocX(), tpPoint.getLocY(), tpPoint.getLocZ(), true);
							return;
						}
						
						for (L2PcInstance member : party.getPartyMembers())
						{
							if (member == null)
							{
								continue;
							}
							
							if (player.getPremiumService() == 1 && member.getPremiumService() == 0)
							{
								continue;
							}
							
							if (!(member.isInsideZone(ZoneId.ZONE_PEACE)))
							{
								member.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
								
								member.setTarget(member);
								member.broadcastPacket(new MagicSkillUser(member, 1050, 1, unstuckTimer2, 0));
								member.sendPacket(new SetupGauge(0, unstuckTimer2));
								member.setTarget(null);
								
								member.setSkillCast(ThreadPoolManager.getInstance().scheduleEffect(new EscapeFinalizer(member, tpPoint), unstuckTimer2));
								member.setSkillCastEndTime(10 + GameTimeController.getInstance().getGameTicks() + unstuckTimer2 / GameTimeController.MILLIS_IN_TICK);
							}
							else
							{
								member.teleToLocation(tpPoint.getLocX(), tpPoint.getLocY(), tpPoint.getLocZ(), true);
							}
							
							if (party.isLeader(member))
							{
								player.sendMessage("Your party members are teleporting follow you.");
							}
							else
							{
								member.sendMessage("Your party leader is teleporting you.");
							}
						}
						
						return;
					}
				}
				
				if (unstuckTimer > 0 && !(player.isInsideZone(ZoneId.ZONE_PEACE)))
				{
					player.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
					
					player.setTarget(player);
					player.broadcastPacket(new MagicSkillUser(player, 1050, 1, unstuckTimer, 0));
					player.sendPacket(new SetupGauge(0, unstuckTimer));
					player.setTarget(null);
					
					player.setSkillCast(ThreadPoolManager.getInstance().scheduleEffect(new EscapeFinalizer(player, tpPoint), unstuckTimer));
					player.setSkillCastEndTime(10 + GameTimeController.getInstance().getGameTicks() + unstuckTimer / GameTimeController.MILLIS_IN_TICK);
					return;
				}
				
				player.teleToLocation(tpPoint.getLocX(), tpPoint.getLocY(), tpPoint.getLocZ(), true);
				return;
			}
		}
		else if (parameters.startsWith("custom_goto"))
		{
			int locId = Integer.parseInt(parameters.substring(parameters.indexOf(" ") + 1).trim());
			L2TeleportLocation tpPoint = TeleportLocationTable.getInstance().getTemplate(locId);
			
			if (tpPoint != null)
			{
				if (player.getInventory().getItemByItemId(PowerPackConfig.GLOBALGK_CUSTOM_ITEM_PRICE) == null || player.getInventory().getItemByItemId(PowerPackConfig.GLOBALGK_CUSTOM_ITEM_PRICE).getCount() < tpPoint.getPrice())
				{
					player.sendMessage("You do not have enough " + L2Item.getItemNameById(PowerPackConfig.GLOBALGK_CUSTOM_ITEM_PRICE) + ".");
					player.sendPacket(new ExShowScreenMessage("You do not have enough " + L2Item.getItemNameById(PowerPackConfig.GLOBALGK_CUSTOM_ITEM_PRICE) + ".", 2000, 2, false));
					player.sendPacket(new PlaySound("ItemSound3.sys_impossible"));
					return;
				}
				
				player.destroyItem("Consume", player.getInventory().getItemByItemId(PowerPackConfig.GLOBALGK_CUSTOM_ITEM_PRICE).getObjectId(), tpPoint.getPrice(), null, true);
				
				if (Config.L2LIMIT_CUSTOM)
				{
					if (player.isInParty() && player.getParty().isLeader(player))
					{
						L2Party party = player.getParty();
						int unstuckTimer2 = 10000;
						
						if (player.getPremiumService() == 0 && !player.isInsideZone(ZoneId.ZONE_PEACE))
						{
							player.teleToLocation(tpPoint.getLocX(), tpPoint.getLocY(), tpPoint.getLocZ(), true);
							return;
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
								member.broadcastPacket(new MagicSkillUser(member, 1050, 1, unstuckTimer2, 0));
								member.sendPacket(new SetupGauge(0, unstuckTimer2));
								member.setTarget(null);
								
								member.setSkillCast(ThreadPoolManager.getInstance().scheduleEffect(new EscapeFinalizer(member, tpPoint), unstuckTimer2));
								member.setSkillCastEndTime(10 + GameTimeController.getInstance().getGameTicks() + unstuckTimer2 / GameTimeController.MILLIS_IN_TICK);
							}
							else
							{
								member.teleToLocation(tpPoint.getLocX(), tpPoint.getLocY(), tpPoint.getLocZ(), true);
							}
							
							if (party.isLeader(member))
							{
								player.sendMessage("Your party members are teleporting follow you.");
							}
							else
							{
								member.sendMessage("Your party leader is teleporting you.");
							}
						}
						
						return;
					}
				}
				
				if (unstuckTimer > 0 && !(player.isInsideZone(ZoneId.ZONE_PEACE)))
				{
					player.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
					
					player.setTarget(player);
					player.broadcastPacket(new MagicSkillUser(player, 1050, 1, unstuckTimer, 0));
					player.sendPacket(new SetupGauge(0, unstuckTimer));
					player.setTarget(null);
					
					player.setSkillCast(ThreadPoolManager.getInstance().scheduleEffect(new EscapeFinalizer(player, tpPoint), unstuckTimer));
					player.setSkillCastEndTime(10 + GameTimeController.getInstance().getGameTicks() + unstuckTimer / GameTimeController.MILLIS_IN_TICK);
					
					return;
				}
				
				player.teleToLocation(tpPoint.getLocX(), tpPoint.getLocY(), tpPoint.getLocZ(), true);
				return;
			}
		}
		else if (parameters.startsWith("cwpopupmenu"))
		{
			if (player.isCwPopUpMenuOn())
			{
				player.setCwPopUpMenuOn(false);
			}
			else
			{
				player.setCwPopUpMenuOn(true);
			}
			showCWinfo(player);
			return;
		}
		else if (parameters.startsWith("cwgoto"))
		{
			try
			{
				if (!st.hasMoreElements())
				{
					player.sendMessage("Not enough parameters.");
					return;
				}
				
				String parameter = st.nextToken();
				int id = 0;
				if (parameter.matches("[0-9]*"))
				{
					id = Integer.parseInt(parameter);
				}
				else
				{
					parameter = parameter.replace('_', ' ');
					for (final CursedWeapon cwp : cursedWeaponsManager.getCursedWeapons())
					{
						if (cwp.getName().toLowerCase().contains(parameter.toLowerCase()))
						{
							id = cwp.getItemId();
							break;
						}
					}
				}
				
				if (cursedWeaponsManager.isCursed(id))
				{
					if (player.getInventory().getItemByItemId(PowerPackConfig.GLOBALGK_CW_ITEM_ID) == null || player.getInventory().getItemByItemId(PowerPackConfig.GLOBALGK_CW_ITEM_ID).getCount() < PowerPackConfig.GLOBALGK_CW_ITEM_PRICE)
					{
						player.sendMessage("You do not have enough " + L2Item.getItemIcon(PowerPackConfig.GLOBALGK_CW_ITEM_ID) + " to pay for service.");
						return;
					}
					player.destroyItem("Consume", player.getInventory().getItemByItemId(PowerPackConfig.GLOBALGK_CW_ITEM_ID).getObjectId(), PowerPackConfig.GLOBALGK_CW_ITEM_PRICE, null, true);
					cursedWeaponsManager.getCursedWeapon(id).goTo(player);
				}
				else
				{
					player.sendMessage("Wrong Cursed Weapon Id.");
				}
			}
			catch (Exception e)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
				{
					e.printStackTrace();
				}
			}
		}
		else if (parameters.startsWith("Noble"))
		{
			if (!player.isNoble())
			{
				player.sendMessage("This menu only for noble.");
				return;
			}
			htm = htm + "-" + parameters.substring(parameters.indexOf(" ") + 1).trim();
		}
		else if (parameters.startsWith("pvp"))
		{
			if (!Config.ALLOW_RANDOM_PVP_ZONE)
			{
				player.sendMessage("This feature is not available now.");
				return;
			}
			
			if (Config.PROHIBIT_HEALER_CLASS && (player.getClassId() == ClassId.cardinal || player.getClassId() == ClassId.evaSaint || player.getClassId() == ClassId.shillienSaint))
			{
				player.sendMessage("You can't enter to zone with Healer Class!");
				player.sendPacket(new ExShowScreenMessage("You can't enter to zone with Healer Class!", 3000, 0x02, false));
				return;
			}
			player.teleToLocation(RandomZoneTaskManager.getInstance().getCurrentZone().getLoc(), 50, true);
			
		}
		else if (parameters.startsWith("cursedw"))
		{
			showCWinfo(player);
			return;
		}
		else if (parameters.startsWith("Chat"))
		{
			htm = htm + "-" + parameters.substring(parameters.indexOf(" ") + 1).trim();
		}
		
		if (htm.contains("-0"))
		{
			htm = "gk";
		}
		
		synchronized (_visitedPages)
		{
			_visitedPages.put(player.getObjectId(), htm);
		}
		
		NpcHtmlMessage html = new NpcHtmlMessage(1);
		String text = HtmCache.getInstance().getHtm("data/html/gatekeeper/" + htm + ".htm");
		
		if (command.startsWith("bbstele"))
		{
			if (Config.L2LIMIT_CUSTOM)
			{
				if (player.getPremiumService() == 0)
				{
					player.sendMessage("Only for the premium account.");
					player.sendPacket(new ExShowScreenMessage("Only for the premium account.", 2000, 2, false));
					player.sendPacket(new PlaySound("ItemSound3.sys_impossible"));
					TopBBSManager.getInstance().parseCmd("_bbshome", player);
					return;
				}
			}
			
			text = text.replaceAll("custom_dotele", "bbs_bbstele");
			text = text.replaceAll("<title>", "<br><center>");
			text = text.replaceAll("</title>", "</center>");
			BaseBBSManager.separateAndSend(text, player, (Config.LIFEDRAIN_CUSTOM ? true : false));
			return;
		}
		
		html.setHtml(text);
		player.sendPacket(html);
	}
	
	public static void showCWinfo(L2PcInstance player)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(1);
		String text = HtmCache.getInstance().getHtm("data/html/gatekeeper/cwinfo.htm");
		html.setHtml(text);
		
		final TextBuilder replyMSG = new TextBuilder();
		for (final CursedWeapon cw : cursedWeaponsManager.getCursedWeapons())
		{
			final int itemId = cw.getItemId();
			replyMSG.append("<table width=270><tr><td>Name:</td><td>" + cw.getName() + "</td></tr>");
			
			if (cw.isActivated())
			{
				final L2PcInstance pl = cw.getPlayer();
				replyMSG.append("<tr><td>Weilder:</td><td>" + (pl == null ? "null" : pl.getName()) + "</td></tr>");
				replyMSG.append("<tr><td>Karma:</td><td>" + String.valueOf(cw.getPlayerKarma()) + "</td></tr>");
				replyMSG.append("<tr><td>Kills:</td><td>" + String.valueOf(cw.getPlayerPkKills()) + "/" + String.valueOf(cw.getNbKills()) + "</td></tr>");
				replyMSG.append("<tr><td>Time remaining:</td><td>" + String.valueOf(cw.getTimeLeft() / 60000) + " min.</td></tr>");
				replyMSG.append("<tr><td><button value=\"Go to\" action=\"bypass -h custom_dotele cwgoto " + String.valueOf(itemId) + "\" width=73 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
				replyMSG.append("<td></td></tr>");
			}
			else if (cw.isDropped())
			{
				replyMSG.append("<tr><td>Position:</td><td>Lying on the ground</td></tr>");
				replyMSG.append("<tr><td>Time remaining:</td><td>" + String.valueOf(cw.getTimeLeft() / 60000) + " min.</td></tr>");
				replyMSG.append("<tr><td>Kills:</td><td>" + String.valueOf(cw.getNbKills()) + "</td></tr>");
				replyMSG.append("<tr><td><button value=\"Go to\" action=\"bypass -h custom_dotele cwgoto " + String.valueOf(itemId) + "\" width=73 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
				replyMSG.append("<td></td></tr>");
			}
			else
			{
				replyMSG.append("<tr><td>Position:</td><td>Doesn't exist.</td></tr>");
			}
			
			replyMSG.append("</table><br>");
		}
		
		if (player.isCwPopUpMenuOn())
		{
			html.replace("%popupmenu%", "<button value=\"\" action=\"bypass -h custom_dotele cwpopupmenu\" width=15 height=15 back=\"L2UI.CheckB﻿ox_checked\" fore=\"L2UI.CheckBox_checked\">");
		}
		else
		{
			html.replace("%popupmenu%", "<button value=\"\" action=\"bypass -h custom_dotele cwpopupmenu\" width=15 height=15 back=\"L2UI.CheckBox\" fore=\"L2UI.CheckBox\">");
		}
		
		html.replace("%cwinfo%", replyMSG.toString());
		player.sendPacket(html);
	}
	
	@Override
	public String[] getBypassBbsCommands()
	{
		return new String[]
		{
			"bbstele"
		};
	}
	
	public void returnHtm(String command, String param, L2PcInstance player)
	{
		if (_visitedPages.get(player.getObjectId()) != null)
		{
			handleCommand(command, player, param + " " + _visitedPages.get(player.getObjectId()));
		}
	}
}
