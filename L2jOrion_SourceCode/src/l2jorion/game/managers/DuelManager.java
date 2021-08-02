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
package l2jorion.game.managers;

import javolution.util.FastList;
import l2jorion.game.model.L2Effect;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.entity.Duel;
import l2jorion.game.network.serverpackets.L2GameServerPacket;

public class DuelManager
{
	//private static final Logger LOG = LoggerFactory.getLogger(DuelManager.class);
	
	private static DuelManager _instance;
	
	public static final DuelManager getInstance()
	{
		if (_instance == null)
		{
			_instance = new DuelManager();
		}
		return _instance;
	}
	
	// Data Field
	private final FastList<Duel> _duels;
	private int _currentDuelId = 0x90;
	
	private DuelManager()
	{
		//LOG.info("Initializing DuelManager");
		_duels = new FastList<>();
	}
	
	private int getNextDuelId()
	{
		_currentDuelId++;
		// In case someone wants to run the server forever :)
		if (_currentDuelId >= 2147483640)
		{
			_currentDuelId = 1;
		}
		
		return _currentDuelId;
	}
	
	public Duel getDuel(final int duelId)
	{
		for (FastList.Node<Duel> e = _duels.head(), end = _duels.tail(); (e = e.getNext()) != end;)
		{
			if (e.getValue().getId() == duelId)
			{
				return e.getValue();
			}
		}
		return null;
	}
	
	public void addDuel(final L2PcInstance playerA, final L2PcInstance playerB, final int partyDuel)
	{
		if (playerA == null || playerB == null)
			return;
		
		// return if a player has PvPFlag
		String engagedInPvP = "The duel was canceled because a duelist engaged in PvP combat.";
		if (partyDuel == 1)
		{
			boolean playerInPvP = false;
			for (final L2PcInstance temp : playerA.getParty().getPartyMembers())
			{
				if (temp.getPvpFlag() != 0)
				{
					playerInPvP = true;
					break;
				}
			}
			if (!playerInPvP)
			{
				for (final L2PcInstance temp : playerB.getParty().getPartyMembers())
				{
					if (temp.getPvpFlag() != 0)
					{
						playerInPvP = true;
						break;
					}
				}
			}
			// A player has PvP flag
			if (playerInPvP)
			{
				for (final L2PcInstance temp : playerA.getParty().getPartyMembers())
				{
					temp.sendMessage(engagedInPvP);
				}
				for (final L2PcInstance temp : playerB.getParty().getPartyMembers())
				{
					temp.sendMessage(engagedInPvP);
				}
				return;
			}
		}
		else
		{
			if (playerA.getPvpFlag() != 0 || playerB.getPvpFlag() != 0)
			{
				playerA.sendMessage(engagedInPvP);
				playerB.sendMessage(engagedInPvP);
				return;
			}
		}
		
		engagedInPvP = null;
		
		Duel duel = new Duel(playerA, playerB, partyDuel, getNextDuelId());
		_duels.add(duel);
		
		duel = null;
	}
	
	public void removeDuel(final Duel duel)
	{
		_duels.remove(duel);
	}
	
	public void doSurrender(final L2PcInstance player)
	{
		if (player == null || !player.isInDuel())
			return;
		Duel duel = getDuel(player.getDuelId());
		duel.doSurrender(player);
		duel = null;
	}
	
	/**
	 * Updates player states.
	 * @param player - the dieing player
	 */
	public void onPlayerDefeat(final L2PcInstance player)
	{
		if (player == null || !player.isInDuel())
		{
			return;
		}
		Duel duel = getDuel(player.getDuelId());
		if (duel != null)
		{
			duel.onPlayerDefeat(player);
		}
	}
	
	/**
	 * Registers a debuff which will be removed if the duel ends
	 * @param player
	 * @param buff
	 */
	public void onBuff(final L2PcInstance player, final L2Effect buff)
	{
		if (player == null || !player.isInDuel() || buff == null)
		{
			return;
		}
		Duel duel = getDuel(player.getDuelId());
		if (duel != null)
		{
			duel.onBuff(player, buff);
		}
	}
	
	/**
	 * Removes player from duel.
	 * @param player - the removed player
	 */
	public void onRemoveFromParty(final L2PcInstance player)
	{
		if (player == null || !player.isInDuel())
		{
			return;
		}
		Duel duel = getDuel(player.getDuelId());
		if (duel != null)
		{
			duel.onRemoveFromParty(player);
		}
	}
	
	/**
	 * Broadcasts a packet to the team opposing the given player.
	 * @param player
	 * @param packet
	 */
	public void broadcastToOppositTeam(final L2PcInstance player, final L2GameServerPacket packet)
	{
		if (player == null || !player.isInDuel())
		{
			return;
		}
		Duel duel = getDuel(player.getDuelId());
		
		if (duel == null)
		{
			return;
		}
		if (duel.getPlayerA() == null || duel.getPlayerB() == null)
		{
			return;
		}
		
		if (duel.getPlayerA() == player)
		{
			duel.broadcastToTeam2(packet);
		}
		else if (duel.getPlayerB() == player)
		{
			duel.broadcastToTeam1(packet);
		}
		else if (duel.isPartyDuel())
		{
			if (duel.getPlayerA().getParty() != null && duel.getPlayerA().getParty().getPartyMembers().contains(player))
			{
				duel.broadcastToTeam2(packet);
			}
			else if (duel.getPlayerB().getParty() != null && duel.getPlayerB().getParty().getPartyMembers().contains(player))
			{
				duel.broadcastToTeam1(packet);
			}
		}
	}
}
