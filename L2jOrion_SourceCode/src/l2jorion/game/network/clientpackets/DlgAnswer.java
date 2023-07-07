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
import l2jorion.game.network.PacketClient;
import l2jorion.game.network.SystemMessageId;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;

public final class DlgAnswer extends PacketClient
{
	private static Logger LOG = LoggerFactory.getLogger(DlgAnswer.class);
	private int _messageId, _answer, _requestId;
	
	@Override
	protected void readImpl()
	{
		_messageId = readD();
		_answer = readD();
		_requestId = readD();
	}
	
	@Override
	public void runImpl()
	{
		final L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null)
		{
			return;
		}
		
		if (Config.DEBUG)
		{
			LOG.debug(getType() + ": Answer acepted. Message ID " + _messageId + ", asnwer " + _answer + ", unknown field " + _requestId);
		}
		
		final Long answerTime = getClient().getActiveChar().getConfirmDlgRequestTime(_requestId);
		if (_answer == 1 && answerTime != null && System.currentTimeMillis() > answerTime)
		{
			_answer = 0;
		}
		getClient().getActiveChar().removeConfirmDlgRequestTime(_requestId);
		
		if (_messageId == SystemMessageId.RESSURECTION_REQUEST.getId())
		{
			activeChar.reviveAnswer(_answer);
		}
		else if (_messageId == SystemMessageId.S1_WISHES_TO_SUMMON_YOU_FROM_S2_DO_YOU_ACCEPT.getId())
		{
			activeChar.teleportAnswer(_answer, _requestId);
		}
		else if (_messageId == SystemMessageId.WOULD_YOU_LIKE_TO_OPEN_THE_GATE.getId())
		{
			activeChar.gatesAnswer(_answer, 1);
		}
		else if (_messageId == SystemMessageId.WOULD_YOU_LIKE_TO_CLOSE_THE_GATE.getId())
		{
			activeChar.gatesAnswer(_answer, 0);
		}
		else if (_messageId == 614 && Config.L2JMOD_ALLOW_WEDDING)
		{
			activeChar.EngageAnswer(_answer);
		}
		else if (_messageId == 1326)
		{
			if (activeChar.dialogAugmentation != null)
			{
				activeChar.dialogAugmentation.onDlgAnswer(activeChar, _answer);
				activeChar.dialogAugmentation = null;
			}
		}
		else if (_messageId == SystemMessageId.S1.getId())
		{
			if (activeChar.dialog != null)
			{
				activeChar.dialog.onDlgAnswer(activeChar);
				activeChar.dialog = null;
			}
		}
	}
	
	@Override
	public String getType()
	{
		return "[C] C5 DlgAnswer";
	}
}