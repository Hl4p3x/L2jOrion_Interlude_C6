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
package l2jorion.game.network.clientpackets;

import l2jorion.Config;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.quest.QuestState;
import l2jorion.game.network.PacketClient;
import l2jorion.game.network.serverpackets.TutorialCloseHtml;

public class RequestTutorialLinkHtml extends PacketClient
{
	private String _bypass;
	private boolean protector_packet = false;
	private int answer_id = 0;
	
	@Override
	protected void readImpl()
	{
		_bypass = readS();
		
		if (_bypass != null)
		{
			
			try
			{
				answer_id = Integer.parseInt(_bypass);
				protector_packet = true;
			}
			catch (final NumberFormatException e)
			{
				// not bot protection packet
			}
		}
	}
	
	@Override
	protected void runImpl()
	{
		final L2PcInstance player = getClient().getActiveChar();
		if (player == null)
		{
			return;
		}
		
		if (protector_packet)
		{
			
			if (Config.BOT_PROTECTOR && answer_id >= 100001 && answer_id <= 100005)
			{
				
				player.checkAnswer(answer_id);
				player.sendPacket(new TutorialCloseHtml());
				return;
				
			}
			
		}
		else
		{
			
			final QuestState qs = player.getQuestState("255_Tutorial");
			if (qs != null)
			{
				qs.getQuest().notifyEvent(_bypass, null, player);
			}
			
		}
		
	}
	
	@Override
	public String getType()
	{
		return "[C] 7b RequestTutorialLinkHtml";
	}
}
