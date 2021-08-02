package l2jorion.game.powerpack.gatekeeper;

import java.util.StringTokenizer;

import javolution.text.TextBuilder;
import l2jorion.Config;
import l2jorion.game.cache.HtmCache;
import l2jorion.game.community.manager.BaseBBSManager;
import l2jorion.game.controllers.GameTimeController;
import l2jorion.game.datatables.sql.ItemTable;
import l2jorion.game.datatables.sql.TeleportLocationTable;
import l2jorion.game.handler.IBBSHandler;
import l2jorion.game.handler.ICustomByPassHandler;
import l2jorion.game.handler.IVoicedCommandHandler;
import l2jorion.game.managers.CursedWeaponsManager;
import l2jorion.game.model.CursedWeapon;
import l2jorion.game.model.L2TeleportLocation;
import l2jorion.game.model.actor.instance.L2NpcInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
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

public class GKHandler implements IVoicedCommandHandler, ICustomByPassHandler, IBBSHandler
{
	private static final CursedWeaponsManager cursedWeaponsManager = CursedWeaponsManager.getInstance();
	
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
		if (activeChar.isSitting())
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
		}
		
		return msg == null;
	}
	
	@Override
	public boolean useVoicedCommand(String cmd, L2PcInstance player, String params)
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
		
		if (cmd.compareTo(PowerPackConfig.GLOBALGK_COMMAND) == 0)
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
		
		if (!PowerPackConfig.GLOBALGK_USEBBS && !PowerPackConfig.GLOBALGK_USECOMMAND)
		{
			L2NpcInstance gknpc = null;
			
			if (player.getTarget() != null)
			{
				if (player.getTarget() instanceof L2NpcInstance)
				{
					gknpc = (L2NpcInstance) player.getTarget();
					if (gknpc.getTemplate().getNpcId() != PowerPackConfig.GLOBALGK_NPC)
					{
						gknpc = null;
					}
				}
			}
			
			// Possible fix to Buffer - 1
			if (gknpc == null)
			{
				return;
			}
			
			// Possible fix to Buffer - 2
			if (!player.isInsideRadius(gknpc, L2NpcInstance.INTERACTION_DISTANCE, false, false))
			{
				return;
			}
		}
		
		int unstuckTimer = PowerPackConfig.GLOBALGK_TIMEOUT * 1000;
		String htm = "gk";
		
		if (parameters.startsWith("goto"))
		{
			try
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
							player.sendPacket(new ExShowScreenMessage("You don't have enough Adena.", 1000, 2, false));
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
							player.sendPacket(new ExShowScreenMessage("You don't have enough Adena.", 1000, 2, false));
							player.sendPacket(new PlaySound("ItemSound3.sys_impossible"));
							return;
						}
						player.reduceAdena("teleport", tpPoint.getPrice(), null, true);
					}
					
					player.setTarget(player);
					player.disableAllSkills();
					
					if (unstuckTimer > 0 && !(player.isInsideZone(ZoneId.ZONE_PEACE)))
					{
						final MagicSkillUser msu = new MagicSkillUser(player, 1050, 1, unstuckTimer, 0);
						player.broadcastPacket(msu);
						player.setTarget(player);
						SetupGauge sg = new SetupGauge(0, unstuckTimer);
						player.sendPacket(sg);
						
						// End SoE Animation section
						player.setTarget(null);
						
						EscapeFinalizer ef = new EscapeFinalizer(player, tpPoint);
						// continue execution later
						player.setSkillCast(ThreadPoolManager.getInstance().scheduleEffect(ef, unstuckTimer));
						player.setSkillCastEndTime(10 + GameTimeController.getInstance().getGameTicks() + unstuckTimer / GameTimeController.MILLIS_IN_TICK);
						
						return;
					}
					
					player.enableAllSkills();
					player.teleToLocation(tpPoint.getLocX(), tpPoint.getLocY(), tpPoint.getLocZ(), true);
					return;
				}
				player.sendMessage("Teleport, with ID " + locId + " does not exist in the database");
			}
			catch (Exception e)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
				{
					e.printStackTrace();
				}
			}
		}
		else if (parameters.startsWith("premium_goto"))
		{
			try
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
							player.sendPacket(new ExShowScreenMessage("You don't have enough Adena.", 1000, 2, false));
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
							player.sendPacket(new ExShowScreenMessage("You don't have enough Adena.", 1000, 2, false));
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
					
					player.setTarget(player);
					player.disableAllSkills();
					
					if (unstuckTimer > 0 && !(player.isInsideZone(ZoneId.ZONE_PEACE)))
					{
						final MagicSkillUser msu = new MagicSkillUser(player, 1050, 1, unstuckTimer, 0);
						player.broadcastPacket(msu);
						player.setTarget(player);
						SetupGauge sg = new SetupGauge(0, unstuckTimer);
						player.sendPacket(sg);
						
						// End SoE Animation section
						player.setTarget(null);
						
						EscapeFinalizer ef = new EscapeFinalizer(player, tpPoint);
						// continue execution later
						player.setSkillCast(ThreadPoolManager.getInstance().scheduleEffect(ef, unstuckTimer));
						player.setSkillCastEndTime(10 + GameTimeController.getInstance().getGameTicks() + unstuckTimer / GameTimeController.MILLIS_IN_TICK);
						
						return;
					}
					
					player.enableAllSkills();
					player.teleToLocation(tpPoint.getLocX(), tpPoint.getLocY(), tpPoint.getLocZ(), true);
					return;
				}
				
				player.sendMessage("Teleport, with ID " + locId + " does not exist in the database");
			}
			catch (Exception e)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
				{
					e.printStackTrace();
				}
			}
		}
		else if (parameters.startsWith("custom_goto"))
		{
			try
			{
				int locId = Integer.parseInt(parameters.substring(parameters.indexOf(" ") + 1).trim());
				L2TeleportLocation tpPoint = TeleportLocationTable.getInstance().getTemplate(locId);
				
				if (tpPoint != null)
				{
					if (player.getInventory().getItemByItemId(PowerPackConfig.GLOBALGK_CUSTOM_ITEM_PRICE) == null || player.getInventory().getItemByItemId(PowerPackConfig.GLOBALGK_CUSTOM_ITEM_PRICE).getCount() < tpPoint.getPrice())
					{
						player.sendMessage("You do not have enough " + getItemNameById(PowerPackConfig.GLOBALGK_CUSTOM_ITEM_PRICE) + " to pay for services.");
						return;
					}
					
					player.destroyItem("Consume", player.getInventory().getItemByItemId(6393).getObjectId(), tpPoint.getPrice(), null, true);
					
					player.setTarget(player);
					player.disableAllSkills();
					
					if (unstuckTimer > 0 && !(player.isInsideZone(ZoneId.ZONE_PEACE)))
					{
						final MagicSkillUser msu = new MagicSkillUser(player, 1050, 1, unstuckTimer, 0);
						player.broadcastPacket(msu);
						player.setTarget(player);
						SetupGauge sg = new SetupGauge(0, unstuckTimer);
						player.sendPacket(sg);
						
						// End SoE Animation section
						player.setTarget(null);
						
						EscapeFinalizer ef = new EscapeFinalizer(player, tpPoint);
						// continue execution later
						player.setSkillCast(ThreadPoolManager.getInstance().scheduleEffect(ef, unstuckTimer));
						player.setSkillCastEndTime(10 + GameTimeController.getInstance().getGameTicks() + unstuckTimer / GameTimeController.MILLIS_IN_TICK);
						
						return;
					}
					
					player.enableAllSkills();
					player.teleToLocation(tpPoint.getLocX(), tpPoint.getLocY(), tpPoint.getLocZ(), true);
					return;
				}
				player.sendMessage("Teleport, with ID " + locId + " does not exist in the database");
			}
			catch (Exception e)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
				{
					e.printStackTrace();
				}
				
				player.sendMessage("Error... maybe you cheat..");
			}
		}
		else if (parameters.startsWith("cwgoto"))
		{
			try
			{
				if (!st.hasMoreElements())
				{
					player.sendMessage("Not enough parameters!");
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
					if (player.getInventory().getItemByItemId(6393) == null || player.getInventory().getItemByItemId(6393).getCount() < 2)
					{
						player.sendMessage("You do not have enough medals to pay for services");
						return;
					}
					player.destroyItem("Consume", player.getInventory().getItemByItemId(6393).getObjectId(), 2, null, true);
					cursedWeaponsManager.getCursedWeapon(id).goTo(player);
				}
				else
				{
					player.sendMessage("Wrong Cursed Weapon Id!");
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
			if (Config.PROHIBIT_HEALER_CLASS && (player.getClassId() == ClassId.cardinal || player.getClassId() == ClassId.evaSaint || player.getClassId() == ClassId.shillienSaint))
			{
				player.sendMessage("You can't enter to zone with Healer Class!");
				player.sendPacket(new ExShowScreenMessage("You can't enter to zone with Healer Class!", 3000, 0x02, false));
				return;
			}
			
			player.teleToLocation(RandomZoneTaskManager.getInstance().getCurrentZone().getLoc(), 25);
		}
		else if (parameters.startsWith("Chat"))
		{
			htm = htm + "-" + parameters.substring(parameters.indexOf(" ") + 1).trim();
		}
		else if (parameters.equalsIgnoreCase("Cwchat"))
		{
			htm = htm + "-" + parameters.substring(parameters.indexOf(" ") + 1).trim();
		}
		
		if (htm.contains("-0"))
		{
			htm = "gk";
		}
		
		NpcHtmlMessage html = new NpcHtmlMessage(player.getLastQuestNpcObject());
		String text = HtmCache.getInstance().getHtm("data/html/gatekeeper/" + htm + ".htm");
		
		if (command.startsWith("bbs"))
		{
			if (!player.isGM() && player.getPremiumService() == 0)
			{
				player.sendMessage("Only for PREMIUM account.");
				return;
			}
			
			text = text.replace("-h custom_do", "bbs_bbs");
			BaseBBSManager.separateAndSend(text, player);
		}
		else
		{
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
			html.replace("%cwinfo%", replyMSG.toString());
			player.sendPacket(html);
		}
		return;
		
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
	
	@Override
	public String[] getBBSCommands()
	{
		return new String[]
		{
			"bbstele"
		};
	}
	
}
