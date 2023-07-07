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
import l2jorion.game.enums.AchType;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.network.PacketClient;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.SystemMessage;
import l2jorion.game.network.serverpackets.UserInfo;

public final class RequestEvaluate extends PacketClient
{
	@SuppressWarnings("unused")
	private int _targetId;
	
	@Override
	protected void readImpl()
	{
		_targetId = readD();
	}
	
	@Override
	protected void runImpl()
	{
		SystemMessage sm;
		final L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null)
		{
			return;
		}
		
		if (!(activeChar.getTarget() instanceof L2PcInstance))
		{
			sm = new SystemMessage(SystemMessageId.TARGET_IS_INCORRECT);
			activeChar.sendPacket(sm);
			sm = null;
			return;
		}
		
		if (activeChar.getLevel() < 10)
		{
			sm = new SystemMessage(SystemMessageId.ONLY_LEVEL_SUP_10_CAN_RECOMMEND);
			activeChar.sendPacket(sm);
			sm = null;
			return;
		}
		
		if (activeChar.getTarget() == activeChar)
		{
			sm = new SystemMessage(SystemMessageId.YOU_CANNOT_RECOMMEND_YOURSELF);
			activeChar.sendPacket(sm);
			sm = null;
			return;
		}
		
		if (activeChar.getRecomLeft() <= 0)
		{
			sm = new SystemMessage(SystemMessageId.NO_MORE_RECOMMENDATIONS_TO_HAVE);
			activeChar.sendPacket(sm);
			sm = null;
			return;
		}
		
		final L2PcInstance target = (L2PcInstance) activeChar.getTarget();
		
		if (target.getRecomHave() >= Config.ALT_RECOMMENDATIONS_NUMBER)
		{
			sm = new SystemMessage(SystemMessageId.YOU_NO_LONGER_RECIVE_A_RECOMMENDATION);
			activeChar.sendPacket(sm);
			sm = null;
			return;
		}
		
		if (!activeChar.canRecom(target))
		{
			sm = new SystemMessage(SystemMessageId.THAT_CHARACTER_IS_RECOMMENDED);
			activeChar.sendPacket(sm);
			sm = null;
			return;
		}
		
		activeChar.giveRecom(target);
		
		sm = new SystemMessage(SystemMessageId.YOU_HAVE_RECOMMENDED);
		sm.addString(target.getName());
		sm.addNumber(activeChar.getRecomLeft());
		activeChar.sendPacket(sm);
		activeChar.getAchievement().increase(AchType.RECOMMEND);
		
		sm = new SystemMessage(SystemMessageId.YOU_HAVE_BEEN_RECOMMENDED);
		sm.addString(activeChar.getName());
		target.sendPacket(sm);
		target.getAchievement().increase(AchType.RECOMMEND);
		
		activeChar.sendPacket(new UserInfo(activeChar));
		target.broadcastUserInfo();
	}
	
	@Override
	public String getType()
	{
		return "[C] B9 RequestEvaluate";
	}
}
