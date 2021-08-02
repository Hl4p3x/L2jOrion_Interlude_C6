/*
 * L2jOrion Project - www.l2jorion.com 
 * 
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package l2jorion.game.model.actor.instance;

import l2jorion.game.model.L2Effect.EffectType;
import l2jorion.game.model.entity.sevensigns.SevenSigns;
import l2jorion.game.model.zone.ZoneId;
import l2jorion.game.network.serverpackets.ActionFailed;
import l2jorion.game.network.serverpackets.NpcHtmlMessage;
import l2jorion.game.templates.L2NpcTemplate;


public class L2CastleMagicianInstance extends L2NpcInstance
{
	
	/** The Constant COND_ALL_FALSE. */
	protected static final int COND_ALL_FALSE = 0;
	
	/** The Constant COND_BUSY_BECAUSE_OF_SIEGE. */
	protected static final int COND_BUSY_BECAUSE_OF_SIEGE = 1;
	
	/** The Constant COND_OWNER. */
	protected static final int COND_OWNER = 2;
	
	/**
	 * Instantiates a new l2 castle magician instance.
	 * @param objectId the object id
	 * @param template the template
	 */
	public L2CastleMagicianInstance(final int objectId, final L2NpcTemplate template)
	{
		super(objectId, template);
	}
	
	/*
	 * (non-Javadoc)
	 * @see l2jorion.game.model.actor.instance.L2NpcInstance#showChatWindow(l2jorion.game.model.actor.instance.L2PcInstance, int)
	 */
	@Override
	public void showChatWindow(final L2PcInstance player, final int val)
	{
		player.sendPacket(ActionFailed.STATIC_PACKET);
		String filename = "data/html/castlemagician/magician-no.htm";
		
		final int condition = validateCondition(player);
		if (condition > COND_ALL_FALSE)
		{
			if (condition == COND_BUSY_BECAUSE_OF_SIEGE)
				filename = "data/html/castlemagician/magician-busy.htm"; // Busy because of siege
			else if (condition == COND_OWNER) // Clan owns castle
			{
				if (val == 0)
					filename = "data/html/castlemagician/magician.htm";
				else
					filename = "data/html/castlemagician/magician-" + val + ".htm";
			}
		}
		
		final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(filename);
		html.replace("%objectId%", String.valueOf(getObjectId()));
		player.sendPacket(html);
	}
	
	/*
	 * (non-Javadoc)
	 * @see l2jorion.game.model.actor.instance.L2NpcInstance#onBypassFeedback(l2jorion.game.model.actor.instance.L2PcInstance, java.lang.String)
	 */
	@Override
	public void onBypassFeedback(final L2PcInstance player, final String command)
	{
		if (command.startsWith("Chat"))
		{
			int val = 0;
			try
			{
				val = Integer.parseInt(command.substring(5));
			}
			catch (IndexOutOfBoundsException | NumberFormatException ioobe)
			{
			}
			showChatWindow(player, val);
			return;
		}
		else if (command.equals("gotoleader"))
		{
			if (player.getClan() != null)
			{
				final L2PcInstance clanLeader = player.getClan().getLeader().getPlayerInstance();
				if (clanLeader == null)
					return;
				
				if (clanLeader.getFirstEffect(EffectType.CLAN_GATE) != null)
				{
					if (!validateGateCondition(clanLeader, player))
						return;
					
					player.teleToLocation(clanLeader.getX(), clanLeader.getY(), clanLeader.getZ(), false);
					return;
				}
				final String filename = "data/html/castlemagician/magician-nogate.htm";
				showChatWindow(player, filename);
			}
			return;
		}
		else
			super.onBypassFeedback(player, command);
	}
	
	/**
	 * Validate condition.
	 * @param player the player
	 * @return the int
	 */
	protected int validateCondition(final L2PcInstance player)
	{
		if (getCastle() != null && getCastle().getCastleId() > 0)
		{
			if (player.getClan() != null)
			{
				if (getCastle().getZone().isSiegeActive())
					return COND_BUSY_BECAUSE_OF_SIEGE; // Busy because of siege
				else if (getCastle().getOwnerId() == player.getClanId()) // Clan owns castle
					return COND_OWNER;
			}
		}
		return COND_ALL_FALSE;
	}
	
	/**
	 * Validate gate condition.
	 * @param clanLeader the clan leader
	 * @param player the player
	 * @return true, if successful
	 */
	private static final boolean validateGateCondition(final L2PcInstance clanLeader, final L2PcInstance player)
	{
		if (clanLeader.isAlikeDead() || clanLeader.isInStoreMode() || clanLeader.isRooted() || clanLeader.isInCombat() || clanLeader.isInOlympiadMode() || clanLeader.isFestivalParticipant() || clanLeader.inObserverMode() || clanLeader.isInsideZone(ZoneId.ZONE_NOSUMMONFRIEND))
		{
			player.sendMessage("Couldn't teleport to clan leader. The requirements was not meet.");
			return false;
		}
		
		if (player.isIn7sDungeon())
		{
			final int targetCabal = SevenSigns.getInstance().getPlayerCabal(clanLeader);
			if (SevenSigns.getInstance().isSealValidationPeriod())
			{
				if (targetCabal != SevenSigns.getInstance().getCabalHighestScore())
				{
					player.sendMessage("Couldn't teleport to clan leader. The requirements was not meet.");
					return false;
				}
			}
			else
			{
				if (targetCabal == SevenSigns.CABAL_NULL)
				{
					player.sendMessage("Couldn't teleport to clan leader. The requirements was not meet.");
					return false;
				}
			}
		}
		
		return true;
	}
}