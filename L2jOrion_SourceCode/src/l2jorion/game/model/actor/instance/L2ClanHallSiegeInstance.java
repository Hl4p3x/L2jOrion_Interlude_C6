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

import l2jorion.Config;
import l2jorion.game.ai.CtrlIntention;
import l2jorion.game.managers.CHSiegeManager;
import l2jorion.game.model.L2Clan;
import l2jorion.game.model.entity.siege.hallsiege.SiegableHall;
import l2jorion.game.model.quest.Quest;
import l2jorion.game.model.quest.Quest.QuestEventType;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.ActionFailed;
import l2jorion.game.network.serverpackets.MoveToPawn;
import l2jorion.game.network.serverpackets.SocialAction;
import l2jorion.game.templates.L2NpcTemplate;
import l2jorion.util.random.Rnd;

public class L2ClanHallSiegeInstance extends L2NpcInstance
{
	public L2ClanHallSiegeInstance(final int objectId, final L2NpcTemplate template)
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
		
		if (this != player.getTarget())
		{
			player.setTarget(this);
		}
		else
		{
			if (isAutoAttackable(player))
			{
				player.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, this);
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
					
					Quest[] questList = getTemplate().getEventQuests(QuestEventType.NPC_FIRST_TALK);
					if ((questList.length >= 1))
					{
						questList[0].notifyFirstTalk(this, player);
					}
					else
					{
						showChatWindow(player);
					}
					
					player.sendPacket(ActionFailed.STATIC_PACKET);
				}
			}
		}
	}
	
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
				if (Config.ENABLE_ALL_EXCEPTIONS)
				{
					ioobe.printStackTrace();
				}
			}
			showChatWindow(player, val);
		}
		else if (command.startsWith("Registration"))
		{
			L2Clan clan = player.getClan();
			if (clan == null)
			{
				return;
			}
			
			SiegableHall hall = CHSiegeManager.getInstance().getSiegableHall(getClanHall().getClanHallId());
			if (hall != null)
			{
				if (System.currentTimeMillis() < clan.getDissolvingExpiryTime())
				{
					player.sendPacket(SystemMessageId.CANT_PARTICIPATE_IN_SIEGE_WHILE_DISSOLUTION_IN_PROGRESS);
					return;
				}
				CHSiegeManager.getInstance().registerClan(clan, hall, player);
			}
		}
		else if (command.startsWith("UnRegister"))
		{
			L2Clan clan = player.getClan();
			if (clan == null)
			{
				return;
			}
			
			SiegableHall hall = CHSiegeManager.getInstance().getSiegableHall(getClanHall().getClanHallId());
			if (hall != null)
			{
				CHSiegeManager.getInstance().unRegisterClan(clan, hall, player);
			}
		}
	}
	
	@Override
	public String getHtmlPath(L2PcInstance player, int npcId, int val)
	{
		String pom = "";
		
		if (val == 0)
		{
			pom = "" + npcId;
		}
		else
		{
			pom = npcId + "-" + val;
		}
		
		return "data/html/SiegableHall/" + pom + ".htm";
	}
}
