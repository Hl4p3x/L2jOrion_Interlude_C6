/* L2jOrion Project - www.l2jorion.com 
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

import l2jorion.game.ai.CtrlIntention;
import l2jorion.game.datatables.SkillTable;
import l2jorion.game.managers.CHSiegeManager;
import l2jorion.game.managers.ClanHallManager;
import l2jorion.game.model.entity.ClanHall;
import l2jorion.game.model.entity.siege.hallsiege.SiegableHall;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.ActionFailed;
import l2jorion.game.network.serverpackets.MoveToPawn;
import l2jorion.game.network.serverpackets.NpcHtmlMessage;
import l2jorion.game.network.serverpackets.Ride;
import l2jorion.game.network.serverpackets.SystemMessage;
import l2jorion.game.templates.L2NpcTemplate;

public class L2WyvernManagerInstance extends L2CastleChamberlainInstance
{
	private int _clanHallId = -1;
	
	public L2WyvernManagerInstance(final int objectId, final L2NpcTemplate template)
	{
		super(objectId, template);
	}
	
	@Override
	public void onBypassFeedback(final L2PcInstance player, final String command)
	{
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
					sm.addString("You already have a pet or are mounted.");
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
				final SystemMessage sm = new SystemMessage(SystemMessageId.S1_S2);
				sm.addString("Unsummon your pet.");
				player.sendPacket(sm);
			}
		}
		else
		{
			super.onBypassFeedback(player, command);
		}
	}
	
	@Override
	public void onAction(final L2PcInstance player)
	{
		if (!canTarget(player))
		{
			return;
		}
		
		if (this != player.getTarget())
		{
			player.setTarget(this);
		}
		else
		{
			if (!canInteract(player))
			{
				player.getAI().setIntention(CtrlIntention.AI_INTENTION_INTERACT, this);
			}
			else
			{
				if (player.isMoving())
				{
					player.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE, this);
				}
				
				player.broadcastPacket(new MoveToPawn(player, this, L2NpcInstance.INTERACTION_DISTANCE));
				
				showMessageWindow(player);
			}
		}
		
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	@Override
	protected int validateCondition(final L2PcInstance player)
	{
		if (player.getClan() != null)
		{
			if (getClanHall() != null && getClanHall().getOwnerId() == player.getClanId())
			{
				SiegableHall hall = (SiegableHall) getClanHall();
				if (hall != null && hall.isInSiege())
				{
					return COND_BUSY_BECAUSE_OF_SIEGE;
				}
				
				if (player.isClanLeader())
				{
					return COND_OWNER;
				}
				
				return COND_CLAN_MEMBER;
			}
			else if (getCastle() != null && getCastle().getCastleId() > 0)
			{
				if (getCastle().getSiege().getIsInProgress())
				{
					return COND_BUSY_BECAUSE_OF_SIEGE;
				}
				
				if (getCastle().getOwnerId() == player.getClanId())
				{
					if (player.isClanLeader())
					{
						return COND_OWNER;
					}
					
					return COND_CLAN_MEMBER;
				}
			}
		}
		return COND_ALL_FALSE;
	}
	
	private void showMessageWindow(final L2PcInstance player)
	{
		player.sendPacket(ActionFailed.STATIC_PACKET);
		
		String filename = "data/html/wyvernmanager/wyvernmanager-no.htm";
		
		final int condition = validateCondition(player);
		if (condition > COND_ALL_FALSE)
		{
			if (condition == COND_OWNER)
			{
				filename = "data/html/wyvernmanager/wyvernmanager.htm"; // Owner message window
			}
			else if (condition == COND_CLAN_MEMBER)
			{
				filename = "data/html/wyvernmanager/wyvernmanager-clan.htm";
			}
		}
		NpcHtmlMessage html = new NpcHtmlMessage(1);
		html.setFile(filename);
		html.replace("%objectId%", String.valueOf(getObjectId()));
		html.replace("%npcname%", getName());
		player.sendPacket(html);
	}
	
	@Override
	public final ClanHall getClanHall()
	{
		if (_clanHallId < 0)
		{
			ClanHall temp = ClanHallManager.getInstance().getNearbyClanHall(getX(), getY(), 2000);
			if (temp == null)
			{
				temp = CHSiegeManager.getInstance().getNearbyClanHall(this);
			}
			
			if (temp != null)
			{
				_clanHallId = temp.getClanHallId();
			}
			
			if (_clanHallId < 0)
			{
				return null;
			}
		}
		return ClanHallManager.getInstance().getClanHallsById(_clanHallId);
	}
}
