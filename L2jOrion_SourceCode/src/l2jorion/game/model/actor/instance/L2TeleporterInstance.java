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
package l2jorion.game.model.actor.instance;

import java.util.StringTokenizer;

import l2jorion.Config;
import l2jorion.game.datatables.sql.TeleportLocationTable;
import l2jorion.game.managers.CastleManager;
import l2jorion.game.managers.GrandBossManager;
import l2jorion.game.managers.SiegeManager;
import l2jorion.game.managers.TownManager;
import l2jorion.game.model.L2Object;
import l2jorion.game.model.L2TeleportLocation;
import l2jorion.game.model.base.ClassId;
import l2jorion.game.model.olympiad.OlympiadManager;
import l2jorion.game.model.zone.type.L2BossZone;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.ActionFailed;
import l2jorion.game.network.serverpackets.ExShowScreenMessage;
import l2jorion.game.network.serverpackets.NpcHtmlMessage;
import l2jorion.game.network.serverpackets.SystemMessage;
import l2jorion.game.powerpack.PowerPackConfig;
import l2jorion.game.taskmanager.RandomZoneTaskManager;
import l2jorion.game.templates.L2NpcTemplate;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;

public final class L2TeleporterInstance extends L2FolkInstance
{
	protected static Logger LOG = LoggerFactory.getLogger(L2TeleporterInstance.class);
	
	/** The Constant COND_ALL_FALSE. */
	private static final int COND_ALL_FALSE = 0;
	
	/** The Constant COND_BUSY_BECAUSE_OF_SIEGE. */
	private static final int COND_BUSY_BECAUSE_OF_SIEGE = 1;
	
	/** The Constant COND_OWNER. */
	private static final int COND_OWNER = 2;
	
	/** The Constant COND_REGULAR. */
	private static final int COND_REGULAR = 3;
	
	/**
	 * Instantiates a new l2 teleporter instance.
	 * @param objectId the object id
	 * @param template the template
	 */
	public L2TeleporterInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
	}
	
	@Override
	public void onBypassFeedback(L2PcInstance player, String command)
	{
		L2Object target = player.getTarget();
		player.sendPacket(ActionFailed.STATIC_PACKET);
		
		if (OlympiadManager.getInstance().isRegisteredInComp(player))
		{
			player.sendMessage("You are not allowed to use a teleport while registered in olympiad game.");
			return;
		}
		
		if (!(target instanceof L2TeleporterInstance))
		{
			return;
		}
		
		int condition = validateCondition(player);
		
		StringTokenizer st = new StringTokenizer(command, " ");
		String actualCommand = st.nextToken(); // Get actual command
		
		if (actualCommand.equalsIgnoreCase("goto"))
		{
			int npcId = getTemplate().npcId;
			
			switch (npcId)
			{
				case 31095: //
				case 31096: //
				case 31097: //
				case 31098: // Enter Necropolises
				case 31099: //
				case 31100: //
				case 31101: //
				case 31102: //
				
				case 31114: //
				case 31115: //
				case 31116: // Enter Catacombs
				case 31117: //
				case 31118: //
				case 31119: //
					player.setIsIn7sDungeon(true);
					break;
				case 31103: //
				case 31104: //
				case 31105: //
				case 31106: // Exit Necropolises
				case 31107: //
				case 31108: //
				case 31109: //
				case 31110: //
				
				case 31120: //
				case 31121: //
				case 31122: // Exit Catacombs
				case 31123: //
				case 31124: //
				case 31125: //
					player.setIsIn7sDungeon(false);
					break;
			}
			
			if (st.countTokens() <= 0)
			{
				return;
			}
			
			int whereTo = Integer.parseInt(st.nextToken());
			if (condition == COND_REGULAR)
			{
				doTeleport(player, whereTo);
				return;
			}
			else if (condition == COND_OWNER)
			{
				int minPrivilegeLevel = 0; // NOTE: Replace 0 with highest level when privilege level is implemented
				if (st.countTokens() >= 1)
				{
					minPrivilegeLevel = Integer.parseInt(st.nextToken());
				}
				
				if (10 >= minPrivilegeLevel)
				{
					doTeleport(player, whereTo);
				}
				else
				{
					player.sendMessage("You don't have the sufficient access level to teleport there.");
				}
				
				return;
			}
		}
		else if (actualCommand.equalsIgnoreCase("pvp"))
		{
			if (Config.PROHIBIT_HEALER_CLASS && (player.getClassId() == ClassId.cardinal || player.getClassId() == ClassId.evaSaint || player.getClassId() == ClassId.shillienSaint))
			{
				player.sendMessage("You can't enter to zone with Healer Class!");
				player.sendPacket(new ExShowScreenMessage("You can't enter to zone with Healer Class!", 3000, 0x02, false));
				return;
			}
			
			player.teleToLocation(RandomZoneTaskManager.getInstance().getCurrentZone().getLoc(), 25);
		}
		super.onBypassFeedback(player, command);
	}
	
	@Override
	public String getHtmlPath(L2PcInstance player, int npcId, int val)
	{
		String pom = "";
		
		if (npcId == PowerPackConfig.GLOBALGK_NPC)
		{
			if (val == 0)
			{
				pom = "gk";
			}
			else
			{
				pom = "gk" + "-" + val;
			}
			
			if (!PowerPackConfig.GLOBALGK_ENABDLED)
			{
				return "data/html/disabled.htm";
			}
			
			return "data/html/gatekeeper/" + pom + ".htm";
		}
		
		if (val == 0)
		{
			pom = "" + npcId;
		}
		else
		{
			pom = npcId + "-" + val;
		}
		
		if (player.getLevel() <= Config.FREE_TELEPORT_UNTIL)
		{
			return "data/html/teleporter/free/" + pom + ".htm";
		}
		
		return "data/html/teleporter/" + pom + ".htm";
	}
	
	@Override
	public void showChatWindow(L2PcInstance player)
	{
		String filename = "data/html/teleporter/castleteleporter-no.htm";
		
		int condition = validateCondition(player);
		if (condition == COND_REGULAR)
		{
			super.showChatWindow(player);
			return;
		}
		else if (condition > COND_ALL_FALSE)
		{
			if (condition == COND_BUSY_BECAUSE_OF_SIEGE)
			{
				filename = "data/html/teleporter/castleteleporter-busy.htm"; // Busy because of siege
			}
			else if (condition == COND_OWNER)
			{
				filename = getHtmlPath(player, getNpcId(), 0);
			}
		}
		
		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(filename);
		html.replace("%objectId%", String.valueOf(getObjectId()));
		html.replace("%npcname%", getName());
		player.sendPacket(html);
	}
	
	/**
	 * Do teleport.
	 * @param player the player
	 * @param val the val
	 */
	private void doTeleport(L2PcInstance player, int val)
	{
		L2TeleportLocation list = TeleportLocationTable.getInstance().getTemplate(val);
		
		if (list != null && player != null)
		{
			// you cannot teleport to village that is in siege
			if (!SiegeManager.getInstance().is_teleport_to_siege_allowed() && SiegeManager.getInstance().getSiege(list.getLocX(), list.getLocY(), list.getLocZ()) != null && !player.isNoble())
			{
				player.sendPacket(new SystemMessage(SystemMessageId.NO_PORT_THAT_IS_IN_SIGE));
				return;
			}
			else if (!SiegeManager.getInstance().is_teleport_to_siege_town_allowed() && TownManager.townHasCastleInSiege(list.getLocX(), list.getLocY()) && !player.isNoble())
			{
				player.sendPacket(new SystemMessage(SystemMessageId.NO_PORT_THAT_IS_IN_SIGE));
				return;
			}
			else if (!player.isGM() && !Config.FLAGED_PLAYER_CAN_USE_GK && player.getPvpFlag() > 0)
			{
				player.sendMessage("Don't run from PvP! You will be able to use the teleporter only after your flag is gone.");
				return;
			}
			else if (!Config.ALT_GAME_KARMA_PLAYER_CAN_USE_GK && player.getKarma() > 0) // karma
			{
				SystemMessage sm = new SystemMessage(SystemMessageId.S1_S2);
				sm.addString("Go away, you're not welcome here.");
				player.sendPacket(sm);
				return;
			}
			else if (list.getIsForNoble() && !player.isNoble())
			{
				String filename = "data/html/teleporter/nobleteleporter-no.htm";
				NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
				html.setFile(filename);
				html.replace("%objectId%", String.valueOf(getObjectId()));
				html.replace("%npcname%", getName());
				player.sendPacket(html);
				return;
			}
			else if (player.isAlikeDead())
			{
				player.sendMessage("You can't use teleport when you are dead.");
				return;
			}
			else if (player.isSitting())
			{
				player.sendMessage("You can't use teleport when you are sitting.");
				return;
			}
			else if (list.getTeleId() == 9982 && list.getTeleId() == 9983 && list.getTeleId() == 9984 && getNpcId() == 30483 && player.getLevel() >= Config.CRUMA_TOWER_LEVEL_RESTRICT)
			{
				// Chars level XX can't enter in Cruma Tower. Retail: level 56 and above
				int maxlvl = Config.CRUMA_TOWER_LEVEL_RESTRICT;
				
				String filename = "data/html/teleporter/30483-biglvl.htm";
				NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
				html.setFile(filename);
				html.replace("%allowedmaxlvl%", "" + maxlvl + "");
				player.sendPacket(html);
				return;
			}
			// Lilith and Anakim have BossZone, so players must be allowed to enter
			else if (list.getTeleId() == 450)
			{
				L2BossZone _zone = GrandBossManager.getInstance().getZone(list.getLocX(), list.getLocY(), list.getLocZ());
				_zone.allowPlayerEntry(player, 300);
				player.teleToLocation(list.getLocX(), list.getLocY(), list.getLocZ(), true);
			}
			else if (!list.getIsForNoble() && ((Config.ALT_GAME_FREE_TELEPORT || player.getLevel() <= Config.FREE_TELEPORT_UNTIL) || player.reduceAdena("Teleport", list.getPrice(), this, true)))
			{
				player.teleToLocation(list.getLocX(), list.getLocY(), list.getLocZ(), true);
			}
			else if (list.getIsForNoble() && (Config.ALT_GAME_FREE_TELEPORT || player.destroyItemByItemId("Noble Teleport", 6651, list.getPrice(), this, true)))
			{
				player.teleToLocation(list.getLocX(), list.getLocY(), list.getLocZ(), true);
			}
		}
		else
		{
			LOG.warn("No teleport destination with id:" + val);
		}
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	/**
	 * Validate condition.
	 * @param player the player
	 * @return the int
	 */
	private int validateCondition(L2PcInstance player)
	{
		if (CastleManager.getInstance().getCastleIndex(this) < 0)
		{
			return COND_REGULAR; // Regular access
		}
		else if (getCastle().getSiege().getIsInProgress())
		{
			return COND_BUSY_BECAUSE_OF_SIEGE; // Busy because of siege
		}
		else if (player.getClan() != null) // Teleporter is on castle ground and player is in a clan
		{
			if (getCastle().getOwnerId() == player.getClanId())
			{
				return COND_OWNER; // Owner
			}
		}
		
		return COND_ALL_FALSE;
	}
}
