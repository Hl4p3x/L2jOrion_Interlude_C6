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
package l2jorion.game.network.clientpackets;

import l2jorion.game.model.L2Party;
import l2jorion.game.model.L2Skill;
import l2jorion.game.model.L2World;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.network.PacketClient;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.ExAskJoinMPCC;
import l2jorion.game.network.serverpackets.SystemMessage;

public final class RequestExAskJoinMPCC extends PacketClient
{
	private String _name;
	
	@Override
	protected void readImpl()
	{
		_name = readS();
	}
	
	@Override
	protected void runImpl()
	{
		final L2PcInstance activeChar = getClient().getActiveChar();
		
		if (activeChar == null)
		{
			return;
		}
		
		final L2PcInstance player = L2World.getInstance().getPlayer(_name);
		
		if (player == null)
		{
			return;
			// invite yourself? ;)
		}
		
		if (activeChar.isInParty() && player.isInParty() && activeChar.getParty().equals(player.getParty()))
		{
			return;
		}
		
		// activeChar is in a Party?
		if (activeChar.isInParty())
		{
			final L2Party activeParty = activeChar.getParty();
			// activeChar is PartyLeader? && activeChars Party is already in a CommandChannel?
			if (activeParty.getLeader().equals(activeChar))
			{
				// if activeChars Party is in CC, is activeChar CCLeader?
				if (activeParty.isInCommandChannel() && activeParty.getCommandChannel().getChannelLeader().equals(activeChar))
				{
					// in CC and the CCLeader
					// target in a party?
					if (player.isInParty())
					{
						// targets party already in a CChannel?
						if (player.getParty().isInCommandChannel())
						{
							activeChar.sendPacket(new SystemMessage(SystemMessageId.S1_ALREADY_MEMBER_OF_COMMAND_CHANNEL).addString(player.getName()));
						}
						else
						{
							askJoinMPCC(activeChar, player);
						}
					}
					else
					{
						activeChar.sendMessage(player.getName() + " doesn't have party and cannot be invited to Command Channel.");
					}
					
				}
				else if (activeParty.isInCommandChannel() && !activeParty.getCommandChannel().getChannelLeader().equals(activeChar))
				{
					activeChar.sendPacket(new SystemMessage(SystemMessageId.CANNOT_INVITE_TO_COMMAND_CHANNEL));
				}
				else
				{
					// target in a party?
					if (player.isInParty())
					{
						// targets party already in a CChannel?
						if (player.getParty().isInCommandChannel())
						{
							activeChar.sendPacket(new SystemMessage(SystemMessageId.S1_ALREADY_MEMBER_OF_COMMAND_CHANNEL).addString(player.getName()));
						}
						else
						{
							askJoinMPCC(activeChar, player);
						}
					}
					else
					{
						activeChar.sendMessage(player.getName() + " doesn't have party and cannot be invited to Command Channel.");
					}
				}
			}
			else
			{
				activeChar.sendPacket(SystemMessageId.CANNOT_INVITE_TO_COMMAND_CHANNEL);
			}
		}
		
	}
	
	private void askJoinMPCC(final L2PcInstance requestor, final L2PcInstance target)
	{
		boolean hasRight = false;
		if (requestor.getClan() != null && requestor.getClan().getLeaderId() == requestor.getObjectId() && requestor.getClan().getLevel() >= 5) // Clanleader of lvl5 Clan or higher
		{
			hasRight = true;
		}
		else if (requestor.getInventory().getItemByItemId(8871) != null)
		{
			hasRight = true;
		}
		else if (requestor.getPledgeClass() >= 5)
		{
			for (final L2Skill skill : requestor.getAllSkills())
			{
				// Skill Clan Imperium
				if (skill.getId() == 391)
				{
					hasRight = true;
					break;
				}
			}
		}
		
		if (!hasRight)
		{
			requestor.sendPacket(SystemMessageId.COMMAND_CHANNEL_ONLY_BY_LEVEL_5_CLAN_LEADER_PARTY_LEADER);
			return;
		}
		
		final L2PcInstance targetLeader = target.getParty().getLeader();
		if (!targetLeader.isProcessingRequest())
		{
			requestor.onTransactionRequest(targetLeader);
			targetLeader.sendPacket(new SystemMessage(SystemMessageId.COMMAND_CHANNEL_CONFIRM).addString(requestor.getName()));
			targetLeader.sendPacket(new ExAskJoinMPCC(requestor.getName()));
		}
		else
		{
			requestor.sendPacket(new SystemMessage(SystemMessageId.S1_IS_BUSY_TRY_LATER).addString(targetLeader.getName()));
		}
	}
	
	@Override
	public String getType()
	{
		return "[C] D0:0D RequestExAskJoinMPCC";
	}
	
}
