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

import java.util.StringTokenizer;

import l2jorion.game.ai.CtrlIntention;
import l2jorion.game.datatables.SkillTable;
import l2jorion.game.datatables.csv.DoorTable;
import l2jorion.game.datatables.sql.ClanTable;
import l2jorion.game.managers.CHSiegeManager;
import l2jorion.game.managers.ClanHallManager;
import l2jorion.game.model.L2Clan;
import l2jorion.game.model.entity.ClanHall;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.ActionFailed;
import l2jorion.game.network.serverpackets.MoveToPawn;
import l2jorion.game.network.serverpackets.MyTargetSelected;
import l2jorion.game.network.serverpackets.NpcHtmlMessage;
import l2jorion.game.network.serverpackets.Ride;
import l2jorion.game.network.serverpackets.SystemMessage;
import l2jorion.game.templates.L2NpcTemplate;

public class L2DoormenInstance extends L2FolkInstance
{
	private ClanHall _clanHall;
	
	private static int COND_ALL_FALSE = 0;
	private static int COND_BUSY_BECAUSE_OF_SIEGE = 1;
	private static int COND_CASTLE_OWNER = 2;
	private static int COND_HALL_OWNER = 3;
	private static int COND_FORT_OWNER = 4;
	
	public L2DoormenInstance(final int objectID, final L2NpcTemplate template)
	{
		super(objectID, template);
	}
	
	@Override
	public final ClanHall getClanHall()
	{
		if (_clanHall == null)
		{
			_clanHall = ClanHallManager.getInstance().getNearbyClanHall(getX(), getY(), 500);
		}
		
		if (_clanHall == null)
		{
			_clanHall = CHSiegeManager.getInstance().getNearbyClanHall(this);
		}
		
		return _clanHall;
	}
	
	@Override
	public void onBypassFeedback(final L2PcInstance player, final String command)
	{
		final int condition = validateCondition(player);
		if (condition <= COND_ALL_FALSE)
		{
			return;
		}
		if (condition == COND_BUSY_BECAUSE_OF_SIEGE)
		{
			return;
		}
		else if (condition == COND_CASTLE_OWNER || condition == COND_HALL_OWNER || condition == COND_FORT_OWNER)
		{
			if (command.startsWith("Chat"))
			{
				showMessageWindow(player);
				return;
			}
			else if (command.startsWith("open_chdoors"))
			{
				if (condition == COND_HALL_OWNER)
				{
					if (isUnderSiege())
					{
						cannotManageDoors(player);
					}
					else
					{
						openDoors(player, command);
					}
				}
				return;
			}
			else if (command.startsWith("open_doors"))
			{
				if (condition == COND_HALL_OWNER)
				{
					StringTokenizer st = new StringTokenizer(command.substring(10), ", ");
					st.nextToken();
					
					while (st.hasMoreTokens())
					{
						DoorTable.getInstance().getDoor(Integer.parseInt(st.nextToken())).openMe();
					}
					return;
				}
				else if (condition == COND_CASTLE_OWNER)
				{
					StringTokenizer st = new StringTokenizer(command.substring(10), ", ");
					st.nextToken();
					
					while (st.hasMoreTokens())
					{
						DoorTable.getInstance().getDoor(Integer.parseInt(st.nextToken())).openMe();
					}
					return;
				}
				else if (condition == COND_FORT_OWNER)
				{
					StringTokenizer st = new StringTokenizer(command.substring(10), ", ");
					st.nextToken();
					
					while (st.hasMoreTokens())
					{
						DoorTable.getInstance().getDoor(Integer.parseInt(st.nextToken())).openMe();
					}
					return;
				}
				
			}
			
			if (command.startsWith("RideWyvern"))
			{
				if (!player.isClanLeader())
				{
					player.sendMessage("Only clan leaders are allowed.");
					return;
				}
				if (player.getPet() == null)
				{
					if (player.isMounted())
					{
						final SystemMessage sm = new SystemMessage(SystemMessageId.S1_S2);
						sm.addString("You Already Have a Pet or Are Mounted.");
						player.sendPacket(sm);
					}
					else
					{
						final SystemMessage sm = new SystemMessage(SystemMessageId.S1_S2);
						sm.addString("Summon your Strider first.");
						player.sendPacket(sm);
					}
					return;
				}
				else if (player.getPet().getNpcId() == 12526 || player.getPet().getNpcId() == 12527 || player.getPet().getNpcId() == 12528)
				{
					if (player.getInventory().getItemByItemId(1460) != null && player.getInventory().getItemByItemId(1460).getCount() >= 10)
					{
						if (player.getPet().getLevel() < 55)
						{
							final SystemMessage sm = new SystemMessage(SystemMessageId.S1_S2);
							sm.addString("Your Strider Has not reached the required level.");
							player.sendPacket(sm);
						}
						else
						{
							if (!player.disarmWeapons())
							{
								return;
							}
							player.getPet().unSummon(player);
							player.getInventory().destroyItemByItemId("Wyvern", 1460, 10, player, player.getTarget());
							final Ride mount = new Ride(player.getObjectId(), Ride.ACTION_MOUNT, 12621);
							player.sendPacket(mount);
							player.broadcastPacket(mount);
							player.setMountType(mount.getMountType());
							player.addSkill(SkillTable.getInstance().getInfo(4289, 1));
							final SystemMessage sm = new SystemMessage(SystemMessageId.S1_S2);
							sm.addString("The Wyvern has been summoned successfully!");
							player.sendPacket(sm);
						}
					}
					else
					{
						final SystemMessage sm = new SystemMessage(SystemMessageId.S1_S2);
						sm.addString("You need 10 Crystals: B Grade.");
						player.sendPacket(sm);
					}
					return;
				}
				else
				{
					SystemMessage sm = new SystemMessage(SystemMessageId.S1_S2);
					sm.addString("Unsummon your pet.");
					player.sendPacket(sm);
					sm = null;
					return;
				}
			}
			else if (command.startsWith("close_chdoors"))
			{
				if (condition == COND_HALL_OWNER)
				{
					if (isUnderSiege())
					{
						cannotManageDoors(player);
					}
					else
					{
						closeDoors(player, command);
					}
				}
				return;
			}
			else if (command.startsWith("close_doors"))
			{
				if (condition == COND_HALL_OWNER)
				{
					StringTokenizer st = new StringTokenizer(command.substring(11), ", ");
					st.nextToken(); // Bypass first value since its castleid/hallid
					
					while (st.hasMoreTokens())
					{
						// getClanHall().closeDoor(player, Integer.parseInt(st.nextToken()));
						DoorTable.getInstance().getDoor(Integer.parseInt(st.nextToken())).closeMe();
					}
					return;
				}
				else if (condition == COND_CASTLE_OWNER)
				{
					// DoorTable doorTable = DoorTable.getInstance();
					StringTokenizer st = new StringTokenizer(command.substring(11), ", ");
					st.nextToken(); // Bypass first value since its castleid/hallid
					// L2Clan playersClan = player.getClan();
					
					while (st.hasMoreTokens())
					{
						// getCastle().closeDoor(player, Integer.parseInt(st.nextToken()));
						DoorTable.getInstance().getDoor(Integer.parseInt(st.nextToken())).closeMe();
					}
					return;
				}
				else if (condition == COND_FORT_OWNER)
				{
					StringTokenizer st = new StringTokenizer(command.substring(10), ", ");
					st.nextToken(); // Bypass first value since its castleid/hallid/fortid
					
					while (st.hasMoreTokens())
					{
						// getFort().closeDoor(player, Integer.parseInt(st.nextToken()));
						DoorTable.getInstance().getDoor(Integer.parseInt(st.nextToken())).closeMe();
					}
					return;
				}
			}
		}
		
		super.onBypassFeedback(player, command);
	}
	
	/**
	 * this is called when a player interacts with this NPC.
	 * @param player the player
	 */
	@Override
	public void onAction(final L2PcInstance player)
	{
		if (!canTarget(player))
		{
			return;
		}
		
		// Check if the L2PcInstance already target the L2NpcInstance
		if (this != player.getTarget())
		{
			// Set the target of the L2PcInstance player
			player.setTarget(this);
			
			// Send a Server->Client packet MyTargetSelected to the L2PcInstance player
			MyTargetSelected my = new MyTargetSelected(getObjectId(), 0);
			player.sendPacket(my);
		}
		else
		{
			// Calculate the distance between the L2PcInstance and the L2NpcInstance
			if (!canInteract(player))
			{
				// Notify the L2PcInstance AI with AI_INTENTION_INTERACT
				player.getAI().setIntention(CtrlIntention.AI_INTENTION_INTERACT, this);
			}
			else
			{
				// Rotate the player to face the instance
				player.broadcastPacket(new MoveToPawn(player, this, L2NpcInstance.INTERACTION_DISTANCE));
				
				showMessageWindow(player);
			}
		}
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	/**
	 * Show message window.
	 * @param player the player
	 */
	public void showMessageWindow(final L2PcInstance player)
	{
		player.sendPacket(ActionFailed.STATIC_PACKET);
		
		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		String str;
		String filename = "data/html/doormen/" + getTemplate().npcId + "-no.htm";
		
		final int condition = validateCondition(player);
		if (condition == COND_BUSY_BECAUSE_OF_SIEGE)
		{
			filename = "data/html/doormen/" + getTemplate().npcId + "-busy.htm"; // Busy because of siege
			html.setFile(filename);
		}
		else if (condition == COND_CASTLE_OWNER)
		{
			filename = "data/html/doormen/" + getTemplate().npcId + ".htm"; // Owner message window
			html.setFile(filename);
		}
		else if (condition == COND_FORT_OWNER)
		{
			filename = "data/html/doormen/fortress/" + getTemplate().npcId + ".htm";
			html.setFile(filename);
		}
		else if (condition == COND_HALL_OWNER)
		{
			final L2Clan owner = ClanTable.getInstance().getClan(getClanHall().getOwnerId());
			html.setFile("data/html/doormen/doormen.htm");
			html.replace("%clanname%", owner.getName());
		}
		else
		{
			if (getClanHall() != null)
			{
				final L2Clan owner = ClanTable.getInstance().getClan(getClanHall().getOwnerId());
				if (owner != null && owner.getLeader() != null)
				{
					str = "<html><body>Hello there!<br>This clan hall is owned by <font color=\"55FFFF\">" + owner.getLeader().getName() + " who is the Lord of the ";
					str += owner.getName() + "</font> clan.<br>";
					str += "I am sorry, but only the clan members who belong to the <font color=\"55FFFF\">" + owner.getName() + "</font> clan can enter the clan hall.</body></html>";
				}
				else
				{
					str = "<html><body>" + getName() + ":<br1>Clan hall <font color=\"LEVEL\">" + getClanHall().getName() + "</font> have no owner clan.<br>You can rent it at auctioneers...</body></html>";
				}
				html.setHtml(str);
			}
			else
			{
				html.setFile(filename);
			}
		}
		
		html.replace("%objectId%", String.valueOf(getObjectId()));
		player.sendPacket(html);
	}
	
	protected final void openDoors(L2PcInstance player, String command)
	{
		getClanHall().openCloseDoors(true);
		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile("data/html/doormen/doormen-opened.htm");
		html.replace("%objectId%", String.valueOf(getObjectId()));
		player.sendPacket(html);
	}
	
	protected final void closeDoors(L2PcInstance player, String command)
	{
		getClanHall().openCloseDoors(false);
		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile("data/html/doormen/doormen-closed.htm");
		html.replace("%objectId%", String.valueOf(getObjectId()));
		player.sendPacket(html);
	}
	
	protected void cannotManageDoors(L2PcInstance player)
	{
		player.sendPacket(ActionFailed.STATIC_PACKET);
		
		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile("data/html/Doormen/" + getTemplate().getNpcId() + "-busy.htm");
		player.sendPacket(html);
	}
	
	/**
	 * Validate condition.
	 * @param player the player
	 * @return the int
	 */
	private int validateCondition(final L2PcInstance player)
	{
		if (player.getClan() != null)
		{
			// Prepare doormen for clan hall
			if (getClanHall() != null)
			{
				if (player.getClanId() == getClanHall().getOwnerId())
				{
					return COND_HALL_OWNER;
				}
				
				return COND_ALL_FALSE;
			}
			// Prepare doormen for Castle
			if (getCastle() != null && getCastle().getCastleId() > 0)
			{
				if (getCastle().getOwnerId() == player.getClanId())
				{
					return COND_CASTLE_OWNER; // Owner
				}
			}
			// Prepare doormen for Fortress
			if (getFort() != null && getFort().getFortId() > 0)
			{
				if (getFort().getOwnerId() == player.getClanId())
				{
					return COND_FORT_OWNER;
				}
			}
		}
		
		return COND_ALL_FALSE;
	}
	
	protected boolean isUnderSiege()
	{
		return false;
	}
}
