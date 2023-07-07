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

import java.util.StringTokenizer;

import l2jorion.Config;
import l2jorion.game.ai.CtrlIntention;
import l2jorion.game.controllers.TradeController;
import l2jorion.game.model.L2Clan;
import l2jorion.game.model.L2TradeList;
import l2jorion.game.network.serverpackets.ActionFailed;
import l2jorion.game.network.serverpackets.BuyList;
import l2jorion.game.network.serverpackets.MoveToPawn;
import l2jorion.game.network.serverpackets.NpcHtmlMessage;
import l2jorion.game.network.serverpackets.SocialAction;
import l2jorion.game.templates.L2NpcTemplate;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;
import l2jorion.util.random.Rnd;

public final class L2MercManagerInstance extends L2FolkInstance
{
	private static Logger LOG = LoggerFactory.getLogger(L2MercManagerInstance.class);
	
	private static final int COND_ALL_FALSE = 0;
	private static final int COND_BUSY_BECAUSE_OF_SIEGE = 1;
	private static final int COND_OWNER = 2;
	
	public L2MercManagerInstance(final int objectId, final L2NpcTemplate template)
	{
		super(objectId, template);
	}
	
	@Override
	public void onAction(final L2PcInstance player)
	{
		if (!canTarget(player))
		{
			return;
		}
		
		player.setLastFolkNPC(this);
		
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
				
				broadcastPacket(new SocialAction(getObjectId(), Rnd.get(8)));
				
				showMessageWindow(player);
			}
		}
		player.sendPacket(ActionFailed.STATIC_PACKET);
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
		else if (condition == COND_OWNER)
		{
			StringTokenizer st = new StringTokenizer(command, " ");
			String actualCommand = st.nextToken(); // Get actual command
			
			String val = "";
			if (st.countTokens() >= 1)
			{
				val = st.nextToken();
			}
			
			if (actualCommand.equalsIgnoreCase("hire"))
			{
				if (val.isEmpty())
				{
					return;
				}
				
				player.setTempAccess(false);
				
				showBuyWindow(player, Integer.parseInt(val));
				return;
			}
			st = null;
			actualCommand = null;
		}
		
		super.onBypassFeedback(player, command);
	}
	
	private void showBuyWindow(final L2PcInstance player, final int val)
	{
		player.tempInvetoryDisable();
		if (Config.DEBUG)
		{
			LOG.debug("Showing buylist");
		}
		L2TradeList list = TradeController.getInstance().getBuyList(val);
		if (list != null && list.getNpcId().equals(String.valueOf(getNpcId())))
		{
			BuyList bl = new BuyList(list, player.getAdena(), 0);
			player.sendPacket(bl);
			list = null;
			bl = null;
		}
		else
		{
			LOG.warn("possible client hacker: " + player.getName() + " attempting to buy from GM shop! (L2MercManagerIntance)");
			LOG.warn("buylist id:" + val);
		}
	}
	
	public void showMessageWindow(final L2PcInstance player)
	{
		String filename = "data/html/mercmanager/mercmanager-no.htm";
		
		final int condition = validateCondition(player);
		if (condition == COND_BUSY_BECAUSE_OF_SIEGE)
		{
			filename = "data/html/mercmanager/mercmanager-busy.htm"; // Busy because of siege
		}
		else if (condition == COND_OWNER)
		{
			filename = "data/html/mercmanager/mercmanager.htm"; // Owner message window
		}
		
		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(filename);
		html.replace("%objectId%", String.valueOf(getObjectId()));
		html.replace("%npcId%", String.valueOf(getNpcId()));
		html.replace("%npcname%", getName());
		player.sendPacket(html);
		filename = null;
		html = null;
	}
	
	private int validateCondition(final L2PcInstance player)
	{
		if (getCastle() != null && getCastle().getCastleId() > 0)
		{
			if (player.getClan() != null)
			{
				if (getCastle().getSiege().getIsInProgress())
				{
					return COND_BUSY_BECAUSE_OF_SIEGE; // Busy because of siege
				}
				else if (getCastle().getOwnerId() == player.getClanId()) // Clan owns castle
				{
					if ((player.getClanPrivileges() & L2Clan.CP_CS_MERCENARIES) == L2Clan.CP_CS_MERCENARIES)
					{
						return COND_OWNER;
					}
				}
			}
		}
		
		return COND_ALL_FALSE;
	}
}
