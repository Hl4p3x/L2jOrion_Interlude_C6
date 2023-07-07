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

import l2jorion.Config;
import l2jorion.game.managers.QuestManager;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.quest.Quest;
import l2jorion.game.model.quest.QuestState;
import l2jorion.game.network.PacketClient;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.QuestList;
import l2jorion.game.network.serverpackets.SystemMessage;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;

public final class RequestQuestAbort extends PacketClient
{
	private static Logger LOG = LoggerFactory.getLogger(RequestQuestAbort.class);
	
	private int _questId;
	
	@Override
	protected void readImpl()
	{
		_questId = readD();
	}
	
	@Override
	protected void runImpl()
	{
		final L2PcInstance activeChar = getClient().getActiveChar();
		
		if (activeChar == null)
		{
			return;
		}
		
		Quest qe = null;
		if (!Config.ALT_DEV_NO_QUESTS)
		{
			qe = QuestManager.getInstance().getQuest(_questId);
		}
		
		if (qe != null)
		{
			final QuestState qs = activeChar.getQuestState(qe.getName());
			if (qs != null)
			{
				/*
				 * if (qs == activeChar.getQuestState("605_AllianceWithKetraOrcs") || qs == activeChar.getQuestState("611_AllianceWithVarkaSilenos")) { activeChar.setAllianceWithVarkaKetra(0); }
				 */
				
				qs.exitQuest(true);
				SystemMessage sm = new SystemMessage(SystemMessageId.S1_S2);
				sm.addString("Quest aborted.");
				activeChar.sendPacket(sm);
				
				final QuestList ql = new QuestList();
				activeChar.sendPacket(ql);
			}
			else
			{
				if (Config.DEBUG)
				{
					LOG.info("Player '" + activeChar.getName() + "' try to abort quest " + qe.getName() + " but he didn't have it started.");
				}
			}
		}
		else
		{
			if (Config.DEBUG)
			{
				LOG.warn("Quest (id='" + _questId + "') not found.");
			}
		}
	}
	
	@Override
	public String getType()
	{
		return "[C] 64 RequestQuestAbort";
	}
}
